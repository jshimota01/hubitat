/**
 * Echos Speak Advanced App
 * Platform: Hubitat Elevation
 * Category: Convenient Architecture
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
 *  Parent Application for managing EchosSpeak Advanced integration with local Node.js / Docker bridge running on Synology NAS.
 *
 *  Changelog:
 *  v1.1.5 (2026-09-09) - Applied Master App Template Alignment based on Mode Manager Advanced v1.1.73:
 *                        - Standardized App Title Banner, Active Status Card, Category Banners, and Collapsible Sections.
 *                        - Integrated single-shot version demarcation trace logging helper checkAndLogVersionDemarcation().
 *                        - Integrated captureSettingsSnapshot() to guard against unnecessary state re-evaluations.
 *                        - Standardized app label badging, centralized logging engine, and auto-debug disabler routine.
 *  v1.1.4 (2026-09-09) - Added bridge health verification and synchronized device state polling schedules.
 *  v1.1.0 (2026-09-08) - Initial deployment of Echos Speak Advanced Parent App architecture.
 **/

import groovy.transform.Field

static String version() { return '1.1.5' }
def timeStamp() { return "2026/09/09 12:30 PM" }

definition(
    name: "Echos Speak Advanced",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Connects Hubitat Elevation to local Synology NAS EchosSpeak Node.js API bridge for advanced Echo device control and automation.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/echos_speak_advanced/echos_speak_advanced.groovy"
)

preferences {
    page(name: "mainPage")
}

/* =========================================================================================
   CONFIGURATION PAGE LAYOUT
   ========================================================================================= */
def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        
        String currentVersion = version()

        /* App Title Banner & Active Status Card */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Echos Speak Advanced</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
            
            String bridgeStatus = state.bridgeConnected ? "<span style='color:#27AE60; font-weight:bold;'>Online</span>" : "<span style='color:#C0392B; font-weight:bold;'>Offline / Disconnected</span>"
            int activeChildCount = getChildDevices()?.size() ?: 0
            
            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #27AE60; padding:10px; border-radius:4px; font-size:13px;'>" +
                      "<b>Local Bridge Connection:</b> ${bridgeStatus} &nbsp;|&nbsp; " +
                      "<b>Active Managed Echo Devices:</b> <span style='color:#2980B9; font-weight:bold;'>${activeChildCount}</span></div>"
        }

        /* CATEGORY A: NETWORK & BRIDGE SETUP */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:10px;'>CATEGORY A: NETWORK & BRIDGE SETUP</div>") {}

        section("<b>SECTION 1: Synology NAS REST API Bridge Configuration</b>", hideable: true, hidden: false) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Bridge Network Setup:</b> Enter the local IP address and REST API port of your containerized Node.js bridge server.</div>"

            input name: "bridgeIp", type: "string", title: "<b>Bridge Server IP Address*</b>", defaultValue: "192.168.1.12", required: true, width: 6
            input name: "bridgePort", type: "number", title: "<b>REST API Port*</b>", defaultValue: 8093, required: true, width: 6

            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-top:6px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Amazon Auth Proxy Link:</b> If Amazon cookie authentication expires, open your browser to " +
                      "<a href='http://${settings.bridgeIp ?: "192.168.1.12"}:8094' target='_blank' style='color:#2980B9; font-weight:bold; text-decoration:underline;'>" +
                      "http://${settings.bridgeIp ?: "192.168.1.12"}:8094</a> to renew your session.</div>"
        }

        /* CATEGORY B: DEVICE DISCOVERY & FILTERING */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY B: DEVICE DISCOVERY & FILTERING</div>") {}

        section("<b>SECTION 2: Amazon Echo Device Discovery</b>", hideable: true, hidden: false) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Device Selection:</b> Select discovered Echo hardware devices from your Amazon account to create child devices in Hubitat.</div>"

            Map devMap = getDiscoveredDeviceMap()
            if (devMap) {
                input name: "selectedEchoes", type: "enum", title: "<b>Discovered Echo Devices</b>", options: devMap, multiple: true, required: false
            } else {
                paragraph "<div style='background-color:#FEF9E7; border-left:4px solid #F39C12; padding:8px; border-radius:4px; font-size:12px;'>" +
                          "⚠️ <b>No Devices Found:</b> Verify that <code>server.js</code> is running and authenticated with Amazon.</div>"
            }
        }

        section("<b>SECTION 3: Device Category Filtering Matrix (Categories 1-8)</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Category Filter Toggles:</b> Filter discovered devices by Amazon hardware category during discovery scans.</div>"

            input name: "catIncludeEchos", type: "bool", title: "<b>Category 1: Standard Echo & Dot Hardware</b>", defaultValue: true, width: 6
            input name: "catIncludeShows", type: "bool", title: "<b>Category 2: Echo Show & Screen Hardware</b>", defaultValue: true, width: 6
            input name: "catIncludeFireTV", type: "bool", title: "<b>Category 3: Fire TV & Media Streaming Devices</b>", defaultValue: true, width: 6
            input name: "catIncludeTablets", type: "bool", title: "<b>Category 4: Fire Tablets</b>", defaultValue: false, width: 6
            input name: "catIncludeAuto", type: "bool", title: "<b>Category 5: Echo Auto & Mobile Accessories</b>", defaultValue: false, width: 6
            input name: "catInclude3rdParty", type: "bool", title: "<b>Category 6: Third-Party Alexa Speakers (Sonos, etc.)</b>", defaultValue: false, width: 6
            input name: "catIncludeGroups", type: "bool", title: "<b>Category 7: Multi-Room Audio Speaker Groups</b>", defaultValue: false, width: 6
            input name: "catIncludeWholesite", type: "bool", title: "<b>Category 8: Everywhere / All-Device Synthetics</b>", defaultValue: false, width: 6
        }

        /* CATEGORY C: POLLING & REFRESH SCHEDULES */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY C: POLLING & REFRESH SCHEDULES</div>") {}

        section("<b>SECTION 4: Background Refresh & State Sync Engine</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Polling Schedules:</b> Configure background synchronization interval for pulling volume, media states, and voice activity.</div>"

            input name: "refreshInterval", type: "enum", title: "<b>Device State Polling Interval</b>", 
                  options: ["1": "Every Minute", "5": "Every 5 Minutes", "10": "Every 10 Minutes", "15": "Every 15 Minutes", "30": "Every 30 Minutes", "0": "Disabled"], 
                  defaultValue: "5", required: true
        }

        /* CATEGORY D: DIAGNOSTICS & SYSTEM OPTIONS */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY D: DIAGNOSTICS & APP PREFERENCES</div>") {}

        section("<b>SECTION 5: System Diagnostics & Logging Options</b>", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            input name: "showCountInLabel", type: "bool", title: "Show Active Device Count in App Label?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Detailed Trace Logging", defaultValue: false, required: true
        }

        section() {
            input name: "btnRefreshDevices", type: "button", title: "<b>Force Refresh Discovered Device Map</b>"
        }
    }
}

