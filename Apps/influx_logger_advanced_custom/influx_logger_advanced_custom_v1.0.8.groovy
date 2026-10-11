/* groovylint-disable LineLength, MethodParameterTypeRequired, MethodReturnTypeRequired, MethodSize, NestedBlockDepth, NoDef, ParameterReassignment, PublicMethodsBeforeNonPublicMethods, UnnecessaryGString, UnnecessaryGetter, UnnecessaryObjectReferences, UnusedMethodParameter, VariableTypeRequired */
/*****************************************************************************************************************
 *  Application Name: InfluxDB Logger Advanced (Custom)
 *  Platform: Hubitat Elevation
 *
 *  Source: https://github.com/HubitatCommunity/InfluxDB-Logger
 *  Forked from: https://github.com/codersaur/SmartThings/tree/master/smartapps/influxdb-logger
 *  Original Author: David Lomas (codersaur)
 *  Previous Author: Joshua Marker (tooluser)
 *  Hubitat Elevation version maintained by HubitatCommunity (https://github.com/HubitatCommunity/InfluxDB-Logger)
 *  Custom Refactor Maintainer: James Shimota
 *
 *  License:
 *   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *   for the specific language governing permissions and limitations under the License.
 *
 *  Modification History:
 *  Date       Name            Change
 *  2019-02-02 Dan Ogorchock   Use asynchttpPost() instead of httpPost() call
 *  2019-09-09 Caleb Morse     Support deferring writes and doing bulk writes to influxdb
 *  2022-06-20 Denny Page      Remove nested sections for device selection.
 *  2023-01-08 Denny Page      Address whitespace related lint issues. No functional changes.
 *  2023-01-09 Craig King      Added InfluxDb2.x support.
 *  2023-01-12 Denny Page      Automatic migration of Influx 1.x settings.
 *  2023-01-15 Denny Page      Clean up various things:
 *                             Remove Group ID/Name which are not supported on Hubitat.
 *                             Remove Location ID and Hub ID which are not supported on Hubitat (always 1).
 *                             Remove blocks of commented out code.
 *                             Don't set page sections hidden to false where hideable is false.
 *                             Remove state.queuedData.
 *  2023-01-22 PJ              Add filterEvents option for subscribe.
 *                             Fix event timestamps.
 *  2023-01-23 Denny Page      Allow multiple instances of the application to be installed.
 *                             NB: This requires Hubitat 2.2.9 or above.
 *  2023-01-25 Craig King      Updated Button selection to valid capability for Hubitat
 *  2023-02-16 PJ              Add error message to log for http response >= 400
 *                             Allow ssl cert verification to be disabled (self signed certificates)
 *  2023-02-26 Denny Page      Cleanup and rationalize UI
 *                             Use time since first data value to trigger post rather than periodic timer
 *                             Only create a keep alive event (softpoll) when no real event has been seen
 *                             Cleanup and rationalize logging
 *                             Further code cleanup
 *  2023-02-28 Denny Page      Retry failed posts
 *                             Enhance post logging
 *                             Allow Hub Name and Location tags to be disabled for device events
 *                             Further code cleanup
 *  2023-03-04 Denny Page      Clean up event processing code
 *                             Fix button event handling
 *                             Fix thermostat fan mode event handling
 *                             Fix threeAxis event encoding
 *                             Add device event handling for filters, gas detectors, power source
 *                             Remove handling for non-existent device capabilities
 *                             Move unnecessary info messages to debug
 *                             Disable debug logging of post data which drives hubs into the ground
 *                             Provide info logging of event data to replace post data logging
 *                             Allow backlog to be set as low as 1, allowing bad records to be cleared
 *  2023-03-12 Denny Page      Use a unified device type / attribute map (deviceTypeMap)
 *                             Unify advanced and non-advanced device selection processing
 *                             Move device event encoding out to a separate function
 *                             Enhance queueToInfluxDb to accept a list of events
 *                             Complete rewrite of softpoll (take advantage of queueToInfluxDb lists)
 *                             Remove unnecessary state variables
 *                             Don't re-schedule batch post based on batch size, wait for existing timer
 *                             Improve backlog warnings
 *                             Lower backlog limits to prevent issues with app database size
 *                             Normalize Hub information logging
 *  2023-03-14 Denny Page      If post of size one fails, log the actual failed record
 *                             Ignore momentary attributes for keep alive
 *                             Display full uri in config page for convenience
 *  2023-03-15 Denny Page      Always treat valid numbers (such as buttons) as numeric values for InfluxDB
 *  2023-03-16 Denny Page      Fix issue of quotes surrounding hub name
 *                             Fix issue of invalid three axis values being posted as a string
 *  2023-03-18 Denny Page      Fix valve attribute
 *                             Clean up logging
 *  2023-04-24 Denny Page      Don't send null units to InfluxDB
 *  2023-10-26 Denny Page      Add support for publishing queue length as a hub variable
 *                             Add support for publishing hub variable events (idea from Scott Chen)
 *  2023-11-14 Denny Page      Remove default values for Influx Version and Authorization Type
 *  2023-11-17 Denny Page      Add warning about special characters in Database, Org and Bucket names
 *  2024-03-17 Denny Page      Add failsafe call to writeQueuedDataToInfluxDb when new events are queued
 *                             Remove old migration items
 *  2025-03-13 Denny Page      When debugging, log individual fields of device and variable events
 *  2025-04-30 Denny Page      Clarify InfluxDB Version as referring to Write Protocol Version
 *  2025-08-30 Denny Page      Address softpoll issue with boolean system variables
 *  2025-12-21 Denny Page      Lower default values
 *  2026-03-17 Denny Page      Add InfluxDB V3 support (Thanks to mavrrick58)
 *  2026-04-03 Denny Page      Add Integrations menu support
 *  2026-10-11 jshimota        v1.0.0 Refactor to 'jshimota' namespace and author James Shimota.
 *                             Added standard versioning, title banner, showVersionInLabel toggle, 
 *                             version update trace demarcation, out-of-scope ${data} fix in HTTP handler, 
 *                             closure-isolated postCount for async callbacks, and null guards for device/variable encoders.
 *  2026-10-11 jshimota        v1.0.1 Implemented unique request ID tracking (state.activeRequestId) to insulate 
 *                             async callbacks from stale failsafe timeouts. Added custom label preservation in 
 *                             updateAppLabel(), guarded getDeviceAttrMap() against null device selections, 
 *                             and added null-safe fallback handling for sunrise/sunset in encodeHubInfo().
 *  2026-10-11 jshimota        v1.0.2 Implemented decoupled state.inFlightQueue tracking to protect active payloads 
 *                             during 90s failsafe timeouts. Added monotonic sequence counter for request IDs, 
 *                             broadened version suffix regex in updateAppLabel(), and guarded hubResponse object 
 *                             in handleInfluxResponse().
 *  2026-10-11 jshimota        v1.0.3 Implemented reconcileFailedBatch() to enforce projected backlog limits 
 *                             on re-insertions. Relocated failsafe watchdog to top of writeQueuedDataToInfluxDb(). 
 *                             Routed null/invalid HTTP responses through failure reconciliation to prevent stranded 
 *                             in-flight state memory. Unified label version suffix cleanup.
 *  2026-10-11 jshimota        v1.0.4 Modernized logging preferences to match Porch Light Manager model. Replaced 
 *                             configLoggingLevelIDE dropdown with boolean switches (logInfoEnable, logWarnEnable, 
 *                             logErrorEnable, logDebugEnable, logTraceEnable) and centralized logMessage() dynamic engine.
 *  2026-10-11 jshimota        v1.0.5 Moved high-frequency routine logging (device/variable subscriptions and queued events) 
 *                             from logInfo to logTrace to reduce Hubitat event log noise.
 *  2026-10-11 jshimota        v1.0.6 Edge Case Hardening: Wrapped HTTP response callback in try/catch block with explicit 
 *                             numeric status checks, updated 90s watchdog to purge timed-out payload keys from state memory, 
 *                             and sanitized logging levels against an explicit allowlist in logMessage().
 *  2026-10-11 jshimota        v1.0.7 Milestone Logging Overhaul: Moved routine HTTP batch completion logs to logTrace. 
 *                             Implemented cumulative 100-event milestone logging at logInfo level to drastically reduce 
 *                             log stream noise while keeping high-level transmission progress visible.
 *  2026-10-11 jshimota        v1.0.8 GUI Milestone Frequency Dropdown: Added user preference for milestone status frequency 
 *                             (25, 50, 100, 250, 500, 1000 events) to give full control over Info log summary verbosity.
 *****************************************************************************************************************/

static String version() { return '1.0.8' }
def timeStamp() { return "2026/10/11 11:25 AM" }

definition(
    name: "InfluxDB Logger Advanced (Custom)",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Log device states to InfluxDB with advanced queuing and custom status options",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    menu: "Integrations",
    singleThreaded: true
)

import groovy.transform.Field

// Device type list
@Field static final Map<String,Map> deviceTypeMap = [
    'accelerometers': [ title: 'Accelerometers', capability: 'accelerationSensor', attributes: ['acceleration'] ],
    'alarms': [ title: 'Alarms', capability: 'alarm', attributes: ['alarm'] ],
    'batteries': [ title: 'Batteries', capability: 'battery', attributes: ['battery'] ],
    'beacons': [ title: 'Beacons', capability: 'beacon', attributes: ['presence'] ],
    'buttons': [ title: 'Buttons', capability: 'pushableButton', attributes: ['pushed', 'doubleTapped', 'held', 'released'] ],
    'cos': [ title: 'Carbon Monoxide Detectors', capability: 'carbonMonoxideDetector', attributes: ['carbonMonoxide'] ],
    'co2s': [ title: 'Carbon Dioxide Detectors', capability: 'carbonDioxideMeasurement', attributes: ['carbonDioxide'] ],
    'colors': [ title: 'Color Controllers', capability: 'colorControl', attributes: ['hue', 'saturation', 'color'] ],
    'consumables': [ title: 'Consumables', capability: 'consumable', attributes: ['consumableStatus'] ],
    'contacts': [ title: 'Contact Sensors', capability: 'contactSensor', attributes: ['contact'] ],
    'currentMeters': [ title: 'Current Meters', capability: 'currentMeter', attributes: ['amperage'] ],
    'doorsControllers': [ title: 'Door Controllers', capability: 'doorControl', attributes: ['door'] ],
    'energyMeters': [ title: 'Energy Meters', capability: 'energyMeter', attributes: ['energy'] ],
    'filters': [ title: 'Filters', capability: 'filterStatus', attributes: ['filterStatus'] ],
    'gasDetectors': [ title: 'Gas Detectors', capability: 'gasDetector', attributes: ['naturalGas'] ],
    'humidities': [ title: 'Humidity Meters', capability: 'relativeHumidityMeasurement', attributes: ['humidity'] ],
    'illuminances': [ title: 'Illuminance Meters', capability: 'illuminanceMeasurement', attributes: ['illuminance'] ],
    'liquidFlowMeters': [ title: 'Liquid Flow Meters', capability: 'liquidFlowRate', attributes: ['rate'] ],
    'locks': [ title: 'Locks', capability: 'lock', attributes: ['lock'] ],
    'motions': [ title: 'Motion Sensors', capability: 'motionSensor', attributes: ['motion'] ],
    'musicPlayers': [ title: 'Music Players', capability: 'musicPlayer', attributes: ['status', 'level', 'trackDescription', 'trackData', 'mute'] ],
    'peds': [ title: 'Pedometers', capability: 'stepSensor', attributes: ['steps', 'goal'] ],
    'phMeters': [ title: 'pH Meters', capability: 'pHMeasurement', attributes: ['pH'] ],
    'powerMeters': [ title: 'Power Meters', capability: 'powerMeter', attributes: ['power'] ],
    'powerSources': [ title: 'Power Sources', capability: 'powerSource', attributes: ['powerSource'] ],
    'presences': [ title: 'Presence Sensors', capability: 'presenceSensor', attributes: ['presence'] ],
    'pressures': [ title: 'Pressure Sensors', capability: 'pressureMeasurement', attributes: ['pressure'] ],
    'shockSensors': [ title: 'Shock Sensors', capability: 'shockSensor', attributes: ['shock'] ],
    'signalStrengthMeters': [ title: 'Signal Strength Meters', capability: 'signalStrength', attributes: ['lqi', 'rssi'] ],
    'sleepSensors': [ title: 'Sleep Sensors', capability: 'sleepSensor', attributes: ['sleeping'] ],
    'smokeDetectors': [ title: 'Smoke Detectors', capability: 'smokeDetector', attributes: ['smoke'] ],
    'soundSensors': [ title: 'Sound Sensors', capability: 'soundSensor', attributes: ['sound'] ],
    'spls': [ title: 'Sound Pressure Level Sensors', capability: 'soundPressureLevel', attributes: ['soundPressureLevel'] ],
    'switches': [ title: 'Switches', capability: 'switch', attributes: ['switch'] ],
    'switchLevels': [ title: 'Switch Levels', capability: 'switchLevel', attributes: ['level'] ],
    'tamperAlerts': [ title: 'Tamper Alerts', capability: 'tamperAlert', attributes: ['tamper'] ],
    'temperatures': [ title: 'Temperature Sensors', capability: 'temperatureMeasurement', attributes: ['temperature'] ],
    'thermostats': [ title: 'Thermostats', capability: 'thermostat', attributes: ['temperature', 'heatingSetpoint', 'coolingSetpoint', 'thermostatSetpoint', 'thermostatMode', 'thermostatFanMode', 'thermostatOperatingState', 'thermostatSetpointMode', 'scheduledSetpoint'] ],
    'threeAxis': [ title: 'Three-axis (Orientation) Sensors', capability: 'threeAxis', attributes: ['threeAxis'] ],
    'touchs': [ title: 'Touch Sensors', capability: 'touchSensor', attributes: ['touch'] ],
    'uvs': [ title: 'UV Sensors', capability: 'ultravioletIndex', attributes: ['ultravioletIndex'] ],
    'valves': [ title: 'Valves', capability: 'valve', attributes: ['valve'] ],
    'volts': [ title: 'Voltage Meters', capability: 'voltageMeasurement', attributes: ['voltage'] ],
    'waterSensors': [ title: 'Water Sensors', capability: 'waterSensor', attributes: ['water'] ],
    'windowShades': [ title: 'Window Shades', capability: 'windowShade', attributes: ['windowShade'] ]
]

