/**
 * Virtual Custom HTML Dashboard Tile Driver
 * Platform: Hubitat Elevation
 * Purpose: Renders raw HTML from user preferences directly into the 'tile' attribute for dashboard testing across platforms (Android vs. Windows HD+).
 **/

static String version() { return '1.0.0' }
def timeStamp() { return "2026/10/13" }

metadata {
    definition (
        name: "Virtual HTML Test Tile Driver",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: ""
    ) {
        capability "Actuator"
        capability "Sensor"
        capability "Refresh"

        // Primary Display Attributes
        attribute "driverVersion", "string"
        attribute "tile", "string"

        // Direct Control Commands
        command "refreshTile"
        command "setHtmlContent", ["string"]
    }

    preferences {
        input name: "htmlContent", type: "textarea", title: "<b>Raw HTML Markup</b>", description: "Enter the custom HTML string to be displayed on your dashboard tile.", defaultValue: "<div style='padding:10px; text-align:center;'><b>HTML Tile Test</b></div>", required: false, submitOnChange: true

        // Logging Controls
        input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false, required: true
    }
}

void parse(String description) {
    logDebug "parse(): ${description}"
}

def refreshTile() {
    logDebug "refreshTile() called"
    updateTileDisplay()
}

def refresh() {
    refreshTile()
    return []
}

def setHtmlContent(String rawHtml) {
    if (rawHtml != null) {
        device.updateSetting("htmlContent", [type: "textarea", value: rawHtml])
        updateTileDisplay(rawHtml)
    }
}

private void updateTileDisplay(String overrideHtml = null) {
    String currentHtml = overrideHtml ?: settings?.htmlContent ?: "<div style='padding:10px; text-align:center;'><b>HTML Tile Test</b></div>"
    
    sendIfChanged([name: "tile", value: currentHtml, descriptionText: "Dashboard HTML Tile Updated"])
    logInfo "Updated tile attribute with current HTML preference string."
}

// Hubitat Lifecycle Routines
void installed() {
    logInfo "Installing Virtual HTML Test Tile Driver v${version()}..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize()
}

void updated() {
    logInfo "Preferences updated..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize()
}

private void initialize() {
    updateTileDisplay()
}

// Utility Routine for State/Attribute De-Duplication
private void sendIfChanged(Map args) {
    if (!args || !args.name) return

    String nameStr = args.name as String
    String oldVal = device.currentValue(nameStr)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        Map eventMap = [
            name: nameStr, 
            value: args.value, 
            descriptionText: args.descriptionText ?: "${nameStr} set to ${args.value}"
        ]
        sendEvent(eventMap)
    }
}

// Logging Engine
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    
    if (getSettingBool(settingKey, lowerLevel == "info")) {
        log."${lowerLevel}" "${devName}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}