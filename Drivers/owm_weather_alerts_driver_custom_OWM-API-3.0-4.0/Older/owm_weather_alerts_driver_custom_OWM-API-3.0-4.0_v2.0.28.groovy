/**
 * OpenWeatherMap Multi-Version Weather Driver (3.0 / 4.0)
 * Platform: Hubitat Elevation
 * Capabilities: Temperature, Illuminance, Relative Humidity, Pressure, Ultraviolet Index
 */

metadata {
    definition(name: "OpenWeatherMap Multi-Version Weather Driver", namespace: "jshimota", author: "James Shimota") {
        capability "Sensor"
        capability "Refresh"
        capability "Initialize"
        
        // Core Capabilities
        capability "TemperatureMeasurement"
        capability "IlluminanceMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "PressureMeasurement"
        capability "UltravioletIndex"

        command "pollOWM"

        // Custom Driver Attributes
        attribute "weatherDescription", "string"
        attribute "lastUpdated", "string"
        attribute "lastResponseCode", "string"
        
        // Standard Dashboard Integration Attributes
        attribute "city", "string"
        attribute "feelsLike", "number"
        attribute "forecastIcon", "string"
        attribute "localSunrise", "string"
        attribute "localSunset", "string"
        attribute "percentPrecip", "number"
        attribute "pressured", "string"
        attribute "weather", "string"
        attribute "weatherIcon", "string"
        attribute "weatherIcons", "string"
        attribute "wind", "number"
        attribute "windDirection", "number"
        attribute "windSpeed", "number"
        attribute "moonrise", "string"
        attribute "moonset", "string"
        attribute "moon_phase", "string"

        // Alert Sub-group
        attribute "alert", "string"
        attribute "alertTile", "string"
        attribute "alertDescr", "string"
        attribute "alertSender", "string"
        attribute "alertDescrFull", "string"
        
        // Extended Tiles
        attribute "threedayfcstTile", "string"
        attribute "betwixt", "string" 
		
        // High/Low Forecast Elements
        attribute "forecastHigh", "number"
        attribute "forecastHigh1", "number"
        attribute "forecastHigh2", "number"
        attribute "forecastLow", "number"
        attribute "forecastLow1", "number"
        attribute "forecastLow2", "number"
        attribute "forecastMorn", "number"
        attribute "forecastDay", "number"
        attribute "forecastEve", "number"
        attribute "forecastNight", "number"
        attribute "forecastMorn1", "number"
        attribute "forecastDay1", "number"
        attribute "forecastEve1", "number"
        attribute "forecastNight1", "number"
        attribute "forecast_text1", "string"
        attribute "forecast_text2", "string"
        attribute "condition_icon_url1", "string"
        attribute "condition_icon_url2", "string"

        // Solar / Ephemeris Coordinates
        attribute "tw_begin", "string"
        attribute "sunriseTime", "string"
        attribute "noonTime", "string"
        attribute "sunsetTime", "string"
        attribute "tw_end", "string"
		attribute "isDay", "enum", ["true","false"]

        // Solar Angles
        attribute "altitude", "number"
        attribute "azimuth", "number"

        // Polling Timestamps
        attribute "last_poll_Forecast", "string"
        attribute "last_observation_Forecast", "string"

        // Extended Precipitation Metrics
        attribute "rainTomorrow", "number"
        attribute "rainDayAfterTomorrow", "number"
        attribute "Precip0", "number"
        attribute "Precip1", "number"
        attribute "Precip2", "number"
        attribute "PoP1", "number"
        attribute "PoP2", "number"

        // Extended Cloud Coverage Metrics (1 Current + 3 Forecast Days)
        attribute "cloudPctCurrent", "number"
        attribute "cloudPctToday", "number"
        attribute "cloudPctTomorrow", "number"
        attribute "cloudPctDayAfterTomorrow", "number"

		// Custom mapping attributes from OWMCodes CSV
		attribute "conditionType", "string"
		attribute "conditionDescrFull", "string"
		attribute "conditionAltDayIcon", "string"
		attribute "conditionAltNightIcon", "string"
		attribute "conditionAltIconUrl", "string"
    }

    preferences {
        input name: "apiKey", type: "text", title: "API Key", description: "Enter your OpenWeatherMap API Key here", required: true
        input name: "apiSelection", type: "enum", title: "API Version", options: ["3.0": "One Call 3.0", "4.0": "One Call 4.0"], defaultValue: "3.0", required: true
        
        // Optional City field that dynamically overrides latitude/longitude if populated
        input name: "overrideCity", type: "text", title: "Base Override - City", description: "Optional - e.g., Newberg, OR or London, UK. Overrides Latitude/Longitude parameters configured below.", required: false
		input name: "altIconLoc", type: "text", title: "Base Override - Icon Location", description: "Optional - Icon Location:<br><i>blank for default location</i>", required: false
        input name: "latitude", type: "text", title: "Base Override - Latitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "longitude", type: "text", title: "Base Override - Longitude", description: "Optional - Leave blank to use Hub location", required: false
		
		        // Display Selector Options
        input name: "precisionPrecip", type: "enum", title: "Display Decimal Precision - Precipitation", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], defaultValue: "2", required: true
        input name: "precisionPressure", type: "enum", title: "Display Decimal Precision - Pressure", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], defaultValue: "2", required: true
        input name: "precisionTemp", type: "enum", title: "Display Decimal Precision - Temperature", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], defaultValue: "2", required: true
        input name: "precisionWind", type: "enum", title: "Display Decimal Precision - Wind Speed", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], defaultValue: "2", required: true
        
		// Display Options
		input name: "owmAlertsEnable", type: "bool", title: "Display Options - Enable Alerts Tile?", defaultValue: true, required: true
		input name: "sliceOfDayEnable", type: "bool", title: "Display Options - Enable Slice Of Day?", defaultValue: true, required: true
		
        // Display Unit Selectors
        input name: "useImperialTemp", type: "bool", title: "Display Unit - Imperial Temperature?", description: "Turn ON for Fahrenheit (°F), OFF for Celsius (°C)", defaultValue: true, required: true
        input name: "pressureUnitSetting", type: "enum", title: "Display Unit - Barometric Pressure", options: ["inHg": "Mercury (inHg)", "hPa": "Hectopascals (hPa)", "mb": "Millibar (mb)"], defaultValue: "inHg", required: true
        input name: "windUnit", type: "enum", title: "Display Unit - Wind Speed", options: ["mph": "Miles per Hour (mph)", "kmh": "Kilometers per Hour (km/h)", "kt": "Knots (kt)", "ms": "Meters per Second (m/s)"], defaultValue: "mph", required: true
		
        // Polling Option Dropdown Menu
        input name: "dayInterval", type: "enum", title: "Polling - Daytime Interval", options: ["manual": "Manual Only (via pollOWM command)", "15": "15 Minutes", "30": "30 Minutes", "60": "1 Hour", "180": "3 Hours"], defaultValue: "30", required: true
        input name: "nightInterval", type: "enum", title: "Polling - Nighttime Interval", options: ["manual": "Manual Only (via pollOWM command)", "15": "15 Minutes", "30": "30 Minutes", "60": "1 Hour", "180": "3 Hours"], defaultValue: "60", required: true

        // Independent Logging Switches
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: true, required: true
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        input name: "logWarnErrorEnable", type: "bool", title: "Logging - Enable Warning & Error Logging", defaultValue: true, required: true
    }
}

