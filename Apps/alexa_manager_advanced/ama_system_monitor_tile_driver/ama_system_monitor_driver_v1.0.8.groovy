/**
 * v1.0.8 - 2026/11/12 - Reduces tile HTML while preserving status layout and indicators.
 *   - Uses the established Arial,sans-serif font family from the original AMA monitor tile.
 *   - Sets base font size and text alignment once on the outer container.
 *   - Replaces nested flex alignment with a compact two-column table.
 *   - Reuses CSS classes for repeated panel, dot, and label styling.
 *   - Preserves NAS, InfluxDB, Amazon inventory, and optional last-checked status.
 * v1.0.7 - 2026/11/12 - Adds Amazon inventory freshness to the compact dashboard tile.
 *   - Displays an independent Amazon Inventory status line with a status indicator.
 *   - Green means CURRENT; amber means STALE or NOT CHECKED; red means ERROR.
 *   - Retains the existing NAS and InfluxDB panels, last-checked footer, and EM layout.
 * AMA System Monitor Device Driver
 * Version: 1.0.8 | 2026/11/12
 *
 * Responsive Hubitat dashboard tile plus diagnostic status attributes maintained by the
 * AMA Monitoring & Analytics child app. The driver does not contact the NAS or InfluxDB.
 * Uses compact table layout, Arial typography, and EM sizing consistent with the original AMA tile.
 *
 * Changelog:
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
    input name:'tileBaseFontSize', type:'decimal',
        title:'Tile - Base Font size (.em)',
        description:'Scales all tile text and spacing. Recommended starting value: 1.0. Range: 0.1 to 5.0.',
        defaultValue:0.75, range:'0.1..5.0', required:true
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

String version() { '1.0.8' }

/**
 * Renders the intentionally minimal dashboard tile.
 * Set the Hubitat Dashboard tile size to 2 columns x 1 row and select
 * the custom attribute "monitorTile".
 */
void updateMonitorTile(String nasValue, String influxValue) {
    String nas = normalizeStatus(nasValue)
    String influx = normalizeStatus(influxValue)
    String nasColor = statusColor(nas)
    String influxColor = statusColor(influx)

    String amazonStatus = (device.currentValue('amazonInventoryStatus') ?: 'NOT_CHECKED').toString().trim().toUpperCase()
    String amazonLabel = amazonStatus in ['CURRENT'] ? 'CURRENT' :
        (amazonStatus in ['STALE'] ? 'STALE' :
        (amazonStatus in ['ERROR', 'FAILED', 'OFFLINE'] ? 'ERROR' : 'NOT CHECKED'))
    String amazonColor = amazonLabel == 'CURRENT' ? '#20C878' :
        (amazonLabel == 'ERROR' ? '#FF625C' : '#E5B84B')

    BigDecimal baseSize = 0.75G
    try {
        if (settings.tileBaseFontSize != null) baseSize = settings.tileBaseFontSize as BigDecimal
    } catch (ignored) {}
    if (baseSize < 0.1G) baseSize = 0.1G
    if (baseSize > 5.0G) baseSize = 5.0G
    String baseEm = baseSize.stripTrailingZeros().toPlainString()

    String checked = device.currentValue('lastStatusCheck')?.toString()
    if (!checked || checked == 'null') checked = 'Waiting for first check'
    String checkedHtml = settings.tileShowLastChecked == false ? '' :
        "<tr><td colspan=2 style='font-size:.56em;color:#89949D;border-top:.05em solid #ffffff0d;padding-top:.15em'>Checked: ${checked}</td></tr>"

    // The outer container supplies base typography and centering. A compact table
    // keeps the two service panels balanced without nested flex/centering wrappers.
    String html = "<div style='background:#20272D;color:#F1F4F6;border-radius:.35em;padding:.45em .55em;font:${baseEm}em Arial,sans-serif;text-align:center'><style>.p{background:#171D22;border-radius:.3em;padding:.2em}.d{font-size:.7em}.l{font-size:.68em;color:#AEB8C0}.s{font-size:1.08em}</style>" +
        "<b style='color:#36A9F4'>AMA System Monitor</b><table style='width:100%;border-spacing:.3em .1em'><tr>" +
        "<td class=p><span class=d style='color:${nasColor}'>●</span><br><span class=l>AMA NAS</span><br><b class=s style='color:${nasColor}'>${nas}</b></td>" +
        "<td class=p><span class=d style='color:${influxColor}'>●</span><br><span class=l>InfluxDB</span><br><b class=s style='color:${influxColor}'>${influx}</b></td></tr>" +
        "<tr><td class=p colspan=2 style='font-size:.78em;color:#AEB8C0;padding:.16em'><span style='color:${amazonColor}'>●</span> Amazon Inventory <b style='color:${amazonColor}'>${amazonLabel}</b></td></tr>" +
        checkedHtml + "</table></div>"
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
