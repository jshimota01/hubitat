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
 *      2024-12-23    jshimota      0.1.0.5          comment out Temperature - messing Alexa
 * 		2025-12-07	  jshimota      0.1.0.6          Made this a pure virtual motion driver for Alexa 
 **/

static String version() { return '0.1.0.6' }

metadata {
    definition (name: "Virtual Motion Sensor (Custom)", namespace: "jshimota", author: "James Shimota", importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/virtual_motion-temp-switch_custom/virtual_motion-temp-switch_custom.groovy") {
        capability "Initialize"
        capability "Actuator"
        capability "Sensor"
        capability "Motion Sensor"
        // capability "Temperature Measurement"

        command "toggleMotion"
        command "readCurrentValuesIntoLog"
    }

    preferences {
        input name: "debugLogEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "defInitState", type: "bool", title: "Set Default Initialize On", defaultValue: false
    }
}

def parse(String description) {
    def pair = description.split(":")
    createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def installed() {
    log.warn "installed..."
    motionInactive()
    runIn(1800,logsOff)
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled due to logoff being run..."
    device.updateSetting("debugLogEnable", [value: "false", type: "bool"])
}

def initialize() {
    if (!defInitState) {
        if (txtEnable) log.info "INITIALIZE button pushed."
        sendEvent(name: "motion", value: "inactive", isStateChange: true)
        state.device = true
        if (txtEnable) log.info "${device.displayName} is initialized"
        if (txtEnable) log.info "Initialized: Motion is Not Active"
    } else {
        if (txtEnable) log.info "INITIALIZE button pushed."
        sendEvent(name: "motion", value: "active", isStateChange: true)
        state.device = false
        if (txtEnable) log.info "${device.displayName} is initialized"
        if (txtEnable) log.info "Initialized: Motion is Active"
    }
}

def updated(){
    log.info "${device.displayName} updated..."
    log.warn "${device.displayName} debug logging is: ${debugLogEnable == true}"
    log.warn "${device.displayName} description logging is: ${txtEnable == true}"
    initialize()
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
    if (txtEnable) log.info "Read Current State value: device motion state is $csMotion."
    if (defInitState) log.info "Read Current Preference Variable: Default Initiaiize State On."
    if (!defInitState) log.info "Read Current Preference Variable: Default Initiaiize State Off."
    if (txtEnable) log.info "Read Current Preference Variable: Description text enabled."
    if (!txtEnable) log.info "Read Current Preference Variable: Description text disabled."
    if (debugLogEnable) log.info "Read Current Preference Variable: Debug text enabled."
    if (!debugLogEnable) log.info "Read Current Preference Variable: Debug text disabled."
    if (debugLogEnable) log.warn "Read Current Preference Variable: Debug ia enabled. It must be enabled to see realtime Switch value in log. This has performance impact."
    if (debugLogEnable) {
        pauseExecution(500)
    }
}