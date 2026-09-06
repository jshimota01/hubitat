/**
 * Advanced Porch Lighting Manager
 * Platform: Hubitat Elevation
 * Notes: Dynamically controls porch and driveway lights based on Twilight Parser event schedules with OWM diagnostic monitoring.
 * Category: Convenience / Environmental
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
 *  Coordinates outdoor lighting automatically using live metrics from OpenWeatherMap (Lux, Sun Altitude, Cloud PCT)
 *  and/or Twilight Parser drivers. Eliminates seasonal twilight drifts using dynamic angle and cloud attenuation.
 *
 *  Instructions:
 *  1. Install in Hubitat Apps Code and add the App.
 *  2. Select target switches, OpenWeatherMap device, and Twilight Parser device.
 *  3. Choose evaluation method (Lux Threshold, Solar Elevation Angle + Overcast Shift, or Twilight Variable Offset).
 *  4. Configure cutoff/schedule times and save.
 *  
 *  Changelog:
 *	v2.0.2    09/06/26    jshimota    Logging expansion: Added logTraceEnable option to App Preferences and embedded deep trace logging wrappers across schedule construction, event handlers, and helper routines.
 *	v2.0.1    09/06/26    jshimota    GUI cleanup: Made Device Connections and Schedule Configuration collapsible (default collapsed), rephrased twilight offset inputs to handle positive numbers safely without validation errors, and aligned dashboard grid layout.
 *	v2.0.0    09/06/26    jshimota    Complete architectural rewrite: Separated scheduling authority (Twilight Parser) from environmental monitoring (OWM). Removed OWM-driven switch triggers and hardcoded hour tests. Added new status dashboard.
 *	v1.0.4    09/05/26    jshimota    Added nightly schedule refresh at 12:02 AM and bulletproofed unschedule routines against orphan jobs.
 *	v1.0.3    09/05/26    jshimota    Bug check: Added morning turn-off evaluation for angle/lux modes, enhanced time-window guard logic, and modernized GUI card containers.
 *	v1.0.2    09/05/26    jshimota    Fixed notification device capability selector and split solar angles into morning/evening thresholds with independent cloud cover shifts.
 *	v1.0.1    09/05/26    jshimota    Added live UI status dashboard and updated version tracking.
 *	v1.0.0    09/05/26    jshimota    Initial release built on standardized App Template v1.2.2.
 **/

static String version() { return '2.0.2' }
def timeStamp() { return "2026/09/06 10:05 AM" }

