/**
 * Virtual Custom HTML Dashboard Tile Driver (Unlimited String Length Version)
 **/

static String version() { return '1.1.0' }

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

        attribute "driverVersion", "string"
        attribute "tile", "string"

        command "refreshTile"
        command "setHtmlContent", [[name: "HTML Content*", type: "STRING", description: "Paste full raw HTML string here (no character limit)"]]
        command "clearHtmlContent"
    }

    preferences {
        input name: "htmlContentPref", type: "textarea", title: "<b>Raw HTML Fallback (Max 255 chars)</b>", description: "Use the 'setHtmlContent' command above for longer HTML payloads.", defaultValue: "", required: false, submitOnChange: true

        input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false, required: true
    }
}

void parse(String description) { logDebug "parse(): ${description}" }

def refreshTile() { updateTileDisplay() }
def refresh() { refreshTile(); return [] }

def setHtmlContent(String rawHtml) {
    if (rawHtml != null) {
        state.customHtml = rawHtml
        updateTileDisplay(rawHtml)
        logInfo "Custom HTML stored via command (${rawHtml.length()} characters)."
    }
}

def clearHtmlContent() {
    state.remove("customHtml")
    updateTileDisplay()
    logInfo "Cleared stored HTML state."
}

private void updateTileDisplay(String overrideHtml = null) {
    String currentHtml = overrideHtml ?: state.customHtml ?: settings?.htmlContentPref ?: "<div style='padding:10px; text-align:center;'><b>HTML Tile Test</b></div>"
    
    sendIfChanged([name: "tile", value: currentHtml, descriptionText: "Dashboard HTML Tile Updated"])
}

void installed() { initialize() }
void updated() { initialize() }

private void initialize() {
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    updateTileDisplay()
}

private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    String nameStr = args.name as String
    String oldVal = device.currentValue(nameStr)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        sendEvent([name: nameStr, value: args.value, descriptionText: args.descriptionText])
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    if (getSettingBool("log${lowerLevel.capitalize()}Enable", lowerLevel == "info")) {
        log."${lowerLevel}" "${device.displayName}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}