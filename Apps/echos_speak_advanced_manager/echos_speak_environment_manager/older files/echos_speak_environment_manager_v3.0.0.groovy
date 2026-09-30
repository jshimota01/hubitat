/**
 * Application Name: Echos Speak Environment Manager
 * Platform: Hubitat Elevation (C-7 / OS 2.4.4.156)
 * Notes: Centralized environmental policy profiles, state reconciliation, volume management, DND, and mute control for Echo Speaks devices with command queuing.
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
 *  Provides a central policy management, state reconciliation, and command pacing layer over physical Echo Speaks devices.
 *  Decouples event triggers from policy profiles and tracks command ownership/source reasons to eliminate HTTP timeouts
 *  and provide clear visibility into device target states.
 *
 *  Changelog:
 *  v3.0.0    09/26/26    jshimota    Renamed app to Echos Speak Environment Manager. Bound as child app to Echos Speak Advanced Manager. Removed SpeechSynthesis capability selector; device list and command targets are now queried directly from top-level parent proxy.
 *  v1.4.1    09/26/26    jshimota    Session version demarcation update and clean SpeechSynthesis device selection with driver-type filtering.
 *  v1.4.0    09/26/26    jshimota    Cleaned device selection architecture using standard SpeechSynthesis capability selector combined with driver-type filtering in getManagedEchoDevices(); removed invalid getDeviceById calls.
 *  v1.3.1    09/26/26    jshimota    Fixed Hubitat API NullPointerException/MissingMethodException by replacing invalid app.getHub() calls with native location/getDeviceById calls in device registry helpers.
 *  v1.3.0    09/26/26    jshimota    Replaced capability.musicPlayer selector with dynamic enum discovering Echos Speak Advanced physical child devices; added auto-filtering for WHA family devices; fixed DND method target names (setDoNotDisturbOn/Off); added getManagedEchoDevices() registry abstraction helper.
 *  v1.2.0    09/26/26    jshimota    Decoupled Policy Engine from Trigger Engine; added Command Source/Reason tracking to state matrix and execution audits.
 *  v1.1.0    09/26/26    jshimota    Added Dry Run mode, Reconcile Now and Clear Queue GUI button triggers, and detailed per-device command execution audit logging in state matrix.
 *  v1.0.0    09/26/26    jshimota    Initial release of Echos Speak Volumes Manager.
 **/

static String version() { return '3.0.0' }
def timeStamp() { return "2026/09/26 12:00 PM" }

