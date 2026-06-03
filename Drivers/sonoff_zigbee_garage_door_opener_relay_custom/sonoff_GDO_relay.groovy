/**
 *  Sonoff Zigbee Garage Door Opener driver Custom for Hubitat
 *
 *  https://community.hubitat.com/t/tuya-zigbee-garage-door-opener/95579
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 * ver. 1.0.0 2022-06-18 kkossev  - Inital test version
 * ver. 1.0.1 2022-06-19 kkossev  - fixed Contact status open/close; added doorTimeout preference, default 15s; improved debug loging; PowerSource capability'; contact open/close status determines door state!
 * ver. 1.0.2 2022-06-20 kkossev  - ignore Open command if the sensor is open; ignore Close command if the sensor is closed.
 * ver. 1.0.3 2022-06-26 kkossev  - fixed new device exceptions bug; warnings in Debug logs only; Debug logs are off by default.
 * ver. 1.0.4 2022-07-06 kkossev  - on() command opens the door if it was closed, off() command closes the door if it was open; 'contact is open/closed' info and warning logs are shown only on contact state change;
 * ver. 1.0.5 2023-10-09 kkossev  - added _TZE204_nklqjk62 fingerprint
 * ver. 1.1.0 2024-07-15 kkossev  - (dev.branch) added commands setContact() and setDoor()
 * ver. 1.1.0b 2024-07-22 jshimota - copy off kkossev Sonoff Zigbee Garage Door Opener
 * ver. 1.1.0c 2024-07-22 jshimota - removed PowerSource - unused.
 * ver. 1.1.0d 2024-07-22 jshimota - replaced PowerSource - still useless but was involved.
 * ver  1.1.0e 2024-09-15 jshimota - removed contact door state stuff
 * ver  1.1.0f 2024-09-24 jshimota - removed more stuff - set open and close just to pass through
 * ver  1.1.0g 2026-05-24 jshimota - fixed an NPE, gemini optimized
 * ver  1.1.0h 2026-05-27 jshimota - rebuilt for Sonoff Mini-ZBD
 */

/**
 * Sonoff Mini-ZBD Zigbee Garage Door Opener Relay driver Custom for Hubitat
 * version 1.1.0g
 */

def version() { "1.1.0h" }
def timeStamp() {"2026/05/27 6:30 PM"}

import hubitat.device.HubAction
import hubitat.device.Protocol
import groovy.transform.Field
import hubitat.zigbee.zcl.DataType

@Field static final Boolean _DEBUG = false
@Field static final Integer PULSE_TIMER  = 1250         // milliseconds

metadata {
    definition (name: "Sonoff GDO Relay - Custom", namespace: "jshimota", author: "James Shimota", importUrl: "https://raw.githubusercontent.com/jshimota/Hubitat/Drivers/sonoff_zigbee_garage_door_opener_relay_custom/sonoff_GDO_relay.groovy", singleThreaded: true ) {
        capability "Actuator"
        capability "Configuration"
        capability "Switch"
        capability "GarageDoorControl"

        if (_DEBUG) {
            command "initialize", [[name: "Manually initialize the device after switching drivers.\n\r     ***** Will load device default values! *****" ]]
        }
			fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0B05,FC57,FC11", outClusters:"0003,0006,0019", model:"MINI-ZBD", manufacturer:"SONOFF", controllerType: "ZGB", deviceJoinName: "Sonoff Garage Door Opener Relay"
    }

    preferences {
        input (name: "logEnable", type: "bool", title: "<b>Text logging</b>", description: "<i>Text information, useful for basic logging. Recommended value is <b>true</b></i>", defaultValue: true)
        input (name: "txtEnable", type: "bool", title: "<b>Description text logging</b>", description: "<i>Display measured values in HE log page. Recommended value is <b>true</b></i>", defaultValue: true)
        input (name: "dbgEnable", type: "bool", title: "<b>Debug logging</b>", description: "<i>Debug information, useful for troubleshooting. Recommended value is <b>false</b></i>", defaultValue: false)
        input (name: "trcEnable", type: "bool", title: "<b>Trace logging</b>", description: "<i>Trace code execution. Recommended value is <b>false</b></i>", defaultValue: false)
    }
}

private getCLUSTER_SONOFF() { 0x006 }

def parse(String description) {
    if (dbgEnable) log.debug "${device.displayName} parse: description is $description"
    checkDriverVersion()
    
    if (description?.startsWith('catchall:') || description?.startsWith('read attr -')) {
        def descMap = [:]
        try {
            descMap = zigbee.parseDescriptionAsMap(description)
        } catch (e) {
            log.warn "${device.displayName} parse: exception caught while parsing descMap: ${descMap}"
            return null
        }
   
        if (descMap?.clusterInt == CLUSTER_SONOFF) {
            if (dbgEnable) log.debug "${device.displayName} parse Sonoff Cluster: descMap = $descMap"
            if ( descMap?.command in ["00", "01", "02"] ) {
                def transid = zigbee.convertHexToInt(descMap?.data[1])
                def dp = zigbee.convertHexToInt(descMap?.data[2])
                def dp_id = zigbee.convertHexToInt(descMap?.data[3])
                def fncmd = getSonoffAttributeValue(descMap?.data)
                
                if (trcEnable) log.trace "${device.displayName} Sonoff cluster dp_id=${dp_id} dp=${dp} fncmd=${fncmd}"
                
                switch (dp) {
                    case 0x01 : // Relay / trigger switch
                        def value = fncmd == 1 ? "on" : "off"
                        if (dbgEnable) log.debug "${device.displayName} received Relay report dp_id=${dp_id} dp=${dp} fncmd=${fncmd}"
                        break
                    case 0x02 : // Confirmation payload
                        if (dbgEnable) log.debug "${device.displayName} received confirmation report dp_id=${dp_id} dp=${dp} fncmd=${fncmd}"
                        break
                    default :
                        if (logEnable) log.warn "${device.displayName} NOT PROCESSED Sonoff cmd: dp=${dp} value=${fncmd} descMap.data = ${descMap?.data}"
                        break
                }
            } 
            else if (descMap?.command == "0B") {    
                if (dbgEnable) log.debug "${device.displayName} ZCL response: 0x${descMap?.data[1]} status: ${descMap?.data[1]=='00'?'success':'FAILURE'} data: ${descMap?.data}"
            } else {
                if (logEnable) log.warn "${device.displayName} NOT PROCESSED COMMAND Sonoff cmd ${descMap?.command} : dp=${descMap?.data[2]} value=${descMap?.data[3]} descMap.data = ${descMap?.data}"
            }
        } 
        else if (descMap?.cluster == "0000" && descMap?.attrId == "0001") {
            if (dbgEnable) log.debug "${device.displayName} Sonoff check-in: ${descMap}"
        } else {
            if (dbgEnable) log.debug "${device.displayName} parsed non-Sonoff cluster: descMap = $descMap"
        }
    } 
}

