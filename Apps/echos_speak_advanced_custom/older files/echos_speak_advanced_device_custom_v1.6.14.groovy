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
 *  v1.6.14 (2026-09-18) - Integer Clamp Method Exception Resolution:
 *                          - Replaced unsupported .clamp(0, 100) calls with standard Math.max(0, Math.min(100, val)) bounds clamping.
 *                          - Fixed parseDeviceFeatures() runtime MissingMethodException when parsing displayBrightness for Echo Dot Gen 5 w/Clock devices.
 *  v1.6.13 (2026-09-18) - Response Safety Unwrap & Platform Capability Declarations:
 *                          - Added automatic payload unwrapping for result objects passed to parseDeviceFeatures().
 *                          - Declared TemperatureMeasurement and MotionSensor capabilities in driver metadata to ensure proper platform indexing.
 *  v1.6.12 (2026-09-18) - Canonical Server Contract Alignment (getDeviceFeatures):
 *                          - Refactored parseDeviceFeatures() to consume official {capabilities, state, errors} payload structure.
 *                          - Implemented dynamic state attribute cleanup for unsupported capabilities (temperature, motion, display, displayBrightness, adaptiveBrightness).
 *                          - Aligned setDisplayBrightness command dispatch with explicit level payload structure ({level: 0..100}).
 *                          - Aligned adaptive brightness command interfaces (setAdaptiveBrightnessOn / setAdaptiveBrightnessOff).
 *  v1.6.11 (2026-09-18) - Device Generation Attribute & Model Table String Standardization:
 *                          - Updated all generation values in deviceModelTable to explicit String literals.
 *                          - Verified parseDeviceData() string coercion and generation state event formatting.
 *  v1.6.10 (2026-09-18) - Device Model Table Refresh:
 *                          - Integrated updated deviceModelTable structure with formatted multi-line entries and stringified non-numeric generation tokens ("Unspecified", "N/A").
 *  v1.6.9 (2026-09-18) - Parent App v1.5.6 Version Demarcation Alignment:
 *                         - Incremented driver version to reflect alignment with Parent App v1.5.6 GUI label ordering updates.
 *  v1.6.8 (2026-09-18) - Amazon Account Device Name Reference Attribute:
 *                         - Added 'amazonDeviceName' attribute to preserve and display the authoritative reference device name as reported by Amazon.
 *                         - Updated parseDeviceData() to capture accountName / deviceName payload values.
 *  v1.6.7 (2026-09-18) - Hardware Identification & Model Lookup Refresh:
 *                         - Replaced embedded map with validated deviceModelTable static map.
 *                         - Maintained Echo Server API-driven capabilities checking while utilizing deviceModelTable for lookup of Model name, assigned image, and generation.
 *  v1.6.6 (2026-09-17) - Capability-Aware Display & Sensor Integration (Server v1.2.8):
 *                         - Implemented dynamic capability guards for DISPLAY_BRIGHTNESS_ADJUST, DISPLAY_POWER_TOGGLE, DISPLAY_ADAPTIVE_BRIGHTNESS, MOTION_DETECTION, and TEMPERATURE_SENSOR.
 *                         - Exposed displayBrightness, displayPower, adaptiveBrightness, motion, and temperature attributes.
 *                         - Exposed setDisplayBrightness(level), setDisplayPowerOn(), setDisplayPowerOff(), setAdaptiveBrightnessOn(), setAdaptiveBrightnessOff() commands.
 *                         - Added location temperature scale auto-conversion (°F / °C).
 *                         - Purged debug attributes (rawCapabilities, liveViewSupported, mediaDisplaySupported).
 *                         - Ensured dynamic in-place attribute migration without device recreation.
 *  v1.6.3 (2026-09-17) - Attribute Terminology Alignment & UI Optimization:
 *                         - Renamed firmwareVer attribute to softwareVer to align with Amazon Alexa app terminology.
 *                         - Replaced empty string defaults with "None" for all string attributes to prevent platform event suppression.
 *  v1.6.2 (2026-09-17) - UI Visibility & Attribute Rendering Fixes:
 *                         - Resolved empty string default issues for attribute rendering to ensure visibility in Current States.
 *  v1.6.1 (2026-09-17) - Attribute Seeding Engine:
 *                         - Introduced comprehensive attribute seeding across initialization routines to guarantee default state presence.
 *  v1.6.0 (2026-09-17) - Dual-Track Interaction Engine & Attribute Matrix Architecture:
 *                         - Finalized core attribute matrix and dual-track speech and announcement execution pipelines.
 *                         - Added WHA hardware safety guards for global announcements.
 *  v1.5.11 (2026-09-16) - Mute Command Payload Dispatch Refactor:
 *                         - Refactored mute() to send explicit level 0 volume commands to bridge server, preventing HTTP 400 errors.
 *
 *				[KEEP] Prior change history is found in changelog_device.txt (v0.0.0 - v1.5.10)
 **/

