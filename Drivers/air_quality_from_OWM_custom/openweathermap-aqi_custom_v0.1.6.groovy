/*
	OpenWeatherMap-Air Quality - Detailed
    Modified from Byrin's original

0.1.6 - cleaned debug and text logging JAS
0.1.5 - Added user editable lat/long, user editable tile width
0.1.4 - Added AQI tile
0.1.3 - Changed the output. Now default output is AirQuality in US format. 2022-10-04
0.1.2 - Fixed "then" statement. (I'm getting too old for this.") 2022-10-03
0.1.1 - Changed Hex value returned to have # in front. Easier to assign to a light. 2022-10-01
*/
static String version()	{  return '0.1.6'  }
import groovy.transform.Field


metadata {
    definition (name: "OpenWeatherMap-Air Quality-custom", namespace: "James Shimota", author: "SJ") {
        capability "AirQuality"
        attribute "CO", "float"
        attribute "NO", "float"
        attribute "NO2", "float"
        attribute "O3", "float"
        attribute "SO2", "float"
        attribute "PM2.5", "float"
        attribute "PM10", "float"
        attribute "NH3", "float"
        // attribute "AQI", "int"
        attribute "AQIColorCode", "string"
        attribute "PrimaryFactor", "string"
        attribute "AlertTile", "string"

        command 'pollData'
    }
    preferences {
        input 'apiKey', 'text', required: true, title: 'Type OpenWeatherMap.org API Key Here', defaultValue: null
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input 'locLat', 'text', title: 'Location Latitude', defaultValue: location.latitude
        input 'locLon', 'text', title: 'Location Longitude', defaultValue: location.longitude
        input 'tileWidth', 'int', title: 'Tile width', defaultValue: '125'
    }
}

void pollAQI() {
    if( apiKey == null ) {
        return
    }
    Map ParamsAQI
    ParamsAQI = [
            uri: 'https://api.openweathermap.org/data/2.5/air_pollution?lat=' + (String)locLat + '&lon=' + (String)locLon + '&appid=' + (String)apiKey,
            timeout: 20 ]
    if (dbgEnable) log.debug "ParamsAQI:${ParamsAQI}"
    asynchttpGet('pollAQIHandler', ParamsAQI)
}

