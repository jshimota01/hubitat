/**
 * v0.7.10 - 2026/11/11 - Removes unsupported top-level static fields and Java time parsing from the Hubitat app script.
 *   - Uses literal AMA skill identifiers inside methods; no class-field declarations are required.
 *   - Calculates snapshot freshness from the Parent's persisted successful-refresh timestamp.
 *   - Preserves the v0.7.8 inventory normalization, reconciliation, and child distribution behavior.
 * v0.7.9 - 2026/11/11 - Replaces Java Base64 decoding with Hubitat-sandbox-safe AMA skill identifier matching.
 *   - Recognizes the confirmed AMA development skill by its exact encoded driverIdentity identifier.
 *   - Avoids java.util.Base64 APIs rejected by Hubitat's Groovy sandbox.
 * v0.7.8 - 2026/11/11 - Adds parent-owned Amazon endpoint inventory and exposure reconciliation.
 *   - Refreshes the authenticated Amazon Smart Home inventory through the existing NAS API.
 *   - Normalizes endpoint owner, skill stage, Hubitat ID, enabled state, aliases, and reachability.
 *   - Separates AMA endpoints from legacy Hubitat and other integrations by Amazon skill identity.
 *   - Distributes read-only exposure observations to all child branches and exposes parent query APIs.
 *   - Retains the last successful snapshot on refresh failure; monitoring never triggers endpoint mutations.
 * v0.7.7 - 2026/11/11 - Adds parent-mediated NAS monitoring status retrieval for the AMA System Monitor.
 *   - Keeps NAS URL and API key ownership in the Parent.
 *   - Returns the authenticated NAS health snapshot to Monitoring & Analytics.
 *   - Preserves the parent-only inter-module communication boundary.
 * v0.7.6 - 2026/11/11 - Aligns the descriptive source header with the executable version(). No functional changes.
 * v0.7.5 - 2026/11/10 - Adds parent-owned forwarding of complete Hubitat telemetry events to the NAS.
 *   - Uses the existing parent-owned AMA URL and API key.
 *   - Sends the complete event envelope to the NAS telemetry ingestion endpoint.
 *   - Does not make child-to-child calls or expose NAS credentials in child preferences.
 * v0.7.4 - 2026/11/10 - Clears stale switch subscriptions and preserves callback compatibility.
 *   - Unsubscribes prior parent subscriptions before registering current callbacks.
 *   - Adds switchEventHandler compatibility wrapper for previously registered Hubitat callbacks.
 * v0.7.3 - 2026/11/10 - Forwards subscribed legacy switch state events to Monitoring & Analytics.
 *   - Adds Hubitat-originated switch event telemetry through the Parent-to-Monitoring branch.
 *   - Keeps NAS state synchronization independent and unchanged.
 *   - Telemetry forwarding is attempted even when NAS API settings are unavailable.
 * v0.7.2 - 2026/11/10 - Aligns the parent to the InfluxDB-enabled Monitoring & Analytics child and canonical branch identities.
 *   - Preserves legacy child-name lookup for existing installations.
 * v0.7.1 - 2026/11/10 - Aligns all four child app type names and parent references.
 *   - Uses Candidate Selection, Candidate Preparation, Exposure Management, and Monitoring & Analytics consistently.
 *   - Retains legacy-name fallback in child lookup to help existing installations transition safely.
 * Alexa Manager Advanced (AMA) - Parent Application
 * Platform: Hubitat Elevation C-7
 * Namespace: jshimota
 * Version: 0.7.10
 *
 * Changelog:
 * v0.7.10 - 2026/11/11 - Removes unsupported top-level static fields and Java time parsing from the Hubitat app script.
 *   - Uses literal AMA skill identifiers inside methods; no class-field declarations are required.
 *   - Calculates snapshot freshness from the Parent's persisted successful-refresh timestamp.
 *   - Preserves Amazon inventory normalization, reconciliation, and child distribution.
 * v0.7.9 - 2026/11/11 - Uses exact AMA skill identifier matching; removes unsupported Java Base64 decoding.
 * v0.7.8 - 2026/11/11 - Adds parent-owned Amazon endpoint inventory and exposure reconciliation.
 *   - Normalizes endpoint ownership and state, caches the last successful snapshot, and distributes it to each child.
 *   - Adds manual refresh and read-only per-device observation APIs.
 * v0.7.1 - 2026/11/10 - Establishes the four-branch Candidate-to-Exposure architecture.
 *   - Renames displayed branches to Candidate Selection, Candidate Preparation, and Exposure Management.
 *   - Adds the new Exposure Management child application entry.
 *   - Adds parent-mediated Exposure Management status and event routing.
 * v0.6.0 - 2026/11/10 - Establishes the first voice-intelligence and naming-learning foundation.
 *   - Updates subordinate application display titles while preserving installed child app type identities.
 *   - Adds parent-mediated voice-test reporting and retrieval APIs.
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

String version() { return '0.7.10' }
String timeStamp() { return '2026/11/11' }

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
            paragraph "Hubitat determines what a device can do; Alexa Smart Home determines what functionality AMA can expose. The Parent coordinates Candidate Selection, Candidate Preparation, Exposure Management, and Monitoring & Analytics."
        }

        section('APPLICATION BRANCHES') {
            paragraph "<div style='color:#555;font-size:12px;margin-bottom:8px;'>Each branch has a distinct responsibility. Candidate selection and preparation do not expose devices to Alexa.</div>"
            app(name: 'amaDeviceHandling', appName: 'AMA Candidate Selection', namespace: 'jshimota', title: '<b>1. Candidate Selection</b>', multiple: false)
            app(name: 'amaDeviceManagement', appName: 'AMA Candidate Preparation', namespace: 'jshimota', title: '<b>2. Candidate Preparation</b>', multiple: false)
            app(name: 'amaExposureManagement', appName: 'AMA Exposure Management', namespace: 'jshimota', title: '<b>3. Exposure Management</b>', multiple: false)
            app(name: 'amaMonitoringAnalytics', appName: 'AMA Monitoring & Analytics', namespace: 'jshimota', title: '<b>4. Monitoring & Analytics</b>', multiple: false)
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
            Map amazonStatus = getAmazonInventoryStatus()
            Map amazonCounts = amazonStatus.counts instanceof Map ? amazonStatus.counts : [:]
            paragraph "<b>Amazon endpoint inventory:</b> ${amazonStatus.endpointCount ?: 0} endpoints | ${amazonStatus.fresh == true ? 'CURRENT' : (amazonStatus.available == true ? 'STALE' : 'NOT CHECKED')}"
            int otherAmazonEndpoints = Math.max(0, ((amazonStatus.endpointCount ?: 0) as Integer) - ((amazonCounts.AMA ?: 0) as Integer) - ((amazonCounts.LEGACY_HUBITAT ?: 0) as Integer))
            paragraph "<b>AMA:</b> ${amazonCounts.AMA ?: 0} | <b>Legacy Hubitat:</b> ${amazonCounts.LEGACY_HUBITAT ?: 0} | <b>Other integrations / Amazon:</b> ${otherAmazonEndpoints}"
            String ownerBreakdown = amazonCounts.keySet().collect { key -> "${key}: ${amazonCounts[key]}" }.sort().join(' | ')
            paragraph "<b>Source breakdown:</b> ${ownerBreakdown ?: 'Not available'}"
            paragraph "<b>Last successful Amazon refresh:</b> ${amazonStatus.retrievedAt ?: 'Never'}"
            if (amazonStatus.lastError) paragraph "<span style='color:#B03A2E;'><b>Amazon inventory error:</b> ${amazonStatus.lastError}</span>"
            input name: 'btnRefreshAmazonInventory', type: 'button', title: 'Refresh Amazon Endpoint Inventory'
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

void appButtonHandler(String buttonName) {
    if (buttonName == 'btnRefreshAmazonInventory') {
        requestAmazonInventoryRefresh('Manual Parent refresh')
    }
}

void uninstalled() {
    logInfo 'Alexa Manager Advanced parent uninstalled. No Amazon endpoint deletion is performed by the parent.'
}

private void initialize() {
    // Remove callbacks retained from prior app versions before registering current handlers.
    unsubscribe()
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

// Compatibility entry point for callbacks retained from earlier app versions.
void switchEventHandler(evt) {
    legacySwitchEventHandler(evt)
}

void legacySwitchEventHandler(evt) {
    def dev = (settings.switchDevices ?: []).find { "${it.id}" == "${evt.deviceId}" }
    if (!dev) return

    String endpointId = endpointIdFor(dev)
    String powerState = evt.value == 'on' ? 'ON' : 'OFF'

    /*
     * Hubitat telemetry is independent of NAS state synchronization.
     * The Parent remains the only communication path to Monitoring & Analytics.
     * A telemetry failure must never prevent the existing NAS state update.
     */
    try {
        reportChildEvent('device.state.updated', [
            hubitatDeviceId: "${dev.id}",
            endpointId: endpointId,
            deviceName: dev.displayName?.toString() ?: dev.name?.toString() ?: "Device ${dev.id}",
            capability: 'switch',
            attribute: evt.name?.toString() ?: 'switch',
            value: evt.value?.toString(),
            powerState: powerState,
            unit: evt.unit?.toString(),
            description: evt.descriptionText?.toString(),
            eventSource: 'hubitat_device_event'
        ])
    } catch (Exception e) {
        logWarn "Could not forward switch event to Monitoring & Analytics for ${endpointId}: ${e.message}"
    }

    // Preserve the existing NAS state synchronization behavior.
    if (!isAmaConfigured()) return
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

/* Parent-owned telemetry transport. Children submit complete events to the Parent;
   the Parent forwards them to the NAS using its existing service credentials. */
public Boolean forwardTelemetryEvent(String eventName, Map payload = [:]) {
    if (!isAmaConfigured()) {
        logWarn 'Telemetry event not forwarded: AMA NAS connection is not configured.'
        return false
    }
    Map envelope = [
        event: eventName ?: 'hubitat_event',
        timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", location?.timeZone ?: TimeZone.getDefault()),
        payload: payload ?: [:]
    ]
    try {
        httpPostJson([uri: getAmaBaseUrl(), path: '/api/v1/telemetry/hubitat-event',
                      headers: getAmaApiHeaders(), body: envelope, timeout: 8]) { resp ->
            if (resp.status >= 200 && resp.status < 300) {
                state.lastTelemetryForwardAt = now()
                state.remove('lastTelemetryForwardError')
                logDebug "Forwarded telemetry event '${eventName}' to AMA NAS."
            } else {
                state.lastTelemetryForwardError = "Telemetry HTTP ${resp.status}"
                logWarn state.lastTelemetryForwardError
            }
        }
        return true
    } catch (Exception e) {
        state.lastTelemetryForwardError = "Telemetry forwarding failed: ${e.message}"
        logWarn state.lastTelemetryForwardError
        return false
    }
}

