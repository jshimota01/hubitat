/**
 * Advanced Virtual Thermostat Device Driver (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Custom virtual thermostat device for joining temperature sensors with heating/cooling switch outlets.
 * Features Thermostat Controller functionality, locked 'Auto' dashboard dual-setpoint display support,
 * customizable ambient display precision, scale-aware setpoint separation, and smart dependency health tracking.
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
 * v2.10.1   10/12/26    jshimota    Synchronous Safety State Events: Enforced immediate synchronous attribute event generation in setSafetyMode() so querying safetyStatus immediately after setSafetyMode("cleared") returns "cleared" without event loop lag.
 * v2.10.0   10/12/26    jshimota    Safety Hold Thermostat Operating State Badge: Explicitly set thermostatOperatingState and controllerState to 'Safety Hold' during active safety interlock states. Preserved enum-compliant thermostatMode attribute so standard Hubitat Thermostat Tiles remain functional while displaying 'Safety Hold' on their main center badge.
 * v2.9.9    10/12/26    jshimota    Safety Interlock Hardening: Added explicit safetyStatus attribute and setSafetyMode() command validation (accepting strictly "active" or "cleared"). Deferred physical mode changes while safety is active. Preserved dashboard Auto mode tile locking during Safety Hold.
 * v2.9.8    10/10/26    jshimota    Ambient Temperature Precision Rendering Fix: Fixed setTemperature precision formatting to preserve exact trailing decimals (e.g., 72.0, 72.00) in temperature attribute events without Double string truncation.
 * v2.9.7    10/10/26    jshimota    GUI Cleanup & Precision Formatting Fix: Purged unnecessary controlCool and controlHeat GUI commands. Fixed Fahrenheit setpoint formatting to display whole integers without trailing .0 decimals. Fixed ambient temperature precision rendering in setTemperature and forced attribute refresh in updated().
 * v2.9.6    10/10/26    jshimota    GUI Cleanup & Logging Standardization: Purged redundant setLogLevel and setHealthStatus GUI commands from metadata and code routines. Standardized logging engine to match CentraLite Pearl driver logMessage implementation.
 * v2.9.5    10/10/26    jshimota    Scale-Aware Separation & Precision Refactor: Added tempPrecision preference (0, 1, 2 decimals) for ambient temperature presentation. Aligned setpointDistance in setHeatingSetpoint and setCoolingSetpoint to scale-aware constants (2.0 °F / 1.0 °C). Enforced standard closest-integer rounding (Math.round) for Fahrenheit setpoints.
 * v2.9.2    09/24/26    jshimota    Updated sendIfChanged to evaluate args.isStateChange correctly. Standardized driverVersion attribute updates to use sendIfChanged across lifecycle methods.
 * v2.9.1    09/24/26    jshimota    Added standard checkAndLogVersionDemarcation trace demarcation output across installed, updated, and configure lifecycle hooks.
 * v2.9.0    09/24/26    jshimota    Enforced strict presentation locking under lockDashboardToAuto = true. Keeps thermostatMode statically locked to 'auto' across all internal physicalThermostatMode transitions, ensuring dashboard tiles retain constant dual-setpoint UI presentation without style flips.
 * v2.8.0    09/24/26    jshimota    Purged 65-minute passive sensor watchdog timers and background scheduled loops. Added setHealthStatus command allowing the Child App Dependency Health Engine to push calculated online/offline health states directly.
 * v2.7.0    09/23/26    jshimota    Ripped out active scheduled health checks/Ping commands and converted healthStatus to a passive sensor update watchdog evaluated inside evaluateMode().
 * v2.6.0    09/18/26    jshimota    Prevented physical mode promotion to 'heat'/'auto' on setpoint commands received while OFF. Restricted sensor update timeout Emergency Stop enforcement strictly to active heating/cooling states to eliminate idle timeout loops and unwanted preEmergencyMode autorecovery cycles. Added enableEmergencyStopOnTimeout toggle preference.
 * v2.5.2    09/06/26    jshimota    Promoted physicalThermostatMode to 'heat' inside setHeatingSetpoint when mode is OFF to ensure manual setpoint changes drive outlets correctly.
 * v2.5.1    09/05/26    jshimota    Increased maximum heating setpoint limit (maxHeatTemp / maxHeatingSetpoint) from 80 °F to 90 °F (32.0 °C).
 * v2.5.0    09/05/26    jshimota    Implemented Option C (Hybrid Architecture): Enabled autonomous auto-evaluation in evaluateMode() when physicalThermostatMode is 'auto', promoted physicalThermostatMode to 'auto' on manual dashboard setpoint commands when locked to Auto, and retained explicit 'off' override capabilities for MEM safety routines.
 * v2.4.2    09/03/26    jshimota    Cleaned up UI language by purging leftover 'ping' terminology from preferences/descriptions in favor of Health Check. Hardened setThermostatMode() with strict validation and normalization against unexpected input values.
 * v2.4.1    09/03/26    jshimota    Removed invalid capability 'ThermostatController' to fix platform compilation error while maintaining all custom control commands.
 * v2.4.0    09/03/26    jshimota    Integrated standard driver template architecture (custom Health Check, healthStatus attribute, phase-anchored ping scheduler, command timeout checks, and master maintenance routines). Preserved locked Auto dashboard tile logic and virtual thermostat execution engine.
 * v2.3.1    09/03/26    jshimota    Added lockDashboardToAuto preference and physicalThermostatMode tracking to preserve dual-setpoint dashboard tiles.
 * v2.3.0    09/01/26    jshimota    Updated with Thermostat Controller functionality.
 * v2.2.0    08/30/26    jshimota    Applied initial Driver Master Template architecture and updated attribute definitions.
 * v2.1.1    08/30/26    jshimota    Formatted names to use (Custom) parenthetical naming standard.
 * v2.1.0    08/30/26    jshimota    Removed v2 identifiers, updated namespace to jshimota, and corrected GitHub import URLs.
 * v2.0.0    08/22/26    jshimota    Forked and bumped definition name to v2 for Hubitat Elevation platform testing.
 * v1.1.0    04/15/21    NelsonClark Added support for threshold hysteresis buffers and multi-sensor temperature averaging.
 * v1.0.0    12/03/20    NelsonClark Initial public release of Advanced Virtual Thermostat Device Driver.
 **/

