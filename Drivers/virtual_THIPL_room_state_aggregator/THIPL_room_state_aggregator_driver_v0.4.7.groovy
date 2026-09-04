/*
 * THIPL Room State Aggregator Driver (Virtual THIPL Room State Aggregator Driver)
 * Supports THIPL - Temp, Humidity, Illuminance, Power, and Light tracking.
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
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
 *      2026-08-03    Gemini        0.3.0       Added Tier routing (Room/Floor/House), configurable deltas, debouncing, and scheduled polling
 *      2026-08-03    Gemini        0.3.1       Bug fix: Linked text attribute updates to primary numeric delta suppression & prevented Tier 1 buffer pollution
 *      2026-08-03    Gemini        0.4.0       Renamed suite to Virtual THIPL Room State Aggregator
 *      2026-08-03    Gemini        0.4.1       Added capability "Switch", setLightsOnCount, lightsOnCount, and lightsOnText attributes
 *      2026-08-03    Gemini        0.4.2       Separated light counters from tier buffering to execute immediately across all tiers
 *      2026-08-03    Gemini        0.4.3       Renamed driver definition to Virtual THIPL Room State Aggregator Driver, added real-time Power execution bypass, updated RoundingMode
 *      2026-08-04    Gemini        0.4.4       Added device count attributes and setter commands for temp, humidity, illuminance, power, and total lights
 *      2026-08-04    Gemini        0.4.5       Incremented to v0.4.5 to align with Child App upward rollup counting logic
 * 		2026-08-04    Jshimota		0.4.6		Attempt to move to Lights not switches so Alexa can detect it possibly.
 * 		2026-09-04    jshimota		0.4.7		Added explicit integer evaluation in sendIfChanged for device counts and lightsOnCount to eliminate duplicate attribute updates.
 *
 */

static String version() { return '0.4.7' }

