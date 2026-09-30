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
 *  v1.5.7 (2026-09-16) - Mute State Engine Decoupling & Server Sync Refactor:
 *                        - Decoupled mute/unmute restoration from general state.storedVolume into dedicated state.muteVolume.
 *                        - Added idempotency guard in mute() to prevent overwriting saved volume when already muted.
 *                        - Added server-side "unmute" bridge command dispatch in unmute().
 *                        - Added state.remove("muteVolume") cleanup following successful unmute restoration.
 *  v1.5.6 (2026-09-15) - UI Metadata Declaration for Ignore DND Inputs:
 *                        - Updated command metadata definitions for speak, playAnnouncement, and playAnnouncementAll to expose ignoreDnd ENUM inputs in Hubitat Web UI.
 *                        - Reinforced type conversions for ignoreDnd parameter inputs across all speech and announcement pathways.
 *  v1.5.5 (2026-09-15) - Ignore DND Bypass Logic & Toggle Playback Refactor:
 *                        - Added togglePlayback() driver command to evaluate transportStatus and route play/pause.
 *                        - Integrated Ignore DND bypass logic across speak(), playAnnouncement(), and playAnnouncementAll().
 *                        - Added state.preDndBypassState tracking and scheduled restoreDoNotDisturbAfterSpeech() callbacks.
 *  v1.5.4 (2026-09-15) - Overloaded Speech Signature Guard & Replay Refactor:
 *                        - Overloaded speak(String text, String voice, Object volume) signature to catch 3-argument Hubitat platform & Rule Machine calls.
 *                        - Integrated inline temporary volume handling and restore callback scheduling when speak() receives volume parameter.
 *  v1.5.3 (2026-09-15) - Consolidated Replay Text & Announcement State Engine:
 *                        - Added state.lastOutputType and attribute lastOutputType tracking to store command modalities (speak, announce, announceAll).
 *                        - Refactored processOutputPayload() pipeline to capture speech, deviceNotification, and announcements into state.lastSpokenText.
 *                        - Refactored replayText() to re-issue stored audio payloads using their exact original command modality.
 *                        - Added playAnnouncementAll() command signature for targeted child-initiated whole-house announcements.
 *  v1.5.2 (2026-09-15) - Stored Volume State Engine Integration:
 *                        - Added storeCurrentVolume() command to snapshot active volume to state.storedVolume.
 *                        - Added restoreLastVolume() / restoreCurrentVolume() command to restore stored volume level.
 *  v1.5.1 (2026-09-15) - Driver-Side Relative Volume Calculation Fix:
 *                        - Refactored volumeUp() and volumeDown() to calculate relative volume changes (+/- 5%) directly from driver state.
 *                        - Dispatches explicit 'volume' command with calculated integer target level to bypass server null currentVolume bugs.
 *  v1.5.0 (2026-09-15) - Template Alignment & Driver Maintenance Refactor:
 *                        - Purged obsolete custom Health Check scheduling and attributes.
 *                        - Integrated single-shot version demarcation logging helper to track version in state.driverVersion.
 *                        - Added resetDriver(), clearAllDriverStates(), clearAllAttributes(), and clearAllSchedules() for GUI maintenance.
 *
 *				[KEEP] Prior change history is found in changelog_device.txt (v0.0.0 - v1.5.0)
 **/

import groovy.transform.Field

static String version() { return '1.5.7' }
def timeStamp() { return "2026/09/16 12:00 PM" }

metadata {
    definition (
        name: "Echos Speak Advanced Device (Custom)",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/echos_speak_advanced_device/echos_speak_advanced_device.groovy"
    ) {
        capability "SpeechSynthesis"
        capability "AudioNotification"
        capability "Notification"
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
        command "storeCurrentVolume"
        command "restoreLastVolume"
        command "mute"
        command "unmute"
        command "togglePlayback"
        command "replayText"
        command "speak", [
            [name: "text*", type: "STRING", description: "Text to speak out loud"],
            [name: "voice", type: "STRING", description: "Optional voice profile"],
            [name: "volume", type: "NUMBER", description: "Optional temporary volume level (0-100)"],
            [name: "ignoreDnd", type: "ENUM", constraints: ["false", "true"], description: "Bypass Do Not Disturb mode"]
        ]
        command "playTextAndRestore", [
            [name: "text*", type: "STRING", description: "Text to speak out loud"],
            [name: "speakVolume", type: "NUMBER", description: "Temporary volume (0-100)"]
        ]
        command "playAnnouncement", [
            [name: "text*", type: "STRING", description: "Play text as an Alexa Announcement chime"],
            [name: "ignoreDnd", type: "ENUM", constraints: ["false", "true"], description: "Bypass Do Not Disturb mode"]
        ]
        command "playAnnouncementAll", [
            [name: "text*", type: "STRING", description: "Play text as a global whole-house Alexa Announcement"],
            [name: "ignoreDnd", type: "ENUM", constraints: ["false", "true"], description: "Bypass Do Not Disturb mode"]
        ]
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
        attribute "lastOutputType", "string"
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
   SPEECH & AUDIO CONTROL COMMANDS WITH DND BYPASS
   ========================================================================================= */

void speak(String text, String voice = null, Object volume = null, Object ignoreDnd = false) {
    if (!text) return
    
    Boolean bypassDnd = (ignoreDnd == true || ignoreDnd.toString() == "true")
    Boolean isDndActive = (device.currentValue("doNotDisturb") == "enabled")

    if (isDndActive && bypassDnd) {
        logInfo "${device.displayName} - Ignore DND active. Temporarily disabling Do Not Disturb for speech output."
        state.preDndBypassState = true
        doNotDisturbOff()
        pauseExecution(600)
    }

    if (volume != null && volume.toString().isInteger()) {
        int targetVol = volume.toInteger()
        int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
        if (targetVol != currentVol) {
            storeCurrentVolume()
            setVolume(targetVol)
            pauseExecution(800)
            int wordCount = text.split("\\s+").size()
            int restoreDelay = Math.max(4, (int) Math.ceil(wordCount * 0.25) + 3)
            runIn(restoreDelay, "restoreVolumeAfterSpeech")
        }
    }
    
    processOutputPayload(text, "speak", [text: text])

    if (isDndActive && bypassDnd) {
        int wordCount = text.split("\\s+").size()
        int dndRestoreDelay = Math.max(5, (int) Math.ceil(wordCount * 0.25) + 4)
        runIn(dndRestoreDelay, "restoreDoNotDisturbAfterSpeech")
    }
}

void deviceNotification(String text) {
    speak(text)
}

void playText(String text) {
    speak(text)
}

void playTextAndRestore(String text, Object speakVolume = null) {
    if (!text) return
    logTrace "--> playTextAndRestore() Initiated -> Text: '${text}' | Temporary Vol: ${speakVolume}"
    speak(text, null, speakVolume)
}

void playAnnouncement(String text, Object ignoreDnd = false) {
    Boolean bypassDnd = (ignoreDnd == true || ignoreDnd.toString() == "true")
    Boolean isDndActive = (device.currentValue("doNotDisturb") == "enabled")

    if (isDndActive && bypassDnd) {
        logInfo "${device.displayName} - Ignore DND active. Temporarily disabling Do Not Disturb for announcement."
        state.preDndBypassState = true
        doNotDisturbOff()
        pauseExecution(600)
    }

    processOutputPayload(text, "announce", [text: text])

    if (isDndActive && bypassDnd) {
        int wordCount = text.split("\\s+").size()
        int dndRestoreDelay = Math.max(6, (int) Math.ceil(wordCount * 0.25) + 5)
        runIn(dndRestoreDelay, "restoreDoNotDisturbAfterSpeech")
    }
}

void playAnnouncementAll(String text, Object ignoreDnd = false) {
    Boolean bypassDnd = (ignoreDnd == true || ignoreDnd.toString() == "true")
    Boolean isDndActive = (device.currentValue("doNotDisturb") == "enabled")

    if (isDndActive && bypassDnd) {
        logInfo "${device.displayName} - Ignore DND active. Temporarily disabling Do Not Disturb for global announcement."
        state.preDndBypassState = true
        doNotDisturbOff()
        pauseExecution(600)
    }

    processOutputPayload(text, "announceAll", [text: text])

    if (isDndActive && bypassDnd) {
        int wordCount = text.split("\\s+").size()
        int dndRestoreDelay = Math.max(6, (int) Math.ceil(wordCount * 0.25) + 5)
        runIn(dndRestoreDelay, "restoreDoNotDisturbAfterSpeech")
    }
}

void playAnnouncementAndRestore(String text, Object announceVolume = null) {
    if (!text) return
    logTrace "--> playAnnouncementAndRestore() Initiated -> Text: '${text}' | Temporary Vol: ${announceVolume}"

    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = (announceVolume != null && announceVolume.toString().isInteger()) ? announceVolume.toInteger() : currentVol

    if (targetVol != currentVol) {
        storeCurrentVolume()
        setVolume(targetVol)
        pauseExecution(800)
    }

    playAnnouncement(text)

    int wordCount = text.split("\\s+").size()
    int restoreDelay = Math.max(5, (int) Math.ceil(wordCount * 0.25) + 4)

    runIn(restoreDelay, "restoreVolumeAfterSpeech")
}

void replayText() {
    String lastText = state.lastSpokenText ?: device.currentValue("lastSpokenText")
    String lastType = state.lastOutputType ?: device.currentValue("lastOutputType") ?: "speak"

    if (lastText) {
        logInfo "${device.displayName} - Replaying last ${lastType}: \"${lastText}\""
        if (lastType == "announce") {
            playAnnouncement(lastText)
        } else if (lastType == "announceAll") {
            playAnnouncementAll(lastText)
        } else {
            speak(lastText)
        }
    } else {
        logWarn "${device.displayName} - Replay requested, but no previous text or announcement is stored."
    }
}

private void processOutputPayload(String text, String commandType, Map payload) {
    if (!text) {
        logWarn "${device.displayName} - Suppressed empty ${commandType} request."
        return
    }

    logTrace "--> processOutputPayload() [${commandType}] -> Text: '${text}'"
    state.lastSpokenText = text
    state.lastOutputType = commandType

    sendEvent(name: "lastSpokenText", value: text, isStateChange: true)
    sendEvent(name: "lastOutputType", value: commandType, isStateChange: true)

    if (parent) {
        parent.sendBridgeCommand(device.deviceNetworkId, commandType, payload)
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
}

void restoreVolumeAfterSpeech() {
    restoreLastVolume()
}

void restoreDoNotDisturbAfterSpeech() {
    if (state.preDndBypassState == true) {
        logInfo "${device.displayName} - Re-enabling Do Not Disturb following Ignore DND speech completion."
        doNotDisturbOn()
        state.remove("preDndBypassState")
    }
}

void voiceCmdAsText(String text) {
    if (!text) return
    logTrace "--> voiceCmdAsText() Initiated -> Text: '${text}'"
    parent?.sendBridgeCommand(device.deviceNetworkId, "voiceCmdAsText", [text: text])
}

/* =========================================================================================
   VOLUME & MEDIA TRANSPORT COMMANDS
   ========================================================================================= */

void togglePlayback() {
    String currentStatus = device.currentValue("transportStatus") ?: state.lastTransportStatus ?: "stopped"
    logTrace "--> togglePlayback() Initiated -> Current Status: '${currentStatus}'"
    
    if (currentStatus == "playing") {
        logInfo "${device.displayName} - Toggle Playback: Pausing active stream."
        pause()
    } else {
        logInfo "${device.displayName} - Toggle Playback: Resuming stream playback."
        play()
    }
}

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
    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = Math.min(100, currentVol + 5)
    logTrace "--> volumeUp() Calculated Target: ${targetVol}% (from ${currentVol}%)"
    setVolume(targetVol)
}

void volumeDown() {
    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = Math.max(0, currentVol - 5)
    logTrace "--> volumeDown() Calculated Target: ${targetVol}% (from ${currentVol}%)"
    setVolume(targetVol)
}

void storeCurrentVolume() {
    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    state.storedVolume = currentVol
    logInfo "Stored current volume level: ${currentVol}%"
}

void restoreLastVolume() {
    if (state.storedVolume != null) {
        int restoreVol = state.storedVolume.toInteger()
        logInfo "Restoring last stored volume level: ${restoreVol}%"
        setVolume(restoreVol)
    } else {
        logWarn "restoreLastVolume() called, but no saved volume exists in state.storedVolume!"
    }
}

void mute() {
    logTrace "--> mute() Initiated"
    String currentMuteState = device.currentValue("mute")
    
    if (currentMuteState != "muted") {
        int activeVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
        state.muteVolume = activeVol
        logInfo "${device.displayName} - Muting output. Saved active volume (${activeVol}%) to state.muteVolume"
    } else {
        logDebug "${device.displayName} - mute() called, but device is already muted. Preserving state.muteVolume (${state.muteVolume}%)."
    }

    sendEvent(name: "mute", value: "muted")
    sendEvent(name: "volume", value: 0, unit: "%")
    sendEvent(name: "level", value: 0, unit: "%")
    
    parent?.sendBridgeCommand(device.deviceNetworkId, "mute", [:])
}

void unmute() {
    logTrace "--> unmute() Initiated"
    
    int targetRestoreVol = (state.muteVolume != null && state.muteVolume.toInteger() > 0) ? state.muteVolume.toInteger() : 30
    logInfo "${device.displayName} - Unmuting output. Restoring target volume to ${targetRestoreVol}%"

    sendEvent(name: "mute", value: "unmuted")
    setVolume(targetRestoreVol)
    
    parent?.sendBridgeCommand(device.deviceNetworkId, "unmute", [:])
    state.remove("muteVolume")
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