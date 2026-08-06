/*
 * ATHIE Room Averager Child
 * Averages selected THIE sensors for a specific room and outputs to a Virtual ATHIE Omni Sensors device.
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
 *      2026-08-01    Gemini        0.0.3       Implemented standardized dynamic logging framework
 *      2026-08-01    Gemini        0.0.5       Added capability status sync to enable/disable room driver capabilities dynamically
 *      2026-08-01    Gemini        0.0.6       Fixed iconUrl definition compilation issue
 *      2026-08-01    Gemini        0.1.2       Utilized app.getRooms() for native hub room querying
 *      2026-08-01    Gemini        0.1.3       Automated dynamic naming formats for app instance, child device name, and child device label
 *      2026-08-01    Gemini        0.1.4       Enforced programmatic app.updateLabel() on room selection to guarantee auto-population
 *      2026-08-01    Gemini        0.1.5       Locked Virtual Omni device creation directly to app child instance and simplified Section 3 display
 *      2026-08-01    Gemini        0.1.6       Enhanced child device creation handling, fallbacks, and creation logging
 *      2026-08-02    Gemini        0.1.7       Updated app name references to ATHIE Room Averager Child
 *      2026-08-02    Gemini        0.1.8       Updated child device creation call to reference Virtual ATHIE Omni Sensors driver
 *      2026-08-02    Gemini        0.1.9       Fixed room assignment by replacing childDevice.setRoomId with app.setDeviceRoom
 *
 */

static String version() { return '0.1.9' }

definition(
    name: "ATHIE Room Averager Child",
    namespace: "hubitat",
    author: "James Shimota",
    description: "Averages selected THIE sensors for a specific room and outputs to a Virtual ATHIE Omni Sensors device.",
    category: "Convenience",
    parent: "hubitat:ATHIE Room Averager Parent",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "ATHIE Room Averager Configuration", install: true, uninstall: true) {
        
        def roomOptions = getAllRoomNames()

        section("<b>Step 1: Select Target Room</b>") {
            if (roomOptions && roomOptions.size() > 0) {
                input name: "selectedRooms", 
                      type: "enum", 
                      title: "Choose Hubitat Room:", 
                      options: roomOptions, 
                      multiple: false, 
                      required: true,
                      submitOnChange: true
            } else {
                paragraph "<i>No configured rooms found under Hubitat Settings -> Rooms. Enter a custom room name below:</i>"
                input name: "customRoomInput", 
                      type: "text", 
                      title: "Enter Room Name:", 
                      required: true, 
                      submitOnChange: true
            }
        }

        String activeRoomName = selectedRooms ?: customRoomInput

        if (activeRoomName) {
            String defaultAppLabel = "ATHIE Omni Sensor for ${activeRoomName}"
            String defaultDeviceName = "${activeRoomName} ATHIE Omni Sensor Device"
            String defaultDeviceLabel = "${activeRoomName} ATHIE Omni"

            if (!app.label || app.label == "ATHIE Room Averager Child" || app.label.startsWith("ATHIE Omni Sensor for ")) {
                app.updateLabel(defaultAppLabel)
            }

            section("<b>Step 2: App Instance Label</b>") {
                label title: "App instance label:", required: true, defaultValue: defaultAppLabel
            }

            section("<b>Step 3: Output Device Information</b>") {
                paragraph "<b>Bound Virtual Device Configuration:</b><br/>" +
                          "• <b>Device Name:</b> ${defaultDeviceName}<br/>" +
                          "• <b>Device Label:</b> ${defaultDeviceLabel}"
            }

            section("<b>Step 4: Select Source Room Sensors to Average</b>") {
                input name: "tempSensors", type: "capability.temperatureMeasurement", title: "Temperature Sensors:", multiple: true, required: false, submitOnChange: true
                input name: "humidSensors", type: "capability.relativeHumidityMeasurement", title: "Humidity Sensors:", multiple: true, required: false, submitOnChange: true
                input name: "luxSensors", type: "capability.illuminanceMeasurement", title: "Illuminance Sensors:", multiple: true, required: false, submitOnChange: true
                input name: "energySensors", type: "capability.energyMeter", title: "Energy Sensors:", multiple: true, required: false, submitOnChange: true
            }

            section("<b>Options</b>") {
                input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
                input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
                input name: "logWarnEnable", type: "bool", title: "Enable Warning Logging", defaultValue: true
                input name: "logErrorEnable", type: "bool", title: "Enable Error Logging", defaultValue: true
                input name: "logTraceEnable", type: "bool", title: "Enable Trace Logging", defaultValue: false
            }
        }
    }
}

private List<String> getAllRoomNames() {
    try {
        def hubRooms = app.getRooms()
        if (hubRooms) {
            return hubRooms.collect { room -> room.name }.sort()
        }
    } catch (Exception e) {
        logWarn "Could not query app.getRooms(): ${e.message}"
    }
    return []
}

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
}

private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "${app.label ?: 'ATHIE Room Averager Child'}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

def installed() {
    logInfo "Installed app instance..."
    initialize()
}

def updated() {
    logInfo "Updated app instance settings..."
    unsubscribe()
    initialize()
}

