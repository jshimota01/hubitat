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
 * 2026-05-01   jshimota    0.1.2   modded to incorporate AI suggestions
 * 2026-05-21   jshimota    0.1.3   more ai cleans
 *
 */
 
static String version() { return '0.1.3' }

metadata {
    definition (name: "avgTHIE virtual temperature device", namespace: "jshimota", author: "James Shimota") {
        capability "Temperature Measurement"
        capability "Sensor" 
        
        // Fixed: Changed from "number" to "decimal" to allow precise temperature entry in UI/Rules
        command "setTemperature", [[name:"temperature*", type: "DECIMAL", description: "Temperature value"]]
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "tempOffset", type: "decimal", title: "Temperature Offset", defaultValue: 0.0, description: "Adjust reading by this many degrees"
    }    
}

def logsOff(){
    log.warn "${device.displayName} debug logging disabled..."
    // Fixed: Passing boolean false instead of a string "false"
    device.updateSetting("logEnable",[value:false, type:"bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setTemperature(70.0)
    runIn(1800, logsOff)
}

def updated() {
    log.info "${device.displayName} updated..."
    if (logEnable) log.info "${device.displayName} debug logging is enabled."
    if (txtEnable) log.info "${device.displayName} description logging is enabled."
    
    unschedule(logsOff) // Fixed: Clear previous timers to avoid event stacking
    if (logEnable) runIn(1800, logsOff)
}

def setTemperature(temp) {
    if (temp == null) return
    
    // Fixed: Safe casting to float to protect against unexpected type objects
    float t = temp as Float
    
    // Fixed: Safe null-check logic for tempOffset to avoid NullPointerExceptions
    float offset = (tempOffset != null) ? (tempOffset as Float) : 0.0
    t += offset
    
    // Ensure we respect the hub's scale (F or C)
    String unit = "°${location.temperatureScale}"
    
    // Round to 1 decimal place for a cleaner dashboard look
    t = t.round(1)

    String descriptionText = "${device.displayName} temperature is ${t}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    
    // isStateChange: true ensures rules fire even if the temp stays the same
    sendEvent(name: "temperature", value: t, unit: unit, descriptionText: descriptionText, isStateChange: true)
}