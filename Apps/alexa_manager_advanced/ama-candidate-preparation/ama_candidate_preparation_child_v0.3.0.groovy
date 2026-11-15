/**
 * Alexa Manager Advanced - Device Management Child
 * Version: 0.3.0 | 2026/11/10
 * Owns AMA-side device review and management metadata. Inventory is read only through
 * the parent API. This module never calls Device Handling or Monitoring directly.
 * No Amazon endpoint registration/removal is performed in this foundation release.
 *
 * Changelog:
 * v0.3.0 - 2026/11/10 - Renames branch to Candidate Preparation and formalizes assessment responsibilities.
 *   - Adds an Alexa Capability Assessment work area and version-awareness summary.
 *   - Clarifies that preparation does not register or remove Alexa endpoints.
 * v0.1.3 - 2026/11/10 - Adds device review and explicit Amazon approval controls.
 *   - Adds a selected-device review page with inventory and management metadata.
 *   - Adds explicit approve/revoke actions; approval never performs endpoint registration.
 * v0.1.2 - 2026/11/09 - Establishes parent-mediated inventory consumption and durable review records.
 *   - Adds public APIs for module status, inventory update receipt, and management records.
 *   - Adds versioned Hubitat Apps label and module status UI.
 *   - Keeps approval false and exposure NOT_EXPOSED unless explicitly managed in a later release.
 * v0.1.1 - 2026/11/09 - Initial branch placeholder.
 */
String version() { '0.3.0' }
String timeStamp() { '2026/11/10' }

definition(name: 'AMA Device Management Child', namespace: 'jshimota', author: 'James Shimota',
    description: 'Reviews discovered devices and manages explicit Amazon exposure approval.',
    category: 'Integrations', parent: 'jshimota:Alexa Manager Advanced', iconUrl: '', iconX2Url: '', iconX3Url: '', importUrl: '')

preferences { page(name: 'mainPage'); page(name: 'reviewApprovalPage') }

def mainPage() {
    updateAppLabel()
    Map inventory = parent?.getDeviceInventory() ?: [:]
    Map status = parent?.getInventoryStatus() ?: [:]
    Map records = getManagementRecords()
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'><h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Candidate Preparation</h2><span style='font-size:12px;'>Candidate assessment &amp; preparation v${version()} (${timeStamp()})</span></div>"
            paragraph 'This branch receives candidates through the AMA parent. It assesses Hubitat capabilities, prepares Amazon Names, evaluates voice risks, and determines which Hubitat functions can be translated to Alexa. It does not create Alexa endpoints.'
        }
        section('PARENT-PROVIDED INVENTORY') {
            paragraph "<b>Inventory records:</b> ${inventory.size()}"
            paragraph "<b>Last inventory scan:</b> ${status.lastScan ?: 'Not scanned'}"
            paragraph "<b>Management records:</b> ${records.size()}"
            paragraph "<b>Voice tests recorded:</b> ${(state.voiceTestLibrary instanceof List) ? state.voiceTestLibrary.size() : 0}"
            paragraph "<b>Last inventory notification:</b> ${state.lastInventoryReceived ?: 'Not received'}"
            paragraph 'The inventory is read through the parent public API. This child does not access Device Handling state or call Device Handling directly.'
        }
        section('CANDIDATE ASSESSMENT & NAMING') {
            paragraph 'Review candidates, assess naming and voice risks, and maintain preparation decisions. Candidate assessment is not endpoint exposure.'
            href name: 'openDeviceReview', page: 'reviewApprovalPage', title: 'Review & Prepare Candidates'
        }
        section('ALEXA CAPABILITY ASSESSMENT') {
            paragraph 'Foundational work area: determine which functions reported by each Hubitat candidate can be represented by the Alexa Smart Home API used by this AMA integration.'
            paragraph '<b>Governing principle:</b> Hubitat determines what a device can do; Alexa Smart Home determines what of that functionality AMA can expose.'
            paragraph 'The assessment will distinguish readable properties, controllable directives, proactively reported events, endpoint metadata, and functions with no useful Alexa mapping.'
            paragraph 'Research is bounded to devices and functions present in this household inventory. Unsupported or irrelevant Alexa features will not become development work.'
            paragraph '<b>Protocol baseline:</b> Alexa Smart Home Skill API payload version 3. Individual Alexa interfaces retain their own interface versions.'
        }
        section('PREPARATION SCOPE') {
            paragraph 'Hubitat identity and driver data; Alexa Capability Assessment; Amazon Name; naming conflict analysis; Group relationships; voice test history; and candidate preparation status.'
            paragraph '<b>Safety:</b> Discovery alone never approves or publishes a device. This release records approval only and performs no Amazon registration or deregistration.'
        }
        section('LOGGING', hideable:true, hidden:true) {
            input name:'logInfoEnable', type:'bool', title:'Enable Info Logging', defaultValue:true
            input name:'logDebugEnable', type:'bool', title:'Enable Debug Logging', defaultValue:false
        }
    }
}

