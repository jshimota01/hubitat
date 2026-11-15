/**
 * v1.0.9 - 2026/11/12 - Restores established MEM/OWM tile sizing and typography while retaining compact CSS.
 *   - Uses the MEM tile's system-ui, -apple-system, sans-serif font stack and .75em base scale.
 *   - Uses the established one-letter CSS class pattern (.c, .p, .d, .s, .f).
 *   - Makes the root container and table fill the complete dashboard tile height without flex layout.
 *   - Restores the original service-panel typography and gives the two panels the available vertical space.
 *   - Retains Amazon Inventory freshness, status colors, and optional last-checked footer.
 * v1.0.8 - 2026/11/12 - Compact table/CSS refactor; replaced in v1.0.9 after HD+ exposed a height regression.
 * v1.0.7 - 2026/11/12 - Adds Amazon inventory freshness to the compact dashboard tile.
 *   - Displays an independent Amazon Inventory status line with a status indicator.
 *   - Green means CURRENT; amber means STALE or NOT CHECKED; red means ERROR.
 *   - Retains the existing NAS and InfluxDB panels, last-checked footer, and EM layout.
 * AMA System Monitor Device Driver
 * Version: 1.0.9 | 2026/11/12
 *
 * Responsive Hubitat dashboard tile plus diagnostic status attributes maintained by the
 * AMA Monitoring & Analytics child app. The driver does not contact the NAS or InfluxDB.
 * Follows the established MEM tile typography and OWM one-letter CSS class pattern, with a full-cell table layout.
 *
 * Changelog:
 * v1.0.9 - 2026/11/12 - Restores MEM/OWM tile conventions and full-cell sizing.
 *   - Matches MEM's font stack and base scale; uses OWM's compact one-letter CSS class pattern.
 *   - Gives the root container and table 100% height so the service panels fill a 2x1 dashboard cell.
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

String version() { '1.0.9' }

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
    String amazonLabel = amazonStatus == 'CURRENT' ? 'CURRENT' :
        (amazonStatus == 'STALE' ? 'STALE' :
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
    if (!checked || checked == 'null') checked = 'Not checked'
    String checkedHtml = settings.tileShowLastChecked == false ? '' :
        "<tr><td class=f colspan=2>Chk: ${checked}</td></tr>"

    // MEM tile root typography; OWM one-letter CSS classes. The table fills the full tile height.
    String html = "<div style='font-size:${baseEm}em;font-family:system-ui,-apple-system,sans-serif;background:#1A252F;color:#fff;padding:.5em .6em;border-radius:.5em;box-sizing:border-box;height:100%'><style>" +
        ".c{width:100%;height:100%;text-align:center;border-spacing:0 .2em}.p{background:#171d22;border-radius:.3em}.d{font-size:.82em;color:#aeb8c0}.s{font-size:1.2em;font-weight:700}.f{font-size:.56em;color:#89949d}" +
        "</style><table class=c><tr><td colspan=2><b style='color:#3498DB'>AMA System Monitor</b></td></tr>" +
        "<tr valign=middle><td class=p><span style='color:${nasColor}'>●</span><br><span class=d>AMA NAS</span><br><b class=s style='color:${nasColor}'>${nas}</b></td>" +
        "<td class=p><span style='color:${influxColor}'>●</span><br><span class=d>InfluxDB</span><br><b class=s style='color:${influxColor}'>${influx}</b></td></tr>" +
        "<tr><td class='p d' colspan=2><span style='color:${amazonColor}'>●</span> Amazon Inventory <b style='color:${amazonColor}'>${amazonLabel}</b></td></tr>" +
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
