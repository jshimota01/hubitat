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
 * v0.5.5    08/31/26    jshimota    Added hardware temperature calibration preference support and Hubitat Dashboard tile state synchronization.
 * v0.5.4    08/31/26    jshimota    Preserved decimal precision in getTemperature(), sanitized Health Check timeout scheduling, and updated reachability terminology.
 * v0.5.3    08/31/26    jshimota    Refactored signed INT16 temperature parsing, declared thermostatFanCycleState attribute, removed Hold Mode setpoint lockout, and added schedule cleanup to fanCirculate.
 * v0.5.2    08/31/26    jshimota    Hardened setThermostatHoldMode String parsing, expanded passive online health updates to all incoming cluster packets, and sanitized command descriptions.
 * v0.5.1    08/31/26    jshimota    Renamed driver display name to CentraLite Pearl Thermostat (Custom).
 * v0.5.0    08/31/26    jshimota    Applied Driver Template v1.0.10: Standardized logging engine, phase-anchored custom Health Check, single-shot version demarcation, master utility routines, and updated GUI controls.
 * v0.4.5    07/10/26    jshimota    Driver hardening.
 * v0.4.4    07/10/26    jshimota    Repaired critical error on Amazon Gateway failure.
 * v0.4.3    06/03/26    jshimota    Text change of UI of Heat and Cool to Heating and Cooling.
 * v0.4.1    06/03/26    jshimota    Text cleanup of UI to show Celsius or Fahrenheit as needed.
 * v0.3.9    06/03/26    jshimota    Modified text string to display temperature values with °F.
 * v0.3.7    06/03/26    jshimota    Fixed 'Auto' mode handling.
 * v0.3.6    06/03/26    jshimota    Changed Setpoint Level button namespace.
 * v0.3.5    06/03/26    jshimota    Repaired Battery and Power Source code.
 * v0.3.4    06/03/26    jshimota    Added supportedThermostatModes attribute definition.
 * v0.3.3    06/03/26    jshimota    Added explicit fanOff command.
 * v0.3.2    06/03/26    jshimota    Added explicit Initialize command.
 * v0.3.1    06/03/26    jshimota    Fixed UI dropdown options mapping to platform standard tokens.
 * v0.3.0    06/03/26    jshimota    Implemented 30-min on/off circulation scheduling logic.
 * v0.2.8    06/03/26    jshimota    Intercept Fan Circulate Exception.
 * v0.2.2    06/03/26    jshimota    Gemini improvements and bug hunt.
 * v0.2.1    06/03/26    jshimota    Adding in log and debug control.
 * v0.2.0    06/03/26    jshimota    Gemini Modernization and optimization.
 * v0.1.1    06/02/26    jshimota    Initial edit, cleanup GNU, basics, remove excess comments.
 * v0.1.0    09/30/21    dagrider    Starting version derived from SmartThings 2021.
 **/

