/**
 *
 *  AvgTHIE Child 
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  05-01-26    jshimota    v1.1.1 		From original jshimota work - start point
 *	05-01-26 	jshimota	v1.1.2  	Implement gemini Recommendations  
 *  05-21-26    jshimota    v1.1.3		Fixed nested UI section bugs, scheduled string context, and energy precision
 *  06-10-26    jshimota    v1.1.4		Added power sensor support
 *
 */
/**
 *  AvgTHIE Child (v1.1.4)
 */
public static String version()   {  return "v1.1.4"  }

definition (
    name: "AvgTHIEChild",
    namespace: "jshimota",
    author: "J Shimota",
    description: "Child: Average a set of humidity, temperature, illuminance energy or power sensors to an individual specialized virtual device.",
    parent: "jshimota:AvgTHIE",
    category: "Averaging",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
    page(name: "mainPage")
}

def installed() {
    log.info "Installed: ${app.label}"
    initialize()
}

def updated() {
    log.info "Updated: ${app.label}"
    unsubscribe()
    unschedule() // Good practice: clear old background tasks before initializing new ones
    // Fixed: Standardized execution callback to pass as a explicit String literal token
    if (debugOutput) runIn(1800, "logsOff")
    initialize()
}

def initialize() {
    // Set default states if empty
    if (state.avgT == null) state.avgT = location.temperatureScale == "F" ? 68.0 : 20.0
    if (state.avgH == null) state.avgH = 50.0
    if (state.avgL == null) state.avgL = 100.0
    if (state.avgE == null) state.avgE = 0.0
    if (state.avgP == null) state.avgP = 0.0


    // Consolidated subscriptions
    if (tempSensors)  subscribe(tempSensors, "temperature", tempHandler)
    if (humidSensors) subscribe(humidSensors, "humidity", humidHandler)
    if (illumSensors) subscribe(illumSensors, "illuminance", illumHandler)
    if (energySensors) subscribe(energySensors, "energy", energyHandler)
    if (powerSensors) subscribe(powerSensors, "power", powerHandler)
}

// --- HANDLERS ---

def tempHandler(evt) {
    processAvg(evt, "avgT", vTDevice, "setTemperature", "temperature")
}

def humidHandler(evt) {
    processAvg(evt, "avgH", vHDevice, "setRelativeHumidity", "humidity")
}

def illumHandler(evt) {
    processAvg(evt, "avgL", vIDevice, "setIlluminance", "illuminance")
}

def energyHandler(evt) {
    processAvg(evt, "avgE", vEDevice, "setEnergy", "energy")
}

def powerHandler(evt) {
    processAvg(evt, "avgP", vPDevice, "setPower", "power")
}
/**
 * Generic Processor to handle the rolling average logic
 */
def processAvg(evt, stateVar, vDevice, commandName, attrName) {
    if (!vDevice) return
    
    // Ensure we have a valid float; handle potential nulls safely
    float newVal = evt.value ? evt.value.toFloat() : 0.0
    int NSample = numberOption ? numberOption.toInteger() : 10
    
    // Retrieve current average from state, default to newVal if state hasn't initialized yet
    float currentAvg = (state."${stateVar}" != null) ? state."${stateVar}".toFloat() : newVal
    
    // Rolling Average (EMA) Math
    currentAvg -= (currentAvg / NSample)
    currentAvg += (newVal / NSample)
    state."${stateVar}" = currentAvg

    if (debugOutput) log.debug "${attrName} update from ${evt.device}: New=${newVal}, NewAvg=${currentAvg.round(3)}"

    // standard Hubitat way to check for command existence
    if (vDevice.supportedCommands.any { it.name == commandName }) {
        // Fixed: Energy and power metric values require high-precision point depth (3 decimals vs 1)
		int precision = (attrName == "energy" || attrName == "power") ? 3 : 1 // Also apply high precision to power if needed
        vDevice."${commandName}"(currentAvg.round(precision))
    } else {
        log.warn "The virtual device ${vDevice.displayName} is missing the required command: ${commandName}"
    }
}

// --- UI PAGES ---

def mainPage() {
    if (!app.label) app.updateLabel(app.name)
    
    dynamicPage(name: "mainPage", uninstall: true, install: true) {
        // Fixed: Flattened the section elements layout structure completely out to prevent structural nesting runtime crashes
        section(getFormat("title", " ${app.label}")) {
            paragraph "<div style='color:#1A77C9'>Calculate a rolling average for your specialized virtual devices.</div>"
        }
        section("Temperature Config") {
            input "vTDevice", "capability.temperatureMeasurement", title: "Virtual Temperature Device", required: false
            input "tempSensors", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: false
        }
        section("Humidity Config") {
            input "vHDevice", "capability.relativeHumidityMeasurement", title: "Virtual Humidity Device", required: false
            input "humidSensors", "capability.relativeHumidityMeasurement", title: "Humidity Sensors", multiple: true, required: false
        }
        section("Illuminance Config") {
            input "vIDevice", "capability.illuminanceMeasurement", title: "Virtual Illuminance Device", required: false
            input "illumSensors", "capability.illuminanceMeasurement", title: "Illuminance Sensors", multiple: true, required: false
        }
        section("Energy Config") {
            input "vEDevice", "capability.energyMeter", title: "Virtual Energy Device", required: false
            input "energySensors", "capability.energyMeter", title: "Energy Sensors", multiple: true, required: false
        }
        section("Power Config") {
            input "vPDevice", "capability.powerMeter", title: "Virtual Power Device", required: false
            input "powerSensors", "capability.powerMeter", title: "Power Sensors", multiple: true, required: false
        }
        section("Filtering Settings") {
            input (name: "numberOption", type: "number", defaultValue: 10, range: "2..200", title: "Quantity of samples", description: "Lower is more responsive, higher is smoother.")
        }
        section("Logging Settings") {
            label title: "Child App Name", required: false
            input "debugOutput", "bool", title: "Enable Debug Logging", defaultValue: true
        }
        section {
            display()
        }
    }
}

def display() {
    // Note: Wrapping section shifted out to comply cleanly with dynamicPage parsing criteria
    paragraph getFormat("line")
    paragraph "<div style='color:#1A77C9;text-align:center;font-size:10px'>Modded by J Shimota<br>Version: ${version()} | ${getThisCopyright()}</div>"
}

def getFormat(type, myText=""){
    // Fixed: Reconstructed malformed explicit HTML closure block to use clean self-closing syntax
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;' />"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def getThisCopyright() { return "&copy; 2026 J Shimota" }

def logsOff() { 
    log.warn "${app.label} execution debug logging auto-disabled..."
    // Fixed: Pass raw boolean primitive value safely
    app.updateSetting("debugOutput",[value:false, type:"bool"]) 
}