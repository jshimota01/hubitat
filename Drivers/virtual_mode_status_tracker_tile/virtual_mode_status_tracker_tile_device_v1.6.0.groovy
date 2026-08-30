/*
 * Virtual Mode Status and State Tracker Tile Driver for Mode Manager Advanced
 * Custom Hubitat Driver for tracking mode state, evaluation control source, HSM status, transition timestamp, and HTML tile layout.
 *
 * Licensed under the Apache License, Version 2.0
 *
 * Change History:
 * v1.6.0 (2026-08-27) - Terminology Refactor (Reason -> Control Source):
 *                        - Renamed activeReason -> activeControlSource and previousReason -> previousControlSource.
 *                        - Updated setStatus() payload parser to accept controlSource keys and map legacy 'reason' arguments cleanly.
 *                        - Refactored preferences, HTML tile labels, and logging from 'Reason' to 'Control Source'.
 * v1.5.0 (2026-08-23) - Downstream Diagnostic Attributes & Flexible Payload Overload:
 *                        - Added structured non-HTML attributes: previousMode, previousReason, activePeriodKey, isSleeping, lastTransactionId, 
 * 						  - lastTriggerSource, lastTransitionEpoch, mode.
 *                        - Expanded setStatus() command to accept optional Map/JSON metadata payloads without altering HTML tile visual layout.
 * v1.4.3 (2026-08-22) - Updated Sleeping Mode Color:
 *                        - Lightened Sleeping color from #3498DB to vivid cyan (#00FFFF) for enhanced contrast/readability.
 * v1.4.2 (2026-08-21) - Minor Typography Tweaks
 * v1.4.1 (2026-08-21) - Fine-Tuned Typography Sizing & Disarmed Contrast Fix
 * v1.4.0 (2026-08-21) - Scaled Font Sizes & High-Contrast Dark Tile Color Palette
 * v1.3.0 (2026-08-21) - Native HSM Status Integration
 * v1.2.3 (2026-08-18) - Code Integrity & Preference Rendering Optimization
 * v1.2.2 (2026-08-18) - Change History Log Restoration
 * v1.2.1 (2026-08-18) - Preferences Null Object Protection Fix
 * v1.2.0 (2026-08-18) - Responsive Font Sizing, Bold Styling Preferences & UI Help
 * v1.1.0 (2026-08-18) - Logical Color Mapping Integration
 * v1.0.0 (2026-08-16) - Initial release of Virtual Status Tile & State Tracker Driver.
 */
 
static String version() { return "1.6.0" }

