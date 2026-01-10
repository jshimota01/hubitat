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
 *
 */

import java.text.SimpleDateFormat
/* import  java.lang.Object */

/* import java.util.Date
 * import java.util.Locale
 */

static String version() { return '0.3.5' }

static String getOrdinal(int n) {
    if (n >= 11 && n <= 13) {
        return "th"
    }
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

    input("autoUpdate", "bool", title: "Enable automatic update?\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
    input(name: "autoUpdateInterval", type: "enum", multiple: false, options: [[1:"1 minute"],[2:"2 minutes"],[5:"5 minutes"],[10:"10 minutes"],[15:"15 minutes"],[20:"20 minutes"],[30:"30 minutes"],[45:"45 minutes"],[59:"59 minutes"]], title: "Auto Update Interval", description: "Number of minutes (range 0-59) between automatic updates", defaultValue: 5, required: true, displayDuringSetup: true)
}


def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("dbgEnable", [value: "false", type: "bool"])
}

def updated() {
    if (dbgEnable) log.debug("updated: Beginning")
    log.info "updated..."
    log.warn "debug logging is: ${dbgEnable}"
    log.warn "description logging is: ${txtEnable}"
    if (dbgEnable) runIn(1800, logsOff)
    refresh()
}

def refresh() {
    if (dbgEnable) log.debug("refresh: Beginning")
    runCmd()
    if (txtEnable) {
        if (!autoUpdate) {
            if (txtEnable) log.info("Refresh: Automatic Update DISABLED")
        } else {
            if (txtEnable) log.info("Refresh: Automatic Update ENABLED")
        }
    }
    if (autoUpdate) {
        if (txtEnable) log.info("Refresh: Cleared all updates scheduled ...")
        schedUpdate()
        schedDailyUpdate()
    } else {
        unschedule()
        if (txtEnable) log.info("Refresh: Cleared all updates scheduled ...")
        schedDailyUpdate()
    }
    return
}

def dailyRefresh() {
    if (dbgEnable) log.debug("dailyRefresh: Beginning")
    runCmd()
    if (logEnable) {
        if (!autoUpdate) {
            if (txtEnable) log.info("Refresh: Automatic Update DISABLED")
        } else {
            if (txtEnable) log.info("Refresh: Automatic Update ENABLED")
        }
    }
    if (autoUpdate) {
        unschedule()
        if (txtEnable) log.info("Refresh: Cleared all updates scheduled ...")
        schedUpdate()
        schedDailyUpdate()
    } else {
        unschedule()
        if (txtEnable) log.info("Refresh: Cleared all updates scheduled ...")
        schedDailyUpdate()
    }
    return
}

def scheduleRefresh() {
    if (dbgEnable) log.debug("scheduleRefresh: Beginning")
    if (autoUpdate) {
        unschedule()
        if (txtEnable) log.info("Refresh: Cleared all updates scheduled ...")
        schedUpdate()
        schedDailyUpdate()
    } else {
        unschedule()
        if (txtEnable) log.info("Refresh: Cleared all updates scheduled ...")
        schedDailyUpdate()
    }
    return
}

def schedDailyUpdate() {
    if (dbgEnable) log.debug("schedDailyUpdate: Beginning")
    runIn(1,dailySchedule)
    if (txtEnable) log.info("schedDailyUpdate: Setting DAILY schedule at 2:45am each day")
    return
}

def schedUpdate() {
    if (dbgEnable) log.debug("schedUpdate: Beginning")
    if (dbgEnable) log.debug("schedUpdate: Setting new schedule ...")
    if (autoUpdate) {
        runIn(1,mySchedule)
        if (txtEnable) log.info("schedUpdate: Set periodic scheduled refresh with ${autoUpdateInterval} minute interval")
        return
    }
    return
}

def mySchedule() {
    if (dbgEnable) log.debug("mySchedule: Beginning")
    if (dbgEnable) log.debug("mySchedule: Setting new refresh interval to ${autoUpdateInterval} ...")
    schedule("0 0/${autoUpdateInterval} * ? * * *", "refresh")  //default 5 mins
}

