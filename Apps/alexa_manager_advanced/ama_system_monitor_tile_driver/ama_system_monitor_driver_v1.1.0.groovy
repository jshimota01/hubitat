/**
 * v1.1.0 - 2026/11/12 - Aligns AMA tile with the established MEM and Echos Speak dashboard tile design.
 *   - Copies the Echos Speak green/amber/red status emoji indicators instead of Unicode text circles.
 *   - Adopts MEM/Echos Speak typography defaults: Header 1.35em, Body 0.95em, Footer 0.70em.
 *   - Adds independent GUI font-scale preferences for header, body, and footer, using the Echos Speak EM sanitizer.
 *   - Matches MEM/Echos Speak outer background, font stack, flex-column layout, and pinned footer.
 *   - Retains the two service panels, Amazon Inventory freshness, status colors, and optional last-checked footer.
 * v1.0.8 - 2026/11/12 - Compact table/CSS refactor; replaced in v1.0.9 after HD+ exposed a height regression.
 * v1.0.7 - 2026/11/12 - Adds Amazon inventory freshness to the compact dashboard tile.
 *   - Displays an independent Amazon Inventory status line with a status indicator.
 *   - Green means CURRENT; amber means STALE or NOT CHECKED; red means ERROR.
 *   - Retains the existing NAS and InfluxDB panels, last-checked footer, and EM layout.
 * AMA System Monitor Device Driver
 * Version: 1.1.0 | 2026/11/12
 *
 * Responsive Hubitat dashboard tile plus diagnostic status attributes maintained by the
 * AMA Monitoring & Analytics child app. The driver does not contact the NAS or InfluxDB.
 * Matches the established MEM/Echos Speak dashboard tile typography and responsive flex layout.
 *
 * Changelog:
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
        command 'updateMonitorTile', [[name:'NAS Status', type:'STRING'], [name:'InfluxDB Status', type:'STRING']]
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
}

void installed() {
    sendEvent(name:'driverVersion', value:version(), isStateChange:true)
    sendEvent(name:'nasStatus', value:'UNKNOWN')
    sendEvent(name:'influxStatus', value:'UNKNOWN')
    updateMonitorTile('UNKNOWN', 'UNKNOWN')
    log.info "AMA System Monitor driver v${version()} installed."
}

void updated() {
    sendEvent(name:'driverVersion', value:version(), isStateChange:true)
    updateMonitorTile(
        (device.currentValue('nasStatus') ?: 'UNKNOWN').toString(),
        (device.currentValue('influxStatus') ?: 'UNKNOWN').toString()
    )
    log.info "AMA System Monitor driver v${version()} updated."
}

String version() { '1.1.0' }

/**
 * Renders the intentionally minimal dashboard tile.
 * Set the Hubitat Dashboard tile size to 2 columns x 1 row and select
 * the custom attribute "monitorTile".
 */
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

void updateMonitorTile(String nasValue, String influxValue) {
    String nas = normalizeStatus(nasValue)
    String influx = normalizeStatus(influxValue)
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
    String checkedHtml = settings.tileShowLastChecked == false ? '' :
        "<span>Chk: ${checked}</span>"

    // Match MEM/Echos Speak typography, dark flex card, and pinned footer.
    String css = ".a{background:#1e242a;color:#fff;padding:.6em .8em;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;box-sizing:border-box;min-height:100%;display:flex;flex-direction:column;justify-content:space-between}.b{display:flex;flex-direction:column;gap:.4em}.h{font-size:${hSize};font-weight:700;color:#3498DB;text-align:center}.r{display:flex;gap:.4em}.p,.i{background:#171d22;border-radius:.3em;text-align:center;padding:.3em;font-size:${bSize};color:#a0aab2}.p{flex:1}.s{font-size:1.2em}.f{text-align:center;font-size:${fSize};color:#808c96}"
    String html = "<div class=a><style>${css}</style><div class=b><div class=h>AMA System Monitor</div>" +
        "<div class=r><div class=p>${nasDot}<br>AMA NAS<br><b class=s style='color:${nasColor}'>${nas}</b></div>" +
        "<div class=p>${influxDot}<br>InfluxDB<br><b class=s style='color:${influxColor}'>${influx}</b></div></div>" +
        "<div class=i>${amazonDot} Amazon Inventory <b style='color:${amazonColor}'>${amazonLabel}</b></div></div>" +
        (checkedHtml ? "<div class=f>Chk: <span style='color:#a0aab2'>${checked}</span></div>" : "") + "</div>"
    sendEvent(name:'monitorTile', value:html)
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
