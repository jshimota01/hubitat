/*
 * THIE Room Averager Parent
 * Parent application to manage multiple THIE room averaging instances.
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2026-06-01    jshimota      0.0.1       Initial release as THIE Averager Parent
 *      2026-08-01    Gemini        0.0.2       Renamed to THIE Room Averager Parent and added standard header
 *      2026-08-01    Gemini        0.0.3       Implemented standardized dynamic logging framework
 *      2026-08-01    Gemini        0.0.4       Fixed iconUrl definition compilation issue
 *
 */

static String version() { return '0.0.4' }

definition(
    name: "THIE Room Averager Parent",
    namespace: "hubitat",
    author: "James Shimota",
    description: "Parent app to manage multiple THIE room averaging instances.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "THIE Room Averager Apps", install: true, uninstall: true) {
        section {
            app(name: "childApps", appName: "THIE Room Averager Child", namespace: "hubitat", title: "Add New THIE Room Averager Instance", multiple: true)
        }
        section("<b>Logging Options</b>") {
            input name: "logDebugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
            input name: "logInfoEnable", type: "bool", title: "Enable Info Logging", defaultValue: true
            input name: "logWarnEnable", type: "bool", title: "Enable Warning Logging", defaultValue: true
            input name: "logErrorEnable", type: "bool", title: "Enable Error Logging", defaultValue: true
            input name: "logTraceEnable", type: "bool", title: "Enable Trace Logging", defaultValue: false
        }
    }
}

// Custom Logging Helper Methods
void disableDebugLogging() {
    logInfo "30 minutes have elapsed. Automatically disabling debug logging."
    app.updateSetting("logDebugEnable", [type: "bool", value: false])
}

private void logMessage(String level, String msg) {
    if (settings["log${level.capitalize()}Enable"] == true) {
        log."${level}" "${app.label ?: 'THIE Room Averager Parent'}${level == 'warn' ? ' WARNING' : level == 'error' ? ' ERROR' : ''}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

def installed() {
    logInfo "THIE Room Averager Parent Installed"
    initialize()
}

def updated() {
    logInfo "THIE Room Averager Parent Updated"
    initialize()
}

def initialize() {
    if (settings.logDebugEnable != false) runIn(1800, disableDebugLogging)
}