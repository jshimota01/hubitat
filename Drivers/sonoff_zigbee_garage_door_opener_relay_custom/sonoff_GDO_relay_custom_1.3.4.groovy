/**
 * Sonoff GDO Relay Driver (Custom)
 * Device Driver for Hubitat Elevation
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
 *  Controls physical Sonoff Zigbee relay module for garage door motor pulse triggering.
 *  
 *  Changelog:
 *  v1.3.4    10/08/26    jshimota    Removed tuyaBlackMagic from refresh() to eliminate excess responses; added attrInt routing safeguards for 0x8001/0x8002/0x8005 attributes and null guards on descMap.data parsing.
 *  v1.3.3    10/08/26    jshimota    Added powerSource, powerOnState, networkIndicator, and turboMode attributes/settings; implemented hardware-level OnWithTimedOff command (0x42) for pulse triggering.
 *  v1.3.2    10/08/26    jshimota    Replaced Ping-based health monitoring with passive inbound activity watchdog modeled on eWeLink MS01; added lastActivity/healthStatus monitoring and removed Ping health commands, attributes, timeouts, and scheduled polling.
 *  v1.3.1    08/30/26    jshimota    Fixed getSonoffAttributeValue array size bounds check; directed open()/close() to on(); added auto-reinit to resetDriver()
 *  v1.3.0    08/30/26    jshimota    Applied Driver Master Template v1.0.4, added lastInitializedVersion check, fixed getSonoffAttributeValue bounds safety
 *  v1.2.6    08/28/26    jshimota    Removed redundant state.driverVersion variable in favor of the driverVersion attribute
 *  v1.2.5    08/28/26    jshimota    Standardized changelog format to tabbed MM/DD/YY columns without parentheses
 *  v1.2.4    08/28/26    jshimota    Applied master driver template, independent logging toggles, sendIfChanged deduplication, and timestamp NPE safeguards
 *  v1.2.3    08/27/26    jshimota    Added lastPing and pingStatus attributes with dynamic timeout tracking
 *  v1.2.2    08/27/26    jshimota    Updated ping and refresh logging to write to info logs when txtEnable is true
 *  v1.2.1    08/27/26    jshimota    Bug fixes: corrected cron syntax for hourly schedule, updated unschedule method call, robust attrInt checking in parse
 *  v1.2.0    08/27/26    jshimota    Added refresh/ping capabilities and automated variable scheduled health checks
 *  v1.1.0h   05/27/26    jshimota    Rebuilt for Sonoff Mini-ZBD
 *  v1.1.0g   05/24/26    jshimota    Fixed an NPE, gemini optimized
 *  v1.1.0f   09/24/24    jshimota    Removed more stuff - set open and close just to pass through
 *  v1.1.0e   09/15/24    jshimota    Removed contact door state stuff
 *  v1.1.0d   07/22/24    jshimota    Replaced PowerSource - still useless but was involved
 *  v1.1.0c   07/22/24    jshimota    Removed PowerSource - unused
 *  v1.1.0b   07/22/24    jshimota    Copy off kkossev Sonoff Zigbee Garage Door Opener
 *  v1.1.0    07/15/24    kkossev     (dev.branch) added commands setContact() and setDoor()
 *  v1.0.5    10/09/23    kkossev     Added _TZE204_nklqjk62 fingerprint
 *  v1.0.4    07/06/22    kkossev     On/off commands open/close door; contact status info/warning logs shown only on state change
 *  v1.0.3    06/26/22    kkossev     Fixed new device exceptions bug; warnings in debug logs only
 *  v1.0.2    06/20/22    kkossev     Ignore open command if sensor is open; ignore close command if sensor is closed
 *  v1.0.1    06/19/22    kkossev     Fixed contact status open/close; added doorTimeout preference; improved debug logging
 *  v1.0.0    06/18/22    kkossev     Initial test version
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

import hubitat.device.HubAction
import hubitat.device.Protocol
import groovy.transform.Field
import hubitat.zigbee.zcl.DataType

static String version() { return '1.3.4' }
def timeStamp() { return "2026/10/08 09:42 AM" }

@Field static final Integer PULSE_TIMER = 1250 // milliseconds

metadata {
    definition (
        name: "Sonoff GDO Relay Driver (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota", 
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/sonoff_gdo_relay_custom/Sonoff_GDO_Relay_Custom.groovy", 
        singleThreaded: true
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Switch"
        capability "GarageDoorControl"
        capability "Refresh"

        // Custom Attributes
        attribute "driverVersion", "string"
        attribute "lastActivity", "string"
        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "powerSource", "string"
        attribute "powerOnState", "enum", ["off", "on", "previous"]
        attribute "networkIndicator", "enum", ["off", "on"]
        attribute "turboMode", "enum", ["disabled", "enabled"]

        // Custom Commands
        command "resetDriver"

        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0B05,FC57,FC11", outClusters:"0003,0006,0019", model:"MINI-ZBD", manufacturer:"SONOFF", controllerType: "ZGB", deviceJoinName: "Sonoff Garage Door Opener Relay"
    }

    preferences {
        input name: "checkInInterval", type: "enum", title: "Activity Check-In Interval", description: "Periodically evaluate the age of the last received device activity. No command is sent to the device.", options: ["0":"Disabled", "1":"Every Hour", "3":"Every 3 Hours", "6":"Every 6 Hours", "12":"Every 12 Hours", "24":"Every 24 Hours"], defaultValue: "3", required: true
        input name: "powerOnState", type: "enum", title: "Power-On Behavior", description: "Select relay state following a power restore.", options: ["off":"Off", "on":"On", "previous":"Remember Previous State"], defaultValue: "off", required: true
        input name: "networkIndicator", type: "enum", title: "Network Indicator LED", description: "Enable or disable onboard network LED indicator.", options: ["off":"Disabled (Off)", "on":"Enabled (On)"], defaultValue: "on", required: true
        input name: "turboMode", type: "enum", title: "Zigbee Turbo RF Power Mode", description: "Boost Zigbee radio transmit power to improve mesh range.", options: ["disabled":"Normal Power", "enabled":"Turbo Power"], defaultValue: "disabled", required: true

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

private getCLUSTER_SONOFF() { 0x0006 }

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// NPE-Safe Timestamp Helper
private String getTimestamp() {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date().format("yyyy-MM-dd HH:mm:ss", tz)
}

// Passive Record Inbound Activity Helper
private void recordActivity() {
    state.lastActivityTime = now()
    sendEvent(name: "lastActivity", value: getTimestamp(), displayed: false)
    sendIfChanged([name: "healthStatus", value: "online", descriptionText: "healthStatus is online"])
}

// Parse incoming Zigbee messages
def parse(String description) {
    logDebug "Raw description -> ${description}"
    
    if (description?.startsWith('catchall:') || description?.startsWith('read attr -')) {
        Map descMap = [:]
        try {
            descMap = zigbee.parseDescriptionAsMap(description)
        } catch (e) {
            logWarn "Exception caught while parsing descMap: ${e.message}"
            return null
        }

        // Record inbound activity only after verifying payload parsed without exception
        recordActivity()
   
        if (descMap?.clusterInt == CLUSTER_SONOFF || descMap?.cluster == "FC11") {
            logDebug "Parse Sonoff Cluster descMap -> ${descMap}"
            
            // Priority check for Sonoff attributes (0x8001: LED, 0x8002: PowerOnState, 0x8005: TurboMode)
            if (descMap?.attrInt == 0x8001 && descMap?.value != null) {
                String ledVal = (Integer.parseInt(descMap.value, 16) == 1) ? "on" : "off"
                sendIfChanged([name: "networkIndicator", value: ledVal, descriptionText: "networkIndicator is ${ledVal}"])
            }
            else if (descMap?.attrInt == 0x8002 && descMap?.value != null) {
                int val = Integer.parseInt(descMap.value, 16)
                String pState = (val == 1) ? "on" : (val == 2 ? "previous" : "off")
                sendIfChanged([name: "powerOnState", value: pState, descriptionText: "powerOnState is ${pState}"])
            }
            else if (descMap?.attrInt == 0x8005 && descMap?.value != null) {
                String turboVal = (Integer.parseInt(descMap.value, 16) == 1) ? "enabled" : "disabled"
                sendIfChanged([name: "turboMode", value: turboVal, descriptionText: "turboMode is ${turboVal}"])
            }
            else if (descMap?.command in ["00", "01", "02"]) {
                if (descMap?.data != null && descMap.data.size() > 3) {
                    def fncmd = getSonoffAttributeValue(descMap?.data)
                    def dp = zigbee.convertHexToInt(descMap?.data[2])
                    def dp_id = zigbee.convertHexToInt(descMap?.data[3])
                    
                    logTrace "Sonoff cluster dp_id=${dp_id} dp=${dp} fncmd=${fncmd}"
                    
                    switch (dp) {
                        case 0x01 : // Relay / trigger switch
                            def value = fncmd == 1 ? "on" : "off"
                            logDebug "Received Relay report dp_id=${dp_id} dp=${dp} fncmd=${fncmd} -> ${value}"
                            break
                        case 0x02 : // Confirmation payload
                            logDebug "Received confirmation report dp_id=${dp_id} dp=${dp} fncmd=${fncmd}"
                            break
                        default :
                            logDebug "Unprocessed Sonoff data command: dp=${dp} value=${fncmd} descMap.data = ${descMap?.data}"
                            break
                    }
                } else {
                    logDebug "Received short Sonoff command payload -> ${descMap?.data}"
                }
            } 
            else if (descMap?.command == "0B") {    
                logDebug "ZCL response: 0x${descMap?.data[1]} status: ${descMap?.data[1]=='00'?'success':'FAILURE'} data: ${descMap?.data}"
            } else {
                logDebug "Unprocessed Sonoff cluster command ${descMap?.command} : descMap.data = ${descMap?.data}"
            }
        } 
        else if (descMap?.clusterInt == 0x0000) {
            if (descMap?.attrInt == 0x0007 && descMap?.value != null) {
                int pVal = Integer.parseInt(descMap.value, 16)
                String pSource = (pVal in [0x03, 0x04]) ? "dc" : "mains"
                sendIfChanged([name: "powerSource", value: pSource, descriptionText: "powerSource is ${pSource}"])
            } else if (descMap?.attrInt == 1 || descMap?.attrId == "0001") {
                logDebug "Sonoff check-in: ${descMap}"
            }
        } else {
            logDebug "Parsed non-Sonoff cluster descMap -> $descMap"
        }
    } 
    return []
}

private int getSonoffAttributeValue(ArrayList _data) {
    int retValue = 0
    if (_data != null && _data.size() >= 6) {
        def lengthVal = _data[5]
        if (lengthVal != null && lengthVal.toString().isNumber()) {
            int dataLength = lengthVal as Integer
            int power = 1
            if (_data.size() >= (6 + dataLength)) {
                for (i in dataLength..1) {
                    retValue = retValue + power * zigbee.convertHexToInt(_data[i+5])
                    power = power * 256
                }
            }
        }
    }
    return retValue
}

// Garage Door Control Commands (Delegates directly to on() for physical trigger)
def open()  { on() }
def close() { on() }

def on() {
    logDebug "Turning ON switch"
    sendSwitchEvent("on", true)
    relayOn()
}

def relayOn() {
    logDebug "Turning the relay ON with hardware pulse duration (${PULSE_TIMER}ms)"
    int tenthsOfSec = Math.round(PULSE_TIMER / 100.0) as int
    String hexTime = hubitat.helper.HexUtils.integerToHexString(tenthsOfSec, 2)
    String formattedTime = hexTime.substring(2,4) + hexTime.substring(0,2)
    
    List<String> cmds = []
    cmds << "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0x42 {00 ${formattedTime} 0000}"
    sendZigbeeCommands(cmds)
    
    pulseOn()
}

def pulseOn() {
    logDebug "pulseOn() software timer started (${PULSE_TIMER}ms)"
    runInMillis(PULSE_TIMER, 'off', [overwrite: true])
}

def off() {
    logDebug "Turning OFF switch"
    sendSwitchEvent("off", true)
    relayOff()
}

def relayOff() {
    logDebug "Turning the relay OFF"
    sendZigbeeCommands(zigbee.command(0x0006, 0x00, "00010101000100"))
}

def refresh() {
    logInfo "refresh() requested"
    List<String> cmds = []
    cmds += zigbee.readAttribute(0x0000, 0x0007) // Power Source
    cmds += zigbee.readAttribute(0x0006, 0x0000) // Switch State
    cmds += zigbee.readAttribute(0x0006, 0x8001) // Network Indicator LED
    cmds += zigbee.readAttribute(0x0006, 0x8002) // Power-On Behavior
    cmds += zigbee.readAttribute(0x0006, 0x8005) // Turbo RF Mode
    logDebug "refresh(): Generated Zigbee read commands -> ${cmds}"
    return cmds
}

def sendSwitchEvent(stateVal, isDigital=false) {
    String typeStr = isDigital ? "digital" : "physical"
    sendIfChanged([name: "switch", value: stateVal, type: typeStr, descriptionText: "switch is ${stateVal} (${typeStr})"])
}

void setupSchedule() {
    unschedule("checkIn")
    String interval = settings?.checkInInterval ?: "3"
    
    switch (interval) {
        case "1":
            schedule("0 0 * ? * * *", "checkIn")
            logDebug "Scheduled passive check-in every hour"
            break
        case "3":
            schedule("0 0 */3 ? * * *", "checkIn")
            logDebug "Scheduled passive check-in every 3 hours"
            break
        case "6":
            schedule("0 0 */6 ? * * *", "checkIn")
            logDebug "Scheduled passive check-in every 6 hours"
            break
        case "12":
            schedule("0 0 */12 ? * * *", "checkIn")
            logDebug "Scheduled passive check-in every 12 hours"
            break
        case "24":
            schedule("0 0 3 ? * * *", "checkIn")
            logDebug "Scheduled passive check-in daily at 3:00 AM"
            break
        default:
            logDebug "Scheduled passive check-in disabled"
            break
    }
}

