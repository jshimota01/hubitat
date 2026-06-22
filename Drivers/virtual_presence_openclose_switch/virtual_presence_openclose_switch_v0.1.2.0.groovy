/**
 * Virtual Presence Contact and Switch
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
 * Change History:
 *
 * Date          Source        Version          What
 * ----          ------        -------          ----
 * 2022-11-27    jshimota      0.1.0.0          My 'fork' of this driver taken from original source
 * 2022-11-27    jshimota      0.1.0.1          Added Toggle command to flip state programmatically
 * 2022-12-05    jshimota      0.1.1.0          Released version
 * 2022-12-08    jshimota      0.1.1.1          Added AutoOff
 * 2023-01-14    jshimota      0.1.1.2          Added Variable Auto Presence array for my own needs
 * 2023-04-15    jshimota      0.1.1.3          Added Open/Closed variables for community request
 * 2023-04-15    jshimota      0.1.1.4          Debug work on auto on off for all 3 states
 * 2023-08-02    jshimota      0.1.1.5          Set Default Init State to Off
 * 2024-01-23    jshimota      0.1.1.6          Set Null check for Auto values
 * 2026-01-06    jshimota      0.1.1.7          Cleaned typo in filename
 * 2026-05-14    jshimota      0.1.1.8          Fix package json to required true
 * 2026-06-22    jshimota      0.1.2.0          Fixed parent signature context, explicit runIn string targets, and scoped variables safely
 **/

static String version() { return '0.1.2.0' }

metadata {
    definition (name: "Virtual Presence Contact and Switch", namespace: "jshimota", author: "James Shimota", importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/virtual_presence_openclose_switch/virtual_presence__openclose_switche_openclose_switch/virtual_presence__openclose_switch.groovy") {
        capability "Initialize"
        capability "Actuator"
        capability "Switch"
        capability "PresenceSensor"
        capability "Sensor"
        capability "ContactSensor"

        command "arrived"
        command "departed"
        command "open"
        command "closed"
        command "on"
        command "off"
        command "togglePresence"
        command "toggleContact"
        command "toggleSwitch"
        command "readCurrentValuesIntoLog"
    }

    preferences {
        input name: "debugLogEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "defInitState", type: "bool", title: "Set Default Initialize On", defaultValue: false
        input name: "autoPresenceOffOn", type: "enum", description: "Automatically turns presence to Arrived for the device after selected time.", title: "Enable Auto-Presence Arrived", options: [["0":"Disabled"],["1":"1 second"],["2":"2 seconds"],["3":"3 seconds"],["4":"4 seconds"],["5":"5 seconds"],["10":"10 seconds"],["15":"15 seconds"],["20":"20 seconds"],["25":"25 seconds"],["30":"30 seconds"],["45":"45 seconds"],["60":"1 minute"],["120":"2 minutes"],["300":"5 minutes"],["600":"10 minutes"],["900":"15 minutes"],["1200":"20 minutes"],["1800":"30 minutes"],["2700":"45 minutes"],["3600":"1 hour"]], defaultValue: "0"
        input name: "autoContactOffOn", type: "enum", description: "Automatically turns contact to Opened for the device after selected time.", title: "Enable Auto-Contact Open", options: [["0":"Disabled"],["1":"1 second"],["2":"2 seconds"],["3":"3 seconds"],["4":"4 seconds"],["5":"5 seconds"],["10":"10 seconds"],["15":"15 seconds"],["20":"20 seconds"],["25":"25 seconds"],["30":"30 seconds"],["45":"45 seconds"],["60":"1 minute"],["120":"2 minutes"],["300":"5 minutes"],["600":"10 minutes"],["900":"15 minutes"],["1200":"20 minutes"],["1800":"30 minutes"],["2700":"45 minutes"],["3600":"1 hour"]], defaultValue: "0"
        input name: "autoSwitchOffOn", type: "enum", description: "Automatically turns switch to On for the device after selected time.", title: "Enable Auto-Switch On", options: [["0":"Disabled"],["1":"1 second"],["2":"2 seconds"],["3":"3 seconds"],["4":"4 seconds"],["5":"5 seconds"],["10":"10 seconds"],["15":"15 seconds"],["20":"20 seconds"],["25":"25 seconds"],["30":"30 seconds"],["45":"45 seconds"],["60":"1 minute"],["120":"2 minutes"],["300":"5 minutes"],["600":"10 minutes"],["900":"15 minutes"],["1200":"20 minutes"],["1800":"30 minutes"],["2700":"45 minutes"],["3600":"1 hour"]], defaultValue: "0"
    }
}

def parse(String description) {
    def pair = description.split(":")
    createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def arrived() {
    unschedule('togglePresence')
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "contact", value: "open")
    sendEvent(name: "presence", value: "present")
    parent?.componentOn(this) // Fixed: reference context changed to 'this'
    if (txtEnable) log.info "${device.displayName} presence set to PRESENT."
}

