/*
 * Virtual Omni THIE Sensors
 * This driver is a stripped down version of the original Hubitat Public version of the OMNI driver.
 * It only supports THIE - Temp, Humidity, Illuminance and Energy
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2026-07-31    jshimota      orig       Starting version - taken from Bruce Ravenal public owmni driver
 *      2026-05-04    Gemini        0.0.1      Stripped down to THIE only
 *
 */

static String version() { return '0.0.1' }

metadata {
    definition (name: "Virtual Omni THIE Sensors", namespace: "hubitat", author: "James Shimota") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Illuminance Measurement"
        capability "Energy Meter"

        command "setTemperature", ["Number"]
        command "setRelativeHumidity", ["Number"]
        command "setIlluminance", ["Number"]
        command "setEnergy", ["Number"]
        command "setVariable", ["String"]
        
        attribute "variable", "String"
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    log.warn "installed..."
    setTemperature(70)
    setRelativeHumidity(35)
    setIlluminance(50)
    setEnergy(0)
    runIn(1800, logsOff)
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def parse(String description) {
}

def setTemperature(temp) {
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} is ${temp}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "temperature", value: temp, descriptionText: descriptionText, unit: unit)
}

def setRelativeHumidity(humid) {
    def descriptionText = "${device.displayName} is ${humid}% humidity"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "humidity", value: humid, descriptionText: descriptionText, unit: "%")
}

def setIlluminance(lux) {
    def descriptionText = "${device.displayName} is ${lux} lux"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "illuminance", value: lux, descriptionText: descriptionText, unit: "Lux")
}

def setEnergy(energy) {
    def descriptionText = "${device.displayName} is ${energy} kWh"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "energy", value: energy, descriptionText: descriptionText, unit: "kWh")
}

def setVariable(str) {
    def descriptionText = "${device.displayName} variable is ${str}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "variable", value: str, descriptionText: descriptionText)
}