/*
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
 *  v2.1.0 (2026-09-21) - Direct Playback Track Artwork Bridge Integration:
 *                          - Updated fetchDeviceMediaState() response mapping pipeline to consume server normalized trackImage and trackImageHtml fields from getPlaybackState().
 *                          - Preserved historical changelog entries and master sync schedules.
 *  v2.0.0 (2026-09-21) - Major Architecture Version Release & Driver Naming Standardization:
 *                          - Promoted major version baseline to v2.0.0 across Parent and Child drivers following completion of core validation suite.
 *                          - Renamed child driver targets to 'Echos Speak Advanced Standard Device' and 'Echos Speak Advanced W/Sensors Device'.
 *                          - Retained complete historical changelog entries without deletion or consolidation.
 *  v1.8.14 (2026-09-21) - Architectural Driver-Selection Refactor:
 *                          - Expanded parent driver selection logic to evaluate Temperature, Motion, AND Display capabilities.
 *                          - Routes devices exposing Temperature, Motion, OR Display capabilities to the extended Sensors driver.
 *                          - Preserved raw server capability contract and historical changelog entries.
 *  v1.8.13 (2026-09-20) - Decoupled Hubitat Device Label Persistence from Amazon Endpoint Sync:
 *                          - Removed child.setLabel() overwrites from periodic syncDeviceStates() and targeted refreshDeviceData().
 *                          - Preserved initial label assignment during child creation via addChildDevice devProps map.
 *                          - Retained amazonDeviceName attribute updates via parseDeviceData() while allowing local Hubitat labels to remain user-managed and persistent.
 *  v1.8.12 (2026-09-20) - Masking Log Escalation Adjustments:
 *                          - Lowered routine Amazon bug-endpoint masking entries ('EverywhereTwo' & 'Office Test') from logWarn to logTrace during background polling.
 *                          - Added isExplicitRefresh parameter to getRawAmazonEndpointMap() to log masking entries as logWarn strictly during Force Device Refresh operations.
 *  v1.8.11 (2026-09-20) - Targeted GUI Refresh Fix for REST Bridge Network Test:
 *                          - Refactored btnTestBridge in appButtonHandler() to execute synchronously before returning mainPage().
 *                          - Guaranteed state.lastBridgeTestResult is written to memory prior to dynamic page rendering so the GUI updates immediately upon button click.
 *  v1.8.10 (2026-09-20) - Targeted Reconciliation & Feature Storm Elimination:
 *                          - Separated raw authoritative Amazon endpoint retrieval (getRawAmazonEndpointMap) from UI family filtering (getDiscoveredDeviceMap). Toggling Category filters no longer deletes existing child devices.
 *                          - Removed fetchDeviceFeatures() from periodic background syncDeviceStates() polling loop while preserving capability fetching during child creation, discovery refresh, and targeted child refresh.
 *                          - Refactored refreshDeviceData(deviceId) to perform targeted single-child state and capability refresh when supplied with a deviceNetworkId.
 *  v1.8.9 (2026-09-20) - Known Amazon API Bug Endpoint Masking:
 *                         - Hardcoded explicit masking map for phantom/corrupt WHA endpoints 'EverywhereTwo' (abf36a9da0114483b860af9748b4b10b) and 'Office Test' (95bdc5e229e245a88f82e2adddf694d3).
 *                         - Excluded masked endpoints from getDiscoveredDeviceMap() discovery map and state.discoveredDeviceCache.
 *                         - Added clear UI callout card in Section 2 displaying the specific masked WHA devices and serial numbers.
 *  v1.8.8 (2026-09-20) - Full Master Orchestration Engine & Authoritative Reconciliation Refactor:
 *                         - Implemented robust syncDeviceStates() as the master synchronization engine between Amazon and Hubitat.
 *                         - Authoritative Amazon Reconciliation: Automatically strips deleted or invalid DNIs from settings.selectedEchoes.
 *                         - Automatic Orphan Cleanup: Deletes Hubitat child devices no longer present on Amazon server endpoint map.
 *                         - Dynamic Label & Name Sync: Automatically updates Hubitat child device labels and amazonDeviceName attributes when changed on Amazon.
 *                         - Connected Capability Pipeline: Invokes fetchDeviceFeatures() during sync path to populate state.capabilitiesMap on child drivers.
 *                         - Standardized Name Resolution Hierarchy: Aligned parent accountName resolution to dev.accountName ?: dev.deviceName ?: dev.label.
 *                         - Wired btnRefreshDevices to trigger full cache clearing and syncDeviceStates() reconciliation directly.
 *  v1.8.7 (2026-09-20) - Discovery Cache Invalidation & Stale Endpoint Cleanup:
 *                         - Clears state.discoveredDeviceCache explicitly inside appButtonHandler() when btnRefreshDevices is clicked.
 *                         - Ensures removed or renamed Alexa endpoints are updated immediately in UI dropdowns without retaining stale state keys.
 *  v1.8.6 (2026-09-19) - Dynamic Dual-Driver Selection Architecture Sync:
 *                         - Updated syncChildDevices() to inspect state.discoveredDeviceCache[dni].capabilities for TEMPERATURE_SENSOR or MOTION_DETECTION.
 *                         - Routes child device creation to "Echos Speak Advanced W/Sensors Device" or "Echos Speak Advanced Standard Device".
 *                         - Updated getDiscoveredDeviceMap() display options to indicate sensor capability status.
 *                         - Renumbered Parent app version to align directly with v1.8.6 child drivers.
 *  v1.6.1 (2026-09-19) - Alignment with Driver v1.7.3 Announcement Pipeline Streamlining:
 *                         - Incremented version demarcation following removal of legacy announceAll command dispatch.
 *  v1.6.0 (2026-09-18) - Shared Device Features & Shared Icon Base Location:
 *                         - Inserted Category C: Shared Device Features into main GUI layout.
 *                         - Added altIconLoc setting defaulting to "http://192.168.1.12/myicons/echos_speak/".
 *                         - Added calcIconBasePath() and getIconBasePath() methods to serve child drivers.
 *                         - Shifted Polling Schedule to Category D and System Diagnostics to Category E.
 *
 *				[KEEP] Prior change history is found in changelog_parent.txt (v0.0.0 - v1.5.10)
 **/

