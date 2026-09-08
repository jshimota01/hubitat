/**
 * Advanced Porch Lighting Manager
 * Platform: Hubitat Elevation
 * Notes: Dynamically controls porch and driveway lights based on Twilight Parser event schedules with OWM diagnostic monitoring and Hubitat Sleep Mode triggers.
 * Category: Convenience / Environmental
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
 *  Coordinates outdoor lighting automatically using live metrics from OpenWeatherMap (Lux, Sun Altitude, Cloud PCT)
 *  and/or Twilight Parser drivers. Eliminates seasonal twilight drifts using dynamic angle and cloud attenuation.
 *
 *  Instructions:
 *  1. Install in Hubitat Apps Code and add the App.
 *  2. Select target switches, OpenWeatherMap device, and Twilight Parser device.
 *  3. Choose evaluation method (Lux Threshold, Solar Elevation Angle + Overcast Shift, or Twilight Variable Offset).
 *  4. Configure cutoff/schedule times and save.
 *  
 *  Changelog:
 *	v3.2.0    09/07/26    jshimota    Lux Expansion & GUI Polish: Expanded Lux Impact to advance Evening ON times earlier during gloomy dusk periods in addition to extending Morning OFF coverage. Updated Section 3 GUI note text to clearly document dual-domain behavior.
 *	v3.1.2    09/07/26    jshimota    GUI Polish & Note Expansion: Renamed Section 3 header to '3. Environmental & Schedule Adjustments'. Added detailed Scope and Behavior note callout blocks under Cloud Cover and Lux Impact adjustment inputs.
 *	v3.1.1    09/07/26    jshimota    GUI Guidance Polish: Updated switch input title to 'Select Light(s) / Switch(es)'. Added explicit scaling and threshold guidance notes under Cloud Cover and Lux Impact toggles in Section 3.
 *	v3.1.0    09/07/26    jshimota    Midnight Boundary Protection & Race Hardening: Defined explicit 12:00 AM -> 12:02 AM execution cascade between Twilight Parser updates, 1-second debouncing, and 12:02 AM master schedule anchoring. Hardened OWM boundary so weather updates read cached metrics on demand rather than triggering recursive schedule rebuilds.
 *	v3.0.0    09/07/26    jshimota    Architectural Cleanliness Milestone: Eliminated code duplication between schedule map generation and reboot reconciliation. Evening resolver now natively anchors cutoffs relative to Evening ON. ReconcileStateOnBoot now directly evaluates schedule map dates in ~10 lines. Renamed getNormalizedOffsetMins to getPositiveOffsetMins. Added active window indicators (ACTIVE / INACTIVE) to status dashboard header.
 *	v2.9.1    09/07/26    jshimota    Bug Fix & Date Anchor Hardening: Corrected Evening Cutoff anchor logic in getEvaluatedScheduleMap() for cross-midnight schedules (12:00 AM / past midnight cutoffs). Anchored driver twilight timestamps to current calendar day during reboot reconciliation. Bulletproofed isInsideScheduleWindow() cross-midnight span checks.
 *	v2.9.0    09/07/26    jshimota    Architecture Overhaul: Implemented domain-specific resolveMorningScheduleTime() and resolveEveningScheduleTime() wrappers over a primitive buildClockDate() engine. Added unified isInsideScheduleWindow() to eliminate cross-midnight and past-time reboot reconciliation bugs.
 *	v2.8.1    09/07/26    jshimota    Reboot Window & State Hardening: Created getTodayClockTime() helper for reconcileStateOnBoot() to prevent past-time date rollovers during reboot window evaluation. Populated state display variables during schedule building to prevent null next event strings. Added try/catch guards to OWM attribute numeric conversions.
 *	v2.8.0    09/07/26    jshimota    GUI Polish & Driver Detection Sync: Dynamically queries Twilight Parser driver to auto-detect active twilight scale and mark option as '(In Use)' in preference dropdown and status header. Added CPU-saving early exit for unconfigured notification devices. Updated GUI labels for switches, Twilight Parser, OWM, Cloud scaling, and Lux impact.
 *	v2.7.1    09/07/26    jshimota    Sleep Guard Refinement: Restructured Sleep Mode scope so it exclusively triggers an immediate turn-OFF when transitioning to 'Sleeping' mode. Removed persistent sleep mode suppression flags from Morning ON, Evening ON, and reboot reconciliation handlers so sleeping in does not block scheduled morning turn-ON events.
 *	v2.7.0    09/06/26    jshimota    Production Hardening Overhaul: Implemented true schedule-aware reboot reconciliation, next-day clock time rollover (fixes midnight cutoff bug), Sleep Mode guards for ON handlers, version change detection fix, debounced Twilight Parser attribute updates, explicit switch collection iteration, and chronological Next Event sorting.
 *	v2.6.0    09/06/26    jshimota    Stability & Reboot Hardening: Added location systemStart subscription to recover missed schedule windows after a hub reboot, added NPE safety guards for uninitialized Twilight/OWM attributes, and optimized UI preview calculation sync.
 *	v2.5.3    09/06/26    jshimota    GUI Polish: Updated Evening OFF calculated factor indicators to explicitly report 'Mode: Sleeping' alongside scheduled cutoff times when sleep mode triggers are active.
 *	v2.5.2    09/06/26    jshimota    GUI Sync Fix: Connected top status dashboard table directly to calculateSchedulePreview() engine so Morning and Evening ON/OFF times update in real time when settings or environmental shifts change.
 *	v2.5.1    09/06/26    jshimota    GUI Expansion: Added explicit 'Morning Off - Extend Time after Sunrise Occurs' and 'Evening On - Lead Time before Sunset Occurs' offset inputs to replace hardcoded 30-minute defaults. Updated GUI factors and calculation engines.
 *	v2.5.0    09/06/26    jshimota    Architectural Correction: Re-anchored Morning OFF to Sunrise Occurs (+30m default + Lux extension) and Evening ON to Sunset Occurs (-30m default - Lead - Cloud). Updated GUI preview labels and logic across schedule construction engines.
 *	v2.4.2    09/06/26    jshimota    GUI & Logic Refinement: Restricted Lux impact exclusively to Morning OFF extension, renamed factor to 'Lux', changed 'Sunrise:' to 'Sunrise Occurs:', renamed header to 'Twilight Scale', updated 'Used (Default)' to 'Used (Civil)', and embedded Sunrise/Sunset timestamps directly into the main status dashboard columns.
 *	v2.4.1    09/06/26    jshimota    Logic Adjustment: Corrected Lux Impact logic so low lux extends lighting coverage by adding positive minutes to Morning OFF / Evening ON windows rather than reducing runtime.
 *	v2.4.0    09/06/26    jshimota    GUI & Logic Overhaul: Added specific ON time overrides for Morning and Evening. Placed factor indicators directly beside the specific ON or OFF time they impact. Added explicit OVERRIDE badges when hard-coded times bypass twilight calculations.
 *	v2.3.6    09/06/26    jshimota    GUI Polish: Rephrased Twilight Begin and Twilight End labels to 'Twilight Begins' and 'Twilight Ends' across descriptions and preview cards for more natural readability.
 *	v2.3.5    09/06/26    jshimota    GUI Refinement: Placed active impact factors inline on the Calculated Morning and Evening time lines for immediate visibility into offset contributors.
 *	v2.3.4    09/06/26    jshimota    Bug check: Fixed getPositiveOffsetMins fallback parsing bug where typed values fell back to default 30. Added explicit environmental shift indicators in Section 2 previews.
 *	v2.3.3    09/06/26    jshimota    GUI Expansion: Added 'Currently Selected Twilight Times' raw preview card above calculated execution times in Section 2 so users can compare raw Twilight Parser timestamps with adjusted schedules.
 *	v2.3.2    09/06/26    jshimota    GUI Enhancement: Added dynamic Live Schedule Preview cards into Section 2 so users can instantly see calculated Morning/Evening ON and OFF times based on active offsets and settings.
 *	v2.3.1    09/06/26    jshimota    Dashboard Expansion: Added Twilight Parser Calculation Reference and Last Illuminance reading to the top status dashboard header.
 *	v2.3.0    09/06/26    jshimota    GUI & Schedule Refinement: Added master Morning/Evening enable toggles, set default Morning Off to +30m post-twilight begin, set default Evening On to -30m pre-twilight end, added calculated estimate badges directly to Section 2 headers, relocated Sleep Mode switch to Section 3, and removed 'or Night' wording.
 *	v2.2.0    09/06/26    jshimota    GUI & Logic overhaul: Added Section 3 'Optional Schedule Adjustments' with Twilight Parser attribute selectors (Used/Civil/Nautical/Astronomical), Cloud Cover extension, and Lux impact logic. Rephrased Schedule labels, split into Morning/Evening subsections, made lead times optional, updated Evening Off default to 12:00 AM, and moved App Preferences to Section 4.
 *	v2.1.0    09/06/26    jshimota    Feature addition: Subscribed to Hubitat location mode events. Added option to turn OFF lights immediately when mode changes to 'Night' / 'Sleeping' ahead of scheduled cutoff.
 *	v2.0.2    09/06/26    jshimota    Logging expansion: Added logTraceEnable option to App Preferences and embedded deep trace logging wrappers across schedule construction, event handlers, and helper routines.
 *	v2.0.1    09/06/26    jshimota    GUI cleanup: Made Device Connections and Schedule Configuration collapsible (default collapsed), rephrased twilight offset inputs to handle positive numbers safely without validation errors, and aligned dashboard grid layout.
 *	v2.0.0    09/06/26    jshimota    Complete architectural rewrite: Separated scheduling authority (Twilight Parser) from environmental monitoring (OWM). Removed OWM-driven switch triggers and hardcoded hour tests. Added new status dashboard.
 *	v1.0.4    09/05/26    jshimota    Added nightly schedule refresh at 12:02 AM and bulletproofed unschedule routines against orphan jobs.
 *	v1.0.3    09/05/26    jshimota    Bug check: Added morning turn-off evaluation for angle/lux modes, enhanced time-window guard logic, and modernized GUI card containers.
 *	v1.0.2    09/05/26    jshimota    Fixed notification device capability selector and split solar angles into morning/evening thresholds with independent cloud cover shifts.
 *	v1.0.1    09/05/26    jshimota    Added live UI status dashboard and updated version tracking.
 *	v1.0.0    09/05/26    jshimota    Initial release built on standardized App Template v1.2.2.
 **/