// Momentary attributes that should be ignored for keep alive
@Field static final List<String> momentaryAttributes = [
    'pushed', 'doubleTapped', 'held', 'released'
]

preferences {
    page(name: "setupMain")
    page(name: "connectionPage")
}

def setupMain() {
    dynamicPage(name: "setupMain", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* Styled App Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>InfluxDB Logger Advanced (Custom)</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        section("<h3>General Settings:</h3>") {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true, submitOnChange: true
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"
            input name: "logMilestoneFrequency", type: "enum", title: "Milestone Status Frequency (Info Level)", 
                  options: ["25": "Every 25 Events", "50": "Every 50 Events", "100": "Every 100 Events", "250": "Every 250 Events", "500": "Every 500 Events", "1000": "Every 1000 Events"], 
                  defaultValue: "100", required: true
        }

        section("<h3>InfluxDB Settings:</h3>") {
            href(
                name: "href",
                title: "InfluxDB connection",
                description: prefDatabaseHost == null ? "Configure database connection parameters" : uriString(),
                required: true,
                page: "connectionPage"
            )
            input(
                name: "prefBatchTimeLimit",
                title: "Batch time limit - maximum number of seconds before writing a batch to InfluxDB (range 1-300)",
                type: "number",
                range: "1..300",
                defaultValue: "15",
                required: true
            )
            input(
                name: "prefBatchSizeLimit",
                title: "Batch size limit - maximum number of events in a batch to InfluxDB (range 1-250)",
                type: "number",
                range: "1..250",
                defaultValue: "50",
                required: true
            )
            input(
                name: "prefBacklogLimit",
                title: "Backlog size limit - maximum number of queued events before dropping failed posts (range 1-5000)",
                type: "number",
                range: "1..5000",
                defaultValue: "500",
                required: true
            )
            input(
                name: "prefQueueSizeVariable",
                title: "Record InfluxDB backlog size in hub variable (optional, off by default)",
                type: "enum",
                required: false,
                options: getGlobalVarsByType("integer").keySet().sort(){ it.capitalize() }
            )
            paragraph "Notes: Queue size variable must be type integer (Number), and is updated following each post to InfluxDB."
        }

        section("<h3>Event Handling:</h3>") {
            input(
                name: "prefSoftPollingInterval",
                title: "Post keep alive events (aka softpoll) - check every softpoll interval and re-post last value if a new event has not occurred in this time",
                type: "enum",
                options: [
                    "0" : "disabled",
                    "1" : "1 minute (not recommended)",
                    "5" : "5 minutes",
                    "10" : "10 minutes",
                    "15" : "15 minutes",
                    "30" : "30 minutes",
                    "60" : "60 minutes",
                    "180" : "3 hours"
                ],
                defaultValue: "15",
                submitOnChange: true,
                required: true
            )
            if (prefSoftPollingInterval?.toInteger()) {
                input(
                    "prefPostHubInfo",
                    "bool",
                    title: "Post Hub information (IP, firmware, uptime, mode, sunrise/sunset) to InfluxDB",
                    defaultValue: false
                )
            }
            input(
                "includeHubInfo",
                "bool",
                title: "Include Hub Name as a tag for events",
                defaultValue: true
            )
            input(
                "filterEvents",
                "bool",
                title: "Only post device events to InfluxDB when the data value changes",
                defaultValue: true
            )
        }

        section("Devices To Monitor:", hideable:true, hidden:false) {
            input(
                "accessAllAttributes",
                "bool",
                title: "Advanced attribute selection?",
                defaultValue: false,
                submitOnChange: true
            )
            if (accessAllAttributes) {
                input(
                    name: "allDevices",
                    type: "capability.*",
                    title: "Selected Devices",
                    multiple: true,
                    required: false,
                    submitOnChange: true
                )
                settings.allDevices?.each { device ->
                    deviceId = device.getId()
                    attrList = device.getSupportedAttributes()?.unique()
                    if (attrList) {
                        options = []
                        attrList.each { attr ->
                            options.add("${attr}")
                        }
                        input(
                            name:"attrForDev${deviceId}",
                            type: "enum",
                            title: "$device",
                            options: options.sort(),
                            multiple: true,
                            required: false,
                            submitOnChange: true )
                    }
                }
            }
            else {
                deviceTypeMap.each { name, entry ->
                    input(
                        "${name}",
                        "capability.${entry.capability}",
                        title: "${entry.title}",
                        multiple: true,
                        required: false
                    )
                }
            }
        }

        section("Variables To Monitor:", hideable:true, hidden:false) {
            List vars = []
            getAllGlobalVars().each {
                switch (it.value.type) {
                    case 'integer':
                    case 'bigdecimal':
                    case 'boolean':
                    case 'string':
                        vars += it.key
                        break
                }
            }
            input(
                "variableList",
                "enum",
                title: "Selected Variables (datetime variables are not supported)",
                multiple: true,
                required: false,
                submitOnChange: true,
                options: vars.sort(){ it.capitalize() }
            )
        }

        section() {
            label title: "Assign a custom label for this SmartApp instance", required: false
        }
    }
}

def connectionPage() {
    dynamicPage(name: "connectionPage", title: "Connection Properties", install: false, uninstall: false) {
        section {
            input(
                "prefDatabaseTls",
                "bool",
                title: "Use TLS?",
                defaultValue: false,
                submitOnChange: true
            )
            if (prefDatabaseTls) {
                input(
                    "prefIgnoreSSLIssues",
                    "bool",
                    title: "Ignore SSL cert verification issues",
                    defaultValue: false
                )
            }
            input(
                "prefDatabaseHost",
                "text",
                title: "Host",
                defaultValue: "",
                required: true
            )
            input(
                "prefDatabasePort",
                "text",
                title: "Port",
                defaultValue: prefDatabaseTls ? "443" : "8086",
                required: false
            )
            input(
                name: "prefInfluxVer",
                title: "Influx Write Protocol Version",
                type: "enum",
                options: [
                    "1" : "v1",
                    "2" : "v2",
                    "3" : "v3"
                ],
                required: true,
                submitOnChange: true
            )
            if (prefInfluxVer == "1") {
                input(
                    "prefDatabaseName",
                    "text",
                    title: "Database Name",
                    defaultValue: "Hubitat",
                    required: true
                )
                paragraph "Do not use special characters (including spaces) in Database Names"
            }
            else if (prefInfluxVer == "2") {
                input(
                    "prefOrg",
                    "text",
                    title: "Org",
                    defaultValue: "",
                    required: true
                )
                input(
                    "prefBucket",
                    "text",
                    title: "Bucket",
                    defaultValue: "",
                    required: true
                )
                paragraph "Do not use special characters (including spaces) in Org or Bucket Names"
            }
            else if (prefInfluxVer == "3") {
                input(
                    "prefDatabaseName",
                    "text",
                    title: "Database Name",
                    defaultValue: "Hubitat",
                    required: true
                )
                paragraph "Do not use special characters (including spaces) in Database Names"
            }
            input(
                name: "prefAuthType",
                title: "Authorization Type",
                type: "enum",
                options: [
                    "none" : "None",
                    "basic" : "Username / Password",
                    "token" : "Token"
                ],
                required: true,
                submitOnChange: true
            )
            if (prefAuthType == "basic") {
                input(
                    "prefDatabaseUser",
                    "text",
                    title: "Username",
                    defaultValue: "",
                    required: true
                )
                input(
                    "prefDatabasePass",
                    "text",
                    title: "Password",
                    defaultValue: "",
                    required: true
                )
            }
            else if (prefAuthType == "token") {
                input(
                    "prefDatabaseToken",
                    "text",
                    title: "Token",
                    required: true
                )
            }
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
    String currentLabel = app.label ?: "InfluxDB Logger Advanced (Custom)"
    String verStr = " v${version()}"

    // Unconditionally clean any legacy version suffix first
    String cleanLabel = currentLabel.replaceAll(/ v\d+\.\d+\.\d+.*$/, '')

    if (showVersion) {
        app.updateLabel(cleanLabel + verStr)
    } else {
        app.updateLabel(cleanLabel)
    }
}

/**
 *  installed()
 *
 *  Runs when the app is first installed.
 **/
void installed() {
    checkAndLogVersionDemarcation()
    state.installedAt = now()
    state.loggerQueue = []
    state.inFlightQueue = [:]
    state.postCount = 0
    state.activeRequestId = null
    state.requestIdSeq = 0
    state.successfulEventsCount = 0
    updated()
    logInfo("Installed v${version()} (${timeStamp()})")
}

/**
 *  uninstalled()
 *
 *  Runs when the app is uninstalled.
 **/
void uninstalled() {
    logInfo("Uninstalled")
}

/**
 *  updated()
 *
 *  Runs when app settings are changed.
 **/
void updated() {
    checkAndLogVersionDemarcation()
    updateAppLabel()
    logInfo("Updated configuration")

    setupDB()

    unsubscribe()
    removeAllInUseGlobalVar()

    if (prefQueueSizeVariable) {
        logInfo("Queue size available in variable ${prefQueueSizeVariable}")
        addInUseGlobalVar(prefQueueSizeVariable)
    }
    
    List monitoredVars = settings.variableList ?: []
    monitoredVars.each { variable ->
        logTrace("Subscribing to variable ${variable}")
        subscribe(location, "variable:${variable}", "handleVariableEvent")
        addInUseGlobalVar(variable)
    }

    Map<String,List> deviceAttrMap = getDeviceAttrMap()
    deviceAttrMap.each { device, attrList ->
        if (device && attrList) {
            attrList.each { attr ->
                logTrace("Subscribing to ${device}: ${attr}")
                subscribe(device, attr, handleEvent, ["filterEvents": filterEvents])
            }
        }
    }

    subscribe(location, "systemStart", hubRestartHandler)

    if (prefPostHubInfo) {
        subscribe(location, "mode", handleModeEvent)
    }

    unschedule()

    state.softPollingInterval = settings.prefSoftPollingInterval ? settings.prefSoftPollingInterval.toInteger() : 15
    switch (state.softPollingInterval) {
        case 1:
            runEvery1Minute(softPoll)
            break
        case 5:
            runEvery5Minutes(softPoll)
            break
        case 10:
            runEvery10Minutes(softPoll)
            break
        case 15:
            runEvery15Minutes(softPoll)
            break
        case 30:
            runEvery30Minutes(softPoll)
            break
        case 60:
            runEvery1Hour(softPoll)
            break
        case 180:
            runEvery3Hours(softPoll)
            break
    }

    runIn(1, writeQueuedDataToInfluxDb)
}

/**
 *  getDeviceAttrMap()
 *
 *  Build a device attribute map safely.
 **/
private Map<String,List> getDeviceAttrMap() {
    deviceAttrMap = [:]

    if (settings.accessAllAttributes) {
        settings.allDevices?.each { device ->
            if (device) {
                deviceId = device.getId()
                List selectedAttrs = settings["attrForDev${deviceId}"]
                if (selectedAttrs) {
                    deviceAttrMap[device] = selectedAttrs
                }
            }
        }
    }
    else {
        deviceTypeMap.each { name, entry ->
            deviceList = settings."${name}"
            if (deviceList) {
                deviceList.each { device ->
                    if (device) {
                        deviceAttrMap[device] = entry.attributes
                    }
                }
            }
        }
    }

    return deviceAttrMap
}

/**
 * hubRestartHandler()
 *
 * Handle hub restarts.
**/
void hubRestartHandler(evt) {
    if (prefPostHubInfo) {
        handleModeEvent(null)
    }

    if (state.loggerQueue?.size()) {
        runIn(60, writeQueuedDataToInfluxDb)
    }
}

/**
 *  encodeVariableEvent(evt)
 *
 *  Builds data to send to InfluxDB for variable events.
 **/
private String encodeVariableEvent(evt) {
    if (evt?.value == null) {
        logWarn("Skipping variable event with null value: ${evt?.name}")
        return null
    }

    String name = evt.name - 'variable:'
    def variable = getGlobalVar(name)
    if (!variable) {
        logWarn("Variable '${name}' not found on hub during encoding")
        return null
    }
    String type = variable.type
    String value

    switch (type) {
        case 'integer':
            value = "${evt.value}i"
            break
        case 'bigdecimal':
            value = "${evt.value}"
            break
        case 'boolean':
            value = evt.value.toString() == 'true' ? 'T' : 'F'
            break
        case 'string':
            value = '"' + escapeStringForInfluxDB(evt.value.toString()) + '"'
            break
    }

    String variableName = escapeStringForInfluxDB(name)
    String data = "${type}Variable,variableName=${variableName}"

    if (settings.includeHubInfo == null || settings.includeHubInfo) {
        String hubName = escapeStringForInfluxDB(location.name)
        data += ",hubName=${hubName}"
    }

    data += " value=${value}"

    long eventTimestamp = (evt.unixTime ?: now()) * 1e6
    data += " ${eventTimestamp}"

    return(data)
}

/**
 *  encodeDeviceEvent(evt)
 *
 *  Builds data to send to InfluxDB for device events.
 **/
private String encodeDeviceEvent(evt) {
    if (evt?.value == null) {
        logWarn("Skipping device event with null value: ${evt?.displayName} [${evt?.name}]")
        return null
    }

    String unit = ''
    String value = ''
    String valueBinary = ''
    String evtValStr = evt.value.toString()

    switch (evt.name) {
        case 'acceleration':
            valueBinary = (evtValStr == 'active') ? '1i' : '0i'
            break
        case 'alarm':
            valueBinary = (evtValStr == 'off') ? '1i' : '0i'
            break
        case 'carbonMonoxide':
            valueBinary = (evtValStr == 'detected') ? '1i' : '0i'
            break
        case 'consumableStatus':
            valueBinary = (evtValStr == 'good') ? '1i' : '0i'
            break
        case 'contact':
            valueBinary = (evtValStr == 'closed') ? '1i' : '0i'
            break
        case 'door':
            valueBinary = (evtValStr == 'closed') ? '1i' : '0i'
            break
        case 'filterStatus':
            valueBinary = (evtValStr == 'normal') ? '1i' : '0i'
            break
        case 'lock':
            valueBinary = (evtValStr == 'locked') ? '1i' : '0i'
            break
        case 'motion':
            valueBinary = (evtValStr == 'active') ? '1i' : '0i'
            break
        case 'mute':
            valueBinary = (evtValStr == 'muted') ? '1i' : '0i'
            break
        case 'naturalGas':
            valueBinary = (evtValStr == 'detected') ? '1i' : '0i'
            break
        case 'powerSource':
            valueBinary = (evtValStr == 'mains') ? '1i' : '0i'
            break
        case 'presence':
            valueBinary = (evtValStr == 'present') ? '1i' : '0i'
            break
        case 'shock':
            valueBinary = (evtValStr == 'detected') ? '1i' : '0i'
            break
        case 'sleeping':
            valueBinary = (evtValStr == 'sleeping') ? '1i' : '0i'
            break
        case 'smoke':
            valueBinary = (evtValStr == 'detected') ? '1i' : '0i'
            break
        case 'sound':
            valueBinary = (evtValStr == 'detected') ? '1i' : '0i'
            break
        case 'switch':
            valueBinary = (evtValStr == 'on') ? '1i' : '0i'
            break
        case 'tamper':
            valueBinary = (evtValStr == 'detected') ? '1i' : '0i'
            break
        case 'thermostatMode':
            valueBinary = (evtValStr == 'off') ? '0i' : '1i'
            break
        case 'thermostatFanMode':
            valueBinary = (evtValStr == 'auto') ? '0i' : '1i'
            break
        case 'thermostatOperatingState':
            valueBinary = (evtValStr == 'heating' || evtValStr == 'cooling') ? '1i' : '0i'
            break
        case 'thermostatSetpointMode':
            valueBinary = (evtValStr == 'followSchedule') ? '0i' : '1i'
            break
        case 'threeAxis':
            unit = evt.name
            try {
                def (_,x,y,z) = (evtValStr =~ /^\[x:(-?[0-9]{1,3}),y:(-?[0-9]{1,3}),z:(-?[0-9]{1,3})\]$/)[0]
                value = "valueX=${x}i,valueY=${y}i,valueZ=${z}i"
            }
            catch (e) {
                value = "valueX=0i,valueY=0i,valueZ=0i"
                logWarn("Invalid threeAxis format: ${evtValStr}")
            }
            break
        case 'touch':
            valueBinary = (evtValStr == 'touched') ? '1i' : '0i'
            break
        case 'valve':
            valueBinary = (evtValStr == 'open') ? '1i' : '0i'
            break
        case 'water':
            valueBinary = (evtValStr == 'wet') ? '1i' : '0i'
            break
        case 'windowShade':
            valueBinary = (evtValStr == 'closed') ? '1i' : '0i'
            break
        case 'doubleTapped':
        case 'held':
        case 'pushed':
            unit = 'button'
            valueBinary = '1i'
            break
        case 'released':
            unit = 'button'
            valueBinary = '0i'
            break
    }

    if (!unit) {
        if (valueBinary) {
            unit = escapeStringForInfluxDB(evt.name)
        }
        else if (evt.unit) {
            unit = escapeStringForInfluxDB(evt.unit)
        }
    }

    if (!value) {
        if (evtValStr.isNumber()) {
            value = evtValStr
        }
        else {
            value = '"' + escapeStringForInfluxDB(evtValStr) + '"'
        }
    }

    String measurement = escapeStringForInfluxDB(evt.name)
    String deviceId = evt.deviceId
    String deviceName = escapeStringForInfluxDB(evt.displayName)
    String data = "${measurement},deviceName=${deviceName},deviceId=${deviceId}"

    if (settings.includeHubInfo == null || settings.includeHubInfo) {
        String hubName = escapeStringForInfluxDB(location.name)
        data += ",hubName=${hubName}"
    }

    if (unit) {
        data += ",unit=${unit}"
    }
    if (value ==~ /^value.*/) {
        data += " ${value}"
    }
    else {
        data += " value=${value}"
    }
    if (valueBinary) {
        data += ",valueBinary=${valueBinary}"
    }

    long eventTimestamp = (evt.unixTime ?: now()) * 1e6
    data += " ${eventTimestamp}"

    return(data)
}

/**
 *  encodeHubInfo(evt)
 *
 *  Build a Hub Information record to send to InfluxDB with null guards.
 **/
private String encodeHubInfo(evt) {
    String hubName = escapeStringForInfluxDB(location.name)
    String localIP = escapeStringForInfluxDB(location.hub?.localIP ?: "0.0.0.0")
    String firmwareVersion = escapeStringForInfluxDB(location.hub?.firmwareVersionString ?: "Unknown")
    String upTime = escapeStringForInfluxDB(location.hub?.uptime?.toString() ?: "0")
    String mode = escapeStringForInfluxDB(evt?.value ? evt.value : location.getMode())

    String sunriseTime = "00:00"
    String sunsetTime = "00:00"
    try {
        def times = getSunriseAndSunset()
        if (times?.sunrise && times?.sunset) {
            sunriseTime = escapeStringForInfluxDB(times.sunrise.format("HH:mm", location.timeZone))
            sunsetTime = escapeStringForInfluxDB(times.sunset.format("HH:mm", location.timeZone))
        }
    } catch (Exception e) {
        logWarn("Failed to evaluate sunrise/sunset times for hub info: ${e.message}")
    }

    Long eventTimestamp = (evt?.unixTime ? evt.unixTime : now()) * 1e6

    String data = "_hubInfo,hubName=${hubName} localIP=\"${localIP}\",firmwareVersion=\"${firmwareVersion}\",upTime=\"${upTime}\",mode=\"${mode}\",sunriseTime=\"${sunriseTime}\",sunsetTime=\"${sunsetTime}\" ${eventTimestamp}"
    return data
}

/**
 *  handleVariableEvent(evt)
 *
 *  Handle variable events
 **/
void handleVariableEvent(evt) {
    logDebug("Handle Variable Event: ${evt.name}, ${evt.value}")

    data = encodeVariableEvent(evt)
    if (data) {
        queueToInfluxDb([data])
    }
}

/**
 *  handleEvent(evt)
 *
 *  Handle device events
**/
void handleEvent(evt) {
    logDebug("Handle Event: ${evt.displayName}, ${evt.name}, ${evt.unit}, ${evt.value}")

    data = encodeDeviceEvent(evt)
    if (data) {
        queueToInfluxDb([data])
    }
}

/**
 *  handleModeEvent(evt)
 *
 *  Log hub information when mode changes.
 **/
void handleModeEvent(evt) {
    logDebug("Handle Mode Event: ${evt}")

    data = encodeHubInfo(evt)
    if (data) {
        queueToInfluxDb([data])
    }
}

/**
 *  softPoll()
 *
 *  Re-queues last value to InfluxDB unless an event has already been seen in the last softPollingInterval.
 **/
void softPoll() {
    logDebug("Keepalive check")

    Long timeNow = now()
    List<String> eventList = []

    List monitoredVars = settings.variableList ?: []
    monitoredVars.each { name ->
        logDebug("Keep alive for variable ${name}")
        def varObj = getGlobalVar(name)
        if (varObj) {
            event = encodeVariableEvent([
                name: "variable:${name}",
                value: varObj.value,
                unixTime: timeNow
            ])
            if (event) eventList.add(event)
        }
    }

    Map<String,List> deviceAttrMap = getDeviceAttrMap()
    deviceAttrMap.each { device, attrList ->
        if (device && attrList) {
            attrList.each { attr ->
                if (momentaryAttributes.contains(attr)) {
                    logDebug("Keep alive for device ${device}(${attr}) suppressed - momentary attribute")
                    return
                }
                if (device.latestState(attr)) {
                    Integer activityMinutes = (timeNow - device.latestState(attr).date.time) / 60000
                    if (activityMinutes > state.softPollingInterval) {
                        logDebug("Keep alive for device ${device}(${attr})")
                        event = encodeDeviceEvent([
                            name: attr,
                            value: device.latestState(attr).value,
                            unit: device.latestState(attr).unit,
                            device: device,
                            deviceId: device.id,
                            displayName: device.displayName,
                            unixTime: timeNow
                        ])
                        if (event) eventList.add(event)
                    }
                    else {
                        logDebug("Keep alive for device ${device}(${attr}) unnecessary - last activity ${activityMinutes} minutes")
                    }
                }
                else {
                    logDebug("Keep alive for device ${device}(${attr}) suppressed - last activity never")
                }
            }
        }
    }

    if (settings.prefPostHubInfo) {
        eventList.add(encodeHubInfo(null))
    }

    queueToInfluxDb(eventList)
}

/**
 *  queueToInfluxDb()
 *
 *  Adds events to the InfluxDB queue.
 **/
private void queueToInfluxDb(List<String> eventList) {
    if (!state.loggerQueue) state.loggerQueue = []
    
    priorLoggerQueueSize = state.loggerQueue.size()
    state.loggerQueue += eventList
    eventList.each { event ->
        logTrace("Queued event: ${event}")
    }

    if (priorLoggerQueueSize == 0) {
        logDebug("Scheduling batch")
        runIn(settings.prefBatchTimeLimit, writeQueuedDataToInfluxDb)
    }
    else if (priorLoggerQueueSize > settings.prefBacklogLimit + settings.prefBatchSizeLimit) {
        writeQueuedDataToInfluxDb()
    }
}

/**
 *  writeQueuedDataToInfluxDb()
 *
 *  Posts data to InfluxDB queue with isolated in-flight queue tracking and 90s watchdog sweep.
 **/
void writeQueuedDataToInfluxDb() {
    if (!state.loggerQueue) state.loggerQueue = []
    if (!state.inFlightQueue) state.inFlightQueue = [:]

    Long timeNow = now()
    Integer postCount = state.postCount ?: 0

    // Watchdog Sweep: Purge hanging requests prior to queue size check
    if (postCount > 0) {
        Long elapsed = timeNow - (state.lastPost ?: timeNow)
        logDebug("Post of ${postCount} events already running (elapsed ${elapsed}ms)")
        if (elapsed < 90000) {
            runIn(30, writeQueuedDataToInfluxDb)
            return
        }

        logWarn("Post callback failsafe timeout reached for request ${state.activeRequestId}. Purging stale payload from in-flight memory.")
        if (state.activeRequestId) {
            state.inFlightQueue.remove(state.activeRequestId)
        }
        state.postCount = 0
        state.activeRequestId = null
    }

    Integer loggerQueueSize = state.loggerQueue.size()
    logDebug("Number of events queued for InfluxDB: ${loggerQueueSize}")
    if (loggerQueueSize == 0) {
        return
    }

    if (loggerQueueSize > settings.prefBacklogLimit) {
        logWarn("Backlog of ${loggerQueueSize} events queued for InfluxDB (exceeds limit ${settings.prefBacklogLimit})")
    }

    postCount = loggerQueueSize < settings.prefBatchSizeLimit ? loggerQueueSize : settings.prefBatchSizeLimit
    
    state.requestIdSeq = (state.requestIdSeq ?: 0) + 1
    String reqId = "req_${timeNow}_${state.requestIdSeq}"

    List payloadBatch = state.loggerQueue.subList(0, postCount)
    state.inFlightQueue[reqId] = payloadBatch
    state.loggerQueue = state.loggerQueue.drop(postCount)

    state.postCount = postCount
    state.lastPost = timeNow
    state.activeRequestId = reqId

    String data = payloadBatch.toArray().join('\n')

    def postParams = [
        uri: state.uri,
        requestContentType: 'application/json',
        contentType: 'application/json',
        headers: state.headers,
        ignoreSSLIssues: settings.prefIgnoreSSLIssues,
        timeout: 60,
        body: data
    ]
    
    asynchttpPost('handleInfluxResponse', postParams, [ postTime: timeNow, postCount: postCount, requestId: reqId ])
}

/**
 *  reconcileFailedBatch()
 *
 *  Centralized failure handler enforcing projected backlog limit checks on retry re-insertions.
 **/
private void reconcileFailedBatch(String reqId, List failedPayload, Boolean isStale) {
    if (!state.loggerQueue) state.loggerQueue = []
    if (!state.inFlightQueue) state.inFlightQueue = [:]

    if (!failedPayload) {
        if (reqId) state.inFlightQueue.remove(reqId)
        return
    }

    Integer currentQueueSize = state.loggerQueue.size()
    Integer projectedQueueSize = currentQueueSize + failedPayload.size()
    Integer maxBacklog = settings.prefBacklogLimit ? settings.prefBacklogLimit.toInteger() : 500

    if (projectedQueueSize <= maxBacklog) {
        logWarn("Re-inserting failed batch [ID: ${reqId}] (${failedPayload.size()} events). Projected Queue Size: ${projectedQueueSize}")
        state.loggerQueue = failedPayload + state.loggerQueue
        if (reqId) state.inFlightQueue.remove(reqId)
        runIn(60, writeQueuedDataToInfluxDb)
    } else {
        logError("Projected queue size (${projectedQueueSize}) exceeds limit of ${maxBacklog}: dropping failed batch [ID: ${reqId}]")
        if (reqId) state.inFlightQueue.remove(reqId)
    }

    if (prefQueueSizeVariable) {
        setGlobalVar(prefQueueSizeVariable, state.loggerQueue.size())
    }
}

/**
 *  handleInfluxResponse()
 *
 *  Handles response from post made in writeQueuedDataToInfluxDb() with isolated in-flight reconciliation, exception guards, and configurable milestone logging.
 **/
void handleInfluxResponse(hubResponse, closure) {
    if (!state.inFlightQueue) state.inFlightQueue = [:]

    Double elapsed = (closure) ? (now() - closure.postTime) / 1000 : 0
    Integer postCount = closure?.postCount ?: 0
    String reqId = closure?.requestId

    Boolean isActiveRequest = (reqId != null && state.activeRequestId == reqId)
    if (isActiveRequest) {
        state.postCount = 0
        state.activeRequestId = null
    } else if (reqId != null) {
        logDebug("Processing late/stale callback response for request ID '${reqId}' (Active is '${state.activeRequestId}').")
    }

    try {
        // Guard against null hubResponse objects from underlying network engine
        if (!hubResponse) {
            logError("Received null hubResponse object in HTTP callback [ID: ${reqId}]")
            List failedPayload = reqId ? state.inFlightQueue[reqId] : null
            reconcileFailedBatch(reqId, failedPayload, !isActiveRequest)
            return
        }

        Integer statusCode = null
        try {
            if (hubResponse.status != null) {
                statusCode = hubResponse.status.toString().toInteger()
            }
        } catch (Exception e) {
            logWarn("Unable to parse numeric status code from response '${hubResponse?.status}': ${e.message}")
            statusCode = null
        }

        if (statusCode != null && statusCode < 400) {
            logTrace("Post of ${postCount} events complete [ID: ${reqId}] - elapsed time ${elapsed} seconds - Status: ${statusCode}")
            if (reqId) state.inFlightQueue.remove(reqId)

            // Cumulative Configurable Milestone Tracking
            Integer milestoneFreq = settings.logMilestoneFrequency ? settings.logMilestoneFrequency.toInteger() : 100
            Integer prevCount = state.successfulEventsCount ?: 0
            Integer newCount = prevCount + postCount
            state.successfulEventsCount = newCount

            if ((prevCount / milestoneFreq).toInteger() < (newCount / milestoneFreq).toInteger()) {
                logInfo("Cumulative Status: ${newCount} total events successfully posted to InfluxDB.")
            }
        }
        else {
            String errStr = hubResponse.hasProperty('errorMessage') ? hubResponse.errorMessage : 'Unknown error'
            logWarn("Post of ${postCount} events failed [ID: ${reqId}] - elapsed time ${elapsed} seconds - Status: ${statusCode ?: 'Invalid'}, Error: ${errStr}")
            
            List failedPayload = reqId ? state.inFlightQueue[reqId] : null
            if (postCount == 1 && failedPayload?.size() > 0) {
                logError("Failed record was: ${failedPayload[0]}")
            }

            reconcileFailedBatch(reqId, failedPayload, !isActiveRequest)
            return
        }
    } catch (Exception e) {
        logError("Unhandled exception inside handleInfluxResponse() [ID: ${reqId}]: ${e.message}")
        List failedPayload = reqId ? state.inFlightQueue[reqId] : null
        reconcileFailedBatch(reqId, failedPayload, !isActiveRequest)
        return
    }

    Integer totalRemainingQueue = state.loggerQueue.size()
    if (totalRemainingQueue > 0) {
        runInMillis(50, writeQueuedDataToInfluxDb)
    }

    if (prefQueueSizeVariable) {
        setGlobalVar(prefQueueSizeVariable, totalRemainingQueue)
    }
}

/**
 *  renameVariable()
 *
 *  Handle renaming of hub variables
 **/
void renameVariable(String oldName, String newName) {
    logInfo("variable \"${oldName}\" renamed to \"${newName}\"")

    if (settings?.prefQueueSizeVariable == oldName) {
        app.updateSetting("prefQueueSizeVariable", [type: "enum", value: newName])
        logInfo("Queue size available in variable ${newName}")
    }

    List variableList = settings.variableList ?: []
    Integer index = variableList.indexOf(oldName)
    if (index >= 0) {
        variableList[index] = newName
        app.updateSetting("variableList", [type: "enum", value: variableList])

        logTrace("Subscribing to variable ${newName}")
        subscribe(location, "variable:${newName}", "handleVariableEvent")
        unsubscribe(location, "variable:${oldName}", "handleVariableEvent")
    }
}

/**
 *  uriString()
 *
 *  Format the uri string.
 **/
private String uriString() {
    String uri

    if (settings?.prefDatabaseTls) {
        uri = "https://"
    }
    else {
        uri = "http://"
    }

    uri += settings.prefDatabaseHost
    if (settings?.prefDatabasePort) {
        uri += ":" + settings.prefDatabasePort
    }

    if (settings?.prefInfluxVer == "3") {
        uri += "/api/v3/write_lp?db=${settings.prefDatabaseName}"
    }
    else if (settings?.prefInfluxVer == "2") {
        uri += "/api/v2/write?org=${settings.prefOrg}&bucket=${settings.prefBucket}"
    }
    else {
        uri += "/write?db=${settings.prefDatabaseName}"
    }

    return uri
}

/**
 *  setupDB()
 *
 *  Set up the database uri and header state variables.
 **/
private void setupDB() {
    def headers = [:]
    if (settings.prefAuthType == null || settings.prefAuthType == "basic") {
        if (settings.prefDatabaseUser && settings.prefDatabasePass) {
            String userpass = "${settings.prefDatabaseUser}:${settings.prefDatabasePass}"
            headers.put("Authorization", "Basic " + userpass.bytes.encodeBase64())
        }
    }
    else if (settings.prefAuthType == "token") {
        headers.put("Authorization", "Token ${settings.prefDatabaseToken}")
    }

    state.uri = uriString()
    state.headers = headers

    logInfo("InfluxDB URI: ${state.uri}")
}

/* =========================================================================================
   CENTRALIZED DYNAMIC LOGGING ENGINE
   ========================================================================================= */

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    if (!(lowerLevel in ["info", "warn", "error", "debug", "trace"])) {
        lowerLevel = "info"
    }

    String appLabel = app.label ?: app.name ?: "InfluxDB Logger"
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

private String escapeStringForInfluxDB(String str) {
    if (str == null) {
        return 'null'
    }

    str = str.replaceAll(" ", "\\\\ ")
    str = str.replaceAll(",", "\\\\,")
    str = str.replaceAll("=", "\\\\=")
    str = str.replaceAll("\"", "\\\\\"")

    return str
}