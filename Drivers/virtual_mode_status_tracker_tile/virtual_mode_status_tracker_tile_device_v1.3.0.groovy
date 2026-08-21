/*
 * Virtual Mode Status and State Tracker Tile Driver for Mode Manager Advanced
 * Custom Hubitat Driver for tracking mode state, evaluation reason, HSM status, transition timestamp, and HTML tile layout.
 *
 * Licensed under the Apache License, Version 2.0
 *
 * Change History:
 * v1.3.0 (2026-08-21) - Native HSM Status Integration:
 *                        - Added 'hsmStatus' custom attribute definition and 'setHsmStatus' command.
 *                        - Expanded 'setStatus' command signature to natively accept HSM state: setStatus(Mode, Reason, Time, HSM).
 *                        - Added formatHsmDisplay() helper with high-contrast color mapping for HSM states.
 *                        - Updated HTML tile generator to render a dedicated HSM status line with configurable styling preferences.
 * v1.2.3 (2026-08-18) - Code Integrity & Preference Rendering Optimization:
 *                        - Safe-guarded input parameter reads against null settings during initial lifecycle installation.
 *                        - Validated font size inputs in updateTileDisplay() to ensure robust relative 'em' fallback values.
 * v1.2.2 (2026-08-18) - Change History Log Restoration.
 * v1.2.1 (2026-08-18) - Preferences Null Object Protection Fix.
 * v1.2.0 (2026-08-18) - Responsive Font Sizing, Bold Styling Preferences & UI Help.
 * v1.1.0 (2026-08-18) - Logical Color Mapping Integration.
 * v1.0.0 (2026-08-16) - Initial release of Virtual Status Tile & State Tracker Driver.
 */

static String version() { return "1.3.0" }

metadata {
    definition (
        name: "Virtual Mode Status & State Tracker Tile Driver",
        namespace: "jshimota",
        author: "James Shimota",
        description: "Custom virtual status device for storing ground-truth Mode, Evaluation Reason, HSM Status, and HTML Dashboard Tile attributes."
    ) {
        capability "Actuator"
        capability "Sensor"
        capability "Initialize"

        // Custom Ground-Truth Attributes
        attribute "activeMode", "string"
        attribute "activeReason", "string"
        attribute "hsmStatus", "string"
        attribute "lastTransitionTime", "string"
        attribute "tile", "string"
        attribute "html", "string"

        // Custom Commands for Direct Control
        command "setActiveMode", ["string"]
        command "setActiveReason", ["string"]
        command "setHsmStatus", ["string"]
        command "setLastTransitionTime", ["string"]
        command "setTile", ["string"]
        command "setStatus", [
            [name: "Mode", type: "STRING"],
            [name: "Reason", type: "STRING"],
            [name: "Time", type: "STRING"],
            [name: "HSM Status", type: "STRING"]
        ]
        command "refreshTile"
    }

    preferences {
        input name: "enableHtmlFormatting", type: "bool", title: "<b>Enable HTML Styling for Tile Attribute</b>", defaultValue: true, submitOnChange: true

        if (settings?.enableHtmlFormatting != false) {
            input name: "modeFontSize", type: "text", title: "<b>Mode Font Size (em)</b> <i>(Default: 1.2em)</i>", defaultValue: "1.2em", required: true
            input name: "boldMode", type: "bool", title: "<b>Bold Mode Text?</b>", defaultValue: true

            input name: "reasonFontSize", type: "text", title: "<b>Reason Font Size (em)</b> <i>(Default: 0.95em)</i>", defaultValue: "0.95em", required: true
            input name: "boldReason", type: "bool", title: "<b>Bold Reason Text?</b>", defaultValue: true

            input name: "hsmFontSize", type: "text", title: "<b>HSM Status Font Size (em)</b> <i>(Default: 0.95em)</i>", defaultValue: "0.95em", required: true
            input name: "boldHsm", type: "bool", title: "<b>Bold HSM Text?</b>", defaultValue: true

            input name: "updatedFontSize", type: "text", title: "<b>Updated Timestamp Font Size (em)</b> <i>(Default: 0.8em)</i>", defaultValue: "0.8em", required: true
            input name: "boldUpdated", type: "bool", title: "<b>Bold Updated Timestamp Text?</b>", defaultValue: false
        }

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
    state.driverVersion = version()
    
    // Seed initial attribute states if empty
    if (device.currentValue("activeMode") == null) sendEvent(name: "activeMode", value: "Unknown")
    if (device.currentValue("activeReason") == null) sendEvent(name: "activeReason", value: "Unknown")
    if (device.currentValue("hsmStatus") == null) sendEvent(name: "hsmStatus", value: "disarmed")
    if (device.currentValue("lastTransitionTime") == null) sendEvent(name: "lastTransitionTime", value: "None")

    updateTileDisplay()
}

def parse(String description) {
    logInfo "parse() received description: ${description}"
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
        updateTileDisplay(modeVal, null, null, null)
    }
}

def setActiveReason(String reasonVal) {
    if (reasonVal != null) {
        logInfo "Setting activeReason -> '${reasonVal}'"
        sendEvent(name: "activeReason", value: reasonVal)
        updateTileDisplay(null, reasonVal, null, null)
    }
}

def setHsmStatus(String hsmVal) {
    if (hsmVal != null) {
        logInfo "Setting hsmStatus -> '${hsmVal}'"
        sendEvent(name: "hsmStatus", value: hsmVal)
        updateTileDisplay(null, null, null, hsmVal)
    }
}

def setLastTransitionTime(String timeVal) {
    if (timeVal != null) {
        logInfo "Setting lastTransitionTime -> '${timeVal}'"
        sendEvent(name: "lastTransitionTime", value: timeVal)
        updateTileDisplay(null, null, timeVal, null)
    }
}

