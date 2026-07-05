/**
 * OpenWeatherMap Multi-Version Weather Driver (2.5 / 3.0 / 4.0)
 * Platform: Hubitat Elevation
 * Capabilities: Temperature, Illuminance, Relative Humidity, Ultraviolet Index
 */

metadata {
    definition(name: "OpenWeatherMap Multi-Version Weather Driver", namespace: "jshimota", author: "James Shimota") {
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
        attribute "city", "string"
		
		// - Api specific response attributes
		attribute "apiLatitude", "number"
		attribute "apiLongitude", "number"
		attribute "apiTimezone", "string"
		attribute "apiTimezoneOffset", "number"
		
        // - Alert attributes
        attribute "currentAlert", "string"
        attribute "currentAlertTile", "string"
        attribute "currentAlertDesc", "string"
        attribute "currentAlertSender", "string"
        attribute "currentAlertDescFull", "string"
        
        // - Calculated Solar Angles attributes
        attribute "altitude", "number"
        attribute "azimuth", "number"

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
	
        command "pollOWM"
    }

    preferences {
        input name: "apiKey", type: "text", title: "API Key", description: "Enter your OpenWeatherMap API Key here<br><b>Required by OpenWeatherMaps</b>", required: true
        input name: "apiSelection", type: "enum", title: "API Version", options: ["2.5": "One Call 2.5 (Obsolete!)","3.0": "One Call 3.0", "4.0": "One Call 4.0"], defaultValue: "3.0", description: "Select your API Key version here<br><b>Required by OpenWeatherMaps</b><br><i>*Note: 2.5 API is now obsolete as of June 2024</i><br><i>*Note: 4.0 API key uses 3.0 API poll method</i>", required: true
        
        // Optional City field that dynamically overrides latitude/longitude if populated
        input name: "overrideCity", type: "text", title: "Base Override - City", description: "Optional - Will attempt to geo lookup and override <b>ALL</b> latitude/longitude values<br><b>Default:(empty)</b><br><i>EG: Portland, OR or London, UK.<br>*Note: Overrides Latitude/Longitude parameters of Hub <b>AND</b> values configured below</i>", required: false
		input name: "altIconLoc", type: "text", title: "Base Override - Icon Location", description: "Optional - Icon Source Location:<br><i>blank for default OWM location</i>", required: false
        input name: "apiLatitude", type: "text", title: "Base Override - Latitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "apiLongitude", type: "text", title: "Base Override - Longitude", description: "Optional - Leave blank to use Hub location", required: false
		input name: "altIconsEnable", type: "bool", title: "Base Override - Use Alternative Icons?", description: "Turn ON to use alternate icons (found in csv map within the driver), or OFF to use the standard OpenWeatherMap icons<br><b>Base Override - Icon Location MUST be filled!</b>", defaultValue: false, required: true
		
		// Display Selector Options
        input name: "precisionPrecip", type: "enum", title: "Display Decimal Precision - Precipitation", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for rainfall readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 1, 1.5, 1.55</i>", defaultValue: "2", required: true
        input name: "precisionPressure", type: "enum", title: "Display Decimal Precision - Pressure", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for barometer readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 30 mb, 30.5 mb, 30.55 mb</i>", defaultValue: "2", required: true
        input name: "precisionTemp", type: "enum", title: "Display Decimal Precision - Temperature", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for temperature readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 70 °F, 70.3 °F, 70.55 °F</i>", defaultValue: "2", required: true
        input name: "precisionWind", type: "enum", title: "Display Decimal Precision - Wind Speed", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision  for wind speed readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 12 mph, 12.7 mph, 12.77 mph</i>", defaultValue: "2", required: true
        
		// Display Options
		input name: "owmAlertsEnable", type: "bool", title: "Display Options - Enable Alerts Tile?", description: "Enable to Alert tile output updates on schedule for normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		input name: "sliceOfDayEnable", type: "bool", title: "Display Options - Enable Slice Of Day?", description: "Enable to slice of day text updates on schedule for normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		
        // Display Unit Selectors
		input name: "pressureUnit", type: "enum", title: "Display Unit - Barometric Pressure", options: ["inHg": "Mercury (inHg)", "hPa": "Hectopascals (hPa)", "mb": "Millibar (mb)", "none": "None (No Unit Suffix)"], description: "Choice of barometer unit used in tiles and logging<br>Default: <b>Mercury (inHg)</b>", defaultValue: "inHg", required: true
		input name: "illuminanceUnit", type: "enum", title: "Display Unit - Illuminance", options: ["lx": "Lux (lx)", "fc": "Foot-candle (fc)", "ph": "Phot (ph)", "none": "None (No Unit Suffix)"], description: "Choice of illuminance unit used in tiles and logging<br>Default: <b>Lux (lx)</b>", defaultValue: "lx", required: true
		input name: "temperatureUnit", type: "enum", title: "Display Unit - Temperature", options: ["f": "Fahrenheit (°F)", "c": "Celsius (°C)", "k": "Kelvin (K)", "none": "None (No Unit Suffix)"], description: "Choice of temperature unit formatting used in tiles and logging<br>Default: <b>Fahrenheit (°F)</b>", defaultValue: "f", required: true
		input name: "windUnit", type: "enum", title: "Display Unit - Wind Speed", options: ["mph": "Miles per Hour (mph)", "kmh": "Kilometers per Hour (km/h)", "kt": "Knots (kt)", "ms": "Meters per Second (m/s)", "none": "None (No Unit Suffix)"], description: "Choice of wind speed unit used in tiles and logging<br>Default: <b>Miles per Hour(mph)</b>", defaultValue: "mph", required: true		
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
    initialize()
}

def updated() {
    logInfo "Preferences updated, re-initializing driver rules..."
    
    if (!settings.altIconLoc || settings.altIconLoc.trim() == "") {
        if (settings.altIconsEnable == true) {
            log.warn "Alternative Icon Location is empty/default. Forcing 'Use Alternative Icons' to OFF for safety."
            device.updateSetting("altIconsEnable", [type: "bool", value: false])
        }
    }
    initialize()
}

def initialize() {
    unschedule()
    
    if (logDebugEnable == true) {
        log.info "Debug logging toggle is currently active. Auto-disable scheduled in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }

    if (dayInterval == "manual") {
        logInfo "Daytime polling interval configured to MANUAL. Automatic daylight scheduling skipped."
    } else if (dayInterval) {
        String dayCronStr = ""
        int mins = dayInterval.toInteger()
        
        if (mins < 60) {
            dayCronStr = "0 0/${mins} 6-17 * * ?"
        } else {
            int hours = mins / 60
            dayCronStr = "0 0 6-17/${hours} * * ?"
        }
        
        logDebug "Generated daytime cron string: ${dayCronStr}"
        schedule(dayCronStr, "refresh")
    }

    if (nightInterval == "manual") {
        logInfo "Nighttime polling interval configured to MANUAL. Automatic night scheduling skipped."
    } else if (nightInterval) {
        String nightCronStr = ""
        int mins = nightInterval.toInteger()
        
        if (mins < 60) {
            nightCronStr = "0 0/${mins} 0-5,18-23 * * ?"
        } else {
            int hours = mins / 60
            nightCronStr = "0 0 0-5/${hours},18-23/${hours} * * ?"
        }
        
        logDebug "Generated nighttime cron string: ${nightCronStr}"
        schedule(nightCronStr, "refresh")
    }
}

// This command executes automatically from your generated cron schedules inside initialize()
def refresh() {
    logDebug "Refresh triggered via schedule or button press."
    
    // Safety check preference values
    if (!apiKey) {
        logWarn "Execution halted: API Key entry is missing!"
        return
    }
    
    // Redirect execution to your main data retrieval logic block
    executeOwmCall() 
}

// This handles the custom user dashboard command on the device profile details view
def pollOWM() {
    logInfo "Manual pollOWM command invoked by user."
    refresh()
}
/**
 * Parses the main JSON payload from OWM One Call API
 * @param json The parsed JSON map object from the HTTP response
 */
private void parseOwmResponse(Map json) {
    if (!json) {
        logError "Null response received, skipping parse."
        return
    }

    // 1. Parse Current Weather Node
    if (json.current) {
        logDebug "Parsing current weather data..."
        def cur = json.current
        
        sendIfChanged(name: "currentTemperature", value: cur.temp)
        sendIfChanged(name: "currentFeelsLike", value: cur.feels_like)
        sendIfChanged(name: "currentHumidity", value: cur.humidity)
        sendIfChanged(name: "currentPressure", value: cur.pressure)
        sendIfChanged(name: "currentUVI", value: cur.uvi)
        sendIfChanged(name: "currentCloudPCT", value: cur.clouds)
        sendIfChanged(name: "currentVisibility", value: cur.visibility)
        
        // Handle optional current rain/snow nodes
		def rainVal = (cur.rain ?: [:])["1h"] ?: 0.0
		def snowVal = (cur.snow ?: [:])["1h"] ?: 0.0
        sendIfChanged(name: "currentRain", value: rainVal)
        sendIfChanged(name: "currentSnow", value: snowVal)
    }

    // 2. Parse Daily Forecast Array (day.0 = today, day.1 = tomorrow, day.2 = dayafter)
    if (json.daily && json.daily.size() >= 3) {
        logDebug "Parsing 3-day weather forecast arrays..."
        
        // Day 0: Today
        parseForecastDay(json.daily[0], "today")
        
        // Day 1: Tomorrow
        parseForecastDay(json.daily[1], "tom")
        
        // Day 2: Day After Tomorrow
        parseForecastDay(json.daily[2], "tda")
    } else {
        logWarn "Daily forecast payload incomplete or missing expected days."
    }
}

/**
 * Helper method to handle redundant day mapping logic cleanly
 * @param dayData Map object representing a single index in the OWM daily array
 * @param prefix String prefix corresponding to the target device attribute
 */

private void parseForecastDay(Map dayData, String prefix) {
    if (!dayData) return
    
    def tempMap = dayData.temp ?: [:]
    def feelsMap = dayData.feels_like ?: [:]
    
    // Core temperatures with 0.0 safe defaults
    sendIfChanged(name: "${prefix}TempMin", value: tempMap.min ?: 0.0)
    sendIfChanged(name: "${prefix}TempMax", value: tempMap.max ?: 0.0)
    sendIfChanged(name: "${prefix}TempNight", value: tempMap.night ?: 0.0)
    sendIfChanged(name: "${prefix}TempEve", value: tempMap.eve ?: 0.0)
    sendIfChanged(name: "${prefix}TempMorn", value: tempMap.morn ?: 0.0)
    sendIfChanged(name: "${prefix}TempDay", value: tempMap.day ?: 0.0)
    
    // Feels like temperatures
    sendIfChanged(name: "${prefix}FeelsLikeDay", value: feelsMap.day ?: 0.0)
    sendIfChanged(name: "${prefix}FeelsLikeNight", value: feelsMap.night ?: 0.0)
    sendIfChanged(name: "${prefix}FeelsLikeEve", value: feelsMap.eve ?: 0.0)
    sendIfChanged(name: "${prefix}FeelsLikeMorn", value: feelsMap.morn ?: 0.0)
    
    // Summary, POP and Moon attributes
    sendIfChanged(name: "${prefix}POP", value: dayData.pop ?: 0.0) 
    sendIfChanged(name: "${prefix}Moonrise", value: dayData.moonrise ?: 0)
    sendIfChanged(name: "${prefix}Moonset", value: dayData.moonset ?: 0)
    sendIfChanged(name: "${prefix}MoonPhase", value: dayData.moon_phase ?: 0.0)
    
    String summaryText = dayData.summary ?: "No summary provided"
    sendIfChanged(name: "${prefix}Summary", value: summaryText)
}

private BigDecimal calcSunPosition() {
    def lat = location.latitude
    def lon = location.longitude
    
    if (lat == null || lon == null) {
        logWarn "Latitude or Longitude is not configured in Hub settings. Skipping sun calculations."
        return 0.0
    }

    def date = new Date()
    def J2000 = 2451545.0
    def JulianDate = (date.getTime() / 86400000.0) + 2440587.5
    def d = JulianDate - J2000

    def rad = Math.PI / 180.0
    def e = rad * 23.4397
    
    def M = rad * (357.5291 + 0.98560028 * d)
    def C = rad * (1.9148 * Math.sin(M) + 0.0200 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M))
    def lambda = M + C + rad * 102.9372 + Math.PI
    
    def declination = Math.asin(Math.sin(lambda) * Math.sin(e))
    def rightAscension = Math.atan2(Math.sin(lambda) * Math.cos(e), Math.cos(lambda))
    
    def lw = rad * -lon
    def phi = rad * lat
    def H = rad * (280.16 + 360.9856235 * d) - lw - rightAscension
    
    def altitude = Math.asin(Math.sin(phi) * Math.sin(declination) + Math.cos(phi) * Math.cos(declination) * Math.cos(H))
    def azimuth = Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(declination) * Math.cos(phi))
    
    def azimuthDeg = azimuth * (180.0 / Math.PI) + 180.0
    def altitudeDeg = altitude * (180.0 / Math.PI)

	azimuthDeg = (Math.round(azimuthDeg * 100.0) / 100.0).toBigDecimal()
	altitudeDeg = (Math.round(altitudeDeg * 100.0) / 100.0).toBigDecimal()

    sendIfChanged(name: "azimuth", value: azimuthDeg, unit: "°")
    sendIfChanged(name: "altitude", value: altitudeDeg, unit: "°")
    return altitudeDeg
}

