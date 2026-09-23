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
 * v1.1.70 (2026-08-27) - Terminology Shift to Control Source Architecture:
 *                        - Refactored all GUI, tile displays, preferences, logs, and state parameters from 'Reason' to 'Control Source'.
 *                        - Clarified the distinction between short-lived pipeline Triggers and persistent Control Sources (Voice, Presence, Override, Scheduled, Reboot).
 * v1.1.66 (2026-08-26) - Clean HTML Payload Optimization:
 *                        - Removed 'white-space: nowrap' and 'font-size:1.0em' CSS properties from HTML push outputs.
 *                        - Outputs color-only markup so tile drivers natively handle wrapping and font scaling.
 * v1.1.65 (2026-08-26) - Inline HTML Output Refactor:
 *                        - Converted HTML push notification wrappers from block <div> to inline <span> tags to prevent unwanted line breaks.
 * v1.1.64 (2026-08-26) - Snapshot Logging Level Adjustment:
 *                        - Demoted raw JSON snapshot payload diff trace lines inside updated() to logDebug to minimize log noise.
 * v1.1.63 (2026-08-26) - Semantic Refactor to State Recheck & State Evaluation Engine:
 *                        - Refactored all log strings, preference templates, UI labels, and helper methods from 'Mode Recheck' to 'State Recheck' / 'State Evaluation'.
 *                        - Renamed recheckSchedule() -> recheckState() across the entire pipeline while keeping transaction parameters intact.
 * v1.1.62 (2026-08-26) - Deterministic Snapshot Canonicalization & Diff Debugger:
 *                        - Added alphabetical sorting to settings.keySet() and List values inside captureSettingsSnapshot() to ensure canonical JSON hashing regardless of Hubitat map iteration order.
 *                        - Added raw JSON snapshot retention (state.currentSettingsJson & state.lastSettingsJson) and trace diff logging inside updated() to pinpoint exact preference discrepancies.
 * v1.1.61 (2026-08-25) - Stale HSM Pending Expiration Reset Patch:
 *                        - Fixed transient stale desync warning inside syncHsmState by explicitly zeroing out state.pendingHsmExpires when pending targets are cleared.
 * v1.1.60 (2026-08-25) - Alexa Awake Switch Pre-emption & Intent Override Fix:
 *                        - Added explicit 'isAwakeRequested' parameter to alexaAwakeSwitchHandler state pipeline payload.
 *                        - Overhauled calculateDecision() to prioritize incoming Alexa awake/sleep intent over stale physical switch reads.
 */
// [KEEP-EXACT] see changelog.txt for v1.0.1 - v1.1.59

import java.util.concurrent.ConcurrentHashMap
import groovy.transform.Field

// Thread-safe heap registers & sequence locks
@Field static ConcurrentHashMap<String, String> pendingAckTokens = new ConcurrentHashMap<>()
@Field static ConcurrentHashMap<String, Long> lastTriggerTimes = new ConcurrentHashMap<>()
@Field static Object txCounterLock = new Object()
@Field static Long txCounter = 0L

private static String getNextTxId() {
    synchronized(txCounterLock) {
        txCounter++
        return "tx_${new Date().getTime()}_${txCounter}"
    }
}

static String version() { return '1.1.70' }

