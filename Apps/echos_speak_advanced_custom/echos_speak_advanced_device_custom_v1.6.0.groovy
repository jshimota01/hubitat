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
 *  v1.6.0 (2026-09-17) - Modernized Finalized Attribute Specification Matrix & Dual-Track Refactor:
 *                         - Declared full attribute specification matrix: serialNumber, family, model, generation, modelNumber, onlineStatus, icon, trackImage, trackImageHtml, lastAnnouncementText.
 *                         - Implemented Dual-Track speech & announcement state engine: lastSpokenText tracks speak() phrases, lastAnnouncementText tracks playAnnouncement() phrases.
 *                         - Updated replayText() to accurately re-issue last speech vs. last announcement payload based on lastOutputType.
 *                         - Added WHA family safety guard to playAnnouncementAll() to prevent unintended group calls on single Echo devices.
 *                         - Expanded parseDeviceData() to cleanly parse all hardware, connectivity, audio, visual artwork, and voice activity attributes.
 *  v1.5.15 (2026-09-17) - Overloaded speak() Signature Resolution & Type Disambiguation:
 *                         - Added explicit speak(String text, Object voiceOrVolume) overload to handle 2-argument calls passing numeric volume (e.g., speak("text", 90)).
 *                         - Evaluates numeric input dynamically: routes integer/long second parameters to temporary speech volume, string parameters to voice profile.
 *                         - Prevents Groovy MissingMethodException when Rule Machine or platform actions invoke 2-argument speech methods with volume numbers.
 *  v1.5.14 (2026-09-17) - Do Not Disturb Standardized Command Refactor:
 *                         - Replaced setDNDOn, setDNDOff, and toggleDND commands with setDoNotDisturbOn and setDoNotDisturbOff in metadata and driver logic.
 *                         - Purged all legacy Tonesto7 and temporary DND aliases.
 *                         - Updated internal DND bypass logic (processDndBypass and restoreDoNotDisturbAfterSpeech) to invoke setDoNotDisturbOff() and setDoNotDisturbOn().
 *                         - Preserved existing parent bridge "dnd" payload structure ([state: true/false]).
 *  v1.5.13 (2026-09-16) - Enhanced Logging Engine Expansion:
 *                         - Added explicit Info log events for every GUI command button push in the Hubitat Web UI.
 *                         - Wrapped trace (--> / <--) and debug entry/exit logging around all major speech, volume, transport, and state routines.
 *                         - Added maintenance boundary logging across reset, state clear, attribute clear, and schedule routines.
 *  v1.5.12 (2026-09-16) - MusicPlayer Media Attributes & Track Metadata Mapping Refactor:
 *                         - Declared trackDescription, trackData, status, and transportStatus MusicPlayer attributes.
 *                         - Expanded parseDeviceData() to map result title, artist, album, and playing state to Hubitat standard attributes.
 *                         - Updated refresh() to fetch live media state via parent.fetchDeviceMediaState() and parse track details.
 *  v1.5.11 (2026-09-16) - Mute Command Payload Bridge Alignment:
 *                         - Refactored mute() to dispatch parent.sendBridgeCommand(..., "volume", [level: 0]) instead of sending unmapped "mute" payload to prevent HTTP 400 Bad Request errors.
 *  v1.5.10 (2026-09-16) - Live Server Media State Query Refactor for Toggle Playback:
 *                         - Refactored togglePlayback() to query server GET /api/media/{serialNumber} endpoint via parent.
 *                         - Added handleMediaStateAndToggle() callback to evaluate result.playing authoritative boolean state.
 *                         - Dispatches pause() if playing == true, or play() if playing == false.
 *                         - Bypasses toggle action cleanly on HTTP/API errors without relying on local attribute assumptions.
 *  v1.5.9 (2026-09-16) - Transactional DND Bypass, Timer Race Guard & Parameter Alignment Refactor:
 *                        - Replaced global state.preDndBypassState with transactional state.dndBypassActiveCount and state.lastDndBypassTime.
 *                        - Eliminated blocking pauseExecution() calls from speech/announcement pipelines to reduce rule latency.
 *                        - Overloaded playAnnouncementAndRestore(text, announceVolume, ignoreDnd) to explicitly accept Ignore DND input.
 *                        - Removed redundant "unmute" bridge HTTP payload dispatch from unmute() (volume set performs restore).
 *                        - Guarded restoreDoNotDisturbAfterSpeech() against race conditions when overlapping announcements occur.
 *  v1.5.8 (2026-09-16) - DND Command Interface Streamlining & Refactor:
 *                        - Removed redundant setDoNotDisturb, doNotDisturbOn, and doNotDisturbOff commands.
 *                        - Added explicit parameterless setDNDOn, setDNDOff, and toggleDND commands.
 *                        - Refactored internal DND bypass helpers to invoke setDNDOn() and setDNDOff().
 *                        - Preserved attribute "doNotDisturb" ("enabled"/"disabled") and parent bridge "dnd" payload structure.
 *  v1.5.7 (2026-09-16) - Mute State Engine Decoupling & Server Sync Refactor:
 *                        - Decoupled mute/unmute restoration from general state.storedVolume into dedicated state.muteVolume.
 *                        - Added idempotency guard in mute() to prevent overwriting saved volume when already muted.
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

static String version() { return '1.6.0' }
def timeStamp() { return "2026/09/17 11:30 AM" }

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
            [name: "announceVolume", type: "NUMBER", description: "Temporary volume (0-100)"],
            [name: "ignoreDnd", type: "ENUM", constraints: ["false", "true"], description: "Bypass Do Not Disturb mode"]
        ]
        command "setDoNotDisturbOn"
        command "setDoNotDisturbOff"
        command "voiceCmdAsText", [[name: "text*", type: "STRING", description: "Execute Alexa voice command string as text"]]
        command "sendRawCommand", [
            [name: "rawCommand*", type: "STRING", description: "Sequence command name"],
            [name: "rawPayload", type: "STRING", description: "Optional command payload"]
        ]
        command "resetDriver"

        // 1. Hardware & Topology
        attribute "serialNumber", "string"
        attribute "family", "string"
        attribute "model", "string"
        attribute "generation", "string"
        attribute "modelNumber", "string"
        attribute "firmwareVer", "string"

        // 2. Connectivity & Device State
        attribute "onlineStatus", "string"
        attribute "doNotDisturb", "string"
        attribute "alexaWakeWord", "string"

        // 3. Audio & Media Controls
        attribute "volume", "number"
        attribute "level", "number"
        attribute "mute", "string"
        attribute "status", "string"
        attribute "transportStatus", "string"
        attribute "trackDescription", "string"
        attribute "trackData", "string"

        // 4. Speech, Announcements & Interactions
        attribute "lastSpokenText", "string"
        attribute "lastAnnouncementText", "string"
        attribute "lastOutputType", "string"
        attribute "lastVoiceActivity", "string"

        // 5. UI & Dashboard Visuals
        attribute "icon", "string"
        attribute "trackImage", "string"
        attribute "trackImageHtml", "string"
    }

    preferences {
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
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
    logInfo "GUI Action: [Refresh] button clicked."
    logTrace "--> refresh() Initiated -> Pulling live device and media state..."
    parent?.refreshDeviceData(device.deviceNetworkId)
    Map mediaData = parent?.fetchDeviceMediaState(device.deviceNetworkId)
    if (mediaData && mediaData.success == true && mediaData.result) {
        logDebug "refresh() received live media payload: ${mediaData.result}"
        parseDeviceData(mediaData.result)
    } else {
        logDebug "refresh() completed device state request."
    }
    logTrace "<-- refresh() Completed"
}

/* =========================================================================================
   SPEECH & AUDIO CONTROL COMMANDS WITH DND BYPASS & OVERLOAD DISAMBIGUATION
   ========================================================================================= */

private Boolean processDndBypass(Object ignoreDnd) {
    logTrace "--> processDndBypass() Initiated -> Ignore DND: ${ignoreDnd}"
    Boolean bypassDnd = (ignoreDnd == true || ignoreDnd.toString() == "true")
    Boolean isDndActive = (device.currentValue("doNotDisturb") == "enabled")

    if (isDndActive && bypassDnd) {
        logInfo "${device.displayName} - Ignore DND active. Temporarily disabling Do Not Disturb for speech output."
        int currentCount = state.dndBypassActiveCount ?: 0
        state.dndBypassActiveCount = currentCount + 1
        state.lastDndBypassTime = now()
        setDoNotDisturbOff()
        logTrace "<-- processDndBypass() Bypassed DND"
        return true
    }
    logTrace "<-- processDndBypass() No Bypass Needed"
    return false
}

