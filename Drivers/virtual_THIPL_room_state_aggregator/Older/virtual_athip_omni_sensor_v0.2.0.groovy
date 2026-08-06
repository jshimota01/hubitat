/*
 * ATHIP Room Averager Virtual Driver (Virtual ATHIP Omni Sensors)
 * This driver is a stripped down version of the original Hubitat Public version of the OMNI driver.
 * It only supports THIP - Temp, Humidity, Illuminance and Power
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
 *      2026-05-04    Gemini        0.0.1       Stripped down to THIP only
 *      2026-07-31    Gemini        0.0.2       Added custom precision preferences and text attributes
 *      2026-08-01    Gemini        0.0.3       Implemented standardized dynamic logging framework
 *      2026-08-01    Gemini        0.0.4       Added sendIfChanged optimization method for state update filtering
 *      2026-08-01    Gemini        0.0.5       Added enable/disable dynamic capability status routing
 *      2026-08-02    Gemini        0.0.6       Cleared text attributes on disable & added disabledCapabilities attribute
 *      2026-08-02    Gemini        0.0.8       Updated app name reference to ATHIP Room Averager
 *      2026-08-02    Gemini        0.0.9       Renamed driver definition to Virtual ATHIP Omni Sensors
 *      2026-08-02    Gemini        0.1.0       Restored setTemperature, setRelativeHumidity, setIlluminance, setPower
 *      2026-08-02    Gemini        0.2.0       Converted Energy capability/attributes/methods to Power throughout
 *
 */

static String version() { return '0.2.0' }

metadata {
    definition (name: "Virtual ATHIP Omni Sensors", namespace: "hubitat", author: "James Shimota") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Illuminance Measurement"
        capability "Power Meter"

        command "setTemperature", ["Number"]
        command "setRelativeHumidity", ["Number"]
        command "setIlluminance", ["Number"]
        command "setPower", ["Number"]
        command "setVariable", ["String"]

        command "disableCapability", ["String"]
        command "enableCapability", ["String"]
        
        // Text attributes with formatted units
        attribute "temperatureText", "String"
        attribute "humidityText", "String"
        attribute "illuminanceText", "String"
        attribute "powerText", "String"
        attribute "variable", "String"
        
        // Capability tracking attributes
        attribute "activeCapabilities", "String"
        attribute "disabledCapabilities", "String"
    }
    preferences {
        input name: "tempPrecision", type: "enum", title: "Temperature Precision", options: ["0": "0 decimal places", "1": "1 decimal place", "2": "2 decimal places"], defaultValue: "1"
        input name: "humidPrecision", type: "enum", title: "Humidity Precision", options: ["0": "0 decimal places", "1": "1 decimal place", "2": "2 decimal places"], defaultValue: "1"
        input name: "luxPrecision", type: "enum", title: "Illuminance Precision", options: ["0": "0 decimal places", "1": "1 decimal place", "2": "2 decimal places"], defaultValue: "0"
        input name: "powerPrecision", type: "enum", title: "Power Precision", options: ["0": "0 decimal places", "1": "1 decimal place", "2": "2 decimal places"], defaultValue: "0"
        
        input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
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
// Updated sendIfChanged to accept an optional log message or log directly
private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    
    String oldVal = device.currentValue(args.name as String)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        Map eventMap = [name: args.name, value: args.value, descriptionText: "Attribute ${args.name} changed to ${args.value}"]
        if (args.unit) eventMap.unit = args.unit
        sendEvent(eventMap)
        logDebug "Event triggered: ${args.name} -> ${args.value}"
        
        // Log primary attribute value updates at Info level
        if (["temperature", "humidity", "illuminance", "power"].contains(args.name)) {
            logInfo "${args.name.capitalize()} updated to ${args.value}${args.unit ? ' ' + args.unit : ''}"
        }
    }
}