import groovy.json.JsonOutput
import groovy.transform.Field
import java.math.RoundingMode

static String version() { return '2.10.1' }
def timeStamp() { return "2026/10/12 02:00 PM" }

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
        attribute "safetyStatus", "enum", ["cleared", "active"]
        attribute "thermostatThreshold", "number"
        attribute "minHeatTemp", "number"
        attribute "maxHeatTemp", "number"
        attribute "minCoolTemp", "number"
        attribute "maxCoolTemp", "number"
        attribute "supportedThermostatModes", "JSON_OBJECT"
        attribute "minCoolingSetpoint", "number"     
        attribute "maxCoolingSetpoint", "number"     
        attribute "minHeatingSetpoint", "number"     
        attribute "maxHeatingSetpoint", "number"     
        attribute "thermostatTemperatureSetpoint", "number"
        attribute "controllerState", "string"
        attribute "physicalThermostatMode", "string"

        // Custom Commands
        command "setSafetyMode", ["string"]
        command "heatUp"
        command "heatDown"
        command "coolUp"
        command "coolDown"
        command "resetDriver"
    }

    preferences {
        input name: "lockDashboardToAuto", type: "bool", title: "<b>Lock Dashboard Tile to 'Auto' Mode</b>", description: "<i>When enabled, locks thermostatMode attribute to 'auto' so dashboard tiles preserve dual heat/cool setpoint controls.</i>", defaultValue: true, required: true
        input name: "tempPrecision", type: "enum", title: "<b>Device Temperature Display Precision</b>", options: ["0": "0 Decimals (Whole Number - e.g., 72)", "1": "1 Decimal (e.g., 71.8)", "2": "2 Decimals (e.g., 71.82)"], defaultValue: "1", description: "<i>Controls decimal precision for reported device ambient temperature attribute only.</i>"

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
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
    sendIfChanged([name: "driverVersion", value: version()])
    if (device.currentValue("healthStatus") == null) {
        sendIfChanged([name: "healthStatus", value: "online"])
    }
    if (device.currentValue("safetyStatus") == null) {
        sendIfChanged([name: "safetyStatus", value: "cleared"])
    }
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating preferences..."
    sendIfChanged([name: "driverVersion", value: version()])
    
    initialize(false)
    
    def hubScale = getTemperatureScale()
    state.currentUnit = hubScale
    sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "cool", "auto", "off"])])
    
    // Re-apply ambient temperature precision on existing temperature reading
    def currentTempRaw = device.currentValue("temperature")
    if (currentTempRaw != null) {
        setTemperature(currentTempRaw)
    }
    
    updateThermostatSetpoint(hubScale)
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    sendIfChanged([name: "driverVersion", value: version()])
    
    initialize(false)
    sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["auto", "cool", "heat", "off"])])
    
    evaluateMode()
    return []
}