/* Parent-owned authenticated monitoring read. The Monitoring child never stores NAS credentials. */
public Boolean requestMonitoringStatus() {
    if (!isAmaConfigured()) {
        String detail = 'AMA NAS monitoring status unavailable: Parent connection is not configured.'
        state.lastMonitoringStatusError = detail
        def monitoring = getAmaChild('AMA Monitoring & Analytics')
        if (monitoring) monitoring.recordNasMonitoringError(detail)
        logWarn detail
        return false
    }
    try {
        httpGet([uri: getAmaBaseUrl(), path: '/api/v1/monitoring/status',
                 headers: getAmaApiHeaders(), timeout: 8]) { resp ->
            def monitoring = getAmaChild('AMA Monitoring & Analytics')
            if (resp.status >= 200 && resp.status < 300) {
                Map body = resp.data instanceof Map ? resp.data : (resp.data ? (parseJson(resp.data.toString()) ?: [:]) : [:])
                state.remove('lastMonitoringStatusError')
                if (monitoring) monitoring.updateNasMonitoringStatus(body)
                logDebug 'Retrieved AMA NAS monitoring status.'
            } else {
                String detail = "AMA NAS monitoring status returned HTTP ${resp.status}."
                state.lastMonitoringStatusError = detail
                if (monitoring) monitoring.recordNasMonitoringError(detail)
                logWarn detail
            }
        }
        return true
    } catch (Exception e) {
        String detail = "AMA NAS monitoring request failed: ${e.message}"
        state.lastMonitoringStatusError = detail
        def monitoring = getAmaChild('AMA Monitoring & Analytics')
        if (monitoring) monitoring.recordNasMonitoringError(detail)
        logWarn detail
        return false
    }
}


/* Parent-owned Amazon inventory and endpoint ownership reconciliation.
 * Only this Parent contacts the NAS. The snapshot is normalized and read-only;
 * it never creates, updates, disables, or removes an Alexa endpoint.
 */
public Boolean requestAmazonInventoryRefresh(String reason = 'Parent request') {
    if (!isAmaConfigured()) {
        state.amazonInventoryLastError = 'Amazon inventory refresh unavailable: Parent NAS connection is not configured.'
        logWarn state.amazonInventoryLastError
        return false
    }
    if (state.amazonInventoryRefreshPending == true && state.amazonInventoryRefreshStartedAt &&
        (now() - (state.amazonInventoryRefreshStartedAt as Long)) < 300000L) {
        logDebug 'Amazon inventory refresh already in progress; duplicate request suppressed.'
        return true
    }

    state.amazonInventoryRefreshPending = true
    state.amazonInventoryRefreshStartedAt = now()
    state.amazonInventoryRefreshReason = reason ?: 'Parent request'
    try {
        httpPostJson([uri:getAmaBaseUrl(), path:'/api/v1/amazon/inventory/refresh',
                      headers:getAmaApiHeaders(), body:[:], timeout:30]) { resp ->
            state.amazonInventoryRefreshPending = false
            if (resp?.status >= 200 && resp?.status < 300) {
                Map body = resp.data instanceof Map ? resp.data :
                    (resp.data ? (parseJson(resp.data.toString()) ?: [:]) : [:])
                if (!(body.devices instanceof Collection)) {
                    state.amazonInventoryLastError = 'Amazon inventory response did not contain a devices collection; previous snapshot retained.'
                    logWarn state.amazonInventoryLastError
                    notifyAmazonInventoryFailure(state.amazonInventoryLastError.toString())
                    return
                }
                Map snapshot = normalizeAmazonInventory(body)
                if ((body.count != null) && (body.count as Integer) != (snapshot.endpointCount as Integer)) {
                    state.amazonInventoryLastError = "Amazon inventory count mismatch (response ${body.count}, normalized ${snapshot.endpointCount}); previous snapshot retained."
                    logWarn state.amazonInventoryLastError
                    notifyAmazonInventoryFailure(state.amazonInventoryLastError.toString())
                    return
                }
                state.amazonInventorySnapshot = snapshot
                state.amazonInventoryRetrievedAt = snapshot.retrievedAt
                state.amazonInventoryLastError = null
                state.amazonInventoryLastReason = state.amazonInventoryRefreshReason
                Map status = getAmazonInventoryStatus()
                Map reconciliation = buildAmazonReconciliationSummary()
                distributeAmazonInventoryUpdate(status, reconciliation)
                state.amazonInventoryLastSuccessAt = now()
                logInfo "Amazon inventory refreshed: ${snapshot.endpointCount} endpoints; ${snapshot.counts.AMA ?: 0} AMA, ${snapshot.counts.LEGACY_HUBITAT ?: 0} legacy Hubitat."
            } else {
                state.amazonInventoryLastError = "Amazon inventory refresh returned HTTP ${resp?.status ?: 'unknown'}; previous snapshot retained."
                logWarn state.amazonInventoryLastError
                notifyAmazonInventoryFailure(state.amazonInventoryLastError.toString())
            }
        }
        return true
    } catch (Exception e) {
        state.amazonInventoryRefreshPending = false
        state.amazonInventoryLastError = "Amazon inventory refresh failed: ${e.message}"
        logWarn state.amazonInventoryLastError
        notifyAmazonInventoryFailure(state.amazonInventoryLastError.toString())
        return false
    }
}

private Map normalizeAmazonInventory(Map response) {
    List normalized = []
    Map byDevice = [:]
    Map counts = [:]
    List rows = response.devices instanceof Collection ? response.devices as List : []

    rows.each { rowObject ->
        Map row = rowObject instanceof Map ? rowObject as Map : [:]
        Map raw = row.raw instanceof Map ? row.raw as Map : [:]
        Map legacy = raw.legacyAppliance instanceof Map ? raw.legacyAppliance as Map :
                     (row.legacyAppliance instanceof Map ? row.legacyAppliance as Map : [:])
        Map identity = legacy.driverIdentity instanceof Map ? legacy.driverIdentity as Map : [:]
        Map decoded = identifyKnownAmazonSkillIdentifier(identity.identifier?.toString())
        String skillId = decoded.skillId?.toString() ?: ''
        String stage = decoded.stage?.toString() ?: ''
        String namespace = identity.namespace?.toString() ?: 'UNKNOWN'
        String manufacturer = normalizeAmazonManufacturer(row.manufacturer ?: legacy.manufacturerName ?: raw.manufacturer)
        String owner
        if (namespace == 'SKILL' && (skillId == 'amzn1.ask.skill.c9249a2f-ac4f-433d-b1d3-d40df9e85e5a' || manufacturer.equalsIgnoreCase('Alexa Manager Advanced'))) {
            owner = 'AMA'
        } else if (namespace == 'SKILL' && manufacturer.equalsIgnoreCase('Hubitat')) {
            owner = 'LEGACY_HUBITAT'
        } else if (namespace == 'AAA' && manufacturer.equalsIgnoreCase('Amazon')) {
            owner = 'NATIVE_AMAZON'
        } else if (namespace == 'AlexaBridge') {
            owner = 'ALEXA_BRIDGE'
        } else if (namespace == 'AAA') {
            owner = "AAA_${manufacturer.toUpperCase().replaceAll('[^A-Z0-9]+', '_')}"
        } else if (namespace == 'AlexaBridge') {
            owner = "ALEXA_BRIDGE_${manufacturer.toUpperCase().replaceAll('[^A-Z0-9]+', '_')}"
        } else if (namespace == 'SKILL') {
            owner = "OTHER_SKILL_${manufacturer.toUpperCase().replaceAll('[^A-Z0-9]+', '_')}"
        } else {
            owner = 'OTHER'
        }

        String applianceId = (legacy.applianceId ?: row.applianceId ?: '').toString()
        String hubitatDeviceId = null
        def idMatcher = applianceId =~ /(?:hubitat-|_)(\d+)$/
        if (idMatcher.find()) hubitatDeviceId = idMatcher.group(1)
        String entityId = (legacy.entityId ?: row.entityId ?: row.endpointId ?: '').toString()
        List aliases = []
        if (legacy.aliases instanceof Collection) {
            aliases = legacy.aliases.collect { alias ->
                alias instanceof Map ? (alias.friendlyName ?: alias.name)?.toString() : null
            }.findAll { it }.unique()
        }
        Map network = legacy.applianceNetworkState instanceof Map ? legacy.applianceNetworkState as Map : [:]
        List types = legacy.applianceTypes instanceof Collection ? legacy.applianceTypes.collect { it.toString() } : []
        Map endpoint = [
            owner:owner,
            sourceNamespace:namespace,
            manufacturer:manufacturer,
            skillId:skillId ?: null,
            skillStage:stage ?: null,
            name:(row.name ?: row.friendlyName ?: legacy.friendlyName ?: 'Unnamed endpoint').toString(),
            friendlyName:(legacy.friendlyName ?: row.name ?: 'Unnamed endpoint').toString(),
            aliases:aliases,
            applianceId:applianceId ?: null,
            entityId:entityId ?: null,
            hubitatDeviceId:hubitatDeviceId,
            endpointTypes:types,
            isEnabled:legacy.isEnabled == null ? null : (legacy.isEnabled as Boolean),
            reachability:network.reachability?.toString(),
            createdAt:network.createdAt,
            lastSeenAt:network.lastSeenAt
        ]
        normalized << endpoint
        counts[owner] = ((counts[owner] ?: 0) as Integer) + 1
        if (hubitatDeviceId) {
            List matches = byDevice[hubitatDeviceId] instanceof List ? byDevice[hubitatDeviceId] as List : []
            matches << endpoint
            byDevice[hubitatDeviceId] = matches
        }
    }

    return [retrievedAt:(response.retrievedAt ?: new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", location?.timeZone ?: TimeZone.getDefault())).toString(),
            endpointCount:normalized.size(), counts:counts, endpoints:normalized, byDevice:byDevice]
}

/*
 * Hubitat's Groovy sandbox rejects java.util.Base64 decoder calls. The AMA
 * skill identity is stable and was confirmed from the live Amazon inventory,
 * so classify this exact opaque identifier without decoding it at runtime.
 * The current account is linked to the AMA development-stage skill.
 */
private Map identifyKnownAmazonSkillIdentifier(String identifier) {
    if (identifier == 'eyJza2lsbElkIjoiYW16bjEuYXNrLnNraWxsLmM5MjQ5YTJmLWFjNGYtNDMzZC1iMWQzLWQ0MGRmOWU4NWU1YSIsInN0YWdlIjoiZGV2ZWxvcG1lbnQifQ==') {
        return [skillId:'amzn1.ask.skill.c9249a2f-ac4f-433d-b1d3-d40df9e85e5a', stage:'development']
    }
    return [:]
}

private String normalizeAmazonManufacturer(Object value) {
    if (value instanceof Map) {
        Map map = value as Map
        if (map.value instanceof Map && map.value.text != null) return map.value.text.toString()
        if (map.text != null) return map.text.toString()
        if (map.name != null) return map.name.toString()
    }
    return value == null ? 'UNKNOWN' : value.toString()
}

public Map getAmazonInventorySnapshot() {
    return state.amazonInventorySnapshot instanceof Map ? state.amazonInventorySnapshot as Map : [:]
}

public Map getAmazonInventoryStatus() {
    Map snapshot = getAmazonInventorySnapshot()
    String retrievedAt = snapshot.retrievedAt?.toString()
    long ageMinutes = -1L
    long lastSuccessAt = 0L
    try { lastSuccessAt = (state.amazonInventoryLastSuccessAt ?: 0L) as Long }
    catch (Exception ignored) { lastSuccessAt = 0L }
    if (lastSuccessAt > 0L) ageMinutes = Math.max(0L, (now() - lastSuccessAt) / 60000L)
    boolean available = snapshot.endpointCount != null
    boolean fresh = available && lastSuccessAt > 0L && ageMinutes <= 120L
    return [available:available, fresh:fresh, stale:(available && !fresh),
            endpointCount:(snapshot.endpointCount ?: 0) as Integer,
            counts:snapshot.counts instanceof Map ? snapshot.counts : [:],
            retrievedAt:retrievedAt, ageMinutes:ageMinutes,
            refreshPending:state.amazonInventoryRefreshPending == true,
            lastError:state.amazonInventoryLastError, lastReason:state.amazonInventoryLastReason]
}

public Map getAmazonExposureForDevice(String deviceId) {
    String id = deviceId?.toString()
    Map snapshot = getAmazonInventorySnapshot()
    Map status = getAmazonInventoryStatus()
    if (!id || !snapshot) return [checked:false, status:'NOT_CHECKED', endpoints:[], endpointCount:0, snapshotFresh:false]
    List matches = snapshot.byDevice instanceof Map && snapshot.byDevice[id] instanceof List ?
                   snapshot.byDevice[id] as List : []
    if (!matches) {
        String resultStatus = status.fresh == true ? 'NOT_FOUND' : 'NOT_CHECKED_STALE'
        return [checked:(status.fresh == true), status:resultStatus, endpoints:[], endpointCount:0,
                snapshotFresh:status.fresh == true, retrievedAt:status.retrievedAt]
    }
    List matchedOwners = matches.collect { it.owner?.toString() }.unique()
    String owner = matchedOwners.size() == 1 ? matches[0].owner.toString() : 'MIXED'
    String observedStatus = matches.size() > 1 ? (matchedOwners.size() > 1 ? 'MULTI_OWNER_EXPOSURE' : 'DUPLICATE_ENDPOINTS') : 'EXPOSED'
    return [checked:true, status:observedStatus,
            owner:owner, endpoints:matches, endpointCount:matches.size(),
            snapshotFresh:status.fresh == true, retrievedAt:status.retrievedAt]
}

public Map getAmazonEndpointsBySource() {
    Map result = [:]
    getAmazonInventorySnapshot().endpoints?.each { endpoint ->
        String owner = endpoint.owner?.toString() ?: 'OTHER'
        List items = result[owner] instanceof List ? result[owner] as List : []
        items << endpoint
        result[owner] = items
    }
    return result
}

public Map buildAmazonReconciliationSummary() {
    Map status = getAmazonInventoryStatus()
    Map candidates = getDeviceInventory()
    Map management = getManagementRecords()
    List endpoints = getAmazonInventorySnapshot().endpoints instanceof List ? getAmazonInventorySnapshot().endpoints as List : []
    List amaEndpoints = endpoints.findAll { it.owner == 'AMA' }
    int approved = 0, approvedNotExposed = 0, approvedWithoutAma = 0, approvedWithLegacy = 0, approvedWithOther = 0
    int withAma = 0, withLegacy = 0, withOther = 0, notChecked = 0, duplicateAma = 0, multiOwner = 0
    candidates.each { deviceId, candidate ->
        Map record = management[deviceId.toString()] instanceof Map ? management[deviceId.toString()] as Map : [:]
        if (record.approvedForAmazon == true) approved++
        Map observed = getAmazonExposureForDevice(deviceId.toString())
        List found = observed.endpoints instanceof List ? observed.endpoints as List : []
        int amaCount = found.count { it.owner == 'AMA' }
        if (amaCount > 1) duplicateAma++
        if (observed.status == 'MULTI_OWNER_EXPOSURE') multiOwner++
        boolean hasLegacy = found.any { it.owner == 'LEGACY_HUBITAT' }
        boolean hasOther = found.any { !(it.owner in ['AMA', 'LEGACY_HUBITAT']) }
        if (amaCount > 0) withAma++
        if (hasLegacy) withLegacy++
        if (hasOther) withOther++
        if (!found && observed.checked != true) notChecked++
        if (record.approvedForAmazon == true && status.fresh == true && amaCount == 0) {
            approvedWithoutAma++
            if (!found) approvedNotExposed++
            else if (found.any { it.owner == 'LEGACY_HUBITAT' }) approvedWithLegacy++
            else approvedWithOther++
        }
    }
    int disabledAma = amaEndpoints.count { it.isEnabled == false }
    int unreachableAma = amaEndpoints.count { it.reachability && !it.reachability.equalsIgnoreCase('REACHABLE') }
    int unknownReachabilityAma = amaEndpoints.count { !it.reachability }
    int orphanAma = amaEndpoints.count { it.hubitatDeviceId && !candidates.containsKey(it.hubitatDeviceId.toString()) }
    return [available:status.available, fresh:status.fresh, endpointCount:status.endpointCount,
            approvedCandidates:approved, approvedNotExposed:approvedNotExposed,
            approvedWithoutAmaEndpoint:approvedWithoutAma, approvedWithLegacyExposure:approvedWithLegacy,
            approvedWithOtherExposure:approvedWithOther, candidatesWithAma:withAma, candidatesWithLegacyHubitat:withLegacy,
            candidatesWithOtherExposure:withOther, candidatesNotChecked:notChecked, duplicateAmaMatches:duplicateAma,
            multiOwnerExposureCount:multiOwner,
            amaEndpointCount:amaEndpoints.size(), disabledAmaEndpoints:disabledAma,
            unreachableAmaEndpoints:unreachableAma, unknownReachabilityAmaEndpoints:unknownReachabilityAma,
            amaEndpointsWithoutCandidate:orphanAma]
}

private void distributeAmazonInventoryUpdate(Map status, Map reconciliation) {
    try {
        def selection = getAmaChild('AMA Candidate Selection')
        if (selection) selection.updateAmazonInventoryStatus(status)
    } catch (Exception e) { logWarn "Candidate Selection Amazon inventory update failed: ${e.message}" }
    try {
        def preparation = getAmaChild('AMA Candidate Preparation')
        if (preparation) preparation.updateAmazonInventoryStatus(status)
    } catch (Exception e) { logWarn "Candidate Preparation Amazon inventory update failed: ${e.message}" }
    try {
        def exposure = getAmaChild('AMA Exposure Management')
        if (exposure) exposure.updateAmazonInventoryStatus(status, reconciliation)
    } catch (Exception e) { logWarn "Exposure Management Amazon inventory update failed: ${e.message}" }
    try {
        def monitoring = getAmaChild('AMA Monitoring & Analytics')
        if (monitoring) monitoring.updateAmazonInventoryStatus(status, reconciliation)
    } catch (Exception e) { logWarn "Monitoring Amazon inventory update failed: ${e.message}" }
}

private void notifyAmazonInventoryFailure(String detail) {
    try {
        def monitoring = getAmaChild('AMA Monitoring & Analytics')
        if (monitoring) monitoring.recordAmazonInventoryError(detail)
    } catch (Exception ignored) { }
}

public Map getInventoryStatus() {
    def child = getAmaChild('AMA Candidate Selection')
    if (child) return child.getInventoryStatus() ?: [:]
    return [deviceCount: 0, lastScan: null]
}

public Map getDeviceInventory() {
    def child = getAmaChild('AMA Candidate Selection')
    if (child) return child.getDeviceInventory() ?: [:]
    return [:]
}

public Map getDeviceInventoryEntry(String deviceId) {
    if (!deviceId) return [:]
    def child = getAmaChild('AMA Candidate Selection')
    if (child) return child.getDeviceInventoryEntry(deviceId.toString()) ?: [:]
    return [:]
}

public void requestDeviceInventoryScan(String reason = 'Parent request') {
    def child = getAmaChild('AMA Candidate Selection')
    if (child) child.scanDeviceInventory(reason)
    else logWarn 'Candidate Selection child is not installed; candidate scan request ignored.'
}

/*
 * Public parent communication contract. Children call these methods; the parent is
 * the ONLY route between branches. No child receives or calls another child reference.
 */
public Map getModuleStatus() {
    Map result = [parent:[name:'Alexa Manager Advanced', version:version()]]
    ['AMA Candidate Selection','AMA Candidate Preparation','AMA Exposure Management','AMA Monitoring & Analytics'].each { childName ->
        def child = getAmaChild(childName)
        result[childName] = child ? (child.getModuleStatus() ?: [installed:true]) : [installed:false]
    }
    result.amazonInventory = getAmazonInventoryStatus()
    result.amazonReconciliation = buildAmazonReconciliationSummary()
    return result
}

public void childInventoryUpdated(Map inventoryStatus = [:]) {
    Map safeStatus = inventoryStatus ?: [:]
    state.lastInventoryUpdate = now()
    state.lastInventoryStatus = safeStatus
    logInfo "Received inventory update from Device Handling (${safeStatus.deviceCount ?: 0} records)."

    /* Each branch is isolated: one child failure must not block another child. */
    try {
        def management = getAmaChild('AMA Candidate Preparation')
        if (management) management.onInventoryUpdated(safeStatus)
        state.remove('lastManagementNotificationError')
    } catch (Exception e) {
        logWarn "Could not notify Device Management child: ${e.message}"
        state.lastManagementNotificationError = e.message?.toString()
    }

    try {
        def monitoring = getAmaChild('AMA Monitoring & Analytics')
        if (monitoring) monitoring.recordParentEvent('inventory.updated', [status:safeStatus])
        state.remove('lastMonitoringNotificationError')
    } catch (Exception e) {
        logWarn "Could not notify Monitoring & Analytics child: ${e.message}"
        state.lastMonitoringNotificationError = e.message?.toString()
    }
}

public Map getManagementRecords() {
    def management = getAmaChild('AMA Candidate Preparation')
    return management ? (management.getManagementRecords() ?: [:]) : [:]
}

public Map getManagementRecord(String deviceId) {
    if (!deviceId) return [:]
    def management = getAmaChild('AMA Candidate Preparation')
    return management ? (management.getManagementRecord(deviceId.toString()) ?: [:]) : [:]
}

public void saveManagementRecord(String deviceId, Map record = [:]) {
    if (!deviceId) return
    def management = getAmaChild('AMA Candidate Preparation')
    if (management) management.saveManagementRecord(deviceId.toString(), record ?: [:])
}

public void recordVoiceTest(Map testRecord = [:]) {
    def management = getAmaChild('AMA Candidate Preparation')
    if (management) management.recordVoiceTest(testRecord ?: [:])
    else logWarn 'Candidate Preparation child is not installed; voice test record was not stored.'
}

public List getVoiceTestLibrary(Integer limit = 50) {
    def management = getAmaChild('AMA Candidate Preparation')
    return management ? (management.getVoiceTestLibrary(limit) ?: []) : []
}

public Map getExposureManagementStatus() {
    def exposure = getAmaChild('AMA Exposure Management')
    return exposure ? (exposure.getModuleStatus() ?: [:]) : [installed:false]
}

public void reportExposureEvent(String eventName, Map payload = [:]) {
    def exposure = getAmaChild('AMA Exposure Management')
    if (exposure) exposure.recordParentEvent(eventName ?: 'exposure.event', payload ?: [:])
}

public void reportChildEvent(String eventName, Map payload = [:]) {
    def monitoring = getAmaChild('AMA Monitoring & Analytics')
    if (monitoring) monitoring.recordParentEvent(eventName ?: 'child.event', payload ?: [:])
}

private Object getAmaChild(String appName) {
    Map legacyNames = [
        'AMA Candidate Selection':'AMA Device Handling Child',
        'AMA Candidate Preparation':'AMA Device Management Child',
        'AMA Exposure Management':'AMA Exposure Management Child',
        'AMA Monitoring & Analytics':'AMA Monitoring & Analytics Child'
    ]
    String legacyName = legacyNames[appName]
    return getChildApps()?.find { child ->
        child.name == appName ||
        child.typeName == appName ||
        (legacyName && (child.name == legacyName || child.typeName == legacyName)) ||
        child.label?.toString()?.startsWith(appName) ||
        (legacyName && child.label?.toString()?.startsWith(legacyName))
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
