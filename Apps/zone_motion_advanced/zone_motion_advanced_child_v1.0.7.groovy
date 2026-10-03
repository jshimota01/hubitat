/**
 * Zone Motion Advanced Child
 * Platform: Hubitat Elevation (v2.4.4.156)
 * Purpose: Child app managing individual zone motion aggregation, false motion reduction, and triggered activation logic.
 **/
/**
 * Copyright 2026 James Shimota
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
 * Purpose:
 * Evaluates motion logic algorithms (Aggregation, False Motion Reduction, Triggered Activation)
 * and maintains the state of the associated child Virtual Motion device.
 *
 * Changelog:
 *  v1.0.7    10/02/26    jshimota    Added 5 seconds, 30 minutes, and 60 minutes options to Activity Timeout; set default Warning logging to false.
 *  v1.0.5    10/02/26    jshimota    Prevented artificial activation timestamp generation for False Motion Reduction during initialization.
 *  v1.0.4    10/02/26    jshimota    Fixed codeVersionChanged bug; reconciled active sensor state on initialize; fixed mode check deactivation lock; added overwrite to expireTrigger; excluded admin settings from snapshot hash.
 *  v1.0.3    10/02/26    jshimota    Renamed SmartApp definition to 'Zone Motion Advanced Child'.
 *  v1.0.2    10/02/26    jshimota    Removed manual asterisk and bold formatting from field titles; added explicit '(required)' label to mandatory inputs.
 *  v1.0.1    10/02/26    jshimota    Cleaned up duplicate input asterisks (* * -> *), updated default names to 'ZMA - ', added activation window notes.
 *  v1.0.0    10/01/26    jshimota    Initial release matching Hubitat zone motion specifications.
 **/

static String version() { return '1.0.7' }
def timeStamp() { return "2026/10/02 03:40 PM" }

definition(
    name: "Zone Motion Advanced Child",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Individual Zone Motion Advanced Child Instance",
    category: "Utility",
    parent: "jshimota:Zone Motion Advanced",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleThreaded: true,
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/zone_motion_advanced/zone_motion_advanced_child.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Zone Motion Advanced Child</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        section() {
            input name: "zoneName", type: "text", title: "Name of this Zone (required)", required: true, defaultValue: "ZMA - New Zone", submitOnChange: true
            input name: "zoneType", type: "enum", title: "Select a Zone Type (required)", required: true, options: [
                "False Motion Reduction": "False Motion Reduction",
                "Motion Aggregation": "Motion Aggregation",
                "Triggered Activation": "Triggered Activation"
            ], submitOnChange: true
        }

        if (settings.zoneType) {
            section() {
                switch (settings.zoneType) {
                    case "Motion Aggregation":
                        paragraph "<i>Any motion sensor will activate this zone.<br>The Activity Timeout is started after all motion sensors become inactive.<br>This zone will deactivate when the Activity Timeout expires, and no further motion was detected.</i>"
                        input name: "motionSensors", type: "capability.motionSensor", title: "Motion Sensors (required)", multiple: true, required: true
                        input name: "activityTimeout", type: "enum", title: "Activity Timeout (required)", required: true, options: timeoutOptions(), defaultValue: "120"
                        break

                    case "False Motion Reduction":
                        paragraph "<i>When at least the Minimum Active Threshold number of motion sensors become active within the Activation Window, the zone will activate.<br>This zone will deactivate when all motion sensors become inactive.</i>"
                        input name: "motionSensors", type: "capability.motionSensor", title: "Motion Sensors (required)", multiple: true, required: true
                        input name: "activationWindow", type: "enum", title: "Activation Window (required)", description: "<i>Maximum time window allowed for Minimum Active Threshold value of sensors to become active.</i>", required: true, options: windowOptions(), defaultValue: "5"
                        input name: "minThreshold", type: "number", title: "Minimum Active Threshold (required)", required: true, defaultValue: 2, range: "2..10"
                        break

                    case "Triggered Activation":
                        paragraph "<i>This zone will activate when any motion sensor becomes active within the Activation Window.<br>The Activation Window is enabled by the Trigger Devices(s).<br>The Activity Timeout is started after all motion sensors become inactive.<br>This zone will deactivate when the Activity Timeout expires, and no further motion was detected.</i>"
                        input name: "motionSensors", type: "capability.motionSensor", title: "Motion Sensors (required)", multiple: true, required: true
                        input name: "activationWindow", type: "enum", title: "Activation Window (required)", description: "<i>Time duration the trigger condition remains active waiting for motion.</i>", required: true, options: windowOptions(), defaultValue: "15"
                        input name: "triggerDevices", type: "capability.sensor", title: "Trigger Devices (required)", multiple: true, required: true
                        input name: "activityTimeout", type: "enum", title: "Activity Timeout (required)", required: true, options: timeoutOptions(), defaultValue: "120"
                        break
                }
            }

            section() {
                input name: "modes", type: "mode", title: "Set for specific mode(s) only, default is all", multiple: true, required: false
            }
        }

        section("App Preferences & Logging Options", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging (required)", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging (required)", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging (required)", defaultValue: false, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging (required)", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging (required)", defaultValue: false, required: true
        }
    }
}

