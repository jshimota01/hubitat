/*
 * Meteorological Season of the Northern Hemisphere
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
 *      Date          Source      Version     What                                              URL
 *      ----          ------      -------     ----                                              ---
 *      2021-09-30    jshimota    0.1.0       Starting version
 *      2021-09-30    thebearmay  0.1.1       Used MoonPhase app components                     https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhase.groovy
 *      2021-09-30    sburke781   0.1.2       Used DateFormat app components                    https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
 *      2021-09-30    luarmr      0.1.3       Used DateFormat app components                    https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
 *      2021-09-30    bravenel    0.1.4       Used Virtual Omni Sensor driver components        https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/virtualOmniSensor.groovy
 *      2021-09-30    jshimota    0.1.5-11    Me working
 */

import java.text.SimpleDateFormat
static String version()	{  return '0.1.10'  }


metadata
definition (
        name: "Meteorological Season of the Northern Hemisphere",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl:"https://raw.githubusercontent.com/jshimota01/Meteorical_Seasons/master/meteorSeason-NH.groovy"
)  {
    capability "Actuator"
    capability "Refresh"
    command "calcSeason", [[name:"rawDate", type:"STRING", description:"Enter date as (yyyy-MM-dd) to calculate the season for."]]
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
    attribute "seasonBegin", "string"
    attribute "seasonEnd", "string"
    attribute "seasonTile", "string"
    attribute "seasonImg", "string"
    attribute "todaysFormattedDate", "string"
    attribute "todaysFormattedMonth", "string"
    attribute "todaysFormattedTime", "string"
    attribute "todaysHtmlFriendlyDate", "string"
    attribute "rawDateFormattedMonth", "string"

    // Line below maybe used for split hemisphere stuff
    // attribute "seasonNum", "number"
    // attribute "hemisphere", "String"

}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input("autoUpdate", "bool", title: "Enable automatic update at midnight\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
    input("htmlVtile", "bool", title:"Use HTML attribute instead of seasonTile\n(Enabled is Yes)")
    input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names fall.svg, winter.svg, spring.svg, summer and unknown.svg)")
    input(name: "timeZone", type: "enum", title: "Select TimeZone:", description: "", multiple: false, required: true, defaultValue:getLocation().timeZone.getID(), options: TimeZone.getAvailableIDs())

    // Lines below taken out but hardset in Installed() section
    // input(name: "dateFormat", type: "string", title:"Date Format", description: "Enter the date format to apply for display purposes", defaultValue: "yyyy-MM-dd", required: true, displayDuringSetup: true)
    // input(name: "monthFormat", type: "string", title:"Month Format", description: "Enter the Month format to apply for display purposes", defaultValue: "MMMM", required: true, displayDuringSetup: true)
    // input(name: "rawDateMonthFormat", type: "string", title:"Raw Date Month Format", description: "Enter the Raw Date Month format to apply for display purposes", defaultValue: "MMMM", required: true, displayDuringSetup: true)
    // input(name: "timeFormat", type: "string", title:"Time Format", description: "Enter the time format to apply for display purposes", defaultValue: "HH:mm:ss.SSSZ", required: false, displayDuringSetup: true)
    // input(name: "AutoUpdateInterval", type: "ENUM", multiple: false, options: ["20", "30", "60", "300", "1800"], title:"Auto Update Interval", description: "Number of seconds between automatic updates", defaultValue: 1800, required: true, displayDuringSetup: true)

    // Line Below is for future Hemisphere work
    // input(name: "hemisphere", type: "bool", title: "Northern Hemisphere", description: "Disable to switch to Southern Hemisphere", defaultValue: true, required: true, displayDuringSetup: true)
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    log.trace "installed()"
    // timeZone() = getLocation().timeZone.getID()
    dateFormat(yyyy-MM-dd)
    monthFormat(MMMM)
    rawDateMonthFormat(MMMM)
    // Line Below I never got to work
    // timeFormat(HH:mm:ss.SSSZ)
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == false}"
    log.warn "description logging is: ${txtEnable == false}"

    // Line below not working due to removal of autoupdate input line above
    // log.debug("updated: AutoPolling = ${AutoUpdate}, StatusPollingInterval = ${AutoUpdateInterval}")
    updatePolling()
    if (logEnable) runIn(1800,logsOff)
}

def refresh() {
    runCmd()
    currentSeason()
}

def runCmd() {
    now = new Date()
    selectedTimeZone = TimeZone.getTimeZone(timeZone)

    simpleDateFormatForDate = new SimpleDateFormat(dateFormat)
    simpleDateFormatForDate.setTimeZone(selectedTimeZone)

    simpleDateFormatForMonth = new SimpleDateFormat(monthFormat)
    simpleDateFormatForMonth.setTimeZone(selectedTimeZone)

    proposedFormattedDate = simpleDateFormatForDate.format(now)
    proposedFormattedMonth = simpleDateFormatForMonth.format(now)
    proposedHtmlFriendlyDate = "<span class=\"dateFormat\">${proposedFormattedDate}</span>"


    sendEvent(name: "todaysFormattedDate", value : proposedFormattedDate)
    sendEvent(name: "todaysFormattedMonth", value : proposedFormattedMonth)
    sendEvent(name: "todaysHtmlFriendlyDate", value : proposedHtmlFriendlyDate)
}

def getSchedule() { }

def updatePolling() {

    def sched
    log.debug("updatePolling: Updating Automatic Polling called, about to unschedule refresh")
    unschedule("refresh")
    log.debug("updatePolling: Unscheduling refresh complete")

    if(AutoUpdate == true) {

        sched = "3600 * * ? * * *"
        log.debug("updatePolling: Setting up schedule with settings: schedule(3600,refresh)")
        try{

            schedule("${sched}","refresh")
        }
        catch(Exception e) {
            log.error("updatePolling: Error - " + e)
        }

        log.debug("updatePolling: Scheduled refresh set")
    }
    else { log.debug("updatePolling: Automatic status polling disabled")  }
}

def parse(String description) {
}

def currentSeason() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
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
        sendEvent(name: "rawDate", value : rawDate,descriptionText: descriptionText)
        sendEvent(name: "seasonName", value: "Not Initialized",descriptionText: descriptionText)
        sendEvent(name: "seasonNum", value: 0,descriptionText: descriptionText)
        sendEvent(name: "seasonBegin", value: "N/A",descriptionText: descriptionText)
        sendEvent(name: "seasonEnd", value: "N/A",descriptionText: descriptionText)
        sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;'><p class='small' style='text-align:center'>(Not Initialized)</p></img></div>",descriptionText: descriptionText)
        sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;' />",descriptionText: descriptionText)
    }
}

def calcSeason() {
    selectedTimeZone = TimeZone.getTimeZone(timeZone)

    simpleDateFormatForRawDateMonth = new SimpleDateFormat(rawDateMonthFormat)
    simpleDateFormatForRawDateMonth.setTimeZone(selectedTimeZone)

    proposedFormattedRawDateMonth = simpleDateFormatForRawDateMonth.format(rawDateMonthFormat)

    sendEvent(name: "rawDateFormattedMonth", value : proposedFormattedRawDateMonth)

    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    if (device.currentValue("rawDateFormattedMonth") == ("September")) {
        fall()
    } else if (device.currentValue("rawDateFormattedMonth") == ("October")) {
        fall()
    } else if (device.currentValue("rawDateFormattedMonth") == ("November")) {
        fall()
    } else if (device.currentValue("rawDateFormattedMonth") == ("December")) {
        winter()
    } else if (device.currentValue("rawDateFormattedMonth") == ("January")) {
        winter()
    } else if (device.currentValue("rawDateFormattedMonth") == ("February")) {
        winter()
    } else if (device.currentValue("rawDateFormattedMonth") == ("March")) {
        spring()
    } else if (device.currentValue("rawDateFormattedMonth") == ("April")) {
        spring()
    } else if (device.currentValue("rawDateFormattedMonth") == ("May")) {
        spring()
    } else if (device.currentValue("rawDateFormattedMonth") == ("June")) {
        summer()
    } else if (device.currentValue("rawDateFormattedMonth") == ("July")) {
        summer()
    } else if (device.currentValue("rawDateFormattedMonth") == ("August")) {
        summer()
    } else {
        sendEvent(name: "rawDate", value : ${rawDate},descriptionText: descriptionText)
        sendEvent(name: "seasonName", value: "Not Initialized",descriptionText: descriptionText)
        sendEvent(name: "seasonNum", value: 0,descriptionText: descriptionText)
        sendEvent(name: "seasonBegin", value: "N/A",descriptionText: descriptionText)
        sendEvent(name: "seasonEnd", value: "N/A",descriptionText: descriptionText)
        sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;'><p class='small' style='text-align:center'>(Not Initialized)</p></img></div>",descriptionText: descriptionText)
        sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;' />",descriptionText: descriptionText)
    }
}

def fall() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Fall",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 4,descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "September 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "November 30",descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;'><p class='small' style='text-align:center'>Fall</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;' />",descriptionText: descriptionText)
}

def winter() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Winter",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 1,descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "December 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "February 28",descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;'><p class='small' style='text-align:center'>Winter</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;' />",descriptionText: descriptionText)
}
def spring() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Spring",descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "March 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "May 31",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 2,descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;'><p class='small' style='text-align:center'>Spring</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;' />",descriptionText: descriptionText)
}
def summer() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Summer",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 3,descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "June 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "August 31",descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;'><p class='small' style='text-align:center'>Summer</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;' />",descriptionText: descriptionText)
}