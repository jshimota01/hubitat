/**
 *  ================================================================================================
 *  Multiperiod Environment Manager
 *  ================================================================================================
 *
 *  Description:
 *      Schedules 6 heating/cooling setpoint windows with switch overrides, live device tracking, 
 *      dynamic fan circulation, 6-tier EPA AQI Air Quality monitoring with health action strings, 
 *      independent Air Filter control, and dashboard tile output.
 *
 *  Author:         James Shimota (jshimota)
 *  App Name:       Multiperiod Environment Manager
 *  Category:       Convenience, HVAC, Thermostat, Fan
 *  License:        Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 *
 *  Documentation & Repository:
 *      https://github.com/jshimota/hubitat-smartapps
 *
 *  Change History:
 *
 *      Date          Source        Version     What
 *      ----          ------        -------     ----
 *      2026-06-01    jshimota      0.0.1       Initial release
 *      2026-08-01    Gemini        0.0.4       Added Air Filter AQI control
 *      2026-08-05    jshimota      0.0.5       Implemented unified logging, auto-debug disable, and sendIfChanged routine
 *      2026-08-05    jshimota      0.0.6       Renamed app to Multiperiod Environment Manager
 *      2026-08-05    Gemini        0.0.7       Bug check fix: quoted subscribe/schedule handlers, fixed boundary condition in isTimeBetween
 *      2026-08-05    jshimota      0.0.8       Added child tile device support and HTML memTile attribute output
 *      2026-08-05    Gemini        0.0.9       Replaced Mode overrides with Switch devices, split Air Filter into separate section
 *      2026-08-05    Gemini        0.1.0       Made Air Filter independently toggleable
 *      2026-08-05    Gemini        0.1.1       Updated AQI sensor pick list to Hubitat capability.airQuality
 *      2026-08-05    jshimota      0.1.2       Execute immediate AQI evaluation and average upon pressing Done (installed/updated)
 *      2026-08-05    jshimota      0.1.3       Renamed section to Room Air Filter Control, default ON, added numerical AQI threshold table
 *      2026-08-05    jshimota      0.1.4       Added AQI thresholds and color table to Air Quality Monitoring section UI
 *      2026-08-05    jshimota      0.1.5       Immediate Air Filter switch status evaluation on app save/initialize
 *      2026-08-05    Gemini        0.1.6       Fixed AQI evaluation null guard on initialize, forced direct switch state refresh
 *      2026-08-05    Gemini        0.1.8       Made AQI range thresholds fully configurable via app settings
 *      2026-08-05    Gemini        0.1.9       Execute dynamic AQI evaluation directly inside mainPage UI render pass
 *      2026-08-05    Gemini        0.2.0       Fixed UnknownDeviceTypeException by switching child device type to 'Virtual Variable'
 *      2026-08-05    jshimota      0.2.1       Fixed ensureChildDevice syntax error and map bracket structure
 *      2026-08-05    Gemini        0.2.2       Updated ensureChildDevice to target custom 'MEM Dashboard Tile' driver
 *      2026-08-05    Gemini        0.2.3       Completed comprehensive 17-point verification audit and code pass
 *      2026-08-05    Gemini        0.2.4       Refactored transient state variables into local variables to reduce DB writes
 *      2026-08-05    Gemini        0.2.5       Added memTile HTML string comparison guard to suppress redundant tile events
 *      2026-08-05    Gemini        0.2.6       Standardized event dispatches to route exclusively through sendIfChanged
 *      2026-08-05    Gemini        0.2.7       Separated sendIfChanged into updateStateValue and updateDeviceAttribute
 *      2026-08-05    Gemini        0.2.8       Renamed helper routines to sendIfChangedStateValue and sendIfChangedAttributeValue
 *      2026-08-05    Gemini        0.2.9       Removed evaluateAirQuality side-effects from mainPage UI rendering
 *      2026-08-05    Gemini        0.3.0       Cached targetThermostat attribute reads into single-pass local variables
 *      2026-08-05    Gemini        0.3.1       Hardened fan circulation loop timers against reboot/orphan states
 *      2026-08-05    Gemini        0.3.2       Cached logging flags during initialize() to eliminate repeated settings lookups
 *      2026-08-05    Gemini        0.3.3       Decomposed evaluateAirQuality into modular sub-routines (SRP)
 *      2026-08-05    Gemini        0.3.4       Replaced HTML string concatenations with StringBuilder
 *      2026-08-05    Gemini        0.3.5       Replaced 5-min polling loop with exact schedule boundary scheduling
 *      2026-08-05    Gemini        0.3.6       Centralized AQI threshold evaluation into getAqiStatus(avg) helper
 *      2026-08-05    Gemini        0.3.7       Wrapped all device state queries and command dispatches in missing device recovery handlers
 *      2026-08-05    Gemini        0.3.8       Standardized method typing: private void for actions, explicit object types for returns
 *      2026-08-05    Gemini        0.3.9       Expanded top-level header metadata block
 *      2026-08-05    Gemini        0.4.0       Added comprehensive Javadoc/GroovyDoc documentation to all methods
 *      2026-08-05    Gemini        0.4.1       Converted schedule evaluation from Date objects to minutes-since-midnight arithmetic
 *      2026-08-05    Gemini        0.5.0       Reorganized full codebase into 10 explicit functional architecture sections
 *      2026-08-05    Gemini        0.5.1       Decomposed Dashboard Tile Engine into buildTileHtml, updateTile, and ensureChildDevice
 *      2026-08-05    Gemini        0.5.2       Decomposed AQI Engine into readSensors, calculateAverage, determineStatus, updateRGB, updateAirFilter, updateDashboard
 *      2026-08-05    Gemini        0.5.3       Consolidated fragmented device attribute reads into unified thermostat state map snapshots
 *      2026-08-05    Gemini        0.6.0       Enforced strict architectural separation: mainPage UI passes are now strictly read-only
 *      2026-08-05    Gemini        0.6.1       Audited structural organization across all 10 conceptual sections
 *      2026-08-05    Gemini        0.7.0       Implemented 6-tier EPA AQI thresholds, configurable override boundaries, and updated UI tables
 *      2026-08-05    Gemini        0.7.1       Added AQI health action strings to status mapping and tile HTML renderer
 *      2026-08-07    Gemini        0.7.2       Updated updateRGB to turn off RGB light when AQI is Good
 *      2026-08-07    Gemini        0.7.3       Turn off RGB light when Hub Mode (or override switch) is Sleeping or Away
 *      2026-08-07    Gemini        0.7.4       Restricted active RGB indicator output strictly to Home or Awake modes
 *      2026-08-07    Gemini        0.7.5       Fixed NullPointerException in mainPage UI table rendering using safeToInt helper
 *      2026-08-07    Gemini        0.7.6       Updated Room Air Filter table headers to 'Indicator Light' and 'Room Air Filter State'
 *      2026-08-07    Gemini        0.7.7       Renamed Room Air Filter setting labels to 'Enable Automated Room Air Filter Control' and 'Select Room Air Filter Switch/Device'
 *      2026-08-07    Gemini        0.7.8       Updated Room Air Filter Control description text
 *      2026-08-07    Gemini        0.8.0       Executed comprehensive bug audit and optimized tile HTML synchronization pass
 *      2026-08-07    Gemini        0.8.1       Fixed critical schedule overwrite bug by passing [overwrite: false] to schedule()
 *      2026-08-07    Gemini        0.8.2       Implemented state.fanControlOwned tracking to safely restore fan to AUTO on disable
 *      2026-08-07    Gemini        0.8.3       Made toggleFanCirculation callback authoritative via targetPhase parameter
 *      2026-08-07    Gemini        0.8.4       Added validateAqiThresholds validation helper and UI error alerting
 *      2026-08-07    Gemini        0.8.5       Hardened AQI sensor value parser against non-numeric payloads
 *      2026-08-07    Gemini        0.8.6       Made secondary AQI sensor optional, updated filter wording, verified actual filter state, and cleared RGB on disable
 *      2026-08-07    Gemini        0.8.7       Added fan input range validation, converted setpoints to BigDecimal, and unified tile evaluation pipeline context
 *      2026-08-07    Gemini        0.9.0       Implemented unified evaluation pipeline pass, explicit state device ownership tracking (fan, rgb, filter), and centralized validateConfiguration validation engine
 *      2026-08-07    Gemini        0.9.1       Fixed UI HTML StringBuilder chaining compilation error in mainPage
 *      2026-08-07    Gemini        0.9.2       Fixed NullPointerException in getAqiTierLimits when settings values are uninitialized
 *      2026-08-07    Gemini        0.9.3       Fixed fan ownership bug: only claim fanOwned when MEM explicitly issues a fanOn command
 *      2026-08-07    Gemini        0.9.4       Validated before unscheduling in updated(), eliminated duplicate thermostat/AQI snapshot reads, and added schedule time string validation
 *      2026-08-07    Gemini        0.9.5       Updated AQI validation error message text to 'strictly positive'
 *      2026-08-07    Gemini        0.9.6       Refactored updateTile() to accept pre-captured evaluation context map to eliminate duplicate reads
 */

