/**
 * Advanced Porch Lighting Manager
 * Platform: Hubitat Elevation
 * Notes: Dynamically controls porch and driveway lights based on Solar Elevation, Cloud Cover, Lux, or Twilight Parser attributes.
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
 *	v1.0.2    09/05/26    jshimota    Fixed notification device capability selector and split solar angles into morning/evening thresholds with independent cloud cover shifts.
 *	v1.0.1    09/05/26    jshimota    Added live UI status dashboard and updated version tracking.
 *	v1.0.0    09/05/26    jshimota    Initial release built on standardized App Template v1.2.2.
 **/

static String version() { return '1.0.2' }
def timeStamp() { return "2026/09/05 11:40 PM" }

definition(
    name: "Advanced Porch Lighting Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Automates porch and outdoor lights using Solar Angles, Cloud Cover, Calculated Lux, and Twilight Parser variables.",
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
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Advanced Porch Lighting Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        /* Live Status Dashboard */
        if (owmDevice) {
            section("<h3 style='color:#1A73E8; margin-bottom:0;'>📊 Current Environment & Schedule Status</h3>") {
                paragraph getStatusBoardHtml()
            }
        }

        section("<b>Main App Configuration</b>") {
            input name: "switches", type: "capability.switch", title: "Select Switches to Control", multiple: true, required: true
            input name: "owmDevice", type: "capability.sensor", title: "Select OpenWeatherMap Device", multiple: false, required: true, submitOnChange: true
            input name: "twilightDevice", type: "capability.sensor", title: "Select Twilight Parser Device (Optional)", multiple: false, required: false, submitOnChange: true
            input name: "notifier", type: "capability.notification", title: "Select Notification Device (Optional)", multiple: true, required: false
        }

        section("<b>Trigger Evaluation Method</b>") {
            input name: "controlMethod", type: "enum", title: "Light Trigger Evaluation Method", 
                  options: [
                      "lux": "Dynamic Lux Threshold (Built-in OWM Lux with Cloud Attenuation)",
                      "angle": "Solar Elevation Angle + Cloud PCT Offset",
                      "twilight": "Twilight Parser Variable Time + Minute Offset"
                  ], 
                  defaultValue: "lux", required: true, submitOnChange: true
        }

        if (controlMethod == "lux") {
            section("<b>Lux Threshold Settings</b>") {
                input name: "luxOnThreshold", type: "number", title: "Turn ON Lights when Lux is at or below", defaultValue: 50, required: true, submitOnChange: true
                input name: "luxOffThreshold", type: "number", title: "Turn OFF Lights when Lux rises above", defaultValue: 100, required: true, submitOnChange: true
            }
        } else if (controlMethod == "angle") {
            section("<b>Morning Solar Altitude Angle Settings</b>") {
                input name: "mornOnAngle", type: "decimal", title: "Morning Light ON Angle (Sun elevation below horizon, e.g. -3.5°)", defaultValue: -3.5, required: true, submitOnChange: true
                input name: "enableMornCloudOffset", type: "bool", title: "Adjust morning angle earlier on heavy overcast days?", defaultValue: true, submitOnChange: true
                if (enableMornCloudOffset) {
                    input name: "maxMornCloudShift", type: "decimal", title: "Max Morning Angle Offset for 100% Cloud Cover (Degrees)", defaultValue: 2.5, required: true, submitOnChange: true
                }
            }
            section("<b>Evening Solar Altitude Angle Settings</b>") {
                input name: "eveOnAngle", type: "decimal", title: "Evening Light ON Angle (Sun elevation below horizon, e.g. -3.5°)", defaultValue: -3.5, required: true, submitOnChange: true
                input name: "enableEveCloudOffset", type: "bool", title: "Adjust evening angle earlier on heavy overcast days?", defaultValue: true, submitOnChange: true
                if (enableEveCloudOffset) {
                    input name: "maxEveCloudShift", type: "decimal", title: "Max Evening Angle Offset for 100% Cloud Cover (Degrees)", defaultValue: 2.5, required: true, submitOnChange: true
                }
            }
        } else if (controlMethod == "twilight") {
            section("<b>Twilight Variable Settings</b>") {
                input name: "mornOffset", type: "number", title: "Morning Offset from Twilight Begin (Minutes)", defaultValue: -15, required: true, submitOnChange: true
                input name: "eveOffset", type: "number", title: "Evening Offset from Twilight End (Minutes)", defaultValue: -75, required: true, submitOnChange: true
            }
        }

        section("<b>Schedule & Cutoff Rules</b>") {
            input name: "morningOnTime", type: "time", title: "Earliest Morning Turn-ON Time", required: false
            input name: "morningOffTime", type: "time", title: "Latest Morning Turn-OFF Time (e.g. 8:00 AM)", required: false
            input name: "eveningCutoffTime", type: "time", title: "Night Turn-OFF Time / Midnight Cutoff", defaultValue: "23:59", required: true, submitOnChange: true
        }

        /* Collapsible App Preferences & Logging Options */
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

/* Status Dashboard HTML Generator */
String getStatusBoardHtml() {
    BigDecimal currentLux   = owmDevice.currentValue("currentIlluminance")?.toBigDecimal() ?: 0.0[cite: 1]
    BigDecimal currentAngle = owmDevice.currentValue("currentSunAltitude")?.toBigDecimal() ?: 0.0[cite: 1]
    BigDecimal currentClouds= owmDevice.currentValue("currentCloudPCT")?.toBigDecimal() ?: 0.0[cite: 1]
    
    String mornStartText = twilightDevice ? (twilightDevice.currentValue("formattedUsedTwilightBegin") ?: "--") : (owmDevice.currentValue("currentTwilightBeginTimeText") ?: "--")[cite: 1, 2]
    String eveStartText  = twilightDevice ? (twilightDevice.currentValue("formattedUsedTwilightEnd") ?: "--") : (owmDevice.currentValue("currentTwilightEndTimeText") ?: "--")[cite: 1, 2]
    
    String switchState = switches ? switches[0].currentValue("switch") : "unknown"
    String badgeColor  = (switchState == "on") ? "#2e7d32" : "#757575"
    String badgeText   = (switchState == "on") ? "LIGHTS ON 💡" : "LIGHTS OFF 🌙"

    String mornTargetStr = "--"
    String eveTargetStr  = "--"

    if (controlMethod == "angle") {
        boolean isMorning = isMorningPeriod()
        
        BigDecimal baseMorn = (mornOnAngle != null) ? mornOnAngle.toBigDecimal() : -3.5
        if (enableMornCloudOffset && currentClouds > 0) {
            BigDecimal shift = (maxMornCloudShift != null) ? maxMornCloudShift.toBigDecimal() : 2.5
            baseMorn = baseMorn + ((currentClouds / 100.0) * shift)
        }
        mornTargetStr = "${baseMorn.setScale(1, java.math.RoundingMode.HALF_UP)}°"

        BigDecimal baseEve = (eveOnAngle != null) ? eveOnAngle.toBigDecimal() : -3.5
        if (enableEveCloudOffset && currentClouds > 0) {
            BigDecimal shift = (maxEveCloudShift != null) ? maxEveCloudShift.toBigDecimal() : 2.5
            baseEve = baseEve + ((currentClouds / 100.0) * shift)
        }
        eveTargetStr = "${baseEve.setScale(1, java.math.RoundingMode.HALF_UP)}°"
    }

    String html = """
    <div style='background-color:#1e1e24; color:#ffffff; padding:12px; border-radius:8px; font-family:sans-serif;'>
        <div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;'>
            <span style='font-weight:bold; font-size:1.1em;'>Real-Time Environment</span>
            <span style='background-color:${badgeColor}; color:#fff; padding:3px 8px; border-radius:4px; font-weight:bold; font-size:0.85em;'>${badgeText}</span>
        </div>
        
        <table style='width:100%; color:#ddd; font-size:0.9em; border-collapse:collapse;'>
            <tr style='border-bottom:1px solid #333;'>
                <td style='padding:4px;'>☀️ <b>Sun Altitude:</b> ${currentAngle}°</td>
                <td style='padding:4px;'>💡 <b>Current Lux:</b> ${currentLux} lx</td>
            </tr>
            <tr style='border-bottom:1px solid #333;'>
                <td style='padding:4px;'>☁️ <b>Cloud Cover:</b> ${currentClouds}%</td>
                <td style='padding:4px;'>🎯 <b>Target Angle:</b> Morn: ${mornTargetStr} | Eve: ${eveTargetStr}</td>
            </tr>
            <tr>
                <td style='padding:4px; padding-top:8px;'>🌅 <b>Morning Twilight:</b> ${mornStartText}</td>
                <td style='padding:4px; padding-top:8px;'>🌆 <b>Evening Twilight:</b> ${eveStartText}</td>
            </tr>
        </table>
    </div>
    """
    return html
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Advanced Porch Lighting Manager"
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

    // Subscribe to OpenWeatherMap Driver Events
    if (owmDevice) {
        subscribe(owmDevice, "currentSunAltitude", evaluateLighting)
        subscribe(owmDevice, "currentIlluminance", evaluateLighting)
        subscribe(owmDevice, "currentCloudPCT", evaluateLighting)
    }

    // Subscribe to Twilight Parser Driver Events
    if (twilightDevice) {
        subscribe(twilightDevice, "formattedUsedTwilightBegin", scheduleTwilightEvents)
        subscribe(twilightDevice, "formattedUsedTwilightEnd", scheduleTwilightEvents)
    }

    // Schedule Cutoff Time
    if (eveningCutoffTime) {
        schedule(eveningCutoffTime, turnOffLightsNight)
    }

    // Initial Lighting Evaluation
    evaluateLighting(null)
}

/* Centralized Lighting Evaluation Handler */
def evaluateLighting(evt = null) {
    if (!owmDevice || !switches) return

    logDebug "Evaluating lighting conditions via method: ${controlMethod}..."

    BigDecimal sunAngle    = owmDevice.currentValue("currentSunAltitude")?.toBigDecimal() ?: 0.0[cite: 1]
    BigDecimal cloudPct    = owmDevice.currentValue("currentCloudPCT")?.toBigDecimal() ?: 0.0[cite: 1]
    BigDecimal currentLux  = owmDevice.currentValue("currentIlluminance")?.toBigDecimal() ?: 0.0[cite: 1]
    String currentSwitchState = switches[0]?.currentValue("switch") ?: "off"

    boolean shouldBeOn = false

    switch (controlMethod) {
        case "lux":
            BigDecimal onLux  = (luxOnThreshold != null) ? luxOnThreshold.toBigDecimal() : 50.0
            BigDecimal offLux = (luxOffThreshold != null) ? luxOffThreshold.toBigDecimal() : 100.0
            
            if (currentSwitchState == "off") {
                shouldBeOn = (currentLux <= onLux)
            } else {
                shouldBeOn = (currentLux < offLux)
            }
            logDebug "Lux Check: Current=${currentLux} lx | Target On<=${onLux} lx | Target Off>=${offLux} lx | ShouldBeOn=${shouldBeOn}"
            break

        case "angle":
            boolean isMorning = isMorningPeriod()
            BigDecimal baseAngle = isMorning ? ((mornOnAngle != null) ? mornOnAngle.toBigDecimal() : -3.5) : ((eveOnAngle != null) ? eveOnAngle.toBigDecimal() : -3.5)
            boolean cloudOffsetEnabled = isMorning ? (enableMornCloudOffset == true) : (enableEveCloudOffset == true)
            BigDecimal cloudShift = isMorning ? ((maxMornCloudShift != null) ? maxMornCloudShift.toBigDecimal() : 2.5) : ((maxEveCloudShift != null) ? maxEveCloudShift.toBigDecimal() : 2.5)

            BigDecimal targetAngle = baseAngle
            if (cloudOffsetEnabled && cloudPct > 0) {
                targetAngle = targetAngle + ((cloudPct / 100.0) * cloudShift)
            }
            shouldBeOn = (sunAngle <= targetAngle)
            logDebug "Angle Check (${isMorning ? 'Morning' : 'Evening'}): Sun=${sunAngle}° | Target=${targetAngle}° (Base: ${baseAngle}°, Clouds: ${cloudPct}%) | ShouldBeOn=${shouldBeOn}"
            break

        case "twilight":
            // Managed via scheduleTwilightEvents()
            return
    }

    // Execute Switch State Changes
    if (shouldBeOn && currentSwitchState == "off") {
        if (isWithinAllowedTimeWindow()) {
            logInfo "Turning switches ON based on ${controlMethod} trigger."
            switches.on()
            sendNotificationMsg("Advanced Porch Lighting: Lights turned ON (${controlMethod} trigger).")
        }
    } else if (!shouldBeOn && currentSwitchState == "on") {
        logInfo "Turning switches OFF based on ${controlMethod} trigger."
        switches.off()
        sendNotificationMsg("Advanced Porch Lighting: Lights turned OFF (${controlMethod} trigger).")
    }
}

/* Twilight Parser Schedule Handler */
def scheduleTwilightEvents(evt = null) {
    if (controlMethod != "twilight" || !twilightDevice) return

    logDebug "Recalculating Twilight Parser scheduled triggers..."
    unschedule("turnOnMorningTwilight")
    unschedule("turnOffEveningTwilight")

    String mornTimeStr = twilightDevice.currentValue("usedTwilightBegin")[cite: 2]
    String eveTimeStr  = twilightDevice.currentValue("usedTwilightEnd")[cite: 2]

    if (mornTimeStr) {
        Date mornDate = parseDateString(mornTimeStr)
        if (mornDate) {
            int offsetMins = (mornOffset != null) ? mornOffset.toInteger() : -15
            Date runTime = new Date(mornDate.time + (offsetMins * 60 * 1000))
            runOnce(runTime, turnOnMorningTwilight)
            logInfo "Scheduled Morning Twilight ON for: ${runTime}"
        }
    }

    if (eveTimeStr) {
        Date eveDate = parseDateString(eveTimeStr)
        if (eveDate) {
            int offsetMins = (eveOffset != null) ? eveOffset.toInteger() : -75
            Date runTime = new Date(eveDate.time + (offsetMins * 60 * 1000))
            runOnce(runTime, turnOnEveningTwilight)
            logInfo "Scheduled Evening Twilight ON for: ${runTime}"
        }
    }
}

def turnOnMorningTwilight() {
    logInfo "Morning Twilight trigger reached: Turning lights ON."
    switches.on()
    sendNotificationMsg("Advanced Porch Lighting: Morning lights turned ON.")
}

def turnOnEveningTwilight() {
    logInfo "Evening Twilight trigger reached: Turning lights ON."
    switches.on()
    sendNotificationMsg("Advanced Porch Lighting: Evening lights turned ON.")
}

def turnOffLightsNight() {
    logInfo "Cutoff/Midnight time reached: Turning switches OFF."
    switches.off()
    sendNotificationMsg("Advanced Porch Lighting: Lights turned OFF due to night cutoff.")
}

private boolean isMorningPeriod() {
    Calendar now = Calendar.getInstance()
    int hour = now.get(Calendar.HOUR_OF_DAY)
    return (hour < 12)
}

private boolean isWithinAllowedTimeWindow() {
    Calendar now = Calendar.getInstance()
    int hour = now.get(Calendar.HOUR_OF_DAY)
    return (hour < 9 || hour >= 16)
}

private void sendNotificationMsg(String msg) {
    if (notifier) {
        notifier.deviceNotification(msg)
    }
}

private Date parseDateString(String dateStr) {
    try {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.setTimeZone(location.timeZone)
        return sdf.parse(dateStr)
    } catch (Exception e) {
        logError "Failed to parse date string '${dateStr}': ${e.message}"
        return null
    }
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