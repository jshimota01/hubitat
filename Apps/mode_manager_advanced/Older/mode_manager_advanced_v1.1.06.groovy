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
 * v1.1.06 (2026-08-21) - In-Memory High-Performance Suppression Architecture:
 *                        - Replaced atomicState pendingOutputSyncs database serialization with a thread-safe heap-level ConcurrentHashMap (@Field).
 *                        - Implemented single-use sub-millisecond suppression checks with immediate memory eviction to prevent thread-racing.
 *                        - Reduced suppression guard window to a tight 1,500ms bus broadcast window, eliminating long time-delay locks.
 *                        - Enforced isInternalTransaction() as the primary line guard across all event handlers.
 * v1.1.05 (2026-08-21) - Pipeline & Exception Bug Fixes:
 *                        - Relocated isInternalTransaction() to be the primary guard in vSwitchHandler() to prevent self-triggered Override pipeline shifts.
 *                        - Removed invalid evt.isStateChange() call in hsmStatusHandler() to resolve MissingMethodException.
 * v1.1.04 (2026-08-21) - Application Version Upgrade Logging Guard:
 *                        - Added state.installedVersion tracking to installed() and updated().
 *                        - Added high-visibility logWarn alert in updated() whenever a code update changes the app version.
 * v1.1.03 (2026-08-21) - HSM Status Tile Rendering:
 *                        - Added active HSM status formatting and state rendering to updateStatusTileDevice().
 *                        - Added real-time tile device refreshes on external HSM status state changes in hsmStatusHandler().
 * v1.1.02 (2026-08-21) - Structural Execution Pipeline & Diagnostic Refactoring:
 *                        - Resolved sequence race condition by deferring sendLocationEvent to final phase of syncOutputs().
 *                        - Fixed premature tile update by relocating updateStatusTileDevice() to Step 2 of syncOutputs().
 *                        - Added verifyHsmSync retry guard and comprehensive trace/debug instrumentation across HSM & Tile outputs.
 * v1.1.01 (2026-08-19) - Tile Notification Formatting:
 *                        - Added 'white-space: nowrap; overflow-x: auto;' to HTML push notification output wrapper
 *                          to prevent line wrapping on Notification Tiles while allowing horizontal scrolling.
 * v1.1.00 (2026-08-19) - Runtime Robustness & Concurrency Protection:
 *                        - Converted lastTriggerTimes to atomicState to guarantee thread safety during rapid multi-device event bursts.
 *                        - Added parseBigDecimalSafely() and parseIntSafely() in delayedStartupHandler() to prevent NumberFormatException during early hub boot.
 *                        - Added null guard around awakeSwitch evaluation in calculateDecision() to avoid NPE risks.
 * v1.0.1  (2026-05-20) - Initial Architectural Concept:
 *                        - Initial release of Mode Manager Advanced framework for Hubitat Elevation.
 *
 */

import java.util.concurrent.ConcurrentHashMap
import groovy.transform.Field

@Field static ConcurrentHashMap<String, Long> internalEventSuppressionMap = new ConcurrentHashMap<>()

