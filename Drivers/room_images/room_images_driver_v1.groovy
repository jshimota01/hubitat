/*
 * Room Images
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
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2023-09-10    jshimota      0.1.0       Starting version
 *      2023-09-10    thebearmay    0.1.1       Used roomImage app components                     https://raw.githubusercontent.com/thebearmay/hubitat/main/roomImage.groovy
 *      2023-09-12    jshimota      0.1.5       Me working
 */

static String version()	{  return '0.1.5'  }

metadata {
    definition (
            name: "Room Images",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl:"https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/room_images/roomImages.groovy"
    ) {
        capability "Actuator"

        attribute "roomName", "string"
        attribute "roomImageNum", "number"
        attribute "roomImageTile", "string"
        attribute "roomImageImg", "string"
        attribute "html", "string"
    }
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("htmlVtile", "bool", title:"Use html attribute instead of roomImageTile")
    input("imagePathOvr", "string", title: "Alternate path to room images \n(must contain file names 0 through 7)")
}

def installed() {
    log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
    List<String>imgList = ["Bathroom", "Bedroom Closet", "Bedroom", "Coat Closet", "Dining Area", "Entryway", "Front Porch", "Guest Bathroom", "Hallway", "Kitchen", "Living Room", "Office", "Patio", "W-D Area"]
    if(imgNum!=null) {
        roomName = imgList[imgNum]
    } else {
        roomName = "Error - Out of Range"
    }
    updateAttr("roomImageImg", "<img class='roomImage' src='${imagePath}${imgNum}.jpg' />")
    updateAttr("roomImage", roomName)
    // String phaseIcon = "<div id='moonTile'><img class='roomImage' src='${imagePath}${imgNum}.jpg' style='max-width: 100%;height: auto;'><p class='small' style='text-align:center'>$roomName</p></div>"
    // String phaseIconHTML = "<div style='margin: auto; display: table; padding: 8px;'><table><tbody><tr><td><img class='roomImage' src='${imagePath}${imgNum}.jpg' ></td></tr><tr><td><p class='small' style='text-align:center'>$roomName</p></td></tr></tbody></table></div>"
    if(!htmlVtile)
        updateAttr("roomImageTile",phaseIcon)
    else
        updateAttr("html",phaseIconHTML)
    unschedule()
}

void updateAttr(String aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

void updateAttr(String aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){
}

def updated(){
    log.trace "updated()"
    unschedule()
    if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
    device.updateSetting("debugEnable",[value:"false",type:"bool"])
}