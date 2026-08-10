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
 */

/**
 * Returns the current application version string.
 * @return String representing semver version.
 */
static String version() { return '0.7.4' }

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
            paragraph "Configure start times, end times, and setpoints for normal daily windows."
            
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
                input name: "fanOnMinutes", type: "number", title: "Fan ON Duration (Minutes)", required: true, defaultValue: 30
                input name: "fanOffMinutes", type: "number", title: "Fan OFF/Auto Duration (Minutes)", required: true, defaultValue: 30
            }
        }

        section("<b>Air Quality Monitoring & RGB Indicator</b>") {
            paragraph "Monitors two airQuality sensors and averages their airQualityIndex attributes. IF RGB light is selected, will represent AQI with standard 6-tier EPA colors (Active strictly during 'Home' or 'Awake' modes, turned OFF during Good AQI):"
            
            Integer tGoodMax = (aqiGoodMax != null) ? (aqiGoodMax as Integer) : 50
            Integer tModMax = (aqiModMax != null) ? (aqiModMax as Integer) : 100
            Integer tSensMax = (aqiSensMax != null) ? (aqiSensMax as Integer) : 150
            Integer tUnhealthMax = (aqiUnhealthyMax != null) ? (aqiUnhealthyMax as Integer) : 200
            Integer tVeryUnhealthMax = (aqiVeryUnhealthyMax != null) ? (aqiVeryUnhealthyMax as Integer) : 300

            // 6-Tier AQI Color & Health Impact Table Display
            StringBuilder colorTableSb = new StringBuilder()
            colorTableSb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>")
                       .append("<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>")
                       .append("<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Level of Concern</th><th style='padding:4px;'>Color</th><th style='padding:4px;'>Health Impact & Actions</th>")
                       .append("</tr>")
                       .append("<tr style='border-bottom:1px solid #eee;'>")
                       .append("<td style='padding:4px;'>0 – ").append(tGoodMax).append("</td><td style='padding:4px; color:green;'><b>Good (Normal)</b></td><td style='padding:4px;'>⚪ OFF</td><td style='padding:4px;'>Satisfactory air quality. Little to no risk.</td>")
                       .append("</tr>")
                       .append("<tr style='border-bottom:1px solid #eee;'>")
                       .append("<td style='padding:4px;'>").append(tGoodMax + 1).append(" – ").append(tModMax).append("</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>🟡 Yellow</td><td style='padding:4px;'>Acceptable. Sensitive groups may feel minor symptoms.</td>")
                       .append("</tr>")
                       .append("<tr style='border-bottom:1px solid #eee;'>")
                       .append("<td style='padding:4px;'>").append(tModMax + 1).append(" – ").append(tSensMax).append("</td><td style='padding:4px; color:orange;'><b>Sensitive Groups</b></td><td style='padding:4px;'>🟠 Orange</td><td style='padding:4px;'>Potentially harmful for kids, elderly, and asthmatics.</td>")
                       .append("</tr>")
                       .append("<tr style='border-bottom:1px solid #eee;'>")
                       .append("<td style='padding:4px;'>").append(tSensMax + 1).append(" – ").append(tUnhealthyMax).append("</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>🔴 Red</td><td style='padding:4px;'>Active danger. Everyone may experience adverse effects.</td>")
                       .append("</tr>")
                       .append("<tr style='border-bottom:1px solid #eee;'>")
                       .append("<td style='padding:4px;'>").append(tUnhealthyMax + 1).append(" – ").append(tVeryUnhealthMax).append("</td><td style='padding:4px; color:purple;'><b>Very Unhealthy</b></td><td style='padding:4px;'>🟣 Purple</td><td style='padding:4px;'>Health alert. High risk of respiratory irritation.</td>")
                       .append("</tr>")
                       .append("<tr>")
                       .append("<td style='padding:4px;'>&ge; ").append(tVeryUnhealthMax + 1).append("</td><td style='padding:4px; color:maroon;'><b>Hazardous</b></td><td style='padding:4px;'>🟤 Maroon</td><td style='padding:4px;'>Emergency conditions. Stay entirely indoors.</td>")
                       .append("</tr>")
                       .append("</table>")
            paragraph colorTableSb.toString()

            input name: "aqiEnabled", type: "bool", title: "Enable Air Quality Monitoring", defaultValue: false, submitOnChange: true
            if (aqiEnabled) {
                input name: "aqiSensor1", type: "capability.airQuality", title: "Select Primary AQI Sensor", required: true, multiple: false, submitOnChange: true
                input name: "aqiSensor2", type: "capability.airQuality", title: "Select Secondary AQI Sensor", required: true, multiple: false, submitOnChange: true
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
            paragraph "Independently turns on an Air Filter device based on calculated AQI thresholds."
            
            Integer tGoodMax = (aqiGoodMax != null) ? (aqiGoodMax as Integer) : 50
            Integer tModMax = (aqiModMax != null) ? (aqiModMax as Integer) : 100
            Integer tSensMax = (aqiSensMax != null) ? (aqiSensMax as Integer) : 150
            Integer tUnhealthMax = (aqiUnhealthyMax != null) ? (aqiUnhealthyMax as Integer) : 200
            Integer tVeryUnhealthMax = (aqiVeryUnhealthyMax != null) ? (aqiVeryUnhealthyMax as Integer) : 300

            // 6-Tier AQI Air Filter Control Table Display
            StringBuilder aqiTableSb = new StringBuilder()
            aqiTableSb.append("<table style='width:100%; border-collapse:collapse; font-size:12px; margin-bottom:10px; text-align:left;'>")
                     .append("<tr style='background-color:#f2f2f2; border-bottom:1px solid #ccc;'>")
                     .append("<th style='padding:4px;'>AQI Range</th><th style='padding:4px;'>Level of Concern</th><th style='padding:4px;'>Indicator</th><th style='padding:4px;'>Air Filter State</th>")
                     .append("</tr>")
                     .append("<tr style='border-bottom:1px solid #eee;'>")
                     .append("<td style='padding:4px;'>0 – ").append(tGoodMax).append("</td><td style='padding:4px; color:green;'><b>Good (Normal)</b></td><td style='padding:4px;'>⚪ OFF</td><td style='padding:4px;'>OFF</td>")
                     .append("</tr>")
                     .append("<tr style='border-bottom:1px solid #eee;'>")
                     .append("<td style='padding:4px;'>").append(tGoodMax + 1).append(" – ").append(tModMax).append("</td><td style='padding:4px; color:#b8860b;'><b>Moderate</b></td><td style='padding:4px;'>🟡 Yellow</td><td style='padding:4px; color:green;'><b>ON</b></td>")
                     .append("</tr>")
                     .append("<tr style='border-bottom:1px solid #eee;'>")
                     .append("<td style='padding:4px;'>").append(tModMax + 1).append(" – ").append(tSensMax).append("</td><td style='padding:4px; color:orange;'><b>Sensitive Groups</b></td><td style='padding:4px;'>🟠 Orange</td><td style='padding:4px; color:green;'><b>ON</b></td>")
                     .append("</tr>")
                     .append("<tr style='border-bottom:1px solid #eee;'>")
                     .append("<td style='padding:4px;'>").append(tSensMax + 1).append(" – ").append(tUnhealthyMax).append("</td><td style='padding:4px; color:red;'><b>Unhealthy</b></td><td style='padding:4px;'>🔴 Red</td><td style='padding:4px; color:green;'><b>ON</b></td>")
                     .append("</tr>")
                     .append("<tr style='border-bottom:1px solid #eee;'>")
                     .append("<td style='padding:4px;'>").append(tUnhealthyMax + 1).append(" – ").append(tVeryUnhealthyMax).append("</td><td style='padding:4px; color:purple;'><b>Very Unhealthy</b></td><td style='padding:4px;'>🟣 Purple</td><td style='padding:4px; color:green;'><b>ON</b></td>")
                     .append("</tr>")
                     .append("<tr>")
                     .append("<td style='padding:4px;'>&ge; ").append(tVeryUnhealthMax + 1).append("</td><td style='padding:4px; color:maroon;'><b>Hazardous</b></td><td style='padding:4px;'>🟤 Maroon</td><td style='padding:4px; color:green;'><b>ON</b></td>")
                     .append("</tr>")
                     .append("</table>")
            paragraph aqiTableSb.toString()

            input name: "airFilterEnabled", type: "bool", title: "Enable Automated Air Filter Control", defaultValue: true, submitOnChange: true
            if (airFilterEnabled) {
                input name: "airFilterSwitch", type: "capability.switch", title: "Select Air Filter Switch/Device", required: true, multiple: false, submitOnChange: true
            }
        }

        // Pure Read-Only UI Displays (Side-Effect Free)
        Map rawSensors = readSensors()
        def v1 = rawSensors.v1
        def v2 = rawSensors.v2
        Integer avgAqi = calculateAverage(v1, v2)
        
        Map aqiStatusInfo = determineStatus(avgAqi)
        String displayAqiStatus = aqiStatusInfo.status

        Map tStates = getThermostatStateSnapshot()
        def devHeat = tStates.heat ?: "--"
        def devCool = tStates.cool ?: "--"
        def devThermostatMode = tStates.mode ?: "--"
        def devFanMode = tStates.fan ?: "--"
        def devOpState = tStates.operatingState ?: "--"

        Map scheduleData = getCalculatedScheduleData()

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
 */
def updated() {
    logInfo "Updated app instance v${version()} settings..."
    unsubscribe()
    unschedule()
    initialize()
}

/**
 * Main initialization lifecycle method. Configures subscriptions, exact time boundary schedules, 
 * logging flags, child tile creation, and executes immediate evaluations.
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
    
    // Evaluate AQI, Setpoints, and Tile status immediately on initialize
    if (aqiEnabled) {
        logInfo "Performing immediate Air Quality evaluation upon initialize."
        evaluateAirQuality()
    }
    
    evaluateSchedule()

    // Initialize or stop Fan Circulation Engine
    if (fanCirculateEnabled) {
        manageFanCirculation(true)
    } else {
        unschedule("toggleFanCirculation")
        sendIfChangedStateValue("fanPhase", "Disabled")
    }
}

// =================================================================================================
// 2. SCHEDULING ENGINE
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
                schedule(timeVal, "evaluateSchedule")
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
 * Handles standard intra-day windows (start <= end) and overnight wraparound windows (start > end).
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
 * Pure computation routine.
 * @return Map containing activePeriod, targetHeat, and targetCool.
 */
private Map getCalculatedScheduleData() {
    if (!appEnabled) {
        return [activePeriod: "Disabled (App OFF)", targetHeat: null, targetCool: null]
    }

    Calendar nowCal = Calendar.getInstance(location.timeZone)
    Integer currentMin = (nowCal.get(Calendar.HOUR_OF_DAY) * 60) + nowCal.get(Calendar.MINUTE)

    String activePeriod = ""
    Integer targetHeat = null
    Integer targetCool = null

    // 1. Check Switch Overrides First
    Boolean isAway = awaySwitch ? (safeGetDeviceAttribute(awaySwitch, "switch") == "on") : false
    Boolean isSleeping = sleepSwitch ? (safeGetDeviceAttribute(sleepSwitch, "switch") == "on") : false

    if (isAway) {
        activePeriod = "Away"
        targetHeat = awayHeat as Integer
        targetCool = awayCool as Integer
    } else if (isSleeping) {
        activePeriod = "Sleeping"
        targetHeat = sleepHeat as Integer
        targetCool = sleepCool as Integer
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
            targetHeat = morningHeat as Integer
            targetCool = morningCool as Integer
        } else if (isTimeInWindow(dStart, dEnd, currentMin)) {
            activePeriod = "Day"
            targetHeat = dayHeat as Integer
            targetCool = dayCool as Integer
        } else if (isTimeInWindow(eStart, eEnd, currentMin)) {
            activePeriod = "Evening"
            targetHeat = eveningHeat as Integer
            targetCool = eveningCool as Integer
        } else if (isTimeInWindow(nStart, nEnd, currentMin)) {
            activePeriod = "Night"
            targetHeat = nightHeat as Integer
            targetCool = nightCool as Integer
        } else {
            activePeriod = "Default / Unassigned Window"
        }
    }

    return [activePeriod: activePeriod, targetHeat: targetHeat, targetCool: targetCool]
}