definition(
    name: "Advanced Porch Lighting Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Automates porch and outdoor lights using Twilight Parser timestamps and OWM diagnostic monitoring.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_porch_lighting_manager/advanced_porch_lighting_manager.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* Styled App Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:12px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Advanced Porch Lighting Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.85;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        /* Real-Time Environment & Schedule Status Board */
        section() {
            paragraph getStatusBoardHtml()
        }

        /* 1. Device Connections (Collapsible & Collapsed by Default) */
        section("<b>1. Device Connections</b>", hideable: true, hidden: true) {
            input name: "switches", type: "capability.switch", title: "Select Switches to Control", multiple: true, required: true
            input name: "twilightDevice", type: "capability.sensor", title: "Select Twilight Parser Device (Schedule Source)", multiple: false, required: true, submitOnChange: true
            input name: "owmDevice", type: "capability.sensor", title: "Select OpenWeatherMap Device (Diagnostic Context)", multiple: false, required: false, submitOnChange: true
            input name: "notifier", type: "capability.notification", title: "Select Notification Device (Optional)", multiple: true, required: false
        }

        /* 2. Schedule Configuration (Collapsible & Collapsed by Default) */
        section("<b>2. Schedule Configuration</b>", hideable: true, hidden: true) {
            input name: "mornOffset", type: "number", title: "Morning Turn-ON Lead Time before Twilight Begin (Minutes)", description: "Enter minutes BEFORE Twilight Begin (e.g. 15 turns lights ON 15 mins before twilight)", defaultValue: 15, required: true, submitOnChange: true
            input name: "morningOffTime", type: "time", title: "Morning Turn-OFF Time / Sunrise Cutoff (Optional)", description: "Leave blank to turn OFF automatically at Sunrise", required: false, submitOnChange: true
            input name: "eveOffset", type: "number", title: "Evening Turn-ON Lead Time before Twilight End (Minutes)", description: "Enter minutes BEFORE Twilight End (e.g. 15 turns lights ON 15 mins before twilight)", defaultValue: 15, required: true, submitOnChange: true
            input name: "eveningCutoffTime", type: "time", title: "Night Turn-OFF Time / Midnight Cutoff", defaultValue: "23:59", required: true, submitOnChange: true
        }

        /* Collapsible App Preferences & Logging Options */
        section("<b>3. App Preferences & Logging Options</b>", hideable: true, hidden: true) {
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

/* Status Dashboard HTML Generator (Clean Grid Alignment) */
String getStatusBoardHtml() {
    logTrace "getStatusBoardHtml() -> Generating live environment dashboard..."
    String switchState = switches ? (switches[0].currentValue("switch") ?: "off") : "unknown"
    String badgeColor  = (switchState == "on") ? "#2e7d32" : "#757575"
    String badgeText   = (switchState == "on") ? "LIGHTS ON 💡" : "LIGHTS OFF 🌙"

    // OWM Diagnostic Attributes
    String betwixtText   = owmDevice ? (owmDevice.currentValue("betwixt") ?: "N/A") : "N/A"
    String sunAltText    = owmDevice ? (owmDevice.currentValue("currentSunAltitudeText") ?: "--°") : "--°"
    String sunAzText     = owmDevice ? (owmDevice.currentValue("currentSunAzimuthText") ?: "--°") : "--°"
    String cloudPctText  = owmDevice ? "${owmDevice.currentValue('currentCloudPCT') ?: 0}%" : "N/A"
    
    // Internal Lighting State Determination
    String lightingState = "NIGHTTIME"
    if (owmDevice) {
        String isDay = owmDevice.currentValue("currentIsDay") ?: "false"
        if (isDay == "true") {
            lightingState = "DAYTIME"
        } else if (betwixtText.contains("twilight")) {
            lightingState = "TWILIGHT"
        }
    }

    String mornOnDisp  = state.schedMornOnDisp  ?: "--"
    String mornOffDisp = state.schedMornOffDisp ?: (morningOffTime ? formatIsoTimeStr(morningOffTime) : "Sunrise")
    String eveOnDisp   = state.schedEveOnDisp   ?: "--"
    String eveOffDisp  = state.schedEveOffDisp  ?: (eveningCutoffTime ? formatIsoTimeStr(eveningCutoffTime) : "11:59 PM")
    String nextEvent   = state.nextScheduledEventDesc ?: "None scheduled"

    String html = """
    <div style='background-color:#1e1e24; color:#ffffff; padding:12px; border-radius:8px; font-family:sans-serif;'>
        <div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;'>
            <span style='font-weight:bold; font-size:1.05em;'>Porch Lighting Manager</span>
            <span style='background-color:${badgeColor}; color:#fff; padding:3px 8px; border-radius:4px; font-weight:bold; font-size:0.85em;'>${badgeText}</span>
        </div>

        <div style='margin-bottom:10px; font-size:0.9em; color:#ddd;'>
            <b>Lighting State:</b> <span style='color:#00BCD4; font-weight:bold;'>${lightingState}</span>
        </div>

        <!-- Aligned Schedule Timetable Grid -->
        <table style='width:100%; color:#fff; text-align:center; margin-bottom:12px; border-collapse:collapse; background:#2A2A32; border-radius:6px; table-layout:fixed;'>
            <thead>
                <tr style='background:#333440; font-weight:bold; font-size:0.85em; color:#FFC107;'>
                    <th style='padding:8px; width:50%; border-right:1px solid #444; text-align:center;'>🌅 MORNING PERIOD</th>
                    <th style='padding:8px; width:50%; text-align:center;'>🌆 EVENING PERIOD</th>
                </tr>
            </thead>
            <tbody>
                <tr style='font-size:0.88em; border-top:1px solid #444;'>
                    <td style='padding:10px 6px; border-right:1px solid #444; vertical-align:middle;'>
                        <div style='margin-bottom:4px;'><b>ON:</b> <span style='color:#81C784; font-weight:bold;'>${mornOnDisp}</span></div>
                        <div><b>OFF:</b> <span style='color:#E57373; font-weight:bold;'>${mornOffDisp}</span></div>
                    </td>
                    <td style='padding:10px 6px; vertical-align:middle;'>
                        <div style='margin-bottom:4px;'><b>ON:</b> <span style='color:#81C784; font-weight:bold;'>${eveOnDisp}</span></div>
                        <div><b>OFF:</b> <span style='color:#E57373; font-weight:bold;'>${eveOffDisp}</span></div>
                    </td>
                </tr>
            </tbody>
        </table>

        <!-- OWM Environmental Diagnostics Grid -->
        <table style='width:100%; color:#bbb; font-size:0.85em; border-collapse:collapse; margin-bottom:8px; table-layout:fixed;'>
            <tr style='border-bottom:1px solid #333;'>
                <td style='padding:5px 2px; width:50%;'>🧭 <b>OWM Segment:</b> ${betwixtText}</td>
                <td style='padding:5px 2px; width:50%;'>☀️ <b>Solar Altitude:</b> ${sunAltText}</td>
            </tr>
            <tr>
                <td style='padding:5px 2px; padding-top:6px;'>☁️ <b>Cloud Cover:</b> ${cloudPctText}</td>
                <td style='padding:5px 2px; padding-top:6px;'>📐 <b>Solar Azimuth:</b> ${sunAzText}</td>
            </tr>
        </table>

        <div style='font-size:0.85em; color:#81D4FA; background:#121216; padding:6px 10px; border-radius:4px; margin-top:6px;'>
            <b>Next Event:</b> ${nextEvent}
        </div>
    </div>
    """
    return html
}

// Single-Shot Version Demarcation Trace Logging Helper Routine
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    logTrace "updateAppLabel() -> Checking app label..."
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Advanced Porch Lighting Manager"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        logTrace "updateAppLabel() -> Updating app label from '${app.label}' to '${baseLabel}'"
        app.updateLabel(baseLabel)
    }
}

