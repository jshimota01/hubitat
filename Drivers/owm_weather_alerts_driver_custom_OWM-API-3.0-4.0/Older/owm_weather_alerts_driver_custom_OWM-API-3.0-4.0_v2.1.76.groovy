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
		attribute "currentSnowText", "string"
		attribute "currentRainText", "string"
		attribute "currentTile", "string"
		attribute "currentVisibility", "number"
        attribute "currentTwilightBegin", "number"
        attribute "currentSunriseTime", "number"
        attribute "currentSolarNoonTime", "number"
        attribute "currentSunsetTime", "number"
        attribute "currentTwilightEnd", "number"
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
		attribute "todaySunrise", "number"
		attribute "todaySunset", "number"
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
		attribute "tomSunrise", "number"
		attribute "tomSunset", "number"
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
		attribute "tdaSunrise", "number"
		attribute "tdaSunset", "number"
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
        input name: "precisionPress", type: "enum", title: "Display Decimal Precision - Pressure", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for barometer readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 30mb,30.5mb, 30.55mb</i>", defaultValue: "2", required: true
        input name: "precisionSunAngles", type: "enum", title: "Display Decimal Precision - Sun Angles", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for sun angles (altitude and azimuth) readings in logging and tiles<br>Default: <b>0</b><br><i>EG(with Unit): 149°, 149.5°, 149.55°</i>", defaultValue: "0", required: true
        input name: "precisionTemp", type: "enum", title: "Display Decimal Precision - Temperature", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for temperature readings in logging and tiles<br>Default: <b>2</b><br><i>EG(with Unit): 70°F, 70.3°F, 70.55°F</i>", defaultValue: "2", required: true
        input name: "precisionWind", type: "enum", title: "Display Decimal Precision - Wind Speed", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for wind speed readings in logging and tiles<br>Default: <b>2</b><br><i>EG (with Unit): 12 mph, 12.7 mph, 12.77 mph</i>", defaultValue: "2", required: true
        
		// Display Options
		input name: "owmAlertsEnable", type: "bool", title: "Display Options - Enable Alerts Tile?", description: "Enable to Alert tile output updates on schedule for normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		input name: "sliceOfDayEnable", type: "bool", title: "Display Options - Enable Slice Of Day?", description: "Enable to slice of day text updates on schedule for normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		
        // Display Unit Selectors
		input name: "pressureUnit", type: "enum", title: "Display Unit - Barometric Pressure", options: ["hPa": "Hectopascals (hPa)", "inHg": "Inches of Mercury (inHg)", "kPa": "Kilopascals (kPa)", "mb": "Millibar (mb)", "mmHg": "Millimeters of Mercury (mmHg)", "none": "None (No Unit Suffix)"], description: "Choice of barometer unit used in tiles and logging<br>Default: <b>Inches of Mercury (inHg)</b>", defaultValue: "inHg", required: true
		input name: "illuminanceUnit", type: "enum", title: "Display Unit - Illuminance", options: ["lx": "Lux (lx)", "fc": "Foot-candle (fc)", "ph": "Phot (ph)", "none": "None (No Unit Suffix)"], description: "Choice of illuminance unit used in tiles and logging<br>Default: <b>Lux (lx)</b>", defaultValue: "lx", required: true
		input name: "precipUnit", type: "enum", title: "Display Unit - Precipitation (Rain/Snow)", options: ["mmHr": "Millimeters per Hour (mmHr)", "inHr": "Inches per Hour (inHr)", "none": "None (No Unit Suffix)"], description: "Choice of precipitation (both rain and snow) unit formatting used in tiles and logging<br>Default: <b>Inches per Hour (inHr)</b>", defaultValue: "inHr", required: true
		input name: "temperatureUnit", type: "enum", title: "Display Unit - Temperature", options: ["°F": "Fahrenheit (°F)", "°C": "Celsius (°C)", "K": "Kelvin (K)", "none": "None (No Unit Suffix)"], description: "Choice of temperature unit formatting used in tiles and logging<br>Default: <b>Fahrenheit (°F)</b>", defaultValue: "°F", required: true
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
    if (settings.pressureUnit == null) device.updateSetting("pressureUnit", [type: "enum", value: "inHg"])
    if (settings.illuminanceUnit == null) device.updateSetting("illuminanceUnit", [type: "enum", value: " lx"])
    if (settings.temperatureUnit == null) device.updateSetting("temperatureUnit", [type: "enum", value: "°F"])
    if (settings.windSpeedUnit == null) device.updateSetting("windSpeedUnit", [type: "enum", value: " mph"])
    if (settings.precipUnit == null) device.updateSetting("precipUnit", [type: "enum", value: " inHr"])

    // Update current device attributes to reflect defaults on initial install
    sendIfChanged(name: "pressureUnit", value: "inHg")
    sendIfChanged(name: "illuminanceUnit", value: " lx")
    sendIfChanged(name: "temperatureUnit", value: "°F")
    sendIfChanged(name: "windSpeedUnit", value: " mph")
    sendIfChanged(name: "precipUnit", value: " inHr")

    initialize()
}

