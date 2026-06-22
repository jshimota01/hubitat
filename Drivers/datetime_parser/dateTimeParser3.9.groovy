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
 * Date         Source      Version What                                        URL
 * ----         ------      ------- ----                                        ---
 * 2022-01-19   jshimota    0.1.0   Starting version
 * 2021-01-19   Simon Burke 0.1.1   Used 2021-09-30 DateFormat app components   https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
 * 2022-01-19   jshimota    0.1.2   Alpha release for testing
 * 2022-01-20   jshimota    0.1.3   Worked on Scheduling cleanup and logging
 * 2022-01-20   jshimota    0.1.4   First efforts to identify workarounds on php variations not found in Java
 * 2022-01-20   jshimota    0.1.5   Heavy work done on basic function cleanup, as well as optimization
 * 2022-01-20   jshimota    0.1.6   Added final missing attributes - DST, ObservesDST, LeapYear, Day Suffix and Ordinal
 * 2022-01-20   jshimota    0.1.7   Tried adding Simons time and date stuff back, changed mind
 * 2022-01-20   jshimota    0.1.8   Added update schedule ability
 * 2022-01-20   jshimota    0.1.9   Commented tile features completely - no intent to support
 * 2022-01-20   jshimota    0.2.0   Release (getting HPM value for package)
 * 2022-01-20   jshimota    0.2.1   Added user compare value requests
 * 2022-01-21   jshimota    0.2.2   Fixed switch case for Suffix, added Nolead to minutes var, scheduler drop down and values
 * 2022-01-22   jshimota    0.2.3   Added WeekOfYearOdd/Even for garbage cans.
 * 2022-01-22   jshimota    0.2.4   with SBurke help - fixed booleans not supported by HE on comparators
 * 2022-01-22   jshimota    0.2.5   Add of Even/Odd value to day of month number variables
 * 2022-01-22   jshimota    0.2.6   Add of Even/Odd value to day of year number variables
 * 2022-01-23   jshimota    0.2.7   TimeHour24NumNoLead fixed - added debug logging check to a line
 * 2022-01-26   jshimota    0.2.8   Added String versions of comparison date times for user
 * 2022-04-30   jshimota    0.2.9   2 minor text changes for clarity, attempt to fix schedule loop
 * 2022-08-12   jshimota    0.3.0   Week of Year was case sensitive and showing week of month, added week of month as well
 * 2022-08-15   jshimota    0.3.1   Typo error found in Week of Mon variables
 * 2025-03-14   jshimota    0.3.2   Added daily schedule run time to after HE and DST changes (2:45am)
 * 2025-10-19   jshimota    0.3.3   Added debug log - restructured log reporting
 * 2025-10-20   jshimota    0.3.4   added dailyRefresh
 * 2026-01-10   jshimota	0.3.5	changed log dbg and txt params for debugging
 * 2026-03-08   jshimota    0.3.6   Moved daily schedule to 3:15 to get past Hub hour update on DST
 * 2026-05-01   jshimota    0.3.7   Bug on 176 log enabled should be txtEnable
 * 2026-05-14   jshimota    0.3.8   fix package json to required true
 * 2026-06-22   jshimota    0.3.9   Gemini Optimization-Modernized refactor 2026.
 */

import java.text.SimpleDateFormat

static String version() { return '0.3.9' }

static String getOrdinal(int n) {
    if (n >= 11 && n <= 13) return "th"
    switch (n % 10) {
        case 1:  return "st"
        case 2:  return "nd"
        case 3:  return "rd"
        default: return "th"
    }
}

metadata {
    definition(
            name: "Date & Time Parser",
            namespace: "jshimota",
            author: "James Shimota",
            importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/datetime_parser/dateTimeParser.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"

        attribute "DayName", "string"
        attribute "DayNameText3", "string"
        attribute "DayOfMonNum", "number"
        attribute "DayOfMonNumNoLead", "number"
        attribute "DayOfWeekNum", "number"
        attribute "DayOfYearNum", "number"
        attribute "DaysInMonthNum", "number"
        attribute "DayOfMonOrd", "string"
        attribute "DayOfMonSuf", "string"
        attribute "GMTDiffHours", "string"
        attribute "IsDayOfMonNumEven", "string"
        attribute "IsDayOfMonNumOdd", "string"
        attribute "IsDayOfYearNumEven", "string"
        attribute "IsDayOfYearNumOdd", "string"
        attribute "IsDSTActive", "string"
        attribute "IsLeapYear", "string"
        attribute "IsObservesDST", "string"
        attribute "IsWeekOfMonNumEven", "string"
        attribute "IsWeekOfMonNumOdd", "string"
        attribute "IsWeekOfYearNumEven", "string"
        attribute "IsWeekOfYearNumOdd", "string"
        attribute "MonthName", "string"
        attribute "MonthNameText3", "string"
        attribute "MonthNum", "number"
        attribute "MonthNumNoLead", "number"
        attribute "TZID", "string"
        attribute "TZIDText3", "string"
        attribute "TimeAntePostLower", "string"
        attribute "TimeAntePostUpper", "string"
        attribute "TimeHour12Num", "number"
        attribute "TimeHour12NumNoLead", "number"
        attribute "TimeHour24Num", "number"
        attribute "TimeHour24NumNoLead", "number"
        attribute "TimeMinNum", "number"
        attribute "TimeMinNumNoLead", "number"
        attribute "WeekOfMonNum", "number"
        attribute "WeekOfYearNum", "number"
        attribute "YearNum2Dig", "number"
        attribute "YearNum4Dig", "number"
        attribute "comparisonDate", "number"
        attribute "comparisonDateStr", "string"
        attribute "comparisonDateTime", "number"
        attribute "comparisonDateTimeStr", "string"
        attribute "comparisonTime", "number"
        attribute "comparisonTimeStr", "string"

        command "scheduleRefresh"
    }
}

