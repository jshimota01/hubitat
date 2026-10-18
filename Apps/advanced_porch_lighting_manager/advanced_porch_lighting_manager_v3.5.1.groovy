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
 *	v3.5.1    10/16/26    jshimota    Cross-Midnight Cutoff Anchor Fix: Hardened resolveEveningScheduleTime to evaluate yesterday's active evening cycle during post-midnight rebuilds so cross-midnight cutoffs anchor strictly to the active cycle date instead of rolling forward to tomorrow.
 *	v3.5.0    10/16/26    jshimota    Log Polish Refactor: Demoted routine schedule announcements, mode transition audits, pre-poll status checks, and nightly refresh logs to Debug level so Info logs strictly prioritize active light execution and key lifecycle events.
 *	v3.4.2    10/14/26    jshimota    Mode Transition & Cutoff Integrity Patch: Hardened mode-change handling to audit and repair evening schedules when transitioning out of Sleeping mode. Added verifyAndRepairEveningCutoff safety check to prevent dropped cutoff jobs. Enhanced trace logging across job schedules.
 *	v3.4.1    09/07/26    jshimota    GUI Polish: Rephrased top status board header label from 'Mode:' to 'Current Hub Mode:' for enhanced UI clarity.
 *	v3.4.0    09/07/26    jshimota    Production Hardening & Robustness Overhaul: Fixed stale Lux trigger state bug by resetting state flags on configuration updates. Fixed OWM missing data bug where null illuminance defaulted to 0.0 lx. Replaced Groovy truthiness fallbacks with explicit null checks for numeric 0 lx readings. Centralized getBoundedLuxShiftMins() and getSanitizedLuxThreshold() helpers. Updated GUI text to accurately specify 'cached OWM illuminance reading'. Decoupled dashboard ACTIVE badge evaluation from future schedule execution dates.
  *	v1.0.0    09/05/26    jshimota    Initial release built on standardized App Template v1.2.2.
  *
  *  [KEEP] for changelog history prior for v1.0.1 - v3.3.9 refer the the external changelog.txt file
  **/

