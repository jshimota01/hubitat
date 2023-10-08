/**
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Inverse Link Two Switches
 *
 *  Author: kahn@lgk.com
 *
 *  Date: 2015-11-08
 */
 
 /**
 *  Inverse Link Two Switches Parent
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/**
 *  Change History:
 *
 *      ORIGINAL SOURCE
 *      2015          kahn@lgk.com  no version 
 *
 *      Date          Source        Version          What                                              URL
 *      ----          ------        -------          ----                                              ---
 *      2023-10-08    jshimota      0.1.0.1          My 'fork' of this driver taken from the above original source
 *      2023-10-08    jshimota      0.1.0.2          Initial edits to create reversal
 *      2023-10-08    jshimota      0.1.0.3          Updates to create HPM usability
 *      2023-10-08    jshimota      0.1.0.4          Split to a Parent / child app. 
 *      2023-10-08    bturcott      0.1.0.5          Used Brians code to create basic Parent/child app as model
 *        
 */
 
def setVersion(){
    state.name = "Inverse Link Two Switches"
	state.version = "0.1.0.5"
}
import groovy.json.*
import hubitat.helper.RMUtils
import java.util.TimeZone
import groovy.transform.Field
import groovy.time.TimeCategory
import java.text.SimpleDateFormat


def checkHubVersion() {
    hubVersion = getHubVersion()
    hubFirmware = location.hub.firmwareVersionString
    if(txtEnabled) log.info "In checkHubVersion - Info: ${hubVersion} - ${hubFirware}"
}

def parentCheck(){  
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
		parentChild = true
  	} else {
    	parentChild = false
  	}
}

def appControlSection() {
    input "pauseApp", "bool", title: "Pause App", defaultValue:false, submitOnChange:true
    if(pauseApp) {
        if(app.label) {
            if(!app.label.contains("(Paused)")) {
                app.updateLabel(app.label + " <span style='color:red'>(Paused)</span>")
            }
        }
    } else {
        if(app.label) {
            if(app.label.contains("(Paused)")) {
                app.updateLabel(app.label - " <span style='color:red'>(Paused)</span>")
            }
        }
    }
    if(pauseApp) { 
        paragraph app.label
    } else {
        label title: "Enter a name for this automation", required:true, submitOnChange:true
    }
}

def uninstalled() {
    sendLocationEvent(name: "updateVersionInfo", value: "${app.id}:remove")
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

// ********** Normal Stuff **********
def logsOff() {
    if(txtEnabled) log.info "${app.label} - Debug logging auto disabled"
    app.updateSetting("txtEnabled",[value:"false",type:"bool"])
}

def checkEnableHandler() {
    setVersion()
    state.eSwitch = false
    if(disableSwitch) { 
        if(dbgEnabled) log.debug "In checkEnableHandler - disableSwitch: ${disableSwitch}"
        disableSwitch.each { it ->
            theStatus = it.currentValue("switch")
            if(theStatus == "on") { state.eSwitch = true }
        }
        if(dbgEnabled) log.debug "In checkEnableHandler - eSwitch: ${state.eSwitch}"
    }
}

def getImage(type) {					// Modified from @Stephack Code
    def loc = "<img src=https://raw.githubusercontent.com/bptworld/Hubitat/master/resources/images/"
    if(type == "Blank") return "${loc}blank.png height=40 width=5}>"
    if(type == "checkMarkGreen") return "${loc}checkMarkGreen2.png height=30 width=30>"
    if(type == "optionsGreen") return "${loc}options-green.png height=30 width=30>"
    if(type == "optionsRed") return "${loc}options-red.png height=30 width=30>"
    if(type == "instructions") return "${loc}instructions.png height=30 width=30>"
    if(type == "logo") return "${loc}logo.png height=40>"
}

def getFormat(type, myText=null, page=null) {			// Modified code from @Stephack
    if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid #000000;box-shadow: 2px 3px #8B8F8F;border-radius: 10px'>${myText}</div>"
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;' />"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
    
    if(type == "button-blue") return "<a style='color:white;text-align:center;font-size:20px;font-weight:bold;background-color:#03FDE5;border:1px solid #000000;box-shadow:3px 4px #8B8F8F;border-radius:10px' href='${page}'>${myText}</a>"
}

def display(data) {
    if(data == null) data = ""
    if(app.label) {
        if(app.label.contains("(Paused)")) {
            theName = app.label - " <span style='color:red'>(Paused)</span>"
        } else {
            theName = app.label
        }
    }
    if(theName == null || theName == "") theName = "New Child App"
    if(!state.name) { state.name = "" }
    if(state.name == theName) {
        headerName = state.name
    } else {
        if(state.name == null || state.name == "") {
            headerName = "${theName}"
        } else {
            headerName = "${state.name} - ${theName}"
        }
    }
}

definition(
	name: "Inverse Link Two Switches",
	namespace: "jshimota",
	author: "James Shimota",
	description: "Based on the Link Two Switches by Larry Kahn, if one switch goes on, the other goes off and vice versa  ",
	category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/inverse_link/inverse_link.groovy",
)

preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
} 

def installed() {
    if(dbgEnabled) log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    if(txtEnabled) log.info "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    if(txtEnabled) log.info "There are ${childApps.size()} child apps"
    childApps.each {child ->
    	if(txtEnabled) log.info "Child app: ${child.label}"
    }
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	installCheck()
		if(state.appInstalled == 'COMPLETE'){
			section("Instructions:", hideable: true, hidden: true) {
				paragraph "<b>Information</b>"
				paragraph "Connect two switches (real or virtual) that will always be opposite."
			}
			section(getFormat("header-green", "${getImage("Blank")}"+" Child Apps")) {
				app(name: "anyOpenApp", appName: "Inverse Link Two Switches Child", namespace: "jshimota", title: "<b>Add a new 'Inverse Link Two Switches' child</b>", multiple: true)
			}
			section(getFormat("header-green", "${getImage("Blank")}"+" General")) {
       			label title: "Enter a name for parent app (optional)", required: false
            }
            section(getFormat("header-green", "${getImage("Blank")}"+" Logging")) {
                input "txtEnabled", "bool", title: "Enable Text Logging Option", description: "Debug Log Options", defaultValue:true, submitOnChange:true
                input "dbgEnabled", "bool", title: "Enable Debug Options", description: "Debug Log Options", defaultValue:false, submitOnChange:true
                if(dbgEnabled) {
                       input "logOffTime", "enum", title: "Logs Off Time", required:false, multiple:false, options: ["1 Hour", "2 Hours", "3 Hours", "4 Hours", "5 Hours", "Keep On"]
                }
                paragraph "This app can be enabled/disabled by using a switch. The switch can also be used to enable/disable several apps at the same time."
                input "disableSwitch", "capability.switch", title: "Switch Device(s) to Enable / Disable this app <small>(When selected switch is ON, app is disabled.)</small>", submitOnChange:true, required:false, multiple:true 			}
		}
	}
}

def installCheck(){
    display()
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
		section{paragraph "Please hit 'Done' to install '${app.label}' parent app "}
  	}
  	else{
    	if(dbgEnabled) log.debug "Parent Installed OK"
  	}
}