/*
 * Mode Manager Advanced
 * Improved Mode Manager that uses Presence and Sleeping in addition to time periods
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Change History:
 *
 * v1.0.61 (2026-08-16) - Notification Mute & Sleep Release Fix:
 *                        - Fixed dispatchTileNotification() to evaluate isSleeping strictly against incoming target decision rather than stale state.modeReason.
 *                        - Correctly allows audio notifications when waking up from Sleeping mode to Day/Morning.
 * v1.0.60 (2026-08-16) - Sleeping State Output Switch Sync Fix.
 *
 */

static String version() { return '1.0.61' }

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

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        
        String currentVersion = version()
        Boolean isCollapsed = (state.sectionsExpanded == true) ? false : true
        state.sectionsExpanded = false
        
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

        /* Section 1: Presence */
        section("<b>SECTION 1: Presence Architecture & State Switches</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'><b>Presence Architecture:</b> Master Presence Sensor updates Home Switch. Away Switch is maintained as its inverse mirror.</div>"

            input name: "masterPresence", type: "capability.presenceSensor", title: "<b>Master Presence Sensor</b>", required: true
            input name: "homeSwitch", type: "capability.switch", title: "<b>Home Switch</b>", required: true
            input name: "awaySwitch", type: "capability.switch", title: "<b>Away Switch</b>", required: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Hubitat Safety Monitor (HSM) Integration</span>"

            input name: "manageHSM", type: "bool", title: "Control Hubitat Safety Monitor (HSM) based on selected Master Presence Sensor?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"

            input name: "alexaModeSwitch", type: "capability.switch", title: "Alexa Mode Virtual Switch <i>(ON = Home, OFF = Away)</i>", required: false
        }
        
        /* Section 2: Sleep Architecture */
        section("<b>SECTION 2: Sleep Architecture & State Switches</b>", hideable: true, hidden: isCollapsed) {
            input name: "awakeSwitch", type: "capability.switch", title: "<b>Awake Switch</b> <i>(ON = Awake, OFF = Sleeping)</i>", required: true
            input name: "sleepSwitch", type: "capability.switch", title: "<b>Sleeping Switch</b> <i>(Passive Inverse Mirror)</i>", required: false
            input name: "sleepMode", type: "mode", title: "<b>Target Mode when Sleeping</b>", required: true, defaultValue: "Sleeping"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"

            input name: "alexaAwakeSwitch", type: "capability.switch", title: "Alexa Awake Virtual Switch <i>(Alexa Routines Sync)</i>", required: false
        }

        /* Section 3: Schedule */
        section("<b>SECTION 3: Time Period & Target Mode Schedule</b>", hideable: true, hidden: isCollapsed) {
            Map activePeriod = getActiveTimePeriodInfo()
            String activePeriodKey = activePeriod?.key ?: "Unknown"
            
            paragraph "<div style='background-color:#EAEDED; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "⏱️ <b>Active Time Schedule Block:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodKey.toUpperCase()}</span> (Target Mode: <b>${activePeriod?.mode ?: 'None'}</b>)</div>"

            input name: "timeWeeHours", type: "time", title: "Wee Hours Start", required: true, defaultValue: "00:30", width: 6
            input name: "weeHoursMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Night", width: 3
            input name: "vSwitchWeeHours", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3
            
            input name: "timeEarlyMorning", type: "time", title: "Early Morning Start", required: true, defaultValue: "04:45", width: 6
            input name: "earlyMorningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Early Morning", width: 3
            input name: "vSwitchEarlyMorning", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3
            
            input name: "timeMorning", type: "time", title: "Morning Start", required: true, defaultValue: "07:30", width: 6
            input name: "morningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Morning", width: 3
            input name: "vSwitchMorning", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3
            
            input name: "timeDay", type: "time", title: "Day Start", required: true, defaultValue: "10:00", width: 6
            input name: "dayMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Day", width: 3
            input name: "vSwitchDay", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3
            
            input name: "timeEvening", type: "time", title: "Evening Start", required: true, defaultValue: "17:00", width: 6
            input name: "eveningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Evening", width: 3
            input name: "vSwitchEvening", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3
            
            input name: "timeLateEvening", type: "time", title: "Late Evening Start", required: true, defaultValue: "21:30", width: 6
            input name: "lateEveningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Late Evening", width: 3
            input name: "vSwitchLateEvening", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3
        }

        /* Section 4: Diagnostics */
        section("<b>SECTION 4: Manual Overrides and Diagnostics</b>", hideable: true, hidden: isCollapsed) {
            input name: "holdOverride", type: "bool", title: "Lock Override against lower-priority events?", defaultValue: false
            input name: "suspendScheduler", type: "bool", title: "Suspend CRON time period schedule during Override?", defaultValue: false
            input name: "statusTileDevice", type: "capability.actuator", title: "Virtual Ground-Truth Status Device", required: false
            input name: "updateTriggerSwitch", type: "capability.switch", title: "Switch(s) to Trigger Evaluation / Update", required: false, multiple: true
            input name: "updateTriggerButton", type: "capability.pushableButton", title: "Button(s) to Trigger Evaluation / Update", required: false, multiple: true
            input name: "btnTrigger", type: "button", title: "Evaluate & Set Active Mode Now"
        }

        /* Section 5: Notifications */
        section("<b>SECTION 5: Notification(s) & Alert Preferences</b>", hideable: true, hidden: isCollapsed) {
            input name: "enableNotifications", type: "bool", title: "Enable Notifications?", defaultValue: true, submitOnChange: true

            if (getSettingBool("enableNotifications", true)) {
                input name: "notificationDevice", type: "capability.notification", title: "Push Notification Device(s)", required: false, multiple: true
                input name: "speechDevice", type: "capability.speechSynthesis", title: "Audio / Speech Device(s)", required: false, multiple: true

                input name: "notifyOnNormal", type: "bool", title: "Notify on Normal Schedule Transitions?", defaultValue: false, submitOnChange: true
                if (getSettingBool("notifyOnNormal", false)) {
                    input name: "templateNormal", type: "text", title: "Normal Template", defaultValue: "Mode scheduled transition: %mode% (from %prevMode% at %time%)", required: true
                }

                input name: "notifyOnVoice", type: "bool", title: "Notify on Voice / Alexa Triggers?", defaultValue: true, submitOnChange: true
                if (getSettingBool("notifyOnVoice", true)) {
                    input name: "templateVoice", type: "text", title: "Voice Template", defaultValue: "Voice command changed mode to %mode% (Reason: %reason% at %time%)", required: true
                }

                input name: "notifyOnPresence", type: "bool", title: "Notify on Presence Changes?", defaultValue: true, submitOnChange: true
                if (getSettingBool("notifyOnPresence", true)) {
                    input name: "templatePresence", type: "text", title: "Presence Template", defaultValue: "Presence update changed mode to %mode% (was %prevMode% at %time%)", required: true
                }

                input name: "notifyOnOverride", type: "bool", title: "Notify on Manual Override Changes?", defaultValue: true, submitOnChange: true
                if (getSettingBool("notifyOnOverride", true)) {
                    input name: "templateOverride", type: "text", title: "Override Template", defaultValue: "Manual Override activated: Mode set to %mode% (Reason: %reason% at %time%)", required: true
                }

                input name: "suppressAudioWhenSleeping", type: "bool", title: "Mute Audio/Speech devices while Mode is 'Sleeping'?", defaultValue: true
            }
        }

        section("<b>App Preferences</b>", hideable: true, hidden: isCollapsed) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            input name: "showModeInLabel", type: "bool", title: "Show Active Mode in App Label?", defaultValue: true
            input name: "showReasonInLabel", type: "bool", title: "Show Evaluation Reason in App Label?", defaultValue: true
            input name: "tileFormat", type: "enum", title: "Dashboard Tile Format", options: ["plain": "Plain Text", "html": "Formatted HTML"], defaultValue: "plain", required: true
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
            input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false
        }
        
        state.sectionsExpanded = true
    }
}

