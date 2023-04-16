/**
 *  Virtual Presence and Switch
 *
 *  Copyright 2019 Joel Wetzel
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
 */

metadata {
        definition (name: "Virtual Presence and Switch", namespace: "jshimota", author: "Jim Shimota") {
        capability "Initialize"
     	capability "Actuator"
        capability "Switch"
        capability "PresenceSensor"
		capability "Sensor"
        
		command "arrived"
		command "departed"
        command "on"
        command "off"
        command "toggle"
        command "readCurrentIntoLog"
    }
    
        preferences {
        input name: "debugLogEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "autoPresenceOffOn", type: "enum", description: "Automatically turns presence to On for the device after selected time.", title: "Enable Auto-Presence On", options: [[0:"Disabled"],[1:"1 second"],[2:"2 seconds"],[3:"3 seconds"],[4:"4 seconds"],[5:"5 seconds"],[10:"10 seconds"],[15:"15 seconds"],[20:"20 seconds"],[25:"25 seconds"],[30:"30 seconds"],[45:"45 seconds"],[60:"1 minute"],[120:"2 minutes"],[300:"5 minutes"],[600:"10 minutes"],[900:"15 minutes"],[1200:"20 minutes"],[1800:"30 minutes"],[2700:"45 minutes"],[3200:"1 hour"]], defaultValue: 0
    }
    
}

def parse(String description) {
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def arrived() {
    sendEvent(name: "presence", value: "present")
    if (txtEnable) log.info "${device.displayName} presence set to present."
}

def departed() {
    sendEvent(name: "presence", value: "not present")
    if (txtEnable) log.info "${device.displayName} presence set to not present."
    autotoggle()
}

def on() {
    sendEvent(name: "switch", value: "on", isStateChange: true)
        state.device = true
        parent?.componentOn(this.device)
        if (txtEnable) log.info "${device.displayName} Switched On: - device state is $state.device."
        if (autoPresenceOffOn.toInteger() > 0) {
        if (txtEnable) log.info "Switched On: presence will toggle in $autoPresenceOffOn seconds"
          departed()
    } else {
        if (txtEnable) log.info "Switched On: presence toggle is disabled"
    }
    
    
    
    if (txtEnable) log.info "${device.displayName} switch turned ON - runnng departed function."
    
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
        state.device = false
        parent?.componentOff(this.device)
        if (txtEnable) log.info "${device.displayName} switch turned OFF - device state is $state.device."
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled due to logoff being run..."
    device.updateSetting("debugLogEnable", [value: "false", type: "bool"])
}

def initialize() {
     if (txtEnable) log.info "INITIALIZE button pushed."
     sendEvent(name: "switch", value: "on", isStateChange: true)
     sendEvent(name: "presence", value: "present", isStateChange: true)
     state.device = false
     
     if (txtEnable) log.info "${device.displayName} is initialized"
     if (txtEnable) log.info "Initialized: Switch is On"
     if (txtEnable) log.info "Initialized: Presence is Present"
     if (autoPresenceOffOn.toInteger() > 0) {
         if (txtEnable) log.info "Initialized: autoPresenceOffOn is set to toggle in $autoPresenceOffOn seconds"
     } else {
         if (txtEnable) log.info "Initialized: autoPresenceOffOn is disabled"
     }
}

def updated(){
    log.info "${device.displayName} updated..."
    log.warn "${device.displayName} debug logging is: ${debugLogEnable == true}"
    log.warn "${device.displayName} description logging is: ${txtEnable == true}"
    initialize()
}

def autotoggle() {
        if (autoPresenceOffOn.toInteger() > 0) {
            if (debugLogEnable) log.debug "${device.displayName} will toggle in $autoPresenceOffOn seconds."
            runIn(autoPresenceOffOn.toInteger(), toggle)
    }
}

def toggle() {
    if (device.currentState("presence")?.value != "present") { 
       arrived()
    } else {
       departed()
    }
   if (debugLogEnable) log.debug "${device.displayName} was turned back on after the set delay."
}

def readCurrentIntoLog() {
    csPresence = device.currentState("presence")?.value
    if (txtEnable) log.info "Read Current Into Log button pushed on device page."
    if (txtEnable) log.info "Read Current: device switch state is $state.device."
    if (txtEnable) log.info "Read Current: device presence state is $csPresence."    
    if (autoPresenceOffOn.toInteger() > 0) {
        if (txtEnable) log.info "Read Current: autoPresenceOffOn will toggle in $autoPresenceOffOn seconds"
    } else {
        if (txtEnable) log.info "Read Current: autoPresenceOffOn is disabled"
    }
    if (debugLogEnable) log.warn "Read Current: Debug ia enabled. It must be enabled to see realtime switchvalue in Log. This has performance impact."
    if (debugLogEnable) {
        pauseExecution(500)
        switchvalue = device.currentValue("switch")
        if (txtEnable) log.warn "Read Current: switchvalue is $switchvalue"
    }
}