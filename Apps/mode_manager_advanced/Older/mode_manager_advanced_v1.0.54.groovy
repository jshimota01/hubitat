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
 * v1.0.54 (2026-08-16) - Native Status Tile Driver Integration Fix:
 *                        - Updated updateStatusTileDevice() to invoke driver setter commands (setActiveMode,
 *                          setActiveReason, setLastTransitionTime) directly on Virtual Mode Status & State Tracker Tile Driver.
 *                        - Prevents app string caching from desynchronizing tile layout and timestamp display.
 * v1.0.53 (2026-08-16) - Diagnostics Ground-Truth Status Tile Device:
 *                        - Added statusTileDevice selector in Section 4 Diagnostics.
 *                        - App directly pushes activeMode, activeReason, lastTransitionTime, and tile attributes
 *                          to the virtual status device on every pipeline execution to bypass UI cache latency.
 * v1.0.52 (2026-08-16) - GUI Presentation & Label Sync Fix:
 *                        - Fixed real-time display mismatch between App Title Label and Active Status Card.
 *                        - Moved updatePresentation() invocation directly inside applyDecision() to ensure state.modeReason
 *                          and app.label stay perfectly synchronized during pipeline transactions and UI redraws.
 * v1.0.51 (2026-08-16) - Notification GUI Preferences & Granular Message Templates:
 *                        - Updated Trigger 6 label in Section 5 to "Mode and Reason Unchanged".
 *                        - Added optional notification templates for Mode Changed Only, Reason Changed Only, and Both Changed.
 *                        - Refined dispatchTileNotification() to evaluate granular mode/reason flags during message formatting.
 * v1.0.50 (2026-08-16) - Reason Change & State Transition Detection Fix:
 *                        - Captured previousReasonAtStart alongside previousModeAtStart in processStatePipeline().
 *                        - Refined applyDecision() and syncOutputs() to evaluate both modeChanged and reasonChanged.
 *                        - System now explicitly logs and notifies when Evaluation Reason changes even if Location Mode remains unchanged.
 * v1.0.49 (2026-08-16) - Diagnostics & Data Type Inspection Logging (Hubitat Sandbox Compliant):
 *                        - Replaced .getClass() with Hubitat-allowed .class property inspection.
 *                        - Added explicit Class/Type logging for device IDs, event values, and pending markers inside
 *                          isInternalTransaction() and syncSwitch() to inspect object type matching across threads.
 * v1.0.48 (2026-08-15) - Settings Modification Watch & Notification Suppression.
 * v1.0.47 (2026-08-15) - Behavioral Audio Suppression & State Clean-Up Sweep.
 * v1.0.46 (2026-08-15) - GUI Architecture Description Update.
 * v1.0.45 (2026-08-15) - Behavioral Sleep & Mode Disambiguation Fix.
 * v1.0.44 (2026-08-15) - HSM Command/Status State Normalization Fix.
 * v1.0.43 (2026-08-15) - Targeted Scheduler Isolation Fix.
 * v1.0.42 (2026-08-15) - Comprehensive Bug Sweep (NPE, GUI State & Override/Sleep Preemption).
 * v1.0.41 (2026-08-15) - Recheck Notification Engine Addition.
 * v1.0.40 (2026-08-15) - Dynamic State-Driven Section Collapse.
 * v1.0.1  (2026-08-13) - Full Architectural Refactor: Linear decision pipeline, transaction output isolation, and infrastructure scheduler.
 *
 */

