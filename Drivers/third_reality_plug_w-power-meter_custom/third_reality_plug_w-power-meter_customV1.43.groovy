/**
 * Third Reality Plug w/Power Meter Support (Custom)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Unified driver supporting both Third Reality Power Meter Plugs (3RSP02028BZ) 
 * and standard non-power-metering Zigbee Plugs (3RSP019BZ). Intelligently detects the device 
 * model ID to adapt configuration parameters, attribute reads, reporting verification, and 
 * capability event emissions—ensuring non-power plugs operate cleanly without sending 
 * unsupported cluster commands.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability.
 * - Hubitat's native capability exposes an redundant "Ping" UI control button
 *   and does not provide the phase-anchored scheduling, timeout guards, or trace 
 *   logging behavior required by this driver architecture.
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
 * Changelog:
 * v1.43    08/31/26    jshimota    Updated Health Check interval preference label from 'Once a Day' to 'Every 24 Hours'.
 * v1.42    08/31/26    jshimota    Enforced immediate Health Check execution on configure() and guaranteed healthStatus state initialization during resetDriver() / initialize().
 * v1.41    08/31/26    jshimota    Added Notes block to header documenting custom Health Check rationale and standardized internal Health Check template routines.
 * v1.40    08/31/26    jshimota    Added clarifying note in HealthCheckInterval preference indicating custom driver implementation vs native Hubitat Health Check capability.
 * v1.39    08/31/26    jshimota    Removed capability "Health Check" to suppress Hubitat's built-in ping button in favor of custom Health Check command.
 * v1.38    08/31/26    jshimota    Isolated ping routine to private helper to prevent Hubitat UI command duplication.
 * v1.37    08/31/26    jshimota    Fixed parseWriteAttributeResponse self-reference bug, added attrInt NPE guards, cleaned state removals, documented calculated PF rationale.
 * v1.36    08/31/26    jshimota    Updated GUI command button to "Health Check" for a cleaner interface.
 * v1.35    08/31/26    jshimota    Fixed Hubitat IDE compilation hang by stripping parentheses from command declaration string.
 * v1.34    08/31/26    jshimota    Renamed Ping GUI command button to "Health Check (Initiate Ping/Pong)".
 * v1.33    08/31/26    jshimota    Consolidated version state variables (lastLoggedVersion / lastInitializedVersion) into a single state.driverVersion.
 * v1.32    08/31/26    jshimota    Removed legacy 'disableOnOff' preference and simplified switch execution paths.
 * v1.31    08/31/26    jshimota    Restored powerMeterCapable attribute for GUI visibility while keeping internal multipliers isolated to state memory.
 * v1.30    08/31/26    jshimota    Architectural overhaul: Removed unnecessary attributes/states, flattened multiplier state variables, and introduced persistent randomized phase-anchor health check scheduling.
**/
// [KEEP-EXACT] See possible changelog.txt for past changelog history versions v0 - v1.29

static String version() { return '1.43' }
def timeStamp() { return "2026/08/31 08:30 PM" }

import groovy.transform.Field
import hubitat.zigbee.zcl.DataType
import java.math.RoundingMode