private Map windowOptions() {
    return ["3": "3 Seconds", "5": "5 Seconds", "10": "10 Seconds", "15": "15 Seconds", "30": "30 Seconds", "60": "1 Minute"]
}

private Map timeoutOptions() {
    return [
        "5": "5 Seconds",
        "10": "10 Seconds",
        "30": "30 Seconds",
        "60": "1 Minute",
        "120": "2 Minutes",
        "300": "5 Minutes",
        "600": "10 Minutes",
        "900": "15 Minutes",
        "1800": "30 Minutes",
        "3600": "60 Minutes"
    ]
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = settings.zoneName ?: "ZMA - Zone"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> ignoredKeys = ["label", "showVersionInLabel", "logInfoEnable", "logErrorEnable", "logWarnEnable", "logDebugEnable", "logTraceEnable"]
    List<String> sortedKeys = settings.keySet().collect { it.toString() }.findAll { k -> !(k.startsWith("btn") || ignoredKeys.contains(k)) }.sort()
    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
}

void installed() {
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    checkAndLogVersionDemarcation()
    logInfo "Installing zone v${version()} (${timeStamp()})..."
    ensureChildDevice()
    initialize(true)
}

void updated() {
    Boolean codeVersionChanged = (state.appVersion != version())
    checkAndLogVersionDemarcation()
    logInfo "Updating zone configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)

    ensureChildDevice()

    if (settingsChanged || codeVersionChanged) {
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(false)
    }
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling zone..."
    unsubscribe()
    unschedule()
    deleteChildDevice()
}

private void ensureChildDevice() {
    String dni = "ZM_CHILD_${app.id}"
    def child = getChildDevice(dni)
    String devName = settings.zoneName ?: "ZMA - Zone Motion Device"
    if (!child) {
        logInfo "Creating Virtual Zone Motion Device: ${devName}"
        addChildDevice("jshimota", "Zone Motion Advanced Child Device", dni, null, [name: devName, label: devName])
    } else {
        if (child.label != devName) child.setLabel(devName)
    }
}

private void deleteChildDevice() {
    String dni = "ZM_CHILD_${app.id}"
    def child = getChildDevice(dni)
    if (child) deleteChildDevice(dni)
}

private def getZoneDevice() {
    return getChildDevice("ZM_CHILD_${app.id}")
}

private void initialize(Boolean isInstall = false) {
    updateAppLabel()

    // Reconcile physical device states upon initialization
    List activeList = []
    Map timeMap = [:]
    Long nowMs = now()

    settings.motionSensors?.each { sensor ->
        if (sensor.currentValue("motion") == "active") {
            String devId = sensor.id.toString()
            activeList.add(devId)
            
            // Do not manufacture artificial timestamps for False Motion Reduction to avoid false threshold activation
            if (settings.zoneType != "False Motion Reduction") {
                timeMap[devId] = nowMs
            }
        }
    }

    state.activeSensors = activeList
    state.activationTimes = timeMap
    state.triggerActive = false

    if (settings.motionSensors) {
        subscribe(settings.motionSensors, "motion", "motionHandler")
    }

    if (settings.zoneType == "Triggered Activation" && settings.triggerDevices) {
        subscribe(settings.triggerDevices, "switch.on", "triggerOnHandler")
        subscribe(settings.triggerDevices, "contact.open", "triggerOnHandler")
        subscribe(settings.triggerDevices, "motion.active", "triggerOnHandler")
        subscribe(settings.triggerDevices, "presence.present", "triggerOnHandler")
    }

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

private boolean modeCheck() {
    return (!settings.modes || settings.modes.contains(location.mode))
}

void triggerOnHandler(evt) {
    if (!modeCheck()) return
    logDebug "Trigger device activated: ${evt.displayName}"
    state.triggerActive = true
    int windowSec = (settings.activationWindow ?: "15").toInteger()
    runIn(windowSec, "expireTrigger", [overwrite: true])
}

void expireTrigger() {
    logDebug "Activation Window expired for Triggered Activation."
    state.triggerActive = false
}

void motionHandler(evt) {
    Boolean isActive = (evt.value == "active")

    // Allow inactive events to execute deactivation logic even when out of mode to prevent stranded state
    if (isActive && !modeCheck()) {
        logDebug "Motion active ignored due to mode filter restriction."
        return
    }

    String devId = evt.deviceId.toString()
    Long nowMs = now()

    List activeList = state.activeSensors ?: []
    Map timeMap = state.activationTimes ?: [:]

    if (isActive) {
        if (!activeList.contains(devId)) activeList.add(devId)
        timeMap[devId] = nowMs
    } else {
        activeList.remove(devId)
    }

    state.activeSensors = activeList
    state.activationTimes = timeMap

    logDebug "Motion event from ${evt.displayName}: ${evt.value} (Active Count: ${activeList.size()})"

    switch (settings.zoneType) {
        case "Motion Aggregation":
            evaluateAggregation(isActive)
            break
        case "False Motion Reduction":
            evaluateFalseMotionReduction(isActive, nowMs)
            break
        case "Triggered Activation":
            evaluateTriggeredActivation(isActive)
            break
    }
}

private void evaluateAggregation(Boolean isActive) {
    def zoneDevice = getZoneDevice()
    if (isActive) {
        unschedule("deactivateZone")
        if (zoneDevice?.currentValue("motion") != "active") {
            logInfo "Motion Aggregation: Zone Activated"
            zoneDevice?.setActive()
        }
    } else {
        if ((state.activeSensors ?: []).isEmpty()) {
            int timeoutSec = (settings.activityTimeout ?: "120").toInteger()
            logDebug "All sensors inactive. Starting Activity Timeout (${timeoutSec}s)..."
            runIn(timeoutSec, "deactivateZone")
        }
    }
}

private void evaluateFalseMotionReduction(Boolean isActive, Long nowMs) {
    def zoneDevice = getZoneDevice()
    int windowMs = ((settings.activationWindow ?: "5").toInteger()) * 1000
    int threshold = (settings.minThreshold ?: 2) as Integer

    Map timeMap = state.activationTimes ?: [:]
    int validRecentCount = timeMap.values().count { (nowMs - (it as Long)) <= windowMs }

    if (isActive) {
        if (validRecentCount >= threshold) {
            if (zoneDevice?.currentValue("motion") != "active") {
                logInfo "False Motion Reduction: Threshold met (${validRecentCount}/${threshold}). Zone Activated."
                zoneDevice?.setActive()
            }
        }
    } else {
        if ((state.activeSensors ?: []).isEmpty()) {
            logInfo "False Motion Reduction: All sensors inactive. Zone Deactivated."
            zoneDevice?.setInactive()
        }
    }
}

private void evaluateTriggeredActivation(Boolean isActive) {
    def zoneDevice = getZoneDevice()
    if (isActive) {
        if (state.triggerActive == true) {
            unschedule("deactivateZone")
            if (zoneDevice?.currentValue("motion") != "active") {
                logInfo "Triggered Activation: Motion detected during window. Zone Activated."
                zoneDevice?.setActive()
            }
        } else {
            logDebug "Motion detected, but Trigger Activation Window is inactive. Ignoring."
        }
    } else {
        if ((state.activeSensors ?: []).isEmpty()) {
            int timeoutSec = (settings.activityTimeout ?: "120").toInteger()
            logDebug "All sensors inactive. Starting Activity Timeout (${timeoutSec}s)..."
            runIn(timeoutSec, "deactivateZone")
        }
    }
}

void deactivateZone() {
    if ((state.activeSensors ?: []).isEmpty()) {
        logInfo "Activity Timeout expired: Zone Deactivated."
        getZoneDevice()?.setInactive()
    } else {
        logDebug "Activity Timeout expired, but active sensors remain. Skipping deactivation."
    }
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appLabel = app.label ?: app.name ?: "App"
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "error"])

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