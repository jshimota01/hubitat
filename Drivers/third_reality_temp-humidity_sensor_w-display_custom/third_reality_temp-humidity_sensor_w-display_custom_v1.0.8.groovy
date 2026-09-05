/**
 * Third Reality Temperature & Humidity Sensor w/Display (Custom)
 * Third Reality Zigbee 3.0 Device Model 3RTHS24BZ (LCD Display Model)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Purpose-built custom driver for the Third Reality Temperature & Humidity Sensor w/Display (3RTHS24BZ).
 * Features read/diagnostic support for private cluster 0xFF01 manufacturer calibration attributes,
 * driver-side delta filtering, software offset calibration, and phase-anchored health monitoring.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability.
 * - Native capability exposes a redundant "Ping" UI button and lacks persistent
 *   phase-anchored scheduling and timeout guards.
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
 * v1.0.8   09/05/26    jshimota    Enforced direct HubMultiAction dispatch in queryPrivateCluster to prevent command queuing on sleepy end-devices.
 * v1.0.7   09/05/26    jshimota    Integrated hardware LCD screen offset calibration preferences targeting cluster 0xFF01 with verified tenths-scale conversion (10 = 1.0 deg).
 * v1.0.6   09/05/26    jshimota    Added writeCelsiusOffset and writeFahrenheitOffset test commands to conduct scaling experiment on cluster 0xFF01.
 * v1.0.5   09/05/26    jshimota    Removed cluster 0x0001 reporting configuration, added cluster 0xFF01 state variable diagnostic tracking (0x0031, 0x0032, 0x0033), and strictly separated driver software calibration from on-device calibration.
 * v1.0.4   09/05/26    jshimota    Verified cluster 0xFF01 (attrs 0x0031, 0x0032, 0x0033) responsiveness, suppressed 0x0001 cluster reporting chatter, and finalized production release.
 * v1.0.3   09/05/26    jshimota    Added queryPrivateCluster command and explicit raw parser logging for 0xFF01 (attrs 0x0031, 0x0032, 0x0033) to verify manufacturer calibration capability.
 * v1.0.2   09/05/26    jshimota    Added diagnostic cluster 0xFF01 query hook in refresh() and header documentation for LCD pairing states.
 * v1.0.1   09/05/26    jshimota    Adapted driver baseline for Model 3RTHS24BZ (LCD Display Model), updated driver definition name, and set importUrl path to third_reality_temp-humidity_sensor_w-display_custom.
**/

static String version() { return '1.0.8' }
def timeStamp() { return "2026/09/05 11:30 AM" }

import groovy.transform.Field
import hubitat.zigbee.zcl.DataType
import java.math.RoundingMode

