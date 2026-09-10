/**
 * Third Reality RGBW Bulb (Custom)
 * Device Driver for Hubitat Elevation
 * Third Reality Smart Color Bulb - model 3RCB01057Z
 *
 * Purpose:
 * Custom purpose-built driver for Third Reality 3RCB01057Z Zigbee RGBW Smart Color Bulb. 
 * Features standardized phase-anchored health tracking, full RGB color control, color temperature 
 * adjustments (2000K - 6500K), level transitions, and human-readable colorName tracking.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability.
 * - Mains-powered router device: Uses active Basic Cluster attribute reading 
 *   to verify online presence with phase-anchored scheduling.
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
 * v1.0.1    09/10/26    jshimota    Bug Review Fixes: Aligned logMessage setting keys, hardened CT/mireds range conversion, and updated cluster 0x0300 color mode 0x0008 attribute router.
 * v1.0.0    09/10/26    jshimota    Initial release for Third Reality 3RCB01057Z smart color bulb supporting RGB, Color Temperature (2000K-6500K), and active Basic Cluster Health Checks.
 **/

static String version() { return '1.0.1' }
def timeStamp() { return "2026/09/10 08:35 AM" }

import groovy.transform.Field
import hubitat.zigbee.zcl.DataType

metadata {
    definition (
        name: "Third Reality RGBW Bulb (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/third_reality_rgbw_bulb_custom/third_reality_rgbw_bulb_custom.groovy"
    ) {
        capability "Actuator"
        capability "ChangeLevel"
        capability "ColorControl"
        capability "ColorTemperature"
        capability "Configuration"
        capability "Light"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "SwitchLevel"

        // Attributes
        attribute "colorMode", "string"
        attribute "colorName", "string"
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]

        // Custom Commands
        command "Health Check"
        command "resetDriver"
        command "toggle"
        command "updateFirmware"

        // Device Fingerprints for 3RCB01057Z
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", model: "3RCB01057Z", manufacturer: "Third Reality, Inc", controllerType: "ZGB"
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", model: "3Reality", controllerType: "ZGB"
    }

    preferences {
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

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    
    initializeHealthCheckPhase()
    sendEvent(name: "healthStatus", value: "unknown")
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "level", value: 100, unit: "%")
    sendEvent(name: "colorMode", value: "RGB")
    sendEvent(name: "colorTemperature", value: 2700, unit: "K")

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    
    if (!getSettingBool("logDebugEnable", false) && state.lastDebugEnabled == true) {
        logInfo "Debug logging disabled"
    }
    if (!getSettingBool("logTraceEnable", false) && state.lastTraceEnabled == true) {
        logInfo "Trace logging disabled"
    }
    
    state.lastDebugEnabled = getSettingBool("logDebugEnable", false)
    state.lastTraceEnabled = getSettingBool("logTraceEnable", false)

    initialize(false)
    runIn(1, "configure")
}

List<String> configure() {
    checkAndLogVersionDemarcation()
    logTrace "configure(): Configuring reporting thresholds..."
    
    initialize(false)
    List<String> cmds = []

    // On/Off Cluster (0x0006)
    cmds += zigbee.configureReporting(0x0006, 0x0000, DataType.BOOLEAN, 0, 3600, null)
    
    // Level Control Cluster (0x0008)
    cmds += zigbee.configureReporting(0x0008, 0x0000, DataType.UINT8, 1, 3600, 1)
    
    // Color Control Cluster (0x0300) - Color Temp (Attr 0x0007)
    cmds += zigbee.configureReporting(0x0300, 0x0007, DataType.UINT16, 1, 3600, 1)

    // Immediately execute Health Check to verify online state
    cmds += executeHealthCheck()
    
    runIn(2, "refresh")
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
        state.lastDebugEnabled = true
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
   COMMAND IMPLEMENTATIONS
   ========================================================================================= */

List<String> on() {
    logTrace "on(): Turning bulb on..."
    state.isDigital = true
    return zigbee.on()
}

List<String> off() {
    logTrace "off(): Turning bulb off..."
    state.isDigital = true
    return zigbee.off()
}

List<String> toggle() {
    logTrace "toggle(): Toggling bulb state..."
    state.isDigital = true
    return zigbee.command(0x0006, 0x02)
}

List<String> setLevel(value, rate = null) {
    logTrace "setLevel(): Setting level to ${value}% (rate: ${rate})"
    state.isDigital = true
    int levelVal = Math.max(1, Math.min(100, value as int))
    int durationSec = rate != null ? (rate as int) : 1
    return zigbee.setLevel(levelVal, durationSec)
}

List<String> startLevelChange(direction) {
    logTrace "startLevelChange(): Starting level change (${direction})..."
    state.isDigital = true
    int upDown = (direction == "down") ? 1 : 0
    return zigbee.command(0x0008, 0x05, zigbee.convertToHexString(upDown, 2), "32")
}

List<String> stopLevelChange() {
    logTrace "stopLevelChange(): Stopping level change..."
    state.isDigital = true
    return zigbee.command(0x0008, 0x07)
}

List<String> setColor(Map colorMap) {
    logTrace "setColor(): Setting color with map ${colorMap}"
    state.isDigital = true
    List<String> cmds = []
    
    if (colorMap.hue != null && colorMap.saturation != null) {
        int scaledHue = Math.round((colorMap.hue as int) * 2.54)
        int scaledSat = Math.round((colorMap.saturation as int) * 2.54)
        cmds += zigbee.command(0x0300, 0x06, zigbee.convertToHexString(scaledHue, 2), zigbee.convertToHexString(scaledSat, 2), "0A00")
        
        updateColorName(colorMap.hue as int, colorMap.saturation as int)
        updateAttribute("colorMode", "RGB")
    }
    if (colorMap.level != null) {
        cmds += setLevel(colorMap.level)
    }
    return cmds
}

List<String> setHue(hue) {
    logTrace "setHue(): Setting hue to ${hue}"
    state.isDigital = true
    int scaledHue = Math.round((hue as int) * 2.54)
    int currentSatVal = (device.currentValue("saturation") ?: 100) as int
    int scaledSat = Math.round(currentSatVal * 2.54)
    
    updateColorName(hue as int, currentSatVal)
    updateAttribute("colorMode", "RGB")
    return zigbee.command(0x0300, 0x06, zigbee.convertToHexString(scaledHue, 2), zigbee.convertToHexString(scaledSat, 2), "0A00")
}

List<String> setSaturation(saturation) {
    logTrace "setSaturation(): Setting saturation to ${saturation}"
    state.isDigital = true
    int currentHueVal = (device.currentValue("hue") ?: 0) as int
    int scaledHue = Math.round(currentHueVal * 2.54)
    int scaledSat = Math.round((saturation as int) * 2.54)
    
    updateColorName(currentHueVal, saturation as int)
    updateAttribute("colorMode", "RGB")
    return zigbee.command(0x0300, 0x06, zigbee.convertToHexString(scaledHue, 2), zigbee.convertToHexString(scaledSat, 2), "0A00")
}

List<String> setColorTemperature(colortemperature, level = null, transitionTime = null) {
    logTrace "setColorTemperature(): Setting color temp to ${colortemperature}K (level: ${level})"
    state.isDigital = true
    List<String> cmds = []
    
    int ctVal = Math.max(2000, Math.min(6500, colortemperature as int))
    int mireds = Math.round(1000000 / ctVal)
    int duration = transitionTime != null ? ((transitionTime as int) * 10) : 10
    
    cmds += zigbee.command(0x0300, 0x0A, zigbee.convertToHexString(mireds, 4), zigbee.convertToHexString(duration, 4))
    
    updateAttribute("colorTemperature", ctVal, "K")
    updateAttribute("colorMode", "CT")
    updateAttribute("colorName", "White")

    if (level != null) {
        cmds += setLevel(level, transitionTime)
    }
    return cmds
}

List<String> refresh() {
    logTrace "refresh(): Requesting attribute reads..."
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000) // Switch State
    cmds += zigbee.readAttribute(0x0008, 0x0000) // Level
    cmds += zigbee.readAttribute(0x0300, 0x0000) // Hue
    cmds += zigbee.readAttribute(0x0300, 0x0001) // Saturation
    cmds += zigbee.readAttribute(0x0300, 0x0007) // Color Temperature
    cmds += zigbee.readAttribute(0x0300, 0x0008) // Color Mode
    return cmds
}

List<String> updateFirmware() {
    logInfo "Checking for firmware updates..."
    return zigbee.updateFirmware()
}

/* =========================================================================================
   COLOR NAME HELPER ENGINE
   ========================================================================================= */

private void updateColorName(int hue, int saturation) {
    String name
    if (saturation < 15) {
        name = "White"
    } else {
        switch (hue) {
            case 0..3:     name = "Red"; break
            case 4..12:    name = "Orange"; break
            case 13..18:   name = "Yellow"; break
            case 19..28:   name = "Chartreuse"; break
            case 29..39:   name = "Green"; break
            case 40..49:   name = "Spring"; break
            case 50..58:   name = "Cyan"; break
            case 59..67:   name = "Azure"; break
            case 68..77:   name = "Blue"; break
            case 78..85:   name = "Violet"; break
            case 86..93:   name = "Magenta"; break
            case 94..98:   name = "Pink"; break
            case 99..100:  name = "Red"; break
            default:       name = "Custom"; break
        }
    }
    updateAttribute("colorName", name)
}

/* =========================================================================================
   HEALTH CHECK ROUTINE TEMPLATE
   ========================================================================================= */

void "Health Check"() {
    List<String> cmds = executeHealthCheck()
    if (cmds) sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

void executeHealthCheckScheduled() {
    logTrace "executeHealthCheckScheduled(): Triggering scheduled health check..."
    List<String> cmds = executeHealthCheck()
    if (cmds) sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

private List<String> executeHealthCheck() {
    logInfo "Executing Health Check..."
    state.healthCheckPending = true
    scheduleCommandTimeoutCheck()
    return zigbee.readAttribute(0x0000, 0x0000) // Read Basic Cluster ZCL Version (0x0000)
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
        case 60:  schedule("0 ${m} * ? * * *", methodToSchedule); break
        case 240: schedule("0 ${m} ${[0,4,8,12,16,20].collect{(it+h)%24}.sort().join(',')} ? * * *", methodToSchedule); break
        case 480: schedule("0 ${m} ${[0,8,16].collect{(it+h)%24}.sort().join(',')} ? * * *", methodToSchedule); break
        case 720: schedule("0 ${m} ${[0,12].collect{(it+h)%24}.sort().join(',')} ? * * *", methodToSchedule); break
        case 1440: schedule("0 ${m} ${h} ? * * *", methodToSchedule); break
        default:  schedule("0 */${intervalMin} * ? * * *", methodToSchedule); break
    }
}

private void scheduleCommandTimeoutCheck(final int delay = COMMAND_TIMEOUT) {
    runIn(delay, "deviceCommandTimeout", [overwrite: true])
}

void deviceCommandTimeout() {
    logWarn "Health Check failed: No response received within ${COMMAND_TIMEOUT} seconds (device offline?)"
    state.healthCheckPending = false
    updateAttribute("healthStatus", "offline")
}

/* =========================================================================================
   ZIGBEE MESSAGE PARSING
   ========================================================================================= */

void parse(String description) {
    logTrace "parse(): Inbound message received"
    logDebug "parse(): Raw description -> ${description}"
    if (!description) return

    Map descMap = zigbee.parseDescriptionAsMap(description)
    if (!descMap) return
    logDebug "parse(): Parsed description map -> ${descMap}"

    String clusterHex = descMap.cluster ?: (descMap.clusterInt != null ? zigbee.convertToHexString(descMap.clusterInt as Integer, 4) : "unknown")

    if (descMap.isClusterSpecific == false && descMap.command == "01") {
        markDeviceOnline(clusterHex)
        logDebug "parse(): Read attribute status response received for cluster 0x${clusterHex}: data=${descMap.data}"
        return
    }

    switch (descMap.clusterInt as Integer) {
        case 0x0000: // Basic Cluster (Health Check target)
            markDeviceOnline(clusterHex)
            logDebug "parse(): Basic cluster packet processed"
            break
            
        case 0x0006: // On/Off Cluster
            markDeviceOnline(clusterHex)
            if (descMap.attrInt == 0x0000 && descMap.value != null) {
                String switchVal = descMap.value == "01" ? "on" : "off"
                String typeVal = state.isDigital == true ? "digital" : "physical"
                state.remove("isDigital")
                updateAttribute("switch", switchVal, null, typeVal)
            }
            break

        case 0x0008: // Level Control Cluster
            markDeviceOnline(clusterHex)
            if (descMap.attrInt == 0x0000 && descMap.value != null) {
                int levelVal = Math.round(Integer.parseInt(descMap.value, 16) / 2.55)
                String typeVal = state.isDigital == true ? "digital" : "physical"
                state.remove("isDigital")
                updateAttribute("level", levelVal, "%", typeVal)
            }
            break

        case 0x0300: // Color Control Cluster
            markDeviceOnline(clusterHex)
            if (descMap.attrInt == 0x0000 && descMap.value != null) { // Hue
                int hueVal = Math.round(Integer.parseInt(descMap.value, 16) / 2.54)
                updateAttribute("hue", hueVal, null, "physical")
                int currentSat = (device.currentValue("saturation") ?: 100) as int
                updateColorName(hueVal, currentSat)
            } else if (descMap.attrInt == 0x0001 && descMap.value != null) { // Saturation
                int satVal = Math.round(Integer.parseInt(descMap.value, 16) / 2.54)
                updateAttribute("saturation", satVal, null, "physical")
                int currentHue = (device.currentValue("hue") ?: 0) as int
                updateColorName(currentHue, satVal)
            } else if (descMap.attrInt == 0x0007 && descMap.value != null) { // Color Temperature
                int mireds = Integer.parseInt(descMap.value, 16)
                if (mireds > 0) {
                    int ctVal = Math.round(1000000 / mireds)
                    updateAttribute("colorTemperature", ctVal, "K", "physical")
                }
            } else if (descMap.attrInt == 0x0008 && descMap.value != null) { // Color Mode
                String modeHex = descMap.value
                String modeStr = (modeHex == "02") ? "CT" : "RGB"
                updateAttribute("colorMode", modeStr)
            }
            break

        default:
            logDebug "parse(): Unhandled cluster 0x${descMap.clusterId} message -> ${descMap}"
            break
    }
}

private void markDeviceOnline(String clusterHex = "unknown") {
    if (state.healthCheckPending == true) {
        logInfo "Valid Health Check response verified on cluster 0x${clusterHex}"
        state.healthCheckPending = false
        unschedule("deviceCommandTimeout")
    }
    if (device.currentValue("healthStatus") != "online") {
        updateAttribute("healthStatus", "online")
    }
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

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
    final String valStr = value?.toString()
    final String currentVal = device.currentValue(attribute)?.toString()

    if (currentVal == valStr) return

    // High-resolution in-memory duplicate debounce guard (< 500ms)
    final String lastVal = state["last_${attribute}"]?.toString()
    final Long lastTime = state["lastTime_${attribute}"] as Long ?: 0L
    final Long now = now()

    if (lastVal == valStr && (now - lastTime) < 500) {
        logDebug "updateAttribute(): Suppressed rapid-fire duplicate ${attribute} event (${valStr}) received within ${now - lastTime}ms"
        return
    }

    state["last_${attribute}"] = valStr
    state["lastTime_${attribute}"] = now

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
    logInfo descriptionText
    sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastDebugEnabled = false
        logInfo "Debug logging disabled"
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    
    String settingKey
    switch (lowerLevel) {
        case "info":  settingKey = "logInfoEnable"; break
        case "error": settingKey = "logErrorEnable"; break
        case "warn":  settingKey = "logWarnEnable"; break
        case "debug": settingKey = "logDebugEnable"; break
        case "trace": settingKey = "logTraceEnable"; break
        default:      settingKey = "logInfoEnable"; break
    }

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

@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10