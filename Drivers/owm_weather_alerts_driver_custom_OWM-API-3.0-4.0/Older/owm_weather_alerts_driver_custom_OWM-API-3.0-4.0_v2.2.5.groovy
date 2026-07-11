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
		attribute "currentWindSpeedText", "string"
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

		// - Current Condition attributes
		attribute "currentConditionCode", "number"
		attribute "currentConditionType", "string"
		attribute "currentConditionTypeFull", "string"
		attribute "currentConditionIcon", "string"
		
		// - Current Condition derived attribute
		attribute "currentConditionAltIcon", "string"
		attribute "currentConditionIconImg", "string"

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
		
		// - Derived attributes (used both current and forescast)
		attribute "currentWindDirection", "string"
		
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
		attribute "todayCloudPCT", "number"
		attribute "todayPOP", "number"
		attribute "todayUVI", "number"
				
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
		attribute "tomCloudPCT", "number"
		attribute "tomPOP", "number"
		attribute "tomUVI", "number"

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
		attribute "tdaCloudPCT", "number"
		attribute "tdaPOP", "number"
		attribute "tdaUVI", "number"

		// - Forecast unique derived attributes
		attribute "todayWindDirCardinal", "string"
		attribute "tomWindDirCardinal", "string"
		attribute "tdaWindDirCardinal", "string"
		attribute "todayWindDirFull", "string"
		attribute "tomWindDirFull", "string"
		attribute "tdaWindDirFull", "string"
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
		input name: "owmAlertsEnable", type: "bool", title: "Display Options - Enable Alerts Tile?", description: "Enable to Alert tile output updates on schedule for normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		input name: "sliceOfDayEnable", type: "bool", title: "Display Options - Enable Slice Of Day?", description: "Enable to slice of day text updates on schedule for normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		
        // Display Unit Selectors
		input name: "pressureUnit", type: "enum", title: "Display Unit - Barometric Pressure", options: ["hPa": "Hectopascals (hPa)", "inHg": "Inches of Mercury (inHg)", "kPa": "Kilopascals (kPa)", "mb": "Millibar (mb)", "mmHg": "Millimeters of Mercury (mmHg)", "none": "None (No Unit Suffix)"], description: "Choice of barometer unit used in tiles and logging<br>Default: <b>Inches of Mercury (inHg)</b>", defaultValue: "inHg", required: true
		input name: "humidityUnit", type: "enum", title: "Display Unit - Humidity", options: ["%": "Percent (%)", "%RH": "Percent RH (%RH)", "g/m³": "Absolute Humidity (g/m³)", "g/kg³": "Mixing Ratio (g/kg³)", "none": "None (No Unit Suffix)"], description: "Choice of humidity unit formatting used in tiles and logging<br>Default: <b>Humidity (%)</b>", defaultValue: "%", required: true
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
    if (settings.humidityUnit == null) device.updateSetting("humidityUnit", [type: "enum", value: "%RH"])
    if (settings.pressureUnit == null) device.updateSetting("pressureUnit", [type: "enum", value: "inHg"])
    if (settings.illuminanceUnit == null) device.updateSetting("illuminanceUnit", [type: "enum", value: "lx"])
    if (settings.temperatureUnit == null) device.updateSetting("temperatureUnit", [type: "enum", value: "°F"])
    if (settings.windSpeedUnit == null) device.updateSetting("windSpeedUnit", [type: "enum", value: "mph"])
    if (settings.precipUnit == null) device.updateSetting("precipUnit", [type: "enum", value: "inHr"])

    // Update current device attributes to reflect defaults on initial install
    sendIfChanged(name: "humidityUnit", value: "%RH")
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
    sendIfChanged(name: "humidityUnit", value: settings.humidityUnit ?: "%RH")
    sendIfChanged(name: "pressureUnit", value: settings.pressureUnit ?: "inHg")
    sendIfChanged(name: "illuminanceUnit", value: settings.illuminanceUnit ?: "lx")
    sendIfChanged(name: "temperatureUnit", value: settings.temperatureUnit ?: "°F")
    sendIfChanged(name: "windSpeedUnit", value: settings.windSpeedUnit ?: "mph")
    sendIfChanged(name: "precipUnit", value: settings.precipUnit ?: "inHr")	
	
	// for Home Assistant on initial load so its not empty for system capability variants
	sendIfChanged(name: "temperature", value: 0)
    sendIfChanged(name: "pressure", value: 0)
    sendIfChanged(name: "illuminance", value: 0)
	sendIfChanged(name: "humidity", value: 0)
      
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

    // 1. Fire the very first poll
    runIn(1, "scheduledPoll")
	
	// 2. Wait 7 seconds for the network call to finish, 
    // then just rebuild the tiles using the newly saved data
    runIn(7, "generateTiles")
}

