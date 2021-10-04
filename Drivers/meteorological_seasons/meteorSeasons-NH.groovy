/*
 * Meteorological Seasons of the Northern Hemisphere
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
 *      2021-10-03    jshimota      0.1.17      Updated icons and importURLS to matchin github correctly / better
 *      2021-10-03    jshimota      0.1.18      Moved once again as I got folder structure corrected on github
 */

import java.text.SimpleDateFormat
static String version() { return '0.1.18' }

metadata {
    definition(
            name: "Meteorological Seasons of the Northern Hemisphere",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/meteorSeasons-NH.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        // Line below removed as I took out calcDate command
        // command "calcSeason", [[name: "rawDate", type: "STRING", description: "Enter date as (dd-MM-yyyy) to calculate the season for."]]
        command "currentSeason", ["$Cdate"]
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
        // Line below removed as I took out calcDate command
        // attribute "rawDateFormattedMonth", "string"

        // Line below maybe used for split hemisphere stuff
        // attribute "hemisphere", "String"
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input("autoUpdate", "bool", title: "Enable automatic update at 6am\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
    input("htmlVtile", "bool", title: "Use HTML attribute instead of seasonTile\n(Enabled is Yes)")
    input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names fall.svg, winter.svg, spring.svg, summer and unknown.svg)")

    // Line below removed as I took out calcDate command
    // input(name: "rawDateFormat", type: "string", title:"RawDate Format", description: "Enter the date format to apply for entering and displaying RawDate purposes", defaultValue: "dd-MM-yyyy", required: true, displayDuringSetup: true)
    // Line Below is for future Hemisphere work
    // input(name: "hemisphere", type: "bool", title: "Northern Hemisphere", description: "Disable to switch to Southern Hemisphere", defaultValue: true, required: true, displayDuringSetup: true)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def installed() {
    logging("installed()", 100)
    unschedule()
    refresh()
    schedule("0 0 6 * * ?", currentSeason) //  daily at 6am
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
    log.debug("autoupdate: Next scheduled refresh set")
    unschedule()
    refresh()
    schedUpdate()

}

def refresh() {
    runCmd()
    currentSeason()
}

def schedUpdate() {
    unschedule()
    if (txtEnable) log.info("schedUpdate: Refresh schedule cleared ...")
    if (autoUpdate) {
        if (txtEnable) log.info("Update: Setting next scheduled refresh...")
        if (autoUpdate) schedule("0 0 6 * * ?", currentSeason) // daily at 6am
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
    currentSeason()
}

def currentSeason() {
    if (device.currentValue("todaysFormattedMonth") == (null)) {
        runCmd()
    } else if (device.currentValue("todaysFormattedMonth") == ("Not Initialized")) {
        seasonPreviouslySet = false
        existingSeasonName = "${seasonName}"
    } else {
        seasonPreviouslySet = true
        existingSeasonName = "${seasonName}"
    }
    // Line below removed cuz it may be causing problem
    // sendEvent(name: "rawDate", value: rawDate, descriptionText: descriptionText)
    if (device.currentValue("todaysFormattedMonth") == ("September")) {
        fall()
    } else if (device.currentValue("todaysFormattedMonth") == ("October")) {
        fall()
    } else if (device.currentValue("todaysFormattedMonth") == ("November")) {
        fall()
    } else if (device.currentValue("todaysFormattedMonth") == ("December")) {
        winter()
    } else if (device.currentValue("todaysFormattedMonth") == ("January")) {
        winter()
    } else if (device.currentValue("todaysFormattedMonth") == ("February")) {
        winter()
    } else if (device.currentValue("todaysFormattedMonth") == ("March")) {
        spring()
    } else if (device.currentValue("todaysFormattedMonth") == ("April")) {
        spring()
    } else if (device.currentValue("todaysFormattedMonth") == ("May")) {
        spring()
    } else if (device.currentValue("todaysFormattedMonth") == ("June")) {
        summer()
    } else if (device.currentValue("todaysFormattedMonth") == ("July")) {
        summer()
    } else if (device.currentValue("todaysFormattedMonth") == ("August")) {
        summer()
    } else {
        String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
        if (iconPathOvr > " ") iconPath = iconPathOvr
        if (txtEnable) log.info "${descriptionText}"
        sendEvent(name: "seasonName", value: "Not Initialized", descriptionText: descriptionText)
        sendEvent(name: "seasonNum", value: 0, descriptionText: descriptionText)
        sendEvent(name: "seasonBegin", value: "N/A", descriptionText: descriptionText)
        sendEvent(name: "seasonEnd", value: "N/A", descriptionText: descriptionText)
        sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;'><p class='small' style='text-align:center'>(Not Initialized)</p></img></div>", descriptionText: descriptionText)
        sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;' />", descriptionText: descriptionText)
    }
    if (seasonPreviouslySet) {
        descriptionText = "Current season refreshed or changed"
        if (txtEnable) log.info "${descriptionText}"
    } else {
        descriptionText = "Current season initialized to ${seasonName}"
        if (txtEnable) log.info "${descriptionText}"
    }
}

def fall() {
    def descriptionText = "Current season is now Fall" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Fall", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 1, descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "September 1", descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "November 30", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;'><p class='small' style='text-align:center'>Fall</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def winter() {
    def descriptionText = "Current season is now Winter" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Winter", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 2, descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "December 1", descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "February 28", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;'><p class='small' style='text-align:center'>Winter</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def spring() {
    def descriptionText = "Current season is now Spring" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Spring", descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "March 1", descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "May 31", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 3, descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;'><p class='small' style='text-align:center'>Spring</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def summer() {
    def descriptionText = "Current season is now Summer" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Summer", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 4, descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "June 1", descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "August 31", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;'><p class='small' style='text-align:center'>Summer</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

/* def calcSeason() {
*    // selectedTimeZone = TimeZone.getTimeZone(timeZone)
*
*    simpleDateFormatForRawDateMonth = new SimpleDateFormat('dd-MM-yyyy')
*    // Line Below commented cuz I removed timezone
*    // simpleDateFormatForRawDateMonth.setTimeZone(selectedTimeZone)
*
*    proposedFormattedRawDateMonth = simpleDateFormatForRawDateMonth.format('MMMM')
*
*   sendEvent(name: "rawDateFormattedMonth", value: proposedFormattedRawDateMonth, descriptionText: descriptionText)
*    // sendEvent(name: "rawDate", value: ${rawDate}, descriptionText: descriptionText)
*
*   def descriptionText = "Current season is ${device.displayName}" as Object
*   String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
*   if (iconPathOvr > " ") iconPath = iconPathOvr
*   if (txtEnable) log.info "${descriptionText}"
*   if (device.currentValue("rawDateFormattedMonth") == ("September")) {
*       fall()
*   } else if (device.currentValue("rawDateFormattedMonth") == ("October")) {
*       fall()
*   } else if (device.currentValue("rawDateFormattedMonth") == ("November")) {
*       fall()
*   } else if (device.currentValue("rawDateFormattedMonth") == ("December")) {
*        winter()
*    } else if (device.currentValue("rawDateFormattedMonth") == ("January")) {
*        winter()
*    } else if (device.currentValue("rawDateFormattedMonth") == ("February")) {
*        winter()
*    } else if (device.currentValue("rawDateFormattedMonth") == ("March")) {
*        spring()
*    } else if (device.currentValue("rawDateFormattedMonth") == ("April")) {
*        spring()
*    } else if (device.currentValue("rawDateFormattedMonth") == ("May")) {
*        spring()
*    } else if (device.currentValue("rawDateFormattedMonth") == ("June")) {
*        summer()
*    } else if (device.currentValue("rawDateFormattedMonth") == ("July")) {
*        summer()
*    } else if (device.currentValue("rawDateFormattedMonth") == ("August")) {
*       summer()
*    } else {
*        sendEvent(name: "seasonName", value: "Not Initialized", descriptionText: descriptionText)
*        sendEvent(name: "seasonNum", value: 0, descriptionText: descriptionText)
*        sendEvent(name: "seasonBegin", value: "N/A", descriptionText: descriptionText)
*        sendEvent(name: "seasonEnd", value: "N/A", descriptionText: descriptionText)
*        sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;'><p class='small' style='text-align:center'>(Not Initialized)</p></img></div>", descriptionText: descriptionText)
*        sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;' />", descriptionText: descriptionText)
*    }
*}
*
*/