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
 *      2026-08-05    Gemini        0.2.0       Fixed UnknownDeviceTypeException by switching child device type to 'Virtual Variable'[cite: 1]
 *      2026-08-05    jshimota      0.2.1       Fixed ensureChildDevice syntax error and map bracket structure
 *      2026-08-05    Gemini        0.2.2       Updated ensureChildDevice to target custom 'MEM Dashboard Tile' driver
 *      2026-08-05    Gemini        0.2.3       Completed comprehensive 17-point verification audit and code pass
 *      2026-08-05    Gemini        0.2.4       Refactored transient state variables into local variables to reduce DB writes
 *      2026-08-05    Gemini        0.2.5       Added memTile HTML string comparison guard to suppress redundant tile events
 *      2026-08-05    Gemini        0.2.6       Standardized event dispatches to route exclusively through sendIfChanged
 *      2026-08-05    Gemini        0.2.7       Separated sendIfChanged into updateStateValue and updateDeviceAttribute
 *      2026-08-05    Gemini        0.2.8       Renamed helper routines to sendIfChangedStateValue and sendIfChangedAttributeValue
 *      2026-08-05    Gemini        0.2.9       Removed evaluateAirQuality side-effects from mainPage UI rendering
 *      2026-08-05    Gemini        0.3.0       Cached targetThermostat attribute reads into single-pass local variables
 *      2026-08-05    Gemini        0.3.1       Hardened fan circulation loop timers against reboot/orphan states
 *      2026-08-05    Gemini        0.3.2       Cached logging flags during initialize() to eliminate repeated settings lookups
 *      2026-08-05    Gemini        0.3.3       Decomposed evaluateAirQuality into modular sub-routines (SRP)
 *      2026-08-05    Gemini        0.3.4       Replaced HTML string concatenations with StringBuilder
 *      2026-08-05    Gemini        0.3.5       Replaced 5-min polling loop with exact schedule boundary scheduling
 *      2026-08-05    Gemini        0.3.6       Centralized AQI threshold evaluation into getAqiStatus(avg) helper
 *      2026-08-05    Gemini        0.3.7       Wrapped all device state queries and command dispatches in missing device recovery handlers
 *      2026-08-05    Gemini        0.3.8       Standardized method typing: private void for actions, explicit object types for returns
 */