/* =========================================================================================
   APPLICATION LIFECYCLE & INFRASTRUCTURE SCHEDULER
   ========================================================================================= */

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

def installed() {
    checkAndLogVersionDemarcation()
    logTrace "=== Echos Speak Advanced [installed] Begins ==="
    logInfo "Installing app v${version()} (${timeStamp()})..."
    seedLoggingState()
    state.installedVersion = version()
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize(true)
    logTrace "=== Echos Speak Advanced [installed] Ends ==="
}

def uninstalled() {
    logTrace "=== Echos Speak Advanced [uninstalled] Begins ==="
    logInfo "Uninstalling Echos Speak Advanced app..."
    unschedule()
    logTrace "=== Echos Speak Advanced [uninstalled] Ends ==="
}

def updated() {
    checkAndLogVersionDemarcation()
    logTrace "=== Echos Speak Advanced [updated] Begins ==="
    logInfo "Updating app configuration..."

    String currentVersion = version()
    String previousVersion = state.installedVersion ?: "Unknown"

    if (previousVersion != currentVersion) {
        logWarn "🚀 APP VERSION CHANGED: Upgraded from v${previousVersion} -> v${currentVersion}"
        state.installedVersion = currentVersion
    }

    checkLoggingChanges()

    String currentSnapshot = captureSettingsSnapshot()
    String previousSnapshot = state.lastSettingsSnapshot
    Boolean settingsChanged = (previousSnapshot == null || previousSnapshot != currentSnapshot)

    if (settingsChanged) {
        state.lastSettingsSnapshot = currentSnapshot
        unschedule()
        initialize(false)
        syncChildDevices()
    } else {
        logInfo "App saved without configuration changes."
    }

    updateAppLabel()
    logTrace "=== Echos Speak Advanced [updated] Ends ==="
}

private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet().collect { it.toString() }.sort()
    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
}

private void seedLoggingState() {
    state.lastLogInfoEnable  = getSettingBool("logInfoEnable", true)
    state.lastLogDebugEnable = getSettingBool("logDebugEnable", false)
    state.lastLogTraceEnable = getSettingBool("logTraceEnable", false)
}

private void checkLoggingChanges() {
    Boolean currentInfo  = getSettingBool("logInfoEnable", true)
    Boolean currentDebug = getSettingBool("logDebugEnable", false)
    Boolean currentTrace = getSettingBool("logTraceEnable", false)

    if (currentInfo != state.lastLogInfoEnable)   logWarn "Info Logging changed to ${currentInfo ? 'ENABLED' : 'DISABLED'}"
    if (currentDebug != state.lastLogDebugEnable) logWarn "Debug Logging changed to ${currentDebug ? 'ENABLED' : 'DISABLED'}"
    if (currentTrace != state.lastLogTraceEnable) logWarn "Trace Logging changed to ${currentTrace ? 'ENABLED' : 'DISABLED'}"

    state.lastLogInfoEnable  = currentInfo
    state.lastLogDebugEnable = currentDebug
    state.lastLogTraceEnable = currentTrace
}

private void initialize(Boolean isInstall = false) {
    logTrace "=== Echos Speak Advanced [initialize] Begins ==="
    testBridgeConnection()

    int interval = settings.refreshInterval ? settings.refreshInterval.toInteger() : 5
    if (interval > 0) {
        schedule("0 */${interval} * ? * * *", "syncDeviceStates")
        logInfo "Scheduled device state background sync every ${interval} minutes."
    }

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        runIn(1800, "disableDebugLogging")
    }
    logTrace "=== Echos Speak Advanced [initialize] Ends ==="
}

/* =========================================================================================
   BRIDGE COMMUNICATIONS & CHILD DEVICE MANAGEMENT
   ========================================================================================= */

private Boolean testBridgeConnection() {
    String url = "http://${settings.bridgeIp}:${settings.bridgePort}/status"
    try {
        httpGet([uri: url, timeout: 5]) { resp ->
            if (resp.status == 200 && resp.data?.status == "online") {
                state.bridgeConnected = true
                logInfo "Local API Bridge connection verified."
                return true
            }
        }
    } catch (Exception e) {
        logError "Failed to connect to Local API Bridge at ${url}: ${e.message}"
    }
    state.bridgeConnected = false
    return false
}

private Map getDiscoveredDeviceMap() {
    String url = "http://${settings.bridgeIp}:${settings.bridgePort}/api/devices"
    Map devices = [:]
    try {
        httpGet([uri: url, timeout: 8]) { resp ->
            if (resp.status == 200 && resp.data?.devices) {
                state.rawDiscoveredDevices = resp.data.devices
                resp.data.devices.each { dev ->
                    devices[dev.serialNumber] = "${dev.accountName} (${dev.deviceFamily ?: 'Echo'})"
                }
            }
        }
    } catch (Exception e) {
        logError "Failed to fetch device map from bridge: ${e.message}"
    }
    return devices
}

private void syncChildDevices() {
    List selected = settings.selectedEchoes ?: []
    List currentChildren = getChildDevices()

    // Delete unselected child devices
    currentChildren.each { child ->
        if (!selected.contains(child.deviceNetworkId)) {
            logWarn "Removing unselected child device: ${child.displayName}"
            deleteChildDevice(child.deviceNetworkId)
        }
    }

    // Add new child devices
    selected.each { dni ->
        def existing = getChildDevice(dni)
        if (!existing) {
            Map devData = state.rawDiscoveredDevices?.find { it.serialNumber == dni }
            String devName = devData?.accountName ?: "Echo Device (${dni})"
            logInfo "Creating child device: ${devName} [${dni}]"
            addChildDevice("jshimota", "Echos Speak Advanced Device (Custom)", dni, [label: devName, isComponent: false])
        }
    }

    syncDeviceStates()
}

def syncDeviceStates() {
    if (!state.bridgeConnected && !testBridgeConnection()) return

    String url = "http://${settings.bridgeIp}:${settings.bridgePort}/api/devices"
    try {
        httpGet([uri: url, timeout: 8]) { resp ->
            if (resp.status == 200 && resp.data?.devices) {
                resp.data.devices.each { devData ->
                    def child = getChildDevice(devData.serialNumber)
                    if (child) {
                        child.parseDeviceData(devData)
                    }
                }
            }
        }
    } catch (Exception e) {
        logError "Error during device state sync: ${e.message}"
    }
}

void refreshDeviceData(String serialNumber) {
    syncDeviceStates()
}

void sendBridgeCommand(String serialNumber, String command, Map payload = [:]) {
    String url = "http://${settings.bridgeIp}:${settings.bridgePort}/api/command"
    Map body = [
        serialNumber: serialNumber,
        command: command,
        payload: payload
    ]

    try {
        httpPostJson([uri: url, body: body, timeout: 10]) { resp ->
            if (resp.status == 200) {
                logDebug "Command ${command} completed successfully for ${serialNumber}"
            } else {
                logError "Command failed for ${serialNumber} (${command}) [HTTP ${resp.status}]"
            }
        }
    } catch (Exception e) {
        logError "HTTP Post error sending command ${command} to ${serialNumber}: ${e.message}"
    }
}

/* =========================================================================================
   UI HELPERS & LOGGING ENGINE
   ========================================================================================= */

def appButtonHandler(btn) {
    if (btn == "btnRefreshDevices") {
        logInfo "Refreshing discovered device map from bridge..."
        getDiscoveredDeviceMap()
    }
}

private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    Boolean showCount   = getSettingBool("showCountInLabel", true)

    String baseLabel = "Echos Speak Advanced"
    if (showVersion) baseLabel += " v${version()}"

    if (showCount) {
        int count = getChildDevices()?.size() ?: 0
        baseLabel += " - [<span style='color:#27AE60; font-weight:bold;'>${count} Devices</span>]"
    }

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appName = app.label ?: 'Echos Speak Advanced'
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${appName}: ${msg}"
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