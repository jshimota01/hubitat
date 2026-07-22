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
 *	05-01-26 	jshimota	Implement gemini Recommendations  
 *
 */
/**
 *  AvgTHIE Child (v1.1.2)
 */
public static String version()   {  return "v1.1.2"  }

definition (
    name: "AvgTHIEChild",
    namespace: "jshimota",
    author: "J Shimota",
    description: "Child: Average a set of humidity, temperature, illuminance or energy sensors to an individual specialized virtual device.",
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
    if (debugOutput) runIn(1800, logsOff)
    initialize()
}

def initialize() {
    // Set default states if empty
    if (state.avgT == null) state.avgT = location.temperatureScale == "F" ? 68.0 : 20.0
    if (state.avgH == null) state.avgH = 50.0
    if (state.avgL == null) state.avgL = 100.0
    if (state.avgE == null) state.avgE = 0.0

    // Consolidated subscriptions
    if (tempSensors)  subscribe(tempSensors, "temperature", tempHandler)
    if (humidSensors) subscribe(humidSensors, "humidity", humidHandler)
    if (illumSensors) subscribe(illumSensors, "illuminance", illumHandler)
    if (energySensors) subscribe(energySensors, "energy", energyHandler)
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

/**
 * Generic Processor to handle the rolling average logic
 * v1.1.2 - Fixed allowsCommand error by using supportedCommands check
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

    if (debugOutput) log.debug "${attrName} update from ${evt.device}: New=${newVal}, NewAvg=${currentAvg.round(2)}"

    // standard Hubitat way to check for command existence
    if (vDevice.supportedCommands.any { it.name == commandName }) {
        vDevice."${commandName}"(currentAvg.round(1))
    } else {
        log.warn "The virtual device ${vDevice.displayName} is missing the required command: ${commandName}"
    }
}

// --- UI PAGES ---

def mainPage() {
    if (!app.label) app.updateLabel(app.name)
    
    dynamicPage(name: "mainPage", uninstall: true, install: true) {
        section(getFormat("title", " ${app.label}")) {
            paragraph "<div style='color:#1A77C9'>Calculate a rolling average for your specialized virtual devices.</div>"
            
            section("Temperature") {
                input "vTDevice", "capability.temperatureMeasurement", title: "Virtual Temperature Device", required: false
                input "tempSensors", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: false
            }
            section("Humidity") {
                input "vHDevice", "capability.relativeHumidityMeasurement", title: "Virtual Humidity Device", required: false
                input "humidSensors", "capability.relativeHumidityMeasurement", title: "Humidity Sensors", multiple: true, required: false
            }
            section("Illuminance") {
                input "vIDevice", "capability.illuminanceMeasurement", title: "Virtual Illuminance Device", required: false
                input "illumSensors", "capability.illuminanceMeasurement", title: "Illuminance Sensors", multiple: true, required: false
            }
            section("Energy") {
                input "vEDevice", "capability.energyMeter", title: "Virtual Energy Device", required: false
                input "energySensors", "capability.energyMeter", title: "Energy Sensors", multiple: true, required: false
            }
            section("Filtering") {
                input (name: "numberOption", type: "number", defaultValue: 10, range: "2..200", title: "Quantity of samples", description: "Lower is more responsive, higher is smoother.")
            }
            section("Settings") {
                label title: "Child App Name", required: false
                input "debugOutput", "bool", title: "Enable Debug Logging", defaultValue: true
            }
            display()
        }
    }
}

def display() {
    section {
       paragraph getFormat("line")
       paragraph "<div style='color:#1A77C9;text-align:center;font-size:10px'>Modded by J Shimota<br>Version: ${version()} | ${getThisCopyright()}</div>"
    }
}

def getFormat(type, myText=""){
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def getThisCopyright() { return "&copy; 2026 J Shimota" }
def logsOff() { app.updateSetting("debugOutput",[value:"false",type:"bool"]) }