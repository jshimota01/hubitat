/**
 * Air Quality from AirNow Custom
 * Purpose-built custom polling driver for AirNow.org API data.
 * Device Driver for Hubitat Elevation
 **/
/*
 * Copyright 2021 C Steele / 2026 James Shimota
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
 */
/**
 * Changelog:
 * v1.2.4   10/03/26    jshimota    Removed siteName from airQualityCity fallback chain to preserve semantic accuracy, and implemented standard EPA AQI range calculation fallback when AirNow omits category metadata to prevent false "Good" reports on elevated AQIs.
 * v1.2.3   10/03/26    jshimota    Refactored observation evaluation into a two-phase architecture matching OWM driver behavior: evaluates all returned pollutants independently, maintains atomic winner state (AQI, category, color, location), handles ties cleanly (> strictly), isolates Worst (Highest) calculations to current-poll payloads, and preserves existing location metadata.
 * v1.2.2   10/03/26    jshimota    Normalized parameter names (OZONE -> O3), added airQualitySite and airQualityAgency attributes, and improved reporting area/state fallbacks.
 * v1.2.1   10/03/26    jshimota    Migrated endpoint URI to replacement /aq/observation/current/ziplatlong/ service, updated JSON payload parsing for modern schema fields, added category name mapping, removed concentration guessing, and maintained sanitized request logging.
 * v1.2.0   10/03/26    jshimota    Added HTML payload guard in pollHandler() to prevent JsonException crashes on API redirects, restored standard endpoint URL structure.
 * v1.1.9   10/03/26    jshimota    Migrated retired /latLong/current/ API path to active AirNow /aq/observation/latLong/byLatLon/ endpoint path to resolve HTTP 410 errors.
 * v1.1.8   10/03/26    jshimota    Added explicit HTTP 410 endpoint retirement error reporting and implemented dual PascalCase/camelCase schema key fallbacks for modern AirNow API payloads.
 * v1.1.7   10/03/26    jshimota    Sanitized debug logging to hide API Key, added trailing slash to endpoint URI to resolve HTTP 410 errors, capped AQI upper bound to 500, removed invented concentration fallbacks, fixed empty payload status reporting, and added missing basis pollutant handling.
 * v1.1.6   09/23/26    jshimota    Encapsulated primary AQI info logging inside sendIfChanged() guard and added explicit manual poll/refresh feedback logging.
 * v1.1.5   09/23/26    jshimota    Set basedOn default to Worst (Highest), updated preference option label, and added explicit attribute initialization so all pollutant fields appear in Hubitat UI.
 * v1.1.4   09/23/26    jshimota    Added dedicated pollutant concentration and _AQI sub-score attributes (CO, NO2, O3, SO2, PM2.5, PM10 and CO_AQI, NO2_AQI, O3_AQI, SO2_AQI, PM2.5_AQI, PM10_AQI) matching OWM driver conventions.
 * v1.1.3   09/23/26    jshimota    Added dominantPollutant attribute and dynamically log whichever pollutant is driving the primary AQI value.
 * v1.1.2   09/23/26    jshimota    Moved routine polling location message from info to debug level logging.
 * v1.1.1   09/23/26    jshimota    Applied standard driver template architecture, upgraded logging engine, implemented sendIfChanged() state guards, added Polling/Refresh capabilities, expanded poll intervals, and cleaned up log message phrasing.
 * v1.1.0   09/20/26    jshimota    Added distinct AirQualityIndex capability so it shows correctly in sensors.
 * v1.0.9   08/14/26    jshimota    Put pm2_5 BACK on PM2.5 so as to work correctly with influxdb and values coming from other drivers.
 * v1.0.8   06/17/26    jshimota    Changed Poll to use new API (by theBearMay).
 * v1.0.7   04/12/26    jshimota    Added success/fail status and last checked attributes.
 * v1.0.6   02/05/26    jshimota    Gemini recommendations integration.
 * v1.0.5   12/15/24    jshimota    Added ReportingArea and StateCode source for tile info.
 * v1.0.4   11/10/23    jshimota    Cleaned up some logging items.
 * v1.0.3   08/08/23    jshimota    Split out category and color.
 * v1.0.2   05/12/22    cmbruns     PR update for airQualityIndex range 0 to 500 matching standard Ecowitt sensor definitions.
 * v1.0.1   03/10/22    jshimota    Renamed PM2.5 attribute.
 * v1.0.0   01/15/21    csteele     Initial release created.
 **/

