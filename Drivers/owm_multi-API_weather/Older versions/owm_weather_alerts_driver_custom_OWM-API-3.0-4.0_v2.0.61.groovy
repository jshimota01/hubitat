/**
 * OpenWeatherMap Multi-Version Weather Driver (2.5 / 3.0 / 4.0)
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
        attribute "percentPrecip", "number"
        attribute "weather", "string"
        attribute "weatherIcon", "string"
        attribute "weatherIcons", "string"
        attribute "wind", "number"
        attribute "windDirection", "number"
        attribute "windSpeed", "number"
        attribute "moonRise", "string"
        attribute "moonSet", "string"
        attribute "moonPhase", "string"

        // Alert Sub-group
        attribute "currentAlert", "string"
        attribute "alertTile", "string"
        attribute "currentAlertDesc", "string"
        attribute "currentAlertSender", "string"
        attribute "currentAlertDescFull", "string"
        
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
        attribute "forecastTomIconUrl", "string"
        attribute "forecastDATIconUrl", "string"
		attribute "forecastTodayAltDayIcon", "string"
		attribute "forecastTodayAltNightIcon", "string"
		attribute "forecastTodayAltIconUrl", "string"

        // Solar / Ephemeris Coordinates
        attribute "twilightBegin", "string"
        attribute "sunriseTime", "string"
        attribute "noonTime", "string"
        attribute "sunsetTime", "string"
        attribute "twilightEnd", "string"
		attribute "isDay", "enum", ["true","false"]

        // Solar Angles
        attribute "altitude", "number"
        attribute "azimuth", "number"

        // Polling Timestamps
        attribute "lastPollForecast", "string"
        attribute "lastObservationForecast", "string"

        // Extended Precipitation Metrics
        attribute "rainTomorrow", "number"
        attribute "rainDAT", "number"
        attribute "Precip0", "number"
        attribute "Precip1", "number"
        attribute "Precip2", "number"
        attribute "PoP1", "number"
        attribute "PoP2", "number"

        // Extended Cloud Coverage Metrics (1 Current + 3 Forecast Days)
		attribute "cloudPctCur", "number"
        attribute "cloudPctToday", "number"
        attribute "cloudPctTom", "number"
        attribute "cloudPctDAT", "number"

		// current condition variables and definitions
		attribute "currentConditionCode", "number"
		attribute "currentConditionType", "string"
		attribute "currentConditionTypeFull", "string"
		attribute "currentConditionIcon", "string"
		attribute "currentConditionAltIcon", "string"
		attribute "currentConditionIconImg", "string"
		attribute "currentIlluminance", "number"
        attribute "currentIlluminanceText", "string"
		attribute "currentPressureText", "string"

    }

    preferences {
        input name: "apiKey", type: "text", title: "API Key", description: "Enter your OpenWeatherMap API Key here<br><b>Required by OpenWeatherMaps</b>", required: true
        input name: "apiSelection", type: "enum", title: "API Version", options: ["2.5": "One Call 2.5 (Obsolete!)","3.0": "One Call 3.0", "4.0": "One Call 4.0"], defaultValue: "3.0", description: "Select your API Key version here<br><b>Required by OpenWeatherMaps</b><br><i>*Note: 2.5 API is now obsolete as of June 2024</i><br><i>*Note: 4.0 API key uses 3.0 API poll method</i>", required: true
        
        // Optional City field that dynamically overrides latitude/longitude if populated
        input name: "overrideCity", type: "text", title: "Base Override - City", description: "Optional - Will attempt to geo lookup and override <b>ALL</b> latitude/longitude values<br><b>Default:(empty)</b><br><i>EG: Portland, OR or London, UK.<br>*Note: Overrides Latitude/Longitude parameters of Hub <b>AND</b> values configured below</i>", required: false
		input name: "altIconLoc", type: "text", title: "Base Override - Icon Location", description: "Optional - Icon Source Location:<br><i>blank for default OWM location</i>", required: false
        input name: "latitude", type: "text", title: "Base Override - Latitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "longitude", type: "text", title: "Base Override - Longitude", description: "Optional - Leave blank to use Hub location", required: false
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
    // Programmatically enable the preference switch on installation
    device.updateSetting("logDebugEnable", [type: "bool", value: true])
    initialize()
}

def updated() {
    logInfo "Preferences updated, re-initializing driver rules..."
    
    // Safety check: force altIconsEnable to false if altIconLoc is default/blank
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
    
    // Check if debug logging is already enabled. If so, schedule it to turn off in 30 minutes (1800 seconds)
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
    device.updateSetting("logDebugEnable", [type: "bool", value: false])
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
			String geoUrl = "https://api.openweathermap.org/geo/1.0/direct?q=${URLEncoder.encode(cleanCity, "UTF-8").toString()}&limit=1&appid=${apiKey}"
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
    // OpenWeatherMap defaults to Kelvin when the units parameter is omitted
    String url = ""
    if (apiSelection == "2.5") {
        url = "https://api.openweathermap.org/data/2.5/onecall?lat=${lat}&lon=${lon}&exclude=minutely,hourly&appid=${apiKey}"
    } else if (apiSelection == "3.0" || apiSelection == "4.0") {
        url = "https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${lon}&exclude=minutely,hourly&appid=${apiKey}"
    }
    
    logTrace "Target URL endpoint assembled: ${url}"

    def params = [
        uri: url,
        contentType: "application/json",
        timeout: 10
    ]

    sendIfChanged(name: "lastPollForecast", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))

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
	String tempUnitLabel = "°F"
	if (settings.temperatureUnit == "c") {
		tempUnitLabel = "°C"
	} else if (settings.temperatureUnit == "k") {
		tempUnitLabel = "K"
	} else if (settings.temperatureUnit == "none") {
		tempUnitLabel = ""
	}
    // Helper closure: Converts OWM's default Kelvin baseline to Fahrenheit, Celsius, or Kelvin display formats
    def convertT = { val ->
        if (val == null) return null
        BigDecimal v = val.toString().toBigDecimal()
        String tUnit = settings.temperatureUnit ?: "f"
    
        if (tUnit == "f") { 
            // Kelvin to Fahrenheit: (K − 273.15) * 1.8 + 32
            v = ((v - 273.15) * 1.8) + 32.0
        } else if (tUnit == "c" || tUnit == "none") {
            // Kelvin to Celsius: K − 273.15
            v = v - 273.15
        } else if (tUnit == "k") {
            // Already Kelvin, do nothing to the base value
        }
        
        return v.setScale(pTemp, java.math.RoundingMode.HALF_UP)
    }

	def current = json.current ?: [:]

    def tempValue = convertT(current.temp)
    def feelsLikeValue = convertT(current.feels_like)
    def rawHumidity = current.humidity != null ? current.humidity.toString().toBigDecimal() : null
    def pressureRaw = current.pressure != null ? current.pressure.toString().toBigDecimal() : null
    def rawWindSpeed = current.wind_speed != null ? current.wind_speed.toString().toBigDecimal() : null
    
    def uvi = current.uvi
    def mainWeather = current.weather ? current.weather[0]?.main : "N/A"
    def description = current.weather ? current.weather[0]?.description : "N/A"

	def conditionId = (current.weather && current.weather.size() > 0 && current.weather[0]?.id != null) ? current.weather[0].id.toInteger() : 0
    String iconCode = current.weather?.getAt(0)?.icon ?: "" // contains "d" or "n" suffix
	
    // Call the custom CSV condition mapping block
    Map parsedCond = lookupConditionDetails(conditionId)
    Boolean isDaytimeIcon = iconCode?.endsWith("d") ?: false
    String chosenAltIcon = isDaytimeIcon ? parsedCond.altDay : parsedCond.altNight

    // Set Base Icon Source Location
    String iconBasePath = (settings.altIconLoc && settings.altIconLoc.trim() != "") ? settings.altIconLoc.trim() : "https://openweathermap.org/img/wn/"

    // Determine target name based on the boolean switch rule
    String activeIconFilename = ""
    String currentIconUrl = ""
    
    if (settings.altIconsEnable == true && chosenAltIcon) {
        activeIconFilename = chosenAltIcon
        currentIconUrl = iconBasePath.endsWith("/") ? "${iconBasePath}${chosenAltIcon}" : "${iconBasePath}/${chosenAltIcon}"
    } else {
        activeIconFilename = iconCode ? "${iconCode}.png" : ""
        currentIconUrl = iconBasePath.endsWith("/") ? "${iconBasePath}${iconCode}.png" : "${iconBasePath}/${iconCode}.png"
    }

    // Construct Alternative Full URL using settings.altIconLoc/chosenAltIcon for raw exposure
    String altIconUrl = ""
    if (chosenAltIcon) {
        altIconUrl = iconBasePath.endsWith("/") ? "${iconBasePath}${chosenAltIcon}" : "${iconBasePath}/${chosenAltIcon}"
    }

    def iconNameValue = activeIconFilename
    def formattedConditionIcon = activeIconFilename ? "<img src='${currentIconUrl}' style='max-width:50px; max-height:50px;'/>" : "N/A"
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
    String targetPressureUnit = pressureUnit ?: "inHg"
    String pressureUnitLabel = "hPa"
    
    if (pressureRaw != null) {
        if (targetPressureUnit == "inHg") {
            pressureValue = (pressureRaw * 0.02953).setScale(pPressure, java.math.RoundingMode.HALF_UP)
            pressureUnitLabel = "inHg"
        } else if (targetPressureUnit == "mb") {
            pressureValue = pressureRaw.setScale(pPressure, java.math.RoundingMode.HALF_UP)
            pressureUnitLabel = "mb"
        } else if (targetPressureUnit == "none") {
            pressureValue = pressureRaw.setScale(pPressure, java.math.RoundingMode.HALF_UP)
            pressureUnitLabel = "" // Drops the label suffix entirely
        } else {
            pressureValue = pressureRaw.setScale(pPressure, java.math.RoundingMode.HALF_UP)
            pressureUnitLabel = "hPa"
        }
    }

// 1. Establish time boundaries to enforce day vs night logic
    long now = new Date().getTime()
    long sunriseTime = (current.sunrise != null) ? (current.sunrise.toLong() * 1000) : 0
    long sunsetTime = (current.sunset != null) ? (current.sunset.toLong() * 1000) : 0
    
    def estimatedLux = 5
    
    // Check if the current time falls during daylight hours
    if (now > sunriseTime && now < sunsetTime) {
        if (uvi != null) {
            // Standard clear sky approximation derived from UV Index
            // Clear sky max lux can approach ~10,000 lux per 1 UVI under peak conditions
            def clearSkyLux = uvi * 10000
            
            // Attenuate based on cloud percentage coverage
            estimatedLux = clearSkyLux * (1.0 - (clouds / 100.0) * 0.75)
            
            // Soft minimum ceiling for a daytime period (e.g., highly overcast daylight shouldn't be pitch black)
            estimatedLux = Math.max(estimatedLux.toInteger(), 50)
        } else {
            // Fallback daylight approximation if UVI is omitted from payload
            estimatedLux = 10000 * (1.0 - (clouds / 100.0) * 0.8)
            estimatedLux = Math.max(estimatedLux.toInteger(), 100)
        }
    } else {
        // Enforce a natural nighttime floor value (0 to 5 lux depending on personal preference)
        estimatedLux = 5
    }

    // 2. Format and convert values strictly honoring user preferenced settings
    String targetIlluminanceUnit = settings.illuminanceUnit ?: "lx"
    String illuminanceUnitLabel = " lx"
    def finalIlluminanceValue = estimatedLux

    if (targetIlluminanceUnit == "fc") {
        // 1 Lux ≈ 0.092903 Foot-candles
        finalIlluminanceValue = (estimatedLux * 0.092903).setScale(0, java.math.RoundingMode.HALF_UP)
        illuminanceUnitLabel = " fc"
    } else if (targetIlluminanceUnit == "ph") {
        // 1 Lux = 0.0001 Phot
        finalIlluminanceValue = (estimatedLux * 0.0001).setScale(4, java.math.RoundingMode.HALF_UP)
        illuminanceUnitLabel = " ph"
    } else if (targetIlluminanceUnit == "none") {
        illuminanceUnitLabel = ""
    }

    // 3. Dispatch standard capability event (always numeric, defaults to standard Lux scale)
	sendIfChanged(name: "currentIlluminance", value: estimatedLux, descriptionText: "Estimated ambient illuminance is ${estimatedLux}")
	sendIfChanged(name: "currentIlluminanceText", value: "${estimatedLux} ${luxUnitLabel}".trim(), descriptionText: "Formatted illuminance text is ${estimatedLux} ${luxUnitLabel}".trim())
    
    // Optional dashboard custom text tile generation
    String formattedIlluminanceStr = "${finalIlluminanceValue}${illuminanceUnitLabel}"
    logDebug "Calculated Illuminance: ${estimatedLux} lx (Preference display target: ${formattedIlluminanceStr})"

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
    String IconUrlTom = ""
    String IconUrlDAT = ""

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
                int condId1 = d1.weather[0].id != null ? d1.weather[0].id.toInteger() : 0
                Map pCond1 = lookupConditionDetails(condId1)
                String altImg1 = (iCode1?.endsWith("d") ?: false) ? pCond1.altDay : pCond1.altNight
                
                if (settings.altIconsEnable == true && altImg1) {
                    IconUrlTom = iconBasePath.endsWith("/") ? "${iconBasePath}${altImg1}" : "${iconBasePath}/${altImg1}"
                } else {
                    IconUrlTom = iconBasePath.endsWith("/") ? "${iconBasePath}${iCode1}.png" : "${iconBasePath}/${iCode1}.png"
                }
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
                int condId2 = d2.weather[0].id != null ? d2.weather[0].id.toInteger() : 0
                Map pCond2 = lookupConditionDetails(condId2)
                String altImg2 = (iCode2?.endsWith("d") ?: false) ? pCond2.altDay : pCond2.altNight
                
                if (settings.altIconsEnable == true && altImg2) {
                    IconUrlDAT = iconBasePath.endsWith("/") ? "${iconBasePath}${altImg2}" : "${iconBasePath}/${altImg2}"
                } else {
                    IconUrlDAT = iconBasePath.endsWith("/") ? "${iconBasePath}${iCode2}.png" : "${iconBasePath}/${iCode2}.png"
                }
            }
        }
    }

    logInfo "Weather data processing completed. Dispatched to active Device states layout."
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
                      "<tr><td>Today</td><td>Tom</td><td>Day After</td></tr>" +
                      "<tr><td>${curTempStr}°</td><td>${tHigh1Str}°</td><td>${tHigh2Str}°</td></tr>" +
                      "</table>"

	if (tempValue != null) sendIfChanged(name: "currentTemperature", value: tempValue, unit: tempUnitLabel)
    if (humidityValue != null) sendIfChanged(name: "humidity", value: humidityValue, unit: "%")
    if (pressureValue != null) sendIfChanged(name: "pressure", value: pressureValue, unit: pressureUnitLabel)
    sendIfChanged(name: "ultravioletIndex", value: uvi ?: 0)
    sendIfChanged(name: "illuminance", value: estimatedLux, unit: "lux")

    sendIfChanged(name: "weatherDescription", value: description)
    sendIfChanged(name: "weather", value: mainWeather)
    sendIfChanged(name: "feelsLike", value: feelsLikeValue ?: tempValue)
    sendIfChanged(name: "city", value: calculatedCityAttr)
    
    sendIfChanged(name: "currentConditionCode", value: conditionId)
    sendIfChanged(name: "currentConditionType", value: parsedCond.type)
    sendIfChanged(name: "currentConditionTypeFull", value: parsedCond.desc)
	sendIfChanged(name: "currentConditionIcon", value: iconNameValue)
    sendIfChanged(name: "currentConditionIconImg", value: formattedConditionIcon)
    
    sendIfChanged(name: "weatherIcon", value: iconCode)
    sendIfChanged(name: "weatherIcons", value: iconCode)
    sendIfChanged(name: "forecastIcon", value: iconCode)

    sendIfChanged(name: "wind", value: windSpd)
    sendIfChanged(name: "windSpeed", value: windSpd)
    sendIfChanged(name: "windDirection", value: windDir)

    sendIfChanged(name: "sunriseTime", value: srTime)
    sendIfChanged(name: "sunsetTime", value: ssTime)
    sendIfChanged(name: "noonTime", value: "12:00")
    sendIfChanged(name: "twilightBegin", value: srTime)
    sendIfChanged(name: "twilightEnd", value: ssTime)
    
    sendIfChanged(name: "moonRise", value: moonRiseStr)
    sendIfChanged(name: "moonSet", value: moonSetStr)
    sendIfChanged(name: "moonPhase", value: moonPhase)

    sendIfChanged(name: "percentPrecip", value: pop1)
    sendIfChanged(name: "Precip0", value: precip0)
    sendIfChanged(name: "Precip1", value: precip1)
    sendIfChanged(name: "Precip2", value: precip2)
    sendIfChanged(name: "PoP1", value: pop1)
    sendIfChanged(name: "PoP2", value: pop2)
    sendIfChanged(name: "rainTomorrow", value: rainTom)
    sendIfChanged(name: "rainDAT", value: rainDat)
    
    sendIfChanged(name: "cloudPctCur", value: clouds)
    sendIfChanged(name: "cloudPctToday", value: (daily.size() > 0 ? daily[0].clouds ?: 0 : 0))
    sendIfChanged(name: "cloudPctTom", value: cloudTom)
    sendIfChanged(name: "cloudPctDAT", value: cloudDat)

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

    sendIfChanged(name: "forecastTomIconUrl", value: IconUrlTom)
    sendIfChanged(name: "forecastDATIconUrl", value: IconUrlDAT)


//  ** WEIRD needs fix
    sendIfChanged(name: "forecastTodayAltDayIcon", value: parsedCond.altDay)
    sendIfChanged(name: "forecastTodayAltNightIcon", value: parsedCond.altNight)
    sendIfChanged(name: "forecastTodayAltIconUrl", value: altIconUrl)

    
    sendIfChanged(name: "forecast_text1", value: fText1)
    sendIfChanged(name: "forecast_text2", value: fText2)
	String formattedPress = (pressureValue != null) ? "${pressureValue} ${pressureUnitLabel}" : "N/A"
	sendIfChanged(name: "currentPressureText", value: formattedPress)
    sendIfChanged(name: "lastUpdated", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))
    
    if (current.sunrise && current.sunset) {
	state.todaySunriseEpoch = current.sunrise.toString().toLong()
	state.todaySunsetEpoch = current.sunset.toString().toLong()
    }
    
    BigDecimal altitude = calcSunPosition()
    calcAlertsState(json, calculatedCityAttr, iconBasePath)
    calcBetwixtState(altitude)
    calcIsDayState(altitude)
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
    
    // Format the current time for the "as of" timestamp
    String lastPollTime = new Date().format("HH:mm", location.timeZone)
    String currentAlertDescFull = "No active alerts for ${calculatedCityAttr} at last poll as of ${lastPollTime}"
    
    // Assembles the path for the default OpenWeatherMap alert/branding icon
    String alertIconUrl = iconBasePath.endsWith("/") ? "${iconBasePath}OWM.png" : "${iconBasePath}/OWM.png"
    
    // Fallback block: Formatted with the icon, "as of", and the last poll time on the last line
    String alertTile = "<div style='text-align:center;'>No active weather alerts from<br>Source: OpenWeatherMap</div>" + 
                       "<div style='text-align:center; margin-top:5px; font-size:0.8em;'>" + 
                       "<img src='${alertIconUrl}' style='max-width:25px; max-height:25px; vertical-align:middle; margin-right:5px;'/>" + 
                       "Updated ${lastPollTime}</div>"
                       
    if (alerts.size() > 0) {
        def a = alerts[0]
        alertActive = a.event ?: "Active Alert"
        currentAlertSender = a.sender_name ?: "Unknown"
        currentAlertDesc = a.description ? a.description.take(100) + "..." : "N/A"
        currentAlertDescFull = (a.description ?: "N/A") + " as of ${lastPollTime}"
        
        // Active alert block: Appends the matching icon and timestamp format to the bottom wrapper row
        alertTile = "<div style='color:red; font-weight:bold; text-align:center;'>${alertActive}</div>" + 
                    "<div style='font-size:0.8em;'>${currentAlertDesc}</div>" + 
                    "<div style='text-align:center; margin-top:5px; font-size:0.8em;'>" + 
                    "<img src='${alertIconUrl}' style='max-width:25px; max-height:25px; vertical-align:middle; margin-right:5px;'/>" + 
                    "as of ${lastPollTime}</div>"
    }

    // Dispatch alert events if the settings permit (or unconditionally as originally written)
    sendIfChanged(name: "currentAlert", value: alertActive)
    sendIfChanged(name: "currentAlertSender", value: currentAlertSender)
    sendIfChanged(name: "currentAlertDesc", value: currentAlertDesc)
    sendIfChanged(name: "currentAlertDescFull", value: currentAlertDescFull)
    sendIfChanged(name: "alertTile", value: alertTile)
}

private void calcBetwixtState(BigDecimal altitudeDeg) {
    // Check if Slice Of Day is enabled in device preferences
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
    
    sendIfChanged(name: "isDay", value: isDayText) 
    logDebug "Calculated isDay: ${isDayText}" 
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
    if (logWarnEnable) log.warn "OpenWeatherMap Driver WARNING: ${msg}"
}

private void logError(String msg) {
    if (logErrorEnable) log.error "OpenWeatherMap Driver ERROR: ${msg}"
}