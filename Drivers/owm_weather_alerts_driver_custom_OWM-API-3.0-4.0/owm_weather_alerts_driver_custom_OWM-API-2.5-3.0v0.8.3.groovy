/* OpenWeatherMap-Alerts Weather Driver (Custom 3.0/4.0 OWM API)            
    Import URL: https://raw.githubusercontent.com/HubitatCommunity/OpenWeatherMap-Alerts-Weather-Driver/master/OpenWeatherMap-Alerts%2520Weather%2520Driver.groovy
    Copyright 2023 @Matthew (Scottma61)

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

    Last Update 06/24/2026
*/
/*
 * 06-24-2026       v0.8.3        jshimota    Prepping cleaning - changed driver name
 * 06-23-2026       v0.8.2        jshimota    Switched sunrise/sunset calculation to prefer OWM polled API payloads.
 * 06-23-2026       v0.8.1        jshimota    Fixing more bugs with Gemini help
 * 06-23-2026        v0.8.0    jshimota    Added Hub Location Coordinates choice vs Manual coordinates input toggle.
 * 06-23-2026        v0.7.9    jshimota    Added useApiV4 toggle to preferences (defaulting to 3.0).
 * 06-23-2026        v0.7.8    jshimota    Fixed 4.0 URI to standard 3.0 path, resolved capability gaps, added safe method stubs.
 * 06-23-2026        v0.7.7    jshimota    Another big bug in legacy garbage for 'irreInstalled'.
 * 06-23-2026        v0.7.6    jshimota    found big bug - changed pollData to pollOWMData
 * 06-23-2026        v0.7.5    jshimota    Added Status and last checked attributes
*/

static String version()    {  return '0.8.3'  }
import groovy.transform.Field