static String version() { return '1.2.4' }
def timeStamp() { return "2026/10/03 09:10 AM" }

import groovy.transform.Field
import java.math.RoundingMode

metadata {
    definition (name: "Air Quality from AirNow Custom", namespace: "jshimota", author: "James Shimota", importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/AirQualityfromAirNow_custom/AirQualityfromAirNow_custom.groovy") {
        capability "AirQuality"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"

        // Concentrations
        attribute "CO", "number"
        attribute "NO2", "number"
        attribute "O3", "number"
        attribute "SO2", "number"
        attribute "PM2.5", "number"
        attribute "PM10", "number"

        // Calculated Component Sub-AQI Scores
        attribute "CO_AQI", "number"
        attribute "NO2_AQI", "number"
        attribute "O3_AQI", "number"
        attribute "SO2_AQI", "number"
        attribute "PM2.5_AQI", "number"
        attribute "PM10_AQI", "number"

        // Overall AQI Metrics
        attribute "airQualityState", "STRING"
        attribute "airQualityCity", "STRING"
        attribute "airQualitySite", "STRING"
        attribute "airQualityAgency", "STRING"
        attribute "airQualityIndex", "number"
        attribute "airQualityColor", "STRING"
        attribute "airQualityCategory", "STRING"
        attribute "dominantPollutant", "STRING"
        
        // Custom operational attributes
        attribute "status", "STRING"
        attribute "lastChecked", "STRING"

        command "pollAirNow"
        command "resetDriver"
    }

    preferences {
        input name: "apiKey", type: "text", title: "<b>AirNow.org API Key</b>", description: "<i>Enter your AirNow.org API Key here.</i>", required: true, defaultValue: null
        input name: "pollEvery", type: "enum", title: "<b>Polling Frequency</b>", description: "<i>How often to request fresh AirNow AQI data.</i>", required: true, defaultValue: "60", options: PollIntervalOpts.options
        input name: "basedOn", type: "enum", title: "<b>Publish Primary AQI Based On</b>", description: "<i>Select parameter basis for Primary AQI evaluation.</i>", required: true, defaultValue: "4", options: [1: "O3 Ozone", 2: "PM2.5 Particle", 3: "PM10 Particle", 4: "Worst (Highest)"]

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper Routine
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    initialize(false)
}

void uninstalled() {
    clearAllSchedules()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule()

    final int intervalMin = settings.pollEvery != null ? settings.pollEvery.toInteger() : 60
    if (intervalMin > 0) {
        schedulePolling(intervalMin)
    }

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    }

    runIn(2, "pollAirNow")
}

/* =========================================================================================
   POLLING & API LOGIC
   ========================================================================================= */

void poll() {
    logInfo "Manual poll requested..."
    pollAirNow()
}

void refresh() {
    logInfo "Manual refresh requested..."
    pollAirNow()
}

void pollAirNow() {
    if (!settings.apiKey) {
        logWarn "AirNow API Key is missing. Please enter it in device settings."
        sendIfChanged("status", "Fail (Missing Key)")
        sendIfChanged("lastChecked", new Date().format("MM/dd/yyyy h:mm a", location.timeZone))
        return
    }

    logDebug "Polling AirNow for Location: Lat: ${location.latitude}, Lon: ${location.longitude}"

    Map params = [
        uri: "https://www.airnowapi.org/aq/observation/current/ziplatlong/?format=application/json&latitude=" + (String)location.latitude + "&longitude=" + (String)location.longitude + "&distance=25&API_KEY=" + (String)settings.apiKey,
        timeout: 20
    ]

    logDebug "Executing AirNow API GET Request to: https://www.airnowapi.org/aq/observation/current/ziplatlong/"
    asynchttpGet("pollHandler", params)
}

void pollHandler(resp, data) {
    String timestamp = new Date().format("MM/dd/yyyy h:mm a", location.timeZone)
    sendIfChanged("lastChecked", timestamp)

    if (resp.getStatus() == 200) {
        if (!resp.data) {
            logWarn "AirNow API returned an empty payload."
            sendIfChanged("status", "Fail (Empty Payload)")
            return
        }

        String body = resp.data.toString().trim()
        if (body.startsWith("<")) {
            logError "AirNow API returned an HTML page instead of JSON. Check API Key or endpoint status."
            sendIfChanged("status", "Fail (Invalid HTML Response)")
            return
        }

        logTrace "AirNow API JSON Response: ${body}"

        def aqi = parseJson(body)
        if (!aqi || aqi.isEmpty()) {
            logWarn "AirNow API returned zero observation records."
            sendIfChanged("status", "Fail (No Data)")
            return
        }

        sendIfChanged("status", "Success")

        // =================================================================================
        // PHASE 1: PROCESS CURRENT POLL OBSERVATIONS INTO IN-MEMORY MAP
        // =================================================================================
        Map<String, Map> currentPollutants = [:]

        aqi.each { obs ->
            def rawAQI = obs.AQI != null ? obs.AQI : (obs.nowcastAQI != null ? obs.nowcastAQI : obs.aqi)
            if (rawAQI != null) {
                int aqiVal = rawAQI as Integer
                
                // Parameter Name Normalization (OZONE -> O3)
                String rawParam = obs.ParameterName ?: obs.parameterName ?: ""
                String paramName = normalizeParameterName(rawParam)

                // Location & Metadata Extraction (Strict Semantic Isolation)
                String siteName = obs.siteName ?: obs.SiteName ?: ""
                String reportingAgency = obs.reportingAgency ?: obs.ReportingAgency ?: ""
                String city = obs.ReportingArea ?: obs.reportingAreaName ?: obs.reportingArea ?: ""
                String devState = obs.StateCode ?: obs.stateCode ?: ""

                // Category Resolution (Defensive Fallback to Standard EPA AQI Ranges)
                int catNum = -1
                if (obs.Category?.Number != null) {
                    catNum = obs.Category.Number as Integer
                } else if (obs.category?.number != null) {
                    catNum = obs.category.number as Integer
                } else if (obs.aqiCategoryName || obs.Category?.Name || obs.category?.name) {
                    String catName = (obs.aqiCategoryName ?: obs.Category?.Name ?: obs.category?.name).toString()
                    catNum = mapCategoryNameToNumber(catName)
                }
                
                if (catNum < 1 || catNum > 6) {
                    catNum = deriveCategoryFromAQI(aqiVal)
                }

                if ((aqiVal >= 0) && (aqiVal <= 500) && paramName) {
                    
                    // Publish Sub-Score AQI Attribute
                    sendIfChanged("${paramName}_AQI", aqiVal, null, "trace", "${paramName} AQI")

                    // Publish Concentration ONLY if explicitly present in payload
                    def rawConc = obs.Concentration != null ? obs.Concentration : obs.concentration
                    if (rawConc != null) {
                        BigDecimal concentration = new BigDecimal(rawConc.toString()).setScale(3, RoundingMode.HALF_UP)
                        String unit = obs.Unit ?: obs.unit ?: null
                        sendIfChanged(paramName, concentration, unit, "trace", "${paramName} Concentration")
                    }

                    // Store observation in fresh in-memory collection for Phase 2 primary evaluation
                    currentPollutants[paramName] = [
                        paramName: paramName,
                        aqi: aqiVal,
                        catNum: catNum,
                        city: city,
                        devState: devState,
                        siteName: siteName,
                        reportingAgency: reportingAgency
                    ]
                }
            }
        }

        if (currentPollutants.isEmpty()) {
            logWarn "AirNow API payload contained no valid AQI pollutant observations."
            return
        }

        // =================================================================================
        // PHASE 2: SELECT & PUBLISH ATOMIC PRIMARY RESULT
        // =================================================================================
        String basisSetting = aqiBasis[settings.basedOn ? settings.basedOn as Integer : 4]
        Map selectedResult = null

        if (basisSetting == "maxAQI") {
            int highestAQI = -1
            // Evaluate all returned pollutants; strictly use > to prevent tie-breaking fluctuation
            currentPollutants.each { key, obs ->
                if (obs.aqi > highestAQI) {
                    highestAQI = obs.aqi
                    selectedResult = obs
                }
            }
        } else {
            if (currentPollutants.containsKey(basisSetting)) {
                selectedResult = currentPollutants[basisSetting]
            } else {
                logWarn "Selected basis pollutant (${basisSetting}) was not returned in current AirNow payload."
            }
        }

        // Publish primary atomic attributes from winning observation
        if (selectedResult) {
            int primaryAQI = selectedResult.aqi as Integer
            String primaryParam = selectedResult.paramName
            int catNum = selectedResult.catNum as Integer

            String primarySummary = "AirNow Air Quality Index is now ${primaryAQI} for ${primaryParam} pollutant"
            sendIfChanged("airQualityIndex", primaryAQI, null, "info", "airQualityIndex", primarySummary)
            sendIfChanged("dominantPollutant", primaryParam, null, "info", "dominantPollutant")
            sendIfChanged("airQualityCategory", aqiCategory[catNum], null, "info", "airQualityCategory")
            sendIfChanged("airQualityColor", aqiColor[catNum], null, "info", "airQualityColor")
            
            if (selectedResult.city) sendIfChanged("airQualityCity", selectedResult.city, null, "info", "airQualityCity")
            if (selectedResult.devState) sendIfChanged("airQualityState", selectedResult.devState, null, "info", "airQualityState")
            if (selectedResult.siteName) sendIfChanged("airQualitySite", selectedResult.siteName, null, "info", "airQualitySite")
            if (selectedResult.reportingAgency) sendIfChanged("airQualityAgency", selectedResult.reportingAgency, null, "info", "airQualityAgency")
        }

    } else if (resp.getStatus() == 410) {
        logError "AirNow API Endpoint Retired/Deprecated (HTTP 410)."
        sendIfChanged("status", "Fail (Endpoint Retired - HTTP 410)")
    } else {
        logWarn "AirNow API lookup failed with status code: ${resp.getStatus()}"
        sendIfChanged("status", "Fail (HTTP ${resp.getStatus()})")
    }
}

