/**
 * InfluxDB Backlog & Health Monitor App
 * Platform: Hubitat Elevation
 * Notes: Validates InfluxDB v2 connections, tracks hub event backlog variables, and pushes dashboard notifications
 * Category: Utility
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
 *  Validates InfluxDB v2 HTTP endpoint connections and manages automated backlog status updates, variable tracking,
 *  dynamic schedule boosting during queue spikes, and dashboard notification tile pushes.
 *
 *  Instructions:
 *  1. Configure your InfluxDB v2 Server URL, Organization, Bucket, and API Authentication Token.
 *  2. Enable Automated Manager Mode to monitor a Hub Variable (e.g., event_count backlog) and send tile updates.
 *  3. Use the Connection Test Validator to verify live HTTP writes to your InfluxDB instance.
 *  
 *  Changelog:
 *  v1.3.1    08/30/26    jshimota    Added non-numeric variable validation guard; modernized HTML font tags to CSS span; extracted getVolumeTag helper
 *  v1.3.0    08/30/26    jshimota    Renamed to InfluxDB Backlog & Health Monitor; applied App Master Template v1.2.1 (styled banner, badging, collapsible prefs, settings hash + code version re-init, standardized logging engine)
 *  v1.2.1    08/28/26    jshimota    Added 'Send Test Values To Tile' button in Section 3 to test 0, 150, 1500, and 2750 counts
 *  v1.2.0    08/28/26    jshimota    Removed HTML font formatting tags to allow native tile driver parsing
 *  v1.1.9    08/28/26    jshimota    Restored count and trend arrows; prepended formatted time; set label to 'Influx Backlog: '
 *  v1.1.8    08/28/26    jshimota    Changed output string display name to 'Backlog Counter' preceding the formatted time
 *  v1.1.7    08/28/26    jshimota    Updated output string to display formatted time
 *  v1.1.6    08/28/26    jshimota    Bug fixes: safe getGlobalVar handling, float delta rounding, deferred initialization execution, and auto-log disabler
 *  v1.1.5    08/28/26    jshimota    Renamed sections (Section 1, Section 2, Section 3, Logging) and inserted clean horizontal rule separators
 *  v1.1.4    08/28/26    jshimota    Added dynamic schedule adjustment: automatically increases update frequency to 1 min when counter climbs
 *  v1.1.3    08/28/26    jshimota    Reordered UI sections (Tester moved to bottom) and added 'Trigger Now' button for immediate output
 *  v1.1.2    08/28/26    jshimota    Updated threshold brackets: [N] <= 100, [L] 101-1000, [H] 1001-2500, [E] > 2500
 *  v1.1.1    08/28/26    jshimota    Added bracketed volume range indicators ([L], [N], [H]) to the output string header
 *  v1.1.0    08/28/26    jshimota    Added Manager Mode (update interval scheduling, variable tracking, trend messaging, and notification tile connection)
 *  v1.0.0    01/01/26    jshimota    Original InfluxDB v2 Connection Tester release
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.3.1' }
def timeStamp() { return "2026/08/30 11:55 AM" }

definition(
    name: "InfluxDB Backlog & Health Monitor",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Validates InfluxDB v2 connections and manages automated backlog status updates and tile notifications",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/influxdb_backlog_health_monitor/influx_backlog_health_monitor_v1.3.1.groovy",
    singleThreaded: true
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
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>InfluxDB Backlog & Health Monitor</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }
        
        section("<b>Section 1. Server Details for InfluxDB v2</b>") {
            input name: "prefServerUrl", type: "text", title: "Server Host / IP & Port", description: "e.g., http://192.168.1.12:8086", defaultValue: "http://192.168.1.12:8086", required: true
            input name: "prefOrg", type: "text", title: "Organization", description: "e.g., DundeeHome", required: true
            input name: "prefBucket", type: "text", title: "Bucket Name", description: "e.g., Hubitat", required: true
            input name: "prefToken", type: "password", title: "API Authentication Token", description: "InfluxDB v2 Token", required: true
        }

        section("<b>Section 2. Notification Manager</b>") {
            input name: "prefEnableManager", type: "bool", title: "Enable Automated Manager Mode", defaultValue: false, submitOnChange: true
            
            if (getSettingBool("prefEnableManager", false)) {
                input name: "prefUpdateInterval", type: "enum", title: "Baseline Update Interval", options: ["1":"1 Minute", "5":"5 Minutes", "15":"15 Minutes", "30":"30 Minutes", "60":"1 Hour"], defaultValue: "15", required: true
                input name: "prefNotificationDevice", type: "capability.notification", title: "Target Notification Tile Device", required: true, multiple: false
                input name: "prefVarName", type: "text", title: "Watched Variable Name", description: "e.g., event_count", required: true
                input name: "btnTriggerNow", type: "button", title: "Trigger Now", submitOnChange: true
            }
        }

        section("<b>Section 3. Connection Test Validator</b>") {
            input name: "btnTestConnection", type: "button", title: "Test InfluxDB Connection", submitOnChange: true
            input name: "btnSendTestTileValues", type: "button", title: "Send Test Values To Tile", submitOnChange: true
            
            if (state.testResult != null) {
                paragraph "${state.testResult}"
            }
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

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "InfluxDB Backlog & Health Monitor"
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

// Helper Routine to Evaluate Volume Threshold Brackets
private String getVolumeTag(Double countVal) {
    if (countVal == null) return "[N] "
    if (countVal <= 100)       return "[N] "
    if (countVal <= 1000)      return "[L] "
    if (countVal <= 2500)      return "[H] "
    return "[E] "
}

// Hubitat UI Button Click Handler
def appButtonHandler(btn) {
    switch(btn) {
        case "btnTestConnection":
            testInfluxDbConnection()
            break
        case "btnTriggerNow":
            logInfo "Manual update triggered via UI button."
            processManagerUpdate()
            break
        case "btnSendTestTileValues":
            logInfo "Pushing test volume levels to tile device via UI button."
            sendTestValuesToTile()
            break
        default:
            logWarn "Unhandled button press: ${btn}"
            break
    }
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
    Boolean codeVersionChanged = (state.lastInitializedVersion != version())

    if (settingsChanged || codeVersionChanged) {
        logInfo "Settings or code version modification detected. Re-establishing schedules..."
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
    logInfo "Uninstalling InfluxDB Backlog & Health Monitor app..."
    unsubscribe()
    unschedule()
}

private void initialize(Boolean isInstall = false) {
    state.lastInitializedVersion = version()
    state.activeSchedule = null
    
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

    applySchedule(settings?.prefUpdateInterval ?: "15")
    
    if (getSettingBool("prefEnableManager", false)) {
        runIn(1, "processManagerUpdate")
    } else {
        logDebug "Automated Manager Mode is disabled."
    }
    updateAppLabel()
}

private void applySchedule(String intervalMinutes) {
    if (!getSettingBool("prefEnableManager", false)) {
        unschedule("processManagerUpdate")
        state.activeSchedule = null
        return
    }

    if (state.activeSchedule == intervalMinutes) {
        return
    }

    unschedule("processManagerUpdate")
    logDebug "Setting schedule to execute processManagerUpdate every ${intervalMinutes} minute(s)."
    
    switch (intervalMinutes) {
        case "1":  runEvery1Minute("processManagerUpdate"); break
        case "5":  runEvery5Minutes("processManagerUpdate"); break
        case "15": runEvery15Minutes("processManagerUpdate"); break
        case "30": runEvery30Minutes("processManagerUpdate"); break
        case "60": runEvery1Hour("processManagerUpdate"); break
        default:   runEvery15Minutes("processManagerUpdate"); break
    }
    
    state.activeSchedule = intervalMinutes
}

def testInfluxDbConnection() {
    def baseUrl = settings?.prefServerUrl ? settings.prefServerUrl.replaceAll("/+\$", "") : ""
    def orgVal = settings?.prefOrg ?: ""
    def bucketVal = settings?.prefBucket ?: ""
    def tokenVal = settings?.prefToken ?: ""
    
    def testPath = "/api/v2/write"
    def fullTargetUrl = "${baseUrl}${testPath}?org=${orgVal}&bucket=${bucketVal}&precision=ns"
    
    logInfo "Request URL: ${fullTargetUrl}"
    
    def hubName = location?.name ? location.name.replaceAll(' ', '\\\\ ') : "Hubitat"
    def testMetric = "hubitat_connection_test,hub=${hubName} status=\"online\" ${now()}000000"
    
    def postParams = [
        uri: baseUrl,
        path: testPath,
        query: [
            org: orgVal,
            bucket: bucketVal,
            precision: "ns"
        ],
        headers: [
            "Authorization": "Token ${tokenVal}",
            "Content-Type": "text/plain; charset=utf-8"
        ],
        body: testMetric,
        timeout: 10
    ]
    
    try {
        httpPost(postParams) { response ->
            if (response.status == 204 || response.status == 200) {
                state.testResult = "<span style='color:green; font-weight:bold;'>SUCCESS (HTTP ${response.status}):</span> Successfully connected and wrote test payload to bucket '${bucketVal}'."
                logInfo "Result: Connection Successful (HTTP ${response.status})"
            } else {
                state.testResult = "<span style='color:orange; font-weight:bold;'>WARNING (HTTP ${response.status}):</span> Connected, but server responded with status ${response.status}."
                logWarn "Result: Unexpected HTTP status ${response.status}"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        def statusCode = e.response?.status
        def errorMessage = e.message
        
        state.testResult = "<span style='color:red; font-weight:bold;'>FAILED (HTTP ${statusCode}):</span> ${errorMessage}. Check token permissions or Org/Bucket names."
        logError "Result: Failed - HTTP ${statusCode} (${errorMessage})"
    } catch (Exception e) {
        state.testResult = "<span style='color:red; font-weight:bold;'>ERROR:</span> ${e.message}. Verify that the server IP/Port is reachable from Hubitat."
        logError "Result: Error - ${e.message}"
    }
}

def sendTestValuesToTile() {
    if (!settings?.prefNotificationDevice) {
        state.testResult = "<span style='color:red; font-weight:bold;'>ERROR:</span> No Target Notification Tile Device selected in Section 2."
        logWarn "sendTestValuesToTile aborted: Target Notification Device is null."
        return
    }

    def testCounts = [0.0, 150.0, 1500.0, 2750.0]
    def nowMs = now()
    def timeZone = location?.timeZone ?: TimeZone.getDefault()
    def formattedTime = new java.text.SimpleDateFormat("h:mma", Locale.US)
    formattedTime.setTimeZone(timeZone)
    def cleanTimeStr = formattedTime.format(new Date(nowMs)).toLowerCase().replaceAll("m\$", "")
    
    testCounts.each { testVal ->
        String volumeTag = getVolumeTag(testVal)
        def testOutputString = "${cleanTimeStr} ${volumeTag}Influx Backlog: ${testVal.toInteger()} steady"
        logDebug "Sending Test Output: ${testOutputString}"
        settings.prefNotificationDevice.deviceNotification(testOutputString)
    }

    state.testResult = "<span style='color:green; font-weight:bold;'>TEST SENT:</span> Successfully pushed 0 [N], 150 [L], 1500 [H], and 2750 [E] test strings to tile device."
}

def processManagerUpdate() {
    def varName = settings?.prefVarName
    if (!varName) {
        logWarn "No variable name defined for tracking."
        return
    }
    
    def currentValRaw = null
    try {
        currentValRaw = getGlobalVar(varName)?.value
    } catch (Exception e) {
        logWarn "Could not read hub variable '${varName}': ${e.message}"
        return
    }

    if (currentValRaw == null) {
        logWarn "Variable '${varName}' was not found or has no value."
        return
    }

    // Safety Guard: Abort update if watched variable contains non-numeric string data
    if (!currentValRaw.toString().isNumber()) {
        logWarn "Watched variable '${varName}' contains non-numeric value: '${currentValRaw}'. Aborting update."
        return
    }
    
    def currentVal = currentValRaw.toString().toDouble()
    def previousVal = state.lastVarValue != null ? state.lastVarValue.toString().toDouble() : currentVal
    
    // Dynamic scheduling logic based on trend
    if (currentVal > previousVal) {
        logDebug "Counter is climbing (${previousVal} -> ${currentVal}). Boosting update frequency to 1 minute."
        applySchedule("1")
    } else {
        def targetInterval = settings?.prefUpdateInterval ?: "15"
        if (state.activeSchedule != targetInterval) {
            logDebug "Counter has stabilized/decreased (${previousVal} -> ${currentVal}). Reverting schedule to baseline (${targetInterval}m)."
            applySchedule(targetInterval)
        }
    }

    // Formatted time (e.g., 9:33a)
    def nowMs = now()
    def timeZone = location?.timeZone ?: TimeZone.getDefault()
    def formattedTime = new java.text.SimpleDateFormat("h:mma", Locale.US)
    formattedTime.setTimeZone(timeZone)
    def cleanTimeStr = formattedTime.format(new Date(nowMs)).toLowerCase().replaceAll("m\$", "")

    // Bracket Volume Tag via Helper
    String volumeTag = getVolumeTag(currentVal)

    // Plain text trend messaging
    String trendMessage = ""
    if (currentVal > previousVal) {
        def diff = (currentVal - previousVal).round(2)
        trendMessage = " ▲ +${diff}"
    } else if (currentVal < previousVal) {
        def diff = (previousVal - currentVal).round(2)
        trendMessage = " ▼ -${diff}"
    } else {
        trendMessage = " steady"
    }
    
    // Output format: 9:33a [N] Influx Backlog: 45 steady
    def outputString = "${cleanTimeStr} ${volumeTag}Influx Backlog: ${currentValRaw}${trendMessage}"
    logDebug "Manager Output: ${outputString}"
    
    // Send to Notification Tile Device
    if (settings?.prefNotificationDevice) {
        settings.prefNotificationDevice.deviceNotification(outputString)
    }
    
    // Cache count state for comparison on next run
    state.lastVarValue = currentVal
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