static String version() { return '3.5.1' }
def timeStamp() { return "2026/10/16 10:15 AM" }

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
                      "• <b>Scope:</b> Applied to Morning ON and Evening ON turn-ON events.<br/>" +
                      "• <b>Behavior:</b> When enabled, heavy overcast (Cloud PCT) advances target turn-ON times earlier (e.g. turning lights on earlier before twilight begin or sunset to compensate for dark skies). Scales proportionally based on current cloud percentage (e.g. 50% cloud cover applies 50% of your maximum offset minutes)." +
                      "</div>"

            if (enableCloudAdjustment) {
                input name: "maxCloudShiftMins", type: "number", title: "Max Cloud Adjustment Offset (Minutes)", description: "Max minutes to advance ON time at 100% cloud cover", defaultValue: 15, required: true, submitOnChange: true
            }

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "enableLuxAdjustment", type: "bool", title: "Enable Lux Impact Adjustment?", description: "Adjusts Morning OFF and Evening ON based on dynamic pre-poll illuminance readings.", defaultValue: false, submitOnChange: true
            paragraph "<div style='background:#F8F9FA; padding:8px 12px; border-left:4px solid #FFB74D; margin-top:-6px; margin-bottom:8px; font-size:0.85em; color:#333;'>" +
                      "<b>Lux Impact Adjustment Behavior:</b><br/>" +
                      "• <b>Scope:</b> Applied to Morning OFF (extends coverage runtime) and Evening ON (advances turn-ON earlier).<br/>" +
                      "• <b>Pre-Poll Mechanism:</b> Lux is evaluated dynamically before target trigger times using the latest cached OWM illuminance reading. For Evening ON, pre-poll occurs 5 minutes before the prospective Lux ON time (Sunset - Lead - Cloud - Lux - 5m). For Morning OFF, pre-poll occurs 5 minutes before standard Morning OFF (Sunrise + Lead - 5m). If live illuminance is at or below threshold, Evening ON is advanced and Morning OFF is extended." +
                      "</div>"

            if (enableLuxAdjustment) {
                input name: "luxThreshold", type: "number", title: "Lux Impact Threshold (lx)", description: "If Lux falls below this value during pre-poll, adjust Evening ON earlier and extend Morning OFF window", defaultValue: 50, range: "0..100000", required: true, submitOnChange: true
                input name: "luxShiftMins", type: "number", title: "Max Lux Adjustment Offset (Minutes, Capped 0-30)", description: "Minutes to advance Evening ON / extend Morning OFF when Lux threshold is triggered (Valid Range: 0 to 30 minutes)", defaultValue: 10, range: "0..30", required: true, submitOnChange: true
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

/* Centralized Helper Utilities */

private int getBoundedLuxShiftMins() {
    return Math.min(Math.max(getPositiveOffsetMins(settings.luxShiftMins, 10), 0), 30)
}

private BigDecimal getSanitizedLuxThreshold() {
    if (settings.luxThreshold == null) return 50.0.toBigDecimal()
    try {
        BigDecimal val = settings.luxThreshold.toString().toBigDecimal()
        return (val < 0.0) ? 0.0.toBigDecimal() : val
    } catch (Exception e) {
        logWarn "getSanitizedLuxThreshold() -> Invalid luxThreshold input '${settings.luxThreshold}', defaulting to 50.0."
        return 50.0.toBigDecimal()
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
        Date now = new Date()

        Calendar onCal = Calendar.getInstance(location.timeZone)
        onCal.time = eveOnReferenceDate

        Calendar offCal = Calendar.getInstance(location.timeZone)
        offCal.time = unanchoredOff

        Boolean isCrossMidnight = (offCal.get(Calendar.HOUR_OF_DAY) < onCal.get(Calendar.HOUR_OF_DAY) || 
                                  (offCal.get(Calendar.HOUR_OF_DAY) == onCal.get(Calendar.HOUR_OF_DAY) && offCal.get(Calendar.MINUTE) <= onCal.get(Calendar.MINUTE)))

        if (isCrossMidnight) {
            // Check if 'now' is in the early morning hours after midnight belonging to yesterday's active evening cycle
            if (now.before(eveOnReferenceDate)) {
                Calendar yesterdayOnCal = Calendar.getInstance(location.timeZone)
                yesterdayOnCal.time = eveOnReferenceDate
                yesterdayOnCal.add(Calendar.DAY_OF_YEAR, -1)

                Calendar todayOffCal = Calendar.getInstance(location.timeZone)
                todayOffCal.time = now
                todayOffCal.set(Calendar.HOUR_OF_DAY, offCal.get(Calendar.HOUR_OF_DAY))
                todayOffCal.set(Calendar.MINUTE, offCal.get(Calendar.MINUTE))
                todayOffCal.set(Calendar.SECOND, 0)
                todayOffCal.set(Calendar.MILLISECOND, 0)

                // If yesterday's ON has occurred and today's early-morning cutoff has not yet arrived, retain today's cutoff
                if (now.after(yesterdayOnCal.time) && todayOffCal.time.after(now)) {
                    logTrace "resolveEveningScheduleTime() -> Retaining active cross-midnight cutoff for current early-morning window: ${todayOffCal.time.format('yyyy-MM-dd h:mm:ss a', location.timeZone)}"
                    return todayOffCal.time
                }
            }

            // Standard future cross-midnight anchor (+1 day relative to upcoming evening ON)
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

    if (unanchoredOff.before(new Date())) {
        return buildClockDate(timeInput, 1)
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

/* Helper to evaluate today's schedule window strictly for status dashboard badges */
private Map getTodayWindowBounds(Map sched) {
    Date mornOnToday = sched.mornOnDate ? buildClockDate(sched.mornOnDate, 0) : null
    Date mornOffToday = sched.mornOffDate ? buildClockDate(sched.mornOffDate, 0) : null
    Date eveOnToday = sched.eveOnDate ? buildClockDate(sched.eveOnDate, 0) : null
    Date eveOffToday = sched.eveOffDate ? resolveEveningScheduleTime(eveningCutoffTime ?: "00:00", eveOnToday) : null

    return [
        mornOn: mornOnToday, mornOff: mornOffToday,
        eveOn: eveOnToday, eveOff: eveOffToday
    ]
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
        mornOnDate: null, mornOffDate: null, eveOnDate: null, eveOffDate: null,
        mornLuxPrePollDate: null, eveLuxPrePollDate: null
    ]

    if (!twilightDevice) return sched

    Map twilightTimes = getTwilightTimestamps()
    Map envShifts = getDetailedEnvironmentalShifts()
    int boundedLuxMins = getBoundedLuxShiftMins()

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
                Boolean isLuxTriggered = state.mornLuxTriggered == true
                int extMins = (enableLuxAdjustment && isLuxTriggered) ? boundedLuxMins : 0
                sched.mornOffDate = new Date(defaultOffDate.time + (extMins * 60 * 1000))
                sched.mornOff = sched.mornOffDate.format("h:mm a", location.timeZone)
                
                List<String> mOffFactors = ["Sunrise (+${mornOffLeadMins}m)"]
                if (enableLuxAdjustment) {
                    if (isLuxTriggered) {
                        mOffFactors << "Lux Triggered (+${extMins}m)"
                    } else {
                        mOffFactors << "Lux Pending Check"
                    }
                }
                sched.mornOffFactors = mOffFactors.join(" + ")

                // Symmetrical Pre-poll anchor: 5 minutes prior to base Morning OFF (Sunrise + Lead)
                if (enableLuxAdjustment) {
                    sched.mornLuxPrePollDate = new Date(defaultOffDate.time - (5 * 60 * 1000))
                }
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
                int baseShift = leadMins + envShifts.cloudShift
                Date baseEveOnDate = new Date(sSetToday.time - (baseShift * 60 * 1000))

                Boolean isLuxTriggered = state.eveLuxTriggered == true
                int luxShift = (enableLuxAdjustment && isLuxTriggered) ? boundedLuxMins : 0
                sched.eveOnDate = new Date(baseEveOnDate.time - (luxShift * 60 * 1000))
                sched.eveOn = sched.eveOnDate.format("h:mm a", location.timeZone)

                List<String> eOnFactors = []
                if (leadMins > 0) eOnFactors << "Lead (-${leadMins}m)"
                if (envShifts.cloudShift > 0) eOnFactors << "Cloud (-${envShifts.cloudShift}m)"
                if (enableLuxAdjustment) {
                    if (isLuxTriggered) {
                        eOnFactors << "Lux Triggered (-${luxShift}m)"
                    } else {
                        eOnFactors << "Lux Pending Check"
                    }
                }
                sched.eveOnFactors = eOnFactors ? eOnFactors.join(" + ") : "Sunset Occurs"

                // Symmetrical Pre-poll anchor: (boundedLuxMins + 5) minutes prior to base Evening ON date (Sunset - Lead - Cloud)
                if (enableLuxAdjustment) {
                    sched.eveLuxPrePollDate = new Date(baseEveOnDate.time - ((boundedLuxMins + 5) * 60 * 1000))
                }
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

    return [cloudShift: cShift]
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
    Map todayBounds = getTodayWindowBounds(liveCalc)
    Boolean mornActive = (enableMorningSchedule != false) && isInsideScheduleWindow(now, todayBounds.mornOn, todayBounds.mornOff)
    Boolean eveActive  = (enableEveningSchedule != false) && isInsideScheduleWindow(now, todayBounds.eveOn, todayBounds.eveOff)

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
            <span><b>Current Hub Mode:</b> <span style='color:#FFB74D; font-weight:bold;'>${currentMode}</span></span>
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
    state.mornLuxTriggered = false
    state.eveLuxTriggered = false
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize(true)
}

void updated() {
    String previousVersion = checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

    // Reset Lux triggers unconditionally on settings/configuration update to prevent stale state inheritance
    state.mornLuxTriggered = false
    state.eveLuxTriggered = false

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

/* Hub Reboot Recovery & Schedule-Aware Reconciliation */
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
        verifyAndRepairEveningCutoff(sched)
    } else {
        logInfo "Reboot Reconciliation: Outside active schedule windows. Ensuring lights are OFF."
        switches?.each { it.off() }
    }
}

def nightlyScheduleRefresh() {
    logDebug "Nightly schedule construction sequence initiated at 12:02 AM."
    logTrace "nightlyScheduleRefresh() -> Invoking buildAndScheduleEvents()..."
    // Reset daily dynamic Lux triggers at midnight
    state.mornLuxTriggered = false
    state.eveLuxTriggered = false
    buildAndScheduleEvents(null)
}

/* Hubitat Mode Event Handler with Schedule Integrity Repair */
def handleModeChange(evt) {
    logTrace "handleModeChange() -> Mode changed to: ${evt?.value}"
    if (!evt) return

    String currentMode = evt.value?.toLowerCase() ?: ""
    Boolean isSleeping = currentMode in ["sleeping", "sleep"]

    if (isSleeping) {
        if (turnOffOnSleepMode == true) {
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
    } else {
        // Safe mode exit: verify active schedule window without forcing lights ON or resetting cutoff times
        logDebug "Mode transitioned away from Sleep Mode to '${evt.value}'. Auditing schedule integrity..."
        Map sched = getEvaluatedScheduleMap()
        Date now = new Date()
        Boolean insideEveningWindow = (enableEveningSchedule != false) && isInsideScheduleWindow(now, sched.eveOnDate, sched.eveOffDate)

        if (insideEveningWindow) {
            verifyAndRepairEveningCutoff(sched)
        } else {
            buildAndScheduleEvents(null)
        }
    }
}

/* Integrity Repair Assistant: Ensures valid future executeNightOff exists during evening window */
private void verifyAndRepairEveningCutoff(Map sched = null) {
    if (enableEveningSchedule == false) return

    Map evalSched = sched ?: getEvaluatedScheduleMap()
    Date now = new Date()

    if (evalSched.eveOffDate && evalSched.eveOffDate.after(now)) {
        logDebug "Auditing Evening Cutoff schedule: Cutoff queued for ${evalSched.eveOffDate.format('yyyy-MM-dd h:mm:ss a', location.timeZone)}"
        runOnce(evalSched.eveOffDate, executeNightOff, [overwrite: true])
        state.schedEveOffDisp = evalSched.eveOff ?: "--"
        state.nextScheduledEventDesc = "Evening OFF at ${state.schedEveOffDisp}"
        logDebug "Verified/Repaired Evening Cutoff scheduled for: ${evalSched.eveOffDate.format('h:mm:ss a', location.timeZone)}"
    } else {
        logWarn "verifyAndRepairEveningCutoff() -> Computed cutoff date '${evalSched.eveOffDate}' is in the past. Skipping repair."
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
    unschedule("evaluateDynamicMorningLux")
    unschedule("evaluateDynamicEveningLux")

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
            logDebug "Scheduled Morning ON for: ${sched.mornOn} (${sched.mornOnFactors})"
            scheduledEvents[sched.mornOnDate] = "Morning ON at ${sched.mornOn}"
        }

        if (sched.mornOffDate && sched.mornOffDate.after(now)) {
            runOnce(sched.mornOffDate, executeMorningOff)
            logDebug "Scheduled Morning OFF for: ${sched.mornOff} (${sched.mornOffFactors})"
            scheduledEvents[sched.mornOffDate] = "Morning OFF at ${sched.mornOff}"
        }

        if (enableLuxAdjustment && sched.mornLuxPrePollDate && sched.mornLuxPrePollDate.after(now)) {
            runOnce(sched.mornLuxPrePollDate, evaluateDynamicMorningLux)
            logDebug "Scheduled Morning Lux Pre-Poll for: ${sched.mornLuxPrePollDate.format('h:mm:ss a', location.timeZone)}"
        }
    } else {
        state.schedMornOnDisp = "DISABLED"
        state.schedMornOffDisp = "DISABLED"
    }

    // 2. EVENING SCHEDULE CONSTRUCTION
    if (enableEveningSchedule != false) {
        if (sched.eveOnDate && sched.eveOnDate.after(now)) {
            runOnce(sched.eveOnDate, executeEveningOn)
            logDebug "Scheduled Evening ON for: ${sched.eveOn} (${sched.eveOnFactors})"
            scheduledEvents[sched.eveOnDate] = "Evening ON at ${sched.eveOn}"
        }

        if (sched.eveOffDate && sched.eveOffDate.after(now)) {
            runOnce(sched.eveOffDate, executeNightOff)
            logDebug "Scheduled Evening Cutoff OFF for: ${sched.eveOffDate.format('yyyy-MM-dd h:mm:ss a', location.timeZone)} (${sched.eveOffFactors})"
            scheduledEvents[sched.eveOffDate] = "Evening OFF at ${sched.eveOff}"
        }

        if (enableLuxAdjustment && sched.eveLuxPrePollDate && sched.eveLuxPrePollDate.after(now)) {
            runOnce(sched.eveLuxPrePollDate, evaluateDynamicEveningLux)
            logDebug "Scheduled Evening Lux Pre-Poll for: ${sched.eveLuxPrePollDate.format('h:mm:ss a', location.timeZone)}"
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

/* Dynamic Lux Pre-Poll Handlers */
def evaluateDynamicMorningLux() {
    logDebug "Executing Morning Lux Pre-Poll check..."
    if (!enableLuxAdjustment || !owmDevice) return

    try {
        def rawLux = owmDevice.currentValue("currentIlluminance")
        if (rawLux == null) {
            rawLux = owmDevice.currentValue("illuminance")
        }

        if (rawLux == null) {
            logWarn "Morning Lux Pre-Poll: Illuminance metric unavailable from OWM device. Retaining standard schedule."
            state.mornLuxTriggered = false
            buildAndScheduleEvents(null)
            return
        }

        BigDecimal currentLux = rawLux.toString().toBigDecimal()
        BigDecimal targetLux = getSanitizedLuxThreshold()

        if (currentLux <= targetLux) {
            logDebug "Morning Lux Pre-Poll: Illuminance (${currentLux} lx) <= Threshold (${targetLux} lx). Extending Morning OFF runtime."
            state.mornLuxTriggered = true
        } else {
            logDebug "Morning Lux Pre-Poll: Illuminance (${currentLux} lx) > Threshold (${targetLux} lx). Proceeding with standard Morning OFF."
            state.mornLuxTriggered = false
        }
        buildAndScheduleEvents(null)
    } catch (Exception e) {
        logError "evaluateDynamicMorningLux() -> Exception during illuminance inspection: ${e.message}"
    }
}

def evaluateDynamicEveningLux() {
    logDebug "Executing Evening Lux Pre-Poll check..."
    if (!enableLuxAdjustment || !owmDevice) return

    try {
        def rawLux = owmDevice.currentValue("currentIlluminance")
        if (rawLux == null) {
            rawLux = owmDevice.currentValue("illuminance")
        }

        if (rawLux == null) {
            logWarn "Evening Lux Pre-Poll: Illuminance metric unavailable from OWM device. Retaining standard schedule."
            state.eveLuxTriggered = false
            buildAndScheduleEvents(null)
            return
        }

        BigDecimal currentLux = rawLux.toString().toBigDecimal()
        BigDecimal targetLux = getSanitizedLuxThreshold()

        if (currentLux <= targetLux) {
            logDebug "Evening Lux Pre-Poll: Illuminance (${currentLux} lx) <= Threshold (${targetLux} lx). Advancing Evening ON time earlier."
            state.eveLuxTriggered = true
        } else {
            logDebug "Evening Lux Pre-Poll: Illuminance (${currentLux} lx) > Threshold (${targetLux} lx). Proceeding with standard Evening ON."
            state.eveLuxTriggered = false
        }
        buildAndScheduleEvents(null)
    } catch (Exception e) {
        logError "evaluateDynamicEveningLux() -> Exception during illuminance inspection: ${e.message}"
    }
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
    
    // Audit Cutoff integrity directly upon Evening ON execution
    Map sched = getEvaluatedScheduleMap()
    verifyAndRepairEveningCutoff(sched)
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
        app.updateSetting("logDebugEnable",[type: "bool", value: false])
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