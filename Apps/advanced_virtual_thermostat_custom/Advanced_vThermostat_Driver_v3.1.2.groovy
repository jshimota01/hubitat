/**
 * Advanced Virtual Thermostat Device Driver
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Virtual thermostat device for joining temperature sensors with heating/cooling switch outlets.
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
 * v3.1.2    11/06/26    jshimota    Interface Simplification: Renamed setThermostatThreshold(number) to setHysteresisThreshold(number). Removed the eight visible min/max heat/cool limit attributes and duplicate limit events; fixed limits remain enforced internally.
 * v3.1.1    11/06/26    jshimota    Hysteresis Interface Cleanup: Renamed driverHysteresisEnabled to Hysteresis State (hysteresisState); removed driverVersion device attribute; removed all hysteresis preferences and the legacy string command; added no-argument enableHysteresis()/disableHysteresis() commands. Runtime attributes are the sole source of truth. Existing v3.1.0 hysteresis state is migrated on initialization.
 * v3.1.0    11/06/26    jshimota    Hysteresis Control & Demand Evaluation: Added standalone hysteresis enable preference (default Off), constrained threshold to 0.1–5.0 with one-decimal precision, exposed setThermostatThreshold as a command, synchronized GUI preferences and runtime attributes without overwriting externally commanded values on unrelated preference saves, and corrected state-aware heating/cooling hysteresis for Heat, Cool, and Auto modes. Hysteresis changes now log at Info and trigger immediate evaluation.
 * v3.0.0    10/14/26    jshimota    Major Release & Logging Optimization: Purged '(Custom)' from display names. Downgraded setDriverHysteresis logs to logDebug and removed forced state change to deduplicate event chatter via sendIfChanged(). Retained operating state transitions at logInfo. Bounced version to v3.0.0.
 * v2.10.5   10/12/26    jshimota    Production Release: Retained detailed, preference-controlled debug tracing for thermostat mode commands and demand evaluation. Includes the accepted-mode sequencing fix from v2.10.4; sendIfChanged() event deduplication and the authoritative safety interlock are preserved.
 * v2.10.4   10/12/26    jshimota    Mode Evaluation Sequencing Fix: Retained sendIfChanged() for event deduplication, but passed the validated/accepted requested physical mode into the immediate internal evaluation and thermostat-setpoint presentation update. This avoids relying on an immediate currentValue("physicalThermostatMode") reread after emitting the mode event. evaluateMode() calls without an override retain their existing behavior; safety interlock remains first and authoritative.
 * v2.10.3   10/12/26    jshimota    Added preference-controlled debug snapshots around heat(), setThermostatMode(), and evaluateMode() decisions/exits to capture command sequencing and demand evaluation. No control logic changes.
 * v2.10.2   10/12/26    jshimota    Exposed thermostatThreshold preference (range 0..10, default 1.0). Added hysteresisState attribute and setDriverHysteresis command allowing MEM to disable internal driver hysteresis. Updated evaluateMode comparison to inclusive (>=) check.
 * v2.10.1   10/12/26    jshimota    Synchronous Safety State Events: Enforced immediate synchronous attribute event generation in setSafetyMode() so querying safetyStatus immediately after setSafetyMode("cleared") returns "cleared" without event loop lag.
 * v2.10.0   10/12/26    jshimota    Safety Hold Thermostat Operating State Badge: Explicitly set thermostatOperatingState and controllerState to 'Safety Hold' during active safety interlock states. Preserved enum-compliant thermostatMode attribute so standard Hubitat Thermostat Tiles remain functional while displaying 'Safety Hold' on their main center badge.
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

static String version() { return '3.1.2' }
def timeStamp() { return "2026/11/06" }

metadata {
    definition (
        name: "Advanced vThermostat Device",
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
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "safetyStatus", "enum", ["cleared", "active"]
        attribute "hysteresisState", "enum", ["enabled", "disabled"]
        attribute "thermostatThreshold", "number"
        attribute "supportedThermostatModes", "JSON_OBJECT"
        attribute "thermostatTemperatureSetpoint", "number"
        attribute "controllerState", "string"
        attribute "physicalThermostatMode", "string"

        // Custom Commands
        command "setSafetyMode", ["string"]
        command "enableHysteresis"
        command "disableHysteresis"
        command "setHysteresisThreshold", ["number"]
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
            sendIfChanged([name: "thermostatThreshold", value: 0.5, unit: "C"])
            sendIfChanged([name: "temperature", value: 22.0, unit: "C"])
            sendIfChanged([name: "heatingSetpoint", value: 21.0, unit: "C"])
            sendIfChanged([name: "coolingSetpoint", value: 24.5, unit: "C"])
        } else {
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
    }

    if (device.currentValue("thermostatThreshold") == null) {
        BigDecimal initialThreshold = (hubScale == "C") ? 0.5g : 1.0g
        sendIfChanged([name: "thermostatThreshold", value: initialThreshold, unit: hubScale])
    }
    // Migrate the previous runtime attribute name once. The attribute is now the
    // sole runtime source of truth; hysteresis is no longer configured by preferences.
    if (device.currentValue("hysteresisState") == null) {
        String previousState = device.currentValue("driverHysteresisEnabled")
        String migratedState = (previousState in ["enabled", "disabled"]) ? previousState : "disabled"
        sendIfChanged([name: "hysteresisState", value: migratedState])
        logInfo "Hysteresis state initialized/migrated to ${migratedState}."
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

def enableHysteresis() {
    setHysteresisState("enabled")
}

def disableHysteresis() {
    setHysteresisState("disabled")
}

private void setHysteresisState(String newState) {
    String oldState = device.currentValue("hysteresisState") ?: "disabled"
    if (oldState != newState) {
        sendIfChanged([name: "hysteresisState", value: newState])
        logInfo "Thermostat hysteresis ${newState.toUpperCase()} by command."
        evaluateMode()
    } else {
        logDebug "Hysteresis state already ${newState}; no change required."
    }
}

def setSafetyMode(String mode) {
    logDebug "setSafetyMode ENTRY: requested=${mode}, safetyBefore=${device.currentValue('safetyStatus')}, physicalModeBefore=${device.currentValue('physicalThermostatMode')}, operatingStateBefore=${device.currentValue('thermostatOperatingState')}, controllerStateBefore=${device.currentValue('controllerState')}, temp=${device.currentValue('temperature')}, heatSP=${device.currentValue('heatingSetpoint')}, hysteresisState=${device.currentValue('hysteresisState')}."
    if (mode == null) {
        logDebug "setSafetyMode EXIT: null request."
        return
    }
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
        logDebug "setSafetyMode CLEARED BEFORE evaluateMode: safety=${device.currentValue('safetyStatus')}, physicalMode=${device.currentValue('physicalThermostatMode')}, operatingState=${device.currentValue('thermostatOperatingState')}, controllerState=${device.currentValue('controllerState')}, hysteresisState=${device.currentValue('hysteresisState')}."
        evaluateMode()
    }
    logDebug "setSafetyMode RETURN: safety=${device.currentValue('safetyStatus')}, physicalMode=${device.currentValue('physicalThermostatMode')}, operatingState=${device.currentValue('thermostatOperatingState')}, controllerState=${device.currentValue('controllerState')}, hysteresisState=${device.currentValue('hysteresisState')}."
}

def updateThermostatSetpoint(String units = null, String modeOverride = null) {
    if (!units) units = getTemperatureScale()
    def mode = modeOverride ?: (device.currentValue("physicalThermostatMode") ?: "off")
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

def evaluateMode(String requestedMode = null) {
    logTrace "evaluateMode() - START (requestedMode=${requestedMode})"

    String isSafetyActive = device.currentValue("safetyStatus") ?: "cleared"
    if (isSafetyActive == "active") {
        logDebug "evaluateMode EXIT: safetyStatus=active; operatingState=${device.currentValue('thermostatOperatingState')}, physicalMode=${device.currentValue('physicalThermostatMode')}. No demand evaluation performed."
        if (device.currentValue("thermostatOperatingState") != "Safety Hold") {
            sendIfChanged([name: "thermostatOperatingState", value: "Safety Hold"])
            sendIfChanged([name: "controllerState", value: "Safety Hold"])
        }
        return
    }
    
    def tempRaw = device.currentValue("temperature")
    if (tempRaw == null || !tempRaw.toString().isNumber()) {
        logDebug "evaluateMode EXIT: temperature is missing/invalid (temperature=${tempRaw}); safetyStatus=${isSafetyActive}, physicalMode=${device.currentValue('physicalThermostatMode')}, operatingState=${device.currentValue('thermostatOperatingState')}."
        return
    }
    Double temp = tempRaw.toString().toDouble()

    def heatingSetpoint = device.currentValue("heatingSetpoint")
    def coolingSetpoint = device.currentValue("coolingSetpoint")
    
    Boolean isHysteresisDisabled = (device.currentValue("hysteresisState") == "disabled")
    
    BigDecimal configuredThreshold = (device.currentValue("thermostatThreshold") != null && device.currentValue("thermostatThreshold").toString().isNumber()) ? device.currentValue("thermostatThreshold").toString().toBigDecimal() : 1.0g
    BigDecimal threshold = isHysteresisDisabled ? 0.0g : configuredThreshold

    def current = device.currentValue("thermostatOperatingState")
    def mode = requestedMode ?: (device.currentValue("physicalThermostatMode") ?: "off")

    logDebug "evaluateMode INPUT: requestedMode=${requestedMode}, modeSource=${requestedMode != null ? 'accepted request' : 'currentValue'}, evaluatedPhysicalMode=${mode}, publishedPhysicalMode=${device.currentValue('physicalThermostatMode')}, thermostatMode=${device.currentValue('thermostatMode')}, operatingState=${current}, tempRaw=${tempRaw}, temp=${temp}, heatSP=${heatingSetpoint}, coolSP=${coolingSetpoint}, hysteresisState=${device.currentValue('hysteresisState')}, threshold=${threshold}, lockDashboardToAuto=${getSettingBool('lockDashboardToAuto', true)}."

    if (mode == "off") {
        logDebug "evaluateMode EXIT: physicalMode=off; would otherwise calculate demand. Current operatingState=${current}."
        if (current != "idle") {
            sendIfChanged([name: "thermostatOperatingState", value: "idle"])
            sendIfChanged([name: "controllerState", value: "idle"])
        }
        return
    }

    def callFor = "idle"

    def units = getTemperatureScale()
    if (mode in ["heat", "emergency heat", "emergencyHeat"]) {
        updateThermostatSetpoint(units, mode)
        Boolean heatDemand = (current == "heating") ? (temp < (heatingSetpoint + threshold)) : (temp <= (heatingSetpoint - threshold))
        logDebug "evaluateMode HEAT TEST: current=${current}, heatSP=${heatingSetpoint}, temp=${temp}, startAt=${heatingSetpoint - threshold}, stopAt=${heatingSetpoint + threshold}, threshold=${threshold}, condition=${heatDemand}."
        if (heatDemand) callFor = "heating"
    } else if (mode == "cool") {
        updateThermostatSetpoint(units, mode)
        Boolean coolDemand = (current == "cooling") ? (temp > (coolingSetpoint - threshold)) : (temp >= (coolingSetpoint + threshold))
        logDebug "evaluateMode COOL TEST: current=${current}, coolSP=${coolingSetpoint}, temp=${temp}, startAt=${coolingSetpoint + threshold}, stopAt=${coolingSetpoint - threshold}, threshold=${threshold}, condition=${coolDemand}."
        if (coolDemand) callFor = "cooling"
    } else if (mode == "auto") {
        updateThermostatSetpoint(units, mode)
        Boolean heatDemand = (current == "heating") ? (temp < (heatingSetpoint + threshold)) : (temp <= (heatingSetpoint - threshold))
        Boolean coolDemand = (current == "cooling") ? (temp > (coolingSetpoint - threshold)) : (temp >= (coolingSetpoint + threshold))
        logDebug "evaluateMode AUTO TEST: current=${current}, temp=${temp}, heatSP=${heatingSetpoint}, coolSP=${coolingSetpoint}, threshold=${threshold}, heatCondition=${heatDemand}, coolCondition=${coolDemand}."
        if (heatDemand) {
            callFor = "heating"
        } else if (coolDemand) {
            callFor = "cooling"
        }
    } else {
        logDebug "evaluateMode: unsupported/unhandled physical mode '${mode}'; callFor remains idle."
    }

    logDebug "evaluateMode DECISION: physicalMode=${mode}, priorOperatingState=${current}, calculatedCallFor=${callFor}."
    if (current != callFor) {
        logDebug "evaluateMode ACTION: sending thermostatOperatingState=${callFor}; controllerState=${callFor == 'idle' ? 'idle' : 'controlling ' + callFor}."
        sendIfChanged([name: "thermostatOperatingState", value: callFor])
        sendIfChanged([name: "controllerState", value: callFor == "idle" ? "idle" : "controlling ${callFor}"])
        logDebug "evaluateMode AFTER ACTION: thermostatOperatingState=${device.currentValue('thermostatOperatingState')}, controllerState=${device.currentValue('controllerState')}."
    } else {
        logDebug "evaluateMode NO EVENT: operatingState already equals calculated callFor (${callFor})."
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
    def coolmax = maxCoolingLimit()
    def heatmin = minHeatingLimit()
    def heatmax = maxHeatingLimit()
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
    def coolmin = minCoolingLimit()
    def coolmax = maxCoolingLimit()
    def heatmin = minHeatingLimit()
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

def setHysteresisThreshold(Object value) {
    if (value == null || !value.toString().isNumber()) {
        logWarn "setHysteresisThreshold() requires a numeric value from 0.1 to 5.0."
        return
    }
    BigDecimal requested = value.toString().toBigDecimal()
    if (requested < 0.1g || requested > 5.0g) {
        logWarn "setHysteresisThreshold() rejected ${requested}; valid range is 0.1–5.0 in the hub temperature scale."
        return
    }
    BigDecimal newValue = requested.setScale(1, RoundingMode.HALF_UP)
    BigDecimal oldValue = device.currentValue("thermostatThreshold")?.toString()?.toBigDecimal()
    if (oldValue == null || oldValue.compareTo(newValue) != 0) {
        sendIfChanged([name: "thermostatThreshold", value: newValue, unit: getTemperatureScale()])
        logInfo "Thermostat hysteresis threshold changed to ${newValue} ${getTemperatureScale()} by command."
        evaluateMode()
    } else {
        logDebug "setHysteresisThreshold(): threshold already ${newValue}; no change required."
    }
}


def setThermostatMode(String value) {
    logDebug "setThermostatMode ENTRY: requested=${value}, physicalModeBefore=${device.currentValue('physicalThermostatMode')}, thermostatModeBefore=${device.currentValue('thermostatMode')}, operatingStateBefore=${device.currentValue('thermostatOperatingState')}, safetyStatus=${device.currentValue('safetyStatus')}, temp=${device.currentValue('temperature')}, heatSP=${device.currentValue('heatingSetpoint')}, coolSP=${device.currentValue('coolingSetpoint')}, hysteresisState=${device.currentValue('hysteresisState')}."
    if (value == null) {
        logDebug "setThermostatMode EXIT: null requested mode."
        return
    }
    
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

    logDebug "setThermostatMode ACCEPTED: normalizedMode=${normMode}; safetyStatus=${isSafetyActive}. Updating physical mode."
    sendIfChanged([name: "physicalThermostatMode", value: normMode])

    if (getSettingBool("lockDashboardToAuto", true)) {
        sendIfChanged([name: "thermostatMode", value: "auto"])
    } else {
        sendIfChanged([name: "thermostatMode", value: normMode])
    }
    
    updateThermostatSetpoint(null, normMode)
    logDebug "setThermostatMode BEFORE evaluateMode: acceptedMode=${normMode}, publishedPhysicalMode=${device.currentValue('physicalThermostatMode')}, thermostatMode=${device.currentValue('thermostatMode')}, operatingState=${device.currentValue('thermostatOperatingState')}, safety=${device.currentValue('safetyStatus')}."
    evaluateMode(normMode)
    logDebug "setThermostatMode RETURN: physicalMode=${device.currentValue('physicalThermostatMode')}, thermostatMode=${device.currentValue('thermostatMode')}, operatingState=${device.currentValue('thermostatOperatingState')}, controllerState=${device.currentValue('controllerState')}."
}

def off()  { setThermostatMode("off") }
def auto() { setThermostatMode("auto") }

def heat() {
    logDebug "heat() ENTRY: physicalMode=${device.currentValue('physicalThermostatMode')}, thermostatMode=${device.currentValue('thermostatMode')}, operatingState=${device.currentValue('thermostatOperatingState')}, safety=${device.currentValue('safetyStatus')}, temp=${device.currentValue('temperature')}, heatSP=${device.currentValue('heatingSetpoint')}, coolSP=${device.currentValue('coolingSetpoint')}, hysteresisState=${device.currentValue('hysteresisState')}."
    sendIfChanged([name: "lastRunningMode", value: "heat"])
    updateDataValue("lastRunningMode", "heat")
    setThermostatMode("heat")
    logDebug "heat() RETURN: physicalMode=${device.currentValue('physicalThermostatMode')}, thermostatMode=${device.currentValue('thermostatMode')}, operatingState=${device.currentValue('thermostatOperatingState')}, controllerState=${device.currentValue('controllerState')}, safety=${device.currentValue('safetyStatus')}."
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

def fanAuto() {}
def fanCirculate() {}
def fanOn() {}
def setSchedule() {}
def setThermostatFanMode(String value) {}

def getTemperatureScale() { return "${location?.temperatureScale ?: 'F'}" }
def getDisplayUnits() { return getTemperatureScale() == "C" ? "°C" : "°F" }
def getThermostatResolution() { return getTemperatureScale() == "C" ? 0.5 : 1.0 }

// Fixed setpoint safety boundaries. These are intentionally internal rather
// than device attributes because they are not user-configurable settings.
private BigDecimal minHeatingLimit() { return getTemperatureScale() == "C" ? 1.5g : 35.0g }
private BigDecimal maxHeatingLimit() { return getTemperatureScale() == "C" ? 32.0g : 90.0g }
private BigDecimal minCoolingLimit() { return getTemperatureScale() == "C" ? 15.5g : 60.0g }
private BigDecimal maxCoolingLimit() { return getTemperatureScale() == "C" ? 35.0g : 95.0g }

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
        logInfo "${desc}"
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