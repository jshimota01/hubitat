/**
 * Advanced vThermostat Manager (Custom) Parent App
 * Platform: Hubitat Elevation
 * Notes: Custom parent manager app for organizing Advanced vThermostat Child (Custom) instances
 **/
/**
 * Copyright 2026 James Shimota / Original 2020 Nelson Clark
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
 *  Parent application container for managing child instances of Advanced vThermostat Child (Custom).
 *
 *  Instructions:
 *  1. Install this parent application.
 *  2. Add new virtual thermostat instances from within this parent app interface.
 *  
 *  Changelog:
 *  v2.3.2    08/30/26    jshimota    Updated child creation callouts to highlight automatic 'Virtual' room placement
 *  v2.3.1    08/30/26    jshimota    Finalized refactor alignment for App Master Template v1.1.0
 *  v2.3.0    08/30/26    jshimota    Applied v1.1.0 App Master Template (styled banner, badging, collapsible prefs, settings snapshot hash)
 *  v2.2.1    08/30/26    jshimota    Updated definition display name to Advanced vThermostat Manager (Custom)
 *  v2.2.0    08/30/26    jshimota    Applied initial App Master Template
 *  v2.1.1    08/30/26    jshimota    Formatted names to use (Custom) in parenthetical style
 *  v2.1.0    08/30/26    jshimota    Removed v2 identifiers and updated references
 *  v2.0.0    08/22/26    jshimota    Bumped definition name and aligned icon/import URLs
 **/

static String version() { return '2.3.2' }
def timeStamp() { return "2026/08/30 09:33 AM" }

definition(
    name: "Advanced vThermostat Manager (Custom)",
    namespace: "jshimota",
    author: "Nelson Clark / Customizations by jshimota",
    description: "Join any sensor(s) with any outlet(s) for virtual thermostat control.",
    category: "Green Living",
    iconUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-logo-small.png",
    iconX2Url: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-logo.png",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat_Custom_Parent.groovy",
    singleInstance: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* Styled App Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Advanced vThermostat Manager (Custom)</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        section("<b>Installed Thermostats</b>") {
            app(name: "thermostats", appName: "Advanced vThermostat Child (Custom)", namespace: "jshimota", title: "Add New Advanced vThermostat Instance", multiple: true)
        }

        /* Collapsible App Preferences & Logging */
        section("<b>App Preferences & Logging Options</b>", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }
    }
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// NPE-Safe Timestamp Helper Routine
private String getTimestamp() {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Advanced vThermostat Manager (Custom)"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

// Settings Hash Snapshot Helper
private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn")) }
        .sort()

    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
}

// Hubitat App Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing app v${version()} (${timeStamp()})..."
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)

    if (settingsChanged) {
        logInfo "Settings modification detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(false)
    } else {
        logDebug "Manager app closed without setting changes. Skipping re-initialization."
    }
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling manager app..."
    unsubscribe()
    unschedule()
}

private void initialize(Boolean isInstall = false) {
    logDebug "Initializing Manager App; ${childApps.size()} child instance(s) connected."
    childApps.each { child -> 
        logTrace "  child app registered: ${child.label}"
    }

    updateAppLabel()

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
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
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

// Centralized Logging Engine
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appLabel = app.label ?: app.name ?: "App"
    
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${appLabel}: ${msg}"
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