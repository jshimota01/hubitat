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
 *
 */
    static String version() { return '0.2.0' }
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
		attribute "formattedUsedSolarNoon",		     "string"
		attribute "formattedUsedLocalSunset",        "string"
		attribute "formattedUsedTwilightEnd",        "string"
        
        attribute "localSunrise",	                 "string"
		attribute "localSunset",	                 "string"
		attribute "localSolarNoon",                  "string"
		attribute "localDayLength",                  "string"
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
        attribute "localSrEpoch",                    "date"
        attribute "localSsEpoch",                    "date"
             
		command "deleteAllStateVariables"
		command "deleteAllCurrentStates"
        }

        preferences {
            def sdf= new SimpleDateFormat("yyyy-MM-dd")  
            defDate = sdf.format(now())
            defLat = location.latitude
            defLong = location.longitude
            defTZ = location.timeZone.ID
            
        input name: 'usedDate', type: 'date', title: 'Override Date', description: 'Override Date to be calculated (default is today)', required:false,defaultValue: defDate           
        input name: 'usedLatitude', type: 'text', title: 'Latitude', description: 'Override Latitude (default is hub value)', required:false, defaultValue: defLat
		input name: 'usedLongitude', type: 'text', title: 'Longitude', description: 'Override Longitude (default is hub value)', required:false, defaultValue: defLong
		input name: 'usedTimeZone', type: 'text', title: 'Timezone', description: 'Override Timezone (default is hub value)', required:false, defaultValue: defTZ
        input name: 'useCDate', type: 'bool', title: 'Use Current Date', description: "Disable to use Override Date (Default is enabled)", required: false, defaultValue: true
		input(name: "twilightChoice", type: "enum", multiple: false, options: [[1:"Civil"],[2:"Nautical"],[3:"Astronomical"]], title: "Twilight Value Used", description: "Sets twilight pair used for main value (default is Civil)", defaultValue: 1, required: true, displayDuringSetup: true)
            
        input("autoUpdate", "bool", title: "Enable automatic update?\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
		input(name: "autoUpdateInterval", type: "enum", multiple: false, options: [[1:"Every Minute"],[60:"Hourly"],[720:"12 Hours (Noon & Midnight)"],[1440:"Nightly (Every day at 1am)"]], title: "Auto Update Interval", description: "Time between automatic updates", defaultValue: 1440, required: true, displayDuringSetup: true)
    	 // standard logging options
		input name: "logEnable", type: "bool", title: "Enable logging", defaultValue: true
		input name: "debugEnable",    type: "bool", title: "<b>Enable debug logging</b>", defaultValue: false
		input name: "descTextEnable", type: "bool", title: "<b>Enable descriptionText logging?</b>", defaultValue: true
        }

}

void updated() {
	initialize()
	unschedule()
      if (debugEnable) runIn(1800,logsOff) //disable debug logs after 30 min
	  if (descTextEnable) log.trace  "${device.displayName} : Updated has run"
      if (descTextEnable) log.trace ("${device.displayName} : twilightChoice set to ${twilightChoice}")
    if (autoUpdate) {
        schedUpdate()
    }
}

void installed() {
	initialize()
	if (descTextEnable) log.trace "${device.displayName} : Initialize ran after install"
}

void uninstalled() {
	if (descTextEnable) log.trace "${device.displayName} : Uninstalled"
}

void initialize() {
	if (descTextEnable) log.trace "${device.displayName} : Initialized"
}

def schedUpdate() {
    if (logEnable) log.info("${device.displayName} :  Update schedule cleared. Setting new schedule ...")
    if (autoUpdate) {
        if (logEnable) log.info("${device.displayName} : Setting next scheduled refresh with ${autoUpdateInterval} minute(s) interval.")
        if (debugEnable) log.debug("${device.displayName} : About to run runIn schedUpdate: Setting up schedule with ${autoUpdateInterval} minute(s) interval.")
        runIn(5,mySchedule)
        if (debugEnable) log.debug("${device.displayName} : After run Runin schedUpdate: Setting up schedule with ${autoUpdateInterval} minute(s) interval")
        if (logEnable) log.info("${device.displayName} : Next scheduled Refresh now set for ${autoUpdateInterval} minute(s).")
        return
    }
    return
}