// Dynamic Capability Management Routines
def disableCapability(String type) {
    logInfo "Disabling capability stream: ${type}"
    switch(type.toLowerCase()) {
        case "temperature":
            sendIfChanged([name: "temperature", value: null])
            sendIfChanged([name: "temperatureText", value: null])
            state.tempEnabled = false
            break
        case "humidity":
            sendIfChanged([name: "humidity", value: null])
            sendIfChanged([name: "humidityText", value: null])
            state.humidEnabled = false
            break
        case "illuminance":
            sendIfChanged([name: "illuminance", value: null])
            sendIfChanged([name: "illuminanceText", value: null])
            state.luxEnabled = false
            break
        case "power":
            sendIfChanged([name: "power", value: null])
            sendIfChanged([name: "powerText", value: null])
            state.powerEnabled = false
            break
    }
    updateCapabilitiesAttributes()
}

def enableCapability(String type) {
    boolean wasDisabled = false
    switch(type.toLowerCase()) {
        case "temperature":
            if (state.tempEnabled == false) wasDisabled = true
            state.tempEnabled = true
            break
        case "humidity":
            if (state.humidEnabled == false) wasDisabled = true
            state.humidEnabled = true
            break
        case "illuminance":
            if (state.luxEnabled == false) wasDisabled = true
            state.luxEnabled = true
            break
        case "power":
            if (state.powerEnabled == false) wasDisabled = true
            state.powerEnabled = true
            break
    }
    
    // Only log and trigger attribute events if the capability state actually changed
    if (wasDisabled) {
        logInfo "Enabling capability stream: ${type}"
        updateCapabilitiesAttributes()
    }
}

private void updateCapabilitiesAttributes() {
    List active = []
    List disabled = []
    
    if (state.tempEnabled != false) active.add("Temperature") else disabled.add("Temperature")
    if (state.humidEnabled != false) active.add("Humidity") else disabled.add("Humidity")
    if (state.luxEnabled != false) active.add("Illuminance") else disabled.add("Illuminance")
    if (state.powerEnabled != false) active.add("Power") else disabled.add("Power")
    
    sendIfChanged([name: "activeCapabilities", value: active.join(", ")])
    sendIfChanged([name: "disabledCapabilities", value: disabled.join(", ")])
}

def installed() {
    logWarn "installed..."
    state.tempEnabled = true
    state.humidEnabled = true
    state.luxEnabled = true
    state.powerEnabled = true
    updateCapabilitiesAttributes()
    if (settings.logDebugEnable != false) runIn(1800, disableDebugLogging)
}

def updated() {
    logInfo "updated..."
    updateCapabilitiesAttributes()
    if (settings.logDebugEnable != false) runIn(1800, disableDebugLogging)
}

def parse(String description) {
}

private BigDecimal formatValue(val, precisionSetting, int defaultPrec = 2) {
    if (val == null) return 0
    int decimals = precisionSetting != null ? precisionSetting.toInteger() : defaultPrec
    return new BigDecimal(val.toString()).setScale(decimals, BigDecimal.ROUND_HALF_UP)
}

def setTemperature(temp) {
    enableCapability("temperature")
    def formattedVal = formatValue(temp, settings.tempPrecision, 1)
    def unit = "°${location.temperatureScale}"
    def textVal = "${formattedVal}${unit}"
    
    sendIfChanged([name: "temperature", value: formattedVal, unit: unit])
    sendIfChanged([name: "temperatureText", value: textVal])
}

def setRelativeHumidity(humid) {
    enableCapability("humidity")
    def formattedVal = formatValue(humid, settings.humidPrecision, 1)
    def textVal = "${formattedVal}% RH"
    
    sendIfChanged([name: "humidity", value: formattedVal, unit: "%"])
    sendIfChanged([name: "humidityText", value: textVal])
}

def setIlluminance(lux) {
    enableCapability("illuminance")
    def formattedVal = formatValue(lux, settings.luxPrecision, 0)
    def textVal = "${formattedVal} lux"
    
    sendIfChanged([name: "illuminance", value: formattedVal, unit: "Lux"])
    sendIfChanged([name: "illuminanceText", value: textVal])
}

def setPower(power) {
    enableCapability("power")
    def formattedVal = formatValue(power, settings.powerPrecision, 0)
    def textVal = "${formattedVal} W"
    
    sendIfChanged([name: "power", value: formattedVal, unit: "W"])
    sendIfChanged([name: "powerText", value: textVal])
}

def setVariable(str) {
    sendIfChanged([name: "variable", value: str])
}