/*
 *
 * Original Code Model - Copyright 2024 C Steele
 *
 * Twilight Parser
 *
 * Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Change History:
 *
 * Date         Source      Version What                                        URL
 * ----         ------      ------- ----                                        ---
 * 2024-04-01   jshimota    0.1.0   Starting version
 * 2024-04-02   C Steele    0.1.1   Used 2024-04-02 Sample code as start point THANKS!  
 * 2024-04-03   jshimota    0.1.2   Added all variables from Sunrise - Sunset api
 * 2024-04-04   jshimota    0.1.3   Fixed default values of user variables
 * 2024-04-04   jshimota    0.1.4   Modification to handle parse of day length
 * 2024-04-13   jshimota    0.1.5   basic testing and some cleanup
 * 2024-04-14   jshimota    0.1.6   Prep for final use 
 * 2024-04-10   jshimota    0.1.7   added functions to connect to HE Global Variables
 * 2024-04-28   jshimota    0.1.8   implemented epoch - later proved unnecessary  - left it for now
 * 2024-04-28   jshimota    0.1.9   implemented hard connection to HE Globals and fixed logging
 * 2024-11-10   jshimota    0.2.0   Added formatted values to be used in custom tiles
 * 2024-12-01   jshimota    0.2.1   Changed SDF to lowercase for formatted dates
 * 2026-06-22   jshimota    0.2.2   Gemini recommendations for modernization and bug fixes
 * 2026-06-22   jshimota    0.2.3   Optimized schedule updates and fixed Quartz cron format
 * 2026-06-22   jshimota    0.2.4   Fixed UI text alignment and info/trace logging check bugs
 * 2026-06-22   jshimota    0.2.5   Added live current date fallback logic if override date is blank or old
 * 2026-07-18	jshimota	0.2.6	fixed odd string being stored incorrectly found with dev info driver
 * 2026-07-18	jshimota	0.2.7	bug hunt, problems with date storage payloads
 * 2026-07-18   jshimota    0.2.8   Refactored out state variables to local variables only
 * 2026-07-18   jshimota    0.2.9   Refactored all sendEvent calls to sendIfChanged
 * 2026-07-18   jshimota    0.3.0   Migrated logging statements to new dynamic logger routines
 * 2026-07-18   jshimota    0.3.1   Refactored redundant date parsing to use getTargetDate() helper
 * 2026-07-18   jshimota    0.3.2   Integrated formatTime and formatDate helpers into handler
 * 2026-07-18   jshimota    0.3.3-4 Minor tweaks
 * 2026-07-18   jshimota    0.3.5   Added JsonSlurper fallback parse and bulletproofed day_length casting
 * 2026-07-18	jshimota	0.3.6-7 Fixed the parser when it errors or comes back as string.
 *
 */

static String version() { return '0.3.7' }
import java.text.SimpleDateFormat
import java.util.Locale
import java.time.*
import groovy.json.JsonSlurper

