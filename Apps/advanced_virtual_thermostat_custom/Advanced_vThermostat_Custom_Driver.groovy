/**
 * Advanced Virtual Thermostat Device Driver (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Custom virtual thermostat device for joining temperature sensors with heating/cooling switch outlets.
 * Features Thermostat Controller functionality, locked 'Auto' dashboard dual-setpoint display support,
 * custom health tracking, and maintenance routines.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability.
 * - Hubitat's native capability exposes an unwanted "Ping" UI control button
 *   and does not provide the phase-anchored scheduling, timeout guards, or trace 
 *   logging behavior required by this driver architecture.
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
 * Changelog:
 * v2.5.1    09/05/26    jshimota    Increased maximum heating setpoint limit (maxHeatTemp / maxHeatingSetpoint) from 80 °F to 90 °F (32.0 °C).
 * v2.5.0    09/05/26    jshimota    Implemented Option C (Hybrid Architecture): Enabled autonomous auto-evaluation in evaluateMode() when physicalThermostatMode is 'auto', promoted physicalThermostatMode to 'auto' on manual dashboard setpoint commands when locked to Auto, and retained explicit 'off' override capabilities for MEM safety routines.
 * v2.4.2    09/03/26    jshimota    Cleaned up UI language by purging leftover 'ping' terminology from preferences/descriptions in favor of Health Check. Hardened setThermostatMode() with strict validation and normalization against unexpected input values.
 * v2.4.1    09/03/26    jshimota    Removed invalid capability 'ThermostatController' to fix platform compilation error while maintaining all custom control commands.
 * v2.4.0    09/03/26    jshimota    Integrated standard driver template architecture (custom Health Check, healthStatus attribute, phase-anchored ping scheduler, command timeout checks, and master maintenance routines). Preserved locked Auto dashboard tile logic and virtual thermostat execution engine.
 * v2.3.1    09/03/26    jshimota    Added lockDashboardToAuto preference and physicalThermostatMode tracking to preserve dual-setpoint dashboard tiles.
 * v2.3.0    09/01/26    jshimota    Updated with Thermostat Controller functionality.
 **/

import groovy.json.JsonOutput
import groovy.transform.Field

static String version() { return '2.5.1' }
def timeStamp() { return "2026/09/05 03:00 PM" }

metadata {
    definition (
        name: "Advanced vThermostat Device (Custom)",
        namespace: "jshimota",
        author: "Nelson Clark / Customizations by jshimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat_Custom_Driver.groovy"
    ) {
        capability "Actuator"
        capability "Thermostat"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "Refresh"
        capability "Configuration"

        // Attributes
        attribute "driverVersion", "string"
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
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
        attribute "controllerState", "string"
        attribute "physicalThermostatMode", "string"

        // Custom Commands
        command "Health Check"
        command "heatUp"
        command "heatDown"
        command "coolUp"
        command "coolDown"
        command "controlHeat", ["string"]
        command "controlCool", ["string"]
        command "setMaxUpdateInterval", ["number"]
        command "resetDriver"
        command "setLogLevel", ["number"]
    }

    preferences {
        input name: "lockDashboardToAuto", type: "bool", title: "<b>Lock Dashboard Tile to 'Auto' Mode</b>", description: "<i>When enabled, locks thermostatMode attribute to 'auto' so dashboard tiles preserve dual heat/cool setpoint controls.</i>", defaultValue: true, required: true
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver executes a Health Check to verify temperature sensor updates.<br><b>Note:</b> This is a custom driver health check routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

void parse(String description) {
    logDebug "parse(): ${description}"
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    updateAttribute("driverVersion", version())
    
    initializeHealthCheckPhase()
    updateAttribute("healthStatus", "unknown")

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating preferences..."
    updateAttribute("driverVersion", version())
    
    initialize(false)
    
    def hubScale = getTemperatureScale()
    state.currentUnit = hubScale
    sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "cool", "auto", "off"])])
    updateThermostatSetpoint(hubScale)
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    updateAttribute("driverVersion", version())
    
    initialize(false)
    sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["auto", "cool", "heat", "off"])])
    
    List<String> cmds = []
    cmds += executePing()
    return cmds
}

def refresh() {
    logInfo "refresh() requested - Updating controller state."
    evaluateMode()
    return []
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    updateAttribute("driverVersion", version())

    if (device.currentValue("healthStatus") == null) {
        updateAttribute("healthStatus", "unknown")
    }

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
            sendIfChanged([name: "maxHeatTemp", value: 32.0, unit: "C"])
            sendIfChanged([name: "maxHeatingSetpoint", value: 32.0, unit: "C"])
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
            sendIfChanged([name: "maxHeatTemp", value: 90, unit: "F"])
            sendIfChanged([name: "maxHeatingSetpoint", value: 90, unit: "F"])
            sendIfChanged([name: "thermostatThreshold", value: 1.0, unit: "F"])
            sendIfChanged([name: "temperature", value: 72, unit: "F"])
            sendIfChanged([name: "heatingSetpoint", value: 70, unit: "F"])
            sendIfChanged([name: "coolingSetpoint", value: 76, unit: "F"])
        }
        
        updateThermostatSetpoint(hubScale)
        state.lastTempUpdate = now()

        if (getSettingBool("lockDashboardToAuto", true)) {
            sendIfChanged([name: "physicalThermostatMode", value: "auto"])
            sendIfChanged([name: "thermostatMode", value: "auto"])
        } else {
            sendIfChanged([name: "physicalThermostatMode", value: "off"])
            sendIfChanged([name: "thermostatMode", value: "off"])
        }

        sendIfChanged([name: "thermostatOperatingState", value: "idle"])
        sendIfChanged([name: "controllerState", value: "idle"])
        sendIfChanged([name: "maxUpdateInterval", value: 65])
        sendIfChanged([name: "preEmergencyMode", value: "none"])
        sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "cool", "auto", "off"])])
    } else {
        // Enforce updated 90 degree boundary on existing device installations
        if (hubScale == "C") {
            sendIfChanged([name: "maxHeatTemp", value: 32.0, unit: "C"])
            sendIfChanged([name: "maxHeatingSetpoint", value: 32.0, unit: "C"])
        } else {
            sendIfChanged([name: "maxHeatTemp", value: 90, unit: "F"])
            sendIfChanged([name: "maxHeatingSetpoint", value: 90, unit: "F"])
        }
    }

    final int interval = settings.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 480
    if (interval > 0) {
        scheduleHealthCheck("executePing", interval)
    } else {
        unschedule("executePing")
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
   HEALTH CHECK ROUTINE ARCHITECTURE (CUSTOM DRIVER HEALTH CHECK)
   ========================================================================================= */

List<String> "Health Check"() {
    return executePing()
}

private List<String> executePing() {
    logDebug "Executing Health Check..."
    scheduleCommandTimeoutCheck()
    
    def nowMs = now()
    def lastUpdate = state.lastTempUpdate ?: nowMs
    def maxInterval = (device.currentValue("maxUpdateInterval") ?: 180) as Long
    def maxIntervalMs = maxInterval * 60000

    if ((nowMs - lastUpdate) < maxIntervalMs) {
        unschedule("deviceCommandTimeout")
        updateAttribute("healthStatus", "online")
    } else {
        logWarn "Virtual thermostat temperature sensor health status is stale (last update > ${maxInterval} mins)."
    }

    return []
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
    unschedule("deviceCommandTimeout")
    runIn(delay, "deviceCommandTimeout")
}

void deviceCommandTimeout() {
    logWarn "No Health Check response received within timeout window (sensor offline?)"
    updateAttribute("healthStatus", "offline")
}

/* =========================================================================================
   VIRTUAL THERMOSTAT CORE CONTROL LOGIC
   ========================================================================================= */

def setLogLevel(level) {
    int lvl = level ? level.toInteger() : 3
    Boolean dbg = lvl >= 4
    Boolean trc = lvl >= 5
    device.updateSetting("logDebugEnable", [type: "bool", value: dbg])
    device.updateSetting("logTraceEnable", [type: "bool", value: trc])
    logWarn "Logging levels updated via app request -> Debug: ${dbg}, Trace: ${trc}"
}

def controlHeat(String action) {
    logInfo "Thermostat Controller driving Heating: ${action}"
    if (action?.toLowerCase() == "on") {
        sendIfChanged([name: "thermostatOperatingState", value: "heating"])
        sendIfChanged([name: "controllerState", value: "controlling heat"])
    } else {
        sendIfChanged([name: "thermostatOperatingState", value: "idle"])
        sendIfChanged([name: "controllerState", value: "idle"])
    }
}

def controlCool(String action) {
    logInfo "Thermostat Controller driving Cooling: ${action}"
    if (action?.toLowerCase() == "on") {
        sendIfChanged([name: "thermostatOperatingState", value: "cooling"])
        sendIfChanged([name: "controllerState", value: "controlling cool"])
    } else {
        sendIfChanged([name: "thermostatOperatingState", value: "idle"])
        sendIfChanged([name: "controllerState", value: "idle"])
    }
}

def updateThermostatSetpoint(String units = null) {
    if (!units) units = getTemperatureScale()
    def mode = device.currentValue("physicalThermostatMode") ?: "off"
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
    def mode = device.currentValue("physicalThermostatMode") ?: "off"

    def nowMs = now()
    def lastUpdate = state.lastTempUpdate ?: nowMs
    def maxInterval = (device.currentValue("maxUpdateInterval") ?: 180) as Long
    if (maxInterval > 180) maxInterval = 180
    if (maxInterval < 1) maxInterval = 1
    
    def maxIntervalMili = maxInterval * 60000
    def preMode = device.currentValue("preEmergencyMode")

    if (current == "idle" && (nowMs - lastUpdate >= maxIntervalMili)) {
        logDebug "Temp sensor maximum update interval exceeded ($maxInterval mins). Thermostat idle."
        updateAttribute("healthStatus", "offline")
    } else if (mode != "off" && current != "idle" && (nowMs - lastUpdate >= maxIntervalMili)) {
        logError "Temp sensor update timeout exceeded. Enforcing EMERGENCY STOP."
        updateAttribute("healthStatus", "offline")
        sendIfChanged([name: "preEmergencyMode", value: mode])
        setThermostatMode("off")
        sendIfChanged([name: "thermostatOperatingState", value: "idle"])
        sendIfChanged([name: "controllerState", value: "idle"])
        return
    } else if (preMode && preMode != "none" && preMode != "" && preMode != "null" && (nowMs - lastUpdate < maxIntervalMili)) {
        logWarn "Sensors reporting again. Autorecovered to previous mode: ${preMode}"
        updateAttribute("healthStatus", "online")
        sendIfChanged([name: "preEmergencyMode", value: "none"])
        setThermostatMode(preMode)
        return
    } else if ((nowMs - lastUpdate) < maxIntervalMili) {
        updateAttribute("healthStatus", "online")
    }

    def callFor = "idle"

    if (!threshold || threshold <= 0) {
        logError "evaluateMode() - Threshold not set or invalid: ${threshold}"
    } else {
        def units = getTemperatureScale()
        if (mode in ["heat", "emergency heat", "emergencyHeat"]) {
            updateThermostatSetpoint(units)
            if ((current == "idle" && (heatingSetpoint - temp) > threshold) || (current == "heating" && (temp - heatingSetpoint) < threshold)) {
                callFor = "heating"
            }
        } else if (mode == "cool") {
            updateThermostatSetpoint(units)
            if ((temp - coolingSetpoint) >= threshold) callFor = "cooling"
        } else if (mode == "auto") {
            updateThermostatSetpoint(units)
            if ((current == "idle" && (heatingSetpoint - temp) > threshold) || (current == "heating" && (temp - heatingSetpoint) < threshold)) {
                callFor = "heating"
            } else if ((current == "idle" && (temp - coolingSetpoint) >= threshold) || (current == "cooling" && (coolingSetpoint - temp) < threshold)) {
                callFor = "cooling"
            }
        }
    }

    if (current != callFor) {
        logDebug "sendEvent : thermostatOperatingState = $callFor"
        sendIfChanged([name: "thermostatOperatingState", value: callFor])
        sendIfChanged([name: "controllerState", value: callFor == "idle" ? "idle" : "controlling ${callFor}"])
    }
}

def emergencyStop() { 
    sendIfChanged([name: "preEmergencyMode", value: device.currentValue("physicalThermostatMode")])
    setThermostatMode("off") 
}

def emergencyHeat() { 
    setThermostatMode("heat") 
}

def setHeatingSetpoint(Object value) {
    if (value == null || !value.toString().isNumber()) return
    Double dVal = value.toString().toDouble()
    Double newHeatingSetpoint = roundDegrees(dVal)

    if (getSettingBool("lockDashboardToAuto", true) && device.currentValue("physicalThermostatMode") == "off") {
        logInfo "Dashboard setpoint command received while physical mode was OFF. Promoting physicalThermostatMode to 'auto'."
        sendIfChanged([name: "physicalThermostatMode", value: "auto"])
    }

    if (newHeatingSetpoint == device.currentValue("heatingSetpoint")) {
        runIn(1, 'evaluateMode')
        return
    }

    def setpointDistance = (getTemperatureScale() == "C") ? 3.0 : 5.0
    def coolmax = device.currentValue("maxCoolTemp") ?: 95.0
    def heatmin = device.currentValue("minHeatTemp") ?: 35.0
    def heatmax = device.currentValue("maxHeatTemp") ?: (getTemperatureScale() == "C" ? 32.0 : 90.0)
    def coolingSetpoint = device.currentValue("coolingSetpoint") ?: 76.0

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
    runIn(1, 'evaluateMode')
}

def setCoolingSetpoint(Object value) {
    if (value == null || !value.toString().isNumber()) return
    Double dVal = value.toString().toDouble()
    Double newCoolingSetpoint = roundDegrees(dVal)

    if (getSettingBool("lockDashboardToAuto", true) && device.currentValue("physicalThermostatMode") == "off") {
        logInfo "Dashboard setpoint command received while physical mode was OFF. Promoting physicalThermostatMode to 'auto'."
        sendIfChanged([name: "physicalThermostatMode", value: "auto"])
    }

    if (newCoolingSetpoint == device.currentValue("coolingSetpoint")) {
        runIn(1, 'evaluateMode')
        return
    }

    def setpointDistance = (getTemperatureScale() == "C") ? 3.0 : 5.0
    def coolmin = device.currentValue("minCoolTemp") ?: 60.0
    def coolmax = device.currentValue("maxCoolTemp") ?: 95.0
    def heatmin = device.currentValue("minHeatTemp") ?: 35.0
    def heatingSetpoint = device.currentValue("heatingSetpoint") ?: 70.0

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
    runIn(1, 'evaluateMode')
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
        runIn(1, 'evaluateMode')
    }
}

