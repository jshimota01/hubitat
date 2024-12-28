/**
 *  Virtual Presence Contact and Switch
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
 *      Date          Source        Version          What                                              URL
 *      ----          ------        -------          ----                                              ---
 *      2024-12-23    jshimota      0.1.0.0          My 'fork' of my virtual presence driver
 *      2024-12-23    jshimota      0.1.0.1          Removed presence
 *      2024-12-23    jshimota      0.1.0.2          Removed autopresence and stuff
 *      2024-12-23    jshimota      0.1.0.3          added temperature and motion
 *      2024-12-23    jshimota      0.1.0.4          removed contact, fixed more motion value stuff
 **/

static String version() { return '0.1.0.4' }

metadata {
    definition (name: "Virtual Motion-Temp-Switch Sensor (Custom)", namespace: "jshimota", author: "James Shimota", importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/virtual_motion-temp-switch_custom/virtual_motion-temp-switch_custom.groovy") {
        capability "Initialize"
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
        capability "Motion Sensor"
        capability "Temperature Measurement"

        command "on"
        command "off"
        command "toggleMotion"
        command "toggleSwitch"
        command "readCurrentValuesIntoLog"
        command "setTemperature", ["Number"]
    }

    preferences {
        input name: "debugLogEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "defInitState", type: "bool", title: "Set Default Initialize On", defaultValue: false
        input name: "autoContactOffOn", type: "enum", description: "Automatically turns contact to Opened for the device after selected time.", title: "Enable Auto-Contact Open", options: [[0:"Disabled"],[1:"1 second"],[2:"2 seconds"],[3:"3 seconds"],[4:"4 seconds"],[5:"5 seconds"],[10:"10 seconds"],[15:"15 seconds"],[20:"20 seconds"],[25:"25 seconds"],[30:"30 seconds"],[45:"45 seconds"],[60:"1 minute"],[120:"2 minutes"],[300:"5 minutes"],[600:"10 minutes"],[900:"15 minutes"],[1200:"20 minutes"],[1800:"30 minutes"],[2700:"45 minutes"],[3200:"1 hour"]], defaultValue: 0
        input name: "autoSwitchOffOn", type: "enum", description: "Automatically turns switch  to On for the device after selected time.", title: "Enable Auto-Switch On", options: [[0:"Disabled"],[1:"1 second"],[2:"2 seconds"],[3:"3 seconds"],[4:"4 seconds"],[5:"5 seconds"],[10:"10 seconds"],[15:"15 seconds"],[20:"20 seconds"],[25:"25 seconds"],[30:"30 seconds"],[45:"45 seconds"],[60:"1 minute"],[120:"2 minutes"],[300:"5 minutes"],[600:"10 minutes"],[900:"15 minutes"],[1200:"20 minutes"],[1800:"30 minutes"],[2700:"45 minutes"],[3200:"1 hour"]], defaultValue: 0
    }
}

def parse(String description) {
    def pair = description.split(":")
    createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def installed() {
    log.warn "installed..."
    motionInactive()
    setTemperature(100)
    runIn(1800,logsOff)
}

def setTemperature(temp) {
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} is ${temp}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "temperature", value: temp, descriptionText: descriptionText, unit: unit)
}

def on() {
    sendEvent(name: "switch", value: "on", isStateChange: true)
    sendEvent(name: "motion", value: "active", isStateChange: true)
    state.device = true
    parent?.componentOn(this.device)
    if (txtEnable) log.info "${device.displayName} switch turned ON: - device state is $state.device."
    if (txtEnable) log.info "${device.displayName} switch turned ON."
}

def off() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
    sendEvent(name: "motion", value: "inactive", isStateChange: true)
    state.device = false
    parent?.componentOff(this.device)
    if (txtEnable) log.info "${device.displayName} switch turned OFF - device state is $state.device."
    autoToggleSwitch()
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled due to logoff being run..."
    device.updateSetting("debugLogEnable", [value: "false", type: "bool"])
}