import groovy.transform.Field

@Field static final List<Map> deviceModelTable = [

    // ============================================================
    // ECHO / ECHO DOT / ECHO SPOT / ECHO PLUS
    // ============================================================

    [deviceType: "AB72C64C86AW2", family: "ECHO", generation: "1",
     modelName: "Echo", imageFilename: "echo_gen1", validated: true],

    [deviceType: "A7WXQPH584YP", family: "ECHO", generation: "2",
     modelName: "Echo", imageFilename: "echo_gen2", validated: true],

    [deviceType: "A3FX4UWTP28V1P", family: "ECHO", generation: "3",
     modelName: "Echo", imageFilename: "echo_gen3", validated: true],

    [deviceType: "A3RMGO6LYLH7YN", family: "ECHO", generation: "4",
     modelName: "Echo", imageFilename: "echo_gen4", validated: true],

    [deviceType: "A2M35JJZWCQOMZ", family: "ECHO", generation: "1",
     modelName: "Echo Plus", imageFilename: "echo_plus_gen1", validated: true],

    [deviceType: "A18O6U1UQFJ0XK", family: "ECHO", generation: "2",
     modelName: "Echo Plus", imageFilename: "echo_plus_gen2", validated: true],

    [deviceType: "ASQZWP4GPYUT7", family: "ECHO", generation: "Unspecified",
     modelName: "Echo Pop", imageFilename: "echo_pop", validated: true],

    // Echo Dots

    [deviceType: "AKNO1N0KSFN8L", family: "ECHO", generation: "1",
     modelName: "Echo Dot", imageFilename: "echo_dot_gen1", validated: true],

    [deviceType: "A3S5BH2HU6VAYF", family: "ECHO", generation: "2",
     modelName: "Echo Dot", imageFilename: "echo_dot_gen2", validated: true],

    [deviceType: "A32DDESGESSHZA", family: "ECHO", generation: "3",
     modelName: "Echo Dot", imageFilename: "echo_dot_gen3", validated: true],

    [deviceType: "A32DOYMUN6DTXA", family: "ECHO", generation: "3",
     modelName: "Echo Dot", imageFilename: "echo_dot_gen3", validated: true],

    [deviceType: "A1RABVCI4QCIKC", family: "ECHO", generation: "3",
     modelName: "Echo Dot", imageFilename: "echo_dot_gen3", validated: true],

    [deviceType: "A2U21SRK4QGSE1", family: "ECHO", generation: "4",
     modelName: "Echo Dot", imageFilename: "echo_dot_gen4", validated: true],

    [deviceType: "A4ZXE0RM7LQ7A", family: "ECHO", generation: "5",
     modelName: "Echo Dot", imageFilename: "echo_dot_gen5", validated: true],

    // Echo Dots with Clock

    [deviceType: "A30YDR2MK8HMRV", family: "ECHO", generation: "3",
     modelName: "Echo Dot with Clock", imageFilename: "echo_dot_clock_gen3", validated: true],

    [deviceType: "A2H4LV5GIZ1JFT", family: "ECHO", generation: "4",
     modelName: "Echo Dot with Clock", imageFilename: "echo_dot_clock_gen4", validated: true],

    [deviceType: "A2U21SRK4QGSE1", family: "ECHO", generation: "4",
     modelName: "Echo Dot", imageFilename: "echo_dot_gen4", validated: true],

    [deviceType: "A2DS1Q2TPDJ48U", family: "ECHO", generation: "5",
     modelName: "Echo Dot with Clock", imageFilename: "echo_dot_clock_gen5", validated: true],

    // Echo Spot

    [deviceType: "A10A33FOX2NUBK", family: "ROOK", generation: "1",
     modelName: "Echo Spot", imageFilename: "echo_spot_gen1", validated: true],

    [deviceType: "A3EH2E0YZ30OD6", family: "ROOK", generation: "2",
     modelName: "Echo Spot", imageFilename: "echo_spot_gen2", validated: true],

    // ============================================================
    // ECHO SHOW
    // ============================================================

    [deviceType: "A1NL4BVLQ4L3N3", family: "KNIGHT", generation: "1",
     modelName: "Echo Show", imageFilename: "echo_show_gen1", validated: true],

    [deviceType: "AWZZ5CVHX2CD", family: "KNIGHT", generation: "2",
     modelName: "Echo Show", imageFilename: "echo_show_gen2", validated: true],

    [deviceType: "A4ZP7ZC4PI6TO", family: "KNIGHT", generation: "1",
     modelName: "Echo Show 5", imageFilename: "echo_show_5", validated: true],

    [deviceType: "A1XWJRHALS1REP", family: "KNIGHT", generation: "2",
     modelName: "Echo Show 5", imageFilename: "echo_show_5", validated: true],

    [deviceType: "A11QM4H9HGV71H", family: "KNIGHT", generation: "3",
     modelName: "Echo Show 5", imageFilename: "echo_show_5", validated: true],

    [deviceType: "A1Z88NGR2BK6A2", family: "KNIGHT", generation: "1",
     modelName: "Echo Show 8", imageFilename: "echo_show_8", validated: true],

    [deviceType: "A15996VY63BQ2D", family: "KNIGHT", generation: "2",
     modelName: "Echo Show 8", imageFilename: "echo_show_8", validated: true],

    [deviceType: "A2UONLFQW0PADH", family: "KNIGHT", generation: "3",
     modelName: "Echo Show 8", imageFilename: "echo_show_8", validated: true],

    [deviceType: "A345JHMQDGG1M5", family: "KNIGHT", generation: "4",
     modelName: "Echo Show 8", imageFilename: "echo_show_8", validated: true],

    [deviceType: "AIPK7MM90V7TB", family: "KNIGHT", generation: "3",
     modelName: "Echo Show 10", imageFilename: "echo_show_10_gen3", validated: true],

    [deviceType: "A1EIANJ7PNB0Q7", family: "KNIGHT", generation: "1",
     modelName: "Echo Show 15", imageFilename: "echo_show_15", validated: true],

    [deviceType: "AQ24620N8QD5Q", family: "KNIGHT", generation: "2",
     modelName: "Echo Show 15", imageFilename: "echo_show_15", validated: true],

    [deviceType: "AZANTNEUTYY6L", family: "KNIGHT", generation: "Unspecified",
     modelName: "Echo Show 21", imageFilename: "echo_show_21", validated: true],

    [deviceType: "A2LLN0UXRW4N50", family: "KNIGHT", generation: "Unspecified",
     modelName: "Echo Show 11", imageFilename: "echo_show_11", validated: true],

    // ============================================================
    // OTHER ECHO HARDWARE
    // ============================================================

    [deviceType: "A3RBAYBE7VM004", family: "ECHO", generation: "Unspecified",
     modelName: "Echo Studio", imageFilename: "echo_studio", validated: true],

    [deviceType: "A3VRME03NAXFUB", family: "ECHO", generation: "Unspecified",
     modelName: "Echo Flex", imageFilename: "echo_flex", validated: true],

    [deviceType: "A3SSG6GR8UU7SN", family: "ECHO", generation: "1",
     modelName: "Echo Sub", imageFilename: "echo_sub_gen1", validated: true],

    [deviceType: "A27VEYGQBW3YR5", family: "ECHO", generation: "Unspecified",
     modelName: "Echo Link", imageFilename: "echo_link", validated: false],

    [deviceType: "A2RU4B77X9R9NZ", family: "ECHO", generation: "Unspecified",
     modelName: "Echo Link Amp", imageFilename: "echo_link_amp", validated: false],

    [deviceType: "A1JJ0KFC4ZPNJ3", family: "ECHO", generation: "Unspecified",
     modelName: "Echo Input", imageFilename: "echo_input", validated: true],

    [deviceType: "A38949IHXHRQ5P", family: "ECHO", generation: "Unspecified",
     modelName: "Echo Tap", imageFilename: "echo_tap", validated: true],

    [deviceType: "A3IYPH06PH1HRA", family: "ECHO", generation: "Unspecified",
     modelName: "Echo Frames", imageFilename: "echo_frames", validated: false],

    [deviceType: "A15QWUTQ6FSMYX", family: "ECHO", generation: "2",
     modelName: "Echo Buds", imageFilename: "echo_buds_gen2", validated: false],

    // ============================================================
    // ECHO AUTO
    // ============================================================

    [deviceType: "A303PJF6ISQ7IC", family: "ALEXA_AUTO", generation: "Unspecified",
     modelName: "Echo Auto", imageFilename: "echo_auto", validated: true],

    [deviceType: "A195TXHV1M5D4A", family: "ALEXA_AUTO", generation: "Unspecified",
     modelName: "Echo Auto", imageFilename: "echo_auto", validated: false],

    [deviceType: "ALT9P69K6LORD", family: "ALEXA_AUTO", generation: "Unspecified",
     modelName: "Echo Auto", imageFilename: "echo_auto", validated: false],

    [deviceType: "A13W6HQIHKEN3Z", family: "ALEXA_AUTO", generation: "Unspecified",
     modelName: "Echo Auto", imageFilename: "echo_auto", validated: false],

    // ============================================================
    // ECHO HUB
    // ============================================================

    [deviceType: "ADMKNMEVNL158", family: "KNIGHT", generation: "Unspecified",
     modelName: "Echo Hub", imageFilename: "echo_hub", validated: true],

    // ============================================================
    // FIRE TV
    // ============================================================

    [deviceType: "A12GXV8XMS007S", family: "FIRE_TV", generation: "1",
     modelName: "Fire TV", imageFilename: "firetv_gen1", validated: false],

    [deviceType: "A2E0SNTXJVT7WK", family: "FIRE_TV", generation: "2",
     modelName: "Fire TV", imageFilename: "firetv_gen2", validated: false],

    [deviceType: "A2GFL5ZMWNE0PX", family: "FIRE_TV", generation: "3",
     modelName: "Fire TV", imageFilename: "firetv_gen3", validated: false],

    [deviceType: "A1P7E7V3FCZKU6", family: "FIRE_TV", generation: "3",
     modelName: "Fire TV", imageFilename: "firetv_gen3", validated: false],

    [deviceType: "A3IKB0DFR7GKZW", family: "FIRE_TV", generation: "3",
     modelName: "Fire TV", imageFilename: "firetv_gen3", validated: false],

    [deviceType: "A265XOI9586NML", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV Stick", imageFilename: "firetv_stick_gen1", validated: false],

    [deviceType: "ADVBD696BHNV5", family: "FIRE_TV", generation: "1",
     modelName: "Fire TV Stick", imageFilename: "firetv_stick_gen1", validated: false],

    [deviceType: "A2LWARUGJLBYEW", family: "FIRE_TV", generation: "2",
     modelName: "Fire TV Stick", imageFilename: "firetv_stick_gen1", validated: false],

    [deviceType: "AKPGW064GI9HE", family: "FIRE_TV", generation: "3",
     modelName: "Fire TV Stick 4K", imageFilename: "firetv_stick_gen1", validated: false],

    [deviceType: "A31DTMEEVDDOIV", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV Stick Lite", imageFilename: "unknown", validated: false],

    [deviceType: "A1WZKXFLI43K86", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV Stick 4K Max", imageFilename: "unknown", validated: false],

    [deviceType: "A3EVMLQTU6WL1W", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV Stick 4K Max", imageFilename: "unknown", validated: false],

    [deviceType: "A2JKHJ0PX4J3L3", family: "FIRE_TV_CUBE", generation: "2",
     modelName: "Fire TV Cube", imageFilename: "firetv_cube", validated: false],

    [deviceType: "A3HF4YRA2L7XGC", family: "FIRE_TV_CUBE", generation: "Unspecified",
     modelName: "Fire TV Cube", imageFilename: "firetv_cube", validated: false],

    [deviceType: "A1VGB7MHSIEYFK", family: "FIRE_TV_CUBE", generation: "3",
     modelName: "Fire TV Cube", imageFilename: "firetv_cube", validated: false],

    [deviceType: "AN630UQPG2CA4", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV Toshiba", imageFilename: "firetv_toshiba", validated: false],

    [deviceType: "A1F8D55J0FWDTN", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV Toshiba", imageFilename: "toshiba_firetv", validated: false],

    [deviceType: "AP4RS91ZQ0OOI", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV Toshiba", imageFilename: "toshiba_firetv", validated: false],

    [deviceType: "AFF5OAL5E3DIU", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV", imageFilename: "firetv_toshiba", validated: false],

    [deviceType: "AFF50AL5E3DIU", family: "FIRE_TV", generation: "Unspecified",
     modelName: "Fire TV Insignia", imageFilename: "insignia_firetv", validated: false],

    // ============================================================
    // AMAZON TABLETS
    // ============================================================

    [deviceType: "A1Q7QCGNMXAKYW", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet", imageFilename: "amazon_tablet", validated: false],

    [deviceType: "ATNLRCEBX3W4P", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet", imageFilename: "amazon_tablet", validated: false],

    [deviceType: "A1J16TEDOYCZTN", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet", imageFilename: "amazon_tablet", validated: false],

    [deviceType: "A2M4YX06LWP8WI", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet", imageFilename: "amazon_tablet", validated: false],

    [deviceType: "A1C66CX2XD756O", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet HD", imageFilename: "amazon_tablet", validated: false],

    [deviceType: "A2N49KXGVA18AR", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet HD 10 Plus", imageFilename: "tablet_hd10", validated: false],

    [deviceType: "A3L0T0VL9A921N", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet HD 8", imageFilename: "tablet_hd10", validated: false],

    [deviceType: "AVU7CPPF2ZRAS", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet HD 8 Plus", imageFilename: "tablet_hd10", validated: false],

    [deviceType: "A38EHHIB10L47V", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet HD 8", imageFilename: "tablet_hd10", validated: false],

    [deviceType: "A3R9S4ZZECZ6YL", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet HD 10", imageFilename: "tablet_hd10", validated: false],

    [deviceType: "A2V9UEGZ82H4KZ", family: "TABLET", generation: "Unspecified",
     modelName: "Fire Tablet HD 10", imageFilename: "tablet_hd10", validated: false],

    // ============================================================
    // MULTIROOM / WHA
    // ============================================================

    [deviceType: "A3C9PE6TNYLTCH", family: "WHA", generation: "N/A",
     modelName: "Multiroom", imageFilename: "echo_wha", validated: true]
]

static String version() { return '1.6.14' }
def timeStamp() { return "2026/09/18 01:30 PM" }

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
        capability "TemperatureMeasurement"
        capability "MotionSensor"

        // BASE CUSTOM COMMANDS
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

        // CAPABILITY-AWARE DISPLAY & FEATURE COMMANDS
        command "setDisplayBrightness", [[name: "level*", type: "NUMBER", description: "Display brightness level (0-100)"]]
        command "setDisplayPowerOn"
        command "setDisplayPowerOff"
        command "setAdaptiveBrightnessOn"
        command "setAdaptiveBrightnessOff"

        // 1. Hardware & Topology
        attribute "serialNumber", "string"
        attribute "family", "string"
        attribute "model", "string"
        attribute "generation", "string"
        attribute "modelNumber", "string"
        attribute "softwareVer", "string"
        attribute "amazonDeviceName", "string"

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

        // 5. Visual Artwork & Dashboard Tiles
        attribute "icon", "string"
        attribute "trackImage", "string"
        attribute "trackImageHtml", "string"

        // 6. Capability-Aware Feature Attributes
        attribute "displayBrightness", "number"
        attribute "displayPower", "string"
        attribute "adaptiveBrightness", "string"
        attribute "motion", "string"
        attribute "temperature", "number"
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

    // Seed Defaults for standard attributes
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
    if (device.currentValue("transportStatus") == null) sendEvent(name: "transportStatus", value: "stopped")
    if (device.currentValue("trackDescription") == null) sendEvent(name: "trackDescription", value: "Stopped")
    if (device.currentValue("trackData") == null) sendEvent(name: "trackData", value: "{}")

    if (device.currentValue("lastSpokenText") == null) sendEvent(name: "lastSpokenText", value: "None")
    if (device.currentValue("lastAnnouncementText") == null) sendEvent(name: "lastAnnouncementText", value: "None")
    if (device.currentValue("lastOutputType") == null) sendEvent(name: "lastOutputType", value: "speak")
    if (device.currentValue("lastVoiceActivity") == null) sendEvent(name: "lastVoiceActivity", value: "None")

    if (device.currentValue("icon") == null) sendEvent(name: "icon", value: "None")
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

void replayText() {
    logInfo "GUI Action: [replayText] button clicked."
    logTrace "--> replayText() Initiated"
    String lastType = state.lastOutputType ?: device.currentValue("lastOutputType") ?: "speak"

    if (lastType in ["announce", "announceAll"]) {
        String lastAnn = state.lastAnnouncementText ?: device.currentValue("lastAnnouncementText")
        if (lastAnn && lastAnn != "None") {
            logInfo "${device.displayName} - Replaying last ${lastType}: \"${lastAnn}\""
            if (lastType == "announceAll") playAnnouncementAll(lastAnn) else playAnnouncement(lastAnn)
        } else {
            logWarn "${device.displayName} - Replay requested for ${lastType}, but no previous announcement text is stored."
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
   CAPABILITY-GUARDED DISPLAY & FEATURE COMMANDS
   ========================================================================================= */

private Boolean hasCapability(String capName) {
    Map caps = state.capabilitiesMap ?: [:]
    return (caps[capName] == true)
}

void setDisplayBrightness(level) {
    if (level == null) return
    if (!hasCapability("displayBrightness") && !hasCapability("display")) {
        logWarn "${device.displayName} - setDisplayBrightness() suppressed: Device does not advertise displayBrightness capability."
        return
    }
    int targetVal = Math.max(0, Math.min(100, level.toBigDecimal().intValue()))
    logInfo "GUI Action: [setDisplayBrightness] -> Target: ${targetVal}%"
    
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setDisplayBrightness", [level: targetVal])
    if (success != false) {
        sendEvent(name: "displayBrightness", value: targetVal, unit: "%")
    }
}

void setDisplayPowerOn() {
    if (!hasCapability("display")) {
        logWarn "${device.displayName} - setDisplayPowerOn() suppressed: Device does not advertise display capability."
        return
    }
    logInfo "GUI Action: [setDisplayPowerOn]"
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setDisplayPowerOn", [:])
    if (success != false) {
        sendEvent(name: "displayPower", value: "on")
    }
}

void setDisplayPowerOff() {
    if (!hasCapability("display")) {
        logWarn "${device.displayName} - setDisplayPowerOff() suppressed: Device does not advertise display capability."
        return
    }
    logInfo "GUI Action: [setDisplayPowerOff]"
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setDisplayPowerOff", [:])
    if (success != false) {
        sendEvent(name: "displayPower", value: "off")
    }
}

void setAdaptiveBrightnessOn() {
    if (!hasCapability("adaptiveBrightness")) {
        logWarn "${device.displayName} - setAdaptiveBrightnessOn() suppressed: Device does not advertise adaptiveBrightness capability."
        return
    }
    logInfo "GUI Action: [setAdaptiveBrightnessOn]"
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setAdaptiveBrightnessOn", [:])
    if (success != false) {
        sendEvent(name: "adaptiveBrightness", value: "on")
    }
}

void setAdaptiveBrightnessOff() {
    if (!hasCapability("adaptiveBrightness")) {
        logWarn "${device.displayName} - setAdaptiveBrightnessOff() suppressed: Device does not advertise adaptiveBrightness capability."
        return
    }
    logInfo "GUI Action: [setAdaptiveBrightnessOff]"
    Boolean success = parent?.sendBridgeCommand(device.deviceNetworkId, "setAdaptiveBrightnessOff", [:])
    if (success != false) {
        sendEvent(name: "adaptiveBrightness", value: "off")
    }
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
    int vol = Math.max(0, Math.min(100, volumeLevel.toBigDecimal().intValue()))

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
   CANONICAL DYNAMIC CAPABILITY & FEATURE PARSER (Server v1.2.8 / v1.3.0)
   ========================================================================================= */

void parseDeviceFeatures(Map resp) {
    if (!resp) return
    
    // Safety Unwrap if parent passes full response container
    Map payload = (resp.result instanceof Map) ? resp.result : resp
    logTrace "--> parseDeviceFeatures() Canonical payload received for ${device.displayName}: ${payload}"

    Map caps  = (payload.capabilities instanceof Map) ? payload.capabilities : [:]
    Map st    = (payload.state instanceof Map)        ? payload.state        : [:]
    Map errs  = (payload.errors instanceof Map)       ? payload.errors       : [:]

    state.capabilitiesMap = caps

    // 1. Temperature Sensor
    if (caps.temperature == true) {
        if (st.temperature != null) {
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
    } else {
        device.deleteCurrentState("temperature")
    }

    // 2. Motion Detection
    if (caps.motion == true) {
        if (st.motion != null) {
            String rawMot = st.motion.toString().trim().toUpperCase()
            String cleanMotion = (rawMot in ["DETECTED", "ACTIVE", "TRUE", "MOTION"]) ? "active" : "inactive"
            sendEvent(name: "motion", value: cleanMotion)
        }
    } else {
        device.deleteCurrentState("motion")
    }

    // 3. Display Brightness (0-100)
    if (caps.displayBrightness == true || caps.display == true) {
        if (st.displayBrightness != null) {
            int brightVal = Math.max(0, Math.min(100, st.displayBrightness.toInteger()))
            sendEvent(name: "displayBrightness", value: brightVal, unit: "%")
        }
    } else {
        device.deleteCurrentState("displayBrightness")
    }

    // 4. Display Power ("on" / "off")
    if (caps.display == true) {
        if (st.displayPower != null) {
            Boolean pwr = st.displayPower.toBoolean()
            sendEvent(name: "displayPower", value: pwr ? "on" : "off")
        }
    } else {
        device.deleteCurrentState("displayPower")
    }

    // 5. Adaptive Brightness ("on" / "off")
    if (caps.adaptiveBrightness == true) {
        if (st.adaptiveBrightness != null) {
            Boolean adapt = st.adaptiveBrightness.toBoolean()
            sendEvent(name: "adaptiveBrightness", value: adapt ? "on" : "off")
        }
    } else {
        device.deleteCurrentState("adaptiveBrightness")
    }

    if (errs && errs.size() > 0) {
        logWarn "${device.displayName} - Server reported feature query errors for supported capability: ${errs}"
    }
}

/* =========================================================================================
   BRIDGE DATA PARSING & STATE UPDATES
   ========================================================================================= */

void parseDeviceData(Map devData) {
    if (!devData) return
    logTrace "--> parseDeviceData() Processing payload for ${device.displayName}"

    // 1. Hardware & Topology Mapping
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

    // 2. Connectivity & Online State Mapping
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

    // 3. Volume & Audio Controls
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
    String iconVal = matchedTypeInfo?.imageFilename ?: devData.icon ?: devData.deviceIcon ?: "None"
    sendEvent(name: "icon", value: iconVal.toString())

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