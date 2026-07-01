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

        // Extended Cloud Coverage Metrics
        attribute "cloudToday", "number"
        attribute "cloudTomorrow", "number"
        attribute "cloudDayAfterTomorrow", "number"
    }

    preferences {
        input name: "apiKey", type: "text", title: "API Key", description: "Enter your OpenWeatherMap API Key here", required: true
        input name: "apiSelection", type: "enum", title: "API Version", options: ["3.0": "One Call 3.0", "4.0": "One Call 4.0"], defaultValue: "3.0", required: true
        
        // Optional City field that dynamically overrides latitude/longitude if populated
        input name: "overrideCity", type: "text", title: "Base Override - City", description: "Optional - e.g., Newberg, OR or London, UK. Overrides Latitude/Longitude parameters configured below.", required: false
        input name: "latitude", type: "text", title: "Base Override - Latitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "longitude", type: "text", title: "Base Override - Longitude", description: "Optional - Leave blank to use Hub location", required: false
        
        // Display Selector Options
        input name: "precisionPrecip", type: "enum", title: "Display Decimal Precision - Precipitation", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], defaultValue: "2", required: true
        input name: "precisionPressure", type: "enum", title: "Display Decimal Precision - Pressure", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], defaultValue: "2", required: true
        input name: "precisionTemp", type: "enum", title: "Display Decimal Precision - Temperature", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], defaultValue: "2", required: true
        input name: "precisionWind", type: "enum", title: "Display Decimal Precision - Wind Speed", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], defaultValue: "2", required: true
        
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
    device.updateSetting("logDebugEnable", [value: "false", type: "bool"])
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
    // Force OWM to return a universal 'metric' baseline (Celsius, m/s, hPa)
    // This completely decouples the API payload from your display toggles,
    // allowing Distance/Speed, Pressure, and Temp to calculate independently locally.
    String owmUnits = "metric"
    String url = ""
    
    if (apiSelection == "3.0") {
        url = "https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${lon}&units=${owmUnits}&appid=${apiKey}"
    } else if (apiSelection == "4.0") {
        url = "https://api.openweathermap.org/data/4.0/onecall/current?lat=${lat}&lon=${lon}&units=${owmUnits}&appid=${apiKey}"
    }
    
    logTrace "Target URL endpoint assembled: ${url}"

    def params = [
        uri: url,
        contentType: "application/json",
        timeout: 20
    ]

    sendEvent(name: "last_poll_Forecast", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))

    try {
        asynchttpGet("handleWeatherResponse", params)
    } catch (Exception e) {
        logError "Failed to invoke async HTTP weather data payload request: ${e.message}"
    }
}

