/**
 * Application Name: Multiperiod Environment Manager
 * Platform: Hubitat Elevation
 * Notes: Schedules heating/cooling setpoint windows with switch overrides, dual House & Office HVAC 
 *        controller management, fire safety forced-off modes, automatic fan circulation, 6-tier EPA AQI 
 *        Air Quality monitoring with health action strings, independent Air Filter control, native automatic 
 *        heat/cool mode changeover, native persisted setpoint tables, Smart Table boundary time pickers, and dashboard tile output.
 * Category: Convenience, HVAC, Thermostat, Fan
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
 *  Purpose:
 *  Schedules independent heating/cooling setpoint windows for House and Office HVAC controllers with switch overrides, 
 *  safety forced-off triggers for office floor heaters, live device tracking, automatic fan circulation, 
 *  6-tier EPA AQI filtering with health alerts, native heat/cool auto-changeover, and dashboard tile output.
 *
 *  Changelog:
 *	v1.7.7	  09/03/26	  jshimota	  Refactored AQI status string formatting on dashboard tile to remove text-based color names and wrapping parentheses while retaining colorized Level of Concern text. Preserved GUI table labels.
 *	v1.7.6	  09/03/26	  jshimota	  Resolved locked-Auto driver mode mismatch by prioritizing physicalThermostatMode over thermostatMode in getThermostatStateSnapshot(). Restored forced-OFF deduplication and auto-changeover evaluation against true hardware/virtual state.
 *	v1.7.5	  09/03/26	  jshimota	  Prevented redundant Office HVAC forced-off commands and log noise by checking thermostatMode first and emitting warn logs only on actual shutoff. Prevented duplicate setpoint calls by passing forceSetpointApply = false on runtime events. Fixed abrupt fan ON trigger when sleeping switch toggles by recalculating timers without resetting to ON phase.
 *	v1.7.4	  09/02/26	  jshimota	  Replaced block-level title wrapper <div> elements with inline <span> wrappers (white-space:nowrap;) across Section 1 and Section 2 schedule grids to prevent Hubitat's required field indicator (*) from wrapping onto a new line.
 *	v1.7.3	  09/02/26	  jshimota	  Extended localized title wrapper height (height:42px) across Section 1 and Section 2 schedule grid columns 1, 2, and 3 to visually project vertical divider lines alongside native input controls.
 *	v1.7.2	  09/02/26	  jshimota	  Added localized border-right vertical divider wrappers to Section 1 and Section 2 schedule grid titles (columns 1, 2, and 3) without modifying any application logic or native input attributes.
 *	v1.7.1	  09/02/26	  jshimota	  Replaced raw HTML time selectors in Section 3 with native Hubitat time inputs for full settings persistence reliability. Added a polished GUI summary table for Section 3 boundaries. Synchronized fan circulation log outputs with state.fanOwned status. Updated chronological ordering warning string to reflect deterministic time sorting.
 *	v1.7.0	  09/02/26	  jshimota	  Fixed Sleeping switch fan circulation delay by triggering immediate fan timer recalculation. Removed dead Office Presence/Sleeping override UI setpoint inputs to eliminate UI misdirection while maintaining hardcoded Office OFF safety protocol. Added presence sensor selection validation warning. Guarded fanAuto() calls against state.fanOwned flag. Secured getBoundaryMinutes() against NumberFormatException with safeToInt(). Added chronological period boundary order validation warning.
 **/

static String version() { return '1.7.7' }
def timeStamp() { return "2026/09/03 10:20 AM" }

definition(
    name: "Multiperiod Environment Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Schedules heating/cooling setpoint windows for House and Office HVAC controllers with switch overrides, safety forced-off modes, live device tracking, automatic fan circulation, EPA AQI monitoring, and dashboard tile output.",
    category: "Convenience, HVAC, Thermostat, Fan",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    importUrl: "https://raw.githubusercontent.com/jshimota/hubitat-smartapps/main/multiperiod_environment_manager.groovy"
)

preferences {
    page(name: "mainPage")
}