private void scheduleDndRestore(String text, Boolean didBypass) {
    if (didBypass) {
        int wordCount = text ? text.split("\\s+").size() : 10
        int dndRestoreDelay = Math.max(10, (int) Math.ceil(wordCount * 0.35) + 8)
        logDebug "scheduleDndRestore() Scheduling DND restore in ${dndRestoreDelay}s"
        runIn(dndRestoreDelay, "restoreDoNotDisturbAfterSpeech")
    }
}

void speak(String text, Object voiceOrVolume) {
    if (!text) return
    if (voiceOrVolume != null && voiceOrVolume.toString().isNumber()) {
        speak(text, null, voiceOrVolume, false)
    } else {
        speak(text, voiceOrVolume?.toString(), null, false)
    }
}

void speak(String text, String voice = null, Object volume = null, Object ignoreDnd = false) {
    if (!text) return
    logInfo "GUI Action: [speak] command triggered -> Text: \"${text}\"${voice ? " | Voice: ${voice}" : ""}${volume != null ? " | Volume: ${volume}%" : ""}${ignoreDnd ? " | Ignore DND: true" : ""}"
    logTrace "--> speak() Initiated"
    
    Boolean didBypass = processDndBypass(ignoreDnd)

    if (volume != null && volume.toString().isNumber()) {
        int targetVol = volume.toBigDecimal().intValue()
        int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
        if (targetVol != currentVol) {
            logDebug "speak() setting temporary speech volume to ${targetVol}%"
            storeCurrentVolume()
            setVolume(targetVol)
            int wordCount = text.split("\\s+").size()
            int restoreDelay = Math.max(6, (int) Math.ceil(wordCount * 0.35) + 5)
            runIn(restoreDelay, "restoreVolumeAfterSpeech")
        }
    }
    
    processOutputPayload(text, "speak", [text: text, voice: voice])
    scheduleDndRestore(text, didBypass)
    logTrace "<-- speak() Completed"
}

void deviceNotification(String text) {
    logInfo "GUI Action: [deviceNotification] triggered -> Text: \"${text}\""
    speak(text)
}

void playText(String text) {
    logInfo "GUI Action: [playText] triggered -> Text: \"${text}\""
    speak(text)
}

void playTextAndRestore(String text, Object speakVolume = null) {
    if (!text) return
    logInfo "GUI Action: [playTextAndRestore] triggered -> Text: \"${text}\" | Vol: ${speakVolume}%"
    logTrace "--> playTextAndRestore() Initiated"
    speak(text, null, speakVolume)
    logTrace "<-- playTextAndRestore() Completed"
}

void playAnnouncement(String text, Object ignoreDnd = false) {
    logInfo "GUI Action: [playAnnouncement] triggered -> Text: \"${text}\"${ignoreDnd ? " | Ignore DND: true" : ""}"
    logTrace "--> playAnnouncement() Initiated"
    Boolean didBypass = processDndBypass(ignoreDnd)
    processOutputPayload(text, "announce", [text: text])
    scheduleDndRestore(text, didBypass)
    logTrace "<-- playAnnouncement() Completed"
}

void playAnnouncementAll(String text, Object ignoreDnd = false) {
    logInfo "GUI Action: [playAnnouncementAll] triggered -> Text: \"${text}\"${ignoreDnd ? " | Ignore DND: true" : ""}"
    logTrace "--> playAnnouncementAll() Initiated"

    String currentFam = device.currentValue("family")?.toString()?.toUpperCase()
    if (currentFam != "WHA" && currentFam != "EVERYWHERE") {
        logWarn "${device.displayName} - playAnnouncementAll invoked on non-WHA hardware (${currentFam ?: 'UNKNOWN'}). Redirecting to targeted playAnnouncement."
        playAnnouncement(text, ignoreDnd)
        return
    }

    Boolean didBypass = processDndBypass(ignoreDnd)
    processOutputPayload(text, "announceAll", [text: text])
    scheduleDndRestore(text, didBypass)
    logTrace "<-- playAnnouncementAll() Completed"
}

