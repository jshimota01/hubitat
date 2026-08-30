/**
 * Virtual Simple Garage Door Controller (Custom)
 * Platform: Hubitat Elevation
 * Notes: Dual-contact state calculation driver to resolve Alexa Garage Door voice control timeouts via Child Shadow Lock
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
 *  Provides virtual garage door state management using two contact sensors with an optional Child Alexa Shadow Lock device to bypass 11.74s voice control timeout limits.
 *
 *  Instructions:
 *  1. Create virtual device and select this driver.
 *  2. Enable "Create Alexa Shadow Lock" in Driver Preferences.
 *  3. In Amazon Alexa Skill, add ONLY the child Shadow Lock device (e.g., "Garage Door Alexa Lock").
 *  
 *  Changelog:
 *  v1.5.1    08/28/26    jshimota    Updated HTML tile center badge text to explicitly show LOCKED / UNLOCKED state
 *  v1.5.0    08/28/26    jshimota    Integrated Option A Child Shadow Lock device generation, bi-directional sync, and on-screen tile/app GUI indicators
 *  v1.4.3    08/28/26    jshimota    Made optimistic state settlement synchronous inside open()/close() to return instant state confirmation to Alexa voice skill
 *  v1.4.2    08/28/26    jshimota    Relocated Alexa confirmation badge to center column between contact sensors with scaled typography
 *  v1.4.1    08/28/26    jshimota    Removed tile Alexa badge to restore full title layout, suppressed duplicate door event emissions during mid-travel
 *  v1.4.0    08/28/26    jshimota    Added Optimistic Voice Confirmation toggle, auto-settle timer, and live Alexa confirmation badge on HTML tile
 *  v1.3.9    08/28/26    jshimota    Converted version marker to a single-shot trace demarcation logged strictly when version changes
 *  v1.3.8    08/28/26    jshimota    Added version and timestamp signature logInfo markers on lifecycle methods
 *  v1.3.7    08/28/26    jshimota    Moved HTML tile rendering engine directly into driver to eliminate asynchronous event-bus state lag
 *  v1.3.6    08/28/26    jshimota    Normalized default tile font relative sizes around 1.0em base (13px equivalent)
 *  v1.3.5    08/28/26    jshimota    Replaced sendIfChanged with explicit isStateChange:true on contact updates
 *  v1.3.4    08/28/26    jshimota    Added explicit logging for mid-travel transitional states
 *  v1.3.3    08/28/26    jshimota    Passed contact states explicitly into evaluateDoorState
 *  v1.3.2    08/28/26    jshimota    Ensured manual state repair clears orphaned transitionSafetyCheck timeouts
 *  v1.3.1    08/28/26    jshimota    Added granular trace and debug logging
 *  v1.3.0    08/28/26    jshimota    Pushed HTML font settings to attributes on updated()
 *  v1.2.9    08/28/26    jshimota    Added dedicated 'relayTrigger' attribute on explicit open()/close() calls
 *  v1.2.8    08/28/26    jshimota    Standardized EM font unit preference handling
 *  v1.2.7    08/28/26    jshimota    Bypassed sendIfChanged in updateNotificationTile with explicit isStateChange:true
 *  v1.2.6    08/28/26    jshimota    Integrated Mode Status Tile font sizing, bold preferences, and dynamic CSS styling
 *  v1.2.5    08/28/26    jshimota    Updated updateNotificationTile signature
 *  v1.2.4    08/28/26    jshimota    Added notificationTile attribute
 *  v1.2.3    08/28/26    jshimota    Added stalled/stuck state detection transitioning door to 'unknown'
 *  v1.2.2    08/28/26    jshimota    Resolved NPE safeguards on uninitialized contact attributes
 *  v1.2.1    08/28/26    jshimota    Cleaned up header block layout
 *  v1.2.0    08/28/26    jshimota    Applied master driver template, added dual-contact calculation logic
 *  v1.1.1    06/22/26    jshimota    Fixed logging preference mappings
 *  v1.1.0    06/22/26    jshimota    Fixed wrnEnable reference bug
 *  v1.0.0    01/01/21    muxa        Original Source Release
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.5.1' }
def timeStamp() { return "2026/08/28 03:30 PM" }

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
        attribute "alexaState", "string"
        attribute "shadowLockState", "string"
        
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
        input name: "enableShadowLock", type: "bool", title: "<b>Create Alexa Child Shadow Lock Device (Option A)</b>", description: "Creates a child Virtual Lock device used exclusively by Alexa for zero-timeout voice response.", defaultValue: true, required: true, submitOnChange: true
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
    if (currentDoorState != "open" && currentDoorState != "opening") {
        logInfo "open() command received. Setting state to opening and pulsing relay."
        sendEvent(name: "relayTrigger", value: "pulse", isStateChange: true, descriptionText: "Triggering relay for open command")
        sendEvent(name: "door", value: "opening", isStateChange: true, descriptionText: "garage door is opening")
        sendEvent(name: "alexaState", value: "PENDING", isStateChange: true)
        
        syncChildShadowLock("unlocked")
        renderNotificationTile("Door state changed to opening", "opening", null, null)

        Integer timeoutSec = (settings?.transitionTimeout != null) ? (settings.transitionTimeout as Integer) : 15
        runIn(timeoutSec, "transitionSafetyCheck")
    } else {
        logDebug "open() requested but door is already open or opening."
    }
}

void close() {
    String currentDoorState = device.currentValue("door")
    logTrace "close() called | current door state: ${currentDoorState}"
    if (currentDoorState != "closed" && currentDoorState != "closing") {
        logInfo "close() command received. Setting state to closing and pulsing relay."
        sendEvent(name: "relayTrigger", value: "pulse", isStateChange: true, descriptionText: "Triggering relay for close command")
        sendEvent(name: "door", value: "closing", isStateChange: true, descriptionText: "garage door is closing")
        sendEvent(name: "alexaState", value: "PENDING", isStateChange: true)
        
        syncChildShadowLock("locked")
        renderNotificationTile("Door state changed to closing", "closing", null, null)

        Integer timeoutSec = (settings?.transitionTimeout != null) ? (settings.transitionTimeout as Integer) : 15
        runIn(timeoutSec, "transitionSafetyCheck")
    } else {
        logDebug "close() requested but door is already closed or closing."
    }
}

// Child Shadow Device Manager (Option A)
private void checkAndCreateChildShadowDevice() {
    Boolean shadowEnabled = settings?.enableShadowLock != false
    String childNetworkId = "${device.deviceNetworkId}-ShadowLock"
    def childDevice = getChildDevice(childNetworkId)

    if (shadowEnabled && !childDevice) {
        logInfo "Creating Child Alexa Shadow Lock Device (${childNetworkId})..."
        try {
            childDevice = addChildDevice("hubitat", "Generic Virtual Lock", childNetworkId, [
                name: "${device.displayName} Alexa Lock",
                label: "${device.displayName} Alexa Lock",
                isComponent: false
            ])
            logInfo "Child Alexa Shadow Lock created successfully."
            sendEvent(name: "shadowLockState", value: "ACTIVE")
        } catch (Exception e) {
            logError "Failed to create Child Virtual Lock: ${e.message}. Ensure 'Generic Virtual Lock' system driver is available."
        }
    } else if (!shadowEnabled && childDevice) {
        logWarn "Shadow Lock disabled in preferences. Removing child device ${childNetworkId}..."
        deleteChildDevice(childNetworkId)
        sendEvent(name: "shadowLockState", value: "INACTIVE")
    } else if (shadowEnabled && childDevice) {
        sendEvent(name: "shadowLockState", value: "ACTIVE")
    } else {
        sendEvent(name: "shadowLockState", value: "INACTIVE")
    }
}

// Handler called when Child Device commands are triggered directly by Alexa
void componentLock(def childDevice) {
    logInfo "Alexa Voice Command received via Child Shadow Lock: LOCK -> Triggering garage CLOSE"
    close()
}

void componentUnlock(def childDevice) {
    logInfo "Alexa Voice Command received via Child Shadow Lock: UNLOCK -> Triggering garage OPEN"
    open()
}

private void syncChildShadowLock(String lockState) {
    String childNetworkId = "${device.deviceNetworkId}-ShadowLock"
    def childDevice = getChildDevice(childNetworkId)
    if (childDevice) {
        logDebug "Syncing Child Alexa Shadow Lock state to: ${lockState}"
        childDevice.parse([[name: "lock", value: lockState, descriptionText: "Alexa Shadow Lock set to ${lockState}"]])
    }
}

// External Contact Sensor Methods
void setClosedContact(String stateStr) {
    if (!stateStr) return
    String cleanState = stateStr.toLowerCase()
    sendEvent(name: "closedContact", value: cleanState, isStateChange: true, descriptionText: "closed contact sensor is ${cleanState}")
    logInfo "closed contact sensor is ${cleanState}"
    evaluateDoorState(cleanState, null)
}

void setOpenContact(String stateStr) {
    if (!stateStr) return
    String cleanState = stateStr.toLowerCase()
    sendEvent(name: "openContact", value: cleanState, isStateChange: true, descriptionText: "open contact sensor is ${cleanState}")
    logInfo "open contact sensor is ${cleanState}"
    evaluateDoorState(null, cleanState)
}

void updateContactStatus(String closedState, String openState) {
    if (!closedState || !openState) return
    String cleanClosed = closedState.toLowerCase()
    String cleanOpen = openState.toLowerCase()
    
    unschedule("transitionSafetyCheck")
    sendEvent(name: "closedContact", value: cleanClosed, isStateChange: true, descriptionText: "closed contact sensor is ${cleanClosed}")
    sendEvent(name: "openContact", value: cleanOpen, isStateChange: true, descriptionText: "open contact sensor is ${cleanOpen}")
    evaluateDoorState(cleanClosed, cleanOpen)
}

void updateNotificationTile(String tileStr) {
    sendEvent(name: "notificationTile", value: tileStr, isStateChange: true, descriptionText: "notification tile updated")
}

// HTML Tile Generator Engine with Explicit Locked / Unlocked Center Badge
private void renderNotificationTile(String reason = "", String doorOverride = null, String closedOverride = null, String openOverride = null) {
    if (settings?.enableHtmlFormatting == false) return

    String timeStr = new Date().format("MM/dd hh:mm a", location.timeZone ?: TimeZone.getDefault())
    String doorState = (doorOverride ?: device.currentValue("door") ?: "UNKNOWN").toUpperCase()
    String closedState = closedOverride ?: device.currentValue("closedContact") ?: "open"
    String openState = openOverride ?: device.currentValue("openContact") ?: "open"
    Boolean shadowEnabled = settings?.enableShadowLock != false
    
    String childNetworkId = "${device.deviceNetworkId}-ShadowLock"
    def shadowChild = getChildDevice(childNetworkId)
    String shadowVal = shadowChild ? (shadowChild.currentValue("lock")?.toUpperCase() ?: "LOCKED") : (shadowEnabled ? "LOCKED" : "OFF")

    String titleStr = "Garage Door Status"

    String tSize = sanitizeFontSize(settings?.prefTitleFontSize, "1.0em")
    String sSize = sanitizeFontSize(settings?.prefStatusFontSize, "0.85em")
    String cSize = sanitizeFontSize(settings?.prefSensorFontSize, "0.77em")
    String uSize = sanitizeFontSize(settings?.prefUpdatedFontSize, "0.70em")

    String tWeight = (settings?.prefBoldTitle != false) ? "font-weight:bold;" : "font-weight:normal;"
    String sWeight = (settings?.prefBoldStatus != false) ? "font-weight:bold;" : "font-weight:normal;"
    String cWeight = (settings?.prefBoldSensors != false) ? "font-weight:bold;" : "font-weight:normal;"

    String stateBgColor = "#2e7d32"
    if (doorState == "OPEN") stateBgColor = "#c62828"
    else if (doorState in ["OPENING", "CLOSING"]) stateBgColor = "#ef6c00"
    else if (doorState == "UNKNOWN") stateBgColor = "#6a1b9a"

    String closedBgColor = closedState == "closed" ? "#2e7d32" : "#c62828"
    String openBgColor = openState == "closed" ? "#2e7d32" : "#c62828"

    if (closedState == "open" && openState == "open") {
        closedBgColor = "#ef6c00"
        openBgColor = "#ef6c00"
    }

    // Shadow Badge Styling (Explicit Locked / Unlocked Text)
    String shadowBgColor = shadowVal == "LOCKED" ? "#2e7d32" : (shadowVal == "UNLOCKED" ? "#c62828" : "#757575")
    String shadowText = shadowEnabled ? (shadowVal == "LOCKED" ? "LOCKED" : "UNLOCKED") : "OFF"
    String alexaBadge = "<span style='background:${shadowBgColor}; color:#fff; padding:1px 4px; border-radius:3px; font-weight:bold; font-size:0.85em; letter-spacing:0.2px;'>${shadowText}</span>"

    String closedBadge = "<span style='background:${closedBgColor}; color:#fff; padding:1px 5px; border-radius:4px; ${cWeight} font-size:${cSize}; letter-spacing:0.3px;'>${closedState.toUpperCase()}</span>"
    String openBadge = "<span style='background:${openBgColor}; color:#fff; padding:1px 5px; border-radius:4px; ${cWeight} font-size:${cSize}; letter-spacing:0.3px;'>${openState.toUpperCase()}</span>"

    String htmlOutput = """<div style='height:100%; width:100%; box-sizing:border-box; padding:4px 6px; font-family:sans-serif; background:#181818; color:#fff; border-radius:4px; overflow:hidden; display:flex; flex-direction:column; justify-content:space-between;'>
        <div style='display:flex; justify-content:space-between; align-items:center; gap:4px;'>
            <span style='font-size:${tSize}; ${tWeight} white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:65%;' title='${titleStr}'>${titleStr}</span>
            <span style='background:${stateBgColor}; color:#fff; padding:2px 6px; border-radius:4px; ${sWeight} font-size:${sSize}; letter-spacing:0.5px;'>${doorState}</span>
        </div>
        <div style='display:flex; justify-content:space-between; align-items:flex-end; font-size:${cSize}; color:#ccc; margin:2px 0;'>
            <div style='display:flex; flex-direction:column; align-items:flex-start; gap:2px;'>
                <span style='color:#aaa; font-size:0.88em;'>Closed Sensor</span>
                ${closedBadge}
            </div>
            <div style='display:flex; flex-direction:column; align-items:center; gap:2px;'>
                <span style='color:#888; font-size:0.75em;'>Shadow</span>
                ${alexaBadge}
            </div>
            <div style='display:flex; flex-direction:column; align-items:flex-end; gap:2px;'>
                <span style='color:#aaa; font-size:0.88em;'>Open Sensor</span>
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

    if (closedSensor == null || openSensor == null) return

    String resolvedDoorState = currentDoorState
    String reasonText = ""

    if (closedSensor == "closed" && openSensor == "open") {
        logInfo "Door reached physical position: FULLY CLOSED"
        unschedule("transitionSafetyCheck")
        resolvedDoorState = "closed"
        reasonText = "Door state changed to closed"
        
        sendEvent(name: "door", value: "closed", isStateChange: true, descriptionText: "garage door is closed")
        sendEvent(name: "alexaState", value: "SETTLED", isStateChange: true)
        syncChildShadowLock("locked")
    } 
    else if (closedSensor == "open" && openSensor == "closed") {
        logInfo "Door reached physical position: FULLY OPEN"
        unschedule("transitionSafetyCheck")
        resolvedDoorState = "open"
        reasonText = "Door state changed to open"
        
        sendEvent(name: "door", value: "open", isStateChange: true, descriptionText: "garage door is open")
        sendEvent(name: "alexaState", value: "SETTLED", isStateChange: true)
        syncChildShadowLock("unlocked")
    } 
    else if (closedSensor == "open" && openSensor == "open") {
        logInfo "Both contact sensors report OPEN (Door is moving mid-travel)"
        if (currentDoorState == "closed" || currentDoorState == "opening") {
            resolvedDoorState = "opening"
            reasonText = "Door state changed to opening"
        } else if (currentDoorState == "open" || currentDoorState == "closing") {
            resolvedDoorState = "closing"
            reasonText = "Door state changed to closing"
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
    String closedSensor = device.currentValue("closedContact")
    String openSensor = device.currentValue("openContact")

    if (currentDoorState == "opening" && openSensor != "closed") {
        logWarn "Safety Check: Door state reported OPENING, but open contact sensor is NOT closed after timeout! Marking door state as UNKNOWN."
        sendEvent(name: "door", value: "unknown", isStateChange: true, descriptionText: "garage door is stuck mid-travel (unknown state)")
        sendEvent(name: "alexaState", value: "SETTLED", isStateChange: true)
        renderNotificationTile("Stalled/stuck safety timeout", "unknown", null, null)
    } else if (currentDoorState == "closing" && closedSensor != "closed") {
        logWarn "Safety Check: Door state reported CLOSING, but closed contact sensor is NOT closed after timeout! Marking door state as UNKNOWN."
        sendEvent(name: "door", value: "unknown", isStateChange: true, descriptionText: "garage door is stuck mid-travel (unknown state)")
        sendEvent(name: "alexaState", value: "SETTLED", isStateChange: true)
        renderNotificationTile("Stalled/stuck safety timeout", "unknown", null, null)
    }
}

private void syncStyleAttributes() {
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
    sendEvent(name: "alexaState", value: "SETTLED")
    checkAndCreateChildShadowDevice()
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    syncStyleAttributes()
    checkAndCreateChildShadowDevice()
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    renderNotificationTile("Driver preferences updated", null, null, null)
    initialize(false)
}

def configure() {
    checkAndLogVersionDemarcation()
    sendEvent(name: "driverVersion", value: version())
    syncStyleAttributes()
    checkAndCreateChildShadowDevice()
    initialize(false)
    return []
}

def refresh() {
    checkAndLogVersionDemarcation()
    evaluateDoorState()
    syncStyleAttributes()
    checkAndCreateChildShadowDevice()
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    return []
}

private void initialize(Boolean isInstall = false) {
    unschedule("disableDebugLogging")
    if (device.currentValue("door") == null) sendEvent(name: "door", value: "unknown")
    if (device.currentValue("alexaState") == null) sendEvent(name: "alexaState", value: "SETTLED")
    syncStyleAttributes()

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
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

void clearAllDriverStates() { state.clear() }
void clearAllAttributes() { device.properties.supportedAttributes.each { device.deleteCurrentState("$it") } }
void clearAllSchedules() { unschedule() }

private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    String nameStr = args.name as String
    String oldVal = device.currentValue(nameStr)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        String desc = args.descriptionText ?: "${nameStr} set to ${args.value}"
        Map eventMap = [name: nameStr, value: args.value, descriptionText: desc]
        if (args.unit) eventMap.unit = args.unit
        if (args.type) eventMap.type = args.type
        if (args.isStateChange != null) eventMap.isStateChange = args.isStateChange

        sendEvent(eventMap)
        logInfo "${desc}"
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