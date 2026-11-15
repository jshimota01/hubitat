/**
 * v0.3.3 - 2026/11/11 - Adds parent-provided Amazon endpoint ownership and observed exposure details.
 *   - Displays Amazon inventory freshness and source totals without contacting the NAS directly.
 *   - Shows per-device endpoint owner, skill stage, alias, enabled state, and reachability in review.
 *   - Keeps observed exposure separate from approval and candidate scan state.
 * v0.3.2 - 2026/11/10 - Aligns Candidate Selection app identity and parent-facing module naming.
 * v0.3.1 - 2026/11/10 - Renames the Hubitat child app type to AMA Candidate Selection and aligns parent identity.
 * Alexa Manager Advanced - Device Handling Child Application
 * Platform: Hubitat Elevation C-7
 * Namespace: jshimota
 * Version: 0.3.3
 *
 * Changelog:
 * v0.3.3 - 2026/11/11 - Adds parent-provided Amazon endpoint ownership and observed exposure details.
 *   - Adds current/stale inventory status and per-device endpoint details to the read-only review page.
 * v0.3.1 - 2026/11/10 - Renames the branch to Candidate Selection and strengthens candidate metadata.
 *   - Captures Hubitat platform version and installed driver namespace/name inventory.
 *   - Captures serial/software/hardware/driver version values when drivers publish them.
 *   - Separates a device whose driver is a Group from a device that may belong to a Group.
 *   - Changes unsupported negative Group membership conclusions to NOT_ASSESSED.
 * v0.2.0 - 2026/11/10 - Removes redundant Display Name and adds voice-intelligence inventory metadata.
 *   - Keeps Hubitat Name, Hubitat Label, and Amazon Name as the three identity fields.
 *   - Defaults a new Amazon Name to Hubitat Label, preserving any existing nonblank value.
 *   - Adds best-effort Group membership evidence for downstream voice checks.
 *   - Removes Display Name from newly collected and retained inventory records.
 * v0.1.4 - 2026/11/10 - Adds collected-data review and explicit scan verification states.
 *   - Combines currentStates snapshots with currentValue() reads for supported attributes.
 *   - Distinguishes verified, failed, filtered, and not-checked inventory records.
 *   - Adds a read-only device inventory review page.
 * v0.1.3 - 2026/11/09 - Formalized parent-only communication and module lifecycle.
 *   - Publishes completed inventory updates to the parent; never calls another child.
 *   - Adds a public module status API and versioned Hubitat Apps label.
 *   - Preserves inventory state and existing application identity.
 * v0.1.2 - 2026/11/09 - Replaced unsupported getAllDevices() with explicit capability.* device authorization.
 *   - Integrated Device Information Viewer collection of device.data and currentStates.
 *   - Preserves previously inventoried devices not selected in a particular scan.
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

String version() { return '0.3.3' }
String timeStamp() { return '2026/11/11' }

definition(
    name: 'AMA Candidate Selection',
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
    page(name: 'reviewPage')
}

def mainPage() {
    updateAppLabel()
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'>" +
                      "<h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Candidate Selection</h2>" +
                      "<span style='font-size:12px;opacity:0.85;'>Candidate inventory v${version()} (${timeStamp()})</span></div>"
            Map status = getInventoryStatus()
            paragraph "<b>Last scan:</b> ${status.lastScan ?: 'Never'}"
            paragraph "<b>Devices in library:</b> ${status.deviceCount ?: 0}"
            paragraph "<b>Verified present:</b> ${status.presentCount ?: 0}"
            paragraph "<b>Not verified:</b> ${status.unverifiedCount ?: 0}"
            paragraph "<b>Last scan result:</b> ${status.lastResult ?: 'No scan performed'}"
            paragraph "<b>Hubitat platform:</b> ${getHubEnvironment().platformVersion ?: 'Not reported'}"
            paragraph "<b>Installed driver definitions:</b> ${getHubEnvironment().installedDriverCount ?: 0}"
        }

        section('CANDIDATE SELECTION') {
            paragraph 'Select Hubitat devices that AMA should consider as candidates. Scanning reads device metadata and current values; it does not expose devices to Alexa or change device configuration.'
            paragraph 'Hubitat custom apps can only inspect devices you explicitly authorize. Select the devices AMA may scan below; use the selector to choose the full set you want included. This selection does not publish devices to Amazon.'
            input name: 'devicesToScan', type: 'capability.*', title: 'Potential Candidates to Evaluate', multiple: true, required: false, submitOnChange: true
            input name: 'btnScanDevices', type: 'button', title: 'Collect Candidate Information'
            href name: 'reviewCollectedData', page: 'reviewPage', title: 'Review Candidate Inventory', description: 'Inspect the inventory records collected from Hubitat. This view is read-only.'
            input name: 'includeVirtualDevices', type: 'bool', title: 'Include Virtual Devices', defaultValue: true, submitOnChange: true
        }

        section('CANDIDATE INFORMATION COLLECTED') {
            paragraph 'Candidate records include Hubitat Name and Label, Room, device ID, driver identity, manufacturer/model/firmware data, detected capabilities, supported attributes and commands, current values, and available hardware/software identifiers.'
            paragraph 'Amazon Name, Alexa capability assessment, naming intelligence, and preparation decisions belong to Candidate Preparation. Endpoint lifecycle belongs to Exposure Management.'
            Map amazonStatus = parent?.getAmazonInventoryStatus() ?: [:]
            Map sourceCounts = amazonStatus.counts instanceof Map ? amazonStatus.counts : [:]
            paragraph "<b>Amazon inventory:</b> ${amazonStatus.available == true ? (amazonStatus.fresh == true ? 'Current' : 'Stale') : 'Not checked'} | <b>Endpoints:</b> ${amazonStatus.endpointCount ?: 0}"
            paragraph "<b>Observed owners:</b> AMA ${sourceCounts.AMA ?: 0} | Legacy Hubitat ${sourceCounts.LEGACY_HUBITAT ?: 0}"
            paragraph "<b>Last Amazon refresh:</b> ${htmlSafe(amazonStatus.retrievedAt)}"
        }

        section('LOGGING', hideable: true, hidden: true) {
            input name: 'logInfoEnable', type: 'bool', title: 'Enable Info Logging', defaultValue: true
            input name: 'logDebugEnable', type: 'bool', title: 'Enable Debug Logging', defaultValue: false
            input name: 'logTraceEnable', type: 'bool', title: 'Enable Trace Logging', defaultValue: false
        }
    }
}

def reviewPage() {
    updateAppLabel()
    Map inventory = getDeviceInventory()
    List ids = inventory.keySet().collect { it.toString() }.sort { a, b ->
        String an = inventory[a]?.hubitatLabel ?: inventory[a]?.hubitatName ?: a
        String bn = inventory[b]?.hubitatLabel ?: inventory[b]?.hubitatName ?: b
        an.toLowerCase() <=> bn.toLowerCase()
    }
    Map options = [:]
    ids.each { id ->
        Map item = inventory[id] instanceof Map ? inventory[id] : [:]
        String label = item.hubitatLabel ?: item.hubitatName ?: "Device ${id}"
        options[id] = "${label} (ID ${id})"
    }
    String selectedId = settings.reviewDeviceId?.toString()
    Map selected = selectedId && inventory[selectedId] instanceof Map ? inventory[selectedId] : [:]
    Map amazonExposure = selectedId ? (parent?.getAmazonExposureForDevice(selectedId) ?: [:]) : [:]

    dynamicPage(name: 'reviewPage', title: 'Review Collected Data', install: false, uninstall: false) {
        section() {
            paragraph "<b>Inventory records:</b> ${inventory.size()} &nbsp; <b>Last scan:</b> ${state.lastScan ?: 'Never'}"
            if (!ids) {
                paragraph 'No inventory records are available yet. Return to Candidate Selection and scan selected devices.'
            } else {
                input name: 'reviewDeviceId', type: 'enum', title: 'Select a device to inspect', options: options,
                      required: false, submitOnChange: true
            }
        }
        if (selected) {
            section('DEVICE IDENTITY') {
                paragraph "<b>Device ID:</b> ${htmlSafe(selected.deviceId)}"
                paragraph "<b>Hubitat Name:</b> ${htmlSafe(selected.hubitatName)}"
                paragraph "<b>Hubitat Label:</b> ${htmlSafe(selected.hubitatLabel)}"
                                paragraph "<b>Room:</b> ${htmlSafe(selected.roomName)}"
                paragraph "<b>Driver:</b> ${htmlSafe(selected.driverType)}"
                paragraph "<b>Driver Namespace:</b> ${htmlSafe(selected.driverNamespace)}"
                paragraph "<b>Driver Version:</b> ${htmlSafe(selected.driverVersion ?: 'Not published by driver')}"
                paragraph "<b>Device Network ID:</b> ${htmlSafe(selected.deviceNetworkId)}"
            }
            section('SCAN VERIFICATION') {
                paragraph "<b>Verification:</b> ${htmlSafe(selected.verificationStatus ?: 'LEGACY / NOT CLASSIFIED')}"
                paragraph "<b>Present in Hubitat:</b> ${selected.presentInHubitat == true ? 'Yes (verified)' : 'Not verified'}"
                paragraph "<b>Last verified:</b> ${htmlSafe(selected.lastVerifiedAt)}"
                paragraph "<b>Last seen in scan:</b> ${htmlSafe(selected.lastSeenInScan)}"
                paragraph "<b>Group membership:</b> ${htmlSafe(selected.groupMembershipStatus ?: 'NOT DETECTED')}"
                paragraph "<b>Group evidence:</b><br/>${formatListForDisplay(selected.groupMembershipEvidence as List)}"
                paragraph 'Group detection is best-effort. NOT DETECTED means no membership marker was exposed by the device metadata inspected during this scan.'
                paragraph "<b>First discovered:</b> ${htmlSafe(selected.firstDiscovered)}"
                paragraph "<b>Last discovered:</b> ${htmlSafe(selected.lastDiscovered)}"
            }
            section('OBSERVED AMAZON EXPOSURE') {
                paragraph "<b>Inventory status:</b> ${htmlSafe(amazonExposure.status ?: 'NOT_CHECKED')}"
                paragraph "<b>Snapshot:</b> ${amazonExposure.snapshotFresh == true ? 'Current' : 'Not current / not checked'} | <b>Last refresh:</b> ${htmlSafe(amazonExposure.retrievedAt)}"
                if (amazonExposure.endpoints instanceof List && amazonExposure.endpoints) {
                    amazonExposure.endpoints.each { endpoint ->
                        paragraph "<b>Owner:</b> ${htmlSafe(endpoint.owner)} | <b>Amazon name:</b> ${htmlSafe(endpoint.name)}"
                        paragraph "<b>Friendly name:</b> ${htmlSafe(endpoint.friendlyName)} | <b>Aliases:</b> ${htmlSafe((endpoint.aliases ?: []).join(', '))}"
                        paragraph "<b>Skill stage:</b> ${htmlSafe(endpoint.skillStage)} | <b>Enabled:</b> ${endpoint.isEnabled == null ? 'Unknown' : (endpoint.isEnabled ? 'Yes' : 'No')} | <b>Reachability:</b> ${htmlSafe(endpoint.reachability)}"
                        paragraph "<b>Amazon entity:</b> ${htmlSafe(endpoint.entityId)}"
                    }
                } else if (amazonExposure.status == 'NOT_FOUND') {
                    paragraph 'No matching endpoint was found in the current Amazon inventory snapshot.'
                } else {
                    paragraph 'No conclusion is made because a current Amazon inventory snapshot is unavailable.'
                }
                paragraph 'Observed Amazon exposure is informational. It does not approve a device or change its endpoint.'
            }
            section('DEVICE DATA') {
                paragraph "<b>Manufacturer:</b> ${htmlSafe(selected.manufacturer)}"
                paragraph "<b>Model:</b> ${htmlSafe(selected.model)}"
                paragraph "<b>Firmware:</b> ${htmlSafe(selected.firmwareVersion)}"
                paragraph "<b>Serial Number:</b> ${htmlSafe(selected.serialNumber)}"
                paragraph "<b>Software Version:</b> ${htmlSafe(selected.softwareVersion)}"
                paragraph "<b>Hardware Version:</b> ${htmlSafe(selected.hardwareVersion)}"
                paragraph "<b>Data values:</b><br/>${formatMapForDisplay(selected.dataValues as Map)}"
            }
            section('CAPABILITIES & INTERFACE') {
                paragraph "<b>Capabilities:</b><br/>${formatListForDisplay(selected.capabilities as List)}"
                paragraph "<b>Supported attributes:</b><br/>${formatListForDisplay(selected.supportedAttributes as List)}"
                paragraph "<b>Supported commands:</b><br/>${formatListForDisplay(selected.supportedCommands as List)}"
            }
            section('CURRENT ATTRIBUTE VALUES') {
                paragraph formatMapForDisplay(selected.currentValues as Map)
            }
        }
        section() {
            href name: 'returnToDiscovery', page: 'mainPage', title: 'Return to Candidate Selection'
        }
    }
}

void installed() {
    updateAppLabel()
    logInfo "Installing Candidate Selection v${version()}"
    if (!(state.deviceInventory instanceof Map)) state.deviceInventory = [:]
}

void updated() {
    updateAppLabel()
    logInfo 'Updating Candidate Selection.'
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
    Set<String> inspectedIds = [] as Set
    Set<String> failedIds = [] as Set

    List fleet = []
    try {
        fleet = (settings.devicesToScan instanceof Collection) ? settings.devicesToScan.findAll { it != null } : []
        if (!fleet) {
            state.lastScanResult = 'NOT RUN: Select one or more devices in Potential Candidates to Evaluate before scanning.'
            logWarn state.lastScanResult
            return
        }
    } catch (Exception e) {
        state.lastScanResult = "FAILED: Unable to enumerate Hubitat devices (${e.message})"
        logError state.lastScanResult
        return
    }

    fleet.each { dev ->
        String fleetId = null
        try {
            fleetId = dev.id?.toString()
            if (fleetId) fleetIds << fleetId
            if (!includeVirtualDevices && isVirtualDevice(dev)) {
                if (fleetId) filteredIds << fleetId
                return
            }
            Map detected = inspectDevice(dev)
            String id = detected.deviceId
            if (!id) {
                failed++
                if (fleetId) failedIds << fleetId
                return
            }
            inspectedIds << id

            Map previous = oldInventory[id] instanceof Map ? oldInventory[id] : [:]
            Map merged = mergeDetectedMetadata(previous, detected)
            newInventory[id] = merged
            scanned++
            if (hasDetectedMetadataChanged(previous, detected)) changed++
        } catch (Exception e) {
            failed++
            if (fleetId) failedIds << fleetId
            logWarn "Could not inspect device ${safeDeviceLabel(dev)}: ${e.message}"
        }
    }

    /* Preserve records, but never describe an uninspected device as verified present. */
    oldInventory.each { id, entry ->
        String key = id.toString()
        if (!newInventory.containsKey(key)) {
            Map retained = new LinkedHashMap(entry as Map)
            if (fleetIds.contains(key)) {
                if (filteredIds.contains(key)) {
                    retained.verificationStatus = 'FILTERED_BY_SETTINGS'
                    retained.presentInHubitat = false
                } else if (failedIds.contains(key)) {
                    retained.verificationStatus = 'SCAN_FAILED'
                    retained.presentInHubitat = false
                } else {
                    retained.verificationStatus = 'SCAN_NOT_COMPLETED'
                    retained.presentInHubitat = false
                }
            } else {
                retained.verificationStatus = 'NOT_CHECKED_IN_THIS_SCAN'
                retained.presentInHubitat = false
            }
            newInventory[key] = retained
        }
    }

    String completed = formatTimestamp(now())
    newInventory.each { id, entry ->
        String key = id.toString()
        if (inspectedIds.contains(key)) {
            entry.presentInHubitat = true
            entry.verificationStatus = 'VERIFIED_PRESENT'
            entry.lastSeenInScan = completed
            entry.lastVerifiedAt = completed
        } else if (fleetIds.contains(key) && filteredIds.contains(key)) {
            entry.verificationStatus = 'FILTERED_BY_SETTINGS'
            entry.presentInHubitat = false
        } else if (fleetIds.contains(key) && failedIds.contains(key)) {
            entry.verificationStatus = 'SCAN_FAILED'
            entry.presentInHubitat = false
        }
    }

    state.deviceInventory = newInventory
    state.lastScan = completed
    state.lastScanDurationMs = now() - started
    state.lastScanResult = "Completed: ${scanned} inspected, ${changed} metadata changes, ${failed} errors."
    state.lastScanReason = reason
    logInfo "Device inventory scan complete (${reason}): ${scanned} inspected, ${changed} changed, ${failed} errors, ${newInventory.size()} library records."
    /* Send the result UP to the parent. The parent routes it to interested branches. */
    try { parent?.childInventoryUpdated(getInventoryStatus()) }
    catch (Exception e) { logWarn "Parent inventory notification failed: ${e.message}" }
}

private Map getHubEnvironment() {
    Map cached = state.hubEnvironment instanceof Map ? state.hubEnvironment : [:]
    if (cached && cached.checkedAt && (now() - (cached.checkedAt as Long)) < 300000L) return cached
    String platform = null
    try { platform = getPlatformVersion()?.toString() } catch (Exception ignored) { }
    List drivers = []
    try {
        def installed = getInstalledDrivers()
        if (installed instanceof Collection) {
            drivers = installed.collect { item ->
                [name:item.name?.toString(), namespace:item.namespace?.toString()]
            }.findAll { it.name || it.namespace }
        }
    } catch (Exception ignored) { }
    Map result = [platformVersion:platform, installedDriverCount:drivers.size(),
                  installedDrivers:drivers, checkedAt:now()]
    state.hubEnvironment = result
    return result
}

private Map resolveDriverInfo(String driverType, Map dataValues) {
    String namespace = null
    String version = null
    try {
        List drivers = getHubEnvironment().installedDrivers ?: []
        List matches = drivers.findAll { it.name?.toString() == driverType }
        if (matches.size() == 1) namespace = matches[0].namespace?.toString()
    } catch (Exception ignored) { }
    ['driverVersion','driver_version','Driver Version'].each { key ->
        if (!version && dataValues?.get(key)?.toString()?.trim()) version = dataValues.get(key).toString().trim()
    }
    return [namespace:namespace, version:version]
}

private Map inspectDevice(dev) {
    Map attributes = readDefinitionNames(dev, 'getSupportedAttributes')
    Map commands = readDefinitionNames(dev, 'getSupportedCommands')
    Map capabilities = readCapabilities(dev)
    Map currentValues = readCurrentStates(dev)
    /* currentStates is a useful snapshot; currentValue() supplements wrappers/drivers
       where the snapshot is incomplete or unavailable. */
    currentValues.putAll(readCurrentValues(dev, attributes.names as List))
    Map dataValues = readDataValues(dev)
    Map groupInfo = detectGroupMembership(dev, dataValues, currentValues)
    Map driverInfo = resolveDriverInfo(dev.typeName?.toString(), dataValues)
    String driverType = safeString(dev.typeName) ?: 'Unknown Driver'
    boolean isGroupDevice = driverType.toLowerCase().contains('group')

    String id = dev.id?.toString()
    String hubitatName = safeString(dev.name)
    String hubitatLabel = safeString(dev.label)
        return [
        deviceId: id,
        hubitatName: hubitatName,
        hubitatLabel: hubitatLabel,
        roomName: safeString(readProperty(dev, 'roomName')),
        driverType: driverType,
        driverNamespace: driverInfo.namespace,
        driverVersion: driverInfo.version,
        isGroupDevice: isGroupDevice,
        groupDeviceDriver: isGroupDevice ? driverType : null,
        deviceNetworkId: safeString(dev.deviceNetworkId),
        manufacturer: dataValues.manufacturer ?: dataValues.MANUFACTURER ?: null,
        model: dataValues.model ?: dataValues.MODEL ?: null,
        firmwareVersion: dataValues.firmwareVersion ?: dataValues.firmware ?: null,
        serialNumber: dataValues.serialNumber ?: dataValues.serial ?: null,
        softwareVersion: dataValues.softwareVersion ?: dataValues.software_version ?: null,
        hardwareVersion: dataValues.hardwareVersion ?: dataValues.hardware_version ?: null,
        dataValues: dataValues,
        capabilities: capabilities.names,
        capabilityDetectionMethod: capabilities.method,
        supportedAttributes: attributes.names,
        supportedCommands: commands.names,
        currentValues: currentValues,
        groupMembershipStatus: groupInfo.status,
        groupMembershipEvidence: groupInfo.evidence,
        hubitatPlatformVersion: getHubEnvironment().platformVersion,
        detectedAt: formatTimestamp(now())
    ]
}

private Boolean hasDetectedMetadataChanged(Map previous, Map detected) {
    if (!previous) return true
    List scannerFields = ['hubitatName','hubitatLabel','roomName','driverType','driverNamespace','driverVersion','isGroupDevice','groupDeviceDriver','deviceNetworkId','manufacturer','model','firmwareVersion','serialNumber','softwareVersion','hardwareVersion','dataValues','capabilities','capabilityDetectionMethod','supportedAttributes','supportedCommands','currentValues','groupMembershipStatus','groupMembershipEvidence','hubitatPlatformVersion']
    return scannerFields.any { key -> previous[key] != detected[key] }
}

private Map mergeDetectedMetadata(Map previous, Map detected) {
    Map merged = new LinkedHashMap(previous ?: [:])
    merged.remove('displayName')
    /* Scanner-owned fields are refreshed. User/AMA-owned fields are deliberately retained. */
    detected.each { key, value -> merged[key] = value }
    merged.presentInHubitat = true
    merged.firstDiscovered = previous.firstDiscovered ?: detected.detectedAt
    merged.lastDiscovered = detected.detectedAt
    /* Amazon Name starts from Hubitat Label, but a nonblank managed name is never overwritten. */
    if (!(merged.amazonName?.toString()?.trim()) || merged.amazonNameDefaultedFromLabel == true) {
        merged.amazonName = detected.hubitatLabel ?: detected.hubitatName ?: ''
        merged.amazonNameDefaultedFromLabel = true
    }
    if (!merged.containsKey('amazonCapabilities')) merged.amazonCapabilities = []
    if (!merged.containsKey('approvedForAmazon')) merged.approvedForAmazon = false
    if (!merged.containsKey('exposureStatus')) merged.exposureStatus = 'NOT_EXPOSED'
    if (!merged.containsKey('namingReviewStatus')) merged.namingReviewStatus = 'NOT_REVIEWED'
    return merged
}

/*
 * Best-effort membership detection from metadata actually exposed by the device.
 * Hubitat drivers do not all publish group membership in a common field. A negative
 * result therefore means "not detected from available metadata", not proof of no group.
 */
private Map detectGroupMembership(dev, Map dataValues, Map currentValues) {
    List evidence = []
    Map sources = [dataValues:(dataValues ?: [:]), currentValues:(currentValues ?: [:])]
    sources.each { sourceName, sourceMap ->
        sourceMap.each { key, value ->
            String normalizedKey = key.toString().toLowerCase().replaceAll('[^a-z0-9]', '')
            String textValue = value == null ? '' : value.toString().trim()
            boolean membershipKey = normalizedKey in [
                'group', 'groups', 'groupid', 'groupids', 'groupname', 'groupnames',
                'groupmembership', 'groupmemberships', 'memberof', 'memberofgroups',
                'hubitatgroup', 'hubitatgroups', 'groupmember', 'isgroupmember'
            ]
            boolean membershipText = textValue.toLowerCase().contains('group member') ||
                                     textValue.toLowerCase().contains('member of group')
            if ((membershipKey && textValue && !(textValue.equalsIgnoreCase('false')) &&
                 !(textValue.equalsIgnoreCase('no')) && !(textValue.equals('[]'))) || membershipText) {
                evidence << "${sourceName}.${key}=${textValue.take(120)}"
            }
        }
    }
    return [status:evidence ? 'POTENTIAL_GROUP_MEMBER_HINT' : 'NOT_ASSESSED',
            evidence:evidence.unique()]
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
    /* Device Information Viewer pattern: preserve all driver-provided data values. */
    try {
        def rawData = dev.data
        if (rawData instanceof Map) {
            rawData.each { key, value ->
                if (value != null) values[key.toString()] = value.toString()
            }
        }
    } catch (Exception ignored) { }
    /* Also try commonly used keys individually for wrappers exposing getDataValue(). */
    ['manufacturer','model','firmwareVersion','firmware','hardwareVersion','protocol','deviceType','deviceClass','application','endpointId'].each { key ->
        try {
            def value = dev.getDataValue(key)
            if (value != null && value.toString().trim() && !values.containsKey(key)) values[key] = value.toString()
        } catch (Exception ignored) { }
    }
    return values
}