/**
 * Primary scheduled routine for evaluating current HVAC schedules and applying target setpoints.
 */
def evaluateSchedule() {
    Map sched = getCalculatedScheduleData()
    
    if (!appEnabled) {
        logInfo "Multiperiod Environment Manager is currently DISABLED via main switch."
        sendIfChangedStateValue("activeSet", sched.activePeriod)
        updateTile()
        return
    }

    Date now = new Date()

    // Update essential persistent tracking via sendIfChangedStateValue
    sendIfChangedStateValue("activeSet", sched.activePeriod)
    sendIfChangedStateValue("lastEvaluated", now.format("yyyy-MM-dd HH:mm:ss", location.timeZone))

    // Apply setpoints
    if (sched.targetHeat != null && sched.targetCool != null && targetThermostat) {
        applySetpoints(sched.targetHeat as Integer, sched.targetCool as Integer)
    } else {
        logWarn "Could not apply setpoints. Active Period: ${sched.activePeriod}, Heat: ${sched.targetHeat}, Cool: ${sched.targetCool}"
    }

    // Guard/re-arm Fan Circulation Loop if timers cleared or lost after reboot
    if (fanCirculateEnabled && (!state.fanPhase || state.fanPhase == "Disabled" || state.fanPhase == "Initializing")) {
        logInfo "Fan Circulation Loop was uninitialized or interrupted. Restarting loop."
        manageFanCirculation(true)
    }

    // Refresh tile HTML attribute
    updateTile()
}

