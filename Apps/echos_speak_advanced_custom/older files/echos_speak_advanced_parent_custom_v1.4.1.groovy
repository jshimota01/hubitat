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
 *  v1.4.1 (2026-09-14) - Restored Complete Parent App Historical Changelog:
 *                        - Restored all historical changelog entries back through v1.0.0.
 *  v1.4.0 (2026-09-14) - Server Command Dispatch Mapping Alignment:
 *                        - Aligned sendBridgeCommand mapping for mute, unmute, dnd, doNotDisturbOn/Off, and voiceCmdAsText.
 *  v1.3.7 (2026-09-14) - Diagnostic Button Refresh & Category Section Persistence Fix:
 *                        - Resolved UI non-responsiveness on diagnostic button presses by enforcing dynamic page re-evaluation.
 *                        - Preserved active Category expansion state during diagnostic button refreshes.
 *  v1.3.6 (2026-09-14) - Advanced State Engine & Section 1B Diagnostic UI Refactor:
 *                        - Integrated Mode Manager Advanced thread-safe transaction counter, state history snapshot hashing, and logging wrappers.
 *                        - Redesigned Section 1B diagnostic panel with stacked permanent status cards (Bridge Network & Amazon Session).
 *                        - Initialized diagnostic status values to 'Last Tested: Not Run' on initial app load.
 *                        - Added timestamped diagnostic outputs (hh:mm:ss a) for both REST API bridge and Watchdog tests.
 *  v1.3.5 (2026-09-14) - Section Collapse Persistence Fix:
 *                        - Fixed premature section collapse on submitOnChange dynamic page refreshes using state.pageOpened tracking.
 *                        - Categories start collapsed on initial app open and remain expanded during active editing until Done is clicked.
 *  v1.3.4 (2026-09-14) - Section Collapse Default & Diagnostic Formatting Fix:
 *                        - Set all main Category headers (A-D) to start collapsed by default (hidden: true) on page entry.
 *                        - Bound btnTestWatchdog UI response to state.lastTestResult for on-screen output rendering.
 *                        - Added execution timestamps (hh:mm:ss a) to diagnostic output banners.
 *  v1.3.3 (2026-09-14) - UI Hierarchy Refactor & Diagnostic Suite Integration:
 *                        - Converted main Category headers (A-D) into native collapsible/expandable sections (hideable: true).
 *                        - Restyled Category headers with lighter slate borders and visual indentation for clear Section nesting.
 *                        - Added Section 1B: Network & Amazon Session Diagnostic Suite under Category A.
 *                        - Implemented btnTestBridge and btnTestWatchdog UI action buttons in appButtonHandler().
 *  v1.3.2 (2026-09-14) - Category Consolidation Refactor:
 *                        - Folded Category 8 (EVERYWHERE) into Category 9 (REAVER / APP).
 *                        - Removed standalone catIncludeWholesite setting input from Section 2 Matrix.
 *                        - Retained Category 8 text label 'Alexa Apps & Software Endpoints (REAVER / APP)'.
 *                        - Updated isFamilyAllowed() to allow EVERYWHERE when catIncludeApps toggle is active.
 *                        - Renumbered VOX Family read-only toggle to Category 9.
 *  v1.3.1 (2026-09-12) - Server Status Child Routing & Binding Fix:
 *                        - Updated child device driver target to 'Echos Speak Server Status Tile Driver'.
 *                        - Dynamic lookup fallback added in fetchAuthStatusData() to find status tile child device by DNI or driver name.
 *  v1.3.0 (2026-09-12) - Active Echo Device Counting Fix:
 *                        - Added getActiveEchoDeviceCount() helper to filter out ECHOS_SPEAK_SERVER_STATUS DNI.
 *                        - Replaced fragile size() - 1 math in updateAppLabel() and renderHealthCard() to accurately reflect Echo speaker count.
 *  v1.2.9 (2026-09-12) - App Template Alignment & Tile Driver Target Update:
 *                        - Aligned lifecycle methods with Master App Template v1.2.2 (state.appVersion & captureSettingsSnapshot).
 *                        - Updated child device creation target to 'Echos Speak Server Status Tile Driver'.
 *                        - Refactored status polling and logging mechanics.
 *  v1.2.8 (2026-09-12) - Server Status Child Device Integration:
 *                        - Added auto-creation of dedicated 'Echos Speak Server Status' child device (DNI: 'ECHOS_SPEAK_SERVER_STATUS').
 *                        - Pushed Watchdog telemetry directly to child status device for Rule Machine & HD+ Dashboard htmlTile support.
 *                        - Added forceServerSessionRevalidation() handler to allow child device manual triggers.
 *  v1.2.7 (2026-09-12) - Watchdog Integration & Dashboard Health Tile:
 *                        - Integrated Watchdog metrics parsing from GET /auth/status.
 *                        - Rendered Amazon Session Watchdog Card in Parent App main page.
 *  v1.2.6 (2026-09-12) - Architectural Refinement & Bug Fixes:
 *                        - Corrected KNIGHT device family mapping to Category 2 (Echo Show / Screen Hardware).
 *                        - Eliminated silent 'ECHO' default fallback on missing device family properties.
 *                        - Normalized settings.selectedEchoes enum parsing to guarantee a robust List scope.
 *                        - Replaced heavy state.rawDiscoveredDevices with lightweight state.discoveredDeviceCache.
 *                        - Completed all 5 log level settings in checkLoggingChanges() state tracker.
 *  v1.2.5 (2026-09-12) - Endpoint Discovery Naming & Strict Filter Enforcement:
 *                        - Strict filtering enforced in getDiscoveredDeviceMap(): devices are omitted unless their corresponding Category Toggle is true.
 *                        - Renamed Section 3 and dropdown title to 'Alexa Device & Endpoint Discovery' / 'Discovered Alexa Endpoints'.
 *                        - Added Category 9: Alexa Apps & Software Endpoints (REAVER).
 *                        - Added Category 10: VOX Family (Disabled at Server) with a read-only disabled toggle element.
 *  v1.2.4 (2026-09-12) - Amazon WHA & Synthetic Family Matrix Correction:
 *                        - Fixed Category 7 filter to map explicitly to Amazon deviceFamily == 'WHA' (Whole Home Audio).
 *                        - Refined Category 8 filter to target 'EVERYWHERE' synthetic targets independently.
 *                        - Corrected family resolution in isFamilyAllowed() to eliminate GROUP/WHA ambiguity.
 *  v1.2.3 (2026-09-12) - Layout & UI Label Formatting:
 *                        - Updated app label count display format to 'X Device(s)'.
 *                        - Re-ordered Section 2 to 'Device Family Filtering Matrix' and Section 3 to 'Amazon Echo Device Discovery'.
 *                        - Moved Server Hardware Masking notice banner into Section 2 Filtering Matrix.
 *  v1.2.2 (2026-09-12) - Family Filtering & VOX Banner Restored:
 *                        - Restored Category Section 3 filtering matrix (toggles for Echo, Show, FireTV, Tablets, Auto, 3rdParty, Groups, Everywhere).
 *                        - Added UI notice in Section 2 highlighting that 'VOX' family devices are masked at the server layer.
 *                        - Applied category filter evaluation to getDiscoveredDeviceMap() parser.
 *  v1.2.1 (2026-09-12) - Backend API Integration Alignment:
 *                        - Aligned GET /api/devices response parsing against alexaWrapper.alexa.serialNumbers structure.
 *                        - Enforced strict POST /api/command payload formatting across all device control methods.
 *                        - Verified volume command routing using sendSequenceCommand via bridge dispatcher.
 *  v1.2.0 (2026-09-11) - Server Network Alignment:
 *                        - Fixed REST API default port to 4000 to match Docker bridge stack.
 *                        - Fixed Amazon Auth Proxy Link port to 4001 (PROXY_PORT).
 *                        - Aligned getBridgePort() dynamic fallback to 4000.
 *                        - Corrected testBridgeConnection() endpoint targeting to /auth/status.
 *  v1.1.9 (2026-09-09) - Diagnostic Trace & Debug Instrumentation:
 *                        - Added entry, exit, and payload trace/debug loggers across testBridgeConnection(), getDiscoveredDeviceMap(), syncChildDevices(), syncDeviceStates(), and sendBridgeCommand().
 *                        - Enforced structured transaction tracing for HTTP dispatch cycles.
 *  v1.1.8 (2026-09-09) - Closure Return Scope Fix:
 *                        - Fixed testBridgeConnection() returning false due to inner httpGet closure return scope.
 *                        - Enforced local variable assignment (connectionValid) inside closure to guarantee accurate UI status card rendering.
 *  v1.1.7 (2026-09-09) - IP/Port Setting Fallback Hardening:
 *                        - Resolved settings population desync on page render by introducing getBridgeIp() and getBridgePort() dynamic fallback resolvers.
 *                        - Fixed false "Offline / Auth Required" banner rendering during dynamic page loading.
 *  v1.1.6 (2026-09-09) - Network Parsing & Protocol Hardening:
 *                        - Added ignoreSSLIssues: true, application/json header enforcing, and explicit HttpResponseException handlers to httpGet/httpPostJson calls.
 *                        - Bypassed bridgeConnected block during sendBridgeCommand() to prevent stale flag locks.
 *  v1.1.5 (2026-09-09) - Applied Master App Template Alignment based on Mode Manager Advanced v1.1.73:
 *                        - Standardized App Title Banner, Active Status Card, Category Banners, and Collapsible Sections.
 *                        - Integrated single-shot version demarcation trace logging helper checkAndLogVersionDemarcation().
 *                        - Integrated captureSettingsSnapshot() to guard against unnecessary state re-evaluations.
 *                        - Standardized app label badging, centralized logging engine, and auto-debug disabler routine.
 *  v1.1.4 (2026-09-09) - Added bridge health verification and synchronized device state polling schedules.
 *  v1.1.3 (2026-09-09) - Merged expanded device map, filtered out blocked/ignored devices, and added category visibility switches for discovery list.
 *  v1.1.2 (2026-09-09) - Integrated legacy deviceStyleMap lookup table to translate Amazon device type IDs into friendly style names and mapped icon filenames.
 *  v1.1.1 (2026-09-09) - Added custom icon and image source URL host location setting to bridgeConfigPage().
 *  v1.1.0 (2026-09-09) - Appended application version directly into definition name / app instance label display.
 *  v1.0.9 (2026-09-09) - Added [INSTALLED] / [NEW] discovery device labels and user-configurable device label prefix options.
 *  v1.0.8 (2026-09-09) - Added Amazon re-authentication proxy link helper section to bridgeConfigPage().
 *  v1.0.7 (2026-09-09) - Standardized trace log version update banner demarcation on app state updates.
 *  v1.0.6 (2026-09-09) - Enhanced HTTP error tracing and response payload logging for HTTP 500 diagnostic visibility.
 *  v1.0.5 (2026-09-09) - Expanded handleDiscoveryResponse() payload dispatch to forward extended attributes (firmwareVer, followUpMode, alexaWakeWord, alarmVolume, permissions, lastVoiceActivity) to child drivers.
 *  v1.0.4 (2026-09-08) - Standardized naming to EchosSpeak and normalized DNI to raw Amazon Serial Numbers to eliminate bridge routing 404 errors.
 *  v1.0.3 (2026-09-08) - Replaced legacy actionName UI element with native Hubitat submit button to resolve MissingMethodException.
 *  v1.0.2 (2026-09-08) - Added getDiscoveredDeviceMap() helper method and updated handleDiscoveryResponse() to support selective child device creation.
 *  v1.0.1 (2026-09-08) - Fix of iconX URLS for now.
 *  v1.0.0 (2026-09-08) - Initial release of EchosSpeak Advanced (Custom) parent application.
 **/

