/**
 *  InfluxDB v2 Connection & Manager App
 *
 *  Copyright 2026 J. Shimota
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  ------------------------------------------------------------------------------------------------
 *  Version History:
 *  v1.0.0 - Original InfluxDB v2 Connection Tester release.
 *  v1.1.0 - Added Manager Mode (update interval scheduling, variable tracking, trend messaging, and notification tile connection).
 *  v1.1.1 - Added bracketed volume range indicators ([L], [N], [H]) to the output string header.
 *  v1.1.2 - Updated threshold brackets: [N] <= 100, [L] 101-1000, [H] 1001-2500, [E] > 2500.
 *  ------------------------------------------------------------------------------------------------
 */

private static String appVersion() { return "1.1.2" }

definition(
    name: "InfluxDB v2 Connection & Manager",
    namespace: "jshimota",
    author: "J. Shimota",
    description: "Validates InfluxDB v2 connections and manages automated status updates/notifications.",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    singleThreaded: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "InfluxDB v2 Manager & Tester (v${appVersion()})", install: true, uninstall: true) {
        
        section("<b>InfluxDB Server Details</b>") {
            input name: "prefServerUrl", type: "text", title: "Server Host / IP & Port", description: "e.g., http://192.168.1.12:8086", defaultValue: "http://192.168.1.12:8086", required: true
            input name: "prefOrg", type: "text", title: "Organization", description: "e.g., DundeeHome", required: true
            input name: "prefBucket", type: "text", title: "Bucket Name", description: "e.g., Hubitat", required: true
            input name: "prefToken", type: "password", title: "API Authentication Token", description: "InfluxDB v2 Token", required: true
        }
        
        section("<b>Connection Test Utility</b>") {
            input name: "btnTestConnection", type: "button", title: "Test InfluxDB Connection", submitOnChange: true
            
            if (state.testResult != null) {
                paragraph "${state.testResult}"
            }
        }

        section("<b>Manager Mode Configuration</b>") {
            input name: "prefEnableManager", type: "bool", title: "Enable Automated Manager Mode", defaultValue: false, submitOnChange: true
            
            if (prefEnableManager) {
                input name: "prefUpdateInterval", type: "enum", title: "Update Interval", options: ["1":"1 Minute", "5":"5 Minutes", "15":"15 Minutes", "30":"30 Minutes", "60":"1 Hour"], defaultValue: "15", required: true
                input name: "prefNotificationDevice", type: "capability.notification", title: "Target Notification Tile Device", required: true, multiple: false
                input name: "prefVarName", type: "text", title: "Watched Variable Name", description: "e.g., event_count", required: true
                input name: "prefSendTrendAlerts", type: "bool", title: "Append Trend Alert Messages (Increasing / Decreasing)", defaultValue: true
            }
        }
        
        section("<b>Logging & System Info</b>") {
            input name: "prefLogEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
            paragraph "<div style='text-align: right; color: #666; font-size: 0.85em;'>App Version: v${appVersion()}</div>"
        }
    }
}

def installed() {
    logDebug "Installed v${appVersion()} with settings: ${settings}"
    initialize()
}

def updated() {
    logDebug "Updated v${appVersion()} with settings: ${settings}"
    initialize()
}

def initialize() {
    unschedule()
    
    if (settings?.prefEnableManager) {
        def interval = settings?.prefUpdateInterval ?: "15"
        logDebug "Scheduling Manager updates every ${interval} minute(s)."
        
        switch (interval) {
            case "1":  runEvery1Minute("processManagerUpdate"); break
            case "5":  runEvery5Minutes("processManagerUpdate"); break
            case "15": runEvery15Minutes("processManagerUpdate"); break
            case "30": runEvery30Minutes("processManagerUpdate"); break
            case "60": runEvery1Hour("processManagerUpdate"); break
            default:   runEvery15Minutes("processManagerUpdate"); break
        }
        
        // Execute immediately upon saving settings
        processManagerUpdate()
    } else {
        logDebug "Manager Mode is disabled."
    }
}

def appButtonHandler(btn) {
    switch(btn) {
        case "btnTestConnection":
            testInfluxDbConnection()
            break
        default:
            log.warn "Unhandled button press: ${btn}"
            break
    }
}

