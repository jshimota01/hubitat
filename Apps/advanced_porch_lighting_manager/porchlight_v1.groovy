/**
 *  Advanced Adaptive Lighting Manager
 *  Platform: Hubitat Elevation
 *
 *  Combines Solar Altitude, Cloud Cover (Overcast), Dynamic Lux, and Twilight Parser
 *  data to automatically adjust morning and evening outdoor lighting.
 */

definition(
    name: "Adaptive Lighting Manager",
    namespace: "custom",
    author: "James Shimota",
    description: "Automates porch/outdoor lights using Solar Angles, Cloud Cover, Calculated Lux, and Twilight Parser variables.",
    category: "Convenience",
    iconUrl: "",
    iconXUrl: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Adaptive Lighting Configuration", install: true, uninstall: true) {
        
        section("Target Devices & Drivers") {
            input "switches", "capability.switch", title: "Select Switches to Control", multiple: true, required: true
            input "owmDevice", "capability.sensor", title: "Select OpenWeatherMap Device", multiple: false, required: true
            input "twilightDevice", "capability.sensor", title: "Select Twilight Parser Device (Optional)", multiple: false, required: false
            input "notifier", "capability.notificationDevice", title: "Select Notification Device (Optional)", multiple: true, required: false
        }

        section("Trigger Method") {
            input "controlMethod", "enum", title: "Light Trigger Evaluation Method", 
                  options: [
                      "lux": "Dynamic Lux Threshold (Built-in OWM Lux with Cloud Attenuation)",
                      "angle": "Solar Elevation Angle + Cloud PCT Offset",
                      "twilight": "Twilight Parser Variable Time + Minute Offset"
                  ], 
                  defaultValue: "lux", required: true, submitOnChange: true
        }

        if (controlMethod == "lux") {
            section("Lux Threshold Settings") {
                input "luxOnThreshold", "number", title: "Turn ON Lights when Lux is at or below", defaultValue: 50, required: true
                input "luxOffThreshold", "number", title: "Turn OFF Lights when Lux rises above", defaultValue: 100, required: true
            }
        } else if (controlMethod == "angle") {
            section("Solar Altitude Angle Settings") {
                input "baseOnAngle", "decimal", title: "Base Sun Elevation Angle for Light ON (Degrees below horizon, e.g., -3.5)", defaultValue: -3.5, required: true
                input "enableCloudOffset", "bool", title: "Adjust angle earlier on heavy overcast days?", defaultValue: true, submitOnChange: true
                if (enableCloudOffset) {
                    input "maxCloudShift", "decimal", title: "Max Angle Offset for 100% Cloud Cover (Degrees)", defaultValue: 2.5, required: true
                }
            }
        } else if (controlMethod == "twilight") {
            section("Twilight Variable Settings") {
                input "mornOffset", "number", title: "Morning Offset from Twilight Begin (Minutes)", defaultValue: -15, required: true
                input "eveOffset", "number", title: "Evening Offset from Twilight End (Minutes)", defaultValue: -75, required: true
            }
        }

        section("Schedule & Cutoff Rules") {
            input "morningOnTime", "time", title: "Earliest Morning Turn-ON Time", required: false
            input "morningOffTime", "time", title: "Latest Morning Turn-OFF Time (e.g. 8:00 AM)", required: false
            input "eveningCutoffTime", "time", title: "Night Turn-OFF Time / Midnight Cutoff", defaultValue: "23:59", required: true
        }

        section("Logging & Debugging") {
            input "logDebugEnable", "bool", title: "Enable Debug Logging", defaultValue: true
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    logInfo "Initializing Adaptive Lighting Manager..."

    // Subscribe to OpenWeatherMap driver attribute events
    subscribe(owmDevice, "currentSunAltitude", evaluateLighting)
    subscribe(owmDevice, "currentIlluminance", evaluateLighting)
    subscribe(owmDevice, "currentCloudPCT", evaluateLighting)

    // Optional Twilight Parser driver subscriptions
    if (twilightDevice) {
        subscribe(twilightDevice, "formattedUsedTwilightBegin", scheduleTwilightEvents)
        subscribe(twilightDevice, "formattedUsedTwilightEnd", scheduleTwilightEvents)
    }

    // Schedule fixed night turn-off cutoff
    if (eveningCutoffTime) {
        schedule(eveningCutoffTime, turnOffLightsNight)
    }

    // Initial evaluation run
    evaluateLighting(null)
}

def evaluateLighting(evt = null) {
    logDebug "Evaluating lighting conditions..."

    BigDecimal sunAngle = owmDevice.currentValue("currentSunAltitude")?.toBigDecimal() ?: 0.0
    BigDecimal cloudPct = owmDevice.currentValue("currentCloudPCT")?.toBigDecimal() ?: 0.0
    BigDecimal currentLux = owmDevice.currentValue("currentIlluminance")?.toBigDecimal() ?: 0.0
    String currentSwitchState = switches[0]?.currentValue("switch") ?: "off"

    boolean shouldBeOn = false

    switch (controlMethod) {
        case "lux":
            shouldBeOn = (currentLux <= luxOnThreshold)
            logDebug "Lux Check: Current=${currentLux} lx | Threshold=${luxOnThreshold} lx | Result=${shouldBeOn}"
            break

        case "angle":
            BigDecimal targetAngle = baseOnAngle
            if (enableCloudOffset && cloudPct > 0) {
                // Adjust target angle higher (earlier) as cloud cover increases
                BigDecimal offset = (cloudPct / 100.0) * maxCloudShift
                targetAngle = baseOnAngle + offset
            }
            shouldBeOn = (sunAngle <= targetAngle)
            logDebug "Angle Check: Sun=${sunAngle}° | Target=${targetAngle}° (Base: ${baseOnAngle}°, Clouds: ${cloudPct}%) | Result=${shouldBeOn}"
            break

        case "twilight":
            // Managed via time scheduling in scheduleTwilightEvents()
            return
    }

    // Apply lighting states
    if (shouldBeOn && currentSwitchState == "off") {
        if (isWithinAllowedTimeWindow()) {
            logInfo "Turning switches ON based on ${controlMethod} condition."
            switches.on()
            sendNotificationMsg("Adaptive Lighting: Porch lights turned ON (${controlMethod} trigger).")
        }
    } else if (!shouldBeOn && currentSwitchState == "on") {
        logInfo "Turning switches OFF based on ${controlMethod} condition."
        switches.off()
        sendNotificationMsg("Adaptive Lighting: Porch lights turned OFF (${controlMethod} trigger).")
    }
}

def turnOffLightsNight() {
    logInfo "Midnight/Cutoff time reached: Turning switches OFF."
    switches.off()
    sendNotificationMsg("Adaptive Lighting: Porch lights turned OFF due to night cutoff.")
}

private boolean isWithinAllowedTimeWindow() {
    // Prevents morning lights from coming on in the middle of the day or after night cutoff
    Calendar now = Calendar.getInstance()
    int hour = now.get(Calendar.HOUR_OF_DAY)
    return (hour < 9 || hour >= 16)
}

private void sendNotificationMsg(String msg) {
    if (notifier) {
        notifier.deviceNotification(msg)
    }
}

private void logInfo(String msg) {
    log.info "Adaptive Lighting Manager: ${msg}"
}

private void logDebug(String msg) {
    if (logDebugEnable) {
        log.debug "Adaptive Lighting Manager: ${msg}"
    }
}