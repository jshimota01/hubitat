/**
 * [Driver Name]
 * Platform: Hubitat Elevation
 * Notes: [Key details, compatibility, or implementation notes]
 * Capabilities: [List primary capabilities supported by this driver]
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
 *
 *  Instructions:
 *  
 *  Changelog:
 *  v1.0.4    08/30/26    jshimota    Applied trace deduplication, null-safe location/time Zone checks, and forced isStateChange initializations
 *  v1.0.3    08/29/26    jshimota    Added setupSchedule call to updated() for automatic schedule re-evaluations
 *  v1.0.2    08/29/26    jshimota    Added updateFirmware command routine and GUI version force updates
 *  v1.0.1    08/29/26    jshimota    Added type-safe preference parsing and non-overwriting debug timers
 *  v1.0.0    08/28/26    jshimota    Initial starting code
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.0.4' }
def timeStamp() { return "2026/08/30 08:10 AM" }

metadata {
    definition(
        name: "[Driver Name]",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota/Hubitat/Drivers/[filename].groovy"
    ) {
        // Capabilities
        capability "Configuration"
        capability "Refresh"

        // Custom Attributes
        attribute "driverVersion", "string"

        // Custom Commands
        command "resetDriver"
        command "updateFirmware"
        
        // Fingerprints (if applicable)
    }

    preferences {
        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// NPE-Safe Timestamp Helper Routine
private String getTimestamp() {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)
}

// Scheduled Cron / Interval Setup Helper (Implement per driver)
def setupSchedule() {
    // Override in specific drivers to manage runEveryX or schedule() routines
}

// Hubitat Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating preferences..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
    setupSchedule()
}

// Return dynamic List for Zigbee/Z-Wave radio command transmission
def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)

    setupSchedule()
    
    // Example: Safe preference integer conversion pattern
    // Integer checkInHours = (checkInInterval ?: "12").toString().toInteger()
    
    List<String> cmds = []
    logDebug "configure() sending Zigbee/Z-Wave payload -> ${cmds}"
    return cmds
}

def refresh() {
    logDebug "Executing refresh()..."
    List<String> cmds = []
    return cmds
}

// Update Firmware Command Routine (Zigbee OTA trigger) [Optional]
List<String> updateFirmware() {
    logInfo "Checking for firmware updates..."
    return zigbee.updateFirmware()
}

private void initialize(Boolean isInstall = false) {
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
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
        String desc = args.descriptionText ?: "${nameStr} set to ${args.value}"
        Map eventMap = [
            name: nameStr, 
            value: args.value, 
            descriptionText: desc
        ]
        if (args.unit) eventMap.unit = args.unit
        if (args.type) eventMap.type = args.type
        if (args.isStateChange != null) eventMap.isStateChange = args.isStateChange

        sendEvent(eventMap)
        logTrace "${desc}"
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