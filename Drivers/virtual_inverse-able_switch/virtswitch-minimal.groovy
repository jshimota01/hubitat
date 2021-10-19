/*
 * Virtual Inverse-able w-autooff (Minimal) Switch
 * 
 */
metadata {
    definition(
            name: "Virtual Inverse-able w-autooff Switch",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: ""
    ) {
        capability "Actuator"
        capability "Switch"
        capability "Initialize"
		command "readCurrent"
    }

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
   }
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def on() {
    if (txtEnable) log.info "ON button pushed."
    sendEvent(name: "switch", value: "on", isStateChange: true)
    switchvalue = device.currentValue("switch", true)
    state.device = true
    if (txtEnable) log.info "${device.displayName} turned ON and device state is $state.device."
    if (txtEnable) log.info "Turned On: switchvalue is $switchvalue"
}

def off() {
    if (txtEnable) log.info "OFF button pushed."
    sendEvent(name: "switch", value: "off", isStateChange: true)
    switchvalue = device.currentValue("switch", true)
    state.device = false
    if (txtEnable) log.info "${device.displayName} turned OFF and device state is $state.device."
    if (txtEnable) log.info "Turned Off: switchvalue is $switchvalue"
}

def installed() {
    initialize()
}

def readCurrent() {
    if (txtEnable) log.info "Read Current button pushed."
    if (txtEnable) log.info "Read Current: device state is $state.device."
    if (txtEnable) log.info "Read Current: switchvalue is $switchvalue"
}

def initialize() {
    if (txtEnable) log.info "INITIALIZE button pushed."
    sendEvent(name: "switch", value: "off", isStateChange: true)
    switchvalue = device.currentValue("switch", true)
    state.device = false
    if (txtEnable) log.info "${device.displayName} is initialized and device state is $state.device."
    if (txtEnable) log.info "Initialized: switchvalue is $switchvalue"
}
