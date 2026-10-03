/**
 * Zone Motion Advanced Child Device (Virtual Motion Driver)
 * Platform: Hubitat Elevation (v2.4.4.156)
 * Purpose: Virtual Motion Sensor driver used by Zone Motion Advanced Child App.
 **/
/**
 * Copyright 2026 James Shimota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
/**
 * Purpose:
 * Represents the aggregated virtual motion status for a Zone Motion Advanced zone.
 *
 * Changelog:
 *  v1.0.0    10/01/26    jshimota    Initial release.
 **/

static String version() { return '1.0.0' }
def timeStamp() { return "2026/10/01 11:00 AM" }

metadata {
    definition(
        name: "Zone Motion Advanced Child Device",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/zone_motion_advanced_child_device/zone_motion_advanced_child_device.groovy"
    ) {
        capability "Motion Sensor"
        capability "Sensor"

        command "setActive"
        command "setInactive"
    }

    preferences {
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
    }
}

void installed() {
    sendEvent(name: "motion", value: "inactive")
}

void updated() {
}

void setActive() {
    if (device.currentValue("motion") != "active") {
        if (settings.logInfoEnable != false) log.info "${device.displayName} motion is active"
        sendEvent(name: "motion", value: "active", isStateChange: true)
    }
}

void setInactive() {
    if (device.currentValue("motion") != "inactive") {
        if (settings.logInfoEnable != false) log.info "${device.displayName} motion is inactive"
        sendEvent(name: "motion", value: "inactive", isStateChange: true)
    }
}