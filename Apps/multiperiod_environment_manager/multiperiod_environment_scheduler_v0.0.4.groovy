/**
 *  Thermostat Multi-Period Scheduler with Dynamic Fan Circulate
 *  Schedules 6 heating/cooling setpoint windows with mode overrides, live device tracking, and dynamic fan/Filtered Air circulation
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2026-06-01    jshimota      0.0.1       Initial release 
 *      2026-08-01    Gemini        0.0.4       Added Air Filter AQI control
 */

static String version() { return '0.0.4' }

definition(
    name: "Multiperiod Environment Scheduler",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Schedules 6 heating/cooling setpoint windows with mode overrides, live device tracking, and dynamic fan/Filtered Air circulation.",
    category: "Convenience, HVAC, Thermostat, Fan",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Multiperiod Environment Scheduler Setup", install: true, uninstall: true) {
        
        section("<b>Control & Target Thermostat Device</b>") {
            input name: "appEnabled", type: "bool", title: "Enable Automated Setpoint Scheduling", defaultValue: true, submitOnChange: true
            input name: "targetThermostat", type: "capability.thermostat", title: "Select Thermostat / Thermostat Controller", required: true, multiple: false, submitOnChange: true
        }

        section("<b>Mode-Based Overrides</b>") {
            paragraph "When the hub enters these modes, these setpoints will override time-based schedules."
            input name: "awayMode", type: "mode", title: "Hub Mode for 'Away'", required: false
            input name: "awayHeat", type: "number", title: "Away Heat Setpoint", required: true, defaultValue: 62
            input name: "awayCool", type: "number", title: "Away Cool Setpoint", required: true, defaultValue: 78
            
            input name: "sleepMode", type: "mode", title: "Hub Mode for 'Sleeping'", required: false
            input name: "sleepHeat", type: "number", title: "Sleeping Heat Setpoint", required: true, defaultValue: 65
            input name: "sleepCool", type: "number", title: "Sleeping Cool Setpoint", required: true, defaultValue: 74
        }

        section("<b>Time-Based Schedules</b>") {
            paragraph "Configure start times, end times, and setpoints for normal daily windows."
            
            // Morning
            input name: "morningStart", type: "time", title: "Morning Start Time", required: true, defaultValue: "06:00"
            input name: "morningEnd", type: "time", title: "Morning End Time", required: true, defaultValue: "09:00"
            input name: "morningHeat", type: "number", title: "Morning Heat Setpoint", required: true, defaultValue: 68
            input name: "morningCool", type: "number", title: "Morning Cool Setpoint", required: true, defaultValue: 72

            // Day
            input name: "dayStart", type: "time", title: "Day Start Time", required: true, defaultValue: "09:00"
            input name: "dayEnd", type: "time", title: "Day End Time", required: true, defaultValue: "17:00"
            input name: "dayHeat", type: "number", title: "Day Heat Setpoint", required: true, defaultValue: 66
            input name: "dayCool", type: "number", title: "Day Cool Setpoint", required: true, defaultValue: 75

            // Evening
            input name: "eveningStart", type: "time", title: "Evening Start Time", required: true, defaultValue: "17:00"
            input name: "eveningEnd", type: "time", title: "Evening End Time", required: true, defaultValue: "22:00"
            input name: "eveningHeat", type: "number", title: "Evening Heat Setpoint", required: true, defaultValue: 69
            input name: "eveningCool", type: "number", title: "Evening Cool Setpoint", required: true, defaultValue: 73

            // Night
            input name: "nightStart", type: "time", title: "Night Start Time", required: true, defaultValue: "22:00"
            input name: "nightEnd", type: "time", title: "Night End Time", required: true, defaultValue: "06:00"
            input name: "nightHeat", type: "number", title: "Night Heat Setpoint", required: true, defaultValue: 64
            input name: "nightCool", type: "number", title: "Night Cool Setpoint", required: true, defaultValue: 75
        }

        section("<b>Simulated Fan Circulation Control</b>") {
            paragraph "Cycles the thermostat fan between ON and AUTO to simulate dynamic air circulation."
            input name: "fanCirculateEnabled", type: "bool", title: "Enable Fan Circulation Loop", defaultValue: false, submitOnChange: true
            if (fanCirculateEnabled) {
                input name: "fanOnMinutes", type: "number", title: "Fan ON Duration (Minutes)", required: true, defaultValue: 30
                input name: "fanOffMinutes", type: "number", title: "Fan OFF/Auto Duration (Minutes)", required: true, defaultValue: 30
            }
        }

        section("<b>Air Quality Control</b>") {
            paragraph "Averages two air quality sensors to trigger a stoplight RGB indicator and an Air Filter device."
            input name: "aqiEnabled", type: "bool", title: "Enable Air Quality Automation", defaultValue: false, submitOnChange: true
            if (aqiEnabled) {
                input name: "aqiSensor1", type: "capability.airQualityIndex", title: "Select Primary AQI Sensor", required: true, multiple: false, submitOnChange: true
                input name: "aqiSensor2", type: "capability.airQualityIndex", title: "Select Secondary AQI Sensor", required: true, multiple: false, submitOnChange: true
                input name: "rgbLight", type: "capability.colorControl", title: "Select Indicator RGB Light", required: false, multiple: false
                input name: "airFilterSwitch", type: "capability.switch", title: "Select Air Filter Switch/Device", required: false, multiple: false
            }
        }
        // Live Device Status Evaluation
        def devHeat = targetThermostat ? targetThermostat.currentValue("heatingSetpoint") : "--"
        def devCool = targetThermostat ? targetThermostat.currentValue("coolingSetpoint") : "--"
        def devThermostatMode = targetThermostat ? targetThermostat.currentValue("thermostatMode") : "--"
        def devFanMode = targetThermostat ? targetThermostat.currentValue("thermostatFanMode") : "--"
        def devOpState = targetThermostat ? targetThermostat.currentValue("thermostatOperatingState") : "--"

        section("<b>Current Status</b>") {
            paragraph "<b>Connected Thermostat Device:</b> ${targetThermostat ? targetThermostat.displayName : 'None Selected'}"
            paragraph "<b>Device Live Status:</b> Heat: ${devHeat}° | Cool: ${devCool}° | Mode: ${devThermostatMode} | Fan Mode: ${devFanMode} | State: ${devOpState}"
            paragraph "--------------------------------------------------"
            paragraph "<b>Active Schedule Period:</b> ${state.activeSet ?: 'Not Evaluated'}"
            paragraph "<b>Target Setpoints:</b> Heat: ${state.currentHeat ?: '--'}° | Cool: ${state.currentCool ?: '--'}°"
            paragraph "<b>Fan Circulation Status:</b> ${fanCirculateEnabled ? (state.fanPhase ?: 'Initializing') : 'Disabled'}"
            paragraph "--------------------------------------------------"
            if (aqiEnabled) {
                paragraph "<b>AQI Sensor 1 (${aqiSensor1?.displayName ?: 'None'}):</b> ${state.aqi1Val ?: '--'}"
                paragraph "<b>AQI Sensor 2 (${aqiSensor2?.displayName ?: 'None'}):</b> ${state.aqi2Val ?: '--'}"
                paragraph "<b>Average AQI:</b> ${state.avgAqi ?: '--'} (${state.aqiStatus ?: 'Not Evaluated'})"
                paragraph "<b>Air Filter Status:</b> ${airFilterSwitch ? airFilterSwitch.currentValue('switch') : 'No Switch Selected'}"
            } else {
                paragraph "<b>Air Quality Automation:</b> Disabled"
            } 
            paragraph "<b>Last Scheduled Evaluation:</b> ${state.lastEvaluated ?: 'Never'}"
        }

        section("<b>App Name</b>") {
            label title: "Assign a name for this app instance", required: false
        }
    }
}

def installed() {
    log.info "Multiperiod Environment Scheduler installed."
    initialize()
}

def updated() {
    log.info "Multiperiod Environment Scheduler updated settings."
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    // Subscribe to Hubitat Mode Changes
    subscribe(location, "mode", modeChangeHandler)
    
    // Subscribe to target thermostat event changes
    if (targetThermostat) {
        subscribe(targetThermostat, "heatingSetpoint", thermostatSetpointHandler)
        subscribe(targetThermostat, "coolingSetpoint", thermostatSetpointHandler)
    }
    
    // Subscribe to Air Quality Sensors
    if (aqiEnabled) {
        if (aqiSensor1) subscribe(aqiSensor1, "airQualityIndex", aqiHandler)
        if (aqiSensor2) subscribe(aqiSensor2, "airQualityIndex", aqiHandler)
        evaluateAirQuality()
    }
    // Schedule periodic checks every 5 minutes for time boundary safety
    schedule("0 */5 * ? * *", evaluateSchedule)
    
    // Evaluate setpoints immediately
    evaluateSchedule()

    // Initialize or stop Fan Circulation Engine
    if (fanCirculateEnabled) {
        manageFanCirculation(true)
    } else {
        state.fanPhase = "Disabled"
    }
}

def modeChangeHandler(evt) {
    log.info "Hub Mode changed to: ${evt.value}. Re-evaluating schedule."
    evaluateSchedule()
}

def thermostatSetpointHandler(evt) {
    log.info "Thermostat setpoint updated externally: ${evt.name} = ${evt.value}°"
    evaluateSchedule()
}

def evaluateSchedule() {
    if (!appEnabled) {
        log.info "Multiperiod Environment is currently DISABLED via main switch."
        state.activeSet = "Disabled (App OFF)"
        return
    }

    String currentMode = location.mode
    Date now = new Date()

    String activePeriod = ""
    Integer targetHeat = null
    Integer targetCool = null

    // 1. Check Mode Overrides First
    if (awayMode && currentMode == awayMode) {
        activePeriod = "Away"
        targetHeat = awayHeat
        targetCool = awayCool
    } else if (sleepMode && currentMode == sleepMode) {
        activePeriod = "Sleeping"
        targetHeat = sleepHeat
        targetCool = sleepCool
    } else {
        // 2. Evaluate Time Windows
        if (isTimeBetween(morningStart, morningEnd, now)) {
            activePeriod = "Morning"
            targetHeat = morningHeat
            targetCool = morningCool
        } else if (isTimeBetween(dayStart, dayEnd, now)) {
            activePeriod = "Day"
            targetHeat = dayHeat
            targetCool = dayCool
        } else if (isTimeBetween(eveningStart, eveningEnd, now)) {
            activePeriod = "Evening"
            targetHeat = eveningHeat
            targetCool = eveningCool
        } else if (isTimeBetween(nightStart, nightEnd, now)) {
            activePeriod = "Night"
            targetHeat = nightHeat
            targetCool = nightCool
        } else {
            activePeriod = "Default / Unassigned Window"
        }
    }

    // Update state tracking
    state.activeSet = activePeriod
    state.currentHeat = targetHeat
    state.currentCool = targetCool
    state.lastEvaluated = now.format("yyyy-MM-dd HH:mm:ss", location.timeZone)

    // Apply setpoints
    if (targetHeat != null && targetCool != null && targetThermostat) {
        applySetpoints(targetHeat, targetCool)
    } else {
        log.warn "Could not apply setpoints. Active Period: ${activePeriod}, Heat: ${targetHeat}, Cool: ${targetCool}"
    }
}

private void applySetpoints(Integer heat, Integer cool) {
    def currentHeatSp = targetThermostat.currentValue("heatingSetpoint")
    def currentCoolSp = targetThermostat.currentValue("coolingSetpoint")

    if (currentHeatSp != heat) {
        log.info "Setting ${targetThermostat.displayName} Heating Setpoint to ${heat}° (Active Set: ${state.activeSet})"
        targetThermostat.setHeatingSetpoint(heat)
    }

    if (currentCoolSp != cool) {
        log.info "Setting ${targetThermostat.displayName} Cooling Setpoint to ${cool}° (Active Set: ${state.activeSet})"
        targetThermostat.setCoolingSetpoint(cool)
    }
}

// --- Fan Circulation Simulation Logic ---

def manageFanCirculation(Boolean isInitial = false) {
    if (!fanCirculateEnabled || !targetThermostat) return

    Integer onTime = (fanOnMinutes ?: 30) * 60
    Integer offTime = (fanOffMinutes ?: 30) * 60

    if (isInitial || state.fanPhase == "OFF Phase" || !state.fanPhase) {
        // Start ON Phase
        state.fanPhase = "ON Phase"
        if (targetThermostat.currentValue("thermostatFanMode") != "on") {
            log.info "Fan Circulation Loop: Turning fan ON for ${fanOnMinutes ?: 30} minutes."
            targetThermostat.fanOn()
        }
        runIn(onTime, "toggleFanCirculation", [data: [nextPhase: "OFF"]])
    } else {
        // Start OFF Phase
        state.fanPhase = "OFF Phase"
        
        // Safety Guard: Don't override fan if heating or cooling is active
        String opState = targetThermostat.currentValue("thermostatOperatingState")
        if (opState != "heating" && opState != "cooling") {
            if (targetThermostat.currentValue("thermostatFanMode") != "auto") {
                log.info "Fan Circulation Loop: Setting fan to AUTO for ${fanOffMinutes ?: 30} minutes."
                targetThermostat.fanAuto()
            }
        } else {
            log.info "Fan Circulation Loop: HVAC actively ${opState}. Leaving fan mode undisturbed."
        }
        runIn(offTime, "toggleFanCirculation", [data: [nextPhase: "ON"]])
    }
}

def toggleFanCirculation(data) {
    if (!fanCirculateEnabled) return
    log.info "Fan Circulation Timer Fired. Transitioning to ${data?.nextPhase ?: 'next'} phase."
    manageFanCirculation(false)
}

// --- Air Quality Automation Logic ---
									 

def aqiHandler(evt) {
    log.info "Air Quality Sensor Event (${evt.device.displayName}): ${evt.value}"
    evaluateAirQuality()
																  
	 
}

def evaluateAirQuality() {
    if (!aqiEnabled || !aqiSensor1 || !aqiSensor2) return

    def val1 = aqiSensor1.currentValue("airQualityIndex")
    def val2 = aqiSensor2.currentValue("airQualityIndex")

    if (val1 == null || val2 == null) {
        log.warn "Could not evaluate Air Quality. Sensor 1: ${val1}, Sensor 2: ${val2}"
        return
						   
																 
    }

    Integer v1 = val1 as Integer
    Integer v2 = val2 as Integer
    Integer avgAqi = Math.round((v1 + v2) / 2.0) as Integer

    state.aqi1Val = v1
    state.aqi2Val = v2
    state.avgAqi = avgAqi

    Map colorMap = [:]
    Boolean filterOn = false

    if (avgAqi < 51) {
        // GREEN - Good
        state.aqiStatus = "Green (Good)"
        colorMap = [hue: 33, saturation: 100, level: 100]
        filterOn = false
    } else if (avgAqi >= 51 && avgAqi <= 99) {
        // YELLOW - Moderate
        state.aqiStatus = "Yellow (Moderate)"
        colorMap = [hue: 16, saturation: 100, level: 100]
        filterOn = true
    } else {
        // RED - Unhealthy
        state.aqiStatus = "Red (Unhealthy)"
        colorMap = [hue: 0, saturation: 100, level: 100]
        filterOn = true
    }

    log.info "AQI Evaluated: Avg ${avgAqi} -> Status: ${state.aqiStatus} | Filter Action: ${filterOn ? 'ON' : 'OFF'}"

    // Apply RGB Light Color
    if (rgbLight) {
        if (rgbLight.currentValue("switch") != "on") {
            rgbLight.on()
        }
        rgbLight.setColor(colorMap)
    }

    // Apply Air Filter State
    if (airFilterSwitch) {
        String currentFilterState = airFilterSwitch.currentValue("switch")
        if (filterOn && currentFilterState != "on") {
            log.info "Turning Air Filter ON due to elevated AQI (${avgAqi})"
            airFilterSwitch.on()
        } else if (!filterOn && currentFilterState != "off") {
            log.info "Turning Air Filter OFF (AQI Good: ${avgAqi})"
            airFilterSwitch.off()
        }
    }
}


private Boolean isTimeBetween(String startTimeStr, String endTimeStr, Date currentTime) {
    if (!startTimeStr || !endTimeStr) return false
    Date start = toDateTime(startTimeStr)
    Date end = toDateTime(endTimeStr)
    if (end.before(start)) {
        return currentTime.after(start) || currentTime.before(end)
    } else {
        return currentTime.after(start) && currentTime.before(end)
    }
}

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

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
}

private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "${app.label ?: 'THIPL Room State Aggregator Child'}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

def installed() {
    logInfo "Installed app instance..."
    initialize()
}

def updated() {
    logInfo "Updated app instance settings..."
    unsubscribe()
    initialize()
}