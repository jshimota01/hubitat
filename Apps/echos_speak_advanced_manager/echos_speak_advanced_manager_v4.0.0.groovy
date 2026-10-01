/**
 * Echos Speak Advanced Manager
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
 *  Parent Application for managing Echos Speak integration with local Node.js / Docker bridge running on Synology NAS.
 *  Maintains the authoritative registry of physical Echo child devices for downstream policy managers.
 *
 *  Changelog:
 *  v4.0.0 (2026-09-30) - Version update to 4.0.0.
 *  v3.9.2 (2026-09-30) - Display dynamic version suffix in Hubitat Apps list GUI label.
 *  v3.9.1 (2026-09-30) - Delegation Expansion:
 *                          - Added public refreshManagedDeviceFleet() proxy method delegating to Echos Speak Device Manager.
 *  v3.0.1 (2026-09-28) - Basic update to cleanup interface and Child feed
 *  v3.0.0 (2026-09-26) - Application Renaming & Driver Standardization.
 *
 *				[KEEP] Prior change history is found in changelog_parent.txt if any
 **/

static String version() { return '4.0.0' }
def timeStamp() { return "2026/09/30 11:50 AM" }

definition(
    name: "Echos Speak Advanced Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Top-level application container for Echos Speak Advanced devices, bridge management, and environmental policy engines.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: true,
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/echos_speak_advanced_manager/echos_speak_advanced_manager.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    updateAppLabel()
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:22px; font-weight:600;'>Echos Speak Advanced Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Ecosystem Container Parent v${currentVersion} (${timeStamp()})</span></div>"
        }

        section("📱 <b>ECOSYSTEM APPLICATION BRANCHES</b>") {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:10px;'>" +
                      "Manage device discovery, Synology REST bridge connectivity, and environmental policy engines from this central container.</div>"

            app(name: "echosSpeakDeviceManagerApp", appName: "Echos Speak Device Manager", namespace: "jshimota", title: "<b>Configure Echos Speak Device Manager (Bridge & Devices)</b>", multiple: false)
            app(name: "echosSpeakEnvironmentApp", appName: "Echos Speak Environment Manager", namespace: "jshimota", title: "<b>Configure Echos Speak Environment Manager (Policies & Rules)</b>", multiple: true)
        }

        section("📊 <b>ECOSYSTEM STATUS & REGISTRY HEALTH</b>") {
            Map physReg = getManagedPhysicalEchoDevices()
            int childCount = physReg ? physReg.size() : 0

            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #2980B9; padding:10px; border-radius:4px; font-size:12px;'>" +
                      "<b>Authoritative Physical Echoes Registered:</b> <span style='color:#27AE60; font-weight:bold;'>${childCount} Device(s)</span><br/>" +
                      "<span style='color:#7F8C8D; font-size:11px;'>WHA audio groups are automatically excluded from the physical registry.</span>" +
                      "</div>"
        }

        section("⚙️ <b>APP PREFERENCES</b>", hideable: true, hidden: true) {
            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
        }
    }
}

private void updateAppLabel() {
    String baseName = "Echos Speak Advanced Manager"
    String expectedLabel = "${baseName} v${version()}"
    if (app.getLabel() != expectedLabel) {
        app.updateLabel(expectedLabel)
    }
}

/* =========================================================================================
   PUBLIC DELEGATION PROXY API FOR CHILD APPLICATIONS
   ========================================================================================= */

private Object getDeviceManagerApp() {
    return getChildApps().find { 
        it.name == "Echos Speak Device Manager" || it.typeName == "Echos Speak Device Manager" ||
        it.name == "Echos Speak Advanced" || it.typeName == "Echos Speak Advanced" 
    }
}

public Map refreshManagedDeviceFleet() {
    def coreApp = getDeviceManagerApp()
    if (coreApp) {
        return coreApp.refreshManagedDeviceFleet() ?: [requested: 0, issued: 0, failed: 0]
    }
    logWarn "refreshManagedDeviceFleet(): 'Echos Speak Device Manager' child app instance not found!"
    return [requested: 0, issued: 0, failed: 0]
}

public Map getManagedPhysicalEchoDevices() {
    def coreApp = getDeviceManagerApp()
    if (coreApp) {
        return coreApp.getManagedPhysicalEchoDevices() ?: [:]
    }
    logWarn "getManagedPhysicalEchoDevices(): 'Echos Speak Device Manager' child app instance not found!"
    return [:]
}

public Map getManagedEchoDeviceInventory() {
    def coreApp = getDeviceManagerApp()
    if (coreApp) {
        return coreApp.getManagedEchoDeviceInventory() ?: [:]
    }
    logWarn "getManagedEchoDeviceInventory(): 'Echos Speak Device Manager' child app instance not found!"
    return [:]
}

public Object getManagedChildDevice(String dni) {
    if (!dni) return null
    def coreApp = getDeviceManagerApp()
    if (coreApp) {
        return coreApp.getChildDevice(dni)
    }
    logWarn "getManagedChildDevice(${dni}): 'Echos Speak Device Manager' child app instance not found!"
    return null
}

void installed() {
    logInfo "Installing Echos Speak Advanced Manager v${version()} (${timeStamp()})..."
    initialize()
}

void updated() {
    logInfo "Updating Echos Speak Advanced Manager..."
    initialize()
}

void uninstalled() {
    logInfo "Uninstalling Echos Speak Advanced Manager..."
}

private void initialize() {
    updateAppLabel()
    logInfo "Echos Speak Advanced Manager initialized."
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    if (settings["log${lowerLevel.capitalize()}Enable"] != false) {
        log."${lowerLevel}" "Echos Speak Advanced Manager: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }