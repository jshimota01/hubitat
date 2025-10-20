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
 */
 public static String version()   {  return "v1.0.0"  }

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

// Preference pages
preferences
{
	page(name: "mainPage")
}

def subscribeTSelected() {
	if (tempSensors?.size()) 
	{
		if (debugOutput) log.debug "subscribeTSelected: $tempSensors"
		subscribe(tempSensors, "temperature", tempHandler)
	}
}

def subscribeHSelected() {
	if (humidSensors?.size()) 
	{
		if (debugOutput) log.debug "subscribeHSelected: $humidSensors"
        subscribe(humidSensors, "humidity", humidHandler)
	}
}

def subscribeISelected() {
	if (illumSensors?.size()) 
	{
		if (debugOutput) log.debug "subscribeISelected: $illumSensors"
	    subscribe(illumSensors, "illuminance", illumHandler)
	}
}

def subscripeESelected() {
	if (energySensors?.size()) 
	{
		if (debugOutput) log.debug "subscripeESelected: $energySensors"
	    subscribe(energySensors, "energy", energyHandler)
	}
}

def tempHandler(evt) {
	def NSample = numberOption as Integer
	if (debugOutput) log.debug "tempHandler: $evt.device, $evt.value, ($state.avgT)"
	def Float avT = state.avgT
	avT -= avT / NSample
	avT += Float.parseFloat(evt.value) / NSample
	state.avgT = avT

  	if (debugOutput) log.debug "   Supported Commands of temperature devices $vTDevice:${vTDevice.supportedCommands}"
	if(vTDevice.supportedCommands.find{it.toString() == "setTemperature"}) { settings.vTDevice.setTemperature("${state.avgT.round(1)}"); sendEvent(name: "temperature", value: state.avgT, unit:"°${location.temperatureScale}", displayed: true)  }
	else { log.warn "Is Incorrect vTDevice - no Temperature" }
}

def humidHandler(evt) {
	def NSample = numberOption as Integer
	if (debugOutput) log.debug "humidHandler: $evt.device, $evt.value, ($state.avgH)"
	def Float avH = state.avgH
	avH -= avH / NSample
	avH += Float.parseFloat(evt.value) / NSample
	state.avgH = avH
	
	if (debugOutput) log.debug "   Supported Commands of humidity devices $vHDevice:${vHDevice.supportedCommands}"
    if(vHDevice.supportedCommands.find{it.toString() == "setRelativeHumidity"}) { settings.vHDevice.setRelativeHumidity("${state.avgH.round(1)}"); sendEvent(name: "humidity", value: state.avgH, unit: "%", displayed: true)  }
	else { log.warn "Is Incorrect vHDevice - no Humidity" }
}

def illumHandler(evt) {
	def NSample = numberOption as Integer
	if (debugOutput) log.debug "illumHandler: $evt.device, $evt.value, ($state.avgL)"
	def Float avL = state.avgL
	avL -= avL / NSample
	avL += Float.parseFloat(evt.value) / NSample
	state.avgL = avL

    if (debugOutput) log.debug "   Supported Commands of illumance devices $vIDevice:${vIDevice.supportedCommands}"
	if(vIDevice.supportedCommands.find{it.toString() == "setIlluminance"}) { settings.vIDevice.setIlluminance("${state.avgL.round(1)}"); sendEvent(name: "illuminance", value: state.avgL, unit: "lux", displayed: true)  }
	else { log.warn "Is Incorrect vIDevice - no Illuminance" }
}

def energyHandler(evt) {
	def NSample = numberOption as Integer
    if (debugOutput) log.debug "energyHandler: $evt.device, $evt.value, ($state.avgE)"
    log.debug "energyHandler: $evt.device, $evt.value, ($state.avgE)"
	def Float avE = state.avgE
    avE -= avE / NSample
	avE += Float.parseFloat(evt.value) / NSample
	state.avgE = avE

    if (debugOutput) log.debug "   Supported Commands of energy devices $vEDevice:${vEDevice.supportedCommands}"
    if(vEDevice.supportedCommands.find{it.toString() == "setEnergy"}) { settings.vEDevice.setEnergy("${state.avgE.round(1)}"); sendEvent(name: "energy", value: state.avgE, unit:"Watts", displayed: true)  }
    else { log.warn "Is Incorrect vEDevice - no Energy" }
}

def installed() {
	log.info "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.info "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	if (debugOutput) runIn(1800,logsOff)
	subscribeTSelected()
    subscribeHSelected()
	subscribeISelected()
	subscripeESelected()
    initialize()
}

def initialize() {
	// an inital value of 0 will take a long time to average out, thus avg is initialized to an arbitrary indoor average
	if (state.avgT == null) state.avgT = location.temperatureScale == "F" ? 68 : 20
	if (state.avgH == null) state.avgH = 50
    if (state.avgL == null) state.avgL = 68
   	if (state.avgE == null) state.avgE = 0
    subscribeHSelected()
	subscribeTSelected()
	subscribeISelected()
	subscripeESelected()
}

def mainPage() {
	if (app.label == null)	{
		app.updateLabel(app.name)
	}
	dynamicPage(name: "mainPage", uninstall: true, install: true) {
		section(getFormat("title", " ${app.label}")) {
			section{paragraph "<div style='color:#1A77C9'>Calculate a Rolling Average of a set of sensors.</div>"    
				section{paragraph "<div style='color:#1A77C9'>Temperature sensors:</div>"   
					input "vTDevice", "capability.temperatureMeasurement", title: "Choose a Virtual Temperature Device to receive the Temperature Average.<i>(must support Temperature Measurement capability)</i>"
					input "tempSensors", "capability.temperatureMeasurement", title: "Choose Temperature Sensors to include in an Average", multiple: true
				}
				section{paragraph "<div style='color:#1A77C9'>Humidity sensors:</div>"    
					input "vHDevice", "capability.relativeHumidityMeasurement", title: "Choose a Virtual Humidity Device to receive the Humidity Average.<i>(must support Relative Humidity Measurement capability)</i>"
					input "humidSensors", "capability.relativeHumidityMeasurement", title: "Choose Humidity Sensors to include in an Average", multiple: true
				}
				section{paragraph "<div style='color:#1A77C9'>Illuminance sensors:</div>"   
					input "vIDevice", "capability.illuminanceMeasurement", title: "Choose a Virtual Illuminance Device to receive the Illuminance Average.<i>(must support Illuminance Measurement capability)</i>"
					input "illumSensors", "capability.illuminanceMeasurement", title: "Choose Illuminance Sensors to include in an Average", multiple: true
				}
				section{paragraph "<div style='color:#1A77C9'>Power sensors:</div>"   
					input "vEDevice", "capability.energyMeter", title: "Choose a Virtual Energy Device to receive the Energy Average.<i>(must support Energy Meter capability)</i>"
					input "energySensors", "capability.energyMeter", title: "Choose Energy capable sensors to include in an Average", multiple: true
				}
				section{paragraph "<div style='color:#1A77C9'Quantity of samples:</div>"   
					input (name: "numberOption", type: "number", defaultValue: "10", range: "10..200", title: "Number of samples to average.", description: "10 samples will be very responsive, while 200 samples is quite slow.", required: true)
				}
			}
			section (title: "<b>Name/Rename</b>") {
				label title: "This child app's Name (optional)", required: false
				input "debugOutput", "bool", title: "Enable Debug Logging?", required: false
			}
			display()
		} 
    }
}

def display() {
    section{
	   paragraph getFormat("line")
	   paragraph "<div style='color:#1A77C9;text-align:center;font-weight:small;font-size:9px'>Developed by: C Steele - Modded by J Shimota<br/>Version Status: $state.status<br>Current Version: ${version()} -  ${thisCopyright}</div>"
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    app?.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def getFormat(type, myText=""){
    if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def getThisCopyright(){"&copy; 2025 J Shimota "}