void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    
    // Clears all data stored in the state map
    state.clear() 
    
    logInfo "All states have been cleared."
}

void clearAllDriverAttributes() {
    logInfo "Clearing all attributes..."
    String attributesDeleted = ''
    device.properties.supportedAttributes.each { it -> 
        attributesDeleted += "${it}, " 
        device.deleteCurrentState("$it") 
    }
    logInfo "All attributes have been cleared."
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
    // Always clear any previous scheduledPoll jobs to ensure only one is ever pending
    unschedule("scheduledPoll")

    if (dayInterval == "manual" && nightInterval == "manual") {
        logInfo "Both daytime and nighttime polling are set to MANUAL. Dynamic scheduling skipped."
        return
    }

    long now = new Date().getTime() / 1000
    boolean isDay = (now >= sunriseEpoch && now < sunsetEpoch)
    String currentInterval = isDay ? dayInterval : nightInterval

    int delaySeconds = 0

    if (currentInterval == "manual") {
        // If the current period is manual, schedule exactly for the next transition boundary
        if (isDay) {
            delaySeconds = (int)(sunsetEpoch - now)
            logDebug "Daytime polling is MANUAL. Scheduling next poll at sunset in ${delaySeconds} seconds."
        } else {
            long nextSunrise = (now > sunriseEpoch) ? (sunriseEpoch + 86400) : sunriseEpoch
            delaySeconds = (int)(nextSunrise - now)
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

// --- Modular Tile Generator Routine (< 1000 chars) ---
void generateTiles() {
    // =========================================================================
    // SECTION 1: currentTile
    // =========================================================================
    String tUnit = settings.temperatureUnit ?: "°F"
    def icon = device.currentValue("currentConditionIcon") ?: "01d"
    def temp = device.currentValue("currentTemperature") ?: "--"
    
    // Fetch the detailed description and the cached city name
    def cond = device.currentValue("currentConditionTypeFull") ?: "Clear Sky"
    String cityName = state.usedCity ?: "Local Area"
    
    def hi   = device.currentValue("todayTempMax") ?: "--"
    def lo   = device.currentValue("todayTempMin") ?: "--"
    def hum  = device.currentValue("currentHumidity") ?: "--"
    def wind = device.currentValue("currentWindSpeed") ?: "--"
    String wUnit = settings.windSpeedUnit ?: "mph"

    // Rendered HTML Tile string
    String tileHtml = "<div style='display:flex;flex-direction:column;justify-content:space-between;height:100%;padding:8px;box-sizing:border-box;background:rgba(30,30,40,0.65);border-radius:12px;color:#fff;font-family:sans-serif;line-height:1.2;text-align:center'><div style='font-size:0.85em;font-weight:bold;color:#fff;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:2px;'>${cityName}</div><div style='display:flex;align-items:center;justify-content:space-around;width:100%'><img src='https://openweathermap.org/img/wn/${icon}@2x.png' style='width:38%;max-height:60px;object-fit:contain'><div style='font-size:1.8em;font-weight:bold;letter-spacing:-1px'>${temp}${tUnit}</div></div><div style='font-size:0.8em;font-weight:600;color:#ddd;text-transform:capitalize;padding:0 4px;overflow:hidden;text-overflow:ellipsis;'>${cond}</div><div style='font-size:0.75em;color:#aaa;display:flex;justify-content:center;gap:10px'><span>H: ${hi}°</span><span>L: ${lo}°</span></div><div style='display:flex;justify-content:space-between;border-top:1px solid rgba(255,255,255,0.15);padding-top:4px;font-size:0.7em;color:#bbb'><span>💧 ${hum}%</span><span>💨 ${wind} ${wUnit}</span></div></div>"
    
    sendIfChanged(name: "currentTile", value: tileHtml)

    // =========================================================================
    // SECTION 2: 3DayForecastTile (Subroutine matching background & format)
    // =========================================================================
    // Determine dynamic Day Names for Tomorrow and Day After safely using sandboxed now()
    String day1Name = new Date(now() + 86400000).format("EEEE", location.timeZone)
    String day2Name = new Date(now() + 172800000).format("EEEE", location.timeZone)

    // Helper closure to build identical columns cleanly
    def buildForecastCol = { dayLabel, prefix ->
        def fHi   = device.currentValue("${prefix}TempMax") ?: "--"
        def fLo   = device.currentValue("${prefix}TempMin") ?: "--"
        def fPop  = device.currentValue("${prefix}POP") ?: 0
        def fSummary = device.currentValue("${prefix}Summary") ?: "Clear"
        
        int popPct = (fPop.toBigDecimal() * 100).intValue()
        String popDisplay = popPct > 0 ? "💧 ${popPct}%" : "☀️ 0%"

        return """
            <div style='flex:1; display:flex; flex-direction:column; justify-content:space-between; align-items:center; padding:0 4px; min-width:0;'>
                <div style='font-size:0.8em; font-weight:bold; color:#fff;'>${dayLabel}</div>
                <div style='font-size:1.1em; font-weight:bold; margin:4px 0;'>${fHi}°<span style='font-size:0.75em; color:#aaa; font-weight:normal;'> / ${fLo}°</span></div>
                <div style='font-size:0.7em; color:#ddd; font-weight:600; text-transform:capitalize; text-overflow:ellipsis; white-space:nowrap; overflow:hidden; width:100%; margin-bottom:4px;'>${fSummary}</div>
                <div style='font-size:0.7em; color:#bbb;'>${popDisplay}</div>
            </div>
        """
    }

    // Assemble the 3 columns horizontally inside the parent panel styling
    String forecastHtml = "<div style='display:flex; flex-direction:row; justify-content:space-between; height:100%; padding:10px 6px; box-sizing:border-box; background:rgba(30,30,40,0.65); border-radius:12px; color:#fff; font-family:sans-serif; line-height:1.2; text-align:center; gap:4px;'>" +
                          buildForecastCol("Today", "today") +
                          "<div style='border-left:1px solid rgba(255,255,255,0.1); height:100%;'></div>" +
                          buildForecastCol(day1Name, "tom") +
                          "<div style='border-left:1px solid rgba(255,255,255,0.1); height:100%;'></div>" +
                          buildForecastCol(day2Name, "tda") +
                          "</div>"

    sendIfChanged(name: "3DayForecastTile", value: forecastHtml)
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
    
    // Extract location and configuration details for the alert builder
    String calculatedCityAttr = state.usedCity ?: "Local Area" 
    String iconBasePath = state.iconBasePath ?: "https://tinyurl.com/icnqz/" 
    
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
    // Execute alerts calculation with live payload data
    calcAlertsState(json, calculatedCityAttr, iconBasePath) 

	calcCurrentTwilight()

    // Call tile generation during startup/initialization
	calcCurrentTwilight()
    generateTiles() 

    // Check if this was the first setup poll
    if (state.isInitializing == true) {
        logInfo "First initialization poll complete. Triggering follow-up poll to populate tiles..."
        state.isInitializing = false // Clear the flag so it doesn't loop
        
        // Use a tiny 1-second delay just to let the hub commit state database changes safely
        runIn(1, "scheduledPoll")
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

		// Cleaned up sendIfChanged calls
		sendIfChanged(name: "currentRain", value: rainVal)
		sendIfChanged(name: "currentSnow", value: snowVal)
		sendIfChanged(name: "currentRainText", value: "${rainVal} ${preUnit}")
		sendIfChanged(name: "currentSnowText", value: "${snowVal} ${preUnit}")
		
		// Current sunrise/sunset
		if (current.sunrise != null) sendIfChanged(name: "currentSunriseTime", value: current.sunrise)
		if (current.sunset  != null) sendIfChanged(name: "currentSunsetTime",  value: current.sunset)
	
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
         if (current.wind_deg != null) {
            Map wDir = convertWindDirectionState(current.wind_deg)
            sendIfChanged(name: "currentWindDirCardinal", value: wDir.cardinal)
            sendIfChanged(name: "currentWindDirFull", value: wDir.full)
        }       
        // Handle nested weather condition arrays safely if available
        if (current.weather && current.weather[0]) {
            sendIfChanged(name: "currentConditionCode", value: current.weather[0].id)
            sendIfChanged(name: "currentConditionType", value: current.weather[0].main)
            sendIfChanged(name: "currentConditionTypeFull", value: current.weather[0].description)
            sendIfChanged(name: "currentConditionIcon", value: current.weather[0].icon)
	logTrace "sendOWMData Current section ended"

        }
    }

    // ==========================================
    // 2. TODAY DATA DISPATCHES (data.0)
    // ==========================================
    if (today) {
		logTrace "sendOWMData today section started"		

			// Today sunrise/sunset
		if (today.sunrise != null) sendIfChanged(name: "todaySunriseTime", value: today.sunrise)
		if (today.sunset  != null) sendIfChanged(name: "todaySunsetTime",  value: today.sunset)
		
        if (today.pop != null) sendIfChanged(name: "todayPOP", value: today.pop)
        if (today.summary != null) sendIfChanged(name: "todaySummary", value: today.summary)
        if (today.moonrise != null) sendIfChanged(name: "todayMoonriseTime", value: today.moonrise)
        if (today.moonset != null) sendIfChanged(name: "todayMoonsetTime", value: today.moonset)
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
		// shared values found in current and forecasts
		if (today.pressure != null) {
            BigDecimal calcPressure = convertPressure(today.pressure)
            sendIfChanged(name: "todayPressure", value: calcPressure)
        }
        if (today.humidity != null) {
			sendIfChanged(name: "todayHumidity", value: convertHumidity(today.humidity))
        }
        if (today.dew_point != null) {
            sendIfChanged(name: "todayDewPoint", value: convertKelvin(today.dew_point))
        }
        if (today.uvi != null) {
            sendIfChanged(name: "todayUVI", value: today.uvi)
        }
       if (today.wind_gust != null) {
            sendIfChanged(name: "todayWindGust", value: convertWindSpeed(today.wind_gust))
        }
        if (today.wind_deg != null) {
            sendIfChanged(name: "todayWindDeg", value: today.wind_deg)
        }
        // Wind Speed elements converted from m/s using user selection preference logic
        if (today.wind_speed != null) {
            sendIfChanged(name: "todayWindSpeed", value: convertWindSpeed(today.wind_speed))
        }
         if (today.wind_deg != null) {
            Map wDir = convertWindDirectionState(today.wind_deg)
            sendIfChanged(name: "todayWindDirCardinal", value: wDir.cardinal)
            sendIfChanged(name: "todayWindDirFull", value: wDir.full)
        } 
        if (today.clouds != null) sendIfChanged(name: "todayCloudPCT", value: today.clouds)
	logTrace "sendOWMData today section ended"
    }

    // ==========================================
    // 3. TOMORROW DATA DISPATCHES (data.1)
    // ==========================================
    if (tom) {
	logTrace "sendOWMData tom section started"		
			// Tomorrow sunrise/sunset
		if (tom.sunrise != null) sendIfChanged(name: "tomSunriseTime", value: tom.sunrise)
		if (tom.sunset  != null) sendIfChanged(name: "tomSunsetTime",  value: tom.sunset)
		
        if (tom.pop != null) sendIfChanged(name: "tomPOP", value: tom.pop)
        if (tom.summary != null) sendIfChanged(name: "tomSummary", value: tom.summary)
        if (tom.moonrise != null) sendIfChanged(name: "tomMoonriseTime", value: tom.moonrise)
        if (tom.moonset != null) sendIfChanged(name: "tomMoonsetTime", value: tom.moonset)
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
		// shared values found in current and forecasts
		if (tom.pressure != null) {
            BigDecimal calcPressure = convertPressure(tom.pressure)
            sendIfChanged(name: "tomPressure", value: calcPressure)
        }
        if (tom.humidity != null) {
			sendIfChanged(name: "tomHumidity", value: convertHumidity(tom.humidity))
        }
        if (tom.dew_point != null) {
            sendIfChanged(name: "tomDewPoint", value: convertKelvin(tom.dew_point))
        }
        if (tom.uvi != null) {
            sendIfChanged(name: "tomUVI", value: tom.uvi)
        }
       if (tom.wind_gust != null) {
            sendIfChanged(name: "tomWindGust", value: convertWindSpeed(tom.wind_gust))
        }
        if (tom.wind_deg != null) {
            sendIfChanged(name: "tomWindDeg", value: tom.wind_deg)
        }
        // Wind Speed elements converted from m/s using user selection preference logic
        if (tom.wind_speed != null) {
            sendIfChanged(name: "tomWindSpeed", value: convertWindSpeed(tom.wind_speed))
        }
         if (tom.wind_deg != null) {
            Map wDir = convertWindDirectionState(tom.wind_deg)
            sendIfChanged(name: "tomWindDirCardinal", value: wDir.cardinal)
            sendIfChanged(name: "tomWindDirFull", value: wDir.full)
        } 
        if (tom.clouds != null) sendIfChanged(name: "tomCloudPCT", value: tom.clouds)
	logTrace "sendOWMData tomorrow section ended"
    }

    // ==========================================
    // 4. DAY AFTER TOMORROW DATA DISPATCHES (data.2)
    // ==========================================
    // ==========================================
    if (tda) {
		logTrace "sendOWMData tda section started"		
		// Tomorrow Day After sunrise/sunset
		if (tda.sunrise != null) sendIfChanged(name: "tdaSunriseTime", value: tda.sunrise)
		if (tda.sunset  != null) sendIfChanged(name: "tdaSunsetTime",  value: tda.sunset)
		
        if (tda.pop != null) sendIfChanged(name: "tdaPOP", value: tda.pop)
        if (tda.summary != null) sendIfChanged(name: "tdaSummary", value: tda.summary)
        if (tda.moonrise != null) sendIfChanged(name: "tdaMoonriseTime", value: tda.moonrise)
        if (tda.moonset != null) sendIfChanged(name: "tdaMoonsetTime", value: tda.moonset)
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
		// shared values found in current and forecasts
		if (tda.pressure != null) {
            BigDecimal calcPressure = convertPressure(tda.pressure)
            sendIfChanged(name: "tdaPressure", value: calcPressure)
        }
        if (tda.humidity != null) {
			sendIfChanged(name: "tdaHumidity", value: convertHumidity(tda.humidity))
        }
        if (tda.dew_point != null) {
            sendIfChanged(name: "tdaDewPoint", value: convertKelvin(tda.dew_point))
        }
        if (tda.uvi != null) {
            sendIfChanged(name: "tdaUVI", value: tda.uvi)
        }
       if (tda.wind_gust != null) {
            sendIfChanged(name: "tdaWindGust", value: convertWindSpeed(tda.wind_gust))
        }
        if (tda.wind_deg != null) {
            sendIfChanged(name: "tdaWindDeg", value: tda.wind_deg)
        }
        // Wind Speed elements converted from m/s using user selection preference logic
        if (tda.wind_speed != null) {
            sendIfChanged(name: "tdaWindSpeed", value: convertWindSpeed(tda.wind_speed))
        }
         if (tda.wind_deg != null) {
            Map wDir = convertWindDirectionState(tda.wind_deg)
            sendIfChanged(name: "tdaWindDirCardinal", value: wDir.cardinal)
            sendIfChanged(name: "tdaWindDirFull", value: wDir.full)
        } 
        if (tda.clouds != null) sendIfChanged(name: "tdaCloudPCT", value: tda.clouds)
	logTrace "sendOWMData tda section ended"		
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
            } else {
                logDebug "convertHumidity: currentTemperature is null; fallback to raw value."
            }
            break

        case "g/kg": // Mixing Ratio
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
            } else {
                logDebug "convertHumidity: currentTemperature is null; fallback to raw value."
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
        return new BigDecimal("0.0").setScale(precision, java.math.RoundingMode.HALF_UP)
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
    return precip.setScale(precision, java.math.RoundingMode.HALF_UP)
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

private Map convertWindDirectionState(degrees) {
    if (degrees == null) {
        logDebug "convertWindDirectionState received null value."
        return [cardinal: "Unknown", full: "Unknown"]
    }
    
    double deg = 0.0
    try {
        deg = degrees.toDouble()
    } catch (Exception e) {
        logError "Failed to parse wind degrees (${degrees}): ${e.message}"
        return [cardinal: "Unknown", full: "Unknown"]
    }

    deg = (deg % 360 + 360) % 360

    def cardinals = [
        "N", "NNE", "NE", "ENE",
        "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW",
        "W", "WNW", "NW", "NNW"
    ]
    
    def fullWords = [
        "North", "North-Northeast", "Northeast", "East-Northeast",
        "East", "East-Southeast", "Southeast", "South-Southeast",
        "South", "South-Southwest", "Southwest", "West-Southwest",
        "West", "West-Northwest", "Northwest", "North-Northwest"
    ]
    
    int index = (int) Math.round(deg / 22.5) % 16
    return [cardinal: cardinals[index], full: fullWords[index]]
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
    String cityName = state.usedCity ?: "Local Area"
    
    // Shared container styling matching currentTile
    String baseStyle = "display:flex;flex-direction:column;justify-content:space-between;height:100%;padding:8px;box-sizing:border-box;background:rgba(30,30,40,0.65);border-radius:12px;color:#fff;font-family:sans-serif;line-height:1.2;text-align:center"
    String currentAlertTile = ""

    if (alerts.size() > 0) {
        def a = alerts[0]
        alertActive = a.event ?: "Active Alert"
        currentAlertSender = a.sender_name ?: "Unknown"
        currentAlertDesc = a.description ? (a.description.length() > 100 ? a.description.take(100) + "..." : a.description) : "No active alerts"
        currentAlertDescFull = (a.description ?: "N/A") + " as of ${lastPollTime}"
        
        // Active Alert Layout
        currentAlertTile = "<div style='${baseStyle}'>" +
                           "<div style='font-size:0.85em;font-weight:bold;color:#ff5555;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:2px;'>⚠️ ${alertActive}</div>" +
                           "<div style='font-size:0.8em;font-weight:600;color:#ddd;padding:4px;overflow:hidden;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;'>${currentAlertDesc}</div>" +
                           "<div style='display:flex;justify-content:space-between;border-top:1px solid rgba(255,255,255,0.15);padding-top:4px;font-size:0.7em;color:#bbb'>" +
                           "<span>👤 ${currentAlertSender.take(12)}</span><span>⏰ ${lastPollTime}</span>" +
                           "</div>" +
                           "</div>"
    } else {
        // No Active Alerts Layout
        currentAlertTile = "<div style='${baseStyle}'>" +
                           "<div style='font-size:0.85em;font-weight:bold;color:#55ff55;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:2px;'>✅ Your Local Area</br>Clear Weather</div>" +
                           "<div style='font-size:0.8em;font-weight:600;color:#aaa;padding:8px 4px;'>No active weather alerts from OpenWeatherMap or their sources</div>" +
                           "<div style='display:flex;justify-content:center;border-top:1px solid rgba(255,255,255,0.15);padding-top:4px;font-size:0.7em;color:#bbb'>" +
                           "<span>Updated ${lastPollTime}</span>" +
                           "</div>" +
                           "</div>"
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
        sendIfChanged(name: "currentTemperatureText", value: "${tempVal}${tUnit}")
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

// 6. Humidity Formatting
	def humVal = device.currentValue("currentHumidity")
	if (humVal != null) {
    String hUnit = settings.humidityUnit ?: "%"

    if (hUnit == "none") hUnit = ""
	if (hUnit == "%RH") hUnit = "%RH"
	if (hUnit == "%") hUnit = "%"
	if (hUnit == "g/m³") hUnit = " g/m³"
	if (hUnit == "g/kg³") hUnit = " g/kg³"
    sendIfChanged(name: "currentHumidityText", value: "${humVal}${hUnit ? "${hUnit}" : ""}")
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