metadata {
    definition(
        name: "Third Reality Plug w/Power Meter Support (Custom)",
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/third_reality_outlet_custom/third_reality_plug_w-power-meter_custom.groovy"
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Current Meter"
        capability "Energy Meter"
        capability "Outlet"
        capability "Power Meter"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "Voltage Measurement"

        command "Health Check"
        command "toggle"
        command "updateFirmware"
        command "resetDriver"

        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "powerFactor", "number"
        attribute "powerMeterCapable", "enum", ["Yes", "No"]

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,FF03,0003,0004,0005,0006", outClusters: "0019", model: "3RSP019BZ", manufacturer: "Third Reality, Inc", controllerType: "ZGB"
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,FF03,0003,0004,0005,0006,0B04,0702", outClusters: "0019", model: "3RSP02028BZ", manufacturer: "Third Reality, Inc", controllerType: "ZGB"
    }

    preferences {
        input name: "powerRestore", type: "enum", title: "<b>Power Restore Mode</b>", options: PowerRestoreOpts.options, defaultValue: PowerRestoreOpts.defaultValue, description: "<i>Changes what happens when power is restored to outlet.</i>"
        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check (Ping/Pong) Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver sends a Health Check Ping to verify device online status.<br><b>Note:</b> This is a custom driver ping/pong routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"

        input name: "powerDelta", type: "number", title: "<b>Power Minimum Change </b><i>(Power Meter Capable Models Only)</i>", description: "<i>The minimum Power (watts) change that will be recorded (3RSP02028BZ model only).</i>", range: "0.1..1500"
        input name: "energyDelta", type: "number", title: "<b>Energy Minimum Change </b><i>(Power Meter Capable Models Only)</i>", description: "<i>The minimum energy kWh change that will be recorded (3RSP02028BZ model only).</i>", range: "0.1..100"
        input name: "amperageDelta", type: "number", title: "<b>Amperage Minimum Change </b><i>(Power Meter Capable Models Only)</i>", description: "<i>The minimum amperage change that will be recorded (3RSP02028BZ model only).</i>", range: "0.1..15"
        input name: "voltageDelta", type: "number", title: "<b>Voltage Minimum Change </b><i>(Power Meter Capable Models Only)</i>", description: "<i>The minimum voltage change that will be recorded (3RSP02028BZ model only).</i>", range: "1..100"

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// Model Detection Helper Routine (Direct Model Matching + Attribute Emitter)
private boolean supportsPowerMeter() {
    String devModel = device.getDataValue("model")?.trim()
    Boolean isPowerCapable = POWER_METER_MODEL.equalsIgnoreCase(devModel)
    
    updateAttribute("powerMeterCapable", isPowerCapable ? "Yes" : "No", null, "digital")
    return isPowerCapable
}

// Single-Shot Version Demarcation Trace Logging Helper Routine
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.driverVersion != currentVer) {
        logTrace "=================== DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.driverVersion = currentVer
    }
}

// NPE-Safe BigDecimal Preference Conversion Helper
private BigDecimal getSettingBigDecimal(String key) {
    Object val = settings[key]
    if (val == null) return null
    try {
        return new BigDecimal(val.toString())
    } catch (Exception e) {
        logWarn "Failed to parse numerical preference [${key}]: ${val}"
        return null
    }
}

/* =========================================================================================
   HUBITAT LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing driver v${version()} (${timeStamp()})..."
    
    initializeHealthCheckPhase()

    sendEvent(name: "healthStatus", value: "unknown")
    sendEvent(name: "switch", value: "off")

    if (supportsPowerMeter()) {
        logInfo "Device model [${POWER_METER_MODEL}] detected: Power metering capabilities enabled."
        sendEvent(name: "amperage", value: 0, unit: "A")
        sendEvent(name: "energy", value: 0, unit: "kWh")
        sendEvent(name: "frequency", value: 0, unit: "Hz")
        sendEvent(name: "power", value: 0, unit: "W")
        sendEvent(name: "voltage", value: 0, unit: "V")
        sendEvent(name: "powerFactor", value: 0)
    } else {
        logInfo "Device model [${device.getDataValue('model') ?: 'Standard'}] detected: Operating in standard plug mode (no power metering)."
    }

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"
    
    state.remove("energyInKwh")
    state.remove("lastPowerUpdate")
    state.remove("supportsPowerMeter")
    state.remove("attributes")
    state.remove("scheduleSegment")
    state.remove("scheduleMinuteOffset")

    initialize(false)
    runIn(1, "configure")
}

List<String> configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device (Power Meter Capable: ${supportsPowerMeter()})..."
    initialize(false)

    List<String> cmds = []

    if (settings.powerRestore != null) {
        cmds += zigbee.writeAttribute(zigbee.ON_OFF_CLUSTER, POWER_RESTORE_ID, DataType.ENUM8, settings.powerRestore as Integer, [:], DELAY_MS)
    }

    // Configure mandatory Zigbee On/Off reporting for all models
    cmds += zigbee.configureReporting(zigbee.ON_OFF_CLUSTER, POWER_ON_OFF_ID, DataType.BOOLEAN, 0, 3600, null, [:], DELAY_MS)

    // Configure Power Metering reporting if supported
    if (supportsPowerMeter()) {
        logDebug "Configuring Zigbee reporting for electrical measurement and metering clusters..."
        cmds += zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ACTIVE_POWER_ID, DataType.INT16, 10, 300, 10, [:], DELAY_MS)
        cmds += zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, RMS_CURRENT_ID, DataType.UINT16, 10, 300, 10, [:], DELAY_MS)
        cmds += zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, RMS_VOLTAGE_ID, DataType.UINT16, 10, 300, 1, [:], DELAY_MS)
        cmds += zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, AC_FREQUENCY_ID, DataType.UINT16, 10, 300, 1, [:], DELAY_MS)
        cmds += zigbee.configureReporting(zigbee.METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET, DataType.UINT48, 10, 300, 1, [:], DELAY_MS)
    }

    // Immediately execute a Health Check to establish online healthStatus right away
    cmds += executePing()

    runIn(5, "refresh")
    return cmds
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    unschedule("disableDebugLogging")

    // Ensure healthStatus attribute exists on initialize/reset
    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

    // Centralized Health Check Scheduler
    final int interval = settings.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 480
    if (interval > 0) {
        scheduleHealthCheck("executePing", interval)
    } else {
        unschedule("executePing")
    }

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

/* =========================================================================================
   COMMAND IMPLEMENTATIONS
   ========================================================================================= */

