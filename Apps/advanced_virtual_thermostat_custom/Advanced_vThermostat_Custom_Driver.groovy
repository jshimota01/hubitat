/**
 * Advanced Virtual Thermostat Device Driver (Custom)
 * Platform: Hubitat Elevation
 * Notes: Custom virtual thermostat device for joining temperature sensors with heating/cooling switch outlets
 * Capabilities: Thermostat, Sensor, TemperatureMeasurement, Refresh, Configuration
 **/
/**
 * Copyright 2026 James Shimota / Original 2020 Nelson Clark
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
 *  Purpose:
 *  Provides a virtual thermostat capability endpoint managed by the Advanced vThermostat Child (Custom) app.
 *
 *  Instructions:
 *  1. This device is automatically created and managed by the Advanced vThermostat Child (Custom) app.
 *  
 *  Changelog:
 *  v2.2.3    08/30/26    jshimota    Added input validation for threshold/interval, manual override safety for preEmergencyMode, and boundary checks
 *  v2.2.2    08/30/26    jshimota    Updated importUrl target to Apps directory on GitHub for co-located deployment
 *  v2.2.1    08/30/26    jshimota    Fixed bracket typo in cool() and method target in setMaxHeatTemp()
 *  v2.2.0    08/30/26    jshimota    Applied v1.0.4 Driver Master Template (centralized logging, sendIfChanged, NPE safe checks, debug timers)
 *  v2.1.1    08/30/26    jshimota    Formatted names to use (Custom) in parenthetical style
 *  v2.1.0    08/30/26    jshimota    Removed v2 identifiers, updated definition name & URLs
 *  v2.0.1    08/22/26    jshimota    Fixed evaluateMode() loop by assigning "none" to preEmergencyMode attribute
 *  v2.0.0    08/22/26    jshimota    Initial custom overhaul
 **/

import groovy.json.JsonOutput

static String version() { return '2.2.3' }
def timeStamp() { return "2026/08/30 10:28 AM" }

metadata {
    definition (
        name: "Advanced vThermostat Device (Custom)",
        namespace: "jshimota",
        author: "Nelson Clark / Customizations by jshimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat_Custom_Driver.groovy"
    ) {
        capability "Thermostat"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "Refresh"
        capability "Configuration"

        // Custom Attributes
        attribute "driverVersion", "string"
        attribute "thermostatThreshold", "number"
        attribute "minHeatTemp", "number"
        attribute "maxHeatTemp", "number"
        attribute "minCoolTemp", "number"
        attribute "maxCoolTemp", "number"
        attribute "lastTempUpdate", "number"
        attribute "supportedThermostatModes", "JSON_OBJECT"
        attribute "maxUpdateInterval", "number"
        attribute "minCoolingSetpoint", "number"     
        attribute "maxCoolingSetpoint", "number"     
        attribute "minHeatingSetpoint", "number"     
        attribute "maxHeatingSetpoint", "number"     
        attribute "thermostatTemperatureSetpoint", "number"
        attribute "preEmergencyMode", "string"

        // Custom Commands
        command "heatUp"
        command "heatDown"
        command "coolUp"
        command "coolDown"
        command "setMaxUpdateInterval", ["number"]
        command "resetDriver"
        command "setLogLevel", ["number"]
    }

    preferences {
        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// Scheduled Cron / Interval Setup Helper
def setupSchedule() {
    // Managed dynamically via evaluateMode runIn schedules
}

void parse(String description) {
    logDebug "parse(): ${description}"
}

// Hubitat Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating preferences..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
    
    def hubScale = getTemperatureScale()
    state.currentUnit = hubScale
    sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "cool", "auto", "off"])])
    updateThermostatSetpoint(hubScale)
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
    sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["auto", "cool", "heat", "off"])])
    return []
}

// Intentionally unsupported endpoint; required for Thermostat capability compatibility.
def refresh() {
    logDebug "Executing refresh() - No-op for virtual thermostat driver."
    return []
}

