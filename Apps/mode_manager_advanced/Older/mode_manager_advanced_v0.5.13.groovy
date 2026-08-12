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
 *      2026-08-10    jshimota      0.5.0       Refactored core state engine logic flow and explicit Reason handling (Override, Owntracks, Voice, Normal - Rechecked, Normal)
 *      2026-08-10    jshimota      0.5.1       Bug check fixes: Resolved Override CRON deadlock, recursive mirror loops, Voice target evaluation, and app badge formatting
 *      2026-08-10    jshimota      0.5.2       Added autoReturnNormal user option to allow staying in Override until manual recheck vs returning on next period
 *      2026-08-10    jshimota      0.5.3       Bug check fixes: Resolved stale modeReason persistence during Voice triggers and synchronized HSM arming state during Owntracks transitions
 *      2026-08-10    jshimota      0.5.4       Fixed app initialization state clobbering: evaluate existing physical device states and preserve Override/Away/HSM states across updates
 *      2026-08-10    jshimota      0.5.5       Converted alexaAwakeSwitchHandler to act purely as a state mirror input for awakeSwitch rather than forcing Voice reason mode changes
 *      2026-08-10    jshimota      0.5.6       Refined updateAppLabel badge renderer to accurately format Sleeping modes without clobbering governing Reason states
 *      2026-08-10    jshimota      0.5.7       Added 3000ms TTL expiration to pendingVSwitchSync and pendingMirrorSync reservation maps to prevent stale event suppression
 *      2026-08-10    jshimota      0.5.8       Clarified 'Normal - Rechecked' reason lifecycle: holds provenance badge until cleared by next scheduled period CRON boundary
 *      2026-08-10    jshimota      0.5.9       Fixed reason carry-over bug in evaluateAndSetMode: fallback to scheduled period resets reason to Normal or Normal - Rechecked
 *      2026-08-10    jshimota      0.5.10      Standardized App Label badge rendering to format Normal mode as 'Mode (Normal)'
 *      2026-08-10    jshimota      0.5.11      Centralized App Label rendering through changeMode() as the single standard update path
 *      2026-08-10    jshimota      0.5.12      Decoupled scheduling methods: introduced startPeriodSchedules() and stopPeriodSchedules() to isolate timer control
 *      2026-08-10    jshimota      0.5.13      Preserved state.lastHsmState across app updates and seeded initial status directly from location.hsmStatus
 *
 */

static String version() { return '0.5.13' }

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
        
        /* Force dynamic version retrieval on page render */
        String currentVersion = version()
        
        /* App Title Banner & Active Status Card */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Mode Manager Advanced</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion}</span></div>"
            
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

        /* Step 4: Overrides & Triggers */
        section("<b>STEP 4: Manual Overrides & Triggers</b>") {
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "<b>Evaluation Logic Flow:</b> Manual Override &gt; Owntracks &gt; Voice &gt; Normal - Rechecked &gt; Normal</div>"
            
            input name: "autoReturnNormal", type: "bool", title: "<b>Automatically return to Normal mode on next Period boundary?</b> (If OFF, system stays in Override until manual Recheck)", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>"

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
    logTrace "Unsubscribing from all active device subscriptions..."
    unsubscribe()
    
    if (logDebugEnable) {
        logDebug "Scheduling automatic disable of debug logging in 30 minutes (1800s)."
        runIn(1800, disableDebugLogging)
    }
    reschedulePeriods()
    initialize()
}