def reviewApprovalPage() {
    updateAppLabel()
    Map inventory = parent?.getDeviceInventory() ?: [:]
    Map records = getManagementRecords()
    List ids = inventory.keySet().collect { it.toString() }.sort { a, b ->
        String an = inventory[a]?.hubitatLabel ?: inventory[a]?.hubitatName ?: a
        String bn = inventory[b]?.hubitatLabel ?: inventory[b]?.hubitatName ?: b
        an.toLowerCase() <=> bn.toLowerCase()
    }
    Map options = [:]
    ids.each { id ->
        Map item = inventory[id] instanceof Map ? inventory[id] : [:]
        String label = item.hubitatLabel ?: item.hubitatName ?: "Device ${id}"
        options[id] = "${label} (ID ${id})"
    }
    String selectedId = settings.managementReviewDeviceId?.toString()
    Map device = selectedId && inventory[selectedId] instanceof Map ? inventory[selectedId] : [:]
    Map record = selectedId && records[selectedId] instanceof Map ? records[selectedId] : [:]
    boolean approved = record.approvedForAmazon == true
    String verification = device.verificationStatus ?: 'LEGACY / NOT CLASSIFIED'
    String amazonName = (record.amazonName ?: device.amazonName ?: device.hubitatLabel ?: device.hubitatName ?: '').toString()
    List voiceWarnings = selectedId ? evaluateVoiceNameChecks(selectedId, inventory, records) : []

    dynamicPage(name: 'reviewApprovalPage', title: 'Candidate Assessment & Naming', install: false, uninstall: false) {
        section() {
            paragraph "<b>Discovered devices:</b> ${inventory.size()} &nbsp; <b>Management records:</b> ${records.size()}"
            if (!ids) {
                paragraph 'No candidates are available. Collect candidate information in Candidate Selection first.'
            } else {
                input name: 'managementReviewDeviceId', type: 'enum', title: 'Select a device to review',
                      options: options, required: false, submitOnChange: true
            }
        }
        if (device) {
            section('DEVICE INFORMATION') {
                paragraph "<b>Hubitat Name:</b> ${htmlSafe(device.hubitatName)}"
                paragraph "<b>Hubitat Label:</b> ${htmlSafe(device.hubitatLabel)}"
                paragraph "<b>Device ID:</b> ${htmlSafe(selectedId)}"
                paragraph "<b>Room:</b> ${htmlSafe(device.roomName)}"
                paragraph "<b>Driver:</b> ${htmlSafe(device.driverType)}"
                paragraph "<b>Driver Namespace:</b> ${htmlSafe(device.driverNamespace)}"
                paragraph "<b>Driver Version:</b> ${htmlSafe(device.driverVersion ?: 'Not published by driver')}"
                paragraph "<b>Hubitat Platform:</b> ${htmlSafe(device.hubitatPlatformVersion)}"
                paragraph "<b>Manufacturer / Model:</b> ${htmlSafe(device.manufacturer)} / ${htmlSafe(device.model)}"
                paragraph "<b>Verification:</b> ${htmlSafe(verification)}"
                paragraph "<b>Present in Hubitat:</b> ${device.presentInHubitat == true ? 'Verified present' : 'Not currently verified'}"
                paragraph "<b>Is Group Device:</b> ${device.isGroupDevice == true ? 'Yes' : 'No'}"
                paragraph "<b>Is Group Member:</b> ${htmlSafe(device.groupMembershipStatus ?: 'NOT_ASSESSED')}"
                paragraph "<b>Capabilities:</b><br/>${formatListForDisplay(device.capabilities as List)}"
                paragraph "<b>Current values:</b><br/>${formatMapForDisplay(device.currentValues as Map)}"
            }
            section('ALEXA CAPABILITY ASSESSMENT') {
                paragraph '<b>Assessment status:</b> NOT ASSESSED'
                paragraph 'Hubitat capabilities are collected as evidence. Candidate Preparation will map each relevant function to an Alexa Smart Home interface, including the interface version and supported properties/directives/events.'
                paragraph "<b>Hubitat capabilities detected:</b><br/>${formatListForDisplay(device.capabilities as List)}"
                paragraph 'No Alexa capability is marked supported until its mapping and implementation requirements have been reviewed.'
            }
            section('AMAZON MANAGEMENT') {
                paragraph "<b>Amazon Name:</b> Defaults to Hubitat Label. Saved custom names are preserved."
                input name: "amazonNameInput_${selectedId}", type: 'text', title: 'Amazon Name', defaultValue: amazonName, required: true
                input name: 'btnSaveAmazonName', type: 'button', title: 'Save Amazon Name'
                paragraph "<b>Amazon Capabilities:</b><br/>${formatListForDisplay((record.amazonCapabilities ?: device.amazonCapabilities) as List)}"
                paragraph "<b>Approval:</b> ${approved ? 'APPROVED' : 'NOT APPROVED'}"
                paragraph "<b>Exposure Status:</b> ${htmlSafe(record.exposureStatus ?: device.exposureStatus ?: 'NOT_EXPOSED')}"
                paragraph "<b>Last management update:</b> ${htmlSafe(record.updatedAt)}"
                paragraph '<b>VOICE RECOGNITION CHECKS</b>'
                if (voiceWarnings) {
                    voiceWarnings.each { warning ->
                        paragraph "<span style='color:#B9770E;'><b>${htmlSafe(warning.title)}:</b></span> ${htmlSafe(warning.detail)}"
                    }
                } else {
                    paragraph 'No potential naming conflicts detected by the current checks. This is not a guarantee of Alexa recognition.'
                }
                paragraph 'These are advisory findings. They do not reject devices or automatically change names.'
                if (approved) {
                    /* Revocation must remain possible even when the device is no longer verified. */
                    input name: 'btnRevokeAmazonApproval', type: 'button', title: 'Revoke Amazon Approval'
                } else if (device.presentInHubitat == true) {
                    input name: 'btnApproveForAmazon', type: 'button', title: 'Approve for Amazon'
                } else {
                    paragraph "<span style='color:#B03A2E;'><b>Approval is disabled because this device was not verified present in the latest scan.</b></span>"
                }
                paragraph 'Approval only records your decision. No Amazon endpoint is created, updated, or deleted by this action.'
            }
            section('VOICE RECOGNITION TEST') {
                input name: 'voiceTestPhrase', type: 'text', title: 'Phrase spoken to Alexa', required: false
                input name: 'voiceTestResult', type: 'enum', title: 'Observed result',
                      options: ['CORRECT':'Correct target','WRONG':'Wrong target','CLARIFICATION':'Alexa asked for clarification','NOT_FOUND':'Device not found'],
                      required: false, defaultValue: 'CORRECT'
                input name: 'voiceTestEcho', type: 'text', title: 'Echo device used', required: false
                input name: 'btnRecordVoiceTest', type: 'button', title: 'Record Voice Test'
                List recentTests = getVoiceTestLibrary(10).findAll { it.expectedDeviceId?.toString() == selectedId }
                paragraph "<b>Recorded tests for this device:</b> ${recentTests.size()} (latest 10 shown)"
                recentTests.each { test ->
                    paragraph "${htmlSafe(test.timestamp)} — ${htmlSafe(test.result)} — “${htmlSafe(test.phrase)}” via ${htmlSafe(test.echoDevice)}"
                }
            }
        }
        section() {
            href name: 'returnToManagement', page: 'mainPage', title: 'Return to Candidate Preparation'
        }
    }
}

