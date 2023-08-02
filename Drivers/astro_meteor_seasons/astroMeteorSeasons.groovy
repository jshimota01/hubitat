/*
 * Astronomical-Meteorological Seasons
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2023-06-24    jshimota      0.1.1       Starting version
 *      2023-07-03    tomw          0.1.5       Restart and recreate
 *      2023-07-04    kkossev       0.1.6       variable Initialize to startup values added
 *
 *
 */

import java.text.SimpleDateFormat
// import hubitat.device.Protocol

static String version() { return '0.1.6' }
static timeStamp() { "2023/07/04 4:50 PM" }

metadata {
    definition(
            name: "Astronomical-Meteorological Seasons",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astro_ometeor_seasons/astroMeteorSeasons.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        command "initialize", [[name: "Select 'Yes' to re-initialize the device to startup values", type: "ENUM", description: "Set device to startup values", required: true, constraints: ["--- Select ---", "Yes", "No"], defaultValue : "--- Select ---"]]
        command "setSeason", [[name: "Season Name*", type: "ENUM", constraints: ["Spring", "Summer", "Autumn/Fall", "Winter"], description: "Select a meteorological season from one of the provided choices."],[name: "Season Type*", type: "ENUM", constraints: ["Astrological", "Meteorological"], description: "Select a season type from one of the provided choices."]]

        attribute "autumnFallName", "string"
        attribute "astroMeteorType", "string"
        attribute "firstEquinox", "string"
        attribute "firstSolstice", "string"
        attribute "hemisphereName", "string"
        attribute "secondEquinox", "string"
        attribute "secondSolstice", "string"
        attribute "seasonName", "string"
        attribute "seasonNum", "number"
        attribute "seasonBegin", "string"
        attribute "seasonEnd", "string"
        attribute "seasonTile", "string"
        attribute "seasonImg", "string"
        attribute "tileFontSize", "number"
        attribute "tileFontColor", "string"
        attribute "tileVertWordPos", "number"
        attribute "todaysFormattedDate", "string"
        attribute "todaysFormattedMonth", "string"
        attribute "todaysHtmlFriendlyDate", "string"
    }
}

preferences {
    input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable logging", defaultValue: true
    input(title: "Note", description: "<b>Debug</b> logging will auto disable after 30 minutes.", type: "paragraph", element: "paragraph")
    input(name: "existingTileFontSize", type: "num", title: "HTML Tile Font Size (%)*", defaultValue: 100)
    input(name: "existingTileVertWordPos", type: "num", title: "HTML Tile Word Position (%)*", defaultValue: 55)
    input(name: "existingTileFontColor", type: "string", title: "HTML Tile Text Color (Hex format with leading #)", defaultValue: "#FFFFFFFF")
    input("autoUpdate", "bool", title: "Enable automatic update at 6am\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
    input("htmlVtile", "bool", title: "Use HTML attribute instead of seasonTile\n(Enabled is Yes)")
    input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names fall.svg, winter.svg, spring.svg, summer.svg and unknown.svg)")
    input(name: "hemisphere", type: "bool", title: "Hemisphere", description: "Off=Northern/On=Southern", defaultValue: false, required: true, displayDuringSetup: true)
    input(name: "astroMeteor", type: "bool", title: "Astronomical or Meteorological Season", description: "Off=Astronomical/On=Meteorological", defaultValue: false, required: true, displayDuringSetup: true)
    input(name: "existingAutumnFall", type: "bool", title: "Autumn/Fall", description: "Off=Autumn/On=Fall", defaultValue: false, required: true, displayDuringSetup: true)
}

/*  tomw section   */
def getAstroSeasonData() {
    def url = "https://aa.usno.navy.mil/api/seasons?year=2023&tz=-6&dst=true"
    def res = pullAstroSeason(url)
    parseResp(res)
}

def pullAstroSeason(url) {
    def res
    httpGet(url) {
        resp->
            if (dbgEnable) { log.debug "Full json response returned from USnavy.org : ${resp.data}" }
            res = resp.data
    }
    return res
}

