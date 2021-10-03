/*
 * Meteorological Season of the Northern Hemisphere
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2021-09-29  thebearmay	 Original code from Moon Phase driver
 *	  2021-09-30  jshimota       Began modifying
 */

import java.text.SimpleDateFormat
static String version()	{  return '0.7.3'  }

metadata {
    definition (
		name: "Meteorological Season of the Northern Hemisphere",
		namespace: "jshimota", 
		author: "James Shimota",
	        importUrl:"https://raw.githubusercontent.com/jshimota01/hubitat/main/meteorSeason-NH.groovy"
	) {
        capability "Actuator"
        
        attribute "seasonName", "string"
		attribute "seasonNameNum", "number"
        attribute "seasonNameTile", "string"
        attribute "seasonNameImg", "string"
        attribute "html", "string"
        
        command "getSeason"
        command "calcSeason", [[name:"dateStr", type:"STRING", description:"Date (yyyy-MM-dd HH:mm:ss) to calculate the season name for."]]              
            
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("autoUpdate", "bool", title: "Enable automatic update at midnight")
    input("htmlVtile", "bool", title:"Use html attribute instead of seasonNameTile")
    input("iconPathOvr", "string", title: "Alternate path to season icons \n(must contain file names season-name-icon-0 through moon-phase-icon-3)")
}

def installed() {
	log.trace "installed()"
}

def configure() {
    if(debugEnable) log.debug "configure()"
}

def calcSeason (String dateStr){
    Long cDate = dateCheck(dateStr)
    if (cDate !=0L) getSeason(cDate)

}

Long dateCheck(String dateStr) {
    try {
        Date cDate = Date.parse("yyyy-MM-dd HH:mm:ss",dateStr)
        return cDate.getTime()
    } catch (ignored) {
        updateAttr("error", "Invalid date string use format yyyy-MM-dd HH:mm:ss")
        return 0L
    }
}

void getSeason(Long cDate = now()) {
    // Date d_refDate = Date.parse("yyyy-MM-dd HH:mm:ss","2000-01-06 18:14:00") //First New Moon of 2000
    // Long refDate = d_refDate.getTime()

    // Double lunarDays = 29.53058770576
    // Double lunarSecs = lunarDays*8640000

    def sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    def seasonWork = cDate //assign current date to seasonWork variable
    // seasonWork = seasonWork/lunarSecs/10.0D //calculate lunar cycles
    
    // seasonWork = seasonWork - seasonWork.toInteger() //remove whole cycles
    // seasonWork = seasonWork.round(2)
    
    if(seasonWork == 1.0) seasonWork = 0.0
    
	updateAttr("seasonNameNum", seasonWork)
    
    String iconPath = "https://raw.githubusercontent.com/jshimota01/hubitat/main/seasonNameRes/"
    if(iconPathOvr > " ") iconPath = iconPathOvr
    Integer imgNum
    String seasonText


        if (seasonWork = "Fall"){
            imgNum = 0
        }else if (seasonWork = "Winter"){
            imgNum = 1
        }else if (seasonWork = "Spring"){
            imgNum = 2
        }else if (seasonWork = "Summer"){
            imgNum = 3
        }else {
            imgNum = null
        }

    List<String>imgList = ["Fall", "Winter", "Spring", "Summer"]
    if(imgNum!=null) {
        seasonText = imgList[imgNum]
    } else seasonText = "Error - Out of Range"
        
    updateAttr("seasonNameImg", "<img class='seasonName' src='${iconPath}season-name-icon-${imgNum}.svg' />")    
    updateAttr("seasonName", seasonText)
    String seasonIcon = "<div id='moonTile'><img class='seasonName' src='${iconPath}season-name-icon-${imgNum}.svg'><p class='small' style='text-align:center'>$seasonText</p></img></div>"
    if(!htmlVtile)
        updateAttr("seasonNameTile",seasonIcon)
    else
        updateAttr("html",seasonIcon)
}

void updateAttr(String aKey, aValue){
    sendEvent(name:aKey, value:aValue)
}

void updateAttr(String aKey, aValue, aUnit){
    sendEvent(name:aKey, value:aValue, unit:aUnit)
}

def initialize(){

}

def updated(){
	log.trace "updated()"
    unschedule()
    if(autoUpdate) schedule("1 0 0 ? * * *", getSeason)
	if(debugEnable) runIn(1800,logsOff)
}

void logsOff(){
     device.updateSetting("debugEnable",[value:"false",type:"bool"])
}