def mySchedule() {
        if (debugEnable) log.debug("${device.displayName}  : Running schedule at entry. Setting up schedule with ${autoUpdateInterval} minute(s) interval")
    if (autoUpdateInterval == "1") {
           if (debugEnable) log.debug("${device.displayName} : Running schedule step 1 after run Runin schedUpdate: Setting up schedule with ${autoUpdateInterval} minute(s) interval")
           schedule("0 0/1 0/1 ? * * *", "refresh")  
           if (debugEnable) log.debug("${device.displayName} : Set up schedule with 1 minute interval")
    }
    if (autoUpdateInterval == "60") {
           if (debugEnable) log.debug("${device.displayName} : Running schedule step 2 after run Runin schedUpdate: Setting up schedule with ${autoUpdateInterval} minute(s) interval")
           schedule("0 1 0/1 ? * * *", "refresh") 
           if (debugEnable) log.debug("${device.displayName} : Set up schedule with 1 hour interval")
    }
    if (autoUpdateInterval == "720") {
           if (debugEnable) log.debug("${device.displayName} : Running schedule step 3 after run Runin schedUpdate: Setting up schedule with ${autoUpdateInterval} minute(s) interval")
           schedule("0 0/1 0/12 ? * * *", "refresh")
           if (debugEnable) log.debug("${device.displayName} : Set up schedule with 12 hour (noon & midnight) interval")
    }
    if (autoUpdateInterval == "1440") {
           if (debugEnable) log.debug("${device.displayName} : Running schedule step 4 after run Runin schedUpdate: Setting up schedule with ${autoUpdateInterval} minute(s) interval")
           schedule("0 0 1 * * ?", "refresh")  //default 
           if (debugEnable) log.debug("${device.displayName} : Set up schedule with daily (1am) interval")
    }
}        
        
        
void parse(String description) {
	if (descTextEnable) log.trace "${device.displayName} : Description is $description"
}

void logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
   
void deleteAllCurrentStates() {
    currentState.remove()
	if (descTextEnable) log.trace "All current states removed" 
}

void deleteAllStateVariables() {
    state.clear()
	if (descTextEnable) log.trace "${device.displayName} : All state variables removed" 
}

void refresh() {
    pollSunRiseSet()
	if (logEnable) log.info "${device.displayName} : gatherer triggered by refresh."  
    schedUpdate()
}

void poll() {
    pollSunRiseSet()
    if (logEnable) log.info "${device.displayName} : gatherer triggered by poll."
    schedUpdate()
}

def pollSunRiseSet() {
   	if (useCDate) {
        def sdf= new SimpleDateFormat("yyyy-MM-dd")  
        cDate = sdf.format(now())
        if (true) {
	    	def requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=$usedLatitude&lng=$usedLongitude&tzid=$usedTimeZone&date=$cDate&formatted=0" ]
		    if (descTextEnable) log.info "SunRiseSet poll for $usedLatitude  $usedLongitude $usedTimeZone $cDate (Today)"
    		if (descTextEnable) log.info "SunRiseSet poll for https://api.sunrise-sunset.org/json?lat=$usedLatitude&lng=$usedLongitude&tzid=$usedTimeZone&date=$cDate&formatted=0 (Today)"
            asynchttpGet("sunRiseSetHandler", requestParams)
    	} else {
	    	state?.sunRiseSet?.init = false
		    log.error "${device.displayName} : No sunrise-sunset without Lat/Long."
    	}
    } else {
        if (true) {
		    def requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=$usedLatitude&lng=$usedLongitude&tzid=$usedTimeZone&date=$usedDate&formatted=0" ]
    		if (descTextEnable) log.info "SunRiseSet poll for $usedLatitude  $usedLongitude $usedTimeZone $usedDate (Overridden Date)"
	    	if (descTextEnable) log.info "SunRiseSet poll for https://api.sunrise-sunset.org/json?lat=$usedLatitude&lng=$usedLongitude&tzid=$usedTimeZone&date=$usedDate&formatted=0 (Overridden Date)"
            asynchttpGet("sunRiseSetHandler", requestParams)
    	} else {
	    	state?.sunRiseSet?.init = false
		    log.error "${device.displayName} : No sunrise-sunset without Lat/Long."
	    }
    }
}

