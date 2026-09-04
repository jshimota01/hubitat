/**
 * eWeLink MS01 Motion Sensor (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Provides full device state management, configurable activity check-in intervals, motion reset timeouts, 
 * firmware querying, battery voltage monitoring, and phase-anchored custom Health Check architecture 
 * for eWeLink MS01 and Sonoff SNZB-03 motion sensors.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability.
 * - Hubitat's native capability exposes an unwanted "Ping" UI control button
 *   and does not provide the phase-anchored scheduling, timeout guards, or trace 
 *   logging behavior required by this driver architecture.
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
 * Instructions: Install driver, pair device or change driver to eWeLink MS01 Motion Sensor (Custom), and click 'Configure' to sync reporting schedules.
 *  
 * Changelog:
 * v1.0.21   09/04/26    jshimota    Integrated Master Driver Template v1.0.11 (custom Health Check architecture, healthStatus attribute, phase-anchored scheduling, and command timeout guards).
 * v1.0.20   09/04/26    jshimota    Deduplicated lastBatteryReport logging by updating timestamp silently via sendEvent in parseBatteryVoltage.
 * v1.0.19   08/30/26    jshimota    Decoupled battery percentage from voltage calculation; updated low voltage thresholds.
 * v1.0.18   08/30/26    jshimota    Applied Driver Master Template v1.0.6 (version demarcation trace helper, state.lastInitializedVersion code-version check, auto-reinit resetDriver).
 * v1.0.17   08/29/26    jshimota    Targeted watchdog unscheduling, raw ASCII string handling for attr 0x0009, and preference reset cleanup.
 * v1.0.16   08/29/26    jshimota    Expanded Basic cluster firmware queries (0x0001, 0x0002, 0x0009) for SNZB-03 compatibility.
 * v1.0.15   08/29/26    jshimota    Added Basic cluster firmware querying (0x0002) with softwareBuild Data fallback handling.
 * v1.0.14   08/29/26    jshimota    Applied template v1.0.3 fixes (setupSchedule in updated, safe preference parsing, updateFirmware, forced version events).
 * v1.0.13   08/28/26    jshimota    Standardized changelog format to tabbed MM/DD/YY columns without parentheses.
 * v1.0.12   08/28/26    jshimota    Applied master template v1.0.0 (def timeStamp(), return command signature updates, getTimestamp NPE safeguard).
 * v1.0.11   08/28/26    jshimota    Added lastBatteryReport timestamp attribute with NPE safeguards.
 * v1.0.10   08/28/26    jshimota    Fixed BigDecimal rounding compatibility, restored event return paths, and eliminated log doubling.
 * v1.0.9    08/28/26    jshimota    Applied master template, independent logging switches, and sendIfChanged deduplication.
 * v1.0.8    08/28/26    jshimota    Added comprehensive debug logging throughout parsing and battery calculation logic.
 * v1.0.7    08/28/26    jshimota    Enhanced cluster 0x0001 parsing to catch broader read attr formats and auto-calculate battery percentage from voltage.
 * v1.0.6    08/27/26    jshimota    Added battery voltage monitoring (0x0020) with low voltage warning logs at <= 2.9V.
 * v1.0.5    08/27/26    jshimota    Set software motion reset timer default to 0 (disabled).
 * v1.0.4    08/27/26    jshimota    Removed invalid IasZoneAttribute import class.
 * v1.0.3    08/27/26    jshimota    Set default check-in interval to 12 hours.
 * v1.0.2    08/27/26    jshimota    Updated check-in intervals to 1, 3, 6, 12, and 24 hours using Hubitat cron/native scheduling.
 * v1.0.1    08/27/26    jshimota    Fixed Hubitat 2.4 scheduler methods and enum type casting.
 * v1.0.0    08/27/26    jshimota    Initial release for eWeLink MS01 & Sonoff SNZB-03 motion sensors.
 **/

static String version() { return '1.0.21' }
def timeStamp() { return "2026/09/04 08:15 AM" }

import groovy.transform.Field
import hubitat.zigbee.zcl.DataType

