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
        attribute "lastUpdated", "string"
        attribute "lastResponseCode", "string"
        attribute "betwixt", "string"
        attribute "overrideCity", "string"
        attribute "overrideLongitude", "number"
        attribute "overrideLatitude", "number"
		attribute "pressureUnit", "string"
		attribute "temperatureUnit", "string"
		attribute "windSpeedUnit", "string"
		attribute "illuminanceUnit", "string"
		
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

        // - Calculated Polling Timestamps attributes
        attribute "lastPollForecast", "string"
        attribute "lastObservationForecast", "string"

		// - Current unique attributes
		attribute "currentSnow", "number"
		attribute "currentRain", "number"
        attribute "currentFeelsLike", "number"
		attribute "currentTemperature", "number"

		// - Current unique derived attributes
        attribute "currentIlluminanceText", "string"
		attribute "currentPressureText", "string"
		attribute "currentTemperatureText", "string"
		attribute "currentWindSpeedText", "string"
		attribute "currentTile", "string"
		attribute "currentVisibility", "number"
        attribute "currentTwilightBegin", "string"
        attribute "currentSunriseTime", "string"
        attribute "currentNoonTime", "string"
        attribute "currentSunsetTime", "string"
        attribute "currentTwilightEnd", "string"
		attribute "currentIsDay", "enum", ["true","false"]

		// - Current Condition attributes
		attribute "currentConditionCode", "number"
		attribute "currentConditionType", "string"
		attribute "currentConditionTypeFull", "string"
		attribute "currentConditionIcon", "string"
		
		// - Current Condition derived attribute
		attribute "currentConditionAltIcon", "string"
		attribute "currentConditionIconImg", "string"

		// - Shared attributes (used both current and forecast)
		attribute "currentIlluminance", "number"
        attribute "currentPressure", "number"
		attribute "currentHumidity", "number"
		attribute "currentDewPoint", "number"
		attribute "currentUVI", "number"
		attribute "currentCloudPCT", "number"
		attribute "currentSunrise", "number"
		attribute "currentSunset", "number"
        attribute "currentWindGust", "number"
        attribute "currentWindDeg", "number"
        attribute "currentWindSpeed", "number"		
		
		// - Shared derived attributes (used both current and forescast)
		attribute "currentWindDirection", "string"
		
		// - Forecast today unique attributes
        attribute "todayMoonrise", "number"
        attribute "todayMoonset", "number"
        attribute "todayMoonPhase", "number"
		attribute "todayPOP", "number"
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
				
		// - Forecast tomorrow unique attributes
        attribute "tomMoonrise", "number"
        attribute "tomMoonset", "number"
        attribute "tomMoonPhase", "number"
		attribute "tomPOP", "number"
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
		
		// - Forecast tomorrow dayafter unique attributes
        attribute "tdaMoonrise", "number"
        attribute "tdaMoonset", "number"
        attribute "tdaMoonPhase", "number"
		attribute "tdaPOP", "number"
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

		// - Forecast unique derived attributes

		// - UNKNOWN ATTRIBUTES
        attribute "forecastIcon", "string"
        attribute "percentPrecip", "number"
        attribute "weather", "string"
        attribute "weatherIcon", "string"
        attribute "weatherIcons", "string"
	
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
		
		// Need to look into this to see why it was implemented. I am not using it
		// input 'luxjitter', 'bool', title: 'Use lux jitter control (rounding)?', required: true, defaultValue: false
		
        input name: "overrideLatitude", type: "decimal", title: "Base Override - Latitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "overrideLongitude", type: "decimal", title: "Base Override - Longitude", description: "Optional - Leave blank to use Hub location", required: false
		input name: "altIconsEnable", type: "bool", title: "Base Override - Use Alternative Icons?", description: "Turn ON to use alternate icons (found in csv map within the driver), or OFF to use the standard OpenWeatherMap icons<br><b>Base Override - Icon Location MUST be filled!</b>", defaultValue: false, required: true
		
		// Display Selector Options
        input name: "precisionPrecip", type: "enum", title: "Display Decimal Precision - Precipitation", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for rainfall readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 1, 1.5, 1.55</i>", defaultValue: "2", required: true
        input name: "precisionPressure", type: "enum", title: "Display Decimal Precision - Pressure", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for barometer readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 30mb,30.5mb, 30.55mb</i>", defaultValue: "2", required: true
        input name: "precisionSunAngles", type: "enum", title: "Display Decimal Precision - Sun Angles", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for sun angles (altitude and azimuth) readings in logging and tiles<br>Default: <b>0</b><br><i>EG(with Unit): 149°, 149.5°, 149.55°</i>", defaultValue: "0", required: true
        input name: "precisionTemp", type: "enum", title: "Display Decimal Precision - Temperature", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for temperature readings in logging and tiles<br>Default: <b>2</b><br><i>EG(with Unit): 70°F, 70.3°F, 70.55°F</i>", defaultValue: "2", required: true
        input name: "precisionWind", type: "enum", title: "Display Decimal Precision - Wind Speed", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for wind speed readings in logging and tiles<br>Default: <b>2</b><br><i>EG (with Unit): 12 mph, 12.7 mph, 12.77 mph</i>", defaultValue: "2", required: true
        
		// Display Options
		input name: "owmAlertsEnable", type: "bool", title: "Display Options - Enable Alerts Tile?", description: "Enable to Alert tile output updates on schedule for normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		input name: "sliceOfDayEnable", type: "bool", title: "Display Options - Enable Slice Of Day?", description: "Enable to slice of day text updates on schedule for normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		
        // Display Unit Selectors
		input name: "pressureUnit", type: "enum", title: "Display Unit - Barometric Pressure", options: ["inHg": "Mercury (inHg)", "hPa": "Hectopascals (hPa)", "mb": "Millibar (mb)", "none": "None (No Unit Suffix)"], description: "Choice of barometer unit used in tiles and logging<br>Default: <b>Mercury (inHg)</b>", defaultValue: "inHg", required: true
		input name: "illuminanceUnit", type: "enum", title: "Display Unit - Illuminance", options: ["lx": "Lux (lx)", "fc": "Foot-candle (fc)", "ph": "Phot (ph)", "none": "None (No Unit Suffix)"], description: "Choice of illuminance unit used in tiles and logging<br>Default: <b>Lux (lx)</b>", defaultValue: "lx", required: true
		input name: "temperatureUnit", type: "enum", title: "Display Unit - Temperature", options: ["°F": "Fahrenheit (°F)", "°C": "Celsius (°C)", "K": "Kelvin (K)", "none": "None (No Unit Suffix)"], description: "Choice of temperature unit formatting used in tiles and logging<br>Default: <b>Fahrenheit (°F)</b>", defaultValue: "°F", required: true
		input name: "windSpeedUnit", type: "enum", title: "Display Unit - Wind Speed", options: ["mph": "Miles per Hour (mph)", "kmh": "Kilometers per Hour (km/h)", "kt": "Knots (kt)", "ms": "Meters per Second (m/s)", "none": "None (No Unit Suffix)"], description: "Choice of wind speed unit used in tiles and logging<br>Default: <b>Miles per Hour(mph)</b>", defaultValue: "mph", required: true		
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
    if (settings.pressureUnit == null) device.updateSetting("pressureUnit", [type: "enum", value: "inHg"])
    if (settings.illuminanceUnit == null) device.updateSetting("illuminanceUnit", [type: "enum", value: "lx"])
    if (settings.temperatureUnit == null) device.updateSetting("temperatureUnit", [type: "enum", value: "°F"])
    if (settings.windSpeedUnit == null) device.updateSetting("windSpeedUnit", [type: "enum", value: "mph"])

    // Update current device attributes to reflect defaults on initial install
    sendIfChanged(name: "pressureUnit", value: "inHg")
    sendIfChanged(name: "illuminanceUnit", value: " lx")
    sendIfChanged(name: "temperatureUnit", value: "°F")
    sendIfChanged(name: "windSpeedUnit", value: " mph")

    initialize()
}

