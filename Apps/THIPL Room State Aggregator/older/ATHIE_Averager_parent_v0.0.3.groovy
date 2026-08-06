/*
 * ATHIE Room Averager Parent
 * Parent application to manage multiple ATHIE room averaging instances.
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
 *      2026-08-02    Gemini        0.0.3       Renamed to ATHIE Room Averager Parent
 *
 */

static String version() { return '0.0.3' }

definition(
    name: "ATHIE Room Averager Parent",
    namespace: "hubitat",
    author: "James Shimota",
    description: "Parent app to manage multiple ATHIE room averaging instances.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "ATHIE Room Averager Apps", install: true, uninstall: true) {
        section {
            app(name: "childApps", appName: "ATHIE Room Averager Child", namespace: "hubitat", title: "Add New ATHIE Room Averager Instance", multiple: true)
        }
    }
}

def installed() {
    log.info "ATHIE Room Averager Parent Installed"
    initialize()
}

def updated() {
    log.info "ATHIE Room Averager Parent Updated"
    initialize()
}

def initialize() {
    // Parent acts primarily as a wrapper for child instances
}