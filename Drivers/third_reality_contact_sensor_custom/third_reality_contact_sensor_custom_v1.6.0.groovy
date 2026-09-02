/**
 * Third Reality Contact Sensor (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Custom driver for Third Reality 3RDS17BZ contact sensor featuring dynamic reporting 
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
 * v1.6.1    08/31/26    jshimota    Stripped active ping/pong health check routines in favor of passive Sleepy End Device (SED) heartbeat monitoring.
 * v1.6.0    08/31/26    jshimota    Applied Template v1.0.10: Consolidated version state tracking and header Notes block.
 * v1.5.4    08/29/26    jshimota    Added setupSchedule call to updated() so preference changes immediately update schedule.[cite: 3]
 * v1.5.3    08/29/26    jshimota    Removed redundant firmware attribute to clean up device UI.[cite: 3]
 * v1.5.2    08/29/26    jshimota    Applied native zigbee.updateFirmware routine from Power Plug driver.[cite: 3]
 * v1.5.1    08/29/26    jshimota    Corrected metadata capability string from FirmwareUpdate to Firmware.[cite: 3]
 * v1.5.0    08/29/26    jshimota    Replaced getFirmware with FirmwareUpdate capability and updateFirmware routine.[cite: 3]
 * v1.4.2    08/29/26    jshimota    Removed blank lines between header comment blocks.[cite: 3]
 * v1.4.1    08/29/26    jshimota    Forced isStateChange on driverVersion events to guarantee GUI updates.[cite: 3]
 * v1.4.0    08/29/26    jshimota    Applied template v1.0.1 fixes (safe preference casting, non-overwriting debug schedule, state cleanup).[cite: 3]
 * v1.3.9    08/29/26    jshimota    Applied master template structure, standardized header block, and normalized lifecycle logging.[cite: 3]
 * v1.3.8    08/29/26    jshimota    Synced configureReporting with checkInInterval preference and added getFirmware.[cite: 3]
 * v1.3.7    08/28/26    jshimota    Standardized changelog format to tabbed MM/DD/YY columns without parentheses.[cite: 3]
 * v1.3.6    08/28/26    jshimota    Applied master template v1.0.0 (def timeStamp(), return command signature updates, getTimestamp NPE safeguard).[cite: 3]
 * v1.3.5    08/28/26    jshimota    Added lastBatteryReport timestamp attribute with NPE safeguards.[cite: 3]
 * v1.3.4    08/28/26    jshimota    Aligned header comment formatting, metadata structure, and changelog layout with MS01 driver.[cite: 3]
 * v1.3.3    08/28/26    jshimota    Refactored parse() to use sendIfChanged for state deduplication.[cite: 3]
 * v1.3.2    08/28/26    jshimota    Added explicit debug logging inside battery parsing functions.[cite: 3]
 * v1.3.1    08/27/26    jshimota    Updated default check-in interval to 12 hours and verified Hubitat 2.4 compatibility.[cite: 3]
 * v1.3.0    08/27/26    jshimota    Improved log prefix formatting with device display name.[cite: 3]
 * v1.2.0    08/27/26    jshimota    Added customizable activity check-in intervals (1, 3, 6, 12, 24 hours).[cite: 3]
 * v1.1.0    08/27/26    jshimota    Added version tracking attribute and forced state updates.[cite: 3]
 * v1.0.0    08/27/26    jshimota    Initial release for Third Reality 3RDS17BZ contact sensor.[cite: 3]
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.6.1' }
def timeStamp() { return "2026/08/31 09:00 PM" }

import groovy.transform.Field
import hubitat.zigbee.zcl.DataType

metadata {
    definition (
        name: "Third Reality Contact Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/third_reality_contact_sensor/third_reality_contact_sensor.groovy"
    ) {
        capability "Actuator"
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
        fingerprint profileId: "0104", inClusters: "0000,0001,0500,0003", outClusters: "0019", manufacturer: "3Reality", model: "3RDS17BZ", deviceJoinName: "Third Reality Contact Sensor (Custom)"[cite: 3]
    }

    preferences {
        input name: "checkInInterval", type: "enum", title: "<b>Battery Check-In Interval</b>", options: ["1":"Every Hour", "3":"Every 3 Hours", "6":"Every 6 Hours", "12":"Every 12 Hours", "24":"Every 24 Hours"], defaultValue: "12", required: true, description: "<i>Sets the maximum interval between passive battery reporting heartbeats.</i>"
        
        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true[cite: 3]
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true[cite: 3]
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true[cite: 3]
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true[cite: 3]
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true[cite: 3]
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
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()[cite: 3]
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)[cite: 3]
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
    Integer checkInHours = (checkInInterval ?: "12").toString().toInteger()[cite: 3]
    Integer maxIntervalSec = checkInHours * 3600[cite: 3]

    List<String> cmds = []
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 30, maxIntervalSec, 0x01) // Passive Battery Heartbeat Reporting[cite: 3]
    cmds += zigbee.readAttribute(0x0001, 0x0021)[cite: 3]
    cmds += zigbee.enrollResponse()[cite: 3]

    logDebug "configure() payload -> ${cmds}"[cite: 3]
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
        device.updateSetting("logDebugEnable", [type: "bool", value: true])[cite: 3]
        logInfo "Debug logging enabled for 30 minutes."[cite: 3]
        runIn(1800, "disableDebugLogging")[cite: 3]
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."[cite: 3]
        runIn(1800, "disableDebugLogging", [overwrite: false])[cite: 3]
    } else {
        unschedule("disableDebugLogging")[cite: 3]
    }
}

/* =========================================================================================
   COMMAND IMPLEMENTATIONS
   ========================================================================================= */