private String normalizeParameterName(String rawParam) {
    if (!rawParam) return ""
    switch (rawParam.toUpperCase().trim()) {
        case "OZONE":
        case "O3":
            return "O3"
        case "PM2.5":
        case "PM25":
            return "PM2.5"
        case "PM10":
            return "PM10"
        case "CO":
            return "CO"
        case "NO2":
            return "NO2"
        case "SO2":
            return "SO2"
        default:
            return rawParam
    }
}

private int mapCategoryNameToNumber(String name) {
    switch (name?.toLowerCase()?.trim()) {
        case "good": return 1
        case "moderate": return 2
        case "unhealthy for sensitive groups": return 3
        case "unhealthy": return 4
        case "very unhealthy": return 5
        case "hazardous": return 6
        default: return -1
    }
}

private int deriveCategoryFromAQI(int aqiVal) {
    if (aqiVal <= 50) return 1       // Good
    if (aqiVal <= 100) return 2      // Moderate
    if (aqiVal <= 150) return 3      // Unhealthy for Sensitive Groups
    if (aqiVal <= 200) return 4      // Unhealthy
    if (aqiVal <= 300) return 5      // Very Unhealthy
    return 6                         // Hazardous
}

/* =========================================================================================
   SCHEDULING & EVENT GUARD ROUTINES
   ========================================================================================= */

