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
 * v1.0.42 (2026-08-15) - Comprehensive Bug Sweep (NPE, GUI State & Override/Sleep Preemption):
 *                        - Fixed dynamic page collapse logic so state.sectionsExpanded cleanly resets on page exit/Done.
 *                        - Added safe string coercion for appButtonHandler parameters and NPE defenses for null settings.
 *                        - Refined calculateDecision() hierarchy so Sleep/Awake state transitions cleanly pre-empt non-locked Overrides.
 *                        - Enforced submitOnChange across all conditional GUI toggles in Section 3 and Section 5.
 * v1.0.41 (2026-08-15) - Recheck Notification Engine Addition:
 *                        - Added SECTION 5 notification trigger and custom message template for rechecks where mode remains unchanged.
 *                        - Updated dispatchTileNotification() to allow "RecheckNoChange" events to bypass lastNotifiedMode suppression.
 * v1.0.40 (2026-08-15) - Dynamic State-Driven Section Collapse:
 *                        - Implemented state.sectionsExpanded flag to manage section visibility.
 *                        - Defaults to collapsed (hidden: true) upon opening the app.
 *                        - Preserves expanded section state during submitOnChange page refreshes while editing preferences.
 *                        - Automatically resets to collapsed state when clicking Done (updated/installed).
 * v1.0.39 (2026-08-15) - GUI Terminology Refactor:
 *                        - Renamed 'STEP 1 ... STEP 5' section headers and descriptions to 'SECTION 1 ... SECTION 5' for clearer UI area organization.
 * v1.0.38 (2026-08-15) - UI Section Auto-Collapse Fix:
 *                        - Removed 'hidden: true' parameter from SECTION 4 section definition to prevent the Simulation Control Panel from recollapsing on submitOnChange dropdown selections.
 * v1.0.37 (2026-08-15) - Hub Reboot Self-Healing & Startup Notifications:
 *                        - Added subscription to location "systemStart" and implemented hubStartupHandler().
 *                        - System automatically re-arms schedules, re-checks presence/awake state against time periods, and syncs output switches after a hub reboot.
 *                        - Dispatches startup notification to Notification Tile / Push / Speech devices.
 * v1.0.36 (2026-08-15) - Architectural Clean-Up & Sleep State Isolation:
 *                        - Removed vSwitchSleeping from SECTION 3 and periodSwitchList mapping table.
 *                        - Added self-defense guard in vSwitchHandler to ignore sleep/awake indicator events.
 *                        - Clarified separation between Behavioral Sleep State (Section 2) and Chronological Schedules (Section 3).
 * v1.0.35 (2026-08-14) - Mode-Change Notification Watch Guard.
 * v1.0.34 (2026-08-14) - Dedicated Message Templates & UI Refactor.
 * v1.0.33 (2026-08-14) - Granular Notification Engine & Speech Device Integration.
 * v1.0.32 (2026-08-14) - Expanded Call Path Tracing & Header History Restoration.
 * v1.0.31 (2026-08-14) - Complete Event Isolation, Scheduler Architecture Overhaul & Expanded Tracing.
 * v1.0.30 (2026-08-13) - Override Release Priority Preemption Fix.
 * v1.0.29 (2026-08-13) - Manual UI Button Override Release.
 * v1.0.28 (2026-08-13) - Simulation Engine Pipeline Injection.
 * v1.0.27 (2026-08-13) - Decoupled Override Persistence & Scheduler Controls.
 * v1.0.26 (2026-08-13) - Formal Authority Hierarchy Engine (Override > Presence > Voice > Normal).
 * v1.0.25 (2026-08-13) - Voice/Override Reason Retention Guard.
 * v1.0.24 (2026-08-13) - Asynchronous Output Event Suppression Buffer.
 * v1.0.1  (2026-08-13) - Full Architectural Refactor: Linear decision pipeline, transaction output isolation, and infrastructure scheduler.
 *
 */

static String version() { return '1.0.42' }

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
        
        /* GUI Section Collapse Behavior Fix:
           Check state.sectionsExpanded. If true, keep uncollapsed for current submitOnChange cycle.
           Immediately reset to false so exiting/saving closes all sections for next open. */
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

        /* Section 1: Presence */
        section("<b>SECTION 1: Presence Architecture & State Switches</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Presence Architecture:</b> Master Presence Sensor updates Home Switch (Primary Authoritative Input). Away Switch is maintained as its inverse mirror.</div>"
            
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Presence Modes:</b> When presence is <b>Away</b>, Location Mode automatically switches to <b>Away</b>. When presence returns to <b>Home</b>, the app automatically evaluates your active Section 3 Time Period schedule (or Section 2 Sleep state). No manual mode assignment is needed for presence.</div>"

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

            input name: "manageHSM", type: "bool", title: "Control Hubitat Safety Monitor (HSM) based on selected Master Presence Sensor?", defaultValue: true, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"
            
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Alexa Mode Integration:</b> Automatically follows the primary Presence state (<b>ON</b> = Home, <b>OFF</b> = Away).<br/>" +
                      "<i><b>External Trigger Behavior:</b> If toggled externally via Alexa voice command or routine, turning it <b>OFF</b> forces the system into <b>Away</b> mode (Reason: Voice). Turning it <b>ON</b> restores <b>Home</b> state and evaluates your active Section 3 Time Period schedule.</i></div>"

            input name: "alexaModeSwitch", type: "capability.switch", title: "Alexa Mode Virtual Switch <i>(ON = Home, OFF = Away)</i>", required: false, submitOnChange: true
        }
        
        /* Section 2: Sleep Architecture (Behavioral State) */
        section("<b>SECTION 2: Sleep Architecture & State Switches</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Sleep Architecture:</b> <b>Awake Switch</b> is the single authoritative input driving sleep evaluations. The <b>Sleeping Switch</b> is maintained automatically as a passive inverse mirror.</div>"
            
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Sleep Modes:</b> Sleeping is a behavioral state, not a chronological period schedule. When <b>Sleeping</b> (Awake Switch = OFF), the app overrides the time schedule and enforces the target mode selected below. When <b>Awake</b> (Awake Switch = ON), the sleep overlay releases and your system automatically resumes the Section 3 Time Period schedule.</div>"

            input name: "awakeSwitch", type: "capability.switch", title: "<b>Awake Switch</b> <i>(Primary Authoritative Input: ON = Awake, OFF = Sleeping)</i>", required: true, submitOnChange: true
            input name: "sleepSwitch", type: "capability.switch", title: "<b>Sleeping Switch</b> <i>(Passive Inverse Mirror Output — Dashboard Indicator)</i>", required: false, submitOnChange: true
            
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-6px; margin-bottom:10px;'>" +
                      "<i><b>Note:</b> You do not need to trigger rules with the Sleeping Switch. The app automatically maintains it as the inverse of your Awake Switch and uses the Awake Switch for all mode decisions.</i></div>"

            input name: "sleepMode", type: "mode", title: "<b>Target Mode when Sleeping</b>", required: true, defaultValue: "Sleeping", submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"
            
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Alexa Awake Integration:</b> Automatically follows the primary Sleep state (<b>ON</b> = Awake, <b>OFF</b> = Sleeping).<br/>" +
                      "<i><b>External Trigger Behavior:</b> If toggled externally via Alexa voice command or routine, turning it <b>OFF</b> forces the system into <b>Sleeping</b> mode (Reason: Voice). Turning it <b>ON</b> restores <b>Awake</b> state and evaluates your active Section 3 Time Period schedule.</i></div>"

            input name: "alexaAwakeSwitch", type: "capability.switch", title: "Alexa Awake Virtual Switch <i>(Alexa Routines Sync)</i>", required: false, submitOnChange: true
        }

        /* Section 3: Chronological Schedule */
        section("<b>SECTION 3: Time Period & Target Mode Schedule</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Schedule Architecture:</b> Dynamic chronological time periods evaluated sequentially throughout the day when Home and Awake.</div>"
            
            Map activePeriod = getActiveTimePeriodInfo()
            String activePeriodKey = activePeriod?.key ?: "Unknown"
            
            paragraph "<div style='background-color:#EAEDED; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "⏱️ <b>Active Time Schedule Block:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodKey.toUpperCase()}</span> (Target Mode: <b>${activePeriod?.mode ?: 'None'}</b>)</div>"

            input name: "timeWeeHours", type: "time", title: "Wee Hours Start", required: true, defaultValue: "00:30", width: 6, submitOnChange: true
            input name: "weeHoursMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Night", width: 3, submitOnChange: true
            input name: "vSwitchWeeHours", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3, submitOnChange: true
            
            input name: "timeEarlyMorning", type: "time", title: "Early Morning Start", required: true, defaultValue: "04:45", width: 6, submitOnChange: true
            input name: "earlyMorningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Early Morning", width: 3, submitOnChange: true
            input name: "vSwitchEarlyMorning", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3, submitOnChange: true
            
            input name: "timeMorning", type: "time", title: "Morning Start", required: true, defaultValue: "07:30", width: 6, submitOnChange: true
            input name: "morningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Morning", width: 3, submitOnChange: true
            input name: "vSwitchMorning", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3, submitOnChange: true
            
            input name: "timeDay", type: "time", title: "Day Start", required: true, defaultValue: "10:00", width: 6, submitOnChange: true
            input name: "dayMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Day", width: 3, submitOnChange: true
            input name: "vSwitchDay", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3, submitOnChange: true
            
            input name: "timeEvening", type: "time", title: "Evening Start", required: true, defaultValue: "17:00", width: 6, submitOnChange: true
            input name: "eveningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Evening", width: 3, submitOnChange: true
            input name: "vSwitchEvening", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3, submitOnChange: true
            
            input name: "timeLateEvening", type: "time", title: "Late Evening Start", required: true, defaultValue: "21:30", width: 6, submitOnChange: true
            input name: "lateEveningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Late Evening", width: 3, submitOnChange: true
            input name: "vSwitchLateEvening", type: "capability.switch", title: "Virtual Indicator / Override", required: false, width: 3, submitOnChange: true
        }

        /* Section 4: Overrides and Diagnostics */
        section("<b>SECTION 4: Manual Overrides and Diagnostics</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "<b>Evaluation Logic Flow:</b> Manual Override &gt; Presence &gt; Voice &gt; Normal</div>"
            
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Override Persistence & Scheduler Controls</span>"
            
            input name: "holdOverride", type: "bool", title: "<b>Lock Override against lower-priority events?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to prevent incoming Voice or Normal event rechecks from unseating an active Manual Override.</span>", defaultValue: false, submitOnChange: true
            input name: "suspendScheduler", type: "bool", title: "<b>Suspend CRON time period schedule during Override?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to pause daily CRON schedule triggers while in Override, stopping automatic returns to Normal mode on schedule boundaries.</span>", defaultValue: false, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>External Trigger Devices</span>"

            input name: "updateTriggerSwitch", type: "capability.switch", title: "Switch(s) to Trigger Evaluation / Update", required: false, multiple: true, submitOnChange: true
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-8px; margin-bottom:10px;'>" +
                      "<i><b>Note:</b> Trigger switches must have an auto-off (momentary/auto-revert) setting enabled in their driver so they naturally return to OFF after firing.</i></div>"

            input name: "updateTriggerButton", type: "capability.pushableButton", title: "Button(s) to Trigger Evaluation / Update", required: false, multiple: true, submitOnChange: true
            
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

        /* Section 5: Notification(s) */
        section("<b>SECTION 5: Notification(s) & Alert Preferences</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Notification Engine:</b> Configure push notifications (phones/dashboards) or audio notifications (Echo/Sonos speech devices) when system mode changes occur.</div>"
            
            input name: "enableNotifications", type: "bool", title: "<b>Enable Notifications?</b>", defaultValue: true, submitOnChange: true

            if (getSettingBool("enableNotifications", true)) {
                
                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>1. Destination Devices</span>"

                input name: "notificationDevice", type: "capability.notification", title: "<b>Push Notification Device(s)</b> <i>(Mobile App, Pushover, Tiles)</i>", required: false, multiple: true, submitOnChange: true
                input name: "speechDevice", type: "capability.speechSynthesis", title: "<b>Audio / Speech Device(s)</b> <i>(Echo Speaks, Sonos, Google Home)</i>", required: false, multiple: true, submitOnChange: true

                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>2. Event Triggers & Custom Message Templates</span>"
                paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-6px; margin-bottom:10px;'>" +
                          "Enable event triggers and customize their message format using dynamic variables:<br/>" +
                          "• <code>%mode%</code> = New Mode &nbsp;|&nbsp; <code>%reason%</code> = Reason &nbsp;|&nbsp; <code>%prevMode%</code> = Previous Mode &nbsp;|&nbsp; <code>%time%</code> = Time</div>"

                /* Trigger 1: Normal */
                input name: "notifyOnNormal", type: "bool", title: "Notify on <b>Normal Schedule Transitions</b>? <i>(e.g., 07:30 Morning CRON)</i>", defaultValue: false, submitOnChange: true
                if (getSettingBool("notifyOnNormal", false)) {
                    input name: "templateNormal", type: "text", title: "<b>Normal Schedule Message Template</b>", 
                          defaultValue: "Mode scheduled transition: %mode% (from %prevMode% at %time%)", required: true, submitOnChange: true
                }

                /* Trigger 2: Voice */
                input name: "notifyOnVoice", type: "bool", title: "Notify on <b>Voice / Alexa Triggers</b>?", defaultValue: true, submitOnChange: true
                if (getSettingBool("notifyOnVoice", true)) {
                    input name: "templateVoice", type: "text", title: "<b>Voice Trigger Message Template</b>", 
                          defaultValue: "Voice command changed mode to %mode% (Reason: %reason% at %time%)", required: true, submitOnChange: true
                }

                /* Trigger 3: Presence */
                input name: "notifyOnPresence", type: "bool", title: "Notify on <b>Presence Changes</b>? <i>(e.g., Home / Away)</i>", defaultValue: true, submitOnChange: true
                if (getSettingBool("notifyOnPresence", true)) {
                    input name: "templatePresence", type: "text", title: "<b>Presence Change Message Template</b>", 
                          defaultValue: "Presence update changed mode to %mode% (was %prevMode% at %time%)", required: true, submitOnChange: true
                }

                /* Trigger 4: Override */
                input name: "notifyOnOverride", type: "bool", title: "Notify on Manual <b>Override Changes</b>? <i>(Virtual switches, UI buttons)</i>", defaultValue: true, submitOnChange: true
                if (getSettingBool("notifyOnOverride", true)) {
                    input name: "templateOverride", type: "text", title: "<b>Manual Override Message Template</b>", 
                          defaultValue: "Manual Override activated: Mode set to %mode% (Reason: %reason% at %time%)", required: true, submitOnChange: true
                }

                /* Trigger 5: Reboot / System Startup */
                input name: "notifyOnReboot", type: "bool", title: "Notify on <b>Hub Reboot / Startup Synchronization</b>?", defaultValue: true, submitOnChange: true
                if (getSettingBool("notifyOnReboot", true)) {
                    input name: "templateReboot", type: "text", title: "<b>Hub Reboot Message Template</b>", 
                          defaultValue: "System restarted following hub boot. Mode synchronized to %mode% (Reason: %reason% at %time%)", required: true, submitOnChange: true
                }

                /* Trigger 6: Recheck No Change */
                input name: "notifyOnRecheckNoChange", type: "bool", title: "Notify on <b>Mode Rechecks when No Change Occurs</b>?", defaultValue: false, submitOnChange: true
                if (getSettingBool("notifyOnRecheckNoChange", false)) {
                    input name: "templateRecheckNoChange", type: "text", title: "<b>Recheck (No Change) Message Template</b>", 
                          defaultValue: "Mode recheck completed: Still in %mode% (Reason: %reason% at %time%)", required: true, submitOnChange: true
                }

                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>3. Quiet Hours / Restrictions</span>"

                input name: "suppressAudioWhenSleeping", type: "bool", title: "<b>Mute Audio/Speech devices while Mode is 'Sleeping'?</b>", defaultValue: true, submitOnChange: true
            }
        }

        /* App Preferences Section */
        section("<b>App Preferences</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Customize application labeling, logging levels, and dashboard output options.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Apps List Label Customization</span>"
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label? <i>(e.g., Mode Manager Advanced v${currentVersion})</i>", defaultValue: true, submitOnChange: true
            input name: "showModeInLabel", type: "bool", title: "Show Active Mode in App Label? <i>(e.g., [Day])</i>", defaultValue: true, submitOnChange: true
            input name: "showReasonInLabel", type: "bool", title: "Show Evaluation Reason in App Label? <i>(e.g., (Normal))</i>", defaultValue: true, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Dashboard Tile Formatting</span>"
            input name: "tileFormat", type: "enum", title: "<b>For Dashboard Tiles - Text Format</b>", options: ["plain": "Plain Text", "html": "Formatted HTML"], defaultValue: "plain", required: true, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Logging Levels</span>"
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-6px; margin-bottom:6px;'><i>Note: System WARN and ERROR logs are critical and always enabled. Debug logging turns off automatically after 30 minutes.</i></div>"
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true, submitOnChange: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true, submitOnChange: true
            input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false, submitOnChange: true
        }
        
        /* Mark state so subsequent submitOnChange cycles keep sections uncollapsed */
        state.sectionsExpanded = true
    }
}