metadata {
    definition(
        name: "Twilight Parser Driver",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/twilight_parser/twilightParser.groovy"
    ) {
        capability "Illuminance Measurement"
        capability "Polling"
        capability "Sensor"
        capability 'Refresh'

        attribute "formattedUsedTwilightBegin",      "string"
        attribute "formattedUsedLocalSunrise",       "string"
        attribute "formattedUsedSolarNoon",          "string"
        attribute "formattedUsedLocalSunset",        "string"
        attribute "formattedUsedTwilightEnd",        "string"
        
        attribute "localSunrise",                     "string"
        attribute "localSunset",                      "string"
        attribute "localSolarNoon",                   "string"
        attribute "localDayLength",                   "string"
        attribute "localCivilTwilightBegin",         "string"
        attribute "localCivilTwilightEnd",           "string"
        attribute "localNauticalTwilightBegin",      "string"
        attribute "localNauticalTwilightEnd",        "string"
        attribute "localAstronomicalTwilightBegin",  "string"
        attribute "localAstronomicalTwilightEnd",    "string"

		attribute "usedLongitude", 					"string"
		attribute "usedLatitude", 					"string"
		attribute "usedDate", 						"string"
		attribute "usedTimeZone", 					"string"
        attribute "usedTwilightBegin",               "string"
        attribute "usedTwilightEnd",                 "string"
        attribute "localSrEpoch",                    "number"
        attribute "localSsEpoch",                    "number"
             
        command "deleteAllStateVariables"
        command "deleteAllCurrentStates"
    }

    preferences {
        input name: 'overrideDate', type: 'date', title: 'Override Date', description: 'Override Date to be calculated (leave blank for today)', required: false           
        input name: 'overrideLatitude', type: 'text', title: 'Latitude', description: 'Override Latitude (leave blank for hub value)', required: false
        input name: 'overrideLongitude', type: 'text', title: 'Longitude', description: 'Override Longitude (leave blank for hub value)', required: false
        input name: 'overrideTimeZone', type: 'text', title: 'Timezone', description: 'Override Timezone (leave blank for hub value)', required: false
        input name: 'useCDate', type: 'bool', title: 'Use Current Date', description: "Disable to use Override Date (Default is enabled)", required: false, defaultValue: true
        input name: "twilightChoice", type: "enum", multiple: false, options: [["1":"Civil"],["2":"Nautical"],["3":"Astronomical"]], title: "Twilight Value Used", description: "Sets twilight pair used for main value (default is Civil)", defaultValue: "1", required: true
            
        input name: "autoUpdate", type: "bool", title: "Enable automatic update?", defaultValue: true, required: true
        input name: "autoUpdateInterval", type: "enum", multiple: false, options: [["1":"Every Minute"],["60":"Hourly"],["720":"12 Hours (Noon & Midnight)"],["1440":"Nightly (Every day at 1am)"]], title: "Auto Update Interval", description: "Time between automatic updates", defaultValue: "1440", required: true
        
        input name: "logEnable", type: "bool", title: "Enable descriptionText logging", description: "Log normal device process operations (Info)", defaultValue: true
        input name: "debugEnable", type: "bool", title: "<b>Enable debug logging</b>", description: "Turn on temporary debug logs", defaultValue: false
        input name: "traceEnable", type: "bool", title: "<b>Enable trace logging</b>", description: "Turn on deep driver execution step logging", defaultValue: false
    }
}

void updated() {
    unschedule()
    if (settings.debugEnable) runIn(1800, disableDebugLogging) 
    logTrace("${device.displayName} : Updated has run")
    logTrace("${device.displayName} : twilightChoice set to ${twilightChoice}")
    if (autoUpdate) {
        schedUpdate()
    }
}

void installed(){
    logInfo("Installed")
    updated()
    refresh()
}

void uninstalled() {
    logTrace("${device.displayName} : Uninstalled")
}

def schedUpdate() {
    if (autoUpdate) {
        logInfo("${device.displayName} : Scheduling update sequence initialization.")
        runIn(5, "mySchedule")
    }
}

def mySchedule() {
    String interval = autoUpdateInterval?.toString()
    logDebug("${device.displayName} : Configuring Cron for interval: ${interval}")
    
    switch(interval) {
        case "1":
            schedule("0 */1 * ? * *", "refresh")  
            break
        case "60":
            schedule("0 0 */1 ? * *", "refresh") 
            break
        case "720":
            schedule("0 0 */12 ? * *", "refresh")
            break
        case "1440":
        default:
            schedule("0 0 1 ? * *", "refresh")  
            break
    }
}        
        
void parse(String description) {
    logTrace("${device.displayName} : Description is $description")
}
   
void deleteAllCurrentStates() {
    def attribs = device.supportedAttributes*.name
    attribs.each { attr ->
        device.deleteCurrentState(attr)
    }
    
    // === ADD THESE TWO LINES TEMPORARILY ===
    device.removeDataValue("sunRiseSet")
    device.removeDataValue("dayLengthSet")
    
    logTrace("All current states and stale device data removed") 
}

void deleteAllStateVariables() {
    state.clear()
    logTrace("${device.displayName} : All state variables removed") 
}

void refresh() {
    pollSunRiseSet()
    logInfo("${device.displayName} : Refresh triggered.")  
}

void poll() {
    pollSunRiseSet()
    logInfo("${device.displayName} : Poll triggered.")
}