// =================================================================================================
// 3. OVERRIDE ENGINE
// =================================================================================================

/**
 * Handler for Hub location mode changes.
 * @param evt Hubitat Event object.
 */
def modeChangeHandler(evt) {
    logInfo "Hub Mode Changed to '${evt?.value}'. Re-evaluating schedule and AQI indicator."
    evaluateSchedule()
    if (aqiEnabled) {
        evaluateAirQuality()
    }
}

/**
 * Handler for override switch events (Away, Sleeping).
 * @param evt Hubitat Event object.
 */
def overrideSwitchHandler(evt) {
    logInfo "Override Switch Changed (${evt?.device?.displayName ?: 'Switch'}): ${evt?.value}. Re-evaluating schedule and AQI indicator."
    evaluateSchedule()
    if (aqiEnabled) {
        evaluateAirQuality()
    }
}

// =================================================================================================
// 4. THERMOSTAT CONTROL ENGINE
// =================================================================================================

/**
 * Handler for external target thermostat setpoint updates.
 * @param evt Hubitat Event object.
 */
def thermostatSetpointHandler(evt) {
    logInfo "Thermostat setpoint updated externally: ${evt?.name} = ${evt?.value}°"
    evaluateSchedule()
}

/**
 * Queries the target thermostat once and returns a consolidated snapshot map of key operational attributes.
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
 * Safely dispatches heating and cooling setpoint commands to the target thermostat if setpoints differ.
 * @param heat Target heating setpoint integer.
 * @param cool Target cooling setpoint integer.
 */
private void applySetpoints(Integer heat, Integer cool) {
    if (!targetThermostat) return
    Map thermostat = getThermostatStateSnapshot()
    def currentHeatSp = thermostat.heat
    def currentCoolSp = thermostat.cool

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
 * @param isInitial Boolean indicating if this is an explicit initialization pass.
 */
def manageFanCirculation(Boolean isInitial = false) {
    if (!fanCirculateEnabled || !targetThermostat) return

    // Explicitly unschedule prior toggle timers to prevent overlapping loops
    unschedule("toggleFanCirculation")

    Integer onTime = (fanOnMinutes ?: 30) * 60
    Integer offTime = (fanOffMinutes ?: 30) * 60

    Map thermostat = getThermostatStateSnapshot()

    try {
        if (isInitial || state.fanPhase == "OFF Phase" || !state.fanPhase) {
            // Start ON Phase
            sendIfChangedStateValue("fanPhase", "ON Phase")
            if (thermostat.fan != "on") {
                logInfo "Fan Circulation Loop: Turning fan ON for ${fanOnMinutes ?: 30} minutes."
                targetThermostat.fanOn()
            }
            runIn(onTime, "toggleFanCirculation", [data: [nextPhase: "OFF"]])
        } else {
            // Start OFF Phase
            sendIfChangedStateValue("fanPhase", "OFF Phase")
            
            // Safety Guard: Don't override fan if heating or cooling is active
            String opState = thermostat.operatingState
            if (opState != "heating" && opState != "cooling") {
                if (thermostat.fan != "auto") {
                    logInfo "Fan Circulation Loop: Setting fan to AUTO for ${fanOffMinutes ?: 30} minutes."
                    targetThermostat.fanAuto()
                }
            } else {
                logDebug "Fan Circulation Loop: HVAC actively ${opState}. Leaving fan mode undisturbed."
            }
            runIn(offTime, "toggleFanCirculation", [data: [nextPhase: "ON"]])
        }
    } catch (Exception e) {
        logError "Failed to execute fan circulation command on ${targetThermostat.displayName}: ${e.message}"
    }

    updateTile()
}

/**
 * Scheduled callback target for transitioning fan circulation phases.
 * @param data Map containing nextPhase indicator string ("ON" or "OFF").
 */
def toggleFanCirculation(data) {
    if (!fanCirculateEnabled) return
    logDebug "Fan Circulation Timer Fired. Transitioning to ${data?.nextPhase ?: 'next'} phase."
    manageFanCirculation(false)
}

// =================================================================================================
// 6. AQI ENGINE
// =================================================================================================

/**
 * Event handler for air quality sensor updates.
 * @param evt Hubitat Event object.
 */
def aqiHandler(evt) {
    logDebug "Air Quality Sensor Event (${evt?.device?.displayName ?: 'AQI Sensor'}): ${evt?.name} = ${evt?.value}"
    evaluateAirQuality()
}

/**
 * Reads raw air quality attributes from configured sensors. Pure read routine.
 * @return Map containing raw sensor values v1 and v2.
 */
private Map readSensors() {
    def v1 = safeGetDeviceAttribute(aqiSensor1, "airQualityIndex")
    def v2 = safeGetDeviceAttribute(aqiSensor2, "airQualityIndex")
    return [v1: v1, v2: v2]
}

/**
 * Calculates a rounded average AQI from two sensor inputs, gracefully handling single-sensor fallbacks.
 * Pure computation routine.
 * @param val1 First sensor reading.
 * @param val2 Second sensor reading.
 * @return Integer average AQI value, or null if both readings are unreadable.
 */
private Integer calculateAverage(Object val1, Object val2) {
    if (val1 == null && val2 == null) return null
    Integer v1 = (val1 != null) ? (val1 as Integer) : (val2 as Integer)
    Integer v2 = (val2 != null) ? (val2 as Integer) : (val1 as Integer)
    return Math.round((v1 + v2) / 2.0) as Integer
}

/**
 * Evaluates an average AQI integer against 6 EPA threshold tiers and returns status label, color map, health action string, and filter trigger flag.
 * Pure computation routine.
 * @param avgAqiVal Integer average AQI value.
 * @return Map containing status label string, colorMap, actionText, and filterOn boolean flag.
 */
private Map determineStatus(Object avgAqiVal) {
    if (avgAqiVal == null || !(avgAqiVal instanceof Integer)) {
        return [status: "Not Evaluated", colorMap: null, actionText: "N/A", filterOn: false]
    }
    
    Integer avgAqi = avgAqiVal as Integer
    Integer gMax = (aqiGoodMax != null) ? (aqiGoodMax as Integer) : 50
    Integer mMax = (aqiModMax != null) ? (aqiModMax as Integer) : 100
    Integer sMax = (aqiSensMax != null) ? (aqiSensMax as Integer) : 150
    Integer uMax = (aqiUnhealthyMax != null) ? (aqiUnhealthyMax as Integer) : 200
    Integer vuMax = (aqiVeryUnhealthyMax != null) ? (aqiVeryUnhealthyMax as Integer) : 300

    if (avgAqi <= gMax) {
        return [
            status: "Green (Good)", 
            colorMap: null, 
            actionText: "Satisfactory air quality. Little to no risk.", 
            filterOn: false
        ]
    } else if (avgAqi <= mMax) {
        return [
            status: "Yellow (Moderate)", 
            colorMap: [hue: 16, saturation: 100, level: 100], 
            actionText: "Acceptable. Sensitive groups may feel minor symptoms.", 
            filterOn: true
        ]
    } else if (avgAqi <= sMax) {
        return [
            status: "Orange (Unhealthy for Sensitive Groups)", 
            colorMap: [hue: 8, saturation: 100, level: 100], 
            actionText: "Potentially harmful for kids, elderly, and asthmatics.", 
            filterOn: true
        ]
    } else if (avgAqi <= uMax) {
        return [
            status: "Red (Unhealthy)", 
            colorMap: [hue: 0, saturation: 100, level: 100], 
            actionText: "Active danger. Everyone may experience adverse effects.", 
            filterOn: true
        ]
    } else if (avgAqi <= vuMax) {
        return [
            status: "Purple (Very Unhealthy)", 
            colorMap: [hue: 75, saturation: 100, level: 100], 
            actionText: "Health alert. High risk of respiratory irritation.", 
            filterOn: true
        ]
    } else {
        return [
            status: "Maroon (Hazardous)", 
            colorMap: [hue: 95, saturation: 100, level: 50], 
            actionText: "Emergency conditions. Stay entirely indoors.", 
            filterOn: true
        ]
    }
}

/**
 * Applies color map settings strictly when location mode is 'Home' or 'Awake'.
 * Turns off indicator RGB light device during all other modes or when AQI is Good. Action Routine.
 * @param colorMap Map containing hue, saturation, and level properties (null when Good).
 */
private void updateRGB(Map colorMap) {
    if (!rgbLight) return

    // Check current location mode
    String currentMode = location.mode?.toLowerCase() ?: ""
    Boolean isAllowedMode = currentMode.contains("home") || currentMode.contains("awake")

    // Check override switch states as additional guard
    Boolean isAwayOverride = (awaySwitch && safeGetDeviceAttribute(awaySwitch, "switch") == "on")
    Boolean isSleepOverride = (sleepSwitch && safeGetDeviceAttribute(sleepSwitch, "switch") == "on")

    try {
        if (isAllowedMode && !isAwayOverride && !isSleepOverride && colorMap != null) {
            if (safeGetDeviceAttribute(rgbLight, "switch") != "on") {
                rgbLight.on()
            }
            rgbLight.setColor(colorMap)
        } else {
            if (safeGetDeviceAttribute(rgbLight, "switch") != "off") {
                logInfo "Turning off AQI RGB Light (${rgbLight.displayName}) - Mode: '${location.mode}', ColorMap Present: ${colorMap != null}"
                rgbLight.off()
            }
        }
    } catch (Exception e) {
        logError "Failed to set indicator light status on ${rgbLight.displayName}: ${e.message}"
    }
}

/**
 * Updates the dashboard tile with the calculated AQI value. Action Routine.
 * @param avgAqi Integer average AQI value.
 */
private void updateDashboard(Integer avgAqi) {
    updateTile(avgAqi)
}

/**
 * Orchestrates air quality evaluation by executing sensor reading, averaging, status determination, 
 * light indicator updates, air filter toggles, and tile refreshes. Action Routine.
 * @return Map containing calculated sensor readings and status summary.
 */
def evaluateAirQuality() {
    if (!aqiEnabled) return [:]

    Map rawVals = readSensors()
    Integer avgAqi = calculateAverage(rawVals.v1, rawVals.v2)

    if (avgAqi == null) {
        logWarn "Could not evaluate Air Quality. Both sensors returned null or were unavailable."
        return [:]
    }

    Map statusMap = determineStatus(avgAqi)
    logInfo "AQI Evaluated: Avg ${avgAqi} -> Status: ${statusMap.status} | Action: ${statusMap.actionText} | Filter: ${statusMap.filterOn ? 'ON' : 'OFF'}"

    updateRGB(statusMap.colorMap)
    updateAirFilter(statusMap.filterOn, avgAqi)
    updateDashboard(avgAqi)

    return [v1: rawVals.v1, v2: rawVals.v2, avgAqi: avgAqi, aqiStatus: statusMap.status, actionText: statusMap.actionText]
}

// =================================================================================================
// 7. AIR FILTER ENGINE
// =================================================================================================

/**
 * Handler for Air Filter switch state changes.
 * @param evt Hubitat Event object.
 */
def airFilterSwitchHandler(evt) {
    logInfo "Air Filter Switch state changed externally: ${evt?.value}"
    updateTile()
}

/**
 * Toggles the room air filter switch device based on calculated AQI action flags. Action Routine.
 * @param filterOn Boolean flag indicating if air filter should be active.
 * @param avgAqi Integer calculated average AQI for logging output.
 */
private void updateAirFilter(Boolean filterOn, Integer avgAqi) {
    if (!airFilterEnabled || !airFilterSwitch) return
    String currentFilterState = safeGetDeviceAttribute(airFilterSwitch, "switch")
    try {
        if (filterOn && currentFilterState != "on") {
            logInfo "Turning Air Filter ON due to elevated AQI (${avgAqi})"
            airFilterSwitch.on()
        } else if (!filterOn && currentFilterState != "off") {
            logInfo "Turning Air Filter OFF (AQI Good: ${avgAqi})"
            airFilterSwitch.off()
        }
    } catch (Exception e) {
        logError "Failed to set switch state on Air Filter device ${airFilterSwitch.displayName}: ${e.message}"
    }
}

// =================================================================================================
// 8. DASHBOARD TILE ENGINE
// =================================================================================================

/**
 * Ensures child MEM Dashboard Tile device is created under app instance. Action Routine.
 */
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

/**
 * Pure rendering function that constructs and returns the dashboard HTML string.
 * @param currentAvgAqi Optional Integer average AQI value to override reading pass.
 * @return String HTML snippet representing current MEM status.
 */
private String buildTileHtml(Integer currentAvgAqi = null) {
    String appStatus = appEnabled ? "ON" : "OFF"
    String activePeriod = state.activeSet ?: "Not Evaluated"
    
    Map thermostat = getThermostatStateSnapshot()
    def realHeat = thermostat.heat ?: "--"
    def realCool = thermostat.cool ?: "--"
    
    Map sched = getCalculatedScheduleData()
    def appHeat = sched.targetHeat ?: "--"
    def appCool = sched.targetCool ?: "--"
    
    String avgAqiStr = "N/A"
    String aqiNoteStr = "AQI Mon: Disabled"
    if (currentAvgAqi != null) {
        avgAqiStr = "${currentAvgAqi}"
        Map statusInfo = determineStatus(currentAvgAqi)
        aqiNoteStr = "${statusInfo.status}<br/><i>${statusInfo.actionText}</i>"
    } else if (aqiEnabled && (aqiSensor1 || aqiSensor2)) {
        Map rawVals = readSensors()
        Integer calcAvg = calculateAverage(rawVals.v1, rawVals.v2)
        if (calcAvg != null) {
            avgAqiStr = "${calcAvg}"
            Map statusInfo = determineStatus(calcAvg)
            aqiNoteStr = "${statusInfo.status}<br/><i>${statusInfo.actionText}</i>"
        }
    }
    
    def rawFilterSwitchVal = safeGetDeviceAttribute(airFilterSwitch, "switch")
    String filterStatus = (airFilterEnabled && airFilterSwitch) ? (rawFilterSwitchVal?.toUpperCase() ?: "OFF") : "N/A"

    StringBuilder tileSb = new StringBuilder()
    tileSb.append("<div style='font-size:12px; line-height:1.3; text-align:left; padding:4px;'>")
          .append("<b>MEM State:</b> ").append(appStatus).append(" | <b>Period:</b> ").append(activePeriod).append("<br/>")
          .append("<b>Device Setpoints:</b> ").append(realHeat).append("° / ").append(realCool).append("°<br/>")
          .append("<b>Target Setpoints:</b> ").append(appHeat).append("° / ").append(appCool).append("°<br/>")
          .append("<b>AQI Avg:</b> ").append(avgAqiStr).append(" | <b>Air Filter:</b> ").append(filterStatus).append("<br/>")
          .append("<b>Air Impact:</b> ").append(aqiNoteStr)
          .append("</div>")

    return tileSb.toString()
}

/**
 * Fetches the child tile device and updates its memTile attribute if the HTML content has changed. Action Routine.
 * @param currentAvgAqi Optional Integer average AQI value to override reading pass.
 */
private void updateTile(Integer currentAvgAqi = null) {
    def child = getChildDevice("MEM_TILE_${app.id}")
    if (!child) return

    String tileHtml = buildTileHtml(currentAvgAqi)
    sendIfChangedAttributeValue(child, "memTile", tileHtml)
}

// =================================================================================================
// 9. LOGGING ENGINE
// =================================================================================================

/**
 * Caches log settings flags into state map to avoid repeated settings map lookups during execution passes.
 */
private void cacheLoggingFlags() {
    state.logFlags = [
        info: logInfoEnable != false,
        debug: logDebugEnable == true,
        trace: logTraceEnable == true,
        warn: logWarnEnable != false,
        error: logErrorEnable != false
    ]
}

/**
 * Timer callback for automatically disabling debug logging after 30 minutes.
 */
void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
    cacheLoggingFlags()
}

