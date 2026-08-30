/**
 * Virtual Simple Garage Door Controller (Custom Test - v1.6.1Dev)
 * Platform: Hubitat Elevation
 * Notes: Diagnostic build to test optimistic door state emission for Alexa response confirmation
 * Capabilities: GarageDoorControl, Actuator, Configuration, Refresh
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

static String version() { return '1.6.1Dev' }
def timeStamp() { return "2026/08/29 12:00 PM" }

metadata {
    definition (
        name: "Virtual Simple Garage Door Controller (Custom Test)", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: ""
    ) {
        capability "GarageDoorControl"
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"

        // Attributes
        attribute "driverVersion", "string"
        attribute "closedContact", "string"
        attribute "openContact", "string"
        attribute "notificationTile", "string"
        attribute "relayTrigger", "string"
        
        // Dynamic HTML Styling Attributes
        attribute "titleFontSize", "string"
        attribute "statusFontSize", "string"
        attribute "sensorFontSize", "string"
        attribute "updatedFontSize", "string"
        attribute "boldTitle", "string"
        attribute "boldStatus", "string"
        attribute "boldSensors", "string"

        // Custom Commands for External Contact Sensors/Apps
        command "setClosedContact", [[name: "Contact State*", type: "ENUM", constraints: ["closed", "open"]]]
        command "setOpenContact", [[name: "Contact State*", type: "ENUM", constraints: ["closed", "open"]]]
        command "updateContactStatus", [
            [name: "Closed Contact State*", type: "ENUM", constraints: ["closed", "open"]],
            [name: "Open Contact State*", type: "ENUM", constraints: ["closed", "open"]]
        ]
        command "updateNotificationTile", [[name: "Tile Text*", type: "STRING"]]
        command "resetDriver"
    }

    preferences {
        input name: "transitionTimeout", type: "number", title: "Stall / Transition Timeout (seconds)", description: "Max travel time allowed in opening/closing state before marking door as stuck (unknown).", defaultValue: 15, required: true
        input name: "enableHtmlFormatting", type: "bool", title: "<b>Enable HTML Styling for Notification Tile</b>", defaultValue: true, required: true, submitOnChange: true

        if (settings?.enableHtmlFormatting != false) {
            input name: "prefTitleFontSize", type: "text", title: "<b>Title Font Size (em)</b> <i>(Default: 1.0em / 13px base)</i>", defaultValue: "1.0em", required: true
            input name: "prefBoldTitle", type: "bool", title: "<b>Bold Title Text?</b>", defaultValue: true, required: true

            input name: "prefStatusFontSize", type: "text", title: "<b>Door State Font Size (em)</b> <i>(Default: 0.85em)</i>", defaultValue: "0.85em", required: true
            input name: "prefBoldStatus", type: "bool", title: "<b>Bold Door State Badge?</b>", defaultValue: true, required: true

            input name: "prefSensorFontSize", type: "text", title: "<b>Sensor Status Font Size (em)</b> <i>(Default: 0.77em)</i>", defaultValue: "0.77em", required: true
            input name: "prefBoldSensors", type: "bool", title: "<b>Bold Sensor Badges?</b>", defaultValue: true, required: true

            input name: "prefUpdatedFontSize", type: "text", title: "<b>Footer Font Size (em)</b> <i>(Default: 0.70em)</i>", defaultValue: "0.70em", required: true
        }

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

private String sanitizeFontSize(def inputVal, String defaultVal) {
    if (!inputVal) return defaultVal
    String trimmed = inputVal.toString().trim()
    return (trimmed.endsWith("em") || trimmed.endsWith("px") || trimmed.endsWith("%")) ? trimmed : "${trimmed}em"
}

void parse(String description) {
    logDebug "parse(): ${description}"
}

// Capability Commands
void open() {
    String currentDoorState = device.currentValue("door")
    logTrace "open() called | current door state: ${currentDoorState}"
    if (currentDoorState != "open") {
        logInfo "open() command received. Emitting relayTrigger: pulse and immediately setting door state to open (v1.6.1Dev test)."
        logDebug "Emitting relayTrigger event -> pulse"
        sendEvent(name: "relayTrigger", value: "pulse", isStateChange: true, descriptionText: "Triggering relay for open command")
        
        // --- DIAGNOSTIC TEST MODIFICATION ---
        sendEvent(name: "door", value: "open", isStateChange: true, descriptionText: "garage door set to open (test)")
        
        // Re-evaluate tile synchronously
        renderNotificationTile("Door state changed to open (test)", "open", null, null)

        Integer timeoutSec = (settings?.transitionTimeout != null) ? (settings.transitionTimeout as Integer) : 15
        logDebug "Scheduling transitionSafetyCheck timer for ${timeoutSec} seconds"
        runIn(timeoutSec, "transitionSafetyCheck")
    } else {
        logDebug "open() requested but door is already open."
    }
}

void close() {
    String currentDoorState = device.currentValue("door")
    logTrace "close() called | current door state: ${currentDoorState}"
    if (currentDoorState != "closed") {
        logInfo "close() command received. Emitting relayTrigger: pulse and immediately setting door state to closed (v1.6.1Dev test)."
        logDebug "Emitting relayTrigger event -> pulse"
        sendEvent(name: "relayTrigger", value: "pulse", isStateChange: true, descriptionText: "Triggering relay for close command")
        
        // --- DIAGNOSTIC TEST MODIFICATION ---
        sendEvent(name: "door", value: "closed", isStateChange: true, descriptionText: "garage door set to closed (test)")
        
        // Re-evaluate tile synchronously
        renderNotificationTile("Door state changed to closed (test)", "closed", null, null)

        Integer timeoutSec = (settings?.transitionTimeout != null) ? (settings.transitionTimeout as Integer) : 15
        logDebug "Scheduling transitionSafetyCheck timer for ${timeoutSec} seconds"
        runIn(timeoutSec, "transitionSafetyCheck")
    } else {
        logDebug "close() requested but door is already closed."
    }
}

// External Contact Sensor Methods
void setClosedContact(String stateStr) {
    if (!stateStr) return
    String cleanState = stateStr.toLowerCase()
    logTrace "setClosedContact() called with state: ${cleanState}"
    sendEvent(name: "closedContact", value: cleanState, isStateChange: true, descriptionText: "closed contact sensor is ${cleanState}")
    logInfo "closed contact sensor is ${cleanState}"
    evaluateDoorState(cleanState, null)
}

void setOpenContact(String stateStr) {
    if (!stateStr) return
    String cleanState = stateStr.toLowerCase()
    logTrace "setOpenContact() called with state: ${cleanState}"
    sendEvent(name: "openContact", value: cleanState, isStateChange: true, descriptionText: "open contact sensor is ${cleanState}")
    logInfo "open contact sensor is ${cleanState}"
    evaluateDoorState(null, cleanState)
}

void updateContactStatus(String closedState, String openState) {
    if (!closedState || !openState) return
    String cleanClosed = closedState.toLowerCase()
    String cleanOpen = openState.toLowerCase()
    logTrace "updateContactStatus() called -> Closed: ${cleanClosed}, Open: ${cleanOpen}"
    
    unschedule("transitionSafetyCheck")
    
    sendEvent(name: "closedContact", value: cleanClosed, isStateChange: true, descriptionText: "closed contact sensor is ${cleanClosed}")
    sendEvent(name: "openContact", value: cleanOpen, isStateChange: true, descriptionText: "open contact sensor is ${cleanOpen}")
    evaluateDoorState(cleanClosed, cleanOpen)
}

void updateNotificationTile(String tileStr) {
    logTrace "updateNotificationTile() received explicit external tile string update"
    sendEvent(name: "notificationTile", value: tileStr, isStateChange: true, descriptionText: "notification tile updated")
}

// Internal Responsive HTML Generator Engine
private void renderNotificationTile(String reason = "", String doorOverride = null, String closedOverride = null, String openOverride = null) {
    if (settings?.enableHtmlFormatting == false) return

    String timeStr = new Date().format("MM/dd hh:mm a", location.timeZone ?: TimeZone.getDefault())
    String doorState = (doorOverride ?: device.currentValue("door") ?: "UNKNOWN").toUpperCase()
    String closedState = closedOverride ?: device.currentValue("closedContact") ?: "open"
    String openState = openOverride ?: device.currentValue("openContact") ?: "open"
    String titleStr = "Garage Door Status"

    String tSize = sanitizeFontSize(settings?.prefTitleFontSize, "1.0em")
    String sSize = sanitizeFontSize(settings?.prefStatusFontSize, "0.85em")
    String cSize = sanitizeFontSize(settings?.prefSensorFontSize, "0.77em")
    String uSize = sanitizeFontSize(settings?.prefUpdatedFontSize, "0.70em")

    String tWeight = (settings?.prefBoldTitle != false) ? "font-weight:bold;" : "font-weight:normal;"
    String sWeight = (settings?.prefBoldStatus != false) ? "font-weight:bold;" : "font-weight:normal;"
    String cWeight = (settings?.prefBoldSensors != false) ? "font-weight:bold;" : "font-weight:normal;"

    String stateBgColor = "#2e7d32" // Dark Green
    if (doorState == "OPEN") stateBgColor = "#c62828" // Dark Red
    else if (doorState in ["OPENING", "CLOSING"]) stateBgColor = "#ef6c00" // Amber
    else if (doorState == "UNKNOWN") stateBgColor = "#6a1b9a" // Purple

    String closedBgColor = closedState == "closed" ? "#2e7d32" : "#c62828"
    String openBgColor = openState == "open" ? "#2e7d32" : "#c62828"

    if (closedState == "open" && openState == "open") {
        closedBgColor = "#ef6c00"
        openBgColor = "#ef6c00"
    }

    String closedBadge = "<span style='background:${closedBgColor}; color:#fff; padding:1px 5px; border-radius:4px; ${cWeight} font-size:${cSize}; letter-spacing:0.3px;'>${closedState.toUpperCase()}</span>"
    String openBadge = "<span style='background:${openBgColor}; color:#fff; padding:1px 5px; border-radius:4px; ${cWeight} font-size:${cSize}; letter-spacing:0.3px;'>${openState.toUpperCase()}</span>"

    String htmlOutput = """<div style='height:100%; width:100%; box-sizing:border-box; padding:4px 6px; font-family:sans-serif; background:#181818; color:#fff; border-radius:4px; overflow:hidden; display:flex; flex-direction:column; justify-content:space-between;'>
        <div style='display:flex; justify-content:space-between; align-items:center; gap:4px;'>
            <span style='font-size:${tSize}; ${tWeight} white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:62%;' title='${titleStr}'>${titleStr}</span>
            <span style='background:${stateBgColor}; color:#fff; padding:2px 6px; border-radius:4px; ${sWeight} font-size:${sSize}; letter-spacing:0.5px;'>${doorState}</span>
        </div>
        <div style='display:flex; justify-content:space-between; font-size:${cSize}; color:#ccc; margin:2px 0;'>
            <div style='display:flex; flex-direction:column; align-items:flex-start; gap:2px;'>
                <span style='color:#aaa; font-size:0.9em;'>Closed Sensor</span>
                ${closedBadge}
            </div>
            <div style='display:flex; flex-direction:column; align-items:flex-end; gap:2px;'>
                <span style='color:#aaa; font-size:0.9em;'>Open Sensor</span>
                ${openBadge}
            </div>
        </div>
        <div style='font-size:${uSize}; color:#4fc3f7; border-top:1px solid #333; padding-top:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>
            ${reason} <span style='color:#666;'>(${timeStr})</span>
        </div>
    </div>"""

    sendEvent(name: "notificationTile", value: htmlOutput, isStateChange: true, descriptionText: "notification tile updated")
}

// State Truth Table Evaluator
private void evaluateDoorState(String closedOverride = null, String openOverride = null) {
    String closedSensor = closedOverride ?: device.currentValue("closedContact")
    String openSensor = openOverride ?: device.currentValue("openContact")
    String currentDoorState = device.currentValue("door") ?: "unknown"

    logTrace "evaluateDoorState() evaluation started -> ClosedContact: ${closedSensor}, OpenContact: ${openSensor}, CurrentDoor: ${currentDoorState}"

    if (closedSensor == null || openSensor == null) {
        logDebug "evaluateDoorState(): Contact sensor states not yet initialized. Skipping evaluation."
        return
    }

    String resolvedDoorState = currentDoorState
    String reasonText = ""

    if (closedSensor == "closed" && openSensor == "open") {
        logInfo "Door reached position: FULLY CLOSED"
        unschedule("transitionSafetyCheck")
        resolvedDoorState = "closed"
        reasonText = "Door state confirmed closed by sensors"
        sendEvent(name: "door", value: "closed", isStateChange: true, descriptionText: "garage door is closed")
    } 
    else if (closedSensor == "open" && openSensor == "closed") {
        logInfo "Door reached position: FULLY OPEN"
        unschedule("transitionSafetyCheck")
        resolvedDoorState = "open"
        reasonText = "Door state confirmed open by sensors"
        sendEvent(name: "door", value: "open", isStateChange: true, descriptionText: "garage door is open")
    } 
    else if (closedSensor == "open" && openSensor == "open") {
        logInfo "Both contact sensors report OPEN (Door is moving mid-travel)"
        if (currentDoorState == "closed" || currentDoorState == "opening") {
            logDebug "Mid-travel transition detected: Door is OPENING"
            resolvedDoorState = "opening"
            reasonText = "Door state changed to opening"
            sendEvent(name: "door", value: "opening", isStateChange: true, descriptionText: "garage door is opening")
        } else if (currentDoorState == "open" || currentDoorState == "closing") {
            logDebug "Mid-travel transition detected: Door is CLOSING"
            resolvedDoorState = "closing"
            reasonText = "Door state changed to closing"
            sendEvent(name: "door", value: "closing", isStateChange: true, descriptionText: "garage door is closing")
        } else if (currentDoorState == "unknown") {
            logWarn "Door remains stuck/stopped mid-travel between contact sensors."
            reasonText = "Door stuck mid-travel"
        }
    } 
    else if (closedSensor == "closed" && openSensor == "closed") {
        logError "Invalid sensor state: Both closed and open contact sensors report CLOSED!"
        reasonText = "Sensor Error: Both Closed"
    }

    renderNotificationTile(reasonText, resolvedDoorState, closedSensor, openSensor)
}

void transitionSafetyCheck() {
    String currentDoorState = device.currentValue("door")
    logTrace "transitionSafetyCheck() timer fired | current door state: ${currentDoorState}"
    if (currentDoorState == "opening" || currentDoorState == "closing") {
        logWarn "Garage door motion stalled or timed out while ${currentDoorState}! Marking door state as unknown (stuck mid-travel)."
        sendEvent(name: "door", value: "unknown", isStateChange: true, descriptionText: "garage door is stuck mid-travel (unknown state)")
        renderNotificationTile("Stalled mid-travel timeout", "unknown", null, null)
    } else {
        logDebug "transitionSafetyCheck() fired, but door state is already resolved to '${currentDoorState}'. No action needed."
    }
}

private void syncStyleAttributes() {
    logTrace "syncStyleAttributes() syncing typography settings"
    sendIfChanged([name: "titleFontSize", value: sanitizeFontSize(settings?.prefTitleFontSize, "1.0em")])
    sendIfChanged([name: "statusFontSize", value: sanitizeFontSize(settings?.prefStatusFontSize, "0.85em")])
    sendIfChanged([name: "sensorFontSize", value: sanitizeFontSize(settings?.prefSensorFontSize, "0.77em")])
    sendIfChanged([name: "updatedFontSize", value: sanitizeFontSize(settings?.prefUpdatedFontSize, "0.70em")])
    
    sendIfChanged([name: "boldTitle", value: (settings?.prefBoldTitle != false)?.toString()])
    sendIfChanged([name: "boldStatus", value: (settings?.prefBoldStatus != false)?.toString()])
    sendIfChanged([name: "boldSensors", value: (settings?.prefBoldSensors != false)?.toString()])
}

void installed() {
    checkAndLogVersionDemarcation()
    sendEvent(name: "driverVersion", value: version())
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    syncStyleAttributes()
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    renderNotificationTile("Driver preferences updated", null, null, null)
    initialize(false)
}

def configure() {
    checkAndLogVersionDemarcation()
    sendEvent(name: "driverVersion", value: version())
    syncStyleAttributes()
    initialize(false)
    return []
}

def refresh() {
    checkAndLogVersionDemarcation()
    evaluateDoorState()
    syncStyleAttributes()
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    return []
}

private void initialize(Boolean isInstall = false) {
    unschedule("disableDebugLogging")
    
    if (device.currentValue("door") == null) {
        sendEvent(name: "door", value: "unknown")
    }
    
    syncStyleAttributes()

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging enabled. Will automatically turn off in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

void resetDriver() {
    logTrace "resetDriver() initiated"
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    logInfo "Driver reset process completed."
}

void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    state.clear()
    logInfo "All states have been cleared."
}

void clearAllAttributes() {
    logInfo "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
    logInfo "All attributes have been cleared."
}

void clearAllSchedules() {
    logInfo "Clearing all scheduled jobs (including orphaned schedules)..."
    unschedule()
    logInfo "All scheduled jobs have been successfully cleared."
}

private void sendIfChanged(Map args) {
    if (!args || !args.name) return

    String nameStr = args.name as String
    String oldVal = device.currentValue(nameStr)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        String desc = args.descriptionText ?: "${nameStr} set to ${args.value}"
        Map eventMap = [
            name: nameStr, 
            value: args.value, 
            descriptionText: desc
        ]
        if (args.unit) eventMap.unit = args.unit
        if (args.type) eventMap.type = args.type
        if (args.isStateChange != null) eventMap.isStateChange = args.isStateChange

        sendEvent(eventMap)
        logInfo "${desc}"
        logDebug "Event triggered: ${nameStr} -> ${args.value}"
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${devName}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}