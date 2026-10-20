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
 *      2026-05-14    jshimota      0.3.4       Fix to Package Json required true
 *      2026-07-23    jshimota      0.3.5       review and update to Initialize issue
 *      2026-07-23    jshimota      0.3.6       Improved logging and cut back on sendEvents
 *      2026-07-23    jshimota      0.3.7       Updated icon path preference and default URL
 *      2026-10-20    jshimota      0.4.0       Flexbox & EM-Only Refactor: Purged percentage/px units, updated preferences to pure EM scales.
 *      2026-10-20    jshimota      0.4.1       Corrective Review & Hardening: Differentiated EM validation ranges for font size (0.5em..5.0em) and offset (-3.0em..3.0em); hardened Hex/Color CSS validation; normalized icon base URLs; safe season key validation; converted seasonImg to EM sizing (6.25em); hardened tile HTML container.
 *
 */

import java.text.SimpleDateFormat

static String version() { return '0.4.1' }

metadata {
    definition(
        name: "Astronomical & Meteorological Seasons",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/astro_meteor_seasons/astroMeteorSeasons.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        capability "Initialize"
        
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
    input name: "primaryDisplay", type: "enum", title: "<b>Primary Dashboard Display</b>", options: ["Meteorological", "Astronomical"], defaultValue: "Meteorological", description: "<i>Select Season Type: Astronomical which is determined by the Earth's position in its orbit relative to the Sun <b>- or -</b> Meteorological, which groups the year into four equal, three-month static blocks</i>"  
    input name: "iconPathOvr", type: "string", title: "<b>Override path to season icons</b>", defaultValue: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/season_icons/", description: "<i>Use this if you have a different set of images. Should end with /.  eg; http://192.168.1.12/myicons/seasons_icons/</i>"  
    input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: false, description: "<i>Enable for problem solving - recommended OFF</i>"  
    input name: "txtEnable", type: "bool", title: "<b>Enable Info logging</b>", defaultValue: true, description: "<i>Enable for general log entries - recommended ON</i>"  
    
    input name: "fontSizeEm", type: "text", title: "<b>HTML Tile Font Scale (EM / Base 1.0 = 100%)</b>", defaultValue: "1.8", description: "<i>Adjust text size multiplier in relative EM units (Range: 0.5 to 5.0). Default: 1.8</i>"  
    input name: "vertOffsetEm", type: "text", title: "<b>HTML Tile Vertical Offset (EM)</b>", defaultValue: "0.0", description: "<i>Adjust text vertical alignment relative to center in EM units (Range: -3.0 to 3.0). Default: 0.0</i>"  
    input name: "fontColor", type: "string", title: "<b>HTML Tile Text Color (Hex)</b>", defaultValue: "#ffffffff", description: "<i>Adjust the text color. Color name or hex value - eg; #ffffff, #ffffffff, or white. Default: #ffffffff</i>"  
    
    input name: "autoUpdate", type: "bool", title: "<b>Enable automatic update at 6am</b>", defaultValue: true, description: "<i>On (Recommended) to enable automatic update. Disable for testing or to have tile static for other purposes</i>"  
    input name: "isNorthern", type: "bool", title: "<b>Hemisphere (On=Northern / Off=Southern)</b>", defaultValue: true, description: "<i>For our friends in the south - Seasons are reversed, and this accounts for their needs.</i>"  
    input name: "useAutumn", type: "bool", title: "<b>Season Naming (On=Autumn / Off=Fall)</b>", defaultValue: false, description: "<i>Choose which word to use for the 3rd season, default is Fall</i>"  
}

def installed() {
    refresh()
}

def updated() {
    logInfo "Settings updated."
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

def initialize() {
    unschedule()
    if (autoUpdate) schedule("0 0 6 * * ?", refresh)
    refresh()
}

// Shortcut commands for manual override (temporary until next automated 6:00 AM refresh or manual refresh call)
def fall()   { setSeasonManual("Fall") }
def autumn() { setSeasonManual("Autumn") }
def winter() { setSeasonManual("Winter") }
def spring() { setSeasonManual("Spring") }
def summer() { setSeasonManual("Summer") }

def setSeasonManual(String sName) {
    logInfo "Manual override command received: Setting season temporarily to ${sName}"
    processSeasonUpdate(sName, sName, sName) // Overrides both to the manual selection
}

def updateSeason() {
    Date now = new Date()
    String dateStr = new SimpleDateFormat("dd-MM-yyyy").format(now)
    int mNum = new SimpleDateFormat("MM").format(now).toInteger()
    int dNum = new SimpleDateFormat("dd").format(now).toInteger()
    logDebug "Calculating season for Date: ${dateStr} (Month: ${mNum}, Day: ${dNum})"
    sendIfChanged(name: "todaysFormattedDate", value: dateStr)
    
    String hName = isNorthern ? "Northern" : "Southern"
    String afName = useAutumn ? "Autumn" : "Fall"
    
    sendIfChanged(name: "hemisphereName", value: hName)
    sendIfChanged(name: "autumnFallName", value: afName)

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

    sendIfChanged(name: "meteorologicalSeason", value: meteoSeason)
    sendIfChanged(name: "astronomicalSeason", value: astroSeason)

    // Determine which calculation drives the primary tile
    String targetSeason = (primaryDisplay == "Astronomical") ? astroSeason : meteoSeason

    processSeasonUpdate(targetSeason, meteoSeason, astroSeason)
}

/**
 * Validates and sanitizes numeric EM input parameters.
 * Strip optional 'em' suffix, validates strict numeric bounds, and returns safe EM string.
 */
private String sanitizeFontSizeEm(Object inputVal, String defaultVal = "1.8em") {
    if (!inputVal) return defaultVal
    String str = inputVal.toString().trim().toLowerCase()
    if (str.endsWith("em")) str = str.substring(0, str.length() - 2).trim()
    
    try {
        BigDecimal num = str.toBigDecimal()
        if (num >= 0.5g && num <= 5.0g) {
            return "${num}em"
        }
    } catch (Exception e) {
        // Fall back to default on parse error
    }
    return defaultVal
}

private String sanitizeOffsetEm(Object inputVal, String defaultVal = "0.0em") {
    if (!inputVal) return defaultVal
    String str = inputVal.toString().trim().toLowerCase()
    if (str.endsWith("em")) str = str.substring(0, str.length() - 2).trim()
    
    try {
        BigDecimal num = str.toBigDecimal()
        if (num >= -3.0g && num <= 3.0g) {
            return "${num}em"
        }
    } catch (Exception e) {
        // Fall back to default on parse error
    }
    return defaultVal
}

/**
 * Validates CSS font color strings to prevent CSS injection.
 * Accepts standard 3, 6, or 8 character hex colors or named color strings.
 */
private String sanitizeFontColor(Object inputVal, String defaultVal = "#ffffffff") {
    if (!inputVal) return defaultVal
    String str = inputVal.toString().trim()
    
    // Validate standard Hex format (#RGB, #RRGGBB, #RRGGBBAA)
    if (str.matches(/^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/)) {
        return str
    }
    
    // Validate CSS basic color names
    if (str.matches(/^[a-zA-Z]{3,20}$/)) {
        return str
    }
    
    return defaultVal
}

/**
 * Normalizes icon directory URL to ensure exactly one trailing slash separator.
 */
private String normalizeIconBaseUrl(String rawUrl) {
    String defaultBase = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/season_icons/"
    if (!rawUrl || rawUrl.trim().isEmpty()) return defaultBase
    
    String trimmed = rawUrl.trim()
    while (trimmed.endsWith("/")) {
        trimmed = trimmed.substring(0, trimmed.length() - 1)
    }
    return "${trimmed}/"
}

def processSeasonUpdate(String sName, String meteoName, String astroName) {
    // Extended Data Definitions mapping both Meteorological and Astronomical dates
    def seasonData = [
        "Fall":   [num: 1, img: "fall",   meteoNStart: "Sept 1", meteoNEnd: "Nov 30", meteoSStart: "March 1", meteoSEnd: "May 31", astroNStart: "Sept 22", astroNEnd: "Dec 20", astroSStart: "March 20", astroSEnd: "June 20"],
        "Autumn": [num: 1, img: "autumn", meteoNStart: "Sept 1", meteoNEnd: "Nov 30", meteoSStart: "March 1", meteoSEnd: "May 31", astroNStart: "Sept 22", astroNEnd: "Dec 20", astroSStart: "March 20", astroSEnd: "June 20"],
        "Winter": [num: 2, img: "winter", meteoNStart: "Dec 1",  meteoNEnd: "Feb 28/29", meteoSStart: "June 1",  meteoSEnd: "Aug 31", astroNStart: "Dec 21", astroNEnd: "March 19", astroSStart: "June 21", astroSEnd: "Sept 21"],
        "Spring": [num: 3, img: "spring", meteoNStart: "March 1", meteoNEnd: "May 31",  meteoSStart: "Sept 1",  meteoSEnd: "Nov 30", astroNStart: "March 20", astroNEnd: "June 20", astroSStart: "Sept 22", astroSEnd: "Dec 20"],
        "Summer": [num: 4, img: "summer", meteoNStart: "June 1",  meteoNEnd: "Aug 31",  meteoSStart: "Dec 1",   meteoSEnd: "Feb 28/29", astroNStart: "June 21", astroNEnd: "Sept 21", astroSStart: "Dec 21", astroSEnd: "March 19"]
    ]

    // 5. Validate season name prior to map lookup
    if (!sName || !seasonData.containsKey(sName)) {
        logWarn "Unrecognized season name '${sName}' requested. Processing aborted without state updates."
        return
    }

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

    String iconBase = normalizeIconBaseUrl(iconPathOvr)
    String imgUrl = "${iconBase}${data.img}.svg"
    
    // Sanitize and resolve EM typography parameters and font colors
    String fSizeEm = sanitizeFontSizeEm(settings?.fontSizeEm, "1.8em")
    String vOffsetEm = sanitizeOffsetEm(settings?.vertOffsetEm, "0.0em")
    String fColor = sanitizeFontColor(settings?.fontColor, "#ffffffff")

    // Responsive Flexbox Card Structure (Container percentages fill 100% tile bounds; contents scale via relative EM)
    String tileHtml = "<div style='width:100%; height:100%; display:flex; flex-direction:column; justify-content:center; align-items:center; " +
                      "background-image:url(${imgUrl}); background-position:center; background-repeat:no-repeat; background-size:contain; " +
                      "box-sizing:border-box; padding:0.5em; overflow:hidden;'>" +
                      "<div style='font-family: Georgia, serif; text-align:center; font-size:${fSizeEm}; color:${fColor}; " +
                      "text-transform:uppercase; font-style:oblique; font-weight:bold; margin:0; padding:0; line-height:1.0em; " +
                      "transform:translateY(${vOffsetEm}); text-shadow:0.05em 0.05em 0.1em rgba(0,0,0,0.6);'>" +
                      "<span>${sName}</span></div></div>"

    sendIfChanged(name: "seasonName", value: sName)
    sendIfChanged(name: "seasonNum", value: data.num)
    sendIfChanged(name: "seasonBegin", value: start)
    sendIfChanged(name: "seasonEnd", value: end)
    sendIfChanged(name: "seasonImg", value: "<img src='${imgUrl}' style='height:6.25em;'/>")
    sendIfChanged(name: "seasonTile", value: tileHtml)
    
    logInfo "Season updated to ${sName} (${primaryDisplay}). Meteo: ${meteoName} | Astro: ${astroName}"
}

private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    
    // Standardize checking logic
    String oldVal = device.currentValue(args.name as String)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        // Ensure standard Hubitat tracking options are appended safely
        Map eventMap = [name: args.name, value: args.value, descriptionText: "Attribute ${args.name} changed to ${args.value}"]
        if (args.unit) eventMap.unit = args.unit
        sendEvent(eventMap)
        logDebug "Event triggered: ${args.name} -> ${args.value}"
    }
}

// Unified dynamic logger helper mapping
private void logMessage(String level, String msg) {
    boolean isEnabled = false
    if (level == "info") {
        isEnabled = (settings.txtEnable != false) // Default ON
    } else if (level == "debug") {
        isEnabled = (settings.logEnable == true) // Default OFF
    } else {
        isEnabled = true // Always log warnings and errors
    }

    if (isEnabled) {
        log."${level}" "Seasons Driver${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }