/**
 * Tuya ZG-204ZL Motion & Lux (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Standalone, lightweight driver for Tuya ZG-204ZL Mini PIR Motion & Illuminance Sensors 
 * (TS0601 / _TZE200_3towulqd and _TZE200_rhgsbacq). Bypasses heavy dynamic driver libraries
 * to eliminate lux-reporting race conditions and execution overhead, with advanced software 
 * lux delta filtering, calibration offset, and a passive battery-friendly health tracking engine.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability.
 * - Battery Device Optimization: Uses passive lastActivity evaluation anchored to the 
 *   Health Check interval to prevent unnecessary wakeups and avoid false offline states 
 *   caused by dropped active read requests on sleepy Zigbee end devices.
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
 * v1.0.11   09/24/26    jshimota    Purged obsolete lastDebugEnabled and lastTraceEnabled state/RAM tracking variables entirely.
 * v1.0.10   09/24/26    jshimota    Reverted updateAttribute() strictly to standard template pattern, eliminating duplicate state checks in favor of native currentValue comparison.
 * v1.0.9    09/24/26    jshimota    Purged all ephemeral last_* and lastTime_* state variable writes from updateAttribute() to protect database I/O, retaining only lastActivityTime for health tracking.
 * v1.0.8    09/24/26    jshimota    Suppressed unhandled warnings for DP 102/103 and added race-condition protection between DP 1 and IAS zone motion updates.
 * v1.0.7    09/24/26    jshimota    Decoupled passive Health Check executions from refresh()/configure() routines to eliminate rapid-fire log cascades during setting changes.
 * v1.0.6    09/24/26    jshimota    Added ZCL Cluster 0x0001 battery fallback parser and synchronized IAS Zone vs DP 1 motion state sequence.
 * v1.0.5    09/24/26    jshimota    Added catchall decoder for raw EF00 strings, added IAS zone status handler for motion events, and fixed string parsing checks.
 * v1.0.4    09/24/26    jshimota    Fixed SQL compilation error by updating controllerType from "ZIGBEE" to "ZGB" (3-char VARCHAR limit).
 * v1.0.3    09/24/26    jshimota    Patched hex formatting bugs in updated(), fixed keepTime DP type, converted Health Check to passive lastActivity tracking for sleepy battery nodes, and tuned intelligent lux/health defaults.
 * v1.0.2    09/24/26    jshimota    Integrated Third Reality Lux Management engine: software Lux delta guard, Lux offset, and 500ms in-memory event debounce buffer in updateAttribute().
 * v1.0.1    09/24/26    jshimota    Integrated Tuya ZG-204ZL motion & lux parsing engine into driver template architecture.
 * v1.0.0    08/30/26    jshimota    Initial version point / release of standardized template.
 **/

static String version() { return '1.0.11' }
def timeStamp() { return "2026/09/24 12:05 PM" }

import groovy.transform.Field

