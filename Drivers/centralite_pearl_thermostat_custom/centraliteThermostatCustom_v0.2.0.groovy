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
 *      2026-05-04    Gemini        0.1.1       Initial edit, cleanup GNU, basics, remove excess comments
 *
 */

/*
 * CentraLite Pearl Thermostat Custom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

import hubitat.zigbee.zcl.DataType

static String version() { return '0.2.0' }

// Static lookups to optimize memory footprint
static final Map MODE_MAP = ["00":"off", "01":"auto", "03":"cool", "04":"heat", "05":"emergency heat", "06":"precooling", "07":"fan only", "08":"dry", "09":"sleep"].freeze()
static final Map FAN_MAP  = ["04":"fanOn", "05":"fanAuto"].freeze()
static final Map HOLD_MAP = ["00":"holdOff", "01":"holdOn"].freeze()
static final Map POWER_MAP = ["01":"24VAC", "03":"Battery", "81":"24VAC"].freeze()
static final Map OP_STATE_MAP = [
    "0000":"idle", "0001":"heating", "0002":"cooling", "0004":"fan only", "0005":"heating", 
    "0006":"cooling", "0008":"heating", "0009":"heating", "000A":"heating", "000D":"heating", 
    "0010":"cooling", "0012":"cooling", "0014":"cooling", "0015":"cooling"
].freeze()

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
        command "setThermostatHoldMode", [[name:"Hold Mode", type: "ENUM", constraints: ["holdOn", "holdOff"]]]

        attribute "thermostatHoldMode", "string"
        attribute "powerSource", "string"
                               
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0020,0201,0202,0204,0B05", outClusters: "000A, 0019"
    }
}
 
def installed() {
    log.debug "installed"
    configure()
}
 
def updated() {
    log.debug "updated"
    configure()
}
 
def configure() {
    log.debug "configure"
    // Leveraging Hubitat's built-in zigbee configuration helpers for clean execution
    def cmds = zigbee.batteryConfig() +
               zigbee.temperatureConfig(5, 300) + // 0.5°C delta
               zigbee.configureReporting(0x0201, 0x0011, DataType.INT16, 5, 300, 50) + // Cool setpoint delta 0.5°C
               zigbee.configureReporting(0x0201, 0x0012, DataType.INT16, 5, 300, 50) + // Heat setpoint delta 0.5°C
               zigbee.configureReporting(0x0201, 0x001C, DataType.ENUM8, 5, 300, 1) +  // System mode
               zigbee.configureReporting(0x0201, 0x0029, DataType.BITMAP16, 5, 300, 1) + // Running state
               zigbee.configureReporting(0x0201, 0x0023, DataType.ENUM8, 5, 300, 1) +  // Hold mode
               zigbee.configureReporting(0x0202, 0x0000, DataType.ENUM8, 5, 300, 1)    // Fan mode
    return cmds
}
 
def refresh() {
    log.debug "refresh called"
    return zigbee.readAttribute(0x0000, 0x0007) + // Power Source
           zigbee.readAttribute(0x0201, 0x0000) + // Temperature
           zigbee.readAttribute(0x0201, 0x0011) + // Cooling setpoint
           zigbee.readAttribute(0x0201, 0x0012) + // Heating setpoint
           zigbee.readAttribute(0x0201, 0x001C) + // Thermostat mode
           zigbee.readAttribute(0x0201, 0x001E) + // Run mode
           zigbee.readAttribute(0x0201, 0x0023) + // Hold mode
           zigbee.readAttribute(0x0201, 0x0029) + // Operating state
           zigbee.readAttribute(0x0001, 0x0020) + // Battery
           zigbee.readAttribute(0x0202, 0x0000)   // Fan mode
}
 
def raiseHeatLevel(){
    if (isHoldOn()) return
    def currentLevel = device.currentValue("heatingSetpoint") ?: 68
    setHeatingSetpoint((currentLevel as Integer) + 1)
}
 
def lowerHeatLevel(){
    if (isHoldOn()) return
    def currentLevel = device.currentValue("heatingSetpoint") ?: 68
    setHeatingSetpoint((currentLevel as Integer) - 1)
}
 
def raiseCoolLevel(){
    if (isHoldOn()) return
    def currentLevel = device.currentValue("coolingSetpoint") ?: 74
    setCoolingSetpoint((currentLevel as Integer) + 1)
}
 
def lowerCoolLevel(){
    if (isHoldOn()) return
    def currentLevel = device.currentValue("coolingSetpoint") ?: 74
    setCoolingSetpoint((currentLevel as Integer) - 1)
}
 
def parse(String description) {
    log.debug "Parse description $description"
    def map = [:]
 
    if (description?.startsWith("read attr -") || description?.startsWith("catchall:")) {
        def descMap = zigbee.parseDescriptionAsMap(description)
        
        if (descMap.cluster == "0201") {
            switch (descMap.attrId) {
                case "0000": // TEMPERATURE
                    map = [name: "temperature", value: getTemperature(descMap.value)]
                    break
                case "0011": // COOLING SETPOINT
                    map = [name: "coolingSetpoint", value: getTemperature(descMap.value)]
                    break
                case "0012": // HEATING SETPOINT
                    map = [name: "heatingSetpoint", value: getTemperature(descMap.value)]
                    break
                case "001C": // MODE
                    map = [name: "thermostatMode", value: MODE_MAP[descMap.value]]
                    break
                case "001E": // RUN MODE
                    map = [name: "thermostatRunMode", value: MODE_MAP[descMap.value]]
                    break
                case "0023": // HOLD MODE
                    map = [name: "thermostatHoldMode", value: HOLD_MAP[descMap.value]]
                    break
                case "0029": // OPERATING MODE
                    map = [name: "thermostatOperatingState", value: OP_STATE_MAP[descMap.value]]
                    break
            }
        } else if (descMap.cluster == "0202" && descMap.attrId == "0000") {
            map = [name: "thermostatFanMode", value: FAN_MAP[descMap.value]]
        } else if (descMap.cluster == "0001" && descMap.attrId == "0020") {
            map = [name: "battery", value: getBatteryLevel(descMap.value)]
        } else if (descMap.cluster == "0000" && descMap.attrId == "0007") {
            map = [name: "powerSource", value: POWER_MAP[descMap.value]]
        }
    }
 
    if (map) {
        log.debug "Parse returned $map"
        return createEvent(map)
    }
    return null
}
 
def modes() {
    ["off", "cool", "heat", "emergency heat"]
}
 
def getTemperature(value) {
    if (value != null) {
        def celsius = Integer.parseInt(value, 16) / 100
        if (getTemperatureScale() == "C") {
            return celsius
        } else {
            return Math.round(celsiusToFahrenheit(celsius)) as Integer
        }
    }
    return null
}
 
def setThermostatHoldMode(String value) {
    log.debug "setThermostatHoldMode(${value})"
    if (value == "holdOn") {
        sendEvent(name: "thermostatHoldMode", value: "holdOn")
        return zigbee.writeAttribute(0x0201, 0x23, DataType.ENUM8, 1)
    } else {
        sendEvent(name: "thermostatHoldMode", value: "holdOff")
        return zigbee.writeAttribute(0x0201, 0x23, DataType.ENUM8, 0)
    }
}
 
def setThermostatMode(String value) {
    log.debug "setThermostatMode to ${value}"
    if (value in modes()) {
        sendEvent(name: "thermostatMode", value: value)
        int modePayload = (value == "off") ? 0 : (value == "cool") ? 3 : (value == "heat") ? 4 : 5
        return zigbee.writeAttribute(0x0201, 0x1C, DataType.ENUM8, modePayload)
    }
}
 
def setThermostatFanMode(String value) {
    log.debug "setThermostatFanMode(${value})"
    if (value == "fanOn") {
        sendEvent(name: "thermostatFanMode", value: "fanOn")
        return zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 4)
    } else if (value == "fanAuto") {
        sendEvent(name: "thermostatFanMode", value: "fanAuto")
        return zigbee.writeAttribute(0x0202, 0x00, DataType.ENUM8, 5)
    }
}
 
def off() { setThermostatMode("off") }
def cool() { setThermostatMode("cool") }
def heat() { setThermostatMode("heat") }
def emergencyHeat() { setThermostatMode("emergency heat") }
def fanOn() { setThermostatFanMode("fanOn") }
def fanAuto() { setThermostatFanMode("fanAuto") }

private isHoldOn() {
    return (device.currentValue("thermostatHoldMode") == "holdOn")
}
 
def setHeatingSetpoint(degrees) {
    if (isHoldOn() || degrees == null) return
    log.debug "setHeatingSetpoint to $degrees"
    return zigbee.setHeatingSetpoint(degrees)
}
 
def setCoolingSetpoint(degrees) {
    if (isHoldOn() || degrees == null) return
    log.debug "setCoolingSetpoint to $degrees"
    return zigbee.setCoolingSetpoint(degrees)
}
 
private getBatteryLevel(rawValue) {
    def intValue = Integer.parseInt(rawValue, 16)
    def vBatt = intValue / 10
    int pct = ((vBatt - 2.1) / (3.0 - 2.1) * 100) as int
    return Math.max(0, Math.min(100, pct)) // bound between 0-100%
}