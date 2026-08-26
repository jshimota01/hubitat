/*
* Notify Tile Device
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
*    Date        Who            What
*    ----        ---            ----
*    2021-01-06  thebearmay	Original version 0.1.0
*    2021-01-07  thebearmay	Fix condition causing a loss notifications if they come in rapidly
*    2021-01-07  thebearmay	Add alternative date format
*    2021-01-07  thebearmay	Add last5H for horizontal display
*    2021-01-07  thebearmay	Add leading date option
*    2021-03-10  thebearmay	Lost span tag with class=last5
*    2021-11-14  ArnB  2.0.0	Add capability Momentary an routine Push allowing a Dashboard switch to clear all messages. 	
*    2021-11-15  ArnB  2.0.0	Revise logic minimizing attributes and sendevents. Allow for 5 to 20 messages in tile. Insure tile is less than 1024 	
*    2021-11-16  ArnB  2.0.1	Fix: storing one less message than requested. 
*					correct <br/> to <br />
*					Restore: attribute last5H as an optional preference. 
*    2021-11-17  ArnB  2.0.2	Add conversion logic from original version in Update routine 
*    2021-11-17  ArnB  2.0.3	Add logic when message count shinks rather than reconfigure
*    2021-11-18  ArnB  2.0.4	Add singleThreaded true
*    2021-11-18  thebearmay    2.0.5 Remove unused attributes from v1.x.x
*    2021-11-20  thebearmay    Add option to only display time
*    2021-11-22  thebearmay    make date time format a selectable option
*    2021-12-07  thebearmay    add "none" as a date time format
*    2022-04-06  thebearmay    fix max message state coming back as string
*    2022-09-15  thebearmay    issue with clean install
*    2022-12-06  thebearmay    additional date/time format
* 	 2025-04-03	 thebearmay	   add time/date formats, lowered mininum message count to 1
*    2025-04-20  amithalp	   add color options
*	 2026-04-21	 thebearmay	   v2.0.14 add a reverse fill option
*	 2026-04-22	 thebearmay	   v2.0.15 initialize state.lastLimit when configuring
*    2026-04-21  jshimota      v2.0.16 added back my customizations of layout values and features
*    2026-04-21  jshimota      v2.0.17 added switch for PRE wrapper
* 	 2026-04-30	 jshimota      v2.0.18 Gemini fixes
*    2026-05-03  jshimota      v2.0.19 Repair of Gemini created issues 
*    2026-05-03  jshimota      v2.0.20 format improvement
*/
/*
* Notification Tile (Custom) - Refactored
*/
import java.text.SimpleDateFormat
import groovy.transform.Field
static String version()    {  return '2.0.20'  }

@Field sdfList = ["ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy hh:mma", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy hh:mma", "MM/dd/yyyy hh:mma", "MM/dd HH:mm", "MM/dd h:mma", "HH:mm", "H:mm","h:mma", "HH:mm ddMMMyyyy","HH:mm:ss ddMMMyyyy","hh:mma ddMMMyyyy", "HH:mm:ss dd/MM/yyyy", "HH:mm:ss MM/dd/yyyy", "hh:mma dd/MM/yyyy ", "hh:mma MM/dd/yyyy", "HH:mm yyyy-MM-dd", "None"]

metadata {
    definition (
            name: "Notification Tile (Custom)", 
            namespace: "jshimota", 
            description: "Simple driver to act as a destination for notifications, and provide an attribute to display the last X on a tile.",
            author: "Jean P. May, Jr. / Refactored",
            importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/notifyTile.groovy",
            singleThreaded: true
        ) {
            capability "Notification"
            capability "Momentary"
            capability "Configuration"

            attribute "last5", "STRING"
            attribute "last5H", "STRING"
            attribute "tileFontSize", "number"
            attribute "tileFontColor", "string"
            attribute "tileHorzWordPos", "string"
            attribute "tileWrap", "string"
            }   
        }

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("sdfPref", "enum", title: "Date/Time Format", options:sdfList, defaultValue:"ddMMMyyyy HH:mm")
    input("leadingDate", "bool", title:"Use leading date instead of trailing")
    input("msgLimit", "number", title:"Number of messages from 1 to 20", defaultValue:5, range:1..20)
    input("create5H", "bool", title: "Create horizontal message tile?")
    input(name: "existingTileFontSize", type: "num", title: "HTML Tile Font Size (%)*", defaultValue: 100)
    input(name: "existingTileHorzWordPos", type: "string", title: "HTML Word Position (left, right, center)", defaultValue: "left")
    input(name: "existingTileFontColor", type: "string", title: "HTML Tile Text Color (also supports 6 or 8 char Hex format with leading #)", defaultValue: "white")
    input("revFill", "bool", title: "Reverse the fill order (Newest at bottom)")
    input("preAdd", "bool", title: "Encase message tile with 'pre' to format")
    input("colorE", "text", title: "Color for [E] Emergency", defaultValue: "red")
    input("colorH", "text", title: "Color for [H] High", defaultValue: "orange")
    input("colorL", "text", title: "Color for [L] Low", defaultValue: "goldenrod")
    input("colorN", "text", title: "Color for [N] Normal", defaultValue: "green")
}

void installed() {
    state.msgCount = 0
    configure()
}

void updated(){
    if(debugEnable) runIn(1800, logsOff)
    configure()
}

void configure() {
    if (debugEnable) log.debug "configure()"
    
    state.msgList = [] 
    state.msgCount = 0

    // 1. Create the formatted "empty" string
    String emptyMsg = " No notifications"
    if (preAdd) emptyMsg = "<pre style='margin:0;'>${emptyMsg}</pre>"
    
    // 2. Wrap it in the styles and span class
    String formattedEmpty = getTileStyles() + "<span class='last5'>${emptyMsg}</span>"

    // 3. Send the formatted version to the attributes
    sendEvent(name: "last5", value: formattedEmpty)
	sendEvent(name: "last5H", value: "** No notifications **")
    
    // Update preference display attributes
    sendEvent(name: "tileFontColor", value: existingTileFontColor)
    sendEvent(name: "tileHorzWordPos", value: existingTileHorzWordPos)
    sendEvent(name: "tileFontSize", value: existingTileFontSize)
}

// Helper to generate the CSS block dynamically
String getTileStyles() {
    String whiteSpace = preAdd ? "white-space:pre-line;" : ""
    return "<style>.last5 {display:block;${whiteSpace}text-align:${existingTileHorzWordPos};font-size:${existingTileFontSize}%;}</style>"
}

def deviceNotification(String notification) {
    if (debugEnable) log.debug "deviceNotification entered: ${notification}" 

    if(sdfPref == null) device.updateSetting("sdfPref",[value:"ddMMMyyyy HH:mm",type:"enum"])
    
    String rawInput = notification?.trim() ?: ""
    if (rawInput.length() > 800) {
        rawInput = rawInput.substring(0, 797) + "..."
    }

    String tag = rawInput.find(/\[[A-Z]+\]/)
    String cleanedMsg = rawInput.replaceFirst(/\[[A-Z]\]/, '').trim()

    String timestamp = ""
    if (sdfPref != "None") {
        SimpleDateFormat sdf = new SimpleDateFormat(sdfPref)
        timestamp = sdf.format(new Date())
    }
    // Determine the separator based on the 'preAdd' switch
    String spacer = preAdd ? "" : " "
    
    // Construct message with conditional spacing
    String msgWithTime = leadingDate ? "${timestamp}${spacer}${cleanedMsg}" : "${cleanedMsg}${spacer}${timestamp}"

    // CHANGE: Ensure we handle the tag check clearly here
    String msgToColor = tag ? "${tag} ${msgWithTime}" : msgWithTime
    String colorized = colorizeNotification(msgToColor) // Line 155
    
    if (preAdd) colorized = "<pre style='margin:0;'>${colorized}</pre>"
    
    if (state.msgList == null) state.msgList = []
    
    if (!revFill) state.msgList.add(0, colorized) 
    else state.msgList.add(colorized)    

    int limit = (settings.msgLimit ?: 5).toInteger()
    while (state.msgList.size() > limit) {
        if (!revFill) state.msgList.removeAt(state.msgList.size() - 1) else state.msgList.removeAt(0)
    }

    String wkTile = state.msgList.join("<br />")
    while (wkTile.length() > 950 && state.msgList.size() > 1) { 
        if (!revFill) state.msgList.removeAt(state.msgList.size() - 1) else state.msgList.removeAt(0)
        wkTile = state.msgList.join("<br />")
    }

    String finalOutput = getTileStyles() + "<span class='last5'>${wkTile}</span>"
    sendEvent(name:"last5", value: finalOutput)
    state.msgCount = state.msgList.size()

    if (settings.create5H) {
        sendEvent(name:"last5H", value: " ** " + wkTile.replaceAll("<br />"," ** ") + " ** ")
    }
}

// CHANGE: Explicitly define as 'def' or 'private String' to ensure visibility
def colorizeNotification(String msg) {
    String color
    String cleaned = msg
    String icon = "" 

    if (msg.startsWith("[E]")) {
        icon = "🚨"
        color = settings.colorE ?: "red"
        cleaned = msg.replace("[E]", "").trim()
    } else if (msg.startsWith("[H]")) {
        icon = "⚠️"
        color = settings.colorH ?: "orange"
        cleaned = msg.replace("[H]", "").trim()
    } else if (msg.startsWith("[L]")) {
        icon = "🔋"
        color = settings.colorL ?: "goldenrod"
        cleaned = msg.replace("[L]", "").trim()
    } else if (msg.startsWith("[N]")) {
        icon = "ℹ️"
        color = settings.colorN ?: "green"
        cleaned = msg.replace("[N]", "").trim()
    } else {
        icon = ""
        color = existingTileFontColor ?: "white"
    }

    return "<span style='color:${color}'> ${icon} ${cleaned}</span>"
}

void logsOff(){
    device.updateSetting("debugEnable",[value:"false",type:"bool"])
}

void push() {
    state.msgList = []
    configure()
}
