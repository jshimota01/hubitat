/*
 * Moon Phase (Optimized)
 */
/*
 * Moon Phase
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
 *    2021-03-17  thebearmay	 Original version 0.1.0
 *                               Calc corrections, add alternate input stream
 *    2021-03-18  thebearmay     Add an tile attribute, and icon path override
 *                               add scheduled update at midnight + 1 second
 *    2021-03-28  thebearmay     Add option to widen the quarterly checkpoints by 1%
 *    2021-03-29  thebearmay     Image path as an attribute
 *    2021-03-30  thebearmay     Image Only tile instead of path
 *    2021-07-04  thebearmay	 Merge pull request from imnotbob, strong typing of variables
 *    2021-08-28  thebearmay	 add option to use html attribute instead of moonPhaseTile
 *    2021-09-29  thebearmay	 Last Quarter typo - left out the first "r"
 *    2021-10-03  thebearmay     Change refresh to sunset
 *    2023-08-29  jshimota       Created B version for custom Icon
 *    2026-05-01  jshimota       Gemini reviewed in attempt to get this working better
 */
 
import java.text.SimpleDateFormat
static String version()    {  return '0.7.4c'  }

metadata {
    definition (
        name: "Moon Phase (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
        importUrl:"https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhase.groovy"
    ) {
        capability "Actuator"
        capability "Refresh"
        
        attribute "moonPhase", "enum", ["New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"]
        attribute "moonPhaseNum", "number"
        attribute "lastQryDate", "string"
        attribute "moonPhaseEmoji", "string"
        attribute "moonPhaseTile", "string"
        attribute "moonPhaseImg", "string"
        attribute "moonPhaseSvg", "string"
        attribute "html", "string"
        
        command "getPhase"
        command "calcPhase", [[name:"dateStr", type:"STRING", description:"Date (yyyy-MM-dd HH:mm:ss)"]]              
    }   
}

preferences {
    input("debugEnable", "bool", title: "Enable debug logging?")
    input("autoUpdate", "bool", title: "Enable automatic update at sunset", defaultValue: true)
    input("widenRange","bool",title:"Widen the Qtrly Checkpoints by 1%")
    input("htmlVtile", "bool", title:"Use 'html' attribute instead of 'moonPhaseTile'")
    input("htmlVStile", "bool", title:"Use SVG for tile images instead of PNG")
    input("iconPathOvr", "string", title: "Alternate path to moon phase icons")
}

def updated() {
    if(debugEnable) log.debug "Updated..."
    scheduleRefresh()
    getPhase()
}

def refresh() {
    if(debugEnable) log.debug "Refresh requested..."
    getPhase()
}

def scheduleRefresh() {
    unschedule()
    if (autoUpdate) {
        def riseAndSet = getSunriseAndSunset()
        def targetDate = riseAndSet.sunset
        if(targetDate < new Date()) {
            targetDate = getSunriseAndSunset(sunsetOffset: "+24:00").sunset
        }
        runOnce(targetDate, getPhase)
        if(debugEnable) log.debug "Next refresh scheduled for sunset: ${targetDate}"
    }
}

def calcPhase(String dateStr) {
    try {
        def cDate = Date.parse("yyyy-MM-dd HH:mm:ss", dateStr)
        getPhase(cDate.getTime())
    } catch (e) {
        log.error "Invalid date format. Use yyyy-MM-dd HH:mm:ss"
    }
}

void getPhase(Long cDate = now()) {
    // Reference: First New Moon of 2000
    def refDate = Date.parse("yyyy-MM-dd HH:mm:ss","2000-01-06 18:14:00").getTime()
    
    // Lunar cycle in milliseconds
    double lunarMs = 29.53058770576 * 86400000.0
    
    double phaseWork = (cDate - refDate) / lunarMs
    phaseWork = phaseWork - phaseWork.toInteger()
    if (phaseWork < 0) phaseWork += 1.0
    
    // Precision adjustment based on preference
    double displayPhase = widenRange ? phaseWork.round(1) : phaseWork.round(2)
    if(displayPhase >= 1.0) displayPhase = 0.0

    // Attribute Updates
    sendEvent(name: "moonPhaseNum", value: displayPhase)
    sendEvent(name: "lastQryDate", value: new Date(cDate).format("yyyy-MM-dd HH:mm:ss"))
    
    // Logic for naming and icons
    int imgNum = (int)(phaseWork * 8) % 8
    List<String> phaseNames = ["New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"]
    List<String> emojis = ["🌑","🌒","🌓","🌔","🌕","🌖","🌗","🌘"]
    
    String phaseText = phaseNames[imgNum]
    String phaseEmoji = emojis[imgNum]
    String iconPath = (iconPathOvr && iconPathOvr.trim()) ? iconPathOvr : "https://raw.githubusercontent.com/thebearmay/hubitat/main/moonPhaseRes/"

    // SVG Math and String Generation
    String svgData = generateSvg(phaseWork, iconPath)
    
    sendEvent(name: "moonPhase", value: phaseText)
    sendEvent(name: "moonPhaseEmoji", value: phaseEmoji)
    sendEvent(name: "moonPhaseSvg", value: svgData)
    sendEvent(name: "moonPhaseImg", value: "<img class='moonPhase' src='${iconPath}moon-phase-icon-${imgNum}.png' />")

    // Tile Generation (Fixed the SVG injection)
    String mediaElement = htmlVStile ? svgData : "<img class='moonPhase' src='${iconPath}moon-phase-icon-${imgNum}.png' style='width:100px;'>"
    String tileHtml = "<div id='moonTile' style='text-align:center;width:100%;'>${mediaElement}<div class='small'>$phaseText</div></div>"
    
    if(htmlVtile) sendEvent(name: "html", value: tileHtml)
    else sendEvent(name: "moonPhaseTile", value: tileHtml)

    scheduleRefresh()
}

String generateSvg(double phase, String path) {
    double rx1 = 65.0, rx2 = 65.0
    int sf1 = 1, sf2 = 1
    
    if (phase <= 0.25) {
        rx1 = rx1 * (1 - 4 * phase)
    } else if (phase <= 0.50) {
        rx1 = rx1 * (4 * phase - 1)
        sf1 = 0
    } else if (phase <= 0.75) {
        rx2 = rx2 * (3 - 4 * phase)
        sf2 = 0
    } else {
        rx2 = rx2 * (4 * phase - 3)
    }

    return """<svg width="100%" viewBox="0 0 140 140" xmlns="http://www.w3.org/2000/svg">
      <filter id="blur"><feGaussianBlur stdDeviation="4"/></filter>
      <mask id="arc"><path d="M70,5 A$rx1,65 180 0 $sf1 70,135 A$rx2,65 180 0 $sf2 70,5 z" fill="#fff" filter="url(#blur)"/></mask>
      <radialGradient id="shadow"><stop offset="10%" stop-color="#0007"/><stop offset="90%" stop-color="#000d"/></radialGradient>
      <image x="6" y="4" width="130" href="${path}lunar_surface.png"/>
      <circle cx="70" cy="70" r="65" mask="url(#arc)" fill="url(#shadow)"/>
    </svg>"""
}

def logsOff() {
    device.updateSetting("debugEnable",[value:"false",type:"bool"])
}