def handleWeatherResponse(response, data) {
    String statusCode = response.status ? response.status.toString() : "Unknown"
    sendEvent(name: "lastResponseCode", value: statusCode, descriptionText: "HTTP response code from OWM: ${statusCode}")
    
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
    
    int pTemp = precisionTemp != null ? precisionTemp.toInteger() : 2
    int pPressure = precisionPressure != null ? precisionPressure.toInteger() : 2
    int pWind = precisionWind != null ? precisionWind.toInteger() : 2
    int pPrecip = precisionPrecip != null ? precisionPrecip.toInteger() : 2

    // Helper closure: Converts OWM's forced 'metric' Celsius baseline to Fahrenheit locally if configured
    def convertT = { val ->
        if (val == null) return null
        BigDecimal v = val.toString().toBigDecimal()
        if (useImperialTemp == true) {
            v = (v * 1.8) + 32.0
        }
        return v.setScale(pTemp, java.math.RoundingMode.HALF_UP)
    }

    // Capture the remote observation timestamp 
    def obTime = json.current?.dt ?: (json.data ? json.data[0]?.dt : null)
    if (obTime) {
        def obDate = new java.util.Date((long)obTime * 1000)
        sendEvent(name: "last_observation_Forecast", value: obDate.format("yyyy-MM-dd HH:mm:ss", location.timeZone))
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

    // --- Core Data Extraction ---
    def tempValue = convertT(current.temp)
    def feelsLikeValue = convertT(current.feels_like)
    def rawHumidity = current.humidity != null ? current.humidity.toString().toBigDecimal() : null
    def pressureRaw = current.pressure != null ? current.pressure.toString().toBigDecimal() : null
    def rawWindSpeed = current.wind_speed != null ? current.wind_speed.toString().toBigDecimal() : null
    
    def uvi = current.uvi
    def mainWeather = current.weather ? current.weather[0]?.main : "N/A"
    def description = current.weather ? current.weather[0]?.description : "N/A"
    def iconCode = current.weather ? current.weather[0]?.icon : ""
    def clouds = current.clouds ?: 0 

    // Dynamic Wind Speed Unit Conversions (Baseline is now strictly 'm/s')
    def windSpd = 0
    if (rawWindSpeed != null) {
        String targetWindUnit = windUnit ?: "mph"
        
        if (targetWindUnit == "mph")  windSpd = rawWindSpeed * 2.236936
        else if (targetWindUnit == "kmh")  windSpd = rawWindSpeed * 3.6
        else if (targetWindUnit == "kt")   windSpd = rawWindSpeed * 1.943844
        else windSpd = rawWindSpeed // "ms"
        
        windSpd = windSpd.toBigDecimal().setScale(pWind, java.math.RoundingMode.HALF_UP)
    }
    
    def windDir = current.wind_deg != null ? current.wind_deg.toString().toBigDecimal().setScale(0, java.math.RoundingMode.HALF_UP) : 0

    // Humidity tracking scaling with precision
    def humidityValue = rawHumidity != null ? rawHumidity.setScale(0, java.math.RoundingMode.HALF_UP) : null

    // Barometric Pressure Unit Dropdown Translation
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

    // Illuminance Proxy
    def estimatedLux = 50
    if (uvi != null) {
        estimatedLux = (uvi * 10000) * (1 - (clouds / 100))
        estimatedLux = Math.max(estimatedLux.toInteger(), 5)
    }

    String srTime = formatTime(current.sunrise)
    String ssTime = formatTime(current.sunset)

    // --- Process Forecast Blocks ---
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
            if (d1.weather ? d1.weather[0]?.icon : null) { iconUrl1 = "https://openweathermap.org/img/wn/${d1.weather[0].icon}.png" }
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
            if (d2.weather ? d2.weather[0]?.icon : null) { iconUrl2 = "https://openweathermap.org/img/wn/${d2.weather[0].icon}.png" }
        }
    }

    // --- Weather Alert Slicing ---
    def alerts = json.alerts ?: []
    String alertActive = "No active alerts"
    String alertSender = "N/A"
    String alertDescr = "N/A"
    String alertDescrFull = "N/A"
    String alertTile = "<div style='text-align:center;'>No Active Weather Alerts</div>"

    if (alerts.size() > 0) {
        def a = alerts[0]
        alertActive = a.event ?: "Active Alert"
        alertSender = a.sender_name ?: "N/A"
        alertDescr = a.description ? a.description.take(100) + "..." : "N/A"
        alertDescrFull = a.description ?: "N/A"
        alertTile = "<div style='color:red; font-weight:bold; text-align:center;'>${alertActive}</div><div style='font-size:0.8em;'>${alertDescr}</div>"
    }

    String curTempStr = tempValue != null ? tempValue.toString() : "N/A"
    String tHigh1Str = fHigh1 != null ? fHigh1.toString() : "N/A"
    String tHigh2Str = fHigh2 != null ? fHigh2.toString() : "N/A"

    String tileHtml = "<table style='width:100%; text-align:center; font-size:0.9em;'>" +
                      "<tr><td>Today</td><td>Tom</td><td>D_After</td></tr>" +
                      "<tr><td>${curTempStr}°</td><td>${tHigh1Str}°</td><td>${tHigh2Str}°</td></tr>" +
                      "</table>"

    logInfo "Weather Data processing completed. Dispatched to active Device states layout."

    String calculatedCityAttr = "Local"
    if (overrideCity && overrideCity.trim() != "") {
        calculatedCityAttr = overrideCity.trim()
    } else if (state.nearestCityName != null) {
        calculatedCityAttr = state.nearestCityName
    } else if (json.timezone) {
        calculatedCityAttr = json.timezone.tokenize('/')[-1].replace('_', ' ')
    }

    if (tempValue != null) sendEvent(name: "temperature", value: tempValue, unit: (useImperialTemp ? "°F" : "°C"))
    if (humidityValue != null) sendEvent(name: "humidity", value: humidityValue, unit: "%")
    if (pressureValue != null) sendEvent(name: "pressure", value: pressureValue, unit: pressureUnitLabel)
    sendEvent(name: "ultravioletIndex", value: uvi ?: 0)
    sendEvent(name: "illuminance", value: estimatedLux, unit: "lux")

    sendEvent(name: "weatherDescription", value: description)
    sendEvent(name: "weather", value: mainWeather)
    sendEvent(name: "feelsLike", value: feelsLikeValue ?: tempValue)
    sendEvent(name: "city", value: calculatedCityAttr)
    
    sendEvent(name: "weatherIcon", value: iconCode)
    sendEvent(name: "weatherIcons", value: iconCode)
    sendEvent(name: "forecastIcon", value: iconCode)

    sendEvent(name: "wind", value: windSpd)
    sendEvent(name: "windSpeed", value: windSpd)
    sendEvent(name: "windDirection", value: windDir)

    sendEvent(name: "localSunrise", value: srTime)
    sendEvent(name: "sunriseTime", value: srTime)
    sendEvent(name: "localSunset", value: ssTime)
    sendEvent(name: "sunsetTime", value: ssTime)
    sendEvent(name: "noonTime", value: "12:00")
    sendEvent(name: "tw_begin", value: srTime)
    sendEvent(name: "tw_end", value: ssTime)
    
    sendEvent(name: "moonrise", value: moonRiseStr)
    sendEvent(name: "moonset", value: moonSetStr)
    sendEvent(name: "moon_phase", value: moonPhase)

    sendEvent(name: "altitude", value: 45)
    sendEvent(name: "azimuth", value: 180)

    sendEvent(name: "percentPrecip", value: pop1)
    sendEvent(name: "Precip0", value: precip0)
    sendEvent(name: "Precip1", value: precip1)
    sendEvent(name: "Precip2", value: precip2)
    sendEvent(name: "PoP1", value: pop1)
    sendEvent(name: "PoP2", value: pop2)
    sendEvent(name: "rainTomorrow", value: rainTom)
    sendEvent(name: "rainDayAfterTomorrow", value: rainDat)
    sendEvent(name: "cloudToday", value: clouds)
    sendEvent(name: "cloudTomorrow", value: cloudTom)
    sendEvent(name: "cloudDayAfterTomorrow", value: cloudDat)

    sendEvent(name: "alert", value: alertActive)
    sendEvent(name: "alertTile", value: alertTile)
    sendEvent(name: "alertSender", value: alertSender)
    sendEvent(name: "alertDescr", value: alertDescr)
    sendEvent(name: "alertDescrFull", value: alertDescrFull)

    sendEvent(name: "threedayfcstTile", value: tileHtml.toString())
    sendEvent(name: "condition_icon_url1", value: iconUrl1)
    sendEvent(name: "condition_icon_url2", value: iconUrl2)

    if (fHigh != null) sendEvent(name: "forecastHigh", value: fHigh)
    if (fHigh1 != null) sendEvent(name: "forecastHigh1", value: fHigh1)
    if (fHigh2 != null) sendEvent(name: "forecastHigh2", value: fHigh2)
    if (fLow != null) sendEvent(name: "forecastLow", value: fLow)
    if (fLow1 != null) sendEvent(name: "forecastLow1", value: fLow1)
    if (fLow2 != null) sendEvent(name: "forecastLow2", value: fLow2)
    
    if (fMorn != null) sendEvent(name: "forecastMorn", value: fMorn)
    if (fDay != null) sendEvent(name: "forecastDay", value: fDay)
    if (fEve != null) sendEvent(name: "forecastEve", value: fEve)
    if (fNight != null) sendEvent(name: "forecastNight", value: fNight)
    if (fMorn1 != null) sendEvent(name: "forecastMorn1", value: fMorn1)
    if (fDay1 != null) sendEvent(name: "forecastDay1", value: fDay1)
    if (fEve1 != null) sendEvent(name: "forecastEve1", value: fEve1)
    if (fNight1 != null) sendEvent(name: "forecastNight1", value: fNight1)
    
    sendEvent(name: "forecast_text1", value: fText1)
    sendEvent(name: "forecast_text2", value: fText2)
    sendEvent(name: "pressured", value: "${pressureValue} ${pressureUnitLabel}".toString())
    sendEvent(name: "lastUpdated", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))
}

private String formatTime(epoch) {
    if (!epoch) return "N/A"
    try {
        return new java.util.Date((long)epoch * 1000).format("HH:mm", location.timeZone)
    } catch (Exception e) {
        return "N/A"
    }
}

private void logInfo(msg) { if (logInfoEnable == true) log.info "${msg}" }
private void logDebug(msg) { if (logDebugEnable == true) log.debug "${msg}" }
private void logTrace(msg) { if (logTraceEnable == true) log.trace "${msg}" }
private void logWarn(msg) { if (logWarnErrorEnable == true) log.warn "${msg}" }
private void logError(msg) { if (logWarnErrorEnable == true) log.error "${msg}" }