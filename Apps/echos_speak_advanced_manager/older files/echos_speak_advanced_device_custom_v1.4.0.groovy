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
 *  v1.4.0 (2026-09-14) - Mute, DND & Voice Command Payload Refactor:
 *                        - Explicitly mapped mute() and unmute() to dispatch 'mute' command with boolean state.
 *                        - Mapped doNotDisturbOn() and doNotDisturbOff() to dispatch 'dnd' command with boolean state.
 *                        - Cleaned up voiceCmdAsText(text) command signature.
 *  v1.3.0 (2026-09-14) - Extended Command Suite Implementation.
 **/

import groovy.transform.Field

static String version() { return '1.4.0' }
def timeStamp() { return "2026/09/14 04:30 PM" }

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
        input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
        input name: "logWarnEnable", type: "bool", title: "Enable Warning Logging", defaultValue: true
        input name: "logErrorEnable", type: "bool", title: "Enable Error Logging", defaultValue: true
        input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
        input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false
    }
}

def installed() {
    logInfo "Installing device driver v${version()} (${timeStamp()})..."
    initialize()
}

def updated() {
    logTrace "-------------------------- Echos Speak Advanced Device v${version()} (${timeStamp()}) --------------------------"
    logInfo "Updating driver configuration..."
    initialize()
}

def initialize() {
    logTrace "=== Initializing Device ${device.displayName} ==="
    if (getSettingBool("logDebugEnable", false)) {
        runIn(1800, "disableDebugLogging")
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

    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 30
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

    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 30
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
    int restoreVol = (data && data.restoreVol != null) ? data.restoreVol.toInteger() : 30
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
    sendEvent(name: "mute", value: "muted")
    parent?.sendBridgeCommand(device.deviceNetworkId, "mute", [state: true])
}

void unmute() {
    logTrace "--> unmute() Initiated"
    sendEvent(name: "mute", value: "unmuted")
    parent?.sendBridgeCommand(device.deviceNetworkId, "mute", [state: false])
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
   LOGGING ENGINE HELPERS
   ========================================================================================= */

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
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