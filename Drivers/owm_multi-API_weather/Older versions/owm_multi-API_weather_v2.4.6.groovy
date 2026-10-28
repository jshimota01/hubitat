/**
 * OpenWeatherMap Multi-API Weather Driver
 * Platform: Hubitat Elevation
 * Compatible with One Call API 2.5, 3.0, and 4.0 Keys
 * Capabilities: Temperature, Illuminance, Barometric Pressure, Relative Humidity, Ultraviolet Index
 
	Licensed under the Apache License, Version 2.0 (the 'License'); you may not use this file except
	in compliance with the License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
	on an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
	for the specific language governing permissions and limitations under the License.
	
**/
/**
	This is a complete rewrite of the original driver done by James Shimota.  This driver is designed to fit 
	a number of needs and does both Tile generation for dashboards AND attributes useful for functions within Hubitat Elevation Rules
	and other apps by the community.  It was original written with the intent of staying a private code work, but I felt
	that there may be value to others in the community.
	The original was title Weather-Alerts and was very Alert tile centric I felt.  It also relied on multiple sources such 
	as OpenWeatherMaps (OWM) and Natiion Weather Service (NWS).  The data returned on NWS was frequently bad and not accurate for 
	my location - I'm in the farm country of Oregon and that created a problem since the City would often say 'local area' instead
	of my actual city!
	
	Icons and Images - there are multiple sources of images used in this driver.  The original base path is the OWM API,
	but the base path can also use the Alternate Path which defaults to the Hubitat Public space.  In addition, there are 
	images from the an app written previously for the Moon Phase, and that also includes a dynamic 'shading' svg if desired.
	Wind Direction images are available from my repo - and if there are no wind direction images, icons are used.
	
	VERSIONS:
	v2.4.6	07/28/26	jshimota	Renamed Wind Direction Icon attributes to WindDirectionImage
	v2.4.5	07/28/26	jshimota	Fix init race condition by evaluating sun position/isDay prior to parsing forecast data
	v2.4.4	07/28/26	jshimota	Fix race condition on forecast tile. fixed state storage of sun altitude to correct name
	v2.4.3	07/28/26	jshimota	changed altitude and azimuth to currentSun prefixed, added currentMoon values, 
	v2.4.2	07/28/26	jshimota	changed api log switch to logging section. renamed precisionSunAngles to precisionSunMoonAngles
	v2.4.1	07/25/26	jshimota	Added button to allow a user to clear all schedules to help people converting
	v2.4.0	07/25/26	jshimota	Initial public release
	v2.3.0	07/20/26	jshimota	Integrated Moon Phase
	v2.2.0	07/17/26	jshimota	basepath modifications
	v2.1.0	07/15/26	jshimota	start point
**/

static String version()    {  return '2.4.6'  }

