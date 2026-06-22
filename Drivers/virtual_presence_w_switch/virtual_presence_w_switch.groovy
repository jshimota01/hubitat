/**
 * Virtual Presence and Switch
 *
 * Copyright 2019 Joel Wetzel
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
 */

/**
 * Change History:
 *
 * ORIGINAL SOURCE
 * 2021          Joel Wetzel   Unknown
 *
 * Date          Source        Version          What
 * ----          ------        -------          ----
 * 2022-11-27    jshimota      0.1.0.0          My 'fork' of this driver taken from the original source
 * 2022-11-27    jshimota      0.1.0.1          Added Toggle command to flip state programmatically
 * 2022-12-05    jshimota      0.1.1.0          Released version
 * 2022-12-08    jshimota      0.1.1.1          Added AutoOff
 * 2023-01-14    jshimota      0.1.1.2          Added Variable Auto Presence array for my own needs
 * 2023-04-15    jshimota      0.1.1.3          Added Open/Closed variables for community request
 * 2026-06-22    jshimota      0.1.2.0          Fixed on() side-effects, parent signature context, explicit runIn string targets, and type protection
 **/

static String version() { return '0.1.2.0' }

metadata {
    definition (name: "Virtual Presence and Switch", namespace: "jshimota", author: "Jim Shimota", importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/virtual_presence_w_switch/virtual_presence_w_switch.groovy") {
        capability "Initialize"
        capability "Actuator"
        capability "Switch"
        capability "PresenceSensor"
        capability "Sensor"

        command "arrived"
        command "departed"
        command "on"
        command "off"
        command "togglePresence"
        command "readCurrentValuesIntoLog"
    }

    preferences {
        input name: "debugLogEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "autoPresenceOffOn", type: "enum", description: "Automatically turns presence to On for the device after selected time.", title: "Enable Auto-Presence On", options: [["0":"Disabled"],["1":"1 second"],["2":"2 seconds"],["3":"3 seconds"],["4":"4 seconds"],["5":"5 seconds"],["10":"10 seconds"],["15":"15 seconds"],["20":"20 seconds"],["25":"25 seconds"],["30":"30 seconds"],["45":"45 seconds"],["60":"1 minute"],["120":"2 minutes"],["300":"5 minutes"],["600":"10 minutes"],["900":"15 minutes"],["1200":"20 minutes"],["1800":"30 minutes"],["2700":"45 minutes"],["3600":"1 hour"]], defaultValue: "0"
    }
}

def parse(String description) {
    def pair = description.split(":")
    createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def arrived() {
    unschedule('togglePresence')
    sendEvent(name: "presence", value: "present")
    if (txtEnable) log.info "${device.displayName} presence set to present."
}

def departed() {
    unschedule('togglePresence')
    sendEvent(name: "presence", value: "not present")
    if (txtEnable) log.info "${device.displayName} presence set to not present."
    autoToggle()
}

def on() {
    sendEvent(name: "switch", value: "on")
    parent?.componentOn(this) // Fixed: Changed reference context from 'this.device' to 'this'
    if (txtEnable) log.info "${device.displayName} switch turned ON."
    
    // Fixed logic: Only handle auto toggle if configuration demands it, do not blind-call departed()
    int delay = (autoPresenceOffOn ?: 0).toInteger()
    if (delay > 0) {
        if (txtEnable) log.info "Switched On: presence auto-activation scheduled in $delay seconds"
        autoToggle()
    }
}

def off() {
    sendEvent(name: "switch", value: "off")
    parent?.componentOff(this) // Fixed: Changed reference context from 'this.device' to 'this'
    if (txtEnable) log.info "${device.displayName} switch turned OFF."
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("debugLogEnable", [value: "false", type: "bool"])
}

def initialize() {
    unschedule()
    if (debugLogEnable) { runIn(1800, 'logsOff') }

    sendEvent(name: "switch", value: "on")
    sendEvent(name: "presence", value: "present")

    if (txtEnable) {
        log.info "${device.displayName} initialized: Switch=On, Presence=Present"
        int delay = (autoPresenceOffOn ?: 0).toInteger()
        if (delay > 0) {
            log.info "Initialized: autoPresenceOffOn is set to toggle in $delay seconds"
        } else {
            log.info "Initialized: autoPresenceOffOn is disabled"
        }
    }
}

def updated(){
    log.info "${device.displayName} updated..."
    initialize()
}

def autoToggle() {
    int delay = (autoPresenceOffOn ?: 0).toInteger()
    if (delay > 0) {
        if (debugLogEnable) log.debug "${device.displayName} will toggle in $delay seconds."
        runIn(delay, 'togglePresence') // Fixed: String target method name literal configuration
    }
}

def togglePresence() {
    if (device.currentValue("presence") != "present") {
        arrived()
    } else {
        departed()
    }
}

void readCurrentValuesIntoLog() {
    if (!txtEnable) return
    String csPresence = device.currentValue("presence")
    String csSwitch = device.currentValue("switch")
    int delay = (autoPresenceOffOn ?: 0).toInteger()
    
    log.info "--- Current Device Values Summary ---"
    log.info "Current Switch Position   : ${csSwitch}"
    log.info "Current Presence Position : ${csPresence}"
    log.info "Auto-Presence Config      : ${delay > 0 ? "$delay seconds" : "Disabled"}"
}