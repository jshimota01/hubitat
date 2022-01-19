/*
 * Meteorological Seasons
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
 *      2021-09-30    jshimota      0.1.0       Starting version
 *      2021-09-30    thebearmay    0.1.1       Used MoonPhase app components                     https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhase.groovy
 *      2021-09-30    sburke781     0.1.2       Used DateFormat app components                    https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
 *      2021-09-30    luarmr        0.1.3       Used DateFormat app components                    https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
 *      2021-09-30    bravenel      0.1.4       Used Virtual Omni Sensor driver components        https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/virtualOmniSensor.groovy
 *      2021-10-01    jshimota      0.1.5-11    Me working
 *      2021-10-02    jshimota      0.1.12      I think I broke it
 *      2021-10-02    jshimota      0.1.13      Fixed and hard-set the formats
 *      2021-10-02    jshimota      0.1.14      Removed Calc Season - it was broken and not necessary at this time. maybe later
 *      2021-10-03    jshimota      0.1.15      Cleaning logs - schedules and basic clean of logic
 *      2021-10-03    jshimota      0.1.16      Fixed Daily values not updating automatically
 *      2021-10-03    jshimota      0.1.17      Updated icons and importURLS to matching github correctly / better
 *      2021-10-03    jshimota      0.1.18      Moved once again as I got folder structure corrected on github
 *		2021-10-03	  jshimota		0.2.0		Integrated beta version with support of both hemispheres
 *		2021-10-03	  jshimota	 	0.2.01		Name Change of Driver
 *      2021-10-06    jshimota      0.2.02      Further Name change (dropped -NH as it was no longer appropriate)
 *      2021-10-26    jshimota      0.2.10      Added Font color and font size options, rebuilt Tile so it fits Hubitat Android Dashboard correctly. HE dashboard is still messed.
 *      2021-10-27    jshimota      0.2.11      Added variable to adjust word in tile overlay vertical position
 *      2021-12-01    jshimota      0.2.12      Botched the scheduled update - was only running the current season, not the date which is relied upon. should be fixed.
 *      2021-12-23    jshimota      0.2.13      Fixed Debug posting to log when debug log disabled.
 *      2022-01-04    jshimota      0.2.14      Added Autumn switch
 *      2022-01-05    jshimota      0.2.15      Changed date to feb 28th - no leap year check.
 *
 */

import java.text.SimpleDateFormat

// import java.io.BufferedReader;
// import java.io.File;
// import java.io.IOException;
// import java.io.InputStreamReader;


static String version() { return '0.2.15' }

metadata {
    definition(
            name: "Meteorological Seasons",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/meteorSeasons.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        // Line below removed as I took out calcDate command
        // command "calcSeason", [[name: "rawDate", type: "STRING", description: "Enter date as (dd-MM-yyyy) to calculate the season for."]]
        command "currentSeason", ["$Cdate"]
        command "autumn"
        command "fall"
        command "winter"
        command "spring"
        command "summer"

        // Lines Below - future task to allow override of season beginning and end for each season
        // command "fall",["Date Range"]
        // command "winter",["Date Range"]
        // command "spring",["Date Range"]
        // command "summer",["Date Range"]

        attribute "seasonName", "string"
        attribute "seasonNum", "number"
        attribute "seasonBegin", "string"
        attribute "seasonEnd", "string"
        attribute "seasonTile", "string"
        attribute "seasonImg", "string"
        attribute "todaysFormattedDate", "string"
        attribute "todaysFormattedMonth", "string"
        attribute "todaysHtmlFriendlyDate", "string"
        attribute "hemisphereName", "string"
        attribute "tileFontSize", "number"
        attribute "tileFontColor", "string"
        attribute "tileVertWordPos", "number"
        attribute "autumnFallName", "string"

        // Line below removed as I took out calcDate command
        // attribute "rawDateFormattedMonth", "string"

    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input(name: "existingTileFontSize", type: "num", title: "HTML Tile Font Size (%)*", defaultValue: 100)
    input(name: "existingTileVertWordPos", type: "num", title: "HTML Tile Word Position (%)*", defaultValue: 55)
    input(name: "existingTileFontColor", type: "string", title: "HTML Tile Text Color (Hex format with leading #)", defaultValue: "#FFFFFFFF")
    input("autoUpdate", "bool", title: "Enable automatic update at 6am\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
    input("htmlVtile", "bool", title: "Use HTML attribute instead of seasonTile\n(Enabled is Yes)")
    input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names fall.svg, winter.svg, spring.svg, summer and unknown.svg)")
    input(name: "hemisphere", type: "bool", title: "Hemisphere", description: "On=Northern/Off=Southern", defaultValue: true, required: true, displayDuringSetup: true)
    input(name: "existingAutumnFall", type: "bool", title: "Autumn/Fall", description: "On=Autumn/Off=Fall", defaultValue: false, required: true, displayDuringSetup: true)

    // Line below removed as I took out calcDate command
    // input(name: "rawDateFormat", type: "string", title:"RawDate Format", description: "Enter the date format to apply for entering and displaying RawDate purposes", defaultValue: "dd-MM-yyyy", required: true, displayDuringSetup: true)
}

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
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def installed() {
    logging("installed()", 100)
    unschedule()
    refresh()
//    schedule("0 0 6 * * ?", currentSeason) //  daily at 6am
    schedule("0 0 6 * * ?", refresh) //  daily at 6am
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable}"
    log.warn "description logging is: ${txtEnable}"
    if (logEnable) {
        if (!autoUpdate)log.warn("Update: Automatic Update DISABLED")
    }
    if (logEnable) {
        if (autoUpdate)log.info("Update: Automatic Update enabled")
    }
    if (logEnable) runIn(1800, logsOff)
    if (logEnable) log.debug("autoupdate: Next scheduled refresh set")
    unschedule()
    refresh()
    schedUpdate()

}

def refresh() {
    // runCmd()
    tileFontColor()
    tileFontSize()
    tileVertWordPos()
    currentSeason()
    // hemisphereName()
    // autumnFallName()
    // currentSeason()
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
    if (hemisphere) sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText) else sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
    if (existingAutumnFall) sendEvent(name: "autumnFallName", value: "Autumn", descriptionText: descriptionText) else sendEvent(name: "autumnFallName", value: "Fall", descriptionText: descriptionText)
    currentSeason()
}

// table
//  VariableName        fallStart   fallEnd     winterStart     winterEnd       springStart springEnd   summerStart summerEnd
//  northPeriod         September 1 November 30 December 1      February 29     March 1      May 31     June 1      August 31
//  southPeriod         March 1     May 31      June 1          August 31       September 1  November 1 December 1  February 29

def hemisphereName() {
    if (hemisphere) {
        sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText)
        def descriptionText = "Current hemisphere is now Northern"
        if (txtEnable) log.info "${descriptionText}"
    } else {
        sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
        def descriptionText= "Current hemisphere is now Southern"
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

def currentSeason() {
    // if (device.currentValue("todaysFormattedMonth") == (null)) {
    //     runCmd()
    // } else if (device.currentValue("todaysFormattedMonth") == ("Not Initialized")) {
    //     seasonPreviouslySet = false
    //     existingSeasonName = "${seasonName}"
    // } else {
    //     seasonPreviouslySet = true
    //     existingSeasonName = "${seasonName}"
    // }
    // Line below removed cuz it may be causing problem
    // sendEvent(name: "rawDate", value: rawDate, descriptionText: descriptionText)
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
        String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
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
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
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
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
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
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
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
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
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
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
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