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
 *
 */

import java.text.SimpleDateFormat

static String version() { return '0.0.7' }

definition(
    name: "Mode Manager Advanced",
    namespace: "community",
    author: "J. Shimota",
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
            input name: "logEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
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
    initialize()
}

def initialize() {
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    // Master Presence Subscription
    if (masterPresence) {
        subscribe(masterPresence, "presence", presenceHandler)
    }
    
    // Reverse-Mirrored Home/Away Presence Subscriptions
    if (homeSwitch) subscribe(homeSwitch, "switch", homeSwitchHandler)
    if (awaySwitch) subscribe(awaySwitch, "switch", awaySwitchHandler)

    // Reverse-Mirrored Sleep/Awake Subscriptions
    if (sleepSwitch) subscribe(sleepSwitch, "switch", sleepSwitchHandler)
    if (awakeSwitch) subscribe(awakeSwitch, "switch", awakeSwitchHandler)
    
    // External Trigger Switch Subscription
    if (updateTriggerSwitch) subscribe(updateTriggerSwitch, "switch.on", updateSwitchHandler)

    // Schedule Dynamic Daily Time Period Transitions
    reschedulePeriods()
    
    // Initial State Check
    evaluateAndSetMode()
}

def reschedulePeriods() {
    logDebug "Rescheduling time period triggers based on updated settings..."
    
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
        schedule(timeDate, periodHandler)
        logDebug "Scheduled period trigger for: ${timeDate.format('HH:mm')}"
    }
}

// Manual App UI Button Handler
def appButtonHandler(btn) {
    if (btn == "btnTrigger") {
        logDebug "Manual trigger button clicked."
        evaluateAndSetMode()
    }
}

// External Trigger Switch Handler
def updateSwitchHandler(evt) {
    logDebug "Update switch triggered: ${evt.device} turned ON"
    evaluateAndSetMode()
    
    // Reset switch back to OFF so it behaves like a trigger/push-button
    if (updateTriggerSwitch && updateTriggerSwitch.currentValue("switch") != "off") {
        logDebug "Resetting trigger switch back to OFF"
        updateTriggerSwitch.off()
    }
}

// Presence Event Handler (OwnTracks Master)
def presenceHandler(evt) {
    logDebug "Master Presence changed: ${evt.device} is now ${evt.value}"
    if (evt.value == "present") {
        if (homeSwitch && homeSwitch.currentValue("switch") != "on") homeSwitch.on()
    } else {
        if (awaySwitch && awaySwitch.currentValue("switch") != "on") awaySwitch.on()
    }
    evaluateAndSetMode()
}

// Presence Reverse Mirroring Handlers
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

def awaySwitchHandler(evt) {
    logDebug "Away switch changed to ${evt.value}"
    if (evt.value == "on") {
        if (homeSwitch && homeSwitch.currentValue("switch") != "off") {
            logDebug "Away switch turned ON. Forcing Home switch OFF."
            homeSwitch.off()
        }
    } else if (evt.value == "off") {
        if (homeSwitch && homeSwitch.currentValue("switch") != "on") {
            logDebug "Away switch turned OFF. Forcing Home switch ON."
            homeSwitch.on()
        }
    }
    evaluateAndSetMode()
}

// Sleep Reverse Mirroring Handlers
def sleepSwitchHandler(evt) {
    logDebug "Sleep switch changed to ${evt.value}"
    if (evt.value == "on") {
        if (awakeSwitch && awakeSwitch.currentValue("switch") != "off") {
            logDebug "Sleep switch turned ON. Forcing Awake switch OFF."
            awakeSwitch.off()
        }
    } else if (evt.value == "off") {
        if (awakeSwitch && awakeSwitch.currentValue("switch") != "on") {
            logDebug "Sleep switch turned OFF. Forcing Awake switch ON."
            awakeSwitch.on()
        }
    }
    evaluateAndSetMode()
}

def awakeSwitchHandler(evt) {
    logDebug "Awake switch changed to ${evt.value}"
    if (evt.value == "on") {
        if (sleepSwitch && sleepSwitch.currentValue("switch") != "off") {
            logDebug "Awake switch turned ON. Forcing Sleep switch OFF."
            sleepSwitch.off()
        }
    } else if (evt.value == "off") {
        if (sleepSwitch && sleepSwitch.currentValue("switch") != "on") {
            logDebug "Sleep switch turned OFF. Forcing Sleep switch ON."
            sleepSwitch.on()
        }
    }
    evaluateAndSetMode()
}

// Scheduled Cron Event Handler
def periodHandler() {
    logDebug "Scheduled time period boundary hit."
    evaluateAndSetMode()
}

// Core State Engine
def evaluateAndSetMode() {
    // 1. Check Presence via Home/Away Switches & Master Sensor
    Boolean isHome = (homeSwitch && homeSwitch.currentValue("switch") == "on") || 
                     (!homeSwitch && masterPresence && masterPresence.currentValue("presence") == "present")
                     
    if (!isHome) {
        logDebug "State Check: Presence is Away."
        
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
        changeMode(sleepMode)
        return
    }

    // 3. Determine Scheduled Time Period Mode
    String targetTimeMode = getActiveTimePeriodMode()
    logDebug "State Check: Home & Awake. Calculated Time Period Target Mode: ${targetTimeMode}"
    
    if (targetTimeMode) {
        changeMode(targetTimeMode)
    }
}

// Calculate active time block using dynamically configured user start times
String getActiveTimePeriodMode() {
    Date now = new Date()
    int currentMinutes = timeToMinutes(now)

    int weeHoursStart     = getMinutesFromSetting(timeWeeHours, 30)       // Default: 00:30
    int earlyMorningStart = getMinutesFromSetting(timeEarlyMorning, 285)    // Default: 04:45
    int morningStart      = getMinutesFromSetting(timeMorning, 450)         // Default: 07:30
    int dayStart          = getMinutesFromSetting(timeDay, 600)             // Default: 10:00
    int eveningStart      = getMinutesFromSetting(timeEvening, 1020)        // Default: 17:00
    int lateEveningStart  = getMinutesFromSetting(timeLateEvening, 1290)     // Default: 21:30

    if (currentMinutes >= lateEveningStart || currentMinutes < weeHoursStart) {
        return lateEveningMode
    } else if (currentMinutes >= weeHoursStart && currentMinutes < earlyMorningStart) {
        return weeHoursMode
    } else if (currentMinutes >= earlyMorningStart && currentMinutes < morningStart) {
        return earlyMorningMode
    } else if (currentMinutes >= morningStart && currentMinutes < dayStart) {
        return morningMode
    } else if (currentMinutes >= dayStart && currentMinutes < eveningStart) {
        return dayMode
    } else if (currentMinutes >= eveningStart && currentMinutes < lateEveningStart) {
        return eveningMode
    }
    return morningMode
}

private int getMinutesFromSetting(String timeIso, int defaultMinutes) {
    if (!timeIso) return defaultMinutes
    try {
        Date d = toDateTime(timeIso)
        return timeToMinutes(d)
    } catch (Exception e) {
        log.warn "Could not parse time string '${timeIso}'. Using default."
        return defaultMinutes
    }
}

private int timeToMinutes(Date time) {
    Calendar cal = Calendar.getInstance()
    cal.setTime(time)
    return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
}

// Mode Changer & Virtual Switch Synchronization
def changeMode(String newMode) {
    if (!newMode) {
        log.warn "Mode Manager Advanced: Target mode is null or empty. Skipping change."
        return
    }

    if (location.mode != newMode) {
        log.info "Mode Manager Advanced: Changing Hubitat Location Mode from '${location.mode}' to '${newMode}'"
        setLocationMode(newMode)
    } else {
        logDebug "Mode Manager Advanced: Current mode is already '${newMode}'. No location mode shift needed."
    }
    
    // Always sync virtual switches to reflect state
    updateVirtualModeSwitches(newMode)
}

def updateVirtualModeSwitches(String activeMode) {
    Map<String, Object> modeVSwitchMap = [
        (sleepMode): vSwitchSleeping,
        (weeHoursMode): vSwitchWeeHours,
        (earlyMorningMode): vSwitchEarlyMorning,
        (morningMode): vSwitchMorning,
        (dayMode): vSwitchDay,
        (eveningMode): vSwitchEvening,
        (lateEveningMode): vSwitchLateEvening
    ]

    modeVSwitchMap.each { modeName, vSwitch ->
        if (vSwitch) {
            if (modeName == activeMode) {
                if (vSwitch.currentValue("switch") != "on") {
                    logDebug "Turning ON Virtual Mode Switch for: ${modeName}"
                    vSwitch.on()
                }
            } else {
                if (vSwitch.currentValue("switch") != "off") {
                    logDebug "Turning OFF Virtual Mode Switch for: ${modeName}"
                    vSwitch.off()
                }
            }
        }
    }
}

def logDebug(msg) {
    if (logEnable) log.debug "Mode Manager Advanced: ${msg}"
}