def testInfluxDbConnection() {
    def baseUrl = settings?.prefServerUrl ? settings.prefServerUrl.replaceAll("/+\$", "") : ""
    def orgVal = settings?.prefOrg ?: ""
    def bucketVal = settings?.prefBucket ?: ""
    def tokenVal = settings?.prefToken ?: ""
    
    def testPath = "/api/v2/write"
    def fullTargetUrl = "${baseUrl}${testPath}?org=${orgVal}&bucket=${bucketVal}&precision=ns"
    
    log.info "[InfluxDB v${appVersion()}] Request URL: ${fullTargetUrl}"
    
    def testMetric = "hubitat_connection_test,hub=${location.name.replaceAll(' ', '\\\\ ')} status=\"online\" ${now()}000000"
    
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
                state.testResult = "<font color='green'><b>SUCCESS (HTTP ${response.status}):</b> Successfully connected and wrote test payload to bucket '${bucketVal}'.</font>"
                log.info "[InfluxDB v${appVersion()}] Result: Connection Successful (HTTP ${response.status})"
            } else {
                state.testResult = "<font color='orange'><b>WARNING (HTTP ${response.status}):</b> Connected, but server responded with status ${response.status}.</font>"
                log.info "[InfluxDB v${appVersion()}] Result: Unexpected HTTP status ${response.status}"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        def statusCode = e.response?.status
        def errorMessage = e.message
        
        state.testResult = "<font color='red'><b>FAILED (HTTP ${statusCode}):</b> ${errorMessage}. Check your token permissions or Org/Bucket names.</font>"
        log.info "[InfluxDB v${appVersion()}] Result: Failed - HTTP ${statusCode} (${errorMessage})"
    } catch (Exception e) {
        state.testResult = "<font color='red'><b>ERROR:</b> ${e.message}. Verify that the server IP/Port is reachable from Hubitat.</font>"
        log.info "[InfluxDB v${appVersion()}] Result: Error - ${e.message}"
    }
}

def processManagerUpdate() {
    def varName = settings?.prefVarName
    if (!varName) {
        log.warn "[InfluxDB v${appVersion()}] No variable name defined for tracking."
        return
    }
    
    // Retrieve Hubitat Global/Hub Variable
    def currentValRaw = getGlobalVar(varName)?.value
    if (currentValRaw == null) {
        log.warn "[InfluxDB v${appVersion()}] Variable '${varName}' was not found or has no value."
        return
    }
    
    def currentVal = currentValRaw.toString().isNumber() ? currentValRaw.toString().toDouble() : 0.0
    def previousVal = state.lastVarValue != null ? state.lastVarValue.toString().toDouble() : currentVal
    
    // Determine Volume Prefix Level based on updated count ranges
    String volumeTag = ""
    if (currentVal <= 100) {
        volumeTag = "[N] "
    } else if (currentVal >= 101 && currentVal <= 1000) {
        volumeTag = "[L] "
    } else if (currentVal >= 1001 && currentVal <= 2500) {
        volumeTag = "[H] "
    } else if (currentVal > 2500) {
        volumeTag = "[E] "
    }
    
    String trendMessage = ""
    if (settings?.prefSendTrendAlerts) {
        if (currentVal > previousVal) {
            trendMessage = " (Volume Increasing: ▲ +${currentVal - previousVal})"
        } else if (currentVal < previousVal) {
            trendMessage = " (Volume Decreasing: ▼ -${previousVal - currentVal})"
        } else {
            trendMessage = " (Volume Steady)"
        }
    }
    
    def outputString = "${volumeTag}${varName}: ${currentValRaw}${trendMessage}"
    logDebug "Manager Output: ${outputString}"
    
    // Send to Notification Tile Device
    if (settings?.prefNotificationDevice) {
        settings.prefNotificationDevice.deviceNotification(outputString)
    }
    
    // Cache count state for comparison on next run
    state.lastVarValue = currentVal
}

private void logDebug(msg) {
    if (settings?.prefLogEnable != false) {
        log.debug "[InfluxDB v${appVersion()}] ${msg}"
    }
}