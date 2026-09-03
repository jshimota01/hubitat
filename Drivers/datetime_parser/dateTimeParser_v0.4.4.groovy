/**
 * Date & Time Parser
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Virtual utility driver that parses the current system time/date into a comprehensive 
 * suite of string, numerical, ordinal, and comparison attributes for use in Hubitat rules and dashboards.
 **/
/**
 * Copyright 2026 James Shimota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
/**
 * Change History:
 *
 * Date         Source      Version What       URL
 * ----         ------      ------- ---- 
 * 2026-09-03   jshimota    0.4.4   Renamed custom command scheduleRefresh to Refresh Scheduler for clearer administrative intent.
 * 2026-09-03   jshimota    0.4.3   Restored v0.3.8 string format preservation for lead/no-lead attributes (preventing .toInteger() truncation), while retaining schedule optimizations, 5-tier logging, and lastUpdate tracking.
 * 2026-09-03   jshimota    0.4.1   Streamlined execution paths: eliminated schedule churn in mySchedule/dailySchedule, shifted attribute logs to logDebug, added lastUpdate attribute, and removed Health Check/Reset driver routines.
 * 2026-09-03   jshimota    0.4.0   Integrated Driver Template v1.0.11 architecture: standardized 5-tier logging engine, 30-min debug auto-off timer, version demarcation tracing, healthStatus tracking, and resetDriver routine.
 * 2026-06-22   jshimota    0.3.9   Gemini Optimization-Modernized refactor 2026.
 * 2026-05-14   jshimota    0.3.8   Fix package json to required true.
 * 2026-05-01   jshimota    0.3.7   Bug on 176 log enabled should be txtEnable.
 * 2026-03-08   jshimota    0.3.6   Moved daily schedule to 3:15 to get past Hub hour update on DST.
 * 2026-01-10   jshimota    0.3.5   Changed log dbg and txt params for debugging.
 * 2025-10-20   jshimota    0.3.4   Added dailyRefresh.
 * 2025-10-19   jshimota    0.3.3   Added debug log - restructured log reporting.
 * 2025-03-14   jshimota    0.3.2   Added daily schedule run time to after HE and DST changes (2:45am).
 * 2022-08-15   jshimota    0.3.1   Typo error found in Week of Mon variables.
 * 2022-08-12   jshimota    0.3.0   Week of Year was case sensitive and showing week of month, added week of month as well.
 * 2022-04-30   jshimota    0.2.9   2 minor text changes for clarity, attempt to fix schedule loop.
 * 2022-01-26   jshimota    0.2.8   Added String versions of comparison date times for user.
 * 2022-01-23   jshimota    0.2.7   TimeHour24NumNoLead fixed - added debug logging check to a line.
 * 2022-01-22   jshimota    0.2.6   Add of Even/Odd value to day of year number variables.
 * 2022-01-22   jshimota    0.2.5   Add of Even/Odd value to day of month number variables.
 * 2022-01-22   jshimota    0.2.4   With SBurke help - fixed booleans not supported by HE on comparators.
 * 2022-01-22   jshimota    0.2.3   Added WeekOfYearOdd/Even for garbage cans.
 * 2022-01-21   jshimota    0.2.2   Fixed switch case for Suffix, added Nolead to minutes var, scheduler drop down and values.
 * 2022-01-20   jshimota    0.2.1   Added user compare value requests.
 * 2022-01-20   jshimota    0.2.0   Release (getting HPM value for package).
 * 2022-01-20   jshimota    0.1.9   Commented tile features completely - no intent to support.
 * 2022-01-20   jshimota    0.1.8   Added update schedule ability.
 * 2022-01-20   jshimota    0.1.7   Tried adding Simons time and date stuff back, changed mind.
 * 2022-01-20   jshimota    0.1.6   Added final missing attributes - DST, ObservesDST, LeapYear, Day Suffix and Ordinal.
 * 2022-01-20   jshimota    0.1.5   Heavy work done on basic function cleanup, as well as optimization.
 * 2022-01-20   jshimota    0.1.4   First efforts to identify workarounds on php variations not found in Java.
 * 2022-01-20   jshimota    0.1.3   Worked on Scheduling cleanup and logging.
 * 2022-01-19   jshimota    0.1.2   Alpha release for testing.
 * 2021-01-19   Simon Burke 0.1.1   Used 2021-09-30 DateFormat app components  https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
 * 2022-01-19   jshimota    0.1.0   Starting version.
 **/

import java.text.SimpleDateFormat
import groovy.transform.Field

static String version() { return '0.4.4' }
def timeStamp() { return "2026/09/03 09:52 AM" }

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
        capability "Configuration"
        capability "Refresh"

        // Driver Tracking Attributes
        attribute "driverVersion", "string"
        attribute "lastUpdate", "string"

        // Time Attributes
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

        // Custom Commands
        command "Refresh Scheduler"
    }

    preferences {
        input name: "autoUpdate", type: "bool", title: "<b>Enable Automatic Refresh?</b>", defaultValue: true, required: true
        input name: "autoUpdateInterval", type: "enum", options: [[1:"1 minute"],[2:"2 minutes"],[5:"5 minutes"],[10:"10 minutes"],[15:"15 minutes"],[20:"20 minutes"],[30:"30 minutes"],[45:"45 minutes"],[59:"59 minutes"]], title: "<b>Auto Refresh Interval</b>", defaultValue: 5, required: true

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper Routine
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

void parse(String description) {
    logDebug "parse(): ${description}"
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    updateAttribute("driverVersion", version())

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    updateAttribute("driverVersion", version())
    
    initialize(false)
    refresh()
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    updateAttribute("driverVersion", version())
    
    initialize(false)
    refresh()
    return []
}

def refresh() {
    logDebug "refresh() requested"
    runCmd()
    manageSchedules()
    return []
}

def dailyRefresh() {
    logDebug "dailyRefresh() executing..."
    runCmd()
}

def "Refresh Scheduler"() {
    logDebug "Refresh Scheduler requested"
    manageSchedules()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    updateAttribute("driverVersion", version())

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging enabled. Will automatically turn off in 30 minutes."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

/* =========================================================================================
   DATE & TIME PARSING CORE LOGIC
   ========================================================================================= */

private void manageSchedules() {
    unschedule("mySchedule")
    unschedule("dailySchedule")
    logInfo "Cleared existing schedules."
    
    if (autoUpdate) {
        int interval = settings.autoUpdateInterval ? settings.autoUpdateInterval.toInteger() : 5
        schedule("0 0/${interval} * ? * * *", "mySchedule")
        logInfo "Set periodic scheduled refresh with ${interval} minute interval."
    } else {
        logInfo "Automatic Update is DISABLED."
    }
    
    schedule("0 15 3 ? * * *", "dailySchedule")
    logInfo "Setting DAILY schedule at 3:15 AM each day."
}

void mySchedule() {
    runCmd()
}

void dailySchedule() {
    dailyRefresh()
}

void runCmd() {
    def now = new Date()
    
    // Reuse SimpleDateFormat instances across pattern evaluations
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

    // Comparison values preserved exactly as original string concatenations
    def comparisonDate = YearNum4Dig + MonthNum + DayOfMonNum
    def comparisonTime = TimeHour24Num + TimeMinNum
    def comparisonDateTime = YearNum4Dig + MonthNum + DayOfMonNum + TimeHour24Num + TimeMinNum

    int iYear = Integer.parseInt(YearNum4Dig)
    int iMonth = Integer.parseInt(MonthNum) - 1 
    int iDay = Integer.parseInt(DayOfMonNum)
    
    GregorianCalendar currentCal = new GregorianCalendar(iYear, iMonth, iDay)
    def DaysInMonthNum = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    def LeapYearBool = currentCal.isLeapYear(iYear)

    TimeZone timezonedefault = TimeZone.getDefault()
    def ObservesDST = timezonedefault.observesDaylightTime()
    def DSTActiveBool = timezonedefault.inDaylightTime(now)

    int iDayOfMonNum = Integer.parseInt(DayOfMonNum)
    boolean DayOfMonNumEven = (iDayOfMonNum % 2 == 0)

    int iDayOfYearNum = Integer.parseInt(DayOfYearNum)
    boolean DayOfYearNumEven = (iDayOfYearNum % 2 == 0)

    int iWeekOfMonNum = Integer.parseInt(WeekOfMonNum)
    boolean WeekOfMonNumEven = (iWeekOfMonNum % 2 == 0)

    int iWeekOfYearNum = Integer.parseInt(WeekOfYearNum)
    boolean WeekOfYearNumEven = (iWeekOfYearNum % 2 == 0)

    String OrdDay = getOrdinal(iDay)
    String DayOfMonSuf = OrdDay
    String DayOfMonOrd = String.valueOf(iDay) + OrdDay

    sdf.applyPattern("yyyy-MM-dd HH:mm:ss")
    def lastUpdateFormatted = sdf.format(now)

    // Preserved raw String payloads for lead/no-lead formatted values
    def events = [
        "DayName": DayName,
        "DayNameText3": DayNameText3,
        "DayOfMonNum": DayOfMonNum,
        "DayOfMonNumNoLead": DayOfMonNumNoLead,
        "DayOfMonOrd": DayOfMonOrd,
        "DayOfMonSuf": DayOfMonSuf,
        "DayOfWeekNum": DayOfWeekNum,
        "DayOfYearNum": DayOfYearNum,
        "DaysInMonthNum": DaysInMonthNum,
        "GMTDiffHours": GMTDiffHours,
        "IsDayOfMonNumEven": DayOfMonNumEven.toString(),
        "IsDayOfMonNumOdd": (!DayOfMonNumEven).toString(),
        "IsDayOfYearNumEven": DayOfYearNumEven.toString(),
        "IsDayOfYearNumOdd": (!DayOfYearNumEven).toString(),
        "IsDSTActive": DSTActiveBool.toString(),
        "IsLeapYear": LeapYearBool.toString(),
        "IsObservesDST": ObservesDST.toString(),
        "IsWeekOfMonNumEven": WeekOfMonNumEven.toString(),
        "IsWeekOfMonNumOdd": (!WeekOfMonNumEven).toString(),
        "IsWeekOfYearNumEven": WeekOfYearNumEven.toString(),
        "IsWeekOfYearNumOdd": (!WeekOfYearNumEven).toString(),
        "MonthName": MonthName,
        "MonthNameText3": MonthNameText3,
        "MonthNum": MonthNum,
        "MonthNumNoLead": MonthNumNoLead,
        "TZID": TZID,
        "TZIDText3": TZIDText3,
        "TimeAntePostLower": TimeAntePostLower,
        "TimeAntePostUpper": TimeAntePostUpper,
        "TimeHour12Num": TimeHour12Num,
        "TimeHour12NumNoLead": TimeHour12NumNoLead,
        "TimeHour24Num": TimeHour24Num,
        "TimeHour24NumNoLead": TimeHour24NumNoLead,
        "TimeMinNum": TimeMinNum,
        "TimeMinNumNoLead": TimeMinNumNoLead,
        "WeekOfMonNum": WeekOfMonNum,
        "WeekOfYearNum": WeekOfYearNum,
        "YearNum2Dig": YearNum2Dig,
        "YearNum4Dig": YearNum4Dig,
        "comparisonDate": comparisonDate,
        "comparisonDateStr": String.valueOf(comparisonDate),
        "comparisonDateTime": comparisonDateTime,
        "comparisonDateTimeStr": String.valueOf(comparisonDateTime),
        "comparisonTime": comparisonTime,
        "comparisonTimeStr": String.valueOf(comparisonTime),
        "lastUpdate": lastUpdateFormatted
    ]

    events.each { name, val -> updateAttribute(name, val) }
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
    logDebug descriptionText
    sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${devName}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}