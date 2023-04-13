/*
=============================================================================
Hubitat Elevation Driver for
Tuya Zigbee dimmer modules (1-Gang and 2-Gang)

    https://github.com/matt-hammond-001/hubitat-code

-----------------------------------------------------------------------------
This code is licensed as follows:

BSD 3-Clause License

Copyright (c) 2020, Matt Hammond
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
-----------------------------------------------------------------------------
ver 0.2.0  11/12/2021 Matt Hammond - added _TZ3000_7ysdnebc
ver 0.2.1  2021/12/29 kkossev      - added cluster 0003 to the fingerprint, model _TZ3000_7ysdnebc
ver 0.2.2  2022/05/28 kkossev      - moved model and inClusters to modelConfigs, added _TZE200_vm1gyrso 3-Gang Dimmer module
ver 0.2.3  2022/08/30 kkossev      - added TS110E _TZ3210_ngqk6jia fingerprint
ver 0.2.4  2022/09/19 kkossev      - added TS0601 _TZE200_w4cryh2i fingerprint
ver 0.2.5  2022/10/19 kkossev      - TS0601 level control; infoLogging
ver 0.2.6  2022/10/22 kkossev      - importURL to dev. branch; toggle() for TS0601; 'autoOn' for TS0601; level scaling for TS0601; minLevel and maxLevel receive/send for TS0601; bugfixes for TS0601 single EP devices
ver 0.2.7  2022/11/11 kkossev      - added _TZE200_ip2akl4w _TZE200_1agwnems _TZE200_la2c2uo9 _TZE200_579lguh2 _TZE200_fjjbhx9d _TZE200_drs6j6m5; secure the while loops coode when deleting and creating child devices;
ver 0.2.8  2022/11/13 kkossev      - _TZE200_ip2akl4w fingerprint hardcoded
ver 0.2.9  2022/12/10 kkossev      - deleting child devices bug fix; added _TZE200_fvldku9h Tuya Fan Switch; Remove old periodic job schedules; Tuya Time Sync';
ver 0.2.10 2023/01/02 kkossev      - added _TZE200_e3oitdyu
ver 0.2.11 2023/02/19 kkossev      - added TS110E _TZ3210_k1msuvg6; TS0601 _TZE200_r32ctezx fan controller; changed importURL to dev. branch; dp=4 - type of light source?; added GLEDOPTO GL-SD-001; 1-gang modles bug fixes;
ver 0.2.12 2023/03/12 kkossev      - more debug logging; fixed incorrect on/off status reporting bug for the standard ZCL dimmers; added autoRefresh option for GLEDOPTO
ver 0.3.0  2023/03/12 kkossev      - bugfix: TS110E/F configiration for the automatic level reporting was not working.
ver 0.4.0  2023/03/25 kkossev      - added TS110E _TZ3210_pagajpog; added advancedOptions; added forcedProfile; added deviceProfilesV2; added initialize() command; sendZigbeeCommands() in all Command handlers; configure() and updated() do not re-initialize the device!; setDeviceNameAndProfile(); destEP here and there
ver 0.4.1  2023/03/31 kkossev      - added new TS110E_GIRIER_DIMMER product profile (Girier _TZ3210_k1msuvg6 support @jshimota); installed() initialization and configuration sequence changed'; fixed GIRIER Toggle command not working; added _TZ3210_4ubylghk
*
*                                   TODO: Hubitat 'F2 bug' patched;
*                                   TODO: TS110E_GIRIER_DIMMER TS011E power_on_behavior_1, TS110E_switch_type ['toggle', 'state', 'momentary']) (TS110E_options - needsMagic())
*                                   TODO: Tuya Fan Switch support
*
*/

def version() { "0.4.1" }
def timeStamp() {"2023/03/31 12:15 AM"}

import groovy.transform.Field

@Field static final Boolean _DEBUG = false
@Field static final Boolean DEFAULT_DEBUG_OPT = true
@Field static final Boolean deviceSimulation = false
@Field static final String  simulatedModel = "TS110E"
@Field static final String  simulatedManufacturer = "_TZ3210_k1msuvg6"