static String version() { return '0.3.8' }

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
            StringBuilder colorTableSb = new StringBuilder()
            colorTableSb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>")
                       .append("<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>")
                       .append("<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Status</th><th style='padding:4px;'>Indicator Color</th>")
                       .append("</tr>")
                       .append("<tr style='border-bottom:1px solid #eee;'>")
                       .append("<td style='padding:4px;'>0 - ").append(goodMax).append("</td><td style='padding:4px; color:green;'><b>Good</b></td><td style='padding:4px;'>Green</td>")
                       .append("</tr>")
                       .append("<tr style='border-bottom:1px solid #eee;'>")
                       .append("<td style='padding:4px;'>").append(goodMax + 1).append(" - ").append(modMax).append("</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>Yellow</td>")
                       .append("</tr>")
                       .append("<tr>")
                       .append("<td style='padding:4px;'>&ge; ").append(modMax + 1).append("</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>Red</td>")
                       .append("</tr>")
                       .append("</table>")
            paragraph colorTableSb.toString()

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
            StringBuilder aqiTableSb = new StringBuilder()
            aqiTableSb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>")
                     .append("<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>")
                     .append("<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Status</th><th style='padding:4px;'>Indicator Color</th><th style='padding:4px;'>Air Filter State</th>")
                     .append("</tr>")
                     .append("<tr style='border-bottom:1px solid #eee;'>")
                     .append("<td style='padding:4px;'>0 - ").append(goodMax).append("</td><td style='padding:4px; color:green;'><b>Good</b></td><td style='padding:4px;'>Green</td><td style='padding:4px;'>OFF</td>")
                     .append("</tr>")
                     .append("<tr style='border-bottom:1px solid #eee;'>")
                     .append("<td style='padding:4px;'>").append(goodMax + 1).append(" - ").append(modMax).append("</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>Yellow</td><td style='padding:4px; color:green;'><b>ON</b></td>")
                     .append("</tr>")
                     .append("<tr>")
                     .append("<td style='padding:4px;'>&ge; ").append(modMax + 1).append("</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>Red</td><td style='padding:4px; color:green;'><b>ON</b></td>")
                     .append("</tr>")
                     .append("</table>")
            paragraph aqiTableSb.toString()

            input name: "airFilterEnabled", type: "bool", title: "Enable Automated Air Filter Control", defaultValue: true, submitOnChange: true
            if (airFilterEnabled) {
                input name: "airFilterSwitch", type: "capability.switch", title: "Select Air Filter Switch/Device", required: true, multiple: false, submitOnChange: true
            }
        }

        // Live Read-Only UI Displays
        Map aqiValues = getRawAQISensorValues()
        def v1 = aqiValues.v1
        def v2 = aqiValues.v2
        Integer avgAqi = calculateAverageAQI(v1, v2)
        
        Map aqiStatusInfo = getAqiStatus(avgAqi)
        String displayAqiStatus = aqiStatusInfo.status

        // Single-pass snapshot of Thermostat state for UI render
        Map tStates = getThermostatStateSnapshot()
        def devHeat = tStates.heatingSetpoint ?: "--"
        def devCool = tStates.coolingSetpoint ?: "--"
        def devThermostatMode = tStates.thermostatMode ?: "--"
        def devFanMode = tStates.thermostatFanMode ?: "--"
        def devOpState = tStates.thermostatOperatingState ?: "--"

        Map scheduleData = getCalculatedScheduleData()

        section("<b>Current Status</b>") {
            paragraph "<b>Connected Thermostat Device:</b> ${targetThermostat ? targetThermostat.displayName : 'None Selected'}"
            paragraph "<b>Device Live Status:</b> Heat: ${devHeat}° | Cool: ${devCool}° | Mode: ${devThermostatMode} | Fan Mode: ${devFanMode} | State: ${devOpState}"
            paragraph "--------------------------------------------------"
            paragraph "<b>Active Schedule Period:</b> ${scheduleData.activePeriod ?: 'Not Evaluated'}"
            paragraph "<b>Target Setpoints:</b> Heat: ${scheduleData.targetHeat ?: '--'}° | Cool: ${scheduleData.targetCool ?: '--'}°"
            paragraph "<b>Fan Circulation Status:</b> ${fanCirculateEnabled ? (state.fanPhase ?: 'Initializing') : 'Disabled'}"
            paragraph "--------------------------------------------------"
            if (aqiEnabled) {
                paragraph "<b>AQI Sensor 1 (${aqiSensor1?.displayName ?: 'None'}):</b> ${v1 ?: '--'}"
                paragraph "<b>AQI Sensor 2 (${aqiSensor2?.displayName ?: 'None'}):</b> ${v2 ?: '--'}"
                paragraph "<b>Average AQI:</b> ${avgAqi ?: '--'} (${displayAqiStatus})"
            } else {
                paragraph "<b>Air Quality Monitoring:</b> Disabled"
            }
            if (airFilterEnabled) {
                def filterVal = safeGetDeviceAttribute(airFilterSwitch, 'switch')
                paragraph "<b>Air Filter Status:</b> ${filterVal ?: 'No Switch Selected / Unavailable'}"
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
    // Cache logging preferences in memory
    cacheLoggingFlags()

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

    // Schedule exact time boundaries instead of polling
    scheduleTimeBoundaries()
    
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
        unschedule("toggleFanCirculation")
        sendIfChangedStateValue("fanPhase", "Disabled")
    }
}

