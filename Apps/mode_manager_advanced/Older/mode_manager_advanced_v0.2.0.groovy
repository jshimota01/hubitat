/*
 * Mode Manager Advanced
 *  Improved Mode Manager that uses Presence and Sleeping in addition to time periods
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                    
 *      ----          ------        -------     ----                                    
 *      2026-08-06    jshimota      0.0.1       Initial release as Mode Manager Advanced
 *      2026-08-06    jshimota      0.0.2       Connected virtual switches to app
 *      2026-08-06    jshimota      0.0.3       Removed away mode requirement; changed to presence tracking
 *      2026-08-06    jshimota      0.0.4       Added reverse-mirrored Home/Away presence switches
 *      2026-08-06    jshimota      0.0.5       Added HTML table layout and dynamic time scheduling per period
 *      2026-08-06    jshimota      0.0.6       Added Alexa Mode virtual switch sync and HSM integration
 *      2026-08-06    jshimota      0.0.7       Added trigger switch option to force app update/evaluation
 *      2026-08-06    jshimota      0.0.8       Added subscriptions and handler for manual virtual mode switch toggles
 *      2026-08-06    jshimota      0.0.9       Added manual mode override state tracking to prevent time calculation reset
 *      2026-08-06    jshimota      0.0.10      Updated namespace to jshimota and author to James Shimota
 *      2026-08-06    jshimota      0.0.11      Added log.info event messaging for mode updates
 *      2026-08-06    jshimota      0.1.2       Added standardized logging methods and 30-min auto-disable for debug logs
 *      2026-08-06    jshimota      0.1.3       Added dynamic green current mode display in Hubitat Apps list
 *      2026-08-06    jshimota      0.1.4       Fixed switch synchronization logic to ensure manual virtual switch overrides turn off other period switches without self-canceling
 *      2026-08-06    jshimota      0.1.5       Added explicit event logging when mode evaluation is triggered by virtual mode switch
 *      2026-08-06    jshimota      0.1.6       Added explicit log message specifying switch manual override action
 *      2026-08-07    jshimota      0.1.7       Fixed race conditions on Home/Away and Awake/Sleeping reverse-mirrored switches
 *      2026-08-07    jshimota      0.1.8       Implemented recurring daily CRON schedules and streamlined presence event routing
 *      2026-08-07    jshimota      0.1.9       Enforced Hubitat location.timeZone and dynamic chronological sorting for time periods
 *      2026-08-07    jshimota      0.2.0       Converted virtual indicators from Mode matching to unique Period Key matching to resolve duplicate mode conflicts
 *
 */

import java.text.SimpleDateFormat

static String version() { return '0.2.0' }

definition(
    name: "Mode Manager Advanced",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Advanced Hubitat Mode Manager driven by master presence, reverse-mirrored presence and sleep/awake switches, dynamic time periods, virtual mode indicators, Alexa Mode sync, and HSM control.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Mode Manager Advanced Configuration (v${version()})", install: true, uninstall: true) {
        
        section("Presence Settings & State Switches (Reverse Mirrored)") {
            input name: "masterPresence", type: "capability.presenceSensor", title: "Master Presence Sensor (OwnTracks - Jim)", required: true, submitOnChange: true
            input name: "homeSwitch", type: "capability.switch", title: "Home Switch", required: true, submitOnChange: true
            input name: "awaySwitch", type: "capability.switch", title: "Away Switch", required: true, submitOnChange: true
            input name: "alexaModeSwitch", type: "capability.switch", title: "Alexa Mode Virtual Switch (ON = Home, OFF = Away)", required: false
            input name: "manageHSM", type: "bool", title: "Control Hubitat Safety Monitor (HSM) based on Presence?", defaultValue: true
        }
        
        section("Sleep / Awake State Switches (Reverse Mirrored)") {
            input name: "sleepSwitch", type: "capability.switch", title: "Sleeping Switch", required: true, submitOnChange: true
            input name: "awakeSwitch", type: "capability.switch", title: "Awake Switch", required: true, submitOnChange: true
            input name: "sleepMode", type: "mode", title: "Target Mode when Sleeping", required: true, defaultValue: "Sleeping"
            input name: "vSwitchSleeping", type: "capability.switch", title: "Virtual Switch for Sleeping Mode", required: false
        }

        section("Time Period Configuration") {
            paragraph "<table style='width:100%; text-align:left; border-collapse: collapse;'>" +
                      "<tr style='border-bottom: 2px solid #666;'><th>Period</th><th>Start Time</th><th>Target Mode</th><th>Virtual Switch Indicator</th></tr>" +
                      "</table>"

            input name: "timeWeeHours", type: "time", title: "Wee Hours Start Time", required: true, defaultValue: "00:30", width: 6
            input name: "weeHoursMode", type: "mode", title: "Wee Hours Target Mode", required: true, defaultValue: "Night", width: 3
            input name: "vSwitchWeeHours", type: "capability.switch", title: "Virtual Switch", required: false, width: 3
            
            input name: "timeEarlyMorning", type: "time", title: "Early Morning Start Time", required: true, defaultValue: "04:45", width: 6
            input name: "earlyMorningMode", type: "mode", title: "Early Morning Target Mode", required: true, defaultValue: "Early Morning", width: 3
            input name: "vSwitchEarlyMorning", type: "capability.switch", title: "Virtual Switch", required: false, width: 3
            
            input name: "timeMorning", type: "time", title: "Morning Start Time", required: true, defaultValue: "07:30", width: 6
            input name: "morningMode", type: "mode", title: "Morning Target Mode", required: true, defaultValue: "Morning", width: 3
            input name: "vSwitchMorning", type: "capability.switch", title: "Virtual Switch", required: false, width: 3
            
            input name: "timeDay", type: "time", title: "Day Start Time", required: true, defaultValue: "10:00", width: 6
            input name: "dayMode", type: "mode", title: "Day Target Mode", required: true, defaultValue: "Day", width: 3
            input name: "vSwitchDay", type: "capability.switch", title: "Virtual Switch", required: false, width: 3
            
            input name: "timeEvening", type: "time", title: "Evening Start Time", required: true, defaultValue: "17:00", width: 6
            input name: "eveningMode", type: "mode", title: "Evening Target Mode", required: true, defaultValue: "Evening", width: 3
            input name: "vSwitchEvening", type: "capability.switch", title: "Virtual Switch", required: false, width: 3
            
            input name: "timeLateEvening", type: "time", title: "Late Evening Start Time", required: true, defaultValue: "21:30", width: 6
            input name: "lateEveningMode", type: "mode", title: "Late Evening Target Mode", required: true, defaultValue: "Late Evening", width: 3
            input name: "vSwitchLateEvening", type: "capability.switch", title: "Virtual Switch", required: false, width: 3
        }

        section("Manual Overrides & External Triggers") {
            input name: "btnTrigger", type: "button", title: "Evaluate & Set Mode Now"
            input name: "updateTriggerSwitch", type: "capability.switch", title: "Switch to Trigger Evaluation/Update", required: false
        }

        section("Logging Options") {
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
        }
    }
}

def installed() {
    logDebug "Installed v${version()} with settings: ${settings}"
    initialize()
}

def updated() {
    logDebug "Updated v${version()} with settings: ${settings}"
    unsubscribe()
    unschedule()
    if (logDebugEnable) {
        runIn(1800, disableDebugLogging)
    }
    initialize()
}

def initialize() {
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    // Master Presence Subscription
    if (masterPresence) {
        subscribe(masterPresence, "presence", presenceHandler)
    }
    
    // Reverse-Mirrored Home Presence Subscription (monitored primary)
    if (homeSwitch) subscribe(homeSwitch, "switch", homeSwitchHandler)

    // Reverse-Mirrored Awake Subscription (monitored primary)
    if (awakeSwitch) subscribe(awakeSwitch, "switch", awakeSwitchHandler)
    
    // External Trigger Switch Subscription
    if (updateTriggerSwitch) subscribe(updateTriggerSwitch, "switch.on", updateSwitchHandler)

    // Virtual Mode Switch Indicator Subscriptions (Manual Overrides)
    [vSwitchSleeping, vSwitchWeeHours, vSwitchEarlyMorning, vSwitchMorning, vSwitchDay, vSwitchEvening, vSwitchLateEvening].each { vSwitch ->
        if (vSwitch) subscribe(vSwitch, "switch.on", vSwitchHandler)
    }

    // Schedule Dynamic Daily Time Period Transitions
    reschedulePeriods()
    
    // Initial State Check
    evaluateAndSetMode()
}

def reschedulePeriods() {
    logDebug "Rescheduling daily recurring time period triggers based on updated settings..."
    
    schedulePeriodTime(timeWeeHours)
    schedulePeriodTime(timeEarlyMorning)
    schedulePeriodTime(timeMorning)
    schedulePeriodTime(timeDay)
    schedulePeriodTime(timeEvening)
    schedulePeriodTime(timeLateEvening)
}

def schedulePeriodTime(String timeIso) {
    if (timeIso) {
        Date timeDate = toDateTime(timeIso)
        TimeZone tz = location.timeZone ?: TimeZone.getDefault()
        Calendar cal = Calendar.getInstance(tz)
        cal.setTime(timeDate)
        
        int min = cal.get(Calendar.MINUTE)
        int hour = cal.get(Calendar.HOUR_OF_DAY)
        
        // Dynamic daily recurring CRON expression: Sec Min Hour Day Month DayOfWeek Year
        String cronStr = "0 ${min} ${hour} * * ? *"
        schedule(cronStr, periodHandler)
        logDebug "Scheduled recurring daily period trigger at ${String.format('%02d:%02d', hour, min)} (${tz.ID}) via CRON [${cronStr}]"
    }
}

// Manual App UI Button Handler
def appButtonHandler(btn) {
    if (btn == "btnTrigger") {
        logInfo "Mode recheck triggered manually via App UI button."
        state.manualOverrideMode = null
        state.manualOverridePeriodKey = null
        evaluateAndSetMode()
    }
}

// External Trigger Switch Handler
def updateSwitchHandler(evt) {
    logInfo "Mode recheck triggered via external update switch '${evt.device}' turning ON."
    state.manualOverrideMode = null
    state.manualOverridePeriodKey = null
    evaluateAndSetMode()
    
    // Reset switch back to OFF so it behaves like a trigger/push-button
    if (updateTriggerSwitch && updateTriggerSwitch.currentValue("switch") != "off") {
        logDebug "Resetting trigger switch back to OFF"
        updateTriggerSwitch.off()
    }
}