List<String> off() {
    logInfo "Turning off..."
    state.isDigital = true
    return zigbee.off()
}

List<String> on() {
    logInfo "Turning on..."
    state.isDigital = true
    return zigbee.on()
}

List<String> toggle() {
    logInfo "Toggling switch..."
    state.isDigital = true
    return zigbee.command(zigbee.ON_OFF_CLUSTER, 0x02, [:], 0)
}

/* =========================================================================================
   HEALTH CHECK ROUTINE TEMPLATE (CUSTOM DRIVER PING/PONG ARCHITECTURE)
   ========================================================================================= */

/**
 * Public GUI Command Entry Point.
 * Single entry point exposed on the Device Detail page to avoid Hubitat UI button duplication.
 **/
List<String> "Health Check"() {
    return executePing()
}

/**
 * Private Ping Execution Helper.
 * Transmits the underlying Zigbee Basic Cluster ping request, registers a timeout check,
 * and remains hidden from the Hubitat GUI command interface.
 **/
private List<String> executePing() {
    logDebug "Health Check Ping sent..."
    scheduleCommandTimeoutCheck()
    return zigbee.readAttribute(zigbee.BASIC_CLUSTER, PING_ATTR_ID, [:], 0)
}

/**
 * Modular Health Check Scheduler with Persistent Phase-Anchoring.
 * Anchors check schedules to a persistent random daily time offset to stagger hub network traffic.
 **/
private void initializeHealthCheckPhase() {
    if (state.healthCheckStartHour == null) state.healthCheckStartHour = new Random().nextInt(24)
    if (state.healthCheckStartMinute == null) state.healthCheckStartMinute = new Random().nextInt(60)
}

private void scheduleHealthCheck(String methodToSchedule, int intervalMin) {
    unschedule(methodToSchedule)
    initializeHealthCheckPhase()

    final int h = state.healthCheckStartHour as Integer
    final int m = state.healthCheckStartMinute as Integer

    logInfo "Scheduling Health Check every ${intervalMin} minutes anchored at ${String.format('%02d:%02d', h, m)} daily"

    switch (intervalMin) {
        case 60:
            schedule("0 ${m} * ? * * *", methodToSchedule)
            break
        case 240:
            String h4 = [0, 4, 8, 12, 16, 20].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h4} ? * * *", methodToSchedule)
            break
        case 480:
            String h8 = [0, 8, 16].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h8} ? * * *", methodToSchedule)
            break
        case 720:
            String h12 = [0, 12].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h12} ? * * *", methodToSchedule)
            break
        case 1440:
            schedule("0 ${m} ${h} ? * * *", methodToSchedule)
            break
        default:
            if (intervalMin >= 60) {
                int hours = intervalMin / 60
                schedule("0 ${m} */${hours} ? * * *", methodToSchedule)
            } else {
                schedule("0 */${intervalMin} * ? * * *", methodToSchedule)
            }
            break
    }
}

private void scheduleCommandTimeoutCheck(final int delay = COMMAND_TIMEOUT) {
    runIn(delay, "deviceCommandTimeout")
}

void deviceCommandTimeout() {
    logWarn "No Health Check Pong received (device offline?)"
    updateAttribute("healthStatus", "offline")
}

/* =========================================================================================
   ZIGBEE MESSAGE PARSING
   ========================================================================================= */

