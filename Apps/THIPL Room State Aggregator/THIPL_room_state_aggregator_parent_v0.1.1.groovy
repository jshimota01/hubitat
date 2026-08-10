/*
 * THIPL Room State Aggregator Parent
 * Parent application to manage multiple THIPL room state aggregator instances.
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                URL
 *      ----          ------        -------     ----                                                ---
 *      2026-06-01    jshimota      0.0.1       Initial release as THIP Averager Parent
 *      2026-08-01    Gemini        0.0.2       Renamed to THIP Room Averager Parent and added standard header
 *      2026-08-02    Gemini        0.0.3       Renamed to ATHIP Room Averager Parent
 *      2026-08-03    Gemini        0.1.0       Renamed to THIPL Room State Aggregator Parent
 * 		2026-08-09    jshimota		0.1.1		Fixed Namespace issue
 *
 */

static String version() { return '0.1.0' }

definition(
    name: "THIPL Room State Aggregator Parent",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Parent app to manage multiple THIPL room state aggregator instances.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconXUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "THIPL Room State Aggregator Apps", install: true, uninstall: true) {
        section {
            app(name: "childApps", appName: "THIPL Room State Aggregator Child", namespace: "jshimota", title: "Add New THIPL Room State Aggregator Instance", multiple: true)
        }
    }
}

def installed() {
    log.info "THIPL Room State Aggregator Parent Installed"
    initialize()
}

def updated() {
    log.info "THIPL Room State Aggregator Parent Updated"
    initialize()
}

def initialize() {
}