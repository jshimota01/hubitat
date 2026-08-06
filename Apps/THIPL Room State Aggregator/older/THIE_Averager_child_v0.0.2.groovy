/*
 * THIE Room Averager Child
 * Averages selected THIE sensors for a specific room and outputs to a Virtual Omni THIE device.
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2026-06-01    jshimota      0.0.1       Initial release as THIE Averager Child
 *      2026-08-01    Gemini        0.0.2       Renamed, added Hubitat room selector dropdown, and integrated automatic room ID assignment for virtual devices
 *
 */

static String version() { return '0.0.2' }

definition(
    name: "THIE Room Averager Child",
    namespace: "hubitat",
    author: "James Shimota",
    description: "Averages selected THIE sensors for a specific room and outputs to a Virtual Omni THIE device.",
    category: "Convenience",
    parent: "hubitat:THIE Room Averager Parent",
    iconUrl: "",
    iconXUrl: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    // Fetch room list dynamically from Hubitat Location API
    def roomList = location.rooms ? location.rooms.collectEntries { room -> [(room.id.toString()): room.name] } : [:]

    dynamicPage(name: "mainPage", title: "THIE Room Averager Instance", install: true, uninstall: true) {
        
        section("<b>Select Target Room</b>") {
            if (roomList) {
                input name: "selectedRoomId", type: "enum", title: "Select Hubitat Room:", options: roomList, required: true, submitOnChange: true
            } else {
                paragraph "<i>No rooms configured in Hubitat Settings -> Rooms. Please add rooms in Hubitat first or enter a label manually below.</i>"
            }
        }

        section("<b>Label App Instance</b>") {
            // Automatically suggests room name if selected
            def defaultLabel = "THIE Room Averager"
            if (selectedRoomId && roomList[selectedRoomId]) {
                defaultLabel = "${roomList[selectedRoomId]} THIE Averager"
            }
            label title: "Enter a name for this room averager instance:", required: true, defaultValue: defaultLabel
        }

        section("<b>Target Output Device</b>") {
            paragraph "Select an existing Virtual Omni THIE device or choose to create one automatically."
            input name: "createDevice", type: "bool", title: "Create a new Virtual Omni THIE child device automatically?", defaultValue: true, submitOnChange: true
            
            if (!createDevice) {
                input name: "omniDevice", type: "capability.actuator", title: "Select Virtual Omni THIE Output Device:", required: true, multiple: false
            }
        }

        section("<b>Select Source Room Sensors to Average</b>") {
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
    // Handle Virtual Device Creation bound to "Virtual Omni THIE Sensors" driver
    if (settings.createDevice) {
        def childDevice = getChildDevice(app.id)
        if (!childDevice) {
            log.info "Creating Virtual Omni THIE Child Device..."
            childDevice = addChildDevice("hubitat", "Virtual Omni THIE Sensors", app.id, [name: app.label, label: app.label])
        } else {
            childDevice.label = app.label
        }
        
        // Assign device to room in Hubitat if selected
        if (settings.selectedRoomId && childDevice) {
            childDevice.setRoomId(settings.selectedRoomId.toLong())
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

// Get target Omni Device (Child device or selected external device)
private getTargetDevice() {
    return settings.createDevice ? getChildDevice(app.id) : settings.omniDevice
}

// Event Handlers
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

// Calculation logic routing commands to Virtual Omni THIE Driver
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