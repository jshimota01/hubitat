/**
 * Echos Speak Advanced Device (Custom)
 * Device Driver for Hubitat Elevation
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
 * Changelog:
 * v1.0.7    09/09/26    jshimota    Standardized trace log version update banner demarcation on driver state updates.
 * v1.0.6    09/09/26    jshimota    Refactored parseDeviceData() mapping to enforce synchronized state updates across all extended bridge attributes.
 * v1.0.5    09/09/26    jshimota    Expanded attribute suite: added alarmVolume, deviceIcon HTML tile, firmwareVer, followUpMode, lastAnnouncement, lastUpdated, lastVoiceActivity, permissions, and phraseSpoken.
 * v1.0.4    09/09/26    jshimota    Added full command suite for announcements, raw commands, alarms, reminders, and relative volume adjustment.
 * v1.0.3    09/08/26    jshimota    Standardized naming references to EchosSpeak and updated metadata display.
 * v1.0.2    09/08/26    jshimota    Added multi-signature speak(), playTextAndRestore(), missing speech attributes, and health check response handlers.
 * v1.0.1    09/08/26    jshimota    Relocated @Field constants above preferences block to resolve compilation error on initial import.
 * v1.0.0    09/08/26    jshimota    Initial release of EchosSpeak Advanced Device (Custom) based on standardized driver template.
 **/

static String version() { return '1.0.7' }
def timeStamp() { return "2026/09/09 10:04 AM" }

import groovy.transform.Field

@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10

