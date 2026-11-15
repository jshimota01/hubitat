/**
 * v0.2.1 - 2026/11/10 - Aligns the InfluxDB-enabled Monitoring & Analytics app identity with the canonical parent branch name.
 * Alexa Manager Advanced - Monitoring & Analytics Child
 * Version: 0.2.1 | 2026/11/10
 * Hubitat-side diagnostics and direct InfluxDB telemetry writer.
 * All inter-module events arrive through the Parent. InfluxDB is optional:
 * telemetry failures must never interrupt Hubitat control or AMA operation.
 *
 * Changelog:
 * v0.2.0 - 2026/11/10 - Adds direct Hubitat-to-InfluxDB 2.x telemetry.
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

String version() { '0.2.1' }
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
            paragraph 'Receives application events through the Parent and independently reports Hubitat-side telemetry to InfluxDB.'
        }
        section('MODULE STATUS') {
            paragraph "<b>Events retained:</b> ${events.size()} (maximum 200)"
            paragraph "<b>Last event:</b> ${state.lastEventAt ?: 'None received'}"
            paragraph "<b>InfluxDB:</b> ${settings.influxEnabled == true ? 'Enabled' : 'Disabled'}"
            paragraph "<b>Last write:</b> ${state.influxLastSuccess ?: 'No successful write yet'}"
            if (state.influxLastError) paragraph "<span style='color:#B03A2E;'><b>Last InfluxDB error:</b> ${htmlSafe(state.influxLastError)}</span>"
            paragraph "<b>Write failures:</b> ${state.influxFailureCount ?: 0}"
        }
        section('INFLUXDB 2.x CONNECTION') {
            input name:'influxEnabled', type:'bool', title:'Enable Hubitat telemetry reporting', defaultValue:false
            input name:'influxUrl', type:'text', title:'InfluxDB Server URL', description:'Example: http://192.168.1.12:8086', defaultValue:'http://192.168.1.12:8086', required:true
            input name:'influxOrg', type:'text', title:'Organization', defaultValue:'DundeeHome', required:true
            input name:'influxBucket', type:'text', title:'Bucket', defaultValue:'HubAlexa', required:true
            input name:'influxToken', type:'password', title:'AMA-Hubitat-Writer token', required:true
            input name:'btnTestInflux', type:'button', title:'Test InfluxDB Write'
            paragraph 'The token should have Read/Write access to HubAlexa only. It is stored in this Hubitat app preference and is never included in telemetry fields or log messages.'
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
        writeInfluxEvent(event.event, compact, 'event')
    }
}

void appButtonHandler(String buttonName) {
    if (buttonName != 'btnTestInflux') return
    Map config = influxConfig()
    if (!config.valid) {
        state.influxLastError = config.error
        logWarn "InfluxDB test not sent: ${config.error}"
        return
    }
    writeInfluxEvent('influx.connection_test', [message:'Hubitat AMA Monitoring & Analytics test write', appVersion:version()], 'test')
}

private Map influxConfig() {
    String base = (settings.influxUrl ?: '').toString().trim().replaceAll('/+$', '')
    String org = (settings.influxOrg ?: '').toString().trim()
    String bucket = (settings.influxBucket ?: '').toString().trim()
    String token = (settings.influxToken ?: '').toString().trim()
    if (!base || !(base ==~ /^https?:\/\/[^\/\s]+(?::\d+)?$/)) return [valid:false, error:'Enter a valid InfluxDB base URL, including http:// or https://.']
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
        noteInfluxFailure("Could not submit write request: ${e.message}")
    }
}

void influxWriteHandler(resp, data) {
    Integer status = null
    try { status = resp?.status as Integer } catch (ignored) {}
    if (status != null && status >= 200 && status < 300) {
        state.influxLastSuccess = new Date().format("yyyy-MM-dd HH:mm:ss", location?.timeZone ?: TimeZone.getDefault())
        state.influxLastError = null
        state.influxFailureCount = 0
        logDebug "InfluxDB write succeeded (${data?.kind ?: 'event'}: ${data?.eventName ?: 'unknown'})."
    } else {
        String detail = "HTTP ${status ?: 'unknown'}"
        try {
            if (resp?.data) detail += ": ${resp.data.toString().take(240)}"
        } catch (ignored) {}
        noteInfluxFailure(detail)
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
