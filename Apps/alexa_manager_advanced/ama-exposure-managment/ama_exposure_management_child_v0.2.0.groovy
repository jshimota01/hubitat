/**
 * v0.2.0 - 2026/11/11 - Adds parent-fed Amazon inventory reconciliation and endpoint ownership visibility.
 *   - Compares prepared Hubitat candidates with observed AMA, legacy, and third-party endpoints.
 *   - Reports disabled, unreachable, duplicate, missing-approved, and orphaned AMA endpoints.
 *   - Keeps all endpoint mutations disabled; this release is read-only reconciliation only.
 * v0.1.2 - 2026/11/10 - Aligns Exposure Management app identity and parent-facing module naming.
 * v0.1.1 - 2026/11/10 - Aligns the Hubitat child app type and parent-facing name with Exposure Management.
 * Alexa Manager Advanced - Exposure Management Child Application
 * Version: 0.2.0 | 2026/11/11
 *
 * Owns the lifecycle of prepared candidates as Alexa endpoints.
 * All communication with other branches is mediated by the AMA Parent.
 *
 * This initial branch establishes the application boundary only.
 * It does not register, update, disable, or remove Alexa endpoints.
 */
String version() { '0.2.0' }
String timeStamp() { '2026/11/11' }

definition(name: 'AMA Exposure Management', namespace: 'jshimota', author: 'James Shimota',
    description: 'Owns Alexa endpoint exposure and lifecycle for prepared AMA candidates.',
    category: 'Integrations', parent: 'jshimota:Alexa Manager Advanced',
    iconUrl: '', iconX2Url: '', iconX3Url: '', importUrl: '')

preferences { page(name: 'mainPage'); page(name: 'reconciliationPage') }

def mainPage() {
    updateAppLabel()
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'><h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Exposure Management</h2><span style='font-size:12px;'>Alexa endpoint lifecycle v${version()} (${timeStamp()})</span></div>"
            paragraph 'This branch compares intended AMA exposure with the live Amazon inventory supplied by the Parent. Endpoint registration, update, disablement, and removal remain separate controlled operations.'
        }
        Map status = parent?.getAmazonInventoryStatus() ?: [:]
        Map reconciliation = parent?.buildAmazonReconciliationSummary() ?: [:]
        section('AMAZON INVENTORY') {
            paragraph "<b>Snapshot:</b> ${status.available == true ? (status.fresh == true ? 'CURRENT' : 'STALE') : 'NOT CHECKED'} | <b>Endpoints:</b> ${status.endpointCount ?: 0}"
            paragraph "<b>Last refresh:</b> ${status.retrievedAt ?: 'Never'}"
            Map counts = status.counts instanceof Map ? status.counts : [:]
            int otherCount = Math.max(0, ((status.endpointCount ?: 0) as Integer) - ((counts.AMA ?: 0) as Integer) - ((counts.LEGACY_HUBITAT ?: 0) as Integer))
            paragraph "<b>AMA:</b> ${counts.AMA ?: 0} | <b>Legacy Hubitat:</b> ${counts.LEGACY_HUBITAT ?: 0} | <b>Other integrations:</b> ${otherCount}"
            String sourceBreakdown = counts.keySet().collect { key -> "${key}: ${counts[key]}" }.sort().join(' | ')
            paragraph "<b>Source breakdown:</b> ${htmlSafe(sourceBreakdown ?: 'Not available')}"
        }
        section('RECONCILIATION FINDINGS') {
            paragraph "<b>Approved candidates:</b> ${reconciliation.approvedCandidates ?: 0}"
            paragraph "<b>Approved but not exposed at all:</b> ${reconciliation.approvedNotExposed ?: 0}"
            paragraph "<b>Approved with legacy Hubitat exposure:</b> ${reconciliation.approvedWithLegacyExposure ?: 0}"
            paragraph "<b>Approved with other integration exposure:</b> ${reconciliation.approvedWithOtherExposure ?: 0}"
            paragraph "<b>Candidate endpoints owned by legacy Hubitat:</b> ${reconciliation.candidatesWithLegacyHubitat ?: 0}"
            paragraph "<b>Disabled AMA endpoints:</b> ${reconciliation.disabledAmaEndpoints ?: 0}"
            paragraph "<b>Unreachable AMA endpoints:</b> ${reconciliation.unreachableAmaEndpoints ?: 0}"
            paragraph "<b>Duplicate AMA matches:</b> ${reconciliation.duplicateAmaMatches ?: 0}"
            paragraph "<b>Devices exposed by multiple integrations:</b> ${reconciliation.multiOwnerExposureCount ?: 0}"
            paragraph "<b>AMA endpoints without a current candidate:</b> ${reconciliation.amaEndpointsWithoutCandidate ?: 0}"
            href name: 'openReconciliation', page: 'reconciliationPage', title: 'Review Candidate Exposure'
        }
        section('OPERATIONAL BOUNDARY') {
            paragraph '<b>Read-only reconciliation:</b> Enabled.'
            paragraph '<b>Alexa endpoint mutations:</b> Not enabled in this release.'
            paragraph 'No endpoint is created, changed, disabled, or deleted by inventory refresh or reconciliation.'
        }
    }
}



