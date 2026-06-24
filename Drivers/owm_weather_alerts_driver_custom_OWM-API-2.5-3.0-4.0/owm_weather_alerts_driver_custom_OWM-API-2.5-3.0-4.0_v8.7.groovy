/* OpenWeatherMap Weather-Alerts Driver (Custom 2.5/3.0/4.0 OWM API)                    
    Import URL: https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/owm_weather_alerts_driver_custom_OWM-API-2.5-3.0-4/owm_weather_alerts_driver_custom_OWM-API-2.5-3.0-4.0.groovy.
    
    Taken from the last work of @Matthew (Scottma61) about 9/2023

    This driver has morphed many, many times, so the genesis is very blurry now.  It stated as a WeatherUnderground
    driver, then when they restricted their API it morphed into an APIXU driver.  When APIXU ceased it became a
    Dark Sky driver .... and now that Dark Sky is going away it is morphing into a OpenWeatherMap driver.

    Many people contributed to the creation of this driver.  Significant contributors include:
    - @Cobra who adapted it from @mattw01's work and I thank them for that!
    - @bangali for his original APIXU.COM base code that much of the early versions of this driver was
      adapted from.
    - @bangali for his the Sunrise-Sunset.org code used to calculate illuminance/lux and the more
      recent adaptations of that code from @csteele in his continuation driver 'wx-ApiXU'.
    - @csteele (and prior versions from @bangali) for the attribute selection code.
    - @csteele for his examples on how to convert to asyncHttp calls to reduce Hub resource utilization.
    - @bangali also contributed the icon work from
      https://github.com/jebbett for new cooler 'Alternative' weather icons with icons courtesy
      of https://www.deviantart.com/vclouds/art/VClouds-Weather-Icons-179152045.
    - @storageanarchy for his Dark Sky Icon mapping and some new icons to compliment the Vclouds set.
    - @nh.schottfam for lots of code clean up and optimizations.

    In addition to all the cloned code from the Hubitat community, I have heavily modified/created new
    code myself @Matthew (Scottma61) with lots of help from the Hubitat community.  If you believe you
    should have been acknowledged or received attribution for a code contribution, I will happily do so.
    While I compiled and orchestrated the driver, very little is actually original work of mine.

    This driver is free to use.  I do not accept donations. Please feel free to contribute to those
    mentioned here if you like this work, as it would not have been possible without them.

    This driver is intended to pull weather data from OpenWeatherMap.org (https://OpenWeatherMap.org). You will need your
    OpenWeatherMap API key to use the data from that site.  It also pulls in weather alerts from the Nation Weather
    Service's API (weather.gov).  At the present time there is no API required for consume Alert data.

    The driver exposes both metric and imperial measurements for you to select from.

    Licensed under the Apache License, Version 2.0 (the 'License'); you may not use this file except
    in compliance with the License. You may obtain a copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
    on an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
    for the specific language governing permissions and limitations under the License.
*/
/*
    Last Update 06/24/2026
  
    V0.8.7  	06/24/2026    JAS     Cleaned missing routing ifreInstalled, fixed pollOWM and pollData
	V0.8.6 		06/24/2026    JAS     Added status and lastChecked attributes to track API health.
    V0.8.5  	06/24/2026    JAS     Began - OWM icons placed into Git
    V0.8.4  	06/24/2026    JAS     Split to 3rd branch to include all API's 2.5/3.0 and 4.0
    V0.7.3d 	06/23/2026    JAS     Made old version cuz my new one is broken on 4.0 api
    V0.7.3c 	06/24/2026	  JAS     updated versioning on split of code between 2.5/3.0 and 3.0/4.0 
    V0.7.3b		06/10/2026    JAS     updating to match Orig Auth version + bug found on rainTodayPublish.
    V0.7.3		03/19/2026    JAS     Clear NWS alerts after they expire (@rschumaker).
    V0.7.2		01/17/2026    JAS     Replaced small icons with unicode characters in dashboard tiles.
    V0.7.1b		10/26/2024    JAS     Custom HTML tile
    V0.7.1		07/29/2024    JAS     Added attribute 'alertDescrFull' that contain the full text of up to 10 current alerts.
    V0.7.0		05/13/2024    JAS     Corrected moon_phase.
    V0.6.9		04/17/2024    JAS     Added moonrise, moonset and moon_phase attributes.
    V0.6.8b		09/02/2023    JAS     1st jas custom html tile
*/

static String version()    {  return '0.8.7'  }
import groovy.transform.Field

