/**
 *  eWeLink MS01 Motion Sensor Custom Driver
 *  Target Hardware: Model MS01 / Manufacturer eWeLink
 *
 *  Licensed under the Apache License, Version 2.0
 */

static String version() { "1.0.4" }
static String timeStamp() { "2026/08/10 10:46 AM" }

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

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", model: "MS01", manufacturer: "eWeLink", deviceJoinName: "eWeLink MS01 Motion Sensor"
    }

    preferences {
        input name: 'logInfoEnable', type: 'bool', title: '<b>Enable descriptionText logging</b>', defaultValue: true
        input name: 'logDebugEnable', type: 'bool', title: '<b>Enable debug logging</b>', defaultValue: false
        input name: 'logTraceEnable', type: 'bool', title: '<b>Enable trace logging</b>', defaultValue: false
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

void configure() {
    logInfo "Configuring device bindings, reporting intervals, and IAS CIE Address..."
    
    List cmds = []

    // 1. Write Hub IEEE Address to IAS CIE (Cluster 0x0500, Attr 0x0010, Type 0x20)
    cmds += zigbee.writeAttribute(0x0500, 0x0010, 0x20, location.hub.zigbeeEui)

    // 2. Send IAS Zone Enroll Response
    cmds += "he raw ${device.deviceNetworkId} 1 0x01 0x0500 {00 00 00 00}"
    cmds += "delay 200"

    // 3. Bind Power Configuration Cluster (0x0001) to Hub via ZDO Bind
    cmds += "zdo bind ${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}"
    cmds += "delay 200"

    // 4. Bind IAS Zone Cluster (0x0500) to Hub via ZDO Bind
    cmds += "zdo bind ${device.deviceNetworkId} 0x01 0x01 0x0500 {${device.zigbeeId}} {}"
    cmds += "delay 200"

    // 5. Configure Battery Reporting (Cluster 0x0001, Attr 0x0021 - Battery Percentage)
    // Min interval: 30s, Max interval: 21600s (6 hrs), Reportable Change: 0x01 (1%)
    cmds += zigbee.configureReporting(0x0001, 0x0021, 0x20, 30, 21600, 0x01)

    // 6. Query initial battery state
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

    // Temporary diagnostic logging to inspect exact MS01 traffic
    logTrace "RAW MS01: ${description}"
    logTrace "PARSED MS01: ${descMap}"

    // IAS Zone Cluster (0x0500) - Motion Reports & Status Updates
    if (descMap.clusterInt == 0x0500) {
        // IAS Zone Auto-Enrollment Request
        if (descMap.command == '01') {
            logInfo "IAS Zone Enroll Request received. Replying with Enroll Response..."
            List<String> enrollCmds = [
                "he raw ${device.deviceNetworkId} 1 0x01 0x0500 {00 00 00 00}",
                "delay 200"
            ]
            sendZigbeeCommands(enrollCmds)
            return
        }

        logTrace "IAS Zone (0x0500) payload received -> command: ${descMap.command}, attrId: ${descMap.attrId}, value: ${descMap.value}, data: ${descMap.data}"

        int statusInt = -1

        if (descMap.value) {
            statusInt = Integer.parseInt(descMap.value, 16)
        } else if (descMap.data && descMap.data.size() >= 2) {
            // Combine Little-Endian bytes (Byte 1 << 8 | Byte 0)
            int lowByte = Integer.parseInt(descMap.data[0], 16)
            int highByte = Integer.parseInt(descMap.data[1], 16)
            statusInt = (highByte << 8) | lowByte
        }

        if (statusInt != -1) {
            boolean isMotion = (statusInt & 1) != 0 // Bit 0 is Alarm 1 (Motion)
            state.rawMotion = isMotion ? 1 : 0
            logDebug "Processed IAS Zone status: raw mask 0x${Integer.toHexString(statusInt)} (${statusInt}) -> Motion: ${isMotion}"
            updateMotionFromRaw()
        } else {
            logWarn "Unhandled IAS Zone message structure: ${descMap}"
        }
    }
    // Native ZCL Power Configuration (0x0001) - Battery Reporting
    else if (descMap.clusterInt == 0x0001 && descMap.value) {
        int rawVal = Integer.parseInt(descMap.value, 16)
        logDebug "Received standard Zigbee battery report: raw value ${rawVal} (Attribute ID: ${descMap.attrId})"
        if (descMap.attrId == "0021") {
            int battPct = Math.round(rawVal / 2.0) as int
            handleBatteryEvent(battPct, "Standard Zigbee Cluster")
        } else {
            handleBatteryEvent(rawVal, "Standard Zigbee Cluster")
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
    String logMsg = "Battery level report via ${source}: ${safeBattery}% (Raw: ${rawBatt})"
    
    if (source.contains("Standard Zigbee Cluster")) {
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
// Attribute Event Filter Helper Method with Deduplication and Delta Enforcement
// ========================================================================================================================

private BigDecimal getDeltaThreshold(String attrName) {
    return 0.0G
}

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
        
        if (["motion", "battery"].contains(attrName)) {
            logInfo "${attrName.capitalize()} updated to ${newVal}${args.unit ? ' ' + args.unit : ''}"
        }
        return true
    }
    return false
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

List<String> queryStandardClusters() {
    // Read Battery Percentage (0x0021) and Battery Voltage (0x0020)
    return zigbee.readAttribute(0x0001, 0x0021) + 
           zigbee.readAttribute(0x0001, 0x0020)
}

void sendZigbeeCommands(List cmds) {
    if (cmds) {
        List<HubAction> hubActions = cmds.collect { cmd ->
            if (cmd instanceof HubAction) {
                return (HubAction) cmd
            } else if (cmd instanceof String) {
                return new HubAction(cmd, Protocol.ZIGBEE)
            }
            return null
        }.findAll { it != null }

        sendHubCommand(new HubMultiAction(hubActions))
    }
}