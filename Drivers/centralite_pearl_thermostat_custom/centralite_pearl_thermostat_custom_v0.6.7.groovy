/**
 * CentraLite Pearl Thermostat (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Custom driver for CentraLite Pearl Zigbee Thermostat featuring heating/cooling setpoint 
 * controls, custom fan circulation cycle loop, battery/power source parsing, and custom health tracking.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability to avoid extra UI buttons.
 * - Uses phase-anchored randomized daily scheduling and timeout guards to monitor ping/response cycles
 *   and maintain device healthStatus ("online", "offline", "unknown").
 *
 * Temperature Calibration Architecture:
 * - Temperature offset adjustments are applied strictly at the driver layer via hardware calibration
 *   attribute 0x0201:0x0010. Downstream applications (such as MEM) consume the calibrated 'temperature' 
 *   attribute directly without applying secondary offsets.
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
 * v0.6.7    08/31/26    jshimota    Fixed case-sensitive heatingSetpoint attribute check bug in changeSetpoint() fallback logic.
 * v0.6.6    08/31/26    jshimota    Separated physical thermostatFanMode (on/auto/off/circulate) from thermostatFanControlSource (local/external). Documented driver-level temperature calibration contract.
 * v0.6.5    08/31/26    jshimota    Hardened 'ext' fan control mode. Preserved 'ext' attribute state while permitting physical fanOn() and fanAuto() Zigbee frame execution from external applications.
 * v0.6.4    08/31/26    jshimota    Removed enableFanCommands preference switch and fanModeNote operational note. Streamlined fan mode bypass to check for 'ext' state.
 * v0.6.3    08/31/26    jshimota    Added 'ext' option to supportedThermostatFanModes JSON array and GUI command dropdown. Updated command execution logic and GUI notes.
 * v0.6.2    08/31/26    jshimota    Refined enableFanCommands preference description text.
 * v0.6.1    08/31/26    jshimota    Added explicit GUI parameter descriptions to Fan commands notifying user that fan mode commands are only functional when enabled in preferences.
 * v0.6.0    08/31/26    jshimota    Added enableFanCommands preference switch to bypass physical fan cluster execution when managed externally (e.g., via MEM app). Updated GUI preferences guidance text.
 * v0.5.0    08/31/26    jshimota    Applied Driver Template v1.0.10: Standardized logging engine, phase-anchored custom Health Check, single-shot version demarcation, master utility routines, and updated GUI controls.
 **/

static String version() { return '0.6.7' }
def timeStamp() { return "2026/08/31 09:50 PM" }

import hubitat.zigbee.zcl.DataType
import groovy.transform.Field
import java.math.RoundingMode