// =================================================================================================
// USER INTERFACE
// =================================================================================================

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        
        String currentVersion = version()

        Map ctx = buildEvaluationContext()
        Integer avgAqi = ctx.aqi as Integer
        
        Map aqiStatusInfo = determineStatus(avgAqi)
        String displayAqiStatus = aqiStatusInfo.status

        Map t1States = ctx.thermostat1 ?: [:]
        def dev1Heat = t1States.heat ?: "--"
        def dev1Cool = t1States.cool ?: "--"
        def dev1Mode = t1States.mode ?: "--"

        Map t2States = ctx.thermostat2 ?: [:]
        def dev2Heat = t2States.heat ?: "--"
        def dev2Cool = t2States.cool ?: "--"
        def dev2Mode = t2States.mode ?: "--"

        Map scheduleData = ctx.schedule ?: [:]

        Boolean isHouseEnabled = targetThermostat && getSettingBool("dev1AppEnabled", true)
        Boolean isOfficeEnabled = targetThermostat2 && getSettingBool("dev2AppEnabled", false)

        /* App Title Banner & Active Status Card */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Multiperiod Environment Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
            
            String activePeriodStr = scheduleData.activePeriod ?: "Not Evaluated"
            String houseBadge = isHouseEnabled ? "<span style='color:#27AE60; font-weight:bold;'>HOUSE ENABLED</span>" : "<span style='color:#C0392B; font-weight:bold;'>HOUSE DISABLED</span>"
            String officeBadge = isOfficeEnabled ? "<span style='color:#27AE60; font-weight:bold;'>OFFICE ENABLED</span>" : "<span style='color:#C0392B; font-weight:bold;'>OFFICE DISABLED</span>"
            
            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #27AE60; padding:10px; border-radius:4px; font-size:13px;'>" +
                      "<b>Units:</b> ${houseBadge} | ${officeBadge} &nbsp;|&nbsp; " +
                      "<b>Active Period:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodStr}</span><br/>" +
                      "<b>House HVAC Live:</b> Heat: <b>${dev1Heat} °F</b> | Cool: <b>${dev1Cool} °F</b> (${dev1Mode}) &nbsp;|&nbsp; " +
                      "<b>Office HVAC Live:</b> Heat: <b>${dev2Heat} °F</b> | Cool: <b>${dev2Cool} °F</b> (${dev2Mode})</div>"
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY A: MANAGED HVAC DEVICES & SCHEDULES
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:10px;'>CATEGORY A: MANAGED HVAC DEVICES & SCHEDULES</div>") {}

        /* Section 1: House HVAC Controller Configuration */
        section("<b>SECTION 1: House HVAC Controller/Thermostat Device (Primary)</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure settings, automated mode changeover, and time schedules for the primary House HVAC Controller/Thermostat Device.</div>"

            input name: "targetThermostat", type: "capability.thermostat", title: "<b>Select House HVAC Controller/Thermostat Device</b>", required: true, multiple: false, submitOnChange: false
            input name: "dev1AppEnabled", type: "bool", title: "<b>Enable House HVAC Controller/Thermostat Device Control</b>", defaultValue: true, submitOnChange: false
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev1AutoChangeoverEnabled", type: "bool", title: "<b>Enable Automatic Mode Changeover (Heat / Cool Switch)</b>", defaultValue: true, submitOnChange: false
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:6px;'>" +
                      "<i>When enabled, MEM automatically toggles the thermostat mode between Heat and Cool based on ambient room temperature readings and setpoint boundaries.</i></div>"
            
            input name: "dev1TempDeadband", type: "number", title: "<b>House Temperature Hysteresis Buffer (°F)</b>", defaultValue: 2, required: true, width: 6
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:6px;'><i>Degree offset required beyond opposing setpoint before triggering mode change.</i></div>"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev1ScheduleEnabled", type: "bool", title: "<b>Enable Setpoint Scheduling</b>", defaultValue: true, submitOnChange: false
            paragraph "<div style='color:#2980B9; font-size:11px; margin-top:-6px; margin-bottom:8px;'>" +
                      "<b>When disabled:</b> Scheduled target setpoints are suspended. The House HVAC unit holds its existing setpoints unless overridden by Presence/Sleeping mode sensors.</div>"

            // 4-Column Banner Header with Explicit Border Dividers
            paragraph "<table style='width:100%; border-collapse:collapse; margin-bottom:4px; font-size:12px; font-weight:bold; text-align:center; background-color:#2C3E50; color:#FFFFFF; border-radius:4px;'>" +
                      "<tr>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:DodgerBlue;'>COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:25%; padding:6px;'><span style='color:DodgerBlue;'>COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "</tr></table>"

            // Row 1: Morning & Day (4-across layout width 3)
            input name: "d1MorningHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Morning</b></span>", defaultValue: 68, required: true, width: 3, submitOnChange: false
            input name: "d1MorningCool", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Morning</b></span>", defaultValue: 72, required: true, width: 3, submitOnChange: false
            input name: "d1DayHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Day</b></span>", defaultValue: 66, required: true, width: 3, submitOnChange: false
            input name: "d1DayCool", type: "number", title: "<b>Day</b>", defaultValue: 75, required: true, width: 3, submitOnChange: false
            
            // Row 2: Evening & Night (4-across layout width 3)
            input name: "d1EveningHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Evening</b></span>", defaultValue: 69, required: true, width: 3, submitOnChange: false
            input name: "d1EveningCool", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Evening</b></span>", defaultValue: 73, required: true, width: 3, submitOnChange: false
            input name: "d1NightHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Night</b></span>", defaultValue: 64, required: true, width: 3, submitOnChange: false
            input name: "d1NightCool", type: "number", title: "<b>Night</b>", defaultValue: 75, required: true, width: 3, submitOnChange: false
        }

        /* Section 2: Office HVAC Controller Configuration */
        section("<b>SECTION 2: Office HVAC Controller/Thermostat Device (Secondary / Floor Heater)</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure settings, automated mode changeover, and time schedules for the secondary Office HVAC Controller/Thermostat Device (Floor Heater).</div>"

            input name: "targetThermostat2", type: "capability.thermostat", title: "<b>Select Office HVAC Controller/Thermostat Device</b>", required: false, multiple: false, submitOnChange: false
            input name: "dev2AppEnabled", type: "bool", title: "<b>Enable Office HVAC Controller/Thermostat Device Control</b>", defaultValue: false, submitOnChange: false
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev2AutoChangeoverEnabled", type: "bool", title: "<b>Enable Automatic Mode Changeover (Heat / Cool Switch)</b>", defaultValue: false, submitOnChange: false
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:6px;'>" +
                      "<i>When enabled, MEM automatically toggles the thermostat mode between Heat and Cool based on ambient room temperature readings and setpoint boundaries.</i></div>"

            input name: "dev2TempDeadband", type: "number", title: "<b>Office Temperature Hysteresis Buffer (°F)</b>", defaultValue: 2, required: true, width: 6
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:6px;'><i>Degree offset required beyond opposing setpoint before triggering mode change.</i></div>"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev2ScheduleEnabled", type: "bool", title: "<b>Enable Setpoint Scheduling</b>", defaultValue: true, submitOnChange: false
            paragraph "<div style='color:#2980B9; font-size:11px; margin-top:-6px; margin-bottom:8px;'>" +
                      "<b>When disabled:</b> Scheduled target setpoints are suspended. The Office HVAC unit holds its existing setpoints unless overridden by Safety Forced-OFF triggers.</div>"

            // 4-Column Banner Header with Explicit Border Dividers
            paragraph "<table style='width:100%; border-collapse:collapse; margin-bottom:4px; font-size:12px; font-weight:bold; text-align:center; background-color:#2C3E50; color:#FFFFFF; border-radius:4px;'>" +
                      "<tr>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:DodgerBlue;'>COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:25%; padding:6px;'><span style='color:DodgerBlue;'>COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "</tr></table>"

            // Row 1: Morning & Day (4-across layout width 3)
            input name: "d2MorningHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Morning</b></span>", defaultValue: 68, required: true, width: 3, submitOnChange: false
            input name: "d2MorningCool", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Morning</b></span>", defaultValue: 72, required: true, width: 3, submitOnChange: false
            input name: "d2DayHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Day</b></span>", defaultValue: 66, required: true, width: 3, submitOnChange: false
            input name: "d2DayCool", type: "number", title: "<b>Day</b>", defaultValue: 75, required: true, width: 3, submitOnChange: false
            
            // Row 2: Evening & Night (4-across layout width 3)
            input name: "d2EveningHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Evening</b></span>", defaultValue: 69, required: true, width: 3, submitOnChange: false
            input name: "d2EveningCool", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Evening</b></span>", defaultValue: 73, required: true, width: 3, submitOnChange: false
            input name: "d2NightHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Night</b></span>", defaultValue: 62, required: true, width: 3, submitOnChange: false
            input name: "d2NightCool", type: "number", title: "<b>Night</b>", defaultValue: 78, required: true, width: 3, submitOnChange: false
        }

        /* Section 3: Period Schedule Window Boundaries */
        section("<b>SECTION 3: Period Schedule Window Boundaries</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Set start boundary times for each chronological schedule period. <i>(Applies to both House and Office HVAC controllers).</i></div>"

            Map val = validateConfiguration()
            if (val.warnings && !val.warnings.isEmpty()) {
                StringBuilder sb = new StringBuilder()
                sb.append("<div style='color:#8a6d3b; background-color:#fcf8e3; padding:8px; border:1px solid #faebcc; border-radius:4px; margin-bottom:10px;'><b>SCHEDULE VALIDATION NOTICE:</b><ul>")
                val.warnings.each { w -> sb.append("<li>").append(w).append("</li>") }
                sb.append("</ul></div>")
                paragraph sb.toString()
            }

            String mStr = formatTimeSetting(settings.mStartTime, "06:00 AM")
            String dStr = formatTimeSetting(settings.dStartTime, "09:00 AM")
            String eStr = formatTimeSetting(settings.eStartTime, "05:00 PM")
            String nStr = formatTimeSetting(settings.nStartTime, "10:00 PM")

            StringBuilder summaryTable = new StringBuilder()
            summaryTable.append("<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:12px; text-align:center; background-color:#F8F9FA; border-radius:4px;'>")
            summaryTable.append("<tr style='background-color:#2C3E50; color:#FFFFFF; font-weight:bold;'>")
            summaryTable.append("<td style='width:25%; padding:6px; border-right:1px solid #FFF;'>Morning Start</td>")
            summaryTable.append("<td style='width:25%; padding:6px; border-right:1px solid #FFF;'>Day Start</td>")
            summaryTable.append("<td style='width:25%; padding:6px; border-right:1px solid #FFF;'>Evening Start</td>")
            summaryTable.append("<td style='width:25%; padding:6px;'>Night Start</td>")
            summaryTable.append("</tr>")
            summaryTable.append("<tr style='font-size:13px; font-weight:bold; color:#2980B9;'>")
            summaryTable.append("<td style='padding:6px; border-right:1px solid #E0E0E0;'>").append(mStr).append("</td>")
            summaryTable.append("<td style='padding:6px; border-right:1px solid #E0E0E0;'>").append(dStr).append("</td>")
            summaryTable.append("<td style='padding:6px; border-right:1px solid #E0E0E0;'>").append(eStr).append("</td>")
            summaryTable.append("<td style='padding:6px;'>").append(nStr).append("</td>")
            summaryTable.append("</tr>")
            summaryTable.append("</table>")
            paragraph summaryTable.toString()

            input name: "mStartTime", type: "time", title: "<b>Morning Start Time</b>", required: true, width: 3, submitOnChange: false
            input name: "dStartTime", type: "time", title: "<b>Day Start Time</b>", required: true, width: 3, submitOnChange: false
            input name: "eStartTime", type: "time", title: "<b>Evening Start Time</b>", required: true, width: 3, submitOnChange: false
            input name: "nStartTime", type: "time", title: "<b>Night Start Time</b>", required: true, width: 3, submitOnChange: false
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY B: SYSTEM OVERRIDES & SAFETY MODES
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY B: SYSTEM OVERRIDES & SAFETY MODES</div>") {}

        /* Section 4: System Overrides */
        section("<b>SECTION 4: System Overrides (Presence & Sleeping Setpoints)</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure temperature setpoint overrides when Presence Sensor (OwnTracks) reads <b>not present</b> or Sleeping switch is active.</div>"

            // --- PRESENCE OVERRIDE GROUP ---
            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #2980B9; margin-bottom:8px;'>Presence Override / Setpoints</div>"
            input name: "presenceSensor", type: "capability.presenceSensor", title: "<b>Select Presence Sensor (e.g. OwnTracks)</b>", required: false, multiple: false, submitOnChange: false
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:8px;'>" +
                      "<i>Triggers presence override setpoints when the sensor attribute evaluates to 'not present'.</i></div>"
            
            input name: "awayDev1Enabled", type: "bool", title: "<b>Enable Control House HVAC Controller/Thermostat Device Presence Override</b>", defaultValue: true, submitOnChange: false

            // Standard 2-Column Header Bar
            paragraph "<table style='width:100%; border-collapse:collapse; margin:8px 0 4px 0; font-size:12px; font-weight:bold; text-align:center; background-color:#2C3E50; color:#FFFFFF; border-radius:4px;'>" +
                      "<tr>" +
                      "<td style='width:50%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HOUSE HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:50%; padding:6px;'><span style='color:DodgerBlue;'>HOUSE COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "</tr></table>"

            input name: "awayDev1Heat", type: "number", title: "<div style='border-right:1px solid #CCC; padding-right:8px;'><b>House Device</b></div>", required: true, defaultValue: 62, width: 6
            input name: "awayDev1Cool", type: "number", title: "<b>House Device</b>", required: true, defaultValue: 78, width: 6

            // --- SLEEPING OVERRIDE GROUP ---
            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #8E44AD; margin-top:16px; margin-bottom:8px;'>Sleeping Override / Setpoints</div>"
            input name: "sleepSwitch", type: "capability.switch", title: "<b>Select Sleeping Switch</b>", required: false, multiple: false, submitOnChange: false
            
            input name: "sleepDev1Enabled", type: "bool", title: "<b>Enable Control House HVAC Controller/Thermostat Device Sleeping Override</b>", defaultValue: true, submitOnChange: false

            paragraph "<table style='width:100%; border-collapse:collapse; margin:8px 0 4px 0; font-size:12px; font-weight:bold; text-align:center; background-color:#2C3E50; color:#FFFFFF; border-radius:4px;'>" +
                      "<tr>" +
                      "<td style='width:50%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HOUSE HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:50%; padding:6px;'><span style='color:DodgerBlue;'>HOUSE COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "</tr></table>"

            input name: "sleepDev1Heat", type: "number", title: "<div style='border-right:1px solid #CCC; padding-right:8px;'><b>House Device</b></div>", required: true, defaultValue: 65, width: 6
            input name: "sleepDev1Cool", type: "number", title: "<b>House Device</b>", required: true, defaultValue: 74, width: 6

            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #2980B9; padding:8px; border-radius:4px; margin-top:12px; font-size:11px; color:#1B4F72;'>" +
                      "<b>OFFICE HVAC ISOLATION:</b> Secondary Office HVAC (Floor Heater) setpoints are not configurable during Presence or Sleeping override states. " +
                      "The Office HVAC unit is automatically forced OFF during these modes as a mandatory safety protocol.</div>"
        }

        /* Section 5: Safety Controls */
        section("<b>SECTION 5: Safety Controls (Office HVAC Isolation)</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure emergency or presence forced-off controls to completely shut down secondary floor heaters.</div>"

            paragraph "<div style='background-color:#FDEDEC; border-left:4px solid #C0392B; padding:10px; border-radius:4px; margin-bottom:8px; font-size:12px; color:#78281F;'>" +
                      "<b>SAFETY MANDATE (NON-CONFIGURABLE):</b> The Office HVAC Controller/Thermostat Device will ALWAYS be forced OFF during active Presence (Away) or Sleeping override states. " +
                      "This safety isolation protocol is hardcoded into the execution core to prevent unmonitored floor heater operation and cannot be toggled or disabled.</div>"
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY C: ENVIRONMENTAL QUALITY & AIR FILTRATION
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY C: ENVIRONMENTAL QUALITY & AIR FILTRATION</div>") {}

        /* Section 6: Automatic Fan Circulation Control */
        section("<b>SECTION 6: Automatic Fan Circulation Control</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "When enabled, MEM manages fan circulation on the House HVAC Controller. Set the thermostat's Fan Control Source to <b>external</b> so the driver does not compete.</div>"

            input name: "fanCirculateEnabled", type: "bool", title: "<b>Enable Fan Circulation Control (House HVAC Controller/Thermostat Device)</b>", defaultValue: false, submitOnChange: false
            
            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #2980B9; margin-top:10px; margin-bottom:6px;'>Standard Circulation Period</div>"
            input name: "fanOnMinutes", type: "number", title: "<div style='border-right:1px solid #CCC; padding-right:8px;'><b>Fan ON (Minutes)</b></div>", required: true, defaultValue: 10, width: 6
            input name: "fanOffMinutes", type: "number", title: "<b>Fan OFF / AUTO (Minutes)</b>", required: true, defaultValue: 50, width: 6
            
            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #8E44AD; margin-top:12px; margin-bottom:6px;'>Sleeping Mode Circulation Period</div>"
            input name: "fanSleepOnMinutes", type: "number", title: "<div style='border-right:1px solid #CCC; padding-right:8px;'><b>Fan ON (Minutes)</b></div>", required: true, defaultValue: 5, width: 6
            input name: "fanSleepOffMinutes", type: "number", title: "<b>Fan OFF / AUTO (Minutes)</b>", required: true, defaultValue: 115, width: 6
        }

        /* Section 7: Air Quality Monitoring */
        section("<b>SECTION 7: Air Quality Monitoring & RGB Indicator Light</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Monitors indoor AQI sensors and calculates real-time averages. Controls an optional RGB indicator light using EPA 6-tier colors.</div>"

            Boolean isAqiValid = validateAqiThresholds()
            if (!isAqiValid && aqiEnabled) {
                paragraph "<div style='color:red; background-color:#ffe6e6; padding:8px; border:1px solid red; border-radius:4px;'><b>CONFIGURATION ERROR:</b> AQI Thresholds must be strictly ascending. Defaults applied until corrected.</div>"
            }

            Map limits = getAqiTierLimits()
            Integer tGoodMax = safeToInt(limits?.good, 50)
            Integer tModMax = safeToInt(limits?.mod, 100)
            Integer tSensMax = safeToInt(limits?.sens, 150)
            Integer tUnhealthMax = safeToInt(limits?.unhealthy, 200)
            Integer tVeryUnhealthMax = safeToInt(limits?.veryUnhealthy, 300)

            StringBuilder colorTableSb = new StringBuilder()
            colorTableSb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>")
            colorTableSb.append("<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>")
            colorTableSb.append("<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Level of Concern</th><th style='padding:4px;'>Color</th><th style='padding:4px;'>Health Impact & Actions</th>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>0 – ").append(tGoodMax).append("</td><td style='padding:4px; color:green;'><b>Good (Normal)</b></td><td style='padding:4px;'>⚪ OFF</td><td style='padding:4px;'>Satisfactory air quality. Little to no risk.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>").append((tGoodMax ?: 50) + 1).append(" – ").append(tModMax).append("</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>🟡 Yellow</td><td style='padding:4px;'>Acceptable. Sensitive groups may feel minor symptoms.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>").append((tModMax ?: 100) + 1).append(" – ").append(tSensMax).append("</td><td style='padding:4px; color:orange;'><b>Sensitive Groups</b></td><td style='padding:4px;'>🟠 Orange</td><td style='padding:4px;'>Potentially harmful for kids, elderly, and asthmatics.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>").append((tSensMax ?: 150) + 1).append(" – ").append(tUnhealthMax).append("</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>🔴 Red</td><td style='padding:4px;'>Active danger. Everyone may experience adverse effects.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>").append((tUnhealthMax ?: 200) + 1).append(" – ").append(tVeryUnhealthMax).append("</td><td style='padding:4px; color:purple;'><b>Very Unhealthy</b></td><td style='padding:4px;'>🟣 Purple</td><td style='padding:4px;'>Health alert. High risk of respiratory irritation.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr>")
            colorTableSb.append("<td style='padding:4px;'>&ge; ").append((tVeryUnhealthMax ?: 300) + 1).append("</td><td style='padding:4px; color:maroon;'><b>Hazardous</b></td><td style='padding:4px;'>🟤 Maroon</td><td style='padding:4px;'>Emergency conditions. Stay entirely indoors.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("</table>")
            paragraph colorTableSb.toString()

            input name: "aqiEnabled", type: "bool", title: "<b>Enable Air Quality Monitoring</b>", defaultValue: false, submitOnChange: false
            input name: "aqiSensor1", type: "capability.airQuality", title: "Select Primary AQI Sensor", required: false, multiple: false, submitOnChange: false
            input name: "aqiSensor2", type: "capability.airQuality", title: "Select Secondary AQI Sensor (Optional)", required: false, multiple: false, submitOnChange: false
            input name: "rgbLight", type: "capability.colorControl", title: "Select Indicator RGB Light Device", required: false, multiple: false
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>AQI Tier Upper Boundary Overrides</span>"
            
            input name: "aqiGoodMax", type: "number", title: "<div style='border-right:1px solid #CCC; padding-right:8px;'><b>Good Max</b></div>", required: true, defaultValue: 50, width: 6, submitOnChange: false
            input name: "aqiModMax", type: "number", title: "<b>Moderate Max</b>", required: true, defaultValue: 100, width: 6, submitOnChange: false
            input name: "aqiSensMax", type: "number", title: "<div style='border-right:1px solid #CCC; padding-right:8px;'><b>Sensitive Max</b></div>", required: true, defaultValue: 150, width: 6, submitOnChange: false
            input name: "aqiUnhealthyMax", type: "number", title: "<b>Unhealthy Max</b>", required: true, defaultValue: 200, width: 6, submitOnChange: false
            input name: "aqiVeryUnhealthyMax", type: "number", title: "<b>Very Unhealthy Max</b>", required: true, defaultValue: 300, width: 6, submitOnChange: false
        }

        /* Section 8: Room Air Filter Control */
        section("<b>SECTION 8: Room Air Filter Automation</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Automatically toggles the room air filter switch on/off based on real-time AQI tier threshold calculations.</div>"

            input name: "airFilterEnabled", type: "bool", title: "<b>Enable Automated Room Air Filter Control</b>", defaultValue: true, submitOnChange: false
            input name: "airFilterSwitch", type: "capability.switch", title: "Select Room Air Filter Switch/Device", required: false, multiple: false, submitOnChange: false
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY D: SYSTEM STATUS & DIAGNOSTICS
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY D: SYSTEM STATUS & DIAGNOSTICS</div>") {}

        /* Section 9: Live Status Summary */
        section("<b>SECTION 9: System Operational Status Summary</b>", hideable: true, hidden: true) {
            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #2980B9; padding:10px; border-radius:4px; font-size:12px;'>" +
                      "<b>House HVAC (${targetThermostat ? targetThermostat.displayName : 'None'}):</b> Temp: ${t1States.temp ?: '--'} °F | Heat: ${dev1Heat} °F | Cool: ${dev1Cool} °F | Mode: ${dev1Mode}<br/>" +
                      "<b>Office HVAC (${targetThermostat2 ? targetThermostat2.displayName : 'None'}):</b> Temp: ${t2States.temp ?: '--'} °F | Heat: ${dev2Heat} °F | Cool: ${dev2Cool} °F | Mode: ${dev2Mode}<br/>" +
                      "<hr style='border:0; border-top:1px solid #E0E0E0; margin:6px 0;'/>" +
                      "<b>Active Window:</b> ${scheduleData.activePeriod ?: 'Not Evaluated'}<br/>" +
                      "<b>House Target Setpoints:</b> Heat: ${scheduleData.d1TargetHeat ?: '--'} °F | Cool: ${scheduleData.d1TargetCool ?: '--'} °F<br/>" +
                      "<b>Office Target Setpoints:</b> " + (scheduleData.d2ForceOff ? "<span style='color:red; font-weight:bold;'>FORCED OFF</span>" : "Heat: ${scheduleData.d2TargetHeat ?: '--'} °F | Cool: ${scheduleData.d2TargetCool ?: '--'} °F") + "<br/>" +
                      "<hr style='border:0; border-top:1px solid #E0E0E0; margin:6px 0;'/>" +
                      (aqiEnabled ? "<b>Calculated Average AQI:</b> ${avgAqi ?: '--'} (${displayAqiStatus})<br/>" : "<b>Air Quality Monitoring:</b> Disabled<br/>") +
                      "<b>Last Evaluation Time:</b> ${state.lastEvaluated ?: 'Never'}</div>"
        }

        /* Section 10: Logging Options */
        section("<b>SECTION 10: App Preferences & Logging Options</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure system logging outputs and app preferences. <i>(Debug logging auto-disables after 30 minutes)</i>.</div>"

            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true, submitOnChange: false
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }

        /* App Name & Footer */
        section() {
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>App Label Customization</span>"
            label title: "Assign a custom label for this SmartApp instance", required: false
        }
    }
}

private String formatTimeSetting(Object timeVal, String defaultStr = "Not Set") {
    if (!timeVal) return defaultStr
    try {
        String isoStr = timeVal.toString()
        if (isoStr.contains("T")) {
            Date parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(isoStr.substring(0, 19))
            return new java.text.SimpleDateFormat("hh:mm a").format(parsedDate)
        }
        return isoStr
    } catch (Exception e) {
        return timeVal.toString()
    }
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Multiperiod Environment Manager"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn")) }
        .sort()

    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
}