private void calcAlertsState(Map json, String calculatedCityAttr, String iconBasePath) {
    def alerts = json.alerts ?: []
    String alertActive = "No active alerts"
	String currentAlertSender = "N/A"
    String currentAlertDesc = "No active alerts"
    
    String lastPollTime = new Date().format("HH:mm", location.timeZone)
    String currentAlertDescFull = "No active alerts for ${calculatedCityAttr} at last poll as of ${lastPollTime}"
    
    String alertIconUrl = iconBasePath.endsWith("/") ? "${iconBasePath}OWM.png" : "${iconBasePath}/OWM.png"
    
    String currentAlertTile = "<div style='text-align:center;'>No active weather alerts from<br>Source: OpenWeatherMap</div>" + 
                       "<div style='text-align:center; margin-top:5px; font-size:0.8em;'>" + 
                       "<img src='${alertIconUrl}' style='max-width:25px; max-height:25px; vertical-align:middle; margin-right:5px;'/>" + 
                       "Updated ${lastPollTime}</div>"
                       
    if (alerts.size() > 0) {
        def a = alerts[0]
        alertActive = a.event ?: "Active Alert"
        currentAlertSender = a.sender_name ?: "Unknown"
        currentAlertDesc = a.description ? a.description.take(100) + "..." : "N/A"
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

private void calcBetwixtState(BigDecimal altitudeDeg) {
    if (settings.sliceOfDayEnable == false) {
        sendIfChanged(name: "betwixt", value: "Disabled in device preferences")
        return
    }

    String sliceText = "fully night time"
    long currentEpoch = (new Date().getTime() / 1000)
    long sunriseEpoch = state.todaySunriseEpoch ?: 0
    long sunsetEpoch = state.todaySunsetEpoch ?: 0
    
    boolean isTwilightAngle = (altitudeDeg >= -6.0 && altitudeDeg < -0.833)
    boolean isSunUp = (altitudeDeg >= -0.833)
    
    if (sunriseEpoch > 0 && sunsetEpoch > 0) { 
        long midDayEpoch = sunriseEpoch + ((sunsetEpoch - sunriseEpoch) / 2) 
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
    logDebug "Calculated betwixt slice: ${sliceText} (Current Alt: ${altitudeDeg}°)" 
}

private void calcIsDayState(BigDecimal altitudeDeg) {
    String isDayText = "false"
    long currentEpoch = (new Date().getTime() / 1000)
    long sunriseEpoch = state.todaySunriseEpoch ?: 0
    long sunsetEpoch = state.todaySunsetEpoch ?: 0
    
    boolean isSunUp = (altitudeDeg >= -0.833) 
    
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
    logDebug "Calculated isDay: ${isDayText}" 
}

def disableDebugLogging() {
    log.info "30 minutes elapsed: Automatically flipping 'Enable Debug Logging' switch off."
    device.updateSetting("logDebugEnable", [type: "bool", value: false])
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