import java.util.concurrent.ConcurrentHashMap
import groovy.transform.Field

@Field static Object txCounterLock = new Object()
@Field static Long txCounter = 0L

@Field static final Map<String, Map<String, String>> maskedAmazonEndpoints = [
    "abf36a9da0114483b860af9748b4b10b": [name: "EverywhereTwo", family: "WHA", reason: "Known Amazon Bug"],
    "95bdc5e229e245a88f82e2adddf694d3": [name: "Office Test",    family: "WHA", reason: "Known Amazon Bug"]
]

private static String getNextTxId() {
    synchronized(txCounterLock) {
        txCounter++
        return "tx_${new Date().getTime()}_${txCounter}"
    }
}

static String version() { return '2.1.0' }
def timeStamp() { return "2026/09/21 06:00 PM" }

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
    logTrace "=== mainPage() Execution Started ==="
    
    Boolean startCollapsed = (state.pageOpened != true)
    state.pageOpened = true

    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* App Title Banner & Active Status Card */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Echos Speak Advanced</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
            
            def authData = fetchAuthStatusData()
            renderHealthCard(authData)
        }

        /* CATEGORY A: NETWORK, BRIDGE & DIAGNOSTICS */
        section("🌐 <b>CATEGORY A: NETWORK, BRIDGE & DIAGNOSTICS</b>", hideable: true, hidden: startCollapsed) {
            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 1A: Synology NAS REST API Bridge Configuration</b>" +
                      "<div style='color:#555; font-size:12px; margin-top:4px; margin-bottom:8px;'>" +
                      "Enter the local IP address and REST API port of your containerized Node.js bridge server.</div></div>"

            input name: "bridgeIp", type: "string", title: "<b>Bridge Server IP Address*</b>", defaultValue: "192.168.1.12", required: true, submitOnChange: true, width: 6
            input name: "bridgePort", type: "number", title: "<b>REST API Port*</b>", defaultValue: 4000, required: true, submitOnChange: true, width: 6

            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:8px; border-radius:4px; font-size:12px; margin-top:6px; margin-bottom:10px;'>" +
                      "ℹ️ <b>Amazon Auth Proxy Link:</b> If Amazon cookie authentication expires, open your browser in an <b>Incognito Window</b> to " +
                      "<a href='http://${getBridgeIp()}:4001' target='_blank' style='color:#2980B9; font-weight:bold; text-decoration:underline;'>" +
                      "http://${getBridgeIp()}:4001</a> to renew your session.</div>"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:10px 0;'/>"

            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 1B: Network & Amazon Session Diagnostic Suite</b>" +
                      "<div style='color:#555; font-size:12px; margin-top:4px; margin-bottom:8px;'>" +
                      "Run live diagnostic tests to verify local bridge connectivity and Amazon Watchdog authentication status.</div></div>"

            input name: "btnTestBridge", type: "button", title: "<b>Test REST API Bridge Network</b>", width: 6
            input name: "btnTestWatchdog", type: "button", title: "<b>Test Amazon Session & Watchdog Health</b>", width: 6

            String bridgeTestOutput = state.lastBridgeTestResult ?: "⚪ <b>Last Tested:</b> Not Run"
            String watchdogTestOutput = state.lastWatchdogTestResult ?: "⚪ <b>Last Tested:</b> Not Run"

            paragraph "<div style='background-color:#F2F4F4; border-left:4px solid #2980B9; padding:10px; border-radius:4px; font-size:12px; margin-top:8px;'>" +
                      "<div style='margin-bottom:6px;'><b>1. REST API Bridge Network:</b><br/>${bridgeTestOutput}</div>" +
                      "<hr style='border:0; border-top:1px solid #D5D8DC; margin:6px 0;'/>" +
                      "<div><b>2. Amazon Session Watchdog:</b><br/>${watchdogTestOutput}</div>" +
                      "</div>"
        }

        /* CATEGORY B: DEVICE DISCOVERY & FILTERING */
        section("📱 <b>CATEGORY B: DEVICE DISCOVERY & FILTERING</b>", hideable: true, hidden: startCollapsed) {
            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 2: Device Family Filtering Matrix</b>" +
                      "<div style='color:#555; font-size:12px; margin-top:4px; margin-bottom:8px;'>" +
                      "Enable specific categories to expose their corresponding Amazon hardware or software family in Section 3 discovery.</div></div>"

            paragraph "<div style='background-color:#E8F8F5; border-left:4px solid #1ABC9C; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "🛡️ <b>Server Hardware Masking:</b> Devices in the <b>'VOX'</b> family are masked directly on the Synology NAS server layer and cannot be toggled.</div>"

            paragraph "<div style='background-color:#FDEDEC; border-left:4px solid #E74C3C; padding:8px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
                      "⚠️ <b>Amazon API Bug Endpoint Masking:</b> The following devices are automatically hidden from discovery due to a known Amazon bug:<br/>" +
                      "• <b>EverywhereTwo</b> (Family: WHA | S/N: <code>abf36a9da0114483b860af9748b4b10b</code>)<br/>" +
                      "• <b>Office Test</b> (Family: WHA | S/N: <code>95bdc5e229e245a88f82e2adddf694d3</code>)</div>"

            input name: "catIncludeEchos", type: "bool", title: "<b>Category 1: Standard Echo & Dot Hardware (ECHO / ROOK)</b>", defaultValue: true, submitOnChange: true, width: 6
            input name: "catIncludeShows", type: "bool", title: "<b>Category 2: Echo Show & Screen Hardware (SHOW / KNIGHT)</b>", defaultValue: true, submitOnChange: true, width: 6
            input name: "catIncludeFireTV", type: "bool", title: "<b>Category 3: Fire TV & Media Streaming Devices (FIRE_TV / AFT)</b>", defaultValue: true, submitOnChange: true, width: 6
            input name: "catIncludeTablets", type: "bool", title: "<b>Category 4: Fire Tablets (TABLET)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catIncludeAuto", type: "bool", title: "<b>Category 5: Echo Auto & Mobile Accessories (AUTO)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catInclude3rdParty", type: "bool", title: "<b>Category 6: Third-Party Alexa Speakers (3RD_PARTY)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catIncludeWHA", type: "bool", title: "<b>Category 7: Multi-Room Audio Groups (WHA)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catIncludeApps", type: "bool", title: "<b>Category 8: Alexa Apps & Software Endpoints (APP / REAVER)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catIncludeVoxLocked", type: "bool", title: "<b>Category 9: VOX Family (Disabled at Server)</b>", defaultValue: false, disabled: true, width: 6

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:10px 0;'/>"

            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 3: Alexa Device & Endpoint Discovery</b>" +
                      "<div style='color:#555; font-size:12px; margin-top:4px; margin-bottom:8px;'>" +
                      "Select discovered Echo hardware or software endpoints to create child devices in Hubitat. Only categories enabled in Section 2 are displayed.</div></div>"

            Map devMap = getDiscoveredDeviceMap()
            if (devMap) {
                input name: "selectedEchoes", type: "enum", title: "<b>Discovered Alexa Endpoints (${devMap.size()} Available)</b>", options: devMap, multiple: true, required: false
            } else {
                paragraph "<div style='background-color:#FEF9E7; border-left:4px solid #F39C12; padding:8px; border-radius:4px; font-size:12px;'>" +
                          "⚠️ <b>No Matching Endpoints Found:</b> Verify server auth on port 4001 or check that relevant Category Toggles in Section 2 are enabled.</div>"
            }

            input name: "btnRefreshDevices", type: "button", title: "<b>Force Refresh Discovered Device Map</b>", width: 12
        }

        /* CATEGORY C: SHARED DEVICE FEATURES */
        section("🎨 <b>CATEGORY C: SHARED DEVICE FEATURES</b>", hideable: true, hidden: startCollapsed) {
            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 4: Icon & Media Repository Settings</b>" +
                      "<div style='color:#555; font-size:12px; margin-top:4px; margin-bottom:8px;'>" +
                      "Specify the base URL directory containing device model artwork and icon resources shared across all child devices.</div></div>"

            input name: "altIconLoc", type: "text", title: "<b>Base Override - Icon Location Location</b>", 
                  description: "Enter URL for shared Echo hardware icons repository<br><b>Default: http://192.168.1.12/myicons/echos_speak/</b>", 
                  defaultValue: "http://192.168.1.12/myicons/echos_speak/", required: true, submitOnChange: true
        }

        /* CATEGORY D: POLLING & REFRESH SCHEDULES */
        section("⏱️ <b>CATEGORY D: POLLING & REFRESH SCHEDULES</b>", hideable: true, hidden: startCollapsed) {
            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 5: Background Refresh & State Sync Engine</b>" +
                      "<div style='color:#555; font-size:12px; margin-top:4px; margin-bottom:8px;'>" +
                      "Configure background synchronization interval for pulling volume, media playback, capabilities, and availability states.</div></div>"

            input name: "refreshInterval", type: "enum", title: "<b>Device State Polling Interval</b>", 
                  options: ["1": "Every Minute", "5": "Every 5 Minutes", "10": "Every 10 Minutes", "15": "Every 30 Minutes", "30": "Every 30 Minutes", "0": "Disabled"], 
                  defaultValue: "5", required: true
        }

        /* CATEGORY E: DIAGNOSTICS & APP PREFERENCES */
        section("⚙️ <b>CATEGORY E: DIAGNOSTICS & APP PREFERENCES</b>", hideable: true, hidden: startCollapsed) {
            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 6: System Diagnostics & Logging Options</b></div>"

            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            input name: "showCountInLabel", type: "bool", title: "Show Active Device Count in App Label?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: false, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }
    }
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

