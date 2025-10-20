/*
 * avgTHIE virtual temperature device
 *
 * Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
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
 * Date         Source      Version What                                        URL
 * ----         ------      ------- ----                                        ---
 * 2025-02-01   jshimota    0.1.0   Starting version (modeled on Hubitat public driver)
 * 2025-02-24   jshimota    0.1.1   Worked on nameing and cleanup
 *
 */
 
static String version() { return '0.1.1' }
 
metadata {
    definition (name: "avgTHIE virtual energy device", namespace: "jshimota", author: "James Shimota") {
        capability "EnergyMeter"
        capability "PowerMeter"
        capability "VoltageMeasurement"
        command "setEnergy", ["Number"]
        command "setPower", ["Number"]
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    log.warn "installed..."
    setPower(1)
    runIn(1800,logsOff)
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def parse(String description) {
}

def setEnergy(energy) {
    def unit = "kWh"
    def descriptionText = "${device.displayName} is ${energy}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "energy", value: energy, descriptionText: descriptionText, unit: unit)
}

// def setPower(pow) {
//    def unit = "W"
//    def descriptionText = "${device.displayName} is ${pow}${unit}"
//    if (txtEnable) log.info "${descriptionText}"
//    sendEvent(name: "power", value: pow, descriptionText: descriptionText, unit: unit)
// }

def setPower(power) {
    def unit = "W"
    def descriptionText = "${device.displayName} is ${power} power"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "power", value: power, descriptionText: descriptionText, unit: unit)
}