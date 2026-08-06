definition(
    name: "THIE Averager Parent",
    namespace: "hubitat",
    author: "James Shimota",
    description: "Parent app to manage multiple THIE sensor averaging instances.",
    category: "Convenience",
    iconUrl: "",
    iconXUrl: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "THIE Sensor Averager Apps", install: true, uninstall: true) {
        section {
            app(name: "childApps", appName: "THIE Averager Child", namespace: "hubitat", title: "Add New THIE Averager Instance", multiple: true)
        }
    }
}

def installed() {
    log.info "THIE Averager Parent Installed"
    initialize()
}

def updated() {
    log.info "THIE Averager Parent Updated"
    initialize()
}

def initialize() {
    // Parent acts primarily as a wrapper for child instances
}