static String version() { return '3.2.0' }
def timeStamp() { return "2026/09/07 09:40 AM" }

definition(
    name: "Advanced Porch Lighting Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Automates porch and outdoor lights using Twilight Parser timestamps, Hubitat Sleep Mode monitoring, and OWM diagnostics.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_porch_lighting_manager/advanced_porch_lighting_manager.groovy"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:12px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Advanced Porch Lighting Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.85;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        /* Calculate Preview Times for UI & Status Board */
        Map calculatedPreview = calculateSchedulePreview()

        /* Real-Time Environment & Schedule Status Board */
        section() {
            paragraph getStatusBoardHtml(calculatedPreview)
        }

        /* 1. Device Connections */
        section("<b>1. Device Connections</b>", hideable: true, hidden: true) {
            input name: "switches", type: "capability.switch", title: "Select Light(s) / Switch(es)", multiple: true, required: true
            input name: "twilightDevice", type: "capability.sensor", title: "Select Schedule Source (Twilight Begin and End Source Values)", multiple: false, required: true, submitOnChange: true
            input name: "owmDevice", type: "capability.sensor", title: "Select Diagnostic and Overrides Source (Lux, Cloud Cover, and Time of Day Source)", multiple: false, required: false, submitOnChange: true
            input name: "notifier", type: "capability.notification", title: "Select Notification Device (Optional)", multiple: true, required: false
        }

        String mornHeaderEst = (enableMorningSchedule != false) ? "Morning ON: ${calculatedPreview.mornOn}" : "Morning: Disabled"
        String eveHeaderEst  = (enableEveningSchedule != false) ? "Evening ON: ${calculatedPreview.eveOn}"   : "Evening: Disabled"
        String schedSectionTitle = "<b>2. Schedule Configuration</b> &nbsp;<span style='font-size:0.85em; font-weight:normal; color:#00BCD4;'>(${mornHeaderEst} | ${eveHeaderEst})</span>"

        /* 2. Schedule Configuration */
        section(schedSectionTitle, hideable: true, hidden: true) {
            // MORNING SUBSECTION
            paragraph "<div style='background:#E8F0FE; padding:6px 10px; border-radius:4px; font-weight:bold; color:#1A73E8;'>🌅 Morning Schedule Configuration</div>"
            input name: "enableMorningSchedule", type: "bool", title: "Enable Morning Lighting Schedule?", defaultValue: true, submitOnChange: true

            if (enableMorningSchedule != false) {
                // Raw Twilight Parser Morning Timestamps
                paragraph "<div style='background:#F8F9FA; padding:6px 12px; border-left:4px solid #6c757d; margin:6px 0 4px 0; font-size:0.88em; color:#333;'>" +
                          "<b>Currently Selected Twilight Times:</b> &nbsp; Twilight Begins: <span style='color:#2E7D32; font-weight:bold;'>${calculatedPreview.rawMornBegin}</span> &nbsp;|&nbsp; Sunrise Occurs: <span style='color:#C62828; font-weight:bold;'>${calculatedPreview.rawSunrise}</span>" +
                          "</div>"

                // Final Adjusted Morning Execution Times + Targeted Factors
                paragraph "<div style='background:#F1F3F4; padding:6px 12px; border-left:4px solid #1A73E8; margin:0 0 8px 0; font-size:0.88em; color:#202124;'>" +
                          "<b>Calculated Morning Times:</b> &nbsp; " +
                          "ON: <span style='color:#2E7D32; font-weight:bold;'>${calculatedPreview.mornOn}</span> <span style='font-size:0.82em; color:#555;'>(${calculatedPreview.mornOnFactors})</span> " +
                          "&nbsp;|&nbsp; " +
                          "OFF: <span style='color:#C62828; font-weight:bold;'>${calculatedPreview.mornOff}</span> <span style='font-size:0.82em; color:#555;'>(${calculatedPreview.mornOffFactors})</span>" +
                          "</div>"

                input name: "mornOnTime", type: "time", title: "Morning On - Specific Time (Optional Override)", description: "If set, overrides all twilight lead times and cloud shifts for Morning ON", required: false, submitOnChange: true
                input name: "mornOffset", type: "number", title: "Morning On - Lead Time before Twilight Begins (Minutes, Optional)", description: "Default is 30 mins BEFORE Twilight Begins (Bypassed if Specific On Time is set)", defaultValue: 30, required: false, submitOnChange: true
                
                paragraph "<hr style='border:0; border-top:1px dashed #CCC; margin:8px 0;'/>"
                
                input name: "morningOffTime", type: "time", title: "Morning Off - Specific Time (Optional Override)", description: "If set, overrides all sunrise offsets for Morning OFF", required: false, submitOnChange: true
                input name: "mornOffOffset", type: "number", title: "Morning Off - Extend Time after Sunrise Occurs (Minutes, Optional)", description: "Default turns OFF 30 mins AFTER Sunrise Occurs (Bypassed if Specific Off Time is set)", defaultValue: 30, required: false, submitOnChange: true
            }

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:12px 0 8px 0;'/>"

            // EVENING SUBSECTION
            paragraph "<div style='background:#E8F0FE; padding:6px 10px; border-radius:4px; font-weight:bold; color:#1A73E8;'>🌆 Evening Schedule Configuration</div>"
            input name: "enableEveningSchedule", type: "bool", title: "Enable Evening Lighting Schedule?", defaultValue: true, submitOnChange: true

            if (enableEveningSchedule != false) {
                // Raw Twilight Parser Evening Timestamps
                paragraph "<div style='background:#F8F9FA; padding:6px 12px; border-left:4px solid #6c757d; margin:6px 0 4px 0; font-size:0.88em; color:#333;'>" +
                          "<b>Currently Selected Twilight Times:</b> &nbsp; Sunset Occurs: <span style='color:#2E7D32; font-weight:bold;'>${calculatedPreview.rawSunset}</span> &nbsp;|&nbsp; Twilight Ends: <span style='color:#C62828; font-weight:bold;'>${calculatedPreview.rawEveEnd}</span>" +
                          "</div>"

                // Final Adjusted Evening Execution Times + Targeted Factors
                paragraph "<div style='background:#F1F3F4; padding:6px 12px; border-left:4px solid #1A73E8; margin:0 0 8px 0; font-size:0.88em; color:#202124;'>" +
                          "<b>Calculated Evening Times:</b> &nbsp; " +
                          "ON: <span style='color:#2E7D32; font-weight:bold;'>${calculatedPreview.eveOn}</span> <span style='font-size:0.82em; color:#555;'>(${calculatedPreview.eveOnFactors})</span> " +
                          "&nbsp;|&nbsp; " +
                          "OFF: <span style='color:#C62828; font-weight:bold;'>${calculatedPreview.eveOff}</span> <span style='font-size:0.82em; color:#555;'>(${calculatedPreview.eveOffFactors})</span>" +
                          "</div>"

                input name: "eveOnTime", type: "time", title: "Evening On - Specific Time (Optional Override)", description: "If set, overrides all sunset lead times and cloud shifts for Evening ON", required: false, submitOnChange: true
                input name: "eveOffset", type: "number", title: "Evening On - Lead Time before Sunset Occurs (Minutes, Optional)", description: "Default turns ON 30 mins BEFORE Sunset Occurs (Bypassed if Specific On Time is set)", defaultValue: 30, required: false, submitOnChange: true
                
                paragraph "<hr style='border:0; border-top:1px dashed #CCC; margin:8px 0;'/>"
                
                input name: "eveningCutoffTime", type: "time", title: "Evening Off - Specific Time (Optional Override)", description: "Default turns OFF at 12:00 AM", defaultValue: "00:00", required: false, submitOnChange: true

                // GUI Validation Warning for Equal Evening ON / OFF Times
                if (eveOnTime && eveningCutoffTime && eveOnTime == eveningCutoffTime) {
                    paragraph "<div style='background:#FFEBEE; color:#C62828; padding:8px; border-radius:4px; font-size:0.85em; margin-top:6px;'>" +
                              "⚠️ <b>Configuration Warning:</b> Evening ON time matches Evening OFF time. Please specify different cutoff times to prevent accidental 24-hour schedules.</div>"
                }
            }
        }

        /* 3. Environmental & Schedule Adjustments */
        section("<b>3. Environmental & Schedule Adjustments</b>", hideable: true, hidden: true) {
            input name: "turnOffOnSleepMode", type: "bool", title: "Turn OFF lights immediately when Hub Mode changes to 'Sleeping'?", description: "Triggers an immediate light shutoff when going to bed early", defaultValue: true, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            // Dynamic Twilight Scale Option Map Builder
            Map scaleOptions = [
                "civil": "Civil",
                "nautical": "Nautical",
                "astronomical": "Astronomical"
            ]

            String activeDriverScale = getActiveTwilightDriverChoiceKey()
            if (scaleOptions.containsKey(activeDriverScale)) {
                scaleOptions[activeDriverScale] = "${scaleOptions[activeDriverScale]} (In Use)"
            }

            input name: "twilightType", type: "enum", title: "Twilight Parser Calculation Reference", 
                  options: scaleOptions, 
                  defaultValue: activeDriverScale, required: true, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "enableCloudAdjustment", type: "bool", title: "Enable Cloud Cover Adjustment?", description: "Advances ON times earlier on heavy overcast days based on OWM Cloud PCT.", defaultValue: false, submitOnChange: true
            paragraph "<div style='background:#F8F9FA; padding:8px 12px; border-left:4px solid #00BCD4; margin-top:-6px; margin-bottom:8px; font-size:0.85em; color:#333;'>" +
                      "<b>Cloud Cover Adjustment Behavior:</b><br/>" +
                      "• <b>Scope:</b> Applied strictly to turn-ON events (Morning ON and Evening ON).<br/>" +
                      "• <b>Behavior:</b> When enabled, heavy overcast (Cloud PCT) advances the target turn-ON time earlier (e.g. turning lights on earlier before sunset or twilight begin to compensate for dark skies). Scales proportionally based on current cloud percentage (e.g. 50% cloud cover applies 50% of your maximum offset minutes)." +
                      "</div>"

            if (enableCloudAdjustment) {
                input name: "maxCloudShiftMins", type: "number", title: "Max Cloud Adjustment Offset (Minutes)", description: "Max minutes to advance ON time at 100% cloud cover", defaultValue: 15, required: true, submitOnChange: true
            }

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "enableLuxAdjustment", type: "bool", title: "Enable Lux Impact Adjustment?", description: "Adjusts Evening ON and Morning OFF based on live illuminance readings.", defaultValue: false, submitOnChange: true
            paragraph "<div style='background:#F8F9FA; padding:8px 12px; border-left:4px solid #FFB74D; margin-top:-6px; margin-bottom:8px; font-size:0.85em; color:#333;'>" +
                      "<b>Lux Impact Adjustment Behavior:</b><br/>" +
                      "• <b>Scope:</b> Applied to Evening ON (advances turn-ON earlier) and Morning OFF (extends coverage runtime).<br/>" +
                      "• <b>Behavior:</b> When illuminance falls at or below your configured threshold, it advances Evening ON earlier for dark gloomy dusks and appends extension minutes to Morning OFF to maintain light coverage during dim mornings." +
                      "</div>"

            if (enableLuxAdjustment) {
                input name: "luxThreshold", type: "number", title: "Lux Impact Threshold (lx)", description: "If Lux falls below this value, adjust Evening ON earlier and extend Morning OFF window", defaultValue: 50, required: true, submitOnChange: true
                input name: "luxShiftMins", type: "number", title: "Max Lux Adjustment Offset (Minutes)", description: "Minutes to advance Evening ON / extend Morning OFF when Lux threshold is triggered", defaultValue: 10, required: true, submitOnChange: true
            }
        }

        /* 4. App Preferences & Logging Options */
        section("<b>4. App Preferences & Logging Options</b>", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }
    }
}

