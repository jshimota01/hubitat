import java.text.SimpleDateFormat
static String version() { return '0.2.0' }

metadata {
    definition(
            name: "Meteorological Seasons",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/Seasons-beta.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
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
        attribute "hemisphereName", "String"
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input("autoUpdate", "bool", title: "Enable automatic update at 6am\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
    input("htmlVtile", "bool", title: "Use HTML attribute instead of seasonTile\n(Enabled is Yes)")
    input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names fall.svg, winter.svg, spring.svg, summer and unknown.svg)")
    input(name: "hemisphere", type: "bool", title: "Hemisphere", description: "On=Northern/Off=Southern", defaultValue: true, required: true, displayDuringSetup: true)
}

// table
//  VariableName        fallStart   fallEnd     winterStart     winterEnd       springStart springEnd   summerStart summerEnd
//  northPeriod         September 1 November 30 December 1      February 29     March 1      May 31     June 1      August 31
//  southPeriod         March 1     May 31      June 1          August 31       September 1  November 1 December 1  February 29

def hemisphereName() {
    if (!hemisphere) {
      sendEvent(name: "hemisphereName", value: "Southern", descriptionText: descriptionText)
    } else {
      sendEvent(name: "hemisphereName", value: "Northern", descriptionText: descriptionText)
    }
}

def installed() {
    refresh()
}

def refresh() {
    runCmd()
    currentSeason()
}

def runCmd() {
    now = new Date()
    simpleDateFormatForDate = new SimpleDateFormat('dd-MM-yyyy')
    simpleDateFormatForMonth = new SimpleDateFormat('MMMM')

    proposedFormattedDate = simpleDateFormatForDate.format(now)
    proposedFormattedMonth = simpleDateFormatForMonth.format(now)
    proposedHtmlFriendlyDate = "<span class=\"dateFormat\">${proposedFormattedDate}</span>"

    sendEvent(name: "todaysFormattedDate", value: proposedFormattedDate)
    sendEvent(name: "todaysFormattedMonth", value: proposedFormattedMonth)
    sendEvent(name: "todaysHtmlFriendlyDate", value: proposedHtmlFriendlyDate)
    currentSeason()
}

def currentSeason() {
    if (device.currentValue("todaysFormattedMonth") == (null)) {
        runCmd()
    } else if (device.currentValue("todaysFormattedMonth") == ("Not Initialized")) {
        seasonPreviouslySet = false
        existingSeasonName = "${seasonName}"
    } else {
        seasonPreviouslySet = true
        existingSeasonName = "${seasonName}"
    }
    if (device.currentValue("todaysFormattedMonth") == ("September") || device.currentValue("todaysFormattedMonth") == ("October") || device.currentValue("todaysFormattedMonth") == ("November")) {
        fall()
    } else if (device.currentValue("todaysFormattedMonth") == ("December") || device.currentValue("todaysFormattedMonth") == ("January") || device.currentValue("todaysFormattedMonth") == ("February")) {
        winter()
    } else if (device.currentValue("todaysFormattedMonth") == ("March") || device.currentValue("todaysFormattedMonth") == ("April") || device.currentValue("todaysFormattedMonth") == ("May")) {
        spring()
    } else if (device.currentValue("todaysFormattedMonth") == ("June") || device.currentValue("todaysFormattedMonth") == ("July") || device.currentValue("todaysFormattedMonth") == ("August")) {
        summer()
    } else {
        String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
        if (iconPathOvr > " ") iconPath = iconPathOvr
        sendEvent(name: "seasonName", value: "Not Initialized", descriptionText: descriptionText)
        sendEvent(name: "seasonNum", value: 0, descriptionText: descriptionText)
        sendEvent(name: "seasonBegin", value: "N/A", descriptionText: descriptionText)
        sendEvent(name: "seasonEnd", value: "N/A", descriptionText: descriptionText)
        sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;'><p class='small' style='text-align:center'>(Not Initialized)</p></img></div>", descriptionText: descriptionText)
        sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}unknown.svg' style='height: 100px;' />", descriptionText: descriptionText)
    }
}

def fall() {
    hemisphereName()
    def descriptionText = "Current season is now Fall" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    sendEvent(name: "seasonName", value: "Fall", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 1, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "September 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "March 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "November 30", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "May 31", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;'><p class='small' style='text-align:center'>Fall</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}fall.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def winter() {
    hemisphereName()
    def descriptionText = "Current season is now Winter" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    sendEvent(name: "seasonName", value: "Winter", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 2, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "December 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "June 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "February 29", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "August 31", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;'><p class='small' style='text-align:center'>Winter</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}winter.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def spring() {
    hemisphereName()
    def descriptionText = "Current season is now Spring" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    sendEvent(name: "seasonName", value: "Spring", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 3, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "March 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "September 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "May 31", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "November 30", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;'><p class='small' style='text-align:center'>Spring</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}spring.svg' style='height: 100px;' />", descriptionText: descriptionText)
}

def summer() {
    hemisphereName()
    def descriptionText = "Current season is now Summer" as Object
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/meteorological_seasons/season_icons/"
    if (iconPathOvr > " ") iconPath = iconPathOvr
    sendEvent(name: "seasonName", value: "Summer", descriptionText: descriptionText)
    sendEvent(name: "seasonNum", value: 4, descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonBegin", value: "June 1", descriptionText: descriptionText) else sendEvent(name: "seasonBegin", value: "December 1", descriptionText: descriptionText)
    if (hemisphere) sendEvent(name: "seasonEnd", value: "August 31", descriptionText: descriptionText) else sendEvent(name: "seasonEnd", value: "February 29", descriptionText: descriptionText)
    sendEvent(name: "seasonTile", value: "<div id='seasonTile'><img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;'><p class='small' style='text-align:center'>Summer</p></img></div>", descriptionText: descriptionText)
    sendEvent(name: "seasonImg", value: "<img class='seasonImg' src='${iconPath}summer.svg' style='height: 100px;' />", descriptionText: descriptionText)
}