/* Master Initialization Routine */
def initialize() {
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    /* Reset transient execution flags and reservation maps */
    state.pendingVoiceSleep = false
    state.isChangingAwakeForOverride = false
    state.lastTriggerTime = 0
    state.pendingVSwitchSync = [:]
    state.pendingMirrorSync = [:]
    
    if (!state.modeReason) {
        state.modeReason = "Normal"
    }

    /* Preserve existing state.lastHsmState or query native Hubitat HSM status if uninitialized */
    if (manageHSM && !state.lastHsmState) {
        String currentHsm = location.hsmStatus ?: "unknown"
        logTrace "Seeding initial state.lastHsmState from native location.hsmStatus: '${currentHsm}'"
        state.lastHsmState = currentHsm
    }

    logTrace "Initialization starting with active modeReason: '${state.modeReason}' | lastHsmState: '${state.lastHsmState}'"
    
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

    /* Alexa Mode Switch Subscription */
    if (alexaModeSwitch) {
        logTrace "Subscribing to Alexa Mode Switch: ${alexaModeSwitch.displayName}"
        subscribe(alexaModeSwitch, "switch", alexaModeSwitchHandler)
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

    /* Re-establish period boundary schedules without forcing device states */
    if (state.modeReason == "Override" && settings.autoReturnNormal == false) {
        logTrace "Preserving active 'Override' state on init. Period schedules kept disabled."
        stopPeriodSchedules()
    } else {
        startPeriodSchedules()
    }

    /* Evaluate mode based on existing physical states rather than imposing Normal */
    logDebug "Evaluating existing physical device states upon initialization..."
    evaluateAndSetMode()
}

/* =========================================================================================
   EVENT QUEUE & SWITCH RESERVATION HELPERS
   ========================================================================================= */

/* Command Virtual Mode Indicator Switch with Timed Reservation (3000ms TTL) */
private void sendVSwitchCommand(def device, String command) {
    if (!device) return
    if (state.pendingVSwitchSync == null) state.pendingVSwitchSync = [:]
    
    String devId = device.id.toString()
    state.pendingVSwitchSync[devId] = [command: command, expires: now() + 3000]
    logTrace "Queued pending VSwitch reservation '${command}' for '${device.displayName}' (ID: ${devId}, Expires: +3000ms)"
    device."${command}"()
}

/* Command Mirror Switch with Timed Reservation (3000ms TTL) */
private void sendMirrorCommand(def device, String command) {
    if (!device) return
    if (state.pendingMirrorSync == null) state.pendingMirrorSync = [:]
    
    String devId = device.id.toString()
    state.pendingMirrorSync[devId] = [command: command, expires: now() + 3000]
    logTrace "Queued pending Mirror reservation '${command}' for '${device.displayName}' (ID: ${devId}, Expires: +3000ms)"
    device."${command}"()
}

/* Helper to validate and consume active reservation maps */
private boolean isReservationValid(Map map, String devId, String value) {
    if (map == null || !map.containsKey(devId)) return false
    
    Map reservation = map[devId]
    long currentMs = now()
    
    if (reservation?.expires && currentMs > (reservation.expires as Long)) {
        logTrace "Pruning expired reservation for device ID '${devId}'."
        map.remove(devId)
        return false
    }
    
    if (reservation?.command == value) {
        map.remove(devId)
        return true
    }
    
    return false
}

/* =========================================================================================
   SCHEDULING & CRON MANAGERS
   ========================================================================================= */

/* Full Reset for Preference Settings Updates */
def reschedulePeriods() {
    logDebug "Fully rescheduling time period triggers for preference settings change..."
    stopPeriodSchedules()
    startPeriodSchedules()
}

/* Start Active Period Boundary Schedules */
def startPeriodSchedules() {
    logTrace "Arming daily recurring time period CRON schedules..."
    schedulePeriodTime(timeWeeHours)
    schedulePeriodTime(timeEarlyMorning)
    schedulePeriodTime(timeMorning)
    schedulePeriodTime(timeDay)
    schedulePeriodTime(timeEvening)
    schedulePeriodTime(timeLateEvening)
}

/* Stop Active Period Boundary Schedules */
def stopPeriodSchedules() {
    logTrace "Unscheduling daily time period triggers..."
    unschedule("periodHandler")
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
    logTrace "Scheduled time period boundary hit."
    if (state.modeReason == "Override" && settings.autoReturnNormal == false) {
        logInfo "Period boundary hit but autoReturnNormal is disabled (Stay in Override). Preserving Override mode."
        return
    }
    
    logInfo "Period boundary hit. Reverting reason to Normal schedule mode."
    processReasonTransition("Normal")
}

/* =========================================================================================
   CORE REASON TRANSITION & STATE CONTROLLER
   ========================================================================================= */

/* Primary state router enforcing user transition rules based on Reason */
def processReasonTransition(String reason, Map params = [:]) {
    logInfo "Processing State Reason Transition -> Reason: '${reason}' | Params: ${params}"
    state.modeReason = reason

    switch (reason) {
        case "Override":
            if (settings.autoReturnNormal == false) {
                logTrace "autoReturnNormal is OFF. Disabling period schedules while in Override."
                stopPeriodSchedules()
            } else {
                logTrace "autoReturnNormal is ON. Period schedules remain active to clear Override at next period start."
                startPeriodSchedules()
            }
            
            syncHomeState("on")
            syncAwakeState("on")
            
            String targetMode = params.targetMode
            String periodKey = params.periodKey
            if (targetMode) {
                changeMode(targetMode, periodKey, reason)
            } else {
                updateAppLabel(location.mode, "Override")
            }
            break

        case "Owntracks":
            String presenceValue = params.presenceValue ?: (masterPresence?.currentValue("presence") == "present" ? "present" : "not present")
            
            if (presenceValue == "not present") {
                stopPeriodSchedules()
                syncHomeState("off")

                if (manageHSM && state.lastHsmState != "armAway") {
                    logDebug "Arming Hubitat Safety Monitor (HSM: armAway)"
                    sendLocationEvent(name: "hsmSetArm", value: "armAway")
                    state.lastHsmState = "armAway"
                }

                changeMode("Away", null, reason)
            } else {
                startPeriodSchedules()
                syncHomeState("on")

                if (manageHSM && state.lastHsmState != "disarm") {
                    logDebug "Disarming Hubitat Safety Monitor (HSM: disarm)"
                    sendLocationEvent(name: "hsmSetArm", value: "disarm")
                    state.lastHsmState = "disarm"
                }
                
                evaluateAndSetMode()
            }
            break

        case "Voice":
            String targetMode = params.targetMode
            if (targetMode) {
                changeMode(targetMode, null, reason)
            } else {
                evaluateAndSetMode()
            }
            break

        case "Normal - Rechecked":
            logTrace "Reason 'Normal - Rechecked' engaged. Holding provenance badge until next scheduled period CRON boundary."
            startPeriodSchedules()
            syncHomeState("on")
            evaluateAndSetMode()
            break

        case "Normal":
            startPeriodSchedules()
            syncHomeState("on")
            syncAwakeState("on")
            evaluateAndSetMode()
            break
            
        default:
            logWarn "Unrecognized reason transition requested: '${reason}'"
            evaluateAndSetMode()
            break
    }
}

/* Ensure Home and Alexa Home states match target command ("on" = Home, "off" = Away) */
private void syncHomeState(String targetState) {
    if (homeSwitch && homeSwitch.currentValue("switch") != targetState) {
        sendMirrorCommand(homeSwitch, targetState)
    }
    if (awaySwitch) {
        String awayTarget = (targetState == "on") ? "off" : "on"
        if (awaySwitch.currentValue("switch") != awayTarget) {
            sendMirrorCommand(awaySwitch, awayTarget)
        }
    }
    if (alexaModeSwitch && alexaModeSwitch.currentValue("switch") != targetState) {
        sendVSwitchCommand(alexaModeSwitch, targetState)
    }
}

/* Ensure Awake, Sleep, and Alexa Awake states match target command ("on" = Awake, "off" = Sleeping) */
private void syncAwakeState(String targetState) {
    if (awakeSwitch && awakeSwitch.currentValue("switch") != targetState) {
        sendMirrorCommand(awakeSwitch, targetState)
    }
    if (sleepSwitch) {
        String sleepTarget = (targetState == "on") ? "off" : "on"
        if (sleepSwitch.currentValue("switch") != sleepTarget) {
            sendMirrorCommand(sleepSwitch, sleepTarget)
        }
    }
    if (alexaAwakeSwitch && alexaAwakeSwitch.currentValue("switch") != targetState) {
        sendVSwitchCommand(alexaAwakeSwitch, targetState)
    }
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

        processReasonTransition("Normal - Rechecked")
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
    processReasonTransition("Normal - Rechecked")
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
    processReasonTransition("Normal - Rechecked")
}

/* Virtual Mode Indicator Switch Manual Toggle Handler */
def vSwitchHandler(evt) {
    logTrace "vSwitchHandler received event from device '${evt.device.displayName}' (ID: ${evt.deviceId}, Value: '${evt.value}')"

    String deviceId = evt.deviceId.toString()

    if (isReservationValid(state.pendingVSwitchSync, deviceId, evt.value)) {
        logTrace "Ignoring internally generated VSwitch '${evt.value}' event from '${evt.device.displayName}' (ID: ${deviceId})."
        return
    }

    if (evt.value != "on") {
        logTrace "Ignoring non-ON value ('${evt.value}') from virtual switch '${evt.device.displayName}'."
        return
    }

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
        logInfo "Period virtual switch '${evt.device.displayName}' triggered externally."
        processReasonTransition("Override", [targetMode: targetPeriod.mode, periodKey: targetPeriod.key])
    } else {
        logWarn "Mode recheck triggered by virtual switch '${evt.device.displayName}', but switch was not recognized in mode mapping."
    }
}

