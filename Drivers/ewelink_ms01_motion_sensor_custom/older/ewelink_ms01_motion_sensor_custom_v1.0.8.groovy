/**
 *  eWeLink MS01 Motion Sensor (Custom)
 *
 *  Device Driver for Hubitat Elevation
 *
 *  Copyright 2026 James Shimota
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  ------------------------------------------------------------------------------------------------------
 *  Changelog:
 *  v1.0.8 (2026-08-28) - Added comprehensive debug logging throughout parsing and battery calculation logic.
 *  v1.0.7 (2026-08-28) - Enhanced cluster 0x0001 parsing to catch broader read attr formats and auto-calculate
 *                        battery percentage from voltage if standard battery percentage reporting fails.
 *  v1.0.6 (2026-08-27) - Added battery voltage monitoring (0x0020) with low voltage warning logs at <= 2.9V.
 *  v1.0.5 (2026-08-27) - Set software motion reset timer default to 0 (disabled).
 *  v1.0.4 (2026-08-27) - Removed invalid IasZoneAttribute import class.
 *  v1.0.3 (2026-08-27) - Set default check-in interval to 12 hours.
 *  v1.0.2 (2026-08-27) - Updated check-in intervals to 1, 3, 6, 12, and 24 hours using Hubitat cron/native scheduling.
 *  v1.0.1 (2026-08-27) - Fixed Hubitat 2.4 scheduler methods and enum type casting.
 *  v1.0.0 (2026-08-27) - Initial release for eWeLink MS01 & Sonoff SNZB-03 motion sensors.
 *  ------------------------------------------------------------------------------------------------------
 */

import hubitat.zigbee.zcl.DataType

metadata {
    definition (
        name: "eWeLink MS01 Motion Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: ""
    ) {
        capability "MotionSensor"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"

        // Attributes
        attribute "driverVersion", "string"
        attribute "batteryVoltage", "number"

        // Fingerprints for eWeLink MS01 & Sonoff variants
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "eWeLink", model: "MS01", deviceJoinName: "eWeLink MS01 Motion Sensor (Custom)"
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "eWeLink", model: "MSO1", deviceJoinName: "eWeLink MS01 Motion Sensor (Custom)"
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "Sonoff", model: "SNZB-03", deviceJoinName: "Sonoff SNZB-03 Motion Sensor"
    }

    preferences {
        input name: "motionResetTimer", type: "number", title: "Software Motion Reset (seconds)", description: "Auto-reset motion to inactive if sensor misses clear (0 to disable)", defaultValue: 0, required: true
        input name: "checkInInterval", type: "enum", title: "Activity Check-In Interval", options: ["1":"1 Hour", "3":"3 Hours", "6":"6 Hours", "12":"12 Hours", "24":"24 Hours"], defaultValue: "12", required: true
        input name: "logEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable Description Text", defaultValue: true
    }
}

private String version() { return "1.0.8" }

// Parse incoming Zigbee messages
def parse(String description) {
    if (logEnable) log.debug "parse(): Raw description -> ${description}"
    
    Map map = [:]
    
    if (description?.startsWith("zone status")) {
        map = parseIasZoneStatus(description)
    } else if (description?.startsWith("read attr") || description?.contains("cluster: 0001")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        if (logEnable) log.debug "parse(): Parsed description map -> ${descMap}"
        
        if (descMap.cluster == "0001") {
            if (descMap.attrId == "0021" && descMap.value) {
                if (logEnable) log.debug "parse(): Processing battery percentage attribute (0x0021)"
                map = parseBattery(descMap.value)
            } else if (descMap.attrId == "0020" && descMap.value) {
                if (logEnable) log.debug "parse(): Processing battery voltage attribute (0x0020)"
                map = parseBatteryVoltage(descMap.value)
            } else {
                if (logEnable) log.debug "parse(): Cluster 0001 attribute ${descMap.attrId} ignored"
            }
        }
    } else if (description?.startsWith("enroll request")) {
        if (logEnable) log.debug "parse(): Handling IAS enrollment request"
        List cmds = zigbee.enrollResponse()
        if (logEnable) log.debug "parse(): Sending enroll response -> ${cmds}"
        return cmds
    } else {
        if (logEnable) log.debug "parse(): Unhandled description pattern"
    }

    if (map) {
        if (txtEnable && map.descriptionText) log.info "${device.displayName} ${map.descriptionText}"
        if (logEnable) log.debug "parse(): Sending event -> ${map}"
        sendEvent(map)
    }
    
    return []
}

// Handle IAS Zone Status (Motion events)
private Map parseIasZoneStatus(String description) {
    if (logEnable) log.debug "parseIasZoneStatus(): Processing IAS status string -> ${description}"
    Integer status = zigbee.parseZoneStatus(description)?.alarm1 ? 1 : 0
    Integer resetTimer = (motionResetTimer != null) ? (motionResetTimer as Integer) : 0
    
    if (status == 1) {
        if (resetTimer > 0) {
            if (logEnable) log.debug "parseIasZoneStatus(): Motion active. Scheduling software reset in ${resetTimer} seconds"
            runIn(resetTimer, "resetMotionToInactive")
        }
        return [name: "motion", value: "active", descriptionText: "motion is active"]
    } else {
        if (logEnable) log.debug "parseIasZoneStatus(): Motion inactive. Clearing software reset timers"
        unschedule("resetMotionToInactive")
        return [name: "motion", value: "inactive", descriptionText: "motion is inactive"]
    }
}

