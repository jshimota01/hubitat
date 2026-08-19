/*
 * Virtual Mode Status and State Tracker Tile Driver for Mode Manager Advanced
 * Custom Hubitat Driver for tracking mode state, evaluation reason, transition timestamp, and HTML tile layout.
 *
 * Licensed under the Apache License, Version 2.0
 *
 * Change History:
 * v1.2.3 (2026-08-18) - Code Integrity & Preference Rendering Optimization:
 *                        - Safe-guarded input parameter reads against null settings during initial lifecycle installation.
 *                        - Validated font size inputs in updateTileDisplay() to ensure robust relative 'em' fallback values.
 * v1.2.2 (2026-08-18) - Change History Log Restoration:
 *                        - Restored complete historical change log entries from v1.0.0 through v1.2.1 in driver header block.
 * v1.2.1 (2026-08-18) - Preferences Null Object Protection Fix:
 *                        - Fixed 'Cannot get property enableHtmlFormatting on null object' exception in preferences block.
 *                        - Safe-guarded settings reference using safe navigation (settings?.enableHtmlFormatting).
 * v1.2.0 (2026-08-18) - Responsive Font Sizing, Bold Styling Preferences & UI Help:
 *                        - Replaced fixed 'px' font sizes with configurable relative 'em' unit inputs (modeFontSize, reasonFontSize, updatedFontSize).
 *                        - Added toggle preferences for bold styling (boldMode, boldReason, boldUpdated).
 *                        - Added preference help text detailing all options, format behaviors, and dynamic color mappings.
 * v1.1.0 (2026-08-18) - Logical Color Mapping Integration:
 *                        - Implemented getModeColor() helper: Blue (#2980B9) for Sleeping, Red (#C0392B) for Away, Green (#27AE60) for Period Modes.
 *                        - Implemented getReasonColor() helper: Slate (#2C3E50) for Scheduled, Orange (#D35400) for Voice, Teal (#16A085) for Presence, Red (#C0392B) for Override, Purple (#8E44AD) for Reboot.
 *                        - Updated updateTileDisplay() HTML tile generation to apply dynamic Mode and Reason color styling.
 * v1.0.9 (2026-08-18) - Device Unsubscribe & Driver Subscription Cleanup Fix:
 *                        - Removed invalid 'device.unsubscribe()' call throwing IllegalArgumentException on line 86.
 *                        - Removed unnecessary device event subscriptions in driver initialize().
 * v1.0.8 (2026-08-18) - Converted Driver Version to State Variable:
 *                        - Removed 'driverVersion' custom attribute from metadata definition.
 *                        - Refactored initialize() to persist version exclusively in state.driverVersion without issuing attribute events.
 * v1.0.7 (2026-08-18) - Driver Unsubscribe Exception Fix:
 *                        - Removed invalid App-scoped 'unsubscribe()' call from initialize() in driver code.
 *                        - Fixed groovy.lang.MissingMethodException causing initialize() and updated() execution failures.
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

static String version() { return "1.2.3" }

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
        input name: "enableHtmlFormatting", type: "bool", title: "<b>Enable HTML Styling for Tile Attribute</b>", defaultValue: true, submitOnChange: true

        if (settings?.enableHtmlFormatting != false) {
            input name: "modeFontSize", type: "text", title: "<b>Mode Font Size (em)</b> <i>(Default: 1.2em)</i>", defaultValue: "1.2em", required: true
            input name: "boldMode", type: "bool", title: "<b>Bold Mode Text?</b>", defaultValue: true

            input name: "reasonFontSize", type: "text", title: "<b>Reason Font Size (em)</b> <i>(Default: 0.95em)</i>", defaultValue: "0.95em", required: true
            input name: "boldReason", type: "bool", title: "<b>Bold Reason Text?</b>", defaultValue: true

            input name: "updatedFontSize", type: "text", title: "<b>Updated Timestamp Font Size (em)</b> <i>(Default: 0.85em)</i>", defaultValue: "0.85em", required: true
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
    // Store driver version as internal state variable
    state.driverVersion = version()
    
    // Seed initial attribute states if empty
    if (device.currentValue("activeMode") == null) sendEvent(name: "activeMode", value: "Unknown")
    if (device.currentValue("activeReason") == null) sendEvent(name: "activeReason", value: "Unknown")
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

private String sanitizeFontSize(String inputVal, String defaultVal) {
    if (!inputVal) return defaultVal
    String trimmed = inputVal.trim()
    return (trimmed.endsWith("em") || trimmed.endsWith("px") || trimmed.endsWith("%")) ? trimmed : "${trimmed}em"
}

private void updateTileDisplay(String overrideMode = null, String overrideReason = null, String overrideTime = null) {
    String currentMode = overrideMode ?: device.currentValue("activeMode") ?: "Unknown"
    String currentReason = overrideReason ?: device.currentValue("activeReason") ?: "Unknown"
    String currentTime = overrideTime ?: device.currentValue("lastTransitionTime") ?: "None"

    Boolean useHtml = (settings?.enableHtmlFormatting != false)

    String formattedTile = ""
    if (useHtml) {
        String modeColor = getModeColor(currentMode)
        String reasonColor = getReasonColor(currentReason)

        String mSize = sanitizeFontSize(settings?.modeFontSize, "1.2em")
        String rSize = sanitizeFontSize(settings?.reasonFontSize, "0.95em")
        String uSize = sanitizeFontSize(settings?.updatedFontSize, "0.85em")

        String mWeight = (settings?.boldMode != false) ? "font-weight:bold;" : "font-weight:normal;"
        String rWeight = (settings?.boldReason != false) ? "font-weight:bold;" : "font-weight:normal;"
        String uWeight = (settings?.boldUpdated == true) ? "font-weight:bold;" : "font-weight:normal;"

        formattedTile = "<div style='text-align:center; padding:0.3em; font-family:sans-serif; color:#000000;'>" +
                        "<div style='font-size:${mSize}; ${mWeight} color:#000000; margin-bottom:0.25em;'>Mode: <span style='color:${modeColor};'>${currentMode}</span></div>" +
                        "<div style='font-size:${rSize}; color:#000000;'>Reason: <span style='color:${reasonColor}; ${rWeight}'>${currentReason}</span></div>" +
                        "<div style='font-size:${uSize}; ${uWeight} color:#000000; margin-top:0.25em;'>Updated: ${currentTime}</div>" +
                        "</div>"
    } else {
        formattedTile = "Mode: ${currentMode} | Reason: ${currentReason} | Time: ${currentTime}"
    }

    sendEvent(name: "tile", value: formattedTile)
    sendEvent(name: "html", value: formattedTile)
}

private void logInfo(String msg) {
    if (settings?.logInfoEnable != false) {
        log.info "${device.displayName}: ${msg}"
    }
}