// Settings Hash Snapshot Helper
private String captureSettingsSnapshot() {
    logTrace "captureSettingsSnapshot() -> Generating settings MD5 hash..."
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn")) }
        .sort()

    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    String hash = java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
    logTrace "captureSettingsSnapshot() -> Hash resolved to: ${hash}"
    return hash
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
    logInfo "Updating app configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (state.appVersion != version())

    if (settingsChanged || codeVersionChanged) {
        logInfo "Settings or code version modification detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(false)
    } else {
        logDebug "App closed without setting or version changes. Skipping re-initialization."
    }
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling app..."
    unsubscribe()
    unschedule()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    logTrace "initialize() -> Re-establishing event subscriptions and cron anchors..."
    updateAppLabel()

    // Completely clear all lingering jobs before rebuilding schedule matrix
    unschedule()

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    }

    // Subscribe exclusively to Twilight Parser attributes to rebuild schedule timestamps
    if (twilightDevice) {
        logTrace "initialize() -> Subscribing to Twilight Parser attributes..."
        subscribe(twilightDevice, "usedTwilightBegin", buildAndScheduleEvents)
        subscribe(twilightDevice, "usedTwilightEnd", buildAndScheduleEvents)
        subscribe(twilightDevice, "localSunrise", buildAndScheduleEvents)
    }

    // Schedule Daily Midnight Schedule Construction
    schedule("0 2 0 * * ?", nightlyScheduleRefresh)
    logTrace "initialize() -> Anchored nightly refresh cron at 12:02 AM."

    // Initial Schedule Construction
    buildAndScheduleEvents(null)
}

def nightlyScheduleRefresh() {
    logInfo "Nightly schedule construction sequence initiated at 12:02 AM."
    logTrace "nightlyScheduleRefresh() -> Invoking buildAndScheduleEvents()..."
    buildAndScheduleEvents(null)
}

/* Helper to convert positive/negative user inputs into lead time minutes before twilight */
private int getNormalizedOffsetMins(def settingVal) {
    if (settingVal == null) return 15
    try {
        int raw = settingVal.toString().replace("-", "").trim().toInteger()
        logTrace "getNormalizedOffsetMins() -> Normalized input '${settingVal}' to ${raw} minutes."
        return raw
    } catch (Exception e) {
        logWarn "getNormalizedOffsetMins() -> Failed to parse offset '${settingVal}', defaulting to 15."
        return 15
    }
}

