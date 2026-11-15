/**
 * v0.2.8 - 2026/11/12 - Refreshes the System Monitor tile immediately on Amazon inventory status changes.
 *   - Updates the Amazon Inventory current/stale/error indicator after a successful refresh or failure.
 *   - Keeps the NAS/InfluxDB indicators independent and preserves the existing hourly inventory schedule.
 * v0.2.7 - 2026/11/11 - Adds scheduled Amazon endpoint inventory monitoring and reconciliation diagnostics.
 *   - Requests an Amazon inventory refresh from the Parent every 60 minutes.
 *   - Reports disabled, unreachable, duplicate, missing-approved, and orphaned AMA endpoints.
 *   - Publishes compact Amazon inventory diagnostics to the existing System Monitor device.
 *   - Never creates, updates, disables, or removes Amazon endpoints automatically.
 * v0.2.6 - 2026/11/11 - Simplifies the System Monitor dashboard tile to a compact 2-wide x 1-high service check.
 *   - Displays only AMA NAS and InfluxDB availability; removes heartbeat/telemetry jargon from the tile.
 *   - Keeps heartbeat and write tracking as background diagnostics.
 *   - Shows InfluxDB as UNKNOWN when NAS is unreachable rather than presenting stale information.
 * v0.2.5 - 2026/11/11 - Adds heartbeat monitoring and the single AMA System Monitor virtual device.
 *   - Sends a five-minute Hubitat heartbeat through the Parent independently of event telemetry enablement.
 *   - Receives authenticated NAS/InfluxDB health snapshots through the Parent only.
 *   - Creates/updates one dashboard-friendly AMA System Monitor child device.
 *   - Distinguishes NAS availability, InfluxDB reachability, Hubitat heartbeat, and end-to-end heartbeat writes.
 * v0.2.4 - 2026/11/10 - Forwards complete Hubitat event payloads to the NAS for normalization.
 *   - Parent remains the sole inter-module communication path and owns NAS credentials.
 *   - NAS performs InfluxDB normalization and preserves the original JSON payload.
 *   - Direct Hubitat-to-InfluxDB event writes are no longer used for parent events.
 * v0.2.3 - 2026/11/10 - Adds explicit InfluxDB test-write result reporting.
 *   - Test button works independently of the continuous telemetry enable switch.
 *   - Shows PENDING, SUCCESS, or FAILED test status in the app.
 *   - Logs every test request and final HTTP result at INFO/WARN level.
 *   - Counts configuration and HTTP test failures.
 * v0.2.2 - 2026/11/10 - Replaces strict URL regex with URI-based InfluxDB base URL validation.
 *   - Accepts valid HTTP/HTTPS host and port combinations, including private IPv4 addresses.
 *   - Returns specific validation errors for malformed URLs and unsupported URL components.
 * v0.2.1 - 2026/11/10 - Aligns the InfluxDB-enabled Monitoring & Analytics app identity with the canonical parent branch name.
 * Alexa Manager Advanced - Monitoring & Analytics Child
 * Version: 0.2.9 | 2026/11/12
 * Hubitat-side event capture and parent-mediated NAS telemetry forwarding.
 * All inter-module events arrive through the Parent. InfluxDB is optional:
 * telemetry failures must never interrupt Hubitat control or AMA operation.
 *
 * Changelog:
 * v0.3.0 - 2026/11/12 - Refreshes the System Monitor tile through its dedicated no-argument driver command.
 *   - Calls refreshTile after Amazon inventory, NAS status, and monitoring-error attribute batches.
 *   - Keeps all monitoring decisions and NAS requests in the Parent/Monitoring & Analytics architecture.
 * v0.2.9 - 2026/11/12 - Handles System Monitor device requests through the Parent.
 *   - Subscribes to the monitor device command-request attribute.
 *   - Routes Configure and Refresh requests to the Parent monitoring-status API.
 *   - Routes Health Check through a fresh Hubitat heartbeat followed by Parent status retrieval.
 *   - Leaves all recurring schedules owned by this child app.
 *   - Removes direct calls to the driver's former exposed tile-update command; driver redraws from attributes.
 * v0.2.8 - 2026/11/12 - Refreshes the System Monitor tile immediately when Amazon inventory status changes.
 *   - Shows CURRENT, STALE/NOT CHECKED, or ERROR without waiting for the next NAS status cycle.
 * v0.2.7 - 2026/11/11 - Adds hourly Amazon inventory reconciliation through the Parent.
 *   - Reports disabled, unreachable, duplicate, unexposed-approved, and orphaned AMA endpoints.
 *   - Never mutates Alexa endpoint registrations automatically.
 * v0.2.0 - 2026/11/10 - Adds direct Hubitat-to-InfluxDB 2.x telemetry (superseded in v0.2.4 by NAS normalization).
 *   - Configures a dedicated HubAlexa bucket in the DundeeHome organization.
 *   - Adds a connection/write test and reporting status.
 *   - Sends compact parent events asynchronously; failures are isolated.
 *   - Does not route Hubitat telemetry through the AMA NAS service.
 * v0.1.5 - 2026/11/10 - Aligns displayed module title with the four-branch architecture.
 * v0.1.4 - 2026/11/10 - Removes the redundant Child suffix from the displayed app title.
 * v0.1.3 - 2026/11/10 - Bounds retained event payload size and nesting.
 * v0.1.2 - 2026/11/09 - Establishes parent-mediated event intake and module status API.
 * v0.1.1 - 2026/11/09 - Initial branch placeholder.
 */