metadata {
    definition(
        name: "CentraLite Pearl Thermostat (Custom)",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/centralite_pearl_thermostat_custom/centralite_pearl_thermostat_custom.groovy"
    ) {
        capability "Actuator"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "Thermostat"

        // Commands
        command "Health Check"
        command "fanOff"
        command "initialize"
        command "lowerCoolingSetpointLevel"
        command "lowerHeatingSetpointLevel"
        command "raiseCoolingSetpointLevel"
        command "raiseHeatingSetpointLevel"
        command "resetDriver"
        command "toggleHoldMode"

        command "setCoolingSetpoint", [[name: "degrees", type: "NUMBER", description: "Cooling Setpoint in degrees"]]
        command "setHeatingSetpoint", [[name: "degrees", type: "NUMBER", description: "Heating Setpoint in degrees"]]
        command "setThermostatFanControlSource", [[name: "source*", type: "ENUM", description: "Select 'external' when fan circulation is managed by an external application like MEM.", constraints: ["local", "external"]]]

        // Attributes
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "powerSource", "string"
        attribute "supportedThermostatModes", "JSON_OBJECT"
        attribute "supportedThermostatFanModes", "JSON_OBJECT"
        attribute "thermostatFanCycleState", "enum", ["on", "off"]
        attribute "thermostatFanControlSource", "enum", ["local", "external"]
        attribute "thermostatFanModes", "JSON_OBJECT"
        attribute "thermostatHoldMode", "string"
        attribute "thermostatRunMode", "string"

        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0201,0202,0204,0B05", outClusters: "000A,0019", manufacturer: "Centralite", model: "3157100", deviceJoinName: "CentraLite Pearl Thermostat (Custom)"
    }

    preferences {
        input name: "tempOffset", type: "decimal", title: "<b>Temperature Offset</b>", description: "<i>Adjust temperature readings by -4.5 to +4.5 degrees. (Applied at driver layer).</i>", defaultValue: 0.0, range: "-4.5..4.5"
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver sends a Health Check ping to verify device online status.</i>"

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

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing CentraLite Pearl Thermostat (Custom) v${version()} (${timeStamp()})..."
    
    initializeHealthCheckPhase()
    sendEvent(name: "healthStatus", value: "unknown")
    
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    
    initialize(false)
    runIn(1, "configure")
}

void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

    def fanOptionsList = ["on", "auto", "off", "circulate"] 
    sendEvent(name: "thermostatFanModes", value: groovy.json.JsonOutput.toJson(fanOptionsList))
    sendEvent(name: "supportedThermostatFanModes", value: groovy.json.JsonOutput.toJson(fanOptionsList))
    
    def systemModesList = ["off", "heat", "cool", "emergencyHeat"] 
    sendEvent(name: "supportedThermostatModes", value: groovy.json.JsonOutput.toJson(systemModesList)) 

    if (device.currentValue("thermostatFanControlSource") == null) sendEvent(name: "thermostatFanControlSource", value: "local")
    if (device.currentValue("battery") == null) sendEvent(name: "battery", value: 100, unit: "%") 
    if (device.currentValue("powerSource") == null) sendEvent(name: "powerSource", value: "unknown") 
    if (device.currentValue("thermostatMode") == null) sendEvent(name: "thermostatMode", value: "off")
    if (device.currentValue("thermostatOperatingState") == null) sendEvent(name: "thermostatOperatingState", value: "idle")
    if (device.currentValue("temperature") == null) sendEvent(name: "temperature", value: 70, unit: getTemperatureScale())
    if (device.currentValue("heatingSetpoint") == null) sendEvent(name: "heatingSetpoint", value: 68, unit: getTemperatureScale())
    if (device.currentValue("coolingSetpoint") == null) sendEvent(name: "coolingSetpoint", value: 74, unit: getTemperatureScale())
    if (device.currentValue("thermostatSetpoint") == null) sendEvent(name: "thermostatSetpoint", value: 68, unit: getTemperatureScale())

    final int interval = settings?.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 480
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

    runIn(2, "refresh")
}

List<String> configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device reporting and options..."

    List<String> cmds = zigbee.batteryConfig() +
               zigbee.configureReporting(0x0201, 0x0000, DataType.INT16, 10, 600, 50) +   // Local Temperature
               zigbee.configureReporting(0x0201, 0x0011, DataType.INT16, 5, 300, 50) +    // Cooling Setpoint
               zigbee.configureReporting(0x0201, 0x0012, DataType.INT16, 5, 300, 50) +    // Heating Setpoint
               zigbee.configureReporting(0x0201, 0x001C, DataType.ENUM8, 5, 300, 1) +     // System Mode
               zigbee.configureReporting(0x0201, 0x0029, DataType.BITMAP16, 5, 300, 1) + // Running State
               zigbee.configureReporting(0x0201, 0x0023, DataType.ENUM8, 5, 300, 1) +     // Hold Mode
               zigbee.configureReporting(0x0202, 0x0000, DataType.ENUM8, 5, 300, 1)       // Fan Mode

    if (settings?.tempOffset != null) {
        BigDecimal offset = settings.tempOffset as BigDecimal
        double celsiusOffset = (getTemperatureScale() == "C") ? offset.doubleValue() : (offset.doubleValue() / 1.8)
        int rawOffset = Math.round(celsiusOffset * 10).toInteger()
        cmds += zigbee.writeAttribute(0x0201, 0x0010, DataType.INT8, rawOffset)
    }

    cmds += executePing()
    logDebug "configure() payload -> ${cmds}"
    return cmds
}

/* =========================================================================================
   COMMAND IMPLEMENTATIONS
   ========================================================================================= */

List<String> refresh() {
    logDebug "Executing refresh()..."

    return zigbee.readAttribute(0x0000, 0x0007) + 
           zigbee.readAttribute(0x0201, 0x0000) + 
           zigbee.readAttribute(0x0201, 0x0010) + 
           zigbee.readAttribute(0x0201, 0x0011) + 
           zigbee.readAttribute(0x0201, 0x0012) + 
           zigbee.readAttribute(0x0201, 0x001C) + 
           zigbee.readAttribute(0x0201, 0x001E) + 
           zigbee.readAttribute(0x0201, 0x0023) + 
           zigbee.readAttribute(0x0201, 0x0029) + 
           zigbee.readAttribute(0x0001, 0x0020) + 
           zigbee.readAttribute(0x0202, 0x0000)   
}

void setThermostatFanControlSource(String value) {
    if (value == null) return
    String norm = value.toLowerCase().trim()
    if (norm in ["local", "external"]) {
        logInfo "Thermostat Fan Control Source set to '${norm}'"
        updateAttribute("thermostatFanControlSource", norm)
        if (norm == "external") {
            unschedule("runCirculateCycle")
        }
    } else {
        logError "Invalid fan control source requested: ${value}"
    }
}

void raiseHeatingSetpointLevel() { changeSetpoint("heatingSetpoint", 1) }
void lowerHeatingSetpointLevel() { changeSetpoint("heatingSetpoint", -1) }
void raiseCoolingSetpointLevel() { changeSetpoint("coolingSetpoint", 1) }
void lowerCoolingSetpointLevel() { changeSetpoint("coolingSetpoint", -1) }

private void changeSetpoint(String attributeName, int delta) {
    // FIX: Replaced case-sensitive substring contains("Heat") with explicit attribute equality
    def currentVal = device.currentValue(attributeName) ?: (attributeName == "heatingSetpoint" ? 68 : 74)
    int nextLevel = currentVal.toInteger() + delta
    
    if (attributeName == "heatingSetpoint") {
        setHeatingSetpoint(nextLevel)
    } else {
        setCoolingSetpoint(nextLevel)
    }
}

void toggleHoldMode() {
    String currentHoldMode = device.currentValue("thermostatHoldMode") ?: "holdOff"
    if (currentHoldMode == "holdOn") { holdOff() } else { holdOn() }
}

void setThermostatMode(String value) {
    if (value == null) return
    logDebug "setThermostatMode requested: ${value}"
    String normalizedValue = value.toLowerCase().replaceAll(/\s+(.)/) { match, group -> group.toUpperCase() }
    if (normalizedValue == "emergencyheat") normalizedValue = "emergencyHeat"
    
    switch(normalizedValue) {
        case "off": off(); break
        case "cool": cool(); break
        case "heat": heat(); break
        case "emergencyHeat": emergencyHeat(); break
        case "auto": auto(); break
        default:
            logError "Unsupported thermostat mode requested: ${value}"
            break
    }
}

void setThermostatFanMode(String value) { 
    if (value == null) return
    String normValue = value.toLowerCase().trim()

    if (normValue in ["fanon", "on"]) {
        fanOn()
    } else if (normValue in ["fanauto", "auto"]) {
        fanAuto()
    } else if (normValue in ["fanoff", "off"]) {
        fanOff()
    } else if (normValue in ["fancirculate", "circulate"]) {
        fanCirculate()
    } else {
        logError "Unsupported fan mode requested: ${value}"
    }
}

void setThermostatHoldMode(String value) {
    if (value == null) return
    String norm = value.toLowerCase().trim()
    if (norm in ["holdon", "on", "true"]) {
        holdOn()
    } else if (norm in ["holdoff", "off", "false"]) {
        holdOff()
    } else {
        logError "Invalid hold mode requested: ${value}"
    }
}

List<String> off() {
    logInfo "Setting thermostat mode to Off"
    updateAttribute("thermostatMode", "off")
    return zigbee.writeAttribute(0x0201, 0x1C, DataType.ENUM8, 0)
}

List<String> cool() {
    logInfo "Setting thermostat mode to Cool"
    updateAttribute("thermostatMode", "cool")
    return zigbee.writeAttribute(0x0201, 0x1C, DataType.ENUM8, 3)
}

