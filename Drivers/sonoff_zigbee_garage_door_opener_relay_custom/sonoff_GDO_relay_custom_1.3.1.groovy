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

static String version() { return '1.3.1' }
def timeStamp() { return "2026/08/30 11:30 AM" }

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
        attribute "lastPing", "string"
        attribute "pingStatus", "string"

        // Custom Commands
        command "ping"
        command "resetDriver"

        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0B05,FC57,FC11", outClusters:"0003,0006,0019", model:"MINI-ZBD", manufacturer:"SONOFF", controllerType: "ZGB", deviceJoinName: "Sonoff Garage Door Opener Relay"
    }

    preferences {
        input name: "healthCheckInterval", type: "enum", title: "Health Check Schedule", description: "Automatically ping device on a regular interval to verify presence.", options: ["0":"Disabled", "1":"Every Hour", "3":"Every 3 Hours", "6":"Every 6 Hours", "12":"Every 12 Hours", "24":"24 Hours"], defaultValue: "3", required: true
        
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
   
        if (descMap?.clusterInt == CLUSTER_SONOFF) {
            logDebug "Parse Sonoff Cluster descMap -> ${descMap}"
            if (descMap?.command in ["00", "01", "02"]) {
                def fncmd = getSonoffAttributeValue(descMap?.data)
                def dp = descMap?.data && descMap.data.size() > 2 ? zigbee.convertHexToInt(descMap?.data[2]) : null
                def dp_id = descMap?.data && descMap.data.size() > 3 ? zigbee.convertHexToInt(descMap?.data[3]) : null
                
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
                        logWarn "UNPROCESSED Sonoff cmd: dp=${dp} value=${fncmd} descMap.data = ${descMap?.data}"
                        break
                }
            } 
            else if (descMap?.command == "0B") {    
                logDebug "ZCL response: 0x${descMap?.data[1]} status: ${descMap?.data[1]=='00'?'success':'FAILURE'} data: ${descMap?.data}"
            } else {
                logWarn "UNPROCESSED COMMAND Sonoff cmd ${descMap?.command} : descMap.data = ${descMap?.data}"
            }
        } 
        else if ((descMap?.cluster == "0000" || descMap?.clusterInt == 0) && (descMap?.attrInt == 0 || descMap?.attrId == "0000")) {
            unschedule('pingTimeout')
            sendIfChanged([name: "pingStatus", value: "Responded", descriptionText: "ping status is Responded"])
            logInfo "Health check response / presence confirmed."
        }
        else if (descMap?.cluster == "0000" && (descMap?.attrInt == 1 || descMap?.attrId == "0001")) {
            logDebug "Sonoff check-in: ${descMap}"
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
            // Requires array size to be at least (6 + dataLength) to safely access _data[i+5]
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
    logDebug "Turning the relay ON"
    sendZigbeeCommands(zigbee.command(0x0006, 0x01, "00010101000101"))
    pulseOn()
}

def pulseOn() {
    logDebug "pulseOn() timer started (${PULSE_TIMER}ms)"
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
    return ping()
}

def ping() {
    String timestamp = getTimestamp()
    sendIfChanged([name: "lastPing", value: timestamp, descriptionText: "last ping timestamp updated"])
    sendIfChanged([name: "pingStatus", value: "Pending", descriptionText: "ping status set to Pending"])
    
    logInfo "Sending health check ping (Basic Cluster read)..."
    
    runIn(10, 'pingTimeout', [overwrite: true])
    return zigbee.readAttribute(0x0000, 0x0000)
}

def pingTimeout() {
    sendIfChanged([name: "pingStatus", value: "Timeout", descriptionText: "ping status set to Timeout"])
    logWarn "Health check ping timed out (no response received)."
}

def sendSwitchEvent(stateVal, isDigital=false) {
    String typeStr = isDigital ? "digital" : "physical"
    sendIfChanged([name: "switch", value: stateVal, type: typeStr, descriptionText: "switch is ${stateVal} (${typeStr})"])
}

void scheduleHealthCheck() {
    unschedule("healthCheck")
    String interval = settings?.healthCheckInterval ?: "3"
    
    switch (interval) {
        case "1":
            schedule("0 0 * ? * * *", "healthCheck")
            logDebug "Scheduled health check every hour"
            break
        case "3":
            schedule("0 0 */3 ? * * *", "healthCheck")
            logDebug "Scheduled health check every 3 hours"
            break
        case "6":
            schedule("0 0 */6 ? * * *", "healthCheck")
            logDebug "Scheduled health check every 6 hours"
            break
        case "12":
            schedule("0 0 */12 ? * * *", "healthCheck")
            logDebug "Scheduled health check every 12 hours"
            break
        case "24":
            schedule("0 0 3 ? * * *", "healthCheck")
            logDebug "Scheduled health check daily at 3:00 AM"
            break
        default:
            logDebug "Scheduled health checks disabled"
            break
    }
}

def healthCheck() {
    logDebug "Executing scheduled health check..."
    ping()
}

// Hubitat Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
}

def configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device..."
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)

    scheduleHealthCheck()
    List<String> cmds = tuyaBlackMagic()
    logDebug "configure(): Generated Zigbee command payload -> ${cmds}"
    return cmds
}

private void initialize(Boolean isInstall = false) {
    state.lastInitializedVersion = version()
    sendEvent(name: "driverVersion", value: version())
    scheduleHealthCheck()
    
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