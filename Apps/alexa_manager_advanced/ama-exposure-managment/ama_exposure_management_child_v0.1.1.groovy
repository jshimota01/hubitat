/**
 * v0.1.1 - 2026/11/10 - Aligns the Hubitat child app type and parent-facing name with Exposure Management.
 * Alexa Manager Advanced - Exposure Management Child Application
 * Version: 0.1.1 | 2026/11/10
 *
 * Owns the lifecycle of prepared candidates as Alexa endpoints.
 * All communication with other branches is mediated by the AMA Parent.
 *
 * This initial branch establishes the application boundary only.
 * It does not register, update, disable, or remove Alexa endpoints.
 */
String version() { '0.1.1' }
String timeStamp() { '2026/11/10' }

definition(name: 'AMA Exposure Management', namespace: 'jshimota', author: 'James Shimota',
    description: 'Owns Alexa endpoint exposure and lifecycle for prepared AMA candidates.',
    category: 'Integrations', parent: 'jshimota:Alexa Manager Advanced',
    iconUrl: '', iconX2Url: '', iconX3Url: '', importUrl: '')

preferences { page(name: 'mainPage') }

def mainPage() {
    updateAppLabel()
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'><h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Exposure Management</h2><span style='font-size:12px;'>Alexa endpoint lifecycle v${version()} (${timeStamp()})</span></div>"
            paragraph 'This branch will manage the controlled transition from prepared Hubitat candidates to Alexa endpoints.'
        }
        section('EXPOSURE RESPONSIBILITIES') {
            paragraph 'Endpoint creation and discovery responses.'
            paragraph 'Endpoint identity, capability advertisement, and synchronization.'
            paragraph 'Endpoint updates, disablement, and removal.'
            paragraph 'Alexa directive routing and endpoint lifecycle diagnostics.'
        }
        section('CURRENT STATUS') {
            paragraph '<b>Exposure operations:</b> Not implemented in this foundation release.'
            paragraph '<b>Prepared candidates received:</b> 0 (parent handoff not yet implemented).'
            paragraph 'No Alexa endpoint registration, update, or removal is performed by this version.'
        }
    }
}

void installed() { updateAppLabel(); log.info "Installed AMA Exposure Management v${version()}" }
void updated() { updateAppLabel(); log.info "Updated AMA Exposure Management v${version()}" }

public void recordParentEvent(String eventName, Map payload = [:]) {
    state.lastParentEvent = [event:eventName ?: 'parent.event', at:now(),
                             summary:(payload ?: [:]).keySet().take(12)]
}

public Map getModuleStatus() {
    return [installed:true, module:'Exposure Management', version:version(),
            exposureOperationsEnabled:false, lastParentEvent:state.lastParentEvent]
}

private void updateAppLabel() {
    String expected = 'AMA Exposure Management v' + version()
    if (app.label != expected) app.updateLabel(expected)
}