metadata {
    definition (
        name: "eWeLink MS01 Motion Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota/Hubitat/Drivers/ewelink_ms01_motion_sensor.groovy"
    ) {
        capability "MotionSensor"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"

        // Attributes
        attribute "driverVersion", "string"
        attribute "batteryVoltage", "number"
        attribute "lastBatteryReport", "string"
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]

        // Custom Commands
        command "Health Check"
        command "resetDriver"
        command "updateFirmware"

        // Fingerprints for eWeLink MS01 & Sonoff variants
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "eWeLink", model: "MS01", deviceJoinName: "eWeLink MS01 Motion Sensor (Custom)"
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "eWeLink", model: "MSO1", deviceJoinName: "eWeLink MS01 Motion Sensor (Custom)"
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "Sonoff", model: "SNZB-03", deviceJoinName: "Sonoff SNZB-03 Motion Sensor"
    }

    preferences {
        input name: "motionResetTimer", type: "number", title: "Software Motion Reset (seconds)", description: "Auto-reset motion to inactive if sensor misses clear (0 to disable)", defaultValue: 0, required: true
        input name: "checkInInterval", type: "enum", title: "Activity Check-In Interval", options: ["1":"1 Hour", "3":"3 Hours", "6":"6 Hours", "12":"12 Hours", "24":"24 Hours"], defaultValue: "12", required: true
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver executes a Health Check to verify device online status.<br><b>Note:</b> This is a custom driver routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"

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

// Parse incoming Zigbee messages
def parse(String description) {
    logDebug "Raw description -> ${description}"
    
    // DEV REVIEW: Clear watchdog and mark online on incoming Zigbee traffic
    unschedule("deviceCommandTimeout")
    updateAttribute("healthStatus", "online")

    Map map = [:]
    
    if (description?.startsWith("zone status")) {
        map = parseIasZoneStatus(description)
    } else if (description?.startsWith("read attr") || description?.contains("cluster: 0001") || description?.contains("cluster: 0000")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        logDebug "Parsed description map -> ${descMap}"
        
        if (descMap.cluster == "0001") {
            if (descMap.attrId == "0021" && descMap.value) {
                logDebug "Processing battery percentage attribute (0x0021)"
                map = parseBattery(descMap.value)
            } else if (descMap.attrId == "0020" && descMap.value) {
                logDebug "Processing battery voltage attribute (0x0020)"
                map = parseBatteryVoltage(descMap.value)
            } else {
                logDebug "Cluster 0001 attribute ${descMap.attrId} ignored"
            }
        } else if (descMap.cluster == "0000") {
            if (descMap.attrId in ["0001", "0002", "0009"] && descMap.value) {
                unschedule("checkFirmwareResponseTimeout")
                logDebug "Processing Basic cluster firmware attribute (0x${descMap.attrId})"
                
                String fwVersion = descMap.value
                if (descMap.attrId in ["0001", "0002"]) {
                    if (descMap.value.matches("-?[0-9a-fA-F]+")) {
                        try {
                            fwVersion = Integer.parseInt(descMap.value, 16).toString()
                        } catch (Exception e) {
                            fwVersion = descMap.value
                        }
                    }
                }
                logInfo "Device software build reported: ${fwVersion}"
                updateDataValue("softwareBuild", fwVersion)
            }
        }
    } else if (description?.startsWith("enroll request")) {
        logDebug "Handling IAS enrollment request"
        List cmds = zigbee.enrollResponse()
        logDebug "Sending enroll response -> ${cmds}"
        return cmds
    } else {
        logDebug "Unhandled description pattern"
    }

    if (map) {
        sendIfChanged(map)
    }
    
    return []
}

// Handle IAS Zone Status (Motion events)
private Map parseIasZoneStatus(String description) {
    logDebug "parseIasZoneStatus(): Processing IAS status string -> ${description}"
    Integer status = zigbee.parseZoneStatus(description)?.alarm1 ? 1 : 0
    Integer resetTimer = (motionResetTimer != null) ? (motionResetTimer as Integer) : 0
    
    if (status == 1) {
        if (resetTimer > 0) {
            logDebug "parseIasZoneStatus(): Motion active. Scheduling software reset in ${resetTimer} seconds"
            runIn(resetTimer, "resetMotionToInactive")
        }
        return [name: "motion", value: "active", descriptionText: "is active"]
    } else {
        logDebug "parseIasZoneStatus(): Motion inactive. Clearing software reset timers"
        unschedule("resetMotionToInactive")
        return [name: "motion", value: "inactive", descriptionText: "is inactive"]
    }
}

// Parse Battery Percentage (0x0001 / 0x0021)
private Map parseBattery(String hexValue) {
    logDebug "parseBattery(): Raw hex -> ${hexValue}"
    Integer rawValue = Integer.parseInt(hexValue, 16)
    // ZCL standard: Value is 0.5% units (0x00-0xC8 maps to 0-100%)
    Integer pct = Math.round(rawValue / 2)
    pct = Math.min(100, Math.max(0, pct))
    logDebug "parseBattery(): Computed battery percentage -> ${pct}%"
    
    // DEV REVIEW: Primary timestamp log emitted via sendIfChanged
    sendIfChanged([name: "lastBatteryReport", value: getTimestamp(), descriptionText: "last battery report timestamp updated"])
    
    return [name: "battery", value: pct, unit: "%", descriptionText: "battery is ${pct}%"]
}

// Parse Battery Raw Voltage (0x0001 / 0x0020)
private Map parseBatteryVoltage(String hexValue) {
    logDebug "parseBatteryVoltage(): Raw hex -> ${hexValue}"
    Integer rawValue = Integer.parseInt(hexValue, 16)
    BigDecimal voltage = (rawValue / 10.0).toBigDecimal().setScale(1, BigDecimal.ROUND_HALF_UP)
    
    logDebug "parseBatteryVoltage(): Measured voltage -> ${voltage}V"
    
    // DEV REVIEW: Silently update attribute without emitting duplicate info log entry
    sendEvent(name: "lastBatteryReport", value: getTimestamp(), displayed: false)
    
    // Low Voltage Warning Triggers based on typical Lithium Coin Cell behavior
    if (voltage <= 2.5) {
        logError "CRITICAL: Battery voltage severely low! Current voltage: ${voltage}V"
    } else if (voltage <= 2.6) {
        logWarn "Low battery voltage detected! Current voltage: ${voltage}V (Threshold: 2.6V)"
    }
    
    return [name: "batteryVoltage", value: voltage, unit: "V", descriptionText: "battery voltage is ${voltage}V"]
}

// Software motion reset timer callback
def resetMotionToInactive() {
    logInfo "Motion reset to inactive (timeout)"
    logDebug "resetMotionToInactive(): Timeout elapsed, emitting inactive event"
    sendIfChanged([name: "motion", value: "inactive", descriptionText: "motion set to inactive via timeout"])
}

// Scheduled check-in to keep device active on inactivity reports
def checkIn() {
    logDebug "Executing scheduled check-in event"
    sendEvent(name: "checkIn", value: now(), displayed: false, isStateChange: true)
}

def setupSchedule() {
    unschedule("checkIn")
    String interval = checkInInterval ?: "12"
    logDebug "Setting activity check-in interval to ${interval} hour(s)"
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

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    
    initializeHealthCheckPhase()
    sendEvent(name: "healthStatus", value: "unknown")

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating preferences..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
    setupSchedule()

    if ((motionResetTimer ?: 0).toInteger() <= 0) {
        logDebug "Software motion reset timer disabled. Unscheduling existing reset jobs."
        unschedule("resetMotionToInactive")
    }
}

// Return dynamic List for Zigbee command transmission
def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)

    setupSchedule()
    
    // Safely parse checkInInterval to avoid casting NPEs
    Integer checkInHours = (checkInInterval ?: "12").toString().toInteger()
    Integer maxIntervalSec = checkInHours * 3600
    
    // Binding and reporting configurations
    List<String> cmds = []
    cmds += zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 30, maxIntervalSec, 0x01) // Raw Voltage (0.1V steps)
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 30, maxIntervalSec, 0x01) // Battery Percentage (0.5% steps)
    cmds += zigbee.readAttribute(0x0001, 0x0020)
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    cmds += zigbee.readAttribute(0x0000, 0x0001) // Application Version
    cmds += zigbee.readAttribute(0x0000, 0x0002) // Stack Version
    cmds += zigbee.readAttribute(0x0000, 0x0009) // Software Build ID
    cmds += zigbee.enrollResponse()
    
    // Immediately execute Health Check to establish online healthStatus right away
    cmds += executeHealthCheck()

    logDebug "configure() sending Zigbee payload -> ${cmds}"
    return cmds
}