def checkIn() {
    logDebug "Executing scheduled passive check-in evaluation"

    String intervalSetting = settings?.checkInInterval ?: "3"
    if (intervalSetting == "0") return

    Integer checkInHours = intervalSetting.toInteger()
    Long allowedThresholdMs = (checkInHours * 1.5 * 3600 * 1000) as Long
    Long lastActivity = state.lastActivityTime != null ? (state.lastActivityTime as Long) : 0L

    if (lastActivity == 0L) {
        logDebug "Check-in evaluation: No activity recorded yet; leaving healthStatus as unknown"
        return
    }

    Long elapsedMs = now() - lastActivity

    if (elapsedMs > allowedThresholdMs) {
        logWarn "No activity received in ${(elapsedMs / 3600000.0).setScale(1, BigDecimal.ROUND_HALF_UP)} hours (threshold: ${(checkInHours * 1.5)} hours). Device marked offline."
        sendIfChanged([name: "healthStatus", value: "offline", descriptionText: "healthStatus set to offline (stale activity)"])
    } else {
        logDebug "Passive check-in verified active (last activity ${(elapsedMs / 60000.0).setScale(1, BigDecimal.ROUND_HALF_UP)} minutes ago)"
        sendIfChanged([name: "healthStatus", value: "online", descriptionText: "healthStatus is online"])
    }
}

// Write Configured Sonoff Preferences to Device
private List<String> syncDevicePreferences() {
    List<String> cmds = []
    
    // Power On State (0x8002 Enum8)
    int pStateVal = (settings?.powerOnState == "on") ? 1 : ((settings?.powerOnState == "previous") ? 2 : 0)
    cmds += zigbee.writeAttribute(0x0006, 0x8002, DataType.ENUM8, pStateVal)
    
    // Network Indicator LED (0x8001 Boolean)
    int ledVal = (settings?.networkIndicator == "off") ? 0 : 1
    cmds += zigbee.writeAttribute(0x0006, 0x8001, DataType.BOOLEAN, ledVal)
    
    // Turbo Mode (0x8005 Boolean)
    int turboVal = (settings?.turboMode == "enabled") ? 1 : 0
    cmds += zigbee.writeAttribute(0x0006, 0x8005, DataType.BOOLEAN, turboVal)
    
    return cmds
}

// Hubitat Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    sendEvent(name: "healthStatus", value: "unknown")
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
    
    List<String> syncCmds = syncDevicePreferences()
    if (syncCmds) {
        logDebug "Syncing preference settings to device -> ${syncCmds}"
        sendZigbeeCommands(syncCmds)
    }
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)

    List<String> cmds = tuyaBlackMagic()
    cmds += syncDevicePreferences()
    cmds += zigbee.readAttribute(0x0000, 0x0007)
    cmds += zigbee.readAttribute(0x0006, 0x8001)
    cmds += zigbee.readAttribute(0x0006, 0x8002)
    cmds += zigbee.readAttribute(0x0006, 0x8005)
    logDebug "configure(): Generated Zigbee command payload -> ${cmds}"
    return cmds
}

