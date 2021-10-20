/*
 * Virtual Inverse-able w-autotoggle Switch
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
 *      2021-10-17    jshimota      0.1.2       Added Toggle feature
 *      2021-10-20    jshimota      0.2.0       Added AutoOffOn - will toggle based on device setting in preferences
 * */
metadata {
    definition(
            name: "Virtual Inverse-able w-autotoggle Switch",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/virtual_inverse-able_switch/virtualInverse-ableSwitch.groovy"
    ) {
        capability "Actuator"
        capability "Switch"
        capability "Initialize"
        command "toggle"
        command "readCurrentIntoLog"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "reversed", type: "bool", title: "Reverse Action", defaultValue: false, required: true
        input name: "autoOffOn", type: "enum", description: "Automatically turns off -or- on the device after selected time.", title: "Enable Auto-Off-On", options: [[0:"Disabled"],[1:"1 second"],[2:"2 seconds"],[5:"5 seconds"],[10:"10 seconds"],[15:"15 seconds"],[20:"20 seconds"],[30:"30 seconds"],[45:"45 seconds"],[60:"1 minute"],[120:"2 minutes"],[300:"5 minutes"],[600:"10 minutes"],[900:"15 minutes"],[1200:"20 minutes"],[1800:"30 minutes"],[2700:"45 minutes"],[3200:"1 hour"]], defaultValue: 0
    }
}

def parse(description) {
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def on() {
    sendEvent(name: "switch", value: "on", isStateChange: true)
    if (reversed) {
        state.device = false
        parent?.componentOff(this.device)
        if (txtEnable) log.info "${device.displayName} switch turned ON - is reversed - device state is $state.device."
    } else {
        state.device = true
        state.autoOffOnFired = false
        parent?.componentOn(this.device)
        if (txtEnable) log.info "${device.displayName} switch turned ON - is NOT reversed - device state is $state.device."
    }
    autotoggle()
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
    if (reversed) {
        state.device = true
        state.autoOffOnFired = false
        parent?.componentOn(this.device)
        if (txtEnable) log.info "${device.displayName} switch turned OFF - is reversed - device state is $state.device."
    } else {
        state.device = false
        parent?.componentOff(this.device)
        if (txtEnable) log.info "${device.displayName} switch turned OFF - is NOT reversed - device state is $state.device."
    }
    autotoggle()
}

def autotoggle() {
    if (logEnable) log.debug "autotoggle: AutoOffOnFired prerun is $state.autoOffOnFired"
    if (!state.autoOffOnFired == true) {
        if (logEnable) log.debug "autotoggle: Auto-Off-OnFired innotloop is $state.autoOffOnFired"
        if (autoOffOn.toInteger() > 0) {
            if (logEnable) log.debug "${device.displayName} will toggle in $autoOffOn seconds."
            runIn(autoOffOn.toInteger(), toggle)
            state.autoOffOnFired = true
            if (logEnable) log.debug "autotoggle: Auto-Off-OnFired afterrun is $autoOffOnFired"
        }
    }
}

def toggle() {
    if (reversed) {
        if (state.device == true) {
            on()
        } else {
            off()
        }
    } else {
        if (!reversed) {
            if (state.device == false) {
                on()
            } else {
                off()
            }
        }
    }
}

def updated(){
    log.info "${device.displayName} updated..."
    log.warn "${device.displayName} debug logging is: ${logEnable == true}"
    log.warn "${device.displayName} description logging is: ${txtEnable == true}"
    initialize()
}

def installed() {
    initialize()
}

def readCurrentIntoLog() {
    if (txtEnable) log.info "Read Current button pushed."
    if (txtEnable) log.info "Read Current: device state is $state.device."
    if (txtEnable) log.info "Read Current: device inverted? $reversed."
    if (logEnable) log.info "Read Current: Auto-Off-OnFired is $state.autoOffOnFired"
    if (autoOffOn.toInteger() > 0) {
        if (txtEnable) log.info "Read Current: AutoOffOn will toggle in $autoOffOn seconds"
    } else {
        if (txtEnable) log.info "Read Current: AutoOffOn will toggle is disabled"
    }
    if (!logEnable) log.info "Read Current: Debug must be enabled to see realtime switchvalue in Log. This has performance impact."
    if (logEnable) {
        pauseExecution(500)
        switchvalue = device.currentValue("switch")
        if (txtEnable) log.warn "Read Current: switchvalue is $switchvalue"
    }
}

def initialize() {
    if (!reversed) {
        if (txtEnable) log.info "INITIALIZE button pushed."
        sendEvent(name: "switch", value: "off", isStateChange: true)
        state.device = false
        state.autoOffOnFired = false
        if (txtEnable) log.info "${device.displayName} is initialized and Not Reversed"
        if (txtEnable) log.info "Initialized: device is Not Reversed (normal)"
        if (txtEnable) log.info "Initialized: device state is $state.device"
        if (txtEnable) log.info "Initialized: switch is Off"
        if (logEnable) log.info "Initialized: AutoOffOnFired is $state.autoOffOnFired"
        if (autoOffOn.toInteger() > 0) {
            if (txtEnable) log.info "Initialized: AutoOffOn is set to toggle in $autoOffOn seconds"
        } else {
            if (txtEnable) log.info "Initialized: AutoOffOn will not toggle"
        }
    } else {
        if (txtEnable) log.info "INITIALIZE button pushed."
        sendEvent(name: "switch", value: "on", isStateChange: true)
        state.device = false
        state.autoOffOnFired = false
        if (txtEnable) log.info "${device.displayName} is initialized and is Reversed"
        if (txtEnable) log.info "Initialized: device is REVERSED"
        if (txtEnable) log.info "Initialized: device state is $state.device"
        if (txtEnable) log.info "Initialized: switch is On"
        if (logEnable) log.info "Initialized: AutoOffOnFired is $state.autoOffOnFired"
        if (autoOffOn.toInteger() > 0) {
            if (txtEnable) log.info "Initialized: AutoOffOn is set to toggle in $autoOffOn seconds"
        } else {
            if (txtEnable) log.info "Initialized: AutoOffOn will not toggle"
        }
    }
}