List<String> refresh() {
    logDebug "Executing refresh()..."
    List<String> cmds = []

    cmds += zigbee.readAttribute(zigbee.BASIC_CLUSTER, FIRMWARE_VERSION_ID, [:], DELAY_MS)
    cmds += zigbee.readAttribute(zigbee.ON_OFF_CLUSTER, POWER_RESTORE_ID, [:], DELAY_MS)
    cmds += zigbee.readAttribute(zigbee.ON_OFF_CLUSTER, POWER_ON_OFF_ID, [:], DELAY_MS)

    if (supportsPowerMeter()) {
        logDebug "Requesting electrical measurement and metering cluster attributes..."
        cmds += zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, [
            AC_CURRENT_MULTIPLIER_ID, AC_CURRENT_DIVISOR_ID, AC_VOLTAGE_MULTIPLIER_ID,
            AC_VOLTAGE_DIVISOR_ID, AC_POWER_MULTIPLIER_ID, AC_POWER_DIVISOR_ID
        ], [:], DELAY_MS)

        cmds += zigbee.readAttribute(zigbee.METERING_CLUSTER, [
            METERING_DIVISOR_ID, METERING_UNIT_OF_MEASURE_ID, METERING_SUMMATION_FORMATTING_ID
        ], [:], DELAY_MS)

        cmds += zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, [
            AC_FREQUENCY_ID, RMS_CURRENT_ID, RMS_VOLTAGE_ID, ACTIVE_POWER_ID
        ], [:], DELAY_MS)

        cmds += zigbee.readAttribute(zigbee.METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET, [:], DELAY_MS)
    } else {
        logDebug "Non-power meter model detected: Skipping power and metering cluster reads."
    }

    if (getSettingBool("logDebugEnable", false)) {
        // Verification read for On/Off reporting (all models)
        cmds += zigbee.reportingConfiguration(zigbee.ON_OFF_CLUSTER, POWER_ON_OFF_ID, [:], DELAY_MS)

        // Verification reads for power-metering reporting (3RSP02028BZ only)
        if (supportsPowerMeter()) {
            cmds += zigbee.reportingConfiguration(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ACTIVE_POWER_ID, [:], DELAY_MS)
            cmds += zigbee.reportingConfiguration(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, RMS_CURRENT_ID, [:], DELAY_MS)
            cmds += zigbee.reportingConfiguration(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, RMS_VOLTAGE_ID, [:], DELAY_MS)
            cmds += zigbee.reportingConfiguration(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, AC_FREQUENCY_ID, [:], DELAY_MS)
            cmds += zigbee.reportingConfiguration(zigbee.METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET, [:], DELAY_MS)
        }
    }

    return cmds
}

List<String> updateFirmware() {
    logInfo "Checking for firmware updates..."
    return zigbee.updateFirmware()
}

void parse(final String description) {
    logDebug "Raw description -> ${description}"
    final Map descMap = zigbee.parseDescriptionAsMap(description)
    if (!descMap) return

    if (descMap.profileId == "0000") {
        parseZdoClusters(descMap)
        return
    }

    if (descMap.isClusterSpecific == false) {
        parseGeneralCommandResponse(descMap)
        return
    }

    if (descMap.clusterInt == null) return
    logTrace "Zigbee cluster message: ${clusterLookup(descMap.clusterInt)}${descMap.attrId ? " attr 0x${descMap.attrId} (val ${descMap.value})" : ''}"

    switch (descMap.clusterInt as Integer) {
        case zigbee.BASIC_CLUSTER:
            parseBasicCluster(descMap)
            descMap.remove("additionalAttrs")?.each { final Map map -> parseBasicCluster(descMap + map) }
            break
        case zigbee.ELECTRICAL_MEASUREMENT_CLUSTER:
            if (!supportsPowerMeter()) {
                logTrace "Ignoring Electrical Measurement cluster message for non-power meter device model."
                break
            }
            parseElectricalMeasureCluster(descMap)
            descMap.remove("additionalAttrs")?.each { final Map map -> parseElectricalMeasureCluster(descMap + map) }
            break
        case zigbee.METERING_CLUSTER:
            if (!supportsPowerMeter()) {
                logTrace "Ignoring Metering cluster message for non-power meter device model."
                break
            }
            parseMeteringCluster(descMap)
            descMap.remove("additionalAttrs")?.each { final Map map -> parseMeteringCluster(descMap + map) }
            break
        case zigbee.ON_OFF_CLUSTER:
            parseOnOffCluster(descMap)
            descMap.remove("additionalAttrs")?.each { final Map map -> parseOnOffCluster(descMap + map) }
            break
        default:
            logDebug "Unknown cluster message: ${descMap}"
            break
    }
}