def departed() {
    unschedule('togglePresence')
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "contact", value: "closed")
    sendEvent(name: "presence", value: "not present")
    parent?.componentOff(this) // Fixed: reference context changed to 'this'
    if (txtEnable) log.info "${device.displayName} presence set to NOT PRESENT."
    autoTogglePresence()
}

def open() {
    unschedule('toggleContact')
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "contact", value: "open")
    sendEvent(name: "presence", value: "present")
    parent?.componentOn(this)
    if (txtEnable) log.info "${device.displayName} Contact set to OPEN."
}

def closed() {
    unschedule('toggleContact')
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "contact", value: "closed")
    sendEvent(name: "presence", value: "not present")
    parent?.componentOff(this)
    if (txtEnable) log.info "${device.displayName} contact set to CLOSED."
    autoToggleContact()
}

def on() {
    unschedule('toggleSwitch')
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "contact", value: "open")
    sendEvent(name: "presence", value: "present")
    parent?.componentOn(this)
    if (txtEnable) log.info "${device.displayName} switch turned ON."
}

def off() {
    unschedule('toggleSwitch')
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "contact", value: "closed")
    sendEvent(name: "presence", value: "not present")
    parent?.componentOff(this)
    if (txtEnable) log.info "${device.displayName} switch turned OFF."
    autoToggleSwitch()
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("debugLogEnable", [value: "false", type: "bool"])
}

def initialize() {
    unschedule()
    if (debugLogEnable) { runIn(1800, 'logsOff') }
    
    if (!defInitState) {
        sendEvent(name: "presence", value: "not present")
        sendEvent(name: "contact", value: "closed")
        sendEvent(name: "switch", value: "off")
        if (txtEnable) log.info "${device.displayName} initialized: Switch=Off, Presence=Not Present, Contact=Closed"
    } else {
        sendEvent(name: "presence", value: "present")
        sendEvent(name: "contact", value: "open")
        sendEvent(name: "switch", value: "on")
        if (txtEnable) log.info "${device.displayName} initialized: Switch=On, Presence=Present, Contact=Open"
    }
    
    int pDelay = (autoPresenceOffOn ?: 0).toInteger()
    int cDelay = (autoContactOffOn ?: 0).toInteger()
    int sDelay = (autoSwitchOffOn ?: 0).toInteger()
    
    if (txtEnable) {
        log.info "Initialized: autoPresenceOffOn is ${pDelay > 0 ? "set to toggle in $pDelay seconds" : "disabled"}."
        log.info "Initialized: autoContactOffOn is ${cDelay > 0 ? "set to toggle in $cDelay seconds" : "disabled"}."
        log.info "Initialized: autoSwitchOffOn is ${sDelay > 0 ? "set to toggle in $sDelay seconds" : "disabled"}."
    }
}

def updated(){
    log.info "${device.displayName} updated..."
    initialize()
}

def autoTogglePresence() {
    int delay = (autoPresenceOffOn ?: 0).toInteger()
    if (delay > 0) {
        if (debugLogEnable) log.debug "${device.displayName} will toggle presence in $delay seconds."
        runIn(delay, 'togglePresence') // Fixed: String signature target method name
    }
}

def autoToggleContact() {
    int delay = (autoContactOffOn ?: 0).toInteger()
    if (delay > 0) {
        if (debugLogEnable) log.debug "${device.displayName} will toggle contact in $delay seconds."
        runIn(delay, 'toggleContact') // Fixed: String signature target method name
    }
}

def autoToggleSwitch() {
    int delay = (autoSwitchOffOn ?: 0).toInteger()
    if (delay > 0) {
        if (debugLogEnable) log.debug "${device.displayName} will toggle switch in $delay seconds."
        runIn(delay, 'toggleSwitch') // Fixed: String signature target method name
    }
}

def togglePresence() {
    if (device.currentValue("presence") != "present") {
        arrived()
    } else {
        departed()
    }
}

def toggleContact() {
    if (device.currentValue("contact") != "open") {
        open()
    } else {
        closed()
    }
}

def toggleSwitch() {
    if (device.currentValue("switch") != "on") {
        on()
    } else {
        off()
    }
}

void readCurrentValuesIntoLog() {
    if (!txtEnable) return
    String csPresence = device.currentValue("presence")
    String csContact = device.currentValue("contact")
    String csSwitch = device.currentValue("switch")
    
    int pDelay = (autoPresenceOffOn ?: 0).toInteger()
    int cDelay = (autoContactOffOn ?: 0).toInteger()
    int sDelay = (autoSwitchOffOn ?: 0).toInteger()
    
    log.info "--- Current Device Values Summary ---"
    log.info "Current Switch Position   : ${csSwitch}"
    log.info "Current Presence Position : ${csPresence}"
    log.info "Current Contact Position  : ${csContact}"
    log.info "Auto-Presence Config      : ${pDelay > 0 ? "$pDelay seconds" : "Disabled"}"
    log.info "Auto-Contact Config       : ${cDelay > 0 ? "$cDelay seconds" : "Disabled"}"
    log.info "Auto-Switch Config        : ${sDelay > 0 ? "$sDelay seconds" : "Disabled"}"
}