def setMaxUpdateInterval(BigDecimal minutes) {
    if (minutes == null) return
    BigDecimal clampedMins = minutes
    if (clampedMins < 1) clampedMins = 1
    if (clampedMins > 180) clampedMins = 180

    if (clampedMins != device.currentValue("maxUpdateInterval")) {
        sendIfChanged([name: "maxUpdateInterval", value: clampedMins])
        runIn(1, 'evaluateMode')
    }
}

def setThermostatMode(String value) {
    if (value == null) return
    
    String normMode = value.toString().trim()
    if (normMode == "emergency heat") normMode = "emergencyHeat"
    
    List<String> validModes = ["off", "heat", "cool", "auto", "emergencyHeat"]
    if (!(normMode in validModes)) {
        logError "setThermostatMode() received invalid thermostat mode: '${value}'. Request ignored."
        return
    }

    sendIfChanged([name: "physicalThermostatMode", value: normMode])

    if (getSettingBool("lockDashboardToAuto", true)) {
        sendIfChanged([name: "thermostatMode", value: "auto"])
    } else {
        sendIfChanged([name: "thermostatMode", value: normMode])
    }

    if (device.currentValue("preEmergencyMode") != "none" && normMode != "off") {
        logInfo "Manual thermostat mode change to '${normMode}' detected during offline emergency. Clearing preEmergencyMode."
        sendIfChanged([name: "preEmergencyMode", value: "none"])
    }
    
    updateThermostatSetpoint()
    runIn(1, 'evaluateMode')
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
    updateAttribute("healthStatus", "online")
    runIn(1, 'evaluateMode')
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
    clearAllSchedules()
    clearAllAttributes()
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

@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10