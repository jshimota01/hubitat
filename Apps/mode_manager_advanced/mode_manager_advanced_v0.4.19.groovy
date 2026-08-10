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
 *      2026-08-07    jshimota      0.2.1       Added state tracking for HSM arm/disarm commands to avoid duplicate event calls; added override invalidation on presence/sleep changes
 *      2026-08-07    jshimota      0.2.2       Standardized presence and sleep architecture: homeSwitch and awakeSwitch act as primary authoritative inputs
 *      2026-08-07    jshimota      0.2.3       Added isSyncingVSwitches state guard in vSwitchHandler to prevent auto-sync events from triggering false manual overrides
 *      2026-08-07    jshimota      0.2.4       Wrapped updateVirtualModeSwitches in try-finally block to ensure sync guard flag is safely cleared
 *      2026-08-07    jshimota      0.2.5       Added try-catch validation in schedulePeriodTime to handle malformed or transient settings safely
 *      2026-08-07    jshimota      0.2.6       Removed silent fallback to morningMode; added warning logging when period mode resolution fails
 *      2026-08-07    jshimota      0.2.7       Updated logMessage() so warnings and errors are always logged regardless of preference settings
 *      2026-08-07    jshimota      0.2.8       Removed unused java.text.SimpleDateFormat import
 *      2026-08-07    jshimota      0.2.9       Updated state engine evaluation priority to: Manual Override > Away > Sleeping > Scheduled Time Period
 *      2026-08-07    jshimota      0.3.0       Added state.modeReason diagnostic engine for enhanced app list status display and troubleshooting logs
 *      2026-08-07    jshimota      0.3.1       Promoted trace logging for high-frequency internal events; added logTraceEnable preference toggle
 *      2026-08-07    jshimota      0.3.2       Fixed schedule overwrite bug by using schedule(timeDate, periodHandler) for each period
 *      2026-08-07    jshimota      0.3.3       Added subscription and handler for sleepSwitch so direct Alexa/manual toggles properly update awakeSwitch
 *      2026-08-07    jshimota      0.3.4       Added subscription and handler for awaySwitch so direct external toggles properly update homeSwitch
 *      2026-08-07    jshimota      0.3.5       Streamlined updateAppLabel HTML rendering to create a concise green status badge
 *      2026-08-07    jshimota      0.3.6       Cleared state.modeReason during manual rechecks to immediately drop (Override) from app label
 *      2026-08-08    jshimota      0.3.7       Added alexaSleepSwitch for Alexa Routine integration
 *      2026-08-08    jshimota      0.3.8       Expanded granular logTrace and logDebug checkpoints across all handlers and mathematical state methods
 *      2026-08-08    jshimota      0.3.9       Converted internal inline comments to block-style comments
 *      2026-08-08    jshimota      0.4.0       Standardized module block documentation and enhanced diagnostic logging
 *      2026-08-08    jshimota      0.4.1       Changed Alexa Sleeping Virtual Switch references to Alexa Awake Virtual Switch (alexaAwakeSwitch)
 *      2026-08-08    jshimota      0.4.2       Refactored state architecture to separate control state (state.modeSource) from log diagnostics (state.modeReason)
 *      2026-08-08    jshimota      0.4.3       Added state.isChangingAwakeForOverride transient guard flag to prevent race conditions during manual sleeping overrides
 *      2026-08-08    jshimota      0.4.4       Explicitly cleared state.modeSource across all handler resets to maintain strict state hygiene
 *      2026-08-08    jshimota      0.4.5       Cleaned up updateAppLabel rendering to prevent redundant [Sleeping (Sleep)] badge text
 *      2026-08-08    jshimota      0.4.6       Renamed validateTimePeriods to checkTimePeriodConfiguration for informational compliance
 *      2026-08-08    jshimota      0.4.7       Wrapped reverse-mirror switch commands in isSyncingVSwitches guard block to eliminate dual-firing race conditions
 *      2026-08-08    jshimota      0.4.8       Replaced global boolean sync guard with per-device event reservation maps (pendingVSwitchSync & pendingMirrorSync)
 *      2026-08-08    jshimota      0.4.9       Enforced VOICE modeSource attribution across alexaAwakeSwitchHandler for both wake/sleep transitions
 *      2026-08-08    jshimota      0.4.10      Added logInfo output to report state.modeReason updates
 *      2026-08-08    jshimota      0.4.11      Prevented vSwitchHandler from overwriting active VOICE mode reason when sleeping switch turns ON
 *      2026-08-08    jshimota      0.4.12      Extended VOICE modeSource protection in vSwitchHandler across all period switches transitioning from Sleeping
 *      2026-08-08    jshimota      0.4.13      Changed modeReason log output to logTrace; simplified Voice reason label text to 'Voice'
 *      2026-08-09    jshimota      0.4.16      Refactored mainPage UI layout with HTML banners, subheadings, and status block; upgraded triggers to multi-select switches and buttons
 *      2026-08-09    jshimota      0.4.17      Added help note under Step 4 trigger switches advising auto-off capability requirement
 *      2026-08-09    jshimota      0.4.18      Added logInfo output when external trigger switches or buttons fire mode evaluations
 *      2026-08-10    jshimota      0.4.19      Added execution debounce guards across trigger switch/button handlers and removed sendVSwitchCommand off reset to eliminate infinite loops
 *
 */

static String version() { return '0.4.19' }

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

/* =========================================================================================
   CONFIGURATION PAGE LAYOUT
   ========================================================================================= */
def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        
        /* App Title Banner & Active Status Card */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Mode Manager Advanced</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${version()}</span></div>"
            
            String currentMode = location.mode ?: "Unknown"
            String modeReason = state.modeReason ?: "Initialization / Idle"
            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #27AE60; padding:10px; border-radius:4px; font-size:13px;'>" +
                      "<b>Current Active Mode:</b> <span style='color:#27AE60; font-weight:bold;'>${currentMode}</span> &nbsp;|&nbsp; " +
                      "<b>Evaluation Reason:</b> <i>${modeReason}</i></div>"
        }

        /* Step 1: Presence */
        section("<b>STEP 1: Presence Architecture & State Switches</b>") {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Presence Architecture:</b> Master Presence Sensor updates Home Switch (Primary Authoritative Input). Away Switch is maintained as its inverse mirror.</div>"
            
            input name: "masterPresence", type: "capability.presenceSensor", title: "<b>Master Presence Sensor</b> <i>(OwnTracks - Jim)</i>", required: true, submitOnChange: true
            input name: "homeSwitch", type: "capability.switch", title: "<b>Home Switch</b> <i>(Primary Input)</i>", required: true, submitOnChange: true
            input name: "awaySwitch", type: "capability.switch", title: "<b>Away Switch</b> <i>(Inverse Mirror)</i>", required: true, submitOnChange: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Hubitat Safety Monitor (HSM) Integration</span>"
            input name: "manageHSM", type: "bool", title: "Control Hubitat Safety Monitor (HSM) based on Presence?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"
            input name: "alexaModeSwitch", type: "capability.switch", title: "Alexa Mode Virtual Switch <i>(ON = Home, OFF = Away)</i>", required: false
        }
        
        /* Step 2: Sleep */
        section("<b>STEP 2: Sleep Architecture & State Switches</b>") {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Sleep Architecture:</b> Awake Switch is the Primary Authoritative Input. Sleeping Switch is maintained as its inverse mirror.</div>"
            
            input name: "awakeSwitch", type: "capability.switch", title: "<b>Awake Switch</b> <i>(Primary Input: ON = Awake, OFF = Sleeping)</i>", required: true, submitOnChange: true
            input name: "sleepSwitch", type: "capability.switch", title: "<b>Sleeping Switch</b> <i>(Inverse Mirror)</i>", required: true, submitOnChange: true
            input name: "sleepMode", type: "mode", title: "<b>Target Mode when Sleeping</b>", required: true, defaultValue: "Sleeping"
            input name: "vSwitchSleeping", type: "capability.switch", title: "Virtual Switch for Sleeping Mode", required: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"
            input name: "alexaAwakeSwitch", type: "capability.switch", title: "Alexa Awake Virtual Switch <i>(Alexa Routines Sync)</i>", required: false
        }

        /* Step 3: Schedule */
        section("<b>STEP 3: Time Period & Target Mode Schedule</b>") {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Schedule Architecture:</b> Dynamic chronological time periods evaluated sequentially throughout the day when Home and Awake.</div>"
            
            paragraph "<table style='width:100%; text-align:left; border-collapse:collapse; background-color:#F2F4F4; border-radius:4px; font-size:12px;'>" +
                      "<tr style='border-bottom: 2px solid #BDC3C7; color:#34495E;'>" +
                      "<th style='padding:6px 8px;'>Period Block</th>" +
                      "<th style='padding:6px 8px;'>Start Time</th>" +
                      "<th style='padding:6px 8px;'>Target Mode</th>" +
                      "<th style='padding:6px 8px;'>Virtual Switch Indicator</th>" +
                      "</tr>" +
                      "</table>"

            input name: "timeWeeHours", type: "time", title: "Wee Hours Start", required: true, defaultValue: "00:30", width: 6
            input name: "weeHoursMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Night", width: 3
            input name: "vSwitchWeeHours", type: "capability.switch", title: "Virtual Indicator", required: false, width: 3
            
            input name: "timeEarlyMorning", type: "time", title: "Early Morning Start", required: true, defaultValue: "04:45", width: 6
            input name: "earlyMorningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Early Morning", width: 3
            input name: "vSwitchEarlyMorning", type: "capability.switch", title: "Virtual Indicator", required: false, width: 3
            
            input name: "timeMorning", type: "time", title: "Morning Start", required: true, defaultValue: "07:30", width: 6
            input name: "morningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Morning", width: 3
            input name: "vSwitchMorning", type: "capability.switch", title: "Virtual Indicator", required: false, width: 3
            
            input name: "timeDay", type: "time", title: "Day Start", required: true, defaultValue: "10:00", width: 6
            input name: "dayMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Day", width: 3
            input name: "vSwitchDay", type: "capability.switch", title: "Virtual Indicator", required: false, width: 3
            
            input name: "timeEvening", type: "time", title: "Evening Start", required: true, defaultValue: "17:00", width: 6
            input name: "eveningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Evening", width: 3
            input name: "vSwitchEvening", type: "capability.switch", title: "Virtual Indicator", required: false, width: 3
            
            input name: "timeLateEvening", type: "time", title: "Late Evening Start", required: true, defaultValue: "21:30", width: 6
            input name: "lateEveningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Late Evening", width: 3
            input name: "vSwitchLateEvening", type: "capability.switch", title: "Virtual Indicator", required: false, width: 3
        }

        /* Step 4: Overrides & Testing */
        section("<b>STEP 4: Manual Overrides & Triggers</b>") {
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "<b>Evaluation Logic Flow:</b> Manual Override &gt; Away &gt; Sleeping &gt; Scheduled Time Period</div>"
            
            input name: "updateTriggerSwitch", type: "capability.switch", title: "Switches to Trigger Evaluation / Update", required: false, multiple: true
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-8px; margin-bottom:10px;'>" +
                      "<i><b>Note:</b> Trigger switches must have an auto-off (momentary/auto-revert) setting enabled in their driver so they naturally return to OFF after firing.</i></div>"

            input name: "updateTriggerButton", type: "capability.pushableButton", title: "Buttons to Trigger Evaluation / Update", required: false, multiple: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Manual Evaluation & Test Trigger</span>"
            
            input name: "btnTrigger", type: "button", title: "Evaluate & Set Mode Now"
        }

        /* Step 5: Logging */
        section("<b>STEP 5: Logging & System Diagnostics</b>") {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Logging Architecture:</b> Info logs state changes. Debug logging automatically disables after 30 minutes to reduce log noise.</div>"
            
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
            input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false
        }
    }
}

