/*
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
 * */
metadata {
    definition(
            name: "Virtual Inverse-able w-autooff Switch",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/virtual_inverse-able_switch/virtualInverse-ableSwitch.groovy"
    ) {
        capability "Actuator"
        capability "Switch"
        capability "Initialize"
        capability "Refresh"
        command "toggle"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "reversed", type: "bool", title: "Reverse Action", defaultValue: false, required: true
        // input name: "forceUpdate", type: "bool", title: "Force State Update", description: "Send event everytime, regardless of current status. ie Send/Do On even if already On.",  defaultValue: false
        input name: "autoOff", type: "enum", description: "Automatically turns off the device after selected time.", title: "Enable Auto-Off", options: [[0:"Disabled"],[1:"1s"],[2:"2s"],[5:"5s"],[10:"10s"],[20:"20s"],[30:"30s"],[60:"1m"],[120:"2m"],[300:"5m"],[1800:"30m"],[3200:"60m"]], defaultValue: 0
    }
}

def parse(description) {
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def on() {
    if (reversed) {
        sendEvent(name: "switch", value: "off")
        state.device = false
        parent?.componentOff(this.device)
        if (txtEnable) log.info "${device.displayName} turned ON but is reversed."
    } else {
        sendEvent(name: "switch", value: "on")
        state.device = true
        parent?.componentOn(this.device)
        if (txtEnable) log.info "${device.displayName} turned ON but is NOT reversed."
    }
    autotoggle()
}

def off() {
    if (reversed) {
        sendEvent(name: "switch", value: "on")
        state.device = true
        parent?.componentOn(this.device)
        if (txtEnable) log.info "${device.displayName} turned OFF but is reversed."
    } else {
        sendEvent(name: "switch", value: "off")
        state.device = false
        parent?.componentOff(this.device)
        if (txtEnable) log.info "${device.displayName} turned OFF but is NOT reversed."
    }
    autotoggle()
}

def autotoggle() {
    if (autoOff.toInteger() > 0) {
        runIn(autoOff.toInteger(), toggle)
    }
}

def toggle() {
    def switchvalue = device.currentValue("switch")
    log.info "switch is ${state.device}"
    log.info "switchvalue is $switchvalue"
    log.info "reversed is $reversed"
    //if (device.currentValue("switch") == "on") log.info "switch is on as toggle runs"
    if ((reversed) && (device.currentValue("switch") == "on")) {
        off()
    }
    if ((reversed) && (device.currentValue("switch") == "off")) {
        on()
    }
    if ((!reversed) && (device.currentValue("switch") == "off")) {
        on()
    }
    if ((!reversed) && (device.currentValue("switch") == "on")) {
        off()
    }
}

def refresh() {
    initialize()
}

def updated(){
    log.info "${device.displayName} updated..."
    log.warn "${device.displayName} debug logging is: ${logEnable == true}"
    log.warn "${device.displayName} description logging is: ${txtEnable == true}"
}

def installed() {
    initialize()
}

def initialize() {
    if (!reversed) {
        off()
        if (txtEnable) log.info "${device.displayName} is initialized as OFF with no reversal"
    } else {
        on()
        log.info "${device.displayName} is initialized as ON but is reversed!"
    }
}