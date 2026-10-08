/**
 * Garage Opener (Custom)
 * Platform: Hubitat Elevation
 * Notes: Bridge app connecting physical relay switch and contact sensors to Virtual Simple Garage Door Controller
 * Category: Convenience
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
 *  Bridges physical garage door relay triggers and dual contact sensors directly into the Virtual Simple Garage Door Controller driver.
 *
 *  Instructions:
 *  1. Select your Virtual Garage Door Controller device under Controls.
 *  2. Select your physical Sonoff GDO Relay switch.
 *  3. Map your physical closed and open contact sensors under Contacts.
 *  
 *  Changelog:
 *  v1.3.1    08/28/26    jshimota    Added subscription to controller driverVersion updates to force instant HTML tile re-renders on driver save
 *  v1.3.0    08/28/26    jshimota    Cleaned up redundant HTML tags in notificationTile, standardized border-radius to 4px, and strictly applied EM font preference units
 *  v1.2.9    08/28/26    jshimota    Integrated driver typography preferences (font sizes, bold options) into HTML tile generator
 *  v1.2.8    08/28/26    jshimota    Made notificationTile responsive with flex layout, title truncation, and zero overflow for 1x1 dashboard tiles
 *  v1.2.7    08/28/26    jshimota    Added tileFormat preference toggle allowing choice between Plain Text and Colorized HTML for notificationTile
 *  v1.2.6    08/28/26    jshimota    Enhanced notificationTile HTML styling with full CSS color badges for sensors, states, and event reasons
 *  v1.2.5    08/28/26    jshimota    Added automated dashboard notificationTile generation pushed directly to controller device
 *  v1.2.4    08/28/26    jshimota    Added colorized live state badge header and dynamic app label formatting
 *  v1.2.3    08/28/26    jshimota    Added HTML status table displaying live states/attributes and integrated app.label for dynamic naming
 *  v1.2.2    08/28/26    jshimota    Added live status summary block pulling driverVersion, door state, and contact attributes from controller device
 *  v1.2.1    08/28/26    jshimota    Removed redundant switch-off and reversal timers; relied on Sonoff driver native pulseOn auto-off
 *  v1.2.0    08/28/26    jshimota    Refactored app to pipe contact sensor events into Virtual Driver commands; applied master template layout
 *  v1.1.1    06/21/26    jshimota    Fixed timeout sensor logic typo, corrected info logging behaviors
 *  v1.1.0    06/21/26    jshimota    Gemini recommendations update
 *  v1.0.0    01/01/20    muxa        Original Source Release
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.3.1' }
def timeStamp() { return "2026/08/28 11:50 AM" }

definition(
    name: "Garage Opener (Custom)",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Control your garage door with a switch and optional contact sensors",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "mainPage", title: "Garage Opener", install: true, uninstall: true)
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        String baseName = app.label ?: "Garage Opener"
        String doorState = garageControl ? (garageControl.currentValue("door")?.toUpperCase() ?: "UNKNOWN") : "NOT CONFIGURED"
        
        // Color mapping for state badge
        String badgeBgColor = "#757575"
        switch(doorState) {
            case "CLOSED":  badgeBgColor = "#2e7d32"; break // Dark Green
            case "OPEN":    badgeBgColor = "#c62828"; break // Dark Red
            case "OPENING": 
            case "CLOSING": badgeBgColor = "#ef6c00"; break // Amber / Gold
            case "UNKNOWN": badgeBgColor = "#6a1b9a"; break // Purple (Stuck/Stalled)
        }

        // Header Section with Live Colorized Badge
        section("""
            <div style='display: flex; align-items: center; justify-content: space-between;'>
                <h2 style='margin:0;'>${baseName}</h2>
                <span style='background-color: ${badgeBgColor}; color: white; padding: 4px 10px; border-radius: 4px; font-weight: bold; font-size: 12px;'>${doorState}</span>
            </div>
        """) {
            // Display Live Status HTML Table if Controller is Selected
            if (garageControl) {
                String closedSensorState = garageControl.currentValue("closedContact") ?: (closedContact ? closedContact.currentValue("contact") : "Not Configured")
                String openSensorState = garageControl.currentValue("openContact") ?: (openContact ? openContact.currentValue("contact") : "Not Configured")
                String relayState = garageSwitch ? garageSwitch.currentValue("switch") : "Not Configured"
                String driverVer = garageControl.currentValue("driverVersion") ?: "v1.2.9"

                paragraph """
                <table style='width:100%; border-collapse: collapse; border: 1px solid #ddd; font-family: sans-serif; font-size: 13px;'>
                    <tr style='background-color: #f2f2f2; text-align: left;'>
                        <th style='padding: 8px; border: 1px solid #ddd;'>Component / Attribute</th>
                        <th style='padding: 8px; border: 1px solid #ddd;'>Device Name</th>
                        <th style='padding: 8px; border: 1px solid #ddd;'>Current Value</th>
                    </tr>
                    <tr>
                        <td style='padding: 8px; border: 1px solid #ddd;'><b>Garage Door State</b></td>
                        <td style='padding: 8px; border: 1px solid #ddd;'>${garageControl.displayName}</td>
                        <td style='padding: 8px; border: 1px solid #ddd; color: ${badgeBgColor}; font-weight: bold;'>${doorState}</td>
                    </tr>
                    <tr>
                        <td style='padding: 8px; border: 1px solid #ddd;'><b>Closed Contact Sensor</b></td>
                        <td style='padding: 8px; border: 1px solid #ddd;'>${closedContact ? closedContact.displayName : 'None'}</td>
                        <td style='padding: 8px; border: 1px solid #ddd;'>${closedSensorState}</td>
                    </tr>
                    <tr>
                        <td style='padding: 8px; border: 1px solid #ddd;'><b>Open Contact Sensor</b></td>
                        <td style='padding: 8px; border: 1px solid #ddd;'>${openContact ? openContact.displayName : 'None'}</td>
                        <td style='padding: 8px; border: 1px solid #ddd;'>${openSensorState}</td>
                    </tr>
                    <tr>
                        <td style='padding: 8px; border: 1px solid #ddd;'><b>Physical Relay Switch</b></td>
                        <td style='padding: 8px; border: 1px solid #ddd;'>${garageSwitch ? garageSwitch.displayName : 'None'}</td>
                        <td style='padding: 8px; border: 1px solid #ddd;'>${relayState}</td>
                    </tr>
                    <tr style='background-color: #fafafa;'>
                        <td style='padding: 6px 8px; border: 1px solid #ddd; font-size: 11px; color: #666;' colspan='2'>Virtual Controller Driver Version</td>
                        <td style='padding: 6px 8px; border: 1px solid #ddd; font-size: 11px; color: #666;'>${driverVer}</td>
                    </tr>
                </table>
                """
            }
        }

        section("<h2>Application Label</h2>") {
            label title: "Assign a custom name for this app instance", required: false, defaultValue: garageControl ? "Garage Opener - ${garageControl.displayName}" : "Garage Opener"
        }

        section("<h2>Controls</h2>") {
            input name: "garageControl", type: "capability.garageDoorControl", title: "Virtual Garage Door Controller", description: "Select Virtual Simple Garage Door Controller device", required: true, submitOnChange: true
            input name: "garageSwitch", type: "capability.switch", title: "Physical Garage Switch / Relay", description: "Physical relay switch controlling garage door motor", required: true, submitOnChange: true
        }
        
        section("<h2>Contacts</h2>") {
            input name: "closedContact", type: "capability.contactSensor", title: "Garage Fully Closed Contact Sensor", required: false, submitOnChange: true
            input name: "openContact", type: "capability.contactSensor", title: "Garage Fully Open Contact Sensor", required: false, submitOnChange: true
        }
        
        section("<h2>Options</h2>") {
            input name: "tileFormat", type: "enum", title: "Notification Tile Format", options: ["html":"Colorized HTML Card", "plain":"Plain Text"], defaultValue: "html", required: true, submitOnChange: true
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging?", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging?", defaultValue: false, required: true
        }
    }
}

def installed() {
    logInfo "Installing app v${version()} (${timeStamp()})..."
    initialize()
}

def updated() {
    logInfo "Updating preferences..."
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    setupSubscriptions()
    
    if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    }
    
    // Sync initial contact sensor states and refresh tile
    syncContactStates()
    refreshNotificationTile("App initialized")
    logInfo "Initialized successfully."
}

def setupSubscriptions() {
    subscribe(garageControl, "door", garageControlHandler)
    subscribe(garageControl, "driverVersion", driverUpdatedHandler)
    if (closedContact) subscribe(closedContact, "contact", garageClosedContactHandler)    
    if (openContact) subscribe(openContact, "contact", garageOpenContactHandler)
}

// Driver Preferences Updated Event Listener
def driverUpdatedHandler(evt) {
    logDebug "Driver settings update detected. Re-rendering notification tile..."
    refreshNotificationTile("Driver preferences updated")
}

// Initial State Sync Helper
private void syncContactStates() {
    String closedState = closedContact ? closedContact.currentValue("contact") : "open"
    String openState = openContact ? openContact.currentValue("contact") : "open"
    
    logDebug "Syncing initial contact states -> ClosedContact: ${closedState}, OpenContact: ${openState}"
    if (garageControl.hasCommand("updateContactStatus")) {
        garageControl.updateContactStatus(closedState, openState)
    }
}

// Handle Commands from Virtual Garage Door Device (e.g. Alexa or Dashboard)
def garageControlHandler(evt) {    
    logDebug "Virtual Garage Controller event: ${evt.value}"
    
    if (evt.value == 'opening' || evt.value == 'closing') {
        logInfo "Virtual Controller requested '${evt.value}'. Triggering physical garage relay..."
        garageSwitch.on()
    }
    refreshNotificationTile("Door state changed to ${evt.value}")
}

// Physical Open Contact Sensor Handler
def garageOpenContactHandler(evt) {    
    logInfo "Open contact sensor changed to '${evt.value}'"
    if (garageControl.hasCommand("setOpenContact")) {
        garageControl.setOpenContact(evt.value)
    }
    refreshNotificationTile("Open contact sensor is ${evt.value}")
}

// Physical Closed Contact Sensor Handler
def garageClosedContactHandler(evt) {    
    logInfo "Closed contact sensor changed to '${evt.value}'"
    if (garageControl.hasCommand("setClosedContact")) {
        garageControl.setClosedContact(evt.value)
    }
    refreshNotificationTile("Closed contact sensor is ${evt.value}")
}

private String sanitizeFontSize(def inputVal, String defaultVal) {
    if (!inputVal) return defaultVal
    String trimmed = inputVal.toString().trim()
    return (trimmed.endsWith("em") || trimmed.endsWith("px") || trimmed.endsWith("%")) ? trimmed : "${trimmed}em"
}

// Generate Responsive Dashboard HTML / Plain Text Notification Tile
private void refreshNotificationTile(String reason = "") {
    if (!garageControl || !garageControl.hasCommand("updateNotificationTile")) return
    
    String timeStr = new Date().format("MM/dd hh:mm a", location.timeZone)
    String doorState = garageControl.currentValue("door")?.toUpperCase() ?: "UNKNOWN"
    String closedState = garageControl.currentValue("closedContact") ?: "open"
    String openState = garageControl.currentValue("openContact") ?: "open"
    String nameStr = app.label ?: "Garage Opener"
    
    String tileOutput = ""
    String formatType = settings?.tileFormat ?: "html"

    if (formatType == "plain") {
        tileOutput = "${nameStr} | ${doorState} | C:${closedState.toUpperCase()} O:${openState.toUpperCase()} | ${reason} (${timeStr})"
    } else {
        // Read driver preferences map
        Map devSettings = garageControl.getSettings() ?: [:]
        
        String tSize = sanitizeFontSize(devSettings.titleFontSize, "0.85em")
        String sSize = sanitizeFontSize(devSettings.statusFontSize, "0.75em")
        String cSize = sanitizeFontSize(devSettings.sensorFontSize, "0.75em")
        String uSize = sanitizeFontSize(devSettings.updatedFontSize, "0.65em")

        String tWeight = (devSettings.boldTitle != false) ? "font-weight:bold;" : "font-weight:normal;"
        String sWeight = (devSettings.boldStatus != false) ? "font-weight:bold;" : "font-weight:normal;"
        String cWeight = (devSettings.boldSensors != false) ? "font-weight:bold;" : "font-weight:normal;"

        // Main door state badge color
        String stateBgColor = "#2e7d32" // Dark Green
        if (doorState == "OPEN") stateBgColor = "#c62828" // Dark Red
        else if (doorState in ["OPENING", "CLOSING"]) stateBgColor = "#ef6c00" // Amber
        else if (doorState == "UNKNOWN") stateBgColor = "#6a1b9a" // Purple

        // Compact Sensor Badges
        String closedBadge = closedState == "closed" ? "<span style='color:#4caf50; ${cWeight}'>CLOSED</span>" : "<span style='color:#888; ${cWeight}'>OPEN</span>"
        String openBadge = openState == "closed" ? "<span style='color:#f44336; ${cWeight}'>CLOSED</span>" : "<span style='color:#888; ${cWeight}'>OPEN</span>"

        // Cleaned HTML String with Standardized border-radius:4px
        tileOutput = """<div style='height:100%; width:100%; box-sizing:border-box; padding:4px 6px; font-family:sans-serif; background:#181818; color:#fff; border-radius:4px; overflow:hidden; display:flex; flex-direction:column; justify-content:space-between;'>
            <div style='display:flex; justify-content:space-between; align-items:center; gap:4px;'>
                <span style='font-size:${tSize}; ${tWeight} white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:65%;' title='${nameStr}'>${nameStr}</span>
                <span style='background:${stateBgColor}; color:#fff; padding:1px 6px; border-radius:4px; ${sWeight} font-size:${sSize}; letter-spacing:0.5px;'>${doorState}</span>
            </div>
            <div style='display:flex; justify-content:space-between; font-size:${cSize}; color:#ccc; margin:2px 0;'>
                <span>C: ${closedBadge}</span>
                <span>O: ${openBadge}</span>
            </div>
            <div style='font-size:${uSize}; color:#4fc3f7; border-top:1px solid #333; padding-top:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>
                ${reason} <span style='color:#666;'>(${timeStr})</span>
            </div>
        </div>"""
    }

    garageControl.updateNotificationTile(tileOutput)
}

// Auto-Disable Debug Logging
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logInfo "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// Centralized Logging Engine
private void logInfo(String msg) {
    String nameStr = app.label ?: "Garage Opener"
    if (getSettingBool("logInfoEnable", true)) log.info "${nameStr}: ${msg}"
}

private void logDebug(String msg) {
    String nameStr = app.label ?: "Garage Opener"
    if (getSettingBool("logDebugEnable", false)) log.debug "${nameStr}: ${msg}"
}

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}