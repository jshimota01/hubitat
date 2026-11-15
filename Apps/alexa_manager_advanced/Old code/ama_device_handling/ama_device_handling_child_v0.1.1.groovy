/**
 * Alexa Manager Advanced - Device Handling Child Application
 * Platform: Hubitat Elevation C-7
 * Namespace: jshimota
 * Version: 0.1.1
 *
 * Changelog:
 * v0.1.1 - 2026/11/09 - Added explicit 'Child' suffix to application name.
 * v0.1.0 - 2026/11/09 - Initial Hubitat device discovery and inventory library.
 *   - Scans the Hubitat device fleet and records device identity, room, driver, data values,
 *     supported capabilities, attributes, commands, and current attribute values.
 *   - Inventory is keyed by Hubitat device ID and rescans update detected metadata only.
 *   - Existing AMA configuration/approval fields are preserved across scans.
 *   - Discovery is non-destructive and never publishes or removes Amazon endpoints.
 *
 * Notes:
 * Device capability enumeration is defensive because Hubitat device wrappers and custom
 * drivers can expose different metadata. Capability API results are preferred; supported
 * attributes/commands are retained as independent evidence and are not silently discarded.
 */

static String version() { return '0.1.1' }
String timeStamp() { return '2026/11/09' }

definition(
    name: 'AMA Device Handling Child',
    namespace: 'jshimota',
    author: 'James Shimota',
    description: 'Discovers Hubitat devices and maintains the AMA device inventory.',
    category: 'Integrations',
    parent: 'jshimota:Alexa Manager Advanced',
    iconUrl: '',
    iconX2Url: '',
    iconX3Url: '',
    importUrl: ''
)

preferences {
    page(name: 'mainPage')
}

def mainPage() {
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'>" +
                      "<h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Device Handling Child</h2>" +
                      "<span style='font-size:12px;opacity:0.85;'>Inventory & Discovery v${version()} (${timeStamp()})</span></div>"
            Map status = getInventoryStatus()
            paragraph "<b>Last scan:</b> ${status.lastScan ?: 'Never'}"
            paragraph "<b>Devices in library:</b> ${status.deviceCount ?: 0}"
            paragraph "<b>Last scan result:</b> ${status.lastResult ?: 'No scan performed'}"
        }

        section('DEVICE DISCOVERY') {
            paragraph 'Scan the Hubitat device registry. This operation only reads device metadata and current values. It does not expose devices to Amazon or change device configuration.'
            input name: 'btnScanDevices', type: 'button', title: 'Scan Hubitat Devices'
            input name: 'includeVirtualDevices', type: 'bool', title: 'Include Virtual Devices', defaultValue: true, submitOnChange: true
        }

        section('INVENTORY CONTENT') {
            paragraph 'The inventory records Hubitat Name, Label, Room, device ID, driver, manufacturer/model data, detected capabilities, supported attributes and commands, and a snapshot of current values.'
            paragraph 'Amazon Name, Amazon capability selection, approval, exposure state, naming analysis, and voice-test results are reserved for Device Management.'
        }

        section('LOGGING', hideable: true, hidden: true) {
            input name: 'logInfoEnable', type: 'bool', title: 'Enable Info Logging', defaultValue: true
            input name: 'logDebugEnable', type: 'bool', title: 'Enable Debug Logging', defaultValue: false
            input name: 'logTraceEnable', type: 'bool', title: 'Enable Trace Logging', defaultValue: false
        }
    }
}

void installed() {
    logInfo "Installing AMA Device Handling Child v${version()}"
    if (!(state.deviceInventory instanceof Map)) state.deviceInventory = [:]
}

void updated() {
    logInfo 'Updating AMA Device Handling Child.'
    if (!(state.deviceInventory instanceof Map)) state.deviceInventory = [:]
}

void appButtonHandler(String buttonName) {
    if (buttonName == 'btnScanDevices') {
        scanDeviceInventory('Manual scan')
    }
}