metadata {
    definition(
        name: "Third Reality Temperature & Humidity Sensor w/Display (Custom)",
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/third_reality_temp-humidity_sensor_w-display_custom/third_reality_temp-humidity_sensor_w-display_custom.groovy"
    ) {
        capability "Battery"
        capability "Configuration"
        capability "RelativeHumidityMeasurement"
        capability "Refresh"
        capability "Sensor"
        capability "TemperatureMeasurement"

        command "Health Check"
        command "queryPrivateCluster"
        command "writeCelsiusOffset", [[name: "Raw Value (INT16)", type: "NUMBER", description: "e.g., 10 for +1.0°C or 1 for +1°C"]]
        command "writeFahrenheitOffset", [[name: "Raw Value (INT16)", type: "NUMBER", description: "e.g., 10 for +1.0°F or 1 for +1°F"]]
        command "updateFirmware"
        command "resetDriver"

        attribute "healthStatus", "enum", ["unknown", "offline", "online"]
        attribute "temperatureText", "string"
        attribute "humidityText", "string"

        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0001,0003,0020,0402,0405,FF01", outClusters: "0019", model: "3RTHS24BZ", manufacturer: "Third Reality", controllerType: "ZGB"
        fingerprint profileId: "0104", endpointId: "01", inClusters: "0000,0001,0003,0020,0402,0405,FF01", outClusters: "0019", model: "Third Reality, Inc", controllerType: "ZGB"
    }

    preferences {
        String tempUnit = location.temperatureScale ?: "F"
        
        // Temperature Settings (Decimal)
        input name: "tempOffset", type: "decimal", title: "<b>Temperature Offset</b>", description: "<i>Adjust temperature reading (-10.0 to +10.0 °${tempUnit}).</i>", range: "-10..10", defaultValue: 0.0
        input name: "tempDelta", type: "decimal", title: "<b>Temperature Minimum Change Delta</b>", description: "<i>Minimum temperature change required to emit an event (0.1 to 10.0 °${tempUnit}). Evaluated on rounded 1-decimal value.</i>", range: "0..10", defaultValue: 0.2

        // Humidity Settings (Integer Enforced)
        input name: "humidityOffset", type: "number", title: "<b>Humidity Offset</b>", description: "<i>Adjust relative humidity reading (-20 to +20 %). Integers only; decimals will be rounded.</i>", range: "-20..20", defaultValue: 0
        input name: "humidityDelta", type: "number", title: "<b>Humidity Minimum Change Delta</b>", description: "<i>Minimum humidity % change required to emit an event (1 to 10 %). Integers only; decimals will be rounded.</i>", range: "1..10", defaultValue: 1

        input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver verifies device online status.</i>"

        // Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b><br>(Is turned on for 30 minutes after Initialized or first installed)", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
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
private BigDecimal getSettingBigDecimal(String key, BigDecimal defaultVal = 0.0G) {
    Object val = settings[key]
    if (val == null) return defaultVal
    try {
        return new BigDecimal(val.toString())
    } catch (Exception e) {
        logWarn "Failed to parse numerical preference [${key}]: ${val}"
        return defaultVal
    }
}

// NPE- and Decimal-Safe Integer Preference Conversion Helper
private Integer getSettingInteger(String key, Integer defaultVal = 0) {
    Object val = settings[key]
    if (val == null) return defaultVal
    try {
        return new BigDecimal(val.toString()).setScale(0, RoundingMode.HALF_UP).intValue()
    } catch (Exception e) {
        logWarn "Failed to parse integer preference [${key}]: ${val}"
        return defaultVal
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

    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Preferences updated"

    // Clear last evaluated state entries if offset changed so next report passes delta filter naturally
    BigDecimal currentTempOffset = getSettingBigDecimal("tempOffset", 0.0G)
    Integer currentHumidityOffset = getSettingInteger("humidityOffset", 0)

    if (state.lastTempOffset != currentTempOffset) {
        state.remove("last_temperature")
        state.remove("lastTime_temperature")
        state.lastTempOffset = currentTempOffset
    }

    if (state.lastHumidityOffset != currentHumidityOffset) {
        state.remove("last_humidity")
        state.remove("lastTime_humidity")
        state.lastHumidityOffset = currentHumidityOffset
    }

    initialize(false)
    runIn(1, "configure")
}

List<String> configure() {
    checkAndLogVersionDemarcation()
    logInfo "Configuring device reporting intervals..."

    List<String> cmds = []

    // Temperature reporting (0x0402): Min 10s, Max 3600s, Reportable change 10 (0.1°C)
    cmds += zigbee.configureReporting(0x0402, 0x0000, DataType.INT16, 10, 3600, 10, [:], DELAY_MS)

    // Humidity reporting (0x0405): Min 10s, Max 3600s, Reportable change 100 (1.0%)
    cmds += zigbee.configureReporting(0x0405, 0x0000, DataType.UINT16, 10, 3600, 100, [:], DELAY_MS)

    // Execute initial Health Check
    cmds += executeHealthCheck()

    runIn(5, "refresh")
    return cmds
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    
    // Targeted unschedule strictly for health check rescheduling
    unschedule("executeHealthCheckScheduled")

    if (device.currentValue("healthStatus") == null) {
        sendEvent(name: "healthStatus", value: "unknown")
    }

    final int interval = settings.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 480
    if (interval > 0) {
        scheduleHealthCheck("executeHealthCheckScheduled", interval)
    }

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    }
}

/* =========================================================================================
   COMMAND IMPLEMENTATIONS & HEALTH CHECK
   ========================================================================================= */

List<String> "Health Check"() {
    return executeHealthCheck()
}

List<String> queryPrivateCluster() {
    logInfo "Querying Private Cluster 0xFF01 calibration attributes..."
    List<String> cmds = []
    cmds += zigbee.readAttribute(0xFF01, 0x0031, [mfgCode: 0x1233], DELAY_MS) // Celsius Offset
    cmds += zigbee.readAttribute(0xFF01, 0x0032, [mfgCode: 0x1233], DELAY_MS) // Humidity Offset
    cmds += zigbee.readAttribute(0xFF01, 0x0033, [mfgCode: 0x1233], DELAY_MS) // Fahrenheit Offset
    
    // Force immediate protocol dispatch so commands do not stall in queue on sleepy end-devices
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
    return cmds
}

List<String> writeCelsiusOffset(Object rawVal) {
    int val = rawVal ? rawVal.toString().toInteger() : 0
    logInfo "EXPERIMENTAL: Writing ${val} to Private Cluster 0xFF01 Celsius Offset (attr 0x0031)..."
    List<String> cmds = []
    cmds += zigbee.writeAttribute(0xFF01, 0x0031, DataType.INT16, val, [mfgCode: 0x1233], DELAY_MS)
    cmds += zigbee.readAttribute(0xFF01, 0x0031, [mfgCode: 0x1233], DELAY_MS)
    
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
    return cmds
}

List<String> writeFahrenheitOffset(Object rawVal) {
    int val = rawVal ? rawVal.toString().toInteger() : 0
    logInfo "EXPERIMENTAL: Writing ${val} to Private Cluster 0xFF01 Fahrenheit Offset (attr 0x0033)..."
    List<String> cmds = []
    cmds += zigbee.writeAttribute(0xFF01, 0x0033, DataType.INT16, val, [mfgCode: 0x1233], DELAY_MS)
    cmds += zigbee.readAttribute(0xFF01, 0x0033, [mfgCode: 0x1233], DELAY_MS)
    
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
    return cmds
}

void executeHealthCheckScheduled() {
    List<String> cmds = executeHealthCheck()
    if (cmds) sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

private List<String> executeHealthCheck() {
    logDebug "Executing Health Check..."
    scheduleCommandTimeoutCheck()
    return zigbee.readAttribute(zigbee.BASIC_CLUSTER, HEALTH_CHECK_ATTR_ID, [:], 0)
}

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
            schedule("0 ${m} * ? * * *", methodToSchedule); break
        case 240:
            String h4 = [0, 4, 8, 12, 16, 20].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h4} ? * * *", methodToSchedule); break
        case 480:
            String h8 = [0, 8, 16].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h8} ? * * *", methodToSchedule); break
        case 720:
            String h12 = [0, 12].collect { (it + h) % 24 }.sort().join(",")
            schedule("0 ${m} ${h12} ? * * *", methodToSchedule); break
        case 1440:
            schedule("0 ${m} ${h} ? * * *", methodToSchedule); break
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
    runIn(delay, "deviceCommandTimeout", [overwrite: true])
}

void deviceCommandTimeout() {
    logWarn "No Health Check response received (device offline?)"
    updateAttribute("healthStatus", "offline")
}

// Assert online status whenever valid sensor traffic arrives from sleepy device
private void assertDeviceOnline() {
    if (device.currentValue("healthStatus") != "online") {
        sendEvent(name: "healthStatus", value: "online", isStateChange: true, descriptionText: "${device.displayName} is online")
    }
}

/* =========================================================================================
   ZIGBEE MESSAGE PARSING & SENSOR PROCESSING
   ========================================================================================= */

List<String> refresh() {
    logDebug "Executing refresh()..."
    List<String> cmds = []

    cmds += zigbee.readAttribute(0x0402, 0x0000, [:], DELAY_MS) // Temperature
    cmds += zigbee.readAttribute(0x0405, 0x0000, [:], DELAY_MS) // Humidity
    cmds += zigbee.readAttribute(0x0001, 0x0021, [:], DELAY_MS) // Battery %
    cmds += zigbee.readAttribute(zigbee.BASIC_CLUSTER, FIRMWARE_VERSION_ID, [:], DELAY_MS)
    
    // Include 0xFF01 diagnostic query
    cmds += queryPrivateCluster()

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

    if (descMap.isClusterSpecific == false) {
        parseGeneralCommandResponse(descMap)
        return
    }

    if (descMap.clusterInt == null) return

    switch (descMap.clusterInt as Integer) {
        case zigbee.BASIC_CLUSTER:
            parseBasicCluster(descMap)
            break
        case 0x0001: // Power Configuration
            parsePowerCluster(descMap)
            break
        case 0x0402: // Temperature Measurement
            parseTemperatureCluster(descMap)
            break
        case 0x0405: // Relative Humidity Measurement
            parseHumidityCluster(descMap)
            break
        case 0xFF01: // Manufacturer Private Cluster Diagnostics
            parsePrivateCluster(descMap)
            break
        default:
            logDebug "Unhandled cluster message: ${descMap}"
            break
    }
}

void parseBasicCluster(final Map descMap) {
    if (descMap.attrInt == null) return
    switch (descMap.attrInt as Integer) {
        case HEALTH_CHECK_ATTR_ID:
            // Unschedule command timeout strictly when the specific Health Check Basic Cluster 0x0000 attribute response arrives
            unschedule("deviceCommandTimeout")
            logDebug "Health Check response received..."
            sendEvent(name: "healthStatus", value: "online", isStateChange: true, descriptionText: "${device.displayName} is online")
            break
        case FIRMWARE_VERSION_ID:
            assertDeviceOnline()
            final String versionStr = descMap.value ?: "unknown"
            logDebug "Device firmware version is ${versionStr}"
            updateDataValue("softwareBuild", versionStr)
            break
    }
}

void parsePowerCluster(final Map descMap) {
    if (descMap.attrInt == null || descMap.value == null) return
    assertDeviceOnline()
    final long rawValue = hexStrToUnsignedInt(descMap.value)

    switch (descMap.attrInt as Integer) {
        case 0x0021: // Battery Percentage (0.5% scale -> raw 200 = 100%)
            int pct = Math.min((int)Math.round(rawValue / 2.0), 100)
            updateAttribute("battery", pct, "%", "physical")
            break
    }
}

void parseTemperatureCluster(final Map descMap) {
    if (descMap.attrInt != 0x0000 || descMap.value == null || descMap.value == "FFFF") return
    assertDeviceOnline()

    // Raw standard temperature is signed 16-bit integer in hundredths of a degree Celsius
    int rawTemp = hexStrToSignedInt(descMap.value)
    BigDecimal celsius = new BigDecimal(rawTemp).divide(100G, 2, RoundingMode.HALF_UP)
    
    // Convert to Fahrenheit if hub scale is set to F
    Boolean isFahrenheit = location.temperatureScale == "F"
    BigDecimal scaleTemp = isFahrenheit ? ((celsius * 1.8G) + 32G) : celsius

    // Apply software preference offset
    BigDecimal offset = getSettingBigDecimal("tempOffset", 0.0G)
    BigDecimal finalTemp = (scaleTemp + offset).setScale(1, RoundingMode.HALF_UP)

    BigDecimal currentVal = device.currentValue("temperature") as BigDecimal
    
    // Code enforcement: minDelta must be at least 0.1G
    BigDecimal minDelta = getSettingBigDecimal("tempDelta", 0.2G)
    if (minDelta < 0.1G) minDelta = 0.1G

    if (isDelta(finalTemp, currentVal, minDelta)) {
        String unit = isFahrenheit ? "°F" : "°C"
        updateAttribute("temperature", finalTemp, unit, "physical")
        updateAttribute("temperatureText", "${finalTemp} ${unit}", null, "physical", "info", "Formatted Temperature Text")
    }
}

void parseHumidityCluster(final Map descMap) {
    if (descMap.attrInt != 0x0000 || descMap.value == null || descMap.value == "FFFF") return
    assertDeviceOnline()

    int rawHumidity = hexStrToUnsignedInt(descMap.value)
    BigDecimal rawPct = new BigDecimal(rawHumidity).divide(100G, 2, RoundingMode.HALF_UP)

    // Apply software integer preference offset before final rounding
    BigDecimal offset = getSettingBigDecimal("humidityOffset", 0.0G)
    BigDecimal calculated = rawPct + offset
    
    if (calculated < 0G) calculated = 0G
    if (calculated > 100G) calculated = 100G

    BigDecimal finalHumidity = calculated.setScale(0, RoundingMode.HALF_UP)

    BigDecimal currentVal = device.currentValue("humidity") as BigDecimal
    
    Integer minDeltaInt = getSettingInteger("humidityDelta", 1)
    if (minDeltaInt < 1) minDeltaInt = 1
    BigDecimal minDelta = new BigDecimal(minDeltaInt)

    if (isDelta(finalHumidity, currentVal, minDelta)) {
        updateAttribute("humidity", finalHumidity, "%", "physical")
        updateAttribute("humidityText", "${finalHumidity}%", null, "physical", "info", "Formatted Humidity Text")
    }
}

void parsePrivateCluster(final Map descMap) {
    if (descMap.attrInt == null || descMap.value == null) return
    assertDeviceOnline()

    int rawVal = hexStrToSignedInt(descMap.value)
    
    switch (descMap.attrInt as Integer) {
        case 0x0031: // Celsius Calibration Offset
            state.deviceCalCelsius = rawVal
            logDebug "Private Cluster 0xFF01 Celsius Calibration Offset: ${rawVal} (raw hex: ${descMap.value})"
            break
        case 0x0032: // Humidity Calibration Offset
            state.deviceCalHumidity = rawVal
            logDebug "Private Cluster 0xFF01 Humidity Calibration Offset: ${rawVal} (raw hex: ${descMap.value})"
            break
        case 0x0033: // Fahrenheit Calibration Offset
            state.deviceCalFahrenheit = rawVal
            logDebug "Private Cluster 0xFF01 Fahrenheit Calibration Offset: ${rawVal} (raw hex: ${descMap.value})"
            break
    }
}

void parseGeneralCommandResponse(final Map descMap) {
    if (!descMap.command) return
    logTrace "General command response cluster ${descMap.clusterInt}: ${descMap.command}"
}

/* =========================================================================================
   MASTER UTILITY ROUTINES & LOGGING ENGINE
   ========================================================================================= */

void resetDriver() {
    logInfo "Starting full driver reset..."
    
    Object savedHour = state.healthCheckStartHour
    Object savedMinute = state.healthCheckStartMinute

    clearAllSchedules()
    clearAllAttributes()
    clearAllDriverStates()

    if (savedHour != null) state.healthCheckStartHour = savedHour
    if (savedMinute != null) state.healthCheckStartMinute = savedMinute

    initialize(false)
    logInfo "Driver reset completed."
}

void clearAllDriverStates() {
    logInfo "Clearing driver states..."
    state.clear()
}

void clearAllAttributes() {
    logInfo "Clearing attributes..."
    device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }
}

void clearAllSchedules() {
    logInfo "Clearing scheduled jobs..."
    unschedule()
}

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null, final String logLevel = "info", final String customLabel = null) {
    final String valStr = value?.toString()
    final String currentVal = device.currentValue(attribute)?.toString()

    if (currentVal == valStr) return

    final String lastVal = state["last_${attribute}"]?.toString()
    final Long lastTime = state["lastTime_${attribute}"] as Long ?: 0L
    final Long now = now()

    if (lastVal == valStr && (now - lastTime) < 500) {
        logDebug "updateAttribute(): Suppressed duplicate ${attribute} event (${valStr}) within ${now - lastTime}ms"
        return
    }

    state["last_${attribute}"] = valStr
    state["lastTime_${attribute}"] = now

    final String label = customLabel ?: attribute
    final String descriptionText = "${device.displayName} - ${label} was set to ${value}${unit ? ' ' + unit : ''}"
    logMessage(logLevel, descriptionText)
    sendEvent(name: attribute, value: value, unit: unit, type: type, isStateChange: true, descriptionText: descriptionText)
}

private boolean isDelta(final BigDecimal value, final BigDecimal previousValue, final BigDecimal minimumChange) {
    if (previousValue == null || minimumChange == null || minimumChange <= 0) return true
    boolean result = (value - previousValue).abs() >= minimumChange
    logDebug "isDelta(value: ${value}, prev: ${previousValue}, min: ${minimumChange}) = ${result}"
    return result
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Device Driver"
    
    String settingKey
    switch (lowerLevel) {
        case "info":  settingKey = "logInfoEnable"; break
        case "error": settingKey = "logErrorEnable"; break
        case "warn":  settingKey = "logWarnEnable"; break
        case "debug": settingKey = "logDebugEnable"; break
        case "trace": settingKey = "logTraceEnable"; break
        default:      settingKey = "logInfoEnable"; break
    }

    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${devName}: ${msg ?: ''}"
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

@Field static final int FIRMWARE_VERSION_ID = 0x4000
@Field static final int HEALTH_CHECK_ATTR_ID = 0x0000

@Field static final Map HealthCheckIntervalOpts = [
    defaultValue: 480,
    options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 20
@Field static final int DELAY_MS = 200