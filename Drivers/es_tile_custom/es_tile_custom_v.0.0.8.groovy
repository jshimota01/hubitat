/* Echo Speaks Tile Custom
 *
 * Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Change History:
 *
 * Date         Who           Version    What
 * ----         ---           ---        ----
 * 28Aug2023    thebearmay               HE 2.3.6.x changes
 * 11Sep2023    thebearmay               Add server attribute option
 * 18Sep2023    thebearmay               Add Debug Logging option
 * 23Oct2023    thebearmay               Add serverIp as an atttribute
 * 18Nov2024    thebearmay    0.0.6      HE 2.4.0.x changes
 * 20Jun2026    jshimota      0.0.8      Resource Optimization Fixes
 */

import java.text.SimpleDateFormat
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

@Field static final String okSymFLD       = "\u2713"
@Field static final String notOkSymFLD    = "<span style='color:red'>\u2715</span>"
@Field static final String sBLANK         = ''
@Field static String minFwVersion = "2.3.6.121"

@SuppressWarnings('unused')
static String version() { return "0.0.8" } 

metadata {
    definition (
        name: "ES Tile Custom", 
        namespace: "jshimota", 
        author: "Jean P. May, Jr.",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/es_tile_custom/es_tile_custom.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        
        attribute "cookieRefreshDays", "number"
        attribute "serverData","string"
        attribute "cookieData","string"
        attribute "csrf","string"
        attribute "amazonDomain","string"
        attribute "tm2NewAtRfrsh", "string"
        attribute "tmFromAtRrsh", "string"
        attribute "serverLocation", "string"
        attribute "anError","string"
        attribute "serverIp","string"
        
        attribute "html","string"
        attribute "htmlAlt", "string"
       
        command "refreshHTML"
    }   
}

preferences {
    input("vDisp", "hidden", title:"Driver Version", description:"<b>v${version()}</b>")
    if(location.hub.firmwareVersionString < minFwVersion){
        input("errMsg", "hidden", title:"<b>Minimum Version Error</b>", description:"<span style='background-color:red;font-weight:bold;color:black;'>Hub does not meet the minimum of HEv$minFwVersion</span>", width:8)
    }
    input("security", "bool", title: "Hub Security Enabled", defaultValue: false, submitOnChange: true, width:4)
    if (security) { 
        input("username", "string", title: "Hub Security Username", required: false, width:4)
        input("password", "password", title: "Hub Security Password", required: false, width:4)
    }
    input("pollRate","number", title:"Poll rate (in minutes) Disable:0):", defaultValue:720, submitOnChange:true, width:4)
    input("debugEnabled","bool", title:"Enable Debug Logging", defaultValue:false, submitOnChange:true, width:4)
}

@SuppressWarnings('unused')
def installed() {
    if(location.hub.firmwareVersionString < minFwVersion){
        updateAttr("anError","<span style='background-color:red;font-weight:bold;color:black;'>Hub does not meet the minimum of HEv$minFwVersion</span>")
    } else {
        device.deleteCurrentState("anError")
    }
}