// =================================================================================================
// 1. INITIALIZATION ENGINE
// =================================================================================================

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing app v${version()} (${timeStamp()})..."
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

    Map validation = validateConfiguration()
    if (!validation.valid) {
        validation.errors.each { err ->
            logError "CONFIGURATION VALIDATION ERROR: ${err}"
        }
        return
    }

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (state.lastInitializedVersion != version())

    if (settingsChanged || codeVersionChanged) {
        logInfo "Settings or code version modification detected. Re-establishing subscriptions and schedules..."
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(false)
    } else {
        logDebug "App closed without version changes. Forcing setpoint and tile update on Done..."
        executeEvaluationPass("GUI Configuration Update (Done Pushed)", true)
    }
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling app..."
    releaseDeviceOwnership()
    unsubscribe()
    unschedule()
}

private void initialize(Boolean isInstall = false) {
    state.lastInitializedVersion = version()

    updateAppLabel()

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }

    ensureChildDevice()

    subscribe(location, "mode", "modeChangeHandler")

    if (targetThermostat) subscribe(targetThermostat, "temperature", "temperatureHandler")
    if (targetThermostat2) subscribe(targetThermostat2, "temperature", "temperatureHandler")

    if (presenceSensor) subscribe(presenceSensor, "presence", "overrideSwitchHandler")
    if (sleepSwitch) subscribe(sleepSwitch, "switch", "overrideSwitchHandler")
    if (airFilterEnabled && airFilterSwitch) subscribe(airFilterSwitch, "switch", "airFilterSwitchHandler")
    
    if (aqiEnabled) {
        if (aqiSensor1) subscribe(aqiSensor1, "airQualityIndex", "aqiHandler")
        if (aqiSensor2) subscribe(aqiSensor2, "airQualityIndex", "aqiHandler")
    }

    scheduleTimeBoundaries()
    executeEvaluationPass("App Initialize", true)

    if (fanCirculateEnabled) {
        manageFanCirculation(true)
    } else {
        unschedule("toggleFanCirculation")
        if (state.fanOwned == true) {
            logInfo "Fan Circulation disabled. Restoring House HVAC fan to AUTO."
            try { targetThermostat?.fanAuto() } catch (Exception e) { logError "Failed to restore fan to AUTO: ${e.message}" }
            sendIfChangedStateValue("fanOwned", false)
        }
        sendIfChangedStateValue("fanPhase", "Disabled")
    }
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// =================================================================================================
// 2. UNIFIED EVALUATION PIPELINE PASS
// =================================================================================================

