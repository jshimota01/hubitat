/*
 * avgTHIE virtual power device
 */

static String version() { return '0.1.9' } // Bumped version for refresh routing

metadata {
    definition (name: "avgTHIE virtual power device", namespace: "jshimota", author: "James Shimota") {
        capability "Power Meter"
        capability "Voltage Measurement"
        capability "Sensor"
        
        command "setPower", [[name:"power*", type: "DECIMAL", description: "Instantaneous Power in Watts"]]
        command "setVoltage", [[name:"voltage*", type: "DECIMAL", description: "Voltage in Volts"]]
		command "clearAllAttributes"
    }
    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("dbgEnable", [value: false, type: "bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setPower(0.0)
    setVoltage(120.0)
    runIn(1800, "logsOff")
}

def updated() {
    log.info "${device.displayName} updated..."
    unschedule("logsOff")
    if (dbgEnable) runIn(1800, "logsOff")
}

void clearAllAttributes() {
    if (dbgEnable) log.debug "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
}

def setPower(power) {
    if (power == null) return
    
    float rawVal = (power instanceof Number) ? power.floatValue() : power.toFloat()
    
    float val = (float)(Math.round(rawVal * 100.0f) / 100.0f)
    
    sendIfChanged("power", val, "W")
}

def setVoltage(volts) {
    if (volts == null) return
    
    float rawVal = (volts instanceof Number) ? volts.floatValue() : volts.toFloat()
    
    float val = (float)(Math.round(rawVal * 10.0f) / 10.0f)
    
    sendIfChanged("voltage", val, "V")
}

private void sendIfChanged(String name, Object value, String unit) {
    Object currentValue = device.currentValue(name)
    
    if (currentValue == value) {
        if (dbgEnable) log.debug "${device.displayName}: ${name} value (${value}) has not changed. Skipping log and event generation."
        return
    }

    String devName = device.displayName
    String descriptionText = "${devName} ${name} is ${value} ${unit}"
    
    if (txtEnable) log.info descriptionText
    
    sendEvent(name: name, value: value, unit: unit, descriptionText: descriptionText, isStateChange: true)
}