private void scheduleTimeBoundaries() {
    unschedule("evaluateSchedule")
    
    List timeInputs = [morningStart, morningEnd, dayStart, dayEnd, eveningStart, eveningEnd, nightStart, nightEnd]
    timeInputs.unique().each { timeStr ->
        if (timeStr) {
            try {
                Date timeVal = toDateTime(timeStr)
                schedule(timeVal, "evaluateSchedule")
                logDebug "Scheduled exact boundary evaluation for ${timeStr}"
            } catch (Exception e) {
                logError "Failed to parse time string '${timeStr}' for boundary scheduling: ${e.message}"
            }
        }
    }
}

def overrideSwitchHandler(evt) {
    logInfo "Override Switch Changed (${evt?.device?.displayName ?: 'Switch'}): ${evt?.value}. Re-evaluating schedule."
    evaluateSchedule()
}

def thermostatSetpointHandler(evt) {
    logInfo "Thermostat setpoint updated externally: ${evt?.name} = ${evt?.value}°"
    evaluateSchedule()
}

def airFilterSwitchHandler(evt) {
    logInfo "Air Filter Switch state changed externally: ${evt?.value}"
    updateMemTile()
}

private Object safeGetDeviceAttribute(Object device, String attributeName) {
    if (!device) return null
    try {
        return device.currentValue(attributeName)
    } catch (Exception e) {
        logWarn "Could not read attribute '${attributeName}' from device '${device}': ${e.message}"
        return null
    }
}

private Map getThermostatStateSnapshot() {
    if (!targetThermostat) return [:]
    return [
        heatingSetpoint: safeGetDeviceAttribute(targetThermostat, "heatingSetpoint"),
        coolingSetpoint: safeGetDeviceAttribute(targetThermostat, "coolingSetpoint"),
        thermostatMode: safeGetDeviceAttribute(targetThermostat, "thermostatMode"),
        thermostatFanMode: safeGetDeviceAttribute(targetThermostat, "thermostatFanMode"),
        thermostatOperatingState: safeGetDeviceAttribute(targetThermostat, "thermostatOperatingState")
    ]
}

private Map getCalculatedScheduleData() {
    if (!appEnabled) {
        return [activePeriod: "Disabled (App OFF)", targetHeat: null, targetCool: null]
    }

    Date now = new Date()
    String activePeriod = ""
    Integer targetHeat = null
    Integer targetCool = null

    // 1. Check Switch Overrides First
    Boolean isAway = awaySwitch ? (safeGetDeviceAttribute(awaySwitch, "switch") == "on") : false
    Boolean isSleeping = sleepSwitch ? (safeGetDeviceAttribute(sleepSwitch, "switch") == "on") : false

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

    return [activePeriod: activePeriod, targetHeat: targetHeat, targetCool: targetCool]
}

def evaluateSchedule() {
    Map sched = getCalculatedScheduleData()
    
    if (!appEnabled) {
        logInfo "Multiperiod Environment Manager is currently DISABLED via main switch."
        sendIfChangedStateValue("activeSet", sched.activePeriod)
        updateMemTile()
        return
    }

    Date now = new Date()

    // Update essential persistent tracking via sendIfChangedStateValue
    sendIfChangedStateValue("activeSet", sched.activePeriod)
    sendIfChangedStateValue("lastEvaluated", now.format("yyyy-MM-dd HH:mm:ss", location.timeZone))

    // Apply setpoints
    if (sched.targetHeat != null && sched.targetCool != null && targetThermostat) {
        applySetpoints(sched.targetHeat as Integer, sched.targetCool as Integer)
    } else {
        logWarn "Could not apply setpoints. Active Period: ${sched.activePeriod}, Heat: ${sched.targetHeat}, Cool: ${sched.targetCool}"
    }

    // Guard/re-arm Fan Circulation Loop if timers cleared or lost after reboot
    if (fanCirculateEnabled && (!state.fanPhase || state.fanPhase == "Disabled" || state.fanPhase == "Initializing")) {
        logInfo "Fan Circulation Loop was uninitialized or interrupted. Restarting loop."
        manageFanCirculation(true)
    }

    // Refresh memTile HTML attribute
    updateMemTile()
}

private void applySetpoints(Integer heat, Integer cool) {
    if (!targetThermostat) return
    Map tStates = getThermostatStateSnapshot()
    def currentHeatSp = tStates.heatingSetpoint
    def currentCoolSp = tStates.coolingSetpoint

    try {
        if (currentHeatSp != heat) {
            logInfo "Setting ${targetThermostat.displayName} Heating Setpoint to ${heat}° (Active Set: ${state.activeSet})"
            targetThermostat.setHeatingSetpoint(heat)
        }

        if (currentCoolSp != cool) {
            logInfo "Setting ${targetThermostat.displayName} Cooling Setpoint to ${cool}° (Active Set: ${state.activeSet})"
            targetThermostat.setCoolingSetpoint(cool)
        }
    } catch (Exception e) {
        logError "Failed to apply setpoints to ${targetThermostat.displayName}: ${e.message}"
    }
}

// --- Fan Circulation Simulation Logic ---

def manageFanCirculation(Boolean isInitial = false) {
    if (!fanCirculateEnabled || !targetThermostat) return

    // Explicitly unschedule prior toggle timers to prevent overlapping loops
    unschedule("toggleFanCirculation")

    Integer onTime = (fanOnMinutes ?: 30) * 60
    Integer offTime = (fanOffMinutes ?: 30) * 60

    Map tStates = getThermostatStateSnapshot()

    try {
        if (isInitial || state.fanPhase == "OFF Phase" || !state.fanPhase) {
            // Start ON Phase
            sendIfChangedStateValue("fanPhase", "ON Phase")
            if (tStates.thermostatFanMode != "on") {
                logInfo "Fan Circulation Loop: Turning fan ON for ${fanOnMinutes ?: 30} minutes."
                targetThermostat.fanOn()
            }
            runIn(onTime, "toggleFanCirculation", [data: [nextPhase: "OFF"]])
        } else {
            // Start OFF Phase
            sendIfChangedStateValue("fanPhase", "OFF Phase")
            
            // Safety Guard: Don't override fan if heating or cooling is active
            String opState = tStates.thermostatOperatingState
            if (opState != "heating" && opState != "cooling") {
                if (tStates.thermostatFanMode != "auto") {
                    logInfo "Fan Circulation Loop: Setting fan to AUTO for ${fanOffMinutes ?: 30} minutes."
                    targetThermostat.fanAuto()
                }
            } else {
                logDebug "Fan Circulation Loop: HVAC actively ${opState}. Leaving fan mode undisturbed."
            }
            runIn(offTime, "toggleFanCirculation", [data: [nextPhase: "ON"]])
        }
    } catch (Exception e) {
        logError "Failed to execute fan circulation command on ${targetThermostat.displayName}: ${e.message}"
    }

    updateMemTile()
}

def toggleFanCirculation(data) {
    if (!fanCirculateEnabled) return
    logDebug "Fan Circulation Timer Fired. Transitioning to ${data?.nextPhase ?: 'next'} phase."
    manageFanCirculation(false)
}

// --- Air Quality Automation Logic (SRP Refactored) ---

def aqiHandler(evt) {
    logDebug "Air Quality Sensor Event (${evt?.device?.displayName ?: 'AQI Sensor'}): ${evt?.name} = ${evt?.value}"
    evaluateAirQuality()
}

private Map getRawAQISensorValues() {
    def v1 = safeGetDeviceAttribute(aqiSensor1, "airQualityIndex")
    def v2 = safeGetDeviceAttribute(aqiSensor2, "airQualityIndex")
    return [v1: v1, v2: v2]
}

private Integer calculateAverageAQI(Object val1, Object val2) {
    if (val1 == null && val2 == null) return null
    Integer v1 = (val1 != null) ? (val1 as Integer) : (val2 as Integer)
    Integer v2 = (val2 != null) ? (val2 as Integer) : (val1 as Integer)
    return Math.round((v1 + v2) / 2.0) as Integer
}