iconLocation = (!iconLocation || iconLocation == null) ? 'https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/owm_weather_alerts_driver_custom_OWM-API-2.5-3.0-4/owm-icons/' : iconLocation
@Field static final String sNULL=(String)null
@Field static final String sAB='<a>'
@Field static final String sACB='</a>'
@Field static final String sCSPAN='</span>'
@Field static final String sBR='<br>'
@Field static final String sBLK=''
@Field static final String sSPC=' '
@Field static final String sRB='>'
@Field static final String sCOMMA=','
@Field static final String sMINUS='-'
@Field static final String sCOLON=':'
@Field static final String sZERO='0'
@Field static final String sONE='1'
@Field static final String sTWO='2'
@Field static final String sDOT='.'
@Field static final String sICON='iconLocation'
@Field static final String sTMETR='tMetric'
@Field static final String sDMETR='dMetric'
@Field static final String sPMETR='pMetric'
@Field static final String sRMETR='rMetric'
@Field static final String sTEMP='temperature'
@Field static final String sSUMLST='Summary_last_poll_time'
@Field static final String sTRU='true'
@Field static final String sFLS='false'
@Field static final String sNPNG='na.png'
@Field static final String s11D='11d.png'
@Field static final String s11N='11n.png'
@Field static final String sCTS='chancetstorms'
@Field static final String sNCTS='nt_chancetstorms'
@Field static final String sRAIN='rain'
@Field static final String sNRAIN='nt_rain'
@Field static final String sPCLDY='partlycloudy'
@Field static final String sNPCLDY='nt_partlycloudy'
@Field static final String s23='23.png'
@Field static final String s9='9.png'
@Field static final String s39='39.png'
@Field static final String sDF='°F'
@Field static final String sIMGS5='<img class="cI" src='
@Field static final String sIMGS8='<img class="cIb" src='
@Field static final String sTD='<td>'
@Field static final String sTR='<tr><td>'
@Field static final String sSTR='string'
@Field static final String sNUM='number'
@Field static final String sNCWA='No current weather alerts for this area'