@Field static final Map attributesMap = [
    'weatherSummary': [ty: 'string', t: 'Weather Summary', defa: true, d: 'Publish weather summary text'],
    'threedayTile': [ty: 'string', t: 'Three Day Forecast Tile', defa: true, d: 'Publish 3-day forecast tile']
]

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
    definition (name: 'OpenWeatherMap-Alerts Weather Driver (Custom 3.0/4.0 OWM API)',
        namespace: 'jshimota',
        author: 'Scottma61',
        importUrl: 'https://raw.githubusercontent.com/HubitatCommunity/OpenWeatherMap-Alerts-Weather-Driver/master/OpenWeatherMap-Alerts%2520Weather%2520Driver.groovy') {

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

        attribute 'city', sSTR            
        attribute 'feelsLike', sNUM        
        attribute 'forecastIcon', sSTR    
        attribute 'localSunrise', sSTR    
        attribute 'localSunset', sSTR    
        attribute 'percentPrecip', sNUM    
        attribute 'pressured', sSTR        
        attribute 'weather', sSTR        
        attribute 'weatherIcon', sSTR    
        attribute 'weatherIcons', sSTR    
        attribute 'wind', sNUM            
        attribute 'windDirection', sNUM    
        attribute 'windSpeed', sNUM        
        attribute 'moonrise', sSTR
        attribute 'moonset', sSTR
        attribute 'moon_phase', sSTR

        // Alert attributes
        attribute 'alert', sSTR
        attribute 'alertTile', sSTR
        attribute 'alertDescr', sSTR
        attribute 'alertSender', sSTR
        attribute 'alertDescrFull', sSTR
        
        // Tiles & Forecasts
        attribute 'threedayfcstTile', sSTR
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

        // Solar / Astronomical
        attribute 'tw_begin', sSTR
        attribute 'sunriseTime', sSTR
        attribute 'noonTime', sSTR
        attribute 'sunsetTime', sSTR
        attribute 'tw_end', sSTR
        attribute 'altitude', sNUM 
        attribute 'azimuth', sNUM  

        // Metadata tracking
        attribute 'last_poll_Forecast', sSTR 
        attribute 'last_observation_Forecast', sSTR  

        // Extended metrics
        attribute 'rainTomorrow', sNUM
        attribute 'rainDayAfterTomorrow', sNUM
        attribute 'Precip0', sNUM
        attribute 'Precip1', sNUM
        attribute 'Precip2', sNUM
        attribute 'PoP1', sNUM
        attribute 'PoP2', sNUM
        attribute 'cloudToday', sNUM
        attribute 'cloudTomorrow', sNUM
        attribute 'cloudDayAfterTomorrow', sNUM

        attribute 'status', 'string'
        attribute 'lastChecked', 'string'

        command 'pollOWMData'
    }

    preferences() {
        String settingDescr = (settings?.settingEnable == true) ? '<br><i>Hide many of the optional attributes to reduce the clutter, if needed, by turning OFF this toggle.</i><br>' : '<br><i>Many optional attributes are available to you, if needed, by turning ON this toggle.</i><br>'
        section('Query Inputs'){
            input 'apiKey', 'text', required: true, title: 'Type OpenWeatherMap.org API Key Here', defaultValue: null
            input 'city', 'text', required: true, defaultValue: 'City or Location name forecast area', title: 'City name'
            
            input 'useApiV4', 'bool', title: '<b>Use OneCall API v4.0</b>', description: '<i>Default is OFF (uses standard v3.0). Toggle ON only if required by your OWM account plan tier.</i>', defaultValue: false
            
            // NEW LOCATION METHOD SELECTION PREFERENCE
            input 'coordSource', 'enum', title: '<b>Coordinates Location Source</b>', required: true, defaultValue: 'Hub', options: ['Hub': 'Use Hub Location Coordinates', 'Custom': 'Override with custom values']
            
            if (settings?.coordSource == 'Custom') {
                input 'altLat', sSTR, title: 'Override location Latitude', required: true, defaultValue: location.latitude ? location.latitude.toString() : "", description: '<br>Enter location Latitude<br>'
                input 'altLon', sSTR, title: 'Override location Longitude', required: true, defaultValue: location.longitude ? location.longitude.toString() : "", description: '<br>Enter location Longitude<br>'
            }
            
            input 'pollIntervalForecast', 'enum', title: 'External Source Poll Interval (daytime)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'pollIntervalForecastnight', 'enum', title: 'External Source Poll Interval (nighttime)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'txtEnable', 'bool', title: 'Enable Extended Logging', description: '<i>Extended logging will turn off automatically after 30 minutes.</i>', required: true, defaultValue: false
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
            input 'iconLocation', 'text', required: false, defaultValue: '', title: 'Alternative Icon Location:<br><i>blank for default location</i>'
            input 'iconType', 'bool', title: 'Condition Icon/Text for current day on MyTile & Three Day Forecast Tile: On=Current or Off=Forecast', required: true, defaultValue: false
            
            input 'settingEnable', 'bool', title: '<b>Display All Optional Attributes</b>', description: settingDescr, defaultValue: true
            
            attributesMap.each {
                keyname, attribute ->
                if (settings?.settingEnable == true) {
                    input keyname+'Publish', 'bool', title: attribute.t, required: true, defaultValue: attribute.defa, description: sBR+(String)attribute.d+sBR
                    if(keyname == 'threedayTile') input 'threedayLH', 'bool', title: 'Three Day Temp Display', description: '<br>High/Low: On or Low/High: Off<br>', required: true, defaultValue: false
                    if(keyname == 'weatherSummary') input 'summaryType', 'bool', title: 'Full Weather Summary', description: '<br>Full: on or short: off summary?<br>', required: true, defaultValue: false
                }
            }
            if (settings?.settingEnable == true) {
                input 'windPublish', 'bool', title: 'Wind Speed', required: true, defaultValue: false, description: '<br>Display wind speed<br>'
            }
        }
    }
}

void refresh() {
    if (txtEnable) log.info "Refresh command received: Polling weather data."
    pollOWMData()
}

void pollSunRiseSet(Map owm = null) {
    TimeZone tZ= TimeZone.getDefault()

    Date dnow= new Date()
    String currDate = dnow.format('yyyy-MM-dd', tZ)

    String tfmt1='HH:mm'
    
    Date tSunrise = null
    if (owm?.current?.sunrise) {
        tSunrise = new Date((Long)owm.current.sunrise * 1000L)
    } else {
        tSunrise = location.sunrise
    }
    tSunrise = (!tSunrise || tSunrise == null) ? Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 00:00:00") : tSunrise

    Date tSunset = null
    if (owm?.current?.sunset) {
        tSunset = new Date((Long)owm.current.sunset * 1000L)
    } else {
        tSunset = location.sunset
    }
    
    if(!tSunset || tSunset == null){
        String currYear = dnow.format('yyyy', tZ)
        Date mar21= Date.parse("yyyy-MM-dd", currYear + '-03-21')
        Date sep21= Date.parse("yyyy-MM-dd", currYear + '-09-21')
        Boolean isBtwn= (dnow >= mar21 && dnow < sep21)
        Date twelve59= Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 23:59:59")
        Date mid01= Date.parse("yyyy-MM-dd hh:mm:ss", currDate + " 00:00:01")
        
        Double latVal = (coordSource == 'Custom' && settings.altLat) ? settings.altLat.toDouble() : (location.latitude ? location.latitude.toDouble() : 0.0D)
        if(latVal > 0.0D) {
            tSunset = isBtwn ? twelve59 : mid01
        } else {
            tSunset = !isBtwn ? twelve59 : mid01
        }
    }
    myUpdData('riseTime', tSunrise.format(tfmt1, tZ))
    myUpdData('noonTime', new Date(tSunrise.getTime() + ((tSunset.getTime() - tSunrise.getTime()).intdiv(2))).format(tfmt1, tZ))
    myUpdData('setTime', tSunset.format(tfmt1, tZ))
    myUpdData('tw_begin', new Date(tSunrise.getTime() - 1773000).format(tfmt1, tZ)) 
    myUpdData('tw_end', new Date(tSunset.getTime() + 1773000).format(tfmt1, tZ)) 
    myUpdData('localSunset', tSunset.format(myGetData('timeFormat') ?: "HH:mm", tZ))
    myUpdData('localSunrise', tSunrise.format(myGetData('timeFormat') ?: "HH:mm", tZ))
    myUpdData('riseTime1', new Date(tSunrise.getTime() + (60*60*24*1000)).format(tfmt1, tZ))
    myUpdData('riseTime2', new Date(tSunrise.getTime() + (60*60*24*1000*2)).format(tfmt1, tZ))
    myUpdData('setTime1', new Date(tSunset.getTime() + (60*60*24*1000)).format(tfmt1, tZ))
    myUpdData('setTime2', new Date(tSunset.getTime() + (60*60*24*1000*2)).format(tfmt1, tZ))
}

void pollOWMData() {
    if( apiKey == null ) {
        LOGWARN('OpenWeatherMap API Key not found. Please configure in preferences.')
        return
    }

    String apiVer = (useApiV4 == true) ? '4.0' : '3.0'
    
    String lat = (coordSource == 'Custom' && settings.altLat) ? settings.altLat : (location.latitude ? location.latitude.toString() : "")
    String lon = (coordSource == 'Custom' && settings.altLon) ? settings.altLon : (location.longitude ? location.longitude.toString() : "")

    if (lat == "" || lon == "") {
        LOGWARN("Latitude or Longitude coordinate properties could not be found. Check your setting parameters.")
        return
    }

    Map ParamsOWM
    if( useApiV4 ) {
        ParamsOWM = [ uri: 'https://api.openweathermap.org/data/' + apiVer + '/onecall/current?lat=' + lat + '&lon=' + lon + '&exclude=minutely,hourly&mode=json&units=imperial&appid=' + (String)apiKey, timeout: 20 ]
    } else {
        ParamsOWM = [ uri: 'https://api.openweathermap.org/data/' + apiVer + '/onecall?lat=' + lat + '&lon=' + lon + '&exclude=minutely,hourly&mode=json&units=imperial&appid=' + (String)apiKey, timeout: 20 ]
    }
    LOGINFO('Poll OpenWeatherMap.org: ' + ParamsOWM)
    asynchttpGet('pollOWMHandler', ParamsOWM)
}