List<String> refresh() {
    logDebug "Executing refresh()..."[cite: 3]
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0001, 0x0021)[cite: 3]
    return cmds
}

List<String> updateFirmware() {
    logInfo "Checking for firmware updates..."[cite: 3]
    return zigbee.updateFirmware()[cite: 3]
}

/* =========================================================================================
   ZIGBEE MESSAGE PARSING (PASSIVE HEALTH UPDATES)
   ========================================================================================= */

void parse(String description) {
    logDebug "Raw description -> ${description}"[cite: 3]
    
    // Any incoming valid packet confirms network presence
    updateAttribute("healthStatus", "online", null, "physical")

    if (description?.startsWith("zone status")) {[cite: 3]
        parseIasZoneStatus(description)[cite: 3]
    } else if (description?.startsWith("read attr -")) {[cite: 3]
        Map descMap = zigbee.parseDescriptionAsMap(description)[cite: 3]
        logDebug "Parsed description map -> ${descMap}"[cite: 3]
        
        if (descMap.clusterInt == 0x0001 && descMap.attrInt == 0x0021) {
            logDebug "Processing battery percentage attribute (0x0021)"[cite: 3]
            parseBattery(descMap.value)[cite: 3]
        }
    } else if (description?.startsWith("enroll request")) {[cite: 3]
        logDebug "Handling IAS zone enrollment request"[cite: 3]
        sendHubCommand(new hubitat.device.HubMultiAction(zigbee.enrollResponse(), hubitat.device.Protocol.ZIGBEE))
    }
}

private void parseIasZoneStatus(String description) {
    logDebug "parseIasZoneStatus(): Processing IAS status string -> ${description}"[cite: 3]
    Integer status = zigbee.parseZoneStatus(description)?.alarm1 ? 1 : 0[cite: 3]

    if (status == 1) {
        updateAttribute("contact", "open", null, "physical")
    } else {
        updateAttribute("contact", "closed", null, "physical")
    }
}

private void parseBattery(String hexValue) {
    if (hexValue == null) return
    logDebug "parseBattery(): Raw hex -> ${hexValue}"[cite: 3]
    Integer rawValue = Integer.parseInt(hexValue, 16)[cite: 3]
    Integer pct = Math.round(rawValue / 2)[cite: 3]
    pct = Math.min(100, Math.max(0, pct))[cite: 3]
    logDebug "parseBattery(): Computed battery percentage -> ${pct}%"[cite: 3]
    
    updateAttribute("lastBatteryReport", getTimestamp(), null, "digital")
    updateAttribute("battery", pct, "%", "physical")
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void resetDriver() {
    logInfo "Starting full driver reset..."[cite: 3]
    clearAllSchedules()[cite: 3]
    clearAllAttributes()[cite: 3]
    clearAllDriverStates()[cite: 3]
    initialize(false)
    logInfo "Driver reset process completed and re-initialized."
}

void clearAllDriverStates() {
    logInfo "Clearing all driver states..."[cite: 3]
    state.clear()[cite: 3]
    logInfo "All states have been cleared."[cite: 3]
}

void clearAllAttributes() {
    logInfo "Clearing all attributes..."[cite: 3]
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }[cite: 3]
    logInfo "All attributes have been cleared."[cite: 3]
}

void clearAllSchedules() {
    logInfo "Clearing all scheduled jobs (including orphaned schedules)..."[cite: 3]
    unschedule()[cite: 3]
    logInfo "All scheduled jobs have been successfully cleared."[cite: 3]
}

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
    logInfo descriptionText
    sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {[cite: 3]
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."[cite: 3]
        device.updateSetting("logDebugEnable", [type: "bool", value: false])[cite: 3]
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"[cite: 3]
    String devName = device.displayName ?: "Device Driver"[cite: 3]
    String settingKey = "log${lowerLevel.capitalize()}Enable"[cite: 3]
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])[cite: 3]

    if (getSettingBool(settingKey, defaultEnabled)) {[cite: 3]
        log."${lowerLevel}" "${devName}: ${msg}"[cite: 3]
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }[cite: 3]
private void logDebug(String msg) { logMessage("debug", msg) }[cite: 3]
private void logTrace(String msg) { logMessage("trace", msg) }[cite: 3]
private void logWarn(String msg)  { logMessage("warn", msg) }[cite: 3]
private void logError(String msg) { logMessage("error", msg) }[cite: 3]

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal[cite: 3]
}