void pollAQIHandler(resp, data) {
    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
        Map aqi = parseJson(resp.data)
        if(aqi.toString()==sNULL) {
            pauseExecution(30000) //5 minute pause
            pollAQI()
            return
        }
        def name = 'airQualityIndex'
        def value = aqi.list[0].main.aqi

        def descriptionText = "${device.displayName} ${name} is ${value}"
//		if (txtEnable) log.info "${descriptionText}"
//		sendEvent(name: name,value: value,descriptionText: descriptionText,unit: unit)

        // Here begins my modification (SJ)
        def int aQItemp =0
        def int aQI = 0
        def aQIfactor = "none"
        name = 'CO'
        value = aqi.list[0].components.co
        def convertedVal = Math.round(value/114.5)/10
        if (convertedVal<4.5){
            aQItemp = (50/4.4)*(convertedVal)
        }else if (convertedVal<9.5){
            aQItemp = (49/4.9)*(convertedVal-4.5)+51
        }else if (convertedVal<12.5){
            aQItemp = (49/2.9)*(convertedVal-9.5)+101
        }else if (convertedVal<15.5){
            aQItemp = (49/2.9)*(convertedVal-12.5)+151
        }else if (convertedVal<30.5){
            aQItemp = (99/14.9)*(convertedVal-15.5)+201
        }else if (convertedVal<40.5){
            aQItemp = (99/9.9)*(convertedVal-30.5)+301
        }else {
            aQItemp = (99/9.9)*(convertedVal-40.5)+401
        }
        aQI=aQItemp
        aQIfactor = name

        descriptionText = "${device.displayName} ${name} is ${convertedVal} AQI ${aQItemp}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: name,value: convertedVal,descriptionText: descriptionText,unit: "ppm")

        name = 'NO2'
        value = aqi.list[0].components.no2
        convertedVal = Math.round(value/1.88)
        if (convertedVal<54){
            aQItemp = (50/53)*(convertedVal)
        }else if (convertedVal<101){
            aQItemp = (49/46)*(convertedVal-54)+51
        }else if (convertedVal<361){
            aQItemp = (49/259)*(convertedVal-101)+101
        }else if (convertedVal<650){
            aQItemp = (49/288)*(convertedVal-361)+151
        }else if (convertedVal<1250){
            aQItemp = (99/599)*(convertedVal-650)+201
        }else if (convertedVal<1650){
            aQItemp = (99/399)*(convertedVal-1250)+301
        }else {
            aQItemp = (99/399)*(convertedVal-1650)+401
        }
        if (aQItemp>aQI){
            aQI=aQItemp
            aQIfactor = name
        }
        descriptionText = "${device.displayName} ${name} is ${convertedVal} AQI ${aQItemp}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: name,value: convertedVal,descriptionText: descriptionText,unit: "ppb")

        name = 'O3'
        value = aqi.list[0].components.o3
        convertedVal = Math.round(value/2)/1000
        if (convertedVal<0.055){
            aQItemp = (50/0.054)*(convertedVal)
        }else if (convertedVal<0.071){
            aQItemp = (49/0.15)*(convertedVal-0.054)+51
        }else if (convertedVal<0.165){
            aQItemp = (49/0.093)*(convertedVal-0.071)+101
        }else if (convertedVal<0.205){
            aQItemp = (49/0.039)*(convertedVal-0.165)+151
        }else if (convertedVal<0.405){
            aQItemp = (99/0.199)*(convertedVal-0.205)+201
        }else if (convertedVal<0.505){
            aQItemp = (99/0.099)*(convertedVal-0.405)+301
        }else {
            aQItemp = (99/0.099)*(convertedVal-0.505)+401
        }
        if (aQItemp>aQI){
            aQI=aQItemp
            aQIfactor = name
        }
        descriptionText = "${device.displayName} ${name} is ${convertedVal} AQI ${aQItemp}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: name,value: convertedVal,descriptionText: descriptionText,unit: "ppm")

        name = 'SO2'
        value = aqi.list[0].components.so2
        convertedVal = Math.round(value/2.62)
        if (convertedVal<36){
            aQItemp = (50/35)*(convertedVal)
        }else if (convertedVal<76){
            aQItemp = (49/39)*(convertedVal-36)+51
        }else if (convertedVal<186){
            aQItemp = (49/109)*(convertedVal-76)+101
        }else if (convertedVal<305){
            aQItemp = (49/118)*(convertedVal-186)+151
        }else if (convertedVal<605){
            aQItemp = (99/299)*(convertedVal-305)+201
        }else if (convertedVal<805){
            aQItemp = (99/199)*(convertedVal-605)+301
        }else {
            aQItemp = (99/199)*(convertedVal-805)+401
        }
        if (aQItemp>aQI){
            aQI=aQItemp
            aQIfactor = name
        }
        descriptionText = "${device.displayName} ${name} is ${convertedVal} AQI ${aQItemp}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: name,value: convertedVal,descriptionText: descriptionText,unit: "ppb")

        name = 'PM2.5'
        value = Math.round(aqi.list[0].components.pm2_5*10)/10
        convertedVal = value
        if (convertedVal<12.1){
            aQItemp = (50/12.0)*(convertedVal)
        }else if (convertedVal<35.5){
            aQItemp = (49/23.3)*(convertedVal-12.1)+51
        }else if (convertedVal<55.5){
            aQItemp = (49/19.9)*(convertedVal-35.5)+101
        }else if (convertedVal<150.5){
            aQItemp = (49/94.9)*(convertedVal-55.5)+151
        }else if (convertedVal<250.5){
            aQItemp = (99/99.9)*(convertedVal-150.5)+201
        }else if (convertedVal<350.5){
            aQItemp = (99/99.9)*(convertedVal-250.5)+301
        }else {
            aQItemp = (99/149.9)*(convertedVal-350.5)+401
        }
        if (aQItemp>aQI){
            aQI=aQItemp
            aQIfactor = name
        }
        descriptionText = "${device.displayName} ${name} is ${convertedVal} AQI ${aQItemp}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: name,value: value,descriptionText: descriptionText,unit: "mg/m^3")

        name = 'PM10'
        value = Math.round(aqi.list[0].components.pm10)
        convertedVal = value
        if (convertedVal<55){
            aQItemp = (50/54)*(convertedVal)
        }else if (convertedVal<155){
            aQItemp = (49/99)*(convertedVal-55)+51
        }else if (convertedVal<255){
            aQItemp = (49/99)*(convertedVal-155)+101
        }else if (convertedVal<355){
            aQItemp = (49/99)*(convertedVal-255)+151
        }else if (convertedVal<425){
            aQItemp = (99/69)*(convertedVal-355)+201
        }else if (convertedVal<505){
            aQItemp = (99/79)*(convertedVal-425)+301
        }else {
            aQItemp = (99/99)*(convertedVal-505)+401
        }
        if (aQItemp>aQI){
            aQI=aQItemp
            aQIfactor = name
        }
        descriptionText = "${device.displayName} ${name} is ${convertedVal} AQI ${aQItemp}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: name,value: value,descriptionText: descriptionText,unit: "mg/m^3")

        name = 'airQualityIndex'
//		def value = aqi.list[0].main.aqi

//		def descriptionText = "${device.displayName} ${name} is ${value}"
//		if (txtEnable) log.info "${descriptionText}"
//		sendEvent(name: name,value: value,descriptionText: descriptionText,unit: unit)
//		name = 'AQI'
        descriptionText = "${device.displayName} ${name} using ${aQIfactor}, AQI is ${aQI}"
        if (txtEnable) log.info "${descriptionText}"
        descriptionText = "${device.displayName} ${name} is ${aQI} Condition is ${aQIfactor}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: name,value: aQI,descriptionText: 'US AQI',unit: aQIfactor)

        name = 'PrimaryFactor'
        descriptionText = "${device.displayName} ${name} is ${aQIfactor}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: name,value: aQIfactor,descriptionText: 'AQI Factor',unit: unit)




        name = 'AQIColorCode'
        def aQIcolor = "#7E0023"
//        def aQIText = ""

        if (aQI>1&&aQI<51) {
            aQIcolor = '#00E400'
        }else if (aQI<101&&aQI>50) {
            aQIcolor = '#FFFF00'
        }else if (aQI<151&&aQI>100) {
            aQIcolor = '#FF7E00'
        }else if (aQI<201&&aQI>150) {
            aQIcolor = '#FF0000'
        }else if (aQI<301&&aQI>200) {
            aQIcolor = '#8F3F97'
        }
        descriptionText = "${device.displayName} ${name} is ${aQIcolor}"
        if (dbgEnable) log.debug "${descriptionText}"
        sendEvent(name: 'AQIColorCode',value: aQIcolor ,descriptionText: 'US AQI Color Code',unit: "RGB")


        // th{padding: 10px;}
        String aTile
        aTile = '<style>h3 {text-align: center;font-size:2.5REM;color:Black;}p {text-align: center;font-size:1REM;color:Black;}</style>'
        // aTile +='<table style="width: calc(100% - 8px);height: calc(100% - 8px);margin: auto;background-color: '
        aTile += '<div style="overflow-x:auto;"><table style="width: calc(100% - 8px);height: max-content;background-color: '
        aTile += aQIcolor
        aTile += '"><tr><td><h3>'
        aTile += aQI
        aTile += '</td></tr><tr><td><p>'
        aTile += aQIfactor
        aTile += '</p></td></tr></table></div>'

        descriptionText = "${device.displayName} Tile is ${aTile}"
        if (dbgEnable) log.debug "${descriptionText}"

        sendEvent(name: 'AlertTile', value: aTile )
    }
}

void refresh() {
}

void installed() {
    schedule("0 0/30 * 1/1 * ? *", pollAQI) //every half hour
}

void uninstalled() {
    unschedule()
}

void pollData() {
    pollAQI()
}