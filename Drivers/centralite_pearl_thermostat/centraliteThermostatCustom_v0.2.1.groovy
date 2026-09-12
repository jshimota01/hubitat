/*
 * CentraLite Pearl Thermostat Custom
 *
 *
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2021-09-30    dagrider      0.1.0       Starting version
 *      2026-06-02    jshimota      0.1.1       Initial edit, cleanup GNU, basics, remove excess comments
 * 		2026-06-03	  jshimota      0.2.0		Gemnini Modernization and optimization
 * 		2026-06-03	  jshimota      0.2.1		Adding in log and debug control
 *
 */

static String version() { return '0.2.1' }

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
        command "setThermostatHoldMode"

        attribute "thermostatHoldMode", "string"
        attribute "powerSource", "string"
                               
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0201,0202,0204,0B05", outClusters: "000A, 0019"
    }

    preferences {
        input name: "dbgEnable",  type: "bool",   title: "Enable debug logging",        defaultValue: false
        input name: "txtEnable",  type: "bool",   title: "Enable descriptionText logging", defaultValue: true  }
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
    configure()
}

def logsOff() {
    log.warn "Debug logging automatically disabled."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}
 
def configure() {
    if (dbgEnable) log.debug "Configuring reporting and bindings..."
    
    // Hubitat native binding & reporting configuration
    def cmds = zigbee.batteryConfig() +
               zigbee.thermostatConfig() +
               zigbee.configureReporting(0x0201, 0x001C, 0x30, 5, 300, 1) + // System Mode
               zigbee.configureReporting(0x0201, 0x0029, 0x19, 5, 300, 1) + // Running State
               zigbee.configureReporting(0x0201, 0x0023, 0x30, 5, 300, 1) + // Hold Mode
               zigbee.configureReporting(0x0202, 0x0000, 0x30, 5, 300, 1)   // Fan Mode

    return cmds
}
 
def refresh() {
    if (dbgEnable) log.debug "Refresh requested"
 
    return zigbee.readAttribute(0x0000, 0x0007) + // Power Source
           zigbee.readAttribute(0x0201, 0x0000) + // Local Temperature
           zigbee.readAttribute(0x0201, 0x0011) + // Cooling Setpoint
           zigbee.readAttribute(0x0201, 0x0012) + // Heating Setpoint
           zigbee.readAttribute(0x0201, 0x001C) + // Thermostat Mode 
           zigbee.readAttribute(0x0201, 0x001E) + // Run Mode
           zigbee.readAttribute(0x0201, 0x0023) + // Hold Mode
           zigbee.readAttribute(0x0201, 0x0029) + // Operating State
           zigbee.readAttribute(0x0001, 0x0020) + // Battery
           zigbee.readAttribute(0x0202, 0x0000)   // Fan Mode
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
        
        switch(descMap.cluster) {
            case "0201": // Thermostat Cluster
                if (descMap.attrId == "0000") {
                    map.name = "temperature"
                    map.value = getTemperature(descMap.value)
                } else if (descMap.attrId == "0011") {
                    map.name = "coolingSetpoint"
                    map.value = getTemperature(descMap.value)
                } else if (descMap.attrId == "0012") {
                    map.name = "heatingSetpoint"
                    map.value = getTemperature(descMap.value)
                } else if (descMap.attrId == "001C") {
                    map.name = "thermostatMode"
                    map.value = getModeMap()[descMap.value] ?: "off"
                } else if (descMap.attrId == "001E") {
                    map.name = "thermostatRunMode"
                    map.value = getModeMap()[descMap.value] ?: "off"
                } else if (descMap.attrId == "0023") {
                    map.name = "thermostatHoldMode"
                    map.value = getHoldModeMap()[descMap.value] ?: "holdOff"
                } else if (descMap.attrId == "0029") {
                    map.name = "thermostatOperatingState"
                    map.value = getThermostatOperatingStateMap()[descMap.value] ?: "idle"
                }
                break
                
            case "0202": // Fan Control
                if (descMap.attrId == "0000") {
                    map.name = "thermostatFanMode"
                    map.value = getFanModeMap()[descMap.value] ?: "fanAuto"
                }
                break
                
            case "0001": // Power Configuration
                if (descMap.attrId == "0020") {
                    map.name = "battery"
                    map.value = getBatteryLevel(descMap.value)
                }
                break
                
            case "0000": // Basic
                if (descMap.attrId == "0007") {
                    map.name = "powerSource"
                    map.value = getPowerSource()[descMap.value] ?: "unknown"
                }
                break
        }
    }
 
    if (map) {
        if (dbgEnable) log.debug "Creating event: $map"
        return createEvent(map)
    }
    return null
}
 
def getModeMap() { ["00":"off", "01":"auto", "03":"cool", "04":"heat", "05":"emergency heat", "06":"precooling", "07":"fan only", "08":"dry", "09":"sleep"] }
def modes() { ["off", "cool", "heat", "emergencyHeat"] }
def getHoldModeMap() { ["00":"holdOff", "01":"holdOn"] }
def getPowerSource() { ["01":"24VAC", "03":"Battery", "81":"24VAC"] }
def getFanModeMap() { ["04":"fanOn", "05":"fanAuto"] }
def getThermostatOperatingStateMap() {
    ["0000":"idle", "0001":"heating", "0002":"cooling", "0004":"fan only", "0005":"heating", "0006":"cooling", "0008":"heating", "0009":"heating", "000A":"heating", "000D":"heating", "0010":"cooling", "0012":"cooling", "0014":"cooling", "0015":"cooling"]
}
 
def getTemperature(value) {
    if (value == null) return null
    def celsius = zigbee.hexStringToInt(value) / 100.0
    return (getTemperatureScale() == "C") ? celsius : Math.round(celsiusToFahrenheit(celsius))
}
 
def setThermostatHoldMode() {
    String currentHoldMode = device.currentValue("thermostatHoldMode") ?: "holdOff"
    return (currentHoldMode == "holdOn") ? holdOff() : holdOn()
}
 
def setThermostatMode(String value) { "$value"() }
def setThermostatFanMode(String value) { "$value"() }
def setThermostatHoldMode(String value) { "$value"() }
 
def off() {
    sendEvent(name: "thermostatMode", value: "off")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 0)
}
 
def cool() {
    sendEvent(name: "thermostatMode", value: "cool")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 3)
}
 
def heat() {
    sendEvent(name: "thermostatMode", value: "heat")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 4)
}
 
def emergencyHeat() {
    sendEvent(name: "thermostatMode", value: "emergencyHeat")
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 5)
}
 
def on() { fanOn() }
 
def fanOn() {
    sendEvent(name: "thermostatFanMode", value: "fanOn")
    zigbee.writeAttribute(0x0202, 0x00, 0x30, 4)
}
 
def auto() { /* Not supported */ }
 
def fanAuto() {
    sendEvent(name: "thermostatFanMode", value: "fanAuto")
    zigbee.writeAttribute(0x0202, 0x00, 0x30, 5)
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
    def intValue = zigbee.hexStringToInt(rawValue)
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