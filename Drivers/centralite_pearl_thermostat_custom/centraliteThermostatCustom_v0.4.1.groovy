/*
 * CentraLite Pearl Thermostat Custom - Original code derived from Smartthings 2021
 * Version: 0.4.1
 *
 * Change History:
 * 2021-09-30    dagrider      0.1.0       Starting version
 * 2026-06-02    jshimota      0.1.1       Initial edit, cleanup GNU, basics, remove excess comments
 * 2026-06-03    jshimota      0.2.0       Gemini Modernization and optimization
 * 2026-06-03    jshimota      0.2.1       Adding in log and debug control
 * 2026-06-03    jshimota      0.2.2       Gemini improvements and bug hunt
 * 2026-06-03    jshimota      0.2.8       Intercept Fan Circulate Exception
 * 2026-06-03    jshimota      0.2.9       Sanitized inline tracking tokens
 * 2026-06-03    jshimota      0.3.0       Implemented 30-min on/off circulation scheduling logic
 * 2026-06-03    jshimota      0.3.1       Fixed UI dropdown options mapping to use platform standard tokens
 * 2026-06-03    jshimota      0.3.2       Added explicit Initialize command to reveal UI button
 * 2026-06-03    jshimota      0.3.3       Added explicit fanOff command to reveal UI action button
 * 2026-06-03    jshimota      0.3.4       Added supportedThermostatModes attribute definition and variable assignment to populate system UI controls
 * 2026-06-03	 jshimota      0.3.5       Repaired Battery and Power Source code
 * 2026-06-03    jshimota      0.3.6       Changed Setpoint Level button namespace
 * 2026-06-03    jshimota      0.3.7       Fixed 'Auto' mode (which isn't supported on this device)
 * 2026-06-03    jshimota	   0.3.9	   Modified text string to display temperature values with °F
 * 2026-06-03    jshimota  	   0.4.1	   text cleanup of UI to show Celsius or Fahrenheit as needed.
 */


static String version() { return '0.4.1' }

metadata {
    definition(
        name: "CentraLite Pearl Thermostat Custom",
        namespace: "jshimota",
        author: "James Shimota",
        filename: "centraliteThermostatCustom.groovy",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/centralite_thermostat_custom/centraliteThermostatCustom.groovy"
    ) {
        capability "Actuator"
        capability "Temperature Measurement"
        capability "Thermostat"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "Battery"
                                
        command "raiseHeatSetpointLevel"
        command "lowerHeatSetpointLevel"
        command "raiseCoolSetpointLevel"
        command "lowerCoolSetpointLevel"
        command "toggleHoldMode" 
        command "initialize"
        command "fanOff"

        // Overrides the standard UI parameter helper text for the interactive command fields
        command "setCoolingSetpoint", [[name: "degrees", type: "NUMBER", description: "Cooling Setpoint in degrees Fahrenheit "]]
        command "setHeatingSetpoint", [[name: "degrees", type: "NUMBER", description: "Heating Setpoint in degrees Fahrenheit "]]

        attribute "thermostatHoldMode", "string"
        attribute "powerSource", "string"
        
        attribute "thermostatFanModes", "JSON_OBJECT"
        attribute "supportedThermostatModes", "JSON_OBJECT"
                               
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0201,0202,0204,0B05", outClusters: "000A, 0019"
    }

    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true 
    }
}

def installed() {
    if (txtEnable) log.info "Installing CentraLite Pearl Thermostat Custom..."
    initialize()
}

def updated() {
    if (txtEnable) log.info "Updating settings..."
    initialize()
}

def initialize() {
    if (dbgEnable) runIn(1800, logsOff)
    
    def fanOptionsList = ["on", "auto", "off", "circulate"]
    sendEvent(name: "thermostatFanModes", value: groovy.json.JsonOutput.toJson(fanOptionsList))
    
    def systemModesList = ["off", "heat", "cool", "emergencyHeat"]
    sendEvent(name: "supportedThermostatModes", value: groovy.json.JsonOutput.toJson(systemModesList))
    
    if (device.currentValue("battery") == null) {
        sendEvent(name: "battery", value: 100, unit: "%")
    }
    if (device.currentValue("powerSource") == null) {
        sendEvent(name: "powerSource", value: "unknown")
    }
    
    configure()
    runIn(2, refresh)
}