List<String> heat() {
    logInfo "Setting thermostat mode to Heat"
    updateAttribute("thermostatMode", "heat")
    return zigbee.writeAttribute(0x0201, 0x1C, DataType.ENUM8, 4)
}

List<String> emergencyHeat() {
    logInfo "Setting thermostat mode to Emergency Heat"
    updateAttribute("thermostatMode", "emergencyHeat")
    return zigbee.writeAttribute(0x0201, 0x1C, DataType.ENUM8, 5)
}

void auto() {
    logWarn "Device does not support automatic system changeover. Request ignored."
}

List<String> on() { return fanOn() }

List<String> fanOn() {
    logInfo "Executing physical fanOn command"
    unschedule("runCirculateCycle") 
    updateAttribute("thermostatFanMode", "on", null, "digital")
    return zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 4)
}

List<String> fanAuto() {
    logInfo "Executing physical fanAuto command"
    unschedule("runCirculateCycle") 
    updateAttribute("thermostatFanMode", "auto", null, "digital")
    return zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 5)
}

List<String> fanOff() {
    logInfo "Executing physical fanOff command"
    unschedule("runCirculateCycle") 
    updateAttribute("thermostatFanMode", "off", null, "digital")
    return zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 0)
}

void fanCirculate() {
    if (device.currentValue("thermostatFanControlSource") == "external") {
        logInfo "fanCirculate command ignored (Control Source is set to 'external')."
        return
    }
    logInfo "Setting thermostat fan mode to Circulate (30m Loop)"
    unschedule("runCirculateCycle")
    updateAttribute("thermostatFanMode", "circulate", null, "digital")
    runCirculateCycle([targetState: "on"])
}

void runCirculateCycle(Map data = [:]) {
    if (device.currentValue("thermostatFanControlSource") == "external" || device.currentValue("thermostatFanMode") != "circulate") return

    String targetState = data?.targetState
    if (targetState == null) {
        String lastCycleState = device.currentValue("thermostatFanCycleState") ?: "off"
        targetState = (lastCycleState == "on") ? "off" : "on"
    }

    sendEvent(name: "thermostatFanCycleState", value: targetState, isStateChange: true, displayed: false)

    if (targetState == "on") {
        logDebug "Circulation Loop: Turning Fan ON"
        sendHubCommand(new hubitat.device.HubAction(zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 4)[0], hubitat.device.Protocol.ZIGBEE))
        runIn(1800, "runCirculateCycle", [overwrite: true, data: [targetState: "off"]])
    } else {
        logDebug "Circulation Loop: Setting Fan to AUTO"
        sendHubCommand(new hubitat.device.HubAction(zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 5)[0], hubitat.device.Protocol.ZIGBEE))
        runIn(1800, "runCirculateCycle", [overwrite: true, data: [targetState: "on"]])
    }
}

List<String> holdOn() {
    logInfo "Setting Hold Mode to On"
    updateAttribute("thermostatHoldMode", "holdOn")
    return zigbee.writeAttribute(0x0201, 0x23, DataType.ENUM8, 1)
}

List<String> holdOff() {
    logInfo "Setting Hold Mode to Off"
    updateAttribute("thermostatHoldMode", "holdOff")
    return zigbee.writeAttribute(0x0201, 0x23, DataType.ENUM8, 0)
}

List<String> setHeatingSetpoint(degrees) {
    return processSetpoint(degrees, 0x12)
}

List<String> setCoolingSetpoint(degrees) {
    return processSetpoint(degrees, 0x11)
}

private List<String> processSetpoint(degrees, int attributeId) { 
    if (degrees == null || !degrees.toString().isNumber()) return []
    
    boolean isC = (getTemperatureScale() == "C") 
    int maxTemp = isC ? 44 : 86 
    int minTemp = isC ? 7 : 30 
    
    BigDecimal numDegrees = new BigDecimal(degrees.toString())
    int degreesInteger = Math.round(numDegrees).toInteger() 
    degreesInteger = Math.max(minTemp, Math.min(degreesInteger, maxTemp)) 
    
    double celsius = isC ? degreesInteger : fahrenheitToCelsius(degreesInteger) 
    int finalValue = Math.round(celsius * 100).toInteger() 
    
    String attrName = (attributeId == 0x12) ? "heatingSetpoint" : "coolingSetpoint" 
    
    logInfo "Setting ${attrName} to ${degreesInteger}°${getTemperatureScale()}"
    
    updateAttribute(attrName, degreesInteger, getTemperatureScale(), "digital")
    updateAttribute("thermostatSetpoint", degreesInteger, getTemperatureScale(), "digital")
    
    if (attrName == "heatingSetpoint" && device.currentValue("coolingSetpoint") == null) {
        updateAttribute("coolingSetpoint", (degreesInteger + 5), getTemperatureScale(), "digital")
    } else if (attrName == "coolingSetpoint" && device.currentValue("heatingSetpoint") == null) {
        updateAttribute("heatingSetpoint", (degreesInteger - 5), getTemperatureScale(), "digital")
    }
    
    return zigbee.writeAttribute(0x0201, attributeId, DataType.INT16, finalValue) 
}

/* =========================================================================================
   HEALTH CHECK ROUTINE TEMPLATE
   ========================================================================================= */

List<String> "Health Check"() {
    return executePing()
}

private List<String> executePing() {
    logDebug "Health Check Ping sent..."
    scheduleCommandTimeoutCheck()
    return zigbee.readAttribute(0x0000, 0x0007)
}

private void initializeHealthCheckPhase() {
    if (state.healthCheckStartHour == null) state.healthCheckStartHour = new Random().nextInt(24)
    if (state.healthCheckStartMinute == null) state.healthCheckStartMinute = new Random().nextInt(60)
}

private void scheduleHealthCheck(String methodToSchedule, int intervalMin) {
    unschedule(methodToSchedule)
    initializeHealthCheckPhase()

    final int h = (state.healthCheckStartHour != null) ? (state.healthCheckStartHour as Integer) : 0
    final int m = (state.healthCheckStartMinute != null) ? (state.healthCheckStartMinute as Integer) : 0

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
    logWarn "No device communication received within health check timeout window (device offline?)"
    updateAttribute("healthStatus", "offline")
}

/* =========================================================================================
   ZIGBEE MESSAGE PARSING
   ========================================================================================= */

void parse(String description) {
    logDebug "Parsing raw description -> ${description}"
    if (!description) return

    try {
        if (description.startsWith("read attr -") || description.startsWith("catchall:")) {
            Map descMap = zigbee.parseDescriptionAsMap(description)
            if (!descMap) return
            logTrace "Parsed description map -> ${descMap}"

            Integer clusterInt = descMap.cluster ? Integer.parseInt(descMap.cluster, 16) : descMap.clusterInt
            Integer attrInt = descMap.attrId ? Integer.parseInt(descMap.attrId, 16) : descMap.attrInt
            
            unschedule("deviceCommandTimeout")
            updateAttribute("healthStatus", "online", null, "physical")

            switch(clusterInt) {
                case 0x0201: // Thermostat Cluster
                    if (attrInt == 0x0000) {
                        BigDecimal temp = getTemperature(descMap.value)
                        updateAttribute("temperature", temp, getTemperatureScale(), "physical")
                    } else if (attrInt == 0x0011) {
                        BigDecimal temp = getTemperature(descMap.value)
                        updateAttribute("coolingSetpoint", temp, getTemperatureScale(), "physical")
                        if (device.currentValue("thermostatMode") == "cool") {
                            updateAttribute("thermostatSetpoint", temp, getTemperatureScale(), "physical")
                        }
                    } else if (attrInt == 0x0012) {
                        BigDecimal temp = getTemperature(descMap.value)
                        updateAttribute("heatingSetpoint", temp, getTemperatureScale(), "physical")
                        if (device.currentValue("thermostatMode") in ["heat", "emergencyHeat"]) {
                            updateAttribute("thermostatSetpoint", temp, getTemperatureScale(), "physical")
                        }
                    } else if (attrInt == 0x001C) {
                        String mode = getModeMap()[descMap.value] ?: "off"
                        updateAttribute("thermostatMode", mode, null, "physical")
                        
                        if (mode == "cool") {
                            Object activeSetpoint = device.currentValue("coolingSetpoint")
                            if (activeSetpoint != null) updateAttribute("thermostatSetpoint", activeSetpoint, getTemperatureScale(), "physical")
                        } else if (mode in ["heat", "emergencyHeat"]) {
                            Object activeSetpoint = device.currentValue("heatingSetpoint")
                            if (activeSetpoint != null) updateAttribute("thermostatSetpoint", activeSetpoint, getTemperatureScale(), "physical")
                        }
                    } else if (attrInt == 0x001E) {
                        String runMode = getModeMap()[descMap.value] ?: "off"
                        updateAttribute("thermostatRunMode", runMode, null, "physical")
                    } else if (attrInt == 0x0023) {
                        String holdMode = getHoldModeMap()[descMap.value] ?: "holdOff"
                        updateAttribute("thermostatHoldMode", holdMode, null, "physical")
                    } else if (attrInt == 0x0029) {
                        String opState = getThermostatOperatingStateMap()[descMap.value] ?: "idle"
                        updateAttribute("thermostatOperatingState", opState, null, "physical")
                    }
                    break
                    
                case 0x0202: // Fan Control Cluster
                    if (attrInt == 0x0000) {
                        if (device.currentValue("thermostatFanMode") != "circulate") {
                            String fanMode = getFanModeMap()[descMap.value] ?: "auto"
                            updateAttribute("thermostatFanMode", fanMode, null, "physical")
                        }
                    }
                    break
                    
                case 0x0001: // Power Configuration Cluster
                    if (attrInt == 0x0020) {
                        Integer batLevel = getBatteryLevel(descMap.value)
                        updateAttribute("battery", batLevel, "%", "physical")
                    }
                    break
                    
                case 0x0000: // Basic Cluster
                    if (attrInt == 0x0007) {
                        String source = getPowerSource()[descMap.value] ?: "unknown"
                        updateAttribute("powerSource", source, null, "physical")
                    }
                    break
            }
        }
    } catch (Exception e) {
        logError "Error parsing description frame [${description}]: ${e.message}"
    }
}

/* =========================================================================================
   LOOKUP MAPS & CONVERSION HELPERS
   ========================================================================================= */

private Map getModeMap() { ["00":"off", "01":"auto", "03":"cool", "04":"heat", "05":"emergencyHeat", "06":"precooling", "07":"fan only", "08":"dry", "09":"sleep"] }
private Map getHoldModeMap() { ["00":"holdOff", "01":"holdOn"] }
private Map getPowerSource() { ["01":"24VAC", "03":"Battery", "81":"24VAC"] }
private Map getFanModeMap() { ["00":"off", "04":"on", "05":"auto"] }
private Map getThermostatOperatingStateMap() {
    ["0000":"idle", "0001":"heating", "0002":"cooling", "0004":"fan only", "0005":"heating", "0006":"cooling", "0008":"heating", "0009":"heating", "000A":"heating", "000D":"heating", "0010":"cooling", "0012":"cooling", "0014":"cooling", "0015":"cooling"]
}

private BigDecimal getTemperature(String value) {
    if (value == null) return null
    int raw = Integer.parseInt(value, 16)
    if (raw > 0x7FFF) raw -= 0x10000
    double celsius = raw / 100.0
    double tempVal = (getTemperatureScale() == "C") ? celsius : celsiusToFahrenheit(celsius)
    return new BigDecimal(tempVal).setScale(1, RoundingMode.HALF_UP)
}

private Integer getBatteryLevel(String rawValue) {
    if (rawValue == null) return null
    int intValue = Integer.parseInt(rawValue, 16)
    int min = 21
    int max = 30
    int pct = Math.round(((intValue - min) * 100.0) / (max - min)) as int
    return Math.max(0, Math.min(pct, 100))
}

private boolean isHoldOn() {
    return (device.currentValue("thermostatHoldMode") == "holdOn")
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllDriverStates()
    initialize(false)
    logInfo "Driver reset process completed and re-initialized."
}

void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    state.clear()
    logInfo "All states have been cleared."
}

void clearAllDeviceAttributes() {
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
    return (settings && settings[key] != null) ? settings[key] as Boolean : defaultVal
}

@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10