def updated() {
    logInfo "Preferences updated. Running initialization ..."
    
    // Ensure changed unit selections immediately update device attributes
    sendIfChanged(name: "pressureUnit", value: settings.pressureUnit ?: "inHg")
    sendIfChanged(name: "illuminanceUnit", value: settings.illuminanceUnit ?: " lx")
    sendIfChanged(name: "temperatureUnit", value: settings.temperatureUnit ?: "°F")
    sendIfChanged(name: "windSpeedUnit", value: settings.windSpeedUnit ?: " mph")
    
    if (!settings.altIconLoc || settings.altIconLoc.trim() == "") {
        if (settings.altIconsEnable == true) {
            logWarn "Alternative Icon Location is empty/default. Forcing 'Use Alternative Icons' to OFF for safety."
            device.updateSetting("altIconsEnable", [type: "bool", value: false])
        }
    }
    initialize()
}

def initialize() {
    unschedule()
    logInfo "Initializing driver ..."  
  
    if (logDebugEnable == true) {
        logInfo "Debug logging toggle is currently active. Auto-disable scheduled in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }

    // Fire an immediate poll to get current sunrise/sunset data and kick off dynamic scheduling
    runIn(2, "scheduledPoll")
}

void clearAllDriverStates() {
    log.info "Clearing all driver states..."
    
    // Clears all data stored in the state map
    state.clear() 
    
    log.info "All states have been cleared."
}

void clearAllDriverAttributes() {
    String attributesDeleted = ''
    device.properties.supportedAttributes.each { it -> 
        attributesDeleted += "${it}, " 
        device.deleteCurrentState("$it") 
    }
    log.info "All current states (attributes) DELETED: ${attributesDeleted}"
}

def scheduledPoll() {
    logDebug "Scheduled background poll sequence initiated."
    pollOWM("schedule")
}

// This command executes automatically from your generated cron schedules inside initialize()
def refresh() {
    logDebug "Refresh triggered via schedule or button press."
    
    // Safety check preference values
    if (!apiKey) {
        logWarn "Execution halted: API Key entry is missing!"
        return
    }

    // Execution to owm Poll logic block, alerting that it was invoked by refresh
    pollOWM("refresh") 
}

private void updateDynamicSchedules(long sunriseEpoch, long sunsetEpoch) {
    // Unschedule previous scheduledPoll instances so we don't stack cron jobs
    unschedule("scheduledPoll")

    if (dayInterval == "manual" && nightInterval == "manual") {
        logInfo "Both daytime and nighttime polling are set to MANUAL. Dynamic scheduling skipped."
        return
    }

    // Convert epoch seconds to local hours (0-23)
    int sunriseHour = new Date(sunriseEpoch * 1000).format("H", location.timeZone).toInteger()
    int sunsetHour = new Date(sunsetEpoch * 1000).format("H", location.timeZone).toInteger()
    
    logDebug "Dynamic scheduling boundaries parsed -> True Sunrise Hour: ${sunriseHour}, True Sunset Hour: ${sunsetHour}"

    // 1. Daytime Cron Generation (From Sunrise Hour up to Sunset Hour minus 1)
    if (dayInterval != "manual" && dayInterval) {
        int mins = dayInterval.toInteger()
        int endDayHour = sunsetHour - 1
        String dayCronStr = ""
        
        if (mins < 60) {
            dayCronStr = "0 0/${mins} ${sunriseHour}-${endDayHour} * * ?"
        } else {
            int hours = mins / 60
            dayCronStr = "0 0 ${sunriseHour}-${endDayHour}/${hours} * * ?"
        }
        logDebug "Generated dynamic daytime cron string: ${dayCronStr}"
        schedule(dayCronStr, "scheduledPoll")
    }

    // 2. Nighttime Cron Generation (From Sunset Hour through midnight to Sunrise Hour minus 1)
    if (nightInterval != "manual" && nightInterval) {
        int mins = nightInterval.toInteger()
        int endNightHour = sunriseHour - 1
        String nightCronStr = ""
        
        if (mins < 60) {
            nightCronStr = "0 0/${mins} ${sunsetHour}-23,0-${endNightHour} * * ?"
        } else {
            int hours = mins / 60
            nightCronStr = "0 0 ${sunsetHour}-23/${hours},0-${endNightHour}/${hours} * * ?"
        }
        logDebug "Generated dynamic nighttime cron string: ${nightCronStr}"
        schedule(nightCronStr, "scheduledPoll")
    }
}

def pollOWM(String type = "manual") {
    // Evaluation of execution triggers using logInfo
    switch(type) {
        case "refresh":
            logInfo "pollOWM run on manual Refresh"
            break
        case "schedule":
            logInfo "polling OpenWeatherMaps API on schedule"
            break
        case "manual":
        default:
            logInfo "PollOWM run manually"
            break
    }

    logDebug "pollOWM triggered. Evaluating location coordinates..."
    
    // Ensure state variables exist by evaluating coordinate overrides
    calcLonLatCityState()
    
    if (state.usedLatitude == 0.0 || state.usedLongitude == 0.0) {
        logWarn "pollOWM aborted: Valid coordinates are missing (Lat: ${state.usedLatitude}, Lon: ${state.usedLongitude})"
        return
    }

    // Execution to check sun position for use in calcBetwixt and calcDayState blocks
    calcSunPosition()
	
    // Execution for certain variables used in parsed data returned from pollOWMAPI
	BigDecimal currentAlt = calcSunPosition()
	calcBetwixtState(currentAlt)
	calcIsDayState(currentAlt)
	
    // Resolve path safely and save it to state before API execution
    state.iconBasePath = calcIconBasePath(settings.altIconLoc)
	
    // Fire off the API poll sequence
    pollOWMAPI()
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
    
    def params = [
        uri: apiUrl,
        contentType: "application/json",
        timeout: 15
    ]
    
    logDebug "Polling OpenWeatherMap via URL: ${apiUrl}"
    
    try {
        httpGet(params) { response ->
            if (response.status == 200 && response.data) {
                sendIfChanged(name: "lastResponseCode", value: response.status.toString())
                sendIfChanged(name: "lastUpdated", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))
                
                // Route the payload to the custom data extractor
                parseOWMData(response.data)
            } else {
                logError "OWM API call failed with status code: ${response.status}"
                sendIfChanged(name: "lastResponseCode", value: response.status.toString())
            }
        }
    } catch (Exception e) {
        logError "Exception during OWM API Call execution: ${e.message}"
    }
}

