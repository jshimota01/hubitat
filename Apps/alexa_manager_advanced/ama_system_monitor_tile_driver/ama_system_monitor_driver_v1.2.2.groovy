/**
 * AMA System Monitor Device Driver
 * Platform: Hubitat Elevation
 *
 * Purpose:
 * Dashboard tile and diagnostic attributes for AMA NAS, InfluxDB, and Amazon inventory monitoring.
 *
 * Notes:
 * - Monitoring data is supplied by the AMA Monitoring & Analytics child app.
 * - This driver renders received status and forwards manual requests through Monitoring & Analytics to the Parent.
 * - It does not independently contact NAS/InfluxDB or own recurring monitoring schedules.
 * - Uses the established Hubitat driver logging, lifecycle, and duplicate-suppression conventions.
 **/
/**
 * Copyright 2026 James Shimota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
/**
 * Changelog:
 * v1.2.2 - 2026/11/12 - Removes unsupported device-driver subscriptions and adds explicit tile refresh.
 *   - Removes app-only subscribe() calls and the self-subscription event handler.
 *   - Adds a no-argument refreshTile command for the Monitoring & Analytics child to call after status batches.
 * v1.2.1 - 2026/11/12 - Corrects driver lifecycle handling and makes version identification visible.
 *   - Removes the unsupported unsubscribe() call from the device driver initialization path.
 *   - Logs the version-demarcation banner at INFO so it appears with normal INFO logging enabled.
 * v1.2.0 - 2026/11/12 - Converts the monitor tile into a parent-mediated control surface.
 *   - Removes the exposed updateMonitorTile command; tile rendering is now internal.
 *   - Adds Configure and Refresh capabilities plus a manual Health Check command.
 *   - Sends requests to Monitoring & Analytics, which forwards them through the Parent.
 *   - Subscribes to monitoring attributes so the tile redraws automatically as data arrives.
 *   - Keeps periodic monitoring schedules exclusively in Monitoring & Analytics.
 * v1.1.2 - 2026/11/12 - Adds standardized sendIfChanged() event publishing.
 * v1.1.2 - 2026/11/12 - Adds standardized sendIfChanged() event publishing.
 *   - Uses persistent duplicate suppression for monitorTile HTML output.
 *   - Logs tile refreshes at DEBUG with concise text rather than logging the full HTML payload.
 *   - Retains updateAttribute() for ordinary diagnostic attributes.
 * v1.1.1 - 2026/11/12 - Applies the standard Hubitat driver template conventions.
 *   - Adds centralized five-level logging preferences, version demarcation, and 30-minute debug timeout.
 *   - Adds duplicate-suppressed updateAttribute() so unchanged values do not create events or INFO logs.
 *   - Standardizes lifecycle initialization and device-name-prefixed logging.
 *   - Changes tile font family to Roboto/sans-serif and footer label to Last Checked.
 *   - Preserves AMA monitoring attributes, parent interface, and dashboard tile layout.
 * v1.1.0 - 2026/11/12 - Aligns tile typography, status indicators, and responsive layout with MEM/Echos Speak.
 * v1.0.9 - 2026/11/12 - Restores MEM tile sizing and typography after the v1.0.8 height regression.
 * v1.0.8 - 2026/11/12 - Compact table/CSS refactor; superseded because HD+ rendered a short, collapsed tile.
 * v1.0.7 - 2026/11/12 - Adds Amazon inventory freshness to the compact dashboard tile.
 *   - Green indicator means CURRENT; amber means STALE/NOT CHECKED; red means ERROR.
 *   - Retains the NAS and InfluxDB panels and EM-based responsive tile layout.
 * v1.0.6 - 2026/11/11 - Adds Amazon inventory and reconciliation diagnostic attributes.
 *   - Exposes endpoint totals, ownership counts, freshness, and key discrepancy counts.
 *   - Leaves the compact NAS/InfluxDB dashboard tile unchanged.
 * v1.0.5 - 2026/11/11 - Refactors tile container to fill its complete dashboard cell.
 *   - Applies the proven MEM/Echos Speak outer flex-column pattern: width/min-height 100%.
 *   - Separates header, vertically expanding service panel row, and pinned footer.
 *   - Vertically centers both symmetrical service columns within the available tile space.
 *   - Retains EM-based typography/spacing, independent service dots, and driverVersion.
 * v1.0.4 - 2026/11/11 - Adds matching status indicators above both service columns.
 *   - Centers a status dot above AMA NAS and InfluxDB for a symmetrical layout.
 *   - Each dot reflects its own service status: green online, red offline, amber unknown.
 *   - Retains centered last-checked text, EM sizing, and driverVersion attribute.
 * v1.0.3 - 2026/11/11 - Refines tile alignment and adds a visible driver version attribute.
 *   - Replaces the ALL ONLINE / CHECKING header text with a centered status indicator dot
 *     in the InfluxDB column (green when both services are online).
 *   - Uses a balanced two-column layout with consistent labels and status alignment.
 *   - Centers the last-checked line beneath both columns.
 *   - Reduces default base font scaling and tightens tile spacing for 2x1 dashboard fit.
 *   - Publishes driverVersion as a device attribute.
 * v1.0.2 - 2026/11/11 - Adds MEM-style responsive EM scaling and device tile preferences.
 *   - Adds Tile - Base Font size (.em), configurable from 0.1 to 5.0.
 *   - Adds optional last-checked display preference.
 *   - Reworks compact tile layout with relative EM typography and spacing only.
 *   - Re-renders the tile when driver preferences are saved.
 * v1.0.1 - 2026/11/11 - Adds compact 2-wide x 1-high NAS/InfluxDB dashboard tile.
 *   - Displays only the two service availability states and a last-checked time.
 *   - Uses UNKNOWN when a service cannot be checked; preserves diagnostic attributes.
 * v1.0.0 - 2026/11/11 - Initial AMA System Monitor virtual device driver.
 */

