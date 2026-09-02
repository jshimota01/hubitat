/*
 * CentraLite Pearl Thermostat Custom - Automated 30m On / 30m Off Software Circulation Loop
 * Version: 0.3.1
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
 */

static String version() { return '0.3.1' }

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
                                
        command "raiseHeatLevel"
        command "lowerHeatLevel"
        command "raiseCoolLevel"
        command "lowerCoolLevel"
        command "toggleHoldMode" 

        attribute "thermostatHoldMode", "string"
        attribute "powerSource", "string"
        
        // Includes standardized options for platform-supported drop-downs
        attribute "thermostatFanModes", "JSON_OBJECT"
                               
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0201,0202,0204,0B05", outClusters: "000A, 0019"
    }

    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: false
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
    
    // UI presentation list context - standardized to Hubitat-native tokens
    def fanOptionsList = ["on", "auto", "off", "circulate"]
    String jsonString = groovy.json.JsonOutput.toJson(fanOptionsList)
    sendEvent(name: "thermostatFanModes", value: jsonString)
    
    configure()
}

def logsOff() {
    log.warn "Debug logging automatically disabled."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}
 
def configure() {
    if (txtEnable) log.info "${device.displayName} received 'configure' request."                                                         
    if (dbgEnable) log.debug "Configuring reporting and bindings..."
    
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
 
def raiseHeatLevel() { changeSetpoint("heatingSetpoint", 1) }
def lowerHeatLevel() { changeSetpoint("heatingSetpoint", -1) }
def raiseCoolLevel() { changeSetpoint("coolingSetpoint", 1) }
def lowerCoolLevel() { changeSetpoint("coolingSetpoint", -1) }

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
                    // Only process incoming reporting status if we are not actively handling a software circulation schedule loop
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
        map.descriptionText = "${device.displayName} ${map.name} is ${map.value}${map.unit ?: ''}"
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
        log.error "Unsupported thermostat mode string requested: $value"
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
        log.error "Unsupported fan mode target selector: $value"
    }
}

def setThermostatHoldMode(String value) { "$value"() }
 
def off() {
    if (txtEnable) log.info "${device.displayName} received 'Off' request."                                            
    sendEvent(name: "thermostatMode", value: "off", descriptionText: "${device.displayName} mode is set to off")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 0)
}
 
def cool() {
    if (txtEnable) log.info "${device.displayName} received 'Cool' request."                                                 
    sendEvent(name: "thermostatMode", value: "cool", descriptionText: "${device.displayName} mode is set to cool")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 3)
}
 
def heat() {
    if (txtEnable) log.info "${device.displayName} received 'Heat' request."                                                 
    sendEvent(name: "thermostatMode", value: "heat", descriptionText: "${device.displayName} mode is set to heat")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 4)
}
 
def emergencyHeat() {
    if (txtEnable) log.info "${device.displayName} received 'Emergency Heat' request."                                                                       
    sendEvent(name: "thermostatMode", value: "emergencyHeat", descriptionText: "${device.displayName} mode is set to emergency heat")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 5)
}
 
def on() { fanOn() }
 
def fanOn() {
    if (txtEnable) log.info "${device.displayName} received 'Fan On' request."        
    unschedule(runCirculateCycle) 
    sendEvent(name: "thermostatFanMode", value: "on", descriptionText: "${device.displayName} fan mode is set to on")
    return zigbee.writeAttribute(0x0202, 0x00, 0x30, 4)
}
 
def auto() { /* Not supported */ }
 
def fanAuto() {
    if (txtEnable) log.info "${device.displayName} received 'Fan Auto' request."    
    unschedule(runCirculateCycle) 
    sendEvent(name: "thermostatFanMode", value: "auto", descriptionText: "${device.displayName} fan mode is set to auto")
    return zigbee.writeAttribute(0x0202, 0x00, 0x30, 5)
}

def fanOff() {
    if (txtEnable) log.info "${device.displayName} received 'Fan Off' request."    
    unschedule(runCirculateCycle) 
    sendEvent(name: "thermostatFanMode", value: "off", descriptionText: "${device.displayName} fan mode is set to off")
    return zigbee.writeAttribute(0x0202, 0x00, 0x30, 0)
}

// Triggers the custom 30m ON / 30m OFF duty loop
def fanCirculate() {
    if (txtEnable) log.info "${device.displayName} received 'Fan Circulate' request. Beginning 30m On/30 min Off cycle."
    sendEvent(name: "thermostatFanMode", value: "circulate", descriptionText: "${device.displayName} fan mode is set to circulate")
    runCirculateCycle("on")
}

// Software scheduler worker loop
def runCirculateCycle(String targetState = null) {
    if (device.currentValue("thermostatFanMode") != "circulate") {
        if (dbgEnable) log.debug "Circulation cycle stopped because fan mode is no longer set to circulate."
        return
    }

    if (targetState == null) {
        String lastCycleState = device.currentValue("thermostatFanCycleState") ?: "off"
        targetState = (lastCycleState == "on") ? "off" : "on"
    }

    sendEvent(name: "thermostatFanCycleState", value: targetState, isStateChange: true, displayed: false)

    if (targetState == "on") {
        if (txtEnable) log.info "${device.displayName} Fan Circulation Cycle: Turning fan ON for 30 minutes."
        sendHubCommand(new hubitat.device.HubAction(zigbee.writeAttribute(0x0202, 0x00, 0x30, 4)[0], hubitat.device.Protocol.ZIGBEE))
        runIn(1800, runCirculateCycle, [overwrite: true, data: "off"])
    } else {
        if (txtEnable) log.info "${device.displayName} Fan Circulation Cycle: Turning fan OFF/Auto for 30 minutes."
        sendHubCommand(new hubitat.device.HubAction(zigbee.writeAttribute(0x0202, 0x00, 0x30, 5)[0], hubitat.device.Protocol.ZIGBEE))
        runIn(1800, runCirculateCycle, [overwrite: true, data: "on"])
    }
}
 
def holdOn() {
    if (txtEnable) log.info "${device.displayName} received 'Hold On' request."    
    sendEvent(name: "thermostatHoldMode", value: "holdOn", descriptionText: "${device.displayName} hold mode is set to holdOn")
    zigbee.writeAttribute(0x0201, 0x23, 0x30, 1)
}
 
def holdOff() {
    if (txtEnable) log.info "${device.displayName} received 'Hold Off' request."    
    sendEvent(name: "thermostatHoldMode", value: "holdOff", descriptionText: "${device.displayName} hold mode is set to holdOff")
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
    sendEvent(name: attrName, value: degreesInteger, unit: getTemperatureScale(), descriptionText: "${device.displayName} ${attrName} set to ${degreesInteger}°${getTemperatureScale()}")
    return zigbee.writeAttribute(0x0201, attributeId, 0x29, finalValue)
}