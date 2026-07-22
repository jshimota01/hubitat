/**
 *
 *  (Mod of C Steele 2019 AverageThis Community app)
 *
 *
 *  AvgTHIE Parent App
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
 * 05-01-26     jshimota     updated to include gemini recommendations
 *
 */

/**
 *  AvgTHIE Parent App
 */
public static String version()   {  return "v1.1.2"  }

definition (
    name: "AvgTHIE",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Average a set of Temperature, Humidity, Illuminance or Energy sensors into a specialized virtual device.",
    category: "Averaging",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
    page(name: "mainPage")
}

def installed() {
    log.info "Installed with settings: ${settings}"
    state.status = "Stable" // Initialize the status variable
    initialize()
}

def updated() {
    log.info "Updated with settings: ${settings}"
    state.status = "Updated"
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
    // Log active children for debugging
    log.info "There are ${childApps.size()} active AvgTHIE child instances."
}

def mainPage() {
    dynamicPage(name: "mainPage", uninstall: true, install: true) {
        section(getFormat("title", " ${app.label ?: 'AvgTHIE Manager'}")) {
            paragraph "<div style='color:#1A77C9'>Calculate a rolling average of a set of temperature, humidity, illuminance or energy sensor values into a specialized virtual device.</div>"    
            paragraph "<b>This parent app is a container for all AvgTHIE child apps.</b>"
        }
        
        section {
            // Ensure appName matches the name in your child app's metadata
            app(name: "AvgTHIEChild", appName: "AvgTHIEChild", namespace: "jshimota", title: "<b>Add a New Average Instance</b>", multiple: true)
        }    
        
        section (title: "<b>App Settings</b>") {
            label title: "Assign a custom name to this Parent App (optional)", required: false
        } 
        
        display()
    }
}

def display() {
    section {
       paragraph getFormat("line")
       paragraph "<div style='color:#1A77C9;text-align:center;font-size:10px'>Developed by: C Steele, Modded by J Shimota<br/>" +
                 "Status: ${state.status ?: 'Initialized'} | Version: ${version()}<br>${getThisCopyright()}</div>"
    }
}

def getFormat(type, myText=""){
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def getThisCopyright() { return "&copy; 2026 J Shimota " }