/* =========================================================================================
   APPLICATION LIFECYCLE
   ========================================================================================= */

def installed() {
    state.sectionsExpanded = false
    seedLoggingState()
    state.lastManageHSM = getSettingBool("manageHSM", true)
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize()
    recheckSchedule("App Installed")
}

def uninstalled() {
    unsubscribe()
    stopPeriodSchedules()
    unschedule()
}

def updated() {
    state.sectionsExpanded = false
    checkLoggingChanges()
    checkHsmSettingChanges()
    unsubscribe()
    
    unschedule("disableDebugLogging")
    if (getSettingBool("logDebugEnable", true)) {
        runIn(1800, disableDebugLogging)
    }
    
    initialize()
    
    String currentSnapshot = captureSettingsSnapshot()
    String previousSnapshot = state.lastSettingsSnapshot
    Boolean settingsChanged = (previousSnapshot != null && previousSnapshot != currentSnapshot)
    state.lastSettingsSnapshot = currentSnapshot

    if (settingsChanged) {
        updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Normal"])
        recheckSchedule("App Preferences Modified")
    } else {
        updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Normal"])
    }
}

private String captureSettingsSnapshot() {
    List<String> watchKeys = [
        "masterPresence", "homeSwitch", "awaySwitch", "awakeSwitch", "sleepSwitch",
        "sleepMode", "timeWeeHours", "weeHoursMode", "timeEarlyMorning", "earlyMorningMode",
        "timeMorning", "morningMode", "timeDay", "dayMode", "timeEvening", "eveningMode",
        "timeLateEvening", "lateEveningMode", "holdOverride", "suspendScheduler", "manageHSM",
        "alexaModeSwitch", "alexaAwakeSwitch", "statusTileDevice", "logInfoEnable", "logDebugEnable", "logTraceEnable"
    ]
    Map snapshot = [:]
    watchKeys.each { key ->
        def val = settings[key]
        snapshot[key] = (val instanceof List) ? val.collect { it.toString() } : val?.toString()
    }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
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
    if (currentManageHSM != lastManageHSM && currentManageHSM) {
        state.lastHsmState = null
    }
    state.lastManageHSM = currentManageHSM
}

private void checkLoggingChanges() {
    Boolean currentInfo  = getSettingBool("logInfoEnable", true)
    Boolean currentDebug = getSettingBool("logDebugEnable", true)
    Boolean currentTrace = getSettingBool("logTraceEnable", false)
    state.lastLogInfoEnable  = currentInfo
    state.lastLogDebugEnable = currentDebug
    state.lastLogTraceEnable = currentTrace
}

def initialize() {
    state.activeTransaction = null
    state.lastTriggerTime = 0
    if (atomicState.pendingOutputSyncs == null) atomicState.pendingOutputSyncs = [:]
    if (!state.modeReason) state.modeReason = "Normal"
    if (getSettingBool("manageHSM", true) && !state.lastHsmState) state.lastHsmState = location.hsmStatus ?: "disarmed"

    subscribe(location, "systemStart", hubStartupHandler)

    if (masterPresence) subscribe(masterPresence, "presence", presenceHandler)
    if (homeSwitch) subscribe(homeSwitch, "switch", homeSwitchHandler)
    if (awaySwitch) subscribe(awaySwitch, "switch", awaySwitchHandler)
    if (awakeSwitch) subscribe(awakeSwitch, "switch", awakeSwitchHandler)
    if (sleepSwitch) subscribe(sleepSwitch, "switch", sleepSwitchHandler)
    if (alexaAwakeSwitch) subscribe(alexaAwakeSwitch, "switch", alexaAwakeSwitchHandler)
    if (alexaModeSwitch) subscribe(alexaModeSwitch, "switch", alexaModeSwitchHandler)
    
    (updateTriggerSwitch ? [updateTriggerSwitch].flatten() : []).each { dev -> subscribe(dev, "switch.on", updateSwitchHandler) }
    (updateTriggerButton ? [updateTriggerButton].flatten() : []).each { dev -> subscribe(dev, "pushed", updateButtonHandler) }

    [vSwitchWeeHours, vSwitchEarlyMorning, vSwitchMorning, vSwitchDay, vSwitchEvening, vSwitchLateEvening].each { vSwitch ->
        if (vSwitch) subscribe(vSwitch, "switch.on", vSwitchHandler)
    }

    if (state.modeReason == "Override" && getSettingBool("suspendScheduler", false)) {
        stopPeriodSchedules()
    } else {
        restartPeriodSchedules()
    }
}

def hubStartupHandler(evt = null) {
    state.lastNotifiedMode = null
    initialize()
    processStatePipeline([reason: "Reboot", source: "Hub System Startup"])
}

def restartPeriodSchedules() {
    stopPeriodSchedules()
    startPeriodSchedules()
}

def startPeriodSchedules() {
    schedulePeriodTime(timeWeeHours, "periodWeeHoursHandler")
    schedulePeriodTime(timeEarlyMorning, "periodEarlyMorningHandler")
    schedulePeriodTime(timeMorning, "periodMorningHandler")
    schedulePeriodTime(timeDay, "periodDayHandler")
    schedulePeriodTime(timeEvening, "periodEveningHandler")
    schedulePeriodTime(timeLateEvening, "periodLateEveningHandler")
}

def stopPeriodSchedules() {
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
        return null
    }
}

def schedulePeriodTime(String timeIso, String handlerMethod) {
    if (timeIso && handlerMethod) {
        String cronExpr = toCronExpression(timeIso)
        if (cronExpr) schedule(cronExpr, handlerMethod)
    }
}

def periodWeeHoursHandler()     { handlePeriodBoundary("weeHours") }
def periodEarlyMorningHandler() { handlePeriodBoundary("earlyMorning") }
def periodMorningHandler()      { handlePeriodBoundary("morning") }
def periodDayHandler()          { handlePeriodBoundary("day") }
def periodEveningHandler()      { handlePeriodBoundary("evening") }
def periodLateEveningHandler()  { handlePeriodBoundary("lateEvening") }

private void handlePeriodBoundary(String periodKey) {
    if (state.modeReason == "Override" && getSettingBool("suspendScheduler", false)) return
    processStatePipeline([reason: "Normal", source: "Schedule CRON (${periodKey})", isBoundaryTrigger: true])
}

/* =========================================================================================
   CORE DECISION PIPELINE
   ========================================================================================= */

private int getReasonRank(String reason) {
    switch (reason) {
        case "Override": return 4
        case "Presence": return 3
        case "Voice":    return 2
        case "Reboot":   return 1
        case "Normal":   return 1
        default:         return 1
    }
}

private void recheckSchedule(String triggerSource) {
    processStatePipeline([reason: "Normal", source: triggerSource, isRecheck: true])
}

private void processStatePipeline(Map request) {
    String reqReason = request.reason ?: "Normal"
    String reqSource = request.source ?: "Internal Pipeline"
    Boolean isRecheck = request.isRecheck ?: false
    
    String txId = "tx_${now()}"
    state.activeTransaction = txId

    try {
        String previousMode = location.mode
        String previousReason = state.modeReason ?: "Normal"
        
        Map decision = calculateDecision(request)
        decision.txId = txId
        decision.isRecheck = isRecheck
        decision.previousModeAtStart = previousMode
        decision.previousReasonAtStart = previousReason
        
        applyDecision(decision)
        syncOutputs(decision)
    } finally {
        state.activeTransaction = null
    }
}

private Map calculateDecision(Map request) {
    String requestedReason = request.reason ?: "Normal"
    String source = request.source ?: "Unknown"
    Boolean isBoundaryTrigger = request.isBoundaryTrigger ?: false
    Boolean forceReleaseLock = request.forceReleaseLock ?: false
    
    String currentActiveReason = state.modeReason ?: "Normal"
    int incomingRank = getReasonRank(requestedReason)
    int currentRank = getReasonRank(currentActiveReason)

    Boolean isHoldOverrideEnabled = getSettingBool("holdOverride", false)

    Boolean isHome = (request.simulatedHome != null) ? request.simulatedHome : (homeSwitch?.currentValue("switch") == "on")
    Boolean isSleeping = (request.simulatedAwake != null) ? !request.simulatedAwake : (awakeSwitch?.currentValue("switch") == "off")

    if (isHome && isSleeping && currentActiveReason == "Override" && !isHoldOverrideEnabled && requestedReason != "Override") {
        currentActiveReason = "Normal"
        currentRank = getReasonRank("Normal")
    }

    if (forceReleaseLock) {
        // Reset state lock
    } else if (currentActiveReason == "Override") {
        if (isHoldOverrideEnabled && incomingRank < currentRank && !isBoundaryTrigger) {
            requestedReason = "Override"
        }
    } else if (incomingRank < currentRank) {
        if (isBoundaryTrigger) {
            requestedReason = "Normal"
        } else {
            requestedReason = currentActiveReason
        }
    }

    String targetMode = null
    String activePeriodKey = null

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
        if (!isHome) {
            targetMode = "Away"
            activePeriodKey = null
            if (requestedReason != "Reboot") requestedReason = "Presence"
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

    String configuredSleepMode = sleepMode?.toString() ?: "Sleeping"
    Boolean finalIsSleeping = (targetMode == configuredSleepMode) ? true : (isHome && isSleeping)

    return [
        reason: requestedReason,
        source: source,
        targetMode: targetMode,
        periodKey: activePeriodKey,
        isSleeping: finalIsSleeping
    ]
}

private void applyDecision(Map decision) {
    String newMode = decision.targetMode
    String newReason = decision.reason
    if (!newMode) return

    String previousReason = decision.previousReasonAtStart ?: state.modeReason ?: "Normal"
    String previousMode = decision.previousModeAtStart ?: location.mode
    Boolean suspendOnOverride = getSettingBool("suspendScheduler", false)

    if (suspendOnOverride) {
        if (newReason == "Override" && previousReason != "Override") {
            stopPeriodSchedules()
        } else if (previousReason == "Override" && newReason != "Override") {
            restartPeriodSchedules()
        }
    }

    Boolean modeChanged = (previousMode != newMode)
    Boolean reasonChanged = (previousReason != newReason)

    state.modeReason = newReason

    if (modeChanged || reasonChanged) {
        setLocationMode(newMode)
    }

    updatePresentation([targetMode: newMode, reason: newReason])
    updateStatusTileDevice(newMode, newReason)
}

private void updateStatusTileDevice(String modeVal, String reasonVal) {
    if (!statusTileDevice) return
    String timeStr = new Date().format("hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())
    try {
        if (statusTileDevice.hasCommand("setStatus")) {
            statusTileDevice.setStatus(modeVal, reasonVal, timeStr)
        } else {
            if (statusTileDevice.hasCommand("setActiveMode")) statusTileDevice.setActiveMode(modeVal)
            if (statusTileDevice.hasCommand("setActiveReason")) statusTileDevice.setActiveReason(reasonVal)
            if (statusTileDevice.hasCommand("setLastTransitionTime")) statusTileDevice.setLastTransitionTime(timeStr)
        }
    } catch (Exception e) {
        logWarn "Failed to update Status Tile Device: ${e.message}"
    }
}

private void syncOutputs(Map decision) {
    String targetMode = decision.targetMode
    String periodKey = decision.periodKey
    String reason = decision.reason
    Boolean isSleeping = (decision.isSleeping == true)

    if (targetMode == "Away") {
        syncSwitch(homeSwitch, "off")
        syncSwitch(awaySwitch, "on")
        syncSwitch(alexaModeSwitch, "off")
        
        if (getSettingBool("manageHSM", true)) {
            String currentHsm = location.hsmStatus
            String expectedHsmStatus = "armedAway"
            if (currentHsm != expectedHsmStatus && state.lastHsmState != expectedHsmStatus) {
                state.lastHsmState = expectedHsmStatus
                sendLocationEvent(name: "hsmSetArm", value: "armAway")
            }
        }
    } else {
        syncSwitch(homeSwitch, "on")
        syncSwitch(awaySwitch, "off")
        syncSwitch(alexaModeSwitch, "on")
        
        if (getSettingBool("manageHSM", true)) {
            Boolean isSleepingOrNight = (isSleeping || periodKey == "weeHours")
            String expectedHsmStatus = isSleepingOrNight ? "armedNight" : "disarmed"
            String hsmCmd = isSleepingOrNight ? "armNight" : "disarm"
            String currentHsm = location.hsmStatus

            if (currentHsm != expectedHsmStatus && state.lastHsmState != expectedHsmStatus) {
                state.lastHsmState = expectedHsmStatus
                sendLocationEvent(name: "hsmSetArm", value: hsmCmd)
            }
        }
    }

    if (isSleeping) {
        syncSwitch(awakeSwitch, "off")
        syncSwitch(sleepSwitch, "on")
        syncSwitch(alexaAwakeSwitch, "off")
    } else if (targetMode != "Away") {
        syncSwitch(awakeSwitch, "on")
        syncSwitch(sleepSwitch, "off")
        syncSwitch(alexaAwakeSwitch, "on")
    }

    updateVirtualModeSwitches(periodKey)
    
    Boolean modeChanged = (decision.previousModeAtStart != targetMode)
    Boolean reasonChanged = (decision.previousReasonAtStart != reason)

    dispatchTileNotification(targetMode, reason, isSleeping, modeChanged, reasonChanged)
}

private void syncSwitch(def device, String targetState) {
    if (device != null && device.hasCommand(targetState)) {
        if (device.currentValue("switch") != targetState) {
            String devId = device.id.toString()
            String compositeKey = "${devId}:${targetState}"

            long currentMs = now()
            Map pendingMap = (atomicState.pendingOutputSyncs != null) ? new HashMap(atomicState.pendingOutputSyncs) : [:]

            pendingMap.entrySet().removeIf { entry ->
                Map marker = entry.value as Map
                return (marker == null || currentMs > (marker.expires as long))
            }

            pendingMap[compositeKey] = [devId: devId, value: targetState, expires: currentMs + 5000]
            atomicState.pendingOutputSyncs = pendingMap
            device."${targetState}"()
        }
    }
}

private void updateVirtualModeSwitches(String activePeriodKey) {
    List<Map> periodSwitchList = [
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

private void dispatchTileNotification(String modeVal, String reasonVal, Boolean isSleepingState = false, Boolean modeChanged = true, Boolean reasonChanged = true) {
    if (!getSettingBool("enableNotifications", true)) return

    if (reasonVal != "RecheckNoChange" && state.lastNotifiedMode == modeVal && !reasonChanged && reasonVal != "Reboot") {
        return
    }

    Boolean shouldNotify = false
    String template = null

    if (!modeChanged && reasonChanged && settings.templateReasonOnly) {
        shouldNotify = true
        template = settings.templateReasonOnly
    } else if (modeChanged && !reasonChanged && settings.templateModeOnly) {
        shouldNotify = true
        template = settings.templateModeOnly
    } else {
        switch (reasonVal) {
            case "Normal":   
                shouldNotify = getSettingBool("notifyOnNormal", false)
                template = settings.templateNormal ?: "Mode scheduled transition: %mode% (from %prevMode% at %time%)"
                break
            case "Voice":    
                shouldNotify = getSettingBool("notifyOnVoice", true)
                template = settings.templateVoice ?: "Voice command changed mode to %mode% (Reason: %reason% at %time%)"
                break
            case "Presence": 
                shouldNotify = getSettingBool("notifyOnPresence", true)
                template = settings.templatePresence ?: "Presence update changed mode to %mode% (was %prevMode% at %time%)"
                break
            case "Override": 
                shouldNotify = getSettingBool("notifyOnOverride", true)
                template = settings.templateOverride ?: "Manual Override activated: Mode set to %mode% (Reason: %reason% at %time%)"
                break
            case "Reboot":
                shouldNotify = getSettingBool("notifyOnReboot", true)
                template = settings.templateReboot ?: "System restarted following hub boot. Mode synchronized to %mode% (Reason: %reason% at %time%)"
                break
            case "RecheckNoChange":
                shouldNotify = getSettingBool("notifyOnRecheckNoChange", false)
                template = settings.templateRecheckNoChange ?: "Mode recheck completed: Still in %mode% (%reason%) at %time%"
                break
            default:         
                shouldNotify = true
                template = "Location Mode changed to %mode% (Reason: %reason% at %time%)"
                break
        }
    }

    if (!shouldNotify) return

    String timeStr = new Date().format("hh:mm a", location.timeZone ?: TimeZone.getDefault())
    String prevMode = state.previousMode ?: "Unknown"
    String prevReason = state.previousReason ?: "Unknown"
    String displayReason = (reasonVal == "RecheckNoChange") ? (state.modeReason ?: "Normal") : reasonVal

    String formattedMsg = template
        .replace("%mode%", modeVal ?: "Unknown")
        .replace("%reason%", displayReason)
        .replace("%prevMode%", prevMode)
        .replace("%prevReason%", prevReason)
        .replace("%time%", timeStr)

    state.previousMode = modeVal
    state.previousReason = displayReason
    
    if (reasonVal != "RecheckNoChange") {
        state.lastNotifiedMode = modeVal
    }

    if (notificationDevice) {
        String pushMsg = (settings.tileFormat == "html") ?
            "<div style='font-size:13px; font-weight:bold; color:#27AE60;'>${formattedMsg}</div>" : formattedMsg
        
        notificationDevice.each { dev -> dev.deviceNotification(pushMsg) }
    }

    if (speechDevice) {
        // Evaluate isSleeping strictly against incoming decision state
        Boolean isSleeping = (isSleepingState == true)
        if (isSleeping && getSettingBool("suppressAudioWhenSleeping", true)) {
            logTrace "Audio notification suppressed because system target state is Sleeping."
        } else {
            String plainSpeechMsg = formattedMsg.replaceAll("<[^>]*>", "")
            speechDevice.each { dev -> dev.speak(plainSpeechMsg) }
        }
    }
}

private void updatePresentation(Map decision) {
    updateAppLabel(decision.targetMode ?: location.mode, decision.reason ?: state.modeReason)
}

private void updateAppLabel(String currentMode = null, String reason = null) {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    Boolean showMode    = getSettingBool("showModeInLabel", true)
    Boolean showReason  = getSettingBool("showReasonInLabel", true)

    String baseLabel = "Mode Manager Advanced"
    if (showVersion) baseLabel += " v${version()}"

    String displayMode = currentMode ?: location.mode ?: "Unknown"
    String currentReason = reason ?: state.modeReason ?: "Normal"

    List<String> badgeParts = []
    if (showMode) badgeParts.add("<span style='color:green; font-weight:bold;'>${displayMode}</span>")
    if (showReason) badgeParts.add("(${currentReason})")

    String formattedLabel = baseLabel
    if (!badgeParts.isEmpty()) formattedLabel += " - [" + badgeParts.join(" ") + "]"
    if (app.label != formattedLabel) app.updateLabel(formattedLabel)
}

private boolean isInternalTransaction(def evt = null) {
    if (state.activeTransaction != null) return true

    if (evt != null) {
        def rawDevId = evt.deviceId ?: evt.device?.id
        if (rawDevId != null) {
            String devId = rawDevId.toString()
            String evtVal = evt.value?.toString()
            String compositeKey = "${devId}:${evtVal}"

            Map pendingMap = (atomicState.pendingOutputSyncs != null) ? new HashMap(atomicState.pendingOutputSyncs) : [:]

            if (pendingMap.containsKey(compositeKey)) {
                Map marker = pendingMap[compositeKey] as Map
                long currentMs = now()

                if (marker && marker.value == evtVal && currentMs <= (marker.expires as long)) {
                    pendingMap.remove(compositeKey)
                    atomicState.pendingOutputSyncs = pendingMap
                    return true
                } else if (marker && currentMs > (marker.expires as long)) {
                    pendingMap.remove(compositeKey)
                    atomicState.pendingOutputSyncs = pendingMap
                }
            }
        }
    }

    return false
}

def appButtonHandler(btn) {
    String btnName = "${btn}".toString()
    if (btnName == "btnTrigger") {
        long currentMs = now()
        if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
        state.lastTriggerTime = currentMs
        recheckSchedule("Manual UI Button ('Evaluate & Set Active Mode Now')")
    } else if (btnName == "btnForceTestEvaluation") {
        executeForcedTestEvaluation()
    }
}

def updateSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
    state.lastTriggerTime = currentMs
    recheckSchedule("Trigger Switch '${evt.device?.displayName}'")
}

def updateButtonHandler(evt) {
    if (isInternalTransaction(evt)) return
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
    state.lastTriggerTime = currentMs
    recheckSchedule("Trigger Button '${evt.device?.displayName}'")
}

def vSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    if (evt.value != "on") return

    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 1000)) return
    state.lastTriggerTime = currentMs

    def rawDevId = evt.deviceId ?: evt.device?.id
    if (rawDevId == null) return
    String deviceId = "${rawDevId}".toString()

    if ((sleepSwitch && "${sleepSwitch.id}".toString() == deviceId) || (awakeSwitch && "${awakeSwitch.id}".toString() == deviceId)) return

    Map<String, Map> switchIdToPeriodMap = [:]
    if (vSwitchWeeHours)     switchIdToPeriodMap["${vSwitchWeeHours.id}".toString()]     = [mode: weeHoursMode?.toString(),     key: "weeHours"]
    if (vSwitchEarlyMorning) switchIdToPeriodMap["${vSwitchEarlyMorning.id}".toString()] = [mode: earlyMorningMode?.toString(), key: "earlyMorning"]
    if (vSwitchMorning)      switchIdToPeriodMap["${vSwitchMorning.id}".toString()]      = [mode: morningMode?.toString(),      key: "morning"]
    if (vSwitchDay)          switchIdToPeriodMap["${vSwitchDay.id}".toString()]          = [mode: dayMode?.toString(),          key: "day"]
    if (vSwitchEvening)      switchIdToPeriodMap["${vSwitchEvening.id}".toString()]      = [mode: eveningMode?.toString(),      key: "evening"]
    if (vSwitchLateEvening)  switchIdToPeriodMap["${vSwitchLateEvening.id}".toString()]  = [mode: lateEveningMode?.toString(),  key: "lateEvening"]

    Map targetPeriod = switchIdToPeriodMap[deviceId]
    
    if (targetPeriod?.mode) {
        processStatePipeline([
            reason: "Override",
            targetMode: targetPeriod.mode,
            periodKey: targetPeriod.key,
            source: "Virtual Switch (${evt.device?.displayName})"
        ])
    }
}

