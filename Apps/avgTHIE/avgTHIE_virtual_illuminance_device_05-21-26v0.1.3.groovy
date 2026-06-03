/*
 * avgTHIE virtual illuminance device
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
 * 2026-05-01   jshimota    0.1.2   Gemini suggestions
 * 2026-05-21   jshimota    0.1.3   Fixed command payload typing, safe offset casting, and scheduler cleanup
 *
 */
 
/*
 * avgTHIE virtual illuminance device
 */

static String version() { return '0.1.3' }

metadata {
    definition (name: "avgTHIE virtual illuminance device", namespace: "jshimota", author: "James Shimota") {
        capability "Illuminance Measurement"
        capability "Sensor" 
        
        // Fixed: Changed from "NUMBER" to "DECIMAL" so incoming raw averaged values don't throw type errors
        command "setIlluminance", [[name:"lux*", type: "DECIMAL", description: "Illuminance value in Lux"]]
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "luxOffset", type: "decimal", title: "Lux Offset", defaultValue: 0.0, description: "Adjust reading by this amount"
    }
}

def logsOff(){
    log.warn "${device.displayName} debug logging disabled..."
    // Fixed: Explicitly passing a boolean false instead of a string "false"
    device.updateSetting("logEnable",[value:false, type:"bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setIlluminance(0.0)
    runIn(1800, logsOff)
}

def updated() {
    log.info "${device.displayName} updated..."
    
    unschedule(logsOff) // Fixed: Clear previous timers to prevent thread proliferation
    if (logEnable) runIn(1800, logsOff)
}

def setIlluminance(lux) {
    if (lux == null) return
    
    // Convert to Float safely using Groovy casting mechanics
    float l = lux as Float
    
    // Fixed: Implemented strict null handling to shield against NullPointerExceptions
    float offset = (luxOffset != null) ? (luxOffset as Float) : 0.0
    l += offset
    
    // Illuminance is typically handled as an integer state in Hubitat
    int finalLux = l.round().toInteger()
    if (finalLux < 0) finalLux = 0 // Lux cannot be physically less than total darkness

    // Standardized log framing text strings
    String descriptionText = "${device.displayName} illuminance is ${finalLux} lx"
    if (txtEnable) log.info "${descriptionText}"
    
    sendEvent(name: "illuminance", value: finalLux, unit: "lx", descriptionText: descriptionText, isStateChange: true)
}