def pollSunRiseSet() {
    def lat = settings.overrideLatitude ?: location.latitude
    def lng = settings.overrideLongitude ?: location.longitude
    def tz = settings.overrideTimeZone ?: location.timeZone?.ID
    
    if (!lat || !lng) {
        logError("${device.displayName} : Cannot poll api. Missing Latitude/Longitude configuration values.")
        return
    }

    String targetDate = getTargetDate()

    def requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=${lat}&lng=${lng}&tzid=${tz}&date=${targetDate}&formatted=0" ]
    logInfo("SunRiseSet execution targeting API request date: ${targetDate}")
    
    asynchttpGet("sunRiseSetHandler", requestParams)
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
        logDebug("Event triggered: ${args.name} -> ${args.value}")
    }
}

private String getTargetDate(){
    if(useCDate || !settings.overrideDate){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd")
        sdf.setTimeZone(location.timeZone)
        return sdf.format(new Date())
    }
    return settings.overrideDate
}

private String formatTime(Date d){
    SimpleDateFormat sdf=new SimpleDateFormat("h:mm a",Locale.US)
    sdf.setTimeZone(location.timeZone)
    return sdf.format(d).toLowerCase()
}

private String formatDate(Date d){
    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US)
    sdf.setTimeZone(location.timeZone)
    return sdf.format(d)
}

private Date parseIsoDate(def value) {
    if (!value) return null
    try {
        return Date.parse("yyyy-MM-dd'T'HH:mm:ssXXX", value.toString())
    }
    catch (Exception e) {
        logDebug("Failed to parse ISO date string [${value}]: ${e.message}")
        return null
    }
}

private int safeInt(def value) {
    if (value == null) return 0

    if (value instanceof Number)
        return value.intValue()

    try {
        return value.toString().trim().toInteger()
    }
    catch (Exception ignored) {
        try {
            return value.toString().trim().toDouble().intValue()
        }
        catch (Exception ignored2) {
            return 0
        }
    }
}

def sunRiseSetHandler(resp, data) {
    int status = resp.getStatus()
    if (!(status >= 200 && status < 300)) {
        logError("Sunrise-Sunset API returned HTTP status ${status}")
        return
    }
    def results = null

    try {
        // Normal Hubitat parsing
        results = resp.getJson()?.results

        // Some firmware versions leave the JSON in resp.data
        if (!results && resp.data) {
            if (resp.data instanceof Map) {
                results = resp.data.results
            }
            else if (resp.data instanceof String) {
                results = new JsonSlurper().parseText(resp.data)?.results
            }
        }
    }
    catch (Exception e) {
        logError("Unable to parse Sunrise-Sunset response: ${e.message}")
        return
    }
    if (!results) {
        logError("Sunrise-Sunset response contained no results object.")
        return
    }

    // Parse API dates
    Date sRise    = parseIsoDate(results.sunrise)
    Date sSet     = parseIsoDate(results.sunset)
    Date sNoon    = parseIsoDate(results.solar_noon)
    Date civBeg   = parseIsoDate(results.civil_twilight_begin)
    Date civEnd   = parseIsoDate(results.civil_twilight_end)
    Date nautBeg  = parseIsoDate(results.nautical_twilight_begin)
    Date nautEnd  = parseIsoDate(results.nautical_twilight_end)
    Date astroBeg = parseIsoDate(results.astronomical_twilight_begin)
    Date astroEnd = parseIsoDate(results.astronomical_twilight_end)

    if (!sRise || !sSet) {
        logError("API did not return valid sunrise/sunset times.")
        return
    }

    Date usedTwilightBegin
    Date usedTwilightEnd

    switch (twilightChoice?.toString()) {

        case "2":
            usedTwilightBegin = nautBeg
            usedTwilightEnd   = nautEnd
            break

        case "3":
            usedTwilightBegin = astroBeg
            usedTwilightEnd   = astroEnd
            break

        default:
            usedTwilightBegin = civBeg
            usedTwilightEnd   = civEnd
            break
    }

    // Format strings
    String formattedUsedTwilightBegin = formatTime(usedTwilightBegin)
    String formattedUsedLocalSunrise  = formatTime(sRise)
    String formattedUsedSolarNoon     = formatTime(sNoon)
    String formattedUsedLocalSunset   = formatTime(sSet)
    String formattedUsedTwilightEnd   = formatTime(usedTwilightEnd)
    String usedTwilightBeginString = formatDate(usedTwilightBegin)
    String usedTwilightEndString   = formatDate(usedTwilightEnd)
    String localSunriseString = formatDate(sRise)
    String localSunsetString  = formatDate(sSet)

    long sunriseEpoch = sRise.time
    long sunsetEpoch  = sSet.time

    int totalSeconds = safeInt(results.day_length)

	int hours   = (totalSeconds / 3600) as Integer
	int minutes = ((totalSeconds % 3600) / 60) as Integer
	int seconds = (totalSeconds % 60) as Integer

	String formattedDayLength = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    String usedLatitude  = settings.overrideLatitude ?: location.latitude
    String usedLongitude = settings.overrideLongitude ?: location.longitude
    String usedDate      = getTargetDate()
    String usedTimeZone  = settings.overrideTimeZone ?: location.timeZone?.ID

    logDebug("Calculated Twilight Values: Begin=${usedTwilightBeginString}, End=${usedTwilightEndString}")

    sendIfChanged(name: "localSrEpoch", value: sunriseEpoch)
    sendIfChanged(name: "localSsEpoch", value: sunsetEpoch)
    sendIfChanged(name: "localSunrise", value: localSunriseString)
    sendIfChanged(name: "localSunset", value: localSunsetString)
    sendIfChanged(name: "localSolarNoon", value: formatDate(sNoon))
    sendIfChanged(name: "localCivilTwilightBegin", value: formatDate(civBeg))
    sendIfChanged(name: "localCivilTwilightEnd", value: formatDate(civEnd))
    sendIfChanged(name: "localNauticalTwilightBegin", value: formatDate(nautBeg))
    sendIfChanged(name: "localNauticalTwilightEnd", value: formatDate(nautEnd))
    sendIfChanged(name: "localAstronomicalTwilightBegin", value: formatDate(astroBeg))
    sendIfChanged(name: "localAstronomicalTwilightEnd", value: formatDate(astroEnd))
    sendIfChanged(name: "usedLatitude", value: usedLatitude)
    sendIfChanged(name: "usedLongitude", value: usedLongitude)
    sendIfChanged(name: "usedDate", value: usedDate)
    sendIfChanged(name: "usedTimeZone", value: usedTimeZone)
    sendIfChanged(name: "usedTwilightBegin", value: usedTwilightBeginString)
    sendIfChanged(name: "usedTwilightEnd", value: usedTwilightEndString)
    sendIfChanged(name: "formattedUsedTwilightBegin", value: formattedUsedTwilightBegin)
    sendIfChanged(name: "formattedUsedLocalSunrise", value: formattedUsedLocalSunrise)
    sendIfChanged(name: "formattedUsedSolarNoon", value: formattedUsedSolarNoon)
    sendIfChanged(name: "formattedUsedLocalSunset", value: formattedUsedLocalSunset)
    sendIfChanged(name: "formattedUsedTwilightEnd", value: formattedUsedTwilightEnd)
    sendIfChanged(name: "localDayLength", value: formattedDayLength)

    // === FORCE CLEAR CACHED AND MEMORY VARIABLES ===
    results = null
    if (resp.metaClass.respondsTo(resp, "clear")) { 
        try { resp.clear() } catch (Exception ignored) {} 
    }
    resp = null
    
    logDebug("Memory clean-up triggered: API response and results object discarded.")
}

void disableDebugLogging() {
    logWarn("30 minutes have elapsed. Automatically disabling debug logging.")
    device.updateSetting("debugEnable", [type: "bool", value: false])
}

// Unified dynamic logger helper mapping
private void logMessage(String level, String msg) {
    String prefKey = level == "info" ? "logEnable" : "${level}Enable"
    if (settings[prefKey] == true || level == "warn" || level == "error") {
        String formattedMsg = "Twilight Parser Driver${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
        
        switch(level) {
            case "error": log.error(formattedMsg); break
            case "warn":  log.warn(formattedMsg);  break
            case "info":  log.info(formattedMsg);  break
            case "debug": log.debug(formattedMsg); break
            case "trace": log.trace(formattedMsg); break
        }
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }