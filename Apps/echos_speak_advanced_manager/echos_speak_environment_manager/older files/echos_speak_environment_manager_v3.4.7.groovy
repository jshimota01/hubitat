/**
 * Application Name: Echos Speak Environment Manager
 * Platform: Hubitat Elevation (C-7 / OS 2.4.4.156)
 * Notes: Centralized environmental state matrix (Do Not Disturb, Mute, Volume, Display Brightness, Display Power), mode reconciliation, and command pacing for Echo Speaks devices.
 * Category: Utility / Convenience / Environmental
 **/
/**
 * Copyright 2026 James Shimota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **/
/**
 *  Purpose:
 *  Provides central 5-Section Shared Settings Matrix state management (DND, Mute, Volume Level, Display Brightness, Display Power) per Hubitat Mode + Sleeping/Away.
 *  Uses client-side HTML/JS matrix state editing inside Hubitat paragraph elements with safe section encapsulation.
 *
 *  Changelog:
 *  v3.4.7 - 2026/09/28 - Matrix Persistence Engine & Live Volume Refactor:
 *                          - Renamed 'Actual / Desired Vol' to 'Volume (Act/Des)' and fixed desired volume rendering in Live Device State matrix.
 *                          - Implemented native Hubitat payload inputs and pre-submit client-side JSON serialization to bridge browser-edited table state to Hubitat params map.
 *                          - Resolved client-side state loss on Save/Apply across all 5 matrices (DND, Mute, Volume, Brightness, Display Power).
 *                          - Added diagnostic logging for matrix state transfer and persistence operations.
 *  v3.4.6 - 2026/09/28 - Authoritative Capabilities Map Display Filtering Refactor:
 *                          - Removed hasCommand() fallbacks from getDeviceProfile() capability determination.
 *                          - Made capabilitiesMap (display, displayBrightness) strictly authoritative for display feature presence.
 *                          - Retained hasCommand() checks strictly within the command dispatch path for capable devices.
 *  v3.4.5 - 2026/09/28 - Expanded Logging & Matrix State Cleanup Refactor:
 *                          - Cleaned Section 4 & Section 5 titles by removing '(Screen Devices Only)'.
 *                          - Added explicit starting/completion logInfo events for Save, Apply Now, State Push, and Refresh operations.
 *                          - Refactored refreshManagedDeviceStates() to purge stale desiredState entries and resolve false 'State Unknown' status badges.
 *  v3.4.4 - 2026/09/28 - Capability Map Precision & State Normalization Refactor:
 *                          - Connected getDeviceProfile() strictly to device state capabilitiesMap (display, displayBrightness).
 *                          - Refactored normalizeDeviceState() to receive profile and eliminate blind fallback from displayBrightness to level.
 *                          - Added explanatory legend banner to Live Device State section distinguishing child device state vs target matrix state.
 *  v3.4.3 - 2026/09/28 - Live Matrix UI Refinement & Refresh Trigger:
 *                          - Sorted devices alphabetically by displayName in the Live Device State table.
 *                          - Renamed table columns to 'Brightness (Act/Des)' and 'Display Power (Act/Des)'.
 *                          - Replaced 'N/A / N/A' placeholder strings with '----' for unsupported device capabilities.
 *                          - Added 'Refresh Device States' button beneath the Live matrix to force device attribute polling and matrix state synchronization.
 *  v3.4.2 - 2026/09/28 - Desired-State & Sync Status Architecture Correction:
 *                          - Centralized device profile capabilities (hasDisplayBrightness, hasDisplayPower) and device state normalization.
 *                          - Excluded displayBrightness/displayPower desired-state entries for devices lacking those capabilities.
 *                          - Fixed setDisplayPower command verification to match setDisplayPowerOn capability checks.
 *                          - Fixed Elvis logic bug on brightness evaluations to preserve explicit brightness level 0.
 *                          - Reordered Live Device State matrix columns (DND, Mute, Vol, Brightness, Power, Active Mode, Sync Status) and simplified Active Mode column text.
 *  v3.4.1 - 2026/09/28 - GUI Cleanup & Architectural Refactor:
 *                          - Removed Section 1 Device Registry dropdown selection; all physical devices automatically included.
 *                          - Exposed Live Device State unconditionally at the top of the page (non-collapsible).
 *                          - Fixed Sync Status calculation bug causing perpetual "Pending" status badge.
 *                          - Merged Diagnostics & Control Panel into Section 8 Command Queue Settings.
 *                          - Renamed all "Policy Matrix" references to "Shared Settings Matrix".
 *                          - Added Version Demarcation banner logging on initialization and page rendering.
 *  v3.4.0 - 2026/09/27 - Section-Specific 'Apply Now' Integration:
 *                          - Added dedicated 'Apply Now' buttons alongside 'Save' in Sections 3 through 7.
 *                          - Implemented applySingleMatrixStateForCurrentMode() to look up the active mode/column state and immediately execute saved matrix settings strictly for that section.
 *                          - Formatted Save and Apply buttons side-by-side (width 6 each) for an intuitive dual-action UI.
 *  v3.3.0 - 2026/09/27 - Architectural Hardening & Matrix Isolation Refactor:
 *                          - P0: Isolated Save handlers so pressing 'Save [Section] Matrix' only persists fields belonging to that section.
 *                          - P0: Wired active column resolution for Location Modes, location sleeping events, and external parent setActiveColumn() triggers for Sleeping/Away.
 *                          - P1: Implemented reconciliation generation epoch to purge stale queued commands from prior modes.
 *                          - P1: Tightened display brightness capability filtering to require explicit displayBrightness capability/command.
 *                          - P2: Rebuilt desiredState map on each evaluation cycle to eliminate lingering stale device state snapshots.
 *  v3.2.0 - 2026/09/27 - UI Persistence, Section Structuring & Collapsible Refactor:
 *                          - Added hidden input name tags to form elements to ensure cell inputs persist correctly upon saving.
 *                          - Added dedicated 'Save Matrix' buttons to sections 3, 4, 5, 6, and 7.
 *                          - Removed excessive top whitespace above Section 3 Do Not Disturb title.
 *                          - Converted sections 1 through 8 into collapsible sections starting collapsed (hideable: true, hidden: true).
 *  v3.1.0 - 2026/09/26 - Restored complete changelog history; added safe getCapabilitiesMap() lookup for screen device filtering.
 *  v3.0.9 - 2026/09/26 - Optimized client-side matrix save handler and table rendering.
 *  v3.0.8 - 2026/09/25 - Added client-side interactive matrix editing for DND, Mute, Volume, Display Brightness, and Display Power.
 *  v3.0.0 - 2026/08/15 - Initial release of Echos Speak Environment Manager under Echos Speak Advanced parent app hierarchy.
 *  v3.0.8    09/26/26    jshimota    UI Policy & State Refactor: Converted Mute matrix (Section 4) into a Boolean ON/OFF toggle defaulting to OFF (Unmuted). Changed Display Brightness (Section 6) default value to 50% for all cells. Added Section 7 for Display Power matrix control (ON/OFF) for screen-capable devices.
 *  v3.0.7    09/26/26    jshimota    Fixed mainPage NPE caused by paragraph UI call outside section block. Scoped buildClientMatrixStylesAndScripts() inside the Section 3 block.
 *  v3.0.6    09/26/26    jshimota    NPE Fix: Added null-safe parameter map validation in mainPage() and saveMatrixSettingsFromParams() to prevent NullPointerException when params object is uninitialized during page dynamic render.
 *  v3.0.5    09/26/26    jshimota    Client-Side Spreadsheet UI Upgrade: Replaced server-reloading buttonLink controls with zero-reload HTML/JS spreadsheet editors. All matrix cells (DND, Mute, Volume, Display) edit fluidly on the client side and commit to settings via a single "Save Environment Matrix" submission.
 *  v3.0.4    09/26/26    jshimota    Interactive Matrix Refactor: Replaced standard inputs with buttonLink() table cell triggers and appButtonHandler() state cycling (Bruce Ravenel / Mode Switches UI pattern). Eliminates dropdown lists and vertical UI fragmentation.
 *  v3.0.3    09/26/26    jshimota    UI Layout Overhaul: Replaced broken HTML input string concatenation with native Hubitat section drill-downs and clean HTML summary tables (Room Lighting pattern). Fixed rendering where inputs stacked vertically down page.
 *  v3.0.2    09/26/26    jshimota    Added Section 4 for Display Brightness matrix control. Dynamically filters physical devices to render only those reporting displayBrightness capability (Echo Show, Spot, Hub).
 *  v3.0.1    09/26/26    jshimota    Architectural Refactor: Removed legacy abstract Policy Profiles and Trigger Engine. Implemented 3-Section Declarative Matrix GUI (Do Not Disturb, Mute, Volume) mapping managed devices against active location modes + Sleeping and Away. Converted DND into a declarative boolean toggle (true = DND On, false = DND Off).
 *  v3.0.0    09/26/26    jshimota    Renamed app to Echos Speak Environment Manager. Bound as child app to Echos Speak Advanced Manager. Removed SpeechSynthesis capability selector; device list and command targets are now queried directly from top-level parent proxy.
 *  v1.4.1    09/26/26    jshimota    Session version demarcation update and clean SpeechSynthesis device selection with driver-type filtering.
 *  v1.4.0    09/26/26    jshimota    Cleaned device selection architecture using standard SpeechSynthesis capability selector combined with driver-type filtering in getManagedEchoDevices(); removed invalid getDeviceById calls.
 *  v1.3.1    09/26/26    jshimota    Fixed Hubitat API NullPointerException/MissingMethodException by replacing invalid app.getHub() calls with native location/getDeviceById calls in device registry helpers.
 *  v1.3.0    09/26/26    jshimota    Replaced capability.musicPlayer selector with dynamic enum discovering Echos Speak Advanced physical child devices; added auto-filtering for WHA family devices; fixed DND method target names (setDoNotDisturbOn/Off); added getManagedEchoDevices() registry abstraction helper.
 *  v1.2.0    09/26/26    jshimota    Decoupled Policy Engine from Trigger Engine; added Command Source/Reason tracking to state matrix and execution audits.
 *  v1.1.0    09/26/26    jshimota    Added Dry Run mode, Reconcile Now and Clear Queue GUI button triggers, and detailed per-device command execution audit logging in state matrix.
 *  v1.0.0    09/26/26    jshimota    Initial release of Echos Speak Volumes Manager.
 **/

static String version() { return '3.4.7' }
def timeStamp() { return "2026/09/28 04:30 PM" }

definition(
    name: "Echos Speak Environment Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Centralized mode-based 5-section shared settings matrix editor (DND, Mute, Volume, Display Brightness, Display Power) and command queuing for physical Echo devices.",
    category: "Utility",
    parent: "jshimota:Echos Speak Advanced Manager",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/echos_speak_environment_manager/echos_speak_environment_manager.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage(Map pageParams = [:]) {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()
        checkAndLogVersionDemarcation()

        /* Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Echos Speak Environment Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        /* Live Device State & Target Matrix (Exposed unconditionally at top) */
        List<String> selectedDnis = getSelectedDeviceDnis()
        section("<b>Live Device State & Target Matrix</b>") {
            if (selectedDnis) {
                paragraph "<div style='color:#5D6D7E; font-size:11px; margin-bottom:6px;'><i>Note: Actual = state currently reported by Echos Speak child devices; Desired = Environment Manager target.</i></div>"
                paragraph buildStatusTable(selectedDnis)
                input name: "btnRefreshDeviceStates", type: "button", title: "<b>🔄 Refresh Device States</b>", width: 6
            } else {
                paragraph "<div style='background-color:#FEF9E7; border-left:4px solid #F39C12; padding:8px; border-radius:4px; font-size:12px;'>" +
                          "⚠️ <b>No Managed Echo Devices Found:</b> Ensure physical devices are configured in Echos Speak Advanced Manager.</div>"
            }
        }

        /* Five Client-Side Interactive Matrix Sections */
        List<String> columns = getActiveColumns()
        if (selectedDnis) {
            
            /* SECTION 1: DO NOT DISTURB MATRIX */
            section("<b>1. Do Not Disturb (DND) Shared Settings Matrix</b>", hideable: true, hidden: true) {
                paragraph "${buildClientMatrixStylesAndScripts()}<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to toggle DND: <span style='color:#1E8449; font-weight:bold;'>☑ ON</span> | <span style='color:#5D6D7E; font-weight:bold;'>☐ OFF</span> (Default: OFF).</div>"
                paragraph buildDndMatrixTable(selectedDnis, columns)
                paragraph "<div style='display:none;'>"
                input name: "dndMatrixPayload", type: "text", title: "", required: false
                paragraph "</div>"
                input name: "btnSaveDndMatrix", type: "button", title: "<b>💾 Save DND Matrix</b>", width: 6
                input name: "btnApplyDndMatrix", type: "button", title: "<b>⚡ Apply DND Now</b>", width: 6
            }

            /* SECTION 2: MUTE MATRIX */
            section("<b>2. Mute Shared Settings Matrix</b>", hideable: true, hidden: true) {
                paragraph "<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to toggle Mute: <span style='color:#900C3F; font-weight:bold;'>☑ ON (Muted)</span> | <span style='color:#5D6D7E; font-weight:bold;'>☐ OFF (Unmuted)</span> (Default: OFF).</div>"
                paragraph buildMuteMatrixTable(selectedDnis, columns)
                paragraph "<div style='display:none;'>"
                input name: "muteMatrixPayload", type: "text", title: "", required: false
                paragraph "</div>"
                input name: "btnSaveMuteMatrix", type: "button", title: "<b>💾 Save Mute Matrix</b>", width: 6
                input name: "btnApplyMuteMatrix", type: "button", title: "<b>⚡ Apply Mute Now</b>", width: 6
            }

            /* SECTION 3: VOLUME LEVEL MATRIX */
            section("<b>3. Volume Level Shared Settings Matrix</b>", hideable: true, hidden: true) {
                paragraph "<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to edit volume level (0-100). Clear value for <code>—</code> (No Change).</div>"
                paragraph buildVolumeMatrixTable(selectedDnis, columns)
                paragraph "<div style='display:none;'>"
                input name: "volMatrixPayload", type: "text", title: "", required: false
                paragraph "</div>"
                input name: "btnSaveVolMatrix", type: "button", title: "<b>💾 Save Volume Matrix</b>", width: 6
                input name: "btnApplyVolMatrix", type: "button", title: "<b>⚡ Apply Volume Now</b>", width: 6
            }

            /* SECTION 4: DISPLAY BRIGHTNESS MATRIX */
            List<String> brightnessDnis = getDisplayBrightnessCapableEchoDevices(selectedDnis)
            section("<b>4. Display Brightness Shared Settings Matrix</b>", hideable: true, hidden: true) {
                if (brightnessDnis) {
                    paragraph "<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to edit screen brightness (0-100). Default value is <b>50%</b> for all cells.</div>"
                    paragraph buildDisplayMatrixTable(brightnessDnis, columns)
                    paragraph "<div style='display:none;'>"
                    input name: "dispMatrixPayload", type: "text", title: "", required: false
                    paragraph "</div>"
                    input name: "btnSaveDispMatrix", type: "button", title: "<b>💾 Save Brightness Matrix</b>", width: 6
                    input name: "btnApplyDispMatrix", type: "button", title: "<b>⚡ Apply Brightness Now</b>", width: 6
                } else {
                    paragraph "<div style='background-color:#F2F4F4; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px;'>" +
                              "ℹ️ <b>No Screen Brightness Devices Selected:</b> None of the physical Echo devices report programmable display brightness capability.</div>"
                }
            }

            /* SECTION 5: DISPLAY POWER MATRIX */
            List<String> displayPowerDnis = getDisplayPowerCapableEchoDevices(selectedDnis)
            section("<b>5. Display Power Shared Settings Matrix</b>", hideable: true, hidden: true) {
                if (displayPowerDnis) {
                    paragraph "<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to toggle display power state: <span style='color:#1E8449; font-weight:bold;'>☑ ON</span> | <span style='color:#5D6D7E; font-weight:bold;'>☐ OFF</span> (Default: ON).</div>"
                    paragraph buildDisplayPowerMatrixTable(displayPowerDnis, columns)
                    paragraph "<div style='display:none;'>"
                    input name: "dispPwrMatrixPayload", type: "text", title: "", required: false
                    paragraph "</div>"
                    input name: "btnSaveDispPwrMatrix", type: "button", title: "<b>💾 Save Display Power Matrix</b>", width: 6
                    input name: "btnApplyDispPwrMatrix", type: "button", title: "<b>⚡ Apply Display Power Now</b>", width: 6
                } else {
                    paragraph "<div style='background-color:#F2F4F4; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px;'>" +
                              "ℹ️ <b>No Display Devices Selected:</b> None of the physical Echo devices report display power capabilities.</div>"
                }
            }
        }

        /* Command Queue & Serialization Settings (Includes Diagnostics & Controls) */
        section("<b>6. Command Queue, Diagnostics & Serialization Settings</b>", hideable: true, hidden: true) {
            input name: "queueDelayMs", type: "number", title: "Pacing Delay between queued commands (ms):", defaultValue: 350, range: "100..2000", required: true
            input name: "forceStateReconcile", type: "bool", title: "Bypass State Check (Force send commands even if state matches)", defaultValue: false
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:10px 0;'/>"
            input name: "dryRunMode", type: "bool", title: "Enable Dry Run Mode (Calculate & log proposed changes without sending commands)", defaultValue: false, submitOnChange: true
            input name: "btnReconcileNow", type: "button", title: "Reconcile Now (Trigger Matrix State Sync)", width: 6
            input name: "btnClearQueue", type: "button", title: "Clear Command Queue", width: 6
        }

        /* Preferences & Logging */
        section("<b>App Preferences & Logging Options</b>", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }
    }
}