def executeEvaluationPass(String cause = "Manual / Scheduled", Boolean forceSetpointApply = false) {
    logDebug "Starting Unified Evaluation Pass. Trigger Cause: '${cause}', ForceApply: ${forceSetpointApply}"

    Map sched = getCalculatedScheduleData()
    Map thermostat1 = getThermostatStateSnapshot(targetThermostat)
    Map thermostat2 = getThermostatStateSnapshot(targetThermostat2)
    
    Integer calculatedAqi = null
    Map aqiStatus = [status: "Disabled", tileStatus: "Disabled", colorMap: null, actionText: "N/A", filterOn: false]
    
    if (aqiEnabled && (aqiSensor1 || aqiSensor2)) {
        Map rawVals = readSensors()
        calculatedAqi = calculateAverage(rawVals.v1, rawVals.v2)
        if (calculatedAqi != null) aqiStatus = determineStatus(calculatedAqi)
    }

    sendIfChangedStateValue("activeSet", sched.activePeriod)
    sendIfChangedStateValue("lastEvaluated", new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))

    // Process House HVAC Controller
    Boolean isHouseActive = targetThermostat && getSettingBool("dev1AppEnabled", true)
    if (isHouseActive && getSettingBool("dev1ScheduleEnabled", true) && sched.d1TargetHeat != null && sched.d1TargetCool != null) {
        applySetpointsAndEvaluateMode(targetThermostat, sched.d1TargetHeat as BigDecimal, sched.d1TargetCool as BigDecimal, thermostat1, dev1AutoChangeoverEnabled, dev1TempDeadband, false, forceSetpointApply)
    }

    // Process Office HVAC Controller
    Boolean isOfficeActive = targetThermostat2 && getSettingBool("dev2AppEnabled", false)
    if (isOfficeActive) {
        if (sched.d2ForceOff) {
            String currentOfficeMode = (thermostat2.mode ?: "").toLowerCase()
            if (currentOfficeMode != "off") {
                logWarn "Office HVAC Safety Trigger Active (${sched.activePeriod}). Forcing Office HVAC OFF."
                try { targetThermostat2.off() } catch (Exception e) { logError "Failed to execute OFF on Office HVAC: ${e.message}" }
            } else {
                logDebug "Office HVAC Safety Trigger Active (${sched.activePeriod}). Office HVAC is already OFF."
            }
        } else if (getSettingBool("dev2ScheduleEnabled", true) && sched.d2TargetHeat != null && sched.d2TargetCool != null) {
            applySetpointsAndEvaluateMode(targetThermostat2, sched.d2TargetHeat as BigDecimal, sched.d2TargetCool as BigDecimal, thermostat2, dev2AutoChangeoverEnabled, dev2TempDeadband, true, forceSetpointApply)
        }
    }

    if (aqiEnabled && rgbLight) {
        String currentMode = location.mode?.toLowerCase() ?: ""
        Boolean isAllowedMode = currentMode.contains("home") || currentMode.contains("awake")
        Boolean isAwayOverride = (presenceSensor && safeGetDeviceAttribute(presenceSensor, "presence") == "not present")
        Boolean isSleepOverride = (sleepSwitch && safeGetDeviceAttribute(sleepSwitch, "switch") == "on")

        if (isAllowedMode && !isAwayOverride && !isSleepOverride && aqiStatus.colorMap != null) {
            if (safeGetDeviceAttribute(rgbLight, "switch") != "on") rgbLight.on()
            rgbLight.setColor(aqiStatus.colorMap)
            sendIfChangedStateValue("rgbOwned", true)
        } else if (state.rgbOwned == true) {
            logInfo "Turning off AQI RGB Light (${rgbLight.displayName}) - Mode/AQI condition clear."
            try { rgbLight.off() } catch (Exception e) { logError "Failed to turn off RGB light: ${e.message}" }
            sendIfChangedStateValue("rgbOwned", false)
        }
    }

    String actualFilterState = "N/A"
    if (airFilterEnabled && airFilterSwitch) {
        Boolean shouldFilterBeOn = aqiEnabled ? aqiStatus.filterOn : false
        String currentFilterState = safeGetDeviceAttribute(airFilterSwitch, "switch")

        try {
            if (shouldFilterBeOn && currentFilterState != "on") {
                logInfo "Turning Air Filter ON due to elevated AQI (${calculatedAqi})"
                airFilterSwitch.on()
                sendIfChangedStateValue("filterOwned", true)
            } else if (!shouldFilterBeOn && currentFilterState != "off" && state.filterOwned == true) {
                logInfo "Turning Air Filter OFF (AQI Normal: ${calculatedAqi})"
                airFilterSwitch.off()
                sendIfChangedStateValue("filterOwned", false)
            }
        } catch (Exception e) {
            logError "Failed to execute Air Filter command: ${e.message}"
        }

        actualFilterState = safeGetDeviceAttribute(airFilterSwitch, "switch")?.toUpperCase() ?: "OFF"
    }

    Map context = [
        schedule: sched,
        thermostat1: thermostat1,
        thermostat2: thermostat2,
        aqi: calculatedAqi,
        filterState: actualFilterState
    ]
    
    updateTile(context)
}

// =================================================================================================
// 3. SCHEDULING & EVENT HANDLER ENGINE (Mode Manager Engine)
// =================================================================================================

private int getBoundaryMinutes(String settingKey, String defaultTime) {
    def timeVal = settings[settingKey] ?: defaultTime
    if (!timeVal) return 0
    
    try {
        String isoStr = timeVal.toString()
        Date parsedDate = null
        if (isoStr.contains("T")) {
            parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(isoStr.substring(0, 19))
        } else if (isoStr.contains(":")) {
            parsedDate = new java.text.SimpleDateFormat("HH:mm").parse(isoStr)
        }

        if (parsedDate != null) {
            Calendar cal = Calendar.getInstance(location.timeZone ?: TimeZone.getDefault())
            cal.setTime(parsedDate)
            return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
        }
    } catch (Exception e) {
        logError "Failed to parse time boundary setting '${settingKey}' (${timeVal}): ${e.message}"
    }

    return 0
}

private void scheduleTimeBoundaries() {
    unschedule("evaluateSchedule")
    
    List<Integer> mins = [
        getBoundaryMinutes("mStartTime", "2026-01-01T06:00:00.000-0000"),
        getBoundaryMinutes("dStartTime", "2026-01-01T09:00:00.000-0000"),
        getBoundaryMinutes("eStartTime", "2026-01-01T17:00:00.000-0000"),
        getBoundaryMinutes("nStartTime", "2026-01-01T22:00:00.000-0000")
    ]
    
    mins.unique().each { minVal ->
        int hrs = (minVal / 60) as int
        int m = minVal % 60
        String cronExpr = String.format("0 %d %d * * ? *", m, hrs)
        try {
            schedule(cronExpr, "evaluateSchedule")
            logDebug "Scheduled period boundary CRON: '${cronExpr}'"
        } catch (Exception e) {
            logError "Failed to schedule period boundary CRON '${cronExpr}': ${e.message}"
        }
    }
}

private Map getCalculatedScheduleData() {
    Calendar nowCal = Calendar.getInstance(location.timeZone ?: TimeZone.getDefault())
    Integer currentMin = (nowCal.get(Calendar.HOUR_OF_DAY) * 60) + nowCal.get(Calendar.MINUTE)

    String activePeriod = ""
    BigDecimal d1TargetHeat = null
    BigDecimal d1TargetCool = null
    BigDecimal d2TargetHeat = null
    BigDecimal d2TargetCool = null
    Boolean d2ForceOff = false

    Boolean isAway = presenceSensor ? (safeGetDeviceAttribute(presenceSensor, "presence") == "not present") : false
    Boolean isSleeping = sleepSwitch ? (safeGetDeviceAttribute(sleepSwitch, "switch") == "on") : false

    if (isAway) {
        activePeriod = "Presence Override"
        d2ForceOff = true // Hardcoded non-configurable safety isolation
        if (getSettingBool("awayDev1Enabled", true)) {
            d1TargetHeat = safeToDecimal(settings.awayDev1Heat, 62.0)
            d1TargetCool = safeToDecimal(settings.awayDev1Cool, 78.0)
        }
    } else if (isSleeping) {
        activePeriod = "Sleeping Override"
        d2ForceOff = true // Hardcoded non-configurable safety isolation
        if (getSettingBool("sleepDev1Enabled", true)) {
            d1TargetHeat = safeToDecimal(settings.sleepDev1Heat, 65.0)
            d1TargetCool = safeToDecimal(settings.sleepDev1Cool, 74.0)
        }
    } else {
        List<Map> periods = [
            [name: "Morning", mins: getBoundaryMinutes("mStartTime", "2026-01-01T06:00:00.000-0000")],
            [name: "Day",     mins: getBoundaryMinutes("dStartTime", "2026-01-01T09:00:00.000-0000")],
            [name: "Evening", mins: getBoundaryMinutes("eStartTime", "2026-01-01T17:00:00.000-0000")],
            [name: "Night",   mins: getBoundaryMinutes("nStartTime", "2026-01-01T22:00:00.000-0000")]
        ]
        
        periods.sort { it.mins }
        
        Map matched = null
        for (int i = periods.size() - 1; i >= 0; i--) {
            if (currentMin >= periods[i].mins) {
                matched = periods[i]
                break
            }
        }
        if (!matched) matched = periods.last()

        activePeriod = matched.name

        switch(activePeriod) {
            case "Morning":
                d1TargetHeat = safeToDecimal(settings.d1MorningHeat, 68.0)
                d1TargetCool = safeToDecimal(settings.d1MorningCool, 72.0)
                d2TargetHeat = safeToDecimal(settings.d2MorningHeat, 68.0)
                d2TargetCool = safeToDecimal(settings.d2MorningCool, 72.0)
                break
            case "Day":
                d1TargetHeat = safeToDecimal(settings.d1DayHeat, 66.0)
                d1TargetCool = safeToDecimal(settings.d1DayCool, 75.0)
                d2TargetHeat = safeToDecimal(settings.d2DayHeat, 66.0)
                d2TargetCool = safeToDecimal(settings.d2DayCool, 75.0)
                break
            case "Evening":
                d1TargetHeat = safeToDecimal(settings.d1EveningHeat, 69.0)
                d1TargetCool = safeToDecimal(settings.d1EveningCool, 73.0)
                d2TargetHeat = safeToDecimal(settings.d2EveningHeat, 69.0)
                d2TargetCool = safeToDecimal(settings.d2EveningCool, 73.0)
                break
            case "Night":
                d1TargetHeat = safeToDecimal(settings.d1NightHeat, 64.0)
                d1TargetCool = safeToDecimal(settings.d1NightCool, 75.0)
                d2TargetHeat = safeToDecimal(settings.d2NightHeat, 62.0)
                d2TargetCool = safeToDecimal(settings.d2NightCool, 78.0)
                break
        }
    }

    return [
        activePeriod: activePeriod,
        d1TargetHeat: d1TargetHeat, d1TargetCool: d1TargetCool,
        d2TargetHeat: d2TargetHeat, d2TargetCool: d2TargetCool,
        d2ForceOff: d2ForceOff
    ]
}