import groovy.json.JsonOutput

String version() { '0.3.0' }
String timeStamp() { '2026/11/11' }

definition(name: 'AMA Monitoring & Analytics', namespace: 'jshimota', author: 'James Shimota',
    description: 'Reports Hubitat-side AMA activity, health, and telemetry.',
    category: 'Integrations', parent: 'jshimota:Alexa Manager Advanced', iconUrl: '', iconX2Url: '', iconX3Url: '', importUrl: '')

preferences { page(name: 'mainPage') }

def mainPage() {
    updateAppLabel()
    List events = getRecentEvents()
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'><h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Monitoring &amp; Analytics</h2><span style='font-size:12px;'>Hubitat telemetry v${version()} (${timeStamp()})</span></div>"
            paragraph 'Receives application events through the Parent, forwards telemetry to the AMA NAS, and maintains one compact AMA System Monitor dashboard tile for NAS and InfluxDB availability.'
        }
        section('MODULE STATUS') {
            paragraph "<b>Events retained:</b> ${events.size()} (maximum 200)"
            paragraph "<b>Last event:</b> ${state.lastEventAt ?: 'None received'}"
            paragraph "<b>Hubitat event forwarding:</b> ${settings.influxEnabled == true ? 'Enabled' : 'Disabled'}"
            paragraph "<b>Last direct test write:</b> ${state.influxLastSuccess ?: 'No successful test write yet'}"
            if (state.influxLastError) paragraph "<span style='color:#B03A2E;'><b>Last InfluxDB error:</b> ${htmlSafe(state.influxLastError)}</span>"
            paragraph "<b>Direct test failures:</b> ${state.influxFailureCount ?: 0}"
            paragraph "<b>Last connection test:</b> ${state.influxTestStatus ?: 'Not tested'}"
            paragraph "<b>System Monitor device:</b> ${getMonitorDevice() ? htmlSafe(getMonitorDevice().displayName ?: 'AMA System Monitor') : 'Not created — install the AMA System Monitor driver and save this app'}"
            paragraph "<b>NAS:</b> ${state.nasStatus ?: 'Not checked'} | <b>InfluxDB:</b> ${state.nasInfluxHealth ?: 'Not checked'} | <b>Heartbeat:</b> ${state.hubitatHeartbeatStatus ?: 'Waiting'} | <b>End-to-end:</b> ${state.telemetryStatus ?: 'Waiting'}"
            Map amazonStatus = parent?.getAmazonInventoryStatus() ?: [:]
            Map amazonRecon = parent?.buildAmazonReconciliationSummary() ?: [:]
            paragraph "<b>Amazon inventory:</b> ${amazonStatus.available == true ? (amazonStatus.fresh == true ? 'CURRENT' : 'STALE') : 'NOT CHECKED'} | ${amazonStatus.endpointCount ?: 0} endpoints | Last refresh: ${amazonStatus.retrievedAt ?: 'Never'}"
            paragraph "<b>AMA reconciliation:</b> ${amazonRecon.approvedNotExposed ?: 0} approved/not exposed | ${amazonRecon.disabledAmaEndpoints ?: 0} disabled | ${amazonRecon.unreachableAmaEndpoints ?: 0} unreachable | ${amazonRecon.duplicateAmaMatches ?: 0} duplicates | ${amazonRecon.multiOwnerExposureCount ?: 0} multi-owner | ${amazonRecon.unknownReachabilityAmaEndpoints ?: 0} reachability unknown"
            if (state.nasMonitoringError) paragraph "<span style='color:#B03A2E;'><b>Monitoring status error:</b> ${htmlSafe(state.nasMonitoringError)}</span>"
            if (state.influxTestAt) paragraph "<b>Test time:</b> ${htmlSafe(state.influxTestAt.toString())}"
            if (state.influxTestDetail) paragraph "<b>Test result:</b> ${htmlSafe(state.influxTestDetail.toString())}"
        }
        section('INFLUXDB 2.x CONNECTION') {
            input name:'influxEnabled', type:'bool', title:'Enable Hubitat telemetry reporting', defaultValue:false
            input name:'influxUrl', type:'text', title:'InfluxDB Server URL', description:'Example: http://192.168.1.12:8086', defaultValue:'http://192.168.1.12:8086', required:true
            input name:'influxOrg', type:'text', title:'Organization', defaultValue:'DundeeHome', required:true
            input name:'influxBucket', type:'text', title:'Bucket', defaultValue:'HubAlexa', required:true
            input name:'influxToken', type:'password', title:'AMA-Hubitat-Writer token', required:true
            input name:'btnTestInflux', type:'button', title:'Test InfluxDB Write'
            paragraph 'Connection test settings are retained for diagnostics. Continuous parent events are forwarded to the AMA NAS using the Parents configured URL and API key; the NAS performs InfluxDB writes.'
        }
        section('LOGGING', hideable:true, hidden:true) {
            input name:'logInfoEnable', type:'bool', title:'Enable Info Logging', defaultValue:true
            input name:'logDebugEnable', type:'bool', title:'Enable Debug Logging', defaultValue:false
        }
    }
}

