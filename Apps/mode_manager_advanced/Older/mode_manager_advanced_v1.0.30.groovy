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
 *  v1.0.30 (2026-08-13) - Override Release Priority Preemption Fix:
 *                         - Corrected calculateDecision() hierarchy logic so that when holdOverride is OFF, lower-priority triggers (Normal/Voice) can clear an active Override state as intended.
 *  v1.0.29 (2026-08-13) - Manual UI Button Override Release.
 *  v1.0.28 (2026-08-13) - Simulation Engine Pipeline Injection.
 *  v1.0.27 (2026-08-13) - Decoupled Override Persistence & Scheduler Controls.
 *  v1.0.26 (2026-08-13) - Formal Authority Hierarchy Engine (Override > Presence > Voice > Normal).
 *  v1.0.25 (2026-08-13) - Voice/Override Reason Retention Guard.
 *  v1.0.24 (2026-08-13) - Asynchronous Output Event Suppression Buffer.
 *  v1.0.1 (2026-08-13)  - Full Architectural Refactor: Linear decision pipeline, transaction output isolation, and infrastructure scheduler.
 *
 */

static String version() { return '1.0.30' }

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
        section("<b>STEP 1: Presence Architecture & State Switches</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Presence Architecture:</b> Master Presence Sensor updates Home Switch (Primary Authoritative Input). Away Switch is maintained as its inverse mirror.</div>"
            
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Presence Modes:</b> When presence is <b>Away</b>, Location Mode automatically switches to <b>Away</b>. When presence returns to <b>Home</b>, the app automatically evaluates your active Step 3 Time Period schedule (or Step 2 Sleep state). No manual mode assignment is needed for presence.</div>"

            input name: "masterPresence", type: "capability.presenceSensor", title: "<b>Master Presence Sensor</b> <i>(OwnTracks - Jim)</i>", required: true, submitOnChange: true
            input name: "homeSwitch", type: "capability.switch", title: "<b>Home Switch</b> <i>(Primary Input)</i>", required: true, submitOnChange: true
            input name: "awaySwitch", type: "capability.switch", title: "<b>Away Switch</b> <i>(Inverse Mirror)</i>", required: true, submitOnChange: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Hubitat Safety Monitor (HSM) Integration</span>"
            
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>HSM Automation Logic:</b> When enabled, this app manages your Hubitat Safety Monitor state dynamically:<br/>" +
                      "• <b>armAway:</b> Set automatically when Master Presence switches to <b>Away</b>.<br/>" +
                      "• <b>armNight:</b> Set automatically when Home during <b>Sleeping</b> state or the late-night <b>Wee Hours</b> period.<br/>" +
                      "• <b>disarm:</b> Set automatically during active daytime schedule periods while Home and Awake.</div>"

            input name: "manageHSM", type: "bool", title: "Control Hubitat Safety Monitor (HSM) based on selected Master Presence Sensor?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"
            
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Alexa Mode Integration:</b> Automatically follows the primary Presence state (<b>ON</b> = Home, <b>OFF</b> = Away).<br/>" +
                      "<i><b>External Trigger Behavior:</b> If toggled externally via Alexa voice command or routine, turning it <b>OFF</b> forces the system into <b>Away</b> mode (Reason: Voice). Turning it <b>ON</b> restores <b>Home</b> state and evaluates your active Step 3 Time Period schedule.</i></div>"

            input name: "alexaModeSwitch", type: "capability.switch", title: "Alexa Mode Virtual Switch <i>(ON = Home, OFF = Away)</i>", required: false
        }
        
        /* Step 2: Sleep */
        section("<b>STEP 2: Sleep Architecture & State Switches</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Sleep Architecture:</b> Awake Switch is the Primary Authoritative Input. Sleeping Switch is maintained as its inverse mirror.</div>"
            
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Sleep Modes:</b> When <b>Sleeping</b>, the app overrides the time schedule and enforces the target mode selected below. When <b>Awake</b>, the sleep overlay is released and your system automatically resumes the Step 3 Time Period schedule.</div>"

            input name: "awakeSwitch", type: "capability.switch", title: "<b>Awake Switch</b> <i>(Primary Input: ON = Awake, OFF = Sleeping)</i>", required: true, submitOnChange: true
            input name: "sleepSwitch", type: "capability.switch", title: "<b>Sleeping Switch</b> <i>(Inverse Mirror)</i>", required: true, submitOnChange: true
            input name: "sleepMode", type: "mode", title: "<b>Target Mode when Sleeping</b>", required: true, defaultValue: "Sleeping"
            input name: "vSwitchSleeping", type: "capability.switch", title: "Virtual Switch for Sleeping Mode", required: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"
            
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Alexa Awake Integration:</b> Automatically follows the primary Sleep state (<b>ON</b> = Awake, <b>OFF</b> = Sleeping).<br/>" +
                      "<i><b>External Trigger Behavior:</b> If toggled externally via Alexa voice command or routine, turning it <b>OFF</b> forces the system into <b>Sleeping</b> mode (Reason: Voice). Turning it <b>ON</b> restores <b>Awake</b> state and evaluates your active Step 3 Time Period schedule.</i></div>"

            input name: "alexaAwakeSwitch", type: "capability.switch", title: "Alexa Awake Virtual Switch <i>(Alexa Routines Sync)</i>", required: false
        }

        /* Step 3: Schedule */
        section("<b>STEP 3: Time Period & Target Mode Schedule</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Schedule Architecture:</b> Dynamic chronological time periods evaluated sequentially throughout the day when Home and Awake.</div>"
            
            Map activePeriod = getActiveTimePeriodInfo()
            String activePeriodKey = activePeriod?.key ?: "Unknown"
            
            paragraph "<div style='background-color:#EAEDED; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "⏱️ <b>Active Time Schedule Block:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodKey.toUpperCase()}</span> (Target Mode: <b>${activePeriod?.mode ?: 'None'}</b>)</div>"

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

        /* Step 4: Overrides and Diagnostics */
        section("<b>STEP 4: Manual Overrides and Diagnostics</b>", hideable: true, hidden: true) {
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "<b>Evaluation Logic Flow:</b> Manual Override &gt; Presence &gt; Voice &gt; Normal</div>"
            
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Override Persistence & Scheduler Controls</span>"
            
            input name: "holdOverride", type: "bool", title: "<b>Lock Override against lower-priority events?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to prevent incoming Voice or Normal event rechecks from unseating an active Manual Override.</span>", defaultValue: false
            input name: "suspendScheduler", type: "bool", title: "<b>Suspend CRON time period schedule during Override?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to pause daily CRON schedule triggers while in Override, stopping automatic returns to Normal mode on schedule boundaries.</span>", defaultValue: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>External Trigger Devices</span>"

            input name: "updateTriggerSwitch", type: "capability.switch", title: "Switch(s) to Trigger Evaluation / Update", required: false, multiple: true
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-8px; margin-bottom:10px;'>" +
                      "<i><b>Note:</b> Trigger switches must have an auto-off (momentary/auto-revert) setting enabled in their driver so they naturally return to OFF after firing.</i></div>"

            input name: "updateTriggerButton", type: "capability.pushableButton", title: "Button(s) to Trigger Evaluation / Update", required: false, multiple: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Manual Evaluation & Mode Recheck</span>"
            
            input name: "btnTrigger", type: "button", title: "Evaluate & Set Active Mode Now"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>System Test & Simulation Engine</span>"
            
            paragraph "<div style='background-color:#FDEDEC; border-left:4px solid #CB4335; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "<b>Simulation Control Panel:</b> Force exact state combinations for Home/Away, Awake/Sleeping, Period Schedule, and Evaluation Reason. Executing forced evaluation will simulate decisions, update mode, and sync all output switches.</div>"

            input name: "testHome", type: "bool", title: "<b>Force Home Status?</b> (ON = Home, OFF = Away)", defaultValue: true, submitOnChange: true
            input name: "testAwake", type: "bool", title: "<b>Force Awake Status?</b> (ON = Awake, OFF = Sleeping)", defaultValue: true, submitOnChange: true

            String trackerName = masterPresence ? masterPresence.displayName : "Master Presence Sensor"
            paragraph "<div style='background-color:#FCF3CF; border-left:4px solid #F1C40F; padding:8px; border-radius:4px; font-size:12px; color:#7D6608; margin-bottom:10px;'>" +
                      "⚠️ <b>Warning:</b> Please check that your Presence tracking device (<b>${trackerName}</b>) is reflecting this status.</div>"

            input name: "testPeriodKey", type: "enum", title: "<b>Select Period for Simulation</b>", required: false, submitOnChange: true,
                  options: [
                      "weeHours": "Wee Hours Target Mode (${weeHoursMode ?: 'Night'})",
                      "earlyMorning": "Early Morning Target Mode (${earlyMorningMode ?: 'Early Morning'})",
                      "morning": "Morning Target Mode (${morningMode ?: 'Morning'})",
                      "day": "Day Target Mode (${dayMode ?: 'Day'})",
                      "evening": "Evening Target Mode (${eveningMode ?: 'Evening'})",
                      "lateEvening": "Late Evening Target Mode (${lateEveningMode ?: 'Late Evening'})"
                  ]

            input name: "testReason", type: "enum", title: "<b>Evaluation Reason for Simulation</b>", defaultValue: "Override", required: false, submitOnChange: true,
                  options: ["Override": "Override (Simulation)", "Normal": "Normal (Simulation)", "Presence": "Presence (Simulation)", "Voice": "Voice (Simulation)"]

            input name: "btnForceTestEvaluation", type: "button", title: "Force Evaluation & Sync Switches"
        }

        /* Step 5: Notification(s) */
        section("<b>STEP 5: Notification(s)</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Notification System:</b> Send mode and status updates to your designated notification devices (such as Echo voice devices, mobile push devices, or dashboard tiles).</div>"
            
            input name: "notificationDevice", type: "capability.notification", title: "<b>Notification Device(s)</b>", required: false, multiple: true
        }

        /* App Preferences Section */
        section("<b>App Preferences</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Customize application labeling, logging levels, and dashboard output options.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Apps List Label Customization</span>"
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label? <i>(e.g., Mode Manager Advanced v${currentVersion})</i>", defaultValue: true, submitOnChange: true
            input name: "showModeInLabel", type: "bool", title: "Show Active Mode in App Label? <i>(e.g., [Day])</i>", defaultValue: true, submitOnChange: true
            input name: "showReasonInLabel", type: "bool", title: "Show Evaluation Reason in App Label? <i>(e.g., (Normal))</i>", defaultValue: true, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Dashboard Tile Formatting</span>"
            input name: "tileFormat", type: "enum", title: "<b>For Dashboard Tiles - Text Format</b>", options: ["plain": "Plain Text", "html": "Formatted HTML"], defaultValue: "plain", required: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Logging Levels</span>"
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-6px; margin-bottom:6px;'><i>Note: System WARN and ERROR logs are critical and always enabled. Debug logging turns off automatically after 30 minutes.</i></div>"
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
            input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false
        }
    }
}

