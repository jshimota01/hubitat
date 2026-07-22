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
 * 2026-07-02   jshimota    0.1.7   Fixed BigDecimal issue
 * 2026-07-19	jshimota	0.1.8	Adding Refresh and fixing auto debug off
 *
 */
 
static String version() { return '0.1.8' }

metadata {
    definition (name: "avgTHIE virtual temperature device", namespace: "jshimota", author: "James Shimota") {
        capability "Temperature Measurement"
        capability "Sensor"
		capability "Refresh"
        
        command "setTemperature", [[name:"temperature*", type: "DECIMAL", description: "Temperature value"]]
		command "clearAllAttributes"
    }
    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "tempOffset", type: "decimal", title: "Temperature Offset", defaultValue: 0.0, description: "Adjust reading by this many degrees"
    }    
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..."
    device.updateSetting("dbgEnable", [value: false, type: "bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setTemperature(70.0)
    runIn(1800, "logsOff")
}

def updated() {
    log.info "${device.displayName} updated..."
    unschedule("logsOff")
    if (dbgEnable) runIn(1800, "logsOff")
}

void clearAllAttributes() {
    if (dbgEnable) log.debug "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
}

def refresh() {
    if (dbgEnable) log.debug "Refresh triggered. Attempting to poll parent app for an immediate update."
    
    // In Hubitat, child devices can access their creating application via the parent object context
    if (parent) {
        try {
            parent.requestTemperatureRefresh()
        } catch (Exception e) {
            log.error "Failed to call parent refresh hook: ${e.message}"
        }
    } else {
        log.warn "No parent application link found for this device. Refresh aborted."
    }
}

def setTemperature(temp) {
    if (temp == null) return
    
    BigDecimal t = (temp instanceof Number) ? temp.toBigDecimal() : new BigDecimal(temp.toString())
    BigDecimal offset = (settings.tempOffset != null) ? settings.tempOffset.toBigDecimal() : 0.0g
    t += offset
    
    t = t.setScale(1, java.math.RoundingMode.HALF_UP)
    String unit = "°${location.temperatureScale}"

    sendIfChanged("temperature", t, unit)
}

/**
 * Standardized internal helper matching the rest of the collection ecosystem.
 */
private void sendIfChanged(String name, Object value, String unit) {
    Object currentValue = device.currentValue(name)
    
    if (currentValue == value) {
        if (dbgEnable) log.debug "${device.displayName}: ${name} value (${value}) has not changed. Skipping log and event generation."
        return
    }

    String devName = device.displayName
    String descriptionText = "${devName} ${name} is ${value}${unit}"
    
    if (txtEnable) log.info descriptionText
    
    sendEvent(name: name, value: value, unit: unit, descriptionText: descriptionText, isStateChange: true)
}