private List<String> getActiveColumns() {
    List<String> hubModes = location.modes ? location.modes.collect { it.name } : ["Day", "Evening", "Night"]
    List<String> cols = []
    
    hubModes.each { m -> if (!cols.contains(m)) cols.add(m) }
    if (!cols.contains("Sleeping")) cols.add("Sleeping")
    if (!cols.contains("Away")) cols.add("Away")
    
    return cols
}

private List<String> getSelectedDeviceDnis() {
    Map physMap = parent ? parent.getManagedPhysicalEchoDevices() : [:]
    if (!physMap) return []
    return physMap.keySet().collect { it.toString() }
}

private Map getDeviceProfile(def dev) {
    if (!dev) return [hasDisplayBrightness: false, hasDisplayPower: false]
    Map caps = dev.hasCommand("getCapabilitiesMap") ? dev.getCapabilitiesMap() : (dev.getState()?.capabilitiesMap ?: [:])
    
    // Strict capability-map checking; no hasCommand() fallbacks permitted
    Boolean hasBrightness = (caps.displayBrightness == true)
    Boolean hasPower = (caps.display == true)
    
    return [hasDisplayBrightness: hasBrightness, hasDisplayPower: hasPower]
}

private List<String> getDisplayBrightnessCapableEchoDevices(List<String> selectedDnis) {
    if (!selectedDnis) return []
    List<String> brightnessCapable = []

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (dev && getDeviceProfile(dev).hasDisplayBrightness) {
            brightnessCapable.add(dni)
        }
    }
    return brightnessCapable
}

private List<String> getDisplayPowerCapableEchoDevices(List<String> selectedDnis) {
    if (!selectedDnis) return []
    List<String> displayPowerCapable = []

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (dev && getDeviceProfile(dev).hasDisplayPower) {
            displayPowerCapable.add(dni)
        }
    }
    return displayPowerCapable
}

private Map normalizeDeviceState(def dev, Map profile = [:]) {
    if (!dev) return [actVol: null, actDisp: null, actDispPwr: null, actMute: null, actDnd: null]
    
    def rawVol = dev.currentValue("volume")
    Integer actVol = (rawVol != null && rawVol.toString().isNumber()) ? rawVol.toInteger() : null

    Integer actDisp = null
    if (profile.hasDisplayBrightness) {
        def rawDispB = dev.currentValue("displayBrightness")
        def rawLevel = dev.currentValue("level")
        def selDisp = (rawDispB != null) ? rawDispB : rawLevel
        if (selDisp != null && selDisp.toString().isNumber()) {
            actDisp = selDisp.toInteger()
        }
    }

    String actDispPwr = null
    if (profile.hasDisplayPower) {
        def rawPwr = dev.currentValue("displayPower")
        if (rawPwr != null) {
            String pStr = rawPwr.toString().toLowerCase()
            if (pStr in ["on", "true"]) actDispPwr = "on"
            else if (pStr in ["off", "false"]) actDispPwr = "off"
        }
    }

    def rawMute = dev.currentValue("mute")
    String actMute = null
    if (rawMute != null) {
        String mStr = rawMute.toString().toLowerCase()
        if (mStr in ["muted", "true", "on"]) actMute = "muted"
        else if (mStr in ["unmuted", "false", "off"]) actMute = "unmuted"
    }

    def rawDnd = dev.currentValue("doNotDisturb")
    String actDnd = null
    if (rawDnd != null) {
        String dStr = rawDnd.toString().toLowerCase()
        if (dStr in ["enabled", "true", "on"]) actDnd = "enabled"
        else if (dStr in ["disabled", "false", "off"]) actDnd = "disabled"
    }

    return [
        actVol: actVol,
        actDisp: actDisp,
        actDispPwr: actDispPwr,
        actMute: actMute,
        actDnd: actDnd
    ]
}

private String buildClientMatrixStylesAndScripts() {
    return """
    <style>
        .matrix-table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 12px; }
        .matrix-table th { background-color: #1A252F; color: #FFFFFF; font-weight: bold; padding: 8px 6px; text-align: center; border: 1px solid #2C3E50; }
        .matrix-table th.dev-col { text-align: left; width: 28%; min-width: 160px; }
        .matrix-table td { border: 1px solid #D5D8DC; padding: 4px; text-align: center; vertical-align: middle; background-color: #FFFFFF; }
        .matrix-table td.dev-name { text-align: left; font-weight: bold; color: #2C3E50; background-color: #F8F9F9; padding-left: 8px; }
        
        .cell-bool { cursor: pointer; user-select: none; font-weight: bold; font-size: 13px; border-radius: 4px; padding: 4px 0; transition: all 0.15s ease; }
        .cell-bool.on { background-color: #D4EFDF; color: #1E8449; border: 1px solid #27AE60; }
        .cell-bool.off { background-color: #F2F4F4; color: #5D6D7E; border: 1px solid #BDC3C7; }

        .cell-mute { cursor: pointer; user-select: none; font-weight: bold; font-size: 13px; border-radius: 4px; padding: 4px 0; transition: all 0.15s ease; }
        .cell-mute.on { background-color: #FADBD8; color: #900C3F; border: 1px solid #C0392B; }
        .cell-mute.off { background-color: #F2F4F4; color: #5D6D7E; border: 1px solid #BDC3C7; }

        .cell-num { cursor: pointer; user-select: none; font-weight: bold; font-size: 12px; border-radius: 4px; padding: 4px 0; background-color: #EBF5FB; color: #2E86C1; border: 1px solid #A9CCE3; transition: all 0.15s ease; }
        .cell-num.empty { background-color: #F2F4F4; color: #7F8C8D; border: 1px solid #BDC3C7; font-weight: normal; }
        .cell-num input { width: 45px; text-align: center; font-weight: bold; font-size: 12px; border: 1px solid #2980B9; border-radius: 3px; padding: 2px; }
    </style>
    <script>
        function serializeMatrixPayload(prefix, targetPayloadInputName) {
            var payloadObj = {};
            var inputs = document.querySelectorAll("input[id^='hid_" + prefix + "_']");
            inputs.forEach(function(inp) {
                var key = inp.id.replace("hid_", "");
                payloadObj[key] = inp.value;
            });
            var payloadInput = document.querySelector("input[name='" + targetPayloadInputName + "']");
            if (payloadInput) {
                payloadInput.value = JSON.stringify(payloadObj);
            }
        }

        document.addEventListener("DOMContentLoaded", function() {
            attachPreSubmitListeners();
        });

        function attachPreSubmitListeners() {
            var buttons = document.querySelectorAll("input[type='button'], button");
            buttons.forEach(function(btn) {
                btn.addEventListener("click", function() {
                    serializeMatrixPayload("dnd", "dndMatrixPayload");
                    serializeMatrixPayload("mute", "muteMatrixPayload");
                    serializeMatrixPayload("vol", "volMatrixPayload");
                    serializeMatrixPayload("disp", "dispMatrixPayload");
                    serializeMatrixPayload("disppwr", "dispPwrMatrixPayload");
                });
            });
        }
        setTimeout(attachPreSubmitListeners, 500);

        function toggleBoolCell(el, hiddenId, onText, offText) {
            var hidden = document.getElementById(hiddenId);
            if (!hidden) return;
            var isCurrentlyOn = (hidden.value === "true");
            var newState = !isCurrentlyOn;
            hidden.value = newState ? "true" : "false";
            
            if (newState) {
                el.className = "cell-bool on";
                el.innerHTML = onText || "☑ ON";
            } else {
                el.className = "cell-bool off";
                el.innerHTML = offText || "☐ OFF";
            }
        }

        function toggleMuteCell(el, hiddenId) {
            var hidden = document.getElementById(hiddenId);
            if (!hidden) return;
            var isCurrentlyOn = (hidden.value === "true");
            var newState = !isCurrentlyOn;
            hidden.value = newState ? "true" : "false";
            
            if (newState) {
                el.className = "cell-mute on";
                el.innerHTML = "☑ ON";
            } else {
                el.className = "cell-mute off";
                el.innerHTML = "☐ OFF";
            }
        }

        function editNumberCell(el, hiddenId) {
            if (el.querySelector('input')) return;
            var hidden = document.getElementById(hiddenId);
            var curVal = hidden ? hidden.value : "";
            
            var inp = document.createElement("input");
            inp.type = "number";
            inp.min = "0";
            inp.max = "100";
            inp.value = curVal;
            
            el.innerHTML = "";
            el.appendChild(inp);
            inp.focus();
            inp.select();

            function commit() {
                var v = inp.value.trim();
                if (v !== "" && !isNaN(v)) {
                    var num = Math.max(0, Math.min(100, parseInt(v, 10)));
                    hidden.value = num;
                    el.className = "cell-num";
                    el.innerHTML = num + "%";
                } else {
                    hidden.value = "";
                    el.className = "cell-num empty";
                    el.innerHTML = "—";
                }
            }

            inp.addEventListener("blur", commit);
            inp.addEventListener("keydown", function(e) {
                if (e.key === "Enter") {
                    e.preventDefault();
                    commit();
                }
            });
        }
    </script>
    """
}

private String buildDndMatrixTable(List<String> selectedDnis, List<String> columns) {
    StringBuilder sb = new StringBuilder()
    sb.append("<table class='matrix-table'>")
    sb.append("<tr><th class='dev-col'>Managed Device</th>")
    columns.each { col -> sb.append("<th>${col}</th>") }
    sb.append("</tr>")

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"

        sb.append("<tr>")
        sb.append("<td class='dev-name'>${devLabel}</td>")
        
        columns.each { col ->
            String settingKey = "dnd_${dni}_${col}"
            Boolean isOn = (settings[settingKey] == true || settings[settingKey]?.toString() == "true")
            String cssClass = isOn ? "cell-bool on" : "cell-bool off"
            String labelText = isOn ? "☑ ON" : "☐ OFF"
            String hiddenVal = isOn ? "true" : "false"

            sb.append("<td>")
            sb.append("<input type='hidden' id='hid_${settingKey}' name='${settingKey}' value='${hiddenVal}'/>")
            sb.append("<div class='${cssClass}' onclick=\"toggleBoolCell(this, 'hid_${settingKey}', '☑ ON', '☐ OFF')\">${labelText}</div>")
            sb.append("</td>")
        }
        sb.append("</tr>")
    }
    sb.append("</table>")
    return sb.toString()
}

private String buildMuteMatrixTable(List<String> selectedDnis, List<String> columns) {
    StringBuilder sb = new StringBuilder()
    sb.append("<table class='matrix-table'>")
    sb.append("<tr><th class='dev-col'>Managed Device</th>")
    columns.each { col -> sb.append("<th>${col}</th>") }
    sb.append("</tr>")

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"

        sb.append("<tr>")
        sb.append("<td class='dev-name'>${devLabel}</td>")
        
        columns.each { col ->
            String settingKey = "mute_${dni}_${col}"
            Boolean isOn = (settings[settingKey] == true || settings[settingKey]?.toString() == "true" || settings[settingKey]?.toString() == "muted")
            String cssClass = isOn ? "cell-mute on" : "cell-mute off"
            String labelText = isOn ? "☑ ON" : "☐ OFF"
            String hiddenVal = isOn ? "true" : "false"

            sb.append("<td>")
            sb.append("<input type='hidden' id='hid_${settingKey}' name='${settingKey}' value='${hiddenVal}'/>")
            sb.append("<div class='${cssClass}' onclick=\"toggleMuteCell(this, 'hid_${settingKey}')\">${labelText}</div>")
            sb.append("</td>")
        }
        sb.append("</tr>")
    }
    sb.append("</table>")
    return sb.toString()
}

