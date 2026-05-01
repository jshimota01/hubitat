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
 *
 */
 
static String version() { return '0.1.2' }
 
metadata {
    definition (name: "avgTHIE virtual temperature device", namespace: "jshimota", author: "James Shimota") {
        capability "Temperature Measurement"
        capability "Sensor" // Add this for maximum app compatibility
        command "setTemperature", ["number"]
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "tempOffset", type: "decimal", title: "Temperature Offset", defaultValue: 0.0, description: "Adjust reading by this many degrees"
}

def logsOff(){
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setTemperature(70)
    runIn(1800,logsOff)
}

def updated() {
    log.info "${device.displayName} updated..."
    log.warn "${device.displayName} debug logging is: ${logEnable == true}"
    log.warn ":${device.displayName} description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def setTemperature(temp) {
    // Convert to float/decimal to ensure math works
    float t = temp.toFloat()
    if (tempOffset) t += tempOffset.toFloat()
    
    // Ensure we respect the hub's scale (F or C)
    String unit = "°${location.temperatureScale}"
    
    // Round to 1 decimal place for a cleaner dashboard look
    t = t.round(1)

    String descriptionText = "${device.displayName} is ${t}${unit}"
    
    if (txtEnable) log.info "${descriptionText}"
    
    // isStateChange: true ensures rules fire even if the temp stays the same
    sendEvent(name: "temperature", value: t, unit: unit, descriptionText: descriptionText, isStateChange: true)
}