// Virtual Mode Indicator Switch Manual Toggle Handler
def vSwitchHandler(evt) {
    // Explicit mapping of Virtual Switch ID to Target Mode and Period Key
    Map<String, Map> switchIdToPeriodMap = [:]
    if (vSwitchSleeping)     switchIdToPeriodMap[vSwitchSleeping.id.toString()]     = [mode: sleepMode?.toString(),        key: "sleeping"]
    if (vSwitchWeeHours)     switchIdToPeriodMap[vSwitchWeeHours.id.toString()]     = [mode: weeHoursMode?.toString(),     key: "weeHours"]
    if (vSwitchEarlyMorning) switchIdToPeriodMap[vSwitchEarlyMorning.id.toString()] = [mode: earlyMorningMode?.toString(), key: "earlyMorning"]
    if (vSwitchMorning)      switchIdToPeriodMap[vSwitchMorning.id.toString()]      = [mode: morningMode?.toString(),      key: "morning"]
    if (vSwitchDay)          switchIdToPeriodMap[vSwitchDay.id.toString()]          = [mode: dayMode?.toString(),          key: "day"]
    if (vSwitchEvening)      switchIdToPeriodMap[vSwitchEvening.id.toString()]      = [mode: eveningMode?.toString(),      key: "evening"]
    if (vSwitchLateEvening)  switchIdToPeriodMap[vSwitchLateEvening.id.toString()]  = [mode: lateEveningMode?.toString(),  key: "lateEvening"]

    Map targetPeriod = switchIdToPeriodMap[evt.deviceId.toString()]
    
    if (targetPeriod?.mode) {
        logInfo "Switch '${evt.device}' turned ON to manually override active period to '${targetPeriod.key}' (Mode: '${targetPeriod.mode}')."
        
        // Save state override so state checks don't instantly overwrite manual choice
        state.manualOverrideMode = targetPeriod.mode
        state.manualOverridePeriodKey = targetPeriod.key
        
        // If turning on a daytime mode switch while sleep switch is ON, set Awake switch ON
        if (vSwitchSleeping && evt.deviceId.toString() != vSwitchSleeping.id.toString() && sleepSwitch?.currentValue("switch") == "on") {
            if (awakeSwitch && awakeSwitch.currentValue("switch") != "on") awakeSwitch.on()
        }
        
        changeMode(targetPeriod.mode, targetPeriod.key)
    } else {
        logWarn "Mode recheck triggered by virtual switch '${evt.device}', but switch was not recognized in mode mapping."
    }
}

// Presence Event Handler (OwnTracks Master)
def presenceHandler(evt) {
    logInfo "Master presence change detected: '${evt.device}' is now '${evt.value}'"
    if (evt.value == "present") {
        if (homeSwitch && homeSwitch.currentValue("switch") != "on") homeSwitch.on()
    } else {
        if (homeSwitch && homeSwitch.currentValue("switch") != "off") homeSwitch.off()
    }
}

// Home Primary Reverse-Mirroring Handler
def homeSwitchHandler(evt) {
    logDebug "Home switch changed to ${evt.value}"
    if (evt.value == "on") {
        if (awaySwitch && awaySwitch.currentValue("switch") != "off") {
            logDebug "Home switch turned ON. Forcing Away switch OFF."
            awaySwitch.off()
        }
    } else if (evt.value == "off") {
        if (awaySwitch && awaySwitch.currentValue("switch") != "on") {
            logDebug "Home switch turned OFF. Forcing Away switch ON."
            awaySwitch.on()
        }
    }
    evaluateAndSetMode()
}

// Awake Primary Reverse-Mirroring Handler
def awakeSwitchHandler(evt) {
    logDebug "Awake switch changed to ${evt.value}"
    if (evt.value == "on") {
        if (sleepSwitch && sleepSwitch.currentValue("switch") != "off") {
            logDebug "Awake switch turned ON. Forcing Sleep switch OFF."
            sleepSwitch.off()
        }
    } else if (evt.value == "off") {
        if (sleepSwitch && sleepSwitch.currentValue("switch") != "on") {
            logDebug "Awake switch turned OFF. Forcing Sleep switch ON."
            sleepSwitch.on()
        }
    }
    evaluateAndSetMode()
}

// Scheduled Cron Event Handler
def periodHandler() {
    logInfo "Mode recheck triggered by scheduled time period boundary hit. Clearing manual override."
    state.manualOverrideMode = null
    state.manualOverridePeriodKey = null
    evaluateAndSetMode()
}