def parseResp(Map res) {
    def phenom
    ["Equinox", "Solstice"].each {
        phenom = res?.data?.findAll { phe -> phe?.phenom == it }
        if(phenom) {
            phenom.eachWithIndex {
                thisone, idx ->
                    sendEvent(name: it+idx, value: "${thisone.month}-${thisone.day}-${thisone.year}")
                    if (dbgEnable) { log.debug "${it+idx}: ${phenom}" }
            }
        }
    }
}

/* initialize button adapted from KKossev Zemismart Zigbee multigang switch 7/4/23 */
void initializeVars(boolean fullInit = true) {
    if (dbgEnable) log.debug "${device.displayName} InitializeVars()... fullInit = ${fullInit}"
    if (fullInit) {
        state.clear()
        state.driverVersion = driverVersionAndTimeStamp()
    String tileFontColor = "#FFFFFFFF"
    tileVertWordPos = 55
    tileFontSize = 100
	dbgEnable == false
	txtEnable == true
	hemisphere == false
	astroMeteor == false
	existingAutumnFall == false
	autoUpdate == true
	htmlVtile == false
	iconPathOvr = ""
    }
}

def initialize( str ) {
    if (str != "Yes") {
        log.warn "initialize aborted, YES not selected!"
    }
    else {
        initialize()
    }
}

def initialize() {
    log.warn "Initialize button pressed ... resetting all values to default."
    initializeVars(fullInit = true)
}

static driverVersionAndTimeStamp() { version() + ' ' + timeStamp() }

def tileFontColor() {
    String tileFontColor = "#FFFFFFFF"
    if(existingTileFontColor > " ") tileFontColor = existingTileFontColor
    sendEvent(name: "Tile Font Color", value: "${tileFontColor}")
}

def tileVertWordPos() {
    tileVertWordPos = 55
    if(existingTileVertWordPos > " ") tileVertWordPos = existingTileVertWordPos
    sendEvent(name: "Tile Vertical Word Position", value: tileVertWordPos)
}

def tileFontSize() {
    tileFontSize = 100
    if(existingTileFontSize > " ") tileFontSize = existingTileFontSize
    sendEvent(name: "Tile Font Size", value: tileFontSize)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}

def installed() {
    logging("installed()", 100)
    unschedule()
    refresh()
    schedule("0 0 6 * * ?", refresh) //  daily at 6am
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${dbgEnable}"
    log.warn "description logging is: ${txtEnable}"
    if (dbgEnable) {
        if (!autoUpdate)log.warn("Update: Automatic Update DISABLED")
    }
    if (dbgEnable) {
        if (autoUpdate)log.info("Update: Automatic Update enabled")
    }
    if (dbgEnable) runIn(1800, logsOff)
    if (dbgEnable) log.debug("autoupdate: Next scheduled refresh set")
    unschedule()
    refresh()
    schedUpdate()

}

def refresh() {
    tileFontColor()
    tileFontSize()
    tileVertWordPos()
    getAstroSeasonData()
    currentMetSeason()
    runCmd()
}

def schedUpdate() {
    unschedule()
    if (txtEnable) log.info("schedUpdate: Refresh schedule cleared ...")
    if (autoUpdate) {
        if (txtEnable) log.info("Update: Setting next scheduled refresh...")
        if (autoUpdate) schedule("0 0 6 * * ?", refresh) // daily at 6am
        if (autoUpdate) log.info("Update: Next scheduled refresh set")
    }
}


def runCmd() {
    now = new Date()
    simpleDateFormatForDate = new SimpleDateFormat('dd-MM-yyyy')
    simpleDateFormatForMonth = new SimpleDateFormat('MMMM')

    proposedFormattedDate = simpleDateFormatForDate.format(now)
    proposedFormattedMonth = simpleDateFormatForMonth.format(now)
    proposedHtmlFriendlyDate = "<span class=\"dateFormat\">${proposedFormattedDate}</span>"

    sendEvent(name: "todaysFormattedDate", value: proposedFormattedDate)
    sendEvent(name: "todaysFormattedMonth", value: proposedFormattedMonth)
    sendEvent(name: "todaysHtmlFriendlyDate", value: proposedHtmlFriendlyDate)
    if (hemisphere) sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText) else sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText)
    if (astroMeteor) sendEvent(name: "astroMeteorType", value: "Meteorological", descriptionText: descriptionText) else sendEvent(name: "astroMeteorType", value: "Astronomical", descriptionText: descriptionText)
    if (existingAutumnFall) sendEvent(name: "autumnFallName", value: "Autumn", descriptionText: descriptionText) else sendEvent(name: "autumnFallName", value: "Fall", descriptionText: descriptionText)
    currentMetSeason()
}