def setTile(String tileContent) {
    if (tileContent != null) {
        sendEvent(name: "tile", value: tileContent)
        sendEvent(name: "html", value: tileContent)
    }
}

def setStatus(String modeVal, String reasonVal, String timeVal = null, String hsmVal = null) {
    String currentMode = modeVal ?: device.currentValue("activeMode") ?: "Unknown"
    String currentReason = reasonVal ?: device.currentValue("activeReason") ?: "Unknown"
    String currentTime = timeVal ?: new Date().format("hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())
    String currentHsm = hsmVal ?: device.currentValue("hsmStatus") ?: "disarmed"

    sendEvent(name: "activeMode", value: currentMode)
    sendEvent(name: "activeReason", value: currentReason)
    sendEvent(name: "hsmStatus", value: currentHsm)
    sendEvent(name: "lastTransitionTime", value: currentTime)

    logInfo "Status Batch Update -> Mode: '${currentMode}' | Reason: '${currentReason}' | HSM: '${currentHsm}' | Time: '${currentTime}'"
    
    updateTileDisplay(currentMode, currentReason, currentTime, currentHsm)
}

/* =========================================================================================
   INTERNAL TILE FORMATTING ROUTINES
   ========================================================================================= */

private String getModeColor(String modeVal) {
    switch (modeVal) {
        case "Sleeping": return "#2980B9" // Blue for Sleeping mode
        case "Away":     return "#C0392B" // Red for Away mode
        default:         return "#27AE60" // Green for Period modes
    }
}

private String getReasonColor(String reasonVal) {
    switch (reasonVal) {
        case "Scheduled": return "#2C3E50" // Slate
        case "Voice":     return "#D35400" // Orange
        case "Presence":  return "#16A085" // Teal
        case "Override":  return "#C0392B" // Red
        case "Reboot":    return "#8E44AD" // Purple
        default:          return "#2C3E50"
    }
}

private String formatHsmDisplay(String rawHsm) {
    switch (rawHsm) {
        case "armedAway":   return "<span style='color:#C0392B; font-weight:bold;'>Armed Away</span>"
        case "armedNight":  return "<span style='color:#2980B9; font-weight:bold;'>Armed Night</span>"
        case "armedHome":   return "<span style='color:#D35400; font-weight:bold;'>Armed Home</span>"
        case "disarmed":    return "<span style='color:#27AE60; font-weight:bold;'>Disarmed</span>"
        case "allDisarmed": return "<span style='color:#27AE60; font-weight:bold;'>All Disarmed</span>"
        default:            return "<span style='color:#7F8C8D; font-weight:bold;'>${rawHsm}</span>"
    }
}

private String sanitizeFontSize(String inputVal, String defaultVal) {
    if (!inputVal) return defaultVal
    String trimmed = inputVal.trim()
    return (trimmed.endsWith("em") || trimmed.endsWith("px") || trimmed.endsWith("%")) ? trimmed : "${trimmed}em"
}

private void updateTileDisplay(String overrideMode = null, String overrideReason = null, String overrideTime = null, String overrideHsm = null) {
    String currentMode = overrideMode ?: device.currentValue("activeMode") ?: "Unknown"
    String currentReason = overrideReason ?: device.currentValue("activeReason") ?: "Unknown"
    String currentTime = overrideTime ?: device.currentValue("lastTransitionTime") ?: "None"
    String currentHsm = overrideHsm ?: device.currentValue("hsmStatus") ?: "disarmed"

    Boolean useHtml = (settings?.enableHtmlFormatting != false)

    String formattedTile = ""
    if (useHtml) {
        String modeColor = getModeColor(currentMode)
        String reasonColor = getReasonColor(currentReason)
        String hsmHtml = formatHsmDisplay(currentHsm)

        String mSize = sanitizeFontSize(settings?.modeFontSize, "1.2em")
        String rSize = sanitizeFontSize(settings?.reasonFontSize, "0.95em")
        String hSize = sanitizeFontSize(settings?.hsmFontSize, "0.95em")
        String uSize = sanitizeFontSize(settings?.updatedFontSize, "0.8em")

        String mWeight = (settings?.boldMode != false) ? "font-weight:bold;" : "font-weight:normal;"
        String rWeight = (settings?.boldReason != false) ? "font-weight:bold;" : "font-weight:normal;"
        String hWeight = (settings?.boldHsm != false) ? "font-weight:bold;" : "font-weight:normal;"
        String uWeight = (settings?.boldUpdated == true) ? "font-weight:bold;" : "font-weight:normal;"

        formattedTile = "<div style='text-align:center; padding:0.3em; font-family:sans-serif; color:#000000;'>" +
                        "<div style='font-size:${mSize}; ${mWeight} color:#000000; margin-bottom:0.15em;'>Mode: <span style='color:${modeColor};'>${currentMode}</span></div>" +
                        "<div style='font-size:${rSize}; color:#000000; margin-bottom:0.15em;'>Reason: <span style='color:${reasonColor}; ${rWeight}'>${currentReason}</span></div>" +
                        "<div style='font-size:${hSize}; color:#000000; ${hWeight} margin-bottom:0.15em;'>HSM: ${hsmHtml}</div>" +
                        "<div style='font-size:${uSize}; ${uWeight} color:#000000; opacity:0.8;'>Updated: ${currentTime}</div>" +
                        "</div>"
    } else {
        formattedTile = "Mode: ${currentMode} | Reason: ${currentReason} | HSM: ${currentHsm} | Time: ${currentTime}"
    }

    sendEvent(name: "tile", value: formattedTile)
    sendEvent(name: "html", value: formattedTile)
}

private void logInfo(String msg) {
    if (settings?.logInfoEnable != false) {
        log.info "${device.displayName}: ${msg}"
    }
}