/* =========================================================================================
   LAYER 3: PRIMITIVE DATE & CALENDAR ENGINE
   ========================================================================================= */

private Date buildClockDate(def timeInput, int dayOffset = 0) {
    if (!timeInput) return null
    try {
        Calendar cal = Calendar.getInstance(location.timeZone)
        if (timeInput instanceof Date) {
            Calendar inputCal = Calendar.getInstance(location.timeZone)
            inputCal.time = (Date) timeInput
            cal.set(Calendar.HOUR_OF_DAY, inputCal.get(Calendar.HOUR_OF_DAY))
            cal.set(Calendar.MINUTE, inputCal.get(Calendar.MINUTE))
        } else {
            java.text.SimpleDateFormat inputSdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
            Date parsed = inputSdf.parse(timeInput.toString())
            Calendar targetCal = Calendar.getInstance(location.timeZone)
            targetCal.time = parsed
            cal.set(Calendar.HOUR_OF_DAY, targetCal.get(Calendar.HOUR_OF_DAY))
            cal.set(Calendar.MINUTE, targetCal.get(Calendar.MINUTE))
        }
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (dayOffset != 0) cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        return cal.time
    } catch (Exception e) {
        logError "buildClockDate() -> Failed to construct date for '${timeInput}': ${e.message}"
        return null
    }
}

/* =========================================================================================
   LAYER 1: DOMAIN-SPECIFIC TIME RESOLVERS
   ========================================================================================= */

private Date resolveMorningScheduleTime(def timeInput, Boolean forNextFutureExecution = true) {
    Date todayDate = buildClockDate(timeInput, 0)
    if (!todayDate) return null
    
    if (forNextFutureExecution) {
        Date now = new Date()
        return todayDate.after(now) ? todayDate : buildClockDate(timeInput, 1)
    }
    return todayDate
}

private Date resolveEveningScheduleTime(def timeInput, Date eveOnReferenceDate = null) {
    Date unanchoredOff = buildClockDate(timeInput, 0)
    if (!unanchoredOff) return null

    if (eveOnReferenceDate) {
        Calendar onCal = Calendar.getInstance(location.timeZone)
        onCal.time = eveOnReferenceDate

        Calendar offCal = Calendar.getInstance(location.timeZone)
        offCal.time = unanchoredOff

        // If cutoff clock time is earlier than or equal to ON time, anchor to the following day
        if (offCal.get(Calendar.HOUR_OF_DAY) < onCal.get(Calendar.HOUR_OF_DAY) || 
           (offCal.get(Calendar.HOUR_OF_DAY) == onCal.get(Calendar.HOUR_OF_DAY) && offCal.get(Calendar.MINUTE) <= onCal.get(Calendar.MINUTE))) {
            
            Calendar anchoredCal = Calendar.getInstance(location.timeZone)
            anchoredCal.time = eveOnReferenceDate
            anchoredCal.add(Calendar.DAY_OF_YEAR, 1)
            anchoredCal.set(Calendar.HOUR_OF_DAY, offCal.get(Calendar.HOUR_OF_DAY))
            anchoredCal.set(Calendar.MINUTE, offCal.get(Calendar.MINUTE))
            anchoredCal.set(Calendar.SECOND, 0)
            anchoredCal.set(Calendar.MILLISECOND, 0)
            return anchoredCal.time
        }
    }
    return unanchoredOff
}

/* =========================================================================================
   LAYER 2: UNIFIED WINDOW EVALUATOR
   ========================================================================================= */

private Boolean isInsideScheduleWindow(Date now, Date onDate, Date offDate) {
    if (!onDate || !offDate || !now) return false
    
    long nowTime = now.time
    long onTime = onDate.time
    long offTime = offDate.time

    if (offTime > onTime) {
        return (nowTime >= onTime && nowTime < offTime)
    }
    
    // Cross-Midnight Window Check
    return (nowTime >= onTime || nowTime < offTime)
}

/* Dynamic Twilight Parser Active Choice Inspector */
private String getActiveTwilightDriverChoiceKey() {
    if (!twilightDevice) return "civil"
    try {
        def rawChoice = twilightDevice.getSetting("twilightChoice")?.toString()
        switch (rawChoice) {
            case "2": return "nautical"
            case "3": return "astronomical"
            case "1":
            default:  return "civil"
        }
    } catch (Exception e) {
        logDebug "getActiveTwilightDriverChoiceKey() -> Unable to read twilightChoice setting: ${e.message}"
        return "civil"
    }
}

