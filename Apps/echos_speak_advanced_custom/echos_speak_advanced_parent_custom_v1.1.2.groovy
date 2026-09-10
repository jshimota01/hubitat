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
  * v1.1.2    09/09/26    jshimota    Integrated legacy deviceStyleMap lookup table to translate Amazon device type IDs into friendly style names and mapped icon filenames.
  * v1.1.1    09/09/26    jshimota    Added custom icon and image source URL host location setting to bridgeConfigPage().
  * v1.1.0    09/09/26    jshimota    Appended application version directly into definition name / app instance label display.
  * v1.0.9    09/09/26    jshimota    Added [INSTALLED] / [NEW] discovery device labels and user-configurable device label prefix options.
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

static String version() { return '1.1.2' }
def timeStamp() { return "2026/09/09 10:40 AM" }

import groovy.transform.Field

@Field static final Map<String, Map<String, String>> DEVICE_STYLE_MAP = [
    "AK2113167O4WB": [name: "Echo Studio", icon: "echo_studio.png", family: "ECHO"],
    "A2DS1Q2TPDJ48U": [name: "Echo Dot Clock (Gen5)", icon: "echo_dot_gen5_clock.png", family: "ECHO"],
    "A15ER2A7M23205": [name: "Echo Dot (Gen5)", icon: "echo_dot_gen5.png", family: "ECHO"],
    "A3S5L0B0E9TNV7": [name: "Echo Show 8 (Gen2)", icon: "echo_show_8.png", family: "KNIGHT"],
    "A1RABVCI4QCIKC": [name: "Echo Dot (Gen3)", icon: "echo_dot_gen3.png", family: "ECHO"],
    "A2E01BBT3V3OH3": [name: "Echo Show 5", icon: "echo_show_5.png", family: "KNIGHT"],
    "A1805IZSGTT6HS": [name: "Echo Show 10", icon: "echo_show_10.png", family: "KNIGHT"],
    "A7WX21B5GJY0M":  [name: "Echo Flex", icon: "echo_flex.png", family: "ECHO"],
    "A3C2A21513F37E": [name: "Echo Plus (Gen2)", icon: "echo_plus_gen2.png", family: "ECHO"],
    "ADVBD696BHNV5":  [name: "Echo Show 15", icon: "echo_show_15.png", family: "KNIGHT"],
    "A2IVLV5VM2W81":  [name: "Echo Input", icon: "echo_input.png", family: "ECHO"],
    "A3282YD29XC27I": [name: "Echo Connect", icon: "echo_connect.png", family: "ECHO"]
]