metadata {
    definition(
            name: "OpenWeatherMap Multi-API Weather Driver",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/owm_multi-API_weather/owm_multi-API_weather.groovy"
    ) {
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
        attribute "currentAlertDesc", "string"
        attribute "currentAlertSender", "string"
        attribute "currentAlertDescFull", "string"
        
        // - Calculated Solar Angle attributes
        attribute "currentSunAltitude", "number"
        attribute "currentSunAzimuth", "number"
        attribute "currentSunAltitudeText", "string"
        attribute "currentSunAzimuthText", "string"
        
        // - Calculated Moon Angle attributes
        attribute "currentMoonAltitude", "number"
        attribute "currentMoonAzimuth", "number"
        attribute "currentMoonAltitudeText", "string"
        attribute "currentMoonAzimuthText", "string"

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
        attribute "currentVisibility", "number"
        attribute "currentTwilightBeginTime", "number"
        attribute "currentSolarNoonTime", "number"
        attribute "currentTwilightEndTime", "number"
        attribute "currentIsDay", "enum", ["true","false"]
        attribute "currentWindDirCardinal", "string"
        attribute "currentWindDirFull", "string"
        attribute "currentWindDirImageUrl", "string"
        attribute "currentWindDirectionImage", "string"
        attribute "currentTwilightBeginTimeText", "string"
        attribute "currentSolarNoonTimeText", "string"
        attribute "currentTwilightEndTimeText", "string"
        attribute "currentVisibilityText", "string"
        attribute "currentWeatherSummaryText", "string"
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
        attribute "todayWindDirImageUrl", "string"
        attribute "tomWindDirImageUrl", "string"
        attribute "tdaWindDirImageUrl", "string"
        attribute "todayWindDirectionImage", "string"
        attribute "tomWindDirectionImage", "string"
        attribute "tdaWindDirectionImage", "string"
        attribute "todayDate", "number"
        attribute "tomDate", "number"
        attribute "tdaDate", "number"
        attribute "todayDateText", "string"
        attribute "tomDateText", "string"
        attribute "tdaDateText", "string"
        attribute "todayMoonPhaseText", "string"
        attribute "tomMoonPhaseText", "string"
        attribute "tdaMoonPhaseText", "string"
		
		// - Moon Phase attributes
        attribute "todayMoonPhaseSvgImage", "string"
        attribute "tomMoonPhaseSvgImage", "string"
        attribute "tdaMoonPhaseSvgImage", "string"
        attribute "todayMoonPhasePngImageUrl", "string"
        attribute "tomMoonPhasePngImageUrl", "string"
        attribute "tdaMoonPhasePngImageUrl", "string"
		attribute "todayMoonPhaseEmojiIcon", "string"
		attribute "tomMoonPhaseEmojiIcon", "string"
		attribute "tdaMoonPhaseEmojiIcon", "string"
		
		// - Wind Direction Emoji Attributes
        attribute "currentWindDirectionEmojiIcon", "string"
        attribute "todayWindDirectionEmojiIcon", "string"
        attribute "tomWindDirectionEmojiIcon", "string"
        attribute "tdaWindDirectionEmojiIcon", "string"

		// - Forecast Weather Condition Attributes
		attribute "todayConditionCode", "number"
		attribute "todayConditionType", "string"
		attribute "todayConditionTypeDesc", "string"
		attribute "todayConditionTypeAltDesc", "string"
		attribute "todayConditionIcon", "string"
		attribute "todayConditionAltIcon", "string"

		attribute "tomConditionCode", "number"
		attribute "tomConditionType", "string"
		attribute "tomConditionTypeDesc", "string"
		attribute "tomConditionTypeAltDesc", "string"
		attribute "tomConditionIcon", "string"
		attribute "tomConditionAltIcon", "string"

		attribute "tdaConditionCode", "number"
		attribute "tdaConditionType", "string"
		attribute "tdaConditionTypeDesc", "string"
		attribute "tdaConditionTypeAltDesc", "string"
		attribute "tdaConditionIcon", "string"
		attribute "tdaConditionAltIcon", "string"

		// - Tiles
        attribute "currentAlertTile", "string"
        attribute "3DayForecastTile", "string"
        attribute "currentTile", "string"
		attribute "currentMoonPhaseTile", "string"
        
        command "clearAllDriverStates"
        command "clearAllAttributes"
		command "clearAllSchedules"
        command "pollOWM"
    }

    preferences {
        input name: "apiKey", type: "text", title: "API Key", description: "Enter your OpenWeatherMap API Key here<br><b>Required by OpenWeatherMaps</b>", required: true
        input name: "apiSelection", type: "enum", title: "API Version", options: ["2.5": "One Call 2.5 (Obsolete!)","3.0": "One Call 3.0", "4.0": "One Call 4.0"], defaultValue: "3.0", description: "Select your API Key version here<br><b>Required by OpenWeatherMaps</b><br><i>*Note: 2.5 API is now obsolete as of June 2024</i><br><i>*Note: 4.0 API key uses 3.0 API poll method</i>", required: true
        
        // Optional City field that dynamically overrides latitude/longitude if populated
        input name: "overrideCity", type: "text", title: "Base Override - City", description: "Optional - Will attempt to geo lookup and override <b>ALL</b> latitude/longitude values<br><b>Default:(empty)</b><br><i>EG: Portland, OR or London, UK.<br>*Note: Overrides Latitude/Longitude parameters of Hub <b>AND</b> values configured below</i>", required: false
		
		// Optional Image location overrides
        input name: "altIconLoc", type: "text", title: "Base Override - Icon Location", description: "Optional - Icon Source Location:<br><i>blank for default OWM location<br>https://tinyurl.com/icnqz/ (Which redirects directly to the<br>Hubitat Community Weather Icons repository)</i>", required: false
        input name: "altMoonPhaseImagePath", type: "text", title: "Base Override - Moon Phase Image Location", description: "Optional - Moon Phase Image Source Location:<br><i>blank for default Moon Phase Image location<br>https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/<br>(Supplied by @thebearmay of the Hubitat community who wrote & maintains the original MoonPhase Tile driver)</i>", required: false
        input name: "altWindDirectionImageLoc", type: "text", title: "Base Override - Wind Direction Image Location", description: "Optional - Wind Direction Image Source Location:<br><i>If not used, Wind Direction icons are used</i>", required: false
        
        input name: "overrideLatitude", type: "decimal", title: "Base Override - Latitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "overrideLongitude", type: "decimal", title: "Base Override - Longitude", description: "Optional - Leave blank to use Hub location", required: false
        input name: "altIconsEnable", type: "bool", title: "Base Override - Use Alternative Icons?", description: "Turn ON to use alternate icons (found in csv map within the driver), or OFF to use the standard OpenWeatherMap icons<br><b>Base Override - Icon Location MUST be filled!</b>", defaultValue: false, required: true
        
        // Display Selector Options
        input name: "precisionHumid", type: "enum", title: "Display Decimal Precision - Humidity", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision for humidity readings in logging and tiles<br>Default: <b>0</b><br><i>EG: 1, 1.5, 1.55</i>", defaultValue: "0", required: true
        input name: "precisionPrecip", type: "enum", title: "Display Decimal Precision - Precipitation", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision for rainfall readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 1, 1.5, 1.55</i>", defaultValue: "2", required: true
        input name: "precisionPress", type: "enum", title: "Display Decimal Precision - Pressure", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision for barometer readings in logging and tiles<br>Default: <b>2</b><br><i>EG: 30mb,30.5mb, 30.55mb</i>", defaultValue: "2", required: true
        input name: "precisionSunMoonAngles", type: "enum", title: "Display Decimal Precision - Sun/Moon Angles", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision for sun and moon angle (altitude and azimuth) readings in logging and tiles<br>Default: <b>0</b><br><i>EG(with Unit): 149°, 149.5°, 149.55°</i>", defaultValue: "0", required: true
        input name: "precisionTemp", type: "enum", title: "Display Decimal Precision - Temperature", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision for temperature readings in logging and tiles<br>Default: <b>2</b><br><i>EG(with Unit): 70°F, 70.3°F, 70.55°F</i>", defaultValue: "2", required: true
        input name: "precisionWind", type: "enum", title: "Display Decimal Precision - Wind Speed", options: ["0": "0 Places", "1": "1 Place", "2": "2 Places"], description: "Choice of decimal precision for wind speed readings in logging and tiles<br>Default: <b>2</b><br><i>EG (with Unit): 12 mph, 12.7 mph, 12.77 mph</i>", defaultValue: "2", required: true
        
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

		// Tile Options
        input name: "displayTileMoonPhaseSVGEnable", type: "bool", title: "Tile - SVG or PNG image use in Moon Phase Tile", description: "Choice of Moon Phase Tile image type. Enabled uses SVG, disabled uses PNG<br>Default: <b>ON</b>", defaultValue: true, required: true
        input name: "debugTileEnable", type: "bool", title: "Tile - Enable Tile Debug Info", description: "Enable to embed character count string on tiles<br>Default: <b>Off</b>", defaultValue: false, required: true

        // Independent Logging Switches
		input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
        input name: "aPIKeyExposedEnable", type: "bool", title: "Logging - Expose API Key In Logging?", description: "Enable to show API Key value in log outputs<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

def installed() {
    logInfo "Driver Installed. Turning on initial debug logging for 30 minutes."
    device.updateSetting("logDebugEnable", [type: "bool", value: true])
    
    // Prefill altIconLoc default setting on install
    if (settings.altIconLoc == null || settings.altIconLoc.trim() == "") {
        device.updateSetting("altIconLoc", [type: "text", value: "https://tinyurl.com/icnqz/"])
    }

    // Push defaults to settings so they display properly on the driver page fields
    ["humidityUnit": "%RH", "pressureUnit": "inHg", "illuminanceUnit": "lx", "temperatureUnit": "°F", "windSpeedUnit": "mph", "precipUnit": "inHr", "visibilityUnit": "miles"].each { k, v ->
        if (settings[k] == null) device.updateSetting(k, [type: "enum", value: v])
        sendIfChanged(name: k, value: v)
    }

    initialize()
}

def updated() {
    logInfo "Preferences updated. Running initialization ..."
    
	// Remove old state of sunAltitude
	if (state.sunAltitude != null) {
		state.currentSunAltitude = state.sunAltitude
		state.remove("sunAltitude")
	}

    // Ensure altIconLoc retains default if cleared by user
    if (settings.altIconLoc == null || settings.altIconLoc.trim() == "") {
        device.updateSetting("altIconLoc", [type: "text", value: "https://tinyurl.com/icnqz/"])
    }

    // Update base paths dynamically on preferences update
    state.iconBasePath = calcIconBasePath(settings.altIconLoc)
    state.moonPhaseImagePath = calcMoonPhaseImagePath(settings.altMoonPhaseImagePath)
    state.windDirectionImagePath = calcWinDirImagePath(settings.altWindDirectionImageLoc)
    
    // --- FORCE REGISTER CORE & SYSTEM ATTRIBUTES ---
    sendEvent(name: "temperature", value: 0, unit: settings.temperatureUnit ?: "°F")
    sendEvent(name: "pressure", value: 0, unit: settings.pressureUnit ?: "inHg")
    sendEvent(name: "illuminance", value: 0, unit: settings.illuminanceUnit ?: "lx")
    sendEvent(name: "humidity", value: 0, unit: settings.humidityUnit ?: "%")
    sendEvent(name: "ultravioletIndex", value: 0)

    // Preset placeholder strings to ensure clean execution bounds
    ["todayMoonPhaseText", "tomMoonPhaseText", "tdaMoonPhaseText"].each { sendIfChanged(name: it, value: "--") }

    // --- CLEAR GEO CACHE TO FORCE RE-EVALUATION IF USER COMES BACK AND CHANGES IT---
    state.lastOverrideCity = null
    state.lastOverrideLatitude = null
    state.lastOverrideLongitude = null
    state.usedCity = null

    initialize()
}

def initialize() {
    logDebug "Clearing all scheduled jobs ..."
    unschedule()
    logInfo "Initializing driver ..."
  
    if (logDebugEnable == true) {
        runIn(1800, "disableDebugLogging")
    }

    // Initialize asset paths safely as persistent states
    state.moonPhaseImagePath = calcMoonPhaseImagePath(settings.altMoonPhaseImagePath)
    state.windDirectionImagePath = calcWinDirImagePath(settings.altWindDirectionImageLoc)

    // Mark that we are currently doing our initial setup
    state.isInitializing = true
    // Fire the very first poll instantly
    runIn(1, "scheduledPoll")
}

void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    state.clear()
    logInfo "All states have been cleared."
}

void clearAllAttributes() {
    logInfo "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
    logInfo "All attributes have been cleared."
}

void clearAllSchedules() {
    logInfo "Clearing all scheduled jobs (including orphaned schedules)..."
    unschedule()
    logInfo "All scheduled jobs have been successfully cleared."
}

def scheduledPoll() {
    logDebug "Scheduled background poll sequence initiated."
    pollOWM("schedule")
}

def refresh() {
    logDebug "Refresh triggered via schedule or button press."
    
    if (!apiKey) {
        logWarn "Execution halted: API Key entry is missing!"
        return
    }
    
    pollOWM("refresh")
}

void scheduledTextValue(List dataList) {
    logDebug "Unpacking scheduled calcTextValue arguments safely..."
    if (dataList && dataList.size() >= 9) {
        Map currentData = dataList[5] instanceof Map ? dataList[5] : [:]
        Map todayData   = dataList[6] instanceof Map ? dataList[6] : [:]
        Map tomData     = dataList[7] instanceof Map ? dataList[7] : [:]
        Map tdaData     = dataList[8] instanceof Map ? dataList[8] : [:]

        calcTextValue(
            dataList[0] != null ? dataList[0].toBigDecimal() : null,
            dataList[1] != null ? dataList[1].toBigDecimal() : null,
            dataList[2] != null ? dataList[2].toBigDecimal() : null,
            dataList[3] != null ? dataList[3].toBigDecimal() : null,
            dataList[4] != null ? dataList[4].toBigDecimal() : null,
            currentData,
            todayData,
            tomData,
            tdaData
        )
        
        calcMoonPhaseValue(todayData, tomData, tdaData)
        calcMoonPhaseSvgImage(todayData, tomData, tdaData)
        
        generateTiles(currentData, todayData, tomData, tdaData)
    } else {
        logWarn "Scheduled calcTextValue skipped: Argument list was incomplete."
    }
}

def pollOWM(String type = "manual") {
    switch(type) {
        case "refresh": logInfo "pollOWM run on manual Refresh"; break
        case "schedule": logInfo "polling OpenWeatherMaps API on schedule"; break
        case "manual":
        default: logInfo "PollOWM run manually"; break
    }

    logDebug "pollOWM triggered. Evaluating location coordinates..."
    
	calcLonLatCityState()
    
    if(state.usedLatitude == null || state.usedLongitude == null) {
        logWarn "pollOWM aborted: Valid coordinates are missing (Lat: ${state.usedLatitude}, Lon: ${state.usedLongitude})"
        return
    }

    BigDecimal currentAlt = calcSunPosition()
    calcBetwixtState(currentAlt)
    calcIsDayState(currentAlt)
    
    state.iconBasePath = calcIconBasePath(settings.altIconLoc)
    state.moonPhaseImagePath = calcMoonPhaseImagePath(settings.altMoonPhaseImagePath)
    state.windDirectionImagePath = calcWinDirImagePath(settings.altWindDirectionImageLoc)
    
    pollOWMAPI()
}

private void updateDynamicSchedules(long sunriseEpoch, long sunsetEpoch) {
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
        if (isDay) {
            delaySeconds = (int)(sunsetEpoch - nowTime)
            logDebug "Daytime polling is MANUAL. Scheduling next poll at sunset in ${delaySeconds} seconds."
        } else {
            long nextSunrise = (nowTime > sunriseEpoch) ? (sunriseEpoch + 86400) : sunriseEpoch
            delaySeconds = (int)(nextSunrise - nowTime)
            logDebug "Nighttime polling is MANUAL. Scheduling next poll at sunrise in ${delaySeconds} seconds."
        }
    } else {
		int intervalMinutes = (currentInterval && currentInterval.isInteger()) ? currentInterval.toInteger() : 30
        delaySeconds = intervalMinutes * 60
        logDebug "Scheduling next background poll in ${intervalMinutes} minutes (${delaySeconds} seconds) via runIn."
    }

    if (delaySeconds <= 0) delaySeconds = 1800
    runIn(delaySeconds, "scheduledPoll", [overwrite: true])
}

