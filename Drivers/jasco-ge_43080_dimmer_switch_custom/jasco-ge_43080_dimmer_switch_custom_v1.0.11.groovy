/**
 * Jasco/GE Dimming Switch (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Dedicated Hubitat Elevation driver for the Jasco / GE / Enbrighten 43080 Zigbee 3.0 In-Wall Smart Dimmer.
 * Provides native On/Off, SwitchLevel (dimming), ChangeLevel control, precise reporting execution,
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
 * v1.0.11   09/04/26    jshimota    Added 'ASAP' option to Level Change Rate preference and set it as default when unselected.
 * v1.0.10   09/04/26    jshimota    Corrected fingerprint cluster typo to 0B05, added setLevel(0) -> off() translation, implemented pending healthCheck state tracking, added Level Change Rate preference, explicit sendHubCommand for GUI Health Check, and aligned logging/preference UI descriptions.
 * v1.0.9    09/04/26    jshimota    Added physical paddle LED configuration sequence and Endpoint 0x02 Direct Binding info blocks to driver preferences.
 * v1.0.8    09/04/26    jshimota    Promoted to production driver. Purged developer archaeology commands, finalized custom Health Check heartbeat, and verified MoveToLevelWithOnOff semantics.
 * v1.0.7    09/04/26    jshimota    Corrected proprietary cluster target from 0xB00D to 0x0B05 based on verified ZDO Simple Descriptor payloads.
 * v1.0.6    09/04/26    jshimota    Refactored archaeology commands into isolated ZDO Simple Descriptor requests (0x0004) for Endpoints 1 & 2 and targeted Cluster 0xB00D probes.
 * v1.0.5    09/04/26    jshimota    Fixed readAttribute parameter signatures in queryClusterB00D, parsed ZDO 0x8005 endpoint payload, and added raw ZCL probe commands.
 * v1.0.4    09/04/26    jshimota    Added developer archaeology commands (discoverEndpoints, queryClusterB00D) and ZDO 0x8005/0xB00D parsing.
 * v1.0.3    09/04/26    jshimota    Standardized setLevel to MoveToLevelWithOnOff (0x04), optimized parseDescMap, and verified daily health check scheduling.
 * v1.0.2    09/04/26    jshimota    Integrated incoming Zigbee frame parser, ZCL parsing engine for On/Off & SwitchLevel, and active Health Check attribute querying to maintain device activity status.
 * v1.0.1    09/04/26    jshimota    Added Switch and SwitchLevel capabilities, initial Zigbee fingerprint for Jasco 43080, and setLevel/on/off command stubs.
 * v1.0.0    09/04/26    jshimota    Initial driver shell creation based on standardized driver template.
 **/

static String version() { return '1.0.11' }
def timeStamp() { return "2026/09/04 05:35 PM" }

import groovy.transform.Field

metadata {
    definition (
        name: "Jasco/GE Dimming Switch (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/jasco-ge-dimmer/jasco-ge-dimmer.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "Switch"
        capability "SwitchLevel"
        capability "ChangeLevel"

        // Attributes
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]

        // Custom Commands
        command "Health Check"
        command "resetDriver"

        // Fingerprints
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0B05", outClusters: "000A,0019", manufacturer: "Jasco Products", model: "43080", deviceJoinName: "Enbrighten Zigbee In-Wall Smart Dimmer (43080)"
    }

    preferences {
        // Driver Informational Blocks
        input name: "ledInfoNote", type: "hidden", title: "<b>LED Indicator Behavior</b>", description: "<i>The driver does not expose remote Zigbee LED configuration because no LED-control parameter has been verified for this firmware.<br>To change the LED mode locally on the hardware, quickly press the <b>TOP rocker 3 times</b>, then press the <b>BOTTOM rocker 1 time</b>.<br>This cycles through the three modes: <b>LED ON when Load OFF</b> (Default), <b>LED ON when Load ON</b>, and <b>LED Always OFF</b>.</i>"
        input name: "bindingInfoNote", type: "hidden", title: "<b>Direct Zigbee Bulb Binding (Endpoint 0x02)</b>", description: "<i>Endpoint 0x02 exposes client output clusters for On/Off (0x0006) and Level Control (0x0008) and may support direct Zigbee binding to compatible bulbs or light groups.</i>"

        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver executes a Health Check to verify device online status and preserve Last Activity reporting.<br><b>Note:</b> This is a custom driver routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"
        input name: "startLevelChangeRate", type: "enum", title: "<b>Level Change Rate</b>", options: ["255": "ASAP (Maximum)", "50": "Fast (50 units/sec)", "25": "Medium (25 units/sec)", "10": "Slow (10 units/sec)"], defaultValue: "255", description: "<i>Sets the rate of level change used by startLevelChange(). Default: <b>ASAP</b>.</i>"

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
    // Cluster 0x0008: Level Control
    else if (descMap.clusterInt == 8 || descMap.clusterId == "0008") {
        if (descMap.attrInt == 0 || descMap.attrId == "0000") {
            isHealthFrame = true
            int rawLevel = Integer.parseInt(descMap.value, 16)
            int calculatedLevel = Math.round((rawLevel * 100) / 254)
            calculatedLevel = Math.max(1, Math.min(100, calculatedLevel))
            updateAttribute("level", calculatedLevel, "%")
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

def on() {
    logInfo "on() requested"
    List<String> cmds = []
    cmds += zigbee.on()
    return cmds
}

def off() {
    logInfo "off() requested"
    List<String> cmds = []
    cmds += zigbee.off()
    return cmds
}

def setLevel(level, duration = null) {
    int targetLevel = level.toInteger()
    
    // SwitchLevel Semantics: setLevel(0) translates to off()
    if (targetLevel <= 0) {
        logInfo "setLevel(${level}) requested (<=0); executing off()"
        return off()
    }
    
    logInfo "setLevel(${level}, ${duration}) requested"
    List<String> cmds = []
    targetLevel = Math.min(100, targetLevel)
    int rawLevel = Math.round((targetLevel * 254) / 100)
    
    if (duration != null) {
        int transitionTime = Math.round(duration.toDouble() * 10)
        cmds += zigbee.command(0x0008, 0x04, zigbee.convertToHexString(rawLevel, 2), zigbee.convertToHexString(transitionTime, 4))
    } else {
        cmds += zigbee.command(0x0008, 0x04, zigbee.convertToHexString(rawLevel, 2), "0000")
    }
    return cmds
}

def startLevelChange(direction) {
    logInfo "startLevelChange(${direction}) requested"
    List<String> cmds = []
    int upDown = (direction == "up") ? 0x00 : 0x01
    
    // Retrieve Level Change Rate preference; default to 255 (ASAP) if unconfigured
    String rateSetting = settings.startLevelChangeRate != null ? settings.startLevelChangeRate.toString() : "255"
    int rate = (rateSetting == "ASAP" || rateSetting == "255") ? 255 : rateSetting.toInteger()
    
    String hexRate = zigbee.convertToHexString(rate, 2)
    cmds += zigbee.command(0x0008, 0x01, zigbee.convertToHexString(upDown, 2), hexRate)
    return cmds
}

def stopLevelChange() {
    logInfo "stopLevelChange() requested"
    List<String> cmds = []
    cmds += zigbee.command(0x0008, 0x03)
    return cmds
}

def refresh() {
    logInfo "refresh() requested"
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000) // OnOff state
    cmds += zigbee.readAttribute(0x0008, 0x0000) // Level Control state
    return cmds
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
    
    cmds += zigbee.configureReporting(0x0006, 0x0000, DataType.BOOLEAN, 0, 65000, null)
    cmds += zigbee.configureReporting(0x0008, 0x0000, DataType.UINT8, 5, 65000, 1)
    
    cmds += executeHealthCheck()
    
    return cmds
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

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
 * Queries actual OnOff and Level Control attributes to refresh device activity status natively.
 **/
private List<String> executeHealthCheck() {
    logDebug "Executing Health Check..."
    state.healthCheckPending = true
    scheduleCommandTimeoutCheck()
    
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000)
    cmds += zigbee.readAttribute(0x0008, 0x0000)
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

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
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