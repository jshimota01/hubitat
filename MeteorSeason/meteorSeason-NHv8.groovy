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
 *    Date        Who            What
 *    ----        ---            ----
 *    2021-09-29  thebearmay	 Original code from Moon Phase driver
 *	  2021-09-30  jshimota       Began modifying
 */

import java.text.SimpleDateFormat
static String version()	{  return '0.7.3'  }


metadata
definition (
        name: "Meteorological Season of the Northern Hemisphere",
        namespace: "jshimota",
        author: "James Shimota",
        importUrl:"https://raw.githubusercontent.com/jshimota01/hubitat/main/meteorSeason-NH.groovy"
)  {
    capability "Actuator"
    capability "Refresh"
    command "calcSeason", [[name:"dateStr", type:"STRING", description:"Enter date as (yyyy-MM-dd HH:mm:ss) to calculate the season for."]]
    command "currentSeason", ["$Cdate"]
    command "fall",["Date Range"]
    command "winter",["Date Range"]
    command "spring",["Date Range"]
    command "summer",["Date Range"]

    attribute "seasonName", "string"
    attribute "seasonNum", "number"
    attribute "seasonBegin", "date"
    attribute "seasonEnd", "date"
    attribute "seasonTile", "string"
    attribute "seasonImg", "string"
    attribute "variable", "string"
    attribute "html", "string"
    attribute "todaysFormattedDate", "string"
    attribute "todaysFormattedMonth", "string"
    attribute "todaysFormattedTime", "string"
    attribute "todaysHtmlFriendlyDateTime", "string"
    attribute "hemisphere", "String"
    attribute "rawDate", "string"
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input(name: "hemisphere", type: "bool", title: "Northern Hemisphere", description: "Disable to switch to Southern Hemisphere", defaultValue: true, required: true, displayDuringSetup: true)
    input("autoUpdate", "bool", title: "Enable automatic update at midnight")
    input("htmlVtile", "bool", title:"Use html attribute instead of seasonTile")
    input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names fall.svg, winter.svg, spring.svg and summer.svg)")
    input(name: "timeZone", type: "enum", title: "Select TimeZone:", description: "", multiple: false, required: true, defaultValue:getLocation().timeZone.getID(), options: TimeZone.getAvailableIDs())
    input(name: "dateFormat", type: "string", title:"Date Format", description: "Enter the date format to apply for display purposes", defaultValue: "yyyy-MM-dd", required: true, displayDuringSetup: true)
    input(name: "monthFormat", type: "string", title:"Month Format", description: "Enter the Month format to apply for display purposes", defaultValue: "MMMM", required: true, displayDuringSetup: true)
    input(name: "timeFormat", type: "string", title:"Time Format", description: "Enter the time format to apply for display purposes", defaultValue: "HH:mm:ss.SSSZ", required: false, displayDuringSetup: true)
    input(name: "AutoUpdate", type: "bool", title:"Automatic Update", description: "Enable / Disable automatic update to date", defaultValue: true, required: true, displayDuringSetup: true)
    input(name: "AutoUpdateInterval", type: "ENUM", multiple: false, options: ["20", "30", "60", "300", "1800"], title:"Auto Update Interval", description: "Number of seconds between automatic updates", defaultValue: 1800, required: true, displayDuringSetup: true)
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    log.trace "installed()"
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == false}"
    log.warn "description logging is: ${txtEnable == false}"
    log.debug("updated: AutoPolling = ${AutoUpdate}, StatusPollingInterval = ${AutoUpdateInterval}")
    updatePolling()
    if (logEnable) runIn(1800,logsOff)
}

def refresh() {
    runCmd()
}

def runCmd() {
    now = new Date()
    selectedTimeZone = TimeZone.getTimeZone(timeZone)

    simpleDateFormatForDate = new SimpleDateFormat(dateFormat)
    simpleDateFormatForDate.setTimeZone(selectedTimeZone)

    simpleDateFormatForMonth = new SimpleDateFormat(monthFormat)
    simpleDateFormatForMonth.setTimeZone(selectedTimeZone)

    simpleDateFormatForTime = new SimpleDateFormat(timeFormat)
    simpleDateFormatForTime.setTimeZone(selectedTimeZone)


    proposedFormattedDate = simpleDateFormatForDate.format(now)
    proposedFormattedMonth = simpleDateFormatForMonth.format(now)
    proposedFormattedTime = simpleDateFormatForTime.format(now)
    proposedHtmlFriendlyDateTime = "<span class=\"timeFormat\">${proposedFormattedTime}</span> <span class=\"dateFormat\">${proposedFormattedDate}</span>"

    sendEvent(name: "todaysFormattedDate", value : proposedFormattedDate)
    sendEvent(name: "todaysFormattedMonth", value : proposedFormattedMonth)
    sendEvent(name: "todaysFormattedTime", value : proposedFormattedTime)
    sendEvent(name: "todaysHtmlFriendlyDateTime", value : proposedHtmlFriendlyDateTime)
}

def getSchedule() { }

def updatePolling() {

    def sched
    log.debug("updatePolling: Updating Automatic Polling called, about to unschedule refresh")
    unschedule("refresh")
    log.debug("updatePolling: Unscheduleing refresh complete")

    if(AutoUpdate == true) {

        sched = "2/${AutoUpdateInterval} * * ? * * *"
        log.debug("updatePolling: Setting up schedule with settings: schedule(\"${sched}\",refresh)")
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
	} elseif (device.currentValue("todaysFormattedMonth") == ("October")) {
		fall()
	} elseif (device.currentValue("todaysFormattedMonth") == ("November")) {
		fall()
	} elseif (device.currentValue("todaysFormattedMonth") == ("December")) {
		winter()
	} elseif (device.currentValue("todaysFormattedMonth") == ("January")) {
		winter()
	} elseif (device.currentValue("todaysFormattedMonth") == ("February")) {
		winter()
	} elseif (device.currentValue("todaysFormattedMonth") == ("March")) {
		spring()
	} elseif (device.currentValue("todaysFormattedMonth") == ("April")) {
		spring()
	} elseif (device.currentValue("todaysFormattedMonth") == ("May")) {
		spring()
	} elseif (device.currentValue("todaysFormattedMonth") == ("June")) {
		summer()
	} elseif (device.currentValue("todaysFormattedMonth") == ("July")) {
		summer()
	} elseif (device.currentValue("todaysFormattedMonth") == ("August")) {
		summer()
	} else { 
        sendEvent(name: "rawDate", value : rawdate,descriptionText: descriptionText)
        sendEvent(name: "seasonName", value: "Not Initialized",descriptionText: descriptionText)
        sendEvent(name: "seasonNum", value: 5,descriptionText: descriptionText)
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
    sendEvent(name: "rawDate", value : rawdate,descriptionText: descriptionText)
    sendEvent(name: "seasonName", value: "Fall",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 0,descriptionText: descriptionText)
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
    sendEvent(name: "rawDate", value : rawdate,descriptionText: descriptionText)
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
    sendEvent(name: "rawDate", value : rawdate,descriptionText: descriptionText)
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
    sendEvent(name: "rawDate", value : rawdate,descriptionText: descriptionText)
    sendEvent(name: "seasonName", value: "Summer",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 3,descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "June 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "August 31",descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;'><p class='small' style='text-align:center'>Summer</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;' />",descriptionText: descriptionText)
}