def logsOff() {
    log.warn "Debug logging automatically disabled."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}
 
def configure() {
    if (txtEnable) log.info "${device.displayName} received configuration query."
    
    def cmds = zigbee.batteryConfig() +
               zigbee.configureReporting(0x0201, 0x0000, 0x29, 10, 600, 50) +  // Local Temperature
               zigbee.configureReporting(0x0201, 0x0011, 0x29, 5, 300, 50) +   // Cooling Setpoint
               zigbee.configureReporting(0x0201, 0x0012, 0x29, 5, 300, 50) +   // Heating Setpoint
               zigbee.configureReporting(0x0201, 0x001C, 0x30, 5, 300, 1) +    // System Mode
               zigbee.configureReporting(0x0201, 0x0029, 0x19, 5, 300, 1) +    // Running State
               zigbee.configureReporting(0x0201, 0x0023, 0x30, 5, 300, 1) +    // Hold Mode
               zigbee.configureReporting(0x0202, 0x0000, 0x30, 5, 300, 1)      // Fan Mode

    return cmds
}
 
def refresh() {
    if (dbgEnable) log.debug "Refresh requested"
 
    return zigbee.readAttribute(0x0000, 0x0007) +
           zigbee.readAttribute(0x0201, 0x0000) +
           zigbee.readAttribute(0x0201, 0x0011) +
           zigbee.readAttribute(0x0201, 0x0012) +
           zigbee.readAttribute(0x0201, 0x001C) +
           zigbee.readAttribute(0x0201, 0x001E) +
           zigbee.readAttribute(0x0201, 0x0023) +
           zigbee.readAttribute(0x0201, 0x0029) +
           zigbee.readAttribute(0x0001, 0x0020) +
           zigbee.readAttribute(0x0202, 0x0000)
}
 
def raiseHeatSetpointLevel() { changeSetpoint("heatingSetpoint", 1) }
def lowerHeatSetpointLevel() { changeSetpoint("heatingSetpoint", -1) }
def raiseCoolSetpointLevel() { changeSetpoint("coolingSetpoint", 1) }
def lowerCoolSetpointLevel() { changeSetpoint("coolingSetpoint", -1) }

private def changeSetpoint(String attributeName, int delta) {
    if (isHoldOn()) return
    def currentVal = device.currentValue(attributeName) ?: (attributeName.contains("Heat") ? 68 : 74)
    int nextLevel = currentVal.toInteger() + delta
    
    if (attributeName == "heatingSetpoint") {
        setHeatingSetpoint(nextLevel)
    } else {
        setCoolingSetpoint(nextLevel)
    }
}
 
def parse(String description) {
    if (dbgEnable) log.debug "Parsing: $description"
    def map = [:]
 
    if (description?.startsWith("read attr -") || description?.startsWith("catchall:")) {
        def descMap = zigbee.parseDescriptionAsMap(description)
        
        def clusterInt = descMap.cluster ? Integer.parseInt(descMap.cluster, 16) : null
        def attrInt = descMap.attrId ? Integer.parseInt(descMap.attrId, 16) : null
        
        switch(clusterInt) {
            case 0x0201: // Thermostat Cluster
                if (attrInt == 0x0000) {
                    map.name = "temperature"
                    map.value = getTemperature(descMap.value)
                    map.unit = getTemperatureScale()
                } else if (attrInt == 0x0011) {
                    map.name = "coolingSetpoint"
                    map.value = getTemperature(descMap.value)
                    map.unit = getTemperatureScale()
                } else if (attrInt == 0x0012) {
                    map.name = "heatingSetpoint"
                    map.value = getTemperature(descMap.value)
                    map.unit = getTemperatureScale()
                } else if (attrInt == 0x001C) {
                    map.name = "thermostatMode"
                    map.value = getModeMap()[descMap.value] ?: "off"
                } else if (attrInt == 0x001E) {
                    map.name = "thermostatRunMode"
                    map.value = getModeMap()[descMap.value] ?: "off"
                } else if (attrInt == 0x0023) {
                    map.name = "thermostatHoldMode"
                    map.value = getHoldModeMap()[descMap.value] ?: "holdOff"
                } else if (attrInt == 0x0029) {
                    map.name = "thermostatOperatingState"
                    map.value = getThermostatOperatingStateMap()[descMap.value] ?: "idle"
                }
                break
                
            case 0x0202: // Fan Control
                if (attrInt == 0x0000) {
                    if (device.currentValue("thermostatFanMode") != "circulate") {
                        map.name = "thermostatFanMode"
                        map.value = getFanModeMap()[descMap.value] ?: "auto"
                    }
                }
                break
                
            case 0x0001: // Power Configuration
                if (attrInt == 0x0020) {
                    map.name = "battery"
                    map.value = getBatteryLevel(descMap.value)
                    map.unit = "%"
                }
                break
                
            case 0x0000: // Basic
                if (attrInt == 0x0007) {
                    map.name = "powerSource"
                    map.value = getPowerSource()[descMap.value] ?: "unknown"
                }
                break
        }
    }
 
    if (map) {
        if (map.name in ["coolingSetpoint", "heatingSetpoint"]) {
            String fullScaleName = (map.unit == "F") ? "Fahrenheit" : "Celsius"
            map.descriptionText = "${device.displayName} ${map.name} is ${map.value} degrees ${fullScaleName}"
        } else if (map.unit == "F" || map.unit == "C") {
            map.descriptionText = "${device.displayName} ${map.name} is ${map.value}°${map.unit}"
        } else {
            map.descriptionText = "${device.displayName} ${map.name} is ${map.value}${map.unit ?: ''}"
        }
        
        if (txtEnable) log.info map.descriptionText
        return createEvent(map)
    }
    return null
}
 