def reconciliationPage() {
    updateAppLabel()
    Map inventory = parent?.getDeviceInventory() ?: [:]
    Map records = parent?.getManagementRecords() ?: [:]
    List ids = inventory.keySet().collect { it.toString() }.sort { a, b ->
        String an = inventory[a]?.hubitatLabel ?: inventory[a]?.hubitatName ?: a
        String bn = inventory[b]?.hubitatLabel ?: inventory[b]?.hubitatName ?: b
        an.toLowerCase() <=> bn.toLowerCase()
    }
    Map options = [:]
    ids.each { id ->
        Map device = inventory[id] instanceof Map ? inventory[id] as Map : [:]
        options[id] = "${device.hubitatLabel ?: device.hubitatName ?: 'Device'} (ID ${id})"
    }
    String selectedId = settings.reconciliationDeviceId?.toString()
    Map device = selectedId && inventory[selectedId] instanceof Map ? inventory[selectedId] as Map : [:]
    Map record = selectedId && records[selectedId] instanceof Map ? records[selectedId] as Map : [:]
    Map exposure = selectedId ? (parent?.getAmazonExposureForDevice(selectedId) ?: [:]) : [:]

    dynamicPage(name: 'reconciliationPage', title: 'Candidate Exposure Reconciliation', install: false, uninstall: false) {
        section() {
            paragraph "<b>Hubitat candidates:</b> ${inventory.size()} | <b>Management records:</b> ${records.size()}"
            if (ids) input name:'reconciliationDeviceId', type:'enum', title:'Select candidate', options:options, required:false, submitOnChange:true
            else paragraph 'No Candidate Selection inventory is available yet.'
        }
        if (device) {
            section('INTENDED AMA STATE') {
                paragraph "<b>Hubitat device:</b> ${htmlSafe(device.hubitatLabel ?: device.hubitatName)} (ID ${htmlSafe(selectedId)})"
                paragraph "<b>Approval:</b> ${record.approvedForAmazon == true ? 'APPROVED' : 'NOT APPROVED'}"
                paragraph "<b>Prepared Amazon name:</b> ${htmlSafe(record.amazonName ?: device.amazonName ?: device.hubitatLabel ?: device.hubitatName)}"
                paragraph "<b>Preparation status:</b> ${htmlSafe(record.exposureStatus ?: device.exposureStatus ?: 'NOT_SET')}"
            }
            section('OBSERVED AMAZON STATE') {
                paragraph "<b>Observation:</b> ${htmlSafe(exposure.status ?: 'NOT_CHECKED')} | <b>Snapshot current:</b> ${exposure.snapshotFresh == true ? 'Yes' : 'No'}"
                if (exposure.endpoints instanceof List && exposure.endpoints) {
                    exposure.endpoints.each { endpoint ->
                        paragraph "<b>Owner:</b> ${htmlSafe(endpoint.owner)} | <b>Endpoint name:</b> ${htmlSafe(endpoint.name)}"
                        paragraph "<b>Skill stage:</b> ${htmlSafe(endpoint.skillStage)} | <b>Enabled:</b> ${endpoint.isEnabled == null ? 'Unknown' : (endpoint.isEnabled ? 'Yes' : 'No')} | <b>Reachability:</b> ${htmlSafe(endpoint.reachability)}"
                        paragraph "<b>Amazon entity ID:</b> ${htmlSafe(endpoint.entityId)} | <b>Appliance ID:</b> ${htmlSafe(endpoint.applianceId)}"
                    }
                } else if (exposure.status == 'NOT_FOUND') {
                    paragraph 'No endpoint for this device was found in the current Amazon snapshot.'
                } else {
                    paragraph 'No current observation is available; absence must not be inferred.'
                }
            }
            section('RECONCILIATION RESULT') {
                String result = 'NOT_CHECKED'
                if (exposure.checked == true) {
                    if (exposure.status == 'MULTI_OWNER_EXPOSURE') result = 'MULTIPLE_INTEGRATIONS_PRESENT'
                    else if (exposure.status == 'DUPLICATE_ENDPOINTS') result = 'DUPLICATE_ENDPOINTS_REVIEW'
                    else if (exposure.endpoints?.any { it.owner == 'AMA' }) result = 'AMA_ENDPOINT_PRESENT'
                    else if (exposure.endpoints?.any { it.owner == 'LEGACY_HUBITAT' }) result = 'LEGACY_HUBITAT_OWNS_EXPOSURE'
                    else if (exposure.endpoints) result = 'OTHER_INTEGRATION_OWNS_EXPOSURE'
                    else result = record.approvedForAmazon == true ? 'APPROVED_BUT_NOT_EXPOSED' : 'NOT_EXPOSED'
                } else if (exposure.status == 'NOT_CHECKED_STALE') result = 'STALE_INVENTORY'
                paragraph "<b>Result:</b> ${result}"
                paragraph 'Reconciliation is observational only. It never changes approval or performs Amazon endpoint operations.'
            }
        }
        section() { href name:'returnToExposure', page:'mainPage', title:'Return to Exposure Management' }
    }
}

void installed() { updateAppLabel(); log.info "Installed AMA Exposure Management v${version()}" }
void updated() { updateAppLabel(); log.info "Updated AMA Exposure Management v${version()}" }

public void recordParentEvent(String eventName, Map payload = [:]) {
    state.lastParentEvent = [event:eventName ?: 'parent.event', at:now(),
                             summary:(payload ?: [:]).keySet().take(12)]
}

public void updateAmazonInventoryStatus(Map status = [:], Map reconciliation = [:]) {
    state.amazonInventoryLastReceivedAt = now()
    state.amazonInventoryStatus = status ?: [:]
    state.amazonReconciliationSummary = reconciliation ?: [:]
    log.info "Exposure Management received Amazon inventory status (${status.endpointCount ?: 0} endpoints)."
}

public Map getModuleStatus() {
    return [installed:true, module:'Exposure Management', version:version(),
            exposureOperationsEnabled:false, reconciliationEnabled:true,
            amazonInventoryReceivedAt:state.amazonInventoryLastReceivedAt,
            reconciliation:state.amazonReconciliationSummary ?: [:], lastParentEvent:state.lastParentEvent]
}

private String htmlSafe(Object value) {
    if (value == null) return 'Not reported'
    String s = value.toString()
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
             .replace('"', '&quot;').replace("'", '&#39;')
}

private void updateAppLabel() {
    String expected = 'AMA Exposure Management v' + version()
    if (app.label != expected) app.updateLabel(expected)
}
