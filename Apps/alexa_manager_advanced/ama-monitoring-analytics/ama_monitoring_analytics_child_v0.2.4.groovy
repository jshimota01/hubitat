/**
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
 * Version: 0.2.4 | 2026/11/10
 * Hubitat-side event capture and parent-mediated NAS telemetry forwarding.
 * All inter-module events arrive through the Parent. InfluxDB is optional:
 * telemetry failures must never interrupt Hubitat control or AMA operation.
 *
 * Changelog:
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

String version() { '0.2.4' }
String timeStamp() { '2026/11/10' }

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
            paragraph 'Receives application events through the Parent and forwards complete event payloads to the AMA NAS for InfluxDB normalization.'
        }
        section('MODULE STATUS') {
            paragraph "<b>Events retained:</b> ${events.size()} (maximum 200)"
            paragraph "<b>Last event:</b> ${state.lastEventAt ?: 'None received'}"
            paragraph "<b>InfluxDB:</b> ${settings.influxEnabled == true ? 'Enabled' : 'Disabled'}"
            paragraph "<b>Last write:</b> ${state.influxLastSuccess ?: 'No successful write yet'}"
            if (state.influxLastError) paragraph "<span style='color:#B03A2E;'><b>Last InfluxDB error:</b> ${htmlSafe(state.influxLastError)}</span>"
            paragraph "<b>Write failures:</b> ${state.influxFailureCount ?: 0}"
            paragraph "<b>Last connection test:</b> ${state.influxTestStatus ?: 'Not tested'}"
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
}

void updated() {
    updateAppLabel()
    if (!(state.eventHistory instanceof List)) state.eventHistory = []
    logInfo "Updated Monitoring & Analytics v${version()}"
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
        influxFailureCount:state.influxFailureCount ?: 0
    ]
}

private void updateAppLabel() {
    String expected = 'AMA Monitoring & Analytics v' + version()
    if (app.label != expected) app.updateLabel(expected)
}

private void logInfo(String message) { if (settings.logInfoEnable != false) log.info "AMA Monitoring: ${message}" }
private void logDebug(String message) { if (settings.logDebugEnable == true) log.debug "AMA Monitoring: ${message}" }
private void logWarn(String message) { log.warn "AMA Monitoring: ${message}" }
