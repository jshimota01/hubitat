/*
 * Meteorological & Astronomical Seasons
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2021-09-30    jshimota      0.1.0       Starting version
 *      ...           ...           ...         ...
 *      2026-05-04    Gemini        0.3.0       Added Astronomical Seasons & Dual-Tracking
 *      2026-05-12    jshimota      0.3.1       Cleanup of preferences info
 *      2026-05-12    jshimota      0.3.2       Typo fixed.
 *      2026-05-12    jshimota      0.3.3       Trying to fix HPM stuck
 *
 */


import java.text.SimpleDateFormat

static String version() { return '0.3.3' }

metadata {
    definition(
        name: "Astronomical & Meteorological Seasons",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astro_meteor_seasons/astroMeteorSeasons.groovy"
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
        attribute "meteorologicalSeason", "string"
        attribute "astronomicalSeason", "string"
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
    input name: "primaryDisplay", type: "enum", title: "<b>Primary Dashboard Display</b>", options: ["Meteorological", "Astronomical"], defaultValue: "Meteorological", description:\
	"<i>Select Season Type: Astronomical which is determined by the Earth's position in its orbit relative to the Sun <b>- or -</b> Meteorological, which groups the year into four equal, three-month static blocks</i>"  
    input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: false, description:\
	"<i>Enable for problem solving - recommended OFF</i>"  
    input name: "txtEnable", type: "bool", title: "<b>Enable Info logging</b>", defaultValue: true, description:\
	"<i>Enable for general log entries - recommended ON</i>"  
    input name: "fontSize", type: "number", title: "<b>HTML Tile Font Size (%)</b>", defaultValue: 100, description:\
	"<i>Adjust the text height of tile output by percentage</i>"  
    input name: "vertPos", type: "number", title: "<b>HTML Tile Word Position (%)</b>", defaultValue: 55, description:\
	"<i>Adjust the text position relative to top of tile by percentage</i>"  
    input name: "fontColor", type: "string", title: "<b>HTML Tile Text Color (Hex)</b>", defaultValue: "#ffffffff", description:\
	"<i>Adjust the text color. Can be color name or hex value -eg; #ffffff or white. 8 char hex is supported for opacity!</i>"  
    input name: "autoUpdate", type: "bool", title: "<b>Enable automatic update at 6am</b>", defaultValue: true, description:\
	"<i>On (Recommended) to enable automatic update.  Disable for testing or to have tile static for other purposes</i>"  
    input name: "iconPathOvr", type: "string", title: "<b>Alternate path to season icons</b>", description:\
	"<i>Use this if you have a different set of images. Should end with /.  eg; http://192.168.1.12/icons/seasons/</i>"  
    input name: "isNorthern", type: "bool", title: "<b>Hemisphere (On=Northern / Off=Southern)</b>", defaultValue: true, description:\
	"<i>For our friends in the south - Seasons are reversed, and this accounts for their needs.</i>"  
    input name: "useAutumn", type: "bool", title: "<b>Season Naming (On=Autumn / Off=Fall)</b>", defaultValue: false, description:\
	"<i>Choose which word to use for the 3rd season, default is Fall</i>"  
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
    processSeasonUpdate(sName, sName, sName) // Overrides both to the manual selection
}

def updateSeason() {
    Date now = new Date()
    String dateStr = new SimpleDateFormat("dd-MM-yyyy").format(now)
    int mNum = new SimpleDateFormat("MM").format(now).toInteger()
    int dNum = new SimpleDateFormat("dd").format(now).toInteger()
    
    sendEvent(name: "todaysFormattedDate", value: dateStr)
    
    String hName = isNorthern ? "Northern" : "Southern"
    String afName = useAutumn ? "Autumn" : "Fall"
    
    sendEvent(name: "hemisphereName", value: hName)
    sendEvent(name: "autumnFallName", value: afName)

    String meteoSeason = ""
    String astroSeason = ""

    // 1. Logic Map for Meteorological Seasons (Strict Months)
    if (isNorthern) {
        if (mNum in 9..11) meteoSeason = afName
        else if (mNum == 12 || mNum in 1..2) meteoSeason = "Winter"
        else if (mNum in 3..5) meteoSeason = "Spring"
        else meteoSeason = "Summer"
    } else {
        if (mNum in 3..5) meteoSeason = afName
        else if (mNum in 6..8) meteoSeason = "Winter"
        else if (mNum in 9..11) meteoSeason = "Spring"
        else meteoSeason = "Summer"
    }

    // 2. Logic Map for Astronomical Seasons (Equinox/Solstice boundaries)
    if (isNorthern) {
        if ((mNum == 3 && dNum >= 20) || mNum in 4..5 || (mNum == 6 && dNum <= 20)) astroSeason = "Spring"
        else if ((mNum == 6 && dNum >= 21) || mNum in 7..8 || (mNum == 9 && dNum <= 21)) astroSeason = "Summer"
        else if ((mNum == 9 && dNum >= 22) || mNum == 10 || mNum == 11 || (mNum == 12 && dNum <= 20)) astroSeason = afName
        else astroSeason = "Winter"
    } else {
        if ((mNum == 3 && dNum >= 20) || mNum in 4..5 || (mNum == 6 && dNum <= 20)) astroSeason = afName
        else if ((mNum == 6 && dNum >= 21) || mNum in 7..8 || (mNum == 9 && dNum <= 21)) astroSeason = "Winter"
        else if ((mNum == 9 && dNum >= 22) || mNum == 10 || mNum == 11 || (mNum == 12 && dNum <= 20)) astroSeason = "Spring"
        else astroSeason = "Summer"
    }

    sendEvent(name: "meteorologicalSeason", value: meteoSeason)
    sendEvent(name: "astronomicalSeason", value: astroSeason)

    // Determine which calculation drives the primary tile
    String targetSeason = (primaryDisplay == "Astronomical") ? astroSeason : meteoSeason

    processSeasonUpdate(targetSeason, meteoSeason, astroSeason)
}

def processSeasonUpdate(String sName, String meteoName, String astroName) {
    // Extended Data Definitions mapping both Meteorological and Astronomical dates
    def seasonData = [
        "Fall":   [num: 1, img: "fall",   meteoNStart: "Sept 1", meteoNEnd: "Nov 30", meteoSStart: "March 1", meteoSEnd: "May 31", astroNStart: "Sept 22", astroNEnd: "Dec 20", astroSStart: "March 20", astroSEnd: "June 20"],
        "Autumn": [num: 1, img: "autumn", meteoNStart: "Sept 1", meteoNEnd: "Nov 30", meteoSStart: "March 1", meteoSEnd: "May 31", astroNStart: "Sept 22", astroNEnd: "Dec 20", astroSStart: "March 20", astroSEnd: "June 20"],
        "Winter": [num: 2, img: "winter", meteoNStart: "Dec 1",  meteoNEnd: "Feb 28", meteoSStart: "June 1",  meteoSEnd: "Aug 31", astroNStart: "Dec 21", astroNEnd: "March 19", astroSStart: "June 21", astroSEnd: "Sept 21"],
        "Spring": [num: 3, img: "spring", meteoNStart: "March 1", meteoNEnd: "May 31",  meteoSStart: "Sept 1",  meteoSEnd: "Nov 30", astroNStart: "March 20", astroNEnd: "June 20", astroSStart: "Sept 22", astroSEnd: "Dec 20"],
        "Summer": [num: 4, img: "summer", meteoNStart: "June 1",  meteoNEnd: "Aug 31",  meteoSStart: "Dec 1",   meteoSEnd: "Feb 28", astroNStart: "June 21", astroNEnd: "Sept 21", astroSStart: "Dec 21", astroSEnd: "March 19"]
    ]

    def data = seasonData[sName]
    
    // Pick the correct start/end dates based on user's preference of Primary Display and Hemisphere
    String start = ""
    String end = ""
    
    if (primaryDisplay == "Astronomical") {
        start = isNorthern ? data.astroNStart : data.astroSStart
        end = isNorthern ? data.astroNEnd : data.astroSEnd
    } else {
        start = isNorthern ? data.meteoNStart : data.meteoSStart
        end = isNorthern ? data.meteoNEnd : data.meteoSEnd
    }

    String iconBase = iconPathOvr ?: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astro_meteor_seasons/season_icons/"
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
    
    if (txtEnable) log.info "Season updated to ${sName} (${primaryDisplay}). Meteo: ${meteoName} | Astro: ${astroName}"
}