void playAnnouncementAndRestore(String text, Object announceVolume = null, Object ignoreDnd = false) {
    if (!text) return
    logInfo "GUI Action: [playAnnouncementAndRestore] triggered -> Text: \"${text}\" | Vol: ${announceVolume}%${ignoreDnd ? " | Ignore DND: true" : ""}"
    logTrace "--> playAnnouncementAndRestore() Initiated"

    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = (announceVolume != null && announceVolume.toString().isNumber()) ? announceVolume.toBigDecimal().intValue() : currentVol

    if (targetVol != currentVol) {
        logDebug "playAnnouncementAndRestore() setting temporary volume to ${targetVol}%"
        storeCurrentVolume()
        setVolume(targetVol)
        int wordCount = text.split("\\s+").size()
        int restoreDelay = Math.max(7, (int) Math.ceil(wordCount * 0.35) + 6)
        runIn(restoreDelay, "restoreVolumeAfterSpeech")
    }

    playAnnouncement(text, ignoreDnd)
    logTrace "<-- playAnnouncementAndRestore() Completed"
}

void replayText() {
    logInfo "GUI Action: [replayText] button clicked."
    logTrace "--> replayText() Initiated"
    String lastType = state.lastOutputType ?: device.currentValue("lastOutputType") ?: "speak"

    if (lastType in ["announce", "announceAll"]) {
        String lastAnn = state.lastAnnouncementText ?: device.currentValue("lastAnnouncementText")
        if (lastAnn) {
            logInfo "${device.displayName} - Replaying last ${lastType}: \"${lastAnn}\""
            if (lastType == "announceAll") playAnnouncementAll(lastAnn) else playAnnouncement(lastAnn)
        } else {
            logWarn "${device.displayName} - Replay requested for ${lastType}, but no previous announcement text is stored."
        }
    } else {
        String lastSpoken = state.lastSpokenText ?: device.currentValue("lastSpokenText")
        if (lastSpoken) {
            logInfo "${device.displayName} - Replaying last spoken text: \"${lastSpoken}\""
            speak(lastSpoken)
        } else {
            logWarn "${device.displayName} - Replay requested, but no previous spoken text is stored."
        }
    }
    logTrace "<-- replayText() Completed"
}

