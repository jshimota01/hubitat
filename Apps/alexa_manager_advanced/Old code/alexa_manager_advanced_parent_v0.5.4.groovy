/**
 * Alexa Manager Advanced (AMA) - Parent Application
 * Platform: Hubitat Elevation C-7
 * Namespace: jshimota
 * Version: 0.5.4
 *
 * Changelog:
 * v0.5.4 - 2026/11/10 - Isolates child notification failures during inventory delegation.
 *   - A failure in one child no longer prevents delivery to the other child.
 * v0.5.3 - 2026/11/09 - Formalized parent-centered, hub-and-spoke module communication.
 *   - All child-to-child communication is prohibited; all requests and updates route through parent APIs.
 *   - Parent receives module status and inventory updates, then delegates to other children.
 *   - Adds public APIs for module status, inventory notifications, and management records.
 *   - Preserves installed app identities and existing child state.
 * v0.5.2 - 2026/11/09 - Clarified child application names for source/version management.
 *   - Renames all three branch applications with the explicit 'Child' suffix.
 * v0.5.0 - 2026/11/09 - Initial parent/child architecture foundation.
 *   - Replaces the single-page v0.4.x integration layout with a parent application.
 *   - Adds Device Handling, Device Management, and Monitoring & Analytics branches.
 *   - Retains the existing AMA NAS URL/API key preference names for transition compatibility.
 *   - Adds parent delegation methods for child coordination and a shared logging policy.
 *   - Does not automatically expose, register, or remove any device from Amazon.
 *
 * Design boundary:
 * Hubitat remains authoritative for device state and commands.
 * AMA NAS remains the integration/registry service.
 * Child applications communicate with the NAS through the parent-owned service boundary.
 * Hubitat-side InfluxDB reporting will be an independent destination, not routed through NAS.
 */

String version() { return '0.5.4' }
String timeStamp() { return '2026/11/10' }

definition(
    name: 'Alexa Manager Advanced',
    namespace: 'jshimota',
    author: 'James Shimota',
    description: 'Local-first Hubitat parent application for Alexa Manager Advanced.',
    category: 'Integrations',
    singleInstance: true,
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: '',
    importUrl: ''
)

preferences {
    page(name: 'mainPage')
}

mappings {
    path('/command') { action: [POST: 'commandHandler'] }
}

def mainPage() {
    updateAppLabel()
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF;margin:0;font-size:22px;font-weight:600;'>Alexa Manager Advanced</h2>" +
                      "<span style='font-size:12px;opacity:0.85;'>Hubitat Parent Application v${version()} (${timeStamp()})</span></div>"
            paragraph "Hubitat is the source of truth for device state and commands. AMA manages discovery, interpretation, approval, Amazon translation, and integration health."
        }

        section('APPLICATION BRANCHES') {
            paragraph "<div style='color:#555;font-size:12px;margin-bottom:8px;'>Each branch has a defined responsibility. Device discovery does not automatically expose devices to Amazon.</div>"
            app(name: 'amaDeviceHandling', appName: 'AMA Device Handling Child', namespace: 'jshimota', title: '<b>Configure Device Handling (Discovery & Inventory)</b>', multiple: false)
            app(name: 'amaDeviceManagement', appName: 'AMA Device Management Child', namespace: 'jshimota', title: '<b>Configure Device Management (Review & Amazon Exposure)</b>', multiple: false)
            app(name: 'amaMonitoringAnalytics', appName: 'AMA Monitoring & Analytics Child', namespace: 'jshimota', title: '<b>Configure Monitoring & Analytics</b>', multiple: false)
        }

        section('AMA NAS SERVICE') {
            input name: 'amaUrl', type: 'text', title: 'AMA NAS Base URL', description: 'Local URL for the AMA service, for example http://192.168.1.23:4100', required: true, submitOnChange: true
            input name: 'amaApiKey', type: 'password', title: 'AMA API Key', required: true
            paragraph "<div style='font-size:12px;color:#666;'>The parent owns the Hubitat-to-NAS connection settings. Child applications request service operations through the parent.</div>"
        }

        section('REGISTRY & INTEGRATION STATUS') {
            Map inventoryStatus = getInventoryStatus()
            int discovered = (inventoryStatus.deviceCount ?: 0) as Integer
            paragraph "<b>Last device inventory:</b> ${inventoryStatus.lastScan ?: 'Not scanned yet'}"
            paragraph "<b>Devices discovered:</b> ${discovered}"
            paragraph "<b>NAS service:</b> ${state.nasStatus ?: 'Not tested'}"
            if (state.lastError) paragraph "<span style='color:#B03A2E;'><b>Last error:</b> ${state.lastError}</span>"
        }

        section('LEGACY ENDPOINT COMPATIBILITY', hideable: true, hidden: true) {
            input name: 'switchDevices', type: 'capability.switch', title: 'Existing v0.4.x Amazon switch endpoints', multiple: true, required: false
            paragraph 'This retained selection keeps existing v0.4.x command callbacks and state updates operating during the transition. It does not add new devices to Amazon.'
        }

        section('APP PREFERENCES', hideable: true, hidden: true) {
            input name: 'showVersionInLabel', type: 'bool', title: 'Show Version in App Label?', defaultValue: true
            input name: 'logInfoEnable', type: 'bool', title: 'Logging - Enable Info Logging', defaultValue: true
            input name: 'logDebugEnable', type: 'bool', title: 'Logging - Enable Debug Logging', defaultValue: false
            input name: 'logTraceEnable', type: 'bool', title: 'Logging - Enable Trace Logging', defaultValue: false
        }
    }
}

void installed() {
    logInfo "Installing Alexa Manager Advanced v${version()} (${timeStamp()})"
    initialize()
}

void updated() {
    logInfo 'Updating Alexa Manager Advanced parent application.'
    initialize()
}

void uninstalled() {
    logInfo 'Alexa Manager Advanced parent uninstalled. No Amazon endpoint deletion is performed by the parent.'
}

private void initialize() {
    updateAppLabel()
    state.installationVersion = version()
    state.initializedAt = now()
    state.moduleCommunicationModel = 'PARENT_HUB_AND_SPOKE'
    ensureLegacyCommandEndpoint()
    /* Preserve the already-exposed v0.4.1 switches during the architecture transition.
       No registration or deregistration is performed here. */
    (settings.switchDevices ?: []).each { dev -> subscribe(dev, 'switch', legacySwitchEventHandler) }
    logInfo "Parent initialized. Child applications: ${getChildApps()?.size() ?: 0}. Legacy switch callbacks retained: ${(settings.switchDevices ?: []).size()}."
}

private void ensureLegacyCommandEndpoint() {
    if (!state.accessToken) {
        try { createAccessToken() }
        catch (Exception e) { state.lastError = "Could not create local command token: ${e.message}"; logWarn state.lastError }
    }
}

def appCommandUrl() {
    return state.accessToken ? "${getFullLocalApiServerUrl()}/command?access_token=${state.accessToken}" : null
}

def endpointIdFor(dev) { "hubitat-${dev.id}" }

void legacySwitchEventHandler(evt) {
    def dev = (settings.switchDevices ?: []).find { "${it.id}" == "${evt.deviceId}" }
    if (!dev || !isAmaConfigured()) return
    String endpointId = endpointIdFor(dev)
    String powerState = evt.value == 'on' ? 'ON' : 'OFF'
    try {
        httpPutJson([uri: getAmaBaseUrl(), path: "/api/v1/devices/${endpointId}/state", headers: getAmaApiHeaders(), body: [powerState: powerState], timeout: 8]) { resp ->
            if (resp.status >= 200 && resp.status < 300) { state.lastSync = now(); state.remove('lastError') }
            else state.lastError = "Legacy state sync HTTP ${resp.status}"
        }
    } catch (Exception e) { state.lastError = "Legacy state sync failed: ${e.message}"; logWarn state.lastError }
}

def commandHandler() {
    Map body = request.JSON ?: [:]
    String deviceId = body.hubitatDeviceId?.toString()
    String command = body.command?.toString()
    def dev = (settings.switchDevices ?: []).find { "${it.id}" == deviceId }
    if (!dev || body.endpointId != endpointIdFor(dev)) {
        render status: 404, contentType: 'application/json', data: '{"error":"unknown_device"}'
        return
    }
    if (!(command in ['ON','OFF'])) {
        render status: 400, contentType: 'application/json', data: '{"error":"unsupported_command"}'
        return
    }
    try {
        if (command == 'ON') dev.on() else dev.off()
        render status: 202, contentType: 'application/json', data: groovy.json.JsonOutput.toJson([accepted:true, endpointId:body.endpointId, command:command])
    } catch (Exception e) {
        logWarn "Legacy Hubitat command failed for ${dev.displayName}: ${e.message}"
        render status: 500, contentType: 'application/json', data: '{"error":"command_failed"}'
    }
}

private void updateAppLabel() {
    boolean showVersion = settings.showVersionInLabel == null || settings.showVersionInLabel.toString().toBoolean()
    String expected = 'Alexa Manager Advanced' + (showVersion ? " v${version()}" : '')
    if (app.label != expected) app.updateLabel(expected)
}

