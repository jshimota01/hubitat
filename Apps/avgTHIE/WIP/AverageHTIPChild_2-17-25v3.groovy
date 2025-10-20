/**
 *
 *  Average Humidity Child 
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
 public static String version()      {  return "v1.0.0"  }

definition (
	name: "AverageHTIPChild",
	namespace: "jshimota",
	author: "J Shimota",
	description: "Child: Average a set of humidity, temperature, illuminance or power sensors to a virtual sensor.",
	category: "Averaging",
	parent: "jshimota:AverageHTIP",
	iconUrl: "",
	iconX2Url: "",
	iconX3Url: "",
)


// Preference pages
preferences
{
	page(name: "mainPage")
}


def subscribeHSelected() {
	if (debugOutput) log.debug "subscribeHSelected: $humidSensors"
	if (humidSensors?.size()) 
	{
	    subscribe(humidSensors, "humidity", humidHandler)
	}
}

def subscribeTSelected() {
	if (debugOutput) log.debug "subscribeTSelected: $tempSensors"
	if (tempSensors?.size()) 
	{
		subscribe(tempSensors, "temperature", tempHandler)
	}
}

def subscribeISelected() {
	if (debugOutput) log.debug "subscribeISelected: $illumSensors"
	if (illumSensors?.size()) 
	{
	    subscribe(illumSensors, "illuminance", illumHandler)
	}
}

def subscribePSelected() {
	if (debugOutput) log.debug "subscribePSelected: $powerSensors"
	if (powerSensors?.size()) 
	{
	    subscribe(powerSensors, "power", powerHandler)
	}
}

def humidHandler(evt) {
	def NSample = numberOption as Integer
	if (debugOutput) log.debug "humidHandler: $evt.device, $evt.value, ($state.avgH)"
	def float avH = state.avgH
	avH -= avH / NSample
	avH += Float.parseFloat(evt.value) / NSample
	state.avgH = avH
	
	if(vHDevice.supportedCommands.find{it.toString() == "setRelativeHumidity"}) { settings.vHDevice.setRelativeHumidity("${state.avgH.round(1)}"); sendEvent(name: "humidity", value: state.avgH, unit: "%", displayed: true)  }
	else { log.warn "Is Incorrect vHDevice - no Humidity" }
}

def tempHandler(evt) {
	def NSample = numberOption as Integer
	if (debugOutput) log.debug "tempHandler: $evt.device, $evt.value, ($state.avgT)"
	def float avT = state.avgT
	avT -= avT / NSample
	avT += Float.parseFloat(evt.value) / NSample
	state.avgT = avT
	
	if(vTDevice.supportedCommands.find{it.toString() == "setTemperature"}) { settings.vTDevice.setTemperature("${state.avgT.round(1)}"); sendEvent(name: "temperature", value: state.avgT, unit:"°${location.temperatureScale}", displayed: true)  }
	else { log.warn "Is Incorrect vTDevice - no Temperature" }
}

def illumHandler(evt) {
	def NSample = numberOption as Integer
	if (debugOutput) log.debug "illumHandler: $evt.device, $evt.value, ($state.avgL)"
	/* exponentially weighted average
	    avg -= avg / N;
    	    avg += new_sample / N;	
	 when N=1 avg = the latest value. When N > 100 is approximately the number of previous values to average
	 */	 
	def float avL = state.avgL
	avL -= avL / NSample
	avL += Float.parseFloat(evt.value) / NSample
	state.avgL = avL
	
	if(vIDevice.supportedCommands.find{it.toString() == "setIlluminance"}) { settings.vIDevice.setIlluminance("${state.avgL.round(1)}"); sendEvent(name: "illuminance", value: state.avgL, unit: "lux", displayed: true)  }
	else { log.warn "Is Incorrect vIDevice - no Illuminance" }
}

def powerHandler(evt) {
	def NSample = numberOption as Integer
    if (debugOutput) log.debug "powerHandler: $evt.device, $evt.value, ($state.avgP)"
	def float avP = state.avgP
	avP -= avP / NSample
	avP += Float.parseFloat(evt.value) / NSample
	state.avgP = avP
	
    if(vPDevice.supportedCommands.find{it.toString() == "setPower"}) { settings.vPDevice.setpower("${state.avgP.round(1)}"); sendEvent(name: "power", value: state.avgP, unit:"w${location.powerScale}", displayed: true)  }
    else { log.warn "Is Incorrect vPDevice - no Power" }
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
	if (debugOutput) log.debug "   Supported Commands of $vHDevice:${vHDevice.supportedCommands}"
   	if (debugOutput) log.debug "   Supported Commands of $vTDevice:${vTDevice.supportedCommands}"
	if (debugOutput) log.debug "   Supported Commands of $vIDevice:${vIDevice.supportedCommands}"
	if (debugOutput) log.debug "   Supported Commands of $vPDevice:${vPDevice.supportedCommands}"
    initialize()
}

def initialize() {
	// an inital value of 0 will take a long time to average out, thus avg is initialized to an arbitrary indoor average
	if (state.avgL == null) state.avgL = 68
	if (state.avgT == null) state.avgT = location.temperatureScale == "F" ? 68 : 20
	if (state.avgH == null) state.avgH = 50
   	if (state.avgP == null) state.avgH = 75
	subscribeHSelected()
	subscribeTSelected()
	subscribeISelected()
	subscribePSelected()
}

def mainPage() {
	if (app.label == null)	{
		app.updateLabel(app.name)
	}
	dynamicPage(name: "mainPage", uninstall: true, install: true) {
		section(getFormat("title", " ${app.label}")) {
			section{paragraph "<div style='color:#1A77C9'>Calculate a Rolling Average of a set of sensors.</div>"    
				section{paragraph "<div style='color:#1A77C9'>Humidity sensors:</div>"    
					input "vHDevice", "capability.relativeHumidityMeasurement", title: "Choose a Virtual Humidity Device to receive the Humidity Average.<i>(must support Relative Humidity Measurement capability)</i>"
					input "humidSensors", "capability.relativeHumidityMeasurement", title: "Choose Humidity Sensors to include in an Average", multiple: true
				}
				section{paragraph "<div style='color:#1A77C9'>Temperature sensors:</div>"   
					input "vTDevice", "capability.temperatureMeasurement", title: "Choose a Virtual Temperature Device to receive the Temperature Average.<i>(must support Temperature Measurement capability)</i>"
					input "tempSensors", "capability.temperatureMeasurement", title: "Choose Temperature Sensors to include in an Average", multiple: true
				}
				section{paragraph "<div style='color:#1A77C9'>Illuminance sensors:</div>"   
					input "vIDevice", "capability.illuminanceMeasurement", title: "Choose a Virtual Illuminance to receive the Illuminance Average.<i>(must support Illuminance Measurement capability)</i>"
					input "illumSensors", "capability.illuminanceMeasurement", title: "Choose Illuminance Sensors to include in an Average", multiple: true
				}
				section{paragraph "<div style='color:#1A77C9'>Power sensors:</div>"   
					input "vPDevice", "capability.powerMeasurement", title: "Choose a Virtual Power Device to receive the Power Average.<i>(must support Power Measurement capability)</i>"
					input "powerSensors", "capability.powerMeasurement", title: "Choose Power Sensors to include in an Average", multiple: true
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