void installed() {
    updateAppLabel()
    if (!(state.eventHistory instanceof List)) state.eventHistory = []
    logInfo "Installed Monitoring & Analytics v${version()}"
    initializeMonitoring()
}

void updated() {
    updateAppLabel()
    if (!(state.eventHistory instanceof List)) state.eventHistory = []
    logInfo "Updated Monitoring & Analytics v${version()}"
    initializeMonitoring()
}

/* Parent is the sole source of inter-module messages. */
public void recordParentEvent(String eventName, Map payload = [:]) {
    List history = getRecentEvents()
    Map compact = compactPayload(payload ?: [:])
    Map event = [event:eventName ?: 'parent.event', timestamp:now(), payload:compact]
    history << event
    if (history.size() > 200) history = history.takeRight(200)
    state.eventHistory = history
    state.lastEventAt = event.timestamp
    logDebug "Received parent event '${event.event}'."

    if (settings.influxEnabled == true) {
        try {
            def parent = getParent()
            if (parent) {
                parent.forwardTelemetryEvent(event.event, [
                    eventTimestamp: event.timestamp,
                    payload: payload ?: [:]
                ])
            } else {
                logWarn 'Parent application is unavailable. Event retained locally but not sent to NAS.'
            }
        } catch (Exception e) {
            logWarn "Could not forward event to AMA NAS through Parent: ${e.message}"
        }
    }
}

/* Monitoring heartbeat and dashboard device. All NAS communication remains Parent-mediated. */
private String monitorDeviceNetworkId() { return "ama-system-monitor-${app.id}" }

private void initializeMonitoring() {
    def monitor = ensureMonitorDevice()
    if (monitor) {
        unsubscribe(monitor)
        subscribe(monitor, 'monitorCommandRequest', 'monitorCommandHandler')
    }
    unschedule('monitoringHeartbeat')
    unschedule('refreshMonitoringStatus')
    unschedule('refreshAmazonInventory')
    runEvery5Minutes('monitoringHeartbeat')
    runEvery1Hour('refreshAmazonInventory')
    runIn(12, 'monitoringHeartbeat')
    runIn(60, 'refreshAmazonInventory')
}

private def ensureMonitorDevice() {
    def monitor = getChildDevice(monitorDeviceNetworkId())
    if (monitor) return monitor
    try {
        monitor = addChildDevice('jshimota', 'AMA System Monitor', monitorDeviceNetworkId(), [
            name: 'AMA System Monitor', label: 'AMA System Monitor', isComponent: false
        ])
        state.remove('monitorDriverMissingLogged')
        logInfo 'Created the AMA System Monitor virtual device.'
    } catch (Exception e) {
        if (state.monitorDriverMissingLogged != true) {
            logWarn "Could not create AMA System Monitor. Confirm the 'AMA System Monitor' driver is installed: ${e.message}"
            state.monitorDriverMissingLogged = true
        }
    }
    return monitor
}

private def getMonitorDevice() {
    return getChildDevice(monitorDeviceNetworkId())
}

/**
 * Commands originate at the System Monitor device and are handled here.
 * All service requests are forwarded to the Parent; no child-to-child calls occur.
 **/
