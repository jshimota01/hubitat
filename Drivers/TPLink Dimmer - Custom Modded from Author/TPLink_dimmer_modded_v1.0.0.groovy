/**
 * TpLink Dimmer (Custom Health Check Refactor)
 * Device Driver for Hubitat Elevation
 *
 * Purpose:
 * Custom driver refactor for TP-Link TAPO/Kasa dimmer switches integrating 
 * phase-anchored custom Health Check polling to ensure proper lastActivity 
 * updates while running independently of parent app update constraints.
 *
 * Notes:
 * Custom Health Check Implementation
 * - Intentionally NOT using Hubitat's native 'Health Check' capability.
 * - Hubitat's native capability exposes an unwanted "Ping" UI control button
 *   and does not provide the phase-anchored scheduling, timeout guards, or trace 
 *   logging behavior required by this driver architecture.
 **/
/**
 * Copyright 2026 James Shimota
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
/**
 * Changelog:
 * v1.0.0    09/14/26    jshimota    Initial refactor integrating custom Health Check architecture into DaveGut TP-Link Dimmer driver.
 **/

/*	TP-Link TAPO plug, switches, lights, hub, and hub sensors.
		Copyright Dave Gutheinz
License:  https://github.com/DaveGut/HubitatActive/blob/master/KasaDevices/License.md
=================================================================================================*/
import groovy.transform.Field

metadata {
	definition (name: "TpLink Dimmer", namespace: nameSpace(), author: "Dave Gutheinz", 
				singleThreaded: true,
				importUrl: "https://raw.githubusercontent.com/DaveGut/tpLink_Hubitat/main/Drivers/tpLink_dimmer.groovy")
	{
		capability "Light"
		
		// Attributes
		attribute "healthStatus", "enum", ["unknown", "offline", "online"]

		// Custom Commands
		command "Health Check"
		command "resetDriver"
	}
	preferences {
		commonPreferences()
		
		input name: "HealthCheckInterval", type: "enum", title: "<b>Health Check Interval</b>", options: HealthCheckIntervalOpts.options, defaultValue: HealthCheckIntervalOpts.defaultValue, description: "<i>Changes how often the driver executes a Health Check to verify device online status.<br><b>Note:</b> This is a custom driver routine and is NOT the native Hubitat Elevation platform Health Check service.</i>"
		
		// Independent Logging Switches
		input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", description: "Enable to output normal activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", description: "Enable to output error activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", description: "Enable to output warning activity to log<br>Default: <b>On</b>", defaultValue: true, required: true
		input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", description: "Enable to output debugging activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
		input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", description: "Enable to output tracing activity to log<br>Default: <b>Off</b>", defaultValue: false, required: true
	}
}

def installed() {
	Map logData = [method: "installed", commonInstalled: commonInstalled()]
	state.eventType = "digital"
	logInfo(logData)
	
	initializeHealthCheckPhase()
	sendEvent(name: "healthStatus", value: "unknown")
	initialize(true)
}

def updated() {
	Map logData = [method: "updated", commonUpdated: commonUpdated()]
	logInfo(logData)
	initialize(false)
}

private void initialize(Boolean isInstall = false) {
	unschedule("disableDebugLogging")

	if (device.currentValue("healthStatus") == null) {
		sendEvent(name: "healthStatus", value: "unknown")
	}

	final int interval = settings.HealthCheckInterval != null ? settings.HealthCheckInterval.toInteger() : 480
	if (interval > 0) {
		scheduleHealthCheck("executeHealthCheckScheduled", interval)
	} else {
		unschedule("executeHealthCheckScheduled")
	}

	if (isInstall) {
		device.updateSetting("logDebugEnable", [type: "bool", value: true])
		logInfo("Debug logging enabled for 30 minutes.")
		runIn(1800, "disableDebugLogging")
	} else if (getSettingBool("logDebugEnable", false)) {
		logInfo("Debug logging enabled. Will automatically turn off in 30 minutes.")
		runIn(1800, "disableDebugLogging", [overwrite: false])
	} else {
		unschedule("disableDebugLogging")
	}
}

def parse_get_device_info(result, data) {
	switchParse(result)
	levelParse(result)
}

/* =========================================================================================
   CUSTOM HEALTH CHECK ARCHITECTURE
   ========================================================================================= */

List<String> "Health Check"() {
	executeHealthCheck()
	return []
}

void executeHealthCheckScheduled() {
	executeHealthCheck()
}

private void executeHealthCheck() {
	logDebug("Executing Health Check via refresh...")
	state.healthCheckPending = true
	scheduleCommandTimeoutCheck()
	refresh()
}

private void markHealthCheckSuccess() {
	if (state.healthCheckPending == true) {
		logDebug("Valid Health Check response verified")
		state.healthCheckPending = false
		unschedule("deviceCommandTimeout")
	}
	
	sendEvent(
		name: "healthStatus", 
		value: "online", 
		isStateChange: false, 
		descriptionText: "${device.displayName} health check verified online"
	)
}

private void initializeHealthCheckPhase() {
	if (state.healthCheckStartHour == null) state.healthCheckStartHour = new Random().nextInt(24)
	if (state.healthCheckStartMinute == null) state.healthCheckStartMinute = new Random().nextInt(60)
}

private void scheduleHealthCheck(String methodToSchedule, int intervalMin) {
	unschedule(methodToSchedule)
	initializeHealthCheckPhase()

	final int h = state.healthCheckStartHour as Integer
	final int m = state.healthCheckStartMinute as Integer

	logInfo("Scheduling Health Check every ${intervalMin} minutes anchored at ${String.format('%02d:%02d', h, m)} daily")

	switch (intervalMin) {
		case 60:
			schedule("0 ${m} * ? * * *", methodToSchedule)
			break
		case 240:
			String h4 = [0, 4, 8, 12, 16, 20].collect { (it + h) % 24 }.sort().join(",")
			schedule("0 ${m} ${h4} ? * * *", methodToSchedule)
			break
		case 480:
			String h8 = [0, 8, 16].collect { (it + h) % 24 }.sort().join(",")
			schedule("0 ${m} ${h8} ? * * *", methodToSchedule)
			break
		case 720:
			String h12 = [0, 12].collect { (it + h) % 24 }.sort().join(",")
			schedule("0 ${m} ${h12} ? * * *", methodToSchedule)
			break
		case 1440:
			schedule("0 ${m} ${h} ? * * *", methodToSchedule)
			break
		default:
			if (intervalMin >= 60) {
				int hours = intervalMin / 60
				schedule("0 ${m} */${hours} ? * * *", methodToSchedule)
			} else {
				schedule("0 */${intervalMin} * ? * * *", methodToSchedule)
			}
			break
	}
}

private void scheduleCommandTimeoutCheck(final int delay = COMMAND_TIMEOUT) {
	runIn(delay, "deviceCommandTimeout", [overwrite: true])
}

