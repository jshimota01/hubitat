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
 * v1.0.72 (2026-08-17) - Section 5 UI Label & Example Text Update:
 *                        - Updated template variable reference table and UI example text to use 'Evening' and 'Late Evening' instead of 'Night'.
 * v1.0.71 (2026-08-17) - Voice Marker Logging & Diagnostics Expansion:
 *                        - Added comprehensive logInfo, logDebug, and logTrace events to alexaVoiceMarkerSwitchHandler and consumeVoiceMarker to 
 * 							track voice source attribution.
 * v1.0.70 (2026-08-17) - Alexa Voice Marker Integration:
 *                        - Added alexaVoiceMarkerSwitch input field to Section 2 (Sleep Architecture GUI).
 *                        - Added one-shot pulse handler (alexaVoiceMarkerSwitchHandler) that sets a 5-second expiration marker (atomicState.alexaVoiceMarkerExpires)
 * 							and auto-reverts the marker switch OFF.
 *                        - Updated alexaAwakeSwitchHandler and awakeSwitchHandler to check and consume the voice marker for explicit voice source attribution.
 * v1.0.69 (2026-08-17) - Alexa Event Inspection Diagnostics:
 *                        - Added log.warn diagnostic logging to alexaAwakeSwitchHandler to inspect evt properties (type, source, physical/digital flags, description).
 * v1.0.68 (2026-08-17) - Atomic Suppression Pre-Registration Fix:
 *                        - Fixed race condition where output switch execution occurred prior to atomic marker storage, preventing loopback triggers on Alexa Mode switch.
 * v1.0.67 (2026-08-17) - Section 5 GUI Layout Restructure:
 *                        - Separated Event Trigger switches from Custom Message text inputs into dedicated sub-sections.
 *                        - Added variable reference table displaying %mode%, %prevMode%, %reason%, %prevReason%, and %time%.
 * v1.0.66 (2026-08-17) - Semantic Reason Labeling Update:
 *                        - Renamed 'Normal' evaluation reason to 'Scheduled' across all pipeline decision logic, handlers, state initializations, UI labels, 
 * 							and notification templates.
 *                        - Improves log and tile readability by clearly attributing background time-table transitions as 'Scheduled'.
 * v1.0.65 (2026-08-17) - Entry Handler Trace Framing:
 *                        - Added context-aware trace log wrappers around external event handlers (updateButtonHandler, updateSwitchHandler, presenceHandler, etc.).
 * v1.0.64 (2026-08-17) - Context-Aware Execution Trace Framing:
 *                        - Updated '=== Mode Manager Adv Begins ===' and '=== Mode Manager Adv Ends ===' markers to include section, reason, and source context.
 * v1.0.63 (2026-08-17) - Execution Framing & Wake Output Sync Fix:
 *                        - Added execution framing log markers and removed trailing hyphen lines at end of transactions.
 *                        - Fixed finalIsSleeping evaluation in calculateDecision() to ensure output switches properly sync on Wake transitions.
 *                        - Optimized updated() lifecycle to prevent redundant re-initialization on save without settings changes.
 * v1.0.62 (2026-08-16) - Presence Authority Pipeline Defect Fix:
 *                        - Fixed calculateDecision() ignoring request.presenceValue from masterPresence events.
 *                        - Established strict presence evaluation precedence: presenceValue -> simulatedHome -> homeSwitch.
 * v1.0.61 (2026-08-16) - Notification Mute & Sleep Release Fix:
 *                        - Fixed dispatchTileNotification() to evaluate isSleeping strictly against incoming target decision rather than stale state.modeReason.
 *                        - Correctly allows audio notifications when waking up from Sleeping mode to Day/Morning.
 * see changelog.txt for 1.0.1 - 1.0.60
 *
 */

static String version() { return '1.0.72' }

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

            input name: "masterPresence", type: "capability.presenceSensor", title: "<b>Master Presence Sensor</b> <i>(OwnTracks - Jim)</i>", required: true
            input name: "homeSwitch", type: "capability.switch", title: "<b>Home Switch</b> <i>(Primary Input)</i>", required: true
            input name: "awaySwitch", type: "capability.switch", title: "<b>Away Switch</b> <i>(Inverse Mirror)</i>", required: true
            
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
                      "<i><b>External Trigger Behavior:</b> If toggled externally via Alexa voice command or routine, turning it <b>OFF</b> forces the system into <b>Away</b> mode (Reason: Voice). Turning it <b>ON</b> restores <b>Home</b> state and evaluates your active Section 3 Time Period schedule.</i></div>"

            input name: "alexaModeSwitch", type: "capability.switch", title: "Alexa Mode Virtual Switch <i>(ON = Home, OFF = Away)</i>", required: false
        }
        
        /* Section 2: Sleep Architecture (Behavioral State) */
        section("<b>SECTION 2: Sleep Architecture & State Switches</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Sleep Architecture:</b> <b>Awake Switch</b> is the single authoritative input driving sleep evaluations. The <b>Sleeping Switch</b> is maintained automatically as a passive inverse mirror.</div>"
            
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Sleep Modes:</b> Sleeping is a behavioral state, not a chronological period schedule. When <b>Sleeping</b> (Awake Switch = OFF), the app overrides the time schedule and enforces the target mode selected below. When <b>Awake</b> (Awake Switch = ON), the sleep overlay releases and your system automatically resumes the Section 3 Time Period schedule.</div>"

            input name: "awakeSwitch", type: "capability.switch", title: "<b>Awake Switch</b> <i>(Primary Authoritative Input: ON = Awake, OFF = Sleeping)</i>", required: true
            input name: "sleepSwitch", type: "capability.switch", title: "<b>Sleeping Switch</b> <i>(Passive Inverse Mirror Output — Dashboard Indicator)</i>", required: false
            
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-6px; margin-bottom:10px;'>" +
                      "<i><b>Note:</b> You do not need to trigger rules with the Sleeping Switch. The app automatically maintains it as the inverse of your Awake Switch and uses the Awake Switch for all mode decisions.</i></div>"

            input name: "sleepMode", type: "mode", title: "<b>Target Mode when Sleeping</b>", required: true, defaultValue: "Sleeping"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Alexa Ecosystem Extensions</span>"
            
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Alexa Awake & Voice Marker Integration:</b> Automatically follows the primary Sleep state.<br/>" +
                      "<i><b>Voice Marker Setup:</b> In your Alexa Routine, set the <b>Alexa Voice Marker Switch</b> to <b>ON</b> prior to or alongside toggling state switches. This provides deterministic voice source attribution and auto-resets in 5 seconds.</i></div>"

            input name: "alexaAwakeSwitch", type: "capability.switch", title: "Alexa Awake Virtual Switch <i>(Alexa Routines Sync)</i>", required: false
            input name: "alexaVoiceMarkerSwitch", type: "capability.switch", title: "<b>Alexa - Voice Marker Source</b> <i>(One-Shot Voice Signal Switch)</i>", required: false
        }

        /* Section 3: Chronological Schedule */
        section("<b>SECTION 3: Time Period & Target Mode Schedule</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Schedule Architecture:</b> Dynamic chronological time periods evaluated sequentially throughout the day when Home and Awake.</div>"
            
            Map activePeriod = getActiveTimePeriodInfo()
            String activePeriodKey = activePeriod?.key ?: "Unknown"
            
            paragraph "<div style='background-color:#EAEDED; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "⏱️ <b>Active Time Schedule Block:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodKey.toUpperCase()}</span> (Target Mode: <b>${activePeriod?.mode ?: 'None'}</b>)</div>"

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

        /* Section 4: Overrides and Diagnostics */
        section("<b>SECTION 4: Manual Overrides and Diagnostics</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "<b>Core Pipeline Architecture:</b><br/>" +
                      "• <b>Authority Triggers:</b> Override &gt; Presence &gt; Voice &gt; Scheduled Time Table<br/>" +
                      "• <b>Behavioral Overlays:</b> Away disables schedules; Sleeping preempts unlocked Overrides to enforce sleeping/evening modes.</div>"
            
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Override Persistence & Scheduler Controls</span>"
            
            input name: "holdOverride", type: "bool", title: "<b>Lock Override against lower-priority events?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to prevent incoming Voice or Scheduled event rechecks from unseating an active Manual Override.</span>", defaultValue: false
            input name: "suspendScheduler", type: "bool", title: "<b>Suspend CRON time period schedule during Override?</b><br/><span style='font-size:11px; color:#555;'>Turn ON to pause daily CRON schedule triggers while in Override, stopping automatic returns to Scheduled mode on period boundaries.</span>", defaultValue: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Ground-Truth Diagnostics Status Device</span>"
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Status Device Output:</b> Select a virtual device (Virtual Mode Status & State Tracker Tile Driver) to receive direct ground-truth updates on every evaluation.</div>"

            input name: "statusTileDevice", type: "capability.actuator", title: "<b>Virtual Ground-Truth Status Device</b> <i>(Dashboard Tile)</i>", required: false, multiple: false

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

            input name: "testHome", type: "bool", title: "<b>Force Home Status?</b> (ON = Home, OFF = Away)", defaultValue: true
            input name: "testAwake", type: "bool", title: "<b>Force Awake Status?</b> (ON = Awake, OFF = Sleeping)", defaultValue: true

            String trackerName = masterPresence ? masterPresence.displayName : "Master Presence Sensor"
            paragraph "<div style='background-color:#FCF3CF; border-left:4px solid #F1C40F; padding:8px; border-radius:4px; font-size:12px; color:#7D6608; margin-bottom:10px;'>" +
                      "⚠️ <b>Warning:</b> Please check that your Presence tracking device (<b>${trackerName}</b>) is reflecting this status.</div>"

            input name: "testPeriodKey", type: "enum", title: "<b>Select Period for Simulation</b>", required: false,
                  options: [
                      "weeHours": "Wee Hours Target Mode (${weeHoursMode ?: 'Late Evening'})",
                      "earlyMorning": "Early Morning Target Mode (${earlyMorningMode ?: 'Early Morning'})",
                      "morning": "Morning Target Mode (${morningMode ?: 'Morning'})",
                      "day": "Day Target Mode (${dayMode ?: 'Day'})",
                      "evening": "Evening Target Mode (${eveningMode ?: 'Evening'})",
                      "lateEvening": "Late Evening Target Mode (${lateEveningMode ?: 'Late Evening'})"
                  ]

            input name: "testReason", type: "enum", title: "<b>Evaluation Reason for Simulation</b>", defaultValue: "Override", required: false,
                  options: ["Override": "Override (Simulation)", "Scheduled": "Scheduled (Simulation)", "Presence": "Presence (Simulation)", "Voice": "Voice (Simulation)"]

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

                input name: "notificationDevice", type: "capability.notification", title: "<b>Push Notification Device(s)</b> <i>(Mobile App, Pushover, Tiles)</i>", required: false, multiple: true
                input name: "speechDevice", type: "capability.speechSynthesis", title: "<b>Audio / Speech Device(s)</b> <i>(Echo Speaks, Sonos, Google Home)</i>", required: false, multiple: true

                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>2. Event Triggers</span>"
                paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>Select which system events will trigger notification dispatches:</div>"

                input name: "notifyOnScheduled", type: "bool", title: "Notify on <b>Scheduled Transitions</b>? <i>(e.g., 07:30 Morning CRON)</i>", defaultValue: false
                input name: "notifyOnVoice", type: "bool", title: "Notify on <b>Voice / Alexa Triggers</b>?", defaultValue: true
                input name: "notifyOnPresence", type: "bool", title: "Notify on <b>Presence Changes</b>? <i>(e.g., Home / Away)</i>", defaultValue: true
                input name: "notifyOnOverride", type: "bool", title: "Notify on Manual <b>Override Changes</b>? <i>(Virtual switches, UI buttons)</i>", defaultValue: true
                input name: "notifyOnReboot", type: "bool", title: "Notify on <b>Hub Reboot / Startup Synchronization</b>?", defaultValue: true
                input name: "notifyOnRecheckNoChange", type: "bool", title: "Notify on <b>Mode Rechecks when Mode AND Reason are Unchanged</b>?", defaultValue: false

                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>3. Custom Message Templates & Variable Reference</span>"
                
                paragraph "<div style='background-color:#F4F6F7; border:1px solid #D5D8DC; border-radius:5px; padding:10px; font-size:12px; margin-bottom:12px;'>" +
                          "<b style='color:#2C3E50;'>Supported Message Template Variables:</b>" +
                          "<table style='width:100%; border-collapse:collapse; margin-top:6px; font-size:11px;'>" +
                          "  <tr style='background-color:#EAECEE; text-align:left; border-bottom:1px solid #D5D8DC;'>" +
                          "    <th style='padding:4px 6px;'>Variable</th>" +
                          "    <th style='padding:4px 6px;'>Description</th>" +
                          "    <th style='padding:4px 6px;'>Example Output Value</th>" +
                          "  </tr>" +
                          "  <tr style='border-bottom:1px solid #EAEDED;'>" +
                          "    <td style='padding:4px 6px;'><code>%mode%</code></td>" +
                          "    <td style='padding:4px 6px;'>Newly evaluated Location Mode</td>" +
                          "    <td style='padding:4px 6px;'><b>Day</b>, <b>Evening</b>, <b>Away</b></td>" +
                          "  </tr>" +
                          "  <tr style='border-bottom:1px solid #EAEDED;'>" +
                          "    <td style='padding:4px 6px;'><code>%prevMode%</code></td>" +
                          "    <td style='padding:4px 6px;'>Previous Location Mode prior to change</td>" +
                          "    <td style='padding:4px 6px;'><b>Morning</b>, <b>Late Evening</b></td>" +
                          "  </tr>" +
                          "  <tr style='border-bottom:1px solid #EAEDED;'>" +
                          "    <td style='padding:4px 6px;'><code>%reason%</code></td>" +
                          "    <td style='padding:4px 6px;'>Current evaluation trigger authority</td>" +
                          "    <td style='padding:4px 6px;'><b>Scheduled</b>, <b>Voice</b>, <b>Presence</b>, <b>Override</b></td>" +
                          "  </tr>" +
                          "  <tr style='border-bottom:1px solid #EAEDED;'>" +
                          "    <td style='padding:4px 6px;'><code>%prevReason%</code></td>" +
                          "    <td style='padding:4px 6px;'>Previous evaluation trigger authority</td>" +
                          "    <td style='padding:4px 6px;'><b>Voice</b>, <b>Scheduled</b></td>" +
                          "  </tr>" +
                          "  <tr>" +
                          "    <td style='padding:4px 6px;'><code>%time%</code></td>" +
                          "    <td style='padding:4px 6px;'>Formatted timestamp of event</td>" +
                          "    <td style='padding:4px 6px;'><b>09:30 AM</b></td>" +
                          "  </tr>" +
                          "</table></div>"

                input name: "templateScheduled", type: "text", title: "<b>Scheduled Message</b>", 
                      defaultValue: "Mode scheduled transition: %mode% (from %prevMode% at %time%)", required: true

                input name: "templateVoice", type: "text", title: "<b>Voice Trigger Message</b>", 
                      defaultValue: "Voice command changed mode to %mode% (Reason: %reason% at %time%)", required: true

                input name: "templatePresence", type: "text", title: "<b>Presence Change Message</b>", 
                      defaultValue: "Presence update changed mode to %mode% (was %prevMode% at %time%)", required: true

                input name: "templateOverride", type: "text", title: "<b>Manual Override Message</b>", 
                      defaultValue: "Manual Override activated: Mode set to %mode% (Reason: %reason% at %time%)", required: true

                input name: "templateReboot", type: "text", title: "<b>Hub Reboot Message</b>", 
                      defaultValue: "System restarted following hub boot. Mode synchronized to %mode% (Reason: %reason% at %time%)", required: true

                input name: "templateRecheckNoChange", type: "text", title: "<b>Recheck (Mode & Reason Unchanged) Message</b>", 
                      defaultValue: "Mode recheck completed: Still in %mode% (%reason%) at %time%", required: true

                input name: "templateReasonOnly", type: "text", title: "<b>Reason Changed Only Message</b> <i>(Mode remains same)</i>", 
                      defaultValue: "Evaluation Reason updated to %reason% (Mode remains %mode% at %time%)", required: false

                input name: "templateModeOnly", type: "text", title: "<b>Mode Changed Only Message</b> <i>(Reason remains same)</i>", 
                      defaultValue: "Mode changed to %mode% (Reason remains %reason% at %time%)", required: false

                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>4. Quiet Hours / Restrictions</span>"

                input name: "suppressAudioWhenSleeping", type: "bool", title: "<b>Mute Audio/Speech devices while Mode is 'Sleeping'?</b>", defaultValue: true
            }
        }

        /* App Preferences Section */
        section("<b>App Preferences</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Customize application labeling, logging levels, and dashboard output options.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Apps List Label Customization</span>"
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label? <i>(e.g., Mode Manager Advanced v${currentVersion})</i>", defaultValue: true
            input name: "showModeInLabel", type: "bool", title: "Show Active Mode in App Label? <i>(e.g., [Day])</i>", defaultValue: true
            input name: "showReasonInLabel", type: "bool", title: "Show Evaluation Reason in App Label? <i>(e.g., (Scheduled))</i>", defaultValue: true

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
    state.lastManageHSM = getSettingBool("manageHSM", true)
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize()
    
    logTrace "CALL PATH: installed() -> requesting initial recheckSchedule('App Installed')"
    recheckSchedule("App Installed")
    logTrace "=== Mode Manager Adv [installed] Ends ==="
}