metadata {
    definition (name: "Virtual THIPL Room State Aggregator Driver", namespace: "hubitat", author: "James Shimota") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Illuminance Measurement"
        capability "Power Meter"
		capability "Light"

        command "setTemperature", ["Number"]
        command "setRelativeHumidity", ["Number"]
        command "setIlluminance", ["Number"]
        command "setPower", ["Number"]
        command "setLightsOnCount", ["Number"]
        command "setVariable", ["String"]

        command "setTempDeviceCount", ["Number"]
        command "setHumidityDeviceCount", ["Number"]
        command "setIlluminanceDeviceCount", ["Number"]
        command "setPowerDeviceCount", ["Number"]
        command "setLightDeviceCount", ["Number"]

        command "disableCapability", ["String"]
        command "enableCapability", ["String"]
        command "refresh"
        
        // Text & Tally attributes
        attribute "temperatureText", "String"
        attribute "humidityText", "String"
        attribute "illuminanceText", "String"
        attribute "powerText", "String"
        attribute "lightsOnCount", "NUMBER"
        attribute "lightsOnText", "String"
        attribute "variable", "String"

        // Device Count attributes
        attribute "tempDeviceCount", "NUMBER"
        attribute "humidityDeviceCount", "NUMBER"
        attribute "illuminanceDeviceCount", "NUMBER"
        attribute "powerDeviceCount", "NUMBER"
        attribute "lightDeviceCount", "NUMBER"
        
        // Capability tracking attributes
        attribute "activeCapabilities", "String"
        attribute "disabledCapabilities", "String"
    }
    preferences {
        input name: "deviceTier", type: "enum", title: "Device Tier Level", options: ["1": "Room (Tier 1 - Event Driven)", "2": "Floor (Tier 2 - Debounced)", "3": "House (Tier 3 - Scheduled Poll)"], defaultValue: "1", required: true
        input name: "debounceTime", type: "enum", title: "Debounce Cooldown (Tier 2 Floor)", options: ["15": "15 Seconds", "30": "30 Seconds", "60": "60 Seconds"], defaultValue: "30"
        input name: "pollInterval", type: "enum", title: "Refresh Interval (Tier 3 House)", options: ["1": "Every 1 Minute", "5": "Every 5 Minutes", "15": "Every 15 Minutes"], defaultValue: "5"

        input name: "tempDelta", type: "decimal", title: "Min Temperature Delta (0 = disable)", defaultValue: 0.5
        input name: "humidDelta", type: "decimal", title: "Min Humidity Delta (0 = disable)", defaultValue: 1.0
        input name: "luxDelta", type: "decimal", title: "Min Illuminance Delta (0 = disable)", defaultValue: 5.0
        input name: "powerDelta", type: "decimal", title: "Min Power Delta (0 = disable)", defaultValue: 2.0

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

def on()  { sendEvent(name: "switch", value: "on") }
def off() { sendEvent(name: "switch", value: "off") }

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

private boolean sendIfChanged(Map args) {
    if (!args || !args.name) return false
    
    String attrName = args.name as String
    def newVal = args.value
    
    if (newVal == null) {
        if (device.currentValue(attrName) != null) {
            sendEvent(name: attrName, value: null)
            logDebug "Attribute ${attrName} reset to null"
            return true
        }
        return false
    }

    // Integer conversion check for light counters and device count attributes
    if (attrName.endsWith("Count") || attrName == "lightsOnCount") {
        Integer curInt = device.currentValue(attrName) as Integer
        Integer newInt = newVal != null ? newVal.toString().toInteger() : 0
        if (curInt == newInt) {
            logTrace "Skipping ${attrName} update. Value (${newInt}) matches current device state."
            return false
        }
    }

    if (["temperature", "humidity", "illuminance", "power"].contains(attrName)) {
        BigDecimal currentVal = device.currentValue(attrName) as BigDecimal
        BigDecimal targetVal = new BigDecimal(newVal.toString())
        
        if (currentVal != null) {
            BigDecimal deltaLimit = getDeltaThreshold(attrName)
            BigDecimal actualDiff = (targetVal - currentVal).abs()
            
            if (actualDiff < deltaLimit) {
                logTrace "Skipping ${attrName} update. Delta (${actualDiff}) is below minimum threshold (${deltaLimit})."
                return false
            }
        }
    }

    String oldValStr = device.currentValue(attrName)?.toString()
    String newValStr = newVal.toString()

    if (oldValStr != newValStr) {
        Map eventMap = [name: attrName, value: newVal, descriptionText: "Attribute ${attrName} changed to ${newVal}"]
        if (args.unit) eventMap.unit = args.unit
        sendEvent(eventMap)
        logDebug "Event triggered: ${attrName} -> ${newVal}"
        
        if (["temperature", "humidity", "illuminance", "power", "switch", "lightsOnCount"].contains(attrName)) {
            logInfo "${attrName.capitalize()} updated to ${newVal}${args.unit ? ' ' + args.unit : ''}"
        }
        return true
    }
    return false
}

private BigDecimal getDeltaThreshold(String attrName) {
    switch(attrName) {
        case "temperature": return settings.tempDelta != null ? new BigDecimal(settings.tempDelta.toString()) : 0.5
        case "humidity":    return settings.humidDelta != null ? new BigDecimal(settings.humidDelta.toString()) : 1.0
        case "illuminance": return settings.luxDelta != null ? new BigDecimal(settings.luxDelta.toString()) : 5.0
        case "power":       return settings.powerDelta != null ? new BigDecimal(settings.powerDelta.toString()) : 2.0
        default: return 0
    }
}

def disableCapability(String type) {
    logInfo "Disabling capability stream: ${type}"
    switch(type.toLowerCase()) {
        case "temperature":
            sendIfChanged([name: "temperature", value: null])
            sendIfChanged([name: "temperatureText", value: null])
            sendIfChanged([name: "tempDeviceCount", value: 0])
            state.tempEnabled = false
            break
        case "humidity":
            sendIfChanged([name: "humidity", value: null])
            sendIfChanged([name: "humidityText", value: null])
            sendIfChanged([name: "humidityDeviceCount", value: 0])
            state.humidEnabled = false
            break
        case "illuminance":
            sendIfChanged([name: "illuminance", value: null])
            sendIfChanged([name: "illuminanceText", value: null])
            sendIfChanged([name: "illuminanceDeviceCount", value: 0])
            state.luxEnabled = false
            break
        case "power":
            sendIfChanged([name: "power", value: null])
            sendIfChanged([name: "powerText", value: null])
            sendIfChanged([name: "powerDeviceCount", value: 0])
            state.powerEnabled = false
            break
        case "switch":
        case "lights":
            sendIfChanged([name: "switch", value: null])
            sendIfChanged([name: "lightsOnCount", value: null])
            sendIfChanged([name: "lightsOnText", value: null])
            sendIfChanged([name: "lightDeviceCount", value: 0])
            state.switchEnabled = false
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
        case "switch":
        case "lights":
            if (state.switchEnabled == false) wasDisabled = true
            state.switchEnabled = true
            break
    }
    
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
    if (state.switchEnabled != false) active.add("Switch/Lights") else disabled.add("Switch/Lights")
    
    sendIfChanged([name: "activeCapabilities", value: active.join(", ")])
    sendIfChanged([name: "disabledCapabilities", value: disabled.join(", ")])
}

def installed() {
    logWarn "installed..."
    state.tempEnabled = true
    state.humidEnabled = true
    state.luxEnabled = true
    state.powerEnabled = true
    state.switchEnabled = true
    
    setLightsOnCount(0)
    setTempDeviceCount(0)
    setHumidityDeviceCount(0)
    setIlluminanceDeviceCount(0)
    setPowerDeviceCount(0)
    setLightDeviceCount(0)
    
    updateCapabilitiesAttributes()
    configureTierScheduling()
    if (settings.logDebugEnable != false) runIn(1800, disableDebugLogging)
}

def updated() {
    logInfo "updated..."
    unschedule()
    updateCapabilitiesAttributes()
    configureTierScheduling()
    if (settings.logDebugEnable != false) runIn(1800, disableDebugLogging)
}

private void configureTierScheduling() {
    String tier = settings.deviceTier ?: "1"
    if (tier == "3") {
        int interval = settings.pollInterval ? settings.pollInterval.toInteger() : 5
        logInfo "Configuring Tier 3 (House) scheduled polling every ${interval} minute(s)."
        if (interval == 1) runEvery1Minute("processPendingBuffer")
        else if (interval == 5) runEvery5Minutes("processPendingBuffer")
        else if (interval == 15) runEvery15Minutes("processPendingBuffer")
    }
}

def parse(String description) {
}

private BigDecimal formatValue(val, precisionSetting, int defaultPrec = 2) {
    if (val == null) return 0
    int decimals = precisionSetting != null ? precisionSetting.toInteger() : defaultPrec
    return new BigDecimal(val.toString()).setScale(decimals, java.math.RoundingMode.HALF_UP)
}

private void routeValueUpdate(String capType, val) {
    if (capType == "lightsOnCount" || capType == "power" || capType.endsWith("DeviceCount")) {
        logDebug "Immediate processing for ${capType} update (bypassing tier caching)."
        processSingleCapability(capType, val)
        return
    }

    String tier = settings.deviceTier ?: "1"

    if (tier != "1") {
        if (state.pendingBuffer == null) state.pendingBuffer = [:]
        state.pendingBuffer[capType] = val
    }

    switch(tier) {
        case "1":
            processSingleCapability(capType, val)
            break
            
        case "2":
            int delaySeconds = settings.debounceTime ? settings.debounceTime.toInteger() : 30
            logDebug "Tier 2 (Floor) sensor change detected for ${capType}. Debouncing execution for ${delaySeconds}s."
            runIn(delaySeconds, "processPendingBuffer", [overwrite: true])
            break
            
        case "3":
            logTrace "Tier 3 (House) buffered ${capType} sensor update (${val}). Waiting for next scheduled poll."
            break
    }
}

void processPendingBuffer() {
    if (!state.pendingBuffer) return
    logDebug "Flushing buffered upper-tier sensor updates: ${state.pendingBuffer}"
    
    Map buffer = [:] + state.pendingBuffer
    state.pendingBuffer = [:]
    
    buffer.each { capType, val ->
        processSingleCapability(capType, val)
    }
}

void refresh() {
    logInfo "Manual refresh triggered. Processing all buffered readings immediately."
    processPendingBuffer()
}

private void processSingleCapability(String capType, val) {
    switch(capType) {
        case "temperature":
            enableCapability("temperature")
            BigDecimal formattedVal = formatValue(val, settings.tempPrecision, 1)
            String unit = "°${location.temperatureScale}"
            String textVal = "${formattedVal}${unit}"
            
            if (sendIfChanged([name: "temperature", value: formattedVal, unit: unit])) {
                sendIfChanged([name: "temperatureText", value: textVal])
            }
            break

        case "humidity":
            enableCapability("humidity")
            BigDecimal formattedVal = formatValue(val, settings.humidPrecision, 1)
            String textVal = "${formattedVal}% RH"
            
            if (sendIfChanged([name: "humidity", value: formattedVal, unit: "%"])) {
                sendIfChanged([name: "humidityText", value: textVal])
            }
            break

        case "illuminance":
            enableCapability("illuminance")
            BigDecimal formattedVal = formatValue(val, settings.luxPrecision, 0)
            String textVal = "${formattedVal} lux"
            
            if (sendIfChanged([name: "illuminance", value: formattedVal, unit: "Lux"])) {
                sendIfChanged([name: "illuminanceText", value: textVal])
            }
            break

        case "power":
            enableCapability("power")
            BigDecimal formattedVal = formatValue(val, settings.powerPrecision, 0)
            String textVal = "${formattedVal} W"
            
            if (sendIfChanged([name: "power", value: formattedVal, unit: "W"])) {
                sendIfChanged([name: "powerText", value: textVal])
            }
            break

        case "lightsOnCount":
            enableCapability("switch")
            int count = val != null ? val.toInteger() : 0
            String switchState = count > 0 ? "on" : "off"
            String textVal = count > 0 ? "${count} Light${count > 1 ? 's' : ''} On" : "All Lights Off"
            
            sendIfChanged([name: "switch", value: switchState])
            sendIfChanged([name: "lightsOnCount", value: count])
            sendIfChanged([name: "lightsOnText", value: textVal])
            break

        case "tempDeviceCount":
            sendIfChanged([name: "tempDeviceCount", value: val != null ? val.toInteger() : 0])
            break

        case "humidityDeviceCount":
            sendIfChanged([name: "humidityDeviceCount", value: val != null ? val.toInteger() : 0])
            break

        case "illuminanceDeviceCount":
            sendIfChanged([name: "illuminanceDeviceCount", value: val != null ? val.toInteger() : 0])
            break

        case "powerDeviceCount":
            sendIfChanged([name: "powerDeviceCount", value: val != null ? val.toInteger() : 0])
            break

        case "lightDeviceCount":
            sendIfChanged([name: "lightDeviceCount", value: val != null ? val.toInteger() : 0])
            break
    }
}

def setTemperature(temp)            { routeValueUpdate("temperature", temp) }
def setRelativeHumidity(humid)      { routeValueUpdate("humidity", humid) }
def setIlluminance(lux)             { routeValueUpdate("illuminance", lux) }
def setPower(power)                 { routeValueUpdate("power", power) }
def setLightsOnCount(count)         { routeValueUpdate("lightsOnCount", count) }

def setTempDeviceCount(count)       { routeValueUpdate("tempDeviceCount", count) }
def setHumidityDeviceCount(count)   { routeValueUpdate("humidityDeviceCount", count) }
def setIlluminanceDeviceCount(count){ routeValueUpdate("illuminanceDeviceCount", count) }
def setPowerDeviceCount(count)      { routeValueUpdate("powerDeviceCount", count) }
def setLightDeviceCount(count)      { routeValueUpdate("lightDeviceCount", count) }

def setVariable(str) {
    sendIfChanged([name: "variable", value: str])
}