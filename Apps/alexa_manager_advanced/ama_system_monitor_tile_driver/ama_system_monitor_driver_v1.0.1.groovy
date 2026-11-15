/**
 * AMA System Monitor Device Driver
 * Version: 1.0.1 | 2026/11/11
 *
 * Compact Hubitat dashboard tile plus diagnostic status attributes maintained by the
 * AMA Monitoring & Analytics child app. The driver does not contact the NAS or InfluxDB.
 * The child receives status through the Parent and updates this device.
 *
 * Changelog:
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

void installed() {
    sendEvent(name:'nasStatus', value:'UNKNOWN')
    sendEvent(name:'influxStatus', value:'UNKNOWN')
    updateMonitorTile('UNKNOWN', 'UNKNOWN')
    log.info "AMA System Monitor driver v${version()} installed."
}

void updated() {
    log.info "AMA System Monitor driver v${version()} updated."
}

String version() { '1.0.1' }

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
    String checked = device.currentValue('lastStatusCheck')?.toString()
    if (!checked || checked == 'null') checked = 'Waiting for first check'
    String html = "<div style='background:#20272D;color:#F1F4F6;border-radius:.4em;padding:.45em .6em;font-family:Arial,sans-serif;'>" +
        "<div style='display:flex;justify-content:space-between;font-size:.85em;font-weight:bold;margin-bottom:.25em;'><span style='color:#36A9F4;'>AMA System Monitor</span><span style='color:${overallColor};'>${overall}</span></div>" +
        "<div style='display:grid;grid-template-columns:1fr 1fr;gap:.4em;text-align:center;'>" +
        "<div style='background:#171D22;border-radius:.3em;padding:.25em;'><div style='color:#AEB8C0;font-size:.7em;'>AMA NAS</div><div style='color:${nasColor};font-size:1em;font-weight:bold;'>${nas}</div></div>" +
        "<div style='background:#171D22;border-radius:.3em;padding:.25em;'><div style='color:#AEB8C0;font-size:.7em;'>InfluxDB</div><div style='color:${influxColor};font-size:1em;font-weight:bold;'>${influx}</div></div></div>" +
        "<div style='color:#89949D;text-align:right;font-size:.6em;margin-top:.2em;'>Checked: ${checked}</div></div>"
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
