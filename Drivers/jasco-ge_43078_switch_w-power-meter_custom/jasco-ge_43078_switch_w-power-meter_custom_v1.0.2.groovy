/**
 * Jasco/GE 43078 Switch w/Power Meter Driver (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Dedicated Hubitat Elevation driver for the Jasco / GE / Enbrighten 43078 (ZB4003) Zigbee 3.0 In-Wall Smart Switch.
 * Provides native On/Off control, real-time Instantaneous Power (W), Cumulative Energy (kWh) tracking,
 * formatted string attributes (powerText, energyText), precise reporting execution,
 * and background custom Health Check monitoring without using stock platform ping/pong UI elements.
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
 * Changelog:
 * v1.0.2    09/04/26    jshimota    Added powerText ("X.X Watts") and energyText ("X.XXXX kWh") custom display attributes for tile visualization.
 * v1.0.1    09/04/26    jshimota    Added Power & Energy informational preference block and enriched descriptionText for metering events.
 * v1.0.0    09/04/26    jshimota    Initial production driver promotion following verified protocol archaeology on 43078 hardware.
 **/

static String version() { return '1.0.2' }
def timeStamp() { return "2026/09/04 06:35 PM" }

import groovy.transform.Field
import hubitat.zigbee.zcl.DataType
import java.math.RoundingMode

