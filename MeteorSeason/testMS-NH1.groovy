import java.text.SimpleDateFormat
static String version() { return '0.1.12' }

metadata {
    definition(
            name: "Meteorological Season of the Northern Hemisphere",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/Meteorical_Seasons/master/meteorSeason-NH.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        command "calcSeason", [[name: "rawDate", type: "STRING", description: "Enter date as (MM-dd-yyyy) to calculate the season for."]]
        command "currentSeason", ["$Cdate"]
        command "fall"
        command "winter"
        command "spring"
        command "summer"

        attribute "seasonName", "string"
        attribute "seasonNum", "number"
        attribute "seasonBegin", "string"
        attribute "seasonEnd", "string"
        attribute "seasonTile", "string"
        attribute "seasonImg", "string"
        attribute "todaysFormattedDate", "string"
        attribute "todaysFormattedMonth", "string"
        attribute "todaysHtmlFriendlyDate", "string"
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names fall.svg, winter.svg, spring.svg, summer and unknown.svg)")
    // input(name: "timeZone", type: "enum", title: "Select TimeZone:", description: "", multiple: false, required: true, defaultValue: getLocation().timeZone.getID(), options: TimeZone.getAvailableIDs())

   // input(name: "dateFormat", type: "string", title:"Date Format", description: "Enter the date format to apply for display purposes", defaultValue: "MM-dd-yyyy", required: true, displayDuringSetup: true)
   // input(name: "monthFormat", type: "string", title:"Month Format", description: "Enter the Month format to apply for display purposes", defaultValue: "MMMM", required: true, displayDuringSetup: true)
   // input(name: "timeFormat", type: "string", title:"Time Format", description: "Enter the time format to apply for display purposes", defaultValue: "HH:mm:ss.SSSZ", required: false, displayDuringSetup: true)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def installed() {
    log.trace "installed()"
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == false}"
    log.warn "description logging is: ${txtEnable == false}"
    updatePolling()
    if (logEnable) runIn(1800, logsOff)
}

def refresh() {
    runCmd()
    currentSeason()
}

def runCmd() {
    now = new Date()
    // selectedTimeZone = TimeZone.getTimeZone(timeZone)

    //simpleDateFormatForDate = new SimpleDateFormat(dateFormat)
    simpleDateFormatForDate = new SimpleDateFormat('dd-MM-yyyy')
    // simpleDateFormatForDate.setTimeZone(selectedTimeZone)

    simpleDateFormatForMonth = new SimpleDateFormat('MMMM')
    // simpleDateFormatForMonth.setTimeZone(selectedTimeZone)

    proposedFormattedDate = simpleDateFormatForDate.format(now)
    proposedFormattedMonth = simpleDateFormatForMonth.format(now)
    proposedHtmlFriendlyDate = "<span class=\"dateFormat\">${proposedFormattedDate}</span>"

    sendEvent(name: "todaysFormattedDate", value: proposedFormattedDate)
    sendEvent(name: "todaysFormattedMonth", value: proposedFormattedMonth)
    sendEvent(name: "todaysHtmlFriendlyDate", value: proposedHtmlFriendlyDate)
}

def parse(String description) {
}

def currentSeason() {
    runCmd()
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    if (device.currentValue("todaysFormattedMonth") == ("September")) {
        fall()
    } else if (device.currentValue("todaysFormattedMonth") == ("October")) {
        fall()
    } else if (device.currentValue("todaysFormattedMonth") == ("November")) {
        fall()
    } else if (device.currentValue("todaysFormattedMonth") == ("December")) {
        winter()
    } else if (device.currentValue("todaysFormattedMonth") == ("January")) {
        winter()
    } else if (device.currentValue("todaysFormattedMonth") == ("February")) {
        winter()
    } else if (device.currentValue("todaysFormattedMonth") == ("March")) {
        spring()
    } else if (device.currentValue("todaysFormattedMonth") == ("April")) {
        spring()
    } else if (device.currentValue("todaysFormattedMonth") == ("May")) {
        spring()
    } else if (device.currentValue("todaysFormattedMonth") == ("June")) {
        summer()
    } else if (device.currentValue("todaysFormattedMonth") == ("July")) {
        summer()
    } else if (device.currentValue("todaysFormattedMonth") == ("August")) {
        summer()
    } else {
        sendEvent(name: "todaysFormattedDate", value: proposedFormattedDate)
        sendEvent(name: "todaysFormattedMonth", value: proposedFormattedMonth)
        sendEvent(name: "todaysHtmlFriendlyDate", value: proposedHtmlFriendlyDate)
        sendEvent(name: "seasonName", value: "Not Initialized", descriptionText: descriptionText)
        sendEvent(name: "seasonNum", value: 0, descriptionText: descriptionText)
        sendEvent(name: "seasonBegin", value: "N/A", descriptionText: descriptionText)
        sendEvent(name: "seasonEnd", value: "N/A", descriptionText: descriptionText)
        sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;'><p class='small' style='text-align:center'>(Not Initialized)</p></img></div>", descriptionText: descriptionText)
        sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;' />", descriptionText: descriptionText)
    }
}

def fall() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Fall", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 1, descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "September 1", descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "November 30", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;'><p class='small' style='text-align:center'>Fall</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def winter() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Winter", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 2, descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "December 1", descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "February 28", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;'><p class='small' style='text-align:center'>Winter</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def spring() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Spring", descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "March 1", descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "May 31", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 3, descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;'><p class='small' style='text-align:center'>Spring</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def summer() {
    def descriptionText = "Current season is ${device.displayName}" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "seasonName", value: "Summer", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 4, descriptionText: descriptionText)
    sendEvent(name: "seasonBegin", value: "June 1", descriptionText: descriptionText)
    sendEvent(name: "seasonEnd", value: "August 31", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;'><p class='small' style='text-align:center'>Summer</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;' />", descriptionText: descriptionText)
}