void deviceCommandTimeout() {
	logWarn("No Health Check response received (device offline?)")
	state.healthCheckPending = false
	updateAttribute("healthStatus", "offline")
}

void disableDebugLogging() {
	if (getSettingBool("logDebugEnable", false)) {
		logWarn("30 minutes have elapsed. Automatically disabling debug logging.")
		device.updateSetting("logDebugEnable", [type: "bool", value: false])
	}
}

void resetDriver() {
	logInfo("Starting full driver reset...")
	
	Object savedHour = state.healthCheckStartHour
	Object savedMinute = state.healthCheckStartMinute

	unschedule()
	state.clear()
	device.properties.supportedAttributes.each { device.deleteCurrentState("$it") }

	if (savedHour != null) state.healthCheckStartHour = savedHour
	if (savedMinute != null) state.healthCheckStartMinute = savedMinute

	initialize(false)
	logInfo("Driver reset process completed and re-initialized.")
}

private void updateAttribute(final String attribute, final Object value, final String unit = null, final String type = null) {
	final String currentVal = device.currentValue(attribute)?.toString()
	if (currentVal == value?.toString()) return

	final String descriptionText = "${device.displayName} - ${attribute} was set to ${value}${unit ?: ''}"
	logInfo(descriptionText)
	sendEvent(name: attribute, value: value, unit: unit, type: type, descriptionText: descriptionText)
}

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
	return settings[key] != null ? settings[key] as Boolean : defaultVal
}

@Field static final Map HealthCheckIntervalOpts = [
	defaultValue: 480,
	options: [ 60: "Every Hour", 240: "Every 4 Hours", 480: "Every 8 Hours", 720: "Every 12 Hours", 1440: "Every 24 Hours", 0: "Disabled" ]
]

@Field static final int COMMAND_TIMEOUT = 10


// ~~~~~ start include (392) davegut.tpLinkCapConfiguration ~~~~~
library (
	name: "tpLinkCapConfiguration",
	namespace: "davegut",
	author: "Compied by Dave Gutheinz",
	description: "Hubitat capability Configuration.",
	category: "utilities",
	documentationLink: ""
)

capability "Configuration"

def configure() {
	String devIp = getDataValue("devIp")
	Map logData = [method: "configure", devIp: devIp]
	def cmdData = "0200000101e51100095c11706d6f58577b22706172616d73223a7b227273615f6b6579223a222d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d5c6e4d494942496a414e42676b71686b6947397730424151454641414f43415138414d49494243674b43415145416d684655445279687367797073467936576c4d385c6e54646154397a61586133586a3042712f4d6f484971696d586e2b736b4e48584d525a6550564134627532416257386d79744a5033445073665173795679536e355c6e6f425841674d303149674d4f46736350316258367679784d523871614b33746e466361665a4653684d79536e31752f564f2f47474f795436507459716f384e315c6e44714d77373563334b5a4952387a4c71516f744657747239543337536e50754a7051555a7055376679574b676377716e7338785a657a78734e6a6465534171765c6e3167574e75436a5356686d437931564d49514942576d616a37414c47544971596a5442376d645348562f2b614a32564467424c6d7770344c7131664c4f6a466f5c6e33737241683144744a6b537376376a624f584d51695666453873764b6877586177717661546b5658382f7a4f44592b2f64684f5374694a4e6c466556636c35585c6e4a514944415141425c6e2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d5c6e227d7d"
	try {
		if (getDataValue("power")) {
			sendFindCmd(devIp, "20004", cmdData, "configure2", 5)
		} else {
			sendFindCmd(devIp, "20002", cmdData, "configure2", 5)
		}
		logInfo(logData)
	} catch (err) {
		def parentChecked = parent.tpLinkCheckForDevices(5)
		logData << [status: "FAILED", error: err, parentChecked: parentChecked]
		logWarn(logData)
		configure3()
	}
	
	executeHealthCheck()
}

def configure2(response) {
	Map logData = [method: "configure2"]
	def respData = parseLanMessage(response)
	String hubDni = device.getDeviceNetworkId()
	logData << [dni: respData.mac, hubDni: hubDni]
	def parentChecked = false
	if (respData.mac != hubDni) {
		logData << [status: "device/ip not found", action: "parentCheck",
				    parentChecked: parent.tpLinkCheckForDevices(5)]
	} else {
		logData << [status: "device/ip found"]
	}
	configure3()
	logInfo(logData)
}

def configure3() {
	Map logData = [method: "configure3"]
	logData <<[updateDeviceData: updateDeviceData(true)]
	logData << [deviceHandshake: deviceHandshake()]
	if (getDataValue("protocol") != "camera") {
		runEvery3Hours("deviceHandshake")
		logData << [handshakeInterval: "3 Hours"]
	}
	runIn(5, refresh)
	logInfo(logData)
}

def deviceHandshake() {
	def protocol = getDataValue("protocol")
	Map logData = [method: "deviceHandshake", protocol: protocol]
	if (protocol == "KLAP") {
		klapHandshake(getDataValue("baseUrl"), parent.localHash)
	} else if (protocol == "camera") {
		Map hsInput = [url: getDataValue("baseUrl"), user: parent.userName,
					   pwd: parent.encPasswordCam]
		cameraHandshake(hsInput)
	} else if (protocol == "AES") {
		aesHandshake()
	} else if (protocol == "vacAes") {
		vacAesHandshake(getDataValue("baseUrl"), parent.userName, parent.encPasswordVac)
	} else {
		logData << [ERROR: "Protocol not supported"]
		logWarn(logData)
	}
	return logData
}

// ~~~~~ end include (392) davegut.tpLinkCapConfiguration ~~~~~

// ~~~~~ start include (381) davegut.tpLinkCapSwitch ~~~~~
library (
	name: "tpLinkCapSwitch",
	namespace: "davegut",
	author: "Compied by Dave Gutheinz",
	description: "Hubitat capability Switch methods",
	category: "utilities",
	documentationLink: ""
)

capability "Switch"

def on() { setPower(true) }

def off() { setPower(false) }

def setPower(onOff) {
	state.eventType = "digital"
	logDebug("setPower: [device_on: ${onOff}]")
	List requests = [[
		method: "set_device_info",
		params: [device_on: onOff]]]
	requests << [method: "get_device_info"]
	sendDevCmd(requests, "setPower", "parseUpdates")
}

def switchParse(result) {
	Map logData = [method: "switchParse"]
	if (result.device_on != null) {
		def onOff = "off"
		if (result.device_on == true) { onOff = "on" }
		if (device.currentValue("switch") != onOff) {
			sendEvent(name: "switch", value: onOff, type: state.eventType)
			if (getDataValue("isEm") == "true" && !pollInterval.contains("sec")) {
				runIn(4, refresh)
			}
		}
		state.eventType = "physical"
		logData << [switch: onOff]
	}
	logDebug(logData)
}

