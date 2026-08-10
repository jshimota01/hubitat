/**
 *  eWeLink MS01 Motion Sensor Custom Driver
 *  Target Hardware: Model MS01 / Manufacturer eWeLink
 *
 *  Licensed under the Apache License, Version 2.0
 */

static String version() { "1.1.6" }
static String timeStamp() { "2026/08/10 11:18 AM" }

import groovy.transform.Field
import hubitat.device.HubAction
import hubitat.device.HubMultiAction
import hubitat.device.Protocol

metadata {
    definition (
        name: 'eWeLink MS01 Motion Sensor Custom Driver',
        importUrl: '',
        namespace: 'jshimota', 
        author: 'James Shimota', 
        singleThreaded: true 
    ) {
        capability 'MotionSensor'
        capability 'Battery'
        capability 'Sensor'
        capability 'Refresh'
        capability 'Initialize'
        capability 'Configuration'

        attribute 'all', 'string'
        attribute 'batteryVoltage', 'number'

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", model: "MS01", manufacturer: "eWeLink", deviceJoinName: "eWeLink MS01 Motion Sensor"
    }

    preferences {
        input name: 'logInfoEnable', type: 'bool', title: '<b>Enable descriptionText logging</b>', defaultValue: true
        input name: 'logDebugEnable', type: 'bool', title: '<b>Enable debug logging</b>', defaultValue: true
        input name: 'logTraceEnable', type: 'bool', title: '<b>Enable trace logging</b>', defaultValue: true
        input name: 'logWarnEnable', type: 'bool', title: '<b>Enable warning logging</b>', defaultValue: true
        input name: 'logErrorEnable', type: 'bool', title: '<b>Enable error logging</b>', defaultValue: true
        input name: 'allStatusTextEnable', type: 'bool', title: "<b>Enable 'all' Status Attribute Creation?</b>", defaultValue: false
        input name: 'invertMotion', type: 'bool', title: '<b>Invert Motion State</b>', defaultValue: false, description: 'Invert active/inactive reported logic if reversed.'
    }
}

// ========================================================================================================================
// Hubitat Lifecycle & Commands
// ========================================================================================================================

void refresh() {
    logInfo "Requesting fresh status update from device..."
    sendZigbeeCommands(queryStandardClusters())
    scheduleFormatAttrib()
}

void initialize() {
    logInfo "Initializing sensor settings and state variables..."
    if (settings.invertMotion == null) { device.updateSetting('invertMotion', false) }
    if (settings.allStatusTextEnable == null) { device.updateSetting('allStatusTextEnable', false) }
    
    if (state.rawMotion == null) { state.rawMotion = 0 }

    configure()
}

void configure() {
    logInfo "Configuring MS01 IAS Zone target and battery reporting..."

    List cmds = []
    String hubIeee = swapEndianHex(device.hub.zigbeeId)

    // 1. Set Hub IEEE Address as IAS CIE Target (Attribute 0x0010, Type 0xF0)
    cmds += "he wattr 0x${device.deviceNetworkId} 0x01 0x0500 0x0010 0xF0 {${hubIeee}}"
    cmds += "delay 250"

    // 2. Send IAS Zone Enroll Response (Command 0x00) -> [00 = Success, 00 = Zone ID]
    cmds += "he raw 0x${device.deviceNetworkId} 1 0x01 0x0500 {05 00 00 00 00}"
    cmds += "delay 250"

    // 3. Configure Battery Reporting (Cluster 0x0001, Attr 0x0021, Min 30s, Max 6h, Delta 1%)
    cmds += zigbee.configureReporting(0x0001, 0x0021, 0x20, 30, 21600, 0x01)
    cmds += "delay 250"

    // 4. Query Initial Attributes
    cmds += queryStandardClusters()

    sendZigbeeCommands(cmds)
}

void updated() {
    logInfo "Device preferences updated by user."

    if (settings.logDebugEnable) {
        runIn(1800, 'disableDebugLogging')
    } else {
        unschedule('disableDebugLogging')
    }

    if (settings.allStatusTextEnable == false) {
        device.deleteCurrentState('all')
    }

    updateMotionFromRaw()
}

// ========================================================================================================================
// Zigbee Event Parsing
// ========================================================================================================================

void parse(final String description) {
    Map descMap = myParseDescriptionAsMap(description)
    if (!descMap) return

    // Diagnostic logging to inspect raw MS01 traffic
    logTrace "RAW MS01: ${description}"
    logTrace "PARSED MS01: ${descMap}"

    // IAS Zone Cluster (0x0500)
    if (descMap.clusterInt == 0x0500) {
        logTrace "IAS Zone message: command=${descMap.command}, attrId=${descMap.attrId}, value=${descMap.value}, data=${descMap.data}"

        // IAS Zone State attribute (0x0000) - Value 01 means the device is enrolled.
        if (descMap.attrId == "0000" && descMap.value) {
            int zoneState = Integer.parseInt(descMap.value, 16)
            if (zoneState == 1) {
                logInfo "MS01 IAS Zone is enrolled."
            } else {
                logWarn "MS01 IAS Zone enrollment state: ${zoneState}"
            }
            return
        }

        // IAS Zone Type (0x0001)
        if (descMap.attrId == "0001") {
            logTrace "MS01 IAS Zone Type report: ${descMap.value}"
            return
        }

        // Actual IAS Zone Status Change Notification (Cluster-specific Command 00)
        if (descMap.command == "00") {
            logDebug "MS01 IAS Zone Status Change Notification received."

            int statusInt = 0

            if (descMap.data && descMap.data.size() >= 2) {
                int lowByte = Integer.parseInt(descMap.data[0], 16)
                int highByte = Integer.parseInt(descMap.data[1], 16)
                statusInt = (highByte << 8) | lowByte
            } else if (descMap.value) {
                statusInt = Integer.parseInt(descMap.value, 16)
            }

            boolean isMotion = (statusInt & 0x0001) != 0

            state.rawMotion = isMotion ? 1 : 0

            logInfo "MS01 IAS Zone status = 0x${Integer.toHexString(statusInt)} -> motion=${isMotion}"

            updateMotionFromRaw()
            return
        }

        logTrace "Unhandled MS01 IAS Zone message: ${descMap}"
    }
    // Power Configuration Cluster (0x0001) - Battery Reporting
    else if (descMap.clusterInt == 0x0001 && descMap.value) {
        // Attr 0x0021: Battery Percentage Remaining (200 = 100%)
        if (descMap.attrId == "0021") {
            int rawVal = Integer.parseInt(descMap.value, 16)
            int battPct = Math.round(rawVal / 2.0) as int
            handleBatteryEvent(battPct, "Battery Percentage Report")
        } 
        // Attr 0x0020: Battery Voltage (Decivolts, e.g., 30 = 3.0V)
        else if (descMap.attrId == "0020") {
            int rawVoltage = Integer.parseInt(descMap.value, 16)
            BigDecimal voltage = rawVoltage / 10.0G
            logDebug "Received battery voltage report: ${voltage} V"
            
            if (sendIfChanged(name: 'batteryVoltage', value: voltage, unit: 'V')) {
                logInfo "Battery Voltage updated to ${voltage} V"
            }
            scheduleFormatAttrib()
        } else {
            logTrace "Ignored unhandled Power Configuration attribute ID: ${descMap.attrId}"
        }
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

    logDebug "Evaluating motion reading: raw=${raw}, inverted=${isInverted} -> state=${motionState}"

    sendIfChanged(name: 'motion', value: motionState)
    scheduleFormatAttrib()
}

void handleBatteryEvent(int rawBatt, String source = "Device") {
    int safeBattery = Math.max(0, Math.min(100, rawBatt))
    String logMsg = "Battery level update via ${source}: ${safeBattery}%"
    
    if (source.contains("Battery")) {
        logTrace logMsg
    } else {
        logInfo logMsg
    }
    
    sendIfChanged(name: 'battery', value: safeBattery, unit: '%')
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

    def voltVal = device.currentValue('batteryVoltage', true)
    if (voltVal != null) parts << "voltage: ${voltVal}V"

    if (parts) {
        sendIfChanged(name: 'all', value: parts.join(' | '))
    }
}

// ========================================================================================================================
// Custom Logging Helper Methods
// ========================================================================================================================

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    device.updateSetting("logDebugEnable", [type: "bool", value: false])
}

private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] != true) return

    String prefix = "${device.displayName}"
    if (level == 'warn') {
        prefix += " WARNING"
    } else if (level == 'error') {
        prefix += " ERROR"
    }

    String text = "${prefix}: ${msg}"

    switch (level) {
        case 'info':
            log.info text
            break
        case 'debug':
            log.debug text
            break
        case 'trace':
            log.trace text
            break
        case 'warn':
            log.warn text
            break
        case 'error':
            log.error text
            break
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

// ========================================================================================================================
// Attribute Event Filter Helper Method
// ========================================================================================================================

private boolean sendIfChanged(Map args) {
    if (!args || !args.name) return false
    
    String attrName = args.name as String
    def newVal = args.value
    
    if (newVal == null) {
        if (device.currentValue(attrName) != null) {
            sendEvent(name: attrName, value: null)
            logDebug "Attribute ${attrName} reset to null"
            return true
        }
        return false
    }

    String oldValStr = device.currentValue(attrName)?.toString()
    String newValStr = newVal.toString()

    if (oldValStr != newValStr) {
        Map eventMap = [name: attrName, value: newVal, descriptionText: "Attribute ${attrName} changed to ${newVal}"]
        if (args.unit) eventMap.unit = args.unit
        sendEvent(eventMap)
        logDebug "Event triggered: ${attrName} -> ${newVal}"
        
        if (["motion", "battery", "batteryVoltage"].contains(attrName)) {
            logInfo "${attrName.capitalize()} updated to ${newVal}${args.unit ? ' ' + args.unit : ''}"
        }
        return true
    }
    return false
}

// ========================================================================================================================
// Helper Functions
// ========================================================================================================================

private String swapEndianHex(String hex) {
    if (!hex) return ''

    String cleanHex = hex.replaceAll(/\s+/, '').toLowerCase()

    if ((cleanHex.length() % 2) != 0) {
        logWarn "Cannot swap endian hex with odd-length value: ${hex}"
        return cleanHex
    }

    return cleanHex.findAll(/../).reverse().join()
}

Map myParseDescriptionAsMap(String description) {
    try {
        return zigbee.parseDescriptionAsMap(description)
    } catch (e) {
        return [:]
    }
}

List queryStandardClusters() {
    // Read Battery Percentage (0x0021) and Battery Voltage (0x0020)
    return zigbee.readAttribute(0x0001, 0x0021) + 
           zigbee.readAttribute(0x0001, 0x0020)
}

void sendZigbeeCommands(List cmds) {
    if (!cmds) return
    List<String> stringCmds = []
    
    cmds.each { cmd ->
        if (cmd instanceof String) {
            stringCmds << cmd
        } else if (cmd != null) {
            stringCmds << cmd.toString()
        }
    }
    
    if (stringCmds) {
        sendHubCommand(new HubMultiAction(stringCmds, Protocol.ZIGBEE))
    }
}