private String buildVolumeMatrixTable(List<String> selectedDnis, List<String> columns) {
    StringBuilder sb = new StringBuilder()
    sb.append("<table class='matrix-table'>")
    sb.append("<tr><th class='dev-col'>Managed Device</th>")
    columns.each { col -> sb.append("<th>${col}</th>") }
    sb.append("</tr>")

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"

        sb.append("<tr>")
        sb.append("<td class='dev-name'>${devLabel}</td>")
        
        columns.each { col ->
            String settingKey = "vol_${dni}_${col}"
            def rawVal = settings[settingKey]
            String valStr = (rawVal != null && rawVal.toString().trim() != "") ? rawVal.toString().trim() : ""
            
            Boolean hasValue = (valStr != "")
            String cssClass = hasValue ? "cell-num" : "cell-num empty"
            String labelText = hasValue ? "${valStr}%" : "—"

            sb.append("<td>")
            sb.append("<input type='hidden' id='hid_${settingKey}' name='${settingKey}' value='${valStr}'/>")
            sb.append("<div class='${cssClass}' onclick=\"editNumberCell(this, 'hid_${settingKey}')\">${labelText}</div>")
            sb.append("</td>")
        }
        sb.append("</tr>")
    }
    sb.append("</table>")
    return sb.toString()
}

private String buildDisplayMatrixTable(List<String> brightnessDnis, List<String> columns) {
    StringBuilder sb = new StringBuilder()
    sb.append("<table class='matrix-table'>")
    sb.append("<tr><th class='dev-col'>Display Device</th>")
    columns.each { col -> sb.append("<th>${col}</th>") }
    sb.append("</tr>")

    brightnessDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"

        sb.append("<tr>")
        sb.append("<td class='dev-name'>${devLabel}</td>")
        
        columns.each { col ->
            String settingKey = "disp_${dni}_${col}"
            def rawVal = settings[settingKey]
            String valStr = (rawVal != null && rawVal.toString().trim() != "") ? rawVal.toString().trim() : "50"
            
            String cssClass = "cell-num"
            String labelText = "${valStr}%"

            sb.append("<td>")
            sb.append("<input type='hidden' id='hid_${settingKey}' name='${settingKey}' value='${valStr}'/>")
            sb.append("<div class='${cssClass}' onclick=\"editNumberCell(this, 'hid_${settingKey}')\">${labelText}</div>")
            sb.append("</td>")
        }
        sb.append("</tr>")
    }
    sb.append("</table>")
    return sb.toString()
}

private String buildDisplayPowerMatrixTable(List<String> displayPowerDnis, List<String> columns) {
    StringBuilder sb = new StringBuilder()
    sb.append("<table class='matrix-table'>")
    sb.append("<tr><th class='dev-col'>Display Device</th>")
    columns.each { col -> sb.append("<th>${col}</th>") }
    sb.append("</tr>")

    displayPowerDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"

        sb.append("<tr>")
        sb.append("<td class='dev-name'>${devLabel}</td>")
        
        columns.each { col ->
            String settingKey = "disppwr_${dni}_${col}"
            Boolean isOn = (settings[settingKey] == null || settings[settingKey] == true || settings[settingKey]?.toString() == "true")
            String cssClass = isOn ? "cell-bool on" : "cell-bool off"
            String labelText = isOn ? "☑ ON" : "☐ OFF"
            String hiddenVal = isOn ? "true" : "false"

            sb.append("<td>")
            sb.append("<input type='hidden' id='hid_${settingKey}' name='${settingKey}' value='${hiddenVal}'/>")
            sb.append("<div class='${cssClass}' onclick=\"toggleBoolCell(this, 'hid_${settingKey}', '☑ ON', '☐ OFF')\">${labelText}</div>")
            sb.append("</td>")
        }
        sb.append("</tr>")
    }
    sb.append("</table>")
    return sb.toString()
}

void appButtonHandler(String btn) {
    if (!btn) return
    logTrace "appButtonHandler() triggered -> Button: [${btn}]"

    if (btn == "btnRefreshDeviceStates") {
        logInfo "<b>'REFRESH DEVICE STATES'</b> STARTING -> Polling attributes across physical Echo devices and re-synchronizing matrix..."
        refreshManagedDeviceStates()
        logInfo "<b>'REFRESH DEVICE STATES'</b> COMPLETED -> Device attributes refreshed and status matrix updated."
    } else if (btn == "btnReconcileNow") {
        logInfo "Manual 'Reconcile Now' requested."
        evaluateCurrentModeState("Manual GUI Reconcile")
    } else if (btn == "btnClearQueue") {
        logWarn "Manual 'Clear Queue' requested. Purging pending commands."
        atomicState.commandQueue = []
    } else if (btn in ["btnSaveDndMatrix", "btnSaveMuteMatrix", "btnSaveVolMatrix", "btnSaveDispMatrix", "btnSaveDispPwrMatrix"]) {
        logInfo "<b>'SAVE MATRIX SECTION'</b> STARTING -> Synchronizing client-side settings for button: [${btn}]"
        saveMatrixSettingsFromParams(btn)
        logInfo "<b>'SAVE MATRIX SECTION'</b> COMPLETED -> Configuration persisted for button: [${btn}]"
    } else if (btn in ["btnApplyDndMatrix", "btnApplyMuteMatrix", "btnApplyVolMatrix", "btnApplyDispMatrix", "btnApplyDispPwrMatrix"]) {
        logInfo "<b>'APPLY MATRIX SECTION NOW'</b> STARTING -> Persisting and executing active column state for button: [${btn}]"
        String correspondingSaveBtn = btn.replace("btnApply", "btnSave")
        saveMatrixSettingsFromParams(correspondingSaveBtn)
        applySingleMatrixStateForCurrentMode(btn)
        logInfo "<b>'APPLY MATRIX SECTION NOW'</b> COMPLETED -> State execution complete for button: [${btn}]"
    }
}

private void refreshManagedDeviceStates() {
    List<String> selectedDnis = getSelectedDeviceDnis()
    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (dev) {
            try {
                if (dev.hasCommand("refresh")) {
                    dev.refresh()
                } else if (dev.hasCommand("poll")) {
                    dev.poll()
                }
            } catch (Exception e) {
                logWarn "Unable to execute poll/refresh on device [${dev.displayName}]: ${e.message}"
            }
        }
    }
    
    // Purge lingering desiredState keys for devices or capabilities no longer managed
    Map currentDesired = atomicState.desiredState ?: [:]
    Map cleanedDesired = [:]
    selectedDnis.each { dni ->
        if (currentDesired.containsKey(dni)) {
            def dev = parent ? parent.getManagedChildDevice(dni) : null
            if (dev) {
                Map profile = getDeviceProfile(dev)
                Map entry = currentDesired[dni] ?: [:]
                if (!profile.hasDisplayBrightness) entry.remove("displayBrightness")
                if (!profile.hasDisplayPower) entry.remove("displayPower")
                cleanedDesired[dni] = entry
            }
        }
    }
    atomicState.desiredState = cleanedDesired

    evaluateCurrentModeState("Device State Refresh Requested")
}

private void saveMatrixSettingsFromParams(String buttonName) {
    Map requestParams = (params != null) ? params : [:]
    int persistedCount = 0

    Map payloadMap = [:]
    String rawPayload = ""

    if (buttonName == "btnSaveDndMatrix" && requestParams.containsKey("dndMatrixPayload")) {
        rawPayload = requestParams["dndMatrixPayload"]?.toString() ?: ""
    } else if (buttonName == "btnSaveMuteMatrix" && requestParams.containsKey("muteMatrixPayload")) {
        rawPayload = requestParams["muteMatrixPayload"]?.toString() ?: ""
    } else if (buttonName == "btnSaveVolMatrix" && requestParams.containsKey("volMatrixPayload")) {
        rawPayload = requestParams["volMatrixPayload"]?.toString() ?: ""
    } else if (buttonName == "btnSaveDispMatrix" && requestParams.containsKey("dispMatrixPayload")) {
        rawPayload = requestParams["dispMatrixPayload"]?.toString() ?: ""
    } else if (buttonName == "btnSaveDispPwrMatrix" && requestParams.containsKey("dispPwrMatrixPayload")) {
        rawPayload = requestParams["dispPwrMatrixPayload"]?.toString() ?: ""
    }

    if (rawPayload != "") {
        try {
            payloadMap = new groovy.json.JsonSlurper().parseText(rawPayload)
        } catch (Exception e) {
            logError "Failed to parse matrix JSON payload for button [${buttonName}]: ${e.message}"
        }
    }

    // Blend parsed payload back into requestParams
    payloadMap.each { k, v ->
        requestParams[k.toString()] = v
    }

    List<String> selectedDnis = getSelectedDeviceDnis()
    List<String> brightnessDnis = getDisplayBrightnessCapableEchoDevices(selectedDnis)
    List<String> displayPowerDnis = getDisplayPowerCapableEchoDevices(selectedDnis)
    List<String> columns = getActiveColumns()

    if (buttonName == "btnSaveDndMatrix") {
        selectedDnis.each { dni ->
            columns.each { col ->
                String dndKey = "dnd_${dni}_${col}"
                if (requestParams.containsKey(dndKey)) {
                    Boolean dndVal = (requestParams[dndKey]?.toString() == "true")
                    app.updateSetting(dndKey, [type: "bool", value: dndVal])
                    persistedCount++
                }
            }
        }
    } else if (buttonName == "btnSaveMuteMatrix") {
        selectedDnis.each { dni ->
            columns.each { col ->
                String muteKey = "mute_${dni}_${col}"
                if (requestParams.containsKey(muteKey)) {
                    Boolean muteVal = (requestParams[muteKey]?.toString() == "true")
                    app.updateSetting(muteKey, [type: "bool", value: muteVal])
                    persistedCount++
                }
            }
        }
    } else if (buttonName == "btnSaveVolMatrix") {
        selectedDnis.each { dni ->
            columns.each { col ->
                String volKey = "vol_${dni}_${col}"
                if (requestParams.containsKey(volKey)) {
                    String vStr = requestParams[volKey]?.toString()?.trim() ?: ""
                    if (vStr != "" && vStr.isNumber()) {
                        app.updateSetting(volKey, [type: "number", value: vStr.toInteger()])
                    } else {
                        app.removeSetting(volKey)
                    }
                    persistedCount++
                }
            }
        }
    } else if (buttonName == "btnSaveDispMatrix") {
        brightnessDnis.each { dni ->
            columns.each { col ->
                String dispKey = "disp_${dni}_${col}"
                if (requestParams.containsKey(dispKey)) {
                    String dStr = requestParams[dispKey]?.toString()?.trim() ?: ""
                    if (dStr != "" && dStr.isNumber()) {
                        app.updateSetting(dispKey, [type: "number", value: dStr.toInteger()])
                    } else {
                        app.updateSetting(dispKey, [type: "number", value: 50])
                    }
                    persistedCount++
                }
            }
        }
    } else if (buttonName == "btnSaveDispPwrMatrix") {
        displayPowerDnis.each { dni ->
            columns.each { col ->
                String dispPwrKey = "disppwr_${dni}_${col}"
                if (requestParams.containsKey(dispPwrKey)) {
                    Boolean pwrVal = (requestParams[dispPwrKey]?.toString() == "true")
                    app.updateSetting(dispPwrKey, [type: "bool", value: pwrVal])
                    persistedCount++
                }
            }
        }
    }

    logInfo "Matrix Section Configuration [${buttonName}] successfully persisted (${persistedCount} field entries synchronized from browser JSON payload)."
}

private void applySingleMatrixStateForCurrentMode(String buttonName) {
    String activeCol = state.activeColumnOverride ?: location.mode
    if (!activeCol) return

    logInfo "Applying Section State [${buttonName}] for Current Mode Column [${activeCol}]"
    
    long currentEpoch = (atomicState.reconciliationEpoch ?: 0L) as long
    Boolean forceDispatch = getSettingBool("forceStateReconcile", false)
    List<String> selectedDnis = getSelectedDeviceDnis()
    Map currentDesired = atomicState.desiredState ?: [:]

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (!dev) return

        Map profile = getDeviceProfile(dev)
        Map normState = normalizeDeviceState(dev, profile)
        Map dMap = currentDesired[dni] ?: [:]
        String sourceReason = activeCol

        if (buttonName == "btnApplyDndMatrix") {
            Boolean targetDndBool = (settings["dnd_${dni}_${activeCol}"] == true || settings["dnd_${dni}_${activeCol}"]?.toString() == "true")
            dMap.dnd = targetDndBool
            dMap.activeColName = sourceReason
            Boolean isDndActive = (normState.actDnd == "enabled")
            if (forceDispatch || isDndActive != targetDndBool) {
                String cmd = targetDndBool ? "setDoNotDisturbOn" : "setDoNotDisturbOff"
                queueCommand(dni, cmd, null, sourceReason, currentEpoch)
            }
        } else if (buttonName == "btnApplyMuteMatrix") {
            Boolean targetMuteBool = (settings["mute_${dni}_${activeCol}"] == true || settings["mute_${dni}_${activeCol}"]?.toString() == "true" || settings["mute_${dni}_${activeCol}"]?.toString() == "muted")
            dMap.mute = targetMuteBool
            dMap.activeColName = sourceReason
            Boolean isMuted = (normState.actMute == "muted")
            if (forceDispatch || isMuted != targetMuteBool) {
                String cmd = targetMuteBool ? "mute" : "unmute"
                queueCommand(dni, cmd, null, sourceReason, currentEpoch)
            }
        } else if (buttonName == "btnApplyVolMatrix") {
            def targetVol = settings["vol_${dni}_${activeCol}"]
            if (targetVol != null && targetVol.toString().trim() != "") {
                dMap.volume = targetVol.toInteger()
                dMap.activeColName = sourceReason
                if (forceDispatch || normState.actVol != targetVol.toInteger()) {
                    queueCommand(dni, "setVolume", targetVol.toInteger(), sourceReason, currentEpoch)
                }
            }
        } else if (buttonName == "btnApplyDispMatrix") {
            if (profile.hasDisplayBrightness) {
                def targetDisp = settings["disp_${dni}_${activeCol}"] != null ? settings["disp_${dni}_${activeCol}"] : 50
                dMap.displayBrightness = targetDisp.toInteger()
                dMap.activeColName = sourceReason
                if (forceDispatch || normState.actDisp != targetDisp.toInteger()) {
                    queueCommand(dni, "setDisplayBrightness", targetDisp.toInteger(), sourceReason, currentEpoch)
                }
            }
        } else if (buttonName == "btnApplyDispPwrMatrix") {
            if (profile.hasDisplayPower) {
                Boolean targetDispPwr = (settings["disppwr_${dni}_${activeCol}"] == null || settings["disppwr_${dni}_${activeCol}"] == true || settings["disppwr_${dni}_${activeCol}"]?.toString() == "true")
                dMap.displayPower = targetDispPwr
                dMap.activeColName = sourceReason
                Boolean isPwrOn = (normState.actDispPwr == "on")
                if (forceDispatch || isPwrOn != targetDispPwr) {
                    String cmd = targetDispPwr ? (dev.hasCommand("setDisplayPowerOn") ? "setDisplayPowerOn" : "setDisplayPower") : (dev.hasCommand("setDisplayPowerOff") ? "setDisplayPowerOff" : "setDisplayPower")
                    Object param = (cmd == "setDisplayPower") ? (targetDispPwr ? "on" : "off") : null
                    queueCommand(dni, cmd, param, sourceReason, currentEpoch)
                }
            }
        }

        currentDesired[dni] = dMap
    }

    atomicState.desiredState = currentDesired
    processQueue()
}

private String buildStatusTable(List<String> selectedDnis) {
    if (!selectedDnis) return "<i>No physical Echo devices found.</i>"

    // Alphabetical device sorting
    List<String> sortedDnis = selectedDnis.sort { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        return dev ? dev.displayName?.toLowerCase() : dni.toLowerCase()
    }

    StringBuilder sb = new StringBuilder()
    if (getSettingBool("dryRunMode", false)) {
        sb.append("<div style='background-color:#E74C3C; color:#FFFFFF; padding:6px; font-weight:bold; text-align:center; border-radius:4px; margin-bottom:8px;'>DRY RUN MODE ACTIVE - Proposed changes calculated but suppressed.</div>")
    }

    sb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; text-align:left;'>")
    sb.append("<tr style='background-color:#2C3E50; color:#FFFFFF;'>")
    sb.append("<th style='padding:6px;'>Device Name</th>")
    sb.append("<th style='padding:6px;'>DND (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>Mute (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>Volume (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>Brightness (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>Display Power (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>Active Mode</th>")
    sb.append("<th style='padding:6px;'>Sync Status</th>")
    sb.append("</tr>")

    Map desired = atomicState.desiredState ?: [:]

    sortedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"
        Map profile = getDeviceProfile(dev)
        Map normState = normalizeDeviceState(dev, profile)
        Map dMap = desired[dni] ?: [:]

        String actVolStr = (normState.actVol != null) ? "${normState.actVol}%" : "UNKNOWN"
        String actDispStr = profile.hasDisplayBrightness ? ((normState.actDisp != null) ? "${normState.actDisp}%" : "UNKNOWN") : "----"
        String actDispPwrStr = profile.hasDisplayPower ? ((normState.actDispPwr != null) ? normState.actDispPwr : "UNKNOWN") : "----"
        String actMuteStr = (normState.actMute != null) ? normState.actMute : "UNKNOWN"
        String actDndStr = (normState.actDnd != null) ? normState.actDnd : "UNKNOWN"

        String desVolStr = (dMap.volume != null) ? "${dMap.volume}%" : "—"
        String desDispStr = profile.hasDisplayBrightness ? ((dMap.displayBrightness != null) ? "${dMap.displayBrightness}%" : "—") : "----"
        String desDispPwrStr = profile.hasDisplayPower ? ((dMap.displayPower != null) ? (dMap.displayPower ? "on" : "off") : "—") : "----"
        String desMuteStr = (dMap.mute != null) ? (dMap.mute ? "muted" : "unmuted") : "—"
        String desDndStr = (dMap.dnd != null) ? (dMap.dnd ? "enabled" : "disabled") : "—"
        
        String activeColName = dMap.activeColName ?: (state.activeColumnOverride ?: location.mode ?: "Unknown")

        Boolean volInSync = (dMap.volume == null || (normState.actVol != null && normState.actVol == dMap.volume))
        Boolean muteInSync = (dMap.mute == null || (normState.actMute != null && (dMap.mute ? normState.actMute == "muted" : normState.actMute == "unmuted")))
        Boolean dndInSync = (dMap.dnd == null || (normState.actDnd != null && (dMap.dnd ? normState.actDnd == "enabled" : normState.actDnd == "disabled")))
        
        Boolean dispInSync = true
        if (profile.hasDisplayBrightness && dMap.displayBrightness != null) {
            dispInSync = (normState.actDisp != null && normState.actDisp == dMap.displayBrightness)
        }

        Boolean dispPwrInSync = true
        if (profile.hasDisplayPower && dMap.displayPower != null) {
            dispPwrInSync = (normState.actDispPwr != null && (dMap.displayPower ? normState.actDispPwr == "on" : normState.actDispPwr == "off"))
        }

        Boolean hasUnknownState = (normState.actVol == null && dMap.volume != null) ||
                                  (normState.actMute == null && dMap.mute != null) ||
                                  (normState.actDnd == null && dMap.dnd != null) ||
                                  (profile.hasDisplayBrightness && normState.actDisp == null && dMap.displayBrightness != null) ||
                                  (profile.hasDisplayPower && normState.actDispPwr == null && dMap.displayPower != null)

        Boolean fullySynced = volInSync && dispInSync && dispPwrInSync && muteInSync && dndInSync && !hasUnknownState
        
        String statusBadge
        if (fullySynced) {
            statusBadge = "<span style='color:#27AE60; font-weight:bold;'>✔ Synced</span>"
        } else if (hasUnknownState) {
            statusBadge = "<span style='color:#7F8C8D; font-weight:bold;'>❓ State Unknown</span>"
        } else {
            statusBadge = "<span style='color:#E67E22; font-weight:bold;'>⚠ Pending</span>"
        }

        String volCellText = "${actVolStr} / ${desVolStr}"
        String dispCellText = profile.hasDisplayBrightness ? "${actDispStr} / ${desDispStr}" : "----"
        String dispPwrCellText = profile.hasDisplayPower ? "${actDispPwrStr} / ${desDispPwrStr}" : "----"

        sb.append("<tr style='border-bottom:1px solid #E0E0E0;'>")
        sb.append("<td style='padding:6px;'><b>${devLabel}</b></td>")
        sb.append("<td style='padding:6px;'>${actDndStr} / ${desDndStr}</td>")
        sb.append("<td style='padding:6px;'>${actMuteStr} / ${desMuteStr}</td>")
        sb.append("<td style='padding:6px;'>${volCellText}</td>")
        sb.append("<td style='padding:6px;'>${dispCellText}</td>")
        sb.append("<td style='padding:6px;'>${dispPwrCellText}</td>")
        sb.append("<td style='padding:6px; font-size:11px; color:#34495E;'><b>${activeColName}</b></td>")
        sb.append("<td style='padding:6px;'>${statusBadge}</td>")
        sb.append("</tr>")
    }

    sb.append("</table>")
    
    List queue = atomicState.commandQueue ?: []
    sb.append("<div style='margin-top:6px; margin-bottom:8px; font-size:11px; color:#7F8C8D;'>Pending Command Queue: <b>${queue.size()} items</b></div>")
    return sb.toString()
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logInfo "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Echos Speak Environment Manager"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn")) }
        .sort()

    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
}

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing app v${version()} (${timeStamp()})..."
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    atomicState.commandQueue = []
    atomicState.desiredState = [:]
    atomicState.reconciliationEpoch = 0
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (state.appVersion != version())

    if (settingsChanged || codeVersionChanged) {
        logInfo "Settings or code version modification detected. Re-establishing subscriptions..."
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(false)
    } else {
        logDebug "App closed without setting or version changes."
    }
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling app..."
    unsubscribe()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    updateAppLabel()

    subscribe(location, "mode", modeChangeHandler)
    subscribe(location, "sleeping", sleepingChangeHandler)
    logInfo "Subscribed to Location Mode and Sleeping status events."

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        unschedule("disableDebugLogging")
        runIn(1800, "disableDebugLogging", [overwrite: false])
    }

    evaluateCurrentModeState("App Initialized")
}

void modeChangeHandler(evt) {
    logInfo "Location mode change event detected -> ${evt.value}"
    state.remove("activeColumnOverride")
    evaluateCurrentModeState("Mode Changed to [${evt.value}]")
}

void sleepingChangeHandler(evt) {
    logInfo "Location sleeping event detected -> ${evt.value}"
    if (evt.value == "sleeping" || evt.value == "true" || evt.value == true) {
        setActiveColumn("Sleeping", "Location Sleeping Event")
    } else {
        state.remove("activeColumnOverride")
        evaluateCurrentModeState("Location Sleeping Ended")
    }
}

public void setActiveColumn(String columnName, String reason = "External Command") {
    if (!columnName) return
    logInfo "External Active Column Override requested: [${columnName}] | Reason: ${reason}"
    state.activeColumnOverride = columnName
    evaluateMatrixForColumn(columnName, reason)
}

private void evaluateCurrentModeState(String triggerReason) {
    String activeCol = state.activeColumnOverride ?: location.mode
    evaluateMatrixForColumn(activeCol, triggerReason)
}

public void evaluateMatrixForColumn(String columnName, String sourceReason) {
    if (!columnName) return
    logInfo "<b>'EVALUATE MATRIX STATE PUSH'</b> STARTING -> Column: [${columnName}] | Trigger: [${sourceReason}]"

    long newEpoch = (atomicState.reconciliationEpoch ?: 0L) + 1L
    atomicState.reconciliationEpoch = newEpoch
    
    // Purge outdated commands from prior modes
    atomicState.commandQueue = []

    // Re-initialize clean desiredState snapshot map
    Map currentDesired = [:]
    Boolean forceDispatch = getSettingBool("forceStateReconcile", false)
    List<String> selectedDnis = getSelectedDeviceDnis()

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (!dev) return

        Map profile = getDeviceProfile(dev)
        Map normState = normalizeDeviceState(dev, profile)
        Map dMap = [:]

        def targetVol  = settings["vol_${dni}_${columnName}"]
        def targetDisp = settings["disp_${dni}_${columnName}"] != null ? settings["disp_${dni}_${columnName}"] : 50
        
        Boolean targetMuteBool = (settings["mute_${dni}_${columnName}"] == true || settings["mute_${dni}_${columnName}"]?.toString() == "true" || settings["mute_${dni}_${columnName}"]?.toString() == "muted")
        Boolean targetDndBool  = (settings["dnd_${dni}_${columnName}"] == true || settings["dnd_${dni}_${columnName}"]?.toString() == "true")
        Boolean targetDispPwr  = (settings["disppwr_${dni}_${columnName}"] == null || settings["disppwr_${dni}_${columnName}"] == true || settings["disppwr_${dni}_${columnName}"]?.toString() == "true")

        if (targetVol != null && targetVol.toString().trim() != "") {
            dMap.volume = targetVol.toInteger()
        }
        
        if (profile.hasDisplayBrightness) {
            dMap.displayBrightness = targetDisp.toInteger()
        }
        
        if (profile.hasDisplayPower) {
            dMap.displayPower = targetDispPwr
        }

        dMap.mute = targetMuteBool
        dMap.dnd  = targetDndBool
        dMap.activeColName = columnName

        currentDesired[dni] = dMap

        if (targetVol != null && targetVol.toString().trim() != "" && (forceDispatch || normState.actVol != targetVol.toInteger())) {
            queueCommand(dni, "setVolume", targetVol.toInteger(), columnName, newEpoch)
        }

        if (profile.hasDisplayBrightness) {
            if (forceDispatch || normState.actDisp != targetDisp.toInteger()) {
                queueCommand(dni, "setDisplayBrightness", targetDisp.toInteger(), columnName, newEpoch)
            }
        }

        if (profile.hasDisplayPower) {
            Boolean isPwrOn = (normState.actDispPwr == "on")
            if (forceDispatch || isPwrOn != targetDispPwr) {
                String cmd = targetDispPwr ? (dev.hasCommand("setDisplayPowerOn") ? "setDisplayPowerOn" : "setDisplayPower") : (dev.hasCommand("setDisplayPowerOff") ? "setDisplayPowerOff" : "setDisplayPower")
                Object param = (cmd == "setDisplayPower") ? (targetDispPwr ? "on" : "off") : null
                queueCommand(dni, cmd, param, columnName, newEpoch)
            }
        }
        
        Boolean isMuted = (normState.actMute == "muted")
        if (forceDispatch || isMuted != targetMuteBool) {
            String cmd = targetMuteBool ? "mute" : "unmute"
            queueCommand(dni, cmd, null, columnName, newEpoch)
        }
        
        Boolean isDndActive = (normState.actDnd == "enabled")
        if (forceDispatch || isDndActive != targetDndBool) {
            String cmd = targetDndBool ? "setDoNotDisturbOn" : "setDoNotDisturbOff"
            queueCommand(dni, cmd, null, columnName, newEpoch)
        }
    }

    atomicState.desiredState = currentDesired
    processQueue()
    logInfo "<b>'EVALUATE MATRIX STATE PUSH'</b> COMPLETED -> State processing finalized for Column: [${columnName}]"
}

private void queueCommand(String dni, String command, Object param, String reason, long epoch) {
    if (getSettingBool("dryRunMode", false)) {
        logInfo "<b>[DRY RUN ACTIVE]</b> Command calculated & suppressed -> Device DNI: ${dni} | Cmd: ${command} | Arg: ${param} | Reason: ${reason}"
        return
    }

    List queue = atomicState.commandQueue ?: []
    queue.removeAll { it.dni == dni && it.command == command }

    queue.add([
        dni: dni, 
        command: command, 
        param: param, 
        reason: reason,
        epoch: epoch,
        queuedAt: now()
    ])
    atomicState.commandQueue = queue
    logDebug "Queued command [${command}] for device DNI [${dni}]. Epoch: ${epoch} | Queue Depth: ${queue.size()}"
}

void processQueue() {
    List queue = atomicState.commandQueue ?: []
    if (!queue || queue.size() == 0) {
        logDebug "Command queue empty. Reconciliation complete."
        return
    }

    Map item = queue.remove(0)
    atomicState.commandQueue = queue

    long activeEpoch = (atomicState.reconciliationEpoch ?: 0L) as long
    long itemEpoch = (item.epoch ?: 0L) as long

    if (itemEpoch != activeEpoch) {
        logWarn "Discarding obsolete command [${item.command}] for device DNI [${item.dni}]. Item Epoch: ${itemEpoch} | Active Epoch: ${activeEpoch}"
        if (atomicState.commandQueue.size() > 0) {
            processQueue()
        }
        return
    }

    def dev = parent ? parent.getManagedChildDevice(item.dni) : null

    if (dev) {
        try {
            logInfo "Executing Queued Command -> Device: ${dev.displayName} | Cmd: ${item.command}(${item.param != null ? item.param : ''}) | Source: ${item.reason}"
            if (item.param != null) {
                dev."${item.command}"(item.param)
            } else {
                dev."${item.command}"()
            }
        } catch (Exception e) {
            logError "Failed to execute command ${item.command} on device ${dev.displayName}: ${e.message}"
        }
    } else {
        logWarn "Target device DNI ${item.dni} no longer found in registry."
    }

    if (atomicState.commandQueue.size() > 0) {
        Integer delay = settings["queueDelayMs"] ? settings["queueDelayMs"].toInteger() : 350
        runInMillis(delay, "processQueue", [overwrite: true])
    }
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appLabel = app.label ?: app.name ?: "App"

    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${appLabel}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    def val = settings[key]
    if (val == null) return defaultVal
    if (val instanceof Boolean) return val
    return val.toString().toBoolean()
}