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
 *		2026-06-03	  jshimota		0.2.2		Gemini improvements and bug hunt
 *
 */

/*
 * CentraLite Pearl Thermostat Custom - Intercept Fan Circulate Exception
 * Version: 0.2.8
 */

static String version() { return '0.2.8' }

metadata {
    definition(
        name: "CentraLite Pearl Thermostat Custom",
        namespace: "jshimota",
        author: "James Shimota",
        filename: "centraliteThermostatCustom.groovy",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/centralite_thermostat_custom/centraliteThermostatCustom.groovy"
    ) {
        capability "Actuator" [cite: 8]
        capability "Temperature Measurement" [cite: 8]
        capability "Thermostat" [cite: 9]
        capability "Configuration" [cite: 9]
        capability "Refresh" [cite: 9]
        capability "Sensor" [cite: 9]
        capability "Battery" [cite: 9]
                                
        command "raiseHeatLevel" [cite: 10]
        command "lowerHeatLevel" [cite: 10]
        command "raiseCoolLevel" [cite: 10]
        command "lowerCoolLevel" [cite: 10]
        command "toggleHoldMode" 

        attribute "thermostatHoldMode", "string" [cite: 10]
        attribute "powerSource", "string" [cite: 10]
        
        // Tells Hubitat's UI to render a dropdown list selector for fan modes
        attribute "thermostatFanModes", "JSON_OBJECT"
                               
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0201,0202,0204,0B05", outClusters: "000A, 0019" [cite: 11]
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
    
    // UI presentation list context
    def fanOptionsList = ["fanOn", "fanAuto", "fanOff"]
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
    if (dbgEnable) log.debug "Refresh requested" [cite: 16]
 
    return zigbee.readAttribute(0x0000, 0x0007) + [cite: 16]
           zigbee.readAttribute(0x0201, 0x0000) + [cite: 17]
           zigbee.readAttribute(0x0201, 0x0011) + [cite: 17]
           zigbee.readAttribute(0x0201, 0x0012) + [cite: 17]
           zigbee.readAttribute(0x0201, 0x001C) + [cite: 17]
           zigbee.readAttribute(0x0201, 0x001E) + [cite: 17]
           zigbee.readAttribute(0x0201, 0x0023) + [cite: 17]
           zigbee.readAttribute(0x0201, 0x0029) + [cite: 18]
           zigbee.readAttribute(0x0001, 0x0020) + [cite: 18]
           zigbee.readAttribute(0x0202, 0x0000) [cite: 18]
}
 
def raiseHeatLevel() { changeSetpoint("heatingSetpoint", 1) } [cite: 18]
def lowerHeatLevel() { changeSetpoint("heatingSetpoint", -1) } [cite: 19]
def raiseCoolLevel() { changeSetpoint("coolingSetpoint", 1) } [cite: 19]
def lowerCoolLevel() { changeSetpoint("coolingSetpoint", -1) } [cite: 19, 20]

private def changeSetpoint(String attributeName, int delta) {
    if (isHoldOn()) return
    def currentVal = device.currentValue(attributeName) ?: (attributeName.contains("Heat") ? 68 : 74) [cite: 19]
    int nextLevel = currentVal.toInteger() + delta [cite: 18, 19]
    
    if (attributeName == "heatingSetpoint") { [cite: 19]
        setHeatingSetpoint(nextLevel) [cite: 18]
    } else {
        setCoolingSetpoint(nextLevel) [cite: 19]
    }
}
 
def parse(String description) {
    if (dbgEnable) log.debug "Parsing: $description" [cite: 20]
    def map = [:]
 
    if (description?.startsWith("read attr -") || description?.startsWith("catchall:")) { [cite: 20]
        def descMap = zigbee.parseDescriptionAsMap(description) [cite: 20]
        
        def clusterInt = descMap.cluster ? Integer.parseInt(descMap.cluster, 16) : null
        def attrInt = descMap.attrId ? Integer.parseInt(descMap.attrId, 16) : null
        
        switch(clusterInt) {
            case 0x0201: // Thermostat Cluster
                if (attrInt == 0x0000) {
                    map.name = "temperature" [cite: 20]
                    map.value = getTemperature(descMap.value) [cite: 21]
                    map.unit = getTemperatureScale()
                } else if (attrInt == 0x0011) {
                    map.name = "coolingSetpoint" [cite: 21]
                    map.value = getTemperature(descMap.value) [cite: 21]
                    map.unit = getTemperatureScale()
                } else if (attrInt == 0x0012) {
                    map.name = "heatingSetpoint" [cite: 22]
                    map.value = getTemperature(descMap.value) [cite: 22]
                    map.unit = getTemperatureScale()
                } else if (attrInt == 0x001C) {
                    map.name = "thermostatMode" [cite: 22]
                    map.value = getModeMap()[descMap.value] ?: "off" [cite: 22]
                } else if (attrInt == 0x001E) {
                    map.name = "thermostatRunMode" [cite: 24]
                    map.value = getModeMap()[descMap.value] ?: "off" [cite: 24]
                } else if (attrInt == 0x0023) {
                    map.name = "thermostatHoldMode" [cite: 25]
                    map.value = getHoldModeMap()[descMap.value] ?: "holdOff" [cite: 25]
                } else if (attrInt == 0x0029) {
                    map.name = "thermostatOperatingState" [cite: 25]
                    map.value = getThermostatOperatingStateMap()[descMap.value] ?: "idle" [cite: 25]
                }
                break
                
            case 0x0202: // Fan Control
                if (attrInt == 0x0000) {
                    map.name = "thermostatFanMode" [cite: 23]
                    map.value = getFanModeMap()[descMap.value] ?: "fanAuto" [cite: 23]
                }
                break
                
            case 0x0001: // Power Configuration
                if (attrInt == 0x0020) {
                    map.name = "battery" [cite: 24]
                    map.value = getBatteryLevel(descMap.value) [cite: 24]
                    map.unit = "%"
                }
                break
                
            case 0x0000: // Basic
                if (attrInt == 0x0007) {
                    map.name = "powerSource" [cite: 26]
                    map.value = getPowerSource()[descMap.value] ?: "unknown" [cite: 26]
                }
                break
        }
    }
 
    if (map) {
        map.descriptionText = "${device.displayName} ${map.name} is ${map.value}${map.unit ?: ''}"
        if (txtEnable) log.info map.descriptionText
        return createEvent(map) [cite: 27]
    }
    return null
}
 
def getModeMap() { ["00":"off", "01":"auto", "03":"cool", "04":"heat", "05":"emergency heat", "06":"precooling", "07":"fan only", "08":"dry", "09":"sleep"] } [cite: 27, 28]
def modes() { ["off", "cool", "heat", "emergencyHeat"] } [cite: 28]
def getHoldModeMap() { ["00":"holdOff", "01":"holdOn"] } [cite: 28]
def getPowerSource() { ["01":"24VAC", "03":"Battery", "81":"24VAC"] } [cite: 28, 29]
def getFanModeMap() { ["00":"fanOff", "04":"fanOn", "05":"fanAuto"] } [cite: 29]
def getThermostatOperatingStateMap() { [cite: 30]
    ["0000":"idle", "0001":"heating", "0002":"cooling", "0004":"fan only", "0005":"heating", "0006":"cooling", "0008":"heating", "0009":"heating", "000A":"heating", "000D":"heating", "0010":"cooling", "0012":"cooling", "0014":"cooling", "0015":"cooling"] [cite: 30, 31]
}
 
def getTemperature(value) {
    if (value == null) return null [cite: 31]
    def celsius = Integer.parseInt(value, 16) / 100.0 [cite: 31]
    return (getTemperatureScale() == "C") ? celsius : Math.round(celsiusToFahrenheit(celsius)) [cite: 31, 32]
}
 
def toggleHoldMode() {
    String currentHoldMode = device.currentValue("thermostatHoldMode") ?: "holdOff" [cite: 32]
    return (currentHoldMode == "holdOn") ? holdOff() : holdOn() [cite: 33, 34]
}
 
def setThermostatMode(String value) {
    String normalizedValue = value.toLowerCase().replaceAll(/\s+(.)/) { match, group -> group.toUpperCase() }
    if (normalizedValue == "emergencyHeat") normalizedValue = "emergencyHeat"
    if (this.hasProperty(normalizedValue) || this.respondsTo(normalizedValue)) {
        "$normalizedValue"() [cite: 34]
    } else {
        log.error "Unsupported thermostat mode string requested: $value"
    }
}

def setThermostatFanMode(String value) { 
    if (value == "fanOn" || value == "on") { [cite: 34]
        fanOn() [cite: 34]
    } else if (value == "fanAuto" || value == "auto") { [cite: 34]
        fanAuto() [cite: 34, 36]
    } else if (value == "fanOff" || value == "off") {
        fanOff()
    } else if (value == "fanCirculate" || value == "circulate") {
        fanCirculate()
    } else {
        log.error "Unsupported fan mode target selector: $value"
    }
}

def setThermostatHoldMode(String value) { "$value"() } [cite: 34]
 
def off() {
    sendEvent(name: "thermostatMode", value: "off", descriptionText: "${device.displayName} mode is set to off") [cite: 34]
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 0) [cite: 34]
}
 
