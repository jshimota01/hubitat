/**
 * Zone Motion Advanced Child Device (Virtual Motion Driver)
 * Platform: Hubitat Elevation (v2.4.4.156)
 * Purpose: Virtual Motion Sensor driver used by Zone Motion Advanced Child App.
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
 * Purpose:
 * Represents the aggregated virtual motion status for a Zone Motion Advanced zone.
 *
 * Changelog:
 *  v1.0.8    10/02/26    jshimota    Suppressed verbose lifecycle Info logging; moved sendIfChanged descriptions to Debug level; consolidated driver reset messaging.
 *  v1.0.7    10/02/26    jshimota    Set default for Warning logging to false; synchronized versioning across suite release.
 *  v1.0.6    10/02/26    jshimota    Optimized lifecycle execution by removing redundant checkAndLogVersionDemarcation() call from initializeRoutine().
 *  v1.0.5    10/02/26    jshimota    Guaranteed motion baseline restoration on resetDriver().
 *  v1.0.4    10/02/26    jshimota    Removed synthetic health check scheduling engine; strictly isolated lastActivity to motion events.
 *  v1.0.3    10/02/26    jshimota    Synchronized version number across suite release.
 *  v1.0.2    10/02/26    jshimota    Fixed Groovy compilation errors by separating capability initialize() from private helper routine.
 *  v1.0.1    10/02/26    jshimota    Standardized to custom driver template (added Refresh, Initialize, Configuration, Health Check, resetDriver, sendIfChanged, version attributes, healthStatus, lastActivity, and independent logging switches).
 *  v1.0.0    10/01/26    jshimota    Initial release.
 **/

static String version() { return '1.0.8' }
def timeStamp() { return "2026/10/02 05:00 PM" }

metadata {
    definition(
        name: "Zone Motion Advanced Child Device",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/zone_motion_advanced_child_device/zone_motion_advanced_child_device.groovy"
    ) {
        capability "Motion Sensor"
        capability "Sensor"
        capability "Actuator"
        capability "Configuration"
        capability "Initialize"
        capability "Refresh"

        // Attributes
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "lastActivity", "string"
        attribute "driverVersion", "string"

        // Custom Commands
        command "setActive"
        command "setInactive"
        command "resetDriver"
    }

    preferences {
        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
        sendIfChanged("driverVersion", currentVer)
    }
}

void parse(String description) {
    logDebug "parse(): ${description}"
}

def refresh() {
    logDebug "refresh() requested"
    sendIfChanged("healthStatus", "online")
    return []
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logDebug "Installing driver v${version()} (${timeStamp()})..."
    
    sendIfChanged("healthStatus", "online")
    sendIfChanged("motion", "inactive")

    initializeRoutine(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logDebug "Preferences updated"
    
    initializeRoutine(false)
}

def configure() {
    checkAndLogVersionDemarcation()
    logDebug "Configuring device..."
    initializeRoutine(false)
    return []
}

void initialize() {
    checkAndLogVersionDemarcation()
    logDebug "Initialize capability command triggered"
    initializeRoutine(false)
}

private void initializeRoutine(Boolean isInstall) {
    unschedule("disableDebugLogging")

    sendIfChanged("healthStatus", "online")
    sendIfChanged("driverVersion", version())

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logDebug "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logDebug "Debug logging enabled. Will automatically turn off in 30 minutes."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

/* =========================================================================================
   MOTION COMMAND ROUTINES
   ========================================================================================= */

void setActive() {
    if (device.currentValue("motion") != "active") {
        logInfo "${device.displayName} motion is active"
        sendIfChanged("motion", "active", null, null, true)
    }
    updateLastActivity()
}

void setInactive() {
    if (device.currentValue("motion") != "inactive") {
        logInfo "${device.displayName} motion is inactive"
        sendIfChanged("motion", "inactive", null, null, true)
    }
    updateLastActivity()
}

private void updateLastActivity() {
    String nowFormatted = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendIfChanged("lastActivity", nowFormatted)
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

void resetDriver() {
    logDebug "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    sendIfChanged("motion", "inactive")
    initializeRoutine(false)
    logInfo "Driver reset completed."
}

void clearAllDriverStates() {
    logDebug "Clearing all driver states..."
    state.clear()
}

void clearAllAttributes() {
    logDebug "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
}

void clearAllSchedules() {
    logDebug "Clearing all scheduled jobs..."
    unschedule()
}

private void sendIfChanged(String name, Object value, String unit = null, String type = null, Boolean isStateChange = false, String desc = null) {
    String currentVal = device.currentValue(name)?.toString()
    if (currentVal != value?.toString() || isStateChange) {
        String descriptionText = desc ?: "${device.displayName} - ${name} was set to ${value}${unit ?: ''}"
        logDebug descriptionText
        sendEvent(name: name, value: value, unit: unit, type: type, isStateChange: isStateChange, descriptionText: descriptionText)
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "error"])

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