def sunRiseSetHandler(resp, data) {
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		sunRiseSet = resp.getJson().results
        	updateDataValue("sunRiseSet", resp.getData())
		state?.sunRiseSet?.init = true
		if (debugEnable) log.debug "${device.displayName} : sunRiseSet value is: $sunRiseSet"

		state.localSunrise = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise)
        state.localSunset  = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset)
		state.localSolarNoon     = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.solar_noon)
		state.localCivilTwilightBegin    = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_begin)
		state.localCivilTwilightEnd      = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_end)
		state.localNauticalTwilightBegin    = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.nautical_twilight_begin)
		state.localNauticalTwilightEnd      = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.nautical_twilight_end)
		state.localAstronomicalTwilightBegin    = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.astronomical_twilight_begin)
		state.localAstronomicalTwilightEnd     = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.astronomical_twilight_end)

		// define pattern
        dTTimeHourMinAPattern = new SimpleDateFormat('h:mm a')
		
		// use pattern
        formattedUsedTwilightBegin = dTTimeHourMinAPattern.format(state.localCivilTwilightBegin)
        formattedUsedLocalSunrise = dTTimeHourMinAPattern.format(state.localSunrise)
        formattedUsedSolarNoon = dTTimeHourMinAPattern.format(state.localSolarNoon)
        formattedUsedLocalSunset = dTTimeHourMinAPattern.format(state.localSunset)
        formattedUsedTwilightEnd = dTTimeHourMinAPattern.format(state.localCivilTwilightEnd)
				
        state.localDayLength                   = sunRiseSet.day_length
        state.usedLatitude = usedLatitude
        state.usedLongitude = usedLongitude

        if (twilightChoice == "1") {
            usedTwilightBegin = state.localCivilTwilightBegin
            usedTwilightEnd = state.localCivilTwilightEnd
            state.usedTwilightBegin = usedTwilightBegin
            state.usedTwilightEnd = usedTwilightEnd
        }
        if (twilightChoice == "2") {
            usedTwilightBegin = state.localNauticalTwilightBegin
            usedTwilightEnd = state.localNauticalTwilightEnd
            state.usedTwilightBegin = usedTwilightBegin
            state.usedTwilightEnd = usedTwilightEnd
        }
        if (twilightChoice == "3") {
            usedTwilightBegin = state.localAstronomicalTwilightBegin
            usedTwilightEnd = state.localAstronomicalTwilightEnd
            state.usedTwilightBegin = usedTwilightBegin
            state.usedTwilightEnd = usedTwilightEnd
        }

        if (useCDate) {
            def sdf= new SimpleDateFormat("yyyy-MM-dd")  
            cDate = sdf.format(now())
            state.usedDate = cDate
        } else {
            state.usedDate = usedDate
        }

        localSrEpoch= state.localSunrise.getTime() // / 1000
        localSsEpoch= state.localSunset.getTime() // / 1000
        state.usedlocalSrEpoch = localSrEpoch
        state.usedlocalSsEpoch = localSrEpoch

        // Hub Global Variables MUST BE CREATED MANUALLY !!!
//        setGlobalVar("TwilightB4SunriseBeginsValue", localSrEpoch) 
        setGlobalVar("TwilightB4SunriseBeginsValue", state.usedTwilightBegin) 
        
                if (descTextEnable) log.info "${device.displayName} : TwilightB4SunriseBeginsValue (hub global variable IF IT EXISTS) set to ${state.usedTwilightBegin}"
        setGlobalVar("TwilightPastSunsetEndsValue", state.usedTwilightEnd) 
                if (descTextEnable) log.info "${device.displayName} : TwilightPastSunsetEndsValue (hub global variable IF IT EXISTS!) set to ${state.usedTwilightEnd}"
        
        state.usedTimeZone = usedTimeZone

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
		sendEvent(name: 'localAstronomicalTwilightEnd'      , value: state.localAstronomicalTwilightEnd)
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
        
// Special handling of day_length

		int hours = sunRiseSet.day_length / 3600;
		int minutes = (sunRiseSet.day_length % 3600) / 60;
		int seconds = sunRiseSet.day_length % 60;
        String formattedDay_Length = ("${hours}:${minutes}:${seconds}");
        
		sendEvent(name: 'localDayLength'      , value: formattedDay_Length)   

    } else { log.error "Sunrise-sunset api poll did not return data" }
}