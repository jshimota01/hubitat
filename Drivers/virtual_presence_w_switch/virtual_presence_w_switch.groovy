/**  ORIGINAL
 *  Virtual Presence and Switch
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
/**
 *  Virtual Presence and Switch
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/**
 *  Change History:
 *
 *      ORIGINAL SOURCE
 *      2021          Joel Wetzel   Unknown
 *
 *      Date          Source        Version          What                                              URL
 *      ----          ------        -------          ----                                              ---
 *      2022-11-27    jshimota      0.1.0.0          My 'fork' of this driver taken from the above original source
 *      2022-11-27    jshimota      0.1.0.1          Added Toggle command to flip state programmatically
 *      2022-12-05    jshimota      0.1.1.0          Released version
 *      2022-12-08    jshimota      0.1.1.1          Added AutoOff
 *      2023-01-14    jshimota      0.1.1.2          Added Variable Auto Presence array for my own needs
 *		2023-04-15	  jshimota		0.1.1.3			 Added Open/Closed variables for community request / changed Command button label
 **/

static String version() { return '0.1.1.3' }

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
        input name: "autoPresenceOffOn", type: "enum", description: "Automatically turns presence to On for the device after selected time.", title: "Enable Auto-Presence On", options: [[0:"Disabled"],[1:"1 second"],[2:"2 seconds"],[3:"3 seconds"],[4:"4 seconds"],[5:"5 seconds"],[10:"10 seconds"],[15:"15 seconds"],[20:"20 seconds"],[25:"25 seconds"],[30:"30 seconds"],[45:"45 seconds"],[60:"1 minute"],[120:"2 minutes"],[300:"5 minutes"],[600:"10 minutes"],[900:"15 minutes"],[1200:"20 minutes"],[1800:"30 minutes"],[2700:"45 minutes"],[3200:"1 hour"]], defaultValue: 0
    }

}

def parse(String description) {
    def pair = description.split(":")
    createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def arrived() {
    sendEvent(name: "presence", value: "present")
    if (txtEnable) log.info "${device.displayName} presence set to present."
}

def departed() {
    sendEvent(name: "presence", value: "not present")
    if (txtEnable) log.info "${device.displayName} presence set to not present."
    autoToggle()
}

def on() {
    sendEvent(name: "switch", value: "on", isStateChange: true)
    state.device = true
    parent?.componentOn(this.device)
    if (txtEnable) log.info "${device.displayName} Switched On: - device state is $state.device."
    if (autoPresenceOffOn.toInteger() > 0) {
        if (txtEnable) log.info "Switched On: presence will toggle in $autoPresenceOffOn seconds"
        departed()
    } else {
        if (txtEnable) log.info "Switched On: presence toggle is disabled"
    }
    if (txtEnable) log.info "${device.displayName} switch turned ON - runnng departed function."
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
    state.device = false
    parent?.componentOff(this.device)
    if (txtEnable) log.info "${device.displayName} switch turned OFF - device state is $state.device."
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled due to logoff being run..."
    device.updateSetting("debugLogEnable", [value: "false", type: "bool"])
}

def initialize() {
    if (txtEnable) log.info "INITIALIZE button pushed."
    sendEvent(name: "switch", value: "on", isStateChange: true)
    sendEvent(name: "presence", value: "present", isStateChange: true)
    state.device = false

    if (txtEnable) log.info "${device.displayName} is initialized"
    if (txtEnable) log.info "Initialized: Switch is On"
    if (txtEnable) log.info "Initialized: Presence is Present"
    if (autoPresenceOffOn.toInteger() > 0) {
        if (txtEnable) log.info "Initialized: autoPresenceOffOn is set to toggle in $autoPresenceOffOn seconds"
    } else {
        if (txtEnable) log.info "Initialized: autoPresenceOffOn is disabled"
    }
}

def updated(){
    log.info "${device.displayName} updated..."
    log.warn "${device.displayName} debug logging is: ${debugLogEnable == true}"
    log.warn "${device.displayName} description logging is: ${txtEnable == true}"
    initialize()
}

def autoToggle() {
    if (autoPresenceOffOn.toInteger() > 0) {
        if (debugLogEnable) log.debug "${device.displayName} will toggle in $autoPresenceOffOn seconds."
        runIn(autoPresenceOffOn.toInteger(), togglePresence)
    }
}

def togglePresence() {
    if (device.currentState("presence")?.value != "present") {
        arrived()
    } else {
        departed()
    }
    if (debugLogEnable) log.debug "${device.displayName} was turned back on after the set delay."
}

def readCurrentValuesIntoLog() {
    csPresence = device.currentState("presence")?.value
    if (txtEnable) log.info "Read Current Values Into Log button pushed on device page."
    if (txtEnable) log.info "Read Current State value: device switch state is $state.device."
    if (txtEnable) log.info "Read Current State value: device presence state is $csPresence."
    if (autoPresenceOffOn.toInteger() > 0) {
        if (txtEnable) log.info "Read Current State Variable: autoPresenceOffOn set to toggle in $autoPresenceOffOn seconds."
    } else {
        if (txtEnable) log.info "Read Current State Variable: autoPresenceOffOn is disabled."
    }
    if (txtEnable) log.info "Read Current Preference Variable: Description text enabled."
    if (!txtEnable) log.info "Read Current Preference Variable: Description text disabled."
    if (debugLogEnable) log.info "Read Current Preference Variable: Debug text enabled."
    if (!debugLogEnable) log.info "Read Current Preference Variable: Debug text disabled."
    if (debugLogEnable) log.warn "Read Current Preference Variable: Debug ia enabled. It must be enabled to see realtime Switch value in log. This has performance impact."
    if (debugLogEnable) {
        pauseExecution(500)
        switchvalue = device.currentValue("switch")
        if (txtEnable) log.warn "Read Current Preference Variable: switchvalue is $switchvalue"
    }
}