metadata {
    definition (
        name: "Echos Speak Advanced Device (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/echos-speak-advanced-device/echos-speak-advanced-device.groovy"
    ) {
        capability "Actuator"
        capability "AudioNotification"
        capability "SpeechSynthesis"
        capability "MusicPlayer"
        capability "Sensor"
        capability "Configuration"
        capability "Refresh"

        // Attributes - Health & System Status
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "onlineStatus", "string"
        attribute "ipAddress", "string"
        attribute "macAddress", "string"
        attribute "firmwareVer", "string"
        attribute "lastUpdated", "string"
        attribute "permissions", "string"

        // Attributes - Audio, Volume & Alarm Controls
        attribute "volume", "number"
        attribute "alarmVolume", "number"
        attribute "mute", "string"
        attribute "dnd", "string"
        attribute "doNotDisturb", "string"
        attribute "followUpMode", "string"
        attribute "alexaWakeWord", "string"

        // Attributes - Speech & Voice Activity
        attribute "lastSpokenText", "string"
        attribute "lastAnnouncement", "string"
        attribute "lastVoiceActivity", "string"
        attribute "phraseSpoken", "string"
        attribute "wasLastSpokenToNode", "string"

        // Attributes - Media & Track Playback
        attribute "status", "string"
        attribute "trackDescription", "string"
        attribute "currentAlbum", "string"
        attribute "currentStation", "string"
        attribute "mediaProvider", "string"

        // Attributes - Visuals & Dashboard Tiles
        attribute "albumArtUrl", "string"
        attribute "trackImage", "string"
        attribute "mediaTile", "string"
        attribute "deviceImageUrl", "string"
        attribute "deviceIcon", "string"
        attribute "deviceFamily", "string"
        attribute "deviceStyle", "string"

        // Custom Commands - Speech & Announcements
        command "playAnnouncement", [[name: "Text*", type: "STRING", description: "Broadcast native Alexa announcement"]]
        command "playAnnouncementAll", [[name: "Text*", type: "STRING", description: "Broadcast announcement to all account devices"]]
        command "playAnnouncementIncludeVis", [[name: "Text*", type: "STRING", description: "Broadcast announcement with display card for Echo Show devices"]]
        command "sendCmdToToDevice", [[name: "Cmd*", type: "STRING"], [name: "Val*", type: "STRING"]]

        // Custom Commands - Automation & Actions
        command "setDoNotDisturb", [[name: "State*", type: "ENUM", constraints: ["true", "false"]]]
        command "setAlarm", [[name: "Time*", type: "STRING", description: "Format: YYYY-MM-DD HH:MM:SS"]]
        command "createReminder", [[name: "Text*", type: "STRING"], [name: "Time*", type: "STRING", description: "Format: YYYY-MM-DD HH:MM:SS"]]
        command "setAlarmVolume", [[name: "Volume (0-100)*", type: "NUMBER"]]

        // Custom Commands - Audio & Volume Control
        command "volumeUp"
        command "volumeDown"
        command "voiceCmdAsText", [[name: "Command Text*", type: "STRING", description: "Send text string to execute as native Alexa voice command"]]
        command "playTextAndRestore", [[name: "Text*", type: "STRING"], [name: "Volume", type: "NUMBER"]]
        command "playTextAndRestoreAll", [[name: "Text*", type: "STRING"], [name: "Volume", type: "NUMBER"]]
        command "Health Check"
        command "resetDriver"
    }

    preferences {
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
    }
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

void parse(String description) {
    checkAndLogVersionDemarcation()
    logDebug "parse(): ${description}"
}

def refresh() {
    checkAndLogVersionDemarcation()
    logInfo "refresh() requested"
    parent?.refreshDeviceData(device.deviceNetworkId)
    return []
}

/* =========================================================================================
   SPEECH & AUDIO COMMAND IMPLEMENTATIONS
   ========================================================================================= */

void speak(String text) {
    checkAndLogVersionDemarcation()
    logInfo "speak(): ${text}"
    updateAttribute("lastSpokenText", text)
    parent?.sendBridgeCommand(device.deviceNetworkId, "speak", [text: text])
}

void speak(String text, BigDecimal volume, String voice = null) {
    checkAndLogVersionDemarcation()
    logInfo "speak(): text=${text}, volume=${volume}, voice=${voice}"
    if (volume != null) setVolume(volume)
    speak(text)
}

void playText(String text) { speak(text) }
void playText(String text, BigDecimal volume) { speak(text, volume) }

void playTextAndRestore(String text, BigDecimal volume = null) {
    checkAndLogVersionDemarcation()
    logInfo "playTextAndRestore(): ${text}"
    speak(text, volume)
}

void playTextAndRestoreAll(String text, BigDecimal volume = null) {
    checkAndLogVersionDemarcation()
    logInfo "playTextAndRestoreAll(): ${text}"
    speak(text, volume)
}

void playAnnouncement(String text) {
    checkAndLogVersionDemarcation()
    logInfo "playAnnouncement(): ${text}"
    updateAttribute("lastAnnouncement", text)
    parent?.sendBridgeCommand(device.deviceNetworkId, "announce", [text: text])
}

void playAnnouncementAll(String text) {
    checkAndLogVersionDemarcation()
    logInfo "playAnnouncementAll(): ${text}"
    updateAttribute("lastAnnouncement", text)
    parent?.sendBridgeCommand(device.deviceNetworkId, "announce", [text: text, target: "all"])
}

void playAnnouncementIncludeVis(String text) {
    checkAndLogVersionDemarcation()
    logInfo "playAnnouncementIncludeVis(): ${text}"
    updateAttribute("lastAnnouncement", text)
    parent?.sendBridgeCommand(device.deviceNetworkId, "announce", [text: text, display: true])
}

void sendCmdToToDevice(String cmd, String val) {
    checkAndLogVersionDemarcation()
    logInfo "sendCmdToToDevice(): cmd=${cmd}, val=${val}"
    parent?.sendBridgeCommand(device.deviceNetworkId, "sendCmd", [cmd: cmd, val: val])
}

void voiceCmdAsText(String cmd) {
    checkAndLogVersionDemarcation()
    logInfo "voiceCmdAsText(): ${cmd}"
    parent?.sendBridgeCommand(device.deviceNetworkId, "voiceCmd", [command: cmd])
}

/* =========================================================================================
   AUTOMATION, ACTIONS & ALARMS
   ========================================================================================= */

void setDoNotDisturb(String state) {
    checkAndLogVersionDemarcation()
    logInfo "setDoNotDisturb(): ${state}"
    updateAttribute("dnd", state)
    updateAttribute("doNotDisturb", state)
    parent?.sendBridgeCommand(device.deviceNetworkId, "dnd", [state: (state == "true")])
}

void setAlarm(String timeStr) {
    checkAndLogVersionDemarcation()
    logInfo "setAlarm(): ${timeStr}"
    parent?.sendBridgeCommand(device.deviceNetworkId, "setAlarm", [time: timeStr])
}

void createReminder(String text, String timeStr) {
    checkAndLogVersionDemarcation()
    logInfo "createReminder(): text=${text}, time=${timeStr}"
    parent?.sendBridgeCommand(device.deviceNetworkId, "createReminder", [text: text, time: timeStr])
}

void setAlarmVolume(BigDecimal level) {
    checkAndLogVersionDemarcation()
    logInfo "setAlarmVolume(): ${level}"
    updateAttribute("alarmVolume", level)
    parent?.sendBridgeCommand(device.deviceNetworkId, "setAlarmVolume", [level: level])
}

/* =========================================================================================
   MEDIA & VOLUME CONTROLS
   ========================================================================================= */

void setVolume(BigDecimal level) {
    checkAndLogVersionDemarcation()
    logInfo "setVolume(): ${level}"
    updateAttribute("volume", level)
    parent?.sendBridgeCommand(device.deviceNetworkId, "volume", [level: level])
}

void setVolume(Double level) { setVolume(level as BigDecimal) }
void setVolume(Integer level) { setVolume(level as BigDecimal) }

void volumeUp() {
    checkAndLogVersionDemarcation()
    BigDecimal curVol = (device.currentValue("volume") as BigDecimal) ?: 20G
    BigDecimal newVol = Math.min((curVol + 5G), 100G)
    logInfo "volumeUp(): Increasing from ${curVol} to ${newVol}"
    setVolume(newVol)
}

void volumeDown() {
    checkAndLogVersionDemarcation()
    BigDecimal curVol = (device.currentValue("volume") as BigDecimal) ?: 20G
    BigDecimal newVol = Math.max((curVol - 5G), 0G)
    logInfo "volumeDown(): Decreasing from ${curVol} to ${newVol}"
    setVolume(newVol)
}

void mute() {
    checkAndLogVersionDemarcation()
    logInfo "mute() requested"
    updateAttribute("mute", "muted")
    parent?.sendBridgeCommand(device.deviceNetworkId, "mute", [state: true])
}

void unmute() {
    checkAndLogVersionDemarcation()
    logInfo "unmute() requested"
    updateAttribute("mute", "unmuted")
    parent?.sendBridgeCommand(device.deviceNetworkId, "mute", [state: false])
}

void play() { checkAndLogVersionDemarcation(); parent?.sendBridgeCommand(device.deviceNetworkId, "mediaControl", [type: "play"]) }
void pause() { checkAndLogVersionDemarcation(); parent?.sendBridgeCommand(device.deviceNetworkId, "mediaControl", [type: "pause"]) }
void stop() { checkAndLogVersionDemarcation(); parent?.sendBridgeCommand(device.deviceNetworkId, "mediaControl", [type: "stop"]) }
void nextTrack() { checkAndLogVersionDemarcation(); parent?.sendBridgeCommand(device.deviceNetworkId, "mediaControl", [type: "next"]) }
void previousTrack() { checkAndLogVersionDemarcation(); parent?.sendBridgeCommand(device.deviceNetworkId, "mediaControl", [type: "previous"]) }

/* =========================================================================================
   PARENT DATA PARSER & STATE MANAGEMENT
   ========================================================================================= */

void parseDeviceData(Map data) {
    checkAndLogVersionDemarcation()
    logDebug "parseDeviceData(): Received map ${data}"
    unschedule("deviceCommandTimeout")

    updateAttribute("lastUpdated", new Date().format("MMM dd, yyyy - h:mm:ss a"))

    if (data.ipAddress != null) updateAttribute("ipAddress", data.ipAddress)
    if (data.macAddress != null) updateAttribute("macAddress", data.macAddress)
    if (data.firmwareVer != null) updateAttribute("firmwareVer", data.firmwareVer)
    if (data.permissions != null) updateAttribute("permissions", data.permissions)
    if (data.alexaWakeWord != null) updateAttribute("alexaWakeWord", data.alexaWakeWord)
    
    if (data.deviceFamily != null) updateAttribute("deviceFamily", data.deviceFamily)
    if (data.deviceStyle != null) updateAttribute("deviceStyle", data.deviceStyle)
    
    if (data.deviceImageUrl != null) {
        updateAttribute("deviceImageUrl", data.deviceImageUrl)
        String iconHtml = "<div style='text-align:center;'><img src='${data.deviceImageUrl}' style='max-height:80px;width:auto;border-radius:6px;'/></div>"
        updateAttribute("deviceIcon", iconHtml)
    }

    if (data.online != null) {
        String statusStr = data.online ? "online" : "offline"
        updateAttribute("onlineStatus", statusStr)
        updateAttribute("healthStatus", statusStr)
    }

    if (data.volume != null) updateAttribute("volume", data.volume as BigDecimal)
    if (data.alarmVolume != null) updateAttribute("alarmVolume", data.alarmVolume as BigDecimal)
    if (data.mute != null) updateAttribute("mute", data.mute ? "muted" : "unmuted")
    
    if (data.dnd != null) {
        String dndStr = data.dnd ? "true" : "false"
        updateAttribute("dnd", dndStr)
        updateAttribute("doNotDisturb", dndStr)
    }

    if (data.followUpMode != null) {
        updateAttribute("followUpMode", data.followUpMode ? "true" : "false")
    }

    if (data.lastVoiceActivity != null) {
        updateAttribute("lastVoiceActivity", data.lastVoiceActivity)
        updateAttribute("phraseSpoken", data.lastVoiceActivity)
    }

    if (data.status != null) updateAttribute("status", data.status)
    if (data.trackDescription != null) updateAttribute("trackDescription", data.trackDescription)
    if (data.currentAlbum != null) updateAttribute("currentAlbum", data.currentAlbum)
    if (data.currentStation != null) updateAttribute("currentStation", data.currentStation)
    if (data.mediaProvider != null) updateAttribute("mediaProvider", data.mediaProvider)

    if (data.albumArtUrl != null) {
        updateAttribute("albumArtUrl", data.albumArtUrl)
        updateAttribute("trackImage", data.albumArtUrl)
        
        String desc = data.trackDescription ?: ""
        String tileHtml = "<div style='text-align:center;'><img src='${data.albumArtUrl}' style='max-width:100%;height:auto;border-radius:8px;'/><br><b>${desc}</b></div>"
        updateAttribute("mediaTile", tileHtml)
    }
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    initializeHealthCheckPhase()
    updateAttribute("healthStatus", "unknown")
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    initialize(false)
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    initialize(false)
    List<String> cmds = []
    cmds += executeHealthCheck()
    return cmds
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    if (device.currentValue("healthStatus") == null) {
        updateAttribute("healthStatus", "unknown")
    }

    final int interval = settings.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 480
    if (interval > 0) {
        scheduleHealthCheck("executeHealthCheckScheduled", interval)
    } else {
        unschedule("executeHealthCheckScheduled")
    }

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging enabled. Will automatically turn off in 30 minutes."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

List<String> "Health Check"() { return executeHealthCheck() }

void executeHealthCheckScheduled() {
    List<String> cmds = executeHealthCheck()
    if (cmds) sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

private List<String> executeHealthCheck() {
    logDebug "Executing Health Check..."
    scheduleCommandTimeoutCheck()
    parent?.refreshDeviceData(device.deviceNetworkId)
    return []
}

private void initializeHealthCheckPhase() {
    if (state.healthCheckStartHour == null) state.healthCheckStartHour = new Random().nextInt(24)
    if (state.healthCheckStartMinute == null) state.healthCheckStartMinute = new Random().nextInt(60)
}

private void scheduleHealthCheck(String methodToSchedule, int intervalMin) {
    unschedule(methodToSchedule)
    initializeHealthCheckPhase()

    final int h = state.healthCheckStartHour as Integer
    final int m = state.healthCheckStartMinute as Integer

    logInfo "Scheduling Health Check every ${intervalMin} minutes anchored at ${String.format('%02d:%02d', h, m)} daily"

    switch (intervalMin) {
        case 60:
            schedule("0 ${m} * ? * * *", methodToSchedule)
            break
        case 240:
            String h4 = [0, 4, 8, 12, 16, 20].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h4} ? * * *", methodToSchedule)
            break
        case 480:
            String h8 = [0, 8, 16].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h8} ? * * *", methodToSchedule)
            break
        case 720:
            String h12 = [0, 12].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h12} ? * * *", methodToSchedule)
            break
        case 1440:
            schedule("0 ${m} ${h} ? * * *", methodToSchedule)
            break
        default:
            if (intervalMin >= 60) {
                int hours = intervalMin / 60
                schedule("0 ${m} */${hours} ? * * *", methodToSchedule)
            } else {
                schedule("0 */${intervalMin} * ? * * *", methodToSchedule)
            }
            break
    }
}

private void scheduleCommandTimeoutCheck(final int delay = COMMAND_TIMEOUT) {
    runIn(delay, "deviceCommandTimeout", [overwrite: true])
}

void deviceCommandTimeout() {
    logWarn "No Health Check response received from local bridge/device"
    updateAttribute("healthStatus", "offline")
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

void resetDriver() {
    logInfo "Starting full driver reset..."
    Object savedHour = state.healthCheckStartHour
    Object savedMinute = state.healthCheckStartMinute

    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()

    if (savedHour != null) state.healthCheckStartHour = savedHour
    if (savedMinute != null) state.healthCheckStartMinute = savedMinute

    initialize(false)
    logInfo "Driver reset process completed and re-initialized."
}

void clearAllDriverStates() {
    state.clear()
}

void clearAllAttributes() {
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
}

void clearAllSchedules() {
    unschedule()
}

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
    logInfo descriptionText
    sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
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
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}