def refresh() {
    logDebug "Executing refresh()..."
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0001, 0x0020)
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    cmds += zigbee.readAttribute(0x0000, 0x0001)
    cmds += zigbee.readAttribute(0x0000, 0x0002)
    cmds += zigbee.readAttribute(0x0000, 0x0009)
    logDebug "refresh(): Generated Zigbee read commands -> ${cmds}"
    return cmds
}

// Update Firmware Command Routine (Zigbee OTA trigger with timeout safeguard)
List<String> updateFirmware() {
    logInfo "Checking for firmware updates..."
    runIn(10, "checkFirmwareResponseTimeout")
    
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0000, 0x0001)
    cmds += zigbee.readAttribute(0x0000, 0x0002)
    cmds += zigbee.readAttribute(0x0000, 0x0009)
    cmds += zigbee.updateFirmware()
    return cmds
}

// Timeout handler if device does not wake up or OTA server returns no match
void checkFirmwareResponseTimeout() {
    if (!device.getDataValue("softwareBuild")) {
        logWarn "No firmware response received from device (device sleeping or OTA image unavailable)."
    } else {
        logInfo "Firmware check complete. Current recorded build: ${device.getDataValue('softwareBuild')}"
    }
}

private void initialize(Boolean isInstall = false) {
    state.lastInitializedVersion = version()
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    unschedule("disableDebugLogging")

    // Ensure healthStatus attribute exists on initialize/reset
    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

    // Centralized Health Check Scheduler
    final int interval = settings.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 480
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
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

/* =========================================================================================
   HEALTH CHECK ROUTINE TEMPLATE (CUSTOM HEALTH CHECK ARCHITECTURE)
   ========================================================================================= */

/**
 * Public GUI Command Entry Point.
 * Single entry point exposed on the Device Detail page to avoid Hubitat UI button duplication.
 **/
List<String> "Health Check"() {
    return executeHealthCheck()
}

/**
 * Public Scheduled Callback Target.
 * Serves as the public entry point required by Hubitat's scheduler engine,
 * delegating to the private execution helper.
 **/
void executeHealthCheckScheduled() {
    List<String> cmds = executeHealthCheck()
    if (cmds) sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

/**
 * Private Health Check Execution Helper.
 * Transmits underlying device query request, registers timeout check,
 * and remains hidden from the Hubitat GUI command interface.
 **/
private List<String> executeHealthCheck() {
    logDebug "Executing Health Check..."
    scheduleCommandTimeoutCheck()
    
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0001, 0x0020) // Read battery voltage
    return cmds
}

/**
 * Modular Health Check Scheduler with Persistent Phase-Anchoring.
 * Anchors check schedules to a persistent random daily time offset to stagger hub network traffic.
 **/
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

private void scheduleCommandTimeoutCheck(final int delay = COMMAND_TIMEOUT) {
    runIn(delay, "deviceCommandTimeout", [overwrite: true])
}

void deviceCommandTimeout() {
    logWarn "No Health Check response received (device offline?)"
    updateAttribute("healthStatus", "offline")
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// Master Utility Routine for Driver GUI Button
void resetDriver() {
    logInfo "Starting full driver reset..."
    
    Object savedHour = state.healthCheckStartHour
    Object savedMinute = state.healthCheckStartMinute

    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()

    if (savedHour != null) state.healthCheckStartHour = savedHour
    if (savedMinute != null) state.healthCheckStartMinute = savedMinute

    initialize(false)
    logInfo "Driver reset process completed and re-initialized."
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

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
    logInfo descriptionText
    sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
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
        logInfo "${desc}"
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

// Constants
@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10