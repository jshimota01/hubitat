/**
 * Third Reality Reversible Contact Sensor (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Custom driver for Third Reality 3RDS17BZ contact sensor featuring state inversion, dynamic reporting 
 * interval sync, battery percentage parsing, and IAS Zone status parsing.
 *
 * Notes:
 * Passive Health Monitoring Implementation
 * - As a battery-operated Sleepy End Device (SED), active ping polling is omitted 
 *   to preserve battery life and avoid false offline timeouts.
 * - Device health is determined passively via periodic Zigbee battery reporting 
 *   and real-time contact state events.
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
 * Changelog:
 * v1.0.1    09/11/26    jshimota    Added detailed preference description and inline red warning callout for Invert Contact Sensor State.
 * v1.0.0    09/11/26    jshimota    Initial fork release for Third Reality Reversible Contact Sensor with state inversion option.
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.0.1' }
def timeStamp() { return "2026/09/11 04:10 PM" }

import groovy.transform.Field
import hubitat.zigbee.zcl.DataType

metadata {
    definition (
        name: "Third Reality Reversible Contact Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/third_reality_reversible_contact_sensor/third_reality_reversible_contact_sensor.groovy"
    ) {
        capability "Battery"
        capability "Configuration"
        capability "ContactSensor"
        capability "Refresh"
        capability "Sensor"

        // Attributes
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "lastBatteryReport", "string"

        // Custom Commands
        command "resetDriver"
        command "updateFirmware"

        // Fingerprint for Third Reality 3RDS17BZ
        fingerprint profileId: "0104", inClusters: "0000,0001,0500,0003", outClusters: "0019", manufacturer: "3Reality", model: "3RDS17BZ", deviceJoinName: "Third Reality Reversible Contact Sensor (Custom)"
    }

    preferences {
        input name: "checkInInterval", type: "enum", title: "<b>Battery Reporting Interval</b>", options: ["1":"Every Hour", "3":"Every 3 Hours", "6":"Every 6 Hours", "12":"Every 12 Hours", "24":"Every 24 Hours"], defaultValue: "12", required: true, description: "<i>Sets the requested maximum interval for Zigbee battery percentage reporting. Actual reporting frequency is controlled by device firmware.</i>"
        
        input name: "invertContact", type: "bool", title: "<b>Invert Contact Sensor State</b>", 
            description: "<i>Reverses logical reporting state of physical contact events.<br>" +
                         "• <b>Off (Default):</b> Physical magnet separation reports <b>Open</b>; physical magnet alignment reports <b>Closed</b>.<br>" +
                         "• <b>On (Inverted):</b> Physical magnet separation reports <b>Closed</b>; physical magnet alignment reports <b>Open</b>.<br>" +
                         "<span style='color: red; font-weight: bold;'>WARNING: Enabling state inversion will flip logic across all Rule Machine rules, dashboards, security monitors (e.g., HSM), and linked automation routines evaluating this contact sensor!</span></i>", 
            defaultValue: false, required: true

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper Routine
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

// NPE-Safe Timestamp Helper Routine
private String getTimestamp() {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    
    sendEvent(name: "healthStatus", value: "unknown")
    sendEvent(name: "contact", value: "closed")

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    
    initialize(false)
    runIn(1, "configure")
}

List<String> configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    initialize(false)

    // Safely parse checkInInterval to avoid casting NPEs
    Integer checkInHours = (checkInInterval ?: "12").toString().toInteger()
    Integer maxIntervalSec = checkInHours * 3600

    List<String> cmds = []
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 30, maxIntervalSec, 0x01) // Passive Battery Heartbeat Reporting
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    cmds += zigbee.enrollResponse()

    logDebug "configure() payload -> ${cmds}"
    return cmds
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    // Ensure healthStatus attribute exists on initialize/reset
    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

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

/* =========================================================================================
   COMMAND IMPLEMENTATIONS
   ========================================================================================= */

List<String> refresh() {
    logDebug "Executing refresh()..."
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    return cmds
}

List<String> updateFirmware() {
    logInfo "Checking for firmware updates..."
    return zigbee.updateFirmware()
}

/* =========================================================================================
   ZIGBEE MESSAGE PARSING (PASSIVE HEALTH UPDATES)
   ========================================================================================= */

void parse(String description) {
    logDebug "Raw description -> ${description}"
    
    // Any incoming valid packet confirms network presence
    updateAttribute("healthStatus", "online", null, "physical")

    if (description?.startsWith("zone status")) {
        parseIasZoneStatus(description)
    } else if (description?.startsWith("read attr -")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        logDebug "Parsed description map -> ${descMap}"
        
        if (descMap.clusterInt == 0x0001 && descMap.attrInt == 0x0021) {
            logDebug "Processing battery percentage attribute (0x0021)"
            parseBattery(descMap.value)
        }
    } else if (description?.startsWith("enroll request")) {
        logDebug "Handling IAS zone enrollment request"
        sendHubCommand(new hubitat.device.HubMultiAction(zigbee.enrollResponse(), hubitat.device.Protocol.ZIGBEE))
    }
}

private void parseIasZoneStatus(String description) {
    logDebug "parseIasZoneStatus(): Processing IAS status string -> ${description}"
    Boolean isAlarm = zigbee.parseZoneStatus(description)?.alarm1
    Boolean invert = getSettingBool("invertContact", false)

    String contactState
    if (invert) {
        contactState = isAlarm ? "closed" : "open"
    } else {
        contactState = isAlarm ? "open" : "closed"
    }

    updateAttribute("contact", contactState, null, "physical")
}

private void parseBattery(String hexValue) {
    if (hexValue == null) return
    logDebug "parseBattery(): Raw hex -> ${hexValue}"
    Integer rawValue = Integer.parseInt(hexValue, 16)
    
    if (rawValue > 200) {
        logWarn "Battery report exceeds expected range: raw=${rawValue} (0x${hexValue})"
    }

    Integer pct = Math.round(rawValue / 2)
    pct = Math.min(100, Math.max(0, pct))
    logDebug "parseBattery(): Computed battery percentage -> ${pct}%"
    
    // Quietly update timestamp state without triggering info log output
    String newTimestamp = getTimestamp()
    if (device.currentValue("lastBatteryReport") != newTimestamp) {
        sendEvent(name: "lastBatteryReport", value: newTimestamp, type: "digital")
    }

    // Standard deduplicated battery attribute update (logs only on % change)
    updateAttribute("battery", pct, "%", "physical")
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllDriverStates()
    initialize(false)
    logInfo "Driver reset process completed and re-initialized."
}

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

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
    logInfo descriptionText
    sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

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