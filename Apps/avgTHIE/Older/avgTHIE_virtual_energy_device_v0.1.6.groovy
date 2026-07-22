/*
 * avgTHIE virtual energy device
 */

static String version() { return '0.1.6' } // Bumped version for refresh routing

metadata {
    definition (name: "avgTHIE virtual energy device", namespace: "jshimota", author: "James Shimota") {
        capability "Energy Meter"
        capability "Voltage Measurement"
        capability "Sensor"
        capability "Refresh" 
        
        command "setEnergy", [[name:"energy*", type: "DECIMAL", description: "Cumulative Energy in kWh"]]
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
    setEnergy(0.0)
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

/**
 * Force-polls the parent child app to recalculate energy immediately
 */
def refresh() {
    if (dbgEnable) log.debug "Refresh triggered. Attempting to poll parent app for an immediate update."
    
    if (parent) {
        try {
            parent.requestEnergyRefresh()
        } catch (Exception e) {
            log.error "Failed to call parent refresh hook: ${e.message}"
        }
    } else {
        log.warn "No parent application link found for this device. Refresh aborted."
    }
}

def setEnergy(energy) {
    if (energy == null) return
    
    float rawVal = (energy instanceof Number) ? energy.floatValue() : energy.toFloat()
    
    float val = (float)(Math.round(rawVal * 1000.0f) / 1000.0f)
    
    sendIfChanged("energy", val, "kWh")
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