/**
 * Garage Opener (custom) v1.1.0
 *
 * Copyright 2020 Mikhail Diatchenko (@muxa)
 * Modernized 2026 - JAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
 
 /*
 * v1.1.0  6-21-2026 	JAS - Gemini recommendations
 */

static String version()    {  return '1.1.0'  }


definition(
    name: "Garage Opener (Custom)",
    namespace: "jshimota",
    author: "Mikhail Diatchenko",
    description: "Control your garage door with a switch and optional contact sensors",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "mainPage", title: "Garage Opener", install: true, uninstall: true) {
        section("<h2>Controls</h2>") {
            input name: "garageControl", type: "capability.garageDoorControl", title: "Garage Door Control", description: "Use a Virtual Garage Door Control device", required: true
            input name: "garageSwitch", type: "capability.switch", title: "Garage Switch", description: "Physical switch that controls your garage door", required: true
        }
        
        section("<h2>Contacts</h2>") {
            input name: "closedContact", type: "capability.contactSensor", title: "Garage Fully Closed Contact", required: false
            input name: "openContact", type: "capability.contactSensor", title: "Garage Fully Open Contact", required: false
        }
        
        section("<h2>Options</h2>") {
            input name: "garageTime", type: "number", title: "Garage opening time (in seconds)", defaultValue: 15, required: true
            input name: "switchOffDelay", type: "long", title: "Switch off delay (in milliseconds)", defaultValue: 1000, required: true
            input name: "reversalDelay", type: "long", title: "Reversal delay (in milliseconds)", defaultValue: 1000, required: true
            input name: "enableLogging", type: "bool", title: "Enable Debug Logging?", defaultValue: false, required: true
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    logInfo "Updating with settings: ${settings}"
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    setupSubscriptions()
    
    state.openingBySwitch = false
    state.doorMoving = false
    state.lastDoorStatus = "unknown"
    state.lastDoorAction = "unknown"
    
    if (enableLogging) {
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, disableDebugLogging)
    }
    logInfo "Initialised"
}

def setupSubscriptions() {
    subscribe(garageControl, "door", garageControlHandler)
    subscribe(garageSwitch, "switch", garageSwitchHandler)
    if (closedContact) subscribe(closedContact, "contact", garageClosedContactHandler)    
    if (openContact) subscribe(openContact, "contact", garageOpenContactHandler)
}

def garageControlHandler(evt) {    
    logDebug "Garage door event: ${evt.value}"
    
    if (evt.value == 'opening' || evt.value == 'closing') {
        if (state.lastDoorStatus == 'opening' || state.lastDoorStatus == 'closing') {
            logInfo "Engage garage switch from controller to stop motion and reverse direction"
            stopAndReverseDoorDirection()
        } else {
            startTimeout()
        
            if (!state.openingBySwitch) {
                if (state.lastDoorStatus == 'unknown') {
                    if (evt.value != state.lastDoorAction) {
                        logInfo "Engage garage switch from controller"
                        garageSwitch.on()
                    } else {
                        logInfo "Engage garage switch from controller to reverse direction"
                        reverseDoorDirection()
                    }
                } else {
                    logInfo "Engage garage switch from controller"
                    garageSwitch.on()
                }
            }
        }
        state.lastDoorAction = evt.value
    } else {
        state.doorMoving = false
        state.openingBySwitch = false
    }
    state.lastDoorStatus = evt.value
}

def stopAndReverseDoorDirection() {
    garageSwitch.on()
    runInMillis(switchOffDelay.toLong() + reversalDelay.toLong(), garageOnOppositeDirection)
}

def reverseDoorDirection() {
    garageSwitch.on()
    runInMillis(switchOffDelay.toLong() + reversalDelay.toLong(), stopAndReverseDoorDirection)
}

def garageOnOppositeDirection() {
    state.lastDoorAction = (state.lastDoorAction == 'opening') ? 'closing' : 'opening'
    garageSwitch.on()
}

def garageSwitchHandler(evt) {    
    if (evt.value == 'on') {
        state.doorMoving = !state.doorMoving
        def doorStatus = garageControl.currentValue('door')
        
        if (state.doorMoving) {
            logDebug "Physical door moving"            
            if (doorStatus != 'opening' && doorStatus != 'closing') {            
                logInfo "Engage garage controller from switch"
                state.openingBySwitch = true
                if (state.lastDoorAction == 'opening') {
                    garageControl.close()
                } else {
                    garageControl.open()
                }
            }
        } else {
            logDebug "Physical door stopped"
            if (doorStatus != 'open' && doorStatus != 'closed') {
                log.warn "${garageControl.label} stopped while ${doorStatus}"
                garageControl.sendEvent(name: "door", value: "unknown", descriptionText: "${garageControl.label} stopped while ${doorStatus}")
            }            
        }
        runInMillis(switchOffDelay.toLong(), garageSwitchOff)
    }
}

def garageSwitchOff() {
    garageSwitch.off()
}

def startTimeout() {
    runIn(garageTime.toLong(), handleTimeout)
}

def handleTimeout() {
    def doorStatus = garageControl.currentValue('door')
    if (doorStatus == 'opening') {
        if (openContact) {
            log.warn "${garageControl.label} might be stuck while opening"
            garageControl.sendEvent(name: "door", value: "unknown", descriptionText: "${garageControl.label} might be stuck while opening")
        } else {
            garageControl.sendEvent(name: "door", value: "open", descriptionText: "${garageControl.label} is open after ${garageTime}s")
        }
    } else if (doorStatus == 'closing') {
        if (openContact) {
            log.warn "${garageControl.label} might be stuck while closing"
            garageControl.sendEvent(name: "door", value: "unknown", descriptionText: "${garageControl.label} might be stuck while closing")
        } else {
            garageControl.sendEvent(name: "door", value: "closed", descriptionText: "${garageControl.label} is closed after ${garageTime}s")
        }
    }
}

def garageOpenContactHandler(evt) {    
    if (evt.value == 'closed' && state.doorMoving) {
        logInfo "${openContact.label} detected that ${garageControl.label} is fully open"
        garageControl.sendEvent(name: "door", value: "open", descriptionText: "${openContact.label} detected that ${garageControl.label} is fully open")
    }
}

def garageClosedContactHandler(evt) {    
    if (evt.value == 'closed') {
        logInfo "${closedContact.label} detected that ${garageControl.label} is fully closed"
        garageControl.sendEvent(name: "door", value: "closed", descriptionText: "${closedContact.label} detected that ${garageControl.label} is fully closed")
    } else if (evt.value == 'open') {
        logInfo "${closedContact.label} detected that ${garageControl.label} is opening"
        garageControl.sendEvent(name: "door", value: "open", descriptionText: "${closedContact.label} detected that ${garageControl.label} is not fully closed")
    }
}

def disableDebugLogging() {
    log.info "Auto-disabling debug logging."
    app.updateSetting("enableLogging", [value: "false", type: "bool"])
}

def logInfo(msg) {
    if (enableLogging) log.info msg
}

def logDebug(msg) {
    if (enableLogging) log.debug msg
}