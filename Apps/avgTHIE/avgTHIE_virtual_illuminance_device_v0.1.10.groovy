static String version() { return '0.1.10' } // Bumped version for the architecture update

metadata {
    definition (name: "avgTHIE virtual illuminance device", namespace: "jshimota", author: "James Shimota") {
        capability "Illuminance Measurement"
        capability "Sensor" 
		
        command "setIlluminance", [[name:"lux*", type: "DECIMAL", description: "Illuminance value in Lux"]]
		command "clearAllAttributes"
		
    }
    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "luxOffset", type: "decimal", title: "Lux Offset", defaultValue: 0.0, description: "Adjust reading by this amount"
		input name: "luxThreshold", type: "number", title: "Minimum Lux Delta to Report (to impact Jitter)", defaultValue: 2, description: "Only send events if lux changes by at least this amount (unless reaching 0)"
    }
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("dbgEnable", [value: false, type: "bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setIlluminance(0.0)
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

def setIlluminance(lux) {
    if (lux == null) return
    
    float l = (lux instanceof Number) ? lux.floatValue() : lux.toFloat()
    float offset = (settings.luxOffset != null) ? settings.luxOffset.floatValue() : 0.0f
    l += offset
    
    int finalLux = (l < 0.0f) ? 0 : (int)(l + 0.5f)

    // Retrieve previous value
    Object prevVal = device.currentValue("illuminance")
    int currentLux = (prevVal != null) ? prevVal.intValue() : -1

    int threshold = (settings.luxThreshold != null) ? settings.luxThreshold.intValue() : 0

    // Always report if it reaches exactly 0, or if the delta meets/exceeds the threshold
    if (currentLux == -1 || finalLux == 0 || Math.abs(finalLux - currentLux) >= threshold) {
        sendIfChanged("illuminance", finalLux, "lx")
    } else {
        if (dbgEnable) log.debug "${device.displayName}: Ignored jitter change from ${currentLux} to ${finalLux} (threshold: ${threshold})"
    }
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