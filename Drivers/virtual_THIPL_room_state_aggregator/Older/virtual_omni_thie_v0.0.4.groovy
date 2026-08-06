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
 *      2026-07-31    jshimota      orig        Starting version - taken from Bruce Ravenal public owmni driver
 *      2026-05-04    Gemini        0.0.1       Stripped down to THIE only
 *      2026-07-31    Gemini        0.0.2       Added custom precision preferences and text attributes
 *      2026-08-01    Gemini        0.0.3       Implemented standardized dynamic logging framework
 *      2026-08-01    Gemini        0.0.4       Added sendIfChanged optimization method for state update filtering
 *
 */

static String version() { return '0.0.4' }

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
        
        input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
        input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
        input name: "logWarnEnable", type: "bool", title: "Enable Warning Logging", defaultValue: true
        input name: "logErrorEnable", type: "bool", title: "Enable Error Logging", defaultValue: true
        input name: "logTraceEnable", type: "bool", title: "Enable Trace Logging", defaultValue: false
    }
}

// Custom Logging Helper Methods
void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    device.updateSetting("logDebugEnable", [type: "bool", value: false])
}

private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "${device.displayName}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

// Attribute Event Filter Helper Method
private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    
    String oldVal = device.currentValue(args.name as String)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        Map eventMap = [name: args.name, value: args.value, descriptionText: "Attribute ${args.name} changed to ${args.value}"]
        if (args.unit) eventMap.unit = args.unit
        sendEvent(eventMap)
        logDebug "Event triggered: ${args.name} -> ${args.value}"
    }
}

def installed() {
    logWarn "installed..."
    setTemperature(70)
    setRelativeHumidity(35)
    setIlluminance(50)
    setEnergy(0)
    if (settings.logDebugEnable != false) runIn(1800, disableDebugLogging)
}

def updated() {
    logInfo "updated..."
    logWarn "debug logging is: ${settings.logDebugEnable == true}"
    logWarn "info logging is: ${settings.logInfoEnable == true}"
    if (settings.logDebugEnable != false) runIn(1800, disableDebugLogging)
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
    
    sendIfChanged([name: "temperature", value: formattedVal, unit: unit])
    sendIfChanged([name: "temperatureText", value: textVal])
}

def setRelativeHumidity(humid) {
    def formattedVal = formatValue(humid, settings.humidPrecision, 1)
    def textVal = "${formattedVal}% RH"
    
    sendIfChanged([name: "humidity", value: formattedVal, unit: "%"])
    sendIfChanged([name: "humidityText", value: textVal])
}

def setIlluminance(lux) {
    def formattedVal = formatValue(lux, settings.luxPrecision, 0)
    def textVal = "${formattedVal} lux"
    
    sendIfChanged([name: "illuminance", value: formattedVal, unit: "Lux"])
    sendIfChanged([name: "illuminanceText", value: textVal])
}

def setEnergy(energy) {
    def formattedVal = formatValue(energy, settings.energyPrecision, 0)
    def textVal = "${formattedVal} Watts"
    
    sendIfChanged([name: "energy", value: formattedVal, unit: "W"])
    sendIfChanged([name: "energyText", value: textVal])
}

def setVariable(str) {
    sendIfChanged([name: "variable", value: str])
}