private void parseOWMData(Map json) {
    if (!json) {
        logWarn "parseOWMData received an empty payload map."
        return
    }
    
    logDebug "Parsing newly received OpenWeatherMap response data structure..."
    
    // Extract location and configuration details for the alert builder
    String calculatedCityAttr = state.usedCity ?: "Local Area"
    String iconBasePath = state.iconBasePath ?: "https://tinyurl.com/icnqz/"
    
    // Execute alerts calculation with live payload data
    calcAlertsState(json, calculatedCityAttr, iconBasePath)
    
    // 1. Gather Current conditions dataset
    def currentData = json.current ?: [:]
    if (currentData) {
        logTrace "Current weather data payload extracted successfully."
        
        // --- ADDED: Extract and track true sunrise/sunset epochs ---
        if (currentData.sunrise) state.todaySunriseEpoch = currentData.sunrise.toLong()
        if (currentData.sunset) state.todaySunsetEpoch = currentData.sunset.toLong()
        
        // Ensure rain and snow default to 0 if missing or nested improperly from OWM
        def rainVal = currentData.rain?.getAt("1h") != null ? currentData.rain["1h"] : 0
        def snowVal = currentData.snow?.getAt("1h") != null ? currentData.snow["1h"] : 0
        
        currentData["calculatedRain"] = rainVal
        currentData["calculatedSnow"] = snowVal
    }
    
    // 2. Process Daily forecast arrays safely 
    def dailyList = json.daily ?: []
    
    // Gather Today data (data.0)
    def data0 = dailyList.size() > 0 ? dailyList[0] : [:]
    // ... [keep your existing data1 and data2 extraction logic here] ...

    // Route all isolated datasets into the custom event dispatcher
    sendOWMData(currentData, data0, data1, data2)
    
    // --- ADDED: Dynamically rebuild cron schedules based on the new true values ---
    if (state.todaySunriseEpoch && state.todaySunsetEpoch) {
        updateDynamicSchedules(state.todaySunriseEpoch, state.todaySunsetEpoch)
    }
}

private void sendOWMData(Map current, Map today, Map tom, Map tda) {
    logDebug "sendOWMData initiated. Dispatching events to device attributes..."

    // ==========================================
    // 1. CURRENT DATA DISPATCHES
    // ==========================================
    if (current) {
        // Safe dispatch of newly captured precipitation attributes
        sendIfChanged(name: "currentRain", value: current.calculatedRain != null ? current.calculatedRain : 0)
        sendIfChanged(name: "currentSnow", value: current.calculatedSnow != null ? current.calculatedSnow : 0)

        if (current.temp != null) {
            BigDecimal calcTemp = convertKelvin(current.temp)
            sendIfChanged(name: "currentTemperature", value: calcTemp)
            sendIfChanged(name: "temperature", value: calcTemp) // Map to Core Capability
        }
        if (current.feels_like != null) {
            sendIfChanged(name: "currentFeelsLike", value: convertKelvin(current.feels_like))
        }
        if (current.dew_point != null) {
            sendIfChanged(name: "currentDewPoint", value: convertKelvin(current.dew_point))
        }
        if (current.humidity != null) {
            sendIfChanged(name: "currentHumidity", value: current.humidity)
            sendIfChanged(name: "humidity", value: current.humidity) // Map to Core Capability
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
        if (current.clouds != null) sendIfChanged(name: "currentCloudPCT", value: current.clouds)
        if (current.visibility != null) sendIfChanged(name: "currentVisibility", value: current.visibility)
        
        // Wind Speed elements converted from m/s using user selection preference logic
        if (current.wind_speed != null) {
            sendIfChanged(name: "currentWindSpeed", value: convertWindSpeed(current.wind_speed))
        }
        if (current.wind_deg != null) {
            sendIfChanged(name: "currentWindDeg", value: current.wind_deg)
        }
        if (current.wind_gust != null) {
            sendIfChanged(name: "currentWindGust", value: convertWindSpeed(current.wind_gust))
        }
        
        // Handle nested weather condition arrays safely if available
        if (current.weather && current.weather[0]) {
            sendIfChanged(name: "currentConditionCode", value: current.weather[0].id)
            sendIfChanged(name: "currentConditionType", value: current.weather[0].main)
            sendIfChanged(name: "currentConditionTypeFull", value: current.weather[0].description)
            sendIfChanged(name: "currentConditionIcon", value: current.weather[0].icon)
        }
    }

    // ==========================================
    // 2. TODAY DATA DISPATCHES (data.0)
    // ==========================================
    if (today) {
        if (today.pop != null) sendIfChanged(name: "todayPOP", value: today.pop)
        if (today.summary != null) sendIfChanged(name: "todaySummary", value: today.summary)
        if (today.moon_phase != null) sendIfChanged(name: "todayMoonPhase", value: today.moon_phase)
        
        // Nested temperature structures converted dynamically via user preferences layout
        if (today.temp) {
            if (today.temp.min != null) sendIfChanged(name: "todayTempMin", value: convertKelvin(today.temp.min))
            if (today.temp.max != null) sendIfChanged(name: "todayTempMax", value: convertKelvin(today.temp.max))
            if (today.temp.day != null) sendIfChanged(name: "todayTempDay", value: convertKelvin(today.temp.day))
            if (today.temp.night != null) sendIfChanged(name: "todayTempNight", value: convertKelvin(today.temp.night))
        }
    }

    // ==========================================
    // 3. TOMORROW DATA DISPATCHES (data.1)
    // ==========================================
    if (tom) {
        if (tom.pop != null) sendIfChanged(name: "tomPOP", value: tom.pop)
        if (tom.summary != null) sendIfChanged(name: "tomSummary", value: tom.summary)
        if (tom.moon_phase != null) sendIfChanged(name: "tomMoonPhase", value: tom.moon_phase)
        
        if (tom.temp) {
            if (tom.temp.min != null) sendIfChanged(name: "tomTempMin", value: convertKelvin(tom.temp.min))
            if (tom.temp.max != null) sendIfChanged(name: "tomTempMax", value: convertKelvin(tom.temp.max))
            if (tom.temp.day != null) sendIfChanged(name: "tomTempDay", value: convertKelvin(tom.temp.day))
            if (tom.temp.night != null) sendIfChanged(name: "tomTempNight", value: convertKelvin(tom.temp.night))
        }
    }

    // ==========================================
    // 4. DAY AFTER TOMORROW DATA DISPATCHES (data.2)
    // ==========================================
    // ==========================================
    if (tda) {
        if (tda.pop != null) sendIfChanged(name: "tdaPOP", value: tda.pop)
        if (tda.summary != null) sendIfChanged(name: "tdaSummary", value: tda.summary)
        if (tda.moon_phase != null) sendIfChanged(name: "tdaMoonPhase", value: tda.moon_phase)
        
        if (tda.temp) {
            if (tda.temp.min != null) sendIfChanged(name: "tdaTempMin", value: convertKelvin(tda.temp.min))
            if (tda.temp.max != null) sendIfChanged(name: "tdaTempMax", value: convertKelvin(tda.temp.max))
            if (tda.temp.day != null) sendIfChanged(name: "tdaTempDay", value: convertKelvin(tda.temp.day))
            if (tda.temp.night != null) sendIfChanged(name: "tdaTempNight", value: convertKelvin(tda.temp.night))
        }
    }
	
	// Trigger the illuminance calculation right before concluding the lifecycle dispatch
    calcCurrentIlluminance()
	calcCurrentText()
	
    logDebug "sendOWMData event parsing complete."
}

		//	https://tinyurl.com/icnqz/ points to https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/
private String calcIconBasePath(String altIconLoc) {
    String base = altIconLoc ? altIconLoc.trim() : ""
    
    // Fall back to target default URL if empty or null
    if (base == "") {
        base = "https://tinyurl.com/icnqz"
    }
    
    // Enforce trailing slash constraint
    if (!base.endsWith("/")) {
        base += "/"
    }
    
    logDebug "Calculated Icon Base Path resolved to: ${base}"
    return base
}

private BigDecimal convertKelvin(def kelvinVal) {
    if (kelvinVal == null) return 0.0
    
    BigDecimal K = kelvinVal.toBigDecimal()
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
            converted = K
            break
    }
    
    // Apply user chosen decimal precision layout
    int precision = (settings.precisionTemp ?: "2").toInteger()
    return converted.setScale(precision, 4)
}

private BigDecimal convertPressure(def hpaVal) {
    if (hpaVal == null) return 0.0
    
    BigDecimal hpa = hpaVal.toBigDecimal()
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
            converted = hpa
            break
    }
    
    // Apply user chosen pressure decimal precision
    int precision = (settings.precisionPressure ?: "2").toInteger()
    return converted.setScale(precision, 4)
}

private BigDecimal convertWindSpeed(def msVal) {
    if (msVal == null) return 0.0
    
    BigDecimal ms = msVal.toBigDecimal()
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
            converted = ms
            break
    }
    
    // Apply user chosen wind speed decimal precision
    int precision = (settings.precisionWind ?: "2").toInteger()
    return converted.setScale(precision, 4)
}

private void calcAlertsState(Map json, String calculatedCityAttr, String iconBasePath) {
    // Safely look up alerts array out of the incoming payload map
    def alerts = json?.alerts ?: []
    String alertActive = "No active alerts"
    String currentAlertSender = "N/A"
    String currentAlertDesc = "No active alerts"
    
    String lastPollTime = new Date().format("HH:mm", location.timeZone)
    String currentAlertDescFull = "No active alerts for ${calculatedCityAttr} at last poll as of ${lastPollTime}"
    
    String alertIconUrl = "${iconBasePath}OWM.png"
    
    String currentAlertTile = "<div style='text-align:center;'>No active weather alerts from<br>Source: OpenWeatherMap</div>" + 
                       "<div style='text-align:center; margin-top:5px; font-size:0.8em;'>" + 
                       "<img src='${alertIconUrl}' style='max-width:25px; max-height:25px; vertical-align:middle; margin-right:5px;'/>" + 
                       "Updated ${lastPollTime}</div>"
                       
    if (alerts.size() > 0) {
        def a = alerts[0]
        alertActive = a.event ?: "Active Alert"
        currentAlertSender = a.sender_name ?: "Unknown"
		currentAlertDesc = a.description ? (a.description.length() > 100 ? a.description.take(100) + "..." : a.description) : "No active alerts"
        currentAlertDescFull = (a.description ?: "N/A") + " as of ${lastPollTime}"
        
        currentAlertTile = "<div style='color:red; font-weight:bold; text-align:center;'>${alertActive}</div>" + 
                    "<div style='font-size:0.8em;'>${currentAlertDesc}</div>" + 
                    "<div style='text-align:center; margin-top:5px; font-size:0.8em;'>" + 
                    "<img src='${alertIconUrl}' style='max-width:25px; max-height:25px; vertical-align:middle; margin-right:5px;'/>" + 
                    "as of ${lastPollTime}</div>"
    }

    sendIfChanged(name: "currentAlert", value: alertActive)
    sendIfChanged(name: "currentAlertSender", value: currentAlertSender)
    sendIfChanged(name: "currentAlertDesc", value: currentAlertDesc)
    sendIfChanged(name: "currentAlertDescFull", value: currentAlertDescFull)
    sendIfChanged(name: "currentAlertTile", value: currentAlertTile)
}