private void schedulePolling(int intervalMin) {
    int m = new Random().nextInt(60)
    
    switch (intervalMin) {
        case 30:
            schedule("0 */30 * ? * * *", "pollAirNow"); break
        case 60:
            schedule("0 ${m} * ? * * *", "pollAirNow"); break
        case 120:
            schedule("0 ${m} */2 ? * * *", "pollAirNow"); break
        case 240:
            schedule("0 ${m} */4 ? * * *", "pollAirNow"); break
        case 480:
            schedule("0 ${m} */8 ? * * *", "pollAirNow"); break
        case 720:
            schedule("0 ${m} */12 ? * * *", "pollAirNow"); break
        case 960:
            schedule("0 ${m} */16 ? * * *", "pollAirNow"); break
        case 1440:
            schedule("0 ${m} 0 ? * * *", "pollAirNow"); break
        default:
            schedule("0 ${m} * ? * * *", "pollAirNow"); break
    }
    logInfo "Polling scheduled every ${intervalMin} minutes."
}

private void sendIfChanged(final String attribute, final Object value, final String unit = null, final String logLevel = "info", final String customLabel = null, final String customLogMessage = null) {
    if (value == null) return
    final String valStr = value.toString()
    if (valStr.trim().isEmpty()) return

    final String currentVal = device.currentValue(attribute)?.toString()

    if (currentVal == valStr) {
        logDebug "sendIfChanged(): Suppressed duplicate ${attribute} event (${valStr})"
        return
    }

    final String lastVal = state["last_${attribute}"]?.toString()
    if (lastVal == valStr) {
        logDebug "sendIfChanged(): Suppressed identical state ${attribute} (${valStr})"
        return
    }

    state["last_${attribute}"] = valStr

    final String label = customLabel ?: attribute
    final String logText = customLogMessage ?: "${label} now ${value}${unit ? ' ' + unit : ''}"
    final String descriptionText = "${device.displayName} ${logText}"

    logMessage(logLevel, logText)
    sendEvent(name: attribute, value: value, unit: unit, isStateChange: true, descriptionText: descriptionText)
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    initialize(false)
    logInfo "Driver reset completed."
}

void clearAllDriverStates() {
    logInfo "Clearing driver states..."
    state.clear()
}

void clearAllAttributes() {
    logInfo "Clearing attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
}

void clearAllSchedules() {
    logInfo "Clearing scheduled jobs..."
    unschedule()
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    
    String settingKey
    switch (lowerLevel) {
        case "info":  settingKey = "logInfoEnable"; break
        case "error": settingKey = "logErrorEnable"; break
        case "warn":  settingKey = "logWarnEnable"; break
        case "debug": settingKey = "logDebugEnable"; break
        case "trace": settingKey = "logTraceEnable"; break
        default:      settingKey = "logInfoEnable"; break
    }

    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${devName}: ${msg ?: ''}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

@Field static final aqiColor = [1: "Green", 2: "Yellow", 3: "Orange", 4: "Red", 5: "Purple", 6: "Maroon"]
@Field static final aqiCategory = [1: "Good", 2: "Moderate", 3: "Unhealthy for Sensitive Groups", 4: "Unhealthy", 5: "Very Unhealthy", 6: "Hazardous"]
@Field static final aqiBasis = [1: "O3", 2: "PM2.5", 3: "PM10", 4: "maxAQI"]

@Field static final Map PollIntervalOpts = [
    options: [ 30: "30 minutes", 60: "1 hour", 120: "2 hours", 240: "4 hours", 480: "8 hours", 720: "12 hours", 960: "16 hours", 1440: "24 hours" ]
]