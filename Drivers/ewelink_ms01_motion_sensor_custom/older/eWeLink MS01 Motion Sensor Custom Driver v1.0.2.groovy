/**
 *  eWeLink MS01 Motion Sensor Custom Driver
 *  Target Hardware: Model MS01 / Manufacturer eWeLink
 *
 *  Licensed under the Apache License, Version 2.0
 */

static String version() { "1.0.2" }
static String timeStamp() { "2026/08/10 10:25 AM" }

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

        attribute 'all', 'string'

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0500,0001,0020", outClusters: "0003", model: "ms01", manufacturer: "eWeLink", deviceJoinName: "eWeLink Motion Sensor"
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

void initialize() {
    logInfo "Initializing sensor settings and state variables..."
    if (settings.invertMotion == null) { device.updateSetting('invertMotion', false) }
    if (settings.allStatusTextEnable == null) { device.updateSetting('allStatusTextEnable', false) }
    
    if (state.rawMotion == null) { state.rawMotion = 0 }

    refresh()
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

    // IAS Zone Cluster (0x0500) - Motion Reports
    if (descMap.clusterInt == 0x0500) {
        if (descMap.command == '00' || descMap.command == '02') {
            // Zone Status Change Notification
            int statusInt = 0
            if (descMap.value) {
                statusInt = Integer.parseInt(descMap.value, 16)
            } else if (descMap.data && descMap.data.size() >= 2) {
                // Combine Little-Endian bytes (Byte 1 << 8 | Byte 0)
                int lowByte = Integer.parseInt(descMap.data[0], 16)
                int highByte = Integer.parseInt(descMap.data[1], 16)
                statusInt = (highByte << 8) | lowByte
            }
            
            boolean isMotion = (statusInt & 1) != 0 // Bit 0 is Alarm 1 (Motion)
            state.rawMotion = isMotion ? 1 : 0
            logDebug "Received IAS Zone report: raw status mask ${statusInt} (Motion: ${isMotion})"
            updateMotionFromRaw()
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
    return zigbee.readAttribute(0x0001, 0x0021) + // Read Battery Percentage
           zigbee.readAttribute(0x0500, 0x0000)   // Read Zone State
}

void sendZigbeeCommands(List<String> cmds) {
    if (cmds) {
        List<HubAction> hubActions = cmds.collect { new HubAction(it, Protocol.ZIGBEE) }
        sendHubCommand(new HubMultiAction(hubActions))
    }
}