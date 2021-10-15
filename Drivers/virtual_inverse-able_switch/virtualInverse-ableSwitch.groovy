/**
 * Virtual Inverse-able Switch
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2021-10-10    jshimota      0.1.0       Starting version
 *      2021-10-11    jshimota      0.1.1       Added redundant set using parent and added initialize to follow preference so always starts in off position
 *
 */
metadata {
    definition(
            name: "Virtual Inverse-able Switch",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/virtual_inverse-able_switch/virtualInverse-ableSwitch.groovy"
    ) {
        capability "Actuator"
        capability "Switch"
        capability "Initialize"
        capability "Refresh"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "reversed", type: "bool", title: "Reverse Action", defaultValue: false, required: true
    }
}

def parse(description) {
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def on() {
    if (reversed) sendEvent(name: "switch", value: "off")
    if (reversed) state.device = false
    parent?.componentOff(this.device)
    if (!reversed) sendEvent(name: "switch", value: "on")
    if (!reversed) state.device = true
    parent?.componentOn(this.device)
}

def off() {
    if (reversed) sendEvent(name: "switch", value: "on")
    if (reversed) state.device = true
    parent?.componentOn(this.device)
    if (!reversed) sendEvent(name: "switch", value: "off")
    if (!reversed) state.device = false
    parent?.componentOff(this.device)
}

def updated(){
}

def installed() {
    initialize()
}

def initialize() {
    if (!reversed) off()
    if (reversed) on()
}