metadata {
    definition (
        name: "Tuya ZG-204ZL Motion & Lux (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/driver_directory/Tuya_ZG204ZL_Motion_Lux_Custom.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "MotionSensor"
        capability "IlluminanceMeasurement"
        capability "Battery"
        capability "Sensor"

        // Attributes
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]

        // Custom Commands
        command "Health Check"
        command "resetDriver"

        // Fingerprints
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0004,0005,EF00", outClusters: "0019,000A", model: "TS0601", manufacturer: "_TZE200_3towulqd", controllerType: "ZGB"
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0004,0005,EF00", outClusters: "0019,000A", model: "TS0601", manufacturer: "_TZE200_rhgsbacq", controllerType: "ZGB"
    }

    preferences {
        input name: "pirSensitivity", type: "enum", title: "<b>PIR Sensitivity</b>", options: ["0": "Low", "1": "Medium", "2": "High"], defaultValue: "1"
        input name: "pirKeepTime", type: "enum", title: "<b>PIR Keep Time</b>", options: ["0": "10 Seconds", "1": "30 Seconds", "2": "60 Seconds", "3": "120 Seconds"], defaultValue: "1"
        
        // Lux Management Preferences
        input name: "luxDelta", type: "enum", title: "<b>Lux Reporting Sensitivity</b>", options: LuxSensitivityOpts.options, defaultValue: LuxSensitivityOpts.defaultValue, description: "<i>Sets the minimum Lux change required before the driver commits an illuminance event.<br><b>Default: 15 units (Responsive for lighting automation).</b></i>"
        input name: "luxOffset", type: "number", title: "<b>Lux Calibration Offset</b>", description: "<i>Adjust reported lux value by adding (+) or subtracting (-) a fixed integer.</i>", defaultValue: 0

        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver checks last activity to verify device online status.<br><b>Note:</b> Uses passive lastActivity tracking to preserve battery life on sleepy sensors.</i>"

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

/* =========================================================================================
   ZIGBEE PARSING ENGINE
   ========================================================================================= */

void parse(String description) {
    logDebug "parse(): ${description}"
    if (!description) return

    // 1. Process Raw Zone Status Frames (Instant Motion Active/Inactive)
    if (description.startsWith("zone status")) {
        markHealthCheckSuccess("0500")
        if (description.contains("zone status 0x0001") || description.contains("zone status 0x0021")) {
            state.lastIasMotionActiveTime = now()
            updateAttribute("motion", "active", null, "physical")
        } else if (description.contains("zone status 0x0000") || description.contains("zone status 0x0020")) {
            updateAttribute("motion", "inactive", null, "physical")
        }
        return
    }

    // 2. Intercept ZCL Cluster 0x0001 Battery Percentage Reports
    if (description.contains("cluster: 0001") || description.contains("clusterInt: 1")) {
        markHealthCheckSuccess("0001")
        Map descMap = zigbee.parseDescriptionAsMap(description)
        if (descMap && descMap.attrInt == 0x0021 && descMap.value != null) {
            int battRaw = Integer.parseInt(descMap.value, 16)
            int battPct = Math.min(100, Math.max(0, Math.round(battRaw / 2.0)))
            updateAttribute("battery", battPct, "%", "physical")
        }
        return
    }

    // 3. Intercept and IGNORE Standard ZCL 0x0400 to prevent race conditions with Tuya EF00 DP 12
    if (description.contains("cluster: 0400") || description.contains("clusterInt: 1024")) {
        logDebug "Ignored duplicate ZCL 0x0400 report (processed via Tuya DP 12 instead)"
        markHealthCheckSuccess("0400")
        return
    }

    // 4. Process Tuya Cluster EF00 Commands
    if (description.contains("EF00")) {
        markHealthCheckSuccess("EF00")
        
        if (description.startsWith("catchall:")) {
            List<String> rawBytes = description.split(" ").collect { it.trim() }
            if (rawBytes.size() >= 11) {
                int dataIdx = description.indexOf("[")
                if (dataIdx != -1) {
                    String cleanBytes = description.substring(dataIdx + 1, description.indexOf("]"))
                    List<String> dataList = cleanBytes.split(",").collect { it.trim() }
                    parseTuyaCluster(dataList)
                    return
                }
            }
        }

        Map descMap = zigbee.parseDescriptionAsMap(description)
        if (descMap && descMap.data) {
            parseTuyaCluster(descMap.data)
        }
    }
}

// Parse raw Tuya EF00 Payload bytes
private void parseTuyaCluster(List<String> data) {
    if (data == null || data.size() < 6) return

    int dp = Integer.parseInt(data[2], 16)
    int type = Integer.parseInt(data[3], 16)
    int len = Integer.parseInt(data[4] + data[5], 16)

    if (data.size() < (6 + len)) return

    long rawVal = 0
    for (int i = 0; i < len; i++) {
        rawVal = (rawVal << 8) + Integer.parseInt(data[6 + i], 16)
    }

    logTrace "Tuya DP Parsed: DP 0x${Integer.toHexString(dp)} (${dp}) | Type: ${type} | Value: ${rawVal}"

    switch (dp) {
        case 0x01: // Motion State (DP 1)
            if (rawVal == 1) {
                updateAttribute("motion", "active", null, "physical")
            } else {
                Long lastIasActive = state.lastIasMotionActiveTime as Long ?: 0L
                if ((now() - lastIasActive) > 1000) {
                    updateAttribute("motion", "inactive", null, "physical")
                } else {
                    logTrace "parseTuyaCluster(): Suppressed DP 1 inactive frame due to recent IAS Zone active trigger"
                }
            }
            break

        case 0x04: // Battery Percentage (DP 4)
            int battVal = Math.min(100, Math.max(0, rawVal.intValue()))
            updateAttribute("battery", battVal, "%", "physical")
            break

        case 0x0C: // Illuminance / Lux (DP 12)
            int reportedLux = rawVal.intValue()
            int finalLux = Math.max(0, reportedLux + (settings.luxOffset ?: 0))

            // Software-Enforced Driver Lux Delta Guard
            int currentLux = (device.currentValue("illuminance") ?: 0) as int
            int minLuxDelta = settings.luxDelta != null ? settings.luxDelta.toInteger() : 15
            int luxChange = Math.abs(finalLux - currentLux)

            if (state.lastReportedLuxRaw != null && luxChange < minLuxDelta) {
                logTrace "parseTuyaCluster(): Lux update suppressed (${finalLux} lx vs current ${currentLux} lx | Delta ${luxChange} < threshold ${minLuxDelta})"
                return
            }

            state.lastReportedLuxRaw = reportedLux
            logDebug "parseTuyaCluster(): Lux report accepted -> Raw=${reportedLux}, Final=${finalLux} lx"
            updateAttribute("illuminance", finalLux, "lx", "physical")
            break

        case 0x09: // Sensitivity Confirmation
            logDebug "Sensitivity confirmed set to: ${rawVal}"
            break

        case 0x0A: // Keep Time Confirmation
            logDebug "Keep Time confirmed set to: ${rawVal}s"
            break

        case 0x66: // DP 102: Illuminance Reporting Enabled
        case 0x67: // DP 103: Minimum Lux Interval
            logTrace "Tuya config parameter confirmed -> DP ${dp}: ${rawVal}"
            break

        default:
            logDebug "Unhandled Tuya DP ID: ${dp} (Value: ${rawVal})"
            break
    }
}

def refresh() {
    logInfo "refresh() requested (Sleepy device will respond/sync on next wake)"
    return []
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    
    initializeHealthCheckPhase()
    sendEvent(name: "healthStatus", value: "unknown")
    sendEvent(name: "motion", value: "inactive")
    sendEvent(name: "illuminance", value: 0, unit: "lx")
    sendEvent(name: "battery", value: 100, unit: "%")

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    purgeObsoleteStates()
    logInfo "Preferences updated"

    initialize(false)

    List<String> cmds = []
    
    if (settings.pirSensitivity != null) {
        String hexVal = String.format("%02x", settings.pirSensitivity.toInteger())
        cmds += sendTuyaCommand("09", "04", "01", hexVal)
    }
    
    if (settings.pirKeepTime != null) {
        String hexVal = String.format("%02x", settings.pirKeepTime.toInteger())
        cmds += sendTuyaCommand("0A", "04", "01", hexVal)
    }

    if (cmds) {
        sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
    }
}

def configure() {
    checkAndLogVersionDemarcation()
    purgeObsoleteStates()
    logInfo "Configuring device..."
    
    List<String> cmds = []
    return cmds
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    purgeObsoleteStates()
    unschedule("disableDebugLogging")

    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

    final int interval = settings.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 1440
    if (interval > 0) {
        scheduleHealthCheck("executeHealthCheckScheduled", interval)
    } else {
        unschedule("executeHealthCheckScheduled")
    }

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging enabled. Will automatically turn off in 30 minutes."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

// Routine to actively purge obsolete state variables from earlier builds
private void purgeObsoleteStates() {
    ["lastDebugEnabled", "lastTraceEnabled"].each { key ->
        if (state.containsKey(key)) {
            state.remove(key)
        }
    }
}

/* =========================================================================================
   PASSIVE BATTERY HEALTH CHECK ENGINE
   ========================================================================================= */

List<String> "Health Check"() {
    return executeHealthCheck()
}

void executeHealthCheckScheduled() {
    executeHealthCheck()
}

private List<String> executeHealthCheck() {
    logDebug "Executing Passive Health Check..."
    
    final Long lastCheckIn = state.lastActivityTime as Long ?: 0L
    final int intervalMin = settings.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 1440
    
    if (intervalMin == 0) {
        logDebug "Health Check disabled via preferences."
        return []
    }

    final Long thresholdMs = (intervalMin + 15) * 60 * 1000L
    final Long timeSinceActivity = now() - lastCheckIn

    if (lastCheckIn > 0 && timeSinceActivity > thresholdMs) {
        logWarn "Health Check failed: Device has not communicated in ${Math.round(timeSinceActivity / 60000)} minutes (offline)."
        updateAttribute("healthStatus", "offline")
    } else {
        logDebug "Health Check passed: Device communicated ${Math.round(timeSinceActivity / 60000)} minutes ago."
        sendEvent(
            name: "healthStatus", 
            value: "online", 
            isStateChange: false, 
            descriptionText: "${device.displayName} health check verified online"
        )
    }
    
    return []
}

private void markHealthCheckSuccess(String clusterHex = null) {
    state.lastActivityTime = now()
    
    sendEvent(
        name: "healthStatus", 
        value: "online", 
        isStateChange: false, 
        descriptionText: "${device.displayName} health check verified online"
    )
}

private void initializeHealthCheckPhase() {
    if (state.healthCheckStartHour == null) state.healthCheckStartHour = new Random().nextInt(24)
    if (state.healthCheckStartMinute == null) state.healthCheckStartMinute = new Random().nextInt(60)
}

private void scheduleHealthCheck(String methodToSchedule, int intervalMin) {
    unschedule(methodToSchedule)
    initializeHealthCheckPhase()

    final int h = state.healthCheckStartHour as Integer
    final int m = state.healthCheckStartMinute as Integer

    logInfo "Scheduling Health Check every ${intervalMin} minutes anchored at ${String.format('%02d:%02d', h, m)} daily"

    switch (intervalMin) {
        case 60:
            schedule("0 ${m} * ? * * *", methodToSchedule)
            break
        case 240:
            String h4 = [0, 4, 8, 12, 16, 20].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h4} ? * * *", methodToSchedule)
            break
        case 480:
            String h8 = [0, 8, 16].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h8} ? * * *", methodToSchedule)
            break
        case 720:
            String h12 = [0, 12].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h12} ? * * *", methodToSchedule)
            break
        case 1440:
            schedule("0 ${m} ${h} ? * * *", methodToSchedule)
            break
        default:
            if (intervalMin >= 60) {
                int hours = intervalMin / 60
                schedule("0 ${m} */${hours} ? * * *", methodToSchedule)
            } else {
                schedule("0 */${intervalMin} * ? * * *", methodToSchedule)
            }
            break
    }
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
        logInfo "Debug logging disabled"
    }
}

void resetDriver() {
    logInfo "Starting full driver reset..."
    
    Object savedHour = state.healthCheckStartHour
    Object savedMinute = state.healthCheckStartMinute

    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()

    if (savedHour != null) state.healthCheckStartHour = savedHour
    if (savedMinute != null) state.healthCheckStartMinute = savedMinute

    state.lastActivityTime = now()
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

private List<String> sendTuyaCommand(String dp, String dpType, String len, String data) {
    String command = "0001${dp}${dpType}${len}${data}"
    return zigbee.command(0xEF00, 0x00, command)
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

// Constants
@Field static final Map LuxSensitivityOpts = [
    defaultValue: 15,
    options: [ 5: "Very High (5 units - Sensitive)", 15: "High (15 units - Default)", 30: "Medium-High (30 units)", 50: "Medium (50 units)", 100: "Low (100 units)", 200: "Very Low (200 units)", 500: "Minimal (500 units)" ]
]

@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 1440,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours (Default)", 0: "Disabled" ]
]