/* Master Schedule Construction Engine (Consumes Twilight Parser Timestamps) */
def buildAndScheduleEvents(evt = null) {
    logTrace "buildAndScheduleEvents() -> Triggered via event [${evt ? evt.name : 'manual/init'}]"
    if (!twilightDevice) {
        logWarn "buildAndScheduleEvents() -> Aborted: Twilight Parser device not selected."
        return
    }

    logDebug "Rebuilding lighting schedule from Twilight Parser timestamps..."
    unschedule("executeMorningOn")
    unschedule("executeMorningOff")
    unschedule("executeEveningOn")
    unschedule("executeNightOff")

    Date now = new Date()
    String upcomingEventDesc = "None scheduled"

    // 1. Morning Turn-ON Event Construction
    String mornTwilightStr = twilightDevice.currentValue("usedTwilightBegin")
    logTrace "buildAndScheduleEvents() -> Raw Morning Twilight Begin: ${mornTwilightStr}"
    if (mornTwilightStr) {
        Date mornTwilightDate = parseDateString(mornTwilightStr)
        if (mornTwilightDate) {
            int leadMins = getNormalizedOffsetMins(mornOffset)
            Date mornRunTime = new Date(mornTwilightDate.time - (leadMins * 60 * 1000))
            state.schedMornOnDisp = mornRunTime.format("h:mm a", location.timeZone)
            logTrace "buildAndScheduleEvents() -> Evaluated Morning ON RunTime: ${mornRunTime} (Now: ${now})"

            if (mornRunTime.after(now)) {
                runOnce(mornRunTime, executeMorningOn)
                logInfo "Scheduled Morning ON for: ${state.schedMornOnDisp} (${leadMins} mins before Twilight Begin)"
                if (upcomingEventDesc == "None scheduled") {
                    upcomingEventDesc = "Morning ON at ${state.schedMornOnDisp}"
                }
            } else {
                logTrace "buildAndScheduleEvents() -> Morning ON time has already passed for today."
            }
        }
    }

    // 2. Morning Turn-OFF Event Construction
    if (morningOffTime) {
        Date mornOffDate = parseIsoTimeToTodayDate(morningOffTime)
        logTrace "buildAndScheduleEvents() -> Explicit Morning Off Time parsed: ${mornOffDate}"
        if (mornOffDate) {
            state.schedMornOffDisp = mornOffDate.format("h:mm a", location.timeZone)
            if (mornOffDate.after(now)) {
                runOnce(mornOffDate, executeMorningOff)
                logInfo "Scheduled Morning Cutoff OFF for: ${state.schedMornOffDisp}"
                if (upcomingEventDesc == "None scheduled" && mornOffDate.after(now)) {
                    upcomingEventDesc = "Morning OFF at ${state.schedMornOffDisp}"
                }
            }
        }
    } else {
        String sunriseStr = twilightDevice.currentValue("localSunrise")
        logTrace "buildAndScheduleEvents() -> Fallback Sunrise time from Twilight Parser: ${sunriseStr}"
        if (sunriseStr) {
            Date sunriseDate = parseDateString(sunriseStr)
            if (sunriseDate) {
                state.schedMornOffDisp = sunriseDate.format("h:mm a", location.timeZone)
                if (sunriseDate.after(now)) {
                    runOnce(sunriseDate, executeMorningOff)
                    logInfo "Scheduled Sunrise Morning OFF for: ${state.schedMornOffDisp}"
                    if (upcomingEventDesc == "None scheduled") {
                        upcomingEventDesc = "Morning OFF at ${state.schedMornOffDisp}"
                    }
                }
            }
        }
    }

    // 3. Evening Turn-ON Event Construction
    String eveTwilightStr = twilightDevice.currentValue("usedTwilightEnd")
    logTrace "buildAndScheduleEvents() -> Raw Evening Twilight End: ${eveTwilightStr}"
    if (eveTwilightStr) {
        Date eveTwilightDate = parseDateString(eveTwilightStr)
        if (eveTwilightDate) {
            int leadMins = getNormalizedOffsetMins(eveOffset)
            Date eveRunTime = new Date(eveTwilightDate.time - (leadMins * 60 * 1000))
            state.schedEveOnDisp = eveRunTime.format("h:mm a", location.timeZone)
            logTrace "buildAndScheduleEvents() -> Evaluated Evening ON RunTime: ${eveRunTime} (Now: ${now})"

            if (eveRunTime.after(now)) {
                runOnce(eveRunTime, executeEveningOn)
                logInfo "Scheduled Evening ON for: ${state.schedEveOnDisp} (${leadMins} mins before Twilight End)"
                if (upcomingEventDesc == "None scheduled") {
                    upcomingEventDesc = "Evening ON at ${state.schedEveOnDisp}"
                }
            } else {
                logTrace "buildAndScheduleEvents() -> Evening ON time has already passed for today."
            }
        }
    }

    // 4. Evening Cutoff / Midnight Turn-OFF Event Construction
    if (eveningCutoffTime) {
        Date eveOffDate = parseIsoTimeToTodayDate(eveningCutoffTime)
        logTrace "buildAndScheduleEvents() -> Night Cutoff Time parsed: ${eveOffDate}"
        if (eveOffDate) {
            state.schedEveOffDisp = eveOffDate.format("h:mm a", location.timeZone)
            if (eveOffDate.after(now)) {
                runOnce(eveOffDate, executeNightOff)
                logInfo "Scheduled Night Cutoff OFF for: ${state.schedEveOffDisp}"
                if (upcomingEventDesc == "None scheduled" && eveOffDate.after(now)) {
                    upcomingEventDesc = "Evening OFF at ${state.schedEveOffDisp}"
                }
            }
        }
    }

    state.nextScheduledEventDesc = upcomingEventDesc
    logTrace "buildAndScheduleEvents() -> Construction complete. Next Event: ${upcomingEventDesc}"
}