private String calcIconBasePath(String urlInput) {
    String base = (urlInput && urlInput.trim() != "") ? urlInput.trim() : "http://192.168.1.12/myicons/echos_speak/"
    if (!base.endsWith("/")) base += "/"
    logDebug "Calculated Shared Icon Base Path resolved to: ${base}"
    return base
}

String getIconBasePath() {
    if (!state.iconBasePath) {
        state.iconBasePath = calcIconBasePath(settings.altIconLoc)
    }
    return state.iconBasePath
}

private int getActiveEchoDeviceCount() {
    return getChildDevices()?.count { child ->
        child.deviceNetworkId != "ECHOS_SPEAK_SERVER_STATUS" && 
        child.typeName != "Echos Speak Server Status Tile Driver"
    } ?: 0
}

private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    Boolean showCount   = getSettingBool("showCountInLabel", true)

    String baseLabel = "Echos Speak Advanced"
    if (showVersion) baseLabel += " v${version()}"

    if (showCount) {
        int count = getActiveEchoDeviceCount()
        baseLabel += " - [<span style='color:#27AE60; font-weight:bold;'>${count} Device(s)</span>]"
    }

    if (app.label != baseLabel) {
        logTrace "Updating App Label -> New: '${baseLabel}'"
        app.updateLabel(baseLabel)
    }
}

private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn")) }
        .sort()

    sortedKeys.each { k ->
        def val = settings[k]
        if (val instanceof List) {
            snapshot[k] = val.collect { it.toString() }.sort()
        } else {
            snapshot[k] = val?.toString()
        }
    }
    
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    state.currentSettingsJson = jsonString
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
}

private void checkLoggingChanges() {
    Boolean currentInfo  = getSettingBool("logInfoEnable", true)
    Boolean currentDebug = getSettingBool("logDebugEnable", false)
    Boolean currentTrace = getSettingBool("logTraceEnable", false)

    if (state.lastLogInfoEnable == null)  state.lastLogInfoEnable  = currentInfo
    if (state.lastLogDebugEnable == null) state.lastLogDebugEnable = currentDebug
    if (state.lastLogTraceEnable == null) state.lastLogTraceEnable = currentTrace

    if (currentInfo != state.lastLogInfoEnable)   logWarn "Info Logging changed: ${state.lastLogInfoEnable ? 'ENABLED' : 'DISABLED'} -> ${currentInfo ? 'ENABLED' : 'DISABLED'}"
    if (currentDebug != state.lastLogDebugEnable) logWarn "Debug Logging changed: ${state.lastLogDebugEnable ? 'ENABLED' : 'DISABLED'} -> ${currentDebug ? 'ENABLED' : 'DISABLED'}"
    if (currentTrace != state.lastLogTraceEnable) logWarn "Trace Logging changed: ${state.lastLogTraceEnable ? 'ENABLED' : 'DISABLED'} -> ${currentTrace ? 'ENABLED' : 'DISABLED'}"

    state.lastLogInfoEnable  = currentInfo
    state.lastLogDebugEnable = currentDebug
    state.lastLogTraceEnable = currentTrace
}

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing app v${version()} (${timeStamp()})..."
    state.iconBasePath = calcIconBasePath(settings.altIconLoc)
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    state.lastSettingsJson = state.currentSettingsJson
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

    state.iconBasePath = calcIconBasePath(settings.altIconLoc)
    checkLoggingChanges()
    state.pageOpened = false

    String currentSnapshot = captureSettingsSnapshot()
    String previousSnapshot = state.lastSettingsSnapshot
    Boolean settingsChanged = (previousSnapshot == null || previousSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (state.appVersion != version())

    if (settingsChanged || codeVersionChanged) {
        logInfo "Settings or code version modification detected. Re-establishing schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        state.lastSettingsJson = state.currentSettingsJson
        unschedule()
        initialize(false)
        ensureServerStatusChildDevice()
        syncChildDevices()
    }
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling app..."
    unschedule()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    updateAppLabel()
    ensureServerStatusChildDevice()
    testBridgeConnection()

    int interval = settings.refreshInterval ? settings.refreshInterval.toInteger() : 5
    if (interval > 0) {
        schedule("0 */${interval} * ? * * *", "syncDeviceStates")
        logInfo "Scheduled device state background sync every ${interval} minutes."
    }

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

private String getBridgeIp() {
    return settings.bridgeIp ?: "192.168.1.12"
}

private int getBridgePort() {
    return settings.bridgePort ? settings.bridgePort.toInteger() : 4000
}

private void ensureServerStatusChildDevice() {
    String dni = "ECHOS_SPEAK_SERVER_STATUS"
    def statusDev = getChildDevice(dni) ?: getChildDevices()?.find { it.typeName == "Echos Speak Server Status Tile Driver" }
    if (!statusDev) {
        logInfo "Provisioning Echos Speak Server Status child device [${dni}]..."
        addChildDevice("jshimota", "Echos Speak Server Status Tile Driver", dni, [label: "Echos Speak Server Status Tile", isComponent: false])
    }
}

def refreshDeviceData(String deviceId = null) {
    if (deviceId) {
        logTrace "refreshDeviceData() triggered for specific child device [${deviceId}]"
        
        // 1. Verify existence against raw Amazon endpoint map (explicit refresh flag true to log bug warnings if applicable)
        Map rawMap = getRawAmazonEndpointMap(true)
        if (!rawMap.containsKey(deviceId)) {
            logWarn "refreshDeviceData(${deviceId}): Targeted device no longer exists on Amazon server or is masked."
            return
        }

        def child = getChildDevice(deviceId)
        if (!child) {
            logWarn "refreshDeviceData(${deviceId}): Hubitat child device [${deviceId}] not found."
            return
        }

        Map devData = state.discoveredDeviceCache?.get(deviceId) ?: [:]
        if (devData) {
            // Dispatch Hardware & Topology Data (updates amazonDeviceName while preserving user's Hubitat label)
            child.parseDeviceData(devData)
        }

        // Fetch and Dispatch Media State
        Map mediaData = fetchDeviceMediaState(deviceId)
        if (mediaData && mediaData.success == true && mediaData.result) {
            child.parseDeviceData(mediaData.result)
        }

        // Fetch and Dispatch Capabilities Contract for targeted child
        fetchDeviceFeatures(deviceId)
    } else {
        logTrace "refreshDeviceData() triggered with no deviceId; executing full system syncDeviceStates()"
        syncDeviceStates()
    }
}

def refreshServerStatusData() {
    logTrace "refreshServerStatusData() requested from child status device"
    fetchAuthStatusData()
}

def forceServerSessionRevalidation() {
    logInfo "forceServerSessionRevalidation() triggered from child status device"
    fetchAuthStatusData()
}

Map fetchDeviceMediaState(String serialNumber) {
    if (!serialNumber) return [:]
    String url = "http://${getBridgeIp()}:${getBridgePort()}/api/media/${serialNumber}"
    logTrace "--> fetchDeviceMediaState() Initiated -> Targeting URL: [${url}]"

    Map params = [
        uri: url,
        timeout: 5,
        contentType: "application/json",
        ignoreSSLIssues: true
    ]

    Map mediaResult = [:]
    try {
        httpGet(params) { resp ->
            if (resp.status == 200 && resp.data) {
                mediaResult = resp.data
            }
        }
    } catch (Exception e) {
        logError "Failed to fetch live media state for device [${serialNumber}] from ${url}: ${e.message}"
        mediaResult = [success: false, error: e.message]
    }

    return mediaResult
}

private fetchAuthStatusData() {
    String url = "http://${getBridgeIp()}:${getBridgePort()}/auth/status"
    logTrace "--> fetchAuthStatusData() Initiated -> Targeting URL: [${url}]"

    Map params = [
        uri: url,
        timeout: 5,
        contentType: "application/json",
        ignoreSSLIssues: true
    ]
    
    def authResult = [:]
    try {
        httpGet(params) { resp ->
            if (resp.status == 200 && resp.data) {
                authResult = resp.data
            }
        }
    } catch (Exception e) {
        logError "Bridge status check exception at ${url}: ${e.message}"
        authResult = [state: "OFFLINE", authenticated: false, lastError: e.message]
    }
    
    state.bridgeConnected = (authResult?.authenticated == true)

    def statusDev = getChildDevice("ECHOS_SPEAK_SERVER_STATUS") ?: getChildDevices()?.find { it.typeName == "Echos Speak Server Status Tile Driver" }
    if (statusDev) {
        statusDev.parseServerStatusData(authResult)
    }

    return authResult
}

private Boolean testBridgeConnection() {
    def authData = fetchAuthStatusData()
    return (authData?.authenticated == true)
}

private void renderHealthCard(auth) {
    String stateStr = auth?.state ?: "OFFLINE"
    Boolean isAuth = auth?.authenticated == true

    String statusBadge = "⚪ UNKNOWN"
    String borderColor = "#7F8C8D"

    switch (stateStr) {
        case "AUTHENTICATED":
            statusBadge = "<span style='color:#27AE60; font-weight:bold;'>🟢 VALID</span>"
            borderColor = "#27AE60"
            break
        case "DEGRADED":
            statusBadge = "<span style='color:#F39C12; font-weight:bold;'>🟡 DEGRADED</span>"
            borderColor = "#F39C12"
            break
        case "AUTH_REQUIRED":
            statusBadge = "<span style='color:#C0392B; font-weight:bold;'>🔴 AUTH REQUIRED</span>"
            borderColor = "#C0392B"
            break
        default:
            statusBadge = "<span style='color:#7F8C8D; font-weight:bold;'>⚪ OFFLINE</span>"
            borderColor = "#7F8C8D"
            break
    }

    String lastValid = auth?.lastValidated ? parseIsoTimestamp(auth.lastValidated.toString()) : "Never"
    String ageStr = formatDuration(auth?.sessionAgeSeconds)
    String nextValStr = auth?.nextValidationSeconds != null ? "${Math.round(auth.nextValidationSeconds / 60)} min" : "N/A"
    String lastErr = auth?.lastError ?: "None"
    int activeChildCount = getActiveEchoDeviceCount()

    paragraph "<div style='background-color:#F8F9FA; border-left:5px solid ${borderColor}; padding:10px; border-radius:4px; font-size:12px; margin-bottom:10px;'>" +
              "<table style='width:100%; border-collapse:collapse; color:#333;'>" +
              "<tr><td style='padding:2px 0;'><b>Amazon Session Status:</b></td><td>${statusBadge}</td>" +
              "<td style='padding:2px 0;'><b>Session Age:</b></td><td>${ageStr}</td></tr>" +
              "<tr><td style='padding:2px 0;'><b>Last Amazon Check:</b></td><td>${lastValid}</td>" +
              "<td style='padding:2px 0;'><b>Next Check:</b></td><td>${nextValStr}</td></tr>" +
              "<tr><td style='padding:2px 0;'><b>Last Error:</b></td><td colspan='3' style='color:${auth?.lastError ? "#C0392B" : "#27AE60"};'>${lastErr}</td></tr>" +
              "</table>" +
              "<hr style='border:0; border-top:1px solid #E0E0E0; margin:6px 0;'/>" +
              "<b>Bridge:</b> <span style='color:${isAuth ? "#27AE60" : "#C0392B"}; font-weight:bold;'>${isAuth ? "ONLINE" : "OFFLINE"}</span> &nbsp;|&nbsp; " +
              "<b>Active Echo Devices:</b> <span style='color:#2980B9; font-weight:bold;'>${activeChildCount}</span>" +
              "</div>"
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

Boolean sendBridgeCommand(String serialNumber, String command, Map payload = [:]) {
    String url = "http://${getBridgeIp()}:${getBridgePort()}/api/command"
    logTrace "--> sendBridgeCommand() Initiated -> Serial: [${serialNumber}] | Cmd: [${command}] | Payload: ${payload}"

    Map bodyData = [
        serialNumber: serialNumber,
        command: command,
        payload: payload
    ]

    Map params = [
        uri: url,
        body: bodyData,
        timeout: 10,
        requestContentType: "application/json",
        contentType: "application/json",
        ignoreSSLIssues: true
    ]

    Boolean isSuccess = false
    try {
        httpPostJson(params) { resp ->
            logDebug "<-- sendBridgeCommand() HTTP Response Code: [${resp.status}]"
            if (resp.status == 200) {
                isSuccess = true
                logDebug "Command '${command}' completed successfully for device [${serialNumber}]"
            } else {
                logError "Command '${command}' failed for device [${serialNumber}] -> HTTP Status [${resp.status}]"
            }
        }
    } catch (Exception e) {
        logError "HTTP Post error sending command '${command}' to device [${serialNumber}]: ${e.message}"
    }
    logTrace "<-- sendBridgeCommand() Exit"
    return isSuccess
}

private void fetchDeviceFeatures(String serialNumber) {
    if (!serialNumber) return
    String url = "http://${getBridgeIp()}:${getBridgePort()}/api/command"
    logTrace "--> fetchDeviceFeatures() Requesting canonical getDeviceFeatures for [${serialNumber}]"

    Map bodyData = [
        serialNumber: serialNumber,
        command: "getDeviceFeatures",
        payload: [serialNumber: serialNumber]
    ]

    Map params = [
        uri: url,
        body: bodyData,
        timeout: 10,
        requestContentType: "application/json",
        contentType: "application/json",
        ignoreSSLIssues: true
    ]

    try {
        httpPostJson(params) { resp ->
            if (resp.status == 200 && resp.data) {
                def child = getChildDevice(serialNumber)
                if (child) {
                    Map payloadData = (resp.data.result instanceof Map) ? resp.data.result : resp.data
                    child.parseDeviceFeatures(payloadData)
                }
            }
        }
    } catch (Exception e) {
        logError "Failed to fetch device features for [${serialNumber}]: ${e.message}"
    }
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

def appButtonHandler(btn) {
    String txId = getNextTxId()
    logTrace "--> appButtonHandler() [#${txId}] Triggered -> Button: [${btn}]"
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    String nowTime = new Date().format("hh:mm:ss a", tz)

    if (btn == "btnRefreshDevices") {
        logInfo "Clearing local cache and executing full sync with bridge..."
        state.discoveredDeviceCache = [:]
        // Pass explicit refresh flag true to log masked endpoints as logWarn during force refresh
        getRawAmazonEndpointMap(true)
        syncDeviceStates()

        // Explicit discovery refresh populates capabilities for all selected devices
        def rawSelected = settings.selectedEchoes
        List<String> currentSelected = rawSelected
            ? (rawSelected instanceof Collection ? rawSelected.collect { it.toString() } : [rawSelected.toString()])
            : []
        currentSelected.each { dni -> fetchDeviceFeatures(dni) }
    } else if (btn == "btnTestBridge") {
        logInfo "Executing Bridge REST API ping test..."
        String url = "http://${getBridgeIp()}:${getBridgePort()}/api/devices"
        Map params = [
            uri: url,
            timeout: 5,
            contentType: "application/json",
            ignoreSSLIssues: true
        ]
        try {
            httpGet(params) { resp ->
                if (resp.status == 200) {
                    state.lastBridgeTestResult = "🟢 <b>Last Tested: ${nowTime}</b> — PASSED: Connected successfully to <code>http://${getBridgeIp()}:${getBridgePort()}</code> (HTTP 200 OK)."
                } else {
                    state.lastBridgeTestResult = "🔴 <b>Last Tested: ${nowTime}</b> — FAILED: Server responded with HTTP ${resp.status}."
                }
            }
        } catch (Exception e) {
            state.lastBridgeTestResult = "🔴 <b>Last Tested: ${nowTime}</b> — FAILED: Exception: ${e.message}"
        }
    } else if (btn == "btnTestWatchdog") {
        logInfo "Executing Amazon Session & Watchdog status test..."
        def authData = fetchAuthStatusData()
        Boolean isAuth = (authData?.authenticated == true)
        String authState = authData?.state ?: "OFFLINE"
        String lastErr = authData?.lastError ?: "None"

        if (isAuth) {
            state.lastWatchdogTestResult = "🟢 <b>Last Tested: ${nowTime}</b> — PASSED: Session State: <b>${authState}</b> | Last Verified: ${authData.lastValidated ? parseIsoTimestamp(authData.lastValidated.toString()) : 'Just Now'}."
        } else {
            state.lastWatchdogTestResult = "🔴 <b>Last Tested: ${nowTime}</b> — FAILED: Session State: <b>${authState}</b> | Error: ${lastErr}."
        }
    }

    return mainPage()
}

private Boolean isFamilyAllowed(String family) {
    String fam = family?.toUpperCase() ?: "UNKNOWN"
    if (fam == "VOX") return false

    Boolean incEcho       = getSettingBool("catIncludeEchos", true)
    Boolean incShow       = getSettingBool("catIncludeShows", true)
    Boolean incFireTV     = getSettingBool("catIncludeFireTV", true)
    Boolean incTablets    = getSettingBool("catIncludeTablets", false)
    Boolean incAuto       = getSettingBool("catIncludeAuto", false)
    Boolean inc3rdParty   = getSettingBool("catInclude3rdParty", false)
    Boolean incWHA        = getSettingBool("catIncludeWHA", false)
    Boolean incApps       = getSettingBool("catIncludeApps", false)

    if (fam in ["ECHO", "ROOK"]) return incEcho
    if (fam in ["KNIGHT", "SHOW"]) return incShow
    if (fam in ["FIRE_TV", "AFT"]) return incFireTV
    if (fam in ["TABLET"]) return incTablets
    if (fam in ["AUTO"]) return incAuto
    if (fam in ["3RD_PARTY", "THIRD_PARTY"]) return inc3rdParty
    if (fam in ["WHA"]) return incWHA
    if (fam in ["REAVER", "APP", "SOFTWARE", "EVERYWHERE"]) return incApps

    return false
}

private Map getRawAmazonEndpointMap(Boolean isExplicitRefresh = false) {
    String url = "http://${getBridgeIp()}:${getBridgePort()}/api/devices"
    logTrace "--> getRawAmazonEndpointMap() Initiated -> Targeting URL: [${url}]"

    Map rawEndpoints = [:]
    Map cache = [:]
    Map params = [
        uri: url,
        timeout: 10,
        contentType: "application/json",
        ignoreSSLIssues: true
    ]

    try {
        httpGet(params) { resp ->
            if (resp.status == 200 && resp.data?.devices) {
                resp.data.devices.each { dev ->
                    String sNum = dev.serialNumber?.toString()
                    String fam = dev.deviceFamily?.toString()?.toUpperCase() ?: "UNKNOWN"
                    String name = dev.accountName ?: dev.deviceName ?: dev.label ?: "Echo Device (${sNum})"
                    List caps = (dev.capabilities instanceof List) ? dev.capabilities.collect { it.toString() } : []

                    if (sNum) {
                        // Exclude server-level masked VOX family
                        if (fam == "VOX") return

                        // Check explicit hardcoded bug mask
                        if (maskedAmazonEndpoints.containsKey(sNum)) {
                            String maskMsg = "Masking Amazon bug endpoint: ${name} [${sNum}] (${fam})"
                            if (isExplicitRefresh) {
                                logWarn maskMsg
                            } else {
                                logTrace maskMsg
                            }
                            return
                        }

                        cache[sNum] = [
                            accountName: name,
                            deviceFamily: fam,
                            deviceType: dev.deviceType?.toString() ?: "UNKNOWN",
                            deviceStyle: dev.deviceStyle?.toString() ?: "UNKNOWN",
                            generation: dev.generation?.toString() ?: "",
                            softwareVer: dev.softwareVersion?.toString() ?: "",
                            capabilities: caps
                        ]

                        rawEndpoints[sNum] = [
                            accountName: name,
                            deviceFamily: fam,
                            capabilities: caps
                        ]
                    }
                }
                state.discoveredDeviceCache = cache
            }
        }
    } catch (Exception e) {
        logError "Failed to fetch raw device map from bridge: ${e.message}"
        state.bridgeConnected = false
    }

    return rawEndpoints
}

private Map getDiscoveredDeviceMap() {
    Map rawMap = getRawAmazonEndpointMap()
    Map devices = [:]

    rawMap.each { sNum, dev ->
        String fam = dev.deviceFamily?.toString()?.toUpperCase() ?: "UNKNOWN"
        String name = dev.accountName ?: "Echo Device (${sNum})"
        List caps = dev.capabilities ?: []

        if (isFamilyAllowed(fam)) {
            Boolean hasTemperature = caps.contains("TEMPERATURE_SENSOR") || caps.contains("TEMPERATURE")
            Boolean hasMotion      = caps.contains("MOTION_DETECTION") || caps.contains("MOTION")
            Boolean hasDisplay     = caps.contains("DISPLAY") || caps.contains("DISPLAY_POWER") || caps.contains("DISPLAY_BRIGHTNESS") || caps.contains("ADAPTIVE_BRIGHTNESS")
            Boolean hasExtended    = hasTemperature || hasMotion || hasDisplay

            devices[sNum] = "${name} (${fam}) ${hasExtended ? '[Sensors/Display]' : '[Standard]'}"
            logDebug "Discovered Device Endpoint (Filtered UI): ${name} [${sNum}] | Family: ${fam} | Extended: ${hasExtended}"
        }
    }

    return devices
}

private void syncChildDevices() {
    def rawSelected = settings.selectedEchoes
    List<String> selected = rawSelected
        ? (rawSelected instanceof Collection 
            ? rawSelected.collect { it.toString() } 
            : [rawSelected.toString()])
        : []

    List currentChildren = getChildDevices().findAll { 
        it.deviceNetworkId != "ECHOS_SPEAK_SERVER_STATUS" && 
        it.typeName != "Echos Speak Server Status Tile Driver" 
    }

    currentChildren.each { child ->
        if (!selected.contains(child.deviceNetworkId)) {
            logWarn "Removing unselected child device: ${child.displayName} [${child.deviceNetworkId}]"
            deleteChildDevice(child.deviceNetworkId)
        }
    }

    selected.each { dni ->
        def existing = getChildDevice(dni)
        if (!existing) {
            Map devData = state.discoveredDeviceCache?.get(dni) ?: [:]
            String devName = devData.accountName ?: "Echo Device (${dni})"
            
            // Extract raw capability array from cached device discovery payload
            List rawCapabilities = (devData.capabilities instanceof List) ? devData.capabilities : []
            
            // Evaluates OR condition: temperature, motion, OR display triggers extended Sensors driver routing
            Boolean hasTemperature = rawCapabilities.contains("TEMPERATURE_SENSOR") || rawCapabilities.contains("TEMPERATURE")
            Boolean hasMotion      = rawCapabilities.contains("MOTION_DETECTION") || rawCapabilities.contains("MOTION")
            Boolean hasDisplay     = rawCapabilities.contains("DISPLAY") || rawCapabilities.contains("DISPLAY_POWER") || rawCapabilities.contains("DISPLAY_BRIGHTNESS") || rawCapabilities.contains("ADAPTIVE_BRIGHTNESS")
            Boolean useSensorDriver = hasTemperature || hasMotion || hasDisplay

            String driverName = useSensorDriver
                ? "Echos Speak Advanced W/Sensors Device"
                : "Echos Speak Advanced Standard Device"

            logInfo "Creating child device [${devName}] (${dni}) using driver: ${driverName} (Extended Capabilities: ${useSensorDriver})"
            
            try {
                addChildDevice(
                    "jshimota",
                    driverName,
                    dni,
                    [label: devName, isComponent: false]
                )
                // Fetch capabilities immediately upon initial child creation
                fetchDeviceFeatures(dni)
            } catch (Exception e) {
                logError "Failed to create child device [${dni}] with driver '${driverName}': ${e.message}"
            }
        }
    }

    syncDeviceStates()
}

def syncDeviceStates() {
    logTrace "--> syncDeviceStates() Initiated -> Starting authoritative Amazon reconciliation..."
    
    // 1. Fetch Authoritative Raw Amazon Endpoint List (unfiltered by Category toggles, enforces bug masking)
    Map rawAmazonEndpointMap = getRawAmazonEndpointMap()
    Set<String> validAmazonDnis = rawAmazonEndpointMap ? rawAmazonEndpointMap.keySet() : ([] as Set<String>)

    if (!validAmazonDnis && state.bridgeConnected == false) {
        logError "syncDeviceStates() aborted: Unable to reach Synology REST API bridge."
        return
    }

    // 2. Reconcile settings.selectedEchoes against Authoritative Raw Amazon Endpoints
    def rawSelected = settings.selectedEchoes
    List<String> currentSelected = rawSelected
        ? (rawSelected instanceof Collection ? rawSelected.collect { it.toString() } : [rawSelected.toString()])
        : []

    List<String> reconciledSelected = currentSelected.findAll { dni -> validAmazonDnis.contains(dni) }
    
    if (currentSelected.size() != reconciledSelected.size()) {
        List<String> removedDnis = currentSelected - reconciledSelected
        logWarn "Amazon Endpoint Reconciliation: Stripping deleted, invalid, or masked device DNIs from settings.selectedEchoes: ${removedDnis}"
        app.updateSetting("selectedEchoes", [type: "enum", value: reconciledSelected])
    }

    // 3. Delete Orphaned Hubitat Child Devices
    List currentChildren = getChildDevices().findAll { 
        it.deviceNetworkId != "ECHOS_SPEAK_SERVER_STATUS" && 
        it.typeName != "Echos Speak Server Status Tile Driver" 
    }

    currentChildren.each { child ->
        if (!validAmazonDnis.contains(child.deviceNetworkId) || !reconciledSelected.contains(child.deviceNetworkId)) {
            logWarn "Deleting orphaned Hubitat child device no longer on Amazon server: ${child.displayName} [${child.deviceNetworkId}]"
            deleteChildDevice(child.deviceNetworkId)
        }
    }

    // 4. Update Existing Children and Dispatch Live State Updates (omits periodic fetchDeviceFeatures call & leaves child labels untouched)
    reconciledSelected.each { dni ->
        def child = getChildDevice(dni)
        Map devData = state.discoveredDeviceCache?.get(dni) ?: [:]

        if (child && devData) {
            // Dispatch Hardware & Topology Data (updates amazonDeviceName while preserving user's Hubitat label)
            child.parseDeviceData(devData)

            // Fetch and Dispatch Media State
            Map mediaData = fetchDeviceMediaState(dni)
            if (mediaData && mediaData.success == true && mediaData.result) {
                child.parseDeviceData(mediaData.result)
            }
        }
    }

    updateAppLabel()
    logTrace "<-- syncDeviceStates() Completed"
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appLabel = app.label ?: app.name ?: "Echos Speak Advanced"

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