public void scanDeviceInventory(String reason = 'Requested scan') {
    long started = now()
    Map oldInventory = (state.deviceInventory instanceof Map) ? state.deviceInventory : [:]
    Map newInventory = [:]
    int scanned = 0
    int changed = 0
    int failed = 0
    Set<String> fleetIds = [] as Set
    Set<String> filteredIds = [] as Set

    List fleet = []
    try {
        fleet = getAllDevices() ?: []
    } catch (Exception e) {
        state.lastScanResult = "FAILED: Unable to enumerate Hubitat devices (${e.message})"
        logError state.lastScanResult
        return
    }

    fleet.each { dev ->
        try {
            String fleetId = dev.id?.toString()
            if (fleetId) fleetIds << fleetId
            if (!includeVirtualDevices && isVirtualDevice(dev)) {
                if (fleetId) filteredIds << fleetId
                return
            }
            Map detected = inspectDevice(dev)
            String id = detected.deviceId
            if (!id) {
                failed++
                return
            }

            Map previous = oldInventory[id] instanceof Map ? oldInventory[id] : [:]
            Map merged = mergeDetectedMetadata(previous, detected)
            newInventory[id] = merged
            scanned++
            if (hasDetectedMetadataChanged(previous, detected)) changed++
        } catch (Exception e) {
            failed++
            logWarn "Could not inspect device ${safeDeviceLabel(dev)}: ${e.message}"
        }
    }

    /* Retain records for devices filtered from this scan; do not mislabel them as removed. */
    oldInventory.each { id, entry ->
        if (!newInventory.containsKey(id)) {
            Map retained = new LinkedHashMap(entry as Map)
            if (filteredIds.contains(id.toString())) {
                retained.presentInHubitat = true
            } else {
                retained.presentInHubitat = fleetIds.contains(id.toString()) ? true : false
            }
            newInventory[id.toString()] = retained
        }
    }

    String completed = formatTimestamp(now())
    newInventory.each { id, entry ->
        if (fleetIds.contains(id.toString())) {
            entry.presentInHubitat = true
            if (!filteredIds.contains(id.toString())) entry.lastSeenInScan = completed
        }
    }

    state.deviceInventory = newInventory
    state.lastScan = completed
    state.lastScanDurationMs = now() - started
    state.lastScanResult = "Completed: ${scanned} inspected, ${changed} metadata changes, ${failed} errors."
    state.lastScanReason = reason
    logInfo "Device inventory scan complete (${reason}): ${scanned} inspected, ${changed} changed, ${failed} errors, ${newInventory.size()} library records."
}

private Map inspectDevice(dev) {
    Map attributes = readDefinitionNames(dev, 'getSupportedAttributes')
    Map commands = readDefinitionNames(dev, 'getSupportedCommands')
    Map capabilities = readCapabilities(dev)
    Map currentValues = readCurrentValues(dev, attributes.names as List)
    Map dataValues = readDataValues(dev)

    String id = dev.id?.toString()
    String hubitatName = safeString(dev.name)
    String hubitatLabel = safeString(dev.label)
    String displayName = safeString(dev.displayName) ?: hubitatLabel ?: hubitatName ?: "Hubitat Device ${id}"
    String driverType = safeString(dev.typeName) ?: 'Unknown Driver'

    return [
        deviceId: id,
        hubitatName: hubitatName,
        hubitatLabel: hubitatLabel,
        displayName: displayName,
        roomName: safeString(readProperty(dev, 'roomName')),
        driverType: driverType,
        deviceNetworkId: safeString(dev.deviceNetworkId),
        manufacturer: dataValues.manufacturer ?: dataValues.MANUFACTURER ?: null,
        model: dataValues.model ?: dataValues.MODEL ?: null,
        firmwareVersion: dataValues.firmwareVersion ?: dataValues.firmware ?: null,
        dataValues: dataValues,
        capabilities: capabilities.names,
        capabilityDetectionMethod: capabilities.method,
        supportedAttributes: attributes.names,
        supportedCommands: commands.names,
        currentValues: currentValues,
        detectedAt: formatTimestamp(now())
    ]
}

private Boolean hasDetectedMetadataChanged(Map previous, Map detected) {
    if (!previous) return true
    List scannerFields = ['hubitatName','hubitatLabel','displayName','roomName','driverType','deviceNetworkId','manufacturer','model','firmwareVersion','dataValues','capabilities','capabilityDetectionMethod','supportedAttributes','supportedCommands','currentValues']
    return scannerFields.any { key -> previous[key] != detected[key] }
}

private Map mergeDetectedMetadata(Map previous, Map detected) {
    Map merged = new LinkedHashMap(previous ?: [:])
    /* Scanner-owned fields are refreshed. User/AMA-owned fields are deliberately retained. */
    detected.each { key, value -> merged[key] = value }
    merged.presentInHubitat = true
    merged.firstDiscovered = previous.firstDiscovered ?: detected.detectedAt
    merged.lastDiscovered = detected.detectedAt
    if (!merged.containsKey('amazonName')) merged.amazonName = ''
    if (!merged.containsKey('amazonCapabilities')) merged.amazonCapabilities = []
    if (!merged.containsKey('approvedForAmazon')) merged.approvedForAmazon = false
    if (!merged.containsKey('exposureStatus')) merged.exposureStatus = 'NOT_EXPOSED'
    if (!merged.containsKey('namingReviewStatus')) merged.namingReviewStatus = 'NOT_REVIEWED'
    return merged
}

private Map readCapabilities(dev) {
    List names = []
    String methodUsed = 'attribute/command inference'
    try {
        def raw = dev.getCapabilities()
        if (raw instanceof Collection) {
            raw.each { item ->
                String name = normalizeDefinitionName(item)
                if (name) names << name
            }
            if (names) methodUsed = 'getCapabilities()'
        }
    } catch (Exception ignored) { }

    if (!names) {
        Map attrs = readDefinitionNames(dev, 'getSupportedAttributes')
        Map cmds = readDefinitionNames(dev, 'getSupportedCommands')
        Map inferred = inferCapabilities(attrs.names as List, cmds.names as List)
        names = inferred.names
    }
    return [names: names.unique().sort(), method: methodUsed]
}

private Map inferCapabilities(List attrs, List cmds) {
    Set a = (attrs ?: []).collect { it.toString().toLowerCase() } as Set
    Set c = (cmds ?: []).collect { it.toString().toLowerCase() } as Set
    Map<String, Map> rules = [
        'Switch': [a:['switch'], c:['on','off']],
        'SwitchLevel': [a:['level'], c:['setlevel']],
        'ColorControl': [a:['hue','saturation','color'], c:['setcolor','sethue','setsaturation']],
        'ColorTemperature': [a:['colortemperature'], c:['setcolortemperature']],
        'TemperatureMeasurement': [a:['temperature'], c:[]],
        'Thermostat': [a:['thermostatmode','thermostatoperatingstate','thermostatsetpoint','heatingsetpoint','coolingsetpoint'], c:['setthermostatmode','setheatingsetpoint','setcoolingsetpoint']],
        'RelativeHumidityMeasurement': [a:['humidity'], c:[]],
        'MotionSensor': [a:['motion'], c:[]],
        'ContactSensor': [a:['contact'], c:[]],
        'PresenceSensor': [a:['presence'], c:[]],
        'IlluminanceMeasurement': [a:['illuminance'], c:[]],
        'PowerMeter': [a:['power'], c:[]],
        'EnergyMeter': [a:['energy'], c:[]],
        'Battery': [a:['battery'], c:[]],
        'Lock': [a:['lock'], c:['lock','unlock']],
        'GarageDoorControl': [a:['door'], c:['opendoor','closedoor']],
        'WindowShade': [a:['windowshade'], c:['open','close','setposition']],
        'WaterSensor': [a:['water'], c:[]],
        'SmokeDetector': [a:['smoke'], c:[]],
        'CarbonMonoxideDetector': [a:['carbonmonoxide'], c:[]],
        'Valve': [a:['valve'], c:['open','close']],
        'PushableButton': [a:['pushed','held','doubleTapped'], c:[]],
        'AudioVolume': [a:['volume','mute'], c:['setvolume','mute','unmute']]
    ]
    List found = []
    rules.each { cap, rule ->
        boolean matchA = (rule.a ?: []).any { token -> token.contains(' ') ? a.any { it in token.split(' ') } : a.contains(token) }
        boolean matchC = (rule.c ?: []).any { c.contains(it.toLowerCase()) }
        if (matchA || matchC) found << cap
    }
    return [names: found]
}

private Map readDefinitionNames(dev, String methodName) {
    List names = []
    try {
        def defs = dev."${methodName}"()
        if (defs instanceof Collection) defs.each { item ->
            String name = normalizeDefinitionName(item)
            if (name) names << name
        }
    } catch (Exception ignored) { }
    return [names: names.unique().sort()]
}

private String normalizeDefinitionName(Object item) {
    if (item == null) return null
    String value = null
    try { value = item.name?.toString() } catch (Exception ignored) { }
    if (!value) {
        String raw = item.toString()
        value = raw.contains(' ') ? raw.substring(raw.lastIndexOf(' ') + 1) : raw
    }
    value = value?.trim()
    return value ? value : null
}

private Map readCurrentValues(dev, List attributeNames) {
    Map values = [:]
    (attributeNames ?: []).each { attr ->
        try {
            def value = dev.currentValue(attr.toString())
            if (value != null) values[attr.toString()] = value
        } catch (Exception ignored) { }
    }
    return values
}

private Map readDataValues(dev) {
    Map values = [:]
    ['manufacturer','model','firmwareVersion','firmware','hardwareVersion','protocol','deviceType','deviceClass','application','endpointId'].each { key ->
        try {
            def value = dev.getDataValue(key)
            if (value != null && value.toString().trim()) values[key] = value.toString()
        } catch (Exception ignored) { }
    }
    return values
}

private Object readProperty(dev, String propertyName) {
    try { return dev."${propertyName}" } catch (Exception ignored) { return null }
}

private Boolean isVirtualDevice(dev) {
    String type = safeString(dev.typeName)?.toLowerCase() ?: ''
    String driver = safeString(dev.getDataValue('driver'))?.toLowerCase() ?: ''
    return type.contains('virtual') || driver.contains('virtual')
}

private String safeDeviceLabel(dev) {
    return safeString(dev.displayName) ?: safeString(dev.name) ?: "device ${dev.id}"
}

private String safeString(Object value) {
    if (value == null) return null
    String s = value.toString().trim()
    return s ? s : null
}

private String formatTimestamp(Long millis) {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date(millis).format("yyyy-MM-dd HH:mm:ss", tz)
}

public Map getDeviceInventory() {
    return (state.deviceInventory instanceof Map) ? state.deviceInventory : [:]
}

public Map getDeviceInventoryEntry(String deviceId) {
    return getDeviceInventory()[deviceId] instanceof Map ? getDeviceInventory()[deviceId] : [:]
}

public Map getInventoryStatus() {
    Map inventory = getDeviceInventory()
    return [
        deviceCount: inventory.size(),
        presentCount: inventory.values().count { it.presentInHubitat != false },
        lastScan: state.lastScan,
        lastResult: state.lastScanResult,
        lastScanReason: state.lastScanReason,
        durationMs: state.lastScanDurationMs
    ]
}

private void logMessage(String level, String message) {
    String normalized = (level ?: 'info').toLowerCase()
    if (settings["log${normalized.capitalize()}Enable"] == false) return
    log."${normalized}" "AMA Device Handling: ${message}"
}
private void logInfo(String message)  { logMessage('info', message) }
private void logDebug(String message) { logMessage('debug', message) }
private void logTrace(String message) { logMessage('trace', message) }
private void logWarn(String message)  { logMessage('warn', message) }
private void logError(String message) { logMessage('error', message) }