def presenceHandler(evt) {
    if (isInternalTransaction(evt)) return
    processStatePipeline([reason: "Presence", presenceValue: evt.value, source: "Master Presence Sensor"])
}

def homeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String presenceVal = (evt.value == "on") ? "present" : "not present"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Home Switch"])
}

def awaySwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String presenceVal = (evt.value == "off") ? "present" : "not present"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Away Switch"])
}

def alexaModeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
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
    recheckSchedule("Awake Switch '${evt.device?.displayName}'")
}

def sleepSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String targetAwakeState = (evt.value == "on") ? "off" : "on"
    if (awakeSwitch) {
        if (awakeSwitch.currentValue("switch") != targetAwakeState) {
            awakeSwitch."${targetAwakeState}"()
        }
    } else {
        recheckSchedule("Sleep Switch '${evt.device?.displayName}'")
    }
}

def alexaAwakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    
    if (evt.value == "off") {
        processStatePipeline([
            reason: "Voice",
            targetMode: sleepMode?.toString() ?: "Sleeping",
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

def executeForcedTestEvaluation() {
    Boolean isHomeTarget = getSettingBool("testHome", true)
    Boolean isAwakeTarget = getSettingBool("testAwake", true)
    String forcedPeriodKey = settings.testPeriodKey ?: "day"
    String targetReason = settings.testReason ?: "Override"

    processStatePipeline([
        reason: targetReason,
        simulatedHome: isHomeTarget,
        simulatedAwake: isAwakeTarget,
        simulatedPeriodKey: forcedPeriodKey,
        source: "Section 4 Simulation Control Panel",
        forceReleaseLock: true
    ])
}

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
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appName = app.label ?: 'Mode Manager Advanced'
    if (lowerLevel == "warn") { log.warn "${appName} WARNING: ${msg}"; return }
    if (lowerLevel == "error") { log.error "${appName} ERROR: ${msg}"; return }
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    if (getSettingBool(settingKey, false)) log."${lowerLevel}" "${appName}: ${msg}"
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }