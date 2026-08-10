/**
 *  Tuya TS0601 TZE200_rrhgsbacq 4 in 1 Driver (Temp, Humidity, Illuminance, Motion)
 *  Target Hardware: Model TS0601 / Manufacturer _TZE200_rhgsbacq (ZG-204ZV)
 *
 *  Licensed under the Apache License, Version 2.0
 */

static String version() { "2.0.2" }
static String timeStamp() { "2026/08/09 2:10 PM" }

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
        input name: 'luxMultiplier', type: 'enum', title: '<b>Illuminance Scaling Factor</b>', options: ['0.01': 'Divide by 100', '1': 'Exact (1x)', '100': 'Multiply by 100'], defaultValue: '1', description: 'Adjust illuminance scaling if reporting factor is off.'
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
    if (state.rawMotion == null) { state.rawMotion = 0 }
    if (settings.invertMotion == null) { device.updateSetting('invertMotion', false) }
    if (settings.motionTimeout == null) { device.updateSetting('motionTimeout', 30) }
    if (settings.allStatusTextEnable == null) { device.updateSetting('allStatusTextEnable', false) }
    if (settings.luxMultiplier == null) { device.updateSetting('luxMultiplier', '1') }
    
    updateMotionFromRaw()
    sendZigbeeCommands(queryAllTuyaDP())
    scheduleFormatAttrib()
}

void updated() {
    logInfo "Updating preference settings..."
    unschedule('motionTimeout')
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
        handleTemperatureEvent(tempC)
    } 
    // Native ZCL Humidity (0x0405)
    else if (descMap.clusterInt == 0x0405 && descMap.value) {
        Float hum = Integer.parseInt(descMap.value, 16) / 100.0f
        handleHumidityEvent(hum)
    } 
    // Native ZCL Illuminance (0x0400)
    else if (descMap.clusterInt == 0x0400 && descMap.value) {
        int rawLux = Integer.parseInt(descMap.value, 16)
        handleIlluminanceEvent(rawLux)
    }
}

private void parseTuyaMcuMessage(final Map descMap) {
    int dataLen = descMap.data ? descMap.data.size() : 0
    for (int i = 0; i < (dataLen - 4); ) {
        int dp = zigbee.convertHexToInt(descMap.data[2 + i])
        int fncmd = getTuyaAttributeValue(descMap.data, i)
        int fncmd_len = zigbee.convertHexToInt(descMap.data[5 + i])
        
        processTuyaDp(dp, fncmd)
        i = i + fncmd_len + 4
    }
}

private void processTuyaDp(int dp, int value) {
    logDebug "Tuya DP ${dp} reported value: ${value}"
    switch (dp) {
        case 1:   // Presence / Motion
            state.rawMotion = (value > 0) ? 1 : 0
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
            handleIlluminanceEvent(value)
            break
        case 110: // Battery
            handleBatteryEvent(value)
            break
        case 111: // Temperature (Tuya Fallback)
            handleTemperatureEvent(value / 10.0f)
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

    if (active) {
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
    sendEvent(name: 'motion', value: 'inactive', descriptionText: "Motion timeout expired")
    scheduleFormatAttrib()
}

void handleTemperatureEvent(Float tempC) {
    Float tempF = ((tempC * 1.8f) + 32.0f).round(1)
    sendEvent(name: 'temperature', value: tempF, unit: '°F', descriptionText: "Temperature is ${tempF} °F")
    scheduleFormatAttrib()
}

void handleHumidityEvent(Float hum) {
    sendEvent(name: 'humidity', value: hum, unit: '%RH', descriptionText: "Humidity is ${hum} %RH")
    scheduleFormatAttrib()
}

void handleIlluminanceEvent(int rawLux) {
    BigDecimal mult = settings.luxMultiplier ? settings.luxMultiplier.toBigDecimal() : 1.0
    int lux = (rawLux * mult).toInteger()
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
    int retValue = 0
    if (_data.size() >= 6) {
        int dataLength = zigbee.convertHexToInt(_data[5 + index])
        if (dataLength == 0) return 0
        int power = 1
        for (i in dataLength..1) {
            retValue = retValue + power * zigbee.convertHexToInt(_data[index + i + 5])
            power = power * 256
        }
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