void appButtonHandler(String buttonName) {
    String deviceId = settings.managementReviewDeviceId?.toString()
    if (!deviceId) {
        logWarn 'Management action ignored: no device selected.'
        return
    }
    Map inventory = parent?.getDeviceInventory() ?: [:]
    Map device = inventory[deviceId] instanceof Map ? inventory[deviceId] : [:]
    Map currentRecord = getManagementRecord(deviceId)

    if (buttonName == 'btnSaveAmazonName') {
        String inputName = "amazonNameInput_${deviceId}"
        String proposedName = settings[inputName]?.toString()?.trim()
        if (!proposedName) {
            logWarn "Amazon Name for ${deviceId} was not saved: name cannot be blank."
            return
        }
        saveManagementRecord(deviceId, [amazonName:proposedName, amazonNameSource:'USER'])
        logInfo "Saved user-selected Amazon Name for device ${deviceId}."
    } else if (buttonName == 'btnRecordVoiceTest') {
        String phrase = settings.voiceTestPhrase?.toString()?.trim()
        if (!phrase) {
            logWarn 'Voice test was not recorded: enter the phrase spoken to Alexa.'
            return
        }
        String amazonName = (currentRecord.amazonName ?: device.amazonName ?: device.hubitatLabel ?: device.hubitatName ?: '').toString()
        Map testRecord = [
            timestamp:new Date().format("yyyy-MM-dd HH:mm:ss", location?.timeZone ?: TimeZone.getDefault()),
            expectedDeviceId:deviceId,
            expectedHubitatName:device.hubitatName,
            expectedHubitatLabel:device.hubitatLabel,
            expectedAmazonName:amazonName,
            phrase:phrase,
            result:(settings.voiceTestResult ?: 'CORRECT').toString(),
            echoDevice:(settings.voiceTestEcho ?: '').toString().trim(),
            roomName:device.roomName,
            groupMembershipStatus:device.groupMembershipStatus ?: 'NOT_DETECTED',
            voiceWarnings:evaluateVoiceNameChecks(deviceId, inventory, getManagementRecords()).collect { it.code }
        ]
        parent?.recordVoiceTest(testRecord)
        logInfo "Submitted voice recognition test through the parent for device ${deviceId}: ${testRecord.result}."
    } else if (buttonName == 'btnApproveForAmazon') {
        if (!device || device.presentInHubitat != true) {
            logWarn "Approval action ignored for ${deviceId}: device is not verified present."
            return
        }
        saveManagementRecord(deviceId, [
            approvedForAmazon:true,
            exposureStatus:'APPROVED_NOT_EXPOSED'
        ])
        logInfo "Device ${deviceId} explicitly approved for future Amazon exposure. No endpoint registration performed."
    } else if (buttonName == 'btnRevokeAmazonApproval') {
        saveManagementRecord(deviceId, [
            approvedForAmazon:false,
            exposureStatus:'NOT_EXPOSED'
        ])
        logInfo "Amazon approval revoked for device ${deviceId}. No endpoint deregistration performed."
    }
}