/* =========================================================================================
   APPLICATION LIFECYCLE & INFRASTRUCTURE SCHEDULER
   ========================================================================================= */

def installed() {
    logDebug "installed() executing v${version()}..."
    seedLoggingState()
    state.lastManageHSM = getSettingBool("manageHSM", true)
    initialize()
    recheckSchedule("App Installed")
}

def uninstalled() {
    logDebug "Uninstalled v${version()}. Cleaning up subscriptions and schedules..."
    unsubscribe()
    stopPeriodSchedules()
    unschedule()
}

def updated() {
    logDebug "updated() executing v${version()}..."
    checkLoggingChanges()
    checkHsmSettingChanges()
    
    logTrace "Unsubscribing from all active device subscriptions..."
    unsubscribe()
    
    unschedule("disableDebugLogging")
    if (getSettingBool("logDebugEnable", true)) {
        logTrace "Scheduling automatic disable of debug logging in 30 minutes (1800s)."
        runIn(1800, disableDebugLogging)
    }
    
    initialize()
    
    /* Update label immediately to reflect any App Preferences toggle changes */
    updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Normal"])
    
    /* Explicitly log 'App Preferences Updated' when clicking Done */
    recheckSchedule("App Preferences Updated")
}

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    def val = settings[key]
    if (val == null) return defaultVal
    if (val instanceof Boolean) return val
    return val.toString().toBoolean()
}