metadata {
    definition (
        name: "Virtual Mode Status & State Tracker Tile Driver",
        namespace: "jshimota",
        author: "James Shimota",
        description: "Custom virtual status device for storing ground-truth Mode, Control Source, HSM Status, and HTML Dashboard Tile attributes."
    ) {
        capability "Actuator"
        capability "Sensor"
        capability "Initialize"

        // Primary Display Attributes
        attribute "activeMode", "string"
        attribute "activeControlSource", "string"
        attribute "hsmStatus", "string"
        attribute "lastTransitionTime", "string"
        attribute "tile", "string"
        attribute "html", "string"

        // Extended Diagnostic Telemetry Attributes
        attribute "mode", "string"
        attribute "previousMode", "string"
        attribute "previousControlSource", "string"
        attribute "activePeriodKey", "string"
        attribute "isSleeping", "string"
        attribute "lastTransactionId", "string"
        attribute "lastTriggerSource", "string"
        attribute "lastTransitionEpoch", "number"

        // Direct Control Commands
        command "setActiveMode", ["string"]
        command "setControlSource", ["string"]
        command "setHsmStatus", ["string"]
        command "setLastTransitionTime", ["string"]
        command "setTile", ["string"]
        command "setStatus", [
            [name: "Mode", type: "STRING"],
            [name: "Control Source", type: "STRING"],
            [name: "Time", type: "STRING"],
            [name: "HSM Status", type: "STRING"],
            [name: "Extended Metadata (Map/JSON)", type: "STRING"]
        ]
        command "refreshTile"
        
        // Backward Compatibility Alias
        command "setActiveReason", ["string"]
    }

    preferences {
        input name: "enableHtmlFormatting", type: "bool", title: "<b>Enable HTML Styling for Tile Attribute</b>", defaultValue: true, submitOnChange: true

        if (settings?.enableHtmlFormatting != false) {
            input name: "modeFontSize", type: "text", title: "<b>Mode Font Size (em)</b> <i>(Default: 0.9em)</i>", defaultValue: "0.9em", required: true
            input name: "boldMode", type: "bool", title: "<b>Bold Mode Text?</b>", defaultValue: true

            input name: "controlSourceFontSize", type: "text", title: "<b>Control Source Font Size (em)</b> <i>(Default: 0.8em)</i>", defaultValue: "0.8em", required: true
            input name: "boldControlSource", type: "bool", title: "<b>Bold Control Source Text?</b>", defaultValue: true

            input name: "hsmFontSize", type: "text", title: "<b>HSM Status Font Size (em)</b> <i>(Default: 0.8em)</i>", defaultValue: "0.8em", required: true
            input name: "boldHsm", type: "bool", title: "<b>Bold HSM Text?</b>", defaultValue: true

            input name: "updatedFontSize", type: "text", title: "<b>Updated Timestamp Font Size (em)</b> <i>(Default: 0.28em)</i>", defaultValue: "0.28em", required: true
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
    
    if (device.currentValue("activeMode") == null) sendEvent(name: "activeMode", value: "Unknown")
    if (device.currentValue("mode") == null) sendEvent(name: "mode", value: "Unknown")
    if (device.currentValue("activeControlSource") == null) sendEvent(name: "activeControlSource", value: "Unknown")
    if (device.currentValue("hsmStatus") == null) sendEvent(name: "hsmStatus", value: "disarmed")
    if (device.currentValue("lastTransitionTime") == null) sendEvent(name: "lastTransitionTime", value: "None")
    
    if (device.currentValue("previousMode") == null) sendEvent(name: "previousMode", value: "Unknown")
    if (device.currentValue("previousControlSource") == null) sendEvent(name: "previousControlSource", value: "Unknown")
    if (device.currentValue("activePeriodKey") == null) sendEvent(name: "activePeriodKey", value: "none")
    if (device.currentValue("isSleeping") == null) sendEvent(name: "isSleeping", value: "false")
    if (device.currentValue("lastTransactionId") == null) sendEvent(name: "lastTransactionId", value: "none")
    if (device.currentValue("lastTriggerSource") == null) sendEvent(name: "lastTriggerSource", value: "Initialization")
    if (device.currentValue("lastTransitionEpoch") == null) sendEvent(name: "lastTransitionEpoch", value: now())

    updateTileDisplay()
}

def parse(String description) { updateTileDisplay() }
def refreshTile() { updateTileDisplay() }

def setActiveMode(String modeVal) {
    if (modeVal != null) {
        logInfo "Setting activeMode -> '${modeVal}'"
        sendEvent(name: "activeMode", value: modeVal)
        sendEvent(name: "mode", value: modeVal)
        updateTileDisplay(modeVal, null, null, null)
    }
}

def setControlSource(String csVal) {
    if (csVal != null) {
        logInfo "Setting activeControlSource -> '${csVal}'"
        sendEvent(name: "activeControlSource", value: csVal)
        updateTileDisplay(null, csVal, null, null)
    }
}

// Backward compatibility alias wrapper
def setActiveReason(String csVal) { setControlSource(csVal) }

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
        sendEvent(name: "lastTransitionEpoch", value: now())
        updateTileDisplay(null, null, timeVal, null)
    }
}

def setTile(String tileContent) {
    if (tileContent != null) {
        sendEvent(name: "tile", value: tileContent)
        sendEvent(name: "html", value: tileContent)
    }
}

def setStatus(String modeVal, String controlSourceVal, String timeVal = null, String hsmVal = null, def metadataInput = null) {
    String currentMode = modeVal ?: device.currentValue("activeMode") ?: "Unknown"
    String currentCS = controlSourceVal ?: device.currentValue("activeControlSource") ?: "Unknown"
    String currentTime = timeVal ?: new Date().format("hh:mm:ss a", location.timeZone ?: TimeZone.getDefault())
    String currentHsm = hsmVal ?: device.currentValue("hsmStatus") ?: "disarmed"

    sendEvent(name: "activeMode", value: currentMode)
    sendEvent(name: "mode", value: currentMode)
    sendEvent(name: "activeControlSource", value: currentCS)
    sendEvent(name: "hsmStatus", value: currentHsm)
    sendEvent(name: "lastTransitionTime", value: currentTime)
    sendEvent(name: "lastTransitionEpoch", value: now())

    Map meta = [:]
    if (metadataInput instanceof Map) {
        meta = metadataInput
    } else if (metadataInput instanceof String && metadataInput.trim().startsWith("{")) {
        try {
            meta = new groovy.json.JsonSlurper().parseText(metadataInput) as Map
        } catch (Exception e) {
            logInfo "Unable to parse metadata JSON payload: ${e.message}"
        }
    }

    if (meta.prevMode != null) sendEvent(name: "previousMode", value: meta.prevMode.toString())
    
    // Support new 'prevControlSource' or legacy 'prevReason' key
    def prevCS = meta.prevControlSource ?: meta.prevReason
    if (prevCS != null) sendEvent(name: "previousControlSource", value: prevCS.toString())

    if (meta.periodKey != null) sendEvent(name: "activePeriodKey", value: meta.periodKey.toString())
    if (meta.isSleeping != null) sendEvent(name: "isSleeping", value: meta.isSleeping.toString())
    if (meta.txId != null) sendEvent(name: "lastTransactionId", value: meta.txId.toString())
    if (meta.source != null) sendEvent(name: "lastTriggerSource", value: meta.source.toString())

    logInfo "Status Batch Update -> Mode: '${currentMode}' | Control Source: '${currentCS}' | HSM: '${currentHsm}' | Time: '${currentTime}'"
    
    updateTileDisplay(currentMode, currentCS, currentTime, currentHsm)
}

private String getModeColor(String modeVal) {
    switch (modeVal) {
        case "Morning":  return "#F1C40F"
        case "Day":      return "#00FF66"
        case "Evening":  return "#E67E22"
        case "Night":    return "#9B59B6"
        case "Sleeping": return "#00FFFF"
        case "Away":     return "#E74C3C"
        default:         return "#1ABC9C"
    }
}

private String getControlSourceColor(String csVal) {
    switch (csVal) {
        case "Scheduled": return "#00FFFF"
        case "Voice":     return "#FF8C00"
        case "Presence":  return "#00FF66"
        case "Override":  return "#FF007F"
        case "Reboot":    return "#BB86FC"
        default:          return "#BDC3C7"
    }
}

private String formatHsmDisplay(String rawHsm) {
    switch (rawHsm) {
        case "armedAway":   return "<span style='color:#FF4D4D; font-weight:bold;'>Armed Away</span>"
        case "armedNight":  return "<span style='color:#00FFFF; font-weight:bold;'>Armed Night</span>"
        case "armedHome":   return "<span style='color:#FF9F43; font-weight:bold;'>Armed Home</span>"
        case "disarmed":    return "<span style='color:#00FF66; font-weight:bold;'>Disarmed</span>"
        case "allDisarmed": return "<span style='color:#00FF66; font-weight:bold;'>All Disarmed</span>"
        default:            return "<span style='color:#BDC3C7; font-weight:bold;'>${rawHsm}</span>"
    }
}

private String sanitizeFontSize(String inputVal, String defaultVal) {
    if (!inputVal) return defaultVal
    String trimmed = inputVal.trim()
    return (trimmed.endsWith("em") || trimmed.endsWith("px") || trimmed.endsWith("%")) ? trimmed : "${trimmed}em"
}

private void updateTileDisplay(String overrideMode = null, String overrideCS = null, String overrideTime = null, String overrideHsm = null) {
    String currentMode = overrideMode ?: device.currentValue("activeMode") ?: "Unknown"
    String currentCS = overrideCS ?: device.currentValue("activeControlSource") ?: "Unknown"
    String currentTime = overrideTime ?: device.currentValue("lastTransitionTime") ?: "None"
    String currentHsm = overrideHsm ?: device.currentValue("hsmStatus") ?: "disarmed"

    Boolean useHtml = (settings?.enableHtmlFormatting != false)

    String formattedTile = ""
    if (useHtml) {
        String modeColor = getModeColor(currentMode)
        String csColor = getControlSourceColor(currentCS)
        String hsmHtml = formatHsmDisplay(currentHsm)

        String mSize = sanitizeFontSize(settings?.modeFontSize, "0.9em")
        String csSize = sanitizeFontSize(settings?.controlSourceFontSize, "0.8em")
        String hSize = sanitizeFontSize(settings?.hsmFontSize, "0.8em")
        String uSize = sanitizeFontSize(settings?.updatedFontSize, "0.28em")

        String mWeight = (settings?.boldMode != false) ? "font-weight:bold;" : "font-weight:normal;"
        String csWeight = (settings?.boldControlSource != false) ? "font-weight:bold;" : "font-weight:normal;"
        String hWeight = (settings?.boldHsm != false) ? "font-weight:bold;" : "font-weight:normal;"
        String uWeight = (settings?.boldUpdated == true) ? "font-weight:bold;" : "font-weight:normal;"

        formattedTile = "<div style='text-align:center; padding:0.15em; font-family:sans-serif; color:#FFFFFF; width:100%; box-sizing:border-box;'>" +
                        "<div style='font-size:${mSize}; ${mWeight} color:#FFFFFF; margin-bottom:0.08em; line-height:1.15;'>Mode: <span style='color:${modeColor};'>${currentMode}</span></div>" +
                        "<div style='font-size:${csSize}; color:#FFFFFF; margin-bottom:0.08em; line-height:1.15;'>Control Source: <span style='color:${csColor}; ${csWeight}'>${currentCS}</span></div>" +
                        "<div style='font-size:${hSize}; color:#FFFFFF; ${hWeight} margin-bottom:0.08em; line-height:1.15;'>HSM: ${hsmHtml}</div>" +
                        "<div style='font-size:${uSize}; ${uWeight} color:#FFFFFF; opacity:0.65; line-height:1.1;'>Updated: ${currentTime}</div>" +
                        "</div>"
    } else {
        formattedTile = "Mode: ${currentMode} | Control Source: ${currentCS} | HSM: ${currentHsm} | Time: ${currentTime}"
    }

    sendEvent(name: "tile", value: formattedTile)
    sendEvent(name: "html", value: formattedTile)
}

def logInfo(String msg) {
    if (settings?.logInfoEnable != false) {
        log.info "${device.displayName}: ${msg}"
    }
}