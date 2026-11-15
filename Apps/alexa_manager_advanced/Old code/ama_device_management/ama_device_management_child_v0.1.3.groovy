/**
 * Alexa Manager Advanced - Device Management Child
 * Version: 0.1.3 | 2026/11/10
 * Owns AMA-side device review and management metadata. Inventory is read only through
 * the parent API. This module never calls Device Handling or Monitoring directly.
 * No Amazon endpoint registration/removal is performed in this foundation release.
 *
 * Changelog:
 * v0.1.3 - 2026/11/10 - Adds device review and explicit Amazon approval controls.
 *   - Adds a selected-device review page with inventory and management metadata.
 *   - Adds explicit approve/revoke actions; approval never performs endpoint registration.
 * v0.1.2 - 2026/11/09 - Establishes parent-mediated inventory consumption and durable review records.
 *   - Adds public APIs for module status, inventory update receipt, and management records.
 *   - Adds versioned Hubitat Apps label and module status UI.
 *   - Keeps approval false and exposure NOT_EXPOSED unless explicitly managed in a later release.
 * v0.1.1 - 2026/11/09 - Initial branch placeholder.
 */
String version() { '0.1.3' }
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
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'><h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Device Management Child</h2><span style='font-size:12px;'>Device review &amp; Amazon management v${version()} (${timeStamp()})</span></div>"
            paragraph 'This branch receives inventory through the AMA parent. It owns naming, capability mapping, review, approval, and Amazon exposure decisions.'
        }
        section('PARENT-PROVIDED INVENTORY') {
            paragraph "<b>Inventory records:</b> ${inventory.size()}"
            paragraph "<b>Last inventory scan:</b> ${status.lastScan ?: 'Not scanned'}"
            paragraph "<b>Management records:</b> ${records.size()}"
            paragraph "<b>Last inventory notification:</b> ${state.lastInventoryReceived ?: 'Not received'}"
            paragraph 'The inventory is read through the parent public API. This child does not access Device Handling state or call Device Handling directly.'
        }
        section('DEVICE REVIEW & APPROVAL') {
            paragraph 'Review individual discovered devices and explicitly approve or revoke their eligibility for future Amazon exposure. Approval is a local management decision only; it does not register or publish an Amazon endpoint.'
            href name: 'openDeviceReview', page: 'reviewApprovalPage', title: 'Review Devices & Manage Approval'
        }
        section('MANAGEMENT SCOPE') {
            paragraph 'Hubitat Name, Hubitat Label, and Amazon Name; capability/category mapping; naming conflict analysis; explicit approval; exposure status; and voice recognition test records.'
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
        String an = inventory[a]?.displayName ?: inventory[a]?.hubitatLabel ?: a
        String bn = inventory[b]?.displayName ?: inventory[b]?.hubitatLabel ?: b
        an.toLowerCase() <=> bn.toLowerCase()
    }
    Map options = [:]
    ids.each { id ->
        Map item = inventory[id] instanceof Map ? inventory[id] : [:]
        String label = item.displayName ?: item.hubitatLabel ?: item.hubitatName ?: "Device ${id}"
        options[id] = "${label} (ID ${id})"
    }
    String selectedId = settings.managementReviewDeviceId?.toString()
    Map device = selectedId && inventory[selectedId] instanceof Map ? inventory[selectedId] : [:]
    Map record = selectedId && records[selectedId] instanceof Map ? records[selectedId] : [:]
    boolean approved = record.approvedForAmazon == true
    String verification = device.verificationStatus ?: 'LEGACY / NOT CLASSIFIED'

    dynamicPage(name: 'reviewApprovalPage', title: 'Device Review & Approval', install: false, uninstall: false) {
        section() {
            paragraph "<b>Discovered devices:</b> ${inventory.size()} &nbsp; <b>Management records:</b> ${records.size()}"
            if (!ids) {
                paragraph 'No discovered devices are available. Run a scan in Device Handling first.'
            } else {
                input name: 'managementReviewDeviceId', type: 'enum', title: 'Select a device to review',
                      options: options, required: false, submitOnChange: true
            }
        }
        if (device) {
            section('DEVICE INFORMATION') {
                paragraph "<b>Name:</b> ${htmlSafe(device.displayName ?: device.hubitatLabel ?: device.hubitatName)}"
                paragraph "<b>Device ID:</b> ${htmlSafe(selectedId)}"
                paragraph "<b>Room:</b> ${htmlSafe(device.roomName)}"
                paragraph "<b>Driver:</b> ${htmlSafe(device.driverType)}"
                paragraph "<b>Manufacturer / Model:</b> ${htmlSafe(device.manufacturer)} / ${htmlSafe(device.model)}"
                paragraph "<b>Verification:</b> ${htmlSafe(verification)}"
                paragraph "<b>Present in Hubitat:</b> ${device.presentInHubitat == true ? 'Verified present' : 'Not currently verified'}"
                paragraph "<b>Capabilities:</b><br/>${formatListForDisplay(device.capabilities as List)}"
                paragraph "<b>Current values:</b><br/>${formatMapForDisplay(device.currentValues as Map)}"
            }
            section('AMAZON MANAGEMENT') {
                paragraph "<b>Amazon Name:</b> ${htmlSafe(record.amazonName ?: device.amazonName)}"
                paragraph "<b>Amazon Capabilities:</b><br/>${formatListForDisplay((record.amazonCapabilities ?: device.amazonCapabilities) as List)}"
                paragraph "<b>Approval:</b> ${approved ? 'APPROVED' : 'NOT APPROVED'}"
                paragraph "<b>Exposure Status:</b> ${htmlSafe(record.exposureStatus ?: device.exposureStatus ?: 'NOT_EXPOSED')}"
                paragraph "<b>Last management update:</b> ${htmlSafe(record.updatedAt)}"
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
        }
        section() {
            href name: 'returnToManagement', page: 'mainPage', title: 'Return to Device Management'
        }
    }
}

void appButtonHandler(String buttonName) {
    String deviceId = settings.managementReviewDeviceId?.toString()
    if (!deviceId) {
        logWarn 'Approval action ignored: no device selected.'
        return
    }
    Map inventory = parent?.getDeviceInventory() ?: [:]
    Map device = inventory[deviceId] instanceof Map ? inventory[deviceId] : [:]

    if (buttonName == 'btnApproveForAmazon') {
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
    logInfo "Installed Device Management Child v${version()}"
}
void updated() {
    updateAppLabel()
    if (!(state.managementRecords instanceof Map)) state.managementRecords = [:]
    logInfo "Updated Device Management Child v${version()}"
}

/* Called only by the parent after it receives an update from Device Handling. */
public void onInventoryUpdated(Map inventoryStatus = [:]) {
    state.lastInventoryReceived = now()
    state.lastInventoryStatus = inventoryStatus ?: [:]
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

public Map getModuleStatus() {
    return [installed:true, module:'Device Management', version:version(),
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
    String expected = 'AMA Device Management Child v' + version()
    if (app.label != expected) app.updateLabel(expected)
}
private void logInfo(String message) { if (settings.logInfoEnable != false) log.info "AMA Device Management: ${message}" }
private void logDebug(String message) { if (settings.logDebugEnable == true) log.debug "AMA Device Management: ${message}" }
private void logWarn(String message) { log.warn "AMA Device Management: ${message}" }