// Core State Engine
def evaluateAndSetMode() {
    // 1. Check Presence via Home/Away Switches & Master Sensor
    Boolean isHome = (homeSwitch && homeSwitch.currentValue("switch") == "on") || 
                     (!homeSwitch && masterPresence && masterPresence.currentValue("presence") == "present")
                     
    if (!isHome) {
        logDebug "State Check: Presence is Away."
        state.manualOverrideMode = null
        state.manualOverridePeriodKey = null
        
        // Sync Alexa Mode Switch -> OFF (Away)
        if (alexaModeSwitch && alexaModeSwitch.currentValue("switch") != "off") {
            logDebug "Setting Alexa Mode Virtual Switch to OFF (Away)"
            alexaModeSwitch.off()
        }
        
        // Set HSM to Arm Away
        if (manageHSM) {
            logDebug "Arming Hubitat Safety Monitor (HSM: armAway)"
            sendLocationEvent(name: "hsmSetArm", value: "armAway")
        }

        updateVirtualModeSwitches(null) // Turn off period indicator switches when away
        updateAppLabel("Away")
        return
    } else {
        // Sync Alexa Mode Switch -> ON (Home)
        if (alexaModeSwitch && alexaModeSwitch.currentValue("switch") != "on") {
            logDebug "Setting Alexa Mode Virtual Switch to ON (Home)"
            alexaModeSwitch.on()
        }

        // Set HSM to Disarm
        if (manageHSM) {
            logDebug "Disarming Hubitat Safety Monitor (HSM: disarm)"
            sendLocationEvent(name: "hsmSetArm", value: "disarm")
        }
    }

    // 2. Check Sleep State
    Boolean isSleeping = (sleepSwitch && sleepSwitch.currentValue("switch") == "on")
    if (isSleeping) {
        logDebug "State Check: Home & Sleeping. Target Mode: ${sleepMode}"
        changeMode(sleepMode?.toString(), "sleeping")
        return
    }

    // 3. Check Manual Switch Override State
    if (state.manualOverrideMode) {
        logDebug "State Check: Using active Manual Switch Override Mode: ${state.manualOverrideMode} (Key: ${state.manualOverridePeriodKey})"
        changeMode(state.manualOverrideMode, state.manualOverridePeriodKey)
        return
    }

    // 4. Determine Scheduled Time Period Mode
    Map periodInfo = getActiveTimePeriodInfo()
    logDebug "State Check: Home & Awake. Active Period: ${periodInfo?.key}, Target Mode: ${periodInfo?.mode}"
    
    if (periodInfo?.mode) {
        changeMode(periodInfo.mode, periodInfo.key)
    }
}

// Calculate active time block returning both target mode and active period key
Map getActiveTimePeriodInfo() {
    validateTimePeriods()

    int currentMinutes = timeToMinutes(new Date())

    List<Map> periods = [
        [key: "weeHours",     mode: weeHoursMode?.toString(),     start: getMinutesFromSetting(timeWeeHours, 30)],
        [key: "earlyMorning", mode: earlyMorningMode?.toString(), start: getMinutesFromSetting(timeEarlyMorning, 285)],
        [key: "morning",      mode: morningMode?.toString(),      start: getMinutesFromSetting(timeMorning, 450)],
        [key: "day",          mode: dayMode?.toString(),          start: getMinutesFromSetting(timeDay, 600)],
        [key: "evening",      mode: eveningMode?.toString(),      start: getMinutesFromSetting(timeEvening, 1020)],
        [key: "lateEvening",  mode: lateEveningMode?.toString(),  start: getMinutesFromSetting(timeLateEvening, 1290)]
    ]

    periods.sort { it.start }

    Map activePeriod = periods.reverse().find { currentMinutes >= it.start }

    if (!activePeriod) {
        activePeriod = periods.last()
    }

    return activePeriod
}

// Validates whether configured time periods are chronological
private Boolean validateTimePeriods() {
    List<Map> periods = [
        [name: "Wee Hours",     min: getMinutesFromSetting(timeWeeHours, 30)],
        [name: "Early Morning", min: getMinutesFromSetting(timeEarlyMorning, 285)],
        [name: "Morning",       min: getMinutesFromSetting(timeMorning, 450)],
        [name: "Day",           min: getMinutesFromSetting(timeDay, 600)],
        [name: "Evening",       min: getMinutesFromSetting(timeEvening, 1020)],
        [name: "Late Evening",  min: getMinutesFromSetting(timeLateEvening, 1290)]
    ]
    
    for (int i = 0; i < periods.size() - 1; i++) {
        if (periods[i].min >= periods[i+1].min) {
            logWarn "Time period configuration error: '${periods[i].name}' (${formatMinutes(periods[i].min)}) starts at or after '${periods[i+1].name}' (${formatMinutes(periods[i+1].min)}). Please verify page settings."
            return false
        }
    }
    return true
}

private String formatMinutes(int totalMinutes) {
    int h = totalMinutes / 60
    int m = totalMinutes % 60
    return String.format("%02d:%02d", h, m)
}

private int getMinutesFromSetting(String timeIso, int defaultMinutes) {
    if (!timeIso) return defaultMinutes
    try {
        Date d = toDateTime(timeIso)
        return timeToMinutes(d)
    } catch (Exception e) {
        logWarn "Could not parse time string '${timeIso}'. Using default."
        return defaultMinutes
    }
}

// Convert Date to minutes from midnight adhering explicitly to Hubitat's Location TimeZone
private int timeToMinutes(Date time) {
    TimeZone tz = location.timeZone ?: TimeZone.getDefault()
    Calendar cal = Calendar.getInstance(tz)
    cal.setTime(time)
    return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
}

// Mode Changer & Virtual Switch Synchronization
def changeMode(String newMode, String activePeriodKey = null) {
    if (!newMode) {
        logWarn "Target mode is null or empty. Skipping change."
        return
    }

    if (location.mode != newMode) {
        logInfo "Changing Hubitat Location Mode from '${location.mode}' to '${newMode}'"
        setLocationMode(newMode)
    } else {
        logDebug "Current mode is already '${newMode}'. No location mode shift needed."
    }
    
    // Always sync virtual switches and app label to reflect state
    updateVirtualModeSwitches(activePeriodKey)
    updateAppLabel(newMode)
}

def updateVirtualModeSwitches(String activePeriodKey) {
    // Collect all configured period-key-to-virtual-switch pairs
    List<Map> periodSwitchList = [
        [key: "sleeping",     vSwitch: vSwitchSleeping],
        [key: "weeHours",     vSwitch: vSwitchWeeHours],
        [key: "earlyMorning", vSwitch: vSwitchEarlyMorning],
        [key: "morning",      vSwitch: vSwitchMorning],
        [key: "day",          vSwitch: vSwitchDay],
        [key: "evening",      vSwitch: vSwitchEvening],
        [key: "lateEvening",  vSwitch: vSwitchLateEvening]
    ]

    periodSwitchList.each { entry ->
        def vSwitch = entry.vSwitch
        String periodKey = entry.key
        
        if (vSwitch) {
            if (activePeriodKey != null && periodKey == activePeriodKey) {
                if (vSwitch.currentValue("switch") != "on") {
                    logInfo "Turning ON Virtual Period Switch for '${periodKey}' (${vSwitch})"
                    vSwitch.on()
                }
            } else {
                if (vSwitch.currentValue("switch") != "off") {
                    logInfo "Turning OFF non-active Virtual Period Switch for '${periodKey}' (${vSwitch})"
                    vSwitch.off()
                }
            }
        }
    }
}

def updateAppLabel(String currentMode) {
    String baseLabel = "Mode Manager Advanced"
    String displayMode = currentMode ?: location.mode ?: "Unknown"
    String formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode}</span>]"
    
    if (app.label != formattedLabel) {
        logDebug "Updating App Label to: ${formattedLabel}"
        app.updateLabel(formattedLabel)
    }
}

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
}

private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "${app.label ?: 'Mode Manager Advanced'}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }