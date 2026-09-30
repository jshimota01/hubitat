/**
 * Application Name: Echos Speak Volumes Manager
 * Platform: Hubitat Elevation (C-7 / OS 2.4.4.156)
 * Notes: Centralized state reconciliation, volume management, DND, and mute control for Echo Speaks devices with command queuing.
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
 *  Provides a central management, reconciliation, and command pacing layer over Echo Speaks parent/child devices.
 *  Prevents command storms and HTTP timeouts on Synology NAS Echo Speaks local servers by evaluating current vs.
 *  desired device state and executing commands through a serialized, rate-limited queue.
 *
 *  Instructions:
 *  1. Install app in Hubitat Code Management -> Apps Code.
 *  2. Add user instance in Apps.
 *  3. Select your Echo Speaks devices (child devices created by Echo Speaks parent).
 *  4. Choose Mode and/or Presence triggers and define target Volume, Mute, and DND states for each state.
 *  5. Adjust Queue Throttle Delay (default 350ms) as needed based on server performance.
 *  
 *  Changelog:
 *  v1.0.0    09/26/26    jshimota    Initial release of Echos Speak Volumes Manager.
 **/

static String version() { return '1.0.0' }
def timeStamp() { return "2026/09/26 09:46 AM" }

definition(
    name: "Echos Speak Volumes Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Centralized state matrix, volume, DND, and mute orchestration with command queueing for Echo Speaks devices.",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/echos_speak_volumes_manager/echos_speak_volumes_manager.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* Styled App Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Echos Speak Volumes Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        /* Device Selection */
        section("<b>Echo Speaks Device Selection</b>") {
            input name: "echoDevices", type: "capability.musicPlayer", title: "Select Echo Speaks Devices to Manage:", multiple: true, required: true, submitOnChange: true
        }

        /* Live Desired vs Actual State Table */
        if (echoDevices) {
            section("<b>Live Device State Matrix</b>") {
                paragraph buildStatusTable()
            }
        }

        /* Trigger Engine Configuration */
        section("<b>Trigger Configuration (Modes & Presence)</b>") {
            input name: "enableModeTriggers", type: "bool", title: "Enable Hubitat Mode Triggers", defaultValue: true, submitOnChange: true
            if (enableModeTriggers) {
                input name: "monitoredModes", type: "mode", title: "Select Modes to Act On:", multiple: true, required: true, submitOnChange: true
            }

            input name: "enablePresenceTriggers", type: "bool", title: "Enable Presence Sensor Triggers", defaultValue: false, submitOnChange: true
            if (enablePresenceTriggers) {
                input name: "presenceSensors", type: "capability.presenceSensor", title: "Select Presence Sensors:", multiple: true, required: true, submitOnChange: true
                input name: "presencePresentPreset", type: "enum", title: "Preset to apply when Present:", options: getDefinedPresets(), required: true
                input name: "presenceAwayPreset", type: "enum", title: "Preset to apply when Away:", options: getDefinedPresets(), required: true
            }
        }

        /* Mode Configuration Maps */
        if (enableModeTriggers && monitoredModes) {
            section("<b>Mode Preset Rules</b>") {
                monitoredModes.each { mName ->
                    paragraph "<b style='color:#2C3E50;'>Mode: ${mName}</b>"
                    input name: "mode_vol_${mName}", type: "number", title: "Target Volume (0-100) for [${mName}]:", range: "0..100", required: false
                    input name: "mode_mute_${mName}", type: "enum", title: "Target Mute for [${mName}]:", options: ["no_change": "No Change", "muted": "Muted", "unmuted": "Unmuted"], defaultValue: "no_change"
                    input name: "mode_dnd_${mName}", type: "enum", title: "Target DND for [${mName}]:", options: ["no_change": "No Change", "on": "DND On", "off": "DND Off"], defaultValue: "no_change"
                    paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:4px 0;'/>"
                }
            }
        }

        /* Queue & Performance Settings */
        section("<b>Command Queue & Pacing Options</b>") {
            input name: "queueDelayMs", type: "number", title: "Delay between queued HTTP commands (milliseconds):", defaultValue: 350, range: "100..2000", required: true
            input name: "forceStateReconcile", type: "bool", title: "Force command dispatch even if device reports desired state (Bypass State Check)", defaultValue: false
        }

        /* Collapsible App Preferences & Logging Options */
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

private List<String> getDefinedPresets() {
    return ["Quiet", "Normal", "Loud", "Night", "Mute All"]
}

/* UI Table Builder */
private String buildStatusTable() {
    if (!echoDevices) return "<i>No Echo devices configured.</i>"

    StringBuilder sb = new StringBuilder()
    sb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; text-align:left;'>")
    sb.append("<tr style='background-color:#2C3E50; color:#FFFFFF;'>")
    sb.append("<th style='padding:6px;'>Device Name</th>")
    sb.append("<th style='padding:6px;'>Actual Vol</th>")
    sb.append("<th style='padding:6px;'>Desired Vol</th>")
    sb.append("<th style='padding:6px;'>Mute State</th>")
    sb.append("<th style='padding:6px;'>DND State</th>")
    sb.append("<th style='padding:6px;'>Sync Status</th>")
    sb.append("</tr>")

    Map desired = atomicState.desiredState ?: [:]

    echoDevices.each { dev ->
        String dId = dev.id.toString()
        Map dMap = desired[dId] ?: [:]
        
        def actVol = dev.currentValue("volume")
        def actMute = dev.currentValue("mute")
        def actDnd = dev.currentValue("doNotDisturb")

        def desVol = dMap.volume != null ? dMap.volume : "-"
        def desMute = dMap.mute ?: "-"
        def desDnd = dMap.dnd ?: "-"

        Boolean volInSync = (dMap.volume == null || actVol?.toInteger() == dMap.volume?.toInteger())
        Boolean muteInSync = (dMap.mute == null || dMap.mute == "no_change" || actMute == dMap.mute)
        Boolean dndInSync = (dMap.dnd == null || dMap.dnd == "no_change" || actDnd == dMap.dnd)

        Boolean fullySynced = volInSync && muteInSync && dndInSync
        String statusBadge = fullySynced ? "<span style='color:#27AE60; font-weight:bold;'>✔ Synced</span>" : "<span style='color:#E67E22; font-weight:bold;'>⚠ Pending/Mismatch</span>"

        sb.append("<tr style='border-bottom:1px solid #E0E0E0;'>")
        sb.append("<td style='padding:6px;'><b>${dev.displayName}</b></td>")
        sb.append("<td style='padding:6px;'>${actVol != null ? actVol : 'N/A'}</td>")
        sb.append("<td style='padding:6px;'>${desVol}</td>")
        sb.append("<td style='padding:6px;'>Act: ${actMute} | Des: ${desMute}</td>")
        sb.append("<td style='padding:6px;'>Act: ${actDnd} | Des: ${desDnd}</td>")
        sb.append("<td style='padding:6px;'>${statusBadge}</td>")
        sb.append("</tr>")
    }

    sb.append("</table>")
    
    List queue = atomicState.commandQueue ?: []
    sb.append("<div style='margin-top:6px; font-size:11px; color:#7F8C8D;'>Pending Queue Depth: <b>${queue.size()} commands</b></div>")
    return sb.toString()
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Echos Speak Volumes Manager"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

// Settings Hash Snapshot Helper
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

// Hubitat App Lifecycle Routines
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
        logInfo "Settings or code version modification detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(false)
    } else {
        logDebug "App closed without setting or version changes. Skipping re-initialization."
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
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }

    // Evaluate state on startup
    if (enableModeTriggers && location.mode in monitoredModes) {
        evaluateModeState(location.mode)
    }
}