/* Dynamic Schedule & Window Map Generator */
private Map getEvaluatedScheduleMap() {
    Map sched = [
        rawMornBegin: "--", rawSunrise: "--", rawSunset: "--", rawEveEnd: "--",
        mornOn: "--", mornOnFactors: "None", mornOff: "--", mornOffDefault: "--", mornOffFactors: "Sunrise (+30m)",
        eveOn: "--", eveOnFactors: "None", eveOff: "--", eveOffFactors: "Default (12:00 AM)",
        mornOnDate: null, mornOffDate: null, eveOnDate: null, eveOffDate: null
    ]

    if (!twilightDevice) return sched

    Map twilightTimes = getTwilightTimestamps()
    Map envShifts = getDetailedEnvironmentalShifts()

    String sunriseStr = twilightDevice?.currentValue("localSunrise")
    if (sunriseStr) {
        Date sDate = parseDateString(sunriseStr)
        if (sDate) sched.rawSunrise = sDate.format("h:mm a", location.timeZone)
    }

    String sunsetStr = twilightDevice?.currentValue("localSunset")
    if (sunsetStr) {
        Date sSetDate = parseDateString(sunsetStr)
        if (sSetDate) sched.rawSunset = sSetDate.format("h:mm a", location.timeZone)
    }

    // MORNING EVALUATION
    if (twilightTimes.begin) {
        Date mornTwilightRaw = parseDateString(twilightTimes.begin)
        if (mornTwilightRaw) {
            sched.rawMornBegin = mornTwilightRaw.format("h:mm a", location.timeZone)

            if (mornOnTime) {
                sched.mornOnDate = resolveMorningScheduleTime(mornOnTime, true)
                sched.mornOn = sched.mornOnDate ? sched.mornOnDate.format("h:mm a", location.timeZone) : "--"
                sched.mornOnFactors = "OVERRIDE: Hard-Coded Time"
            } else {
                Date mornTwilightToday = buildClockDate(mornTwilightRaw, 0)
                int leadMins = getPositiveOffsetMins(settings.mornOffset, 30)
                int totalMornOnShift = leadMins + envShifts.cloudShift
                sched.mornOnDate = new Date(mornTwilightToday.time - (totalMornOnShift * 60 * 1000))
                sched.mornOn = sched.mornOnDate.format("h:mm a", location.timeZone)

                List<String> mOnFactors = []
                if (leadMins > 0) mOnFactors << "Lead (-${leadMins}m)"
                if (envShifts.cloudShift > 0) mOnFactors << "Cloud (-${envShifts.cloudShift}m)"
                sched.mornOnFactors = mOnFactors ? mOnFactors.join(" + ") : "Twilight Begins"
            }
        }
    }

    if (sunriseStr) {
        Date sDateRaw = parseDateString(sunriseStr)
        if (sDateRaw) {
            int mornOffLeadMins = getPositiveOffsetMins(settings.mornOffOffset, 30)
            Date sDateToday = buildClockDate(sDateRaw, 0)
            Date defaultOffDate = new Date(sDateToday.time + (mornOffLeadMins * 60 * 1000))
            sched.mornOffDefault = defaultOffDate.format("h:mm a", location.timeZone)

            if (morningOffTime) {
                sched.mornOffDate = resolveMorningScheduleTime(morningOffTime, true)
                sched.mornOff = sched.mornOffDate ? sched.mornOffDate.format("h:mm a", location.timeZone) : sched.mornOffDefault
                sched.mornOffFactors = "OVERRIDE: Hard-Coded Time"
            } else {
                int extMins = envShifts.luxShift
                sched.mornOffDate = new Date(defaultOffDate.time + (extMins * 60 * 1000))
                sched.mornOff = sched.mornOffDate.format("h:mm a", location.timeZone)
                
                List<String> mOffFactors = ["Sunrise (+${mornOffLeadMins}m)"]
                if (extMins > 0) mOffFactors << "Lux (+${extMins}m)"
                sched.mornOffFactors = mOffFactors.join(" + ")
            }
        }
    }

    // EVENING EVALUATION
    if (twilightTimes.end) {
        Date eveTwilightDate = parseDateString(twilightTimes.end)
        if (eveTwilightDate) sched.rawEveEnd = eveTwilightDate.format("h:mm a", location.timeZone)
    }

    if (sunsetStr) {
        Date sSetRaw = parseDateString(sunsetStr)
        if (sSetRaw) {
            if (eveOnTime) {
                sched.eveOnDate = buildClockDate(eveOnTime, 0)
                sched.eveOn = sched.eveOnDate ? sched.eveOnDate.format("h:mm a", location.timeZone) : "--"
                sched.eveOnFactors = "OVERRIDE: Hard-Coded Time"
            } else {
                Date sSetToday = buildClockDate(sSetRaw, 0)
                int leadMins = getPositiveOffsetMins(settings.eveOffset, 30)
                int totalEveShift = leadMins + envShifts.cloudShift + envShifts.luxShift
                sched.eveOnDate = new Date(sSetToday.time - (totalEveShift * 60 * 1000))
                sched.eveOn = sched.eveOnDate.format("h:mm a", location.timeZone)

                List<String> eOnFactors = []
                if (leadMins > 0) eOnFactors << "Lead (-${leadMins}m)"
                if (envShifts.cloudShift > 0) eOnFactors << "Cloud (-${envShifts.cloudShift}m)"
                if (envShifts.luxShift > 0) eOnFactors << "Lux (-${envShifts.luxShift}m)"
                sched.eveOnFactors = eOnFactors ? eOnFactors.join(" + ") : "Sunset Occurs"
            }
        }
    }

    // Anchoring Evening OFF Cutoff relative to Evening ON
    String offTimeIso = eveningCutoffTime ?: "00:00"
    sched.eveOffDate = resolveEveningScheduleTime(offTimeIso, sched.eveOnDate)
    sched.eveOff = sched.eveOffDate ? sched.eveOffDate.format("h:mm a", location.timeZone) : "12:00 AM"

    List<String> eOffFactors = []
    if (eveningCutoffTime) {
        eOffFactors << "OVERRIDE: Hard-Coded Time"
    } else {
        eOffFactors << "Default (12:00 AM)"
    }

    if (turnOffOnSleepMode == true) {
        eOffFactors << "or Mode: Sleeping"
    }

    sched.eveOffFactors = eOffFactors.join(" ")

    return sched
}

private Map calculateSchedulePreview() {
    return getEvaluatedScheduleMap()
}

/* Exception-Guarded Environmental Shift Breakdown Engine */
private Map getDetailedEnvironmentalShifts() {
    int cShift = 0
    int lShift = 0

    if (enableCloudAdjustment && owmDevice) {
        try {
            def rawCloud = owmDevice.currentValue("currentCloudPCT")
            BigDecimal cloudPct = (rawCloud != null) ? rawCloud.toString().toBigDecimal() : 0.0
            int maxShift = (maxCloudShiftMins != null) ? maxCloudShiftMins.toInteger() : 15
            cShift = Math.round((cloudPct / 100.0) * maxShift) as int
        } catch (Exception e) {
            logWarn "getDetailedEnvironmentalShifts() -> Unable to parse cloud cover attribute: ${e.message}"
            cShift = 0
        }
    }

    if (enableLuxAdjustment && owmDevice) {
        try {
            def rawLux = owmDevice.currentValue("currentIlluminance") ?: owmDevice.currentValue("illuminance")
            BigDecimal currentLux = (rawLux != null) ? rawLux.toString().toBigDecimal() : 0.0
            BigDecimal targetLux = (luxThreshold != null) ? luxThreshold.toBigDecimal() : 50.0
            if (currentLux <= targetLux) {
                lShift = (luxShiftMins != null) ? luxShiftMins.toInteger() : 10
            }
        } catch (Exception e) {
            logWarn "getDetailedEnvironmentalShifts() -> Unable to parse illuminance attribute: ${e.message}"
            lShift = 0
        }
    }

    return [cloudShift: cShift, luxShift: lShift]
}

/* Status Dashboard HTML Generator with Real-Time Window Badging */
String getStatusBoardHtml(Map liveCalc = [:]) {
    logTrace "getStatusBoardHtml() -> Generating live environment dashboard..."
    String switchState = switches ? (switches[0]?.currentValue("switch") ?: "off") : "unknown"
    String badgeColor  = (switchState == "on") ? "#2e7d32" : "#757575"
    String badgeText   = (switchState == "on") ? "LIGHTS ON 💡" : "LIGHTS OFF 🌙"

    String betwixtText   = owmDevice ? (owmDevice.currentValue("betwixt") ?: "N/A") : "N/A"
    String sunAltText    = owmDevice ? (owmDevice.currentValue("currentSunAltitudeText") ?: "--°") : "--°"
    String cloudPctText  = owmDevice ? "${owmDevice.currentValue('currentCloudPCT') ?: 0}%" : "N/A"
    String currentIllum  = owmDevice ? (owmDevice.currentValue("currentIlluminanceText") ?: "${owmDevice.currentValue('illuminance') ?: 0} lx") : "N/A"
    String currentMode   = location.mode ?: "Unknown"

    String sunriseDisp = liveCalc?.rawSunrise ?: "--"
    String sunsetDisp  = liveCalc?.rawSunset ?: "--"

    String activeDriverScale = getActiveTwilightDriverChoiceKey()
    String selectedScaleKey = (twilightType ?: activeDriverScale).toLowerCase()
    String twilightScale = selectedScaleKey.capitalize()
    if (selectedScaleKey == activeDriverScale) {
        twilightScale += " (In Use)"
    }

    String lightingState = "NIGHTTIME"
    if (owmDevice) {
        String isDay = owmDevice.currentValue("currentIsDay") ?: "false"
        if (isDay == "true") {
            lightingState = "DAYTIME"
        } else if (betwixtText.contains("twilight")) {
            lightingState = "TWILIGHT"
        }
    }

    Date now = new Date()
    Boolean mornActive = (enableMorningSchedule != false) && isInsideScheduleWindow(now, liveCalc?.mornOnDate, liveCalc?.mornOffDate)
    Boolean eveActive  = (enableEveningSchedule != false) && isInsideScheduleWindow(now, liveCalc?.eveOnDate, liveCalc?.eveOffDate)

    String mornStatusBadge = mornActive ? "<span style='background:#2E7D32; color:#FFF; padding:2px 6px; border-radius:3px; font-size:0.8em; margin-left:6px;'>ACTIVE</span>" : ""
    String eveStatusBadge  = eveActive  ? "<span style='background:#2E7D32; color:#FFF; padding:2px 6px; border-radius:3px; font-size:0.8em; margin-left:6px;'>ACTIVE</span>" : ""

    String mornOnDisp  = (enableMorningSchedule != false) ? (liveCalc?.mornOn ?: state.schedMornOnDisp ?: "--") : "DISABLED"
    String mornOffDisp = (enableMorningSchedule != false) ? (liveCalc?.mornOff ?: state.schedMornOffDisp ?: "--") : "DISABLED"
    String eveOnDisp   = (enableEveningSchedule != false) ? (liveCalc?.eveOn ?: state.schedEveOnDisp ?: "--") : "DISABLED"
    String eveOffDisp  = (enableEveningSchedule != false) ? (liveCalc?.eveOff ?: state.schedEveOffDisp ?: "--") : "DISABLED"
    String nextEvent   = state.nextScheduledEventDesc ?: "None scheduled"

    String html = """
    <div style='background-color:#1e1e24; color:#ffffff; padding:12px; border-radius:8px; font-family:sans-serif;'>
        <div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;'>
            <span style='font-weight:bold; font-size:1.05em;'>Porch Lighting Manager</span>
            <span style='background-color:${badgeColor}; color:#fff; padding:3px 8px; border-radius:4px; font-weight:bold; font-size:0.85em;'>${badgeText}</span>
        </div>

        <div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; font-size:0.85em; color:#ddd; background:#121216; padding:6px 8px; border-radius:4px;'>
            <span><b>State:</b> <span style='color:#00BCD4; font-weight:bold;'>${lightingState}</span></span>
            <span><b>Mode:</b> <span style='color:#FFB74D; font-weight:bold;'>${currentMode}</span></span>
            <span><b>Twilight Scale:</b> <span style='color:#AB47BC; font-weight:bold;'>${twilightScale}</span></span>
        </div>

        <table style='width:100%; color:#fff; text-align:center; margin-bottom:12px; border-collapse:collapse; background:#2A2A32; border-radius:6px; table-layout:fixed;'>
            <thead>
                <tr style='background:#333440; font-weight:bold; font-size:0.85em; color:#FFC107;'>
                    <th style='padding:8px; width:50%; border-right:1px solid #444; text-align:center;'>🌅 MORNING PERIOD ${mornStatusBadge}</th>
                    <th style='padding:8px; width:50%; text-align:center;'>🌆 EVENING PERIOD ${eveStatusBadge}</th>
                </tr>
            </thead>
            <tbody>
                <tr style='font-size:0.88em; border-top:1px solid #444;'>
                    <td style='padding:10px 6px; border-right:1px solid #444; vertical-align:middle;'>
                        <div style='margin-bottom:4px;'><b>ON:</b> <span style='color:${enableMorningSchedule != false ? "#81C784" : "#888"}; font-weight:bold;'>${mornOnDisp}</span></div>
                        <div style='margin-bottom:6px;'><b>OFF:</b> <span style='color:${enableMorningSchedule != false ? "#E57373" : "#888"}; font-weight:bold;'>${mornOffDisp}</span></div>
                        <div style='font-size:0.82em; color:#FFD54F; border-top:1px dashed #444; padding-top:4px;'>☀️ <b>Sunrise:</b> ${sunriseDisp}</div>
                    </td>
                    <td style='padding:10px 6px; vertical-align:middle;'>
                        <div style='margin-bottom:4px;'><b>ON:</b> <span style='color:${enableEveningSchedule != false ? "#81C784" : "#888"}; font-weight:bold;'>${eveOnDisp}</span></div>
                        <div style='margin-bottom:6px;'><b>OFF:</b> <span style='color:${enableEveningSchedule != false ? "#E57373" : "#888"}; font-weight:bold;'>${eveOffDisp}</span></div>
                        <div style='font-size:0.82em; color:#FFD54F; border-top:1px dashed #444; padding-top:4px;'>🌙 <b>Sunset:</b> ${sunsetDisp}</div>
                    </td>
                </tr>
            </tbody>
        </table>

        <table style='width:100%; color:#bbb; font-size:0.85em; border-collapse:collapse; margin-bottom:8px; table-layout:fixed;'>
            <tr style='border-bottom:1px solid #333;'>
                <td style='padding:5px 2px; width:50%;'>💡 <b>Illuminance:</b> ${currentIllum}</td>
                <td style='padding:5px 2px; width:50%;'>☀️ <b>Solar Altitude:</b> ${sunAltText}</td>
            </tr>
            <tr>
                <td style='padding:5px 2px; padding-top:6px;'>☁️ <b>Cloud Cover:</b> ${cloudPctText}</td>
                <td style='padding:5px 2px; padding-top:6px;'>🧭 <b>OWM Segment:</b> ${betwixtText}</td>
            </tr>
        </table>

        <div style='font-size:0.85em; color:#81D4FA; background:#121216; padding:6px 10px; border-radius:4px; margin-top:6px;'>
            <b>Next Event:</b> ${nextEvent}
        </div>
    </div>
    """
    return html
}