metadata {
    definition(name: 'AMA System Monitor', namespace: 'jshimota', author: 'James Shimota',
        description: 'Compact dashboard tile and diagnostic status device for AMA NAS and InfluxDB.') {
        capability 'Sensor'
        capability 'Configuration'
        capability 'Refresh'
        attribute 'driverVersion', 'string'
        attribute 'nasStatus', 'string'
        attribute 'nasVersion', 'string'
        attribute 'nasUptimeSeconds', 'number'
        attribute 'nasEnvironment', 'string'
        attribute 'influxStatus', 'string'
        attribute 'influxWriteStatus', 'string'
        attribute 'influxEnabled', 'string'
        attribute 'influxConfigured', 'string'
        attribute 'hubitatHeartbeatStatus', 'string'
        attribute 'telemetryStatus', 'string'
        attribute 'lastHubitatHeartbeat', 'string'
        attribute 'lastHubitatHeartbeatWrite', 'string'
        attribute 'lastHubitatEvent', 'string'
        attribute 'lastInfluxWrite', 'string'
        attribute 'lastInfluxHealthCheck', 'string'
        attribute 'hubitatEventCount', 'number'
        attribute 'hubitatHeartbeatCount', 'number'
        attribute 'influxWriteSuccessCount', 'number'
        attribute 'influxWriteFailureCount', 'number'
        attribute 'lastInfluxError', 'string'
        attribute 'lastStatusCheck', 'string'
        attribute 'monitorSummary', 'string'
        attribute 'amazonInventoryStatus', 'string'
        attribute 'amazonEndpointCount', 'number'
        attribute 'amazonAmaEndpointCount', 'number'
        attribute 'amazonLegacyHubitatEndpointCount', 'number'
        attribute 'amazonDisabledAmaCount', 'number'
        attribute 'amazonUnreachableAmaCount', 'number'
        attribute 'amazonUnknownReachabilityAmaCount', 'number'
        attribute 'amazonApprovedNotExposedCount', 'number'
        attribute 'amazonDuplicateAmaCount', 'number'
        attribute 'amazonMultiOwnerExposureCount', 'number'
        attribute 'amazonOrphanAmaCount', 'number'
        attribute 'amazonLastInventoryRefresh', 'string'
        attribute 'amazonReconciliationSummary', 'string'
        attribute 'monitorTile', 'string'
        attribute 'monitorCommandRequest', 'string'
        command 'healthCheck'
        command 'refreshTile'
    }
}

preferences {
    input name:'prefHeaderFontSize', type:'text',
        title:'Tile Header Font Scale (EM / Base 1.0 = 100%)',
        description:'Default: 1.35em. Enter a number from 0.1 to 3.0.',
        defaultValue:'1.35', required:true
    input name:'prefBodyFontSize', type:'text',
        title:'Tile Telemetry Font Scale (EM / Base 1.0 = 100%)',
        description:'Default: 0.95em. Enter a number from 0.1 to 3.0.',
        defaultValue:'0.95', required:true
    input name:'prefFooterFontSize', type:'text',
        title:'Tile Footer Font Scale (EM / Base 1.0 = 100%)',
        description:'Default: 0.70em. Enter a number from 0.1 to 3.0.',
        defaultValue:'0.70', required:true
    input name:'tileShowLastChecked', type:'bool',
        title:'Tile - Show Last Checked',
        description:'Display the last service-check time along the bottom of the tile.',
        defaultValue:true, required:true

    // Independent Logging Switches - standardized driver template
    input name:'logInfoEnable', type:'bool', title:'Logging - Enable Info Logging',
        description:'Enable to output normal activity to log<br>Default: <b>On</b>',
        defaultValue:true, required:true
    input name:'logErrorEnable', type:'bool', title:'Logging - Enable Error Logging',
        description:'Enable to output error activity to log<br>Default: <b>On</b>',
        defaultValue:true, required:true
    input name:'logWarnEnable', type:'bool', title:'Logging - Enable Warning Logging',
        description:'Enable to output warning activity to log<br>Default: <b>On</b>',
        defaultValue:true, required:true
    input name:'logDebugEnable', type:'bool', title:'Logging - Enable Debug Logging',
        description:'Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Automatically turns off after 30 minutes)',
        defaultValue:false, required:true
    input name:'logTraceEnable', type:'bool', title:'Logging - Enable Detailed Trace Logging',
        description:'Enable to output tracing activity to log<br>Default: <b>Off</b>',
        defaultValue:false, required:true
}

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    initialize(true)
    updateAttribute('driverVersion', version())
    updateAttribute('nasStatus', 'UNKNOWN')
    updateAttribute('influxStatus', 'UNKNOWN')
    renderMonitorTile()
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    initialize(false)
    updateAttribute('driverVersion', version())
    renderMonitorTile()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule('disableDebugLogging')
    if (isInstall) {
        device.updateSetting('logDebugEnable', [type:'bool', value:true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, 'disableDebugLogging')
    } else if (getSettingBool('logDebugEnable', false)) {
        logInfo "Debug logging enabled. Automatic turn-off scheduled."
        runIn(1800, 'disableDebugLogging', [overwrite:false])
    }
}

String version() { '1.2.2' }
String timeStamp() { '2026/11/12' }

// Single-shot version demarcation, consistent with the standard driver template.
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logInfo "=================== DRIVER VERSION: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

/**
 * Renders the intentionally minimal dashboard tile.
 * Set the Hubitat Dashboard tile size to 2 columns x 1 row and select
 * the custom attribute "monitorTile".
 */
/**
 * Hubitat Configuration capability. The Monitoring child validates its Parent path
 * and requests a fresh status snapshot; no service polling is scheduled here.
 **/
