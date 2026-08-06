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
 *      2026-08-01    Gemini        0.0.5       Added enable/disable dynamic capability status routing
 *      2026-08-02    Gemini        0.0.6       Cleared text attributes on disable & added disabledCapabilities attribute
 *      2026-08-02    Gemini        0.0.7       Removed set commands; added clearState and clearAttributes commands
 *
 */

static String version() { return '0.0.7' }

metadata {
    definition (name: "Virtual Omni THIE Sensors", namespace: "hubitat", author: "James Shimota") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Illuminance Measurement"
        capability "Energy Meter"

        command "disableCapability", ["String"]
        command "enableCapability", ["String"]
        command "clearState"
        command "clearAttributes"
        
        // Text attributes with formatted units
        attribute "temperatureText", "String"
        attribute "humidityText", "String"
        attribute "illuminanceText", "String"
        attribute "energyText", "String"
        attribute "variable", "String"
        
        // Capability tracking attributes
        attribute "activeCapabilities", "String"
        attribute "disabledCapabilities", "String"
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
        log."${level}" "${device.displayName}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}:${msg}"
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
        Map eventMap = [name: args.name, value: args.value, descriptionText: "Attribute ${args.name} changed to${args.value}"]
        if (args.unit) eventMap.unit = args.unit
        sendEvent(eventMap)
        logDebug "Event triggered: ${args.name} ->${args.value}"
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
        case "energy":
            sendIfChanged([name: "energy", value: null])
            sendIfChanged([name: "energyText", value: null])
            state.energyEnabled = false
            break
    }
    updateCapabilitiesAttributes()
}

def enableCapability(String type) {
    logInfo "Enabling capability stream: ${type}"
    switch(type.toLowerCase()) {
        case "temperature": state.tempEnabled = true; break
        case "humidity": state.humidEnabled = true; break
        case "illuminance": state.luxEnabled = true; break
        case "energy": state.energyEnabled = true; break
    }
    updateCapabilitiesAttributes()
}

private void updateCapabilitiesAttributes() {
    List active = []
    List disabled = []
    
    if (state.tempEnabled != false) active.add("Temperature") else disabled.add("Temperature")
    if (state.humidEnabled != false) active.add("Humidity") else disabled.add("Humidity")
    if (state.luxEnabled != false) active.add("Illuminance") else disabled.add("Illuminance")
    if (state.energyEnabled != false) active.add("Energy") else disabled.add("Energy")
    
    sendIfChanged([name: "activeCapabilities", value: active.join(", ")])
    sendIfChanged([name: "disabledCapabilities", value: disabled.join(", ")])
}

def clearState() {
    logInfo "Clearing device state variables..."
    state.clear()
    state.tempEnabled = true
    state.humidEnabled = true
    state.luxEnabled = true
    state.energyEnabled = true
    updateCapabilitiesAttributes()
}

def clearAttributes() {
    logInfo "Clearing device attributes..."
    List attrs = [
        "temperature", "temperatureText", 
        "humidity", "humidityText", 
        "illuminance", "illuminanceText", 
        "energy", "energyText", 
        "variable", "activeCapabilities", "disabledCapabilities"
    ]
    attrs.each { attr ->
        sendEvent(name: attr, value: null, isStateChange: true)
    }
}

def installed() {
    logWarn "installed..."
    state.tempEnabled = true
    state.humidEnabled = true
    state.luxEnabled = true
    state.energyEnabled = true
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