/* Parent-owned service boundary. Child apps must not each maintain separate NAS credentials. */
public String getAmaBaseUrl() {
    return (settings.amaUrl ?: '').toString().trim().replaceAll('/+$', '')
}

public Map getAmaApiHeaders() {
    return [Authorization: "Bearer ${settings.amaApiKey ?: ''}", Accept: 'application/json']
}

public Boolean isAmaConfigured() {
    return (getAmaBaseUrl() && settings.amaApiKey) ? true : false
}

public Map getInventoryStatus() {
    def child = getAmaChild('AMA Device Handling Child')
    if (child) return child.getInventoryStatus() ?: [:]
    return [deviceCount: 0, lastScan: null]
}

public Map getDeviceInventory() {
    def child = getAmaChild('AMA Device Handling Child')
    if (child) return child.getDeviceInventory() ?: [:]
    return [:]
}

public Map getDeviceInventoryEntry(String deviceId) {
    if (!deviceId) return [:]
    def child = getAmaChild('AMA Device Handling Child')
    if (child) return child.getDeviceInventoryEntry(deviceId.toString()) ?: [:]
    return [:]
}

public void requestDeviceInventoryScan(String reason = 'Parent request') {
    def child = getAmaChild('AMA Device Handling Child')
    if (child) child.scanDeviceInventory(reason)
    else logWarn 'Device Handling child is not installed; inventory scan request ignored.'
}

/*
 * Public parent communication contract. Children call these methods; the parent is
 * the ONLY route between branches. No child receives or calls another child reference.
 */
public Map getModuleStatus() {
    Map result = [parent:[name:'Alexa Manager Advanced', version:version()]]
    ['AMA Device Handling Child','AMA Device Management Child','AMA Monitoring & Analytics Child'].each { childName ->
        def child = getAmaChild(childName)
        result[childName] = child ? (child.getModuleStatus() ?: [installed:true]) : [installed:false]
    }
    return result
}

public void childInventoryUpdated(Map inventoryStatus = [:]) {
    Map safeStatus = inventoryStatus ?: [:]
    state.lastInventoryUpdate = now()
    state.lastInventoryStatus = safeStatus
    logInfo "Received inventory update from Device Handling (${safeStatus.deviceCount ?: 0} records)."

    /* Each branch is isolated: one child failure must not block another child. */
    try {
        def management = getAmaChild('AMA Device Management Child')
        if (management) management.onInventoryUpdated(safeStatus)
        state.remove('lastManagementNotificationError')
    } catch (Exception e) {
        logWarn "Could not notify Device Management child: ${e.message}"
        state.lastManagementNotificationError = e.message?.toString()
    }

    try {
        def monitoring = getAmaChild('AMA Monitoring & Analytics Child')
        if (monitoring) monitoring.recordParentEvent('inventory.updated', [status:safeStatus])
        state.remove('lastMonitoringNotificationError')
    } catch (Exception e) {
        logWarn "Could not notify Monitoring & Analytics child: ${e.message}"
        state.lastMonitoringNotificationError = e.message?.toString()
    }
}

public Map getManagementRecords() {
    def management = getAmaChild('AMA Device Management Child')
    return management ? (management.getManagementRecords() ?: [:]) : [:]
}

public Map getManagementRecord(String deviceId) {
    if (!deviceId) return [:]
    def management = getAmaChild('AMA Device Management Child')
    return management ? (management.getManagementRecord(deviceId.toString()) ?: [:]) : [:]
}

public void saveManagementRecord(String deviceId, Map record = [:]) {
    if (!deviceId) return
    def management = getAmaChild('AMA Device Management Child')
    if (management) management.saveManagementRecord(deviceId.toString(), record ?: [:])
}

public void reportChildEvent(String eventName, Map payload = [:]) {
    def monitoring = getAmaChild('AMA Monitoring & Analytics Child')
    if (monitoring) monitoring.recordParentEvent(eventName ?: 'child.event', payload ?: [:])
}

private Object getAmaChild(String appName) {
    return getChildApps()?.find { child ->
        child.name == appName || child.typeName == appName || child.label?.toString()?.startsWith(appName)
    }
}

private void logMessage(String level, String message) {
    String normalized = (level ?: 'info').toLowerCase()
    String setting = "log${normalized.capitalize()}Enable"
    if (settings[setting] == false) return
    log."${normalized}" "AMA Parent: ${message}"
}

private void logInfo(String message)  { logMessage('info', message) }
private void logDebug(String message) { logMessage('debug', message) }
private void logTrace(String message) { logMessage('trace', message) }
private void logWarn(String message)  { logMessage('warn', message) }