def cool() {
    sendEvent(name: "thermostatMode", value: "cool", descriptionText: "${device.displayName} mode is set to cool") [cite: 35]
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 3) [cite: 35]
}
 
def heat() {
    sendEvent(name: "thermostatMode", value: "heat", descriptionText: "${device.displayName} mode is set to heat") [cite: 35]
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 4) [cite: 35]
}
 
def emergencyHeat() {
    sendEvent(name: "thermostatMode", value: "emergencyHeat", descriptionText: "${device.displayName} mode is set to emergency heat") [cite: 35]
    zigbee.writeAttribute(0x0201, 0x1C, 0x30, 5) [cite: 35]
}
 
def on() { fanOn() } [cite: 35]
 
def fanOn() {
    if (txtEnable) log.info "${device.displayName} received 'fan On' request."
    sendEvent(name: "thermostatFanMode", value: "fanOn", descriptionText: "${device.displayName} fan mode is set to fanOn") [cite: 35]
    zigbee.writeAttribute(0x0202, 0x00, 0x30, 4) [cite: 35]
}
 
def auto() { /* Not supported */ } [cite: 35]
 
def fanAuto() {
    if (txtEnable) log.info "${device.displayName} received 'fan Auto' request."
    sendEvent(name: "thermostatFanMode", value: "fanAuto", descriptionText: "${device.displayName} fan mode is set to fanAuto") [cite: 36]
    zigbee.writeAttribute(0x0202, 0x00, 0x30, 5) [cite: 36]
}

def fanOff() {
    if (txtEnable) log.info "${device.displayName} received 'fan Off' request."
    sendEvent(name: "thermostatFanMode", value: "fanOff", descriptionText: "${device.displayName} fan mode is set to fanOff")
    zigbee.writeAttribute(0x0202, 0x00, 0x30, 0)
}

// Added method to cleanly catch system/dashboard requests for circulation
def fanCirculate() {
    if (txtEnable) log.info "${device.displayName} received 'circulate' request. Falling back to fanAuto (hardware limit)."
    fanAuto() [cite: 36]
}
 
def holdOn() {
    sendEvent(name: "thermostatHoldMode", value: "holdOn", descriptionText: "${device.displayName} hold mode is set to holdOn") [cite: 36]
    zigbee.writeAttribute(0x0201, 0x23, 0x30, 1) [cite: 36]
}
 
def holdOff() {
    sendEvent(name: "thermostatHoldMode", value: "holdOff", descriptionText: "${device.displayName} hold mode is set to holdOff") [cite: 36]
    zigbee.writeAttribute(0x0201, 0x23, 0x30, 0) [cite: 36]
}
 
private getBatteryLevel(rawValue) {
    def intValue = Integer.parseInt(rawValue, 16) [cite: 37]
    def min = 21
    def max = 30
    def pct = ((intValue - min) / (max - min) * 100) as int
    return Math.max(0, Math.min(pct, 100))
}

private isHoldOn() {
    return (device.currentValue("thermostatHoldMode") == "holdOn") [cite: 37, 38]
}
 
def setHeatingSetpoint(degrees) { [cite: 38]
    processSetpoint(degrees, 0x12) [cite: 41]
}
 
def setCoolingSetpoint(degrees) { [cite: 41]
    processSetpoint(degrees, 0x11) [cite: 44]
}

private def processSetpoint(degrees, int attributeId) {
    if (isHoldOn() || degrees == null) return [cite: 38]
    
    def isC = (getTemperatureScale() == "C")
    int maxTemp = isC ? 44 : 86 [cite: 39, 42]
    int minTemp = isC ? 7 : 30 [cite: 39, 42]
    
    int degreesInteger = Math.round(degrees).toInteger() [cite: 38, 41]
    degreesInteger = Math.max(minTemp, Math.min(degreesInteger, maxTemp))
    
    double celsius = isC ? degreesInteger : fahrenheitToCelsius(degreesInteger) [cite: 41, 44]
    int finalValue = Math.round(celsius * 100).toInteger() [cite: 41, 44]
    
    String attrName = (attributeId == 0x12) ? "heatingSetpoint" : "coolingSetpoint" [cite: 38, 41]
    sendEvent(name: attrName, value: degreesInteger, unit: getTemperatureScale(), descriptionText: "${device.displayName} ${attrName} set to ${degreesInteger}°${getTemperatureScale()}") [cite: 41, 44]
    return zigbee.writeAttribute(0x0201, attributeId, 0x29, finalValue) [cite: 41, 44]
}