/**
 *  Tuya TS0601 TZE200_rrhgsbacq 4 in 1 Driver (Temp, Humidity, Illuminance, Motion)
 *  Target Hardware: Model TS0601 / Manufacturer _TZE200_rhgsbacq (ZG-204ZV)
 *
 *  Licensed under the Apache License, Version 2.0
 */

static String version() { "2.0.4" }
static String timeStamp() { "2026/08/09 3:30 PM" }

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
        attribute 'motionKeepTime', 'number'

        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0500,EF00,0402,0405,0001,0400", outClusters:"0003", model:"TS0601", manufacturer:"_TZE200_rhgsbacq", controllerType: "ZGB"
    }

    preferences {
        input name: 'txtEnable', type: 'bool', title: '<b>Enable descriptionText logging</b>', defaultValue: true
        input name: 'logEnable', type: 'bool', title: '<b>Enable debug logging</b>', defaultValue: false
        input name: 'allStatusTextEnable', type: 'bool', title: "<b>Enable 'all' Status Attribute Creation?</b>", defaultValue: false
        input name: 'invertMotion', type: 'bool', title: '<b>Invert Motion State</b>', defaultValue: false, description: 'Invert active/inactive reported logic if reversed.'
        input name: 'motionKeepTime', type: 'number', title: '<b>Hardware Motion Keep Time (Seconds)</b>', defaultValue: 30, range: '0..28800', description: 'Sets native hardware hold time via DP 102 (Default: 30s).'
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
    if (settings.allStatusTextEnable == null) { device.updateSetting('allStatusTextEnable', false) }
    if (settings.motionKeepTime == null) { device.updateSetting('motionKeepTime', 30) }
    
    if (state.rawMotion == null) { state.rawMotion = 0 }

    sendZigbeeCommands(queryAllTuyaDP())
    scheduleFormatAttrib()
}

void updated() {
    logInfo "Updating preference settings..."
    
    // Push DP 102 (fading_time) setting to hardware if changed
    if (settings.motionKeepTime != null) {
        setMotionKeepTime(settings.motionKeepTime as int)
    }

    // Re-apply motion evaluation with inverted toggle
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
        case 1:   // Presence / Motion (0 = inactive, 1 = active)
            state.rawMotion = (value > 0) ? 1 : 0
            updateMotionFromRaw()
            break
        case 2:   // Motion sensitivity
            logDebug "Motion sensitivity reported: ${value}"
            break
        case 101: // DP 101 is presence_time in seconds (Not humidity/motion)
            logDebug "Presence duration reported (DP 101): ${value}s"
            break
        case 102: // Motion keep time / fading_time
            logInfo "Hardware motion keep time (DP 102) is ${value}s"
            sendEvent(name: 'motionKeepTime', value: value, unit: 's')
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
    boolean active = (raw == 1)

    if (settings.invertMotion == true) {
        active = !active
    }

    String motionState = active ? 'active' : 'inactive'
    sendEvent(name: 'motion', value: motionState, descriptionText: "Motion is ${motionState}")
    scheduleFormatAttrib()
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

void setMotionKeepTime(int seconds) {
    logInfo "Setting hardware motion keep time (DP 102) to ${seconds} seconds"
    sendZigbeeCommands(sendTuyaCommand("66", "02", "04", seconds))
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

List<String> sendTuyaCommand(String dpHex, String dpType, String lengthHex, int value) {
    String hexVal = hubitat.helper.HexUtils.integerToHexString(value, 4)
    List<String> cmds = [
        "he cmd 0x${device.deviceNetworkId} 0x01 0xEF00 0x00 {0001${dpHex}${dpType}00${lengthHex}${hexVal}}",
        "delay 200"
    ]
    return cmds
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