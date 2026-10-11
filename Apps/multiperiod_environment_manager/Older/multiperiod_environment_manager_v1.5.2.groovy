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
 *	v1.5.2	  09/02/26	  jshimota	  Re-aligned Heat (left) and Cool (right) setpoint columns across all sections to match tile output. Refined Section 4 terminology to Presence & Sleeping Overrides. Clarified Force Off descriptions. Integrated JavaScript-bound HTML spinners across schedule tables, overrides, and fan circulation. Added live active target setpoints to Dashboard Tile output. Restored settings as authoritative configuration.
 *	v1.5.1	  09/02/26	  jshimota	  Restored native Hubitat number inputs structured side-by-side with width-6 controls to guarantee settings persistence on Done while keeping spinner functionality.
 *	v1.4.9	  09/02/26	  jshimota	  Forced immediate setpoint re-application to physical thermostats and dashboard tiles whenever Done is clicked in GUI.
 *	v1.4.8	  09/02/26	  jshimota	  Updated UI header label to Units. Restored setpoint matrix HTML number spinners decoupled from live updates. Added clear guidance for Enable Setpoint Scheduling toggles. Integrated sleeping mode fan circulation schedule with revised defaults.
 *	v1.4.7	  09/01/26	  jshimota	  Overhauled Section 4 layout and purged red formatting. Added 4 explicit enable toggles for House/Office Away/Sleeping overrides, integrated standard setpoint inputs, and added clean Force OFF safety toggles for Office units.
 *	v1.4.6	  09/01/26	  jshimota	  Converted schedule setpoint tables to use native Groovy input controls styled inside layout tables to guarantee Hubitat settings persistence on Done. Relabeled Section 3 to 'Period Schedule Window Boundaries'.
 *	v1.4.5	  09/01/26	  jshimota	  Converted Section 3 to a Smart Table utilizing Hour/Minute/AM-PM selectors modeled after Mode Manager Advanced. Integrated robust time-to-minutes engine and overlapping boundary warnings.
 *	v1.4.4	  09/01/26	  jshimota	  Removed all remaining submitOnChange triggers to eliminate page refreshes and unwanted section collapsing. All configuration changes persist exclusively when Done is clicked.
 *	v1.4.3	  09/01/26	  jshimota	  Fixed configuration validation guard when no device is selected.
 *	v1.4.2	  09/01/26	  jshimota	  Updated schedule table column headers to 'Cooling (if temp exceeds -)' and 'Heating (if temp falls below -)'. Enforced standard page submit model (save on Done).
 *	v1.4.1	  09/01/26	  jshimota	  Decoupled enable switches from section collapse routines. Clarified Mode Changeover guidance. Renamed schedule section headers to 'Setpoints for Periods'.
 *	v1.4.0	  09/01/26	  jshimota	  Converted schedule summary tables to active interactive spinner tables. Removed redundant manual input fields. Decoupled section toggling from device enable switches. Relabeled device switches to 'Enable Home Thermostat' and 'Enable Office Thermostat'. Relocated Enable Scheduling toggles above schedule tables.
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history versions v0 - v1.3.9

static String version() { return '1.5.2' }
def timeStamp() { return "2026/09/02 10:45 AM" }

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
        section("<b>SECTION 1: House HVAC Controller (Primary)</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure settings, automated mode changeover, and time schedules for the main House HVAC Controller.</div>"

            input name: "targetThermostat", type: "capability.thermostat", title: "<b>Select House HVAC Controller Device</b>", required: true, multiple: false, submitOnChange: false
            input name: "dev1AppEnabled", type: "bool", title: "<b>Enable Home Thermostat Control</b>", defaultValue: true, submitOnChange: false
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev1AutoChangeoverEnabled", type: "bool", title: "<b>Enable Automatic Mode Changeover (Heat / Cool Switch)</b>", defaultValue: true, submitOnChange: false
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:6px;'>" +
                      "<i>When enabled, MEM automatically toggles the thermostat mode between Heat and Cool based on ambient room temperature readings and setpoint boundaries.</i></div>"
            
            input name: "dev1TempDeadband", type: "number", title: "<b>House Temperature Hysteresis Buffer (°F)</b>", defaultValue: 2, required: true, width: 4
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:6px;'><i>Degree offset required beyond opposing setpoint before triggering mode change.</i></div>"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev1ScheduleEnabled", type: "bool", title: "<b>Enable Setpoint Scheduling</b>", defaultValue: true, submitOnChange: false
            paragraph "<div style='color:#2980B9; font-size:11px; margin-top:-6px; margin-bottom:8px;'>" +
                      "<b>When disabled:</b> Scheduled target setpoints are suspended. The House HVAC unit holds its existing setpoints unless overridden by Presence/Sleeping mode switches.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Setpoints for Periods</span>"
            
            // Render Bound HTML Table Matrix for House Setpoints (Heat Left, Cool Right)
            renderBoundMatrixTable("House HVAC Period Setpoint Matrix", "d1", [
                [name: "Morning", heatKey: "d1MorningHeat", coolKey: "d1MorningCool", defaultHeat: 68, defaultCool: 72],
                [name: "Day",     heatKey: "d1DayHeat",     coolKey: "d1DayCool",     defaultHeat: 66, defaultCool: 75],
                [name: "Evening", heatKey: "d1EveningHeat", coolKey: "d1EveningCool", defaultHeat: 69, defaultCool: 73],
                [name: "Night",   heatKey: "d1NightHeat",   coolKey: "d1NightCool",   defaultHeat: 64, defaultCool: 75]
            ])
        }

        /* Section 2: Office HVAC Controller Configuration */
        section("<b>SECTION 2: Office HVAC Controller (Secondary / Floor Heater)</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure settings, automated mode changeover, and time schedules for the Office HVAC Controller (Floor Heater).</div>"

            input name: "targetThermostat2", type: "capability.thermostat", title: "<b>Select Office HVAC Controller Device</b>", required: false, multiple: false, submitOnChange: false
            input name: "dev2AppEnabled", type: "bool", title: "<b>Enable Office Thermostat Control</b>", defaultValue: false, submitOnChange: false
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev2AutoChangeoverEnabled", type: "bool", title: "<b>Enable Automatic Mode Changeover (Heat / Cool Switch)</b>", defaultValue: false, submitOnChange: false
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:6px;'>" +
                      "<i>When enabled, MEM automatically toggles the thermostat mode between Heat and Cool based on ambient room temperature readings and setpoint boundaries.</i></div>"

            input name: "dev2TempDeadband", type: "number", title: "<b>Office Temperature Hysteresis Buffer (°F)</b>", defaultValue: 2, required: true, width: 4
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:6px;'><i>Degree offset required beyond opposing setpoint before triggering mode change.</i></div>"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev2ScheduleEnabled", type: "bool", title: "<b>Enable Setpoint Scheduling</b>", defaultValue: true, submitOnChange: false
            paragraph "<div style='color:#2980B9; font-size:11px; margin-top:-6px; margin-bottom:8px;'>" +
                      "<b>When disabled:</b> Scheduled target setpoints are suspended. The Office HVAC unit holds its existing setpoints unless overridden by Presence/Sleeping modes or Safety Forced-OFF triggers.</div>"

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Setpoints for Periods</span>"

            // Render Bound HTML Table Matrix for Office Setpoints (Heat Left, Cool Right)
            renderBoundMatrixTable("Office HVAC Period Setpoint Matrix", "d2", [
                [name: "Morning", heatKey: "d2MorningHeat", coolKey: "d2MorningCool", defaultHeat: 68, defaultCool: 72],
                [name: "Day",     heatKey: "d2DayHeat",     coolKey: "d2DayCool",     defaultHeat: 66, defaultCool: 75],
                [name: "Evening", heatKey: "d2EveningHeat", coolKey: "d2EveningCool", defaultHeat: 69, defaultCool: 73],
                [name: "Night",   heatKey: "d2NightHeat",   coolKey: "d2NightCool",   defaultHeat: 62, defaultCool: 78]
            ])
        }

        /* Section 3: Period Schedule Window Boundaries */
        section("<b>SECTION 3: Period Schedule Window Boundaries</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Set start boundary times for each chronological schedule period using the Smart Table below. <i>(Applies to both House and Office HVAC controllers).</i></div>"

            Map val = validateConfiguration()
            if (val.warnings && !val.warnings.isEmpty()) {
                StringBuilder sb = new StringBuilder()
                sb.append("<div style='color:#8a6d3b; background-color:#fcf8e3; padding:8px; border:1px solid #faebcc; border-radius:4px; margin-bottom:10px;'><b>SCHEDULE VALIDATION NOTICE:</b><ul>")
                val.warnings.each { w -> sb.append("<li>").append(w).append("</li>") }
                sb.append("</ul></div>")
                paragraph sb.toString()
            }

            List hourOpts = ["01","02","03","04","05","06","07","08","09","10","11","12"]
            List minOpts  = ["00","05","10","15","20","25","30","35","40","45","50","55"]
            List amPmOpts = ["AM","PM"]

            String selectStyle = "padding:3px; font-size:13px; border:1px solid #ccc; border-radius:4px; background-color:#FFF;"

            StringBuilder boundaryTable = new StringBuilder()
            boundaryTable.append("<table style='width:100%; border-collapse:collapse; font-size:13px; margin:8px 0; text-align:left;'>")
            boundaryTable.append("<tr style='border-bottom:2px solid #2C3E50; background-color:#F2F4F4;'>")
            boundaryTable.append("<th style='padding:6px; width:30%;'>Schedule Period</th>")
            boundaryTable.append("<th style='padding:6px; width:70%;'>Start Time Selector (Hour : Minute : AM/PM)</th>")
            boundaryTable.append("</tr>")

            List<Map> timeRows = [
                [key: "Morning", prefix: "mStart"],
                [key: "Day",     prefix: "dStart"],
                [key: "Evening", prefix: "eStart"],
                [key: "Night",   prefix: "nStart"]
            ]

            timeRows.each { row ->
                String curH  = settings["${row.prefix}Hour"] ?: (row.key == "Morning" ? "06" : row.key == "Day" ? "09" : row.key == "Evening" ? "05" : "10")
                String curM  = settings["${row.prefix}Min"]  ?: "00"
                String curAP = settings["${row.prefix}AmPm"] ?: (row.key == "Morning" || row.key == "Day" ? "AM" : "PM")

                boundaryTable.append("<tr style='border-bottom:1px solid #E0E0E0;'>")
                boundaryTable.append("<td style='padding:6px;'><b>${row.key} Start</b></td>")
                boundaryTable.append("<td style='padding:6px;'>")
                
                // Hour Dropdown
                boundaryTable.append("<select name='${row.prefix}Hour' style='${selectStyle}'>")
                hourOpts.each { h -> boundaryTable.append("<option value='${h}' ${h == curH ? 'selected' : ''}>${h}</option>") }
                boundaryTable.append(" </select> : ")
                
                // Minute Dropdown
                boundaryTable.append("<select name='${row.prefix}Min' style='${selectStyle}'>")
                minOpts.each { m -> boundaryTable.append("<option value='${m}' ${m == curM ? 'selected' : ''}>${m}</option>") }
                boundaryTable.append(" </select> ")
                
                // AM/PM Dropdown
                boundaryTable.append("<select name='${row.prefix}AmPm' style='${selectStyle}'>")
                amPmOpts.each { ap -> boundaryTable.append("<option value='${ap}' ${ap == curAP ? 'selected' : ''}>${ap}</option>") }
                boundaryTable.append(" </select>")
                
                boundaryTable.append("</td></tr>")
            }

            boundaryTable.append("</table>")
            paragraph boundaryTable.toString()
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY B: SYSTEM OVERRIDES & SAFETY MODES
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY B: SYSTEM OVERRIDES & SAFETY MODES</div>") {}

        /* Section 4: System Overrides */
        section("<b>SECTION 4: System Overrides & Safety Controls</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure setpoints or force devices <b>OFF</b> when Presence or Sleeping switches are active.</div>"

            // --- PRESENCE OVERRIDE GROUP ---
            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #2980B9; margin-bottom:8px;'>Presence Override</div>"
            input name: "awaySwitch", type: "capability.switch", title: "<b>Select Away / Absence Switch</b>", required: false, multiple: false, submitOnChange: false
            
            input name: "awayDev1Enabled", type: "bool", title: "<b>Enable House HVAC Presence Override</b>", defaultValue: true, submitOnChange: false
            input name: "awayDev2Enabled", type: "bool", title: "<b>Enable Office HVAC Presence Override</b>", defaultValue: true, submitOnChange: false
            
            paragraph "<div style='color:#666; font-size:11px; margin-top:4px;'><i>Configure temperature target boundaries active during presence override:</i></div>"
            renderBoundOverrideTable("presence", [
                [label: "House Unit", heatKey: "awayDev1Heat", coolKey: "awayDev1Cool", defaultHeat: 62, defaultCool: 78],
                [label: "Office Unit", heatKey: "awayDev2Heat", coolKey: "awayDev2Cool", defaultHeat: 60, defaultCool: 80]
            ])
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "awayDev2ForceOff", type: "bool", title: "<b>Force Office HVAC OFF During Presence Override</b>", defaultValue: true, submitOnChange: false
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-6px; margin-bottom:8px;'>" +
                      "<i>Safety Isolation: Forces the secondary Office unit entirely OFF when the house is vacant, overriding standard presence setpoints.</i></div>"

            // --- SLEEPING OVERRIDE GROUP ---
            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #8E44AD; margin-top:12px; margin-bottom:8px;'>Sleeping Override</div>"
            input name: "sleepSwitch", type: "capability.switch", title: "<b>Select Sleeping Switch</b>", required: false, multiple: false, submitOnChange: false
            
            input name: "sleepDev1Enabled", type: "bool", title: "<b>Enable House HVAC Sleeping Override</b>", defaultValue: true, submitOnChange: false
            input name: "sleepDev2Enabled", type: "bool", title: "<b>Enable Office HVAC Sleeping Override</b>", defaultValue: true, submitOnChange: false
            
            paragraph "<div style='color:#666; font-size:11px; margin-top:4px;'><i>Configure temperature target boundaries active during sleeping override:</i></div>"
            renderBoundOverrideTable("sleeping", [
                [label: "House Unit", heatKey: "sleepDev1Heat", coolKey: "sleepDev1Cool", defaultHeat: 65, defaultCool: 74],
                [label: "Office Unit", heatKey: "sleepDev2Heat", coolKey: "sleepDev2Cool", defaultHeat: 62, defaultCool: 76]
            ])
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "sleepDev2ForceOff", type: "bool", title: "<b>Force Office HVAC OFF During Sleeping Override</b>", defaultValue: true, submitOnChange: false
            paragraph "<div style='color:#7F8C8D; font-size:11px; margin-top:-6px; margin-bottom:8px;'>" +
                      "<i>Safety Isolation: Forces the secondary Office unit entirely OFF during sleeping hours to prevent unattended overnight operation.</i></div>"
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY C: ENVIRONMENTAL QUALITY & AIR FILTRATION
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY C: ENVIRONMENTAL QUALITY & AIR FILTRATION</div>") {}

        /* Section 5: Automatic Fan Circulation Control */
        section("<b>SECTION 5: Automatic Fan Circulation Control</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "When enabled, MEM manages fan circulation on the House HVAC Controller. Set the thermostat's Fan Control Source to <b>external</b> so the driver does not compete.</div>"

            input name: "fanCirculateEnabled", type: "bool", title: "<b>Enable Fan Circulation Loop (House HVAC)</b>", defaultValue: false, submitOnChange: false
            
            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #2980B9; margin-top:6px; margin-bottom:6px;'>Standard Circulation Schedule</div>"
            renderBoundDurationInputs("standardFan", [
                [key: "fanOnMinutes", label: "Standard Fan ON Duration (Minutes)", defaultVal: 10],
                [key: "fanOffMinutes", label: "Standard Fan OFF / AUTO Duration (Minutes)", defaultVal: 50]
            ])
            
            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #8E44AD; margin-top:10px; margin-bottom:6px;'>Sleeping Mode Circulation Schedule</div>"
            renderBoundDurationInputs("sleepingFan", [
                [key: "fanSleepOnMinutes", label: "Sleeping Fan ON Duration (Minutes)", defaultVal: 5],
                [key: "fanSleepOffMinutes", label: "Sleeping Fan OFF / AUTO Duration (Minutes)", defaultVal: 115]
            ])
        }

        /* Section 6: Air Quality Monitoring */
        section("<b>SECTION 6: Air Quality Monitoring & RGB Indicator Light</b>", hideable: true, hidden: true) {
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
            colorTableSb.append("<td style='padding:4px;'>").append((tUnhealthMax ?: 200) + 1).append(" – ").append(tVeryUnhealthyMax).append("</td><td style='padding:4px; color:purple;'><b>Very Unhealthy</b></td><td style='padding:4px;'>🟣 Purple</td><td style='padding:4px;'>Health alert. High risk of respiratory irritation.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr>")
            colorTableSb.append("<td style='padding:4px;'>&ge; ").append((tVeryUnhealthyMax ?: 300) + 1).append("</td><td style='padding:4px; color:maroon;'><b>Hazardous</b></td><td style='padding:4px;'>🟤 Maroon</td><td style='padding:4px;'>Emergency conditions. Stay entirely indoors.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("</table>")
            paragraph colorTableSb.toString()

            input name: "aqiEnabled", type: "bool", title: "<b>Enable Air Quality Monitoring</b>", defaultValue: false, submitOnChange: false
            input name: "aqiSensor1", type: "capability.airQuality", title: "Select Primary AQI Sensor", required: false, multiple: false, submitOnChange: false
            input name: "aqiSensor2", type: "capability.airQuality", title: "Select Secondary AQI Sensor (Optional)", required: false, multiple: false, submitOnChange: false
            input name: "rgbLight", type: "capability.colorControl", title: "Select Indicator RGB Light Device", required: false, multiple: false
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>" +
                      "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>AQI Tier Upper Boundary Overrides</span>"
            
            input name: "aqiGoodMax", type: "number", title: "Good AQI Max (Default: 50)", required: true, defaultValue: 50, submitOnChange: false
            input name: "aqiModMax", type: "number", title: "Moderate AQI Max (Default: 100)", required: true, defaultValue: 100, submitOnChange: false
            input name: "aqiSensMax", type: "number", title: "Sensitive Groups Max (Default: 150)", required: true, defaultValue: 150, submitOnChange: false
            input name: "aqiUnhealthyMax", type: "number", title: "Unhealthy Max (Default: 200)", required: true, defaultValue: 200, submitOnChange: false
            input name: "aqiVeryUnhealthyMax", type: "number", title: "Very Unhealthy Max (Default: 300)", required: true, defaultValue: 300, submitOnChange: false
        }

        /* Section 7: Room Air Filter Control */
        section("<b>SECTION 7: Room Air Filter Automation</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Automatically toggles the room air filter switch on/off based on real-time AQI tier threshold calculations.</div>"

            input name: "airFilterEnabled", type: "bool", title: "<b>Enable Automated Room Air Filter Control</b>", defaultValue: true, submitOnChange: false
            input name: "airFilterSwitch", type: "capability.switch", title: "Select Room Air Filter Switch/Device", required: false, multiple: false, submitOnChange: false
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY D: SYSTEM STATUS & DIAGNOSTICS
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY D: SYSTEM STATUS & DIAGNOSTICS</div>") {}

        /* Section 8: Live Status Summary */
        section("<b>SECTION 8: System Operational Status Summary</b>", hideable: true, hidden: true) {
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

        /* Section 9: Logging Options */
        section("<b>SECTION 9: App Preferences & Logging Options</b>", hideable: true, hidden: true) {
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

// =================================================================================================
// GUI SPINNER & BINDING HELPERS (Solving Issue A)
// =================================================================================================

private void renderBoundMatrixTable(String title, String prefix, List<Map> rows) {
    String inputStyle = "width:65px; padding:3px; font-size:13px; text-align:center; border:1px solid #ccc; border-radius:4px;"
    
    // Register native hidden inputs so Hubitat persists target setting keys on Done
    rows.each { r ->
        def curHeat = settings[r.heatKey] != null ? settings[r.heatKey] : r.defaultHeat
        def curCool = settings[r.coolKey] != null ? settings[r.coolKey] : r.defaultCool
        input name: r.heatKey, type: "hidden", defaultValue: curHeat
        input name: r.coolKey, type: "hidden", defaultValue: curCool
    }

    StringBuilder html = new StringBuilder()
    html.append("<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-bottom:2px solid #2C3E50;'>${title}</div>")
    html.append("<table style='width:100%; border-collapse:collapse; font-size:13px; margin:8px 0; text-align:left;'>")
    html.append("<tr style='border-bottom:2px solid #2C3E50; background-color:#F2F4F4;'>")
    html.append("<th style='padding:6px; width:34%;'>Period</th>")
    html.append("<th style='padding:6px; width:33%;'>Heating (°F) <i>(below)</i></th>")
    html.append("<th style='padding:6px; width:33%;'>Cooling (°F) <i>(exceeds)</i></th>")
    html.append("</tr>")

    rows.each { r ->
        def valHeat = settings[r.heatKey] != null ? settings[r.heatKey] : r.defaultHeat
        def valCool = settings[r.coolKey] != null ? settings[r.coolKey] : r.defaultCool
        
        html.append("<tr style='border-bottom:1px solid #E0E0E0;'>")
        html.append("<td style='padding:6px;'><b>${r.name}</b></td>")
        html.append("<td style='padding:6px;'>")
        html.append("<input type='number' value='${valHeat}' min='45' max='90' style='${inputStyle}' onchange=\"document.getElementsByName('${r.heatKey}')[0].value=this.value;\" />")
        html.append("</td>")
        html.append("<td style='padding:6px;'>")
        html.append("<input type='number' value='${valCool}' min='50' max='95' style='${inputStyle}' onchange=\"document.getElementsByName('${r.coolKey}')[0].value=this.value;\" />")
        html.append("</td>")
        html.append("</tr>")
    }
    html.append("</table>")
    paragraph html.toString()
}

private void renderBoundOverrideTable(String groupName, List<Map> rows) {
    String inputStyle = "width:65px; padding:3px; font-size:13px; text-align:center; border:1px solid #ccc; border-radius:4px;"

    rows.each { r ->
        def curHeat = settings[r.heatKey] != null ? settings[r.heatKey] : r.defaultHeat
        def curCool = settings[r.coolKey] != null ? settings[r.coolKey] : r.defaultCool
        input name: r.heatKey, type: "hidden", defaultValue: curHeat
        input name: r.coolKey, type: "hidden", defaultValue: curCool
    }

    StringBuilder html = new StringBuilder()
    html.append("<table style='width:100%; border-collapse:collapse; font-size:13px; margin:6px 0; text-align:left;'>")
    html.append("<tr style='border-bottom:1px solid #2C3E50; background-color:#F8F9FA;'>")
    html.append("<th style='padding:4px; width:34%;'>Unit</th>")
    html.append("<th style='padding:4px; width:33%;'>Heating Target (°F)</th>")
    html.append("<th style='padding:4px; width:33%;'>Cooling Target (°F)</th>")
    html.append("</tr>")

    rows.each { r ->
        def valHeat = settings[r.heatKey] != null ? settings[r.heatKey] : r.defaultHeat
        def valCool = settings[r.coolKey] != null ? settings[r.coolKey] : r.defaultCool
        
        html.append("<tr style='border-bottom:1px solid #E0E0E0;'>")
        html.append("<td style='padding:4px;'><b>${r.label}</b></td>")
        html.append("<td style='padding:4px;'>")
        html.append("<input type='number' value='${valHeat}' min='45' max='90' style='${inputStyle}' onchange=\"document.getElementsByName('${r.heatKey}')[0].value=this.value;\" />")
        html.append("</td>")
        html.append("<td style='padding:4px;'>")
        html.append("<input type='number' value='${valCool}' min='50' max='95' style='${inputStyle}' onchange=\"document.getElementsByName('${r.coolKey}')[0].value=this.value;\" />")
        html.append("</td>")
        html.append("</tr>")
    }
    html.append("</table>")
    paragraph html.toString()
}

private void renderBoundDurationInputs(String groupName, List<Map> items) {
    String inputStyle = "width:65px; padding:3px; font-size:13px; text-align:center; border:1px solid #ccc; border-radius:4px;"

    items.each { item ->
        def curVal = settings[item.key] != null ? settings[item.key] : item.defaultVal
        input name: item.key, type: "hidden", defaultValue: curVal
    }

    StringBuilder html = new StringBuilder()
    html.append("<table style='width:100%; border-collapse:collapse; font-size:13px; margin:4px 0; text-align:left;'>")
    items.each { item ->
        def val = settings[item.key] != null ? settings[item.key] : item.defaultVal
        html.append("<tr style='border-bottom:1px solid #E0E0E0;'>")
        html.append("<td style='padding:4px; width:70%;'>${item.label}</td>")
        html.append("<td style='padding:4px; width:30%;'>")
        html.append("<input type='number' value='${val}' min='1' max='240' style='${inputStyle}' onchange=\"document.getElementsByName('${item.key}')[0].value=this.value;\" />")
        html.append("</td></tr>")
    }
    html.append("</table>")
    paragraph html.toString()
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

    if (awaySwitch) subscribe(awaySwitch, "switch", "overrideSwitchHandler")
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
    Map aqiStatus = [status: "Disabled", colorMap: null, actionText: "N/A", filterOn: false]
    
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
            logInfo "Office HVAC Safety Trigger Active (${sched.activePeriod}). Forcing Office HVAC OFF."
            try { targetThermostat2.off() } catch (Exception e) { logError "Failed to execute OFF on Office HVAC: ${e.message}" }
        } else if (getSettingBool("dev2ScheduleEnabled", true) && sched.d2TargetHeat != null && sched.d2TargetCool != null) {
            applySetpointsAndEvaluateMode(targetThermostat2, sched.d2TargetHeat as BigDecimal, sched.d2TargetCool as BigDecimal, thermostat2, dev2AutoChangeoverEnabled, dev2TempDeadband, true, forceSetpointApply)
        }
    }

    if (aqiEnabled && rgbLight) {
        String currentMode = location.mode?.toLowerCase() ?: ""
        Boolean isAllowedMode = currentMode.contains("home") || currentMode.contains("awake")
        Boolean isAwayOverride = (awaySwitch && safeGetDeviceAttribute(awaySwitch, "switch") == "on")
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

private int getBoundaryMinutes(String prefix, String defaultHour, String defaultAmPm) {
    String h = settings["${prefix}Hour"] ?: defaultHour
    String m = settings["${prefix}Min"] ?: "00"
    String ap = settings["${prefix}AmPm"] ?: defaultAmPm
    
    int hrs = h.toInteger()
    int mins = m.toInteger()
    
    if (ap == "PM" && hrs < 12) hrs += 12
    if (ap == "AM" && hrs == 12) hrs = 0
    
    return (hrs * 60) + mins
}

private void scheduleTimeBoundaries() {
    unschedule("evaluateSchedule")
    
    List<Integer> mins = [
        getBoundaryMinutes("mStart", "06", "AM"),
        getBoundaryMinutes("dStart", "09", "AM"),
        getBoundaryMinutes("eStart", "05", "PM"),
        getBoundaryMinutes("nStart", "10", "PM")
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

    Boolean isAway = awaySwitch ? (safeGetDeviceAttribute(awaySwitch, "switch") == "on") : false
    Boolean isSleeping = sleepSwitch ? (safeGetDeviceAttribute(sleepSwitch, "switch") == "on") : false

    if (isAway) {
        activePeriod = "Presence Override"
        if (getSettingBool("awayDev1Enabled", true)) {
            d1TargetHeat = safeToDecimal(settings.awayDev1Heat, 62.0)
            d1TargetCool = safeToDecimal(settings.awayDev1Cool, 78.0)
        }
        
        if (getSettingBool("awayDev2Enabled", true)) {
            if (getSettingBool("awayDev2ForceOff", true)) {
                d2ForceOff = true
            } else {
                d2TargetHeat = safeToDecimal(settings.awayDev2Heat, 60.0)
                d2TargetCool = safeToDecimal(settings.awayDev2Cool, 80.0)
            }
        }
    } else if (isSleeping) {
        activePeriod = "Sleeping Override"
        if (getSettingBool("sleepDev1Enabled", true)) {
            d1TargetHeat = safeToDecimal(settings.sleepDev1Heat, 65.0)
            d1TargetCool = safeToDecimal(settings.sleepDev1Cool, 74.0)
        }
        
        if (getSettingBool("sleepDev2Enabled", true)) {
            if (getSettingBool("sleepDev2ForceOff", true)) {
                d2ForceOff = true
            } else {
                d2TargetHeat = safeToDecimal(settings.sleepDev2Heat, 62.0)
                d2TargetCool = safeToDecimal(settings.sleepDev2Cool, 76.0)
            }
        }
    } else {
        List<Map> periods = [
            [name: "Morning", mins: getBoundaryMinutes("mStart", "06", "AM")],
            [name: "Day",     mins: getBoundaryMinutes("dStart", "09", "AM")],
            [name: "Evening", mins: getBoundaryMinutes("eStart", "05", "PM")],
            [name: "Night",   mins: getBoundaryMinutes("nStart", "10", "PM")]
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
def overrideSwitchHandler(evt) { executeEvaluationPass("Override Switch Changed (${evt?.device?.displayName} = ${evt?.value})", true) }
def aqiHandler(evt) { executeEvaluationPass("AQI Sensor Updated (${evt?.device?.displayName} = ${evt?.value})", false) }
def airFilterSwitchHandler(evt) { executeEvaluationPass("Air Filter Switch State Changed (${evt?.value})", false) }
def temperatureHandler(evt) { executeEvaluationPass("Thermostat Ambient Temperature Updated (${evt?.device?.displayName})", false) }
def evaluateSchedule() { executeEvaluationPass("Scheduled Window Boundary Trigger", true) }

// =================================================================================================
// 4. THERMOSTAT CONTROL & AUTO-CHANGEOVER ENGINE
// =================================================================================================

private Map getThermostatStateSnapshot(Object dev) {
    if (!dev) return [:]
    return [
        temp: safeGetDeviceAttribute(dev, "temperature"),
        heat: safeGetDeviceAttribute(dev, "heatingSetpoint"),
        cool: safeGetDeviceAttribute(dev, "coolingSetpoint"),
        mode: safeGetDeviceAttribute(dev, "thermostatMode"),
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
                logInfo "Fan Circulation Loop (${isSleeping ? 'Sleeping Schedule' : 'Standard Schedule'}): Turning fan ON for ${safeOn} minutes."
                targetThermostat.fanOn()
                sendIfChangedStateValue("fanOwned", true)
            } else {
                sendIfChangedStateValue("fanOwned", false)
            }
            runIn(onTime, "toggleFanCirculation", [data: [nextPhase: "OFF"]])
        } else {
            sendIfChangedStateValue("fanPhase", "OFF Phase")
            logInfo "Fan Circulation Loop (${isSleeping ? 'Sleeping Schedule' : 'Standard Schedule'}): Setting fan to AUTO for ${safeOff} minutes."
            targetThermostat.fanAuto()
            sendIfChangedStateValue("fanOwned", false)
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
        return [status: "Not Evaluated", colorMap: null, actionText: "N/A", filterOn: false]
    }
    
    Integer avgAqi = avgAqiVal as Integer
    Map limits = getAqiTierLimits()
    Integer tGoodMax = safeToInt(limits?.good, 50)
    Integer tModMax = safeToInt(limits?.mod, 100)
    Integer tSensMax = safeToInt(limits?.sens, 150)
    Integer tUnhealthMax = safeToInt(limits?.unhealthy, 200)
    Integer tVeryUnhealthMax = safeToInt(limits?.veryUnhealthy, 300)

    if (avgAqi <= tGoodMax) {
        return [status: "Green (Good)", colorMap: null, actionText: "Satisfactory air quality. Little to no risk.", filterOn: false]
    } else if (avgAqi <= tModMax) {
        return [status: "Yellow (Moderate)", colorMap: [hue: 16, saturation: 100, level: 100], actionText: "Acceptable. Sensitive groups may feel minor symptoms.", filterOn: true]
    } else if (avgAqi <= tSensMax) {
        return [status: "Orange (Unhealthy for Sensitive Groups)", colorMap: [hue: 8, saturation: 100, level: 100], actionText: "Potentially harmful for kids, elderly, and asthmatics.", filterOn: true]
    } else if (avgAqi <= tUnhealthMax) {
        return [status: "Red (Unhealthy)", colorMap: [hue: 0, saturation: 100, level: 100], actionText: "Active danger. Everyone may experience adverse effects.", filterOn: true]
    } else if (avgAqi <= tVeryUnhealthMax) {
        return [status: "Purple (Very Unhealthy)", colorMap: [hue: 75, saturation: 100, level: 100], actionText: "Health alert. High risk of respiratory irritation.", filterOn: true]
    } else {
        return [status: "Maroon (Hazardous)", colorMap: [hue: 95, saturation: 100, level: 50], actionText: "Emergency conditions. Stay entirely indoors.", filterOn: true]
    }
}

// =================================================================================================
// 7. DASHBOARD TILE ENGINE (Solving Issue B)
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

    // Retrieve active schedule targets rendered on Tile
    String hTargetStr = (sched.d1TargetHeat != null && sched.d1TargetCool != null) ? "H:${sched.d1TargetHeat?.setScale(0, BigDecimal.ROUND_HALF_UP)}/C:${sched.d1TargetCool?.setScale(0, BigDecimal.ROUND_HALF_UP)}" : "N/A"
    String oTargetStr = sched.d2ForceOff ? "OFF" : ((sched.d2TargetHeat != null && sched.d2TargetCool != null) ? "H:${sched.d2TargetHeat?.setScale(0, BigDecimal.ROUND_HALF_UP)}/C:${sched.d2TargetCool?.setScale(0, BigDecimal.ROUND_HALF_UP)}" : "N/A")
    
    String avgAqiStr = "N/A"
    String aqiStatusStr = "Disabled"
    String aqiColorCss = "#888888"
    
    Integer avgAqi = ctx.aqi as Integer
    if (avgAqi != null) {
        avgAqiStr = "${avgAqi}"
        Map statusInfo = determineStatus(avgAqi)
        aqiStatusStr = statusInfo.status ?: "N/A"
        
        if (aqiStatusStr.contains("Good")) aqiColorCss = "#008000"
        else if (aqiStatusStr.contains("Moderate")) aqiColorCss = "#b8860b"
        else if (aqiStatusStr.contains("Sensitive")) aqiColorCss = "#ff8c00"
        else if (aqiStatusStr.contains("Unhealthy for")) aqiColorCss = "#ff0000"
        else if (aqiStatusStr.contains("Very Unhealthy")) aqiColorCss = "#800080"
        else if (aqiStatusStr.contains("Hazardous")) aqiColorCss = "#800000"
    }

    StringBuilder tileSb = new StringBuilder()
    tileSb.append("<div style='font-size:.42em; line-height:1.2; text-align:center; padding:2px;'>")
    tileSb.append("<b>MEM Period:</b> ").append(activePeriod).append("<br/>")
    tileSb.append("<b>House (${t1Temp}):</b> ").append(hTargetStr).append("<br/>")
    tileSb.append("<b>Office (${t2Mode}):</b> ").append(oTargetStr).append("<br/>")
    tileSb.append("<div style='font-size:.3em;'><b>AQI:</b> <span style='color:").append(aqiColorCss)
          .append("; font-weight:bold;'>").append(avgAqiStr).append(" (").append(aqiStatusStr).append(")</span></div>")
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
        errors.add("House HVAC Controller device selection is required before enabling House Thermostat automations.")
    }

    if (getSettingBool("dev2AppEnabled", false) && !targetThermostat2) {
        errors.add("Office HVAC Controller device selection is required before enabling Office Thermostat automations.")
    }

    List<Map> periodCheck = [
        [name: "Morning", mins: getBoundaryMinutes("mStart", "06", "AM")],
        [name: "Day",     mins: getBoundaryMinutes("dStart", "09", "AM")],
        [name: "Evening", mins: getBoundaryMinutes("eStart", "05", "PM")],
        [name: "Night",   mins: getBoundaryMinutes("nStart", "10", "PM")]
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