@Field static def modelConfigs = [
        "_TYZB01_v8gtiaed": [ numEps: 2, model: "TS110F", inClusters: "0000,0004,0005,0006,0008",     joinName: "Tuya Zigbee 2-Gang Dimmer module" ],                // '2 gang smart dimmer switch module with neutral'
        "_TYZB01_qezuin6k": [ numEps: 1, model: "TS110F", inClusters: "0000,0004,0005,0006,0008",     joinName: "Tuya Zigbee 1-Gang Dimmer module" ],                // '1 gang smart dimmer switch module with neutral'
        "_TZ3000_ktuoyvt5": [ numEps: 1, model: "TS110F", inClusters: "0000,0004,0005,0006,0008",     joinName: "Tuya Zigbee 1-Gang Switch module" ],                // '1 gang smart        switch module without neutral'
        "_TZ3000_92chsky7": [ numEps: 2, model: "TS110F", inClusters: "0000,0004,0005,0006,0008",     joinName: "Tuya Zigbee 2-Gang Dimmer module (no-neutral)" ],   // '2 gang smart dimmer switch module without neutral'
        "_TZ3000_7ysdnebc": [ numEps: 2, model: "TS110F", inClusters: "0000,0004,0005,0003,0006,0008",joinName: "Tuya 2CH Zigbee dimmer module" ],
        "_TZE200_vm1gyrso": [ numEps: 3, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Tuya Zigbee 3-Gang Dimmer module" ],
        "_TZE200_whpb9yts": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Tuya Zigbee 1-Gang Dimmer module" ],                // 'Zigbee smart dimmer'
        "_TZE200_ebwgzdqq": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Tuya Zigbee 1-Gang Dimmer module" ],
        "_TZE200_9i9dt8is": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Tuya Zigbee 1-Gang Dimmer module" ],
        "_TZE200_dfxkcots": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Tuya Zigbee 1-Gang Dimmer module" ],
        "gq8b1uv":          [ numEps: 1, model: "gq8b1uv", inClusters: "0000,0004,0005,0006,0008",    joinName: "TUYATEC Zigbee smart dimmer" ],                     //  TUYATEC Zigbee smart dimmer
        "_TZ3210_ngqk6jia": [ numEps: 2, model: "TS110E", inClusters: "0005,0004,0006,0008,EF00,0000", joinName: "Lonsonho 2-gang Dimmer module"],                    // https://www.aliexpress.com/item/4001279149071.html
        "_TZ3210_zxbtub8r": [ numEps: 1, model: "TS110E", inClusters: "0004,0005,0003,0006,0008,EF00,0000", joinName: "GIRIER Dimmer module 1 ch."],                  // not tested
        "_TZE200_w4cryh2i": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Moes Zigbee Rotary/Touch Light Dimmer" ],             // https://community.hubitat.com/t/moes-zigbee-dimmer-touch/101195
        "_TZE200_ip2akl4w": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Moes Zigbee 1-Gang Dimmer module" ],                  // https://community.hubitat.com/t/tuya-moes-1-2-3-gang-dimmer/104596/5?u=kkossev
        "_TZE200_1agwnems": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Moes Zigbee 1-Gang Dimmer module" ],                  // not tested
        "_TZE200_la2c2uo9": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Moes Zigbee 1-Gang Dimmer module" ],                  // not tested
        "_TZE200_579lguh2": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Moes Zigbee 1-Gang Dimmer module" ],                  // not tested
        "_TZE200_fjjbhx9d": [ numEps: 2, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Moes Zigbee 2-Gang Dimmer module" ],                  // https://community.hubitat.com/t/tuya-moes-1-2-3-gang-dimmer/104596/5?u=kkossev
        "_TZE200_drs6j6m5": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Lifud Model LF-AAZ030-0750-42" ],                     // https://community.hubitat.com/t/tuya-moes-1-2-3-gang-dimmer/104596/25?u=kkossev
        "_TZE200_fvldku9h": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Tuya Fan Switch" ] ,                                  // https://www.aliexpress.com/item/4001242513879.html
        "_TZE200_r32ctezx": [ numEps: 1, model: "TS0601", inClusters: "0004,0005,EF00,0000",          joinName: "Tuya Fan Switch" ],                                   // https://www.aliexpress.us/item/3256804518783061.html https://github.com/Koenkk/zigbee2mqtt/issues/12793
        "_TZE200_e3oitdyu": [ numEps: 2, model: "TS110E", inClusters: "0000,0004,0005,EF00",          joinName: "Moes ZigBee Dimmer Switch 2CH"],                     // https://community.hubitat.com/t/moes-dimmer-module-2ch/110512
        "_TZ3210_k1msuvg6": [ numEps: 1, model: "TS110E", inClusters: "0004,0005,0003,0006,0008,EF00,0000", joinName: "Girier Zigbee 1-Gang Dimmer module"],           // https://community.hubitat.com/t/girier-tuya-zigbee-3-0-light-switch-module-smart-diy-breaker-1-2-3-4-gang-supports-2-way-control/104546/36?u=kkossev
        "GLEDOPTO":         [ numEps: 1, model: "GL-SD-001", inClusters: "0000,0003,0004,0005,0006,0008,1000", joinName: "Gledopto Triac Dimmer"],                     //
        "_TZ3210_pagajpog": [ numEps: 2, model: "TS110E", inClusters: "0005,0004,0006,0008,E001,0000", joinName: "Lonsonho Tuya Smart Zigbee Dimmer"],                 // https://community.hubitat.com/t/release-tuya-lonsonho-1-gang-and-2-gang-zigbee-dimmer-module-driver/60372/76?u=kkossev
        "_TZ3210_4ubylghk": [ numEps: 2, model: "TS110E", inClusters: "0005,0004,0006,0008,E001,0000", joinName: "Lonsonho Tuya Smart Zigbee Dimmer"]                  // https://community.hubitat.com/t/driver-support-for-tuya-dimmer-module-model-ts110e-manufacturer-tz3210-4ubylghk/116077?u=kkossev

]

def config() {
    return modelConfigs[device.getDataValue("manufacturer")]
}

def isTS0601() {
    if (isParent()) {
        return device.getDataValue('model') == "TS0601"
    }
    else {
        return parent?.device.getDataValue('model') == "TS0601"
    }
}

@Field static final Map deviceProfilesV2 = [
        "TS110F_DIMMER"  : [
                description   : "TS110F Tuya Dimmers",
                models        : ["TS110F"],
                fingerprints  : [
                        [numEps: 2, profileId:"0104", endpointId:"01", inClusters:"0000,0004,0005,0006,0008", outClusters:"0019,000A", model:"TS110F", manufacturer:"_TYZB01_v8gtiaed", deviceJoinName: "Tuya Zigbee 2-Gang Dimmer module"],            // '2 gang smart dimmer switch module with neutral'
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0000,0004,0005,0006,0008", outClusters:"0019,000A", model:"TS110F", manufacturer:"_TYZB01_qezuin6k", deviceJoinName: "Tuya Zigbee 1-Gang Dimmer module"],           // '1 gang smart dimmer switch module with neutral'
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0000,0004,0005,0006,0008", outClusters:"0019,000A", model:"TS110F", manufacturer:"_TZ3000_ktuoyvt5", deviceJoinName: "Tuya Zigbee 1-Gang Dimmer module"],            // '1 gang smart        switch module without neutral'
                        [numEps: 2, profileId:"0104", endpointId:"01", inClusters:"0000,0004,0005,0006,0008", outClusters:"0019,000A", model:"TS110F", manufacturer:"_TZ3000_92chsky7", deviceJoinName: "Tuya Zigbee 2-Gang Dimmer module (no-neutral)"], // '2 gang smart dimmer switch module without neutral'
                        [numEps: 2, profileId:"0104", endpointId:"01", inClusters:"0000,0004,0005,0003,0006,0008", outClusters:"0019,000A", model:"TS110F", manufacturer:"_TZ3000_7ysdnebc", deviceJoinName: "Tuya 2CH Zigbee dimmer module"]
                ],
                deviceJoinName: "TS110F Tuya Dimmer",
                capabilities  : ["SwitchLevel": true],
                attributes    : ["healthStatus": "unknown", "powerSource": "mains"],
                configuration : [],
                preferences   : []
        ],

        "TS110E_DIMMER"  : [
                description   : "TS110E Tuya Dimmers",
                models        : ["TS110F"],
                fingerprints  : [
                        [numEps: 2, profileId:"0104", endpointId:"01", inClusters:"0005,0004,0006,0008,EF00,0000", outClusters:"0019,000A", model:"TS110E", manufacturer:"_TZ3210_ngqk6jia", deviceJoinName: "Lonsonho 2-gang Dimmer module"],           // https://www.aliexpress.com/item/4001279149071.html
                        [numEps: 2, profileId:"0104", endpointId:"01", inClusters:"0000,0004,0005,EF00", outClusters:"0019,000A", model:"TS110E", manufacturer:"_TZE200_e3oitdyu", deviceJoinName: "Moes ZigBee Dimmer Switch 2CH"],                    // https://community.hubitat.com/t/moes-dimmer-module-2ch/110512
                        [numEps: 2, profileId:"0104", endpointId:"01", inClusters:"0005,0004,0006,0008,E001,0000", outClusters:"0019,000A", model:"TS110E", manufacturer:"_TZ3210_pagajpog", deviceJoinName: "Lonsonho Tuya Smart Zigbee Dimmer"],       // https://community.hubitat.com/t/release-tuya-lonsonho-1-gang-and-2-gang-zigbee-dimmer-module-driver/60372/76?u=kkossev
                        [numEps: 2, profileId:"0104", endpointId:"01", inClusters:"0005,0004,0006,0008,E001,0000", outClusters:"0019,000A", model:"TS110E", manufacturer:"_TZ3210_4ubylghk", deviceJoinName: "Lonsonho Tuya Smart Zigbee Dimmer"]        // https://community.hubitat.com/t/driver-support-for-tuya-dimmer-module-model-ts110e-manufacturer-tz3210-4ubylghk/116077?u=kkossev
                ],
                deviceJoinName: "TS110E Tuya Dimmer",
                capabilities  : ["SwitchLevel": true],
                attributes    : ["healthStatus": "unknown", "powerSource": "mains"],
                configuration : [],
                preferences   : []
        ],

        "TS110E_GIRIER_DIMMER"  : [
                description   : "TS110E Girier Dimmers",
                models        : ["TS110F"],
                fingerprints  : [
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,0003,0006,0008,EF00,0000", outClusters:"0019,000A", model:"TS110E", manufacturer:"_TZ3210_zxbtub8r", deviceJoinName: "GIRIER Dimmer module 1 ch."],         // not tested
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,0003,0006,0008,EF00,0000", outClusters:"0019,000A", model:"TS110E", manufacturer:"_TZ3210_k1msuvg6", deviceJoinName: "Girier Zigbee 1-Gang Dimmer module"], // https://community.hubitat.com/t/girier-tuya-zigbee-3-0-light-switch-module-smart-diy-breaker-1-2-3-4-gang-supports-2-way-control/104546/36?u=kkossev
                ],
                deviceJoinName: "TS110E Girier Dimmer",
                capabilities  : ["SwitchLevel": true],
                attributes    : ["healthStatus": "unknown", "powerSource": "mains"],
                configuration : [],
                preferences   : []
        ],


        "TS0601_DIMMER"  : [
                description   : "TS0601 Tuya Dimmers",
                models        : ["TS0601"],
                fingerprints  : [
                        [numEps: 3, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_vm1gyrso", deviceJoinName: "Tuya Zigbee 3-Gang Dimmer module"],
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_whpb9yts", deviceJoinName: "Tuya Zigbee 1-Gang Dimmer module"],        // 'Zigbee smart dimmer'
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_ebwgzdqq", deviceJoinName: "Tuya Zigbee 1-Gang Dimmer module"],
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_9i9dt8is", deviceJoinName: "Tuya Zigbee 1-Gang Dimmer module"],
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_dfxkcots", deviceJoinName: "Tuya Zigbee 1-Gang Dimmer module"],
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_w4cryh2i", deviceJoinName: "Moes Zigbee Rotary/Touch Light Dimmer"],   // https://community.hubitat.com/t/moes-zigbee-dimmer-touch/101195
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_ip2akl4w", deviceJoinName: "Moes Zigbee 1-Gang Dimmer module"],        // https://community.hubitat.com/t/tuya-moes-1-2-3-gang-dimmer/104596/5?u=kkossev
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_1agwnems", deviceJoinName: "Moes Zigbee 1-Gang Dimmer module"],        // not tested
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_la2c2uo9", deviceJoinName: "Moes Zigbee 1-Gang Dimmer module"],        // not tested
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_579lguh2", deviceJoinName: "Moes Zigbee 1-Gang Dimmer module"],        // not tested
                        [numEps: 2, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_fjjbhx9d", deviceJoinName: "Moes Zigbee 2-Gang Dimmer module"],        // https://community.hubitat.com/t/tuya-moes-1-2-3-gang-dimmer/104596/5?u=kkossev
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_drs6j6m5", deviceJoinName: "Lifud Model LF-AAZ030-0750-42"]            // https://community.hubitat.com/t/tuya-moes-1-2-3-gang-dimmer/104596/25?u=kkossev
                ],
                deviceJoinName: "TS0601 Tuya Dimmer",
                capabilities  : ["SwitchLevel": true],
                attributes    : ["healthStatus": "unknown", "powerSource": "mains"],
                configuration : [],
                preferences   : []
        ],

        "TS0601_FAN"  : [
                description   : "TS0601 Fan Switch",
                models        : ["TS0601"],
                fingerprints  : [
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_fvldku9h", deviceJoinName: "Tuya Fan Switch"],                         // https://www.aliexpress.com/item/4001242513879.html
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"TS0601", manufacturer:"_TZE200_r32ctezx", deviceJoinName: "Tuya Fan Switch"]                          // https://www.aliexpress.us/item/3256804518783061.html https://github.com/Koenkk/zigbee2mqtt/issues/12793
                ],
                deviceJoinName: "TS0601 Fan Switch",
                capabilities  : ["SwitchLevel": false],
                attributes    : ["healthStatus": "unknown", "powerSource": "mains"],
                configuration : [],
                preferences   : []
        ],

        "OTHER_OEM_DIMMER"  : [
                description   : "Other OEM Dimmer",
                models        : ["gq8b1uv", "GL-SD-001"],
                fingerprints  : [
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"gq8b1uv", manufacturer:"gq8b1uv", deviceJoinName: "TUYATEC Zigbee smart dimmer"],                         // https://www.aliexpress.com/item/4001242513879.html
                        [numEps: 1, profileId:"0104", endpointId:"01", inClusters:"0004,0005,EF00,0000", outClusters:"0019,000A", model:"GL-SD-001", manufacturer:"GLEDOPTO", deviceJoinName: "Gledopto Triac Dimmer"]                          // https://www.aliexpress.us/item/3256804518783061.html https://github.com/Koenkk/zigbee2mqtt/issues/12793
                ],
                deviceJoinName: "Other OEM Dimmer",
                capabilities  : ["SwitchLevel": false],
                attributes    : ["healthStatus": "unknown", "powerSource": "mains"],
                configuration : [],
                preferences   : []
        ]
]

def getModelGroup()          { return state.deviceProfile ?: "UNKNOWN" }
def getDeviceProfilesMap()   {deviceProfilesV2.values().description as List<String>}

def isFanController() { return getModelGroup().contains("TS0601_FAN") }
def isTS110E()        { return getModelGroup().contains("TS110E_DIMMER") }
def isGirier()        { return getModelGroup().contains("TS110E_GIRIER_DIMMER") }

metadata {
    definition (
            name: "Tuya Zigbee dimmer module",
            namespace: "matthammonddotorg",
            author: "Matt Hammond",
            description: "Driver for Tuya zigbee dimmer modules",
            documentationLink: "https://github.com/matt-hammond-001/hubitat-code/blob/master/drivers/tuya-zigbee-dimmer-module.README.md",
            importUrl: "https://raw.githubusercontent.com/kkossev/hubitat-matt-hammond-fork/development/drivers/tuya-zigbee-dimmer-module.groovy"
    ) {

        capability "Configuration"
        capability "Refresh"
        capability "Light"
        capability "Switch"
        capability "SwitchLevel"

        command "toggle"
        command "initialize", [[name: "Initialize the sensor after switching drivers.  \n\r   ***** Will load device default values! *****" ]]

        if (_DEBUG == true) {
            command "zTest", [
                    [name:"dpCommand", type: "STRING", description: "Tuya DP Command", constraints: ["STRING"]],
                    [name:"dpValue",   type: "STRING", description: "Tuya DP value", constraints: ["STRING"]],
                    [name:"dpType",    type: "ENUM",   constraints: ["DP_TYPE_VALUE", "DP_TYPE_BOOL", "DP_TYPE_ENUM"], description: "DP data type"]
            ]
            command "test", [[name: "test", type: "STRING", description: "test", constraints: ["STRING"]]]
        }

        modelConfigs.each{ data ->
            fingerprint profileId: "0104",
                    inClusters: data.value.inClusters,
                    outClusters:"0019,000A",
                    model: data.value.model,
                    manufacturer: data.key,
                    deviceJoinName: data.value.joinName
        }
    }

    preferences {
        input "debugEnable", "bool", title: "<b>Enable debug logging</b>", required: false, defaultValue: DEFAULT_DEBUG_OPT
        input "infoEnable", "bool", title: "<b>Enable info logging</b>", required: false, defaultValue: true
        input "warnEnable", "bool", title: "<b>Enable warning logging</b>", required: false, defaultValue: false
        input "traceEnable", "bool", title: "<b>Enable trace logging</b>", required: false, defaultValue: false
        input "autoOn", "bool", title: "<b>Turn on when level adjusted</b>", description: "<i>Switch turns on automatically when dimmer level is adjusted.</i>", required: true, multiple: false, defaultValue: true
        input "autoRefresh", "bool", title: "<b>Auto refresh when level adjusted</b>", description: "<i>Automatically send an Refresh command when dimmer level is adjusted.</i>", required: true, multiple: false, defaultValue: false

        input "minLevel", "number", title: "<b>Minimum level</b>", description: "<i>Minimum brightness level (%). 0% on the dimmer level is mapped to this.</i>", required: true, multiple: false, defaultValue: 0
        if (minLevel == null ) { minLevel = 0 }
        if (minLevel < 0) { minLevel = 0 } else if (minLevel > 99) { minLevel = 99 }

        input "maxLevel", "number", title: "<b>Maximum level</b>", description: "<i>Maximum brightness level (%). 100% on the dimmer level is mapped to this.</i>", required: true, multiple: false, defaultValue: 100
        if (maxLevel == null ) { maxLevel = 100 }
        if (maxLevel < minLevel) { maxLevel = 100 } else if (maxLevel > 100) { maxLevel = 100 }
        /*
        if (isTS110E()) {
            input name: 'lightType', type: 'enum', title: '<b>Light Type</b>', options: TS110ELightTypeOptions.options, defaultValue: TS110ELightTypeOptions.defaultValue, description: \
                '<i>Configures the lights type.</i>'
            input name: 'switchType', type: 'enum', title: '<b>Switch Type</b>', options: TS110ESwitchTypeOptions.options, defaultValue: TS110ESwitchTypeOptions.defaultValue, description: \
                '<i>Configures the switch type.</i>'

        }
        */
        input (name: "advancedOptions", type: "bool", title: "Advanced Options", description: "<i>May not work for all device types!</i>", defaultValue: false)
        if (advancedOptions == true) {
            input (name: "forcedProfile", type: "enum", title: "<b>Device Profile</b>", description: "<i>Forcibly change the Device Profile, if the model/manufacturer was not recognized automatically.<br>Warning! Manually setting a device profile may not always work!</i>",
                    options: getDeviceProfilesMap() /*getDeviceProfiles()*/)

        }
    }
}
/*
-----------------------------------------------------------------------------
Logging output
-----------------------------------------------------------------------------
*/

def logDebug(msg) {
    if (settings.debugEnable) {
        log.debug msg
    }
}

def logInfo(msg) {
    if (settings.infoEnable) {
        log.info msg
    }
}

def logWarn(msg) {
    if (settings.warnEnable) {
        log.warn msg
    }
}

def logTrace(msg) {
    if (settings.traceEnable) {
        log.trace msg
    }
}
/// end of Logging output
@Field static final int TS110E_BRIGHTNESS = 0xF000            // (61440)       // TODO: 0, 1000 -> 1, 254 (don't go to 255!!) - Check - probably the brightness is controlled on attribute 0xF000 (cluster 8)!!!
@Field static final Map TS110ESwitchTypeOptions = [           // 0xFC00 (64512)
                                                              defaultValue: 0,
                                                              options     : [0: 'momentary', 1: 'toggle', 2: 'state']
]
@Field static final Map TS110ELightTypeOptions = [            // 0xFC02 (64514), type: 0x20
                                                              defaultValue: 0,
                                                              options     : [0: 'led', 1: 'incandescent', 2: 'halogen']
]
@Field static final int TS110E_MIN_BRIGHTNESS = 0xFC03        // (64515)       // TODO: 0, 1000 -> 1, 255,  type: 0x21 Check !!!
@Field static final int TS110E_MAX_BRIGHTNESS = 0xFC04        // (64516)       // TODO: 0, 1000 -> 1, 255,  type: 0x21 -  Check !!!
// Girier TS110E may not support power-on-behavour?  https://github.com/Koenkk/zigbee2mqtt/issues/15902#issuecomment-1382848150     https://github.com/Koenkk/zigbee2mqtt/issues/16804




/*
-----------------------------------------------------------------------------
Setting up child devices
-----------------------------------------------------------------------------
*/

def createChildDevices() {
    def numEps = config().numEps

    if (numEps == 1) {
        numEps = 0
        logDebug "single channel dimmer, no child devices are needed"
    } else {
        logDebug "about to delete the child devices:  ${numEps} expected , found ${getChildDevices().size()}"
    }

    def index = getChildDevices().size()
    if (index == null || index == 0)   {
        logDebug "no child devices to be deleted"
    }
    else {
        for (int i=0; i<index; i++) {
            def dni = indexToChildDni(i)
            if (dni != null) {
                logInfo "Deleting child ${i} with dni ${dni}"
                deleteChildDevice(dni)
            }
            else {
                logDebug "child device ${i} DNI was not found!"
            }
        }
    }
    if (numEps <= 1) {
        logDebug "no child devices to be created"
        return
    }

    logDebug "about to create ${numEps} child devices"
    for (int i=0; i<numEps; i++) {
        // create child devices
        def dni = indexToChildDni(i)
        def endpointId = indexToEndpointId(i)
        logInfo "Creating child ${i} with dni ${dni}"
        addChildDevice(
                "Tuya Zigbee dimmer module",
                dni,
                [
                        completedSetup: true,
                        label: "${device.displayName} (CH${endpointId})",
                        isComponent:true,
                        componentName: "ch${endpointId}",
                        componentLabel: "Channel ${endpointId}"
                ]
        )
    }
}


// called from initialized(); returns the bind+configuration commands or null
def listenChildDevices() {
    ArrayList<String> cmds = []

    if (isTS0601()) {
        return null
    }
    logDebug "listenChildDevices(): getChildEndpointIds() = ${getChildEndpointIds()} size=${getChildEndpointIds().size()}"
    getChildEndpointIds().each{ endpointId ->
        //logDebug "endpointId = ${endpointId}"
        if (endpointId != null && endpointId != 0 && endpointId != 0xF2) {
            cmds += [
                    //bindings
                    "zdo bind 0x${device.deviceNetworkId} 0x${endpointId} 0x01 0x0006 {${device.zigbeeId}} {}", "delay 200",
                    "zdo bind 0x${device.deviceNetworkId} 0x${endpointId} 0x01 0x0008 {${device.zigbeeId}} {}", "delay 200",
                    //reporting
                    "he cr 0x${device.deviceNetworkId} 0x${endpointId} 0x0006 0 0x10 1 0xFFFE {}","delay 200",
                    "he cr 0x${device.deviceNetworkId} 0x${endpointId} 0x0008 0 0x20 1 0xFFFE {}", "delay 200",
            ] + cmdRefresh(endpointIdToChildDni(endpointId))
        }
    }
    logDebug "returning binding and reporting configuration commands: ${cmds}"
    return cmds
}

/*
-----------------------------------------------------------------------------
Propagate settings changes both ways between Parent and First Child
-----------------------------------------------------------------------------
*/

def onChildSettingsChange(childDni, childSettings) {
    def i = endpointIdToIndex(childDniToEndpointId(childDni))
    logInfo "updating settings for child device #${i+1} ..."
    if (i==0) {
        device.updateSetting("minLevel",childSettings.minLevel)
        device.updateSetting("maxLevel",childSettings.maxLevel)
        device.updateSetting("autoOn",childSettings.autoOn)
    }
    else {
        logWarn "skipped onChildSettingsChange() for child device #${i+1}"
    }
}

def onParentSettingsChange(parentSettings) {
    logInfo "updating settings for the parent device ..."
    device.updateSetting("minLevel",parentSettings.minLevel)
    device.updateSetting("maxLevel",parentSettings.maxLevel)
    device.updateSetting("autoOn",parentSettings.autoOn)
    /*
    if (isTS0601()) {
        logWarn "Min/Max levels NOT changed on the parent device"
    }
*/
}

/*
-----------------------------------------------------------------------------
Command handlers - send Zigbee commands to the device

if child, then ask parent to act on its behalf
if parent, then act on endpoint 1
-----------------------------------------------------------------------------
*/

// sends Zigbee commands to refresh the switch and the level
def refresh() {
    logWarn("REFRESH command being run ...")
    if (isParent()) {
        logDebug "refresh(): parent ${indexToChildDni(0)}"
        ArrayList<String> cmds = cmdRefresh(indexToChildDni(0))
        sendZigbeeCommands(cmds)
    } else {
        logDebug "refresh(): child ${device.deviceNetworkId}"
        parent?.doActions( parent?.cmdRefresh(device.deviceNetworkId) )
    }
}

// sends Zigbee commands to turn the switch on
def on() {
    logWarn("ON command being run ...")
    if (isParent()) {
        sendZigbeeCommands(cmdSwitch(indexToChildDni(0), 1))
    } else {
        parent?.doActions( parent?.cmdSwitch(device.deviceNetworkId, 1) )
    }
}

// sends Zigbee commands to turn the switch off
def off() {
    logWarn("OFF command being run...")
    if (isParent()) {
        sendZigbeeCommands(cmdSwitch(indexToChildDni(0), 0))
    } else {
        parent?.doActions( parent?.cmdSwitch(device.deviceNetworkId, 0) )
    }
}

// sends Zigbee commands to toggle the switch
def toggle() {
    logWarn("TOGGLE command being run ...")
    if (isParent()) {
        sendZigbeeCommands(cmdSwitchToggle(indexToChildDni(0)))
        logWarn("Opposite switch state sent ...")
    } else {
        parent?.doActions( parent?.cmdSwitchToggle(device.deviceNetworkId) )
    }
}

// sends Zigbee commands to set level
def setLevel(level, duration=0) {
    logWarn("SETLEVEL command being run ...")
    if (settings.autoRefresh == true) {
        runIn(1, 'refresh')
    }
    if (isParent()) {
        def value = levelToValue(level)
        sendZigbeeCommands(cmdSetLevel(indexToChildDni(0), value, duration))
    } else {
        def value = levelToValue(level)
        parent?.doActions( parent?.cmdSetLevel(device.deviceNetworkId, value, duration) )
    }
}



/*
---------------------------------------------------------------------------------------------------
Hub Action (cmd) generators - only return ArrayList<String> Zigbee commands to the calling function
---------------------------------------------------------------------------------------------------
*/


// returns Zigbee commands to refresh the switch and the level
def cmdRefresh(String childDni) {
    def endpointId = childDniToEndpointId(childDni)
    logDebug "cmdRefresh(childDni=${childDni} endpointId=${endpointId})  isParent()=${isParent()}"
    // changed 03/25/2023 - always try to refresh clusters 6 & 8, even for Tuya switches... do not return null!
    /*
    if (isTS0601()) {
        logWarn "cmdRefresh NOT implemented for TS0601!"
        return null
    }
    */
    return [
            "he rattr 0x${device.deviceNetworkId} 0x${endpointId} 0x0006 0 {}",
            "delay 100",
            "he rattr 0x${device.deviceNetworkId} 0x${endpointId} 0x0008 0 {}",
            "delay 100"
    ]
}

// returns Zigbee commands to toggle the switch
def cmdSwitchToggle(String childDni) {
    logWarn("Toggle begin...")
    def endpointId = childDniToEndpointId(childDni)
    if (isTS0601() || isGirier()) {
        logWarn("Toggle found an isTS0601 or Girier...")
        if (device.currentState('switch', true).value == 'on') {
            logWarn("Toggle found an isTS0601 or Girier that was ON...")
            return cmdSwitch(childDni, 0)
        }
        else {
            logWarn("Toggle found an isTS0601 or Girier that was OFF...")
            return cmdSwitch(childDni, 1)
        }
    }
    else {
        return [
                "he cmd 0x${device.deviceNetworkId} 0x${endpointId} 0x0006 2 {}",
                "delay 500"
        ] + cmdRefresh(childDni)
    }
}

// returns Zigbee commands for on or off
def cmdSwitch(String childDni, onOff) {
    ArrayList<String> cmds = []
    def endpointId = childDniToEndpointId(childDni)
    logDebug "cmdSwitch: childDni=${childDni} endpointId=${endpointId} onOff=${onOff}"
    onOff = onOff ? "1" : "0"

    if (isTS0601()) {
        def dpValHex  = zigbee.convertToHexString(onOff as int, 2)
        def cmd = childDni[-2..-1]
        def dpCommand = cmd == "01" ? "01" : cmd == "02" ? "07" : cmd == "03" ? "0F" : null
        cmds = sendTuyaCommand(dpCommand, DP_TYPE_BOOL, dpValHex)
        logDebug "${device.displayName}  sending cmdSwitch command=${dpCommand} value=${onOff} ($dpValHex) cmds=${cmds}"
    }
    else {
        cmds = ["he cmd 0x${device.deviceNetworkId} 0x${endpointId} 0x0006 ${onOff} {}"]
        logDebug "${device.displayName}  sending cmdSwitch endpointId=${endpointId} value=${onOff} cmds=${cmds}"
    }
    return cmds
}

// returns Zigbee commands for level control
def cmdSetLevel(String childDni, value, duration) {
    def endpointId = childDniToEndpointId(childDni)
    value = value.toInteger()
    value = value > 255 ? 255 : value
    value = value < 1 ? 0 : value

    duration = (duration * 10).toInteger()
    def child = getChildByEndpointId(endpointId)
    logDebug "cmdSetLevel: child=${child} childDni=${childDni} value=${value} duration=${duration}"
    if (isTS0601()) {
        ArrayList<String> cmdsTuya = []
        value = (value*10) as int
        def dpValHex  = zigbee.convertToHexString(value as int, 8)
        def cmd = childDni[-2..-1]
        def dpCommand = cmd == "01" ? "02" : cmd == "02" ? "08" : cmd == "03" ? "10" : null
        if (isFanController()) {
            dpCommand = "04"
            dpValHex  = zigbee.convertToHexString((value/10) as int, 8)
        }
        logDebug "${device.displayName}  sending cmdSetLevel command=${dpCommand} value=${value} ($dpValHex)"
        cmdsTuya = sendTuyaCommand(dpCommand, DP_TYPE_VALUE, dpValHex)
        if (child.isAutoOn() && device.currentState('switch', true).value != 'on') {
            logDebug "${device.displayName} AutoOn(): sending cmdSwitch on for switch #${endpointId}"
            cmdsTuya += cmdSwitch(childDni, 1)
        }
        logDebug "cmdSetLevel: sending cmdsTuya=${cmdsTuya}"
        return cmdsTuya
    }

    def cmd = [
            "he cmd 0x${device.deviceNetworkId} 0x${endpointId} 0x0008 4 { 0x${intTo8bitUnsignedHex(value)} 0x${intTo16bitUnsignedHex(duration)} }",
    ]

    if (child.isAutoOn()) {
        cmd += "he cmd 0x${device.deviceNetworkId} 0x${endpointId} 0x0006 1 {}"
    }
    logDebug "cmdSetLevel: sending cmds=${cmd}"
    return cmd
}

/*
-----------------------------------------------------------------------------
Parent only code
-----------------------------------------------------------------------------
*/

// replaced with sendZigbeeCommands
def doActions(List<String> cmds) {
    if (isParent()) {
        sendZigbeeCommands( cmds )
/*
        hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()

        cmds.each { it ->
            if(it.startsWith('delay') == true) {
                allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.DELAY))
            } else {
                allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE))
            }
        }
        sendHubCommand(allActions)
*/
        logDebug "Sending actions: ${cmds}"
    } else {
        throw new Exception("doActions() called incorrectly by child")
    }
}


def parse(String description) {
    checkDriverVersion()
    logDebug "Received raw: ${description}"

    if (isParent()) {
        def descMap = [:]
        try {
            descMap = zigbee.parseDescriptionAsMap(description)
        }
        catch (e) {
            logWarn "exception ${e} caught while parsing description:  ${description}"
        }
        logDebug "Received descMap: ${descMap}"
        if (description.startsWith("catchall")) {
            logDebug "catchall clusterId=${descMap?.clusterId} command=${descMap?.command} data=${descMap?.data}"
            if (descMap?.clusterId == "EF00") {
                return parseTuyaCluster(descMap)
            }
            /*
            else {
                logWarn "Ignored non-Tuya cluster catchall clusterId=${descMap?.clusterId} command=${descMap?.command} data=${descMap?.data}"
                return null
            }
*/
        }
        //
        Integer value = 0
        if (descMap?.value != null && descMap?.encoding != "42") {
            try {
                value = Integer.parseInt(descMap.value, 16)
            }
            catch (e) {
                logWarn "exception ${e} caught while converting ${descMap?.value} to integer, encoding is ${descMap?.encoding}"
            }
        }
        def child = this
        def isFirst = true
        if (descMap?.endpoint != null) {
            child = getChildByEndpointId(descMap.endpoint)
            isFirst = 0 == endpointIdToIndex(descMap.endpoint)
        }

        switch (descMap.clusterInt) {
            case 0x0000: // basic cluster
                parseBasicCluster( descMap )
                break
            case 0x0006: // switch state
                logDebug "on/off cluster 0x0006 command ${descMap?.command} value ${value}"
                if (descMap?.command == "07" && descMap?.data.size() >= 1) {
                    logDebug "Received Configure Reporting Response for cluster:${descMap.clusterId} , data=${descMap.data} (Status: ${descMap.data[0]=="00" ? 'Success' : '<b>Failure</b>'})"
                    break
                }
                if (descMap?.command == "0B" && descMap?.data.size() >= 2) {
                    String clusterCmd = descMap.data[0]
                    def status = descMap.data[1]
                    logDebug "Received ZCL Command Response for cluster ${descMap.clusterId} command ${clusterCmd}, data=${descMap.data} (Status: ${descMap.data[1]=="00" ? 'Success' : '<b>Failure</b>'})"
                    break
                }
                child.onSwitchState(value)
                if (isFirst && child != this) {
                    logDebug "Replicating switchState in parent"
                    onSwitchState(value)
                } else {
                    logDebug "${isFirst} ${this} ${child} ${value}"
                }
                break
            case 0x0008: // switch level state
                if (descMap?.command == "07" && descMap?.data.size() >= 1) {
                    logDebug "Received Configure Reporting Response for cluster:${descMap.clusterId} , data=${descMap.data} (Status: ${descMap.data[0]=="00" ? 'Success' : '<b>Failure</b>'})"
                    break
                }
                if (descMap?.command == "0B" && descMap?.data.size() >= 2) {
                    String clusterCmd = descMap.data[0]
                    def status = descMap.data[1]
                    logDebug "Received ZCL Command Response for cluster ${descMap.clusterId} command ${clusterCmd}, data=${descMap.data} (Status: ${descMap.data[1]=="00" ? 'Success' : '<b>Failure</b>'})"
                    break
                }
                logDebug "switch level cluster 0x0008 command ${descMap?.command} attrId ${descMap?.attrId} value ${value}"
                if (descMap?.attrId == "0000" || descMap?.attrId == "F000") {
                    child.onSwitchLevel(value)
                    if (isFirst && child != this) {
                        logDebug "Replicating switchLevel in parent"
                        onSwitchLevel(value)
                    }
                } else if (descMap?.attrId == "FC02") {
                    //value = hexStrToUnsignedInt(descMap.value)
                    logInfo "light type is '${TS110ELightTypeOptions.options[value]}' (0x${descMap.value})"
                    //device.updateSetting('lightType', [value: value.toString(), type: 'enum'])
                } else if (descMap?.attrId == "FC03") {
                    logInfo "minLevel is '${TS110ELightTypeOptions.options[value]}' (0x${descMap.value})"
                    //device.updateSetting('minLevel', [value: value, type: 'number'])
                } else if (descMap?.attrId == "FC04") {
                    logInfo "maxLevel is '${TS110ELightTypeOptions.options[value]}' (0x${descMap.value})"
                    //device.updateSetting('maxLevel', [value: value, type: 'number'])
                }
                break
            case 0x8021:
                logDebug "Received bind response, data=${descMap.data} (Sequence Number:${descMap.data[0]}, Status: ${descMap.data[1]=="00" ? 'Success' : '<b>Failure</b>'})"
                break
            default :
                logWarn "UNPROCESSED endpoint=${descMap?.endpoint} cluster=${descMap?.cluster} command=${descMap?.command} attrInt = ${descMap?.attrInt} value= ${descMap?.value} data=${descMap?.data}"
                break
        }
    }
    else {
        throw new Exception("parse() called incorrectly by child")
    }
}

/*
-----------------------------------------------------------------------------
Tuya cluster EF00 specific code
-----------------------------------------------------------------------------
*/

def tuyaBlackMagic() {
    def ep = safeToInt(state.destinationEP ?: 01)
    if (ep==null || ep==0) ep = 1
    return zigbee.readAttribute(0x0000, [0x0004, 0x000, 0x0001, 0x0005, 0x0007, 0xfffe], [destEndpoint :ep], delay=200)
}

def parseTuyaCluster( descMap ) {
    if (descMap.clusterId != "EF00") {
        return null
    }
    if (descMap.command == "0B") {
        logDebug "Tuya command 0x0B data=${descMap.data}"
        return null
    }
    if (descMap.command == "24") {
        logDebug "Tuya Time Sync request data=${descMap.data}"
        def offset = 0
        try {
            offset = location.getTimeZone().getOffset(new Date().getTime())
        }
        catch(e) {
            logWarn "Cannot resolve current location. please set location in Hubitat location setting. Setting timezone offset to zero"
        }
        def cmds = zigbee.command(0xEF00, 0x24, "0008" +zigbee.convertToHexString((int)(now()/1000),8) +  zigbee.convertToHexString((int)((now()+offset)/1000), 8))
        logDebug "sending time data : ${cmds}"
        cmds.each{ sendHubCommand(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE)) }
        return null
    }
    def cmd = descMap.data[2]
    def value = getAttributeValue(descMap.data)
    switch (cmd) {
        case "01" : // Switch1
        case "07" : // Switch2
        case "0F" : // Switch3
            handleTuyaClusterSwitchCmd(cmd, value)
            break
        case "02" : // Brightness1 (switch level state)
        case "08" : // Brightness2
        case "10" : // Brightness3
            logDebug "received: Tuya brighntness(level) cmd=${cmd} value=${value}"
            handleTuyaClusterBrightnessCmd(cmd, value/10 as int)
            break
        case "03" : // Minimum brightness1
        case "09" : // Minimum brightness2
        case "11" : // Minimum brightness3
            def switchNumber = cmd == "03" ? "01" : cmd == "09" ? "02" : cmd == "11" ? "03" : null
            logDebug "received: minimum brightness switch#${switchNumber} is ${value/10 as int} (raw=${value})"
            handleTuyaClusterMinBrightnessCmd(cmd, value/10 as int)
            break
        case "05" : // Maximum brightness1
        case "0B" : // Maximum brightness2
        case "13" : // Maximum brightness3
            def switchNumber = cmd == "05" ? "01" : cmd == "0B" ? "02" : cmd == "13" ? "03" : null
            logDebug "received: maximum brightness switch#${switchNumber} is ${value/10 as int} (raw=${value})"
            handleTuyaClusterMaxBrightnessCmd(cmd, value/10 as int)
            break
        case "06" : // Countdown1
        case "0C" : // Countdown2
        case "14" : // Countdown3
            def switchNumber = cmd == "06" ? "01" : cmd == "0C" ? "02" : cmd == "14" ? "03" : null
            logInfo "Countdown ${switchNumber} is ${value}s"
            break
        case "04" : // (04) level for _TZE200_fvldku9h ;  Tuya type of light source for all others?
            if (isFanController()) {
                handleTuyaClusterBrightnessCmd(cmd, value as int)
            } else {
                logDebug "received: Tuya type of light source cmd=${cmd} value=${value}"    // (LED, halogen, incandescent)
            }
            break
        case "0A" : // (10)
            logDebug "Unknown Tuya dp= ${cmd} fn=${value}"
            break
        case "0E" : // (14)
            logInfo "Power-on Status Setting is ${value}"
            break
        case "12" : // (18)
            logDebug "Unknown Tuya dp= ${cmd} fn=${value}"
            break
        case "15" : // (21)
            logInfo "Light Mode is ${value}"
            break
        case "1A" : // (26)
            logInfo "Switch backlight ${value}"
            break
        case "40" : // (64)
            logDebug "Unknown Tuya dp= ${cmd} fn=${value}"
            break
        default :
            logWarn "UNHANDLED Tuya cmd=${cmd} value=${value}"
            break
    }
}

def parseBasicCluster( descMap ) {
    switch (descMap.attrId) {
        case "0001" :
            logDebug "Tuya check-in ${descMap.attrId} (${descMap.value})"
            break
        case "0004" : // attrInt = 4 value= _TZE200_vm1gyrso data=null
            logDebug "Tuya check-in ${descMap.attrId} (${descMap.value})"
            break
        default :
            logWarn "unprocessed Basic cluster endpoint=${descMap?.endpoint} cluster=${descMap?.cluster} command=${descMap?.command} attrInt = ${descMap?.attrInt} value= ${descMap?.value} data=${descMap?.data}"
            break
    }

}

def handleTuyaClusterSwitchCmd(cmd,value) {
    def switchNumber = cmd == "01" ? "01" : cmd == "07" ? "02" : cmd == "0F" ? "03" : null
    logInfo "Switch ${switchNumber} is ${value==0 ? "off" : "on"}"
    if (config().numEps == 1) {
        onSwitchState(value)
    }
    else {
        def child = getChildByEndpointId(switchNumber)
        def isFirst = 0 == endpointIdToIndex(switchNumber)
        child.onSwitchState(value)
        if (isFirst && child != this) {
            logDebug "Replicating switchState in parent"
            onSwitchState(value)
        }
    }
}

def handleTuyaClusterBrightnessCmd(cmd, value) {
    def switchNumber = cmd == "02" ? "01" : cmd == "08" ? "02" : cmd == "10" ? "03" : "01"
    scaledValue = valueToLevel(value)
    logInfo "Brightness ${switchNumber} is ${scaledValue}% (${value})"
    if (config().numEps == 1)  {
        onSwitchLevel(value)
    }
    else {
        def child = getChildByEndpointId(switchNumber)
        def isFirst = 0 == endpointIdToIndex(switchNumber)
        child.onSwitchLevel(value)
        if (isFirst && child != this) {
            logDebug "Replicating switchLevel in parent"
            onSwitchLevel(value)
        }
    }
}

def handleTuyaClusterMinBrightnessCmd(cmd, value) {
    def switchNumber = cmd == "03" ? "01" : cmd == "09" ? "02" : cmd == "11" ? "03" : null
    if (config().numEps == 1) {
        device.updateSetting("minLevel", [value: value , type:"number"])
        logInfo "minLevel brightness parameter was updated to ${value}%"
    }
    else {
        def child = getChildByEndpointId(switchNumber)
        logDebug "cmd=${cmd} switchNumber=${switchNumber} child = ${child}"
        def isFirst = 0 == endpointIdToIndex(switchNumber)
        child.updateSetting("minLevel", [value: value , type:"number"])
        logInfo "minLevel brightness parameter for switch #${switchNumber} was updated to ${value}%"
        if (isFirst && child != this) {
            logDebug "Replicating minBrightness in parent"
            device.updateSetting("minLevel", [value: value , type:"number"])
        }
    }
}

def handleTuyaClusterMaxBrightnessCmd(cmd, value) {
    def switchNumber = cmd == "05" ? "01" : cmd == "0B" ? "02" : cmd == "13" ? "03" : null
    if (config().numEps == 1) {
        device.updateSetting("maxLevel", [value: value , type:"number"])
        logInfo "maxLevel brightness parameter was updated to ${value}%"
    }
    else {
        def child = getChildByEndpointId(switchNumber)
        logDebug "child = ${child}"
        def isFirst = 0 == endpointIdToIndex(switchNumber)
        child.updateSetting("maxLevel", [value: value , type:"number"])
        logInfo "maxLevel brightness parameter for switch #${switchNumber} was updated to ${value}%"
        if (isFirst && child != this) {
            logDebug "Replicating maxBrightness in parent"
            device.updateSetting("maxLevel", [value: value , type:"number"])
        }
    }
}



private int getAttributeValue(ArrayList _data) {
    int retValue = 0
    try {
        if (_data.size() >= 6) {
            int dataLength = zigbee.convertHexToInt(_data[5]) as Integer
            int power = 1
            for (i in dataLength..1) {
                retValue = retValue + power * zigbee.convertHexToInt(_data[i+5])
                power = power * 256
            }
        }
    }
    catch ( e ) {
        log.error "Exception caught while parsing Tuya data : ${_data}"
    }
    return retValue
}

private sendTuyaCommand(int dp, int dpType, int fnCmd, int fnCmdLength) {
    def ep = safeToInt(state.destinationEP)
    if (ep==null || ep==0) ep = 1

    def dpHex = zigbee.convertToHexString(dp, 2)
    def dpTypeHex = zigbee.convertToHexString(dpType, 2)
    def fnCmdHex = zigbee.convertToHexString(fnCmd, fnCmdLength)
    logDebug("sendTuyaCommand: dp=0x${dpHex}, dpType=0x${dpTypeHex}, fnCmd=0x${fnCmdHex}, fnCmdLength=${fnCmdLength}")
    def message = (randomPacketId().toString()
            + dpHex
            + dpTypeHex
            + zigbee.convertToHexString((fnCmdLength / 2) as int, 4)
            + fnCmdHex)
    logTrace("sendTuyaCommand: message=${message}")
    zigbee.command(CLUSTER_TUYA, ZIGBEE_COMMAND_SET_DATA, [destEndpoint :ep], message)
}

private randomPacketId() {
    return zigbee.convertToHexString(new Random().nextInt(65536), 4)
}

/*
-----------------------------------------------------------------------------
Child only code
-----------------------------------------------------------------------------
*/

def onSwitchState(value) {
    def valueText = value == 0 ? "off" : "on"
    logInfo "${device.displayName} set ${valueText}"
    sendEvent(name:"switch", value: valueText, descriptionText: "${device.displayName} set ${valueText}", unit: null)
}

def onSwitchLevel(value) {
    def level = valueToLevel(value.toInteger())    // TODO - null pointer exception! https://community.hubitat.com/t/girier-tuya-zigbee-3-0-dimmable-1-gang-switch-w-neutral/112620/10?u=kkossev
    logDebug "onSwitchLevel: Value=${value} level=${level} (value=${value})"

    sendEvent(name:"level", value: level, descriptionText:"${device.displayName} set ${level}%", unit: "%")
}

/*
-----------------------------------------------------------------------------
Parent only helper functions
-----------------------------------------------------------------------------
*/

@Field static def childDniPattern = ~/^([0-9A-Za-z]+)-([0-9A-Za-z]+)$/

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// mappings between index, endpointId and childDni
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

def indexToEndpointId(i) {
    return intTo8bitUnsignedHex(i+1)
}

def endpointIdToIndex(i) {
    //log.trace "endpointIdToIndex i=${i}"
    return Integer.parseInt(i,16) - 1
}

def endpointIdToChildDni(endpointId) {
    //log.trace "endpointIdToChildDni ${endpointId} = ${device.deviceNetworkId} - ${endpointId}"
    //def childDni = "${device.deviceNetworkId}-${endpointId}"
    //log.trace "childDni = ${childDni}"
    return "${device.deviceNetworkId}-${endpointId}"
}

def indexToChildDni(i) {
    return endpointIdToChildDni(indexToEndpointId(i))
}

def childDniToEndpointId(childDni) {
    def match = childDni =~ childDniPattern
    if (match) {
        if (match[0][1] == device.deviceNetworkId) {
            return match[0][2]
        }
    }
    return null
}

def childDnisToEndpointIds(List<String> childDnis) {
    return childDnis.collect{cDni -> childDniToEndpointId(cDni)}
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// retrieving children, or retrieving parent if no children and endpointId
// is first index
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

def getChildByEndpointId(endpointId) {
    //log.trace "getChildByEndpointId endpointId=${endpointId}"
    if (endpointIdToIndex(endpointId) == 0 && getChildDevices().size() == 0) {
        //log.trace "getChildByEndpointId returning this: ${this}"
        return this
    } else {
        //log.trace "getChildByEndpointId returning getChildDevice: ${getChildDevice(endpointIdToChildDni(endpointId))}"
        return getChildDevice(endpointIdToChildDni(endpointId))
    }
}

def getChildEndpointIds() {
    if (getChildDevices().size() == 0) {
        //logDebug "getChildEndpointIds() : no children"
        return [device.endpointId]
    } else {
        //logDebug "getChildEndpointIds() : size = ${getChildDevices().size()}"
        return getChildDevices().collect{childDniToEndpointId(it.getDni())}
    }
}

/*
-----------------------------------------------------------------------------
Child only helper functions
-----------------------------------------------------------------------------
*/

def getDni() {
    return device.deviceNetworkId
}

def isAutoOn() {
    return settings.autoOn
}



def levelToValue(BigDecimal level) {
    return levelToValue(level.toInteger())
}

def levelToValue(Integer level) {
    def mult = isTS0601() ? 1.0 : 2.55
    Integer minValue = Math.round(settings.minLevel * mult)
    Integer maxValue = Math.round(settings.maxLevel * mult)
    def reScaled =  rescale(level, 0, 100, minValue, maxValue)
    logDebug "level=${level} reScaled=${reScaled}"
    return reScaled
}

def valueToLevel(BigDecimal value) {
    return valueToLevel(value.toInteger())
}

def valueToLevel(Integer value) {
    def mult = isTS0601() ? 1.0 : 2.55
    Integer minValue = Math.round(settings.minLevel * mult)
    Integer maxValue = Math.round(settings.maxLevel * mult)
    if (value < minValue) return 0
    if (value > maxValue) return 100
    def reScaled = rescale(value, minValue, maxValue, 0, 100)
    logDebug "value=${value} reScaled=${reScaled}"
    return reScaled
}

/*
-----------------------------------------------------------------------------
Standard handlers
-----------------------------------------------------------------------------
*/

def getDeviceInfo() {
    return "model=${device.getDataValue('model')} manufacturer=${device.getDataValue('manufacturer')} destinationEP=${state.destinationEP} <b>deviceProfile=${state.deviceProfile}</b>"
}

//  will be the first function called just once when paired as a new device. Not called again on consequent re-pairings !
def installed() {
    logDebug "<b>installed()</b> ... ${getDeviceInfo()}"
}

// called every time the device is paired to the HUB (both as new or as an existing device)
def configure() {
    logDebug "<b>configure()</b> ... ${getDeviceInfo()}"
    setDestinationEP()
    checkDriverVersion()
    if (state.deviceProfile == null) {
        setDeviceNameAndProfile()
    }
    else {
        logInfo "the selected ${state.deviceProfile} device profile was not changed!"
    }
    // TuyaBlackMagic + reate child devices
    initialized()
    updated()
}

// called on hub startup if driver specifies capability "Initialize" (otherwise is not required or automatically called if present)
def initialize() {
    logDebug "<b>initialize()</b> ... ${getDeviceInfo()}"
    initializeVars( fullInit = true )
    configure()
}

// called from configure()
def setDestinationEP() {
    def ep = device.getEndpointId()
    if (ep != null && ep != 'F2') {
        state.destinationEP = ep
        logDebug "setDestinationEP() destinationEP = ${state.destinationEP}"
    }
    else {
        logWarn "setDestinationEP() Destination End Point not found or invalid(${ep}), activating the F2 bug patch!"
        state.destinationEP = "01"    // fallback EP
    }
}

void initializeVars( boolean fullInit = false ) {
    logInfo "InitializeVars( fullInit = ${fullInit} )..."
    if (fullInit) {
        state.clear()
        state.driverVersion = driverVersionAndTimeStamp()
    }
    if (fullInit || state.deviceProfile == null) {
        setDeviceNameAndProfile()
    }
}

// will be called when user selects Save Preferences
def updated() {
    logDebug "<b>updated()</b> ... ${getDeviceInfo()}"
    checkDriverVersion()

    // version 0.3.1
    if (settings?.forcedProfile != null) {
        logDebug "state.deviceProfile=${state.deviceProfile}, settings.forcedProfile=${settings?.forcedProfile}, getProfileKey()=${getProfileKey(settings?.forcedProfile)}"
        if (getProfileKey(settings?.forcedProfile) != state.deviceProfile) {
            logWarn "changing the device profile from ${state.deviceProfile} to ${getProfileKey(settings?.forcedProfile)}"
            state.deviceProfile = getProfileKey(settings?.forcedProfile)
            logInfo "press F5 to refresh the page"
        }
    }
    else {
        logDebug "forcedProfile is not set"
    }


    //
    if (isTS0601()) {
        ArrayList<String> cmdsTuya = []
        def cmd = "01"
        def eps = parent?.config()?.numEps
        logDebug "config().numEps = ${eps}"
        if (eps > 1) {
            cmd = this.device.getData().componentName[-2..-1]
            logInfo "### updating settings for child device ${this.device.getData().componentName} ... device #${cmd}"
        }
        else {    // single EP device
            logInfo "### updating settings for device ${device.getDataValue("manufacturer")} ${config()}"
        }
        // minLevel
        Integer value = Math.round(this.minLevel * 10)
        def dpValHex  = zigbee.convertToHexString(value as int, 8)
        logDebug "updated() minLevel value = ${this.minLevel} (raw=$value)"
        def dpCommand = cmd == "01" ? "03" : cmd == "02" ? "09" : cmd == "03" ? "11" : null
        logDebug "sending minLevel command=${dpCommand} value=${value} ($dpValHex)"
        if (isParent()) cmdsTuya += sendTuyaCommand(dpCommand, DP_TYPE_VALUE, dpValHex)
        else cmdsTuya += parent?.sendTuyaCommand(dpCommand, DP_TYPE_VALUE, dpValHex)
        // maxLevel
        value = Math.round(this.maxLevel * 10)
        dpValHex  = zigbee.convertToHexString(value as int, 8)
        logDebug "updated() maxLevel  value = ${this.maxLevel} (raw=$value)"
        dpCommand = cmd == "01" ? "05" : cmd == "02" ? "0B" : cmd == "03" ? "13" : null
        logDebug "sending maxLevel command=${dpCommand} value=${value} ($dpValHex)"
        if (isParent()) cmdsTuya += sendTuyaCommand(dpCommand, DP_TYPE_VALUE, dpValHex)
        else cmdsTuya += parent?.sendTuyaCommand(dpCommand, DP_TYPE_VALUE, dpValHex)
        //
        hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
        cmdsTuya.each {
            allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE))
        }
        sendHubCommand(allActions)
    }
    if (isParent()) {
        logDebug "updated parent->child"
        getChildByEndpointId(indexToEndpointId(0)).onParentSettingsChange(settings)
    } else {
        logDebug "updated child->parent"
        parent?.onChildSettingsChange(device.deviceNetworkId, settings)
    }
    // ver 1.3.1 03/25/2023 commented out
    //return initialized()
}

