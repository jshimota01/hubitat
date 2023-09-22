import java.text.SimpleDateFormat

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
 *      2023-09-14    jshimota      0.1.6       progress
 *      2023-09-14    jshimota      0.1.7       progress
 */

static String version()	{  return '0.1.7'  }

metadata {
    definition (
            name: "Room Images",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl:"https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/room_images/roomImages.groovy"
    ) {
        capability "Actuator"

        attribute "rn0", "string"
        attribute "rn1", "string"
        attribute "rn2", "string"
        attribute "rn3", "string"
        attribute "rn4", "string"
        attribute "rn5", "string"
        attribute "rn6", "string"
        attribute "rn7", "string"
        attribute "rn8", "string"
        attribute "rn9", "string"
        attribute "rn10", "string"
        attribute "rn11", "string"
        attribute "rn12", "string"
        attribute "rn13", "string"

        attribute "rnNum0", "number"
        attribute "rnNum1", "number"
        attribute "rnNum2", "number"
        attribute "rnNum3", "number"
        attribute "rnNum4", "number"
        attribute "rnNum5", "number"
        attribute "rnNum6", "number"
        attribute "rnNum7", "number"
        attribute "rnNum8", "number"
        attribute "rnNum9", "number"
        attribute "rnNum10", "number"
        attribute "rnNum11", "number"
        attribute "rnNum12", "number"
        attribute "rnNum13", "number"

        attribute "rnImg0", "string"
        attribute "rnImg1", "string"
        attribute "rnImg2", "string"
        attribute "rnImg3", "string"
        attribute "rnImg4", "string"
        attribute "rnImg5", "string"
        attribute "rnImg6", "string"
        attribute "rnImg7", "string"
        attribute "rnImg8", "string"
        attribute "rnImg9", "string"
        attribute "rnImg10", "string"
        attribute "rnImg11", "string"
        attribute "rnImg12", "string"
        attribute "rnImg13", "string"

        attribute "roomImageTile", "string"
        attribute "roomImageImg", "string"
        attribute "html", "string"

        command "getMeta"
        command "updateMeta"
    }
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("htmlVtile", "bool", title:"Use html attribute instead of roomImageTile")
    input("imagePathOvr", "string", title: "Alternate path to room images \n(must contain file names 0 through 14)")
    input "pollEvery",   "enum", title: "<b>(for debugging) Poll/Update Images how frequently?</b>",  required:false, defaultValue: 1, options:[1:"minute",2:"hour"]
}

def installed() {
    log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
    // List<String>imgList = ["Bathroom", "Bedroom Closet", "Bedroom", "Coat Closet", "Dining Area", "Entryway", "Front Porch", "Guest Bathroom", "Hallway", "Kitchen", "Living Room", "Office", "Patio", "W-D Area"]
    getMeta()
}

    // updateMeta("roomImageImg", "<img class='roomImage' src='${imagePath}${imgNum}.jpg' />")
    // updateMeta("roomImage", roomName)
    // String roomIcon = "<div id='moonTile'><img class='roomImage' src='${imagePath}${imgNum}.jpg' style='max-width: 100%;height: auto;'><p class='small' style='text-align:center'>$roomName</p></div>"
    // String roomIconHTML = "<div style='margin: auto; display: table; padding: 8px;'><table><tbody><tr><td><img class='roomImage' src='${imagePath}${imgNum}.jpg' ></td></tr><tr><td><p class='small' style='text-align:center'>$roomName</p></td></tr></tbody></table></div>"
    // if(!htmlVtile)
    //    updateMeta("roomImageTile",roomIcon)
    // else
    //     updateMeta("html",roomIconHTML)
    // unschedule()

void getMeta() {

    //updateMeta("roomImageNum", roomList)
    // sendEvent(name: "comparisonTimeStr", value: comparisonTimeStr)
    // sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}${seasonName}.svg' style='height: 100px;' />", descriptionText: descriptionText)

    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/room_images/room_images/"
    if(iconPathOvr > " ") iconPath = iconPathOvr


    sendEvent(name: "rn0", value: "Bedroom Closet")
    sendEvent(name: "rnNum0", value: 0)
    sendEvent(name: "rnImg0", value: "<img class='rnImg' src='${iconPath}0.jpg' />")
    sendEvent(name: "rn1", value: "Bedroom")
    sendEvent(name: "rnNum1", value: 1)
    sendEvent(name: "rnImg1", value: "<img class='rnImg' src='${iconPath}1.jpg' />")
    sendEvent(name: "rn2", value: "Coat Closet")
    sendEvent(name: "rnNum2", value: 2)
    sendEvent(name: "rnImg2", value: "<img class='rnImg' src='${iconPath}2.jpg' />")
    sendEvent(name: "rn3", value: "Dining Area")
    sendEvent(name: "rnNum3", value: 3)
    sendEvent(name: "rnImg3", value: "<img class='rnImg' src='${iconPath}3.jpg' />")
    sendEvent(name: "rn4", value: "Entryway")
    sendEvent(name: "rnNum4", value: 4)
    sendEvent(name: "rnImg4", value: "<img class='rnImg' src='${iconPath}4.jpg' />")
    sendEvent(name: "rn5", value: "Front Porch")
    sendEvent(name: "rnNum5", value: 5)
    sendEvent(name: "rnImg5", value: "<img class='rnImg' src='${iconPath}5.jpg' />")
    sendEvent(name: "rn6", value: "Guest Bathroom")
    sendEvent(name: "rnNum6", value: 6)
    sendEvent(name: "rnImg6", value: "<img class='rnImg' src='${iconPath}6.jpg' />")
    sendEvent(name: "rn7", value: "Hallway")
    sendEvent(name: "rnNum7", value: 7)
    sendEvent(name: "rnImg7", value: "<img class='rnImg' src='${iconPath}7.jpg' />")
    sendEvent(name: "rn8", value: "Kitchen")
    sendEvent(name: "rnNum8", value: 8)
    sendEvent(name: "rnImg8", value: "<img class='rnImg' src='${iconPath}8.jpg' />")
    sendEvent(name: "rn9", value: "Living Room")
    sendEvent(name: "rnNum9", value: 9)
    sendEvent(name: "rnImg9", value: "<img class='rnImg' src='${iconPath}9.jpg' />")
    sendEvent(name: "rn10", value: "Office")
    sendEvent(name: "rnNum10", value: 10)
    sendEvent(name: "rnImg10", value: "<img class='rnImg' src='${iconPath}10.jpg' />")
    sendEvent(name: "rn11", value: "Patio")
    sendEvent(name: "rnNum11", value: 11)
    sendEvent(name: "rnImg11", value: "<img class='rnImg' src='${iconPath}11.jpg' />")
    sendEvent(name: "rn12", value: "W-D Area")
    sendEvent(name: "rnNum12", value: 12)
    sendEvent(name: "rnImg12", value: "<img class='rnImg' src='${iconPath}12.jpg' />")
    sendEvent(name: "rn13", value: "Bathroom")
    sendEvent(name: "rnNum13", value: 13)
    sendEvent(name: "rnImg13", value: "<img class='rnImg' src='${iconPath}13.jpg' />")
    unschedule()
    //  "Seconds" "Minutes" "Hours" "Day Of Month" "Month" "Day Of Week" "Year"
    if(debugEnable) {
        unschedule()
        if (pollEvery == 1) schedule("* * * * *", getMeta())
        if (pollEvery == 2) schedule("0 * * * *", getMeta())
    else
        unschedule()
    }
}

void updateMeta(String aKey, aValue, aUnit){
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