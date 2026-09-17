/**
 * Echos Speak Server Status Tile Driver
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Custom driver for monitoring Echos Speak Node.js / Docker bridge authentication health, watchdog interval, 
 * session age, and generating compact HTML tile output for HD+ Dashboard display.
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
 * v1.1.0    09/15/26    jshimota    Aligned versioning and updated parent callback hooks for refresh and session revalidation.
 * v1.0.9    09/12/26    jshimota    Centered Last Validated footer row in htmlTile and scaled text down by 10%.
 * v1.0.8    09/12/26    jshimota    Redesigned htmlTile to include Server IP sub-header, Bridge/Watchdog green dots, restored Last Validated footer, and updated labels to Session Age / Next Check: ~.
 * v1.0.7    09/12/26    jshimota    Re-architected htmlTile layout with relative em units, added tileFontSize preference input, and fixed HD+ tile wrapping overflow.
 * v1.0.6    09/12/26    jshimota    Aligned driver definition name to Echos Speak Server Status Tile Driver and updated version tracking.
 * v1.0.5    09/12/26    jshimota    Added lastUpdated attribute to track exact tile refresh timestamp and incorporated it into tile HTML outputs.
 * v1.0.4    09/12/26    jshimota    Removed unused htmlAlt attribute to streamline driver execution and reduce attribute overhead.
 * v1.0.3    09/12/26    jshimota    Restored full legacy attribute suite (csrf, cookieData, amazonDomain, etc.) and enhanced responsive minimal CSS HTML tile output for HD+ Dashboards.
 * v1.0.2    09/12/26    jshimota    Re-architected HTML table builder to match ES Tile Custom template styling, removed obsolete 127.0.0.1 app scraping routines, and bound metrics directly to Node.js Watchdog payload.
 * v1.0.1    09/12/26    jshimota    Renamed driver to Echos Speak Server Status Tile Driver, aligned version tracking, and added trace log version demarcation.
 * v1.0.0    09/12/26    jshimota    Initial release of dedicated server status child device driver.
 **/

static String version() { return '1.1.0' }
def timeStamp() { return "2026/09/15 09:15 AM" }

import groovy.transform.Field

@Field static final String okSymFLD    = "\u2713"
@Field static final String notOkSymFLD = "<span style='color:red'>\u2715</span>"

