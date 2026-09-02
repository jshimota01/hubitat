/**
 * Application Name: Multiperiod Environment Manager
 * Platform: Hubitat Elevation
 * Notes: Schedules heating/cooling setpoint windows with switch overrides, dual House & Office HVAC 
 *        controller management, fire safety forced-off modes, automatic fan circulation, 6-tier EPA AQI 
 *        Air Quality monitoring with health action strings, independent Air Filter control, native automatic 
 *        heat/cool mode changeover, static form layout (saves on Done), and dashboard tile output.
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
 *	v1.4.4	  09/01/26	  jshimota	  Removed all remaining submitOnChange triggers to eliminate page refreshes and unwanted section collapsing. All configuration changes persist exclusively when Done is clicked.
 *	v1.4.3	  09/01/26	  jshimota	  Fixed configuration validation guard when no device is selected.
 *	v1.4.2	  09/01/26	  jshimota	  Updated schedule table column headers to 'Cooling (if temp exceeds -)' and 'Heating (if temp falls below -)'. Enforced standard page submit model (save on Done).
 *	v1.4.1	  09/01/26	  jshimota	  Decoupled enable switches from section collapse routines. Clarified Mode Changeover guidance. Renamed schedule section headers to 'Setpoints for Periods'.
 *	v1.4.0	  09/01/26	  jshimota	  Converted schedule summary tables to active interactive spinner tables. Removed redundant manual input fields. Decoupled section toggling from device enable switches. Relabeled device switches to 'Enable Home Thermostat' and 'Enable Office Thermostat'. Relocated Enable Scheduling toggles above schedule tables.
 **/