// ~~~~~ end include (381) davegut.tpLinkCapSwitch ~~~~~

// ~~~~~ start include (382) davegut.tpLinkCapSwitchLevel ~~~~~
library (
	name: "tpLinkCapSwitchLevel",
	namespace: "davegut",
	author: "Compied by Dave Gutheinz",
	description: "Hubitat capability Switch Level and Change Level methods",
	category: "utilities",
	documentationLink: ""
)

capability "Switch Level"
capability "Change Level"

def setLevel(level, transTime=0) {
	state.eventType = "digital"
	Map logData = [method: "setLevel", level: level, transTime: transTime]
	if (level > 100 || level < 0 || level == null) {
		logData << [error: "level out of range"]
		logWarn(logData)
		return
	}
	if (level == 0) {
		off()
	} else if (transTime > 0) {
		execLevelTrans(level, transTime)
	} else {
		List requests = [[method: "set_device_info",
						  params: [brightness: level]]]
		requests << [method: "get_device_info"]
		sendDevCmd(requests, "setLevel", "parseUpdates")
	}
	logDebug(logData)
}

def execLevelTrans(targetLevel, transTime) {
	Map logData = [method: "execLevelTrans", targetLevel: targetLevel,
				   transTime: transTime]
	def currLevel = device.currentValue("level")
	logData << [currLevel: currLevel]
	if (device.currentValue("switch") == "off") { currLevel = 0 }
	if (targetLevel == currLevel) {
		logData << [ERROR:  "no level change"]
		logWarn(logData)
		return
	}
	if (transTime > 10) {
		transTime = 10
		logData << [transTimeChange: transTime]
	}
	def levelChange = targetLevel - currLevel
	def deltaLevel = (0.99 + (levelChange / (4 * transTime))).toInteger()
	if(levelChange < 0) {
		deltaLevel = (-0.99 + (levelChange / (4 * transTime))).toInteger()
	}
	def incrs = (0.99 + (levelChange / deltaLevel)).toInteger()
	def cmdDelay = (1000 * transTime / incrs).toInteger() - 10
	logData << [incrs: incrs, cmdDelay: cmdDelay]
	def startTime = now()
	def newLevel = currLevel
	if (targetLevel < currLevel) {
		while(targetLevel < newLevel) {
			newLevel = setNewLevel(targetLevel, currLevel, newLevel, deltaLevel)
			sendTransCmd(newLevel)
			pauseExecution(cmdDelay)
		}		
	} else if (targetLevel > currLevel) {
		while(targetLevel > newLevel) {
			newLevel = setNewLevel(targetLevel, currLevel, newLevel, deltaLevel)
			sendTransCmd(newLevel)
			pauseExecution(cmdDelay)
		}
	}
	def execTime = now() - startTime
	runIn(3, setLevel, [data: targetLevel])
	logData << [execTime: execTime]
	logDebug(logData)
}

def setNewLevel(targetLevel, currLevel, newLevel, deltaLevel) {
	newLevel += deltaLevel
	if ((targetLevel < currLevel && newLevel < targetLevel) ||
		(targetLevel > currLevel && newLevel > targetLevel)) {
		newLevel = targetLevel
	}
	return newLevel
}

def sendTransCmd(newLevel) {
		List requests = [[method: "set_device_info",
						  params: [
							  brightness: newLevel]]]
			sendDevCmd(requests, "sendTransCmd", "")
}

def startLevelChange(direction) {
	logDebug("startLevelChange: [level: ${device.currentValue("level")}, direction: ${direction}]")
	if (direction == "up") { levelUp() }
	else { levelDown() }
}

def stopLevelChange() {
	logDebug("stopLevelChange: [level: ${device.currentValue("level")}]")
	unschedule(levelUp)
	unschedule(levelDown)
}

def levelUp() {
	def curLevel = device.currentValue("level").toInteger()
	if (curLevel != 100) {
		def newLevel = curLevel + 4
		if (newLevel > 100) { newLevel = 100 }
		setLevel(newLevel)
		runIn(1, levelUp)
	}
}

def levelDown() {
	def curLevel = device.currentValue("level").toInteger()
	if (device.currentValue("switch") == "on") {
		def newLevel = curLevel - 4
		if (newLevel <= 0) { off() }
		else {
			setLevel(newLevel)
			runIn(1, levelDown)
		}
	}
}

def levelParse(result) {
	Map logData = [method: "levelParse"]
	if (device.currentValue("level") != result.brightness) {
		sendEvent(name: "level", value: result.brightness, type: state.eventType)
	}
	state.eventType = "physical"
	logData << [level: result.brightness]
	logDebug(logData)
}

// ~~~~~ end include (382) davegut.tpLinkCapSwitchLevel ~~~~~

// ~~~~~ start include (384) davegut.tpLinkCommon ~~~~~
library (
	name: "tpLinkCommon",
	namespace: "davegut",
	author: "Compied by Dave Gutheinz",
	description: "Common driver methods including capability Refresh and Configuration methods",
	category: "utilities",
	documentationLink: ""
)

capability "Refresh"
attribute "commsError", "string"

def commonPreferences() {
	List pollOptions = ["5 sec", "10 sec", "1 min", "5 min", "15 min", "30 min"]
	input ("pollInterval", "enum", title: "Poll Interval",
		   options: pollOptions, defaultValue: "30 min")
	if (getDataValue("hasLed") == "true") {
		input ("ledRule", "enum", title: "LED Mode (if night mode, set type and times in phone app)",
			   options: ["always", "never", "night_mode"], defaultValue: "always")
	}
	input ("rebootDev", "bool", title: "Reboot Device", defaultValue: false)
	input ("syncName", "enum", title: "Update Device Names and Labels", 
		   options: ["hubMaster", "tapoAppMaster", "notSet"], defaultValue: "notSet")
	input ("logEnable", "bool",  title: "Enable debug logging for 30 minutes", defaultValue: false)
	input ("infoLog", "bool", title: "Enable information logging",defaultValue: true)
}

def commonInstalled() {
	Map logData = [method: "commonInstalled"]
	updateAttr("commsError", "false")
	state.errorCount = 0
	logData << [configure: configure()]
	return logData
}

def commonUpdated() {
	unschedule()
	sendEvent(name: "commsError", value: "false")
	state.errorCount = 0
	Map logData = [commsError: "cleared"]
	if (rebootDev == true) {
		List requests = [[method: "device_reboot"]]
		sendDevCmd(requests, "rebootDevice", "parseUpdates")
		logData << [rebootDevice: "device reboot being attempted"]
	} else {
		runEvery3Hours(deviceHandshake)
		logData << [handshakeInterval: "3 Hours"]
		logData << [pollInterval: setPollInterval()]
		logData << [logging: setLogsOff()]
		logData << [updateDevSettings: updDevSettings()]
		runIn(2, refresh)
		if (getDataValue("isEm") == "true") {
			runIn(7, emUpdated)
		}
	}
	return logData
}