private int getSonoffAttributeValue(ArrayList _data) {
    int retValue = 0
    if (_data?.size() >= 6) {
        def lengthVal = _data[5]
        if (lengthVal != null) {
            int dataLength = lengthVal as Integer
            int power = 1
            for (i in dataLength..1) {
                retValue = retValue + power * zigbee.convertHexToInt(_data[i+5])
                power = power * 256
            }
        }
    }
    return retValue
}

def open() { pulseOn() }
def close() { pulseOn() }

def on() {
    if (dbgEnable) log.debug "${device.displayName} Turning ON"
    sendSwitchEvent("on", true)
    relayOn()
}

def relayOn() {
    if (dbgEnable) log.debug "${device.displayName} Turning the relay ON"
    sendZigbeeCommands(zigbee.command(0x006, 0x01, "00010101000101"))
    pulseOn()
}

def pulseOn() {
    if (dbgEnable) log.debug "${device.displayName} pulseOn() timer started"
    runInMillis(PULSE_TIMER, 'off', [overwrite: true])
}

def off() {
    if (dbgEnable) log.debug "${device.displayName} Turning OFF"
    sendSwitchEvent("off", true)
    relayOff()
}

def relayOff() {
    if (dbgEnable) log.debug "${device.displayName} Turning the relay OFF"
    sendZigbeeCommands(zigbee.command(0x006, 0x00, "00010101000100"))
}

def sendSwitchEvent(state, isDigital=false) {
    // Prevent redundant event writes if the state hasn't changed
    if (device.currentValue("switch") == state) {
        if (trcEnable) log.trace "${device.displayName} switch is already ${state}, skipping event."
        return
    }

    def map = [:]
    map.name = "switch"
    map.value = state    
    map.type = isDigital ? "digital" : "physical"
    map.descriptionText = "${device.displayName} switch is ${map.value}"
    
    if (txtEnable) log.info "${map.descriptionText} (${map.type})"
    sendEvent(map)
}

void initializeVars( boolean fullInit = true ) {
    if (logEnable) log.info "${device.displayName} InitializeVars()... fullInit = ${fullInit}" 
    if (fullInit) {
        state.clear()
        state.driverVersion = driverVersionAndTimeStamp()
    }
    if (fullInit || settings?.logEnable == null) device.updateSetting("logEnable", [value:"false", type:"bool"]) 
    if (fullInit || settings?.txtEnable == null) device.updateSetting("txtEnable", [value:"true", type:"bool"]) 
}

def initialize() {
    if (txtEnable) log.info "${device.displayName} Initialize()..."
    unschedule()
    initializeVars()
    sendEvent(name: "power_retvalue", value: "1")
    updated()            
}

void logsOff(){
    log.warn "${device.displayName} Debug logging disabled..."
    device.updateSetting("dbgEnable", [value:"false", type:"bool"])
}

def tuyaBlackMagic() {
    return zigbee.readAttribute(0x0000, [0x0004, 0x000, 0x0001, 0x0005, 0x0007, 0xfffe], [:], delay=200)
}

def configure() {
    if (txtEnable) log.info "${device.displayName} configure().."
    checkDriverVersion()
    List<String> cmds = tuyaBlackMagic()
    sendZigbeeCommands(cmds)
}

def updated() {
    checkDriverVersion()
    log.info "${device.displayName} debug logging is: ${dbgEnable == true}"
    log.info "${device.displayName} description logging is: ${txtEnable == true}"
    if (txtEnable) log.info "${device.displayName} Updated..."
    if (dbgEnable) runIn(1800, logsOff, [overwrite: true])
}

def installed() {
    log.info "Installing..."
    log.info "Debug logging will be automatically disabled after 30 minutes"
    device.updateSetting("dbgEnable", [type:"bool", value:"false"])
    device.updateSetting("txtEnable", [type:"bool", value:"true"])
    if (dbgEnable) runIn(1800, logsOff, [overwrite: true])
}

def driverVersionAndTimeStamp() { version() + ' ' + timeStamp() }

def checkDriverVersion() {
    if (state.driverVersion != driverVersionAndTimeStamp()) {
        if (txtEnable) log.info "${device.displayName} updating settings from version ${state.driverVersion} to ${driverVersionAndTimeStamp()}"
        initializeVars(false)
        state.driverVersion = driverVersionAndTimeStamp()
    }
}

void sendZigbeeCommands(List<String> cmds) {
    if (trcEnable) log.trace "${device.displayName} sendZigbeeCommands : ${cmds}"
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}