private void initialize(Boolean isInstall = false) {
    state.lastInitializedVersion = version()
    sendEvent(name: "driverVersion", value: version())

    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

    setupSchedule()
    
    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging enabled. Will automatically turn off in 30 minutes."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// Master Utility Routine for Driver GUI Button
void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    initialize(false)
    logInfo "Driver reset process completed and re-initialized."
}

// Individual Utility Routines
void clearAllDriverStates() {
    logInfo "Clearing all driver states..."
    state.clear()
    logInfo "All states have been cleared."
}

void clearAllAttributes() {
    logInfo "Clearing all attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
    logInfo "All attributes have been cleared."
}

void clearAllSchedules() {
    logInfo "Clearing all scheduled jobs (including orphaned schedules)..."
    unschedule()
    logInfo "All scheduled jobs have been successfully cleared."
}

def tuyaBlackMagic() {
    return zigbee.readAttribute(0x0000, [0x0004, 0x0000, 0x0001, 0x0005, 0x0007, 0xfffe], [:], 200)
}

void sendZigbeeCommands(List<String> cmds) {
    logTrace "sendZigbeeCommands : ${cmds}"
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

// State-De-Duplication Helper Routine
private void sendIfChanged(Map args) {
    if (!args || !args.name) return

    String nameStr = args.name as String
    String oldVal = device.currentValue(nameStr)?.toString()
    String newVal = args.value != null ? args.value.toString() : ""

    if (oldVal != newVal) {
        String desc = args.descriptionText ?: "${nameStr} set to ${args.value}"
        Map eventMap = [
            name: nameStr, 
            value: args.value, 
            descriptionText: desc
        ]
        if (args.unit) eventMap.unit = args.unit
        if (args.type) eventMap.type = args.type
        if (args.isStateChange != null) eventMap.isStateChange = args.isStateChange

        sendEvent(eventMap)
        logInfo "${desc}"
        logDebug "Event triggered: ${nameStr} -> ${args.value}"
    }
}

// Centralized Logging Engine
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${devName}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    return settings[key] != null ? settings[key] as Boolean : defaultVal
}