private Map getAqiStatus(Object avgAqiVal) {
    if (avgAqiVal == null || !(avgAqiVal instanceof Integer)) {
        return [status: "Not Evaluated", colorMap: [:], filterOn: false]
    }
    
    Integer avgAqi = avgAqiVal as Integer
    Integer goodMax = (aqiGoodMax != null) ? (aqiGoodMax as Integer) : 50
    Integer modMax = (aqiModMax != null) ? (aqiModMax as Integer) : 99

    if (avgAqi <= goodMax) {
        return [status: "Green (Good)", colorMap: [hue: 33, saturation: 100, level: 100], filterOn: false]
    } else if (avgAqi <= modMax) {
        return [status: "Yellow (Moderate)", colorMap: [hue: 16, saturation: 100, level: 100], filterOn: true]
    } else {
        return [status: "Red (Unhealthy)", colorMap: [hue: 0, saturation: 100, level: 100], filterOn: true]
    }
}

private void applyIndicatorLight(Map colorMap) {
    if (!rgbLight || !colorMap) return
    try {
        if (safeGetDeviceAttribute(rgbLight, "switch") != "on") {
            rgbLight.on()
        }
        rgbLight.setColor(colorMap)
    } catch (Exception e) {
        logError "Failed to set indicator light color on ${rgbLight.displayName}: ${e.message}"
    }
}

private void applyAirFilterSwitch(Boolean filterOn, Integer avgAqi) {
    if (!airFilterEnabled || !airFilterSwitch) return
    String currentFilterState = safeGetDeviceAttribute(airFilterSwitch, "switch")
    try {
        if (filterOn && currentFilterState != "on") {
            logInfo "Turning Air Filter ON due to elevated AQI (${avgAqi})"
            airFilterSwitch.on()
        } else if (!filterOn && currentFilterState != "off") {
            logInfo "Turning Air Filter OFF (AQI Good: ${avgAqi})"
            airFilterSwitch.off()
        }
    } catch (Exception e) {
        logError "Failed to set switch state on Air Filter device ${airFilterSwitch.displayName}: ${e.message}"
    }
}

def evaluateAirQuality() {
    if (!aqiEnabled) return [:]

    Map rawVals = getRawAQISensorValues()
    Integer avgAqi = calculateAverageAQI(rawVals.v1, rawVals.v2)

    if (avgAqi == null) {
        logWarn "Could not evaluate Air Quality. Both sensors returned null or were unavailable."
        return [:]
    }

    Map statusMap = getAqiStatus(avgAqi)
    logInfo "AQI Evaluated: Avg ${avgAqi} -> Status: ${statusMap.status} | Filter Action: ${statusMap.filterOn ? 'ON' : 'OFF'}"

    applyIndicatorLight(statusMap.colorMap)
    applyAirFilterSwitch(statusMap.filterOn, avgAqi)

    updateMemTile(avgAqi)

    return [v1: rawVals.v1, v2: rawVals.v2, avgAqi: avgAqi, aqiStatus: statusMap.status]
}

// --- Tile Rendering & Device Management ---

private void ensureChildDevice() {
    String childDni = "MEM_TILE_${app.id}"
    def child = getChildDevice(childDni)
    if (!child) {
        logInfo "Creating Child Tile Device (${childDni})"
        try {
            addChildDevice("jshimota", "MEM Dashboard Tile", childDni, [
                name: "MEM Tile Device",
                label: "${app.label ?: 'Multiperiod Environment Manager'} Tile",
                isComponent: true
            ])
        } catch (Exception e) {
            logError "Failed to create child tile device '${childDni}': ${e.message}"
        }
    }
}