// table
//  VariableName        fallStart   fallEnd     winterStart     winterEnd       springStart springEnd   summerStart summerEnd
//  northPeriod         September 1 November 30 December 1      February 29     March 1      May 31     June 1      August 31
//  southPeriod         March 1     May 31      June 1          August 31       September 1  November 1 December 1  February 29

def hemisphereName() {
    if (hemisphere) {
        sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
        def descriptionText = "Current hemisphere is now Southern"
        if (txtEnable) log.info "${descriptionText}"
    } else {
        sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText)
        def descriptionText= "Current hemisphere is now Northern"
        if (txtEnable) log.info "${descriptionText}"
    }
}

def astroMeteorType() {
    if (astroMeteor) {
        sendEvent(name: "astroMeteorType", value: "Meteorological", descriptionText: descriptionText)
        def descriptionText = "Current Astrological or Meteorological type is Meteorological"
        if (txtEnable) log.info "${descriptionText}"
    } else {
        sendEvent(name: "astroMeteorType", value: "Astronomical", descriptionText: descriptionText)
        def descriptionText= "Current Astrological or Meteorological type is Astronomical"
        if (txtEnable) log.info "${descriptionText}"
    }
}

def autumnFallName() {
    if (existingAutumnFall) {
        sendEvent(name: "autumnFallName", value: "Autumn", descriptionText: descriptionText)
        def descriptionText = "Current Autumn/Fall choice is now Autumn"
        if (txtEnable) log.info "${descriptionText}"
    } else {
        sendEvent(name: "autumnFallName", value: "Fall", descriptionText: descriptionText)
        def descriptionText= "Current Autumn/Fall choice is now Fall"
        if (txtEnable) log.info "${descriptionText}"
    }
}

def currentMetSeason() {
    if (device.currentValue("todaysFormattedMonth") == (null)) {
        runCmd()
    } else if (device.currentValue("todaysFormattedMonth") == ("Not Initialized")) {
        seasonPreviouslySet = false
        existingSeasonName = "not set"
        hemispherePreviouslySet = false
        existingHemisphereName = "not set"
        autumnFallPreviouslySet = false
        existingAutumnFall = "not set"
    } else {
        seasonPreviouslySet = true
        existingSeasonName = "${seasonName}"
        hemispherePreviouslySet = true
        existingHemisphereName = "${hemisphereName}"
        autumnFallPreviouslySet = true
        existingAutumnFallName = "${autumnFallName}"
    }
    if (hemisphere) {
        if (device.currentValue("todaysFormattedMonth") == ("September") || device.currentValue("todaysFormattedMonth") == ("October") || device.currentValue("todaysFormattedMonth") == ("November")) {
            if(!autumnFall) fall() else autumn()
        } else if (device.currentValue("todaysFormattedMonth") == ("December") || device.currentValue("todaysFormattedMonth") == ("January") || device.currentValue("todaysFormattedMonth") == ("February")) {
            winter()
        } else if (device.currentValue("todaysFormattedMonth") == ("March") || device.currentValue("todaysFormattedMonth") == ("April") || device.currentValue("todaysFormattedMonth") == ("May")) {
            spring()
        } else if (device.currentValue("todaysFormattedMonth") == ("June") || device.currentValue("todaysFormattedMonth") == ("July") || device.currentValue("todaysFormattedMonth") == ("August")) {
            summer()
        }
    } else if (!hemisphere) {
        if (device.currentValue("todaysFormattedMonth") == ("September") || device.currentValue("todaysFormattedMonth") == ("October") || device.currentValue("todaysFormattedMonth") == ("November")) {
            spring()
        } else if (device.currentValue("todaysFormattedMonth") == ("December") || device.currentValue("todaysFormattedMonth") == ("January") || device.currentValue("todaysFormattedMonth") == ("February")) {
            summer()
        } else if (device.currentValue("todaysFormattedMonth") == ("March") || device.currentValue("todaysFormattedMonth") == ("April") || device.currentValue("todaysFormattedMonth") == ("May")) {
            if(!autumnFall) fall() else autumn()
        } else if (device.currentValue("todaysFormattedMonth") == ("June") || device.currentValue("todaysFormattedMonth") == ("July") || device.currentValue("todaysFormattedMonth") == ("August")) {
            winter()
        }
    } else {
        String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astro_meteor_seasons/season_icons/"
        if (iconPathOvr > " ") iconPath = iconPathOvr
        sendEvent(name: "seasonName", value: "Not Initialized", descriptionText: descriptionText)
        sendEvent(name: "seasonNum", value: 0, descriptionText: descriptionText)
        sendEvent(name: "seasonBegin", value: "N/A", descriptionText: descriptionText)
        sendEvent(name: "seasonEnd", value: "N/A", descriptionText: descriptionText)
        sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;'><p class='small' style='text-align:center'>(Not Initialized)</p></img></div>", descriptionText: descriptionText)
        sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;' />", descriptionText: descriptionText)
        sendEvent(name: "hemisphereName", value: "not set", descriptionText: descriptionText)
        sendEvent(name: "autumnFallName", value: "not set", descriptionText: descriptionText)
    }
    if (seasonPreviouslySet) {
        descriptionText = "Current season refreshed or changed"
        if (txtEnable) log.info "${descriptionText}"
    } else {
        descriptionText = "Current season initialized to ${seasonName}"
        if (txtEnable) log.info "${descriptionText}"
    }
    if (hemispherePreviouslySet) {
        descriptionText = "Current hemisphere refreshed or changed"
        if (txtEnable) log.info "${descriptionText}"
    } else {
        if (hemisphere) descriptionText = "Current hemisphere initialized to Northern" else descriptionText = "Current hemisphere initialized to Southern"
        if (txtEnable) log.info "${descriptionText}"
    }
    if (autumnFallPreviouslySet) {
        descriptionText = "Current Autumn/Fall choice refreshed or changed"
        if (txtEnable) log.info "${descriptionText}"
    } else {
        if (existingAutumnFall) descriptionText = "Current Autumn/Fall choice initialized to Autumn" else descriptionText = "Current Autumn/Fall choice initialized to Fall"
        if (txtEnable) log.info "${descriptionText}"
    }
}

def fall() {
    hemisphereName()
    autumnFallName()
    def descriptionText = "Current season is now Fall" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astronomical_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Fall", descriptionText: descriptionText)
    String seasonName = "fall"
    sendEvent(name: "seasonNum", value: 1, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "September 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "March 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "November 30", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "May 31", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div style='background-image: url(${iconPath}${seasonName}.svg);background-position: center;background-repeat: no-repeat;background-size: contain;width: 100%;height: 100%;'><div style='font-family: Georgia, serif;text-align: center;position: relative;top:${existingTileVertWordPos}%;font-size:${existingTileFontSize}%;color:${existingTileFontColor};text-transform: uppercase;font-style: oblique;'><h1 style='font-size:${existingTileFontSize}%;'>${seasonName}</h1></div></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}${seasonName}.svg' style='height: 100px;' />", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText) else sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
    if (existingAutumnFall) sendEvent(name: "autumnFallName", value: "Autumn", descriptionText: descriptionText) else sendEvent(name: "autumnFallName", value: "Fall", descriptionText: descriptionText)
}