// custom initialization method, called from installed()
def initialized() {
    logDebug "<b>initialized()</b> ... ${getDeviceInfo()}"
    ArrayList<String> cmds = []
    if (debug == true && deviceSimulation == true) {
        device.updateDataValue("model", simulatedModel)
        device.updateDataValue("manufacturer", simulatedManufacturer)
        logDebug "device simulation: ${simulatedModel} ${simulatedManufacturer}"
    }
    unschedule() // added 12/10/2022
    logDebug "initialized() device.getData() = ${device.getData()}"

    if (isParent()) {
        createChildDevices()
        if (true /*device.getDataValue("model") == "TS0601"*/) {    // TODO !! must be called for TS0011E also! TODO: isTuya() !
            logDebug "spelling tuyaBlackMagic()"
            cmds += tuyaBlackMagic()
        }
        else {
            logDebug "tuyaBlackMagic() was skipped for model ${device.getDataValue('model')}"
        }
        ArrayList<String> configCmds = listenChildDevices()
        if (configCmds != null) {
            cmds += configCmds
        }
        //
        sendZigbeeCommands(cmds)
    }
    else {
        logDebug "skipping initialized() for child device"
    }
}



/*
-----------------------------------------------------------------------------
Shared helper functions
-----------------------------------------------------------------------------
*/

