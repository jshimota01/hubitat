/**
 * Echos Speak Advanced Device (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Lightweight virtual child device driver for Echos Speak Advanced.
 * Exposes core audio controls, custom text-to-speech commands, native announcements,
 * media state tracking, album artwork tiles, and IP network identification.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability.
 * - Hubitat's native capability exposes an unwanted "Ping" UI control button
 *   and does not provide the phase-anchored scheduling, timeout guards, or trace 
 *   logging behavior required by this driver architecture.
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
 * v1.0.0    09/08/26    jshimota    Initial release of Echos Speak Advanced Device (Custom) based on standardized driver template.
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.0.0' }
def timeStamp() { return "2026/09/08 09:30 AM" }

import groovy.transform.Field

metadata {
    definition (
        name: "Echos Speak Advanced Device (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
		importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/echos-speak-advanced/echos-speak-advanced_custom.groovy"
    ) {
        capability "Actuator"
        capability "AudioNotification"
        capability "SpeechSynthesis"
        capability "MusicPlayer"
        capability "Sensor"
        capability "Configuration"
        capability "Refresh"

        // Attributes - Health & Network
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "ipAddress", "string"
        attribute "macAddress", "string"

        // Attributes - Audio & Device Controls
        attribute "volume", "number"
        attribute "mute", "string"
        attribute "dnd", "string"
        attribute "doNotDisturb", "string"
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
        attribute "deviceFamily", "string"
        attribute "deviceStyle", "string"
        attribute "onlineStatus", "string"

        // Custom Commands
        command "voiceCmdAsText", [[name: "Command Text*", type: "STRING", description: "Send text string to execute as native Alexa voice command"]]
        command "setDoNotDisturb", [[name: "State*", type: "ENUM", constraints: ["true", "false"]]]
        command "playAnnouncement", [[name: "Text*", type: "STRING", description: "Broadcast native Alexa announcement"]]
        command "Health Check"
        command "resetDriver"
    }

    preferences {
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver executes a Health Check to verify device online status.<br><b>Note:</b> This is a custom driver routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}