metadata {
    definition (name: 'OpenWeatherMap Weather-Alerts Driver (Custom 2.5/3.0/4.0 OWM API)',
        namespace: 'jshimota',
        author: 'ManyHands',
        importUrl: 'https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/owm_weather_alerts_driver_custom_OWM-API-2.5-3.0-4/owm_weather_alerts_driver_custom_OWM-API-2.5-3.0-4.0.groovy') 
        {
        capability 'Sensor'
        capability 'Temperature Measurement'
        capability 'Illuminance Measurement'
        capability 'Relative Humidity Measurement'
        capability 'Pressure Measurement'
        capability 'Ultraviolet Index'

        capability 'Refresh'

        attributesMap.each {
            k, v -> if (v.ty)    attribute k, v.ty
        }

//    The following attributes may be needed for dashboards that require these attributes,
//    so they are alway available and shown by default.
        attribute 'city', sSTR            //Hubitat  OpenWeather  SharpTool.io  SmartTiles
        attribute 'feelsLike', sNUM        //SharpTool.io  SmartTiles
        attribute 'forecastIcon', sSTR    //SharpTool.io
        attribute 'localSunrise', sSTR    //SharpTool.io  SmartTiles
        attribute 'localSunset', sSTR    //SharpTool.io  SmartTiles
        attribute 'percentPrecip', sNUM    //SharpTool.io  SmartTiles
        attribute 'pressured', sSTR        //UNSURE SharpTool.io  SmartTiles
        attribute 'weather', sSTR        //SharpTool.io  SmartTiles
        attribute 'weatherIcon', sSTR    //SharpTool.io  SmartTiles
        attribute 'weatherIcons', sSTR    //Hubitat  openWeather
        attribute 'wind', sNUM            //SharpTool.io
        attribute 'windDirection', sNUM    //Hubitat  OpenWeather
        attribute 'windSpeed', sNUM        //Hubitat  OpenWeather
        attribute 'moonrise', sSTR
        attribute 'moonset', sSTR
        attribute 'moon_phase', sSTR
        
        // Custom API Connection Health Trackers
        attribute 'status', sSTR
        attribute 'lastChecked', sSTR

        
//    The attributes below are sub-groups of optional attributes.  They need to be listed here to be available
//alert
        attribute 'alert', sSTR
        attribute 'alertTile', sSTR
        attribute 'alertDescr', sSTR
        attribute 'alertSender', sSTR
        attribute 'alertDescrFull', sSTR
        
//threedayTile
        attribute 'threedayfcstTile', sSTR

//fcstHighLow
        attribute 'forecastHigh', sNUM
        attribute 'forecastHigh1', sNUM
        attribute 'forecastHigh2', sNUM
        attribute 'forecastLow', sNUM
        attribute 'forecastLow1', sNUM
        attribute 'forecastLow2', sNUM
        attribute 'forecastMorn', sNUM
        attribute 'forecastDay', sNUM
        attribute 'forecastEve', sNUM
        attribute 'forecastNight', sNUM
        attribute 'forecastMorn1', sNUM
        attribute 'forecastDay1', sNUM
        attribute 'forecastEve1', sNUM
        attribute 'forecastNight1', sNUM
        attribute 'forecast_text1', sSTR
        attribute 'forecast_text2', sSTR
        attribute 'condition_icon_url1', sSTR
        attribute 'condition_icon_url2', sSTR

//controlled with localSunrise
        attribute 'tw_begin', sSTR
        attribute 'sunriseTime', sSTR
        attribute 'noonTime', sSTR
        attribute 'sunsetTime', sSTR
        attribute 'tw_end', sSTR

//suncalc
        attribute 'altitude', sNUM // sun angle up from the horizon (0 on your horizon, 90 straight up)
        attribute 'azimuth', sNUM  // sun angle along the horizon (0 is N, 90 East, etc..)

//obspoll
        attribute 'last_poll_Forecast', sSTR // time the poll was initiated
        attribute 'last_observation_Forecast', sSTR  // datestamp of the forecast observation

//precipExtended
        attribute 'rainTomorrow', sNUM
        attribute 'rainDayAfterTomorrow', sNUM
        attribute 'Precip0', sNUM
        attribute 'Precip1', sNUM
        attribute 'Precip2', sNUM
        attribute 'PoP1', sNUM
        attribute 'PoP2', sNUM

//cloudExtended
        attribute 'cloudToday', sNUM
        attribute 'cloudTomorrow', sNUM
        attribute 'cloudDayAfterTomorrow', sNUM

        command 'pollOWMData'
    }

    preferences() {
        String settingDescr = settingEnable ? '<br><i>Hide many of the optional attributes to reduce the clutter, if needed, by turning OFF this toggle.</i><br>' : '<br><i>Many optional attributes are available to you, if needed, by turning ON this toggle.</i><br>'
        section('Query Inputs'){
            input 'apiKey', 'text', required: true, title: 'Type OpenWeatherMap.org API Key Here', defaultValue: null
            input 'apiVer', 'bool', required: true, title: 'API Key Version (2.5 = OFF;  3.0 = ON)', defaultValue: false
            input 'city', 'text', required: true, defaultValue: 'City or Location name forecast area', title: 'City name'
            input 'pollIntervalForecast', 'enum', title: 'External Source Poll Interval (daytime)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'pollIntervalForecastnight', 'enum', title: 'External Source Poll Interval (nighttime)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'dbgEnable', 'bool', title: 'Enable Debug Logging', description: '<i>Debug logging will turn off automatically after 30 minutes. CURRENTLY UNUSED</i>', required: true, defaultValue: false
            input 'txtEnable', 'bool', title: 'Enable Description Text Logging', description: '<i>Info and Description Text logging - Enabled by default</i>', required: true, defaultValue: true
            input 'wrnEnable', 'bool', title: 'Enable Warning Logging', description: '<i>Warning logging is generally useful but can be turned off. It is on by default</i>', required: true, defaultValue: true
            input 'alertSource', 'enum', required: true, defaultValue: sONE, title: 'Weather Alert Source<br>0=None 1=OWM or 2=Weather.gov (US only)', options: [0:sZERO, 1:sONE, 2:sTWO]
            input 'tempFormat', 'enum', required: true, defaultValue: 'Fahrenheit (°F)', title: 'Display Unit - Temperature: Fahrenheit (°F) or Celsius (°C)',  options: ['Fahrenheit (°F)', 'Celsius (°C)']
            input 'TWDDecimals', 'enum', required: true, defaultValue: sZERO, title: 'Display decimals for Temperature & Wind Speed', options: [0:sZERO, 1:sONE, 2:sTWO, 3:'3', 4:'4']
            input 'RDecimals', 'enum', required: true, defaultValue: sZERO, title: 'Display decimals for Precipitation', options: [0:sZERO, 1:sONE, 2:sTWO, 3:'3', 4:'4']
            input 'PDecimals', 'enum', required: true, defaultValue: sZERO, title: 'Display decimals for Pressure', options: [0:sZERO, 1:sONE, 2:sTWO, 3:'3', 4:'4']
            input 'datetimeFormat', 'enum', required: true, defaultValue: sONE, title: 'Display Unit - Date-Time Format',  options: [1:'m/d/yyyy 12 hour (am|pm)', 2:'m/d/yyyy 24 hour', 3:'mm/dd/yyyy 12 hour (am|pm)', 4:'mm/dd/yyyy 24 hour', 5:'d/m/yyyy 12 hour (am|pm)', 6:'d/m/yyyy 24 hour', 7:'dd/mm/yyyy 12 hour (am|pm)', 8:'dd/mm/yyyy 24 hour', 9:'yyyy/mm/dd 24 hour']
            input 'distanceFormat', 'enum', required: true, defaultValue: 'Miles (mph)', title: 'Display Unit - Distance/Speed: Miles, Kilometers, knots or meters',  options: ['Miles (mph)', 'Kilometers (kph)', 'knots', 'meters (m/s)']
            input 'pressureFormat', 'enum', required: true, defaultValue: 'Inches', title: 'Display Unit - Pressure: Inches or Millibar/Hectopascal',  options: ['Inches', 'Millibar', 'Hectopascal']
            input 'rainFormat', 'enum', required: true, defaultValue: 'Inches', title: 'Display Unit - Precipitation: Inches or Millimeters',  options: ['Inches', 'Millimeters']
            input 'luxjitter', 'bool', title: 'Use lux jitter control (rounding)?', required: true, defaultValue: false
            input 'iconLocation', 'text', required: false, defaultValue: 'https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/owm_weather_alerts_driver_custom_OWM-API-2.5-3.0-4/owm-icons/', title: 'Alternative Icon Location:<br><i>blank for default location</i>'
            input 'iconType', 'bool', title: 'Condition Icon/Text for current day on MyTile & Three Day Forecast Tile: On=Current or Off=Forecast', required: true, defaultValue: false
            input 'altCoord', 'bool', required: true, defaultValue: false, title: "Override Hub's location coordinates"
            if (altCoord) {
                input 'altLat', sSTR, title: 'Override location Latitude', required: false, defaultValue: location.latitude.toString(), description: '<br>Enter location Latitude<br>'
                input 'altLon', sSTR, title: 'Override location Longitude', required: false, defaultValue: location.longitude.toString(), description: '<br>Enter location Longitude<br>'
            }
            input 'settingEnable', 'bool', title: '<b>Display All Optional Attributes</b>', description: settingDescr, defaultValue: true
    //build a Selector for each mapped Attribute or group of attributes
            attributesMap.each {
                keyname, attribute ->
                if (settingEnable) {
                    input keyname+'Publish', 'bool', title: attribute.t, required: true, defaultValue: attribute.defa, description: sBR+(String)attribute.d+sBR
                    if(keyname == 'threedayTile') input 'threedayLH', 'bool', title: 'Three Day Temp Display', description: '<br>High/Low: On or Low/High: Off<br>', required: true, defaultValue: false
                    if(keyname == 'weatherSummary') input 'summaryType', 'bool', title: 'Full Weather Summary', description: '<br>Full: on or short: off summary?<br>', required: true, defaultValue: false
                }
            }
            if (settingEnable) {
                input 'windPublish', 'bool', title: 'Wind Speed', required: true, defaultValue: sFLS, description: '<br>Display wind speed<br>'
            }
        }
    }
}


// <<<<<<<<<< Begin Sunrise-Sunset Poll Routines >>>>>>>>>>
void pollSunRiseSet() {
    TimeZone tZ= TimeZone.getDefault()

    Date dnow= new Date()
    String currDate = dnow.format('yyyy-MM-dd', tZ)

    String tfmt1='HH:mm'
    Date tSunrise; tSunrise = (Date)todaysSunrise
    tSunrise = (!tSunrise || tSunrise == null) ? Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 00:00:00") : tSunrise

    Date tSunset; tSunset = (Date)todaysSunset
    if(!tSunset || tSunset == null){
        String currYear = dnow.format('yyyy', tZ)
        Date mar21= Date.parse("yyyy-MM-dd", currYear + '-03-21')
        Date sep21= Date.parse("yyyy-MM-dd", currYear + '-09-21')
        Boolean isBtwn= (dnow >= mar21 && dnow < sep21)
        Date twelve59= Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 23:59:59")
        Date mid01= Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 00:00:01")
        if(altLat.toDouble() > 0.0D) {
            tSunset = isBtwn ? twelve59 : mid01
        } else {
            tSunset = !isBtwn ? twelve59 : mid01
        }
    }
    myUpdData('riseTime', tSunrise.format(tfmt1, tZ))
    myUpdData('noonTime', new Date(tSunrise.getTime() + ((tSunset.getTime() - tSunrise.getTime()).intdiv(2))).format(tfmt1, tZ))
    myUpdData('setTime', tSunset.format(tfmt1, tZ))
    myUpdData('tw_begin', new Date(tSunrise.getTime() - 1773000).format(tfmt1, tZ)) // (29.55*60*1000) 29.55 minutes before sunrise
    myUpdData('tw_end', new Date(tSunset.getTime() + 1773000).format(tfmt1, tZ)) // (29.55*60*1000) 29.55 minutes after sunset
    myUpdData('localSunset', tSunset.format(myGetData('timeFormat'), tZ))
    myUpdData('localSunrise', tSunrise.format(myGetData('timeFormat'), tZ))
    myUpdData('riseTime1', new Date(tSunrise.getTime() + (60*60*24*1000)).format(tfmt1, tZ))
    myUpdData('riseTime2', new Date(tSunrise.getTime() + (60*60*24*1000*2)).format(tfmt1, tZ))
    myUpdData('setTime1', new Date(tSunset.getTime() + (60*60*24*1000)).format(tfmt1, tZ))
    myUpdData('setTime2', new Date(tSunset.getTime() + (60*60*24*1000*2)).format(tfmt1, tZ))
}
// >>>>>>>>>> End Sunrise-Sunset Routines <<<<<<<<<<

// <<<<<<<<<< Begin OWM Poll Routines >>>>>>>>>>
void pollOWMData() {
    if( apiKey == null ) {
    if (wrnEnable) log.warn ('OpenWeatherMap API Key not found.  Please configure in preferences.')
        return
    }

/* for testing a different Lat/Lon location uncommnent the two lines below */
//    String altLat = "44.809122" //"41.5051613" // "40.6" //"38.627003" //"30.6953657"
//    String altLon = "-68.735892" //"-81.6934446" // "-75.43" //"-90.199402" //-88.0398912"

    Map ParamsOWM
    ParamsOWM = [ uri: 'https://api.openweathermap.org/data/' + (apiVer==true ? '3.0' : '2.5') + '/onecall?lat=' + (String)altLat + '&lon=' + (String)altLon + '&exclude=minutely,hourly&mode=json&units=imperial&appid=' + (String)apiKey, timeout: 20 ]
    if (txtEnable) log.info ('Poll OpenWeatherMap.org: ' + ParamsOWM)
    asynchttpGet('pollOWMHandler', ParamsOWM)
}

void pollOWMHandler(resp, data) {
    if (txtEnable) log.info ('Polling OpenWeatherMap.org')
    
    // Capture execution timestamp contextually
    TimeZone tZ = TimeZone.getDefault()
    String timestamp = new Date().format("yyyy-MM-dd HH:mm:ss", tZ)
    sendEvent(name: "lastChecked", value: timestamp)

    if(resp.getStatus() != 200 && resp.getStatus() != 207) {
    if (wrnEnable) log.warn ('Calling https://api.openweathermap.org/data/' + (apiVer==true ? '3.0' : '2.5') + '/onecall?lat=' + (String)altLat + '&lon=' + (String)altLon + '&exclude=minutely,hourly&mode=json&units=imperial&appid=' + (String)apiKey)
    if (wrnEnable) log.warn (resp.getStatus() + sCOLON + resp.getErrorMessage())
        
        sendEvent(name: "status", value: "Error: ${resp.getStatus()}")
    }else{
        sendEvent(name: "status", value: "Success")
        
        Map owm = parseJson(resp.data)
    if (txtEnable) log.info ('OpenWeatherMap Data: ' + owm.toString())
        if(owm.toString()==sNULL) {
            pauseExecution(1000)
            pollOWMData()
            return
        }
        Date fotime = (owm?.current?.dt==null) ? new Date() : new Date((Long)owm.current.dt * 1000L)
        myUpdData('fotime', fotime.toString())
        Date futime = new Date()
        myUpdData('futime', futime.toString())
        myUpdData(sSUMLST, futime.format(myGetData('timeFormat'), tZ).toString())
        myUpdData('Summary_last_poll_date', futime.format(myGetData('dateFormat'), tZ).toString())
        myUpdData('currDate', new Date().format('yyyy-MM-dd', tZ))
        myUpdData('currTime', new Date().format('HH:mm', tZ))
        if(myGetData('riseTime') <= myGetData('currTime') && myGetData('setTime') >= myGetData('currTime')) {
            myUpdData('is_day', sTRU)
        }else{
            myUpdData('is_day', sFLS)
        }
        if(myGetData('currTime') < myGetData('tw_begin') || myGetData('currTime') > myGetData('tw_end')) {
            myUpdData('is_light', sFLS)
        }else{
            myUpdData('is_light', sTRU)
        }
        if(myGetData('is_light') != myGetData('is_lightOld')) {
            if(myGetData('is_light')==sTRU) {
     if (txtEnable) log.info (' Switching to Daytime schedule.')
            }else{
    if (txtEnable) log.info (' Switching to Nighttime schedule.')
            }
            initialize_poll()
            myUpdData('is_lightOld', myGetData('is_light'))
        }
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<

// <<<<<<<<<< Begin Process Standard Weather-Station Variables (Regardless of Forecast Selection)  >>>>>>>>>>
        Integer mult_twd = myGetData('mult_twd')==sNULL ? 1 : myGetData('mult_twd').toInteger()
        Integer mult_p = myGetData('mult_p')==sNULL ? 1 : myGetData('mult_p').toInteger()
        Integer mult_r = myGetData('mult_r')==sNULL ? 1 : myGetData('mult_r').toInteger()
        String ddisp_twd = myGetData('ddisp_twd')==sNULL ? '%3.0f' : myGetData('ddisp_twd')

        Boolean isF = myGetData(sTMETR) == sDF

        BigDecimal t_dew = owm?.current?.dew_point
        myUpdData('dewpoint', adjTemp(t_dew, isF, mult_twd))
        myUpdData('humidity', (Math.round((owm?.current?.humidity==null ? 0.00 : owm.current.humidity.toBigDecimal()) * 10) / 10).toString())

        BigDecimal t_press; t_press = owm?.current?.pressure==null ? 0.00 : owm.current.pressure.toBigDecimal()
        if(myGetData(sPMETR) == 'inHg') {
            t_press = Math.round(t_press * 0.029529983071445 * mult_p) / mult_p
        }else{
            t_press = Math.round(t_press * mult_p) / mult_p
        }
        myUpdData('pressure', t_press.toString())

        myUpdData(sTEMP, adjTemp(owm?.current?.temp, isF, mult_twd))

        String w_string_bft,w_bft_icon
        w_string_bft=sNULL
        w_bft_icon=sNULL
        BigDecimal t_ws = owm?.current?.wind_speed==null ? 0.00 : owm.current.wind_speed.toBigDecimal()
        if(t_ws < 1.0) {
            w_string_bft = 'Calm'; w_bft_icon = 'wb0.png'
        }else if(t_ws < 4.0) {
            w_string_bft = 'Light air'; w_bft_icon = 'wb1.png'
        }else if(t_ws < 8.0) {
            w_string_bft = 'Light breeze'; w_bft_icon = 'wb2.png'
        }else if(t_ws < 13.0) {
            w_string_bft = 'Gentle breeze'; w_bft_icon = 'wb3.png'
        }else if(t_ws < 19.0) {
            w_string_bft = 'Moderate breeze'; w_bft_icon = 'wb4.png'
        }else if(t_ws < 25.0) {
            w_string_bft = 'Fresh breeze'; w_bft_icon = 'wb5.png'
        }else if(t_ws < 32.0) {
            w_string_bft = 'Strong breeze'; w_bft_icon = 'wb6.png'
        }else if(t_ws < 39.0) {
            w_string_bft = 'High wind, moderate gale, near gale'; w_bft_icon = 'wb7.png'
        }else if(t_ws < 47.0) {
            w_string_bft = 'Gale, fresh gale'; w_bft_icon = 'wb8.png'
        }else if(t_ws < 55.0) {
            w_string_bft = 'Strong/severe gale'; w_bft_icon = 'wb9.png'
        }else if(t_ws < 64.0) {
            w_string_bft = 'Storm, whole gale'; w_bft_icon = 'wb10.png'
        }else if(t_ws < 73.0) {
            w_string_bft = 'Violent storm'; w_bft_icon = 'wb11.png'
        }else if(t_ws >= 73.0) {
            w_string_bft = 'Hurricane force'; w_bft_icon = 'wb12.png'
        }
        myUpdData('wind_string_bft', w_string_bft)
        myUpdData('wind_bft_icon', w_bft_icon)

        BigDecimal t_wd,t_wg
        t_wd = owm?.current?.wind_speed==null ? 0.00 : owm.current.wind_speed.toBigDecimal()
        t_wg = owm?.current?.wind_gust==null ? t_wd : owm.current.wind_gust.toBigDecimal()
        if(myGetData(sDMETR) == 'MPH') {
            t_wd = Math.round(t_wd * mult_twd) / mult_twd
            t_wg = Math.round(t_wg * mult_twd) / mult_twd
        } else if(myGetData(sDMETR) == 'KPH') {
            t_wd = Math.round(t_wd * 1.609344 * mult_twd) / mult_twd
            t_wg = Math.round(t_wg * 1.609344 * mult_twd) / mult_twd
        } else if(myGetData(sDMETR) == 'knots') {
            t_wd = Math.round(t_wd * 0.868976 * mult_twd) / mult_twd
            t_wg = Math.round(t_wg * 0.868976 * mult_twd) / mult_twd
        }else{  //  this leave only m/s
            t_wd = Math.round(t_wd * 0.44704 * mult_twd) / mult_twd
            t_wg = Math.round(t_wg * 0.44704 * mult_twd) / mult_twd
        }
        myUpdData('wind', t_wd.toString())
        myUpdData('wind_gust', t_wg.toString())

        BigDecimal twb = owm?.current?.wind_deg==null ? 0.00 : owm.current.wind_deg.toBigDecimal()
        myUpdData('wind_degree', twb.toInteger().toString())
        String w_cardinal,w_direction
        w_cardinal=sNULL
        w_direction=sNULL
        if(twb < 11.25) {
            w_cardinal = 'N'; w_direction = 'North'
        }else if(twb < 33.75) {
            w_cardinal = 'NNE'; w_direction = 'North-Northeast'
        }else if(twb < 56.25) {
            w_cardinal = 'NE';  w_direction = 'Northeast'
        }else if(twb < 78.75) {
            w_cardinal = 'ENE'; w_direction = 'East-Northeast'
        }else if(twb < 101.25) {
            w_cardinal = 'E'; w_direction = 'East'
        }else if(twb < 123.75) {
            w_cardinal = 'ESE'; w_direction = 'East-Southeast'
        }else if(twb < 146.25) {
            w_cardinal = 'SE'; w_direction = 'Southeast'
        }else if(twb < 168.75) {
            w_cardinal = 'SSE'; w_direction = 'South-Southeast'
        }else if(twb < 191.25) {
            w_cardinal = 'S'; w_direction = 'South'
        }else if(twb < 213.75) {
            w_cardinal = 'SSW'; w_direction = 'South-Southwest'
        }else if(twb < 236.25) {
            w_cardinal = 'SW'; w_direction = 'Southwest'
        }else if(twb < 258.75) {
            w_cardinal = 'WSW'; w_direction = 'West-Southwest'
        }else if(twb < 281.25) {
            w_cardinal = 'W'; w_direction = 'West'
        }else if(twb < 303.75) {
            w_cardinal = 'WNW'; w_direction = 'West-Northwest'
        }else if(twb < 326.25) {
            w_cardinal = 'NW'; w_direction = 'Northwest'
        }else if(twb < 348.75) {
            w_cardinal = 'NNW'; w_direction = 'North-Northwest'
        }else if(twb >= 348.75) {
            w_cardinal = 'N'; w_direction = 'North'
        }
        myUpdData('wind_direction', w_direction)
        myUpdData('wind_cardinal', w_cardinal)
        myUpdData('wind_string', w_string_bft + ' from the ' + myGetData('wind_direction') + (myGetDataBD('wind') < 1.0 ? sBLK: ' at ' + String.format(ddisp_twd, myGetDataBD('wind')) + sSPC + myGetData(sDMETR)))
// >>>>>>>>>> End Process Standard Weather-Station Variables (Regardless of Forecast Selection)  <<<<<<<<<<

        Integer cloudCover = owm?.current?.clouds==null ? 1 : owm.current.clouds <= 1 ? 1 : owm.current.clouds
        myUpdData('cloud', cloudCover.toString())
        myUpdData('vis', (myGetData(sDMETR)!='MPH' ? Math.round(owm?.current?.visibility==null ? 0.01 : owm.current.visibility.toBigDecimal() * 0.001 * mult_twd) / mult_twd : Math.round(owm?.current?.visibility==null ? 0.00 : owm.current.visibility.toBigDecimal() * 0.0006213712 * mult_twd) / mult_twd).toString())

        List owmCweat = owm?.current?.weather
        myUpdData('condition_id', owmCweat==null || owmCweat[0]?.id==null ? '999' : owmCweat[0].id.toString())
        myUpdData('condition_code', getCondCode(myGetData('condition_id').toInteger(), myGetData('is_day')))
        myUpdData('OWN_icon', owmCweat == null || owmCweat[0]?.icon==null ? (myGetData('is_day')==sTRU ? '50d' : '50n') : owmCweat[0].icon)

        List<Map> owmDaily
        owmDaily = owm?.daily != null && ((List)owm.daily)[0]?.weather != null ? ((List)owm?.daily)[0].weather : null
        myUpdData('forecast_id', owmDaily==null || owmDaily[0]?.id==null ? '999' : owmDaily[0].id.toString())
        myUpdData('forecast_code', getCondCode(myGetData('forecast_id').toInteger(), sTRU))
        myUpdData('forecast_text', owmDaily==null || owmDaily[0]?.description==null ? 'Unknown' : owmDaily[0].description.capitalize())

        myUpdData('condition_text', myGetData('iconType')== sTRU ? (owmCweat==null || owmCweat[0]?.description==null ? 'Unknown' : owmCweat[0].description.capitalize()): (owm?.daily==null || owm?.daily[0]?.weather[0]?.description==null ? 'Unknown' : owm.daily[0].weather[0].description.capitalize()))

        owmDaily = owm?.daily != null ? (List)owm.daily : null
        BigDecimal t_p0 = (owmDaily==null || !owmDaily[0]?.rain ? 0.00 : owmDaily[0].rain.toBigDecimal()) + (owmDaily==null || !owmDaily[0]?.snow ? 0.00 : owmDaily[0].snow.toBigDecimal())
        myUpdData('rainToday', (Math.round((myGetData(sRMETR) == 'in' ? t_p0 * 0.03937008 : t_p0) * mult_r) / mult_r).toString())
        myUpdData('PoP', (!owmDaily[0].pop ? 0 : Math.round(owmDaily[0].pop.toBigDecimal() * 100.toInteger())).toString())
        myUpdData('percentPrecip', myGetData('PoP'))

        owmDaily = owm?.daily != null ? (List)owm.daily : null
        Date moonrise = (owmDaily[0].moonrise==null) ? new Date() : new Date((Long)owmDaily[0].moonrise * 1000L)
        myUpdData('moonrise', moonrise.toString())
        Date moonset = (owmDaily[0].moonset==null) ? new Date() : new Date((Long)owmDaily[0].moonset * 1000L)
        myUpdData('moonset', moonset.toString())
        String mPhase
        BigDecimal tma = !owmDaily[0]?.moon_phase ? 0.00 : owmDaily[0].moon_phase.toBigDecimal()
        if (tma < 0.0625) {mPhase = 'New Moon'}
        if (tma >= 0.0625 && tma < 0.1875) {mPhase = 'Waxing Crescent'}
        if (tma >= 0.1875 && tma < 0.3125) {mPhase = 'First Quarter'}
        if (tma >= 0.3125 && tma < 0.4375) {mPhase = 'Waxing Gibbous'}
        if (tma >= 0.4375 && tma < 0.5625) {mPhase = 'Full Moon'}
        if (tma >= 0.5625 && tma < 0.6875) {mPhase = 'Waning Gibbous'}
        if (tma >= 0.6875 && tma < 0.8125) {mPhase = 'Last Quarter'}
        if (tma >= 0.8125 && tma < 0.9375) {mPhase = 'Waning Cresent'}
        if (tma >= 0.9375) {mPhase = 'New Moon'}
        myUpdData('moon_phase', mPhase)
        
        if(owmDaily && (threedayTilePublish || precipExtendedPublish || myTilePublish)) {
            BigDecimal t_p1 = (owmDaily==null || !owmDaily[1]?.rain ? 0.00 : owmDaily[1].rain.toBigDecimal()) + (owmDaily==null || !owmDaily[1]?.snow ? 0.00 : owmDaily[1].snow.toBigDecimal())
            BigDecimal t_p2 = (owmDaily==null || !owmDaily[2]?.rain ? 0.00 : owmDaily[2].rain.toBigDecimal()) + (owmDaily==null || !owmDaily[2]?.snow ? 0.00 : owmDaily[2].snow.toBigDecimal())
            myUpdData('Precip0', (Math.round((myGetData(sRMETR) == 'in' ? t_p0 * 0.03937008 : t_p0) * mult_r) / mult_r).toString())
            myUpdData('Precip1', (Math.round((myGetData(sRMETR) == 'in' ? t_p1 * 0.03937008 : t_p1) * mult_r) / mult_r).toString())
            myUpdData('Precip2', (Math.round((myGetData(sRMETR) == 'in' ? t_p2 * 0.03937008 : t_p2) * mult_r) / mult_r).toString())
            myUpdData('PoP1', (!owmDaily[1].pop ? 0 : Math.round(owmDaily[1].pop.toBigDecimal() * 100.toInteger())).toString())
        }
    }
}