/* Explicit Execution Handlers (Single Authority Points for Switch Commands) */
def executeMorningOn() {
    logTrace "executeMorningOn() -> Entering handler..."
    logInfo "Scheduled Event Reached: Morning Turn-ON"
    switches?.on()
    sendNotificationMsg("Advanced Porch Lighting: Morning lights turned ON.")
    state.nextScheduledEventDesc = "Morning OFF at ${state.schedMornOffDisp}"
}

def executeMorningOff() {
    logTrace "executeMorningOff() -> Entering handler..."
    logInfo "Scheduled Event Reached: Morning Turn-OFF"
    switches?.off()
    sendNotificationMsg("Advanced Porch Lighting: Morning lights turned OFF.")
    state.nextScheduledEventDesc = "Evening ON at ${state.schedEveOnDisp}"
}

def executeEveningOn() {
    logTrace "executeEveningOn() -> Entering handler..."
    logInfo "Scheduled Event Reached: Evening Turn-ON"
    switches?.on()
    sendNotificationMsg("Advanced Porch Lighting: Evening lights turned ON.")
    state.nextScheduledEventDesc = "Evening OFF at ${state.schedEveOffDisp}"
}

def executeNightOff() {
    logTrace "executeNightOff() -> Entering handler..."
    logInfo "Scheduled Event Reached: Night Cutoff Turn-OFF"
    switches?.off()
    sendNotificationMsg("Advanced Porch Lighting: Night cutoff lights turned OFF.")
    state.nextScheduledEventDesc = "Morning ON at ${state.schedMornOnDisp}"
}

private void sendNotificationMsg(String msg) {
    logTrace "sendNotificationMsg() -> Sending message: '${msg}'"
    if (notifier) {
        notifier.deviceNotification(msg)
    }
}

private Date parseDateString(String dateStr) {
    logTrace "parseDateString() -> Parsing '${dateStr}'"
    try {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.setTimeZone(location.timeZone)
        return sdf.parse(dateStr)
    } catch (Exception e) {
        logError "Failed to parse date string '${dateStr}': ${e.message}"
        return null
    }
}

private Date parseIsoTimeToTodayDate(String isoTime) {
    logTrace "parseIsoTimeToTodayDate() -> Parsing ISO time string '${isoTime}'"
    try {
        java.text.SimpleDateFormat inputSdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        Date parsed = inputSdf.parse(isoTime)
        
        Calendar targetCal = Calendar.getInstance(location.timeZone)
        Calendar sourceCal = Calendar.getInstance(location.timeZone)
        sourceCal.time = parsed

        targetCal.set(Calendar.HOUR_OF_DAY, sourceCal.get(Calendar.HOUR_OF_DAY))
        targetCal.set(Calendar.MINUTE, sourceCal.get(Calendar.MINUTE))
        targetCal.set(Calendar.SECOND, 0)
        targetCal.set(Calendar.MILLISECOND, 0)
        return targetCal.time
    } catch (Exception e) {
        logError "Failed to parse ISO time string '${isoTime}': ${e.message}"
        return null
    }
}

private String formatIsoTimeStr(String isoTime) {
    logTrace "formatIsoTimeStr() -> Formatting ISO string '${isoTime}'"
    Date d = parseIsoTimeToTodayDate(isoTime)
    return d ? d.format("h:mm a", location.timeZone) : isoTime
}

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
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