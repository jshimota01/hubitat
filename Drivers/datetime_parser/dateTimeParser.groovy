/*
 * Date & Time Parser
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
 *      Date          Source        Version     What                                              URL
 *      ----          ------        -------     ----                                              ---
 *      2022-01-20    jshimota      0.1.0       Starting version
 *      2021-09-30    Simon Burke   0.1.1       Used DateFormat app components                    https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
 *
 */

import java.text.SimpleDateFormat
import java.util.Date

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

        attribute "formattedDTDayOfMonNum", "number"
        attribute "formattedDTDayText3", "string"
        attribute "formattedDTDayOfMonNumNoLead", "number"
        attribute "formattedDTDayName", "string"
        attribute "formattedDTDayOfWeekNum", "number"
        attribute "formattedDTDayOfMonSuf", "string"
        attribute "formattedDTDayOfYearNum", "number"
        attribute "formattedDTWeekOfYearNum", "number"
        attribute "formattedDTMonthName", "string"
        attribute "formattedDTMonthNum", "number"
        attribute "formattedDTMonthNameText3", "string"
        attribute "formattedDTMonthNumNoLead", "number"
        //attribute "formattedDTDaysInMonthNum", "number"
        //attribute "formattedDTLeapBool","boolean"
        attribute "formattedDTYearNum4Dig", "number"
        attribute "formattedDTYearNum2Dig", "number"
        //attribute "formattedDTTimeLowerAntePost", "string"
        attribute "formattedDTTimeUpperAntePost", "string"
        //attribute "formattedDTTimeHour12NumNoLead", "number"
        //attribute "formattedDTTimeHour24NumNoLead", "number"
        attribute "formattedDTTimeHour12Num", "number"
        attribute "formattedDTTimeHour24Num", "number"
        attribute "formattedDTTimeMinNum", "number"
        attribute "formattedDTTZIDText3", "string"
        attribute "formattedDTTZID", "string"
        //attribute "formattedDTDSTBool","boolean"
        attribute "formattedDTGMTDiffHours", "string"
        //attribute "formattedDTTZText3", "string"

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
    input("autoUpdate", "bool", title: "Enable automatic update at 6am\n(Enabled is Yes)", defaultValue: true, required: true, displayDuringSetup: true)
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
}

