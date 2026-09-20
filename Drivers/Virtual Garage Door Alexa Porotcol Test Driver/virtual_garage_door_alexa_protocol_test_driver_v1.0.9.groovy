/**
 * Virtual Garage Door Alexa Protocol Test Driver (Diagnostic Tool)
 * Platform: Hubitat Elevation
 * Notes: Dedicated protocol test harness to isolate and simulate Alexa Garage Door voice skill state responses
 * Capabilities: GarageDoorControl, Actuator, Configuration, Refresh
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
 *  Provides a highly configurable, non-physical garage door state simulation environment
 *  to test Alexa voice skill completion responses, timeouts, intermediate transitions, and duplicate event sensitivities.
 *  
 *  Changelog:
 *  v1.0.9Test    08/30/26    jshimota    Clean refactor: consolidated queue engine, refined comments, optimized trace logging structures
 *  v1.0.8Test    08/30/26    jshimota    Added comprehensive trace and debug logging across queue scheduler, millisecond delays, and inter-event spacing engine
 *  v1.0.7Test    08/30/26    jshimota    Added master toggle for inter-event spacing; updated preference labels/descriptions to explicitly note milliseconds
 *  v1.0.6Test    08/30/26    jshimota    Upgraded state engine to runInMillis timing; added configurable minimum inter-event spacing (default 250ms)
 *  v1.0.5Test    08/29/26    jshimota    Added explicit Case 1 vs Case 2 command lifecycle instrumentation logging
 *  v1.0.4Test    08/29/26    jshimota    Replaced System.currentTimeMillis() with sandbox-compliant new Date().getTime()
 *  v1.0.3Test    08/29/26    jshimota    Added ENTRY/ACTION/EXIT command instrumentation and calculated door event delta milliseconds relative to COMMAND EXIT
 *  v1.0.2Test    08/29/26    jshimota    Added strict door state validation, added lastDoorEventDescription attribute, fixed dup-event step timing offsets
 *  v1.0.1Test    08/29/26    jshimota    Renamed driver definition to Virtual Garage Door Alexa Protocol Test Driver
 *  v1.0.0Test    08/29/26    jshimota    Initial release of the Alexa Garage Door Protocol Test Harness Driver
 **/

static String version() { return '1.0.9Test' }
def timeStamp() { return "2026/08/30 03:30 PM" }

