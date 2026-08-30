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
 *  v1.0.0 (2026-08-27) - Initial release for eWeLink MS01 & Sonoff SNZB-03 motion sensors.
 *  ------------------------------------------------------------------------------------------------------
 */

import hubitat.zigbee.clusters.iaszone.IasZoneAttribute
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

        // Version Status
        attribute "driverVersion", "string"

        // Fingerprints for eWeLink MS01 & Sonoff variants
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "eWeLink", model: "MS01", deviceJoinName: "eWeLink MS01 Motion Sensor (Custom)"
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "eWeLink", model: "MSO1", deviceJoinName: "eWeLink MS01 Motion Sensor (Custom)"
        fingerprint profileId: "0104", inClusters: "0000,0003,0001,0500,0020", outClusters: "0019", manufacturer: "Sonoff", model: "SNZB-03", deviceJoinName: "Sonoff SNZB-03 Motion Sensor"
    }

    preferences {
        input name: "motionResetTimer", type: "number", title: "Software Motion Reset (seconds)", description: "Auto-reset motion to inactive if sensor misses clear (0 to disable)", defaultValue: 60, required: true
        input name: "checkInInterval", type: "enum", title: "Activity Check-In Interval", options: ["4":"4 Hours", "8":"8 Hours", "12":"12 Hours", "24":"24 Hours"], defaultValue: "8", required: true
        input name: "logEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable Description Text", defaultValue: true
    }
}

private String version() { return "1.0.0" }

// Parse incoming Zigbee messages
def parse(String description) {
    if (logEnable) log.debug "eWeLink MS01 (Custom): Parsing description: ${description}"
    
    Map map = [:]
    
    if (description?.startsWith("zone status")) {
        map = parseIasZoneStatus(description)
    } else if (description?.startsWith("read attr -")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        if (descMap.cluster == "0001" && descMap.attrId == "0021") {
            map = parseBattery(descMap.value)
        }
    } else if (description?.startsWith("enroll request")) {
        List cmds = zigbee.enrollResponse()
        return cmds
    }

    if (map) {
        if (txtEnable && map.descriptionText) log.info "${device.displayName} ${map.descriptionText}"
        sendEvent(map)
    }
    
    // Update last activity timestamp for health check tracking
    state.lastActivity = now()
    return []
}

// Handle IAS Zone Status (Motion events)
private Map parseIasZoneStatus(String description) {
    Integer status = zigbee.parseZoneStatus(description)?.alarm1 ? 1 : 0
    if (status == 1) {
        if (motionResetTimer && motionResetTimer > 0) {
            runIn(motionResetTimer, "resetMotionToInactive")
        }
        return [name: "motion", value: "active", descriptionText: "motion is active"]
    } else {
        unschedule("resetMotionToInactive")
        return [name: "motion", value: "inactive", descriptionText: "motion is inactive"]
    }
}

// Parse Battery Percentage (0x0001 / 0x0021)
private Map parseBattery(String hexValue) {
    Integer rawValue = Integer.parseInt(hexValue, 16)
    Integer pct = Math.round(rawValue / 2)
    pct = Math.min(100, Math.max(0, pct))
    return [name: "battery", value: pct, unit: "%", descriptionText: "battery is ${pct}%", isStateChange: true]
}

// Software motion reset timer callback
def resetMotionToInactive() {
    if (txtEnable) log.info "${device.displayName} motion reset to inactive (timeout)"
    sendEvent(name: "motion", value: "inactive", descriptionText: "motion set to inactive via timeout")
}

// Scheduled check-in to keep device active on inactivity reports
def checkIn() {
    if (logEnable) log.debug "eWeLink MS01 (Custom): Executing scheduled check-in"
    sendEvent(name: "checkIn", value: now(), displayed: false, isStateChange: true)
}

def configure() {
    log.info "Configuring eWeLink MS01 Motion Sensor (Custom) (v${version()})..."
    unschedule()
    
    // Set driver version state
    sendEvent(name: "driverVersion", value: version())
    
    // Schedule checkIn based on preference
    Integer hours = (checkInInterval ?: "8").toInteger()
    switch(hours) {
        case 4: runEvery3Hours("checkIn"); break
        case 8: runEvery8Hours("checkIn"); break
        case 12: runEvery12Hours("checkIn"); break
        case 24: runEvery3Hours("checkIn"); break // Fallback mapping using standard scheduler
        default: runEvery8Hours("checkIn"); break
    }
    
    // Binding and reporting configurations
    List<String> cmds = []
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 30, 21600, 0x01) // Battery reporting
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    cmds += zigbee.enrollResponse()
    
    return cmds
}

def refresh() {
    if (logEnable) log.debug "eWeLink MS01 (Custom): Refreshing battery attribute"
    return zigbee.readAttribute(0x0001, 0x0021)
}

def updated() {
    log.info "eWeLink MS01 (Custom): Preferences updated"
    sendEvent(name: "driverVersion", value: version())
    if (logEnable) runIn(1800, "logsOff")
}

def logsOff() {
    device.updateSetting("logEnable", [value: "false", type: "bool"])
    log.info "eWeLink MS01 (Custom): Debug logging disabled"
}