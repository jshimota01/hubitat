/**
 * Jasco/GE 43080 Dimmer Switch (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Dedicated Hubitat Elevation driver for the Jasco / GE / Enbrighten 43080 Zigbee 3.0 In-Wall Smart Dimmer.
 * Provides native On/Off, SwitchLevel (dimming), ChangeLevel control, precise reporting execution,
 * background custom Health Check monitoring, and developer protocol archaeology routines.
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
 * v1.0.4    09/04/26    jshimota    Added developer archaeology commands (discoverEndpoints, queryClusterB00D) and ZDO 0x8005/0xB00D parsing.
 * v1.0.3    09/04/26    jshimota    Standardized setLevel to MoveToLevelWithOnOff (0x04), optimized parseDescMap, and verified daily health check scheduling.
 * v1.0.2    09/04/26    jshimota    Integrated incoming Zigbee frame parser, ZCL parsing engine for On/Off & SwitchLevel, and active Health Check attribute querying to maintain device activity status.
 * v1.0.1    09/04/26    jshimota    Added Switch and SwitchLevel capabilities, initial Zigbee fingerprint for Jasco 43080, and setLevel/on/off command stubs.
 * v1.0.0    09/04/26    jshimota    Initial driver shell creation based on standardized driver template.
 **/

static String version() { return '1.0.4' }
def timeStamp() { return "2026/09/04 03:40 PM" }

import groovy.transform.Field

metadata {
    definition (
        name: "Jasco/GE 43080 Dimmer Switch (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/jasco-ge_43080_dimmer_switch_custom/jasco-ge_43080_dimmer_switch_custom.groovy"
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
        command "discoverEndpoints"
        command "queryClusterB00D"

        // Fingerprints
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,B00D", outClusters: "000A,0019", manufacturer: "Jasco Products", model: "43080", deviceJoinName: "Enbrighten Zigbee In-Wall Smart Dimmer (43080)"
    }

    preferences {
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver executes a Health Check to verify device online status and preserve Last Activity reporting.<br><b>Note:</b> This is a custom driver routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>Off</b>", defaultValue: true, required: true
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
    
    // Clear pending Health Check timeout guard upon receiving any frame
    unschedule("deviceCommandTimeout")
    if (device.currentValue("healthStatus") != "online") {
        updateAttribute("healthStatus", "online")
    }

    if (description?.startsWith("read attr -") || description?.startsWith("catchall:")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        if (descMap) {
            parseDescMap(descMap)
        }
    }
}

private void parseDescMap(Map descMap) {
    logTrace "parseDescMap(): ${descMap}"

    // ZDO Cluster 0x8005 (Active Endpoints Response)
    if (descMap.clusterInt == 32773 || descMap.clusterId == "8005") {
        logInfo "*** ARCHAEOLOGY: Active Endpoints Response received *** data: ${descMap.data}"
        return
    }

    // ZDO Cluster 0x8021 (Bind Response) - Confirmation frame
    if (descMap.clusterInt == 32801 || descMap.clusterId == "8021") {
        logDebug "Received Zigbee Bind Response (0x8021) status: ${descMap.data}"
        return
    }

    // Cluster 0xB00D: Jasco Proprietary Cluster
    if (descMap.clusterInt == 45069 || descMap.clusterId == "B00D" || descMap.clusterId == "b00d") {
        logInfo "*** ARCHAEOLOGY: Cluster 0xB00D Response received *** raw: ${descMap}"
        return
    }

    // Cluster 0x0006: On/Off Control
    if (descMap.clusterInt == 6 || descMap.clusterId == "0006") {
        if (descMap.attrInt == 0 || descMap.attrId == "0000") {
            String val = (descMap.value == "01" || descMap.value == "1") ? "on" : "off"
            updateAttribute("switch", val)
        }
    }
    // Cluster 0x0008: Level Control
    else if (descMap.clusterInt == 8 || descMap.clusterId == "0008") {
        if (descMap.attrInt == 0 || descMap.attrId == "0000") {
            int rawLevel = Integer.parseInt(descMap.value, 16)
            int calculatedLevel = Math.round((rawLevel * 100) / 254)
            calculatedLevel = Math.max(1, Math.min(100, calculatedLevel))
            updateAttribute("level", calculatedLevel, "%")
        }
    }
}

/* =========================================================================================
   DEVELOPER ARCHAEOLOGY COMMANDS
   ========================================================================================= */

def discoverEndpoints() {
    logInfo "Executing Protocol Archaeology: Requesting Active Endpoints list (ZDO 0x0005)..."
    // ZDO Request 0x0005 (Active_EP_req) targeted at device short address
    List<String> cmds = ["he raw ${device.deviceNetworkId} 0 0 0x0005 {00 ${zigbee.swapOctets(device.deviceNetworkId)}} {0000}"]
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

def queryClusterB00D() {
    logInfo "Executing Protocol Archaeology: Probing Cluster 0xB00D attributes 0x0000..0x0005..."
    List<String> cmds = []
    // Read attributes 0x0000 through 0x0003 on Endpoint 01, Cluster 0xB00D (Jasco Mfg code 0x1124)
    cmds += zigbee.readAttribute(0xB00D, 0x0000, [mfgCode: "1124"], "01")
    cmds += zigbee.readAttribute(0xB00D, 0x0001, [mfgCode: "1124"], "01")
    cmds += zigbee.readAttribute(0xB00D, 0x0000, [:], "01") // Standard non-mfg query
    // Read attributes on Endpoint 02 if it exists
    cmds += zigbee.readAttribute(0xB00D, 0x0000, [mfgCode: "1124"], "02")
    
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
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
    logInfo "setLevel(${level}, ${duration}) requested"
    List<String> cmds = []
    int targetLevel = Math.max(0, Math.min(100, level.toInteger()))
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
    cmds += zigbee.command(0x0008, 0x01, zigbee.convertToHexString(upDown, 2), "32")
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
 **/
List<String> "Health Check"() {
    return executeHealthCheck()
}

/**
 * Public Scheduled Callback Target.
 **/
void executeHealthCheckScheduled() {
    List<String> cmds = executeHealthCheck()
    if (cmds) sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

/**
 * Private Health Check Execution Helper.
 * Queries actual OnOff and Level Control attributes to refresh device activity status natively.
 **/
private List<String> executeHealthCheck() {
    logDebug "Executing Health Check..."
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