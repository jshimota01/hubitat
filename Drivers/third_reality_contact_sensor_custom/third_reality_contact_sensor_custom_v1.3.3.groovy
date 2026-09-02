/**
 * Third Reality Contact Sensor (Custom)
 * Platform: Hubitat Elevation
 * Notes: Custom driver for Third Reality 3RDS17BZ contact sensor with state deduplication
 * Capabilities: Contact Sensor, Battery, Configuration, Refresh, Sensor
 **/

/**
 * Copyright 2026 James Shimota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

/**
 *  Purpose:
 *  Provides contact state, battery reporting, and periodic check-in management for Third Reality Contact Sensors.
 *
 *  Instructions:
 *  1. Pair the device or assign this custom driver in Hubitat.
 *  2. Click Configure to initialize battery reporting and check-in schedules.
 *  
 *  Changelog:
 *  v1.3.3    08/28/26    jshimota    Refactored parse() to use sendIfChanged for state deduplication
 *  v1.3.2    08/28/26    jshimota    Added explicit debug logging inside battery parsing functions
 *  v1.3.1    08/27/26    jshimota    Updated default check-in interval to 12 hours and verified Hubitat 2.4 compatibility
 *  v1.3.0    08/27/26    jshimota    Improved log prefix formatting with device display name
 *  v1.2.0    08/27/26    jshimota    Added customizable activity check-in intervals (1, 3, 6, 12, 24 hours)
 *  v1.1.0    08/27/26    jshimota    Added version tracking attribute and forced state updates
 *  v1.0.0    08/27/26    jshimota    Initial release for Third Reality 3RDS17BZ contact sensor
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.
static String version() { return '1.3.3' }

import hubitat.zigbee.zcl.DataType

metadata {
    definition (
        name: "Third Reality Contact Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/dev/Drivers/third_reality_contact_sensor.groovy"
    ) {
        capability "ContactSensor"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"

        // Version Status & Attributes
        attribute "driverVersion", "string"

        // Custom Commands
        command "resetDriver"

        // Fingerprint for Third Reality 3RDS17BZ
        fingerprint profileId: "0104", inClusters: "0000,0001,0500,0003", outClusters: "0019", manufacturer: "3Reality", model: "3RDS17BZ", deviceJoinName: "Third Reality Contact Sensor (Custom)"
    }

    preferences {
        input name: "checkInInterval", type: "enum", title: "Activity Check-In Interval", options: ["1":"1 Hour", "3":"3 Hours", "6":"6 Hours", "12":"12 Hours", "24":"24 Hours"], defaultValue: "12", required: true
        
        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Parse incoming Zigbee messages
def parse(String description) {
    logDebug "Parsing description -> ${description}"
    
    Map map = [:]
    
    if (description?.startsWith("zone status")) {
        map = parseIasZoneStatus(description)
    } else if (description?.startsWith("read attr -")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        logDebug "Parsed description map -> ${descMap}"
        
        if (descMap.cluster == "0001" && descMap.attrId == "0021") {
            logDebug "Processing battery percentage attribute (0x0021)"
            map = parseBattery(descMap.value)
        }
    } else if (description?.startsWith("enroll request")) {
        logDebug "Handling IAS zone enrollment request"
        List cmds = zigbee.enrollResponse()
        return cmds
    }

    if (map) {
        // Deduplicate events before sending to hub pipeline
        sendIfChanged(map)
    }
    
    return []
}

// Handle IAS Zone Status (Contact Open/Closed events)
private Map parseIasZoneStatus(String description) {
    Integer status = zigbee.parseZoneStatus(description)?.alarm1 ? 1 : 0
    if (status == 1) {
        return [name: "contact", value: "open", descriptionText: "is open"]
    } else {
        return [name: "contact", value: "closed", descriptionText: "is closed"]
    }
}

// Parse Battery Percentage (0x0001 / 0x0021)
private Map parseBattery(String hexValue) {
    logDebug "parseBattery() raw hex -> ${hexValue}"
    Integer rawValue = Integer.parseInt(hexValue, 16)
    Integer pct = Math.round(rawValue / 2)
    pct = Math.min(100, Math.max(0, pct))
    logDebug "Calculated battery percentage -> ${pct}% (raw: ${rawValue})"
    return [name: "battery", value: pct, unit: "%", descriptionText: "battery is ${pct}%"]
}

// Scheduled check-in to maintain activity state on inactivity reports
def checkIn() {
    logDebug "Executing scheduled check-in"
    sendEvent(name: "checkIn", value: now(), displayed: false, isStateChange: true)
}

def setupSchedule() {
    unschedule("checkIn")
    String interval = checkInInterval ?: "12"
    logDebug "Setting check-in schedule interval to ${interval} hour(s)"
    switch(interval) {
        case "1":
            runEvery1Hour("checkIn")
            break
        case "3":
            runEvery3Hours("checkIn")
            break
        case "6":
            schedule("0 0 */6 ? * *", "checkIn")
            break
        case "12":
            schedule("0 0 */12 ? * *", "checkIn")
            break
        case "24":
            schedule("0 0 0 ? * *", "checkIn")
            break
        default:
            schedule("0 0 */12 ? * *", "checkIn")
            break
    }
}

// Hubitat Lifecycle Routines
void installed() {
    logInfo "Installing driver v${version()}..."
    sendEvent(name: "driverVersion", value: version())
    initialize(true)
}

void updated() {
    logInfo "Updating preferences..."
    sendEvent(name: "driverVersion", value: version())
    initialize(false)
}

def configure() {
    logInfo "Configuring device..."
    sendEvent(name: "driverVersion", value: version())
    initialize(false)

    setupSchedule()
    
    // Binding and reporting configurations
    List<String> cmds = []
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 30, 21600, 0x01) // Battery reporting
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    cmds += zigbee.enrollResponse()
    
    logDebug "configure() sending Zigbee payload -> ${cmds}"
    return cmds
}

def refresh() {
    logDebug "Refreshing battery attribute"
    return zigbee.readAttribute(0x0001, 0x0021)
}

private void initialize(Boolean isInstall = false) {
    unschedule("disableDebugLogging")
    
    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging enabled. Will automatically turn off in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }
}

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

// Master Utility Routine for Driver GUI Button
void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    logInfo "Driver reset process completed."
}

// Individual Utility Routines
void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    state.clear()
    logInfo "All states have been cleared."
}

void clearAllAttributes() {
    logInfo "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
    logInfo "All attributes have been cleared."
}

void clearAllSchedules() {
    logInfo "Clearing all scheduled jobs (including orphaned schedules)..."
    unschedule()
    logInfo "All scheduled jobs have been successfully cleared."
}

// State-De-Duplication Helper Routine
private void sendIfChanged(Map args) {
    if (!args || !args.name) return

    String nameStr = args.name as String
    String oldVal = device.currentValue(nameStr)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        Map eventMap = [
            name: nameStr, 
            value: args.value, 
            descriptionText: args.descriptionText ?: "${device.displayName} ${args.name} set to ${args.value}"
        ]
        if (args.unit) eventMap.unit = args.unit
        if (args.isStateChange != null) eventMap.isStateChange = args.isStateChange

        sendEvent(eventMap)
        logInfo "${eventMap.descriptionText}"
        logDebug "Event triggered: ${nameStr} -> ${args.value}"
    }
}

// Centralized Logging Engine
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${devName}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}