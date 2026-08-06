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
 *      2026-07-31    Gemini        0.0.2      Added custom precision preferences and text attributes
 *
 */

static String version() { return '0.0.2' }

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
        
        // Text attributes with formatted units
        attribute "temperatureText", "String"
        attribute "humidityText", "String"
        attribute "illuminanceText", "String"
        attribute "energyText", "String"
        attribute "variable", "String"
    }
    preferences {
        input name: "tempPrecision", type: "enum", title: "Temperature Precision", options: ["0": "0 decimal places", "1": "1 decimal place", "2": "2 decimal places"], defaultValue: "1"
        input name: "humidPrecision", type: "enum", title: "Humidity Precision", options: ["0": "0 decimal places", "1": "1 decimal place", "2": "2 decimal places"], defaultValue: "1"
        input name: "luxPrecision", type: "enum", title: "Illuminance Precision", options: ["0": "0 decimal places", "1": "1 decimal place", "2": "2 decimal places"], defaultValue: "0"
        input name: "energyPrecision", type: "enum", title: "Energy Precision", options: ["0": "0 decimal places", "1": "1 decimal place", "2": "2 decimal places"], defaultValue: "0"
        
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

// Helper function to handle precision formatting with a fallback default of 2
private BigDecimal formatValue(val, precisionSetting, int defaultPrec = 2) {
    if (val == null) return 0
    int decimals = precisionSetting != null ? precisionSetting.toInteger() : defaultPrec
    return new BigDecimal(val.toString()).setScale(decimals, BigDecimal.ROUND_HALF_UP)
}

def setTemperature(temp) {
    def formattedVal = formatValue(temp, settings.tempPrecision, 1)
    def unit = "°${location.temperatureScale}"
    def textVal = "${formattedVal}${unit}"
    def descriptionText = "${device.displayName} is ${textVal}"
    
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "temperature", value: formattedVal, descriptionText: descriptionText, unit: unit)
    sendEvent(name: "temperatureText", value: textVal)
}

def setRelativeHumidity(humid) {
    def formattedVal = formatValue(humid, settings.humidPrecision, 1)
    def textVal = "${formattedVal}% RH"
    def descriptionText = "${device.displayName} is ${formattedVal}% humidity"
    
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "humidity", value: formattedVal, descriptionText: descriptionText, unit: "%")
    sendEvent(name: "humidityText", value: textVal)
}

def setIlluminance(lux) {
    def formattedVal = formatValue(lux, settings.luxPrecision, 0)
    def textVal = "${formattedVal} lux"
    def descriptionText = "${device.displayName} is ${textVal}"
    
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "illuminance", value: formattedVal, descriptionText: descriptionText, unit: "Lux")
    sendEvent(name: "illuminanceText", value: textVal)
}

def setEnergy(energy) {
    def formattedVal = formatValue(energy, settings.energyPrecision, 0)
    def textVal = "${formattedVal} Watts"
    def descriptionText = "${device.displayName} is ${textVal}"
    
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "energy", value: formattedVal, descriptionText: descriptionText, unit: "W")
    sendEvent(name: "energyText", value: textVal)
}

def setVariable(str) {
    def descriptionText = "${device.displayName} variable is ${str}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "variable", value: str, descriptionText: descriptionText)
}