metadata {
    definition (
        name: "Virtual Garage Door Alexa Protocol Test Driver", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl: ""
    ) {
        capability "GarageDoorControl"
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"

        // Capability Standard Attributes
        // "door" (open, closed, opening, closing, unknown)

        // Diagnostic-Only Attributes (Hidden from Alexa Interface)
        attribute "driverVersion", "string"
        attribute "lastCommand", "string"
        attribute "lastCommandTime", "string"
        attribute "lastCommandPhase", "string"
        attribute "lastCommandEntryTime", "string"
        attribute "lastCommandExitTime", "string"
        attribute "commandCount", "number"
        attribute "lastDoorEvent", "string"
        attribute "lastDoorEventTime", "string"
        attribute "lastDoorEventDescription", "string"
        attribute "lastEventCommandExitDeltaMs", "number"
        attribute "doorEventCount", "number"
        attribute "testSequence", "string"
        attribute "testStatus", "string"

        // Manual Event Override Commands
        command "setDoorOpen"
        command "setDoorClosed"
        command "setDoorOpening"
        command "setDoorClosing"
        command "setDoorUnknown"

        // Sequence Execution & Diagnostic Management Commands
        command "runTestSequence", [[name: "Custom CSV Sequence*", type: "STRING", description: "Format in milliseconds: e.g., closing,0,closing,250,closed or opening,12000,open"]]
        command "applyTestPreset", [[name: "Select Test Preset*", type: "ENUM", constraints: [
            "Preset 1: Final Immediately",
            "Preset 2: Normal Transition (10s)",
            "Preset 3: Transition Only (No Final)",
            "Preset 4: Final Only After Delay (10s)",
            "Preset 5: Duplicate Transition",
            "Preset 6: Duplicate Final",
            "Preset 7: Long Completion Delay (45s)"
        ]]]
        command "clearDiagnostics"
        command "resetDriver"
    }

    preferences {
        // Section A: State Sequence Engine Controls
        input name: "prefCommandSequence", type: "enum", title: "<b>Command State Sequence Engine</b>", 
            options: ["FINAL ONLY", "TRANSITION -> FINAL", "TRANSITION ONLY", "NO RESPONSE", "CUSTOM CSV"], defaultValue: "TRANSITION -> FINAL", required: true

        input name: "prefFinalDelay", type: "number", title: "<b>Final State Delay (seconds)</b>", range: "0..120", defaultValue: 10, required: true

        // Section B: Inter-Event Spacing Controls (Millisecond Timing)
        input name: "prefEnableInterEventSpacing", type: "bool", title: "<b>Enable Minimum Inter-Event Spacing</b>", description: "Turn On to enforce minimum millisecond delays between events. Turn Off for raw zero-gap testing.", defaultValue: true, required: true
        input name: "prefMinInterEventSpacingMs", type: "number", title: "<b>Minimum Inter-Event Spacing Duration (milliseconds)</b>", description: "Spacing value in milliseconds (ms) applied when inter-event spacing is enabled above.", range: "0..10000", defaultValue: 250, required: true

        // Section C: Intermediate & Final Duplicate Event Injection
        input name: "prefDupIntermediate", type: "bool", title: "<b>Inject Duplicate Intermediate Event?</b>", defaultValue: false, required: true
        input name: "prefDupFinal", type: "bool", title: "<b>Inject Duplicate Final Event?</b>", defaultValue: false, required: true
        input name: "prefDupDelay", type: "number", title: "<b>Duplicate Event Stagger Delay (seconds)</b>", range: "0..30", defaultValue: 2, required: true

        // Section D: Protocol Event Flag Override
        input name: "prefIsStateChange", type: "bool", title: "<b>Event State Change Flag (isStateChange)</b>", defaultValue: true, required: true

        // Section E: Custom CSV Sequence String
        input name: "prefCustomCsvSequence", type: "text", title: "<b>Custom CSV Test Sequence String</b>", description: "Format using millisecond (ms) delays: state,delayMs,state,delayMs... (e.g. closing,250,closing,10000,closed)", defaultValue: "closing,10000,closed", required: false

        // Section F: Independent Logging Switches
        input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
        input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
        input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
    }
}

// -----------------------------------------------------------------------------
// Helper Routines: Time & State Validation
// -----------------------------------------------------------------------------
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== TEST DRIVER VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

private String getFormattedTime(Long epochMs = null) {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    Date targetDate = (epochMs != null) ? new Date(epochMs) : new Date()
    return targetDate.format("yyyy-MM-dd HH:mm:ss.SSS", tz)
}

private Boolean isValidGarageState(String st) {
    return (st in ["open", "closed", "opening", "closing", "unknown"])
}

void parse(String description) {
    logDebug "parse(): ${description}"
}

// -----------------------------------------------------------------------------
// Capability Commands: open() & close() with Lifecycle Instrumentation
// -----------------------------------------------------------------------------
void open()  { executeCommandWithInstrumentation("open") }
void close() { executeCommandWithInstrumentation("close") }

private void executeCommandWithInstrumentation(String cmd) {
    Long entryEpoch = new Date().getTime()
    String entryTimeStr = getFormattedTime(entryEpoch)
    state.lastCommandExitEpoch = null
    state.lastCommandExitTimeStr = null

    sendEvent(name: "lastCommandPhase", value: "ENTRY", isStateChange: true)
    sendEvent(name: "lastCommandEntryTime", value: entryTimeStr, isStateChange: true)
    logInfo "COMMAND ENTRY: ${cmd}() @ ${entryTimeStr} [Epoch: ${entryEpoch} ms]"

    sendEvent(name: "lastCommandPhase", value: "ACTION", isStateChange: true)
    
    handleIncomingCommand(cmd)

    Long exitEpoch = new Date().getTime()
    String exitTimeStr = getFormattedTime(exitEpoch)
    
    // Cache command exit time for relative event delta timing calculations
    state.lastCommandExitEpoch = exitEpoch
    state.lastCommandExitTimeStr = exitTimeStr

    sendEvent(name: "lastCommandExitTime", value: exitTimeStr, isStateChange: true)
    sendEvent(name: "lastCommandPhase", value: "EXIT", isStateChange: true)
    logInfo "COMMAND EXIT:  ${cmd}() @ ${exitTimeStr} [Epoch: ${exitEpoch} ms | Duration: ${exitEpoch - entryEpoch} ms]"
}