void parseBasicCluster(final Map descMap) {
    if (descMap.attrInt == null) return
    switch (descMap.attrInt as Integer) {
        case PING_ATTR_ID:
            unschedule("deviceCommandTimeout") // Cancel health check timeout ONLY upon verified Pong response
            logDebug "Health Check Pong received..."
            sendEvent(name: "healthStatus", value: "online", isStateChange: true, descriptionText: "${device.displayName} is online")
            break
        case FIRMWARE_VERSION_ID:
            final String versionStr = descMap.value ?: "unknown"
            logDebug "Device firmware version is ${versionStr}"
            updateDataValue("softwareBuild", versionStr)
            break
        default:
            logWarn "Unknown Basic cluster attribute 0x${descMap.attrId} (val ${descMap.value})"
            break
    }
}

void parseElectricalMeasureCluster(final Map descMap) {
    if (descMap.attrInt == null || descMap.value == null || descMap.value == "FFFF") return
    final long value = hexStrToUnsignedInt(descMap.value)
    switch (descMap.attrInt as Integer) {
        case AC_CURRENT_MULTIPLIER_ID: state.currentMultiplier = value; break
        case AC_CURRENT_DIVISOR_ID: state.currentDivisor = value; break
        case AC_POWER_MULTIPLIER_ID: state.powerMultiplier = value; break
        case AC_POWER_DIVISOR_ID: state.powerDivisor = value; break
        case AC_VOLTAGE_MULTIPLIER_ID: state.voltageMultiplier = value; break
        case AC_VOLTAGE_DIVISOR_ID: state.voltageDivisor = value; break
        case AC_FREQUENCY_ID:
            updateAttribute("frequency", value, "Hz", "physical")
            break
        case RMS_CURRENT_ID:
            handleRmsCurrentValue(value)
            break
        case ACTIVE_POWER_ID:
            handleActivePowerValue(value)
            break
        case RMS_VOLTAGE_ID:
            handleRmsVoltageValue(value)
            break
        default:
            logWarn "Unknown Electrical Measurement cluster attribute 0x${descMap.attrId} (val ${descMap.value})"
            break
    }
}

void handleRmsCurrentValue(final long value) {
    final Long multiplier = state.currentMultiplier as Long
    final Long divisor = state.currentDivisor as Long
    
    if (multiplier != null && divisor != null && multiplier > 0 && divisor > 0) {
        final BigDecimal currentValue = device.currentValue("amperage") as BigDecimal
        BigDecimal result = value * multiplier / divisor
        result = result.setScale(1, RoundingMode.HALF_UP)
        
        final BigDecimal inputDelta = getSettingBigDecimal("amperageDelta")
        if (isDelta(result, currentValue, inputDelta)) {
            updateAttribute("amperage", result, "A", "physical")
            updatePowerFactor() 
        }
    }
}

void handleActivePowerValue(final long value) {
    final Long multiplier = state.powerMultiplier as Long
    final Long divisor = state.powerDivisor as Long
    
    if (multiplier != null && divisor != null && multiplier > 0 && divisor > 0) {
        final BigDecimal currentValue = device.currentValue("power") as BigDecimal
        BigDecimal result = (int)value * multiplier / divisor
        result = result.setScale(1, RoundingMode.HALF_UP)
        
        final BigDecimal inputDelta = getSettingBigDecimal("powerDelta")
        if (isDelta(result, currentValue, inputDelta)) {
            updateAttribute("power", result, "W", "physical")
            updatePowerFactor()
        }
    }
}

void handleRmsVoltageValue(final long value) {
    final Long multiplier = state.voltageMultiplier as Long
    final Long divisor = state.voltageDivisor as Long
    
    if (multiplier != null && divisor != null && multiplier > 0 && divisor > 0) {
        final BigDecimal currentValue = device.currentValue("voltage") as BigDecimal
        BigDecimal result = value * multiplier / divisor
        result = result.setScale(0, RoundingMode.HALF_UP)
        
        final BigDecimal inputDelta = getSettingBigDecimal("voltageDelta")
        if (isDelta(result, currentValue, inputDelta)) {
            updateAttribute("voltage", result, "V", "physical")
            updatePowerFactor()
        }
    }
}

void parseGeneralCommandResponse(final Map descMap) {
    if (!descMap.command) return
    final int commandId = hexStrToUnsignedInt(descMap.command)
    switch (commandId) {
        case 0x01: parseReadAttributeResponse(descMap); break
        case 0x04: parseWriteAttributeResponse(descMap); break
        case 0x07:
            if (!(descMap.data instanceof List) || descMap.data.isEmpty()) return
            final String status = ((List)descMap.data).first()
            final int statusCode = hexStrToUnsignedInt(status)
            final String statusName = ZigbeeStatusEnum[statusCode] ?: "0x${status}"
            if (statusCode > 0x00) logWarn "Configure reporting error: ${statusName} ${descMap.data}"
            else logTrace "Configure reporting response: ${statusName} ${descMap.data}"
            break
        case 0x09: parseReadReportingConfigResponse(descMap); break
        case 0x0B: parseDefaultCommandResponse(descMap); break
        default:
            final String commandName = ZigbeeGeneralCommandEnum[commandId] ?: "UNKNOWN_COMMAND (0x${descMap.command})"
            final String status = descMap.data instanceof List ? ((List)descMap.data).last() : descMap.data
            final int statusCode = hexStrToUnsignedInt(status)
            final String statusName = ZigbeeStatusEnum[statusCode] ?: "0x${status}"
            if (statusCode > 0x00) logWarn "${commandName} ${clusterLookup(descMap.clusterInt)} error: ${statusName}"
            else logTrace "${commandName} ${clusterLookup(descMap.clusterInt)}: ${descMap.data}"
            break
    }
}

void parseDefaultCommandResponse(final Map descMap) {
    if (!(descMap.data instanceof List) || ((List)descMap.data).size() < 2) return
    final List<String> data = descMap.data as List<String>
    final String commandId = data[0]
    final int statusCode = hexStrToUnsignedInt(data[1])
    final String status = ZigbeeStatusEnum[statusCode] ?: "0x${data[1]}"
    if (statusCode > 0x00) logWarn "${clusterLookup(descMap.clusterInt)} command 0x${commandId} error: ${status}"
    else logTrace "${clusterLookup(descMap.clusterInt)} command 0x${commandId} response: ${status}"
}

void parseMeteringCluster(final Map descMap) {
    if (descMap.attrInt == null || descMap.value == null || descMap.value == "FFFF") return
    final long value = hexStrToUnsignedInt(descMap.value)
    switch (descMap.attrInt as Integer) {
        case ATTRIBUTE_READING_INFO_SET:
            final Long divisor = state.meteringDivisor as Long
            final BigDecimal currentValue = device.currentValue("energy") as BigDecimal
            
            if (divisor != null && divisor > 0) {
                BigDecimal result = value / divisor
                result = result.setScale(1, RoundingMode.HALF_UP)
                final String unit = state.meteringUnit == 0 ? "kWh" : ""
                final BigDecimal inputDelta = getSettingBigDecimal("energyDelta")
                if (isDelta(result, currentValue, inputDelta)) {
                    updateAttribute("energy", result, unit, "physical")
                }
            }
            break
        case METERING_DIVISOR_ID: state.meteringDivisor = value; break
        case METERING_UNIT_OF_MEASURE_ID: state.meteringUnit = value; break
        case METERING_SUMMATION_FORMATTING_ID: break
        default:
            logWarn "Unknown Metering cluster attribute 0x${descMap.attrId} (val ${descMap.value})"
            break
    }
}

void parseReadAttributeResponse(final Map descMap) {
    if (!(descMap.data instanceof List) || ((List)descMap.data).size() < 3) return
    final List<String> data = descMap.data as List<String>
    final String attribute = data[1] + data[0]
    final int statusCode = hexStrToUnsignedInt(data[2])
    final String status = ZigbeeStatusEnum[statusCode] ?: "0x${data}"
    if (statusCode > 0x00) logWarn "Read ${clusterLookup(descMap.clusterInt)} attr 0x${attribute} error: ${status}"
    else logTrace "Read ${clusterLookup(descMap.clusterInt)} attr 0x${attribute} response: ${status}"
}

void parseReadReportingConfigResponse(final Map descMap) {
    if (!(descMap.data instanceof List) || ((List)descMap.data).size() < 9) return
    final List<String> data = descMap.data as List<String>
    final String status = data.first()
    final int statusCode = hexStrToUnsignedInt(status)
    final String statusName = ZigbeeStatusEnum[statusCode] ?: "0x${status}"
    if (statusCode > 0x00) {
        logWarn "Read reporting config error: ${statusName} ${descMap.data}"
        return
    }
    if (data[1] != "00") return

    final String attribute = "0x" + data[3] + data[2]
    final int dataType = hexStrToUnsignedInt(data[4])
    final int minReportingInterval = hexStrToUnsignedInt(data[6] + data[5])
    final int maxReportingInterval = hexStrToUnsignedInt(data[8] + data[7])
    Integer reportableChange = null
    
    if (!DataType.isDiscrete(dataType)) {
        final int start = DataType.getLength(dataType) + 8
        final int dataLen = DataType.getLength(dataType)
        final int end = start + (dataLen * 2) - 1
        if (data.size() > end) {
            reportableChange = hexStrToUnsignedInt(data[start..end].join())
        }
    }
    logDebug "Zigbee reporting config verified [attr: ${attribute}, dataType: ${dataType}, min: ${minReportingInterval}s, max: ${maxReportingInterval}s, change: ${reportableChange}]"
}

void parseWriteAttributeResponse(final Map descMap) {
    final String data = descMap.data instanceof List ? ((List)descMap.data).first() : descMap.data
    if (data == null) return
    final int statusCode = hexStrToUnsignedInt(data)
    final String statusName = ZigbeeStatusEnum[statusCode] ?: "0x${data}"
    if (statusCode > 0x00) logWarn "Write ${clusterLookup(descMap.clusterInt)} attribute error: ${statusName}"
    else logTrace "Write ${clusterLookup(descMap.clusterInt)} attribute response: ${statusName}"
}

void parseOnOffCluster(final Map descMap) {
    if (descMap.attrInt == null) return
    switch (descMap.attrInt as Integer) {
        case POWER_ON_OFF_ID:
            final String type = state.isDigital == true ? "digital" : "physical"
            state.remove("isDigital")
            updateAttribute("switch", descMap.value == "01" ? "on" : "off", null, type)
            break
        case POWER_RESTORE_ID:
            final Map<Integer, String> options = PowerRestoreOpts.options as Map<Integer, String>
            final Integer value = hexStrToUnsignedInt(descMap.value)
            logInfo "Power restore mode is '${options[value]}' (0x${descMap.value})"
            device.updateSetting("powerRestore", value.toString())
            break
        default:
            logWarn "Unknown On/Off cluster attribute: ${descMap}"
            break
    }
}

void parseZdoClusters(final Map descMap) {
    if (!(descMap.data instanceof List) || ((List)descMap.data).size() < 2) return
    final Integer clusterId = descMap.clusterInt as Integer
    final String clusterName = ZdoClusterEnum[clusterId] ?: "UNKNOWN_CLUSTER (0x${descMap.clusterId})"
    final String statusHex = ((List)descMap.data)[1]
    final Integer statusCode = hexStrToUnsignedInt(statusHex)
    final String statusName = ZigbeeStatusEnum[statusCode] ?: "0x${statusHex}"
    if (statusCode > 0x00) logWarn "ZDO ${clusterName} error: ${statusName}"
    else logTrace "ZDO ${clusterName} success: ${descMap.data}"
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void resetDriver() {
    logInfo "Starting full driver reset..."
    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()
    initialize(false)
    logInfo "Driver reset process completed and re-initialized."
}

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

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null) {
    final String currentVal = device.currentValue(attribute)?.toString()
    if (currentVal == value?.toString()) return

    final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
    logInfo descriptionText
    sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
}

/**
 * Calculates Power Factor mathematically from currently stored Active Power, RMS Voltage, and RMS Current.
 * Note: Calculated Power Factor (P / (V * I)) is preferred over raw Zigbee attribute 0x0510 (Power Factor)
 * reads to guarantee mathematical alignment across all displayed attributes and avoid unstandardized
 * hardware scaling discrepancies.
 **/
private void updatePowerFactor() {
    if (!supportsPowerMeter()) return
    final Object rawVoltage = device.currentValue("voltage")
    final Object rawCurrent = device.currentValue("amperage")
    final Object rawPower = device.currentValue("power")

    if (rawVoltage != null && rawCurrent != null && rawPower != null) {
        final BigDecimal rmsVoltage = rawVoltage as BigDecimal
        final BigDecimal rmsCurrent = rawCurrent as BigDecimal
        final BigDecimal activePower = rawPower as BigDecimal
        
        if (rmsVoltage > 0 && rmsCurrent > 0) { 
            BigDecimal powerFactor = calculatePowerFactor(rmsVoltage, rmsCurrent, activePower)
            if (powerFactor < -1) powerFactor = -1
            if (powerFactor > 1) powerFactor = 1
            updateAttribute("powerFactor", powerFactor.setScale(1, RoundingMode.HALF_UP), null, "digital")
        }
    }
}

private static BigDecimal calculatePowerFactor(final BigDecimal rmsVoltage, final BigDecimal rmsCurrent, final BigDecimal activePower) {
    return activePower / (rmsVoltage * rmsCurrent)
}

