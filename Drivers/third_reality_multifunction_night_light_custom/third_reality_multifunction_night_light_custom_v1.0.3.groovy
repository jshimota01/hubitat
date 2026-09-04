/**
 * Third Reality Night Light (Custom)
 * Device Driver for Hubitat Elevation
 * Third Reality Multifunction Night Light - model 3RSNL02043Z
 *
 * Purpose:
 * Custom multi-function driver for Third Reality 3RSNL02043Z Zigbee RGB Night Light, 
 * Motion Sensor, and Illuminance/Lux Sensor. Features standardized phase-anchored 
 * health tracking, color/level control, motion parsing via private cluster 0xFC00, 
 * illuminance measurement, and human-readable colorName tracking.
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
 * v1.0.3    09/04/26    jshimota    Refined Cluster 0x0400 illuminance parsing calculation to properly handle endianness conversion against firmware v1.00.86 raw reports.
 * v1.0.2    09/04/26    jshimota    Added colorName and colorMode attributes, along with automatic hue/saturation-to-colorName calculation engine.
 * v1.0.1    09/04/26    jshimota    Implemented private cluster 0xFC00 motion parsing, illuminance conversion, ColorControl, and standardized active Health Check architecture.
 * v1.0.0    09/04/26    jshimota    Initial baseline release for Third Reality 3RSNL02043Z night light driver development.
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.0.3' }
def timeStamp() { return "2026/09/04 10:15 AM" }

import groovy.transform.Field
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType

metadata {
    definition (
        name: "Third Reality Night Light (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/third_reality_night_light/third_reality_night_light.groovy"
    ) {
        capability "Actuator"
        capability "ColorControl"
        capability "Configuration"
        capability "IlluminanceMeasurement"
        capability "Light"
        capability "MotionSensor"
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

        // Device Fingerprints for 3RSNL02043Z
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0004,0005,0006,0008,0012,0300,0400,1000,FC00", outClusters: "0019", model: "3RSNL02043Z", manufacturer: "Third Reality, Inc", controllerType: "ZGB"
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0003,0004,0005,0006,0008,0012,0300,0400,1000,FC00", outClusters: "0019", model: "3RSNL02043Z", manufacturer: "3Reality", controllerType: "ZGB"
    }

    preferences {
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver executes a Health Check to verify device online status.<br><b>Note:</b> This is a custom driver routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"
        input name: "luxOffset", type: "number", title: "<b>Lux Calibration Offset</b>", description: "<i>Adjust reported lux value by adding (+) or subtracting (-) a fixed integer.</i>", defaultValue: 0

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
    sendEvent(name: "motion", value: "inactive")
    sendEvent(name: "illuminance", value: 0, unit: "lx")
    sendEvent(name: "colorMode", value: "RGB")

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
    logInfo "Configuring reporting and attributes..."
    
    initialize(false)
    List<String> cmds = []
    
    // On/Off Cluster (0x0006)
    cmds += zigbee.configureReporting(0x0006, 0x0000, DataType.BOOLEAN, 0, 3600, null)
    
    // Level Control Cluster (0x0008)
    cmds += zigbee.configureReporting(0x0008, 0x0000, DataType.UINT8, 1, 3600, 1)
    
    // Illuminance Measurement Cluster (0x0400)
    cmds += zigbee.configureReporting(0x0400, 0x0000, DataType.UINT16, 10, 3600, 5)
    
    // IAS Zone Enrollment Response
    cmds += zigbee.enrollResponse()
    
    // Immediately execute Health Check to verify online state
    cmds += executeHealthCheck()
    
    runIn(2, "refresh")
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
   COMMAND IMPLEMENTATIONS
   ========================================================================================= */

List<String> on() {
    logInfo "Turning light on..."
    state.isDigital = true
    return zigbee.on()
}

List<String> off() {
    logInfo "Turning light off..."
    state.isDigital = true
    return zigbee.off()
}

List<String> toggle() {
    logInfo "Toggling light..."
    state.isDigital = true
    return zigbee.command(0x0006, 0x02)
}

List<String> setLevel(value, rate = null) {
    logInfo "Setting level to ${value}%"
    state.isDigital = true
    int scaledLevel = Math.round((value as int) * 2.55)
    int duration = rate != null ? (rate as int) * 10 : 10
    return zigbee.command(0x0008, 0x04, zigbee.convertToHexString(scaledLevel, 2), zigbee.convertToHexString(duration, 4))
}

List<String> setColor(Map colorMap) {
    logInfo "Setting color to ${colorMap}"
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
    logInfo "Setting hue to ${hue}"
    state.isDigital = true
    int scaledHue = Math.round((hue as int) * 2.54)
    int currentSatVal = (device.currentValue("saturation") ?: 100) as int
    int scaledSat = Math.round(currentSatVal * 2.54)
    
    updateColorName(hue as int, currentSatVal)
    updateAttribute("colorMode", "RGB")
    return zigbee.command(0x0300, 0x06, zigbee.convertToHexString(scaledHue, 2), zigbee.convertToHexString(scaledSat, 2), "0A00")
}

List<String> setSaturation(saturation) {
    logInfo "Setting saturation to ${saturation}"
    state.isDigital = true
    int currentHueVal = (device.currentValue("hue") ?: 0) as int
    int scaledHue = Math.round(currentHueVal * 2.54)
    int scaledSat = Math.round((saturation as int) * 2.54)
    
    updateColorName(currentHueVal, saturation as int)
    updateAttribute("colorMode", "RGB")
    return zigbee.command(0x0300, 0x06, zigbee.convertToHexString(scaledHue, 2), zigbee.convertToHexString(scaledSat, 2), "0A00")
}