def finishReboot(respData) {
	Map logData = [method: "finishReboot", respData: respData]
	logData << [wait: "<b>20s for device to reconnect to LAN</b>", action: "executing deviceHandshake"]
	device.updateSetting("rebootDev",[type:"bool", value: false])
	runIn(20, configure)
	logDebug(logData)
}

def updDevSettings() {
	List requests = []
	if (syncName == "hubMaster") {
		String nickname = device.getLabel().bytes.encodeBase64().toString()
		requests << [method: "set_device_info", params: [nickname: nickname]]
	}
	if (ledRule) {
		requests << [method: "get_led_info"]
	}
	if (getDataValue("isEm") == "true") {
		requests << [method: "get_energy_usage"]
	}
	requests << [method: "get_device_info"]
	sendDevCmd(requests, "updateDevSettings", "parseUpdates")
	return "Updated"
}

def setPollInterval(pInterval = pollInterval) {
	if (pInterval.contains("sec")) {
		logWarn("<b>Poll intervals of less than 1 minute may overload the Hub</b>")
		def interval = pInterval.replace(" sec", "").toInteger()
		def start = Math.round((interval-1) * Math.random()).toInteger()
		schedule("${start}/${interval} * * * * ?", "refresh")
	} else {
		def interval= pInterval.replace(" min", "").toInteger()
		def start = Math.round(59 * Math.random()).toInteger()
		schedule("${start} */${interval} * * * ?", "refresh")
	}
	return pInterval
}

//	===== Data Distribution (and parse) =====
def parseUpdates(resp, data = null) {
	Map logData = [method: "parseUpdates", data: data]
	def respData = parseData(resp, getDataValue("protocol"), data)
	if (resp.status == 200 && respData.cryptoStatus == "OK") {
		markHealthCheckSuccess()
		def cmdResp = respData.cmdResp.result.responses
		if (respData.cmdResp.result.responses != null) {
			respData.cmdResp.result.responses.each {
				if (it.error_code == 0) {
					distGetData(it, data)
				} else {
					logData << ["${it.method}": [status: "cmdFailed", data: it]]
					logDebug(logData)
				}
			}
		}
		if (respData.cmdResp.result.responseData != null) {
			respData.cmdResp.result.responseData.result.responses.each {
				if (it.error_code == 0) {
					distChildGetData(it, data)
				} else {
					logData << ["${it.method}": [status: "cmdFailed", data: it]]
					logDebug(logData)
				}
			}
		}
	} else {
		logData << [errorMsg: "Misc Error"]
		logDebug(logData)
	}
}

def distGetData(devResp, data) {
	switch(devResp.method) {
		case "get_device_info":
			parse_get_device_info(devResp.result, data)
			parseNameUpdate(devResp.result)
			break
		case "get_current_power":
			parse_get_current_power(devResp.result, data)
			break
		case "get_device_usage":
			parse_get_device_usage(devResp.result, data)
			break
		case "get_child_device_list":
			parse_get_child_device_list(devResp.result, data)
			break
		case "get_alarm_configure":
			parse_get_alarm_configure(devResp.result, data)
			break
		case "get_led_info":
			parse_get_led_info(devResp.result, data)
			break
		case "device_reboot":
			finishReboot(devResp)
			break
		case "getBatteryInfo":
			parse_getBatteryInfo(devResp.result, data)
			break
		case "getCleanNumber":
			parse_getCleanNumber(devResp.result, data)
			break
		case "getSwitchClean":
			parse_getSwitchClean(devResp, data)
			break
		case "getMopState":
			parse_getMopState(devResp, data)
			break
		case "getSwitchCharge":
			updateAttr("docking", devResp.switch_charge, data)
			break
		case "getVacStatus":
			parse_getVacStatus(devResp.result, data)
			break
		case "getMapInfo":
			parse_getMapInfo(devResp.result, data)
			break
		case "getMapData":
			parse_getMapData(devResp.result, data)
			break
		case "getLastAlarmInfo": 
			parse_getLastAlarmInfo(devResp.result, data)
			break
		default:
			if (!devResp.method.contains("set_")) {
				Map logData = [method: "distGetData", data: data,
							   devMethod: devResp.method, status: "unprocessed"]
				logDebug(logData)
			}
	}
}

def parse_get_led_info(result, data) {
	Map logData = [method: "parse_get_led_info", data: data]
	if (ledRule != result.led_rule) {
		Map request = [
			method: "set_led_info",
			params: [
				led_rule: ledRule,
				night_mode: [
					night_mode_type: result.night_mode.night_mode_type,
					sunrise_offset: result.night_mode.sunrise_offset, 
					sunset_offset:result.night_mode.sunset_offset,
					start_time: result.night_mode.start_time,
					end_time: result.night_mode.end_time
				]]]
		asyncSend(request, "delayedUpdates", "parseUpdates")
		device.updateSetting("ledRule", [type:"enum", value: ledRule])
		logData << [status: "updatingLedRule"]
	}
	logData << [ledRule: ledRule]
	logDebug(logData)
}

def parseNameUpdate(result) {
	if (syncName != "notSet") {
		Map logData = [method: "parseNameUpdate"]
		byte[] plainBytes = result.nickname.decodeBase64()
		def newLabel = new String(plainBytes)
		device.setLabel(newLabel)
		device.updateSetting("syncName",[type:"enum", value: "notSet"])
		logData << [label: newLabel]
		logDebug(logData)
	}
}

//	===== Capability Refresh =====
def refresh() {
	def type = getDataValue("type")
	List requests = [[method: "get_device_info"]]
	if (type == "Hub" || type == "Parent") {
		requests << [method:"get_child_device_list"]
	}
	if (getDataValue("isEm") == "true") {
		requests << [method: "get_current_power"]
	}
	if (type == "Robovac") {
		requests = [[method: "getBatteryInfo"],
					[method: "getCleanNumber"],
					[method: "getSwitchClean"],
					[method: "getVacStatus"],
					[method: "getMopState"],
					[method: "getSwitchCharge"]]
	}
	sendDevCmd(requests, "refresh", "parseUpdates")
}

def plugEmRefresh() { refresh() }
def parentRefresh() { refresh() }
def minRefresh() { refresh() }

def sendDevCmd(requests, data, action) {
	Map cmdBody = [
		method: "multipleRequest",
		params: [requests: requests]]
	asyncSend(cmdBody, data, action)
}

def nullParse(resp, data) { }

//	===== Check/Update device data =====
def updateDeviceData(fromConfig = false) {
	def devData = parent.getDeviceData(device.getDeviceNetworkId())
	updateChild(devData, fromConfig)
	return [updateDeviceData: "updating with app data"]
}