private boolean isDelta(final BigDecimal value, final BigDecimal previousValue, final BigDecimal minimumChange) {
    if (previousValue == null || minimumChange == null || minimumChange <= 0) return true
    boolean result = (value - previousValue).abs() >= minimumChange
    logDebug "isDelta(value: ${value}, prev: ${previousValue}, min: ${minimumChange}) = ${result}"
    return result
}

private String clusterLookup(final Object cluster) {
    return zigbee.clusterLookup(cluster.toInteger()) ?: "private cluster 0x${intToHexStr(cluster.toInteger())}"
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

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

// Device Model & Attribute Constants
@Field static final String POWER_METER_MODEL = "3RSP02028BZ"

@Field static final int AC_CURRENT_DIVISOR_ID = 0x0603
@Field static final int AC_CURRENT_MULTIPLIER_ID = 0x0602
@Field static final int AC_FREQUENCY_ID = 0x0300
@Field static final int AC_POWER_DIVISOR_ID = 0x0605
@Field static final int AC_POWER_MULTIPLIER_ID = 0x0604
@Field static final int AC_VOLTAGE_DIVISOR_ID = 0x0601
@Field static final int AC_VOLTAGE_MULTIPLIER_ID = 0x0600
@Field static final int ACTIVE_POWER_ID = 0x050B
@Field static final int ATTRIBUTE_READING_INFO_SET = 0x0000
@Field static final int FIRMWARE_VERSION_ID = 0x4000
@Field static final int PING_ATTR_ID = 0x01
@Field static final int POWER_ON_OFF_ID = 0x0000
@Field static final int POWER_RESTORE_ID = 0x4003
@Field static final int RMS_CURRENT_ID = 0x0508
@Field static final int RMS_VOLTAGE_ID = 0x0505
@Field static final int METERING_UNIT_OF_MEASURE_ID = 0x0300
@Field static final int METERING_DIVISOR_ID = 0x0302
@Field static final int METERING_SUMMATION_FORMATTING_ID = 0x0303

@Field static final Map PowerRestoreOpts = [
    defaultValue: 0xFF,
    options: [ 0x00: "Off", 0x01: "On", 0xFF: "Last State" ]
]

@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10
@Field static final int DELAY_MS = 200

@Field static final Map<Integer, String> ZigbeeStatusEnum = [
    0x00: "Success", 0x01: "Failure", 0x02: "Not Authorized", 0x80: "Malformed Command",
    0x81: "Unsupported COMMAND", 0x85: "Invalid Field", 0x86: "Unsupported Attribute",
    0x87: "Invalid Value", 0x88: "Read Only", 0x89: "Insufficient Space", 0x8A: "Duplicate Exists",
    0x8B: "Not Found", 0x8C: "Unreportable Attribute", 0x8D: "Invalid Data Type",
    0x8E: "Invalid Selector", 0x94: "Time out", 0x9A: "Notification Pending", 0xC3: "Unsupported Cluster"
]

@Field static final Map<Integer, String> ZdoClusterEnum = [
    0x0013: "Device announce", 0x8004: "Simple Descriptor Response", 0x8005: "Active Endpoints Response",
    0x801D: "Extended Simple Descriptor Response", 0x801E: "Extended Active Endpoint Response",
    0x8021: "Bind Response", 0x8022: "Unbind Response", 0x8023: "Bind Register Response"
]

@Field static final Map<Integer, String> ZigbeeGeneralCommandEnum = [
    0x00: "Read Attributes", 0x01: "Read Attributes Response", 0x02: "Write Attributes",
    0x03: "Write Attributes Undivided", 0x04: "Write Attributes Response", 0x05: "Write Attributes No Response",
    0x06: "Configure Reporting", 0x07: "Configure Reporting Response", 0x08: "Read Reporting Configuration",
    0x09: "Read Reporting Configuration Response", 0x0A: "Report Attributes", 0x0B: "Default Response",
    0x0C: "Discover Attributes", 0x0D: "Discover Attributes Response", 0x0E: "Read Attributes Structured",
    0x0F: "Write Attributes Structured", 0x10: "Write Attributes Structured Response",
    0x11: "Discover Commands Received", 0x12: "Discover Commands Received Response",
    0x13: "Discover Commands Generated", 0x14: "Discover Commands Generated Response",
    0x15: "Discover Attributes Extended", 0x16: "Discover Attributes Extended Response"
]