preferences {
    input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input name: "dbgEnable", type: "bool", title: "Enable debugText logging", defaultValue: false
    input name: "autoUpdate", type: "bool", title: "Enable automatic update?", defaultValue: true, required: true
    input name: "autoUpdateInterval", type: "enum", options: [[1:"1 minute"],[2:"2 minutes"],[5:"5 minutes"],[10:"10 minutes"],[15:"15 minutes"],[20:"20 minutes"],[30:"30 minutes"],[45:"45 minutes"],[59:"59 minutes"]], title: "Auto Update Interval", defaultValue: 5, required: true
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}

def updated() {
    if (dbgEnable) log.debug("updated: Beginning")
    if (dbgEnable) runIn(1800, logsOff)
    refresh()
}

def refresh() {
    if (dbgEnable) log.debug("refresh: Beginning")
    runCmd()
    manageSchedules()
}

def dailyRefresh() {
    if (dbgEnable) log.debug("dailyRefresh: Beginning")
    runCmd()
    manageSchedules()
}

def scheduleRefresh() {
    if (dbgEnable) log.debug("scheduleRefresh: Beginning")
    manageSchedules()
}

private def manageSchedules() {
    unschedule()
    if (txtEnable) log.info("Refresh: Cleared all updates scheduled ...")
    
    if (autoUpdate) {
        runIn(1, mySchedule)
        if (txtEnable) log.info("schedUpdate: Set periodic scheduled refresh with ${autoUpdateInterval} minute interval")
    } else {
        if (txtEnable) log.info("Refresh: Automatic Update DISABLED")
    }
    runIn(1, dailySchedule)
    if (txtEnable) log.info("schedDailyUpdate: Setting DAILY schedule at 3:15am each day")
}

def mySchedule() {
    schedule("0 0/${autoUpdateInterval} * ? * * *", "refresh")
}

def dailySchedule() {
    schedule("0 15 3 ? * * *", "dailyRefresh")
}

