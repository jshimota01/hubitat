/*
 * THIPL Room State Aggregator Child
 * Aggregates selected THIPL sensors for a specific room/floor/house and outputs to a Virtual THIPL Room State Aggregator Driver device.
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2026-06-01    jshimota      0.0.1       Initial release as THIP Averager Child
 *      2026-08-01    Gemini        0.0.2       Renamed, added Hubitat room selector dropdown, and integrated automatic room ID assignment for virtual devices
 *      2026-08-01    Gemini        0.0.3       Implemented standardized dynamic logging framework
 *      2026-08-01    Gemini        0.0.5       Added capability status sync to enable/disable room driver capabilities dynamically
 *      2026-08-01    Gemini        0.0.6       Fixed iconUrl definition compilation issue
 *      2026-08-01    Gemini        0.1.2       Utilized app.getRooms() for native hub room querying
 *      2026-08-01    Gemini        0.1.3       Automated dynamic naming formats for app instance, child device name, and child device label
 *      2026-08-01    Gemini        0.1.4       Enforced programmatic app.updateLabel() on room selection to guarantee auto-population
 *      2026-08-01    Gemini        0.1.5       Locked Virtual Omni device creation directly to app child instance and simplified Section 3 display
 *      2026-08-01    Gemini        0.1.6       Enhanced child device creation handling, fallbacks, and creation logging
 *      2026-08-02    Gemini        0.1.7       Updated app name references to ATHIP Room Averager Child
 *      2026-08-02    Gemini        0.1.8       Updated child device creation call to reference Virtual ATHIP Omni Sensors driver
 *      2026-08-02    Gemini        0.2.0       Corrected setDeviceRoom execution target to location.setDeviceRoom(deviceId, roomId)
 *      2026-08-02    Gemini        0.3.0       Converted Energy capability/attributes/handlers to Power throughout app
 *      2026-08-02    Gemini        0.3.1       Enhanced room assignment with explicit Long conversion, logging, and property map room passing
 *      2026-08-02    Gemini        0.3.2       Fixed String formatting issue in line 211 logging output
 *      2026-08-03    Gemini        0.4.0       Renamed suite to THIPL Room State Aggregator Child
 *      2026-08-03    Gemini        0.4.1       Added lightHandler method and capability sync for light counter tracking
 *      2026-08-03    Gemini        0.4.2       Updated addChildDevice target driver name to Virtual THIPL Room State Aggregator Driver and added null/empty safety checks
 *		2026-08-04	  jshimota		0.4.3		Possible Runaway problem with light counter
 *		2026-08-04	  Gemini		0.4.4		Added device counts for all sensor types and total configured lights
 *		2026-08-04	  Gemini		0.4.5		Implemented recursive/upward device counting for multi-tier THIPL aggregators
 *      2026-08-04    jshimota		0.4.6		attempt to change to lights not switches to help alexa detect sensors.
 * 		2026-08-09	  jshimota		0.4.7		Fixed the namespace problem 
 *
 */

static String version() { return '0.4.7' }

definition(
    name: "THIPL Room State Aggregator Child",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Aggregates selected THIPL sensors for a specific room and outputs to a Virtual THIPL Room State Aggregator device.",
    category: "Convenience",
    parent: "jshimota:THIPL Room State Aggregator Parent",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "THIPL Room State Aggregator Configuration", install: true, uninstall: true) {
        
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
            String defaultAppLabel = "THIPL Aggregator for ${activeRoomName}"
            String defaultDeviceName = "${activeRoomName} THIPL Room State Device"
            String defaultDeviceLabel = "${activeRoomName} THIPL Aggregator"

            if (!app.label || app.label == "THIPL Room State Aggregator Child" || app.label.startsWith("THIPL State Aggregator for ")) {
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

            section("<b>Step 4: Select Source Room Sensors to Aggregate</b>") {
                input name: "tempSensors", type: "capability.temperatureMeasurement", title: "Temperature Sensors:", multiple: true, required: false, submitOnChange: true
                input name: "humidSensors", type: "capability.relativeHumidityMeasurement", title: "Humidity Sensors:", multiple: true, required: false, submitOnChange: true
                input name: "luxSensors", type: "capability.illuminanceMeasurement", title: "Illuminance Sensors:", multiple: true, required: false, submitOnChange: true
                input name: "powerSensors", type: "capability.powerMeter", title: "Power Sensors:", multiple: true, required: false, submitOnChange: true
                input name: "lightSensors", type: "capability.light", title: "Select Room Lights / Switches:", multiple: true, required: false, submitOnChange: true  // modified from switch to light
                // input name: "lightSensors", type: "capability.switch", title: "Select Room Lights / Switches:", multiple: true, required: false, submitOnChange: true  // modified from switch to light
                input name: "aggregationType", type: "enum", title: "Power Calculation Mode:", options: ["avg": "Average (Room Level)", "sum": "Sum Total (Floor/House Level)"], defaultValue: "avg", required: true
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

private Long getRoomIdByName(String roomName) {
    if (!roomName) return null
    try {
        def hubRooms = app.getRooms()
        def matchedRoom = hubRooms?.find { room -> room.name.trim().equalsIgnoreCase(roomName.trim()) }
        if (matchedRoom) {
            logDebug "Found matching room '${matchedRoom.name}' with ID: ${matchedRoom.id}"
            return matchedRoom.id.toLong()
        } else {
            logWarn "No matching room found in Hubitat for name '${roomName}'"
        }
    } catch (Exception e) {
        logWarn "Could not locate room ID for '${roomName}': ${e.message}"
    }
    return null
}

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
}

private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "${app.label ?: 'THIPL Room State Aggregator Child'}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
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
        String expectedDeviceName = "${targetRoom} THIPL Room State Device"
        String expectedDeviceLabel = "${targetRoom} THIPL Aggregator"
        Long roomId = getRoomIdByName(targetRoom)
        
        def childDevice = getChildDevice("${app.id}")

        if (!childDevice) {
            logInfo "Creating Virtual THIPL Room State Aggregator Child Device (${expectedDeviceLabel})..."
            
            Map devProps = [
                name: expectedDeviceName, 
                label: expectedDeviceLabel
            ]
            
            if (roomId != null) {
                devProps.roomId = roomId
            }

            try {
                childDevice = addChildDevice(
                    "hubitat", 
                    "Virtual THIPL Room State Aggregator Driver", 
                    "${app.id}", 
                    devProps
                )
                if (roomId != null) {
                    logInfo "Successfully created child device (${expectedDeviceLabel}) in room '${targetRoom}' (ID: ${roomId})"
                }
            } catch (Exception e) {
                logWarn "Could not create child device under 'hubitat' namespace: ${e.message}"
            }
        } else {
            logInfo "Updating existing Virtual THIPL Room State Aggregator Child Device label to: ${expectedDeviceLabel}"
            childDevice.name = expectedDeviceName
            childDevice.label = expectedDeviceLabel
        }

        if (!childDevice) {
            logError "Failed to locate or create child device. Please ensure the driver 'Virtual THIPL Room State Aggregator Driver' is installed in Hubitat's 'Drivers Code' section."
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
        subscribe(tempSensors, "tempDeviceCount", tempHandler)
        calculateAndSetTemp()
    } else {
        target.disableCapability("temperature")
    }

    if (humidSensors && humidSensors.size() > 0) {
        subscribe(humidSensors, "humidity", humidHandler)
        subscribe(humidSensors, "humidityDeviceCount", humidHandler)
        calculateAndSetHumidity()
    } else {
        target.disableCapability("humidity")
    }

    if (luxSensors && luxSensors.size() > 0) {
        subscribe(luxSensors, "illuminance", luxHandler)
        subscribe(luxSensors, "illuminanceDeviceCount", luxHandler)
        calculateAndSetLux()
    } else {
        target.disableCapability("illuminance")
    }

    if (powerSensors && powerSensors.size() > 0) {
        subscribe(powerSensors, "power", powerHandler)
        subscribe(powerSensors, "powerDeviceCount", powerHandler)
        calculateAndSetPower()
    } else {
        target.disableCapability("power")
    }

    if (lightSensors && lightSensors.size() > 0) {
        subscribe(lightSensors, "switch", lightHandler)
        subscribe(lightSensors, "lightsOnCount", lightHandler)
        subscribe(lightSensors, "lightDeviceCount", lightHandler)
        calculateAndSetLights()
    } else {
        target.disableCapability("switch")
    }
}

def tempHandler(evt) {
    logDebug "Temperature event triggered by ${evt.displayName}: ${evt.name} = ${evt.value}"
    calculateAndSetTemp()
}

def humidHandler(evt) {
    logDebug "Humidity event triggered by ${evt.displayName}: ${evt.name} = ${evt.value}"
    calculateAndSetHumidity()
}

def luxHandler(evt) {
    logDebug "Illuminance event triggered by ${evt.displayName}: ${evt.name} = ${evt.value}"
    calculateAndSetLux()
}

def powerHandler(evt) {
    logDebug "Power event triggered by ${evt.displayName}: ${evt.name} = ${evt.value}"
    calculateAndSetPower()
}

def lightHandler(evt) {
    def target = getTargetDevice()
    if (target && evt.deviceId == target.deviceId) {
        return // Ignore state changes coming from the aggregator driver itself
    }
    logDebug "Light switch event triggered by ${evt.displayName}: ${evt.name} = ${evt.value}"
    calculateAndSetLights()
}

// Helper method to count underlying physical sensors or aggregate upward child device counts
private int calculateRollupDeviceCount(sensorList, String countAttributeName) {
    if (!sensorList) return 0
    int totalCount = 0
    sensorList.each { dev ->
        if (dev.hasAttribute(countAttributeName)) {
            def childCount = dev.currentValue(countAttributeName)
            totalCount += (childCount != null ? childCount.toInteger() : 1)
        } else {
            totalCount += 1
        }
    }
    return totalCount
}

private void calculateAndSetTemp() {
    def target = getTargetDevice()
    if (!target || !tempSensors) return
    
    int totalDevices = calculateRollupDeviceCount(tempSensors, "tempDeviceCount")
    target.setTempDeviceCount(totalDevices)

    def validValues = tempSensors.collect { it.currentValue("temperature") }.findAll { it != null }
    if (validValues && validValues.size() > 0) {
        def avg = validValues.sum() / validValues.size()
        logDebug "Calculated Temp Average (${totalDevices} physical sensors rolled up): ${avg}"
        target.setTemperature(avg)
    }
}

private void calculateAndSetHumidity() {
    def target = getTargetDevice()
    if (!target || !humidSensors) return
    
    int totalDevices = calculateRollupDeviceCount(humidSensors, "humidityDeviceCount")
    target.setHumidityDeviceCount(totalDevices)

    def validValues = humidSensors.collect { it.currentValue("humidity") }.findAll { it != null }
    if (validValues && validValues.size() > 0) {
        def avg = validValues.sum() / validValues.size()
        logDebug "Calculated Humidity Average (${totalDevices} physical sensors rolled up): ${avg}"
        target.setRelativeHumidity(avg)
    }
}

private void calculateAndSetLux() {
    def target = getTargetDevice()
    if (!target || !luxSensors) return
    
    int totalDevices = calculateRollupDeviceCount(luxSensors, "illuminanceDeviceCount")
    target.setIlluminanceDeviceCount(totalDevices)

    def validValues = luxSensors.collect { it.currentValue("illuminance") }.findAll { it != null }
    if (validValues && validValues.size() > 0) {
        def avg = validValues.sum() / validValues.size()
        logDebug "Calculated Lux Average (${totalDevices} physical sensors rolled up): ${avg}"
        target.setIlluminance(avg)
    }
}

private void calculateAndSetPower() {
    def target = getTargetDevice()
    if (!target || !powerSensors) return
    
    int totalDevices = calculateRollupDeviceCount(powerSensors, "powerDeviceCount")
    target.setPowerDeviceCount(totalDevices)

    def validValues = powerSensors.collect { it.currentValue("power") }.findAll { it != null }
    if (validValues && validValues.size() > 0) {
        def result = (settings.aggregationType == "sum") ? validValues.sum() : (validValues.sum() / validValues.size())
        logDebug "Calculated Power (${settings.aggregationType ?: 'avg'}) (${totalDevices} physical sensors rolled up): ${result}"
        target.setPower(result)
    }
}

private void calculateAndSetLights() {
    def target = getTargetDevice()
    if (!target || !lightSensors) return

    // Exclude self-references
    def filteredSensors = lightSensors.findAll { it.deviceId != target.deviceId }
    
    int totalConfiguredLights = calculateRollupDeviceCount(filteredSensors, "lightDeviceCount")
    target.setLightDeviceCount(totalConfiguredLights)

    int countOn = 0
    filteredSensors.each { dev ->
        if (dev.hasAttribute("lightsOnCount")) {
            def activeChildCount = dev.currentValue("lightsOnCount")
            countOn += (activeChildCount != null ? activeChildCount.toInteger() : 0)
        } else {
            if (dev.currentValue("switch") == "on") countOn += 1
        }
    }

    logDebug "Calculated Active Light Count (${countOn}/${totalConfiguredLights} rolled up): ${countOn}"
    target.setLightsOnCount(countOn)
}