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
  * v1.0.0    09/08/26    jshimota    Initial release of Echos Speak Advanced (Custom) parent application.
  * v1.0.1	  09/08/26	  jshimota	  FIx of iconX URLS for now.
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.0.1' }
def timeStamp() { return "2026/09/08 09:30 AM" }

import groovy.transform.Field

definition(
    name: "Echos Speak Advanced (Custom)",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Parent app for Echos Speak Advanced. Controls local proxy connectivity, device discovery, and state sync.",
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

/* =========================================================================================
   UI PAGES & CONFIGURATION LAYOUT
   ========================================================================================= */

def mainPage() {
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
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }
    }
}

def bridgeConfigPage() {
    dynamicPage(name: "bridgeConfigPage", title: "Bridge Configuration", nextPage: "mainPage") {
        section("<b>Local Node.js / Docker Server</b>") {
            input name: "bridgeIp", type: "string", title: "Bridge IP Address", required: true, defaultValue: "192.168.1.X"
            input name: "bridgePort", type: "string", title: "Bridge REST Port", required: true, defaultValue: "8091"
        }
    }
}

def deviceDiscoveryPage() {
    dynamicPage(name: "deviceDiscoveryPage", title: "Device Discovery", nextPage: "mainPage") {
        section("<b>Amazon Echo Devices</b>") {
            paragraph "Click below to query your Amazon account via the local proxy bridge."
            actionName "executeDeviceDiscovery", title: "Run Device Discovery"
        }
    }
}

/* =========================================================================================
   HUBITAT APP LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    logInfo "Installing Echos Speak Advanced (Custom) v${version()}..."
    initialize()
}

void updated() {
    logInfo "Updating preferences for Echos Speak Advanced (Custom)..."
    initialize()
}

private void initialize() {
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
    if (!settings.bridgeIp || !settings.bridgePort) return
    
    asynchttpGet("handleConnectionResponse", [
        uri: "${getBridgeUrl()}/status",
        timeout: 10
    ])
}

void handleConnectionResponse(response, data) {
    if (response.hasError()) {
        logError "Bridge Connection Failed: ${response.getErrorMessage()}"
        state.bridgeConnected = false
    } else {
        logInfo "Local API Bridge connection verified."
        state.bridgeConnected = true
    }
}

void executeDeviceDiscovery() {
    logInfo "Executing Echo device discovery request..."
    
    asynchttpGet("handleDiscoveryResponse", [
        uri: "${getBridgeUrl()}/api/devices",
        timeout: 15
    ])
}

void handleDiscoveryResponse(response, data) {
    if (response.hasError()) {
        logError "Device discovery failed: ${response.getErrorMessage()}"
        return
    }

    Map json = response.getJson()
    if (!json || !json.devices) {
        logWarn "No devices returned from bridge."
        return
    }

    json.devices.each { dev ->
        String dni = "ECHOSPEAK-${dev.serialNumber}"
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

        // Push network and status attributes to child driver
        child.parseDeviceData([
            ipAddress: dev.ipAddress,
            macAddress: dev.macAddress,
            deviceFamily: dev.deviceFamily,
            deviceStyle: dev.deviceStyle,
            deviceImageUrl: dev.deviceImageUrl,
            online: dev.online
        ])
    }
}

void syncDeviceStates() {
    if (!state.bridgeConnected) testBridgeConnection()
    executeDeviceDiscovery()
}

/**
 * Public Dispatcher called by Child Drivers to send HTTP commands to Local Proxy Container
 **/
void sendBridgeCommand(String deviceDni, String commandType, Map commandPayload = [:]) {
    String serialNumber = deviceDni.replace("ECHOSPEAK-", "")
    
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
    if (response.hasError()) {
        logError "Command failed for ${data.dni} (${data.command}): ${response.getErrorMessage()}"
    } else {
        logDebug "Command ${data.command} completed successfully for ${data.dni}"
    }
}

void refreshDeviceData(String deviceDni) {
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