def isParent() {
    return getParent() == null
}

def rescale(value, fromLo, fromHi, toLo, toHi) {
    return Math.round((((value-fromLo)*(toHi-toLo))/(fromHi-fromLo)+toLo))
}

def intTo16bitUnsignedHex(value) {
    def hexStr = zigbee.convertToHexString(value.toInteger(),4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}

def intTo8bitUnsignedHex(value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
}


//////////////////////////////////////////////////////////////////////////////

private getCLUSTER_TUYA()       { 0xEF00 }
private getSETDATA()            { 0x00 }
private getSETTIME()            { 0x24 }

// Tuya Commands
private getTUYA_REQUEST()       { 0x00 }
private getTUYA_REPORTING()     { 0x01 }
private getTUYA_QUERY()         { 0x02 }
private getTUYA_STATUS_SEARCH() { 0x06 }
private getTUYA_TIME_SYNCHRONISATION() { 0x24 }

// tuya DP type
private getDP_TYPE_RAW()        { "01" }    // [ bytes ]
private getDP_TYPE_BOOL()       { "01" }    // [ 0/1 ]
private getDP_TYPE_VALUE()      { "02" }    // [ 4 byte value ]
private getDP_TYPE_STRING()     { "03" }    // [ N byte string ]
private getDP_TYPE_ENUM()       { "04" }    // [ 0-255 ]
private getDP_TYPE_BITMAP()     { "05" }    // [ 1,2,4 bytes ] as bits

private sendTuyaCommand(dp, dp_type, fncmd) {
    ArrayList<String> cmds = []
    def ep = safeToInt(state.destinationEP)
    if (ep==null || ep==0) ep = 1

    cmds += zigbee.command(CLUSTER_TUYA, SETDATA, [destEndpoint :ep], PACKET_ID + dp + dp_type + zigbee.convertToHexString((int)(fncmd.length()/2), 4) + fncmd )
    logDebug "${device.displayName} sendTuyaCommand = ${cmds}"
    return cmds
}

private getPACKET_ID() {
    return randomPacketId()
}

Integer safeToInt(val, Integer defaultVal=0) {
    return "${val}"?.isInteger() ? "${val}".toInteger() : defaultVal
}

Double safeToDouble(val, Double defaultVal=0.0) {
    return "${val}"?.isDouble() ? "${val}".toDouble() : defaultVal
}

void sendZigbeeCommands(ArrayList<String> cmd) {
    logDebug "${device.displayName} sendZigbeeCommands(cmd=$cmd)"
    hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
    cmd.each {
        allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE))
    }
    sendHubCommand(allActions)
}

def driverVersionAndTimeStamp() {version() + ' ' + timeStamp() + ((debug==true) ? " debug version!" : " ")}

def checkDriverVersion() {
    if (state.driverVersion != null && driverVersionAndTimeStamp() == state.driverVersion) {
        // no driver version change
    }
    else {
        logDebug "${device.displayName} updating the settings from the current driver version ${state.driverVersion} to the new version ${driverVersionAndTimeStamp()}"
        state.driverVersion = driverVersionAndTimeStamp()
    }
}

def updateTuyaVersion() {
    def application = device.getDataValue("application")
    if (application != null) {
        def ver = zigbee.convertHexToInt(application)
        def str = ((ver&0xC0)>>6).toString() + "." + ((ver&0x30)>>4).toString() + "." + (ver&0x0F).toString()
        if (device.getDataValue("tuyaVersion") != str) {
            device.updateDataValue("tuyaVersion", str)
            logInfo "tuyaVersion set to $str"
        }
    }
    else {
        return null
    }
}

def getProfileKey(String valueStr) {
    def key = null
    deviceProfilesV2.each {  profileName, profileMap ->
        if (profileMap.description.equals(valueStr)) {
            key = profileName
        }
    }
    return key
}

def getDeviceNameAndProfile( model=null, manufacturer=null) {
    def deviceName         = UNKNOWN
    def deviceProfile      = UNKNOWN
    String deviceModel        = model != null ? model : device.getDataValue('model') ?: UNKNOWN
    String deviceManufacturer = manufacturer != null ? manufacturer : device.getDataValue('manufacturer') ?: UNKNOWN
    deviceProfilesV2.each { profileName, profileMap ->
        profileMap.fingerprints.each { fingerprint ->
            if (fingerprint.model == deviceModel && fingerprint.manufacturer == deviceManufacturer) {
                deviceProfile = profileName
                deviceName = fingerprint.deviceJoinName ?: deviceProfilesV2[deviceProfile].deviceJoinName ?: UNKNOWN
                logDebug "<b>found exact match</b> for model ${deviceModel} manufacturer ${deviceManufacturer} : <b>profileName=${deviceProfile}</b> deviceName =${deviceName}"
                return [deviceName, deviceProfile]
            }
        }
    }
    if (deviceProfile == UNKNOWN) {
        logWarn "<b>NOT FOUND!</b> deviceName =${deviceName} profileName=${deviceProfile} for model ${deviceModel} manufacturer ${deviceManufacturer}"
    }
    return [deviceName, deviceProfile]
}

// called from TODO
def setDeviceNameAndProfile( model=null, manufacturer=null) {
    def (String deviceName, String deviceProfile) = getDeviceNameAndProfile(model, manufacturer)
    if (deviceProfile == null) {
        logWarn "unknown model ${deviceModel} manufacturer ${deviceManufacturer}"
        // don't change the device name when unknown
        state.deviceProfile = UNKNOWN
    }
    def dataValueModel = model != null ? model : device.getDataValue('model') ?: UNKNOWN
    def dataValueManufacturer  = manufacturer != null ? manufacturer : device.getDataValue('manufacturer') ?: UNKNOWN
    if (deviceName != NULL && deviceName != UNKNOWN  ) {
        device.setName(deviceName)
        state.deviceProfile = deviceProfile
        //logDebug "before: forcedProfile = ${settings.forcedProfile} to be set to ${deviceProfilesV2[deviceProfile].description}"
        device.updateSetting("forcedProfile", [value:deviceProfilesV2[deviceProfile].description, type:"enum"])
        //pause(1)
        //logDebug "after : forcedProfile = ${settings.forcedProfile}"
        logInfo "device model ${dataValueModel} manufacturer ${dataValueManufacturer} was set to : <b>deviceProfile=${deviceProfile} : deviceName=${deviceName}</b>"
    } else {
        logWarn "device model ${dataValueModel} manufacturer ${dataValueManufacturer} was not found!"
    }
}


def zTest( dpCommand, dpValue, dpTypeString ) {
    ArrayList<String> cmds = []
    def dpType   = dpTypeString=="DP_TYPE_VALUE" ? DP_TYPE_VALUE : dpTypeString=="DP_TYPE_BOOL" ? DP_TYPE_BOOL : dpTypeString=="DP_TYPE_ENUM" ? DP_TYPE_ENUM : null
    def dpValHex = dpTypeString=="DP_TYPE_VALUE" ? zigbee.convertToHexString(dpValue as int, 8) : dpValue

    logWarn "${device.displayName}  sending TEST command=${dpCommand} value=${dpValue} ($dpValHex) type=${dpType}"

    sendZigbeeCommands( sendTuyaCommand(dpCommand, dpType, dpValHex) )
}

def test(String description) {
    log.warn "test parsing : ${description}"
    parse( description)
    setDestinationEP()
}