static String version() { return '1.4.4' }
def timeStamp() { return "2026/09/01 12:15 PM" }

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
                      "<b>Automations:</b> ${houseBadge} | ${officeBadge} &nbsp;|&nbsp; " +
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
            
            paragraph "<b>House Temperature Hysteresis Buffer (°F):</b><br/><input type='number' name='dev1TempDeadband' min='1' max='10' step='1' value='${settings.dev1TempDeadband ?: 2}' style='width:70px; padding:4px; font-size:14px; border:1px solid #ccc; border-radius:4px;'/> <i>(Degree offset required beyond opposing setpoint before triggering mode change)</i>"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev1ScheduleEnabled", type: "bool", title: "<b>Enable Setpoint Scheduling</b>", defaultValue: true, submitOnChange: false
            
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Setpoints for Periods</span>"
            
            String spinnerStyle = "width:65px; padding:3px; font-size:13px; border:1px solid #ccc; border-radius:4px; text-align:center;"
            
            // Interactive Schedule Table
            StringBuilder houseTable = new StringBuilder()
            houseTable.append("<table style='width:100%; border-collapse:collapse; font-size:13px; margin:8px 0; text-align:left;'>")
            houseTable.append("<tr style='border-bottom:2px solid #2C3E50;'>")
            houseTable.append("<th style='padding:6px;'>Time Period</th><th style='padding:6px; color:#2980B9;'>Cooling (if temp exceeds -)</th><th style='padding:6px; color:#C0392B;'>Heating (if temp falls below -)</th>")
            houseTable.append("</tr>")
            
            // Morning
            houseTable.append("<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Morning</b></td>")
            houseTable.append("<td style='padding:6px;'><input type='number' name='d1MorningCool' min='50' max='90' step='1' value='${settings.d1MorningCool ?: 72}' style='${spinnerStyle}'/></td>")
            houseTable.append("<td style='padding:6px;'><input type='number' name='d1MorningHeat' min='50' max='90' step='1' value='${settings.d1MorningHeat ?: 68}' style='${spinnerStyle}'/></td></tr>")
            
            // Day
            houseTable.append("<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Day</b></td>")
            houseTable.append("<td style='padding:6px;'><input type='number' name='d1DayCool' min='50' max='90' step='1' value='${settings.d1DayCool ?: 75}' style='${spinnerStyle}'/></td>")
            houseTable.append("<td style='padding:6px;'><input type='number' name='d1DayHeat' min='50' max='90' step='1' value='${settings.d1DayHeat ?: 66}' style='${spinnerStyle}'/></td></tr>")
            
            // Evening
            houseTable.append("<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Evening</b></td>")
            houseTable.append("<td style='padding:6px;'><input type='number' name='d1EveningCool' min='50' max='90' step='1' value='${settings.d1EveningCool ?: 73}' style='${spinnerStyle}'/></td>")
            houseTable.append("<td style='padding:6px;'><input type='number' name='d1EveningHeat' min='50' max='90' step='1' value='${settings.d1EveningHeat ?: 69}' style='${spinnerStyle}'/></td></tr>")
            
            // Night
            houseTable.append("<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Night</b></td>")
            houseTable.append("<td style='padding:6px;'><input type='number' name='d1NightCool' min='50' max='90' step='1' value='${settings.d1NightCool ?: 75}' style='${spinnerStyle}'/></td>")
            houseTable.append("<td style='padding:6px;'><input type='number' name='d1NightHeat' min='50' max='90' step='1' value='${settings.d1NightHeat ?: 64}' style='${spinnerStyle}'/></td></tr>")
            
            houseTable.append("</table>")
            paragraph houseTable.toString()
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

            paragraph "<b>Office Temperature Hysteresis Buffer (°F):</b><br/><input type='number' name='dev2TempDeadband' min='1' max='10' step='1' value='${settings.dev2TempDeadband ?: 2}' style='width:70px; padding:4px; font-size:14px; border:1px solid #ccc; border-radius:4px;'/> <i>(Degree offset required beyond opposing setpoint before triggering mode change)</i>"

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "dev2ScheduleEnabled", type: "bool", title: "<b>Enable Setpoint Scheduling</b>", defaultValue: true, submitOnChange: false

            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Setpoints for Periods</span>"

            String spinnerStyle = "width:65px; padding:3px; font-size:13px; border:1px solid #ccc; border-radius:4px; text-align:center;"

            // Interactive Schedule Table
            StringBuilder officeTable = new StringBuilder()
            officeTable.append("<table style='width:100%; border-collapse:collapse; font-size:13px; margin:8px 0; text-align:left;'>")
            officeTable.append("<tr style='border-bottom:2px solid #2C3E50;'>")
            officeTable.append("<th style='padding:6px;'>Time Period</th><th style='padding:6px; color:#2980B9;'>Cooling (if temp exceeds -)</th><th style='padding:6px; color:#C0392B;'>Heating (if temp falls below -)</th>")
            officeTable.append("</tr>")
            
            // Morning
            officeTable.append("<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Morning</b></td>")
            officeTable.append("<td style='padding:6px;'><input type='number' name='d2MorningCool' min='50' max='90' step='1' value='${settings.d2MorningCool ?: 72}' style='${spinnerStyle}'/></td>")
            officeTable.append("<td style='padding:6px;'><input type='number' name='d2MorningHeat' min='50' max='90' step='1' value='${settings.d2MorningHeat ?: 68}' style='${spinnerStyle}'/></td></tr>")
            
            // Day
            officeTable.append("<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Day</b></td>")
            officeTable.append("<td style='padding:6px;'><input type='number' name='d2DayCool' min='50' max='90' step='1' value='${settings.d2DayCool ?: 75}' style='${spinnerStyle}'/></td>")
            officeTable.append("<td style='padding:6px;'><input type='number' name='d2DayHeat' min='50' max='90' step='1' value='${settings.d2DayHeat ?: 66}' style='${spinnerStyle}'/></td></tr>")
            
            // Evening
            officeTable.append("<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Evening</b></td>")
            officeTable.append("<td style='padding:6px;'><input type='number' name='d2EveningCool' min='50' max='90' step='1' value='${settings.d2EveningCool ?: 73}' style='${spinnerStyle}'/></td>")
            officeTable.append("<td style='padding:6px;'><input type='number' name='d2EveningHeat' min='50' max='90' step='1' value='${settings.d2EveningHeat ?: 69}' style='${spinnerStyle}'/></td></tr>")
            
            // Night
            officeTable.append("<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Night</b></td>")
            officeTable.append("<td style='padding:6px;'><input type='number' name='d2NightCool' min='50' max='90' step='1' value='${settings.d2NightCool ?: 78}' style='${spinnerStyle}'/></td>")
            officeTable.append("<td style='padding:6px;'><input type='number' name='d2NightHeat' min='50' max='90' step='1' value='${settings.d2NightHeat ?: 62}' style='${spinnerStyle}'/></td></tr>")
            
            officeTable.append("</table>")
            paragraph officeTable.toString()
        }

        /* Section 3: Time Window Boundary Settings */
        section("<b>SECTION 3: Time Schedule Window Boundaries</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Set global start and end times for daily schedule periods. <i>(Applies to both House and Office HVAC controllers).</i></div>"

            Map val = validateConfiguration()
            if (val.warnings && !val.warnings.isEmpty()) {
                StringBuilder sb = new StringBuilder()
                sb.append("<div style='color:#8a6d3b; background-color:#fcf8e3; padding:8px; border:1px solid #faebcc; border-radius:4px; margin-bottom:10px;'><b>SCHEDULE VALIDATION NOTICE:</b><ul>")
                val.warnings.each { w -> sb.append("<li>").append(w).append("</li>") }
                sb.append("</ul></div>")
                paragraph sb.toString()
            }

            input name: "morningStart", type: "time", title: "Morning Start Time", required: true, defaultValue: "06:00", width: 3
            input name: "morningEnd", type: "time", title: "Morning End Time", required: true, defaultValue: "09:00", width: 3
            input name: "dayStart", type: "time", title: "Day Start Time", required: true, defaultValue: "09:00", width: 3
            input name: "dayEnd", type: "time", title: "Day End Time", required: true, defaultValue: "17:00", width: 3
            input name: "eveningStart", type: "time", title: "Evening Start Time", required: true, defaultValue: "17:00", width: 3
            input name: "eveningEnd", type: "time", title: "Evening End Time", required: true, defaultValue: "22:00", width: 3
            input name: "nightStart", type: "time", title: "Night Start Time", required: true, defaultValue: "22:00", width: 3
            input name: "nightEnd", type: "time", title: "Night End Time", required: true, defaultValue: "06:00", width: 3
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY B: SYSTEM OVERRIDES & SAFETY MODES
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY B: SYSTEM OVERRIDES & SAFETY MODES</div>") {}

        /* Section 4: System Overrides */
        section("<b>SECTION 4: System Overrides & Fire Hazard Safety Controls</b>", hideable: true, hidden: true) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure behavioral setpoints or force devices <b>OFF</b> when Away or Sleeping switches are active.</div>"

            input name: "awaySwitch", type: "capability.switch", title: "<b>Select 'Away' Switch</b>", required: false, multiple: false, submitOnChange: false
            paragraph "<span style='color:#2C3E50; font-weight:bold;'>House HVAC Away Configuration</span>"
            input name: "awayDev1Heat", type: "number", title: "House Away Heat Setpoint", required: true, defaultValue: 62, width: 6
            input name: "awayDev1Cool", type: "number", title: "House Away Cool Setpoint", required: true, defaultValue: 78, width: 6
            
            paragraph "<span style='color:#C0392B; font-weight:bold;'>Office HVAC Away Fire Safety Configuration</span>"
            input name: "awayDev2Action", type: "enum", title: "Office HVAC Away Mode Action", options: ["setpoint": "Use Away Setpoints", "off": "FORCE OFF (Fire Hazard Safety)"], defaultValue: "off", submitOnChange: false
            input name: "awayDev2Heat", type: "number", title: "Office Away Heat Setpoint", required: true, defaultValue: 60, width: 6
            input name: "awayDev2Cool", type: "number", title: "Office Away Cool Setpoint", required: true, defaultValue: 80, width: 6
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "sleepSwitch", type: "capability.switch", title: "<b>Select 'Sleeping' Switch</b>", required: false, multiple: false, submitOnChange: false
            paragraph "<span style='color:#2C3E50; font-weight:bold;'>House HVAC Sleeping Configuration</span>"
            input name: "sleepDev1Heat", type: "number", title: "House Sleeping Heat Setpoint", required: true, defaultValue: 65, width: 6
            input name: "sleepDev1Cool", type: "number", title: "House Sleeping Cool Setpoint", required: true, defaultValue: 74, width: 6
            
            paragraph "<span style='color:#C0392B; font-weight:bold;'>Office HVAC Sleeping Fire Safety Configuration</span>"
            input name: "sleepDev2Action", type: "enum", title: "Office HVAC Sleeping Mode Action", options: ["setpoint": "Use Sleeping Setpoints", "off": "FORCE OFF (Fire Hazard Safety)"], defaultValue: "off", submitOnChange: false
            input name: "sleepDev2Heat", type: "number", title: "Office Sleeping Heat Setpoint", required: true, defaultValue: 62, width: 6
            input name: "sleepDev2Cool", type: "number", title: "Office Sleeping Cool Setpoint", required: true, defaultValue: 76, width: 6
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
            input name: "fanOnMinutes", type: "number", title: "Fan ON Duration (1-240 Minutes)", required: true, defaultValue: 30, width: 6
            input name: "fanOffMinutes", type: "number", title: "Fan OFF / AUTO Duration (1-240 Minutes)", required: true, defaultValue: 30, width: 6
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
        logDebug "App closed without setting or version changes. Skipping re-initialization."
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
// 3. SCHEDULING & EVENT HANDLER ENGINE
// =================================================================================================

private void scheduleTimeBoundaries() {
    unschedule("evaluateSchedule")
    
    List timeInputs = [morningStart, morningEnd, dayStart, dayEnd, eveningStart, eveningEnd, nightStart, nightEnd]
    timeInputs.unique().each { timeStr ->
        if (timeStr) {
            try {
                Date timeVal = toDateTime(timeStr)
                schedule(timeVal, "evaluateSchedule", [overwrite: false])
                logDebug "Scheduled exact boundary evaluation for ${timeStr}"
            } catch (Exception e) {
                logError "Failed to parse time string '${timeStr}' for boundary scheduling: ${e.message}"
            }
        }
    }
}

private Integer timeToMinutes(Object timeInput) {
    if (!timeInput) return null
    try {
        String str = timeInput.toString()
        if (str.contains("T")) {
            Date dateVal = toDateTime(str)
            Calendar cal = Calendar.getInstance(location.timeZone)
            cal.setTime(dateVal)
            return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
        } else if (str.contains(":")) {
            List parts = str.split(":")
            return (parts[0].trim().toInteger() * 60) + parts[1].trim().toInteger()
        }
    } catch (Exception e) {
        logError "Failed to convert time input '${timeInput}' to minutes: ${e.message}"
    }
    return null
}

private Boolean isTimeInWindow(Integer startMin, Integer endMin, Integer currentMin) {
    if (startMin == null || endMin == null || currentMin == null) return false
    if (startMin <= endMin) return (currentMin >= startMin && currentMin < endMin)
    else return (currentMin >= startMin || currentMin < endMin)
}

private Map getCalculatedScheduleData() {
    Calendar nowCal = Calendar.getInstance(location.timeZone)
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
        activePeriod = "Away"
        d1TargetHeat = safeToDecimal(awayDev1Heat, 62.0)
        d1TargetCool = safeToDecimal(awayDev1Cool, 78.0)
        
        if (awayDev2Action == "off") {
            d2ForceOff = true
        } else {
            d2TargetHeat = safeToDecimal(awayDev2Heat, 60.0)
            d2TargetCool = safeToDecimal(awayDev2Cool, 80.0)
        }
    } else if (isSleeping) {
        activePeriod = "Sleeping"
        d1TargetHeat = safeToDecimal(sleepDev1Heat, 65.0)
        d1TargetCool = safeToDecimal(sleepDev1Cool, 74.0)
        
        if (sleepDev2Action == "off") {
            d2ForceOff = true
        } else {
            d2TargetHeat = safeToDecimal(sleepDev2Heat, 62.0)
            d2TargetCool = safeToDecimal(sleepDev2Cool, 76.0)
        }
    } else {
        Integer mStart = timeToMinutes(morningStart)
        Integer mEnd   = timeToMinutes(morningEnd)
        Integer dStart = timeToMinutes(dayStart)
        Integer dEnd   = timeToMinutes(dayEnd)
        Integer eStart = timeToMinutes(eveningStart)
        Integer eEnd   = timeToMinutes(eveningEnd)
        Integer nStart = timeToMinutes(nightStart)
        Integer nEnd   = timeToMinutes(nightEnd)

        if (isTimeInWindow(mStart, mEnd, currentMin)) {
            activePeriod = "Morning"
            d1TargetHeat = safeToDecimal(settings.d1MorningHeat, 68.0)
            d1TargetCool = safeToDecimal(settings.d1MorningCool, 72.0)
            d2TargetHeat = safeToDecimal(settings.d2MorningHeat, 68.0)
            d2TargetCool = safeToDecimal(settings.d2MorningCool, 72.0)
        } else if (isTimeInWindow(dStart, dEnd, currentMin)) {
            activePeriod = "Day"
            d1TargetHeat = safeToDecimal(settings.d1DayHeat, 66.0)
            d1TargetCool = safeToDecimal(settings.d1DayCool, 75.0)
            d2TargetHeat = safeToDecimal(settings.d2DayHeat, 66.0)
            d2TargetCool = safeToDecimal(settings.d2DayCool, 75.0)
        } else if (isTimeInWindow(eStart, eEnd, currentMin)) {
            activePeriod = "Evening"
            d1TargetHeat = safeToDecimal(settings.d1EveningHeat, 69.0)
            d1TargetCool = safeToDecimal(settings.d1EveningCool, 73.0)
            d2TargetHeat = safeToDecimal(settings.d2EveningHeat, 69.0)
            d2TargetCool = safeToDecimal(settings.d2EveningCool, 73.0)
        } else if (isTimeInWindow(nStart, nEnd, currentMin)) {
            activePeriod = "Night"
            d1TargetHeat = safeToDecimal(settings.d1NightHeat, 64.0)
            d1TargetCool = safeToDecimal(settings.d1NightCool, 75.0)
            d2TargetHeat = safeToDecimal(settings.d2NightHeat, 62.0)
            d2TargetCool = safeToDecimal(settings.d2NightCool, 78.0)
        } else {
            activePeriod = "Default Window"
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

    Integer safeOn = safeFanDuration(fanOnMinutes, 30)
    Integer safeOff = safeFanDuration(fanOffMinutes, 30)

    Integer onTime = safeOn * 60
    Integer offTime = safeOff * 60

    Map thermostat = getThermostatStateSnapshot(targetThermostat)
    String opState = thermostat.operatingState?.toLowerCase() ?: "idle"

    Boolean executeOnPhase = (targetPhase != null) ? (targetPhase == "ON") : (isInitial || state.fanPhase == "OFF Phase" || !state.fanPhase)

    try {
        if (executeOnPhase) {
            sendIfChangedStateValue("fanPhase", "ON Phase")
            if (opState != "heating" && opState != "cooling") {
                logInfo "Fan Circulation Loop: Turning fan ON for ${safeOn} minutes."
                targetThermostat.fanOn()
                sendIfChangedStateValue("fanOwned", true)
            } else {
                sendIfChangedStateValue("fanOwned", false)
            }
            runIn(onTime, "toggleFanCirculation", [data: [nextPhase: "OFF"]])
        } else {
            sendIfChangedStateValue("fanPhase", "OFF Phase")
            logInfo "Fan Circulation Loop: Setting fan to AUTO for ${safeOff} minutes."
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
    tileSb.append("<b>House Temp:</b> ").append(t1Temp).append(" | <b>Office Mode:</b> ").append(t2Mode).append("<br/>")
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

    if (fanCirculateEnabled) {
        Integer onMin = safeToInt(fanOnMinutes, 30)
        Integer offMin = safeToInt(fanOffMinutes, 30)
        if (onMin < 1 || onMin > 240) errors.add("Fan ON duration must be between 1 and 240 minutes.")
        if (offMin < 1 || offMin > 240) errors.add("Fan OFF duration must be between 1 and 240 minutes.")
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