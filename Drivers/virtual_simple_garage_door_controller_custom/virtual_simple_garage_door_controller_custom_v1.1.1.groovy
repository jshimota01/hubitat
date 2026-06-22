/**
 * Virtual Simple Garage Door Controller (Custom)
 * Device Driver for Hubitat Elevation hub
 * Version 1.1.1
 *
 * This is a simple virtual garage door controller driver that does not change its state automatically. 
 * Used by the Garage Opener app.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Change History:
 *
 * Date          Source        Version     What
 * ----          ------        -------     ----
 * 2021          muxa          1.0.0       Original Source Release
 * 2026-06-22    jshimota      1.1.0       Fixed wrnEnable reference bug, generalized logging helpers, added Initialize capability, added auto logsOff
 * 2026-06-22    jshimota      1.1.1       Fixed logging preference mappings between wrnEnable and dbgEnable, aligned auto-timeout
 */

static String version() { return '1.1.1' }

metadata {
    definition (name: "Virtual Simple Garage Door Controller (Custom)", namespace: "jshimota", author: "Mikhail Diatchenko") {
        capability "GarageDoorControl"
        capability "Actuator"
        capability "Initialize"
    }

    preferences {
        input name: "wrnEnable", type: "bool", title: "Enable warning logging", defaultValue: false
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

void parse(String description) {
    displayDebugLog "parse ${description}"
}

void logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}

void installed() {
    displayDebugLog "installed"
    initialize()
}

void configure() {
    displayDebugLog "configure"
}

void updated() {
    displayDebugLog "updated"
    unschedule('logsOff')
    if (dbgEnable) { // Fixed: Condition gated to debug preference instead of warnings
        log.warn "${device.displayName} debug logging is enabled for 30 minutes."
        runIn(1800, 'logsOff') 
    }
    initialize()
}

void initialize() {
    if (device.currentValue("door") == null) {
        sendEvent(name: "door", value: "unknown")
    }
}

void close() {
    if (device.currentValue("door") != "closed") {
        displayInfoLog "closing"
        sendEvent(name: "door", value: "closing")
    }
}

void open() {
    if (device.currentValue("door") != "open") {
        displayInfoLog "opening"
        sendEvent(name: "door", value: "opening")
    }
}

private void displayDebugLog(String message) {
    if (dbgEnable) log.debug "${device.displayName}: ${message}" // Fixed: Realigned to use dbgEnable
}

private void displayInfoLog(String message) {
    if (txtEnable) log.info "${device.displayName}: ${message}"
}