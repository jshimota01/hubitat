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
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
		input("autoUpdate", "bool", title: "Enable automatic update at midnight")
		input("htmlVtile", "bool", title:"Use html attribute instead of seasonTile")
		input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names fall.svg, winter.svg, spring.svg and summer.svg)")
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
    if (logEnable) runIn(1800,logsOff)
}

def parse(String description) {
}

def currentSeason() {
    def descriptionText = "Current season is ${device.displayName}"
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Not Initialized",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 5,descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;'><p class='small' style='text-align:center'>$(seasonName)</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;' />",descriptionText: descriptionText)
}

def fall() {
    def descriptionText = "Current season is ${device.displayName}"
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Fall",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 0,descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "September 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "November 30",descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;'><p class='small' style='text-align:center'>$(seasonName)</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;' />",descriptionText: descriptionText)
}

def winter() {
    def descriptionText = "Current season is ${device.displayName}"
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Winter",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 1,descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "December 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "February 28",descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;'><p class='small' style='text-align:center'>$(seasonName)</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;' />",descriptionText: descriptionText)
}
def spring() {
    def descriptionText = "Current season is ${device.displayName}"
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Spring",descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "March 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "May 31",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 2,descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;'><p class='small' style='text-align:center'>$(seasonName)</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;' />",descriptionText: descriptionText)
}
def summer() {
    def descriptionText = "Current season is ${device.displayName}"
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Summer",descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 3,descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "June 1",descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "August 31",descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;'><p class='small' style='text-align:center'>$(seasonName)</p></img></div>",descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;' />",descriptionText: descriptionText)
}