private void initialize(Boolean isInstall = false) {
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)

    def hubScale = getTemperatureScale()
    state.currentUnit = hubScale

    if (isInstall || device.currentValue("temperature") == null) {
        if (hubScale == "C") {
            sendIfChanged([name: "minCoolTemp", value: 15.5, unit: "C"])
            sendIfChanged([name: "minCoolingSetpoint", value: 15.5, unit: "C"])
            sendIfChanged([name: "maxCoolTemp", value: 35.0, unit: "C"])
            sendIfChanged([name: "maxCoolingSetpoint", value: 35.0, unit: "C"])
            sendIfChanged([name: "minHeatTemp", value: 1.5, unit: "C"])
            sendIfChanged([name: "minHeatingSetpoint", value: 1.5, unit: "C"])
            sendIfChanged([name: "maxHeatTemp", value: 26.5, unit: "C"])
            sendIfChanged([name: "maxHeatingSetpoint", value: 26.5, unit: "C"])
            sendIfChanged([name: "thermostatThreshold", value: 0.5, unit: "C"])
            sendIfChanged([name: "temperature", value: 22.0, unit: "C"])
            sendIfChanged([name: "heatingSetpoint", value: 21.0, unit: "C"])
            sendIfChanged([name: "coolingSetpoint", value: 24.5, unit: "C"])
        } else {
            sendIfChanged([name: "minCoolTemp", value: 60, unit: "F"])
            sendIfChanged([name: "minCoolingSetpoint", value: 60, unit: "F"])
            sendIfChanged([name: "maxCoolTemp", value: 95, unit: "F"])
            sendIfChanged([name: "maxCoolingSetpoint", value: 95, unit: "F"])
            sendIfChanged([name: "minHeatTemp", value: 35, unit: "F"])
            sendIfChanged([name: "minHeatingSetpoint", value: 35, unit: "F"])
            sendIfChanged([name: "maxHeatTemp", value: 80, unit: "F"])
            sendIfChanged([name: "maxHeatingSetpoint", value: 80, unit: "F"])
            sendIfChanged([name: "thermostatThreshold", value: 1.0, unit: "F"])
            sendIfChanged([name: "temperature", value: 72, unit: "F"])
            sendIfChanged([name: "heatingSetpoint", value: 70, unit: "F"])
            sendIfChanged([name: "coolingSetpoint", value: 76, unit: "F"])
        }
        
        updateThermostatSetpoint(hubScale)
        state.lastTempUpdate = now()
        sendIfChanged([name: "thermostatMode", value: "off"])
        sendIfChanged([name: "thermostatOperatingState", value: "idle"])
        sendIfChanged([name: "maxUpdateInterval", value: 65])
        sendIfChanged([name: "preEmergencyMode", value: "none"])
        sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "cool", "auto", "off"])])
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

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// Compatibility Helper for Legacy App Log Commands
def setLogLevel(level) {
    int lvl = level ? level.toInteger() : 3
    Boolean dbg = lvl >= 4
    Boolean trc = lvl >= 5
    device.updateSetting("logDebugEnable", [type: "bool", value: dbg])
    device.updateSetting("logTraceEnable", [type: "bool", value: trc])
    logWarn "Logging levels updated via app request -> Debug: ${dbg}, Trace: ${trc}"
}

// Master Utility Routine for Driver GUI Button
void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    logInfo "Driver reset process completed."
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
    logInfo "Clearing all scheduled jobs..."
    unschedule()
    logInfo "All scheduled jobs have been successfully cleared."
}

// Thermostat Core Logic Routines
def updateThermostatSetpoint(String units = null) {
    if (!units) units = getTemperatureScale()
    def mode = device.currentValue("thermostatMode") ?: "off"
    def targetVal
    
    if (mode == "cool") {
        targetVal = device.currentValue("coolingSetpoint") ?: (units == "C" ? 24.5 : 76.0)
    } else {
        targetVal = device.currentValue("heatingSetpoint") ?: (units == "C" ? 21.0 : 70.0)
    }
    
    sendIfChanged([name: "thermostatSetpoint", value: targetVal, unit: units])
    sendIfChanged([name: "thermostatTemperatureSetpoint", value: targetVal, unit: units])
}