private void seedLoggingState() {
    state.lastLogInfoEnable  = getSettingBool("logInfoEnable", true)
    state.lastLogDebugEnable = getSettingBool("logDebugEnable", true)
    state.lastLogTraceEnable = getSettingBool("logTraceEnable", false)
}

private void checkHsmSettingChanges() {
    Boolean currentManageHSM = getSettingBool("manageHSM", true)
    Boolean lastManageHSM    = (state.lastManageHSM != null) ? state.lastManageHSM : currentManageHSM

    if (currentManageHSM != lastManageHSM) {
        logWarn "HSM Integration changed: ${lastManageHSM ? 'ENABLED' : 'DISABLED'} -> ${currentManageHSM ? 'ENABLED' : 'DISABLED'}"
        if (currentManageHSM) {
            state.lastHsmState = null
        }
    }
    state.lastManageHSM = currentManageHSM
}

private void checkLoggingChanges() {
    Boolean currentInfo  = getSettingBool("logInfoEnable", true)
    Boolean currentDebug = getSettingBool("logDebugEnable", true)
    Boolean currentTrace = getSettingBool("logTraceEnable", false)

    if (state.lastLogInfoEnable == null)  state.lastLogInfoEnable  = currentInfo
    if (state.lastLogDebugEnable == null) state.lastLogDebugEnable = currentDebug
    if (state.lastLogTraceEnable == null) state.lastLogTraceEnable = currentTrace

    if (currentInfo != state.lastLogInfoEnable) {
        logWarn "Info Logging changed: ${state.lastLogInfoEnable ? 'ENABLED' : 'DISABLED'} -> ${currentInfo ? 'ENABLED' : 'DISABLED'}"
    }
    if (currentDebug != state.lastLogDebugEnable) {
        logWarn "Debug Logging changed: ${state.lastLogDebugEnable ? 'ENABLED' : 'DISABLED'} -> ${currentDebug ? 'ENABLED' : 'DISABLED'}"
    }
    if (currentTrace != state.lastLogTraceEnable) {
        logWarn "Trace Logging changed: ${state.lastLogTraceEnable ? 'ENABLED' : 'DISABLED'} -> ${currentTrace ? 'ENABLED' : 'DISABLED'}"
    }

    state.lastLogInfoEnable  = currentInfo
    state.lastLogDebugEnable = currentDebug
    state.lastLogTraceEnable = currentTrace
}

def initialize() {
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    state.activeTransaction = null
    state.pendingOutputSyncs = [:]
    state.lastTriggerTime = 0
    
    if (!state.modeReason) {
        state.modeReason = "Normal"
        logTrace "Seeded initial state.modeReason = 'Normal'"
    }

    if (getSettingBool("manageHSM", true) && !state.lastHsmState) {
        state.lastHsmState = location.hsmStatus ?: "disarmed"
        logTrace "Seeded initial state.lastHsmState = '${state.lastHsmState}' from location.hsmStatus"
    }

    /* Subscriptions Logging */
    logTrace "Establishing device event subscriptions..."
    if (masterPresence) {
        logTrace "Subscribing to Master Presence Sensor: ${masterPresence.displayName}"
        subscribe(masterPresence, "presence", presenceHandler)
    }
    if (homeSwitch) {
        logTrace "Subscribing to Home Switch: ${homeSwitch.displayName}"
        subscribe(homeSwitch, "switch", homeSwitchHandler)
    }
    if (awaySwitch) {
        logTrace "Subscribing to Away Switch: ${awaySwitch.displayName}"
        subscribe(awaySwitch, "switch", awaySwitchHandler)
    }
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
    if (alexaModeSwitch) {
        logTrace "Subscribing to Alexa Mode Switch: ${alexaModeSwitch.displayName}"
        subscribe(alexaModeSwitch, "switch", alexaModeSwitchHandler)
    }
    
    (updateTriggerSwitch ? [updateTriggerSwitch].flatten() : []).each { dev ->
        logTrace "Subscribing to External Trigger Switch: ${dev.displayName}"
        subscribe(dev, "switch.on", updateSwitchHandler)
    }
    (updateTriggerButton ? [updateTriggerButton].flatten() : []).each { dev ->
        logTrace "Subscribing to External Trigger Button: ${dev.displayName}"
        subscribe(dev, "pushed", updateButtonHandler)
    }

    [vSwitchSleeping, vSwitchWeeHours, vSwitchEarlyMorning, vSwitchMorning, vSwitchDay, vSwitchEvening, vSwitchLateEvening].each { vSwitch ->
        if (vSwitch) {
            logTrace "Subscribing to Virtual Indicator Switch: ${vSwitch.displayName}"
            subscribe(vSwitch, "switch.on", vSwitchHandler)
        }
    }

    /* Infrastructure Scheduler Management */
    if (state.modeReason == "Override" && getSettingBool("suspendScheduler", false)) {
        logTrace "System is in Override with suspendScheduler=ON. CRON schedules kept disabled."
        stopPeriodSchedules()
    } else {
        restartPeriodSchedules()
    }
}

/* Infrastructure Scheduler Methods */
def restartPeriodSchedules() {
    stopPeriodSchedules()
    startPeriodSchedules()
}

def startPeriodSchedules() {
    logTrace "Arming daily recurring time period CRON schedules..."
    schedulePeriodTime(timeWeeHours, "periodWeeHoursHandler")
    schedulePeriodTime(timeEarlyMorning, "periodEarlyMorningHandler")
    schedulePeriodTime(timeMorning, "periodMorningHandler")
    schedulePeriodTime(timeDay, "periodDayHandler")
    schedulePeriodTime(timeEvening, "periodEveningHandler")
    schedulePeriodTime(timeLateEvening, "periodLateEveningHandler")
}

def stopPeriodSchedules() {
    logTrace "Unscheduling daily time period triggers..."
    unschedule("periodWeeHoursHandler")
    unschedule("periodEarlyMorningHandler")
    unschedule("periodMorningHandler")
    unschedule("periodDayHandler")
    unschedule("periodEveningHandler")
    unschedule("periodLateEveningHandler")
}

private String toCronExpression(String timeIso) {
    if (!timeIso) return null
    try {
        Date d = toDateTime(timeIso)
        TimeZone tz = location.timeZone ?: TimeZone.getDefault()
        Calendar cal = Calendar.getInstance(tz)
        cal.setTime(d)
        int min = cal.get(Calendar.MINUTE)
        int hour = cal.get(Calendar.HOUR_OF_DAY)
        return "0 ${min} ${hour} * * ? *"
    } catch (Exception e) {
        logWarn "Could not parse time string '${timeIso}' to CRON: ${e.message}"
        return null
    }
}

def schedulePeriodTime(String timeIso, String handlerMethod) {
    if (timeIso && handlerMethod) {
        String cronExpr = toCronExpression(timeIso)
        if (cronExpr) {
            try {
                schedule(cronExpr, handlerMethod)
                logTrace "Scheduled daily recurring CRON period trigger '${cronExpr}' -> ${handlerMethod}()"
            } catch (Exception e) {
                logWarn "Unable to schedule CRON trigger '${cronExpr}' for '${handlerMethod}': ${e.message}"
            }
        }
    }
}

def periodWeeHoursHandler()     { handlePeriodBoundary("weeHours") }
def periodEarlyMorningHandler() { handlePeriodBoundary("earlyMorning") }
def periodMorningHandler()      { handlePeriodBoundary("morning") }
def periodDayHandler()          { handlePeriodBoundary("day") }
def periodEveningHandler()      { handlePeriodBoundary("evening") }
def periodLateEveningHandler()  { handlePeriodBoundary("lateEvening") }

