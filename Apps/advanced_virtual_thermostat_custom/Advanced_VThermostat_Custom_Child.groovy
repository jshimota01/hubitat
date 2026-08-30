/**
 * Advanced vThermostat Child App (Custom)
 * Platform: Hubitat Elevation
 * Notes: Custom child app for binding temperature sensors to heating/cooling outlets for virtual thermostat operation
 **/
/**
 * Copyright 2026 James Shimota / Original 2020 Nelson Clark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
/**
 *  Purpose:
 *  Child application instance managing a single Advanced vThermostat Device (Custom) instance.
 *
 *  Instructions:
 *  1. Created automatically via Advanced vThermostat Manager (Custom).
 *  2. Select temperature sensors, heating outlets, and cooling outlets to pair.
 *  
 *  Changelog:
 *  v2.3.2    08/30/26    jshimota    Updated child creation callouts to highlight automatic 'Virtual' room placement
 *  v2.3.1    08/30/26    jshimota    Added automatic assignment of new child devices to 'Virtual' room
 *  v2.3.0    08/30/26    jshimota    Added automated device creation help callout and applied v1.1.0 App Master Template
 *  v2.2.1    08/30/26    jshimota    Verified brackets/parentheses parity and NPE-safe location scale checks
 *  v2.2.0    08/30/26    jshimota    Applied initial App Master Template
 *  v2.1.1    08/30/26    jshimota    Formatted names to use (Custom) in parenthetical style
 *  v2.1.0    08/30/26    jshimota    Removed v2 identifiers, updated URLs and app/device references
 *  v2.0.0    08/22/26    jshimota    Bumped definition name to v2 and corrected child device creation
 **/

static String version() { return '2.3.2' }
def timeStamp() { return "2026/08/30 09:33 AM" }

definition(
    name: "Advanced vThermostat Child (Custom)",
    namespace: "jshimota",
    author: "Nelson Clark / Customizations by jshimota",
    description: "Join any sensor(s) with any outlet(s) for virtual thermostat control.",
    category: "Green Living",
    iconUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-logo-small.png",
    iconX2Url: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-logo.png",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat_Custom_Child.groovy",
    parent: "jshimota:Advanced vThermostat Manager (Custom)"
)

preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    def displayUnits = getDisplayUnits()
    def hubScale = getTemperatureScale()
    
    def setpointDistance = (hubScale == "C") ? 3.0 : 5.0
    def defaultHeat = (hubScale == "C") ? 21.0 : 70.0
    def defaultCool = (hubScale == "C") ? 24.5 : 76.0
    def defaultThresh = (hubScale == "C") ? 0.5 : 1.0

    dynamicPage(name: "pageConfig", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* Styled App Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Advanced vThermostat Child (Custom)</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        section("<b>Thermostat Naming & Auto-Creation Notice</b>") {
            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #3498DB; padding:10px; border-radius:4px; font-size:12px; margin-bottom:8px;'>" +
                      "ℹ️ <b>Automatic Device Creation & Room Assignment:</b> Entering a name below automatically creates and configures your virtual thermostat device under <b>Devices</b> using the <code>Advanced vThermostat Device (Custom)</code> driver.<br/><br/>" +
                      "<b>Room Assignment:</b> This app will automatically place the newly created virtual thermostat directly into the <b>Virtual</b> room on your hub. <b>You do not need to manually create a device or create a room!</b></div>"
            
            label title: "<b>Name for this Advanced vThermostat Instance:</b>", required: true
        }
        
        section("<b>Temperature Sensors</b>") {
            input "sensors", "capability.temperatureMeasurement", title: "Select Sensor(s) (Average value will be used if multiple selected):", multiple: true, required: true
        }

        section("<b>Heating Outlets</b>") {
            input "heatOutlets", "capability.switch", title: "Select Outlet(s) to use for Heating:", multiple: true
        }

        section("<b>Cooling Outlets</b>") {
            input "coolOutlets", "capability.switch", title: "Select Outlet(s) to use for Cooling:", multiple: true
        }

        if (!state.deviceID) {
            section("<b>Initial Thermostat Settings</b>") {
                input "heatingSetPoint", "decimal", title: "Heating Setpoint in $displayUnits (min $setpointDistance $displayUnits lower than cooling)", required: true, defaultValue: defaultHeat
                input "coolingSetPoint", "decimal", title: "Cooling Setpoint in $displayUnits (min $setpointDistance $displayUnits higher than heating)", required: true, defaultValue: defaultCool
                input "thermostatThreshold", "decimal", title: "Temperature Threshold in $displayUnits", required: true, defaultValue: defaultThresh
            }
        }
    
        /* Collapsible App Preferences & Logging Options */
        section("<b>App Preferences & Logging Options</b>", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            input name: "showStatusInLabel", type: "bool", title: "Show Active Status in App Label?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
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

// NPE-Safe Timestamp Helper Routine
private String getTimestamp() {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)
}

// Dynamic App Label Badging Helper
private void updateAppLabel(String statusText = null) {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    Boolean showStatus  = getSettingBool("showStatusInLabel", true)

    String customLabel = app.getLabel() ?: "Advanced vThermostat Child"
    String baseLabel = customLabel
    
    // Clean out prior badge appended strings if re-evaluating
    if (baseLabel.contains(" v2.")) baseLabel = baseLabel.substring(0, baseLabel.indexOf(" v2."))
    
    if (showVersion) baseLabel += " v${version()}"

    if (showStatus && statusText) {
        baseLabel += " - [${statusText}]"
    }

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

// Hubitat App Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing child app v${version()} (${timeStamp()})..."
    
    state.deviceID = "avt-" + app.id
    def label = app.getLabel()
    logInfo "Creating vThermostat child device automatically in 'Virtual' room: ${label} with ID: ${state.deviceID}"
    
    def thermostat = null
    try {
        thermostat = addChildDevice(
            "jshimota", 
            "Advanced vThermostat Device (Custom)", 
            state.deviceID, 
            null, 
            [
                label: label, 
                name: label, 
                room: "Virtual", 
                completedSetup: true
            ]
        )
    } catch(e) {
        logError "Error adding vThermostat child device ${label}: ${e}"
    }
    
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize(thermostat, true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating child app configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)

    if (settingsChanged) {
        logInfo "Settings modification detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(getThermostat(), false)
    } else {
        logDebug "Child app closed without setting changes. Skipping re-initialization."
    }
    
    def thermostat = getThermostat()
    String currentOpState = thermostat ? thermostat.currentValue("thermostatOperatingState") : "Idle"
    updateAppLabel(currentOpState?.capitalize())
}

void uninstalled() {
    logInfo "Uninstalling child app and device ${state.deviceID}..."
    unsubscribe()
    unschedule()
    if (state.deviceID) {
        deleteChildDevice(state.deviceID)
    }
}

private void initialize(thermostatInstance = null, Boolean isInstall = false) {
    if (!thermostatInstance) thermostatInstance = getThermostat()
    if (!thermostatInstance) {
        logError "initialize() - Device instance not found."
        return
    }

    logTrace "Initialize Running vThermostat: $app.label"

    def thermostatMode = "off"
    if (heatOutlets && coolOutlets) {
        thermostatMode = "auto"
    } else if (heatOutlets) {
        thermostatMode = "heat"
    } else if (coolOutlets) {
        thermostatMode = "cool"
    }
    
    if (heatingSetPoint != null) thermostatInstance.setHeatingSetpoint(heatingSetPoint.toDouble())
    if (coolingSetPoint != null) thermostatInstance.setCoolingSetpoint(coolingSetPoint.toDouble())
    if (thermostatThreshold != null) thermostatInstance.setThermostatThreshold(thermostatThreshold.toDouble())
    
    thermostatInstance.setThermostatMode(thermostatMode)

    subscribe(sensors, "temperature", temperatureHandler)
    subscribe(thermostatInstance, "thermostatOperatingState", thermostatStateHandler)

    updateTemperature()

    String currentOpState = thermostatInstance.currentValue("thermostatOperatingState") ?: "Idle"
    updateAppLabel(currentOpState?.capitalize())

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
}

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
        state.lastLogDebugEnable = false
    }
}

