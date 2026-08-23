/*
 *  Advanced vThermostat Parent App
 *  Copyright 2020 Nelson Clark / Customizations by jshimota
 */

definition(
    name: "Advanced vThermostat Manager Custom",
    namespace: "jshimota",
    author: "Nelson Clark",
    description: "Join any sensor(s) with any outlet(s) for virtual thermostat control.",
    category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-logo.png",
	importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/advanced_virtual_thermostat_custom/Advanced_vThermostat-Parent_Custom.groovy",
    singleInstance: true
)

preferences {
    page(name: "Install", title: "Advanced vThermostat Manager Custom", install: true, uninstall: true) {
        section("Devices") {}
        section {
            app(name: "thermostats", appName: "Advanced vThermostat Child Custom", namespace: "jshimota", title: "Add Advanced vThermostat", multiple: true)
        }
    }
}

def installed() {
    log.debug "Installed"
    initialize()
}

def updated() {
    log.debug "Updated"
    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "Initializing; there are ${childApps.size()} child apps installed"
    childApps.each { child -> 
        log.debug "  child app: ${child.label}"
    }
}