/* =========================================================================================
   APPLICATION LIFECYCLE & INFRASTRUCTURE SCHEDULER
   ========================================================================================= */

def installed() {
    logDebug "installed() executing v${version()}..."
    state.sectionsExpanded = false
    seedLoggingState()
    state.lastManageHSM = getSettingBool("manageHSM", true)
    initialize()
    
    logTrace "CALL PATH: installed() -> requesting initial recheckSchedule('App Installed')"
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
    state.sectionsExpanded = false
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
    
    updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Normal"])
    logTrace "CALL PATH: updated() -> requesting recheckSchedule('App Preferences Updated')"
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
    
    if (state.pendingOutputSyncs == null) {
        state.pendingOutputSyncs = [:]
    }
    
    if (!state.modeReason) {
        state.modeReason = "Normal"
        logTrace "Seeded initial state.modeReason = 'Normal'"
    }

    if (getSettingBool("manageHSM", true) && !state.lastHsmState) {
        state.lastHsmState = location.hsmStatus ?: "disarmed"
        logTrace "Seeded initial state.lastHsmState = '${state.lastHsmState}' from location.hsmStatus"
    }

    logTrace "Establishing device and system event subscriptions..."
    
    subscribe(location, "systemStart", hubStartupHandler)

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
        logTrace "Subscribing to Sleep Switch (Output Mirror Handler): ${sleepSwitch.displayName}"
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

    [vSwitchWeeHours, vSwitchEarlyMorning, vSwitchMorning, vSwitchDay, vSwitchEvening, vSwitchLateEvening].each { vSwitch ->
        if (vSwitch) {
            logTrace "Subscribing to Virtual Indicator Switch: ${vSwitch.displayName}"
            subscribe(vSwitch, "switch.on", vSwitchHandler)
        }
    }

    if (state.modeReason == "Override" && getSettingBool("suspendScheduler", false)) {
        logTrace "System is in Override with suspendScheduler=ON. CRON schedules kept disabled."
        stopPeriodSchedules()
    } else {
        logTrace "CALL PATH: initialize() -> triggering restartPeriodSchedules()"
        restartPeriodSchedules()
    }
}