static String version() { return '1.0.54' }

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
                      "<b>Core Pipeline Architecture:</b><br/>" +
                      "• <b>Authority Triggers:</b> Override &gt; Presence &gt; Voice &gt; Normal Schedule<br/>" +
                      "• <b>Behavioral Overlays:</b> Away disables schedules; Sleeping preempts unlocked Overrides to enforce night/sleep modes.</div>"
            
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Override Persistence & Scheduler Controls</span>"
            
            input name: "holdOverride", type: "bool", title: "<b>Lock Override against lower-priority events?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to prevent incoming Voice or Normal event rechecks from unseating an active Manual Override.</span>", defaultValue: false, submitOnChange: true
            input name: "suspendScheduler", type: "bool", title: "<b>Suspend CRON time period schedule during Override?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to pause daily CRON schedule triggers while in Override, stopping automatic returns to Normal mode on schedule boundaries.</span>", defaultValue: false, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Ground-Truth Diagnostics Status Device</span>"
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Status Device Output:</b> Select a virtual device (Virtual Mode Status & State Tracker Tile Driver) to receive direct ground-truth updates on every evaluation.</div>"

            input name: "statusTileDevice", type: "capability.actuator", title: "<b>Virtual Ground-Truth Status Device</b> <i>(Dashboard Tile)</i>", required: false, multiple: false, submitOnChange: true

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

                /* Trigger 6: Recheck Unchanged */
                input name: "notifyOnRecheckNoChange", type: "bool", title: "Notify on <b>Mode Rechecks when Mode AND Reason are Unchanged</b>?", defaultValue: false, submitOnChange: true
                if (getSettingBool("notifyOnRecheckNoChange", false)) {
                    input name: "templateRecheckNoChange", type: "text", title: "<b>Recheck (Mode & Reason Unchanged) Message Template</b>", 
                          defaultValue: "Mode recheck completed: Still in %mode% (%reason%) at %time%", required: true, submitOnChange: true
                }

                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>3. State Transition Message Override Templates</span>"

                input name: "templateReasonOnly", type: "text", title: "<b>Reason Changed Only Message Template</b> <i>(Mode remains same)</i>", 
                      defaultValue: "Evaluation Reason updated to %reason% (Mode remains %mode% at %time%)", required: false, submitOnChange: true

                input name: "templateModeOnly", type: "text", title: "<b>Mode Changed Only Message Template</b> <i>(Reason remains same)</i>", 
                      defaultValue: "Mode changed to %mode% (Reason remains %reason% at %time%)", required: false, submitOnChange: true

                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>4. Quiet Hours / Restrictions</span>"

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
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true, submitOnChange: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true, submitOnChange: true
            input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false, submitOnChange: true
        }
        
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
    state.lastSettingsSnapshot = captureSettingsSnapshot()
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
        logInfo "Settings modification detected. Executing mode recheck..."
        updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Normal"])
        recheckSchedule("App Preferences Modified")
    } else {
        logInfo "App closed via Done without setting changes. Skipping mode recheck and notification."
        updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Normal"])
    }
}

