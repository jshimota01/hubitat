/**
 * Virtual Simple Garage Door Controller (Custom)
 * Platform: Hubitat Elevation
 * Notes: Dual-contact state calculation driver with transition direction tracking and stall alert visualization
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
 *  Provides virtual garage door state management using two contact sensors (closed/open position)
 *  and transition direction tracking. Detects stalls/timeouts during travel and renders alert badges for dashboard tiles.
 *
 *  Instructions:
 *  1. Create virtual device and select this driver.
 *  2. Map your physical open/closed contact sensors to update setClosedContact and setOpenContact via rules or app bindings.
 *  
 *  Changelog:
 *  v1.6.5    08/29/26    jshimota    Ensured transitionSafetyCheck is scheduled on manual contact transitions and cleared stale direction on timeout
 *  v1.6.4    08/29/26    jshimota    Integrated transition safety check stall alerts, updating door state to 'unknown' and displaying alert badges
 *  v1.6.3    08/29/26    jshimota    Added explicit state.transitionDirection tracking to lock mid-travel evaluations to active direction
 *  v1.6.2    08/29/26    jshimota    Redesigned state architecture to separate command intent from physical sensor evaluation; suppressed duplicate mid-travel events
 *  v1.6.1    08/29/26    jshimota    Inverted Open Sensor badge color mapping (Green when open, Red when closed)
 *  v1.6.0    08/28/26    jshimota    Reverted driver codebase to stable v1.3.9 baseline versioning
 *  v1.5.9    08/28/26    jshimota    Rebound child event subscription with explicit string method target to ensure dev:1027 unlock triggers parent open/close
 *  v1.5.8    08/28/26    jshimota    Fixed MissingMethodException by calling no-arg unsubscribe() in setupChildSubscriptions()
 *  v1.5.7    08/28/26    jshimota    Added child device event subscriptions (childLockHandler) so native child lock commands trigger parent open()/close()
 *  v1.5.6    08/28/26    jshimota    Suppressed duplicate event emissions on syncChildShadowLock to eliminate secondary API payloads to Alexa Skill
 *  v1.5.5    08/28/26    jshimota    Emitted explicit LOCKED / UNLOCKED values to shadowLockState attribute during child sync
 *  v1.5.4    08/28/26    jshimota    Ensured updateContactStatus forces child Shadow Lock alignment during manual app reset/repair requests
 *  v1.5.3    08/28/26    jshimota    Fixed MissingMethodException on syncChildShadowLock by calling lock()/unlock() directly on child device
 *  v1.5.2    08/28/26    jshimota    Fixed system driver string from 'Generic Virtual Lock' to native Hubitat 'Virtual Lock'
 *  v1.5.1    08/28/26    jshimota    Updated HTML tile center badge text to explicitly show LOCKED / UNLOCKED state
 *  v1.5.0    08/28/26    jshimota    Integrated Option A Child Shadow Lock device generation, bi-directional sync, and on-screen tile/app GUI indicators
 *  v1.4.3    08/28/26    jshimota    Made optimistic state settlement synchronous inside open()/close() to return instant state confirmation to Alexa voice skill
 *  v1.4.2    08/28/26    jshimota    Relocated Alexa confirmation badge to center column between contact sensors with scaled typography
 *  v1.4.1    08/28/26    jshimota    Removed tile Alexa badge to restore full title layout, suppressed duplicate door event emissions during mid-travel
 *  v1.4.0    08/28/26    jshimota    Added Optimistic Voice Confirmation toggle, auto-settle timer, and live Alexa confirmation badge on HTML tile
 *  v1.3.9    08/28/26    jshimota    Converted version marker to a single-shot trace demarcation logged strictly when version changes
 *  v1.3.8    08/28/26    jshimota    Added version and timestamp signature logInfo markers on lifecycle methods (installed/updated/configure/refresh)
 *  v1.3.7    08/28/26    jshimota    Moved HTML tile rendering engine directly into driver to eliminate asynchronous event-bus state lag
 *  v1.3.6    08/28/26    jshimota    Normalized default tile font relative sizes around 1.0em base (13px equivalent)
 *  v1.3.5    08/28/26    jshimota    Replaced sendIfChanged with explicit isStateChange:true on contact updates to guarantee instant tile badge synchronization
 *  v1.3.4    08/28/26    jshimota    Added explicit logging for mid-travel transitional states (opening/closing) when contact sensors open mid-travel
 *  v1.3.3    08/28/26    jshimota    Passed contact states explicitly into evaluateDoorState to prevent stale currentValue race conditions
 *  v1.3.2    08/28/26    jshimota    Ensured manual state repair clears orphaned transitionSafetyCheck timeouts and logs repair state
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

static String version() { return '1.6.5' }
def timeStamp() { return "2026/08/29 02:15 PM" }

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

private Integer getTransitionTimeoutSec() {
    return (settings?.transitionTimeout != null) ? (settings.transitionTimeout as Integer) : 15
}

void parse(String description) {
    logDebug "parse(): ${description}"
}

// Capability Commands
void open() {
    String currentDoorState = device.currentValue("door")
    logTrace "open() called | current door state: ${currentDoorState}"
    if (currentDoorState != "open") {
        state.transitionDirection = "opening"
        logInfo "open() command received. Setting transition direction to OPENING and pulsing relay."
        logDebug "Emitting relayTrigger event -> pulse"
        sendEvent(name: "relayTrigger", value: "pulse", isStateChange: true, descriptionText: "Triggering relay for open command")
        
        sendEvent(name: "door", value: "opening", isStateChange: true, descriptionText: "garage door is opening")
        
        renderNotificationTile("Door state changed to opening", "opening", null, null)

        Integer timeoutSec = getTransitionTimeoutSec()
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
        state.transitionDirection = "closing"
        logInfo "close() command received. Setting transition direction to CLOSING and pulsing relay."
        logDebug "Emitting relayTrigger event -> pulse"
        sendEvent(name: "relayTrigger", value: "pulse", isStateChange: true, descriptionText: "Triggering relay for close command")
        
        sendEvent(name: "door", value: "closing", isStateChange: true, descriptionText: "garage door is closing")
        
        renderNotificationTile("Door state changed to closing", "closing", null, null)

        Integer timeoutSec = getTransitionTimeoutSec()
        logDebug "Scheduling transitionSafetyCheck timer for ${timeoutSec} seconds"
        runIn(timeoutSec, "transitionSafetyCheck")
    } else {
        logDebug "close() requested but door is already closed."
    }
}

// External Contact Sensor Methods (Forced Synchronous Tile Refresh)
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
    state.transitionDirection = null
    
    sendEvent(name: "closedContact", value: cleanClosed, isStateChange: true, descriptionText: "closed contact sensor is ${cleanClosed}")
    sendEvent(name: "openContact", value: cleanOpen, isStateChange: true, descriptionText: "open contact sensor is ${cleanOpen}")
    evaluateDoorState(cleanClosed, cleanOpen)
}

// Direct Command Tile Override
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

    // Main Door State Badge Color
    String stateBgColor = "#2e7d32" // Dark Green
    String displayStateText = doorState
    
    if (doorState == "OPEN") {
        stateBgColor = "#c62828" // Dark Red
    } else if (doorState in ["OPENING", "CLOSING"]) {
        stateBgColor = "#ef6c00" // Amber
    } else if (doorState == "UNKNOWN") {
        stateBgColor = "#6a1b9a" // Purple (Stuck / Stalled)
        displayStateText = "STUCK"
    }

    // Sensor Button Colors
    String closedBgColor = closedState == "closed" ? "#2e7d32" : "#c62828"
    // Inverted Open Sensor logic: GREEN when open (door not fully up), RED when closed (door fully up)
    String openBgColor = openState == "open" ? "#2e7d32" : "#c62828"

    // Both Open Transition Rule -> Apply Amber/Gold transition color to both sensor buttons
    if (closedState == "open" && openState == "open") {
        closedBgColor = "#ef6c00"
        openBgColor = "#ef6c00"
    }

    String closedBadge = "<span style='background:${closedBgColor}; color:#fff; padding:1px 5px; border-radius:4px; ${cWeight} font-size:${cSize}; letter-spacing:0.3px;'>${closedState.toUpperCase()}</span>"
    String openBadge = "<span style='background:${openBgColor}; color:#fff; padding:1px 5px; border-radius:4px; ${cWeight} font-size:${cSize}; letter-spacing:0.3px;'>${openState.toUpperCase()}</span>"

    String htmlOutput = """<div style='height:100%; width:100%; box-sizing:border-box; padding:4px 6px; font-family:sans-serif; background:#181818; color:#fff; border-radius:4px; overflow:hidden; display:flex; flex-direction:column; justify-content:space-between;'>
        <div style='display:flex; justify-content:space-between; align-items:center; gap:4px;'>
            <span style='font-size:${tSize}; ${tWeight} white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:62%;' title='${titleStr}'>${titleStr}</span>
            <span style='background:${stateBgColor}; color:#fff; padding:2px 6px; border-radius:4px; ${sWeight} font-size:${sSize}; letter-spacing:0.5px;'>${displayStateText}</span>
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

// State Truth Table Evaluator with Synchronous Tile Generation
private void evaluateDoorState(String closedOverride = null, String openOverride = null) {
    String closedSensor = closedOverride ?: device.currentValue("closedContact")
    String openSensor = openOverride ?: device.currentValue("openContact")
    String currentDoorState = device.currentValue("door") ?: "unknown"
    String activeDirection = state.transitionDirection

    logTrace "evaluateDoorState() evaluation started -> ClosedContact: ${closedSensor}, OpenContact: ${openSensor}, CurrentDoor: ${currentDoorState}, TransitionDir: ${activeDirection}"

    // NPE Safeguard: Do not evaluate until contact attributes are established
    if (closedSensor == null || openSensor == null) {
        logDebug "evaluateDoorState(): Contact sensor states not yet initialized. Skipping evaluation."
        return
    }

    String resolvedDoorState = currentDoorState
    String reasonText = ""

    if (closedSensor == "closed" && openSensor == "open") {
        logInfo "Door reached position: FULLY CLOSED"
        unschedule("transitionSafetyCheck")
        state.transitionDirection = null
        resolvedDoorState = "closed"
        reasonText = "Door state changed to closed"
        sendEvent(name: "door", value: "closed", isStateChange: true, descriptionText: "garage door is closed")
    } 
    else if (closedSensor == "open" && openSensor == "closed") {
        logInfo "Door reached position: FULLY OPEN"
        unschedule("transitionSafetyCheck")
        state.transitionDirection = null
        resolvedDoorState = "open"
        reasonText = "Door state changed to open"
        sendEvent(name: "door", value: "open", isStateChange: true, descriptionText: "garage door is open")
    } 
    else if (closedSensor == "open" && openSensor == "open") {
        logInfo "Both contact sensors report OPEN (Door is moving mid-travel)"
        
        // Priority 1: Use explicitly set transitionDirection
        if (activeDirection == "closing") {
            logDebug "Mid-travel transition evaluated: Door is CLOSING"
            resolvedDoorState = "closing"
            reasonText = "Door state changed to closing"
            if (currentDoorState != "closing") {
                sendEvent(name: "door", value: "closing", isStateChange: true, descriptionText: "garage door is closing")
            }
        } 
        else if (activeDirection == "opening") {
            logDebug "Mid-travel transition evaluated: Door is OPENING"
            resolvedDoorState = "opening"
            reasonText = "Door state changed to opening"
            if (currentDoorState != "opening") {
                sendEvent(name: "door", value: "opening", isStateChange: true, descriptionText: "garage door is opening")
            }
        } 
        // Priority 2: Fallback for manual physical triggers without command intent
        else if (currentDoorState == "closed") {
            logDebug "Mid-travel transition detected (manual trigger): Door is OPENING"
            state.transitionDirection = "opening"
            resolvedDoorState = "opening"
            reasonText = "Door state changed to opening"
            sendEvent(name: "door", value: "opening", isStateChange: true, descriptionText: "garage door is opening")
            
            Integer timeoutSec = getTransitionTimeoutSec()
            logDebug "Scheduling transitionSafetyCheck timer for manual opening -> ${timeoutSec} seconds"
            runIn(timeoutSec, "transitionSafetyCheck")
        } 
        else if (currentDoorState == "open") {
            logDebug "Mid-travel transition detected (manual trigger): Door is CLOSING"
            state.transitionDirection = "closing"
            resolvedDoorState = "closing"
            reasonText = "Door state changed to closing"
            sendEvent(name: "door", value: "closing", isStateChange: true, descriptionText: "garage door is closing")
            
            Integer timeoutSec = getTransitionTimeoutSec()
            logDebug "Scheduling transitionSafetyCheck timer for manual closing -> ${timeoutSec} seconds"
            runIn(timeoutSec, "transitionSafetyCheck")
        } 
        else if (currentDoorState == "unknown") {
            logWarn "Door remains stuck/stopped mid-travel between contact sensors."
            reasonText = "Door stuck mid-travel"
        }
    } 
    else if (closedSensor == "closed" && openSensor == "closed") {
        logError "Invalid sensor state: Both closed and open contact sensors report CLOSED!"
        reasonText = "Sensor Error: Both Closed"
    }

    // Force synchronous tile update with live parameter overrides
    renderNotificationTile(reasonText, resolvedDoorState, closedSensor, openSensor)
}

// Safety Timeout Callback for Stalled/Stuck Door Motion
void transitionSafetyCheck() {
    String currentDoorState = device.currentValue("door")
    logTrace "transitionSafetyCheck() timer fired | current door state: ${currentDoorState}"
    if (currentDoorState == "opening" || currentDoorState == "closing") {
        logWarn "Garage door motion stalled or timed out while ${currentDoorState}! Marking door state as unknown (stuck mid-travel)."
        state.transitionDirection = null
        sendEvent(name: "door", value: "unknown", isStateChange: true, descriptionText: "garage door is stuck mid-travel (unknown state)")
        renderNotificationTile("Stalled mid-travel timeout", "unknown", null, null)
    } else {
        logDebug "transitionSafetyCheck() fired, but door state is already resolved to '${currentDoorState}'. No action needed."
    }
}

// Update Driver Attributes with Preferences to Avoid getSettings() Crashes
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

// Hubitat Lifecycle Routines
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
    logTrace "resetDriver() initiated"
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