/* OpenWeatherMap-Alerts Weather Driver (Custom)            
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

    Last Update 06/23/2026
*/

//file:noinspection GroovyUnusedAssignment
//file:noinspection SpellCheckingInspection
//file:noinspection unused
//file:noinspection GroovyAssignabilityCheck
//file:noinspection GrDeprecatedAPIUsage

static String version()    {  return '0.7.4'  }
import groovy.transform.Field

metadata {
    definition (name: 'OpenWeatherMap-Alerts Weather Driver (Custom)',
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

        // New Device Health Attributes
        attribute 'status', 'string'
        attribute 'lastChecked', 'string'

        command 'pollData'
    }

    preferences() {
        String settingDescr = settingEnable ? '<br><i>Hide many of the optional attributes to reduce the clutter, if needed, by turning OFF this toggle.</i><br>' : '<br><i>Many optional attributes are available to you, if needed, by turning ON this toggle.</i><br>'
        section('Query Inputs'){
            input 'apiKey', 'text', required: true, title: 'Type OpenWeatherMap.org API Key Here', defaultValue: null
            input 'city', 'text', required: true, defaultValue: 'City or Location name forecast area', title: 'City name'
            input 'pollIntervalForecast', 'enum', title: 'External Source Poll Interval (daytime)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'pollIntervalForecastnight', 'enum', title: 'External Source Poll Interval (nighttime)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'txtEnable', 'bool', title: 'Enable Extended Logging', description: '<i>Extended logging will turn off automatically after 30 minutes.</i>', required: true, defaultValue: false
            input 'alertSource', 'enum', required: true, defaultValue: sONE, title: 'Weather Alert