static String version() { return '0.5.5' }
def timeStamp() { return "2026/08/31 02:45 PM" }

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

        // Attributes
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "powerSource", "string"
        attribute "supportedThermostatModes", "JSON_OBJECT"
        attribute "supportedThermostatFanModes", "JSON_OBJECT"
        attribute "thermostatFanCycleState", "enum", ["on", "off"]
        attribute "thermostatFanModes", "JSON_OBJECT"
        attribute "thermostatHoldMode", "string"
        attribute "thermostatRunMode", "string"

        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0201,0202,0204,0B05", outClusters: "000A,0019", manufacturer: "Centralite", model: "3157100", deviceJoinName: "CentraLite Pearl Thermostat (Custom)"
    }

    preferences {
        input name: "tempOffset", type: "decimal", title: "<b>Temperature Offset</b>", description: "<i>Adjust temperature readings by -4.5 to +4.5 degrees.</i>", defaultValue: 0.0, range: "-4.5..4.5"
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver sends a Health Check ping to verify device online status.</i>"

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

    // Ensure healthStatus attribute exists on initialize/reset
    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

    // Populate Supported Modes
    def fanOptionsList = ["on", "auto", "off", "circulate"] 
    sendEvent(name: "thermostatFanModes", value: groovy.json.JsonOutput.toJson(fanOptionsList))
    sendEvent(name: "supportedThermostatFanModes", value: groovy.json.JsonOutput.toJson(fanOptionsList))
    
    def systemModesList = ["off", "heat", "cool", "emergencyHeat"] 
    sendEvent(name: "supportedThermostatModes", value: groovy.json.JsonOutput.toJson(systemModesList)) 

    // Amazon API & Dashboard Safeguards
    if (device.currentValue("battery") == null) sendEvent(name: "battery", value: 100, unit: "%") 
    if (device.currentValue("powerSource") == null) sendEvent(name: "powerSource", value: "unknown") 
    if (device.currentValue("thermostatMode") == null) sendEvent(name: "thermostatMode", value: "off")
    if (device.currentValue("thermostatOperatingState") == null) sendEvent(name: "thermostatOperatingState", value: "idle")
    if (device.currentValue("temperature") == null) sendEvent(name: "temperature", value: 70, unit: getTemperatureScale())
    if (device.currentValue("heatingSetpoint") == null) sendEvent(name: "heatingSetpoint", value: 68, unit: getTemperatureScale())
    if (device.currentValue("coolingSetpoint") == null) sendEvent(name: "coolingSetpoint", value: 74, unit: getTemperatureScale())
    if (device.currentValue("thermostatSetpoint") == null) sendEvent(name: "thermostatSetpoint", value: 68, unit: getTemperatureScale())

    // Centralized Health Check Scheduler
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

    // Sync Temperature Offset hardware attribute (0x0201, attr 0x0010)
    if (settings.tempOffset != null) {
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

    return zigbee.readAttribute(0x0000, 0x0007) + // Power Source
           zigbee.readAttribute(0x0201, 0x0000) + // Temperature
           zigbee.readAttribute(0x0201, 0x0010) + // Temperature Calibration Offset
           zigbee.readAttribute(0x0201, 0x0011) + // Cooling Setpoint
           zigbee.readAttribute(0x0201, 0x0012) + // Heating Setpoint
           zigbee.readAttribute(0x0201, 0x001C) + // System Mode
           zigbee.readAttribute(0x0201, 0x001E) + // Run Mode
           zigbee.readAttribute(0x0201, 0x0023) + // Hold Mode
           zigbee.readAttribute(0x0201, 0x0029) + // Operating State
           zigbee.readAttribute(0x0001, 0x0020) + // Battery
           zigbee.readAttribute(0x0202, 0x0000)   // Fan Mode
}

void raiseHeatingSetpointLevel() { changeSetpoint("heatingSetpoint", 1) }
void lowerHeatingSetpointLevel() { changeSetpoint("heatingSetpoint", -1) }
void raiseCoolingSetpointLevel() { changeSetpoint("coolingSetpoint", 1) }
void lowerCoolingSetpointLevel() { changeSetpoint("coolingSetpoint", -1) }

private void changeSetpoint(String attributeName, int delta) {
    def currentVal = device.currentValue(attributeName) ?: (attributeName.contains("Heat") ? 68 : 74)
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
    logDebug "setThermostatMode requested: ${value}"
    String normalizedValue = value?.toLowerCase()?.replaceAll(/\s+(.)/) { match, group -> group.toUpperCase() }
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
    if (value == "fanOn" || value == "on") {
        fanOn()
    } else if (value == "fanAuto" || value == "auto") {
        fanAuto()
    } else if (value == "fanOff" || value == "off") {
        fanOff()
    } else if (value == "fanCirculate" || value == "circulate") {
        fanCirculate()
    } else {
        logError "Unsupported fan mode requested: ${value}"
    }
}

void setThermostatHoldMode(String value) {
    String norm = value?.toLowerCase()?.trim()
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
    logInfo "Setting thermostat fan mode to On"
    unschedule("runCirculateCycle") 
    updateAttribute("thermostatFanMode", "on")
    return zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 4)
}

