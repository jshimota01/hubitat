/**
 * Echos Speak Advanced W/Sensors Device Driver
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
 *  Child Device Driver for managing sensor and display-capable Amazon Echo devices bridged via Echos Speak Advanced Parent App.
 *
 *  Changelog:
 *  v2.1.8 (2026-09-22) - Missing voiceCmdAsText Method Fix:
 *                          - Added missing voiceCmdAsText(String text) method implementation to resolve MissingMethodExceptionNoStack error on sensor devices.
 *  v2.1.7 (2026-09-22) - Toggle Playback Trace & Parent Guard Alignment (A5):
 *                          - Aligned togglePlayback() with standard transport commands by adding explicit parent reference check and updating exit trace to '<-- togglePlayback() Transport Dispatch Completed'.
 *  v2.1.6 (2026-09-22) - Playback Command Hardening (A5):
 *                          - Added hasServerCapability("musicPlayer") guards, Parent reference verification, and boolean return evaluation across play(), pause(), stop(), previousTrack(), and nextTrack().
 *                          - Added standardized GUI Action logInfo and Trace logging for transport execution.
 *  v2.1.5 (2026-09-22) - Direct Volume Command Target Transaction Logging:
 *                          - Refactored volumeCommandState == "success" logging in parseDeviceData() to output devData.volumeCommandTarget directly.
 *                          - Eliminates ambiguity by reporting the precise transaction target confirmed by the bridge server rather than relying on current volume state.
 *  v2.1.4 (2026-09-22) - Target Volume Parsing Sequence Correction:
 *                          - Moved volume parsing block ahead of transaction log evaluation in parseDeviceData() so Target % prints accurately instead of N/A%.
 *
 *                [KEEP] Prior change history is found in changelog_device.txt (v0.0.0 - v2.0.9)
 **/

import groovy.transform.Field

@Field static final List<Map> deviceModelTable = [
    [deviceType: "AB72C64C86AW2", family: "ECHO", generation: "1", modelName: "Echo", imageFilename: "echo_gen1", validated: true],
    [deviceType: "A7WXQPH584YP", family: "ECHO", generation: "2", modelName: "Echo", imageFilename: "echo_gen2", validated: true],
    [deviceType: "A3FX4UWTP28V1P", family: "ECHO", generation: "3", modelName: "Echo", imageFilename: "echo_gen3", validated: true],
    [deviceType: "A3RMGO6LYLH7YN", family: "ECHO", generation: "4", modelName: "Echo", imageFilename: "echo_gen4", validated: true],
    [deviceType: "A2M35JJZWCQOMZ", family: "ECHO", generation: "1", modelName: "Echo Plus", imageFilename: "echo_plus_gen1", validated: true],
    [deviceType: "A18O6U1UQFJ0XK", family: "ECHO", generation: "2", modelName: "Echo Plus", imageFilename: "echo_plus_gen2", validated: true],
    [deviceType: "ASQZWP4GPYUT7", family: "ECHO", generation: "Unspecified", modelName: "Echo Pop", imageFilename: "echo_pop", validated: true],
    [deviceType: "AKNO1N0KSFN8L", family: "ECHO", generation: "1", modelName: "Echo Dot", imageFilename: "echo_dot_gen1", validated: true],
    [deviceType: "A3S5BH2HU6VAYF", family: "ECHO", generation: "2", modelName: "Echo Dot", imageFilename: "echo_dot_gen2", validated: true],
    [deviceType: "A32DDESGESSHZA", family: "ECHO", generation: "3", modelName: "Echo Dot", imageFilename: "echo_dot_gen3", validated: true],
    [deviceType: "A32DOYMUN6DTXA", family: "ECHO", generation: "3", modelName: "Echo Dot", imageFilename: "echo_dot_gen3", validated: true],
    [deviceType: "A1RABVCI4QCIKC", family: "ECHO", generation: "3", modelName: "Echo Dot", imageFilename: "echo_dot_gen3", validated: true],
    [deviceType: "A2U21SRK4QGSE1", family: "ECHO", generation: "4", modelName: "Echo Dot", imageFilename: "echo_dot_gen4", validated: true],
    [deviceType: "A4ZXE0RM7LQ7A", family: "ECHO", generation: "5", modelName: "Echo Dot", imageFilename: "echo_dot_gen5", validated: true],
    [deviceType: "A30YDR2MK8HMRV", family: "ECHO", generation: "3", modelName: "Echo Dot with Clock", imageFilename: "echo_dot_clock_gen3", validated: true],
    [deviceType: "A2H4LV5GIZ1JFT", family: "ECHO", generation: "4", modelName: "Echo Dot with Clock", imageFilename: "echo_dot_clock_gen4", validated: true],
    [deviceType: "A2DS1Q2TPDJ48U", family: "ECHO", generation: "5", modelName: "Echo Dot with Clock", imageFilename: "echo_dot_clock_gen5", validated: true],
    [deviceType: "A10A33FOX2NUBK", family: "ROOK", generation: "1", modelName: "Echo Spot", imageFilename: "echo_spot_gen1", validated: true],
    [deviceType: "A3EH2E0YZ30OD6", family: "ROOK", generation: "2", modelName: "Echo Spot", imageFilename: "echo_spot_gen2", validated: true],
    [deviceType: "A1NL4BVLQ4L3N3", family: "KNIGHT", generation: "1", modelName: "Echo Show", imageFilename: "echo_show_gen1", validated: true],
    [deviceType: "AWZZ5CVHX2CD", family: "KNIGHT", generation: "2", modelName: "Echo Show", imageFilename: "echo_show_gen2", validated: true],
    [deviceType: "A4ZP7ZC4PI6TO", family: "KNIGHT", generation: "1", modelName: "Echo Show 5", imageFilename: "echo_show_5", validated: true],
    [deviceType: "A1XWJRHALS1REP", family: "KNIGHT", generation: "2", modelName: "Echo Show 5", imageFilename: "echo_show_5", validated: true],
    [deviceType: "A11QM4H9HGV71H", family: "KNIGHT", generation: "3", modelName: "Echo Show 5", imageFilename: "echo_show_5", validated: true],
    [deviceType: "A1Z88NGR2BK6A2", family: "KNIGHT", generation: "1", modelName: "Echo Show 8", imageFilename: "echo_show_8", validated: true],
    [deviceType: "A15996VY63BQ2D", family: "KNIGHT", generation: "2", modelName: "Echo Show 8", imageFilename: "echo_show_8", validated: true],
    [deviceType: "A2UONLFQW0PADH", family: "KNIGHT", generation: "3", modelName: "Echo Show 8", imageFilename: "echo_show_8", validated: true],
    [deviceType: "A345JHMQDGG1M5", family: "KNIGHT", generation: "4", modelName: "Echo Show 8", imageFilename: "echo_show_8", validated: true],
    [deviceType: "AIPK7MM90V7TB", family: "KNIGHT", generation: "3", modelName: "Echo Show 10", imageFilename: "echo_show_10_gen3", validated: true],
    [deviceType: "A1EIANJ7PNB0Q7", family: "KNIGHT", generation: "1", modelName: "Echo Show 15", imageFilename: "echo_show_15", validated: true],
    [deviceType: "AQ24620N8QD5Q", family: "KNIGHT", generation: "2", modelName: "Echo Show 15", imageFilename: "echo_show_15", validated: true],
    [deviceType: "AZANTNEUTYY6L", family: "KNIGHT", generation: "Unspecified", modelName: "Echo Show 21", imageFilename: "echo_show_21", validated: true],
    [deviceType: "A2LLN0UXRW4N50", family: "KNIGHT", generation: "1", modelName: "Echo Show 11", imageFilename: "echo_show_11_gen1", validated: true],
    [deviceType: "A3RBAYBE7VM004", family: "ECHO", generation: "Unspecified", modelName: "Echo Studio", imageFilename: "echo_studio", validated: true],
    [deviceType: "A3VRME03NAXFUB", family: "ECHO", generation: "Unspecified", modelName: "Echo Flex", imageFilename: "echo_flex", validated: true],
    [deviceType: "A3SSG6GR8UU7SN", family: "ECHO", generation: "1", modelName: "Echo Sub", imageFilename: "echo_sub_gen1", validated: true],
    [deviceType: "A27VEYGQBW3YR5", family: "ECHO", generation: "Unspecified", modelName: "Echo Link", imageFilename: "echo_link", validated: false],
    [deviceType: "A2RU4B77X9R9NZ", family: "ECHO", generation: "Unspecified", modelName: "Echo Link Amp", imageFilename: "echo_link_amp", validated: false],
    [deviceType: "A1JJ0KFC4ZPNJ3", family: "ECHO", generation: "Unspecified", modelName: "Echo Input", imageFilename: "echo_input", validated: true],
    [deviceType: "A38949IHXHRQ5P", family: "ECHO", generation: "Unspecified", modelName: "Echo Tap", imageFilename: "echo_tap", validated: true],
    [deviceType: "A3IYPH06PH1HRA", family: "ECHO", generation: "Unspecified", modelName: "Echo Frames", imageFilename: "echo_frames", validated: false],
    [deviceType: "A15QWUTQ6FSMYX", family: "ECHO", generation: "2", modelName: "Echo Buds", imageFilename: "echo_buds_gen2", validated: false],
    [deviceType: "A303PJF6ISQ7IC", family: "ALEXA_AUTO", generation: "Unspecified", modelName: "Echo Auto", imageFilename: "echo_auto", validated: true],
    [deviceType: "A195TXHV1M5D4A", family: "ALEXA_AUTO", generation: "Unspecified", modelName: "Echo Auto", imageFilename: "echo_auto", validated: false],
    [deviceType: "ALT9P69K6LORD", family: "ALEXA_AUTO", generation: "Unspecified", modelName: "Echo Auto", imageFilename: "echo_auto", validated: false],
    [deviceType: "A13W6HQIHKEN3Z", family: "ALEXA_AUTO", generation: "Unspecified", modelName: "Echo Auto", imageFilename: "echo_auto", validated: false],
    [deviceType: "ADMKNMEVNL158", family: "KNIGHT", generation: "Unspecified", modelName: "Echo Hub", imageFilename: "echo_hub", validated: true],
    [deviceType: "A12GXV8XMS007S", family: "FIRE_TV", generation: "1", modelName: "Fire TV", imageFilename: "firetv_gen1", validated: false],
    [deviceType: "A2E0SNTXJVT7WK", family: "FIRE_TV", generation: "2", modelName: "Fire TV", imageFilename: "firetv_gen2", validated: false],
    [deviceType: "A2GFL5ZMWNE0PX", family: "FIRE_TV", generation: "3", modelName: "Fire TV", imageFilename: "firetv_gen3", validated: false],
    [deviceType: "A1P7E7V3FCZKU6", family: "FIRE_TV", generation: "3", modelName: "Fire TV", imageFilename: "firetv_gen3", validated: false],
    [deviceType: "A3IKB0DFR7GKZW", family: "FIRE_TV", generation: "3", modelName: "Fire TV", imageFilename: "firetv_gen3", validated: false],
    [deviceType: "A265XOI9586NML", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV Stick", imageFilename: "firetv_stick_gen1", validated: false],
    [deviceType: "ADVBD696BHNV5", family: "FIRE_TV", generation: "1", modelName: "Fire TV Stick", imageFilename: "firetv_stick_gen1", validated: false],
    [deviceType: "A2LWARUGJLBYEW", family: "FIRE_TV", generation: "2", modelName: "Fire TV Stick", imageFilename: "firetv_stick_gen1", validated: false],
    [deviceType: "AKPGW064GI9HE", family: "FIRE_TV", generation: "3", modelName: "Fire TV Stick 4K", imageFilename: "firetv_stick_gen1", validated: false],
    [deviceType: "A31DTMEEVDDOIV", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV Stick Lite", imageFilename: "unknown", validated: false],
    [deviceType: "A1WZKXFLI43K86", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV Stick 4K Max", imageFilename: "unknown", validated: false],
    [deviceType: "A3EVMLQTU6WL1W", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV Stick 4K Max", imageFilename: "unknown", validated: false],
    [deviceType: "A2JKHJ0PX4J3L3", family: "FIRE_TV_CUBE", generation: "2", modelName: "Fire TV Cube", imageFilename: "firetv_cube", validated: false],
    [deviceType: "A3HF4YRA2L7XGC", family: "FIRE_TV_CUBE", generation: "Unspecified", modelName: "Fire TV Cube", imageFilename: "firetv_cube", validated: false],
    [deviceType: "A1VGB7MHSIEYFK", family: "FIRE_TV_CUBE", generation: "3", modelName: "Fire TV Cube", imageFilename: "firetv_cube", validated: false],
    [deviceType: "AN630UQPG2CA4", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV Toshiba", imageFilename: "firetv_toshiba", validated: false],
    [deviceType: "A1F8D55J0FWDTN", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV Toshiba", imageFilename: "toshiba_firetv", validated: false],
    [deviceType: "AP4RS91ZQ0OOI", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV Toshiba", imageFilename: "toshiba_firetv", validated: false],
    [deviceType: "AFF5OAL5E3DIU", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV", imageFilename: "firetv_toshiba", validated: false],
    [deviceType: "AFF50AL5E3DIU", family: "FIRE_TV", generation: "Unspecified", modelName: "Fire TV Insignia", imageFilename: "insignia_firetv", validated: false],
    [deviceType: "A1Q7QCGNMXAKYW", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet", imageFilename: "amazon_tablet", validated: false],
    [deviceType: "ATNLRCEBX3W4P", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet", imageFilename: "amazon_tablet", validated: false],
    [deviceType: "A1J16TEDOYCZTN", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet", imageFilename: "amazon_tablet", validated: false],
    [deviceType: "A2M4YX06LWP8WI", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet", imageFilename: "amazon_tablet", validated: false],
    [deviceType: "A1C66CX2XD756O", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet HD", imageFilename: "amazon_tablet", validated: false],
    [deviceType: "A2N49KXGVA18AR", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet HD 10 Plus", imageFilename: "tablet_hd10", validated: false],
    [deviceType: "A3L0T0VL9A921N", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet HD 8", imageFilename: "tablet_hd10", validated: false],
    [deviceType: "AVU7CPPF2ZRAS", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet HD 8 Plus", imageFilename: "tablet_hd10", validated: false],
    [deviceType: "A38EHHIB10L47V", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet HD 8", imageFilename: "tablet_hd10", validated: false],
    [deviceType: "A3R9S4ZZECZ6YL", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet HD 10", imageFilename: "tablet_hd10", validated: false],
    [deviceType: "A2V9UEGZ82H4KZ", family: "TABLET", generation: "Unspecified", modelName: "Fire Tablet HD 10", imageFilename: "tablet_hd10", validated: false],
    [deviceType: "A3C9PE6TNYLTCH", family: "WHA", generation: "N/A", modelName: "Multiroom", imageFilename: "echo_wha", validated: true]
]

static String version() { return '2.1.8' }
def timeStamp() { return "2026/09/22 02:48 PM" }

metadata {
    definition (
        name: "Echos Speak Advanced W/Sensors Device",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/echos_speak_advanced_device/echos_speak_advanced_device_sensors.groovy"
    ) {
        capability "SpeechSynthesis"
        capability "AudioVolume"
        capability "TemperatureMeasurement"
        capability "MotionSensor"
        capability "Refresh"
        capability "Sensor"
        capability "Actuator"

        command "aRefresh"
        command "initialize"
        command "play"
        command "pause"
        command "stop"
        command "previousTrack"
        command "nextTrack"
        command "playText", [[name: "text*", type: "STRING", description: "Play text as speech"]]
        command "playTextAndRestore", [
            [name: "text*", type: "STRING", description: "Play text as speech"],
            [name: "volume", type: "NUMBER", description: "Optional temporary speech volume (0-100)"]
        ]
        command "setVolume", [[name: "volumeLevel*", type: "NUMBER", description: "Set speaker volume level (0-100)"]]
        command "volumeUp"
        command "volumeDown"
        command "storeCurrentVolume"
        command "restoreLastVolume"
        command "mute"
        command "unmute"
        command "togglePlayback"
        command "replayLastSpokenText"
        command "speak", [
            [name: "text*", type: "STRING", description: "Text to speak out loud"],
            [name: "voice", type: "STRING", description: "Optional voice profile"],
            [name: "volume", type: "NUMBER", description: "Optional temporary volume level (0-100)"],
            [name: "ignoreDnd", type: "ENUM", constraints: ["false", "true"], description: "Bypass Do Not Disturb mode"]
        ]
        command "playAnnouncement", [
            [name: "text*", type: "STRING", description: "Play text as an Alexa Announcement chime"],
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
        command "resetDriver"

        command "setDisplayBrightness", [[name: "level*", type: "NUMBER", description: "Display brightness level (0-100)"]]
        command "setDisplayPowerOn"
        command "setDisplayPowerOff"
        command "setAdaptiveBrightnessOn"
        command "setAdaptiveBrightnessOff"

        attribute "serialNumber", "string"
        attribute "family", "string"
        attribute "model", "string"
        attribute "generation", "string"
        attribute "modelNumber", "string"
        attribute "softwareVer", "string"
        attribute "amazonDeviceName", "string"

        attribute "onlineStatus", "string"
        attribute "doNotDisturb", "string"
        attribute "alexaWakeWord", "string"

        attribute "volume", "number"
        attribute "level", "number"
        attribute "mute", "string"
        attribute "status", "string"
        attribute "trackDescription", "string"
        attribute "trackData", "string"

        attribute "lastSpokenText", "string"
        attribute "lastAnnouncementText", "string"
        attribute "lastOutputType", "string"
        attribute "lastVoiceActivity", "string"

        attribute "iconFileName", "string"
        attribute "iconImage", "string"
        attribute "trackImage", "string"
        attribute "trackImageHtml", "string"

        attribute "displayBrightness", "number"
        attribute "displayPower", "string"
        attribute "adaptiveBrightness", "string"
        attribute "switch", "string"
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

def installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing device driver v${version()} (${timeStamp()})..."
    initializeDriverLifecycle(true)
}

def updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    initializeDriverLifecycle(false)
}

void initialize() {
    logInfo "Initialize requested"
    if (parent) {
        logDebug "Re-establishing baseline capabilities and device identity from parent bridge..."
        parent.fetchDeviceFeatures(device.deviceNetworkId)
    }
    refresh()
}

private void initializeDriverLifecycle(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    if (state.containsKey("capabilities")) {
        logDebug "Removing legacy state.capabilities entry in favor of state.capabilitiesMap"
        state.remove("capabilities")
    }

    device.deleteCurrentState("deviceStatus")
    device.deleteCurrentState("transportStatus")

    if (device.currentValue("serialNumber") == null) sendEvent(name: "serialNumber", value: device.deviceNetworkId)
    if (device.currentValue("family") == null) sendEvent(name: "family", value: "ECHO")
    if (device.currentValue("model") == null) sendEvent(name: "model", value: "Echo")
    if (device.currentValue("generation") == null) sendEvent(name: "generation", value: "Unknown")
    if (device.currentValue("modelNumber") == null) sendEvent(name: "modelNumber", value: "Unknown")
    if (device.currentValue("softwareVer") == null) sendEvent(name: "softwareVer", value: "Unknown")
    if (device.currentValue("amazonDeviceName") == null) sendEvent(name: "amazonDeviceName", value: "None")

    if (device.currentValue("onlineStatus") == null) sendEvent(name: "onlineStatus", value: "online")
    if (device.currentValue("doNotDisturb") == null) sendEvent(name: "doNotDisturb", value: "disabled")
    if (device.currentValue("alexaWakeWord") == null) sendEvent(name: "alexaWakeWord", value: "ALEXA")

    if (device.currentValue("volume") == null) sendEvent(name: "volume", value: 50, unit: "%")
    if (device.currentValue("level") == null) sendEvent(name: "level", value: 50, unit: "%")
    if (device.currentValue("mute") == null) sendEvent(name: "mute", value: "unmuted")
    if (device.currentValue("status") == null) sendEvent(name: "status", value: "stopped")
    if (device.currentValue("trackDescription") == null) sendEvent(name: "trackDescription", value: "Stopped")
    if (device.currentValue("trackData") == null) sendEvent(name: "trackData", value: "{}")

    if (device.currentValue("lastSpokenText") == null) sendEvent(name: "lastSpokenText", value: "None")
    if (device.currentValue("lastAnnouncementText") == null) sendEvent(name: "lastAnnouncementText", value: "None")
    if (device.currentValue("lastOutputType") == null) sendEvent(name: "lastOutputType", value: "speak")
    if (device.currentValue("lastVoiceActivity") == null) sendEvent(name: "lastVoiceActivity", value: "None")

    if (device.currentValue("iconFileName") == null) sendEvent(name: "iconFileName", value: "None")
    if (device.currentValue("iconImage") == null) sendEvent(name: "iconImage", value: "None")
    if (device.currentValue("trackImage") == null) sendEvent(name: "trackImage", value: "None")
    if (device.currentValue("trackImageHtml") == null) sendEvent(name: "trackImageHtml", value: "None")

    if (state.storedVolume == null) state.storedVolume = 50

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

    if (parent) {
        logDebug "Re-establishing baseline capabilities and device identity from parent bridge..."
        parent.fetchDeviceFeatures(device.deviceNetworkId)
    }

    refresh()
}

void aRefresh() {
    logInfo "GUI Action: [aRefresh] button clicked."
    refresh()
}

def refresh() {
    logInfo "GUI Action: [Refresh] button clicked."
    logTrace "--> refresh() Initiated -> Requesting device and media refresh..."
    parent?.refreshDeviceData(device.deviceNetworkId)
    Map mediaData = parent?.fetchDeviceMediaState(device.deviceNetworkId)
    if (mediaData && mediaData.success == true && mediaData.result) {
        parseDeviceData(mediaData.result)
    }
    logTrace "<-- refresh() Completed"
}

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

void playText(String text) {
    if (!text) return
    logInfo "GUI Action: [playText] triggered -> Routing text through speak(): \"${text}\""
    speak(text)
}

void playTextAndRestore(String text, Object volume = null) {
    if (!text) return
    logInfo "GUI Action: [playTextAndRestore] triggered -> Routing text through speak() with volume (${volume ?: 'current'}%): \"${text}\""
    speak(text, null, volume, false)
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
    if (!hasServerCapability("speechSynthesis")) {
        logWarn "${device.displayName} - speak() suppressed: Server capability contract indicates speechSynthesis is unsupported."
        return
    }
    logInfo "GUI Action: [speak] command triggered -> Text: \"${text}\"${voice ? " | Voice: ${voice}" : ""}${volume != null ? " | Volume: ${volume}%" : ""}${ignoreDnd ? " | Ignore DND: true" : ""}"
    logTrace "--> speak() Initiated"
    
    Boolean didBypass = processDndBypass(ignoreDnd)

    if (volume != null && volume.toString().isNumber()) {
        int targetVol = Math.max(0, Math.min(100, volume.toBigDecimal().intValue()))
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

void playAnnouncement(String text, Object ignoreDnd = false) {
    if (!text) return
    if (!hasServerCapability("audioNotification")) {
        logWarn "${device.displayName} - playAnnouncement() suppressed: Server capability contract indicates audioNotification is unsupported."
        return
    }
    logInfo "GUI Action: [playAnnouncement] triggered -> Text: \"${text}\"${ignoreDnd ? " | Ignore DND: true" : ""}"
    logTrace "--> playAnnouncement() Initiated"
    Boolean didBypass = processDndBypass(ignoreDnd)
    processOutputPayload(text, "announce", [text: text])
    scheduleDndRestore(text, didBypass)
    logTrace "<-- playAnnouncement() Completed"
}

void playAnnouncementAndRestore(String text, Object announceVolume = null, Object ignoreDnd = false) {
    if (!text) return
    if (!hasServerCapability("audioNotification")) {
        logWarn "${device.displayName} - playAnnouncementAndRestore() suppressed: Server capability contract indicates audioNotification is unsupported."
        return
    }
    logInfo "GUI Action: [playAnnouncementAndRestore] triggered -> Text: \"${text}\" | Vol: ${announceVolume}%${ignoreDnd ? " | Ignore DND: true" : ""}"
    logTrace "--> playAnnouncementAndRestore() Initiated"

    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = (announceVolume != null && announceVolume.toString().isNumber()) ? Math.max(0, Math.min(100, announceVolume.toBigDecimal().intValue())) : currentVol

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

void replayLastSpokenText() {
    logInfo "GUI Action: [replayLastSpokenText] button clicked."
    logTrace "--> replayLastSpokenText() Initiated"
    String lastType = state.lastOutputType ?: device.currentValue("lastOutputType") ?: "speak"

    if (lastType == "announce") {
        String lastAnn = state.lastAnnouncementText ?: device.currentValue("lastAnnouncementText")
        if (lastAnn && lastAnn != "None") {
            logInfo "${device.displayName} - Replaying last announcement: \"${lastAnn}\""
            playAnnouncement(lastAnn)
        } else {
            logWarn "${device.displayName} - Replay requested for announcement, but no previous announcement text is stored."
        }
    } else {
        String lastSpoken = state.lastSpokenText ?: device.currentValue("lastSpokenText")
        if (lastSpoken && lastSpoken != "None") {
            logInfo "${device.displayName} - Replaying last spoken text: \"${lastSpoken}\""
            speak(lastSpoken)
        } else {
            logWarn "${device.displayName} - Replay requested, but no previous spoken text is stored."
        }
    }
    logTrace "<-- replayLastSpokenText() Completed"
}

private void processOutputPayload(String text, String commandType, Map payload) {
    if (!text) {
        logWarn "${device.displayName} - Suppressed empty ${commandType} request."
        return
    }

    logTrace "--> processOutputPayload() [${commandType}] -> Text: '${text}'"
    state.lastOutputType = commandType
    sendEvent(name: "lastOutputType", value: commandType, isStateChange: true)

    if (commandType == "announce") {
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
    if (parent) {
        parent.sendBridgeCommand(device.deviceNetworkId, "voiceCmdAsText", [text: text])
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
    logTrace "<-- voiceCmdAsText() Completed"
}

private Boolean hasServerCapability(String capKey) {
    Map caps = state.capabilitiesMap ?: [:]
    return (caps[capKey] == true)
}

void setDisplayBrightness(level) {
    if (level == null) return
    if (!hasServerCapability("displayBrightness") && !hasServerCapability("display")) {
        logWarn "${device.displayName} - setDisplayBrightness() suppressed: Server capability contract indicates displayBrightness is unsupported."
        return
    }
    int targetVal = Math.max(0, Math.min(100, level.toBigDecimal().intValue()))
    logInfo "GUI Action: [setDisplayBrightness] -> Target: ${targetVal}%"
    
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setDisplayBrightness", [level: targetVal])
    if (success != false) {
        sendEvent(name: "displayBrightness", value: targetVal, unit: "%")
        sendEvent(name: "level", value: targetVal, unit: "%")
    }
}

void setDisplayPowerOn() {
    if (!hasServerCapability("display")) {
        logWarn "${device.displayName} - setDisplayPowerOn() suppressed: Server capability contract indicates display is unsupported."
        return
    }
    logInfo "GUI Action: [setDisplayPowerOn]"
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setDisplayPowerOn", [:])
    if (success != false) {
        sendEvent(name: "displayPower", value: "on")
        sendEvent(name: "switch", value: "on")
    }
}

void setDisplayPowerOff() {
    if (!hasServerCapability("display")) {
        logWarn "${device.displayName} - setDisplayPowerOff() suppressed: Server capability contract indicates display is unsupported."
        return
    }
    logInfo "GUI Action: [setDisplayPowerOff]"
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setDisplayPowerOff", [:])
    if (success != false) {
        sendEvent(name: "displayPower", value: "off")
        sendEvent(name: "switch", value: "off")
    }
}

void on() { setDisplayPowerOn() }
void off() { setDisplayPowerOff() }

void setAdaptiveBrightnessOn() {
    if (!hasServerCapability("adaptiveBrightness")) {
        logWarn "${device.displayName} - setAdaptiveBrightnessOn() suppressed: Server capability contract indicates adaptiveBrightness is unsupported."
        return
    }
    logInfo "GUI Action: [setAdaptiveBrightnessOn]"
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setAdaptiveBrightnessOn", [:])
    if (success != false) {
        sendEvent(name: "adaptiveBrightness", value: "on")
    }
}

void setAdaptiveBrightnessOff() {
    if (!hasServerCapability("adaptiveBrightness")) {
        logWarn "${device.displayName} - setAdaptiveBrightnessOff() suppressed: Server capability contract indicates adaptiveBrightness is unsupported."
        return
    }
    logInfo "GUI Action: [setAdaptiveBrightnessOff]"
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setAdaptiveBrightnessOff", [:])
    if (success != false) {
        sendEvent(name: "adaptiveBrightness", value: "off")
    }
}

void play() {
    if (!hasServerCapability("musicPlayer")) {
        logWarn "${device.displayName} - play() suppressed: Server capability contract indicates musicPlayer is unsupported."
        return
    }
    logInfo "GUI Action: [play] command triggered."
    logTrace "--> play() Initiated"
    if (parent) {
        Boolean success = parent.sendBridgeCommand(device.deviceNetworkId, "play", [:])
        if (!success) {
            logError "${device.displayName} - play() command HTTP request failed to execute on bridge server."
        }
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
    logTrace "<-- play() Transport Dispatch Completed"
}

void pause() {
    if (!hasServerCapability("musicPlayer")) {
        logWarn "${device.displayName} - pause() suppressed: Server capability contract indicates musicPlayer is unsupported."
        return
    }
    logInfo "GUI Action: [pause] command triggered."
    logTrace "--> pause() Initiated"
    if (parent) {
        Boolean success = parent.sendBridgeCommand(device.deviceNetworkId, "pause", [:])
        if (!success) {
            logError "${device.displayName} - pause() command HTTP request failed to execute on bridge server."
        }
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
    logTrace "<-- pause() Transport Dispatch Completed"
}

void stop() {
    if (!hasServerCapability("musicPlayer")) {
        logWarn "${device.displayName} - stop() suppressed: Server capability contract indicates musicPlayer is unsupported."
        return
    }
    logInfo "GUI Action: [stop] command triggered."
    logTrace "--> stop() Initiated"
    if (parent) {
        Boolean success = parent.sendBridgeCommand(device.deviceNetworkId, "stop", [:])
        if (!success) {
            logError "${device.displayName} - stop() command HTTP request failed to execute on bridge server."
        }
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
    logTrace "<-- stop() Transport Dispatch Completed"
}

void previousTrack() {
    if (!hasServerCapability("musicPlayer")) {
        logWarn "${device.displayName} - previousTrack() suppressed: Server capability contract indicates musicPlayer is unsupported."
        return
    }
    logInfo "GUI Action: [previousTrack] command triggered."
    logTrace "--> previousTrack() Initiated"
    if (parent) {
        Boolean success = parent.sendBridgeCommand(device.deviceNetworkId, "previousTrack", [:])
        if (!success) {
            logError "${device.displayName} - previousTrack() command HTTP request failed to execute on bridge server."
        }
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
    logTrace "<-- previousTrack() Transport Dispatch Completed"
}

void nextTrack() {
    if (!hasServerCapability("musicPlayer")) {
        logWarn "${device.displayName} - nextTrack() suppressed: Server capability contract indicates musicPlayer is unsupported."
        return
    }
    logInfo "GUI Action: [nextTrack] command triggered."
    logTrace "--> nextTrack() Initiated"
    if (parent) {
        Boolean success = parent.sendBridgeCommand(device.deviceNetworkId, "nextTrack", [:])
        if (!success) {
            logError "${device.displayName} - nextTrack() command HTTP request failed to execute on bridge server."
        }
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
    logTrace "<-- nextTrack() Transport Dispatch Completed"
}

void togglePlayback() {
    if (!hasServerCapability("musicPlayer")) {
        logWarn "${device.displayName} - togglePlayback() suppressed: Server capability contract indicates musicPlayer is unsupported."
        return
    }
    logInfo "GUI Action: [togglePlayback] button clicked."
    logTrace "--> togglePlayback() Initiated -> Querying live server media endpoint..."
    if (parent) {
        Map mediaData = parent.fetchDeviceMediaState(device.deviceNetworkId)
        handleMediaStateAndToggle(mediaData)
    } else {
        logError "${device.displayName} - Parent application reference missing."
    }
    logTrace "<-- togglePlayback() Transport Dispatch Completed"
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
    if (hasServerCapability("displayBrightness") || hasServerCapability("display")) {
        setDisplayBrightness(level)
    } else {
        logInfo "GUI Action: [setLevel] triggered -> Volume Level: ${level}%"
        setVolume(level)
    }
}

void setVolume(volumeLevel) {
    if (volumeLevel == null) return
    if (!hasServerCapability("audioVolume")) {
        logWarn "${device.displayName} - setVolume() suppressed: Server capability contract indicates audioVolume is unsupported."
        return
    }
    int vol = Math.max(0, Math.min(100, volumeLevel.toBigDecimal().intValue()))

    logInfo "GUI Action: [setVolume] triggered -> Target Level: ${vol}%"
    logTrace "--> setVolume() Initiated"
    sendEvent(name: "volume", value: vol, unit: "%")
    if (!hasServerCapability("displayBrightness") && !hasServerCapability("display")) {
        sendEvent(name: "level", value: vol, unit: "%")
    }
    
    parent?.sendBridgeCommand(device.deviceNetworkId, "volume", [level: vol])
    logTrace "<-- setVolume() Completed"
}

void volumeUp() {
    if (!hasServerCapability("audioVolume")) {
        logWarn "${device.displayName} - volumeUp() suppressed: Server capability contract indicates audioVolume is unsupported."
        return
    }
    logInfo "GUI Action: [volumeUp] button clicked."
    logTrace "--> volumeUp() Initiated"
    int currentVol = device.currentValue("volume") != null ? device.currentValue("volume").toInteger() : 75
    int targetVol = Math.min(100, currentVol + 5)
    logDebug "volumeUp() calculated target: ${targetVol}% (from ${currentVol}%)"
    setVolume(targetVol)
    logTrace "<-- volumeUp() Completed"
}

void volumeDown() {
    if (!hasServerCapability("audioVolume")) {
        logWarn "${device.displayName} - volumeDown() suppressed: Server capability contract indicates audioVolume is unsupported."
        return
    }
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
    if (!hasServerCapability("audioVolume")) {
        logWarn "${device.displayName} - mute() suppressed: Server capability contract indicates audioVolume is unsupported."
        return
    }
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
    if (!hasServerCapability("displayBrightness") && !hasServerCapability("display")) {
        sendEvent(name: "level", value: 0, unit: "%")
    }
    
    parent?.sendBridgeCommand(device.deviceNetworkId, "volume", [level: 0])
    logTrace "<-- mute() Completed"
}

void unmute() {
    if (!hasServerCapability("audioVolume")) {
        logWarn "${device.displayName} - unmute() suppressed: Server capability contract indicates audioVolume is unsupported."
        return
    }
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

void parseDeviceFeatures(Map resp) {
    if (!resp) return
    Map payload = (resp.result instanceof Map) ? resp.result : resp
    logTrace "--> parseDeviceFeatures() Processing server feature payload for ${device.displayName}: ${payload}"

    Map caps  = (payload.capabilities instanceof Map) ? payload.capabilities : [:]
    Map st    = (payload.state instanceof Map)        ? payload.state        : [:]
    Map errs  = (payload.errors instanceof Map)       ? payload.errors       : [:]

    state.capabilitiesMap = [
        temperatureMeasurement: (caps.temperatureMeasurement == true || caps.temperature == true),
        motionSensor:          (caps.motionSensor == true || caps.motion == true),
        displayBrightness:     (caps.displayBrightness == true || caps.brightness == true),
        musicPlayer:           (caps.musicPlayer == true),
        audioVolume:           (caps.audioVolume == true),
        speechSynthesis:       (caps.speechSynthesis == true),
        audioNotification:     (caps.audioNotification == true),
        notification:          (caps.notification == true),
        display:               (caps.display == true || caps.displayPower == true),
        adaptiveBrightness:    (caps.adaptiveBrightness == true)
    ]

    if (st.temperature != null && st.temperature.toString().isNumber()) {
        BigDecimal rawTemp = st.temperature.toBigDecimal()
        String rawScale = st.temperatureScale?.toString()?.toUpperCase() ?: "CELSIUS"
        String locationScale = location?.temperatureScale ?: "F"

        BigDecimal finalTemp = rawTemp
        if (rawScale in ["CELSIUS", "C"] && locationScale == "F") {
            finalTemp = (rawTemp * 1.8 + 32).setScale(1, BigDecimal.ROUND_HALF_UP)
        } else if (rawScale in ["FAHRENHEIT", "F"] && locationScale == "C") {
            finalTemp = ((rawTemp - 32) / 1.8).setScale(1, BigDecimal.ROUND_HALF_UP)
        }

        sendEvent(name: "temperature", value: finalTemp, unit: "°${locationScale}")
    }

    if (st.motion != null) {
        String rawMot = st.motion.toString().trim().toUpperCase()
        String cleanMotion = (rawMot in ["DETECTED", "ACTIVE", "TRUE", "MOTION"]) ? "active" : "inactive"
        sendEvent(name: "motion", value: cleanMotion)
    }

    if (st.displayBrightness != null && st.displayBrightness.toString().isNumber()) {
        int brightVal = Math.max(0, Math.min(100, st.displayBrightness.toInteger()))
        sendEvent(name: "displayBrightness", value: brightVal, unit: "%")
        sendEvent(name: "level", value: brightVal, unit: "%")
    }

    if (st.displayPower != null) {
        Boolean pwr = st.displayPower.toBoolean()
        String switchState = pwr ? "on" : "off"
        sendEvent(name: "displayPower", value: switchState)
        sendEvent(name: "switch", value: switchState)
    }

    if (st.adaptiveBrightness != null) {
        Boolean adapt = st.adaptiveBrightness.toBoolean()
        sendEvent(name: "adaptiveBrightness", value: adapt ? "on" : "off")
    }

    if (errs && errs.size() > 0) {
        logWarn "${device.displayName} - Server reported feature query errors: ${errs}"
    }
}

void parseDeviceData(Map devData) {
    if (!devData) return
    logTrace "--> parseDeviceData() Processing payload for ${device.displayName}"

    String sNum = devData.serialNumber?.toString() ?: device.deviceNetworkId
    if (sNum) sendEvent(name: "serialNumber", value: sNum)

    String devType = (devData.modelNumber ?: devData.deviceType ?: "").toString()
    Map matchedTypeInfo = devType ? deviceModelTable.find { it.deviceType == devType } : null

    String famVal = devData.family ?: devData.deviceFamily ?: matchedTypeInfo?.family ?: "ECHO"
    sendEvent(name: "family", value: famVal.toString().toUpperCase())

    String modelVal = matchedTypeInfo?.modelName ?: devData.model ?: devData.deviceStyle ?: "Echo"
    sendEvent(name: "model", value: modelVal.toString())

    String genVal = matchedTypeInfo?.generation != null ? matchedTypeInfo.generation.toString() : (devData.generation?.toString() ?: "Unknown")
    sendEvent(name: "generation", value: genVal.toString())

    sendEvent(name: "modelNumber", value: devType ?: "Unknown")

    if (devData.softwareVer || devData.softwareVersion || devData.firmwareVer) {
        sendEvent(name: "softwareVer", value: (devData.softwareVer ?: devData.softwareVersion ?: devData.firmwareVer).toString())
    }

    String accountDevName = devData.accountName ?: devData.deviceName ?: devData.label
    if (accountDevName) {
        sendEvent(name: "amazonDeviceName", value: accountDevName.toString())
    }

    if (devData.onlineStatus != null || devData.online != null) {
        Boolean isOnline = (devData.onlineStatus == "online" || devData.online == true)
        String onlineStr = isOnline ? "online" : "offline"
        sendEvent(name: "onlineStatus", value: onlineStr)
    }

    if (devData.doNotDisturb != null || devData.dnd != null) {
        Boolean isDnd = (devData.doNotDisturb == true || devData.doNotDisturb == "enabled" || devData.dnd == true)
        sendEvent(name: "doNotDisturb", value: isDnd ? "enabled" : "disabled")
    }

    if (devData.alexaWakeWord || devData.wakeWord) {
        sendEvent(name: "alexaWakeWord", value: (devData.alexaWakeWord ?: devData.wakeWord).toString().toUpperCase())
    }

    // AUTHORITATIVE VOLUME STATE SYNC
    if (devData.currentVolume != null || devData.volume != null) {
        int vol = (devData.currentVolume != null ? devData.currentVolume : devData.volume).toInteger()
        sendEvent(name: "volume", value: vol, unit: "%")
        if (!hasServerCapability("displayBrightness") && !hasServerCapability("display")) {
            sendEvent(name: "level", value: vol, unit: "%")
        }
    }

    // TRANSIENT SERVER TRANSACTION PROTOCOL & EXPLICIT DEBUG LOGGING
    if (devData.volumeCommandId != null) {
        String cmdId = devData.volumeCommandId.toString()
        String cmdState = devData.volumeCommandState?.toString()?.toLowerCase() ?: "unknown"
        String cmdError = devData.volumeCommandError?.toString() ?: ""
        String lastKnownId = state.lastVolumeCommandId?.toString() ?: ""

        if (cmdState == "failed") {
            if (cmdId != lastKnownId) {
                logWarn "${device.displayName} - Volume transaction [${cmdId}] reported failure from server: ${cmdError}"
                state.lastVolumeCommandId = cmdId
            }
        } else if (cmdState == "success") {
            if (cmdId != lastKnownId) {
                state.lastVolumeCommandId = cmdId
                String successTarget = devData.volumeCommandTarget?.toString() ?: "N/A"
                logDebug "Volume transaction [${cmdId}] verified SUCCESS by server -> Target: ${successTarget}%"
            }
        } else if (cmdState == "pending") {
            state.lastVolumeCommandId = cmdId
        }
    }

    if (devData.muted != null) {
        Boolean isMuted = devData.muted.toBoolean()
        sendEvent(name: "mute", value: isMuted ? "muted" : "unmuted")
    }

    if (devData.state != null || devData.playing != null) {
        Boolean isPlaying = (devData.playing == true || devData.state?.toString()?.toUpperCase() == "PLAYING")
        String transport = isPlaying ? "playing" : (devData.state?.toString()?.toLowerCase() ?: "stopped")
        if (devData.state?.toString()?.toUpperCase() in ["PAUSED", "IDLE"]) transport = "stopped"
        sendEvent(name: "status", value: transport)
    }

    String title  = devData.title?.toString() ?: ""
    String artist = devData.artist?.toString() ?: ""
    String album  = devData.album?.toString() ?: ""

    if (title || artist) {
        String desc = (artist && title) ? "${artist} - ${title}" : (title ?: artist)
        sendEvent(name: "trackDescription", value: desc)
        Map trackMap = [title: title, artist: artist, album: album]
        sendEvent(name: "trackData", value: groovy.json.JsonOutput.toJson(trackMap))
    } else if (devData.playing == false || devData.state == "PAUSED" || devData.state == "IDLE" || devData.state == "STOPPED") {
        sendEvent(name: "status", value: "stopped")
        sendEvent(name: "trackDescription", value: "Stopped")
    }

    if (devData.lastVoiceActivity || devData.phraseSpoken) {
        sendEvent(name: "lastVoiceActivity", value: (devData.lastVoiceActivity ?: devData.phraseSpoken).toString())
    }

    String rawFilename = matchedTypeInfo?.imageFilename ?: devData.icon ?: devData.deviceIcon
    if (rawFilename) {
        String cleanBaseFilename = rawFilename.replaceAll(/\.(png|jpg|svg)$/, "")
        String iconFileNameVal = "${cleanBaseFilename}.png"
        sendEvent(name: "iconFileName", value: iconFileNameVal)

        String basePath = parent ? parent.getIconBasePath() : "http://192.168.1.12/myicons/echos_speak/"
        String fullIconUrl = rawFilename.startsWith("http") ? rawFilename : "${basePath}${iconFileNameVal}"
        sendEvent(name: "iconImage", value: "<img src='${fullIconUrl}' style='max-height:100px; max-width:100px;'/>")
    } else {
        if (device.currentValue("iconFileName") == null) sendEvent(name: "iconFileName", value: "None")
        if (device.currentValue("iconImage") == null) sendEvent(name: "iconImage", value: "None")
    }

    if (devData.trackImage != null) {
        sendEvent(name: "trackImage", value: devData.trackImage.toString())
    }

    if (devData.trackImageHtml != null) {
        sendEvent(name: "trackImageHtml", value: devData.trackImageHtml.toString())
    } else if (devData.trackImage || devData.albumArtUrl) {
        String imgUrl = (devData.trackImage ?: devData.albumArtUrl).toString()
        sendEvent(name: "trackImageHtml", value: "<img src='${imgUrl}' style='max-height:100px; max-width:100px;'/>")
    }

    logTrace "<-- parseDeviceData() Completed"
}

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
    initializeDriverLifecycle(false)
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