/* Primary Master Presence Event Handler */
def presenceHandler(evt) {
    logTrace "Master presence change detected: '${evt.device.displayName}' is now '${evt.value}'"
    
    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Ignoring master presence toggle."
        return
    }

    processReasonTransition("Owntracks", [presenceValue: evt.value])
}

/* Home Primary Reverse-Mirroring Handler */
def homeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (isReservationValid(state.pendingMirrorSync, devId, evt.value)) {
        logTrace "Ignoring internally generated Mirror switch '${evt.value}' event from '${evt.device.displayName}'."
        return
    }

    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Suppressing external home switch toggle."
        return
    }

    logDebug "Home switch '${evt.device.displayName}' changed to value: ${evt.value}"
    String presenceVal = (evt.value == "on") ? "present" : "not present"
    processReasonTransition("Owntracks", [presenceValue: presenceVal])
}

/* Away Switch Inverse Mirror Handler */
def awaySwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (isReservationValid(state.pendingMirrorSync, devId, evt.value)) {
        logTrace "Ignoring internally generated Mirror switch '${evt.value}' event from '${evt.device.displayName}'."
        return
    }

    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Suppressing external away switch toggle."
        return
    }

    logDebug "Away switch '${evt.device.displayName}' changed to value: ${evt.value}"
    String presenceVal = (evt.value == "off") ? "present" : "not present"
    processReasonTransition("Owntracks", [presenceValue: presenceVal])
}

