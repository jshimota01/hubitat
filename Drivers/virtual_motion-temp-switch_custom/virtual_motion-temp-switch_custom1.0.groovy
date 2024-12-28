// Copyright 2016-2019 Hubitat Inc.  All Rights Reserved

metadata {
    definition (name: "Virtual Motion-Temp Sensor (Custom)", namespace: "jshimota", author: "Bruce Ravenel") {
        capability "Motion Sensor"
        capability "Temperature Measurement"
        command "motionActive"
        command "motionInactive"
        command "setTemperature", ["Number"]
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
    motionInactive()
    setTemperature(100)
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

def motionActive() {
    def descriptionText = "${device.displayName} is active"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "motion", value: "active", descriptionText: descriptionText)
}

def motionInactive() {
    def descriptionText = "${device.displayName} is inactive"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "motion", value: "inactive", descriptionText: descriptionText)
}
def setTemperature(temp) {
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} is ${temp}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "temperature", value: temp, descriptionText: descriptionText, unit: unit)
}