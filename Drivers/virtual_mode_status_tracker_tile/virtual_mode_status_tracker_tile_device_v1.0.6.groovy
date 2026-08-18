/*
 * Virtual Mode Status and State Tile Driver for Mode Manager Advanced
 * Custom Hubitat Driver for tracking mode state, evaluation reason, transition timestamp, and HTML tile layout.
 *
 * Licensed under the Apache License, Version 2.0
 *
 * Change History:
 * v1.0.6 (2026-08-18) - Initialize Capability & UI State Refresh Fix:
 *                        - Added 'capability "Initialize"' to metadata definition.
 *                        - Exposed explicit initialize() command button in Hubitat Device Details UI.
 * v1.0.5 (2026-08-18) - State Attribute Driver Version Addition:
 *                        - Added 'driverVersion' custom attribute definition.
 *                        - Updated initialize() to persist driverVersion to state.driverVersion and dispatch a state event on driver load.
 * v1.0.4 (2026-08-16) - Synchronous State Batching & Execution Race Condition Fix:
 *                        - Refactored updateTileDisplay() to accept direct state overrides from setStatus().
 *                        - Bypasses device.currentValue() database latency during atomic multi-attribute batch updates.
 * v1.0.3 (2026-08-16) - Tile Formatting & High-Contrast Visibility Update:
 *                        - Added explicit "Mode: " prefix before colorized mode string.
 *                        - Fixed dark theme text clipping by converting Reason and Updated strings to solid black (#000000).
 * v1.0.2 (2026-08-16) - Self-Event Monitoring & HTML Tile Refresh Guarantee.
 * v1.0.1 (2026-08-16) - Auto-rebuild tile/html attributes when sendEvent updates individual attributes.
 * v1.0.0 (2026-08-16) - Initial release of Virtual Status Tile & State Tracker Driver.
 */

static String version() { return "1.0.6" }

metadata {
    definition (
        name: "Virtual Mode Status & State Tracker Tile Driver",
        namespace: "jshimota",
        author: "James Shimota",
        description: "Custom virtual status device for storing ground-truth Mode, Evaluation Reason, and HTML Dashboard Tile attributes."
    ) {
        capability "Actuator"
        capability "Sensor"
        capability "Initialize"

        // Custom Ground-Truth Attributes
        attribute "activeMode", "string"
        attribute "activeReason", "string"
        attribute "lastTransitionTime", "string"
        attribute "driverVersion", "string"
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
    initialize()
}

def initialize() {
    unsubscribe()
    
    // Store driver version in state object and dispatch attribute event
    String currentVer = version()
    state.driverVersion = currentVer
    sendEvent(name: "driverVersion", value: currentVer, isStateChange: true, descriptionText: "Driver version set to ${currentVer}")
    
    // Seed initial attribute states if empty
    if (device.currentValue("activeMode") == null) sendEvent(name: "activeMode", value: "Unknown")
    if (device.currentValue("activeReason") == null) sendEvent(name: "activeReason", value: "Unknown")
    if (device.currentValue("lastTransitionTime") == null) sendEvent(name: "lastTransitionTime", value: "None")
    
    // Self-subscribe to internal attribute changes as a fail-safe
    subscribe(device, "activeMode", attributeChangeHandler)
    subscribe(device, "activeReason", attributeChangeHandler)
    subscribe(device, "lastTransitionTime", attributeChangeHandler)

    updateTileDisplay()
}

def parse(String description) {
    logInfo "parse() received description: ${description}"
    updateTileDisplay()
}

def attributeChangeHandler(evt) {
    logInfo "Attribute '${evt.name}' updated to '${evt.value}'. Refreshing tile layout..."
    updateTileDisplay()
}

/* =========================================================================================
   DRIVER COMMANDS
   ========================================================================================= */

def refreshTile() {
    updateTileDisplay()
}

def setActiveMode(String modeVal) {
    if (modeVal != null) {
        logInfo "Setting activeMode -> '${modeVal}'"
        sendEvent(name: "activeMode", value: modeVal)
        updateTileDisplay(modeVal, null, null)
    }
}

def setActiveReason(String reasonVal) {
    if (reasonVal != null) {
        logInfo "Setting activeReason -> '${reasonVal}'"
        sendEvent(name: "activeReason", value: reasonVal)
        updateTileDisplay(null, reasonVal, null)
    }
}

def setLastTransitionTime(String timeVal) {
    if (timeVal != null) {
        logInfo "Setting lastTransitionTime -> '${timeVal}'"
        sendEvent(name: "lastTransitionTime", value: timeVal)
        updateTileDisplay(null, null, timeVal)
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
    
    updateTileDisplay(currentMode, currentReason, currentTime)
}

/* =========================================================================================
   INTERNAL TILE FORMATTING ROUTINES
   ========================================================================================= */

private void updateTileDisplay(String overrideMode = null, String overrideReason = null, String overrideTime = null) {
    String currentMode = overrideMode ?: device.currentValue("activeMode") ?: "Unknown"
    String currentReason = overrideReason ?: device.currentValue("activeReason") ?: "Unknown"
    String currentTime = overrideTime ?: device.currentValue("lastTransitionTime") ?: "None"

    Boolean useHtml = (settings.enableHtmlFormatting != null) ? settings.enableHtmlFormatting : true

    String formattedTile = ""
    if (useHtml) {
        formattedTile = "<div style='text-align:center; padding:4px; font-family:sans-serif; color:#000000;'>" +
                        "<div style='font-size:15px; font-weight:bold; color:#000000; margin-bottom:4px;'>Mode: <span style='color:#27AE60;'>${currentMode}</span></div>" +
                        "<div style='font-size:12px; color:#000000;'>Reason: <b>${currentReason}</b></div>" +
                        "<div style='font-size:11px; color:#000000; margin-top:4px;'>Updated: ${currentTime}</div>" +
                        "</div>"
    } else {
        formattedTile = "Mode: ${currentMode} | Reason: ${currentReason} | Time: ${currentTime}"
    }

    sendEvent(name: "tile", value: formattedTile)
    sendEvent(name: "html", value: formattedTile)
}

private void logInfo(String msg) {
    if (settings.logInfoEnable != false) {
        log.info "${device.displayName}: ${msg}"
    }
}