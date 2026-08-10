/**
 *  Tuya TS0601 TZE200_rrhgsbacq 4 in 1 Driver (Temp, Humidity, Illuminance, Motion)
 *  Target Hardware: Model TS0601 / Manufacturer _TZE200_rhgsbacq (ZG-204ZV)
 *
 *  Licensed under the Apache License, Version 2.0
 */

static String version() { "2.1.2" }
static String timeStamp() { "2026/08/09 5:30 PM" }

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
        input name: 'motionKeepTime', type: 'number', title: '<b>Hardware Motion Keep Time (Seconds)</b>', defaultValue: 30, range: '0..28800', description: 'Desired native hardware hold time via DP 102 (Default: 30s).'
    }
}

// ========================================================================================================================
// Hubitat Lifecycle & Commands
// ========================================================================================================================

void refresh() {
    logInfo "Requesting fresh status update from device..."
    sendZigbeeCommands(queryAllTuyaDP())
    scheduleFormatAttrib()
}

void initialize() {
    logInfo "Initializing sensor settings and state variables..."
    if (settings.invertMotion == null) { device.updateSetting('invertMotion', false) }
    if (settings.allStatusTextEnable == null) { device.updateSetting('allStatusTextEnable', false) }
    if (settings.motionKeepTime == null) { device.updateSetting('motionKeepTime', 30) }
    
    if (state.rawMotion == null) { state.rawMotion = 0 }

    refresh()
}

