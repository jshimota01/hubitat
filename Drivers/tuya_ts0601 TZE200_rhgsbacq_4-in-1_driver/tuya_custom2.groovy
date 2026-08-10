/* groovylint-disable NglParseError, ImplicitReturnStatement, InsecureRandom, MethodReturnTypeRequired, MethodSize, ParameterName, PublicMethodsBeforeNonPublicMethods, StaticMethodsBeforeInstanceMethods, UnnecessaryGroovyImport, UnnecessaryObjectReferences, UnusedImport, VariableName */
/**
 *  Tuya Multi Sensor 4 In 1 Targeted Driver (Temp, Humidity, Illuminance, Motion)
 *  Target Hardware: Model TS0601 / Manufacturer _TZE200_rhgsbacq (ZG-204ZV)
 *
 *  Licensed under the Apache License, Version 2.0
 */

static String version() { "1.0.0" }
static String timeStamp() {"2026/08/09 1:30 PM"}

@Field static final Boolean _DEBUG = false
@Field static final Boolean _TRACE_ALL = false
@Field static final Boolean DEFAULT_DEBUG_LOGGING = false

import groovy.transform.Field
import hubitat.device.HubMultiAction
import hubitat.device.Protocol
import hubitat.helper.HexUtils

deviceType = "MultiSensor4in1"
@Field static final String DEVICE_TYPE = "MultiSensor4in1"

metadata {
    definition (
		name: 'Tuya 4 In 1 Sensor (_TZE200_rhgsbacq)',
		importUrl: '',
		namespace: 'jshimota', 
		author: 'James Shimota', 
		singleThreaded: true 
	) {
        capability 'MotionSensor'
        capability 'TemperatureMeasurement'
        capability 'RelativeHumidityMeasurement'
        capability 'IlluminanceMeasurement'
        capability 'Battery'
        capability 'Sensor'

        attribute 'all', 'string'
        attribute 'pirSensitivity', 'enum', ['low', 'middle', 'high']
        attribute 'pirDelay', 'number'

        command 'sendCommand', [
            [name:'command', type: 'STRING', description: 'command name', constraints: ['STRING']],
            [name:'val',     type: 'STRING', description: 'command parameter value', constraints: ['STRING']]
        ]
        command 'setPar', [
            [name:'par', type: 'STRING', description: 'preference parameter name', constraints: ['STRING']],
            [name:'val', type: 'STRING', description: 'preference parameter value', constraints: ['STRING']]
        ]       

        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0003,0500,EF00,0402,0405,0001,0400", outClusters:"0003", model:"TS0601", manufacturer:"_TZE200_rhgsbacq", controllerType: "ZGB"
    }

    preferences {
        input name: 'txtEnable', type: 'bool', title: '<b>Enable descriptionText logging</b>', defaultValue: true, description: 'Enables events logging.'
        input name: 'logEnable', type: 'bool', title: '<b>Enable debug logging</b>', defaultValue: DEFAULT_DEBUG_LOGGING, description: 'Turns on debug logging for 24 hours.'
        input name: 'allStatusTextEnable', type: 'bool', title: "<b>Enable 'all' Status Attribute Creation?</b>", description: 'Status attribute for Devices/Rooms', defaultValue: false
        
        // Profile specific parameters
        input name: 'invertMotion', type: 'bool', title: '<b>Invert Motion State</b>', defaultValue: true, description: 'Invert active/inactive reported logic if reversed.'
        input name: 'pirSensitivity', type: 'enum', title: '<b>PIR Sensitivity</b>', options: [0:'low', 1:'middle', 2:'high'], defaultValue: '1'
        input name: 'pirDelay', type: 'number', title: '<b>PIR Delay (Seconds)</b>', range: '10..180', defaultValue: 30, description: 'Time before presence resets after last motion (10-180s).'
    }
}

boolean is4in1() { return true }

@Field static final Map deviceProfilesV3 = [
    'TS0601_TZE200_RHGSBACQ' : [
        description   : 'Tuya ZG-204ZV 4-in-1 Sensor (_TZE200_rhgsbacq)',
        models        : ['TS0601'],
        device        : [type: 'PIR', isIAS:false, powerSource: 'battery', isSleepy:true],
        capabilities  : ['MotionSensor': true, 'TemperatureMeasurement': true, 'RelativeHumidityMeasurement': true, 'IlluminanceMeasurement': true, 'Battery': true],
        preferences   : ['motionReset':true, 'invertMotion':true, 'pirSensitivity':'9', 'pirDelay':'12'],
        commands      : ['resetStats':'resetStats', 'resetPreferencesToDefaults':'resetPreferencesToDefaults'],
        fingerprints  : [
            [profileId:'0104', endpointId:'01', inClusters:'0000,0003,0500,EF00,0402,0405,0001,0400', outClusters:'0003', model:'TS0601', manufacturer:'_TZE200_rhgsbacq', controllerType:'ZGB', deviceJoinName: 'Tuya ZG-204ZV 4-in-1 Sensor']
        ],
        tuyaDPs: [
            [dp:1,  name:'motion',          preProc:'invert', type:'enum',   rw: 'ro', min:0,     max:1,      defVal:'0',  scale:1,  map:[0:'inactive', 1:'active'], unit:'', title:'<b>Motion</b>', description:'Motion detection'],
            [dp:4,  name:'battery',         type:'number',    rw: 'ro', min:0,     max:100,    defVal:100,  scale:1,  unit:'%', title:'<b>Battery level</b>', description:'Battery level'],
            [dp:7,  name:'temperature',     type:'decimal',   rw: 'ro', min:-20.0, max:80.0,   defVal:0.0,  scale:10, unit:'deg.', title:'<b>Temperature</b>', description:'Temperature'],
            [dp:8,  name:'humidity',        type:'number',    rw: 'ro', min:0,     max:100,    defVal:50,   scale:1,  unit:'%RH', title:'<b>Humidity</b>', description:'Relative humidity'],
            [dp:9,  name:'pirSensitivity',  type:'enum',      rw: 'rw', min:0,     max:2,      defVal:'1',  scale:1,  map:[0:'low', 1:'middle', 2:'high'], unit:'', dt:'01', tuyaCmd:04, title:'<b>PIR Sensitivity</b>', description:'PIR sensitivity level'],
            [dp:11, name:'illuminance',     type:'number',    rw: 'ro', min:0,     max:100000, defVal:0,    scale:1,  unit:'lx', title:'<b>Illuminance</b>', description:'Illuminance'],
            [dp:12, name:'pirDelay',        type:'number',    rw: 'rw', min:10,    max:180,    defVal:30,   scale:1,  unit:'seconds', dt:'02', tuyaCmd:04, title:'<b>PIR Delay</b>', description:'Time before motion resets (seconds)']
        ],
        configuration : ['battery': false]
    ]
]

public void customParseIasMessage(final String description) {
    Map zs = zigbee.parseZoneStatusChange(description)
    if (zs.alarm1Set == true) {
        logDebug "customParseIasMessage: Alarm 1 is set"
        handleMotion(true)
    } else {
        logDebug "customParseIasMessage: Alarm 1 is cleared"
        handleMotion(false)
    }
}

void customParseOccupancyCluster(final Map descMap) {
    final Integer value = safeToInt(hexStrToUnsignedInt(descMap.value))
    boolean result = processClusterAttributeFromDeviceProfile(descMap)
    if (result == false && descMap.attrId == '0000') {
        int raw = Integer.parseInt(descMap.value, 16)
        handleMotion(raw ? true : false)
    }
}

boolean customProcessTuyaDp(final Map descMap, final int dp, final int dp_id, final int fncmd, final int dp_len=0) {
    logDebug "customProcessTuyaDp: dp=${dp} dp_id=${dp_id} fncmd=${fncmd} dp_len=${dp_len}"
    if (processTuyaDPfromDeviceProfile(descMap, dp, dp_id, fncmd, dp_len) == true) {
        return true 
    }
    localProcessTuyaDP(descMap, dp, dp_id, fncmd, dp_len)
    return true
}

void localProcessTuyaDP(final Map descMap, final int dp, final int dp_id, final int fncmd, final int dp_len) {
    switch (dp) {
        case 0x01:
            handleMotion(fncmd ? true : false)
            break
        case 0x04:
            handleTuyaBatteryLevel(fncmd)
            break
        case 0x07:
            handleTemperatureEvent(fncmd / 10.0 as BigDecimal)
            break
        case 0x08:
            handleHumidityEvent(fncmd as BigDecimal)
            break
        case 0x0B:
            handleIlluminanceEvent(fncmd as int)
            break
        default:
            logDebug "NOT PROCESSED Tuya cmd: dp=${dp} value=${fncmd}"
            break
    }
}

void customProcessDeviceProfileEvent(final Map descMap, final String name, valueScaled, final String unitText, final String descText) {
    switch (name) {
        case 'motion':
            handleMotion(valueScaled == 'active' ? true : false)
            break
        case 'temperature':
            handleTemperatureEvent(valueScaled as Float)
            break
        case 'humidity':
            handleHumidityEvent(valueScaled as Float)
            break
        case 'illuminance':
            handleIlluminanceEvent(valueScaled as int)
            break
        default:
            sendEvent(name: name, value: valueScaled, unit: unitText, descriptionText: descText, type: 'physical', isStateChange: true)
            break
    }
}

List<String> customRefresh() {
    List<String> cmds = refreshFromDeviceProfileList() ?: []
    if (settings.allStatusTextEnable == true) {
        runIn(3, 'formatAttrib', [overwrite: true])
    }
    return cmds
}

void customUpdated() {
    if (settings?.allStatusTextEnable == false) {
        device.deleteCurrentState('all')
    }
    updateAllPreferences()
    if (settings.allStatusTextEnable == true) {
        runIn(3, 'formatAttrib', [overwrite: true])
    }
}

boolean isIAS() { return false }

List<String> customConfigureDevice() {
    List<String> cmds = []
    return cmds
}

void customInitializeVars(final boolean fullInit=false) {
    state.deviceProfile = 'TS0601_TZE200_RHGSBACQ'
    if (fullInit == true || state.motionStarted == null) { state.motionStarted = unix2formattedDate(now()) }
    if (fullInit == true || settings.invertMotion == null) device.updateSetting('invertMotion', true)
    if (fullInit == true || settings.allStatusTextEnable == null) device.updateSetting('allStatusTextEnable', false)
}

void customInitEvents(final boolean fullInit=false) { }

void customParseIlluminanceCluster(final Map descMap) {
    standardParseIlluminanceCluster(descMap)
}

void customParseIASCluster(final Map descMap) {
    boolean result = processClusterAttributeFromDeviceProfile(descMap)
    if (!result) standardParseIASCluster(descMap) 
}

void formatAttrib() {
    if (settings.allStatusTextEnable == false) return
    String attrStr = ''
    attrStr += addToAttr('status', 'healthStatus')
    attrStr += addToAttr('motion', 'motion')
    attrStr += addToAttr('battery', 'battery')
    attrStr += addToAttr('illuminance', 'illuminance')
    attrStr += addToAttr('temperature', 'temperature')
    attrStr += addToAttr('humidity', 'humidity')
    attrStr = attrStr.substring(0, attrStr.length() - 3)
    updateAttr('all', attrStr)
}

String addToAttr(String name, String key, String convert = 'none') {
    String retResult = ''
    String attrUnit = getUnitFromState(key) ?: ''
    def curVal = device.currentValue(key, true)
    if (curVal != null) {
        retResult += curVal.toString() + '' + attrUnit
    } else {
        retResult += 'n/a'
    }
    retResult += ',  '
    return retResult
}

String getUnitFromState(String attrName) {
    return device.currentState(attrName)?.unit
}

void updateAttr(String aKey, String aValue, String aUnit = '') {
    sendEvent(name:aKey, value:aValue, unit:aUnit, type: 'digital')
}

// ========================================================================================================================
// Libraries (deviceProfileLib & commonLib) trimmed to targeted requirements
// ========================================================================================================================

library(
    base: 'driver', author: 'Krassimir Kossev', category: 'zigbee', description: 'Device Profile Library', name: 'deviceProfileLib', namespace: 'kkossev',
    importUrl: '', version: '3.5.7'
)

public String getDeviceProfile() { return 'TS0601_TZE200_RHGSBACQ' }
public Map getDEVICE() { return deviceProfilesV3['TS0601_TZE200_RHGSBACQ'] }

private Map getPreferencesMapByName(final String param, boolean debug=false) {
    if (!(param in DEVICE?.preferences)) return [:]
    def preference = DEVICE?.preferences["$param"]
    if (preference in [true, false]) return [:]
    if (safeToInt(preference, -1) > 0) {
        int dp = safeToInt(preference)
        return DEVICE?.tuyaDPs.find { it.dp == dp }
    } else {
        return DEVICE?.attributes.find { it.at == preference }
    }
}

public Map getAttributesMap(String attribName, boolean debug=false) {
    Map foundMap = DEVICE?.tuyaDPs?.find { it.name == attribName }
    if (foundMap) return foundMap
    return DEVICE?.attributes?.find { it.name == attribName } ?: [:]
}

public void resetPreferencesToDefaults(boolean debug=false) {
    Map preferences = DEVICE?.preferences ?: [:]
    preferences.each { parName, mapValue ->
        Map parMap = getPreferencesMapByName(parName, false)
        if (parMap?.defVal != null) {
            device.updateSetting("${parMap.name}", [value:parMap.defVal as String, type:parMap.type])
        }
    }
}

public void updateAllPreferences() {
    (DEVICE?.preferences).each { name, dp ->
        Map foundMap = getPreferencesMapByName(name, false)
        if (foundMap) {
            def preferenceValue = settings."${name}"
            if (preferenceValue != null) {
                setPar(name, preferenceValue.toString())
            }
        }
    }
}

int invert(int val) {
    if (settings.invertMotion == true) { return val == 0 ? 1 : 0 }
    else { return val }
}

private def validateAndScaleParameterValue(Map dpMap, String val) {
    def value, scaledValue
    switch (dpMap.type) {
        case 'number':
            value = safeToInt(val, 0)
            scaledValue = dpMap.scale ? (value * dpMap.scale) as Integer : value
            break
        case 'decimal':
            value = safeToDouble(val, 0.0)
            scaledValue = dpMap.scale ? (value * dpMap.scale) as Integer : value
            break
        case 'enum':
            value = scaledValue = safeToInt(val, -1)
            if (scaledValue < 0) {
                String key = dpMap.map.find { it.value == val }?.key
                if (key != null) value = scaledValue = key as Integer
            }
            break
    }
    return scaledValue
}

public boolean setPar(final String parPar=null, final String val=null) {
    if (parPar == null || val == null) return false
    String par = parPar.trim()
    Map dpMap = getPreferencesMapByName(par, false)
    if (!dpMap) return false
    def scaledValue = validateAndScaleParameterValue(dpMap, val as String)
    if (scaledValue == null) return false

    if (dpMap.dp != null) {
        List<String> cmds = sendTuyaParameter(dpMap, par, scaledValue)
        if (cmds) {
            sendZigbeeCommands(cmds)
            return true
        }
    }
    return false
}

public List<String> sendTuyaParameter(Map dpMap, String par, tuyaValue) {
    String dp = zigbee.convertToHexString(dpMap.dp, 2)
    String dpType = dpMap.dt ?: (dpMap.type == 'enum' ? '04' : '02')
    String dpValHex = dpType == '02' ? zigbee.convertToHexString(tuyaValue as int, 8) : zigbee.convertToHexString(tuyaValue as int, 2)
    return sendTuyaCommand(dp, dpType, dpValHex, dpMap.tuyaCmd ?: 0)
}

public boolean processClusterAttributeFromDeviceProfile(final Map descMap) {
    String clusterAttribute = "0x${descMap.cluster}:0x${descMap.attrId}"
    int value = hexStrToUnsignedInt(descMap.value)
    Map foundItem = DEVICE?.attributes?.find { it['at'] == clusterAttribute }
    if (!foundItem) return false
    return processFoundItem(descMap, foundItem, value, false)
}

public boolean processTuyaDPfromDeviceProfile(final Map descMap, final int dp, final int dp_id, final int fncmd_orig, final int dp_len) {
    Map foundItem = DEVICE?.tuyaDPs?.find { it['dp'] == (dp as int) }
    if (!foundItem) return false
    return processFoundItem(descMap, foundItem, fncmd_orig, false)
}

private boolean processFoundItem(final Map descMap, final Map foundItem, int value, boolean doNotTrace = false) {
    if (foundItem == null) return false
    if (foundItem.preProc != null && this.respondsTo(foundItem.preProc)) {
        value = this."${foundItem.preProc}"(value)
    }

    String name = foundItem.name
    String unitText = foundItem.unit ?: ''
    def valueScaled = value

    if (foundItem.type == 'decimal' && foundItem.scale) {
        valueScaled = value / (foundItem.scale as Float)
    } else if (foundItem.type == 'enum' && foundItem.map) {
        valueScaled = foundItem.map[value as int] ?: value
    }

    String descText = "${name} is ${valueScaled} ${unitText}"
    customProcessDeviceProfileEvent(descMap, name, valueScaled, unitText, descText)
    return true
}

public List<String> refreshFromDeviceProfileList() {
    List<String> cmds = []
    if (DEVICE?.tuyaDPs) {
        cmds += queryAllTuyaDP()
    }
    return cmds
}

library(
    base: 'driver', author: 'Krassimir Kossev', category: 'zigbee', description: 'Common ZCL Library', name: 'commonLib', namespace: 'kkossev',
    importUrl: '', version: '4.0.5'
)

public void parse(final String description) {
    if (state.stats != null) { state.stats?.rxCtr = (state.stats?.rxCtr ?: 0) + 1 } else { state.stats = [:] }
    final Map descMap = myParseDescriptionAsMap(description)
    if (!descMap) return

    if (descMap.clusterInt == 0xEF00) {
        standardParseTuyaCluster(descMap)
    } else if (descMap.clusterInt == 0x0402) {
        Float temp = Integer.parseInt(descMap.value, 16) / 100.0f
        handleTemperatureEvent(temp)
    } else if (descMap.clusterInt == 0x0405) {
        Float hum = Integer.parseInt(descMap.value, 16) / 100.0f
        handleHumidityEvent(hum)
    } else if (descMap.clusterInt == 0x0400) {
        int lux = Integer.parseInt(descMap.value, 16)
        handleIlluminanceEvent(lux)
    }
}

Map myParseDescriptionAsMap(String description) {
    try {
        return zigbee.parseDescriptionAsMap(description)
    } catch (e) {
        return [:]
    }
}

public void standardParseTuyaCluster(final Map descMap) {
    if (descMap?.clusterInt == 0xEF00 && (descMap?.command == '01' || descMap?.command == '02')) {
        int dataLen = descMap?.data.size()
        for (int i = 0; i < (dataLen - 4); ) {
            int dp = zigbee.convertHexToInt(descMap?.data[2 + i])
            int dp_id = zigbee.convertHexToInt(descMap?.data[3 + i])
            int fncmd_len = zigbee.convertHexToInt(descMap?.data[5 + i])
            int fncmd = getTuyaAttributeValue(descMap?.data, i)
            customProcessTuyaDp(descMap, dp, dp_id, fncmd, fncmd_len)
            i = i + fncmd_len + 4
        }
    }
}

public int getTuyaAttributeValue(final List<String> _data, final int index) {
    int retValue = 0
    if (_data.size() >= 6) {
        int dataLength = zigbee.convertHexToInt(_data[5 + index])
        if (dataLength == 0) return 0
        int power = 1
        for (i in dataLength..1) {
            retValue = retValue + power * zigbee.convertHexToInt(_data[index + i + 5])
            power = power * 256
        }
    }
    return retValue
}

public List<String> sendTuyaCommand(String dp, String dp_type, String fncmd, int tuyaCmdDefault = 0) {
    return zigbee.command(0xEF00, tuyaCmdDefault, [destEndpoint: 1], 200, "0001" + dp + dp_type + zigbee.convertToHexString((int)(fncmd.length() / 2), 4) + fncmd)
}

public List<String> queryAllTuyaDP() {
    return zigbee.command(0xEF00, 0x03)
}

public void sendZigbeeCommands(List<String> cmds) {
    sendHubCommand(new HubMultiAction(cmds, Protocol.ZIGBEE))
}

void handleMotion(boolean active) {
    String val = active ? 'active' : 'inactive'
    sendEvent(name: 'motion', value: val, descriptionText: "Motion is ${val}")
}

void handleTemperatureEvent(Float temp) {
    sendEvent(name: 'temperature', value: temp, unit: '°C', descriptionText: "Temperature is ${temp} °C")
}

void handleHumidityEvent(Float hum) {
    sendEvent(name: 'humidity', value: hum, unit: '%RH', descriptionText: "Humidity is ${hum} %RH")
}

void handleIlluminanceEvent(int lux) {
    sendEvent(name: 'illuminance', value: lux, unit: 'lx', descriptionText: "Illuminance is ${lux} lx")
}

void handleTuyaBatteryLevel(int batt) {
    sendEvent(name: 'battery', value: batt, unit: '%', descriptionText: "Battery level is ${batt} %")
}

int safeToInt(val, defaultVal=0) {
    try { return val.toInteger() } catch (e) { return defaultVal }
}

Float safeToDouble(val, defaultVal=0.0) {
    try { return val.toFloat() } catch (e) { return defaultVal }
}

int hexStrToUnsignedInt(String hex) {
    return Integer.parseInt(hex, 16)
}

String unix2formattedDate(long time) {
    return new Date(time).format("yyyy-MM-dd HH:mm:ss")
}

void logDebug(String msg) { if (settings.logEnable) log.debug msg }
void logInfo(String msg) { if (settings.txtEnable) log.info msg }
void logWarn(String msg) { log.warn msg }
void logTrace(String msg) { if (_TRACE_ALL) log.trace msg }