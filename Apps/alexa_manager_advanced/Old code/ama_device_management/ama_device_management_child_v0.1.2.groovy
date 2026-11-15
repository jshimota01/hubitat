/**
 * Alexa Manager Advanced - Device Management Child
 * Version: 0.1.2 | 2026/11/09
 * Owns AMA-side device review and management metadata. Inventory is read only through
 * the parent API. This module never calls Device Handling or Monitoring directly.
 * No Amazon endpoint registration/removal is performed in this foundation release.
 *
 * Changelog:
 * v0.1.2 - 2026/11/09 - Establishes parent-mediated inventory consumption and durable review records.
 *   - Adds public APIs for module status, inventory update receipt, and management records.
 *   - Adds versioned Hubitat Apps label and module status UI.
 *   - Keeps approval false and exposure NOT_EXPOSED unless explicitly managed in a later release.
 * v0.1.1 - 2026/11/09 - Initial branch placeholder.
 */
String version() { '0.1.2' }
String timeStamp() { '2026/11/09' }

definition(name: 'AMA Device Management Child', namespace: 'jshimota', author: 'James Shimota',
    description: 'Reviews discovered devices and manages explicit Amazon exposure approval.',
    category: 'Integrations', parent: 'jshimota:Alexa Manager Advanced', iconUrl: '', iconX2Url: '', iconX3Url: '', importUrl: '')

preferences { page(name: 'mainPage') }

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
        section('MANAGEMENT SCOPE') {
            paragraph 'Hubitat Name, Hubitat Label, and Amazon Name; capability/category mapping; naming conflict analysis; explicit approval; exposure status; and voice recognition test records.'
            paragraph '<b>Safety:</b> Discovery alone never approves or publishes a device. This foundation release performs no Amazon registration or deregistration.'
        }
        section('LOGGING', hideable:true, hidden:true) {
            input name:'logInfoEnable', type:'bool', title:'Enable Info Logging', defaultValue:true
            input name:'logDebugEnable', type:'bool', title:'Enable Debug Logging', defaultValue:false
        }
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
    if (updatedRecord.approvedForAmazon != true && !updatedRecord.exposureStatus) updatedRecord.exposureStatus = 'NOT_EXPOSED'
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

private void updateAppLabel() {
    String expected = 'AMA Device Management Child v' + version()
    if (app.label != expected) app.updateLabel(expected)
}
private void logInfo(String message) { if (settings.logInfoEnable != false) log.info "AMA Device Management: ${message}" }
private void logDebug(String message) { if (settings.logDebugEnable == true) log.debug "AMA Device Management: ${message}" }
private void logWarn(String message) { log.warn "AMA Device Management: ${message}" }