// Fixed Version Demarcation Trace Logging Helper
private String checkAndLogVersionDemarcation() {
    String previousVer = state.appVersion
    String currentVer = version()
    if (previousVer != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
    return previousVer
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    logTrace "updateAppLabel() -> Checking app label..."
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Advanced Porch Lighting Manager"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        logTrace "updateAppLabel() -> Updating app label from '${app.label}' to '${baseLabel}'"
        app.updateLabel(baseLabel)
    }
}

// Settings Hash Snapshot Helper
private String captureSettingsSnapshot() {
    logTrace "captureSettingsSnapshot() -> Generating settings MD5 hash..."
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn")) }
        .sort()

    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    String hash = java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
    logTrace "captureSettingsSnapshot() -> Hash resolved to: ${hash}"
    return hash
}

// Hubitat App Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing app v${version()} (${timeStamp()})..."
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize(true)
}

void updated() {
    String previousVersion = checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (previousVersion != version())

    if (settingsChanged || codeVersionChanged) {
        logInfo "Settings or code version modification detected (Version changed: ${codeVersionChanged}). Re-establishing subscriptions and schedules..."
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
    logTrace "initialize() -> Re-establishing event subscriptions and cron anchors..."
    updateAppLabel()

    unschedule()

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    }

    if (twilightDevice) {
        logTrace "initialize() -> Subscribing to Twilight Parser attributes..."
        subscribe(twilightDevice, "usedTwilightBegin", handleTwilightEvent)
        subscribe(twilightDevice, "usedTwilightEnd", handleTwilightEvent)
        subscribe(twilightDevice, "localCivilTwilightBegin", handleTwilightEvent)
        subscribe(twilightDevice, "localCivilTwilightEnd", handleTwilightEvent)
        subscribe(twilightDevice, "localNauticalTwilightBegin", handleTwilightEvent)
        subscribe(twilightDevice, "localNauticalTwilightEnd", handleTwilightEvent)
        subscribe(twilightDevice, "localAstronomicalTwilightBegin", handleTwilightEvent)
        subscribe(twilightDevice, "localAstronomicalTwilightEnd", handleTwilightEvent)
        subscribe(twilightDevice, "localSunrise", handleTwilightEvent)
        subscribe(twilightDevice, "localSunset", handleTwilightEvent)
    }

    if (turnOffOnSleepMode == true) {
        logTrace "initialize() -> Subscribing to location mode changes for Sleep Mode trigger..."
        subscribe(location, "mode", handleModeChange)
    }

    subscribe(location, "systemStart", handleHubReboot)

    schedule("0 2 0 * * ?", nightlyScheduleRefresh)
    logTrace "initialize() -> Anchored nightly refresh cron at 12:02 AM."

    buildAndScheduleEvents(null)
    reconcileStateOnBoot()
}

/* Debounced Twilight Event Handler */
def handleTwilightEvent(evt) {
    logTrace "handleTwilightEvent() -> Attribute update '${evt?.name}' received. Debouncing rebuild by 1s..."
    runIn(1, "debouncedBuildAndScheduleEvents", [overwrite: true])
}

def debouncedBuildAndScheduleEvents() {
    logDebug "Executing debounced schedule rebuild from Twilight Parser updates..."
    buildAndScheduleEvents(null)
}

/* Hub Reboot Recovery & Schedule-Aware Reconciliation (Streamlined to 10 Lines) */
def handleHubReboot(evt) {
    logInfo "Hub reboot / systemStart event detected. Re-evaluating schedule and active light state..."
    buildAndScheduleEvents(null)
    reconcileStateOnBoot()
}

private void reconcileStateOnBoot() {
    Date now = new Date()
    Map sched = getEvaluatedScheduleMap()

    Boolean insideMorningWindow = (enableMorningSchedule != false) && isInsideScheduleWindow(now, sched.mornOnDate, sched.mornOffDate)
    Boolean insideEveningWindow = (enableEveningSchedule != false) && isInsideScheduleWindow(now, sched.eveOnDate, sched.eveOffDate)

    if (insideMorningWindow || insideEveningWindow) {
        logInfo "Reboot Reconciliation: Active schedule window detected (Morning: ${insideMorningWindow}, Evening: ${insideEveningWindow}). Ensuring lights are ON."
        switches?.each { it.on() }
    } else {
        logInfo "Reboot Reconciliation: Outside active schedule windows. Ensuring lights are OFF."
        switches?.each { it.off() }
    }
}

def nightlyScheduleRefresh() {
    logInfo "Nightly schedule construction sequence initiated at 12:02 AM."
    logTrace "nightlyScheduleRefresh() -> Invoking buildAndScheduleEvents()..."
    buildAndScheduleEvents(null)
}

/* Hubitat Mode Event Handler for Early Sleep Cutoff Trigger */
def handleModeChange(evt) {
    logTrace "handleModeChange() -> Mode changed to: ${evt?.value}"
    if (turnOffOnSleepMode != true || !evt) return

    String currentMode = evt.value?.toLowerCase() ?: ""
    if (currentMode in ["sleeping", "sleep"]) {
        String switchState = switches ? (switches[0]?.currentValue("switch") ?: "off") : "off"
        if (switchState == "on") {
            logInfo "Sleep Mode triggered ('${evt.value}'): Turning lights OFF immediately ahead of scheduled cutoff."
            switches?.each { it.off() }
            sendNotificationMsg("Advanced Porch Lighting: Lights turned OFF (Sleep Mode trigger).")
            state.nextScheduledEventDesc = (enableMorningSchedule != false) ? "Morning ON at ${state.schedMornOnDisp ?: '--'}" : "None scheduled"
        } else {
            logDebug "Sleep Mode triggered ('${evt.value}'), but lights are already OFF."
        }
    }
}

/* Helper to convert positive/negative user inputs into lead time minutes before twilight */
private int getPositiveOffsetMins(def settingVal, int defaultFallback = 30) {
    if (settingVal == null || settingVal.toString().trim() == "") return defaultFallback
    try {
        int raw = settingVal.toString().replace("-", "").trim().toInteger()
        logTrace "getPositiveOffsetMins() -> Normalized input '${settingVal}' to ${raw} minutes."
        return raw
    } catch (Exception e) {
        logWarn "getPositiveOffsetMins() -> Failed to parse offset '${settingVal}', defaulting to ${defaultFallback}."
        return defaultFallback
    }
}

