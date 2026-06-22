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
 *      2026-05-01    jshimota      0.2.16      Gemini Recommendations
 *      2026-05-14    jshimota      0.2.17      Fix to package json to required true
 *
 */

/**
 * Meteorological Seasons - Optimized
 */

import java.text.SimpleDateFormat

static String version() { return '0.2.17' }

metadata {
    definition(
        name: "Meteorological Seasons",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/meteorSeasons.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        
        command "updateSeason"
        command "autumn"
        command "fall"
        command "winter"
        command "spring"
        command "summer"

        attribute "seasonName", "string"
        attribute "seasonNum", "number"
        attribute "seasonBegin", "string"
        attribute "seasonEnd", "string"
        attribute "seasonTile", "string"
        attribute "seasonImg", "string"
        attribute "todaysFormattedDate", "string"
        attribute "hemisphereName", "string"
        attribute "autumnFallName", "string"
    }
}

preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input name: "fontSize", type: "number", title: "HTML Tile Font Size (%)", defaultValue: 100
    input name: "vertPos", type: "number", title: "HTML Tile Word Position (%)", defaultValue: 55
    input name: "fontColor", type: "string", title: "HTML Tile Text Color (Hex)", defaultValue: "#FFFFFF"
    input name: "autoUpdate", type: "bool", title: "Enable automatic update at 6am", defaultValue: true
    input name: "iconPathOvr", type: "string", title: "Alternate path to season icons"
    input name: "isNorthern", type: "bool", title: "Hemisphere (On=Northern / Off=Southern)", defaultValue: true
    input name: "useAutumn", type: "bool", title: "Naming (On=Autumn / Off=Fall)", defaultValue: false
}

def installed() {
    refresh()
}

def updated() {
    log.info "Settings updated."
    if (logEnable) runIn(1800, logsOff)
    unschedule()
    if (autoUpdate) schedule("0 0 6 * * ?", refresh)
    refresh()
}

def logsOff() {
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def refresh() {
    updateSeason()
}

// Shortcut commands for manual override
def fall()   { setSeasonManual("Fall") }
def autumn() { setSeasonManual("Autumn") }
def winter() { setSeasonManual("Winter") }
def spring() { setSeasonManual("Spring") }
def summer() { setSeasonManual("Summer") }

def setSeasonManual(String sName) {
    if (txtEnable) log.info "Manual override: Setting season to ${sName}"
    processSeasonUpdate(sName)
}

def updateSeason() {
    Date now = new Date()
    String month = new SimpleDateFormat("MMMM").format(now)
    String dateStr = new SimpleDateFormat("dd-MM-yyyy").format(now)
    
    sendEvent(name: "todaysFormattedDate", value: dateStr)
    
    String hName = isNorthern ? "Northern" : "Southern"
    String afName = useAutumn ? "Autumn" : "Fall"
    
    sendEvent(name: "hemisphereName", value: hName)
    sendEvent(name: "autumnFallName", value: afName)

    String targetSeason = ""

    // Logic Map for Meteorological Seasons
    if (isNorthern) {
        if (["September", "October", "November"].contains(month)) targetSeason = afName
        else if (["December", "January", "February"].contains(month)) targetSeason = "Winter"
        else if (["March", "April", "May"].contains(month)) targetSeason = "Spring"
        else targetSeason = "Summer"
    } else {
        if (["March", "April", "May"].contains(month)) targetSeason = afName
        else if (["June", "July", "August"].contains(month)) targetSeason = "Winter"
        else if (["September", "October", "November"].contains(month)) targetSeason = "Spring"
        else targetSeason = "Summer"
    }

    processSeasonUpdate(targetSeason)
}

def processSeasonUpdate(String sName) {
    // Data definitions to avoid redundant methods
    def seasonData = [
        "Fall":   [num: 1, nStart: "Sept 1", nEnd: "Nov 30", sStart: "March 1", sEnd: "May 31", img: "fall"],
        "Autumn": [num: 1, nStart: "Sept 1", nEnd: "Nov 30", sStart: "March 1", sEnd: "May 31", img: "autumn"],
        "Winter": [num: 2, nStart: "Dec 1",  nEnd: "Feb 28", sStart: "June 1",  sEnd: "Aug 31", img: "winter"],
        "Spring": [num: 3, nStart: "March 1", nEnd: "May 31",  sStart: "Sept 1",  sEnd: "Nov 30", img: "spring"],
        "Summer": [num: 4, nStart: "June 1",  nEnd: "Aug 31",  sStart: "Dec 1",   sEnd: "Feb 28", img: "summer"]
    ]

    def data = seasonData[sName]
    String start = isNorthern ? data.nStart : data.sStart
    String end = isNorthern ? data.nEnd : data.sEnd
    String iconBase = iconPathOvr ?: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    
    String imgUrl = "${iconBase}${data.img}.svg"
    
    // Build Tile HTML
    String tileHtml = "<div style='background-image: url(${imgUrl});background-position: center;background-repeat: no-repeat;background-size: contain;width: 100%;height: 100%;'>" +
                      "<div style='font-family: Georgia, serif;text-align: center;position: relative;top:${vertPos}%;font-size:${fontSize}%;color:${fontColor};text-transform: uppercase;font-style: oblique;'>" +
                      "<h1 style='font-size:100%;'>${sName}</h1></div></div>"

    sendEvent(name: "seasonName", value: sName)
    sendEvent(name: "seasonNum", value: data.num)
    sendEvent(name: "seasonBegin", value: start)
    sendEvent(name: "seasonEnd", value: end)
    sendEvent(name: "seasonImg", value: "<img src='${imgUrl}' style='height:100px;'/>")
    sendEvent(name: "seasonTile", value: tileHtml)
    
    if (txtEnable) log.info "Season updated to ${sName}"
}