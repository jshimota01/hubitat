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
 *      2026-06-01    jshimota      0.0.1       Initial release as Mode Manager Advanced
 *
 */

static String version() { return '0.0.1' }



definition(
    name: "Mode Manager Advanced",
    namespace: "jshimota",
    author: "J. Shimota",
    description: "Advanced Hubitat Mode Manager driven by time periods, presence, and sleep states.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Mode Manager Advanced Configuration", install: true, uninstall: true) {
        section("Presence Settings") {
            input name: "presenceSensors", type: "capability.presenceSensor", title: "Select Presence Sensor(s)", multiple: true, required: true, submitOnChange: true
            input name: "awayMode", type: "mode", title: "Mode when Away", required: true, defaultValue: "Away"
        }
        
        section("Sleep Settings") {
            input name: "sleepSwitch", type: "capability.switch", title: "Sleep State Switch (ON = Sleeping, OFF = Awake)", required: true, submitOnChange: true
            input name: "sleepMode", type: "mode", title: "Mode when Sleeping", required: true, defaultValue: "Night"
        }

        section("Time Period Settings") {
            input name: "period1Time", type: "time", title: "Period 1 Start Time", required: true
            input name: "period1Mode", type: "mode", title: "Period 1 Target Mode", required: true
            
            input name: "period2Time", type: "time", title: "Period 2 Start Time", required: false
            input name: "period2Mode", type: "mode", title: "Period 2 Target Mode", required: false
            
            input name: "period3Time", type: "time", title: "Period 3 Start Time", required: false
            input name: "period3Mode", type: "mode", title: "Period 3 Target Mode", required: false
        }

        section("Manual Override") {
            input name: "btnTrigger", type: "button", title: "Evaluate & Set Mode Now"
        }

        section("Logging") {
            input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    logDebug "Initializing Mode Manager Advanced..."
    
    // Event Subscriptions
    subscribe(presenceSensors, "presence", presenceHandler)
    subscribe(sleepSwitch, "switch", sleepHandler)
    
    // Schedule Time Periods
    if (period1Time) schedule(period1Time, period1Handler)
    if (period2Time) schedule(period2Time, period2Handler)
    if (period3Time) schedule(period3Time, period3Handler)
    
    // Initial evaluation on save/install
    evaluateAndSetMode()
}

def appButtonHandler(btn) {
    if (btn == "btnTrigger") {
        logDebug "Manual trigger button pressed."
        evaluateAndSetMode()
    }
}

def presenceHandler(evt) {
    logDebug "Presence changed: ${evt.device} is ${evt.value}"
    evaluateAndSetMode()
}

def sleepHandler(evt) {
    logDebug "Sleep switch changed: ${evt.value}"
    evaluateAndSetMode()
}

def period1Handler() { logDebug "Period 1 triggered"; evaluateAndSetMode() }
def period2Handler() { logDebug "Period 2 triggered"; evaluateAndSetMode() }
def period3Handler() { logDebug "Period 3 triggered"; evaluateAndSetMode() }

def evaluateAndSetMode() {
    // 1. Check Presence
    Boolean isAnyHome = presenceSensors.any { it.currentValue("presence") == "present" }
    
    if (!isAnyHome) {
        logDebug "State Check: Presence is Away."
        changeMode(awayMode)
        return
    }

    // 2. Check Sleep State
    Boolean isSleeping = (sleepSwitch.currentValue("switch") == "on")
    if (isSleeping) {
        logDebug "State Check: Home & Sleeping."
        changeMode(sleepMode)
        return
    }

    // 3. Determine Scheduled Time Period Mode
    String targetTimeMode = getActiveTimePeriodMode()
    logDebug "State Check: Home & Awake. Target time mode: ${targetTimeMode}"
    
    if (targetTimeMode) {
        changeMode(targetTimeMode)
    }
}

String getActiveTimePeriodMode() {
    // Logic to calculate which scheduled time slot the current time falls within
    // Returns period1Mode, period2Mode, or period3Mode based on current time
    Date now = new Date()
    // (Time comparison logic goes here)
    return period1Mode // Placeholder for current matching period
}

def changeMode(String newMode) {
    if (location.mode != newMode) {
        log.info "Mode Manager Advanced: Changing mode from '${location.mode}' to '${newMode}'"
        setLocationMode(newMode)
    } else {
        logDebug "Mode is already '${newMode}'. No change needed."
    }
}

def logDebug(msg) {
    if (logEnable) log.debug msg
}