metadata {
    definition (
        name: "Jasco/GE 43078 Switch w/Power Meter Driver (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/jasco-ge-switch-43078/jasco-43078-switch-custom.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "EnergyMeter"
        capability "PowerMeter"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"

        // Attributes
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "powerText", "string"
        attribute "energyText", "string"

        // Custom Commands
        command "Health Check"
        command "updateFirmware"
        command "resetDriver"

        // Fingerprint (Jasco 43078 / ZB4003)
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0702,0B05", outClusters: "000A,0019", manufacturer: "Jasco Products", model: "43078", deviceJoinName: "Enbrighten Zigbee In-Wall Smart Switch w/ Metering (43078)"
    }

    preferences {
        // Driver Informational Preference Blocks
        input name: "meteringInfoNote", type: "hidden", title: "<b>Power & Energy Metering Explanation</b>", description: "<i><b>Power (`power` in Watts):</b> Instantaneous real-time electrical draw currently consumed by the connected load (scaled ÷10).<br><b>Energy (`energy` in kWh):</b> Total cumulative electrical usage accrued by the switch over time (scaled ÷10000).<br><b>Formatted Attributes:</b> `powerText` (\"X Watts\") and `energyText` (\"X kWh\") are available for tile visualization.</i>"
        input name: "ledInfoNote", type: "hidden", title: "<b>LED Indicator Behavior</b>", description: "<i>The driver does not expose remote Zigbee LED configuration because no LED-control parameter has been verified for this firmware.<br>To change the LED mode locally on the hardware, quickly press the <b>TOP rocker 3 times</b>, then press the <b>BOTTOM rocker 1 time</b>.<br>This cycles through the three modes: <b>LED ON when Load OFF</b> (Default), <b>LED ON when Load ON</b>, and <b>LED Always OFF</b>.</i>"

        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver executes a Health Check to verify device online status and preserve Last Activity reporting.<br><b>Note:</b> This is a custom driver routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"

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

    if (description?.startsWith("read attr -") || description?.startsWith("catchall:")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        if (descMap) {
            parseDescMap(descMap)
        }
    }
}

private void parseDescMap(Map descMap) {
    logTrace "parseDescMap(): ${descMap}"

    // ZDO Cluster 0x8021 (Bind Response) - Confirmation frame
    if (descMap.clusterInt == 32801 || descMap.clusterId == "8021") {
        logDebug "Received Zigbee Bind Response (0x8021) status: ${descMap.data}"
        return
    }

    boolean isHealthFrame = false

    // Cluster 0x0006: On/Off Control
    if (descMap.clusterInt == 6 || descMap.clusterId == "0006") {
        if (descMap.attrInt == 0 || descMap.attrId == "0000") {
            isHealthFrame = true
            String val = (descMap.value == "01" || descMap.value == "1") ? "on" : "off"
            updateAttribute("switch", val)
        }
    }
    // Cluster 0x0702: Simple Metering
    else if (descMap.clusterInt == 1794 || descMap.clusterId == "0702") {
        if (descMap.attrInt == 0 || descMap.attrId == "0000") { // CurrentSummationDelivered (Cumulative Energy)
            isHealthFrame = true
            Long rawVal = Long.parseLong(descMap.value, 16)
            BigDecimal energyKwh = (rawVal / 10000.0).setScale(4, RoundingMode.HALF_UP)
            
            updateAttribute("energy", energyKwh, "kWh", null, "Cumulative Energy")
            updateAttribute("energyText", "${energyKwh} kWh", null, null, "Cumulative Energy Text")
        } else if (descMap.attrInt == 1024 || descMap.attrId == "0400") { // InstantaneousDemand (Real-time Power)
            isHealthFrame = true
            Long rawVal = Long.parseLong(descMap.value, 16)
            BigDecimal powerWatts = (rawVal / 10.0).setScale(1, RoundingMode.HALF_UP)
            
            updateAttribute("power", powerWatts, "W", null, "Instantaneous Power")
            updateAttribute("powerText", "${powerWatts} Watts", null, null, "Instantaneous Power Text")
        }
    }

    // Strict Health Check Validation
    if (isHealthFrame) {
        if (state.healthCheckPending == true) {
            logDebug "Valid Health Check response verified on cluster 0x${descMap.clusterId}"
            state.healthCheckPending = false
            unschedule("deviceCommandTimeout")
        }
        if (device.currentValue("healthStatus") != "online") {
            updateAttribute("healthStatus", "online")
        }
    }
}

/* =========================================================================================
   COMMAND IMPLEMENTATIONS
   ========================================================================================= */

List<String> on() {
    logInfo "on() requested"
    return zigbee.on()
}

List<String> off() {
    logInfo "off() requested"
    return zigbee.off()
}

List<String> refresh() {
    logInfo "refresh() requested"
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff state
    cmds += zigbee.readAttribute(0x0702, 0x0000) // Cumulative Energy (kWh)
    cmds += zigbee.readAttribute(0x0702, 0x0400) // Instantaneous Power (W)
    return cmds
}

List<String> updateFirmware() {
    logInfo "Checking for firmware updates..."
    return zigbee.updateFirmware()
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    
    initializeHealthCheckPhase()
    sendEvent(name: "healthStatus", value: "unknown")

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    
    initialize(false)
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device reporting & bindings..."
    
    initialize(false)
    
    List<String> cmds = []
    
    // Configure OnOff Reporting
    cmds += zigbee.configureReporting(0x0006, 0x0000, DataType.BOOLEAN, 0, 65000, null)
    
    // Configure Simple Metering Reporting (Power & Energy)
    cmds += zigbee.configureReporting(0x0702, 0x0000, DataType.UINT48, 10, 3600, 10) // Energy (0.001 kWh delta)
    cmds += zigbee.configureReporting(0x0702, 0x0400, DataType.INT24, 5, 300, 10)   // Power (1.0 W delta)
    
    cmds += executeHealthCheck()
    
    return cmds
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    state.healthCheckPending = false

    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

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
        logInfo "Debug logging enabled. Will automatically turn off in 30 minutes."
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
 * Explicitly dispatches health check commands using sendHubCommand.
 **/
void "Health Check"() {
    List<String> cmds = executeHealthCheck()
    if (cmds) {
        sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
    }
}

/**
 * Public Scheduled Callback Target.
 **/
void executeHealthCheckScheduled() {
    List<String> cmds = executeHealthCheck()
    if (cmds) {
        sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
    }
}

/**
 * Private Health Check Execution Helper.
 * Queries actual OnOff, Power, and Energy attributes to refresh device activity status natively.
 **/
private List<String> executeHealthCheck() {
    logDebug "Executing Health Check..."
    state.healthCheckPending = true
    scheduleCommandTimeoutCheck()
    
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000)
    cmds += zigbee.readAttribute(0x0702, 0x0000)
    cmds += zigbee.readAttribute(0x0702, 0x0400)
    return cmds
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

private void scheduleCommandTimeoutCheck(final int delay = COMMAND_TIMEOUT) {
    runIn(delay, "deviceCommandTimeout", [overwrite: true])
}

void deviceCommandTimeout() {
    logWarn "No Health Check response received (device offline?)"
    state.healthCheckPending = false
    updateAttribute("healthStatus", "offline")
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

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null, final String customLabel = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String label = customLabel ?: attribute
    final String descriptionText = "${device.displayName} - ${label} was set to ${value}${unit ? ' ' + unit : ''}"
    logInfo descriptionText
    sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
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

// Constants
@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10