/**
 * Application Name: Echos Speak Environment Manager
 * Platform: Hubitat Elevation (C-7 / OS 2.4.4.156)
 * Notes: Centralized environmental state matrix (Do Not Disturb, Mute, Volume, Display Brightness, Display Power, Adaptive Brightness), mode reconciliation, and command pacing for Echo Speaks devices.
 * Category: Utility / Convenience / Environmental
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
 *  Provides central 5-Section Shared Settings Matrix state management per Hubitat Mode + Sleeping/Away.
 *  Includes support for Adaptive Display Brightness handling, command queuing, and local SPA REST integration.
 *
 *  Changelog:
 *  v3.7.7 - 2026/09/29 - Added Diagnostic DND vs Mute Persistence Tracing:
 *                          - Promoted version to v3.7.7.
 *                          - Added comparative diagnostic tracing in apiSaveMatrix() to log requested value vs immediate settings[key] value for both DND and Mute keys.
 *                          - Added DND diagnostic tracing in apiGetMatrix() alongside Mute diagnostics to compare readback behaviors.
 *  v3.7.6 - 2026/09/29 - Settings Persistence "no_change" Sanitization Fix[cite: 1]:
 *                          - Sanitized apiSaveMatrix() to filter out default "no_change" string values posted by form/SPA submissions[cite: 1].
 *                          - Prevented setting corruption where "no_change" values were silently overwriting persisted Mute matrix booleans with false[cite: 1].
 *  v3.7.5 - 2026/09/29 - Added Diagnostic REST Mute Endpoint Tracing[cite: 1]:
 *                          - Added targeted debug logging in apiGetMatrix() to output detailed Mute setting evaluation diagnostics[cite: 1].
 *  v3.7.4 - 2026/09/29 - Mute Matrix REST Payload Normalization Fix[cite: 1]:
 *                          - Robustified muteMap boolean evaluation in apiGetMatrix() to recognize true, "true", "muted", and "on" setting representations[cite: 1].
 *                          - Guaranteed persisted Mute matrix values display accurately in SPA UI on reload without relying on physical device states[cite: 1].
 *  v3.7.3 - 2026/09/29 - Muted Device Volume Sync State Resolution[cite: 1]:
 *                          - Refactored volInSync check in apiGetState() to ignore reported volume (0) while device target or actual state is muted[cite: 1].
 *  v3.7.2 - 2026/09/29 - REST Apply Payload Evaluation Bug Fix[cite: 1].
 *  v3.7.1 - 2026/09/29 - Delay Removal & Clean Logging Refactor[cite: 1].
 *  v3.7.0 - 2026/09/29 - Section-Isolated Persistence & Dynamic Tab Action Bar Refactor[cite: 1].
 *  v3.6.9 - 2026/09/29 - UI Layout, Table Styling & Version Sync Refactor[cite: 1].
 *  v3.6.8 - 2026/09/28 - Simplified Mute State Matching to Mirror DND Logic[cite: 1].
 *  v3.6.7 - 2026/09/28 - REST Boundary Diagnostic Logging & Mute Normalization[cite: 1].
 *  v3.6.6 - 2026/09/28 - Mute Engine Command Dispatch Alignment[cite: 1].
 *  v3.6.5 - 2026/09/28 - Explicit Dual-Setting Adaptive Display Engine[cite: 1].
 *  v3.6.4 - 2026/09/28 - Adaptive Brightness Command Routing & SPA Payload Alignment[cite: 1].
 *  v3.6.3 - 2026/09/28 - Application Layer Ownership Correction & Adaptive Brightness Attribute Alignment[cite: 1].
 *  v3.6.2 - 2026/09/28 - Corrected unmanaged WHA calculation by inspecting parent.getChildDevices()[cite: 1].
 *  v3.6.1 - 2026/09/28 - Added unmanaged WHA device calculation to REST API state payload[cite: 1].
 *  v3.6.0 - 2026/09/28 - Adaptive Display Brightness Engine & Matrix Refactor[cite: 1].
 *  v3.5.1 - 2026/09/28 - Encoding Sanitization & Button Style Refactor[cite: 1].
 *  v3.5.0 - 2026/09/28 - Standalone Local REST/SPA Architecture Migration[cite: 1].
 *  v3.0.0 - 2026/08/15 - Initial release under Echos Speak Advanced suite[cite: 1].
 **/

static String version() { return '3.7.7' }
def timeStamp() { return "2026/09/29 11:50 AM" }

definition(
    name: "Echos Speak Environment Manager",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Centralized mode-based 5-section shared settings matrix editor and command queuing for physical Echo devices via standalone local SPA dashboard.",
    category: "Utility",
    parent: "jshimota:Echos Speak Advanced Manager",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: true,
    oauth: true,
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/echos_speak_environment_manager/echos_speak_environment_manager.groovy"
)

preferences {
    page(name: "mainPage")
}

mappings {
    path('/ui.html')              { action: [GET: 'serveUI'] }
    path('/api/state')            { action: [GET: 'apiGetState'] }
    path('/api/matrix')           { action: [GET: 'apiGetMatrix'] }
    path('/api/matrix/save')      { action: [POST: 'apiSaveMatrix'] }
    path('/api/matrix/apply')     { action: [POST: 'apiApplyMatrix'] }
    path('/api/refresh')          { action: [POST: 'apiRefreshFleet'] }
}

def mainPage(Map pageParams = [:]) {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()
        checkAndLogVersionDemarcation()
        ensureAccessToken()

        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Echos Speak Environment Manager</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        section("<b>Interactive Control Dashboard</b>") {
            if (state.accessToken) {
                String dashboardUrl = "${fullLocalApiServerUrl}/ui.html?access_token=${state.accessToken}"
                paragraph "Launch the standalone interactive matrix control dashboard:"
                paragraph "<a href='${dashboardUrl}' target='_blank' style='background-color: #28a745; color: #ffffff !important; padding: 12px 20px; text-decoration: none; border-radius: 4px; display: inline-block; font-weight: bold; font-size: 14px; box-shadow: 0 2px 4px rgba(0,0,0,0.2);'>&#128640; Open Environment Manager Dashboard</a>"
            } else {
                paragraph "<div style='background-color:#FADBD8; border-left:4px solid #C0392B; padding:10px; border-radius:4px; font-size:12px; color:#78281F;'>" +
                          "&#9888;&#65039; <b>OAuth Token Missing:</b> OAuth is enabled in driver code, but a local access token has not yet been granted. Please click <b>Done</b> or enable OAuth in App Settings.</div>"
            }
        }

        section("<b>Command Queue & Execution Settings</b>", hideable: true, hidden: true) {
            input name: "queueDelayMs", type: "number", title: "Pacing Delay between queued commands (ms):", defaultValue: 350, range: "100..2000", required: true
            input name: "forceStateReconcile", type: "bool", title: "Bypass State Check (Force send commands even if state matches)", defaultValue: false
            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:10px 0;'/>"
            input name: "dryRunMode", type: "bool", title: "Enable Dry Run Mode (Calculate & log proposed changes without sending commands)", defaultValue: false, submitOnChange: true
            input name: "btnReconcileNow", type: "button", title: "Reconcile Now (Trigger Matrix State Sync)", width: 6
            input name: "btnClearQueue", type: "button", title: "Clear Command Queue", width: 6
        }

        section("<b>App Preferences & Logging Options</b>", hideable: true, hidden: true) {
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

private void ensureAccessToken() {
    if (!state.accessToken) {
        try {
            createAccessToken()
            logInfo "Generated new local access token for Environment Manager UI."
        } catch (e) {
            logError "Error generating local access token: ${e.message}"
        }
    }
}

Map jsonResponse(Map data) {
    return render(status: 200, contentType: 'application/json', data: groovy.json.JsonOutput.toJson(data))
}

Map serveUI() {
    if (!state.accessToken) {
        return render(status: 403, contentType: 'text/plain', data: 'Access denied: Token missing.')
    }
    
    try {
        byte[] htmlBytes = downloadHubFile('environment_manager_ui.html')
        if (!htmlBytes) {
            return render(status: 404, contentType: 'text/plain', data: 'environment_manager_ui.html not found in Hubitat File Manager.')
        }
        
        String html = new String(htmlBytes, 'UTF-8')
        html = html.replace('${access_token}', state.accessToken)
                   .replace('${api_base}', fullLocalApiServerUrl)
                   
        return render(status: 200, contentType: 'text/html', data: html)
    } catch (Exception e) {
        logError "Error serving UI: ${e.message}"
        return render(status: 500, contentType: 'text/plain', data: "Error loading interface: ${e.message}")
    }
}

Map apiGetState() {
    List<String> selectedDnis = getSelectedDeviceDnis()
    Map currentDesired = atomicState.desiredState ?: [:]
    List deviceList = []

    Map managedInventory = parent ? parent.getManagedEchoDeviceInventory() : [:]
    int unmanagedCount = managedInventory ? managedInventory.values().count { entry ->
        entry?.family?.toString()?.trim()?.toUpperCase() == "WHA"
    } : 0

    List<String> sortedDnis = selectedDnis.sort { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        return dev ? dev.displayName?.toLowerCase() : dni.toLowerCase()
    }

    sortedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (dev) {
            Map profile = getDeviceProfile(dev)
            Map normState = normalizeDeviceState(dev, profile)
            Map dMap = currentDesired[dni] ?: [:]

            Boolean isMuted = (dMap.mute == true || normState.actMute == "muted")
            Boolean volInSync = isMuted || (dMap.volume == null || (normState.actVol != null && normState.actVol == dMap.volume))
            Boolean muteInSync = (dMap.mute == null || (normState.actMute != null && (dMap.mute ? normState.actMute == "muted" : normState.actMute == "unmuted")))
            Boolean dndInSync = (dMap.dnd == null || (normState.actDnd != null && (dMap.dnd ? normState.actDnd == "enabled" : normState.actDnd == "disabled")))
            
            Boolean dispInSync = true
            if (profile.hasDisplayBrightness) {
                if (dMap.adaptive == true) {
                    dispInSync = (normState.actAdaptive == "on")
                } else if (dMap.displayBrightness != null) {
                    dispInSync = (normState.actAdaptive != "on" && normState.actDisp != null && normState.actDisp == dMap.displayBrightness)
                }
            }

            Boolean dispPwrInSync = true
            if (profile.hasDisplayPower && dMap.displayPower != null) {
                dispPwrInSync = (normState.actDispPwr != null && (dMap.displayPower ? normState.actDispPwr == "on" : normState.actDispPwr == "off"))
            }

            Boolean fullySynced = volInSync && dispInSync && dispPwrInSync && muteInSync && dndInSync

            deviceList << [
                id: dni,
                label: dev.displayName,
                capabilities: [
                    volume: true,
                    mute: true,
                    dnd: true,
                    brightness: profile.hasDisplayBrightness,
                    adaptive: profile.hasAdaptiveBrightness,
                    power: profile.hasDisplayPower
                ],
                actual: [
                    volume: normState.actVol,
                    mute: normState.actMute,
                    dnd: normState.actDnd,
                    brightness: normState.actDisp,
                    adaptive: normState.actAdaptive,
                    power: normState.actDispPwr
                ],
                desired: [
                    volume: dMap.volume,
                    mute: dMap.mute,
                    dnd: dMap.dnd,
                    brightness: dMap.displayBrightness,
                    adaptive: dMap.adaptive,
                    power: dMap.displayPower
                ],
                synced: fullySynced
            ]
        }
    }

    List queue = atomicState.commandQueue ?: []
    String formattedSyncTime = state.lastSyncTimestamp ? state.lastSyncTimestamp : new Date().format("yyyy/MM/dd hh:mm:ss a", location.timeZone)

    return jsonResponse([
        activeMode: location.mode,
        activePresence: (state.activeColumnOverride ?: location.mode),
        activeColumn: (state.activeColumnOverride ?: location.mode),
        lastSynced: formattedSyncTime,
        queueDepth: queue.size(),
        unmanagedCount: unmanagedCount,
        devices: deviceList
    ])
}

Map apiGetMatrix() {
    List<String> selectedDnis = getSelectedDeviceDnis()
    List<String> columns = getActiveColumns()

    Map dndMap = [:]
    Map muteMap = [:]
    Map volMap = [:]
    Map dispMap = [:]
    Map adaptMap = [:]
    Map dispPwrMap = [:]

    selectedDnis.each { dni ->
        Map dndDev = [:]
        Map muteDev = [:]
        Map volDev = [:]
        Map dispDev = [:]
        Map adaptDev = [:]
        Map dispPwrDev = [:]

        columns.each { col ->
            String dndKey = "dnd_${dni}_${col}"
            def rawDndSetting = settings[dndKey]
            Boolean returnedDnd = (rawDndSetting != null && rawDndSetting.toString() != "no_change" && (rawDndSetting == true || rawDndSetting.toString() == "true"))
            dndDev[col] = returnedDnd

            String muteKey = "mute_${dni}_${col}"
            def rawMuteSetting = settings[muteKey]
            Boolean returnedMute = (rawMuteSetting != null && rawMuteSetting.toString() != "no_change" && (rawMuteSetting == true || rawMuteSetting.toString().toLowerCase() in ["true", "muted", "on"]))
            muteDev[col] = returnedMute

            logDebug "/api/matrix Read Diagnostic:\nDevice=${dni} | Col=${col}\n  DND Key=${dndKey} | RawDND=${rawDndSetting} | ReturnedDND=${returnedDnd}\n  Mute Key=${muteKey} | RawMute=${rawMuteSetting} | ReturnedMute=${returnedMute}"

            def rawVol = settings["vol_${dni}_${col}"]
            volDev[col] = (rawVol != null && rawVol.toString().trim() != "" && rawVol.toString() != "no_change" && rawVol.toString().isNumber()) ? rawVol.toInteger() : null

            def rawDisp = settings["disp_${dni}_${col}"]
            dispDev[col] = (rawDisp != null && rawDisp.toString().trim() != "" && rawDisp.toString() != "no_change" && rawDisp.toString().isNumber()) ? rawDisp.toInteger() : 50

            def rawAdapt = settings["adapt_${dni}_${col}"]
            adaptDev[col] = (rawAdapt != null && rawAdapt.toString() != "no_change" && (rawAdapt == true || rawAdapt.toString() == "true"))

            def rawPwr = settings["disppwr_${dni}_${col}"]
            dispPwrDev[col] = (rawPwr == null || rawPwr.toString() == "no_change" || rawPwr == true || rawPwr.toString() == "true")
        }

        dndMap[dni] = dndDev
        muteMap[dni] = muteDev
        volMap[dni] = volDev
        dispMap[dni] = dispDev
        adaptMap[dni] = adaptDev
        dispPwrMap[dni] = dispPwrDev
    }

    return jsonResponse([
        modes: columns,
        matrices: [
            dnd: dndMap,
            mute: muteMap,
            volume: volMap,
            brightness: dispMap,
            adaptive: adaptMap,
            power: dispPwrMap
        ]
    ])
}

Map apiSaveMatrix() {
    Map body = (request?.JSON instanceof Map) ? (Map) request.JSON : [:]
    Map matrices = body.matrices as Map
    if (!matrices) {
        return jsonResponse([success: false, error: "Invalid payload."])
    }

    int persistedCount = 0

    if (matrices.dnd instanceof Map) {
        matrices.dnd.each { dni, cols ->
            if (cols instanceof Map) {
                cols.each { col, val ->
                    if (val != null && val.toString() != "no_change") {
                        String key = "dnd_${dni}_${col}"
                        Boolean reqVal = (val == true || val.toString() == "true")
                        app.updateSetting(key, [type: "bool", value: reqVal])
                        def immediateVal = settings[key]
                        logDebug "PERSIST DIAGNOSTIC (DND):\n  Key: ${key}\n  Requested: ${reqVal}\n  Immediate settings[key]: ${immediateVal}"
                        persistedCount++
                    }
                }
            }
        }
    }

    if (matrices.mute instanceof Map) {
        matrices.mute.each { dni, cols ->
            if (cols instanceof Map) {
                cols.each { col, val ->
                    if (val != null && val.toString() != "no_change") {
                        String key = "mute_${dni}_${col}"
                        Boolean reqVal = (val == true || val.toString().toLowerCase() in ["true", "muted", "on"])
                        app.updateSetting(key, [type: "bool", value: reqVal])
                        def immediateVal = settings[key]
                        logDebug "PERSIST DIAGNOSTIC (MUTE):\n  Key: ${key}\n  Requested: ${reqVal}\n  Immediate settings[key]: ${immediateVal}"
                        persistedCount++
                    }
                }
            }
        }
    }

    if (matrices.volume instanceof Map) {
        matrices.volume.each { dni, cols ->
            if (cols instanceof Map) {
                cols.each { col, val ->
                    if (val != null && val.toString().trim() != "" && val.toString() != "no_change") {
                        if (val.toString().isNumber()) {
                            app.updateSetting("vol_${dni}_${col}", [type: "number", value: val.toInteger()])
                            persistedCount++
                        }
                    } else if (val == null) {
                        app.removeSetting("vol_${dni}_${col}")
                        persistedCount++
                    }
                }
            }
        }
    }

    if (matrices.brightness instanceof Map) {
        matrices.brightness.each { dni, cols ->
            if (cols instanceof Map) {
                cols.each { col, val ->
                    if (val != null && val.toString().trim() != "" && val.toString() != "no_change" && val.toString().isNumber()) {
                        app.updateSetting("disp_${dni}_${col}", [type: "number", value: val.toInteger()])
                        persistedCount++
                    }
                }
            }
        }
    }

    if (matrices.adaptive instanceof Map) {
        matrices.adaptive.each { dni, cols ->
            if (cols instanceof Map) {
                cols.each { col, val ->
                    if (val != null && val.toString() != "no_change") {
                        app.updateSetting("adapt_${dni}_${col}", [type: "bool", value: (val == true || val.toString() == "true")])
                        persistedCount++
                    }
                }
            }
        }
    }

    if (matrices.power instanceof Map) {
        matrices.power.each { dni, cols ->
            if (cols instanceof Map) {
                cols.each { col, val ->
                    if (val != null && val.toString() != "no_change") {
                        app.updateSetting("disppwr_${dni}_${col}", [type: "bool", value: (val == true || val.toString() == "true" || val.toString() == "on")])
                        persistedCount++
                    }
                }
            }
        }
    }

    logInfo "Saved ${persistedCount} matrix entries via REST API."
    return jsonResponse([success: true, persistedCount: persistedCount])
}

Map apiApplyMatrix() {
    Map body = (request?.JSON instanceof Map) ? (Map) request.JSON : [:]
    String sectionTitle = body.sectionTitle ? body.sectionTitle.toString().trim() : "Environment"
    Map submittedMatrices = body.matrices instanceof Map ? (Map) body.matrices : [:]

    if (submittedMatrices) {
        apiSaveMatrix()
    }
    
    evaluateCurrentModeState("REST API Matrix Apply Triggered", sectionTitle, submittedMatrices)
    List queue = atomicState.commandQueue ?: []
    
    return jsonResponse([success: true, queuedCommands: queue.size()])
}

Map apiRefreshFleet() {
    logInfo "Executing fleet device refresh via REST API call..."
    refreshManagedDeviceStates()
    return jsonResponse([success: true])
}

private List<String> getActiveColumns() {
    List<String> hubModes = location.modes ? location.modes.collect { it.name } : ["Day", "Evening", "Night"]
    List<String> cols = []
    
    hubModes.each { m -> if (!cols.contains(m)) cols.add(m) }
    if (!cols.contains("Sleeping")) cols.add("Sleeping")
    if (!cols.contains("Away")) cols.add("Away")
    
    return cols
}

private List<String> getSelectedDeviceDnis() {
    Map physMap = parent ? parent.getManagedPhysicalEchoDevices() : [:]
    if (!physMap) return []
    return physMap.keySet().collect { it.toString() }
}

private Map getDeviceProfile(def dev) {
    if (!dev) return [hasDisplayBrightness: false, hasAdaptiveBrightness: false, hasDisplayPower: false]
    Map caps = dev.hasCommand("getCapabilitiesMap") ? dev.getCapabilitiesMap() : (dev.getState()?.capabilitiesMap ?: [:])
    
    Boolean hasBrightness = (caps.displayBrightness == true)
    Boolean hasAdaptive   = (caps.adaptiveBrightness == true)
    Boolean hasPower      = (caps.display == true)
    
    return [hasDisplayBrightness: hasBrightness, hasAdaptiveBrightness: hasAdaptive, hasDisplayPower: hasPower]
}

private Map normalizeDeviceState(def dev, Map profile = [:]) {
    if (!dev) return [actVol: null, actDisp: null, actAdaptive: null, actDispPwr: null, actMute: null, actDnd: null]
    
    def rawVol = dev.currentValue("volume")
    Integer actVol = (rawVol != null && rawVol.toString().isNumber()) ? rawVol.toInteger() : null

    Integer actDisp = null
    String actAdaptive = "off"

    if (profile.hasDisplayBrightness) {
        def rawDispB = dev.currentValue("displayBrightness")
        def rawLevel = dev.currentValue("level")
        def selDisp = (rawDispB != null) ? rawDispB : rawLevel
        if (selDisp != null && selDisp.toString().isNumber()) {
            actDisp = selDisp.toInteger()
        }

        def rawAdapt = dev.currentValue("adaptiveBrightness") ?: dev.currentValue("adaptiveDisplay") ?: dev.currentValue("autobrightness")
        if (rawAdapt != null) {
            String aStr = rawAdapt.toString().toLowerCase()
            if (aStr in ["on", "true", "enabled"]) actAdaptive = "on"
        }
    }

    String actDispPwr = null
    if (profile.hasDisplayPower) {
        def rawPwr = dev.currentValue("displayPower")
        if (rawPwr != null) {
            String pStr = rawPwr.toString().toLowerCase()
            if (pStr in ["on", "true"]) actDispPwr = "on"
            else if (pStr in ["off", "false"]) actDispPwr = "off"
        }
    }

    def rawMute = dev.currentValue("mute")
    String actMute = "unmuted"
    if (rawMute != null) {
        String mStr = rawMute.toString().toLowerCase()
        if (mStr in ["muted", "true", "on"]) actMute = "muted"
        else if (mStr in ["unmuted", "false", "off"]) actMute = "unmuted"
    }

    def rawDnd = dev.currentValue("doNotDisturb")
    String actDnd = null
    if (rawDnd != null) {
        String dStr = rawDnd.toString().toLowerCase()
        if (dStr in ["enabled", "true", "on"]) actDnd = "enabled"
        else if (dStr in ["disabled", "false", "off"]) actDnd = "disabled"
    }

    return [
        actVol: actVol,
        actDisp: actDisp,
        actAdaptive: actAdaptive,
        actDispPwr: actDispPwr,
        actMute: actMute,
        actDnd: actDnd
    ]
}

void appButtonHandler(String btn) {
    if (!btn) return
    logTrace "appButtonHandler() triggered -> Button: [${btn}]"

    if (btn == "btnReconcileNow") {
        logInfo "Manual 'Reconcile Now' requested."
        evaluateCurrentModeState("Manual GUI Reconcile", "Environment")
    } else if (btn == "btnClearQueue") {
        logWarn "Manual 'Clear Queue' requested. Purging pending commands."
        atomicState.commandQueue = []
    }
}

private void refreshManagedDeviceStates() {
    List<String> selectedDnis = getSelectedDeviceDnis()
    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (dev) {
            try {
                if (dev.hasCommand("refresh")) {
                    dev.refresh()
                } else if (dev.hasCommand("poll")) {
                    dev.poll()
                }
            } catch (Exception e) {
                logWarn "Unable to execute poll/refresh on device [${dev.displayName}]: ${e.message}"
            }
        }
    }
    
    Map currentDesired = atomicState.desiredState ?: [:]
    Map cleanedDesired = [:]
    selectedDnis.each { dni ->
        if (currentDesired.containsKey(dni)) {
            def dev = parent ? parent.getManagedChildDevice(dni) : null
            if (dev) {
                Map profile = getDeviceProfile(dev)
                Map entry = currentDesired[dni] ?: [:]
                if (!profile.hasDisplayBrightness) entry.remove("displayBrightness")
                if (!profile.hasDisplayPower) entry.remove("displayPower")
                cleanedDesired[dni] = entry
            }
        }
    }
    atomicState.desiredState = cleanedDesired

    evaluateCurrentModeState("Device State Refresh Completed", "Environment")
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logInfo "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Echos Speak Environment Manager"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing app v${version()} (${timeStamp()})..."
    ensureAccessToken()
    atomicState.commandQueue = []
    atomicState.desiredState = [:]
    atomicState.reconciliationEpoch = 0
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."
    ensureAccessToken()
    unsubscribe()
    unschedule()
    initialize(false)
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling app..."
    unsubscribe()
}

private void initialize(Boolean isInstall = false) {
    checkAndLogVersionDemarcation()
    updateAppLabel()
    ensureAccessToken()

    subscribe(location, "mode", modeChangeHandler)
    subscribe(location, "sleeping", sleepingChangeHandler)
    logDebug "Subscribed to Location Mode and Sleeping status events."

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        unschedule("disableDebugLogging")
        runIn(1800, "disableDebugLogging", [overwrite: false])
    }

    evaluateCurrentModeState("App Initialized", "Environment")
}

void modeChangeHandler(evt) {
    logInfo "Location mode change event detected -> ${evt.value}"
    state.remove("activeColumnOverride")
    evaluateCurrentModeState("Mode Changed to [${evt.value}]", "Environment")
}

void sleepingChangeHandler(evt) {
    logInfo "Location sleeping event detected -> ${evt.value}"
    if (evt.value == "sleeping" || evt.value == "true" || evt.value == true) {
        setActiveColumn("Sleeping", "Location Sleeping Event")
    } else {
        state.remove("activeColumnOverride")
        evaluateCurrentModeState("Location Sleeping Ended", "Environment")
    }
}

public void setActiveColumn(String columnName, String reason = "External Command") {
    if (!columnName) return
    logInfo "External Active Column Override requested: [${columnName}] | Reason: ${reason}"
    state.activeColumnOverride = columnName
    evaluateMatrixForColumn(columnName, reason, "Environment")
}

private void evaluateCurrentModeState(String triggerReason, String sectionTitle = "Environment", Map submittedMatrices = null) {
    String activeCol = state.activeColumnOverride ?: location.mode
    evaluateMatrixForColumn(activeCol, triggerReason, sectionTitle, submittedMatrices)
}

public void evaluateMatrixForColumn(String columnName, String sourceReason, String sectionTitle = "Environment", Map submittedMatrices = null) {
    if (!columnName) return
    logInfo "Evaluating ${sectionTitle} Matrix for Mode [${columnName}]..."
    if (submittedMatrices) {
        logDebug "Evaluating matrix using submitted REST payload override for section [${sectionTitle}]."
    }

    state.lastSyncTimestamp = new Date().format("yyyy/MM/dd hh:mm:ss a", location.timeZone)
    long newEpoch = (atomicState.reconciliationEpoch ?: 0L) + 1L
    atomicState.reconciliationEpoch = newEpoch
    atomicState.commandQueue = []

    Map currentDesired = [:]
    Boolean forceDispatch = getSettingBool("forceStateReconcile", false)
    List<String> selectedDnis = getSelectedDeviceDnis()

    selectedDnis.each { dni ->
        def dev = parent ? parent.getManagedChildDevice(dni) : null
        if (!dev) return

        Map profile = getDeviceProfile(dev)
        Map normState = normalizeDeviceState(dev, profile)
        Map dMap = [:]

        // Target evaluation helper with REST override fallback to settings[...]
        def resolveTarget = { String sectionKey, String settingPrefix, Object defaultVal = null ->
            if (submittedMatrices && submittedMatrices.containsKey(sectionKey)) {
                Map secMap = submittedMatrices[sectionKey] instanceof Map ? (Map) submittedMatrices[sectionKey] : [:]
                Map devMap = secMap[dni] instanceof Map ? (Map) secMap[dni] : [:]
                if (devMap.containsKey(columnName)) {
                    def sumVal = devMap[columnName]
                    if (sumVal != null && sumVal.toString() != "no_change") {
                        return sumVal
                    }
                }
            }
            def storedVal = settings["${settingPrefix}_${dni}_${columnName}"]
            return (storedVal != null && storedVal.toString() != "no_change") ? storedVal : defaultVal
        }

        def rawVol = resolveTarget("volume", "vol")
        def rawDisp = resolveTarget("brightness", "disp")
        def rawAdapt = resolveTarget("adaptive", "adapt")
        def rawMute = resolveTarget("mute", "mute")
        def rawDnd = resolveTarget("dnd", "dnd")
        def rawPwr = resolveTarget("power", "disppwr")

        def targetVol = (rawVol != null && rawVol.toString().trim() != "" && rawVol.toString() != "no_change" && rawVol.toString().isNumber()) ? rawVol.toInteger() : null
        def targetDisp = (rawDisp != null && rawDisp.toString().trim() != "" && rawDisp.toString() != "no_change" && rawDisp.toString().isNumber()) ? rawDisp.toInteger() : 50
        Boolean targetAdaptBool = (rawAdapt != null && rawAdapt.toString() != "no_change" && (rawAdapt == true || rawAdapt.toString() == "true"))
        Boolean targetMuteBool  = (rawMute != null && rawMute.toString() != "no_change" && (rawMute == true || rawMute.toString().toLowerCase() in ["true", "muted", "on"]))
        Boolean targetDndBool   = (rawDnd != null && rawDnd.toString() != "no_change" && (rawDnd == true || rawDnd.toString() == "true"))
        Boolean targetDispPwr   = (rawPwr == null || rawPwr.toString() == "no_change" || rawPwr == true || rawPwr.toString() == "true" || rawPwr.toString() == "on")

        if (targetVol != null) {
            dMap.volume = targetVol
        }
        
        if (profile.hasDisplayBrightness) {
            dMap.adaptive = targetAdaptBool
            dMap.displayBrightness = targetDisp
        }
        
        if (profile.hasDisplayPower) {
            dMap.displayPower = targetDispPwr
        }

        dMap.mute = targetMuteBool
        dMap.dnd  = targetDndBool
        dMap.activeColName = columnName

        currentDesired[dni] = dMap

        // Volume Command Queue
        if (targetVol != null && (forceDispatch || normState.actVol != targetVol)) {
            queueCommand(dni, "setVolume", targetVol, columnName, newEpoch)
        }

        // Display Brightness & Adaptive Display Logic
        if (profile.hasDisplayBrightness) {
            if (targetAdaptBool) {
                if (forceDispatch || normState.actAdaptive != "on") {
                    String adaptiveCmd = dev.hasCommand("setAdaptiveBrightnessOn") ? "setAdaptiveBrightnessOn" : (dev.hasCommand("setAdaptiveDisplayOn") ? "setAdaptiveDisplayOn" : "setAdaptiveBrightnessOn")
                    queueCommand(dni, adaptiveCmd, null, columnName, newEpoch)
                }
            } else {
                if (profile.hasAdaptiveBrightness && (forceDispatch || normState.actAdaptive == "on")) {
                    String adaptiveOffCmd = dev.hasCommand("setAdaptiveBrightnessOff") ? "setAdaptiveBrightnessOff" : (dev.hasCommand("setAdaptiveDisplayOff") ? "setAdaptiveDisplayOff" : "setAdaptiveBrightnessOff")
                    queueCommand(dni, adaptiveOffCmd, null, columnName, newEpoch)
                }
                
                if (forceDispatch || normState.actDisp != dMap.displayBrightness) {
                    queueCommand(dni, "setDisplayBrightness", dMap.displayBrightness, columnName, newEpoch)
                }
            }
        }

        // Display Power Command Queue
        if (profile.hasDisplayPower) {
            Boolean isPwrOn = (normState.actDispPwr == "on")
            if (forceDispatch || isPwrOn != targetDispPwr) {
                String cmd = targetDispPwr ? (dev.hasCommand("setDisplayPowerOn") ? "setDisplayPowerOn" : "setDisplayPower") : (dev.hasCommand("setDisplayPowerOff") ? "setDisplayPowerOff" : "setDisplayPower")
                Object param = (cmd == "setDisplayPower") ? (targetDispPwr ? "on" : "off") : null
                queueCommand(dni, cmd, param, columnName, newEpoch)
            }
        }
        
        // Mute Command Queue (Strict Boolean Comparison Identical to DND)
        Boolean isCurrentlyMuted = (normState.actMute == "muted")
        if (forceDispatch || isCurrentlyMuted != targetMuteBool) {
            String muteCmd = targetMuteBool ? "mute" : "unmute"
            queueCommand(dni, muteCmd, null, columnName, newEpoch)
        }
        
        // DND Command Queue
        Boolean isDndActive = (normState.actDnd == "enabled")
        if (forceDispatch || isDndActive != targetDndBool) {
            String cmd = targetDndBool ? "setDoNotDisturbOn" : "setDoNotDisturbOff"
            queueCommand(dni, cmd, null, columnName, newEpoch)
        }
    }

    atomicState.desiredState = currentDesired
    processQueue()
    logInfo "Finished evaluating ${sectionTitle} Matrix for Mode [${columnName}]."
}

private void queueCommand(String dni, String command, Object param, String reason, long epoch) {
    if (getSettingBool("dryRunMode", false)) {
        logInfo "[DRY RUN ACTIVE] Command calculated & suppressed -> Device DNI: ${dni} | Cmd: ${command} | Arg: ${param} | Reason: ${reason}"
        return
    }

    List queue = atomicState.commandQueue ?: []
    queue.removeAll { it.dni == dni && it.command == command }

    queue.add([
        dni: dni, 
        command: command, 
        param: param, 
        reason: reason,
        epoch: epoch,
        queuedAt: now()
    ])
    atomicState.commandQueue = queue
    logDebug "Queued command [${command}] for device DNI [${dni}]. Epoch: ${epoch} | Queue Depth: ${queue.size()}"
}

void processQueue() {
    List queue = atomicState.commandQueue ?: []
    if (!queue || queue.size() == 0) {
        logDebug "Command queue empty. Reconciliation complete."
        return
    }

    Map item = queue.remove(0)
    atomicState.commandQueue = queue

    long activeEpoch = (atomicState.reconciliationEpoch ?: 0L) as long
    long itemEpoch = (item.epoch ?: 0L) as long

    if (itemEpoch != activeEpoch) {
        logWarn "Discarding obsolete command [${item.command}] for device DNI [${item.dni}]. Item Epoch: ${itemEpoch} | Active Epoch: ${activeEpoch}"
        if (atomicState.commandQueue.size() > 0) {
            processQueue()
        }
        return
    }

    def dev = parent ? parent.getManagedChildDevice(item.dni) : null

    if (dev) {
        try {
            logInfo "Executing Queued Command -> Device: ${dev.displayName} | Cmd: ${item.command}(${item.param != null ? item.param : ''}) | Source: ${item.reason}"
            if (item.param != null) {
                dev."${item.command}"(item.param)
            } else {
                dev."${item.command}"()
            }
        } catch (Exception e) {
            logError "Failed to execute command ${item.command} on device ${dev.displayName}: ${e.message}"
        }
    } else {
        logWarn "Target device DNI ${item.dni} no longer found in registry."
    }

    if (atomicState.commandQueue.size() > 0) {
        Integer delay = settings["queueDelayMs"] ? settings["queueDelayMs"].toInteger() : 350
        runInMillis(delay, "processQueue", [overwrite: true])
    }
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

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
    if (val == null || val.toString() == "no_change") return defaultVal
    if (val instanceof Boolean) return val
    return val.toString().toBoolean()
}