private void calcBetwixtState(BigDecimal altitude) {
    if (settings.sliceOfDayEnable == false) {
        sendIfChanged(name: "betwixt", value: "Disabled in device preferences")
        return
    }

    String sliceText = "fully night time"
    long currentEpoch = (new Date().getTime() / 1000)
    long sunriseEpoch = state.todaySunriseEpoch ?: 0
    long sunsetEpoch = state.todaySunsetEpoch ?: 0
    
    boolean isTwilightAngle = (altitude >= -6.0 && altitude < -0.833)
    boolean isSunUp = (altitude >= -0.833)
    
    if (sunriseEpoch > 0 && sunsetEpoch > 0) { 
        long midDayEpoch = sunriseEpoch + ((sunsetEpoch - sunriseEpoch) / 2)
        
        // --- ADDED LOGIC FOR CURRENT NOON TIME ---
        try {
            String noonTimeStr = new Date(midDayEpoch * 1000).format("HH:mm", location.timeZone)
            sendIfChanged(name: "currentNoonTime", value: noonTimeStr)
        } catch (Exception e) {
            logError "Exception occurred while calculating currentNoonTime: ${e.message}"
        }
        // ----------------------------------------

        if (currentEpoch < midDayEpoch) {
            if (isTwilightAngle) {
                sliceText = "between twilight and sunrise"
            } else if (isSunUp) {
                sliceText = "between sunrise and noon"
            }
        } else {
            if (isSunUp) {
                sliceText = "between noon and sunset"
            } else if (isTwilightAngle) {
                sliceText = "between sunset and twilight"
            }
        }
    } else {
        if (isTwilightAngle) {
            sliceText = "between twilight and sunrise"
        } else if (isSunUp) {
            sliceText = "between sunrise and noon"
        }
    }

    sendIfChanged(name: "betwixt", value: sliceText)
    logDebug "Calculated betwixt slice: ${sliceText} (Current Altitude: ${altitude}°)"
}

private BigDecimal calcSunPosition() {
    // 1. Retrieve location coordinates from state (falling back to location object if missing)
    def lat = (state.usedLatitude != null) ? state.usedLatitude.toBigDecimal() : location.latitude.toBigDecimal()
    def lon = (state.usedLongitude != null) ? state.usedLongitude.toBigDecimal() : location.longitude.toBigDecimal()

    // 2. Fetch the precision setting from preferences, defaulting to "0" if null
    int precision = (settings.precisionSunAngles ?: "0").toInteger()

    // 3. Time calculations (Universal Time / Julian Dates)
    def now = new Date()
    def lct = now.getTime() / 1000.0 // Local time in Unix timestamp seconds
    
    // Calculate Julian Date (JD) and Julian Centuries (JC) from 2000 epoch
    def jd = (lct / 86400.0) + 2440587.5
    def jc = (jd - 2451545.0) / 36525.0

    // 4. Solar geometric equations
    def rawGeomMeanLongSun = 280.46646 + jc * (36000.76983 + jc * 0.0003032)
    // FIX: Using .remainder() instead of % to prevent BigDecimal mod errors
    def geomMeanLongSun = rawGeomMeanLongSun.toBigDecimal().remainder(360.0.toBigDecimal()).doubleValue()
    
    def geomMeanAnomSun = 357.52911 + jc * (35999.05029 - 0.0001537 * jc)
    def eccentEarthOrbit = 0.016708634 - jc * (0.000042037 + 0.0000001267 * jc)
    
    def sunEqOfCtr = Math.sin(Math.toRadians(geomMeanAnomSun)) * (1.914602 - jc * (0.004817 + 0.000014 * jc)) + 
                     Math.sin(Math.toRadians(2.0 * geomMeanAnomSun)) * (0.019993 - 0.000101 * jc) + 
                     Math.sin(Math.toRadians(3.0 * geomMeanAnomSun)) * 0.000289
                     
    def sunTrueLong = geomMeanLongSun + sunEqOfCtr
    def sunAppLong = sunTrueLong - 0.00569 - 0.00478 * Math.sin(Math.toRadians(125.04 - 1934.13 * jc))
    def meanObliqEcliptic = 23.439291 - jc * (46.815 + jc * (0.00059 - jc * 0.001813)) / 3600.0
    def obliqCorr = meanObliqEcliptic + 0.00256 * Math.cos(Math.toRadians(125.04 - 1934.13 * jc))
    
    // Solar Declination
    def sunDeclination = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(obliqCorr)) * Math.sin(Math.toRadians(sunAppLong))))
    
    def varY = Math.tan(Math.toRadians(obliqCorr / 2.0)) * Math.tan(Math.toRadians(obliqCorr / 2.0))
    def eqOfTime = 4.0 * Math.toDegrees(varY * Math.sin(2.0 * Math.toRadians(geomMeanLongSun)) - 
                   2.0 * eccentEarthOrbit * Math.sin(Math.toRadians(geomMeanAnomSun)) + 
                   4.0 * eccentEarthOrbit * varY * Math.sin(Math.toRadians(geomMeanAnomSun)) * Math.cos(2.0 * Math.toRadians(geomMeanLongSun)) - 
                   0.5 * varY * varY * Math.sin(4.0 * Math.toRadians(geomMeanLongSun)) - 
                   1.25 * eccentEarthOrbit * eccentEarthOrbit * Math.sin(2.0 * Math.toRadians(geomMeanAnomSun)))

    // 5. Hour Angle calculation
    def timeZoneOffset = (location.timeZone.getOffset(now.getTime()) / 3600000.0) // Local timezone offset
    def rawTrueSolarTime = ((lct / 60.0) + eqOfTime + (4.0 * lon) - (60.0 * timeZoneOffset))
    // FIX: Using .remainder() instead of % to prevent BigDecimal mod errors
    def trueSolarTime = rawTrueSolarTime.toBigDecimal().remainder(1440.0.toBigDecimal()).doubleValue()
    def hourAngle = (trueSolarTime / 4.0 < 0) ? (trueSolarTime / 4.0 + 180.0) : (trueSolarTime / 4.0 - 180.0)

    // 6. Solar Zenith, Altitude and Azimuth angles
    def solarZenith = Math.toDegrees(Math.acos(Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(sunDeclination)) + 
                      Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(sunDeclination)) * Math.cos(Math.toRadians(hourAngle))))
                      
    def rawAltitude = 90.0 - solarZenith
    
    def rawAzimuth = 0.0
    if (hourAngle > 0) {
        def calculatedAzimuth = (Math.toDegrees(Math.acos(((Math.sin(Math.toRadians(lat)) * Math.cos(Math.toRadians(solarZenith))) - Math.sin(Math.toRadians(sunDeclination))) / 
                     (Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(solarZenith))))) + 180.0)
        // FIX: Using .remainder() instead of % to prevent BigDecimal mod errors
        rawAzimuth = calculatedAzimuth.toBigDecimal().remainder(360.0.toBigDecimal()).doubleValue()
    } else {
        def calculatedAzimuth = (540.0 - Math.toDegrees(Math.acos(((Math.sin(Math.toRadians(lat)) * Math.cos(Math.toRadians(solarZenith))) - Math.sin(Math.toRadians(sunDeclination))) / 
                     (Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(solarZenith))))))
        // FIX: Using .remainder() instead of % to prevent BigDecimal mod errors
        rawAzimuth = calculatedAzimuth.toBigDecimal().remainder(360.0.toBigDecimal()).doubleValue()
    }

    // 7. Enforce precision rules via BigDecimal scaling
    BigDecimal finalAltitude = BigDecimal.valueOf(rawAltitude).setScale(precision, BigDecimal.ROUND_HALF_UP)
    BigDecimal finalAzimuth = BigDecimal.valueOf(rawAzimuth).setScale(precision, BigDecimal.ROUND_HALF_UP)

    // 8. Publish the rounded numbers to the device attributes
    sendIfChanged(name: "altitude", value: finalAltitude)
    sendIfChanged(name: "azimuth", value: finalAzimuth)
    
    // Optional clean text representation for UI tiles
    sendIfChanged(name: "altitudeText", value: "${finalAltitude}°")
    sendIfChanged(name: "azimuthText", value: "${finalAzimuth}°")
	
	return finalAltitude
}