/* =========================================================================================
   APPLICATION LIFECYCLE HANDLERS
   ========================================================================================= */

/* Application Installed */
def installed() {
    logDebug "Installed v${version()} with settings: ${settings}"
    initialize()
}

/* Application Preferences Updated */
def updated() {
    logDebug "Updated v${version()} with settings: ${settings}"
    logTrace "Unsubscribing from all active device subscriptions and clearing existing schedules..."
    unsubscribe()
    unschedule()
    if (logDebugEnable) {
        logDebug "Scheduling automatic disable of debug logging in 30 minutes (1800s)."
        runIn(1800, disableDebugLogging)
    }
    initialize()
}

/* Master Initialization Routine */
def initialize() {
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    /* Reset transient execution state flags and event reservation maps */
    state.lastHsmState = null
    state.pendingVoiceSleep = false
    state.isChangingAwakeForOverride = false
    state.lastTriggerTime = 0
    state.pendingVSwitchSync = [:]
    state.pendingMirrorSync = [:]
    logTrace "Reset execution state flags and reservation maps."
    
    /* Master Presence Subscription */
    if (masterPresence) {
        logTrace "Subscribing to Master Presence Sensor: ${masterPresence.displayName}"
        subscribe(masterPresence, "presence", presenceHandler)
    }
    
    /* Reverse-Mirrored Home and Away Presence Subscriptions */
    if (homeSwitch) {
        logTrace "Subscribing to Home Switch: ${homeSwitch.displayName}"
        subscribe(homeSwitch, "switch", homeSwitchHandler)
    }
    if (awaySwitch) {
        logTrace "Subscribing to Away Switch: ${awaySwitch.displayName}"
        subscribe(awaySwitch, "switch", awaySwitchHandler)
    }

    /* Reverse-Mirrored Awake and Sleep Subscriptions */
    if (awakeSwitch) {
        logTrace "Subscribing to Awake Switch: ${awakeSwitch.displayName}"
        subscribe(awakeSwitch, "switch", awakeSwitchHandler)
    }
    if (sleepSwitch) {
        logTrace "Subscribing to Sleep Switch: ${sleepSwitch.displayName}"
        subscribe(sleepSwitch, "switch", sleepSwitchHandler)
    }
    if (alexaAwakeSwitch) {
        logTrace "Subscribing to Alexa Awake Switch: ${alexaAwakeSwitch.displayName}"
        subscribe(alexaAwakeSwitch, "switch", alexaAwakeSwitchHandler)
    }
    
    /* External Trigger Switches Subscription */
    if (updateTriggerSwitch) {
        updateTriggerSwitch.each { dev ->
            logTrace "Subscribing to External Trigger Switch: ${dev.displayName}"
            subscribe(dev, "switch.on", updateSwitchHandler)
        }
    }

    /* External Trigger Buttons Subscription */
    if (updateTriggerButton) {
        updateTriggerButton.each { dev ->
            logTrace "Subscribing to External Trigger Button: ${dev.displayName}"
            subscribe(dev, "pushed", updateButtonHandler)
        }
    }

    /* Virtual Mode Switch Indicator Subscriptions (Manual Overrides) */
    [vSwitchSleeping, vSwitchWeeHours, vSwitchEarlyMorning, vSwitchMorning, vSwitchDay, vSwitchEvening, vSwitchLateEvening].each { vSwitch ->
        if (vSwitch) {
            logTrace "Subscribing to Virtual Mode Indicator Switch: ${vSwitch.displayName}"
            subscribe(vSwitch, "switch.on", vSwitchHandler)
        }
    }

    /* Schedule Dynamic Daily Time Period Transitions */
    reschedulePeriods()
    
    /* Initial State Assessment */
    logDebug "Running initial evaluateAndSetMode() upon app initialization..."
    evaluateAndSetMode()
}

/* =========================================================================================
   EVENT QUEUE & SWITCH RESERVATION HELPERS
   ========================================================================================= */

/* Command Virtual Mode Indicator Switch with Reservation */
private void sendVSwitchCommand(def device, String command) {
    if (!device) return
    if (state.pendingVSwitchSync == null) state.pendingVSwitchSync = [:]
    
    String devId = device.id.toString()
    state.pendingVSwitchSync[devId] = command
    logTrace "Queued pending VSwitch reservation '${command}' for '${device.displayName}' (ID: ${devId})"
    device."${command}"()
}

/* Command Mirror Switch with Reservation */
private void sendMirrorCommand(def device, String command) {
    if (!device) return
    if (state.pendingMirrorSync == null) state.pendingMirrorSync = [:]
    
    String devId = device.id.toString()
    state.pendingMirrorSync[devId] = command
    logTrace "Queued pending Mirror reservation '${command}' for '${device.displayName}' (ID: ${devId})"
    device."${command}"()
}

/* =========================================================================================
   SCHEDULING & CRON MANAGERS
   ========================================================================================= */

/* Reschedule Daily Recurring Timers */
def reschedulePeriods() {
    logDebug "Rescheduling daily recurring time period triggers based on updated settings..."
    
    schedulePeriodTime(timeWeeHours)
    schedulePeriodTime(timeEarlyMorning)
    schedulePeriodTime(timeMorning)
    schedulePeriodTime(timeDay)
    schedulePeriodTime(timeEvening)
    schedulePeriodTime(timeLateEvening)
}

/* Schedule Individual Period Boundary Trigger */
def schedulePeriodTime(String timeIso) {
    if (timeIso) {
        try {
            Date timeDate = toDateTime(timeIso)
            schedule(timeDate, periodHandler)
            logTrace "Successfully scheduled daily period trigger for time string '${timeIso}' -> Parsed Date: ${timeDate}"
        } catch (Exception e) {
            logWarn "Unable to schedule period trigger for time string '${timeIso}': ${e.message}"
        }
    } else {
        logTrace "Skipped schedulePeriodTime call for unconfigured time slot."
    }
}

/* Scheduled Cron Period Boundary Handler */
def periodHandler() {
    logTrace "Scheduled time period boundary hit. Clearing manual override flags."
    state.manualOverrideMode = null
    state.manualOverridePeriodKey = null
    state.modeReason = null
    state.modeSource = "SCHEDULED"
    evaluateAndSetMode()
}

/* =========================================================================================
   EVENT HANDLERS & DEVICE SUBSCRIPTIONS
   ========================================================================================= */

/* Manual App UI Button Handler */
def appButtonHandler(btn) {
    logTrace "appButtonHandler invoked by UI button event: '${btn}'"
    if (btn == "btnTrigger") {
        long currentMs = now()
        if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) {
            logTrace "Ignoring rapid consecutive UI button trigger."
            return
        }
        state.lastTriggerTime = currentMs

        logInfo "Mode recheck triggered manually via App UI button. Clearing manual override state."
        state.manualOverrideMode = null
        state.manualOverridePeriodKey = null
        state.modeReason = null
        state.modeSource = null
        evaluateAndSetMode()
    } else {
        logTrace "Unrecognized button press received: '${btn}'"
    }
}

/* External Trigger Switch Handler */
def updateSwitchHandler(evt) {
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) {
        logTrace "Ignoring rapid consecutive trigger switch event from '${evt.device.displayName}' to prevent loop."
        return
    }
    state.lastTriggerTime = currentMs

    logInfo "Mode recheck triggered via external trigger switch '${evt.device.displayName}' turning ON."
    state.manualOverrideMode = null
    state.manualOverridePeriodKey = null
    state.modeReason = null
    state.modeSource = null
    evaluateAndSetMode()
}

/* External Trigger Button Handler */
def updateButtonHandler(evt) {
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) {
        logTrace "Ignoring rapid consecutive button push from '${evt.device.displayName}'."
        return
    }
    state.lastTriggerTime = currentMs

    logInfo "Mode recheck triggered via external trigger button '${evt.device.displayName}' pushed (Button #${evt.value})."
    state.manualOverrideMode = null
    state.manualOverridePeriodKey = null
    state.modeReason = null
    state.modeSource = null
    evaluateAndSetMode()
}

/* Virtual Mode Indicator Switch Manual Toggle Handler */
def vSwitchHandler(evt) {
    logTrace "vSwitchHandler received event from device '${evt.device.displayName}' (ID: ${evt.deviceId}, Value: '${evt.value}')"

    String deviceId = evt.deviceId.toString()

    /* 1. Consume pending internal VSwitch reservation if present */
    if (state.pendingVSwitchSync?.containsKey(deviceId)) {
        String expectedState = state.pendingVSwitchSync[deviceId]
        if (evt.value == expectedState) {
            logTrace "Ignoring internally generated VSwitch '${evt.value}' event from '${evt.device.displayName}' (ID: ${deviceId})."
            state.pendingVSwitchSync.remove(deviceId)
            return
        }
    }

    /* 2. Defensive check to ignore non-ON events */
    if (evt.value != "on") {
        logTrace "Ignoring non-ON value ('${evt.value}') from virtual switch '${evt.device.displayName}'."
        return
    }

    /* Explicit mapping of Virtual Switch ID to Target Mode and Period Key */
    Map<String, Map> switchIdToPeriodMap = [:]
    if (vSwitchSleeping)     switchIdToPeriodMap[vSwitchSleeping.id.toString()]     = [mode: sleepMode?.toString(),        key: "sleeping"]
    if (vSwitchWeeHours)     switchIdToPeriodMap[vSwitchWeeHours.id.toString()]     = [mode: weeHoursMode?.toString(),     key: "weeHours"]
    if (vSwitchEarlyMorning) switchIdToPeriodMap[vSwitchEarlyMorning.id.toString()] = [mode: earlyMorningMode?.toString(), key: "earlyMorning"]
    if (vSwitchMorning)      switchIdToPeriodMap[vSwitchMorning.id.toString()]      = [mode: morningMode?.toString(),      key: "morning"]
    if (vSwitchDay)          switchIdToPeriodMap[vSwitchDay.id.toString()]          = [mode: dayMode?.toString(),          key: "day"]
    if (vSwitchEvening)      switchIdToPeriodMap[vSwitchEvening.id.toString()]      = [mode: eveningMode?.toString(),      key: "evening"]
    if (vSwitchLateEvening)  switchIdToPeriodMap[vSwitchLateEvening.id.toString()]  = [mode: lateEveningMode?.toString(),  key: "lateEvening"]

    Map targetPeriod = switchIdToPeriodMap[evt.deviceId.toString()]
    logTrace "Mapped switch device ID '${evt.deviceId}' to target period: ${targetPeriod}"
    
    if (targetPeriod?.mode) {
        /* Guard against overwriting an active Voice Commanded transition when matching period switch turns ON */
        if (state.modeSource == "VOICE" && location.mode == targetPeriod.mode) {
            logTrace "Switch '${evt.device.displayName}' turned ON while already in voice-commanded '${targetPeriod.mode}' mode. Preserving VOICE mode source."
            return
        }

        logInfo "Switch '${evt.device.displayName}' turned ON to manually override active period to '${targetPeriod.key}' (Mode: '${targetPeriod.mode}')."
        
        /* Save state override so state checks don't instantly overwrite manual choice */
        state.manualOverrideMode = targetPeriod.mode
        state.manualOverridePeriodKey = targetPeriod.key
        state.modeSource = "OVERRIDE"
        logDebug "Stored active manual override in state: Mode='${state.manualOverrideMode}', Key='${state.manualOverridePeriodKey}'"
        
        /* If turning on a daytime mode switch while awakeSwitch is OFF (sleeping), set Awake switch ON with transient override guard */
        if (vSwitchSleeping && evt.deviceId.toString() != vSwitchSleeping.id.toString() && awakeSwitch.currentValue("switch") == "off") {
            logDebug "Daytime mode switch forced manually while sleeping. Setting guard and forcing Awake switch '${awakeSwitch.displayName}' ON."
            state.isChangingAwakeForOverride = true
            awakeSwitch.on()
        }
        
        changeMode(targetPeriod.mode, targetPeriod.key, "Manual Override (${targetPeriod.key})")
    } else {
        logWarn "Mode recheck triggered by virtual switch '${evt.device.displayName}', but switch was not recognized in mode mapping."
    }
}

/* Primary Master Presence Event Handler */
def presenceHandler(evt) {
    logTrace "Master presence change detected: '${evt.device.displayName}' is now '${evt.value}'"
    
    /* Retain actual presence device display name for status rendering */
    state.presenceDeviceName = evt.device?.displayName ?: "Presence"
    logTrace "Captured presence device name: state.presenceDeviceName = '${state.presenceDeviceName}'"

    if (evt.value == "present") {
        if (homeSwitch.currentValue("switch") != "on") {
            logDebug "Master presence is present. Turning Home switch '${homeSwitch.displayName}' ON."
            homeSwitch.on()
        } else {
            logTrace "Home switch '${homeSwitch.displayName}' is already ON. No action taken."
        }
    } else {
        if (homeSwitch.currentValue("switch") != "off") {
            logDebug "Master presence is not present. Turning Home switch '${homeSwitch.displayName}' OFF."
            homeSwitch.off()
        } else {
            logTrace "Home switch '${homeSwitch.displayName}' is already OFF. No action taken."
        }
    }
}

/* Home Primary Reverse-Mirroring Handler (Authoritative Presence Input) */
def homeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (state.pendingMirrorSync?.containsKey(devId)) {
        String expectedState = state.pendingMirrorSync[devId]
        if (evt.value == expectedState) {
            logTrace "Ignoring internally generated Mirror switch '${evt.value}' event from '${evt.device.displayName}'."
            state.pendingMirrorSync.remove(devId)
            return
        }
    }

    logDebug "Home switch '${evt.device.displayName}' changed to value: ${evt.value}"
    
    /* Clear manual overrides when presence state changes */
    state.manualOverrideMode = null
    state.manualOverridePeriodKey = null
    state.modeReason = null
    state.modeSource = null
    logTrace "Presence change detected: Cleared all manual override state flags."

    if (evt.value == "on") {
        if (awaySwitch && awaySwitch.currentValue("switch") != "off") {
            logDebug "Home switch turned ON. Forcing Away switch '${awaySwitch.displayName}' OFF."
            sendMirrorCommand(awaySwitch, "off")
        }
    } else if (evt.value == "off") {
        if (awaySwitch && awaySwitch.currentValue("switch") != "on") {
            logDebug "Home switch turned OFF. Forcing Away switch '${awaySwitch.displayName}' ON."
            sendMirrorCommand(awaySwitch, "on")
        }
    }
    evaluateAndSetMode()
}

/* Away Switch Inverse Mirror Handler */
def awaySwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (state.pendingMirrorSync?.containsKey(devId)) {
        String expectedState = state.pendingMirrorSync[devId]
        if (evt.value == expectedState) {
            logTrace "Ignoring internally generated Mirror switch '${evt.value}' event from '${evt.device.displayName}'."
            state.pendingMirrorSync.remove(devId)
            return
        }
    }

    logDebug "Away switch '${evt.device.displayName}' changed to value: ${evt.value}"
    
    if (evt.value == "on") {
        if (homeSwitch && homeSwitch.currentValue("switch") != "off") {
            logDebug "Away switch turned ON externally. Forcing Home switch '${homeSwitch.displayName}' OFF."
            sendMirrorCommand(homeSwitch, "off")
        }
    } else if (evt.value == "off") {
        if (homeSwitch && homeSwitch.currentValue("switch") != "on") {
            logDebug "Away switch turned OFF externally. Forcing Home switch '${homeSwitch.displayName}' ON."
            sendMirrorCommand(homeSwitch, "on")
        }
    }
}

/* Awake Primary Reverse-Mirroring Handler (Authoritative Sleep Input) */
def awakeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (state.pendingMirrorSync?.containsKey(devId)) {
        String expectedState = state.pendingMirrorSync[devId]
        if (evt.value == expectedState) {
            logTrace "Ignoring internally generated Mirror switch '${evt.value}' event from '${evt.device.displayName}'."
            state.pendingMirrorSync.remove(devId)
            return
        }
    }

    logDebug "Awake switch '${evt.device.displayName}' changed to value: ${evt.value}"
    
    /* Clear manual overrides when sleep/awake state changes, unless triggered by an active manual override */
    if (state.isChangingAwakeForOverride == true) {
        logTrace "Awake state changed due to manual switch override. Preserving override state."
        state.isChangingAwakeForOverride = false
    } else {
        state.manualOverrideMode = null
        state.manualOverridePeriodKey = null
        state.modeReason = null
        if (state.modeSource != "VOICE") {
            state.modeSource = null
        }
        logTrace "Sleep/Awake change detected: Cleared manual override state flags."
    }

    /* Check and consume transient voice sleep flag */
    if (evt.value == "off" && state.pendingVoiceSleep == true) {
        state.modeSource = "VOICE"
        state.pendingVoiceSleep = false
        logDebug "Consumed transient pendingVoiceSleep flag -> state.modeSource set to 'VOICE'"
    }

    if (evt.value == "on") {
        if (sleepSwitch && sleepSwitch.currentValue("switch") != "off") {
            logDebug "Awake switch turned ON. Forcing Sleep switch '${sleepSwitch.displayName}' OFF."
            sendMirrorCommand(sleepSwitch, "off")
        }
    } else if (evt.value == "off") {
        if (sleepSwitch && sleepSwitch.currentValue("switch") != "on") {
            logDebug "Awake switch turned OFF. Forcing Sleep switch '${sleepSwitch.displayName}' ON."
            sendMirrorCommand(sleepSwitch, "on")
        }
    }
    evaluateAndSetMode()
}

/* Sleep Switch Inverse Mirror Handler */
def sleepSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (state.pendingMirrorSync?.containsKey(devId)) {
        String expectedState = state.pendingMirrorSync[devId]
        if (evt.value == expectedState) {
            logTrace "Ignoring internally generated Mirror switch '${evt.value}' event from '${evt.device.displayName}'."
            state.pendingMirrorSync.remove(devId)
            return
        }
    }

    logDebug "Sleep switch '${evt.device.displayName}' changed to value: ${evt.value}"
    
    if (evt.value == "on") {
        if (awakeSwitch && awakeSwitch.currentValue("switch") != "off") {
            logDebug "Sleep switch turned ON externally. Forcing Awake switch '${awakeSwitch.displayName}' OFF."
            sendMirrorCommand(awakeSwitch, "off")
        }
    } else if (evt.value == "off") {
        if (awakeSwitch && awakeSwitch.currentValue("switch") != "on") {
            logDebug "Sleep switch turned OFF externally. Forcing Awake switch '${awakeSwitch.displayName}' ON."
            sendMirrorCommand(awakeSwitch, "on")
        }
    }
}

/* Alexa Awake Switch Handler */
def alexaAwakeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (state.pendingVSwitchSync?.containsKey(devId)) {
        String expectedState = state.pendingVSwitchSync[devId]
        if (evt.value == expectedState) {
            logTrace "Ignoring internally generated Alexa Awake Switch '${evt.value}' event."
            state.pendingVSwitchSync.remove(devId)
            return
        }
    }

    if (evt.value != "on" && evt.value != "off") {
        logTrace "Ignoring non-switch event value ('${evt.value}') from Alexa Awake Switch."
        return
    }
    
    logDebug "Alexa Awake Switch '${evt.device.displayName}' changed to value: ${evt.value}"

    if (evt.value == "off") {
        logInfo "Alexa Awake Switch turned OFF via voice command."
        state.pendingVoiceSleep = true
        if (awakeSwitch && awakeSwitch.currentValue("switch") != "off") {
            awakeSwitch.off()
        } else {
            state.modeSource = "VOICE"
            state.pendingVoiceSleep = false
            evaluateAndSetMode()
        }
    } else if (evt.value == "on") {
        logInfo "Alexa Awake Switch turned ON via voice command."
        state.modeSource = "VOICE"
        if (awakeSwitch && awakeSwitch.currentValue("switch") != "on") {
            awakeSwitch.on()
        } else {
            evaluateAndSetMode()
        }
    }
}

/* =========================================================================================
   CORE STATE ENGINE & CALCULATION ROUTINES
   ========================================================================================= */

/* Core Decision Priority Engine */
def evaluateAndSetMode() {
    logTrace "Executing evaluateAndSetMode()..."

    /* 1. Check Manual Switch Override State (Highest Priority) */
    if (state.manualOverrideMode) {
        state.modeSource = "OVERRIDE"
        logDebug "State Priority 1: Active Manual Switch Override Mode found -> '${state.manualOverrideMode}' (Key: '${state.manualOverridePeriodKey}')"
        changeMode(state.manualOverrideMode, state.manualOverridePeriodKey, "Manual Override (${state.manualOverridePeriodKey})")
        return
    }

    /* 2. Check Presence via Authoritative Home Switch */
    Boolean isHome = (homeSwitch.currentValue("switch") == "on")
    logTrace "State Priority 2 Check: Presence isHome = ${isHome}"
                     
    if (!isHome) {
        logDebug "State Priority 2 Matched: Presence is Away."
        state.manualOverrideMode = null
        state.manualOverridePeriodKey = null
        state.modeSource = "PRESENCE"
        
        String deviceName = state.presenceDeviceName ?: masterPresence?.displayName ?: "Presence"
        state.modeReason = "Presence Away (${deviceName})"
        
        /* Sync Alexa Mode Switch -> OFF (Away) */
        if (alexaModeSwitch && alexaModeSwitch.currentValue("switch") != "off") {
            logDebug "Presence Away: Setting Alexa Mode Switch '${alexaModeSwitch.displayName}' to OFF"
            sendVSwitchCommand(alexaModeSwitch, "off")
        }
        
        /* Set HSM to Arm Away (only if state changed) */
        if (manageHSM) {
            if (state.lastHsmState != "armAway") {
                logDebug "Arming Hubitat Safety Monitor (HSM: armAway)"
                sendLocationEvent(name: "hsmSetArm", value: "armAway")
                state.lastHsmState = "armAway"
            } else {
                logTrace "HSM is already armed away (${state.lastHsmState}). Skipping duplicate HSM command."
            }
        }

        updateVirtualModeSwitches(null) /* Turn off period indicator switches when away */
        updateAppLabel("Away", state.modeReason)
        return
    } else {
        /* Sync Alexa Mode Switch -> ON (Home) */
        if (alexaModeSwitch && alexaModeSwitch.currentValue("switch") != "on") {
            logDebug "Presence Home: Setting Alexa Mode Switch '${alexaModeSwitch.displayName}' to ON"
            sendVSwitchCommand(alexaModeSwitch, "on")
        }

        /* Set HSM to Disarm (only if state changed) */
        if (manageHSM) {
            if (state.lastHsmState != "disarm") {
                logDebug "Disarming Hubitat Safety Monitor (HSM: disarm)"
                sendLocationEvent(name: "hsmSetArm", value: "disarm")
                state.lastHsmState = "disarm"
            } else {
                logTrace "HSM is already disarmed (${state.lastHsmState}). Skipping duplicate HSM command."
            }
        }
    }

    /* 3. Check Sleep State via Authoritative Awake Switch (OFF = Sleeping) */
    Boolean isSleeping = (awakeSwitch.currentValue("switch") == "off")
    logTrace "State Priority 3 Check: Sleep State isSleeping = ${isSleeping}"

    if (isSleeping) {
        if (state.modeSource != "VOICE") {
            state.modeSource = "SLEEP"
        }
        logDebug "State Priority 3 Matched: Home & Sleeping. Target Mode: '${sleepMode}' (Source: ${state.modeSource})"
        changeMode(sleepMode?.toString(), "sleeping", state.modeSource == "VOICE" ? "Voice" : "Sleeping")
        return
    }

    /* 4. Determine Scheduled Time Period Mode */
    logTrace "State Priority 4 Check: Calculating active time period block..."
    Map periodInfo = getActiveTimePeriodInfo()
    
    if (periodInfo?.mode) {
        if (state.modeSource != "VOICE") {
            state.modeSource = "SCHEDULED"
        }
        logDebug "State Priority 4 Matched: Home & Awake. Active Period Key: '${periodInfo.key}', Target Mode: '${periodInfo.mode}'"
        changeMode(periodInfo.mode, periodInfo.key, state.modeSource == "VOICE" ? "Voice" : "Scheduled Period (${periodInfo.key})")
    } else {
        logWarn "State Check: Unable to determine active time period mode. Skipping location mode change."
    }
}

/* Active Time Period Calculator */
Map getActiveTimePeriodInfo() {
    /* Perform informational check on configured boundary sequence */
    checkTimePeriodConfiguration()

    Date now = new Date()
    int currentMinutes = timeToMinutes(now)
    logTrace "getActiveTimePeriodInfo() evaluating time: ${now} (${currentMinutes} total minutes from midnight)"

    List<Map> periods = [
        [key: "weeHours",     mode: weeHoursMode?.toString(),     start: getMinutesFromSetting(timeWeeHours, 30)],
        [key: "earlyMorning", mode: earlyMorningMode?.toString(), start: getMinutesFromSetting(timeEarlyMorning, 285)],
        [key: "morning",      mode: morningMode?.toString(),      start: getMinutesFromSetting(timeMorning, 450)],
        [key: "day",          mode: dayMode?.toString(),          start: getMinutesFromSetting(timeDay, 600)],
        [key: "evening",      mode: eveningMode?.toString(),      start: getMinutesFromSetting(timeEvening, 1020)],
        [key: "lateEvening",  mode: lateEveningMode?.toString(),  start: getMinutesFromSetting(timeLateEvening, 1290)]
    ]

    logTrace "Unsorted time period array: ${periods}"
    periods.sort { it.start }
    logTrace "Chronologically sorted time period array: ${periods}"

    Map activePeriod = periods.reverse().find { currentMinutes >= it.start }

    if (!activePeriod) {
        logTrace "Current minute count (${currentMinutes}m) is before first period start time. Falling back to last night period."
        activePeriod = periods.last()
    }

    logTrace "Resolved active period info block: ${activePeriod}"
    return activePeriod ?: null
}

/* Informational Time Schedule Sequence Checker */
private void checkTimePeriodConfiguration() {
    List<Map> periods = [
        [name: "Wee Hours",     min: getMinutesFromSetting(timeWeeHours, 30)],
        [name: "Early Morning", min: getMinutesFromSetting(timeEarlyMorning, 285)],
        [name: "Morning",       min: getMinutesFromSetting(timeMorning, 450)],
        [name: "Day",           min: getMinutesFromSetting(timeDay, 600)],
        [name: "Evening",       min: getMinutesFromSetting(timeEvening, 1020)],
        [name: "Late Evening",  min: getMinutesFromSetting(timeLateEvening, 1290)]
    ]
    
    for (int i = 0; i < periods.size() - 1; i++) {
        logTrace "Validating boundary sequence: '${periods[i].name}' (${periods[i].min}m) vs '${periods[i+1].name}' (${periods[i+1].min}m)"
        if (periods[i].min >= periods[i+1].min) {
            logWarn "Time period configuration notice: '${periods[i].name}' (${formatMinutes(periods[i].min)}) starts at or after '${periods[i+1].name}' (${formatMinutes(periods[i+1].min)}). Chronological sorting will still resolve correctly, but please verify UI settings."
        }
    }
}

/* Format Minutes to String Time */
private String formatMinutes(int totalMinutes) {
    int safeMinutes = Math.max(0, totalMinutes)
    int h = safeMinutes / 60
    int m = safeMinutes % 60
    return String.format("%02d:%02d", h, m)
}

/* Safe Minute Extractor */
private int getMinutesFromSetting(String timeIso, int defaultMinutes) {
    if (!timeIso) {
        logTrace "Time setting string is null/empty. Using default boundary (${defaultMinutes}m)."
        return defaultMinutes
    }
    try {
        Date d = toDateTime(timeIso)
        int parsedMins = timeToMinutes(d)
        logTrace "Parsed ISO time string '${timeIso}' to ${parsedMins} minutes from midnight."
        return parsedMins
    } catch (Exception e) {
        logWarn "Could not parse time string '${timeIso}'. Using default boundary (${defaultMinutes}m). Error: ${e.message}"
        return defaultMinutes
    }
}

/* TimeZone Conversions */
private int timeToMinutes(Date time) {
    TimeZone tz = location.timeZone ?: TimeZone.getDefault()
    Calendar cal = Calendar.getInstance(tz)
    cal.setTime(time)
    int calculatedMinutes = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
    logTrace "Converted Date '${time}' to ${calculatedMinutes} total minutes using TimeZone '${tz.ID}'"
    return calculatedMinutes
}

/* =========================================================================================
   OUTPUT SYNCHRONIZATION & DISPLAY MANAGERS
   ========================================================================================= */

/* Location Mode Router */
def changeMode(String newMode, String activePeriodKey = null, String reason = null) {
    if (!newMode) {
        logWarn "Target mode is null or empty. Skipping location mode change execution."
        return
    }

    /* Save persistent diagnostic reason and log changes */
    if (reason) {
        if (state.modeReason != reason) {
            logTrace "Mode Reason updated: '${state.modeReason ?: 'None'}' -> '${reason}'"
            state.modeReason = reason
        }
        logTrace "Updated diagnostic reason in state: state.modeReason = '${state.modeReason}'"
    }

    if (location.mode != newMode) {
        logInfo "Changing Hubitat Location Mode from '${location.mode}' to '${newMode}' | Reason: ${state.modeReason ?: 'State Update'}"
        setLocationMode(newMode)
    } else {
        logTrace "Current location mode is already '${newMode}'. No location mode update needed. Reason: ${state.modeReason ?: 'State Update'}"
    }
    
    /* Always sync virtual switches and app label to reflect state */
    updateVirtualModeSwitches(activePeriodKey)
    updateAppLabel(newMode, state.modeReason)
}

/* Virtual Indicator Switch Synchronization Engine */
def updateVirtualModeSwitches(String activePeriodKey) {
    logTrace "updateVirtualModeSwitches invoked with activePeriodKey: '${activePeriodKey}'"
    
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
                    logTrace "Turning ON Virtual Period Switch for '${periodKey}' (${vSwitch.displayName})"
                    sendVSwitchCommand(vSwitch, "on")
                } else {
                    logTrace "Virtual Period Switch for '${periodKey}' (${vSwitch.displayName}) is already ON."
                }
            } else {
                if (vSwitch.currentValue("switch") != "off") {
                    logTrace "Turning OFF non-active Virtual Period Switch for '${periodKey}' (${vSwitch.displayName})"
                    sendVSwitchCommand(vSwitch, "off")
                } else {
                    logTrace "Virtual Period Switch for '${periodKey}' (${vSwitch.displayName}) is already OFF."
                }
            }
        }
    }

    /* Mirror Alexa Awake virtual switch state based on active awake state */
    if (alexaAwakeSwitch) {
        Boolean isAwake = (awakeSwitch?.currentValue("switch") == "on")
        logTrace "Checking Alexa Awake Virtual Switch sync state. isAwake calculated as: ${isAwake}"
        
        if (isAwake && alexaAwakeSwitch.currentValue("switch") != "on") {
            logTrace "Syncing Alexa Awake Virtual Switch '${alexaAwakeSwitch.displayName}' -> ON"
            sendVSwitchCommand(alexaAwakeSwitch, "on")
        } else if (!isAwake && alexaAwakeSwitch.currentValue("switch") != "off") {
            logTrace "Syncing Alexa Awake Virtual Switch '${alexaAwakeSwitch.displayName}' -> OFF"
            sendVSwitchCommand(alexaAwakeSwitch, "off")
        } else {
            logTrace "Alexa Awake Virtual Switch '${alexaAwakeSwitch.displayName}' already synchronized."
        }
    }
}

/* Dynamic HTML Apps List Badge Renderer */
def updateAppLabel(String currentMode, String reason = null) {
    String baseLabel = "Mode Manager Advanced"
    String displayMode = currentMode ?: location.mode ?: "Unknown"
    
    logTrace "updateAppLabel formatting badge: Mode='${displayMode}', Source='${state.modeSource}'"

    String shortReason = ""
    switch (state.modeSource) {
        case "PRESENCE":
            String deviceName = state.presenceDeviceName ?: masterPresence?.displayName ?: "Presence"
            shortReason = " (${deviceName})"
            break
            
        case "OVERRIDE":
            shortReason = " (Override)"
            break
            
        case "VOICE":
            shortReason = " (Voice)"
            break
            
        case "SLEEP":
        case "SCHEDULED":
        default:
            shortReason = ""
            break
    }

    String formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode}${shortReason}</span>]"
    
    if (app.label != formattedLabel) {
        logTrace "Updating Hubitat App Label to: ${formattedLabel}"
        app.updateLabel(formattedLabel)
    } else {
        logTrace "App Label is already up to date. Skipping app.updateLabel call."
    }
}

/* =========================================================================================
   SYSTEM LOGGING UTILITIES
   ========================================================================================= */

/* 30-Minute Debug Logging Auto-Disable Callback */
void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
}

/* Central Logging Router */
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    
    /* Always log warnings and errors regardless of preference settings */
    if (lowerLevel in ["warn", "error"]) {
        log."${lowerLevel}" "${app.label ?: 'Mode Manager Advanced'}${lowerLevel == 'warn' ? ' WARNING' : ' ERROR'}: ${msg}"
        return
    }
    
    /* Gate info, debug, and trace logs behind preference settings */
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    if (settings[settingKey] == true) {
        log."${lowerLevel}" "${app.label ?: 'Mode Manager Advanced'}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }