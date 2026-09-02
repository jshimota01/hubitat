/**
 * Application Name: Multiperiod Environment Manager
 * Platform: Hubitat Elevation
 * Notes: Schedules 6 heating/cooling setpoint windows with switch overrides, live device tracking, 
 *        dynamic fan circulation, 6-tier EPA AQI Air Quality monitoring with health action strings, 
 *        independent Air Filter control, native automatic heat/cool mode changeover, and dashboard tile output.
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
 *  Schedules 6 heating/cooling setpoint windows with switch overrides, live device tracking, 
 *  dynamic fan circulation, 6-tier EPA AQI filtering with health alerts, native automatic heat/cool 
 *  mode changeover evaluation, and dashboard tile output.
 *
 *  Changelog:
 *	v1.2.1	  08/31/26	  jshimota	  Implemented active deadband hysteresis evaluation in auto-changeover engine, added 10-sec mode change race guard, streamlined fan circulation logic for driver 'ext' preservation, and added GUI schedule overlap/gap validation warnings.
 *	v1.2.0	  08/31/26	  jshimota	  Added native support for CentraLite driver 'ext' fan mode. Hardened fan circulation handoffs and updated operational telemetry snapshots.
 *	v1.1.0	  08/31/26	  jshimota	  Added native automatic heat/cool mode changeover engine to eliminate virtual controller dependencies. Subscribed to ambient temperature updates with deadband guards.
 *	v1.0.2	  08/30/26	  Gemini	  Reverted RoundingMode.HALF_UP back to BigDecimal.ROUND_HALF_UP in safeAqiValue
 *	v1.0.1	  08/30/26	  Gemini	  Hardened fan ownership retention during active HVAC OFF phases and strictly enforced RGB light ownership checking
 *	v1.0.0	  08/30/26	  jshimota	  Applied standardized app template v1.0.0 with settings snapshotting, dynamic labeling, and demarcation logging
 *	v0.11.0	  08/20/26	  Gemini	  Restructured mainPage GUI layout to match Mode Manager Advanced styling standards
 *	v0.10.0	  08/20/26	  Gemini	  Removed thermostat setpoint subscriptions to allow temporary external manual setpoint overrides until next period boundary reset
 *	v0.0.1	  06/01/26	  jshimota	  Initial release
 **/

static String version() { return '1.2.1' }
def timeStamp() { return "2026/08/31 09:20 PM" }

definition(
    name: "Multiperiod Environment Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Schedules 6 heating/cooling setpoint windows with switch overrides, live device tracking, dynamic fan circulation, 6-tier EPA AQI filtering with health alerts, native heat/cool auto-changeover, and dashboard tile output.",
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
// USER INTERFACE (STRICTLY READ-ONLY / NO AUTOMATION SIDE-EFFECTS)
// =================================================================================================

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        
        String currentVersion = version()
        Boolean isCollapsed = (state.sectionsExpanded == true) ? false : true
        state.sectionsExpanded = false

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

        /* App Title Banner & Active Status Card */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Multiperiod Environment Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
            
            String activePeriodStr = scheduleData.activePeriod ?: "Not Evaluated"
            String appStateBadge = appEnabled ? "<span style='color:#27AE60; font-weight:bold;'>ENABLED</span>" : "<span style='color:#C0392B; font-weight:bold;'>DISABLED</span>"
            
            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #27AE60; padding:10px; border-radius:4px; font-size:13px;'>" +
                      "<b>App Automation:</b> ${appStateBadge} &nbsp;|&nbsp; " +
                      "<b>Active Period:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodStr}</span> &nbsp;|&nbsp; " +
                      "<b>Live Setpoints:</b> Heat: <b>${devHeat}°</b> | Cool: <b>${devCool}°</b></div>"
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY A: HVAC & SCHEDULE CONTROL
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:10px;'>CATEGORY A: HVAC & SCHEDULE CONTROL (Thermostat, Overrides, Timers)</div>") {}

        /* Section 1: Target Thermostat Device */
        section("<b>SECTION 1: Control & Target Thermostat Device</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Select the primary physical thermostat managed directly by MEM.</div>"

            input name: "appEnabled", type: "bool", title: "<b>Enable Automated Setpoint Scheduling</b>", defaultValue: true, submitOnChange: true
            input name: "targetThermostat", type: "capability.thermostat", title: "<b>Select Target Thermostat Device</b>", required: true, multiple: false, submitOnChange: true
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            
            input name: "autoChangeoverEnabled", type: "bool", title: "<b>Enable Automated Heat/Cool System Mode Changeover</b>", defaultValue: true, submitOnChange: true
            if (autoChangeoverEnabled) {
                input name: "tempDeadband", type: "number", title: "Setpoint Deadband Buffer (°F)", description: "Degree offset required beyond setpoint before triggering mode change", defaultValue: 2, required: true, width: 6
            }
        }

        /* Section 2: Switch-Based Overrides */
        section("<b>SECTION 2: Switch-Based Overrides & Behavioral Setpoints</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "<b>Switch Overrides:</b> When these switches are <b>ON</b>, these specific setpoints preempt time-based schedules.</div>"

            input name: "awaySwitch", type: "capability.switch", title: "<b>Select 'Away' Switch</b>", required: false, multiple: false, submitOnChange: true
            input name: "awayHeat", type: "number", title: "Away Heat Setpoint", required: true, defaultValue: 62, width: 6
            input name: "awayCool", type: "number", title: "Away Cool Setpoint", required: true, defaultValue: 78, width: 6
            
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "sleepSwitch", type: "capability.switch", title: "<b>Select 'Sleeping' Switch</b>", required: false, multiple: false, submitOnChange: true
            input name: "sleepHeat", type: "number", title: "Sleeping Heat Setpoint", required: true, defaultValue: 65, width: 6
            input name: "sleepCool", type: "number", title: "Sleeping Cool Setpoint", required: true, defaultValue: 74, width: 6
        }

        /* Section 3: Time-Based Schedules */
        section("<b>SECTION 3: Time-Based Setpoint Schedule</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure start times, end times, and target setpoints for daily windows. <i>(Earlier defined windows take priority during overlaps).</i></div>"

            Map val = validateConfiguration()
            if (val.warnings && !val.warnings.isEmpty()) {
                StringBuilder sb = new StringBuilder()
                sb.append("<div style='color:#8a6d3b; background-color:#fcf8e3; padding:8px; border:1px solid #faebcc; border-radius:4px; margin-bottom:10px;'><b>SCHEDULE VALIDATION NOTICE:</b><ul>")
                val.warnings.each { w -> sb.append("<li>").append(w).append("</li>") }
                sb.append("</ul></div>")
                paragraph sb.toString()
            }

            // Morning
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Morning Schedule Period</span>"
            input name: "morningStart", type: "time", title: "Morning Start Time", required: true, defaultValue: "06:00", width: 3
            input name: "morningEnd", type: "time", title: "Morning End Time", required: true, defaultValue: "09:00", width: 3
            input name: "morningHeat", type: "number", title: "Morning Heat Setpoint", required: true, defaultValue: 68, width: 3
            input name: "morningCool", type: "number", title: "Morning Cool Setpoint", required: true, defaultValue: 72, width: 3

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            // Day
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Day Schedule Period</span>"
            input name: "dayStart", type: "time", title: "Day Start Time", required: true, defaultValue: "09:00", width: 3
            input name: "dayEnd", type: "time", title: "Day End Time", required: true, defaultValue: "17:00", width: 3
            input name: "dayHeat", type: "number", title: "Day Heat Setpoint", required: true, defaultValue: 66, width: 3
            input name: "dayCool", type: "number", title: "Day Cool Setpoint", required: true, defaultValue: 75, width: 3

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            // Evening
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Evening Schedule Period</span>"
            input name: "eveningStart", type: "time", title: "Evening Start Time", required: true, defaultValue: "17:00", width: 3
            input name: "eveningEnd", type: "time", title: "Evening End Time", required: true, defaultValue: "22:00", width: 3
            input name: "eveningHeat", type: "number", title: "Evening Heat Setpoint", required: true, defaultValue: 69, width: 3
            input name: "eveningCool", type: "number", title: "Evening Cool Setpoint", required: true, defaultValue: 73, width: 3

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            // Night
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>Night Schedule Period</span>"
            input name: "nightStart", type: "time", title: "Night Start Time", required: true, defaultValue: "22:00", width: 3
            input name: "nightEnd", type: "time", title: "Night End Time", required: true, defaultValue: "06:00", width: 3
            input name: "nightHeat", type: "number", title: "Night Heat Setpoint", required: true, defaultValue: 64, width: 3
            input name: "nightCool", type: "number", title: "Night Cool Setpoint", required: true, defaultValue: 75, width: 3
        }

        /* Section 4: Fan Circulation Control */
        section("<b>SECTION 4: Simulated Fan Circulation Control</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Cycles the thermostat fan between <b>ON</b> and <b>AUTO</b> to simulate dynamic air circulation throughout the home. <i>(Set target thermostat Fan Mode to <b>Ext</b>)</i>.</div>"

            input name: "fanCirculateEnabled", type: "bool", title: "<b>Enable Fan Circulation Loop</b>", defaultValue: false, submitOnChange: true
            if (fanCirculateEnabled) {
                input name: "fanOnMinutes", type: "number", title: "Fan ON Duration (1-240 Minutes)", required: true, defaultValue: 30, width: 6
                input name: "fanOffMinutes", type: "number", title: "Fan OFF/Auto Duration (1-240 Minutes)", required: true, defaultValue: 30, width: 6
            }
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY B: ENVIRONMENTAL QUALITY & AIR FILTRATION
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY B: ENVIRONMENTAL QUALITY & AIR FILTRATION</div>") {}

        /* Section 5: Air Quality Monitoring */
        section("<b>SECTION 5: Air Quality Monitoring & RGB Indicator Light</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Monitors indoor AQI sensors and calculates real-time averages. Controls an optional RGB indicator light using standard 6-tier EPA colors during Home/Awake modes.</div>"

            Boolean isAqiValid = validateAqiThresholds()
            if (!isAqiValid && aqiEnabled) {
                paragraph "<div style='color:red; background-color:#ffe6e6; padding:8px; border:1px solid red; border-radius:4px;'><b>CONFIGURATION ERROR:</b> AQI Thresholds must be in strictly ascending order. Defaults applied until corrected.</div>"
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

            input name: "aqiEnabled", type: "bool", title: "<b>Enable Air Quality Monitoring</b>", defaultValue: false, submitOnChange: true
            if (aqiEnabled) {
                input name: "aqiSensor1", type: "capability.airQuality", title: "Select Primary AQI Sensor", required: true, multiple: false, submitOnChange: true
                input name: "aqiSensor2", type: "capability.airQuality", title: "Select Secondary AQI Sensor (Optional)", required: false, multiple: false, submitOnChange: true
                input name: "rgbLight", type: "capability.colorControl", title: "Select Indicator RGB Light Device", required: false, multiple: false
                
                paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>" +
                          "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>AQI Tier Upper Boundary Overrides</span>"
                
                input name: "aqiGoodMax", type: "number", title: "Good AQI Max (Default: 50)", required: true, defaultValue: 50, submitOnChange: true
                input name: "aqiModMax", type: "number", title: "Moderate AQI Max (Default: 100)", required: true, defaultValue: 100, submitOnChange: true
                input name: "aqiSensMax", type: "number", title: "Sensitive Groups Max (Default: 150)", required: true, defaultValue: 150, submitOnChange: true
                input name: "aqiUnhealthyMax", type: "number", title: "Unhealthy Max (Default: 200)", required: true, defaultValue: 200, submitOnChange: true
                input name: "aqiVeryUnhealthyMax", type: "number", title: "Very Unhealthy Max (Default: 300)", required: true, defaultValue: 300, submitOnChange: true
            }
        }

        /* Section 6: Room Air Filter Control */
        section("<b>SECTION 6: Room Air Filter Automation</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Automatically toggles the room air filter switch on/off based on real-time AQI tier threshold calculations.</div>"

            input name: "airFilterEnabled", type: "bool", title: "<b>Enable Automated Room Air Filter Control</b>", defaultValue: true, submitOnChange: true
            if (airFilterEnabled) {
                input name: "airFilterSwitch", type: "capability.switch", title: "Select Room Air Filter Switch/Device", required: true, multiple: false, submitOnChange: true
            }
        }

        /* ---------------------------------------------------------------------------------
           CATEGORY C: SYSTEM STATUS & DIAGNOSTICS
           --------------------------------------------------------------------------------- */
        section("<div style='background-color:#2C3E50; color:#FFF; padding:6px 10px; font-weight:bold; border-radius:4px; margin-top:15px;'>CATEGORY C: SYSTEM STATUS & DIAGNOSTICS</div>") {}

        /* Section 7: Live Status Summary */
        section("<b>SECTION 7: System Operational Status Summary</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #2980B9; padding:10px; border-radius:4px; font-size:12px;'>" +
                      "<b>Connected Thermostat Device:</b> ${targetThermostat ? targetThermostat.displayName : 'None Selected'}<br/>" +
                      "<b>Thermostat Telemetry:</b> Temp: ${tStates.temp ?: '--'}° | Heat: ${devHeat}° | Cool: ${devCool}° | Mode: ${devThermostatMode} | Fan Mode: ${devFanMode} | Operating State: ${devOpState}<br/>" +
                      "<hr style='border:0; border-top:1px solid #E0E0E0; margin:6px 0;'/>" +
                      "<b>Active Schedule Window:</b> ${scheduleData.activePeriod ?: 'Not Evaluated'}<br/>" +
                      "<b>Target Schedule Setpoints:</b> Heat: ${scheduleData.targetHeat ?: '--'}° | Cool: ${scheduleData.targetCool ?: '--'}°<br/>" +
                      "<b>Auto-Changeover Mode Engine:</b> ${autoChangeoverEnabled ? 'Active (Deadband Buffer: ' + (tempDeadband ?: 2) + '°)' : 'Disabled'}<br/>" +
                      "<b>Fan Circulation Loop:</b> ${fanCirculateEnabled ? (state.fanPhase ?: 'Initializing') : 'Disabled'}<br/>" +
                      "<hr style='border:0; border-top:1px solid #E0E0E0; margin:6px 0;'/>" +
                      (aqiEnabled ? "<b>AQI Sensor 1 (${aqiSensor1?.displayName ?: 'None'}):</b> ${v1 ?: '--'}<br/>" +
                                    "<b>AQI Sensor 2 (${aqiSensor2?.displayName ?: 'None'}):</b> ${v2 ?: '--'}<br/>" +
                                    "<b>Calculated Average AQI:</b> ${avgAqi ?: '--'} (${displayAqiStatus})<br/>" +
                                    "<b>Health Advisory:</b> ${aqiStatusInfo.actionText ?: 'N/A'}<br/>" : "<b>Air Quality Monitoring:</b> Disabled<br/>") +
                      "<hr style='border:0; border-top:1px solid #E0E0E0; margin:6px 0;'/>" +
                      (airFilterEnabled ? "<b>Air Filter Status:</b> ${safeGetDeviceAttribute(airFilterSwitch, 'switch') ?: 'No Switch / Unavailable'}<br/>" : "<b>Air Filter Automation:</b> Disabled<br/>") +
                      "<b>Last Evaluation Time:</b> ${state.lastEvaluated ?: 'Never'}</div>"
        }

        /* Section 8: Logging Options */
        section("<b>SECTION 8: App Preferences & Logging Options</b>", hideable: true, hidden: isCollapsed) {
            paragraph "<div style='color:#555; font-size:12px; margin-bottom:8px;'>" +
                      "Configure system logging outputs and app preferences. <i>(Debug logging auto-disables after 30 minutes)</i>.</div>"

            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true, submitOnChange: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }

        /* App Name & Footer */
        section() {
            paragraph "<span style='color:#2C3E50; font-weight:bold; font-size:13px;'>App Label Customization</span>"
            label title: "Assign a custom label for this SmartApp instance", required: false
        }

        state.sectionsExpanded = true
    }
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Multiperiod Environment Manager"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

// Settings Hash Snapshot Helper
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

    if (targetThermostat) {
        subscribe(targetThermostat, "temperature", "temperatureHandler")
    }

    if (awaySwitch) {
        subscribe(awaySwitch, "switch", "overrideSwitchHandler")
    }
    if (sleepSwitch) {
        subscribe(sleepSwitch, "switch", "overrideSwitchHandler")
    }
    
    if (airFilterEnabled && airFilterSwitch) {
        subscribe(airFilterSwitch, "switch", "airFilterSwitchHandler")
    }
    
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
    if (!appEnabled) {
        logInfo "MEM Pass skipped: Application is disabled."
        releaseDeviceOwnership()
        updateTile()
        return
    }

    logDebug "Starting Unified Evaluation Pass. Trigger Cause: '${cause}', ForceApply: ${forceSetpointApply}"

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

    sendIfChangedStateValue("activeSet", sched.activePeriod)
    sendIfChangedStateValue("lastEvaluated", new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))

    if (sched.targetHeat != null && sched.targetCool != null && targetThermostat) {
        applySetpointsAndEvaluateMode(sched.targetHeat as BigDecimal, sched.targetCool as BigDecimal, thermostat, forceSetpointApply)
    }

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
            if (state.rgbOwned == true) {
                logInfo "Turning off AQI RGB Light (${rgbLight.displayName}) - Mode/AQI condition clear (MEM owned)."
                try {
                    rgbLight.off()
                } catch (Exception e) {
                    logError "Failed to turn off RGB light: ${e.message}"
                }
                sendIfChangedStateValue("rgbOwned", false)
            }
        }
    } else if (!aqiEnabled && state.rgbOwned == true) {
        releaseDeviceOwnership()
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
        thermostat: thermostat,
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
    if (startMin <= endMin) {
        return (currentMin >= startMin && currentMin < endMin)
    } else {
        return (currentMin >= startMin || currentMin < endMin)
    }
}

private Map getCalculatedScheduleData() {
    if (!appEnabled) {
        return [activePeriod: "Disabled (App OFF)", targetHeat: null, targetCool: null]
    }

    Calendar nowCal = Calendar.getInstance(location.timeZone)
    Integer currentMin = (nowCal.get(Calendar.HOUR_OF_DAY) * 60) + nowCal.get(Calendar.MINUTE)

    String activePeriod = ""
    BigDecimal targetHeat = null
    BigDecimal targetCool = null

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

def modeChangeHandler(evt) { executeEvaluationPass("Hub Mode Changed (${evt?.value})", false) }
def overrideSwitchHandler(evt) { executeEvaluationPass("Override Switch Changed (${evt?.device?.displayName} = ${evt?.value})", true) }
def aqiHandler(evt) { executeEvaluationPass("AQI Sensor Updated (${evt?.device?.displayName} = ${evt?.value})", false) }
def airFilterSwitchHandler(evt) { executeEvaluationPass("Air Filter Switch State Changed (${evt?.value})", false) }
def temperatureHandler(evt) { executeEvaluationPass("Thermostat Ambient Temperature Updated (${evt?.value}°)", false) }
def evaluateSchedule() { executeEvaluationPass("Scheduled Window Boundary Trigger", true) }
def evaluateAirQuality() { executeEvaluationPass("Direct AQI Evaluation Call", false) }

// =================================================================================================
// 4. THERMOSTAT CONTROL & AUTO-CHANGEOVER ENGINE
// =================================================================================================

private Map getThermostatStateSnapshot() {
    if (!targetThermostat) return [:]
    return [
        temp: safeGetDeviceAttribute(targetThermostat, "temperature"),
        heat: safeGetDeviceAttribute(targetThermostat, "heatingSetpoint"),
        cool: safeGetDeviceAttribute(targetThermostat, "coolingSetpoint"),
        mode: safeGetDeviceAttribute(targetThermostat, "thermostatMode"),
        fan: safeGetDeviceAttribute(targetThermostat, "thermostatFanMode"),
        operatingState: safeGetDeviceAttribute(targetThermostat, "thermostatOperatingState")
    ]
}

private void applySetpointsAndEvaluateMode(BigDecimal targetHeat, BigDecimal targetCool, Map thermostat = null, Boolean forceSetpointApply = false) {
    if (!targetThermostat || targetHeat == null || targetCool == null) return
    
    Map snapshot = thermostat ?: getThermostatStateSnapshot()
    BigDecimal currentHeatSp = safeToDecimal(snapshot.heat)
    BigDecimal currentCoolSp = safeToDecimal(snapshot.cool)
    BigDecimal currentTemp = safeToDecimal(snapshot.temp)
    String currentMode = snapshot.mode?.toLowerCase() ?: "off"

    try {
        if (forceSetpointApply || currentHeatSp != targetHeat) {
            logInfo "Setting ${targetThermostat.displayName} Heating Setpoint to ${targetHeat}° (Active Period: ${state.activeSet})"
            targetThermostat.setHeatingSetpoint(targetHeat)
        }

        if (forceSetpointApply || currentCoolSp != targetCool) {
            logInfo "Setting ${targetThermostat.displayName} Cooling Setpoint to ${targetCool}° (Active Period: ${state.activeSet})"
            targetThermostat.setCoolingSetpoint(targetCool)
        }
    } catch (Exception e) {
        logError "Failed to apply setpoints to ${targetThermostat.displayName}: ${e.message}"
    }

    // 2. Evaluate Automatic System Mode Changeover with Active Deadband Hysteresis Guard
    if (getSettingBool("autoChangeoverEnabled", true) && currentTemp != null) {
        BigDecimal deadband = safeToDecimal(tempDeadband, 2.0)
        
        // 10-Second Pending Guard to prevent async mode change races
        if (state.modeChangePending == true) {
            logDebug "Auto-Changeover: Mode change execution pending. Skipping redundant pass."
            return
        }

        try {
            if (currentTemp <= targetHeat && currentMode != "heat") {
                logInfo "Auto-Changeover: Ambient temp (${currentTemp}°) <= Heat Setpoint (${targetHeat}°). Setting mode to HEAT."
                state.modeChangePending = true
                runIn(10, "clearModeChangeGuard")
                targetThermostat.heat()
            } else if (currentTemp >= targetCool && currentMode != "cool") {
                logInfo "Auto-Changeover: Ambient temp (${currentTemp}°) >= Cool Setpoint (${targetCool}°). Setting mode to COOL."
                state.modeChangePending = true
                runIn(10, "clearModeChangeGuard")
                targetThermostat.cool()
            } else if (currentMode == "heat" && currentTemp >= (targetHeat + deadband) && currentTemp < targetCool) {
                logDebug "Auto-Changeover: Temp (${currentTemp}°) cleared Heat target + Deadband buffer (${targetHeat + deadband}°). Hysteresis holding current mode."
            } else if (currentMode == "cool" && currentTemp <= (targetCool - deadband) && currentTemp > targetHeat) {
                logDebug "Auto-Changeover: Temp (${currentTemp}°) cleared Cool target - Deadband buffer (${targetCool - deadband}°). Hysteresis holding current mode."
            }
        } catch (Exception e) {
            logError "Failed to execute auto-changeover mode evaluation on ${targetThermostat.displayName}: ${e.message}"
            state.modeChangePending = false
        }
    }
}

void clearModeChangeGuard() {
    state.modeChangePending = false
}

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
            logInfo "Fan Circulation Loop: Turning fan ON for ${safeOn} minutes."
            targetThermostat.fanOn()
            sendIfChangedStateValue("fanOwned", true)
            runIn(onTime, "toggleFanCirculation", [data: [nextPhase: "OFF"]])
        } else {
            sendIfChangedStateValue("fanPhase", "OFF Phase")
            
            String opState = thermostat.operatingState
            if (opState != "heating" && opState != "cooling") {
                logInfo "Fan Circulation Loop: Setting fan to AUTO for ${safeOff} minutes."
                targetThermostat.fanAuto()
            } else {
                logDebug "Fan Circulation Loop: HVAC actively ${opState}. Issuing AUTO handoff command to driver."
                targetThermostat.fanAuto()
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
    String activePeriod = state.activeSet ?: (sched.activePeriod ?: "N/A")
    
    def appHeat = sched.targetHeat != null ? "${sched.targetHeat}°" : "--"
    def appCool = sched.targetCool != null ? "${sched.targetCool}°" : "--"

    Map thermostat = ctx.thermostat ?: [:]
    def realHeat = thermostat.heat != null ? "${thermostat.heat}°" : "--"
    def realCool = thermostat.cool != null ? "${thermostat.cool}°" : "--"
    def realTemp = thermostat.temp != null ? "${thermostat.temp}°" : "--"
    
    String avgAqiStr = "N/A"
    String aqiStatusStr = "Disabled"
    String aqiColorCss = "#888888"
    
    Integer avgAqi = ctx.aqi as Integer
    if (avgAqi != null) {
        avgAqiStr = "${avgAqi}"
        Map statusInfo = determineStatus(avgAqi)
        aqiStatusStr = statusInfo.status ?: "N/A"
        
        if (aqiStatusStr.contains("Good")) {
            aqiColorCss = "#008000"
        } else if (aqiStatusStr.contains("Moderate")) {
            aqiColorCss = "#b8860b"
        } else if (aqiStatusStr.contains("Sensitive")) {
            aqiColorCss = "#ff8c00"
        } else if (aqiStatusStr.contains("Unhealthy for")) {
            aqiColorCss = "#ff0000"
        } else if (aqiStatusStr.contains("Very Unhealthy")) {
            aqiColorCss = "#800080"
        } else if (aqiStatusStr.contains("Hazardous")) {
            aqiColorCss = "#800000"
        }
    }
    
    String filterStatus = ctx.filterState ?: "N/A"
    String circStatus = fanCirculateEnabled ? (state.fanPhase ?: "Initializing") : "Disabled"

    StringBuilder tileSb = new StringBuilder()
    tileSb.append("<div style='font-size:.42em; line-height:1.2; text-align:center; padding:2px;'>")
    tileSb.append("<b>MEM:</b> ").append(appStatus).append(" | <b>Room:</b> ").append(realTemp).append("<br/>")
    tileSb.append("<b>Period:</b> ").append(activePeriod).append("<br/>")
    tileSb.append("<b>Device Setpoints:</b> ").append(realHeat).append(" / ").append(realCool).append("<br/>")
    tileSb.append("<b>Target Setpoints:</b> ").append(appHeat).append(" / ").append(appCool).append("<br/>")
    tileSb.append("<div style='font-size:.3em;'><b>AQI:</b> <span style='color:").append(aqiColorCss)
          .append("; font-weight:bold;'>").append(avgAqiStr).append(" (").append(aqiStatusStr).append(")</span></div>")
    tileSb.append("<b>Filter:</b> ").append(filterStatus).append("<br/>")
    tileSb.append("<b>Circulation:</b> ").append(circStatus)
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

    if (!targetThermostat) {
        errors.add("Target Thermostat device is required.")
    }

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

    // Schedule Overlap/Gap Detection
    Integer mStart = timeToMinutes(morningStart)
    Integer mEnd   = timeToMinutes(morningEnd)
    Integer dStart = timeToMinutes(dayStart)
    Integer dEnd   = timeToMinutes(dayEnd)
    Integer eStart = timeToMinutes(eveningStart)
    Integer eEnd   = timeToMinutes(eveningEnd)
    Integer nStart = timeToMinutes(nightStart)
    Integer nEnd   = timeToMinutes(nightEnd)

    if (mStart != null && mEnd != null && dStart != null && isTimeInWindow(mStart, mEnd, dStart)) {
        warnings.add("Morning window (${morningStart}-${morningEnd}) overlaps with Day Start (${dayStart}). Morning takes evaluation priority.")
    }
    if (dStart != null && dEnd != null && eStart != null && isTimeInWindow(dStart, dEnd, eStart)) {
        warnings.add("Day window (${dayStart}-${dayEnd}) overlaps with Evening Start (${eveningStart}). Day takes evaluation priority.")
    }
    if (eStart != null && eEnd != null && nStart != null && isTimeInWindow(eStart, eEnd, nStart)) {
        warnings.add("Evening window (${eveningStart}-${eveningEnd}) overlaps with Night Start (${nightStart}). Evening takes evaluation priority.")
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