/*
 * avgTHIE virtual humidity device
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
 * 2026-05-01   jshimota    0.1.2   Gemini recommendations
 *
 */
 
/*
 * avgTHIE virtual humidity device
 */

static String version() { return '0.1.2' }

metadata {
    definition (name: "avgTHIE virtual humidity device", namespace: "jshimota", author: "James Shimota") {
        capability "Relative Humidity Measurement"
        capability "Sensor" 
        command "setRelativeHumidity", [[name:"humidity*", type: "NUMBER", description: "Humidity percentage (0-100)"]]
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "humidOffset", type: "decimal", title: "Humidity Offset", defaultValue: 0, description: "Adjust reading by this percentage"
    }
}

def logsOff(){
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setRelativeHumidity(50)
    runIn(1800,logsOff)
}

def updated() {
    log.info "${device.displayName} updated..."
    if (logEnable) runIn(1800,logsOff)
}

def setRelativeHumidity(humid) {
    if (humid == null) return
    
    // Convert to Float for calculation
    float h = humid.toFloat()
    if (humidOffset) h += humidOffset.toFloat()
    
    // Round to 1 decimal place and clamp between 0-100
    h = h.round(1)
    if (h < 0) h = 0
    if (h > 100) h = 100

    String descriptionText = "${device.displayName} is ${h}%"
    if (txtEnable) log.info "${descriptionText}"
    
    // Hubitat standard unit for humidity is "%"
    sendEvent(name: "humidity", value: h, unit: "%", descriptionText: descriptionText, isStateChange: true)
}