def uninstalled() {
    logTrace "=== Mode Manager Adv [uninstalled] Begins ==="
    logDebug "Uninstalled v${version()}. Cleaning up subscriptions and schedules..."
    unsubscribe()
    stopPeriodSchedules()
    unschedule()
    logTrace "=== Mode Manager Adv [uninstalled] Ends ==="
}

def updated() {
    logTrace "=== Mode Manager Adv [updated] Begins ==="
    logDebug "updated() executing v${version()}..."
    state.sectionsExpanded = false
    checkLoggingChanges()
    checkHsmSettingChanges()
    
    String currentSnapshot = captureSettingsSnapshot()
    String previousSnapshot = state.lastSettingsSnapshot
    Boolean settingsChanged = (previousSnapshot != null && previousSnapshot != currentSnapshot)

    if (settingsChanged) {
        logInfo "Settings modification detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        
        unsubscribe()
        
        unschedule("disableDebugLogging")
        if (getSettingBool("logDebugEnable", true)) {
            logTrace "Scheduling automatic disable of debug logging in 30 minutes (1800s)."
            runIn(1800, disableDebugLogging)
        }
        
        initialize()
        
        updatePresentation([targetMode: location.mode, reason: state.modeReason ?: "Scheduled"])
        logTrace "CALL PATH: updated() -> requesting recheckSchedule('App Preferences Modified')"
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
        "timeLateEvening", "lateEveningMode", "holdOverride", "suspendScheduler", "manageHSM",
        "alexaModeSwitch", "alexaAwakeSwitch", "alexaVoiceMarkerSwitch", "statusTileDevice", "logInfoEnable", "logDebugEnable", "logTraceEnable"
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

    if (currentInfo != state.lastLogInfoEnable)   logWarn "Info Logging changed: ${state.lastLogInfoEnable ? 'ENABLED' : 'DISABLED'} -> ${currentInfo ? 'ENABLED' : 'DISABLED'}"
    if (currentDebug != state.lastLogDebugEnable) logWarn "Debug Logging changed: ${state.lastLogDebugEnable ? 'ENABLED' : 'DISABLED'} -> ${currentDebug ? 'ENABLED' : 'DISABLED'}"
    if (currentTrace != state.lastLogTraceEnable) logWarn "Trace Logging changed: ${state.lastLogTraceEnable ? 'ENABLED' : 'DISABLED'} -> ${currentTrace ? 'ENABLED' : 'DISABLED'}"

    state.lastLogInfoEnable  = currentInfo
    state.lastLogDebugEnable = currentDebug
    state.lastLogTraceEnable = currentTrace
}

def initialize() {
    logTrace "=== Mode Manager Adv [initialize] Begins ==="
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    state.activeTransaction = null
    state.lastTriggerTime = 0
    
    if (atomicState.pendingOutputSyncs == null) atomicState.pendingOutputSyncs = [:]
    if (!state.modeReason || state.modeReason == "Normal") {
        state.modeReason = "Scheduled"
        logTrace "Seeded initial state.modeReason = 'Scheduled'"
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
    if (alexaVoiceMarkerSwitch) {
        logTrace "Subscribing to Alexa Voice Marker Switch: ${alexaVoiceMarkerSwitch.displayName}"
        subscribe(alexaVoiceMarkerSwitch, "switch", alexaVoiceMarkerSwitchHandler)
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
    logTrace "=== Mode Manager Adv [initialize] Ends ==="
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
    logTrace "CALL PATH: stopPeriodSchedules() -> Unscheduling period CRON triggers..."
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
    logTrace "=== Mode Manager Adv [CRON Boundary: ${periodKey}] Begins ==="
    logTrace "CALL PATH: CRON Handler fired for period key: '${periodKey}'"
    if (state.modeReason == "Override" && getSettingBool("suspendScheduler", false)) {
        logInfo "Period boundary '${periodKey}' hit but suspendScheduler is enabled. Staying in Override."
        logTrace "=== Mode Manager Adv [CRON Boundary: ${periodKey}] Ends ==="
        return
    }
    logInfo "Period boundary '${periodKey}' hit. Processing Scheduled transition."
    logTrace "CALL PATH: CRON Boundary '${periodKey}' -> dispatching processStatePipeline(Reason: Scheduled, Boundary: true)"
    processStatePipeline([reason: "Scheduled", source: "Schedule CRON (${periodKey})", isBoundaryTrigger: true])
    logTrace "=== Mode Manager Adv [CRON Boundary: ${periodKey}] Ends ==="
}

/* =========================================================================================
   CORE DECISION PIPELINE
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
    logTrace "CALL PATH: recheckSchedule('${triggerSource}') -> dispatching processStatePipeline(Reason: Scheduled)"
    processStatePipeline([reason: "Scheduled", source: triggerSource, isRecheck: true])
}

private void processStatePipeline(Map request) {
    String reqReason = request.reason ?: "Scheduled"
    String reqSource = request.source ?: "Internal Pipeline"
    Boolean isRecheck = request.isRecheck ?: false
    
    String txId = "tx_${now()}"
    state.activeTransaction = txId

    logTrace "=== Mode Manager Adv [Pipeline: ${reqReason} | ${reqSource}] Begins ==="
    logTrace "START TRANSACTION #${txId} [Reason: ${reqReason} | Source: ${reqSource}]"

    try {
        String previousMode = location.mode
        String previousReason = (state.modeReason == "Normal") ? "Scheduled" : (state.modeReason ?: "Scheduled")
        
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
    if (requestedReason == "Normal") requestedReason = "Scheduled"

    String source = request.source ?: "Unknown"
    Boolean isBoundaryTrigger = request.isBoundaryTrigger ?: false
    Boolean forceReleaseLock = request.forceReleaseLock ?: false
    
    String currentActiveReason = (state.modeReason == "Normal") ? "Scheduled" : (state.modeReason ?: "Scheduled")
    int incomingRank = getReasonRank(requestedReason)
    int currentRank = getReasonRank(currentActiveReason)

    Boolean isHoldOverrideEnabled = getSettingBool("holdOverride", false)

    /* --- STRICT PRESENCE EVALUATION PRECEDENCE --- */
    Boolean isHome
    if (request.presenceValue != null) {
        isHome = (request.presenceValue == "present")
    } else if (request.simulatedHome != null) {
        isHome = request.simulatedHome
    } else {
        isHome = (homeSwitch?.currentValue("switch") == "on")
    }

    Boolean isSleeping = (request.simulatedAwake != null) ? !request.simulatedAwake : (awakeSwitch?.currentValue("switch") == "off")

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
        if (isBoundaryTrigger) {
            logInfo "Schedule period boundary hit. Releasing active '${currentActiveReason}' state to 'Scheduled'."
            requestedReason = "Scheduled"
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

    // Force isSleeping flag if target mode is explicitly equal to configured sleepMode
    String configuredSleepMode = sleepMode?.toString() ?: "Sleeping"
    Boolean finalIsSleeping = (targetMode == configuredSleepMode)

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
    if (!newMode) {
        logWarn "Decision produced null target mode. Aborting mode apply."
        return
    }

    String previousReason = decision.previousReasonAtStart ?: state.modeReason ?: "Scheduled"
    if (previousReason == "Normal") previousReason = "Scheduled"
    
    String previousMode = decision.previousModeAtStart ?: location.mode
    Boolean suspendOnOverride = getSettingBool("suspendScheduler", false)

    if (suspendOnOverride) {
        if (newReason == "Override" && previousReason != "Override") {
            logInfo "Entering Override state with suspendScheduler enabled. Pausing CRON schedules."
            logTrace "CALL PATH: applyDecision() -> State Transition (Non-Override -> Override) -> calling stopPeriodSchedules()"
            stopPeriodSchedules()
        } else if (previousReason == "Override" && newReason != "Override") {
            logInfo "Exiting Override state. Resuming CRON schedules."
            logTrace "CALL PATH: applyDecision() -> State Transition (Override -> ${newReason}) -> calling restartPeriodSchedules()"
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
        logInfo "Mode recheck completed: Location Mode remains '${newMode}' and Reason remains '${newReason}' | (${decision.source})"
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
            String currentHsm = location.hsmStatus
            String expectedHsmStatus = "armedAway"
            if (currentHsm != expectedHsmStatus && state.lastHsmState != expectedHsmStatus) {
                logInfo "HSM changed: '${currentHsm ?: state.lastHsmState ?: 'unknown'}' -> '${expectedHsmStatus}'"
                logDebug "Executing HSM arming command -> armAway (Reason: ${reason})"
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
                logInfo "HSM changed: '${currentHsm ?: state.lastHsmState ?: 'unknown'}' -> '${expectedHsmStatus}'"
                logDebug "Executing HSM state update command -> ${hsmCmd} (Reason: ${reason})"
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

    if (modeChanged || reasonChanged || reason == "Reboot") {
        dispatchTileNotification(targetMode, reason, isSleeping, modeChanged, reasonChanged)
    } else if (decision.isRecheck == true) {
        dispatchTileNotification(targetMode, "RecheckNoChange", isSleeping, false, false)
    } else {
        dispatchTileNotification(targetMode, reason, isSleeping, false, false)
    }
}

private void syncSwitch(def device, String targetState) {
    if (device != null && device.hasCommand(targetState)) {
        if (device.currentValue("switch") != targetState) {
            String devId = device.id.toString()
            String compositeKey = "${devId}:${targetState}"
            logTrace "Syncing Output Device '${device.displayName}' [ID: ${devId} | CompositeKey: ${compositeKey}] -> ${targetState.toUpperCase()}"

            long currentMs = now()
            Map pendingMap = (atomicState.pendingOutputSyncs != null) ? new HashMap(atomicState.pendingOutputSyncs) : [:]

            // Prune expired markers
            pendingMap.entrySet().removeIf { entry ->
                Map marker = entry.value as Map
                return (marker == null || currentMs > (marker.expires as long))
            }

            // REGISTER MARKER FIRST BEFORE EXECUTING COMMAND
            pendingMap[compositeKey] = [devId: devId, value: targetState, expires: currentMs + 5000]
            atomicState.pendingOutputSyncs = pendingMap

            logTrace "REGISTER ATOMIC MARKER: Key='${compositeKey}' | Target='${targetState}' | ActiveMapKeys=${pendingMap.keySet()}"
            
            // Execute device command AFTER marker registration
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
        logTrace "Notification suppressed: Mode '${modeVal}' and Reason '${reasonVal}' have not changed since last notification dispatch."
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
            case "Scheduled":   
                shouldNotify = getSettingBool("notifyOnScheduled", false) || getSettingBool("notifyOnNormal", false)
                template = settings.templateScheduled ?: settings.templateNormal ?: "Mode scheduled transition: %mode% (from %prevMode% at %time%)"
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
    String prevReason = (state.previousReason == "Normal") ? "Scheduled" : (state.previousReason ?: "Unknown")
    String displayReason = (reasonVal == "RecheckNoChange") ? ((state.modeReason == "Normal") ? "Scheduled" : (state.modeReason ?: "Scheduled")) : reasonVal

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
        
        notificationDevice.each { dev -> 
            logTrace "Sending push notification to '${dev.displayName}'"
            dev.deviceNotification(pushMsg) 
        }
    }

    if (speechDevice) {
        // Evaluate isSleeping strictly against incoming decision state
        Boolean isSleeping = (isSleepingState == true)
        if (isSleeping && getSettingBool("suppressAudioWhenSleeping", true)) {
            logTrace "Audio notification suppressed because system target state is Sleeping."
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
    String currentReason = reason ?: state.modeReason ?: "Scheduled"
    if (currentReason == "Normal") currentReason = "Scheduled"

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

private boolean isInternalTransaction(def evt = null) {
    logTrace "CHECKING INTERNAL TRANSACTION: activeTx='${state.activeTransaction}' | evtDevice='${evt?.device?.displayName}'"

    if (state.activeTransaction != null) {
        logTrace "Suppressing event generated during active pipeline transaction (${state.activeTransaction})"
        return true
    }

    if (evt != null) {
        def rawDevId = evt.deviceId ?: evt.device?.id
        if (rawDevId != null) {
            String devId = rawDevId.toString()
            String evtVal = evt.value?.toString()
            String compositeKey = "${devId}:${evtVal}"
            
            logTrace "INSPECT EVENT ID: rawDevId=${rawDevId} -> String devId='${devId}' | CompositeKey='${compositeKey}' | Value='${evtVal}'"

            Map pendingMap = (atomicState.pendingOutputSyncs != null) ? new HashMap(atomicState.pendingOutputSyncs) : [:]

            logTrace "PENDING ATOMIC MAP KEYS: ${pendingMap.keySet()}"

            if (pendingMap.containsKey(compositeKey)) {
                Map marker = pendingMap[compositeKey] as Map
                long currentMs = now()

                logTrace "FOUND ATOMIC MARKER FOR KEY '${compositeKey}': MarkerValue='${marker?.value}' | MarkerExpires=${marker?.expires} | CurrentTime=${currentMs}"

                if (marker && marker.value == evtVal && currentMs <= (marker.expires as long)) {
                    logInfo "SUCCESS: Suppressed app-generated internal output event from '${evt.device?.displayName}' [ID: ${devId} | CompositeKey: ${compositeKey}]"
                    
                    // Consume marker atomically so it is one-shot
                    pendingMap.remove(compositeKey)
                    atomicState.pendingOutputSyncs = pendingMap
                    return true
                } else if (marker && currentMs > (marker.expires as long)) {
                    logTrace "Expired suppression marker removed for '${evt.device?.displayName}' [CompositeKey: ${compositeKey}]"
                    pendingMap.remove(compositeKey)
                    atomicState.pendingOutputSyncs = pendingMap
                }
            } else {
                logTrace "Event from '${evt.device?.displayName}' [CompositeKey: ${compositeKey}] has no active atomic suppression marker -> Treating as External Input."
            }
        }
    }

    return false
}

private boolean consumeVoiceMarker() {
    long expires = atomicState.alexaVoiceMarkerExpires ? (atomicState.alexaVoiceMarkerExpires as long) : 0L
    long currentMs = now()
    
    logTrace "CONSUME VOICE MARKER INSPECTION: expiresMs=${expires} | currentMs=${currentMs} | diffMs=${expires - currentMs}"
    
    if (expires > 0L && currentMs <= expires) {
        atomicState.alexaVoiceMarkerExpires = 0L
        logInfo "Alexa Voice Source Marker verified and consumed successfully (Remaining window was ${expires - currentMs}ms)."
        return true
    } else if (expires > 0L) {
        logDebug "Alexa Voice Source Marker checked, but expired ${currentMs - expires}ms ago."
        atomicState.alexaVoiceMarkerExpires = 0L
    } else {
        logTrace "Alexa Voice Source Marker checked: No active marker stored."
    }
    return false
}

def appButtonHandler(btn) {
    String btnName = "${btn}".toString()
    logTrace "=== Mode Manager Adv [Handler: UI Button (${btnName})] Begins ==="
    if (btnName == "btnTrigger") {
        long currentMs = now()
        if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) {
            logTrace "=== Mode Manager Adv [Handler: UI Button (${btnName})] Ends (Debounced) ==="
            return
        }
        state.lastTriggerTime = currentMs

        logTrace "CALL PATH: UI Button ('Evaluate & Set Active Mode Now') clicked -> calling recheckSchedule()"
        recheckSchedule("Manual UI Button ('Evaluate & Set Active Mode Now')")
    } else if (btnName == "btnForceTestEvaluation") {
        logTrace "CALL PATH: UI Button ('btnForceTestEvaluation') clicked -> calling executeForcedTestEvaluation()"
        executeForcedTestEvaluation()
    }
    logTrace "=== Mode Manager Adv [Handler: UI Button (${btnName})] Ends ==="
}

def updateSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Trigger Switch"
    logTrace "=== Mode Manager Adv [Handler: Trigger Switch '${devName}'] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Trigger Switch '${devName}'] Ends (Internal Transaction) ==="
        return
    }
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) {
        logTrace "=== Mode Manager Adv [Handler: Trigger Switch '${devName}'] Ends (Debounced) ==="
        return
    }
    state.lastTriggerTime = currentMs

    logTrace "CALL PATH: updateSwitchHandler('${devName}') -> calling recheckSchedule()"
    recheckSchedule("Trigger Switch '${devName}'")
    logTrace "=== Mode Manager Adv [Handler: Trigger Switch '${devName}'] Ends ==="
}

def updateButtonHandler(evt) {
    String devName = evt.device?.displayName ?: "Trigger Button"
    logTrace "=== Mode Manager Adv [Handler: Trigger Button '${devName}' (Button #${evt.value})] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Trigger Button '${devName}'] Ends (Internal Transaction) ==="
        return
    }
    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 2000)) {
        logTrace "=== Mode Manager Adv [Handler: Trigger Button '${devName}'] Ends (Debounced) ==="
        return
    }
    state.lastTriggerTime = currentMs

    logTrace "CALL PATH: updateButtonHandler('${devName}') -> calling recheckSchedule()"
    recheckSchedule("Trigger Button '${devName}' (Button #${evt.value})")
    logTrace "=== Mode Manager Adv [Handler: Trigger Button '${devName}' (Button #${evt.value})] Ends ==="
}

def vSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Virtual Switch"
    logTrace "=== Mode Manager Adv [Handler: Virtual Period Switch '${devName}'] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Virtual Period Switch '${devName}'] Ends (Internal Transaction) ==="
        return
    }
    if (evt.value != "on") {
        logTrace "=== Mode Manager Adv [Handler: Virtual Period Switch '${devName}'] Ends (Value != ON) ==="
        return
    }

    long currentMs = now()
    if (state.lastTriggerTime && (currentMs - state.lastTriggerTime < 1000)) {
        logTrace "=== Mode Manager Adv [Handler: Virtual Period Switch '${devName}'] Ends (Debounced) ==="
        return
    }
    state.lastTriggerTime = currentMs

    def rawDevId = evt.deviceId ?: evt.device?.id
    if (rawDevId == null) {
        logTrace "=== Mode Manager Adv [Handler: Virtual Period Switch '${devName}'] Ends (Null Dev ID) ==="
        return
    }
    String deviceId = "${rawDevId}".toString()

    if ((sleepSwitch && "${sleepSwitch.id}".toString() == deviceId) || (awakeSwitch && "${awakeSwitch.id}".toString() == deviceId)) {
        logTrace "vSwitchHandler: Ignored event from '${evt.device?.displayName}' because it is assigned to Section 2 Sleep Architecture."
        logTrace "=== Mode Manager Adv [Handler: Virtual Period Switch '${devName}'] Ends ==="
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
        logInfo "Period virtual switch '${devName}' toggled externally."
        logTrace "CALL PATH: vSwitchHandler('${devName}') -> External Override -> dispatching processStatePipeline(Reason: Override)"
        processStatePipeline([
            reason: "Override",
            targetMode: targetPeriod.mode,
            periodKey: targetPeriod.key,
            source: "Virtual Switch (${devName})"
        ])
    }
    logTrace "=== Mode Manager Adv [Handler: Virtual Period Switch '${devName}'] Ends ==="
}

def presenceHandler(evt) {
    String devName = evt.device?.displayName ?: "Master Presence Sensor"
    logTrace "=== Mode Manager Adv [Handler: Master Presence '${devName}' (${evt.value})] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Master Presence '${devName}'] Ends (Internal Transaction) ==="
        return
    }
    logInfo "Master presence sensor '${devName}' changed to '${evt.value}'"
    logTrace "CALL PATH: presenceHandler('${devName}') [${evt.value}] -> dispatching processStatePipeline(Reason: Presence)"
    processStatePipeline([reason: "Presence", presenceValue: evt.value, source: "Master Presence Sensor"])
    logTrace "=== Mode Manager Adv [Handler: Master Presence '${devName}'] Ends ==="
}

def homeSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Home Switch"
    logTrace "=== Mode Manager Adv [Handler: Home Switch '${devName}' (${evt.value})] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Home Switch '${devName}'] Ends (Internal Transaction) ==="
        return
    }
    logInfo "Home switch '${devName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "on") ? "present" : "not present"
    logTrace "CALL PATH: homeSwitchHandler('${devName}') [${evt.value}] -> dispatching processStatePipeline(Reason: Presence)"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Home Switch"])
    logTrace "=== Mode Manager Adv [Handler: Home Switch '${devName}'] Ends ==="
}

def awaySwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Away Switch"
    logTrace "=== Mode Manager Adv [Handler: Away Switch '${devName}' (${evt.value})] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Away Switch '${devName}'] Ends (Internal Transaction) ==="
        return
    }
    logInfo "Away switch '${devName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "off") ? "present" : "not present"
    logTrace "CALL PATH: awaySwitchHandler('${devName}') [${evt.value}] -> dispatching processStatePipeline(Reason: Presence)"
    processStatePipeline([reason: "Presence", presenceValue: presenceVal, source: "Away Switch"])
    logTrace "=== Mode Manager Adv [Handler: Away Switch '${devName}'] Ends ==="
}

def alexaModeSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Alexa Mode Switch"
    logTrace "=== Mode Manager Adv [Handler: Alexa Mode Switch '${devName}' (${evt.value})] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Alexa Mode Switch '${devName}'] Ends (Internal Transaction) ==="
        return
    }
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
    logTrace "=== Mode Manager Adv [Handler: Alexa Mode Switch '${devName}'] Ends ==="
}

def alexaVoiceMarkerSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Alexa Voice Marker Switch"
    logTrace "=== Mode Manager Adv [Handler: Alexa Voice Marker Switch '${devName}' (${evt.value})] Begins ==="
    
    if (evt.value == "on") {
        long currentMs = now()
        long expiresMs = currentMs + 5000
        atomicState.alexaVoiceMarkerExpires = expiresMs
        
        logInfo "VOICE MARKER SIGNAL RECEIVED: Switch '${devName}' turned ON. Armed voice source marker for 5s (Expires at timestamp ${expiresMs})."
        
        if (alexaVoiceMarkerSwitch && alexaVoiceMarkerSwitch.hasCommand("off")) {
            logDebug "Resetting Voice Marker switch '${devName}' back to OFF for next one-shot trigger."
            syncSwitch(alexaVoiceMarkerSwitch, "off")
        }
    } else {
        logTrace "Voice Marker switch '${devName}' reverted to OFF."
    }
    logTrace "=== Mode Manager Adv [Handler: Alexa Voice Marker Switch '${devName}'] Ends ==="
}

def awakeSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Awake Switch"
    logTrace "=== Mode Manager Adv [Handler: Awake Switch '${devName}' (${evt.value})] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Awake Switch '${devName}'] Ends (Internal Transaction) ==="
        return
    }
    
    Boolean isVoiceSource = consumeVoiceMarker()
    String evalReason = isVoiceSource ? "Voice" : "Scheduled"
    String srcLabel = isVoiceSource ? "Awake Switch '${devName}' (${evt.value} via Alexa Routine)" : "Awake Switch '${devName}' (${evt.value})"

    logInfo "Awake switch '${devName}' changed to '${evt.value}' (Attributed Reason: '${evalReason}')"
    logDebug "Awake Switch evaluation path selected -> isVoiceSource=${isVoiceSource} | Reason='${evalReason}'"
    
    if (isVoiceSource) {
        Map activePeriod = getActiveTimePeriodInfo()
        String targetMode = (evt.value == "off") ? (sleepMode?.toString() ?: "Sleeping") : (activePeriod?.mode ?: location.mode)
        String targetKey = (evt.value == "off") ? "sleeping" : activePeriod?.key
        
        processStatePipeline([
            reason: "Voice",
            targetMode: targetMode,
            periodKey: targetKey,
            source: srcLabel
        ])
    } else {
        recheckSchedule(srcLabel)
    }
    logTrace "=== Mode Manager Adv [Handler: Awake Switch '${devName}'] Ends ==="
}

def sleepSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Sleep Switch"
    logTrace "=== Mode Manager Adv [Handler: Sleep Switch '${devName}'] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Sleep Switch '${devName}'] Ends (Internal Transaction) ==="
        return
    }
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
    logTrace "=== Mode Manager Adv [Handler: Sleep Switch '${devName}'] Ends ==="
}

def alexaAwakeSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Alexa Awake Switch"
    logTrace "=== Mode Manager Adv [Handler: Alexa Awake Switch '${devName}' (${evt.value})] Begins ==="
    if (isInternalTransaction(evt)) {
        logTrace "=== Mode Manager Adv [Handler: Alexa Awake Switch '${devName}'] Ends (Internal Transaction) ==="
        return
    }
    
    Boolean markerFound = consumeVoiceMarker()
    logInfo "Alexa Awake Switch changed to '${evt.value}' (Voice Marker Active: ${markerFound})"
    
    if (evt.value == "off") {
        logTrace "CALL PATH: alexaAwakeSwitchHandler [OFF] -> dispatching processStatePipeline(Reason: Voice, Mode: Sleeping)"
        processStatePipeline([
            reason: "Voice",
            targetMode: sleepMode?.toString() ?: "Sleeping",
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
    logTrace "=== Mode Manager Adv [Handler: Alexa Awake Switch '${devName}'] Ends ==="
}

def executeForcedTestEvaluation() {
    logTrace "=== Mode Manager Adv [Handler: Simulation Control Panel] Begins ==="
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
    logTrace "=== Mode Manager Adv [Handler: Simulation Control Panel] Ends ==="
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