def installed() {
    logInfo "Driver Installed."
    initialize()
}

def updated() {
    logInfo "Preferences updated, re-initializing driver rules..."
    initialize()
}

def initialize() {
    unschedule()
    
    if (logDebugEnable == true) {
        log.info "Debug logging toggle is currently active. Auto-disable scheduled in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }

    // 1. Process Day Interval Loop Setup
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

    // 2. Process Night Interval Loop Setup
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

def pollOWM() {
    logInfo "Manual pollOWM command invoked by user or Rule Machine."
    refresh()
}

def disableDebugLogging() {
    log.info "30 minutes elapsed: Automatically flipping 'Enable Debug Logging' switch off."
    device.updateSetting("logDebugEnable", [type: "bool", value: "false"])
}

def refresh() {
    logDebug "Refresh task executed."
    
    if (!apiKey) {
        logWarn "Execution halted: API Key entry is missing!"
        return
    }

    // Context Evaluation Rules: Process City Override Lookup vs Fallback coordinates
    if (overrideCity && overrideCity.trim() != "") {
        String cleanCity = overrideCity.trim()
        
        if (state.cachedCity == cleanCity && state.resolvedLat != null && state.resolvedLon != null) {
            logDebug "Using cached forward geocoding coordinates for '${cleanCity}': Lat: ${state.resolvedLat}, Lon: ${state.resolvedLon}"
            requestWeatherData(state.resolvedLat, state.resolvedLon)
        } else {
            logInfo "New override location requested: Fetching forward lookup coordinates for '${cleanCity}'"
            String geoUrl = "https://api.openweathermap.org/geo/1.0/direct?q=${URLEncoder.encode(cleanCity, "UTF-8")}&limit=1&appid=${apiKey}"
            
            try {
                asynchttpGet("handleGeocodeResponse", [uri: geoUrl, timeout: 10], [cityQuery: cleanCity])
            } catch (Exception e) {
                logError "Failed to initiate async forward geocoding request: ${e.message}"
                fallbackWeatherFetch()
            }
        }
    } else {
        state.cachedCity = null
        state.resolvedLat = null
        state.resolvedLon = null
        fallbackWeatherFetch()
    }
}

def handleGeocodeResponse(response, data) {
    String queryCity = data?.cityQuery ?: "Unknown"
    
    if (response.hasError()) {
        logWarn "Geocoding lookup failed for '${queryCity}': ${response.errorMessage}. Falling back to default values."
        fallbackWeatherFetch()
        return
    }

    try {
        def geoData = response.getJson()
        if (geoData && geoData.size() > 0) {
            state.cachedCity = queryCity
            state.resolvedLat = geoData[0].lat
            state.resolvedLon = geoData[0].lon
            logInfo "Forward Geocoding Success! Coordinates for '${queryCity}' saved -> Lat: ${state.resolvedLat}, Lon: ${state.resolvedLon}"
            
            requestWeatherData(state.resolvedLat, state.resolvedLon)
        } else {
            logWarn "Geocoding API found 0 matching coordinate indices for entry: '${queryCity}'. Falling back to default values."
            fallbackWeatherFetch()
        }
    } catch (Exception e) {
        logError "An error occurred while parsing forward geocoding server response properties: ${e.message}"
        fallbackWeatherFetch()
    }
}

private void fallbackWeatherFetch() {
    def lat = latitude ?: location.latitude
    def lon = longitude ?: location.longitude
    
    if (!lat || !lon) {
        logWarn "Execution halted: Missing valid fallback Latitude/Longitude coordinates!"
        return
    }

    String cacheCoordsKey = "${lat},${lon}"
    if (state.lastReverseCoords != cacheCoordsKey) {
        logDebug "Reverse geocoding initiated to determine nearest city attributes for coordinates: [${cacheCoordsKey}]"
        String revGeoUrl = "https://api.openweathermap.org/geo/1.0/reverse?lat=${lat}&lon=${lon}&limit=1&appid=${apiKey}"
        
        try {
            asynchttpGet("handleReverseGeocodeResponse", [uri: revGeoUrl, timeout: 10], [coordsKey: cacheCoordsKey])
        } catch (Exception e) {
            logError "Failed to initialize reverse geocoding lookup: ${e.message}"
        }
    }

    requestWeatherData(lat, lon)
}

def handleReverseGeocodeResponse(response, data) {
    if (response.hasError()) {
        logDebug "Reverse geocoding lookup attempt failed: ${response.errorMessage}"
        return
    }
    
    try {
        def revData = response.getJson()
        if (revData && revData.size() > 0) {
            state.lastReverseCoords = data?.coordsKey
            state.nearestCityName = revData[0].name ? revData[0].name.toString() : null
            logDebug "Reverse Geocoding Success! Evaluated nearest city as: [${state.nearestCityName}]"
        }
    } catch (Exception e) {
        logDebug "Exception caught processing reverse geocode data array: ${e.message}"
    }
}

private void requestWeatherData(def lat, def lon) {
    String owmUnits = "metric"
    String url = ""
    
    if (apiSelection == "3.0") {
        url = "https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${lon}&units=${owmUnits}&exclude=minutely,hourly&appid=${apiKey}"
    } else if (apiSelection == "4.0") {
        url = "https://api.openweathermap.org/data/4.0/onecall/current?lat=${lat}&lon=${lon}&units=${owmUnits}&exclude=minutely,hourly&appid=${apiKey}"
    }
    
    logTrace "Target URL endpoint assembled: ${url}"

    def params = [
        uri: url,
        contentType: "application/json",
        timeout: 10
    ]

    sendIfChanged(name: "last_poll_Forecast", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))

    try {
        asynchttpGet("handleWeatherResponse", params)
    } catch (Exception e) {
        logError "Failed to invoke async HTTP weather data payload request: ${e.message}"
    }
}

def handleWeatherResponse(response, data) {
    String statusCode = response.status ? response.status.toString() : "Unknown"
    sendIfChanged(name: "lastResponseCode", value: statusCode, descriptionText: "HTTP response code from OWM: ${statusCode}")
    
    if (response.hasError()) {
        logError "API target endpoint rejected payload parsing state: ${response.errorMessage} (Status code: ${statusCode})"
        return
    }

    try {
        def json = response.getJson()
        parsePayload(json)
    } catch (Exception e) {
        logError "An error occurred during payload processing operations: ${e.message}"
    }
}

private def parsePayload(Map json) {
    logDebug "Beginning global weather payload parsing routine..."
    
	int pTemp = settings.precisionTemp != null ? settings.precisionTemp.toInteger() : 2
	int pPressure = settings.precisionPressure != null ? settings.precisionPressure.toInteger() : 2
	int pWind = settings.precisionWind != null ? settings.precisionWind.toInteger() : 2
	int pPrecip = settings.precisionPrecip != null ? settings.precisionPrecip.toInteger() : 2

	// Helper closure: Converts OWM's forced 'metric' Celsius baseline to Fahrenheit locally if configured
    def convertT = { val ->
        if (val == null) return null
        BigDecimal v = val.toString().toBigDecimal()
        if (settings.useImperialTemp == true) { // Added settings. scope
            v = (v * 1.8) + 32.0
        }
        return v.setScale(pTemp, java.math.RoundingMode.HALF_UP)
    }

    def obTime = json.current?.dt ?: (json.data ? json.data[0]?.dt : null)
    if (obTime) {
        def obDate = new java.util.Date((long)obTime * 1000)
       sendIfChanged(name: "last_observation_Forecast", value: obDate.format("yyyy-MM-dd HH:mm:ss", location.timeZone))
    }

    def current = [:]
    if (apiSelection == "3.0") {
        current = json.current
    } else if (apiSelection == "4.0") {
        current = json.data ? json.data[0] : json
    }

    if (!current) {
        logWarn "No current conditions block found in the response payload structural tree."
        return
    }

    def tempValue = convertT(current.temp)
    def feelsLikeValue = convertT(current.feels_like)
    def rawHumidity = current.humidity != null ? current.humidity.toString().toBigDecimal() : null
    def pressureRaw = current.pressure != null ? current.pressure.toString().toBigDecimal() : null
    def rawWindSpeed = current.wind_speed != null ? current.wind_speed.toString().toBigDecimal() : null
    
    def uvi = current.uvi
    def mainWeather = current.weather ? current.weather[0]?.main : "N/A"
    def description = current.weather ? current.weather[0]?.description : "N/A"
	
	// undate conditionID against code table
	def conditionId = current.weather && current.weather[0]?.id != null ? current.weather[0].id.toInteger() : 0
	def iconCode = current.weather ? current.weather[0]?.icon : "" // contains "d" or "n" suffix
    //def iconCode = current.weather ? current.weather[0]?.icon : ""
	//def conditionId = current.weather && current.weather[0]?.id != null ? current.weather[0].id.toInteger() : 0
	
	// Call the custom CSV condition mapping block
	Map parsedCond = lookupConditionDetails(conditionId)
	Boolean isDaytimeIcon = iconCode.endsWith("d")
	String chosenAltIcon = isDaytimeIcon ? parsedCond.altDay : parsedCond.altNight

// Construct Alternative Full URL using settings.altIconLoc if defined
	String altIconUrl = ""
	if (chosenAltIcon) {
		String iconBasePath = (settings.altIconLoc && settings.altIconLoc.trim() != "") ? settings.altIconLoc.trim() : "https://openweathermap.org/img/wn/"
		altIconUrl = iconBasePath.endsWith("/") ? "${iconBasePath}${chosenAltIcon}" : "${iconBasePath}/${chosenAltIcon}"
	}

	// Updated to append the .png extension directly to the icon name string
    def iconNameValue = iconCode ? "${iconCode}.png" : ""
    
	// Determine base URL path for the current condition tilede
    String iconBasePath = (settings.altIconLoc && settings.altIconLoc.trim() != "") ? settings.altIconLoc.trim() : "https://openweathermap.org/img/wn/"
    
    // Build the fully qualified image source URL path
    String currentIconUrl = iconBasePath.endsWith("/") ? "${iconBasePath}${iconCode}.png" : "${iconBasePath}/${iconCode}.png"
    
    // Wraps the icon in an HTML img tag to force rendering as a dashboard image tile
    def formattedConditionIcon = iconCode ? "<img src='${currentIconUrl}' style='max-width:50px; max-height:50px;'/>" : "N/A"
    def clouds = current.clouds ?: 0
    
    def windSpd = 0
    if (rawWindSpeed != null) {
        String targetWindUnit = windUnit ?: "mph"
        
        if (targetWindUnit == "mph")  windSpd = rawWindSpeed * 2.236936
        else if (targetWindUnit == "kmh")  windSpd = rawWindSpeed * 3.6
        else if (targetWindUnit == "kt")   windSpd = rawWindSpeed * 1.943844
        else windSpd = rawWindSpeed
        
        windSpd = windSpd.toBigDecimal().setScale(pWind, java.math.RoundingMode.HALF_UP)
    }
    
    def windDir = current.wind_deg != null ? current.wind_deg.toString().toBigDecimal().setScale(0, java.math.RoundingMode.HALF_UP) : 0
    def humidityValue = rawHumidity != null ? rawHumidity.setScale(0, java.math.RoundingMode.HALF_UP) : null

    def pressureValue = null
    String targetPressureUnit = pressureUnitSetting ?: "inHg"
    String pressureUnitLabel = "hPa"
    
    if (pressureRaw != null) {
        if (targetPressureUnit == "inHg") {
            pressureValue = (pressureRaw * 0.02953).setScale(pPressure, java.math.RoundingMode.HALF_UP)
            pressureUnitLabel = "inHg"
        } else if (targetPressureUnit == "mb") {
            pressureValue = pressureRaw.setScale(pPressure, java.math.RoundingMode.HALF_UP)
            pressureUnitLabel = "mb"
        } else {
            pressureValue = pressureRaw.setScale(pPressure, java.math.RoundingMode.HALF_UP)
            pressureUnitLabel = "hPa"
        }
    }

    def estimatedLux = 50
    if (uvi != null) {
        estimatedLux = (uvi * 10000) * (1 - (clouds / 100))
        estimatedLux = Math.max(estimatedLux.toInteger(), 5)
    }

    String srTime = formatTime(current.sunrise)
    String ssTime = formatTime(current.sunset)

    def daily = json.daily ?: []
    def fHigh = null; def fHigh1 = null; def fHigh2 = null
    def fLow = null; def fLow1 = null; def fLow2 = null
    def fMorn = null; def fDay = null; def fEve = null; def fNight = null
    def fMorn1 = null; def fDay1 = null; def fEve1 = null; def fNight1 = null
    def pop1 = 0; def pop2 = 0
    def precip0 = 0; def precip1 = 0; def precip2 = 0
    def rainTom = 0; def rainDat = 0
    def cloudTom = 0; def cloudDat = 0
    String moonPhase = "N/A"
    def moonRiseStr = "N/A"; def moonSetStr = "N/A"
    String fText1 = "N/A"; def fText2 = "N/A"
    String iconUrl1 = ""; def iconUrl2 = ""

    if (daily.size() > 0) {
        def d0 = daily[0]
        fHigh = convertT(d0.temp?.max)
        fLow  = convertT(d0.temp?.min)
        fMorn = convertT(d0.temp?.morn)
        fDay  = convertT(d0.temp?.day)
        fEve  = convertT(d0.temp?.eve)
        fNight = convertT(d0.temp?.night)
        precip0 = (d0.rain ?: d0.snow ?: 0).toString().toBigDecimal().setScale(pPrecip, java.math.RoundingMode.HALF_UP)
        moonPhase = d0.moon_phase != null ? d0.moon_phase.toString() : "N/A"
        moonRiseStr = formatTime(d0.moonrise)
        moonSetStr = formatTime(d0.moonset)

        if (daily.size() > 1) {
            def d1 = daily[1]
            fHigh1 = convertT(d1.temp?.max)
            fLow1  = convertT(d1.temp?.min)
            fMorn1 = convertT(d1.temp?.morn)
            fDay1  = convertT(d1.temp?.day)
            fEve1  = convertT(d1.temp?.eve)
            fNight1 = convertT(d1.temp?.night)
            pop1 = (d1.pop != null) ? (d1.pop.toString().toBigDecimal() * 100).setScale(0, java.math.RoundingMode.HALF_UP) : 0
            precip1 = (d1.rain ?: d1.snow ?: 0).toString().toBigDecimal().setScale(pPrecip, java.math.RoundingMode.HALF_UP)
            rainTom = (d1.rain ?: 0).toString().toBigDecimal().setScale(pPrecip, java.math.RoundingMode.HALF_UP)
            cloudTom = d1.clouds ?: 0
            fText1 = d1.weather ? d1.weather[0]?.description : "N/A"
			if (d1.weather ? d1.weather[0]?.icon : null) { 
                String iCode1 = d1.weather[0].icon
                iconUrl1 = iconBasePath.endsWith("/") ? "${iconBasePath}${iCode1}.png" : "${iconBasePath}/${iCode1}.png"
            }
        }

        if (daily.size() > 2) {
            def d2 = daily[2]
            fHigh2 = convertT(d2.temp?.max)
            fLow2  = convertT(d2.temp?.min)
            pop2 = (d2.pop != null) ? (d2.pop.toString().toBigDecimal() * 100).setScale(0, java.math.RoundingMode.HALF_UP) : 0
            precip2 = (d2.rain ?: d2.snow ?: 0).toString().toBigDecimal().setScale(pPrecip, java.math.RoundingMode.HALF_UP)
            rainDat = (d2.rain ?: 0).toString().toBigDecimal().setScale(pPrecip, java.math.RoundingMode.HALF_UP)
            cloudDat = d2.clouds ?: 0
            fText2 = d2.weather ? d2.weather[0]?.description : "N/A"
			if (d2.weather ? d2.weather[0]?.icon : null) { 
                String iCode2 = d2.weather[0].icon
                iconUrl2 = iconBasePath.endsWith("/") ? "${iconBasePath}${iCode2}.png" : "${iconBasePath}/${iCode2}.png"
            }
        }
    }

    logInfo "Weather Data processing completed. Dispatched to active Device states layout."
    String calculatedCityAttr = "Local"
    if (overrideCity && overrideCity.trim() != "") {
        calculatedCityAttr = overrideCity.trim()
    } else if (state.nearestCityName != null) {
        calculatedCityAttr = state.nearestCityName
    } else if (json.timezone) {
        calculatedCityAttr = json.timezone.tokenize('/')[-1].replace('_', ' ')
    }

    String curTempStr = tempValue != null ? tempValue.toString() : "N/A"
    String tHigh1Str = fHigh1 != null ? fHigh1.toString() : "N/A"
    String tHigh2Str = fHigh2 != null ? fHigh2.toString() : "N/A"

    String tileHtml = "<table style='width:100%; text-align:center; font-size:0.9em;'>" +
                      "<tr><td>Today</td><td>Tom</td><td>D_After</td></tr>" +
                      "<tr><td>${curTempStr}°</td><td>${tHigh1Str}°</td><td>${tHigh2Str}°</td></tr>" +
                      "</table>"

    if (tempValue != null) sendIfChanged(name: "temperature", value: tempValue, unit: (settings.useImperialTemp ? "°F" : "°C"))
    if (humidityValue != null) sendIfChanged(name: "humidity", value: humidityValue, unit: "%")
    if (pressureValue != null) sendIfChanged(name: "pressure", value: pressureValue, unit: pressureUnitLabel)
    sendIfChanged(name: "ultravioletIndex", value: uvi ?: 0)
    sendIfChanged(name: "illuminance", value: estimatedLux, unit: "lux")

    sendIfChanged(name: "weatherDescription", value: description)
    sendIfChanged(name: "weather", value: mainWeather)
    sendIfChanged(name: "feelsLike", value: feelsLikeValue ?: tempValue)
    sendIfChanged(name: "city", value: calculatedCityAttr)
    
    // Condition mapping attributes (Refactored to condition_icon_name)
	sendIfChanged(name: "condition_code", value: conditionId)
	sendIfChanged(name: "conditionType", value: parsedCond.type)
	sendIfChanged(name: "conditionDescrFull", value: parsedCond.desc)
    sendIfChanged(name: "condition_icon_url1", value: iconUrl1)
    sendIfChanged(name: "condition_icon_url2", value: iconUrl2)

	// New Condtion mapping additions from CSV mapping:
	sendIfChanged(name: "conditionAltDayIcon", value: parsedCond.altDay)
	sendIfChanged(name: "conditionAltNightIcon", value: parsedCond.altNight)
	sendIfChanged(name: "conditionAltIconUrl", value: altIconUrl)
	sendIfChanged(name: "condition_code", value: conditionId)
	sendIfChanged(name: "condition_icon", value: formattedConditionIcon)
	sendIfChanged(name: "condition_icon_name", value: iconNameValue)
    
    sendIfChanged(name: "weatherIcon", value: iconCode)
    sendIfChanged(name: "weatherIcons", value: iconCode)
    sendIfChanged(name: "forecastIcon", value: iconCode)

    sendIfChanged(name: "wind", value: windSpd)
    sendIfChanged(name: "windSpeed", value: windSpd)
    sendIfChanged(name: "windDirection", value: windDir)

    sendIfChanged(name: "localSunrise", value: srTime)
    sendIfChanged(name: "sunriseTime", value: srTime)
    sendIfChanged(name: "localSunset", value: ssTime)
    sendIfChanged(name: "sunsetTime", value: ssTime)
    sendIfChanged(name: "noonTime", value: "12:00")
    sendIfChanged(name: "tw_begin", value: srTime)
    sendIfChanged(name: "tw_end", value: ssTime)
    
    sendIfChanged(name: "moonrise", value: moonRiseStr)
    sendIfChanged(name: "moonset", value: moonSetStr)
    sendIfChanged(name: "moon_phase", value: moonPhase)

    sendIfChanged(name: "percentPrecip", value: pop1)
    sendIfChanged(name: "Precip0", value: precip0)
    sendIfChanged(name: "Precip1", value: precip1)
    sendIfChanged(name: "Precip2", value: precip2)
    sendIfChanged(name: "PoP1", value: pop1)
    sendIfChanged(name: "PoP2", value: pop2)
    sendIfChanged(name: "rainTomorrow", value: rainTom)
    sendIfChanged(name: "rainDayAfterTomorrow", value: rainDat)
    
    // --- Cloud Values mapped correctly (1 Current + 3 Daily Forecasts) ---
    sendIfChanged(name: "cloudPctCurrent", value: clouds)
    sendIfChanged(name: "cloudPctToday", value: (daily.size() > 0 ? daily[0].clouds ?: 0 : 0))
    sendIfChanged(name: "cloudPctTomorrow", value: cloudTom)
    sendIfChanged(name: "cloudPctDayAfterTomorrow", value: cloudDat)

    sendIfChanged(name: "threedayfcstTile", value: tileHtml.toString())

    if (fHigh != null) sendIfChanged(name: "forecastHigh", value: fHigh)
    if (fHigh1 != null) sendIfChanged(name: "forecastHigh1", value: fHigh1)
    if (fHigh2 != null) sendIfChanged(name: "forecastHigh2", value: fHigh2)
    if (fLow != null) sendIfChanged(name: "forecastLow", value: fLow)
    if (fLow1 != null) sendIfChanged(name: "forecastLow1", value: fLow1)
    if (fLow2 != null) sendIfChanged(name: "forecastLow2", value: fLow2)
    
    if (fMorn != null) sendIfChanged(name: "forecastMorn", value: fMorn)
    if (fDay != null) sendIfChanged(name: "forecastDay", value: fDay)
    if (fEve != null) sendIfChanged(name: "forecastEve", value: fEve)
    if (fNight != null) sendIfChanged(name: "forecastNight", value: fNight)
    if (fMorn1 != null) sendIfChanged(name: "forecastMorn1", value: fMorn1)
    if (fDay1 != null) sendIfChanged(name: "forecastDay1", value: fDay1)
    if (fEve1 != null) sendIfChanged(name: "forecastEve1", value: fEve1)
    if (fNight1 != null) sendIfChanged(name: "forecastNight1", value: fNight1)
    
    sendIfChanged(name: "forecast_text1", value: fText1)
    sendIfChanged(name: "forecast_text2", value: fText2)
    sendIfChanged(name: "pressured", value: "${pressureValue} ${pressureUnitLabel}".toString())
    sendIfChanged(name: "lastUpdated", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))
    
    // Cache astronomical ephemeris locally inside the parsing instance to feed the custom slicing sequence
    if (current.sunrise && current.sunset) {
        state.todaySunriseEpoch = (long)current.sunrise
        state.todaySunsetEpoch = (long)current.sunset
    }
    
    // --- Auto-trigger calculations on execution schedule ---
	BigDecimal altitude = calcSunPosition()
	calcAlertsState(json, calculatedCityAttr, iconBasePath)
	calcBetwixtState(altitude)
}

private String formatTime(epoch) {
    if (!epoch) return "N/A"
    try {
        return new java.util.Date((long)epoch * 1000).format("HH:mm", location.timeZone)
    } catch (Exception e) {
        return "N/A"
    }
}

private BigDecimal calcSunPosition() {
    def lat = location.latitude
    def lon = location.longitude
    
    if (lat == null || lon == null) {
        logWarn "Latitude or Longitude is not configured in Hub settings. Skipping sun calculations."
        return
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
    
    azimuthDeg = Math.round(azimuthDeg * 100.0) / 100.0
    altitudeDeg = Math.round(altitudeDeg * 100.0) / 100.0

    sendIfChanged(name: "azimuth", value: azimuthDeg, unit: "°")
    sendIfChanged(name: "altitude", value: altitudeDeg, unit: "°")
    return altitudeDeg
}

private void calcAlertsState(Map json, String calculatedCityAttr, String iconBasePath) {
    def alerts = json.alerts ?: []
    String alertActive = "No active alerts"
    String alertSender = "N/A"
    String alertDescr = "No active alerts"
    String alertDescrFull = "No active alerts for ${calculatedCityAttr} at last poll"
    
    // Assembles the path for the default OpenWeatherMap alert/branding icon
    String alertIconUrl = iconBasePath.endsWith("/") ? "${iconBasePath}OWM.png" : "${iconBasePath}/OWM.png"
    
    // Format the current time for the "as of" timestamp
    String lastPollTime = new Date().format("HH:mm", location.timeZone)
    
    // Fallback block: Formatted with the icon, "as of", and the last poll time on the last line
    String alertTile = "<div style='text-align:center;'>No Active Weather Alerts From</br>OpenWeatherMap.Org</div>" + 
                       "<div style='text-align:center; margin-top:5px; font-size:0.8em;'>" + 
                       "<img src='${alertIconUrl}' style='max-width:25px; max-height:25px; vertical-align:middle; margin-right:5px;'/>" + 
                       "as of ${lastPollTime}</div>"
                       
    if (alerts.size() > 0) {
        def a = alerts[0]
        alertActive = a.event ?: "Active Alert"
        alertSender = a.sender_name ?: "N/A"
        alertDescr = a.description ? a.description.take(100) + "..." : "N/A"
        alertDescrFull = a.description ?: "N/A"
        
        // Active alert block: Appends the matching icon and timestamp format to the bottom wrapper row
        alertTile = "<div style='color:red; font-weight:bold; text-align:center;'>${alertActive}</div>" + 
                    "<div style='font-size:0.8em;'>${alertDescr}</div>" + 
                    "<div style='text-align:center; margin-top:5px; font-size:0.8em;'>" + 
                    "<img src='${alertIconUrl}' style='max-width:25px; max-height:25px; vertical-align:middle; margin-right:5px;'/>" + 
                    "as of ${lastPollTime}</div>"
    }

    // Dispatch alert events if the settings permit (or unconditionally as originally written)
    sendIfChanged(name: "alert", value: alertActive)
    sendIfChanged(name: "alertSender", value: alertSender)
    sendIfChanged(name: "alertDescr", value: alertDescr)
    sendIfChanged(name: "alertDescrFull", value: alertDescrFull)
    sendIfChanged(name: "alertTile", value: alertTile)
}

private void calcBetwixtState(BigDecimal altitudeDeg) {
    String sliceText = "fully night time"
    
    // Check if Slice Of Day is enabled in device preferences
    if (settings.sliceOfDayEnable == false) {
        sliceText = "Disabled in device preferences"
        sendIfChanged(name: "betwixt", value: sliceText)
        logDebug "Calculated betwixt slice: ${sliceText}"
        return
    }

    long currentEpoch = (new Date().getTime() / 1000)
    long sunriseEpoch = state.todaySunriseEpoch ?: 0
    long sunsetEpoch = state.todaySunsetEpoch ?: 0
    
    // Civil Twilight baseline is typically between -6.0° and -0.833° (atmospheric refraction accounted)
    boolean isTwilightAngle = (altitudeDeg >= -6.0 && altitudeDeg < -0.833)
    boolean isSunUp = (altitudeDeg >= -0.833)
    
    // Determine context based on whether the current time falls in the morning or evening half of the day
    if (sunriseEpoch > 0 && sunsetEpoch > 0) {
        long midDayEpoch = sunriseEpoch + ((sunsetEpoch - sunriseEpoch) / 2)
        
        if (currentEpoch < midDayEpoch) {
            // Morning progression
            if (isTwilightAngle) {
                sliceText = "between twilight and sunrise"
            } else if (isSunUp) {
                sliceText = "between sunrise and noon"
            }
        } else {
            // Afternoon/Evening progression
            if (isSunUp) {
                sliceText = "between noon and sunset"
            } else if (isTwilightAngle) {
                sliceText = "between sunset and twilight"
            }
        }
    } else {
        // Fallback checks entirely matching altitude vectors if epochs are structurally absent
        if (isTwilightAngle) {
            sliceText = "between twilight and sunrise" // Default early assignment context
        } else if (isSunUp) {
            sliceText = "between sunrise and noon"
        }
    }
    
    sendIfChanged(name: "betwixt", value: sliceText)
    logDebug "Calculated betwixt slice: ${sliceText} (Current Alt: ${altitudeDeg}°)"
}

private void sendIfChanged(Map args) {
    if (!args || !args.name) return

    String oldVal = device.currentValue(args.name as String)?.toString()
    String newVal = args.value?.toString()

    if (oldVal != newVal) {
        sendEvent(args)
    }
}

private Map lookupConditionDetails(Integer code) {
    def map = [:]
    switch(code) {
        // Group 2xx: Thunderstorm
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

        // Group 3xx: Drizzle
        case 300: map = [type: "Drizzle", desc: "Light Intensity Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 301: map = [type: "Drizzle", desc: "Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 302: map = [type: "Drizzle", desc: "Heavy Intensity Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 310: map = [type: "Drizzle", desc: "Light Intensity Drizzle Rain", altDay: "9.png", altNight: "9.png"]; break
        case 311: map = [type: "Drizzle", desc: "Drizzle Rain", altDay: "9.png", altNight: "9.png"]; break
        case 312: map = [type: "Drizzle", desc: "Heavy Intensity Drizzle Rain", altDay: "9.png", altNight: "9.png"]; break
        case 313: map = [type: "Drizzle", desc: "Shower Rain and Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 314: map = [type: "Drizzle", desc: "Heavy Shower Rain and Drizzle", altDay: "9.png", altNight: "9.png"]; break
        case 321: map = [type: "Drizzle", desc: "Shower Drizzle", altDay: "9.png", altNight: "9.png"]; break

        // Group 5xx: Rain
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

        // Group 6xx: Snow
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

        // Group 7xx: Atmosphere
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

        // Group 800: Clear
        case 800: map = [type: "Clear", desc: "Clear Sky", altDay: "32.png", altNight: "31.png"]; break

        // Group 80x: Clouds
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
    if (logWarnErrorEnable) log.warn "OpenWeatherMap Driver WARNING: ${msg}"
}

private void logError(String msg) {
    if (logWarnErrorEnable) log.error "OpenWeatherMap Driver ERROR: ${msg}"
}