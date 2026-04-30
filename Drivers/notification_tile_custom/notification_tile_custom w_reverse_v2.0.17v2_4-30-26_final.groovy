/*
* Notify Tile Device Customized by JAS
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
*    2026-04-30  jshimota      v2.0.18 ran through chatGPT
*/
/*
 * Notification Tile v2.0.18 (JAS Clean Rewrite)
 */

import java.text.SimpleDateFormat
import groovy.transform.Field

static String version() { '2.0.18' }

@Field sdfList = [
 "ddMMMyyyy HH:mm","ddMMMyyyy HH:mm:ss","ddMMMyyyy hh:mma",
 "dd/MM/yyyy HH:mm:ss","MM/dd/yyyy HH:mm:ss",
 "dd/MM/yyyy hh:mma","MM/dd/yyyy hh:mma",
 "MM/dd HH:mm","MM/dd h:mma",
 "HH:mm","H:mm","h:mma","None"
]

metadata {
    definition (
		name: "Notification Tile (Custom)", 
		namespace: "jshimota", 
		description: "Simple driver to act as a destination for notifications, and provide an attribute to display the last 5 on a tile.",
		author: "Jean P. May, Jr.",
		importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/notifyTile.groovy",
           singleThreaded: true
    ) {
        capability "Notification"
        capability "Momentary"
        capability "Configuration"

        attribute "last5", "STRING"
        attribute "last5H", "STRING"
    }
}
    
preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("sdfPref", "enum", title: "Date Format", options:sdfList, defaultValue:"ddMMMyyyy HH:mm")
    input("leadingDate", "bool", title:"Date first?")
    input("msgLimit", "number", title:"Messages (1–20)", defaultValue:5, range:"1..20")
    input("create5H", "bool", title: "Create horizontal tile?")

    input("existingTileFontSize", "number", title: "Font Size (%)", defaultValue:100)
    input("existingTileHorzWordPos", "enum", title: "Alignment", options:["left","center","right"], defaultValue:"left")
    input("existingTileFontColor", "text", title: "Default Text Color", defaultValue:"#FFFFFF")

    input("revFill", "bool", title: "Reverse order")
    input("preAdd", "bool", title: "Use PRE formatting")

    input("colorE", "text", title: "[E]", defaultValue:"red")
    input("colorH", "text", title: "[H]", defaultValue:"orange")
    input("colorL", "text", title: "[L]", defaultValue:"goldenrod")
    input("colorN", "text", title: "[N]", defaultValue:"green")
    input("colorDefault", "text", title: "Default", defaultValue:"white")
}

void installed() {
    state.messages = []
    updateTiles()
}

void updated() {
    if(!state.messages) state.messages = []
    updateTiles()
}

void deviceNotification(notification) {
    if(debugEnable) log.debug "Incoming: ${notification}"

    String msg = notification?.trim()
    if(!msg) return

    msg = formatMessage(msg)

    if(!state.messages) state.messages = []

    if(revFill) {
        state.messages << msg
    } else {
        state.messages.add(0, msg)
    }

    Integer limit = (settings.msgLimit ?: 5)
    state.messages = state.messages.take(limit)

    updateTiles()
}

void updateTiles() {
    def msgs = state.messages ?: []

    sendEvent(name:"last5", value: buildTile(msgs))

    if(settings.create5H) {
        sendEvent(name:"last5H", value: msgs.collect { stripHtml(it) }.join(" ** "))
    }
}

String buildTile(List msgs) {

    String style = "<style>.tile {display:block;" +
        "text-align:${settings.existingTileHorzWordPos};" +
        "font-size:${settings.existingTileFontSize}%;" +
        "color:${settings.existingTileFontColor};" +
        (settings.preAdd ? "white-space:pre-line;" : "") +
        "}</style>"

    if(!msgs) return style + "<span class='tile'></span>"

    String body = msgs.join("<br />")

    // Safety trim (Hubitat limit)
    while(body.length() > 1000 && msgs.size() > 0) {
        msgs = msgs.dropRight(1)
        body = msgs.join("<br />")
    }

    return style + "<span class='tile'>${body}</span>"
}

String formatMessage(String msg) {

    def match = (msg =~ /^\\[([EHLN])\\]/)
    String tag = match ? match[0][1] : null

    msg = msg.replaceFirst(/^\\[[EHLN]\\]\\s*/, "")

    String ts = ""
    if(settings.sdfPref != "None") {
        ts = new SimpleDateFormat(settings.sdfPref).format(new Date())
    }

    String combined = settings.leadingDate ?
        "${ts} ${msg}".trim() :
        "${msg} ${ts}".trim()

    return applyColor(tag, combined)
}

String applyColor(String tag, String msg) {

    Map colors = [
        "E": settings.colorE,
        "H": settings.colorH,
        "L": settings.colorL,
        "N": settings.colorN
    ]

    String color = colors[tag] ?: settings.colorDefault ?: "#FFFFFF"

    return "<span style='color:${color}'>${msg}</span>"
}

String stripHtml(String input) {
    return input?.replaceAll("<[^>]*>", "")
}

void push() {
    state.messages = []
    updateTiles()
}

void logsOff(){
    device.updateSetting("debugEnable",[value:"false",type:"bool"])
}