def hubStartupHandler(evt = null) {
    logInfo "HUB STARTUP DETECTED: Re-building schedules and performing state synchronization..."
    
    state.lastNotifiedMode = null
    
    initialize()
    
    logTrace "CALL PATH: hubStartupHandler() -> dispatching processStatePipeline(Reason: Reboot)"
    processStatePipeline([reason: "Reboot", source: "Hub System Startup"])
}

def restartPeriodSchedules() {
    logTrace "CALL PATH: restartPeriodSchedules() requested. Re-building infrastructure CRON triggers."
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
    logTrace "CALL PATH: stopPeriodSchedules() -> Unscheduling all daily time period triggers..."
    unschedule()
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
    logTrace "CALL PATH: CRON Handler fired for period key: '${periodKey}'"
    if (state.modeReason == "Override" && getSettingBool("suspendScheduler", false)) {
        logInfo "Period boundary '${periodKey}' hit but suspendScheduler is enabled. Staying in Override."
        return
    }
    
    logInfo "Period boundary '${periodKey}' hit. Processing Normal schedule transition."
    logTrace "CALL PATH: CRON Boundary '${periodKey}' -> dispatching processStatePipeline(Reason: Normal, Boundary: true)"
    processStatePipeline([reason: "Normal", source: "Schedule CRON (${periodKey})", isBoundaryTrigger: true])
}

/* =========================================================================================
   CORE DECISION PIPELINE (Calculate -> Apply -> Sync -> Present)
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
    logInfo "Mode recheck requested by trigger source: '${triggerSource}'"
    logTrace "CALL PATH: recheckSchedule('${triggerSource}') -> dispatching processStatePipeline(Reason: Normal)"
    processStatePipeline([reason: "Normal", source: triggerSource, isRecheck: true])
}

private void processStatePipeline(Map request) {
    String reqReason = request.reason ?: "Normal"
    String reqSource = request.source ?: "Internal Pipeline"
    Boolean isRecheck = request.isRecheck ?: false
    
    String txId = "tx_${now()}"
    state.activeTransaction = txId
    
    logTrace "--------------------------------------------------------------------------------"
    logTrace "START TRANSACTION #${txId} [Reason: ${reqReason} | Source: ${reqSource}]"

    try {
        String previousMode = location.mode
        Map decision = calculateDecision(request)
        decision.txId = txId
        decision.isRecheck = isRecheck
        decision.previousModeAtStart = previousMode
        
        applyDecision(decision)
        syncOutputs(decision)
        updatePresentation(decision)
        
        logTrace "COMPLETE TRANSACTION #${txId} -> Mode: '${location.mode}' | Reason: '${state.modeReason}'"
        logTrace "--------------------------------------------------------------------------------"
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

    /* Sleep State Preemption Logic:
       If Home and transitioning into Sleeping state, pre-empt unlocked Overrides */
    if (isHome && isSleeping && currentActiveReason == "Override" && !isHoldOverrideEnabled && requestedReason != "Override") {
        logInfo "Behavioral Sleep state triggered while Home. Pre-empting active unlocked Override."
        currentActiveReason = "Normal"
        currentRank = getReasonRank("Normal")
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
        if (isBoundaryTrigger) {
            logInfo "Schedule period boundary hit. Releasing active '${currentActiveReason}' state to 'Normal'."
        } else {
            logInfo "Incoming evaluation request [Reason: ${requestedReason}] preempted by active higher-priority state [Reason: ${currentActiveReason}]."
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
        /* Normal / Reboot Evaluation */
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

    return [
        reason: requestedReason,
        source: source,
        targetMode: targetMode,
        periodKey: activePeriodKey
    ]
}

private void applyDecision(Map decision) {
    String newMode = decision.targetMode
    String newReason = decision.reason
    
    if (!newMode) {
        logWarn "Decision produced null target mode. Aborting mode apply."
        return
    }

    String previousReason = state.modeReason ?: "Normal"
    Boolean suspendOnOverride = getSettingBool("suspendScheduler", false)

    if (suspendOnOverride) {
        if (newReason == "Override" && previousReason != "Override") {
            logInfo "Entering Override state with suspendScheduler enabled. Pausing CRON schedules."
            logTrace "CALL PATH: applyDecision() -> State Transition (Non-Override -> Override) -> calling stopPeriodSchedules()"
            stopPeriodSchedules()
        } 
        else if (previousReason == "Override" && newReason != "Override") {
            logInfo "Exiting Override state. Resuming CRON schedules."
            logTrace "CALL PATH: applyDecision() -> State Transition (Override -> ${newReason}) -> calling restartPeriodSchedules()"
            restartPeriodSchedules()
        }
    }

    state.modeReason = newReason

    if (location.mode != newMode) {
        logInfo "Changing Hubitat Location Mode from '${location.mode}' to '${newMode}' | Reason: ${newReason} (${decision.source})"
        setLocationMode(newMode)
    } else {
        logInfo "Mode recheck completed: Location Mode remains '${location.mode}' | Reason: ${newReason} (${decision.source})"
    }
}

private void syncOutputs(Map decision) {
    String targetMode = decision.targetMode
    String periodKey = decision.periodKey
    String reason = decision.reason

    if (targetMode == "Away") {
        syncSwitch(homeSwitch, "off")
        syncSwitch(awaySwitch, "on")
        syncSwitch(alexaModeSwitch, "off")
        
        if (getSettingBool("manageHSM", true)) {
            String currentHsm = location.hsmStatus
            if (currentHsm != "armedAway" && state.lastHsmState != "armedAway") {
                logInfo "HSM changed: '${currentHsm ?: state.lastHsmState ?: 'unknown'}' -> 'armAway'"
                logDebug "Executing HSM arming command -> armAway (Reason: ${reason})"
                state.lastHsmState = "armedAway"
                sendLocationEvent(name: "hsmSetArm", value: "armAway")
            }
        }
    } else {
        syncSwitch(homeSwitch, "on")
        syncSwitch(awaySwitch, "off")
        syncSwitch(alexaModeSwitch, "on")
        
        if (getSettingBool("manageHSM", true)) {
            Boolean isSleepingOrNight = (periodKey == "sleeping" || periodKey == "weeHours" || targetMode == sleepMode?.toString() || targetMode == weeHoursMode?.toString())
            String targetHsmCommand = isSleepingOrNight ? "armNight" : "disarm"
            String expectedHsmStatus = isSleepingOrNight ? "armedNight" : "disarmed"
            String currentHsm = location.hsmStatus

            if (currentHsm != expectedHsmStatus && state.lastHsmState != targetHsmCommand) {
                logInfo "HSM changed: '${currentHsm ?: state.lastHsmState ?: 'unknown'}' -> '${targetHsmCommand}'"
                logDebug "Executing HSM state update command -> ${targetHsmCommand} (Reason: ${reason})"
                state.lastHsmState = targetHsmCommand
                sendLocationEvent(name: "hsmSetArm", value: targetHsmCommand)
            }
        }
    }

    if (periodKey == "sleeping" || targetMode == sleepMode?.toString()) {
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
    if (modeChanged || reason == "Reboot") {
        dispatchTileNotification(targetMode, reason)
    } else if (decision.isRecheck == true) {
        dispatchTileNotification(targetMode, "RecheckNoChange")
    } else {
        dispatchTileNotification(targetMode, reason)
    }
}

private void syncSwitch(def device, String targetState) {
    if (device != null && device.hasCommand(targetState)) {
        if (device.currentValue("switch") != targetState) {
            String devId = "${device.id}".toString()
            logTrace "Syncing Output Device '${device.displayName}' [ID: ${devId}] -> ${targetState.toUpperCase()}"
            
            Map pendingMap = (state.pendingOutputSyncs != null) ? new HashMap(state.pendingOutputSyncs) : [:]
            pendingMap[devId] = [value: targetState, expires: now() + 3000]
            state.pendingOutputSyncs = pendingMap

            logTrace "Registered internal sync marker for '${device.displayName}' [ID: ${devId} | Value: ${targetState}]"
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

private void dispatchTileNotification(String modeVal, String reasonVal) {
    if (!getSettingBool("enableNotifications", true)) return

    if (reasonVal != "RecheckNoChange" && state.lastNotifiedMode == modeVal && reasonVal != "Reboot") {
        logTrace "Notification suppressed: Mode '${modeVal}' has not changed since last notification dispatch."
        return
    }

    Boolean shouldNotify = false
    String template = null

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
            template = settings.templateRecheckNoChange ?: "Mode recheck completed: Still in %mode% (Reason: %reason% at %time%)"
            break
        default:         
            shouldNotify = true
            template = "Location Mode changed to %mode% (Reason: %reason% at %time%)"
            break
    }

    if (!shouldNotify) {
        logTrace "Notification suppressed: Reason '${reasonVal}' is disabled in Notification settings."
        return
    }

    String timeStr = new Date().format("hh:mm a", location.timeZone ?: TimeZone.getDefault())
    String prevMode = state.previousMode ?: "Unknown"
    String displayReason = (reasonVal == "RecheckNoChange") ? (state.modeReason ?: "Normal") : reasonVal

    String formattedMsg = template
        .replace("%mode%", modeVal ?: "Unknown")
        .replace("%reason%", displayReason)
        .replace("%prevMode%", prevMode)
        .replace("%time%", timeStr)

    state.previousMode = modeVal
    if (reasonVal != "RecheckNoChange") {
        state.lastNotifiedMode = modeVal
    }

    if (notificationDevice) {
        String pushMsg = (settings.tileFormat == "html") ?
            "<div style='font-size:13px; font-weight:bold; color:#27AE60;'>${formattedMsg}</div>" : formattedMsg
        
        notificationDevice.each { dev -> 
            logTrace "Sending push notification to '${dev.displayName}'"
            dev.deviceNotification(pushMsg) 
        }
    }

    if (speechDevice) {
        Boolean isSleeping = (modeVal == sleepMode?.toString() || location.mode == sleepMode?.toString())
        if (isSleeping && getSettingBool("suppressAudioWhenSleeping", true)) {
            logTrace "Audio notification suppressed because system is in Sleeping mode."
        } else {
            String plainSpeechMsg = formattedMsg.replaceAll("<[^>]*>", "")
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
    if (state.activeTransaction != null) {
        logTrace "Suppressing event generated during active pipeline transaction (${state.activeTransaction})"
        return true
    }

    if (evt != null) {
        def rawDevId = evt.deviceId ?: evt.device?.id
        if (rawDevId != null) {
            String devId = "${rawDevId}".toString()
            String evtVal = evt.value?.toString()
            
            Map pendingMap = (state.pendingOutputSyncs != null) ? new HashMap(state.pendingOutputSyncs) : [:]

            if (pendingMap.containsKey(devId)) {
                Map marker = pendingMap[devId] as Map
                long currentMs = now()

                if (marker && marker.value == evtVal && currentMs <= (marker.expires as long)) {
                    logInfo "SUCCESS: Suppressed app-generated internal output event from '${evt.device?.displayName}' [ID: ${devId} | Value: ${evtVal}]"
                    return true
                } else if (marker && currentMs > (marker.expires as long)) {
                    logTrace "Expired suppression marker removed for '${evt.device?.displayName}' [ID: ${devId}]"
                    pendingMap.remove(devId)
                    state.pendingOutputSyncs = pendingMap
                }
            } else {
                logTrace "Event from '${evt.device?.displayName}' [ID: ${devId} | Value: ${evtVal}] has no active suppression marker -> Treating as External Input."
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

        logTrace "CALL PATH: UI Button ('Evaluate & Set Active Mode Now') clicked -> calling recheckSchedule()"
        recheckSchedule("Manual UI Button ('Evaluate & Set Active Mode Now')")
    } else if (btnName == "btnForceTestEvaluation") {
        logTrace "CALL PATH: UI Button ('btnForceTestEvaluation') clicked -> calling executeForcedTestEvaluation()"
        executeForcedTestEvaluation()
    }
}

def updateSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
    state.lastTriggerTime = currentMs

    String devName = evt.device?.displayName ?: "Trigger Switch"
    logTrace "CALL PATH: updateSwitchHandler('${devName}') -> calling recheckSchedule()"
    recheckSchedule("Trigger Switch '${devName}'")
}

def updateButtonHandler(evt) {
    if (isInternalTransaction(evt)) return
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) return
    state.lastTriggerTime = currentMs

    String devName = evt.device?.displayName ?: "Trigger Button"
    logTrace "CALL PATH: updateButtonHandler('${devName}') -> calling recheckSchedule()"
    recheckSchedule("Trigger Button '${devName}' (Button #${evt.value})")
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

    if ((sleepSwitch && "${sleepSwitch.id}".toString() == deviceId) || (awakeSwitch && "${awakeSwitch.id}".toString() == deviceId)) {
        logTrace "vSwitchHandler: Ignored event from '${evt.device?.displayName}' because it is assigned to Section 2 Sleep Architecture."
        return
    }

    Map<String, Map> switchIdToPeriodMap = [:]
    if (vSwitchWeeHours)     switchIdToPeriodMap["${vSwitchWeeHours.id}".toString()]     = [mode: weeHoursMode?.toString(),     key: "weeHours"]
    if (vSwitchEarlyMorning) switchIdToPeriodMap["${vSwitchEarlyMorning.id}".toString()] = [mode: earlyMorningMode?.toString(), key: "earlyMorning"]
    if (vSwitchMorning)      switchIdToPeriodMap["${vSwitchMorning.id}".toString()]      = [mode: morningMode?.toString(),      key: "morning"]
    if (vSwitchDay)          switchIdToPeriodMap["${vSwitchDay.id}".toString()]          = [mode: dayMode?.toString(),          key: "day"]
    if (vSwitchEvening)      switchIdToPeriodMap["${vSwitchEvening.id}".toString()]      = [mode: eveningMode?.toString(),      key: "evening"]
    if (vSwitchLateEvening)  switchIdToPeriodMap["${vSwitchLateEvening.id}".toString()]  = [mode: lateEveningMode?.toString(),  key: "lateEvening"]

    Map targetPeriod = switchIdToPeriodMap[deviceId]
    
    if (targetPeriod?.mode) {
        String devName = evt.device?.displayName ?: "Virtual Switch"
        logInfo "Period virtual switch '${devName}' toggled externally."
        logTrace "CALL PATH: vSwitchHandler('${devName}') -> External Override -> dispatching processStatePipeline(Reason: Override)"
        processStatePipeline([
            reason: "Override",
            targetMode: targetPeriod.mode,
            periodKey: targetPeriod.key,
            source: "Virtual Switch (${devName})"
        ])
    }
}

def presenceHandler(evt) {
    if (isInternalTransaction(evt)) return
    String devName = evt.device?.displayName ?: "Master Presence Sensor"
    logInfo "Master presence sensor '${devName}' changed to '${evt.value}'"
    logTrace "CALL PATH: presenceHandler('${devName}') [${evt.value}] -> dispatching processStatePipeline(Reason: Presence)"
    processStatePipeline([reason: "Presence", presenceValue: evt.value, source: "Master Presence Sensor"])
}

def homeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String devName = evt.device?.displayName ?: "Home Switch"
    logInfo "Home switch '${devName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "on") ? "present" : "not present"
    logTrace "CALL PATH: homeSwitchHandler('${devName}') [${evt.value}] -> dispatching processStatePipeline(Reason: Presence)"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Home Switch"])
}

def awaySwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String devName = evt.device?.displayName ?: "Away Switch"
    logInfo "Away switch '${devName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "off") ? "present" : "not present"
    logTrace "CALL PATH: awaySwitchHandler('${devName}') [${evt.value}] -> dispatching processStatePipeline(Reason: Presence)"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Away Switch"])
}

def alexaModeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Alexa Mode Switch changed to '${evt.value}'"
    String targetMode = (evt.value == "on") ? (getActiveTimePeriodInfo()?.mode ?: location.mode) : "Away"
    String targetKey = (evt.value == "on") ? getActiveTimePeriodInfo()?.key : null
    logTrace "CALL PATH: alexaModeSwitchHandler [${evt.value}] -> dispatching processStatePipeline(Reason: Voice)"
    processStatePipeline([
        reason: "Voice",
        targetMode: targetMode,
        periodKey: targetKey,
        source: "Alexa Mode Switch"
    ])
}

def awakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String devName = evt.device?.displayName ?: "Awake Switch"
    logInfo "Awake switch '${devName}' changed to '${evt.value}'"
    logTrace "CALL PATH: awakeSwitchHandler('${devName}') [${evt.value}] -> calling recheckSchedule()"
    recheckSchedule("Awake Switch '${devName}' (${evt.value})")
}

def sleepSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String devName = evt.device?.displayName ?: "Sleep Switch"
    logInfo "Sleep switch '${devName}' toggled externally to '${evt.value}'."
    
    String targetAwakeState = (evt.value == "on") ? "off" : "on"
    if (awakeSwitch) {
        if (awakeSwitch.currentValue("switch") != targetAwakeState) {
            logTrace "CALL PATH: sleepSwitchHandler('${devName}') [${evt.value}] -> Mirroring directly to Awake Switch '${awakeSwitch.displayName}' (${targetAwakeState.toUpperCase()})"
            awakeSwitch."${targetAwakeState}"()
        }
    } else {
        logTrace "CALL PATH: sleepSwitchHandler('${devName}') [${evt.value}] -> awakeSwitch is null -> calling recheckSchedule() as fallback"
        recheckSchedule("Sleep Switch '${devName}' (${evt.value})")
    }
}

def alexaAwakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    logInfo "Alexa Awake Switch changed to '${evt.value}'"
    
    if (evt.value == "off") {
        logTrace "CALL PATH: alexaAwakeSwitchHandler [OFF] -> dispatching processStatePipeline(Reason: Voice, Mode: Sleeping)"
        processStatePipeline([
            reason: "Voice",
            targetMode: sleepMode?.toString(),
            periodKey: "sleeping",
            source: "Alexa Awake Switch (OFF)"
        ])
    } else {
        Map activePeriod = getActiveTimePeriodInfo()
        logTrace "CALL PATH: alexaAwakeSwitchHandler [ON] -> dispatching processStatePipeline(Reason: Voice, Mode: ${activePeriod?.mode})"
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

    logInfo "Executing Forced Simulation -> Home: ${isHomeTarget} | Awake: ${isAwakeTarget} | Period: ${forcedPeriodKey} | Reason: ${targetReason}"
    logTrace "CALL PATH: executeForcedTestEvaluation() -> dispatching processStatePipeline(Simulated: true, ForceRelease: true)"
    
    processStatePipeline([
        reason: targetReason,
        simulatedHome: isHomeTarget,
        simulatedAwake: isAwakeTarget,
        simulatedPeriodKey: forcedPeriodKey,
        source: "Section 4 Simulation Control Panel",
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