/**
 * Application Name: Echos Speak Environment Manager
 * Platform: Hubitat Elevation (C-7 / OS 2.4.4.156)
 * Notes: Centralized environmental state matrix (Do Not Disturb, Mute, Volume, Display Brightness), mode reconciliation, and command pacing for Echo Speaks devices.
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
 **/
/**
 *  Purpose:
 *  Provides central 4-Section Matrix state management (DND, Mute, Volume Level, Display Brightness) per Hubitat Mode + Sleeping/Away.
 *  Uses client-side HTML/JS matrix state editing inside Hubitat paragraph elements with safe section encapsulation.
 *
 *  Changelog:
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

static String version() { return '3.0.7' }
def timeStamp() { return "2026/09/26 3:52 PM" }

definition(
    name: "Echos Speak Environment Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Centralized mode-based 4-section matrix editor (DND, Mute, Volume, Display) and command queuing for physical Echo devices.",
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

        /* Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Echos Speak Environment Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        /* 1. Device Registry Selection */
        Map physOptions = getPhysicalEchoDropdownOptions()
        section("<b>1. Device Registry (Physical Echo Devices)</b>") {
            if (physOptions) {
                input name: "selectedEchoDnis", type: "enum", title: "Select Physical Echo Devices to Manage (WHA groups automatically excluded):", options: physOptions, multiple: true, required: true, submitOnChange: true
            } else {
                paragraph "<div style='background-color:#FEF9E7; border-left:4px solid #F39C12; padding:8px; border-radius:4px; font-size:12px;'>" +
                          "⚠️ <b>No Physical Echo Devices Discovered:</b> Ensure Echos Speak Advanced Manager is configured and managing physical devices.</div>"
            }
        }

        /* Diagnostics & Control Panel */
        section("<b>Diagnostics & Control Panel</b>") {
            input name: "dryRunMode", type: "bool", title: "Enable Dry Run Mode (Calculate & log proposed changes without sending commands)", defaultValue: false, submitOnChange: true
            input name: "btnReconcileNow", type: "button", title: "Reconcile Now (Trigger Matrix State Sync)", width: 6
            input name: "btnClearQueue", type: "button", title: "Clear Command Queue", width: 6
        }

        /* 2. Live Device State & Target Matrix */
        List<String> selectedDnis = getSelectedDeviceDnis()
        if (selectedDnis) {
            section("<b>2. Live Device State & Target Matrix</b>") {
                paragraph buildStatusTable(selectedDnis)
            }
        }

        /* 3. Four Client-Side Interactive Matrix Sections */
        List<String> columns = getActiveColumns()
        if (selectedDnis) {
            
            /* SECTION 1: DO NOT DISTURB MATRIX */
            section("<b>3. Do Not Disturb (DND) Policy Matrix</b>") {
                /* Global CSS & JS Injection for Client-Side Matrix Editing */
                paragraph buildClientMatrixStylesAndScripts()
                paragraph "<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to toggle DND: <span style='color:#1E8449; font-weight:bold;'>☑ ON</span> | <span style='color:#5D6D7E; font-weight:bold;'>☐ OFF</span>. Edits remain client-side until saved below.</div>"
                paragraph buildDndMatrixTable(selectedDnis, columns)
            }

            /* SECTION 2: MUTE POLICY MATRIX */
            section("<b>4. Mute Policy Matrix</b>") {
                paragraph "<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to cycle Mute: <span style='color:#5D6D7E; font-weight:bold;'>— (NO CHANGE)</span> ➔ <span style='color:#900C3F; font-weight:bold;'>MUTED</span> ➔ <span style='color:#1E8449; font-weight:bold;'>UNMUTED</span>.</div>"
                paragraph buildMuteMatrixTable(selectedDnis, columns)
            }

            /* SECTION 3: VOLUME LEVEL MATRIX */
            section("<b>5. Volume Level Policy Matrix</b>") {
                paragraph "<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to edit volume (0-100). Clear value for <code>—</code> (No Change).</div>"
                paragraph buildVolumeMatrixTable(selectedDnis, columns)
            }

            /* SECTION 4: DISPLAY BRIGHTNESS MATRIX */
            List<String> displayDnis = getDisplayCapableEchoDevices(selectedDnis)
            section("<b>6. Display Brightness Policy Matrix (Screen Devices Only)</b>") {
                if (displayDnis) {
                    paragraph "<div style='color:#555; font-size:12px; margin-bottom:6px;'>Click cell to edit screen brightness (0-100) for Shows, Spots, and Hubs. Clear value for <code>—</code> (No Change).</div>"
                    paragraph buildDisplayMatrixTable(displayDnis, columns)
                } else {
                    paragraph "<div style='background-color:#F2F4F4; border-left:4px solid #7F8C8D; padding:8px; border-radius:4px; font-size:12px;'>" +
                              "ℹ️ <b>No Screen-Capable Echo Devices Selected:</b> None of the currently selected physical Echo devices report display brightness capabilities.</div>"
                }
            }

            /* Single Save Section for all Matrices */
            section("<b>Save Configuration Changes</b>") {
                paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>Clicking Save persists all updated DND, Mute, Volume, and Display settings without immediately dispatching commands to Echo hardware.</div>"
                input name: "btnSaveMatrix", type: "button", title: "<b>💾 SAVE ENVIRONMENT MATRIX</b>", width: 12
            }
        }

        /* 5. Command Queue & Pacing Options */
        section("<b>7. Command Queue & Serialization Settings</b>") {
            input name: "queueDelayMs", type: "number", title: "Pacing Delay between queued commands (ms):", defaultValue: 350, range: "100..2000", required: true
            input name: "forceStateReconcile", type: "bool", title: "Bypass State Check (Force send commands even if state matches)", defaultValue: false
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

/* Dynamic Column Resolution: All Active Hub Modes + Sleeping + Away */
private List<String> getActiveColumns() {
    List<String> hubModes = location.modes ? location.modes.collect { it.name } : ["Day", "Evening", "Night"]
    List<String> cols = []
    
    hubModes.each { m -> if (!cols.contains(m)) cols.add(m) }
    if (!cols.contains("Sleeping")) cols.add("Sleeping")
    if (!cols.contains("Away")) cols.add("Away")
    
    return cols
}

/* Query Authoritative Registry From Parent Container Proxy */
private Map getPhysicalEchoDropdownOptions() {
    Map physMap = parent ? parent.getManagedPhysicalEchoDevices() : [:]
    Map options = [:]
    physMap.each { dni, info ->
        options[dni] = "${info.displayName} (${info.family ?: 'ECHO'})"
    }
    return options
}

private List<String> getSelectedDeviceDnis() {
    def raw = settings.selectedEchoDnis
    if (!raw) return []
    return (raw instanceof Collection) ? raw.collect { it.toString() } : [raw.toString()]
}

private List<String> getDisplayCapableEchoDevices(List<String> selectedDnis) {
    if (!selectedDnis) return []
    List<String> displayCapable = []

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (dev) {
            Map caps = dev.getState()?.capabilitiesMap ?: [:]
            Boolean hasDisplay = (caps.displayBrightness == true || dev.hasCommand("setDisplayBrightness"))
            if (hasDisplay) {
                displayCapable.add(dni)
            }
        }
    }
    return displayCapable
}