void generateTiles(Map currentData = [:], Map todayData = [:], Map tomData = [:], Map tdaData = [:]) {
    String tUnit = settings.temperatureUnit ?: "°F"
    String wUnit = settings.windSpeedUnit ?: "mph"
    String cityName = state.usedCity ?: "Local Area"
    
    def appendTileDebug = { String bodyHtml ->
        if (settings.debugTileEnable != true) return bodyHtml + "</div>"
        int actualContentLen = bodyHtml.length() + 6
        String footerStart = "<div style='position:absolute;top:2px;right:4px;font-size:.6em;color:#fff;background:rgba(0,0,0,0.6);padding:1px 4px;border-radius:3px;z-index:99;'>Len:"
        return bodyHtml + footerStart + "${actualContentLen}</div></div>"
    }

	// SECTION 1: currentTile
    def temp, cond, hi, lo, hum, wind, currentIconUrl, feelsLike

    if (currentData && todayData) {
        temp      = currentData.temp != null ? convertKelvin(currentData.temp) : "--"
        feelsLike = currentData.feels_like != null ? convertKelvin(currentData.feels_like) : "--"
        cond      = (currentData.weather && currentData.weather[0]) ? currentData.weather[0].description : "--"
        hi        = todayData.temp?.max != null ? convertKelvin(todayData.temp.max) : "--"
        lo        = todayData.temp?.min != null ? convertKelvin(todayData.temp.min) : "--"
        hum       = currentData.humidity != null ? convertHumidity(currentData.humidity) : "--"
        wind      = currentData.wind_speed != null ? convertWindSpeed(currentData.wind_speed) : "--"
    } else {
        temp      = device.currentValue("currentTemperature") ?: "--"
        feelsLike = device.currentValue("currentFeelsLike") ?: "--"
        cond      = device.currentValue("currentConditionTypeDesc") ?: "--"
        hi        = device.currentValue("todayTempMax") ?: "--"
        lo        = device.currentValue("todayTempMin") ?: "--"
        hum       = device.currentValue("currentHumidity") ?: "--"
        wind      = device.currentValue("currentWindSpeed") ?: "--"
    }

    currentIconUrl = device.currentValue("currentConditionAltIcon")
    if (!currentIconUrl) {
        String iconCode = (currentData.weather && currentData.weather[0]) ? currentData.weather[0].icon : (device.currentValue("currentConditionIcon") ?: "01d")
        String basePath = state.iconBasePath ?: calcIconBasePath(settings.altIconLoc)
        currentIconUrl = "${basePath}${iconCode}.png"
    }

    String windIconDisplay = "💨"
    if (settings.altIconsEnable == true) {
        String windIconUrl = device.currentValue("currentWindDirImageUrl") ?: ""
        if (windIconUrl != "") {
            windIconDisplay = "<img src='${windIconUrl}' style='height:.9em;vertical-align:middle;margin-right:.1em;'>"
        }
    }

    String sT  = (temp instanceof BigDecimal) ? temp.setScale(0, java.math.RoundingMode.HALF_UP) : temp
    String sFL = (feelsLike instanceof BigDecimal) ? feelsLike.setScale(0, java.math.RoundingMode.HALF_UP) : feelsLike
    String sH  = (hi instanceof BigDecimal) ? hi.setScale(0, java.math.RoundingMode.HALF_UP) : hi
    String sL  = (lo instanceof BigDecimal) ? lo.setScale(0, java.math.RoundingMode.HALF_UP) : lo
    String sW  = (wind instanceof BigDecimal) ? wind.setScale(0, java.math.RoundingMode.HALF_UP) : wind

    String currentBodyHtml = "<div style='color:#fff;display:flex;flex-direction:column;justify-content:space-around;align-items:center;height:100%;box-sizing:border-box;overflow:hidden;padding:.1em;line-height:1;text-align:center;font-size:.7em'>" +
        "<div style='font-size:1.07em;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%'>Currently in ${cityName}</div>" +
        "<div style='display:flex;align-items:center'><img src='${currentIconUrl}' style='height:2em;max-width:2.4em;object-fit:contain'><b style='font-size:1.74em;margin-left:.2em'>${sT}${tUnit}</b></div>" +
        "<div style='font-size:.93em;color:#ddd;margin-top:-.2em'>(feels like ${sFL}${tUnit})</div>" +
        "<div style='text-transform:capitalize;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%'>${cond}</div>" +
        "<div>High: ${sH}° Low: ${sL}°</div>" +
        "<div style='white-space:nowrap'>💦${hum}% &nbsp;&nbsp; ${windIconDisplay}${sW} ${wUnit}</div>"

    sendIfChanged(name: "currentTile", value: appendTileDebug(currentBodyHtml))

	// SECTION 2: 3DayForecastTile
	long epochMsNow = now()
	String day1Name = (tomData && tomData.dt) ? new Date(tomData.dt * 1000L).format("EEE", location.timeZone) : "Tom"
	String day2Name = (tdaData && tdaData.dt) ? new Date(tdaData.dt * 1000L).format("EEE", location.timeZone) : "TDA"
	String pUnit = (settings.precipUnit == "none" || settings.precipUnit == null) ? "" : settings.precipUnit
	String basePath = state.iconBasePath ?: calcIconBasePath(settings.altIconLoc)

	String cIcon = device.currentValue("currentConditionAltIcon") ?: ""
	if (!cIcon && currentData && currentData.weather && currentData.weather[0]) {
		Integer code = currentData.weather[0].id != null ? currentData.weather[0].id.toInteger() : 0
		String omiIcon = currentData.weather[0].icon ?: "01d"
		if (settings.altIconsEnable == true) {
			Map condDetails = lookupConditionDetails(code)
			BigDecimal currentAlt = state.currentSunAltitude != null ? state.currentSunAltitude.toBigDecimal() : (device.currentValue("currentSunAltitude")?.toBigDecimal() ?: 0.0)
			boolean isDay = (currentAlt >= -0.833)
			String iconFilename = isDay ? condDetails.altDay : condDetails.altNight
			cIcon = "${basePath}${iconFilename}"
		} else {
			cIcon = "${basePath}${omiIcon}.png"
		}
	}

	def getFcstData = { prefix, Map sourceMap ->
		String iconUrl = device.currentValue("${prefix}ConditionAltIcon") ?: ""
		if (!iconUrl && sourceMap && sourceMap.weather && sourceMap.weather[0]) {
			Integer code = sourceMap.weather[0].id != null ? sourceMap.weather[0].id.toInteger() : 0
			String omiIcon = sourceMap.weather[0].icon ?: "01d"
			if (settings.altIconsEnable == true) {
				Map condDetails = lookupConditionDetails(code)
				BigDecimal currentAlt = state.currentSunAltitude != null ? state.currentSunAltitude.toBigDecimal() : (device.currentValue("currentSunAltitude")?.toBigDecimal() ?: 0.0)
				boolean isDay = (prefix == "today") ? (currentAlt >= -0.833) : true
				String iconFilename = isDay ? condDetails.altDay : condDetails.altNight
				iconUrl = "${basePath}${iconFilename}"
			} else {
				iconUrl = "${basePath}${omiIcon}.png"
			}
		}
 		String desc = sourceMap.weather ? sourceMap.weather[0].description : (device.currentValue("${prefix}ConditionTypeDesc") ?: "--")
    
		if (desc && desc != "--") {
			desc = desc.replace(" ", "<br>")
		}

		def fHi = sourceMap.temp?.max != null ? convertKelvin(sourceMap.temp.max) : (device.currentValue("${prefix}TempMax") ?: "--")
		def fLo = sourceMap.temp?.min != null ? convertKelvin(sourceMap.temp.min) : (device.currentValue("${prefix}TempMin") ?: "--")
		def pop = sourceMap.pop != null ? sourceMap.pop : (device.currentValue("${prefix}POP") ?: 0)
		def rain = sourceMap.rain != null ? sourceMap.rain : 0.0
		def snow = sourceMap.snow != null ? sourceMap.snow : 0.0
		def precip = (rain.toBigDecimal() > 0) ? convertPrecip(rain) : convertPrecip(snow)

		String sHi = (fHi instanceof BigDecimal) ? fHi.setScale(0, java.math.RoundingMode.HALF_UP) : fHi
		String sLo = (fLo instanceof BigDecimal) ? fLo.setScale(0, java.math.RoundingMode.HALF_UP) : fLo
		int popPct = (pop.toBigDecimal() * 100).intValue()
		String precipStr = (precip != null && precip.toBigDecimal() > 0) ? "${precip}${pUnit}" : ""

		return [icon: iconUrl, desc: desc, hi: sHi, lo: sLo, pop: popPct, precip: precipStr]
	}

	def d0 = getFcstData("today", todayData)
	def d1 = getFcstData("tom", tomData)
	def d2 = getFcstData("tda", tdaData)

	String forecastBodyHtml = "<style>.fcTable{width:100%;height:100%;text-align:center;color:#fff;font-size:.55rem;border-collapse:collapse;line-height:1}.fcTable td{padding:1px;vertical-align:middle}.fcTable img{height:3.2em;vertical-align:middle}.fcD{font-size:.48rem}.fcT{font-size:.7rem;font-weight:bold}</style>" +
    "<table class='fcTable'>" +
    "<tr><td colspan='4'><b>${cityName}</b></td></tr>" +
    "<tr><td>Now</td><td>Today</td><td>${day1Name}</td><td>${day2Name}</td></tr>" +
    "<tr><td><img src='${cIcon}'></td><td><img src='${d0.icon}'></td><td><img src='${d1.icon}'></td><td><img src='${d2.icon}'></td></tr>" +
    "<tr><td>Currently<br><span class='fcT'>${sT}°</span></td><td class='fcD'>${d0.desc}</td><td class='fcD'>${d1.desc}</td><td class='fcD'>${d2.desc}</td></tr>" +
    "<tr><td>Hi/Lo</td><td><b>${d0.hi}°/${d0.lo}°</b></td><td><b>${d1.hi}°/${d1.lo}°</b></td><td><b>${d2.hi}°/${d2.lo}°</b></td></tr>" +
    "<tr><td>Chance of Rain</td><td>💧${d0.pop}%${d0.precip ? ' ' + d0.precip : ''}</td><td>💧${d1.pop}%${d1.precip ? ' ' + d1.precip : ''}</td><td>💧${d2.pop}%${d2.precip ? ' ' + d2.precip : ''}</td></tr>" +
    "</table>"

	sendIfChanged(name: "3DayForecastTile", value: appendTileDebug(forecastBodyHtml))

	// SECTION 3: currentAlertTile
    String alertActive = device.currentValue("currentAlert") ?: "No active alerts"
    String alertSender = device.currentValue("currentAlertSender") ?: "N/A"
    String alertDescFull = device.currentValue("currentAlertDescFull") ?: "No active alerts"
    String lastPollTime = new Date().format("HH:mm", location.timeZone)

    String containerStart = "<div style='display:flex;flex-direction:column;justify-content:space-between;height:100%;box-sizing:border-box;overflow:hidden;padding:6px 4px;border-radius:6px;color:#fff;line-height:1.2;text-align:center'>"
    String alertBodyHtml = ""

    if (alertActive != "No active alerts" && alertActive != "N/A" && alertActive != "") {
        String headerHtml = "<div style='font-size:.85em;color:#ff5555;padding-top:10px;margin-bottom:6px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;'><b>⚠️ ${alertActive}</b></div><div style='font-size:.5em;color:#ddd;padding:2px 0;text-align:left;flex-grow:1;overflow:hidden;'>"
        String footerHtml = "</div><div style='display:flex;justify-content:space-between;border-top:1px solid rgba(255,255,255,0.15);padding-top:4px;font-size:.6em;color:#bbb'><span>👤 ${alertSender.take(12)}</span><span>⏰ ${lastPollTime}</span></div></div>"
        
        int debugBadgeLen = (settings.debugTileEnable == true) ? 143 : 0
        int overheadLen = containerStart.length() + headerHtml.length() + footerHtml.length() + debugBadgeLen
        int maxDescLen = 1023 - overheadLen
        
        String truncatedDesc = alertDescFull
        if (maxDescLen > 3 && alertDescFull.length() > maxDescLen) {
            truncatedDesc = alertDescFull.take(maxDescLen - 3) + "..."
        } else if (maxDescLen <= 3) {
            truncatedDesc = alertDescFull.take(Math.max(0, maxDescLen))
        }

        alertBodyHtml = "${containerStart}${headerHtml}${truncatedDesc}${footerHtml}"
    } else {
        alertBodyHtml = "${containerStart}<div style='font-size:0.85em;font-weight:bold;color:#5f5;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:2px;'>✅ ${cityName}<br>No Alerts</div><div style='font-size:0.8em;color:#aaa;padding:4px 4px;'>No active weather alerts from OpenWeatherMap or their sources</div><div style='display:flex;justify-content:center;border-top:1px solid rgba(255,255,255,0.15);padding-top:4px;font-size:.6em;color:#bbb'><span>Updated ${lastPollTime}</span></div></div>"
    }

    sendIfChanged(name: "currentAlertTile", value: appendTileDebug(alertBodyHtml))

    // SECTION 4: currentMoonPhaseTile
    Map moonVals = calcMoonPhaseValue(todayData, tomData, tdaData)
    String liveSvg = calcMoonPhaseSvgImage(todayData, tomData, tdaData)

    String moonPhaseText = moonVals.text ?: (device.currentValue("todayMoonPhaseText") ?: "Unknown")
    String moonSvg       = liveSvg ?: (device.currentValue("todayMoonPhaseSvgImage") ?: "")
    String moonPngUrl    = moonVals.png ?: (device.currentValue("todayMoonPhasePngImageUrl") ?: "")

    String graphicDisplay = ""
    if (settings.displayTileMoonPhaseSVGEnable == true && moonSvg != "") {
        graphicDisplay = moonSvg
    } else if (moonPngUrl != "") {
        graphicDisplay = "<img src='${moonPngUrl}' style='width:100%;height:100%;object-fit:contain;'>"
    } else {
        String moonEmoji = moonVals.emoji ?: (device.currentValue("todayMoonPhaseEmojiIcon") ?: "🌑")
        graphicDisplay = "<div style='font-size:min(12vw,12vh,4.5em);line-height:1;'>${moonEmoji}</div>"
    }

    String moonBodyHtml = "<div style='display:flex;justify-content:center;align-items:center;height:100%;width:100%;text-align:center;position:relative;overflow:hidden;'>" +
                          graphicDisplay +
                          "<div style='position:absolute; bottom:38%; width:92%; box-sizing:border-box; font-size:clamp(.6rem, 6vw, .8rem); font-weight:bold; text-shadow:1px 1px 3px #000; line-height:1.1; word-wrap:break-word; text-align:center; color:#fff;'>${moonPhaseText}</div>" +
                          "</div>"
    sendIfChanged(name: "currentMoonPhaseTile", value: appendTileDebug(moonBodyHtml))
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
    
    String apiUrl = ""
    switch(version) {
        case "2.5":
            apiUrl = "https://api.openweathermap.org/data/2.5/onecall?lat=${lat}&lon=${lon}&exclude=minutely,hourly&appid=${apiKey}"
            break
        case "3.0":
        case "4.0":
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
                state.lastResponseCode = response.status.toString()
                
                long currentUnixEpochSeconds = now() / 1000L
                state.lastUpdatedDateTime = currentUnixEpochSeconds
    
                String chosenFormat = settings.DateTimeForm ?: "1"
                if (chosenFormat == "10") {
                    state.lastUpdatedDateTimeText = "${currentUnixEpochSeconds}"
                } else {
                    Map formattedMap = convertDateTimeFormat(currentUnixEpochSeconds, chosenFormat)
                    state.lastUpdatedDateTimeText = formattedMap.dateTime
                }
                parseOWMData(response.data)
            } else {
                logError "OWM API call failed with status code: ${response.status}"
                state.lastResponseCode = response.status.toString()
            }
        }
    } catch (Exception e) {
        logError "OWM API request failed: ${e.message}"
        if (e.hasProperty("statusCode")) {
            state.lastResponseCode = "${e.statusCode}"
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
    
    BigDecimal currentAlt = calcSunPosition()
    calcBetwixtState(currentAlt, liveSunrise, liveSunset)
    calcIsDayState(currentAlt)
    calcCurrentTwilight()
    
    def dailyList = json.daily ?: []
    def data0 = dailyList.size() > 0 ? dailyList[0] : [:]
    def data1 = dailyList.size() > 1 ? dailyList[1] : [:]
    def data2 = dailyList.size() > 2 ? dailyList[2] : [:]
    
    if (data0 && data0.dt != null) sendIfChanged(name: "todayDate", value: data0.dt)
    if (data1 && data1.dt != null) sendIfChanged(name: "tomDate", value: data1.dt)
    if (data2 && data2.dt != null) sendIfChanged(name: "tdaDate", value: data2.dt)
    
    calcAlertsState(json, calculatedCityAttr, iconBasePath)

    sendOWMData(currentData, data0, data1, data2)
    
    if (liveSunrise > 0 && liveSunset > 0) {
        updateDynamicSchedules(liveSunrise, liveSunset)
    }
	
    calcMoonPhaseValue(data0, data1, data2)
    calcMoonPhaseSvgImage(data0, data1, data2)

    if (state.isInitializing == true) {
        state.isInitializing = false
        logDebug "Driver is initializing. Scheduling secondary out-of-band text refresh to prevent DB race conditions."
        
        def liveClouds = currentData?.clouds != null ? currentData.clouds : null
        
        BigDecimal freshLux = calcCurrentIlluminance(currentAlt, liveClouds)
        BigDecimal freshTemp = currentData?.temp != null ? convertKelvin(currentData.temp) : null
        BigDecimal freshPress = currentData?.pressure != null ? convertPressure(currentData.pressure) : null
        BigDecimal freshWind = currentData?.wind_speed != null ? convertWindSpeed(currentData.wind_speed) : null
        BigDecimal freshHum = currentData?.humidity != null ? convertHumidity(currentData.humidity) : null

        runIn(2, "scheduledTextValue", [data: [freshLux, freshTemp, freshPress, freshWind, freshHum, currentData, data0, data1, data2, json]])
    } else {
        generateTiles(currentData, data0, data1, data2)
    }
}

private void sendOWMData(Map current, Map today, Map tom, Map tda) {
    logDebug "sendOWMData initiated. Dispatching events to device attributes..."
    logTrace "sendOWMData Current section starting"

    // 1. CURRENT DATA DISPATCHES
    if (current) {
        def rawRain = current.calculatedRain ?: 0.00
        def rawSnow = current.calculatedSnow ?: 0.00
        
        def rainVal = convertPrecip(rawRain)
        def snowVal = convertPrecip(rawSnow)
        
        String preUnit = (settings.precipUnit == "none" || settings.precipUnit == null) ? "" : "${settings.precipUnit}"
        String spaceStr = (preUnit == '"') ? "" : " "

        sendIfChanged(name: "currentRain", value: rainVal)
        sendIfChanged(name: "currentSnow", value: snowVal)
        sendIfChanged(name: "currentRainText", value: "${rainVal}${spaceStr}${preUnit}")
        sendIfChanged(name: "currentSnowText", value: "${snowVal}${spaceStr}${preUnit}")
        
        if (current.sunrise != null) sendIfChanged(name: "currentSunriseTime", value: current.sunrise)
        if (current.sunset  != null) sendIfChanged(name: "currentSunsetTime",  value: current.sunset)
    
        if (current.temp != null) {
            BigDecimal calcTemp = convertKelvin(current.temp)
            sendIfChanged(name: "currentTemperature", value: calcTemp)
            sendIfChanged(name: "temperature", value: calcTemp)
        }
        if (current.feels_like != null) sendIfChanged(name: "currentFeelsLike", value: convertKelvin(current.feels_like))
        if (current.dew_point != null) sendIfChanged(name: "currentDewPoint", value: convertKelvin(current.dew_point))
        if (current.humidity != null) {
            BigDecimal calcHumidity = convertHumidity(current.humidity)
            sendIfChanged(name: "currentHumidity", value: calcHumidity)
            sendIfChanged(name: "humidity", value: calcHumidity)
        }
        if (current.pressure != null) {
            BigDecimal calcPressure = convertPressure(current.pressure)
            sendIfChanged(name: "currentPressure", value: calcPressure)
            sendIfChanged(name: "pressure", value: calcPressure)
        }
        if (current.uvi != null) {
            sendIfChanged(name: "currentUVI", value: current.uvi)
            sendIfChanged(name: "ultravioletIndex", value: current.uvi)
        }
        if (current.visibility != null) {
            BigDecimal visDist = convertVisibilityDistance(current.visibility)
            sendIfChanged(name: "currentVisibility", value: visDist)
            sendIfChanged(name: "currentVisibilityText", value: "${visDist} ${visibilityUnit}")
        }
        if (current.clouds != null) sendIfChanged(name: "currentCloudPCT", value: current.clouds)
               
        if (current.wind_speed != null) sendIfChanged(name: "currentWindSpeed", value: convertWindSpeed(current.wind_speed))
        if (current.wind_deg != null) sendIfChanged(name: "currentWindDeg", value: current.wind_deg)
        if (current.wind_gust != null) sendIfChanged(name: "currentWindGust", value: convertWindSpeed(current.wind_gust))
		if (current.wind_deg != null) {
            Map wDir = convertWindDirectionState(current.wind_deg)
            sendIfChanged(name: "currentWindDirCardinal", value: wDir.cardinal)
            sendIfChanged(name: "currentWindDirFull", value: wDir.full)
            sendIfChanged(name: "currentWindDirImageUrl", value: wDir.iconUrl)
            sendIfChanged(name: "currentWindDirectionImage", value: wDir.icon)
            sendIfChanged(name: "currentWindDirectionEmojiIcon", value: wDir.emoji)
        }
        
        if (current.weather && current.weather[0]) {
            Integer code = current.weather[0].id != null ? current.weather[0].id.toInteger() : 0
            String omiIcon = current.weather[0].icon ?: "01d"

            sendIfChanged(name: "currentConditionCode", value: code)
            sendIfChanged(name: "currentConditionType", value: current.weather[0].main)
            sendIfChanged(name: "currentConditionTypeDesc", value: current.weather[0].description)
            sendIfChanged(name: "currentConditionIcon", value: omiIcon)
            
            Map condDetails = lookupConditionDetails(code)
            sendIfChanged(name: "currentConditionTypeAltDesc", value: condDetails.desc)

            String basePath = state.iconBasePath ?: calcIconBasePath(settings.altIconLoc)
            String finalIconUrl = ""

            if (settings.altIconsEnable == true) {
                BigDecimal currentAlt = state.currentSunAltitude != null ? state.currentSunAltitude.toBigDecimal() : (device.currentValue("currentSunAltitude")?.toBigDecimal() ?: 0.0)
                boolean isDay = (currentAlt >= -0.833)
                String iconFilename = isDay ? condDetails.altDay : condDetails.altNight
                finalIconUrl = "${basePath}${iconFilename}"
                logDebug "Alternate Icons Enabled. Day: ${isDay} | File: ${iconFilename} | URL: ${finalIconUrl}"
            } else {
                finalIconUrl = "${basePath}${omiIcon}.png"
                logDebug "Standard OpenWeatherMap Icon Used | URL: ${finalIconUrl}"
            }
            
            sendIfChanged(name: "currentConditionAltIcon", value: finalIconUrl)
        }
        logTrace "sendOWMData Current section ended"
    }

    // 2. FORECAST DISPATCHES
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
            
			if (data.weather && data.weather[0]) {
				Integer code = data.weather[0].id != null ? data.weather[0].id.toInteger() : 0
				String omiIcon = data.weather[0].icon ?: "01d"

				sendIfChanged(name: "${prefix}ConditionCode", value: code)
				sendIfChanged(name: "${prefix}ConditionType", value: data.weather[0].main)
				sendIfChanged(name: "${prefix}ConditionTypeDesc", value: data.weather[0].description)
				sendIfChanged(name: "${prefix}ConditionIcon", value: omiIcon)

				Map condDetails = lookupConditionDetails(code)
				sendIfChanged(name: "${prefix}ConditionTypeAltDesc", value: condDetails.desc)

				String basePath = state.iconBasePath ?: calcIconBasePath(settings.altIconLoc)
				String finalIconUrl = ""

				if (settings.altIconsEnable == true) {
					finalIconUrl = "${basePath}${condDetails.altDay}"
				} else {
					finalIconUrl = "${basePath}${omiIcon}.png"
				}
				sendIfChanged(name: "${prefix}ConditionAltIcon", value: finalIconUrl)
			}

            if (data.temp) {
                ["min", "max", "day", "night", "eve", "morn"].each { tKey ->
                    if (data.temp[tKey] != null) {
                        sendIfChanged(name: "${prefix}Temp${tKey.capitalize()}", value: convertKelvin(data.temp[tKey]))
                    }
                }
            }

            if (data.feels_like) {
                ["day", "night", "eve", "morn"].each { fKey ->
                    if (data.feels_like[fKey] != null) {
                        sendIfChanged(name: "${prefix}FeelsLike${fKey.capitalize()}", value: convertKelvin(data.feels_like[fKey]))
                    }
                }
            }

            if (data.pressure != null)   sendIfChanged(name: "${prefix}Pressure", value: convertPressure(data.pressure))
            if (data.humidity != null)   sendIfChanged(name: "${prefix}Humidity", value: convertHumidity(data.humidity))
            if (data.dew_point != null)  sendIfChanged(name: "${prefix}DewPoint", value: convertKelvin(data.dew_point))
            if (data.uvi != null)        sendIfChanged(name: "${prefix}UVI", value: data.uvi)
            if (data.clouds != null)     sendIfChanged(name: "${prefix}CloudPCT", value: data.clouds)
            if (data.wind_gust != null)  sendIfChanged(name: "${prefix}WindGust", value: convertWindSpeed(data.wind_gust))
            if (data.wind_deg != null)   sendIfChanged(name: "${prefix}WindDeg", value: data.wind_deg)
            if (data.wind_speed != null) sendIfChanged(name: "${prefix}WindSpeed", value: convertWindSpeed(data.wind_speed))

			if (data.wind_deg != null) {
                Map wDir = convertWindDirectionState(data.wind_deg)
                sendIfChanged(name: "${prefix}WindDirCardinal", value: wDir.cardinal)
                sendIfChanged(name: "${prefix}WindDirFull", value: wDir.full)
                sendIfChanged(name: "${prefix}WindDirImageUrl", value: wDir.iconUrl)
                sendIfChanged(name: "${prefix}WindDirectionImage", value: wDir.icon)
                sendIfChanged(name: "${prefix}WindDirectionEmojiIcon", value: wDir.emoji)
            }
            logTrace "sendOWMData ${prefix} section ended"
        }
    }

    // 3. ILLUMINANCE & OUT-OF-BAND MATH EXECUTION
    BigDecimal currentAlt = state.currentSunAltitude != null ? state.currentSunAltitude.toBigDecimal() : (device.currentValue("currentSunAltitude")?.toBigDecimal() ?: 0.0)
    
    def liveClouds = current?.clouds != null ? current.clouds : null
    BigDecimal freshLux = calcCurrentIlluminance(currentAlt, liveClouds)
    
    BigDecimal freshTemp = current?.temp != null ? convertKelvin(current.temp) : null
    BigDecimal freshPress = current?.pressure != null ? convertPressure(current.pressure) : null
    BigDecimal freshWind = current?.wind_speed != null ? convertWindSpeed(current.wind_speed) : null
    BigDecimal freshHum = current?.humidity != null ? convertHumidity(current.humidity) : null

    calcTextValue(freshLux, freshTemp, freshPress, freshWind, freshHum, current, today, tom, tda)
    calcMoonPhaseValue(today, tom, tda)
    calcMoonPhaseSvgImage(today, tom, tda)
    
    logDebug "sendOWMData event parsing complete."
}

// -----------------CONVERTERS

private Map convertDateTimeFormat(def epochSeconds, String formatOption) {
    if (epochSeconds == null) {
        logDebug "convertDateTimeFormat received a null epoch timestamp value."
        return [dateTime: "--", date: "--", time: "--"]
    }
    
    long msecs = epochSeconds.toLong() * 1000L
    Date dateObject = new Date(msecs)
    
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
    int precision = 1
    
    switch (targetUnit) {
        case "km":
            converted = meters / 1000.0
            precision = 1
            break
        case "miles":
            converted = meters / 1609.344
            precision = 2
            break
        case "ft":
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
    
    String targetUnit = settings.illuminanceUnit ?: "lx"
    BigDecimal convertedValue = rawLux
    int precision = 0
    
    switch (targetUnit) {
        case "fc":
            convertedValue = rawLux * 0.092903
            precision = 1
            break
        case "ph":
            convertedValue = rawLux * 0.0001
            precision = 4
            break
        case "lx":
        case "none":
        default:
            convertedValue = rawLux
            precision = 0
            break
    }
    
    BigDecimal finalValue = convertedValue.setScale(precision, java.math.RoundingMode.HALF_UP)
    return finalValue < 0 ? 0.0 : finalValue
}

private BigDecimal convertHumidity(BigDecimal rawHumidity) {
    if (rawHumidity == null || rawHumidity == "") {
        int precision = settings.precisionHumid != null ? settings.precisionHumid.toInteger() : 0
        return new BigDecimal("0.0").setScale(precision, java.math.RoundingMode.HALF_UP)
    }

    String hUnit = settings.humidityUnit ?: "%"
    Double calculatedValue = rawHumidity.toDouble()

    switch (hUnit) {
        case "%":
        case "%RH":
            break
        case "g/m³":
            def tempVal = device.currentValue("currentTemperature")
            if (tempVal != null && tempVal.toString().isNumber()) {
				Double tempC = tempVal.toDouble()
                String tUnit = settings.temperatureUnit ?: "°F"
                if (tUnit == "°F") { tempC = (tempC - 32.0) * 5.0 / 9.0 }
                else if (tUnit == "K") { tempC = tempC - 273.15 }

                Double es = 6.112 * Math.exp((17.67 * tempC) / (tempC + 243.5))
                Double e = (calculatedValue / 100.0) * es
                calculatedValue = (e * 216.7) / (tempC + 273.15)
            }
            break
        case "g/kg³":
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
    
    String targetUnit = settings.temperatureUnit ?: "°F"

    switch (targetUnit) {
        case "°F":
            converted = (K - 273.15) * 1.8 + 32
            break
        case "°C":
            converted = K - 273.15
            break
        case "K":
        case "none":
        default:
            break
    }
    
    int precision = (settings.precisionTemp ?: "2").toInteger()
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertPrecip(precipVal) {
    if (precipVal == null || precipVal == "") {
        int precision = settings.precisionPrecip != null ? settings.precisionPrecip.toInteger() : 2
        return new BigDecimal("0.0").setScale(precision, java.math.RoundingMode.HALF_UP)
    }
    
    BigDecimal precip = (precipVal instanceof BigDecimal) ? precipVal : new BigDecimal(precipVal.toString())
    
    String unit = settings.precipUnit ?: "inHr"
    if (unit in ["inHr", "in", "\""]) {
        precip = precip * 0.0393701
    }
    
    int precision = settings.precisionPrecip != null ? settings.precisionPrecip.toInteger() : 2
    return precip.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertPressure(def hpaVal) {
    BigDecimal hpa = hpaVal?.toBigDecimal()
    if (hpa == null) return 0.0
    BigDecimal converted = hpa
    
    String targetUnit = settings.pressureUnit ?: "inHg"
    
    switch (targetUnit) {
        case "inHg":
            converted = hpa * 0.029530
            break
        case "mmHg":
            converted = hpa * 0.750062
            break
        case "kPa":
            converted = hpa * 0.1
            break
        case "hPa":
        case "mb":
        case "none":
        default:
            break
    }
    
    int precision = (settings.precisionPress ?: "2").toInteger()
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private BigDecimal convertWindSpeed(def msVal) {
    BigDecimal ms = msVal?.toBigDecimal()
    if (ms == null) return 0.0
    BigDecimal converted = ms
    
    String targetUnit = settings.windSpeedUnit ?: "mph"
    
    switch (targetUnit) {
        case "mph":
            converted = ms * 2.23694
            break
        case "kmh":
            converted = ms * 3.6
            break
        case "kt":
            converted = ms * 1.94384
            break
        case "ms":
        case "none":
        default:
            break
    }
    
    int precision = (settings.precisionWind ?: "2").toInteger()
    return converted.setScale(precision, java.math.RoundingMode.HALF_UP)
}

private Map convertWindDirectionState(degrees) {
    if (degrees == null) {
        logDebug "convertWindDirectionState received null value."
        return [cardinal: "Unknown", full: "Unknown", iconUrl: "", icon: "💨", emoji: "💨"]
    }
    double deg = 0.0
    try {
        deg = degrees.toDouble()
    } catch (Exception e) {
        logError "Failed to parse wind degrees (${degrees}): ${e.message}"
        return [cardinal: "Unknown", full: "Unknown", iconUrl: "", icon: "💨", emoji: "💨"]
    }

    deg = (deg % 360 + 360) % 360
    def cardinals = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"]
    def fullWords = ["North", "North-Northeast", "Northeast", "East-Northeast", "East", "East-Southeast", "Southeast", "South-Southeast", "South", "South-Southwest", "Southwest", "West-Southwest", "West", "West-Northwest", "Northwest", "North-Northwest"]
    def directionEmojis = ["⬇️", "↙️", "↙️", "↙️", "⬅️", "↖️", "↖️", "↖️", "⬆️", "↗️", "↗️", "↗️", "➡️", "↘️", "↘️", "↘️"]

    int index = (int) Math.round(deg / 22.5) % 16
    String token = cardinals[index]
    String word  = fullWords[index]
    String chosenEmoji = directionEmojis[index]
    
    String finalIconUrl = ""
    String finalIconDisplay = chosenEmoji

    String basePath = state.windDirectionImagePath ?: calcWinDirImagePath(settings.altWindDirectionImageLoc)

    if (basePath != null && basePath.trim() != "") {
        String filename = "wind-${token.toLowerCase()}.png"
        finalIconUrl = "${basePath}${filename}"
        finalIconDisplay = "<img src='${finalIconUrl}' style='height:1em;vertical-align:middle;'>"
    }
    
    return [
        cardinal: token, 
        full: word, 
        iconUrl: finalIconUrl, 
        icon: finalIconDisplay, 
        emoji: chosenEmoji
    ]
}

// ----------------------- CALCS

private String calcIconBasePath(String altIconLoc) {
    String base = ""
    
    if (settings.altIconsEnable == true) {
        base = (altIconLoc && altIconLoc.trim() != "") ? altIconLoc.trim() : "https://tinyurl.com/icnqz/"
    } else {
        base = "https://openweathermap.org/img/wn/"
    }
    
    if (!base.endsWith("/")) base += "/"
    logDebug "Calculated Icon Base Path resolved to: ${base}"
    return base
}

private String calcMoonPhaseImagePath(String altMPLoc) {
    String base = altMPLoc ? altMPLoc.trim() : "https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/"
    if (!base.endsWith("/")) base += "/"
    logDebug "Calculated Moon Phase Image base path resolved to: ${base}"
    return base
}

private String calcWinDirImagePath(String altWDLoc) {
    String base = (altWDLoc && altWDLoc.trim() != "") ? altWDLoc.trim() : ""
    
    if (base != "" && !base.endsWith("/")) {
        base += "/"
    }
    
    logDebug "Calculated Wind Direction Image base path resolved to: '${base}'"
    return base
}

Map calcMoonPhaseValue(Map todayData = [:], Map tomData = [:], Map tdaData = [:]) {
    logDebug "Calculating moon phase icons, text names, and emojis from API payload maps..."
    
    List<String> emojis = ["🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"]
    
    def phases = [
        [sourceMap: todayData, apiKey: "moon_phase", valAttr: "todayMoonPhase", pngAttr: "todayMoonPhasePngImageUrl", textAttr: "todayMoonPhaseText", emojiAttr: "todayMoonPhaseEmojiIcon"],
        [sourceMap: tomData,   apiKey: "moon_phase", valAttr: "tomMoonPhase",   pngAttr: "tomMoonPhasePngImageUrl",   textAttr: "tomMoonPhaseText",   emojiAttr: "tomMoonPhaseEmojiIcon"],
        [sourceMap: tdaData,   apiKey: "moon_phase", valAttr: "tdaMoonPhase",   pngAttr: "tdaMoonPhasePngImageUrl",   textAttr: "tdaMoonPhaseText",   emojiAttr: "tdaMoonPhaseEmojiIcon"]
    ]
    
    def todayVal = null
    boolean isPathOverridden = (settings.altMoonPhaseImagePath != null && settings.altMoonPhaseImagePath.trim() != "")
    Map resultMap = [:]

    phases.each { phase ->
        def rawVal = (phase.sourceMap && phase.sourceMap[phase.apiKey] != null) ? phase.sourceMap[phase.apiKey] : device.currentValue(phase.valAttr)
        
        if (rawVal != null && rawVal.toString().isNumber()) {
            double val = (rawVal.toDouble() % 1.0 + 1.0) % 1.0
            
            if (phase.valAttr == "todayMoonPhase") {
                todayVal = val
            }

            int index = (int) Math.floor((val * 8) + 0.5) % 8
            String filename = isPathOverridden ? "mp${index}.png" : "moon-phase-icon-${index}.png"
            
            String basePath = state.moonPhaseImagePath ?: "https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/"
            String fullPath = "${basePath}${filename}"
            
            sendIfChanged(name: phase.pngAttr, value: fullPath)

            int emojiIndex = (int) Math.floor((val * 8) + 0.5) % 8
            String chosenEmoji = emojis[emojiIndex]
            sendIfChanged(name: phase.emojiAttr, value: chosenEmoji)

            String phaseName = "Unknown"
            double tolerance = 0.02
            if (val <= tolerance || val >= (1.0 - tolerance)) phaseName = "New Moon"
            else if (val > tolerance && val < (0.25 - tolerance)) phaseName = "Waxing Crescent"
            else if (val >= (0.25 - tolerance) && val <= (0.25 + tolerance)) phaseName = "First Quarter"
            else if (val > (0.25 + tolerance) && val < (0.5 - tolerance)) phaseName = "Waxing Gibbous"
            else if (val >= (0.5 - tolerance) && val <= (0.5 + tolerance)) phaseName = "Full Moon"
            else if (val > (0.5 + tolerance) && val < (0.75 - tolerance)) phaseName = "Waning Gibbous"
            else if (val >= (0.75 - tolerance) && val <= (0.75 + tolerance)) phaseName = "Last Quarter"
            else if (val > (0.75 + tolerance) && val < (1.0 - tolerance)) phaseName = "Waning Crescent"
            
            sendIfChanged(name: phase.textAttr, value: phaseName)

            if (phase.valAttr == "todayMoonPhase") {
                resultMap.text = phaseName
                resultMap.png = fullPath
                resultMap.emoji = chosenEmoji
            }
        }
    }
    return resultMap
}

String calcMoonPhaseSvgMask(double phase, String path) {
    if (phase <= 0.02 || phase >= 0.98) {
        return """<svg viewBox="0 0 256 256" style="width:100%;height:100%;display:block;"><circle cx="128" cy="128" r="127" fill="#121214"/></svg>"""
    }
    double rx1 = 127.0, rx2 = 127.0
    int sf1 = 1, sf2 = 1
    if (phase <= 0.25) { rx1 *= (1 - 4 * phase) }
    else if (phase <= 0.50) { rx1 *= (4 * phase - 1); sf1 = 0 }
    else if (phase <= 0.75) { rx2 *= (3 - 4 * phase); sf2 = 0 }
    else { rx2 *= (4 * phase - 3) }

    return """<svg viewBox="0 0 256 256" style="width:100%;height:100%;display:block;"><filter id="b"><feGaussianBlur stdDeviation="6"/></filter><mask id="a"><path d="M128,1A${rx1.round(1)},127 180 0 $sf1 128,255A${rx2.round(1)},127 180 0 $sf2 128,1z" fill="#fff" filter="url(#b)"/></mask><radialGradient id="s"><stop offset="10%" stop-color="#0007"/><stop offset="90%" stop-color="#000d"/></radialGradient><image width="256" height="256" href="${path}lunar_surface.png"/><circle cx="128" cy="128" r="127" mask="url(#a)" fill="url(#s)"/></svg>"""
}

String calcMoonPhaseSvgImage(Map todayData = [:], Map tomData = [:], Map tdaData = [:]) {
    logDebug "Evaluating moon phase calculations for SVG output variables..."
    
    String path = state.moonPhaseImagePath ?: "https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/"
    String todaySvg = ""

    def targets = [
        [sourceMap: todayData, apiKey: "moon_phase", valAttr: "todayMoonPhase", targetAttr: "todayMoonPhaseSvgImage"],
        [sourceMap: tomData,   apiKey: "moon_phase", valAttr: "tomMoonPhase",   targetAttr: "tomMoonPhaseSvgImage"],
        [sourceMap: tdaData,   apiKey: "moon_phase", valAttr: "tdaMoonPhase",   targetAttr: "tdaMoonPhaseSvgImage"]
    ]
    
    targets.each { entry ->
        def rawVal = (entry.sourceMap && entry.sourceMap[entry.apiKey] != null) ? entry.sourceMap[entry.apiKey] : device.currentValue(entry.valAttr)
        if (rawVal != null && rawVal.toString().isNumber()) {
            double val = (rawVal.toDouble() % 1.0 + 1.0) % 1.0
            String svgContent = calcMoonPhaseSvgMask(val, path)
            sendIfChanged(name: entry.targetAttr, value: svgContent)

            if (entry.targetAttr == "todayMoonPhaseSvgImage") {
                todaySvg = svgContent
            }
        }
    }
    return todaySvg
}

private void calcAlertsState(Map json, String calculatedCityAttr, String iconBasePath) {
    def alerts = json?.alerts ?: []
    String alertActive = "No active alerts"
    String currentAlertSender = "N/A"
    String currentAlertDesc = "No active alerts"
    String lastPollTime = new Date().format("HH:mm", location.timeZone)
    String currentAlertDescFull = "No active alerts for ${calculatedCityAttr} at last poll as of ${lastPollTime}"

    if (alerts.size() > 0) {
        def a = alerts[0]
        alertActive = a.event ?: "Active Alert"
        currentAlertSender = a.sender_name ?: "Unknown"
		currentAlertDesc = a.description ? (a.description.length() > 100 ? a.description.take(100) + "..." : a.description) : "Alert details unavailable"
        currentAlertDescFull = (a.description ?: "N/A") + " as of ${lastPollTime}"
    }

    sendIfChanged(name: "currentAlert", value: alertActive)
    sendIfChanged(name: "currentAlertSender", value: currentAlertSender)
    sendIfChanged(name: "currentAlertDesc", value: currentAlertDesc)
    sendIfChanged(name: "currentAlertDescFull", value: currentAlertDescFull)
}

private BigDecimal calcSunPosition() {
    int precision = (settings.precisionSunMoonAngles ?: "0").toInteger()
    BigDecimal locLat = state.usedLatitude != null ? state.usedLatitude.toBigDecimal() : location.latitude
    BigDecimal locLon = state.usedLongitude != null ? state.usedLongitude.toBigDecimal() : location.longitude

    if (locLat == null || locLon == null) {
        logWarn "calcSunPosition: Latitude or Longitude coordinates are unavailable."
        return 0.0
    }

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    double hour = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0) + (cal.get(Calendar.SECOND) / 3600.0)
    int day = cal.get(Calendar.DAY_OF_MONTH)
    int month = cal.get(Calendar.MONTH) + 1
    int year = cal.get(Calendar.YEAR)

    if (month <= 2) {
        year -= 1
        month += 12
    }
    int A = (int)(year / 100)
    int B = 2 - A + (int)(A / 4)
    double jd = (int)(365.25 * (year + 4716)) + (int)(30.6001 * (month + 1)) + day + (hour / 24.0) + B - 1524.5
    double d = jd - 2451545.0

    double g = 357.529 + 0.98560028 * d
    double q = 280.459 + 0.98564736 * d
    double L = q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g))
    double e = 23.439 - 0.00000036 * d

    double sin_delta = Math.sin(Math.toRadians(e)) * Math.sin(Math.toRadians(L))
    double delta = Math.toDegrees(Math.asin(sin_delta))
    double ra = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(e)) * Math.sin(Math.toRadians(L)), Math.cos(Math.toRadians(L))))

    double gst = 280.46061837 + 360.98564736629 * d
    double lst = gst + locLon
    double H = lst - ra

    double latRad = Math.toRadians(locLat)
    double deltaRad = Math.toRadians(delta)
    double hRad = Math.toRadians(H)

    double sin_alt = Math.sin(latRad) * Math.sin(deltaRad) + Math.cos(latRad) * Math.cos(deltaRad) * Math.cos(hRad)
    sin_alt = Math.max(-1.0, Math.min(1.0, sin_alt))
    double alt = Math.toDegrees(Math.asin(sin_alt))

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

    az = (az % 360.0 + 360.0) % 360.0

    BigDecimal finalAltitude = BigDecimal.valueOf(alt).setScale(precision, java.math.RoundingMode.HALF_UP)
    BigDecimal finalAzimuth = BigDecimal.valueOf(az).setScale(precision, java.math.RoundingMode.HALF_UP)

    logTrace "calcSunPosition: Solar Altitude computed as ${finalAltitude}°, Azimuth as ${finalAzimuth}°"

    sendIfChanged(name: "currentSunAltitude", value: finalAltitude)
    sendIfChanged(name: "currentSunAzimuth", value: finalAzimuth)
    sendIfChanged(name: "currentSunAltitudeText", value: "${finalAltitude}°")
    sendIfChanged(name: "currentSunAzimuthText", value: "${finalAzimuth}°")

    state.currentSunAltitude = finalAltitude
    return finalAltitude
}

private void calcCurrentTwilight() {
    if (state.todaySunriseEpoch && state.todaySunsetEpoch) {
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
    
    long sunriseEpoch = (liveSunrise > 0) ? liveSunrise : (state.todaySunriseEpoch ?: 0)
    long sunsetEpoch = (liveSunset > 0) ? liveSunset : (state.todaySunsetEpoch ?: 0)
    
    boolean isTwilightAngle = (altitude >= -6.0 && altitude < -0.833)
    boolean isSunUp = (altitude >= -0.833)
    
    if (sunriseEpoch > 0 && sunsetEpoch > 0) {
        long midDayEpoch = sunriseEpoch + ((sunsetEpoch - sunriseEpoch) / 2)
        
        try {
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
    String isDayText = (altitude != null && altitude >= -0.833) ? "true" : "false"
    sendIfChanged(name: "currentIsDay", value: isDayText)
    logTrace "Calculated currentIsDay: ${isDayText}"
}

private BigDecimal calcCurrentIlluminance(BigDecimal altitude, def liveClouds = null) {
    logDebug "Calculating dynamic current illuminance adjusted for chosen unit..."
    
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

    // 3. Wind Speed Text Formatting
    def windTargets = [
        [prefix: "current", val: freshWind, map: currentMap],
        [prefix: "today",   val: null,      map: todayMap],
        [prefix: "tom",     val: null,      map: tomMap],
        [prefix: "tda",     val: null,      map: tdaMap]
    ]

    windTargets.each { target ->
        def speedVal = target.val
        if (speedVal == null) {
            speedVal = (target.map && target.map.wind_speed != null) ? convertWindSpeed(target.map.wind_speed) : device.currentValue("${target.prefix}WindSpeed")
        }
        if (speedVal != null) sendIfChanged(name: "${target.prefix}WindSpeedText", value: "${speedVal}${wUnit}")
        
        def rawSpeed = (target.map && target.map.wind_speed != null) ? target.map.wind_speed : device.currentValue("${target.prefix}WindSpeed")
        sendIfChanged(name: "${target.prefix}WindSpeedDescText", value: getBeaufortText(rawSpeed))
    }

    // 4. Illuminance Text Formatting
    def luxVal = (freshLux != null) ? freshLux : device.currentValue("currentIlluminance")
    if (luxVal != null) sendIfChanged(name: "currentIlluminanceText", value: "${luxVal} ${iUnit}")
    
    // 5. Solar Angles Formatting
    int sunPrecision = (settings.precisionSunMoonAngles ?: "0").toInteger()
    
    def altVal = device.currentValue("currentSunAltitude")
    if (altVal != null && altVal.toString().isNumber()) {
        BigDecimal formattedAlt = altVal.toBigDecimal().setScale(sunPrecision, java.math.RoundingMode.HALF_UP)
        String altStr = (sunPrecision == 0) ? "${formattedAlt.toBigInteger()}" : "${formattedAlt}"
        sendIfChanged(name: "currentSunAltitudeText", value: "${altStr}°")
    }
    
    def azVal = device.currentValue("currentSunAzimuth")
    if (azVal != null && azVal.toString().isNumber()) {
        BigDecimal formattedAz = azVal.toBigDecimal().setScale(sunPrecision, java.math.RoundingMode.HALF_UP)
        String azStr = (sunPrecision == 0) ? "${formattedAz.toBigInteger()}" : "${formattedAz}"
        sendIfChanged(name: "currentSunAzimuthText", value: "${azStr}°")
    }

    // 6. Humidity Formatting
    def humVal = (freshHum != null) ? freshHum : device.currentValue("currentHumidity")
    if (humVal != null) sendIfChanged(name: "currentHumidityText", value: "${humVal}${hUnit}")

    // DATE/TIME PROPERTIES CONSOLIDATED LOOP
    logDebug "Consolidated loop executing for date/time properties..."
    String chosenDTForm = settings.DateTimeForm ?: "1"
    String chosenDateForm = settings.DateForm ?: "1"
    String chosenTimeForm = settings.TimeForm ?: "1"

    device.properties.supportedAttributes.each { attr ->
        String attrName = attr.name
        def epochValue = null
        String formatSelection = "1"
        String textTargetSuffix = ""

        if (attrName.endsWith("DateTime")) {
            epochValue = device.currentValue(attrName)
            formatSelection = chosenDTForm
            textTargetSuffix = "Text"
        } else if (attrName.endsWith("Date")) {
            if (attrName == "todayDate" && todayMap?.dt != null) epochValue = todayMap.dt
            else if (attrName == "tomDate" && tomMap?.dt != null) epochValue = tomMap.dt
            else if (attrName == "tdaDate" && tdaMap?.dt != null) epochValue = tdaMap.dt
            else epochValue = device.currentValue(attrName)
            formatSelection = chosenDateForm
            textTargetSuffix = "Text"
        } else if (attrName.endsWith("Time") && !attrName.contains("DateTime")) {
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

    // Current Wind Summary
    logDebug "Section 11b: Formatting Current Wind Summary Text ..."
    try {
        def windValLocal = (freshWind != null) ? freshWind : device.currentValue("currentWindSpeed")
        String windSpeedText = (windValLocal != null) ? "${windValLocal}${wUnit}" : (device.currentValue("currentWindSpeedText") ?: "--")
        
        String currentWindSpeedDescText = device.currentValue("currentWindSpeedDescText") ?: "Calm"
        String windDirFull = device.currentValue("currentWindDirFull") ?: "Unknown"
        
        sendIfChanged(name: "currentWindSummaryText", value: "${currentWindSpeedDescText} from the ${windDirFull} at ${windSpeedText}")
    } catch (Exception e) {
        logError "Exception occurred during currentWindSummaryText generation: ${e.message}"
    }
    
    // Current Weather Summary
    logDebug "Section 12: Formatting Current Weather Summary Text ..."
    try {
        def city = state.usedCity ?: "Local Area"
        def lastUpdatedEpoch = state.lastUpdatedDateTime
        
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
        
        def temp = (freshTemp != null) ? freshTemp : device.currentValue("currentTemperature")
        def hi = (todayMap && todayMap.temp?.max != null) ? convertKelvin(todayMap.temp.max) : (device.currentValue("todayTempMax") ?: "--")
        def lo = (todayMap && todayMap.temp?.min != null) ? convertKelvin(todayMap.temp.min) : (device.currentValue("todayTempMin") ?: "--")
        def feels = device.currentValue("currentFeelsLike") ?: temp
        
        String tempText = (temp != null) ? "${temp}${tUnit}" : (device.currentValue("currentTemperatureText") ?: "--")
        
        humVal = (freshHum != null) ? freshHum : device.currentValue("currentHumidity")
        String humText = (humVal != null) ? "${humVal}%" : "--"
        
        def windRaw = (currentMap && currentMap.wind_speed != null) ? currentMap.wind_speed : device.currentValue("currentWindSpeed")
        String windStrengthText = getBeaufortText(windRaw)
        String windDirFull = device.currentValue("currentWindDirFull") ?: "Unknown"
        
        def windValLocal = (freshWind != null) ? freshWind : device.currentValue("currentWindSpeed")
        String windSpeedText = (windValLocal != null) ? "${windValLocal}${wUnit}" : (device.currentValue("currentWindSpeedText") ?: "--")
        
        def popRaw = (todayMap && todayMap.pop != null) ? todayMap.pop : (device.currentValue("todayPOP") ?: 0)
        int popPct = (popRaw.toBigDecimal() * 100).intValue()
        
        String visibilityText = device.currentValue("currentVisibilityText") ?: "--"
        String alertVal = device.currentValue("currentAlert") ?: "No active alerts"
        
        String alertSentence = (alertVal != "No active alerts" && alertVal != "N/A" && alertVal != "") ? "Active alerts for ${alertVal} exist for this area." : "No active alerts exist for this area."

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
    String currentCity = settings.overrideCity ?: ""
    BigDecimal currentLat = settings.overrideLatitude ? settings.overrideLatitude.toBigDecimal() : null
    BigDecimal currentLon = settings.overrideLongitude ? settings.overrideLongitude.toBigDecimal() : null

    Boolean settingsChanged = (currentCity != state.lastOverrideCity || currentLat != state.lastOverrideLatitude || currentLon != state.lastOverrideLongitude)
    Boolean hasCachedData = (state.usedCity && state.usedLatitude != null && state.usedLongitude != null)

    if (!settingsChanged && hasCachedData) {
        logDebug "Coordinates and city are unchanged and cached. Skipping geo lookup."
        return
    }

    String usedCity = ""
    BigDecimal usedLatitude = 0.0
    BigDecimal usedLongitude = 0.0

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
    } else {
        logDebug "Scenario 2: No overrideCity provided. Evaluating coordinate inputs or Hub configuration."
        usedLatitude = currentLat ?: location.latitude?.toBigDecimal()
        usedLongitude = currentLon ?: location.longitude?.toBigDecimal()
        
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

    if (!usedLatitude || usedLatitude == 0.0) {
        usedLatitude = currentLat ?: location.latitude?.toBigDecimal() ?: 0.0
    }
    if (!usedLongitude || usedLongitude == 0.0) {
        usedLongitude = currentLon ?: location.longitude?.toBigDecimal() ?: 0.0
    }
    if (!usedCity || usedCity.trim() == "") {
        usedCity = "Local Area"
    }

    state.usedCity = usedCity
    state.usedLatitude = usedLatitude
    state.usedLongitude = usedLongitude
    
    state.lastOverrideCity = currentCity
    state.lastOverrideLatitude = currentLat
    state.lastOverrideLongitude = currentLon
}

private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    
    String oldVal = device.currentValue(args.name as String)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
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