/*
SmartThings Motion Sensor Enhanced Custom

Version: 1.7.9
Author: jshimota
Namespace: jdthomas24

Supported Models:
- STS-IRM-250 (motionv4)
- STS-IRM-251 (motionv5)
- GP-AEOMSSUS (Aeotec Zigbee motion)
- GP-U999SJVLBAA (Samsung SmartThings motion)

Enhancements:
- Motion auto reset with race condition fix
- Optional temperature reporting (enableTemp)
- Battery voltage curve with 5% increments & smoothing
- Battery reporting interval in minutes (converted to seconds for Zigbee)
- LQI/RSSI signal monitoring with route health rating
- Health Check ping() implementation
- Debug logging auto-disables after 30 minutes
- Temperature logging suppressed when enableTemp is off

Changes is 1.7.9:
- Gemini modernization caught errors abd bugs.

Changes in 1.7.8:
- Updated battery voltage curve for CR2 lithium chemistry
  Previous curve was tuned for CR2450/CR2477 coin cells — incorrect for these devices
  New curve reflects CR2 discharge profile: 3.0V fresh, ~2.8V plateau, 2.0V cutoff
  Steeper end-of-life drop below 2.5V matches CR2 real-world behavior
- Default battery reporting interval changed from 60 to 240 minutes
  Reduces overly frequent reporting that caused Battery Monitor drain outlier rejection
  Existing installs are unaffected — only applies on fresh install

Changes in 1.7.7:
- configure() now only fires when enableTemp or batteryReportMinutes change —
  not on every updated() save, which could interrupt the device reporting cycle
- Toggling enableTemp off sends a null temperature event with isStateChange: true
  so dashboards and apps see the attribute clear immediately
- Toggling enableTemp on calls configure() + refresh() so temperature populates
  immediately without waiting for the next natural device report
- batteryVoltage event now only fires on change, consistent with battery %
- Removed presenceTimeoutCheck() stub — safe to drop now that all devices
  running v1.7.4 will have saved preferences and cleared the stale timer

Changes in 1.7.6:
- Removed presence detection — was causing interference with device reporting
  due to aggressive runIn() scheduling on every parse event
- Removed lastCheckin attribute — redundant with Hubitat built-in Last Activity
- Removed zigbeeHealth and missedCheckins — were presence-driven, no data source
- Tightened battery voltage curve to 5% increments for cleaner reporting
- Temperature events fully suppressed (no log, no event) when enableTemp is off
- Fixed driverVersion() returning "1.7.5" (was mismatched with header)
- Added isStateChange: true to temperature sendEvent so repeated identical
  readings are still logged — prevents gaps in home page temperature graphs
*/

import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType

def driverVersion() { return "1.7.9" }

metadata {
    definition(
        name: "SmartThings Motion Sensor Enhanced Custom",
        namespace: "jshimota",
        author: "jdthomas24"
    ) {
        capability "Battery"
        capability "Configuration"
        capability "MotionSensor"
        capability "Initialize"
        capability "Refresh"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "Health Check"

        attribute "batteryVoltage", "number"
        attribute "lqi",            "number"
        attribute "rssi",           "number"
        attribute "routeHealth",    "string"

        fingerprint inClusters:"0000,0001,0003,000F,0020,0402,0500", model:"motionv4",        manufacturer:"SmartThings"
        fingerprint inClusters:"0000,0001,0003,000F,0020,0402,0500", model:"motionv5",        manufacturer:"SmartThings"
        fingerprint inClusters:"0000,0001,0003,000F,0020,0402,0500", model:"GP-AEOMSSUS",     manufacturer:"Aeotec"
        fingerprint inClusters:"0000,0001,0003,000F,0020,0402,0500", model:"GP-U999SJVLBAA", manufacturer:"Samsung"
    }

    preferences {
        input name: "motionReset",          type: "number",  title: "Motion Reset Time (seconds)",          defaultValue: 30
        input name: "enableTemp",           type: "bool",    title: "Enable Temperature Reporting",         defaultValue: true
        input name: "tempAdj",              type: "decimal", title: "Temperature Offset",                   defaultValue: 0
        input name: "batteryReportMinutes", type: "enum",
              title: "Battery Reporting Interval (minutes)",
              description: "How often the device reports battery. Converted to seconds for Zigbee reporting.",
              options: ["30","60","120","240","360"],
              defaultValue: "240"
        input name: "infoLogging",  type: "bool", title: "Enable Info Logging",                               defaultValue: true
        input name: "debugLogging", type: "bool", title: "Enable Debug Logging (auto-disables after 30 min)", defaultValue: false
    }
}

