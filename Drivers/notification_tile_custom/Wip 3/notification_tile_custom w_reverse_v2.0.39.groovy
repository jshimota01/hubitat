/*
* Notify Tile Device (Custom)
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
*	 2026-04-21	 thebearmay	   v2.0.14 add a reverse fill order option
*	 2026-04-22	 thebearmay	   v2.0.15 initialize state.lastLimit when configuring
*    2026-04-21  jshimota      v2.0.16 added back my customizations of layout values and features
*    2026-04-21  jshimota      v2.0.17 added switch for PRE wrapper
* 	 2026-04-30	 jshimota      v2.0.18 Gemini fixes
*    2026-05-03  jshimota      v2.0.19 Repair of Gemini created issues 
*    2026-05-03  jshimota      v2.0.20 format improvement
*    2026-07-27  jshimota      v2.0.21 Updated color default for Low/High brackets to improve high contrast legibility
*    2026-07-27  jshimota      v2.0.22 Drop shadow trial
*    2026-07-27  jshimota      v2.0.23 Outline trial
*    2026-07-27  jshimota      v2.0.24 Vertical backlight trial
*    2026-07-27  jshimota      v2.0.25 Split drop shadows: Black for [L]/[H], White for [E]/[N]
*    2026-07-27  jshimota      v2.0.26 Updated default colors for [L] (#FFD700) and [H] (#FF6600) for vibrant contrast
*    2026-07-27  jshimota      v2.0.27 Added enableShadow preference switch
*    2026-08-06  jshimota      v2.0.28 Increased HTML tile length safety buffer from 950 to 1010 chars
*    2026-08-06  jshimota      v2.0.29 Replaced per-line PRE tags with lightweight CSS pre-wrap styling
*    2026-08-23  jshimota      v2.0.30 Integrated dynamic log handlers and sendIfChanged state manager
*    2026-08-23  jshimota      v2.0.31 Resolved regex tag stripping bug, logsOff settings type, and sendIfChanged comparison
*    2026-08-23  jshimota      v2.0.32 Added description text for preference switches
*    2026-08-23  jshimota      v2.0.33 Converted font size to em units and added missing preference descriptions
*    2026-08-23  jshimota      v2.0.34 Added preference descriptions for bracket color preferences
*    2026-08-23  jshimota      v2.0.35 Added description for Date/Time format preference selector
*    2026-08-23  jshimota      v2.0.36 Fixed BigDecimal tileFontSize attribute type, dynamic HTML buffer limit, and CSS fallbacks
*    2026-08-23  jshimota      v2.0.37 Minified CSS payload and shortened shadow class names to optimize tile storage capacity
*    2026-08-23  jshimota      v2.0.38 Converted priority colors to CSS classes, optimized wrapper defaults, and stripped empty timestamp spaces
*    2026-08-23  jshimota      v2.0.39 Option A base 10px scaling (0.625em) and string-type flexible font size input parser
*/
/*
* Notification Tile (Custom) - Refactored
*/
import java.text.SimpleDateFormat
import groovy.transform.Field
static String version()    {  return '2.0.39'  }

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
            attribute "tileFontSize", "string"
            attribute "tileFontColor", "string"
            attribute "tileHorzWordPos", "string"
            attribute "tileWrap", "string"
            }   
        }

preferences {
    input("logInfoEnable", "bool", title: "Enable info logging?", description: "Turns on info logging.", defaultValue: true)
    input("logDebugEnable", "bool", title: "Enable debug logging?", description: "Turns on debug logging.", defaultValue: false)
    input("logTraceEnable", "bool", title: "Enable trace logging?", description: "Turns on trace logging.", defaultValue: false)
    input("logWarnEnable", "bool", title: "Enable warning logging?", description: "Turns on warning logging.", defaultValue: true)
    input("logErrorEnable", "bool", title: "Enable error logging?", description: "Turns on error logging.", defaultValue: true)
    input("sdfPref", "enum", title: "Date/Time Format", description: "Select the timestamp style to append to incoming notification entries (or 'None' to omit timestamps).", options:sdfList, defaultValue:"ddMMMyyyy HH:mm")
    input("leadingDate", "bool", title: "Use leading date instead of trailing", description: "Places timestamp before the notification message text.")
    input("msgLimit", "number", title: "Number of messages from 1 to 20", description: "Sets the maximum number of notification entries stored and displayed on the tile.", defaultValue: 5, range: "1..20")
    input("create5H", "bool", title: "Create horizontal message tile?", description: "Generates the horizontal last5H attribute string.")
    input(name: "existingTileFontSize", type: "string", title: "HTML Tile Font Size*", description: "Sets font scaling (1.0 = standard ~10px size, .85 = smaller text). Accepts values like 1.0, .85, or 85%.", defaultValue: "1.0")
    input(name: "existingTileHorzWordPos", type: "string", title: "HTML Word Position (left, right, center)", description: "Sets horizontal text alignment for tile entries.", defaultValue: "left")
    input(name: "existingTileFontColor", type: "string", title: "HTML Tile Text Color", description: "Sets default text color (supports named colors or 6/8 character Hex with leading #).", defaultValue: "white")
    input("revFill", "bool", title: "Reverse the fill order (Newest at bottom)", description: "Appends new notifications to bottom of tile instead of top.")
    input("preAdd", "bool", title: "Enable monospace PRE formatting for tile text", description: "Applies monospace pre-wrap CSS formatting to preserve alignment.")
    input("enableShadow", "bool", title: "Enable text shadow effects?", description: "Applies high-contrast drop shadow styling to tile text.", defaultValue: true)
    input("colorE", "text", title: "Color for [E] Emergency", description: "Sets text color for notifications tagged with [E] (Emergency).", defaultValue: "#FF0000")
    input("colorH", "text", title: "Color for [H] High", description: "Sets text color for notifications tagged with [H] (High priority).", defaultValue: "#FF6600")
    input("colorL", "text", title: "Color for [L] Low", description: "Sets text color for notifications tagged with [L] (Low priority).", defaultValue: "#FFD700")
    input("colorN", "text", title: "Color for [N] Normal", description: "Sets text color for notifications tagged with [N] (Normal priority).", defaultValue: "#2E7D32")
}

void installed() {
    state.msgCount = 0
    configure()
}

void updated(){
    if(logDebugEnable) runIn(1800, logsOff)
    configure()
}

void configure() {
    logDebug "configure()"
    
    state.msgList = []
    state.msgCount = 0

    String emptyMsg = "No notifications"
    String formattedEmpty = getTileStyles() + "<span class='last5'>${emptyMsg}</span>"

    sendIfChanged([name: "last5", value: formattedEmpty])
    sendIfChanged([name: "last5H", value: "** No notifications **"])
    
    sendIfChanged([name: "tileFontColor", value: existingTileFontColor ?: "white"])
    sendIfChanged([name: "tileHorzWordPos", value: existingTileHorzWordPos ?: "left"])
    sendIfChanged([name: "tileFontSize", value: parseFontSizeInput(existingTileFontSize).toString()])
}

// Helper to sanitize flexible user font size entries (.85, 0.85, 85%, etc.)
private BigDecimal parseFontSizeInput(Object inputVal) {
    if (inputVal == null) return 1.0
    String str = inputVal.toString().trim().replace("%", "")
    if (str.startsWith(".")) str = "0" + str
    
    try {
        BigDecimal parsed = str.toBigDecimal()
        // If user enters percentage like 85, scale it to 0.85
        if (parsed > 10.0) parsed = parsed / 100.0
        return parsed > 0 ? parsed : 1.0
    } catch (Exception e) {
        return 1.0
    }
}

String getTileStyles() {
    String alignment = existingTileHorzWordPos ?: "left"
    
    // Scale so input of 1.0 = ~10px (0.625em)
    BigDecimal userSize = parseFontSizeInput(existingTileFontSize)
    String fontSize = (userSize * 0.625).setScale(3, BigDecimal.ROUND_HALF_UP).toString()

    String fontColor = existingTileFontColor ?: "white"
    String preStyle = preAdd ? "white-space:pre-wrap;font-family:monospace;" : ""
    
    Boolean useShadow = settings.enableShadow != null ? settings.enableShadow : true
    String shadowCss = useShadow ? ".sb{text-shadow:0 2px 2px rgba(0,0,0,.8);}.sw{text-shadow:0 2px 2px rgba(255,255,255,.6);}" : ""

    String cE = settings.colorE ?: "#FF0000"
    String cH = settings.colorH ?: "#FF6600"
    String cL = settings.colorL ?: "#FFD700"
    String cN = settings.colorN ?: "#2E7D32"

    return "<style>.last5{display:block;${preStyle}text-align:${alignment};font-size:${fontSize}em;color:${fontColor};}.e{color:${cE}}.h{color:${cH}}.l{color:${cL}}.n{color:${cN}}${shadowCss}</style>"
}

def deviceNotification(String notification) {
    logDebug "deviceNotification entered: ${notification}"
    
    String rawInput = notification?.trim() ?: ""
    logInfo "Received notification entry: ${rawInput}"

    if(sdfPref == null) device.updateSetting("sdfPref",[value:"ddMMMyyyy HH:mm",type:"enum"])
    
    if (rawInput.length() > 800) {
        rawInput = rawInput.substring(0, 797) + "..."
    }

    String tag = rawInput.find(/\[[A-Z]+\]/)
    String cleanedMsg = rawInput.replaceFirst(/\[[A-Z]+\]/, '').trim()

    String timestamp = ""
    if (sdfPref != "None") {
        SimpleDateFormat sdf = new SimpleDateFormat(sdfPref)
        timestamp = sdf.format(new Date())
    }
    
    String msgWithTime
    if (timestamp) {
        msgWithTime = leadingDate ? "${timestamp} ${cleanedMsg}" : "${cleanedMsg} ${timestamp}"
    } else {
        msgWithTime = cleanedMsg
    }

    String colorized = colorizeNotification(tag, msgWithTime)
    
    if (state.msgList == null) state.msgList = []
    
    if (!revFill) state.msgList.add(0, colorized)
    else state.msgList.add(colorized)  

    int limit = (settings.msgLimit ?: 5).toInteger()
    while (state.msgList.size() > limit) {
        if (!revFill) state.msgList.removeAt(state.msgList.size() - 1) else state.msgList.removeAt(0)
    }

    String styleBlock = getTileStyles()
    int maxWkLength = 1020 - styleBlock.length() - 27
    if (maxWkLength < 100) maxWkLength = 100

    String wkTile = state.msgList.join("<br />")
    while (wkTile.length() > maxWkLength && state.msgList.size() > 1) {
        if (!revFill) state.msgList.removeAt(state.msgList.size() - 1) else state.msgList.removeAt(0)
        wkTile = state.msgList.join("<br />")
    }

    String finalOutput = styleBlock + "<span class='last5'>${wkTile}</span>"
    sendIfChanged([name: "last5", value: finalOutput])
    state.msgCount = state.msgList.size()

    if (settings.create5H) {
        sendIfChanged([name: "last5H", value: " ** " + wkTile.replaceAll("<br />"," ** ") + " ** "])
    }
}

def colorizeNotification(String tag, String body) {
    String icon = "" 
    String colorClass = ""
    String shadowClass = ""

    Boolean useShadow = settings.enableShadow != null ? settings.enableShadow : true

    switch(tag) {
        case "[E]":
            icon = "🚨"
            colorClass = "e"
            shadowClass = useShadow ? "sw" : ""
            break
        case "[H]":
            icon = "⚠️"
            colorClass = "h"
            shadowClass = useShadow ? "sb" : ""
            break
        case "[L]":
            icon = "🔋"
            colorClass = "l"
            shadowClass = useShadow ? "sb" : ""
            break
        case "[N]":
            icon = "ℹ️"
            colorClass = "n"
            shadowClass = useShadow ? "sw" : ""
            break
        default:
            icon = ""
            colorClass = ""
            shadowClass = useShadow ? "sw" : ""
            break
    }

    List classes = []
    if (colorClass) classes.add(colorClass)
    if (shadowClass) classes.add(shadowClass)

    String classAttr = classes ? "class='${classes.join(' ')}' " : ""
    String iconSpacer = icon ? "${icon} " : ""
    
    return "<span ${classAttr}>${iconSpacer}${body}</span>"
}

void logsOff(){
    device.updateSetting("logDebugEnable", [value: false, type: "bool"])
}

void push() {
    state.msgList = []
    configure()
}

// Custom Logging Functions
private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "Notification Tile Driver${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

// Custom sendEvent wrapper
private void sendIfChanged(Map args) {
    if (!args || !args.name) return
    
    String oldVal = device.currentValue(args.name as String)?.toString() ?: ""
    String newVal = args.value != null ? args.value.toString() : ""

    String cleanOld = oldVal.replaceAll(/&lt;/, "<").replaceAll(/&gt;/, ">")
    String cleanNew = newVal.replaceAll(/&lt;/, "<").replaceAll(/&gt;/, ">")

    if (cleanOld != cleanNew) {
        Map eventMap = [name: args.name, value: args.value, descriptionText: "Attribute ${args.name} changed"]
        if (args.unit) eventMap.unit = args.unit
        sendEvent(eventMap)
        logDebug "Event triggered: ${args.name} -> ${args.value}"
    }
}