void updateAttr(String aKey, aValue, String aUnit = ""){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

void refresh(){
    if(location.hub.firmwareVersionString < minFwVersion){
        updateAttr("anError","<span style='background-color:red;font-weight:bold;color:black;'>Hub does not meet the minimum of HEv$minFwVersion</span>")
        return
    } else {
        device.deleteCurrentState("anError")
    }
    
    processPage()
    refreshHTML()
    
    if(pollRate == null) {
        device.updateSetting("pollRate",[value:720, type:"number"])   
    }
    if(pollRate > 0) {
        runIn(pollRate * 60, "refresh")
    }
}

def updated(){
    if(pollRate == null) {
        device.updateSetting("pollRate",[value:720, type:"number"])   
    }
    if(pollRate > 0) {
        runIn(pollRate * 60, "refresh")
    }
    if(debugEnabled) {
        runIn(1800, "logsOff")
    }
}

void processPage(){
    def app = findPage()
    if(app == -1) {
        log.error "Echo Speaks not Installed"
        return
    }
    if(minVerCheck("2.4.0.0")) {
        processJsonData(app)
    } else {    
        def pData = readExtPage("http://127.0.0.1:8080/installedapp/status/$app")
        if(!pData) return
        
        def dWork = pData.substring(pData.indexOf('refreshCookieDays'), pData.indexOf('refreshCookieDays') + 500)
        if(debugEnabled) log.debug "Refresh Days Work: <br> $dWork"
        
        dWork = dWork.replace('<','')
        def dWorkSplit = dWork.split(' ')
        dWorkSplit.each {
            if(debugEnabled) log.debug "Refresh Split Each: $it"
            if(it.isNumber()) updateAttr("cookieRefreshDays", it.toInteger())
        }
        
        dWork = pData.substring(pData.indexOf('serverDataMap'), pData.indexOf('serverDataMap') + 800)
        dWork = dWork.substring(dWork.indexOf('{'), dWork.indexOf('}') + 1)
   
        createServerMap(dWork)
        if(pData.indexOf("cookieData") > -1){
            if(debugEnabled) log.debug "Found Cookie Data"
            updateAttr("cookieData", "true")
            if(pData.indexOf("csrf") > -1){
                updateAttr("csrf", "true")
                if(debugEnabled) log.debug "Found csrf"
            }
        }
        
        dWork = pData.substring(pData.indexOf('amazonDomain'), pData.indexOf('amazonDomain') + 300)
        if(debugEnabled) log.debug "Amazon Domain Raw: $dWork"
        dWork = dWork.replace('<','')
        dWorkSplit = dWork.split(' ')
        dWorkSplit.each {
            if(it.contains(".")){
                updateAttr("amazonDomain", it.trim())
                if(debugEnabled) log.debug "Amazon Domain: ${it.trim()}"
            }
        }
    }
}

void processJsonData(app){
    def jData = readJsonPage("http://127.0.0.1:8080/installedapp/statusJson/$app") 
    if(!jData) return
    
    def cookieRefreshDays = 0
    jData.appSettings.each {
        if(it.name == "cookieRefreshDays"){
            cookieRefreshDays = it.value.toInteger()
            updateAttr("cookieRefreshDays", cookieRefreshDays)
        }
    }
    def cookieData = ''
    jData.appState.each {
        if(it.name == 'serverDataMap'){
            updateAttr("serverData", JsonOutput.toJson(it.value))
        }
        if(it.name == 'cookieData'){
            cookieData = it.value
            updateAttr("cookieData", "true")
            if(cookieData && cookieData.csrf){
                updateAttr("csrf", "true")
                if(debugEnabled) log.debug "Found csrf"
            }
        }
        if(it.name == 'amazonDomain'){
            updateAttr("amazonDomain", it.value.trim())
        }
    }
}

Integer findPage(){
    def params = [
        uri: "http://127.0.0.1:8080/hub2/appsList",
        contentType: "application/json",
        followRedirects: false,
        textParser: false
    ]
    
    def appId = -1
    try {
        httpGet(params) { resp ->  
            if(debugEnabled) log.debug "GET: ${resp.data?.apps}"
            resp.data?.apps.each { a ->
                if(debugEnabled) log.debug "${a.data?.type}"
                if("${a.data?.type}" == "Echo Speaks") {
                    appId = a.data.id
                    if(debugEnabled) log.debug "Found it ${a.data.id}"
                }
            }
        }
    } catch (e) {
        log.error "Error retrieving installed apps: ${e}"
    }
    return appId
}

void createServerMap(sData){
    if(debugEnabled) log.debug "Server Data Raw: $sData"
    def sWork = sData.replace("&#x3D;", '\":\"')
    sWork = sWork.replace(', ', '","')
    sWork = sWork.replace('{', '{\"')
    sWork = sWork.replace('}', '\"}')
    if(debugEnabled) log.debug "Server Information: $sWork"
    updateAttr("serverData", sWork)
}

void refreshHTML(){
    if(debugEnabled) log.debug "Refreshing HTML"
    Long tNow = new Date().getTime()
    
    def serverDataRaw = device.currentValue("serverData", true)
    if(!serverDataRaw) return
    
    def jSlurp = new JsonSlurper()
    def serverData = jSlurp.parseText(serverDataRaw)
    
    // Pass the parsed serverData map down directly to minimize slurper overhead
    nextCookieRefreshDur(serverData, tNow)
    
    def isCsrf = device.currentValue("csrf", true) == "true"
    def isCookie = device.currentValue("cookieData", true) == "true"
    def authSymbol = (isCsrf && isCookie) ? okSymFLD : notOkSymFLD
    def cookieSymbol = isCookie ? okSymFLD : notOkSymFLD
    def csrfSymbol = isCsrf ? okSymFLD : notOkSymFLD
    
    def baseTable = "<table style='color:mediumblue;font-size:small'><tr><th>Auth Status: ${authSymbol}</th></tr>" +
                    "<tr><td>&nbsp;&nbsp;Cookie: ${cookieSymbol}</td></tr>" +
                    "<tr><td>&nbsp;&nbsp;CSRF: ${csrfSymbol}</td></tr>" +
                    "<tr><th>Cookie Data</th></tr>"
    
    def refreshDays = device.currentValue("cookieRefreshDays") ? device.currentValue("cookieRefreshDays").toInteger() : 0
    def startDate = Date.parse("E MMM dd HH:mm:ss z yyyy", serverData.lastCookieRrshDt).getTime()
    def nextDate = startDate + (86400000L * refreshDays)
    
    SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
    def dateStr = sdf.format(nextDate)
    def refreshRow = (nextDate > tNow) ? "<tr><td>Next Refresh: ${dateStr}</td></tr>" : "<tr><td style='color:red;font-weight:bold'>Missed Refresh: ${dateStr}</td></tr>"
    
    def isHeroku = (serverData.onHeroku == "true" || serverData.onHeroku)
    updateAttr("serverLocation", isHeroku ? "Heroku" : "Local")
    def herokuSymbol = isHeroku ? okSymFLD : notOkSymFLD
    
    def localSymbol = (serverData.isLocal == "true" || serverData.isLocal) ? okSymFLD : notOkSymFLD
    def hostIp = serverData.serverHost ?: ""
    def domainVal = device.currentValue("amazonDomain", true) ?: ""
    
    if(hostIp.length() > 7) {
        updateAttr("serverIp", hostIp.substring(7))
    }
    
    // Build HTML 1
    def wkStr = baseTable + "<tr><td>Last Refresh: ${serverData.lastCookieRrshDt}</td></tr>" + refreshRow +
                "</td></tr><tr><th>Server Data</th></tr><tr><td>Heroku: ${herokuSymbol}</td></tr>" +
                "<tr><td>Local Server: ${localSymbol}</td></tr><tr><td>Server IP: ${hostIp}</td></tr>" +
                "<tr><td>Domain: ${domainVal}</td></tr></table>"
    updateAttr("html", wkStr)
    
    // Build HTML Alt
    def wkStr2 = baseTable + "<tr><td>Last Refresh: ${device.currentValue("tmFromAtRrsh", true)} ago</td></tr>" +
                 ((nextDate > tNow) ? "<tr><td>Next Refresh: ${device.currentValue("tm2NewAtRfrsh", true)}</td></tr>" : "<tr><td style='color:red;font-weight:bold'>Missed Refresh: ${dateStr}</td></tr>") +
                 "</td></tr><tr><th>Server Data</th></tr><tr><td>Heroku: ${herokuSymbol}</td></tr>" +
                 "<tr><td>Local Server: ${localSymbol}</td></tr><tr><td>Server IP: ${hostIp}</td></tr>" +
                 "<tr><td>Domain: ${domainVal}</td></tr></table>"
    updateAttr("htmlAlt", wkStr2)
    
    if(debugEnabled) log.debug "HTML Refresh complete"    
}

String nextCookieRefreshDur(Map serverData, Long tNow) {
    def refreshDaysVal = device.currentValue("cookieRefreshDays")
    Integer days = refreshDaysVal ? refreshDaysVal.toInteger() : 0
    String lastCookieRfsh = serverData.lastCookieRrshDt
    if(!lastCookieRfsh) { return "Not Sure" }
    
    Date lastDt = Date.parse("E MMM dd HH:mm:ss z yyyy", lastCookieRfsh)   
    String dMinus = seconds2Duration(((tNow - lastDt.getTime()) / 1000) as Integer, false, 3)
    updateAttr("tmFromAtRrsh", dMinus)                                                                        
                                                                             
    Date nextDt = lastDt + days
    Integer diff = ((nextDt.getTime() - tNow) / 1000) as Integer
    String dur = seconds2Duration(diff, false, 3)
    updateAttr("tm2NewAtRfrsh", dur)
    return dur
}

String formatDt(Date dt, Boolean tzChg=true) {
    def tf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
    if(tzChg && location.timeZone) { tf.setTimeZone((TimeZone)location.timeZone) }
    return (String)tf.format(dt)
}

@SuppressWarnings('GroovyAssignabilityCheck')
static String seconds2Duration(Integer itimeSec, Boolean postfix=true, Integer tk=2) {
    Integer timeSec = itimeSec < 0 ? 0 : itimeSec
    
    // Efficient native integer division instead of forcing double parsing math operations
    Integer years = timeSec / 31536000; timeSec -= years * 31536000
    Integer months = timeSec / 2592000; timeSec -= months * 2592000
    Integer days = timeSec / 86400; timeSec -= days * 86400
    Integer hours = timeSec / 3600; timeSec -= hours * 3600
    Integer minutes = timeSec / 60; timeSec -= minutes * 60
    Integer seconds = timeSec % 60
    
    List l = []
    if(days > 0) { l.push("${days} ${pluralize(days, "day")}") }
    if(hours > 0) { l.push("${hours} ${pluralize(hours, "hour")}") }
    if(minutes > 0) { l.push("${minutes} ${pluralize(minutes, "min")}") }
    if(seconds > 0) { l.push("${seconds} ${pluralize(seconds, "sec")}") }
    
    return l.size() ? "${l.take(tk ?: 2)?.join(", ")}${postfix ? " ago" : sBLANK}" : "0 sec"
}

static String pluralize(Integer itemVal, String str) { return (itemVal > 1) ? str+"s" : str }

def readJsonPage(fName){
    def params = [
        uri: fName,
        contentType: "application/json",
        headers: [ "Connection-Timeout": 600 ]
    ]
    try {
        httpGet(params) { resp ->
            if(resp != null) {
                return resp.data
            } else {
                log.error "Read External - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null
    }
}

String readExtPage(fName){  
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true,
        headers: [ "Connection-Timeout": 600 ]
    ]
    try {
        httpGet(params) { resp ->
            if(resp != null) {
                return """${resp.data}"""
            } else {
                log.error "Read External - Null Response"
                return null
            }
        }
    } catch (exception) {
        log.error "Read Ext Error: ${exception.message}"
        return null
    }
}

Boolean minVerCheck(vStr){
    def fwTokens = location.hub.firmwareVersionString.split("\\.")
    def vTokens = vStr.split("\\.")
    
    int size = Math.min(fwTokens.size(), vTokens.size())
    for(int i = 0; i < size; i++) {
        int fwVal = fwTokens[i].toInteger()
        int vVal = vTokens[i].toInteger()
        if (fwVal > vVal) return true
        if (fwVal < vVal) return false
    }
    return fwTokens.size() >= vTokens.size()
}

@SuppressWarnings('unused')
void logsOff(){
     device.updateSetting("debugEnabled",[value:"false", type:"bool"])
}