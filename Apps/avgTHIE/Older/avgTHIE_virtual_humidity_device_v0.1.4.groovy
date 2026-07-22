/*
 * avgTHIE virtual humidity device
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
 * 2026-05-01   jshimota    0.1.2   Gemini recommendations 
 * 2026-05-21   jshimota    0.1.3   Fixed command typing, safe offset casting, and timer cleanup 
 * 2026-07-02   jshimota    0.1.4   Optimized math casting, added sendIfChanged engine, and renamed to dbgEnable
 *
 */
 
/*
 * avgTHIE virtual humidity device
 */

static String version() { return '0.1.4' }

metadata {
    definition (name: "avgTHIE virtual humidity device", namespace: "jshimota", author: "James Shimota") { 
        capability "Relative Humidity Measurement" 
        capability "Sensor"  
        
        command "setRelativeHumidity", [[name:"humidity*", type: "DECIMAL", description: "Humidity percentage (0-100)"]] 
    }
    preferences {
        input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true 
        input name: "humidOffset", type: "decimal", title: "Humidity Offset", defaultValue: 0.0, description: "Adjust reading by this percentage" 
    }
}

def logsOff() {
    log.warn "${device.displayName} debug logging disabled..." 
    device.updateSetting("dbgEnable", [value: false, type: "bool"])
}

def installed() {
    log.warn "${device.displayName} installed..."
    setRelativeHumidity(50.0) 
    runIn(1800, "logsOff")
}

def updated() {
    log.info "${device.displayName} updated..." 
    unschedule("logsOff")
    if (dbgEnable) runIn(1800, "logsOff")
}

def setRelativeHumidity(humid) {
    if (humid == null) return
    
    // Low-overhead conversion without using the heavy 'as Float' lookup table coercion
    float h = (humid instanceof Number) ? humid.floatValue() : humid.toFloat()
    
    // Leverage safe navigation operator and direct fallback assignment via settings map
    float offset = (settings.humidOffset != null) ? settings.humidOffset.floatValue() : 0.0f
    h += offset
    
    // Fast inline mathematical rounding to 1 decimal place
    float val = (float)(Math.round(h * 10.0f) / 10.0f)
    
    // Primitive clamping between 0.0f and 100.0f
    if (val < 0.0f) {
        val = 0.0f
    } else if (val > 100.0f) {
        val = 100.0f
    }

    // Handle deduplicated event routing
    sendIfChanged("humidity", val, "%")
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
    String descriptionText = "${devName} ${name} is ${value}${unit}"
    
    if (txtEnable) log.info descriptionText
    
    sendEvent(name: name, value: value, unit: unit, descriptionText: descriptionText, isStateChange: true)
}