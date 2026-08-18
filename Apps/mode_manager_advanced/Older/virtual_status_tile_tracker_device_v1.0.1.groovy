/*
 * Virtual Mode Status and State Tile Driver for Mode Manager Advanced
 * Custom Hubitat Driver for tracking mode state, evaluation reason, transition timestamp, and HTML tile layout.
 *
 * Licensed under the Apache License, Version 2.0
 *
 * Change History:
 * v1.0.1 (2026-08-16) - Auto-rebuild tile/html attributes when sendEvent updates individual attributes.
 * v1.0.0 (2026-08-16) - Initial release of Virtual Status Tile & State Tracker Driver.
 */

metadata {
    definition (
        name: "Virtual Mode Status & State Tracker Driver",
        namespace: "jshimota",
        author: "James Shimota",
        description: "Custom virtual status device for storing ground-truth Mode, Evaluation Reason, and HTML Dashboard Tile attributes."
    ) {
        capability "Actuator"
        capability "Sensor"

        // Custom Ground-Truth Attributes
        attribute "activeMode", "string"
        attribute "activeReason", "string"
        attribute "lastTransitionTime", "string"
        attribute "tile", "string"
        attribute "html", "string"

        // Custom Commands for Direct Control
        command "setActiveMode", ["string"]
        command "setActiveReason", ["string"]
        command "setLastTransitionTime", ["string"]
        command "setTile", ["string"]
        command "setStatus", ["string", "string", "string"]
        command "refreshTile"
    }

    preferences {
        input name: "enableHtmlFormatting", type: "bool", title: "Enable HTML Styling for Tile Attribute", defaultValue: true
        input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
    }
}

def installed() {
    logInfo "Virtual Mode Status & State Tracker Driver installed."
    initialize()
}

def updated() {
    logInfo "Virtual Mode Status & State Tracker Driver updated."
    updateTileDisplay()
}

def initialize() {
    if (device.currentValue("activeMode") == null) sendEvent(name: "activeMode", value: "Unknown")
    if (device.currentValue("activeReason") == null) sendEvent(name: "activeReason", value: "Unknown")
    if (device.currentValue("lastTransitionTime") == null) sendEvent(name: "lastTransitionTime", value: "None")
    updateTileDisplay()
}

/* =========================================================================================
   DRIVER COMMANDS & EVENT OVERRIDES
   ========================================================================================= */

def sendEvent(Map properties) {
    super.sendEvent(properties)
    // Auto-rebuild the HTML tile whenever activeMode, activeReason, or lastTransitionTime are updated via sendEvent
    if (["activeMode", "activeReason", "lastTransitionTime"].contains(properties?.name)) {
        updateTileDisplay()
    }
}

def refreshTile() {
    updateTileDisplay()
}

def setActiveMode(String modeVal) {
    if (modeVal != null) {
        logInfo "Setting activeMode -> '${modeVal}'"
        sendEvent(name: "activeMode", value: modeVal)
    }
}

def setActiveReason(String reasonVal) {
    if (reasonVal != null) {
        logInfo "Setting activeReason -> '${reasonVal}'"
        sendEvent(name: "activeReason", value: reasonVal)
    }
}

def setLastTransitionTime(String timeVal) {
    if (timeVal != null) {
        logInfo "Setting lastTransitionTime -> '${timeVal}'"
        sendEvent(name: "lastTransitionTime", value: timeVal)
    }
}

def setTile(String tileContent) {
    if (tileContent != null) {
        sendEvent(name: "tile", value: tileContent)
        sendEvent(name: "html", value: tileContent)
    }
}

def setStatus(String modeVal, String reasonVal, String timeVal = null) {
    String currentMode = modeVal ?: device.currentValue("activeMode") ?: "Unknown"
    String currentReason = reasonVal ?: device.currentValue("activeReason") ?: "Unknown"
    String currentTime = timeVal ?: new Date().format("hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())

    sendEvent(name: "activeMode", value: currentMode)
    sendEvent(name: "activeReason", value: currentReason)
    sendEvent(name: "lastTransitionTime", value: currentTime)

    logInfo "Status Batch Update -> Mode: '${currentMode}' | Reason: '${currentReason}' | Time: '${currentTime}'"
    updateTileDisplay()
}

/* =========================================================================================
   INTERNAL TILE FORMATTING ROUTINES
   ========================================================================================= */

private void updateTileDisplay() {
    String currentMode = device.currentValue("activeMode") ?: "Unknown"
    String currentReason = device.currentValue("activeReason") ?: "Unknown"
    String currentTime = device.currentValue("lastTransitionTime") ?: "None"

    Boolean useHtml = (settings.enableHtmlFormatting != null) ? settings.enableHtmlFormatting : true

    String formattedTile = ""
    if (useHtml) {
        formattedTile = "<div style='text-align:center; padding:6px; font-family:sans-serif;'>" +
                        "<div style='font-size:16px; font-weight:bold; color:#27AE60; margin-bottom:4px;'>${currentMode}</div>" +
                        "<div style='font-size:12px; color:#34495E;'>Reason: <b>${currentReason}</b></div>" +
                        "<div style='font-size:10px; color:#7F8C8D; margin-top:4px;'>Updated: ${currentTime}</div>" +
                        "</div>"
    } else {
        formattedTile = "Mode: ${currentMode} | Reason: ${currentReason} | Time: ${currentTime}"
    }

    super.sendEvent(name: "tile", value: formattedTile)
    super.sendEvent(name: "html", value: formattedTile)
}

private void logInfo(String msg) {
    if (settings.logInfoEnable != false) {
        log.info "${device.displayName}: ${msg}"
    }
}