def autumn() {
    hemisphereName()
    autumnFallName()
    def descriptionText = "Current season is now Autumn" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astronomical_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Autumn", descriptionText: descriptionText)
    String seasonName = "autumn"
    sendEvent(name: "seasonNum", value: 1, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "September 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "March 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "November 30", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "May 31", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div style='background-image: url(${iconPath}${seasonName}.svg);background-position: center;background-repeat: no-repeat;background-size: contain;width: 100%;height: 100%;'><div style='font-family: Georgia, serif;text-align: center;position: relative;top:${existingTileVertWordPos}%;font-size:${existingTileFontSize}%;color:${existingTileFontColor};text-transform: uppercase;font-style: oblique;'><h1 style='font-size:${existingTileFontSize}%;'>${seasonName}</h1></div></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}${seasonName}.svg' style='height: 100px;' />", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText) else sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
    if (existingAutumnFall) sendEvent(name: "autumnFallName", value: "Autumn", descriptionText: descriptionText) else sendEvent(name: "autumnFallName", value: "Fall", descriptionText: descriptionText)
}

def winter() {
    hemisphereName()
    autumnFallName()
    def descriptionText = "Current season is now Winter" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astronomical_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Winter", descriptionText: descriptionText)
    String seasonName = "winter"
    sendEvent(name: "seasonNum", value: 2, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "December 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "June 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "February 28", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "August 31", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div style='background-image: url(${iconPath}${seasonName}.svg);background-position: center;background-repeat: no-repeat;background-size: contain;width: 100%;height: 100%;'><div style='font-family: Georgia, serif;text-align: center;position: relative;top:${existingTileVertWordPos}%;font-size:${existingTileFontSize}%;color:${existingTileFontColor};text-transform: uppercase;font-style: oblique;'><h1 style='font-size:${existingTileFontSize}%;'>${seasonName}</h1></div></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}${seasonName}.svg' style='height: 100px;' />", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText) else sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
    if (existingAutumnFall) sendEvent(name: "autumnFallName", value: "Autumn", descriptionText: descriptionText) else sendEvent(name: "autumnFallName", value: "Fall", descriptionText: descriptionText)
}

def spring() {
    hemisphereName()
    autumnFallName()
    def descriptionText = "Current season is now Spring" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astronomical_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Spring", descriptionText: descriptionText)
    String seasonName = "spring"
    sendEvent(name: "seasonNum", value: 3, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "March 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "September 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "May 31", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "November 30", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div style='background-image: url(${iconPath}${seasonName}.svg);background-position: center;background-repeat: no-repeat;background-size: contain;width: 100%;height: 100%;'><div style='font-family: Georgia, serif;text-align: center;position: relative;top:${existingTileVertWordPos}%;font-size:${existingTileFontSize}%;color:${existingTileFontColor};text-transform: uppercase;font-style: oblique;'><h1 style='font-size:${existingTileFontSize}%;'>${seasonName}</h1></div></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}${seasonName}.svg' style='height: 100px;' />", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText) else sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
    if (existingAutumnFall) sendEvent(name: "autumnFallName", value: "Autumn", descriptionText: descriptionText) else sendEvent(name: "autumnFallName", value: "Fall", descriptionText: descriptionText)
}

def summer() {
    hemisphereName()
    autumnFallName()
    def descriptionText = "Current season is now Summer" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astronomical_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Summer", descriptionText: descriptionText)
    String seasonName = "summer"
    sendEvent(name: "seasonNum", value: 4, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "June 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "December 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "August 31", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "February 28", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div style='background-image: url(${iconPath}${seasonName}.svg);background-position: center;background-repeat: no-repeat;background-size: contain;width: 100%;height: 100%;'><div style='font-family: Georgia, serif;text-align: center;position: relative;top:${existingTileVertWordPos}%;font-size:${existingTileFontSize}%;color:${existingTileFontColor};text-transform: uppercase;font-style: oblique;'><h1 style='font-size:${existingTileFontSize}%;'>${seasonName}</h1></div></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}${seasonName}.svg' style='height: 100px;' />", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText) else sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
    if (existingAutumnFall) sendEvent(name: "autumnFallName", value: "Autumn", descriptionText: descriptionText) else sendEvent(name: "autumnFallName", value: "Fall", descriptionText: descriptionText)
}