/*
 * avgTHIE virtual humidity device
 */

static String version() { return '0.1.7' } // Bumped version for refresh routing

metadata {
    definition (name: "avgTHIE virtual humidity device", namespace: "jshimota", author: "James Shimota") { 
        capability "Relative Humidity Measurement" 
        capability "Sensor"  
        capability "Refresh" 
        
        command "setRelativeHumidity", [[name:"humidity*", type: "DECIMAL", description: "Humidity percentage (0-100)"]] 
		command "clearAllAttributes"
    }
    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true 
        input name: "humidOffset", type: "decimal", title: "Humidity Offset", defaultValue: 0.0, description: "Adjust reading by this percentage" 
    }
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..." 
    device.updateSetting("dbgEnable", [value: false, type: "bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setRelativeHumidity(50.0) 
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
 * Force-polls the parent child app to recalculate humidity immediately
 */
def refresh() {
    if (dbgEnable) log.debug "Refresh triggered. Attempting to poll parent app for an immediate update."
    
    if (parent) {
        try {
            parent.requestHumidityRefresh()
        } catch (Exception e) {
            log.error "Failed to call parent refresh hook: ${e.message}"
        }
    } else {
        log.warn "No parent application link found for this device. Refresh aborted."
    }
}

def setRelativeHumidity(humid) {
    if (humid == null) return
    
    float h = (humid instanceof Number) ? humid.floatValue() : humid.toFloat()
    
    float offset = (settings.humidOffset != null) ? settings.humidOffset.floatValue() : 0.0f
    h += offset
    
    float val = (float)(Math.round(h * 10.0f) / 10.0f)
    
    if (val < 0.0f) {
        val = 0.0f
    } else if (val > 100.0f) {
        val = 100.0f
    }

    sendIfChanged("humidity", val, "%")
}

private void sendIfChanged(String name, Object value, String unit) {
    Object currentValue = device.currentValue(name)
    
    if (currentValue == value) {
        if (dbgEnable) log.debug "${device.displayName}: ${name} value (${value}) has not changed. Skipping log and event generation."
        return
    }

    String devName = device.displayName
    String descriptionText = "${devName} ${name} is ${value}${unit}"
    
    if (txtEnable) log.info descriptionText
    
    sendEvent(name: name, value: value, unit: unit, descriptionText: descriptionText, isStateChange: true)
}