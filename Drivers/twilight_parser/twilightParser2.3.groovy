/*
 *
 *	Original Code Model - Copyright 2024 C Steele
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
 * 2024-11-10   jshimota	0.2.0   Added formatted values to be used in custom tiles
 * 2024-12-01   jshimota	0.2.1	Changed SDF to lowercase for formatted dates
 * 2026-06-22   jshimota    0.2.2   Gemini recommendations for modernization and bug fixes
 * 2026-06-22   jshimota    0.2.3   Optimized schedule updates and fixed Quartz cron format
 *
 */

static String version() { return '0.2.3' }
import java.text.SimpleDateFormat
import java.time.*

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

        attribute "usedTwilightBegin",               "string"
        attribute "usedTwilightEnd",                 "string"
        attribute "usedLatitude",                    "string"
        attribute "usedLongitude",                   "string"
        attribute "usedDate",                        "string"
        attribute "usedTimeZone",                    "string"
        attribute "localSrEpoch",                    "number"
        attribute "localSsEpoch",                    "number"
             
        command "deleteAllStateVariables"
        command "deleteAllCurrentStates"
    }

    preferences {
        input name: 'usedDate', type: 'date', title: 'Override Date', description: 'Override Date to be calculated (leave blank for today)', required: false           
        input name: 'usedLatitude', type: 'text', title: 'Latitude', description: 'Override Latitude (leave blank for hub value)', required: false
        input name: 'usedLongitude', type: 'text', title: 'Longitude', description: 'Override Longitude (leave blank for hub value)', required: false
        input name: 'usedTimeZone', type: 'text', title: 'Timezone', description: 'Override Timezone (leave blank for hub value)', required: false
        input name: 'useCDate', type: 'bool', title: 'Use Current Date', description: "Disable to use Override Date (Default is enabled)", required: false, defaultValue: true
        input name: "twilightChoice", type: "enum", multiple: false, options: [["1":"Civil"],["2":"Nautical"],["3":"Astronomical"]], title: "Twilight Value Used", description: "Sets twilight pair used for main value (default is Civil)", defaultValue: "1", required: true
            
        input name: "autoUpdate", type: "bool", title: "Enable automatic update?", defaultValue: true, required: true
        input name: "autoUpdateInterval", type: "enum", multiple: false, options: [["1":"Every Minute"],["60":"Hourly"],["720":"12 Hours (Noon & Midnight)"],["1440":"Nightly (Every day at 1am)"]], title: "Auto Update Interval", description: "Time between automatic updates", defaultValue: "1440", required: true
        
        input name: "logEnable", type: "bool", title: "Enable description text logging", description: "Enable normal Logging", defaultValue: true
        input name: "debugEnable", type: "bool", title: "<b>Enable debug logging</b>", description: "Enable Debug Logging", defaultValue: false
        input name: "traceEnable", type: "bool", title: "<b>Enable trace logging?</b>", description: "Enable Trace Logging", defaultValue: false
    }
}

void updated() {
    unschedule()
    if (debugEnable) runIn(1800, logsOff) 
    if (traceEnable) log.trace "${device.displayName} : Updated has run"
    if (traceEnable) log.trace "${device.displayName} : twilightChoice set to ${twilightChoice}"
    if (autoUpdate) {
        schedUpdate()
    }
}

void installed() {
    if (traceEnable) log.trace "${device.displayName} : Installed"
    refresh()
}

void uninstalled() {
    if (traceEnable) log.trace "${device.displayName} : Uninstalled"
}

def schedUpdate() {
    if (autoUpdate) {
        if (logEnable) log.info "${device.displayName} : Scheduling update sequence initialization."
        runIn(5, "mySchedule")
    }
}

def mySchedule() {
    String interval = autoUpdateInterval?.toString()
    if (debugEnable) log.debug "${device.displayName} : Configuring Cron for interval: ${interval}"
    
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
    if (traceEnable) log.trace "${device.displayName} : Description is $description"
}

void logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugEnable", [value: "false", type: "bool"])
}
   
void deleteAllCurrentStates() {
    def attribs = device.supportedAttributes*.name
    attribs.each { attr ->
        device.deleteCurrentState(attr)
    }
    if (traceEnable) log.trace "All current states removed" 
}

void deleteAllStateVariables() {
    state.clear()
    if (traceEnable) log.trace "${device.displayName} : All state variables removed" 
}

void refresh() {
    pollSunRiseSet()
    if (logEnable) log.info "${device.displayName} : Refresh triggered."  
}

void poll() {
    pollSunRiseSet()
    if (logEnable) log.info "${device.displayName} : Poll triggered."
}

def pollSunRiseSet() {
    def lat = usedLatitude ?: location.latitude
    def lng = usedLongitude ?: location.longitude
    def tz = usedTimeZone ?: location.timeZone?.ID
    
    if (!lat || !lng) {
        log.error "${device.displayName} : Cannot poll api. Missing Latitude/Longitude configuration values."
        return
    }

    def targetDate
    if (useCDate) {
        def sdf = new SimpleDateFormat("yyyy-MM-dd")  
        sdf.setTimeZone(location.timeZone)
        targetDate = sdf.format(new Date(now()))
    } else {
        targetDate = usedDate
    }

    def requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=${lat}&lng=${lng}&tzid=${tz}&date=${targetDate}&formatted=0" ]
    if (traceEnable) log.info "SunRiseSet execution targeting API request date: ${targetDate}"
    
    asynchttpGet("sunRiseSetHandler", requestParams)
}

def sunRiseSetHandler(resp, data) {
    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
        def sunRiseSet = resp.getJson()?.results
        if (!sunRiseSet) {
            log.error "API returned clean 200 response but empty payload fields."
            return
        }
        
        updateDataValue("sunRiseSet", resp.getData())
        state.sunRiseSetInit = true

        state.localSunrise = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise)
        state.localSunset  = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset)
        state.localSolarNoon     = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.solar_noon)
        state.localCivilTwilightBegin    = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_begin)
        state.localCivilTwilightEnd      = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_end)
        state.localNauticalTwilightBegin    = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.nautical_twilight_begin)
        state.localNauticalTwilightEnd      = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.nautical_twilight_end)
        state.localAstronomicalTwilightBegin    = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.astronomical_twilight_begin)
        state.localAstronomicalTwilightEnd     = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.astronomical_twilight_end)

        def dTTimeHourMinAPattern = new SimpleDateFormat('h:mm a')
        dTTimeHourMinAPattern.setTimeZone(location.timeZone)
        
        String formattedUsedTwilightBegin = dTTimeHourMinAPattern.format(state.localCivilTwilightBegin).toLowerCase(Locale.US) 
        String formattedUsedLocalSunrise = dTTimeHourMinAPattern.format(state.localSunrise).toLowerCase(Locale.US)
        String formattedUsedSolarNoon = dTTimeHourMinAPattern.format(state.localSolarNoon).toLowerCase(Locale.US)
        String formattedUsedLocalSunset = dTTimeHourMinAPattern.format(state.localSunset).toLowerCase(Locale.US)
        String formattedUsedTwilightEnd = dTTimeHourMinAPattern.format(state.localCivilTwilightEnd).toLowerCase(Locale.US)
        
        state.localDayLength = sunRiseSet.day_length
        state.usedLatitude = usedLatitude ?: location.latitude
        state.usedLongitude = usedLongitude ?: location.longitude

        def usedTwilightBegin
        def usedTwilightEnd
        String choice = twilightChoice?.toString() ?: "1"

        if (choice == "1") {
            usedTwilightBegin = state.localCivilTwilightBegin
            usedTwilightEnd = state.localCivilTwilightEnd
        } else if (choice == "2") {
            usedTwilightBegin = state.localNauticalTwilightBegin
            usedTwilightEnd = state.localNauticalTwilightEnd
        } else if (choice == "3") {
            usedTwilightBegin = state.localAstronomicalTwilightBegin
            usedTwilightEnd = state.localAstronomicalTwilightEnd
        }
        
        state.usedTwilightBegin = usedTwilightBegin
        state.usedTwilightEnd = usedTwilightEnd

        if (useCDate) {
            def sdf = new SimpleDateFormat("yyyy-MM-dd")  
            sdf.setTimeZone(location.timeZone)
            state.usedDate = sdf.format(new Date(now()))
        } else {
            state.usedDate = usedDate
        }

        def localSrEpoch = state.localSunrise.getTime()
        def localSsEpoch = state.localSunset.getTime()
        
        state.usedlocalSrEpoch = localSrEpoch
        state.usedlocalSsEpoch = localSsEpoch

        try {
            setGlobalVar("TwilightB4SunriseBeginsValue", state.usedTwilightBegin) 
            setGlobalVar("TwilightPastSunsetEndsValue", state.usedTwilightEnd) 
        } catch (e) {
            if (debugEnable) log.debug "Global Variable updates failed. Ensure variables exist inside Hub settings."
        }
        
        state.usedTimeZone = usedTimeZone ?: location.timeZone?.ID

        sendEvent(name: 'localSrEpoch', value: localSrEpoch)
        sendEvent(name: 'localSsEpoch', value: localSsEpoch)
        sendEvent(name: 'localSunrise', value: state.localSunrise)        
        sendEvent(name: 'localSunset' , value: state.localSunset)
        sendEvent(name: 'localSolarNoon'    , value: state.localSolarNoon)
        sendEvent(name: 'localCivilTwilightBegin'    , value: state.localCivilTwilightBegin)
        sendEvent(name: 'localCivilTwilightEnd'      , value: state.localCivilTwilightEnd)
        sendEvent(name: 'localNauticalTwilightBegin'    , value: state.localNauticalTwilightBegin)
        sendEvent(name: 'localNauticalTwilightEnd'      , value: state.localNauticalTwilightEnd)
        sendEvent(name: 'localAstronomicalTwilightBegin'    , value: state.localAstronomicalTwilightBegin)
        sendEvent(name: 'localAstronomicalTwilightEnd'     , value: state.localAstronomicalTwilightEnd)
        sendEvent(name: 'usedLatitude', value: state.usedLatitude)
        sendEvent(name: 'usedLongitude', value: state.usedLongitude)
        sendEvent(name: 'usedDate', value: state.usedDate)
        sendEvent(name: 'usedTimeZone', value: state.usedTimeZone)
        sendEvent(name: 'usedTwilightBegin'    , value: usedTwilightBegin)
        sendEvent(name: 'usedTwilightEnd'      , value: usedTwilightEnd)
        
        sendEvent(name: 'formattedUsedTwilightBegin'    , value: formattedUsedTwilightBegin)
        sendEvent(name: 'formattedUsedLocalSunrise'    , value: formattedUsedLocalSunrise)
        sendEvent(name: 'formattedUsedSolarNoon'    , value: formattedUsedSolarNoon)
        sendEvent(name: 'formattedUsedLocalSunset'    , value: formattedUsedLocalSunset)
        sendEvent(name: 'formattedUsedTwilightEnd'    , value: formattedUsedTwilightEnd)
        
        int totalSeconds = sunRiseSet.day_length as Integer
        int hours = totalSeconds / 3600
        int minutes = (totalSeconds % 3600) / 60
        int seconds = totalSeconds % 60
        String formattedDay_Length = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        
        sendEvent(name: 'localDayLength', value: formattedDay_Length)   
    } else { 
        log.error "Sunrise-sunset api poll did not return data. Status Code: ${resp.getStatus()}" 
    }
}