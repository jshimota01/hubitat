/*
 * Virtual Inverse-able Switch w-autoOff and Toggle
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Change History:
 *
 * Date          Source        Version     What
 * ----          ------        -------     ----
 * 2021-10-10    jshimota      0.1.0       Starting version
 * 2021-10-11    jshimota      0.1.1       Added redundant set using parent; initialize always starts in off position
 * 2021-10-17    jshimota      0.1.2       Added Toggle feature
 * 2021-10-20    jshimota      0.2.0       Added AutoOffOn - toggles based on device preference
 * 2021-12-24    jshimota      0.2.1       Clean up of name and manifest package
 * 2023-09-22    jshimota      0.2.2       Cleanup log/debug checks
 * 2024-01-26    jshimota      0.2.3       Optimized auto-toggle branches and tracking states
 * 2026-06-22    jshimota      0.2.4       Gemini check - Fixed reversed autoOffOnFired bug, preference type null pointer crash protection, and optimized out state.device
 * 2026-06-22    jshimota      0.2.5       Gemini error check Fixed parent signature to 'this', removed isStateChange loop vectors, fixed 1 hour typo
 */
 
static String version() { return '0.2.5' }
 
metadata {
    definition(
            name: "Virtual Inverse-able Switch w-autoOff and Toggle",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/virtual_inverse-able_switch/virtualInverse-ableSwitch.groovy"
    ) {
        capability "Actuator"
        capability "Switch"
        capability "Initialize"
        command "toggle"
        command "readCurrentIntoLog"
    }
 
    preferences {
        input name: "dbgEnable",  type: "bool",   title: "Enable debug logging",        defaultValue: false
        input name: "txtEnable",  type: "bool",   title: "Enable descriptionText logging", defaultValue: true
        input name: "reversed",   type: "bool",   title: "Reverse Action",              defaultValue: false, required: true
        input name: "autoOffOn",  type: "enum",   title: "Enable Auto-Off-On",
              description: "Automatically turns off -or- on the device after selected time.",
              options: [
                  ["0":"Disabled"],["1":"1 second"],["2":"2 seconds"],["5":"5 seconds"],
                  ["10":"10 seconds"],["15":"15 seconds"],["20":"20 seconds"],["30":"30 seconds"],
                  ["45":"45 seconds"],["60":"1 minute"],["120":"2 minutes"],["300":"5 minutes"],
                  ["600":"10 minutes"],["900":"15 minutes"],["1200":"20 minutes"],["1800":"30 minutes"],
                  ["2700":"45 minutes"],["3600":"1 hour"] // Fixed: Typo fixed from 3200 to 3600
              ], defaultValue: "0"
    }
}
 
void parse(String description) { /* not used for virtual device */ }
 
void logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}
 
void on() {
    unschedule('toggle')
    sendEvent(name: "switch", value: "on") // Fixed: Removed loop-vulnerable isStateChange: true
    state.autoOffOnFired = false
    
    if (reversed) {
        parent?.componentOff(this) // Fixed: Changed 'device' to 'this' for correct parent handling
        if (txtEnable) log.info "${device.displayName} turned ON (reversed) → physical device OFF"
    } else {
        parent?.componentOn(this)  // Fixed: Changed 'device' to 'this' for correct parent handling
        if (txtEnable) log.info "${device.displayName} turned ON → physical device ON"
    }
    autotoggle()
}
 
void off() {
    unschedule('toggle')
    sendEvent(name: "switch", value: "off") // Fixed: Removed loop-vulnerable isStateChange: true
    state.autoOffOnFired = false
    
    if (reversed) {
        parent?.componentOn(this)  // Fixed: Changed 'device' to 'this' for correct parent handling
        if (txtEnable) log.info "${device.displayName} turned OFF (reversed) → physical device ON"
    } else {
        parent?.componentOff(this) // Fixed: Changed 'device' to 'this' for correct parent handling
        if (txtEnable) log.info "${device.displayName} turned OFF → physical device OFF"
    }
    autotoggle()
}
 
private void autotoggle() {
    if (dbgEnable) log.debug "${device.displayName} autotoggle: autoOffOnFired=${state.autoOffOnFired}"
    if (state.autoOffOnFired != true) {
        final int delay = (autoOffOn ?: 0).toInteger()
        if (delay > 0) {
            if (dbgEnable) log.debug "${device.displayName} will auto-toggle in ${delay}s"
            runIn(delay, 'toggle')
            state.autoOffOnFired = true
        }
    }
}
 
void toggle() {
    final String currentSwitch = device.currentValue("switch")
    if (dbgEnable) log.debug "${device.displayName} toggle called, current switch position=${currentSwitch}"
    if (currentSwitch == "on") {
        off()
    } else {
        on()
    }
}
 
void updated() {
    log.info  "${device.displayName} updated..."
    if (dbgEnable) { 
        log.warn "${device.displayName} debug logging is enabled for 30 minutes."
        runIn(1800, logsOff) 
    }
    initialize()
}
 
void installed() {
    initialize()
}
 
void initialize() {
    unschedule()
    state.autoOffOnFired = false
 
    final String switchVal  = reversed ? "on" : "off"
    final String reversedMsg = reversed ? "REVERSED (switch=on → device off)" : "normal (switch=off → device off)"
 
    sendEvent(name: "switch", value: switchVal)
 
    if (txtEnable) {
        log.info "${device.displayName} initialized: mode is ${reversedMsg}"
        final int delay = (autoOffOn ?: 0).toInteger()
        if (delay > 0) {
            log.info "Initialized: AutoOffOn set to toggle in ${delay}s"
        } else {
            log.info "Initialized: AutoOffOn is disabled"
        }
    }
}
 
void readCurrentIntoLog() {
    if (!txtEnable) { return }
    final String switchvalue = device.currentValue("switch")
    final int delay = (autoOffOn ?: 0).toInteger()
    
    log.info "Read Current: switchvalue=${switchvalue}, reversed=${reversed}, autoOffOnFired=${state.autoOffOnFired}"
    if (delay > 0) {
        log.info "Read Current: AutoOffOn will toggle in ${delay}s"
    } else {
        log.info "Read Current: AutoOffOn is disabled"
    }
}