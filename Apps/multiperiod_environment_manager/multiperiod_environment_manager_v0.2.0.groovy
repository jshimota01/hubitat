/**
 *  Multiperiod Environment Manager
 *  Schedules 6 heating/cooling setpoint windows with switch overrides, live device tracking, dynamic fan circulation, AQI Air Quality monitoring, independent Air Filter control, and dashboard tile output.
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2026-06-01    jshimota      0.0.1       Initial release[cite: 1]
 *      2026-08-01    Gemini        0.0.4       Added Air Filter AQI control[cite: 1]
 *      2026-08-05    jshimota      0.0.5       Implemented unified logging, auto-debug disable, and sendIfChanged routine[cite: 1]
 *      2026-08-05    jshimota      0.0.6       Renamed app to Multiperiod Environment Manager[cite: 1]
 *      2026-08-05    Gemini        0.0.7       Bug check fix: quoted subscribe/schedule handlers, fixed boundary condition in isTimeBetween[cite: 1]
 *      2026-08-05    jshimota      0.0.8       Added child tile device support and HTML memTile attribute output[cite: 1]
 *      2026-08-05    Gemini        0.0.9       Replaced Mode overrides with Switch devices, split Air Filter into separate section[cite: 1]
 *      2026-08-05    Gemini        0.1.0       Made Air Filter independently toggleable[cite: 1]
 *      2026-08-05    Gemini        0.1.1       Updated AQI sensor pick list to Hubitat capability.airQuality[cite: 1]
 *      2026-08-05    jshimota      0.1.2       Execute immediate AQI evaluation and average upon pressing Done (installed/updated)[cite: 1]
 *      2026-08-05    jshimota      0.1.3       Renamed section to Room Air Filter Control, default ON, added numerical AQI threshold table[cite: 1]
 *      2026-08-05    jshimota      0.1.4       Added AQI thresholds and color table to Air Quality Monitoring section UI[cite: 1]
 *      2026-08-05    jshimota      0.1.5       Immediate Air Filter switch status evaluation on app save/initialize[cite: 1]
 *      2026-08-05    Gemini        0.1.6       Fixed AQI evaluation null guard on initialize, forced direct switch state refresh[cite: 1]
 *      2026-08-05    Gemini        0.1.8       Made AQI range thresholds fully configurable via app settings[cite: 1]
 *      2026-08-05    Gemini        0.1.9       Execute dynamic AQI evaluation directly inside mainPage UI render pass[cite: 1]
 *      2026-08-05    Gemini        0.2.0       Fixed UnknownDeviceTypeException by switching child device type to 'Virtual Variable'
 */

static String version() { return '0.2.0' }

definition(
    name: "Multiperiod Environment Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Schedules 6 heating/cooling setpoint windows with switch overrides, live device tracking, dynamic fan circulation, independent AQI filtering, and dashboard tile output.",
    category: "Convenience, HVAC, Thermostat, Fan",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Multiperiod Environment Manager Setup v${version()}", install: true, uninstall: true) {
        
        section("<b>Control & Target Thermostat Device</b>") {
            input name: "appEnabled", type: "bool", title: "Enable Automated Setpoint Scheduling", defaultValue: true, submitOnChange: true
            input name: "targetThermostat", type: "capability.thermostat", title: "Select Thermostat / Thermostat Controller", required: true, multiple: false, submitOnChange: true
        }

        section("<b>Switch-Based Overrides</b>") {
            paragraph "When these switches are ON, these setpoints will override time-based schedules."
            input name: "awaySwitch", type: "capability.switch", title: "Select 'Away' Switch", required: false, multiple: false, submitOnChange: true
            input name: "awayHeat", type: "number", title: "Away Heat Setpoint", required: true, defaultValue: 62
            input name: "awayCool", type: "number", title: "Away Cool Setpoint", required: true, defaultValue: 78
            
            input name: "sleepSwitch", type: "capability.switch", title: "Select 'Sleeping' Switch", required: false, multiple: false, submitOnChange: true
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

        section("<b>Air Quality Monitoring & RGB Indicator</b>") {
            paragraph "Monitors two airQuality sensors and averages their airQualityIndex attributes. IF RGB light is selected, will represent AQI with:"
            
            Integer goodMax = (aqiGoodMax != null) ? (aqiGoodMax as Integer) : 50
            Integer modMax = (aqiModMax != null) ? (aqiModMax as Integer) : 99

            // AQI Color Threshold Table Display
            String aqiColorTableHtml = "<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>" +
                                       "<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>" +
                                       "<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Status</th><th style='padding:4px;'>Indicator Color</th>" +
                                       "</tr>" +
                                       "<tr style='border-bottom:1px solid #eee;'>" +
                                       "<td style='padding:4px;'>0 - ${goodMax}</td><td style='padding:4px; color:green;'><b>Good</b></td><td style='padding:4px;'>Green</td>" +
                                       "</tr>" +
                                       "<tr style='border-bottom:1px solid #eee;'>" +
                                       "<td style='padding:4px;'>${goodMax + 1} - ${modMax}</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>Yellow</td>" +
                                       "</tr>" +
                                       "<tr>" +
                                       "<td style='padding:4px;'>&ge; ${modMax + 1}</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>Red</td>" +
                                       "</tr>" +
                                       "</table>"
            paragraph aqiColorTableHtml

            input name: "aqiEnabled", type: "bool", title: "Enable Air Quality Monitoring", defaultValue: false, submitOnChange: true
            if (aqiEnabled) {
                input name: "aqiSensor1", type: "capability.airQuality", title: "Select Primary AQI Sensor", required: true, multiple: false, submitOnChange: true
                input name: "aqiSensor2", type: "capability.airQuality", title: "Select Secondary AQI Sensor", required: true, multiple: false, submitOnChange: true
                input name: "rgbLight", type: "capability.colorControl", title: "Select Indicator RGB Light", required: false, multiple: false
                
                input name: "aqiGoodMax", type: "number", title: "Good AQI Max Threshold", required: true, defaultValue: 50, submitOnChange: true
                input name: "aqiModMax", type: "number", title: "Moderate AQI Max Threshold", required: true, defaultValue: 99, submitOnChange: true
            }
        }

        section("<b>Room Air Filter Control</b>") {
            paragraph "Independently turns on an Air Filter device based on calculated AQI thresholds."
            
            Integer goodMax = (aqiGoodMax != null) ? (aqiGoodMax as Integer) : 50
            Integer modMax = (aqiModMax != null) ? (aqiModMax as Integer) : 99

            // Numerical AQI Threshold Table Display
            String aqiTableHtml = "<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>" +
                                  "<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>" +
                                  "<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Status</th><th style='padding:4px;'>Indicator Color</th><th style='padding:4px;'>Air Filter State</th>" +
                                  "</tr>" +
                                  "<tr style='border-bottom:1px solid #eee;'>" +
                                  "<td style='padding:4px;'>0 - ${goodMax}</td><td style='padding:4px; color:green;'><b>Good</b></td><td style='padding:4px;'>Green</td><td style='padding:4px;'>OFF</td>" +
                                  "</tr>" +
                                  "<tr style='border-bottom:1px solid #eee;'>" +
                                  "<td style='padding:4px;'>${goodMax + 1} - ${modMax}</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>Yellow</td><td style='padding:4px; color:green;'><b>ON</b></td>" +
                                  "</tr>" +
                                  "<tr>" +
                                  "<td style='padding:4px;'>&ge; ${modMax + 1}</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>Red</td><td style='padding:4px; color:green;'><b>ON</b></td>" +
                                  "</tr>" +
                                  "</table>"
            paragraph aqiTableHtml

            input name: "airFilterEnabled", type: "bool", title: "Enable Automated Air Filter Control", defaultValue: true, submitOnChange: true
            if (airFilterEnabled) {
                input name: "airFilterSwitch", type: "capability.switch", title: "Select Air Filter Switch/Device", required: true, multiple: false, submitOnChange: true
            }
        }

        // Run direct inline UI pass for live state display
        if (aqiEnabled && (aqiSensor1 || aqiSensor2)) {
            evaluateAirQuality()
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
            } else {
                paragraph "<b>Air Quality Monitoring:</b> Disabled"
            }
            if (airFilterEnabled) {
                paragraph "<b>Air Filter Status:</b> ${airFilterSwitch ? airFilterSwitch.currentValue('switch') : 'No Switch Selected'}"
            } else {
                paragraph "<b>Air Filter Automation:</b> Disabled"
            }
            paragraph "<b>Last Scheduled Evaluation:</b> ${state.lastEvaluated ?: 'Never'}"
        }

        section("<b>Logging Options</b>") {
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging (Auto-disables after 30 mins)", defaultValue: false, submitOnChange: true
            input name: "logTraceEnable", type: "bool", title: "Enable Trace Logging", defaultValue: false
            input name: "logWarnEnable", type: "bool", title: "Enable Warning Logging", defaultValue: true
            input name: "logErrorEnable", type: "bool", title: "Enable Error Logging", defaultValue: true
        }

        section("<b>App Name</b>") {
            label title: "Assign a name for this app instance", required: false
        }
    }
}