def getModeMap() { ["00":"off", "01":"auto", "03":"cool", "04":"heat", "05":"emergency heat", "06":"precooling", "07":"fan only", "08":"dry", "09":"sleep"] }
def modes() { ["off", "cool", "heat", "emergencyHeat"] }
def getHoldModeMap() { ["00":"holdOff", "01":"holdOn"] }
def getPowerSource() { ["01":"24VAC", "03":"Battery", "81":"24VAC"] }
def getFanModeMap() { ["00":"off", "04":"on", "05":"auto"] }
def getThermostatOperatingStateMap() {
    ["0000":"idle", "0001":"heating", "0002":"cooling", "0004":"fan only", "0005":"heating", "0006":"cooling", "0008":"heating", "0009":"heating", "000A":"heating", "000D":"heating", "0010":"cooling", "0012":"cooling", "0014":"cooling", "0015":"cooling"]
}
 
def getTemperature(value) {
    if (value == null) return null
    def celsius = Integer.parseInt(value, 16) / 100.0
    return (getTemperatureScale() == "C") ? celsius : Math.round(celsiusToFahrenheit(celsius))
}
 
def toggleHoldMode() {
    String currentHoldMode = device.currentValue("thermostatHoldMode") ?: "holdOff"
    return (currentHoldMode == "holdOn") ? holdOff() : holdOn()
}
 
def setThermostatMode(String value) {
    String normalizedValue = value.toLowerCase().replaceAll(/\s+(.)/) { match, group -> group.toUpperCase() }
    if (normalizedValue == "emergencyHeat") normalizedValue = "emergencyHeat"
    
    if (this.hasProperty(normalizedValue) || this.respondsTo(normalizedValue)) {
        "$normalizedValue"()
    } else {
        log.error "Unsupported thermostat mode requested: $value"
    }
}

def setThermostatFanMode(String value) { 
    if (value == "fanOn" || value == "on") {
        fanOn()
    } else if (value == "fanAuto" || value == "auto") {
        fanAuto()
    } else if (value == "fanOff" || value == "off") {
        fanOff()
    } else if (value == "fanCirculate" || value == "circulate") {
        fanCirculate()
    } else {
        log.error "Unsupported fan mode requested: $value"
    }
}

def setThermostatHoldMode(String value) { "$value"() }
 
def off() {
    if (txtEnable) log.info "${device.displayName} mode set to Off."                                            
    sendEvent(name: "thermostatMode", value: "off")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 0)
}
 
def cool() {
    if (txtEnable) log.info "${device.displayName} mode set to Cool."                                                 
    sendEvent(name: "thermostatMode", value: "cool")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 3)
}
 
def heat() {
    if (txtEnable) log.info "${device.displayName} mode set to Heat."                                                 
    sendEvent(name: "thermostatMode", value: "heat")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 4)
}
 
