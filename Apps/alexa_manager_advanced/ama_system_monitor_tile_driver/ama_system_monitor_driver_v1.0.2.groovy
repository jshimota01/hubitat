/**
 * AMA System Monitor Device Driver
 * Version: 1.0.2 | 2026/11/11
 *
 * Responsive Hubitat dashboard tile plus diagnostic status attributes maintained by the
 * AMA Monitoring & Analytics child app. The driver does not contact the NAS or InfluxDB.
 * All tile typography and spacing use EM units; no fixed pixel or percentage sizing.
 *
 * Changelog:
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
        attribute 'monitorTile', 'string'
        command 'updateMonitorTile', [[name:'NAS Status', type:'STRING'], [name:'InfluxDB Status', type:'STRING']]
    }
}

preferences {
    input name:'tileBaseFontSize', type:'decimal',
        title:'Tile - Base Font size (.em)',
        description:'Scales all tile text and spacing. Recommended starting value: 1.0. Range: 0.1 to 5.0.',
        defaultValue:1.0, range:'0.1..5.0', required:true
    input name:'tileShowLastChecked', type:'bool',
        title:'Tile - Show Last Checked',
        description:'Display the last service-check time along the bottom of the tile.',
        defaultValue:true, required:true
}

void installed() {
    sendEvent(name:'nasStatus', value:'UNKNOWN')
    sendEvent(name:'influxStatus', value:'UNKNOWN')
    updateMonitorTile('UNKNOWN', 'UNKNOWN')
    log.info "AMA System Monitor driver v${version()} installed."
}

void updated() {
    updateMonitorTile(
        (device.currentValue('nasStatus') ?: 'UNKNOWN').toString(),
        (device.currentValue('influxStatus') ?: 'UNKNOWN').toString()
    )
    log.info "AMA System Monitor driver v${version()} updated."
}

String version() { '1.0.2' }

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
    String overall = (nas == 'ONLINE' && influx == 'ONLINE') ? 'ALL ONLINE' :
        ((nas == 'OFFLINE' || influx == 'OFFLINE') ? 'ATTENTION' : 'CHECKING')
    String overallColor = overall == 'ALL ONLINE' ? '#20C878' : (overall == 'ATTENTION' ? '#FF625C' : '#E5B84B')

    BigDecimal baseSize = 1.0G
    try {
        if (settings.tileBaseFontSize != null) baseSize = settings.tileBaseFontSize as BigDecimal
    } catch (ignored) {}
    if (baseSize < 0.1G) baseSize = 0.1G
    if (baseSize > 5.0G) baseSize = 5.0G
    String baseEm = baseSize.stripTrailingZeros().toPlainString()

    String checked = device.currentValue('lastStatusCheck')?.toString()
    if (!checked || checked == 'null') checked = 'Waiting for first check'
    String checkedHtml = settings.tileShowLastChecked == false ? '' :
        "<div style='color:#89949D;text-align:right;font-size:.58em;margin-top:.18em;'>Checked: ${checked}</div>"

    // Compact, responsive layout: every font size, gap, padding, and radius is EM-based.
    // Keep the rendered attribute concise for Hubitat Dashboard attribute limits.
    String html = "<div style='background:#20272D;color:#F1F4F6;border-radius:.4em;padding:.38em .52em;font-family:Arial,sans-serif;font-size:${baseEm}em;line-height:1.12;'>" +
        "<div style='display:flex;justify-content:space-between;align-items:center;font-size:.86em;font-weight:bold;margin-bottom:.28em;'>" +
        "<span style='color:#36A9F4;'>AMA System Monitor</span><span style='color:${overallColor};'>${overall}</span></div>" +
        "<div style='display:flex;gap:.38em;text-align:center;'>" +
        "<div style='flex:1;background:#171D22;border-radius:.28em;padding:.22em .12em;'><div style='color:#AEB8C0;font-size:.68em;'>AMA NAS</div><div style='color:${nasColor};font-size:1.08em;font-weight:bold;'>${nas}</div></div>" +
        "<div style='flex:1;background:#171D22;border-radius:.28em;padding:.22em .12em;'><div style='color:#AEB8C0;font-size:.68em;'>InfluxDB</div><div style='color:${influxColor};font-size:1.08em;font-weight:bold;'>${influx}</div></div></div>" +
        checkedHtml + "</div>"
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
