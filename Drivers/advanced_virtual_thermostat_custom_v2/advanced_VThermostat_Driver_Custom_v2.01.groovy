/* 
 *  Advanced Virtual Thermostat Device Driver Custom v2
 *  Copyright 2020 Nelson Clark / Customizations by jshimota
 *
 *  ======================================================================================
 *  CHANGELOG:
 *  --------------------------------------------------------------------------------------
 *  Date       Version   Author     Description
 *  --------------------------------------------------------------------------------------
 *  2026-08-22 v2.0.1    jshimota   Fixed evaluateMode() loop by assigning empty string "" 
 *                                  instead of null to preEmergencyMode attribute. Renamed 
 *                                  driver definition to Advanced vThermostat Device Custom v2.
 *  ======================================================================================
 */
 
import groovy.json.JsonOutput

metadata {
    definition (
        name: "Advanced vThermostat Device Custom v2", 
        namespace: "jshimota", 
        author: "Nelson Clark",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/advanced_virtual_thermostat_custom_v2advanced_vThermostat_driver_custom.groovy"
    ) {
        capability "Thermostat"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "Refresh"
        capability "Configuration"

        command "heatUp"
        command "heatDown"
        command "coolUp"
        command "coolDown"
        command "setMaxUpdateInterval", ["number"]
        
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
    }
}

def installed() {
    state.loggingLevel = 3
    def hubScale = getTemperatureScale()
    
    if (hubScale == "C") {
        state.currentUnit = "C"
        sendEvent(name: "minCoolTemp", value: 15.5, unit: "C")
        sendEvent(name: "minCoolingSetpoint", value: 15.5, unit: "C")
        sendEvent(name: "maxCoolTemp", value: 35.0, unit: "C")
        sendEvent(name: "maxCoolingSetpoint", value: 35.0, unit: "C")
        sendEvent(name: "minHeatTemp", value: 1.5, unit: "C")
        sendEvent(name: "minHeatingSetpoint", value: 1.5, unit: "C")
        sendEvent(name: "maxHeatTemp", value: 26.5, unit: "C")
        sendEvent(name: "maxHeatingSetpoint", value: 26.5, unit: "C")
        sendEvent(name: "thermostatThreshold", value: 0.5, unit: "C")
        sendEvent(name: "temperature", value: 22.0, unit: "C")
        sendEvent(name: "heatingSetpoint", value: 21.0, unit: "C")
        sendEvent(name: "coolingSetpoint", value: 24.5, unit: "C")
    } else {
        state.currentUnit = "F"
        sendEvent(name: "minCoolTemp", value: 60, unit: "F")
        sendEvent(name: "minCoolingSetpoint", value: 60, unit: "F")
        sendEvent(name: "maxCoolTemp", value: 95, unit: "F")
        sendEvent(name: "maxCoolingSetpoint", value: 95, unit: "F")
        sendEvent(name: "minHeatTemp", value: 35, unit: "F")
        sendEvent(name: "minHeatingSetpoint", value: 35, unit: "F")
        sendEvent(name: "maxHeatTemp", value: 80, unit: "F")
        sendEvent(name: "maxHeatingSetpoint", value: 80, unit: "F")
        sendEvent(name: "thermostatThreshold", value: 1.0, unit: "F")
        sendEvent(name: "temperature", value: 72, unit: "F")
        sendEvent(name: "heatingSetpoint", value: 70, unit: "F")
        sendEvent(name: "coolingSetpoint", value: 76, unit: "F")
    }
    
    updateThermostatSetpoint(hubScale)
    state.lastTempUpdate = now()
    sendEvent(name: "thermostatMode", value: "off")
    sendEvent(name: "thermostatOperatingState", value: "idle")
    sendEvent(name: "maxUpdateInterval", value: 65)
    sendEvent(name: "preEmergencyMode", value: "")
    sendEvent(name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "cool", "auto", "off"]))
}

def updated() {
    def hubScale = getTemperatureScale()
    state.currentUnit = hubScale
    sendEvent(name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "cool", "auto", "off"]))
    updateThermostatSetpoint(hubScale)
}

def configure() {
    sendEvent(name: "supportedThermostatModes", value: JsonOutput.toJson(["auto", "cool", "heat", "off"]))
}

def parse(String description) {}

/**
 * Helper routine to keep thermostatTemperatureSetpoint and thermostatSetpoint
 * synchronized based on current thermostat mode and setpoints.
 */
def updateThermostatSetpoint(String units = null) {
    if (!units) units = getTemperatureScale()
    def mode = device.currentValue("thermostatMode") ?: "off"
    def targetVal
    
    if (mode == "cool") {
        targetVal = device.currentValue("coolingSetpoint") ?: (units == "C" ? 24.5 : 76.0)
    } else {
        targetVal = device.currentValue("heatingSetpoint") ?: (units == "C" ? 21.0 : 70.0)
    }
    
    sendEvent(name: "thermostatSetpoint", value: targetVal, unit: units)
    sendEvent(name: "thermostatTemperatureSetpoint", value: targetVal, unit: units)
}

def evaluateMode() {
    logger("trace", "evaluateMode() - START")
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
    
    def maxIntervalMili = maxInterval * 60000

    if (current == "idle" && (nowMs - lastUpdate >= maxIntervalMili)) {
        logger("debug", "Temp sensor maximum update interval exceeded ($maxInterval mins). Thermostat idle.")
    } else if (mode != "off" && current != "idle" && (nowMs - lastUpdate >= maxIntervalMili)) {
        logger("error", "Temp sensor update timeout exceeded. Enforcing EMERGENCY STOP.")
        sendEvent(name: "preEmergencyMode", value: mode)
        sendEvent(name: "thermostatMode", value: "off") // Standard Google Home mode
        sendEvent(name: "thermostatOperatingState", value: "idle")
        runIn(2, 'evaluateMode')
        return
    } else if (device.currentValue("preEmergencyMode") && device.currentValue("preEmergencyMode") != "" && (nowMs - lastUpdate < maxIntervalMili)) {
        logger("warn", "Sensors reporting again. Autorecovered to previous mode.")
        def prevMode = device.currentValue("preEmergencyMode")
        sendEvent(name: "preEmergencyMode", value: "")
        sendEvent(name: "thermostatMode", value: prevMode)
        runIn(2, 'evaluateMode')
        return
    }

    def callFor = "idle"

    if (!threshold) {
        logger("error", "evaluateMode() - Threshold not set.")
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
        logger("debug", "sendEvent : thermostatOperatingState = $callFor")  
        sendEvent(name: "thermostatOperatingState", value: callFor)
    }
}

def emergencyStop() { 
    sendEvent(name: "preEmergencyMode", value: device.currentValue("thermostatMode"))
    setThermostatMode("off") 
}

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

    Double newCoolingSetpoint = null
    if (newHeatingSetpoint > (coolingSetpoint - setpointDistance)) {
        newCoolingSetpoint = newHeatingSetpoint + setpointDistance
    }
    
    if (newHeatingSetpoint > heatmax || newHeatingSetpoint < heatmin) {
        logger("warn", "setHeatingSetpoint() ignoring out of range value ($newHeatingSetpoint)")
        return
    }

    def units = getTemperatureScale()
    sendEvent(name: "heatingSetpoint", value: newHeatingSetpoint, unit: units)
    
    if (newCoolingSetpoint && newCoolingSetpoint <= coolmax) {
        sendEvent(name: "coolingSetpoint", value: newCoolingSetpoint, unit: units)
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

    Double newHeatingSetpoint = null
    if ((newCoolingSetpoint - setpointDistance) < heatingSetpoint) {
        newHeatingSetpoint = newCoolingSetpoint - setpointDistance
    }
    
    if (newCoolingSetpoint > coolmax || newCoolingSetpoint < coolmin) {
        logger("warn", "setCoolingSetpoint() ignoring out of range value ($newCoolingSetpoint)")
        return
    }

    def units = getTemperatureScale()
    sendEvent(name: "coolingSetpoint", value: newCoolingSetpoint, unit: units)
    if (newHeatingSetpoint && newHeatingSetpoint >= heatmin) {
        sendEvent(name: "heatingSetpoint", value: newHeatingSetpoint, unit: units)
    }
    updateThermostatSetpoint(units)
    runIn(2, 'evaluateMode')
}

def setThermostatThreshold(Object value) {
    if (value == null || !value.toString().isNumber()) return
    Double dVal = value.toString().toDouble()
    if (dVal != device.currentValue("thermostatThreshold")) {
        sendEvent(name: "thermostatThreshold", value: dVal, unit: getTemperatureScale())
        runIn(2, 'evaluateMode')
    }
}

def setMaxUpdateInterval(BigDecimal minutes) {
    if (minutes != device.currentValue("maxUpdateInterval")) {
        sendEvent(name: "maxUpdateInterval", value: minutes)
        runIn(2, 'evaluateMode')
    }
}

def setThermostatMode(String value) {
    if (value != device.currentValue("thermostatMode")) {
        sendEvent(name: "thermostatMode", value: value)
        updateThermostatSetpoint()
        runIn(2, 'evaluateMode')
    }
}

def off() {
	setThermostatMode("off") 
}

def heat() {
    sendEvent(name: "lastRunningMode", value: "heat")
    updateDataValue("lastRunningMode", "heat")
    setThermostatMode("heat")
}
def auto() {
	setThermostatMode("auto") 
}

def cool() {
    sendEvent(name: "lastRunningMode", value: "cool")
    updateDataValue("lastRunningMode", "cool")
    setThermostatMode("cool")
}

def poll() { return null }

def refresh() {
}

def setTemperature(value) {
    if (value == null || !value.toString().isNumber()) {
        logger("warn", "setTemperature() received invalid temperature value: ${value}")
        return
    }
    Double dVal = value.toString().toDouble()
    def units = getTemperatureScale()
    sendEvent(name: "temperature", value: dVal, unit: units)
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
    sendEvent(name: "minCoolTemp", value: value, unit: units)
    sendEvent(name: "minCoolingSetpoint", value: value, unit: units)
    if (device.currentValue("coolingSetpoint") < value) setCoolingSetpoint(value)
}

def setMaxCoolTemp(Double value) {
    def units = getTemperatureScale()
    sendEvent(name: "maxCoolTemp", value: value, unit: units)
    sendEvent(name: "maxCoolingSetpoint", value: value, unit: units)
    if (device.currentValue("coolingSetpoint") > value) setCoolingSetpoint(value)
}

def setMinHeatTemp(Double value) {
    def units = getTemperatureScale()
    sendEvent(name: "minHeatTemp", value: value, unit: units)
    sendEvent(name: "minHeatingSetpoint", value: value, unit: units)
    if (device.currentValue("heatingSetpoint") < value) setHeatingSetpoint(value)
}

def setMaxHeatTemp(Double value) {
    def units = getTemperatureScale()
    sendEvent(name: "maxHeatTemp", value: value, unit: units)
    sendEvent(name: "maxHeatingSetpoint", value: value, unit: units)
    if (device.currentValue("heatingSetpoint") > value) setHeatingSetpoint(value)
}

def fanAuto() {}
def fanCirculate() {}
def fanOn() {}
def setSchedule() {}
def setThermostatFanMode(String value) {}

def logger(level, msg) {
    int lvl = state.loggingLevel != null ? state.loggingLevel : 3
    switch(level) {
        case "error": if (lvl >= 1) log.error msg; break
        case "warn":  if (lvl >= 2) log.warn msg;  break
        case "info":  if (lvl >= 3) log.info msg;  break
        case "debug": if (lvl >= 4) log.debug msg; break
        case "trace": if (lvl >= 5) log.trace msg; break
        default:      log.debug msg; break
    }
}

def setLogLevel(level) {
    state.loggingLevel = level.toInteger()
    logger("warn", "Device logging level set to ${state.loggingLevel}")
}

def getTemperatureScale() { return "${location.temperatureScale}" }
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