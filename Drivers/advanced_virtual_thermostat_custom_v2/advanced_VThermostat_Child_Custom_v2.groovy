/*
 *  Advanced vThermostat Child App
 *  Copyright 2020 Nelson Clark / Customizations by jshimota
 */

definition(
    name: "Advanced vThermostat Child Custom",
    namespace: "jshimota",
    author: "Nelson Clark",
    description: "Join any sensor(s) with any outlet(s) for virtual thermostat control.",
    category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-logo.png",
	importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/advanced_VThermostat_Child_Custom_v2.groovy",
    parent: "jshimota:Advanced vThermostat Manager Custom"
)

preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    def displayUnits = getDisplayUnits()
    def hubScale = getTemperatureScale()
    
    def setpointDistance = (hubScale == "C") ? 3.0 : 5.0
    def defaultHeat = (hubScale == "C") ? 21.0 : 70.0
    def defaultCool = (hubScale == "C") ? 24.5 : 76.0
    def defaultThresh = (hubScale == "C") ? 0.5 : 1.0

    dynamicPage(name: "", title: "", install: true, uninstall: true, refreshInterval: 0) {
        section() {
            label title: "Name of new Advanced vThermostat app/device:", required: true
        }
        
        section("Select temperature sensor(s)... (Average value will be used if you select multiple sensors)"){
            input "sensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true, required: true
        }

        section("Select outlet(s) to use for heating... "){
            input "heatOutlets", "capability.switch", title: "Outlets", multiple: true
        }

        section("Select outlet(s) to use for cooling... "){
            input "coolOutlets", "capability.switch", title: "Outlets", multiple: true
        }

        if (!state.deviceID) {
            section("Initial Thermostat Settings..."){
                input "heatingSetPoint", "decimal", title: "Heating Setpoint in $displayUnits (min $setpointDistance $displayUnits lower than cooling)", required: true, defaultValue: defaultHeat
                input "coolingSetPoint", "decimal", title: "Cooling Setpoint in $displayUnits (min $setpointDistance $displayUnits higher than heating)", required: true, defaultValue: defaultCool
                input "thermostatThreshold", "decimal", title: "Temperature Threshold in $displayUnits", required: true, defaultValue: defaultThresh
            }
        }
    
        section("Log Settings...") {
            input name: "logLevel", type: "enum", title: "Live Logging Level", options: [[0: 'Disabled'], [1: 'Error'], [2: 'Warning'], [3: 'Info'], [4: 'Debug'], [5: 'Trace']], defaultValue: 3
            input "logDropLevelTime", "decimal", title: "Drop down to Info Level Minutes", required: true, defaultValue: 5
        }
    }
}

def installed() {
    state.loggingLevel = settings.logLevel ? settings.logLevel.toInteger() : 3
    logger("trace", "Installed Running vThermostat: $app.label")
    
    // Guaranteed-unique ID using app.id
    state.deviceID = "avt-" + app.id

    def label = app.getLabel()
    logger("info", "Creating vThermostat : ${label} with device id: ${state.deviceID}")
    
    def thermostat = null
    try {
        thermostat = addChildDevice("jshimota", "Advanced vThermostat Device Custom v2", state.deviceID, null, [label: label, name: label, completedSetup: true])
    } catch(e) {
        logger("error", "Error adding vThermostat child ${label}: ${e}")
    }
    initialize(thermostat)
}

def updated() {
    state.loggingLevel = settings.logLevel ? settings.logLevel.toInteger() : 3
    logger("trace", "Updated Running vThermostat: $app.label")
    initialize(getThermostat())
}

def uninstalled() {
    logger("info", "Child Device " + state.deviceID + " removed")
    deleteChildDevice(state.deviceID)
}

