/*
 * avgTHIE virtual illuminance device
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
 * 2026-05-01   jshimota    0.1.2   Gemini suggestions
 * 2026-05-21   jshimota    0.1.3   Fixed command payload typing, safe offset casting, and scheduler cleanup
 * 2026-07-02   jshimota    0.1.4   Optimized typecasting, string allocations, and property access
 *
 */
 
/*
 * avgTHIE virtual illuminance device
 */

static String version() { return '0.1.4' }

metadata {
    definition (name: "avgTHIE virtual illuminance device", namespace: "jshimota", author: "James Shimota") {
        capability "Illuminance Measurement"
        capability "Sensor" 
        
        command "setIlluminance", [[name:"lux*", type: "DECIMAL", description: "Illuminance value in Lux"]]
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "luxOffset", type: "decimal", title: "Lux Offset", defaultValue: 0.0, description: "Adjust reading by this amount"
    }
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("logEnable", [value: false, type: "bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setIlluminance(0.0)
    runIn(1800, "logsOff") // Explicit string handler usage
}

def updated() {
    log.info "${device.displayName} updated..."
    unschedule("logsOff") // Explicit string handler usage
    if (logEnable) runIn(1800, "logsOff")
}

def setIlluminance(lux) {
    if (lux == null) return
    
    // Use low-overhead primitive conversions instead of 'as Float' object wrappers
    float l = (lux instanceof Number) ? lux.floatValue() : lux.toFloat()
    
    // Leverage safe navigation operator ?. and direct fallback assignment
    float offset = (settings.luxOffset != null) ? settings.luxOffset.floatValue() : 0.0f
    l += offset
    
    // Direct primitive math casting is faster than calling .round().toInteger() object methods
    int finalLux = (l < 0.0f) ? 0 : (int)(l + 0.5f)

    // Construct string once dynamically inside sendEvent to minimize memory allocations if txtEnable is false
    String devName = device.displayName
    String descriptionText = "${devName} illuminance is ${finalLux} lx"
    
    if (txtEnable) log.info descriptionText
    
    sendEvent(name: "illuminance", value: finalLux, unit: "lx", descriptionText: descriptionText, isStateChange: true)
}