def installed() {
    log.info "${device.displayName}: Installed driver v${driverVersion()}"
    scheduleDebugAutoOff()
    initialize()
}

def initialize() {
    runIn(2, configure)
    runIn(7, refresh)
}

def updated() {
    log.info "${device.displayName}: Updated driver v${driverVersion()}"

    unschedule()

    ["presence", "lastCheckin", "checkinInterval", "presenceTimeout",
     "missedCheckins", "zigbeeHealth"].each { attr ->
        device.deleteCurrentState(attr)
    }

    state.remove("lastCheckin")
    state.remove("checkinHistory")
    state.remove("avgCheckin")
    state.remove("missed")

    scheduleDebugAutoOff()

    def prevTemp    = state.prevEnableTemp
    def prevBattInt = state.prevBatteryReportMinutes
    def tempChanged = (prevTemp    != null && prevTemp    != enableTemp)
    def battChanged = (prevBattInt != null && prevBattInt != batteryReportMinutes)
    def firstSave = (prevTemp == null)

    if (firstSave || tempChanged || battChanged) {
        if (debugLogging) log.debug "${device.displayName}: configure() triggered — firstSave:${firstSave} tempChanged:${tempChanged} battChanged:${battChanged}"
        configure()
    } else {
        if (debugLogging) log.debug "${device.displayName}: Skipping configure() — no relevant settings changed"
    }

    if (tempChanged) {
        if (enableTemp) {
            if (infoLogging) log.info "${device.displayName}: Temperature reporting enabled — refreshing"
            runIn(2, refresh)
        } else {
            if (infoLogging) log.info "${device.displayName}: Temperature reporting disabled — clearing attribute"
            sendEvent(name: "temperature", value: null, isStateChange: true, descriptionText: "Temperature reporting disabled")
        }
    }

    state.prevEnableTemp            = enableTemp
    state.prevBatteryReportMinutes  = batteryReportMinutes
}

def configure() {
    def battInterval = (batteryReportMinutes ?: "240").toInteger() * 60

    def cmds = []
    cmds += zigbee.batteryConfig()
    cmds += zigbee.configureReporting(0x0500, 0x0002, DataType.BITMAP16, 30, 3600, null)
    cmds += zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 30, battInterval, 1)

    if (enableTemp) {
        cmds += zigbee.temperatureConfig(30, 1800)
    }

    cmds += zigbee.enrollResponse()
    sendZigbeeCommands(cmds)
}

def refresh() {
    def cmds = []
    cmds += zigbee.readAttribute(0x0001, 0x0020)
    cmds += zigbee.readAttribute(0x0500, 0x0002)
    if (enableTemp) cmds += zigbee.readAttribute(0x0402, 0x0000)
    sendZigbeeCommands(cmds)
}

private void scheduleDebugAutoOff() {
    unschedule("disableDebugLogging")
    if (debugLogging) {
        log.warn "${device.displayName}: Debug logging enabled — will auto-disable in 30 minutes"
        runIn(1800, "disableDebugLogging")
    }
}

def disableDebugLogging() {
    log.info "${device.displayName}: Auto-disabling debug logging after 30 minutes"
    device.updateSetting("debugLogging", [value: "false", type: "bool"])
}

def ping() {
    if (debugLogging) log.debug "${device.displayName}: ping() — refreshing device state"
    refresh()
}

def parse(String description) {
    if (!description) return

    Map descMap = zigbee.parseDescriptionAsMap(description)
    
    // Safely parse Hex values for LQI/RSSI to avoid NumberFormatException
    if (descMap?.lqi)  { 
        def lqiInt = Integer.parseInt(descMap.lqi, 16)
        sendEvent(name: "lqi", value: lqiInt, descriptionText: "LQI is ${lqiInt}")
        updateRouteHealth(lqiInt) 
    }
    if (descMap?.rssi) { 
        def rssiInt = Integer.parseInt(descMap.rssi, 16)
        // Convert to signed 8-bit value if RSSI is returned as unsigned hex above 127
        if (rssiInt > 127) rssiInt -= 256
        sendEvent(name: "rssi", value: rssiInt, descriptionText: "RSSI is ${rssiInt} dBm") 
    }

    if (description.startsWith("zone status")) {
        ZoneStatus status = zigbee.parseZoneStatus(description)
        processMotion(status)
        return
    }

    if (descMap?.cluster == "0001" && descMap?.attrId == "0020") {
        def rawVolts = Integer.parseInt(descMap.value, 16) / 10.0
        def volts    = smoothBattery(rawVolts)
        
        if (device.currentValue("batteryVoltage") != volts) {
            sendEvent(name: "batteryVoltage", value: volts, unit: "V", descriptionText: "Battery voltage is ${volts}V")
        }
        def pct = calculateBattery(volts)
        if (device.currentValue("battery") != pct) {
            def descText = "${device.displayName}: Battery ${pct}% (${volts}V)"
            if (infoLogging) log.info descText
            sendEvent(name: "battery", value: pct, unit: "%", descriptionText: descText)
        }
        return
    }

    // Process temperature reports safely
    def evt = zigbee.getEvent(description)
    if (!evt) return

    if (evt.name == "temperature") {
        if (!enableTemp) return
        Double offset = tempAdj ?: 0
        def temp = (evt.value + offset).round(2)
        def descText = "${device.displayName}: Temperature ${temp}°${evt.unit}"
        if (infoLogging) log.info descText
        sendEvent(name: "temperature", value: temp, unit: evt.unit, isStateChange: true, descriptionText: descText)
        return
    }
}

def processMotion(ZoneStatus status) {
    if (status.isAlarm1Set()) {
        unschedule("motionInactive")
        def descText = "${device.displayName}: Motion active"
        if (infoLogging) log.info descText
        sendEvent(name: "motion", value: "active", descriptionText: descText)

        def resetTime = (motionReset ?: 30).toInteger()
        if (resetTime > 0) runIn(resetTime, motionInactive)
    } else {
        if (debugLogging) log.debug "${device.displayName}: Hardware inactive received — waiting for motionReset timer (${motionReset ?: 30}s)"
    }
}

def motionInactive() {
    def descText = "${device.displayName}: Motion inactive"
    if (infoLogging) log.info descText
    sendEvent(name: "motion", value: "inactive", descriptionText: descText)
}

def updateRouteHealth(Integer lqi) {
    def health = lqi >= 150 ? "Excellent" :
                 lqi >= 100 ? "Good" :
                 lqi >= 60  ? "Weak" : "Poor"
    sendEvent(name: "routeHealth", value: health, descriptionText: "Route health rated as ${health}")
    if (debugLogging) log.debug "${device.displayName}: LQI=${lqi} routeHealth=${health}"
}

def calculateBattery(Double voltage) {
    if (voltage >= 3.00) return 100
    if (voltage >= 2.95) return 99  
    if (voltage >= 2.90) return 95
    if (voltage >= 2.85) return 90
    if (voltage >= 2.80) return 85  
    if (voltage >= 2.75) return 75
    if (voltage >= 2.70) return 65
    if (voltage >= 2.65) return 55
    if (voltage >= 2.60) return 45
    if (voltage >= 2.55) return 35
    if (voltage >= 2.50) return 28
    if (voltage >= 2.45) return 22
    if (voltage >= 2.40) return 17
    if (voltage >= 2.35) return 13
    if (voltage >= 2.30) return 9
    if (voltage >= 2.25) return 6
    if (voltage >= 2.20) return 4
    if (voltage >= 2.10) return 2
    if (voltage >= 2.00) return 1
    return 0
}

def smoothBattery(Double voltage) {
    if (!state.lastVolt) { state.lastVolt = voltage; return voltage }
    def smoothed = (state.lastVolt + voltage) / 2
    state.lastVolt = smoothed
    return smoothed.round(2)
}

void sendZigbeeCommands(List cmds) {
    if (!cmds) return
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}
