/**
 *  eWeLink MS01 Motion Sensor (Custom)
 *
 *  Device Driver for Hubitat Elevation
 *
 *  Copyright 2026 James Shimota[cite: 1]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:[cite: 1]
 *
 *      http://www.apache.org/licenses/LICENSE-2.0[cite: 1]
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.[cite: 1]
 *
 *  ------------------------------------------------------------------------------------------------------
 *  Changelog:
 *  v1.0.7 (2026-08-28) - Enhanced cluster 0x0001 parsing to catch broader read attr formats and auto-calculate
 *                        battery percentage from voltage if standard battery percentage reporting fails.
 *  v1.0.6 (2026-08-27) - Added battery voltage monitoring (0x0020) with low voltage warning logs at <= 2.9V.[cite: 1]
 *  v1.0.5 (2026-08-27) - Set software motion reset timer default to 0 (disabled).[cite: 1]
 *  v1.0.4 (2026-08-27) - Removed invalid IasZoneAttribute import class.[cite: 1]
 *  v1.0.3 (2026-08-27) - Set default check-in interval to 12 hours.[cite: 1]
 *  v1.0.2 (2026-08-27) - Updated check-in intervals to 1, 3, 6, 12, and 24 hours using Hubitat cron/native scheduling.[cite: 1]
 *  v1.0.1 (2026-08-27) - Fixed Hubitat 2.4 scheduler methods and enum type casting.[cite: 1]
 *  v1.0.0 (2026-08-27) - Initial release for eWeLink MS01 & Sonoff SNZB-03 motion sensors.[cite: 1]
 *  ------------------------------------------------------------------------------------------------------
 */

import hubitat.zigbee.zcl.DataType[cite: 1]

metadata {
    definition (
        name: "eWeLink MS01 Motion Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",[cite: 1]
        importUrl: ""[cite: 1]
    ) {
        capability "MotionSensor"[cite: 1]
        capability "Battery"[cite: 1]
        capability "Configuration"[cite: 1]
        capability "Refresh"[cite: 1]
        capability "Sensor"[cite: 1]

        // Attributes
        attribute "driverVersion", "string"[cite: 1]
        attribute "batteryVoltage", "number"[cite: 1]

        // Fingerprints for eWeLink MS01 & Sonoff variants
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "eWeLink", model: "MS01", deviceJoinName: "eWeLink MS01 Motion Sensor (Custom)"[cite: 1]
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "eWeLink", model: "MSO1", deviceJoinName: "eWeLink MS01 Motion Sensor (Custom)"[cite: 1]
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "Sonoff", model: "SNZB-03", deviceJoinName: "Sonoff SNZB-03 Motion Sensor"[cite: 1]
    }

    preferences {
        input name: "motionResetTimer", type: "number", title: "Software Motion Reset (seconds)", description: "Auto-reset motion to inactive if sensor misses clear (0 to disable)", defaultValue: 0, required: true[cite: 1]
        input name: "checkInInterval", type: "enum", title: "Activity Check-In Interval", options: ["1":"1 Hour", "3":"3 Hours", "6":"6 Hours", "12":"12 Hours", "24":"24 Hours"], defaultValue: "12", required: true[cite: 1]
        input name: "logEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false[cite: 1]
        input name: "txtEnable", type: "bool", title: "Enable Description Text", defaultValue: true[cite: 1]
    }
}

private String version() { return "1.0.7" }

// Parse incoming Zigbee messages
def parse(String description) {
    if (logEnable) log.debug "eWeLink MS01 (Custom): Parsing description: ${description}"[cite: 1]
    
    Map map = [:][cite: 1]
    
    if (description?.startsWith("zone status")) {[cite: 1]
        map = parseIasZoneStatus(description)[cite: 1]
    } else if (description?.startsWith("read attr") || description?.contains("cluster: 0001")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)[cite: 1]
        if (descMap.cluster == "0001") {[cite: 1]
            if (descMap.attrId == "0021" && descMap.value) {[cite: 1]
                map = parseBattery(descMap.value)[cite: 1]
            } else if (descMap.attrId == "0020" && descMap.value) {[cite: 1]
                map = parseBatteryVoltage(descMap.value)[cite: 1]
            }
        }
    } else if (description?.startsWith("enroll request")) {[cite: 1]
        List cmds = zigbee.enrollResponse()[cite: 1]
        return cmds[cite: 1]
    }

    if (map) {[cite: 1]
        if (txtEnable && map.descriptionText) log.info "${device.displayName} ${map.descriptionText}"[cite: 1]
        sendEvent(map)[cite: 1]
    }
    
    return [][cite: 1]
}

// Handle IAS Zone Status (Motion events)
private Map parseIasZoneStatus(String description) {
    Integer status = zigbee.parseZoneStatus(description)?.alarm1 ? 1 : 0[cite: 1]
    Integer resetTimer = (motionResetTimer != null) ? (motionResetTimer as Integer) : 0[cite: 1]
    
    if (status == 1) {[cite: 1]
        if (resetTimer > 0) {[cite: 1]
            runIn(resetTimer, "resetMotionToInactive")[cite: 1]
        }
        return [name: "motion", value: "active", descriptionText: "motion is active"][cite: 1]
    } else {
        unschedule("resetMotionToInactive")[cite: 1]
        return [name: "motion", value: "inactive", descriptionText: "motion is inactive"][cite: 1]
    }
}

// Parse Battery Percentage (0x0001 / 0x0021)
private Map parseBattery(String hexValue) {
    Integer rawValue = Integer.parseInt(hexValue, 16)[cite: 1]
    Integer pct = Math.round(rawValue / 2)[cite: 1]
    pct = Math.min(100, Math.max(0, pct))[cite: 1]
    return [name: "battery", value: pct, unit: "%", descriptionText: "battery is ${pct}%", isStateChange: true][cite: 1]
}

// Parse Battery Raw Voltage (0x0001 / 0x0020) and calculate percentage fallback
private Map parseBatteryVoltage(String hexValue) {
    Integer rawValue = Integer.parseInt(hexValue, 16)[cite: 1]
    BigDecimal voltage = (rawValue / 10.0).setScale(1, BigDecimal.ROUND_HALF_UP)[cite: 1]
    
    // Calculate battery percentage profile (3.0V = 100%, 2.5V = 0%)
    BigDecimal maxVolts = 3.0
    BigDecimal minVolts = 2.5
    Integer pct = (int) (((voltage - minVolts) / (maxVolts - minVolts)) * 100)
    pct = Math.min(100, Math.max(0, pct))
    
    // Explicitly send standard battery percentage event for capability "Battery"
    sendEvent(name: "battery", value: pct, unit: "%", descriptionText: "battery is ${pct}% (${voltage}V)", isStateChange: true)
    
    if (voltage <= 2.9) {[cite: 1]
        log.warn "${device.displayName}: Low battery voltage detected! Current voltage: ${voltage}V (Threshold: 2.9V)"[cite: 1]
    }
    
    return [name: "batteryVoltage", value: voltage, unit: "V", descriptionText: "battery voltage is ${voltage}V", isStateChange: true][cite: 1]
}

// Software motion reset timer callback
def resetMotionToInactive() {
    if (txtEnable) log.info "${device.displayName} motion reset to inactive (timeout)"[cite: 1]
    sendEvent(name: "motion", value: "inactive", descriptionText: "motion set to inactive via timeout")[cite: 1]
}

// Scheduled check-in to keep device active on inactivity reports
def checkIn() {
    if (logEnable) log.debug "eWeLink MS01 (Custom): Executing scheduled check-in"[cite: 1]
    sendEvent(name: "checkIn", value: now(), displayed: false, isStateChange: true)[cite: 1]
}

def setupSchedule() {
    unschedule("checkIn")[cite: 1]
    String interval = checkInInterval ?: "12"[cite: 1]
    switch(interval) {
        case "1":[cite: 1]
            runEvery1Hour("checkIn")[cite: 1]
            break[cite: 1]
        case "3":[cite: 1]
            runEvery3Hours("checkIn")[cite: 1]
            break[cite: 1]
        case "6":[cite: 1]
            schedule("0 0 */6 ? * *", "checkIn")[cite: 1]
            break[cite: 1]
        case "12":[cite: 1]
            schedule("0 0 */12 ? * *", "checkIn")[cite: 1]
            break[cite: 1]
        case "24":[cite: 1]
            schedule("0 0 0 ? * *", "checkIn")[cite: 1]
            break[cite: 1]
        default:
            schedule("0 0 */12 ? * *", "checkIn")[cite: 1]
            break[cite: 1]
    }
}

def configure() {
    log.info "Configuring eWeLink MS01 Motion Sensor (Custom) (v${version()})..."[cite: 1]
    
    sendEvent(name: "driverVersion", value: version())[cite: 1]
    setupSchedule()[cite: 1]
    
    // Binding and reporting configurations
    List<String> cmds = [][cite: 1]
    cmds += zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 30, 21600, 0x01) // Raw Voltage[cite: 1]
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 30, 21600, 0x01) // Battery Percentage[cite: 1]
    cmds += zigbee.readAttribute(0x0001, 0x0020)[cite: 1]
    cmds += zigbee.readAttribute(0x0001, 0x0021)[cite: 1]
    cmds += zigbee.enrollResponse()[cite: 1]
    
    return cmds[cite: 1]
}

def refresh() {
    if (logEnable) log.debug "eWeLink MS01 (Custom): Refreshing battery attributes"[cite: 1]
    List<String> cmds = [][cite: 1]
    cmds += zigbee.readAttribute(0x0001, 0x0020)[cite: 1]
    cmds += zigbee.readAttribute(0x0001, 0x0021)[cite: 1]
    return cmds[cite: 1]
}

def updated() {
    log.info "eWeLink MS01 (Custom): Preferences updated"[cite: 1]
    sendEvent(name: "driverVersion", value: version())[cite: 1]
    setupSchedule()[cite: 1]
    if (logEnable) runIn(1800, "logsOff")[cite: 1]
}

def logsOff() {
    device.updateSetting("logEnable", [value: false, type: "bool"])[cite: 1]
    log.info "eWeLink MS01 (Custom): Debug logging disabled"[cite: 1]
}