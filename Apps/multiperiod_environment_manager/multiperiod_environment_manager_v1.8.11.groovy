/**
 * Application Name: Multiperiod Environment Manager
 * Platform: Hubitat Elevation
 *
 * Notes: Schedules heating/cooling setpoint windows with switch overrides, dual House & Office HVAC 
 *        controller management, fire safety forced-off modes, automatic fan circulation, 6-tier EPA AQI 
 *        Air Quality monitoring with health action strings, independent Air Filter control, native automatic 
 *        heat/cool mode changeover, native persisted setpoint tables, Smart Table boundary time pickers, and dashboard tile output.
 *
 * HPM Category: Convenience, HVAC, Thermostat, Fan
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
 *  v1.8.11   09/25/26    jshimota    Architectural Refactor: Implemented requestEvaluation() debounced dispatcher engine (runIn 1s with overwrite) to coalesce concurrent multi-threaded event triggers (Mode, Override, Thermostat, AQI). Prevents execution race conditions, duplicate period transition logs, and thermostat command echo loops.
 *  v1.8.10   09/24/26    jshimota    Standardized version update trace log line format across installed and updated SmartApp lifecycle hooks.
 *  v1.8.9    09/24/26    jshimota    Fixed StackOverflowError caused by recursive executeEvaluationPass calls inside applySetpointsAndEvaluateMode when clearing Office manual heat override. Clearing state.dev2ManualHeat now updates internal state cleanly without triggering synchronous execution re-entry loops.
 *  v1.8.8    09/24/26    jshimota    Refactored Office manual heat override release mechanism. When an Office manual heat request is satisfied, MEM now clears the manual override state and restores the target schedule setpoint without executing dev.auto() mode calls, letting AVT's internal hysteresis engine manage heater relays while preserving locked AUTO presentation.
 *  v1.8.7    09/24/26    jshimota    Pre-armed setpoint in-flight guard state in applySetpointsAndEvaluateMode() prior to dispatching setHeatingSetpoint/setCoolingSetpoint commands, eliminating synchronous event-driven race conditions. Hardened setpointChangeHandler() to absorb internal MEM target updates accurately before checking manual dashboard overrides.
 *  v1.8.6    09/14/26    jshimota    Fixed Office manual heat setpoint shutdown. When an active Office dashboard heat override is satisfied while the Office thermostat is heating, MEM returns the Office thermostat to AUTO so its existing idle evaluation shuts the floor heater off. House HVAC control is unchanged.
 *  v1.8.5    09/14/26    jshimota    Added dashboard setpoint override handling for House and Office thermostats. Manual dashboard heat/cool changes now become temporary overrides until the next schedule/override transition. Internal MEM setpoint commands are ignored by the override handler. Manual setpoint activation bypasses MEM's auto-changeover deadband so 1–2°F dashboard changes are honored immediately.
 *  v1.8.4    09/13/26    jshimota    Demoted Office HVAC Safety Trigger logs from WARN to INFO to reflect standard operating state. Deduplicated repetitive evaluation log loops by guarding office safety trigger entries against state.officeSafetyActive. Added period transition info logging to announce schedule period and active setpoint changes.
 *  v1.8.3    09/11/26    jshimota    Added 1-hour activity cooldown guard to Sleeping Mode fan circulation. Postpones circulation ON phase until 60 minutes have elapsed since the last heat, cool, or fan run. Added thermostatOperatingState subscription tracking.
 *  v1.8.2    09/07/26    jshimota    Refactored buildTileHtml EM font scaling. Set standard lines to 0.75em base font size and House/Office thermostat lines to 0.825em (10% larger). Removed root px overrides to allow native dashboard tile auto-fit without scrollbars.
 *  v1.8.1    09/07/26    jshimota    Refactored buildTileHtml font scaling and layout. Set base EM container size to 12px. House and Office temperatures now render on 2 distinct lines at 1.0em (12px) without vertical bar separators. Reduced MEM Period header, Device Modes, merged Air Filter/Circulation line, and AQI line to 0.833em (~10px). Tightened line-height to 1.15 to eliminate dashboard tile scrollbars.
 *  v1.8.0    09/07/26    jshimota    Refactored Dashboard Tile engine to use EM units for dynamic responsive scaling. Standardized House and Office top header lines to show current temperatures side-by-side with 15% larger font size (1.15em). Added lines for Device Mode, Room Air Filter state, and Circulation phase. Added dedicated Dashboard Tile Reference UI card and diagnostic panel.
 *  v1.7.9    09/04/26    jshimota    Added per-device short-lived in-flight setpoint command pending guards (dev1SetpointPending, dev2SetpointPending) with target memory to prevent duplicate command transmission loops during radio propagation windows. Retained exact degree comparisons and forceSetpointApply behavior.
 *  v1.7.8    09/04/26    jshimota    Added explicit warn log entries when Office HVAC safety trigger activates and when it clears upon returning home or exiting sleeping mode. Added state tracking for office safety trigger persistence.
 *  v1.7.7    09/03/26    jshimota    Refactored AQI status string formatting on dashboard tile to remove text-based color names and wrapping parentheses while retaining colorized Level of Concern text. Preserved GUI table labels.
 *  v1.7.6    09/03/26    jshimota    Resolved locked-Auto driver mode mismatch by prioritizing physicalThermostatMode over thermostatMode in getThermostatStateSnapshot(). Restored forced-OFF deduplication and auto-changeover evaluation against true hardware/virtual state.
 *  v1.7.5    09/03/26    jshimota    Prevented redundant Office HVAC forced-off commands and log noise by checking thermostatMode first and emitting warn logs only on actual shutoff. Prevented duplicate setpoint calls by passing forceSetpointApply = false on runtime events. Fixed abrupt fan ON trigger when sleeping switch toggles by recalculating timers without resetting to ON phase.
 *  v1.7.4    09/02/26    jshimota    Replaced block-level title wrapper <div> elements with inline <span> wrappers (white-space:nowrap;) across Section 1 and Section 2 schedule grids to prevent Hubitat's required field indicator (*) from wrapping onto a new line.
 *  v1.7.3    09/02/26    jshimota    Extended localized title wrapper height (height:42px) across Section 1 and Section 2 schedule grid columns 1, 2, and 3 to visually project vertical divider lines alongside native input controls.
 *  v1.7.2    09/02/26    jshimota    Added localized border-right vertical divider wrappers to Section 1 and Section 2 schedule grid titles (columns 1, 2, and 3) without modifying any application logic or native input attributes.
 *  v1.7.1    09/02/26    jshimota    Replaced raw HTML time selectors in Section 3 with native Hubitat time inputs for full settings persistence reliability. Added a polished GUI summary table for Section 3 boundaries. Synchronized fan circulation log outputs with state.fanOwned status. Updated chronological ordering warning string to reflect deterministic time sorting.
 *  v1.7.0    09/02/26    jshimota    Fixed Sleeping switch fan circulation delay by triggering immediate fan timer recalculation. Removed dead Office Presence/Sleeping override UI setpoint inputs to eliminate UI misdirection while maintaining hardcoded Office OFF safety protocol. Added presence sensor selection validation warning. Guarded fanAuto() calls against state.fanOwned flag. Secured getBoundaryMinutes() against NumberFormatException with safeToInt(). Added chronological period boundary order validation warning.
 *  v1.6.9    09/02/26    jshimota    Removed redundant 'Sleeping' prefix from Section 6 input labels. Restored inline vertical border separation across side-by-side setpoint and fan duration inputs.
 *  v1.6.8    09/02/26    jshimota    Standardized terminology in Section 4 (Control prefix and Device labels). Enforced hardcoded non-configurable safety isolation in Section 5 (Office HVAC forced off during Presence/Sleeping overrides). Updated Section 6 headers to 'Circulation Control' and 'Circulation Period'.
 *  v1.6.7    09/02/26    jshimota    Standardized UI labels across Sections 1, 2, and 4 to consistently reference 'House HVAC Controller/Thermostat Device' and 'Office HVAC Controller/Thermostat Device'.
 *  v1.6.6    09/02/26    jshimota    Upgraded Presence override engine from capability.switch to capability.presenceSensor (OwnTracks). Trigger active when presence attribute == 'not present'.
 *  v1.6.5    09/02/26    jshimota    Purged redundant 'Heating' and 'Cooling' text from native input titles across schedule grids and override setpoints, relying purely on column header banners.
 *  v1.6.4    09/02/26    jshimota    Restored 4-column (width: 3) 2-row layout across House and Office schedule setpoint grids. Applied localized vertical divider borders directly to headers without global CSS injections.
 *  v1.6.3    09/02/26    jshimota    Removed 'Target' from Presence/Sleeping setpoint titles. Added standardized header banners with '(Heat ON if <)' and '(A/C ON if >)' subtitles. Brightened heating label color to #E74C3C.
 *  v1.6.2    09/02/26    jshimota    Refactored Section 1 (House HVAC) setpoints and Section 4 (Presence/Sleeping) inputs to match the explicit width-6 grid pairs of Section 2. Removed global CSS to avoid renderer exceptions. Fixed AQI Very Unhealthy boundary range string formatting.
 *  v1.6.1    09/02/26    jshimota    Fixed NullPointerException on line 105 in mainPage by replacing dynamic CSS string concatenation with a static HTML block.
 *  v1.6.0    09/02/26    jshimota    Fixed AQI Very Unhealthy range display bug in Section 7 HTML table (resolved 201-Null string formatting issue to properly display 201 to one less than Hazardous).
 **/