def modeChangeHandler(evt) { executeEvaluationPass("Hub Mode Changed (${evt?.value})", false) }
def overrideSwitchHandler(evt) { 
    executeEvaluationPass("Override Attribute Changed (${evt?.device?.displayName} = ${evt?.value})", false) 
    if (fanCirculateEnabled && sleepSwitch && evt?.device?.id == sleepSwitch.id) {
        logInfo "Sleeping switch state change detected. Recalculating fan circulation schedule."
        manageFanCirculation(false)
    }
}
def aqiHandler(evt) { executeEvaluationPass("AQI Sensor Updated (${evt?.device?.displayName} = ${evt?.value})", false) }
def airFilterSwitchHandler(evt) { executeEvaluationPass("Air Filter Switch State Changed (${evt?.value})", false) }
def temperatureHandler(evt) { executeEvaluationPass("Thermostat Ambient Temperature Updated (${evt?.device?.displayName})", false) }
def evaluateSchedule() { executeEvaluationPass("Scheduled Window Boundary Trigger", false) }

// =================================================================================================
// 4. THERMOSTAT CONTROL & AUTO-CHANGEOVER ENGINE
// =================================================================================================

private Map getThermostatStateSnapshot(Object dev) {
    if (!dev) return [:]
    
    // Inspect physicalThermostatMode first to correctly evaluate locked-Auto driver states
    Object physMode = safeGetDeviceAttribute(dev, "physicalThermostatMode")
    Object stdMode = safeGetDeviceAttribute(dev, "thermostatMode")
    Object effectiveMode = (physMode != null && physMode.toString().trim() != "") ? physMode : stdMode

    return [
        temp: safeGetDeviceAttribute(dev, "temperature"),
        heat: safeGetDeviceAttribute(dev, "heatingSetpoint"),
        cool: safeGetDeviceAttribute(dev, "coolingSetpoint"),
        mode: effectiveMode,
        fan: safeGetDeviceAttribute(dev, "thermostatFanMode"),
        operatingState: safeGetDeviceAttribute(dev, "thermostatOperatingState")
    ]
}

private void applySetpointsAndEvaluateMode(Object dev, BigDecimal targetHeat, BigDecimal targetCool, Map thermostatSnapshot = null, Boolean autoChangeover = true, Object hysteresisBuffer = 2, Boolean isOffice = false, Boolean forceSetpointApply = false) {
    if (!dev || targetHeat == null || targetCool == null) return
    
    Map snapshot = thermostatSnapshot ?: getThermostatStateSnapshot(dev)
    BigDecimal currentHeatSp = safeToDecimal(snapshot.heat)
    BigDecimal currentCoolSp = safeToDecimal(snapshot.cool)
    BigDecimal currentTemp = safeToDecimal(snapshot.temp)
    String currentMode = snapshot.mode?.toLowerCase() ?: "off"

    String scale = location.temperatureScale ?: "F"

    try {
        if (forceSetpointApply || currentHeatSp != targetHeat) {
            logInfo "Setting ${dev.displayName} Heating Setpoint to ${targetHeat} °${scale}"
            dev.setHeatingSetpoint(targetHeat)
        }

        if (forceSetpointApply || currentCoolSp != targetCool) {
            logInfo "Setting ${dev.displayName} Cooling Setpoint to ${targetCool} °${scale}"
            dev.setCoolingSetpoint(targetCool)
        }
    } catch (Exception e) {
        logError "Failed to apply setpoints to ${dev.displayName}: ${e.message}"
    }

    if (autoChangeover && currentTemp != null) {
        BigDecimal deadband = safeToDecimal(hysteresisBuffer, 2.0)
        String guardKey = isOffice ? "dev2ModeChangePending" : "dev1ModeChangePending"
        
        if (state[guardKey] == true) {
            logDebug "Auto-Changeover (${dev.displayName}): Execution pending. Skipping pass."
            return
        }

        try {
            if (currentMode == "cool" && currentTemp <= targetHeat) {
                logInfo "Auto-Changeover (${dev.displayName}): Temp (${currentTemp} °${scale}) <= Heat Setpoint (${targetHeat} °${scale}). Changing to HEAT."
                state[guardKey] = true
                runIn(10, isOffice ? "clearDev2ModeGuard" : "clearDev1ModeGuard")
                dev.heat()
            } else if (currentMode == "heat" && currentTemp >= targetCool) {
                logInfo "Auto-Changeover (${dev.displayName}): Temp (${currentTemp} °${scale}) >= Cool Setpoint (${targetCool} °${scale}). Changing to COOL."
                state[guardKey] = true
                runIn(10, isOffice ? "clearDev2ModeGuard" : "clearDev1ModeGuard")
                dev.cool()
            } else if (currentMode == "off" || currentMode == "auto") {
                if (currentTemp <= (targetHeat - deadband)) {
                    logInfo "Auto-Changeover (${dev.displayName}): Temp (${currentTemp} °${scale}) breached Heat Deadband. Setting to HEAT."
                    state[guardKey] = true
                    runIn(10, isOffice ? "clearDev2ModeGuard" : "clearDev1ModeGuard")
                    dev.heat()
                } else if (currentTemp >= (targetCool + deadband)) {
                    logInfo "Auto-Changeover (${dev.displayName}): Temp (${currentTemp} °${scale}) breached Cool Deadband. Setting to COOL."
                    state[guardKey] = true
                    runIn(10, isOffice ? "clearDev2ModeGuard" : "clearDev1ModeGuard")
                    dev.cool()
                }
            }
        } catch (Exception e) {
            logError "Failed auto-changeover on ${dev.displayName}: ${e.message}"
            state[guardKey] = false
        }
    }
}

void clearDev1ModeGuard() { state.dev1ModeChangePending = false }
void clearDev2ModeGuard() { state.dev2ModeChangePending = false }

// =================================================================================================
// 5. FAN CIRCULATION ENGINE
// =================================================================================================

def manageFanCirculation(Boolean isInitial = false, String targetPhase = null) {
    if (!fanCirculateEnabled || !targetThermostat) return

    unschedule("toggleFanCirculation")

    Boolean isSleeping = sleepSwitch ? (safeGetDeviceAttribute(sleepSwitch, "switch") == "on") : false

    Integer safeOn = isSleeping ? safeFanDuration(settings.fanSleepOnMinutes, 5) : safeFanDuration(settings.fanOnMinutes, 10)
    Integer safeOff = isSleeping ? safeFanDuration(settings.fanSleepOffMinutes, 115) : safeFanDuration(settings.fanOffMinutes, 50)

    Integer onTime = safeOn * 60
    Integer offTime = safeOff * 60

    Map thermostat = getThermostatStateSnapshot(targetThermostat)
    String opState = thermostat.operatingState?.toLowerCase() ?: "idle"

    Boolean executeOnPhase = (targetPhase != null) ? (targetPhase == "ON") : (isInitial || state.fanPhase == "OFF Phase" || !state.fanPhase)

    try {
        if (executeOnPhase) {
            sendIfChangedStateValue("fanPhase", "ON Phase")
            if (opState != "heating" && opState != "cooling") {
                logInfo "Fan Circulation Control (${isSleeping ? 'Sleeping Mode Circulation Period' : 'Standard Circulation Period'}): Turning fan ON for ${safeOn} minutes."
                targetThermostat.fanOn()
                sendIfChangedStateValue("fanOwned", true)
            } else {
                sendIfChangedStateValue("fanOwned", false)
            }
            runIn(onTime, "toggleFanCirculation", [data: [nextPhase: "OFF"]])
        } else {
            sendIfChangedStateValue("fanPhase", "OFF Phase")
            if (state.fanOwned == true) {
                logInfo "Fan Circulation Control (${isSleeping ? 'Sleeping Mode Circulation Period' : 'Standard Circulation Period'}): Returning fan to AUTO for ${safeOff} minutes."
                targetThermostat.fanAuto()
                sendIfChangedStateValue("fanOwned", false)
            } else {
                logDebug "Fan Circulation Control: Fan not MEM-owned; leaving current fan control unchanged for ${safeOff} minutes."
            }
            runIn(offTime, "toggleFanCirculation", [data: [nextPhase: "ON"]])
        }
    } catch (Exception e) {
        logError "Failed fan circulation command: ${e.message}"
    }

    updateTile()
}

