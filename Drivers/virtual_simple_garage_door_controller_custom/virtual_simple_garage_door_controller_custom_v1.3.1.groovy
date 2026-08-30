/**
 * Virtual Simple Garage Door Controller (Custom)
 * Platform: Hubitat Elevation
 * Notes: Dual-contact state calculation driver to resolve Alexa Garage Door voice control timeouts
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

/**
 *  Purpose:
 *  Provides virtual garage door state management using two contact sensors (closed/open position) to resolve Alexa voice control state confirmation timeouts.
 *
 *  Instructions:
 *  1. Create virtual device and select this driver.
 *  2. Map your physical open/closed contact sensors to update setClosedContact and setOpenContact via rules or app bindings.
 *  
 *  Changelog:
 *  v1.3.1    08/28/26    jshimota    Added granular trace and debug logging throughout open/close command execution and relayTrigger emissions
 *  v1.3.0    08/28/26    jshimota    Pushed HTML font settings to attributes on updated() to fix illegal getSettings exception causing relay bouncing
 *  v1.2.9    08/28/26    jshimota    Added dedicated 'relayTrigger' attribute on explicit open()/close() calls to prevent feedback loops
 *  v1.2.8    08/28/26    jshimota    Standardized EM font unit preference handling and sanitized typography inputs for HTML tile generation
 *  v1.2.7    08/28/26    jshimota    Bypassed sendIfChanged in updateNotificationTile with explicit isStateChange:true to ensure instant dashboard CSS updates
 *  v1.2.6    08/28/26    jshimota    Integrated Mode Status Tile font sizing, bold preferences, and dynamic CSS styling for notificationTile
 *  v1.2.5    08/28/26    jshimota    Updated updateNotificationTile signature to handle both Plain Text and Formatted HTML outputs
 *  v1.2.4    08/28/26    jshimota    Added notificationTile attribute and updateNotificationTile method for dashboard tile visualizations
 *  v1.2.3    08/28/26    jshimota    Added stalled/stuck state detection transitioning door to 'unknown' on safety timeout
 *  v1.2.2    08/28/26    jshimota    Resolved NPE safeguards on uninitialized contact attributes and added transition race-condition protection
 *  v1.2.1    08/28/26    jshimota    Cleaned up header block layout and license comment structure to strictly match master template
 *  v1.2.0    08/28/26    jshimota    Applied master driver template, added dual-contact state calculation logic, and fixed Alexa voice confirmation timeout
 *  v1.1.1    06/22/26    jshimota    Fixed logging preference mappings between wrnEnable and dbgEnable, aligned auto-timeout
 *  v1.1.0    06/22/26    jshimota    Fixed wrnEnable reference bug, generalized logging helpers, added Initialize capability, added auto logsOff
 *  v1.0.0    01/01/21    muxa        Original Source Release
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.3.1' }
def timeStamp() { return "2026/08/28 01:10 PM" }

metadata {
    definition (
        name: "Virtual Simple Garage Door Controller (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota/Hubitat/Drivers/virtual_simple_garage_door_controller.groovy"
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
            input name: "prefTitleFontSize", type: "text", title: "<b>Title Font Size (em)</b> <i>(Default: 0.85em)</i>", defaultValue: "0.85em", required: true
            input name: "prefBoldTitle", type: "bool", title: "<b>Bold Title Text?</b>", defaultValue: true, required: true

            input name: "prefStatusFontSize", type: "text", title: "<b>Door State Font Size (em)</b> <i>(Default: 0.75em)</i>", defaultValue: "0.75em", required: true
            input name: "prefBoldStatus", type: "bool", title: "<b>Bold Door State Badge?</b>", defaultValue: true, required: true

            input name: "prefSensorFontSize", type: "text", title: "<b>Sensor Status Font Size (em)</b> <i>(Default: 0.75em)</i>", defaultValue: "0.75em", required: true
            input name: "prefBoldSensors", type: "bool", title: "<b>Bold Sensor Badges?</b>", defaultValue: true, required: true

            input name: "prefUpdatedFontSize", type: "text", title: "<b>Footer Font Size (em)</b> <i>(Default: 0.65em)</i>", defaultValue: "0.65em", required: true
        }

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// NPE-Safe Timestamp Helper Routine
private String getTimestamp() {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)
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
        logInfo "open() command received. Emitting relayTrigger: pulse and setting door state to opening."
        logDebug "Emitting relayTrigger event -> pulse"
        sendEvent(name: "relayTrigger", value: "pulse", isStateChange: true, descriptionText: "Triggering relay for open command")
        
        sendIfChanged([name: "door", value: "opening", descriptionText: "garage door is opening"])
        
        // Schedule transition timeout to catch stuck/stalled door motion
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
        logInfo "close() command received. Emitting relayTrigger: pulse and setting door state to closing."
        logDebug "Emitting relayTrigger event -> pulse"
        sendEvent(name: "relayTrigger", value: "pulse", isStateChange: true, descriptionText: "Triggering relay for close command")
        
        sendIfChanged([name: "door", value: "closing", descriptionText: "garage door is closing"])
        
        // Schedule transition timeout to catch stuck/stalled door motion
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
    sendIfChanged([name: "closedContact", value: cleanState, descriptionText: "closed contact sensor is ${cleanState}"])
    evaluateDoorState()
}

void setOpenContact(String stateStr) {
    if (!stateStr) return
    String cleanState = stateStr.toLowerCase()
    logTrace "setOpenContact() called with state: ${cleanState}"
    sendIfChanged([name: "openContact", value: cleanState, descriptionText: "open contact sensor is ${cleanState}"])
    evaluateDoorState()
}

void updateContactStatus(String closedState, String openState) {
    if (!closedState || !openState) return
    String cleanClosed = closedState.toLowerCase()
    String cleanOpen = openState.toLowerCase()
    logTrace "updateContactStatus() called -> Closed: ${cleanClosed}, Open: ${cleanOpen}"
    sendIfChanged([name: "closedContact", value: cleanClosed, descriptionText: "closed contact sensor is ${cleanClosed}"])
    sendIfChanged([name: "openContact", value: cleanOpen, descriptionText: "open contact sensor is ${cleanOpen}"])
    evaluateDoorState()
}

// Direct Forced Tile Update
void updateNotificationTile(String tileStr) {
    logTrace "updateNotificationTile() received tile update"
    sendEvent(name: "notificationTile", value: tileStr, isStateChange: true, descriptionText: "notification tile updated")
}

// State Truth Table Evaluator
private void evaluateDoorState() {
    String closedSensor = device.currentValue("closedContact")
    String openSensor = device.currentValue("openContact")
    String currentDoorState = device.currentValue("door") ?: "unknown"

    logTrace "evaluateDoorState() evaluation started -> ClosedContact: ${closedSensor}, OpenContact: ${openSensor}, CurrentDoor: ${currentDoorState}"

    // NPE Safeguard: Do not evaluate until contact attributes are established
    if (closedSensor == null || openSensor == null) {
        logDebug "evaluateDoorState(): Contact sensor states not yet initialized (Closed: ${closedSensor}, Open: ${openSensor}). Skipping evaluation."
        return
    }

    if (closedSensor == "closed" && openSensor == "open") {
        logTrace "Truth table matched: Door is FULLY CLOSED"
        unschedule("transitionSafetyCheck")
        sendIfChanged([name: "door", value: "closed", descriptionText: "garage door is closed"])
    } 
    else if (closedSensor == "open" && openSensor == "closed") {
        logTrace "Truth table matched: Door is FULLY OPEN"
        unschedule("transitionSafetyCheck")
        sendIfChanged([name: "door", value: "open", descriptionText: "garage door is open"])
    } 
    else if (closedSensor == "open" && openSensor == "open") {
        logTrace "Truth table matched: Door is TRANSITIONAL (mid-travel)"
        if (currentDoorState == "closed" || currentDoorState == "opening") {
            sendIfChanged([name: "door", value: "opening", descriptionText: "garage door is opening"])
        } else if (currentDoorState == "open" || currentDoorState == "closing") {
            sendIfChanged([name: "door", value: "closing", descriptionText: "garage door is closing"])
        } else if (currentDoorState == "unknown") {
            logWarn "Door remains stuck/stopped mid-travel between contact sensors."
        }
    } 
    else if (closedSensor == "closed" && openSensor == "closed") {
        logError "Invalid sensor state: Both closed and open contact sensors report CLOSED!"
    }
}

// Safety Timeout Callback for Stalled/Stuck Door Motion
void transitionSafetyCheck() {
    String currentDoorState = device.currentValue("door")
    logTrace "transitionSafetyCheck() timer fired | current door state: ${currentDoorState}"
    if (currentDoorState == "opening" || currentDoorState == "closing") {
        logWarn "Garage door motion stalled or timed out while ${currentDoorState}! Marking door state as unknown (stuck mid-travel)."
        sendIfChanged([name: "door", value: "unknown", descriptionText: "garage door is stuck mid-travel (unknown state)"])
    } else {
        logDebug "transitionSafetyCheck() fired, but door state is already resolved to '${currentDoorState}'. No action needed."
    }
}

// Update Driver Attributes with Preferences to Avoid getSettings() Crashes
private void syncStyleAttributes() {
    logTrace "syncStyleAttributes() syncing typography settings"
    sendIfChanged([name: "titleFontSize", value: sanitizeFontSize(settings?.prefTitleFontSize, "0.85em")])
    sendIfChanged([name: "statusFontSize", value: sanitizeFontSize(settings?.prefStatusFontSize, "0.75em")])
    sendIfChanged([name: "sensorFontSize", value: sanitizeFontSize(settings?.prefSensorFontSize, "0.75em")])
    sendIfChanged([name: "updatedFontSize", value: sanitizeFontSize(settings?.prefUpdatedFontSize, "0.65em")])
    
    sendIfChanged([name: "boldTitle", value: (settings?.prefBoldTitle != false)?.toString()])
    sendIfChanged([name: "boldStatus", value: (settings?.prefBoldStatus != false)?.toString()])
    sendIfChanged([name: "boldSensors", value: (settings?.prefBoldSensors != false)?.toString()])
}

// Hubitat Lifecycle Routines
void installed() {
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    sendEvent(name: "driverVersion", value: version())
    initialize(true)
}

void updated() {
    logInfo "Preferences updated. Syncing styling attributes..."
    syncStyleAttributes()
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
}

def configure() {
    logInfo "Configuring device..."
    sendEvent(name: "driverVersion", value: version())
    syncStyleAttributes()
    initialize(false)
    return []
}

def refresh() {
    logDebug "Executing refresh()..."
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

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

// Master Utility Routine for Driver GUI Button
void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    logInfo "Driver reset process completed."
}

// Individual Utility Routines
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

// State-De-Duplication Helper Routine
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

// Centralized Logging Engine
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