def initialize(thermostatInstance) {
    if (!thermostatInstance) thermostatInstance = getThermostat()
    if (!thermostatInstance) {
        logger("error", "initialize() - Device instance not found.")
        return
    }

    logger("trace", "Initialize Running vThermostat: $app.label")

    unsubscribe()
    unschedule()

    state.loggingLevel = settings.logLevel ? settings.logLevel.toInteger() : 3
    
    if (state.loggingLevel > 3) {
        runIn(settings.logDropLevelTime.toInteger() * 60, logsDropLevel)
    }

    def thermostatMode = "off"
    if (heatOutlets && coolOutlets) {
        thermostatMode = "auto"
    } else if (heatOutlets) {
        thermostatMode = "heat"
    } else if (coolOutlets) {
        thermostatMode = "cool"
    }
    
    if (heatingSetPoint != null) thermostatInstance.setHeatingSetpoint(heatingSetPoint.toDouble())
    if (coolingSetPoint != null) thermostatInstance.setCoolingSetpoint(coolingSetPoint.toDouble())
    if (thermostatThreshold != null) thermostatInstance.setThermostatThreshold(thermostatThreshold.toDouble())
    
    thermostatInstance.setLogLevel(state.loggingLevel)
    thermostatInstance.setThermostatMode(thermostatMode)

    subscribe(sensors, "temperature", temperatureHandler)
    subscribe(thermostatInstance, "thermostatOperatingState", thermostatStateHandler)

    updateTemperature()
}

def getThermostat() {
    if (!state.deviceID) {
        logger("error", "getThermostat cannot access deviceID!")
        return null
    }
    return getChildDevices().find { d -> d.deviceNetworkId == state.deviceID }
}

def temperatureHandler(evt) {
    logger("debug", "Temperature changed to ${evt.value}")
    updateTemperature()
}

def updateTemperature() {
    def thermostat = getThermostat()
    if (!thermostat || !sensors) return null

    def validTemps = []
    sensors.each { sensor ->
        def val = sensor.currentValue("temperature")
        if (val != null) {
            if (val instanceof Number) {
                validTemps << val.toDouble()
            } else if (val.toString().isNumber()) {
                validTemps << val.toString().toDouble()
            } else {
                logger("warn", "updateTemperature() - Invalid non-numeric temperature value received from ${sensor.displayName}: ${val}")
            }
        }
    }

    if (validTemps.isEmpty()) return null

    def avgTemp = (validTemps.sum() / validTemps.size()).toDouble().round(1)
    
    if (thermostat.currentValue("temperature") != avgTemp) {
        thermostat.setTemperature(avgTemp)
    }
    return avgTemp
}

def thermostatStateHandler(evt) {
    if (evt.value) {
        logger("info", "Thermostat state changed to ${evt.value}")
        setOutletsState(evt.value)
    }
}

def setOutletsState(opState = null) {
    def thermostat = getThermostat()
    if (!thermostat) return

    def currentState = opState ?: thermostat.currentValue("thermostatOperatingState")

    if (currentState == "heating") {
        safelyControlSwitches(coolOutlets, "off")
        safelyControlSwitches(heatOutlets, "on")
    } else if (currentState == "cooling") {
        safelyControlSwitches(heatOutlets, "off")
        safelyControlSwitches(coolOutlets, "on")
    } else {
        safelyControlSwitches(heatOutlets, "off")
        safelyControlSwitches(coolOutlets, "off")
    }
}

def safelyControlSwitches(devices, String targetState) {
    if (!devices) return
    devices.each { device ->
        if (device.currentValue("switch") != targetState) {
            if (device.hasCommand(targetState)) {
                device."${targetState}"()
            }
        }
    }
}

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

def logsDropLevel() {
    def thermostat = getThermostat()
    app.updateSetting("logLevel", [type: "enum", value: "3"])
    state.loggingLevel = 3
    if (thermostat) thermostat.setLogLevel(3)
    logger("warn", "App logging level reset to 3")
}

def getTemperatureScale() { return "${location.temperatureScale}" }
def getDisplayUnits() { return getTemperatureScale() == "C" ? "°C" : "°F" }