def installed() {
    logInfo "Installed app instance v${version()}..."
    initialize()
}

def updated() {
    logInfo "Updated app instance v${version()} settings..."
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    // Manage dynamic debug logging schedule
    if (logDebugEnable) {
        logDebug "Debug logging enabled. Scheduling auto-disable in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }

    // Ensure child tile device exists
    ensureChildDevice()

    // Subscribe to Override Switches
    if (awaySwitch) {
        subscribe(awaySwitch, "switch", "overrideSwitchHandler")
    }
    if (sleepSwitch) {
        subscribe(sleepSwitch, "switch", "overrideSwitchHandler")
    }
    
    // Subscribe to target thermostat event changes
    if (targetThermostat) {
        subscribe(targetThermostat, "heatingSetpoint", "thermostatSetpointHandler")
        subscribe(targetThermostat, "coolingSetpoint", "thermostatSetpointHandler")
    }
    
    // Subscribe to Air Filter Switch changes
    if (airFilterEnabled && airFilterSwitch) {
        subscribe(airFilterSwitch, "switch", "airFilterSwitchHandler")
    }
    
    // Subscribe to Air Quality Sensors specifically on the airQualityIndex attribute
    if (aqiEnabled) {
        if (aqiSensor1) subscribe(aqiSensor1, "airQualityIndex", "aqiHandler")
        if (aqiSensor2) subscribe(aqiSensor2, "airQualityIndex", "aqiHandler")
    }

    // Schedule periodic checks every 5 minutes for time boundary safety
    schedule("0 */5 * ? * *", "evaluateSchedule")
    
    // Evaluate AQI, Setpoints, and Tile status immediately
    if (aqiEnabled) {
        logInfo "Performing immediate Air Quality evaluation upon initialize."
        evaluateAirQuality()
    }
    
    evaluateSchedule()

    // Initialize or stop Fan Circulation Engine
    if (fanCirculateEnabled) {
        manageFanCirculation(true)
    } else {
        state.fanPhase = "Disabled"
    }
}

def overrideSwitchHandler(evt) {
    logInfo "Override Switch Changed (${evt.device.displayName}): ${evt.value}. Re-evaluating schedule."
    evaluateSchedule()
}

def thermostatSetpointHandler(evt) {
    logInfo "Thermostat setpoint updated externally: ${evt.name} = ${evt.value}°"
    evaluateSchedule()
}

def airFilterSwitchHandler(evt) {
    logInfo "Air Filter Switch state changed externally: ${evt.value}"
    updateMemTile()
}

def evaluateSchedule() {
    if (!appEnabled) {
        logInfo "Multiperiod Environment Manager is currently DISABLED via main switch."
        state.activeSet = "Disabled (App OFF)"
        updateMemTile()
        return
    }

    Date now = new Date()

    String activePeriod = ""
    Integer targetHeat = null
    Integer targetCool = null

    // 1. Check Switch Overrides First
    Boolean isAway = awaySwitch ? (awaySwitch.currentValue("switch") == "on") : false
    Boolean isSleeping = sleepSwitch ? (sleepSwitch.currentValue("switch") == "on") : false

    if (isAway) {
        activePeriod = "Away"
        targetHeat = awayHeat as Integer
        targetCool = awayCool as Integer
    } else if (isSleeping) {
        activePeriod = "Sleeping"
        targetHeat = sleepHeat as Integer
        targetCool = sleepCool as Integer
    } else {
        // 2. Evaluate Time Windows
        if (isTimeBetween(morningStart, morningEnd, now)) {
            activePeriod = "Morning"
            targetHeat = morningHeat as Integer
            targetCool = morningCool as Integer
        } else if (isTimeBetween(dayStart, dayEnd, now)) {
            activePeriod = "Day"
            targetHeat = dayHeat as Integer
            targetCool = dayCool as Integer
        } else if (isTimeBetween(eveningStart, eveningEnd, now)) {
            activePeriod = "Evening"
            targetHeat = eveningHeat as Integer
            targetCool = eveningCool as Integer
        } else if (isTimeBetween(nightStart, nightEnd, now)) {
            activePeriod = "Night"
            targetHeat = nightHeat as Integer
            targetCool = nightCool as Integer
        } else {
            activePeriod = "Default / Unassigned Window"
        }
    }

    // Update state tracking via sendIfChanged logic
    sendIfChanged([name: "activeSet", value: activePeriod])
    sendIfChanged([name: "currentHeat", value: targetHeat])
    sendIfChanged([name: "currentCool", value: targetCool])
    state.lastEvaluated = now.format("yyyy-MM-dd HH:mm:ss", location.timeZone)

    // Apply setpoints
    if (targetHeat != null && targetCool != null && targetThermostat) {
        applySetpoints(targetHeat, targetCool)
    } else {
        logWarn "Could not apply setpoints. Active Period: ${activePeriod}, Heat: ${targetHeat}, Cool: ${targetCool}"
    }

    // Refresh memTile HTML attribute
    updateMemTile()
}

private void applySetpoints(Integer heat, Integer cool) {
    def currentHeatSp = targetThermostat.currentValue("heatingSetpoint")
    def currentCoolSp = targetThermostat.currentValue("coolingSetpoint")

    if (currentHeatSp != heat) {
        logInfo "Setting ${targetThermostat.displayName} Heating Setpoint to ${heat}° (Active Set: ${state.activeSet})"
        targetThermostat.setHeatingSetpoint(heat)
    }

    if (currentCoolSp != cool) {
        logInfo "Setting ${targetThermostat.displayName} Cooling Setpoint to ${cool}° (Active Set: ${state.activeSet})"
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
            logInfo "Fan Circulation Loop: Turning fan ON for ${fanOnMinutes ?: 30} minutes."
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
                logInfo "Fan Circulation Loop: Setting fan to AUTO for ${fanOffMinutes ?: 30} minutes."
                targetThermostat.fanAuto()
            }
        } else {
            logDebug "Fan Circulation Loop: HVAC actively ${opState}. Leaving fan mode undisturbed."
        }
        runIn(offTime, "toggleFanCirculation", [data: [nextPhase: "ON"]])
    }

    updateMemTile()
}

def toggleFanCirculation(data) {
    if (!fanCirculateEnabled) return
    logDebug "Fan Circulation Timer Fired. Transitioning to ${data?.nextPhase ?: 'next'} phase."
    manageFanCirculation(false)
}

// --- Air Quality Automation Logic ---

def aqiHandler(evt) {
    logDebug "Air Quality Sensor Event (${evt.device.displayName}): ${evt.name} = ${evt.value}"
    evaluateAirQuality()
}

def evaluateAirQuality() {
    if (!aqiEnabled) return

    def val1 = aqiSensor1 ? aqiSensor1.currentValue("airQualityIndex") : null
    def val2 = aqiSensor2 ? aqiSensor2.currentValue("airQualityIndex") : null

    if (val1 == null && val2 == null) {
        logWarn "Could not evaluate Air Quality. Both sensors returned null."
        return
    }

    Integer v1 = (val1 != null) ? (val1 as Integer) : (val2 as Integer)
    Integer v2 = (val2 != null) ? (val2 as Integer) : (val1 as Integer)
    Integer avgAqi = Math.round((v1 + v2) / 2.0) as Integer

    state.aqi1Val = v1
    state.aqi2Val = v2
    state.avgAqi = avgAqi

    Integer goodMax = (aqiGoodMax != null) ? (aqiGoodMax as Integer) : 50
    Integer modMax = (aqiModMax != null) ? (aqiModMax as Integer) : 99

    Map colorMap = [:]
    Boolean filterOn = false

    if (avgAqi <= goodMax) {
        // GREEN - Good
        state.aqiStatus = "Green (Good)"
        colorMap = [hue: 33, saturation: 100, level: 100]
        filterOn = false
    } else if (avgAqi > goodMax && avgAqi <= modMax) {
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

    logInfo "AQI Evaluated: Avg ${avgAqi} -> Status: ${state.aqiStatus} | Filter Action: ${filterOn ? 'ON' : 'OFF'}"

    // Apply RGB Light Color
    if (rgbLight) {
        if (rgbLight.currentValue("switch") != "on") {
            rgbLight.on()
        }
        rgbLight.setColor(colorMap)
    }

    // Apply Air Filter State independently if enabled
    if (airFilterEnabled && airFilterSwitch) {
        String currentFilterState = airFilterSwitch.currentValue("switch")
        if (filterOn && currentFilterState != "on") {
            logInfo "Turning Air Filter ON due to elevated AQI (${avgAqi})"
            airFilterSwitch.on()
        } else if (!filterOn && currentFilterState != "off") {
            logInfo "Turning Air Filter OFF (AQI Good: ${avgAqi})"
            airFilterSwitch.off()
        }
    }

    updateMemTile()
}

// --- Tile Rendering & Device Management ---

private void ensureChildDevice() {
    String childDni = "MEM_TILE_${app.id}"
    def child = getChildDevice(childDni)
    if (!child) {
        logInfo "Creating Child Tile Device (${childDni})"
		addChildDevice("hubitat", "Generic Component Air Quality Sensor", deviceNetworkId, [
			name: "Air Quality Sensor", isComponent: true])
            label: "${app.label ?: 'Multiperiod Environment Manager'} Tile",
            isComponent: true
        ])
    }
}

private void updateMemTile() {
    def child = getChildDevice("MEM_TILE_${app.id}")
    if (!child) return

    String appStatus = appEnabled ? "ON" : "OFF"
    String activePeriod = state.activeSet ?: "Not Evaluated"
    
    def realHeat = targetThermostat ? targetThermostat.currentValue("heatingSetpoint") : "--"
    def realCool = targetThermostat ? targetThermostat.currentValue("coolingSetpoint") : "--"
    def appHeat = state.currentHeat ?: "--"
    def appCool = state.currentCool ?: "--"
    
    String avgAqi = state.avgAqi != null ? "${state.avgAqi}" : "N/A"
    String filterStatus = (airFilterEnabled && airFilterSwitch) ? (airFilterSwitch.currentValue("switch")?.toUpperCase() ?: "OFF") : "N/A"

    String tileHtml = "<div style='font-size:12px; line-height:1.3; text-align:left; padding:4px;'>"
    tileHtml += "<b>MEM State:</b> ${appStatus} | <b>Period:</b> ${activePeriod}<br/>"
    tileHtml += "<b>Device Setpoints:</b> ${realHeat}° / ${realCool}°<br/>"
    tileHtml += "<b>Target Setpoints:</b> ${appHeat}° / ${appCool}°<br/>"
    tileHtml += "<b>AQI Avg:</b> ${avgAqi} | <b>Air Filter:</b> ${filterStatus}"
    tileHtml += "</div>"

    sendIfChanged([device: child, name: "memTile", value: tileHtml])
}

private Boolean isTimeBetween(String startTimeStr, String endTimeStr, Date currentTime) {
    if (!startTimeStr || !endTimeStr) return false
    Date start = toDateTime(startTimeStr)
    Date end = toDateTime(endTimeStr)
    if (end.before(start)) {
        return !currentTime.before(start) || !currentTime.after(end)
    } else {
        return !currentTime.before(start) && !currentTime.after(end)
    }
}

// --- Helper & Utility Routines ---

private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    
    if (args.device) {
        // Device attribute execution
        def dev = args.device
        String oldVal = dev.currentValue(args.name as String)?.toString()
        String newVal = args.value != null ? args.value.toString() : ""
        if (oldVal != newVal) {
            dev.sendEvent(name: args.name, value: args.value)
            logDebug "Child Device Attribute updated: ${args.name} -> ${args.value}"
        }
    } else {
        // App State execution
        String oldVal = state[args.name as String]?.toString()
        String newVal = args.value != null ? args.value.toString() : ""
        if (oldVal != newVal) {
            state[args.name as String] = args.value
            logDebug "State updated: ${args.name} -> ${args.value}"
        }
    }
}

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
}

private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "${app.label ?: 'Multiperiod Environment Manager'}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }