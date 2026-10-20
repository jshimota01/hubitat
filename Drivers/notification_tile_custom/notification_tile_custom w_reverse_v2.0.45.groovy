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
*    2026-08-23  jshimota      v2.0.41 Added once-per-day date separator header injection using Groovy native Date.format()
*    2026-08-26  jshimota      v2.0.42 Added HTML payload interceptor inside deviceNotification to unwrap block-level app tags and enforce inline timestamp placement
*    2026-10-17  jshimota      v2.0.43 Cleaned up log prefix formatting (Device Label; Message), decoupled msgList pruning from date headers, preserved notification history across preference updates, and implemented null-safe attribute comparison guards.
*    2026-10-20  jshimota      v2.0.44 Corrected HTML font scaling in getTileStyles() to use userSize directly without 0.625 scaling, and updated preference description.
*    2026-10-20  jshimota      v2.0.45 Decoupled configure() from destructive history resets to preserve notifications across updates, moving explicit reset logic strictly to push().
*/

import groovy.transform.Field
static String version()    {  return '2.0.45'  }

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
    input(name: "existingTileFontSize", type: "string", title: "HTML Tile Font Size*", description: "Sets font scaling (1.0 = 1.0em or 100% of inherited tile font size; values < 1.0 reduce size, > 1.0 increase size). Accepts values like 1.0, 0.85, or 85%.", defaultValue: "1.0")
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
    configure()
}

void updated() {
    if (logDebugEnable) runIn(1800, logsOff)
    logDebug "updated(): Refreshing preferences and re-rendering tile..."
    
    sendIfChanged([name: "tileFontColor", value: existingTileFontColor ?: "white"])
    sendIfChanged([name: "tileHorzWordPos", value: existingTileHorzWordPos ?: "left"])
    sendIfChanged([name: "tileFontSize", value: parseFontSizeInput(existingTileFontSize).toString()])
    
    // Re-render tile with existing history preserved
    renderTile()
}

void configure() {
    logDebug "configure(): Safe configuration executed"
    
    if (state.msgList == null) state.msgList = []
    if (state.msgCount == null) state.msgCount = state.msgList.size()

    sendIfChanged([name: "tileFontColor", value: existingTileFontColor ?: "white"])
    sendIfChanged([name: "tileHorzWordPos", value: existingTileHorzWordPos ?: "left"])
    sendIfChanged([name: "tileFontSize", value: parseFontSizeInput(existingTileFontSize).toString()])

    renderTile()
}

// Helper to sanitize flexible user font size entries (.85, 0.85, 85%, etc.)
private BigDecimal parseFontSizeInput(Object inputVal) {
    if (inputVal == null) return 1.0
    String str = inputVal.toString().trim().replace("%", "")
    if (str.startsWith(".")) str = "0" + str
    
    try {
        BigDecimal parsed = str.toBigDecimal()
        if (parsed > 10.0) parsed = parsed / 100.0
        return parsed > 0 ? parsed : 1.0
    } catch (Exception e) {
        return 1.0
    }
}

String getTileStyles() {
    String alignment = existingTileHorzWordPos ?: "left"
    
    BigDecimal userSize = parseFontSizeInput(existingTileFontSize)
    String fontSize = userSize.setScale(3, BigDecimal.ROUND_HALF_UP).toString()

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
    logInfo rawInput

    if (sdfPref == null) device.updateSetting("sdfPref", [value: "ddMMMyyyy HH:mm", type: "enum"])
    
    if (rawInput.length() > 800) {
        rawInput = rawInput.substring(0, 797) + "..."
    }

    String tag = rawInput.find(/\[[A-Z]+\]/)
    String cleanedMsg = rawInput.replaceFirst(/\[[A-Z]+\]/, '').trim()

    // --- HTML PAYLOAD INTERCEPTOR & UNWRAPPER ---
    // Safely unwrap outer div/span block elements while preserving inner content and multiline formatting
    if (cleanedMsg.matches(/(?si)^<(div|span)[^>]*>.*<\/(div|span)>$/)) {
        cleanedMsg = cleanedMsg.replaceAll(/(?si)^<(div|span)[^>]*>|<\/(div|span)>$/, '').trim()
    }

    String timestamp = ""
    if (sdfPref != "None") {
        timestamp = new Date().format(sdfPref, location.timeZone)
    }
    
    String msgWithTime
    if (timestamp) {
        msgWithTime = leadingDate ? "${timestamp} ${cleanedMsg}" : "${cleanedMsg} ${timestamp}"
    } else {
        msgWithTime = cleanedMsg
    }

    String colorized = colorizeNotification(tag, msgWithTime)
    
    if (state.msgList == null) state.msgList = []
    
    // --- ONCE-PER-DAY DATE HEADER LOGIC ---
    String currentDate = new Date().format("MM/dd", location.timeZone)
    
    if (state.lastDate != currentDate) {
        state.lastDate = currentDate
        String dateHeader = "<b>${currentDate}</b>"
        
        if (!revFill) {
            state.msgList.add(0, dateHeader)
        } else {
            state.msgList.add(dateHeader)
        }
    }

    // Add actual notification entry
    if (!revFill) {
        state.msgList.add(0, colorized)
    } else {
        state.msgList.add(colorized)
    }

    renderTile()
}

private void renderTile() {
    if (state.msgList == null || state.msgList.isEmpty()) {
        String emptyMsg = "No notifications"
        String formattedEmpty = getTileStyles() + "<span class='last5'>${emptyMsg}</span>"
        
        sendIfChanged([name: "last5", value: formattedEmpty])
        sendIfChanged([name: "last5H", value: "** No notifications **"])
        state.msgCount = 0
        return
    }

    int limit = (settings.msgLimit ?: 5).toInteger()
    
    // Helper closure to count non-header notification entries
    def countMessages = { list -> list.count { !it.toString().startsWith("<b>") } }

    // Prune entries exceeding msgLimit based on non-header message count
    while (countMessages(state.msgList) > limit) {
        if (!revFill) {
            state.msgList.removeAt(state.msgList.size() - 1)
        } else {
            state.msgList.removeAt(0)
        }
    }

    // Clean up trailing/leading orphaned date headers
    cleanOrphanedDateHeaders()

    String styleBlock = getTileStyles()
    int maxWkLength = 1020 - styleBlock.length() - 27
    if (maxWkLength < 100) maxWkLength = 100

    String wkTile = state.msgList.join("<br />")
    while (wkTile.length() > maxWkLength && countMessages(state.msgList) > 1) {
        if (!revFill) {
            state.msgList.removeAt(state.msgList.size() - 1)
        } else {
            state.msgList.removeAt(0)
        }
        cleanOrphanedDateHeaders()
        wkTile = state.msgList.join("<br />")
    }

    String finalOutput = styleBlock + "<span class='last5'>${wkTile}</span>"
    sendIfChanged([name: "last5", value: finalOutput])
    state.msgCount = state.msgList.size()

    if (settings.create5H) {
        sendIfChanged([name: "last5H", value: " ** " + wkTile.replaceAll("<br />", " ** ") + " ** "])
    }
}

private void cleanOrphanedDateHeaders() {
    if (state.msgList == null || state.msgList.isEmpty()) return

    // Remove orphaned date header at list boundaries if no message follows or precedes it
    if (!revFill) {
        while (!state.msgList.isEmpty() && state.msgList.last().toString().startsWith("<b>")) {
            state.msgList.removeAt(state.msgList.size() - 1)
        }
    } else {
        while (!state.msgList.isEmpty() && state.msgList.first().toString().startsWith("<b>")) {
            state.msgList.removeAt(0)
        }
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
    state.msgCount = 0
    state.lastDate = null
    renderTile()
}

// Custom Logging Functions
private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "${device.displayName}; ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

// Custom sendEvent wrapper with null-safe string guards
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