/**
 * Returns the current application version string.
 * @return String representing semver version.
 */
static String version() { return '0.9.6' }

definition(
    name: "Multiperiod Environment Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Schedules 6 heating/cooling setpoint windows with switch overrides, live device tracking, dynamic fan circulation, 6-tier EPA AQI filtering with health alerts, and dashboard tile output.",
    category: "Convenience, HVAC, Thermostat, Fan",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

// =================================================================================================
// USER INTERFACE (STRICTLY READ-ONLY / NO AUTOMATION SIDE-EFFECTS)
// =================================================================================================

/**
 * Main application setup page rendering dynamic sections, settings inputs, and live device status displays.
 * Strictly read-only pass: performs NO state mutations or device command dispatches.
 * @return DynamicPage configuration object for Hubitat web UI.
 */
def mainPage() {
    dynamicPage(name: "mainPage", title: "Multiperiod Environment Manager Setup v${version()}", install: true, uninstall: true) {
        
        section("<b>Control & Target Thermostat Device</b>") {
            input name: "appEnabled", type: "bool", title: "Enable Automated Setpoint Scheduling", defaultValue: true, submitOnChange: true
            input name: "targetThermostat", type: "capability.thermostat", title: "Select Thermostat / Thermostat Controller", required: true, multiple: false, submitOnChange: true
        }

        section("<b>Switch-Based Overrides</b>") {
            paragraph "When these switches are ON, these setpoints will override time-based schedules."
            input name: "awaySwitch", type: "capability.switch", title: "Select 'Away' Switch", required: false, multiple: false, submitOnChange: true
            input name: "awayHeat", type: "number", title: "Away Heat Setpoint", required: true, defaultValue: 62
            input name: "awayCool", type: "number", title: "Away Cool Setpoint", required: true, defaultValue: 78
            
            input name: "sleepSwitch", type: "capability.switch", title: "Select 'Sleeping' Switch", required: false, multiple: false, submitOnChange: true
            input name: "sleepHeat", type: "number", title: "Sleeping Heat Setpoint", required: true, defaultValue: 65
            input name: "sleepCool", type: "number", title: "Sleeping Cool Setpoint", required: true, defaultValue: 74
        }

        section("<b>Time-Based Schedules</b>") {
            paragraph "Configure start times, end times, and setpoints for normal daily windows. Note: In the event of overlapping periods, the earlier defined window takes evaluation priority."
            
            // Morning
            input name: "morningStart", type: "time", title: "Morning Start Time", required: true, defaultValue: "06:00"
            input name: "morningEnd", type: "time", title: "Morning End Time", required: true, defaultValue: "09:00"
            input name: "morningHeat", type: "number", title: "Morning Heat Setpoint", required: true, defaultValue: 68
            input name: "morningCool", type: "number", title: "Morning Cool Setpoint", required: true, defaultValue: 72

            // Day
            input name: "dayStart", type: "time", title: "Day Start Time", required: true, defaultValue: "09:00"
            input name: "dayEnd", type: "time", title: "Day End Time", required: true, defaultValue: "17:00"
            input name: "dayHeat", type: "number", title: "Day Heat Setpoint", required: true, defaultValue: 66
            input name: "dayCool", type: "number", title: "Day Cool Setpoint", required: true, defaultValue: 75

            // Evening
            input name: "eveningStart", type: "time", title: "Evening Start Time", required: true, defaultValue: "17:00"
            input name: "eveningEnd", type: "time", title: "Evening End Time", required: true, defaultValue: "22:00"
            input name: "eveningHeat", type: "number", title: "Evening Heat Setpoint", required: true, defaultValue: 69
            input name: "eveningCool", type: "number", title: "Evening Cool Setpoint", required: true, defaultValue: 73

            // Night
            input name: "nightStart", type: "time", title: "Night Start Time", required: true, defaultValue: "22:00"
            input name: "nightEnd", type: "time", title: "Night End Time", required: true, defaultValue: "06:00"
            input name: "nightHeat", type: "number", title: "Night Heat Setpoint", required: true, defaultValue: 64
            input name: "nightCool", type: "number", title: "Night Cool Setpoint", required: true, defaultValue: 75
        }

        section("<b>Simulated Fan Circulation Control</b>") {
            paragraph "Cycles the thermostat fan between ON and AUTO to simulate dynamic air circulation."
            input name: "fanCirculateEnabled", type: "bool", title: "Enable Fan Circulation Loop", defaultValue: false, submitOnChange: true
            if (fanCirculateEnabled) {
                input name: "fanOnMinutes", type: "number", title: "Fan ON Duration (1-240 Minutes)", required: true, defaultValue: 30
                input name: "fanOffMinutes", type: "number", title: "Fan OFF/Auto Duration (1-240 Minutes)", required: true, defaultValue: 30
            }
        }

        section("<b>Air Quality Monitoring & RGB Indicator</b>") {
            paragraph "Monitors AQI sensors and averages their airQualityIndex attributes. IF RGB light is selected, will represent AQI with standard 6-tier EPA colors (Active strictly during 'Home' or 'Awake' modes, turned OFF during Good AQI):"
            
            // Validate threshold inputs before rendering table
            Boolean isAqiValid = validateAqiThresholds()
            if (!isAqiValid && aqiEnabled) {
                paragraph "<div style='color:red; background-color:#ffe6e6; padding:8px; border:1px solid red; border-radius:4px;'><b>CONFIGURATION ERROR:</b> AQI Thresholds must be in strictly ascending order (0 &lt; Good &lt; Moderate &lt; Sensitive &lt; Unhealthy &lt; Very Unhealthy). Standard defaults will be applied until corrected.</div>"
            }

            Map limits = getAqiTierLimits()
            Integer tGoodMax = limits.good ?: 50
            Integer tModMax = limits.mod ?: 100
            Integer tSensMax = limits.sens ?: 150
            Integer tUnhealthMax = limits.unhealthy ?: 200
            Integer tVeryUnhealthMax = limits.veryUnhealthy ?: 300

            // 6-Tier AQI Color & Health Impact Table Display
            StringBuilder colorTableSb = new StringBuilder()
            colorTableSb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>")
            colorTableSb.append("<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>")
            colorTableSb.append("<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Level of Concern</th><th style='padding:4px;'>Color</th><th style='padding:4px;'>Health Impact & Actions</th>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>0 – ").append(tGoodMax).append("</td><td style='padding:4px; color:green;'><b>Good (Normal)</b></td><td style='padding:4px;'>⚪ OFF</td><td style='padding:4px;'>Satisfactory air quality. Little to no risk.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>").append(tGoodMax + 1).append(" – ").append(tModMax).append("</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>🟡 Yellow</td><td style='padding:4px;'>Acceptable. Sensitive groups may feel minor symptoms.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>").append(tModMax + 1).append(" – ").append(tSensMax).append("</td><td style='padding:4px; color:orange;'><b>Sensitive Groups</b></td><td style='padding:4px;'>🟠 Orange</td><td style='padding:4px;'>Potentially harmful for kids, elderly, and asthmatics.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>").append(tSensMax + 1).append(" – ").append(tUnhealthMax).append("</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>🔴 Red</td><td style='padding:4px;'>Active danger. Everyone may experience adverse effects.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            colorTableSb.append("<td style='padding:4px;'>").append(tUnhealthMax + 1).append(" – ").append(tVeryUnhealthyMax).append("</td><td style='padding:4px; color:purple;'><b>Very Unhealthy</b></td><td style='padding:4px;'>🟣 Purple</td><td style='padding:4px;'>Health alert. High risk of respiratory irritation.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("<tr>")
            colorTableSb.append("<td style='padding:4px;'>&ge; ").append(tVeryUnhealthyMax + 1).append("</td><td style='padding:4px; color:maroon;'><b>Hazardous</b></td><td style='padding:4px;'>🟤 Maroon</td><td style='padding:4px;'>Emergency conditions. Stay entirely indoors.</td>")
            colorTableSb.append("</tr>")
            colorTableSb.append("</table>")
            paragraph colorTableSb.toString()

            input name: "aqiEnabled", type: "bool", title: "Enable Air Quality Monitoring", defaultValue: false, submitOnChange: true
            if (aqiEnabled) {
                input name: "aqiSensor1", type: "capability.airQuality", title: "Select Primary AQI Sensor", required: true, multiple: false, submitOnChange: true
                input name: "aqiSensor2", type: "capability.airQuality", title: "Select Secondary AQI Sensor (Optional)", required: false, multiple: false, submitOnChange: true
                input name: "rgbLight", type: "capability.colorControl", title: "Select Indicator RGB Light", required: false, multiple: false
                
                paragraph "<b>Configure AQI Tier Threshold Overrides (Upper Boundaries)</b>"
                input name: "aqiGoodMax", type: "number", title: "Good AQI Max (Default: 50)", required: true, defaultValue: 50, submitOnChange: true
                input name: "aqiModMax", type: "number", title: "Moderate AQI Max (Default: 100)", required: true, defaultValue: 100, submitOnChange: true
                input name: "aqiSensMax", type: "number", title: "Sensitive Groups Max (Default: 150)", required: true, defaultValue: 150, submitOnChange: true
                input name: "aqiUnhealthyMax", type: "number", title: "Unhealthy Max (Default: 200)", required: true, defaultValue: 200, submitOnChange: true
                input name: "aqiVeryUnhealthyMax", type: "number", title: "Very Unhealthy Max (Default: 300)", required: true, defaultValue: 300, submitOnChange: true
            }
        }

        section("<b>Room Air Filter Control</b>") {
            paragraph "Automatically controls the room air filter using the calculated AQI when Air Quality Monitoring is enabled."
            
            Map limits = getAqiTierLimits()
            Integer tGoodMax = limits.good ?: 50
            Integer tModMax = limits.mod ?: 100
            Integer tSensMax = limits.sens ?: 150
            Integer tUnhealthMax = limits.unhealthy ?: 200
            Integer tVeryUnhealthMax = limits.veryUnhealthy ?: 300

            // 6-Tier AQI Air Filter Control Table Display
            StringBuilder aqiTableSb = new StringBuilder()
            aqiTableSb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>")
            aqiTableSb.append("<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>")
            aqiTableSb.append("<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Level of Concern</th><th style='padding:4px;'>Indicator Light</th><th style='padding:4px;'>Room Air Filter State</th>")
            aqiTableSb.append("</tr>")
            aqiTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            aqiTableSb.append("<td style='padding:4px;'>0 – ").append(tGoodMax).append("</td><td style='padding:4px; color:green;'><b>Good (Normal)</b></td><td style='padding:4px;'>⚪ OFF</td><td style='padding:4px;'>OFF</td>")
            aqiTableSb.append("</tr>")
            aqiTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            aqiTableSb.append("<td style='padding:4px;'>").append(tGoodMax + 1).append(" – ").append(tModMax).append("</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>🟡 Yellow</td><td style='padding:4px; color:green;'><b>ON</b></td>")
            aqiTableSb.append("</tr>")
            aqiTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            aqiTableSb.append("<td style='padding:4px;'>").append(tModMax + 1).append(" – ").append(tSensMax).append("</td><td style='padding:4px; color:orange;'><b>Sensitive Groups</b></td><td style='padding:4px;'>🟠 Orange</td><td style='padding:4px; color:green;'><b>ON</b></td>")
            aqiTableSb.append("</tr>")
            aqiTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            aqiTableSb.append("<td style='padding:4px;'>").append(tSensMax + 1).append(" – ").append(tUnhealthMax).append("</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>🔴 Red</td><td style='padding:4px; color:green;'><b>ON</b></td>")
            aqiTableSb.append("</tr>")
            aqiTableSb.append("<tr style='border-bottom:1px solid #eee;'>")
            aqiTableSb.append("<td style='padding:4px;'>").append(tUnhealthMax + 1).append(" – ").append(tVeryUnhealthyMax).append("</td><td style='padding:4px; color:purple;'><b>Very Unhealthy</b></td><td style='padding:4px;'>🟣 Purple</td><td style='padding:4px; color:green;'><b>ON</b></td>")
            aqiTableSb.append("</tr>")
            aqiTableSb.append("<tr>")
            aqiTableSb.append("<td style='padding:4px;'>&ge; ").append(tVeryUnhealthyMax + 1).append("</td><td style='padding:4px; color:maroon;'><b>Hazardous</b></td><td style='padding:4px;'>🟤 Maroon</td><td style='padding:4px; color:green;'><b>ON</b></td>")
            aqiTableSb.append("</tr>")
            aqiTableSb.append("</table>")
            paragraph aqiTableSb.toString()

            input name: "airFilterEnabled", type: "bool", title: "Enable Automated Room Air Filter Control", defaultValue: true, submitOnChange: true
            if (airFilterEnabled) {
                input name: "airFilterSwitch", type: "capability.switch", title: "Select Room Air Filter Switch/Device", required: true, multiple: false, submitOnChange: true
            }
        }

        // Single-pass snapshot evaluation for read-only UI rendering
        Map ctx = buildEvaluationContext()
        def v1 = ctx.rawV1
        def v2 = ctx.rawV2
        Integer avgAqi = ctx.aqi as Integer
        
        Map aqiStatusInfo = determineStatus(avgAqi)
        String displayAqiStatus = aqiStatusInfo.status

        Map tStates = ctx.thermostat ?: [:]
        def devHeat = tStates.heat ?: "--"
        def devCool = tStates.cool ?: "--"
        def devThermostatMode = tStates.mode ?: "--"
        def devFanMode = tStates.fan ?: "--"
        def devOpState = tStates.operatingState ?: "--"

        Map scheduleData = ctx.schedule ?: [:]

        section("<b>Current Status</b>") {
            paragraph "<b>Connected Thermostat Device:</b> ${targetThermostat ? targetThermostat.displayName : 'None Selected'}"
            paragraph "<b>Device Live Status:</b> Heat: ${devHeat}° | Cool: ${devCool}° | Mode: ${devThermostatMode} | Fan Mode: ${devFanMode} | State: ${devOpState}"
            paragraph "--------------------------------------------------"
            paragraph "<b>Active Schedule Period:</b> ${scheduleData.activePeriod ?: 'Not Evaluated'}"
            paragraph "<b>Target Setpoints:</b> Heat: ${scheduleData.targetHeat ?: '--'}° | Cool: ${scheduleData.targetCool ?: '--'}°"
            paragraph "<b>Fan Circulation Status:</b> ${fanCirculateEnabled ? (state.fanPhase ?: 'Initializing') : 'Disabled'}"
            paragraph "--------------------------------------------------"
            if (aqiEnabled) {
                paragraph "<b>AQI Sensor 1 (${aqiSensor1?.displayName ?: 'None'}):</b> ${v1 ?: '--'}"
                paragraph "<b>AQI Sensor 2 (${aqiSensor2?.displayName ?: 'None'}):</b> ${v2 ?: '--'}"
                paragraph "<b>Average AQI:</b> ${avgAqi ?: '--'} (${displayAqiStatus})"
                paragraph "<b>Health Impact:</b> ${aqiStatusInfo.actionText ?: 'N/A'}"
            } else {
                paragraph "<b>Air Quality Monitoring:</b> Disabled"
            }
            if (airFilterEnabled) {
                def filterVal = safeGetDeviceAttribute(airFilterSwitch, 'switch')
                paragraph "<b>Air Filter Status:</b> ${filterVal ?: 'No Switch Selected / Unavailable'}"
            } else {
                paragraph "<b>Air Filter Automation:</b> Disabled"
            }
            paragraph "<b>Last Scheduled Evaluation:</b> ${state.lastEvaluated ?: 'Never'}"
        }

        section("<b>Logging Options</b>") {
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging (Auto-disables after 30 mins)", defaultValue: false, submitOnChange: true
            input name: "logTraceEnable", type: "bool", title: "Enable Trace Logging", defaultValue: false
            input name: "logWarnEnable", type: "bool", title: "Enable Warning Logging", defaultValue: true
            input name: "logErrorEnable", type: "bool", title: "Enable Error Logging", defaultValue: true
        }

        section("<b>App Name</b>") {
            label title: "Assign a name for this app instance", required: false
        }
    }
}

// =================================================================================================
// 1. INITIALIZATION ENGINE
// =================================================================================================

/**
 * Hubitat Lifecycle Hook: Executed when the SmartApp is first installed.
 */
def installed() {
    logInfo "Installed app instance v${version()}..."
    initialize()
}

/**
 * Hubitat Lifecycle Hook: Executed when app settings are saved/updated.
 * Validates configuration FIRST before clearing subscriptions to prevent bricking active automation.
 */
def updated() {
    logInfo "Updated app instance v${version()} settings..."

    Map validation = validateConfiguration()
    if (!validation.valid) {
        validation.errors.each { err ->
            logError "CONFIGURATION VALIDATION ERROR: ${err}"
        }
        return
    }

    unsubscribe()
    unschedule()
    initialize()
}

/**
 * Main initialization lifecycle method. Configures subscriptions, exact time boundary schedules, 
 * logging flags, child tile creation, and executes immediate evaluation passes.
 */
def initialize() {
    // Cache logging preferences in memory
    cacheLoggingFlags()

    // Manage dynamic debug logging schedule
    if (logDebugEnable) {
        logDebug "Debug logging enabled. Scheduling auto-disable in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }

    // Ensure child tile device exists
    ensureChildDevice()

    // Subscribe to Hub Mode changes
    subscribe(location, "mode", "modeChangeHandler")

    // Subscribe to Override Switches
    if (awaySwitch) {
        subscribe(awaySwitch, "switch", "overrideSwitchHandler")
    }
    if (sleepSwitch) {
        subscribe(sleepSwitch, "switch", "overrideSwitchHandler")
    }
    
    // Subscribe to target thermostat event changes
    if (targetThermostat) {
        subscribe(targetThermostat, "heatingSetpoint", "thermostatSetpointHandler")
        subscribe(targetThermostat, "coolingSetpoint", "thermostatSetpointHandler")
    }
    
    // Subscribe to Air Filter Switch changes
    if (airFilterEnabled && airFilterSwitch) {
        subscribe(airFilterSwitch, "switch", "airFilterSwitchHandler")
    }
    
    // Subscribe to Air Quality Sensors specifically on the airQualityIndex attribute
    if (aqiEnabled) {
        if (aqiSensor1) subscribe(aqiSensor1, "airQualityIndex", "aqiHandler")
        if (aqiSensor2) subscribe(aqiSensor2, "airQualityIndex", "aqiHandler")
    }

    // Schedule exact time boundaries instead of polling
    scheduleTimeBoundaries()
    
    // Run unified evaluation pipeline
    executeEvaluationPass("App Initialize")

    // Initialize or stop Fan Circulation Engine
    if (fanCirculateEnabled) {
        manageFanCirculation(true)
    } else {
        unschedule("toggleFanCirculation")
        if (state.fanOwned == true) {
            logInfo "Fan Circulation disabled while MEM owned fan state. Restoring fan to AUTO."
            try {
                targetThermostat?.fanAuto()
            } catch (Exception e) {
                logError "Failed to restore fan to AUTO during disable: ${e.message}"
            }
            sendIfChangedStateValue("fanOwned", false)
        }
        sendIfChangedStateValue("fanPhase", "Disabled")
    }
}

// =================================================================================================
// 2. UNIFIED EVALUATION PIPELINE PASS
// =================================================================================================

/**
 * Unified Evaluation Pass: Single-pass pipeline that gathers telemetry, calculates active targets,
 * dispatches physical commands, manages explicit ownership, and refreshes tile output.
 * @param cause String description of the triggering event or schedule.
 */
def executeEvaluationPass(String cause = "Manual / Scheduled") {
    if (!appEnabled) {
        logInfo "MEM Pass skipped: Application is disabled."
        releaseDeviceOwnership()
        updateTile()
        return
    }

    logDebug "Starting Unified Evaluation Pass. Trigger Cause: '${cause}'"

    // STEP 1: CAPTURE TELEMETRY SNAPSHOT (SINGLE PASS)
    Map sched = getCalculatedScheduleData()
    Map thermostat = getThermostatStateSnapshot()
    
    Integer calculatedAqi = null
    Map aqiStatus = [status: "Disabled", colorMap: null, actionText: "N/A", filterOn: false]
    
    if (aqiEnabled && (aqiSensor1 || aqiSensor2)) {
        Map rawVals = readSensors()
        calculatedAqi = calculateAverage(rawVals.v1, rawVals.v2)
        if (calculatedAqi != null) {
            aqiStatus = determineStatus(calculatedAqi)
        }
    }

    // STEP 2: DISPATCH THERMOSTAT SETPOINTS (USES SINGLE-PASS SNAPSHOT)
    sendIfChangedStateValue("activeSet", sched.activePeriod)
    sendIfChangedStateValue("lastEvaluated", new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))

    if (sched.targetHeat != null && sched.targetCool != null && targetThermostat) {
        applySetpoints(sched.targetHeat as BigDecimal, sched.targetCool as BigDecimal, thermostat)
    }

    // STEP 3: DISPATCH RGB INDICATOR (WITH EXPLICIT OWNERSHIP)
    if (aqiEnabled && rgbLight) {
        String currentMode = location.mode?.toLowerCase() ?: ""
        Boolean isAllowedMode = currentMode.contains("home") || currentMode.contains("awake")
        Boolean isAwayOverride = (awaySwitch && safeGetDeviceAttribute(awaySwitch, "switch") == "on")
        Boolean isSleepOverride = (sleepSwitch && safeGetDeviceAttribute(sleepSwitch, "switch") == "on")

        if (isAllowedMode && !isAwayOverride && !isSleepOverride && aqiStatus.colorMap != null) {
            if (safeGetDeviceAttribute(rgbLight, "switch") != "on") {
                rgbLight.on()
            }
            rgbLight.setColor(aqiStatus.colorMap)
            sendIfChangedStateValue("rgbOwned", true)
        } else {
            if (state.rgbOwned == true || safeGetDeviceAttribute(rgbLight, "switch") != "off") {
                logInfo "Turning off AQI RGB Light (${rgbLight.displayName}) - Mode/AQI condition clear."
                rgbLight.off()
                sendIfChangedStateValue("rgbOwned", false)
            }
        }
    } else if (!aqiEnabled && state.rgbOwned == true) {
        releaseDeviceOwnership()
    }

    // STEP 4: DISPATCH ROOM AIR FILTER (WITH EXPLICIT OWNERSHIP)
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

    // STEP 5: REFRESH DASHBOARD TILE USING PRE-CAPTURED CONTEXT SNAPSHOT
    Map context = [
        schedule: sched,
        thermostat: thermostat,
        aqi: calculatedAqi,
        filterState: actualFilterState
    ]
    
    updateTile(context)
}

// =================================================================================================
// 3. SCHEDULING & EVENT HANDLER ENGINE
// =================================================================================================

/**
 * Schedules exact cron/time triggers based on configured schedule start/end preferences.
 */
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

/**
 * Converts a time preference input (either "HH:mm" or Hubitat ISO string) into minutes since midnight (0..1439).
 * @param timeInput String time setting or ISO string.
 * @return Integer minutes since midnight, or null if unparseable.
 */
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

/**
 * Evaluates whether current minutes-since-midnight falls within a target window.
 * @param startMin Integer window start time in minutes since midnight.
 * @param endMin Integer window end time in minutes since midnight.
 * @param currentMin Integer current time in minutes since midnight.
 * @return Boolean true if current minute falls within the specified period window.
 */
private Boolean isTimeInWindow(Integer startMin, Integer endMin, Integer currentMin) {
    if (startMin == null || endMin == null || currentMin == null) return false
    if (startMin <= endMin) {
        return (currentMin >= startMin && currentMin < endMin)
    } else {
        return (currentMin >= startMin || currentMin < endMin)
    }
}

/**
 * Computes active schedule period and target heating/cooling setpoints using minute-based arithmetic.
 * First matching time period takes priority when schedules overlap.
 * @return Map containing activePeriod, targetHeat, and targetCool.
 */
private Map getCalculatedScheduleData() {
    if (!appEnabled) {
        return [activePeriod: "Disabled (App OFF)", targetHeat: null, targetCool: null]
    }

    Calendar nowCal = Calendar.getInstance(location.timeZone)
    Integer currentMin = (nowCal.get(Calendar.HOUR_OF_DAY) * 60) + nowCal.get(Calendar.MINUTE)

    String activePeriod = ""
    BigDecimal targetHeat = null
    BigDecimal targetCool = null

    // 1. Check Switch Overrides First
    Boolean isAway = awaySwitch ? (safeGetDeviceAttribute(awaySwitch, "switch") == "on") : false
    Boolean isSleeping = sleepSwitch ? (safeGetDeviceAttribute(sleepSwitch, "switch") == "on") : false

    if (isAway) {
        activePeriod = "Away"
        targetHeat = safeToDecimal(awayHeat, 62.0)
        targetCool = safeToDecimal(awayCool, 78.0)
    } else if (isSleeping) {
        activePeriod = "Sleeping"
        targetHeat = safeToDecimal(sleepHeat, 65.0)
        targetCool = safeToDecimal(sleepCool, 74.0)
    } else {
        // 2. Evaluate Time Windows using Minutes Since Midnight
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
            targetHeat = safeToDecimal(morningHeat, 68.0)
            targetCool = safeToDecimal(morningCool, 72.0)
        } else if (isTimeInWindow(dStart, dEnd, currentMin)) {
            activePeriod = "Day"
            targetHeat = safeToDecimal(dayHeat, 66.0)
            targetCool = safeToDecimal(dayCool, 75.0)
        } else if (isTimeInWindow(eStart, eEnd, currentMin)) {
            activePeriod = "Evening"
            targetHeat = safeToDecimal(eveningHeat, 69.0)
            targetCool = safeToDecimal(eveningCool, 73.0)
        } else if (isTimeInWindow(nStart, nEnd, currentMin)) {
            activePeriod = "Night"
            targetHeat = safeToDecimal(nightHeat, 64.0)
            targetCool = safeToDecimal(nightCool, 75.0)
        } else {
            activePeriod = "Default / Unassigned Window"
        }
    }

    return [activePeriod: activePeriod, targetHeat: targetHeat, targetCool: targetCool]
}

def modeChangeHandler(evt) { executeEvaluationPass("Hub Mode Changed (${evt?.value})") }
def overrideSwitchHandler(evt) { executeEvaluationPass("Override Switch Changed (${evt?.device?.displayName} = ${evt?.value})") }
def thermostatSetpointHandler(evt) { executeEvaluationPass("Thermostat Setpoint Updated (${evt?.name} = ${evt?.value}°)") }
def aqiHandler(evt) { executeEvaluationPass("AQI Sensor Updated (${evt?.device?.displayName} = ${evt?.value})") }
def airFilterSwitchHandler(evt) { executeEvaluationPass("Air Filter Switch State Changed (${evt?.value})") }
def evaluateSchedule() { executeEvaluationPass("Scheduled Window Boundary Trigger") }
def evaluateAirQuality() { executeEvaluationPass("Direct AQI Evaluation Call") }

// =================================================================================================
// 4. THERMOSTAT CONTROL ENGINE
// =================================================================================================

/**
 * Queries target thermostat once and returns consolidated snapshot map of key operational attributes.
 * @return Map containing heat, cool, mode, fan, and operatingState.
 */
private Map getThermostatStateSnapshot() {
    if (!targetThermostat) return [:]
    return [
        heat: safeGetDeviceAttribute(targetThermostat, "heatingSetpoint"),
        cool: safeGetDeviceAttribute(targetThermostat, "coolingSetpoint"),
        mode: safeGetDeviceAttribute(targetThermostat, "thermostatMode"),
        fan: safeGetDeviceAttribute(targetThermostat, "thermostatFanMode"),
        operatingState: safeGetDeviceAttribute(targetThermostat, "thermostatOperatingState")
    ]
}

/**
 * Safely dispatches heating and cooling setpoint commands using an existing snapshot map.
 * @param heat Target heating setpoint BigDecimal.
 * @param cool Target cooling setpoint BigDecimal.
 * @param thermostat Existing thermostat state snapshot Map.
 */
private void applySetpoints(BigDecimal heat, BigDecimal cool, Map thermostat = null) {
    if (!targetThermostat || heat == null || cool == null) return
    
    Map snapshot = thermostat ?: getThermostatStateSnapshot()
    BigDecimal currentHeatSp = safeToDecimal(snapshot.heat)
    BigDecimal currentCoolSp = safeToDecimal(snapshot.cool)

    try {
        if (currentHeatSp != heat) {
            logInfo "Setting ${targetThermostat.displayName} Heating Setpoint to ${heat}° (Active Set: ${state.activeSet})"
            targetThermostat.setHeatingSetpoint(heat)
        }

        if (currentCoolSp != cool) {
            logInfo "Setting ${targetThermostat.displayName} Cooling Setpoint to ${cool}° (Active Set: ${state.activeSet})"
            targetThermostat.setCoolingSetpoint(cool)
        }
    } catch (Exception e) {
        logError "Failed to apply setpoints to ${targetThermostat.displayName}: ${e.message}"
    }
}

// =================================================================================================
// 5. FAN CIRCULATION ENGINE
// =================================================================================================

/**
 * Controls simulated thermostat fan circulation loops between ON and AUTO modes.
 * Tracks explicit fan control ownership in state.fanOwned.
 * @param isInitial Boolean indicating if this is an explicit initialization pass.
 * @param targetPhase Optional String phase target ("ON" or "OFF").
 */
def manageFanCirculation(Boolean isInitial = false, String targetPhase = null) {
    if (!fanCirculateEnabled || !targetThermostat) return

    unschedule("toggleFanCirculation")

    Integer safeOn = safeFanDuration(fanOnMinutes, 30)
    Integer safeOff = safeFanDuration(fanOffMinutes, 30)

    Integer onTime = safeOn * 60
    Integer offTime = safeOff * 60

    Map thermostat = getThermostatStateSnapshot()

    Boolean executeOnPhase = false
    if (targetPhase != null) {
        executeOnPhase = (targetPhase == "ON")
    } else {
        executeOnPhase = isInitial || state.fanPhase == "OFF Phase" || !state.fanPhase
    }

    try {
        if (executeOnPhase) {
            sendIfChangedStateValue("fanPhase", "ON Phase")
            Boolean memTurnedFanOn = false
            if (thermostat.fan != "on") {
                logInfo "Fan Circulation Loop: Turning fan ON for ${safeOn} minutes."
                targetThermostat.fanOn()
                memTurnedFanOn = true
            }
            if (memTurnedFanOn) {
                sendIfChangedStateValue("fanOwned", true)
            }
            runIn(onTime, "toggleFanCirculation", [data: [nextPhase: "OFF"]])
        } else {
            sendIfChangedStateValue("fanPhase", "OFF Phase")
            
            String opState = thermostat.operatingState
            if (opState != "heating" && opState != "cooling") {
                if (thermostat.fan != "auto") {
                    logInfo "Fan Circulation Loop: Setting fan to AUTO for ${safeOff} minutes."
                    targetThermostat.fanAuto()
                }
            } else {
                logDebug "Fan Circulation Loop: HVAC actively ${opState}. Leaving fan mode undisturbed."
            }
            sendIfChangedStateValue("fanOwned", false)
            runIn(offTime, "toggleFanCirculation", [data: [nextPhase: "ON"]])
        }
    } catch (Exception e) {
        logError "Failed to execute fan circulation command on ${targetThermostat.displayName}: ${e.message}"
    }

    updateTile()
}

def toggleFanCirculation(data) {
    if (!fanCirculateEnabled) return
    String nextPhase = data?.nextPhase ?: "ON"
    logDebug "Fan Circulation Timer Fired. Transitioning to ${nextPhase} phase."
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
    Integer tGoodMax = limits.good ?: 50
    Integer tModMax = limits.mod ?: 100
    Integer tSensMax = limits.sens ?: 150
    Integer tUnhealthMax = limits.unhealthy ?: 200
    Integer tVeryUnhealthMax = limits.veryUnhealthy ?: 300

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
    Map thermostat = getThermostatStateSnapshot()
    
    def raw1 = null
    def raw2 = null
    Integer calculatedAqi = overrideAvgAqi
    if (aqiEnabled && (aqiSensor1 || aqiSensor2)) {
        Map rawVals = readSensors()
        raw1 = rawVals.v1
        raw2 = rawVals.v2
        if (calculatedAqi == null) {
            calculatedAqi = calculateAverage(raw1, raw2)
        }
    }

    String currentFilterState = "N/A"
    if (airFilterEnabled && airFilterSwitch) {
        currentFilterState = filterOverride ?: (safeGetDeviceAttribute(airFilterSwitch, "switch")?.toUpperCase() ?: "OFF")
    }

    return [
        schedule: sched,
        thermostat: thermostat,
        aqi: calculatedAqi,
        rawV1: raw1,
        rawV2: raw2,
        filterState: currentFilterState
    ]
}

private String buildTileHtml(Map ctx = null) {
    if (ctx == null) ctx = buildEvaluationContext()

    String appStatus = appEnabled ? "ON" : "OFF"
    
    Map sched = ctx.schedule ?: [:]
    String activePeriod = state.activeSet ?: (sched.activePeriod ?: "Not Evaluated")
    def appHeat = sched.targetHeat ?: "--"
    def appCool = sched.targetCool ?: "--"

    Map thermostat = ctx.thermostat ?: [:]
    def realHeat = thermostat.heat ?: "--"
    def realCool = thermostat.cool ?: "--"
    
    String avgAqiStr = "N/A"
    String aqiNoteStr = "AQI Mon: Disabled"
    Integer avgAqi = ctx.aqi as Integer

    if (avgAqi != null) {
        avgAqiStr = "${avgAqi}"
        Map statusInfo = determineStatus(avgAqi)
        aqiNoteStr = "${statusInfo.status}<br/><i>${statusInfo.actionText}</i>"
    }
    
    String filterStatus = ctx.filterState ?: "N/A"

    StringBuilder tileSb = new StringBuilder()
    tileSb.append("<div style='font-size:12px; line-height:1.3; text-align:left; padding:4px;'>")
    tileSb.append("<b>MEM State:</b> ").append(appStatus).append(" | <b>Period:</b> ").append(activePeriod).append("<br/>")
    tileSb.append("<b>Device Setpoints:</b> ").append(realHeat).append("° / ").append(realCool).append("°<br/>")
    tileSb.append("<b>Target Setpoints:</b> ").append(appHeat).append("° / ").append(appCool).append("°<br/>")
    tileSb.append("<b>AQI Avg:</b> ").append(avgAqiStr).append(" | <b>Air Filter:</b> ").append(filterStatus).append("<br/>")
    tileSb.append("<b>Air Impact:</b> ").append(aqiNoteStr)
    tileSb.append("</div>")

    return tileSb.toString()
}

/**
 * Refreshes child tile output. If an existing context snapshot map is supplied, reuses it directly
 * without querying devices. Otherwise, safely builds context on-demand.
 * @param context Pre-captured evaluation context Map snapshot.
 */
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

private void cacheLoggingFlags() {
    state.logFlags = [
        info: logInfoEnable != false,
        debug: logDebugEnable == true,
        trace: logTraceEnable == true,
        warn: logWarnEnable != false,
        error: logErrorEnable != false
    ]
}

void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
    cacheLoggingFlags()
}

private void logMessage(String level, String msg) {
    Map flags = state.logFlags ?: [info: true, debug: false, trace: false, warn: true, error: true]
    if (flags[level] == true) {
        log."${level}" "${app.label ?: 'Multiperiod Environment Manager'}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

// =================================================================================================
// 9. UTILITY & VALIDATION ENGINE
// =================================================================================================

private Map validateConfiguration() {
    List errors = []

    if (!targetThermostat) {
        errors.add("Target Thermostat device is required.")
    }

    // Validate Setpoint Boundaries
    BigDecimal hAway = safeToDecimal(awayHeat, 62.0)
    BigDecimal cAway = safeToDecimal(awayCool, 78.0)
    if (hAway >= cAway) errors.add("Away Heat setpoint (${hAway}°) must be strictly less than Away Cool setpoint (${cAway}°).")

    BigDecimal hSleep = safeToDecimal(sleepHeat, 65.0)
    BigDecimal cSleep = safeToDecimal(sleepCool, 74.0)
    if (hSleep >= cSleep) errors.add("Sleeping Heat setpoint (${hSleep}°) must be strictly less than Sleeping Cool setpoint (${cSleep}°).")

    List periods = [
        [name: "Morning", heat: morningHeat, cool: morningCool, defH: 68.0, defC: 72.0, start: morningStart, end: morningEnd],
        [name: "Day", heat: dayHeat, cool: dayCool, defH: 66.0, defC: 75.0, start: dayStart, end: dayEnd],
        [name: "Evening", heat: eveningHeat, cool: eveningCool, defH: 69.0, defC: 73.0, start: eveningStart, end: eveningEnd],
        [name: "Night", heat: nightHeat, cool: nightCool, defH: 64.0, defC: 75.0, start: nightStart, end: nightEnd]
    ]

    periods.each { p ->
        BigDecimal h = safeToDecimal(p.heat, p.defH as BigDecimal)
        BigDecimal c = safeToDecimal(p.cool, p.defC as BigDecimal)
        if (h >= c) errors.add("${p.name} Heat setpoint (${h}°) must be strictly less than ${p.name} Cool setpoint (${c}°).")
        
        Integer startMin = timeToMinutes(p.start)
        Integer endMin = timeToMinutes(p.end)
        
        if (startMin == null) errors.add("${p.name} Start Time is invalid or missing.")
        if (endMin == null) errors.add("${p.name} End Time is invalid or missing.")
        if (startMin != null && endMin != null && startMin == endMin) {
            errors.add("${p.name} Start Time and End Time cannot be identical.")
        }
    }

    if (fanCirculateEnabled) {
        Integer onMin = safeToInt(fanOnMinutes, 30)
        Integer offMin = safeToInt(fanOffMinutes, 30)
        if (onMin < 1 || onMin > 240) errors.add("Fan ON duration must be between 1 and 240 minutes.")
        if (offMin < 1 || offMin > 240) errors.add("Fan OFF duration must be between 1 and 240 minutes.")
    }

    if (aqiEnabled) {
        if (!aqiSensor1) errors.add("Primary AQI Sensor is required when Air Quality Monitoring is enabled.")
        if (!validateAqiThresholds()) errors.add("AQI Tier Thresholds must be strictly positive and strictly ascending (0 < Good < Mod < Sens < Unhealthy < VeryUnhealthy).")
    }

    if (airFilterEnabled && !airFilterSwitch) {
        errors.add("Room Air Filter Switch/Device is required when Automated Room Air Filter Control is enabled.")
    }

    return [valid: errors.isEmpty(), errors: errors]
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

private Integer safeToInt(Object val, Integer defaultVal) {
    if (val == null) return defaultVal
    try {
        String str = val.toString().trim()
        return str.length() > 0 ? str.toInteger() : defaultVal
    } catch (Exception e) {
        return defaultVal
    }
}

private BigDecimal safeToDecimal(Object val, BigDecimal defaultVal = null) {
    if (val == null) return defaultVal
    try {
        String str = val.toString().trim()
        return str.length() > 0 ? str.toBigDecimal() : defaultVal
    } catch (Exception e) {
        return defaultVal
    }
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
    } catch (Exception e) {
        logWarn "Failed to parse AQI sensor value '${value}': ${e.message}"
        return null
    }
}

private Object safeGetDeviceAttribute(Object device, String attributeName) {
    if (!device) return null
    try {
        return device.currentValue(attributeName)
    } catch (Exception e) {
        logWarn "Could not read attribute '${attributeName}' from device '${device}': ${e.message}"
        return null
    }
}

private void sendIfChangedStateValue(String key, Object value) {
    if (!key) return
    String oldVal = state[key]?.toString()
    String newVal = value != null ? value.toString() : ""
    if (oldVal != newVal) {
        state[key] = value
        logDebug "State updated: ${key} -> ${value}"
    }
}

private void sendIfChangedAttributeValue(Object device, String attributeName, Object value) {
    if (!device || !attributeName) return
    try {
        String oldVal = device.currentValue(attributeName as String)?.toString()
        String newVal = value != null ? value.toString() : ""
        if (oldVal != newVal) {
            device.sendEvent(name: attributeName, value: value)
            logDebug "Child Device Attribute updated: ${attributeName} -> ${value}"
        }
    } catch (Exception e) {
        logError "Failed to update device attribute '${attributeName}' on ${device}: ${e.message}"
    }
}