private String captureSettingsSnapshot() {
    List<String> watchKeys = [
        "masterPresence", "homeSwitch", "awaySwitch", "awakeSwitch", "sleepSwitch",
        "sleepMode", "timeWeeHours", "weeHoursMode", "timeEarlyMorning", "earlyMorningMode",
        "timeMorning", "morningMode", "timeDay", "dayMode", "timeEvening", "eveningMode",
        "timeLateEvening", "lateEveningMode", "holdOverride", "suspendScheduler", "manageHSM",
        "alexaModeSwitch", "alexaAwakeSwitch", "statusTileDevice"
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

    if (currentInfo != state.lastLogInfoEnable)  logWarn "Info Logging changed to: ${currentInfo}"
    if (currentDebug != state.lastLogDebugEnable) logWarn "Debug Logging changed to: ${currentDebug}"
    if (currentTrace != state.lastLogTraceEnable) logWarn "Trace Logging changed to: ${currentTrace}"

    state.lastLogInfoEnable  = currentInfo
    state.lastLogDebugEnable = currentDebug
    state.lastLogTraceEnable = currentTrace
}

def initialize() {
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    state.activeTransaction = null
    state.lastTriggerTime = 0
    
    if (state.pendingOutputSyncs == null) state.pendingOutputSyncs = [:]
    if (!state.modeReason) state.modeReason = "Normal"

    if (getSettingBool("manageHSM", true) && !state.lastHsmState) {
        state.lastHsmState = location.hsmStatus ?: "disarmed"
    }

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
    logInfo "HUB STARTUP DETECTED: Performing state synchronization..."
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
        return "0 ${cal.get(Calendar.MINUTE)} ${cal.get(Calendar.HOUR_OF_DAY)} * * ? *"
    } catch (Exception e) {
        logWarn "Could not parse time string '${timeIso}' to CRON: ${e.message}"
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
        // Force release
    } else if (currentActiveReason == "Override") {
        if (isHoldOverrideEnabled) {
            if (incomingRank < currentRank && !isBoundaryTrigger) requestedReason = "Override"
        }
    } else if (incomingRank < currentRank) {
        if (isBoundaryTrigger) requestedReason = "Normal"
        else requestedReason = currentActiveReason
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

    return [
        reason: requestedReason,
        source: source,
        targetMode: targetMode,
        periodKey: activePeriodKey,
        isSleeping: (isHome && isSleeping)
    ]
}

private void applyDecision(Map decision) {
    String newMode = decision.targetMode
    String newReason = decision.reason
    if (!newMode) return

    String previousReason = decision.previousReasonAtStart ?: state.modeReason ?: "Normal"
    String previousMode = decision.previousModeAtStart ?: location.mode

    if (getSettingBool("suspendScheduler", false)) {
        if (newReason == "Override" && previousReason != "Override") stopPeriodSchedules()
        else if (previousReason == "Override" && newReason != "Override") restartPeriodSchedules()
    }

    state.modeReason = newReason

    if (previousMode != newMode) {
        setLocationMode(newMode)
    }

    updatePresentation([targetMode: newMode, reason: newReason])
    updateStatusTileDevice(newMode, newReason)
}

private void updateStatusTileDevice(String modeVal, String reasonVal) {
    if (!statusTileDevice) return

    String timeStr = new Date().format("hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())

    try {
        // Native Driver Command Invocation
        if (statusTileDevice.hasCommand("setStatus")) {
            statusTileDevice.setStatus(modeVal, reasonVal, timeStr)
        } else {
            if (statusTileDevice.hasCommand("setActiveMode")) statusTileDevice.setActiveMode(modeVal)
            if (statusTileDevice.hasCommand("setActiveReason")) statusTileDevice.setActiveReason(reasonVal)
            if (statusTileDevice.hasCommand("setLastTransitionTime")) statusTileDevice.setLastTransitionTime(timeStr)
        }
        logTrace "Native Status Tile Device '${statusTileDevice.displayName}' updated -> Mode: '${modeVal}' | Reason: '${reasonVal}'"
    } catch (Exception e) {
        logWarn "Failed to update Status Tile Device '${statusTileDevice?.displayName}': ${e.message}"
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
            sendLocationEvent(name: "hsmSetArm", value: "armAway")
        }
    } else {
        syncSwitch(homeSwitch, "on")
        syncSwitch(awaySwitch, "off")
        syncSwitch(alexaModeSwitch, "on")
        if (getSettingBool("manageHSM", true)) {
            String hsmCmd = (isSleeping || periodKey == "weeHours") ? "armNight" : "disarm"
            sendLocationEvent(name: "hsmSetArm", value: hsmCmd)
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
            long currentMs = now()
            Map pendingMap = (state.pendingOutputSyncs != null) ? new HashMap(state.pendingOutputSyncs) : [:]

            pendingMap.entrySet().removeIf { entry ->
                Map marker = entry.value as Map
                return (marker == null || currentMs > (marker.expires as long))
            }

            pendingMap[devId] = [value: targetState, expires: currentMs + 3000]
            state.pendingOutputSyncs = pendingMap
            device."${targetState}"()
        }
    }
}

private void updateVirtualModeSwitches(String activePeriodKey) {
    [
        [key: "weeHours",     vSwitch: vSwitchWeeHours],
        [key: "earlyMorning", vSwitch: vSwitchEarlyMorning],
        [key: "morning",      vSwitch: vSwitchMorning],
        [key: "day",          vSwitch: vSwitchDay],
        [key: "evening",      vSwitch: vSwitchEvening],
        [key: "lateEvening",  vSwitch: vSwitchLateEvening]
    ].each { entry ->
        if (entry.vSwitch) {
            String targetState = (activePeriodKey != null && entry.key == activePeriodKey) ? "on" : "off"
            syncSwitch(entry.vSwitch, targetState)
        }
    }
}

private void dispatchTileNotification(String modeVal, String reasonVal, Boolean isSleepingState = false, Boolean modeChanged = true, Boolean reasonChanged = true) {
    if (!getSettingBool("enableNotifications", true)) return
    
    String timeStr = new Date().format("hh:mm a", location.timeZone ?: TimeZone.getDefault())
    String formattedMsg = "Mode: ${modeVal} (Reason: ${reasonVal} at ${timeStr})"

    if (notificationDevice) {
        notificationDevice.each { dev -> dev.deviceNotification(formattedMsg) }
    }
}

private void updatePresentation(Map decision) {
    updateAppLabel(decision.targetMode ?: location.mode, decision.reason ?: state.modeReason)
}

private void updateAppLabel(String currentMode = null, String reason = null) {
    String baseLabel = "Mode Manager Advanced v${version()}"
    String displayMode = currentMode ?: location.mode ?: "Unknown"
    String currentReason = reason ?: state.modeReason ?: "Normal"

    String formattedLabel = "${baseLabel} - [<span style='color:green; font-weight:bold;'>${displayMode}</span> (${currentReason})]"
    if (app.label != formattedLabel) {
        app.updateLabel(formattedLabel)
    }
}

private boolean isInternalTransaction(def evt = null) {
    if (state.activeTransaction != null) return true
    if (evt != null) {
        def rawDevId = evt.deviceId ?: evt.device?.id
        if (rawDevId != null) {
            String devId = rawDevId.toString()
            String evtVal = evt.value?.toString()
            Map pendingMap = (state.pendingOutputSyncs != null) ? new HashMap(state.pendingOutputSyncs) : [:]

            if (pendingMap.containsKey(devId)) {
                Map marker = pendingMap[devId] as Map
                long currentMs = now()
                if (marker && marker.value == evtVal && currentMs <= (marker.expires as long)) return true
            }
        }
    }
    return false
}

def appButtonHandler(btn) {
    if ("${btn}" == "btnTrigger") recheckSchedule("Manual UI Button")
    else if ("${btn}" == "btnForceTestEvaluation") executeForcedTestEvaluation()
}

def updateSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    recheckSchedule("Trigger Switch '${evt.device?.displayName}'")
}

def updateButtonHandler(evt) {
    if (isInternalTransaction(evt)) return
    recheckSchedule("Trigger Button '${evt.device?.displayName}'")
}

def vSwitchHandler(evt) {
    if (isInternalTransaction(evt) || evt.value != "on") return
    
    Map<String, Map> switchIdToPeriodMap = [:]
    if (vSwitchWeeHours)     switchIdToPeriodMap["${vSwitchWeeHours.id}"]     = [mode: weeHoursMode?.toString(),     key: "weeHours"]
    if (vSwitchEarlyMorning) switchIdToPeriodMap["${vSwitchEarlyMorning.id}"] = [mode: earlyMorningMode?.toString(), key: "earlyMorning"]
    if (vSwitchMorning)      switchIdToPeriodMap["${vSwitchMorning.id}"]      = [mode: morningMode?.toString(),      key: "morning"]
    if (vSwitchDay)          switchIdToPeriodMap["${vSwitchDay.id}"]          = [mode: dayMode?.toString(),          key: "day"]
    if (vSwitchEvening)      switchIdToPeriodMap["${vSwitchEvening.id}"]      = [mode: eveningMode?.toString(),      key: "evening"]
    if (vSwitchLateEvening)  switchIdToPeriodMap["${vSwitchLateEvening.id}"]  = [mode: lateEveningMode?.toString(),  key: "lateEvening"]

    Map targetPeriod = switchIdToPeriodMap["${evt.deviceId ?: evt.device?.id}"]
    if (targetPeriod?.mode) {
        processStatePipeline([reason: "Override", targetMode: targetPeriod.mode, periodKey: targetPeriod.key, source: "Virtual Switch"])
    }
}

def presenceHandler(evt) {
    if (isInternalTransaction(evt)) return
    processStatePipeline([reason: "Presence", source: "Master Presence Sensor"])
}

def homeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    processStatePipeline([reason: "Presence", source: "Home Switch"])
}

def awaySwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    processStatePipeline([reason: "Presence", source: "Away Switch"])
}

def alexaModeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    String targetMode = (evt.value == "on") ? (getActiveTimePeriodInfo()?.mode ?: location.mode) : "Away"
    processStatePipeline([reason: "Voice", targetMode: targetMode, source: "Alexa Mode Switch"])
}

def awakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    recheckSchedule("Awake Switch")
}

def sleepSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    recheckSchedule("Sleep Switch")
}

def alexaAwakeSwitchHandler(evt) {
    if (isInternalTransaction(evt)) return
    processStatePipeline([reason: "Voice", source: "Alexa Awake Switch"])
}

def executeForcedTestEvaluation() {
    processStatePipeline([
        reason: settings.testReason ?: "Override",
        simulatedHome: getSettingBool("testHome", true),
        simulatedAwake: getSettingBool("testAwake", true),
        simulatedPeriodKey: settings.testPeriodKey ?: "day",
        source: "Simulation Panel",
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

    int currentMinutes = timeToMinutes(new Date())
    periods.sort { it.start }
    return periods.reverse().find { currentMinutes >= it.start } ?: periods.last()
}

private int getMinutesFromSetting(String timeIso, int defaultMinutes) {
    if (!timeIso) return defaultMinutes
    try {
        return timeToMinutes(toDateTime(timeIso))
    } catch (Exception e) {
        return defaultMinutes
    }
}

private int timeToMinutes(Date time) {
    Calendar cal = Calendar.getInstance(location.timeZone ?: TimeZone.getDefault())
    cal.setTime(time)
    return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", true)) {
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

private void logInfo(String msg)  { if (getSettingBool("logInfoEnable", true)) log.info "${app.label}: ${msg}" }
private void logDebug(String msg) { if (getSettingBool("logDebugEnable", true)) log.debug "${app.label}: ${msg}" }
private void logTrace(String msg) { if (getSettingBool("logTraceEnable", false)) log.trace "${app.label}: ${msg}" }
private void logWarn(String msg)  { log.warn "${app.label} WARNING: ${msg}" }