def updateChild(devData, fromConfig = false) {
	def currVersion = getDataValue("version")
	Map logData = [method: "updateChild", devData: devData]
	if (devData != null) {
		devData.each {
			if (it.key != "deviceType" && it.key != "model" && it.key != "alias") {
				updateDataValue(it.key, it.value.toString())
			}
		}
		if (currVersion != version()) {
			updateDataValue("version", version())
			logData << [updateVersion: version()]
			runIn(20, updated)
		}
	} else {
		logData << [Note: "DEVICE DATA IS NULL"]
	}
	if (!fromConfig) { deviceHandshake() }
	logInfo(logData)
}

// ~~~~~ end include (384) davegut.tpLinkCommon ~~~~~

// ~~~~~ start include (385) davegut.tpLinkComms ~~~~~
library (
	name: "tpLinkComms",
	namespace: "davegut",
	author: "Compiled by Dave Gutheinz",
	description: "Communication methods for TP-Link Integration",
	category: "utilities",
	documentationLink: ""
)
import org.json.JSONObject
import groovy.json.JsonOutput
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

def asyncSend(cmdBody, reqData, action) {
	Map cmdData = [cmdBody: cmdBody, reqData: reqData, action: action]
	def protocol = getDataValue("protocol")
	Map reqParams = [:]
	if (protocol == "KLAP") {
		reqParams = getKlapParams(cmdBody)
	} else if (protocol == "camera") {
		reqParams = getCameraParams(cmdBody, reqData)
	} else if (protocol == "AES") {
		reqParams = getAesParams(cmdBody)
	} else if (protocol == "vacAes") {
		reqParams = getVacAesParams(cmdBody, "${getDataValue("baseUrl")}/?token=${token}")
	}
	if (reqParams != [:]) {
		if (state.errorCount == 0) { state.lastCommand = cmdData }
		asynchttpPost(action, reqParams, [data: reqData])
		logDebug([method: "asyncSend", reqData: reqData])
	} else {
		unknownProt(reqData)
	}
}

def unknownProt(reqData) {
	Map warnData = ["<b>UnknownProtocol</b>": [data: reqData,
				    msg: "Device will not install or if installed will not work"]]
	logWarn(warnData)
}

def parseData(resp, protocol = getDataValue("protocol"), data = null) {
	Map logData = [method: "parseData", status: resp.status, protocol: protocol,
				   sourceMethod: data.data]
	def message = "OK"
	if (resp.status == 200) {
		if (protocol == "KLAP") {
			logData << parseKlapData(resp, data)
		} else if (protocol == "AES") {
			logData << parseAesData(resp, data)
		} else if (protocol == "vacAes") {
			logData << parseVacAesData(resp, data)
		} else if (protocol == "camera") {
			logData << parseCameraData(resp, data)
		}
	} else {
		message = resp.errorMessage
		String userMessage = "unspecified"
		if (resp.status == 403) {
			userMessage = "<b>Try again. If error persists, check your credentials</b>"
		} else if (resp.status == 408) {
			userMessage = "<b>Your router connection to ${getDataValue("baseUrl")} failed.  Run Configure.</b>"
		} else {
			userMessage = "<b>Unhandled error Lan return</b>"
		}
		logData << [respMessage: message, userMessage: userMessage]
		logDebug(logData)
	}
	handleCommsError(resp.status, message)
	return logData
}

private sendFindCmd(ip, port, cmdData, action, commsTo = 5, ignore = false) {
	def myHubAction = new hubitat.device.HubAction(
		cmdData,
		hubitat.device.Protocol.LAN,
		[type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
		 destinationAddress: "${ip}:${port}",
		 encoding: hubitat.device.HubAction.Encoding.HEX_STRING,
		 ignoreResponse: ignore,
		 parseWarning: true,
		 timeout: commsTo,
		 callback: action])
	try {
		sendHubCommand(myHubAction)
	} catch (error) {
		logWarn("sendLanCmd: command to ${ip}:${port} failed. Error = ${error}")
	}
	return
}

def handleCommsError(status, msg = "") {
	if (status == 200) {
		setCommsError(status, "OK")
	} else {
		Map logData = [method: "handleCommsError", status: code, msg: msg]
		def count = state.errorCount + 1
		logData << [count: count, status: status, msg: msg]
		switch(count) {
			case 1:
			case 2:
				runIn(1, delayedPassThrough)
				break
			case 3:
				if (status == 403) {
					logData << [action: "attemptLogin"]
					deviceHandshake()
					runIn(4, delayedPassThrough)
				} else {
					logData << [action: "Find on LAN then login"]
					configure()
					runIn(10, delayedPassThrough)
				}
				break
			case 4:
				runIn(1, delayedPassThrough)
				break
			default:
				logData << [action: "SetCommsErrorTrue"]
				setCommsError(status, msg, 5)
		}
		state.errorCount = count
		logInfo(logData)
	}
}

def delayedPassThrough() {
	def cmdData = new JSONObject(state.lastCommand)
	def cmdBody = parseJson(cmdData.cmdBody.toString())
	asyncSend(cmdBody, cmdData.reqData, cmdData.action)
}

def setCommsError(status, msg = "OK", count = state.commsError) {
	Map logData = [method: "setCommsError", status: status, errorMsg: msg, count: count]
	if (device && status == 200) {
		state.errorCount = 0
		if (device.currentValue("commsError") == "true") {
			sendEvent(name: "commsError", value: "false")
			setPollInterval()
			unschedule("errorConfigure")
			logInfo(logData)
		}
	} else if (device) {
		if (device.currentValue("commsError") == "false" && count > 4) {
			updateAttr("commsError", "true")
			setPollInterval("30 min")
			runEvery10Minutes(errorConfigure)
			logData << [pollInterval: "30 Min", errorConfigure: "ever 10 min"]
			logWarn(logData)
			if (status == 403) {
				logWarn(logInErrorAction())
			} else {
				logWarn(lanErrorAction())
			}
		} else {
			logData << [error: "Unspecified Error"]
			logWarn(logData)
		}
	}
}

def errorConfigure() {
	logDebug([method: "errorConfigure"])
	if (device.currentValue("commsError") == "true") {
		configure()
	} else {
		unschedule("errorConfigure")
	}
}

def lanErrorAction() {
	def action = "Likely cause of this error is YOUR LAN device configuration: "
	action += "a. VERIFY your device is on the DHCP list in your router, "
	action += "b. VERIFY your device is in the active device list in your router, and "
	action += "c. TRY controlling your device from the TAPO phone app."
	return action
}

def logInErrorAction() {
	def action = "Likely cause is your login credentials are incorrect or the login has expired. "
	action += "a. RUN command Configure. b. If error persists, check your credentials in the App"
	return action
}

// ~~~~~ end include (385) davegut.tpLinkComms ~~~~~

// ~~~~~ start include (386) davegut.tpLinkCrypto ~~~~~
library (
	name: "tpLinkCrypto",
	namespace: "davegut",
	author: "Compiled by Dave Gutheinz",
	description: "Handshake methods for TP-Link Integration",
	category: "utilities",
	documentationLink: ""
)
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import java.security.KeyFactory
import java.util.Random
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.security.MessageDigest

def klapEncrypt(byte[] request, encKey, encIv, encSig, seqNo) {
	byte[] encSeqNo = integerToByteArray(seqNo)
	byte[] ivEnc = [encIv, encSeqNo].flatten()
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
	SecretKeySpec key = new SecretKeySpec(encKey, "AES")
	IvParameterSpec iv = new IvParameterSpec(ivEnc)
	cipher.init(Cipher.ENCRYPT_MODE, key, iv)
	byte[] cipherRequest = cipher.doFinal(request)

	byte[] payload = [encSig, encSeqNo, cipherRequest].flatten()
	byte[] signature = mdEncode("SHA-256", payload)
	cipherRequest = [signature, cipherRequest].flatten()
	return [cipherData: cipherRequest, seqNumber: seqNo]
}

def klapDecrypt(cipherResponse, encKey, encIv, seqNo) {
	byte[] encSeqNo = integerToByteArray(seqNo)
	byte[] ivEnc = [encIv, encSeqNo].flatten()
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    SecretKeySpec key = new SecretKeySpec(encKey, "AES")
	IvParameterSpec iv = new IvParameterSpec(ivEnc)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
	byte[] byteResponse = cipher.doFinal(cipherResponse)
	return new String(byteResponse, "UTF-8")
}

def aesEncrypt(request, encKey, encIv) {
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
	SecretKeySpec key = new SecretKeySpec(encKey, "AES")
	IvParameterSpec iv = new IvParameterSpec(encIv)
	cipher.init(Cipher.ENCRYPT_MODE, key, iv)
	String result = cipher.doFinal(request.getBytes("UTF-8")).encodeBase64().toString()
	return result.replace("\r\n","")
}

def aesDecrypt(cipherResponse, encKey, encIv) {
    byte[] decodedBytes = cipherResponse.decodeBase64()
	def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    SecretKeySpec key = new SecretKeySpec(encKey, "AES")
	IvParameterSpec iv = new IvParameterSpec(encIv)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
	return new String(cipher.doFinal(decodedBytes), "UTF-8")
}

def mdEncode(hashMethod, byte[] data) {
	MessageDigest md = MessageDigest.getInstance(hashMethod)
	md.update(data)
	return md.digest()
}

String encodeUtf8(String message) {
	byte[] arr = message.getBytes("UTF8")
	return new String(arr)
}

int byteArrayToInteger(byte[] byteArr) {
	int arrayASInteger
	try {
		arrayAsInteger = ((byteArr[0] & 0xFF) << 24) + ((byteArr[1] & 0xFF) << 16) +
			((byteArr[2] & 0xFF) << 8) + (byteArr[3] & 0xFF)
	} catch (error) {
		Map errLog = [byteArr: byteArr, ERROR: error]
		logWarn("byteArrayToInteger: ${errLog}")
	}
	return arrayAsInteger
}

byte[] integerToByteArray(value) {
	String hexValue = hubitat.helper.HexUtils.integerToHexString(value, 4)
	byte[] byteValue = hubitat.helper.HexUtils.hexStringToByteArray(hexValue)
	return byteValue
}

def getSeed(size) {
	byte[] temp = new byte[size]
	new Random().nextBytes(temp)
	return temp
}

// ~~~~~ end include (386) davegut.tpLinkCrypto ~~~~~

// ~~~~~ start include (388) davegut.tpLinkTransAes ~~~~~
library (
	name: "tpLinkTransAes",
	namespace: "davegut",
	author: "Compiled by Dave Gutheinz",
	description: "Handshake methods for TP-Link Integration",
	category: "utilities",
	documentationLink: ""
)

def aesHandshake(baseUrl = getDataValue("baseUrl"), devData = null) {
	Map reqData = [baseUrl: baseUrl, devData: devData]
	Map rsaKey = getRsaKey()
	def pubPem = "-----BEGIN PUBLIC KEY-----\n${rsaKey.public}-----END PUBLIC KEY-----\n"
	Map cmdBody = [ method: "handshake", params: [ key: pubPem]]
	Map reqParams = [uri: baseUrl,
					 body: new groovy.json.JsonBuilder(cmdBody).toString(),
					 requestContentType: "application/json",
					 timeout: 10]
	asynchttpPost("parseAesHandshake", reqParams, [data: reqData])
}

def parseAesHandshake(resp, data){
	Map logData = [method: "parseAesHandshake"]
	if (resp.status == 200 && resp.data != null) {
		try {
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl]
			Map cmdResp =  new JsonSlurper().parseText(resp.data)
			def cookieHeader = resp.headers["Set-Cookie"].toString()
			def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";"))
			byte[] privateKeyBytes = getRsaKey().private.decodeBase64()
			byte[] deviceKeyBytes = cmdResp.result.key.getBytes("UTF-8").decodeBase64()
    		Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding")
			instance.init(2, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes)))
			byte[] cryptoArray = instance.doFinal(deviceKeyBytes)
			byte[] encKey = cryptoArray[0..15]
			byte[] encIv = cryptoArray[16..31]
			logData << [respStatus: "Cookies/Keys Updated", cookie: cookie,
						encKey: encKey, encIv: encIv]
			String password = encPassword
			String username = encUsername
			if (device) {
				password = parent.encPassword
				username = parent.encUsername
				device.updateSetting("cookie",[type:"password", value: cookie])
				device.updateSetting("encKey",[type:"password", value: encKey])
				device.updateSetting("encIv",[type:"password", value: encIv])
			} else {
				reqData << [cookie: cookie, encIv: encIv, encKey: encKey]
			}
			Map cmdBody = [method: "login_device",
						   params: [password: password,
									username: username],
						   requestTimeMils: 0]
			def cmdStr = JsonOutput.toJson(cmdBody).toString()
			Map reqBody = [method: "securePassthrough",
						   params: [request: aesEncrypt(cmdStr, encKey, encIv)]]
			Map reqParams = [uri: reqData.baseUrl,
							  body: reqBody,
							  timeout:10, 
							  headers: ["Cookie": cookie],
							  contentType: "application/json",
							  requestContentType: "application/json"]
			asynchttpPost("parseAesLogin", reqParams, [data: reqData])
			logDebug(logData)
		} catch (err) {
			logData << [respStatus: "ERROR parsing HTTP resp.data",
						respData: resp.data, error: err]
			logWarn(logData)
		}
	} else {
		logData << [respStatus: "ERROR in HTTP response", resp: resp.properties]
		logWarn(logData)
	}
}

def parseAesLogin(resp, data) {
	if (device) {
		Map logData = [method: "parseAesLogin"]
		if (resp.status == 200) {
			if (resp.json.error_code == 0) {
				try {
					byte[] encKey = new JsonSlurper().parseText(encKey)
					byte[] encIv = new JsonSlurper().parseText(encIv)
					def clearResp = aesDecrypt(resp.json.result.response, encKey, encIv)
					Map cmdResp = new JsonSlurper().parseText(clearResp)
					if (cmdResp.error_code == 0) {
						def token = cmdResp.result.token
						logData << [respStatus: "OK", token: token]
						device.updateSetting("token",[type:"password", value: token])
						setCommsError(200)
						logDebug(logData)
					} else {
						logData << [respStatus: "ERROR code in cmdResp", 
									error_code: cmdResp.error_code,
									check: "cryptoArray, credentials", data: cmdResp]
						logInfo(logData)
					}
				} catch (err) {
					logData << [respStatus: "ERROR parsing respJson", respJson: resp.json,
								error: err]
					logInfo(logData)
				}
			} else {
				logData << [respStatus: "ERROR code in resp.json", errorCode: resp.json.error_code,
							respJson: resp.json]
				logInfo(logData)
			}
		} else {
			logData << [respStatus: "ERROR in HTTP response", respStatus: resp.status, data: resp.properties]
			logInfo(logData)
		}
	} else {
		getAesToken(resp, data.data)
	}
}

def getAesParams(cmdBody) {
	byte[] encKey = new JsonSlurper().parseText(encKey)
	byte[] encIv = new JsonSlurper().parseText(encIv)
	def cmdStr = JsonOutput.toJson(cmdBody).toString()
	Map reqBody = [method: "securePassthrough",
				   params: [request: aesEncrypt(cmdStr, encKey, encIv)]]
	Map reqParams = [uri: "${getDataValue("baseUrl")}?token=${token}",
					 body: new groovy.json.JsonBuilder(reqBody).toString(),
					 contentType: "application/json",
					 requestContentType: "application/json",
					 timeout: 10,
					 ignoreSSLIssues: true,
					 headers: ["Cookie": cookie]]
	return reqParams
}

def parseAesData(resp, data) {
	Map parseData = [parseMethod: "parseAesData", sourceMethod: data.data]
	try {
		byte[] encKey = new JsonSlurper().parseText(encKey)
		byte[] encIv = new JsonSlurper().parseText(encIv)
		Map cmdResp = new JsonSlurper().parseText(aesDecrypt(resp.json.result.response,
														 encKey, encIv))
		parseData << [cryptoStatus: "OK", cmdResp: cmdResp]
	} catch (err) {
		parseData << [cryptoStatus: "decryptDataError", error: err, dataLength: resp.data.length()]
	}
	return parseData
}

def getRsaKey() {
	return [public: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDGr/mHBK8aqx7UAS+g+TuAvE3J2DdwsqRn9MmAkjPGNon1ZlwM6nLQHfJHebdohyVqkNWaCECGXnftnlC8CM2c/RujvCrStRA0lVD+jixO9QJ9PcYTa07Z1FuEze7Q5OIa6pEoPxomrjxzVlUWLDXt901qCdn3/zRZpBdpXzVZtQIDAQAB",
			private: "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMav+YcErxqrHtQBL6D5O4C8TcnYN3CypGf0yYCSM8Y2ifVmXAzqctAd8kd5t2iHJWqQ1ZoIQIZed+2eULwIzZz9G6O8KtK1EDSVUP6OLE71An09xhNrTtnUW4TN7tDk4hrqkSg/GiauPHNWVRYsNe33TWoJ2ff/NFmkF2lfNVm1AgMBAAECgYEAocxCHmKBGe2KAEkq+SKdAxvVGO77TsobOhDMWug0Q1C8jduaUGZHsxT/7JbA9d1AagSh/XqE2Sdq8FUBF+7vSFzozBHyGkrX1iKURpQFEQM2j9JgUCucEavnxvCqDYpscyNRAgqz9jdh+BjEMcKAG7o68bOw41ZC+JyYR41xSe0CQQD1os71NcZiMVqYcBud6fTYFHZz3HBNcbzOk+RpIHyi8aF3zIqPKIAh2pO4s7vJgrMZTc2wkIe0ZnUrm0oaC//jAkEAzxIPW1mWd3+KE3gpgyX0cFkZsDmlIbWojUIbyz8NgeUglr+BczARG4ITrTV4fxkGwNI4EZxBT8vXDSIXJ8NDhwJBAIiKndx0rfg7Uw7VkqRvPqk2hrnU2aBTDw8N6rP9WQsCoi0DyCnX65Hl/KN5VXOocYIpW6NAVA8VvSAmTES6Ut0CQQCX20jD13mPfUsHaDIZafZPhiheoofFpvFLVtYHQeBoCF7T7vHCRdfl8oj3l6UcoH/hXMmdsJf9KyI1EXElyf91AkAvLfmAS2UvUnhX4qyFioitjxwWawSnf+CewN8LDbH7m5JVXJEh3hqp+aLHg1EaW4wJtkoKLCF+DeVIgbSvOLJw"]
}

// ~~~~~ end include (388) davegut.tpLinkTransAes ~~~~~

// ~~~~~ start include (390) davegut.tpLinkTransKlap ~~~~~
library (
	name: "tpLinkTransKlap",
	namespace: "davegut",
	author: "Compiled by Dave Gutheinz",
	description: "Handshake methods for TP-Link Integration",
	category: "utilities",
	documentationLink: ""
)

def klapHandshake(baseUrl, localHash, devData = null) {
	byte[] localSeed = getSeed(16)
	Map reqData = [localSeed: localSeed, baseUrl: baseUrl, localHash: localHash, devData:devData]
	Map reqParams = [uri: "${baseUrl}/handshake1",
					 body: localSeed,
					 contentType: "application/octet-stream",
					 requestContentType: "application/octet-stream",
					 timeout:10,
					 ignoreSSLIssues: true]
	asynchttpPost("parseKlapHandshake", reqParams, [data: reqData])
}

def parseKlapHandshake(resp, data) {
	Map logData = [method: "parseKlapHandshake"]
	if (resp.status == 200 && resp.data != null) {
		try {
			Map reqData = [devData: data.data.devData, baseUrl: data.data.baseUrl]
			byte[] localSeed = data.data.localSeed
			byte[] seedData = resp.data.decodeBase64()
			byte[] remoteSeed = seedData[0 .. 15]
			byte[] serverHash = seedData[16 .. 47]
			byte[] localHash = data.data.localHash.decodeBase64()
			byte[] authHash = [localSeed, remoteSeed, localHash].flatten()
			byte[] localAuthHash = mdEncode("SHA-256", authHash)
			if (localAuthHash == serverHash) {
				def cookieHeader = resp.headers["Set-Cookie"].toString()
				def cookie = cookieHeader.substring(cookieHeader.indexOf(":") +1, cookieHeader.indexOf(";"))
				byte[] payload = ["iv".getBytes(), localSeed, remoteSeed, localHash].flatten()
				byte[] fullIv = mdEncode("SHA-256", payload)
				byte[] byteSeqNo = fullIv[-4..-1]

				int seqNo = byteArrayToInteger(byteSeqNo)
				if (device) {
					state.seqNo = seqNo
				} else {
					atomicState.seqNo = seqNo
				}

				payload = ["lsk".getBytes(), localSeed, remoteSeed, localHash].flatten()
				byte[] encKey = mdEncode("SHA-256", payload)[0..15]
				payload = ["ldk".getBytes(), localSeed, remoteSeed, localHash].flatten()
				byte[] encSig = mdEncode("SHA-256", payload)[0..27]
				if (device) {
					device.updateSetting("cookie",[type:"password", value: cookie]) 
					device.updateSetting("encKey",[type:"password", value: encKey]) 
					device.updateSetting("encIv",[type:"password", value: fullIv[0..11]]) 
					device.updateSetting("encSig",[type:"password", value: encSig]) 
				} else {
					reqData << [cookie: cookie, seqNo: seqNo, encIv: fullIv[0..11], 
								encSig: encSig, encKey: encKey]
				}
				byte[] loginHash = [remoteSeed, localSeed, localHash].flatten()
				byte[] body = mdEncode("SHA-256", loginHash)
				Map reqParams = [uri: "${data.data.baseUrl}/handshake2",
								 body: body,
								 timeout:10,
								 ignoreSSLIssues: true,
								 headers: ["Cookie": cookie],
								 contentType: "application/octet-stream",
								 requestContentType: "application/octet-stream"]
				asynchttpPost("parseKlapHandshake2", reqParams, [data: reqData])
			} else {
				logData << [respStatus: "ERROR: localAuthHash != serverHash", data: data,
							action: "<b>Check credentials and try again</b>"]
				logWarn(logData)
			}
		} catch (err) {
			logData << [respStatus: "ERROR parsing 200 response", resp: resp.properties, error: err]
			logData << [action: "<b>Try Configure command</b>"]
			logWarn(logData)
		}
	} else {
		logData << [respStatus: resp.status, message: resp.errorMessage]
		logData << [action: "<b>Try Configure command</b>"]
		logWarn(logData)
	}
}

def parseKlapHandshake2(resp, data) {
	Map logData = [method: "parseKlapHandshake2"]
	if (resp.status == 200 && resp.data == null) {
		logData << [respStatus: "Login OK"]
		setCommsError(200)
		logDebug(logData)
	} else {
		logData << [respStatus: "LOGIN FAILED", reason: "ERROR in HTTP response",
					resp: resp.properties]
		logWarn(logData)
	}
	if (!device) { sendKlapDataCmd(logData, data) }
}

def getKlapParams(cmdBody) {
	int seqNo = state.seqNo + 1
	state.seqNo = seqNo
	byte[] encKey = new JsonSlurper().parseText(encKey)
	byte[] encIv = new JsonSlurper().parseText(encIv)
	byte[] encSig = new JsonSlurper().parseText(encSig)
	String cmdBodyJson = new groovy.json.JsonBuilder(cmdBody).toString()

	Map encryptedData = klapEncrypt(cmdBodyJson.getBytes(), encKey, encIv,
									encSig, seqNo)
	Map reqParams = [
		uri: "${getDataValue("baseUrl")}/request?seq=${seqNo}",
		body: encryptedData.cipherData,
		headers: ["Cookie": cookie],
		contentType: "application/octet-stream",
		requestContentType: "application/octet-stream",
		timeout: 10,
		ignoreSSLIssues: true]
	return reqParams
}

def parseKlapData(resp, data) {
	Map parseData = [Method: "parseKlapData", sourceMethod: data.data]
	try {
		byte[] encKey = new JsonSlurper().parseText(encKey)
		byte[] encIv = new JsonSlurper().parseText(encIv)
		int seqNo = state.seqNo
		byte[] cipherResponse = resp.data.decodeBase64()[32..-1]
		Map cmdResp =  new JsonSlurper().parseText(klapDecrypt(cipherResponse, encKey,
														   encIv, seqNo))
		parseData << [cryptoStatus: "OK", cmdResp: cmdResp]
	} catch (err) {
		parseData << [cryptoStatus: "decryptDataError", error: err]
	}
	return parseData
}

// ~~~~~ end include (390) davegut.tpLinkTransKlap ~~~~~

// ~~~~~ start include (376) davegut.Logging ~~~~~
library (
	name: "Logging",
	namespace: "davegut",
	author: "Dave Gutheinz",
	description: "Common Logging and info gathering Methods",
	category: "utilities",
	documentationLink: ""
)

def nameSpace() { return "davegut" }

def version() { return "2.4.2a" }

def label() {
	if (device) { 
		return device.displayName + "-${version()}"
	} else { 
		return app.getLabel() + "-${version()}"
	}
}

def updateAttr(attr, value) {
	if (device.currentValue(attr) != value) {
		sendEvent(name: attr, value: value)
	}
}

def listAttributes() {
	def attrData = device.getCurrentStates()
	Map attrs = [:]
	attrData.each {
		attrs << ["${it.name}": it.value]
	}
	return attrs
}

def setLogsOff() {
	def logData = [infoLog: infoLog, logEnable: logEnable]
	if (logEnable) {
		runIn(1800, debugLogOff)
		logData << [debugLogOff: "scheduled"]
	}
	return logData
}

def logTrace(msg){ 
	if (getSettingBool("logTraceEnable", false)) {
		log.trace "${label()}: ${msg}" 
	}
}

def logInfo(msg) { 
	if (getSettingBool("logInfoEnable", true) && infoLog) { log.info "${label()}: ${msg}" }
}

def debugLogOff() {
	if (device) {
		device.updateSetting("logEnable", [type:"bool", value: false])
		device.updateSetting("logDebugEnable", [type:"bool", value: false])
	} else {
		app.updateSetting("logEnable", false)
	}
	logInfo("debugLogOff")
}

def logDebug(msg) {
	if (getSettingBool("logDebugEnable", false) || logEnable) { log.debug "${label()}: ${msg}" }
}

def logWarn(msg) { 
	if (getSettingBool("logWarnEnable", true)) { log.warn "${label()}: ${msg}" }
}

def logError(msg) { 
	if (getSettingBool("logErrorEnable", true)) { log.error "${label()}: ${msg}" }
}

// ~~~~~ end include (376) davegut.Logging ~~~~~