/**
 * Echos Speak Advanced Device Driver
 * Platform: Hubitat Elevation
 * Category: Convenient Architecture
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
 *  Child Device Driver for managing individual Amazon Echo devices bridged via Echos Speak Advanced Parent App.
 *
 *  Changelog:
 *  v1.5.0 (2026-09-15) - Template Alignment & Driver Maintenance Refactor:
 *                        - Purged obsolete custom Health Check scheduling and attributes.
 *                        - Integrated single-shot version demarcation logging helper to track version in state.driverVersion.
 *                        - Added resetDriver(), clearAllDriverStates(), clearAllAttributes(), and clearAllSchedules() for GUI maintenance.
 *  v1.4.3 (2026-09-15) - Health Check Method Signature Fix:
 *                        - Added zero-argument executeHealthCheckScheduled() method signature to resolve missing method exception during scheduled health checks.
 *  v1.4.2 (2026-09-14) - Volume 0 + Saved State Mute/Unmute Implementation:
 *                        - Refactored mute() to save current volume to state.preMuteVolume before dispatching server mute (volume 0).
 *                        - Refactored unmute() to retrieve state.preMuteVolume and pass { level: savedVolume } payload to server unmute.
 *  v1.4.1 (2026-09-14) - Restored Complete Driver Historical Changelog:
 *                        - Restored all historical changelog entries back through v1.0.0.
 *  v1.4.0 (2026-09-14) - Mute, DND & Voice Command Payload Refactor:
 *                        - Explicitly mapped mute() and unmute() to dispatch 'mute' command with boolean state.
 *                        - Mapped doNotDisturbOn() and doNotDisturbOff() to dispatch 'dnd' command with boolean state.
 *                        - Cleaned up voiceCmdAsText(text) command signature.
 *  v1.3.0 (2026-09-14) - Extended Command Suite Implementation:
 *                        - Added capability 'AudioVolume' to register standard volume/mute attributes.
 *                        - Added native mute() and unmute() command implementations.
 *                        - Added doNotDisturbOn() and doNotDisturbOff() convenience commands.
 *                        - Added voiceCmdAsText(text) command to execute text-based voice command sequences.
 *  v1.2.0 (2026-09-12) - Backend Contract Alignment:
 *                        - Aligned sendBridgeCommand dispatch cycles to strict POST /api/command contract.
 *                        - Mapped media player controls (play, pause, stop, nextTrack, previousTrack) to backend dispatcher.
 *                        - Updated volume commands to interface directly with sendSequenceCommand('volume', level) backend logic.
 *  v1.1.4 (2026-09-09) - Capability & Handler Alignment:
 *                        - Added trace log version banner demarcation on driver configuration updates.
 *                        - Added SwitchLevel capability and setLevel() signatures to satisfy MusicPlayer/SwitchLevel integrations.
 *                        - Safe-guarded restoreVolumeAfterSpeech map parsing to prevent NPEs during delayed callbacks.
 *  v1.1.3 (2026-09-09) - Hubitat Platform Schema Alignment:
 *                        - Removed invalid capability "VolumeControl" to resolve Hubitat metadata import error.
 *  v1.1.2 (2026-09-09) - Speech & Restore Sequence Timing Fix:
 *                        - Resolved premature volume restore truncating speech playback by introducing runIn() delayed restore callbacks.
 *                        - Added word-count based speech duration estimation to prevent clipping TTS audio.
 *  v1.1.1 (2026-09-09) - Command Parameter Normalization:
 *                        - Enforced strict integer casting on setVolume() to align with bridge API sequence requirements.
 *  v1.1.0 (2026-09-08) - Initial deployment of Echos Speak Advanced Child Device Driver.
 *  v1.1.0    09/09/26    jshimota    Audited all metadata command parameters to remove strict required field flags and added null/empty safety guards.
 *  v1.0.9    09/09/26    jshimota    Removed duplicate setLevel command definition and updated default IP/MAC attributes to 'Masked by Amazon'.
 *  v1.0.8    09/09/26    jshimota    Added SwitchLevel capability, setLevel() signatures, multi-type command guards, and bd() BigDecimal helper.
 *  v1.0.7    09/09/26    jshimota    Standardized trace log version update banner demarcation on driver state updates.
 *  v1.0.6    09/09/26    jshimota    Refactored parseDeviceData() mapping to enforce synchronized state updates across all extended bridge attributes.
 *  v1.0.5    09/09/26    jshimota    Expanded attribute suite: added alarmVolume, deviceIcon HTML tile, firmwareVer, followUpMode, lastAnnouncement, lastUpdated, lastVoiceActivity, permissions, and phraseSpoken.
 *  v1.0.4    09/09/26    jshimota    Added full command suite for announcements, raw commands, alarms, reminders, and relative volume adjustment.
 *  v1.0.3    09/08/26    jshimota    Standardized naming references to EchosSpeak and updated metadata display.
 *  v1.0.2    09/08/26    jshimota    Added multi-signature speak(), playTextAndRestore(), missing speech attributes, and health check response handlers.
 *  v1.0.1    09/08/26    jshimota    Relocated @Field constants above preferences block to resolve compilation error on initial import.
 *  v1.0.0    09/08/26    jshimota    Initial release of EchosSpeak Advanced Device (Custom) based on standardized driver template.
 **/

import groovy.transform.Field

static String version() { return '1.5.0' }
def timeStamp() { return "2026/09/15 09:20 AM" }

metadata {
    definition (
        name: "Echos Speak Advanced Device (Custom)",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/echos_speak_advanced_device/echos_speak_advanced_device.groovy"
    ) {
        capability "SpeechSynthesis"
        capability "AudioNotification"
        capability "MusicPlayer"
        capability "SwitchLevel"
        capability "AudioVolume"
        capability "Refresh"
        capability "Sensor"
        capability "Actuator"

        // CUSTOM COMMANDS
        command "setVolume", [[name: "volumeLevel*", type: "NUMBER", description: "Set speaker volume level (0-100)"]]
        command "volumeUp"
        command "volumeDown"
        command "mute"
        command "unmute"
        command "playTextAndRestore", [
            [name: "text*", type: "STRING", description: "Text to speak out loud"],
            [name: "speakVolume", type: "NUMBER", description: "Temporary volume (0-100)"]
        ]
        command "playAnnouncement", [[name: "text*", type: "STRING", description: "Play text as an Alexa Announcement chime"]]
        command "playAnnouncementAndRestore", [
            [name: "text*", type: "STRING", description: "Announcement text to speak"],
            [name: "announceVolume", type: "NUMBER", description: "Temporary volume (0-100)"]
        ]
        command "setDoNotDisturb", [[name: "enabled*", type: "ENUM", constraints: ["true", "false"], description: "Toggle Do Not Disturb Mode"]]
        command "doNotDisturbOn"
        command "doNotDisturbOff"
        command "voiceCmdAsText", [[name: "text*", type: "STRING", description: "Execute Alexa voice command string as text"]]
        command "sendRawCommand", [
            [name: "rawCommand*", type: "STRING", description: "Sequence command name"],
            [name: "rawPayload", type: "STRING", description: "Optional command payload"]
        ]
        command "resetDriver"

        // ATTRIBUTES
        attribute "lastSpokenText", "string"
        attribute "lastVoiceActivity", "string"
        attribute "doNotDisturb", "string"
        attribute "deviceStatus", "string"
        attribute "firmwareVer", "string"
        attribute "alexaWakeWord", "string"
        attribute "mute", "string"
    }

    preferences {
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Detailed Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper Routine
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

def installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing device driver v${version()} (${timeStamp()})..."
    initialize(true)
}

def updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    initialize(false)
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

def refresh() {
    logTrace "refresh() called"
    parent?.refreshDeviceData(device.deviceNetworkId)
}

/* =========================================================================================
   SPEECH & AUDIO CONTROL COMMANDS
   ========================================================================================= */

void speak(String text) {
    if (!text) return
    logTrace "--> speak() Initiated -> Text: '${text}'"
    sendEvent(name: "lastSpokenText", value: text)
    parent?.sendBridgeCommand(device.deviceNetworkId, "speak", [text: text])
}

void playText(String text) {
    speak(text)
}

void playTextAndRestore(String text, Object speakVolume = null) {
    if (!text) return
    logTrace "--> playTextAndRestore() Initiated -> Text: '${text}' | Temporary Vol: ${speakVolume}"

    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = (speakVolume != null && speakVolume.toString().isInteger()) ? speakVolume.toInteger() : currentVol

    if (targetVol != currentVol) {
        setVolume(targetVol)
        pauseExecution(800)
    }

    speak(text)

    int wordCount = text.split("\\s+").size()
    int restoreDelay = Math.max(4, (int) Math.ceil(wordCount * 0.25) + 3)

    runIn(restoreDelay, "restoreVolumeAfterSpeech", [data: [restoreVol: currentVol]])
}

void playAnnouncement(String text) {
    if (!text) return
    logTrace "--> playAnnouncement() Initiated -> Text: '${text}'"
    sendEvent(name: "lastSpokenText", value: text)
    parent?.sendBridgeCommand(device.deviceNetworkId, "announce", [text: text])
}

void playAnnouncementAndRestore(String text, Object announceVolume = null) {
    if (!text) return
    logTrace "--> playAnnouncementAndRestore() Initiated -> Text: '${text}' | Temporary Vol: ${announceVolume}"

    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = (announceVolume != null && announceVolume.toString().isInteger()) ? announceVolume.toInteger() : currentVol

    if (targetVol != currentVol) {
        setVolume(targetVol)
        pauseExecution(800)
    }

    playAnnouncement(text)

    int wordCount = text.split("\\s+").size()
    int restoreDelay = Math.max(5, (int) Math.ceil(wordCount * 0.25) + 4)

    runIn(restoreDelay, "restoreVolumeAfterSpeech", [data: [restoreVol: currentVol]])
}

void restoreVolumeAfterSpeech(Map data) {
    int restoreVol = (data && data.restoreVol != null) ? data.restoreVol.toInteger() : 75
    setVolume(restoreVol)
}

void voiceCmdAsText(String text) {
    if (!text) return
    logTrace "--> voiceCmdAsText() Initiated -> Text: '${text}'"
    parent?.sendBridgeCommand(device.deviceNetworkId, "voiceCmdAsText", [text: text])
}

/* =========================================================================================
   VOLUME & MUTE COMMANDS
   ========================================================================================= */

void setLevel(level, duration = null) {
    setVolume(level)
}

void setVolume(volumeLevel) {
    if (volumeLevel == null) return
    int vol = volumeLevel.toInteger()
    if (vol < 0) vol = 0
    if (vol > 100) vol = 100

    logTrace "--> setVolume() Initiated -> Target Level: ${vol}%"
    sendEvent(name: "volume", value: vol, unit: "%")
    sendEvent(name: "level", value: vol, unit: "%")
    parent?.sendBridgeCommand(device.deviceNetworkId, "volume", [level: vol])
}

void volumeUp() {
    logTrace "--> volumeUp() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "volumeUp", [:])
}

void volumeDown() {
    logTrace "--> volumeDown() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "volumeDown", [:])
}

void mute() {
    logTrace "--> mute() Initiated"
    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    if (currentVol > 0) {
        state.preMuteVolume = currentVol
        logDebug "Saved pre-mute volume: ${currentVol}%"
    }
    sendEvent(name: "mute", value: "muted")
    sendEvent(name: "volume", value: 0, unit: "%")
    sendEvent(name: "level", value: 0, unit: "%")
    parent?.sendBridgeCommand(device.deviceNetworkId, "mute", [:])
}

void unmute() {
    logTrace "--> unmute() Initiated"
    int restoreVol = (state.preMuteVolume != null) ? state.preMuteVolume.toInteger() : 75
    logDebug "Unmuting and restoring saved volume: ${restoreVol}%"
    sendEvent(name: "mute", value: "unmuted")
    sendEvent(name: "volume", value: restoreVol, unit: "%")
    sendEvent(name: "level", value: restoreVol, unit: "%")
    parent?.sendBridgeCommand(device.deviceNetworkId, "unmute", [level: restoreVol])
    state.remove("preMuteVolume")
}

void setDoNotDisturb(String enabled) {
    Boolean dndState = (enabled == "true")
    logTrace "--> setDoNotDisturb() Initiated -> Enabled: ${dndState}"
    sendEvent(name: "doNotDisturb", value: dndState ? "enabled" : "disabled")
    parent?.sendBridgeCommand(device.deviceNetworkId, "dnd", [state: dndState])
}

void doNotDisturbOn() {
    setDoNotDisturb("true")
}

void doNotDisturbOff() {
    setDoNotDisturb("false")
}

void play()          { parent?.sendBridgeCommand(device.deviceNetworkId, "play", [:]) }
void pause()         { parent?.sendBridgeCommand(device.deviceNetworkId, "pause", [:]) }
void stop()          { parent?.sendBridgeCommand(device.deviceNetworkId, "stop", [:]) }
void nextTrack()     { parent?.sendBridgeCommand(device.deviceNetworkId, "nextTrack", [:]) }
void previousTrack() { parent?.sendBridgeCommand(device.deviceNetworkId, "previousTrack", [:]) }

void sendRawCommand(String rawCommand, String rawPayload = null) {
    if (!rawCommand) return
    logTrace "--> sendRawCommand() Initiated -> Cmd: '${rawCommand}' | Payload: '${rawPayload}'"
    parent?.sendBridgeCommand(device.deviceNetworkId, "raw", [rawCommand: rawCommand, rawPayload: rawPayload])
}

/* =========================================================================================
   BRIDGE DATA PARSING & STATE UPDATES
   ========================================================================================= */

void parseDeviceData(Map devData) {
    if (!devData) return
    logTrace "--> parseDeviceData() Processing payload for ${device.displayName}"

    if (devData.currentVolume != null) {
        int vol = devData.currentVolume.toInteger()
        sendEvent(name: "volume", value: vol, unit: "%")
        sendEvent(name: "level", value: vol, unit: "%")
    } else if (devData.volume != null) {
        int vol = devData.volume.toInteger()
        sendEvent(name: "volume", value: vol, unit: "%")
        sendEvent(name: "level", value: vol, unit: "%")
    }

    if (devData.muted != null) {
        Boolean isMuted = devData.muted.toBoolean()
        sendEvent(name: "mute", value: isMuted ? "muted" : "unmuted")
    }

    if (devData.online != null) {
        String status = devData.online ? "online" : "offline"
        sendEvent(name: "deviceStatus", value: status)
    }
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    initialize(false)
    logInfo "Driver reset process completed and re-initialized."
}

void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    state.clear()
    logInfo "All states have been cleared."
}

void clearAllAttributes() {
    logInfo "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
    logInfo "All attributes have been cleared."
}

void clearAllSchedules() {
    logInfo "Clearing all scheduled jobs..."
    unschedule()
    logInfo "All scheduled jobs have been successfully cleared."
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: 'Echo Device'
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${devName}: ${msg}"
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