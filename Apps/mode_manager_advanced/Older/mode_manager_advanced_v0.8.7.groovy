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
 * 		2026          jshimota		0.0.2 - 0.7.9   See changelog.txt stored on Git
 *      2026-08-10    jshimota      0.8.0       Added uninstalled() cleanup handler; added collection null-safety across trigger switch iterators; optimized integer division in formatMinutes
 *      2026-08-10    jshimota      0.8.1       Updated Step 5 preference UI notice clarifying that WARN and ERROR logs remain permanently enabled
 *      2026-08-11    jshimota      0.8.2       Decoupled vSwitchSleeping from Override hijacking; enforced unidirectional Alexa Awake input listener; explicitly passed 'sleeping' period key during Voice sleep transitions
 *      2026-08-11    jshimota      0.8.3       Fixed reason hijacking by pre-stamping Voice state in alexaAwakeSwitchHandler and evaluateAndSetMode(); resolved duplicate ON commands to virtual switches
 *      2026-08-11    jshimota      0.8.4       Trying to fix Voice Transition labeling 
 *      2026-08-11    jshimota      0.8.5       Expanded Voice reason protection in vSwitchHandler to prevent virtual indicator sync events from hijacking Voice transitions to Override
 * 		2026-08-12	  jshimota      0.8.6		Added Notification Tile to Logging
 *      2026-08-12    jshimota      0.8.7       Added 'Mode stays' notification dispatching on recheck evaluations where active mode does not change
 *
 */

static String version() { return '0.8.7' }

definition(
    name: "Mode Manager Advanced",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Advanced Hubitat Mode Manager driven by master presence, reverse-mirrored presence and sleep/awake switches, dynamic time periods, virtual mode indicators, Alexa Mode sync, HSM control, and Dashboard Notification Tile outputs.",
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

        /* Step 5: Logging & Notifications */
        section("<b>STEP 5: Logging & System Diagnostics</b>") {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Logging Architecture:</b> Info logs state changes. Debug logging automatically disables after 30 minutes to reduce log noise.<br/>" +
                      "<i><b>Note:</b> WARN and ERROR logs are system-critical and always enabled.</i></div>"
            
            input name: "notificationDevice", type: "capability.notification", title: "<b>Notification Device (Notification Tile)</b>", required: false, multiple: true
            input name: "tileFormat", type: "enum", title: "<b>Notification Tile Output Format</b>", options: ["plain": "Plain Text", "html": "Formatted HTML"], defaultValue: "plain", required: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>"
            
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
    seedLoggingState()
    state.lastManageHSM = getSettingBool("manageHSM", true)
    checkTimePeriodSettingChanges()
    initialize()
}

/* Application Uninstalled */
def uninstalled() {
    logDebug "Uninstalled v${version()}. Cleaning up active subscriptions and scheduled jobs..."
    unsubscribe()
    unschedule()
}

/* Application Preferences Updated */
def updated() {
    checkLoggingChanges()
    checkHsmSettingChanges()
    checkTimePeriodSettingChanges()
    
    logDebug "Updated v${version()} with settings: ${settings}"
    logTrace "Unsubscribing from all active device subscriptions..."
    unsubscribe()
    
    unschedule("disableDebugLogging")
    if (getSettingBool("logDebugEnable", true)) {
        logDebug "Scheduling automatic disable of debug logging in 30 minutes (1800s)."
        runIn(1800, disableDebugLogging)
    }
    reschedulePeriods()
    initialize()
}

/* Safe Boolean Setting Converter */
private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    def val = settings[key]
    if (val == null) return defaultVal
    if (val instanceof Boolean) return val
    return val.toString().toBoolean()
}

/* Seed logging preferences into state for change tracking */
private void seedLoggingState() {
    state.lastLogInfoEnable  = getSettingBool("logInfoEnable", true)
    state.lastLogDebugEnable = getSettingBool("logDebugEnable", true)
    state.lastLogTraceEnable = getSettingBool("logTraceEnable", false)
}

/* Detect changes in HSM management preference, invalidating cached state on re-enablement */
private void checkHsmSettingChanges() {
    Boolean currentManageHSM = getSettingBool("manageHSM", true)
    Boolean lastManageHSM    = (state.lastManageHSM != null) ? state.lastManageHSM : currentManageHSM

    if (currentManageHSM != lastManageHSM) {
        String fromText = lastManageHSM ? "ENABLED" : "DISABLED"
        String toText   = currentManageHSM ? "ENABLED" : "DISABLED"
        logWarn "HSM Integration changed: ${fromText} -> ${toText}"
        if (currentManageHSM) {
            logTrace "HSM Management re-enabled. Invalidating cached state.lastHsmState to force fresh re-sync."
            state.lastHsmState = null
        }
    }
    state.lastManageHSM = currentManageHSM
}

/* Detect and log individual WARN messages per altered logging switch using ENABLED / DISABLED text */
private void checkLoggingChanges() {
    Boolean currentInfo  = getSettingBool("logInfoEnable", true)
    Boolean currentDebug = getSettingBool("logDebugEnable", true)
    Boolean currentTrace = getSettingBool("logTraceEnable", false)

    if (state.lastLogInfoEnable == null)  state.lastLogInfoEnable  = currentInfo
    if (state.lastLogDebugEnable == null) state.lastLogDebugEnable = currentDebug
    if (state.lastLogTraceEnable == null) state.lastLogTraceEnable = currentTrace

    Boolean lastInfo  = state.lastLogInfoEnable
    Boolean lastDebug = state.lastLogDebugEnable
    Boolean lastTrace = state.lastLogTraceEnable

    if (currentInfo != lastInfo) {
        logWarn "Info Logging changed: ${lastInfo ? 'ENABLED' : 'DISABLED'} -> ${currentInfo ? 'ENABLED' : 'DISABLED'}"
    }
    if (currentDebug != lastDebug) {
        logWarn "Debug Logging changed: ${lastDebug ? 'ENABLED' : 'DISABLED'} -> ${currentDebug ? 'ENABLED' : 'DISABLED'}"
    }
    if (currentTrace != lastTrace) {
        logWarn "Trace Logging changed: ${lastTrace ? 'ENABLED' : 'DISABLED'} -> ${currentTrace ? 'ENABLED' : 'DISABLED'}"
    }

    state.lastLogInfoEnable  = currentInfo
    state.lastLogDebugEnable = currentDebug
    state.lastLogTraceEnable = currentTrace
}

/* Check time period configuration settings for sequence overlaps on preference change only */
private void checkTimePeriodSettingChanges() {
    String currentConfigHash = "${timeWeeHours}|${timeEarlyMorning}|${timeMorning}|${timeDay}|${timeEvening}|${timeLateEvening}"
    
    if (state.lastPeriodConfigHash != currentConfigHash) {
        logTrace "Time period schedule configuration altered or initialized. Validating chronological sequence..."
        checkTimePeriodConfiguration()
        state.lastPeriodConfigHash = currentConfigHash
    }
}

/* Master Initialization Routine */
def initialize() {
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    /* Reset transient execution flags */
    state.pendingVoiceSleep = false
    state.isChangingAwakeForOverride = false
    state.lastTriggerTime = 0
    
    /* Preserve existing reservation maps across re-initialization if valid */
    if (!(state.pendingVSwitchSync instanceof Map)) {
        state.pendingVSwitchSync = [:]
    }
    if (!(state.pendingMirrorSync instanceof Map)) {
        state.pendingMirrorSync = [:]
    }
    
    if (!state.modeReason) {
        state.modeReason = "Normal"
    }

    /* Preserve existing state.lastHsmState or query native Hubitat HSM status if uninitialized */
    if (getSettingBool("manageHSM", true) && !state.lastHsmState) {
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
    
    /* External Trigger Switches Subscription (Null-safe collection cast) */
    (updateTriggerSwitch ? [updateTriggerSwitch].flatten() : []).each { dev ->
        logTrace "Subscribing to External Trigger Switch: ${dev.displayName}"
        subscribe(dev, "switch.on", updateSwitchHandler)
    }

    /* External Trigger Buttons Subscription (Null-safe collection cast) */
    (updateTriggerButton ? [updateTriggerButton].flatten() : []).each { dev ->
        logTrace "Subscribing to External Trigger Button: ${dev.displayName}"
        subscribe(dev, "pushed", updateButtonHandler)
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
    Map map = state.pendingVSwitchSync ? new HashMap(state.pendingVSwitchSync) : [:]
    
    String devId = device.id.toString()
    map[devId] = [command: command, expires: now() + 3000]
    state.pendingVSwitchSync = map
    
    logTrace "Queued pending VSwitch reservation '${command}' for '${device.displayName}' (ID: ${devId}, Expires: +3000ms)"
    device."${command}"()
}

/* Command Mirror Switch with Timed Reservation (3000ms TTL) */
private void sendMirrorCommand(def device, String command) {
    if (!device) return
    Map map = state.pendingMirrorSync ? new HashMap(state.pendingMirrorSync) : [:]
    
    String devId = device.id.toString()
    map[devId] = [command: command, expires: now() + 3000]
    state.pendingMirrorSync = map
    
    logTrace "Queued pending Mirror reservation '${command}' for '${device.displayName}' (ID: ${devId}, Expires: +3000ms)"
    device."${command}"()
}

/* Helper to validate and consume active reservation maps safely */
private boolean isReservationValid(String mapType, String devId, String value) {
    Map targetStateMap = (mapType == "VSwitch") ? state.pendingVSwitchSync : state.pendingMirrorSync
    if (!targetStateMap || !targetStateMap.containsKey(devId)) return false
    
    Map map = new HashMap(targetStateMap)
    Map reservation = map[devId]
    long currentMs = now()
    
    if (reservation?.expires && currentMs > (reservation.expires as Long)) {
        logTrace "Pruning expired reservation for device ID '${devId}'."
        map.remove(devId)
        if (mapType == "VSwitch") state.pendingVSwitchSync = map
        else state.pendingMirrorSync = map
        return false
    }
    
    if (reservation?.command == value) {
        map.remove(devId)
        if (mapType == "VSwitch") state.pendingVSwitchSync = map
        else state.pendingMirrorSync = map
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

                if (getSettingBool("manageHSM", true) && state.lastHsmState != "armAway") {
                    logDebug "Arming Hubitat Safety Monitor (HSM: armAway)"
                    sendLocationEvent(name: "hsmSetArm", value: "armAway")
                    state.lastHsmState = "armAway"
                }

                changeMode("Away", null, reason)
            } else {
                startPeriodSchedules()
                syncHomeState("on")

                if (getSettingBool("manageHSM", true) && state.lastHsmState != "disarm") {
                    logDebug "Disarming Hubitat Safety Monitor (HSM: disarm)"
                    sendLocationEvent(name: "hsmSetArm", value: "disarm")
                    state.lastHsmState = "disarm"
                }
                
                evaluateAndSetMode()
            }
            break

        case "Voice":
            String targetMode = params.targetMode
            String periodKey = params.periodKey
            if (targetMode) {
                changeMode(targetMode, periodKey, reason)
            } else {
                evaluateAndSetMode()
            }
            /* Force app label synchronization for Voice reason */
            updateAppLabel(location.mode, "Voice")
            break

        case "Normal - Rechecked":
            logTrace "Reason 'Normal - Rechecked' engaged. Holding provenance badge until next scheduled period CRON boundary."
            startPeriodSchedules()
            syncHomeState("on")
            
            String previousMode = location.mode
            evaluateAndSetMode()
            
            /* Dispatch recheck notification if mode stayed the same */
            if (location.mode == previousMode) {
                notifyModeRechecked(previousMode, "Normal - Rechecked")
            }
            break

        case "Normal":
            startPeriodSchedules()
            syncHomeState("on")
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

/* Ensure Awake and Sleep mirror states match target command ("on" = Awake, "off" = Sleeping) */
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
    /* Note: alexaAwakeSwitch is an input listener and is intentionally omitted here to prevent bidirectional loops */
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

    if (isReservationValid("VSwitch", deviceId, evt.value)) {
        logTrace "Ignoring internally generated VSwitch '${evt.value}' event from '${evt.device.displayName}' (ID: ${deviceId})."
        return
    }

    if (evt.value != "on") {
        logTrace "Ignoring non-ON value ('${evt.value}') from virtual switch '${evt.device.displayName}'."
        return
    }

    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 1000)) {
        logTrace "Ignoring rapid consecutive VSwitch override event from '${evt.device.displayName}'."
        return
    }
    state.lastTriggerTime = currentMs

    Map<String, Map> switchIdToPeriodMap = [:]
    if (vSwitchSleeping)     switchIdToPeriodMap[vSwitchSleeping.id.toString()]     = [mode: sleepMode?.toString(),        key: "sleeping"]
    if (vSwitchWeeHours)     switchIdToPeriodMap[vSwitchWeeHours.id.toString()]     = [mode: weeHoursMode?.toString(),     key: "weeHours"]
    if (vSwitchEarlyMorning) switchIdToPeriodMap[vSwitchEarlyMorning.id.toString()] = [mode: earlyMorningMode?.toString(), key: "earlyMorning"]
    if (vSwitchMorning)      switchIdToPeriodMap[vSwitchMorning.id.toString()]      = [mode: morningMode?.toString(),      key: "morning"]
    if (vSwitchDay)          switchIdToPeriodMap[vSwitchDay.id.toString()]          = [mode: dayMode?.toString(),          key: "day"]
    if (vSwitchEvening)      switchIdToPeriodMap[vSwitchEvening.id.toString()]      = [mode: eveningMode?.toString(),      key: "evening"]
    if (vSwitchLateEvening)  switchIdToPeriodMap[vSwitchLateEvening.id.toString()]  = [mode: lateEveningMode?.toString(),  key: "lateEvening"]

    Map targetPeriod = switchIdToPeriodMap[deviceId]
    logTrace "Mapped switch device ID '${evt.deviceId}' to target period: ${targetPeriod}"
    
    if (targetPeriod?.mode) {
        /* SPECIAL CASE: Suppress Override conversion when virtual switch turns ON during an active Voice transition */
        if (state.modeReason == "Voice") {
            logInfo "Period virtual switch '${evt.device.displayName}' turned ON during Voice transition. Preserving 'Voice' reason."
            updateVirtualModeSwitches(targetPeriod.key)
            updateAppLabel(location.mode, "Voice")
            return
        }

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
    if (isReservationValid("Mirror", devId, evt.value)) {
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
    if (isReservationValid("Mirror", devId, evt.value)) {
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
    if (isReservationValid("VSwitch", devId, evt.value)) {
        logTrace "Ignoring internally generated Alexa Mode Switch '${evt.value}' event."
        return
    }

    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Suppressing Alexa Mode switch toggle."
        return
    }

    logInfo "Alexa Mode Switch changed to value: ${evt.value}"
    String targetMode = (evt.value == "on") ? (getActiveTimePeriodInfo()?.mode ?: location.mode) : "Away"
    String targetKey = (evt.value == "on") ? getActiveTimePeriodInfo()?.key : null
    processReasonTransition("Voice", [targetMode: targetMode, periodKey: targetKey])
}

/* Awake Primary Reverse-Mirroring Handler */
def awakeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (isReservationValid("Mirror", devId, evt.value)) {
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
    if (isReservationValid("Mirror", devId, evt.value)) {
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

/* Alexa Awake Switch Handler (Unidirectional Input -> Authoritative Awake Switch) */
def alexaAwakeSwitchHandler(evt) {
    String devId = evt.deviceId.toString()
    if (isReservationValid("VSwitch", devId, evt.value)) {
        logTrace "Ignoring internally generated Alexa Awake Switch '${evt.value}' event."
        return
    }

    if (state.modeReason == "Override") {
        logInfo "System is in 'Override' reason state. Suppressing Alexa Awake switch toggle."
        return
    }

    logInfo "Alexa Awake Switch changed to value: ${evt.value}"
    
    /* Set state.modeReason IMMEDIATELY so child handlers pick up Voice */
    state.modeReason = "Voice"

    if (evt.value == "off") {
        syncAwakeState("off")
        processReasonTransition("Voice", [targetMode: sleepMode?.toString(), periodKey: "sleeping"])
    } else if (evt.value == "on") {
        syncAwakeState("on")
        String targetMode = getActiveTimePeriodInfo()?.mode ?: location.mode
        String targetKey = getActiveTimePeriodInfo()?.key
        processReasonTransition("Voice", [targetMode: targetMode, periodKey: targetKey])
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
    Boolean isHome = (homeSwitch?.currentValue("switch") == "on")
    logTrace "Presence Check: isHome = ${isHome}"
                     
    if (!isHome) {
        logDebug "Presence Matched: Away."
        
        if (getSettingBool("manageHSM", true) && state.lastHsmState != "armAway") {
            logDebug "Arming Hubitat Safety Monitor (HSM: armAway)"
            sendLocationEvent(name: "hsmSetArm", value: "armAway")
            state.lastHsmState = "armAway"
        }

        updateVirtualModeSwitches(null)
        changeMode("Away", null, state.modeReason ?: "Owntracks")
        return
    } else {
        if (getSettingBool("manageHSM", true) && state.lastHsmState != "disarm") {
            logDebug "Disarming Hubitat Safety Monitor (HSM: disarm)"
            sendLocationEvent(name: "hsmSetArm", value: "disarm")
            state.lastHsmState = "disarm"
        }
    }

    /* 3. Check Sleep State via Authoritative Awake Switch (OFF = Sleeping) */
    Boolean isSleeping = (awakeSwitch?.currentValue("switch") == "off")
    logTrace "Sleep State Check: isSleeping = ${isSleeping}"

    if (isSleeping) {
        logDebug "Sleep Matched: Home & Sleeping. Target Mode: '${sleepMode}'"
        String targetReason = (state.modeReason == "Voice") ? "Voice" : (state.modeReason ?: "Normal")
        changeMode(sleepMode?.toString(), "sleeping", targetReason)
        return
    }

    /* 4. Determine Scheduled Time Period Mode */
    logTrace "Calculating active time period block..."
    Map periodInfo = getActiveTimePeriodInfo()
    
    if (periodInfo?.mode) {
        logDebug "Scheduled Period Matched: Active Key: '${periodInfo.key}', Target Mode: '${periodInfo.mode}'"
        
        /* Preserve Normal - Rechecked or Voice if active; otherwise fall back to Normal schedule reason */
        String targetReason = (state.modeReason == "Normal - Rechecked") ? "Normal - Rechecked" : ((state.modeReason == "Voice") ? "Voice" : "Normal")
        changeMode(periodInfo.mode, periodInfo.key, targetReason)
    } else {
        logWarn "State Check: Unable to determine active time period mode."
    }
}

/* Active Time Period Calculator */
Map getActiveTimePeriodInfo() {
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
    int h = safeMinutes.intdiv(60)
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

    Boolean modeChanged = (location.mode != newMode)

    if (modeChanged) {
        logInfo "Changing Hubitat Location Mode from '${location.mode}' to '${newMode}' | Reason: ${state.modeReason}"
        setLocationMode(newMode)
        
        /* Dispatch Notification Tile Event on Mode Change */
        if (notificationDevice) {
            String msg = ""
            if (settings.tileFormat == "html") {
                msg = "<div style='font-size:13px; font-weight:bold; color:#27AE60;'>Mode is now ${newMode}</div><div style='font-size:11px; color:#7F8C8D; font-style:italic;'>Reason: ${state.modeReason}</div>"
            } else {
                msg = "Mode is now ${newMode} (Reason: ${state.modeReason})"
            }

            notificationDevice.each { device ->
                device.deviceNotification(msg)
            }
        }
    }
    
    /* Always sync virtual indicator switches and app label badge */
    updateVirtualModeSwitches(activePeriodKey)
    updateAppLabel(newMode, state.modeReason)
}

/* Recheck Notification Handler */
private void notifyModeRechecked(String currentMode, String reason) {
    if (!notificationDevice) return
    
    String msg = ""
    if (settings.tileFormat == "html") {
        msg = "<div style='font-size:13px; font-weight:bold; color:#2980B9;'>Mode stays ${currentMode}</div><div style='font-size:11px; color:#7F8C8D; font-style:italic;'>Reason: ${reason}</div>"
    } else {
        msg = "Mode stays ${currentMode} (Reason: ${reason})"
    }

    notificationDevice.each { device ->
        device.deviceNotification(msg)
    }
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
def updateAppLabel(String currentMode = null, String reason = null) {
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
    if (getSettingBool("logDebugEnable", true)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

/* Central Logging Router */
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appName = app.label ?: 'Mode Manager Advanced'
    
    if (lowerLevel == "warn") {
        log.warn "${appName} WARNING: ${msg}"
        return
    }
    if (lowerLevel == "error") {
        log.error "${appName} ERROR: ${msg}"
        return
    }
    
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    if (getSettingBool(settingKey, false)) {
        log."${lowerLevel}" "${appName}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }