definition(
    name: "THIE Averager Child",
    namespace: "hubitat",
    author: "James Shimota",
    description: "Averages selected THIE sensors and outputs to a Virtual Omni THIE device.",
    category: "Convenience",
    parent: "hubitat:THIE Averager Parent",
    iconUrl: "",
    iconXUrl: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "THIE Sensor Averager Instance", install: true, uninstall: true) {
        section("<b>Label App Instance</b>") {
            label title: "Enter a name for this averager instance:", required: true, defaultValue: "THIE Averager"
        }

        section("<b>Target Output Device</b>") {
            paragraph "Select an existing Virtual Omni THIE device or choose to create one automatically."
            input name: "createDevice", type: "bool", title: "Create a new Virtual Omni THIE child device automatically?", defaultValue: false, submitOnChange: true
            
            if (!createDevice) {
                input name: "omniDevice", type: "capability.actuator", title: "Select Virtual Omni THIE Output Device:", required: true, multiple: false
            }
        }

        section("<b>Select Source Sensors to Average</b>") {
            input name: "tempSensors", type: "capability.temperatureMeasurement", title: "Temperature Sensors:", multiple: true, required: false, submitOnChange: true
            input name: "humidSensors", type: "capability.relativeHumidityMeasurement", title: "Humidity Sensors:", multiple: true, required: false, submitOnChange: true
            input name: "luxSensors", type: "capability.illuminanceMeasurement", title: "Illuminance Sensors:", multiple: true, required: false, submitOnChange: true
            input name: "energySensors", type: "capability.energyMeter", title: "Energy Sensors:", multiple: true, required: false, submitOnChange: true
        }

        section("<b>Options</b>") {
            input name: "logEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
        }
    }
}

def installed() {
    log.info "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.info "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    // Handle Virtual Device Creation if selected
    if (settings.createDevice) {
        def childDevice = getChildDevice(app.id)
        if (!childDevice) {
            log.info "Creating Virtual Omni THIE Child Device..."
            childDevice = addChildDevice("hubitat", "Virtual Omni THIE Sensors", app.id, [name: app.label, label: app.label])
        }
    }

    // Subscribe to events
    if (tempSensors) {
        subscribe(tempSensors, "temperature", tempHandler)
        calculateAndSetTemp()
    }
    if (humidSensors) {
        subscribe(humidSensors, "humidity", humidHandler)
        calculateAndSetHumidity()
    }
    if (luxSensors) {
        subscribe(luxSensors, "illuminance", luxHandler)
        calculateAndSetLux()
    }
    if (energySensors) {
        subscribe(energySensors, "energy", energyHandler)
        calculateAndSetEnergy()
    }
}

// Get the correct target Omni Device (Child device or selected external device)
private getTargetDevice() {
    return settings.createDevice ? getChildDevice(app.id) : settings.omniDevice
}

// Handlers
def tempHandler(evt) {
    if (logEnable) log.debug "Temperature event triggered by ${evt.displayName}: ${evt.value}"
    calculateAndSetTemp()
}

def humidHandler(evt) {
    if (logEnable) log.debug "Humidity event triggered by ${evt.displayName}: ${evt.value}"
    calculateAndSetHumidity()
}

def luxHandler(evt) {
    if (logEnable) log.debug "Illuminance event triggered by ${evt.displayName}: ${evt.value}"
    calculateAndSetLux()
}

def energyHandler(evt) {
    if (logEnable) log.debug "Energy event triggered by ${evt.displayName}: ${evt.value}"
    calculateAndSetEnergy()
}

// Calculation logic
private calculateAndSetTemp() {
    def target = getTargetDevice()
    if (!target || !tempSensors) return
    
    def validValues = tempSensors.collect { it.currentValue("temperature") }.findAll { it != null }
    if (validValues) {
        def avg = validValues.sum() / validValues.size()
        if (logEnable) log.debug "Calculated Temp Average: ${avg}"
        target.setTemperature(avg)
    }
}

private calculateAndSetHumidity() {
    def target = getTargetDevice()
    if (!target || !humidSensors) return
    
    def validValues = humidSensors.collect { it.currentValue("humidity") }.findAll { it != null }
    if (validValues) {
        def avg = validValues.sum() / validValues.size()
        if (logEnable) log.debug "Calculated Humidity Average: ${avg}"
        target.setRelativeHumidity(avg)
    }
}

private calculateAndSetLux() {
    def target = getTargetDevice()
    if (!target || !luxSensors) return
    
    def validValues = luxSensors.collect { it.currentValue("illuminance") }.findAll { it != null }
    if (validValues) {
        def avg = validValues.sum() / validValues.size()
        if (logEnable) log.debug "Calculated Lux Average: ${avg}"
        target.setIlluminance(avg)
    }
}

private calculateAndSetEnergy() {
    def target = getTargetDevice()
    if (!target || !energySensors) return
    
    def validValues = energySensors.collect { it.currentValue("energy") }.findAll { it != null }
    if (validValues) {
        def avg = validValues.sum() / validValues.size()
        if (logEnable) log.debug "Calculated Energy Average: ${avg}"
        target.setEnergy(avg)
    }
}