def updated() {
    logInfo "Preferences updated. Running initialization ..."
    
    // Ensure changed unit selections immediately update device attributes
    sendIfChanged(name: "pressureUnit", value: settings.pressureUnit ?: "inHg")
    sendIfChanged(name: "illuminanceUnit", value: settings.illuminanceUnit ?: " lx")
    sendIfChanged(name: "temperatureUnit", value: settings.temperatureUnit ?: "°F")
    sendIfChanged(name: "windSpeedUnit", value: settings.windSpeedUnit ?: " mph")
    sendIfChanged(name: "precipUnit", value: settings.precipUnit ?: " inHr")	
    
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
        logInfo "Debug logging toggle is currently active. Auto-disable scheduled in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }

    // Fire an immediate poll to get current sunrise/sunset data and kick off dynamic scheduling
    runIn(2, "scheduledPoll")
}

void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    
    // Clears all data stored in the state map
    state.clear() 
    
    logInfo "All states have been cleared."
}

void clearAllDriverAttributes() {
    String attributesDeleted = ''
    device.properties.supportedAttributes.each { it -> 
        attributesDeleted += "${it}, " 
        device.deleteCurrentState("$it") 
    }
    logInfo "All current states (attributes) DELETED: ${attributesDeleted}"
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

    // Protect against invalid intervals (Sunrise must be before Sunset)
    if (sunriseHour >= sunsetHour) {
        logWarn "Dynamic scheduling warning: Sunrise hour (${sunriseHour}) is >= Sunset hour (${sunsetHour}). Defaulting to standard interval."
        schedule("0 0/30 * * * ?", "scheduledPoll") // Fallback safe poll every 30 mins
        return
    }

    // 1. Daytime Cron Generation (From Sunrise Hour up to Sunset Hour minus 1)
    if (dayInterval != "manual" && dayInterval) {
        int mins = dayInterval.toInteger()
        int endDayHour = sunsetHour - 1
        if (endDayHour < sunriseHour) endDayHour = sunriseHour // Guard rails
        
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
        if (endNightHour < 0) endNightHour = 0 // Guard rails
        
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
    
    // 1. Extract API specific response location metadata
    if (json.lat != null) sendIfChanged(name: "apiLatitude", value: json.lat)
    if (json.lon != null) sendIfChanged(name: "apiLongitude", value: json.lon)
    if (json.timezone != null) sendIfChanged(name: "apiTimezone", value: json.timezone)
    if (json.timezone_offset != null) sendIfChanged(name: "apiTimezoneOffset", value: json.timezone_offset)

    // Establish local variables to pass directly into scheduling, bypassing async state lag
    long liveSunrise = 0
    long liveSunset = 0

    // 2. Gather Current conditions dataset
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
        
        // Ensure rain and snow default to 0 if missing or nested improperly from OWM
        def rainVal = currentData.rain?.getAt("1h") != null ? currentData.rain["1h"] : 0.00
        def snowVal = currentData.snow?.getAt("1h") != null ? currentData.snow["1h"] : 0.00
        currentData["calculatedRain"] = rainVal 
        currentData["calculatedSnow"] = snowVal 
    }
    
    // 3. Process Daily forecast arrays safely
    def dailyList = json.daily ?: [] 
    
    // Gather Today data (data.0)
    def data0 = dailyList.size() > 0 ? dailyList[0] : [:]
    def data1 = dailyList.size() > 1 ? dailyList[1] : [:] 
    def data2 = dailyList.size() > 2 ? dailyList[2] : [:]
    
    // Route all isolated datasets into the custom event dispatcher
    sendOWMData(currentData, data0, data1, data2) 
    
    // --- FIXED: Pass the local variables directly so it never uses old state data ---
    if (liveSunrise > 0 && liveSunset > 0) {
        updateDynamicSchedules(liveSunrise, liveSunset) 
    }
	calcCurrentTwilight()
}

