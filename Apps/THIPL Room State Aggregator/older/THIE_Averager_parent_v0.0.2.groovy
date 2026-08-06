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
 *
 */

static String version() { return '0.0.2' }

definition(
    name: "THIE Room Averager Parent",
    namespace: "hubitat",
    author: "James Shimota",
    description: "Parent app to manage multiple THIE room averaging instances.",
    category: "Convenience"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "THIE Room Averager Apps", install: true, uninstall: true) {
        section {
            app(name: "childApps", appName: "THIE Room Averager Child", namespace: "hubitat", title: "Add New THIE Room Averager Instance", multiple: true)
        }
    }
}

def installed() {
    log.info "THIE Room Averager Parent Installed"
    initialize()
}

def updated() {
    log.info "THIE Room Averager Parent Updated"
    initialize()
}

def initialize() {
    // Parent acts primarily as a wrapper for child instances
}