// Parse Battery Percentage (0x0001 / 0x0021)
private Map parseBattery(String hexValue) {
    if (logEnable) log.debug "parseBattery(): Raw hex -> ${hexValue}"
    Integer rawValue = Integer.parseInt(hexValue, 16)
    Integer pct = Math.round(rawValue / 2)
    pct = Math.min(100, Math.max(0, pct))
    if (logEnable) log.debug "parseBattery(): Computed battery percentage -> ${pct}%"
    return [name: "battery", value: pct, unit: "%", descriptionText: "battery is ${pct}%", isStateChange: true]
}

// Parse Battery Raw Voltage (0x0001 / 0x0020) and calculate percentage fallback
private Map parseBatteryVoltage(String hexValue) {
    if (logEnable) log.debug "parseBatteryVoltage(): Raw hex -> ${hexValue}"
    Integer rawValue = Integer.parseInt(hexValue, 16)
    BigDecimal voltage = (rawValue / 10.0).setScale(1, BigDecimal.ROUND_HALF_UP)
    
    // Calculate battery percentage profile (3.0V = 100%, 2.5V = 0%)
    BigDecimal maxVolts = 3.0
    BigDecimal minVolts = 2.5
    Integer pct = (int) (((voltage - minVolts) / (maxVolts - minVolts)) * 100)
    pct = Math.min(100, Math.max(0, pct))
    
    if (logEnable) log.debug "parseBatteryVoltage(): Measured ${voltage}V -> Calculated battery fallback percentage -> ${pct}%"
    
    // Explicitly send standard battery percentage event for capability "Battery"
    sendEvent(name: "battery", value: pct, unit: "%", descriptionText: "battery is ${pct}% (${voltage}V)", isStateChange: true)
    
    if (voltage <= 2.9) {
        log.warn "${device.displayName}: Low battery voltage detected! Current voltage: ${voltage}V (Threshold: 2.9V)"
    }
    
    return [name: "batteryVoltage", value: voltage, unit: "V", descriptionText: "battery voltage is ${voltage}V", isStateChange: true]
}

// Software motion reset timer callback
def resetMotionToInactive() {
    if (txtEnable) log.info "${device.displayName} motion reset to inactive (timeout)"
    if (logEnable) log.debug "resetMotionToInactive(): Timeout elapsed, emitting inactive event"
    sendEvent(name: "motion", value: "inactive", descriptionText: "motion set to inactive via timeout")
}

// Scheduled check-in to keep device active on inactivity reports
def checkIn() {
    if (logEnable) log.debug "checkIn(): Executing scheduled check-in event"
    sendEvent(name: "checkIn", value: now(), displayed: false, isStateChange: true)
}

def setupSchedule() {
    unschedule("checkIn")
    String interval = checkInInterval ?: "12"
    if (logEnable) log.debug "setupSchedule(): Setting activity check-in interval to ${interval} hour(s)"
    switch(interval) {
        case "1":
            runEvery1Hour("checkIn")
            break
        case "3":
            runEvery3Hours("checkIn")
            break
        case "6":
            schedule("0 0 */6 ? * *", "checkIn")
            break
        case "12":
            schedule("0 0 */12 ? * *", "checkIn")
            break
        case "24":
            schedule("0 0 0 ? * *", "checkIn")
            break
        default:
            schedule("0 0 */12 ? * *", "checkIn")
            break
    }
}

def configure() {
    log.info "Configuring eWeLink MS01 Motion Sensor (Custom) (v${version()})..."
    
    sendEvent(name: "driverVersion", value: version())
    setupSchedule()
    
    // Binding and reporting configurations
    List<String> cmds = []
    cmds += zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 30, 21600, 0x01) // Raw Voltage
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 30, 21600, 0x01) // Battery Percentage
    cmds += zigbee.readAttribute(0x0001, 0x0020)
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    cmds += zigbee.enrollResponse()
    
    if (logEnable) log.debug "configure(): Generated Zigbee command payload -> ${cmds}"
    return cmds
}

def refresh() {
    if (logEnable) log.debug "refresh(): Requesting battery attributes (0x0020, 0x0021)"
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0001, 0x0020)
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    if (logEnable) log.debug "refresh(): Generated Zigbee read commands -> ${cmds}"
    return cmds
}

def updated() {
    log.info "eWeLink MS01 (Custom): Preferences updated"
    if (logEnable) log.debug "updated(): Re-applying configuration and scheduler"
    sendEvent(name: "driverVersion", value: version())
    setupSchedule()
    if (logEnable) runIn(1800, "logsOff")
}

def logsOff() {
    device.updateSetting("logEnable", [value: false, type: "bool"])
    log.info "eWeLink MS01 (Custom): Debug logging disabled"
}