private void sendOWMData(Map current, Map today, Map tom, Map tda) {
    logDebug "sendOWMData initiated. Dispatching events to device attributes..."

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
		String preUnit = (settings.precipUnit == "none" || settings.precipUnit == null) ? "" : " ${settings.precipUnit}"

		// Cleaned up sendIfChanged calls
		sendIfChanged(name: "currentRain", value: rainVal)
		sendIfChanged(name: "currentSnow", value: snowVal)
		sendIfChanged(name: "currentRainText", value: "${rainVal}${preUnit}")
		sendIfChanged(name: "currentSnowText", value: "${snowVal}${preUnit}")
		
		// Current sunrise/sunset
		if (current.sunrise != null) sendIfChanged(name: "currentSunrise", value: current.sunrise)
		if (current.sunset  != null) sendIfChanged(name: "currentSunset",  value: current.sunset)
	
    //  EXAMPLE sendIfChanged(name: "currentIlluminanceText", value: "${luxVal} ${iUnit}")
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
        if (today.moonrise != null) sendIfChanged(name: "todayMoonrise", value: today.moonrise)
        if (today.moonset != null) sendIfChanged(name: "todayMoonset", value: today.moonset)
        if (today.moon_phase != null) sendIfChanged(name: "todayMoonPhase", value: today.moon_phase)
        
        // Nested temperature structures converted dynamically via user preferences layout
        if (today.temp) {
            if (today.temp.min != null) sendIfChanged(name: "todayTempMin", value: convertKelvin(today.temp.min))
            if (today.temp.max != null) sendIfChanged(name: "todayTempMax", value: convertKelvin(today.temp.max))
            if (today.temp.day != null) sendIfChanged(name: "todayTempDay", value: convertKelvin(today.temp.day))
            if (today.temp.night != null) sendIfChanged(name: "todayTempNight", value: convertKelvin(today.temp.night))
            if (today.temp.eve != null) sendIfChanged(name: "todayTempEve", value: convertKelvin(today.temp.eve))
            if (today.temp.morn != null) sendIfChanged(name: "todayTempMorn", value: convertKelvin(today.temp.morn))
        }
		// Nested feels like temperature structures converted dynamically via user preferences layout
        if (today.feels_like) {
            if (today.feels_like.day != null) sendIfChanged(name: "todayFeelsLikeDay", value: convertKelvin(today.feels_like.day))
            if (today.feels_like.night != null) sendIfChanged(name: "todayFeelsLikeNight", value: convertKelvin(today.feels_like.night))
            if (today.feels_like.eve != null) sendIfChanged(name: "todayFeelsLikeEve", value: convertKelvin(today.feels_like.eve))
            if (today.feels_like.morn != null) sendIfChanged(name: "todayFeelsLikeMorn", value: convertKelvin(today.feels_like.morn))
        }
    }

    // ==========================================
    // 3. TOMORROW DATA DISPATCHES (data.1)
    // ==========================================
    if (tom) {
        if (tom.pop != null) sendIfChanged(name: "tomPOP", value: tom.pop)
        if (tom.summary != null) sendIfChanged(name: "tomSummary", value: tom.summary)
        if (tom.moonrise != null) sendIfChanged(name: "tomMoonrise", value: tom.moonrise)
        if (tom.moonset != null) sendIfChanged(name: "tomMoonset", value: tom.moonset)
        if (tom.moon_phase != null) sendIfChanged(name: "tomMoonPhase", value: tom.moon_phase)
        
        if (tom.temp) {
            if (tom.temp.min != null) sendIfChanged(name: "tomTempMin", value: convertKelvin(tom.temp.min))
            if (tom.temp.max != null) sendIfChanged(name: "tomTempMax", value: convertKelvin(tom.temp.max))
            if (tom.temp.day != null) sendIfChanged(name: "tomTempDay", value: convertKelvin(tom.temp.day))
            if (tom.temp.night != null) sendIfChanged(name: "tomTempNight", value: convertKelvin(tom.temp.night))
            if (tom.temp.eve != null) sendIfChanged(name: "tomTempEve", value: convertKelvin(tom.temp.eve))
            if (tom.temp.morn != null) sendIfChanged(name: "tomTempMorn", value: convertKelvin(tom.temp.morn))
        }
		// Nested feels like temperature structures converted dynamically via user preferences layout
        if (tom.feels_like) {
            if (tom.feels_like.day != null) sendIfChanged(name: "tomFeelsLikeDay", value: convertKelvin(tom.feels_like.day))
            if (tom.feels_like.night != null) sendIfChanged(name: "tomFeelsLikeNight", value: convertKelvin(tom.feels_like.night))
            if (tom.feels_like.eve != null) sendIfChanged(name: "tomFeelsLikeEve", value: convertKelvin(tom.feels_like.eve))
            if (tom.feels_like.morn != null) sendIfChanged(name: "tomFeelsLikeMorn", value: convertKelvin(tom.feels_like.morn))
        }
    }

    // ==========================================
    // 4. DAY AFTER TOMORROW DATA DISPATCHES (data.2)
    // ==========================================
    // ==========================================
    if (tda) {
        if (tda.pop != null) sendIfChanged(name: "tdaPOP", value: tda.pop)
        if (tda.summary != null) sendIfChanged(name: "tdaSummary", value: tda.summary)
        if (tda.moonrise != null) sendIfChanged(name: "tdaMoonrise", value: tda.moonrise)
        if (tda.moonset != null) sendIfChanged(name: "tdaMoonset", value: tda.moonset)
        if (tda.moon_phase != null) sendIfChanged(name: "tdaMoonPhase", value: tda.moon_phase)
        
        if (tda.temp) {
            if (tda.temp.min != null) sendIfChanged(name: "tdaTempMin", value: convertKelvin(tda.temp.min))
            if (tda.temp.max != null) sendIfChanged(name: "tdaTempMax", value: convertKelvin(tda.temp.max))
            if (tda.temp.day != null) sendIfChanged(name: "tdaTempDay", value: convertKelvin(tda.temp.day))
            if (tda.temp.night != null) sendIfChanged(name: "tdaTempNight", value: convertKelvin(tda.temp.night))
            if (tda.temp.eve != null) sendIfChanged(name: "tdaTempEve", value: convertKelvin(tda.temp.eve))
            if (tda.temp.morn != null) sendIfChanged(name: "tdaTempMorn", value: convertKelvin(tda.temp.morn))
        }
        if (tda.feels_like) {
            if (tda.feels_like.day != null) sendIfChanged(name: "tdaFeelsLikeDay", value: convertKelvin(tda.feels_like.day))
            if (tda.feels_like.night != null) sendIfChanged(name: "tdaFeelsLikeNight", value: convertKelvin(tda.feels_like.night))
            if (tda.feels_like.eve != null) sendIfChanged(name: "tdaFeelsLikeEve", value: convertKelvin(tda.feels_like.eve))
            if (tda.feels_like.morn != null) sendIfChanged(name: "tdaFeelsLikeMorn", value: convertKelvin(tda.feels_like.morn))
        }
    }
	
	// Trigger the illuminance calculation right before concluding the lifecycle dispatch
	BigDecimal currentAlt = state.sunAltitude != null ? state.sunAltitude.toBigDecimal() : (device.currentValue("altitude")?.toBigDecimal() ?: 0.0)
        
    // Execute illuminance math now that cloud cover and sun position are synchronized
    BigDecimal freshLux = calcCurrentIlluminance(currentAlt)
	calcCurrentText(freshLux)
	
    logDebug "sendOWMData event parsing complete."
}

private String calcIconBasePath(String altIconLoc) {
	//	https://tinyurl.com/icnqz/ points to https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/
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
            convertedValue = rawLux * 0.00001
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
    if (finalValue < 0) finalValue = 0.0
    
    return finalValue
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
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertPrecip(precipVal) {
    // If precipVal is null, empty string, or evaluates as false/missing, immediately return 0 formatted to precision
    if (precipVal == null || precipVal == "") {
        int precision = settings.precisionPrecip != null ? settings.precisionPrecip.toInteger() : 2
        return new BigDecimal("0.0").setScale(precision, BigDecimal.ROUND_HALF_UP)
    }
    
    // Ensure we are starting with a clean BigDecimal representation
    BigDecimal precip = (precipVal instanceof BigDecimal) ? precipVal : new BigDecimal(precipVal.toString())
    
    // 1. Handle unit conversion if necessary (OWM returns mm)
    String unit = settings.precipUnit ?: "inHr"
    if (unit == "inHr" || unit == "in") {
        precip = precip * 0.0393701
    }
    
    // 2. Grab precision selection from preferences
    int precision = settings.precisionPrecip != null ? settings.precisionPrecip.toInteger() : 2
    
    // 3. Scale and round using HALF_UP strategy
    return precip.setScale(precision, BigDecimal.ROUND_HALF_UP)
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
    int precision = (settings.precisionPress ?: "2").toInteger()
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
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
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
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
    double g = 357.529 + 0.98560028 * d        // Mean anomaly of the Sun
    double q = 280.459 + 0.98564736 * d        // Mean longitude of the Sun
    double L = q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g)) // Ecliptic longitude
    double e = 23.439 - 0.00000036 * d         // Obliquity of the ecliptic

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
        
        if (Math.sin(hRad) > 0) {
            az = 360.0 - az
        }
    } else {
        az = (locLat > 0) ? 180.0 : 0.0
    }

    // Normalize Azimuth loop to 0-360 boundaries
    az = (az % 360.0 + 360.0) % 360.0

    // 7. Process precision preferences and build attributes
    BigDecimal finalAltitude = BigDecimal.valueOf(alt).setScale(precision, java.math.RoundingMode.HALF_UP)
    BigDecimal finalAzimuth = BigDecimal.valueOf(az).setScale(precision, java.math.RoundingMode.HALF_UP)

    logInfo "calcSunPosition: Solar Altitude computed as ${finalAltitude}°, Azimuth as ${finalAzimuth}°"

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

        sendIfChanged(name: "currentTwilightBegin", value: twilightBeginEpoch)
        sendIfChanged(name: "currentTwilightEnd", value: twilightEndEpoch)
    } else {
        logWarn "calcCurrentTwilight skipped: Missing todaySunriseEpoch or todaySunsetEpoch in state."
    }
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
            sendIfChanged(name: "currentSolarNoonTime", value: noonTimeStr)
        } catch (Exception e) {
            logError "Exception occurred while calculating currentSolarNoonTime: ${e.message}"
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

private BigDecimal calcCurrentIlluminance(BigDecimal altitude) { // <-- Changed to BigDecimal
    logDebug "Calculating dynamic current illuminance adjusted for chosen unit..."
    
    // Fetch dependencies safely from existing device attributes
    def cloudPctVal = device.currentValue("currentCloudPCT")

    logDebug "calcCurrentIlluminance() - altitude value being used in calculation: ${altitude}"
	logDebug "calcCurrentIlluminance() cloudPctVal value being used in calculation: ${cloudPctVal}"
    
    if (cloudPctVal == null || altitude == null) {
        logDebug "calcCurrentIlluminance postponed: Waiting for cloud percentage or sun altitude data."
        return 0.0
    }
    
    BigDecimal clouds = cloudPctVal.toBigDecimal()
        
    // If the sun is below the horizon, illuminance is 0 across all units
    if (altitude <= 0) {
        sendIfChanged(name: "currentIlluminance", value: 0)
        sendIfChanged(name: "illuminance", value: 0) // Core capability compliance
        return 0.0 // <-- Return 0
    }
    
    // Step 1: Compute maximum potential clear sky lux based on sun altitude angle
    double radians = Math.toRadians(altitude.doubleValue())
    BigDecimal clearSkyLux = 100000 * Math.sin(radians)
    
    // Step 2: Apply linear cloud percentage reduction factor
    BigDecimal cloudFactor = (100 - clouds) / 100
    BigDecimal rawLuxCalculation = clearSkyLux * cloudFactor
    
    // Step 3: Apply the 0.75 attenuation factor
    BigDecimal attenuatedLux = rawLuxCalculation * 0.75
    
    // Step 4: Adjust calculation based on preferred illuminanceUnit preference selection
    String targetUnit = settings.illuminanceUnit ?: "lx"
    
    BigDecimal finalValue = convertIlluminance(attenuatedLux)

    // Cap Lux at its relative 100k maximum if math exceeds standard constraints
    if (targetUnit == "lx" && finalValue > 100000) finalValue = 100000   
    logDebug "Illuminance Parsed: ${finalValue} ${targetUnit} (Base Lux: ${attenuatedLux.setScale(0, 4)} lx)"

	// Handle your final attribute updates using the converted results
	sendIfChanged(name: "currentIlluminance", value: finalValue)
    sendIfChanged(name: "illuminance", value: finalValue) // Maps directly to capability "IlluminanceMeasurement" 

    return finalValue // <-- Added this return statement
}

private void calcCurrentText(BigDecimal freshLux = null) { // <-- Accept parameter with null fallback
    logDebug "Generating formatted text attributes with unit suffixes..."

    // 1. Temperature Text Formatting
    def tempVal = device.currentValue("currentTemperature")
    if (tempVal != null) {
        String tUnit = settings.temperatureUnit ?: "°F"
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
    // Use the real-time parameter passed from sendOWMData if available, otherwise look up cache
    def luxVal = (freshLux != null) ? freshLux : device.currentValue("currentIlluminance")
    if (luxVal != null) {
        String iUnit = settings.illuminanceUnit ?: "lx"
        if (iUnit == "none") iUnit = "lx" // Fallback to standard reading string if none selected
        sendIfChanged(name: "currentIlluminanceText", value: "${luxVal} ${iUnit}")
    }
	
	// 5. Solar Angles Formatting
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
        
        def params = [
            uri: geoUrl,
            contentType: "application/json",
            timeout: 10
        ]
        
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
			// example - https://api.openweathermap.org/geo/1.0/reverse?lat=45.27219&lon=-123.00785&limit=1&appid=cddd5d10f3b7521ac34097c2c4f24da0
            String reverseGeoUrl = "https://api.openweathermap.org/geo/1.0/reverse?lat=${usedLatitude}&lon=${usedLongitude}&limit=1&appid=${apiKey}"
            
            def params = [
                uri: reverseGeoUrl,
                contentType: "application/json",
                timeout: 10
            ]
            
            try {
                httpGet(params) { response ->
                    if (response.status == 200 && response.data && response.data.size() > 0) {
                        def locationData = response.data[0]
                        usedCity = locationData.name ?: "Local Area"
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
        logDebug "Enforcing latitude fallback context. Target assigned: ${usedLatitude}"
    }
    if (!usedLongitude || usedLongitude == 0.0) {
        usedLongitude = currentLon ?: location.longitude?.toBigDecimal() ?: 0.0
        logDebug "Enforcing longitude fallback context. Target assigned: ${usedLongitude}"
    }
    if (!usedCity || usedCity.trim() == "") {
        usedCity = "Local Area"
    }

    // Commit calculated configurations into global variables for API calls
    state.usedCity = usedCity
    state.usedLatitude = usedLatitude
    state.usedLongitude = usedLongitude

    // Cache the current settings so we can compare against them next time
    state.lastOverrideCity = currentCity
    state.lastOverrideLatitude = currentLat
    state.lastOverrideLongitude = currentLon
    logDebug "Completed calcLonLatCityState. Used City: ${state.usedCity}"
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

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    device.updateSetting("logDebugEnable", [type: "bool", value: false])
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