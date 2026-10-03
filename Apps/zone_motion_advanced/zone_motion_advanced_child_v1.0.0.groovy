/**
 * Zone Motion Advanced Zone (Child Instance)
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
 *  v1.0.0    10/01/26    jshimota    Initial release matching Hubitat zone motion specifications.
 **/

static String version() { return '1.0.0' }
def timeStamp() { return "2026/10/01 11:00 AM" }

definition(
    name: "Zone Motion Advanced Zone",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Individual Zone Motion Advanced Child Instance",
    category: "Utility",
    parent: "jshimota:Zone Motion Advanced",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleThreaded: true,
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/zone_motion_advanced/zone_motion_advanced_zone.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Zone Configuration</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        section() {
            input name: "zoneName", type: "text", title: "<b>Name of this Zone:*</b>", required: true, defaultValue: "ZM - New Zone", submitOnChange: true
            input name: "zoneType", type: "enum", title: "<b>Select a Zone Type *</b>", required: true, options: [
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
                        input name: "motionSensors", type: "capability.motionSensor", title: "<b>Motion Sensors: *</b>", multiple: true, required: true
                        input name: "activityTimeout", type: "enum", title: "<b>Activity Timeout: *</b>", required: true, options: timeoutOptions(), defaultValue: "120"
                        break

                    case "False Motion Reduction":
                        paragraph "<i>When at least the Minimum Active Threshold number of motion sensors become active within the Activation Window, the zone will activate.<br>This zone will deactivate when all motion sensors become inactive.</i>"
                        input name: "motionSensors", type: "capability.motionSensor", title: "<b>Motion Sensors: *</b>", multiple: true, required: true
                        input name: "activationWindow", type: "enum", title: "<b>Activation Window: *</b>", required: true, options: windowOptions(), defaultValue: "5"
                        input name: "minThreshold", type: "number", title: "<b>Minimum Active Threshold:*</b>", required: true, defaultValue: 2, range: "2..10"
                        break

                    case "Triggered Activation":
                        paragraph "<i>This zone will activate when any motion sensor becomes active within the Activation Window.<br>The Activation Window is enabled by the Trigger Devices(s).<br>The Activity Timeout is started after all motion sensors become inactive.<br>This zone will deactivate when the Activity Timeout expires, and no further motion was detected.</i>"
                        input name: "motionSensors", type: "capability.motionSensor", title: "<b>Motion Sensors: *</b>", multiple: true, required: true
                        input name: "activationWindow", type: "enum", title: "<b>Activation Window: *</b>", required: true, options: windowOptions(), defaultValue: "15"
                        input name: "triggerDevices", type: "capability.sensor", title: "<b>Trigger Devices</b>", multiple: true, required: true
                        input name: "activityTimeout", type: "enum", title: "<b>Activity Timeout: *</b>", required: true, options: timeoutOptions(), defaultValue: "120"
                        break
                }
            }

            section() {
                input name: "modes", type: "mode", title: "<b>Set for specific mode(s) only, default is all</b>", multiple: true, required: false
            }
        }

        section("<b>App Preferences & Logging Options</b>", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }
    }
}

private Map windowOptions() {
    return ["3": "3 Seconds", "5": "5 Seconds", "10": "10 Seconds", "15": "15 Seconds", "30": "30 Seconds", "60": "1 Minute"]
}

private Map timeoutOptions() {
    return ["10": "10 Seconds", "30": "30 Seconds", "60": "1 Minute", "120": "2 Minutes", "300": "5 Minutes", "600": "10 Minutes", "900": "15 Minutes"]
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
    String baseLabel = settings.zoneName ?: "ZM - Zone"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet().collect { it.toString() }.findAll { k -> !(k == "label" || k.startsWith("btn")) }.sort()
    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
}

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing zone v${version()} (${timeStamp()})..."
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    ensureChildDevice()
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating zone configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (state.appVersion != version())

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
    String devName = settings.zoneName ?: "ZM Zone Motion Device"
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
    checkAndLogVersionDemarcation()
    updateAppLabel()

    state.activeSensors = []
    state.activationTimes = [:]
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
    runIn(windowSec, "expireTrigger")
}

void expireTrigger() {
    logDebug "Activation Window expired for Triggered Activation."
    state.triggerActive = false
}

void motionHandler(evt) {
    if (!modeCheck()) return
    String devId = evt.deviceId.toString()
    Boolean isActive = (evt.value == "active")
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
    logInfo "Activity Timeout expired: Zone Deactivated."
    getZoneDevice()?.setInactive()
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