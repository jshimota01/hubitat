/*
 * Import URL: https://raw.githubusercontent.com/HubitatCommunity/AirQuality-AirNow/master/AirQuality-AirNow.groovy"
 *
 *	Copyright 2021 C Steele
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License. You may obtain a copy
 *	of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *	License for the specific language governing permissions and limitations
 *	under the License.
 *
 *
 */

/*
 *         v1.0.6  JAS - Gemini recommendations
 *         v1.0.5  JAS - Added ReportingArea and StateCode source for tile info 12/15/2024
 *         v1.0.4  JAS - Cleaned up some logging items 11/10/23
 *         v1.0.3  JAS - Split out category and color 08/08/23
 *         v1.0.2  PR from cmbruns
 *			 According to Hubitat docs the airQualityIndex attribute is supposed range from 0 to 500, meaning it should be the 
 *			 full PM10-equivalent value, not the 6-category meaning previously used and matches Ecowitt air quality sensor range.
 *         v1.0.1  renamed "PM2.5" attribute to not use a dot (.)
 * csteele v1.0.0  created.
 */

static String version()	{  return '1.0.6'  }

import groovy.transform.Field

metadata {
	definition (name: 'Air Quality from AirNow Custom', namespace: 'jshimota', author: 'jshimota') {
		capability 'AirQuality'
		capability 'Sensor'

		attribute 'airQualityState', 'STRING'
		attribute 'airQualityCity', 'STRING'
		attribute 'O3', 'number'
		attribute 'PM2_5', 'number'
		attribute 'PM10', 'number'
		attribute 'airQualityIndex', 'number'
		attribute 'airQualityColor', 'STRING'
		attribute 'airQualityCategory', 'number'

		command 'pollAirNow'
	}

	preferences {
		input 'apiKey',      'text', title: '<b>Type AirNow.org API Key Here</b>', required: true, defaultValue: null
		input 'pollEvery',   'enum', title: '<b>Publish AQI how frequently?</b>',  required:false, defaultValue: 1, options:[1:'1 hour',2:'2 hours',8:'8 hours',16:'16 hours']
		input 'basedOn',     'enum', title: '<b>Publish AQI Number based on?</b>', required:false, defaultValue: 1, options:[1:'O3 ozone',2:'PM2.5 particle',3:'PM10 partice', 4: 'Worst AQI']
		input 'debugOutput', 'bool', title: 'Enable debug logging',                required:false, defaultValue: true
		input 'txtEnable',   'bool', title: 'Enable descriptionText logging',      required:false, defaultValue: true
	}
}

void pollAirNow() {
    if (apiKey == null) {
        log.warn "AirNow API Key is missing. Please enter it in the device settings."
        return
    }
    
    // Log the coordinates being used
    if (txtEnable) log.info "Polling AirNow for Location: Lat: ${location.latitude}, Lon: ${location.longitude}"

    Map params = [
            uri: 'https://www.airnowapi.org/aq/observation/latLong/current/?format=application/json&latitude=' + (String)location.latitude + '&longitude=' + (String)location.longitude + '&distance=25&API_KEY=' + (String)apiKey,
            timeout: 20 ]
            
    if (debugOutput) log.debug "Full Request Params: ${params}"
    asynchttpGet('pollHandler', params)
}

void pollHandler(resp, data) {
    if (resp.getStatus() == 200 || resp.getStatus() == 207) {
        if (debugOutput) log.debug "R: $resp.data"
        def aqi = parseJson(resp.data)

        def isBasis = aqiBasis[basedOn as Integer]
        def maxAQI  = -1
        def maxAQICat = -1
        def maxCity = ""
        def maxDevState = ""

        aqi.each { obs ->
            if ((obs.AQI >= 0) && (obs.AQI <= 2000)) { // sanity check the value
                
                // Track the worst AQI and its associated metadata
                if (obs.AQI > maxAQI) {
                    maxAQI = obs.AQI
                    maxAQICat = obs.Category.Number
                    maxCity = obs.ReportingArea
                    maxDevState = obs.StateCode
                }

                def descriptionText = "${device.displayName} ${obs.ParameterName} is ${obs.AQI}"
                def attrNam = obs.ParameterName.replace('.', '_')

                if (debugOutput) log.debug "${descriptionText}"
                sendEvent(name: attrNam, value: obs.AQI, descriptionText: descriptionText)

                // If the user selected a specific parameter (O3, PM2.5, PM10)
                if (isBasis == obs.ParameterName) {
                    if (txtEnable) log.info "AQI Values updated. Enable Debug to monitor changes realtime."
                    
                    sendEvent(name: "airQualityIndex", value: obs.AQI, descriptionText: "${device.displayName} airQualityIndex is ${obs.AQI}")
                    sendEvent(name: "airQualityCategory", value: aqiCategory[obs.Category.Number], descriptionText: "${device.displayName} airQualityCategory is ${aqiCategory[obs.Category.Number]}")
                    sendEvent(name: "airQualityColor", value: aqiColor[obs.Category.Number], descriptionText: "${device.displayName} airQualityColor is ${aqiColor[obs.Category.Number]}")
                    sendEvent(name: "airQualityCity", value: obs.ReportingArea, descriptionText: "${device.displayName} airQualityCity is ${obs.ReportingArea}")
                    sendEvent(name: "airQualityState", value: obs.StateCode, descriptionText: "${device.displayName} airQualityState is ${obs.StateCode}")                    
                }
            }
        }
        
        // If the user selected "Worst AQI"
        if (isBasis == "maxAQI" && maxAQI >= 0) {
            if (txtEnable) log.info "AQI Values updated based on Worst AQI. Enable Debug to monitor changes realtime."
            
            sendEvent(name: "airQualityIndex", value: maxAQI, descriptionText: "${device.displayName} airQualityIndex is $maxAQI")
            sendEvent(name: "airQualityCategory", value: aqiCategory[maxAQICat], descriptionText: "${device.displayName} airQualityCategory is ${aqiCategory[maxAQICat]}")
            sendEvent(name: "airQualityColor", value: aqiColor[maxAQICat], descriptionText: "${device.displayName} airQualityColor is ${aqiColor[maxAQICat]}")
            sendEvent(name: "airQualityCity", value: maxCity, descriptionText: "${device.displayName} airQualityCity is $maxCity")
            sendEvent(name: "airQualityState", value: maxDevState, descriptionText: "${device.displayName} airQualityState is $maxDevState")
        }
    }
}

void updated() {
	unschedule()
	//               "Seconds" "Minutes" "Hours" "Day Of Month" "Month" "Day Of Week" "Year"
	if (apiKey) schedule("3 7 0/${pollEvery.toInteger()} ? * * *", pollAirNow)
	if (debugOutput) runIn(1800,logsOff)
}

void uninstalled() {
	unschedule()
}

def logsOff(){
	log.warn "debug logging disabled..."
	device.updateSetting("debugOutput",[value:"false",type:"bool"])
}

@Field static aqiColor = [1: "Green", 2: "Yellow", 3: "Orange", 4: "Red", 5: "Purple", 6: "Maroon"]
@Field static aqiCategory = [1: "Good", 2: "Moderate", 3: "Unhealthy for Sensitive Groups", 4: "Unhealthy", 5: "Very Unhealthy", 6: "Hazardous"]
@Field static aqiBasis = [1: "O3", 2: "PM2.5", 3: "PM10", 4: "maxAQI"]
