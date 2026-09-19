/**
 * WakeOnLan Win10 Instance (Custom)
 * Platform: Hubitat Elevation
 * Notes: Child app instance representing a individual WakeOnLan target workstation.
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
 *  Changelog:
 *  v1.1.0   09/17/26   jshimota   Initial child app release for Parent/Child App architecture.
 **/

static String version() { return '1.1.0' }
def timeStamp() { return "2026/09/17 09:00 AM" }

definition(
    name: "WakeOnLan Win10 Instance (Custom)",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Child app instance for WakeOnLan targets.",
    category: "Utility",
    parent: "jshimota:WakeOnLan Win10 (Custom)",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Configure WakeOnLan Target", install: true, uninstall: true) {
        section("<b>Device Details</b>") {
            label title: "Target Device Name", required: true
            input "configureMACAddress", "text", title: "MAC Address", description: "Delimiters ':' and '-' will be automatically stripped", required: true
            input "configureIP", "text", title: "IP Address", description: "Default: 255.255.255.255 (Subnet Broadcast)", required: false, defaultValue: "255.255.255.255"
            input "configurePort", "number", title: "UDP Port", description: "Default: 9", required: false, defaultValue: 9
            input "configureUseSecureOn", "bool", title: "Use SecureOn Password?", required: false, defaultValue: false
            input "configureSecureOn", "text", title: "SecureOn Password", description: "12-character hex password (optional)", required: false
        }
    }
}

void installed() {
    logInfo "Installing Child Instance '${app.label}' v${version()}..."
    initialize()
}

void updated() {
    logInfo "Updating Child Instance '${app.label}'..."
    initialize()
}

void uninstalled() {
    logInfo "Uninstalling Child Instance '${app.label}'..."
    deleteChildDevice(state.childDni)
}

private String normalizeMac(String mac) {
    if (!mac) return null
    String normalized = mac.replaceAll(/[^0-9A-Fa-f]/, '').toUpperCase()
    return (normalized ==~ /[0-9A-F]{12}/) ? normalized : null
}

private void initialize() {
    String cleanMac = normalizeMac(configureMACAddress)
    if (!cleanMac) {
        logError "Invalid MAC address '${configureMACAddress}'. Cannot sync device settings."
        return
    }

    Integer port = configurePort ? configurePort.toInteger() : 9
    String targetIp = configureIP ?: "255.255.255.255"
    Boolean useSecOn = (configureUseSecureOn != null) ? configureUseSecureOn : false

    String dni = "WOL_CHILD_" + app.id
    state.childDni = dni

    def childDev = getChildDevice(dni)
    if (!childDev) {
        try {
            childDev = addChildDevice(
                "jshimota",
                "WakeOnLan Win10 (Custom)",
                dni,
                [label: app.label, isComponent: true]
            )
            logInfo "Created child device '${app.label}' [${dni}]"
        } catch (Exception e) {
            logError "Failed to create child device: ${e.message}"
            return
        }
    } else {
        childDev.label = app.label
    }

    if (childDev) {
        childDev.updateSetting("myMac", [type: "text", value: cleanMac])
        childDev.updateSetting("myPort", [type: "number", value: port])
        childDev.updateSetting("myIP", [type: "text", value: targetIp])
        childDev.updateSetting("useSecureOn", [type: "bool", value: useSecOn])
        childDev.updateSetting("mySecureOn", [type: "text", value: configureSecureOn ?: ""])
        childDev.updated()
    }
}

private void logInfo(String msg)  { log.info "${app.label}: ${msg}" }
private void logError(String msg) { log.error "${app.label}: ${msg}" }