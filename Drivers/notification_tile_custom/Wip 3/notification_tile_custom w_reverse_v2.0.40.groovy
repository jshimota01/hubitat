void configure() {
    logDebug "configure()"
    
    state.msgList = []
    state.msgCount = 0
    state.lastDate = null // Reset so the next incoming message will trigger the date line

    String emptyMsg = "No notifications"
    String formattedEmpty = getTileStyles() + "<span class='last5'>${emptyMsg}</span>"

    sendIfChanged([name: "last5", value: formattedEmpty])
    sendIfChanged([name: "last5H", value: "** No notifications **"])
    
    sendIfChanged([name: "tileFontColor", value: existingTileFontColor ?: "white"])
    sendIfChanged([name: "tileHorzWordPos", value: existingTileHorzWordPos ?: "left"])
    sendIfChanged([name: "tileFontSize", value: parseFontSizeInput(existingTileFontSize).toString()])
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
    
    // --- ONCE-PER-DAY DATE HEADER LOGIC ---
    String currentDate = new SimpleDateFormat("MM/dd").format(new Date())
    
    if (state.lastDate != currentDate) {
        state.lastDate = currentDate // Locks out subsequent runs for today
        String dateHeader = "<b>${currentDate}</b>"
        
        if (!revFill) {
            // Newest at top: Add date header first, then the message above it
            state.msgList.add(0, dateHeader)
        } else {
            // Newest at bottom: Add date header before the new message
            state.msgList.add(dateHeader)
        }
    }

    // Add actual notification entry
    if (!revFill) {
        state.msgList.add(0, colorized)
    } else {
        state.msgList.add(colorized)
    }

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