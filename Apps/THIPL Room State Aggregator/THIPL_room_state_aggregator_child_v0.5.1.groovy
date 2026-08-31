/*
 * THIPL Room State Aggregator Child
 * Aggregates selected THIPL sensors for a specific room/floor/house and outputs to a Virtual THIPL Room State Aggregator Driver device.
 * Platform: Hubitat Elevation
 *
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
 *      2026-08-04    jshimota      0.4.3       Possible Runaway problem with light counter
 *      2026-08-04    Gemini        0.4.4       Added device counts for all sensor types and total configured lights
 *      2026-08-04    Gemini        0.4.5       Implemented recursive/upward device counting for multi-tier THIPL aggregators
 *      2026-08-04    jshimota      0.4.6       Attempt to change to lights not switches to help alexa detect sensors.
 *      2026-08-09    jshimota      0.4.7       Fixed the namespace problem 
 *      2026-08-09    jshimota      0.4.8       Put switches back from light capability - I guess dimmers don't have to be lights.
 *      2026-08-30    jshimota      0.5.0       Applied master template, fixed multi-tier weighted averages, symmetrical capability sync, and room re-assignment.
 *      2026-08-30    jshimota      0.5.1       Implemented delta-threshold logging suppression for high-frequency illuminance events.
 *
 */

// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '0.5.1' }
def timeStamp() { return "2026/08/30 02:03 PM" }

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
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* Styled App Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>THIPL Room State Aggregator Child</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

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
            String defaultDeviceName = "${activeRoomName} THIPL Room State Device"
            String defaultDeviceLabel = "${activeRoomName} THIPL Aggregator"

            section("<b>Step 2: App Instance Label</b>") {
                label title: "App instance label:", required: true, defaultValue: "THIPL Aggregator for ${activeRoomName}"
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
                input name: "lightSensors", type: "capability.switch", title: "Select Room Lights / Switches:", multiple: true, required: false, submitOnChange: true  
                input name: "aggregationType", type: "enum", title: "Power Calculation Mode:", options: ["avg": "Average (Room Level)", "sum": "Sum Total (Floor/House Level)"], defaultValue: "avg", required: true
            }

            /* Collapsible App Preferences & Logging Options */
            section("<b>App Preferences & Logging Options</b>", hideable: true, hidden: true) {
                input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true

                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

                input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
                input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
                input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
                input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: true, required: true
                input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
            }
        }
    }
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    String activeRoomName = selectedRooms ?: customRoomInput
    if (!activeRoomName) return

    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "THIPL Aggregator for ${activeRoomName}"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

// Settings Hash Snapshot Helper
private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn")) }
        .sort()

    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
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

// Hubitat App Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing app v${version()} (${timeStamp()})..."
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (state.lastInitializedVersion != version())

    if (settingsChanged || codeVersionChanged) {
        logInfo "Settings or code version modification detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(false)
    } else {
        logDebug "App closed without setting or version changes. Skipping re-initialization."
    }
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling app..."
    unsubscribe()
    unschedule()
}

private void initialize(Boolean isInstall = false) {
    state.lastInitializedVersion = version()

    updateAppLabel()

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }

    createOrUpdateChildDevice()
    syncCapabilitiesAndAverages()
}

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// Centralized Logging Engine
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appLabel = app.label ?: app.name ?: "App"

    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${appLabel}${lowerLevel == 'warn' ? ' WARNING' : lowerLevel == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    def val = settings[key]
    if (val == null) return defaultVal
    if (val instanceof Boolean) return val
    return val.toString().toBoolean()
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
            if (roomId != null) {
                try {
                    location.setDeviceRoom(childDevice.id, roomId)
                    logDebug "Updated existing child device room assignment to: ${targetRoom} (ID: ${roomId})"
                } catch (Exception e) {
                    logWarn "Could not update room assignment for existing child device: ${e.message}"
                }
            }
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
        if (target.hasCommand("enableCapability")) target.enableCapability("temperature")
        subscribe(tempSensors, "temperature", tempHandler)
        subscribe(tempSensors, "tempDeviceCount", tempHandler)
        calculateAndSetTemp()
    } else {
        if (target.hasCommand("disableCapability")) target.disableCapability("temperature")
    }

    if (humidSensors && humidSensors.size() > 0) {
        if (target.hasCommand("enableCapability")) target.enableCapability("humidity")
        subscribe(humidSensors, "humidity", humidHandler)
        subscribe(humidSensors, "humidityDeviceCount", humidHandler)
        calculateAndSetHumidity()
    } else {
        if (target.hasCommand("disableCapability")) target.disableCapability("humidity")
    }

    if (luxSensors && luxSensors.size() > 0) {
        if (target.hasCommand("enableCapability")) target.enableCapability("illuminance")
        subscribe(luxSensors, "illuminance", luxHandler)
        subscribe(luxSensors, "illuminanceDeviceCount", luxHandler)
        calculateAndSetLux()
    } else {
        if (target.hasCommand("disableCapability")) target.disableCapability("illuminance")
    }

    if (powerSensors && powerSensors.size() > 0) {
        if (target.hasCommand("enableCapability")) target.enableCapability("power")
        subscribe(powerSensors, "power", powerHandler)
        subscribe(powerSensors, "powerDeviceCount", powerHandler)
        calculateAndSetPower()
    } else {
        if (target.hasCommand("disableCapability")) target.disableCapability("power")
    }

    if (lightSensors && lightSensors.size() > 0) {
        if (target.hasCommand("enableCapability")) target.enableCapability("switch")
        subscribe(lightSensors, "switch", lightHandler)
        subscribe(lightSensors, "lightsOnCount", lightHandler)
        subscribe(lightSensors, "lightDeviceCount", lightHandler)
        calculateAndSetLights()
    } else {
        if (target.hasCommand("disableCapability")) target.disableCapability("switch")
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
    logTrace "Illuminance event triggered by ${evt.displayName}: ${evt.name} = ${evt.value}"
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

// Helper method to count underlying physical sensors or aggregate upward child device counts safely
private int calculateRollupDeviceCount(sensorList, String countAttributeName) {
    if (!sensorList) return 0
    int totalCount = 0
    sensorList.each { dev ->
        if (dev.hasAttribute(countAttributeName)) {
            def childCount = dev.currentValue(countAttributeName)
            if (childCount != null) {
                totalCount += childCount.toInteger()
            } else {
                logDebug "Attribute '${countAttributeName}' exists on ${dev.displayName} but value is null. Defaulting child count to 1."
                totalCount += 1
            }
        } else {
            totalCount += 1
        }
    }
    return totalCount
}

// Helper method for multi-tier weighted averages
private BigDecimal calculateWeightedAverage(sensorList, String valueAttribute, String countAttribute) {
    if (!sensorList) return null
    BigDecimal weightedSum = 0
    int totalWeight = 0

    sensorList.each { dev ->
        def val = dev.currentValue(valueAttribute)
        if (val != null) {
            int weight = 1
            if (dev.hasAttribute(countAttribute)) {
                def childCount = dev.currentValue(countAttribute)
                if (childCount != null) {
                    weight = childCount.toInteger()
                }
            }
            weightedSum += (val.toBigDecimal() * weight)
            totalWeight += weight
        }
    }

    if (totalWeight > 0) {
        return weightedSum / totalWeight
    }
    return null
}

private void calculateAndSetTemp() {
    def target = getTargetDevice()
    if (!target || !tempSensors) return
    
    int totalDevices = calculateRollupDeviceCount(tempSensors, "tempDeviceCount")
    target.setTempDeviceCount(totalDevices)

    BigDecimal weightedAvg = calculateWeightedAverage(tempSensors, "temperature", "tempDeviceCount")
    if (weightedAvg != null) {
        logDebug "Calculated Weighted Temp Average (${totalDevices} physical sensors rolled up): ${weightedAvg}"
        target.setTemperature(weightedAvg)
    }
}

private void calculateAndSetHumidity() {
    def target = getTargetDevice()
    if (!target || !humidSensors) return
    
    int totalDevices = calculateRollupDeviceCount(humidSensors, "humidityDeviceCount")
    target.setHumidityDeviceCount(totalDevices)

    BigDecimal weightedAvg = calculateWeightedAverage(humidSensors, "humidity", "humidityDeviceCount")
    if (weightedAvg != null) {
        logDebug "Calculated Weighted Humidity Average (${totalDevices} physical sensors rolled up): ${weightedAvg}"
        target.setRelativeHumidity(weightedAvg)
    }
}

private void calculateAndSetLux() {
    def target = getTargetDevice()
    if (!target || !luxSensors) return
    
    int totalDevices = calculateRollupDeviceCount(luxSensors, "illuminanceDeviceCount")
    target.setIlluminanceDeviceCount(totalDevices)

    BigDecimal weightedAvg = calculateWeightedAverage(luxSensors, "illuminance", "illuminanceDeviceCount")
    if (weightedAvg != null) {
        // ALWAYS update the child device for real-time state fidelity
        target.setIlluminance(weightedAvg)

        // Delta-Threshold Filter for logDebug emission
        BigDecimal lastLogged = state.lastLoggedLux != null ? (state.lastLoggedLux as BigDecimal) : null
        Boolean isSignificant = false

        if (lastLogged == null) {
            isSignificant = true
        } else {
            BigDecimal delta = (weightedAvg - lastLogged).abs()
            BigDecimal percentChange = lastLogged != 0 ? (delta / lastLogged) * 100 : 100

            // Threshold: >= 10% change OR >= 25 lux absolute change
            if (percentChange >= 10 || delta >= 25) {
                isSignificant = true
            }
        }

        if (isSignificant) {
            logDebug "Calculated Weighted Lux Average (${totalDevices} physical sensors rolled up): ${weightedAvg} (Previous Logged: ${lastLogged ?: 'None'})"
            state.lastLoggedLux = weightedAvg
        } else {
            logTrace "Calculated Weighted Lux Average: ${weightedAvg} (Suppressed logDebug due to minor change from ${lastLogged})"
        }
    }
}

private void calculateAndSetPower() {
    def target = getTargetDevice()
    if (!target || !powerSensors) return
    
    int totalDevices = calculateRollupDeviceCount(powerSensors, "powerDeviceCount")
    target.setPowerDeviceCount(totalDevices)

    if (settings.aggregationType == "sum") {
        def validValues = powerSensors.collect { it.currentValue("power") }.findAll { it != null }
        if (validValues && validValues.size() > 0) {
            def totalPower = validValues.sum()
            logDebug "Calculated Power Sum (${totalDevices} physical sensors rolled up): ${totalPower}"
            target.setPower(totalPower)
        }
    } else {
        BigDecimal weightedAvg = calculateWeightedAverage(powerSensors, "power", "powerDeviceCount")
        if (weightedAvg != null) {
            logDebug "Calculated Weighted Power Average (${totalDevices} physical sensors rolled up): ${weightedAvg}"
            target.setPower(weightedAvg)
        }
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