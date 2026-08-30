/**
 *  Third Reality Contact Sensor (Custom)
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
 *  v1.3.1 (2026-08-27) - Updated default check-in interval to 12 hours and verified Hubitat 2.4 compatibility.
 *  v1.3.0 (2026-08-27) - Improved log prefix formatting with device display name.
 *  v1.2.0 (2026-08-27) - Added customizable activity check-in intervals (1, 3, 6, 12, 24 hours).
 *  v1.1.0 (2026-08-27) - Added version tracking attribute and forced state updates.
 *  v1.0.0 (2026-08-27) - Initial release for Third Reality 3RDS17BZ contact sensor.
 *  ------------------------------------------------------------------------------------------------------
 */

import hubitat.zigbee.zcl.DataType

metadata {
    definition (
        name: "Third Reality Contact Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: ""
    ) {
        capability "ContactSensor"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"

        // Version Status
        attribute "driverVersion", "string"

        // Fingerprint for Third Reality 3RDS17BZ
        fingerprint profileId: "0104", inClusters: "0000,0001,0500,0003", outClusters: "0019", manufacturer: "3Reality", model: "3RDS17BZ", deviceJoinName: "Third Reality Contact Sensor (Custom)"
    }

    preferences {
        input name: "checkInInterval", type: "enum", title: "Activity Check-In Interval", options: ["1":"1 Hour", "3":"3 Hours", "6":"6 Hours", "12":"12 Hours", "24":"24 Hours"], defaultValue: "12", required: true
        input name: "logEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable Description Text", defaultValue: true
    }
}

private String version() { return "1.3.1" }

// Parse incoming Zigbee messages
def parse(String description) {
    if (logEnable) log.debug "${device.displayName}: Parsing description: ${description}"
    
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
    
    return []
}

// Handle IAS Zone Status (Contact Open/Closed events)
private Map parseIasZoneStatus(String description) {
    Integer status = zigbee.parseZoneStatus(description)?.alarm1 ? 1 : 0
    if (status == 1) {
        return [name: "contact", value: "open", descriptionText: "is open"]
    } else {
        return [name: "contact", value: "closed", descriptionText: "is closed"]
    }
}

// Parse Battery Percentage (0x0001 / 0x0021)
private Map parseBattery(String hexValue) {
    Integer rawValue = Integer.parseInt(hexValue, 16)
    Integer pct = Math.round(rawValue / 2)
    pct = Math.min(100, Math.max(0, pct))
    return [name: "battery", value: pct, unit: "%", descriptionText: "battery is ${pct}%", isStateChange: true]
}

// Scheduled check-in to maintain activity state on inactivity reports
def checkIn() {
    if (logEnable) log.debug "${device.displayName}: Executing scheduled check-in"
    sendEvent(name: "checkIn", value: now(), displayed: false, isStateChange: true)
}

def setupSchedule() {
    unschedule("checkIn")
    String interval = checkInInterval ?: "12"
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
    log.info "${device.displayName}: Configuring Third Reality Contact Sensor (v${version()})..."
    
    sendEvent(name: "driverVersion", value: version())
    setupSchedule()
    
    // Binding and reporting configurations
    List<String> cmds = []
    cmds += zigbee.configureReporting(0x0001, 0x0021, DataType.UINT8, 30, 21600, 0x01) // Battery reporting
    cmds += zigbee.readAttribute(0x0001, 0x0021)
    cmds += zigbee.enrollResponse()
    
    return cmds
}

def refresh() {
    if (logEnable) log.debug "${device.displayName}: Refreshing battery attribute"
    return zigbee.readAttribute(0x0001, 0x0021)
}

def updated() {
    log.info "${device.displayName}: Preferences updated"
    sendEvent(name: "driverVersion", value: version())
    setupSchedule()
    if (logEnable) runIn(1800, "logsOff")
}

def logsOff() {
    device.updateSetting("logEnable", [value: false, type: "bool"])
    log.info "${device.displayName}: Debug logging disabled"
}