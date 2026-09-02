/**
 * Third Reality Motion Sensor (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Custom driver for Third Reality 3RMS16BZ Zigbee motion sensor featuring IAS Zone status 
 * parsing, motion deduplication, battery voltage calculation, and passive network health tracking.
 *
 * Notes:
 * Passive Health Monitoring Implementation
 * - As a battery-operated Sleepy End Device (SED), active ping polling is omitted 
 *   to preserve battery life and avoid false offline timeouts.
 * - Device health is determined passively upon successful processing of periodic 
 *   Zigbee battery reports and real-time IAS Zone motion events.
 *
 * Battery Percentage Policy
 * - Uses a linear voltage-span mapping algorithm between the user-configured minimum 
 *   operating voltage (default 2.1V) and 3.0V nominal max.
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
 * v1.3.1    08/31/26    jshimota    Fixed 0% battery clamping floor bug, moved passive healthStatus updates into validated parser branches, and cleaned unused Field import.
 * v1.3.0    08/31/26    jshimota    Applied Template v1.0.10 for SED devices: Removed presence polling/ping scheduling overhead, added passive healthStatus tracking, standardized logging engine, and refactored battery calculation logic.
 * v1.2.7    06/20/26    jshimota    Further fix double activity reporting.
 * v1.2.6    06/12/26    jshimota    Added updateFirmware command routine.
 * v1.2.5    06/12/26    jshimota    Fixed double reporting issue by separating IAS Zone Status parsing from cluster 0500 parsing.
 * v1.2.4    06/12/26    jshimota    Optimized logic, fixed scoping, fixed bitwise operator bugs, cleaned up battery processing.
 * v1.2.3    03/25/23    tmaster     Added low bat setting.
 * v1.2.2    01/29/23    tmaster     Changed anti dupe routines to adding State change.
 * v1.2.1    01/23/23    tmaster     Power up init rewritten.
 * v1.2.0    12/14/22    tmaster     Battery code rewrite.
 * v1.1.0    12/11/22    tmaster     Working release.
 * v1.0.0    12/10/22    tmaster     Initial release for Third Reality 3RMS16BZ motion sensor.
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.3.1' }
def timeStamp() { return "2026/08/31 01:00 PM" }

import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
import java.math.RoundingMode

metadata {
    definition (
        name: "Third Reality Motion Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/third_reality_motion_sensor/third_reality_motion_sensor.groovy"
    ) {
        capability "Battery"
        capability "Configuration"
        capability "MotionSensor"
        capability "Refresh"
        capability "Sensor"

        // Attributes
        attribute "batteryVoltage", "number"
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "lastBatteryReport", "string"

        // Custom Commands
        command "resetDriver"
        command "updateFirmware"

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0001,0500", outClusters: "0019", model: "3RMS16BZ", manufacturer: "Third Reality, Inc", deviceJoinName: "Third Reality Motion Sensor (Custom)"
    }

    preferences {
        input name: "minVolts", type: "enum", title: "<b>Minimum Operating Voltage</b>", options: ["1.8":"1.8 V", "2.0":"2.0 V", "2.1":"2.1 V", "2.2":"2.2 V", "2.3":"2.3 V", "2.4":"2.4 V"], defaultValue: "2.1", required: true, description: "<i>Sets the minimum threshold voltage used to calculate linear battery percentage (0-100%).</i>"

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
    sendEvent(name: "motion", value: "inactive")

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
    logInfo "Configuring device reporting..."
    initialize(false)

    List<String> cmds = []
    
    // Configure reporting for Battery Voltage (Cluster 0x0001, Attr 0x0020) every 30s to 12h
    cmds += zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 30, 43200, 0x01)
    
    // Configure IAS Zone Status reporting (Cluster 0x0500, Attr 0x0002)
    cmds += zigbee.configureReporting(0x0500, 0x0002, DataType.BITMAP16, 0, 3600, null)
    
    // Request attribute reads to sync current device state
    cmds += zigbee.readAttribute(0x0001, 0x0020)
    cmds += zigbee.readAttribute(0x0000, 0x0004) // Manufacturer
    cmds += zigbee.readAttribute(0x0000, 0x0005) // Model
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
    cmds += zigbee.readAttribute(0x0001, 0x0020) // Battery Voltage
    cmds += zigbee.readAttribute(0x0500, 0x0002) // IAS Zone Status
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
    if (!description) return

    if (description.startsWith("zone status")) {
        updateAttribute("healthStatus", "online", null, "physical")
        parseIasZoneStatus(description)
        return
    }

    if (description.startsWith("enroll request")) {
        updateAttribute("healthStatus", "online", null, "physical")
        logDebug "Handling IAS zone enrollment request"
        sendHubCommand(new hubitat.device.HubMultiAction(zigbee.enrollResponse(), hubitat.device.Protocol.ZIGBEE))
        return
    }

    Map descMap = zigbee.parseDescriptionAsMap(description)
    if (!descMap) return
    logTrace "Parsed description map -> ${descMap}"

    // Basic Cluster (0x0000)
    if (descMap.clusterInt == 0x0000) {
        if (descMap.attrInt == 0x0004 && descMap.value) {
            updateAttribute("healthStatus", "online", null, "physical")
            updateDataValue("manufacturer", descMap.value)
            logDebug "Manufacturer identified: ${descMap.value}"
        } else if (descMap.attrInt == 0x0005 && descMap.value) {
            updateAttribute("healthStatus", "online", null, "physical")
            updateDataValue("model", descMap.value)
            logDebug "Model identified: ${descMap.value}"
        }
        return
    }

    // Power Configuration Cluster (0x0001) - Battery Voltage
    if (descMap.clusterInt == 0x0001 && descMap.attrInt == 0x0020 && descMap.value) {
        updateAttribute("healthStatus", "online", null, "physical")
        parseBatteryVoltage(descMap.value)
        return
    }

    // IAS Zone Cluster (0x0500)
    if (descMap.clusterInt == 0x0500) {
        if (descMap.attrInt == 0x0002 && descMap.value != null) {
            updateAttribute("healthStatus", "online", null, "physical")
            logDebug "Processing IAS Zone attribute 0x0002 fallback: ${descMap.value}"
            int value = Integer.parseInt(descMap.value, 16)
            processMotionState((value & 0x01) != 0)
        }
        return
    }
}

private void parseIasZoneStatus(String description) {
    logDebug "parseIasZoneStatus(): Processing IAS status string -> ${description}"
    ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
    if (zoneStatus != null) {
        boolean isMotion = zoneStatus.isAlarm1Set() || zoneStatus.isAlarm2Set()
        processMotionState(isMotion)
    }
}

private void processMotionState(boolean isActive) {
    String newState = isActive ? "active" : "inactive"
    updateAttribute("motion", newState, null, "physical")
}

private void parseBatteryVoltage(String hexValue) {
    if (hexValue == null) return
    logDebug "parseBatteryVoltage(): Raw hex -> ${hexValue}"
    
    int rawValue = Integer.parseInt(hexValue, 16)
    if (rawValue == 0 || rawValue == 0xFF) return

    // Attribute 0x0020 is unsigned 8-bit in 100mV units (30 = 3.0V)
    BigDecimal batteryVoltage = (rawValue / 10.0).setScale(1, RoundingMode.HALF_UP)
    
    if (batteryVoltage > 3.6) {
        logWarn "Battery voltage report exceeds expected range: ${batteryVoltage}V (raw hex: 0x${hexValue})"
    }

    BigDecimal minV = settings.minVolts != null ? new BigDecimal(settings.minVolts.toString()) : new BigDecimal("2.1")
    BigDecimal maxV = new BigDecimal("3.0")

    BigDecimal pct = (batteryVoltage - minV) / (maxV - minV) * 100
    int roundedPct = Math.round(pct.doubleValue())
    int batteryPercentage = Math.min(100, Math.max(0, roundedPct))

    logDebug "parseBatteryVoltage(): Voltage=${batteryVoltage}V, Calculated=${batteryPercentage}% (MinV threshold=${minV}V)"

    updateAttribute("lastBatteryReport", getTimestamp(), null, "digital")
    updateAttribute("batteryVoltage", batteryVoltage, "V", "physical")
    updateAttribute("battery", batteryPercentage, "%", "physical")
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