/**
 * Internal log dispatcher referencing cached logging flags.
 * @param level String log level ("info", "debug", "trace", "warn", "error").
 * @param msg String log message content.
 */
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
// 10. UTILITY FUNCTIONS
// =================================================================================================

/**
 * Safely fetches a device attribute value, trapping missing or disconnected device exceptions.
 * @param device Target Hubitat device instance.
 * @param attributeName String attribute name to query.
 * @return Object attribute value, or null if unreadable.
 */
private Object safeGetDeviceAttribute(Object device, String attributeName) {
    if (!device) return null
    try {
        return device.currentValue(attributeName)
    } catch (Exception e) {
        logWarn "Could not read attribute '${attributeName}' from device '${device}': ${e.message}"
        return null
    }
}

/**
 * Compares and updates persistent app state value only when changed to minimize database writes.
 * @param key State key string.
 * @param value Object new value.
 */
private void sendIfChangedStateValue(String key, Object value) {
    if (!key) return
    String oldVal = state[key]?.toString()
    String newVal = value != null ? value.toString() : ""
    if (oldVal != newVal) {
        state[key] = value
        logDebug "State updated: ${key} -> ${value}"
    }
}

/**
 * Compares and dispatches device attribute event only when value changes to suppress redundant hub events.
 * @param device Target Hubitat device instance.
 * @param attributeName String attribute name.
 * @param value Object new attribute value.
 */
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