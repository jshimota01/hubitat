/**
 * Virtual Mode Status & State Tracker Tile Driver
 * Platform: Hubitat Elevation
 * Notes: Custom virtual status device for storing ground-truth Mode, Control Source, HSM Status, and HTML Dashboard Tile attributes for Mode Manager Advanced
 * Capabilities: Actuator, Sensor, Configuration, Refresh
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
 *  Tracks active location mode, control source, HSM status, and transition timestamps while generating colorized HTML tile attributes for Hubitat dashboards.
 *
 *  Instructions:
 *  1. Create virtual device using this driver.
 *  2. Connect to Mode Manager Advanced to automatically stream mode transitions, control sources, and diagnostic metadata.
 *  
 *  Changelog:
 *  v1.9.3    10/20/26    jshimota    Corrected font input validation in sanitizeFontSize() using strict regex matching; enforced pure numbers and optional 'em' suffix, rejected invalid units (px, %, rem), malformed strings, and out-of-bounds numbers.
 *  v1.9.2    10/20/26    jshimota    Enforced strict relative EM unit scaling; removed non-responsive unit overrides (px, %, rem) and streamlined sanitizeFontSize() to strictly output CSS EM values.
 *  v1.9.1    10/20/26    jshimota    Hardened font-size validation against explicit numeric bounds (0.1..3.0) and refined flex layout container for auto vs fixed parent height rendering.
 *  v1.9.0    10/20/26    jshimota    Simplified font-size preferences to pure numeric multipliers, added responsive flex-column vertical expansion, improved typography hierarchy, and cleaned inline HTML/CSS styling.
 *  v1.8.2    10/13/26    jshimota    Updated HSM Disarmed and All Disarmed status color to match Scheduled control source color (#00FFFF)
 *  v1.8.1    10/13/26    jshimota    Updated HSM status colors: Disarmed set to control source color (#ffffff), Armed states set to green (#2ecc71), and Intrusion / Alert states set to red (#e74c3c)
 *  v1.8.0    10/13/26    jshimota    Redesigned HTML tile layout and color scheme to match standard card snapshot design (top header bar, key-value rows, status accents, and muted footer timestamp) while maintaining custom preference sizing
 *  v1.7.2    08/30/26    jshimota    Removed redundant 'html' attribute in favor of single 'tile' attribute for dashboard display
 *  v1.7.1    08/30/26    jshimota    Resolved double-logging redundancy in setter methods, added null-safe initialization checks, and updated trace handling
 *  v1.7.0    08/30/26    jshimota    Refactored log outputs: moved individual attribute logs to trace, preserved summary log at info level
 *  v1.6.3    08/29/26    jshimota    Integrated version demarcation tracing and non-overwriting debug logging timer from master template
 *  v1.6.2    08/29/26    jshimota    Cleaned up tile/html attribute log entries to output clean update notices instead of raw HTML markup
 *  v1.6.1    08/28/26    jshimota    Applied master driver template, sendIfChanged deduplication, NPE timestamp helpers, and centralized logging engine
 *  v1.6.0    08/27/26    jshimota    Terminology Refactor (Reason -> Control Source); updated setStatus payload parser to accept controlSource keys
 *  v1.5.0    08/23/23    jshimota    Added downstream diagnostic attributes (previousMode, activePeriodKey, etc.) and flexible payload overload
 *  v1.4.3    08/22/26    jshimota    Lightened Sleeping mode color from #3498DB to vivid cyan (#00FFFF) for enhanced contrast
 *  v1.4.2    08/21/26    jshimota    Minor typography tweaks
 *  v1.4.1    08/21/26    jshimota    Fine-tuned typography sizing and disarmed contrast fix
 *  v1.4.0    08/21/26    jshimota    Scaled font sizes and high-contrast dark tile color palette
 *  v1.3.0    08/21/26    jshimota    Native HSM status integration
 *  v1.2.3    08/18/26    jshimota    Code integrity and preference rendering optimization
 *  v1.2.2    08/18/26    jshimota    Change history log restoration
 *  v1.2.1    08/18/26    jshimota    Preferences null object protection fix
 *  v1.2.0    08/18/26    jshimota    Responsive font sizing, bold styling preferences, and UI help
 *  v1.1.0    08/18/26    jshimota    Logical color mapping integration
 *  v1.0.0    08/16/26    jshimota    Initial release of Virtual Status Tile & State Tracker Driver
 **/

static String version() { return '1.9.3' }
def timeStamp() { return "2026/10/20 10:45 AM" }