def toggleFanCirculation(data) {
    if (!fanCirculateEnabled) return
    String nextPhase = data?.nextPhase ?: "ON"
    manageFanCirculation(false, nextPhase)
}

// =================================================================================================
// 6. AQI ENGINE
// =================================================================================================

private Boolean validateAqiThresholds() {
    Integer g = safeToInt(settings?.aqiGoodMax, 50)
    Integer m = safeToInt(settings?.aqiModMax, 100)
    Integer s = safeToInt(settings?.aqiSensMax, 150)
    Integer u = safeToInt(settings?.aqiUnhealthyMax, 200)
    Integer vu = safeToInt(settings?.aqiVeryUnhealthyMax, 300)

    return (g > 0 && m > g && s > m && u > s && vu > u)
}

private Map getAqiTierLimits() {
    if (validateAqiThresholds()) {
        return [
            good: safeToInt(settings?.aqiGoodMax, 50),
            mod: safeToInt(settings?.aqiModMax, 100),
            sens: safeToInt(settings?.aqiSensMax, 150),
            unhealthy: safeToInt(settings?.aqiUnhealthyMax, 200),
            veryUnhealthy: safeToInt(settings?.aqiVeryUnhealthyMax, 300)
        ]
    } else {
        return [good: 50, mod: 100, sens: 150, unhealthy: 200, veryUnhealthy: 300]
    }
}

private Map readSensors() {
    def v1 = safeGetDeviceAttribute(aqiSensor1, "airQualityIndex")
    def v2 = safeGetDeviceAttribute(aqiSensor2, "airQualityIndex")
    return [v1: v1, v2: v2]
}

private Integer calculateAverage(Object val1, Object val2) {
    Integer v1 = safeAqiValue(val1)
    Integer v2 = safeAqiValue(val2)

    if (v1 == null && v2 == null) return null
    if (v1 == null) return v2
    if (v2 == null) return v1

    return Math.round((v1 + v2) / 2.0) as Integer
}

private Map determineStatus(Object avgAqiVal) {
    if (avgAqiVal == null || !(avgAqiVal instanceof Integer)) {
        return [status: "Not Evaluated", tileStatus: "Not Evaluated", colorMap: null, actionText: "N/A", filterOn: false]
    }
    
    Integer avgAqi = avgAqiVal as Integer
    Map limits = getAqiTierLimits()
    Integer tGoodMax = safeToInt(limits?.good, 50)
    Integer tModMax = safeToInt(limits?.mod, 100)
    Integer tSensMax = safeToInt(limits?.sens, 150)
    Integer tUnhealthMax = safeToInt(limits?.unhealthy, 200)
    Integer tVeryUnhealthMax = safeToInt(limits?.veryUnhealthy, 300)

    if (avgAqi <= tGoodMax) {
        return [status: "Green (Good)", tileStatus: "Good (Normal)", colorMap: null, actionText: "Satisfactory air quality. Little to no risk.", filterOn: false]
    } else if (avgAqi <= tModMax) {
        return [status: "Yellow (Moderate)", tileStatus: "Moderate", colorMap: [hue: 16, saturation: 100, level: 100], actionText: "Acceptable. Sensitive groups may feel minor symptoms.", filterOn: true]
    } else if (avgAqi <= tSensMax) {
        return [status: "Orange (Unhealthy for Sensitive Groups)", tileStatus: "Sensitive Groups", colorMap: [hue: 8, saturation: 100, level: 100], actionText: "Potentially harmful for kids, elderly, and asthmatics.", filterOn: true]
    } else if (avgAqi <= tUnhealthMax) {
        return [status: "Red (Unhealthy)", tileStatus: "Unhealthy", colorMap: [hue: 0, saturation: 100, level: 100], actionText: "Active danger. Everyone may experience adverse effects.", filterOn: true]
    } else if (avgAqi <= tVeryUnhealthMax) {
        return [status: "Purple (Very Unhealthy)", tileStatus: "Very Unhealthy", colorMap: [hue: 75, saturation: 100, level: 100], actionText: "Health alert. High risk of respiratory irritation.", filterOn: true]
    } else {
        return [status: "Maroon (Hazardous)", tileStatus: "Hazardous", colorMap: [hue: 95, saturation: 100, level: 50], actionText: "Emergency conditions. Stay entirely indoors.", filterOn: true]
    }
}

// =================================================================================================
// 7. DASHBOARD TILE ENGINE
// =================================================================================================

private void ensureChildDevice() {
    String childDni = "MEM_TILE_${app.id}"
    def child = getChildDevice(childDni)
    if (!child) {
        logInfo "Creating Child Tile Device (${childDni})"
        try {
            addChildDevice("jshimota", "MEM Dashboard Tile", childDni, [
                name: "MEM Tile Device",
                label: "${app.label ?: 'Multiperiod Environment Manager'} Tile",
                isComponent: true
            ])
        } catch (Exception e) {
            logError "Failed to create child tile device '${childDni}': ${e.message}"
        }
    }
}

private Map buildEvaluationContext(Integer overrideAvgAqi = null, String filterOverride = null) {
    Map sched = getCalculatedScheduleData()
    Map t1 = getThermostatStateSnapshot(targetThermostat)
    Map t2 = getThermostatStateSnapshot(targetThermostat2)
    
    def raw1 = null
    def raw2 = null
    Integer calculatedAqi = overrideAvgAqi
    if (aqiEnabled && (aqiSensor1 || aqiSensor2)) {
        Map rawVals = readSensors()
        raw1 = rawVals.v1
        raw2 = rawVals.v2
        if (calculatedAqi == null) calculatedAqi = calculateAverage(raw1, raw2)
    }

    String currentFilterState = "N/A"
    if (airFilterEnabled && airFilterSwitch) {
        currentFilterState = filterOverride ?: (safeGetDeviceAttribute(airFilterSwitch, "switch")?.toUpperCase() ?: "OFF")
    }

    return [
        schedule: sched,
        thermostat1: t1,
        thermostat2: t2,
        aqi: calculatedAqi,
        rawV1: raw1,
        rawV2: raw2,
        filterState: currentFilterState
    ]
}

private String buildTileHtml(Map ctx = null) {
    if (ctx == null) ctx = buildEvaluationContext()

    Map sched = ctx.schedule ?: [:]
    String activePeriod = state.activeSet ?: (sched.activePeriod ?: "N/A")
    
    Map t1 = ctx.thermostat1 ?: [:]
    def t1Temp = t1.temp != null ? "${t1.temp} °F" : "--"

    Map t2 = ctx.thermostat2 ?: [:]
    def t2Mode = sched.d2ForceOff ? "OFF" : (t2.mode ?: "--")

    String hTargetStr = (sched.d1TargetHeat != null && sched.d1TargetCool != null) ? "H:${sched.d1TargetHeat?.setScale(0, BigDecimal.ROUND_HALF_UP)}/C:${sched.d1TargetCool?.setScale(0, BigDecimal.ROUND_HALF_UP)}" : "N/A"
    String oTargetStr = sched.d2ForceOff ? "OFF" : ((sched.d2TargetHeat != null && sched.d2TargetCool != null) ? "H:${sched.d2TargetHeat?.setScale(0, BigDecimal.ROUND_HALF_UP)}/C:${sched.d2TargetCool?.setScale(0, BigDecimal.ROUND_HALF_UP)}" : "N/A")
    
    String avgAqiStr = "N/A"
    String aqiStatusStr = "Disabled"
    String aqiColorCss = "#888888"
    
    Integer avgAqi = ctx.aqi as Integer
    if (avgAqi != null) {
        avgAqiStr = "${avgAqi}"
        Map statusInfo = determineStatus(avgAqi)
        aqiStatusStr = statusInfo.tileStatus ?: "N/A"
        
        if (aqiStatusStr.contains("Good")) aqiColorCss = "#008000"
        else if (aqiStatusStr.contains("Moderate")) aqiColorCss = "#b8860b"
        else if (aqiStatusStr.contains("Sensitive")) aqiColorCss = "#ff8c00"
        else if (aqiStatusStr.contains("Very Unhealthy")) aqiColorCss = "#800080"
        else if (aqiStatusStr.contains("Unhealthy")) aqiColorCss = "#ff0000"
        else if (aqiStatusStr.contains("Hazardous")) aqiColorCss = "#800000"
    }

    StringBuilder tileSb = new StringBuilder()
    tileSb.append("<div style='font-size:.42em; line-height:1.2; text-align:center; padding:2px;'>")
    tileSb.append("<b>MEM Period:</b> ").append(activePeriod).append("<br/>")
    tileSb.append("<b>House (${t1Temp}):</b> ").append(hTargetStr).append("<br/>")
    tileSb.append("<b>Office (${t2Mode}):</b> ").append(oTargetStr).append("<br/>")
    tileSb.append("<div style='font-size:.3em;'><b>AQI:</b> <span style='color:").append(aqiColorCss)
          .append("; font-weight:bold;'>").append(avgAqiStr).append(" ").append(aqiStatusStr).append("</span></div>")
    tileSb.append("</div>")

    return tileSb.toString()
}

private void updateTile(Map context = null) {
    def child = getChildDevice("MEM_TILE_${app.id}")
    if (!child) return

    Map ctx = context ?: buildEvaluationContext()
    String tileHtml = buildTileHtml(ctx)
    sendIfChangedAttributeValue(child, "memTile", tileHtml)
}

// =================================================================================================
// 8. LOGGING ENGINE
// =================================================================================================

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appLabel = app.label ?: app.name ?: "App"

    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${appLabel}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    def val = settings[key]
    if (val == null) return defaultVal
    if (val instanceof Boolean) return val
    return val.toString().toBoolean()
}

// =================================================================================================
// 9. UTILITY & VALIDATION ENGINE
// =================================================================================================

private Map validateConfiguration() {
    List errors = []
    List warnings = []

    if (getSettingBool("dev1AppEnabled", false) && !targetThermostat) {
        errors.add("House HVAC Controller/Thermostat Device selection is required before enabling House Thermostat automations.")
    }

    if (getSettingBool("dev2AppEnabled", false) && !targetThermostat2) {
        errors.add("Office HVAC Controller/Thermostat Device selection is required before enabling Office Thermostat automations.")
    }

    if (getSettingBool("awayDev1Enabled", true) && !presenceSensor) {
        warnings.add("House Presence Override control is enabled, but no Presence Sensor has been selected.")
    }

    int mMin = getBoundaryMinutes("mStartTime", "2026-01-01T06:00:00.000-0000")
    int dMin = getBoundaryMinutes("dStartTime", "2026-01-01T09:00:00.000-0000")
    int eMin = getBoundaryMinutes("eStartTime", "2026-01-01T17:00:00.000-0000")
    int nMin = getBoundaryMinutes("nStartTime", "2026-01-01T22:00:00.000-0000")

    if (!(mMin < dMin && dMin < eMin && eMin < nMin)) {
        warnings.add("Schedule period boundary times are not in chronological order (Morning < Day < Evening < Night). Periods will be evaluated by their actual clock times.")
    }

    List<Map> periodCheck = [
        [name: "Morning", mins: mMin],
        [name: "Day",     mins: dMin],
        [name: "Evening", mins: eMin],
        [name: "Night",   mins: nMin]
    ]

    Map<Integer, List<String>> minuteMap = [:]
    periodCheck.each { p ->
        if (!minuteMap.containsKey(p.mins)) minuteMap[p.mins] = []
        minuteMap[p.mins].add(p.name)
    }

    minuteMap.each { mins, names ->
        if (names.size() > 1) {
            int hrs = (mins / 60) as int
            int m = mins % 60
            String timeFormatted = String.format("%02d:%02d", hrs, m)
            warnings.add("${names.join(' and ')} share the exact boundary time (${timeFormatted}). Evaluation order may be ambiguous.")
        }
    }

    if (fanCirculateEnabled) {
        Integer onMin = safeToInt(settings.fanOnMinutes, 10)
        Integer offMin = safeToInt(settings.fanOffMinutes, 50)
        Integer sleepOnMin = safeToInt(settings.fanSleepOnMinutes, 5)
        Integer sleepOffMin = safeToInt(settings.fanSleepOffMinutes, 115)
        
        if (onMin < 1 || onMin > 240) errors.add("Standard Fan ON duration must be between 1 and 240 minutes.")
        if (offMin < 1 || offMin > 240) errors.add("Standard Fan OFF duration must be between 1 and 240 minutes.")
        if (sleepOnMin < 1 || sleepOnMin > 240) errors.add("Sleeping Fan ON duration must be between 1 and 240 minutes.")
        if (sleepOffMin < 1 || sleepOffMin > 240) errors.add("Sleeping Fan OFF duration must be between 1 and 240 minutes.")
    }

    if (aqiEnabled) {
        if (!aqiSensor1) errors.add("Primary AQI Sensor is required when Air Quality Monitoring is enabled.")
        if (!validateAqiThresholds()) errors.add("AQI Tier Thresholds must be strictly positive and strictly ascending.")
    }

    if (airFilterEnabled && !airFilterSwitch) {
        errors.add("Room Air Filter Switch/Device is required when Automated Room Air Filter Control is enabled.")
    }

    return [valid: errors.isEmpty(), errors: errors, warnings: warnings]
}

private void releaseDeviceOwnership() {
    if (state.fanOwned == true) {
        logInfo "Releasing owned Thermostat Fan state to AUTO."
        try { targetThermostat?.fanAuto() } catch (Exception e) { logError "Failed to reset fan to AUTO: ${e.message}" }
        sendIfChangedStateValue("fanOwned", false)
    }

    if (state.rgbOwned == true && rgbLight) {
        logInfo "Releasing owned RGB Indicator Light (${rgbLight.displayName}) to OFF."
        try { rgbLight.off() } catch (Exception e) { logError "Failed to turn off RGB light: ${e.message}" }
        sendIfChangedStateValue("rgbOwned", false)
    }

    if (state.filterOwned == true && airFilterSwitch) {
        logInfo "Releasing owned Air Filter Switch (${airFilterSwitch.displayName}) to OFF."
        try { airFilterSwitch.off() } catch (Exception e) { logError "Failed to turn off Air Filter: ${e.message}" }
        sendIfChangedStateValue("filterOwned", false)
    }
}

private Integer safeToInt(Object val, Integer defaultVal = 0) {
    if (val == null) return defaultVal
    try {
        String str = val.toString().trim()
        return str.length() > 0 ? str.toInteger() : defaultVal
    } catch (Exception e) { return defaultVal }
}

private BigDecimal safeToDecimal(Object val, BigDecimal defaultVal = null) {
    if (val == null) return defaultVal
    try {
        String str = val.toString().trim()
        return str.length() > 0 ? str.toBigDecimal() : defaultVal
    } catch (Exception e) { return defaultVal }
}

private Integer safeFanDuration(Object val, Integer defaultVal = 30) {
    Integer duration = safeToInt(val, defaultVal)
    if (duration < 1) return 1
    if (duration > 240) return 240
    return duration
}

private Integer safeAqiValue(Object value) {
    if (value == null) return null
    try {
        return value.toString().trim().toBigDecimal().setScale(0, BigDecimal.ROUND_HALF_UP).intValue()
    } catch (Exception e) { return null }
}

private Object safeGetDeviceAttribute(Object device, String attributeName) {
    if (!device) return null
    try { return device.currentValue(attributeName) } catch (Exception e) { return null }
}

private void sendIfChangedStateValue(String key, Object value) {
    if (!key) return
    String oldVal = state[key]?.toString()
    String newVal = value != null ? value.toString() : ""
    if (oldVal != newVal) state[key] = value
}

private void sendIfChangedAttributeValue(Object device, String attributeName, Object value) {
    if (!device || !attributeName) return
    try {
        String oldVal = device.currentValue(attributeName as String)?.toString()
        String newVal = value != null ? value.toString() : ""
        if (oldVal != newVal) device.sendEvent(name: attributeName, value: value)
    } catch (Exception e) { logError "Failed attribute update: ${e.message}" }
}