definition(
    name: "Echos Speak Environment Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Centralized policy profiles, state matrix reconciliation, and command queueing for physical Echo devices.",
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

def mainPage() {
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
                          "⚠️ <b>No Physical Echo Devices Discovered:</b> Ensure Echos Speak Advanced is configured and managing physical devices under the top-level Manager.</div>"
            }
        }

        /* Diagnostics & Control Panel */
        section("<b>Diagnostics & Control Panel</b>") {
            input name: "dryRunMode", type: "bool", title: "Enable Dry Run Mode (Calculate & log proposed changes without sending commands)", defaultValue: false, submitOnChange: true
            input name: "btnReconcileNow", type: "button", title: "Reconcile Now (Trigger Policy Sync)", width: 6
            input name: "btnClearQueue", type: "button", title: "Clear Command Queue", width: 6
        }

        /* 2. Live State Matrix */
        List<String> selectedDnis = getSelectedDeviceDnis()
        if (selectedDnis) {
            section("<b>2. Live Device State & Policy Matrix</b>") {
                paragraph buildStatusTable(selectedDnis)
            }
        }

        /* 3. Policy Engine Profiles */
        section("<b>3. Policy Engine (Define Profile Parameters)</b>") {
            paragraph "<div style='color:#555; font-size:12px;'>Configure desired volume, mute, and DND states for each reusable policy profile.</div>"
            List<String> profiles = ["Normal", "Night", "Sleeping", "Away", "Movie", "Quiet"]
            profiles.each { prof ->
                paragraph "<b style='color:#2C3E50;'>Policy Profile: ${prof}</b>"
                input name: "pol_vol_${prof}", type: "number", title: "Target Volume (0-100) [${prof}]:", range: "0..100", required: false, width: 4
                input name: "pol_mute_${prof}", type: "enum", title: "Target Mute [${prof}]:", options: ["no_change": "No Change", "muted": "Muted", "unmuted": "Unmuted"], defaultValue: "no_change", width: 4
                input name: "pol_dnd_${prof}", type: "enum", title: "Target DND [${prof}]:", options: ["no_change": "No Change", "on": "DND On", "off": "DND Off"], defaultValue: "no_change", width: 4
                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0;'/>"
            }
        }

        /* 4. Trigger Engine Mapping */
        section("<b>4. Trigger Engine (Map Triggers to Policies)</b>") {
            input name: "enableModeTriggers", type: "bool", title: "Enable Mode Triggers", defaultValue: true, submitOnChange: true
            if (enableModeTriggers) {
                input name: "monitoredModes", type: "mode", title: "Select Monitored Modes:", multiple: true, required: true, submitOnChange: true
                if (monitoredModes) {
                    monitoredModes.each { mName ->
                        input name: "mode_policy_${mName}", type: "enum", title: "Policy for Mode [${mName}]:", options: ["Normal", "Night", "Sleeping", "Away", "Movie", "Quiet"], required: true
                    }
                }
            }

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "enablePresenceTriggers", type: "bool", title: "Enable Presence Sensor Triggers", defaultValue: false, submitOnChange: true
            if (enablePresenceTriggers) {
                input name: "presenceSensors", type: "capability.presenceSensor", title: "Select Presence Sensors:", multiple: true, required: true, submitOnChange: true
                input name: "presencePresentPolicy", type: "enum", title: "Policy when Present:", options: ["Normal", "Night", "Sleeping", "Away", "Movie", "Quiet"], required: true
                input name: "presenceAwayPolicy", type: "enum", title: "Policy when Away:", options: ["Normal", "Night", "Sleeping", "Away", "Movie", "Quiet"], required: true
            }
        }

        /* 5. Command Queue & Pacing Options */
        section("<b>5. Command Queue & Serialization Settings</b>") {
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

/* UI Table Builder with Ownership/Source Reason Visibility */
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
    sb.append("<th style='padding:6px;'>Mute (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>DND (Act/Des)</th>")
    sb.append("<th style='padding:6px;'>Active Source / Policy</th>")
    sb.append("<th style='padding:6px;'>Sync Status</th>")
    sb.append("</tr>")

    Map desired = atomicState.desiredState ?: [:]

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        String devLabel = dev ? dev.displayName : "Device [${dni}]"
        Map dMap = desired[dni] ?: [:]

        def actVol = dev ? dev.currentValue("volume") : "N/A"
        def actMute = dev ? dev.currentValue("mute") : "N/A"
        def actDnd = dev ? dev.currentValue("doNotDisturb") : "N/A"

        def desVol = dMap.volume != null ? dMap.volume : "-"
        def desMute = dMap.mute ?: "-"
        def desDnd = dMap.dnd ?: "-"
        String sourceReason = dMap.sourceReason ?: "Unassigned"

        Boolean volInSync = (dMap.volume == null || actVol?.toString()?.isNumber() && actVol.toInteger() == dMap.volume?.toInteger())
        Boolean muteInSync = (dMap.mute == null || dMap.mute == "no_change" || actMute == dMap.mute)
        Boolean dndInSync = (dMap.dnd == null || dMap.dnd == "no_change" || actDnd == dMap.dnd)

        Boolean fullySynced = volInSync && muteInSync && dndInSync
        String statusBadge = fullySynced ? "<span style='color:#27AE60; font-weight:bold;'>✔ Synced</span>" : "<span style='color:#E67E22; font-weight:bold;'>⚠ Pending</span>"

        sb.append("<tr style='border-bottom:1px solid #E0E0E0;'>")
        sb.append("<td style='padding:6px;'><b>${devLabel}</b></td>")
        sb.append("<td style='padding:6px;'>${actVol}</td>")
        sb.append("<td style='padding:6px;'>${desVol}</td>")
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

/* GUI Button Action Handler */
void appButtonHandler(String btn) {
    switch(btn) {
        case "btnReconcileNow":
            logInfo "Manual 'Reconcile Now' requested."
            if (enableModeTriggers && location.mode in monitoredModes) {
                String assignedPolicy = settings["mode_policy_${location.mode}"]
                evaluatePolicy(assignedPolicy, "Manual GUI Reconcile (Mode: ${location.mode})")
            } else {
                evaluatePolicy("Normal", "Manual GUI Reconcile (Fallback Default)")
            }
            break

        case "btnClearQueue":
            logWarn "Manual 'Clear Queue' requested. Purging pending commands."
            atomicState.commandQueue = []
            break
    }
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
    unschedule()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    updateAppLabel()

    if (enableModeTriggers) {
        subscribe(location, "mode", modeChangeHandler)
        logInfo "Subscribed to Location Mode changes."
    }

    if (enablePresenceTriggers && presenceSensors) {
        subscribe(presenceSensors, "presence", presenceChangeHandler)
        logInfo "Subscribed to Presence Sensor events."
    }

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        unschedule("disableDebugLogging")
        runIn(1800, "disableDebugLogging", [overwrite: false])
    }

    if (enableModeTriggers && location.mode in monitoredModes) {
        String assignedPolicy = settings["mode_policy_${location.mode}"]
        evaluatePolicy(assignedPolicy, "App Initialized (Mode: ${location.mode})")
    }
}

