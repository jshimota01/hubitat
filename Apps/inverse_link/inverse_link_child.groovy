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
 *  Inverse Link Two Switches Child
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
 *      2023-10-08    bturcott      0.1.0.5          Used Brian for model on parent child
 *        
 */

definition(
   
	name: "Inverse Link Two Switches Child",
	namespace: "jshimota",
	author: "James Shimota",
	description: "Based on the Link Two Switches by Larry Kahn, if one switch goes on, the other goes off and vice versa  ",
	category: "Convenience",
  	parent: "jshimota:Inverse Link Two Switches",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/inverse_link/inverse_link_child.groovy",
)

def setVersion(){
    state.name = "Inverse Link Two Switches Child"
	state.version = "0.1.0.5"
}


preferences {
	section("Master Switch?") {
		input "master", "capability.switch", title: "Which switch to be the master?"
	}
    section("Linked Switch?") {
		input "linked", "capability.switch", title: "Which switch to be linked?"
	}
    section("Logging") {
    	input "txtEnabled", "bool", title: "Turn on Text/Description Logging", defaultvalue: "true"
        input "debugEnabled", "bool", title: "Turn on Debug Logging", defaultvalue: "false"

    }
}

def installed()
{   
	subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
 
    subscribe(linked, "switch.on", linkedOnHandler)
	subscribe(linked, "switch.off", linkedOffHandler)
   
}

def updated()
{
	unsubscribe()
	subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
	
    subscribe(linked, "switch.on", linkedOnHandler)
	subscribe(linked, "switch.off", linkedOffHandler)
	
	if(dbgEnabled) log.debug "in updated ... master state= $master.currentSwitch"
	if(dbgEnabled) log.debug "in updated ... linked state= $linked.currentSwitch"
}

def logHandler(evt) {
	if(txtEnabled) log.info evt.value
}

def onHandler(evt) {
	if(dbgEnabled) log.debug "In Master on handler"
	if(dbgEnabled) log.debug evt.value	
    if (linked.currentSwitch == "on")
     {	 if(txtEnabled) log.info "Master - $master.name turned On. Linked - $linked.name was on, turning it off"
   		 linked.off()
     }
}

def offHandler(evt) {
	if(dbgEnabled) log.debug evt.value
    if(dbgEnabled) log.debug "In Master off handler"
	  if (linked.currentSwitch == "off")
     {	 if(txtEnabled) log.info "Master - $master.name turned Off. Linked - $linked.name was off, turning it on"
   		 linked.on()
     }
}

def linkedOnHandler(evt) {
	if(dbgEnabled) log.debug evt.value
	if(dbgEnabled) log.debug "In Linked on handler"
     if (master.currentSwitch == "on")
     {	 if(txtEnabled) log.info "Linked - $linked.name turned On. Master - $master.name was on, turning it off"
   		 master.off()
     }
}

def linkedOffHandler(evt) {
	if(dbgEnabled) log.debug evt.value
    if(dbgEnabled) log.debug "In Linked off handler"
   if (master.currentSwitch == "off")
     {	 if(txtEnabled) log.info "Linked - $linked.name turned Off. Master - $master.name was off, turning it on"
   		master.on()
     }
}