// Centralized Command Dispatcher
private void handleIncomingCommand(String cmd) {
    String nowStr = getFormattedTime()
    Integer cCount = (device.currentValue("commandCount") ?: 0) + 1

    sendEvent(name: "lastCommand", value: cmd, isStateChange: true)
    sendEvent(name: "lastCommandTime", value: nowStr, isStateChange: true)
    sendEvent(name: "commandCount", value: cCount, isStateChange: true)

    // Cancel any active running test sequence callbacks
    unschedule("executeScheduledStepHandler")

    String seqMode = settings?.prefCommandSequence ?: "TRANSITION -> FINAL"
    sendEvent(name: "testSequence", value: "${cmd.toUpperCase()}: ${seqMode}", isStateChange: true)
    sendEvent(name: "testStatus", value: "Executing", isStateChange: true)

    if (seqMode == "NO RESPONSE") {
        logWarn "TEST: Command sequence mode set to 'NO RESPONSE'. Ignoring command."
        sendEvent(name: "testStatus", value: "Idle (No Response)", isStateChange: true)
        return
    }

    if (seqMode == "CUSTOM CSV") {
        String customStr = settings?.prefCustomCsvSequence
        if (customStr) {
            runTestSequence(customStr)
        } else {
            logError "TEST: CUSTOM CSV selected but preference is empty!"
            sendEvent(name: "testStatus", value: "Error (Empty CSV)", isStateChange: true)
        }
        return
    }

    // Construct state queue based on preferences
    List queue = []
    String intermediateState = (cmd == "open") ? "opening" : "closing"
    String finalState = (cmd == "open") ? "open" : "closed"
    
    Long finalDelayMs = ((settings?.prefFinalDelay != null) ? (settings.prefFinalDelay as Long) : 10L) * 1000L
    Long dupDelayMs = ((settings?.prefDupDelay != null) ? (settings.prefDupDelay as Long) : 2L) * 1000L

    Boolean dupInter = settings?.prefDupIntermediate ?: false
    Boolean dupFin = settings?.prefDupFinal ?: false

    switch (seqMode) {
        case "FINAL ONLY":
            queue.add([state: finalState, delayMs: finalDelayMs, source: "${cmd}() ${finalDelayMs == 0L ? 'immediate' : 'delayed'} final"])
            if (dupFin) queue.add([state: finalState, delayMs: dupDelayMs, source: "${cmd}() dup final"])
            break

        case "TRANSITION -> FINAL":
            queue.add([state: intermediateState, delayMs: 0L, source: "${cmd}() initial transition"])
            if (dupInter) queue.add([state: intermediateState, delayMs: dupDelayMs, source: "${cmd}() dup transition"])

            Long remainingDelayMs = Math.max(0L, finalDelayMs - (dupInter ? dupDelayMs : 0L))
            queue.add([state: finalState, delayMs: remainingDelayMs, source: "${cmd}() final completion"])
            if (dupFin) queue.add([state: finalState, delayMs: dupDelayMs, source: "${cmd}() dup final"])
            break

        case "TRANSITION ONLY":
            queue.add([state: intermediateState, delayMs: 0L, source: "${cmd}() transition only"])
            if (dupInter) queue.add([state: intermediateState, delayMs: dupDelayMs, source: "${cmd}() dup transition"])
            break
    }

    processEventQueue(queue)
}

// -----------------------------------------------------------------------------
// Queue Engine & Scheduler
// -----------------------------------------------------------------------------
private void processEventQueue(List queue) {
    if (!queue || queue.isEmpty()) return

    logTrace "TEST: Initializing event queue with ${queue.size()} step(s)."
    state.testQueue = queue
    state.lastEventEmitEpoch = null
    executeNextQueueStep()
}

void executeNextQueueStep() {
    List queue = state.testQueue
    if (!queue || queue.isEmpty()) {
        logDebug "TEST: Event queue empty. Transitioning testStatus to Completed."
        sendEvent(name: "testStatus", value: "Completed", isStateChange: true)
        return
    }

    Map step = queue.remove(0)
    state.testQueue = queue

    Long requestedDelayMs = (step.delayMs != null) ? (step.delayMs as Long) : 0L
    Boolean spacingEnabled = (settings?.prefEnableInterEventSpacing != null) ? (settings.prefEnableInterEventSpacing as Boolean) : true
    Long minSpacingMs = spacingEnabled ? ((settings?.prefMinInterEventSpacingMs != null) ? (settings.prefMinInterEventSpacingMs as Long) : 250L) : 0L

    Long effectiveDelayMs = requestedDelayMs

    logTrace "TEST STEP EVALUATION -> Target: '${step.state}' | Source: '${step.source}' | Requested Delay: ${requestedDelayMs} ms | Spacing Enabled: ${spacingEnabled} | Min Spacing Setting: ${minSpacingMs} ms"

    // Enforce minimum inter-event spacing if enabled and previous event epoch exists
    if (spacingEnabled && minSpacingMs > 0L && state.lastEventEmitEpoch != null) {
        Long nowEpoch = new Date().getTime()
        Long timeSinceLastEvent = nowEpoch - (state.lastEventEmitEpoch as Long)
        Long requiredSpacingDelay = minSpacingMs - timeSinceLastEvent

        logTrace "TEST SPACING ENGINE -> Now: ${nowEpoch} ms | Last Event: ${state.lastEventEmitEpoch} ms | Time Elapsed: ${timeSinceLastEvent} ms | Required Gap Delay: ${requiredSpacingDelay} ms"

        if (requiredSpacingDelay > effectiveDelayMs) {
            logDebug "TEST: Enforcing inter-event spacing (${minSpacingMs} ms gap). Delay increased from ${effectiveDelayMs} ms to ${requiredSpacingDelay} ms."
            effectiveDelayMs = requiredSpacingDelay
        }
    } else if (!spacingEnabled) {
        logTrace "TEST: Inter-event spacing is OFF. Running with unthrottled zero-gap timing."
    }

    if (effectiveDelayMs <= 0L) {
        logTrace "TEST: Dispatching state '${step.state}' immediately (0 ms effective delay)."
        emitTestDoorEvent(step.state as String, step.source as String)
        executeNextQueueStep()
    } else {
        logDebug "TEST: Scheduling state '${step.state}' in ${effectiveDelayMs} ms via runInMillis (Source: ${step.source})"
        runInMillis(effectiveDelayMs, "executeScheduledStepHandler", [data: step])
    }
}

void executeScheduledStepHandler(Map step) {
    logTrace "TEST: Scheduled timer fired for step '${step.state}' (Source: ${step.source})"
    emitTestDoorEvent(step.state as String, step.source as String)
    executeNextQueueStep()
}

// -----------------------------------------------------------------------------
// Event Emission & Logging Transcript Engine
// -----------------------------------------------------------------------------
private void emitTestDoorEvent(String targetState, String sourceStr) {
    Long eventEpoch = new Date().getTime()
    state.lastEventEmitEpoch = eventEpoch
    String eventTimeStr = getFormattedTime(eventEpoch)
    String cleanTarget = targetState?.trim()?.toLowerCase()

    // Strict Garage Capability State Guard Validation
    if (!isValidGarageState(cleanTarget)) {
        String errLog = "TEST ERROR: Invalid state '${targetState}' rejected! Allowed states are [open, closed, opening, closing, unknown]. Aborting sequence."
        logError errLog
        sendEvent(name: "testStatus", value: "Aborted (Invalid State '${targetState}')", isStateChange: true)
        state.testQueue = []
        return
    }

    Long cmdExitEpoch = state.lastCommandExitEpoch as Long
    String cmdExitTimeStr = state.lastCommandExitTimeStr as String ?: "Pending (Command In Flight)"

    Long deltaMs = -1
    String timingRelation = ""

    if (cmdExitEpoch == null) {
        timingRelation = "[CASE 1: EMITTED INSIDE METHOD BEFORE COMMAND EXIT]"
        deltaMs = -1
    } else {
        deltaMs = eventEpoch - cmdExitEpoch
        timingRelation = "[CASE 2: EMITTED ASYNCHRONOUSLY AFTER COMMAND EXIT]"
    }

    String prevDoorState = device.currentValue("door") ?: "uninitialized"
    Boolean flagVal = (settings?.prefIsStateChange != false)
    Integer eCount = (device.currentValue("doorEventCount") ?: 0) + 1

    sendEvent(
        name: "door", 
        value: cleanTarget, 
        isStateChange: flagVal, 
        descriptionText: "Test Event: garage door set to ${cleanTarget} (Source: ${sourceStr})"
    )

    sendEvent(name: "lastDoorEvent", value: cleanTarget, isStateChange: true)
    sendEvent(name: "lastDoorEventTime", value: eventTimeStr, isStateChange: true)
    sendEvent(name: "lastDoorEventDescription", value: sourceStr, isStateChange: true)
    sendEvent(name: "lastEventCommandExitDeltaMs", value: deltaMs, isStateChange: true)
    sendEvent(name: "doorEventCount", value: eCount, isStateChange: true)

    logInfo """
TEST: DOOR EVENT TRANSCRIPT
  ------------------------------------------------------------------------
  Target State                   : ${cleanTarget}
  Previous State                 : ${prevDoorState}
  Event Trigger Source           : ${sourceStr}
  isStateChange                  : ${flagVal}
  ------------------------------------------------------------------------
  EVENT TIME                     : ${eventTimeStr}
  COMMAND EXIT TIME              : ${cmdExitTimeStr}
  MILLISECONDS AFTER COMMAND EXIT: ${deltaMs >= 0 ? deltaMs + " ms" : "N/A (Inside Method)"}
  EXECUTION TIMING RELATION      : ${timingRelation}
  ------------------------------------------------------------------------"""
}

// -----------------------------------------------------------------------------
// Custom CSV Sequence Parser Engine
// -----------------------------------------------------------------------------
void runTestSequence(String csvStr) {
    if (!csvStr) {
        logError "TEST: runTestSequence called with empty string!"
        return
    }

    logInfo "TEST: RUNNING CUSTOM CSV SEQUENCE -> '${csvStr}'"
    unschedule("executeScheduledStepHandler")

    String[] tokens = csvStr.split(",")
    List queue = []

    if (tokens.length % 2 != 0 && tokens.length != 1) {
        logWarn "TEST CSV WARNING: Token count (${tokens.length}) is odd. Expecting state,delayMs pairs."
    }

    for (int i = 0; i < tokens.length; i += 2) {
        String st = tokens[i].trim().toLowerCase()

        if (!isValidGarageState(st)) {
            logError "TEST CSV ERROR: Invalid state '${st}' found in token ${i+1}. Sequence aborted."
            sendEvent(name: "testStatus", value: "Aborted (CSV Invalid State '${st}')", isStateChange: true)
            return
        }

        Long delMs = 0L
        if (i + 1 < tokens.length) {
            try {
                delMs = tokens[i + 1].trim().toLong()
            } catch (Exception e) {
                logError "TEST CSV ERROR: Invalid delay token '${tokens[i+1]}', defaulting to 0 ms."
            }
        }
        queue.add([state: st, delayMs: delMs, source: "Custom CSV Step ${queue.size() + 1}"])
    }

    sendEvent(name: "testSequence", value: "CSV: ${csvStr}", isStateChange: true)
    sendEvent(name: "testStatus", value: "Executing CSV", isStateChange: true)
    processEventQueue(queue)
}