void pollOWMHandler(resp, data) {
    LOGINFO('Polling OpenWeatherMap.org')
    
    String timestamp = new Date().format(myGetData('timeFormat') ?: "yyyy-MM-dd HH:mm:ss", TimeZone.getDefault())
    
    if(resp.getStatus() != 200 && resp.getStatus() != 207) {
        log.info "OpenWeatherMap Poll Failed. Status: ${resp.getStatus()} - Error: ${resp.getErrorMessage()}"
        
        sendEvent(name: "status", value: "Failed (${resp.getStatus()})", descriptionText: "OpenWeatherMap poll failed with status ${resp.getStatus()}")
        sendEvent(name: "lastChecked", value: timestamp, descriptionText: "Last poll attempt timestamp")
        
        LOGWARN(resp.getStatus() + sCOLON + resp.getErrorMessage())
    }else{
        log.info "OpenWeatherMap Poll Successful. Status: ${resp.getStatus()}"
        
        sendEvent(name: "status", value: "Success", descriptionText: "OpenWeatherMap poll completed successfully")
        sendEvent(name: "lastChecked", value: timestamp, descriptionText: "Last successful poll timestamp")
        
        Map owm = parseJson(resp.data)
        LOGINFO('OpenWeatherMap Data: ' + owm.toString())
        if(owm.toString()==sNULL) {
            log.info "OpenWeatherMap returned null data; re-polling..."
            sendEvent(name: "status", value: "Null Data Response")
            pauseExecution(1000)
            pollOWMData() 
            return
        }
        
        // Compute astronomical data from API parsed data before checking limits
        pollSunRiseSet(owm)

        Date fotime = (owm?.current?.dt==null) ? new Date() : new Date((Long)owm.current.dt * 1000L)
        myUpdData('fotime', fotime.toString())
        Date futime = new Date()
        myUpdData('futime', futime.toString())
        TimeZone tZ= TimeZone.getDefault()
        myUpdData(sSUMLST, futime.format(myGetData('timeFormat') ?: "HH:mm", tZ).toString())
        myUpdData('Summary_last_poll_date', futime.format(myGetData('dateFormat') ?: "yyyy-MM-dd", tZ).toString())
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
                LOGINFO(' Switching to Daytime schedule.')
            }else{
                LOGINFO(' Switching to Nighttime schedule.')
            }
            initialize_poll()
            myUpdData('is_lightOld', myGetData('is_light'))
        }

        // Map decimal selections safely from settings
        Integer twdDec = settings.TWDDecimals != null ? settings.TWDDecimals.toInteger() : 0
        Integer pDec = settings.PDecimals != null ? settings.PDecimals.toInteger() : 0
        Integer mult_twd = Math.pow(10, twdDec).toInteger()
        Integer mult_p = Math.pow(10, pDec).toInteger()

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
            t_wd = Math.round(t_wd * mult_twd) 
        }
    }
}

// ── SAFE FALLBACK STUBS FOR WRAPPER FUNCTIONS ──
void myUpdData(String key, String val) {
    updateDataValue(key, val)
}

String myGetData(String key) {
    return getDataValue(key) ?: sNULL
}

String adjTemp(val, Boolean isF, Integer mult) {
    if(val == null) return "0.0"
    return (Math.round(val.toBigDecimal() * mult) / mult).toString()
}

void initialize_poll() {
    if (txtEnable) log.info "Re-initializing polling schedules..."
}

void LOGINFO(String msg) {
    if (txtEnable) log.info "${msg}"
}

void LOGWARN(String msg) {
    log.warn "${msg}"
}