private Map readCurrentStates(dev) {
    Map values = [:]
    /* Device Information Viewer pattern: iterate the currentStates snapshot. */
    try {
        dev.currentStates?.each { stateItem ->
            String attrName = safeString(stateItem.name)
            if (attrName) values[attrName] = stateItem.value
        }
    } catch (Exception ignored) { }
    return values
}

private String htmlSafe(Object value) {
    if (value == null) return 'Not reported'
    String s = value.toString()
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
             .replace('"', '&quot;').replace("'", '&#39;')
}

private String formatListForDisplay(List values) {
    List safe = (values ?: []).collect { htmlSafe(it) }
    return safe ? safe.join('<br/>') : 'None reported'
}

private String formatMapForDisplay(Map values) {
    if (!(values instanceof Map) || !values) return 'None reported'
    return values.collect { key, value ->
        "<b>${htmlSafe(key)}:</b> ${htmlSafe(value)}"
    }.join('<br/>')
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
    return safeString(dev.label) ?: safeString(dev.name) ?: "device ${dev.id}"
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

public void updateAmazonInventoryStatus(Map status = [:]) {
    state.amazonInventoryLastReceivedAt = now()
    state.amazonInventoryStatus = [available:status.available == true, fresh:status.fresh == true,
        endpointCount:status.endpointCount ?: 0, retrievedAt:status.retrievedAt, counts:status.counts ?: [:]]
    logDebug "Parent delivered Amazon endpoint inventory status (${status.endpointCount ?: 0} endpoints)."
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
        presentCount: inventory.values().count { it.presentInHubitat == true },
        unverifiedCount: inventory.values().count { it.presentInHubitat != true },
        lastScan: state.lastScan,
        lastResult: state.lastScanResult,
        lastScanReason: state.lastScanReason,
        durationMs: state.lastScanDurationMs,
        hubitatPlatformVersion: getHubEnvironment().platformVersion,
        installedDriverCount: getHubEnvironment().installedDriverCount
    ]
}

public Map getModuleStatus() {
    return [installed:true, module:'Candidate Selection', version:version(),
            deviceCount:getInventoryStatus().deviceCount ?: 0,
            presentCount:getInventoryStatus().presentCount ?: 0,
            unverifiedCount:getInventoryStatus().unverifiedCount ?: 0,
            lastScan:state.lastScan, lastResult:state.lastScanResult]
}

private void updateAppLabel() {
    String expected = 'AMA Candidate Selection v' + version()
    if (app.label != expected) app.updateLabel(expected)
}

private void logMessage(String level, String message) {
    String normalized = (level ?: 'info').toLowerCase()
    if (settings["log${normalized.capitalize()}Enable"] == false) return
    log."${normalized}" "AMA Candidate Selection: ${message}"
}
private void logInfo(String message)  { logMessage('info', message) }
private void logDebug(String message) { logMessage('debug', message) }
private void logTrace(String message) { logMessage('trace', message) }
private void logWarn(String message)  { logMessage('warn', message) }
private void logError(String message) { logMessage('error', message) }
