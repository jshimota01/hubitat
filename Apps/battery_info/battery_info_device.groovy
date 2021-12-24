/*
 * Battery Info Device
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2021-12-22    jshimota      0.1.01       Starting version
 *
 *
 */

import java.text.SimpleDateFormat

static String version() { return '0.1.01' }

metadata{
    definition( name: "Battery Info Device", namespace: "jshimota", author: "James Shimota") {

        capability "Actuator"
        capability "Refresh"
        attribute "ID", "number"
        attribute "Battery_Type", "string"
        attribute "Battery_Qty", "number"
        attribute "Battery_Brand", "string"
        attribute "Battery_Rechargeable", "bool"
        attribute "Battery_First_Installed", "string"
        attribute "Battery_Last_Changed", "string"
    }
    preferences{
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true

    }
}

def ID(str) {
    Number ID = 1347
    def descriptionText = "ID being set" as Object
    sendEvent(name: "ID", value: "${ID}")
    if (txtEnable) log.info "${descriptionText}"
}
def Battery_Type(str) {
    String Battery_Type = "AA"
    sendEvent(name: "Battery_Type", value: "${Battery_Type}")
}
def Battery_Qty(str) {
    Number Battery_Qty = 4
    sendEvent(name: "Battery_Qty", value: "${Battery_Qty}")
}
def Battery_Brand(str) {
    String Battery_Brand = "Amazon"
    sendEvent(name: "Battery_Brand", value: "${Battery_Brand}")
}
def Battery_Rechargeable(str) {
    Boolean Battery_Rechargeable = false
    sendEvent(name: "Battery_Rechargeable", value: "${Battery_Rechargeable}")
}
def Battery_First_Installed(str) {
    String Battery_First_Installed = "12/10/2021"
    sendEvent(name: "Battery_First_Installed", value: "${Battery_First_Installed}")
}
def Battery_Last_Changed(str) {
    String Battery_Last_Changed = "12/29/2021"
    sendEvent(name: "Battery_Last_Changed", value: "${Battery_Last_Changed}")
}

def initialize(){
    Battery_Last_Changed(str)
    Battery_First_Installed(str)
    Battery_Rechargeable(str)
    Battery_Brand(str)
    Battery_Qty(str)
    Battery_Type(str)
    ID(str)
}

def refresh() {
    // runCmd()
    Battery_Last_Changed(str)
    Battery_First_Installed(str)
    Battery_Rechargeable(str)
    Battery_Brand(str)
    Battery_Qty(str)
    Battery_Type(str)
    ID(str)
}