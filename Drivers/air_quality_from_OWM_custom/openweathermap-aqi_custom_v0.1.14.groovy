/**
 * OpenWeatherMap-Air Quality - Custom
 * Purpose-built custom polling driver for OpenWeatherMap Air Pollution API data.
 * Device Driver for Hubitat Elevation
 **/
/*
 * Copyright 2022 SJ / 2026 James Shimota
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
 * v0.1.14  09/23/26    jshimota    Added explicit info logging upon manual poll/refresh execution to provide immediate UI feedback while maintaining sendIfChanged data suppression.
 * v0.1.13  09/23/26    jshimota    Encapsulated primary AQI info logging inside sendIfChanged() guard to suppress duplicate log messages on manual poll execution.
 * v0.1.12  09/23/26    jshimota    Changed AirNowCity logging level to debug and synchronized primary AQI info log phrasing with AirNow driver conventions.
 * v0.1.11  09/23/26    jshimota    Added dedicated sub-score AQI attributes (CO_AQI, NO2_AQI, O3_AQI, SO2_AQI, PM2.5_AQI, PM10_AQI) alongside raw concentrations.
 * v0.1.10  09/23/26    jshimota    Renamed PrimaryFactor attribute to dominantPollutant to align with AirNow driver conventions.
 * v0.1.9   09/23/26    jshimota    Applied standard driver template architecture, upgraded logging engine, implemented sendIfChanged() state guards, added Polling capability, and standardized primary AQI info logging.
 * v0.1.8   09/20/26    jshimota    Added AirQualityIndex and Sensor capabilities so it shows up correctly in sensor listings.
 * v0.1.7   04/12/26    jshimota    Integrated AirNowCity via OWM Geo API and stabilized null/empty checks.
 * v0.1.6   11/10/23    jshimota    Cleaned debug and text logging.
 * v0.1.5   08/08/23    jshimota    Added user editable lat/long and user editable tile width.
 * v0.1.4   05/12/23    jshimota    Added AQI tile.
 * v0.1.3   10/04/22    SJ          Changed output to default AirQuality in US format.
 * v0.1.2   10/03/22    SJ          Fixed then statement logic.
 * v0.1.1   10/01/22    SJ          Changed Hex value returned to have # in front for light assignment.
 * v0.1.0   09/15/22    Byrin       Initial release created.
 **/

static String version() { return '0.1.14' }
def timeStamp() { return "2026/09/23 01:25 PM" }

import groovy.transform.Field
import java.math.RoundingMode

metadata {
    definition (name: "OpenWeatherMap-Air Quality-custom", namespace: "James Shimota", author: "James Shimota", importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/OpenWeatherMap-AirQuality_custom/OpenWeatherMap-AirQuality_custom.groovy") {
        capability "AirQuality"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"

        // Concentrations
        attribute "CO", "number"
        attribute "NO", "number"
        attribute "NO2", "number"
        attribute "O3", "number"
        attribute "SO2", "number"
        attribute "PM2.5", "number"
        attribute "PM10", "number"
        attribute "NH3", "number"

        // Calculated Component Sub-AQI Scores
        attribute "CO_AQI", "number"
        attribute "NO2_AQI", "number"
        attribute "O3_AQI", "number"
        attribute "SO2_AQI", "number"
        attribute "PM2.5_AQI", "number"
        attribute "PM10_AQI", "number"

        // Overall AQI Metrics
        attribute "airQualityIndex", "number"
        attribute "AQIColorCode", "string"
        attribute "dominantPollutant", "string"
        attribute "AlertTile", "string"
        attribute "AirNowCity", "string"

        command "pollData"
        command "resetDriver"
    }

    preferences {
        input name: "apiKey", type: "text", title: "<b>OpenWeatherMap.org API Key</b>", description: "<i>Type OpenWeatherMap.org API Key Here</i>", required: true, defaultValue: null
        input name: "pollEvery", type: "enum", title: "<b>Polling Frequency</b>", description: "<i>How often to request fresh OpenWeatherMap AQI data.</i>", required: true, defaultValue: "30", options: PollIntervalOpts.options
        input name: "locLat", type: "text", title: "<b>Location Latitude</b>", defaultValue: location.latitude
        input name: "locLon", type: "text", title: "<b>Location Longitude</b>", defaultValue: location.longitude
        input name: "tileWidth", type: "number", title: "<b>Tile Width</b>", defaultValue: 125

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

    final int intervalMin = settings.pollEvery != null ? settings.pollEvery.toInteger() : 30
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

    runIn(2, "pollData")
}

/* =========================================================================================
   POLLING & API LOGIC
   ========================================================================================= */

void poll() {
    logInfo "Manual poll requested..."
    pollData()
}

void refresh() {
    logInfo "Manual refresh requested..."
    pollData()
}

void pollData() {
    pollAQI()
    pollCityName()
}

void pollAQI() {
    if (!settings.apiKey) {
        logWarn "OpenWeatherMap API Key is missing. Please enter it in device settings."
        return
    }

    String lat = settings.locLat ?: location.latitude
    String lon = settings.locLon ?: location.longitude

    Map paramsAQI = [
        uri: "https://api.openweathermap.org/data/2.5/air_pollution?lat=" + (String)lat + "&lon=" + (String)lon + "&appid=" + (String)settings.apiKey,
        timeout: 20
    ]
    logDebug "ParamsAQI: ${paramsAQI}"
    asynchttpGet("pollAQIHandler", paramsAQI)
}

void pollAQIHandler(resp, data) {
    if (resp.getStatus() == 200 || resp.getStatus() == 207) {
        Map aqi = parseJson(resp.data)
        
        if (!aqi || !aqi.list || !aqi.list[0]) {
            logWarn "AQI payload was null or empty."
            return
        }

        int aQItemp = 0
        int aQI = 0
        String aQIfactor = "none"

        // CO Evaluation
        String name = "CO"
        def rawVal = aqi.list[0].components.co
        def convertedVal = Math.round((rawVal / 114.5) * 10) / 10.0
        if (convertedVal < 4.5) {
            aQItemp = (50 / 4.4) * (convertedVal)
        } else if (convertedVal < 9.5) {
            aQItemp = (49 / 4.9) * (convertedVal - 4.5) + 51
        } else if (convertedVal < 12.5) {
            aQItemp = (49 / 2.9) * (convertedVal - 9.5) + 101
        } else if (convertedVal < 15.5) {
            aQItemp = (49 / 2.9) * (convertedVal - 12.5) + 151
        } else if (convertedVal < 30.5) {
            aQItemp = (99 / 14.9) * (convertedVal - 15.5) + 201
        } else if (convertedVal < 40.5) {
            aQItemp = (99 / 9.9) * (convertedVal - 30.5) + 301
        } else {
            aQItemp = (99 / 9.9) * (convertedVal - 40.5) + 401
        }
        int coAQI = Math.round(aQItemp)
        aQI = coAQI
        aQIfactor = name
        sendIfChanged("CO", convertedVal, "ppm", "trace", "CO Concentration")
        sendIfChanged("CO_AQI", coAQI, null, "trace", "CO AQI")

        // NO2 Evaluation
        name = "NO2"
        rawVal = aqi.list[0].components.no2
        convertedVal = Math.round(rawVal / 1.88)
        if (convertedVal < 54) {
            aQItemp = (50 / 53) * (convertedVal)
        } else if (convertedVal < 101) {
            aQItemp = (49 / 46) * (convertedVal - 54) + 51
        } else if (convertedVal < 361) {
            aQItemp = (49 / 259) * (convertedVal - 101) + 101
        } else if (convertedVal < 650) {
            aQItemp = (49 / 288) * (convertedVal - 361) + 151
        } else if (convertedVal < 1250) {
            aQItemp = (99 / 599) * (convertedVal - 650) + 201
        } else if (convertedVal < 1650) {
            aQItemp = (99 / 399) * (convertedVal - 1250) + 301
        } else {
            aQItemp = (99 / 399) * (convertedVal - 1650) + 401
        }
        int no2AQI = Math.round(aQItemp)
        if (no2AQI > aQI) {
            aQI = no2AQI
            aQIfactor = name
        }
        sendIfChanged("NO2", convertedVal, "ppb", "trace", "NO2 Concentration")
        sendIfChanged("NO2_AQI", no2AQI, null, "trace", "NO2 AQI")

        // O3 Evaluation
        name = "O3"
        rawVal = aqi.list[0].components.o3
        convertedVal = Math.round(rawVal / 2) / 1000.0
        if (convertedVal < 0.055) {
            aQItemp = (50 / 0.054) * (convertedVal)
        } else if (convertedVal < 0.071) {
            aQItemp = (49 / 0.15) * (convertedVal - 0.054) + 51
        } else if (convertedVal < 0.165) {
            aQItemp = (49 / 0.093) * (convertedVal - 0.071) + 101
        } else if (convertedVal < 0.205) {
            aQItemp = (49 / 0.039) * (convertedVal - 0.165) + 151
        } else if (convertedVal < 0.405) {
            aQItemp = (99 / 0.199) * (convertedVal - 0.205) + 201
        } else if (convertedVal < 0.505) {
            aQItemp = (99 / 0.099) * (convertedVal - 0.405) + 301
        } else {
            aQItemp = (99 / 0.099) * (convertedVal - 0.505) + 401
        }
        int o3AQI = Math.round(aQItemp)
        if (o3AQI > aQI) {
            aQI = o3AQI
            aQIfactor = name
        }
        sendIfChanged("O3", convertedVal, "ppm", "trace", "O3 Concentration")
        sendIfChanged("O3_AQI", o3AQI, null, "trace", "O3 AQI")

        // SO2 Evaluation
        name = "SO2"
        rawVal = aqi.list[0].components.so2
        convertedVal = Math.round(rawVal / 2.62)
        if (convertedVal < 36) {
            aQItemp = (50 / 35) * (convertedVal)
        } else if (convertedVal < 76) {
            aQItemp = (49 / 39) * (convertedVal - 36) + 51
        } else if (convertedVal < 186) {
            aQItemp = (49 / 109) * (convertedVal - 76) + 101
        } else if (convertedVal < 305) {
            aQItemp = (49 / 118) * (convertedVal - 186) + 151
        } else if (convertedVal < 605) {
            aQItemp = (99 / 299) * (convertedVal - 305) + 201
        } else if (convertedVal < 805) {
            aQItemp = (99 / 199) * (convertedVal - 605) + 301
        } else {
            aQItemp = (99 / 199) * (convertedVal - 805) + 401
        }
        int so2AQI = Math.round(aQItemp)
        if (so2AQI > aQI) {
            aQI = so2AQI
            aQIfactor = name
        }
        sendIfChanged("SO2", convertedVal, "ppb", "trace", "SO2 Concentration")
        sendIfChanged("SO2_AQI", so2AQI, null, "trace", "SO2 AQI")

        // PM2.5 Evaluation
        name = "PM2.5"
        convertedVal = Math.round(aqi.list[0].components.pm2_5 * 10) / 10.0
        if (convertedVal < 12.1) {
            aQItemp = (50 / 12.0) * (convertedVal)
        } else if (convertedVal < 35.5) {
            aQItemp = (49 / 23.3) * (convertedVal - 12.1) + 51
        } else if (convertedVal < 55.5) {
            aQItemp = (49 / 19.9) * (convertedVal - 35.5) + 101
        } else if (convertedVal < 150.5) {
            aQItemp = (49 / 94.9) * (convertedVal - 55.5) + 151
        } else if (convertedVal < 250.5) {
            aQItemp = (99 / 99.9) * (convertedVal - 150.5) + 201
        } else if (convertedVal < 350.5) {
            aQItemp = (99 / 99.9) * (convertedVal - 250.5) + 301
        } else {
            aQItemp = (99 / 149.9) * (convertedVal - 350.5) + 401
        }
        int pm25AQI = Math.round(aQItemp)
        if (pm25AQI > aQI) {
            aQI = pm25AQI
            aQIfactor = name
        }
        sendIfChanged("PM2.5", convertedVal, "µg/m³", "trace", "PM2.5 Concentration")
        sendIfChanged("PM2.5_AQI", pm25AQI, null, "trace", "PM2.5 AQI")

        // PM10 Evaluation
        name = "PM10"
        convertedVal = Math.round(aqi.list[0].components.pm10)
        if (convertedVal < 55) {
            aQItemp = (50 / 54) * (convertedVal)
        } else if (convertedVal < 155) {
            aQItemp = (49 / 99) * (convertedVal - 55) + 51
        } else if (convertedVal < 255) {
            aQItemp = (49 / 99) * (convertedVal - 155) + 101
        } else if (convertedVal < 355) {
            aQItemp = (49 / 99) * (convertedVal - 255) + 151
        } else if (convertedVal < 425) {
            aQItemp = (99 / 69) * (convertedVal - 355) + 201
        } else if (convertedVal < 505) {
            aQItemp = (99 / 79) * (convertedVal - 425) + 301
        } else {
            aQItemp = (99 / 99) * (convertedVal - 505) + 401
        }
        int pm10AQI = Math.round(aQItemp)
        if (pm10AQI > aQI) {
            aQI = pm10AQI
            aQIfactor = name
        }
        sendIfChanged("PM10", convertedVal, "µg/m³", "trace", "PM10 Concentration")
        sendIfChanged("PM10_AQI", pm10AQI, null, "trace", "PM10 AQI")

        // Primary AQI & Dominant Pollutant Events (Info log encapsulated inside sendIfChanged)
        String primarySummary = "OpenWeatherMap Air Quality Index is now ${aQI} for ${aQIfactor} pollutant"
        sendIfChanged("airQualityIndex", aQI, null, "info", "airQualityIndex", primarySummary)
        sendIfChanged("dominantPollutant", aQIfactor, null, "info", "dominantPollutant")

        // AQI Color Code Evaluation
        String aQIcolor = "#7E0023"
        if (aQI >= 0 && aQI <= 50) {
            aQIcolor = "#00E400"
        } else if (aQI <= 100) {
            aQIcolor = "#FFFF00"
        } else if (aQI <= 150) {
            aQIcolor = "#FF7E00"
        } else if (aQI <= 200) {
            aQIcolor = "#FF0000"
        } else if (aQI <= 300) {
            aQIcolor = "#8F3F97"
        }
        sendIfChanged("AQIColorCode", aQIcolor, null, "trace", "AQIColorCode")

        // Tile Construction
        String aTile = '<style>h3 {text-align: center;font-size:2.5REM;color:Black;}p {text-align: center;font-size:1REM;color:Black;}</style>'
        aTile += '<div style="overflow-x:auto;"><table style="width: calc(100% - 8px);height: max-content;background-color: '
        aTile += aQIcolor
        aTile += '"><tr><td><h3>'
        aTile += aQI
        aTile += '</h3></td></tr><tr><td><p>'
        aTile += aQIfactor
        aTile += '</p></td></tr></table></div>'

        sendIfChanged("AlertTile", aTile, null, "trace", "AlertTile")
    } else {
        logWarn "OpenWeatherMap API lookup failed with status code: ${resp.getStatus()}"
    }
}

void pollCityName() {
    if (!settings.apiKey) return
    
    String lat = settings.locLat ?: location.latitude
    String lon = settings.locLon ?: location.longitude

    Map paramsGeo = [ 
        uri: "https://api.openweathermap.org/geo/1.0/reverse?lat=${lat}&lon=${lon}&limit=1&appid=${settings.apiKey}", 
        timeout: 20 
    ]
    logDebug "Polling City Name Params: ${paramsGeo}"
    asynchttpGet("pollCityNameHandler", paramsGeo)
}

void pollCityNameHandler(resp, data) {
    if (resp.getStatus() == 200 || resp.getStatus() == 207) {
        def geoData = parseJson(resp.data)
        if (geoData && geoData[0]?.name) {
            String cityName = geoData[0].name
            sendIfChanged("AirNowCity", cityName, null, "debug", "AirNowCity")
        }
    } else {
        logDebug "Geo API failed with status ${resp.getStatus()}"
    }
}

/* =========================================================================================
   SCHEDULING & EVENT GUARD ROUTINES
   ========================================================================================= */

private void schedulePolling(int intervalMin) {
    int m = new Random().nextInt(60)
    
    switch (intervalMin) {
        case 30:
            schedule("0 */30 * ? * * *", "pollData"); break
        case 60:
            schedule("0 ${m} * ? * * *", "pollData"); break
        case 120:
            schedule("0 ${m} */2 ? * * *", "pollData"); break
        case 240:
            schedule("0 ${m} */4 ? * * *", "pollData"); break
        case 480:
            schedule("0 ${m} */8 ? * * *", "pollData"); break
        case 720:
            schedule("0 ${m} */12 ? * * *", "pollData"); break
        case 1440:
            schedule("0 ${m} 0 ? * * *", "pollData"); break
        default:
            schedule("0 */30 * ? * * *", "pollData"); break
    }
    logInfo "Polling scheduled every ${intervalMin} minutes."
}

private void sendIfChanged(final String attribute, final Object value, final String unit = null, final String logLevel = "info", final String customLabel = null, final String customLogMessage = null) {
    final String valStr = value?.toString()
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

@Field static final Map PollIntervalOpts = [
    options: [ 30: "30 minutes", 60: "1 hour", 120: "2 hours", 240: "4 hours", 480: "8 hours", 720: "12 hours", 1440: "24 hours" ]
]