def emergencyHeat() {
    if (txtEnable) log.info "${device.displayName} mode set to Emergency Heat."                                                                       
    sendEvent(name: "thermostatMode", value: "emergencyHeat")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 5)
}

def auto() {
    log.warn "${device.displayName} does not support automatic system changeover. Ignoring request."
}
 
def on() { fanOn() }
 
def fanOn() {
    if (txtEnable) log.info "${device.displayName} fan set to On."        
    unschedule(runCirculateCycle) 
    sendEvent(name: "thermostatFanMode", value: "on")
    return zigbee.writeAttribute(0x0202, 0x00, 0x30, 4)
}
 
def fanAuto() {
    if (txtEnable) log.info "${device.displayName} fan set to Auto."    
    unschedule(runCirculateCycle) 
    sendEvent(name: "thermostatFanMode", value: "auto")
    return zigbee.writeAttribute(0x0202, 0x00, 0x30, 5)
}

def fanOff() {
    if (txtEnable) log.info "${device.displayName} fan shut Off."    
    unschedule(runCirculateCycle) 
    sendEvent(name: "thermostatFanMode", value: "off")
    return zigbee.writeAttribute(0x0202, 0x00, 0x30, 0)
}

def fanCirculate() {
    if (txtEnable) log.info "${device.displayName} fan set to Circulate (30m Loop)."
    sendEvent(name: "thermostatFanMode", value: "circulate")
    runCirculateCycle("on")
}

def runCirculateCycle(String targetState = null) {
    if (device.currentValue("thermostatFanMode") != "circulate") return

    if (targetState == null) {
        String lastCycleState = device.currentValue("thermostatFanCycleState") ?: "off"
        targetState = (lastCycleState == "on") ? "off" : "on"
    }

    sendEvent(name: "thermostatFanCycleState", value: targetState, isStateChange: true, displayed: false)

    if (targetState == "on") {
        sendHubCommand(new hubitat.device.HubAction(zigbee.writeAttribute(0x0202, 0x00, 0x30, 4)[0], hubitat.device.Protocol.ZIGBEE))
        runIn(1800, runCirculateCycle, [overwrite: true, data: "off"])
    } else {
        sendHubCommand(new hubitat.device.HubAction(zigbee.writeAttribute(0x0202, 0x00, 0x30, 5)[0], hubitat.device.Protocol.ZIGBEE))
        runIn(1800, runCirculateCycle, [overwrite: true, data: "on"])
    }
}
 
def holdOn() {
    sendEvent(name: "thermostatHoldMode", value: "holdOn")
    zigbee.writeAttribute(0x0201, 0x23, 0x30, 1)
}
 
def holdOff() {
    sendEvent(name: "thermostatHoldMode", value: "holdOff")
    zigbee.writeAttribute(0x0201, 0x23, 0x30, 0)
}
 
private getBatteryLevel(rawValue) {
    def intValue = Integer.parseInt(rawValue, 16)
    def min = 21
    def max = 30
    def pct = ((intValue - min) / (max - min) * 100) as int
    return Math.max(0, Math.min(pct, 100))
}

private isHoldOn() {
    return (device.currentValue("thermostatHoldMode") == "holdOn")
}
 
def setHeatingSetpoint(degrees) {
    processSetpoint(degrees, 0x12)
}
 
def setCoolingSetpoint(degrees) {
    processSetpoint(degrees, 0x11)
}

private def processSetpoint(degrees, int attributeId) {
    if (isHoldOn() || degrees == null) return
    
    def isC = (getTemperatureScale() == "C")
    int maxTemp = isC ? 44 : 86
    int minTemp = isC ? 7 : 30
    
    int degreesInteger = Math.round(degrees).toInteger()
    degreesInteger = Math.max(minTemp, Math.min(degreesInteger, maxTemp))
    
    double celsius = isC ? degreesInteger : fahrenheitToCelsius(degreesInteger)
    int finalValue = Math.round(celsius * 100).toInteger()
    
    String attrName = (attributeId == 0x12) ? "heatingSetpoint" : "coolingSetpoint"
    sendEvent(name: attrName, value: degreesInteger, unit: getTemperatureScale())
    return zigbee.writeAttribute(0x0201, attributeId, 0x29, finalValue)
}