void monitorCommandHandler(evt) {
    String request = evt?.value?.toString()?.trim()?.toUpperCase()
    logInfo "System Monitor requested ${request ?: 'UNKNOWN'} through Parent."

    switch (request) {
        case 'CONFIGURE':
            if (!getParent()) {
                recordNasMonitoringError('Parent application is unavailable; System Monitor could not be configured.')
            } else {
                refreshMonitoringStatus()
            }
            break
        case 'REFRESH':
            refreshMonitoringStatus()
            break
        case 'HEALTH_CHECK':
            monitoringHeartbeat()
            break
        default:
            logWarn "Ignored unknown System Monitor request '${request}'."
    }
}

void monitoringHeartbeat() {
    def parent = getParent()
    if (!parent) {
        recordNasMonitoringError('Parent application is unavailable; heartbeat was not sent.')
        return
    }
    try {
        Boolean sent = parent.forwardTelemetryEvent('hubitat.heartbeat', [
            eventTimestamp: now(),
            payload: [monitoringVersion:version(), hubitatTime:new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", location?.timeZone ?: TimeZone.getDefault())]
        ])
        if (sent) {
            state.lastHeartbeatSentAt = now()
            state.remove('heartbeatSendError')
            logDebug 'Hubitat monitoring heartbeat submitted through Parent.'
        } else {
            state.heartbeatSendError = 'Parent could not submit the heartbeat to the AMA NAS.'
            logWarn state.heartbeatSendError
        }
    } catch (Exception e) {
        state.heartbeatSendError = "Heartbeat submission failed: ${e.message}"
        logWarn state.heartbeatSendError
    }
    runIn(12, 'refreshMonitoringStatus')
}

void refreshMonitoringStatus() {
    def parent = getParent()
    if (!parent) {
        recordNasMonitoringError('Parent application is unavailable; NAS status cannot be retrieved.')
        return
    }
    try {
        if (!parent.requestMonitoringStatus()) {
            if (!state.nasMonitoringError) recordNasMonitoringError('Parent could not retrieve NAS monitoring status.')
        }
    } catch (Exception e) {
        recordNasMonitoringError("Could not request NAS monitoring status through Parent: ${e.message}")
    }
}


void refreshAmazonInventory() {
    def parent = getParent()
    if (!parent) {
        recordAmazonInventoryError('Parent application is unavailable; Amazon inventory was not refreshed.')
        return
    }
    try {
        if (!parent.requestAmazonInventoryRefresh('Monitoring hourly schedule')) {
            recordAmazonInventoryError('Parent could not start the Amazon inventory refresh.')
        } else {
            state.amazonInventoryRefreshRequestedAt = now()
            logDebug 'Requested scheduled Amazon endpoint inventory refresh through Parent.'
        }
    } catch (Exception e) {
        recordAmazonInventoryError("Scheduled Amazon inventory refresh failed: ${e.message}")
    }
}

public void updateAmazonInventoryStatus(Map status = [:], Map reconciliation = [:]) {
    state.amazonInventoryStatus = status ?: [:]
    state.amazonReconciliationSummary = reconciliation ?: [:]
    state.amazonInventoryLastReceivedAt = now()
    state.amazonInventoryError = null
    def monitor = ensureMonitorDevice()
    if (monitor) {
        monitor.sendEvent(name:'amazonInventoryStatus', value:status.fresh == true ? 'CURRENT' : (status.available == true ? 'STALE' : 'NOT_CHECKED'))
        monitor.sendEvent(name:'amazonEndpointCount', value:(status.endpointCount ?: 0) as Integer)
        monitor.sendEvent(name:'amazonAmaEndpointCount', value:(reconciliation.amaEndpointCount ?: 0) as Integer)
        monitor.sendEvent(name:'amazonLegacyHubitatEndpointCount', value:(status.counts?.LEGACY_HUBITAT ?: 0) as Integer)
        monitor.sendEvent(name:'amazonDisabledAmaCount', value:(reconciliation.disabledAmaEndpoints ?: 0) as Integer)
        monitor.sendEvent(name:'amazonUnreachableAmaCount', value:(reconciliation.unreachableAmaEndpoints ?: 0) as Integer)
        monitor.sendEvent(name:'amazonUnknownReachabilityAmaCount', value:(reconciliation.unknownReachabilityAmaEndpoints ?: 0) as Integer)
        monitor.sendEvent(name:'amazonApprovedNotExposedCount', value:(reconciliation.approvedNotExposed ?: 0) as Integer)
        monitor.sendEvent(name:'amazonDuplicateAmaCount', value:(reconciliation.duplicateAmaMatches ?: 0) as Integer)
        monitor.sendEvent(name:'amazonMultiOwnerExposureCount', value:(reconciliation.multiOwnerExposureCount ?: 0) as Integer)
        monitor.sendEvent(name:'amazonOrphanAmaCount', value:(reconciliation.amaEndpointsWithoutCandidate ?: 0) as Integer)
        monitor.sendEvent(name:'amazonLastInventoryRefresh', value:(status.retrievedAt ?: 'Never').toString())
        monitor.sendEvent(name:'amazonReconciliationSummary', value:"${reconciliation.approvedNotExposed ?: 0} approved not exposed | ${reconciliation.disabledAmaEndpoints ?: 0} disabled | ${reconciliation.unreachableAmaEndpoints ?: 0} unreachable | ${reconciliation.duplicateAmaMatches ?: 0} duplicates | ${reconciliation.multiOwnerExposureCount ?: 0} multi-owner | ${reconciliation.unknownReachabilityAmaEndpoints ?: 0} reachability unknown".take(250))
        monitor.refreshTile()
    }

    List findings = []
    if (reconciliation.approvedNotExposed) findings << "${reconciliation.approvedNotExposed} approved candidate(s) have no Alexa endpoint"
    if (reconciliation.disabledAmaEndpoints) findings << "${reconciliation.disabledAmaEndpoints} AMA endpoint(s) disabled"
    if (reconciliation.unreachableAmaEndpoints) findings << "${reconciliation.unreachableAmaEndpoints} AMA endpoint(s) not reachable"
    if (reconciliation.duplicateAmaMatches) findings << "${reconciliation.duplicateAmaMatches} duplicate AMA mapping(s)"
    if (reconciliation.multiOwnerExposureCount) findings << "${reconciliation.multiOwnerExposureCount} candidate(s) exposed by multiple integrations"
    if (reconciliation.amaEndpointsWithoutCandidate) findings << "${reconciliation.amaEndpointsWithoutCandidate} AMA endpoint(s) without current candidate"
    if (findings) {
        logWarn "Amazon inventory reconciliation findings: ${findings.join('; ')}"
        recordParentEvent('amazon.inventory.reconciliation_warning', [findings:findings, reconciliation:reconciliation])
    } else {
        logInfo "Amazon inventory reconciled: ${status.endpointCount ?: 0} endpoints; ${reconciliation.amaEndpointCount ?: 0} AMA endpoints; no discrepancies detected."
        recordParentEvent('amazon.inventory.reconciled', [endpointCount:status.endpointCount ?: 0, reconciliation:reconciliation])
    }
}

public void recordAmazonInventoryError(String message) {
    String detail = (message ?: 'Unknown Amazon inventory error').toString().take(250)
    state.amazonInventoryError = detail
    logWarn detail
    def monitor = ensureMonitorDevice()
    if (monitor) {
        monitor.sendEvent(name:'amazonInventoryStatus', value:'ERROR')
        monitor.sendEvent(name:'amazonReconciliationSummary', value:detail)
        monitor.refreshTile()
    }
}

public void updateNasMonitoringStatus(Map snapshot = [:]) {
    Map hubitat = snapshot?.hubitat instanceof Map ? snapshot.hubitat : [:]
    Map influx = snapshot?.influx instanceof Map ? snapshot.influx : [:]
    String nasStatus = snapshot?.status?.toString()?.equalsIgnoreCase('ok') ? 'ONLINE' : 'UNHEALTHY'
    String influxHealth = (influx.healthStatus ?: 'UNKNOWN').toString().toUpperCase()
    String heartbeatStatus = !hubitat.lastHeartbeatReceivedAt ? 'WAITING' : (isFreshTimestamp(hubitat.lastHeartbeatReceivedAt, 12) ? 'HEALTHY' : 'STALE')
    String telemetryStatus
    if (influx.configured != true) telemetryStatus = 'NOT_CONFIGURED'
    else if (influx.enabled != true) telemetryStatus = 'DISABLED'
    else if (influxHealth == 'UNHEALTHY') telemetryStatus = 'INFLUX_UNREACHABLE'
    else if (influx.writeStatus == 'ERROR') telemetryStatus = 'WRITE_ERROR'
    else if (hubitat.lastHeartbeatWrittenAt && isFreshTimestamp(hubitat.lastHeartbeatWrittenAt, 12)) telemetryStatus = 'HEALTHY'
    else if (hubitat.lastHeartbeatReceivedAt) telemetryStatus = 'WAITING_FOR_WRITE'
    else telemetryStatus = 'WAITING'

    state.nasStatus = nasStatus
    state.nasInfluxHealth = influxHealth
    state.nasInfluxWriteStatus = (influx.writeStatus ?: 'UNKNOWN').toString().toUpperCase()
    state.hubitatHeartbeatStatus = heartbeatStatus
    state.telemetryStatus = telemetryStatus
    state.nasMonitoringError = null
    state.lastNasStatusAt = now()
    state.lastNasVersion = snapshot?.version?.toString()
    state.lastNasStatus = snapshot ?: [:]

    def monitor = ensureMonitorDevice()
    if (!monitor) return
    monitor.sendEvent(name:'nasStatus', value:nasStatus)
    monitor.sendEvent(name:'nasVersion', value:(snapshot?.version ?: 'Unknown').toString())
    monitor.sendEvent(name:'nasUptimeSeconds', value:(snapshot?.uptimeSeconds ?: 0) as Long, unit:'s')
    monitor.sendEvent(name:'nasEnvironment', value:(snapshot?.environment ?: 'Unknown').toString())
    monitor.sendEvent(name:'influxStatus', value:influxHealth)
    monitor.sendEvent(name:'influxWriteStatus', value:(influx.writeStatus ?: 'UNKNOWN').toString().toUpperCase())
    monitor.sendEvent(name:'influxEnabled', value:influx.enabled == true ? 'true' : 'false')
    monitor.sendEvent(name:'influxConfigured', value:influx.configured == true ? 'true' : 'false')
    monitor.sendEvent(name:'hubitatHeartbeatStatus', value:heartbeatStatus)
    monitor.sendEvent(name:'telemetryStatus', value:telemetryStatus)
    monitor.sendEvent(name:'lastHubitatHeartbeat', value:(hubitat.lastHeartbeatReceivedAt ?: 'Never').toString())
    monitor.sendEvent(name:'lastHubitatHeartbeatWrite', value:(hubitat.lastHeartbeatWrittenAt ?: 'Never').toString())
    monitor.sendEvent(name:'lastHubitatEvent', value:(hubitat.lastEventReceivedAt ?: 'Never').toString())
    monitor.sendEvent(name:'lastInfluxWrite', value:(influx.lastWriteAt ?: 'Never').toString())
    monitor.sendEvent(name:'lastInfluxHealthCheck', value:(influx.lastHealthCheckAt ?: 'Never').toString())
    monitor.sendEvent(name:'hubitatEventCount', value:(hubitat.eventCount ?: 0) as Integer)
    monitor.sendEvent(name:'hubitatHeartbeatCount', value:(hubitat.heartbeatCount ?: 0) as Integer)
    monitor.sendEvent(name:'influxWriteSuccessCount', value:(influx.successCount ?: 0) as Integer)
    monitor.sendEvent(name:'influxWriteFailureCount', value:(influx.failureCount ?: 0) as Integer)
    String errorText = (influx.lastHealthError ?: influx.lastError ?: '').toString()
    monitor.sendEvent(name:'lastInfluxError', value:errorText.take(250))
    monitor.sendEvent(name:'lastStatusCheck', value:new Date().format("yyyy-MM-dd HH:mm:ss", location?.timeZone ?: TimeZone.getDefault()))
    monitor.sendEvent(name:'monitorSummary', value:"NAS ${nasStatus} | Influx ${influxHealth} | Heartbeat ${heartbeatStatus} | Telemetry ${telemetryStatus}".take(250))
    monitor.refreshTile()
    logDebug "Updated AMA System Monitor: NAS ${nasStatus}, InfluxDB ${influxHealth}, heartbeat ${heartbeatStatus}, telemetry ${telemetryStatus}."
}

public void recordNasMonitoringError(String message) {
    String detail = (message ?: 'Unknown NAS monitoring error').toString().take(250)
    state.nasStatus = 'OFFLINE'
    state.nasMonitoringError = detail
    state.lastNasStatusAt = now()
    def monitor = ensureMonitorDevice()
    if (monitor) {
        monitor.sendEvent(name:'nasStatus', value:'OFFLINE')
        monitor.sendEvent(name:'influxStatus', value:'UNKNOWN')
        monitor.sendEvent(name:'lastStatusCheck', value:new Date().format("yyyy-MM-dd HH:mm:ss", location?.timeZone ?: TimeZone.getDefault()))
        monitor.sendEvent(name:'monitorSummary', value:"NAS OFFLINE | ${detail}".take(250))
        monitor.refreshTile()
    }
    logWarn detail
}

private boolean isFreshTimestamp(def value, Integer minutes = 12) {
    if (!value) return false
    try {
        long timestamp = java.time.Instant.parse(value.toString()).toEpochMilli()
        return (now() - timestamp) <= (minutes * 60L * 1000L) && timestamp <= now() + 60000L
    } catch (Exception ignored) { return false }
}

void appButtonHandler(String buttonName) {
    if (buttonName != 'btnTestInflux') return

    String testTime = new Date().format("yyyy-MM-dd HH:mm:ss", location?.timeZone ?: TimeZone.getDefault())
    state.influxTestAt = testTime
    state.influxTestStatus = 'PENDING'
    state.influxTestDetail = 'Test write submitted; waiting for InfluxDB response.'
    logInfo "InfluxDB connection test requested. Continuous telemetry is ${settings.influxEnabled == true ? 'enabled' : 'disabled'}; test writes are independent of that setting."

    Map config = influxConfig()
    if (!config.valid) {
        state.influxTestStatus = 'FAILED'
        state.influxTestDetail = config.error
        noteInfluxFailure(config.error)
        logWarn "InfluxDB connection test FAILED before transmission: ${config.error}"
        return
    }

    writeInfluxEvent('influx.connection_test', [message:'Hubitat AMA Monitoring & Analytics test write', appVersion:version()], 'test')
}

private Map influxConfig() {
    String base = (settings.influxUrl ?: '').toString().trim().replaceAll('/+$', '')
    String org = (settings.influxOrg ?: '').toString().trim()
    String bucket = (settings.influxBucket ?: '').toString().trim()
    String token = (settings.influxToken ?: '').toString().trim()
    if (!base || !(base.toLowerCase().startsWith('http://') || base.toLowerCase().startsWith('https://'))) {
        return [valid:false, error:'InfluxDB URL must begin with http:// or https://.']
    }
    try {
        URI parsed = new URI(base)
        if (!(parsed.scheme?.toLowerCase() in ['http', 'https']) || !parsed.host || parsed.userInfo || parsed.query || parsed.fragment) {
            return [valid:false, error:'InfluxDB URL must be a base address such as http://192.168.1.12:8086, without a path, query, or fragment.']
        }
        if (parsed.port < -1 || parsed.port > 65535) {
            return [valid:false, error:'InfluxDB URL contains an invalid port number.']
        }
    } catch (Exception ignored) {
        return [valid:false, error:'InfluxDB URL could not be parsed. Use a base address such as http://192.168.1.12:8086.']
    }
    if (!org) return [valid:false, error:'InfluxDB organization is required.']
    if (!bucket) return [valid:false, error:'InfluxDB bucket is required.']
    if (!token) return [valid:false, error:'InfluxDB API token is required.']
    return [valid:true, url:base, org:org, bucket:bucket, token:token]
}

private void writeInfluxEvent(String eventName, Map payload = [:], String kind = 'event') {
    Map cfg = influxConfig()
    if (!cfg.valid) {
        noteInfluxFailure(cfg.error)
        return
    }

    String measurement = 'ama_hubitat'
    String tags = "source=hubitat,module=monitoring_analytics,kind=${escapeInfluxTag(kind)},event=${escapeInfluxTag(eventName ?: 'unknown')}"
    String payloadJson
    try { payloadJson = JsonOutput.toJson(payload ?: [:]) }
    catch (Exception ignored) { payloadJson = '{}' }
    if (payloadJson.length() > 3000) payloadJson = payloadJson.take(2997) + '...'

    String fields = "event_name=${escapeInfluxString(eventName ?: 'unknown')},payload=${escapeInfluxString(payloadJson)},app_version=${escapeInfluxString(version())}"
    String line = "${measurement},${tags} ${fields} ${now()}"
    String query = "${cfg.url}/api/v2/write?org=${urlEncode(cfg.org)}&bucket=${urlEncode(cfg.bucket)}&precision=ms"

    Map params = [
        uri: query,
        headers: [
            'Authorization': "Token ${cfg.token}",
            'Content-Type': 'text/plain; charset=utf-8',
            'Accept': 'application/json'
        ],
        body: line,
        contentType: 'text/plain'
    ]

    try {
        asynchttpPost('influxWriteHandler', params, [eventName:eventName ?: 'unknown', kind:kind ?: 'event'])
    } catch (Exception e) {
        String detail = "Could not submit write request: ${e.message}"
        if (kind == 'test') {
            state.influxTestStatus = 'FAILED'
            state.influxTestDetail = detail
            logWarn "InfluxDB connection test FAILED: ${detail}"
        }
        noteInfluxFailure(detail)
    }
}

void influxWriteHandler(resp, data) {
    Integer status = null
    try { status = resp?.status as Integer } catch (ignored) {}
    if (status != null && status >= 200 && status < 300) {
        state.influxLastSuccess = new Date().format("yyyy-MM-dd HH:mm:ss", location?.timeZone ?: TimeZone.getDefault())
        state.influxLastError = null
        state.influxFailureCount = 0
        if (data?.kind == 'test') {
            state.influxTestStatus = 'SUCCESS'
            state.influxTestDetail = "InfluxDB accepted the test point (HTTP ${status})."
            logInfo "InfluxDB connection test SUCCESS (HTTP ${status}); test point accepted by bucket '${settings.influxBucket}'."
        } else {
            logDebug "InfluxDB write succeeded (${data?.kind ?: 'event'}: ${data?.eventName ?: 'unknown'})."
        }
    } else {
        String detail = "HTTP ${status ?: 'unknown'}"
        try {
            if (resp?.data) detail += ": ${resp.data.toString().take(240)}"
        } catch (ignored) {}
        if (data?.kind == 'test') {
            state.influxTestStatus = 'FAILED'
            state.influxTestDetail = detail
        }
        noteInfluxFailure(detail)
        if (data?.kind == 'test') logWarn "InfluxDB connection test FAILED: ${detail}"
    }
}

private void noteInfluxFailure(String message) {
    state.influxLastError = (message ?: 'Unknown InfluxDB error').take(300)
    state.influxFailureCount = ((state.influxFailureCount ?: 0) as Integer) + 1
    logWarn "InfluxDB reporting failed: ${state.influxLastError}"
}

private String escapeInfluxTag(String value) {
    return (value ?: '').toString().replace('\\', '\\\\').replace(',', '\\,').replace(' ', '\\ ').replace('=', '\\=')
}

private String escapeInfluxString(String value) {
    String safe = (value ?: '').toString().replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r')
    return '"' + safe + '"'
}

private String urlEncode(String value) {
    return java.net.URLEncoder.encode((value ?: '').toString(), 'UTF-8')
}

private String htmlSafe(String value) {
    return (value ?: '').toString().replace('&','&amp;').replace('<','&lt;').replace('>','&gt;').replace('"','&quot;').replace("'",'&#39;')
}

private Map compactPayload(Map payload) {
    Map compact = [:]
    int count = 0
    (payload ?: [:]).each { key, value ->
        if (count >= 12) return
        String k = key?.toString()
        if (!k) return
        if (value instanceof Map) {
            Map nested = [:]
            int nestedCount = 0
            value.each { nk, nv ->
                if (nestedCount < 8 && nv != null && !(nv instanceof Map) && !(nv instanceof Collection)) {
                    String textValue = nv.toString()
                    nested[nk.toString()] = textValue.length() > 160 ? textValue.take(157) + '...' : textValue
                    nestedCount++
                }
            }
            compact[k] = nested
        } else if (value instanceof Collection) {
            compact[k] = "(${value.size()} items)"
        } else if (value != null) {
            String textValue = value.toString()
            compact[k] = textValue.length() > 160 ? textValue.take(157) + '...' : textValue
        }
        count++
    }
    return compact
}

public List getRecentEvents() { return (state.eventHistory instanceof List) ? state.eventHistory : [] }

public Map getModuleStatus() {
    return [
        installed:true,
        module:'Monitoring & Analytics',
        version:version(),
        eventCount:getRecentEvents().size(),
        lastEventAt:state.lastEventAt,
        influxEnabled:settings.influxEnabled == true,
        influxLastSuccess:state.influxLastSuccess,
        influxLastError:state.influxLastError,
        influxFailureCount:state.influxFailureCount ?: 0,
        nasStatus:state.nasStatus ?: 'UNKNOWN',
        influxHealth:state.nasInfluxHealth ?: 'UNKNOWN',
        influxWriteStatus:state.nasInfluxWriteStatus ?: 'UNKNOWN',
        heartbeatStatus:state.hubitatHeartbeatStatus ?: 'UNKNOWN',
        telemetryStatus:state.telemetryStatus ?: 'UNKNOWN',
        amazonInventoryStatus:state.amazonInventoryStatus ?: [:],
        amazonReconciliationSummary:state.amazonReconciliationSummary ?: [:],
        amazonInventoryError:state.amazonInventoryError,
        lastHeartbeatSentAt:state.lastHeartbeatSentAt
    ]
}

private void updateAppLabel() {
    String expected = 'AMA Monitoring & Analytics v' + version()
    if (app.label != expected) app.updateLabel(expected)
}

private void logInfo(String message) { if (settings.logInfoEnable != false) log.info "AMA Monitoring: ${message}" }
private void logDebug(String message) { if (settings.logDebugEnable == true) log.debug "AMA Monitoring: ${message}" }
private void logWarn(String message) { log.warn "AMA Monitoring: ${message}" }
