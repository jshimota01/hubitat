/*
 * avgTHIE virtual power device
 *
 * Licensed Under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 * Change History:
 *
 * Date         Source      Version What                                        URL
 * ----         ------      ------- ----                    
 * 2025-02-01   jshimota    0.1.0   Starting version (modeled on Hubitat public driver)
 * 2025-02-24   jshimota    0.1.1   Worked on naming and cleanup
 * 2026-05-01   jshimota    0.1.2   Geminii Recommendations
 * 2026-05-21   jshimota    0.1.3   Gemini AI changes
 * 2026-07-02   jshimota    0.1.4   Optimized math casting, added sendIfChanged engine, and renamed to dbgEnable
 * 2026-07-19 	jshimota	0.1.5	removed Energy - just power now
 *
 */
 
static String version() { return '0.1.5' }

metadata {
    definition (name: "avgTHIE virtual power device", namespace: "jshimota", author: "James Shimota") {
        capability "Power Meter"
        capability "Voltage Measurement"
        capability "Sensor"
        
        command "setPower", [[name:"power*", type: "DECIMAL", description: "Instantaneous Power in Watts"]]
        command "setVoltage", [[name:"voltage*", type: "DECIMAL", description: "Voltage in Volts"]]
    }
    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("dbgEnable", [value: false, type: "bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setPower(0.0)
    setVoltage(120.0)
    runIn(1800, "logsOff")
}

def updated() {
    log.info "${device.displayName} updated..."
    unschedule("logsOff")
    if (dbgEnable) runIn(1800, "logsOff")
}

def setPower(power) {
    if (power == null) return
    
    float rawVal = (power instanceof Number) ? power.floatValue() : power.toFloat()
    
    // Fast inline mathematical rounding to 2 decimal places
    float val = (float)(Math.round(rawVal * 100.0f) / 100.0f)
    
    sendIfChanged("power", val, "W")
}

def setVoltage(volts) {
    if (volts == null) return
    
    float rawVal = (volts instanceof Number) ? volts.floatValue() : volts.toFloat()
    
    // Fast inline mathematical rounding to 1 decimal place
    float val = (float)(Math.round(rawVal * 10.0f) / 10.0f)
    
    sendIfChanged("voltage", val, "V")
}

/**
 * Internal helper to update state and log ONLY when the value genuinely alters.
 */
private void sendIfChanged(String name, Object value, String unit) {
    Object currentValue = device.currentValue(name)
    
    if (currentValue == value) {
        if (dbgEnable) log.debug "${device.displayName}: ${name} value (${value}) has not changed. Skipping log and event generation."
        return
    }

    String devName = device.displayName
    String descriptionText = "${devName} ${name} is ${value} ${unit}"
    
    if (txtEnable) log.info descriptionText
    
    sendEvent(name: name, value: value, unit: unit, descriptionText: descriptionText, isStateChange: true)
}