static String version() { return '1.8.11' }
def timeStamp() { return "2026/09/25 08:00 AM" }

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
            
            paragraph "<div style='background-color:#F8F9FA; border-left:4px solid #27AE60; padding:10px; border-radius:4px; font-size:13px; margin-bottom:10px;'>" +
                      "<b>Units:</b> ${houseBadge} | ${officeBadge} &nbsp;|&nbsp; " +
                      "<b>Active Period:</b> <span style='color:#2980B9; font-weight:bold;'>${activePeriodStr}</span><br/>" +
                      "<b>House HVAC Live:</b> Heat: <b>${dev1Heat} °F</b> | Cool: <b>${dev1Cool} °F</b> (${dev1Mode}) &nbsp;|&nbsp; " +
                      "<b>Office HVAC Live:</b> Heat: <b>${dev2Heat} °F</b> | Cool: <b>${dev2Cool} °F</b> (${dev2Mode})</div>"

            def childTile = getChildDevice("MEM_TILE_${app.id}")
            String tileDeviceName = childTile ? childTile.displayName : "MEM Tile Device"
            String tileDni = "MEM_TILE_${app.id}"

            paragraph "<div style='background-color:#EBF5FB; border-left:4px solid #2980B9; padding:10px; border-radius:4px; font-size:12px; margin-bottom:12px; color:#1B4F72;'>" +
                      "<b>DASHBOARD TILE INTEGRATION:</b><br/>" +
                      "• <b>Child Device Name:</b> <code>${tileDeviceName}</code><br/>" +
                      "• <b>Device Network ID (DNI):</b> <code>${tileDni}</code><br/>" +
                      "• <b>Dashboard Tile Attribute:</b> <code>memTile</code><br/>" +
                      "<span style='font-size:11px; opacity:0.9;'><i>To display this tile, add <b>${tileDeviceName}</b> to your Hubitat Dashboard, select template <b>Attribute</b>, and set the attribute to <code>memTile</code>.</i></span></div>"
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

            paragraph "<table style='width:100%; border-collapse:collapse; margin-bottom:4px; font-size:12px; font-weight:bold; text-align:center; background-color:#2C3E50; color:#FFFFFF; border-radius:4px;'>" +
                      "<tr>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:DodgerBlue;'>COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:25%; padding:6px;'><span style='color:DodgerBlue;'>COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "</tr></table>"

            input name: "d1MorningHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Morning</b></span>", defaultValue: 68, required: true, width: 3, submitOnChange: false
            input name: "d1MorningCool", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Morning</b></span>", defaultValue: 72, required: true, width: 3, submitOnChange: false
            input name: "d1DayHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Day</b></span>", defaultValue: 66, required: true, width: 3, submitOnChange: false
            input name: "d1DayCool", type: "number", title: "<b>Day</b>", defaultValue: 75, required: true, width: 3, submitOnChange: false
            
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

            paragraph "<table style='width:100%; border-collapse:collapse; margin-bottom:4px; font-size:12px; font-weight:bold; text-align:center; background-color:#2C3E50; color:#FFFFFF; border-radius:4px;'>" +
                      "<tr>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:DodgerBlue;'>COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "<td style='width:25%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:25%; padding:6px;'><span style='color:DodgerBlue;'>COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "</tr></table>"

            input name: "d2MorningHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Morning</b></span>", defaultValue: 68, required: true, width: 3, submitOnChange: false
            input name: "d2MorningCool", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Morning</b></span>", defaultValue: 72, required: true, width: 3, submitOnChange: false
            input name: "d2DayHeat", type: "number", title: "<span style='border-right:1px solid #CCC; padding-right:8px; white-space:nowrap;'><b>Day</b></span>", defaultValue: 66, required: true, width: 3, submitOnChange: false
            input name: "d2DayCool", type: "number", title: "<b>Day</b>", defaultValue: 75, required: true, width: 3, submitOnChange: false
            
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

            paragraph "<div style='background-color:#F2F4F4; padding:6px; font-weight:bold; border-left:4px solid #2980B9; margin-bottom:8px;'>Presence Override / Setpoints</div>"
            input name: "presenceSensor", type: "capability.presenceSensor", title: "<b>Select Presence Sensor (e.g. OwnTracks)</b>", required: false, multiple: false, submitOnChange: false
            paragraph "<div style='color:#666; font-size:11px; margin-top:-6px; margin-bottom:8px;'>" +
                      "<i>Triggers presence override setpoints when the sensor attribute evaluates to 'not present'.</i></div>"
            
            input name: "awayDev1Enabled", type: "bool", title: "<b>Enable Control House HVAC Controller/Thermostat Device Presence Override</b>", defaultValue: true, submitOnChange: false

            paragraph "<table style='width:100%; border-collapse:collapse; margin:8px 0 4px 0; font-size:12px; font-weight:bold; text-align:center; background-color:#2C3E50; color:#FFFFFF; border-radius:4px;'>" +
                      "<tr>" +
                      "<td style='width:50%; padding:6px; border-right:2px solid #FFFFFF;'><span style='color:#E74C3C;'>HOUSE HEATING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(Heat ON if &lt;)</span></td>" +
                      "<td style='width:50%; padding:6px;'><span style='color:DodgerBlue;'>HOUSE COOLING (°F)</span><br/><span style='font-size:10px; font-weight:normal; opacity:0.9;'>(A/C ON if &gt;)</span></td>" +
                      "</tr></table>"

            input name: "awayDev1Heat", type: "number", title: "<div style='border-right:1px solid #CCC; padding-right:8px;'><b>House Device</b></div>", required: true, defaultValue: 62, width: 6
            input name: "awayDev1Cool", type: "number", title: "<b>House Device</b>", required: true, defaultValue: 78, width: 6

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

        /* Section 9b: Dashboard Tile Diagnostics */
        section("<b>SECTION 9b: Built-in Dashboard Tile Details</b>", hideable: true, hidden: true) {
            def childTileDev = getChildDevice("MEM_TILE_${app.id}")
            String tileStatus = childTileDev ? "<span style='color:green; font-weight:bold;'>Active / Connected</span>" : "<span style='color:red; font-weight:bold;'>Missing</span>"
            String currentTileHtml = childTileDev ? (childTileDev.currentValue("memTile") ?: "No HTML Payload Rendered") : "N/A"

            paragraph "<div style='font-size:12px; margin-bottom:8px;'>" +
                      "MEM automatically creates and updates a child tile device for dashboard integration.</div>"

            paragraph "<table style='width:100%; border-collapse:collapse; font-size:12px; background-color:#F8F9FA; border:1px solid #E0E0E0; border-radius:4px;'>" +
                      "<tr style='background-color:#2C3E50; color:#FFF;'><th style='padding:6px; text-align:left;'>Setting / Attribute</th><th style='padding:6px; text-align:left;'>Value</th></tr>" +
                      "<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Child Device Status</b></td><td style='padding:6px;'>${tileStatus}</td></tr>" +
                      "<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Child Device Label</b></td><td style='padding:6px;'><code>${childTileDev ? childTileDev.displayName : 'MEM Tile Device'}</code></td></tr>" +
                      "<tr style='border-bottom:1px solid #E0E0E0;'><td style='padding:6px;'><b>Device Network ID</b></td><td style='padding:6px;'><code>MEM_TILE_${app.id}</code></td></tr>" +
                      "<tr><td style='padding:6px;'><b>Target Attribute</b></td><td style='padding:6px;'><code>memTile</code></td></tr>" +
                      "</table>"

            paragraph "<div style='margin-top:10px;'><b>Current Raw HTML Payload:</b></div>" +
                      "<div style='background-color:#272727; color:#00FF00; padding:8px; font-family:monospace; font-size:11px; border-radius:4px; max-height:120px; overflow-y:auto;'>" +
                      "${currentTileHtml.replace('<', '&lt;').replace('>', '&gt;')}</div>"
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

    if (targetThermostat) {
        subscribe(targetThermostat, "temperature", "temperatureHandler")
        subscribe(targetThermostat, "thermostatOperatingState", "operatingStateHandler")
        subscribe(targetThermostat, "heatingSetpoint", "setpointChangeHandler")
        subscribe(targetThermostat, "coolingSetpoint", "setpointChangeHandler")
    }
    if (targetThermostat2) {
        subscribe(targetThermostat2, "temperature", "temperatureHandler")
        subscribe(targetThermostat2, "heatingSetpoint", "setpointChangeHandler")
        subscribe(targetThermostat2, "coolingSetpoint", "setpointChangeHandler")
    }

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
// 2. UNIFIED EVALUATION PIPELINE & DEBOUNCED DISPATCHER
// =================================================================================================

/**
 * Coalesces near-simultaneous event triggers into a single evaluation pass.
 * Uses runIn(1, ..., [overwrite: true]) to debounce event surges (e.g. Mode + Override switches).
 */
void requestEvaluation(String reason, Boolean forceSetpointApply = false) {
    state.pendingEvalReason = reason
    if (forceSetpointApply) state.pendingForceSetpointApply = true
    
    logDebug "Queuing evaluation request ('${reason}'). Debouncing via 1s window..."
    runIn(1, "executeEvaluationPassDelayed", [overwrite: true])
}

void executeEvaluationPassDelayed() {
    String cause = state.pendingEvalReason ?: "Coalesced Event Stream"
    Boolean forceApply = (state.pendingForceSetpointApply == true)
    
    state.pendingEvalReason = null
    state.pendingForceSetpointApply = false
    
    executeEvaluationPass(cause, forceApply)
}

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

    String currentPeriod = sched.activePeriod ?: "N/A"
    if (state.lastActivePeriod != currentPeriod) {
        StringBuilder transitionSb = new StringBuilder()
        transitionSb.append("Period transitioned to ").append(currentPeriod)
        
        if (sched.d1TargetHeat != null && sched.d1TargetCool != null) {
            transitionSb.append(" | House Target: Heat ").append(sched.d1TargetHeat).append(" °F, Cool ").append(sched.d1TargetCool).append(" °F")
        }
        
        if (targetThermostat2 && getSettingBool("dev2AppEnabled", false)) {
            if (sched.d2ForceOff) {
                transitionSb.append(" | Office Target: FORCED OFF")
            } else if (sched.d2TargetHeat != null && sched.d2TargetCool != null) {
                transitionSb.append(" | Office Target: Heat ").append(sched.d2TargetHeat).append(" °F, Cool ").append(sched.d2TargetCool).append(" °F")
            }
        }
        
        logInfo(transitionSb.toString())
        state.lastActivePeriod = currentPeriod
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
            Boolean isSafetyActive = (state.officeSafetyActive == true)

            if (!isSafetyActive) {
                logInfo "Office HVAC Safety Trigger Active (${sched.activePeriod}). Holding Office HVAC OFF."
                state.officeSafetyActive = true
            }

            if (currentOfficeMode != "off") {
                logInfo "Office HVAC Safety Trigger Active (${sched.activePeriod}). Forcing Office HVAC OFF."
                try { targetThermostat2.off() } catch (Exception e) { logError "Failed to execute OFF on Office HVAC: ${e.message}" }
            }
        } else if (getSettingBool("dev2ScheduleEnabled", true) && sched.d2TargetHeat != null && sched.d2TargetCool != null) {
            if (state.officeSafetyActive == true) {
                logInfo "Office HVAC Safety Trigger Cleared (${sched.activePeriod}). Resuming normal scheduled operation."
                state.officeSafetyActive = false
            }
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
        d2ForceOff = true
        if (getSettingBool("awayDev1Enabled", true)) {
            d1TargetHeat = safeToDecimal(settings.awayDev1Heat, 62.0)
            d1TargetCool = safeToDecimal(settings.awayDev1Cool, 78.0)
        }
    } else if (isSleeping) {
        activePeriod = "Sleeping Override"
        d2ForceOff = true
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

    String previousOverridePeriod = state.manualOverridePeriod
    if (previousOverridePeriod && previousOverridePeriod != activePeriod) {
        logInfo "Dashboard setpoint overrides cleared (${previousOverridePeriod} -> ${activePeriod})."
        clearManualSetpointOverrides()
    }

    Boolean normalSchedulePeriod = !(activePeriod in ["Presence Override", "Sleeping Override"])

    if (normalSchedulePeriod) {
        if (state.dev1ManualHeat != null) d1TargetHeat = safeToDecimal(state.dev1ManualHeat, d1TargetHeat)
        if (state.dev1ManualCool != null) d1TargetCool = safeToDecimal(state.dev1ManualCool, d1TargetCool)
        if (state.dev2ManualHeat != null) d2TargetHeat = safeToDecimal(state.dev2ManualHeat, d2TargetHeat)
        if (state.dev2ManualCool != null) d2TargetCool = safeToDecimal(state.dev2ManualCool, d2TargetCool)
    } else {
        if (activePeriod != state.manualOverridePeriod) clearManualSetpointOverrides()
    }

    return [
        activePeriod: activePeriod,
        d1TargetHeat: d1TargetHeat, d1TargetCool: d1TargetCool,
        d2TargetHeat: d2TargetHeat, d2TargetCool: d2TargetCool,
        d2ForceOff: d2ForceOff,
        d1ManualOverride: (normalSchedulePeriod && (state.dev1ManualHeat != null || state.dev1ManualCool != null)),
        d2ManualOverride: (normalSchedulePeriod && (state.dev2ManualHeat != null || state.dev2ManualCool != null))
    ]
}

private void clearManualSetpointOverrides() {
    state.dev1ManualHeat = null
    state.dev1ManualCool = null
    state.dev2ManualHeat = null
    state.dev2ManualCool = null
    state.manualOverridePeriod = null
}

private void clearOfficeManualHeatOverride() {
    state.dev2ManualHeat = null
}

private void setManualSetpointOverride(Boolean isOffice, String attributeName, BigDecimal value, String period) {
    if (isOffice) {
        if (attributeName == "heatingSetpoint") state.dev2ManualHeat = value
        if (attributeName == "coolingSetpoint") state.dev2ManualCool = value
    } else {
        if (attributeName == "heatingSetpoint") state.dev1ManualHeat = value
        if (attributeName == "coolingSetpoint") state.dev1ManualCool = value
    }
    state.manualOverridePeriod = period
}

def setpointChangeHandler(evt) {
    if (!evt?.device || !evt?.name) return

    Boolean isHouse = targetThermostat && evt.device.id == targetThermostat.id
    Boolean isOffice = targetThermostat2 && evt.device.id == targetThermostat2.id
    if (!isHouse && !isOffice) return

    String attributeName = evt.name.toString()
    if (!(attributeName in ["heatingSetpoint", "coolingSetpoint"])) return

    BigDecimal newValue = safeToDecimal(evt.value, null)
    if (newValue == null) return

    Boolean office = isOffice
    String pendingKey = office ? "dev2SetpointPending" : "dev1SetpointPending"
    String pendingHeatKey = office ? "dev2PendingHeat" : "dev1PendingHeat"
    String pendingCoolKey = office ? "dev2PendingCool" : "dev1PendingCool"
    
    BigDecimal expectedHeat = safeToDecimal(state[pendingHeatKey], null)
    BigDecimal expectedCool = safeToDecimal(state[pendingCoolKey], null)
    BigDecimal expectedValue = (attributeName == "heatingSetpoint") ? expectedHeat : expectedCool

    if (state[pendingKey] == true && expectedValue != null && expectedValue == newValue) {
        logDebug "Absorbed internal ${evt.device.displayName} ${attributeName} event (${newValue}°F); MEM command acknowledged."
        return
    }

    Map sched = getCalculatedScheduleData()
    if (office && sched.d2ForceOff) {
        logDebug "Ignoring dashboard Office setpoint change while ${sched.activePeriod} safety isolation is active."
        return
    }

    if (!(sched.activePeriod in ["Presence Override", "Sleeping Override"])) {
        setManualSetpointOverride(office, attributeName, newValue, sched.activePeriod)
        logInfo "Dashboard override detected: ${evt.device.displayName} ${attributeName} = ${newValue}°F. Honoring manual setpoint for ${sched.activePeriod}."
        requestEvaluation("Dashboard Setpoint Override (${evt.device.displayName})", false)
    }
}

def modeChangeHandler(evt) { requestEvaluation("Hub Mode Changed (${evt?.value})", false) }
def overrideSwitchHandler(evt) { 
    requestEvaluation("Override Attribute Changed (${evt?.device?.displayName} = ${evt?.value})", false) 
    if (fanCirculateEnabled && sleepSwitch && evt?.device?.id == sleepSwitch.id) {
        logInfo "Sleeping switch state change detected (${evt?.value}). Recalculating fan circulation schedule."
        manageFanCirculation(false)
    }
}
def operatingStateHandler(evt) {
    String stateVal = evt?.value?.toLowerCase() ?: ""
    if (stateVal == "idle" || stateVal == "fan only") {
        state.lastSystemRunTime = now()
        logDebug "Recorded last HVAC activity completion timestamp: ${new Date(state.lastSystemRunTime as Long)}"
    }
}
def aqiHandler(evt) { requestEvaluation("AQI Sensor Updated (${evt?.device?.displayName} = ${evt?.value})", false) }
def airFilterSwitchHandler(evt) { requestEvaluation("Air Filter Switch State Changed (${evt?.value})", false) }
def temperatureHandler(evt) { requestEvaluation("Thermostat Ambient Temperature Updated (${evt?.device?.displayName})", false) }
def evaluateSchedule() { requestEvaluation("Scheduled Window Boundary Trigger", false) }

// =================================================================================================
// 4. THERMOSTAT CONTROL & AUTO-CHANGEOVER ENGINE
// =================================================================================================

private Map getThermostatStateSnapshot(Object dev) {
    if (!dev) return [:]
    
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

void clearDev1SetpointGuard() { 
    state.dev1SetpointPending = false 
    state.dev1PendingHeat = null
    state.dev1PendingCool = null
}

void clearDev2SetpointGuard() { 
    state.dev2SetpointPending = false 
    state.dev2PendingHeat = null
    state.dev2PendingCool = null
}

private void applySetpointsAndEvaluateMode(Object dev, BigDecimal targetHeat, BigDecimal targetCool, Map thermostatSnapshot = null, Boolean autoChangeover = true, Object hysteresisBuffer = 2, Boolean isOffice = false, Boolean forceSetpointApply = false) {
    if (!dev || targetHeat == null || targetCool == null) return
    
    Map snapshot = thermostatSnapshot ?: getThermostatStateSnapshot(dev)
    BigDecimal currentHeatSp = safeToDecimal(snapshot.heat)
    BigDecimal currentCoolSp = safeToDecimal(snapshot.cool)
    BigDecimal currentTemp = safeToDecimal(snapshot.temp)
    String currentMode = snapshot.mode?.toLowerCase() ?: "off"

    String scale = location.temperatureScale ?: "F"

    String pendingKey = isOffice ? "dev2SetpointPending" : "dev1SetpointPending"
    String pendingHeatKey = isOffice ? "dev2PendingHeat" : "dev1PendingHeat"
    String pendingCoolKey = isOffice ? "dev2PendingCool" : "dev1PendingCool"
    String clearGuardMethod = isOffice ? "clearDev2SetpointGuard" : "clearDev1SetpointGuard"

    Boolean heatNeedsUpdate = (currentHeatSp != targetHeat)
    Boolean coolNeedsUpdate = (currentCoolSp != targetCool)

    Boolean isCommandPending = (state[pendingKey] == true && 
                                state[pendingHeatKey] == targetHeat && 
                                state[pendingCoolKey] == targetCool)

    if (!forceSetpointApply && isCommandPending) {
        logDebug "Setpoint command currently in-flight for ${dev.displayName} (${targetHeat}° / ${targetCool}°). Suppressing duplicate transmission."
    } else {
        if (forceSetpointApply || heatNeedsUpdate || coolNeedsUpdate) {
            state[pendingKey] = true
            state[pendingHeatKey] = targetHeat
            state[pendingCoolKey] = targetCool
            runIn(5, clearGuardMethod)
        }

        try {
            if (forceSetpointApply || heatNeedsUpdate) {
                logInfo "Setting ${dev.displayName} Heating Setpoint to ${targetHeat} °${scale}"
                dev.setHeatingSetpoint(targetHeat)
            }

            if (forceSetpointApply || coolNeedsUpdate) {
                logInfo "Setting ${dev.displayName} Cooling Setpoint to ${targetCool} °${scale}"
                dev.setCoolingSetpoint(targetCool)
            }
        } catch (Exception e) {
            logError "Failed to apply setpoints to ${dev.displayName}: ${e.message}"
            state[pendingKey] = false
        }
    }

    if (autoChangeover && currentTemp != null) {
        BigDecimal deadband = safeToDecimal(hysteresisBuffer, 2.0)
        String guardKey = isOffice ? "dev2ModeChangePending" : "dev1ModeChangePending"
        Boolean manualOverrideActive = isOffice
            ? (state.dev2ManualHeat != null || state.dev2ManualCool != null)
            : (state.dev1ManualHeat != null || state.dev1ManualCool != null)
        
        if (state[guardKey] == true) {
            logDebug "Auto-Changeover (${dev.displayName}): Execution pending. Skipping pass."
            return
        }

        try {
            // OFFICE MANUAL HEAT SATISFACTION (CLEAN STATE CLEAR WITHOUT RECURSIVE EVALUATION)
            if (isOffice && manualOverrideActive && currentTemp >= targetHeat &&
                (currentMode == "heat" || snapshot.operatingState?.toLowerCase() == "heating" || currentMode == "auto")) {
                logInfo "Office Manual Heat Override Satisfied: Temp (${currentTemp} °${scale}) >= Target Heat (${targetHeat} °${scale}). Clearing override state."
                clearOfficeManualHeatOverride()
            } else if (currentMode == "cool" && currentTemp <= targetHeat) {
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
                BigDecimal heatTrigger = manualOverrideActive ? targetHeat : (targetHeat - deadband)
                BigDecimal coolTrigger = manualOverrideActive ? targetCool : (targetCool + deadband)

                if (currentTemp <= heatTrigger) {
                    logInfo "Auto-Changeover (${dev.displayName}): Temp (${currentTemp} °${scale}) ${manualOverrideActive ? "reached manual Heat Setpoint" : "breached Heat Deadband"}. Setting to HEAT."
                    state[guardKey] = true
                    runIn(10, isOffice ? "clearDev2ModeGuard" : "clearDev1ModeGuard")
                    dev.heat()
                } else if (currentTemp >= coolTrigger) {
                    logInfo "Auto-Changeover (${dev.displayName}): Temp (${currentTemp} °${scale}) ${manualOverrideActive ? "reached manual Cool Setpoint" : "breached Cool Deadband"}. Setting to COOL."
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

    if (isSleeping && executeOnPhase) {
        Long lastRun = state.lastSystemRunTime as Long
        if (lastRun != null) {
            Long elapsedMs = now() - lastRun
            Long requiredDelayMs = 60 * 60 * 1000L

            if (elapsedMs < requiredDelayMs) {
                Long remainingSec = ((requiredDelayMs - elapsedMs) / 1000).longValue()
                Integer remainingMin = Math.ceil(remainingSec / 60.0).intValue()

                logInfo "Fan Circulation (Sleeping Mode): Less than 1 hour elapsed since last HVAC/fan activity (${remainingMin} min remaining). Deferring circulation ON phase."

                sendIfChangedStateValue("fanPhase", "OFF Phase (Cooldown Guard)")
                runIn(remainingSec as Integer, "toggleFanCirculation", [data: [nextPhase: "ON"]])
                updateTile()
                return
            }
        }
    }

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
            state.lastSystemRunTime = now()
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
    def t2Temp = t2.temp != null ? "${t2.temp} °F" : "--"

    String hTargetStr = (sched.d1TargetHeat != null && sched.d1TargetCool != null) ? "H:${sched.d1TargetHeat?.setScale(0, BigDecimal.ROUND_HALF_UP)}/C:${sched.d1TargetCool?.setScale(0, BigDecimal.ROUND_HALF_UP)}" : "N/A"
    String oTargetStr = sched.d2ForceOff ? "OFF" : ((sched.d2TargetHeat != null && sched.d2TargetCool != null) ? "H:${sched.d2TargetHeat?.setScale(0, BigDecimal.ROUND_HALF_UP)}/C:${sched.d2TargetCool?.setScale(0, BigDecimal.ROUND_HALF_UP)}" : "N/A")

    def t1Mode = t1.mode != null ? t1.mode.toString().toUpperCase() : "--"
    def t2Mode = sched.d2ForceOff ? "OFF" : (t2.mode != null ? t2.mode.toString().toUpperCase() : "--")

    String airFilterState = ctx.filterState ?: "N/A"
    String circState = fanCirculateEnabled ? (state.fanPhase ?: "AUTO") : "Disabled"

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
    tileSb.append("<div style='line-height:1.15; text-align:center; padding:1px;'>")
    tileSb.append("<div style='font-size:0.45em;'><b>MEM Period:</b> ").append(activePeriod).append("</div>")
    tileSb.append("<div style='font-size:0.55em;'><b>House (${t1Temp}):</b> ").append(hTargetStr).append("</div>")
    tileSb.append("<div style='font-size:0.55em;'><b>Office (${t2Temp}):</b> ").append(oTargetStr).append("</div>")
    tileSb.append("<div style='font-size:0.45em;'><b>Device Mode:</b> House - ").append(t1Mode).append("</div>")
    tileSb.append("<div style='font-size:0.45em;'><b>Device Mode:</b> Office - ").append(t2Mode).append("</div>")
    tileSb.append("<div style='font-size:0.45em;'><b>Air Filter:</b> ").append(airFilterState).append(" | <b>Circulation:</b> ").append(circState).append("</div>")
    tileSb.append("<div style='font-size:0.45em;'><b>AQI:</b> <span style='color:").append(aqiColorCss).append("; font-weight:bold;'>").append(avgAqiStr).append(" ").append(aqiStatusStr).append("</span></div>")
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