void installed() {
    updateAppLabel()
    if (!(state.managementRecords instanceof Map)) state.managementRecords = [:]
    logInfo "Installed Candidate Preparation v${version()}"
}
void updated() {
    updateAppLabel()
    if (!(state.managementRecords instanceof Map)) state.managementRecords = [:]
    logInfo "Updated Candidate Preparation v${version()}"
}

/* Called only by the parent after it receives an update from Device Handling. */
public void onInventoryUpdated(Map inventoryStatus = [:]) {
    state.lastInventoryReceived = now()
    state.lastInventoryStatus = inventoryStatus ?: [:]

    Map inventory = parent?.getDeviceInventory() ?: [:]
    Map records = getManagementRecords()
    boolean changed = false
    records.each { deviceId, record ->
        if (record instanceof Map && record.amazonNameSource == 'DEFAULT_HUBITAT_LABEL') {
            Map device = inventory[deviceId.toString()] instanceof Map ? inventory[deviceId.toString()] : [:]
            String latestLabel = (device.hubitatLabel ?: device.hubitatName ?: '').toString()
            if (latestLabel && record.amazonName?.toString() != latestLabel) {
                record.amazonName = latestLabel
                record.updatedAt = new Date().format("yyyy-MM-dd HH:mm:ss", location?.timeZone ?: TimeZone.getDefault())
                changed = true
            }
        }
    }
    if (changed) state.managementRecords = records
    logInfo "Parent delivered inventory update (${inventoryStatus.deviceCount ?: 0} records)."
}

