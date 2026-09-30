/**
 * Echos Speak Advanced (Custom)
 * Smart Application for Hubitat Elevation
 *
 * Purpose:
 * Central parent application managing connection to local Echo API bridge,
 * discovering Amazon Echo hardware devices, and maintaining child devices.
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
  * v1.0.8    09/09/26    jshimota    Added Amazon re-authentication proxy link helper section to bridgeConfigPage().
  * v1.0.7    09/09/26    jshimota    Standardized trace log version update banner demarcation on app state updates.
  * v1.0.6    09/09/26    jshimota    Enhanced HTTP error tracing and response payload logging for HTTP 500 diagnostic visibility.
  * v1.0.5    09/09/26    jshimota    Expanded handleDiscoveryResponse() payload dispatch to forward extended attributes (firmwareVer, followUpMode, alexaWakeWord, alarmVolume, permissions, lastVoiceActivity) to child drivers.
  * v1.0.4    09/08/26    jshimota    Standardized naming to EchosSpeak and normalized DNI to raw Amazon Serial Numbers to eliminate bridge routing 404 errors.
  * v1.0.3    09/08/26    jshimota    Replaced legacy actionName UI element with native Hubitat submit button to resolve MissingMethodException.
  * v1.0.2    09/08/26    jshimota    Added getDiscoveredDeviceMap() helper method and updated handleDiscoveryResponse() to support selective child device creation.
  * v1.0.1    09/08/26    jshimota    Fix of iconX URLS for now.
  * v1.0.0    09/08/26    jshimota    Initial release of EchosSpeak Advanced (Custom) parent application.
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.0.8' }
def timeStamp() { return "2026/09/09 10:10 AM" }

import groovy.transform.Field

definition(
    name: "Echos Speak Advanced (Custom)",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Parent app for EchosSpeak Advanced. Controls local proxy connectivity, device discovery, and state sync.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/echo_speaks_3.1x.png",
    iconX2Url: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/echo_speaks_3.2x.png",
    iconX3Url: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/echo_speaks_3.3x.png",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/echos-speak-advanced/echos-speak-advanced_custom.groovy"
)

preferences {
    page(name: "mainPage")
    page(name: "bridgeConfigPage")
    page(name: "deviceDiscoveryPage")
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

/* =========================================================================================
   UI PAGES & CONFIGURATION LAYOUT
   ========================================================================================= */

def mainPage() {
    checkAndLogVersionDemarcation()
    dynamicPage(name: "mainPage", title: "Echos Speak Advanced (Custom) v${version()}", install: true, uninstall: true) {
        section("<b>Local API Bridge Connection</b>") {
            String statusText = state.bridgeConnected ? "<font color='green'><b>Connected</b></font>" : "<font color='red'><b>Disconnected</b></font>"
            paragraph "Bridge Status: ${statusText}"
            href name: "toBridgeConfig", page: "bridgeConfigPage", title: "Configure API Bridge Settings", description: "Set Bridge IP address and port"
        }

        section("<b>Device Management</b>") {
            href name: "toDeviceDiscovery", page: "deviceDiscoveryPage", title: "Discover & Sync Echo Devices", description: "Scan Amazon account via Bridge and update child devices"
        }

        section("<b>Application Logging Options</b>") {
            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: true, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: true, required: true
        }
    }
}

def bridgeConfigPage() {
    checkAndLogVersionDemarcation()
    dynamicPage(name: "bridgeConfigPage", title: "Bridge Configuration", nextPage: "mainPage") {
        section("<b>Local Node.js / Docker Server</b>") {
            input name: "bridgeIp", type: "string", title: "Bridge IP Address", required: true, defaultValue: "192.168.1.12"
            input name: "bridgePort", type: "string", title: "Bridge REST Port", required: true, defaultValue: "8093"
            input name: "proxyPort", type: "string", title: "Bridge Amazon Auth Proxy Port", required: true, defaultValue: "8094"
        }

        section("<b>Amazon Account Authentication</b>") {
            paragraph "If your bridge reports <i>'Cookie invalid, Renew unsuccessful'</i> or HTTP 500 errors during discovery, click the button below to re-authenticate with Amazon:"
            
            String proxyUrl = "http://${settings.bridgeIp ?: '192.168.1.12'}:${settings.proxyPort ?: '8094'}"
            paragraph "<a href='${proxyUrl}' target='_blank' style='background-color:#007185;color:white;padding:8px 12px;text-decoration:none;border-radius:4px;display:inline-block;font-weight:bold;'>🔐 Open Amazon Proxy Login (${proxyUrl})</a>"
        }
    }
}

def deviceDiscoveryPage() {
    checkAndLogVersionDemarcation()
    dynamicPage(name: "deviceDiscoveryPage", title: "Device Discovery", nextPage: "mainPage") {
        section("<b>Select Echo Devices</b>") {
            paragraph "Select only the devices you want to manage with Echos Speak Advanced."
            input name: "selectedDevices", type: "enum", title: "Devices to Import", options: getDiscoveredDeviceMap(), multiple: true, required: false, submitOnChange: true
            input name: "btnSaveDiscovery", type: "button", title: "Save & Create Selected Child Devices"
        }
    }
}

void appButtonHandler(String btn) {
    checkAndLogVersionDemarcation()
    if (btn == "btnSaveDiscovery") {
        executeDeviceDiscovery()
    }
}

Map getDiscoveredDeviceMap() {
    checkAndLogVersionDemarcation()
    Map devMap = [:]
    if (!settings.bridgeIp || !settings.bridgePort) return devMap

    String targetUrl = "${getBridgeUrl()}/api/devices"
    logTrace "getDiscoveredDeviceMap(): Fetching devices synchronous from ${targetUrl}"

    try {
        httpGet([uri: targetUrl, timeout: 10]) { resp ->
            logTrace "getDiscoveredDeviceMap(): HTTP Status=${resp.status}"
            if (resp.data && resp.data.devices) {
                resp.data.devices.each { dev ->
                    devMap[dev.serialNumber] = "${dev.accountName} (${dev.deviceFamily ?: 'Echo'})"
                }
            } else {
                logDebug "getDiscoveredDeviceMap(): Bridge returned response without devices array: ${resp.data}"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        String errorResponseBody = e.response?.data ? e.response.data.toString() : "No body text"
        logError "Failed to fetch device map from bridge [HTTP ${e.statusCode}]: ${e.message} | Response Body: ${errorResponseBody}"
    } catch (Exception e) {
        logError "Failed to fetch device map from bridge (General Exception): ${e.class.name} - ${e.message}"
    }
    return devMap
}

/* =========================================================================================
   HUBITAT APP LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing Echos Speak Advanced (Custom) v${version()}..."
    initialize()
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating preferences for Echos Speak Advanced (Custom)..."
    initialize()
}

private void initialize() {
    checkAndLogVersionDemarcation()
    unschedule()
    
    // Poll bridge every 5 minutes for state/IP updates
    schedule("0 */5 * ? * * *", "syncDeviceStates")
    
    // Initial connection test
    testBridgeConnection()
}

/* =========================================================================================
   BRIDGE COMMUNICATIONS & COMMAND DISPATCH
   ========================================================================================= */

private String getBridgeUrl() {
    return "http://${settings.bridgeIp}:${settings.bridgePort}"
}

void testBridgeConnection() {
    checkAndLogVersionDemarcation()
    if (!settings.bridgeIp || !settings.bridgePort) return
    
    String targetUrl = "${getBridgeUrl()}/status"
    logTrace "testBridgeConnection(): Testing endpoint ${targetUrl}"

    asynchttpGet("handleConnectionResponse", [
        uri: targetUrl,
        timeout: 10
    ])
}

void handleConnectionResponse(response, data) {
    checkAndLogVersionDemarcation()
    if (response.hasError()) {
        logError "Bridge Connection Failed [HTTP ${response.getStatus()}]: ${response.getErrorMessage()} | Body: ${response.getErrorData()}"
        state.bridgeConnected = false
    } else {
        logInfo "Local API Bridge connection verified."
        state.bridgeConnected = true
    }
}

void executeDeviceDiscovery() {
    checkAndLogVersionDemarcation()
    String targetUrl = "${getBridgeUrl()}/api/devices"
    logInfo "Executing Echo device discovery request to ${targetUrl}..."
    
    asynchttpGet("handleDiscoveryResponse", [
        uri: targetUrl,
        timeout: 15
    ])
}

void handleDiscoveryResponse(response, data) {
    checkAndLogVersionDemarcation()
    logTrace "handleDiscoveryResponse(): Received HTTP ${response.getStatus()}"

    if (response.hasError()) {
        String rawErrorBody = response.getErrorData() ?: "No error data body"
        logError "Device discovery failed [HTTP ${response.getStatus()}]: ${response.getErrorMessage()} | Detailed Bridge Error: ${rawErrorBody}"
        return
    }

    Map json = response.getJson()
    logTrace "handleDiscoveryResponse(): Parsed JSON: ${json}"

    if (!json || !json.devices) {
        logWarn "No devices returned from bridge. Response body: ${response.getData()}"
        return
    }

    List<String> chosenSerials = settings.selectedDevices ? (settings.selectedDevices instanceof List ? settings.selectedDevices : [settings.selectedDevices]) : []

    json.devices.each { dev ->
        if (chosenSerials.contains(dev.serialNumber)) {
            String dni = dev.serialNumber
            def child = getChildDevice(dni)
            
            if (!child) {
                logInfo "Creating child device: ${dev.accountName} (${dni})"
                child = addChildDevice(
                    "jshimota", 
                    "Echos Speak Advanced Device (Custom)", 
                    dni, 
                    [name: dev.accountName, label: "Echo - ${dev.accountName}"]
                )
            }

            // Push ALL extended attributes to child driver
            child.parseDeviceData([
                ipAddress: dev.ipAddress,
                macAddress: dev.macAddress,
                deviceFamily: dev.deviceFamily,
                deviceStyle: dev.deviceStyle,
                deviceImageUrl: dev.deviceImageUrl,
                online: dev.online,
                firmwareVer: dev.firmwareVer,
                followUpMode: dev.followUpMode,
                alexaWakeWord: dev.alexaWakeWord,
                alarmVolume: dev.alarmVolume,
                permissions: dev.permissions,
                lastVoiceActivity: dev.lastVoiceActivity
            ])
        }
    }
}

void syncDeviceStates() {
    checkAndLogVersionDemarcation()
    if (!state.bridgeConnected) testBridgeConnection()
    executeDeviceDiscovery()
}

void sendBridgeCommand(String deviceDni, String commandType, Map commandPayload = [:]) {
    checkAndLogVersionDemarcation()
    String serialNumber = deviceDni.trim()
    
    Map bodyData = [
        serialNumber: serialNumber,
        command: commandType,
        payload: commandPayload
    ]

    logDebug "Sending Bridge Command (${commandType}) for ${serialNumber}: ${commandPayload}"

    asynchttpPost("handleCommandResponse", [
        uri: "${getBridgeUrl()}/api/command",
        requestContentType: "application/json",
        contentType: "application/json",
        body: groovy.json.JsonOutput.toJson(bodyData),
        timeout: 10
    ], [dni: deviceDni, command: commandType])
}

void handleCommandResponse(response, data) {
    checkAndLogVersionDemarcation()
    if (response.hasError()) {
        logError "Command failed for ${data.dni} (${data.command}) [HTTP ${response.getStatus()}]: ${response.getErrorMessage()} | Body: ${response.getErrorData()}"
    } else {
        logDebug "Command ${data.command} completed successfully for ${data.dni}"
    }
}

void refreshDeviceData(String deviceDni) {
    checkAndLogVersionDemarcation()
    syncDeviceStates()
}

/* =========================================================================================
   LOGGING ENGINE
   ========================================================================================= */

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (settings[settingKey] != null ? settings[settingKey] as Boolean : defaultEnabled) {
        log."${lowerLevel}" "Echos Speak Advanced App: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }