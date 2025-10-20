/**
 *
 *  Mod of C Steele 2019 AverageThis Community app
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
	name: "AverageHTIP",
	namespace: "jshimota",
	author: "James Shimota",
	description: "Average a set of Humidity, Temperature, Illuminance or Power Sensors into a Virtual Humidity Sensor.",
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


def installed() {
	log.info "Installed with settings: ${settings}"
	initialize()
}


def updated() {
	log.info "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}


def initialize() {
	version()
	log.info "There are ${childApps.size()} child Apps"
	childApps.each {child -> log.info "Child app: ${child.label}" }
}


def mainPage() {
	dynamicPage(name: "mainPage", uninstall: true, install: true)
	{
		section(getFormat("title", " ${app.label}")) {}
		section
		{
			paragraph "<div style='color:#1A77C9'>Calculate a rolling average of a set of humidity, temperature, illuminance or power sensor values into a virtual corresponding sensor.</div>"    
			paragraph title: "<AverageHTIP",
			"<b>This parent app is a container for all:</b><br> AverageHTIP child apps"
		}
		section 
		{
			app(name: "AverageHTIPChild", appName: "AverageHTIPChild", namespace: "jshimota", title: "<b>New Average HTIP child</b>", multiple: true)
		//	app(name: "AverageTemperatureChild", appName: "AverageTemperatureChild", namespace: "jshimota", title: "<b>New AverageTemerature child</b>", multiple: true)
		//	app(name: "AverageIlluminanceChild", appName: "AverageIlluminanceChild", namespace: "jshimota", title: "<b>New AverageIlluminance child</b>", multiple: true)
		//	app(name: "AveragePowerChild", appName: "AveragePowerChild", namespace: "jshimota", title: "<b>New AveragePower child</b>", multiple: true)
		}    
		section (title: "<b>Name/Rename</b>") 
		{
			label title: "Enter a name for this parent app (optional)", required: false
		} 
		display()
	}
}


def display() {
    section{
	   paragraph getFormat("line")
	   paragraph "<div style='color:#1A77C9;text-align:center;font-weight:small;font-size:9px'>Developed by: C Steele, Modded by J Shimota<br/>Version Status: $state.status<br>Current Version: ${version()} -  ${thisCopyright}</div>"
    }
}


def getFormat(type, myText=""){
    if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def getThisCopyright(){"&copy; 2025 J Shimota "}