/* Event Handlers */
void modeChangeHandler(evt) {
    logInfo "Trigger Engine: Mode change event detected -> ${evt.value}"
    if (enableModeTriggers && evt.value in monitoredModes) {
        String assignedPolicy = settings["mode_policy_${evt.value}"]
        evaluatePolicy(assignedPolicy, "Trigger: Mode Changed to [${evt.value}]")
    }
}

void presenceChangeHandler(evt) {
    logInfo "Trigger Engine: Presence change event -> ${evt.device.displayName} is ${evt.value}"
    if (!enablePresenceTriggers) return

    Boolean anyPresent = presenceSensors.any { it.currentValue("presence") == "present" }
    String assignedPolicy = anyPresent ? presencePresentPolicy : presenceAwayPolicy
    evaluatePolicy(assignedPolicy, "Trigger: Presence State Changed (${anyPresent ? 'Present' : 'Away'})")
}

/* Policy Engine & Reconciliation Core */
public void evaluatePolicy(String policyName, String sourceReason) {
    if (!policyName) return
    logInfo "Policy Engine: Evaluating Profile [${policyName}] | Source: ${sourceReason}"

    def targetVol = settings["pol_vol_${policyName}"]
    String targetMute = settings["pol_mute_${policyName}"] ?: "no_change"
    String targetDnd = settings["pol_dnd_${policyName}"] ?: "no_change"

    Map currentDesired = atomicState.desiredState ?: [:]
    Boolean forceDispatch = getSettingBool("forceStateReconcile", false)
    List<String> selectedDnis = getSelectedDeviceDnis()

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (!dev) return

        Map dMap = currentDesired[dni] ?: [:]

        if (targetVol != null) dMap.volume = targetVol.toInteger()
        if (targetMute != "no_change") dMap.mute = targetMute
        if (targetDnd != "no_change") dMap.dnd = targetDnd
        dMap.sourceReason = "${sourceReason} → [${policyName}]"

        currentDesired[dni] = dMap

        // Reconciliation Engine: Difference Detection & Duplicate Suppression
        if (targetVol != null && (forceDispatch || dev.currentValue("volume")?.toInteger() != targetVol.toInteger())) {
            queueCommand(dni, "setVolume", targetVol.toInteger(), dMap.sourceReason)
        }
        if (targetMute != "no_change" && (forceDispatch || dev.currentValue("mute") != targetMute)) {
            String cmd = (targetMute == "muted") ? "mute" : "unmute"
            queueCommand(dni, cmd, null, dMap.sourceReason)
        }
        if (targetDnd != "no_change" && (forceDispatch || dev.currentValue("doNotDisturb") != targetDnd)) {
            String cmd = (targetDnd == "on") ? "setDoNotDisturbOn" : "setDoNotDisturbOff"
            queueCommand(dni, cmd, null, dMap.sourceReason)
        }
    }

    atomicState.desiredState = currentDesired
    processQueue()
}

/* Command Queue & Throttle Processor Engine */
private void queueCommand(String dni, String command, Object param, String reason) {
    if (getSettingBool("dryRunMode", false)) {
        logInfo "<b>[DRY RUN ACTIVE]</b> Command calculated & suppressed -> Device DNI: ${dni} | Cmd: ${command} | Arg: ${param} | Reason: ${reason}"
        return
    }

    List queue = atomicState.commandQueue ?: []
    
    // Command Coalescing: Drop previous unexecuted commands of the same type for this device DNI
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