List<String> refresh() {
    logDebug "Executing refresh()..."
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0006, 0x0000) // Switch State
    cmds += zigbee.readAttribute(0x0008, 0x0000) // Level
    cmds += zigbee.readAttribute(0x0300, 0x0000) // Hue
    cmds += zigbee.readAttribute(0x0300, 0x0001) // Saturation
    cmds += zigbee.readAttribute(0x0400, 0x0000) // Lux
    cmds += zigbee.readAttribute(0x0500, 0x0002) // Motion IAS Status
    return cmds
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

List<String> "Health Check"() {
    return executeHealthCheck()
}

void executeHealthCheckScheduled() {
    List<String> cmds = executeHealthCheck()
    if (cmds) sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

private List<String> executeHealthCheck() {
    logDebug "Executing Health Check..."
    scheduleCommandTimeoutCheck()
    return zigbee.readAttribute(0x0000, 0x0000) // Read Basic Cluster ZCL Version
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
    logWarn "No Health Check response received (device offline?)"
    updateAttribute("healthStatus", "offline")
}

/* =========================================================================================
   ZIGBEE MESSAGE PARSING
   ========================================================================================= */

void parse(String description) {
    logDebug "parse(): ${description}"
    if (!description) return

    // Any incoming packet clears timeout and marks device online
    unschedule("deviceCommandTimeout")
    updateAttribute("healthStatus", "online")

    if (description.startsWith("zone status")) {
        parseIasZoneStatus(description)
        return
    }

    if (description.startsWith("enroll request")) {
        logDebug "Handling IAS zone enrollment request"
        sendHubCommand(new hubitat.device.HubMultiAction(zigbee.enrollResponse(), hubitat.device.Protocol.ZIGBEE))
        return
    }

    Map descMap = zigbee.parseDescriptionAsMap(description)
    if (!descMap) return
    logTrace "Parsed description map -> ${descMap}"

    switch (descMap.clusterInt as Integer) {
        case 0x0000: // Basic Cluster
            logDebug "Basic cluster message received: ${descMap}"
            break
            
        case 0x0006: // On/Off Cluster
            if (descMap.attrInt == 0x0000 && descMap.value != null) {
                String switchVal = descMap.value == "01" ? "on" : "off"
                String typeVal = state.isDigital == true ? "digital" : "physical"
                state.remove("isDigital")
                updateAttribute("switch", switchVal, null, typeVal)
            }
            break

        case 0x0008: // Level Control Cluster
            if (descMap.attrInt == 0x0000 && descMap.value != null) {
                int levelVal = Math.round(Integer.parseInt(descMap.value, 16) / 2.55)
                String typeVal = state.isDigital == true ? "digital" : "physical"
                state.remove("isDigital")
                updateAttribute("level", levelVal, "%", typeVal)
            }
            break

        case 0x0300: // Color Control Cluster
            if (descMap.attrInt == 0x0000 && descMap.value != null) { // Hue
                int hueVal = Math.round(Integer.parseInt(descMap.value, 16) / 2.54)
                updateAttribute("hue", hueVal, null, "physical")
                int currentSat = (device.currentValue("saturation") ?: 100) as int
                updateColorName(hueVal, currentSat)
                updateAttribute("colorMode", "RGB")
            } else if (descMap.attrInt == 0x0001 && descMap.value != null) { // Saturation
                int satVal = Math.round(Integer.parseInt(descMap.value, 16) / 2.54)
                updateAttribute("saturation", satVal, null, "physical")
                int currentHue = (device.currentValue("hue") ?: 0) as int
                updateColorName(currentHue, satVal)
                updateAttribute("colorMode", "RGB")
            }
            break

        case 0x0400: // Illuminance Measurement Cluster
            if (descMap.attrInt == 0x0000 && descMap.value != null) {
                int rawValue = Integer.parseInt(descMap.value, 16)
                // Swap bytes if little-endian representation is passed as standard string
                if (rawValue > 0x3FFF && descMap.value.length() == 4) {
                    String swapped = descMap.value.substring(2, 4) + descMap.value.substring(0, 2)
                    rawValue = Integer.parseInt(swapped, 16)
                }
                int calculatedLux = (rawValue > 0) ? Math.round(Math.pow(10, (rawValue - 1) / 10000.0)) : 0
                int finalLux = Math.max(0, calculatedLux + (settings.luxOffset ?: 0))
                updateAttribute("illuminance", finalLux, "lx", "physical")
            }
            break

        case 0x0500: // IAS Zone Cluster
            if (descMap.attrInt == 0x0002 && descMap.value != null) {
                int val = Integer.parseInt(descMap.value, 16)
                updateAttribute("motion", (val & 0x01) != 0 ? "active" : "inactive", null, "physical")
            }
            break

        case 0xFC00: // Third Reality Private Cluster (Motion Sensor Payload)
            if (descMap.attrInt == 0x0002 && descMap.value != null) {
                String motionState = (descMap.value == "0001" || descMap.value == "01") ? "active" : "inactive"
                updateAttribute("motion", motionState, null, "physical")
            } else {
                logTrace "Third Reality Private Cluster 0xFC00 attribute 0x${descMap.attrId} payload: ${descMap.value}"
            }
            break

        default:
            logTrace "Unhandled cluster 0x${descMap.clusterId} message: ${descMap}"
            break
    }
}

private void parseIasZoneStatus(String description) {
    logDebug "parseIasZoneStatus(): ${description}"
    ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
    if (zoneStatus != null) {
        boolean isActive = zoneStatus.isAlarm1Set() || zoneStatus.isAlarm2Set()
        updateAttribute("motion", isActive ? "active" : "inactive", null, "physical")
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

@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10