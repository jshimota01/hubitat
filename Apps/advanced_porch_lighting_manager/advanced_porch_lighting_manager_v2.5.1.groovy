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
 *	v2.5.1    09/06/26    jshimota    GUI Expansion: Added explicit 'Morning Off - Extend Time after Sunrise Occurs' and 'Evening On - Lead Time before Sunset Occurs' offset inputs to replace hardcoded 30-minute defaults. Updated GUI factors and calculation engines.
 *	v2.5.0    09/06/26    jshimota    Architectural Correction: Re-anchored Morning OFF to Sunrise Occurs (+30m default + Lux extension) and Evening ON to Sunset Occurs (-30m default - Lead - Cloud). Updated GUI preview labels and logic across schedule construction engines.
 *	v2.4.2    09/06/26    jshimota    GUI & Logic Refinement: Restricted Lux impact exclusively to Morning OFF extension, renamed factor to 'Lux', changed 'Sunrise:' to 'Sunrise Occurs:', renamed header to 'Twilight Scale', updated 'Used (Default)' to 'Used (Civil)', and embedded Sunrise/Sunset timestamps directly into the main status dashboard columns.
 *	v2.4.1    09/06/26    jshimota    Logic Adjustment: Corrected Lux Impact logic so low lux extends lighting coverage by adding positive minutes to Morning OFF / Evening ON windows rather than reducing runtime.
 *	v2.4.0    09/06/26    jshimota    GUI & Logic Overhaul: Added specific ON time overrides for Morning and Evening. Placed factor indicators directly beside the specific ON or OFF time they impact. Added explicit OVERRIDE badges when hard-coded times bypass twilight calculations.
 *	v2.3.6    09/06/26    jshimota    GUI Polish: Rephrased Twilight Begin and Twilight End labels to 'Twilight Begins' and 'Twilight Ends' across descriptions and preview cards for more natural readability.
 *	v2.3.5    09/06/26    jshimota    GUI Refinement: Placed active impact factors inline on the Calculated Morning and Evening time lines for immediate visibility into offset contributors.
 *	v2.3.4    09/06/26    jshimota    Bug check: Fixed getNormalizedOffsetMins fallback parsing bug where typed values fell back to default 30. Added explicit environmental shift indicators in Section 2 previews.
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

static String version() { return '2.5.1' }
def timeStamp() { return "2026/09/06 02:30 PM" }

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

        /* Real-Time Environment & Schedule Status Board */
        section() {
            paragraph getStatusBoardHtml()
        }

        /* 1. Device Connections */
        section("<b>1. Device Connections</b>", hideable: true, hidden: true) {
            input name: "switches", type: "capability.switch", title: "Select Switches to Control", multiple: true, required: true
            input name: "twilightDevice", type: "capability.sensor", title: "Select Twilight Parser Device (Schedule Source)", multiple: false, required: true, submitOnChange: true
            input name: "owmDevice", type: "capability.sensor", title: "Select OpenWeatherMap Device (Diagnostic Context)", multiple: false, required: false, submitOnChange: true
            input name: "notifier", type: "capability.notification", title: "Select Notification Device (Optional)", multiple: true, required: false
        }

        /* Calculate Preview Times for GUI */
        Map calculatedPreview = calculateSchedulePreview()

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
            }
        }

        /* 3. Optional Schedule Adjustments */
        section("<b>3. Optional Schedule Adjustments</b>", hideable: true, hidden: true) {
            input name: "turnOffOnSleepMode", type: "bool", title: "Turn OFF lights immediately when Hub Mode changes to 'Sleeping'?", description: "Overrides scheduled evening cutoff if triggered earlier", defaultValue: true, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "twilightType", type: "enum", title: "Twilight Parser Calculation Reference", 
                  options: ["used": "Used (Civil)", "civil": "Civil", "nautical": "Nautical", "astronomical": "Astronomical"], 
                  defaultValue: "used", required: true, submitOnChange: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "enableCloudAdjustment", type: "bool", title: "Enable Cloud Cover Adjustment?", description: "Advances ON times earlier on heavy overcast days based on OWM Cloud PCT", defaultValue: false, submitOnChange: true
            if (enableCloudAdjustment) {
                input name: "maxCloudShiftMins", type: "number", title: "Max Cloud Adjustment Offset (Minutes)", description: "Max minutes to advance ON time at 100% cloud cover", defaultValue: 15, required: true, submitOnChange: true
            }

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "enableLuxAdjustment", type: "bool", title: "Enable Lux Impact Adjustment?", description: "Extends Morning OFF time when OWM illuminance falls below threshold", defaultValue: false, submitOnChange: true
            if (enableLuxAdjustment) {
                input name: "luxThreshold", type: "number", title: "Lux Impact Threshold (lx)", description: "If Lux falls below this value, extend Morning OFF coverage window", defaultValue: 50, required: true, submitOnChange: true
                input name: "luxShiftMins", type: "number", title: "Max Lux Adjustment Offset (Minutes)", description: "Minutes to extend Morning OFF time when Lux threshold is triggered", defaultValue: 10, required: true, submitOnChange: true
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

/* GUI Schedule Preview Engine */
private Map calculateSchedulePreview() {
    Map preview = [
        rawMornBegin: "--", rawSunrise: "--", rawSunset: "--", rawEveEnd: "--",
        mornOn: "--", mornOnFactors: "None", mornOff: "--", mornOffDefault: "--", mornOffFactors: "Sunrise (+30m)",
        eveOn: "--", eveOnFactors: "None", eveOff: "--", eveOffFactors: "Default (12:00 AM)"
    ]

    if (!twilightDevice) return preview

    Map twilightTimes = getTwilightTimestamps()
    Map envShifts = getDetailedEnvironmentalShifts()

    // Raw Sunrise & Sunset Extraction
    String sunriseStr = twilightDevice.currentValue("localSunrise")
    if (sunriseStr) {
        Date sDate = parseDateString(sunriseStr)
        if (sDate) preview.rawSunrise = sDate.format("h:mm a", location.timeZone)
    }

    String sunsetStr = twilightDevice.currentValue("localSunset")
    if (sunsetStr) {
        Date sSetDate = parseDateString(sunsetStr)
        if (sSetDate) preview.rawSunset = sSetDate.format("h:mm a", location.timeZone)
    }

    // MORNING PREVIEW CONSTRUCTION
    if (twilightTimes.begin) {
        Date mornTwilightDate = parseDateString(twilightTimes.begin)
        if (mornTwilightDate) {
            preview.rawMornBegin = mornTwilightDate.format("h:mm a", location.timeZone)

            // Morning ON: Anchored to Twilight Begins
            if (mornOnTime) {
                Date explicitOnDate = parseIsoTimeToTodayDate(mornOnTime)
                preview.mornOn = explicitOnDate ? explicitOnDate.format("h:mm a", location.timeZone) : "--"
                preview.mornOnFactors = "OVERRIDE: Hard-Coded Time"
            } else {
                int leadMins = getNormalizedOffsetMins(settings.mornOffset, 30)
                int totalMornOnShift = leadMins + envShifts.cloudShift
                Date mornRunTime = new Date(mornTwilightDate.time - (totalMornOnShift * 60 * 1000))
                preview.mornOn = mornRunTime.format("h:mm a", location.timeZone)

                List<String> mOnFactors = []
                if (leadMins > 0) mOnFactors << "Lead (-${leadMins}m)"
                if (envShifts.cloudShift > 0) mOnFactors << "Cloud (-${envShifts.cloudShift}m)"
                preview.mornOnFactors = mOnFactors ? mOnFactors.join(" + ") : "Twilight Begins"
            }
        }
    }

    // Morning OFF: Anchored to Sunrise Occurs
    if (sunriseStr) {
        Date sDate = parseDateString(sunriseStr)
        if (sDate) {
            int mornOffLeadMins = getNormalizedOffsetMins(settings.mornOffOffset, 30)
            Date defaultOffDate = new Date(sDate.time + (mornOffLeadMins * 60 * 1000))
            preview.mornOffDefault = defaultOffDate.format("h:mm a", location.timeZone)

            if (morningOffTime) {
                Date mornOffDate = parseIsoTimeToTodayDate(morningOffTime)
                preview.mornOff = mornOffDate ? mornOffDate.format("h:mm a", location.timeZone) : preview.mornOffDefault
                preview.mornOffFactors = "OVERRIDE: Hard-Coded Time"
            } else {
                int extMins = envShifts.luxShift
                Date finalMornOffDate = new Date(defaultOffDate.time + (extMins * 60 * 1000))
                preview.mornOff = finalMornOffDate.format("h:mm a", location.timeZone)
                
                List<String> mOffFactors = ["Sunrise (+${mornOffLeadMins}m)"]
                if (extMins > 0) mOffFactors << "Lux (+${extMins}m)"
                preview.mornOffFactors = mOffFactors.join(" + ")
            }
        }
    }

    // EVENING PREVIEW CONSTRUCTION
    if (twilightTimes.end) {
        Date eveTwilightDate = parseDateString(twilightTimes.end)
        if (eveTwilightDate) preview.rawEveEnd = eveTwilightDate.format("h:mm a", location.timeZone)
    }

    // Evening ON: Anchored to Sunset Occurs
    if (sunsetStr) {
        Date sSetDate = parseDateString(sunsetStr)
        if (sSetDate) {
            if (eveOnTime) {
                Date explicitEveOnDate = parseIsoTimeToTodayDate(eveOnTime)
                preview.eveOn = explicitEveOnDate ? explicitEveOnDate.format("h:mm a", location.timeZone) : "--"
                preview.eveOnFactors = "OVERRIDE: Hard-Coded Time"
            } else {
                int leadMins = getNormalizedOffsetMins(settings.eveOffset, 30)
                int totalEveShift = leadMins + envShifts.cloudShift
                Date eveRunTime = new Date(sSetDate.time - (totalEveShift * 60 * 1000))
                preview.eveOn = eveRunTime.format("h:mm a", location.timeZone)

                List<String> eOnFactors = []
                if (leadMins > 0) eOnFactors << "Lead (-${leadMins}m)"
                if (envShifts.cloudShift > 0) eOnFactors << "Cloud (-${envShifts.cloudShift}m)"
                preview.eveOnFactors = eOnFactors ? eOnFactors.join(" + ") : "Sunset Occurs"
            }
        }
    }

    // Evening OFF: Anchored to Cutoff Time
    String offTimeIso = eveningCutoffTime ?: "00:00"
    Date eveOffDate = parseIsoTimeToTodayDate(offTimeIso)
    preview.eveOff = eveOffDate ? eveOffDate.format("h:mm a", location.timeZone) : "12:00 AM"
    preview.eveOffFactors = eveningCutoffTime ? "OVERRIDE: Hard-Coded Time" : "Default (12:00 AM)"

    return preview
}

/* Detailed Environmental Shift Breakdown Engine */
private Map getDetailedEnvironmentalShifts() {
    int cShift = 0
    int lShift = 0

    if (enableCloudAdjustment && owmDevice) {
        BigDecimal cloudPct = owmDevice.currentValue("currentCloudPCT")?.toBigDecimal() ?: 0.0
        int maxShift = (maxCloudShiftMins != null) ? maxCloudShiftMins.toInteger() : 15
        cShift = Math.round((cloudPct / 100.0) * maxShift) as int
    }

    if (enableLuxAdjustment && owmDevice) {
        BigDecimal currentLux = owmDevice.currentValue("currentIlluminance")?.toBigDecimal() ?: 0.0
        BigDecimal targetLux = (luxThreshold != null) ? luxThreshold.toBigDecimal() : 50.0
        if (currentLux <= targetLux) {
            lShift = (luxShiftMins != null) ? luxShiftMins.toInteger() : 10
        }
    }

    return [cloudShift: cShift, luxShift: lShift]
}

/* Status Dashboard HTML Generator */
String getStatusBoardHtml() {
    logTrace "getStatusBoardHtml() -> Generating live environment dashboard..."
    String switchState = switches ? (switches[0].currentValue("switch") ?: "off") : "unknown"
    String badgeColor  = (switchState == "on") ? "#2e7d32" : "#757575"
    String badgeText   = (switchState == "on") ? "LIGHTS ON 💡" : "LIGHTS OFF 🌙"

    // OWM Diagnostic Attributes
    String betwixtText   = owmDevice ? (owmDevice.currentValue("betwixt") ?: "N/A") : "N/A"
    String sunAltText    = owmDevice ? (owmDevice.currentValue("currentSunAltitudeText") ?: "--°") : "--°"
    String sunAzText     = owmDevice ? (owmDevice.currentValue("currentSunAzimuthText") ?: "--°") : "--°"
    String cloudPctText  = owmDevice ? "${owmDevice.currentValue('currentCloudPCT') ?: 0}%" : "N/A"
    String currentIllum  = owmDevice ? (owmDevice.currentValue("currentIlluminanceText") ?: "${owmDevice.currentValue('illuminance') ?: 0} lx") : "N/A"
    String currentMode   = location.mode ?: "Unknown"

    // Raw Twilight Parser Astronomical Values
    String sunriseDisp = "--"
    String sunsetDisp  = "--"
    if (twilightDevice) {
        String sRise = twilightDevice.currentValue("localSunrise")
        if (sRise) {
            Date d = parseDateString(sRise)
            if (d) sunriseDisp = d.format("h:mm a", location.timeZone)
        }
        String sSet = twilightDevice.currentValue("localSunset")
        if (sSet) {
            Date d = parseDateString(sSet)
            if (d) sunsetDisp = d.format("h:mm a", location.timeZone)
        }
    }

    // Twilight Calculation Reference Text
    String twilightScale = (twilightType ?: "used").capitalize()
    if (twilightScale == "Used") twilightScale = "Used (Civil)"

    // Internal Lighting State Determination
    String lightingState = "NIGHTTIME"
    if (owmDevice) {
        String isDay = owmDevice.currentValue("currentIsDay") ?: "false"
        if (isDay == "true") {
            lightingState = "DAYTIME"
        } else if (betwixtText.contains("twilight")) {
            lightingState = "TWILIGHT"
        }
    }

    String mornOnDisp  = (enableMorningSchedule != false) ? (state.schedMornOnDisp ?: "--") : "DISABLED"
    String mornOffDisp = (enableMorningSchedule != false) ? (state.schedMornOffDisp ?: "--") : "DISABLED"
    String eveOnDisp   = (enableEveningSchedule != false) ? (state.schedEveOnDisp ?: "--") : "DISABLED"
    String eveOffDisp  = (enableEveningSchedule != false) ? (state.schedEveOffDisp ?: "--") : "DISABLED"
    String nextEvent   = state.nextScheduledEventDesc ?: "None scheduled"

    String html = """
    <div style='background-color:#1e1e24; color:#ffffff; padding:12px; border-radius:8px; font-family:sans-serif;'>
        <div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;'>
            <span style='font-weight:bold; font-size:1.05em;'>Porch Lighting Manager</span>
            <span style='background-color:${badgeColor}; color:#fff; padding:3px 8px; border-radius:4px; font-weight:bold; font-size:0.85em;'>${badgeText}</span>
        </div>

        <!-- Top Header Status Line -->
        <div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; font-size:0.85em; color:#ddd; background:#121216; padding:6px 8px; border-radius:4px;'>
            <span><b>State:</b> <span style='color:#00BCD4; font-weight:bold;'>${lightingState}</span></span>
            <span><b>Mode:</b> <span style='color:#FFB74D; font-weight:bold;'>${currentMode}</span></span>
            <span><b>Twilight Scale:</b> <span style='color:#AB47BC; font-weight:bold;'>${twilightScale}</span></span>
        </div>

        <!-- Aligned Schedule Timetable Grid with Embedded Sunrise/Sunset -->
        <table style='width:100%; color:#fff; text-align:center; margin-bottom:12px; border-collapse:collapse; background:#2A2A32; border-radius:6px; table-layout:fixed;'>
            <thead>
                <tr style='background:#333440; font-weight:bold; font-size:0.85em; color:#FFC107;'>
                    <th style='padding:8px; width:50%; border-right:1px solid #444; text-align:center;'>🌅 MORNING PERIOD</th>
                    <th style='padding:8px; width:50%; text-align:center;'>🌆 EVENING PERIOD</th>
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

        <!-- OWM Environmental Diagnostics Grid -->
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

// Single-Shot Version Demarcation Trace Logging Helper Routine
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
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
    checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (state.appVersion != version())

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
    checkAndLogVersionDemarcation()
    logTrace "initialize() -> Re-establishing event subscriptions and cron anchors..."
    updateAppLabel()

    // Completely clear all lingering jobs before rebuilding schedule matrix
    unschedule()

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    }

    // Subscribe exclusively to Twilight Parser attributes
    if (twilightDevice) {
        logTrace "initialize() -> Subscribing to Twilight Parser attributes..."
        subscribe(twilightDevice, "usedTwilightBegin", buildAndScheduleEvents)
        subscribe(twilightDevice, "usedTwilightEnd", buildAndScheduleEvents)
        subscribe(twilightDevice, "localCivilTwilightBegin", buildAndScheduleEvents)
        subscribe(twilightDevice, "localCivilTwilightEnd", buildAndScheduleEvents)
        subscribe(twilightDevice, "localNauticalTwilightBegin", buildAndScheduleEvents)
        subscribe(twilightDevice, "localNauticalTwilightEnd", buildAndScheduleEvents)
        subscribe(twilightDevice, "localAstronomicalTwilightBegin", buildAndScheduleEvents)
        subscribe(twilightDevice, "localAstronomicalTwilightEnd", buildAndScheduleEvents)
        subscribe(twilightDevice, "localSunrise", buildAndScheduleEvents)
        subscribe(twilightDevice, "localSunset", buildAndScheduleEvents)
    }

    // Subscribe to Hubitat Location Mode changes for Sleep Mode monitoring
    if (turnOffOnSleepMode == true) {
        logTrace "initialize() -> Subscribing to location mode changes for Sleep Mode trigger..."
        subscribe(location, "mode", handleModeChange)
    }

    // Schedule Daily Midnight Schedule Construction
    schedule("0 2 0 * * ?", nightlyScheduleRefresh)
    logTrace "initialize() -> Anchored nightly refresh cron at 12:02 AM."

    // Initial Schedule Construction
    buildAndScheduleEvents(null)
}

def nightlyScheduleRefresh() {
    logInfo "Nightly schedule construction sequence initiated at 12:02 AM."
    logTrace "nightlyScheduleRefresh() -> Invoking buildAndScheduleEvents()..."
    buildAndScheduleEvents(null)
}

/* Hubitat Mode Event Handler for Sleep Trigger */
def handleModeChange(evt) {
    logTrace "handleModeChange() -> Mode changed to: ${evt?.value}"
    if (turnOffOnSleepMode != true || !evt) return

    String currentMode = evt.value?.toLowerCase() ?: ""
    if (currentMode in ["sleeping", "sleep"]) {
        String switchState = switches ? (switches[0].currentValue("switch") ?: "off") : "off"
        if (switchState == "on") {
            logInfo "Sleep Mode triggered ('${evt.value}'): Turning lights OFF immediately ahead of scheduled cutoff."
            switches?.off()
            sendNotificationMsg("Advanced Porch Lighting: Lights turned OFF (Sleep Mode trigger).")
            state.nextScheduledEventDesc = (enableMorningSchedule != false) ? "Morning ON at ${state.schedMornOnDisp}" : "None scheduled"
        } else {
            logDebug "Sleep Mode triggered ('${evt.value}'), but lights are already OFF."
        }
    }
}

/* Helper to convert positive/negative user inputs into lead time minutes before twilight */
private int getNormalizedOffsetMins(def settingVal, int defaultFallback = 30) {
    if (settingVal == null || settingVal.toString().trim() == "") return defaultFallback
    try {
        int raw = settingVal.toString().replace("-", "").trim().toInteger()
        logTrace "getNormalizedOffsetMins() -> Normalized input '${settingVal}' to ${raw} minutes."
        return raw
    } catch (Exception e) {
        logWarn "getNormalizedOffsetMins() -> Failed to parse offset '${settingVal}', defaulting to ${defaultFallback}."
        return defaultFallback
    }
}

/* Helper to extract chosen Twilight Parser begin/end attribute timestamps */
private Map getTwilightTimestamps() {
    String choice = (twilightType ?: "used").toLowerCase()
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
        case "used":
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

/* Master Schedule Construction Engine (Consumes Twilight Parser Timestamps) */
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
    String upcomingEventDesc = "None scheduled"

    Map twilightTimes = getTwilightTimestamps()
    Map envShifts = getDetailedEnvironmentalShifts()

    String sunriseStr = twilightDevice.currentValue("localSunrise")
    String sunsetStr  = twilightDevice.currentValue("localSunset")

    // 1. MORNING SCHEDULE CONSTRUCTION
    if (enableMorningSchedule != false) {
        // Morning ON Evaluation (Twilight Begins)
        if (mornOnTime) {
            Date explicitOnDate = parseIsoTimeToTodayDate(mornOnTime)
            if (explicitOnDate) {
                state.schedMornOnDisp = explicitOnDate.format("h:mm a", location.timeZone)
                if (explicitOnDate.after(now)) {
                    runOnce(explicitOnDate, executeMorningOn)
                    logInfo "Scheduled Specific Morning ON for: ${state.schedMornOnDisp} (Hard-Coded Override)"
                    if (upcomingEventDesc == "None scheduled") upcomingEventDesc = "Morning ON at ${state.schedMornOnDisp}"
                }
            }
        } else if (twilightTimes.begin) {
            Date mornTwilightDate = parseDateString(twilightTimes.begin)
            if (mornTwilightDate) {
                int leadMins = getNormalizedOffsetMins(settings.mornOffset, 30) + envShifts.cloudShift
                Date mornRunTime = new Date(mornTwilightDate.time - (leadMins * 60 * 1000))
                state.schedMornOnDisp = mornRunTime.format("h:mm a", location.timeZone)

                if (mornRunTime.after(now)) {
                    runOnce(mornRunTime, executeMorningOn)
                    logInfo "Scheduled Twilight Morning ON for: ${state.schedMornOnDisp} (${leadMins} mins lead time before twilight)"
                    if (upcomingEventDesc == "None scheduled") upcomingEventDesc = "Morning ON at ${state.schedMornOnDisp}"
                }
            }
        }

        // Morning OFF Evaluation (Sunrise Occurs)
        if (morningOffTime) {
            Date mornOffDate = parseIsoTimeToTodayDate(morningOffTime)
            if (mornOffDate) {
                state.schedMornOffDisp = mornOffDate.format("h:mm a", location.timeZone)
                if (mornOffDate.after(now)) {
                    runOnce(mornOffDate, executeMorningOff)
                    logInfo "Scheduled Morning Cutoff OFF for: ${state.schedMornOffDisp}"
                    if (upcomingEventDesc == "None scheduled") upcomingEventDesc = "Morning OFF at ${state.schedMornOffDisp}"
                }
            }
        } else if (sunriseStr) {
            Date sDate = parseDateString(sunriseStr)
            if (sDate) {
                int mornOffLeadMins = getNormalizedOffsetMins(settings.mornOffOffset, 30)
                int extMins = envShifts.luxShift
                Date defaultMornOffDate = new Date(sDate.time + ((mornOffLeadMins + extMins) * 60 * 1000))
                state.schedMornOffDisp = defaultMornOffDate.format("h:mm a", location.timeZone)
                if (defaultMornOffDate.after(now)) {
                    runOnce(defaultMornOffDate, executeMorningOff)
                    logInfo "Scheduled Default Morning OFF for: ${state.schedMornOffDisp} (+${mornOffLeadMins}m post-sunrise + ${extMins}m lux extension)"
                    if (upcomingEventDesc == "None scheduled") upcomingEventDesc = "Morning OFF at ${state.schedMornOffDisp}"
                }
            }
        }
    } else {
        state.schedMornOnDisp = "DISABLED"
        state.schedMornOffDisp = "DISABLED"
    }

    // 2. EVENING SCHEDULE CONSTRUCTION
    if (enableEveningSchedule != false) {
        // Evening ON Evaluation (Sunset Occurs)
        if (eveOnTime) {
            Date explicitEveOnDate = parseIsoTimeToTodayDate(eveOnTime)
            if (explicitEveOnDate) {
                state.schedEveOnDisp = explicitEveOnDate.format("h:mm a", location.timeZone)
                if (explicitEveOnDate.after(now)) {
                    runOnce(explicitEveOnDate, executeEveningOn)
                    logInfo "Scheduled Specific Evening ON for: ${state.schedEveOnDisp} (Hard-Coded Override)"
                    if (upcomingEventDesc == "None scheduled") upcomingEventDesc = "Evening ON at ${state.schedEveOnDisp}"
                }
            }
        } else if (sunsetStr) {
            Date sSetDate = parseDateString(sunsetStr)
            if (sSetDate) {
                int leadMins = getNormalizedOffsetMins(settings.eveOffset, 30) + envShifts.cloudShift
                Date eveRunTime = new Date(sSetDate.time - (leadMins * 60 * 1000))
                state.schedEveOnDisp = eveRunTime.format("h:mm a", location.timeZone)

                if (eveRunTime.after(now)) {
                    runOnce(eveRunTime, executeEveningOn)
                    logInfo "Scheduled Twilight Evening ON for: ${state.schedEveOnDisp} (${leadMins} mins lead time before sunset)"
                    if (upcomingEventDesc == "None scheduled") upcomingEventDesc = "Evening ON at ${state.schedEveOnDisp}"
                }
            }
        }

        // Evening OFF Evaluation
        String offTimeIso = eveningCutoffTime ?: "00:00"
        Date eveOffDate = parseIsoTimeToTodayDate(offTimeIso)
        if (eveOffDate) {
            state.schedEveOffDisp = eveOffDate.format("h:mm a", location.timeZone)
            if (eveOffDate.after(now)) {
                runOnce(eveOffDate, executeNightOff)
                logInfo "Scheduled Evening Cutoff OFF for: ${state.schedEveOffDisp}"
                if (upcomingEventDesc == "None scheduled") upcomingEventDesc = "Evening OFF at ${state.schedEveOffDisp}"
            }
        }
    } else {
        state.schedEveOnDisp = "DISABLED"
        state.schedEveOffDisp = "DISABLED"
    }

    state.nextScheduledEventDesc = upcomingEventDesc
    logTrace "buildAndScheduleEvents() -> Construction complete. Next Event: ${upcomingEventDesc}"
}

/* Explicit Execution Handlers */
def executeMorningOn() {
    logTrace "executeMorningOn() -> Entering handler..."
    logInfo "Scheduled Event Reached: Morning Turn-ON"
    switches?.on()
    sendNotificationMsg("Advanced Porch Lighting: Morning lights turned ON.")
    state.nextScheduledEventDesc = "Morning OFF at ${state.schedMornOffDisp}"
}

def executeMorningOff() {
    logTrace "executeMorningOff() -> Entering handler..."
    logInfo "Scheduled Event Reached: Morning Turn-OFF"
    switches?.off()
    sendNotificationMsg("Advanced Porch Lighting: Morning lights turned OFF.")
    state.nextScheduledEventDesc = (enableEveningSchedule != false) ? "Evening ON at ${state.schedEveOnDisp}" : "None scheduled"
}

def executeEveningOn() {
    logTrace "executeEveningOn() -> Entering handler..."
    logInfo "Scheduled Event Reached: Evening Turn-ON"
    switches?.on()
    sendNotificationMsg("Advanced Porch Lighting: Evening lights turned ON.")
    state.nextScheduledEventDesc = "Evening OFF at ${state.schedEveOffDisp}"
}

def executeNightOff() {
    logTrace "executeNightOff() -> Entering handler..."
    logInfo "Scheduled Event Reached: Evening Cutoff Turn-OFF"
    switches?.off()
    sendNotificationMsg("Advanced Porch Lighting: Evening cutoff lights turned OFF.")
    state.nextScheduledEventDesc = (enableMorningSchedule != false) ? "Morning ON at ${state.schedMornOnDisp}" : "None scheduled"
}

private void sendNotificationMsg(String msg) {
    logTrace "sendNotificationMsg() -> Sending message: '${msg}'"
    if (notifier) {
        notifier.deviceNotification(msg)
    }
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

private Date parseIsoTimeToTodayDate(String isoTime) {
    logTrace "parseIsoTimeToTodayDate() -> Parsing ISO time string '${isoTime}'"
    if (!isoTime) return null
    try {
        java.text.SimpleDateFormat inputSdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        Date parsed = inputSdf.parse(isoTime)
        
        Calendar targetCal = Calendar.getInstance(location.timeZone)
        Calendar sourceCal = Calendar.getInstance(location.timeZone)
        sourceCal.time = parsed

        targetCal.set(Calendar.HOUR_OF_DAY, sourceCal.get(Calendar.HOUR_OF_DAY))
        targetCal.set(Calendar.MINUTE, sourceCal.get(Calendar.MINUTE))
        targetCal.set(Calendar.SECOND, 0)
        targetCal.set(Calendar.MILLISECOND, 0)
        return targetCal.time
    } catch (Exception e) {
        logError "Failed to parse ISO time string '${isoTime}': ${e.message}"
        return null
    }
}

private String formatIsoTimeStr(String isoTime) {
    logTrace "formatIsoTimeStr() -> Formatting ISO string '${isoTime}'"
    Date d = parseIsoTimeToTodayDate(isoTime)
    return d ? d.format("h:mm a", location.timeZone) : isoTime
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