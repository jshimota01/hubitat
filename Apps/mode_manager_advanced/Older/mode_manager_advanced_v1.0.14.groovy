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
 *  v1.0.14 (2026-08-13) - Step 6 Test Harness Refinement:
 *                         - Cleaned up Step 6 time period options to align strictly with Step 3 schedule blocks.
 *                         - Removed legacy 'Test Mode' reason choice; options now strictly match real pipeline reasons (Normal, Presence, Voice, Override).
 *  v1.0.13 (2026-08-13) - UI Label Refinement:
 *                         - Updated Step 5 notification input title to 'Notification Device(s)'.
 *                         - Updated tile formatting input title to 'For Dashboard Tiles - Text Format' to distinguish tile formatting from voice/push outputs.
 *  v1.0.12 (2026-08-13) - UI Text Refinement:
 *                         - Renamed Step 4 manual section header to 'Manual Evaluation & Mode Recheck'.
 *                         - Renamed manual execution button to 'Evaluate & Set Active Mode Now'.
 *  v1.0.11 (2026-08-13) - UI Label Refinement:
 *                         - Updated Step 4 trigger inputs to say 'Switch(s)' and 'Button(s)'.
 *  v1.0.10 (2026-08-13) - Step 4 Logic & UI Refinement:
 *                         - Inverted autoReturnNormal logic to holdOverride (default: OFF).
 *                         - Updated Step 4 description to clarify holding override across period boundaries.
 *  v1.0.9 (2026-08-13) - UI Text Refinement:
 *                        - Added descriptive text under Step 2 Alexa Ecosystem section.
 *  v1.0.8 (2026-08-13) - UI Text Refinement:
 *                        - Added descriptive text under Step 1 Alexa Ecosystem section.
 *  v1.0.7 (2026-08-13) - UI Text Refinement:
 *                        - Clarified Step 1 note wording to reference active Time Period schedule and Sleep state.
 *  v1.0.6 (2026-08-13) - UI Clarity Enhancements:
 *                        - Added explicit explanatory notes in Step 1 and Step 2 UI sections.
 *  v1.0.5 (2026-08-13) - Lifecycle Logging & Trigger Clarity:
 *                        - Added explicit trace/debug logs across installed(), updated(), and initialize().
 *  v1.0.4 (2026-08-13) - UI Refinement: Dynamic clickable section header carets.
 *  v1.0.3 (2026-08-13) - UI Enhancement: Dynamic section carets.
 *  v1.0.2 (2026-08-13) - Bug Fixes & Code Audit: Fixed transaction release, null switch commands, and Test Harness forced reason overrides.
 *  v1.0.1 (2026-08-13) - Full Architectural Refactor: Linear decision pipeline, transaction output isolation, and infrastructure scheduler.
 *
 */

