/**
 * Alexa Manager Advanced - Monitoring & Analytics Child
 * Version: 0.1.2 | 2026/11/09
 * Owns Hubitat-side diagnostics and telemetry. Receives events only from the parent.
 * InfluxDB is an optional direct destination from Hubitat; it is not routed through AMA NAS.
 * This foundation release records a bounded local event history; network reporting is not yet enabled.
 *
 * Changelog:
 * v0.1.2 - 2026/11/09 - Establishes parent-mediated event intake and module status API.
 *   - Adds bounded telemetry history, versioned Hubitat Apps label, and status UI.
 *   - Explicitly reserves direct Hubitat-to-InfluxDB reporting as a separate optional feature.
 * v0.1.1 - 2026/11/09 - Initial branch placeholder.
 */
String version() { '0.1.2' }
String timeStamp() { '2026/11/09' }

definition(name: 'AMA Monitoring & Analytics Child', namespace: 'jshimota', author: 'James Shimota',
    description: 'Reports Hubitat-side AMA activity, health, and telemetry.',
    category: 'Integrations', parent: 'jshimota:Alexa Manager Advanced', iconUrl: '', iconX2Url: '', iconX3Url: '', importUrl: '')

preferences { page(name: 'mainPage') }

def mainPage() {
    updateAppLabel()
    List events = getRecentEvents()
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'><h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Monitoring &amp; Analytics Child</h2><span style='font-size:12px;'>Hubitat telemetry v${version()} (${timeStamp()})</span></div>"
            paragraph 'This branch receives module events through the parent and owns Hubitat-side health, diagnostics, event history, and optional direct InfluxDB reporting.'
        }
        section('MODULE STATUS') {
            paragraph "<b>Events retained:</b> ${events.size()} (maximum 200)"
            paragraph "<b>Last event:</b> ${state.lastEventAt ?: 'None received'}"
            paragraph "<b>InfluxDB reporting:</b> Not enabled in this foundation release"
        }
        section('REPORTING BOUNDARY') {
            paragraph 'When implemented, Hubitat telemetry will be sent directly from this child to InfluxDB. It will not pass through the AMA NAS service. NAS-side Amazon/service observations remain the NAS service responsibility.'
        }
        section('LOGGING', hideable:true, hidden:true) {
            input name:'logInfoEnable', type:'bool', title:'Enable Info Logging', defaultValue:true
            input name:'logDebugEnable', type:'bool', title:'Enable Debug Logging', defaultValue:false
        }
    }
}

void installed() { updateAppLabel(); if (!(state.eventHistory instanceof List)) state.eventHistory = []; logInfo "Installed Monitoring & Analytics Child v${version()}" }
void updated() { updateAppLabel(); if (!(state.eventHistory instanceof List)) state.eventHistory = []; logInfo "Updated Monitoring & Analytics Child v${version()}" }

/* Parent is the sole source of inter-module messages. */
public void recordParentEvent(String eventName, Map payload = [:]) {
    List history = getRecentEvents()
    Map event = [event:eventName ?: 'parent.event', timestamp:now(), payload:payload ?: [:]]
    history << event
    if (history.size() > 200) history = history.takeRight(200)
    state.eventHistory = history
    state.lastEventAt = event.timestamp
    logDebug "Received parent event '${event.event}'."
}

public List getRecentEvents() { return (state.eventHistory instanceof List) ? state.eventHistory : [] }
public Map getModuleStatus() { return [installed:true, module:'Monitoring & Analytics', version:version(), eventCount:getRecentEvents().size(), lastEventAt:state.lastEventAt, influxEnabled:false] }

private void updateAppLabel() { String expected = 'AMA Monitoring & Analytics Child v' + version(); if (app.label != expected) app.updateLabel(expected) }
private void logInfo(String message) { if (settings.logInfoEnable != false) log.info "AMA Monitoring: ${message}" }
private void logDebug(String message) { if (settings.logDebugEnable == true) log.debug "AMA Monitoring: ${message}" }