// -----------------------------------------------------------------------------
// Test Presets Applicator
// -----------------------------------------------------------------------------
void applyTestPreset(String presetName) {
    logInfo "TEST: APPLYING PRESET -> ${presetName}"
    unschedule("executeScheduledStepHandler")

    Map presets = [
        "Preset 1: Final Immediately":        [seq: "FINAL ONLY",          delay: 0,  dupInter: false, dupFin: false],
        "Preset 2: Normal Transition (10s)":   [seq: "TRANSITION -> FINAL", delay: 10, dupInter: false, dupFin: false],
        "Preset 3: Transition Only (No Final)":[seq: "TRANSITION ONLY",     delay: 0,  dupInter: false, dupFin: false],
        "Preset 4: Final Only After Delay (10s)":[seq: "FINAL ONLY",       delay: 10, dupInter: false, dupFin: false],
        "Preset 5: Duplicate Transition":      [seq: "TRANSITION -> FINAL", delay: 10, dupInter: true,  dupFin: false, dupDelay: 2],
        "Preset 6: Duplicate Final":           [seq: "TRANSITION -> FINAL", delay: 10, dupInter: false, dupFin: true,  dupDelay: 2],
        "Preset 7: Long Completion Delay (45s)":[seq: "TRANSITION -> FINAL", delay: 45, dupInter: false, dupFin: false]
    ]

    Map targetPreset = presets[presetName]
    if (targetPreset) {
        device.updateSetting("prefCommandSequence", [type: "enum", value: targetPreset.seq])
        device.updateSetting("prefFinalDelay", [type: "number", value: targetPreset.delay])
        device.updateSetting("prefDupIntermediate", [type: "bool", value: targetPreset.dupInter])
        device.updateSetting("prefDupFinal", [type: "bool", value: targetPreset.dupFin])
        if (targetPreset.dupDelay != null) {
            device.updateSetting("prefDupDelay", [type: "number", value: targetPreset.dupDelay])
        }
    }

    sendEvent(name: "testStatus", value: "Preset Applied: ${presetName}", isStateChange: true)
}

// -----------------------------------------------------------------------------
// Manual Event Controls & Diagnostics Management
// -----------------------------------------------------------------------------
void setDoorOpen()     { emitTestDoorEvent("open", "Manual GUI Button") }
void setDoorClosed()   { emitTestDoorEvent("closed", "Manual GUI Button") }
void setDoorOpening()  { emitTestDoorEvent("opening", "Manual GUI Button") }
void setDoorClosing()  { emitTestDoorEvent("closing", "Manual GUI Button") }
void setDoorUnknown()  { emitTestDoorEvent("unknown", "Manual GUI Button") }

void clearDiagnostics() {
    logInfo "TEST: Clearing all diagnostic attributes and state counters..."
    sendEvent(name: "lastCommand", value: "none", isStateChange: true)
    sendEvent(name: "lastCommandTime", value: "none", isStateChange: true)
    sendEvent(name: "lastCommandPhase", value: "none", isStateChange: true)
    sendEvent(name: "lastCommandEntryTime", value: "none", isStateChange: true)
    sendEvent(name: "lastCommandExitTime", value: "none", isStateChange: true)
    sendEvent(name: "commandCount", value: 0, isStateChange: true)
    sendEvent(name: "lastDoorEvent", value: "none", isStateChange: true)
    sendEvent(name: "lastDoorEventTime", value: "none", isStateChange: true)
    sendEvent(name: "lastDoorEventDescription", value: "none", isStateChange: true)
    sendEvent(name: "lastEventCommandExitDeltaMs", value: -1, isStateChange: true)
    sendEvent(name: "doorEventCount", value: 0, isStateChange: true)
    sendEvent(name: "testSequence", value: "none", isStateChange: true)
    sendEvent(name: "testStatus", value: "Cleared", isStateChange: true)
    
    state.testQueue = []
    state.lastCommandExitEpoch = null
    state.lastCommandExitTimeStr = null
    state.lastEventEmitEpoch = null
}

// -----------------------------------------------------------------------------
// Hubitat Platform Lifecycle Routines
// -----------------------------------------------------------------------------
void installed() {
    checkAndLogVersionDemarcation()
    sendEvent(name: "driverVersion", value: version())
    clearDiagnostics()
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    initialize(false)
}

def configure() {
    checkAndLogVersionDemarcation()
    sendEvent(name: "driverVersion", value: version())
    initialize(false)
    return []
}

def refresh() {
    checkAndLogVersionDemarcation()
    sendEvent(name: "driverVersion", value: version(), isStateChange: true)
    return []
}

private void initialize(Boolean isInstall = false) {
    unschedule("disableDebugLogging")
    
    if (device.currentValue("door") == null) {
        sendEvent(name: "door", value: "unknown")
    }

    if (isInstall) {
        device.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging enabled. Will automatically turn off in 30 minutes."
        runIn(1800, "disableDebugLogging")
    }
}

void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        device.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

void resetDriver() {
    logTrace "resetDriver() initiated"
    unschedule()
    state.clear()
    clearDiagnostics()
    logInfo "Test driver reset completed."
}

// Centralized Logging Engine
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String devName = device.displayName ?: "Test Driver"
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