definition(
    name: "Echos Speak Advanced (Custom) (v${version()})",
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

private void updateAppLabelVersion() {
    String desiredLabel = "Echos Speak Advanced (Custom) (v${version()})"
    if (app.label != desiredLabel) {
        logInfo "Updating application instance label to: '${desiredLabel}'"
        app.updateLabel(desiredLabel)
    }
}

/* =========================================================================================
   UI PAGES & CONFIGURATION LAYOUT
   ========================================================================================= */

def mainPage() {
    checkAndLogVersionDemarcation()
    updateAppLabelVersion()

    dynamicPage(name: "mainPage", title: "Echos Speak Advanced (Custom) v${version()}", install: true, uninstall: true) {
        section("<b>Local API Bridge Connection</b>") {
            String statusText = state.bridgeConnected ? "<font color='green'><b>Connected</b></font>" : "<font color='red'><b>Disconnected</b></font>"
            paragraph "Bridge Status: ${statusText}"
            href name: "toBridgeConfig", page: "bridgeConfigPage", title: "Configure API Bridge Settings", description: "Set Bridge IP address, port, and icon asset URL"
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
    dynamicPage(name: "bridgeConfigPage", title: "Bridge & Asset Configuration", nextPage: "mainPage") {
        section("<b>Local Node.js / Docker Server</b>") {
            input name: "bridgeIp", type: "string", title: "Bridge IP Address", required: true, defaultValue: "192.168.1.12"
            input name: "bridgePort", type: "string", title: "Bridge REST Port", required: true, defaultValue: "8093"
            input name: "proxyPort", type: "string", title: "Bridge Amazon Auth Proxy Port", required: true, defaultValue: "8094"
        }

        section("<b>Custom Icon & Image Asset Host</b>") {
            paragraph "Base URL location used for device icons and media tiles when creating or updating devices:"
            input name: "imageSourceUrl", type: "string", title: "Custom Icon Source Base URL", required: false, defaultValue: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/"
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
        section("<b>Device Label Naming Options</b>") {
            input name: "enableDevicePrefix", type: "bool", title: "Add Prefix to Device Labels?", defaultValue: true, submitOnChange: true
            if (settings.enableDevicePrefix != false) {
                input name: "devicePrefixText", type: "string", title: "Custom Prefix Text", defaultValue: "Echo - ", required: false
            }
        }

        section("<b>Select Echo Devices</b>") {
            paragraph "Devices marked as <b>[INSTALLED]</b> already exist on your hub. Unchecked or new devices show as <b>[NEW]</b>."
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

    Set<String> installedDnis = getChildDevices()*.deviceNetworkId as Set

    try {
        httpGet([uri: targetUrl, timeout: 10]) { resp ->
            logTrace "getDiscoveredDeviceMap(): HTTP Status=${resp.status}"
            if (resp.data && resp.data.devices) {
                resp.data.devices.each { dev ->
                    Boolean isInstalled = installedDnis.contains(dev.serialNumber)
                    String statusTag = isInstalled ? "[INSTALLED]" : "[NEW]"
                    
                    // Lookup style metadata from DEVICE_STYLE_MAP
                    Map styleMeta = DEVICE_STYLE_MAP[dev.deviceStyle]
                    String friendlyModel = styleMeta ? styleMeta.name : (dev.deviceFamily ?: 'Echo Device')

                    devMap[dev.serialNumber] = "${statusTag} ${dev.accountName} (${friendlyModel})"
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
    updateAppLabelVersion()
    logInfo "Installing Echos Speak Advanced (Custom) v${version()}..."
    initialize()
}

void updated() {
    checkAndLogVersionDemarcation()
    updateAppLabelVersion()
    logInfo "Updating preferences for Echos Speak Advanced (Custom)..."
    initialize()
}

private void initialize() {
    checkAndLogVersionDemarcation()
    updateAppLabelVersion()
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

    // Calculate dynamic label prefix based on user preferences
    Boolean usePrefix = settings.enableDevicePrefix != false
    String prefix = usePrefix ? (settings.devicePrefixText != null ? settings.devicePrefixText : "Echo - ") : ""
    
    // Determine configured image source base path
    String baseImgUrl = settings.imageSourceUrl ?: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/"
    if (!baseImgUrl.endsWith("/")) baseImgUrl += "/"

    json.devices.each { dev ->
        if (chosenSerials.contains(dev.serialNumber)) {
            String dni = dev.serialNumber
            String computedLabel = "${prefix}${dev.accountName}".trim()
            def child = getChildDevice(dni)
            
            // Resolve model style name and icon file from DEVICE_STYLE_MAP
            Map styleMeta = DEVICE_STYLE_MAP[dev.deviceStyle]
            String resolvedStyle = styleMeta ? styleMeta.name : (dev.deviceStyle ?: "Echo Device")
            String iconFile = styleMeta ? styleMeta.icon : "echo_gen1.png"
            String finalIconUrl = "${baseImgUrl}${iconFile}"

            if (!child) {
                logInfo "Creating child device: ${computedLabel} (${dni})"
                child = addChildDevice(
                    "jshimota", 
                    "Echos Speak Advanced Device (Custom)", 
                    dni, 
                    [name: dev.accountName, label: computedLabel]
                )
            } else {
                if (child.label != computedLabel) {
                    logInfo "Updating label for device ${dni} to '${computedLabel}'"
                    child.setLabel(computedLabel)
                }
            }

            // Push ALL extended attributes to child driver
            child.parseDeviceData([
                ipAddress: dev.ipAddress,
                macAddress: dev.macAddress,
                deviceFamily: dev.deviceFamily ?: (styleMeta ? styleMeta.family : "ECHO"),
                deviceStyle: resolvedStyle,
                deviceImageUrl: finalIconUrl,
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