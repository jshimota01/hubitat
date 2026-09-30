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
 *  v1.1.2 (2026-09-09) - Speech & Restore Sequence Timing Fix:
 *                        - Resolved premature volume restore truncating speech playback by introducing runIn() delayed restore callbacks.
 *                        - Added word-count based speech duration estimation to prevent clipping TTS audio.
 *  v1.1.1 (2026-09-09) - Command Parameter Normalization:
 *                        - Enforced strict integer casting on setVolume() to align with bridge API sequence requirements.
 *  v1.1.0 (2026-09-08) - Initial deployment of Echos Speak Advanced Child Device Driver.
 **/

import groovy.transform.Field

static String version() { return '1.1.2' }
def timeStamp() { return "2026/09/09 02:42 PM" }

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
        capability "VolumeControl"
        capability "Refresh"
        capability "Sensor"

        // CUSTOM CAPABILITIES & COMMANDS
        command "voiceCmdAsText", [[name: "command*", type: "STRING", description: "Send text voice command directly to Alexa (e.g. 'turn on office ceiling')"]]
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

        // ATTRIBUTES
        attribute "lastSpokenText", "string"
        attribute "lastVoiceActivity", "string"
        attribute "doNotDisturb", "string"
        attribute "deviceStatus", "string"
        attribute "firmwareVer", "string"
        attribute "alexaWakeWord", "string"
    }

    preferences {
        input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
        input name: "logWarnEnable", type: "bool", title: "Enable Warning Logging", defaultValue: true
        input name: "logErrorEnable", type: "bool", title: "Enable Error Logging", defaultValue: true
        input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
        input name: "logTraceEnable", type: "bool", title: "Enable Detailed Trace Logging", defaultValue: false
    }
}

/* =========================================================================================
   DRIVER LIFECYCLE HANDLERS
   ========================================================================================= */

def installed() {
    logInfo "Installing device driver v${version()} (${timeStamp()})..."
    initialize()
}

def updated() {
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
    logTrace "<-- speak() Exit"
}

void playText(String text) {
    speak(text)
}

void playTextAndRestore(String text, Object speakVolume = null) {
    if (!text) return
    logTrace "--> playTextAndRestore() Initiated -> Text: '${text}' | Temporary Vol: ${speakVolume}"

    int currentVol = device.currentValue("volume") ? device.currentValue("volume").toInteger() : 30
    int targetVol = (speakVolume != null && speakVolume.toString().isInteger()) ? speakVolume.toInteger() : currentVol

    if (targetVol != currentVol) {
        logDebug "playTextAndRestore() -> Adjusting volume to temporary level: ${targetVol}%"
        setVolume(targetVol)
        pauseExecution(800) // Allow Amazon hardware to adjust audio pipeline
    }

    speak(text)

    // Estimate speech duration: 250ms per word + 3 second safety buffer
    int wordCount = text.split("\\s+").size()
    int restoreDelay = Math.max(4, (int) Math.ceil(wordCount * 0.25) + 3)

    logDebug "playTextAndRestore() -> Scheduling volume restore to ${currentVol}% in ${restoreDelay} seconds"
    runIn(restoreDelay, "restoreVolumeAfterSpeech", [data: [restoreVol: currentVol]])
    logTrace "<-- playTextAndRestore() Exit"
}

void playAnnouncement(String text) {
    if (!text) return
    logTrace "--> playAnnouncement() Initiated -> Text: '${text}'"
    sendEvent(name: "lastSpokenText", value: text)
    parent?.sendBridgeCommand(device.deviceNetworkId, "announce", [text: text])
    logTrace "<-- playAnnouncement() Exit"
}

void playAnnouncementAndRestore(String text, Object announceVolume = null) {
    if (!text) return
    logTrace "--> playAnnouncementAndRestore() Initiated -> Text: '${text}' | Temporary Vol: ${announceVolume}"

    int currentVol = device.currentValue("volume") ? device.currentValue("volume").toInteger() : 30
    int targetVol = (announceVolume != null && announceVolume.toString().isInteger()) ? announceVolume.toInteger() : currentVol

    if (targetVol != currentVol) {
        logDebug "playAnnouncementAndRestore() -> Adjusting volume to temporary level: ${targetVol}%"
        setVolume(targetVol)
        pauseExecution(800)
    }

    playAnnouncement(text)

    int wordCount = text.split("\\s+").size()
    int restoreDelay = Math.max(5, (int) Math.ceil(wordCount * 0.25) + 4)

    logDebug "playAnnouncementAndRestore() -> Scheduling volume restore to ${currentVol}% in ${restoreDelay} seconds"
    runIn(restoreDelay, "restoreVolumeAfterSpeech", [data: [restoreVol: currentVol]])
    logTrace "<-- playAnnouncementAndRestore() Exit"
}

void restoreVolumeAfterSpeech(Map data) {
    int restoreVol = data?.restoreVol ? data.restoreVol.toInteger() : 30
    logDebug "restoreVolumeAfterSpeech() -> Restoring volume back to: ${restoreVol}%"
    setVolume(restoreVol)
}

void voiceCmdAsText(String command) {
    if (!command) return
    logTrace "--> voiceCmdAsText() Initiated -> Command: '${command}'"
    parent?.sendBridgeCommand(device.deviceNetworkId, "voiceCmd", [command: command])
    logTrace "<-- voiceCmdAsText() Exit"
}

/* =========================================================================================
   VOLUME & MEDIA CONTROL COMMANDS
   ========================================================================================= */

void setVolume(volumeLevel) {
    if (volumeLevel == null) return
    int vol = volumeLevel.toInteger()
    if (vol < 0) vol = 0
    if (vol > 100) vol = 100

    logTrace "--> setVolume() Initiated -> Target Level: ${vol}%"
    sendEvent(name: "volume", value: vol, unit: "%")
    parent?.sendBridgeCommand(device.deviceNetworkId, "volume", [level: vol])
    logTrace "<-- setVolume() Exit"
}

void volumeUp() {
    int currentVol = device.currentValue("volume") ? device.currentValue("volume").toInteger() : 30
    setVolume(currentVol + 5)
}

void volumeDown() {
    int currentVol = device.currentValue("volume") ? device.currentValue("volume").toInteger() : 30
    setVolume(currentVol - 5)
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

void play()  { parent?.sendBridgeCommand(device.deviceNetworkId, "mediaControl", [type: "play"]) }
void pause() { parent?.sendBridgeCommand(device.deviceNetworkId, "mediaControl", [type: "pause"]) }
void stop()  { parent?.sendBridgeCommand(device.deviceNetworkId, "mediaControl", [type: "stop"]) }

/* =========================================================================================
   BRIDGE DATA PARSING & STATE UPDATES
   ========================================================================================= */

void parseDeviceData(Map devData) {
    if (!devData) return
    logTrace "--> parseDeviceData() Processing payload for ${device.displayName}"

    if (devData.volume != null) {
        sendEvent(name: "volume", value: devData.volume.toInteger(), unit: "%")
    }

    if (devData.online != null) {
        String status = devData.online ? "online" : "offline"
        sendEvent(name: "deviceStatus", value: status)
    }

    if (devData.firmwareVer) {
        sendEvent(name: "firmwareVer", value: devData.firmwareVer.toString())
    }

    if (devData.alexaWakeWord) {
        sendEvent(name: "alexaWakeWord", value: devData.alexaWakeWord.toString())
    }

    if (devData.lastVoiceActivity) {
        sendEvent(name: "lastVoiceActivity", value: devData.lastVoiceActivity.toString())
    }

    logTrace "<-- parseDeviceData() Exit"
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