def evaluateMode() {
    logTrace "evaluateMode() - START"
    runIn(60, 'evaluateMode')
    
    def temp = device.currentValue("temperature")
    def heatingSetpoint = device.currentValue("heatingSetpoint")
    def coolingSetpoint = device.currentValue("coolingSetpoint")
    def threshold = device.currentValue("thermostatThreshold")
    def current = device.currentValue("thermostatOperatingState")
    def mode = device.currentValue("thermostatMode")

    def nowMs = now()
    def lastUpdate = state.lastTempUpdate ?: nowMs
    def maxInterval = (device.currentValue("maxUpdateInterval") ?: 180) as Long
    if (maxInterval > 180) maxInterval = 180
    if (maxInterval < 1) maxInterval = 1
    
    def maxIntervalMili = maxInterval * 60000
    def preMode = device.currentValue("preEmergencyMode")

    if (current == "idle" && (nowMs - lastUpdate >= maxIntervalMili)) {
        logDebug "Temp sensor maximum update interval exceeded ($maxInterval mins). Thermostat idle."
    } else if (mode != "off" && current != "idle" && (nowMs - lastUpdate >= maxIntervalMili)) {
        logError "Temp sensor update timeout exceeded. Enforcing EMERGENCY STOP."
        sendIfChanged([name: "preEmergencyMode", value: mode])
        sendIfChanged([name: "thermostatMode", value: "off"])
        sendIfChanged([name: "thermostatOperatingState", value: "idle"])
        return
    } else if (preMode && preMode != "none" && preMode != "" && preMode != "null" && (nowMs - lastUpdate < maxIntervalMili)) {
        logWarn "Sensors reporting again. Autorecovered to previous mode: ${preMode}"
        sendIfChanged([name: "preEmergencyMode", value: "none"])
        setThermostatMode(preMode)
        return
    }

    def callFor = "idle"

    if (!threshold || threshold <= 0) {
        logError "evaluateMode() - Threshold not set or invalid: ${threshold}"
    } else {
        def units = getTemperatureScale()
        if (mode in ["heat", "emergency heat"]) {
            updateThermostatSetpoint(units)
            if ((current == "idle" && (heatingSetpoint - temp) > threshold) || (current == "heating" && (temp - heatingSetpoint) < threshold)) {
                callFor = "heating"
            }
        } else if (mode == "cool") {
            updateThermostatSetpoint(units)
            if ((temp - coolingSetpoint) >= threshold) callFor = "cooling"
        } else if (mode == "auto") {
            if (temp > coolingSetpoint) {
                updateThermostatSetpoint(units)
                if ((temp - coolingSetpoint) >= threshold) callFor = "cooling"
            } else {
                updateThermostatSetpoint(units)
                if ((heatingSetpoint - temp) >= threshold) callFor = "heating"
            }
        }
    }

    if (current != callFor) {
        logDebug "sendEvent : thermostatOperatingState = $callFor"
        sendIfChanged([name: "thermostatOperatingState", value: callFor])
    }
}

def emergencyStop() { 
    sendIfChanged([name: "preEmergencyMode", value: device.currentValue("thermostatMode")])
    setThermostatMode("off") 
}

// Maps directly to normal heat mode for capability compatibility
def emergencyHeat() { 
    setThermostatMode("heat") 
}