private void processOutputPayload(String text, String commandType, Map payload) {
    if (!text) {
        logWarn "${device.displayName} - Suppressed empty ${commandType} request."
        return
    }

    logTrace "--> processOutputPayload() [${commandType}] -> Text: '${text}'"
    state.lastOutputType = commandType
    sendEvent(name: "lastOutputType", value: commandType, isStateChange: true)

    if (commandType in ["announce", "announceAll"]) {
        state.lastAnnouncementText = text
        sendEvent(name: "lastAnnouncementText", value: text, isStateChange: true)
    } else {
        state.lastSpokenText = text
        sendEvent(name: "lastSpokenText", value: text, isStateChange: true)
    }

    if (parent) {
        logDebug "processOutputPayload() dispatching '${commandType}' payload to parent bridge"
        parent.sendBridgeCommand(device.deviceNetworkId, commandType, payload)
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
    logTrace "<-- processOutputPayload() Completed"
}

void restoreVolumeAfterSpeech() {
    logTrace "--> restoreVolumeAfterSpeech() Callback triggered"
    restoreLastVolume()
    logTrace "<-- restoreVolumeAfterSpeech() Completed"
}

void restoreDoNotDisturbAfterSpeech() {
    logTrace "--> restoreDoNotDisturbAfterSpeech() Callback triggered"
    long lastBypassTime = state.lastDndBypassTime ?: 0L
    long elapsedTime = now() - lastBypassTime

    if (elapsedTime < 8000) {
        logDebug "${device.displayName} - Skipping DND restoration; a newer Ignore DND transaction occurred recently (${elapsedTime}ms ago)."
        logTrace "<-- restoreDoNotDisturbAfterSpeech() Aborted due to race guard"
        return
    }

    int activeCount = state.dndBypassActiveCount ?: 0
    if (activeCount > 0) {
        state.dndBypassActiveCount = activeCount - 1
    }

    if ((state.dndBypassActiveCount ?: 0) == 0) {
        logInfo "${device.displayName} - Re-enabling Do Not Disturb following Ignore DND speech completion."
        setDoNotDisturbOn()
        state.remove("lastDndBypassTime")
        state.remove("dndBypassActiveCount")
    } else {
        logDebug "${device.displayName} - DND restoration deferred; ${state.dndBypassActiveCount} active bypass operations remain queued."
    }
    logTrace "<-- restoreDoNotDisturbAfterSpeech() Completed"
}

void voiceCmdAsText(String text) {
    if (!text) return
    logInfo "GUI Action: [voiceCmdAsText] triggered -> Text: \"${text}\""
    logTrace "--> voiceCmdAsText() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "voiceCmdAsText", [text: text])
    logTrace "<-- voiceCmdAsText() Completed"
}

/* =========================================================================================
   VOLUME, MEDIA TRANSPORT & DND COMMANDS
   ========================================================================================= */

void togglePlayback() {
    logInfo "GUI Action: [togglePlayback] button clicked."
    logTrace "--> togglePlayback() Initiated -> Querying live server media endpoint..."
    Map mediaData = parent?.fetchDeviceMediaState(device.deviceNetworkId)
    handleMediaStateAndToggle(mediaData)
    logTrace "<-- togglePlayback() Completed"
}

private void handleMediaStateAndToggle(Map mediaData) {
    logTrace "--> handleMediaStateAndToggle() Initiated"
    if (!mediaData || mediaData.success != true) {
        logError "${device.displayName} - togglePlayback() failed: Unable to fetch live media state from bridge server."
        return
    }

    Map res = mediaData.result ?: [:]
    Boolean isPlaying = (res.playing == true || res.state?.toString()?.toUpperCase() == "PLAYING")
    String serverStateStr = res.state ?: (isPlaying ? "PLAYING" : "PAUSED/IDLE")

    logInfo "${device.displayName} - togglePlayback() Authoritative Server State: [${serverStateStr}] (playing: ${isPlaying})"

    if (isPlaying) {
        logInfo "${device.displayName} - Active media detected on server. Dispatching pause."
        pause()
    } else {
        logInfo "${device.displayName} - Media inactive/paused on server. Dispatching play."
        play()
    }
    logTrace "<-- handleMediaStateAndToggle() Completed"
}

void setLevel(level, duration = null) {
    logInfo "GUI Action: [setLevel] triggered -> Level: ${level}%"
    setVolume(level)
}

void setVolume(volumeLevel) {
    if (volumeLevel == null) return
    int vol = volumeLevel.toBigDecimal().intValue()
    if (vol < 0) vol = 0
    if (vol > 100) vol = 100

    logInfo "GUI Action: [setVolume] triggered -> Target Level: ${vol}%"
    logTrace "--> setVolume() Initiated"
    sendEvent(name: "volume", value: vol, unit: "%")
    sendEvent(name: "level", value: vol, unit: "%")
    parent?.sendBridgeCommand(device.deviceNetworkId, "volume", [level: vol])
    logTrace "<-- setVolume() Completed"
}

void volumeUp() {
    logInfo "GUI Action: [volumeUp] button clicked."
    logTrace "--> volumeUp() Initiated"
    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = Math.min(100, currentVol + 5)
    logDebug "volumeUp() calculated target: ${targetVol}% (from ${currentVol}%)"
    setVolume(targetVol)
    logTrace "<-- volumeUp() Completed"
}

void volumeDown() {
    logInfo "GUI Action: [volumeDown] button clicked."
    logTrace "--> volumeDown() Initiated"
    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = Math.max(0, currentVol - 5)
    logDebug "volumeDown() calculated target: ${targetVol}% (from ${currentVol}%)"
    setVolume(targetVol)
    logTrace "<-- volumeDown() Completed"
}

void storeCurrentVolume() {
    logInfo "GUI Action: [storeCurrentVolume] button clicked."
    logTrace "--> storeCurrentVolume() Initiated"
    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    state.storedVolume = currentVol
    logInfo "${device.displayName} - Stored current volume level: ${currentVol}%"
    logTrace "<-- storeCurrentVolume() Completed"
}

void restoreLastVolume() {
    logInfo "GUI Action: [restoreLastVolume] button clicked."
    logTrace "--> restoreLastVolume() Initiated"
    if (state.storedVolume != null) {
        int restoreVol = state.storedVolume.toInteger()
        logInfo "${device.displayName} - Restoring last stored volume level: ${restoreVol}%"
        setVolume(restoreVol)
    } else {
        logWarn "${device.displayName} - restoreLastVolume() called, but no saved volume exists in state.storedVolume!"
    }
    logTrace "<-- restoreLastVolume() Completed"
}

void mute() {
    logInfo "GUI Action: [mute] button clicked."
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
    
    parent?.sendBridgeCommand(device.deviceNetworkId, "volume", [level: 0])
    logTrace "<-- mute() Completed"
}

void unmute() {
    logInfo "GUI Action: [unmute] button clicked."
    logTrace "--> unmute() Initiated"
    
    int targetRestoreVol = (state.muteVolume != null && state.muteVolume.toInteger() > 0) ? state.muteVolume.toInteger() : 30
    logInfo "${device.displayName} - Unmuting output. Restoring target volume to ${targetRestoreVol}%"

    sendEvent(name: "mute", value: "unmuted")
    setVolume(targetRestoreVol)
    state.remove("muteVolume")
    logTrace "<-- unmute() Completed"
}

void setDoNotDisturbOn() {
    logInfo "GUI Action: [setDoNotDisturbOn] button clicked."
    logTrace "--> setDoNotDisturbOn() Initiated"
    sendEvent(name: "doNotDisturb", value: "enabled")
    parent?.sendBridgeCommand(device.deviceNetworkId, "dnd", [state: true])
    logTrace "<-- setDoNotDisturbOn() Completed"
}

void setDoNotDisturbOff() {
    logInfo "GUI Action: [setDoNotDisturbOff] button clicked."
    logTrace "--> setDoNotDisturbOff() Initiated"
    sendEvent(name: "doNotDisturb", value: "disabled")
    parent?.sendBridgeCommand(device.deviceNetworkId, "dnd", [state: false])
    logTrace "<-- setDoNotDisturbOff() Completed"
}

void play() {
    logInfo "GUI Action: [play] button clicked."
    logTrace "--> play() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "play", [:])
    logTrace "<-- play() Completed"
}

void pause() {
    logInfo "GUI Action: [pause] button clicked."
    logTrace "--> pause() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "pause", [:])
    logTrace "<-- pause() Completed"
}

void stop() {
    logInfo "GUI Action: [stop] button clicked."
    logTrace "--> stop() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "stop", [:])
    logTrace "<-- stop() Completed"
}

void nextTrack() {
    logInfo "GUI Action: [nextTrack] button clicked."
    logTrace "--> nextTrack() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "nextTrack", [:])
    logTrace "<-- nextTrack() Completed"
}

void previousTrack() {
    logInfo "GUI Action: [previousTrack] button clicked."
    logTrace "--> previousTrack() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "previousTrack", [:])
    logTrace "<-- previousTrack() Completed"
}

void sendRawCommand(String rawCommand, String rawPayload = null) {
    if (!rawCommand) return
    logInfo "GUI Action: [sendRawCommand] triggered -> Command: '${rawCommand}'${rawPayload ? " | Payload: ${rawPayload}" : ""}"
    logTrace "--> sendRawCommand() Initiated"
    parent?.sendBridgeCommand(device.deviceNetworkId, "raw", [rawCommand: rawCommand, rawPayload: rawPayload])
    logTrace "<-- sendRawCommand() Completed"
}

/* =========================================================================================
   BRIDGE DATA PARSING & STATE UPDATES
   ========================================================================================= */

void parseDeviceData(Map devData) {
    if (!devData) return
    logTrace "--> parseDeviceData() Processing payload for ${device.displayName}"

    // 1. Hardware & Topology
    String sNum = devData.serialNumber?.toString() ?: device.deviceNetworkId
    if (sNum) sendEvent(name: "serialNumber", value: sNum)
    if (devData.family || devData.deviceFamily) sendEvent(name: "family", value: (devData.family ?: devData.deviceFamily).toString().toUpperCase())
    if (devData.model || devData.deviceStyle) sendEvent(name: "model", value: (devData.model ?: devData.deviceStyle).toString())
    if (devData.generation) sendEvent(name: "generation", value: devData.generation.toString())
    if (devData.modelNumber || devData.deviceType) sendEvent(name: "modelNumber", value: (devData.modelNumber ?: devData.deviceType).toString())
    if (devData.firmwareVer || devData.softwareVersion) sendEvent(name: "firmwareVer", value: (devData.firmwareVer ?: devData.softwareVersion).toString())

    // 2. Connectivity & Device State
    if (devData.onlineStatus != null || devData.online != null) {
        Boolean isOnline = (devData.onlineStatus == "online" || devData.online == true)
        String onlineStr = isOnline ? "online" : "offline"
        sendEvent(name: "onlineStatus", value: onlineStr)
        sendEvent(name: "deviceStatus", value: onlineStr)
    }

    if (devData.doNotDisturb != null || devData.dnd != null) {
        Boolean isDnd = (devData.doNotDisturb == true || devData.doNotDisturb == "enabled" || devData.dnd == true)
        sendEvent(name: "doNotDisturb", value: isDnd ? "enabled" : "disabled")
    }

    if (devData.alexaWakeWord || devData.wakeWord) {
        sendEvent(name: "alexaWakeWord", value: (devData.alexaWakeWord ?: devData.wakeWord).toString().toUpperCase())
    }

    // 3. Audio & Media Controls
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

    if (devData.state != null || devData.playing != null) {
        Boolean isPlaying = (devData.playing == true || devData.state?.toString()?.toUpperCase() == "PLAYING")
        String transport = isPlaying ? "playing" : (devData.state?.toString()?.toLowerCase() ?: "stopped")
        sendEvent(name: "status", value: transport)
        sendEvent(name: "transportStatus", value: transport)
    }

    String title  = devData.title?.toString() ?: ""
    String artist = devData.artist?.toString() ?: ""
    String album  = devData.album?.toString() ?: ""

    if (title || artist) {
        String desc = (artist && title) ? "${artist} - ${title}" : (title ?: artist)
        sendEvent(name: "trackDescription", value: desc)
        Map trackMap = [title: title, artist: artist, album: album]
        sendEvent(name: "trackData", value: groovy.json.JsonOutput.toJson(trackMap))
    } else if (devData.playing == false || devData.state == "PAUSED" || devData.state == "IDLE") {
        sendEvent(name: "trackDescription", value: "Stopped")
    }

    // 4. Speech, Announcements & Interactions
    if (devData.lastVoiceActivity || devData.phraseSpoken) {
        sendEvent(name: "lastVoiceActivity", value: (devData.lastVoiceActivity ?: devData.phraseSpoken).toString())
    }

    // 5. Visual Artwork & Icons
    if (devData.icon || devData.deviceIcon) {
        sendEvent(name: "icon", value: (devData.icon ?: devData.deviceIcon).toString())
    }

    if (devData.trackImage || devData.albumArtUrl) {
        String imgUrl = (devData.trackImage ?: devData.albumArtUrl).toString()
        sendEvent(name: "trackImage", value: imgUrl)
        sendEvent(name: "trackImageHtml", value: "<img src='${imgUrl}' style='max-height:100px; max-width:100px;'/>")
    }

    logTrace "<-- parseDeviceData() Completed"
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
    logInfo "GUI Action: [resetDriver] triggered -> Starting full driver reset..."
    logTrace "--> resetDriver() Initiated"
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    initialize(false)
    logInfo "${device.displayName} - Driver reset process completed and re-initialized."
    logTrace "<-- resetDriver() Completed"
}

void clearAllDriverStates() {
    logInfo "GUI Action: [clearAllDriverStates] triggered -> Clearing driver state variables..."
    logTrace "--> clearAllDriverStates() Initiated"
    state.clear()
    logInfo "${device.displayName} - All driver state variables have been cleared."
    logTrace "<-- clearAllDriverStates() Completed"
}

void clearAllAttributes() {
    logInfo "GUI Action: [clearAllAttributes] triggered -> Clearing current attribute states..."
    logTrace "--> clearAllAttributes() Initiated"
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
    logInfo "${device.displayName} - All attributes have been cleared."
    logTrace "<-- clearAllAttributes() Completed"
}

void clearAllSchedules() {
    logInfo "GUI Action: [clearAllSchedules] triggered -> Unscheduling all background jobs..."
    logTrace "--> clearAllSchedules() Initiated"
    unschedule()
    logInfo "${device.displayName} - All scheduled jobs have been successfully cleared."
    logTrace "<-- clearAllSchedules() Completed"
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