List<String> fanAuto() {
    logInfo "Setting thermostat fan mode to Auto"
    unschedule("runCirculateCycle") 
    updateAttribute("thermostatFanMode", "auto")
    return zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 5)
}

List<String> fanOff() {
    logInfo "Setting thermostat fan mode to Off"
    unschedule("runCirculateCycle") 
    updateAttribute("thermostatFanMode", "off")
    return zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 0)
}

void fanCirculate() {
    logInfo "Setting thermostat fan mode to Circulate (30m Loop)"
    unschedule("runCirculateCycle")
    updateAttribute("thermostatFanMode", "circulate")
    runCirculateCycle([targetState: "on"])
}

void runCirculateCycle(Map data = [:]) {
    if (device.currentValue("thermostatFanMode") != "circulate") return

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
    if (degrees == null) return []
    
    boolean isC = (getTemperatureScale() == "C") 
    int maxTemp = isC ? 44 : 86 
    int minTemp = isC ? 7 : 30 
    
    int degreesInteger = Math.round(degrees).toInteger() 
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
   HEALTH CHECK ROUTINE TEMPLATE (CUSTOM DRIVER PING ARCHITECTURE)
   ========================================================================================= */

List<String> "Health Check"() {
    return executePing()
}

private List<String> executePing() {
    logDebug "Health Check Ping sent..."
    scheduleCommandTimeoutCheck()
    return zigbee.readAttribute(0x0000, 0x0007) // Read Basic PowerSource as Ping
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
    logWarn "No Health Check response received within timeout window (device offline?)"
    updateAttribute("healthStatus", "offline")
}

/* =========================================================================================
   ZIGBEE MESSAGE PARSING
   ========================================================================================= */

void parse(String description) {
    logDebug "Parsing raw description -> ${description}"
    if (!description) return

    if (description.startsWith("read attr -") || description.startsWith("catchall:")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        if (!descMap) return
        logTrace "Parsed description map -> ${descMap}"

        Integer clusterInt = descMap.cluster ? Integer.parseInt(descMap.cluster, 16) : descMap.clusterInt
        Integer attrInt = descMap.attrId ? Integer.parseInt(descMap.attrId, 16) : descMap.attrInt
        
        // Passive network health assertion: Any valid parsed packet proves device network reachability
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
                    Object activeSetpoint = (mode == "cool") ? device.currentValue("coolingSetpoint") : device.currentValue("heatingSetpoint")
                    if (activeSetpoint != null) updateAttribute("thermostatSetpoint", activeSetpoint, getTemperatureScale(), "physical")
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
                
            case 0x0000: // Basic Cluster (Ping response verification)
                if (attrInt == 0x0007) {
                    String source = getPowerSource()[descMap.value] ?: "unknown"
                    updateAttribute("powerSource", source, null, "physical")
                }
                break
        }
    }
}

/* =========================================================================================
   LOOKUP MAPS & CONVERSION HELPERS
   ========================================================================================= */

// Device ZCL protocol mode lookup map.
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
    if (raw > 0x7FFF) raw -= 0x10000 // Convert signed 16-bit integer two's complement
    double celsius = raw / 100.0
    double tempVal = (getTemperatureScale() == "C") ? celsius : celsiusToFahrenheit(celsius)
    return new BigDecimal(tempVal).setScale(1, RoundingMode.HALF_UP)
}

private Integer getBatteryLevel(String rawValue) {
    if (rawValue == null) return null
    int intValue = Integer.parseInt(rawValue, 16)
    int min = 21
    int max = 30
    int pct = ((intValue - min) / (max - min) * 100) as int
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