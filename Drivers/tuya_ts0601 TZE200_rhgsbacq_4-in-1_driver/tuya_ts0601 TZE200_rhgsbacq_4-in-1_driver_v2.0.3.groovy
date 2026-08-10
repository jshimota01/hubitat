/**
 *  Tuya TS0601 TZE200_rrhgsbacq 4 in 1 Driver (Temp, Humidity, Illuminance, Motion)
 *  Target Hardware: Model TS0601 / Manufacturer _TZE200_rhgsbacq (ZG-204ZV)
 *
 *  Licensed under the Apache License, Version 2.0
 */

static String version() { "2.0.3" }
static String timeStamp() { "2026/08/09 3:00 PM" }

import groovy.transform.Field
import hubitat.device.HubMultiAction
import hubitat.device.Protocol

metadata {
    definition (
        name: 'Tuya TS0601 TZE200_rrhgsbacq 4 in 1 Driver',
        importUrl: '',
        namespace: 'jshimota', 
        author: 'James Shimota', 
        singleThreaded: true 
    ) {
        capability 'MotionSensor'
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
        capability 'IlluminanceMeasurement'
        capability 'Battery'
        capability 'Sensor'
        capability 'Refresh'
        capability 'Initialize'

        attribute 'all', 'string'

        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0500,EF00,0402,0405,0001,0400", outClusters:"0003", model:"TS0601", manufacturer:"_TZE200_rhgsbacq", controllerType: "ZGB"
    }

    preferences {
        input name: 'txtEnable', type: 'bool', title: '<b>Enable descriptionText logging</b>', defaultValue: true
        input name: 'logEnable', type: 'bool', title: '<b>Enable debug logging</b>', defaultValue: false
        input name: 'allStatusTextEnable', type: 'bool', title: "<b>Enable 'all' Status Attribute Creation?</b>", defaultValue: false
        input name: 'invertMotion', type: 'bool', title: '<b>Invert Motion State</b>', defaultValue: false, description: 'Invert active/inactive reported logic if reversed.'
        input name: 'motionTimeout', type: 'number', title: '<b>Hubitat Motion Timeout (Seconds)</b>', defaultValue: 30, range: '0..3600', description: 'Local timeout to reset motion to inactive (0 to disable local timer).'
    }
}

// ========================================================================================================================
// Hubitat Lifecycle & Commands
// ========================================================================================================================

void refresh() {
    logInfo "Refreshing device..."
    sendZigbeeCommands(queryAllTuyaDP())
    scheduleFormatAttrib()
}

void initialize() {
    logInfo "Initializing device states..."
    if (settings.invertMotion == null) { device.updateSetting('invertMotion', false) }
    if (settings.motionTimeout == null) { device.updateSetting('motionTimeout', 30) }
    if (settings.allStatusTextEnable == null) { device.updateSetting('allStatusTextEnable', false) }
    
    // Default internal tracking without firing premature motion events
    if (state.rawMotion == null) { state.rawMotion = 0 }
    if (state.motionTimedOut == null) { state.motionTimedOut = false }

    sendZigbeeCommands(queryAllTuyaDP())
    scheduleFormatAttrib()
}

void updated() {
    logInfo "Updating preference settings..."
    unschedule('motionTimeout')
    
    // Re-evaluate motion state upon setting changes using raw state and timeout flags
    updateMotionFromRaw()
    
    if (settings.allStatusTextEnable == false) {
        device.deleteCurrentState('all')
    } else {
        scheduleFormatAttrib()
    }
}

// ========================================================================================================================
// Zigbee Event Parsing
// ========================================================================================================================

void parse(final String description) {
    Map descMap = myParseDescriptionAsMap(description)
    if (!descMap) return

    // Tuya MCU Cluster (0xEF00)
    if (descMap.clusterInt == 0xEF00 && (descMap.command == '01' || descMap.command == '02')) {
        parseTuyaMcuMessage(descMap)
    } 
    // Native ZCL Temperature (0x0402)
    else if (descMap.clusterInt == 0x0402 && descMap.value) {
        Float tempC = Integer.parseInt(descMap.value, 16) / 100.0f
        handleTemperatureCelsius(tempC)
    } 
    // Native ZCL Humidity (0x0405)
    else if (descMap.clusterInt == 0x0405 && descMap.value) {
        Float hum = Integer.parseInt(descMap.value, 16) / 100.0f
        handleHumidityEvent(hum)
    } 
    // Native ZCL Illuminance (0x0400)
    else if (descMap.clusterInt == 0x0400 && descMap.value) {
        int rawZcl = Integer.parseInt(descMap.value, 16)
        logDebug "Native 0400 raw = ${rawZcl}"
        
        // Standard ZCL MeasuredValue logarithmic conversion: Lux = 10^((raw - 1) / 10000)
        if (rawZcl > 0 && rawZcl != 0xFFFF) {
            int calculatedLux = Math.round(Math.pow(10, (rawZcl - 1) / 10000.0)).toInteger()
            handleIlluminanceEvent(calculatedLux)
        } else if (rawZcl == 0) {
            handleIlluminanceEvent(0)
        }
    }
}

private void parseTuyaMcuMessage(final Map descMap) {
    List<String> data = descMap.data
    int dataLen = data ? data.size() : 0

    if (dataLen < 6) return

    for (int i = 0; i + 5 < dataLen; ) {
        int dp = zigbee.convertHexToInt(data[2 + i])
        int fncmd_len = zigbee.convertHexToInt(data[5 + i])
        
        // Defensive bounds validation
        if (i + 5 + fncmd_len >= dataLen) {
            logDebug "Truncated Tuya payload detected at offset ${i}"
            break
        }

        int fncmd = getTuyaAttributeValue(data, i)
        processTuyaDp(dp, fncmd)
        
        i += fncmd_len + 4
    }
}

private void processTuyaDp(int dp, int value) {
    logDebug "Tuya DP ${dp} reported value: ${value}"
    switch (dp) {
        case 1:   // Presence / Motion
            state.rawMotion = (value > 0) ? 1 : 0
            state.motionTimedOut = false  // Reset local timeout flag on fresh report
            updateMotionFromRaw()
            break
        case 2:   // Motion sensitivity
            logDebug "Motion sensitivity reported: ${value}"
            break
        case 101: // Humidity (Tuya Fallback)
            handleHumidityEvent(value as Float)
            break
        case 102: // Fading time
            logDebug "Fading time reported: ${value}s"
            break
        case 106: // Illuminance (Tuya Fallback)
            logDebug "Tuya DP 106 raw = ${value}"
            handleIlluminanceEvent(value)
            break
        case 110: // Battery
            handleBatteryEvent(value)
            break
        case 111: // Temperature (Tuya Fallback)
            handleTemperatureCelsius(value / 10.0f)
            break
        default:
            logDebug "Unhandled Tuya DP ${dp} with value ${value}"
            break
    }
}

// ========================================================================================================================
// State Processing & Attribute Logic
// ========================================================================================================================

void updateMotionFromRaw() {
    int raw = (state.rawMotion != null) ? (state.rawMotion as int) : 0
    boolean isDeviceActive = (raw == 1)

    if (settings.invertMotion == true) {
        isDeviceActive = !isDeviceActive
    }

    // Motion is active if device indicates motion AND local timer has not timed out
    boolean finalActiveState = isDeviceActive && !(state.motionTimedOut == true)

    if (finalActiveState) {
        sendEvent(name: 'motion', value: 'active', descriptionText: "Motion is active")
        scheduleMotionTimeout()
    } else {
        unschedule('motionTimeout')
        sendEvent(name: 'motion', value: 'inactive', descriptionText: "Motion is inactive")
    }
    scheduleFormatAttrib()
}

private void scheduleMotionTimeout() {
    unschedule('motionTimeout')
    int timeout = (settings.motionTimeout != null) ? settings.motionTimeout as int : 30
    if (timeout > 0) {
        runIn(timeout, 'motionTimeout')
    }
}

void motionTimeout() {
    logInfo "Local motion timeout expired."
    state.motionTimedOut = true
    updateMotionFromRaw()
}

void handleTemperatureCelsius(Float tempC) {
    Float tempF = ((tempC * 1.8f) + 32.0f).round(1)
    sendEvent(name: 'temperature', value: tempF, unit: '°F', descriptionText: "Temperature is ${tempF} °F")
    scheduleFormatAttrib()
}

void handleHumidityEvent(Float hum) {
    sendEvent(name: 'humidity', value: hum, unit: '%RH', descriptionText: "Humidity is ${hum} %RH")
    scheduleFormatAttrib()
}

void handleIlluminanceEvent(int lux) {
    sendEvent(name: 'illuminance', value: lux, unit: 'lx', descriptionText: "Illuminance is ${lux} lx")
    scheduleFormatAttrib()
}

void handleBatteryEvent(int batt) {
    sendEvent(name: 'battery', value: batt, unit: '%', descriptionText: "Battery level is ${batt} %")
    scheduleFormatAttrib()
}

void scheduleFormatAttrib() {
    if (settings?.allStatusTextEnable == true) {
        runIn(1, 'formatAttrib', [overwrite: true])
    }
}

void formatAttrib() {
    if (settings?.allStatusTextEnable == false) return
    List<String> parts = []
    
    def motionVal = device.currentValue('motion', true)
    if (motionVal != null) parts << "motion: ${motionVal}"
    
    def battVal = device.currentValue('battery', true)
    if (battVal != null) parts << "battery: ${battVal}%"
    
    def luxVal = device.currentValue('illuminance', true)
    if (luxVal != null) parts << "illuminance: ${luxVal} lx"
    
    def tempVal = device.currentValue('temperature', true)
    if (tempVal != null) parts << "temp: ${tempVal}°F"
    
    def humVal = device.currentValue('humidity', true)
    if (humVal != null) parts << "humidity: ${humVal}%RH"

    if (parts) {
        sendEvent(name: 'all', value: parts.join(' | '), type: 'digital')
    }
}

// ========================================================================================================================
// Helper Functions
// ========================================================================================================================

Map myParseDescriptionAsMap(String description) {
    try {
        return zigbee.parseDescriptionAsMap(description)
    } catch (e) {
        return [:]
    }
}

int getTuyaAttributeValue(List<String> _data, int index) {
    if (!_data || _data.size() < index + 6) return 0
    
    int dataLength = zigbee.convertHexToInt(_data[index + 5])
    if (dataLength == 0 || _data.size() < index + 6 + dataLength) return 0

    int retValue = 0
    int power = 1
    for (int i = dataLength; i >= 1; i--) {
        retValue += power * zigbee.convertHexToInt(_data[index + 5 + i])
        power *= 256
    }
    return retValue
}

List<String> queryAllTuyaDP() {
    return zigbee.command(0xEF00, 0x03)
}

void sendZigbeeCommands(List<String> cmds) {
    if (cmds) {
        sendHubCommand(new HubMultiAction(cmds, Protocol.ZIGBEE))
    }
}

void logDebug(String msg) { if (settings.logEnable) log.debug msg }
void logInfo(String msg) { if (settings.txtEnable) log.info msg }