metadata {
    definition (
        name: "Virtual Mode Status & State Tracker Tile Driver",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota/Hubitat/Drivers/virtual_mode_status_tile_driver.groovy"
    ) {
        capability "Actuator"
        capability "Sensor"
        capability "Configuration"
        capability "Refresh"

        // Primary Display Attributes
        attribute "driverVersion", "string"
        attribute "activeMode", "string"
        attribute "activeControlSource", "string"
        attribute "hsmStatus", "string"
        attribute "lastTransitionTime", "string"
        attribute "tile", "string"

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
        command "resetDriver"
        
        // Backward Compatibility Alias
        command "setActiveReason", ["string"]
    }

    preferences {
        input name: "enableHtmlFormatting", type: "bool", title: "<b>Enable HTML Styling for Tile Attribute</b>", defaultValue: true, required: true, submitOnChange: true

        if (settings?.enableHtmlFormatting != false) {
            input name: "modeFontSize", type: "text", title: "<b>Mode Font Scale (EM)</b> <i>(Default: 0.95 = 0.95em)</i>", defaultValue: "0.95", required: true
            input name: "boldMode", type: "bool", title: "<b>Bold Mode Text?</b>", defaultValue: true, required: true

            input name: "controlSourceFontSize", type: "text", title: "<b>Control Source Font Scale (EM)</b> <i>(Default: 0.9 = 0.9em)</i>", defaultValue: "0.9", required: true
            input name: "boldControlSource", type: "bool", title: "<b>Bold Control Source Text?</b>", defaultValue: true, required: true

            input name: "hsmFontSize", type: "text", title: "<b>HSM Status Font Scale (EM)</b> <i>(Default: 0.85 = 0.85em)</i>", defaultValue: "0.85", required: true
            input name: "boldHsm", type: "bool", title: "<b>Bold HSM Text?</b>", defaultValue: true, required: true

            input name: "updatedFontSize", type: "text", title: "<b>Updated Timestamp Font Scale (EM)</b> <i>(Default: 0.65 = 0.65em)</i>", defaultValue: "0.65", required: true
            input name: "boldUpdated", type: "bool", title: "<b>Bold Updated Timestamp Text?</b>", defaultValue: false, required: true
        }

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// NPE-Safe Timestamp Helper Routine
private String getTimestamp() {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)
}

// Scheduled Cron / Interval Setup Helper
def setupSchedule() {
}

void parse(String description) {
    logDebug "parse(): ${description}"
    updateTileDisplay()
}

// Custom Capability Commands
def refreshTile() {
    logDebug "refreshTile() requested"
    updateTileDisplay()
}

def refresh() {
    refreshTile()
    return []
}

def setActiveMode(String modeVal) {
    if (modeVal != null) {
        sendIfChanged([name: "activeMode", value: modeVal])
        sendIfChanged([name: "mode", value: modeVal])
        updateTileDisplay(modeVal, null, null, null)
    }
}

def setControlSource(String csVal) {
    if (csVal != null) {
        sendIfChanged([name: "activeControlSource", value: csVal])
        updateTileDisplay(null, csVal, null, null)
    }
}

// Backward compatibility alias wrapper
def setActiveReason(String csVal) { setControlSource(csVal) }

def setHsmStatus(String hsmVal) {
    if (hsmVal != null) {
        sendIfChanged([name: "hsmStatus", value: hsmVal])
        updateTileDisplay(null, null, null, hsmVal)
    }
}

def setLastTransitionTime(String timeVal) {
    if (timeVal != null) {
        sendIfChanged([name: "lastTransitionTime", value: timeVal])
        sendIfChanged([name: "lastTransitionEpoch", value: now()])
        updateTileDisplay(null, null, timeVal, null)
    }
}

def setTile(String tileContent) {
    if (tileContent != null) {
        sendIfChanged([name: "tile", value: tileContent, descriptionText: "Dashboard Tile updated"])
    }
}

def setStatus(String modeVal, String controlSourceVal, String timeVal = null, String hsmVal = null, def metadataInput = null) {
    String currentMode = modeVal ?: device.currentValue("activeMode") ?: "Unknown"
    String currentCS = controlSourceVal ?: device.currentValue("activeControlSource") ?: "Unknown"
    String currentTime = timeVal ?: new Date().format("hh:mm:ss a", location?.timeZone ?: TimeZone.getDefault())
    String currentHsm = hsmVal ?: device.currentValue("hsmStatus") ?: "disarmed"

    sendIfChanged([name: "activeMode", value: currentMode])
    sendIfChanged([name: "mode", value: currentMode])
    sendIfChanged([name: "activeControlSource", value: currentCS])
    sendIfChanged([name: "hsmStatus", value: currentHsm])
    sendIfChanged([name: "lastTransitionTime", value: currentTime])
    sendIfChanged([name: "lastTransitionEpoch", value: now()])

    Map meta = [:]
    if (metadataInput instanceof Map) {
        meta = metadataInput
    } else if (metadataInput instanceof String && metadataInput.trim().startsWith("{")) {
        try {
            meta = new groovy.json.JsonSlurper().parseText(metadataInput) as Map
        } catch (Exception e) {
            logWarn "Unable to parse metadata JSON payload: ${e.message}"
        }
    }

    if (meta.prevMode != null) sendIfChanged([name: "previousMode", value: meta.prevMode.toString()])
    
    def prevCS = meta.prevControlSource ?: meta.prevReason
    if (prevCS != null) sendIfChanged([name: "previousControlSource", value: prevCS.toString()])

    if (meta.periodKey != null) sendIfChanged([name: "activePeriodKey", value: meta.periodKey.toString()])
    if (meta.isSleeping != null) sendIfChanged([name: "isSleeping", value: meta.isSleeping.toString()])
    if (meta.txId != null) sendIfChanged([name: "lastTransactionId", value: meta.txId.toString()])
    if (meta.source != null) sendIfChanged([name: "lastTriggerSource", value: meta.source.toString()])

    logInfo "Status Batch Update -> Mode: '${currentMode}' | Control Source: '${currentCS}' | HSM: '${currentHsm}' | Time: '${currentTime}'"
    
    updateTileDisplay(currentMode, currentCS, currentTime, currentHsm)
}

private String getModeColor(String modeVal) {
    switch (modeVal) {
        case "Morning":  return "#2ecc71"
        case "Day":      return "#2ecc71"
        case "Evening":  return "#e67e22"
        case "Night":    return "#9b59b6"
        case "Sleeping": return "#00ffff"
        case "Away":     return "#e74c3c"
        default:         return "#2ecc71"
    }
}

private String getControlSourceColor(String csVal) {
    switch (csVal) {
        case "Scheduled": return "#00ffff"
        case "Voice":     return "#ff8c00"
        case "Presence":  return "#2ecc71"
        case "Override":  return "#ff007f"
        case "Reboot":    return "#bb86fc"
        default:          return "#ffffff"
    }
}

private String formatHsmDisplay(String rawHsm) {
    switch (rawHsm) {
        case "disarmed":    return "<span style='color:#00ffff;'>DISARMED</span>"
        case "allDisarmed": return "<span style='color:#00ffff;'>ALL DISARMED</span>"

        case "armedAway":   return "<span style='color:#2ecc71;'>ARMED AWAY</span>"
        case "armedNight":  return "<span style='color:#2ecc71;'>ARMED NIGHT</span>"
        case "armedHome":   return "<span style='color:#2ecc71;'>ARMED HOME</span>"
        case "arming":      return "<span style='color:#2ecc71;'>ARMING</span>"

        case "intrusion":
        case "intrusion-home":
        case "intrusion-night":
        case "intrusion-away":
        case "alert":       return "<span style='color:#e74c3c;'>${rawHsm.replace('-', ' ').toUpperCase()}</span>"

        default:            return "<span style='color:#ffffff;'>${rawHsm.toUpperCase()}</span>"
    }
}

private String sanitizeFontSize(String inputVal, String defaultVal) {
    if (!inputVal) return defaultVal
    String trimmed = inputVal.trim().toLowerCase()
    
    // Strict Regex Match: Requires valid decimal/integer with optional 'em' suffix only
    def matcher = (trimmed =~ /^([0-9]+(?:\.[0-9]+)?)(?:em)?$/)
    if (!matcher.matches()) {
        return defaultVal
    }
    
    String rawNumStr = matcher[0][1]
    
    try {
        BigDecimal num = rawNumStr.toBigDecimal()
        if (num >= 0.1 && num <= 3.0) {
            return "${num.stripTrailingZeros().toPlainString()}em"
        }
    } catch (Exception e) {
        // Fall back to default on parse exception
    }
    
    return defaultVal
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

        String mSize = sanitizeFontSize(settings?.modeFontSize, "0.95em")
        String csSize = sanitizeFontSize(settings?.controlSourceFontSize, "0.9em")
        String hSize = sanitizeFontSize(settings?.hsmFontSize, "0.85em")
        String uSize = sanitizeFontSize(settings?.updatedFontSize, "0.65em")

        String mWeight = (settings?.boldMode != false) ? "font-weight:bold;" : "font-weight:normal;"
        String csWeight = (settings?.boldControlSource != false) ? "font-weight:bold;" : "font-weight:normal;"
        String hWeight = (settings?.boldHsm != false) ? "font-weight:bold;" : "font-weight:normal;"
        String uWeight = (settings?.boldUpdated == true) ? "font-weight:bold;" : "font-weight:normal;"

        // Flexible vertical container with EM responsive scaling
        formattedTile = "<div style='background-color:#1e242a; border-radius:6px; padding:10px 12px; font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif; color:#ffffff; width:100%; min-height:100%; box-sizing:border-box; display:flex; flex-direction:column; justify-content:space-between; gap:6px;'>" +
                        "<div style='display:flex; flex-direction:column; gap:6px; width:100%;'>" +
                        "<div style='display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid rgba(255,255,255,0.12); padding-bottom:6px;'>" +
                        "<span style='color:#3498db; font-weight:bold; font-size:${mSize};'>Mode Status</span>" +
                        "<span style='color:${modeColor}; ${mWeight} font-size:${mSize};'>${currentMode}</span>" +
                        "</div>" +
                        "<div style='display:flex; justify-content:space-between; align-items:center; font-size:${csSize};'>" +
                        "<span style='color:#a0aab2;'>Control Source</span>" +
                        "<span style='color:${csColor}; ${csWeight}'>${currentCS}</span>" +
                        "</div>" +
                        "<div style='display:flex; justify-content:space-between; align-items:center; font-size:${hSize};'>" +
                        "<span style='color:#a0aab2;'>HSM</span>" +
                        "<span style='${hWeight}'>${hsmHtml}</span>" +
                        "</div>" +
                        "</div>" +
                        "<div style='text-align:center; font-size:${uSize}; ${uWeight} color:#808c96; padding-top:4px; border-top:1px solid rgba(255,255,255,0.05);'>" +
                        "Updated: ${currentTime}" +
                        "</div>" +
                        "</div>"
    } else {
        formattedTile = "Mode: ${currentMode} | Control Source: ${currentCS} | HSM: ${currentHsm} | Time: ${currentTime}"
    }

    sendIfChanged([name: "tile", value: formattedTile, descriptionText: "Dashboard Tile updated"])
}

// Hubitat Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
    setupSchedule()
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
    setupSchedule()
    return []
}

private void initialize(Boolean isInstall = false) {
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)

    if (device.currentValue("activeMode") == null) sendEvent(name: "activeMode", value: "Unknown", isStateChange: true)
    if (device.currentValue("mode") == null) sendEvent(name: "mode", value: "Unknown", isStateChange: true)
    if (device.currentValue("activeControlSource") == null) sendEvent(name: "activeControlSource", value: "Unknown", isStateChange: true)
    if (device.currentValue("hsmStatus") == null) sendEvent(name: "hsmStatus", value: "disarmed", isStateChange: true)
    if (device.currentValue("lastTransitionTime") == null) sendEvent(name: "lastTransitionTime", value: "None", isStateChange: true)
    
    if (device.currentValue("previousMode") == null) sendEvent(name: "previousMode", value: "Unknown", isStateChange: true)
    if (device.currentValue("previousControlSource") == null) sendEvent(name: "previousControlSource", value: "Unknown", isStateChange: true)
    if (device.currentValue("activePeriodKey") == null) sendEvent(name: "activePeriodKey", value: "none", isStateChange: true)
    if (device.currentValue("isSleeping") == null) sendEvent(name: "isSleeping", value: "false", isStateChange: true)
    if (device.currentValue("lastTransactionId") == null) sendEvent(name: "lastTransactionId", value: "none", isStateChange: true)
    if (device.currentValue("lastTriggerSource") == null) sendEvent(name: "lastTriggerSource", value: "Initialization", isStateChange: true)
    if (device.currentValue("lastTransitionEpoch") == null) sendEvent(name: "lastTransitionEpoch", value: now(), isStateChange: true)

    updateTileDisplay()

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

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

// Master Utility Routine for Driver GUI Button
void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    logInfo "Driver reset process completed."
}

// Individual Utility Routines
void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    state.clear()
    logInfo "All states have been cleared."
}

void clearAllAttributes() {
    logInfo "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
    logInfo "All attributes have been cleared."
}

void clearAllSchedules() {
    logInfo "Clearing all scheduled jobs (including orphaned schedules)..."
    unschedule()
    logInfo "All scheduled jobs have been successfully cleared."
}

// State-De-Duplication Helper Routine
private void sendIfChanged(Map args) {
    if (!args || !args.name) return

    String nameStr = args.name as String
    String oldVal = device.currentValue(nameStr)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        String desc = args.descriptionText ?: "${nameStr} set to ${args.value}"
        Map eventMap = [
            name: nameStr, 
            value: args.value, 
            descriptionText: desc
        ]
        if (args.unit) eventMap.unit = args.unit
        if (args.type) eventMap.type = args.type
        if (args.isStateChange != null) eventMap.isStateChange = args.isStateChange

        sendEvent(eventMap)
        logTrace "${desc}"
    }
}

// Centralized Logging Engine
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