/* Helper to extract chosen Twilight Parser begin/end attribute timestamps */
private Map getTwilightTimestamps() {
    String choice = (twilightType ?: getActiveTwilightDriverChoiceKey()).toLowerCase()
    String beginAttr = "usedTwilightBegin"
    String endAttr   = "usedTwilightEnd"

    switch (choice) {
        case "civil":
            beginAttr = "localCivilTwilightBegin"
            endAttr   = "localCivilTwilightEnd"
            break
        case "nautical":
            beginAttr = "localNauticalTwilightBegin"
            endAttr   = "localNauticalTwilightEnd"
            break
        case "astronomical":
            beginAttr = "localAstronomicalTwilightBegin"
            endAttr   = "localAstronomicalTwilightEnd"
            break
        default:
            beginAttr = "usedTwilightBegin"
            endAttr   = "usedTwilightEnd"
            break
    }

    String beginStr = twilightDevice?.currentValue(beginAttr)
    String endStr   = twilightDevice?.currentValue(endAttr)

    logTrace "getTwilightTimestamps() -> [Choice: ${choice}] Resolved Begin (${beginAttr}): '${beginStr}' | End (${endAttr}): '${endStr}'"
    return [begin: beginStr, end: endStr]
}

/* Master Schedule Construction Engine */
def buildAndScheduleEvents(evt = null) {
    logTrace "buildAndScheduleEvents() -> Triggered via event [${evt ? evt.name : 'manual/init'}]"
    if (!twilightDevice) {
        logWarn "buildAndScheduleEvents() -> Aborted: Twilight Parser device not selected."
        return
    }

    logDebug "Rebuilding lighting schedule from Twilight Parser timestamps..."
    unschedule("executeMorningOn")
    unschedule("executeMorningOff")
    unschedule("executeEveningOn")
    unschedule("executeNightOff")

    Date now = new Date()
    Map<Date, String> scheduledEvents = [:]

    Map sched = getEvaluatedScheduleMap()

    // Populating state display variables to ensure logged executions never hit stale or null strings
    state.schedMornOnDisp  = sched.mornOn  ?: "--"
    state.schedMornOffDisp = sched.mornOff ?: "--"
    state.schedEveOnDisp   = sched.eveOn   ?: "--"
    state.schedEveOffDisp  = sched.eveOff  ?: "--"

    // 1. MORNING SCHEDULE CONSTRUCTION
    if (enableMorningSchedule != false) {
        if (sched.mornOnDate && sched.mornOnDate.after(now)) {
            runOnce(sched.mornOnDate, executeMorningOn)
            logInfo "Scheduled Morning ON for: ${sched.mornOn} (${sched.mornOnFactors})"
            scheduledEvents[sched.mornOnDate] = "Morning ON at ${sched.mornOn}"
        }

        if (sched.mornOffDate && sched.mornOffDate.after(now)) {
            runOnce(sched.mornOffDate, executeMorningOff)
            logInfo "Scheduled Morning OFF for: ${sched.mornOff} (${sched.mornOffFactors})"
            scheduledEvents[sched.mornOffDate] = "Morning OFF at ${sched.mornOff}"
        }
    } else {
        state.schedMornOnDisp = "DISABLED"
        state.schedMornOffDisp = "DISABLED"
    }

    // 2. EVENING SCHEDULE CONSTRUCTION
    if (enableEveningSchedule != false) {
        if (sched.eveOnDate && sched.eveOnDate.after(now)) {
            runOnce(sched.eveOnDate, executeEveningOn)
            logInfo "Scheduled Evening ON for: ${sched.eveOn} (${sched.eveOnFactors})"
            scheduledEvents[sched.eveOnDate] = "Evening ON at ${sched.eveOn}"
        }

        if (sched.eveOffDate && sched.eveOffDate.after(now)) {
            runOnce(sched.eveOffDate, executeNightOff)
            logInfo "Scheduled Evening Cutoff OFF for: ${sched.eveOff} (${sched.eveOffFactors})"
            scheduledEvents[sched.eveOffDate] = "Evening OFF at ${sched.eveOff}"
        }
    } else {
        state.schedEveOnDisp = "DISABLED"
        state.schedEveOffDisp = "DISABLED"
    }

    // Chronological Next Event Selection
    if (scheduledEvents) {
        Date earliestDate = scheduledEvents.keySet().min()
        state.nextScheduledEventDesc = scheduledEvents[earliestDate]
    } else {
        state.nextScheduledEventDesc = "None scheduled"
    }

    logTrace "buildAndScheduleEvents() -> Construction complete. Next Event: ${state.nextScheduledEventDesc}"
}

/* Explicit Execution Handlers */
def executeMorningOn() {
    logTrace "executeMorningOn() -> Entering handler..."
    logInfo "Scheduled Event Reached: Morning Turn-ON"
    switches?.each { it.on() }
    sendNotificationMsg("Advanced Porch Lighting: Morning lights turned ON.")
    state.nextScheduledEventDesc = "Morning OFF at ${state.schedMornOffDisp}"
}

def executeMorningOff() {
    logTrace "executeMorningOff() -> Entering handler..."
    logInfo "Scheduled Event Reached: Morning Turn-OFF"
    switches?.each { it.off() }
    sendNotificationMsg("Advanced Porch Lighting: Morning lights turned OFF.")
    state.nextScheduledEventDesc = (enableEveningSchedule != false) ? "Evening ON at ${state.schedEveOnDisp}" : "None scheduled"
}

def executeEveningOn() {
    logTrace "executeEveningOn() -> Entering handler..."
    logInfo "Scheduled Event Reached: Evening Turn-ON"
    switches?.each { it.on() }
    sendNotificationMsg("Advanced Porch Lighting: Evening lights turned ON.")
    state.nextScheduledEventDesc = "Evening OFF at ${state.schedEveOffDisp}"
}

def executeNightOff() {
    logTrace "executeNightOff() -> Entering handler..."
    logInfo "Scheduled Event Reached: Evening Cutoff Turn-OFF"
    switches?.each { it.off() }
    sendNotificationMsg("Advanced Porch Lighting: Evening cutoff lights turned OFF.")
    state.nextScheduledEventDesc = (enableMorningSchedule != false) ? "Morning ON at ${state.schedMornOnDisp}" : "None scheduled"
}

/* Highly Efficient Notification Dispatcher */
private void sendNotificationMsg(String msg) {
    if (!notifier) return
    logTrace "sendNotificationMsg() -> Sending message: '${msg}'"
    notifier.each { it.deviceNotification(msg) }
}

private Date parseDateString(String dateStr) {
    logTrace "parseDateString() -> Parsing '${dateStr}'"
    if (!dateStr) return null
    try {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.setTimeZone(location.timeZone)
        return sdf.parse(dateStr)
    } catch (Exception e) {
        logError "Failed to parse date string '${dateStr}': ${e.message}"
        return null
    }
}

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// Centralized Logging Engine
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