static String version() { return '1.0.14' }

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
            input name: "manageHSM", type: "bool", title: "Control Hubitat Safety Monitor (HSM) based on Presence?", defaultValue: true

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
        section("<b>STEP 4: Manual Overrides & Triggers</b>", hideable: true, hidden: true) {
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "<b>Evaluation Logic Flow:</b> Manual Override &gt; Presence &gt; Voice &gt; Normal</div>"
            
            input name: "holdOverride", type: "bool", title: "<b>Hold Override across Time Period boundaries?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to stop scheduled return to Normal mode on the next Period boundary. Useful for testing or forcing a persistent override state.</span>", defaultValue: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>"

            input name: "updateTriggerSwitch", type: "capability.switch", title: "Switch(s) to Trigger Evaluation / Update", required: false, multiple: true
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-8px; margin-bottom:10px;'>" +
                      "<i><b>Note:</b> Trigger switches must have an auto-off (momentary/auto-revert) setting enabled in their driver so they naturally return to OFF after firing.</i></div>"

            input name: "updateTriggerButton", type: "capability.pushableButton", title: "Button(s) to Trigger Evaluation / Update", required: false, multiple: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Manual Evaluation & Mode Recheck</span>"
            
            input name: "btnTrigger", type: "button", title: "Evaluate & Set Active Mode Now"
        }

        /* Step 5: Logging & Notifications */
        section("<b>STEP 5: Logging & System Diagnostics</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Logging Architecture:</b> Info logs state changes. Debug logging automatically disables after 30 minutes to reduce log noise.<br/>" +
                      "<i><b>Note:</b> WARN and ERROR logs are system-critical and always enabled.</i></div>"
            
            input name: "notificationDevice", type: "capability.notification", title: "<b>Notification Device(s)</b>", required: false, multiple: true
            input name: "tileFormat", type: "enum", title: "<b>For Dashboard Tiles - Text Format</b>", options: ["plain": "Plain Text", "html": "Formatted HTML"], defaultValue: "plain", required: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>"
            
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
            input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false
        }

        /* Step 6: Testing & Diagnostics Engine */
        section("<b>STEP 6: Testing & Diagnostics Engine</b>", hideable: true, hidden: true) {
            paragraph "<div style='background-color:#FDEDEC; border-left:4px solid #CB4335; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "<b>Test Control Panel:</b> Force exact state combinations for Home/Away, Awake/Sleeping, Period Schedule, and Evaluation Reason. Executing forced evaluation will simulate decisions, update mode, and sync all output switches.</div>"

            input name: "testHome", type: "bool", title: "<b>Force Home Status?</b> (ON = Home, OFF = Away)", defaultValue: true, submitOnChange: true
            input name: "testAwake", type: "bool", title: "<b>Force Awake Status?</b> (ON = Awake, OFF = Sleeping)", defaultValue: true, submitOnChange: true

            String trackerName = masterPresence ? masterPresence.displayName : "Master Presence Sensor"
            paragraph "<div style='background-color:#FCF3CF; border-left:4px solid #F1C40F; padding:8px; border-radius:4px; font-size:12px; color:#7D6608; margin-bottom:10px;'>" +
                      "⚠️ <b>Warning:</b> Please check that your Presence tracking device (<b>${trackerName}</b>) is reflecting this status.</div>"

            input name: "testPeriodKey", type: "enum", title: "<b>Step 3 Schedule Period to Test</b>", required: false, submitOnChange: true,
                  options: [
                      "weeHours": "Wee Hours Target Mode (${weeHoursMode ?: 'Night'})",
                      "earlyMorning": "Early Morning Target Mode (${earlyMorningMode ?: 'Early Morning'})",
                      "morning": "Morning Target Mode (${morningMode ?: 'Morning'})",
                      "day": "Day Target Mode (${dayMode ?: 'Day'})",
                      "evening": "Evening Target Mode (${eveningMode ?: 'Evening'})",
                      "lateEvening": "Late Evening Target Mode (${lateEveningMode ?: 'Late Evening'})"
                  ]

            input name: "testReason", type: "enum", title: "<b>Evaluation Reason to Simulate</b>", defaultValue: "Override", required: false, submitOnChange: true,
                  options: ["Override": "Override", "Normal": "Normal", "Presence": "Presence", "Voice": "Voice"]

            input name: "btnForceTestEvaluation", type: "button", title: "Force Evaluation & Sync Switches"
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
    if (state.modeReason == "Override" && getSettingBool("holdOverride", false)) {
        logTrace "System is in Override with holdOverride=ON. CRON schedules kept disabled."
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
    if (state.modeReason == "Override" && getSettingBool("holdOverride", false)) {
        logInfo "Period boundary '${periodKey}' hit but holdOverride is enabled. Staying in Override."
        return
    }
    
    logInfo "Period boundary '${periodKey}' hit. Processing Normal schedule transition."
    processStatePipeline([reason: "Normal", source: "Schedule CRON (${periodKey})"])
}

/* =========================================================================================
   CORE DECISION PIPELINE (Calculate -> Apply -> Sync -> Present)
   ========================================================================================= */

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

/* Step 1: Calculate Decision */
private Map calculateDecision(Map request) {
    String requestedReason = request.reason ?: "Normal"
    String source = request.source ?: "Unknown"
    
    String targetMode = null
    String activePeriodKey = null
    
    if (requestedReason == "Override") {
        targetMode = request.targetMode ?: location.mode
        activePeriodKey = request.periodKey
    } else if (requestedReason == "Voice") {
        targetMode = request.targetMode
        activePeriodKey = request.periodKey
        if (!targetMode) {
            Map activePeriod = getActiveTimePeriodInfo()
            targetMode = activePeriod?.mode ?: location.mode
            activePeriodKey = activePeriod?.key
        }
    } else if (requestedReason == "Presence") {
        String presenceVal = request.presenceValue ?: (masterPresence?.currentValue("presence") == "present" ? "present" : "not present")
        if (presenceVal == "not present") {
            targetMode = "Away"
            activePeriodKey = null
        } else {
            Boolean isSleeping = (awakeSwitch?.currentValue("switch") == "off")
            if (isSleeping) {
                targetMode = sleepMode?.toString() ?: "Sleeping"
                activePeriodKey = "sleeping"
            } else {
                Map activePeriod = getActiveTimePeriodInfo()
                targetMode = activePeriod?.mode ?: location.mode
                activePeriodKey = activePeriod?.key
            }
        }
    } else {
        /* Normal Schedule Evaluation */
        Boolean isHome = (homeSwitch?.currentValue("switch") == "on")
        if (!isHome) {
            targetMode = "Away"
            activePeriodKey = null
            requestedReason = "Presence"
        } else {
            Boolean isSleeping = (awakeSwitch?.currentValue("switch") == "off")
            if (isSleeping) {
                targetMode = sleepMode?.toString() ?: "Sleeping"
                activePeriodKey = "sleeping"
            } else {
                Map activePeriod = getActiveTimePeriodInfo()
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
    
    if (newReason == "Override" && getSettingBool("holdOverride", false)) {
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

    /* Presence Switches Sync */
    if (targetMode == "Away") {
        syncSwitch(homeSwitch, "off")
        syncSwitch(awaySwitch, "on")
        syncSwitch(alexaModeSwitch, "off")
        
        if (getSettingBool("manageHSM", true) && state.lastHsmState != "armedAway") {
            logDebug "Arming Hubitat Safety Monitor (HSM: armAway)"
            sendLocationEvent(name: "hsmSetArm", value: "armAway")
            state.lastHsmState = "armedAway"
        }
    } else {
        syncSwitch(homeSwitch, "on")
        syncSwitch(awaySwitch, "off")
        syncSwitch(alexaModeSwitch, "on")
        
        if (getSettingBool("manageHSM", true) && state.lastHsmState != "disarmed") {
            logDebug "Disarming Hubitat Safety Monitor (HSM: disarm)"
            sendLocationEvent(name: "hsmSetArm", value: "disarm")
            state.lastHsmState = "disarmed"
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

/* Safe Output Switch Command Execution */
private void syncSwitch(def device, String targetState) {
    if (device != null && device.hasCommand(targetState)) {
        if (device.currentValue("switch") != targetState) {
            logTrace "Syncing Output Device '${device.displayName}' -> ${targetState.toUpperCase()}"
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
    String baseLabel = "Mode Manager Advanced v${version()}"
    String displayMode = currentMode ?: location.mode ?: "Unknown"
    String currentReason = reason ?: state.modeReason ?: "Normal"

    String formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode} (${currentReason})</span>]"
    
    if (app.label != formattedLabel) {
        app.updateLabel(formattedLabel)
    }
}

/* =========================================================================================
   EVENT HANDLERS & INPUT ISOLATION
   ========================================================================================= */

private boolean isInternalTransaction() {
    if (state.activeTransaction != null) {
        logTrace "Suppressing event generated by internal transaction (${state.activeTransaction})"
        return true
    }
    return false
}

def appButtonHandler(btn) {
    if (btn == "btnTrigger") {
        long currentMs = now()
        if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
        state.lastTriggerTime = currentMs

        recheckSchedule("Manual UI Button ('Evaluate & Set Active Mode Now')")
    } else if (btn == "btnForceTestEvaluation") {
        executeForcedTestEvaluation()
    }
}

def updateSwitchHandler(evt) {
    if (isInternalTransaction()) return
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
    state.lastTriggerTime = currentMs

    recheckSchedule("Trigger Switch '${evt.device.displayName}'")
}

def updateButtonHandler(evt) {
    if (isInternalTransaction()) return
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
    state.lastTriggerTime = currentMs

    recheckSchedule("Trigger Button '${evt.device.displayName}' (Button #${evt.value})")
}

def vSwitchHandler(evt) {
    if (isInternalTransaction()) return
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
    if (isInternalTransaction()) return
    logInfo "Master presence sensor '${evt.device.displayName}' changed to '${evt.value}'"
    processStatePipeline([reason: "Presence", presenceValue: evt.value, source: "Master Presence Sensor"])
}

def homeSwitchHandler(evt) {
    if (isInternalTransaction()) return
    logInfo "Home switch '${evt.device.displayName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "on") ? "present" : "not present"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Home Switch"])
}

def awaySwitchHandler(evt) {
    if (isInternalTransaction()) return
    logInfo "Away switch '${evt.device.displayName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "off") ? "present" : "not present"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Away Switch"])
}

def alexaModeSwitchHandler(evt) {
    if (isInternalTransaction()) return
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
    if (isInternalTransaction()) return
    logInfo "Awake switch '${evt.device.displayName}' changed to '${evt.value}'"
    recheckSchedule("Awake Switch '${evt.device.displayName}' (${evt.value})")
}

def sleepSwitchHandler(evt) {
    if (isInternalTransaction()) return
    logInfo "Sleep switch '${evt.device.displayName}' changed to '${evt.value}'"
    recheckSchedule("Sleep Switch '${evt.device.displayName}' (${evt.value})")
}

def alexaAwakeSwitchHandler(evt) {
    if (isInternalTransaction()) return
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

/* Step 6 Diagnostic Test Harness Engine */
def executeForcedTestEvaluation() {
    Boolean isHomeTarget = getSettingBool("testHome", true)
    Boolean isAwakeTarget = getSettingBool("testAwake", true)
    String periodKey = settings.testPeriodKey ?: "day"
    String targetReason = settings.testReason ?: "Override"

    logInfo "Executing Forced Test Evaluation -> Home: ${isHomeTarget} | Awake: ${isAwakeTarget} | Schedule Period: ${periodKey} | Reason: ${targetReason}"

    String targetMode = null
    if (!isHomeTarget) {
        targetMode = "Away"
        periodKey = null
    } else if (!isAwakeTarget) {
        targetMode = sleepMode?.toString() ?: "Sleeping"
        periodKey = "sleeping"
    } else {
        Map<String, String> periodToModeMap = [
            "weeHours": weeHoursMode?.toString(),
            "earlyMorning": earlyMorningMode?.toString(),
            "morning": morningMode?.toString(),
            "day": dayMode?.toString(),
            "evening": eveningMode?.toString(),
            "lateEvening": lateEveningMode?.toString()
        ]
        targetMode = periodToModeMap[periodKey] ?: location.mode
    }

    processStatePipeline([
        reason: targetReason,
        targetMode: targetMode,
        periodKey: periodKey,
        source: "Step 6 Test Control Panel"
    ])
}

/* =========================================================================================
   CALCULATION & UTILITY ROUTINES
   ========================================================================================= */

Map getActiveTimePeriodInfo() {
    Date now = new Date()
    int currentMinutes = timeToMinutes(now)

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