def setHeatingSetpoint(Object value) {
    if (value == null || !value.toString().isNumber()) return
    Double dVal = value.toString().toDouble()
    Double newHeatingSetpoint = roundDegrees(dVal)

    if (newHeatingSetpoint == device.currentValue("heatingSetpoint")) return

    def setpointDistance = (getTemperatureScale() == "C") ? 3.0 : 5.0
    def coolmax = device.currentValue("maxCoolTemp") ?: 95.0
    def heatmin = device.currentValue("minHeatTemp") ?: 35.0
    def heatmax = device.currentValue("maxHeatTemp") ?: 80.0
    def coolingSetpoint = device.currentValue("coolingSetpoint") ?: 76.0

    // Prevent heating setpoint from forcing cooling setpoint beyond coolmax
    if (newHeatingSetpoint > (coolmax - setpointDistance)) {
        logWarn "setHeatingSetpoint() requested ${newHeatingSetpoint} which would exceed cooling max boundary. Capping."
        newHeatingSetpoint = coolmax - setpointDistance
    }

    Double newCoolingSetpoint = null
    if (newHeatingSetpoint > (coolingSetpoint - setpointDistance)) {
        newCoolingSetpoint = newHeatingSetpoint + setpointDistance
    }
    
    if (newHeatingSetpoint > heatmax || newHeatingSetpoint < heatmin) {
        logWarn "setHeatingSetpoint() ignoring out of range value ($newHeatingSetpoint)"
        return
    }

    def units = getTemperatureScale()
    sendIfChanged([name: "heatingSetpoint", value: newHeatingSetpoint, unit: units])
    
    if (newCoolingSetpoint && newCoolingSetpoint <= coolmax) {
        sendIfChanged([name: "coolingSetpoint", value: newCoolingSetpoint, unit: units])
    }
    updateThermostatSetpoint(units)
    runIn(2, 'evaluateMode')
}

def setCoolingSetpoint(Object value) {
    if (value == null || !value.toString().isNumber()) return
    Double dVal = value.toString().toDouble()
    Double newCoolingSetpoint = roundDegrees(dVal)

    if (newCoolingSetpoint == device.currentValue("coolingSetpoint")) return

    def setpointDistance = (getTemperatureScale() == "C") ? 3.0 : 5.0
    def coolmin = device.currentValue("minCoolTemp") ?: 60.0
    def coolmax = device.currentValue("maxCoolTemp") ?: 95.0
    def heatmin = device.currentValue("minHeatTemp") ?: 35.0
    def heatingSetpoint = device.currentValue("heatingSetpoint") ?: 70.0

    // Prevent cooling setpoint from forcing heating setpoint below heatmin
    if (newCoolingSetpoint < (heatmin + setpointDistance)) {
        logWarn "setCoolingSetpoint() requested ${newCoolingSetpoint} which would violate heating minimum boundary. Capping."
        newCoolingSetpoint = heatmin + setpointDistance
    }

    Double newHeatingSetpoint = null
    if ((newCoolingSetpoint - setpointDistance) < heatingSetpoint) {
        newHeatingSetpoint = newCoolingSetpoint - setpointDistance
    }
    
    if (newCoolingSetpoint > coolmax || newCoolingSetpoint < coolmin) {
        logWarn "setCoolingSetpoint() ignoring out of range value ($newCoolingSetpoint)"
        return
    }

    def units = getTemperatureScale()
    sendIfChanged([name: "coolingSetpoint", value: newCoolingSetpoint, unit: units])
    if (newHeatingSetpoint && newHeatingSetpoint >= heatmin) {
        sendIfChanged([name: "heatingSetpoint", value: newHeatingSetpoint, unit: units])
    }
    updateThermostatSetpoint(units)
    runIn(2, 'evaluateMode')
}

def setThermostatThreshold(Object value) {
    if (value == null || !value.toString().isNumber()) return
    Double dVal = value.toString().toDouble()
    if (dVal <= 0) {
        logWarn "setThermostatThreshold() ignoring invalid non-positive value (${dVal}). Threshold must be > 0."
        return
    }
    if (dVal != device.currentValue("thermostatThreshold")) {
        sendIfChanged([name: "thermostatThreshold", value: dVal, unit: getTemperatureScale()])
        runIn(2, 'evaluateMode')
    }
}

def setMaxUpdateInterval(BigDecimal minutes) {
    if (minutes == null) return
    BigDecimal clampedMins = minutes
    if (clampedMins < 1) clampedMins = 1
    if (clampedMins > 180) clampedMins = 180

    if (clampedMins != device.currentValue("maxUpdateInterval")) {
        sendIfChanged([name: "maxUpdateInterval", value: clampedMins])
        runIn(2, 'evaluateMode')
    }
}

def setThermostatMode(String value) {
    if (value != device.currentValue("thermostatMode")) {
        // Clear emergency recovery marker if user manually changes mode while sensor is offline
        if (device.currentValue("preEmergencyMode") != "none") {
            logInfo "Manual thermostat mode change to '${value}' detected during offline emergency. Clearing preEmergencyMode."
            sendIfChanged([name: "preEmergencyMode", value: "none"])
        }
        sendIfChanged([name: "thermostatMode", value: value])
        updateThermostatSetpoint()
        runIn(2, 'evaluateMode')
    }
}

def off()  { setThermostatMode("off") }
def auto() { setThermostatMode("auto") }

def heat() {
    sendIfChanged([name: "lastRunningMode", value: "heat"])
    updateDataValue("lastRunningMode", "heat")
    setThermostatMode("heat")
}

def cool() {
    sendIfChanged([name: "lastRunningMode", value: "cool"])
    updateDataValue("lastRunningMode", "cool")
    setThermostatMode("cool")
}

def setTemperature(value) {
    if (value == null || !value.toString().isNumber()) {
        logWarn "setTemperature() received invalid temperature value: ${value}"
        return
    }
    Double dVal = value.toString().toDouble()
    def units = getTemperatureScale()
    sendIfChanged([name: "temperature", value: dVal, unit: units])
    state.lastTempUpdate = now()
    runIn(2, 'evaluateMode')
}

def heatUp() {
    Double ts = (device.currentValue("heatingSetpoint") ?: 70.0).toDouble()
    setHeatingSetpoint(ts + getThermostatResolution())
}

def heatDown() {
    Double ts = (device.currentValue("heatingSetpoint") ?: 70.0).toDouble()
    setHeatingSetpoint(ts - getThermostatResolution())
}

def coolUp() {
    Double ts = (device.currentValue("coolingSetpoint") ?: 76.0).toDouble()
    setCoolingSetpoint(ts + getThermostatResolution())
}

def coolDown() {
    Double ts = (device.currentValue("coolingSetpoint") ?: 76.0).toDouble()
    setCoolingSetpoint(ts - getThermostatResolution())
}

def setMinCoolTemp(Double value) {
    def units = getTemperatureScale()
    sendIfChanged([name: "minCoolTemp", value: value, unit: units])
    sendIfChanged([name: "minCoolingSetpoint", value: value, unit: units])
    if (device.currentValue("coolingSetpoint") < value) setCoolingSetpoint(value)
}

def setMaxCoolTemp(Double value) {
    def units = getTemperatureScale()
    sendIfChanged([name: "maxCoolTemp", value: value, unit: units])
    sendIfChanged([name: "maxCoolingSetpoint", value: value, unit: units])
    if (device.currentValue("coolingSetpoint") > value) setCoolingSetpoint(value)
}

def setMinHeatTemp(Double value) {
    def units = getTemperatureScale()
    sendIfChanged([name: "minHeatTemp", value: value, unit: units])
    sendIfChanged([name: "minHeatingSetpoint", value: value, unit: units])
    if (device.currentValue("heatingSetpoint") < value) setHeatingSetpoint(value)
}

def setMaxHeatTemp(Double value) {
    def units = getTemperatureScale()
    sendIfChanged([name: "maxHeatTemp", value: value, unit: units])
    sendIfChanged([name: "maxHeatingSetpoint", value: value, unit: units])
    if (device.currentValue("heatingSetpoint") > value) setHeatingSetpoint(value)
}

// Capability compatibility stubs required by Hubitat / Alexa integrations
def fanAuto() {}
def fanCirculate() {}
def fanOn() {}
def setSchedule() {}
def setThermostatFanMode(String value) {}

def getTemperatureScale() { return "${location?.temperatureScale ?: 'F'}" }
def getDisplayUnits() { return getTemperatureScale() == "C" ? "°C" : "°F" }
def getThermostatResolution() { return getTemperatureScale() == "C" ? 0.5 : 1.0 }

def convertToHubTempScale(Double value) {
    if (getTemperatureScale() == "C") {
        return roundDegrees((value - 32) * 5 / 9)
    } else {
        return roundDegrees((value * 9 / 5) + 32)
    }
}

def roundDegrees(Double value) {
    if (getTemperatureScale() == "C") { 
        return Math.round(value * 2.0) / 2.0
    } else {
        return Math.round(value).toDouble()
    }
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
        logTrace "${desc}"
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