// Thermostat Child Core Logic Routines
def getThermostat() {
    if (!state.deviceID) {
        logError "getThermostat cannot access deviceID!"
        return null
    }
    return getChildDevices().find { d -> d.deviceNetworkId == state.deviceID }
}

def temperatureHandler(evt) {
    logDebug "Temperature changed to ${evt.value}"
    updateTemperature()
}

def updateTemperature() {
    def thermostat = getThermostat()
    if (!thermostat || !sensors) return null

    def validTemps = []
    sensors.each { sensor ->
        def val = sensor.currentValue("temperature")
        if (val != null) {
            if (val instanceof Number) {
                validTemps << val.toDouble()
            } else if (val.toString().isNumber()) {
                validTemps << val.toString().toDouble()
            } else {
                logWarn "updateTemperature() - Invalid non-numeric temperature value received from ${sensor.displayName}: ${val}"
            }
        }
    }

    if (validTemps.isEmpty()) return null

    def avgTemp = (validTemps.sum() / validTemps.size()).toDouble().round(1)
    
    if (thermostat.currentValue("temperature") != avgTemp) {
        thermostat.setTemperature(avgTemp)
    }
    return avgTemp
}

def thermostatStateHandler(evt) {
    if (evt.value) {
        logInfo "Thermostat state changed to ${evt.value}"
        setOutletsState(evt.value)
        updateAppLabel(evt.value?.capitalize())
    }
}

def setOutletsState(opState = null) {
    def thermostat = getThermostat()
    if (!thermostat) return

    def currentState = opState ?: thermostat.currentValue("thermostatOperatingState")

    if (currentState == "heating") {
        safelyControlSwitches(coolOutlets, "off")
        safelyControlSwitches(heatOutlets, "on")
    } else if (currentState == "cooling") {
        safelyControlSwitches(heatOutlets, "off")
        safelyControlSwitches(coolOutlets, "on")
    } else {
        safelyControlSwitches(heatOutlets, "off")
        safelyControlSwitches(coolOutlets, "off")
    }
}

def safelyControlSwitches(devices, String targetState) {
    if (!devices) return
    devices.each { device ->
        if (device.currentValue("switch") != targetState) {
            if (device.hasCommand(targetState)) {
                device."${targetState}"()
            }
        }
    }
}

// Centralized Logging Engine
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appLabel = app.label ?: app.name ?: "App"
    
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${appLabel}: ${msg}"
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

def getTemperatureScale() { return "${location?.temperatureScale ?: 'F'}" }
def getDisplayUnits() { return getTemperatureScale() == "C" ? "°C" : "°F" }