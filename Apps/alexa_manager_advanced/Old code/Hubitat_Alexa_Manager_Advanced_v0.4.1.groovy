/**
 * Alexa Manager Advanced - Hubitat Integration
 * Version: 0.4.0
 * Requires: Hubitat Elevation C-7, local network access to AMA NAS service
 *
 * Registers selected switch devices with AMA, pushes switch state changes,
 * and accepts local TurnOn/TurnOff callbacks from AMA.
 */
 /**
 * Changelog History:
 * v0.4.1    11/05/26    jshimota    Added automatic app-label version display with optional setting.
 * v0.4.0    11/04/26    jshimota    Initial working AMA Hubitat integration.
 **/

static String version() { return '0.4.1' }
String timeStamp() { return "2026/11/05" }

definition(
    name: "Alexa Manager Advanced",
    namespace: "jshimota",
    author: "Alexa Manager Advanced",
    description: "Local Hubitat integration for Alexa Manager Advanced on Synology NAS",
    category: "Integrations",
    singleInstance: true,
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: ""
)

preferences {
    page(name: "mainPage", title: "Alexa Manager Advanced", install: true, uninstall: true)
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        section("AMA NAS Service") {
            input "amaUrl", "text", title: "AMA Base URL", description: "Example: http://192.168.1.12:4100", required: true
            input "amaApiKey", "password", title: "AMA API Key", required: true
        }
        section("Devices to expose") {
            input "switchDevices", "capability.switch", title: "Select switches", multiple: true, required: true
        }
        section("Integration status") {
            input "showVersionInLabel", "bool", title: "Show Version in App Label?", defaultValue: true
            paragraph "Selected switches are registered with AMA. State changes are pushed over your LAN. AMA sends commands back through this app's local endpoint."
            if (state.lastSync) paragraph "Last successful sync: ${state.lastSync}"
            if (state.lastError) paragraph "Last error: ${state.lastError}"
        }
    }
}

def installed() {
    initialize()
    updateAppLabel()
}

def updated() {
    unsubscribe()
    initialize()
    updateAppLabel()
}

private void updateAppLabel() {
    boolean showVersion = (settings.showVersionInLabel == null) ? true : settings.showVersionInLabel.toString().toBoolean()
    String baseLabel = "Alexa Manager Advanced"
    if (showVersion) baseLabel += " v${version()}"
    if (app.label != baseLabel) app.updateLabel(baseLabel)
}

def initialize() {
    if (!state.accessToken) {
        try { createAccessToken() } catch (e) { log.error "Could not create app access token: ${e.message}"; state.lastError = "Could not create local app token"; return }
    }
    def selectedIds = (switchDevices ?: []).collect { "${it.id}" } as Set
    ((state.registeredDeviceIds ?: []) as List).findAll { !(it in selectedIds) }.each { oldId -> deregisterDevice(oldId) }
    (switchDevices ?: []).each { dev ->
        subscribe(dev, "switch", switchEventHandler)
        registerDevice(dev)
    }
    state.registeredDeviceIds = selectedIds as List
    state.initializedAt = new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX", location.timeZone)
}

def baseUrl() {
    (amaUrl ?: "").trim().replaceAll('/+$', '')
}

def appCommandUrl() {
    return "${getFullLocalApiServerUrl()}/command?access_token=${state.accessToken}"
}

def endpointIdFor(dev) { "hubitat-${dev.id}" }

def registerDevice(dev) {
    def endpointId = endpointIdFor(dev)
    def payload = [
        endpointId: endpointId,
        hubitatDeviceId: "${dev.id}",
        friendlyName: dev.displayName ?: dev.name,
        description: "Hubitat switch integrated through Alexa Manager Advanced",
        modelName: dev.typeName ?: "Hubitat Switch",
        displayCategories: ["SWITCH"],
        powerState: dev.currentValue("switch") == "on" ? "ON" : "OFF",
        commandUrl: appCommandUrl(),
        enabled: true
    ]
    try {
        httpPutJson([uri: baseUrl(), path: "/api/v1/devices/${endpointId}", headers: apiHeaders(), body: payload, timeout: 10]) { resp ->
            if (resp.status >= 200 && resp.status < 300) { state.lastSync = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone); state.remove("lastError") }
            else { state.lastError = "Device registration HTTP ${resp.status}" }
        }
    } catch (e) { state.lastError = "Registration failed: ${e.message}"; log.warn state.lastError }
}

def switchEventHandler(evt) {
    def dev = (switchDevices ?: []).find { "${it.id}" == "${evt.deviceId}" }
    if (!dev) return
    def endpointId = endpointIdFor(dev)
    def powerState = evt.value == "on" ? "ON" : "OFF"
    try {
        httpPutJson([uri: baseUrl(), path: "/api/v1/devices/${endpointId}/state", headers: apiHeaders(), body: [powerState: powerState], timeout: 8]) { resp ->
            if (resp.status >= 200 && resp.status < 300) { state.lastSync = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone); state.remove("lastError") }
            else { state.lastError = "State sync HTTP ${resp.status}" }
        }
    } catch (e) { state.lastError = "State sync failed: ${e.message}"; log.warn state.lastError }
}

def deregisterDevice(deviceId) {
    def endpointId = "hubitat-${deviceId}"
    try {
        httpDelete([uri: baseUrl(), path: "/api/v1/devices/${endpointId}", headers: apiHeaders(), timeout: 8]) { resp -> }
    } catch (e) { log.warn "Could not deregister ${endpointId}: ${e.message}" }
}

def uninstalled() {
    unsubscribe()
    ((state.registeredDeviceIds ?: []) as List).each { deregisterDevice(it) }
}

def apiHeaders() { [Authorization: "Bearer ${amaApiKey}", Accept: "application/json"] }

mappings {
    path("/command") { action: [POST: "commandHandler"] }
}

def commandHandler() {
    def body = request.JSON ?: [:]
    def deviceId = body.hubitatDeviceId?.toString()
    def command = body.command?.toString()
    def dev = (switchDevices ?: []).find { "${it.id}" == deviceId }
    if (!dev || body.endpointId != endpointIdFor(dev)) {
        render status: 404, contentType: "application/json", data: '{"error":"unknown_device"}'
        return
    }
    if (!(command in ["ON", "OFF"])) {
        render status: 400, contentType: "application/json", data: '{"error":"unsupported_command"}'
        return
    }
    try {
        if (command == "ON") dev.on() else dev.off()
        render status: 202, contentType: "application/json", data: groovy.json.JsonOutput.toJson([accepted: true, endpointId: body.endpointId, command: command])
    } catch (e) {
        log.warn "Hubitat command failed for ${dev.displayName}: ${e.message}"
        render status: 500, contentType: "application/json", data: '{"error":"command_failed"}'
    }
}