definition(
    name: "Mode Manager Advanced",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Advanced Hubitat Mode Manager driven by master presence, reverse-mirrored presence and sleep/awake switches, dynamic time periods, virtual mode indicators, Alexa Mode sync, HSM control, and Dashboard Notification Tile outputs.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
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
            String controlSource = state.controlSource ?: "Initialization / Idle"
            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #27AE60; padding:10px; border-radius:4px; font-size:13px;'>" +
                      "<b>Current Active Mode:</b> <span style='color:#27AE60; font-weight:bold;'>${currentMode}</span> &nbsp;|&nbsp; " +
                      "<b>Control Source:</b> <i>${controlSource}</i></div>"
        }

        /* CATEGORY A: INPUT ARCHITECTURE */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:10px;'>CATEGORY A: INPUT ARCHITECTURE (Presence, Sleep, Schedule)</div>") {}

        section("<b>SECTION 1: Presence Architecture & State Switches</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Presence Architecture:</b> Master Presence Sensor updates Home Switch (Primary Authoritative Input). Away Switch is maintained as its inverse mirror.</div>"
            
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Presence Modes:</b> When presence is <b>Away</b>, Location Mode automatically switches to <b>Away</b>. When presence returns to <b>Home</b>, the app automatically evaluates your active Section 3 Time Period schedule (or Section 2 Sleep state). No manual mode assignment is needed for presence.</div>"

            input name: "masterPresence", type: "capability.presenceSensor", title: "<b>Master Presence Sensor</b> <i>(OwnTracks - Jim)</i>", required: true
            input name: "homeSwitch", type: "capability.switch", title: "<b>Home Switch</b> <i>(Primary Input)</i>", required: true
            input name: "awaySwitch", type: "capability.switch", title: "<b>Away Switch</b> <i>(Inverse Mirror)</i>", required: true
        }
        
        section("<b>SECTION 2: Sleep Architecture & Behavioral Overlay</b>", hideable: true, hidden: true) {
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

        section("<b>SECTION 3: Time Period & Target Mode Schedule</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Schedule Architecture:</b> Dynamic chronological time periods evaluated sequentially throughout the day when Home and Awake.</div>"
            
            Map activePeriod = getActiveTimePeriodInfo()
            String activePeriodKey = activePeriod?.key ?: "Unknown"

            Boolean isSleeping = (awakeSwitch ? (awakeSwitch.currentValue("switch") == "off") : false)
            String overlayText = isSleeping ? " <span style='color:#8E44AD; font-weight:bold;'>(Overridden by Sleeping Overlay)</span>" : ""
            
            paragraph "<div style='background-color:#EAEDED; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px; margin-bottom:8px;'>" +
                      "⏱️ <b>Current Schedule Block:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodKey.toUpperCase()}</span> " +
                      "(Schedule Target: <b>${activePeriod?.mode ?: 'None'}</b>)${overlayText}</div>"

            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Schedule Wrap-Around:</b> The schedule operates as a continuous 24-hour circular list. Each period remains active until the next boundary start time. The final period of the day (e.g. Late Evening) carries over after midnight until the first scheduled period of the morning begins.</div>"

            paragraph "<div style='color:#555; font-size:12px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Note on Period Switches:</b> These devices serve a dual role. Mode Manager turns a switch <b>ON</b> to indicate the active period. Manually turning a switch <b>ON</b> from a dashboard requests that specific period as an immediate <b>Override</b>.</div>"

            input name: "timeWeeHours", type: "time", title: "Wee Hours Start", required: true, defaultValue: "00:30", width: 6
            input name: "weeHoursMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Late Evening", width: 3
            input name: "vSwitchWeeHours", type: "capability.switch", title: "<b>Period Indicator / Override Switch</b>", required: false, width: 3
            
            input name: "timeEarlyMorning", type: "time", title: "Early Morning Start", required: true, defaultValue: "04:45", width: 6
            input name: "earlyMorningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Early Morning", width: 3
            input name: "vSwitchEarlyMorning", type: "capability.switch", title: "<b>Period Indicator / Override Switch</b>", required: false, width: 3
            
            input name: "timeMorning", type: "time", title: "Morning Start", required: true, defaultValue: "07:30", width: 6
            input name: "morningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Morning", width: 3
            input name: "vSwitchMorning", type: "capability.switch", title: "<b>Period Indicator / Override Switch</b>", required: false, width: 3
            
            input name: "timeDay", type: "time", title: "Day Start", required: true, defaultValue: "10:00", width: 6
            input name: "dayMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Day", width: 3
            input name: "vSwitchDay", type: "capability.switch", title: "<b>Period Indicator / Override Switch</b>", required: false, width: 3
            
            input name: "timeEvening", type: "time", title: "Evening Start", required: true, defaultValue: "17:00", width: 6
            input name: "eveningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Evening", width: 3
            input name: "vSwitchEvening", type: "capability.switch", title: "<b>Period Indicator / Override Switch</b>", required: false, width: 3
            
            input name: "timeLateEvening", type: "time", title: "Late Evening Start", required: true, defaultValue: "21:30", width: 6
            input name: "lateEveningMode", type: "mode", title: "Target Mode", required: true, defaultValue: "Late Evening", width: 3
            input name: "vSwitchLateEvening", type: "capability.switch", title: "<b>Period Indicator / Override Switch</b>", required: false, width: 3
        }

        /* CATEGORY B: SYSTEM INTEGRATIONS */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY B: SYSTEM INTEGRATIONS (HSM & Alexa)</div>") {}

        section("<b>SECTION 4: Hubitat Safety Monitor (HSM) Integration</b>", hideable: true, hidden: true) {
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

        section("<b>SECTION 5: Alexa Ecosystem & Voice Marker Integration</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Alexa Ecosystem Sync:</b> Virtual switches for bidirectional status synchronization and voice command attribution.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Presence Integration</span>"
            input name: "alexaPresenceSwitch", type: "capability.switch", title: "Alexa Presence Virtual Switch <i>(ON = Home, OFF = Away)</i>", required: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Sleep & Awake Integration</span>"
            input name: "alexaAwakeSwitch", type: "capability.switch", title: "Alexa Awake Virtual Switch <i>(ON = Awake, OFF = Sleeping)</i>", required: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Voice Marker Attribution Signal</span>"
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Voice Marker Signal:</b> Pulse this switch ON in Alexa Routines to give Mode Manager a 2.5-second window to attribute toggles as <b>Voice</b> triggers.</div>"
            input name: "alexaVoiceMarkerSwitch", type: "capability.switch", title: "<b>Alexa - Voice Marker Source Switch</b>", required: false
        }

        /* CATEGORY C: OUTPUTS & NOTIFICATION ENGINE */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY C: OUTPUTS & NOTIFICATION ENGINE (Tiles, Push, Audio)</div>") {}

        section("<b>SECTION 6: Status Tile(s) and Output Devices</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Status Tiles Architecture:</b> Select virtual status devices for dynamic dashboard rendering and status output tracking.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>\"Ground-Truth\" Virtual Tile Status Device</span>"
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Select a virtual device powered by the " +
                      "<a href='https://github.com/jshimota01/hubitat/blob/main/Drivers/virtual_mode_status_tracker_tile/virtual_mode_status_tracker_tile_device.groovy' target='_blank' style='color:#2980B9; font-weight:bold; text-decoration:underline;'>" +
                      "Virtual Mode Status & State Tracker Tile Driver</a> to receive real-time status updates.</div>"

            input name: "statusTileDevice", type: "capability.actuator", title: "<b>Virtual \"Ground-Truth\" Status Tile Device</b>", required: false, multiple: false

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Notification Target Device(s) Selection</span>"

            input name: "notificationDevice", type: "capability.notification", title: "<b>Push Notification Device(s)</b>", required: false, multiple: true
            input name: "notificationFormat", type: "enum", title: "<b>Push Notification Text Format</b>", options: ["plain": "Plain Text", "html": "Formatted HTML"], defaultValue: "plain", required: false
            
            input name: "notificationDeviceMobile", type: "capability.notification", title: "<b>Push (Mobile) Device(s)</b>", required: false, multiple: true
            input name: "notificationFormatMobile", type: "enum", title: "<b>Push (Mobile) Text Format</b>", options: ["plain": "Plain Text", "html": "Formatted HTML"], defaultValue: "plain", required: false

            input name: "speechDevice", type: "capability.speechSynthesis", title: "<b>Audio / Speech Device(s)</b>", required: false, multiple: true
        }

        section("<b>SECTION 7: Global Notification Controls & Master Switches</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Global Notification Preferences:</b> Manage master output toggles and global suppression rules across all notification targets.</div>"

            input name: "enableNotifications", type: "bool", title: "<b>Enable All Notification Outputs</b>", defaultValue: true

            paragraph "<div style='padding: 6px 10px; background-color: #F8F9FA; border-left: 3px solid #3498DB; border-radius: 4px; margin-top: 6px; margin-bottom: 8px;'>" +
                      "<span style='color: #2C3E50; font-size: 12px; font-weight: bold;'>Enabled Notification Types (Master Triggers):</span></div>"
            
            input name: "masterEnablePush", type: "bool", title: "<b>Standard Push</b>", defaultValue: true, width: 4
            input name: "masterEnablePushMobile", type: "bool", title: "<b>Mobile Push</b>", defaultValue: true, width: 4
            input name: "masterEnableAudio", type: "bool", title: "<b>Audio / Speech</b>", defaultValue: true, width: 4

            paragraph "<div style='margin-top: 10px;'></div>"
            input name: "suppressAudioWhenSleeping", type: "bool", title: "<b>Mute Audio/Speech devices while Mode is 'Sleeping'?</b>", defaultValue: true
        }

        section("<b>SECTION 8: Event Trigger Dispatch Matrix & Message Templates</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Integrated Trigger Matrix & Formatters:</b> Configure dispatch output toggles (Push, Mobile, Audio) and customize message format strings for each active Control Source.</div>"

            paragraph "<div style='width:85%; margin:4px auto 16px auto; background-color:#FFFFFF; border:1px solid #E0E0E0; border-left:5px solid #8E44AD; border-radius:6px; box-shadow:0 4px 8px rgba(0,0,0,0.1); padding:12px 16px; font-size:12px; font-family:sans-serif;'>" +
                      "  <div style='font-weight:bold; font-size:13px; color:#2C3E50; margin-bottom:6px;'>Notification Template Placeholders</div>" +
                      "  <div style='color:#555; margin-bottom:10px;'>Customize push and speech notification strings using dynamic placeholders below:</div>" +
                      "  <table style='width:100%; border-collapse:collapse; font-size:12px;'>" +
                      "    <tr style='background-color:#EAEDED; text-align:left; border-bottom:2px solid #BDC3C7;'>" +
                      "      <th style='padding:6px;'>Placeholder</th>" +
                      "      <th style='padding:6px;'>Description</th>" +
                      "      <th style='padding:6px;'>Example Output</th>" +
                      "    </tr>" +
                      "    <tr style='border-bottom:1px solid #E0E0E0;'>" +
                      "      <td style='padding:5px;'><code>%mode%</code></td>" +
                      "      <td style='padding:5px;'>Current Target Mode</td>" +
                      "      <td style='padding:5px;'><code>Morning</code></td>" +
                      "    </tr>" +
                      "    <tr style='border-bottom:1px solid #E0E0E0; background-color:#FAFAFA;'>" +
                      "      <td style='padding:5px;'><code>%prevMode%</code></td>" +
                      "      <td style='padding:5px;'>Previous Location Mode</td>" +
                      "      <td style='padding:5px;'><code>Sleeping</code></td>" +
                      "    </tr>" +
                      "    <tr style='border-bottom:1px solid #E0E0E0;'>" +
                      "      <td style='padding:5px;'><code>%controlSource%</code></td>" +
                      "      <td style='padding:5px;'>Active Control Source (Authoritative Input)</td>" +
                      "      <td style='padding:5px;'><code>Voice</code></td>" +
                      "    </tr>" +
                      "    <tr style='border-bottom:1px solid #E0E0E0; background-color:#FAFAFA;'>" +
                      "      <td style='padding:5px;'><code>%prevControlSource%</code></td>" +
                      "      <td style='padding:5px;'>Previous Control Source</td>" +
                      "      <td style='padding:5px;'><code>Scheduled</code></td>" +
                      "    </tr>" +
                      "    <tr style='border-bottom:1px solid #E0E0E0;'>" +
                      "      <td style='padding:5px;'><code>%time%</code></td>" +
                      "      <td style='padding:5px;'>Formatted Transition Time</td>" +
                      "      <td style='padding:5px;'><code>08:03 AM</code></td>" +
                      "    </tr>" +
                      "  </table>" +
                      "</div>"

            input name: "previewCause", type: "enum", title: "<b>Select Control Source to Preview:</b>", 
                  options: [
                      "Scheduled": "Scheduled Transitions", 
                      "Voice": "Voice / Alexa Triggers", 
                      "Presence": "Presence Changes", 
                      "Override": "Manual Override Changes", 
                      "Reboot": "Hub Reboot / Startup", 
                      "RecheckNoChange": "Passive State Alignment (Unchanged State)", 
                      "SourceOnly": "Control Source Changed Only", 
                      "ModeOnly": "Mode Changed Only"
                  ], 
                  defaultValue: "Scheduled", submitOnChange: true

            String selCause = settings.previewCause ?: "Scheduled"
            String sampleTime = new Date().format("hh:mm a", location.timeZone ?: TimeZone.getDefault())
            String sampleMode = location.mode ?: "Morning"
            String sampleControlSource = (selCause == "RecheckNoChange") ? "Scheduled" : selCause
            
            String selectedTemplate = null
            switch(selCause) {
                case "Scheduled":   selectedTemplate = settings.templateScheduled ?: "Mode scheduled transition: %mode% (from %prevMode% at %time%)"; break
                case "Voice":       selectedTemplate = settings.templateVoice ?: "Voice command changed mode to %mode% (Control Source: %controlSource% at %time%)"; break
                case "Presence":    selectedTemplate = settings.templatePresence ?: "Presence update changed mode to %mode% (was %prevMode% at %time%)"; break
                case "Override":    selectedTemplate = settings.templateOverride ?: "Manual Override activated: Mode set to %mode% (Control Source: %controlSource% at %time%)"; break
                case "Reboot":      selectedTemplate = settings.templateReboot ?: "System restarted following hub boot. Mode synchronized to %mode% (Control Source: %controlSource% at %time%)"; break
                case "RecheckNoChange": selectedTemplate = settings.templateRecheckNoChange ?: "State recheck completed: Still in %mode% (%controlSource%) at %time%"; break
                case "SourceOnly":  selectedTemplate = settings.templateSourceOnly ?: "Control Source updated to %controlSource% (Mode remains %mode% at %time%)"; break
                case "ModeOnly":    selectedTemplate = settings.templateModeOnly ?: "Mode changed to %mode% (Control Source remains %controlSource% at %time%)"; break
                default:            selectedTemplate = "Location Mode changed to %mode% (Control Source: %controlSource% at %time%)"; break
            }

            String previewStr = selectedTemplate
                .replace("%mode%", sampleMode)
                .replace("%controlSource%", sampleControlSource)
                .replace("%prevMode%", "Sleeping")
                .replace("%prevControlSource%", "Override")
                .replace("%time%", sampleTime)

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #16A085; padding:8px 12px; border-radius:4px; font-size:12px; margin-top:-6px; margin-bottom:14px;'>" +
                      "🔍 <b>Live Template Preview (${selCause}):</b> <i>\"${previewStr}\"</i></div>"

            paragraph "<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; font-size:12px; border-radius:4px;'>ACTIVE CONTROL SOURCE</div>", width: 6
            paragraph "<div style='background-color:#2C3E50; color:#FFF; padding:6px 0; font-weight:bold; font-size:12px; text-align:center; border-radius:4px;'>PUSH</div>", width: 2
            paragraph "<div style='background-color:#2C3E50; color:#FFF; padding:6px 0; font-weight:bold; font-size:12px; text-align:center; border-radius:4px;'>PUSH MOBILE</div>", width: 2
            paragraph "<div style='background-color:#2C3E50; color:#FFF; padding:6px 0; font-weight:bold; font-size:12px; text-align:center; border-radius:4px;'>AUDIO / SPEECH</div>", width: 2

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #2980B9; padding:6px 10px; border-radius:4px; font-weight:bold; font-size:13px; color:#2C3E50;'>Scheduled Transitions</div>" +
                      "<div style='color:#7F8C8D; font-size:11px; margin-left:10px; margin-top:2px;'>ℹ️ <b>Control Source (%controlSource%):</b> <code>Scheduled (Chronological time period boundaries)</code></div>", width: 6
            input name: "pushScheduled", type: "bool", title: "", defaultValue: false, width: 2
            input name: "pushMobileScheduled", type: "bool", title: "", defaultValue: false, width: 2
            input name: "audioScheduled", type: "bool", title: "", defaultValue: false, width: 2
            input name: "templateScheduled", type: "text", title: "<b>Message Text Template*</b>", defaultValue: "Mode scheduled transition: %mode% (from %prevMode% at %time%)", required: true, width: 12
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0 12px 0;'/>"

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #E67E22; padding:6px 10px; border-radius:4px; font-weight:bold; font-size:13px; color:#2C3E50;'>Voice / Alexa Triggers</div>" +
                      "<div style='color:#7F8C8D; font-size:11px; margin-left:10px; margin-top:2px;'>ℹ️ <b>Control Source (%controlSource%):</b> <code>Voice (Alexa Voice Routine verified via Voice Marker)</code></div>", width: 6
            input name: "pushVoice", type: "bool", title: "", defaultValue: false, width: 2
            input name: "pushMobileVoice", type: "bool", title: "", defaultValue: false, width: 2
            input name: "audioVoice", type: "bool", title: "", defaultValue: false, width: 2
            input name: "templateVoice", type: "text", title: "<b>Message Text Template*</b>", defaultValue: "Voice command changed mode to %mode% (Control Source: %controlSource% at %time%)", required: true, width: 12
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0 12px 0;'/>"

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #27AE60; padding:6px 10px; border-radius:4px; font-weight:bold; font-size:13px; color:#2C3E50;'>Presence Changes</div>" +
                      "<div style='color:#7F8C8D; font-size:11px; margin-left:10px; margin-top:2px;'>ℹ️ <b>Control Source (%controlSource%):</b> <code>Presence (Master Presence Sensor or Home/Away toggles)</code></div>", width: 6
            input name: "pushPresence", type: "bool", title: "", defaultValue: false, width: 2
            input name: "pushMobilePresence", type: "bool", title: "", defaultValue: false, width: 2
            input name: "audioPresence", type: "bool", title: "", defaultValue: false, width: 2
            input name: "templatePresence", type: "text", title: "<b>Message Text Template*</b>", defaultValue: "Presence update changed mode to %mode% (was %prevMode% at %time%)", required: true, width: 12
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0 12px 0;'/>"

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #C0392B; padding:6px 10px; border-radius:4px; font-weight:bold; font-size:13px; color:#2C3E50;'>Manual Override Changes</div>" +
                      "<div style='color:#7F8C8D; font-size:11px; margin-left:10px; margin-top:2px;'>ℹ️ <b>Control Source (%controlSource%):</b> <code>Override (Dashboard switch toggle or Awake switch override)</code></div>", width: 6
            input name: "pushOverride", type: "bool", title: "", defaultValue: false, width: 2
            input name: "pushMobileOverride", type: "bool", title: "", defaultValue: false, width: 2
            input name: "audioOverride", type: "bool", title: "", defaultValue: false, width: 2
            input name: "templateOverride", type: "text", title: "<b>Message Text Template*</b>", defaultValue: "Manual Override activated: Mode set to %mode% (Control Source: %controlSource% at %time%)", required: true, width: 12
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0 12px 0;'/>"

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #8E44AD; padding:6px 10px; border-radius:4px; font-weight:bold; font-size:13px; color:#2C3E50;'>Hub Reboot / Startup Synchronization</div>" +
                      "<div style='color:#7F8C8D; font-size:11px; margin-left:10px; margin-top:2px;'>ℹ️ <b>Control Source (%controlSource%):</b> <code>Reboot (Hub startup/reboot system alignment check)</code></div>", width: 6
            input name: "pushReboot", type: "bool", title: "", defaultValue: false, width: 2
            input name: "pushMobileReboot", type: "bool", title: "", defaultValue: false, width: 2
            input name: "audioReboot", type: "bool", title: "", defaultValue: false, width: 2
            input name: "templateReboot", type: "text", title: "<b>Message Text Template*</b>", defaultValue: "System restarted following hub boot. Mode synchronized to %mode% (Control Source: %controlSource% at %time%)", required: true, width: 12
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0 12px 0;'/>"

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #7F8C8D; padding:6px 10px; border-radius:4px; font-weight:bold; font-size:13px; color:#2C3E50;'>Passive State Alignment (Unchanged State)</div>" +
                      "<div style='color:#7F8C8D; font-size:11px; margin-left:10px; margin-top:2px;'>ℹ️ <b>Control Source (%controlSource%):</b> <code>RecheckNoChange (Periodic state evaluation where state/controlSource are unchanged)</code></div>", width: 6
            input name: "pushRecheckNoChange", type: "bool", title: "", defaultValue: false, width: 2
            input name: "pushMobileRecheckNoChange", type: "bool", title: "", defaultValue: false, width: 2
            input name: "audioRecheckNoChange", type: "bool", title: "", defaultValue: false, width: 2
            input name: "templateRecheckNoChange", type: "text", title: "<b>Message Text Template*</b>", defaultValue: "State recheck completed: Still in %mode% (%controlSource%) at %time%", required: true, width: 12
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0 12px 0;'/>"

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #D35400; padding:6px 10px; border-radius:4px; font-weight:bold; font-size:13px; color:#2C3E50;'>Control Source Changed Only (Mode Unchanged)</div>" +
                      "<div style='color:#7F8C8D; font-size:11px; margin-left:10px; margin-top:2px;'>ℹ️ <b>Control Source (%controlSource%):</b> Dynamic authority updated while Mode remains identical</div>", width: 6
            input name: "pushSourceOnly", type: "bool", title: "", defaultValue: false, width: 2
            input name: "pushMobileSourceOnly", type: "bool", title: "", defaultValue: false, width: 2
            input name: "audioSourceOnly", type: "bool", title: "", defaultValue: false, width: 2
            input name: "templateSourceOnly", type: "text", title: "<b>Message Text Template</b>", defaultValue: "Control Source updated to %controlSource% (Mode remains %mode% at %time%)", required: false, width: 12
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0 12px 0;'/>"

            paragraph "<div style='background-color:#F4F6F7; border-left:4px solid #16A085; padding:6px 10px; border-radius:4px; font-weight:bold; font-size:13px; color:#2C3E50;'>Mode Changed Only (Control Source Unchanged)</div>" +
                      "<div style='color:#7F8C8D; font-size:11px; margin-left:10px; margin-top:2px;'>ℹ️ <b>Control Source (%controlSource%):</b> Location Mode changed under an ongoing authority</div>", width: 6
            input name: "pushModeOnly", type: "bool", title: "", defaultValue: false, width: 2
            input name: "pushMobileModeOnly", type: "bool", title: "", defaultValue: false, width: 2
            input name: "audioModeOnly", type: "bool", title: "", defaultValue: false, width: 2
            input name: "templateModeOnly", type: "text", title: "<b>Message Text Template</b>", defaultValue: "Mode changed to %mode% (Control Source remains %controlSource% at %time%)", required: false, width: 12
        }

        /* CATEGORY D: DIAGNOSTICS & CONTROL ENGINE */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY D: DIAGNOSTICS & CONTROL ENGINE</div>") {}

        section("<b>SECTION 9: System Diagnostics, Overrides & Simulation Panel</b>", hideable: true, hidden: true) {
            input name: "holdOverride", type: "bool", title: "<b>Lock Override against lower-priority events?</b>", defaultValue: false
            input name: "suspendScheduler", type: "bool", title: "<b>Suspend CRON time period schedule during Override?</b>", defaultValue: false

            paragraph "<div style='background-color:#FEF9E7; border-left:4px solid #F39C12; padding:8px; border-radius:4px; font-size:12px; margin-top:6px; margin-bottom:10px;'>" +
                      "⚠️ <b>Note on Override Interactions:</b> If <b>Suspend Scheduler</b> is ON while <b>Lock Override</b> is OFF, time boundary CRON events are suspended and will not occur to release an active Override. Another state event, manual switch toggle, or resynchronization button press is required to release the override state.</div>"

            input name: "updateTriggerSwitch", type: "capability.switch", title: "Switch(s) to Trigger Evaluation / Update", required: false, multiple: true
            input name: "updateTriggerButton", type: "capability.pushableButton", title: "Button(s) to Trigger Evaluation / Update", required: false, multiple: true

            input name: "testHome", type: "bool", title: "<b>Force Home Status?</b>", defaultValue: true
            input name: "testAwake", type: "bool", title: "<b>Force Awake Status?</b>", defaultValue: true
            input name: "testPeriodKey", type: "enum", title: "<b>Select Period for Simulation</b>", required: false, options: ["weeHours": "Wee Hours", "earlyMorning": "Early Morning", "morning": "Morning", "day": "Day", "evening": "Evening", "lateEvening": "Late Evening"]
            input name: "testControlSource", type: "enum", title: "<b>Control Source for Simulation</b>", defaultValue: "Override", required: false, options: ["Override": "Override", "Scheduled": "Scheduled", "Presence": "Presence", "Voice": "Voice"]

            input name: "btnForceTestEvaluation", type: "button", title: "Force Evaluation & Sync Switches"
        }

        section("<b>SECTION 10: System Startup & Boot Guard</b>", hideable: true, hidden: true) {
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

        section("<b>App Preferences</b>", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            input name: "showModeInLabel", type: "bool", title: "Show Active Mode in App Label?", defaultValue: true
            input name: "showControlSourceInLabel", type: "bool", title: "Show Control Source in App Label?", defaultValue: true

            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
            input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false
        }
        
        section() {
            input name: "btnTrigger", type: "button", title: "<b>Resynchronize System & Reset Active Mode</b>"
        }
    }
}

/* =========================================================================================
   APPLICATION LIFECYCLE & INFRASTRUCTURE SCHEDULER
   ========================================================================================= */

def installed() {
    logTrace "=== Mode Manager Adv [installed] Begins ==="
    logDebug "installed() executing v${version()}..."
    seedLoggingState()
    state.installedVersion = version()
    state.lastManageHSM = getSettingBool("manageHSM", true)
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    state.lastSettingsJson = state.currentSettingsJson
    
    purgePendingAckTokens()
    resetHsmStateVariables()
    lastTriggerTimes.clear()
    
    initialize()
    recheckState("App Installed")
    logTrace "=== Mode Manager Adv [installed] Ends ==="
}

def uninstalled() {
    logTrace "=== Mode Manager Adv [uninstalled] Begins ==="
    unsubscribe()
    stopPeriodSchedules()
    unschedule()
    purgePendingAckTokens()
    resetHsmStateVariables()
    lastTriggerTimes.clear()
    logTrace "=== Mode Manager Adv [uninstalled] Ends ==="
}

def updated() {
    logTrace "=== Mode Manager Adv [updated] Begins ==="
    logDebug "updated() executing v${version()}..."
    
    String currentVersion = version()
    String previousVersion = state.installedVersion ?: "Unknown"
    
    if (previousVersion != currentVersion) {
        logWarn "🚀 APP VERSION CHANGED: Upgraded from v${previousVersion} -> v${currentVersion}"
        state.installedVersion = currentVersion
    }

    checkLoggingChanges()
    checkHsmSettingChanges()
    
    String currentSnapshot = captureSettingsSnapshot()
    String previousSnapshot = state.lastSettingsSnapshot
    Boolean settingsChanged = (previousSnapshot == null || previousSnapshot != currentSnapshot)

    logTrace "Settings Snapshot Comparison -> Prev Hash: [${previousSnapshot}] | Current Hash: [${currentSnapshot}] | Changed: ${settingsChanged}"
    
    if (settingsChanged && previousSnapshot != null) {
        logDebug "SNAPSHOT DIFF DETECTED!"
        logDebug "Previous Snapshot Data: ${state.lastSettingsJson ?: 'None (First run or upgraded from prior version)'}"
        logDebug "Current Snapshot Data:  ${state.currentSettingsJson}"
    }

    if (settingsChanged) {
        logInfo "Settings modification or initial snapshot detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        state.lastSettingsJson = state.currentSettingsJson
        
        unsubscribe()
        unschedule("disableDebugLogging")
        if (getSettingBool("logDebugEnable", true)) {
            runIn(1800, disableDebugLogging)
        }
        
        initialize()
        updatePresentation([targetMode: location.mode, controlSource: state.controlSource ?: "Scheduled"])
        recheckState("App Preferences Modified", false, true)
    } else {
        logInfo "App closed via Done without setting changes. Skipping re-initialization and state evaluation."
        updatePresentation([targetMode: location.mode, controlSource: state.controlSource ?: "Scheduled"])
    }
    logTrace "=== Mode Manager Adv [updated] Ends ==="
}

private String captureSettingsSnapshot() {
    Map snapshot = [:]
    
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn") || k.endsWith("Button")) }
        .sort()

    sortedKeys.each { k ->
        def val = settings[k]
        if (val instanceof List) {
            snapshot[k] = val.collect { it.toString() }.sort()
        } else {
            snapshot[k] = val?.toString()
        }
    }
    
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    state.currentSettingsJson = jsonString
    
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

private void resetHsmStateVariables() {
    state.lastHsmState = null
    state.pendingHsmTarget = null
    state.pendingHsmDispatchedTime = null
    state.pendingHsmDispatchedMs = null
    state.pendingHsmExpires = 0L
}

private void checkHsmSettingChanges() {
    Boolean currentManageHSM = getSettingBool("manageHSM", true)
    Boolean lastManageHSM    = (state.lastManageHSM != null) ? state.lastManageHSM : currentManageHSM

    if (currentManageHSM != lastManageHSM) {
        logWarn "HSM Integration changed: ${lastManageHSM ? 'ENABLED' : 'DISABLED'} -> ${currentManageHSM ? 'ENABLED' : 'DISABLED'}"
        if (!currentManageHSM) {
            resetHsmStateVariables()
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
    long currentMs = new Date().getTime()
    
    lastTriggerTimes.entrySet().removeIf { entry -> (currentMs - entry.value) > 10000L }

    Long lastTime = lastTriggerTimes.get(sourceKey)
    if (lastTime != null && (currentMs - lastTime < windowMs)) {
        logTrace "Trigger source '${sourceKey}' debounced (${currentMs - lastTime}ms < ${windowMs}ms window)."
        return true
    }
    
    lastTriggerTimes.put(sourceKey, currentMs)
    return false
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

private long parseLongSafely(def val, long defaultVal = 0L) {
    if (val == null) return defaultVal
    try {
        String s = val.toString().replaceAll("[^0-9]", "")
        return s ? s.toLong() : defaultVal
    } catch (Exception e) {
        return defaultVal
    }
}

def initialize() {
    logTrace "=== Mode Manager Adv [initialize] Begins ==="
    logDebug "Initializing Mode Manager Advanced v${version()}..."
    
    state.activeTransaction = null
    if (!state.controlSource) {
        state.controlSource = "Scheduled"
    }

    Map currentPeriod = getActiveTimePeriodInfo()
    if (currentPeriod?.key) {
        state.activePeriodKey = currentPeriod.key
    }

    if (getSettingBool("manageHSM", true)) {
        subscribe(location, "hsmStatus", hsmStatusHandler)
        if (state.lastHsmState == null) {
            state.lastHsmState = location.hsmStatus ?: "disarmed"
        }
    }

    subscribe(location, "systemStart", hubStartupHandler)

    if (masterPresence) subscribe(masterPresence, "presence", presenceHandler)
    if (homeSwitch) subscribe(homeSwitch, "switch", homeSwitchHandler)
    if (awaySwitch) subscribe(awaySwitch, "switch", awaySwitchHandler)
    if (awakeSwitch) subscribe(awakeSwitch, "switch", awakeSwitchHandler)

    if (alexaAwakeSwitch) subscribe(alexaAwakeSwitch, "switch", alexaAwakeSwitchHandler)
    if (alexaPresenceSwitch) subscribe(alexaPresenceSwitch, "switch", alexaPresenceSwitchHandler)
    if (alexaVoiceMarkerSwitch) subscribe(alexaVoiceMarkerSwitch, "switch", alexaVoiceMarkerSwitchHandler)
    
    (updateTriggerSwitch ? [updateTriggerSwitch].flatten() : []).each { dev -> subscribe(dev, "switch.on", updateSwitchHandler) }
    (updateTriggerButton ? [updateTriggerButton].flatten() : []).each { dev -> subscribe(dev, "pushed", updateButtonHandler) }

    [vSwitchWeeHours, vSwitchEarlyMorning, vSwitchMorning, vSwitchDay, vSwitchEvening, vSwitchLateEvening].each { vSwitch ->
        if (vSwitch) subscribe(vSwitch, "switch.on", vSwitchHandler)
    }

    if (state.controlSource == "Override" && getSettingBool("suspendScheduler", false)) {
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

    purgePendingAckTokens()
    resetHsmStateVariables()
    lastTriggerTimes.clear()

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
    processStatePipeline([controlSource: "Reboot", isStartupSync: true, source: "Hub System Startup (Post-Boot Delay)"])
}

/* =========================================================================================
   DIAGNOSTIC ENHANCEMENTS & HSM ENGINE
   ========================================================================================= */

def hsmStatusHandler(evt) {
    String newHsmStatus = evt.value
    logTrace "=== Mode Manager Adv [Handler: HSM Status Update ('${newHsmStatus}')] Begins ==="
    logTrace "HSM Event Received -> Value: '${newHsmStatus}' | Source: '${evt.name}'"
    
    String previousStatus = state.lastHsmState
    state.lastHsmState = newHsmStatus
    
    if (state.pendingHsmTarget == newHsmStatus) {
        long transitTime = state.pendingHsmDispatchedMs ? (now() - parseLongSafely(state.pendingHsmDispatchedMs, now())) : 0L
        logTrace "HSM status successfully updated to '${newHsmStatus}' (Confirmed in ${transitTime}ms)."
        logTrace "HSM Target Reached: Clearing pending state variables. (Was targeting '${state.pendingHsmTarget}')"
        state.pendingHsmTarget = null
        state.pendingHsmDispatchedTime = null
        state.pendingHsmDispatchedMs = null
        state.pendingHsmExpires = 0L
    } else if (previousStatus != newHsmStatus) {
        logTrace "HSM status updated externally to '${newHsmStatus}' (was '${previousStatus}')."
        if (state.pendingHsmTarget != null) {
            logWarn "HSM Mismatch! External event brought HSM to '${newHsmStatus}', but Mode Manager is currently waiting for '${state.pendingHsmTarget}'."
        }
    } else {
        logTrace "HSM status event received with identical state '${newHsmStatus}'. No transition required."
    }
    
    updateStatusTileDevice([
        targetMode: location.mode,
        controlSource: state.controlSource ?: "Scheduled",
        previousModeAtStart: state.previousMode ?: location.mode,
        previousControlSourceAtStart: state.previousControlSource ?: state.controlSource,
        periodKey: state.activePeriodKey ?: "none",
        isSleeping: (awakeSwitch ? (awakeSwitch.currentValue("switch") == "off") : false),
        txId: "tx_external_hsm",
        source: "HSM Event Handler"
    ])
    logTrace "=== Mode Manager Adv [Handler: HSM Status Update] Ends ==="
}

private void syncHsmState(String expectedHsmStatus, String hsmCmd, String controlSource, int retryCount = 0) {
    if (!getSettingBool("manageHSM", true)) {
        logTrace "syncHsmState: HSM management is disabled in app settings. Skipping."
        return
    }

    String currentHsm = location.hsmStatus ?: "unknown"
    long currentMs = now()
    
    logTrace "syncHsmState Evaluation -> Current Location HSM: '${currentHsm}' | Expected Target: '${expectedHsmStatus}' | Command: '${hsmCmd}' | Control Source: '${controlSource}' | RetryCount: ${retryCount}"

    if (currentHsm == expectedHsmStatus) {
        if (state.pendingHsmTarget != null) {
            logTrace "HSM synchronized: Currently '${currentHsm}'."
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

    long expiresMs = parseLongSafely(state.pendingHsmExpires, 0L)
    Boolean isMatchingPending = (state.pendingHsmTarget == expectedHsmStatus)
    Boolean isWindowActive = (currentMs <= expiresMs)

    logTrace "HSM In-Flight Guard Inspection -> PendingTarget: '${state.pendingHsmTarget}' | MatchingPending: ${isMatchingPending} | ExpiresMs: ${expiresMs} | CurrentMs: ${currentMs} | WindowActive: ${isWindowActive}"

    if (isMatchingPending && isWindowActive) {
        long remainingMs = expiresMs - currentMs
        logTrace "HSM command '${hsmCmd}' already in-flight. Skipping redundant dispatch because target '${expectedHsmStatus}' was commanded at ${state.pendingHsmDispatchedTime ?: 'recent tx'} (${remainingMs}ms remaining in confirmation window)."
        return
    }

    if (state.pendingHsmTarget != null && !isWindowActive) {
        logWarn "HSM desynchronization timeout! HSM failed to reach '${state.pendingHsmTarget}' within 5s window. Current: '${currentHsm}'. Re-issuing command: '${hsmCmd}'"
    } else {
        logTrace "HSM state transition required. Current: '${currentHsm}' | Target: '${expectedHsmStatus}' → Dispatching location event: ${hsmCmd}"
    }

    String formattedTime = new Date().format("hh:mm:ss.SSS a", location.timeZone ?: TimeZone.getDefault())
    state.pendingHsmTarget = expectedHsmStatus
    state.pendingHsmDispatchedTime = formattedTime
    state.pendingHsmDispatchedMs = currentMs
    state.pendingHsmExpires = currentMs + 5000L
    
    logTrace "DISPATCHING LOCATION EVENT -> sendLocationEvent(name: 'hsmSetArm', value: '${hsmCmd}') [Timestamp: ${formattedTime}]"
    sendLocationEvent(name: "hsmSetArm", value: hsmCmd)
    
    logTrace "Scheduling verification check 'verifyHsmSync' in 6 seconds to confirm receipt..."
    runIn(6, "verifyHsmSync", [data: [expectedHsm: expectedHsmStatus, commandedHsm: hsmCmd, dispatchMs: currentMs, retryCount: retryCount]])
}

def verifyHsmSync(Map data) {
    String expected = data?.expectedHsm
    String hsmCmd = data?.commandedHsm
    long initialDispatchMs = parseLongSafely(data?.dispatchMs, 0L)
    int retryCount = parseIntSafely(data?.retryCount, 0)
    int maxRetries = 3
    
    if (state.pendingHsmTarget != null && state.pendingHsmTarget != expected) {
        logTrace "verifyHsmSync: Superseded timer ignored. Pending target moved to '${state.pendingHsmTarget}' (was '${expected}')."
        return
    }

    String actualHsm = location.hsmStatus ?: "unknown"
    long elapsedMs = initialDispatchMs > 0L ? (now() - initialDispatchMs) : 0L
    logTrace "=== verifyHsmSync Execution -> Expected: '${expected}' | Actual: '${actualHsm}' | DispatchMs: ${initialDispatchMs} (Dispatched ${elapsedMs}ms ago) | Retry: ${retryCount}/${maxRetries} ==="

    if (actualHsm != expected) {
        if (retryCount >= maxRetries) {
            logWarn "🚨 HSM VERIFICATION FAILED CRITICALLY! Location HSM remained '${actualHsm}' after ${maxRetries} retry attempts to set '${expected}' (${hsmCmd}). Halting retries to prevent loop."
            state.pendingHsmTarget = null
            state.pendingHsmExpires = 0L
            if (notificationDevice) {
                String errorAlert = "⚠️ Mode Manager Alert: Failed to synchronize HSM to '${expected}' after ${maxRetries} attempts (Current state: ${actualHsm}). Check for faulted security sensors."
                notificationDevice.each { dev -> dev.deviceNotification(errorAlert) }
            }
            return
        }
        
        logWarn "HSM VERIFICATION FAILED! Location HSM status is '${actualHsm}' 6 seconds after sending '${hsmCmd}'. Target was '${expected}'. Retrying command dispatch (Attempt ${retryCount + 1}/${maxRetries})...."
        state.pendingHsmExpires = 0L 
        syncHsmState(expected, hsmCmd, "Verification Retry Guard", retryCount + 1)
    } else {
        logTrace "HSM Verification Guard Passed: Location HSM is confirmed at '${actualHsm}'."
    }
}

def restartPeriodSchedules() {
    stopPeriodSchedules()
    startPeriodSchedules()
}

def startPeriodSchedules() {
    List<Map> periodCheck = [
        [name: "Wee Hours",     mins: getMinutesFromSetting(settings.timeWeeHours, 30)],
        [name: "Early Morning", mins: getMinutesFromSetting(settings.timeEarlyMorning, 285)],
        [name: "Morning",       mins: getMinutesFromSetting(settings.timeMorning, 450)],
        [name: "Day",           mins: getMinutesFromSetting(settings.timeDay, 600)],
        [name: "Evening",       mins: getMinutesFromSetting(settings.timeEvening, 1020)],
        [name: "Late Evening",  mins: getMinutesFromSetting(settings.timeLateEvening, 1290)]
    ]

    Map<Integer, List<String>> minuteMap = [:]
    periodCheck.each { p ->
        if (!minuteMap.containsKey(p.mins)) minuteMap[p.mins] = []
        minuteMap[p.mins].add(p.name)
    }

    minuteMap.each { mins, names ->
        if (names.size() > 1) {
            int hrs = (mins / 60) as int
            int m = mins % 60
            String timeFormatted = String.format("%02d:%02d", hrs, m)
            logWarn "⚠️ CONFIGURATION WARNING: ${names.join(' and ')} share the exact same start time (${timeFormatted}). Period boundary evaluation order may be ambiguous."
        }
    }

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
        return d.format("s m H * * ? *", tz)
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
    if (state.controlSource == "Override" && getSettingBool("suspendScheduler", false)) return
    processStatePipeline([controlSource: "Scheduled", source: "Schedule CRON (${periodKey})", isBoundaryTrigger: true])
}

/* =========================================================================================
   CORE DECISION PIPELINE & PHASE SYNCHRONIZATION
   ========================================================================================= */

private int getControlSourceRank(String controlSource) {
    switch (controlSource) {
        case "Override":  return 4
        case "Presence":  return 3
        case "Voice":     return 2
        case "Reboot":    return 1
        case "Scheduled": return 1
        default:          return 1
    }
}

private void recheckState(String triggerSource, Boolean isButtonTrigger = false, Boolean isSaveEvent = false) {
    logInfo "State recheck requested by trigger source: '${triggerSource}'"
    processStatePipeline([controlSource: "Scheduled", source: triggerSource, isRecheck: true, isButtonTrigger: isButtonTrigger, isSaveEvent: isSaveEvent])
}

private void processStatePipeline(Map request) {
    String reqControlSource = request.controlSource ?: "Scheduled"
    String reqSource = request.source ?: "Internal Pipeline"
    Boolean isRecheck = request.isRecheck ?: false
    
    String txId = getNextTxId()
    state.activeTransaction = txId

    logTrace "=== Mode Manager Adv [Pipeline: ${reqControlSource} | ${reqSource} | ${txId}] Begins ==="

    try {
        String previousMode = location.mode
        String previousControlSource = state.controlSource ?: "Scheduled"
        String previousPeriod = state.activePeriodKey ?: "unknown"
        
        Map decision = calculateDecision(request)
        decision.txId = txId
        decision.isRecheck = isRecheck
        decision.isButtonTrigger = (request.isButtonTrigger == true)
        decision.isSaveEvent = (request.isSaveEvent == true)
        decision.isVoiceAttributed = (request.isVoiceAttributed == true)
        decision.previousModeAtStart = previousMode
        decision.previousControlSourceAtStart = previousControlSource
        decision.previousPeriodAtStart = previousPeriod
        
        Boolean decisionApplied = applyDecision(decision)
        if (!decisionApplied) {
            logError "Transaction #${txId} ABORTED: Failed to apply state decision (${reqSource}). Skipping output synchronization."
            return
        }

        Boolean forceSync = (decision.isButtonTrigger || decision.isStartupSync || decision.isSaveEvent)
        if (decision.stateChanged || forceSync) {
            syncOutputs(decision)
        } else {
            logTrace "Skipping syncOutputs execution: System state unchanged (${decision.targetMode} | ${decision.controlSource}) on passive recheck/CRON."
        }

        logTrace "COMPLETE TRANSACTION #${txId} -> Mode: '${location.mode}' | Control Source: '${state.controlSource}'"
        logTrace "=== Mode Manager Adv [Pipeline: ${reqControlSource} | ${reqSource} | ${txId}] Ends ==="
    } finally {
        if (state.activeTransaction == txId) {
            state.activeTransaction = null
        }
    }
}

private Map calculateDecision(Map request) {
    String requestedControlSource = request.controlSource ?: "Scheduled"
    String source = request.source ?: "Unknown"
    Boolean isBoundaryTrigger = request.isBoundaryTrigger ?: false
    Boolean forceReleaseLock = request.forceReleaseLock ?: false
    Boolean isStartupSync = (request.isStartupSync == true || requestedControlSource == "Reboot")
    
    String currentActiveSource = state.controlSource ?: "Scheduled"
    int incomingRank = getControlSourceRank(requestedControlSource)
    int currentRank = getControlSourceRank(currentActiveSource)

    Boolean isHoldOverrideEnabled = getSettingBool("holdOverride", false)

    Boolean isHome
    if (request.presenceValue != null) {
        isHome = (request.presenceValue == "present")
    } else if (request.simulatedHome != null) {
        isHome = request.simulatedHome
    } else {
        isHome = (homeSwitch ? (homeSwitch.currentValue("switch") == "on") : true)
    }

    Boolean requestedSleeping = (request.periodKey == "sleeping" || request.targetMode == (sleepMode?.toString() ?: "Sleeping"))
    Boolean physicalSleeping = (awakeSwitch ? (awakeSwitch.currentValue("switch") == "off") : false)
    
    Boolean isSleeping
    if (request.simulatedAwake != null) {
        isSleeping = !request.simulatedAwake
    } else if (request.isAwakeRequested != null) {
        isSleeping = !request.isAwakeRequested
    } else {
        isSleeping = requestedSleeping || physicalSleeping
    }

    if (isHome && isSleeping && currentActiveSource == "Override" && !isHoldOverrideEnabled && requestedControlSource != "Override") {
        logInfo "Behavioral Sleep state triggered while Home. Pre-empting active unlocked Override."
        currentActiveSource = "Scheduled"
        currentRank = getControlSourceRank("Scheduled")
    }

    if (forceReleaseLock) {
        logInfo "Force-release requested by '${source}'. Resetting active '${currentActiveSource}' state to '${requestedControlSource}'."
    } else if (currentActiveSource == "Override") {
        if (isHoldOverrideEnabled) {
            if (incomingRank < currentRank && !isBoundaryTrigger) {
                logInfo "Incoming request [Control Source: ${requestedControlSource}] blocked by active 'holdOverride' Mode Lock."
                requestedControlSource = "Override"
            }
        } else {
            logInfo "Active 'Override' state released to incoming request [Control Source: ${requestedControlSource}] because 'holdOverride' is OFF."
        }
    } else if (incomingRank < currentRank) {
        if (isBoundaryTrigger && !isSleeping && isHome) {
            requestedControlSource = "Scheduled"
        } else {
            requestedControlSource = currentActiveSource
        }
    }

    String targetMode = null
    String activePeriodKey = null

    if (requestedControlSource == "Override") {
        targetMode = request.targetMode ?: location.mode
        activePeriodKey = request.periodKey
    } else if (requestedControlSource == "Voice") {
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
    } else if (requestedControlSource == "Presence") {
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
            if (requestedControlSource != "Reboot") requestedControlSource = "Presence"
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
        controlSource: requestedControlSource,
        source: source,
        targetMode: targetMode,
        periodKey: activePeriodKey,
        isSleeping: isSleeping,
        isStartupSync: isStartupSync
    ]
}

private Boolean applyDecision(Map decision) {
    String newMode = decision.targetMode
    String newControlSource = (decision.isVoiceAttributed == true) ? "Voice" : decision.controlSource
    String newPeriod = decision.periodKey
    
    decision.controlSource = newControlSource

    if (!newMode) {
        logWarn "Decision produced null target mode. Aborting mode apply."
        return false
    }

    String previousControlSource = decision.previousControlSourceAtStart ?: state.controlSource ?: "Scheduled"
    String previousMode = decision.previousModeAtStart ?: location.mode
    String previousPeriod = decision.previousPeriodAtStart ?: state.activePeriodKey
    Boolean suspendOnOverride = getSettingBool("suspendScheduler", false)

    if (suspendOnOverride) {
        if (newControlSource == "Override" && previousControlSource != "Override") {
            logInfo "Entering Override state with suspendScheduler enabled. Pausing CRON schedules."
            stopPeriodSchedules()
        } else if (previousControlSource == "Override" && newControlSource != "Override") {
            logInfo "Exiting Override state. Resuming CRON schedules."
            restartPeriodSchedules()
        }
    }

    Boolean modeChanged = (previousMode != newMode)
    Boolean controlSourceChanged = (previousControlSource != newControlSource)
    Boolean periodChanged = (previousPeriod != newPeriod && newPeriod != null)

    decision.modeChanged = modeChanged
    decision.controlSourceChanged = controlSourceChanged
    decision.periodChanged = periodChanged
    decision.stateChanged = (modeChanged || controlSourceChanged || periodChanged)

    state.controlSource = newControlSource
    state.activePeriodKey = newPeriod

    if (modeChanged && controlSourceChanged) {
        logInfo "Location Mode changed from '${previousMode}' to '${newMode}' AND Control Source changed from '${previousControlSource}' to '${newControlSource}' | (${decision.source})"
        setLocationMode(newMode)
    } else if (modeChanged) {
        logInfo "Changing Hubitat Location Mode from '${previousMode}' to '${newMode}' | Control Source remains '${newControlSource}' (${decision.source})"
        setLocationMode(newMode)
    } else if (controlSourceChanged) {
        logInfo "Location Mode remains '${newMode}', but Control Source changed from '${previousControlSource}' to '${newControlSource}' | (${decision.source})"
    } else if (periodChanged) {
        logInfo "Location Mode '${newMode}' unchanged, but Schedule Period transitioned from '${previousPeriod}' to '${newPeriod}' | (${decision.source})"
    } else if (decision.isButtonTrigger == true) {
        logInfo "Resynchronize button pressed: Mode remains '${newMode}' and Control Source remains '${newControlSource}'. No state change required."
    } else {
        logTrace "State evaluation completed: Location Mode remains '${newMode}' and Control Source remains '${newControlSource}' | (${decision.source})"
    }

    updatePresentation([targetMode: newMode, controlSource: newControlSource])
    return true
}

private void syncOutputs(Map decision) {
    String targetMode = decision.targetMode
    String periodKey = decision.periodKey
    String controlSource = decision.controlSource
    Boolean isSleeping = (decision.isSleeping == true)
    Boolean isStartupSync = (decision.isStartupSync == true)

    logTrace "=== syncOutputs Execution Sequence Initiated ==="
    logTrace "Parameters -> TargetMode: '${targetMode}' | PeriodKey: '${periodKey}' | Control Source: '${controlSource}' | IsSleeping: ${isSleeping} | IsStartupSync: ${isStartupSync}"

    // STEP 1: PHYSICAL & VIRTUAL SWITCH SYNCHRONIZATION
    logTrace "syncOutputs [Step 1/3]: Synchronizing Presence, Sleep, and Alexa switches..."
    if (targetMode == "Away") {
        syncSwitch(homeSwitch, "off")
        syncSwitch(awaySwitch, "on")
        syncSwitch(alexaPresenceSwitch, "off")
    } else {
        syncSwitch(homeSwitch, "on")
        syncSwitch(awaySwitch, "off")
        syncSwitch(alexaPresenceSwitch, "on")
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
    updateStatusTileDevice(decision)

    Boolean modeChanged = (decision.modeChanged == true)
    Boolean controlSourceChanged = (decision.controlSourceChanged == true)

    if (isStartupSync) {
        dispatchTileNotification(decision, "Reboot", isSleeping)
    } else if (modeChanged || controlSourceChanged) {
        dispatchTileNotification(decision, controlSource, isSleeping)
    } else if (decision.isRecheck == true) {
        dispatchTileNotification(decision, "RecheckNoChange", isSleeping)
    } else {
        dispatchTileNotification(decision, controlSource, isSleeping)
    }

    // STEP 3: LOCATION HSM EVENT DISPATCH
    logTrace "syncOutputs [Step 3/3]: Evaluating HSM state synchronization..."
    if (targetMode == "Away") {
        syncHsmState("armedAway", "armAway", controlSource)
    } else {
        Boolean isSleepingOrNight = (isSleeping || periodKey == "weeHours")
        String expectedHsmStatus = isSleepingOrNight ? "armedNight" : "disarmed"
        String hsmCmd = isSleepingOrNight ? "armNight" : "disarm"

        logTrace "HSM Target Calculation -> Mode: '${targetMode}' | Period: '${periodKey}' | IsSleeping: ${isSleeping} | Calculated Target: '${expectedHsmStatus}' (${hsmCmd})"
        syncHsmState(expectedHsmStatus, hsmCmd, controlSource)
    }

    logTrace "=== syncOutputs Execution Sequence Completed ==="
}

private void updateStatusTileDevice(Map decision) {
    if (!statusTileDevice) {
        logTrace "updateStatusTileDevice: No status tile device configured. Skipping tile command."
        return
    }

    String modeVal = decision.targetMode ?: location.mode
    String controlSourceVal = decision.controlSource ?: state.controlSource
    String timeStr = new Date().format("hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())
    
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

    Map extendedMetadata = [
        prevMode: decision.previousModeAtStart ?: "Unknown",
        prevControlSource: decision.previousControlSourceAtStart ?: "Unknown",
        periodKey: decision.periodKey ?: "none",
        isSleeping: (decision.isSleeping == true).toString(),
        txId: decision.txId ?: "tx_unknown",
        source: decision.source ?: "Internal Pipeline"
    ]

    logTrace "updateStatusTileDevice -> Updating '${statusTileDevice.displayName}' | Mode: '${modeVal}' | Control Source: '${controlSourceVal}' | HSM: '${formattedHsm}' | Time: '${timeStr}'"

    try {
        if (statusTileDevice.hasCommand("setStatus")) {
            logTrace "Calling setStatus() on tile device with extended metadata payload..."
            try {
                statusTileDevice.setStatus(modeVal, controlSourceVal, timeStr, formattedHsm, extendedMetadata)
            } catch (MissingMethodException e1) {
                try {
                    statusTileDevice.setStatus(modeVal, controlSourceVal, timeStr, formattedHsm)
                } catch (MissingMethodException e2) {
                    String compositeTimeHsm = "${timeStr} | HSM: ${formattedHsm}"
                    statusTileDevice.setStatus(modeVal, controlSourceVal, compositeTimeHsm)
                }
            }
        } else {
            logTrace "Tile device missing setStatus(). Falling back to discrete attribute setters..."
            if (statusTileDevice.hasCommand("setActiveMode")) statusTileDevice.setActiveMode(modeVal)
            if (statusTileDevice.hasCommand("setControlSource")) statusTileDevice.setControlSource(controlSourceVal)
            if (statusTileDevice.hasCommand("setActiveReason")) statusTileDevice.setActiveReason(controlSourceVal)
            if (statusTileDevice.hasCommand("setLastTransitionTime")) statusTileDevice.setLastTransitionTime("${timeStr} (HSM: ${formattedHsm})")
            if (statusTileDevice.hasCommand("setHsmStatus")) statusTileDevice.setHsmStatus(formattedHsm)
        }
        logDebug "Native Status Tile Device '${statusTileDevice.displayName}' update command completed successfully."
    } catch (Exception e) {
        logWarn "Failed to update Status Tile Device '${statusTileDevice?.displayName}': ${e.message}"
    }
}

/* =========================================================================================
   DETERMINISTIC SINGLE-ACK TOKEN REGISTER ENGINE (BIDIRECTIONAL & AUTHORITATIVE DEVICES)
   ========================================================================================= */

private void registerExpectedAck(String deviceId, String expectedValue) {
    if (!deviceId) return
    long expirationMs = now() + 2000L
    pendingAckTokens.put(deviceId.toString(), "${expectedValue}:${expirationMs}")
}

private boolean consumeExpectedAck(def evt) {
    if (evt == null) return false
    def rawDevId = evt.deviceId ?: evt.device?.id
    if (!rawDevId) return false
    
    String devId = rawDevId.toString()
    String actualVal = evt.value?.toString()
    String tokenData = pendingAckTokens.get(devId)

    if (tokenData != null) {
        int delimIdx = tokenData.indexOf(":")
        String expectedVal = delimIdx > 0 ? tokenData.substring(0, delimIdx) : tokenData
        long expiresMs = delimIdx > 0 ? parseLongSafely(tokenData.substring(delimIdx + 1), 0L) : 0L
        long currentMs = now()

        if (currentMs <= expiresMs) {
            if (expectedVal == actualVal) {
                pendingAckTokens.remove(devId)
                logTrace "SINGLE-ACK CONSUMED: Confirmed internal output sync for '${evt.device?.displayName}' [${devId}:${actualVal}]"
                return true
            }
        } else {
            pendingAckTokens.remove(devId)
            logDebug "HOUSEKEEPING EVICTION: Retired expired acknowledgment token for '${evt.device?.displayName}' [${devId}]"
        }
    }
    return false
}

private void purgePendingAckTokens() {
    if (pendingAckTokens != null) {
        pendingAckTokens.clear()
        logTrace "SINGLE-ACK ENGINE: Purged in-memory pending acknowledgment register following lifecycle reset."
    }
}

private void syncSwitch(def device, String targetState) {
    if (device != null && device.hasCommand(targetState)) {
        if (device.currentValue("switch") != targetState) {
            String devId = device.id.toString()
            registerExpectedAck(devId, targetState)

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
    switch (modeVal) {
        case "Morning":  return "#F1C40F"
        case "Day":      return "#00FF66"
        case "Evening":  return "#E67E22"
        case "Night":    return "#9B59B6"
        case "Sleeping": return "#00FFFF"
        case "Away":     return "#E74C3C"
        default:         return "#1ABC9C"
    }
}

private String getControlSourceColor(String controlSourceVal) {
    switch (controlSourceVal) {
        case "Scheduled": return "#00FFFF"
        case "Voice":     return "#FF8C00"
        case "Presence":  return "#00FF66"
        case "Override":  return "#FF007F"
        case "Reboot":    return "#BB86FC"
        default:          return "#BDC3C7"
    }
}

private void dispatchTileNotification(Map decision, String overrideSourceVal = null, Boolean isSleepingState = false) {
    if (!notificationDevice && !notificationDeviceMobile && !speechDevice) return

    Boolean isSaveEvent = (decision.isSaveEvent == true)

    if (!getSettingBool("enableNotifications", true)) return

    String modeVal = decision.targetMode
    String controlSourceVal = overrideSourceVal ?: decision.controlSource
    Boolean modeChanged = (decision.modeChanged == true)
    Boolean controlSourceChanged = (decision.controlSourceChanged == true)
    Boolean isButtonTrigger = (decision.isButtonTrigger == true)

    if (controlSourceVal != "RecheckNoChange" && controlSourceVal != "Reboot" && state.lastNotifiedMode == modeVal && !controlSourceChanged && !isButtonTrigger && !isSaveEvent) {
        logTrace "Notification suppressed: Mode '${modeVal}' and Control Source '${controlSourceVal}' completely unchanged."
        return
    }

    Boolean masterPushAllowed = getSettingBool("masterEnablePush", true)
    Boolean masterPushMobileAllowed = getSettingBool("masterEnablePushMobile", true)
    Boolean masterAudioAllowed = getSettingBool("masterEnableAudio", true)

    Boolean triggerPushEnabled = false
    Boolean triggerPushMobileEnabled = false
    Boolean triggerAudioEnabled = false

    if (controlSourceVal != "Reboot" && !modeChanged && controlSourceChanged) {
        triggerPushEnabled       = getSettingBool("pushSourceOnly", false)
        triggerPushMobileEnabled = getSettingBool("pushMobileSourceOnly", false)
        triggerAudioEnabled      = getSettingBool("audioSourceOnly", false)
    } else if (controlSourceVal != "Reboot" && modeChanged && !controlSourceChanged) {
        triggerPushEnabled       = getSettingBool("pushModeOnly", false)
        triggerPushMobileEnabled = getSettingBool("pushMobileModeOnly", false)
        triggerAudioEnabled      = getSettingBool("audioModeOnly", false)
    } else {
        switch (controlSourceVal) {
            case "Scheduled":
                triggerPushEnabled       = getSettingBool("pushScheduled", false)
                triggerPushMobileEnabled = getSettingBool("pushMobileScheduled", false)
                triggerAudioEnabled      = getSettingBool("audioScheduled", false)
                break
            case "Voice":
                triggerPushEnabled       = getSettingBool("pushVoice", false)
                triggerPushMobileEnabled = getSettingBool("pushMobileVoice", false)
                triggerAudioEnabled      = getSettingBool("audioVoice", false)
                break
            case "Presence":
                triggerPushEnabled       = getSettingBool("pushPresence", false)
                triggerPushMobileEnabled = getSettingBool("pushMobilePresence", false)
                triggerAudioEnabled      = getSettingBool("audioPresence", false)
                break
            case "Override":
                triggerPushEnabled       = getSettingBool("pushOverride", false)
                triggerPushMobileEnabled = getSettingBool("pushMobileOverride", false)
                triggerAudioEnabled      = getSettingBool("audioOverride", false)
                break
            case "Reboot":
                triggerPushEnabled       = getSettingBool("pushReboot", false)
                triggerPushMobileEnabled = getSettingBool("pushMobileReboot", false)
                triggerAudioEnabled      = getSettingBool("audioReboot", false)
                break
            case "RecheckNoChange":
                triggerPushEnabled       = getSettingBool("pushRecheckNoChange", false)
                triggerPushMobileEnabled = getSettingBool("pushMobileRecheckNoChange", false)
                triggerAudioEnabled      = getSettingBool("audioRecheckNoChange", false)
                break
            default:
                triggerPushEnabled       = false
                triggerPushMobileEnabled = false
                triggerAudioEnabled      = false
                break
        }
    }

    String template = null

    if (controlSourceVal != "Reboot" && !modeChanged && controlSourceChanged) {
        template = settings.templateSourceOnly ?: "Control Source updated to %controlSource% (Mode remains %mode% at %time%)"
    } else if (controlSourceVal != "Reboot" && modeChanged && !controlSourceChanged) {
        template = settings.templateModeOnly ?: "Mode changed to %mode% (Control Source remains %controlSource% at %time%)"
    } else {
        switch (controlSourceVal) {
            case "Scheduled":   
                template = settings.templateScheduled ?: "Mode scheduled transition: %mode% (from %prevMode% at %time%)"
                break
            case "Voice":    
                template = settings.templateVoice ?: "Voice command changed mode to %mode% (Control Source: %controlSource% at %time%)"
                break
            case "Presence": 
                template = settings.templatePresence ?: "Presence update changed mode to %mode% (was %prevMode% at %time%)"
                break
            case "Override": 
                template = settings.templateOverride ?: "Manual Override activated: Mode set to %mode% (Control Source: %controlSource% at %time%)"
                break
            case "Reboot":
                template = settings.templateReboot ?: "System restarted following hub boot. Mode synchronized to %mode% (Control Source: %controlSource% at %time%)"
                break
            case "RecheckNoChange":
                template = settings.templateRecheckNoChange ?: "State recheck completed: Still in %mode% (%controlSource%) at %time%"
                break
            default:         
                template = "Location Mode changed to %mode% (Control Source: %controlSource% at %time%)"
                break
        }
    }

    Boolean allowStandardPush = masterPushAllowed && (triggerPushEnabled || isButtonTrigger || isSaveEvent)
    Boolean allowMobilePush = masterPushMobileAllowed && (triggerPushMobileEnabled || isButtonTrigger || isSaveEvent)
    Boolean allowAudio = masterAudioAllowed && (triggerAudioEnabled || isButtonTrigger || isSaveEvent)

    if (!allowStandardPush && !allowMobilePush && !allowAudio && !isButtonTrigger && !isSaveEvent) {
        logTrace "Notification suppressed: Push and Audio outputs disabled by trigger or master settings for '${controlSourceVal}'."
        return
    }

    String timeStr = new Date().format("hh:mm a", location.timeZone ?: TimeZone.getDefault())
    String rawMode = modeVal ?: "Unknown"
    String rawPrevMode = decision.previousModeAtStart ?: "Unknown"
    String rawSource = (controlSourceVal == "RecheckNoChange") ? decision.controlSource : controlSourceVal
    String rawPrevSource = decision.previousControlSourceAtStart ?: "Unknown"

    if (controlSourceVal != "RecheckNoChange") state.lastNotifiedMode = rawMode

    if (notificationDevice && allowStandardPush) {
        String pushFormat = settings.notificationFormat ?: "plain"
        Boolean useHtmlPush = (pushFormat == "html")

        String formattedMode = useHtmlPush ? "<span style='color:${getModeColor(rawMode)}; font-weight:bold;'>${rawMode}</span>" : rawMode
        String formattedPrevMode = useHtmlPush ? "<span style='color:${getModeColor(rawPrevMode)}; font-weight:bold;'>${rawPrevMode}</span>" : rawPrevMode
        String formattedSource = useHtmlPush ? "<span style='color:${getControlSourceColor(rawSource)}; font-weight:bold;'>${rawSource}</span>" : rawSource
        String formattedPrevSource = useHtmlPush ? "<span style='color:${getControlSourceColor(rawPrevSource)}; font-weight:bold;'>${rawPrevSource}</span>" : rawPrevSource

        String pushMsg = isSaveEvent ? "Mode Manager settings changed and saved." : template
            .replace("%mode%", formattedMode)
            .replace("%controlSource%", formattedSource)
            .replace("%reason%", formattedSource)
            .replace("%prevMode%", formattedPrevMode)
            .replace("%prevControlSource%", formattedPrevSource)
            .replace("%prevReason%", formattedPrevSource)
            .replace("%time%", timeStr)

        if (useHtmlPush) pushMsg = "<span>${pushMsg}</span>"

        notificationDevice.each { dev -> 
            logTrace "Sending standard push notification to '${dev.displayName}'"
            dev.deviceNotification(pushMsg) 
        }
    }

    if (notificationDeviceMobile && allowMobilePush) {
        String pushFormatMobile = settings.notificationFormatMobile ?: "plain"
        Boolean useHtmlPushMobile = (pushFormatMobile == "html")

        String formattedModeM = useHtmlPushMobile ? "<span style='color:${getModeColor(rawMode)}; font-weight:bold;'>${rawMode}</span>" : rawMode
        String formattedPrevModeM = useHtmlPushMobile ? "<span style='color:${getModeColor(rawPrevMode)}; font-weight:bold;'>${rawPrevMode}</span>" : rawPrevMode
        String formattedSourceM = useHtmlPushMobile ? "<span style='color:${getControlSourceColor(rawSource)}; font-weight:bold;'>${rawSource}</span>" : rawSource
        String formattedPrevSourceM = useHtmlPushMobile ? "<span style='color:${getControlSourceColor(rawPrevSource)}; font-weight:bold;'>${rawPrevSource}</span>" : rawPrevSource

        String pushMsgMobile = isSaveEvent ? "Mode Manager settings changed and saved." : template
            .replace("%mode%", formattedModeM)
            .replace("%controlSource%", formattedSourceM)
            .replace("%reason%", formattedSourceM)
            .replace("%prevMode%", formattedPrevModeM)
            .replace("%prevControlSource%", formattedPrevSourceM)
            .replace("%prevReason%", formattedPrevSourceM)
            .replace("%time%", timeStr)

        if (useHtmlPushMobile) pushMsgMobile = "<span>${pushMsgMobile}</span>"

        notificationDeviceMobile.each { dev -> 
            logTrace "Sending mobile push notification to '${dev.displayName}'"
            dev.deviceNotification(pushMsgMobile) 
        }
    }

    if (speechDevice && allowAudio) {
        Boolean isSleeping = (isSleepingState == true)
        Boolean suppressUnchangedButtonAudio = (isButtonTrigger && !modeChanged && !controlSourceChanged)
        
        if (isSleeping && getSettingBool("suppressAudioWhenSleeping", true)) {
            logTrace "Audio notification suppressed because system target state is Sleeping."
        } else if (suppressUnchangedButtonAudio) {
            logTrace "Audio speech notification suppressed: Resynchronize button pressed with no mode or control source change."
        } else {
            String plainSpeechMsg = isSaveEvent ? "Mode Manager settings changed and saved." : template
                .replace("%mode%", rawMode)
                .replace("%controlSource%", rawSource)
                .replace("%reason%", rawSource)
                .replace("%prevMode%", rawPrevMode)
                .replace("%prevControlSource%", rawPrevSource)
                .replace("%prevReason%", rawPrevSource)
                .replace("%time%", timeStr)
                .replaceAll("<[^>]*>", "")

            speechDevice.each { dev -> 
                logTrace "Sending audio speech notification to '${dev.displayName}'"
                dev.speak(plainSpeechMsg) 
            }
        }
    }
}

private void updatePresentation(Map decision) {
    updateAppLabel(decision.targetMode ?: location.mode, decision.controlSource ?: state.controlSource)
}

private void updateAppLabel(String currentMode = null, String controlSource = null) {
    Boolean showVersion       = getSettingBool("showVersionInLabel", true)
    Boolean showMode          = getSettingBool("showModeInLabel", true)
    Boolean showControlSource = getSettingBool("showControlSourceInLabel", true)

    String baseLabel = "Mode Manager Advanced"
    if (showVersion) baseLabel += " v${version()}"

    String displayMode = currentMode ?: location.mode ?: "Unknown"
    String currentSource = controlSource ?: state.controlSource ?: "Scheduled"

    List<String> badgeParts = []
    if (showMode) badgeParts.add("<span style='color:green; font-weight:bold;'>${displayMode}</span>")
    if (showControlSource) badgeParts.add("(${currentSource})")

    String formattedLabel = baseLabel
    if (!badgeParts.isEmpty()) formattedLabel += " - [" + badgeParts.join(" ") + "]"
    
    if (app.label != formattedLabel) {
        app.updateLabel(formattedLabel)
    }
}

private boolean consumeVoiceMarker() {
    long expires = parseLongSafely(atomicState.alexaVoiceMarkerExpires, 0L)
    long currentMs = now()
    
    if (expires > 0L && currentMs <= expires) {
        logTrace "Alexa Voice Source Marker verified successfully (${expires - currentMs}ms remaining in routine burst window)."
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
        recheckState("Manual UI Button ('Resynchronize System & Reset Active Mode')", true)
    } else if (btnName == "btnForceTestEvaluation") {
        executeForcedTestEvaluation()
    }
}

def updateSwitchHandler(evt) {
    String devName = evt.device?.displayName ?: "Trigger Switch"
    String devId = evt.deviceId ? "${evt.deviceId}" : (evt.device ? "${evt.device.id}" : "unknown")
    if (isTriggerDebounced("switch_${devId}", 2000L)) return

    recheckState("Trigger Switch '${devName}'")
}

def updateButtonHandler(evt) {
    String devName = evt.device?.displayName ?: "Trigger Button"
    String devId = evt.deviceId ? "${evt.deviceId}" : (evt.device ? "${evt.device.id}" : "unknown")
    if (isTriggerDebounced("button_${devId}_${evt.value}", 2000L)) return

    recheckState("Trigger Button '${devName}' (Button #${evt.value})")
}

def presenceHandler(evt) {
    logInfo "Master presence sensor '${evt.device?.displayName}' changed to '${evt.value}'"
    processStatePipeline([controlSource: "Presence", presenceValue: evt.value, source: "Master Presence Sensor"])
}

def homeSwitchHandler(evt) {
    if (consumeExpectedAck(evt)) return
    logInfo "Home switch '${evt.device?.displayName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "on") ? "present" : "not present"
    processStatePipeline([controlSource: "Presence", presenceValue: presenceVal, source: "Home Switch"])
}

def awaySwitchHandler(evt) {
    if (consumeExpectedAck(evt)) return
    logInfo "Away switch '${evt.device?.displayName}' changed to '${evt.value}'"
    String presenceVal = (evt.value == "off") ? "present" : "not present"
    processStatePipeline([controlSource: "Presence", presenceValue: presenceVal, source: "Away Switch"])
}

def alexaPresenceSwitchHandler(evt) {
    if (consumeExpectedAck(evt)) return
    Boolean isVoiceSource = consumeVoiceMarker()
    String evalSource = isVoiceSource ? "Override" : "Override"
    String srcLabel = isVoiceSource ? 
        "Alexa Presence Switch '${evt.device?.displayName}' (${evt.value} via Alexa Voice Routine)" : 
        "Alexa Presence Switch '${evt.device?.displayName}' (${evt.value} via Dashboard/Manual)"

    logTrace "Alexa Presence Switch changed to '${evt.value}' (Voice Marker Active: ${isVoiceSource} | Control Source: '${evalSource}')"
    
    String targetMode = (evt.value == "on") ? (getActiveTimePeriodInfo()?.mode ?: location.mode) : "Away"
    String targetKey = (evt.value == "on") ? getActiveTimePeriodInfo()?.key : null
    
    processStatePipeline([
        controlSource: evalSource,
        isVoiceAttributed: isVoiceSource,
        targetMode: targetMode,
        periodKey: targetKey,
        presenceValue: (evt.value == "on") ? "present" : "not present",
        source: srcLabel
    ])
}

def alexaVoiceMarkerSwitchHandler(evt) {
    if (evt.value == "on") {
        long currentMs = now()
        long expiresMs = currentMs + 2500L
        atomicState.alexaVoiceMarkerExpires = expiresMs
        logTrace "Alexa Voice Marker activated. Trigger window open for 2500ms."
        
        if (alexaVoiceMarkerSwitch && alexaVoiceMarkerSwitch.hasCommand("off")) {
            syncSwitch(alexaVoiceMarkerSwitch, "off")
        }
    }
}

def awakeSwitchHandler(evt) {
    if (consumeExpectedAck(evt)) return
    Boolean isVoiceSource = consumeVoiceMarker()
    Boolean isAwake = (evt.value == "on")
    String evalSource = isVoiceSource ? "Override" : "Override"
    String srcLabel = isVoiceSource ? 
        "Awake Switch '${evt.device?.displayName}' (${evt.value} via Alexa Voice Routine)" : 
        "Awake Switch '${evt.device?.displayName}' (${evt.value} Manual/External)"

    logInfo "Awake switch '${evt.device?.displayName}' changed to '${evt.value}' (Voice Marker Active: ${isVoiceSource} | Attributed Control Source: 'Voice')"
    
    Map activePeriod = getActiveTimePeriodInfo()
    String targetMode = (!isAwake) ? (sleepMode?.toString() ?: "Sleeping") : (activePeriod?.mode ?: location.mode)
    String targetKey = (!isAwake) ? "sleeping" : activePeriod?.key
    
    processStatePipeline([
        controlSource: evalSource,
        isVoiceAttributed: isVoiceSource,
        targetMode: targetMode,
        periodKey: targetKey,
        isAwakeRequested: isAwake,
        source: srcLabel
    ])
}

def alexaAwakeSwitchHandler(evt) {
    if (consumeExpectedAck(evt)) return
    if (isTriggerDebounced("alexaAwake_${evt.value}", 1500L)) return
    
    Boolean isVoiceSource = consumeVoiceMarker()
    Boolean isAwake = (evt.value == "on")
    String evalSource = isVoiceSource ? "Override" : "Override"
    String srcLabel = isVoiceSource ? 
        "Alexa Awake Switch '${evt.device?.displayName}' (${evt.value} via Alexa Voice Routine)" : 
        "Alexa Awake Switch '${evt.device?.displayName}' (${evt.value} via Dashboard/Manual)"

    logTrace "Alexa Awake Switch changed to '${evt.value}' (Voice Marker Active: ${isVoiceSource} | Attributed Control Source: 'Voice')"
    
    if (!isAwake) {
        processStatePipeline([
            controlSource: evalSource,
            isVoiceAttributed: isVoiceSource,
            targetMode: sleepMode?.toString() ?: "Sleeping",
            periodKey: "sleeping",
            isAwakeRequested: false,
            source: srcLabel
        ])
    } else {
        Map activePeriod = getActiveTimePeriodInfo()
        processStatePipeline([
            controlSource: evalSource,
            isVoiceAttributed: isVoiceSource,
            targetMode: activePeriod?.mode ?: location.mode,
            periodKey: activePeriod?.key,
            isAwakeRequested: true,
            source: srcLabel
        ])
    }
}

def sleepSwitchHandler(evt) {
    logTrace "sleepSwitchHandler: Event ignored. Passive output indicator."
}

def vSwitchHandler(evt) {
    if (consumeExpectedAck(evt)) return
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
            controlSource: "Override",
            targetMode: targetPeriod.mode,
            periodKey: targetPeriod.key,
            source: "Virtual Switch (${evt.device?.displayName})"
        ])
    }
}

def executeForcedTestEvaluation() {
    Boolean isHomeTarget = getSettingBool("testHome", true)
    Boolean isAwakeTarget = getSettingBool("testAwake", true)
    String forcedPeriodKey = settings.testPeriodKey ?: "day"
    String targetControlSource = settings.testControlSource ?: "Override"

    logInfo "Executing Forced Simulation -> Home: ${isHomeTarget} | Awake: ${isAwakeTarget} | Period: ${forcedPeriodKey} | Control Source: ${targetControlSource}"
    
    processStatePipeline([
        controlSource: targetControlSource,
        simulatedHome: isHomeTarget,
        simulatedAwake: isAwakeTarget,
        simulatedPeriodKey: forcedPeriodKey,
        source: "Section 9 Simulation Control Panel",
        forceReleaseLock: true
    ])
}

Map getActiveTimePeriodInfo(String overrideKey = null) {
    List<Map> periods = [
        [key: "weeHours",     mode: weeHoursMode?.toString(),     start: getMinutesFromSetting(settings.timeWeeHours, 30)],
        [key: "earlyMorning", mode: earlyMorningMode?.toString(), start: getMinutesFromSetting(settings.timeEarlyMorning, 285)],
        [key: "morning",      mode: morningMode?.toString(),      start: getMinutesFromSetting(settings.timeMorning, 450)],
        [key: "day",          mode: dayMode?.toString(),          start: getMinutesFromSetting(settings.timeDay, 600)],
        [key: "evening",      mode: eveningMode?.toString(),      start: getMinutesFromSetting(settings.timeEvening, 1020)],
        [key: "lateEvening",  mode: lateEveningMode?.toString(),  start: getMinutesFromSetting(settings.timeLateEvening, 1290)]
    ]

    if (overrideKey != null) {
        Map match = periods.find { it.key == overrideKey }
        if (match) return match
    }

    periods.sort { it.start }

    int currentMinutes = timeToMinutes(new Date())
    Map activePeriod = null

    for (int i = periods.size() - 1; i >= 0; i--) {
        if (currentMinutes >= periods[i].start) {
            activePeriod = periods[i]
            break
        }
    }

    return activePeriod ?: periods.last()
}

private int getMinutesFromSetting(def timeInput, int defaultMinutes) {
    if (timeInput == null) return defaultMinutes
    return timeToMinutes(timeInput, defaultMinutes)
}

private int timeToMinutes(def timeInput, int defaultVal = 0) {
    if (timeInput == null) return defaultVal
    try {
        if (timeInput instanceof Date) {
            String formatted = timeInput.format("HH:mm", location.timeZone ?: TimeZone.getDefault())
            List<String> parts = formatted.split(":")
            return (parts[0].toInteger() * 60) + parts[1].toInteger()
        }
        
        String strVal = timeInput.toString().trim()
        
        if (strVal.contains("T")) {
            Date d = toDateTime(strVal)
            String formatted = d.format("HH:mm", location.timeZone ?: TimeZone.getDefault())
            List<String> parts = formatted.split(":")
            return (parts[0].toInteger() * 60) + parts[1].toInteger()
        } else if (strVal.contains(":")) {
            String lowerStr = strVal.toLowerCase()
            Boolean isPm = lowerStr.contains("pm")
            Boolean isAm = lowerStr.contains("am")

            String cleanStr = strVal.replaceAll("(?i)[^0-9:]", "")
            List<String> parts = cleanStr.split(":")
            
            if (parts.size() >= 2) {
                int hrs = parts[0].toInteger()
                int mins = parts[1].toInteger()

                if (isPm && hrs < 12) hrs += 12
                if (isAm && hrs == 12) hrs = 0

                return (hrs * 60) + mins
            }
        }
    } catch (Exception e) {
        logWarn "timeToMinutes failed to parse '${timeInput}': ${e.message}"
    }
    return defaultVal
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