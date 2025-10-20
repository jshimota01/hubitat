// Copyright 2016-2019 Hubitat Inc.  All Rights Reserved

metadata {
    definition (name: "Virtual Power Meter Sensor (Custom)", namespace: "jshimota", author: "Bruce Ravenel") {
        capability "EnergyMeter"
        capability "PowerMeter"
        capability "VoltageMeasurement"
        command "setEnergy", ["Number"]
        command "setPower", ["Number"]
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    log.warn "installed..."
    setPower(1)
    runIn(1800,logsOff)
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def parse(String description) {
}

def setEnergy(energy) {
    def descriptionText = "${device.displayName} is ${energy} energy"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "energy", value: energy, descriptionText: descriptionText)
}

// def setPower(pow) {
//    def unit = "W"
//    def descriptionText = "${device.displayName} is ${pow}${unit}"
//    if (txtEnable) log.info "${descriptionText}"
//    sendEvent(name: "power", value: pow, descriptionText: descriptionText, unit: unit)
// }

def setPower(power) {
    def descriptionText = "${device.displayName} is ${power} power"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "power", value: power, descriptionText: descriptionText)
}