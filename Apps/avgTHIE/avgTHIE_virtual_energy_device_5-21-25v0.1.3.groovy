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
 * 2026-05-01   jshimota    0.1.2   Geminii Recommendations
 * 2026-05-21   jshimota    0.1.3   Gemini AI changes
 *
 */
 
static String version() { return '0.1.3' }

metadata {
    definition (name: "avgTHIE virtual energy device", namespace: "jshimota", author: "James Shimota") {
        capability "Energy Meter"
        capability "Power Meter"
        capability "Voltage Measurement"
        capability "Sensor"
        
        // Fixed: Changed "NUMBER" to "DECIMAL" to allow floating-point entries in UI/Rules
        command "setEnergy", [[name:"energy*", type: "DECIMAL", description: "Cumulative Energy in kWh"]]
        command "setPower", [[name:"power*", type: "DECIMAL", description: "Instantaneous Power in Watts"]]
        command "setVoltage", [[name:"voltage*", type: "DECIMAL", description: "Voltage in Volts"]]
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff(){
    log.warn "${device.displayName} debug logging disabled..."
    // Fixed: Passing boolean false instead of a string "false"
    device.updateSetting("logEnable",[value:false, type:"bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setEnergy(0.0)
    setPower(0.0)
    setVoltage(120.0)
    runIn(1800, logsOff)
}

def updated() {
    log.info "${device.displayName} updated..."
    unschedule(logsOff) // Good practice: clear old timers before starting a new one
    if (logEnable) runIn(1800, logsOff)
}

def setEnergy(energy) {
    if (energy == null) return
    // Safe conversion using BigDecimal/Float math safely
    float val = (energy as Float).round(3) 
    String unit = "kWh"
    String descriptionText = "${device.displayName} energy is ${val} ${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "energy", value: val, unit: unit, descriptionText: descriptionText, isStateChange: true)
}

def setPower(power) {
    if (power == null) return
    float val = (power as Float).round(2)
    String unit = "W"
    String descriptionText = "${device.displayName} power is ${val} ${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "power", value: val, unit: unit, descriptionText: descriptionText, isStateChange: true)
}

def setVoltage(volts) {
    if (volts == null) return
    float val = (volts as Float).round(1)
    String unit = "V"
    String descriptionText = "${device.displayName} voltage is ${val} ${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "voltage", value: val, unit: unit, descriptionText: descriptionText, isStateChange: true)
}