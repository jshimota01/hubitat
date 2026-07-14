/**
 * OpenWeatherMap Multi-Version Weather Driver 2.0 (2.5 / 3.0 / 4.0)
 * Platform: Hubitat Elevation
 * Capabilities: Temperature, Illuminance, Relative Humidity, Ultraviolet Index
 */

metadata {
    definition(name: "OpenWeatherMap Multi-Version Weather Driver 2.0", namespace: "jshimota", author: "James Shimota") {
        capability "Sensor"
        capability "Refresh"
        capability "Initialize"
        
        // - Core Capabilities
        capability "TemperatureMeasurement"
        capability "IlluminanceMeasurement"
        capability "PressureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "UltravioletIndex"

        // - Custom Driver Attributes
        attribute "lastUpdatedDateTime", "number"
        attribute "lastUpdatedDateTimeText", "string"
        attribute "lastResponseCode", "string"
        attribute "betwixt", "string"
        attribute "overrideCity", "string"
        attribute "overrideLongitude", "number"
        attribute "overrideLatitude", "number"
        attribute "pressureUnit", "string"
        attribute "temperatureUnit", "string"
        attribute "windSpeedUnit", "string"
        attribute "illuminanceUnit", "string"
        attribute "humidityUnit", "string"
        
        // - Api specific response attributes
        attribute "apiTimezone", "string"
        attribute "apiTimezoneOffset", "number"
        attribute "apiLatitude", "number"
        attribute "apiLongitude", "number"
        
        // - Alert attributes
        attribute "currentAlert", "string"
        attribute "currentAlertTile", "string"
        attribute "currentAlertDesc", "string"
        attribute "currentAlertSender", "string"
        attribute "currentAlertDescFull", "string"
        
        // - Calculated Solar Angles attributes
        attribute "altitude", "number"
        attribute "azimuth", "number"
        attribute "altitudeText", "string"
        attribute "azimuthText", "string"

        // - Current unique attributes
        attribute "currentSnow", "number"
        attribute "currentRain", "number"
        attribute "currentFeelsLike", "number"
        attribute "currentTemperature", "number"

        // - Current unique derived attributes
        attribute "currentHumidityText", "string"
        attribute "currentIlluminanceText", "string"
        attribute "currentPressureText", "string"
        attribute "currentTemperatureText", "string"
        attribute "currentWindSpeedDescText", "string"
        attribute "currentSnowText", "string"
        attribute "currentRainText", "string"
        attribute "currentTile", "string"
        attribute "currentVisibility", "number"
        attribute "currentTwilightBeginTime", "number"
        attribute "currentSolarNoonTime", "number"
        attribute "currentTwilightEndTime", "number"
        attribute "currentIsDay", "enum", ["true","false"]
        attribute "currentWindDirCardinal", "string"
        attribute "currentWindDirFull", "string"
        attribute "currentWindDirIcon", "string"
        attribute "currentTwilightBeginTimeText", "string"
        attribute "currentSolarNoonTimeText", "string"
        attribute "currentTwilightEndTimeText", "string"
        attribute "currentVisibilityText", "string"
        attribute "currentWeatherSummary", "string"
        attribute "currentWindSummaryText", "string"

        // - Current Condition attributes
        attribute "currentConditionCode", "number"
        attribute "currentConditionType", "string"
        attribute "currentConditionTypeDesc", "string"
        attribute "currentConditionIcon", "string"
        
        // - Current Condition derived attribute
        attribute "currentConditionAltIcon", "string"
        attribute "currentConditionIconImg", "string"
		attribute "currentConditionTypeAltDesc", "string"

        // - Current attributes (commonly found in both current and forecast)
        attribute "currentSunriseTime", "number"
        attribute "currentSunsetTime", "number"
        attribute "currentIlluminance", "number"
        attribute "currentPressure", "number"
        attribute "currentHumidity", "number"
        attribute "currentDewPoint", "number"
        attribute "currentUVI", "number"
        attribute "currentCloudPCT", "number"
        attribute "currentWindGust", "number"
        attribute "currentWindDeg", "number"
        attribute "currentWindSpeed", "number"    
        attribute "currentWindSpeedText", "string"        
        attribute "currentSunriseTimeText", "string"
        attribute "currentSunsetTimeText", "string"
        
        // - Forecast today unique attributes
        attribute "todaySunriseTime", "number"
        attribute "todaySunsetTime", "number"
        attribute "todayMoonriseTime", "number"
        attribute "todayMoonsetTime", "number"
        attribute "todayMoonPhase", "number"
        attribute "todaySummary", "string"
        attribute "todayTempMin", "number"
        attribute "todayTempNight", "number"
        attribute "todayTempMax", "number"
        attribute "todayTempEve", "number"
        attribute "todayTempMorn", "number"
        attribute "todayTempDay", "number"
        attribute "todayFeelsLikeDay", "number"
        attribute "todayFeelsLikeNight", "number"
        attribute "todayFeelsLikeEve", "number"
        attribute "todayFeelsLikeMorn", "number"
        attribute "todayPressure", "number"
        attribute "todayHumidity", "number"
        attribute "todayDewPoint", "number"
        attribute "todayWindGust", "number"
        attribute "todayWindDeg", "number"
        attribute "todayWindSpeed", "number"
        attribute "todayWindSpeedText", "string"
        attribute "todayWindSpeedDescText", "string"
        attribute "todayCloudPCT", "number"
        attribute "todayPOP", "number"
        attribute "todayUVI", "number"
        attribute "todaySunriseTimeText", "string"
        attribute "todaySunsetTimeText", "string"
        attribute "todayMoonriseTimeText", "string"
        attribute "todayMoonsetTimeText", "string"
                
        // - Forecast tomorrow unique attributes
        attribute "tomSunriseTime", "number"
        attribute "tomSunsetTime", "number"
        attribute "tomMoonriseTime", "number"
        attribute "tomMoonsetTime", "number"
        attribute "tomMoonPhase", "number"
        attribute "tomSummary", "string"
        attribute "tomTempMin", "number"
        attribute "tomTempNight", "number"
        attribute "tomTempMax", "number"
        attribute "tomTempEve", "number"
        attribute "tomTempMorn", "number"
        attribute "tomTempDay", "number"
        attribute "tomFeelsLikeDay", "number"
        attribute "tomFeelsLikeNight", "number"
        attribute "tomFeelsLikeEve", "number"
        attribute "tomFeelsLikeMorn", "number"
        attribute "tomPressure", "number"
        attribute "tomHumidity", "number"
        attribute "tomDewPoint", "number"
        attribute "tomWindGust", "number"
        attribute "tomWindDeg", "number"
        attribute "tomWindSpeed", "number"
        attribute "tomWindSpeedText", "string"
        attribute "tomWindSpeedDescText", "string"
        attribute "tomCloudPCT", "number"
        attribute "tomPOP", "number"
        attribute "tomUVI", "number"
        attribute "tomSunriseTimeText", "string"
        attribute "tomSunsetTimeText", "string"
        attribute "tomMoonriseTimeText", "string"
        attribute "tomMoonsetTimeText", "string"

        // - Forecast tomorrow dayafter unique attributes
        attribute "tdaSunriseTime", "number"
        attribute "tdaSunsetTime", "number"
        attribute "tdaMoonriseTime", "number"
        attribute "tdaMoonsetTime", "number"
        attribute "tdaMoonPhase", "number"
        attribute "tdaSummary", "string"
        attribute "tdaTempMin", "number"
        attribute "tdaTempNight", "number"
        attribute "tdaTempMax", "number"
        attribute "tdaTempEve", "number"
        attribute "tdaTempMorn", "number"
        attribute "tdaTempDay", "number"
        attribute "tdaFeelsLikeDay", "number"
        attribute "tdaFeelsLikeNight", "number"
        attribute "tdaFeelsLikeEve", "number"
        attribute "tdaFeelsLikeMorn", "number"
        attribute "tdaPressure", "number"
        attribute "tdaHumidity", "number"
        attribute "tdaDewPoint", "number"
        attribute "tdaWindGust", "number"
        attribute "tdaWindDeg", "number"
        attribute "tdaWindSpeed", "number"
        attribute "tdaWindSpeedText", "string"
        attribute "tdaWindSpeedDescText", "string"
        attribute "tdaCloudPCT", "number"
        attribute "tdaPOP", "number"
        attribute "tdaUVI", "number"
        attribute "tdaSunriseTimeText", "string"
        attribute "tdaSunsetTimeText", "string"
        attribute "tdaMoonriseTimeText", "string"
        attribute "tdaMoonsetTimeText", "string"

        // - Forecast unique derived attributes
        attribute "todayWindDirCardinal", "string"
        attribute "tomWindDirCardinal", "string"
        attribute "tdaWindDirCardinal", "string"
        attribute "todayWindDirFull", "string"
        attribute "tomWindDirFull", "string"
        attribute "tdaWindDirFull", "string"
        attribute "todayWindDirIcon", "string"
        attribute "tomWindDirIcon", "string"
        attribute "tdaWindDirIcon", "string"
        attribute "todayMoonPhaseIcon", "string"
        attribute "tomMoonPhaseIcon", "string"
        attribute "tdaMoonPhaseIcon", "string"
        attribute "todayDate", "number"
        attribute "tomDate", "number"
        attribute "tdaDate", "number"
        attribute "todayDateText", "string"
        attribute "tomDateText", "string"
        attribute "tdaDateText", "string"
        attribute "todayMoonPhaseText", "string"
        attribute "tomMoonPhaseText", "string"
        attribute "tdaMoonPhaseText", "string"

        attribute "3DayForecastTile", "string"
        
        command "clearAllDriverStates"
        command "clearAllDriverAttributes"
        command "pollOWM"
    }

    preferences {
        input name: "apiKey", type: "text", title: "API Key", description: "Enter your OpenWeatherMap API Key here<br><b>Required by OpenWeatherMaps</b>", required: true
        input name: "apiSelection", type: "enum", title: "API Version", options: ["2.5": "One Call 2.5 (Obsolete!)","3.0": "One Call 3.0", "4.0": "One Call 4.0"], defaultValue: "3.0", description: "Select your API Key version here<br><b>Required by OpenWeatherMaps</b><br><i>*Note: 2.5 API is now obsolete as of June 2024</i><br><i>*Note: 4.0 API key uses 3.0 API poll method</i>", required: true
        
        // Optional City field that dynamically overrides latitude/longitude if populated
        input name: "overrideCity", type: "text", title: "Base Override - City", description: "Optional - Will attempt to geo lookup and override <b>ALL</b> latitude/longitude values<br><b>Default:(empty)</b><br><i>EG: Portland, OR or London, UK.<br>*Note: Overrides Latitude/Longitude parameters of Hub <b>AND</b> values configured below</i>", required: false
        input name: "altIconLoc", type: "text", title: "Base Override - Icon Location", description: "Optional - Icon Source Location:<br><i>blank for default OWM location</i>", required: false
        
        // Need to look into this to see why it was implemented - I am not using it
        // input 'luxjitter', 'bool', title: 'Use lux jitter control (rounding)?', required: true, defaultValue: false
        
        input name: "overrideLatitude", type: "decimal", title: "Base Override - Latitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "overrideLongitude", type: "decimal", title: "Base Override - Longitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "altIconsEnable", type: "bool", title: "Base Override - Use Alternative Icons?", description: "Turn ON to use alternate icons (found in csv map within the driver), or OFF to use the standard OpenWeatherMap icons<br><b>Base Override - Icon Location MUST be filled!</b>", defaultValue: false, required: true
        
        // Display Selector Options
        input name: "precisionHumid", type: "enum", title: "Display Decimal Precision - Humidity", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for humidity readings in logging and tiles<br>Default: <b>0</b><br><i>EG: 1, 1.5, 1.55</i>", defaultValue: "0", required: true
        input name: "precisionPrecip", type: "enum", title: "Display Decimal Precision - Precipitation", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for rainfall readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 1, 1.5, 1.55</i>", defaultValue: "2", required: true
        input name: "precisionPress", type: "enum", title: "Display Decimal Precision - Pressure", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for barometer readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 30mb,30.5mb, 30.55mb</i>", defaultValue: "2", required: true
        input name: "precisionSunAngles", type: "enum", title: "Display Decimal Precision - Sun Angles", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for sun angles (altitude and azimuth) readings in logging and tiles<br>Default: <b>0</b><br><i>EG(with Unit): 149°, 149.5°, 149.55°</i>", defaultValue: "0", required: true
        input name: "precisionTemp", type: "enum", title: "Display Decimal Precision - Temperature", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for temperature readings in logging and tiles<br>Default: <b>2</b><br><i>EG(with Unit): 70°F, 70.3°F, 70.55°F</i>", defaultValue: "2", required: true
        input name: "precisionWind", type: "enum", title: "Display Decimal Precision - Wind Speed", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for wind speed readings in logging and tiles<br>Default: <b>2</b><br><i>EG (with Unit): 12 mph, 12.7 mph, 12.77 mph</i>", defaultValue: "2", required: true
        
        // Display Options
        input name: "aPIKeyExposedEnable", type: "bool", title: "Display Options - Expose API Key In Logging?", description: "Enable to show API Key value in log outputs<br>Default: <b>Off</b>", defaultValue: false, required: true
        
        // Display Format Selectors
        input name: "DateTimeForm", type: "enum", title: "Display Format - Date & Time Attributes", options: ["1": "M/d/yyyy h:mm a", "2": "M/d/yyyy HH:mm", "3": "MM/dd/yyyy h:mm a", "4": "MM/dd/yyyy HH:mm", "5": "d/M/yyyy h:mm a", "6": "d/M/yyyy HH:mm", "7": "dd/MM/yyyy h:mm a", "8": "dd/MM/yyyy HH:mm", "9": "yyyy/MM/dd HH:mm", "10": "Unix, UTC (native)"], description: "Choice of date & time format used in tiles and logging for attributes containing both date and time<br>Default: <b>M/d/yyyy h:mm a</b><br><i>EG: 7/12/2026 9:00 AM, 12/7/2026 09:01, 2026/07/12 09:02, 1783859756</i>", defaultValue: "1", required: true
        input name: "DateForm", type: "enum", title: "Display Format - Date Only Attributes", options: ["1": "M/d/yyyy", "3": "MM/dd/yyyy", "5": "d/M/yyyy", "7": "dd/MM/yyyy", "9": "yyyy/MM/dd", "10": "Unix, UTC (native)"], description: "Choice of date format used in tiles and logging for attribute names containing ONLY date<br>Default: <b>M/d/yyyy</b><br><i>EG: 7/12/2026, 12/7/2026, 2026/07/12, 1783859756</i>", defaultValue: "1", required: true
        input name: "TimeForm", type: "enum", title: "Display Format - Time Only Attributes", options: ["1": "h:mm a", "2": "HH:mm", "10": "Unix, UTC (native)"], description: "Choice of time format used in tiles and logging for attribute names containing ONLY time<br>Default: <b>h:mm a</b><br><i>EG: 9:00 AM, 13:00, 1783859756</i>", defaultValue: "1", required: true

        // Display Unit Selectors
        input name: "pressureUnit", type: "enum", title: "Display Unit - Barometric Pressure", options: ["hPa": "Hectopascals (hPa)", "inHg": "Inches of Mercury (inHg)", "kPa": "Kilopascals (kPa)", "mb": "Millibar (mb)", "mmHg": "Millimeters of Mercury (mmHg)", "none": "None (No Unit Suffix)"], description: "Choice of barometer unit used in tiles and logging<br>Default: <b>Inches of Mercury (inHg)</b>", defaultValue: "inHg", required: true
        input name: "humidityUnit", type: "enum", title: "Display Unit - Humidity", options: ["%": "Percent (%)", "%RH": "Percent RH (%RH)", "g/m³": "Absolute Humidity (g/m³)", "g/kg³": "Mixing Ratio (g/kg³)", "none": "None (No Unit Suffix)"], description: "Choice of humidity unit formatting used in tiles and logging<br>Default: <b>Humidity (%)</b>", defaultValue: "%", required: true
        input name: "illuminanceUnit", type: "enum", title: "Display Unit - Illuminance", options: ["lx": "Lux (lx)", "fc": "Foot-candle (fc)", "ph": "Phot (ph)", "none": "None (No Unit Suffix)"], description: "Choice of illuminance unit used in tiles and logging<br>Default: <b>Lux (lx)</b>", defaultValue: "lx", required: true
        input name: "precipUnit", type: "enum", title: "Display Unit - Precipitation (Rain/Snow)", options: ["mmHr": "Millimeters per Hour (mmHr)", "inHr": "Inches per Hour (inHr)", "\"": "Inches (\")", "none": "None (No Unit Suffix)"], description: "Choice of precipitation (both rain and snow) unit formatting used in tiles and logging<br>Default: <b>Inches per Hour (inHr)</b>", defaultValue: "inHr", required: true
        input name: "temperatureUnit", type: "enum", title: "Display Unit - Temperature", options: ["°F": "Fahrenheit (°F)", "°C": "Celsius (°C)", "K": "Kelvin (K)", "none": "None (No Unit Suffix)"], description: "Choice of temperature unit formatting used in tiles and logging<br>Default: <b>Fahrenheit (°F)</b>", defaultValue: "°F", required: true
        input name: "visibilityUnit", type: "enum", title: "Display Unit - Visibility Distance", options: ["m": "Meters (m)", "miles": "Miles (miles)", "ft": "Feet (ft)", "km": "Kilometers (km)", "none": "None (No Unit Suffix)"], description: "Choice of visibility distance unit used in tiles and logging<br>Default: <b>Miles (miles)</b>", defaultValue: "miles", required: true    
        input name: "windSpeedUnit", type: "enum", title: "Display Unit - Wind Speed", options: ["mph": "Miles per Hour (mph)", "kmh": "Kilometers per Hour (km/h)", "kt": "Knots (kt)", "ms": "Meters per Second (m/s)", "none": "None (No Unit Suffix)"], description: "Choice of wind speed unit used in tiles and logging<br>Default: <b>Miles per Hour (mph)</b>", defaultValue: "mph", required: true        

        // Polling Option Dropdown Menu
        input name: "dayInterval", type: "enum", title: "Polling - Daytime Interval", options: ["manual": "Manual Only (via pollOWM command)", "15": "15 Minutes", "30": "30 Minutes", "60": "1 Hour", "180": "3 Hours"], description: "Polling frequency to OWM during daytime (between sunrise and sunset)<br>Default: <b>30 Minutes</b>", defaultValue: "30", required: true
        input name: "nightInterval", type: "enum", title: "Polling - Nighttime Interval", options: ["manual": "Manual Only (via pollOWM command)", "15": "15 Minutes", "30": "30 Minutes", "60": "1 Hour", "180": "3 Hours"], description: "Polling frequency of OWM during nighttime (between sunset and sunrise)<br>Default: <b>1 Hour</b>", defaultValue: "60", required: true

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

def installed() {
    logInfo "Driver Installed. Turning on initial debug logging for 30 minutes."
    device.updateSetting("logDebugEnable", [type: "bool", value: true])
    
    // Push defaults to settings so they display properly on the driver page fields
    ["humidityUnit": "%RH", "pressureUnit": "inHg", "illuminanceUnit": "lx", "temperatureUnit": "°F", "windSpeedUnit": "mph", "precipUnit": "inHr", "visibilityUnit": "miles"].each { k, v ->
        if (settings[k] == null) device.updateSetting(k, [type: "enum", value: v])
        // Update current device attributes to reflect defaults on initial install
        sendIfChanged(name: k, value: v)
    }

    initialize()
}

def updated() {
    logInfo "Preferences updated. Running initialization ..."
    
    // Ensure changed unit selections immediately update device attributes
    ["humidityUnit": "%RH", "pressureUnit": "inHg", "illuminanceUnit": "lx", "temperatureUnit": "°F", "windSpeedUnit": "mph", "precipUnit": "inHr", "visibilityUnit": "miles"].each { k, v ->
        sendIfChanged(name: k, value: settings[k] ?: v)
    }
    
    // --- FORCE REGISTER CORE & SYSTEM ATTRIBUTES ---
    sendEvent(name: "temperature", value: 0, unit: settings.temperatureUnit ?: "°F")
    sendEvent(name: "pressure", value: 0, unit: settings.pressureUnit ?: "inHg")
    sendEvent(name: "illuminance", value: 0, unit: settings.illuminanceUnit ?: "lx")
    sendEvent(name: "humidity", value: 0, unit: settings.humidityUnit ?: "%")
    sendEvent(name: "ultravioletIndex", value: 0)

    // Preset placeholder strings to ensure clean execution bounds
    ["todayMoonPhaseText", "tomMoonPhaseText", "tdaMoonPhaseText"].each { sendIfChanged(name: it, value: "--") }

    if (!settings.altIconLoc || settings.altIconLoc.trim() == "") {
        if (settings.altIconsEnable == true) {
            logWarn "Alternative Icon Location is empty/default. Forcing 'Use Alternative Icons' to OFF for safety."
            device.updateSetting("altIconsEnable", [type: "bool", value: false])
        }
    }
    initialize()
}

def initialize() {
    logDebug "Clearing all scheduled jobs ..."
    unschedule()
    logInfo "Initializing driver ..." 
  
    if (logDebugEnable == true) {
        runIn(1800, "disableDebugLogging")
    }

    // Mark that we are currently doing our initial setup
    state.isInitializing = true
    // Fire the very first poll instantly
    runIn(1, "scheduledPoll")
}

void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    // Clears all data stored in the state map
    state.clear() 
    logInfo "All states have been cleared."
}

void clearAllDriverAttributes() {
    logInfo "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
    logInfo "All attributes have been cleared."
}

def scheduledPoll() {
    // Scheduled background poll sequence initiated.
    logDebug "Scheduled background poll sequence initiated."
    pollOWM("schedule")
}

def refresh() {
    // Refresh triggered via schedule or button press.
    logDebug "Refresh triggered via schedule or button press."
    
    // Safety check preference values
    if (!apiKey) {
        logWarn "Execution halted: API Key entry is missing!"
        return
    }
    
    // Execution to owm Poll logic block, alerting that it was invoked by refresh
    pollOWM("refresh") 
}

def pollOWM(String type = "manual") {
    // Evaluation of execution triggers using logInfo
    switch(type) {
        case "refresh": logInfo "pollOWM run on manual Refresh"; break
        case "schedule": logInfo "polling OpenWeatherMaps API on schedule"; break
        case "manual":
        default: logInfo "PollOWM run manually"; break
    }

    // pollOWM triggered. Evaluating location coordinates...
    logDebug "pollOWM triggered. Evaluating location coordinates..."
    
    // Ensure state variables exist by evaluating coordinate overrides
    calcLonLatCityState()
    
    if(state.usedLatitude == null || state.usedLongitude == null) {
        logWarn "pollOWM aborted: Valid coordinates are missing (Lat: ${state.usedLatitude}, Lon: ${state.usedLongitude})"
        return
    }

    // Execution to check sun position for use in calcBetwixt and calcDayState blocks
    BigDecimal currentAlt = calcSunPosition()
    
    // Execution for certain variables used in parsed data returned from pollOWMAPI
    calcBetwixtState(currentAlt)
    calcIsDayState(currentAlt)
    
    // Resolve path safely and save it to state before API execution
    state.iconBasePath = calcIconBasePath(settings.altIconLoc)
    
    // Fire off the API poll sequence
    pollOWMAPI()
}

private void updateDynamicSchedules(long sunriseEpoch, long sunsetEpoch) {
    // Always clear any previous scheduledPoll jobs to ensure only one is ever pending
    unschedule("scheduledPoll")

    if (dayInterval == "manual" && nightInterval == "manual") {
        logInfo "Both daytime and nighttime polling are set to MANUAL. Dynamic scheduling skipped."
        return
    }

    long nowTime = now() / 1000L
    boolean isDay = (nowTime >= sunriseEpoch && nowTime < sunsetEpoch)
    String currentInterval = isDay ? dayInterval : nightInterval

    int delaySeconds = 0

    if (currentInterval == "manual") {
        // If the current period is manual, schedule exactly for the next transition boundary
        if (isDay) {
            delaySeconds = (int)(sunsetEpoch - nowTime)
            logDebug "Daytime polling is MANUAL. Scheduling next poll at sunset in ${delaySeconds} seconds."
        } else {
            long nextSunrise = (nowTime > sunriseEpoch) ? (sunriseEpoch + 86400) : sunriseEpoch
            delaySeconds = (int)(nextSunrise - nowTime)
            logDebug "Nighttime polling is MANUAL. Scheduling next poll at sunrise in ${delaySeconds} seconds."
        }
    } else {
        // Otherwise, simply look up the active period's interval in minutes
        int intervalMinutes = currentInterval?.isInteger() ? currentInterval.toInteger() : 30
        delaySeconds = intervalMinutes * 60
        logDebug "Scheduling next background poll in ${intervalMinutes} minutes (${delaySeconds} seconds) via runIn."
    }

    // Guard rail against negative or zero delays
    if (delaySeconds <= 0) delaySeconds = 1800 
    runIn(delaySeconds, "scheduledPoll", [overwrite: true])
}

// Add this handler to unpack scheduled runIn calls safely
void scheduledTextValue(List dataList) {
    logDebug "Unpacking scheduled calcTextValue arguments safely..."
    if (dataList && dataList.size() >= 9) {
        calcTextValue(
            dataList[0] != null ? dataList[0].toBigDecimal() : null,
            dataList[1] != null ? dataList[1].toBigDecimal() : null,
            dataList[2] != null ? dataList[2].toBigDecimal() : null,
            dataList[3] != null ? dataList[3].toBigDecimal() : null,
            dataList[4] != null ? dataList[4].toBigDecimal() : null,
            dataList[5] instanceof Map ? dataList[5] : [:],
            dataList[6] instanceof Map ? dataList[6] : [:],
            dataList[7] instanceof Map ? dataList[7] : [:],
            dataList[8] instanceof Map ? dataList[8] : [:]
        )
    } else {
        logWarn "Scheduled calcTextValue skipped: Argument list was incomplete."
    }
}

// --- Modular Tile Generator Routine ---
void generateTiles(Map currentData = [:], Map todayData = [:], Map tomData = [:], Map tdaData = [:]) {
    // =========================================================================
    // SECTION 1: currentTile (Optimized & Relaxed CSS Style)
    // =========================================================================
    String tUnit = settings.temperatureUnit ?: "°F"
    String wUnit = settings.windSpeedUnit ?: "mph"
    String cityName = state.usedCity ?: "Local Area"
    
    def icon, temp, cond, hi, lo, hum, wind

    if (currentData && todayData) {
        icon = currentData.weather && currentData.weather[0] ? currentData.weather[0].icon : "01d"
        temp = currentData.temp != null ? convertKelvin(currentData.temp) : "--"
        cond = currentData.weather && currentData.weather[0] ? currentData.weather[0].description : "--"
        hi   = todayData.temp?.max != null ? convertKelvin(todayData.temp.max) : "--"
        lo   = todayData.temp?.min != null ? convertKelvin(todayData.temp.min) : "--"
        hum  = currentData.humidity != null ? convertHumidity(currentData.humidity) : "--"
        wind = currentData.wind_speed != null ? convertWindSpeed(currentData.wind_speed) : "--"
    } else {
        icon = device.currentValue("currentConditionIcon") ?: "01d"
        temp = device.currentValue("currentTemperature") ?: "--"
        cond = device.currentValue("currentConditionTypeDesc") ?: "--"
        hi   = device.currentValue("todayTempMax") ?: "--"
        lo   = device.currentValue("todayTempMin") ?: "--"
        hum  = device.currentValue("currentHumidity") ?: "--"
        wind = device.currentValue("currentWindSpeed") ?: "--"
    }

    String windIconDisplay = "💨"
    if (settings.altIconsEnable == true) {
        String windIconUrl = device.currentValue("currentWindDirIcon") ?: ""
        if (windIconUrl != "") {
            windIconDisplay = "<img src='${windIconUrl}' style='height:1.1em;vertical-align:middle;margin-right:2px;'>"
        }
    }

    // Relaxed layout framework maximizing native browser rendering over hyper-specific CSS blocks
    String currentBodyHtml = "<div style='background:rgba(30,30,40,0.65);border-radius:10px;padding:6px;color:#fff;font-family:sans-serif;text-align:center;line-height:1.25;'><div style='font-weight:bold;font-size:1.1em;'>${cityName} - Currently</div><div style='display:flex;align-items:center;justify-content:center;gap:10px;'><img src='https://openweathermap.org/img/wn/${icon}.png' style='width:42px;height:42px;'> <span style='font-size:2.2em;font-weight:bold;'>${temp}${tUnit}</span></div><div style='color:#ddd;text-transform:capitalize;'>${cond}</div><div style='color:#aaa;margin:2px 0;'>H: ${hi}° | L: ${lo}°</div><div style='display:flex;justify-content:space-between;border-top:1px solid rgba(255,255,255,0.15);padding-top:4px;margin-top:2px;color:#bbb;'><span>💧 ${hum}%</span><span>${windIconDisplay}${wind} ${wUnit}</span></div>"
    int currentLenEstimate = currentBodyHtml.length() + "<div style='font-size:0.75em;color:#777;margin-top:3px;'>999 chars</div></div>".length()
    sendIfChanged(name: "currentTile", value: currentBodyHtml + "<div style='font-size:0.75em;color:#777;margin-top:3px;'>${currentLenEstimate} chars</div></div>")

    // =========================================================================
    // SECTION 2: 3DayForecastTile (forecastTile)
    // =========================================================================
    long epochMsNow = now()
    String day1Name = new Date(epochMsNow + 86400000L).format("EEEE", location.timeZone)
    String day2Name = new Date(epochMsNow + 172800000L).format("EEEE", location.timeZone)

    def buildForecastCol = { dayLabel, prefix, Map sourceMap ->
        def fHi, fLo, fPop, fSummary
        if (sourceMap && sourceMap.temp) {
            fHi = sourceMap.temp.max != null ? convertKelvin(sourceMap.temp.max) : "--"
            fLo = sourceMap.temp.min != null ? convertKelvin(sourceMap.temp.min) : "--"
            fPop = sourceMap.pop != null ? sourceMap.pop : 0
            fSummary = sourceMap.summary != null ? sourceMap.summary : "Clear"
        } else {
            fHi = device.currentValue("${prefix}TempMax") ?: "--"
            fLo = device.currentValue("${prefix}TempMin") ?: "--"
            fPop = device.currentValue("${prefix}POP") ?: 0
            fSummary = device.currentValue("${prefix}Summary") ?: "Clear"
        }
        int popPct = (fPop.toBigDecimal() * 100).intValue()
        return "<div style='flex:1; display:flex; flex-direction:column; justify-content:space-between; align-items:center; padding:0 4px; min-width:0;'><div style='font-size:0.8em; font-weight:bold; color:#fff;'>${dayLabel}</div><div style='font-size:1.1em; font-weight:bold; margin:4px 0;'>${fHi}°<span style='font-size:0.75em; color:#aaa; font-weight:normal;'> / ${fLo}°</span></div><div style='font-size:0.7em; color:#ddd; font-weight:600; text-transform:capitalize; text-overflow:ellipsis; white-space:nowrap; overflow:hidden; width:100%; margin-bottom:4px;'>${fSummary}</div><div style='font-size:0.7em; color:#bbb;'>${popPct > 0 ? "💧 ${popPct}%" : "☀️ 0%"}</div></div>"
    }

    String forecastBodyHtml = "<div style='display:flex; flex-direction:column; justify-content:space-between; height:100%; padding:10px 6px; box-sizing:border-box; background:rgba(30,30,40,0.65); border-radius:12px; color:#fff; font-family:sans-serif; line-height:1.2; text-align:center;'><div style='display:flex; flex-direction:row; justify-content:space-between; gap:4px; flex-grow:1;'>" +
                          buildForecastCol("Today", "today", todayData) +
                          "<div style='border-left:1px solid rgba(255,255,255,0.1); height:100%;'></div>" +
                          buildForecastCol(day1Name, "tom", tomData) +
                          "<div style='border-left:1px solid rgba(255,255,255,0.1); height:100%;'></div>" +
                          buildForecastCol(day2Name, "tda", tdaData) +
                          "</div><div style='border-top:1px solid rgba(255,255,255,0.1); padding-top:4px; margin-top:4px;'>"

    int forecastLenEstimate = forecastBodyHtml.length() + "999 chars long</div></div>".length()
    sendIfChanged(name: "3DayForecastTile", value: forecastBodyHtml + "<span style='font-size:0.6em;color:#888;'>${forecastLenEstimate} chars long</span></div></div>")
}

private void pollOWMAPI() {
    logDebug "Building OpenWeatherMap API HTTP Request..."
    def lat = state.usedLatitude
    def lon = state.usedLongitude
    def version = settings.apiSelection ?: "3.0"
    
    if (!apiKey) {
        logError "API request aborted: Missing API Key."
        return
    }
    
    // Structure endpoint variants depending on driver apiSelection context
    String apiUrl = ""
    switch(version) {
        case "2.5":
            apiUrl = "https://api.openweathermap.org/data/2.5/onecall?lat=${lat}&lon=${lon}&exclude=minutely,hourly&appid=${apiKey}"
            break
        case "3.0":
        case "4.0": // Note: 4.0 API key uses 3.0 API poll method
            apiUrl = "https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${lon}&exclude=minutely,hourly&appid=${apiKey}"
            break
        default:
            logError "Unknown API Version Selection: ${version}"
            return
    }
    
    def params = [uri: apiUrl, contentType: "application/json", timeout: 15]
    
	if (!settings.aPIKeyExposedEnable) {
		logDebug "Polling OpenWeatherMap via URL: ${apiUrl.replaceAll(/appid=[^&]+/, 'appid=***')}"
    } else {
		logDebug "Polling OpenWeatherMap via URL: ${apiUrl}"
	}
    try {
        httpGet(params) { response ->
            if (response.status == 200 && response.data) {
                sendIfChanged(name: "lastResponseCode", value: response.status.toString())
                long currentUnixEpochSeconds = now() / 1000L
                sendIfChanged(name: "lastUpdatedDateTime", value: currentUnixEpochSeconds)  
    
                // --- FIX: Respect user preference formatting immediately on API response ---
                String chosenFormat = settings.DateTimeForm ?: "1"
                if (chosenFormat == "10") {
                    sendIfChanged(name: "lastUpdatedDateTimeText", value: "${currentUnixEpochSeconds}")
                } else {
                    Map formattedMap = convertDateTimeFormat(currentUnixEpochSeconds, chosenFormat)
                    sendIfChanged(name: "lastUpdatedDateTimeText", value: formattedMap.dateTime)
                }                
                // Route the payload to the custom data extractor
                parseOWMData(response.data)
            } else {
                logError "OWM API call failed with status code: ${response.status}"
                sendIfChanged(name: "lastResponseCode", value: response.status.toString())
            }
        }
    } catch (Exception e) {
        logError "OWM API request failed: ${e.message}"
        if (e.hasProperty("statusCode")) {
            sendIfChanged(name: "lastResponseCode", value: "${e.statusCode}")
        }
    }
}

private void parseOWMData(Map json) {
    if (!json) {
        logWarn "parseOWMData received an empty payload map."
        return 
    }
    
    logDebug "Parsing newly received OpenWeatherMap response data structure..."
    String calculatedCityAttr = state.usedCity ?: "Local Area"
    String iconBasePath = state.iconBasePath ?: "https://tinyurl.com/icnqz/"
    
    if (json.lat != null) sendIfChanged(name: "apiLatitude", value: json.lat)
    if (json.lon != null) sendIfChanged(name: "apiLongitude", value: json.lon)
    if (json.timezone != null) sendIfChanged(name: "apiTimezone", value: json.timezone)
    if (json.timezone_offset != null) sendIfChanged(name: "apiTimezoneOffset", value: json.timezone_offset)

    long liveSunrise = 0
    long liveSunset = 0

    def currentData = json.current ?: [:]
    if (currentData) {
        logTrace "Current weather data payload extracted successfully."
        if (currentData.sunrise) {
            liveSunrise = currentData.sunrise.toLong()
            state.todaySunriseEpoch = liveSunrise
        }
        if (currentData.sunset) {
            liveSunset = currentData.sunset.toLong()
            state.todaySunsetEpoch = liveSunset
        }
        currentData["calculatedRain"] = currentData.rain?.getAt("1h") != null ? currentData.rain["1h"] : 0.00
        currentData["calculatedSnow"] = currentData.snow?.getAt("1h") != null ? currentData.snow["1h"] : 0.00
    }
    
    // Process Daily forecast arrays safely
    def dailyList = json.daily ?: []
    
    // Gather Today, Tomorrow, and Day After data maps
    def data0 = dailyList.size() > 0 ? dailyList[0] : [:]
    def data1 = dailyList.size() > 1 ? dailyList[1] : [:]
    def data2 = dailyList.size() > 2 ? dailyList[2] : [:]
    
    // --- POPULATE NEW DATE ATTRIBUTES ---
    if (data0 && data0.dt != null) sendIfChanged(name: "todayDate", value: data0.dt)
    if (data1 && data1.dt != null) sendIfChanged(name: "tomDate", value: data1.dt)
    if (data2 && data2.dt != null) sendIfChanged(name: "tdaDate", value: data2.dt)
    
    // Route all isolated datasets into the custom event dispatcher
    sendOWMData(currentData, data0, data1, data2)
    
    if (liveSunrise > 0 && liveSunset > 0) {
        updateDynamicSchedules(liveSunrise, liveSunset)
    }
    calcAlertsState(json, calculatedCityAttr, iconBasePath)
    calcCurrentTwilight()
    
    BigDecimal currentAlt = state.sunAltitude != null ? state.sunAltitude.toBigDecimal() : (device.currentValue("altitude")?.toBigDecimal() ?: 0.0)
    calcBetwixtState(currentAlt, liveSunrise, liveSunset)
    generateTiles(currentData, data0, data1, data2)

    // --- FIX PATH: If initializing, force a brief 2-second out-of-band delayed execution
    if (state.isInitializing == true) {
        state.isInitializing = false
        logDebug "Driver is initializing. Scheduling secondary out-of-band text refresh to prevent DB race conditions."
        
        // Define variables locally so they can be bundled into delayed schedule execution safely
        currentAlt = state.sunAltitude != null ? state.sunAltitude.toBigDecimal() : (device.currentValue("altitude")?.toBigDecimal() ?: 0.0)
        def liveClouds = currentData?.clouds != null ? currentData.clouds : null
        
        BigDecimal freshLux = calcCurrentIlluminance(currentAlt, liveClouds)
        BigDecimal freshTemp = currentData?.temp != null ? convertKelvin(currentData.temp) : null
        BigDecimal freshPress = currentData?.pressure != null ? convertPressure(currentData.pressure) : null
        BigDecimal freshWind = currentData?.wind_speed != null ? convertWindSpeed(currentData.wind_speed) : null
        BigDecimal freshHum = currentData?.humidity != null ? convertHumidity(currentData.humidity) : null

        // Pass safely instantiated local values to out-of-band thread
        runIn(2, "scheduledTextValue", [data: [freshLux, freshTemp, freshPress, freshWind, freshHum, currentData, data0, data1, data2]])
        runIn(2, "calcMoonPhaseIconWithText", [data: [data0, data1, data2]])
    }
}

private void sendOWMData(Map current, Map today, Map tom, Map tda) {
    logDebug "sendOWMData initiated. Dispatching events to device attributes..."
    logTrace "sendOWMData Current section starting"

    // ==========================================
    // 1. CURRENT DATA DISPATCHES
    // ==========================================
    if (current) {
        // 0. handle the hourly rain and snow values if provided by OWM Api
        def rawRain = current.calculatedRain ?: 0.00
        def rawSnow = current.calculatedSnow ?: 0.00
        
        // Process through convertPrecip() which handles unit conversion and precision
        def rainVal = convertPrecip(rawRain)
        def snowVal = convertPrecip(rawSnow)
        
        // Handle 'none' display unit preference gracefully
        String preUnit = (settings.precipUnit == "none" || settings.precipUnit == null) ? "" : "${settings.precipUnit}"
        
        // --- FIX: Dynamic spacing based on precipUnit selection ---
        String spaceStr = (preUnit == '"') ? "" : " "

        // Cleaned up sendIfChanged calls
        sendIfChanged(name: "currentRain", value: rainVal)
        sendIfChanged(name: "currentSnow", value: snowVal)
        sendIfChanged(name: "currentRainText", value: "${rainVal}${spaceStr}${preUnit}")
        sendIfChanged(name: "currentSnowText", value: "${snowVal}${spaceStr}${preUnit}")
        
        // Current sunrise/sunset
        if (current.sunrise != null) sendIfChanged(name: "currentSunriseTime", value: current.sunrise)
        if (current.sunset  != null) sendIfChanged(name: "currentSunsetTime",  value: current.sunset)
    
        // EXAMPLE sendIfChanged(name: "currentIlluminanceText", value: "${luxVal} ${iUnit}")
        if (current.temp != null) {
            BigDecimal calcTemp = convertKelvin(current.temp)
            sendIfChanged(name: "currentTemperature", value: calcTemp)
            sendIfChanged(name: "temperature", value: calcTemp) // Map to Core Capability
        }
        if (current.feels_like != null) sendIfChanged(name: "currentFeelsLike", value: convertKelvin(current.feels_like))
        if (current.dew_point != null) sendIfChanged(name: "currentDewPoint", value: convertKelvin(current.dew_point))
        if (current.humidity != null) {
            BigDecimal calcHumidity = convertHumidity(current.humidity)
            sendIfChanged(name: "currentHumidity", value: calcHumidity)
            sendIfChanged(name: "humidity", value: calcHumidity) // Map to Core Capability
        }
        if (current.pressure != null) {
            BigDecimal calcPressure = convertPressure(current.pressure)
            sendIfChanged(name: "currentPressure", value: calcPressure)
            sendIfChanged(name: "pressure", value: calcPressure) // Map to Core Capability
        }
        if (current.uvi != null) {
            sendIfChanged(name: "currentUVI", value: current.uvi)
            sendIfChanged(name: "ultravioletIndex", value: current.uvi) // Map to Core Capability
        }
        if (current.visibility != null) {
            BigDecimal visDist = convertVisibilityDistance(current.visibility)
            sendIfChanged(name: "currentVisibility", value: visDist)
            sendIfChanged(name: "currentVisibilityText", value: "${visDist} ${visibilityUnit}")
        }
        if (current.clouds != null) sendIfChanged(name: "currentCloudPCT", value: current.clouds)
               
        // Wind Speed elements converted from m/s using user selection preference logic
        if (current.wind_speed != null) sendIfChanged(name: "currentWindSpeed", value: convertWindSpeed(current.wind_speed))
        if (current.wind_deg != null) sendIfChanged(name: "currentWindDeg", value: current.wind_deg)
        if (current.wind_gust != null) sendIfChanged(name: "currentWindGust", value: convertWindSpeed(current.wind_gust))
        if (current.wind_deg != null) {
            Map wDir = convertWindDirectionState(current.wind_deg)
            sendIfChanged(name: "currentWindDirCardinal", value: wDir.cardinal)
            sendIfChanged(name: "currentWindDirFull", value: wDir.full)
            sendIfChanged(name: "currentWindDirIcon", value: wDir.iconUrl)
        }       
        
        // Handle nested weather condition arrays safely if available
        if (current.weather && current.weather[0]) {
            Integer code = current.weather[0].id != null ? current.weather[0].id.toInteger() : 0
            sendIfChanged(name: "currentConditionCode", value: code)
            sendIfChanged(name: "currentConditionType", value: current.weather[0].main)
            sendIfChanged(name: "currentConditionTypeDesc", value: current.weather[0].description)
            sendIfChanged(name: "currentConditionIcon", value: current.weather[0].icon)
            
            // 1. Fetch condition details from the lookup map
            Map condDetails = lookupConditionDetails(code)
            sendIfChanged(name: "currentConditionTypeAltDesc", value: condDetails.desc)

            // 2. Determine and apply alternate icons if enabled
            if (settings.altIconsEnable == true) {
                // Determine if it is day or night based on the attribute we updated earlier
                boolean isDay = (device.currentValue("currentIsDay") == "true")
                
                // Pick the correct filename from your lookup map
                String iconFilename = isDay ? condDetails.altDay : condDetails.altNight
                
                // Construct the full path using your driver's base icon path configuration
                String basePath = state.iconBasePath ?: "https://tinyurl.com/icnqz/"
                String fullAltIconUrl = "${basePath}${iconFilename}"
                
                sendIfChanged(name: "currentConditionAltIcon", value: fullAltIconUrl)
                logDebug "Alternate Icon Enabled. Chosen file: ${iconFilename} | Full URL: ${fullAltIconUrl}"
            } else {
                // Fallback: Clear it or set to standard OpenWeatherMap icon URL if altIcons is disabled
                String omiIcon = current.weather[0].icon ?: "01d"
                sendIfChanged(name: "currentConditionAltIcon", value: "https://openweathermap.org/img/wn/${omiIcon}.png")
            }
		}
        logTrace "sendOWMData Current section ended"
    }

    // ==========================================
    // 2. REFRACTORED FORECAST DISPATCHES (Today, Tom, Tda)
    // ==========================================
    def forecastDays = [
        [prefix: "today", map: today],
        [prefix: "tom",   map: tom],
        [prefix: "tda",   map: tda]
    ]

    forecastDays.each { day ->
        String prefix = day.prefix
        Map data = day.map

        if (data) {
            logTrace "sendOWMData ${prefix} section started"

            if (data.sunrise != null)    sendIfChanged(name: "${prefix}SunriseTime", value: data.sunrise)
            if (data.sunset != null)     sendIfChanged(name: "${prefix}SunsetTime",  value: data.sunset)
            if (data.pop != null)        sendIfChanged(name: "${prefix}POP", value: data.pop)
            if (data.summary != null)    sendIfChanged(name: "${prefix}Summary", value: data.summary)
            if (data.moonrise != null)   sendIfChanged(name: "${prefix}MoonriseTime", value: data.moonrise)
            if (data.moonset != null)    sendIfChanged(name: "${prefix}MoonsetTime", value: data.moonset)
            if (data.moon_phase != null) sendIfChanged(name: "${prefix}MoonPhase", value: data.moon_phase)
            
            // Nested Temperature structures converted dynamically via user preferences layout
            if (data.temp) {
                ["min", "max", "day", "night", "eve", "morn"].each { tKey ->
                    if (data.temp[tKey] != null) {
                        sendIfChanged(name: "${prefix}Temp${tKey.capitalize()}", value: convertKelvin(data.temp[tKey]))
                    }
                }
            }

            // Nested feels like temperature structures converted dynamically via user preferences layout
            if (data.feels_like) {
                ["day", "night", "eve", "morn"].each { fKey ->
                    if (data.feels_like[fKey] != null) {
                        sendIfChanged(name: "${prefix}FeelsLike${fKey.capitalize()}", value: convertKelvin(data.feels_like[fKey]))
                    }
                }
            }

            // shared values found in current and forecasts
            if (data.pressure != null)   sendIfChanged(name: "${prefix}Pressure", value: convertPressure(data.pressure))
            if (data.humidity != null)   sendIfChanged(name: "${prefix}Humidity", value: convertHumidity(data.humidity))
            if (data.dew_point != null)  sendIfChanged(name: "${prefix}DewPoint", value: convertKelvin(data.dew_point))
            if (data.uvi != null)        sendIfChanged(name: "${prefix}UVI", value: data.uvi)
            if (data.clouds != null)     sendIfChanged(name: "${prefix}CloudPCT", value: data.clouds)
            if (data.wind_gust != null)  sendIfChanged(name: "${prefix}WindGust", value: convertWindSpeed(data.wind_gust))
            if (data.wind_deg != null)   sendIfChanged(name: "${prefix}WindDeg", value: data.wind_deg)
            
            // Wind Speed elements converted from m/s using user selection preference logic
            if (data.wind_speed != null) sendIfChanged(name: "${prefix}WindSpeed", value: convertWindSpeed(data.wind_speed))

            if (data.wind_deg != null) {
                Map wDir = convertWindDirectionState(data.wind_deg)
                sendIfChanged(name: "${prefix}WindDirCardinal", value: wDir.cardinal)
                sendIfChanged(name: "${prefix}WindDirFull", value: wDir.full)
                sendIfChanged(name: "${prefix}WindDirIcon", value: wDir.iconUrl)
            }
            logTrace "sendOWMData ${prefix} section ended"
        }
    }

    // ==========================================
    // 3. ILLUMINANCE & OUT-OF-BAND MATH EXECUTION
    // ==========================================
    // Trigger the illuminance calculation right before concluding the lifecycle dispatch
    BigDecimal currentAlt = state.sunAltitude != null ? state.sunAltitude.toBigDecimal() : (device.currentValue("altitude")?.toBigDecimal() ?: 0.0)
    
    // --- FIXED: Pass the in-memory current cloud layer to avoid initialization null-outs ---
    def liveClouds = current?.clouds != null ? current.clouds : null
    BigDecimal freshLux = calcCurrentIlluminance(currentAlt, liveClouds)
    
    // Extract the localized in-memory blocks to bypass async DB lag during initialization
    BigDecimal freshTemp = current?.temp != null ? convertKelvin(current.temp) : null
    BigDecimal freshPress = current?.pressure != null ? convertPressure(current.pressure) : null
    BigDecimal freshWind = current?.wind_speed != null ? convertWindSpeed(current.wind_speed) : null
    BigDecimal freshHum = current?.humidity != null ? convertHumidity(current.humidity) : null

    // Pass the local variables to keep the text fields populated instantly on install
    calcTextValue(freshLux, freshTemp, freshPress, freshWind, freshHum, current, today, tom, tda)
    // uses api response data so it works on initialization // uses api response data so it works on
    calcMoonPhaseIconWithText(today, tom, tda)
    logDebug "sendOWMData event parsing complete."
}

// -----------------CONVERTERS

private Map convertDateTimeFormat(def epochSeconds, String formatOption) {
    if (epochSeconds == null) {
        logDebug "convertDateTimeFormat received a null epoch timestamp value."
        return [dateTime: "--", date: "--", time: "--"]
    }
    
    // Convert epoch seconds to milliseconds for Java/Groovy Date tracking
    long msecs = epochSeconds.toLong() * 1000L
    Date dateObject = new Date(msecs)
    
    // --- BUG FIX: Use the remote OWM local timezone offset instead of only the hub's local zone ---
    TimeZone tz = location.timeZone ?: TimeZone.getDefault()
    if (device.currentValue("apiTimezone") != null) {
        tz = TimeZone.getTimeZone(device.currentValue("apiTimezone").toString())
    }
    
    String DTFormat = ""
    String dateFormat = ""
    String timeFormat = ""
    
    switch(formatOption?.toString()) {
        case '1': DTFormat = 'M/d/yyyy h:mm a';  dateFormat = 'M/d/yyyy';   timeFormat = 'h:mm a'; break
        case '2': DTFormat = 'M/d/yyyy HH:mm';   dateFormat = 'M/d/yyyy';   timeFormat = 'HH:mm'; break
        case '3': DTFormat = 'MM/dd/yyyy h:mm a'; dateFormat = 'MM/dd/yyyy'; timeFormat = 'h:mm a'; break
        case '4': DTFormat = 'MM/dd/yyyy HH:mm';  dateFormat = 'MM/dd/yyyy'; timeFormat = 'HH:mm'; break
        case '5': DTFormat = 'd/M/yyyy h:mm a';  dateFormat = 'd/M/yyyy';   timeFormat = 'h:mm a'; break
        case '6': DTFormat = 'd/M/yyyy HH:mm';   dateFormat = 'd/M/yyyy';   timeFormat = 'HH:mm'; break
        case '7': DTFormat = 'dd/MM/yyyy h:mm a'; dateFormat = 'dd/MM/yyyy'; timeFormat = 'h:mm a'; break
        case '8': DTFormat = 'dd/MM/yyyy HH:mm';  dateFormat = 'dd/MM/yyyy'; timeFormat = 'HH:mm'; break
        case '9': DTFormat = 'yyyy/MM/dd HH:mm';  dateFormat = 'yyyy/MM/dd'; timeFormat = 'HH:mm'; break
        default:  DTFormat = 'M/d/yyyy h:mm a';  dateFormat = 'M/d/yyyy';   timeFormat = 'h:mm a'; break
    }
    
    // Format the date target strings utilizing Hubitat's local timezone rules safely
    try {
        return [
            dateTime: dateObject.format(DTFormat, tz),
            date: dateObject.format(dateFormat, tz),
            time: dateObject.format(timeFormat, tz)
        ]
    } catch (Exception e) {
        logError "Exception occurred during convertDateTimeFormat conversion execution: ${e.message}"
        return [dateTime: "--", date: "--", time: "--"]
    }
}

private BigDecimal convertVisibilityDistance(def rawVisibility) {
    BigDecimal meters = rawVisibility?.toBigDecimal()
    if (meters == null) return 0.0
    
    BigDecimal converted = meters
    String targetUnit = settings.visibilityUnit ?: "miles"
    int precision = 1 // Default precision fallback
    
    switch (targetUnit) {
        case "km":
            // 1 meter = 0.001 kilometers
            converted = meters / 1000.0
            precision = 1
            break
        case "miles":
            // 1 meter = 0.000621371 miles (or divided by 1609.344)
            converted = meters / 1609.344
            precision = 2
            break
        case "ft":
            // 1 meter = 3.28084 feet
            converted = meters * 3.28084
            precision = 0
            break
        default:
            precision = 0
            break
    }
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertIlluminance(BigDecimal rawLux) {
    if (rawLux == null) return 0.0
    
    // Fall back to 'lx' if the preference is null or unconfigured
    String targetUnit = settings.illuminanceUnit ?: "lx"
    BigDecimal convertedValue = rawLux
    int precision = 0 // Default precision for Lux and Foot-candles
    
    switch (targetUnit) {
        case "fc":
            // Lux to Foot-candle conversion: lx * 0.092903
            convertedValue = rawLux * 0.092903
            precision = 1 // Standard clarity representation
            break
        case "ph":
            // Lux to Phot conversion: lx * 0.0001
            convertedValue = rawLux * 0.0001
            precision = 4 // Phot values are extremely small, requiring deeper decimal precision
            break
        case "lx":
        case "none":
        default:
            // Remain as standard Lux
            convertedValue = rawLux
            precision = 0
            break
    }
    
    // Round using standard half-up scaling rules and guard against negative outcomes
    BigDecimal finalValue = convertedValue.setScale(precision, java.math.RoundingMode.HALF_UP)
    return finalValue < 0 ? 0.0 : finalValue
}

private BigDecimal convertHumidity(BigDecimal rawHumidity) {
    // If rawHumidity is null, empty string, or evaluates as false/missing, immediately return 0 formatted to precision
    if (rawHumidity == null || rawHumidity == "") {
        int precision = settings.precisionHumid != null ? settings.precisionHumid.toInteger() : 0
        return new BigDecimal("0.0").setScale(precision, java.math.RoundingMode.HALF_UP)
    }

    // Match the exact preference keys from your metadata options
    String hUnit = settings.humidityUnit ?: "%"
    Double calculatedValue = rawHumidity.toDouble()

    switch (hUnit) {
        case "%":
        case "%RH":
            // Default: no conversion needed, just return the raw percentage
            break
        case "g/m³": // Absolute Humidity
            def tempVal = device.currentValue("currentTemperature")
            if (tempVal != null) {
                Double tempC = tempVal.toDouble()
                String tUnit = settings.temperatureUnit ?: "°F"
                if (tUnit == "°F") { tempC = (tempC - 32.0) * 5.0 / 9.0 }
                else if (tUnit == "K") { tempC = tempC - 273.15 }

                // Vapor pressure approximations
                Double es = 6.112 * Math.exp((17.67 * tempC) / (tempC + 243.5))
                Double e = (calculatedValue / 100.0) * es
                calculatedValue = (e * 216.7) / (tempC + 273.15)
            }
            break
        case "g/kg³": // Mixing Ratio
            def tempVal = device.currentValue("currentTemperature")
            def pressVal = device.currentValue("currentPressure")
            if (tempVal != null) {
                Double tempC = tempVal.toDouble()
                String tUnit = settings.temperatureUnit ?: "°F"
                if (tUnit == "°F") { tempC = (tempC - 32.0) * 5.0 / 9.0 }
                else if (tUnit == "K") { tempC = tempC - 273.15 }

                Double es = 6.112 * Math.exp((17.67 * tempC) / (tempC + 243.5))
                Double e = (calculatedValue / 100.0) * es

                Double pressureHpa = (pressVal != null) ? pressVal.toDouble() : 1013.25
                String pUnit = settings.pressureUnit ?: "inHg"
                if (pUnit == "inHg") { pressureHpa = pressureHpa * 33.8639 }
                else if (pUnit == "kPa") { pressureHpa = pressureHpa * 10.0 }
                else if (pUnit == "mmHg") { pressureHpa = pressureHpa * 1.33322 }

                calculatedValue = 621.99 * e / (pressureHpa - e)
            }
            break
        case "none":
            // Simply pass the raw numeric value out without text/units decoration
            break
        default:
            break
    }

    int precision = (settings.precisionHumid ?: "0").toInteger()
    return BigDecimal.valueOf(calculatedValue).setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertKelvin(def kelvinVal) {
    BigDecimal K = kelvinVal?.toBigDecimal()
    if (K == null) return 0.0
    BigDecimal converted = K
    
    // Determine conversion target from preference selection
    String targetUnit = settings.temperatureUnit ?: "°F"

    switch (targetUnit) {
        case "°F":
            // Kelvin to Fahrenheit: (K − 273.15) × 9/5 + 32
            converted = (K - 273.15) * 1.8 + 32
            break
        case "°C":
            // Kelvin to Celsius: K − 273.15
            converted = K - 273.15
            break
        case "K":
        case "none":
        default:
            // Remain as Kelvin
            break
    }
    
    // Apply user chosen decimal precision layout
    int precision = (settings.precisionTemp ?: "2").toInteger()
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertPrecip(precipVal) {
    // If precipVal is null, empty string, or evaluates as false/missing, immediately return 0 formatted to precision
    if (precipVal == null || precipVal == "") {
        int precision = settings.precisionPrecip != null ? settings.precisionPrecip.toInteger() : 2
        return new BigDecimal("0.0").setScale(precision, java.math.RoundingMode.HALF_UP)
    }
    
    // Ensure we are starting with a clean BigDecimal representation
    BigDecimal precip = (precipVal instanceof BigDecimal) ? precipVal : new BigDecimal(precipVal.toString())
    
    // 1. Handle unit conversion if necessary (OWM returns mm)
    String unit = settings.precipUnit ?: "inHr"
    if (unit in ["inHr", "in", "\""]) {
        precip = precip * 0.0393701
    }
    
    // 2. Grab precision selection from preferences
    int precision = settings.precisionPrecip != null ? settings.precisionPrecip.toInteger() : 2
    
    // 3. Scale and round using HALF_UP strategy
    return precip.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertPressure(def hpaVal) {
    BigDecimal hpa = hpaVal?.toBigDecimal()
    if (hpa == null) return 0.0
    BigDecimal converted = hpa
    
    // Determine conversion target from preference selection
    String targetUnit = settings.pressureUnit ?: "inHg"
    
    switch (targetUnit) {
        case "inHg":
            // Hectopascals to Inches of Mercury
            converted = hpa * 0.029530
            break
        case "mmHg":
            // Hectopascals to Millimeters of Mercury
            converted = hpa * 0.750062
            break
        case "kPa":
            // Hectopascals to Kilopascals
            converted = hpa * 0.1
            break
        case "hPa":
        case "mb":
        case "none":
        default:
            // Remain as hPa / Millibars
            break
    }
    
    // Apply user chosen pressure decimal precision
    int precision = (settings.precisionPress ?: "2").toInteger()
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertWindSpeed(def msVal) {
    BigDecimal ms = msVal?.toBigDecimal()
    if (ms == null) return 0.0
    BigDecimal converted = ms
    
    // Determine conversion target from user preference selection
    String targetUnit = settings.windSpeedUnit ?: "mph"
    
    switch (targetUnit) {
        case "mph":
            // Meters per second to Miles per Hour
            converted = ms * 2.23694
            break
        case "kmh":
            // Meters per second to Kilometers per Hour
            converted = ms * 3.6
            break
        case "kt":
            // Meters per second to Knots
            converted = ms * 1.94384
            break
        case "ms":
        case "none":
        default:
            // Remain as Meters per second
            break
    }
    
    // Apply user chosen wind speed decimal precision
    int precision = (settings.precisionWind ?: "2").toInteger()
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private Map convertWindDirectionState(degrees) {
    if (degrees == null) {
        logDebug "convertWindDirectionState received null value."
        return [cardinal: "Unknown", full: "Unknown", iconUrl: ""]
    }
    double deg = 0.0
    try {
        deg = degrees.toDouble()
    } catch (Exception e) {
        logError "Failed to parse wind degrees (${degrees}): ${e.message}"
        return [cardinal: "Unknown", full: "Unknown", iconUrl: ""]
    }

    deg = (deg % 360 + 360) % 360
    def cardinals = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"]
    def fullWords = ["North", "North-Northeast", "Northeast", "East-Northeast", "East", "East-Southeast", "Southeast", "South-Southeast", "South", "South-Southwest", "Southwest", "West-Southwest", "West", "West-Northwest", "Northwest", "North-Northwest"]
    
    int index = (int) Math.round(deg / 22.5) % 16
    String token = cardinals[index]
    String word  = fullWords[index]
    
    // Determine base path cleanly using fallback-safe runtime state property
    String basePath = state.iconBasePath ?: "https://tinyurl.com/icnqz/"
    
    // Evaluate user preference for alternative icons
    String filename = (settings.altIconsEnable == false) ? "wb1.png" : "wind-${token.toLowerCase()}.png"
    
    // Safety Fallback Check: If path points to OWM standard, fallback to Hubitat Community assets
    String finalIconUrl = basePath.contains("openweathermap.org") ? "https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/${filename}" : "${basePath}${filename}"
    
    return [cardinal: token, full: word, iconUrl: finalIconUrl]
}

// -----------------------  CALCS

private String calcIconBasePath(String altIconLoc) {
    // https://tinyurl.com/icnqz/ points to https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/
    String base = altIconLoc ? altIconLoc.trim() : "https://tinyurl.com/icnqz"
    
    // Enforce trailing slash constraint
    if (!base.endsWith("/")) base += "/"
    logDebug "Calculated Icon Base Path resolved to: ${base}"
    return base
}

void calcMoonPhaseIconWithText(Map todayData = [:], Map tomData = [:], Map tdaData = [:]) {
    logDebug "Calculating moon phase icons and text names from API payload maps..."
    
    def phases = [
        [sourceMap: todayData, apiKey: "moon_phase", valAttr: "todayMoonPhase", iconAttr: "todayMoonPhaseIcon", textAttr: "todayMoonPhaseText"],
        [sourceMap: tomData,   apiKey: "moon_phase", valAttr: "tomMoonPhase",   iconAttr: "tomMoonPhaseIcon",   textAttr: "tomMoonPhaseText"],
        [sourceMap: tdaData,   apiKey: "moon_phase", valAttr: "tdaMoonPhase",   iconAttr: "tdaMoonPhaseIcon",   textAttr: "tdaMoonPhaseText"]
    ]
    
    phases.each { phase ->
        // Direct extraction from API response payload data, fallback to database attributes if empty
        def rawVal = (phase.sourceMap && phase.sourceMap[phase.apiKey] != null) ? phase.sourceMap[phase.apiKey] : device.currentValue(phase.valAttr)
        
        // Ensure value is both non-null and safely numeric before processing
        if (rawVal != null && rawVal.toString().isNumber()) {
            // Normalize values to stay strictly within 0.0 to 1.0 bounds
            double val = (rawVal.toDouble() % 1.0 + 1.0) % 1.0
            
            // 1. Determine the icon bin index (0 to 7)
            int index = (int) Math.floor((val * 8) + 0.5) % 8
            String filename = "mp${index + 1}.png"
            String basePath = state.iconBasePath ?: "https://tinyurl.com/icnqz/"
            sendIfChanged(name: phase.iconAttr, value: "${basePath}${filename}")

            // 2. Determine phase name using small range tolerances (+/- 0.02) around major quarter marks
            String phaseName = "Unknown"
            double tolerance = 0.02
            if (val <= tolerance || val >= (1.0 - tolerance)) phaseName = "New Moon"
            else if (val > tolerance && val < (0.25 - tolerance)) phaseName = "Waxing Crescent"
            else if (val >= (0.25 - tolerance) && val <= (0.25 + tolerance)) phaseName = "First Quarter Moon"
            else if (val > (0.25 + tolerance) && val < (0.5 - tolerance)) phaseName = "Waxing Gibbous"
            else if (val >= (0.5 - tolerance) && val <= (0.5 + tolerance)) phaseName = "Full Moon"
            else if (val > (0.5 + tolerance) && val < (0.75 - tolerance)) phaseName = "Waning Gibbous"
            else if (val >= (0.75 - tolerance) && val <= (0.75 + tolerance)) phaseName = "Last Quarter Moon"
            else if (val > (0.75 + tolerance) && val < (1.0 - tolerance)) phaseName = "Waning Crescent"
            
            sendIfChanged(name: phase.textAttr, value: phaseName)
        }
    }
}

private void calcAlertsState(Map json, String calculatedCityAttr, String iconBasePath) {
    def alerts = json?.alerts ?: []
    String alertActive = "No active alerts"
    String currentAlertSender = "N/A"
    String currentAlertDesc = "No active alerts"
    String lastPollTime = new Date().format("HH:mm", location.timeZone)
    String currentAlertDescFull = "No active alerts for ${calculatedCityAttr} at last poll as of ${lastPollTime}"
    String baseStyle = "display:flex;flex-direction:column;justify-content:space-between;height:100%;padding:8px;box-sizing:border-box;background:rgba(30,30,40,0.65);border-radius:12px;color:#fff;font-family:sans-serif;line-height:1.2;text-align:center"
    String currentAlertTile = ""

    if (alerts.size() > 0) {
        def a = alerts[0]
        alertActive = a.event ?: "Active Alert"
        currentAlertSender = a.sender_name ?: "Unknown"
        currentAlertDesc = a.description ? (a.description.length() > 100 ? a.description.take(100) + "..." : a.description) : "No active alerts"
        currentAlertDescFull = (a.description ?: "N/A") + " as of ${lastPollTime}"
        
        String alertBody = "<div style='${baseStyle}'><div style='font-size:0.85em;font-weight:bold;color:#ff5555;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:2px;'>⚠️ ${alertActive}</div><div style='font-size:0.8em;font-weight:600;color:#ddd;padding:4px;overflow:hidden;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;'>${currentAlertDesc}</div><div style='display:flex;justify-content:space-between;border-top:1px solid rgba(255,255,255,0.15);padding-top:4px;font-size:0.7em;color:#bbb'><span>👤 ${currentAlertSender.take(12)}</span><span>⏰ ${lastPollTime}</span></div><div style='font-size:0.6em;color:#888;margin-top:2px;'>"
        int alertLenEstimate = alertBody.length() + "999 chars long</div></div>".length()
        currentAlertTile = alertBody + "${alertLenEstimate} chars long</div></div>"
    } else {
        String noAlertBody = "<div style='${baseStyle}'><div style='font-size:0.85em;font-weight:bold;color:#55ff55;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:2px;'>✅ Your Local Area</br>Clear Weather</div><div style='font-size:0.8em;font-weight:600;color:#aaa;padding:8px 4px;'>No active weather alerts from OpenWeatherMap or their sources</div><div style='display:flex;justify-content:center;border-top:1px solid rgba(255,255,255,0.15);padding-top:4px;font-size:0.7em;color:#bbb'><span>Updated ${lastPollTime}</span></div><div style='font-size:0.6em;color:#888;margin-top:2px;'>"
        int alertLenEstimate = noAlertBody.length() + "999 chars long</div></div>".length()
        currentAlertTile = noAlertBody + "${alertLenEstimate} chars long</div></div>"
    }

    sendIfChanged(name: "currentAlert", value: alertActive)
    sendIfChanged(name: "currentAlertSender", value: currentAlertSender)
    sendIfChanged(name: "currentAlertDesc", value: currentAlertDesc)
    sendIfChanged(name: "currentAlertDescFull", value: currentAlertDescFull)
    sendIfChanged(name: "currentAlertTile", value: currentAlertTile)
}

private BigDecimal calcSunPosition() {
    // 1. Establish User Precision and Coordinates
    int precision = (settings.precisionSunAngles ?: "0").toInteger()
    BigDecimal locLat = state.usedLatitude != null ? state.usedLatitude.toBigDecimal() : location.latitude
    BigDecimal locLon = state.usedLongitude != null ? state.usedLongitude.toBigDecimal() : location.longitude

    if (locLat == null || locLon == null) {
        logWarn "calcSunPosition: Latitude or Longitude coordinates are unavailable."
        return 0.0
    }

    // 2. Get current universal time in UTC to bypass local timezone/DST errors
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    double hour = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0) + (cal.get(Calendar.SECOND) / 3600.0)
    int day = cal.get(Calendar.DAY_OF_MONTH)
    int month = cal.get(Calendar.MONTH) + 1
    int year = cal.get(Calendar.YEAR)

    // 3. Compute fractional Julian Date (relative to standard J2000 epoch)
    if (month <= 2) {
        year -= 1
        month += 12
    }
    int A = (int)(year / 100)
    int B = 2 - A + (int)(A / 4)
    double jd = (int)(365.25 * (year + 4716)) + (int)(30.6001 * (month + 1)) + day + (hour / 24.0) + B - 1524.5
    double d = jd - 2451545.0

    // 4. Calculate Keplerian Solar Coordinates
    double g = 357.529 + 0.98560028 * d // Mean anomaly of the Sun
    double q = 280.459 + 0.98564736 * d // Mean longitude of the Sun
    double L = q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g)) // Ecliptic longitude
    double e = 23.439 - 0.00000036 * d // Obliquity of the ecliptic

    // Declination and Right Ascension
    double sin_delta = Math.sin(Math.toRadians(e)) * Math.sin(Math.toRadians(L))
    double delta = Math.toDegrees(Math.asin(sin_delta))
    double ra = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(e)) * Math.sin(Math.toRadians(L)), Math.cos(Math.toRadians(L))))

    // 5. Sidereal Time tracking and Local Hour Angle
    double gst = 280.46061837 + 360.98564736629 * d
    double lst = gst + locLon
    double H = lst - ra

    // 6. Project onto local horizontal plane (Altitude & Azimuth)
    double latRad = Math.toRadians(locLat)
    double deltaRad = Math.toRadians(delta)
    double hRad = Math.toRadians(H)

    // Calculate Altitude
    double sin_alt = Math.sin(latRad) * Math.sin(deltaRad) + Math.cos(latRad) * Math.cos(deltaRad) * Math.cos(hRad)
    sin_alt = Math.max(-1.0, Math.min(1.0, sin_alt)) // Safety clamp
    double alt = Math.toDegrees(Math.asin(sin_alt))

    // Calculate Azimuth
    double cos_alt = Math.cos(Math.toRadians(alt))
    double az = 0.0
    if (Math.abs(cos_alt) > 0.0001) {
        double cos_az = (Math.sin(deltaRad) - Math.sin(latRad) * sin_alt) / (Math.cos(latRad) * cos_alt)
        cos_az = Math.max(-1.0, Math.min(1.0, cos_az))
        az = Math.toDegrees(Math.acos(cos_az))
        if (Math.sin(hRad) > 0) az = 360.0 - az
    } else {
        az = (locLat > 0) ? 180.0 : 0.0
    }

    // Normalize Azimuth loop to 0-360 boundaries
    az = (az % 360.0 + 360.0) % 360.0

    // 7. Process precision preferences and build attributes
    BigDecimal finalAltitude = BigDecimal.valueOf(alt).setScale(precision, java.math.RoundingMode.HALF_UP)
    BigDecimal finalAzimuth = BigDecimal.valueOf(az).setScale(precision, java.math.RoundingMode.HALF_UP)

    logTrace "calcSunPosition: Solar Altitude computed as ${finalAltitude}°, Azimuth as ${finalAzimuth}°"

    // 8. Publish the rounded numbers to the device attributes
    sendIfChanged(name: "altitude", value: finalAltitude)
    sendIfChanged(name: "azimuth", value: finalAzimuth)
    sendIfChanged(name: "altitudeText", value: "${finalAltitude}°")
    sendIfChanged(name: "azimuthText", value: "${finalAzimuth}°")

    state.sunAltitude = finalAltitude
    return finalAltitude
}

private void calcCurrentTwilight() {
    if (state.todaySunriseEpoch && state.todaySunsetEpoch) {
        // Civil twilight ≈ 24 minutes (1440 seconds) before sunrise and after sunset
        long twilightBeginEpoch = (state.todaySunriseEpoch as Long) - 1440L
        long twilightEndEpoch   = (state.todaySunsetEpoch  as Long) + 1440L
        logDebug "Calculated Twilight Epochs -> Begin: ${twilightBeginEpoch}, End: ${twilightEndEpoch}"

        sendIfChanged(name: "currentTwilightBeginTime", value: twilightBeginEpoch)
        sendIfChanged(name: "currentTwilightEndTime", value: twilightEndEpoch)
        state.twilightBeginEpoch = twilightBeginEpoch
        state.twilightEndEpoch   = twilightEndEpoch
    } else {
        logWarn "calcCurrentTwilight skipped: Missing todaySunriseEpoch or todaySunsetEpoch in state."
    }
}

private void calcBetwixtState(BigDecimal altitude, long liveSunrise = 0, long liveSunset = 0) {
    String sliceText = "fully night time"
    long currentEpoch = (now() / 1000L)
    
    // Use live method arguments if provided, otherwise fall back to cached states
    long sunriseEpoch = (liveSunrise > 0) ? liveSunrise : (state.todaySunriseEpoch ?: 0)
    long sunsetEpoch = (liveSunset > 0) ? liveSunset : (state.todaySunsetEpoch ?: 0)
    
    boolean isTwilightAngle = (altitude >= -6.0 && altitude < -0.833)
    boolean isSunUp = (altitude >= -0.833)
    
    if (sunriseEpoch > 0 && sunsetEpoch > 0) { 
        long midDayEpoch = sunriseEpoch + ((sunsetEpoch - sunriseEpoch) / 2)
        
        // --- ADDED LOGIC FOR CURRENT NOON TIME ---
        try {
            // Hubitat sandboxed way to format a raw long timestamp without instantiating Date(long)
            sendIfChanged(name: "currentSolarNoonTime", value: midDayEpoch)
            state.solarNoonEpoch = midDayEpoch
        } catch (Exception e) {
            logError "Exception occurred while calculating currentSolarNoonTime: ${e.message}"
        }
        if (currentEpoch < midDayEpoch) {
            sliceText = isTwilightAngle ? "between twilight and sunrise" : (isSunUp ? "between sunrise and noon" : sliceText)
        } else {
            sliceText = isSunUp ? "between noon and sunset" : (isTwilightAngle ? "between sunset and twilight" : sliceText)
        }
    } else {
        sliceText = isTwilightAngle ? "between twilight and sunrise" : (isSunUp ? "between sunrise and noon" : sliceText)
    }

    sendIfChanged(name: "betwixt", value: sliceText)
    logDebug "Calculated betwixt slice: ${sliceText} (Current Altitude: ${altitude}°)"
}

private void calcIsDayState(BigDecimal altitude) {
    // The sun is considered "up" (daytime) if its altitude is >= -0.833 degrees
    String isDayText = (altitude != null && altitude >= -0.833) ? "true" : "false"
    sendIfChanged(name: "currentIsDay", value: isDayText) 
    logTrace "Calculated currentIsDay: ${isDayText}" 
}

private BigDecimal calcCurrentIlluminance(BigDecimal altitude, def liveClouds = null) { 
    logDebug "Calculating dynamic current illuminance adjusted for chosen unit..."
    
    // Fall back to DB lookup only if an in-memory cloud value wasn't provided
    def cloudPctVal = (liveClouds != null) ? liveClouds : device.currentValue("currentCloudPCT")
    
    if (cloudPctVal == null || altitude == null) {
        logDebug "calcCurrentIlluminance postponed: Waiting for cloud percentage or sun altitude data."
        return 0.0
    }
    
    if (altitude <= 0) {
        sendIfChanged(name: "currentIlluminance", value: 0)
        sendIfChanged(name: "illuminance", value: 0) 
        return 0.0 
    }
    
    BigDecimal clouds = cloudPctVal.toBigDecimal()
    double radians = Math.toRadians(altitude.doubleValue())
    BigDecimal clearSkyLux = 100000 * Math.sin(radians)
    BigDecimal attenuatedLux = (clearSkyLux * ((100 - clouds) / 100)) * 0.75
    
    String targetUnit = settings.illuminanceUnit ?: "lx"
    BigDecimal finalValue = convertIlluminance(attenuatedLux)

    if (targetUnit == "lx" && finalValue > 100000) finalValue = 100000 
    logDebug "Illuminance Parsed: ${finalValue} ${targetUnit} (Base Lux: ${attenuatedLux.setScale(0, 4)} lx)"

    sendIfChanged(name: "currentIlluminance", value: finalValue)
    sendIfChanged(name: "illuminance", value: finalValue) 
    return finalValue 
}

private void calcTextValue(BigDecimal freshLux = null, BigDecimal freshTemp = null, BigDecimal freshPress = null, BigDecimal freshWind = null, BigDecimal freshHum = null, Map currentMap = [:], Map todayMap = [:], Map tomMap = [:], Map tdaMap = [:]) {
    logDebug "Generating formatted text attributes with unit suffixes..."

    // --- CONSOLIDATED ENVIRONMENT PREFERENCES ---
    String tUnit = settings.temperatureUnit ?: "°F"
    String pUnit = settings.pressureUnit ?: "inHg"
    String wUnit = (settings.windSpeedUnit == "none") ? "" : " ${settings.windSpeedUnit ?: 'mph'}"
    String iUnit = (settings.illuminanceUnit == "none") ? "lx" : (settings.illuminanceUnit ?: "lx")
    String hUnit = settings.humidityUnit ?: "%"
    
    if (hUnit == "none") hUnit = ""
    else if (hUnit in ["g/m³", "g/kg³"]) hUnit = " ${hUnit}"

    // 1. Temperature Text Formatting
    def tempVal = (freshTemp != null) ? freshTemp : device.currentValue("currentTemperature")
    if (tempVal != null) sendIfChanged(name: "currentTemperatureText", value: "${tempVal}${tUnit}")

    // 2. Pressure Text Formatting
    def pressVal = (freshPress != null) ? freshPress : device.currentValue("currentPressure")
    if (pressVal != null) sendIfChanged(name: "currentPressureText", value: "${pressVal} ${pUnit}")

    // 3. Wind Speed Text Formatting (Current, Today, Tomorrow, Day After) & Beaufort Scale Loop
    def windTargets = [
        [prefix: "current", val: freshWind, map: currentMap],
        [prefix: "today",   val: null,      map: todayMap],
        [prefix: "tom",     val: null,      map: tomMap],
        [prefix: "tda",     val: null,      map: tdaMap]
    ]

    windTargets.each { target ->
        // Resolve dynamic forecast/current speeds safely
        def speedVal = target.val
        if (speedVal == null) {
            speedVal = (target.map && target.map.wind_speed != null) ? convertWindSpeed(target.map.wind_speed) : device.currentValue("${target.prefix}WindSpeed")
        }
        if (speedVal != null) sendIfChanged(name: "${target.prefix}WindSpeedText", value: "${speedVal}${wUnit}")
        
        // Output matching Beaufort texts
        def rawSpeed = (target.map && target.map.wind_speed != null) ? target.map.wind_speed : device.currentValue("${target.prefix}WindSpeed")
        sendIfChanged(name: "${target.prefix}WindSpeedDescText", value: getBeaufortText(rawSpeed))
    }

    // 4. Illuminance Text Formatting
    def luxVal = (freshLux != null) ? freshLux : device.currentValue("currentIlluminance")
    if (luxVal != null) sendIfChanged(name: "currentIlluminanceText", value: "${luxVal} ${iUnit}")
    
    // 5. Solar Angles Formatting
    def altVal = device.currentValue("altitude")
    if (altVal != null) sendIfChanged(name: "altitudeText", value: "${altVal}°")
    def azVal = device.currentValue("azimuth")
    if (azVal != null) sendIfChanged(name: "azimuthText", value: "${azVal}°")

    // 6. Humidity Formatting
    def humVal = (freshHum != null) ? freshHum : device.currentValue("currentHumidity")
    if (humVal != null) sendIfChanged(name: "currentHumidityText", value: "${humVal}${hUnit}")

    // --- CONSOLIDATED LOOP FOR ALL DATE/TIME PROPERTIES (SECTIONS 7, 8, 9, 10) ---
    logDebug "Consolidated loop executing for date/time properties..."
    String chosenDTForm = settings.DateTimeForm ?: "1"
    String chosenDateForm = settings.DateForm ?: "1"
    String chosenTimeForm = settings.TimeForm ?: "1"

    device.properties.supportedAttributes.each { attr ->
        String attrName = attr.name
        def epochValue = null
        String formatSelection = "1"
        String textTargetSuffix = ""

        if (attrName == "lastUpdatedDateTime" || attrName.endsWith("DateTime")) {
            epochValue = device.currentValue(attrName)
            formatSelection = chosenDTForm
            textTargetSuffix = "Text"
        } else if (attrName.endsWith("Date")) {
            // Intercept with local maps if available to prevent initialization race condition
            if (attrName == "todayDate" && todayMap?.dt != null) epochValue = todayMap.dt
            else if (attrName == "tomDate" && tomMap?.dt != null) epochValue = tomMap.dt
            else if (attrName == "tdaDate" && tdaMap?.dt != null) epochValue = tdaMap.dt
            else epochValue = device.currentValue(attrName)
            formatSelection = chosenDateForm
            textTargetSuffix = "Text"
        } else if (attrName.endsWith("Time") && !attrName.contains("DateTime")) {
            // Intercept with live API maps to prevent initialization database lag
            if (attrName == "currentSunriseTime" && currentMap?.sunrise != null) epochValue = currentMap.sunrise
            else if (attrName == "currentSunsetTime" && currentMap?.sunset != null) epochValue = currentMap.sunset
            else if (attrName == "todaySunriseTime" && todayMap?.sunrise != null) epochValue = todayMap.sunrise
            else if (attrName == "todaySunsetTime" && todayMap?.sunset != null) epochValue = todayMap.sunset
            else if (attrName == "todayMoonriseTime" && todayMap?.moonrise != null) epochValue = todayMap.moonrise
            else if (attrName == "todayMoonsetTime" && todayMap?.moonset != null) epochValue = todayMap.moonset
            else if (attrName == "tomSunriseTime" && tomMap?.sunrise != null) epochValue = tomMap.sunrise
            else if (attrName == "tomSunsetTime" && tomMap?.sunset != null) epochValue = tomMap.sunset
            else if (attrName == "tomMoonriseTime" && tomMap?.moonrise != null) epochValue = tomMap.moonrise
            else if (attrName == "tomMoonsetTime" && tomMap?.moonset != null) epochValue = tomMap.moonset
            else if (attrName == "tdaSunriseTime" && tdaMap?.sunrise != null) epochValue = tdaMap.sunrise
            else if (attrName == "tdaSunsetTime" && tdaMap?.sunset != null) epochValue = tdaMap.sunset
            else if (attrName == "tdaMoonriseTime" && tdaMap?.moonrise != null) epochValue = tdaMap.moonrise
            else if (attrName == "tdaMoonsetTime" && tdaMap?.moonset != null) epochValue = tdaMap.moonset
            else if (attrName == "currentTwilightBeginTime") epochValue = state.twilightBeginEpoch ?: device.currentValue(attrName)
            else if (attrName == "currentTwilightEndTime") epochValue = state.twilightEndEpoch ?: device.currentValue(attrName)
            else if (attrName == "currentSolarNoonTime") epochValue = state.solarNoonEpoch ?: device.currentValue(attrName)
            else epochValue = device.currentValue(attrName)
            
            formatSelection = chosenTimeForm
            textTargetSuffix = "Text"
        }

        if (epochValue != null && textTargetSuffix != "") {
            String targetAttr = "${attrName}${textTargetSuffix}"
            if (formatSelection == "10") {
                sendIfChanged(name: targetAttr, value: "${epochValue}")
            } else {
                Map formattedMap = convertDateTimeFormat(epochValue, formatSelection)
                String finalVal = (attrName.endsWith("Date")) ? formattedMap.date : ((attrName.endsWith("Time") && !attrName.contains("DateTime")) ? formattedMap.time : formattedMap.dateTime)
                sendIfChanged(name: targetAttr, value: finalVal)
            }
        }
    }

    // 12. Current Wind Summary Text Formatter
    logDebug "Section 11b: Formatting Current Wind Summary Text ..."
    try {
        // Resolve wind speed using in-memory local variables first to bypass DB lag
        def windValLocal = (freshWind != null) ? freshWind : device.currentValue("currentWindSpeed")
        String windSpeedText = (windValLocal != null) ? "${windValLocal}${wUnit}" : (device.currentValue("currentWindSpeedText") ?: "--")
        
        // Retrieve the current wind speed description text (e.g., "Gentle Breeze")
        String currentWindSpeedDescText = device.currentValue("currentWindSpeedDescText") ?: "Calm"
        String windDirFull = device.currentValue("currentWindDirFull") ?: "Unknown"
        
        // Updated implementation: Starts with currentWindSpeedDescText instead of windSpeedText
        // Format: 'currentWindSpeedDescText' from the 'currentWindDirFull' at 'windSpeedText'
        // Example: "Gentle Breeze from the Southwest at 8.5 mph"
        sendIfChanged(name: "currentWindSummaryText", value: "${currentWindSpeedDescText} from the ${windDirFull} at ${windSpeedText}")
    } catch (Exception e) {
        logError "Exception occurred during currentWindSummaryText generation: ${e.message}"
    }
    
    // 13. Current Weather Summary Text Formatter
    logDebug "Section 12: Formatting Current Weather Summary Text ..."
    try {
        // Collect localized parameters or database values
        def city = state.usedCity ?: "Local Area"
        def lastUpdatedEpoch = device.currentValue("lastUpdatedDateTime")
        
        // DateTime Formats
        String lastUpdatedDate = "--"
        String lastUpdatedTime = "--"
        if (lastUpdatedEpoch != null) {
            if (chosenDateForm == "10") {
                lastUpdatedDate = "${lastUpdatedEpoch}"
            } else {
                lastUpdatedDate = convertDateTimeFormat(lastUpdatedEpoch, chosenDateForm).date
            }
            if (chosenTimeForm == "10") {
                lastUpdatedTime = "${lastUpdatedEpoch}"
            } else {
                lastUpdatedTime = convertDateTimeFormat(lastUpdatedEpoch, chosenTimeForm).time
            }
        }
        
        def cond = device.currentValue("currentConditionTypeDesc") ?: "--"
        
        // Temperatures
        def temp = (freshTemp != null) ? freshTemp : device.currentValue("currentTemperature")
        def hi = (todayMap && todayMap.temp?.max != null) ? convertKelvin(todayMap.temp.max) : (device.currentValue("todayTempMax") ?: "--")
        def lo = (todayMap && todayMap.temp?.min != null) ? convertKelvin(todayMap.temp.min) : (device.currentValue("todayTempMin") ?: "--")
        def feels = device.currentValue("currentFeelsLike") ?: temp
        
        // --- FIX 1: Resolve the current temperature text in-memory to bypass async DB lag ---
        String tempText = (temp != null) ? "${temp}${tUnit}" : (device.currentValue("currentTemperatureText") ?: "--")
        
        // Humidity and Wind
        humVal = (freshHum != null) ? freshHum : device.currentValue("currentHumidity")
        String humText = (humVal != null) ? "${humVal}%" : "--"
        
        // Wind descriptor parsing
        def windRaw = (currentMap && currentMap.wind_speed != null) ? currentMap.wind_speed : device.currentValue("currentWindSpeed")
        String windStrengthText = getBeaufortText(windRaw)
        String windDirFull = device.currentValue("currentWindDirFull") ?: "Unknown"
        
        // --- FIX 2: Resolve the wind speed text in-memory to bypass async DB lag ---
        def windValLocal = (freshWind != null) ? freshWind : device.currentValue("currentWindSpeed")
        String windSpeedText = (windValLocal != null) ? "${windValLocal}${wUnit}" : (device.currentValue("currentWindSpeedText") ?: "--")
        
        // Precipitation Probability
        def popRaw = (todayMap && todayMap.pop != null) ? todayMap.pop : (device.currentValue("todayPOP") ?: 0)
        int popPct = (popRaw.toBigDecimal() * 100).intValue()
        
        // Visibility and Alert Details
        String visibilityText = device.currentValue("currentVisibilityText") ?: "--"
        String alertVal = device.currentValue("currentAlert") ?: "No active alerts"
        
        // Handle plural/singular grammar for alerts dynamically
        String alertSentence = (alertVal != "No active alerts" && alertVal != "N/A" && alertVal != "") ? "Active alerts for ${alertVal} exist for this area." : "No active alerts exist for this area."

        // Main dynamic string assembly
        String summary = "Weather summary for ${city} updated at ${lastUpdatedTime} on ${lastUpdatedDate}. " +
                         "Expect ${cond} with a high of ${hi}${tUnit} and a low of ${lo}${tUnit}. " +
                         "Humidity is ${humText} and the current temperature is ${tempText}. " +
                         "The temperature feels like it is ${feels}${tUnit}. " +
                         "Wind: ${windStrengthText} from the ${windDirFull} at ${windSpeedText}. " +
                         "There is a ${popPct}% chance of precipitation. " +
                         "Visibility is around ${visibilityText}. " +
                         "${alertSentence}"

        sendIfChanged(name: "currentWeatherSummaryText", value: summary)
    } catch (Exception e) {
        logError "Exception occurred during currentWeatherSummaryText generation: ${e.message}"
    }
}

private String getBeaufortText(def rawMsVal) {
    // Beaufort scale bounds based on m/s
    double ms = rawMsVal?.toDouble() ?: 0.0
    if (ms < 0.3)   return "Calm"
    if (ms <= 1.5)  return "Light Air"
    if (ms <= 3.3)  return "Light Breeze"
    if (ms <= 5.4)  return "Gentle Breeze"
    if (ms <= 7.9)  return "Moderate Breeze"
    if (ms <= 10.7) return "Fresh Breeze"
    if (ms <= 13.8) return "Strong Breeze"
    if (ms <= 17.1) return "Near Gale"
    if (ms <= 20.7) return "Gale"
    if (ms <= 24.4) return "Severe Gale"
    if (ms <= 28.4) return "Storm"
    if (ms <= 32.6) return "Violent Storm"
    return "Hurricane"
}

private void calcLonLatCityState() {
    // Read current input settings values safely
    String currentCity = settings.overrideCity ?: ""
    BigDecimal currentLat = settings.overrideLatitude ? settings.overrideLatitude.toBigDecimal() : null 
    BigDecimal currentLon = settings.overrideLongitude ? settings.overrideLongitude.toBigDecimal() : null 

    // Optimisation check: If preferences are unchanged and we already have cached outputs, skip
    Boolean settingsChanged = (currentCity != state.lastOverrideCity || currentLat != state.lastOverrideLatitude || currentLon != state.lastOverrideLongitude)
    Boolean hasCachedData = (state.usedCity && state.usedLatitude != null && state.usedLongitude != null)

    if (!settingsChanged && hasCachedData) {
        logDebug "Coordinates and city are unchanged and cached. Skipping geo lookup."
        return
    }

    String usedCity = ""
    BigDecimal usedLatitude = 0.0
    BigDecimal usedLongitude = 0.0

    // ------------------------------------------------------------- 
    // SCENARIO 1: An explicit override city name has been given
    // ------------------------------------------------------------- 
    if (currentCity && currentCity.trim() != "") {
        logDebug "Scenario 1: overrideCity provided ('${currentCity}'). Running Direct Geo-Lookup."
        String encodedCity = URLEncoder.encode(currentCity.trim(), "UTF-8")
        String geoUrl = "https://api.openweathermap.org/geo/1.0/direct?q=${encodedCity}&limit=1&appid=${apiKey}"
		if (!settings.aPIKeyExposedEnable) {
			logDebug "Polling OpenWeatherMap GEO via URL: ${geoUrl.replaceAll(/appid=[^&]+/, 'appid=***')}"
		} else {
			logDebug "Polling OpenWeatherMap GEO via URL: ${geoUrl}"
		}
        
        def params = [uri: geoUrl, contentType: "application/json", timeout: 10]
        try {
            httpGet(params) { response ->
                if (response.status == 200 && response.data && response.data.size() > 0) {
                    def locationData = response.data[0]
                    usedCity = locationData.name ?: currentCity
                    usedLatitude = locationData.lat ? locationData.lat.toBigDecimal() : 0.0
                    usedLongitude = locationData.lon ? locationData.lon.toBigDecimal() : 0.0
                    logDebug "Direct Geo-Lookup Success -> City: ${usedCity}, Lat: ${usedLatitude}, Lon: ${usedLongitude}"
                } else {
                    logWarn "Direct Geo-Lookup returned no results. Falling back to configurations."
                }
            }
        } catch (Exception e) {
            logError "Exception occurred during Direct Geo-Lookup execution: ${e.message}"
        }
    } 
    // ------------------------------------------------------------- 
    // SCENARIO 2: Fall back to coordinate inputs or hub defaults
    // ------------------------------------------------------------- 
    else {
        logDebug "Scenario 2: No overrideCity provided. Evaluating coordinate inputs or Hub configuration."
        usedLatitude = currentLat ?: location.latitude?.toBigDecimal()
        usedLongitude = currentLon ?: location.longitude?.toBigDecimal()
        
        // Execute Reverse Geo Lookup using the determined coordinates
        if (usedLatitude && usedLongitude && apiKey) {
            logDebug "Attempting Reverse Geo-Lookup using Lat: ${usedLatitude}, Lon: ${usedLongitude}"
            String reverseGeoUrl = "https://api.openweathermap.org/geo/1.0/reverse?lat=${usedLatitude}&lon=${usedLongitude}&limit=1&appid=${apiKey}"
            def params = [uri: reverseGeoUrl, contentType: "application/json", timeout: 10]
            try {
                httpGet(params) { response ->
                    if (response.status == 200 && response.data && response.data.size() > 0) {
                        usedCity = response.data[0].name ?: "Local Area"
                        logDebug "Reverse Geo-Lookup Success -> City resolved: ${usedCity}"
                    } else {
                        logWarn "Reverse Geo-Lookup returned empty results. Defaulting city name."
                        usedCity = "Local Area"
                    }
                }
            } catch (Exception e) {
                logError "Exception occurred during Reverse Geo-Lookup execution: ${e.message}"
                usedCity = "Local Area"
            }
        } else {
            usedCity = "Local Area"
        }
    } 

    // ------------------------------------------------------------- 
    // EXTRA PROTECTION: Enforce fallback to Hub defaults if values 
    // remain blank or zero (e.g. failed lookups or empty settings)
    // ------------------------------------------------------------- 
    if (!usedLatitude || usedLatitude == 0.0) {
        usedLatitude = currentLat ?: location.latitude?.toBigDecimal() ?: 0.0
    }
    if (!usedLongitude || usedLongitude == 0.0) {
        usedLongitude = currentLon ?: location.longitude?.toBigDecimal() ?: 0.0
    }
    if (!usedCity || usedCity.trim() == "") {
        usedCity = "Local Area"
    }

    // Commit calculated configurations into global variables for API calls
    state.usedCity = usedCity
    state.usedLatitude = usedLatitude
    state.usedLongitude = usedLongitude
    
    // Cache current settings so we can compare against them next time
    state.lastOverrideCity = currentCity
    state.lastOverrideLatitude = currentLat
    state.lastOverrideLongitude = currentLon
}

private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    
    // Standardize checking logic
    String oldVal = device.currentValue(args.name as String)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        // Ensure standard Hubitat tracking options are appended safely
        Map eventMap = [name: args.name, value: args.value, descriptionText: "Attribute ${args.name} changed to ${args.value}"]
        if (args.unit) eventMap.unit = args.unit
        sendEvent(eventMap)
        logDebug "Event triggered: ${args.name} -> ${args.value}"
    }
}

private Map lookupConditionDetails(Integer code) {
    def conditions = [
        // Thunderstorms
        200: [type: "Thunderstorm", desc: "Thunderstorm with Light Rain", altDay: "38.png", altNight: "47.png"],
        201: [type: "Thunderstorm", desc: "Thunderstorm with Rain", altDay: "38.png", altNight: "47.png"],
        202: [type: "Thunderstorm", desc: "Thunderstorm with Heavy Rain", altDay: "38.png", altNight: "47.png"],
        210: [type: "Thunderstorm", desc: "Light Thunderstorm", altDay: "38.png", altNight: "47.png"],
        211: [type: "Thunderstorm", desc: "Thunderstorm", altDay: "38.png", altNight: "47.png"],
        212: [type: "Thunderstorm", desc: "Heavy Thunderstorm", altDay: "38.png", altNight: "47.png"],
        221: [type: "Thunderstorm", desc: "Ragged Thunderstorm", altDay: "38.png", altNight: "47.png"],
        230: [type: "Thunderstorm", desc: "Thunderstorm with Light Drizzle", altDay: "38.png", altNight: "47.png"],
        231: [type: "Thunderstorm", desc: "Thunderstorm with Drizzle", altDay: "38.png", altNight: "47.png"],
        232: [type: "Thunderstorm", desc: "Thunderstorm with Heavy Drizzle", altDay: "38.png", altNight: "47.png"],

        // Drizzle
        300: [type: "Drizzle", desc: "Light Intensity Drizzle", altDay: "9.png", altNight: "9.png"],
        301: [type: "Drizzle", desc: "Drizzle", altDay: "9.png", altNight: "9.png"],
        302: [type: "Drizzle", desc: "Heavy Intensity Drizzle", altDay: "9.png", altNight: "9.png"],
        310: [type: "Drizzle", desc: "Light Intensity Drizzle Rain", altDay: "9.png", altNight: "9.png"],
        311: [type: "Drizzle", desc: "Drizzle Rain", altDay: "9.png", altNight: "9.png"],
        312: [type: "Drizzle", desc: "Heavy Intensity Drizzle Rain", altDay: "9.png", altNight: "9.png"],
        313: [type: "Drizzle", desc: "Shower Rain and Drizzle", altDay: "9.png", altNight: "9.png"],
        314: [type: "Drizzle", desc: "Heavy Shower Rain and Drizzle", altDay: "9.png", altNight: "9.png"],
        321: [type: "Drizzle", desc: "Shower Drizzle", altDay: "9.png", altNight: "9.png"],

        // Rain
        500: [type: "Rain", desc: "Light Rain", altDay: "39.png", altNight: "9.png"],
        501: [type: "Rain", desc: "Moderate Rain", altDay: "39.png", altNight: "11.png"],
        502: [type: "Rain", desc: "Heavy Intensity Rain", altDay: "39.png", altNight: "11.png"],
        503: [type: "Rain", desc: "Very Heavy Rain", altDay: "39.png", altNight: "11.png"],
        504: [type: "Rain", desc: "Extreme Rain", altDay: "39.png", altNight: "11.png"],
        511: [type: "Rain", desc: "Freezing Rain", altDay: "39.png", altNight: "11.png"],
        520: [type: "Rain", desc: "Light Intensity Shower Rain", altDay: "39.png", altNight: "9.png"],
        521: [type: "Rain", desc: "Shower Rain", altDay: "39.png", altNight: "11.png"],
        522: [type: "Rain", desc: "Heavy Intensity Shower Rain", altDay: "39.png", altNight: "11.png"],
        531: [type: "Rain", desc: "Ragged Shower Rain", altDay: "39.png", altNight: "9.png"],

        // Snow
        600: [type: "Snow", desc: "Light Snow", altDay: "13.png", altNight: "13.png"],
        601: [type: "Snow", desc: "Snow", altDay: "14.png", altNight: "14.png"],
        602: [type: "Snow", desc: "Heavy Snow", altDay: "16.png", altNight: "16.png"],
        611: [type: "Snow", desc: "Sleet", altDay: "18.png", altNight: "18.png"],
        612: [type: "Snow", desc: "Light Shower Sleet", altDay: "18.png", altNight: "18.png"],
        613: [type: "Snow", desc: "Shower Sleet", altDay: "18.png", altNight: "18.png"],
        615: [type: "Snow", desc: "Light Rain and Snow", altDay: "5.png", altNight: "5.png"],
        616: [type: "Snow", desc: "Rain and Snow", altDay: "5.png", altNight: "5.png"],
        620: [type: "Snow", desc: "Light Shower Snow", altDay: "13.png", altNight: "13.png"],
        621: [type: "Snow", desc: "Shower Snow", altDay: "14.png", altNight: "14.png"],
        622: [type: "Snow", desc: "Heavy Shower Snow", altDay: "16.png", altNight: "16.png"],

        // Atmosphere
        701: [type: "Atmosphere", desc: "Mist", altDay: "20.png", altNight: "20.png"],
        711: [type: "Atmosphere", desc: "Smoke", altDay: "20.png", altNight: "20.png"],
        721: [type: "Atmosphere", desc: "Haze", altDay: "21.png", altNight: "21.png"],
        731: [type: "Atmosphere", desc: "Sand/Dust Whirls", altDay: "19.png", altNight: "19.png"],
        741: [type: "Atmosphere", desc: "Fog", altDay: "20.png", altNight: "20.png"],
        751: [type: "Atmosphere", desc: "Sand", altDay: "19.png", altNight: "19.png"],
        761: [type: "Atmosphere", desc: "Dust", altDay: "19.png", altNight: "19.png"],
        762: [type: "Atmosphere", desc: "Volcanic Ash", altDay: "19.png", altNight: "19.png"],
        771: [type: "Atmosphere", desc: "Squalls", altDay: "24.png", altNight: "24.png"],
        781: [type: "Atmosphere", desc: "Tornado", altDay: "24.png", altNight: "24.png"],

        // Clear & Clouds
        800: [type: "Clear",  desc: "Clear Sky", altDay: "32.png", altNight: "31.png"],
        801: [type: "Clouds", desc: "Few Clouds: 11-25%", altDay: "34.png", altNight: "29.png"],
        802: [type: "Clouds", desc: "Scattered Clouds: 25-50%", altDay: "30.png", altNight: "30.png"],
        803: [type: "Clouds", desc: "Broken Clouds: 51-84%", altDay: "28.png", altNight: "27.png"],
        804: [type: "Clouds", desc: "Overcast Clouds: 85-100%", altDay: "26.png", altNight: "26.png"]
    ]
    return conditions[code] ?: [type: "Unknown", desc: "Unknown Condition", altDay: "unknown.png", altNight: "unknown.png"]
}

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    device.updateSetting("logDebugEnable", [type: "bool", value: false])
}

// Unified dynamic logger helper mapping
private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "OpenWeatherMap Driver${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }