/**
 *  AvgTHIE Child (v1.1.5)
 *  Optimized for straight mathematical averaging with throttled/periodic execution options.
 */
public static String version()   {  return "v1.1.5"  }

definition (
    name: "AvgTHIEChild",
    namespace: "jshimota",
    author: "J Shimota",
    description: "Child: Clean mathematical average of humidity, temperature, illuminance, energy or power sensors.",
    parent: "jshimota:AvgTHIE",
    category: "Averaging",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
    page(name: "mainPage")
}

def installed() {
    log.info "Installed: ${app.label}"
    initialize()
}

def updated() {
    log.info "Updated: ${app.label}"
    unsubscribe()
    unschedule() 
    if (debugOutput) runIn(1800, "logsOff")
    initialize()
}

def initialize() {
    // 1. Subscribe to real-time device changes
    if (tempSensors)   subscribe(tempSensors, "temperature", tempHandler)
    if (humidSensors)  subscribe(humidSensors, "humidity", humidHandler)
    if (illumSensors)  subscribe(illumSensors, "illuminance", illumHandler)
    if (energySensors) subscribe(energySensors, "energy", energyHandler)
    if (powerSensors)  subscribe(powerSensors, "power", powerHandler)

    // 2. Schedule a periodic health-check/refresh every 5 minutes
    runEvery5Minutes("periodicRefresh")
    
    // Run an initial calculation right now
    periodicRefresh()
}

// --- HANDLERS ---

def tempHandler(evt) {
    if (debugOutput) log.debug "Temperature update received from ${evt.device}"
    calculateTemp()
}

def humidHandler(evt) {
    if (debugOutput) log.debug "Humidity update received from ${evt.device}"
    calculateHumid()
}

def illumHandler(evt) {
    if (debugOutput) log.debug "Illuminance update received from ${evt.device}"
    calculateIllum()
}

def energyHandler(evt) {
    if (debugOutput) log.debug "Energy update received from ${evt.device}"
    calculateEnergy()
}

def powerHandler(evt) {
    if (debugOutput) log.debug "Power update received from ${evt.device}"
    calculatePower()
}

def periodicRefresh() {
    if (debugOutput) log.debug "Executing scheduled 5-minute periodic averaging refresh."
    calculateTemp()
    calculateHumid()
    calculateIllum()
    calculateEnergy()
    calculatePower()
}

// --- CALCULATION ENGINES ---

def calculateTemp() {
    if (!vTDevice || !tempSensors) return
    float total = 0.0
    int count = 0
    tempSensors.each { dev ->
        def val = dev.currentValue("temperature")
        if (val != null) { total += val.toFloat(); count++ }
    }
    if (count > 0) {
        float avg = total / count
        vTDevice.setTemperature(avg.round(1))
    }
}

def calculateHumid() {
    if (!vHDevice || !humidSensors) return
    float total = 0.0
    int count = 0
    humidSensors.each { dev ->
        def val = dev.currentValue("humidity")
        if (val != null) { total += val.toFloat(); count++ }
    }
    if (count > 0) {
        float avg = total / count
        vHDevice.setRelativeHumidity(avg.round(1))
    }
}

def calculateIllum() {
    if (!vIDevice || !illumSensors) return
    float total = 0.0
    int count = 0
    illumSensors.each { dev ->
        def val = dev.currentValue("illuminance")
        if (val != null) { total += val.toFloat(); count++ }
    }
    if (count > 0) {
        float avg = total / count
        vIDevice.setIlluminance(avg.round(0))
    }
}

def calculateEnergy() {
    if (!vEDevice || !energySensors) return
    float total = 0.0
    int count = 0
    energySensors.each { dev ->
        def val = dev.currentValue("energy")
        if (val != null) { total += val.toFloat(); count++ }
    }
    if (count > 0) {
        float avg = total / count
        vEDevice.setEnergy(avg.round(3))
    }
}

def calculatePower() {
    if (!vPDevice || !powerSensors) return
    float total = 0.0
    int count = 0
    powerSensors.each { dev ->
        def val = dev.currentValue("power")
        if (val != null) { total += val.toFloat(); count++ }
    }
    if (count > 0) {
        float avg = total / count
        vPDevice.setPower(avg.round(2))
    }
}

// --- UI PAGES ---

def mainPage() {
    if (!app.label) app.updateLabel(app.name)
    
    dynamicPage(name: "mainPage", uninstall: true, install: true) {
        section(getFormat("title", " ${app.label}")) {
            paragraph "<div style='color:#1A77C9'>Calculate a rolling average for your specialized virtual devices.</div>"
        }
        section("Temperature Config") {
            input "vTDevice", "capability.temperatureMeasurement", title: "Virtual Temperature Device", required: false
            input "tempSensors", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: false
        }
        section("Humidity Config") {
            input "vHDevice", "capability.relativeHumidityMeasurement", title: "Virtual Humidity Device", required: false
            input "humidSensors", "capability.relativeHumidityMeasurement", title: "Humidity Sensors", multiple: true, required: false
        }
        section("Illuminance Config") {
            input "vIDevice", "capability.illuminanceMeasurement", title: "Virtual Illuminance Device", required: false
            input "illumSensors", "capability.illuminanceMeasurement", title: "Illuminance Sensors", multiple: true, required: false
        }
        section("Energy Config") {
            input "vEDevice", "capability.energyMeter", title: "Virtual Energy Device", required: false
            input "energySensors", "capability.energyMeter", title: "Energy Sensors", multiple: true, required: false
        }
        section("Power Config") {
            input "vPDevice", "capability.powerMeter", title: "Virtual Power Device", required: false
            input "powerSensors", "capability.powerMeter", title: "Power Sensors", multiple: true, required: false
        }
        section("Logging Settings") {
            label title: "Child App Name", required: false
            input "debugOutput", "bool", title: "Enable Debug Logging", defaultValue: true
        }
        section {
            display()
        }
    }
}

def display() {
    paragraph getFormat("line")
    paragraph "<div style='color:#1A77C9;text-align:center;font-size:10px'>Modded by J Shimota<br>Version: ${version()} | ${getThisCopyright()}</div>"
}

def getFormat(type, myText=""){
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;' />"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def getThisCopyright() { return "&copy; 2026 J Shimota" }

def logsOff() { 
    log.warn "${app.label} execution debug logging auto-disabled..."
    app.updateSetting("debugOutput",[value:false, type:"bool"]) 
}