def runCmd() {
    def now = new Date()
    
    // Reusable formatter to drastically cut down memory allocation
    def sdf = new SimpleDateFormat()

    sdf.applyPattern('EEEE'); def DayName = sdf.format(now)
    sdf.applyPattern('EEE');  def DayNameText3 = sdf.format(now)
    sdf.applyPattern('dd');   def DayOfMonNum = sdf.format(now)
    sdf.applyPattern('d');    def DayOfMonNumNoLead = sdf.format(now)
    sdf.applyPattern('u');    def DayOfWeekNum = sdf.format(now)
    sdf.applyPattern('D');    def DayOfYearNum = sdf.format(now)
    sdf.applyPattern('W');    def WeekOfMonNum = sdf.format(now)
    sdf.applyPattern('w');    def WeekOfYearNum = sdf.format(now)
    sdf.applyPattern('MMMM'); def MonthName = sdf.format(now)
    sdf.applyPattern('MMM');  def MonthNameText3 = sdf.format(now)
    sdf.applyPattern('MM');   def MonthNum = sdf.format(now)
    sdf.applyPattern('M');    def MonthNumNoLead = sdf.format(now)
    sdf.applyPattern('yyyy'); def YearNum4Dig = sdf.format(now)
    sdf.applyPattern('yy');   def YearNum2Dig = sdf.format(now)
    sdf.applyPattern('hh');   def TimeHour12Num = sdf.format(now)
    sdf.applyPattern('HH');   def TimeHour24Num = sdf.format(now)
    sdf.applyPattern('h');    def TimeHour12NumNoLead = sdf.format(now)
    sdf.applyPattern('H');    def TimeHour24NumNoLead = sdf.format(now)
    sdf.applyPattern('mm');   def TimeMinNum = sdf.format(now)
    sdf.applyPattern('m');    def TimeMinNumNoLead = sdf.format(now)
    sdf.applyPattern('a');    def TimeAntePostUpper = sdf.format(now)
    def TimeAntePostLower = TimeAntePostUpper.toLowerCase()
    sdf.applyPattern('zzzz'); def TZID = sdf.format(now)
    sdf.applyPattern('z');    def TZIDText3 = sdf.format(now)
    sdf.applyPattern('Z');    def GMTDiffHours = sdf.format(now)

    def comparisonDate = "${YearNum4Dig}${MonthNum}${DayOfMonNum}".toInteger()
    def comparisonTime = "${TimeHour24Num}${TimeMinNum}".toInteger()
    def comparisonDateTime = "${YearNum4Dig}${MonthNum}${DayOfMonNum}${TimeHour24Num}${TimeMinNum}".toLong()

    int iYear = YearNum4Dig.toInteger()
    int iMonth = MonthNum.toInteger() - 1 
    int iDay = DayOfMonNum.toInteger()
    
    def currentCal = new GregorianCalendar(iYear, iMonth, iDay)
    def DaysInMonthNum = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    def LeapYearBool = currentCal.isLeapYear(iYear)

    def timezonedefault = TimeZone.getDefault()
    def ObservesDST = timezonedefault.observesDaylightTime()
    def DSTActiveBool = timezonedefault.inDaylightTime(now)

    // Simplified Boolean Eval & Scope Protections
    def DayOfMonNumEven = (DayOfMonNum.toInteger() % 2 == 0)
    def DayOfYearNumEven = (DayOfYearNum.toInteger() % 2 == 0)
    def WeekOfMonNumEven = (WeekOfMonNum.toInteger() % 2 == 0)
    def WeekOfYearNumEven = (WeekOfYearNum.toInteger() % 2 == 0)

    def OrdDay = getOrdinal(iDay)
    def DayOfMonSuf = OrdDay
    def DayOfMonOrd = "${iDay}${OrdDay}"

    // Output Event Parsing Map
    def events = [
        "DayName": DayName, "DayNameText3": DayNameText3, "DayOfMonNum": DayOfMonNum.toInteger(),
        "DayOfMonNumNoLead": DayOfMonNumNoLead.toInteger(), "DayOfMonOrd": DayOfMonOrd, "DayOfMonSuf": DayOfMonSuf,
        "DayOfWeekNum": DayOfWeekNum.toInteger(), "DayOfYearNum": DayOfYearNum.toInteger(), "DaysInMonthNum": DaysInMonthNum,
        "GMTDiffHours": GMTDiffHours, "IsDayOfMonNumEven": DayOfMonNumEven.toString(), "IsDayOfMonNumOdd": (!DayOfMonNumEven).toString(),
        "IsDayOfYearNumEven": DayOfYearNumEven.toString(), "IsDayOfYearNumOdd": (!DayOfYearNumEven).toString(),
        "IsDSTActive": DSTActiveBool.toString(), "IsLeapYear": LeapYearBool.toString(), "IsObservesDST": ObservesDST.toString(),
        "IsWeekOfMonNumEven": WeekOfMonNumEven.toString(), "IsWeekOfMonNumOdd": (!WeekOfMonNumEven).toString(),
        "IsWeekOfYearNumEven": WeekOfYearNumEven.toString(), "IsWeekOfYearNumOdd": (!WeekOfYearNumEven).toString(),
        "MonthName": MonthName, "MonthNameText3": MonthNameText3, "MonthNum": MonthNum.toInteger(),
        "MonthNumNoLead": MonthNumNoLead.toInteger(), "TZID": TZID, "TZIDText3": TZIDText3,
        "TimeAntePostLower": TimeAntePostLower, "TimeAntePostUpper": TimeAntePostUpper, "TimeHour12Num": TimeHour12Num.toInteger(),
        "TimeHour12NumNoLead": TimeHour12NumNoLead.toInteger(), "TimeHour24Num": TimeHour24Num.toInteger(),
        "TimeHour24NumNoLead": TimeHour24NumNoLead.toInteger(), "TimeMinNum": TimeMinNum.toInteger(),
        "TimeMinNumNoLead": TimeMinNumNoLead.toInteger(), "WeekOfMonNum": WeekOfMonNum.toInteger(),
        "WeekOfYearNum": WeekOfYearNum.toInteger(), "YearNum2Dig": YearNum2Dig.toInteger(), "YearNum4Dig": YearNum4Dig.toInteger(),
        "comparisonDate": comparisonDate, "comparisonDateStr": comparisonDate.toString(),
        "comparisonDateTime": comparisonDateTime, "comparisonDateTimeStr": comparisonDateTime.toString(),
        "comparisonTime": comparisonTime, "comparisonTimeStr": comparisonTime.toString()
    ]

    events.each { name, val -> sendEvent(name: name, value: val) }
}