import java.util.concurrent.ConcurrentHashMap
import groovy.transform.Field

@Field static Object txCounterLock = new Object()
@Field static Long txCounter = 0L

private static String getNextTxId() {
    synchronized(txCounterLock) {
        txCounter++
        return "tx_${new Date().getTime()}_${txCounter}"
    }
}

static String version() { return '1.4.1' }
def timeStamp() { return "2026/09/14 04:35 PM" }

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
            
            Map authData = fetchAuthStatusData()
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

            input name: "catIncludeEchos", type: "bool", title: "<b>Category 1: Standard Echo & Dot Hardware (ECHO)</b>", defaultValue: true, submitOnChange: true, width: 6
            input name: "catIncludeShows", type: "bool", title: "<b>Category 2: Echo Show & Screen Hardware (KNIGHT / SHOW)</b>", defaultValue: true, submitOnChange: true, width: 6
            input name: "catIncludeFireTV", type: "bool", title: "<b>Category 3: Fire TV & Media Streaming Devices (FIRE_TV / AFT)</b>", defaultValue: true, submitOnChange: true, width: 6
            input name: "catIncludeTablets", type: "bool", title: "<b>Category 4: Fire Tablets (TABLET)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catIncludeAuto", type: "bool", title: "<b>Category 5: Echo Auto & Mobile Accessories (AUTO)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catInclude3rdParty", type: "bool", title: "<b>Category 6: Third-Party Alexa Speakers (3RD_PARTY)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catIncludeWHA", type: "bool", title: "<b>Category 7: Multi-Room Audio Groups (WHA)</b>", defaultValue: false, submitOnChange: true, width: 6
            input name: "catIncludeApps", type: "bool", title: "<b>Category 8: Alexa Apps & Software Endpoints (REAVER / APP)</b>", defaultValue: false, submitOnChange: true, width: 6
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

        /* CATEGORY C: POLLING & REFRESH SCHEDULES */
        section("⏱️ <b>CATEGORY C: POLLING & REFRESH SCHEDULES</b>", hideable: true, hidden: startCollapsed) {
            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 4: Background Refresh & State Sync Engine</b>" +
                      "<div style='color:#555; font-size:12px; margin-top:4px; margin-bottom:8px;'>" +
                      "Configure background synchronization interval for pulling volume and availability states.</div></div>"

            input name: "refreshInterval", type: "enum", title: "<b>Device State Polling Interval</b>", 
                  options: ["1": "Every Minute", "5": "Every 5 Minutes", "10": "Every 10 Minutes", "15": "Every 15 Minutes", "30": "Every 30 Minutes", "0": "Disabled"], 
                  defaultValue: "5", required: true
        }

        /* CATEGORY D: DIAGNOSTICS & SYSTEM OPTIONS */
        section("⚙️ <b>CATEGORY D: DIAGNOSTICS & APP PREFERENCES</b>", hideable: true, hidden: startCollapsed) {
            paragraph "<div style='border-left:3px solid #34495E; padding-left:10px; margin-left:5px;'>" +
                      "<b>SECTION 5: System Diagnostics & Logging Options</b></div>"

            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            input name: "showCountInLabel", type: "bool", title: "Show Active Device Count in App Label?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
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
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    state.lastSettingsJson = state.currentSettingsJson
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

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

private Map fetchAuthStatusData() {
    String url = "http://${getBridgeIp()}:${getBridgePort()}/auth/status"
    logTrace "--> fetchAuthStatusData() Initiated -> Targeting URL: [${url}]"

    Map params = [
        uri: url,
        timeout: 5,
        contentType: "application/json",
        ignoreSSLIssues: true
    ]
    
    Map authResult = [:]
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
    
    state.bridgeConnected = (authResult.authenticated == true)

    def statusDev = getChildDevice("ECHOS_SPEAK_SERVER_STATUS") ?: getChildDevices()?.find { it.typeName == "Echos Speak Server Status Tile Driver" }
    if (statusDev) {
        statusDev.parseServerStatusData(authResult)
    }

    return authResult
}

private Boolean testBridgeConnection() {
    Map authData = fetchAuthStatusData()
    return (authData.authenticated == true)
}

void sendBridgeCommand(String serialNumber, String command, Map payload = [:]) {
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

    try {
        httpPostJson(params) { resp ->
            logDebug "<-- sendBridgeCommand() HTTP Response Code: [${resp.status}]"
            if (resp.status == 200) {
                logDebug "Command '${command}' completed successfully for device [${serialNumber}]"
            } else {
                logError "Command '${command}' failed for device [${serialNumber}] -> HTTP Status [${resp.status}]"
            }
        }
    } catch (Exception e) {
        logError "HTTP Post error sending command '${command}' to device [${serialNumber}]: ${e.message}"
    }
    logTrace "<-- sendBridgeCommand() Exit"
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
        logInfo "Refreshing discovered device map from bridge..."
        getDiscoveredDeviceMap()
    } else if (btn == "btnTestBridge") {
        logInfo "Executing Bridge REST API ping test..."
        String url = "http://${getBridgeIp()}:${getBridgePort()}/api/devices"
        try {
            httpGet([uri: url, timeout: 5, contentType: "application/json", ignoreSSLIssues: true]) { resp ->
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
        Map authData = fetchAuthStatusData()
        Boolean isAuth = (authData.authenticated == true)
        String authState = authData.state ?: "OFFLINE"
        String lastErr = authData.lastError ?: "None"

        if (isAuth) {
            state.lastWatchdogTestResult = "🟢 <b>Last Tested: ${nowTime}</b> — PASSED: Session State: <b>${authState}</b> | Last Verified: ${authData.lastValidated ? parseIsoTimestamp(authData.lastValidated) : 'Just Now'}."
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

    if (fam == "ECHO") return incEcho
    if (fam in ["KNIGHT", "SHOW"]) return incShow
    if (fam in ["FIRE_TV", "AFT"]) return incFireTV
    if (fam in ["TABLET"]) return incTablets
    if (fam in ["AUTO"]) return incAuto
    if (fam in ["3RD_PARTY", "THIRD_PARTY"]) return inc3rdParty
    if (fam in ["WHA"]) return incWHA
    if (fam in ["REAVER", "APP", "SOFTWARE", "EVERYWHERE"]) return incApps

    return false
}

private Map getDiscoveredDeviceMap() {
    String url = "http://${getBridgeIp()}:${getBridgePort()}/api/devices"
    logTrace "--> getDiscoveredDeviceMap() Initiated -> Targeting URL: [${url}]"

    Map devices = [:]
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
                    String name = dev.accountName ?: "Echo Device (${sNum})"

                    if (sNum) {
                        cache[sNum] = [
                            accountName: name,
                            deviceFamily: fam,
                            deviceType: dev.deviceType?.toString() ?: "UNKNOWN"
                        ]

                        if (isFamilyAllowed(fam)) {
                            devices[sNum] = "${name} (${fam})"
                        }
                    }
                }
                state.discoveredDeviceCache = cache
            }
        }
    } catch (Exception e) {
        logError "Failed to fetch device map from bridge: ${e.message}"
        state.bridgeConnected = false
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
            Map devData = state.discoveredDeviceCache?.get(dni)
            String devName = devData?.accountName ?: "Echo Device (${dni})"
            logInfo "Creating child device: ${devName} [${dni}]"
            addChildDevice("jshimota", "Echos Speak Advanced Device (Custom)", dni, [label: devName, isComponent: false])
        }
    }

    syncDeviceStates()
}

def syncDeviceStates() {
    String url = "http://${getBridgeIp()}:${getBridgePort()}/api/devices"
    Map params = [
        uri: url,
        timeout: 10,
        contentType: "application/json",
        ignoreSSLIssues: true
    ]

    try {
        httpGet(params) { resp ->
            if (resp.status == 200 && resp.data?.devices) {
                state.bridgeConnected = true
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
        state.bridgeConnected = false
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