private void calcIsDayState(BigDecimal altitude) {
	
    String isDayText = "false"
    long currentEpoch = (new Date().getTime() / 1000)
    long sunriseEpoch = state.todaySunriseEpoch ?: 0
    long sunsetEpoch = state.todaySunsetEpoch ?: 0
    
    boolean isSunUp = (altitude >= -0.833) 
    
    if (sunriseEpoch > 0 && sunsetEpoch > 0) { 
        long midDayEpoch = sunriseEpoch + ((sunsetEpoch - sunriseEpoch) / 2) 
        if (currentEpoch < midDayEpoch) { 
            if (isSunUp) isDayText = "true" 
        } else {
            if (isSunUp) isDayText = "true" 
        }
    } else {
        if (isSunUp) isDayText = "true" 
    }
    
    sendIfChanged(name: "currentIsDay", value: isDayText) 
    logTrace "Calculated currentIsDay: ${isDayText}" 
}

private void calcCurrentIlluminance() {
    logDebug "Calculating dynamic current illuminance adjusted for chosen unit..."
    
    // Fetch dependencies safely from existing device attributes
    def cloudPctVal = device.currentValue("currentCloudPCT")
    def altitudeVal = device.currentValue("altitude")
    
    if (cloudPctVal == null || altitudeVal == null) {
        logDebug "calcCurrentIlluminance postponed: Waiting for cloud percentage or sun altitude data."
        return
    }
    
    BigDecimal clouds = cloudPctVal.toBigDecimal()
    BigDecimal altitude = altitudeVal.toBigDecimal()
    
    // If the sun is below the horizon, illuminance is 0 across all units
    if (altitude <= 0) {
        sendIfChanged(name: "currentIlluminance", value: 0)
        sendIfChanged(name: "illuminance", value: 0) // Core capability compliance
        return
    }
    
    // Step 1: Compute maximum potential clear sky lux based on sun altitude angle
    // Baseline max of 100,000 Lux for direct overhead sunlight
    double radians = Math.toRadians(altitude.doubleValue())
    BigDecimal clearSkyLux = 100000 * Math.sin(radians)
    
    // Step 2: Apply linear cloud percentage reduction factor
    BigDecimal cloudFactor = (100 - clouds) / 100
    BigDecimal rawLuxCalculation = clearSkyLux * cloudFactor
    
    // Step 3: Apply the 0.75 attenuation factor
    BigDecimal attenuatedLux = rawLuxCalculation * 0.75
    
    // Step 4: Adjust calculation based on preferred illuminanceUnit preference selection
    String targetUnit = settings.illuminanceUnit ?: "lx"
    BigDecimal convertedValue = attenuatedLux
    int precision = 0 // Default to 0 places for pure Lux readings
    
    switch (targetUnit) {
        case "fc":
            // Lux to Foot-candle conversion: lx * 0.092903
            convertedValue = attenuatedLux * 0.09290304
            precision = 1 // Standard practice allows decimal precision for lower values like fc
            break
        case "ph":
            // Lux to Phot conversion: lx * 0.0001
            convertedValue = attenuatedLux * 0.0001
            precision = 4 // Phot readings are highly compact and require deeper decimal fields
            break
        case "lx":
        case "none":
        default:
            // Remain as Standard Lux
            convertedValue = attenuatedLux
            precision = 0
            break
    }
    
    // Enforce valid boundaries and round using appropriate scale rules
    BigDecimal finalValue = convertedValue.setScale(precision, 4)
    if (finalValue < 0) finalValue = 0
    
    // Cap Lux at its relative 100k maximum if math exceeds standard constraints
    if (targetUnit == "lx" && finalValue > 100000) finalValue = 100000
    
    logDebug "Illuminance Parsed: ${finalValue} ${targetUnit} (Base Lux: ${attenuatedLux.setScale(precision, 4)} lx)"
    
    // Persist to the matching attributes
    sendIfChanged(name: "currentIlluminance", value: finalValue)
    sendIfChanged(name: "illuminance", value: finalValue) // Maps directly to capability "IlluminanceMeasurement" 
}

private void calcCurrentText() {
    logDebug "Generating formatted text attributes with unit suffixes..."

    // 1. Temperature Text Formatting
    def tempVal = device.currentValue("currentTemperature")
    if (tempVal != null) {
        String tUnit = settings.temperatureUnit ?: "°F"
        // Cleanup selection values to display cleanly as text labels
        if (tUnit == "f") tUnit = "°F"
        if (tUnit == "c") tUnit = "°C"
        if (tUnit == "k") tUnit = "K"
        sendIfChanged(name: "currentTemperatureText", value: "${tempVal} ${tUnit}")
    }

    // 2. Pressure Text Formatting
    def pressVal = device.currentValue("currentPressure")
    if (pressVal != null) {
        String pUnit = settings.pressureUnit ?: "inHg"
        sendIfChanged(name: "currentPressureText", value: "${pressVal} ${pUnit}")
    }

    // 3. Wind Speed Text Formatting
    def windVal = device.currentValue("currentWindSpeed")
    if (windVal != null) {
        String wUnit = settings.windSpeedUnit ?: "mph"
        sendIfChanged(name: "currentWindSpeedText", value: "${windVal} ${wUnit}")
    }

    // 4. Illuminance Text Formatting
    def luxVal = device.currentValue("currentIlluminance")
    if (luxVal != null) {
        String iUnit = settings.illuminanceUnit ?: "lx"
        if (iUnit == "none") iUnit = "lx" // Fallback to standard reading string if none selected
        sendIfChanged(name: "currentIlluminanceText", value: "${luxVal} ${iUnit}")
    }
	
	// 5. Append inside your existing calcCurrentText() routine
    def altVal = device.currentValue("altitude")
    if (altVal != null) {
        sendIfChanged(name: "altitudeText", value: "${altVal}°")
    }

    def azVal = device.currentValue("azimuth")
    if (azVal != null) {
        sendIfChanged(name: "azimuthText", value: "${azVal}°")
    }
}

private void calcLonLatCityState() {
    // Read current input settings values safely
    String currentCity = settings.overrideCity ?: ""
    BigDecimal currentLat = settings.overrideLatitude ? settings.overrideLatitude.toBigDecimal() : null
    BigDecimal currentLon = settings.overrideLongitude ? settings.overrideLongitude.toBigDecimal() : null

    // Optimisation check: If preferences match our last evaluated values, skip reprocessing
    if (state.lastOverrideCity == currentCity &&
        state.lastOverrideLatitude == currentLat &&
        state.lastOverrideLongitude == currentLon &&
        state.usedLatitude != null && state.usedLongitude != null) {
        logDebug "Preferences unchanged. Skipping geo-lookup and using cached values."
        return
    }

    // Define the placeholder variables to output to String
    String usedCity = ""
    BigDecimal usedLatitude = 0.0
    BigDecimal usedLongitude = 0.0

    // Base URL for the OWM Geocoding API
    String geoApiUrl = "https://api.openweathermap.org/geo/1.0/"

    // -------------------------------------------------------------
    // SCENARIO 1: If overrideCity is filled, prioritize it entirely
    // -------------------------------------------------------------
    if (settings.overrideCity && settings.overrideCity.trim() != "") {
        logDebug "Scenario 1: overrideCity is populated ('${settings.overrideCity}'). Performing Direct Geo-Lookup."
        try {
            def encodedCity = URLEncoder.encode(settings.overrideCity.trim(), "UTF-8")
            def params = [
                uri: "${geoApiUrl}direct?q=${encodedCity}&limit=1&appid=${apiKey}",
                contentType: "application/json",
                timeout: 10
            ]
            httpGet(params) { response ->
                if (response.status == 200 && response.data) {
                    def geoData = response.data[0]
                    if (geoData) {
                        usedCity = geoData.name ?: settings.overrideCity
                        usedLatitude = geoData.lat ? geoData.lat.toBigDecimal() : 0.0
                        usedLongitude = geoData.lon ? geoData.lon.toBigDecimal() : 0.0
                        logInfo "Direct Geo-Lookup success. Resolved to: ${usedCity} (${usedLatitude}, ${usedLongitude})"
                    } else {
                        logWarn "Direct Geo-Lookup returned no matching results for: ${settings.overrideCity}"
                    }
                } else {
                    logError "Direct Geo-Lookup failed with status code: ${response.status}"
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
        usedCity = "Local Area"
        usedLatitude = currentLat ?: location.latitude?.toBigDecimal()
        usedLongitude = currentLon ?: location.longitude?.toBigDecimal()
    }

    // -------------------------------------------------------------
    // EXTRA PROTECTION: Enforce fallback to Hub defaults if values 
    // remain blank or zero (e.g. failed lookups or empty settings)
    // -------------------------------------------------------------
    if (!usedLatitude || usedLatitude == 0.0) {
        usedLatitude = currentLat ?: location.latitude?.toBigDecimal() ?: 0.0
        logDebug "Enforcing latitude fallback context. Target assigned: ${usedLatitude}"
    }
    if (!usedLongitude || usedLongitude == 0.0) {
        usedLongitude = currentLon ?: location.longitude?.toBigDecimal() ?: 0.0
        logDebug "Enforcing longitude fallback context. Target assigned: ${usedLongitude}"
    }

    // Commit calculated configurations into global variables for API calls
    state.usedCity = usedCity
    state.usedLatitude = usedLatitude
    state.usedLongitude = usedLongitude

    // Cache the current settings so we can compare against them next time
    state.lastOverrideCity = currentCity
    state.lastOverrideLatitude = currentLat
    state.lastOverrideLongitude = currentLon

    logDebug "Completed calcLonLatCityState. Outputs -> City: ${usedCity} | Lat: ${usedLatitude} | Lon: ${usedLongitude}"
}

private void sendIfChanged(Map args) {
    if (!args || !args.name) return

    // Standardize checking logic
    String oldVal = device.currentValue(args.name as String)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        // Ensure standard Hubitat tracking options are appended safely
        Map eventMap = [
            name: args.name, 
            value: args.value, 
            descriptionText: "Attribute ${args.name} changed to ${args.value}"
        ]
        if (args.unit) eventMap.unit = args.unit
        
        sendEvent(eventMap)
        logDebug "Event triggered: ${args.name} -> ${args.value}"
    }
}

private Map lookupConditionDetails(Integer code) {
    def map = [:]
    switch(code) {
        case 200: map = [type: "Thunderstorm", desc: "Thunderstorm with Light Rain", altDay: "38.png", altNight: "47.png"]; break
        case 201: map = [type: "Thunderstorm", desc: "Thunderstorm with Rain", altDay: "38.png", altNight: "47.png"]; break
        case 202: map = [type: "Thunderstorm", desc: "Thunderstorm with Heavy Rain", altDay: "38.png", altNight: "47.png"]; break
        case 210: map = [type: "Thunderstorm", desc: "Light Thunderstorm", altDay: "38.png", altNight: "47.png"]; break
        case 211: map = [type: "Thunderstorm", desc: "Thunderstorm", altDay: "38.png", altNight: "47.png"]; break
        case 212: map = [type: "Thunderstorm", desc: "Heavy Thunderstorm", altDay: "38.png", altNight: "47.png"]; break
        case 221: map = [type: "Thunderstorm", desc: "Ragged Thunderstorm", altDay: "38.png", altNight: "47.png"]; break
        case 230: map = [type: "Thunderstorm", desc: "Thunderstorm with Light Drizzle", altDay: "38.png", altNight: "47.png"]; break
        case 231: map = [type: "Thunderstorm", desc: "Thunderstorm with Drizzle", altDay: "38.png", altNight: "47.png"]; break
        case 232: map = [type: "Thunderstorm", desc: "Thunderstorm with Heavy Drizzle", altDay: "38.png", altNight: "47.png"]; break

        case 300: map = [type: "Drizzle", desc: "Light Intensity Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 301: map = [type: "Drizzle", desc: "Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 302: map = [type: "Drizzle", desc: "Heavy Intensity Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 310: map = [type: "Drizzle", desc: "Light Intensity Drizzle Rain", altDay: "9.png", altNight: "9.png"]; break
        case 311: map = [type: "Drizzle", desc: "Drizzle Rain", altDay: "9.png", altNight: "9.png"]; break
        case 312: map = [type: "Drizzle", desc: "Heavy Intensity Drizzle Rain", altDay: "9.png", altNight: "9.png"]; break
        case 313: map = [type: "Drizzle", desc: "Shower Rain and Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 314: map = [type: "Drizzle", desc: "Heavy Shower Rain and Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 321: map = [type: "Drizzle", desc: "Shower Drizzle", altDay: "9.png", altNight: "9.png"]; break

        case 500: map = [type: "Rain", desc: "Light Rain", altDay: "39.png", altNight: "9.png"]; break
        case 501: map = [type: "Rain", desc: "Moderate Rain", altDay: "39.png", altNight: "11.png"]; break
        case 502: map = [type: "Rain", desc: "Heavy Intensity Rain", altDay: "39.png", altNight: "11.png"]; break
        case 503: map = [type: "Rain", desc: "Very Heavy Rain", altDay: "39.png", altNight: "11.png"]; break
        case 504: map = [type: "Rain", desc: "Extreme Rain", altDay: "39.png", altNight: "11.png"]; break
        case 511: map = [type: "Rain", desc: "Freezing Rain", altDay: "39.png", altNight: "11.png"]; break
        case 520: map = [type: "Rain", desc: "Light Intensity Shower Rain", altDay: "39.png", altNight: "9.png"]; break
        case 521: map = [type: "Rain", desc: "Shower Rain", altDay: "39.png", altNight: "11.png"]; break
        case 522: map = [type: "Rain", desc: "Heavy Intensity Shower Rain", altDay: "39.png", altNight: "11.png"]; break
        case 531: map = [type: "Rain", desc: "Ragged Shower Rain", altDay: "39.png", altNight: "9.png"]; break

        case 600: map = [type: "Snow", desc: "Light Snow", altDay: "13.png", altNight: "13.png"]; break
        case 601: map = [type: "Snow", desc: "Snow", altDay: "14.png", altNight: "14.png"]; break
        case 602: map = [type: "Snow", desc: "Heavy Snow", altDay: "16.png", altNight: "16.png"]; break
        case 611: map = [type: "Snow", desc: "Sleet", altDay: "18.png", altNight: "18.png"]; break
        case 612: map = [type: "Snow", desc: "Light Shower Sleet", altDay: "18.png", altNight: "18.png"]; break
        case 613: map = [type: "Snow", desc: "Shower Sleet", altDay: "18.png", altNight: "18.png"]; break
        case 615: map = [type: "Snow", desc: "Light Rain and Snow", altDay: "5.png", altNight: "5.png"]; break
        case 616: map = [type: "Snow", desc: "Rain and Snow", altDay: "5.png", altNight: "5.png"]; break
        case 620: map = [type: "Snow", desc: "Light Shower Snow", altDay: "13.png", altNight: "13.png"]; break
        case 621: map = [type: "Snow", desc: "Shower Snow", altDay: "14.png", altNight: "14.png"]; break
        case 622: map = [type: "Snow", desc: "Heavy Shower Snow", altDay: "16.png", altNight: "16.png"]; break

        case 701: map = [type: "Atmosphere", desc: "Mist", altDay: "20.png", altNight: "20.png"]; break
        case 711: map = [type: "Atmosphere", desc: "Smoke", altDay: "20.png", altNight: "20.png"]; break
        case 721: map = [type: "Atmosphere", desc: "Haze", altDay: "21.png", altNight: "21.png"]; break
        case 731: map = [type: "Atmosphere", desc: "Sand/Dust Whirls", altDay: "19.png", altNight: "19.png"]; break
        case 741: map = [type: "Atmosphere", desc: "Fog", altDay: "20.png", altNight: "20.png"]; break
        case 751: map = [type: "Atmosphere", desc: "Sand", altDay: "19.png", altNight: "19.png"]; break
        case 761: map = [type: "Atmosphere", desc: "Dust", altDay: "19.png", altNight: "19.png"]; break
        case 762: map = [type: "Atmosphere", desc: "Volcanic Ash", altDay: "19.png", altNight: "19.png"]; break
        case 771: map = [type: "Atmosphere", desc: "Squalls", altDay: "24.png", altNight: "24.png"]; break
        case 781: map = [type: "Atmosphere", desc: "Tornado", altDay: "24.png", altNight: "24.png"]; break

        case 800: map = [type: "Clear", desc: "Clear Sky", altDay: "32.png", altNight: "31.png"]; break

        case 801: map = [type: "Clouds", desc: "Few Clouds: 11-25%", altDay: "34.png", altNight: "29.png"]; break
        case 802: map = [type: "Clouds", desc: "Scattered Clouds: 25-50%", altDay: "30.png", altNight: "30.png"]; break
        case 803: map = [type: "Clouds", desc: "Broken Clouds: 51-84%", altDay: "28.png", altNight: "27.png"]; break
        case 804: map = [type: "Clouds", desc: "Overcast Clouds: 85-100%", altDay: "26.png", altNight: "26.png"]; break

        default: map = [type: "Unknown", desc: "Unknown Condition", altDay: "unknown.png", altNight: "unknown.png"]; break
    }
    return map
}

private void logInfo(String msg) {
    if (logInfoEnable) log.info "OpenWeatherMap Driver: ${msg}"
}

private void logDebug(String msg) {
    if (logDebugEnable) log.debug "OpenWeatherMap Driver: ${msg}"
}

private void logTrace(String msg) {
    if (logTraceEnable) log.trace "OpenWeatherMap Driver: ${msg}"
}

private void logWarn(String msg) {
    if (logWarnEnable) log.warn "OpenWeatherMap Driver WARNING: ${msg}"
}

private void logError(String msg) {
    if (logErrorEnable) log.error "OpenWeatherMap Driver ERROR: ${msg}"
}