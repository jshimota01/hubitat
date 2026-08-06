/*
 * Mode Manager Advanced
 *  Improved Mode Manager that uses Presence and Sleeping in addiition to time periods
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
 *      2026-08-06    jshimota      0.0.2       Connected vritual switches to app
 *
 */


static String version() { return '0.0.2' }

definition(
    name: "Mode Manager Advanced",
    namespace: "community",
    author: "J. Shimota",
    description: "Advanced Hubitat Mode Manager driven by master presence, reverse-mirrored sleep/awake switches, custom time periods, and virtual mode indicators.",
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
        
        section("Presence Settings") {
            input name: "masterPresence", type: "capability.presenceSensor", title: "Master Presence Sensor (OwnTracks - Jim)", required: true, submitOnChange: true
            input name: "awayMode", type: "mode", title: "Target Mode when Away", required: true, defaultValue: "Away"
            input name: "vSwitchAway", type: "capability.switch", title: "Virtual Switch for Away Mode", required: false
        }
        
        section("Sleep / Awake State Switches (Reverse Mirrored)") {
            input name: "sleepSwitch", type: "capability.switch", title: "Sleeping Switch", required: true, submitOnChange: true
            input name: "awakeSwitch", type: "capability.switch", title: "Awake Switch", required: true, submitOnChange: true
            input name: "sleepMode", type: "mode", title: "Target Mode when Sleeping", required: true, defaultValue: "Sleeping"
            input name: "vSwitchSleeping", type: "capability.switch", title: "Virtual Switch for Sleeping Mode", required: false
        }

        section("Time Period Modes & Virtual Switches") {
            input name: "weeHoursMode", type: "mode", title: "Mode for Wee Hours (12:30 AM)", required: true, defaultValue: "Night"
            input name: "vSwitchWeeHours", type: "capability.switch", title: "Virtual Switch for Wee Hours Mode", required: false
            
            input name: "earlyMorningMode", type: "mode", title: "Mode for Early Morning (4:45 AM)", required: true, defaultValue: "Early Morning"
            input name: "vSwitchEarlyMorning", type: "capability.switch", title: "Virtual Switch for Early Morning Mode", required: false
            
            input name: "morningMode", type: "mode", title: "Mode for Morning (7:30 AM)", required: true, defaultValue: "Morning"
            input name: "vSwitchMorning", type: "capability.switch", title: "Virtual Switch for Morning Mode", required: false
            
            input name: "dayMode", type: "mode", title: "Mode for Day (10:00 AM)", required: true, defaultValue: "Day"
            input name: "vSwitchDay", type: "capability.switch", title: "Virtual Switch for Day Mode", required: false
            
            input name: "eveningMode", type: "mode", title: "Mode for Evening (5:00 PM)", required: true, defaultValue: "Evening"
            input name: "vSwitchEvening", type: "capability.switch", title: "Virtual Switch for Evening Mode", required: false
            
            input name: "lateEveningMode", type: "mode", title: "Mode for Late Evening (9:30 PM)", required: true, defaultValue: "Late Evening"
            input name: "vSwitchLateEvening", type: "capability.switch", title: "Virtual Switch for Late Evening Mode", required: false
        }

        section("Manual Override / Trigger") {
            input name: "btnTrigger", type: "button", title: "Evaluate & Set Mode Now"
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
    
    // Reverse-Mirrored Sleep/Awake Subscriptions
    if (sleepSwitch) subscribe(sleepSwitch, "switch", sleepSwitchHandler)
    if (awakeSwitch) subscribe(awakeSwitch, "switch", awakeSwitchHandler)
    
    // Schedule Daily Time Period Transitions
    schedule("0 30 0 * * ?", periodHandler)   // 12:30 AM (Wee Hours)
    schedule("0 45 4 * * ?", periodHandler)   // 04:45 AM (Early Morning)
    schedule("0 30 7 * * ?", periodHandler)   // 07:30 AM (Morning)
    schedule("0 0 10 * * ?", periodHandler)   // 10:00 AM (Day)
    schedule("0 0 17 * * ?", periodHandler)   // 05:00 PM (Evening)
    schedule("0 30 21 * * ?", periodHandler)  // 09:30 PM (Late Evening)
    
    // Initial State Check
    evaluateAndSetMode()
}

// Manual App UI Button Handler
def appButtonHandler(btn) {
    if (btn == "btnTrigger") {
        logDebug "Manual trigger button clicked."
        evaluateAndSetMode()
    }
}

// Presence Event Handler
def presenceHandler(evt) {
    logDebug "Master Presence changed: ${evt.device} is now ${evt.value}"
    evaluateAndSetMode()
}

// Reverse Mirroring Event Handlers
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
            logDebug "Awake switch turned OFF. Forcing Sleep switch ON."
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
    // 1. Check Master Presence State
    String currentPresence = masterPresence ? masterPresence.currentValue("presence") : "present"
    if (currentPresence != "present") {
        logDebug "State Check: Away (Master Presence = '${currentPresence}'). Target Mode: ${awayMode}"
        changeMode(awayMode)
        return
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

// Calculate active time block using minutes past midnight
String getActiveTimePeriodMode() {
    Date now = new Date()
    int currentMinutes = timeToMinutes(now)

    int weeHoursStart     = 30    // 12:30 AM (00:30)
    int earlyMorningStart = 285   // 04:45 AM (04:45)
    int morningStart      = 450   // 07:30 AM (07:30)
    int dayStart          = 600   // 10:00 AM (10:00)
    int eveningStart      = 1020  // 05:00 PM (17:00)
    int lateEveningStart  = 1290  // 09:30 PM (21:30)

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
        (awayMode): vSwitchAway,
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