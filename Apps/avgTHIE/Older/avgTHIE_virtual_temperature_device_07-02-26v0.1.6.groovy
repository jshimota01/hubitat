/*
 * avgTHIE virtual temperature device
 *
 * Licensed Virtual the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
 * ----         ------      ------- ----                                        ---
 * 2025-02-01   jshimota    0.1.0   Starting version (modeled on Hubitat public driver)
 * 2025-02-24   jshimota    0.1.1   Worked on nameing and cleanup
 * 2026-05-01   jshimota    0.1.2   modded to incorporate AI suggestions
 * 2026-05-21   jshimota    0.1.3   more ai cleans
 * 2026-07-02   jshimota    0.1.4   Optimized typecasting and switched to BigDecimal precision
 * 2026-07-02   jshimota    0.1.5   Added sendIfChanged logic to reduce duplicate logging
 * 2026-07-02   jshimota    0.1.6   Renamed logEnable to dbgEnable for readability
 *
 */
 
static String version() { return '0.1.6' }

metadata {
    definition (name: "avgTHIE virtual temperature device", namespace: "jshimota", author: "James Shimota") {
        capability "Temperature Measurement"
        capability "Sensor" 
        
        command "setTemperature", [[name:"temperature*", type: "DECIMAL", description: "Temperature value"]]
    }
    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "tempOffset", type: "decimal", title: "Temperature Offset", defaultValue: 0.0, description: "Adjust reading by this many degrees"
    }    
}

def logsOff(){
    log.warn "${device.displayName} debug logging disabled..."
    // Fixed: Updated target setting name to match preference rewrite
    device.updateSetting("dbgEnable",[value:false, type:"bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setTemperature(70.0)
    runIn(1800, logsOff)
}

def updated() {
    log.info "${device.displayName} updated..."
    if (dbgEnable) log.info "${device.displayName} debug logging is enabled."
    if (txtEnable) log.info "${device.displayName} description logging is enabled."
    
    unschedule(logsOff) 
    if (dbgEnable) runIn(1800, logsOff)
}

def setTemperature(temp) {
    if (temp == null) return
    
    BigDecimal t = temp as BigDecimal
    BigDecimal offset = (tempOffset != null) ? (tempOffset as BigDecimal) : 0.0
    t += offset
    
    String unit = "°${location.temperatureScale}"
    t = t.round(1)

    String descriptionText = "${device.displayName} temperature is ${t}${unit}"
    
    sendIfChanged(name: "temperature", value: t, unit: unit, descriptionText: descriptionText, isStateChange: true)
}

/**
 * Custom helper to handle event execution while conditionally suppressing info logging
 * if the value matches the current device state.
 */
private void sendIfChanged(Map evt) {
    Boolean isChanged = (device.currentValue(evt.name) != evt.value)
    
    if (txtEnable && (isChanged || device.currentValue(evt.name) == null)) {
        log.info "${evt.descriptionText}"
    }
    
    sendEvent(evt)
}