public Map getManagementRecords() {
    return (state.managementRecords instanceof Map) ? state.managementRecords : [:]
}
public Map getManagementRecord(String deviceId) {
    def value = getManagementRecords()[deviceId]
    return value instanceof Map ? value : [:]
}

/* Management metadata is stored here, separate from scanner-owned inventory. */
public void saveManagementRecord(String deviceId, Map record = [:]) {
    if (!deviceId) return
    Map all = getManagementRecords()
    Map previous = all[deviceId.toString()] instanceof Map ? all[deviceId.toString()] : [:]
    Map updatedRecord = new LinkedHashMap(previous)
    (record ?: [:]).each { key, value -> updatedRecord[key] = value }
    if (!(updatedRecord.amazonName?.toString()?.trim())) {
        Map inventory = parent?.getDeviceInventory() ?: [:]
        Map device = inventory[deviceId.toString()] instanceof Map ? inventory[deviceId.toString()] : [:]
        updatedRecord.amazonName = device.hubitatLabel ?: device.hubitatName ?: ''
        updatedRecord.amazonNameSource = 'DEFAULT_HUBITAT_LABEL'
    }
    updatedRecord.deviceId = deviceId.toString()
    updatedRecord.updatedAt = new Date().format("yyyy-MM-dd HH:mm:ss", location?.timeZone ?: TimeZone.getDefault())
    if (updatedRecord.approvedForAmazon != true) {
        updatedRecord.approvedForAmazon = false
        if (!updatedRecord.exposureStatus) updatedRecord.exposureStatus = 'NOT_EXPOSED'
    }
    all[deviceId.toString()] = updatedRecord
    state.managementRecords = all
    try { parent?.reportChildEvent('device.management.record.updated', [deviceId:deviceId.toString()]) }
    catch (Exception e) { logWarn "Parent event notification failed: ${e.message}" }
}

/* Parent-mediated voice test history. Retention is bounded to 250 records. */
public void recordVoiceTest(Map testRecord = [:]) {
    if (!testRecord?.expectedDeviceId || !testRecord?.phrase) return
    List history = (state.voiceTestLibrary instanceof List) ?
                   new ArrayList(state.voiceTestLibrary) : []
    history << new LinkedHashMap(testRecord)
    if (history.size() > 250) history = history.takeRight(250)
    state.voiceTestLibrary = history
    try { parent?.reportChildEvent('voice.test.recorded', [deviceId:testRecord.expectedDeviceId, result:testRecord.result]) }
    catch (Exception e) { logWarn "Voice test event notification failed: ${e.message}" }
}

public List getVoiceTestLibrary(Integer limit = 50) {
    List history = (state.voiceTestLibrary instanceof List) ? state.voiceTestLibrary : []
    int safeLimit = Math.max(1, Math.min(250, limit ?: 50))
    return history.takeRight(safeLimit).reverse()
}

/*
 * Conservative, advisory checks. These identify patterns for human review;
 * they do not reject devices, change names, or claim to predict Alexa perfectly.
 */
private List evaluateVoiceNameChecks(String deviceId, Map inventory, Map records) {
    Map device = inventory[deviceId] instanceof Map ? inventory[deviceId] : [:]
    if (!device) return []
    String targetName = (records[deviceId]?.amazonName ?: device.amazonName ?: device.hubitatLabel ?: device.hubitatName ?: '').toString()
    String normalized = normalizeVoiceName(targetName)
    List tokens = normalized ? normalized.split(' ').findAll { it } : []
    List warnings = []

    if (normalized) {
        inventory.each { otherId, other ->
            if (otherId.toString() == deviceId || !(other instanceof Map)) return
            String otherName = (records[otherId.toString()]?.amazonName ?: other.amazonName ?: other.hubitatLabel ?: other.hubitatName ?: '').toString()
            String otherNormalized = normalizeVoiceName(otherName)
            if (otherNormalized == normalized) {
                warnings << [code:'EXACT_DUPLICATE', title:'Exact duplicate name', detail:"Another device uses “${otherName}”."]
            } else if (otherNormalized) {
                List otherTokens = otherNormalized.split(' ').findAll { it }
                if (tokens && otherTokens && (tokens[0] == otherTokens[0] || tokens[-1] == otherTokens[-1])) {
                    warnings << [code:'SHARED_PREFIX_SUFFIX', title:'Shared prefix or suffix', detail:"“${otherName}” shares a leading or trailing word."]
                }
                Set common = (tokens as Set).intersect(otherTokens as Set)
                if (common && common.size() >= 1 && (tokens.size() > 1 || otherTokens.size() > 1)) {
                    warnings << [code:'SHARED_NOUN', title:'Shared name component', detail:"“${otherName}” shares ${common.join(', ')}."]
                }
                String room = normalizeVoiceName(device.roomName ?: '')
                String otherRoom = normalizeVoiceName(other.roomName ?: '')
                if (room && room == otherRoom && tokenSimilarity(tokens, otherTokens) >= 0.5d) {
                    warnings << [code:'SAME_ROOM_SIMILAR', title:'Similar names in same room', detail:"“${otherName}” is in the same room and shares several name components."]
                }
                if (pluralVariant(normalized, otherNormalized)) {
                    warnings << [code:'SINGULAR_PLURAL', title:'Singular/plural variation', detail:"“${otherName}” differs by a likely singular/plural form."]
                }
                if (phoneticNameKey(normalized) == phoneticNameKey(otherNormalized) &&
                    normalized != otherNormalized) {
                    warnings << [code:'SIMILAR_SPOKEN_PHRASE', title:'Potentially similar spoken phrase', detail:"“${otherName}” has a similar simplified phonetic signature; test both names with Alexa."]
                }
            }
        }

        String room = normalizeVoiceName(device.roomName ?: '')
        if (room && (normalized.startsWith(room + ' ') || normalized.endsWith(' ' + room))) {
            warnings << [code:'ROOM_NAME_REDUNDANCY', title:'Room-name redundancy', detail:"The Amazon Name already contains the assigned room name “${device.roomName}”."]
        }
        if (tokens.size() == 1 && tokens[0].size() <= 3) {
            warnings << [code:'SHORT_NAME', title:'Very short name', detail:'A very short name or abbreviation may be difficult to distinguish in speech.']
        }
        if (tokens.any { it.size() <= 2 } && tokens.size() > 1) {
            warnings << [code:'SHORTENED_FORM', title:'Possible shortened form', detail:'The name contains a very short token or abbreviation; compare it with other spoken device names.']
        }
        if (tokens.any { it in ['on','off','open','close','stop','start','up','down'] }) {
            warnings << [code:'COMMAND_LANGUAGE', title:'Command-like wording', detail:'The name contains a word commonly used in voice commands; test it in natural phrases.']
        }
    }

    if (device.groupMembershipStatus in ['POTENTIAL_GROUP_MEMBER','POTENTIAL_GROUP_MEMBER_HINT']) {
        warnings << [code:'GROUP_MEMBER', title:'Potential Group member', detail:"Hubitat metadata suggests Group membership. Evidence: ${(device.groupMembershipEvidence ?: []).join('; ')}"]
    }

    Map learning = getNameLearningSummary(targetName)
    if ((learning.correct + learning.problematic) > 0) {
        warnings << [code:'LEARNED_VOICE_RESULTS', title:'Observed Alexa test history',
                     detail:"For this Amazon Name, ${learning.correct} test(s) reached the expected target and ${learning.problematic} test(s) produced a wrong target, clarification, or no match. Treat this as observed evidence, not a universal prediction."]
    }
    return warnings.unique { it.code }
}

private Map getNameLearningSummary(String amazonName) {
    String target = normalizeVoiceName(amazonName)
    int correct = 0
    int problematic = 0
    getVoiceTestLibrary(250).each { test ->
        if (normalizeVoiceName(test.expectedAmazonName?.toString()) == target) {
            if (test.result?.toString() == 'CORRECT') correct++
            else problematic++
        }
    }
    return [correct:correct, problematic:problematic]
}

private String normalizeVoiceName(String value) {
    return (value ?: '').toLowerCase().replaceAll('[^a-z0-9 ]', ' ').replaceAll('\\s+', ' ').trim()
}

private Double tokenSimilarity(List a, List b) {
    Set aa = (a ?: []) as Set
    Set bb = (b ?: []) as Set
    if (!aa || !bb) return 0d
    Set common = aa.intersect(bb)
    return common.size() / (double)Math.max(aa.size(), bb.size())
}

private String phoneticNameKey(String value) {
    return (value ?: '').split(' ').findAll { it }.collect { word ->
        String upper = word.toUpperCase().replaceAll('[^A-Z]', '')
        if (!upper) return ''
        String first = upper[0].toString()
        String tail = upper.substring(1).replaceAll('[BFPV]', '1')
                .replaceAll('[CGJKQSXZ]', '2').replaceAll('[DT]', '3')
                .replaceAll('L', '4').replaceAll('[MN]', '5').replaceAll('R', '6')
                .replaceAll('[AEIOUHWY]', '')
        String collapsed = tail.replaceAll(/(.)\1+/, '$1')
        return (first + collapsed + '000').take(4)
    }.join(' ')
}

private Boolean pluralVariant(String a, String b) {
    if (!a || !b || a == b) return false
    List aa = a.split(' ') as List
    List bb = b.split(' ') as List
    if (aa.size() != bb.size()) return false
    int differences = 0
    for (int i = 0; i < aa.size(); i++) {
        if (aa[i] == bb[i]) continue
        String x = aa[i]
        String y = bb[i]
        if (!(x == y + 's' || y == x + 's' || x == y + 'es' || y == x + 'es')) return false
        differences++
    }
    return differences == 1
}

public Map getModuleStatus() {
    return [installed:true, module:'Candidate Preparation', version:version(),
            managementRecordCount:getManagementRecords().size(),
            lastInventoryReceived:state.lastInventoryReceived]
}

private String htmlSafe(Object value) {
    if (value == null) return 'Not reported'
    String s = value.toString()
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
             .replace('"', '&quot;').replace("'", '&#39;')
}

private String formatListForDisplay(List values) {
    List safe = (values ?: []).collect { htmlSafe(it) }
    return safe ? safe.join('<br/>') : 'None reported'
}

private String formatMapForDisplay(Map values) {
    if (!(values instanceof Map) || !values) return 'None reported'
    return values.collect { key, value ->
        "<b>${htmlSafe(key)}:</b> ${htmlSafe(value)}"
    }.join('<br/>')
}

private void updateAppLabel() {
    String expected = 'AMA Candidate Preparation v' + version()
    if (app.label != expected) app.updateLabel(expected)
}
private void logInfo(String message) { if (settings.logInfoEnable != false) log.info "AMA Candidate Preparation: ${message}" }
private void logDebug(String message) { if (settings.logDebugEnable == true) log.debug "AMA Candidate Preparation: ${message}" }
private void logWarn(String message) { log.warn "AMA Candidate Preparation: ${message}" }