def refresh() {
    logInfo "refresh() requested - Updating controller state."
    evaluateMode()
    return []
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    sendIfChanged([name: "driverVersion", value: version()])

    if (device.currentValue("healthStatus") == null) {
        sendIfChanged([name: "healthStatus", value: "online"])
    }
    if (device.currentValue("safetyStatus") == null) {
        sendIfChanged([name: "safetyStatus", value: "cleared"])
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

        if (getSettingBool("lockDashboardToAuto", true)) {
            sendIfChanged([name: "physicalThermostatMode", value: "off"])
            sendIfChanged([name: "thermostatMode", value: "auto"])
        } else {
            sendIfChanged([name: "physicalThermostatMode", value: "off"])
            sendIfChanged([name: "thermostatMode", value: "off"])
        }

        sendIfChanged([name: "thermostatOperatingState", value: "idle"])
        sendIfChanged([name: "controllerState", value: "idle"])
        sendIfChanged([name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "cool", "auto", "off"])])
    } else {
        if (hubScale == "C") {
            sendIfChanged([name: "maxHeatTemp", value: 32.0, unit: "C"])
            sendIfChanged([name: "maxHeatingSetpoint", value: 32.0, unit: "C"])
        } else {
            sendIfChanged([name: "maxHeatTemp", value: 90, unit: "F"])
            sendIfChanged([name: "maxHeatingSetpoint", value: 90, unit: "F"])
        }
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
   VIRTUAL THERMOSTAT CORE CONTROL LOGIC
   ========================================================================================= */

def setSafetyMode(String mode) {
    if (mode == null) return
    String normMode = mode.toString().trim().toLowerCase()
    
    if (!(normMode in ["active", "cleared"])) {
        logError "setSafetyMode() received invalid safety mode string: '${mode}'. Strictly accepting 'active' or 'cleared'."
        return
    }

    if (normMode == "active") {
        logWarn "Safety mode ACTIVE. Forcing physical thermostat mode to OFF and holding idle."
        sendIfChanged([name: "physicalThermostatMode", value: "off"])
        sendIfChanged([name: "thermostatOperatingState", value: "Safety Hold"])
        sendIfChanged([name: "controllerState", value: "Safety Hold"])
        sendIfChanged([name: "safetyStatus", value: "active", isStateChange: true])
    } else {
        logInfo "Safety mode CLEARED. Resuming normal thermostat evaluation."
        sendIfChanged([name: "safetyStatus", value: "cleared", isStateChange: true])
        sendIfChanged([name: "thermostatOperatingState", value: "idle"])
        sendIfChanged([name: "controllerState", value: "idle"])
        evaluateMode()
    }
}

def updateThermostatSetpoint(String units = null) {
    if (!units) units = getTemperatureScale()
    def mode = device.currentValue("physicalThermostatMode") ?: "off"
    def targetVal
    
    if (mode == "cool") {
        targetVal = device.currentValue("coolingSetpoint") ?: (units == "C" ? 24.5 : 76)
    } else {
        targetVal = device.currentValue("heatingSetpoint") ?: (units == "C" ? 21.0 : 70)
    }
    
    if (units == "F" && targetVal instanceof Number) {
        targetVal = Math.round(targetVal.toDouble()) as Integer
    }
    
    sendIfChanged([name: "thermostatSetpoint", value: targetVal, unit: units])
    sendIfChanged([name: "thermostatTemperatureSetpoint", value: targetVal, unit: units])
}

def evaluateMode() {
    logTrace "evaluateMode() - START"

    String isSafetyActive = device.currentValue("safetyStatus") ?: "cleared"
    if (isSafetyActive == "active") {
        if (device.currentValue("thermostatOperatingState") != "Safety Hold") {
            sendIfChanged([name: "thermostatOperatingState", value: "Safety Hold"])
            sendIfChanged([name: "controllerState", value: "Safety Hold"])
        }
        return
    }
    
    def tempRaw = device.currentValue("temperature")
    if (tempRaw == null || !tempRaw.toString().isNumber()) return
    Double temp = tempRaw.toString().toDouble()

    def heatingSetpoint = device.currentValue("heatingSetpoint")
    def coolingSetpoint = device.currentValue("coolingSetpoint")
    def threshold = device.currentValue("thermostatThreshold")
    def current = device.currentValue("thermostatOperatingState")
    def mode = device.currentValue("physicalThermostatMode") ?: "off"

    if (mode == "off") {
        if (current != "idle") {
            sendIfChanged([name: "thermostatOperatingState", value: "idle"])
            sendIfChanged([name: "controllerState", value: "idle"])
        }
        return
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
    setThermostatMode("off") 
}

def emergencyHeat() { 
    setThermostatMode("heat") 
}

def setHeatingSetpoint(Object value) {
    if (value == null || !value.toString().isNumber()) return
    Double dVal = value.toString().toDouble()
    Object newHeatingSetpoint = roundDegrees(dVal)

    if (getTemperatureScale() == "F") {
        newHeatingSetpoint = Math.round(newHeatingSetpoint.toDouble()) as Integer
    }

    if (newHeatingSetpoint == device.currentValue("heatingSetpoint")) {
        evaluateMode()
        return
    }

    def setpointDistance = (getTemperatureScale() == "C") ? 1.0 : 2.0
    def coolmax = device.currentValue("maxCoolTemp") ?: 95.0
    def heatmin = device.currentValue("minHeatTemp") ?: 35.0
    def heatmax = device.currentValue("maxHeatTemp") ?: (getTemperatureScale() == "C" ? 32.0 : 90.0)
    def coolingSetpoint = device.currentValue("coolingSetpoint") ?: 76.0

    if (newHeatingSetpoint > (coolmax - setpointDistance)) {
        logWarn "setHeatingSetpoint() requested ${newHeatingSetpoint} which would exceed cooling max boundary. Capping."
        newHeatingSetpoint = coolmax - setpointDistance
    }

    Object newCoolingSetpoint = null
    if (newHeatingSetpoint > (coolingSetpoint - setpointDistance)) {
        newCoolingSetpoint = newHeatingSetpoint + setpointDistance
    }

    if (getTemperatureScale() == "F") {
        newHeatingSetpoint = Math.round((newHeatingSetpoint as Number).toDouble()) as Integer
        if (newCoolingSetpoint != null) {
            newCoolingSetpoint = Math.round((newCoolingSetpoint as Number).toDouble()) as Integer
        }
    }
    
    if (newHeatingSetpoint > heatmax || newHeatingSetpoint < heatmin) {
        logWarn "setHeatingSetpoint() ignoring out of range value ($newHeatingSetpoint)"
        return
    }

    def units = getTemperatureScale()
    sendIfChanged([name: "heatingSetpoint", value: newHeatingSetpoint, unit: units])
    
    if (newCoolingSetpoint != null && newCoolingSetpoint <= coolmax) {
        sendIfChanged([name: "coolingSetpoint", value: newCoolingSetpoint, unit: units])
    }
    updateThermostatSetpoint(units)
    evaluateMode()
}

def setCoolingSetpoint(Object value) {
    if (value == null || !value.toString().isNumber()) return
    Double dVal = value.toString().toDouble()
    Object newCoolingSetpoint = roundDegrees(dVal)

    if (getTemperatureScale() == "F") {
        newCoolingSetpoint = Math.round(newCoolingSetpoint.toDouble()) as Integer
    }

    if (newCoolingSetpoint == device.currentValue("coolingSetpoint")) {
        evaluateMode()
        return
    }

    def setpointDistance = (getTemperatureScale() == "C") ? 1.0 : 2.0
    def coolmin = device.currentValue("minCoolTemp") ?: 60.0
    def coolmax = device.currentValue("maxCoolTemp") ?: 95.0
    def heatmin = device.currentValue("minHeatTemp") ?: 35.0
    def heatingSetpoint = device.currentValue("heatingSetpoint") ?: 70.0

    if (newCoolingSetpoint < (heatmin + setpointDistance)) {
        logWarn "setCoolingSetpoint() requested ${newCoolingSetpoint} which would violate heating minimum boundary. Capping."
        newCoolingSetpoint = heatmin + setpointDistance
    }

    Object newHeatingSetpoint = null
    if ((newCoolingSetpoint - setpointDistance) < heatingSetpoint) {
        newHeatingSetpoint = newCoolingSetpoint - setpointDistance
    }

    if (getTemperatureScale() == "F") {
        newCoolingSetpoint = Math.round((newCoolingSetpoint as Number).toDouble()) as Integer
        if (newHeatingSetpoint != null) {
            newHeatingSetpoint = Math.round((newHeatingSetpoint as Number).toDouble()) as Integer
        }
    }
    
    if (newCoolingSetpoint > coolmax || newCoolingSetpoint < coolmin) {
        logWarn "setCoolingSetpoint() ignoring out of range value ($newCoolingSetpoint)"
        return
    }

    def units = getTemperatureScale()
    sendIfChanged([name: "coolingSetpoint", value: newCoolingSetpoint, unit: units])
    if (newHeatingSetpoint != null && newHeatingSetpoint >= heatmin) {
        sendIfChanged([name: "heatingSetpoint", value: newHeatingSetpoint, unit: units])
    }
    updateThermostatSetpoint(units)
    evaluateMode()
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
        evaluateMode()
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

    String isSafetyActive = device.currentValue("safetyStatus") ?: "cleared"
    if (isSafetyActive == "active" && normMode != "off") {
        logWarn "setThermostatMode(${normMode}) blocked while Safety Hold is active. Physical mode remains OFF."
        sendIfChanged([name: "physicalThermostatMode", value: "off"])
        return
    }

    sendIfChanged([name: "physicalThermostatMode", value: normMode])

    if (getSettingBool("lockDashboardToAuto", true)) {
        sendIfChanged([name: "thermostatMode", value: "auto"])
    } else {
        sendIfChanged([name: "thermostatMode", value: normMode])
    }
    
    updateThermostatSetpoint()
    evaluateMode()
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
    int precision = settings?.tempPrecision != null ? settings.tempPrecision.toInteger() : 1
    
    Object formattedValue
    if (precision == 0) {
        formattedValue = Math.round(dVal) as Integer
    } else {
        formattedValue = BigDecimal.valueOf(dVal).setScale(precision, RoundingMode.HALF_UP)
    }

    def units = getTemperatureScale()
    sendIfChanged([name: "temperature", value: formattedValue, unit: units, isStateChange: true])
    evaluateMode()
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
   MASTER UTILITY ROUTINES & LOGGING ENGINE (CENTRALITE PATTERN)
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

    if (oldVal != newVal || args.isStateChange == true) {
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
    sendIfChanged([name: attribute, value: value, unit: unit, type: type])
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
    return (settings && settings[key] != null) ? settings[key] as Boolean : defaultVal
}