/* =========================================================================================
   CLIENT-SIDE SCRIPT & STYLESHEET ENGINE (Zero-Reload Editing Matrix)
   ========================================================================================= */

private String buildClientMatrixStylesAndScripts() {
    return """
    <style>
        .matrix-table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 12px; }
        .matrix-table th { background-color: #1A252F; color: #FFFFFF; font-weight: bold; padding: 8px 6px; text-align: center; border: 1px solid #2C3E50; }
        .matrix-table th.dev-col { text-align: left; width: 28%; min-width: 160px; }
        .matrix-table td { border: 1px solid #D5D8DC; padding: 4px; text-align: center; vertical-align: middle; background-color: #FFFFFF; }
        .matrix-table td.dev-name { text-align: left; font-weight: bold; color: #2C3E50; background-color: #F8F9F9; padding-left: 8px; }
        
        .cell-dnd { cursor: pointer; user-select: none; font-weight: bold; font-size: 13px; border-radius: 4px; padding: 4px 0; transition: all 0.15s ease; }
        .cell-dnd.on { background-color: #D4EFDF; color: #1E8449; border: 1px solid #27AE60; }
        .cell-dnd.off { background-color: #F2F4F4; color: #5D6D7E; border: 1px solid #BDC3C7; }

        .cell-mute { cursor: pointer; user-select: none; font-weight: bold; font-size: 11px; border-radius: 4px; padding: 5px 0; transition: all 0.15s ease; }
        .cell-mute.no_change { background-color: #F2F4F4; color: #5D6D7E; border: 1px solid #BDC3C7; }
        .cell-mute.muted { background-color: #FADBD8; color: #900C3F; border: 1px solid #C0392B; }
        .cell-mute.unmuted { background-color: #D4EFDF; color: #1E8449; border: 1px solid #27AE60; }

        .cell-num { cursor: pointer; user-select: none; font-weight: bold; font-size: 12px; border-radius: 4px; padding: 4px 0; background-color: #EBF5FB; color: #2E86C1; border: 1px solid #A9CCE3; transition: all 0.15s ease; }
        .cell-num.empty { background-color: #F2F4F4; color: #7F8C8D; border: 1px solid #BDC3C7; font-weight: normal; }
        .cell-num input { width: 45px; text-align: center; font-weight: bold; font-size: 12px; border: 1px solid #2980B9; border-radius: 3px; padding: 2px; }
    </style>
    <script>
        function toggleDndCell(el, hiddenId) {
            var hidden = document.getElementById(hiddenId);
            if (!hidden) return;
            var isCurrentlyOn = (hidden.value === "true");
            var newState = !isCurrentlyOn;
            hidden.value = newState ? "true" : "false";
            
            if (newState) {
                el.className = "cell-dnd on";
                el.innerHTML = "☑ ON";
            } else {
                el.className = "cell-dnd off";
                el.innerHTML = "☐ OFF";
            }
        }

        function cycleMuteCell(el, hiddenId) {
            var hidden = document.getElementById(hiddenId);
            if (!hidden) return;
            var cur = hidden.value || "no_change";
            var next = "no_change";
            if (cur === "no_change") next = "muted";
            else if (cur === "muted") next = "unmuted";
            else if (cur === "unmuted") next = "no_change";
            
            hidden.value = next;
            
            if (next === "muted") {
                el.className = "cell-mute muted";
                el.innerHTML = "MUTED";
            } else if (next === "unmuted") {
                el.className = "cell-mute unmuted";
                el.innerHTML = "UNMUTED";
            } else {
                el.className = "cell-mute no_change";
                el.innerHTML = "—";
            }
        }

        function editNumberCell(el, hiddenId) {
            if (el.querySelector('input')) return; // Already editing
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

/* =========================================================================================
   CLIENT-SIDE MATRIX TABLE BUILDERS
   ========================================================================================= */

/* 1. DND MATRIX BUILDER */
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
            String cssClass = isOn ? "cell-dnd on" : "cell-dnd off"
            String labelText = isOn ? "☑ ON" : "☐ OFF"
            String hiddenVal = isOn ? "true" : "false"

            sb.append("<td>")
            sb.append("<input type='hidden' id='hid_${settingKey}' name='${settingKey}' value='${hiddenVal}'/>")
            sb.append("<div class='${cssClass}' onclick=\"toggleDndCell(this, 'hid_${settingKey}')\">${labelText}</div>")
            sb.append("</td>")
        }
        sb.append("</tr>")
    }
    sb.append("</table>")
    return sb.toString()
}

/* 2. MUTE MATRIX BUILDER */
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
            String stateVal = settings[settingKey]?.toString() ?: "no_change"
            
            String cssClass = "cell-mute no_change"
            String labelText = "—"

            if (stateVal == "muted") {
                cssClass = "cell-mute muted"
                labelText = "MUTED"
            } else if (stateVal == "unmuted") {
                cssClass = "cell-mute unmuted"
                labelText = "UNMUTED"
            }

            sb.append("<td>")
            sb.append("<input type='hidden' id='hid_${settingKey}' name='${settingKey}' value='${stateVal}'/>")
            sb.append("<div class='${cssClass}' onclick=\"cycleMuteCell(this, 'hid_${settingKey}')\">${labelText}</div>")
            sb.append("</td>")
        }
        sb.append("</tr>")
    }
    sb.append("</table>")
    return sb.toString()
}

/* 3. VOLUME MATRIX BUILDER */
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

/* 4. DISPLAY BRIGHTNESS MATRIX BUILDER */
private String buildDisplayMatrixTable(List<String> displayDnis, List<String> columns) {
    StringBuilder sb = new StringBuilder()
    sb.append("<table class='matrix-table'>")
    sb.append("<tr><th class='dev-col'>Display Device</th>")
    columns.each { col -> sb.append("<th>${col}</th>") }
    sb.append("</tr>")

    displayDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"

        sb.append("<tr>")
        sb.append("<td class='dev-name'>${devLabel}</td>")
        
        columns.each { col ->
            String settingKey = "disp_${dni}_${col}"
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

/* =========================================================================================
   APP BUTTON ACTION HANDLER & SETTINGS PERSISTENCE
   ========================================================================================= */

void appButtonHandler(String btn) {
    if (!btn) return
    logTrace "appButtonHandler() triggered -> Button: [${btn}]"

    if (btn == "btnReconcileNow") {
        logInfo "Manual 'Reconcile Now' requested."
        evaluateCurrentModeState("Manual GUI Reconcile")
    } else if (btn == "btnClearQueue") {
        logWarn "Manual 'Clear Queue' requested. Purging pending commands."
        atomicState.commandQueue = []
    } else if (btn == "btnSaveMatrix") {
        logInfo "<b>'SAVE ENVIRONMENT MATRIX'</b> triggered -> Synchronizing client-side settings..."
        saveMatrixSettingsFromParams()
    }
}

private void saveMatrixSettingsFromParams() {
    Map requestParams = (params != null) ? params : [:]
    
    List<String> selectedDnis = getSelectedDeviceDnis()
    List<String> displayDnis = getDisplayCapableEchoDevices(selectedDnis)
    List<String> columns = getActiveColumns()

    int persistedCount = 0

    selectedDnis.each { dni ->
        columns.each { col ->
            // DND Persistence (Boolean)
            String dndKey = "dnd_${dni}_${col}"
            if (requestParams.containsKey(dndKey)) {
                def dndParam = requestParams[dndKey]
                Boolean dndVal = (dndParam?.toString() == "true")
                app.updateSetting(dndKey, [type: "bool", value: dndVal])
                persistedCount++
            }

            // Mute Persistence (Enum)
            String muteKey = "mute_${dni}_${col}"
            if (requestParams.containsKey(muteKey)) {
                def muteParam = requestParams[muteKey]
                String muteVal = muteParam?.toString() ?: "no_change"
                app.updateSetting(muteKey, [type: "enum", value: muteVal])
                persistedCount++
            }

            // Volume Persistence (Number / Null)
            String volKey = "vol_${dni}_${col}"
            if (requestParams.containsKey(volKey)) {
                def volParam = requestParams[volKey]
                String vStr = volParam?.toString()?.trim() ?: ""
                if (vStr != "" && vStr.isNumber()) {
                    app.updateSetting(volKey, [type: "number", value: vStr.toInteger()])
                } else {
                    app.removeSetting(volKey)
                }
                persistedCount++
            }
        }
    }

    displayDnis.each { dni ->
        columns.each { col ->
            // Display Brightness Persistence (Number / Null)
            String dispKey = "disp_${dni}_${col}"
            if (requestParams.containsKey(dispKey)) {
                def dispParam = requestParams[dispKey]
                String dStr = dispParam?.toString()?.trim() ?: ""
                if (dStr != "" && dStr.isNumber()) {
                    app.updateSetting(dispKey, [type: "number", value: dStr.toInteger()])
                } else {
                    app.removeSetting(dispKey)
                }
                persistedCount++
            }
        }
    }

    logInfo "Matrix Configuration successfully persisted (${persistedCount} field entries synchronized to app settings)."
}

/* UI Status Table */
private String buildStatusTable(List<String> selectedDnis) {
    if (!selectedDnis) return "<i>No physical Echo devices selected.</i>"

    StringBuilder sb = new StringBuilder()
    if (getSettingBool("dryRunMode", false)) {
        sb.append("<div style='background-color:#E74C3C; color:#FFFFFF; padding:6px; font-weight:bold; text-align:center; border-radius:4px; margin-bottom:8px;'>DRY RUN MODE ACTIVE - Proposed changes calculated but suppressed.</div>")
    }

    sb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; text-align:left;'>")
    sb.append("<tr style='background-color:#2C3E50; color:#FFFFFF;'>")
    sb.append("<th style='padding:6px;'>Device Name</th>")
    sb.append("<th style='padding:6px;'>Actual Vol</th>")
    sb.append("<th style='padding:6px;'>Desired Vol</th>")
    sb.append("<th style='padding:6px;'>Display (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>Mute (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>DND (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>Active Mode Column</th>")
    sb.append("<th style='padding:6px;'>Sync Status</th>")
    sb.append("</tr>")

    Map desired = atomicState.desiredState ?: [:]

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"
        Map dMap = desired[dni] ?: [:]

        def actVol = dev ? dev.currentValue("volume") : "N/A"
        def actDisp = dev ? (dev.currentValue("displayBrightness") ?: dev.currentValue("level")) : "N/A"
        def actMute = dev ? dev.currentValue("mute") : "N/A"
        def actDnd = dev ? dev.currentValue("doNotDisturb") : "N/A"

        def desVol = dMap.volume != null ? dMap.volume : "-"
        def desDisp = dMap.displayBrightness != null ? dMap.displayBrightness : "-"
        def desMute = dMap.mute ?: "-"
        def desDnd = dMap.dnd != null ? (dMap.dnd ? "enabled" : "disabled") : "-"
        String sourceReason = dMap.sourceReason ?: "Unassigned"

        Boolean volInSync = (dMap.volume == null || actVol?.toString()?.isNumber() && actVol.toInteger() == dMap.volume?.toInteger())
        Boolean dispInSync = (dMap.displayBrightness == null || actDisp?.toString()?.isNumber() && actDisp.toInteger() == dMap.displayBrightness?.toInteger())
        Boolean muteInSync = (dMap.mute == null || dMap.mute == "no_change" || actMute == dMap.mute)
        Boolean dndInSync = (dMap.dnd == null || (dMap.dnd ? actDnd == "enabled" : actDnd == "disabled"))

        Boolean fullySynced = volInSync && dispInSync && muteInSync && dndInSync
        String statusBadge = fullySynced ? "<span style='color:#27AE60; font-weight:bold;'>✔ Synced</span>" : "<span style='color:#E67E22; font-weight:bold;'>⚠ Pending</span>"

        sb.append("<tr style='border-bottom:1px solid #E0E0E0;'>")
        sb.append("<td style='padding:6px;'><b>${devLabel}</b></td>")
        sb.append("<td style='padding:6px;'>${actVol}</td>")
        sb.append("<td style='padding:6px;'>${desVol}</td>")
        sb.append("<td style='padding:6px;'>${actDisp} / ${desDisp}</td>")
        sb.append("<td style='padding:6px;'>${actMute} / ${desMute}</td>")
        sb.append("<td style='padding:6px;'>${actDnd} / ${desDnd}</td>")
        sb.append("<td style='padding:6px; font-size:11px; color:#34495E;'><b>${sourceReason}</b></td>")
        sb.append("<td style='padding:6px;'>${statusBadge}</td>")
        sb.append("</tr>")
    }

    sb.append("</table>")
    
    List queue = atomicState.commandQueue ?: []
    sb.append("<div style='margin-top:6px; font-size:11px; color:#7F8C8D;'>Pending Command Queue: <b>${queue.size()} items</b></div>")
    return sb.toString()
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
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
    logInfo "Subscribed to Location Mode changes."

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

/* Event Handlers */
void modeChangeHandler(evt) {
    logInfo "Location mode change event detected -> ${evt.value}"
    evaluateCurrentModeState("Mode Changed to [${evt.value}]")
}

private void evaluateCurrentModeState(String triggerReason) {
    String activeCol = location.mode
    evaluateMatrixForColumn(activeCol, triggerReason)
}

/* Matrix Evaluation & Declarative Reconciliation Core */
public void evaluateMatrixForColumn(String columnName, String sourceReason) {
    if (!columnName) return
    logInfo "Evaluating Matrix Targets for Column [${columnName}] | Trigger: ${sourceReason}"

    Map currentDesired = atomicState.desiredState ?: [:]
    Boolean forceDispatch = getSettingBool("forceStateReconcile", false)
    List<String> selectedDnis = getSelectedDeviceDnis()

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (!dev) return

        Map dMap = currentDesired[dni] ?: [:]

        // Retrieve setting values mapped to this specific device DNI and active column
        def targetVol  = settings["vol_${dni}_${columnName}"]
        def targetDisp = settings["disp_${dni}_${columnName}"]
        String targetMute = settings["mute_${dni}_${columnName}"] ?: "no_change"
        
        // DND Boolean logic: True = On, False = Off
        Boolean targetDndBool = (settings["dnd_${dni}_${columnName}"] == true || settings["dnd_${dni}_${columnName}"]?.toString() == "true")

        if (targetVol != null && targetVol.toString().trim() != "") {
            dMap.volume = targetVol.toInteger()
        }
        if (targetDisp != null && targetDisp.toString().trim() != "") {
            dMap.displayBrightness = targetDisp.toInteger()
        }
        if (targetMute != "no_change") dMap.mute = targetMute
        dMap.dnd = targetDndBool
        dMap.sourceReason = "${sourceReason} → [Column: ${columnName}]"

        currentDesired[dni] = dMap

        // Reconciliation Engine
        if (targetVol != null && targetVol.toString().trim() != "" && (forceDispatch || dev.currentValue("volume")?.toInteger() != targetVol.toInteger())) {
            queueCommand(dni, "setVolume", targetVol.toInteger(), dMap.sourceReason)
        }

        if (targetDisp != null && targetDisp.toString().trim() != "" && dev.hasCommand("setDisplayBrightness")) {
            def curDisp = dev.currentValue("displayBrightness") ?: dev.currentValue("level")
            if (forceDispatch || curDisp?.toInteger() != targetDisp.toInteger()) {
                queueCommand(dni, "setDisplayBrightness", targetDisp.toInteger(), dMap.sourceReason)
            }
        }
        
        if (targetMute != "no_change" && (forceDispatch || dev.currentValue("mute") != targetMute)) {
            String cmd = (targetMute == "muted") ? "mute" : "unmute"
            queueCommand(dni, cmd, null, dMap.sourceReason)
        }
        
        String currentDndState = dev.currentValue("doNotDisturb")
        Boolean isDndActive = (currentDndState == "enabled")
        
        if (forceDispatch || isDndActive != targetDndBool) {
            String cmd = targetDndBool ? "setDoNotDisturbOn" : "setDoNotDisturbOff"
            queueCommand(dni, cmd, null, dMap.sourceReason)
        }
    }

    atomicState.desiredState = currentDesired
    processQueue()
}

/* Command Queue Engine */
private void queueCommand(String dni, String command, Object param, String reason) {
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
        queuedAt: now()
    ])
    atomicState.commandQueue = queue
    logDebug "Queued command [${command}] for device DNI [${dni}]. Queue Depth: ${queue.size()}"
}

void processQueue() {
    List queue = atomicState.commandQueue ?: []
    if (!queue || queue.size() == 0) {
        logDebug "Command queue empty. Reconciliation complete."
        return
    }

    Map item = queue.remove(0)
    atomicState.commandQueue = queue

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