private void updateMemTile(Integer currentAvgAqi = null) {
    def child = getChildDevice("MEM_TILE_${app.id}")
    if (!child) return

    String appStatus = appEnabled ? "ON" : "OFF"
    String activePeriod = state.activeSet ?: "Not Evaluated"
    
    Map tStates = getThermostatStateSnapshot()
    def realHeat = tStates.heatingSetpoint ?: "--"
    def realCool = tStates.coolingSetpoint ?: "--"
    
    Map sched = getCalculatedScheduleData()
    def appHeat = sched.targetHeat ?: "--"
    def appCool = sched.targetCool ?: "--"
    
    String avgAqi = "N/A"
    if (currentAvgAqi != null) {
        avgAqi = "${currentAvgAqi}"
    } else if (aqiEnabled && (aqiSensor1 || aqiSensor2)) {
        Map rawVals = getRawAQISensorValues()
        Integer calcAvg = calculateAverageAQI(rawVals.v1, rawVals.v2)
        if (calcAvg != null) avgAqi = "${calcAvg}"
    }
    
    def rawFilterSwitchVal = safeGetDeviceAttribute(airFilterSwitch, "switch")
    String filterStatus = (airFilterEnabled && airFilterSwitch) ? (rawFilterSwitchVal?.toUpperCase() ?: "OFF") : "N/A"

    StringBuilder tileSb = new StringBuilder()
    tileSb.append("<div style='font-size:12px; line-height:1.3; text-align:left; padding:4px;'>")
          .append("<b>MEM State:</b> ").append(appStatus).append(" | <b>Period:</b> ").append(activePeriod).append("<br/>")
          .append("<b>Device Setpoints:</b> ").append(realHeat).append("° / ").append(realCool).append("°<br/>")
          .append("<b>Target Setpoints:</b> ").append(appHeat).append("° / ").append(appCool).append("°<br/>")
          .append("<b>AQI Avg:</b> ").append(avgAqi).append(" | <b>Air Filter:</b> ").append(filterStatus)
          .append("</div>")

    // Deduplicated event dispatching via sendIfChangedAttributeValue
    sendIfChangedAttributeValue(child, "memTile", tileSb.toString())
}

private Boolean isTimeBetween(String startTimeStr, String endTimeStr, Date currentTime) {
    if (!startTimeStr || !endTimeStr) return false
    try {
        Date start = toDateTime(startTimeStr)
        Date end = toDateTime(endTimeStr)
        if (end.before(start)) {
            return !currentTime.before(start) || !currentTime.after(end)
        } else {
            return !currentTime.before(start) && !currentTime.after(end)
        }
    } catch (Exception e) {
        logError "Error parsing time boundaries in isTimeBetween (${startTimeStr} - ${endTimeStr}): ${e.message}"
        return false
    }
}

// --- Helper & Utility Routines ---

private void sendIfChangedStateValue(String key, Object value) {
    if (!key) return
    String oldVal = state[key]?.toString()
    String newVal = value != null ? value.toString() : ""
    if (oldVal != newVal) {
        state[key] = value
        logDebug "State updated: ${key} -> ${value}"
    }
}

private void sendIfChangedAttributeValue(Object device, String attributeName, Object value) {
    if (!device || !attributeName) return
    try {
        String oldVal = device.currentValue(attributeName as String)?.toString()
        String newVal = value != null ? value.toString() : ""
        if (oldVal != newVal) {
            device.sendEvent(name: attributeName, value: value)
            logDebug "Child Device Attribute updated: ${attributeName} -> ${value}"
        }
    } catch (Exception e) {
        logError "Failed to update device attribute '${attributeName}' on ${device}: ${e.message}"
    }
}

private void cacheLoggingFlags() {
    state.logFlags = [
        info: logInfoEnable != false,
        debug: logDebugEnable == true,
        trace: logTraceEnable == true,
        warn: logWarnEnable != false,
        error: logErrorEnable != false
    ]
}

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
    cacheLoggingFlags()
}

private void logMessage(String level, String msg) {
    Map flags = state.logFlags ?: [info: true, debug: false, trace: false, warn: true, error: true]
    if (flags[level] == true) {
        log."${level}" "${app.label ?: 'Multiperiod Environment Manager'}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }