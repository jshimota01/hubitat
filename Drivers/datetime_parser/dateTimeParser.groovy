/*
 * Date & Time Parser
 *
 * Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Change History:
 *
 * Date Source Version What URL
 * ---- ------ ------- ---- ---
 * 2022-01-20 jshimota 0.1.0 Starting version
 * 2021-09-30 Simon Burke 0.1.1 Used DateFormat app components https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
 *
 */

import java.text.SimpleDateFormat
import java.util.Date

//import java.time.Duration
//import java.time.ZoneOffset
//import java.time.ZonedDateTime
//import java.time.format.DateTimeFormatter
//import java.time.temporal.ChronoUnit

static String version() { return '0.1.1' }

metadata {
    definition(
            name: "Date & Time Parser",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/datetime_parser/dateTimeParser.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"

        attribute "DayOfMonNum", "number"
        attribute "DayText3", "string"
        attribute "DayOfMonNumNoLead", "number"
        attribute "DayName", "string"
        attribute "DayOfWeekNum", "number"
        //attribute "DayOfMonSuf", "string"
        attribute "DayOfYearNum", "number"
        attribute "WeekOfYearNum", "number"
        attribute "MonthName", "string"
        attribute "MonthNum", "number"
        attribute "MonthNameText3", "string"
        attribute "MonthNumNoLead", "number"
        //attribute "DaysInMonthNum", "number"
        //attribute "LeapBool","boolean"
        attribute "YearNum4Dig", "number"
        attribute "YearNum2Dig", "number"
        //attribute "TimeLowerAntePost", "string"
        attribute "TimeUpperAntePost", "string"
        //attribute "TimeHour12NumNoLead", "number"
        //attribute "TimeHour24NumNoLead", "number"
        attribute "TimeHour12Num", "number"
        attribute "TimeHour24Num", "number"
        attribute "TimeMinNum", "number"
        attribute "TZIDText3", "string"
        attribute "TZID", "string"
        //attribute "DSTBool","boolean"
        attribute "GMTDiffHours", "string"
        //attribute "TZText3", "string"

        attribute "tileFontSize", "number"
        attribute "tileFontColor", "string"
        attribute "tileVertWordPos", "number"

    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input(name: "existingTileFontSize", type: "num", title: "HTML Tile Font Size (%)*", defaultValue: 100)
    input(name: "existingTileVertWordPos", type: "num", title: "HTML Tile Word Position (%)*", defaultValue: 55)
    input(name: "existingTileFontColor", type: "string", title: "HTML Tile Text Color (Hex format with leading #)", defaultValue: "#FFFFFFFF")
    input("autoUpdate", "bool", title: "Enable automatic update every 5 mins?\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
    input("htmlVtile", "bool", title: "Use HTML attribute?\n(Enabled is Yes)")
}

def tileFontColor() {
    String tileFontColor = "#FFFFFFFF"
    if(existingTileFontColor > " ") tileFontColor = existingTileFontColor
    sendEvent(name: "Tile Font Color", value: "${tileFontColor}")
}

def tileVertWordPos() {
    tileVertWordPos = 55
    if(existingTileVertWordPos > " ") tileVertWordPos = existingTileVertWordPos
    sendEvent(name: "Tile Vertical Word Position", value: tileVertWordPos)
}

def tileFontSize() {
    tileFontSize = 100
    if(existingTileFontSize > " ") tileFontSize = existingTileFontSize
    sendEvent(name: "Tile Font Size", value: tileFontSize)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable}"
    log.warn "description logging is: ${txtEnable}"
    if (logEnable) {
        if (!autoUpdate)log.warn("Update: Automatic Update DISABLED")
    }
    if (logEnable) {
        if (autoUpdate)log.info("Update: Automatic Update ENABLED")
    }
    if (logEnable) runIn(1800, logsOff)
    if (logEnable) log.debug("autoupdate: Next scheduled refresh set")
    unschedule()
    refresh()
    schedUpdate()
}

def refresh() {
    tileFontColor()
    tileFontSize()
    tileVertWordPos()
    runCmd()
    schedUpdate()
}

def schedUpdate() {
    unschedule()
    if (txtEnable) log.info("schedUpdate: Refresh schedule cleared ...")
    if (autoUpdate) {
        if (txtEnable) log.info("Update: Setting next scheduled refresh...")
        if (autoUpdate) schedule("0 0/5 * 1/1 * ? *", refresh) // every 5 minutes
        if (autoUpdate) log.info("Update: Next scheduled refresh set")
    }
}

def runCmd() {
    now = new Date()
    // simpleDateFormatForMonth = new SimpleDateFormat('MMMM')
    dTDayOfMonNumPattern = new SimpleDateFormat('dd')
    dTDayText3Pattern = new SimpleDateFormat('EEE')
    dTDayOfMonNumNoLeadPattern = new SimpleDateFormat('d')
    dTDayNamePattern = new SimpleDateFormat('EEEE')
    dTDayOfWeekNumPattern = new SimpleDateFormat('u')
    //dTDayOfMonSufPattern = new SimpleDateFormat('s')
    dTDayOfYearNumPattern = new SimpleDateFormat('D')
    dTWeekOfYearNumPattern = new SimpleDateFormat('W')
    dTMonthNamePattern = new SimpleDateFormat('MMMM')
    dTMonthNumPattern = new SimpleDateFormat('MM')
    dTMonthNameText3Pattern = new SimpleDateFormat('MMM')
    dTMonthNumNoLeadPattern = new SimpleDateFormat('M')
    //dTDaysInMonthNumPattern = new SimpleDateFormat('t')
    //dTLeapBoolPattern = new SimpleDateFormat('L')
    dTYearNum4DigPattern = new SimpleDateFormat('yyyy')
    dTYearNum2DigPattern = new SimpleDateFormat('yy')
    //dTTimeLowerAntePostPattern = new SimpleDateFormat('q')
    dTTimeUpperAntePostPattern = new SimpleDateFormat('a')
    //dTTimeHour12NumNoLeadPattern = new SimpleDateFormat('g')
    //dTTimeHour24NumNoLeadPattern = new SimpleDateFormat('G')
    dTTimeHour12NumPattern = new SimpleDateFormat('h')
    dTTimeHour24NumPattern = new SimpleDateFormat('H')
    dTTimeMinNumPattern = new SimpleDateFormat('m')
    dTTZIDPattern = new SimpleDateFormat('zzzz')
    dTTZIDText3Pattern = new SimpleDateFormat('z')
    //dTDSTBoolPattern = new SimpleDateFormat('I')
    dTGMTDiffHoursPattern = new SimpleDateFormat('Z')
    //dTTZText3Pattern = new SimpleDateFormat('T')


    // proposedFormattedMonth = simpleDateFormatForMonth.format(now)
    DayOfMonNum = dTDayOfMonNumPattern.format(now)
    DayText3 = dTDayText3Pattern.format(now)
    DayOfMonNumNoLead = dTDayOfMonNumNoLeadPattern.format(now)
    DayName = dTDayNamePattern.format(now)
    DayOfWeekNum = dTDayOfWeekNumPattern.format(now)
    //DayOfMonSuf = dTDayOfMonSufPattern.format(now)
    DayOfYearNum = dTDayOfYearNumPattern.format(now)
    WeekOfYearNum = dTWeekOfYearNumPattern.format(now)
    MonthName = dTMonthNamePattern.format(now)
    MonthNum = dTMonthNumPattern.format(now)
    MonthNameText3 = dTMonthNameText3Pattern.format(now)
    MonthNumNoLead = dTMonthNumNoLeadPattern.format(now)
    //DaysInMonthNum = dTDaysInMonthNumPattern.format(now)
    //LeapBool = isLeapYear(now)
    YearNum4Dig = dTYearNum4DigPattern.format(now)
    YearNum2Dig = dTYearNum2DigPattern.format(now)
    //TimeLowerAntePost = dTTimeLowerAntePostPattern.format(now)
    TimeUpperAntePost = dTTimeUpperAntePostPattern.format(now)
    //TimeHour12NumNoLead = dTTimeHour12NumNoLeadPattern.format(now)
    //TimeHour24NumNoLead = dTTimeHour24NumNoLeadPattern.format(now)
    TimeHour12Num = dTTimeHour12NumPattern.format(now)
    TimeHour24Num = dTTimeHour24NumPattern.format(now)
    TimeMinNum = dTTimeMinNumPattern.format(now)
    TZID = dTTZIDPattern.format(now)
    TZIDText3 = dTTZIDText3Pattern.format(now)
    //DSTBool = dTDSTBoolPattern.format(now)
    GMTDiffHours = dTGMTDiffHoursPattern.format(now)
    //TZText3 = dTTZText3Pattern.format(now)

    //sendEvent(name: "todaysFormattedMonth", value: proposedFormattedMonth)
    sendEvent(name: "DayOfMonNum", value: DayOfMonNum)
    sendEvent(name: "DayText3", value: DayText3)
    sendEvent(name: "DayOfMonNumNoLead", value: DayOfMonNumNoLead)
    sendEvent(name: "DayName", value: DayName)
    sendEvent(name: "DayOfWeekNum", value: DayOfWeekNum)
    //sendEvent(name: "DayOfMonSuf", value: DayOfMonSuf)
    sendEvent(name: "DayOfYearNum", value: DayOfYearNum)
    sendEvent(name: "WeekOfYearNum", value: WeekOfYearNum)
    sendEvent(name: "MonthName", value: MonthName)
    sendEvent(name: "MonthNum", value: MonthNum)
    sendEvent(name: "MonthNameText3", value: MonthNameText3)
    sendEvent(name: "MonthNumNoLead", value: MonthNumNoLead)
    //sendEvent(name: "DaysInMonthNum", value: DaysInMonthNum)
    //sendEvent(name: "LeapBool", value: LeapBool)
    sendEvent(name: "YearNum4Dig", value: YearNum4Dig)
    sendEvent(name: "YearNum2Dig", value: YearNum2Dig)
    //sendEvent(name: "TimeLowerAntePost", value: TimeLowerAntePost)
    sendEvent(name: "TimeUpperAntePost", value: TimeUpperAntePost)
    //sendEvent(name: "TimeHour12NumNoLead", value: TimeHour12NumNoLead)
    //sendEvent(name: "TimeHour24NumNoLead", value: TimeHour24NumNoLead)
    sendEvent(name: "TimeHour12Num", value: TimeHour12Num)
    sendEvent(name: "TimeHour24Num", value: TimeHour24Num)
    sendEvent(name: "TimeMinNum", value: TimeMinNum)
    sendEvent(name: "TZID", value: TZID)
    sendEvent(name: "TZIDText3", value: TZIDText3)
    //sendEvent(name: "DSTBool", value: DSTBool)
    sendEvent(name: "GMTDiffHours", value: GMTDiffHours)
    //sendEvent(name: "TZText3", value: TZText3)
}