void configure() {
    requestMonitoringChild('CONFIGURE')
}

/** Hubitat Refresh capability: request an immediate status snapshot through Parent. */
void refresh() {
    requestMonitoringChild('REFRESH')
}

/** Actively test the end-to-end heartbeat/status path through Parent and NAS. */
void healthCheck() {
    requestMonitoringChild('HEALTH_CHECK')
}

/** Called by Monitoring & Analytics after it has published a complete status batch. */
void refreshTile() {
    renderMonitorTile()
}

private void requestMonitoringChild(String request) {
    logInfo "Requesting ${request.replace('_', ' ').toLowerCase()} through AMA Parent."
    sendEvent(name:'monitorCommandRequest', value:request, isStateChange:true,
        descriptionText:"${device.displayName} requested ${request} through AMA Parent")
}

private String sanitizeFontSize(Object inputVal, String defaultVal) {
    if (!inputVal) return defaultVal
    String trimmed = inputVal.toString().trim().toLowerCase()
    def match = (trimmed =~ /^([0-9]+(?:\.[0-9]+)?)(?:em)?$/)
    if (match.matches()) {
        try {
            BigDecimal num = match[0][1].toBigDecimal()
            if (num >= 0.1G && num <= 3.0G) {
                return "${num.stripTrailingZeros().toPlainString()}em"
            }
        } catch (Exception ignored) {}
    }
    return defaultVal
}

private void renderMonitorTile() {
    String nas = normalizeStatus(device.currentValue('nasStatus')?.toString())
    String influx = normalizeStatus(device.currentValue('influxStatus')?.toString())
    String nasColor = statusColor(nas)
    String influxColor = statusColor(influx)

    String amazonStatus = (device.currentValue('amazonInventoryStatus') ?: 'NOT_CHECKED').toString().trim().toUpperCase()
    String amazonLabel = amazonStatus == 'CURRENT' ? 'CURRENT' :
        (amazonStatus == 'STALE' ? 'STALE' :
        (amazonStatus in ['ERROR', 'FAILED', 'OFFLINE'] ? 'ERROR' : 'NOT CHECKED'))
    String amazonColor = amazonLabel == 'CURRENT' ? '#27AE60' :
        (amazonLabel == 'ERROR' ? '#E74C3C' : '#F1C40F')
    String nasDot = nas == 'ONLINE' ? '🟢' : (nas == 'OFFLINE' ? '🔴' : '🟡')
    String influxDot = influx == 'ONLINE' ? '🟢' : (influx == 'OFFLINE' ? '🔴' : '🟡')
    String amazonDot = amazonLabel == 'CURRENT' ? '🟢' : (amazonLabel == 'ERROR' ? '🔴' : '🟡')

    String hSize = sanitizeFontSize(settings?.prefHeaderFontSize, '1.35em')
    String bSize = sanitizeFontSize(settings?.prefBodyFontSize, '0.95em')
    String fSize = sanitizeFontSize(settings?.prefFooterFontSize, '0.70em')

    String checked = device.currentValue('lastStatusCheck')?.toString()
    if (!checked || checked == 'null') checked = 'Not checked'
    Boolean showLastChecked = settings.tileShowLastChecked != false

    // Match MEM/Echos Speak typography, dark flex card, and pinned footer.
    String css = ".a{background:#1e242a;color:#fff;padding:.6em .8em;font-family:Roboto,sans-serif;box-sizing:border-box;min-height:100%;display:flex;flex-direction:column;justify-content:space-between}.b{display:flex;flex-direction:column;gap:.4em}.h{font-size:${hSize};font-weight:700;color:#3498DB;text-align:center}.r{display:flex;gap:.4em}.p,.i{background:#171d22;border-radius:.3em;text-align:center;padding:.3em;font-size:${bSize};color:#a0aab2}.p{flex:1}.s{font-size:1.2em}.f{text-align:center;font-size:${fSize};color:#808c96}"
    String html = "<div class=a><style>${css}</style><div class=b><div class=h>AMA System Monitor</div>" +
        "<div class=r><div class=p>${nasDot}<br>AMA NAS<br><b class=s style='color:${nasColor}'>${nas}</b></div>" +
        "<div class=p>${influxDot}<br>InfluxDB<br><b class=s style='color:${influxColor}'>${influx}</b></div></div>" +
        "<div class=i>${amazonDot} Amazon Inventory <b style='color:${amazonColor}'>${amazonLabel}</b></div></div>" +
        (showLastChecked ? "<div class=f>Last Checked: <span style='color:#a0aab2'>${checked}</span></div>" : "") + "</div>"
    // If the platform attribute was cleared independently, discard the persistent
    // duplicate cache so the next render can restore the tile attribute.
    if (device.currentValue('monitorTile') == null) state.remove('last_monitorTile')
    sendIfChanged('monitorTile', html, null, 'debug', 'Dashboard tile', 'Dashboard tile HTML refreshed')
}

private String normalizeStatus(String value) {
    String status = (value ?: 'UNKNOWN').toString().trim().toUpperCase()
    if (status in ['OK', 'HEALTHY', 'ONLINE']) return 'ONLINE'
    if (status in ['OFFLINE', 'UNHEALTHY', 'ERROR', 'FAILED']) return 'OFFLINE'
    return 'UNKNOWN'
}

private String statusColor(String status) {
    if (status == 'ONLINE') return '#20C878'
    if (status == 'OFFLINE') return '#FF625C'
    return '#E5B84B'
}


/* =========================================================================================
   MASTER UTILITY ROUTINES & STANDARDIZED LOGGING ENGINE
   ========================================================================================= */

// Automatically disable debug logging 30 minutes after initialization/preferences enable it.
void disableDebugLogging() {
    if (getSettingBool('logDebugEnable', false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting('logDebugEnable', [type:'bool', value:false])
    }
}

// Duplicate-suppressed attribute update. Unchanged values create neither events nor INFO logs.
private void updateAttribute(final String attribute, final Object value,
                             final String unit = null, final String type = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
    logInfo descriptionText
    sendEvent(name:attribute, value:value, unit:unit, type:type, descriptionText:descriptionText)
}

/**
 * Standardized duplicate-suppressed event publisher.
 * Checks both the live device attribute and a persistent last-sent value before
 * publishing. Supports caller-selected logging level and custom event/log text.
 **/
private void sendIfChanged(final String attribute, final Object value, final String unit = null, final String logLevel = "info", final String customLabel = null, final String customLogMessage = null) {
    final String valStr = value?.toString()
    final String currentVal = device.currentValue(attribute)?.toString()

    if (currentVal == valStr) {
        logDebug "sendIfChanged(): Suppressed duplicate ${attribute} event (${valStr})"
        return
    }

    final String lastVal = state["last_${attribute}"]?.toString()
    if (lastVal == valStr) {
        logDebug "sendIfChanged(): Suppressed identical state ${attribute} (${valStr})"
        return
    }

    state["last_${attribute}"] = valStr

    final String label = customLabel ?: attribute
    final String logText = customLogMessage ?: "${label} now ${value}${unit ? ' ' + unit : ''}"
    final String descriptionText = "${device.displayName} ${logText}"

    logMessage(logLevel, logText)
    sendEvent(name: attribute, value: value, unit: unit, isStateChange: true, descriptionText: descriptionText)
}

// Centralized logging engine with independent GUI controls.
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: 'info'
    String devName = device.displayName ?: 'AMA System Monitor'
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ['info', 'warn', 'error'])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${devName}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage('info', msg) }
private void logDebug(String msg) { logMessage('debug', msg) }
private void logTrace(String msg) { logMessage('trace', msg) }
private void logWarn(String msg)  { logMessage('warn', msg) }
private void logError(String msg) { logMessage('error', msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}