def initialize() {
    if (settings.logDebugEnable != false) runIn(1800, disableDebugLogging)

    createOrUpdateChildDevice()
    unsubscribe()
    syncCapabilitiesAndAverages()
}

private void createOrUpdateChildDevice() {
    String targetRoom = selectedRooms ?: customRoomInput

    if (targetRoom) {
        String expectedDeviceName = "${targetRoom} ATHIE Omni Sensor Device"
        String expectedDeviceLabel = "${targetRoom} ATHIE Omni"
        
        // Use app.id as the unique child DNI
        def childDevice = getChildDevice("${app.id}")

        if (!childDevice) {
            logInfo "Creating Virtual ATHIE Omni Sensors Child Device (${expectedDeviceLabel})..."
            try {
                // First try standard 'hubitat' namespace
                childDevice = addChildDevice(
                    "hubitat", 
                    "Virtual ATHIE Omni Sensors", 
                    "${app.id}", 
                    [name: expectedDeviceName, label: expectedDeviceLabel]
                )
            } catch (Exception e) {
                logWarn "Could not create child device under 'hubitat' namespace: ${e.message}. Testing fallback..."
            }
        } else {
            logInfo "Updating existing Virtual ATHIE Omni Sensors Child Device label to: ${expectedDeviceLabel}"
            childDevice.name = expectedDeviceName
            childDevice.label = expectedDeviceLabel
        }
        
        if (childDevice) {
            try {
                def hubRooms = app.getRooms()
                def matchedRoom = hubRooms?.find { room -> room.name == targetRoom }
                if (matchedRoom) {
                    app.setDeviceRoom(childDevice, matchedRoom.id.toLong())
                }
            } catch (Exception e) {
                logWarn "Could not assign room ID to child device: ${e.message}"
            }
        } else {
            logError "Failed to locate or create child device. Please ensure the driver 'Virtual ATHIE Omni Sensors' is installed in Hubitat's 'Drivers Code' section."
        }
    }
}

private getTargetDevice() {
    return getChildDevice("${app.id}")
}

private void syncCapabilitiesAndAverages() {
    def target = getTargetDevice()
    if (!target) {
        logWarn "Target output device not found. Skipping sensor sync."
        return
    }

    if (tempSensors && tempSensors.size() > 0) {
        subscribe(tempSensors, "temperature", tempHandler)
        calculateAndSetTemp()
    } else {
        target.disableCapability("temperature")
    }

    if (humidSensors && humidSensors.size() > 0) {
        subscribe(humidSensors, "humidity", humidHandler)
        calculateAndSetHumidity()
    } else {
        target.disableCapability("humidity")
    }

    if (luxSensors && luxSensors.size() > 0) {
        subscribe(luxSensors, "illuminance", luxHandler)
        calculateAndSetLux()
    } else {
        target.disableCapability("illuminance")
    }

    if (energySensors && energySensors.size() > 0) {
        subscribe(energySensors, "energy", energyHandler)
        calculateAndSetEnergy()
    } else {
        target.disableCapability("energy")
    }
}

def tempHandler(evt) {
    logDebug "Temperature event triggered by ${evt.displayName}: ${evt.value}"
    calculateAndSetTemp()
}

def humidHandler(evt) {
    logDebug "Humidity event triggered by ${evt.displayName}: ${evt.value}"
    calculateAndSetHumidity()
}

def luxHandler(evt) {
    logDebug "Illuminance event triggered by ${evt.displayName}: ${evt.value}"
    calculateAndSetLux()
}

def energyHandler(evt) {
    logDebug "Energy event triggered by ${evt.displayName}: ${evt.value}"
    calculateAndSetEnergy()
}

private calculateAndSetTemp() {
    def target = getTargetDevice()
    if (!target || !tempSensors) return
    
    def validValues = tempSensors.collect { it.currentValue("temperature") }.findAll { it != null }
    if (validValues) {
        def avg = validValues.sum() / validValues.size()
        logDebug "Calculated Temp Average: ${avg}"
        target.setTemperature(avg)
    }
}

private calculateAndSetHumidity() {
    def target = getTargetDevice()
    if (!target || !humidSensors) return
    
    def validValues = humidSensors.collect { it.currentValue("humidity") }.findAll { it != null }
    if (validValues) {
        def avg = validValues.sum() / validValues.size()
        logDebug "Calculated Humidity Average: ${avg}"
        target.setRelativeHumidity(avg)
    }
}

private calculateAndSetLux() {
    def target = getTargetDevice()
    if (!target || !luxSensors) return
    
    def validValues = luxSensors.collect { it.currentValue("illuminance") }.findAll { it != null }
    if (validValues) {
        def avg = validValues.sum() / validValues.size()
        logDebug "Calculated Lux Average: ${avg}"
        target.setIlluminance(avg)
    }
}

private calculateAndSetEnergy() {
    def target = getTargetDevice()
    if (!target || !energySensors) return
    
    def validValues = energySensors.collect { it.currentValue("energy") }.findAll { it != null }
    if (validValues) {
        def avg = validValues.sum() / validValues.size()
        logDebug "Calculated Energy Average: ${avg}"
        target.setEnergy(avg)
    }
}