/* Alexa Mode Virtual Switch Handler */
def alexaModeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (isReservationValid(state.pendingVSwitchSync, devId, evt.value)) {
        logTrace "Ignoring internally generated Alexa Mode Switch '${evt.value}' event."
        return
    }

    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Suppressing Alexa Mode switch toggle."
        return
    }

    logInfo "Alexa Mode Switch changed to value: ${evt.value}"
    String targetMode = (evt.value == "on") ? (getActiveTimePeriodInfo()?.mode ?: location.mode) : "Away"
    processReasonTransition("Voice", [targetMode: targetMode])
}

/* Awake Primary Reverse-Mirroring Handler */
def awakeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (isReservationValid(state.pendingMirrorSync, devId, evt.value)) {
        logTrace "Ignoring internally generated Mirror switch '${evt.value}' event from '${evt.device.displayName}'."
        return
    }

    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Suppressing external awake switch toggle."
        return
    }

    logDebug "Awake switch '${evt.device.displayName}' changed to value: ${evt.value}"
    evaluateAndSetMode()
}

/* Sleep Switch Inverse Mirror Handler */
def sleepSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (isReservationValid(state.pendingMirrorSync, devId, evt.value)) {
        logTrace "Ignoring internally generated Mirror switch '${evt.value}' event from '${evt.device.displayName}'."
        return
    }

    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Suppressing external sleep switch toggle."
        return
    }

    logDebug "Sleep switch '${evt.device.displayName}' changed to value: ${evt.value}"
    evaluateAndSetMode()
}

/* Alexa Awake Switch Handler (Mirrors state to authoritative awakeSwitch) */
def alexaAwakeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (isReservationValid(state.pendingVSwitchSync, devId, evt.value)) {
        logTrace "Ignoring internally generated Alexa Awake Switch '${evt.value}' event."
        return
    }

    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Suppressing Alexa Awake switch toggle."
        return
    }

    logInfo "Alexa Awake Switch changed to value: ${evt.value}"
    if (evt.value == "off") {
        if (awakeSwitch && awakeSwitch.currentValue("switch") != "off") {
            logDebug "Alexa Awake OFF -> Setting primary Awake Switch '${awakeSwitch.displayName}' to OFF"
            awakeSwitch.off()
        }
    } else if (evt.value == "on") {
        if (awakeSwitch && awakeSwitch.currentValue("switch") != "on") {
            logDebug "Alexa Awake ON -> Setting primary Awake Switch '${awakeSwitch.displayName}' to ON"
            awakeSwitch.on()
        }
    }
}

/* =========================================================================================
   CORE STATE ENGINE & CALCULATION ROUTINES
   ========================================================================================= */

/* Core Decision Priority Engine */
def evaluateAndSetMode() {
    logTrace "Executing evaluateAndSetMode()..."

    /* 1. If in Override, do not evaluate normal flow */
    if (state.modeReason == "Override") {
        logDebug "State Engine: System locked in 'Override'. Preserving current mode '${location.mode}'."
        return
    }

    /* 2. Check Presence via Authoritative Home Switch */
    Boolean isHome = (homeSwitch.currentValue("switch") == "on")
    logTrace "Presence Check: isHome = ${isHome}"
                     
    if (!isHome) {
        logDebug "Presence Matched: Away."
        
        if (manageHSM && state.lastHsmState != "armAway") {
            logDebug "Arming Hubitat Safety Monitor (HSM: armAway)"
            sendLocationEvent(name: "hsmSetArm", value: "armAway")
            state.lastHsmState = "armAway"
        }

        updateVirtualModeSwitches(null)
        changeMode("Away", null, state.modeReason ?: "Owntracks")
        return
    } else {
        if (manageHSM && state.lastHsmState != "disarm") {
            logDebug "Disarming Hubitat Safety Monitor (HSM: disarm)"
            sendLocationEvent(name: "hsmSetArm", value: "disarm")
            state.lastHsmState = "disarm"
        }
    }

    /* 3. Check Sleep State via Authoritative Awake Switch (OFF = Sleeping) */
    Boolean isSleeping = (awakeSwitch.currentValue("switch") == "off")
    logTrace "Sleep State Check: isSleeping = ${isSleeping}"

    if (isSleeping) {
        logDebug "Sleep Matched: Home & Sleeping. Target Mode: '${sleepMode}'"
        changeMode(sleepMode?.toString(), "sleeping", state.modeReason ?: "Normal")
        return
    }

    /* 4. Determine Scheduled Time Period Mode */
    logTrace "Calculating active time period block..."
    Map periodInfo = getActiveTimePeriodInfo()
    
    if (periodInfo?.mode) {
        logDebug "Scheduled Period Matched: Active Key: '${periodInfo.key}', Target Mode: '${periodInfo.mode}'"
        
        /* Preserve Normal - Rechecked if active; otherwise fall back to Normal schedule reason */
        String targetReason = (state.modeReason == "Normal - Rechecked") ? "Normal - Rechecked" : "Normal"
        changeMode(periodInfo.mode, periodInfo.key, targetReason)
    } else {
        logWarn "State Check: Unable to determine active time period mode."
    }
}

/* Active Time Period Calculator */
Map getActiveTimePeriodInfo() {
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

    periods.sort { it.start }
    Map activePeriod = periods.reverse().find { currentMinutes >= it.start }

    if (!activePeriod) {
        activePeriod = periods.last()
    }

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
        if (periods[i].min >= periods[i+1].min) {
            logWarn "Time period configuration notice: '${periods[i].name}' (${formatMinutes(periods[i].min)}) starts at or after '${periods[i+1].name}' (${formatMinutes(periods[i+1].min)})."
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
    if (!timeIso) return defaultMinutes
    try {
        Date d = toDateTime(timeIso)
        return timeToMinutes(d)
    } catch (Exception e) {
        logWarn "Could not parse time string '${timeIso}'. Using default (${defaultMinutes}m)."
        return defaultMinutes
    }
}

/* TimeZone Conversions */
private int timeToMinutes(Date time) {
    TimeZone tz = location.timeZone ?: TimeZone.getDefault()
    Calendar cal = Calendar.getInstance(tz)
    cal.setTime(time)
    return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
}

/* =========================================================================================
   OUTPUT SYNCHRONIZATION & DISPLAY MANAGERS
   ========================================================================================= */

/* Location Mode Router */
def changeMode(String newMode, String activePeriodKey = null, String reason = null) {
    if (!newMode) {
        logWarn "Target mode is null or empty. Skipping mode change."
        return
    }

    if (reason) {
        state.modeReason = reason
    }

    if (location.mode != newMode) {
        logInfo "Changing Hubitat Location Mode from '${location.mode}' to '${newMode}' | Reason: ${state.modeReason}"
        setLocationMode(newMode)
    }
    
    /* Always sync virtual indicator switches and app label badge */
    updateVirtualModeSwitches(activePeriodKey)
    updateAppLabel(newMode, state.modeReason)
}

/* Virtual Indicator Switch Synchronization Engine */
def updateVirtualModeSwitches(String activePeriodKey) {
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
                    sendVSwitchCommand(vSwitch, "on")
                }
            } else {
                if (vSwitch.currentValue("switch") != "off") {
                    sendVSwitchCommand(vSwitch, "off")
                }
            }
        }
    }
}

/* Dynamic HTML Apps List Badge Renderer */
def updateAppLabel(String currentMode, String reason = null) {
    String baseLabel = "Mode Manager Advanced v${version()}"
    String displayMode = currentMode ?: location.mode ?: "Unknown"
    String currentReason = reason ?: state.modeReason ?: "Normal"

    String formattedLabel = ""

    switch (currentReason) {
        case "Override":
            formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode} (Override)</span>]"
            break
            
        case "Owntracks":
            if (displayMode.equalsIgnoreCase("Away")) {
                formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>Away (Owntracks)</span>]"
            } else {
                formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode} (Owntracks)</span>]"
            }
            break
            
        case "Normal - Rechecked":
            formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode} (Normal - Rechecked)</span>]"
            break
            
        case "Voice":
            formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode} (Voice)</span>]"
            break
            
        case "Normal":
        default:
            formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode} (Normal)</span>]"
            break
    }
    
    if (app.label != formattedLabel) {
        logTrace "Updating Hubitat App Label to: ${formattedLabel}"
        app.updateLabel(formattedLabel)
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
    
    if (lowerLevel in ["warn", "error"]) {
        log."${lowerLevel}" "${app.label ?: 'Mode Manager Advanced'}${lowerLevel == 'warn' ? ' WARNING' : ' ERROR'}: ${msg}"
        return
    }
    
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