private void handlePeriodBoundary(String periodKey) {
    logTrace "Scheduled time period boundary hit for key: '${periodKey}'"
    if (state.modeReason == "Override" && getSettingBool("suspendScheduler", false)) {
        logInfo "Period boundary '${periodKey}' hit but suspendScheduler is enabled. Staying in Override."
        return
    }
    
    logInfo "Period boundary '${periodKey}' hit. Processing Normal schedule transition."
    processStatePipeline([reason: "Normal", source: "Schedule CRON (${periodKey})", isBoundaryTrigger: true])
}

/* =========================================================================================
   CORE DECISION PIPELINE (Calculate -> Apply -> Sync -> Present)
   ========================================================================================= */

/* Central Authority Ranking Matrix */
private int getReasonRank(String reason) {
    switch (reason) {
        case "Override": return 4
        case "Presence": return 3
        case "Voice":    return 2
        case "Normal":   return 1
        default:         return 1
    }
}

/* Helper for manual triggers/rechecks */
private void recheckSchedule(String triggerSource) {
    logInfo "Mode recheck requested by trigger source: '${triggerSource}'"
    processStatePipeline([reason: "Normal", source: triggerSource])
}

/* Central Transaction & Pipeline Dispatcher */
private void processStatePipeline(Map request) {
    String reqReason = request.reason ?: "Normal"
    String reqSource = request.source ?: "Internal Pipeline"
    
    String txId = "tx_${now()}"
    state.activeTransaction = txId
    
    logTrace "--------------------------------------------------------------------------------"
    logTrace "START TRANSACTION #${txId} [Reason: ${reqReason} | Source: ${reqSource}]"

    try {
        /* 1. Calculate Decision */
        Map decision = calculateDecision(request)
        decision.txId = txId
        
        /* 2. Apply Decision (Commit Mode & Reason) */
        applyDecision(decision)
        
        /* 3. Sync Outputs (Switches, HSM, Notification Tile) */
        syncOutputs(decision)
        
        /* 4. Update Presentation (App Label) */
        updatePresentation(decision)
        
        logTrace "COMPLETE TRANSACTION #${txId} -> Mode: '${location.mode}' | Reason: '${state.modeReason}'"
        logTrace "--------------------------------------------------------------------------------"
    } finally {
        /* Synchronously clear active transaction context upon completion */
        state.activeTransaction = null
    }
}

/* Step 1: Calculate Decision with Simulation Injections & Preemption Checks */
private Map calculateDecision(Map request) {
    String requestedReason = request.reason ?: "Normal"
    String source = request.source ?: "Unknown"
    Boolean isBoundaryTrigger = request.isBoundaryTrigger ?: false
    Boolean forceReleaseLock = request.forceReleaseLock ?: false
    
    String currentActiveReason = state.modeReason ?: "Normal"
    int incomingRank = getReasonRank(requestedReason)
    int currentRank = getReasonRank(currentActiveReason)

    Boolean isHoldOverrideEnabled = getSettingBool("holdOverride", false)

    /* Force Release Flag overrides Lock Checks */
    if (forceReleaseLock) {
        logInfo "Force-release requested by '${source}'. Resetting active '${currentActiveReason}' state to '${requestedReason}'."
    } else if (currentActiveReason == "Override") {
        if (isHoldOverrideEnabled) {
            if (incomingRank < currentRank && !isBoundaryTrigger) {
                logInfo "Incoming request [Reason: ${requestedReason}] blocked by active 'holdOverride' Mode Lock."
                requestedReason = "Override"
            }
        } else {
            /* holdOverride is OFF: Allow lower-priority incoming events (like Normal rechecks) to unseat Override */
            logInfo "Active 'Override' state released to incoming request [Reason: ${requestedReason}] because 'holdOverride' is OFF."
        }
    } else if (incomingRank < currentRank) {
        if (isBoundaryTrigger) {
            logInfo "Schedule period boundary hit. Releasing active '${currentActiveReason}' state to 'Normal'."
        } else {
            logInfo "Incoming evaluation request [Reason: ${requestedReason}] preempted by active higher-priority state [Reason: ${currentActiveReason}]."
            requestedReason = currentActiveReason
        }
    }

    String targetMode = null
    String activePeriodKey = null

    /* Evaluate Hardware vs. Simulated Input Injection */
    Boolean isHome = (request.simulatedHome != null) ? request.simulatedHome : (homeSwitch?.currentValue("switch") == "on")
    Boolean isSleeping = (request.simulatedAwake != null) ? !request.simulatedAwake : (awakeSwitch?.currentValue("switch") == "off")

    if (requestedReason == "Override") {
        targetMode = request.targetMode ?: location.mode
        activePeriodKey = request.periodKey
    } else if (requestedReason == "Voice") {
        targetMode = request.targetMode
        activePeriodKey = request.periodKey
        if (!targetMode) {
            if (isSleeping) {
                targetMode = sleepMode?.toString() ?: "Sleeping"
                activePeriodKey = "sleeping"
            } else {
                Map activePeriod = getActiveTimePeriodInfo(request.simulatedPeriodKey)
                targetMode = activePeriod?.mode ?: location.mode
                activePeriodKey = activePeriod?.key
            }
        }
    } else if (requestedReason == "Presence") {
        if (!isHome) {
            targetMode = "Away"
            activePeriodKey = null
        } else {
            if (isSleeping) {
                targetMode = sleepMode?.toString() ?: "Sleeping"
                activePeriodKey = "sleeping"
            } else {
                Map activePeriod = getActiveTimePeriodInfo(request.simulatedPeriodKey)
                targetMode = activePeriod?.mode ?: location.mode
                activePeriodKey = activePeriod?.key
            }
        }
    } else {
        /* Normal Schedule Evaluation */
        if (!isHome) {
            targetMode = "Away"
            activePeriodKey = null
            requestedReason = "Presence"
        } else {
            if (isSleeping) {
                targetMode = sleepMode?.toString() ?: "Sleeping"
                activePeriodKey = "sleeping"
            } else {
                Map activePeriod = getActiveTimePeriodInfo(request.simulatedPeriodKey)
                targetMode = activePeriod?.mode ?: location.mode
                activePeriodKey = activePeriod?.key
            }
        }
    }

    return [
        reason: requestedReason,
        source: source,
        targetMode: targetMode,
        periodKey: activePeriodKey
    ]
}

/* Step 2: Apply Decision */
private void applyDecision(Map decision) {
    String newMode = decision.targetMode
    String newReason = decision.reason
    
    if (!newMode) {
        logWarn "Decision produced null target mode. Aborting mode apply."
        return
    }

    state.modeReason = newReason
    
    if (newReason == "Override" && getSettingBool("suspendScheduler", false)) {
        stopPeriodSchedules()
    } else if (newReason != "Override") {
        restartPeriodSchedules()
    }

    if (location.mode != newMode) {
        logInfo "Changing Hubitat Location Mode from '${location.mode}' to '${newMode}' | Reason: ${newReason} (${decision.source})"
        setLocationMode(newMode)
    }
}

/* Step 3: Sync Outputs */
private void syncOutputs(Map decision) {
    String targetMode = decision.targetMode
    String periodKey = decision.periodKey
    String reason = decision.reason

    /* Presence & HSM Integration Synchronization */
    if (targetMode == "Away") {
        syncSwitch(homeSwitch, "off")
        syncSwitch(awaySwitch, "on")
        syncSwitch(alexaModeSwitch, "off")
        
        if (getSettingBool("manageHSM", true) && state.lastHsmState != "armedAway") {
            logInfo "HSM changed: '${state.lastHsmState ?: 'unknown'}' -> 'armAway'"
            logDebug "Executing HSM arming command -> armAway (Reason: ${reason})"
            state.lastHsmState = "armedAway"
            sendLocationEvent(name: "hsmSetArm", value: "armAway")
        }
    } else {
        syncSwitch(homeSwitch, "on")
        syncSwitch(awaySwitch, "off")
        syncSwitch(alexaModeSwitch, "on")
        
        if (getSettingBool("manageHSM", true)) {
            Boolean isSleepingOrNight = (periodKey == "sleeping" || periodKey == "weeHours" || targetMode == sleepMode?.toString() || targetMode == weeHoursMode?.toString())
            String targetHsmState = isSleepingOrNight ? "armNight" : "disarm"

            if (state.lastHsmState != targetHsmState) {
                logInfo "HSM changed: '${state.lastHsmState ?: 'unknown'}' -> '${targetHsmState}'"
                logDebug "Executing HSM state update command -> ${targetHsmState} (Reason: ${reason})"
                state.lastHsmState = targetHsmState
                sendLocationEvent(name: "hsmSetArm", value: targetHsmState)
            }
        }
    }

    /* Sleep Switches Sync */
    if (periodKey == "sleeping" || targetMode == sleepMode?.toString()) {
        syncSwitch(awakeSwitch, "off")
        syncSwitch(sleepSwitch, "on")
        syncSwitch(alexaAwakeSwitch, "off")
    } else if (targetMode != "Away") {
        syncSwitch(awakeSwitch, "on")
        syncSwitch(sleepSwitch, "off")
        syncSwitch(alexaAwakeSwitch, "on")
    }

    /* Virtual Mode Indicator Switches Sync */
    updateVirtualModeSwitches(periodKey)

    /* Dashboard Tile Notification Sync */
    dispatchTileNotification(targetMode, reason)
}

/* Safe Output Switch Command Execution with Asynchronous Event Registration */
private void syncSwitch(def device, String targetState) {
    if (device != null && device.hasCommand(targetState)) {
        if (device.currentValue("switch") != targetState) {
            String devId = device.id.toString()
            logTrace "Syncing Output Device '${device.displayName}' -> ${targetState.toUpperCase()}"
            
            /* Register expected asynchronous output event marker (valid for 3000ms) */
            Map pendingMap = state.pendingOutputSyncs ?: [:]
            pendingMap[devId] = [value: targetState, expires: now() + 3000]
            state.pendingOutputSyncs = pendingMap

            device."${targetState}"()
        }
    }
}

/* Virtual Indicator Switch Synchronization Engine */
private void updateVirtualModeSwitches(String activePeriodKey) {
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
            String targetState = (activePeriodKey != null && periodKey == activePeriodKey) ? "on" : "off"
            syncSwitch(vSwitch, targetState)
        }
    }
}

/* Notification Tile Dispatch */
private void dispatchTileNotification(String modeVal, String reasonVal) {
    if (!notificationDevice) return

    String msg = (settings.tileFormat == "html") ?
        "<div style='font-size:13px; font-weight:bold; color:#27AE60;'>Mode is now ${modeVal}</div><div style='font-size:11px; color:#7F8C8D; font-style:italic;'>Reason: ${reasonVal}</div>" :
        "Mode is now ${modeVal} (Reason: ${reasonVal})"

    notificationDevice.each { device ->
        device.deviceNotification(msg)
    }
}

/* Step 4: Update Presentation */
private void updatePresentation(Map decision) {
    updateAppLabel(decision.targetMode ?: location.mode, decision.reason ?: state.modeReason)
}

/* Dynamic Apps List Badge Renderer */
private void updateAppLabel(String currentMode = null, String reason = null) {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    Boolean showMode    = getSettingBool("showModeInLabel", true)
    Boolean showReason  = getSettingBool("showReasonInLabel", true)

    String baseLabel = "Mode Manager Advanced"
    if (showVersion) {
        baseLabel += " v${version()}"
    }

    String displayMode = currentMode ?: location.mode ?: "Unknown"
    String currentReason = reason ?: state.modeReason ?: "Normal"

    List<String> badgeParts = []
    if (showMode) {
        badgeParts.add("<span style='color:green; font-weight:bold;'>${displayMode}</span>")
    }
    if (showReason) {
        badgeParts.add("(${currentReason})")
    }

    String formattedLabel = baseLabel
    if (!badgeParts.isEmpty()) {
        formattedLabel += " - [" + badgeParts.join(" ") + "]"
    }
    
    if (app.label != formattedLabel) {
        app.updateLabel(formattedLabel)
    }
}

/* =========================================================================================
   EVENT HANDLERS & INPUT ISOLATION
   ========================================================================================= */

private boolean isInternalTransaction(def evt = null) {
    /* 1. Synchronous Transaction Isolation */
    if (state.activeTransaction != null) {
        logTrace "Suppressing event generated during active pipeline transaction (${state.activeTransaction})"
        return true
    }

    /* 2. Asynchronous Output Suppression Buffer Check */
    if (evt != null) {
        String devId = evt.deviceId.toString()
        String evtVal = evt.value?.toString()
        Map pendingMap = state.pendingOutputSyncs ?: [:]

        if (pendingMap.containsKey(devId)) {
            Map marker = pendingMap[devId]
            long currentMs = now()

            if (marker && marker.value == evtVal && currentMs <= (marker.expires as long)) {
                logTrace "Suppressing internal output sync event from '${evt.device?.displayName}' [${evtVal}]"
                pendingMap.remove(devId)
                state.pendingOutputSyncs = pendingMap
                return true
            } else if (marker && currentMs > (marker.expires as long)) {
                pendingMap.remove(devId)
                state.pendingOutputSyncs = pendingMap
            }
        }
    }

    return false
}

def appButtonHandler(btn) {
    if (btn == "btnTrigger") {
        long currentMs = now()
        if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
        state.lastTriggerTime = currentMs

        /* Recheck schedule (will force-release if holdOverride is OFF or evaluate normally) */
        recheckSchedule("Manual UI Button ('Evaluate & Set Active Mode Now')")
    } else if (btn == "btnForceTestEvaluation") {
        executeForcedTestEvaluation()
    }
}

def updateSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
    state.lastTriggerTime = currentMs

    recheckSchedule("Trigger Switch '${evt.device.displayName}'")
}

def updateButtonHandler(evt) {
    if (isInternalTransaction(evt)) return
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
    state.lastTriggerTime = currentMs

    recheckSchedule("Trigger Button '${evt.device.displayName}' (Button #${evt.value})")
}

def vSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    if (evt.value != "on") return

    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 1000)) return
    state.lastTriggerTime = currentMs

    String deviceId = evt.deviceId.toString()
    Map<String, Map> switchIdToPeriodMap = [:]
    if (vSwitchSleeping)     switchIdToPeriodMap[vSwitchSleeping.id.toString()]     = [mode: sleepMode?.toString(),        key: "sleeping"]
    if (vSwitchWeeHours)     switchIdToPeriodMap[vSwitchWeeHours.id.toString()]     = [mode: weeHoursMode?.toString(),     key: "weeHours"]
    if (vSwitchEarlyMorning) switchIdToPeriodMap[vSwitchEarlyMorning.id.toString()] = [mode: earlyMorningMode?.toString(), key: "earlyMorning"]
    if (vSwitchMorning)      switchIdToPeriodMap[vSwitchMorning.id.toString()]      = [mode: morningMode?.toString(),      key: "morning"]
    if (vSwitchDay)          switchIdToPeriodMap[vSwitchDay.id.toString()]          = [mode: dayMode?.toString(),          key: "day"]
    if (vSwitchEvening)      switchIdToPeriodMap[vSwitchEvening.id.toString()]      = [mode: eveningMode?.toString(),      key: "evening"]
    if (vSwitchLateEvening)  switchIdToPeriodMap[vSwitchLateEvening.id.toString()]  = [mode: lateEveningMode?.toString(),  key: "lateEvening"]

    Map targetPeriod = switchIdToPeriodMap[deviceId]
    
    if (targetPeriod?.mode) {
        logInfo "Period virtual switch '${evt.device.displayName}' toggled externally."
        processStatePipeline([
            reason: "Override",
            targetMode: targetPeriod.mode,
            periodKey: targetPeriod.key,
            source: "Virtual Switch (${evt.device.displayName})"
        ])
    }
}

def presenceHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Master presence sensor '${evt.device.displayName}' changed to '${evt.value}'"
    processStatePipeline([reason: "Presence", presenceValue: evt.value, source: "Master Presence Sensor"])
}

def homeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Home switch '${evt.device.displayName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "on") ? "present" : "not present"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Home Switch"])
}

def awaySwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Away switch '${evt.device.displayName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "off") ? "present" : "not present"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Away Switch"])
}

def alexaModeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Alexa Mode Switch changed to '${evt.value}'"
    String targetMode = (evt.value == "on") ? (getActiveTimePeriodInfo()?.mode ?: location.mode) : "Away"
    String targetKey = (evt.value == "on") ? getActiveTimePeriodInfo()?.key : null
    processStatePipeline([
        reason: "Voice",
        targetMode: targetMode,
        periodKey: targetKey,
        source: "Alexa Mode Switch"
    ])
}

def awakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Awake switch '${evt.device.displayName}' changed to '${evt.value}'"
    recheckSchedule("Awake Switch '${evt.device.displayName}' (${evt.value})")
}

def sleepSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Sleep switch '${evt.device.displayName}' changed to '${evt.value}'"
    recheckSchedule("Sleep Switch '${evt.device.displayName}' (${evt.value})")
}

def alexaAwakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Alexa Awake Switch changed to '${evt.value}'"
    
    if (evt.value == "off") {
        processStatePipeline([
            reason: "Voice",
            targetMode: sleepMode?.toString(),
            periodKey: "sleeping",
            source: "Alexa Awake Switch (OFF)"
        ])
    } else {
        Map activePeriod = getActiveTimePeriodInfo()
        processStatePipeline([
            reason: "Voice",
            targetMode: activePeriod?.mode ?: location.mode,
            periodKey: activePeriod?.key,
            source: "Alexa Awake Switch (ON)"
        ])
    }
}

/* Step 4 Diagnostic Simulation Engine */
def executeForcedTestEvaluation() {
    Boolean isHomeTarget = getSettingBool("testHome", true)
    Boolean isAwakeTarget = getSettingBool("testAwake", true)
    String forcedPeriodKey = settings.testPeriodKey ?: "day"
    String targetReason = settings.testReason ?: "Override"

    logInfo "Executing Forced Simulation -> Home: ${isHomeTarget} | Awake: ${isAwakeTarget} | Period: ${forcedPeriodKey} | Reason: ${targetReason}"

    /* Inject simulated inputs into pipeline rather than bypassing decision logic */
    processStatePipeline([
        reason: targetReason,
        simulatedHome: isHomeTarget,
        simulatedAwake: isAwakeTarget,
        simulatedPeriodKey: forcedPeriodKey,
        source: "Step 4 Simulation Control Panel",
        forceReleaseLock: true
    ])
}

/* =========================================================================================
   CALCULATION & UTILITY ROUTINES
   ========================================================================================= */

Map getActiveTimePeriodInfo(String overrideKey = null) {
    List<Map> periods = [
        [key: "weeHours",     mode: weeHoursMode?.toString(),     start: getMinutesFromSetting(timeWeeHours, 30)],
        [key: "earlyMorning", mode: earlyMorningMode?.toString(), start: getMinutesFromSetting(timeEarlyMorning, 285)],
        [key: "morning",      mode: morningMode?.toString(),      start: getMinutesFromSetting(timeMorning, 450)],
        [key: "day",          mode: dayMode?.toString(),          start: getMinutesFromSetting(timeDay, 600)],
        [key: "evening",      mode: eveningMode?.toString(),      start: getMinutesFromSetting(timeEvening, 1020)],
        [key: "lateEvening",  mode: lateEveningMode?.toString(),  start: getMinutesFromSetting(timeLateEvening, 1290)]
    ]

    if (overrideKey != null) {
        Map match = periods.find { it.key == overrideKey }
        if (match) return match
    }

    Date now = new Date()
    int currentMinutes = timeToMinutes(now)
    periods.sort { it.start }
    Map activePeriod = periods.reverse().find { currentMinutes >= it.start }

    return activePeriod ?: periods.last()
}

private int getMinutesFromSetting(String timeIso, int defaultMinutes) {
    if (!timeIso) return defaultMinutes
    try {
        Date d = toDateTime(timeIso)
        return timeToMinutes(d)
    } catch (Exception e) {
        return defaultMinutes
    }
}

private int timeToMinutes(Date time) {
    TimeZone tz = location.timeZone ?: TimeZone.getDefault()
    Calendar cal = Calendar.getInstance(tz)
    cal.setTime(time)
    return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", true)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

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