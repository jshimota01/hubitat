/*
 * Virtual Inverse-able Switch w-autoOff and Toggle
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
 *  Change History:
 *
 *      Date          Source        Version     What
 *      ----          ------        -------     ----
 *      2021-10-10    jshimota      0.1.0       Starting version
 *      2021-10-11    jshimota      0.1.1       Added redundant set using parent; initialize always starts in off position
 *      2021-10-17    jshimota      0.1.2       Added Toggle feature
 *      2021-10-20    jshimota      0.2.0       Added AutoOffOn - toggles based on device preference
 *      2021-12-24    jshimota      0.2.1       Clean up of name and manifest package
 *      2023-09-22    jshimota      0.2.2       Cleanup log/debug checks
 *      2024-01-26    jshimota      0.2.3       Optimized: fixed autotoggle condition, dead toggle branch,
 *                                              missing state.prefix, asymmetric autoOffOnFired reset,
 *                                              initialize() duplication, added logsOff scheduling
 */
 
static String version() { return '0.2.3' }
 
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
                  [0:"Disabled"],[1:"1 second"],[2:"2 seconds"],[5:"5 seconds"],
                  [10:"10 seconds"],[15:"15 seconds"],[20:"20 seconds"],[30:"30 seconds"],
                  [45:"45 seconds"],[60:"1 minute"],[120:"2 minutes"],[300:"5 minutes"],
                  [600:"10 minutes"],[900:"15 minutes"],[1200:"20 minutes"],[1800:"30 minutes"],
                  [2700:"45 minutes"],[3200:"1 hour"]
              ], defaultValue: 0
    }
}
 
void parse(String description) { /* not used for virtual device */ }
 
void logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}
 
void on() {
    sendEvent(name: "switch", value: "on", isStateChange: true)
    if (reversed) {
        state.device = false
        parent?.componentOff(this.device)
        if (txtEnable) log.info "${device.displayName} turned ON (reversed) → physical device OFF"
    } else {
        state.device = true
        state.autoOffOnFired = false
        parent?.componentOn(this.device)
        if (txtEnable) log.info "${device.displayName} turned ON → physical device ON"
    }
    autotoggle()
}
 
void off() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
    if (reversed) {
        state.device = true
        state.autoOffOnFired = false
        parent?.componentOn(this.device)
        if (txtEnable) log.info "${device.displayName} turned OFF (reversed) → physical device ON"
    } else {
        state.device = false
        state.autoOffOnFired = false   // FIX: was missing; prevented re-triggering after manual off
        parent?.componentOff(this.device)
        if (txtEnable) log.info "${device.displayName} turned OFF → physical device OFF"
    }
    autotoggle()
}
 
/**
 * Schedule an auto-toggle if configured and not already pending.
 * FIX: original condition `!state.autoOffOnFired == true` evaluated as
 *      `(!state.autoOffOnFired) == true` which happened to work but was
 *      fragile. Now uses explicit `state.autoOffOnFired != true`.
 * FIX: log referenced bare `$autoOffOnFired` (would print null) instead
 *      of `$state.autoOffOnFired`.
 */
private void autotoggle() {
    if (dbgEnable) log.debug "${device.displayName} autotoggle: autoOffOnFired=${state.autoOffOnFired}"
    if (state.autoOffOnFired != true) {
        final int delay = autoOffOn.toInteger()
        if (delay > 0) {
            if (dbgEnable) log.debug "${device.displayName} will auto-toggle in ${delay}s"
            runIn(delay, 'toggle')
            state.autoOffOnFired = true
            if (dbgEnable) log.debug "${device.displayName} autotoggle: autoOffOnFired set to ${state.autoOffOnFired}"
        }
    }
}
 
/**
 * Toggle the switch.
 * FIX: original had an unreachable `else { if (!reversed) { ... } }` branch.
 * Simplified: regardless of reversal, state.device==true means the physical
 * device is on, so we always toggle based on that flag.
 */
void toggle() {
    if (dbgEnable) log.debug "${device.displayName} toggle called, state.device=${state.device}"
    if (state.device == true) {
        off()
    } else {
        on()
    }
}
 
void updated() {
    log.info  "${device.displayName} updated..."
    log.warn  "${device.displayName} debug logging is: ${dbgEnable == true}"
    log.warn  "${device.displayName} description logging is: ${txtEnable == true}"
    if (dbgEnable) { runIn(1800, logsOff) }  // FIX: was missing; auto-disable debug after 30 min
    initialize()
}
 
void installed() {
    initialize()
}
 
/**
 * Initialize device state.
 * FIX: both branches were near-identical; extracted common logic.
 * Reversed starts with switch=on (physical device off); normal starts with switch=off.
 */
void initialize() {
    state.device        = false
    state.autoOffOnFired = false
 
    final String switchVal  = reversed ? "on" : "off"
    final String reversedMsg = reversed ? "REVERSED (switch=on → device off)" : "normal (switch=off → device off)"
 
    sendEvent(name: "switch", value: switchVal, isStateChange: true)
 
    if (txtEnable) {
        log.info "${device.displayName} initialized"
        log.info "Initialized: mode is ${reversedMsg}"
        log.info "Initialized: state.device=${state.device}, switch=${switchVal}, autoOffOnFired=${state.autoOffOnFired}"
        if (autoOffOn.toInteger() > 0) {
            log.info "Initialized: AutoOffOn set to toggle in ${autoOffOn}s"
        } else {
            log.info "Initialized: AutoOffOn is disabled"
        }
    }
}
 
void readCurrentIntoLog() {
    if (!txtEnable) { return }
    log.info "Read Current button pushed."
    log.info "Read Current: state.device=${state.device}, reversed=${reversed}, autoOffOnFired=${state.autoOffOnFired}"
    if (autoOffOn.toInteger() > 0) {
        log.info "Read Current: AutoOffOn will toggle in ${autoOffOn}s"
    } else {
        log.info "Read Current: AutoOffOn is disabled"
    }
    if (!dbgEnable) {
        log.info "Read Current: enable debug logging to see real-time switch value (has performance impact)."
    } else {
        pauseExecution(500)
        final String switchvalue = device.currentValue("switch")
        log.info "Read Current: switchvalue=${switchvalue}"
    }
}