/* Event Handlers */
void modeChangeHandler(evt) {
    logInfo "Mode change event detected: ${evt.value}"
    if (enableModeTriggers && evt.value in monitoredModes) {
        evaluateModeState(evt.value)
    }
}

void presenceChangeHandler(evt) {
    logInfo "Presence change event: ${evt.device.displayName} is ${evt.value}"
    if (!enablePresenceTriggers) return

    Boolean anyPresent = presenceSensors.any { it.currentValue("presence") == "present" }
    String targetPreset = anyPresent ? presencePresentPreset : presenceAwayPreset
    logInfo "Evaluated overall presence: ${anyPresent ? 'Present' : 'Away'}. Applying Preset: ${targetPreset}"
    applyPreset(targetPreset)
}

/* State Evaluation & Command Generation Engine */
private void evaluateModeState(String modeName) {
    def targetVol = settings["mode_vol_${modeName}"]
    String targetMute = settings["mode_mute_${modeName}"] ?: "no_change"
    String targetDnd = settings["mode_dnd_${modeName}"] ?: "no_change"

    logInfo "Evaluating Mode [${modeName}] targets -> Volume: ${targetVol}, Mute: ${targetMute}, DND: ${targetDnd}"

    Map currentDesired = atomicState.desiredState ?: [:]
    Boolean forceDispatch = getSettingBool("forceStateReconcile", false)

    echoDevices.each { dev ->
        String dId = dev.id.toString()
        Map dMap = currentDesired[dId] ?: [:]

        if (targetVol != null) dMap.volume = targetVol.toInteger()
        if (targetMute != "no_change") dMap.mute = targetMute
        if (targetDnd != "no_change") dMap.dnd = targetDnd

        currentDesired[dId] = dMap

        // Deduplication & Target Checking
        if (targetVol != null && (forceDispatch || dev.currentValue("volume")?.toInteger() != targetVol.toInteger())) {
            queueCommand(dId, "setVolume", targetVol.toInteger())
        }
        if (targetMute != "no_change" && (forceDispatch || dev.currentValue("mute") != targetMute)) {
            String cmd = (targetMute == "muted") ? "mute" : "unmute"
            queueCommand(dId, cmd, null)
        }
        if (targetDnd != "no_change" && (forceDispatch || dev.currentValue("doNotDisturb") != targetDnd)) {
            String cmd = (targetDnd == "on") ? "setDoNotDisturb" : "setDoNotDisturb"
            Boolean param = (targetDnd == "on")
            queueCommand(dId, cmd, param)
        }
    }

    atomicState.desiredState = currentDesired
    triggerQueueProcessor()
}

public void applyPreset(String presetName) {
    logInfo "Applying explicit preset: ${presetName}"
    Integer targetVol = null
    String targetMute = "no_change"
    String targetDnd = "no_change"

    switch(presetName) {
        case "Quiet":
            targetVol = 10; targetMute = "unmuted"; targetDnd = "off"; break
        case "Normal":
            targetVol = 30; targetMute = "unmuted"; targetDnd = "off"; break
        case "Loud":
            targetVol = 60; targetMute = "unmuted"; targetDnd = "off"; break
        case "Night":
            targetVol = 5; targetMute = "unmuted"; targetDnd = "on"; break
        case "Mute All":
            targetMute = "muted"; break
    }

    Map currentDesired = atomicState.desiredState ?: [:]

    echoDevices.each { dev ->
        String dId = dev.id.toString()
        Map dMap = currentDesired[dId] ?: [:]

        if (targetVol != null) {
            dMap.volume = targetVol
            queueCommand(dId, "setVolume", targetVol)
        }
        if (targetMute != "no_change") {
            dMap.mute = targetMute
            queueCommand(dId, (targetMute == "muted" ? "mute" : "unmute"), null)
        }
        if (targetDnd != "no_change") {
            dMap.dnd = targetDnd
            queueCommand(dId, "setDoNotDisturb", (targetDnd == "on"))
        }

        currentDesired[dId] = dMap
    }

    atomicState.desiredState = currentDesired
    triggerQueueProcessor()
}

/* Command Queue & Throttle Processor Engine */
private void queueCommand(String deviceId, String command, Object param) {
    List queue = atomicState.commandQueue ?: []
    
    // Command Coalescing: Drop previous unexecuted commands of the same type for this device
    queue.removeAll { it.deviceId == deviceId && it.command == command }

    queue.add([deviceId: deviceId, command: command, param: param])
    atomicState.commandQueue = queue
    logDebug "Queued command [${command}] for device ID [${deviceId}]. New Queue Depth: ${queue.size()}"
}

private void triggerQueueProcessor() {
    processQueue()
}

void processQueue() {
    List queue = atomicState.commandQueue ?: []
    if (!queue || queue.size() == 0) {
        logDebug "Command queue empty. Reconciliation complete."
        return
    }

    Map item = queue.remove(0)
    atomicState.commandQueue = queue

    def dev = echoDevices.find { it.id.toString() == item.deviceId }
    if (dev) {
        try {
            logInfo "Executing Queued Command -> Device: ${dev.displayName} | Cmd: ${item.command} | Arg: ${item.param}"
            if (item.param != null) {
                dev."${item.command}"(item.param)
            } else {
                dev."${item.command}"()
            }
        } catch (Exception e) {
            logError "Failed to execute command ${item.command} on device ${dev.displayName}: ${e.message}"
        }
    } else {
        logWarn "Target device ID ${item.deviceId} no longer found in configured device list."
    }

    if (atomicState.commandQueue.size() > 0) {
        Integer delay = settings["queueDelayMs"] ? settings["queueDelayMs"].toInteger() : 350
        runInMillis(delay, "processQueue", [overwrite: true])
    }
}

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// Centralized Logging Engine
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