def dailySchedule() {
    if (dbgEnable) log.debug("dailySchedule: Beginning")
    if (dbgEnable) log.debug("dailySchedule: Setting new dailyRefresh job for 2:45am ...")
    schedule("0 45 2 ? * * *", "dailyRefresh")  // hard set at 2:45am
}

def runCmd() {
    now = new Date()

    // pattern definitions
    dTDayNamePattern = new SimpleDateFormat('EEEE')
    dTDayNameText3Pattern = new SimpleDateFormat('EEE')
    dTDayOfMonNumPattern = new SimpleDateFormat('dd')
    dTDayOfMonNumNoLeadPattern = new SimpleDateFormat('d')
    dTDayOfWeekNumPattern = new SimpleDateFormat('u')
    dTDayOfYearNumPattern = new SimpleDateFormat('D')
    dTDaysInMonthNumPattern = new SimpleDateFormat('MMMM')
    dTMonthNamePattern = new SimpleDateFormat('MMMM')
    dTMonthNameText3Pattern = new SimpleDateFormat('MMM')
    dTMonthNumPattern = new SimpleDateFormat('MM')
    dTMonthNumNoLeadPattern = new SimpleDateFormat('M')
    dTYearNum4DigPattern = new SimpleDateFormat('yyyy')
    dTYearNum2DigPattern = new SimpleDateFormat('yy')
    dTTimeHour12NumPattern = new SimpleDateFormat('hh')
    dTTimeHour24NumPattern = new SimpleDateFormat('HH')
    dTTimeHour12NumNoLeadPattern = new SimpleDateFormat('h')
    dTTimeHour24NumNoLeadPattern = new SimpleDateFormat('H')
    dTTimeMinNumNoLeadPattern = new SimpleDateFormat('m')
    dTTimeMinNumPattern = new SimpleDateFormat('mm')
    dTTZIDPattern = new SimpleDateFormat('zzzz')
    dTTZIDText3Pattern = new SimpleDateFormat('z')
    dTGMTDiffHoursPattern = new SimpleDateFormat('Z')
    dTTimeAntePostUpperPattern = new SimpleDateFormat('a')
    dTTimeAntePostLowerPattern = new SimpleDateFormat('a') //drop to lower case using temp value
    dTWeekOfMonNumPattern = new SimpleDateFormat('W')
    dTWeekOfYearNumPattern = new SimpleDateFormat('w')

    // set attribute using pattern
    DayName = dTDayNamePattern.format(now)
    DayNameText3 = dTDayNameText3Pattern.format(now)
    DayOfMonNum = dTDayOfMonNumPattern.format(now)
    DayOfMonNumNoLead = dTDayOfMonNumNoLeadPattern.format(now)
    DayOfWeekNum = dTDayOfWeekNumPattern.format(now)
    DayOfYearNum = dTDayOfYearNumPattern.format(now)
    WeekOfMonNum = dTWeekOfMonNumPattern.format(now)
    WeekOfYearNum = dTWeekOfYearNumPattern.format(now)
    MonthName = dTMonthNamePattern.format(now)
    MonthNameText3 = dTMonthNameText3Pattern.format(now)
    MonthNum = dTMonthNumPattern.format(now)
    MonthNumNoLead = dTMonthNumNoLeadPattern.format(now)
    YearNum4Dig = dTYearNum4DigPattern.format(now)
    YearNum2Dig = dTYearNum2DigPattern.format(now)
    TimeHour12Num = dTTimeHour12NumPattern.format(now)
    TimeHour24Num = dTTimeHour24NumPattern.format(now)
    TimeHour12NumNoLead = dTTimeHour12NumNoLeadPattern.format(now)
    TimeHour24NumNoLead = dTTimeHour24NumNoLeadPattern.format(now)
    TimeMinNum = dTTimeMinNumPattern.format(now)
    TimeMinNumNoLead = dTTimeMinNumNoLeadPattern.format(now)
    TimeAntePostUpper = dTTimeAntePostUpperPattern.format(now)
    TimeAntePostLowerTmp = dTTimeAntePostLowerPattern.format(now)
    TimeAntePostLower = TimeAntePostLowerTmp.toLowerCase()
    TZID = dTTZIDPattern.format(now)
    TZIDText3 = dTTZIDText3Pattern.format(now)
    GMTDiffHours = dTGMTDiffHoursPattern.format(now)

    // set attribute using non-pattern
    comparisonDate = YearNum4Dig + MonthNum + DayOfMonNum
    comparisonTime = TimeHour24Num + TimeMinNum
    comparisonDateTime = YearNum4Dig + MonthNum + DayOfMonNum + TimeHour24Num + TimeMinNum

    // Leap Year
    int iYear = Integer.parseInt(YearNum4Dig)
    int iMonth = Integer.parseInt(MonthNum) - 1 // 1 (months begin with 0)
    int iDay = Integer.parseInt(DayOfMonNum)
    Calendar currentCal = new GregorianCalendar(iYear, iMonth, iDay) // used to check boolean
    DaysInMonthNum = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH) // 2
    LeapYearBool = currentCal.isLeapYear(Calendar.YEAR)

    //DST - Observed and if Enabled
    // check if it has DST now or in the future (doesn't check the past)
    TimeZone timezonedefault = TimeZone.getDefault()
    ObservesDST = timezonedefault.observesDaylightTime()
    DSTActiveBool = timezonedefault.inDaylightTime(now)

    //DayOfMonNum odd or even
    // DayOfMonNum = 6 // test case
    int iDayOfMonNum =  Integer.parseInt(DayOfMonNum)
    if (iDayOfMonNum % 2 == 0 ) {
        DayOfMonNumEven = true
        DayOfMonNumOdd = false
    } else {
        DayOfMonNumEven = false
        DayOfMonNumOdd = true
    }

    //DayOfYearNum odd or even
    // DayOfYearNum = 6 // test case
    int iDayOfYearNum =  Integer.parseInt(DayOfYearNum)
    if (iDayOfYearNum % 2 == 0 ) {
        DayOfYearNumEven = true
        DayOfYearNumOdd = false
    } else {
        DayOfYearNumEven = false
        DayOfYearNumOdd = true
    }

    //WeekOfMonNum odd or even
    // WeekOfMonNum = 3 // test case
    int iWeekOfMonNum =  Integer.parseInt(WeekOfMonNum)
    if (iWeekOfMonNum % 2 == 0 ) {
        WeekOfMonNumEven = true
        WeekOfMonNumOdd = false
    } else {
        WeekOfMonNumEven = false
        WeekOfMonNumOdd = true
    }

    //WeekOfYearNum odd or even
    // WeekOfYearNum = 6 // test case
    int iWeekOfYearNum =  Integer.parseInt(WeekOfYearNum)
    if (iWeekOfYearNum % 2 == 0 ) {
        WeekOfYearNumEven = true
        WeekOfYearNumOdd = false
    } else {
        WeekOfYearNumEven = false
        WeekOfYearNumOdd = true
    }

    // Ordinals
    OrdDay = getOrdinal(iDay)
    DayOfMonSuf = OrdDay
    DayOfMonOrd = String.valueOf(iDay) + OrdDay

    //convert all booleans to text strings
    IsDayOfMonNumOdd = String.valueOf(DayOfMonNumOdd)
    IsDayOfMonNumEven = String.valueOf(DayOfMonNumEven)
    IsDayOfYearNumOdd = String.valueOf(DayOfYearNumOdd)
    IsDayOfYearNumEven = String.valueOf(DayOfYearNumEven)
    IsDSTActive = String.valueOf(DSTActiveBool)
    IsLeapYear = String.valueOf(LeapYearBool)
    IsWeekOfMonNumOdd = String.valueOf(WeekOfMonNumOdd)
    IsWeekOfMonNumEven = String.valueOf(WeekOfMonNumEven)
    IsWeekOfYearNumOdd = String.valueOf(WeekOfYearNumOdd)
    IsWeekOfYearNumEven = String.valueOf(WeekOfYearNumEven)
    IsObservesDST = String.valueOf(ObservesDST)

    //convert comparison number fields to strings
    comparisonDateStr = String.valueOf(comparisonDate)
    comparisonTimeStr = String.valueOf(comparisonTime)
    comparisonDateTimeStr = String.valueOf(comparisonDateTime)


    sendEvent(name: "DayName", value: DayName)
    sendEvent(name: "DayNameText3", value: DayNameText3)
    sendEvent(name: "DayOfMonNum", value: DayOfMonNum)
    sendEvent(name: "DayOfMonNumNoLead", value: DayOfMonNumNoLead)
    sendEvent(name: "DayOfMonOrd", value: DayOfMonOrd)
    sendEvent(name: "DayOfMonSuf", value: DayOfMonSuf)
    sendEvent(name: "DayOfWeekNum", value: DayOfWeekNum)
    sendEvent(name: "DayOfYearNum", value: DayOfYearNum)
    sendEvent(name: "DaysInMonthNum", value: DaysInMonthNum)
    sendEvent(name: "GMTDiffHours", value: GMTDiffHours)
    sendEvent(name: "IsDayOfMonNumEven", value: IsDayOfMonNumEven)
    sendEvent(name: "IsDayOfMonNumOdd", value: IsDayOfMonNumOdd)
    sendEvent(name: "IsDayOfYearNumEven", value: IsDayOfYearNumEven)
    sendEvent(name: "IsDayOfYearNumOdd", value: IsDayOfYearNumOdd)
    sendEvent(name: "IsDSTActive", value: IsDSTActive)
    sendEvent(name: "IsLeapYear", value: IsLeapYear)
    sendEvent(name: "IsObservesDST", value: IsObservesDST)
    sendEvent(name: "IsWeekOfMonNumEven", value: IsWeekOfMonNumEven)
    sendEvent(name: "IsWeekOfMonNumOdd", value: IsWeekOfMonNumOdd)
    sendEvent(name: "IsWeekOfYearNumEven", value: IsWeekOfYearNumEven)
    sendEvent(name: "IsWeekOfYearNumOdd", value: IsWeekOfYearNumOdd)
    sendEvent(name: "MonthName", value: MonthName)
    sendEvent(name: "MonthNameText3", value: MonthNameText3)
    sendEvent(name: "MonthNum", value: MonthNum)
    sendEvent(name: "MonthNumNoLead", value: MonthNumNoLead)
    sendEvent(name: "TZID", value: TZID)
    sendEvent(name: "TZIDText3", value: TZIDText3)
    sendEvent(name: "TimeAntePostLower", value: TimeAntePostLower)
    sendEvent(name: "TimeAntePostUpper", value: TimeAntePostUpper)
    sendEvent(name: "TimeHour12Num", value: TimeHour12Num)
    sendEvent(name: "TimeHour12NumNoLead", value: TimeHour12NumNoLead)
    sendEvent(name: "TimeHour24Num", value: TimeHour24Num)
    sendEvent(name: "TimeHour24NumNoLead", value: TimeHour24NumNoLead)
    sendEvent(name: "TimeMinNum", value: TimeMinNum)
    sendEvent(name: "TimeMinNumNoLead", value: TimeMinNumNoLead)
    sendEvent(name: "WeekOfMonNum", value: WeekOfMonNum)
    sendEvent(name: "WeekOfYearNum", value: WeekOfYearNum)
    sendEvent(name: "YearNum2Dig", value: YearNum2Dig)
    sendEvent(name: "YearNum4Dig", value: YearNum4Dig)
    sendEvent(name: "comparisonDate", value: comparisonDate)
    sendEvent(name: "comparisonDateStr", value: comparisonDateStr)
    sendEvent(name: "comparisonDateTime", value: comparisonDateTime)
    sendEvent(name: "comparisonDateTimeStr", value: comparisonDateTimeStr)
    sendEvent(name: "comparisonTime", value: comparisonTime)
    sendEvent(name: "comparisonTimeStr", value: comparisonTimeStr)
}
