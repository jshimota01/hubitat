/**
 * AMA System Monitor Device Driver
 * Version: 1.0.0 | 2026/11/11
 *
 * Dashboard-friendly status attributes maintained by the AMA Monitoring & Analytics child app.
 * This driver does not communicate with the NAS or InfluxDB directly; all monitoring data
 * is retrieved by the Hubitat Parent and delivered to the Monitoring & Analytics child.
 *
 * Changelog:
 * v1.0.0 - 2026/11/11 - Initial AMA System Monitor virtual device driver.
 */

metadata {
    definition(name: 'AMA System Monitor', namespace: 'jshimota', author: 'James Shimota',
        description: 'Dashboard status device for AMA NAS, InfluxDB, Hubitat heartbeat, and telemetry health.') {
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
    }
}

void installed() {
    log.info "AMA System Monitor driver v${version()} installed."
}

void updated() {
    log.info "AMA System Monitor driver v${version()} updated."
}

String version() { '1.0.0' }