def schedUpdate() {
    unschedule()
    if (txtEnable) log.info("schedUpdate: Refresh schedule cleared ...")
    if (autoUpdate) {
        if (txtEnable) log.info("Update: Setting next scheduled refresh...")
        if (autoUpdate) schedule("0 0 0 5 * ?", refresh) // every 5 minutes
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
    dTDayOfMonSufPattern = new SimpleDateFormat('s')
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
    formattedDTDayOfMonNum = dTDayOfMonNumPattern.format(now)
    formattedDTDayText3 = dTDayText3Pattern.format(now)
    formattedDTDayOfMonNumNoLead = dTDayOfMonNumNoLeadPattern.format(now)
    formattedDTDayName = dTDayNamePattern.format(now)
    formattedDTDayOfWeekNum = dTDayOfWeekNumPattern.format(now)
    formattedDTDayOfMonSuf = dTDayOfMonSufPattern.format(now)
    formattedDTDayOfYearNum = dTDayOfYearNumPattern.format(now)
    formattedDTWeekOfYearNum = dTWeekOfYearNumPattern.format(now)
    formattedDTMonthName = dTMonthNamePattern.format(now)
    formattedDTMonthNum = dTMonthNumPattern.format(now)
    formattedDTMonthNameText3 = dTMonthNameText3Pattern.format(now)
    formattedDTMonthNumNoLead = dTMonthNumNoLeadPattern.format(now)
    //formattedDTDaysInMonthNum = dTDaysInMonthNumPattern.format(now)
    //formattedDTLeapBool = isLeapYear(now)
    formattedDTYearNum4Dig = dTYearNum4DigPattern.format(now)
    formattedDTYearNum2Dig = dTYearNum2DigPattern.format(now)
    //formattedDTTimeLowerAntePost = dTTimeLowerAntePostPattern.format(now)
    formattedDTTimeUpperAntePost = dTTimeUpperAntePostPattern.format(now)
    //formattedDTTimeHour12NumNoLead = dTTimeHour12NumNoLeadPattern.format(now)
    //formattedDTTimeHour24NumNoLead = dTTimeHour24NumNoLeadPattern.format(now)
    formattedDTTimeHour12Num = dTTimeHour12NumPattern.format(now)
    formattedDTTimeHour24Num = dTTimeHour24NumPattern.format(now)
    formattedDTTimeMinNum = dTTimeMinNumPattern.format(now)
    formattedDTTZID = dTTZIDPattern.format(now)
    formattedDTTZIDText3 = dTTZIDText3Pattern.format(now)
    //formattedDTDSTBool = dTDSTBoolPattern.format(now)
    formattedDTGMTDiffHours = dTGMTDiffHoursPattern.format(now)
    //formattedDTTZText3 = dTTZText3Pattern.format(now)

    //sendEvent(name: "todaysFormattedMonth", value: proposedFormattedMonth)
    sendEvent(name: "todays Day Of Month Number", value: formattedDTDayOfMonNum)
    sendEvent(name: "todays Day Name - 3 chars", value: formattedDTDayText3)
    sendEvent(name: "todays Day of Mon Number - no lead", value: formattedDTDayOfMonNumNoLead)
    sendEvent(name: "todays Day Name", value: formattedDTDayName)
    sendEvent(name: "todays Day Of Week Number", value: formattedDTDayOfWeekNum)
    sendEvent(name: "todays Day of Month Suffix ", value:  formattedDTDayOfMonSuf)
    sendEvent(name: "todays Day of the Year Number", value:  formattedDTDayOfYearNum)
    sendEvent(name: "todays Week of the Year Number", value:  formattedDTWeekOfYearNum)
    sendEvent(name: "todays Month Name", value:  formattedDTMonthName)
    sendEvent(name: "todays Month Number", value:  formattedDTMonthNum)
    sendEvent(name: "todays Month Name - 3 chars", value:  formattedDTMonthNameText3)
    sendEvent(name: "todays Month Number - no lead", value:  formattedDTMonthNumNoLead)
    //sendEvent(name: "todays Days In Month", value:  formattedDTDaysInMonthNum)
    //sendEvent(name: "todays In Leap Year?", value:  formattedDTLeapBool)
    sendEvent(name: "todays Year Number", value:  formattedDTYearNum4Dig)
    sendEvent(name: "todays Year Number - 2 digit", value:  formattedDTYearNum2Dig)
    //sendEvent(name: "todays Lowercase am-pm", value:  formattedDTTimeLowerAntePost)
    sendEvent(name: "todays Uppercase AM-PM", value:  formattedDTTimeUpperAntePost)
    //sendEvent(name: "todays time 12 hour (no lead) ", value:  formattedDTTimeHour12NumNoLead)
    //sendEvent(name: "todays time 24 hour (no lead)", value:  formattedDTTimeHour24NumNoLead)
    sendEvent(name: "todays time 12 hour", value:  formattedDTTimeHour12Num)
    sendEvent(name: "todays time 24 hour", value:  formattedDTTimeHour24Num)
    sendEvent(name: "todays time minutes", value:  formattedDTTimeMinNum)
    sendEvent(name: "TimeZone identifier", value:  formattedDTTZID)
    sendEvent(name: "TimeZone identifier - 3 char", value:  formattedDTTZIDText3)
    //sendEvent(name: "todays time in DST?", value:  formattedDTDSTBool)
    sendEvent(name: "todays GMT hours difference", value:  formattedDTGMTDiffHours)
    //sendEvent(name: "todays Abbreviated Timezone", value:  formattedDTTZText3)

}