static String version() { return '1.1.06' }

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
        
        Boolean isCollapsed = (state.sectionsExpanded == true) ? false : true
        state.sectionsExpanded = false
        
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

        /* CATEGORY A: INPUT ARCHITECTURE */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:10px;'>CATEGORY A: INPUT ARCHITECTURE (Presence, Sleep, Schedule)</div>") {}

        section("<b>SECTION 1: Presence Architecture & State Switches</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Presence Architecture:</b> Master Presence Sensor updates Home Switch (Primary Authoritative Input). Away Switch is maintained as its inverse mirror.</div>"
            
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Presence Modes:</b> When presence is <b>Away</b>, Location Mode automatically switches to <b>Away</b>. When presence returns to <b>Home</b>, the app automatically evaluates your active Section 3 Time Period schedule (or Section 2 Sleep state). No manual mode assignment is needed for presence.</div>"

            input name: "masterPresence", type: "capability.presenceSensor", title: "<b>Master Presence Sensor</b> <i>(OwnTracks - Jim)</i>", required: true
            input name: "homeSwitch", type: "capability.switch", title: "<b>Home Switch</b> <i>(Primary Input)</i>", required: true
            input name: "awaySwitch", type: "capability.switch", title: "<b>Away Switch</b> <i>(Inverse Mirror)</i>", required: true
        }
        
        section("<b>SECTION 2: Sleep Architecture & Behavioral Overlay</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Sleep Architecture:</b> <b>Awake Switch</b> is the single authoritative input driving sleep evaluations. The <b>Sleeping Switch</b> is maintained automatically as a passive inverse mirror.</div>"
            
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Sleep Modes:</b> Sleeping is a behavioral state, not a chronological period schedule. When <b>Sleeping</b> (Awake Switch = OFF), the app overrides the time schedule and enforces the target mode selected below. When <b>Awake</b> (Awake Switch = ON), the sleep overlay releases and your system automatically resumes the Section 3 Time Period schedule.</div>"

            input name: "awakeSwitch", type: "capability.switch", title: "<b>Awake Switch</b> <i>(Primary Authoritative Input: ON = Awake, OFF = Sleeping)</i>", required: true
            input name: "sleepSwitch", type: "capability.switch", title: "<b>Sleeping Switch</b> <i>(Passive Inverse Mirror Output — Dashboard Indicator)</i>", required: false
            
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-6px; margin-bottom:10px;'>" +
                      "<i><b>Note:</b> You do not need to trigger rules with the Sleeping Switch. The app automatically maintains it as the inverse of your Awake Switch and uses the Awake Switch for all mode decisions.</i></div>"

            input name: "sleepMode", type: "mode", title: "<b>Target Mode when Sleeping</b>", required: true, defaultValue: "Sleeping"
        }

        section("<b>SECTION 3: Time Period & Target Mode Schedule</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Schedule Architecture:</b> Dynamic chronological time periods evaluated sequentially throughout the day when Home and Awake.</div>"
            
            Map activePeriod = getActiveTimePeriodInfo()
            String activePeriodKey = activePeriod?.key ?: "Unknown"

            Boolean isSleeping = (awakeSwitch ? (awakeSwitch.currentValue("switch") == "off") : false)
            String overlayText = isSleeping ? " <span style='color:#8E44AD; font-weight:bold;'>(Overridden by Sleeping Overlay)</span>" : ""
            
            paragraph "<div style='background-color:#EAEDED; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "⏱️ <b>Current Schedule Block:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodKey.toUpperCase()}</span> " +
                      "(Schedule Target: <b>${activePeriod?.mode ?: 'None'}</b>)${overlayText}</div>"

            input name: "timeWeeHours", type: "time", title: "Wee Hours Start", required: true, defaultValue: "00:30", width: 6
            input name: "weeHoursMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Late Evening", width: 3
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

        /* CATEGORY B: SYSTEM INTEGRATIONS */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY B: SYSTEM INTEGRATIONS (HSM & Alexa)</div>") {}

        section("<b>SECTION 4: Hubitat Safety Monitor (HSM) Integration</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>HSM Safety Automation:</b> Hubitat Safety Monitor is driven by Mode Manager's combined state engine (Presence + Sleep Overlay + Time Schedule).</div>"

            input name: "manageHSM", type: "bool", title: "<b>Manage Hubitat Safety Monitor automatically?</b>", defaultValue: true, submitOnChange: true

            if (getSettingBool("manageHSM", true)) {
                String currentHsm = location.hsmStatus ?: "disarmed"
                String pendingHsm = state.pendingHsmTarget

                Boolean isHome = (homeSwitch ? (homeSwitch.currentValue("switch") == "on") : true)
                Boolean isSleeping = (awakeSwitch ? (awakeSwitch.currentValue("switch") == "off") : false)
                Map activePeriod = getActiveTimePeriodInfo()
                String periodKey = activePeriod?.key

                String expectedHsm
                if (!isHome) {
                    expectedHsm = "armedAway"
                } else {
                    Boolean isSleepingOrNight = (isSleeping || periodKey == "weeHours")
                    expectedHsm = isSleepingOrNight ? "armedNight" : "disarmed"
                }

                String hsmBadge
                if (pendingHsm != null) {
                    hsmBadge = "<span style='color:#D35400; font-weight:bold;'>Transitioning to ${pendingHsm}...</span>"
                } else if (currentHsm == expectedHsm) {
                    hsmBadge = "<span style='color:#27AE60; font-weight:bold;'>Synchronized (${currentHsm})</span>"
                } else {
                    hsmBadge = "<span style='color:#C0392B; font-weight:bold;'>Desynchronized (Current: ${currentHsm} | Expected: ${expectedHsm})</span>"
                }

                paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #2980B9; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                          "<b>Current HSM Status:</b> ${hsmBadge}<br/>" +
                          "• <b>Arm Away:</b> Enforced when Master Presence is <b>Away</b>.<br/>" +
                          "• <b>Arm Night:</b> Enforced when Home and <b>Sleeping</b>, or during <b>Wee Hours</b>.<br/>" +
                          "• <b>Disarm:</b> Enforced when Home, <b>Awake</b>, and in daytime schedule periods.</div>"
            }
        }

        section("<b>SECTION 5: Alexa Ecosystem & Voice Marker Integration</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Alexa Ecosystem Sync:</b> Virtual switches for bidirectional status synchronization and voice command attribution.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Mode & Presence Integration</span>"
            input name: "alexaModeSwitch", type: "capability.switch", title: "Alexa Mode Virtual Switch <i>(ON = Home, OFF = Away)</i>", required: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Sleep & Awake Integration</span>"
            input name: "alexaAwakeSwitch", type: "capability.switch", title: "Alexa Awake Virtual Switch <i>(ON = Awake, OFF = Sleeping)</i>", required: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Voice Marker Attribution Signal</span>"
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Voice Marker Signal:</b> Pulse this switch ON in Alexa Routines to give Mode Manager a 5-second window to attribute toggles as <b>Voice</b> triggers.</div>"
            input name: "alexaVoiceMarkerSwitch", type: "capability.switch", title: "<b>Alexa - Voice Marker Source Switch</b>", required: false
        }

        /* CATEGORY C: OUTPUTS & NOTIFICATION ENGINE */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY C: OUTPUTS & NOTIFICATION ENGINE (Tiles, Push, Audio)</div>") {}

        section("<b>SECTION 6: Status Tiles & Notification Alerts</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Outputs Engine:</b> Select virtual status devices for dynamic dashboard rendering and configure push/speech notification alerts.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Ground-Truth Virtual Tile Status Device</span>"
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Select a virtual device powered by the " +
                      "<a href='https://github.com/jshimota01/hubitat/blob/main/Drivers/virtual_mode_status_tracker_tile/virtual_mode_status_tracker_tile_device.groovy' target='_blank' style='color:#2980B9; font-weight:bold; text-decoration:underline;'>" +
                      "Virtual Mode Status & State Tracker Tile Driver</a> to receive real-time status updates.</div>"

            input name: "statusTileDevice", type: "capability.actuator", title: "<b>Virtual Ground-Truth Status Tile Device</b>", required: false, multiple: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Push & Audio Notifications</span>"
            
            input name: "enableNotifications", type: "bool", title: "<b>Enable Notifications?</b>", defaultValue: true, submitOnChange: true

            if (getSettingBool("enableNotifications", true)) {
                input name: "notificationDevice", type: "capability.notification", title: "<b>Push Notification Device(s)</b>", required: false, multiple: true
                input name: "speechDevice", type: "capability.speechSynthesis", title: "<b>Audio / Speech Device(s)</b>", required: false, multiple: true
                input name: "notificationFormat", type: "enum", title: "<b>Push Notification Text Format</b>", options: ["plain": "Plain Text", "html": "Formatted HTML"], defaultValue: "plain", required: false
                
                input name: "notifyOnScheduled", type: "bool", title: "Notify on <b>Scheduled Transitions</b>?", defaultValue: false
                input name: "notifyOnVoice", type: "bool", title: "Notify on <b>Voice / Alexa Triggers</b>?", defaultValue: true
                input name: "notifyOnPresence", type: "bool", title: "Notify on <b>Presence Changes</b>?", defaultValue: true
                input name: "notifyOnOverride", type: "bool", title: "Notify on Manual <b>Override Changes</b>?", defaultValue: true
                input name: "notifyOnReboot", type: "bool", title: "Notify on <b>Hub Reboot / Startup Synchronization</b>?", defaultValue: true
                input name: "notifyOnRecheckNoChange", type: "bool", title: "Notify on <b>Mode Rechecks when Mode AND Reason are Unchanged</b>?", defaultValue: false

                input name: "templateScheduled", type: "text", title: "<b>Scheduled Message</b>", defaultValue: "Mode scheduled transition: %mode% (from %prevMode% at %time%)", required: true
                input name: "templateVoice", type: "text", title: "<b>Voice Trigger Message</b>", defaultValue: "Voice command changed mode to %mode% (Reason: %reason% at %time%)", required: true
                input name: "templatePresence", type: "text", title: "<b>Presence Change Message</b>", defaultValue: "Presence update changed mode to %mode% (was %prevMode% at %time%)", required: true
                input name: "templateOverride", type: "text", title: "<b>Manual Override Message</b>", defaultValue: "Manual Override activated: Mode set to %mode% (Reason: %reason% at %time%)", required: true
                input name: "templateReboot", type: "text", title: "<b>Hub Reboot Message</b>", defaultValue: "System restarted following hub boot. Mode synchronized to %mode% (Reason: %reason% at %time%)", required: true
                input name: "templateRecheckNoChange", type: "text", title: "<b>Recheck Message</b>", defaultValue: "Mode recheck completed: Still in %mode% (%reason%) at %time%", required: true
                input name: "templateReasonOnly", type: "text", title: "<b>Reason Changed Only Message</b>", defaultValue: "Evaluation Reason updated to %reason% (Mode remains %mode% at %time%)", required: false
                input name: "templateModeOnly", type: "text", title: "<b>Mode Changed Only Message</b>", defaultValue: "Mode changed to %mode% (Reason remains %reason% at %time%)", required: false

                input name: "suppressAudioWhenSleeping", type: "bool", title: "<b>Mute Audio/Speech devices while Mode is 'Sleeping'?</b>", defaultValue: true
            }
        }

        /* CATEGORY D: DIAGNOSTICS & CONTROL ENGINE */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY D: DIAGNOSTICS & CONTROL ENGINE</div>") {}

        section("<b>SECTION 7: System Diagnostics, Overrides & Simulation Panel</b>", hideable: true, hidden: isCollapsed) {
            input name: "holdOverride", type: "bool", title: "<b>Lock Override against lower-priority events?</b>", defaultValue: false
            input name: "suspendScheduler", type: "bool", title: "<b>Suspend CRON time period schedule during Override?</b>", defaultValue: false

            input name: "updateTriggerSwitch", type: "capability.switch", title: "Switch(s) to Trigger Evaluation / Update", required: false, multiple: true
            input name: "updateTriggerButton", type: "capability.pushableButton", title: "Button(s) to Trigger Evaluation / Update", required: false, multiple: true

            input name: "testHome", type: "bool", title: "<b>Force Home Status?</b>", defaultValue: true
            input name: "testAwake", type: "bool", title: "<b>Force Awake Status?</b>", defaultValue: true
            input name: "testPeriodKey", type: "enum", title: "<b>Select Period for Simulation</b>", required: false, options: ["weeHours": "Wee Hours", "earlyMorning": "Early Morning", "morning": "Morning", "day": "Day", "evening": "Evening", "lateEvening": "Late Evening"]
            input name: "testReason", type: "enum", title: "<b>Evaluation Reason for Simulation</b>", defaultValue: "Override", required: false, options: ["Override": "Override", "Scheduled": "Scheduled", "Presence": "Presence", "Voice": "Voice"]

            input name: "btnForceTestEvaluation", type: "button", title: "Force Evaluation & Sync Switches"
        }

        section("<b>SECTION 8: System Startup & Boot Guard</b>", hideable: true, hidden: isCollapsed) {
            input name: "bootSyncDelay", type: "number", title: "<b>Base Initial Boot Delay (Seconds)</b>", defaultValue: 30, required: true, range: "5..300"
            input name: "hubInfoDevice", type: "capability.actuator", title: "<b>Select Hub Information Driver v3 Device</b>", required: false, submitOnChange: true

            if (hubInfoDevice) {
                input name: "enableWatchUptime", type: "bool", title: "<b>Watch Minimum Uptime?</b>", defaultValue: true, submitOnChange: true
                if (getSettingBool("enableWatchUptime", true)) {
                    input name: "minUptimeMinutes", type: "enum", title: "Minimum Required Uptime", defaultValue: "2", required: true, options: ["1": "1 Min", "2": "2 Mins", "3": "3 Mins", "4": "4 Mins", "5": "5 Mins"]
                }
                input name: "enableWatchZigbee", type: "bool", title: "<b>Watch Zigbee Radio Status?</b>", defaultValue: true
                input name: "enableWatchZwave", type: "bool", title: "<b>Watch Z-Wave Radio Status?</b>", defaultValue: true
                input name: "enableWatchCpu", type: "bool", title: "<b>Watch CPU Load Average?</b>", defaultValue: true, submitOnChange: true
                if (getSettingBool("enableWatchCpu", true)) {
                    input name: "maxCpuThreshold", type: "enum", title: "Maximum Acceptable CPU Load", defaultValue: "2.0", required: true, options: ["1.0": "1.0", "1.5": "1.5", "2.0": "2.0", "3.0": "3.0"]
                }
            }
        }

        section("<b>App Preferences</b>", hideable: true, hidden: isCollapsed) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            input name: "showModeInLabel", type: "bool", title: "Show Active Mode in App Label?", defaultValue: true
            input name: "showReasonInLabel", type: "bool", title: "Show Evaluation Reason in App Label?", defaultValue: true

            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
            input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false
        }
        
        section() {
            input name: "btnTrigger", type: "button", title: "<b>Resynchronize System & Reset Active Mode</b>"
        }
        
        state.sectionsExpanded = true
    }
}

/* =========================================================================================
   APPLICATION LIFECYCLE & INFRASTRUCTURE SCHEDULER
   ========================================================================================= */

def installed() {
    logTrace "=== Mode Manager Adv [installed] Begins ==="
    logDebug "installed() executing v${version()}..."
    state.sectionsExpanded = false
    seedLoggingState()
    state.installedVersion = version()
    state.lastManageHSM = getSettingBool("manageHSM", true)
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    state.isSettingsSaveEvent = true
    atomicState.lastTriggerTimes = [:]
    initialize()
    recheckSchedule("App Installed")
    logTrace "=== Mode Manager Adv [installed] Ends ==="
}

def uninstalled() {
    logTrace "=== Mode Manager Adv [uninstalled] Begins ==="
    unsubscribe()
    stopPeriodSchedules()
    unschedule()
    logTrace "=== Mode Manager Adv [uninstalled] Ends ==="
}

def updated() {
    logTrace "=== Mode Manager Adv [updated] Begins ==="
    logDebug "updated() executing v${version()}..."
    
    // Version Upgrade Detector & Logging Guard
    String currentVersion = version()
    String previousVersion = state.installedVersion ?: "Unknown"
    
    if (previousVersion != currentVersion) {
        logWarn "🚀 APP VERSION CHANGED: Upgraded from v${previousVersion} -> v${currentVersion}"
        state.installedVersion = currentVersion
    }

    state.sectionsExpanded = false
    checkLoggingChanges()
    checkHsmSettingChanges()
    
    String currentSnapshot = captureSettingsSnapshot()
    String previousSnapshot = state.lastSettingsSnapshot
    Boolean settingsChanged = (previousSnapshot == null || previousSnapshot != currentSnapshot)

    if (settingsChanged) {
        logInfo "Settings modification or initial snapshot detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        state.isSettingsSaveEvent = true
        atomicState.lastTriggerTimes = [:]
        
        unsubscribe()
        unschedule("disableDebugLogging")
        if (getSettingBool("logDebugEnable", true)) {
            runIn(1800, disableDebugLogging)
        }
        
        initialize()
        updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Scheduled"])
        recheckSchedule("App Preferences Modified")
    } else {
        logInfo "App closed via Done without setting changes. Skipping re-initialization and mode recheck."
        updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Scheduled"])
    }
    logTrace "=== Mode Manager Adv [updated] Ends ==="
}

private String captureSettingsSnapshot() {
    List<String> watchKeys = [
        "masterPresence", "homeSwitch", "awaySwitch", "awakeSwitch", "sleepSwitch",
        "sleepMode", "timeWeeHours", "weeHoursMode", "timeEarlyMorning", "earlyMorningMode",
        "timeMorning", "morningMode", "timeDay", "dayMode", "timeEvening", "eveningMode",
        "timeLateEvening", "lateEveningMode",
        "vSwitchWeeHours", "vSwitchEarlyMorning", "vSwitchMorning", "vSwitchDay", "vSwitchEvening", "vSwitchLateEvening",
        "updateTriggerSwitch", "updateTriggerButton", "bootSyncDelay", "hubInfoDevice",
        "enableWatchUptime", "minUptimeMinutes", "enableWatchZigbee", "enableWatchZwave", "enableWatchCpu", "maxCpuThreshold",
        "holdOverride", "suspendScheduler", "manageHSM",
        "alexaModeSwitch", "alexaAwakeSwitch", "alexaVoiceMarkerSwitch", "statusTileDevice", "notificationFormat", "logInfoEnable", "logDebugEnable", "logTraceEnable"
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

    if (currentManageHSM != lastManageHSM) {
        logWarn "HSM Integration changed: ${lastManageHSM ? 'ENABLED' : 'DISABLED'} -> ${currentManageHSM ? 'ENABLED' : 'DISABLED'}"
        if (currentManageHSM) {
            state.lastHsmState = null
            state.pendingHsmTarget = null
            state.pendingHsmDispatchedTime = null
            state.pendingHsmDispatchedMs = null
            state.pendingHsmExpires = 0L
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

    if (currentInfo != state.lastLogInfoEnable)   logWarn "Info Logging changed: ${state.lastLogInfoEnable ? 'ENABLED' : 'DISABLED'} -> ${currentInfo ? 'ENABLED' : 'DISABLED'}"
    if (currentDebug != state.lastLogDebugEnable) logWarn "Debug Logging changed: ${state.lastLogDebugEnable ? 'ENABLED' : 'DISABLED'} -> ${currentDebug ? 'ENABLED' : 'DISABLED'}"
    if (currentTrace != state.lastLogTraceEnable) logWarn "Trace Logging changed: ${state.lastLogTraceEnable ? 'ENABLED' : 'DISABLED'} -> ${currentTrace ? 'ENABLED' : 'DISABLED'}"

    state.lastLogInfoEnable  = currentInfo
    state.lastLogDebugEnable = currentDebug
    state.lastLogTraceEnable = currentTrace
}

private Boolean isTriggerDebounced(String sourceKey, long windowMs = 2000L) {
    long currentMs = now()
    Map times = atomicState.lastTriggerTimes ? new HashMap(atomicState.lastTriggerTimes) : [:]
    cleanTriggerTimesMap(times, currentMs)
    long lastTime = (times[sourceKey] as Long) ?: 0L
    if (currentMs - lastTime < windowMs) {
        logTrace "Trigger source '${sourceKey}' debounced (${currentMs - lastTime}ms < ${windowMs}ms window)."
        return true
    }
    times[sourceKey] = currentMs
    atomicState.lastTriggerTimes = times
    return false
}

private void cleanTriggerTimesMap(Map timesMap, long currentMs) {
    if (timesMap) {
        timesMap.entrySet().removeIf { entry -> (currentMs - (entry.value as Long)) > 10000L }
    }
}

private BigDecimal parseBigDecimalSafely(def val, BigDecimal defaultVal = 0G) {
    if (val == null) return defaultVal
    try {
        String s = val.toString().replaceAll("[^0-9.]", "")
        return s ? new BigDecimal(s) : defaultVal
    } catch (Exception e) {
        return defaultVal
    }
}

private int parseIntSafely(def val, int defaultVal = 0) {
    if (val == null) return defaultVal
    try {
        String s = val.toString().replaceAll("[^0-9]", "")
        return s ? s.toInteger() : defaultVal
    } catch (Exception e) {
        return defaultVal
    }
}

def initialize() {
    logTrace "=== Mode Manager Adv [initialize] Begins ==="
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    state.activeTransaction = null
    if (atomicState.lastTriggerTimes == null) atomicState.lastTriggerTimes = [:]
    if (!state.modeReason) {
        state.modeReason = "Scheduled"
    }

    if (getSettingBool("manageHSM", true)) {
        subscribe(location, "hsmStatus", hsmStatusHandler)
        state.lastHsmState = location.hsmStatus ?: "disarmed"
        state.pendingHsmTarget = null
        state.pendingHsmDispatchedTime = null
        state.pendingHsmDispatchedMs = null
        state.pendingHsmExpires = 0L
    }

    subscribe(location, "systemStart", hubStartupHandler)

    if (masterPresence) subscribe(masterPresence, "presence", presenceHandler)
    if (homeSwitch) subscribe(homeSwitch, "switch", homeSwitchHandler)
    if (awaySwitch) subscribe(awaySwitch, "switch", awaySwitchHandler)
    if (awakeSwitch) subscribe(awakeSwitch, "switch", awakeSwitchHandler)

    if (alexaAwakeSwitch) subscribe(alexaAwakeSwitch, "switch", alexaAwakeSwitchHandler)
    if (alexaModeSwitch) subscribe(alexaModeSwitch, "switch", alexaModeSwitchHandler)
    if (alexaVoiceMarkerSwitch) subscribe(alexaVoiceMarkerSwitch, "switch", alexaVoiceMarkerSwitchHandler)
    
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
    logTrace "=== Mode Manager Adv [initialize] Ends ==="
}

def hubStartupHandler(evt = null) {
    String formattedBootTime = new Date().format("yyyy-MM-dd hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())
    state.lastHubBootTime = formattedBootTime
    state.isStartupPending = true
    state.lastNotifiedMode = null

    int delaySec = settings.bootSyncDelay ? settings.bootSyncDelay.toInteger() : 30
    logInfo "HUB STARTUP DETECTED at ${formattedBootTime}. Holding state evaluation for ${delaySec} seconds..."

    initialize()
    unschedule("delayedStartupHandler")
    runIn(delaySec, "delayedStartupHandler")
}

def delayedStartupHandler() {
    if (hubInfoDevice) {
        List<String> unreadyReasons = []

        if (getSettingBool("enableWatchUptime", true)) {
            int targetUptimeSec = (settings.minUptimeMinutes ? settings.minUptimeMinutes.toInteger() : 2) * 60
            def rawUptime = hubInfoDevice.currentValue("uptime")
            int currentUptimeSec = parseIntSafely(rawUptime, 0)
            if (currentUptimeSec < targetUptimeSec) {
                unreadyReasons.add("Uptime (${currentUptimeSec}s) < required ${targetUptimeSec}s")
            }
        }

        if (getSettingBool("enableWatchZigbee", true)) {
            String zbStatus = hubInfoDevice.currentValue("zigbeeStatus")?.toString()?.toLowerCase()
            String zbHealthy = hubInfoDevice.currentValue("zbHealthy")?.toString()?.toLowerCase()
            Boolean isZbReady = (zbStatus == "enabled" || zbStatus == "network ready" || zbStatus == "true" || zbHealthy == "true")
            if (!isZbReady) unreadyReasons.add("Zigbee Radio status '${zbStatus ?: 'unknown'}' not ready")
        }

        if (getSettingBool("enableWatchZwave", true)) {
            String zwStatus = hubInfoDevice.currentValue("zwaveStatus")?.toString()?.toLowerCase()
            Boolean isZwReady = (zwStatus == "enabled" || zwStatus == "network ready" || zwStatus == "true")
            if (!isZwReady) unreadyReasons.add("Z-Wave Radio status '${zwStatus ?: 'unknown'}' not ready")
        }

        if (getSettingBool("enableWatchCpu", true)) {
            BigDecimal maxCpu = settings.maxCpuThreshold ? new BigDecimal(settings.maxCpuThreshold) : 2.0G
            def rawCpu5 = hubInfoDevice.currentValue("cpu5Min")
            def rawCpu1 = hubInfoDevice.currentValue("cpu1Min")
            BigDecimal currentCpu = (rawCpu5 != null) ? parseBigDecimalSafely(rawCpu5) : parseBigDecimalSafely(rawCpu1)
            if (currentCpu > maxCpu) unreadyReasons.add("CPU Load (${currentCpu}) > max threshold ${maxCpu}")
        }

        if (!unreadyReasons.isEmpty()) {
            logInfo "Boot Guard: System still initializing (${unreadyReasons.join(', ')}). Deferring alignment for 15s..."
            runIn(15, "delayedStartupHandler")
            return
        }
    }

    String formattedSyncTime = new Date().format("yyyy-MM-dd hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())
    state.lastStartupSyncTime = formattedSyncTime
    state.isStartupPending = false

    logInfo "Boot stabilization window elapsed at ${formattedSyncTime}. Executing system-wide ground-truth alignment..."
    processStatePipeline([reason: "Reboot", isStartupSync: true, source: "Hub System Startup (Post-Boot Delay)"])
}

/* =========================================================================================
   DIAGNOSTIC ENHANCEMENTS & HSM ENGINE
   ========================================================================================= */

def hsmStatusHandler(evt) {
    String newHsmStatus = evt.value
    logTrace "=== Mode Manager Adv [Handler: HSM Status Update ('${newHsmStatus}')] Begins ==="
    logDebug "HSM Event Received -> Value: '${newHsmStatus}' | Source: '${evt.name}'"
    
    String previousStatus = state.lastHsmState
    state.lastHsmState = newHsmStatus
    
    if (state.pendingHsmTarget == newHsmStatus) {
        long transitTime = state.pendingHsmDispatchedMs ? (now() - (state.pendingHsmDispatchedMs as long)) : 0L
        logInfo "HSM status successfully updated to '${newHsmStatus}' (Confirmed in ${transitTime}ms)."
        logTrace "HSM Target Reached: Clearing pending state variables. (Was targeting '${state.pendingHsmTarget}')"
        state.pendingHsmTarget = null
        state.pendingHsmDispatchedTime = null
        state.pendingHsmDispatchedMs = null
        state.pendingHsmExpires = 0L
    } else if (previousStatus != newHsmStatus) {
        logInfo "HSM status updated externally to '${newHsmStatus}' (was '${previousStatus}')."
        if (state.pendingHsmTarget != null) {
            logWarn "HSM Mismatch! External event brought HSM to '${newHsmStatus}', but Mode Manager is currently waiting for '${state.pendingHsmTarget}'."
        }
    } else {
        logTrace "HSM status event received with identical state '${newHsmStatus}'. No transition required."
    }
    
    // Refresh status tile on HSM state change
    updateStatusTileDevice(location.mode, state.modeReason ?: "Scheduled")
    
    logTrace "=== Mode Manager Adv [Handler: HSM Status Update] Ends ==="
}

private void syncHsmState(String expectedHsmStatus, String hsmCmd, String reason) {
    if (!getSettingBool("manageHSM", true)) {
        logTrace "syncHsmState: HSM management is disabled in app settings. Skipping."
        return
    }

    String currentHsm = location.hsmStatus ?: "unknown"
    long currentMs = now()
    
    logTrace "syncHsmState Evaluation -> Current Location HSM: '${currentHsm}' | Expected Target: '${expectedHsmStatus}' | Command: '${hsmCmd}' | Reason: '${reason}'"

    if (currentHsm == expectedHsmStatus) {
        if (state.pendingHsmTarget != null) {
            logInfo "HSM synchronized: Currently '${currentHsm}'."
            logTrace "Clearing stale pending target '${state.pendingHsmTarget}' as location HSM matches expected status."
            state.pendingHsmTarget = null
            state.pendingHsmDispatchedTime = null
            state.pendingHsmDispatchedMs = null
            state.pendingHsmExpires = 0L
        } else {
            logTrace "HSM sync check passed: System is already '${currentHsm}'. No location event dispatch required."
        }
        return
    }

    long expiresMs = (state.pendingHsmExpires as long) ?: 0L
    Boolean isMatchingPending = (state.pendingHsmTarget == expectedHsmStatus)
    Boolean isWindowActive = (currentMs <= expiresMs)

    logTrace "HSM In-Flight Guard Inspection -> PendingTarget: '${state.pendingHsmTarget}' | MatchingPending: ${isMatchingPending} | ExpiresMs: ${expiresMs} | CurrentMs: ${currentMs} | WindowActive: ${isWindowActive}"

    if (isMatchingPending && isWindowActive) {
        long remainingMs = expiresMs - currentMs
        logDebug "HSM command '${hsmCmd}' already in-flight. Skipping redundant dispatch because target '${expectedHsmStatus}' was commanded at ${state.pendingHsmDispatchedTime ?: 'recent tx'} (${remainingMs}ms remaining in confirmation window)."
        return
    }

    if (state.pendingHsmTarget != null && !isWindowActive) {
        logWarn "HSM desynchronization timeout! HSM failed to reach '${state.pendingHsmTarget}' within 5s window. Current: '${currentHsm}'. Re-issuing command: '${hsmCmd}'"
    } else {
        logInfo "HSM state transition required. Current: '${currentHsm}' | Target: '${expectedHsmStatus}' → Dispatching location event: ${hsmCmd}"
    }

    String formattedTime = new Date().format("hh:mm:ss.SSS a", location.timeZone ?: TimeZone.getDefault())
    state.pendingHsmTarget = expectedHsmStatus
    state.pendingHsmDispatchedTime = formattedTime
    state.pendingHsmDispatchedMs = currentMs
    state.pendingHsmExpires = currentMs + 5000L
    
    logTrace "DISPATCHING LOCATION EVENT -> sendLocationEvent(name: 'hsmSetArm', value: '${hsmCmd}') [Timestamp: ${formattedTime}]"
    sendLocationEvent(name: "hsmSetArm", value: hsmCmd)
    
    logTrace "Scheduling verification check 'verifyHsmSync' in 6 seconds to confirm receipt..."
    runIn(6, "verifyHsmSync", [data: [expectedHsm: expectedHsmStatus, commandedHsm: hsmCmd, dispatchMs: currentMs]])
}

def verifyHsmSync(Map data) {
    String expected = data?.expectedHsm
    String hsmCmd = data?.commandedHsm
    long initialDispatchMs = (data?.dispatchMs as long) ?: 0L
    
    String actualHsm = location.hsmStatus ?: "unknown"
    logTrace "=== verifyHsmSync Execution -> Expected: '${expected}' | Actual: '${actualHsm}' | DispatchMs: ${initialDispatchMs} ==="

    if (actualHsm != expected) {
        logWarn "HSM VERIFICATION FAILED! Location HSM status is '${actualHsm}' 6 seconds after sending '${hsmCmd}'. Target was '${expected}'. Retrying command dispatch..."
        state.pendingHsmExpires = 0L 
        syncHsmState(expected, hsmCmd, "Verification Retry Guard")
    } else {
        logTrace "HSM Verification Guard Passed: Location HSM is confirmed at '${actualHsm}'."
    }
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
    if (state.modeReason == "Override" && getSettingBool("suspendScheduler", false)) return
    processStatePipeline([reason: "Scheduled", source: "Schedule CRON (${periodKey})", isBoundaryTrigger: true])
}

/* =========================================================================================
   CORE DECISION PIPELINE & PHASE SYNCHRONIZATION
   ========================================================================================= */

private int getReasonRank(String reason) {
    switch (reason) {
        case "Override":  return 4
        case "Presence":  return 3
        case "Voice":     return 2
        case "Reboot":    return 1
        case "Scheduled": return 1
        default:          return 1
    }
}

private void recheckSchedule(String triggerSource) {
    logInfo "Mode recheck requested by trigger source: '${triggerSource}'"
    processStatePipeline([reason: "Scheduled", source: triggerSource, isRecheck: true])
}

private void processStatePipeline(Map request) {
    String reqReason = request.reason ?: "Scheduled"
    String reqSource = request.source ?: "Internal Pipeline"
    Boolean isRecheck = request.isRecheck ?: false
    
    String txId = "tx_${now()}"
    state.activeTransaction = txId

    logTrace "=== Mode Manager Adv [Pipeline: ${reqReason} | ${reqSource}] Begins ==="

    try {
        String previousMode = location.mode
        String previousReason = state.modeReason ?: "Scheduled"
        
        Map decision = calculateDecision(request)
        decision.txId = txId
        decision.isRecheck = isRecheck
        decision.previousModeAtStart = previousMode
        decision.previousReasonAtStart = previousReason
        
        applyDecision(decision)
        syncOutputs(decision)

        logTrace "COMPLETE TRANSACTION #${txId} -> Mode: '${location.mode}' | Reason: '${state.modeReason}'"
        logTrace "=== Mode Manager Adv [Pipeline: ${reqReason} | ${reqSource}] Ends ==="
    } finally {
        state.activeTransaction = null
    }
}

private Map calculateDecision(Map request) {
    String requestedReason = request.reason ?: "Scheduled"
    String source = request.source ?: "Unknown"
    Boolean isBoundaryTrigger = request.isBoundaryTrigger ?: false
    Boolean forceReleaseLock = request.forceReleaseLock ?: false
    Boolean isStartupSync = (request.isStartupSync == true || requestedReason == "Reboot")
    
    String currentActiveReason = state.modeReason ?: "Scheduled"
    int incomingRank = getReasonRank(requestedReason)
    int currentRank = getReasonRank(currentActiveReason)

    Boolean isHoldOverrideEnabled = getSettingBool("holdOverride", false)

    Boolean isHome
    if (request.presenceValue != null) {
        isHome = (request.presenceValue == "present")
    } else if (request.simulatedHome != null) {
        isHome = request.simulatedHome
    } else {
        isHome = (homeSwitch ? (homeSwitch.currentValue("switch") == "on") : true)
    }

    Boolean isSleeping = (request.simulatedAwake != null) ? 
        !request.simulatedAwake : 
        (awakeSwitch ? (awakeSwitch.currentValue("switch") == "off") : false)

    if (isHome && isSleeping && currentActiveReason == "Override" && !isHoldOverrideEnabled && requestedReason != "Override") {
        logInfo "Behavioral Sleep state triggered while Home. Pre-empting active unlocked Override."
        currentActiveReason = "Scheduled"
        currentRank = getReasonRank("Scheduled")
    }

    if (forceReleaseLock) {
        logInfo "Force-release requested by '${source}'. Resetting active '${currentActiveReason}' state to '${requestedReason}'."
    } else if (currentActiveReason == "Override") {
        if (isHoldOverrideEnabled) {
            if (incomingRank < currentRank && !isBoundaryTrigger) {
                logInfo "Incoming request [Reason: ${requestedReason}] blocked by active 'holdOverride' Mode Lock."
                requestedReason = "Override"
            }
        } else {
            logInfo "Active 'Override' state released to incoming request [Reason: ${requestedReason}] because 'holdOverride' is OFF."
        }
    } else if (incomingRank < currentRank) {
        if (isBoundaryTrigger && !isSleeping && isHome) {
            requestedReason = "Scheduled"
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
    Boolean finalIsSleeping = (targetMode == configuredSleepMode)

    return [
        reason: requestedReason,
        source: source,
        targetMode: targetMode,
        periodKey: activePeriodKey,
        isSleeping: finalIsSleeping,
        isStartupSync: isStartupSync
    ]
}

private void applyDecision(Map decision) {
    String newMode = decision.targetMode
    String newReason = decision.reason
    if (!newMode) {
        logWarn "Decision produced null target mode. Aborting mode apply."
        return
    }

    String previousReason = decision.previousReasonAtStart ?: state.modeReason ?: "Scheduled"
    String previousMode = decision.previousModeAtStart ?: location.mode
    Boolean suspendOnOverride = getSettingBool("suspendScheduler", false)

    if (suspendOnOverride) {
        if (newReason == "Override" && previousReason != "Override") {
            logInfo "Entering Override state with suspendScheduler enabled. Pausing CRON schedules."
            stopPeriodSchedules()
        } else if (previousReason == "Override" && newReason != "Override") {
            logInfo "Exiting Override state. Resuming CRON schedules."
            restartPeriodSchedules()
        }
    }

    Boolean modeChanged = (previousMode != newMode)
    Boolean reasonChanged = (previousReason != newReason)

    state.modeReason = newReason

    if (modeChanged && reasonChanged) {
        logInfo "Location Mode changed from '${previousMode}' to '${newMode}' AND Evaluation Reason changed from '${previousReason}' to '${newReason}' | (${decision.source})"
        setLocationMode(newMode)
    } else if (modeChanged) {
        logInfo "Changing Hubitat Location Mode from '${previousMode}' to '${newMode}' | Reason remains '${newReason}' (${decision.source})"
        setLocationMode(newMode)
    } else if (reasonChanged) {
        logInfo "Location Mode remains '${newMode}', but Evaluation Reason changed from '${previousReason}' to '${newReason}' | (${decision.source})"
    } else {
        logTrace "Mode recheck completed: Location Mode remains '${newMode}' and Reason remains '${newReason}' | (${decision.source})"
    }

    updatePresentation([targetMode: newMode, reason: newReason])
}

private void syncOutputs(Map decision) {
    String targetMode = decision.targetMode
    String periodKey = decision.periodKey
    String reason = decision.reason
    Boolean isSleeping = (decision.isSleeping == true)
    Boolean isStartupSync = (decision.isStartupSync == true)

    logTrace "=== syncOutputs Execution Sequence Initiated ==="
    logTrace "Parameters -> TargetMode: '${targetMode}' | PeriodKey: '${periodKey}' | Reason: '${reason}' | IsSleeping: ${isSleeping} | IsStartupSync: ${isStartupSync}"

    // STEP 1: PHYSICAL & VIRTUAL SWITCH SYNCHRONIZATION
    logTrace "syncOutputs [Step 1/3]: Synchronizing Presence, Sleep, and Alexa switches..."
    if (targetMode == "Away") {
        syncSwitch(homeSwitch, "off")
        syncSwitch(awaySwitch, "on")
        syncSwitch(alexaModeSwitch, "off")
    } else {
        syncSwitch(homeSwitch, "on")
        syncSwitch(awaySwitch, "off")
        syncSwitch(alexaModeSwitch, "on")
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

    // STEP 2: NOTIFICATIONS & DASHBOARD TILE OUTPUTS
    logTrace "syncOutputs [Step 2/3]: Processing status tile update and push notifications..."
    updateStatusTileDevice(targetMode, reason)

    Boolean modeChanged = (decision.previousModeAtStart != targetMode)
    Boolean reasonChanged = (decision.previousReasonAtStart != reason)

    if (isStartupSync) {
        dispatchTileNotification(targetMode, "Reboot", isSleeping, modeChanged, reasonChanged)
    } else if (modeChanged || reasonChanged) {
        dispatchTileNotification(targetMode, reason, isSleeping, modeChanged, reasonChanged)
    } else if (decision.isRecheck == true) {
        dispatchTileNotification(targetMode, "RecheckNoChange", isSleeping, false, false)
    } else {
        dispatchTileNotification(targetMode, reason, isSleeping, false, false)
    }

    // STEP 3: LOCATION HSM EVENT DISPATCH (DISPATCHED LAST TO PREVENT BUS FEEDBACK LOOPS)
    logTrace "syncOutputs [Step 3/3]: Evaluating HSM state synchronization..."
    if (targetMode == "Away") {
        syncHsmState("armedAway", "armAway", reason)
    } else {
        Boolean isSleepingOrNight = (isSleeping || periodKey == "weeHours")
        String expectedHsmStatus = isSleepingOrNight ? "armedNight" : "disarmed"
        String hsmCmd = isSleepingOrNight ? "armNight" : "disarm"

        logTrace "HSM Target Calculation -> Mode: '${targetMode}' | Period: '${periodKey}' | IsSleeping: ${isSleeping} | Calculated Target: '${expectedHsmStatus}' (${hsmCmd})"
        syncHsmState(expectedHsmStatus, hsmCmd, reason)
    }

    logTrace "=== syncOutputs Execution Sequence Completed ==="
}

private void updateStatusTileDevice(String modeVal, String reasonVal) {
    if (!statusTileDevice) {
        logTrace "updateStatusTileDevice: No status tile device configured. Skipping tile command."
        return
    }

    String timeStr = new Date().format("hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())
    
    // Format active HSM status
    Boolean isHsmManaged = getSettingBool("manageHSM", true)
    String rawHsmStatus = isHsmManaged ? (location.hsmStatus ?: "disarmed") : "N/A"
    String formattedHsm = rawHsmStatus
    switch (rawHsmStatus) {
        case "armedAway":   formattedHsm = "Armed Away"; break
        case "armedHome":   formattedHsm = "Armed Home"; break
        case "armedNight":  formattedHsm = "Armed Night"; break
        case "disarmed":    formattedHsm = "Disarmed"; break
        case "allDisarmed": formattedHsm = "All Disarmed"; break
    }

    logTrace "updateStatusTileDevice -> Updating '${statusTileDevice.displayName}' | Mode: '${modeVal}' | Reason: '${reasonVal}' | HSM: '${formattedHsm}' | Time: '${timeStr}'"

    try {
        if (statusTileDevice.hasCommand("setStatus")) {
            logTrace "Calling setStatus('${modeVal}', '${reasonVal}', '${timeStr}', '${formattedHsm}') on tile device..."
            try {
                statusTileDevice.setStatus(modeVal, reasonVal, timeStr, formattedHsm)
            } catch (MissingMethodException e) {
                String compositeTimeHsm = "${timeStr} | HSM: ${formattedHsm}"
                statusTileDevice.setStatus(modeVal, reasonVal, compositeTimeHsm)
            }
        } else {
            logTrace "Tile device missing setStatus(). Falling back to discrete attribute setters..."
            if (statusTileDevice.hasCommand("setActiveMode")) statusTileDevice.setActiveMode(modeVal)
            if (statusTileDevice.hasCommand("setActiveReason")) statusTileDevice.setActiveReason(reasonVal)
            if (statusTileDevice.hasCommand("setLastTransitionTime")) statusTileDevice.setLastTransitionTime("${timeStr} (HSM: ${formattedHsm})")
            if (statusTileDevice.hasCommand("setHsmStatus")) statusTileDevice.setHsmStatus(formattedHsm)
        }
        logDebug "Native Status Tile Device '${statusTileDevice.displayName}' update command completed successfully with HSM state '${formattedHsm}'."
    } catch (Exception e) {
        logWarn "Failed to update Status Tile Device '${statusTileDevice?.displayName}': ${e.message}"
    }
}

private void markInternalSync(String devId, String value) {
    if (!devId) return
    String key = "${devId}:${value}"
    internalEventSuppressionMap.put(key, System.currentTimeMillis() + 1500L)
}

private boolean isInternalTransaction(def evt = null) {
    if (evt == null) return false

    def rawDevId = evt.deviceId ?: evt.device?.id
    if (rawDevId != null) {
        String devId = rawDevId.toString()
        String evtVal = evt.value?.toString()
        String key = "${devId}:${evtVal}"
        
        Long expires = internalEventSuppressionMap.get(key)
        if (expires != null) {
            long currentMs = System.currentTimeMillis()
            if (currentMs <= expires) {
                internalEventSuppressionMap.remove(key)
                logTrace "ATOMIC SUPPRESS SUCCESS: Suppressed internal event from '${evt.device?.displayName}' [${key}]"
                return true
            } else {
                internalEventSuppressionMap.remove(key)
            }
        }
    }
    return false
}

private void syncSwitch(def device, String targetState) {
    if (device != null && device.hasCommand(targetState)) {
        if (device.currentValue("switch") != targetState) {
            String devId = device.id.toString()
            markInternalSync(devId, targetState)

            logTrace "Syncing Output Device '${device.displayName}' [ID: ${devId} | Target: ${targetState.toUpperCase()}]"
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

private String getModeColor(String modeVal) {
    String cfgSleepMode = sleepMode?.toString() ?: "Sleeping"
    if (modeVal == cfgSleepMode) {
        return "#2980B9"
    } else if (modeVal == "Away") {
        return "#C0392B"
    } else {
        return "#27AE60"
    }
}

private String getReasonColor(String reasonVal) {
    switch (reasonVal) {
        case "Scheduled":   return "#2C3E50"
        case "Voice":       return "#D35400"
        case "Presence":    return "#16A085"
        case "Override":    return "#C0392B"
        case "Reboot":      return "#8E44AD"
        default:            return "#2C3E50"
    }
}

private void dispatchTileNotification(String modeVal, String reasonVal, Boolean isSleepingState = false, Boolean modeChanged = true, Boolean reasonChanged = true) {
    Boolean isSaveEvent = (state.isSettingsSaveEvent == true)
    if (isSaveEvent) state.isSettingsSaveEvent = false

    if (!getSettingBool("enableNotifications", true)) return

    if (reasonVal != "RecheckNoChange" && reasonVal != "Reboot" && state.lastNotifiedMode == modeVal && !reasonChanged) {
        logTrace "Notification suppressed: Mode '${modeVal}' and Reason '${reasonVal}' unchanged."
        return
    }

    Boolean shouldNotify = false
    String template = null

    if (reasonVal != "Reboot" && !modeChanged && reasonChanged && settings.templateReasonOnly) {
        shouldNotify = true
        template = settings.templateReasonOnly
    } else if (reasonVal != "Reboot" && modeChanged && !reasonChanged && settings.templateModeOnly) {
        shouldNotify = true
        template = settings.templateModeOnly
    } else {
        switch (reasonVal) {
            case "Scheduled":   
                shouldNotify = getSettingBool("notifyOnScheduled", false)
                template = settings.templateScheduled ?: "Mode scheduled transition: %mode% (from %prevMode% at %time%)"
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

    if (!shouldNotify) {
        logTrace "Notification suppressed: Reason '${reasonVal}' is disabled in Notification settings."
        return
    }

    String timeStr = new Date().format("hh:mm a", location.timeZone ?: TimeZone.getDefault())
    String prevMode = state.previousMode ?: "Unknown"
    String prevReason = state.previousReason ?: "Unknown"
    String displayReason = (reasonVal == "RecheckNoChange") ? (state.modeReason ?: "Scheduled") : reasonVal

    String rawMode = modeVal ?: "Unknown"
    String rawPrevMode = prevMode
    String rawReason = displayReason
    String rawPrevReason = prevReason

    String modeColor = getModeColor(rawMode)
    String reasonColor = getReasonColor(rawReason)

    String pushFormat = settings.notificationFormat ?: "plain"
    Boolean useHtmlPush = (pushFormat == "html")

    String formattedMode = useHtmlPush ? "<span style='color:${modeColor}; font-weight:bold;'>${rawMode}</span>" : rawMode
    String formattedPrevMode = useHtmlPush ? "<span style='color:${getModeColor(rawPrevMode)}; font-weight:bold;'>${rawPrevMode}</span>" : rawPrevMode
    String formattedReason = useHtmlPush ? "<span style='color:${reasonColor}; font-weight:bold;'>${rawReason}</span>" : rawReason
    String formattedPrevReason = useHtmlPush ? "<span style='color:${getReasonColor(rawPrevReason)}; font-weight:bold;'>${rawPrevReason}</span>" : rawPrevReason

    String pushMsg = template
        .replace("%mode%", formattedMode)
        .replace("%reason%", formattedReason)
        .replace("%prevMode%", formattedPrevMode)
        .replace("%prevReason%", formattedPrevReason)
        .replace("%time%", timeStr)

    if (useHtmlPush) {
        pushMsg = "<div style='white-space: nowrap; overflow-x: auto; font-size:1.0em;'>${pushMsg}</div>"
    }

    state.previousMode = rawMode
    state.previousReason = displayReason
    if (reasonVal != "RecheckNoChange") state.lastNotifiedMode = rawMode

    if (notificationDevice) {
        notificationDevice.each { dev -> 
            logTrace "Sending push notification to '${dev.displayName}'"
            dev.deviceNotification(pushMsg) 
        }
    }

    if (speechDevice) {
        Boolean isSleeping = (isSleepingState == true)
        if (isSleeping && getSettingBool("suppressAudioWhenSleeping", true)) {
            logTrace "Audio notification suppressed because system target state is Sleeping."
        } else {
            String plainSpeechMsg = isSaveEvent ? "Mode Manager settings changed and saved." : pushMsg.replaceAll("<[^>]*>", "")
            speechDevice.each { dev -> 
                logTrace "Sending audio speech notification to '${dev.displayName}'"
                dev.speak(plainSpeechMsg) 
            }
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
    String currentReason = reason ?: state.modeReason ?: "Scheduled"

    List<String> badgeParts = []
    if (showMode) badgeParts.add("<span style='color:green; font-weight:bold;'>${displayMode}</span>")
    if (showReason) badgeParts.add("(${currentReason})")

    String formattedLabel = baseLabel
    if (!badgeParts.isEmpty()) formattedLabel += " - [" + badgeParts.join(" ") + "]"
    
    if (app.label != formattedLabel) {
        app.updateLabel(formattedLabel)
    }
}

private boolean consumeVoiceMarker() {
    long expires = atomicState.alexaVoiceMarkerExpires ? (atomicState.alexaVoiceMarkerExpires as long) : 0L
    long currentMs = now()
    
    if (expires > 0L && currentMs <= expires) {
        atomicState.alexaVoiceMarkerExpires = 0L
        logInfo "Alexa Voice Source Marker verified and consumed successfully (${expires - currentMs}ms remaining)."
        return true
    } else if (expires > 0L) {
        atomicState.alexaVoiceMarkerExpires = 0L
    }
    return false
}

/* =========================================================================================
   EVENT HANDLERS & SIMULATION ENGINE
   ========================================================================================= */

def appButtonHandler(btn) {
    String btnName = "${btn}".toString()
    if (isTriggerDebounced("btn_${btnName}", 2000L)) return

    if (btnName == "btnTrigger") {
        recheckSchedule("Manual UI Button ('Resynchronize System & Reset Active Mode')")
    } else if (btnName == "btnForceTestEvaluation") {
        executeForcedTestEvaluation()
    }
}

def updateSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String devName = evt.device?.displayName ?: "Trigger Switch"
    String devId = evt.deviceId ? "${evt.deviceId}" : (evt.device ? "${evt.device.id}" : "unknown")
    if (isTriggerDebounced("switch_${devId}", 2000L)) return

    recheckSchedule("Trigger Switch '${devName}'")
}

def updateButtonHandler(evt) {
    if (isInternalTransaction(evt)) return
    String devName = evt.device?.displayName ?: "Trigger Button"
    String devId = evt.deviceId ? "${evt.deviceId}" : (evt.device ? "${evt.device.id}" : "unknown")
    if (isTriggerDebounced("button_${devId}_${evt.value}", 2000L)) return

    recheckSchedule("Trigger Button '${devName}' (Button #${evt.value})")
}

def vSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    if (evt.value != "on") return

    def rawDevId = evt.deviceId ?: evt.device?.id
    if (rawDevId == null) return
    String deviceId = "${rawDevId}".toString()

    if (isTriggerDebounced("vswitch_${deviceId}", 1000L)) return

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
        logInfo "Period virtual switch '${evt.device?.displayName}' toggled externally."
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
    logInfo "Master presence sensor '${evt.device?.displayName}' changed to '${evt.value}'"
    processStatePipeline([reason: "Presence", presenceValue: evt.value, source: "Master Presence Sensor"])
}

def homeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Home switch '${evt.device?.displayName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "on") ? "present" : "not present"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Home Switch"])
}

def awaySwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Away switch '${evt.device?.displayName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "off") ? "present" : "not present"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Away Switch"])
}

def alexaModeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    Boolean isVoiceSource = consumeVoiceMarker()
    String evalReason = "Voice"
    String srcLabel = isVoiceSource ? 
        "Alexa Mode Switch '${evt.device?.displayName}' (${evt.value} via Alexa Voice Routine)" : 
        "Alexa Mode Switch '${evt.device?.displayName}' (${evt.value} via Alexa Dashboard)"

    logInfo "Alexa Mode Switch changed to '${evt.value}' (Voice Marker Active: ${isVoiceSource} | Reason: '${evalReason}')"
    
    String targetMode = (evt.value == "on") ? (getActiveTimePeriodInfo()?.mode ?: location.mode) : "Away"
    String targetKey = (evt.value == "on") ? getActiveTimePeriodInfo()?.key : null
    
    processStatePipeline([
        reason: evalReason,
        targetMode: targetMode,
        periodKey: targetKey,
        source: srcLabel
    ])
}

def alexaVoiceMarkerSwitchHandler(evt) {
    if (evt.value == "on") {
        long currentMs = now()
        long expiresMs = currentMs + 5000
        atomicState.alexaVoiceMarkerExpires = expiresMs
        
        if (alexaVoiceMarkerSwitch && alexaVoiceMarkerSwitch.hasCommand("off")) {
            syncSwitch(alexaVoiceMarkerSwitch, "off")
        }
    }
}

def awakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    Boolean isVoiceSource = consumeVoiceMarker()
    String evalReason = isVoiceSource ? "Voice" : "Override"
    String srcLabel = isVoiceSource ? 
        "Awake Switch '${evt.device?.displayName}' (${evt.value} via Alexa Voice Routine)" : 
        "Awake Switch '${evt.device?.displayName}' (${evt.value} Manual/External)"

    logInfo "Awake switch '${evt.device?.displayName}' changed to '${evt.value}' (Voice Marker Active: ${isVoiceSource} | Attributed Reason: '${evalReason}')"
    
    Map activePeriod = getActiveTimePeriodInfo()
    String targetMode = (evt.value == "off") ? (sleepMode?.toString() ?: "Sleeping") : (activePeriod?.mode ?: location.mode)
    String targetKey = (evt.value == "off") ? "sleeping" : activePeriod?.key
    
    processStatePipeline([
        reason: evalReason,
        targetMode: targetMode,
        periodKey: targetKey,
        source: srcLabel
    ])
}

def sleepSwitchHandler(evt) {
    logTrace "sleepSwitchHandler: Event ignored. Passive output indicator."
}

def alexaAwakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    Boolean isVoiceSource = consumeVoiceMarker()
    String evalReason = "Voice"
    String srcLabel = isVoiceSource ? 
        "Alexa Awake Switch '${evt.device?.displayName}' (${evt.value} via Alexa Voice Routine)" : 
        "Alexa Awake Switch '${evt.device?.displayName}' (${evt.value} via Alexa Dashboard)"

    logInfo "Alexa Awake Switch changed to '${evt.value}' (Voice Marker Active: ${isVoiceSource} | Reason: '${evalReason}')"
    
    if (evt.value == "off") {
        processStatePipeline([
            reason: evalReason,
            targetMode: sleepMode?.toString() ?: "Sleeping",
            periodKey: "sleeping",
            source: srcLabel
        ])
    } else {
        Map activePeriod = getActiveTimePeriodInfo()
        processStatePipeline([
            reason: evalReason,
            targetMode: activePeriod?.mode ?: location.mode,
            periodKey: activePeriod?.key,
            source: srcLabel
        ])
    }
}

def executeForcedTestEvaluation() {
    Boolean isHomeTarget = getSettingBool("testHome", true)
    Boolean isAwakeTarget = getSettingBool("testAwake", true)
    String forcedPeriodKey = settings.testPeriodKey ?: "day"
    String targetReason = settings.testReason ?: "Override"

    logInfo "Executing Forced Simulation -> Home: ${isHomeTarget} | Awake: ${isAwakeTarget} | Period: ${forcedPeriodKey} | Reason: ${targetReason}"
    
    processStatePipeline([
        reason: targetReason,
        simulatedHome: isHomeTarget,
        simulatedAwake: isAwakeTarget,
        simulatedPeriodKey: forcedPeriodKey,
        source: "Section 7 Simulation Control Panel",
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