def initialize() {
    if (!defInitState) {
        if (txtEnable) log.info "INITIALIZE button pushed."
        sendEvent(name: "motion", value: "inactive", isStateChange: true)
        sendEvent(name: "switch", value: "off", isStateChange: true)
        state.device = true
        if (txtEnable) log.info "${device.displayName} is initialized"
        if (txtEnable) log.info "Initialized: Switch is Off"
        if (txtEnable) log.info "Initialized: Motion is Not Active"
    } else {
        if (txtEnable) log.info "INITIALIZE button pushed."
        sendEvent(name: "motion", value: "active", isStateChange: true)
        sendEvent(name: "switch", value: "on", isStateChange: true)
        state.device = false
        if (txtEnable) log.info "${device.displayName} is initialized"
        if (txtEnable) log.info "Initialized: Switch is On"
        if (txtEnable) log.info "Initialized: Motion is Active"
    }
    if (autoSwitchOffOn != null && autoSwitchOffOn.toInteger() > 0) {
        if (txtEnable) log.info "Initialized: autoSwitchOffOn is set to toggle in $autoSwitchOffOn seconds"
    } else {
        if (txtEnable) log.info "Initialized: autoSwitchOffOn is disabled."
    }
}

def updated(){
    log.info "${device.displayName} updated..."
    log.warn "${device.displayName} debug logging is: ${debugLogEnable == true}"
    log.warn "${device.displayName} description logging is: ${txtEnable == true}"
    initialize()
}

def autoToggleSwitch() {
    if (autoSwitchOffOn.toInteger() > 0) {
        if (debugLogEnable) log.debug "${device.displayName} will toggle in $autoSwitchOffOn seconds."
        runIn(autoSwitchOffOn.toInteger(), toggleSwitch)
    }
}

def toggleSwitch() {
    if (device.currentState("switch")?.value != "on") {
        on()
    } else {
        off()
    }
    if (debugLogEnable) log.debug "${device.displayName} was turned back on after the set delay."
}

def toggleMotion() {
    csMotion = device.currentState("motion")?.value
    if (txtEnable) log.info "Read Current State value: device motion state is $csMotion."
    if (device.currentState("motion")?.value != "active") {
        motionActive()
    } else {
        motionInactive()
        if (txtEnable) log.info "closed run."
    }
    csMotion = device.currentState("motion")?.value
    if (txtEnable) log.info "Read Current State value: device motion state now $csMotion."
}

def motionActive() {
    def descriptionText = "${device.displayName} is active"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "motion", value: "active", descriptionText: descriptionText)
}

def motionInactive() {
    def descriptionText = "${device.displayName} is inactive"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "motion", value: "inactive", descriptionText: descriptionText)
}

def readCurrentValuesIntoLog() {
    csMotion = device.currentState("motion")?.value
    if (txtEnable) log.info "Read Current Values Into Log button pushed on device page."
    if (txtEnable) log.info "Read Current State value: device switch state is $state.device."
    if (txtEnable) log.info "Read Current State value: device motion state is $csMotion."
    if (autoSwitchOffOn.toInteger() > 0) {
        if (txtEnable) log.info "Read Current State Variable: autoSwitchOffOn set to toggle in $autoSwitchOffOn seconds."
    } else {
        if (txtEnable) log.info "Read Current State Variable: autoSwitchOffOn is disabled."
    }
    if (defInitState) log.info "Read Current Preference Variable: Default Initiaiize State On."
    if (!defInitState) log.info "Read Current Preference Variable: Default Initiaiize State Off."
    if (txtEnable) log.info "Read Current Preference Variable: Description text enabled."
    if (!txtEnable) log.info "Read Current Preference Variable: Description text disabled."
    if (debugLogEnable) log.info "Read Current Preference Variable: Debug text enabled."
    if (!debugLogEnable) log.info "Read Current Preference Variable: Debug text disabled."
    if (debugLogEnable) log.warn "Read Current Preference Variable: Debug ia enabled. It must be enabled to see realtime Switch value in log. This has performance impact."
    if (debugLogEnable) {
        pauseExecution(500)
        switchvalue = device.currentValue("switch")
        if (txtEnable) log.warn "Read Current Preference Variable: switchvalue is $switchvalue"
    }
}