metadata {
    definition (
        name: "Echos Speak Server Status Tile Driver",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/echos_speak_server_status_tile_driver/echos_speak_server_status_tile_driver.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"

        // Explicit Device Commands
        command "refresh"
        command "refreshHTML"
        command "revalidateSession"

        // Core Attributes (HD+ Dashboard & Rule Machine Integration)
        attribute "amazonDomain", "string"
        attribute "authStatus", "string"
        attribute "bridgeStatus", "string"
        attribute "cookieData", "string"
        attribute "csrf", "string"
        attribute "lastError", "string"
        attribute "lastUpdated", "string"
        attribute "lastValidated", "string"
        attribute "nextValidation", "string"
        attribute "serverIp", "string"
        attribute "serverLocation", "string"
        attribute "sessionAge", "string"
        
        // Formatted HTML Tile Output Attributes
        attribute "html", "string"
        attribute "htmlTile", "string"
    }

    preferences {
        input name: "tileFontSize", type: "enum", title: "<b>HD+ Tile Base Text Size (em)</b>", description: "Adjust overall scaling for HD+ dashboard tile layout", options: ["0.6":"0.6 em (Tiny)", "0.7":"0.7 em (Small)", "0.8":"0.8 em (Default)", "0.9":"0.9 em (Medium)", "1.0":"1.0 em (Large)"], defaultValue: "0.7", required: true
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Detailed Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper Routine
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

/* =========================================================================================
   DRIVER LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    initialize(false)
    refresh()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

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

/* =========================================================================================
   COMMAND IMPLEMENTATIONS
   ========================================================================================= */

void refresh() {
    logTrace "refresh() requested from Server Status Tile device"
    parent?.refreshServerStatusData()
}

void refreshHTML() {
    logDebug "Executing refreshHTML()..."
    refresh()
}

void revalidateSession() {
    logInfo "Manual session revalidation trigger requested..."
    parent?.forceServerSessionRevalidation()
}

/* =========================================================================================
   PARENT DATA PARSER & RESPONSIVE HTML TILE BUILDERS
   ========================================================================================= */

void parseServerStatusData(Map statusData) {
    if (!statusData) return
    logTrace "--> parseServerStatusData() Processing server metrics"

    String rawState = statusData.state ?: "OFFLINE"
    Boolean isAuth = (statusData.authenticated == true)
    String bridgeState = isAuth ? "online" : "offline"
    String lastErr = statusData.lastError ?: "None"
    String lastValid = statusData.lastValidated ? parseIsoTimestamp(statusData.lastValidated) : "Never"
    String ageStr = formatDuration(statusData.sessionAgeSeconds)
    String nextValStr = statusData.nextValidationSeconds != null ? "~${Math.round(statusData.nextValidationSeconds / 60)}m" : "N/A"
    
    String serverIpVal = parent?.getBridgeIp() ?: "192.168.1.12"
    
    // Format Current Refresh Timestamp
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    String nowFormatted = new Date().format("hh:mm:ss a", tz)

    // Populate Core HD+ & Rule Machine Attributes
    updateAttribute("amazonDomain", "amazon.com")
    updateAttribute("authStatus", rawState)
    updateAttribute("bridgeStatus", bridgeState)
    updateAttribute("cookieData", isAuth ? "true" : "false")
    updateAttribute("csrf", isAuth ? "true" : "false")
    updateAttribute("lastError", lastErr)
    updateAttribute("lastUpdated", nowFormatted)
    updateAttribute("lastValidated", lastValid)
    updateAttribute("nextValidation", nextValStr)
    updateAttribute("serverIp", serverIpVal)
    updateAttribute("serverLocation", "Local Synology NAS")
    updateAttribute("sessionAge", ageStr)

    // Resolve Base EM Font Size Preference
    String baseEm = settings.tileFontSize ?: "0.7"

    // Responsive Color & Dot Indicators
    String badgeColor = "#7F8C8D"
    String statusIcon = "⚪"
    String bridgeDot  = isAuth ? "🟢" : "🔴"
    String watchdogDot = (rawState == "AUTHENTICATED") ? "🟢" : (rawState == "DEGRADED" ? "🟡" : "🔴")

    switch (rawState) {
        case "AUTHENTICATED":
            badgeColor = "#27AE60"
            statusIcon = "🟢"
            break
        case "DEGRADED":
            badgeColor = "#F39C12"
            statusIcon = "🟡"
            break
        case "AUTH_REQUIRED":
            badgeColor = "#C0392B"
            statusIcon = "🔴"
            break
    }

    // Fully Responsive HD+ Dashboard HTML Tile Output (htmlTile)
    String tileHtml = "<div style='font-size:${baseEm}em; background-color:#1A252F; color:#FFFFFF; padding:0.5em 0.6em; border-radius:0.5em; font-family:system-ui, -apple-system, sans-serif; border-left:0.4em solid ${badgeColor}; box-sizing:border-box; width:100%; overflow:hidden;'>" +
                      "<div style='display:flex; justify-content:space-between; align-items:flex-start; border-bottom:0.08em solid #34495E; padding-bottom:0.3em; margin-bottom:0.4em; gap:0.4em;'>" +
                      "<div>" +
                      "<div style='font-size:0.95em; font-weight:700; color:#3498DB; white-space:nowrap;'>Echos Speak Server</div>" +
                      "<div style='font-size:0.75em; color:#7F8C8D;'>${serverIpVal}</div>" +
                      "</div>" +
                      "<div style='text-align:right; flex-shrink:0;'>" +
                      "<div style='font-size:0.85em; font-weight:700; color:${badgeColor}; white-space:nowrap;'>${statusIcon} ${rawState}</div>" +
                      "<div style='font-size:0.7em; color:#BDC3C7;'>Bridge: ${bridgeDot} &nbsp;Watchdog: ${watchdogDot}</div>" +
                      "</div>" +
                      "</div>" +
                      "<div style='display:flex; justify-content:space-between; gap:0.4em; color:#BDC3C7; font-size:0.85em; line-height:1.2em;'>" +
                      "<div style='white-space:nowrap;'><b>Session Age:</b> <span style='color:#FFF;'>${ageStr}</span></div>" +
                      "<div style='white-space:nowrap;'><b>Next Check:</b> <span style='color:#FFF;'>${nextValStr}</span></div>" +
                      "</div>" +
                      "<div style='border-top:0.05em solid #2C3E50; margin-top:0.3em; padding-top:0.2em; font-size:0.675em; color:#95A5A6; text-align:center;'>" +
                      "<span><b>Last Validated:</b> ${lastValid}</span>" +
                      "</div>" +
                      (lastErr != "None" ? "<div style='color:#E74C3C; margin-top:0.2em; font-size:0.7em; font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; text-align:center;'>⚠️ ${lastErr}</div>" : "") +
                      "</div>"

    updateAttribute("htmlTile", tileHtml)

    // Build Standard Table Output (html)
    String authSymbol = isAuth ? okSymFLD : notOkSymFLD
    String bridgeSymbol = isAuth ? okSymFLD : notOkSymFLD
    String watchdogSymbol = (rawState == "AUTHENTICATED") ? okSymFLD : notOkSymFLD

    String baseTable = "<table style='color:mediumblue;font-size:small;border-collapse:collapse;'>" +
                       "<tr><th style='text-align:left;'>Auth Status: ${authSymbol}</th></tr>" +
                       "<tr><td>&nbsp;&nbsp;Bridge: ${bridgeSymbol}</td></tr>" +
                       "<tr><td>&nbsp;&nbsp;Watchdog: ${watchdogSymbol}</td></tr>" +
                       "<tr><th style='text-align:left;padding-top:4px;'>Session Metrics</th></tr>"

    String refreshRow = (rawState == "AUTHENTICATED") 
        ? "<tr><td>Next Check: ${nextValStr}</td></tr>" 
        : "<tr><td style='color:red;font-weight:bold;'>Attention: ${rawState}</td></tr>"

    String wkStr = baseTable + 
                   "<tr><td>Last Validated: ${lastValid}</td></tr>" + 
                   "<tr><td>Session Age: ${ageStr}</td></tr>" + 
                   refreshRow +
                   "<tr><th style='text-align:left;padding-top:4px;'>Server Data</th></tr>" +
                   "<tr><td>Server IP: ${serverIpVal}</td></tr>" +
                   "<tr><td>Status: ${rawState}</td></tr>" +
                   "<tr><td>Tile Refreshed: ${nowFormatted}</td></tr>" +
                   (lastErr != "None" ? "<tr><td style='color:red;font-size:10px;'>Error: ${lastErr}</td></tr>" : "") +
                   "</table>"
    updateAttribute("html", wkStr)

    logTrace "<-- parseServerStatusData() Exit"
}

private void updateAttribute(String name, Object value) {
    if (device.currentValue(name)?.toString() != value?.toString()) {
        sendEvent(name: name, value: value)
    }
}

private String formatDuration(Object secondsObj) {
    if (secondsObj == null) return "Unknown"
    long sec = secondsObj.toString().toLong()
    long days = sec / 86400
    long hours = (sec % 86400) / 3600
    long mins = (sec % 3600) / 60

    if (days > 0) return "${days}d ${hours}h"
    if (hours > 0) return "${hours}h ${mins}m"
    return "${mins}m"
}

private String parseIsoTimestamp(String isoStr) {
    try {
        Date d = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", isoStr)
        TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
        return d.format("MMM dd hh:mm a", tz)
    } catch (Exception e) {
        return isoStr
    }
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

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Server Status Tile Device"
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