void updated() {
    logInfo "Device preferences updated by user."
    
    Integer newKeepTime = settings.motionKeepTime as Integer
    Integer oldKeepTime = state.lastMotionKeepTime as Integer

    if (newKeepTime != null && newKeepTime != oldKeepTime) {
        setMotionKeepTime(newKeepTime)
    }

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
    // Native ZCL Power Configuration (0x0001) - Battery Reporting
    else if (descMap.clusterInt == 0x0001 && descMap.value) {
        int rawVal = Integer.parseInt(descMap.value, 16)
        logDebug "Received standard Zigbee battery report: raw value ${rawVal} (Attribute ID: ${descMap.attrId})"
        if (descMap.attrId == "0021") {
            int battPct = Math.round(rawVal / 2.0).toInteger()
            handleBatteryEvent(battPct, "Standard Zigbee Cluster")
        } else {
            handleBatteryEvent(rawVal, "Standard Zigbee Cluster")
        }
    }
    // Native ZCL Temperature (0x0402) - Signed 16-bit Integer
    else if (descMap.clusterInt == 0x0402 && descMap.value) {
        int rawTemp = Short.parseShort(descMap.value, 16) as int
        Float tempC = rawTemp / 100.0f
        handleTemperatureCelsius(tempC)
    } 
    // Native ZCL Humidity (0x0405)
    else if (descMap.clusterInt == 0x0405 && descMap.value) {
        Float hum = Integer.parseInt(descMap.value, 16) / 100.0f
        handleHumidityEvent(hum)
    } 
    // Native ZCL Illuminance (0x0400) — Primary/Authoritative Source
    else if (descMap.clusterInt == 0x0400 && descMap.value) {
        int rawZcl = Integer.parseInt(descMap.value, 16)
        
        if (rawZcl > 0 && rawZcl < 0xFFFF) {
            int calculatedLux = Math.round(Math.pow(10, (rawZcl - 1) / 10000.0)).toInteger()
            logDebug "Received standard light level report: raw ${rawZcl} converted to ${calculatedLux} lx"
            handleIlluminanceEvent(calculatedLux)
        } else if (rawZcl == 0) {
            logDebug "Received standard light level report: total darkness (0 lx)"
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
        
        if (fncmd_len <= 0 || fncmd_len > 8) {
            logWarn "Corrupted data packet detected: invalid message length (${fncmd_len} bytes) at position ${i}. Skipping remaining packet."
            break
        }

        if (i + 5 + fncmd_len >= dataLen) {
            logWarn "Incomplete data packet received: length indicates ${i + 6 + fncmd_len} bytes required, but only ${dataLen} bytes present. Stopping parse."
            break
        }

        Integer fncmd = getTuyaAttributeValue(data, i)
        if (fncmd == null) {
            logWarn "Could not decode data point ${dp} value at position ${i}. Skipping this update."
        } else {
            processTuyaDp(dp, fncmd)
        }
        
        i += fncmd_len + 4
    }
}

private void processTuyaDp(int dp, int value) {
    logDebug "Received Tuya Data Point ${dp} with value: ${value}"
    switch (dp) {
        case 1:   // Presence / Motion
            state.rawMotion = (value > 0) ? 1 : 0
            updateMotionFromRaw()
            break
        case 2:   // Motion sensitivity
            logDebug "Motion sensitivity set on hardware: ${value}"
            break
        case 101: // Presence duration
            logDebug "Sensor reported continuous active motion duration: ${value} seconds"
            break
        case 102: // Motion keep time / fading_time
            state.lastMotionKeepTime = value
            sendEvent(name: 'motionKeepTime', value: value, unit: 's')
            
            Integer requestedTime = settings.motionKeepTime as Integer
            if (requestedTime != null && value != requestedTime) {
                logWarn "Hardware setting mismatch! Requested hold time was ${requestedTime} seconds, but device reported ${value} seconds."
            } else {
                logInfo "Hardware motion hold time successfully verified at ${value} seconds."
            }
            break
        case 106: // Illuminance — Logging only
            logDebug "Tuya secondary light level report: ${value} lx"
            break
        case 110: // Battery
            handleBatteryEvent(value, "Tuya Protocol")
            break
        case 111: // Temperature Fallback
            int rawTemp = (value > 0x7FFF) ? (value - 0x10000) : value
            handleTemperatureCelsius(rawTemp / 10.0f)
            break
        default:
            logDebug "Received unhandled Tuya Data Point ${dp} with value ${value}"
            break
    }
}

// ========================================================================================================================
// State Processing & Attribute Logic
// ========================================================================================================================

void updateMotionFromRaw() {
    int raw = (state.rawMotion != null) ? (state.rawMotion as int) : 0
    boolean isDeviceActive = (raw == 1)
    boolean isInverted = (settings.invertMotion == true)
    
    boolean finalActiveState = isInverted ? !isDeviceActive : isDeviceActive
    String motionState = finalActiveState ? 'active' : 'inactive'

    logInfo "Motion state updated: ${motionState} (Raw sensor reading: ${raw}, Inverted mode: ${isInverted ? 'enabled' : 'disabled'})"

    sendEvent(name: 'motion', value: motionState, descriptionText: "Motion is ${motionState}")
    scheduleFormatAttrib()
}

void handleTemperatureCelsius(Float tempC) {
    Float tempF = ((tempC * 1.8f) + 32.0f).round(1)
    logInfo "Temperature report: ${tempF} °F (${tempC} °C)"
    sendEvent(name: 'temperature', value: tempF, unit: '°F', descriptionText: "Temperature is ${tempF} °F")
    scheduleFormatAttrib()
}

void handleHumidityEvent(Float hum) {
    logInfo "Humidity report: ${hum} %RH"
    sendEvent(name: 'humidity', value: hum, unit: '%RH', descriptionText: "Humidity is ${hum} %RH")
    scheduleFormatAttrib()
}

void handleIlluminanceEvent(int lux) {
    logInfo "Illuminance report: ${lux} lx"
    sendEvent(name: 'illuminance', value: lux, unit: 'lx', descriptionText: "Illuminance is ${lux} lx")
    scheduleFormatAttrib()
}

void handleBatteryEvent(int rawBatt, String source = "Device") {
    int safeBattery = Math.max(0, Math.min(100, rawBatt))
    logInfo "Battery level report via ${source}: ${safeBattery}% (Raw: ${rawBatt})"
    sendEvent(name: 'battery', value: safeBattery, unit: '%', descriptionText: "Battery level is ${safeBattery} %")
    scheduleFormatAttrib()
}

void setMotionKeepTime(int requestedSeconds) {
    int clampedSeconds = Math.max(0, Math.min(28800, requestedSeconds))
    logInfo "Configuring hardware motion hold time to ${clampedSeconds} seconds..."
    sendZigbeeCommands(setTuyaDp102(clampedSeconds))
}

List<String> setTuyaDp102(int seconds) {
    String hexVal = hubitat.helper.HexUtils.integerToHexString(seconds, 4)
    List<String> cmds = [
        "he cmd 0x${device.deviceNetworkId} 0x01 0xEF00 0x00 {000166020004${hexVal}}",
        "delay 200"
    ]
    return cmds
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

Integer getTuyaAttributeValue(List<String> _data, int index) {
    if (!_data || _data.size() < index + 6) return null
    
    int dataLength = zigbee.convertHexToInt(_data[index + 5])
    if (dataLength == 0 || _data.size() < index + 6 + dataLength) return null

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
void logWarn(String msg) { log.warn msg }