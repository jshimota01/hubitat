/**  Authors Notes:
 *  For more information on Tile Builder Grid check out these resources.
 *  Original posting on Hubitat Community forum: https://community.hubitat.com/t/release-tile-builder-build-beautiful-tiles-of-tabular-data-for-your-dashboard/118822
 *  Tile Builder Documentation: https://github.com/GaryMilne/Hubitat-TileBuilder/blob/main/Tile%20Builder%20Grid%20Help.pdf
 *
 *  Copyright 2022 Gary J. Milne
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  License:
 *  You are free to use this software in an un-modified form. Software cannot be modified or redistributed.
 *  You may use the code for educational purposes or for use within other applications as long as they are unrelated to the
 *  production of tabular data in HTML form, unless you have the prior consent of the author.
 *  You are granted a license to use Tile Builder in its standard configuration without limits.
 *  Use of Tile Builder in it's Advanced requires a license key that must be issued to you by the original developer. TileBuilderApp@gmail.com
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 *  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  CHANGELOG
 *  Version 1.0.0 - Initial Public Release
 *  Version 1.0.1 - Added %lastOpen% and %lastClosed% as variables for contacts. Added logic to handle device with a deviceLabel of null. Added logic for Boolean values to dataType(). Cleanup up some logging.
 *  Version 1.0.2 - Bugfix: Fixed typo in line 969\970
 *  Version 1.0.3 - Added Try\Catch logic around device\hub variable retrieval in the event of a deleted or renamed device or hub variable. Split %lastEvent% into %lastEvent% (changed attribute) and %lastEventValue% (changed attribute value)
 *  Version 1.0.4 - Bugfix: Fixes issue with incomplete subscriptions when in Device Group mode.
 *  Version 1.0.5 - Bugfix: Fixes issue where only the first first attribute of interest would be subscribed correctly when in Device Group mode.
 *  Version 1.0.6 - Breaks out attribute subscription to a separate function and adds improved error handling and logging.
 *  Version 1.0.7 - Bugfix: Corrects error where switching from Device Group to Free From prevents multiple rows from being displayed. Adds function checkNulls() to check for any unhandled null values introduced by the user selecting "No Selection" from a dialog box.
 *  Version 1.0.8 - Bugfix: Keywords not working properly, needed Break statements to exit case statement correctly. Eliminates "double refresh" resulting from the "Refresh" action.
 *  Version 1.0.9 - Feature: Added %deviceID% to allow creation of Dynamic URL's using keyword substitution. Some cosmetic changes to Keyword screen.
 *  Version 1.0.9A - Bugfix: Performed datatype check on line 896. Version not revved!
 *  Version 1.1.0 - Feature: Added %sunrise%, %sunset% variables with variations. (Unreleased)
 *  Version 1.2.0 - Features: Added input options for Device Details and Hub Properties when using Free Form mode.  Improved logging and minor UI tweaks. (Unreleased)
 *  Version 2.0.0 - Complete overhaul - see documentation. Preliminary release April 2024. Production release May 18, 2024.
 *  Version 2.0.1 - Bugfix: Spurious characters causing errors.
 *  Version 2.0.2 - Bugfix: Missing controls under certain null conditions.
 *  Version 2.1.0 - Feature: Added Cloud Endpoints as a publishing option for output > 1,024 bytes.
 *  Version 2.1.1 - Bugfix: Corrected issue with tables < 1,024 bytes being sent to a file. Couple of minor tweaks.
 *  Version 2.2.0 - Feature: Restores Footer Class. Adds device rename in Device Group mode. Added built-in variable %count% for number of rows.
 *  Version 2.2.1 - Bugfix: Changed logic in the handler to eliminate the possibility of a non positive number.
 *  Version 2.2.2 - Bugfix: Changed logic in the handler to eliminate the possibility of a non positive number.
 *					Enhancement: Added Alt Image URL as a cleanup option. This gets the weather images from the gitHub server instead.
 *  Version 2.2.3 - Bugfix: Improved handling of device names with restricted characters. Added all Opacity value to the checkNulls() function.
 *  Version 2.2.4 - Enhancement: Added two new rules options Threshold 1 & 2 and Threshold 3 & 4. This allows greater formatting control when two different attributes use similar numeric ranges like blin position and battery.
 *  Version 2.2.5 - Bugfix: Bad declaration with state.vars = [] but should be state.vars = [:] around line 1766
 *  Version 2.2.6 - Bugfix: Displayed variables not refreshing correctly whe number of variables changes.
 *  Version 2.2.7 - Refactor: Fixed compressHexColor regex escaping and fallback return initialization. Standardized null-type string handling and safe string conversions in getDataType(), cleanupCore(), cleanupFreeForm(), and cleanupDeviceGroup(). Fixed capability-change assignment bug in refreshUIafter(). Enforced exact attribute matching boundaries in getVariablesDeviceGroup() to resolve partial key collisions. Added robust null guards across event processing, subscription handling, and cloud endpoint paths.
 *  Version 2.2.8 - Bugfix: Fixed MissingMethodExceptionNoStack event handler failure by passing string method names to subscribeAttribute and subscribeVariable. Fixed regex end-of-string boundary escaping in compressHexColor.
 *  Version 2.2.9 - Bugfix: Fixed NullPointerException in mainPage() on line 745 by applying safe navigation and default fallbacks for tilePreviewWidth, tilePreviewHeight, and all integer conversions across mainPage UI settings. Expanded checkNulls() to guarantee initialization.
 *  Version 2.3.0 - Bugfix: Fixed Base Font Size (#bfs#) omitted from table CSS declaration in makeTable(). Added narrowly scoped style logging for font settings before publication and verified checkNulls() default initialization for bfs.
 *  Version 2.3.1 - Bugfix: Fixed font-family CSS defect by adding single quotes around #tff# placeholders in table and title style declarations. Sanitized quote/backslash characters in fillStyle() and aligned scrubHTML() default font string matching.
 *
 *  Gary Milne - August 14th, 2026 6:08 PM
 *  Refactored - October 18th, 2026
 *
 **/

import groovy.transform.Field
import java.text.SimpleDateFormat
import java.util.Date
import java.time.LocalDate
import java.time.LocalDateTime
import java.text.ParseException
import java.util.regex.Pattern
import java.util.regex.Matcher

//These are supported capabilities. Layout is "device.selector":["attribute","attribute2"].
static def capabilities() {
    return ["accelerationSensor"      : ["acceleration"], "airQuality": ["airQualityIndex"], "alarm": ["alarm"], "audioVolume": ["mute", "volume"], "battery": ["battery"], "beacon": ["presence"], "bulb": ["switch"], "button": ["button", "holdableButton", "numberOfButtons"],
            "carbonDioxideMeasurement": ["carbonMonoxide"], "carbonMonoxideDetector": ["carbonMonoxide"], "chime": ["soundEffects", "soundName"], "colorControl": ["RGB", "color", "colorName", "hue", "saturation"], "colorMode": ["CT", "RGB", "EFFECTS"],
            "colorTemperature"        : ["colorName", "colorTemperature"], "consumable": ["consumableStatus"], "contactSensor": ["contact"], "currentMeter": ["amperage"], "doorControl": ["door"], "doubleTappableButton": ["doubleTapped", "doubleTap", "buttonNumber"],
            "energyMeter"             : ["energy"], "estimatedTimeOfArrival": ["eta"], "fanControl": ["speed", "supportedFanSpeeds"], "filterStatus": ["filterStatus"], "flash": ["rateToFlash"], "garageDoorControl": ["door"], "gasDetector": ["naturalGas"], "healthCheck": ["healthCheck"],
            "holdableButton"          : ["held", "buttonNumber"], "illuminanceMeasurement": ["illuminance"], "imageCapture": ["image"], "indicator": ["indicatorStatus"], "levelPreset": ["levelPreset"], "light": ["switch"], "lightEffects": ["effectName", "lightEffects"],
            "liquidFlowRate"          : ["rate"], "locationMode": ["mode"], "lock": ["lock"], "lockCodes": ["codeChanged", "lockCodes", "maxCodes"], "mediaController": ["activities", "currentActivity"], "mediaInputSource": ["supportedInputs", "mediaInputSource"],
            "mediaTransport"          : ["transportStatus"], "motionSensor": ["motion"], "musicPlayer": ["level", "mute", "status", "trackData", "trackDescription"], "outlet": ["switch"], "powerMeter": ["power"], "powerSource": ["powerSource"], "presenceSensor": ["presence"],
            "pressureMeasurement"     : ["pressure"], "pushableButton": ["numberOfButtons", "pushed"], "relativeHumidityMeasurement": ["humidity"], "relaySwitch": ["switch"], "releasableButton": ["released"], "samsungTV": ["messageButton", "mute", "pictureMode", "soundMode", "volume"],
            "securityKeypad"          : ["codeChanged", "codeLength", "lockCodes", "maxCodes", "securityKeypad"], "shockSensor": ["shock"], "signalStrength": ["ldi", "rssi"], "sleepSensor": ["sleeping"], "smokeDetector": ["smoke"], "soundPressureLevel": ["soundPressureLevel"],
            "soundSensor"             : ["sound"], "speechRecognition": ["phraseSpoken"], "stepSensor": ["goal", "steps"], "switch": ["switch"], "switchLevel": ["level", "duration"], "tv": ["channel", "movieMode", "picture", "power", "sound", "volume"], "tamperAlert": ["tamper"], "telnet": ["networkStatus"],
            "temperatureMeasurement"  : ["temperature"], "thermostat": ["coolingSetpoint", "thermostatFanMode", "thermostatHeatingSetpoint", "thermostatMode", "thermostatOperatingState", "thermostatSchedule", "thermostatSetpoint"], "threeAxis": ["threeAxis"],
            "timedSession"            : ["sessionStatus", "timeRemaining"], "touchSensor": ["touch"], "ultravioletIndex": ["ultravioletIndex"], "valve": ["valve"], "variable": ["variable"], "videoCamera": ["camera", "mute", "settings"], "videoCapture": ["clip"],
            "voltageMeasurement"      : ["voltage", "frequency"], "waterSensor": ["water"], "windowBlind": ["position", "windowBlind", "tilt"], "windowShade": ["position", "windowShade"], "zwMultichannel": ["epEvent", "epInfo"], "pHMeasurement": ["pH"]]
}

//Cloud Endpoint Mapping
mappings { path("/tb") { action: [GET: "getTile"] } }

static def codeDescription() { return ("<b>Tile Builder Grid v2.3.1 (10/18/2026)</b>") }

static def codeVersion() { return (231) }

static def cleanups() {
    return ["None", "Capitalize", "Capitalize All", "Commas", "0 Decimal Places", "1 Decimal Place", "Upper Case", "OW Code to Emoji", "OW Code to PNG", "Image URL", "Alt Image URL", "Remove Tags [] <>"]
}

static def dateFormatsMap() {
    return [1: "To: yyyy-MM-dd HH:mm:ss.SSS", 2: "To: HH:mm", 3: "To: h:mm a", 4: "To: HH:mm:ss", 5: "To: h:mm:ss a", 6: "To: E HH:mm", 7: "To: E h:mm a", 8: "To: EEEE HH:mm", 9: "To: EEEE h:mm a", 10: "To: MM-dd HH:mm", 11: "To: MM-dd h:mm a",   \
                                 12: "To: MMMM dd HH:mm", 13: "To: MMMM dd h:mm a", 14: "To: yyyy-MM-dd HH:mm", 15: "To: dd-MM-yyyy h:mm a", 16: "To: MM-dd-yyyy h:mm a", 17: "To: E @ h:mm a",   \
                                 21: "To: Elapsed Time (dd):hh:mm:ss", 22: "To: Elapsed Time (dd):hh:mm", 23: "To: Remaining Time (dd):hh:mm:ss", 24: "To: Remaining Time (dd):hh:mm"]
}

static def dateFormatsList() { return dateFormatsMap().values() }

static def deviceDetailDurations() {
    return ["lastOnDuration", "lastOffDuration", "lastOpenDuration", "lastClosedDuration", "lastLockedDuration", "lastUnlockedDuration", "lastPresentDuration", "lastNotPresentDuration", "lastActiveDuration", "lastInactiveDuration"]
}

static def deviceDetails() {
    return ["deviceID", "deviceName", "deviceLabel", "lastActivity", "roomName", "lastOn", "lastOff", "lastOpen", "lastClosed", "lastEventName", "lastEventValue", "lastLocked", "lastUnlocked", "lastEventDescription", "lastEventType",
            "lastPresent", "lastNotPresent", "lastActive", "lastInactive"].sort() + deviceDetailDurations()
}

static def hubProperties() {
    return ["sunrise", "sunset", "sunriseTomorrow", "sunsetTomorrow", "hubName", "hsmStatus", "currentMode", "firmwareVersionString", "uptime", "timeZone", "daylightSavingsTime", "currentTime"].sort()
}

static def rules() {
    return ["None", "All Keywords", "All Thresholds", "Threshold 1", "Threshold 2", "Threshold 3", "Threshold 4", "Threshold 5", "Threshold 1 & 2", "Threshold 3 & 4", "Format Rule 1", "Format Rule 2", "Format Rule 3", "Replace Chars"]
}

static def invalidAttributeStrings() { return ["N/A", "n/a", " ", "-", "--", "?", "??"] }

static def elementSize() {
    return ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '30', '40', '50', '75', '100']
}

static def textScale() {
    return ['50', '55', '60', '65', '70', '75', '80', '85', '90', '95', '100', '105', '110', '115', '120', '125', '130', '135', '140', '145', '150', '175', '200', '250', '300', '350', '400', '450', '500']
}

static def fontFamily() {
    return ['Arial', 'Arial Sans Serif', 'Arial Black', 'Brush Script MT', 'Comic Sans MS', 'Courier New', 'Garamond', 'Georgia', 'Hubitat', 'Lucid', 'Monospace', 'Palatino', 'Roboto', 'Tahoma', 'Times New Roman', 'Trebuchet MS', 'Verdana']
}

static def borderStyle() {
    return ['Dashed', 'Dotted', 'Double', 'Groove', 'Hidden', 'Inset', 'Outset', 'Ridge', 'Solid']
}

static def tableStyle() { return ['Collapse', 'Seperate'] }

static def textAlignment() { return ['Left', 'Center', 'Right', 'Justify'] }

static def tableSize() { return ['Auto', '50', '55', '60', '65', '70', '75', '80', '85', '90', '95', '100'] }

static def opacity() { return ['1', '0.9', '0.8', '0.7', '0.6', '0.5', '0.4', '0.3', '0.2', '0.1', '0'] }

static def inactivityTime() {
    return [0: '0 hours', 1: '1 hour', 2: '2 hours', 4: '4 Hours', 8: '8 hours', 12: '12 hours', 24: '1 day', 48: '2 days', 72: '3 days', 168: '1 week', 336: '2 weeks', 730: '1 month', 2190: '3 months', 4380: '6 months', 8760: '1 year']
}

static def deviceLimit() {
    return [0: '0 devices', 1: '1 device', 2: '2 devices', 3: '3 devices', 4: '4 devices', 5: '5 devices', 6: '6 devices', 7: '7 devices', 8: '8 devices', 9: '9 devices', 10: '10 devices', 11: '11 device', 12: '12 devices', 13: '13 devices', 14: '14 devices', 15: '15 devices', 16: '16 devices', 17: '17 devices', 18: '18 devices', 19: '19 devices', 20: '20 devices', 21: '21 device', 22: '22 devices', 23: '23 devices', 24: '24 devices', 25: '25 devices', 26: '26 devices', 27: '27 devices', 28: '28 devices', 29: '29 devices', 30: '30 devices']
}

static def truncateLength() {
    return [99: 'No truncation.', 98: 'First Space', 97: 'Second Space', 96: 'Third Space', 95: 'Fourth Space', 94: 'Fifth Space', 93: 'Sixth Space', 10: '10 characters.', 12: '12 characters.', 15: '15 characters.', 18: '18 characters.', 20: '20 characters.', 22: '22 characters.', 25: '25 characters.', 30: '30 characters.']
}

static def refreshInterval() {
    return [0: 'Never', 1: '1 minute', 2: '2 minutes', 5: '5 minutes', 10: '10 minutes', 15: '15 minutes', 30: '30 minutes', 60: '1 hour', 120: '2 hours', 240: '4 hours', 480: '8 hours', 720: '12 hours', 1440: '24 hours']
}

static def pixels() {
    return ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '-1', '-2', '-3', '-4', '-5', '-6', '-7', '-8', '-9', '-10', '-11', '-12', '-13', '-14', '-15', '-16', '-17', '-18', '-19', '-20']
}

static def borderRadius() {
    return ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30']
}

static def baseFontSize() {
    return ['10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '22', '24', '26', '28', '30', '32', '34', '36', '38', '40']
}

static def tilePreviewList() {
    return [1: '1 x 1', 2: '1 x 2', 3: '1 x 3', 4: '1 x 4', 5: '2 x 1', 6: '2 x 2', 7: '2 x 3', 8: '2 x 4', 9: '2 x 5', 10: '3 x 1', 11: '3 x 2', 12: '3 x 3', 13: '3 x 4', 14: '3 x 5']
}

static def allTileList() {
    return [1: 'tile1', 2: 'tile2', 3: 'tile3', 4: 'tile4', 5: 'tile5', 6: 'tile6', 7: 'tile7', 8: 'tile8', 9: 'tile9', 10: 'tile10', 11: 'tile11', 12: 'tile12', 13: 'tile13', 14: 'tile14', 15: 'tile15', 16: 'tile16', 17: 'tile17', 18: 'tile18', 19: 'tile19', 20: 'tile20', 21: 'tile21', 22: 'tile22', 23: 'tile23', 24: 'tile24', 25: 'tile25']
}

static def filterList() {
    return [0: 'No Filter', 1: 'String ==', 2: 'String !=', 3: 'Numeric ==', 4: 'Numeric <=', 5: 'Numeric >=']
}

static def overrideCategory() {
    return ['Animation', 'Background', 'Border', 'Cell Operations', 'Classes', 'Field Replacement', 'Font', 'Margin & Padding', 'Misc', 'Text', 'Transform']
}

static def messageList() {
    return ['clearOverrides', 'disableOverrides', 'disableKeywords', 'disableThresholds', 'clearDeviceList', 'clearIconBarADevices', 'clearIconBarBDevices']
}

static def unitsMap() {
    return ['None', '°F', '_°F', '°C', '_°C', '%', '_%', 'A', '_A', 'V', '_V', 'W', '_W', 'kWh', '_kWH', 'K', '_K', 'ppm', '_ppm', 'lx', '_lx']
}

static def comparators() { return [0: 'None', 1: '<=', 2: '==', 3: '>='] }

static def htmlScrubLevel() { return [0: 'Basic', 1: 'Normal', 2: 'Aggressive', 3: 'Extreme'] }

static def durationsMap() {
    [lastOnDuration    : ["lastOn", "lastOff"], lastOffDuration: ["lastOff", "lastOn"], lastOpenDuration: ["lastOpen", "lastClosed"], lastClosedDuration: ["lastClosed", "lastOpen"], lastUnlockedDuration: ["lastUnlocked", "lastLocked"],
     lastLockedDuration: ["lastLocked", "lastUnlocked"], lastPresentDuration: ["lastPresent", "lastNotPresent"], lastNotPresentDuration: ["lastNotPresent", "lastPresent"], lastActiveDuration: ["lastActive", "lastInactive"], lastInactiveDuration: ["lastInactive", "lastActive"]]
}

definition(
        name: "Tile Builder - Grid (Custom)",
        description: "Monitors multiple attributes for a grid of device information. Publishes an HTML table of results for a quick and attractive display in the Hubitat Dashboard environment.",
        importUrl: "https://raw.githubusercontent.com/GaryMilne/Hubitat-TileBuilder/main/Grid.groovy",
        namespace: "garyjmilne", author: "Gary J. Milne", category: "Utilities", iconUrl: "", iconX2Url: "", iconX3Url: "", singleThreaded: true, parent: "garyjmilne:Tile Builder", installOnOpen: true
)

preferences { page(name: "mainPage") }

def mainPage() {
    if (isLogAppPerformance) log.info("Start of Main Page: " + (now() - state.refresh) / 1000 + " seconds")
    
    //Handles the initialization of new variables added after the original release.
    def currentVersion = state.variablesVersion?.toString()?.isInteger() ? state.variablesVersion.toString().toInteger() : 0
	if (currentVersion < codeVersion()) updateVariables()

    //Basic initialization for the initial release
    if (state.initialized == null) initialize()
    checkNulls()

    //Checks to see if there are any messages for this child app. This is used to recover broken child apps from certain error conditions
    myMessage = parent.messageForTile(app.label)
    if (myMessage != "") supportFunction(myMessage)

    //See if the user has selected a different capability. If so a flag is set and the device list is cleared on the refresh.
    isMyCapabilityChanged()

    //This causes refresh to run every time the page is refreshed. It may run twice if the called action also initiates a refresh.
    refreshUIbefore()
    dynamicPage(name: "mainPage", uninstall: true, install: true, singleThreaded: true) {

        //This section is used for the control to go full screen or normal.
        if (isLogAppPerformance) log.info("Start of Full Screen\\Normal Section: " + (now() - state.refresh) / 1000 + " seconds")
        section(hideable: false, hidden: false) {
            if (state.flags.isEnhancedView == true) {
                UIsettings = "<style>#divMainUIMenu{width: 0px !important;}</style>"
                UIsettings = UIsettings + "<style>#divMainUIHeader{display:none !important}</style>"
                UIsettings = UIsettings + "<style>#divLogoContainer{display:none !important}</style>"
                UIsettings = UIsettings + "<style>#divAppUISettings{display:none !important}</style>"
                UIsettings = UIsettings + "<style>#divMainUIContentContainer{margin-top: 12px !important}</style>"
                paragraph buttonLink('EnhancedView', largeText("⏭️ Tile Builder Grid <b>𝄜</b>"), 0) + UIsettings
            } else paragraph buttonLink('EnhancedView', largeText("⏮️ Tile Builder Grid <b>𝄜</b>"), 0) + "<style>h3.pre{display:none !important;}</style>"
        }
        
        if (isLogAppPerformance) log.info("Start of Layout Mode Section: " + (now() - state.refresh) / 1000 + " seconds")
        section(hideable: true, hidden: state.hidden.LayoutMode, title: buttonLink('btnHideLayoutMode', getSectionTitle("Layout Mode"), 20)) {
            input(name: "layoutMode", title: red("<b>Layout Mode</b>"), type: "enum", options: ["Device Group", "Free Form"], submitOnChange: true, defaultValue: "Device Group", style: "width:10%")
            paragraph "<mark>" + red("<b>Important!!!</b> If you switch between modes the current configuration is wiped and you start over. !!!") + "</mark>"
            myString = "<b>Tile Builder Grid</b> has two modes of operation:<br>"
            myString += "<b>1) Device Group:</b> A Device Group is a set of devices with at least one common attribute such as temperature, switch or contact for example. You fill out a one line template and the table is filled using that template for each device.<br>"
            myString += "Device Groups are used for showing the status of a list of devices. For example: A Lights table could show the On/Off status, the Bulb Color and the Dimmer setting for each of the bulbs.<br>"
            myString += "<b>2) Free Form:</b> In Free Form you determine the exact size of the table and configure a template for each cell. The template is filled with the actual values at run-time.<br>"
            myString += "Free Form mode is suited for showing a lot of mixed attribute data, often from a single device, such as Hub Info, Weather etc or mixed data such as Security or HVAC.<br>"
            myString += "In both cases the template is filled out using %variable% to represent an attribute and the can be accompanied by other text such us HTML tags or units. Example: Conditions are [b]%temperature%°F[/b] & [b]%humidity%%RH.[/b]<br>"
            paragraph summary("Layout Mode", myString)
        }

        //If the layoutMode has changed we reset everything that matters.
        if (state.layoutMode != layoutMode.toString()) {
            resetVariables()
        }
        if (isLogAppPerformance) log.info("Start of Device Group Section: " + (now() - state.refresh) / 1000 + " seconds")

        //Start of Device Group Section
        if (layoutMode.toString() == "Device Group") {
            section(hideable: true, hidden: state.hidden.DeviceGroup, title: buttonLink('btnHideDeviceGroup', getSectionTitle("Device Group"), 20)) {
                input(name: "myCapability", title: "<b>Select Filter (Optional)</b>", type: "enum", options: capabilities().keySet().collect { it }.sort(), submitOnChange: true, width: 2, defaultValue: 1)
                if (myCapability != null) input "myDeviceList", "capability.$myCapability", title: "<b>Select Devices</b> " + dodgerBlue("(%deviceLabel%)"), multiple: true, required: false, submitOnChange: true, width: 3
                else input "myDeviceList", "capability.*", title: "<b>Select Devices</b> " + dodgerBlue("(%deviceLabel%)"), multiple: true, required: false, submitOnChange: true, width: 3
                input(name: "myVariableCount", title: "<b>Variable Count?</b>", type: "enum", options: [1, 2, 3, 4, 5, 6], submitOnChange: true, defaultValue: 0, style: "margin-left:20px; width:10%")
                input(name: "myColumns", title: "<b>Column Count?</b>", type: "enum", options: [1, 2, 3, 4, 5], submitOnChange: true, defaultValue: 0, newLineAfter: true, style: "margin-left:20px; width:10%")

                def variableCount = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0)

                for (int i = 1; i <= variableCount; i++) {
                    boolean isNewLine = (i % 4 == 0)
                    result = "(%" + settings["myAttribute$i"] + "%)"
                    if (result.contains("null")) result = invalidAttribute.toString()
                    if (myCapability != null) input(name: "myAttribute${i}", title: dodgerBlue("<b>Variable</b> " + result), type: "enum", options: capabilities().values().collectMany { it }.unique().sort(), submitOnChange: true, defaultValue: "Var${i}", newLine: isNewLine, style: "margin-left:10px;width:12%")
                    else input (name: "myAttribute${i}", title: dodgerBlue("<b>Variable</b> " + result ), type: "enum", options: capabilities().values().collectMany {it}.unique().sort(), submitOnChange: true, defaultValue: "Var${i}", newLine:isNewLine, style: "margin-left:10px;width:12%")
                    input "actionA${i}", "enum", title: "<b>Cleanup</b>", options: cleanups(), defaultValue: "None", required: false, submitOnChange: true, newLine: false, style: "margin-left:10px;width:8%"
                    input "actionB${i}", "enum", title: "<b>Rules</b>", options: rules(), defaultValue: "None", required: false, submitOnChange: true, newLine: false, style: "margin-right:20px;width:8%"
                    myAttr = settings["myAttribute$i"].toString()
                    app.updateSetting("name$i", [value: myAttr + i, type: "string"])
                }
				
				input (name: "isShowDeviceNameModification", type: "bool", title: "<b>Show Device Name Modification</b>", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 3, newLine:true )    
				if (isShowDeviceNameModification == true) {
					input (name: "mySearchText1", title: "<b>Search Device Text #1</b>", type: "string", submitOnChange:true, width:2, defaultValue: "?", newLine:true)
					input (name: "myReplaceText1", title: "<b>Replace Device Text #1</b>", type: "string", submitOnChange:true, width:2, defaultValue: "")
					input (name: "mySearchText2", title: "<b>Search Device Text #2</b>", type: "string", submitOnChange:true, width:2, defaultValue: "?", newLine:true)
					input (name: "myReplaceText2", title: "<b>Replace Device Text #2</b>", type: "string", submitOnChange:true, width:2, defaultValue: "")
					input (name: "mySearchText3", title: "<b>Search Device Text #3</b>", type: "string", submitOnChange:true, width:2, defaultValue: "?", newLine:true)
					input (name: "myReplaceText3", title: "<b>Replace Device Text #3</b>", type: "string", submitOnChange:true, width:2, defaultValue: "")
					input (name: "mySearchText4", title: "<b>Search Device Text #4</b>", type: "string", submitOnChange:true, width:2, defaultValue: "?", newLine:true)
					input (name: "myReplaceText4", title: "<b>Replace Device Text #4</b>", type: "string", submitOnChange:true, width:2, defaultValue: "")
					input (name: "mySearchText5", title: "<b>Search Device Text #5</b>", type: "string", submitOnChange:true, width:2, defaultValue: "?", newLine:true)
					input (name: "myReplaceText5", title: "<b>Replace Device Text #5</b>", type: "string", submitOnChange:true, width:2, defaultValue: "")
				}
				paragraph line(1)
                paragraph summary("Cleanup and Rules Help", parent.textCleanupsRules())

                paragraph line(1)
                input "gatherDeviceDetails", "enum", title: "<b>Gather Device Details</b>", options: deviceDetails(), multiple: true, submitOnChange: true, width: 4, required: false, newLine: false
                input(name: "defaultDateTimeFormat", title: "<b>Default Date Time Format</b>", type: "enum", options: dateFormatsMap(), submitOnChange: true, defaultValue: 1, width: 2)
                input(name: "myTruncateLength", title: "<b>Truncate Device Name\\Label</b>", type: "enum", options: truncateLength(), submitOnChange: true, defaultValue: 99, width: 2)
                input(name: "invalidAttribute", title: "<b>Invalid Attribute String</b>", type: "enum", options: invalidAttributeStrings(), submitOnChange: true, defaultValue: "N/A", width: 2)
                paragraph line(2)

                def numColsVal = (myColumns?.toString()?.isInteger() ? myColumns.toInteger() : 1)
                def myColumnWidth = 12 / (numColsVal > 0 ? numColsVal : 1)
                for (j = 1; j <= numColsVal; j++) {
                    if (j == 1) input "Template${j}", "string", title: "<b>Template Column ${j}</b>", submitOnChange: true, defaultValue: "%deviceLabel%", newLine: false, width: myColumnWidth.toInteger()
                    else input "Template${j}", "string", title: "<b>Template Column ${j}</b>", submitOnChange: true, defaultValue: "%variable%", newLine: false, width: myColumnWidth.toInteger()
                }
				
                paragraph summary("Text Fields and Variables Help", parent.textFieldNotesGrid())

                createCellTemplates()
                varString = getVariablesDeviceGroup()
                makeTable()
            }
        }
        //End of Device Group Section

        //Start of Free Form Layout Section
        if (isLogAppPerformance) log.info("Start of Free Form Section: " + (now() - state.refresh) / 1000 + " seconds")
        if (layoutMode.toString() == "Free Form") {
            section(hideable: true, hidden: state.hidden.Variables, title: buttonLink('btnHideVariables', getSectionTitle("Variables"), 20)) {
                input(name: "myVariableCount", title: "<b>Variable Count?</b>", type: "enum", options: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40], submitOnChange: true, defaultValue: 0, style: "width:10%")
                input(name: "varColumns", title: "<b>Variable Columns?</b>", type: "enum", options: [1, 2], submitOnChange: true, defaultValue: 1, style: "margin-left:20px; width:10%")
                input "showVariables", "enum", title: "<b>Show Variables?</b>", options: ["Hide Variables", "Show Variables", "Show Variables & HTML"], defaultValue: "Show Variables", required: false, submitOnChange: true, width: 2, newLine: false, style: "width:12%;margin-left:20px;"
                input "defaultDevice", "capability.*", title: "<b>Default Device</b> (Optional)", multiple: false, required: false, submitOnChange: true, newLine: false, style: "width:12%;margin-left:20px;"
                input(name: "invalidAttribute", title: "<b>Invalid Attribute String</b>", type: "enum", options: invalidAttributeStrings(), submitOnChange: true, defaultValue: "N/A", style: "width:12%;margin-left:20px;")
                def variableCount = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0)
                if (variableCount > 0) input(name: 'btnClearLastVariable', type: 'button', title: ("Clear Var " + myVariableCount.toString() + "<hr>"), backgroundColor: '#00a2ed', textColor: 'yellow', submitOnChange: true, width: 1, style: "margin-left:20px;")
                input(name: "Refresh", type: "button", title: "Submit Changes", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 1)

                def varColsVal = (varColumns?.toString()?.isInteger() ? varColumns.toInteger() : 1)
                for (int i = 1; i <= variableCount; i++) {
                    boolean isEven = (i % 2 == 0)
                    if (varColsVal == 1) isEven = false
                    if (!isEven) input "variableSource${i}", "enum", title: dodgerBlue("<b>Source #$i</b>"), options: ["Default Device", "Device Attribute", "Device Detail", "Hub Property", "Hub Variable"], submitOnChange: true, width: 1, defaultValue: "Default Device", newLine: !isEven, style: varColsVal == 1 ? "width:12%" : "width:8%"
                    else input "variableSource${i}", "enum", title: dodgerBlue("<b>Source #$i</b>"), options: ["Default Device", "Device Attribute", "Device Detail", "Hub Property", "Hub Variable"], submitOnChange: true, width: 1, defaultValue: "Default Device", newLine: !isEven, style: varColsVal == 1 ? "width:12%" : "width:8%; margin-left:20px;"
                    input "name${i}", "string", title: "<b>Var Name</b>", submitOnChange: true, width: 1, defaultValue: "Var${i}", newLine: false, style: varColsVal == 1 ? "width:6%" : "width:6%;margin-left:10px"

                    if (settings["variableSource${i}"] == "Default Device") {
                        input "myAttribute${i}", "enum", title: "&nbsp<b>Attribute</b>", options: getAttributeList(settings["defaultDevice"]), multiple: false, submitOnChange: true, width: 2, required: false, newLine: false, style: varColsVal == 1 ? "margin-left:20px;width:26.5%" : "margin-left:10px;width:16.5%"
                        myDevice$i = null
                    }

                    if (settings["variableSource${i}"] == "Device Attribute") {
                        input "myDevice${i}", "capability.*", title: "<b>Device</b>", multiple: false, required: false, submitOnChange: true, width: 2, newLine: false, style: varColsVal == 1 ? "margin-left:20px; width:13.5%" : "margin-left:10px;width:8%"
                        input "myAttribute${i}", "enum", title: "&nbsp<b>Attribute</b>", options: getAttributeList(settings["myDevice${i}"]), multiple: false, submitOnChange: true, width: 2, required: true, newLine: false, style: varColsVal == 1 ? "margin-left:20px;width:12%" : "margin-left:10px;width:8%"
                    }

                    if (settings["variableSource${i}"] == "Device Detail") {
                        input "myDevice${i}", "capability.*", title: "<b>Device</b>", multiple: false, required: false, submitOnChange: true, width: 2, newLine: false, style: varColsVal == 1 ? "margin-left:20px; width:13.5%" : "margin-left:10px;width:8%"
                        input "myAttribute${i}", "enum", title: "&nbsp<b>Device Detail</b>", options: deviceDetails(), multiple: false, submitOnChange: true, width: 2, required: true, newLine: false, style: varColsVal == 1 ? "margin-left:20px;width:12%" : "margin-left:10px;width:8%"
                    }

                    if (settings["variableSource${i}"] == "Hub Property") {
                        input "myAttribute${i}", "enum", title: "&nbsp<b>Hub Property</b>", options: hubProperties(), multiple: false, submitOnChange: true, width: 2, required: true, newLine: false, style: varColsVal == 1 ? "margin-left:20px;width:26.5%" : "margin-left:10px;width:16.5%"
                    }

                    if (settings["variableSource${i}"] == "Hub Variable") {
                        input "myHubVariable${i}", "enum", title: "<b>Hub Variable</b>", submitOnChange: true, width: 2, options: getAllGlobalVars().findAll { it.value.type != null }.keySet().collect().sort { it.capitalize() }, style: varColsVal == 1 ? "margin-left:20px;width:26.5%" : "margin-left:10px;width:16%"
                    }
                    input "actionA${i}", "enum", title: "<b>Cleanup</b>", options: cleanups() + dateFormatsList(), defaultValue: "None", required: false, submitOnChange: true, width: 2, newLine: false, style: varColsVal == 1 ? "margin-left:10px;width:15%" : "margin-left:5px;width:8%"
                    input "actionB${i}", "enum", title: "<b>Rules</b>", options: rules(), defaultValue: "None", required: false, submitOnChange: true, newLine: false, style: varColsVal == 1 ? "margin-left:10px;width:12%" : "margin-left:5px;width:8%"
                }

                input(name: "Refresh", type: "button", title: "Submit Changes", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 1)
                paragraph summary("Cleanup and Rules Help", parent.textCleanupsRules())

                varString = getVariablesFreeForm()

                if ((variableSource1 != null) && showVariables != "Hide Variables") {
                    paragraph line(1); paragraph state.highlightStyles + varString
                }
                makeTable()
            }

            section { if (state.hidden.Variables == true && variableSource1 != null) { paragraph line(1); paragraph state.highlightStyles + varString } }

            section(hideable: true, hidden: state.hidden.GridLayout, title: buttonLink('btnHideGridLayout', getSectionTitle("Grid Layout"), 20)) {
                if (isLogAppPerformance) log.info("Start of Grid Layout Section: " + (now() - state.refresh) / 1000 + " seconds")
                input(name: "rows", title: "<b>How Many Data Rows?</b>", type: "enum", options: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30], submitOnChange: true, width: 2, defaultValue: 0)
                input(name: "myColumns", title: "<b>How Many Data Columns?</b>", type: "enum", options: [1, 2, 3, 4, 5], submitOnChange: true, width: 2, defaultValue: 0)
                input(name: "autoRefreshGrid", title: "<b>Auto Refresh Table on Save?</b>", type: "enum", options: ["True", "False"], submitOnChange: true, width: 2, defaultValue: "True", newLineAfter: true)

                def gridRowsVal = (rows?.toString()?.isInteger() ? rows.toInteger() : 1)
                def gridColsVal = (myColumns?.toString()?.isInteger() ? myColumns.toInteger() : 1)
                def columnWidth = 12 / (gridColsVal > 0 ? gridColsVal : 1)

                for (int i = 1; i <= gridRowsVal; i++) {
                    for (j = 1; j <= gridColsVal; j++) {
                        def isNewLineAfter = true
                        def cell = "R${i}C${j}"
                        if (j / gridColsVal < 1) isNewLineAfter = false
                        input "$cell", "string", title: italic("<b>${cell}</b>"), submitOnChange: (autoRefreshGrid != null) ? autoRefreshGrid.toBoolean() : false, defaultValue: "?", newLine: false, newLineAfter: isNewLineAfter, width: columnWidth.toInteger()
                    }
                }
                paragraph line(1)
                paragraph summary("Text Fields and Variables Help", parent.textFieldNotesGrid())
            }
        }
        // End of Free Form Layout

        //Start of Design Table Section
        section(hideable: true, hidden: state.hidden.Design, title: buttonLink('btnHideDesign', getSectionTitle("Design"), 20)) {
            if (isLogAppPerformance) log.info("Start of Design Table Section: " + (now() - state.refresh) / 1000 + " seconds")
            input(name: "Refresh", type: "button", title: "Refresh Table", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 2)
            input(name: "isCustomize", type: "bool", title: "Customize Table", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 2)
            if (isCustomize == true) {
                input(name: "isCompactDisplay", type: "bool", title: "Compact Display", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 2)

                paragraph "<style>#buttons {font-family: Arial, Helvetica, sans-serif;width:100%;text-align:'Center'} #buttons td,tr {background:#00a2ed;color:#FFFFFF;text-align:Center;opacity:0.75;padding: 8px} #buttons td:hover {background: #27ae61;opacity:1}</style>"
                part1 = "<table id='buttons'><td>" + buttonLink('General', 'General', 1) + "</td><td>" + buttonLink('Title', 'Title', 2) + "</td><td>" + buttonLink('Headers', 'Headers', 3) + "</td>"
                part2 = "<td>" + buttonLink('Borders', 'Borders', 4) + "</td><td>" + buttonLink('Rows', 'Rows', 5) + "</td><td>" + buttonLink('Footer', 'Footer', 6) + "</td>"
                part3 = "<td>" + buttonLink('Highlights', 'Highlights', 7) + "</td><td>" + buttonLink('Styles', 'Styles', 8) + "</td><td>" + buttonLink('Advanced', 'Advanced', 9) + "</td>"
                table = part1 + part2 + part3 + "</table>"
                paragraph table

                //General Properties
                if (activeButton == 1) {
                    if (isLogAppPerformance) log.info("Start of General Properties Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("General Properties")
                    input(name: "tw", type: "enum", title: bold("Table Width %"), options: tableSize(), required: false, defaultValue: "90", submitOnChange: true, width: 1)
                    input(name: "th", type: "enum", title: bold("Table Height %"), options: tableSize(), required: false, defaultValue: "auto", submitOnChange: true, width: 1)
                    input(name: "tbc", type: "color", title: bold2("Background Color", tbc), required: false, defaultValue: "#ffffff", width: 2, submitOnChange: true, style: "width:15%")
                    input(name: "tbo", type: "enum", title: bold("Background Opacity"), options: opacity(), required: false, defaultValue: "1", submitOnChange: true, style: "width:10%")
                    input(name: "tff", type: "enum", title: bold("Font"), options: fontFamily(), required: false, defaultValue: "Roboto", submitOnChange: true, width: 1, style: "width:10%")
                    input(name: "bm", type: "enum", title: bold("Border Mode"), options: tableStyle(), required: false, defaultValue: "collapse", submitOnChange: true, width: 1)
                    input(name: "bfs", type: "enum", title: bold("Base Font Size"), options: baseFontSize(), required: false, defaultValue: "18", submitOnChange: true, width: 1)
                    input(name: "iFrameColor", type: "color", title: bold2("Dashboard Color", iFrameColor), required: false, defaultValue: "#000000", submitOnChange: true, style: "width:15%", newLine: true)
                    input(name: "tilePreviewWidth", type: "enum", title: bold("Preview Width (x200px)"), options: [1, 2, 3, 4, 5, 6, 7, 8], required: false, defaultValue: 2, submitOnChange: true, style: "width:12%")
                    input(name: "tilePreviewHeight", type: "enum", title: bold("Preview Height (x190px)"), options: [1, 2, 3, 4, 5, 6, 7, 8], required: false, defaultValue: 2, submitOnChange: true, style: "width:12%")
                    input(name: "isComment", type: "bool", title: "<b>Add comment?</b>", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 1, style: "width:10%")
                    if (isComment == true) {
                        input(name: "comment", type: "text", title: bold("Comment"), required: false, defaultValue: "?", width: 3, submitOnChange: true)
                    }
                    input(name: "isFrame", type: "bool", title: bold("Add Frame"), required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 2)
                    if (isFrame == true) {
                        input(name: "fbc", type: "color", title: bold2("Frame Color", fbc), required: false, defaultValue: "#90C226", submitOnChange: true, width: 3)
                    }
                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("General Notes", parent.generalNotes())
                    }
                }

                //Title Properties
                if (activeButton == 2) {
                    if (isLogAppPerformance) log.info("Start of Title Properties Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("Title Properties")
                    input(name: "isTitle", type: "bool", title: "<b>Display Title?</b>", required: false, multiple: false, defaultValue: false, submitOnChange: true, style: "width:10%")
                    if (isTitle == true) {
                        input(name: "tt", title: "<b>Title Text</b>", type: "string", required: false, defaultValue: "My Title", width: 3, submitOnChange: true)
                        input(name: "ts", type: "enum", title: bold("Text Size %"), options: textScale(), required: false, defaultValue: "150", submitOnChange: true, style: "width:10%")
                        input(name: "ta", type: "enum", title: bold("Alignment"), options: textAlignment(), required: false, defaultValue: "Center", width: 2, submitOnChange: true, style: "width:10%")
                        input(name: "tc", type: "color", title: bold2("Text Color", tc), required: false, defaultValue: "#000000", width: 3, submitOnChange: true, style: "width:15%")
                        input(name: "to", type: "enum", title: bold("Text Opacity"), options: opacity(), required: false, defaultValue: "1", submitOnChange: true, style: "width:10%")
                        input(name: "tp", type: "enum", title: bold("Text Padding"), options: elementSize(), required: false, defaultValue: "0", width: 2, submitOnChange: true, style: "width:10%")
                    }
                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("Title Notes", parent.titleNotes())
                    }
                }

                //Header Properties
                if (activeButton == 3) {
                    if (isLogAppPerformance) log.info("Start of Header Properties Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("Header Properties")
                    input(name: "isHeaders", type: "bool", title: "<b>Display Headers?</b>", required: false, multiple: false, defaultValue: false, submitOnChange: true, style: "width:10%")
                    if (isHeaders == true) {
                        input(name: "isMergeHeaders", type: "enum", title: bold("Merge Headers"), options: ["Do Not Merge", "Merge All", "Merge 2 & 3", "Merge 2, 3 & 4"], required: false, defaultValue: "Do Not Merge", submitOnChange: true, style: "width:10%", newLineAfter: true)
                        input(name: "R0C1", type: "text", title: bold("Heading 1"), required: false, defaultValue: "Device", submitOnChange: true, width: 2)
                        def hdrColsVal = (myColumns?.toString()?.isInteger() ? myColumns.toInteger() : 1)
                        if (hdrColsVal >= 2) input(name: 'R0C2', type: 'text', title: bold('Heading 2'), required: false, defaultValue: 'State', submitOnChange: true, width: 2)
                        if (hdrColsVal >= 3) input(name: 'R0C3', type: 'text', title: bold('Heading 3'), required: false, defaultValue: 'Other 1', submitOnChange: true, width: 2)
                        if (hdrColsVal >= 4) input(name: 'R0C4', type: 'text', title: bold('Heading 4'), required: false, defaultValue: 'Other 2', submitOnChange: true, width: 2)
                        if (hdrColsVal >= 5) input(name: 'R0C5', type: 'text', title: bold('Heading 5'), required: false, defaultValue: 'Other 3', submitOnChange: true, width: 2)

                        input(name: "hts", type: "enum", title: bold("Text Size %"), options: textScale(), required: false, defaultValue: "125", submitOnChange: true, style: "width:10%", newLine: true)
                        input(name: "hta", type: "enum", title: bold("Alignment"), options: textAlignment(), required: false, defaultValue: 2, submitOnChange: true, style: "width:10%")
                        input(name: "htc", type: "color", title: bold2("Text Color", htc), required: false, defaultValue: "#000000", submitOnChange: true, style: "width:15%")
                        input(name: "hto", type: "enum", title: bold("Text Opacity"), options: opacity(), required: false, defaultValue: "1", submitOnChange: true, style: "width:10%")
                        input(name: "hp", type: "enum", title: bold("Text Padding"), options: elementSize(), required: false, defaultValue: "0", submitOnChange: true, style: "width:10%")
                        input(name: "hbc", type: "color", title: bold2("Background Color", hbc), required: false, defaultValue: "#90C226", submitOnChange: true, style: "width:15%")
                        input(name: "hbo", type: "enum", title: bold("Background Opacity"), options: opacity(), required: false, defaultValue: "1", submitOnChange: true, style: "width:10%")
                    }
                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("Header Notes", parent.headerNotes())
                    }
                }

                //Border Properties
                if (activeButton == 4) {
                    if (isLogAppPerformance) log.info("Start of Border Properties Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("Border Properties")
                    input(name: "isBorder", type: "bool", title: "<b>Display Borders?</b>", required: false, multiple: false, defaultValue: true, submitOnChange: true, style: "width:10%")
                    if (isBorder == true) {
                        input(name: "bs", type: "enum", title: bold("Style"), options: borderStyle(), required: false, defaultValue: "Solid", submitOnChange: true, style: "width:10%")
                        input(name: "bw", type: "enum", title: bold("Width"), options: elementSize(), required: false, defaultValue: 2, submitOnChange: true, style: "width:10%")
                        input(name: "bc", type: "color", title: bold2("Border Color", bc), required: false, defaultValue: "#000000", submitOnChange: true, style: "width:15%")
                        input(name: "bo", type: "enum", title: bold("Opacity"), options: opacity(), required: false, defaultValue: "1", submitOnChange: true, style: "width:10%")
                        input(name: "br", type: "enum", title: bold("Radius"), options: borderRadius(), required: false, defaultValue: "0", submitOnChange: true, style: "width:10%")
                        input(name: "bp", type: "enum", title: bold("Padding"), options: elementSize(), required: false, defaultValue: "0", submitOnChange: true, style: "width:10%")
                    }
                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("Border Notes", parent.borderNotes())
                    }
                }

                //Row Properties
                if (activeButton == 5) {
                    if (isLogAppPerformance) log.info("Start of Row Properties Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("Data Row Properties")
                    input(name: "rts", type: "enum", title: bold("Text Size %"), options: textScale(), required: false, defaultValue: "100", submitOnChange: true, style: "width:10%")
                    input(name: "rta", type: "enum", title: bold("Alignment"), options: textAlignment(), required: false, defaultValue: 15, submitOnChange: true, style: "width:10%")
                    input(name: "rtc", type: "color", title: bold2("Text Color", rtc), required: false, defaultValue: "#000000", submitOnChange: true, style: "width:15%")
                    input(name: "rto", type: "enum", title: bold("Text Opacity"), options: opacity(), required: false, defaultValue: "1", submitOnChange: true, style: "width:10%")
                    input(name: "rp", type: "enum", title: bold("Text Padding"), options: elementSize(), required: false, defaultValue: "0", submitOnChange: true, style: "width:10%")
                    input(name: "rbc", type: "color", title: bold2("Background Color", rbc), required: false, defaultValue: "#BFE373", submitOnChange: true, style: "width:15%")
                    input(name: "rbo", type: "enum", title: bold("Background Opacity"), options: opacity(), required: false, defaultValue: "1", submitOnChange: true, style: "width:10%")
                    input(name: "isAlternateRows", type: "bool", title: bold("Use Alternate<br>Row Colors?"), required: false, defaultValue: true, submitOnChange: true, style: "width:10%")
                    if (isAlternateRows == true) {
                        input(name: "ratc", type: "color", title: bold2("Alternate Text Color", ratc), required: false, defaultValue: "#000000", submitOnChange: true, style: "width:15%", newLine: true)
                        input(name: "rabc", type: "color", title: bold2("Alternate Background Color", rabc), required: false, defaultValue: "#E9F5CF", submitOnChange: true, style: "width:15%")
                    }
                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("Row Notes", parent.rowNotes())
                    }
                }

                //Footer Properties
                if (activeButton == 6) {
                    if (isLogAppPerformance) log.info("Start of Footer Properties Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("Footer Properties")
                    input(name: "isFooter", type: "bool", title: "<b>Display Footer?</b>", required: false, multiple: false, defaultValue: true, submitOnChange: true, style: "width:10%")
                    if (isFooter == true) {
                        input(name: "ft", type: "text", title: bold("Footer Text"), required: false, defaultValue: "%time%", width: 3, submitOnChange: true)
                        input(name: "fs", type: "enum", title: bold("Text Size %"), options: textScale(), required: false, defaultValue: "50", submitOnChange: true, style: "width:10%")
                        input(name: "fa", type: "enum", title: bold("Alignment"), options: textAlignment(), required: false, defaultValue: "Center", submitOnChange: true, style: "width:10%")
                        input(name: "fc", type: "color", title: bold2("Text Color", fc), required: false, defaultValue: "#000000", submitOnChange: true, style: "width:15%")
                    }
                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("Footer Notes", parent.footerNotes())
                    }
                }

                //Highlight Properties
                if (activeButton == 7) {
                    if (isLogAppPerformance) log.info("Start of Highlights Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("Highlights")

                    //Keywords
                    if (state.show.Keywords == true) {
                        input(name: 'btnShowKeywords', type: 'button', title: 'Show Keywords ▼', backgroundColor: 'navy', textColor: 'white', submitOnChange: true, width: 2)
                        input(name: "myKeywordCount", title: "<b>How Many Keywords?</b>", type: "enum", options: [0, 1, 2, 3, 4, 5], submitOnChange: true, defaultValue: 0, style: "width:12%;margin-top:-15px;margin-left:-5%", newLineAfter: true)
                        def kwCountVal = (myKeywordCount?.toString()?.isInteger() ? myKeywordCount.toInteger() : 0)
                        for (int i = 1; i <= 5; i++) {
                            if (kwCountVal >= i) {
                                input(name: "kop${i}", type: "enum", title: bold("Keyword Match Type"), options: ["Value Matches Keyword (Match Case)", "Value Matches Keyword (Ignore Case)", "Value Contains Keyword (Match Case)", "Value Contains Keyword (Ignore Case)"], defaultValue: "Value Contains Keyword (Ignore Case)", submitOnChange: true, newLine: true, style: "width:17.5%")
                                input(name: "k${i}", type: "text", title: bold("Keyword #${i}"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, newLine: false, style: "width:8%")
                                input(name: "ktr${i}", type: "text", title: bold("Replacement Text #${i}"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, style: "width:50%")
                                input(name: "hc${i}", type: "color", title: bold2(("Color"), settings["hc$i"]), required: false, submitOnChange: true, style: "width:10%")
                                input(name: "hts${i}", type: "enum", title: bold("Text Scale"), options: textScale(), required: false, submitOnChange: true, defaultValue: "125", newLineAfter: true, style: "width:9%")
                            }
                        }
                    } else input(name: 'btnShowKeywords', type: 'button', title: 'Show Keywords ▶', backgroundColor: 'dodgerBlue', textColor: 'white', submitOnChange: true, width: 2, newLineAfter: true)

                    //Thresholds
                    if (state.show.Thresholds == true) {
                        input(name: 'btnShowThresholds', type: 'button', title: 'Show Thresholds ▼', backgroundColor: 'navy', textColor: 'white', submitOnChange: true, width: 2, style: "margin-top:20px")
                        input(name: "myThresholdCount", title: "<b>How Many Thresholds?</b>", type: "enum", options: [0, 1, 2, 3, 4, 5], submitOnChange: true, width: 3, defaultValue: 0, style: "width:12%;margin-top:10px;margin-left:-5%", newLineAfter: true)
                        def thCountVal = (myThresholdCount?.toString()?.isInteger() ? myThresholdCount.toInteger() : 0)
                        for (int i = 6; i <= 10; i++) {
                            if (thCountVal >= i - 5) {
                                myColor = settings["hc$i"]
                                input(name: "top${i}", type: "enum", title: bold("Threshold ${i - 5} (Op. #${i})"), required: false, options: comparators(), displayDuringSetup: true, defaultValue: 0, submitOnChange: true, width: 2, newLine: true)
                                input(name: "tcv${i}", type: "number", title: bold("Comparison Value #${i}"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, width: 2)
                                input(name: "ttr${i}", type: "text", title: bold("Replacement Text #${i}"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, width: 2)
                                input(name: "hc${i}", type: "color", title: bold2(("Highlight ${i} Color"), settings["hc$i"]), required: false, submitOnChange: true, width: 2)
                                input(name: "hts${i}", type: "enum", title: bold("Highlight ${i} Text Scale"), options: textScale(), required: false, submitOnChange: true, defaultValue: "125", width: 2, newLineAfter: true)
                            }
                        }
                    } else input(name: 'btnShowThresholds', type: 'button', title: 'Show Thresholds ▶', backgroundColor: 'dodgerBlue', textColor: 'white', submitOnChange: true, width: 2, newLine: true, newLineAfter: true)

                    //FormatRules
                    if (state.show.FormatRules == true) {
                        input(name: 'btnShowFormatRules', type: 'button', title: 'Show Format Rules ▼', backgroundColor: 'navy', textColor: 'white', submitOnChange: true, width: 3, newLine: true, newLineAfter: true)
                        input(name: "fr1", type: "text", title: bold("Format Rule 1"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, width: 10, newLine: true)
                        input(name: "fr2", type: "text", title: bold("Format Rule 2"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, width: 10, newLine: true)
                        input(name: "fr3", type: "text", title: bold("Format Rule 3"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, width: 10, newLine: true, newLineAfter: true)
                    } else input(name: 'btnShowFormatRules', type: 'button', title: 'Show Format Rules ▶', backgroundColor: 'dodgerBlue', textColor: 'white', submitOnChange: true, width: 3, newLine: true, newLineAfter: true)

                    //Replace Chars
                    if (state.show.ReplaceCharacters == true) {
                        input(name: 'btnShowReplaceCharacters', type: 'button', title: 'Show Replace Chars ▼', backgroundColor: 'navy', textColor: 'white', submitOnChange: true, width: 3, newLine: true, newLineAfter: true)
                        input(name: "oc1", type: "text", title: bold("Original Character(s)"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, width: 2, newLine: false)
                        input(name: "nc1", type: "text", title: bold("New Character(s)"), required: false, displayDuringSetup: true, defaultValue: "?", submitOnChange: true, width: 2, newLine: false)
                    } else input(name: 'btnShowReplaceCharacters', type: 'button', title: 'Show Replace Chars ▶', backgroundColor: 'dodgerBlue', textColor: 'white', submitOnChange: true, width: 3, newLine: true)

                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("Highlighting Help", parent.highlightNotes())
                    }
                }

                //Styles
                if (activeButton == 8) {
                    if (isLogAppPerformance) log.info("Start of Styles Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("Styles")
                    input(name: "applyStyleName", type: "enum", title: bold("Select Style to Apply"), options: parent.listStyles(), required: false, submitOnChange: true, defaultValue: null, width: 3)
                    input(name: "saveStyleName", type: "text", title: bold("Save as Style: (Tab or Enter)"), backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, defaultValue: "?", width: 3)
                    input(name: "deleteStyleName", type: "enum", title: bold("Select Style to Delete"), options: parent.listStyles(), required: false, submitOnChange: true, defaultValue: null, width: 3)
                    input(name: "isShowImportExport", type: "bool", title: "<b>Show Import\\Export?</b>", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 3, newLineAfter: true)

                    if (applyStyleName != null)
                        input(name: "applyStyle", type: "button", title: "Apply Style", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 3, newLine: true, newLineAfter: false)
                    else
                        input(name: "doNothing", type: "button", title: "Apply Style", backgroundColor: "#D3D3D3", textColor: "black", submitOnChange: true, width: 3, newLine: true, newLineAfter: false)

                    if (saveStyleName != null && saveStyleName != "?") input(name: "saveStyle", type: "button", title: "Save Current Style", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 3, newLine: false, newLineAfter: false)
                    if (saveStyleName == null || saveStyleName == "?") input(name: "doNothing", type: "button", title: "Save Current Style", backgroundColor: "#D3D3D3", textColor: "black", submitOnChange: true, width: 3, newLine: false, newLineAfter: false)

                    if (deleteStyleName != null)
                        input(name: "deleteStyle", type: "button", title: "Delete Selected Style", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 3, newLine: false, newLineAfter: true)
                    else
                        input(name: "doNothing", type: "button", title: "Delete Selected Style", backgroundColor: "#D3D3D3", textColor: "black", submitOnChange: true, width: 3, newLine: false, newLineAfter: true)

                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("Styles Notes", parent.styleNotes())
                    }

                    if (isShowImportExport == true) {
                        paragraph "<b>Export</b><br>These are your currently active settings. You can copy these and share them with others via the Hubitat Community forum. Tweaking can be addictive but a lot of fun to explore!"
                        paragraph "<style><div {width: 150px; border: 5px solid #000000;} div.a {word-wrap: break-word;}</style><body><div class='a'><b>Basic Settings:</b><br><mark>" + state.myBaseSettingsMap.sort() + "</mark></div></body>"
                        paragraph "<style><div {width: 150px; border: 5px solid #000000;} div.a {word-wrap: break-word;}</style></head><body><div class='a'><b>Overrides:</b><br><mark>" + overrides.toString() + "</mark></div></body>"
                        paragraph line(1)
                        paragraph "<b>Import</b><br>You can paste settings from other people in here and save them as a new style. How great is that!"
                        input(name: "importStyleText", type: "text", title: bold("Paste Basic Settings Here!"), required: false, defaultValue: "?", width: 12, height: 4, submitOnChange: true)
                        input(name: "importStyleOverridesText", type: "text", title: bold("Paste Overrides Here!"), required: false, defaultValue: "?", width: 12, submitOnChange: true)

                        if (importStyleText == null || importStyleText.size() == 0) {
                            input(name: "doNothing", type: "button", title: "Import Style?", backgroundColor: "#D3D3D3", textColor: "black", submitOnChange: true, width: 3, newLine: false, newLineAfter: false)
                            input(name: "doNothing", type: "button", title: "Clear Import", backgroundColor: "#D3D3D3", textColor: "black", submitOnChange: true, width: 3, newLine: false, newLineAfter: false)
                        } else {
                            input(name: "importStyle", type: "button", title: "Import Style", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 3, newLine: true, newLineAfter: false)
                            input(name: "clearImport", type: "button", title: "Clear Import", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 3, newLine: true, newLineAfter: false)
                        }
                        paragraph "<b>Once you have imported a new Style you can save it if you wish to preserve it.</b>"
                    }
                }

                //Advanced Settings
                if (activeButton == 9) {
                    if (isLogAppPerformance) log.info("Start of Advanced Section: " + (now() - state.refresh) / 1000 + " seconds")
                    if (isCompactDisplay == false) paragraph titleise("Advanced Settings")
                    input(name: "scrubHTMLlevel", type: "enum", title: bold("HTML Scrub Level"), options: htmlScrubLevel(), required: false, submitOnChange: true, defaultValue: 1, width: 2, newLineAfter: false)
                    input(name: "isOverrides", type: "bool", title: "<b>Enable Overrides?</b>", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 2, newLineAfter: false)
                    input(name: "isShowSettings", type: "bool", title: "<b>Show Effective Settings?</b>", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 2)
                    input(name: "isShowHTML", type: "bool", title: "<b>Show Pseudo HTML?</b>", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 2)

                    if (isOverrides == true) {
                        paragraph line(1)
                        input(name: "overrideHelperCategory", type: "enum", title: bold("Override Category"), options: overrideCategory().sort(), required: true, width: 2, submitOnChange: true, newLineAfter: true)
                        input(name: "overridesHelperSelection", type: "enum", title: bold("$overrideHelperCategory Examples"), options: getOverrideCommands(overrideHelperCategory.toString()), required: false, width: 12, submitOnChange: true, newLineAfter: true)
                        if (state.currentHelperCommand != null) paragraph "<mark>" + state.currentHelperCommand + "</mark></body>"
                        input(name: "clearOverrides", type: "button", title: "Clear the Overrides", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 2, newLine: true, newLineAfter: false)
                        input(name: "copyOverrides", type: "button", title: "Copy To Overrides", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 2, newLine: true, newLineAfter: false)
                        input(name: "appendOverrides", type: "button", title: "Append To Overrides", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 2, newLine: true, newLineAfter: false)
                        input(name: "Refresh", type: "button", title: "Refresh Table", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 2)
                        input(name: "overrides", type: "textarea", title: titleise("Settings Overrides"), required: false, defaultValue: "?", width: 12, rows: 5, submitOnChange: true)
                        if (isCompactDisplay == false) paragraph summary("About Overrides", parent.overrideNotes())
                    }

                    if (isShowSettings == true) {
                        paragraph line(1)
                        paragraph "<b>Effective Settings</b>"
                        paragraph "<head><style><div {width: 150px; border: 5px solid #000000;} div.a {word-wrap: break-word;}</style></head><body><div class='a'><mark>" + state.myEffectiveSettingsMap.sort() + "</mark></div></body>"
                    }

                    if (isShowHTML == true) {
                        paragraph line(1)
                        paragraph "<b>Pseudo HTML</b>"
                        myHTML = state.iFrameHTML
                        paragraph "<head><style><div {width: 150px; border: 5px solid #000000;} div.a {word-wrap: break-word;}</style></head><body><div class='a'><mark>" + unHTML(state.HTML) + "</mark></div></body>"
                    }
                    if (isCompactDisplay == false) {
                        paragraph line(1)
                        paragraph summary("Advanced Notes", parent.advancedNotes())
                    }
                }

                if (isCompactDisplay == false) paragraph line(2)
            }

            //Display Table
            if (isLogAppPerformance) log.info("Start of Display Table Section: " + (now() - state.refresh) / 1000 + " seconds")
            if (isCompactDisplay == false) paragraph summary("Display Tips", parent.displayTips())

            myHTML = toHTML(state.iframeHTML)
            myHTML = myHTML.replace("#iFrame1#", "body{background:${iFrameColor};font-size:${bfs}px;}")
            state.iFrameFinalHTML = myHTML

            def previewWidth = (tilePreviewWidth?.toString()?.isInteger() ? tilePreviewWidth.toInteger() : 2)
            def previewHeight = (tilePreviewHeight?.toString()?.isInteger() ? tilePreviewHeight.toInteger() : 2)
            def width = (previewWidth * 200)
            def height = (previewHeight * 190)
            myHTML = """<iframe srcdoc="${myHTML}" width="${width}" height="${height}" style="left:calc((80% - ${width}px) / 5);position:relative" scrolling="no"></iframe>"""
            paragraph myHTML

            if (state.HTMLsizes.Final < 4096) {
                if (isCompactDisplay == false) paragraph "<div style='color:#17202A;text-align:left; margin-top:0em; margin-bottom:0em ; font-size:18px'>Current HTML size is: <font color = 'green'><b>${state.HTMLsizes.Final}</b></font color = '#17202A'> bytes. Maximum size for dashboard tiles is <b>4,096</b> bytes.</div>"
            } else {
                if (isCompactDisplay == false) paragraph "<div style='color:#17202A;text-align:left; margin-top:0em; margin-bottom:0em ; font-size:18px'>Current HTML size is: <font color = 'red'><b>${state.HTMLsizes.Final}</b></font color = '#17202A'> bytes. Maximum size for dashboard tiles is <b>4,096</b> bytes.</div>"
            }

            if (isCustomize == true) {
                overridesSize = 0
                if (settings.overrides?.size() != null && isOverrides == true) overridesSize = settings.overrides?.size()
                line = "<b>Enabled Features:</b> Comment:${isComment}, Frame:${isFrame}, Title:${isTitle}, Headers:${isHeaders}, Border:${isBorder}, Alternate Rows:${isAlternateRows}, Footer:${isFooter}, Overrides:${isOverrides} ($overridesSize bytes)<br>"
                line += "<b>Space Usage:</b> Comment: <b>${state.HTMLsizes.Comment}</b>  Head: <b>${state.HTMLsizes.Head}</b>  Body: <b>${state.HTMLsizes.Body}</b>  Interim Size: <b>${state.HTMLsizes.Interim}</b>  Final Size: <b>${state.HTMLsizes.Final}</b> (Scrubbing level is: ${parent.htmlScrubLevel()[scrubHTMLlevel?.toString()?.isInteger() ? scrubHTMLlevel.toInteger() : 1]})<br>"
                line = line.replace("true", "<b><font color = 'green'> On</font color = 'black'></b>")
                line = line.replace("false", "<b><font color = 'grey'> Off</font color = 'grey'></b>")
                if (isCompactDisplay == false) {
                    paragraph note("", line)
                    if (state.HTMLsizes.Final < 1024) paragraph note("Note: ", "Current tile is less than 1,024 bytes and will be stored within an attribute.")
                    else paragraph note("Note: ", "Current tile is greater than 1,024 bytes and will be stored as a file in File Manager and linked with an attribute.")
                }
            }
        }

        //Start of Publish Section
        section(hideable: true, hidden: state.hidden.Publish, title: buttonLink('btnHidePublish', getSectionTitle("Publish"), 20)) {
            if (isLogAppPerformance) log.info("Start of Publishing Section: " + (now() - state.refresh) / 1000 + " seconds")
            myText = "Here you will configure where the table will be stored. It will be refreshed based upon your publishing preferences."
            paragraph myText
            input(name: "myTile", title: "<b>Tile Attribute to store the table?</b>", type: "enum", options: parent.allTileList(), required: true, submitOnChange: true, width: 2, defaultValue: 0, newLine: false)
            input(name: "myTileName", type: "text", title: "<b>Name this Tile</b>", submitOnChange: true, width: 2, newLine: false, required: true)
            input(name: "tilesAlreadyInUse", type: "enum", title: bold("For Reference Only: Tiles in Use"), options: parent.getTileList(), required: false, defaultValue: "Tile List", submitOnChange: true, width: 2)
            input(name: "eventTimeout", type: "enum", title: "<b>Event Timeout (millis)</b>", required: false, multiple: false, defaultValue: "2000", options: ["0", "250", "500", "1000", "2000", "5000", "10000"], submitOnChange: true, width: 2)
            input(name: "republishDelay", type: "enum", title: "<b>Republish Delay (minutes)</b>", required: false, multiple: false, defaultValue: 0, options: [0, 1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60], submitOnChange: true, width: 2)
            input (name: "oversizeTileHandling", type: "enum", title: "<b>Oversize Tile Handling</b>", required: false, multiple: false, defaultValue: "File Manager", options: ["File Manager","Cloud Endpoint"], submitOnChange: true, width: 2, newLineAfter:true)
            
            if (myTileName) app.updateLabel(myTileName)
            myText =  "The <b>Tile Name</b> given here will also be used as the name for this instance of Tile Builder. Appending the name with your chosen tile number can make parent display more readable.<br>"
            myText += "The <b>Event Timeout</b> period is how long Tile Builder will wait for subsequent events before publishing the table. Devices that do bulk updates create a lot of events in a short period of time. This setting batches requests within this period into a single publishing event. "
            myText += "The default timeout period is 2000 milliseconds (2 seconds). If you want a more responsive table you can lower this number, but it will slightly increase the CPU utilization.<br>"
            myText += "The <b>Republish Delay</b> sets a minimum amount of time before a Tile is re-published. This can be used to prevent <b>chatty sensors</b> from causing a Tile to republish too frequently. The default value for this setting is 0 (no delay)." +\
                       "<b>Republish Delay</b> is not available in Activity Monitor because it already works on a timed basis.<br>"
            myText += "<b>When immediate updates are important such as switches, locks, motion or contact sensors then set both the the Event Timeout and Republish Delay to 0.</b><br><br>"
            myText += "<b>Oversize Tiles:</b> Tiles over 1,024 bytes can either be stored in Hubitat File Manager or published to a Hubitat Cloud Endpoint<br>"
            myText += "Tiles stored to the Hubitat File Manager are only accessible from the local LAN or when using a VPN.<br>"
            myText += "Tiles stored to a Hubitat Cloud Endpoint are accessible from the local LAN or the cloud via the Hubitat App but does require the internet to be operational in order to display.<br>"
            myText += "Storing oversize tiles in File Manager is the faster of the two options and is the default operation. Only use cloud endpoints when you have an explicit need for the information to be availble via the internet.<br>"
            myText += "<b>Important: If you wish to use cloud endpoints you must go to the Apps / Code for this module and enable OAuth using the default Auto-Generated values.</b><br>"
            if (state.cloudEndpoint != null ) myText += "The Hubitat Cloud Endpoint for this Tile Builder table is: ${state.cloudEndpoint}"
            paragraph summary("Publishing Notes", myText)
            paragraph line(1)
            
            if (state.HTMLsizes.Final < 4096 && settings.myTile != null && myTileName != null) {
                input(name: "publishSubscribe", type: "button", title: "Publish and Subscribe", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 12)
                input(name: "unsubscribe", type: "button", title: "Delete Subscription", backgroundColor: "#27ae61", textColor: "white", submitOnChange: true, width: 12)
            } else input(name: "cannotPublish", type: "button", title: "Publish", backgroundColor: "#D3D3D3", textColor: "black", submitOnChange: true, width: 12)
        }

        //Start of More Section
        section {
            if (isCompactDisplay == false) paragraph line(2)
            input(name: "isMore", type: "bool", title: "More Options", required: false, multiple: false, defaultValue: false, submitOnChange: true, width: 2)
            if (isMore == true) {
                paragraph "<div style='background:#FFFFFF; height: 1px; margin-top:0em; margin-bottom:0em ; border: 0;'></div>"
                input(name: "isLogTrace", type: "bool", title: "<b>Enable Trace logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogVariables", type: "bool", title: "<b>Enable Variables logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogCleanups", type: "bool", title: "<b>Enable Cleanups logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogHighlights", type: "bool", title: "<b>Enable Highlights logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogStyles", type: "bool", title: "<b>Enable Style logging?</b>", defaultValue: false, submitOnChange: true, width: 3, newLine: true)
                input(name: "isLogHTML", type: "bool", title: "<b>Enable HTML Assembly logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogPublish", type: "bool", title: "<b>Enable Publishing logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogDateTime", type: "bool", title: "<b>Enable Date\\Time logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogDeviceInfo", type: "bool", title: "<b>Enable Device Details logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogAppPerformance", type: "bool", title: "<b>Enable App Performance Logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
                input(name: "isLogDetails", type: "bool", title: "<b>Add more detail to each type of logging?</b>", defaultValue: false, submitOnChange: true, width: 3)
            }
            myDocURL = "<a href='https://github.com/GaryMilne/Hubitat-TileBuilder/blob/main/Tile%20Builder%20Grid%20Help.pdf' target=_blank> <i><b>Tile Builder Grid Help</b></i></a>"
            myText = '<div style="display: flex; justify-content: space-between;">'
            myText += '<div style="text-align:left;font-weight:small;font-size:12px"> <b>Documentation:</b> ' + myDocURL + '</div>'
            myText += '<div style="text-align:center;font-weight:small;font-size:12px">Version: ' + codeDescription() + '</div>'
            myText += '<div style="text-align:right;font-weight:small;font-size:12px">Copyright 2022 - 2024</div>'
            myText += '</div>'
            paragraph myText
        }
        refreshUIafter()
    }
}

def test() {}

def truncateName(deviceName) {
    if (deviceName == null) return ""
    def truncLen = (myTruncateLength?.toString()?.isInteger() ? myTruncateLength.toInteger() : 99)
    if (isLogTrace && isLogDetails) log.info("<b>truncateName: Entering with: deviceName $deviceName and truncate length is ${truncLen} </b>")
    shortName = deviceName.toString() + "                                                                                                 "
    if (truncLen == 93) shortName = findSpace(shortName, 6)
    if (truncLen == 94) shortName = findSpace(shortName, 5)
    if (truncLen == 95) shortName = findSpace(shortName, 4)
    if (truncLen == 96) shortName = findSpace(shortName, 3)
    if (truncLen == 97) shortName = findSpace(shortName, 2)
    if (truncLen == 98) shortName = findSpace(shortName, 1)
    if (truncLen <= 30) {
        if (deviceName.toString().size() > truncLen) {
            shortName = shortName.substring(0, truncLen)
        }
    }
    return shortName.trim()
}

def findSpace(String myDevice, int myOccurrence) {
    if (myDevice == null) return ""
    i = 1
    n = myDevice.size()
    def spaceList = []

    if (n > 30) n = 30
    while (i < (n - 1)) {
        thisChar = myDevice.substring(i, i + 1)
        if (thisChar == " ") {
            spaceList.add(i)
        }
        i++
    }
    if (spaceList.size() < myOccurrence) return myDevice.substring(0, Math.min(n, 30))
    cutOff = spaceList.get(myOccurrence - 1)
    myShortDevice = myDevice.substring(0, cutOff)
    if (isLogDebug) log.debug("myShortDevice ${myShortDevice}")
    return myShortDevice
}

def checkNulls() {
    if (myThresholdCount == null) app.updateSetting("myThresholdCount", [value: "0", type: "enum"])
    if (myKeywordCount == null) app.updateSetting("myKeywordCount", [value: "0", type: "enum"])
    if (myVariableCount == null) app.updateSetting("myVariableCount", [value: "0", type: "enum"])
    if (rows == null) app.updateSetting("rows", [value: "1", type: "enum"])
    if (myColumns == null) app.updateSetting("myColumns", [value: "1", type: "enum"])
    if (varColumns == null) app.updateSetting("varColumns", [value: "1", type: "enum"])
    if (tilePreviewWidth == null) app.updateSetting("tilePreviewWidth", [value: "2", type: "enum"])
    if (tilePreviewHeight == null) app.updateSetting("tilePreviewHeight", [value: "2", type: "enum"])
    if (eventTimeout == null) app.updateSetting("eventTimeout", "2000")
    if (scrubHTMLlevel == null) app.updateSetting("scrubHTMLlevel", [value: "1", type: "enum"] )
    if (tbo == null) app.updateSetting("tbo", [value: "1", type: "enum"] )
    if (to == null) app.updateSetting("to", [value: "1", type: "enum"] )
    if (hto == null) app.updateSetting("hto", [value: "1", type: "enum"] )
    if (bo == null) app.updateSetting("bo", [value: "1", type: "enum"] )
    if (rto == null) app.updateSetting("rto", [value: "1", type: "enum"] )
    if (rbo == null) app.updateSetting("rbo", [value: "1", type: "enum"] )
    if (bfs == null) app.updateSetting("bfs", [value: "18", type: "enum"] )
    if (tff == null) app.updateSetting("tff", [value: "Roboto", type: "enum"] )
}

def getSectionTitle(section) {
    if (section == "Layout Mode") {
        if (state.hidden.LayoutMode == true) return sectionTitle("Layout Mode ▶") else return sectionTitle("Layout Mode ▼")
    }
    if (section == "Device Group") {
        if (state.hidden.DeviceGroup == true) return sectionTitle("Device Group ▶") else return sectionTitle("Device Group ▼")
    }
    if (section == "Variables") {
        if (state.hidden.Variables == true) return sectionTitle("Configure Variables ▶") else return sectionTitle("Configure Variables ▼")
    }
    if (section == "Grid Layout") {
        if (state.hidden.GridLayout == true) return sectionTitle("Grid Layout ▶") else return sectionTitle("Grid Layout ▼")
    }
    if (section == "Design") {
        if (state.hidden.Design == true) return sectionTitle("Design Table ▶") else return sectionTitle("Design Table ▼")
    }
    if (section == "Publish") {
        if (state.hidden.Publish == true) return sectionTitle("Publish Table ▶") else return sectionTitle("Publish Table ▼")
    }
}

def getSelectedAttributes() {
    def attributeList = []
    int count = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0)
    for (i = 1; i <= count; i++) {
        myAttr = settings["myAttribute$i"]
        if (myAttr != null) attributeList.add(myAttr)
    }
    return attributeList
}

def getVariablesFreeForm() {
    if (isLogTrace || isLogVariables) log.trace("<b>getVariablesFreeForm: Entering.</b>")
    def varString1 = ""
    def varString2 = ""
    def varName
    state.vars = [:]

    int count = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0)
    for (int i = 1; i <= count; i++) {
        try {
            resultMap = cleanupFreeForm(i)

            if (isLogVariables) log.info("getVariablesFreeForm: received " + resultMap.toString() + "from cleanupFreeForm - varNumber is: $i")
            def myValue
            varName = settings["name$i"] ?: "Var${i}"

            resultMap?.each { itemName, value ->
                if (value == null || value == "Null" || value == "null") value = invalidAttribute.toString()
                if (isLogVariables) log.info("getVariablesFreeForm: Item Name: $itemName, Value: $value,  varName is: $varName")

                myValue = highlightValue(value, i)
                state.vars."$varName" = (myValue != null ? myValue.toString() : invalidAttribute.toString())
                if (isLogVariables && isLogDetails) log.info("getVariablesFreeForm: state.vars.$varName is: " + (myValue != null ? myValue.toString() : ""))
            }
        }
        catch (Exception e) { log.error("getVariablesFreeForm: Error getting Device\\Variable info for index ${i}: ${e.message}") }
    }

    def sortedMap = state.vars.entrySet().sort { it.key.capitalize() }.collectEntries { entry -> [entry.key, entry.value] }
    sortedMap.each { thisVar, value ->
        if (value != null && value != "Null" && value != "null") {
            varString1 += dodgerBlue(bold("$thisVar: ")) + toHTML(value) + ",  "
            varString2 += dodgerBlue(bold("$thisVar: ")) + toHTML(value) + " (<mark>" + unHTML(value) + "</mark>),  "
        }
    }

    if (isLogVariables && isLogDetails) log.info("getVariablesFreeForm: varString1 is: $varString1")
    if (isLogVariables && isLogDetails) log.info("getVariablesFreeForm: varString2 is: $varString2")
    if (showVariables == "Show Variables") return varString1
    if (showVariables == "Show Variables & HTML") {
        varString2 = varString2.replaceAll("(?i)<style>", "<!style!>")
        return varString2
    }
}

def cleanupFreeForm(i) {
    if (isLogTrace || isLogCleanups) log.trace("<b>cleanupFreeForm: Entering with $i.</b>")
    def myMap = [:]
    def myValue = null
    def myName = settings["name$i"] ?: "Var${i}"
    def dataType = "null"

    if (isLogCleanups) log.info("<u>cleanupFreeForm: Performing cleanup for: $myName</u>")

    if (settings["variableSource$i"] == "Default Device" && settings["defaultDevice"] != null) {
        myValue = settings["defaultDevice"]?.currentValue(settings["myAttribute$i"])
        if (myValue == null) myValue = invalidAttribute.toString()
        dataType = getDataType(myValue)
        if (isLogCleanups && isLogDetails) log.info("cleanupFreeForm: Processing Default Device: $myName with index:$i and the myValue is:$myValue with dataType: $dataType")
    }
    if (settings["variableSource$i"] == "Device Attribute" && settings["myDevice$i"] != null && settings["myAttribute$i"] != null) {
        myValue = settings["myDevice$i"]?.currentValue(settings["myAttribute$i"])
        if (myValue == null) myValue = invalidAttribute.toString()
        dataType = getDataType(myValue)
        if (isLogCleanups && isLogDetails) log.info("cleanupFreeForm: Processing Device: $myName with index:$i and the value is:$myValue with dataType: $dataType")
    }

    if (settings["variableSource$i"] == "Hub Variable" && settings["myHubVariable$i"] != null) {
        myMap = getGlobalVar(settings["myHubVariable$i"])
        myValue = myMap?.value
        dataType = getDataType(myValue)
        if (isLogCleanups && isLogDetails) log.info("cleanupFreeForm: Processing Hub Variable: $myName with index:$i and the value is:$myValue with dataType: $dataType")
    }

    if (settings["variableSource$i"] == "Device Detail" && settings["myDevice$i"] != null && settings["myAttribute$i"] != null) {
        if (settings["myAttribute$i"].contains("Duration")) {
            myMap = getDurationFreeForm(settings["myAttribute$i"], settings["myDevice$i"])
            state.vars."$myName" = myMap?.value
        } else myMap = getDeviceDetail(settings["myAttribute$i"], settings["myDevice$i"], i)
        myValue = myMap?.value
        dataType = myMap?.dataType ?: "String"
        if (isLogCleanups && isLogDetails) log.info("cleanupFreeForm: Processing Device Detail: $myName with index:$i and the value is:$myValue with dataType: $dataType")
    }

    if (settings["variableSource$i"] == "Hub Property" && settings["myAttribute$i"] != null) {
        myMap = getHubProperty(settings["myAttribute$i"], i)
        myValue = myMap?.value
        dataType = myMap?.dataType ?: "String"
        if (isLogCleanups && isLogDetails) log.info("cleanupFreeForm: Processing Hub Property: $myName with index:$i and the value is:$myValue with dataType: $dataType")
    }

    if (myValue == null) myValue = invalidAttribute.toString()
    myMap = cleanupCore(dataType, myName, myValue.toString(), i)
    if (isLogCleanups) log.info("cleanUpFreeForm: Leaving with myMap is: $myMap")

    return myMap
}

def getVariablesDeviceGroup() {
    if (isLogTrace || isLogVariables) log.trace("<b>getVariablesDeviceGroup: Entering.</b>")
    state.vars = [:]
    int i = 1
    def attributeList = getSelectedAttributes()
    def deviceCount = myDeviceList?.size() ?: 0
    if (isLogVariables) log.info("Device Count is: $deviceCount")
    app.updateSetting("rows", [value: "$deviceCount", type: "enum"])
    if (deviceCount > 0) {
        myDeviceList.sort { it?.getLabel() ?: it?.getName() ?: "" }.each { it ->
            try {
                if (it == null) return
                gatherDeviceDetails?.each { value ->
                    resultMap = getDeviceDetail(value, it, i)
                    state.vars."$value$i" = resultMap?.value
                }
                
                if (state.vars."lastOn$i" != null && state.vars."lastOff$i" != null) {
                    getDurationDeviceGroup("lastOnDuration", i)
                }
                if (state.vars."lastOpen$i" != null && state.vars."lastClosed$i" != null) {
                    getDurationDeviceGroup("lastOpenDuration", i)
                }
                if (state.vars."lastLocked$i" != null && state.vars."lastUnlocked$i" != null) {
                    getDurationDeviceGroup("lastLockedDuration", i)
                }
                if (state.vars."lastActive$i" != null && state.vars."lastInactive$i" != null) {
                    getDurationDeviceGroup("lastActiveDuration", i)
                }
                if (state.vars."lastPresent$i" != null && state.vars."lastNotPresent$i" != null) {
                    getDurationDeviceGroup("lastPresentDuration", i)
                }

                attributeList.each { attributeName ->
                    myValue = it.currentValue(attributeName)?.toString() ?: invalidAttribute.toString()
                    varName = "$attributeName" + i
                    state.vars."$varName" = (myValue != null ? myValue.toString() : "")
                    if (isLogVariables && isLogDetails) log.info("getVariablesDeviceGroup: Attribute: $attributeName ($myValue) saved to state.vars.$varName")
                }
                i++
            }
            catch (Exception e) { log.error("getVariablesDeviceGroup: Error getting Device info for device '${it}': ${e.message}") }
        }
    }

    int varCount = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0)
    for (i = 1; i <= varCount; i++) {
        myAttr = settings["myAttribute$i"]
        if (!myAttr) continue
        
        def resultList = state.vars.findAll { it.key ==~ /^\Q${myAttr}\E\d+$/ }

        resultList.each { name, value ->
            def matcher = name =~ /^(.+?)(\d+)$/
            if (matcher.matches()) {
                def attribute = matcher[0][1]
                def index = matcher[0][2]
                myName = state.vars."deviceLabel$index" ?: state.vars."deviceName$index" ?: "Device$index"
                newMap = cleanupDeviceGroup(myName, attribute, index, value)
                newValue = newMap ? newMap["$myName"] : value
                myValue = highlightValue(newValue, i)

                state.vars."$attribute$index" = myValue
            } else log.error("getVariablesDeviceGroup: Invalid name format: $name")
        }
    }
}

def cleanupDeviceGroup(myName, myAttribute, myIndex, myValue) {
    if (isLogTrace == true || isLogCleanups) log.info("<b>cleanupDeviceGroup: Entering with $myName,  $myAttribute,  $myValue,  $myIndex.</b>")
    def newMap
    def dataType
    def i

    selectedAttributes = getSelectedAttributes()
    if (isLogCleanups && isLogDetails) log.info("selectedAttributes is: $selectedAttributes")
    i = selectedAttributes.indexOf(myAttribute).toInteger() + 1

    if (isLogCleanups && isLogDetails) log.info("cleanupDeviceGroup: myAttribute is: $myAttribute, myIndex is: $myIndex, myValue is: $myValue, myName is: $myName and ActionIndex is $i")
    dataType = getDataType(myValue)
    newMap = cleanupCore(dataType, myName, myValue, i)

    if (isLogCleanups) log.info("cleanup: newMap is: $newMap")
    return newMap
}

def cleanupCore(dataType, myName, myValue, i) {
    if (isLogTrace && isLogDetails || isLogCleanups) log.info("<b>cleanupCore: Entering with $dataType, $myName, $myValue, $i.</b>")
    def newMap = [:]
    def myAction = settings["actionA$i"]
    
    String normType = dataType != null ? dataType.toLowerCase() : "null"

    if (myValue == null || normType == "null") {
        if (isLogCleanups && isLogDetails) log.info("cleanup: The attribute for device $myName is a null value")
        newMap[myName] = invalidAttribute.toString()
        return newMap
    }

    if (normType == "instant") {
        resultMap = isValidInstant(myValue.toString())
        if (resultMap.valid == true) {
            if (isLogCleanups && isLogDetails || isLogDateTime) log.info("cleanupCore: myValue $myValue is a valid instant.")
            myDateTimeIndex = getDateTimeFormatIndex(myAction)
            if (myDateTimeIndex == null || myDateTimeIndex == 0) {
                newMap[myName] = resultMap.date
                return newMap
            }
            newMap[myName] = formatTime(resultMap.date, myDateTimeIndex)
            return newMap
        }
    }

    if (normType == "date") {
        if (isLogCleanups && isLogDetails || isLogDateTime) log.info("cleanupCore: Convert time: $myValue")
        myDateTimeIndex = getDateTimeFormatIndex(myAction)
        if (myDateTimeIndex == null || myDateTimeIndex == 0) {
            newMap[myName] = myValue
            return newMap
        }
        newMap[myName] = formatTime(myValue, myDateTimeIndex)
        return newMap
    }

    if (normType == "boolean") {
        newMap[myName] = myValue.toString()
        switch (myAction) {
            case "Capitalize":
                newMap[myName] = myValue.toString().capitalize()
                break
            default:
                newMap[myName] = myValue.toString()
        }
        return newMap
    }

    if (normType == "string") {
        switch (myAction) {
            case "Capitalize":
                newMap[myName] = myValue.toString().capitalize()
                break
            case "Capitalize All":
                newMap[myName] = myValue.toString().split(' ').collect { it.capitalize() }.join(' ')
                break
            case "Upper Case":
                newMap[myName] = myValue.toString().toUpperCase()
                break
            case "OW Code to Emoji":
                newMap[myName] = getOpenWeatherEmoji(myValue.toString())
                break
            case "OW Code to PNG":
                newMap[myName] = getOpenWeatherPNG(myValue.toString())
                break
            case "Image URL":
                newMap[myName] = getImageURL(myValue.toString())
                break
            case "Alt Image URL":
                newMap[myName] = getAltImageURL(myValue.toString())
                break
            case "Remove Tags [] <>":
                newMap[myName] = clearHTML(myValue.toString())
                break
            default:
                newMap[myName] = myValue
        }
        return newMap
    }
    
    if (normType == "float") {
        def myFloat = myValue.toString().toFloat()
        switch (myAction) {
            case "0 Decimal Places":
                newMap[myName] = myFloat.round(0).toInteger()
                break
            case "1 Decimal Place":
                newMap[myName] = myFloat.round(1)
                break
            case "Commas":
                if (myFloat >= 1000) {
                    def formatter = java.text.DecimalFormat.getInstance(Locale.US)
                    formatter.applyPattern("#,##0")
                    newMap[myName] = formatter.format(myFloat)
                } else newMap[myName] = myFloat
                break
            default:
                newMap[myName] = myFloat
        }
        return newMap
    }

    if (normType == "integer") {
        switch (myAction) {
            case "1 Decimal Place":
                newMap[myName] = myValue.toString().toFloat().round(1)
                break
            case "Commas":
                newMap[myName] = String.format("%,d", myValue.toString().toInteger())
                break
            default:
                newMap[myName] = myValue
        }
        return newMap
    }
    
    newMap[myName] = myValue.toString()
    if (isLogCleanups) log.info("cleanupCore: newMap is: $newMap")
    return newMap
}

def getDeviceDetailMap(searchTerm) {
    if (isLogTrace && isLogDetails) log.info("<b>getDeviceDetailMap() Received $searchTerm</b>")

    mapOfMapsA = [lastOn     : [attribute: "switch", value: "on", dataType: "Date", max: 50], lastOff: [attribute: "switch", value: "off", dataType: "Date", max: 50],
                  lastOpen   : [attribute: "contact", value: "open", dataType: "Date", max: 50], lastClosed: [attribute: "contact", value: "closed", dataType: "Date", max: 50],
                  lastLocked : [attribute: "lock", value: "locked", dataType: "Date", max: 50], lastUnlocked: [attribute: "lock", value: "unlocked", dataType: "Date", max: 50],
                  lastPresent: [attribute: "presence", value: "present", dataType: "Date", max: 50], lastNotPresent: [attribute: "presence", value: "not present", dataType: "Date", max: 50],
                  lastActive : [attribute: "motion", value: "active", dataType: "Date", max: 50], lastInactive: [attribute: "motion", value: "inactive", dataType: "Date", max: 50]
    ]

    mapOfMapsB = [lastEventName: [dataType: "String", max: 1, field: "name"], lastEventValue: [dataType: "String", max: 1, field: "value"], lastEventDescription: [dataType: "String", max: 1, field: "descriptionText"], lastEventType: [dataType: "String", max: 1, field: "type"]]

    myMap = mapOfMapsA[searchTerm]
    if (myMap != null) return myMap

    myMap = mapOfMapsB[searchTerm]
    if (myMap != null) return myMap
}

def getShortName(myDevice){
	if (myDevice == null) return ""
	if (layoutMode.toString() != "Device Group") return myDevice
	def shortName = myDevice.toString()
		
	if (myReplaceText1 == null || myReplaceText1 == "?" ) myReplaceText1 = ""
	if (myReplaceText2 == null || myReplaceText2 == "?" ) myReplaceText2 = ""
	if (myReplaceText3 == null || myReplaceText3 == "?" ) myReplaceText3 = ""
	if (myReplaceText4 == null || myReplaceText4 == "?" ) myReplaceText4 = ""
	if (myReplaceText5 == null || myReplaceText5 == "?" ) myReplaceText5 = ""
        
	if (mySearchText1 != null  && mySearchText1 != "?") shortName = shortName.replace(mySearchText1, myReplaceText1)
	if (mySearchText2 != null  && mySearchText2 != "?") shortName = shortName.replace(mySearchText2, myReplaceText2)
	if (mySearchText3 != null  && mySearchText3 != "?") shortName = shortName.replace(mySearchText3, myReplaceText3)
	if (mySearchText4 != null  && mySearchText4 != "?") shortName = shortName.replace(mySearchText4, myReplaceText4)
	if (mySearchText5 != null  && mySearchText5 != "?") shortName = shortName.replace(mySearchText5, myReplaceText5)
	return shortName
}

def getDeviceDetail(deviceDetailName, device, i) {
    if (isLogTrace && isLogDetails) log.info("<b>getDeviceDetail() Received $deviceDetailName</b>")
    if (isLogDeviceInfo) log.info("getDeviceDetail() Device is: $device")

    if (device == null) return [value: invalidAttribute.toString(), dataType: "String"]

    if (layoutMode.toString() == "Free Form") myDateTimeIndex = 1
    if (layoutMode.toString() == "Device Group") myDateTimeIndex = defaultDateTimeFormat?.toString()?.isInteger() ? defaultDateTimeFormat.toInteger() : 1

    result = getDeviceDetailMap(deviceDetailName)

    if (result != null && result.dataType == "Date") {
        def lastEvent = device.events(max: result.max)?.find { it.name == result.attribute && it.value == result.value }
        if (lastEvent == null) return [value: invalidAttribute.toString(), dataType: "String"]
        def lastEventInstant = formatTime(lastEvent?.getDate(), 0)
        def lastEventDate = formatTime(lastEvent?.getDate(), myDateTimeIndex)
        if (layoutMode.toString() == "Device Group") {
            def instant = "Instant"
            state.vars."$deviceDetailName$instant$i" = lastEventInstant ?: 0
            state.vars."$deviceDetailName$i" = lastEventDate ?: invalidAttribute.toString()
        }
        if (isLogDeviceInfo) log.info("<b>getDeviceDetail: $deviceDetailName is: $lastEventDate</b>")
        return [value: lastEventDate, dataType: result.dataType]
    }

    if (result != null && result.field != null) {
        def lastEventValue = device.events(max: 1)?."$result.field"[0]
        if (lastEventValue == null) return [value: invalidAttribute.toString(), dataType: "String"]
        if (layoutMode.toString() == "Device Group") state.vars."$deviceDetailName$i" = lastEventValue
        if (isLogDeviceInfo) log.info("getDeviceDetail: $deviceDetailName is: $lastEventValue")
        return [value: lastEventValue, dataType: "String"]
    }

    if (layoutMode.toString() == "Device Group") state.vars."$deviceDetailName$i" = invalidAttribute.toString()

    switch (deviceDetailName) {
        case "lastActivity":
            myTimeStamp = device.getLastActivity()

            lastActivity = formatTime(myTimeStamp, myDateTimeIndex)
            if (layoutMode.toString() == "Device Group") state.vars."lastActivity$i" = lastActivity
            if (isLogDeviceInfo) log.info("getDeviceDetail: myLastActivity is: $lastActivity")
            if (lastActivity == null) return [value: invalidAttribute.toString(), dataType: "String"]
            return [value: lastActivity, dataType: "Date"]
            break
        case "deviceID":
            def deviceID = device.getId()
            if (isLogDeviceInfo) log.info("getDeviceDetail: deviceID is: $deviceID")
            if (layoutMode.toString() == "Device Group") state.vars."deviceID$i" = deviceID
            return [value: deviceID, dataType: "Integer"]
            break
        case "deviceName":
            def deviceName = device.getName()
            if (layoutMode.toString() == "Device Group") state.vars."deviceName$i" = deviceName
            if (isLogDeviceInfo) log.info("getDeviceDetail: deviceName is: $deviceName")
            return [value: truncateName(deviceName), dataType: "String"]
            break
        case "deviceLabel":
            def deviceLabel = device.getLabel()
            if (deviceLabel == null || deviceLabel == "null" || deviceLabel.toString().size() == 0) deviceLabel = "No Device Label"
            if (layoutMode.toString() == "Device Group") state.vars."deviceLabel$i" = deviceLabel
            if (isLogDeviceInfo) log.info("getDeviceDetail: deviceLabel is: $deviceLabel")
            return [value: truncateName(deviceLabel), dataType: "String"]
            break
        case "roomName":
            def roomName = device.getRoomName()
            if (roomName == null) return [value: invalidAttribute.toString(), dataType: "String"]
            if (layoutMode.toString() == "Device Group") state.vars."roomName$i" = roomName
            if (isLogDeviceInfo) log.info("getDeviceDetail: Room is: $roomName")
            return [value: roomName, dataType: "String"]
            break
        case "default":
            log.error("getDeviceDetail: deviceDetailName was not found. Returning 'EmptyDeviceDetail' with dataType 'String'")
            return [value: "EmptyDeviceDetail", dataType: "String"]
            break
    }
    return [value: "EmptyDeviceDetail", dataType: "String"]
}

def getDurationDeviceGroup(deviceDetail, i) {
    if (isLogTrace && isLogDetails) log.info("<b>getDurationDeviceGroup: Received $deviceDetail , $i</b>")
    var1 = durationsMap()."$deviceDetail"[0]
    var2 = durationsMap()."$deviceDetail"[1]
    long instant1 = state.vars."${var1}Instant$i" != null ? state.vars."${var1}Instant$i".toLong() : 0L
    long instant2 = state.vars."${var2}Instant$i" != null ? state.vars."${var2}Instant$i".toLong() : 0L
    state.vars."${var1}Duration$i" = getDuration(instant1, instant2)
    state.vars."${var2}Duration$i" = getDuration(instant2, instant1)
}

def getDurationFreeForm(deviceDetail, device) {
    if (isLogVariables) log.info("<b>getDurationFreeForm: Received $deviceDetail , $device</b>")
    long instant1 = 0L
    long instant2 = 0L
    def runTime

    var1 = durationsMap()."$deviceDetail"[0]
    var2 = durationsMap()."$deviceDetail"[1]

    def res1 = getDeviceDetail("$var1", device, 0)?.value
    def res2 = getDeviceDetail("$var2", device, 0)?.value

    instant1 = res1 ? formatTime(res1, 0) : 0L
    instant2 = res2 ? formatTime(res2, 0) : 0L

    runTime = getDuration(instant1, instant2)
    return [value: runTime, dataType: "String"]
}

def getDuration(long instant1, long instant2) {
    if (isLogTrace && isLogDetails) log.info("<b>getDuration: Received $instant1 , $instant2</b>")
    def indicator = ""
    long diff = instant1 - instant2
    if (diff == 0) return invalidAttribute.toString()
    if (diff > 0) {
        diff = (now() - instant1) / 1000; indicator = "<rt+>"
    }
    if (diff <= 0) {
        diff = (instant2 - instant1) / 1000; indicator = "<rt->"
    }
    def timeString = indicator + convertSecondsToDHMS(diff, true).toString()
    return timeString
}

def getHubProperty(hubPropertyName, i) {
    if (isLogTrace) log.info("<b>getHubProperty() Received $hubPropertyName</b>")
    myDateTimeIndex = 0
    def outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    switch (hubPropertyName) {
        case "sunrise":
            String sunrise = outputFormat.format(getTodaysSunrise())
            return [value: sunrise, dataType: "Date"]
            break
        case "sunset":
            String sunset = outputFormat.format(getTodaysSunset())
            return [value: sunset, dataType: "Date"]
            break
        case "sunriseTomorrow":
            String sunriseTomorrow = outputFormat.format(getTomorrowsSunrise())
            return [value: sunriseTomorrow, dataType: "Date"]
            break
        case "sunsetTomorrow":
            String sunsetTomorrow = outputFormat.format(getTomorrowsSunset())
            return [value: sunsetTomorrow, dataType: "Date"]
            break
        case "hubName":
            return [value: location.hub.name, dataType: "String"]
            break
        case "currentMode":
            return [value: location.properties.currentMode, dataType: "String"]
            break
        case "hsmStatus":
            return [value: location.hsmStatus, dataType: "String"]
            break
        case "firmwareVersionString":
            return [value: location.hub.firmwareVersionString, dataType: "String"]
            break
        case "uptime":
            long myUptime = location.hub.uptime
            uptime = convertSecondsToDHMS(myUptime, false)
            return [value: uptime, dataType: "String"]
            break
        case "timeZone":
            def timeZone = location.timeZone
            def myTimeZoneAbbreviation = timeZone?.getDisplayName(false, TimeZone.SHORT) ?: "UTC"
            return [value: myTimeZoneAbbreviation, dataType: "String"]
            break
        case "daylightSavingsTime":
            def timeZone = location.timeZone
            boolean DST = timeZone ? timeZone.inDaylightTime(new Date()) : false
            return [value: DST, dataType: "Boolean"]
            break
        case "currentTime":
            def currentDate = new Date()
            def dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            def myDate = dateFormat.format(currentDate)
            log.info("myDate is: $myDate")
            return [value: myDate, dataType: "Date"]

            break
        case "default":
            log.error("getHubProperty: getHubProperty was not found. Returning 'EmptyHubProperty' with dataType 'String'")
            return [value: "EmptyHubProperty", dataType: "String"]
            break
    }
    return [value: "EmptyHubProperty", dataType: "String"]
}

static def getAttributeList(thisDevice) {
    if (thisDevice != null) {
        def uniqueAttributes = thisDevice?.supportedAttributes?.collect { it.name }?.unique()
        return uniqueAttributes?.sort { a, b -> a.compareToIgnoreCase(b) }
    }
    return []
}

def formatTime(timeValue, int format) {
    if (timeValue == null || timeValue == "null" || timeValue == "N/A") return invalidAttribute.toString()
    def myType = getObjectClassName(timeValue)
    if (isLogTrace && isLogDetails || isLogDateTime) log.info("<b>formatTime: Time received is: $timeValue with dataType: $myType and requesting format: $format</b>")
    def myLongTime = 0L
    def testDate

    try {
        testDate = toDateTime(timeValue)
        if (isLogDateTime) log.info("It is a Hubitat Date: $testDate")
        myType = "Hubitat Date"
    }
    catch (Exception e) {
        if (isLogDateTime) log.info("Not a Hubitat Date: $e")
    }

    switch (myType) {
        case "java.sql.Timestamp":
            myLongTime = timeValue.getTime()
            if (isLogDateTime) log.info("<b>Received timestamp: $timeValue  -  Converted to: $myLongTime")
            break
        case "Hubitat Date":
            myLongTime = testDate.time
            if (isLogDateTime) log.info("<b>Received Hubitat Date: $timeValue  -  Converted to: $myLongTime</b>")
            break
        case "java.lang.String":
            try {
                def dateFormatPattern = "yyyy-MM-dd HH:mm:ss.SSS"
                def dateFormat = new SimpleDateFormat(dateFormatPattern)
                def date = dateFormat.parse(timeValue.toString())
                myLongTime = date.time
                if (isLogDateTime) log.info("<b>Received String: $timeValue  -  Converted to: $myLongTime</b>")
            }
            catch (Exception ignored) {
                if (isLogDateTime) log.info("Error converting java.lang.String $timeValue to a date.")
                myLongTime = 0L
            }
            break
        case "com.hubitat.helper.ReadableUTCDate":
            String timestampString = timeValue.toString()
            try {
                if (isLogDateTime) log.info("Trying format A: com.hubitat.helper.ReadableUTCDate")
                def inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                def date = inputFormat.parse(timestampString)
                myLongTime = date.time
                if (isLogDateTime) log.info("Success converting com.hubitat.helper.ReadableUTCDate - Format A. $timeValue  -  Converted to: $myLongTime")
                break
            }
            catch (Exception ignored) {
                if (isLogDateTime) log.info("Failed converting com.hubitat.helper.ReadableUTCDate - Format A")
            }
            try {
                if (isLogDateTime) log.info("Trying format B: com.hubitat.helper.ReadableUTCDate")
                inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ")
                def date = inputFormat.parse(timestampString)
                myLongTime = date.time
                if (isLogDateTime) log.info("Success converting com.hubitat.helper.ReadableUTCDate - Format B. $timeValue  -  Converted to: $myLongTime")
                break
            }
            catch (Exception ignored) {
                if (isLogDateTime) log.info("Failed converting com.hubitat.helper.ReadableUTCDate - Format B")
            }

            break
        default:
            if (timeValue instanceof Number) {
                myLongTime = timeValue.toLong()
            } else {
                if (isLogDateTime) log.info("myType is unknown or unhandled")
                return timeValue
            }
    }
    if (isLogDateTime && isLogDetails) log.info("myLongTime is: $myLongTime")

    if (myLongTime == 0L) return invalidAttribute.toString()
    Date myDateTime = new Date(myLongTime)

    if (format == 0) return myLongTime
    def targetFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    if (2 <= format && format <= 20) targetFormat = new SimpleDateFormat(getDateTimeFormatDescription(format))

    if (format == 21 || format == 22) {
        def diff = ((now() - myLongTime) / 1000).toLong()
        if (format == 21) elapsedTime = convertSecondsToDHMS(diff, true)
        if (format == 22) elapsedTime = convertSecondsToDHMS(diff, false)
        if (isLogDateTime && isLogDetails) log.info("Elapsedtime is $elapsedTime")
        return elapsedTime
    }

    if (format == 23 || format == 24) {
        def diff = ((myLongTime - now()) / 1000).toLong()
        if (format == 23) remainingTime = convertSecondsToDHMS(diff, true)
        if (format == 24) remainingTime = convertSecondsToDHMS(diff, false)
        if (isLogDateTime && isLogDetails) log.info("Remaining time is $remainingTime")
        return remainingTime
    }

    try {
        Date date = new Date(myLongTime)
        String formattedDateTime = targetFormat.format(date)
        if (isLogDateTime && isLogDetails) log.info("formatTime: Returning date $formattedDateTime")
        return formattedDateTime
    }
    catch (Exception ignored) {
        return timeValue
    }
}

static def getDateTimeFormatIndex(description) {
    def map = dateFormatsMap()
    for (entry in map) {
        if (entry.value == description) return entry.key
    }
    return 0
}

static def getDateTimeFormatDescription(format) {
    def dateFormatString = dateFormatsMap()[format] ?: "yyyy-MM-dd HH:mm:ss.SSS"
    def dateFormat = dateFormatString.replaceAll('To: ', '')
    return dateFormat
}

String convertSecondsToDHMS(long seconds, boolean includeSeconds) {
    if (isLogDateTime) log.info("<convertSecondsToDHMS: Received: $seconds")
    def days = (seconds / (24 * 3600)) as int
    def hours = ((seconds % (24 * 3600)) / 3600) as int
    def minutes = ((seconds % 3600) / 60) as int
    def remainingSeconds = (seconds % 60) as int

    def daysString = days > 0 ? "${days}d " : ""
    def hoursString = hours > 0 ? "${hours}h " : ""

    if (!includeSeconds) return "${daysString}${hoursString} ${minutes}m"
    else return "${daysString}${hoursString}${minutes}m ${remainingSeconds}s"
}

void refreshUIbefore() {
    if (isLogTrace == true) log.trace("<b style='color:green;font-size:medium'>refreshUIbefore: Entering.</b>")
    state.currentHelperCommand = ""
    overridesHelperMap = parent.getOverridesListAll()
    state.currentHelperCommand = overridesHelperMap?.get(overridesHelperSelection) ?: ""
    if (state.flags.isClearOverridesHelperCommand == true) {
        app.updateSetting("overrides", [value: "", type: "textarea"])
        state.flags.isClearOverridesHelperCommand = false
    }

    if (layoutMode.toString() == "Device Group") {
        if (gatherDeviceDetails == null) app.updateSetting("gatherDeviceDetails", [value: ["deviceLabel", "deviceName"], type: "enum"])

        if (!gatherDeviceDetails?.contains("deviceLabel") || !gatherDeviceDetails?.contains("deviceName")) app.updateSetting("gatherDeviceDetails", [value: gatherDeviceDetails + ["deviceName"], type: "enum"])

        if (gatherDeviceDetails?.contains("lastOnDuration") || gatherDeviceDetails?.contains("lastOffDuration")) app.updateSetting("gatherDeviceDetails", [value: gatherDeviceDetails + ["lastOn", "lastOff"], type: "enum"])
        if (gatherDeviceDetails?.contains("lastOpenDuration") || gatherDeviceDetails?.contains("lastClosedDuration")) app.updateSetting("gatherDeviceDetails", [value: gatherDeviceDetails + ["lastOpen", "lastClosed"], type: "enum"])
        if (gatherDeviceDetails?.contains("lastLockedDuration") || gatherDeviceDetails?.contains("lastUnlockedDuration")) app.updateSetting("gatherDeviceDetails", [value: gatherDeviceDetails + ["lastLocked", "lastUnlocked"], type: "enum"])
        if (gatherDeviceDetails?.contains("lastActiveDuration") || gatherDeviceDetails?.contains("lastInactiveDuration")) app.updateSetting("gatherDeviceDetails", [value: gatherDeviceDetails + ["lastActive", "lastInactive"], type: "enum"])
        if (gatherDeviceDetails?.contains("lastPresentDuration") || gatherDeviceDetails?.contains("lastNotPresentDuration")) app.updateSetting("gatherDeviceDetails", [value: gatherDeviceDetails + ["lastPresent", "lastNotPresent"], type: "enum"])
    }
}

void refreshUIafter() {
    if (isLogTrace == true) log.trace("<b style='color:green;font-size:medium'>refreshUIafter: Entering.</b>")
    if (state.flags.styleSaved == true) {
        app.updateSetting("saveStyleName", "?")
        state.flags.styleSaved = false
    }

    if (state.flags.myCapabilityChanged == true) {
        app.updateSetting("myDeviceList", [type: "capability", value: []])
        state.flags.myCapabilityChanged = false
    }
    if (state.flags.isCopyOverridesHelperCommand == true) {
        myCommand = state.currentHelperCommand
        app.updateSetting("overrides", [value: myCommand, type: "textarea"])
        state.flags.isCopyOverridesHelperCommand = false
    }
    if (state.flags.isAppendOverridesHelperCommand == true) {
        myCurrentCommand = overrides.toString()
        combinedCommand = myCurrentCommand.toString() + " | \n" + state.currentHelperCommand.toString()
        app.updateSetting("overrides", [value: combinedCommand.toString(), type: "textarea"])
        state.flags.isAppendOverridesHelperCommand = false
    }
}

def resetVariables() {
    if (isLogTrace) log.trace("<b>resetVariables: Entering. (A mode change has occurred).</b>")
    state.vars = [:]
    state.initialized = false
    initialize()
    state.layoutMode = layoutMode.toString()

    int maxCols = (myColumns?.toString()?.isInteger() ? myColumns.toInteger() : 5)
    for (i = 1; i <= 40; i++) {
        if (settings["variableSource$i"] != null) app.removeSetting("variableSource$i")
        if (settings["myAttribute$i"] != null) app.removeSetting("myAttribute$i")
        if (settings["name$i"] != null) app.removeSetting("name$i")
        if (settings["actionA$i"] != null) app.removeSetting("actionA$i")
        if (settings["actionB$i"] != null) app.removeSetting("actionB$i")

        for (j = 1; j <= maxCols; j++) {
            def cell = "R${i}C${j}"
            if (settings["$cell"] != null) {
                log.info("Removing Cell: $cell")
                app.removeSetting("$cell")
            }
        }
    }
    for (i = 1; i <= 5; i++) {
        if (settings["Template$i"] != null) app.removeSetting("Template$i")
    }

    app.removeSetting("name1")
    app.updateSetting("myVariableCount", [value: "1", type: "enum"])
    app.updateSetting("myColumns", [value: "2", type: "enum"])
    app.updateSetting("rows", [value: "1", type: "enum"])

}

def supportFunction(supportCode) {
    if (supportCode == null || supportCode.toString() == "0") return
    log.info "Running supportFunction with code: $supportCode"
    switch (supportCode.toString()) {
        case "disableOverrides":
            app.updateSetting("isOverrides", false)
            break
        case "disableKeywords":
            app.updateSetting("myKeywordCount", 0)
            break
        case "disableThresholds":
            app.updateSetting("myThresholdCount", 0)
            break
        case "clearDeviceList":
            int varCount = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0)
            for (i = 1; i <= varCount; i++) {
                app.updateSetting("variableSource$i", [value: null, type: "enum"])
            }
            break
    }
}

def appButtonHandler(btn) {
    if (isLogTrace == true) log.trace("<b style='color:green;font-size:medium'>appButtonHandler: Clicked on button: $btn</b>")
    switch (btn) {
        case 'EnhancedView':
            state.flags.isEnhancedView = state.flags.isEnhancedView ? false : true
            break
        case 'btnHideLayoutMode':
            state.hidden.LayoutMode = state.hidden.LayoutMode ? false : true
            break
        case 'btnHideVariables':
            state.hidden.Variables = state.hidden.Variables ? false : true
            break
        case 'btnHideDeviceGroup':
            state.hidden.DeviceGroup = state.hidden.DeviceGroup ? false : true
            break
        case 'btnHideGridLayout':
            state.hidden.GridLayout = state.hidden.GridLayout ? false : true
            break
        case 'btnHideDesign':
            state.hidden.Design = state.hidden.Design ? false : true
            break
        case 'btnHidePublish':
            state.hidden.Publish = state.hidden.Publish ? false : true
            break
        case 'btnShowKeywords':
            state.show.Keywords = state.show.Keywords ? false : true
            break
        case 'btnShowThresholds':
            state.show.Thresholds = state.show.Thresholds ? false : true
            break
        case 'btnShowFormatRules':
            state.show.FormatRules = state.show.FormatRules ? false : true
            break
        case 'btnShowReplaceCharacters':
            state.show.ReplaceCharacters = state.show.ReplaceCharacters ? false : true
            break
        case 'btnClearLastVariable':
            myIndex = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0) + 30
            if (myIndex > 0) {
                app.updateSetting("myDevice${myIndex}", [type: "capability", value: []])
                myIndex = myIndex - 31
                app.updateSetting("myVariableCount", [value: "$myIndex", type: "enum"])
            }
            break
        case "Refresh":
            state.refresh = now()
            if (isLogAppPerformance) log.info("The user clicked refresh: " + (now() - state.refresh) / 1000 + " seconds")
            makeTable()
            break
        case "publish":
            if (isLogAppPerformance) log.info("The user clicked publish: " + (now() - state.refresh) / 1000 + " seconds")
            publishTable()
            createSchedule()
            break
        case "cannotPublish":
            cannotPublishTable()
            break
        case "General":
            app.updateSetting("activeButton", 1)
            break
        case "Title":
            app.updateSetting("activeButton", 2)
            break
        case "Headers":
            app.updateSetting("activeButton", 3)
            break
        case "Borders":
            app.updateSetting("activeButton", 4)
            break
        case "Rows":
            app.updateSetting("activeButton", 5)
            break
        case "Footer":
            app.updateSetting("activeButton", 6)
            break
        case "Highlights":
            app.updateSetting("activeButton", 7)
            break
        case "Styles":
            app.updateSetting("activeButton", 8)
            break
        case "Advanced":
            app.updateSetting("activeButton", 9)
            break
        case "test":
            test()
            break
        case "copyOverrides":
            state.flags.isCopyOverridesHelperCommand = true
            break
        case "appendOverrides":
            state.flags.isAppendOverridesHelperCommand = true
            break
        case "clearOverrides":
            state.flags.isClearOverridesHelperCommand = true
            break
        case "applyStyle":
            myStyle = loadStyle(applyStyleName.toString())
            applyStyle(myStyle)
            makeTable()
            break
        case "saveStyle":
            saveCurrentStyle(saveStyleName)
            state.flags.styleSaved = true
            break
        case "deleteStyle":
            deleteSelectedStyle(deleteStyleName)
            break
        case "importStyle":
            app.updateSetting("overrides", importStyleOverridesText)
            myOverridesMap.overrides = importStyleOverrides
            myImportMap = importStyleString(settings.importStyleText)
            myImportStyle = myImportMap.clone()
            applyStyle(myImportStyle)
            break
        case "clearImport":
            app.updateSetting("importStyleText", "")
            app.updateSetting("importStyleOverridesText", "")
            break
        case "publishSubscribe":
            if (isLogAppPerformance) log.info("The user clicked publish\\subscribe: " + (now() - state.refresh) / 1000 + " seconds")
            publishSubscribe()
            break
        case "unsubscribe":
            deleteSubscription()
            break
    }
}

def getOverrideCommands(myCategory) {
    if (isLogTrace == true) log.trace("<b>getOverrideCommands: Entering with $myCategory.</b>")
    def commandList = []
    overridesHelperMap = [:]
    switch (myCategory) {
        case "Animation":
            overridesHelperMap = parent.getOverrideAnimationList()
            break
        case "Background":
            overridesHelperMap = parent.getOverrideBackgroundList()
            break
        case "Border":
            overridesHelperMap = parent.getOverrideBorderList()
            break
        case "Classes":
            overridesHelperMap = parent.getOverrideClassList()
            break
        case "Cell Operations":
            overridesHelperMap = parent.getOverrideCellOperationsList()
            break
        case "Field Replacement":
            overridesHelperMap = parent.getOverrideFieldReplacementList()
            break
        case "Font":
            overridesHelperMap = parent.getOverrideFontList()
            break
        case "Margin & Padding":
            overridesHelperMap = parent.getOverrideMarginPaddingList()
            break
        case "Misc":
            overridesHelperMap = parent.getOverrideMiscList()
            break
        case "Text":
            overridesHelperMap = parent.getOverrideTextList()
            break
        case "Transform":
            overridesHelperMap = parent.getOverrideTransformList()
            break
    }

    overridesHelperMap?.each {
        key = it.key.toString()
        value = it.value.toString()
        details = value.tokenize('|')
        commandList.add(key)
    }
    return commandList.unique().sort()
}

def createCellTemplates() {
    if (isLogTrace && isLogDetails) log.info("<b>createCellTemplates: Entering.</b>")
    def attributeList = getSelectedAttributes()

    replacementList = attributeList.findAll { it != null }
    if (gatherDeviceDetails != null) replacementList += gatherDeviceDetails.findAll { it != null }

    def deviceCount = myDeviceList?.size() ?: 0
    int cols = (myColumns?.toString()?.isInteger() ? myColumns.toInteger() : 1)

    for (int i = 1; i <= deviceCount; i++) {
        for (int j = 1; j <= cols; j++) {
            template = settings["Template${j}"]
            if (template != null) {
                replacementList.each { value ->
                    if (template.toLowerCase().contains("${value.toLowerCase()}")) {
                        template = template.replaceAll("(?i)%${value}%", "%${value}${i}%")
                    }
                }
                app.updateSetting("R${i}C${j}", [value: "$template", type: "text"])
            }
        }
    }
}

void makeTable() {
    if (isLogTrace == true) log.trace("<b>makeTable: Entering.</b>")

    int numCols = (myColumns?.toString()?.isInteger() ? myColumns.toInteger() : 1)
    int numRows = (rows?.toString()?.isInteger() ? rows.toInteger() : 1)

    String HTMLCOMMENT = "<!--#comment#-->"
    String HTMLSTYLE1 = "<head>#head#<style>#class# #class1# #class2# #class3# #class4# #class5# #iFrame1#table.#id#{border-collapse:#bm#;width:#tw#%;height:#th#%;margin:Auto;font-family:'#tff#';font-size:#bfs#px;background:#tbc#;#table#;}"
    String HTMLSTYLE2 = ".#id# tr{color:#rtc#;text-align:#rta#;#row#}.#id# td{background:#rbc#;font-size:#rts#%;padding:#rp#px;#data#}</style>"
    String HTMLBORDERSTYLE = "<style>.#id# th,.#id# td{border:#bs# #bw#px #bc#;padding:#bp#px;border-radius:#br#px;#border#}</style>"
    String HTMLTITLESTYLE = "<style>ti#id#{display:block;color:#tc#;font-size:#ts#%;font-family:'#tff#';text-align:#ta#;padding:#tp#px;#title#}</style>"
    String HTMLHEADERSTYLE = "<style>.#id# th{background:#hbc#;color:#htc#;text-align:#hta#;font-size:#hts#%;padding:#hp#px;#header#}</style>"
    String HTMLARSTYLE = "<style>.#id# tr:nth-child(even){color:#ratc#;background:#rabc#;#alternaterow#;}</style>"
    String HTMLFOOTERSTYLE = "<style>ft#id#{display:block;text-align:#fa#;font-size:#fs#%;color:#fc#}</style>"
    String HTMLHIGHLIGHT1STYLE = "<style>h#id#1{color:#hc1#;font-size:#hts1#%;#high1#}</style>"
    String HTMLHIGHLIGHT2STYLE = "<style>h#id#2{color:#hc2#;font-size:#hts2#%;#high2#}</style>"
    String HTMLHIGHLIGHT3STYLE = "<style>h#id#3{color:#hc3#;font-size:#hts3#%;#high3#}</style>"
    String HTMLHIGHLIGHT4STYLE = "<style>h#id#4{color:#hc4#;font-size:#hts4#%;#high4#}</style>"
    String HTMLHIGHLIGHT5STYLE = "<style>h#id#5{color:#hc5#;font-size:#hts5#%;#high5#}</style>"
    String HTMLHIGHLIGHT6STYLE = "<style>h#id#6{color:#hc6#;font-size:#hts6#%;#high6#}</style>"
    String HTMLHIGHLIGHT7STYLE = "<style>h#id#7{color:#hc7#;font-size:#hts7#%;#high7#}</style>"
    String HTMLHIGHLIGHT8STYLE = "<style>h#id#8{color:#hc8#;font-size:#hts8#%;#high8#}</style>"
    String HTMLHIGHLIGHT9STYLE = "<style>h#id#9{color:#hc9#;font-size:#hts9#%;#high9#}</style>"
    String HTMLHIGHLIGHT10STYLE = "<style>h#id#10{color:#hc10#;font-size:#hts10#%;#high10#}</style>"
    String HTMLDIVSTYLE = "<style>div.#id#{height:auto;background:#fbc#;padding:20px;#frame#}</style>"
    String HTMLDIVSTART = "<div class=#id#>"
    String HTMLTITLE = "<ti#id#>#tt#</ti#id#>"
    String HTMLTABLESTART = "</head><body><table class=#id#>"
    String HTMLR0 = generateTableHeader(numCols)

    if (isMergeHeaders == "Merge All") HTMLR0 = "<tr><th colspan=${numCols}>#R0C1#</th></tr>"
    if (numCols == 3 && isMergeHeaders == "Merge 2 & 3") HTMLR0 = "<thead><tr><th>#R0C1#</th><th colspan=${2}>#R0C2#</th></tr></thead>"
    if (numCols == 4 && isMergeHeaders == "Merge 2 & 3") HTMLR0 = "<thead><tr><th>#R0C1#</th><th colspan=${2}>#R0C2#</th><th>#R0C3#</th></tr></thead>"
    if (numCols == 4 && isMergeHeaders == "Merge 2, 3 & 4") HTMLR0 = "<thead><tr><th>#R0C1#</th><th colspan=${3}>#R0C2#</th></tr></thead>"
    if (numCols == 5 && isMergeHeaders == "Merge 2 & 3") HTMLR0 = "<thead><tr><th>#R0C1#</th><th colspan=${2}>#R0C2#</th><th>#R0C3#</th><th>#R0C4#</th></tr></thead>"
    if (numCols == 5 && isMergeHeaders == "Merge 2, 3 & 4") HTMLR0 = "<thead><tr><th>#R0C1#</th><th colspan=${3}>#R0C2#</th><th>#R0C3#</th></tr></thead>"
    if (isLogHTML) log.info "Table Header is: " + unHTML(HTMLR0)
    String HTMLGRID = generateTableRows(numRows, numCols)
    String HTMLTABLEEND = "</table>"
    String HTMLFOOTER = "<ft#id#>#ft#</ft#id#>"
    String HTMLDIVEND = "</div>"
    String HTMLEND = "</body>"

    if (isComment == false) HTMLCOMMENT = ""
    if (isFrame == false) {
        HTMLDIVSTYLE = ""; HTMLDIVSTART = ""; HTMLDIVEND = ""
    }
    if (isAlternateRows == false) HTMLARSTYLE = ""

    int kwCount = (myKeywordCount?.toString()?.isInteger() ? myKeywordCount.toInteger() : 0)
    if (kwCount < 1) HTMLHIGHLIGHT1STYLE = ""
    if (kwCount < 2) HTMLHIGHLIGHT2STYLE = ""
    if (kwCount < 3) HTMLHIGHLIGHT3STYLE = ""
    if (kwCount < 4) HTMLHIGHLIGHT4STYLE = ""
    if (kwCount < 5) HTMLHIGHLIGHT5STYLE = ""

    int thCount = (myThresholdCount?.toString()?.isInteger() ? myThresholdCount.toInteger() : 0)
    if (thCount < 1) HTMLHIGHLIGHT6STYLE = ""
    if (thCount < 2) HTMLHIGHLIGHT7STYLE = ""
    if (thCount < 3) HTMLHIGHLIGHT8STYLE = ""
    if (thCount < 4) HTMLHIGHLIGHT9STYLE = ""
    if (thCount < 5) HTMLHIGHLIGHT10STYLE = ""

    if (isFooter == false) {
        HTMLFOOTERSTYLE = ""; HTMLFOOTER = ""
    }
    if (isBorder == false) HTMLBORDERSTYLE = ""
    if (isTitle == false) {
        HTMLTITLESTYLE = ""; HTMLTITLE = ""
    }
    if (isHeaders == false) {
        HTMLHEADERSTYLE = ""; HTMLR0 = ""
    }

    def interimHTML = HTMLCOMMENT + HTMLSTYLE1 + HTMLSTYLE2 + HTMLDIVSTYLE + HTMLBORDERSTYLE + HTMLTITLESTYLE + HTMLHEADERSTYLE + HTMLARSTYLE + HTMLFOOTERSTYLE + "#HIGHLIGHTSTARTSTYLE#" + HTMLHIGHLIGHT1STYLE + HTMLHIGHLIGHT2STYLE + HTMLHIGHLIGHT3STYLE + HTMLHIGHLIGHT4STYLE + HTMLHIGHLIGHT5STYLE
    interimHTML += HTMLHIGHLIGHT6STYLE + HTMLHIGHLIGHT7STYLE + HTMLHIGHLIGHT8STYLE + HTMLHIGHLIGHT9STYLE + HTMLHIGHLIGHT10STYLE + "#HIGHLIGHTENDSTYLE#" + HTMLDIVSTART + HTMLTITLE + HTMLTABLESTART + HTMLR0
    interimHTML += HTMLGRID + HTMLTABLEEND + HTMLFOOTER + HTMLDIVEND + HTMLEND
    if (isLogHTML) log.info("makeTable: HTML Template is: ${interimHTML}")

    def myTemplate = fillStyle()

    if (isLogStyles) log.info("Tile Builder Grid CSS Font Settings - Font Family: '${myTemplate['#tff#']}', Base Font Size: ${myTemplate['#bfs#']}px")
    if (isLogHTML) log.info("makeTable: myTemplate with Row Data is : ${myTemplate}")

    myIndex = 0

    for (int i = 1; i <= numRows; i++) {
        for (j = 1; j <= numCols; j++) {
            def cell = "R${i}C${j}"
            def myText = settings["$cell"]
            if (isLogHTML == true) log.info("Row is: $i  Column is: $j  Cell is: $cell  and value is: $myText")
            if (myText != "?" && myText != null) interimHTML = interimHTML.replace("#${cell}#", myText)
            else interimHTML = interimHTML.replace("#${cell}#", "")
        }
    }

    myTemplate.each { it, value ->
        if (isLogStyles == true && isLogDetails) log.info("Iterating myTemplate: it is: $it and value is: $value")
        interimHTML = interimHTML.replaceAll(it, value != null ? Matcher.quoteReplacement(value.toString()) : "")
    }

    for (int i = 0; i < 2; i++) {
        state.vars?.each { varName, value ->
            if (isLogHTML && isLogDetails) log.info("makeTable: Loop(i) - Replacing var: $varName with: $value")
            varName = varName.toString().toLowerCase()
						
            myValue = checkContentsforVariables(varName, value != null ? value.toString() : "")
			
			if (varName.contains("devicelabel")) { myValue = getShortName(myValue) }
			if (varName.contains("devicename")) { myValue = getShortName(myValue) }
            interimHTML = interimHTML.replaceAll( "(?i)" + Pattern.quote("%${varName}%"), Matcher.quoteReplacement(myValue)	)
        }
    }

    state.highlightStyles = toHTML(extractValueBetweenStrings(unHTML(interimHTML), "#HIGHLIGHTSTARTSTYLE#", "#HIGHLIGHTENDSTYLE#"))

    interimHTML = replaceDateTimeVariables(interimHTML)
	interimHTML = interimHTML.replaceAll("(?i)%count%", numRows.toString())
    interimHTML = toHTML(interimHTML)

    iframeHTML = scrubHTML(interimHTML, false)
    finalHTML = scrubHTML(interimHTML, true)
    state.interimHTML = interimHTML
    getHTMLSize(finalHTML.toString(), interimHTML.toString())

    if (state.HTMLsizes.Final < 4096) {
        state.iframeHTML = iframeHTML
        state.HTML = finalHTML
        if (isLogTrace || isLogHTML) log.info("makeTable: HTML final size is <= than 4,096 bytes.")
    } else {
        state.iframeHTML = iframeHTML
        state.HTML = "<b>HTML length exceeded 4,096 bytes for '${myTileName}' (${state.HTMLsizes.Final}).</b>"
        if (isLogTrace || isLogHTML) log.info("makeTable: HTML final size is > 4,096 bytes.")
    }
}

def checkContentsforVariables(myVar, myValue) {
    if (myValue == null) return ""
    def matcher2 = (myVar =~ /(.*?)(\d+)$/)
    def index = matcher2.find() ? matcher2.group(2).toInteger() : null

    def pattern = Pattern.compile("%\\S{2,}?%")
    myValue = myValue.replaceAll(pattern) { match ->
        match[0..-2] + (index != null ? index : "") + "%"
    }
    return myValue
}

def replaceDateTimeVariables(interimHTML) {
    if (interimHTML == null) return ""
    def myDay = new Date().format('E')
    def myDate = new Date().format('dd-MM')
    def myDate1 = new Date().format('MMM-dd')
    def myTime = new Date().format('HH:mm a')
    def myTime1 = new Date().format('HH:mm')
    def myTime2 = new Date().format('h:mm a')
    def today = java.time.LocalDate.now().dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
    def tomorrow = java.time.LocalDate.now().plusDays(1).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()).toString()
    def dayAfterTomorrow = java.time.LocalDate.now().plusDays(2).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()).toString()

    def sunrise = getTodaysSunrise()
    def mySunrise = sunrise ? sunrise.format('HH:mm a') : ""
    def mySunrise1 = sunrise ? sunrise.format('HH:mm') : ""
    def mySunrise2 = sunrise ? sunrise.format('h:mm a') : ""

    def sunset = getTodaysSunset()
    def mySunset = sunset ? sunset.format('HH:mm a') : ""
    def mySunset1 = sunset ? sunset.format('HH:mm') : ""
    def mySunset2 = sunset ? sunset.format('h:mm a') : ""

    interimHTML = interimHTML.replaceAll("(?i)%day%", myDay)
    interimHTML = interimHTML.replaceAll("(?i)%date%", myDate)
    interimHTML = interimHTML.replaceAll("(?i)%date1%", myDate1)
    interimHTML = interimHTML.replaceAll("(?i)%time%", myTime)
    interimHTML = interimHTML.replaceAll("(?i)%time1%", myTime1)
    interimHTML = interimHTML.replaceAll("(?i)%time2%", myTime2)
    interimHTML = interimHTML.replaceAll("(?i)%today%", today)
    interimHTML = interimHTML.replaceAll("(?i)%tomorrow%", tomorrow)
    interimHTML = interimHTML.replaceAll("(?i)%dayAfterTomorrow%", dayAfterTomorrow)
    interimHTML = interimHTML.replaceAll("(?i)%sunrise%", mySunrise)
    interimHTML = interimHTML.replaceAll("(?i)%sunrise1%", mySunrise1)
    interimHTML = interimHTML.replaceAll("(?i)%sunrise2%", mySunrise2)
    interimHTML = interimHTML.replaceAll("(?i)%sunset%", mySunset)
    interimHTML = interimHTML.replaceAll("(?i)%sunset1%", mySunset1)
    interimHTML = interimHTML.replaceAll("(?i)%sunset2%", mySunset2)
    return interimHTML
}

def isStringMatch(matchType, keyword, attributeValue) {
    if (isLogTrace || isLogHighlights) log.trace("<b>isStringMatch: Entering with $matchType  $keyword  $attributeValue</b>")
    if (attributeValue == null || keyword == null) return false
    switch (matchType) {
        case "Value Matches Keyword (Match Case)":
            if (keyword.trim() == attributeValue.toString().trim()) {
                if (isLogHighlights) log.info("<b>isStringMatch: Taking Branch Value Matches Keyword (Match Case)")
                return true
            }
            break
        case "Value Matches Keyword (Ignore Case)":
            if (keyword.trim().toUpperCase() == attributeValue.toString().trim().toUpperCase()) {
                if (isLogHighlights) log.info("<b>isStringMatch: Taking Branch Value Matches Keyword (Ignore Case)")
                return true
            }
            break
        case "Value Contains Keyword (Match Case)":
            if (attributeValue.toString().contains(keyword.trim())) {
                if (isLogHighlights) log.info("<b>isStringMatch: Taking Branch Value Contains Keyword (Match Case)")
                return true
            }
            break
        case "Value Contains Keyword (Ignore Case)":
            if (attributeValue.toString().toUpperCase().contains(keyword.trim().toUpperCase())) {
                if (isLogHighlights) log.info("<b>isStringMatch: Taking Branch Value Contains Keyword (Ignore Case)")
                return true
            }
            break
    }
    return false
}

def highlightValue(attributeValue, myIndex) {
    if (isLogTrace && isLogDetail || isLogHighlights) log.trace("<b>highlightValue: Entering with $attributeValue  $myIndex")
    if (attributeValue == null) return invalidAttribute.toString()
    def originalValue = attributeValue.toString()
    def returnValue
    dataType = getDataType(attributeValue)
    if (isLogHighlights && isLogDetails) log.info("highlightValue: Received attributeValue: ${attributeValue} with index: $myIndex and DataType is: $dataType")
    if (dataType == "String" && settings["actionB$myIndex"] == "Replace Chars") {
        def oldCharacters = settings["oc1"] ?: ""
        def newCharacters = settings["nc1"] ?: ""
        if (oldCharacters != "?" && newCharacters != "?") attributeValue = attributeValue.toString().replace(oldCharacters, newCharacters)
    }
    if (settings["actionB$myIndex"] == "Format Rule 1") {
        returnValue = (settings["fr1"] + "").replace("%value%", attributeValue.toString())
        if (isLogHighlights && isLogDetails) log.info("highlightValue: Processed Format Rule 1, returning: " + unHTML(returnValue))
        return returnValue
    }
    if (settings["actionB$myIndex"] == "Format Rule 2") {
        returnValue = (settings["fr2"] + "").replace("%value%", attributeValue.toString())
        if (isLogHighlights && isLogDetails) log.info("highlightValue: Processed Format Rule 2, returning: " + unHTML(returnValue))
        return returnValue
    }
    if (settings["actionB$myIndex"] == "Format Rule 3") {
        returnValue = (settings["fr3"] + "").replace("%value%", attributeValue.toString())
        if (isLogHighlights && isLogDetails) log.info("highlightValue: Processed Format Rule 3, returning: " + unHTML(returnValue))
        return returnValue
    }
    if (settings["actionB$myIndex"] == "All Keywords") {
        int kwCount = (myKeywordCount?.toString()?.isInteger() ? myKeywordCount.toInteger() : 0)
        for (i = 1; i <= kwCount; i++) {
            def keyword = settings["k$i"]
            def keywordTr = settings["ktr$i"]
            if (isLogHighlights) log.info("highlightValue: Attribute is: ${attributeValue} Keyword is: $keyword and Search is:" + settings["kop$i"].toString())
            result = isStringMatch(settings["kop$i"].toString(), keyword, attributeValue)
            if (result == true) {
                if (isLogHighlights && isLogDetails) log.info("highlightValue: Keyword ${attributeValue} meets conditions for Keyword$i.")
                if (keywordTr && keywordTr.size() > 0) {
                    returnValue = keywordTr.replace("%value%", attributeValue.toString())
                    returnValue = "[hqq$i]" + returnValue + "[/hqq$i]"
                    if (isLogHighlights && isLogDetails) log.info("highlightValue: Returning: " + unHTML(returnValue))
                    return returnValue
                }
            }
        }
        returnValue = attributeValue
        if (isLogHighlights) log.info("highlightValue: Returning: " + unHTML(returnValue))
        return returnValue
    }
    returnValue = attributeValue.toString()
    def lastThreshold = 0
    i = 6
    int thCount = (myThresholdCount?.toString()?.isInteger() ? myThresholdCount.toInteger() : 0)
    while (i <= thCount + 5) {
        myVal1 = settings["tcv$i"]
        myVal2 = settings["top$i"]
        myThresholdText = "Threshold " + (i - 5).toString()
        if (isLogHighlights) log.info("i is: $i.  tcv$i is: $myVal1  top$i is: $myVal2  dataType is $dataType  Threshold is: $myThresholdText originalValue is: $originalValue")
        if (settings["tcv$i"] != null && settings["tcv$i"] != "" && settings["tcv$i"] != "None" &&
            ((settings["actionB$myIndex"] == "All Thresholds") ||
             settings["actionB$myIndex"] == myThresholdText ||
             (settings["actionB$myIndex"] == "Threshold 1 & 2"    && (i == 6 || i == 7))  ||
             (settings["actionB$myIndex"] == "Threshold 3 & 4" && (i == 8 || i == 9))) &&
            (dataType == "Integer" || dataType == "Float")) {
            if ((settings["top$i"] == "1" || settings["top$i"] == "<=") && originalValue.toFloat() <= settings["tcv$i"].toFloat()) {
                if (isLogHighlights && isLogDetails) log.info("highlightValue: A <= than condition was met.")
                if ((settings["ttr$i"] != null && settings["ttr$i"] != " ") && settings["ttr$i"] != "?") {
                    returnValue = settings["ttr$i"]
                }
                lastThreshold = i
            }
            if ((settings["top$i"] == "2" || settings["top$i"] == "==") && originalValue.toFloat() == settings["tcv$i"].toFloat()) {
                if (isLogHighlights && isLogDetails) log.info("highlightValue: An == than condition was met.")
                if (settings["ttr$i"] != null && settings["ttr$i"] != " " && settings["ttr$i"] != "?") {
                    returnValue = settings["ttr$i"]
                }
                lastThreshold = i
            }
            if ((settings["top$i"] == "3" || settings["top$i"] == ">=") && originalValue.toFloat() >= settings["tcv$i"].toFloat()) {
                if (isLogHighlights && isLogDetails) log.info("highlightValue: A >= than condition was met.")
                if (settings["ttr$i"] != null && settings["ttr$i"] != " " && settings["ttr$i"] != "?") {
                    returnValue = settings["ttr$i"]
                }
                lastThreshold = i
            }
        }
        i = i + 1
    }
    if (lastThreshold == 0) {
        returnValue = attributeValue
        if (isLogHighlights) log.info("highlightValue: Threshold NOT matched, returning: " + returnValue)
        return returnValue
    } else {
        returnValue = returnValue.toString().replace("%value%", attributeValue.toString())
        returnValue = "[hqq$lastThreshold]" + returnValue + "[/hqq$lastThreshold]"
        if (isLogHighlights) log.info("highlightValue: Threshold MATCHED, returning: " + returnValue)
        return returnValue
    }
}

def generateTableHeader(cols) {
    if (isLogTrace && isLogDetails) log.trace("<b>generateTableHeader: Entering with $cols</b>")

    def html = "<thead><tr>"
    (1..cols).each { col -> html += "<th>#R0C${col}#</th>" }
    html += """</tr></thead>"""
    return html
}

def generateTableRows(rows, cols) {
    if (isLogTrace && isLogDetails) log.trace("<b>generateTableRows: Entering with $rows  $cols</b>")

    def html = """<tbody>"""
    for (row = 1; row <= rows; row++) {
        html += "<tr>"
        (1..cols).each { col -> html += "<td>#R${row}C${col}#</td>" }
        html += "</tr>"
    }
    html += """</tbody>"""
    return html
}

def getOpenWeatherPNG(weatherIcon) {
    if (isLogTrace && isLogDetails) log.trace("<b>getOpenWeatherPNG: Entering with $weatherIcon</b>")
    if (weatherIcon == null) return ""
    return "<img src=https://openweathermap.org/img/wn/" + weatherIcon + ".png>"
}

String getOpenWeatherEmoji(weatherCode) {
    if (isLogTrace && isLogDetails) log.trace("<b>getOpenWeatherEmoji: Entering with $weatherCode</b>")
    if (isLogCleanups) log.info("getOpenWeatherEmoji: Received weatherCode is: $weatherCode")
    if (weatherCode == null) return "❓"
    emojiMap = ["01d": "☀️", "01n": "🌙", "02d": "🌤️", "02n": "🌤️", "03d": "☁️", "03n": "☁️", "04d": "☁︎", "04n": "☁︎", "09d": "☔", "09n": "☔", "10d": "🌧️", "10n": "🌧️", "11d": "⛈️", "11n": "⛈️", "13d": "❄️", "13n": "❄️", "50d": "🌫️", "50n": "🌫️"]
    return emojiMap[weatherCode.toString()] ?: "❓"
}

def getImageURL(myString) {
    if (isLogTrace && isLogDetails) log.trace("<b>getImageURL: Entering with $myString.</b>")
    if (myString == null) return ""
    url = myString.toString()
    url = "<img src=" + url + ">"
    if (isLogCleanups) log.info("getImageURL: url: " + unHTML(url) + " ")
    return url
}

def getAltImageURL(myString) {
    if (isLogTrace && isLogDetails) log.trace("<b>getAltImageURL: Entering with $myString.</b>")
    if (myString == null) return ""
    
    def fileName = myString.toString().tokenize('/').last()
    def newURL = "https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/" + fileName
    def url = "<img src=" + newURL + ">"
    if (isLogCleanups) log.info("getAltImageURL: url: " + unHTML(url) + " ")
    return url
}

void deleteSubscription() {
    if (isLogTrace) log.trace("<b>deleteSubscription: Entering.</b>")
    if (isLogPublish) log.info("deleteSubscription: Deleted all subscriptions. To verify click on the App ⚙️ Symbol and look for the Event Subscriptions section. ")
    unsubscribe()
}

void publishSubscribe() {
    if (isLogTrace || isLogPublish) log.trace("<b>publishSubscribe: Entering and creating subscriptions for Tile: $myTile with description: $myTileName.</b>")
    unsubscribe()
    def device
    def attribute

    if (layoutMode.toString() == "Free Form") {
        int varCount = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0)
        (1..varCount).each { i ->
            if (settings["variableSource${i}"] == "Default Device") device = settings["defaultDevice"]
            if (settings["variableSource${i}"] == "Device Attribute") device = settings["myDevice$i"]

            if (settings["variableSource${i}"] == "Default Device" || settings["variableSource${i}"] == "Device Attribute") {
                attribute = settings["myAttribute$i"]
                if (device && attribute) subscribeAttribute(device, attribute, "handler")
            }

            if (settings["variableSource${i}"] == "Hub Variable") {
                variable = settings["myHubVariable$i"]?.toString()
                if (variable) subscribeVariable(variable, "handler")
            }
        }
    }

    if (layoutMode.toString() == "Device Group") {
        myDeviceList?.each { it ->
            device = it
            int varCount = (myVariableCount?.toString()?.isInteger() ? myVariableCount.toInteger() : 0)
            (1..varCount).each { i ->
                attribute = settings["myAttribute$i"]
                if (device && attribute) subscribeAttribute(device, attribute, "handler")
            }
        }
    }

    publishTable()
}

def getTile(){
    if (isLogDebug) log.debug "getTile called: $params source: ${request.requestSource}"
    if (oversizeTileHandling == "Cloud Endpoint") {    
        render contentType: "text/html;charset=UTF-8", data:(state.HTML ?: "<h3>Tile data unavailable</h3>"), status:200
    }
    else { render contentType: "text/html;charset=UTF-8", data:"<H1>Tile Builder</H1><H2>Cloud Endpoint Inactive</H2>Check your <b>Oversize Tile Handling</b> configuration for <b>${myTileName}</b>.", status:200 }
}

void subscribeVariable(varName, handler) {
    if (isLogTrace) log.trace("<b>subscribeVariable: Entering with $location  $varName  $handler.</b>")
    try {
        if (varName) {
            subscribe(location, "variable:$varName", "handler")
            if (isLogPublish) log.info("subscribeVariable: Subscribed to Hub variable: $varName")
        }
    }
    catch (Exception ignored) {
        if (isLogPublish) log.error("subscribeVariable: Error subscribing to Hub variable: $varName")
    }
}

void subscribeAttribute(device, attribute, handler) {
    if (isLogTrace) log.trace("<b>subscribeAttribute: Entering with $device  $attribute  $handler.</b>")
    try {
        if (device && attribute) {
            subscribe(device, attribute, "handler")
            if (isLogPublish) log.info("subscribeAttribute: Subscribed to device: $device - attribute: $attribute.")
        }
    }
    catch (Exception ignored) {
        if (isLogPublish) log.error("subscribeAttribute: Error subscribing to device: $device - attribute: $attribute.")
    }
}

def handler(evt) {
    def currentVersion = state.variablesVersion?.toString()?.isInteger() ? state.variablesVersion.toString().toInteger() : 0
	if (currentVersion < codeVersion()) updateVariables()
    
    def currentTime = now()
    
    if (isLogTrace) log.trace("<b>handler: Entering with $evt</b>")
    if (isLogPublish) log.info("handler: Event received from Device:${evt?.device}  -  Attribute:${evt?.name}  -  Value:${evt?.value}")
    
    def lastPublicationTime = state.publish?.lastPublished ?: 0
    def nextPublicationTime
    
    int delayMins = (republishDelay?.toString()?.isInteger() ? republishDelay.toInteger() : 0)
    if (delayMins > 0) {
        nextPublicationTime = (delayMins * 60 * 1000) + lastPublicationTime
    } else {
        nextPublicationTime = currentTime
    }
    
    if (isLogPublish) log.info("republishDelay is: $republishDelay mins. LastPub:$lastPublicationTime  NextPub:$nextPublicationTime")
    
    def delay = nextPublicationTime - currentTime
    
    if (delay > 0) {
        runInMillis(delay, publishTable, [overwrite: true])
        if (isLogPublish) log.info("handler: republishDelay of $republishDelay minutes has not been met. Publication deferred. (" + delay / 1000 + " seconds)")
    } else {
        int timeoutMs = (eventTimeout?.toString()?.isInteger() ? eventTimeout.toInteger() : 2000)
        runInMillis(timeoutMs, publishTable, [overwrite: true])
    }
}

void publishTable() {
    if (isLogTrace || isLogPublish) log.info("<b>publishTable: Tile $myTile ($myTileName) is being refreshed.</b>")
    updateAppVariables()
    
    if (oversizeTileHandling == "Cloud Endpoint") {
        try {
            if( !state.accessToken ) createAccessToken()
            state.cloudEndpoint = getFullApiServerUrl() + "/tb?access_token=" + state.accessToken
        }
        catch (Exception e){
            log.error("This app is not OAuth Enabled.  Go to: <b>Developer Tools</b> / <b>Apps Code</b> and open the code for this app.  Click on <b>OAuth</b> and then <b>Enable OAuth in App</b> and leave it athe default values.")
        }
    }

    if (layoutMode.toString() == "Free Form") {
        getVariablesFreeForm()
    }
    if (layoutMode.toString() == "Device Group") {
        createCellTemplates(); varString = getVariablesDeviceGroup()
    }
    makeTable()

    myStorageDevice = parent.getStorageDevice()
    if (myStorageDevice == null) {
        log.error("publishTable: myStorageDevice is null. Is the device created and available? This error can occur immediately upon hub startup. Nothing published.")
        return
    }

    if (isLogPublish) log.info("publishTable: Size is: ${state.HTML?.size()}")
    
    if (state.publish == null) state.publish = [:]

    if (state.HTML != null && state.HTML.size() < 1024) {
        myStorageDevice.createTile(settings.myTile, state.HTML, settings.myTileName)
        state.publish.lastPublished = now()
        if (isLogTrace || isLogPublish) log.info("<b style='color:orange;font-size:medium'>publishTable: Tile size < 1,024 bytes. Published to attribute: $settings.myTile</b>")
    }
    else { 
		if (oversizeTileHandling == "Cloud Endpoint") {
			def src = state.cloudEndpoint
			def stubHTML = "<!--Generated:" + now() + "-->" + """<div style='height:100%; width:100%; scrolling:no; overflow:hidden;'><iframe src=""" + src + """ style='height: 100%; width:100%; border: none; scrolling:no; overflow: hidden;'></iframe><div>"""
			if (isLogEvents) log.debug ("stub is : ${unHTML(stubHTML)}")
			
			myStorageDevice.createTile(settings.myTile, stubHTML, settings.myTileName)
            state.publish.lastPublished = now()
			return
		}
		
		def prefix = parent.getStorageShortName()
		def fileName = prefix + "_Tile_" + myTile.toString() + ".html"
		if (isLogEvents) log.debug ("filename is: ${fileName}")
		def myBytes = state.HTML ? state.HTML.getBytes("UTF-8") : "".getBytes("UTF-8")
		
		try {
			def myIP = location.hub.localIP
			uploadHubFile("${fileName}", myBytes)
			pauseExecution (250)
			def src = "http://" + myIP + "/local/" + fileName
			def stubHTML = "<!--Generated:" + now() + "-->" + """<div style='height:100%; width:100%; scrolling:no; overflow:hidden;'><iframe src=""" + src + """ style='height: 100%; width:100%; border: none; scrolling:no; overflow: hidden;'></iframe><div>"""
			if (isLogEvents) log.debug ("stub is : ${unHTML(stubHTML)}")
		
			myStorageDevice.createTile(settings.myTile, stubHTML, settings.myTileName)
            state.publish.lastPublished = now()
		}
		catch (Exception e){
			if ( isLogError ) log.error ("Exception ${e} in publishTable. Probably an error uploading file to hub.") 
			def myTime = new Date().format('E @ HH:mm a')
			myStorageDevice.createTile(settings.myTile, "The tile did not upload\\update correctly. Check the logs. ${myTime}", settings.myTileName)
			if (isLogTrace || isLogPublish) log.info("<b style='color:red;font-size:medium'>publishTable: The tile did not upload or update correctly. Check the logs.</b>")
		}
	}
}

void cannotPublishTable() {
    log.error("cannotPublishTile: Tile $myTile ($myTileName) cannot be published because it's size is great than 4,096 bytes.")
}

def getHTMLSize(String finalHTML, String interimHTML) {
    if (isLogTrace && isLogDetails || isLogPublish) log.trace("<b>getHTMLSize: Entering (parameters not shown)</b>.")
    String fHTML = finalHTML ?: ""
    String iHTML = interimHTML ?: ""
    state.HTMLsizes = [Comment: countBetween(fHTML, "<!--", "-->"), Head: countBetween(fHTML, "<head>", "</head>"), Body: countBetween(fHTML, "<body>", "</body>"), Interim: iHTML.size(), Final: fHTML.size()]
}

def saveCurrentStyle(String styleName) {
    if (isLogTrace && isLogDetails || isLogStyles) log.trace("<b>saveCurrentStyle: Entering with $styleName</b>")
    styleMap = state.myActiveStyleMap
    saveStyleName = "Style-AM-${styleName}"
    if (isLogStyles) log.info("saveCurrentStyle: Child saving style '${saveStyleName}' with settings: ${styleMap}")
    parent.saveStyle(saveStyleName, styleMap)
}

def loadStyle(String styleName) {
    if (isLogTrace && isLogDetails || isLogStyles) log.trace("<b>loadStyle: Entering with $styleName</b>")
    myStyle = parent.loadStyle(styleName)
    if (isLogStyles) log.info("loadStyle: style ${styleName} received from parent with settings: ${myStyle}.")
    return myStyle
}

def deleteSelectedStyle(String styleName) {
    if (isLogTrace && isLogDetails || isLogStyles) log.trace("<b>deleteSelectedStyle: Entering with $styleName</b>")
    if (isLogStyles) log.info("deleteSelectedStyle: Deleting Style ${styleName}.")
    parent.deleteStyle(styleName)
}

def importStyle() {
    if (isLogTrace && isLogDetails || isLogStyles) log.trace("<b>importStyle: Entering.</b>")
    if (isLogStyles) log.info("importStyleString: Importing style.")
    myStyle = importStyleString(settings.importStyleText)
    newStyle = [myStyle]
    return newStyle
}

def applyStyle(style) {
    if (isLogTrace && isLogDetails || isLogStyles) log.trace("<b>applyStyle: Entering with $style</b>")
    if (isLogStyles) log.info("applyStyle: Received style: ${style}")
    def exclusionList1 = ["hc1", "hts1", "hc2", "hts2", "hc3", "hts3", "hc4", "hts4", "hc5", "hts5", "hc6", "hts6", "hc7", "hts7", "hc8", "hts8", "hc9", "hts9", "hc10", "hts10"]
    def exclusionList2 = ["myKeywordCount", "k1", "ktr1", "k2", "ktr2", "k3", "ktr3", "k4", "ktr4", "k5", "ktr5"]
    def exclusionList3 = ["myThresholdCount", "top1", "tcv1", "ttr1", "top2", "tcv2", "ttr2", "top3", "tcv3", "ttr3", "top4", "tcv4", "ttr4", "top5", "tcv5", "ttr5"]
    def exclusionList4 = ["isTitleShadow", "shcolor", "shhor", "shver", "shblur"]
    def combinedExclusionList = []
    combinedExclusionList.addAll(exclusionList1 + exclusionList2 + exclusionList3 + exclusionList4)

    style?.each { mySetting, myValue ->
        mySetting = mySetting.toString().replaceAll("#", "")
        if (isLogStyles) log.info("setting is: ${mySetting} and value is: ${myValue}")
        if (!combinedExclusionList.contains(mySetting)) {
            myClass = getSettingType(mySetting)
            if (myClass == "color") app.updateSetting(mySetting, [value: myValue, type: "color"])
            if (myClass == "enum") app.updateSetting(mySetting, [value: myValue.toString(), type: "enum"])
            if (myClass == "bool") app.updateSetting(mySetting, [value: myValue.toString(), type: "bool"])
            if (myClass == "text") app.updateSetting(mySetting, [value: myValue.toString(), type: "text"])
            if (myClass == "textarea") app.updateSetting(mySetting, [value: myValue.toString(), type: "textarea"])
            if (myClass == null) log.warn("applyStyle: Found setting: ${mySetting} in style with value: ${myValue} but no such setting exists. This is not harmful and does not affect the operation of the program.")
        }
    }
}

def importStyleString(styleString) {
    if (isLogTrace && isLogDetails || isLogStyles) log.trace("<b>importStyleString: Entering with $styleString</b>")
    if (styleString == null) return [:]
    String sString = styleString.toString().replace(", ", ",")
    if (isLogStyles) log.info("importStyleString: Style is: ${sString}")
    def newStyle = [:]

    myArr = sString.tokenize(",")
    myArr.each {
        it = it.replace("[", "")
        it = it.replace("]", "")
        it = it.replace("[[", "[")
        it = it.replace("]]", "]")
        it = it.replace("[ ", "[")
        it = it.replace(" ]", "]")

        details = it.tokenize(":")
        if (isLogStyles && isLogDetails) log.info("importStyleString: Details is: ${details}")
        if (details[0] != null) d0 = details[0].trim()
        if (details[1] != null) d1 = details[1].trim()
        if (d0 != null && d1 != null) {
            if (isLogStyles && isLogDetails) log.info("d0 is:${d0} and d1 is: ${d1}")
            newStyle."${d0}" = d1
        }
    }
    if (isLogStyles) log.info("importStyleString: Returning - ${newStyle}")
    return newStyle
}

def fillStyle() {
    if (isLogTrace && isLogDetails || isLogStyles) log.trace("<b>fillStyle: Entering.</b>")

    def myRP

    if (isLogStyles && isLogDetails) log.info("fillStyle: isBorder: ${isBorder}")
    if (isLogStyles && isLogDetails) log.info("fillStyle: bp: ${settings.bp}")
    if (isBorder == true && bp != null && bp.toInteger() > 0) {
        if (isLogStyles && isLogDetails) log.info("fillStyle: Border is on and > 0")
        myHP = "0"; myRP = "0"
    } else {
        myHP = hp; myRP = rp
    }

    float tboVal = tbo != null ? tbo.toFloat() : 1.0f
    float toVal = to != null ? to.toFloat() : 1.0f
    float boVal = bo != null ? bo.toFloat() : 1.0f
    float hboVal = hbo != null ? hbo.toFloat() : 1.0f
    float htoVal = hto != null ? hto.toFloat() : 1.0f
    float rboVal = rbo != null ? rbo.toFloat() : 1.0f
    float rtoVal = rto != null ? rto.toFloat() : 1.0f

    def mytbc = convertToHex8(tbc, tboVal)
    def mytc = convertToHex8(tc, toVal)
    def mybc = convertToHex8(bc, boVal)
    def myhbc = convertToHex8(hbc, hboVal)
    def myhtc = convertToHex8(htc, htoVal)
    def myrbc = convertToHex8(rbc, rboVal)
    def myrtc = convertToHex8(rtc, rtoVal)

    Hex8ColorScheme = ["#tbc#": mytbc, "#tc#": mytc, "#hbc#": myhbc, "#htc#": myhtc, "#rbc#": myrbc, "#rtc#": myrtc, "#bc#": mybc]
    titleScheme = ["#tt#": tt, "#ts#": ts, "#tc#": tc, "#tp#": tp, "#ta#": ta, "#to#": to]
    headerScheme = ["#R0C1#": R0C1, "#R0C2#": R0C2, "#R0C3#": R0C3, "#R0C4#": R0C4, "#R0C5#": R0C5, "#hbc#": hbc, "#hbo#": hbo, "#htc#": htc, "#hto#": hto, "#hts#": hts, "#hta#": hta, "#hp#": myHP]
    rowScheme = ["#rbc#": rbc, "#rtc#": rtc, "#rts#": rts, "#rta#": rta, "#rabc#": rabc, "#ratc#": ratc, "#rp#": myRP, "#rto#": rto, "#rbo#": rbo]
    tableScheme = ["#id#": "qq", "#th#": th, "#tw#": tw, "#tbc#": tbc, "#tbo#": tbo]
    borderScheme = ["#bw#": bw, "#bc#": bc, "#bs#": bs, "#br#": br, "#bp#": bp, "#bo#": bo]
    footerScheme = ["#ft#": ft, "#fs#": fs, "#fc#": fc, "#fa#": fa]

    highlightScheme = ["#hc1#": compress(hc1), "#hts1#": hts1, "#hc2#": compress(hc2), "#hts2#": hts2, "#hc3#": compress(hc3), "#hts3#": hts3, "#hc4#": compress(hc4), "#hts4#": hts4, "#hc5#": compress(hc5), "#hts5#": hts5, "#hc6#": compress(hc6), "#hts6#": hts6, "#hc7#": compress(hc7), "#hts7#": hts7, "#hc8#": compress(hc8), "#hts8#": hts8, "#hc9#": compress(hc9), "#hts9#": hts9, "#hc10#": compress(hc10), "#hts10#": hts10]
    keywordScheme = ["#k1#": k1, "#ktr1#": ktr1, "#k2#": k2, "#ktr2#": ktr2, "#k3#": k3, "#ktr3#": ktr3, "#k4#": k4, "#ktr4#": ktr4, "#k5#": k5, "#ktr5#": ktr5]
    thresholdScheme = ["#top1#": top1, "#tcv1#": tcv1, "#ttr1#": ttr1, "#top2#": top2, "#tcv2#": tcv2, "#ttr2#": ttr2, "#top3#": top3, "#tcv3#": tcv3, "#ttr3#": ttr3, "#top4#": top4, "#tcv4#": tcv4, "#ttr4#": ttr4, "#top5#": top5, "#tcv5#": tcv5, "#ttr5#": ttr5]
    otherScheme = ["#comment#": comment, "#bm#": bm, "#tff#": (tff != null ? tff.toString().replace('\\', '').replace("'", "").replace('"', '') : "Roboto"), "#bfs#": bfs, "#fbc#": compress(fbc), "#iFrameColor#": compress(iFrameColor), "#myKeywordCount#": myKeywordCount, "#myThresholdCount#": myThresholdCount]

    booleanScheme1 = ["#isFrame#": isFrame, "#isComment#": isComment, "#isTitle#": isTitle, "#isHeaders#": isHeaders, "#isBorder#": isBorder, "#isAlternateRows#": isAlternateRows, "#isFooter#": isFooter]
    booleanScheme2 = ["#isOverrides#": isOverrides]

    def myBaseSettingsMap
    def myOverridesMap = [:]

    if (overrides != null && isOverrides == true) {
        if (isLogStyles) log.info("fillStyle: overrides is: ${overrides}")
        tmpOverrides = overrides.toString().replace("| | |", " | ")
        tmpOverrides = tmpOverrides.replace("| |", " | ")
        tmpOverrides = tmpOverrides.replace("||", " | ")
        myOverridesMap = overridesToMap(tmpOverrides, "|", "=")
    }
    state.myOverrides = myOverridesMap

    myBaseSettingsMap = titleScheme.clone() + headerScheme.clone() + rowScheme.clone() + tableScheme.clone() + borderScheme.clone() + footerScheme.clone() + highlightScheme.clone() + keywordScheme.clone() + thresholdScheme.clone() + otherScheme.clone() + booleanScheme1.clone() + booleanScheme2.clone()

    state.myBaseSettingsMap = myBaseSettingsMap.clone()
    myBaseSettingsMapHEX8 = myBaseSettingsMap.clone() + Hex8ColorScheme.clone()

    def myBaseSettingsPlusOverrides = myBaseSettingsMap.clone() + myOverridesMap.clone()
    state.myBaseSettingsPlusOverrides = myBaseSettingsPlusOverrides.clone()

    float effTbo = myBaseSettingsPlusOverrides.get("#tbo#") != null ? myBaseSettingsPlusOverrides.get("#tbo#").toFloat() : 1.0f
    float effTo = myBaseSettingsPlusOverrides.get("#to#") != null ? myBaseSettingsPlusOverrides.get("#to#").toFloat() : 1.0f
    float effBo = myBaseSettingsPlusOverrides.get("#bo#") != null ? myBaseSettingsPlusOverrides.get("#bo#").toFloat() : 1.0f
    float effHbo = myBaseSettingsPlusOverrides.get("#hbo#") != null ? myBaseSettingsPlusOverrides.get("#hbo#").toFloat() : 1.0f
    float effHto = myBaseSettingsPlusOverrides.get("#hto#") != null ? myBaseSettingsPlusOverrides.get("#hto#").toFloat() : 1.0f
    float effRbo = myBaseSettingsPlusOverrides.get("#rbo#") != null ? myBaseSettingsPlusOverrides.get("#rbo#").toFloat() : 1.0f
    float effRto = myBaseSettingsPlusOverrides.get("#rto#") != null ? myBaseSettingsPlusOverrides.get("#rto#").toFloat() : 1.0f

    mytbc = convertToHex8(myBaseSettingsPlusOverrides.get("#tbc#"), effTbo)
    mytc = convertToHex8(myBaseSettingsPlusOverrides.get("#tc#"), effTo)
    mybc = convertToHex8(myBaseSettingsPlusOverrides.get("#bc#"), effBo)

    myhbc = convertToHex8(myBaseSettingsPlusOverrides.get("#hbc#"), effHbo)
    myhtc = convertToHex8(myBaseSettingsPlusOverrides.get("#htc#"), effHto)

    myrbc = convertToHex8(myBaseSettingsPlusOverrides.get("#rbc#"), effRbo)
    myrtc = convertToHex8(myBaseSettingsPlusOverrides.get("#rtc#"), effRto)

    Hex8ColorScheme = ["#tbc#": compress(mytbc), "#tc#": compress(mytc), "#hbc#": compress(myhbc), "#htc#": compress(myhtc), "#rbc#": compress(myrbc), "#rtc#": compress(myrtc), "#bc#": compress(mybc)]

    def myEffectiveSettingsMap = myBaseSettingsPlusOverrides.clone() + Hex8ColorScheme.clone()
    state.myEffectiveSettingsMap = myEffectiveSettingsMap.clone()

    def myStyleMap = myBaseSettingsMap
    def stylesToRemove = ["#Comment#", "#id#", "#tt#", "#R0C0#", "#R0C1#", "#R0C2#", "#R0C3#", "#ft#"]
    stylesToRemove.each { style -> myStyleMap.remove(style) }

    myStyleMap.overrides = settings.overrides

    myStyleMap."#tbc#" = tbc
    myStyleMap."#tc#" = tc
    myStyleMap."#bc#" = bc
    myStyleMap."#rbc#" = rbc
    myStyleMap."#rtc#" = rtc
    myStyleMap."#hbc#" = hbc
    myStyleMap."#htc#" = htc

    state.myActiveStyleMap = myStyleMap

    if (isLogStyles) log.info("fillStyle: myEffectiveSettingsMap is: ${myEffectiveSettingsMap}")
    return myEffectiveSettingsMap
}

void installed() {
    if (isLogTrace) log.trace("<b>installed: Calling initialize() then exiting.</b>")
    initialize()
}

def initialize() {
    if (state.initialized == true) {
        if (isLogTrace) log.trace("<b>initialize: Initialize has already been run. Exiting</b>")
        return
    }

    if (isLogTrace) log.trace("<b>initialize: Initializing has begun.</b>")
    state.initialized = true

    if (layoutMode == null) app.updateSetting("layoutMode", "Device Group")

    app.updateSetting("myVariableCount", [value: "1", type: "enum"])
    app.updateSetting("varColumns", [value: "1", type: "enum"])
    app.updateSetting("showVariables", [value: "Show Variables", type: "enum"])
    app.updateSetting("invalidAttribute", [value: "N/A", type: "enum"])

    app.updateSetting("rows", [value: "1", type: "enum"])
    app.updateSetting("myColumns", [value: "1", type: "enum"])
    app.updateSetting("autoRefreshGrid", [value: "True", type: "enum"])

    app.updateSetting("gatherDeviceDetails", [value: ["deviceLabel", "deviceName", "lastActivity"], type: "enum"])
    app.updateSetting("defaultDateTimeFormat", [value: "4", type: "enum"])
    app.updateSetting("myTruncateLength", [value: "99", type: "enum"])

    app.updateSetting("classID", "qq")
    app.updateSetting("tilePreviewWidth", "2")
    app.updateSetting("tilePreviewHeight", "2")
    app.updateSetting("isComment", false)
    app.updateSetting("comment", "?")
    app.updateSetting("isFrame", false)
    app.updateSetting("fbc", [value: "#bbbbbb", type: "color"])
    app.updateSetting("tbc", [value: "#d9ecb1", type: "color"])
    app.updateSetting("tbo", "1")
    app.updateSetting("isShowSettings", false)
    app.updateSetting("iFrameColor", [value: "#bbbbbb", type: "color"])

    app.updateSetting("isTitle", false)
    app.updateSetting("tt", "My Title")
    app.updateSetting("ts", "125")
    app.updateSetting("tc", [value: "#000000", type: "color"])
    app.updateSetting("ta", "Center")
    app.updateSetting("tp", "3")
    app.updateSetting("to", "1")

    app.updateSetting("tw", "100")
    app.updateSetting("th", "Auto")
    app.updateSetting("bm", "Collapse")

    app.updateSetting("isBorder", true)
    app.updateSetting("bs", "Solid")
    app.updateSetting("bc", [value: "#000000", type: "color"])
    app.updateSetting("bo", "1")
    app.updateSetting("bw", "2")
    app.updateSetting("br", "0")
    app.updateSetting("bp", "0")

    app.updateSetting("isHeaders", true)
    app.updateSetting("isMergeHeaders", "Do Not Merge")
    app.updateSetting("R0C1", "Device")
    app.updateSetting("R0C2", "State")
    app.updateSetting("R0C3", "Other 1")
    app.updateSetting("R0C4", "Other 2")
    app.updateSetting("R0C5", "Other 3")
    app.updateSetting("hbc", [value: "#90C226", type: "color"])
    app.updateSetting("hbo", "1")
    app.updateSetting("htc", [value: "#000000", type: "color"])
    app.updateSetting("hts", "100")
    app.updateSetting("hta", "Center")
    app.updateSetting("hto", "1")
    app.updateSetting("hp", "0")

    app.updateSetting("rtc", [value: "#000000", type: "color"])
    app.updateSetting("rts", "80")
    app.updateSetting("rbc", [value: "#000000", type: "color"])
    app.updateSetting("rbo", "0")
    app.updateSetting("rta", "Center")
    app.updateSetting("rto", "1")
    app.updateSetting("isAlternateRows", false)
    app.updateSetting("rabc", [value: "#dff8aa", type: "color"])
    app.updateSetting("ratc", [value: "#000000", type: "color"])
    app.updateSetting("rp", "0")

    app.updateSetting("isFooter", true)
    app.updateSetting("ft", "%time%")
    app.updateSetting("fs", "60")
    app.updateSetting("fc", [value: "#000000", type: "color"])
    app.updateSetting("fa", "Center")

    app.updateSetting("hc1", [value: "#008000", type: "color"])
    app.updateSetting("hts1", "100")
    app.updateSetting("hc2", [value: "#CA6F1E", type: "color"])
    app.updateSetting("hts2", "100")
    app.updateSetting("hc3", [value: "#00FF00", type: "color"])
    app.updateSetting("hts3", "100")
    app.updateSetting("hc4", [value: "#0000FF", type: "color"])
    app.updateSetting("hts4", "100")
    app.updateSetting("hc5", [value: "#FF0000", type: "color"])
    app.updateSetting("hts5", "100")
    app.updateSetting("hc6", [value: "#008000", type: "color"])
    app.updateSetting("hts6", "100")
    app.updateSetting("hc7", [value: "#CA6F1E", type: "color"])
    app.updateSetting("hts7", "100")
    app.updateSetting("hc8", [value: "#00FF00", type: "color"])
    app.updateSetting("hts8", "100")
    app.updateSetting("hc9", [value: "#0000FF", type: "color"])
    app.updateSetting("hts9", "100")
    app.updateSetting("hc10", [value: "#FF0000", type: "color"])
    app.updateSetting("hts10", "100")

    app.updateSetting("myKeywordCount", 0)
    app.updateSetting("k1", [value: "?", type: "text"])
    app.updateSetting("ktr1", [value: "?", type: "text"])
    app.updateSetting("k2", [value: "?", type: "text"])
    app.updateSetting("ktr2", [value: "?", type: "text"])
    app.updateSetting("k3", [value: "?", type: "text"])
    app.updateSetting("ktr3", [value: "?", type: "text"])
    app.updateSetting("k4", [value: "?", type: "text"])
    app.updateSetting("ktr4", [value: "?", type: "text"])
    app.updateSetting("k5", [value: "?", type: "text"])
    app.updateSetting("ktr5", [value: "?", type: "text"])

    app.updateSetting("myThresholdCount", 0)
    app.updateSetting("top1", [value: "0", type: "enum"])
    app.updateSetting("tcv1", [value: 70, type: "number"])
    app.updateSetting("ttr1", [value: "?", type: "text"])
    app.updateSetting("top2", [value: "0", type: "enum"])
    app.updateSetting("tcv2", [value: 70, type: "number"])
    app.updateSetting("ttr2", [value: "?", type: "text"])
    app.updateSetting("top3", [value: "0", type: "enum"])
    app.updateSetting("tcv3", [value: 70, type: "number"])
    app.updateSetting("ttr3", [value: "?", type: "text"])
    app.updateSetting("top4", [value: "0", type: "enum"])
    app.updateSetting("tcv4", [value: 70, type: "number"])
    app.updateSetting("ttr4", [value: "?", type: "text"])
    app.updateSetting("top5", [value: "0", type: "enum"])
    app.updateSetting("tcv5", [value: 70, type: "number"])
    app.updateSetting("ttr5", [value: "?", type: "text"])

    app.updateSetting("fr1", [value: "?", type: "text"])
    app.updateSetting("fr2", [value: "?", type: "text"])
    app.updateSetting("fr3", [value: "?", type: "text"])

    app.updateSetting("oc1", [value: ",", type: "text"])
    app.updateSetting("nc1", [value: "[hr]", type: "text"])

    app.updateSetting("bfs", "18")
    app.updateSetting("tff", "Roboto")
    app.updateSetting("scrubHTMLlevel", 1)
    app.updateSetting("isShowImportExport", false)
    app.updateSetting("isShowHTML", false)
    app.updateSetting("importStyleText", "?")
    app.updateSetting("importStyleOverridesText", "?")
    app.updateSetting("isOverrides", false)
    app.updateSetting("overrides", [value: "?", type: "textarea"])

    app.updateSetting("mySelectedTile", "")
    app.updateSetting("isCompactDisplay", false)
    app.updateSetting("overrideHelperCategory", [value: "Animation", type: "text"])
    app.updateSetting("overridesHelperSelection", [value: "Fade: Fades in an object on refresh.", type: "text"])

    app.updateSetting("mySelectedTile", "")
    app.updateSetting("publishInterval", [value: "1", type: "enum"])
    app.updateSetting("eventTimeout", "2000")
    app.updateSetting("republishDelay", [value: "0", type: "enum"])
    app.updateSetting("oversizeTileHandling", [value:"File Manager", type:"enum"])
    state.publish = [lastPublished: now(), active: false]

    app.updateSetting('isLogTrace', false)
    app.updateSetting('isLogVariables', false)
    app.updateSetting('isLogCleanups', false)
    app.updateSetting('isLogHighlights', false)
    app.updateSetting('isLogStyles', false)
    app.updateSetting('isLogHTML', false)
    app.updateSetting('isLogPublish', false)
    app.updateSetting('isLogDateTime', false)
    app.updateSetting('isLogDeviceInfo', false)
    app.updateSetting('isLogAppPerformance', false)
    app.updateSetting('isLogDetails', false)

    state.flags = [isClearImport: false, isCopyOverridesHelperCommand: false, isAppendOverridesHelperCommand: false, isClearOverridesHelperCommand: false, styleSaved: false, myCapabilityChanged: false]
    state.myCapabilityHistory = [new: "seed1", old: "seed"]
    if (state.HTMLsizes == null) state.HTMLsizes = [Comment: 0, Head: 0, Body: 0, Interim: 0, Final: 0]

    state.hidden = [LayoutMode: false, DeviceGroup: false, Variables: false, GridLayout: true, Design: false, Publish: false, More: true]
    state.layoutMode = "Device Group"
    state.show = [Keywords: false, Thresholds: false, ReplaceCharacters: false, FormatRules: false]
}

def isMyCapabilityChanged() {
    if (state.myCapabilityHistory == null) state.myCapabilityHistory = [new: "seed1", old: "seed"]
    if (state.flags == null) state.flags = [:]
    if (state.myCapabilityHistory.new != myCapability) {
        state.myCapabilityHistory.old = state.myCapabilityHistory.new
        state.myCapabilityHistory.new = myCapability
        state.flags.myCapabilityChanged = true
    } else state.flags.myCapabilityChanged = false
}

def updateAppVariables() {
    if (isLogTrace) log.trace("<b>updateAppVariables: Entering.</b>")
    def currentVersion = state.variablesVersion?.toString()?.isInteger() ? state.variablesVersion.toString().toInteger() : 0
    if (currentVersion < codeVersion()) {
        log.info("Updating Variables to Version ${codeVersion()}")
        state.variablesVersion = codeVersion().toString()
    }
}

def removeRetiredVariables(myVersion) {
    if (isLogTrace) log.trace("<b>removeRetiredVariables: Entering.</b>")
    if (myVersion == 130) {
    }
}

private static boolean isEven(int number) { return number % 2 == 0 }

static String bold2(s, var) {
    if (var == null) return "<b>$s (N/A)</b>"
    else return ("<b>$s ($var)</b>")
}

static String bold(s) { return "<b>$s</b>" }

static String italic(s) { return "<i>$s</i>" }

static String italicBold(s) { return "<i><b>$s</b></i>" }

static String underline(s) { return "<u>$s</u>" }

static String dodgerBlue(s) { return '<font color = "DodgerBlue">' + s + '</font>' }

static String myTitle(s1, s2) { return '<h3><b><font color = "DodgerBlue">' + s1 + '</font></h3>' + s2 + '</b>' }

static String red(s) { return '<r style="color:red">' + s + '</r>' }

static String green(s) { return '<g style="color:green">' + s + '</g>' }

static def titleise(title) {
    return "<span style='color:#1962d7;text-align:left; margin-top:0em; font-size:20px; padding:2px'><b>${title}</b></span>"
}

static def sectionTitle(title) {
    return "<span style='color:#000000; margin-top:1em; font-size:16px; box-shadow: 0px 0px 3px 3px #40b9f2; padding:1px; background:#40b9f2;'><b>${title}</b></span>"
}

static def largeText(myText) {
    return "<span style='font-size:20px; box-shadow: 0px 0px 3px 3px #40b9f2; padding:1px; background:#40b9f2;'><b>${myText}</b></span>"
}

static String note(myTitle, myText) {
    return "<span style='color:#17202A;text-align:left; margin-top:0.25em; margin-bottom:0.25em ; font-size:16px'>" + "<b>" + myTitle + "</b>" + myText + "</span>"
}

static String body(myBody) {
    return "<span style='color:#17202A;text-align:left; margin-top:0em; margin-bottom:0em ; font-size:18px'>" + myBody + "</span>&nbsp"
}

static String line(myHeight) {
    return "<div style='background:#005A9C; height: " + myHeight.toString() + "px; margin-top:0em; margin-bottom:0em ; border: 0;'></div>"
}

static String summary(myTitle, myText) {
    myTitle = dodgerBlue(myTitle)
    return "<details><summary>" + myTitle + "</summary>" + myText + "</details>"
}

boolean containsSpecialCharacters(String input) {
    if (isLogTrace && isLogDetails) log.trace("<b>containsSpecialCharacters: Entering with $input</b>")
    if (input == null) return false
    try {
        def specialCharacters = ['\\', '(', ')', '{', '}', '*', '+', '?', '|', '^', '$']
        return specialCharacters.any { input.contains(it) }
    } catch (Exception e) {
        log.warn("containsSpecialCharacters: An exception occurred: ${e.message}")
        return true
    }
}

static def beginsWith(data, startString) { data ? data.startsWith(startString) : false }

def overridesToMap(myString, recordSeparator, fieldSeparator) {
    if (isLogTrace && isLogDetails || isLogStyles) log.trace("<b>overridesToMap: Entering with $myString  $recordSeparator</b>")
    myoverrides = [:]
    if (myString == null || myString.toString().size() < 7) return myoverrides

    String s = myString.toString() + " "
    try {
        myArr = s.tokenize(recordSeparator)
        myArr.each {
            int equalsLoc = it.indexOf("=")
            if (equalsLoc != -1) {
                String d0 = it.substring(0, equalsLoc).trim()
                String d1 = it.substring(equalsLoc + 1, it.size()).trim()
                if (isLogStyles) log.info("overridesToMap: it is: ${it}   equalsLoc:${equalsLoc}  myString is: ${s}  d0 is:${d0} and d1 is:${d1}")
                myoverrides."${d0.toLowerCase()}" = d1
            }
        }
    }
    catch (Exception e) {
        log.error("Exception ${e} in overridesToMap. Probably a malformed overrides string.")
    }
    if (isLogStyles) log.info("overrides: Returning map: ${overrides}")
    return myoverrides
}

def scrubHTML(myHTML, iFrame) {
    if (isLogTrace && isLogDetails) log.trace("<b>scrubHTML: Entering (parameters not shown)</b>")
    if (myHTML == null) return ""

    int scrubLevel = (scrubHTMLlevel?.toString()?.isInteger() ? scrubHTMLlevel.toInteger() : 1)

    if (scrubLevel >= 0) {
        def placeholdersToRemove = ["#HIGHLIGHTSTARTSTYLE#", "#HIGHLIGHTENDSTYLE#", "#head#", "#title#", "#table#", "#header#", "#row#", "#alternaterow#", "#data#", "#footer#", "#class#", "#class1#", "#class2#", "#class3#", "#class4#", "#class5#", "#border#", "#frame#", "#high1#", "#high2#", "#high3#", "#high4#", "#high5#", "#high6#", "#high7#", "#high8#", "#high9#", "#high10#"]
        placeholdersToRemove.each { placeholder -> myHTML = myHTML.replace(placeholder, "") }
    }

    myHTML = myHTML.replace("<rt+>", "")
    myHTML = myHTML.replace("<rt->", "")

    if (scrubLevel >= 1) {
        def replacements = ["</style><style>": "", "</style> <style>": "", "<style> ": "<style>", "auto%": "auto", "Auto%": "auto"]
        replacements.each { original, replacement -> myHTML = myHTML.replaceAll(original, replacement) }
        def tagsToRemove = ["font-family:'Roboto'", "font-size:100%", "width:auto", "height:auto", "border-radius:0px", "padding:0px", "border-collapse:Seperate", "background:#00000000", "color:#00000000"]
        tagsToRemove.each { property -> myHTML = myHTML.replaceAll("(?i)" + property, "") }
        if (iFrame == true) myHTML = myHTML.replace("#iFrame1#", "")
    }

    if (scrubLevel >= 2) {
        def tagsToRemove = ["</td>", "</tr>", "</ftqq>", "</tbody>"]
        tagsToRemove.each { tag -> myHTML = myHTML.replace(tag, "") }
    }

    if (scrubLevel == 3) {
        if (isFooter == false) myHTML = myHTML.replace("</table>", "")
        myHTML = myHTML.replace("</body>", "")
        def tagsToRemove = ["</hqq1>", "</hqq2>", "</hqq3>", "</hqq4>", "</hqq5>", "</hqq6>", "</hqq7>", "</hqq8>", "</hqq9>", "</hqq10>"]
        tagsToRemove.each { tag -> myHTML = myHTML.replace(tag, "") }

        myHTML = myHTML.replace(": ", ":")
        myHTML = myHTML.replace("  ", " ")
        myHTML = myHTML.replace("%%", "%")
        myHTML = myHTML.replace(", ", ",")
    }

    if (scrubLevel >= 1) {
        def replacements = [" :": ":", " {": "{", "} ": "}", "{;": "{", ";;;": ";", ";;": ";", ";}": "}", ",,": ",", "    ": "  ", "   ": "  ", "> ": ">"]
        replacements.each { original, replacement -> myHTML = myHTML.replace(original, replacement) }
    }
    return myHTML
}

static def countBetween(String searchString, String match1, String match2) {
    if (searchString == null) return 0
    def item1 = searchString.indexOf(match1)
    def item2 = searchString.indexOf(match2, item1)
    return (item1 != -1 && item2 != -1) ? (item2 - item1 + match2.size()) : 0
}

def unHTML(HTML) {
    if (HTML == null) return ""
    myHTML = HTML.toString().replace("<", "[")
    myHTML = myHTML.replace(">", "]")
    return myHTML
}

def toHTML(HTML) {
    if (HTML == null) return ""
    myHTML = HTML.toString().replace("[", "<")
    myHTML = myHTML.replace("]", ">")
    return myHTML
}

def clearHTML(HTML) {
    if (HTML == null) return ""
    myHTML = HTML.toString().replace("[", "")
    myHTML = myHTML.replace("]", "")
    myHTML = myHTML.replace("<", "")
    myHTML = myHTML.replace(">", "")
    return myHTML
}

def isValidDate(String dateString) {
    if (dateString == null) return false
    def dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    def sdf = new SimpleDateFormat(dateFormat)
    sdf.lenient = false
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"))

    try {
        sdf.parse(dateString)
        return true
    } catch (Exception ignored) {
        return false
    }
}

def isValidInstant(String value) {
    if (isLogDateTime) log.info("<b>isValidInstant: Received is: $value</b>")
    def resultMap = [:]
    resultMap.valid = false
    if (value == null) return resultMap

    try {
        if (value.toLong() < 1000000000000L || value.toLong() > (now() + 63072000000L)) return resultMap
    }
    catch (Exception ignored) {
    }

    def dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    try {
        if (value.contains('E')) {
            def bigDecimalValue = new BigDecimal(value)
            def longValue = bigDecimalValue.longValueExact()
            resultMap.valid = true
            def date = new Date(longValue)
            resultMap.date = dateFormat.format(date)
        } else {
            Long.parseLong(value)
            resultMap.valid = true
            date = new Date(Long.parseLong(value))
            resultMap.date = dateFormat.format(date)
        }
    } catch (Exception ignored) {
    }
    return resultMap
}

def getDataType(myOriginalVal) {
    if (myOriginalVal == null) return "null"
    String myOldType = getObjectClassName(myOriginalVal)
    if (isLogVariables) log.info("getDataType: Received value: $myOriginalVal with type is: $myOldType")

    def myVal = myOriginalVal.toString()

    if (myVal == "null" || myVal == "Null") {
        if (isLogVariables) log.info("getDataType returning 'null'")
        return "null"
    }

    if (myVal.toLowerCase() == "true" || myVal.toLowerCase() == "false") {
        if (isLogVariables) log.info("getDataType returning 'Boolean'")
        return "Boolean"
    }

    def resultMap = isValidInstant(myVal)

    if (resultMap.valid == true) {
        if (isLogVariables) log.info("getDataType returning 'Instant'")
        return "Instant"
    }

    if (isValidDate(myVal)) {
        if (isLogVariables) log.info("getDataType returning 'Date'")
        return "Date"
    }

    if (myVal ==~ /^-?[0-9]+(\.[0-9]+)$/) {
        if (isLogVariables) log.info("getDataType returning 'Float'")
        return "Float"
    }

    if (myVal ==~ /^-?[0-9]+$/) {
        if (isLogVariables) log.info("getDataType returning 'Integer'")
        return "Integer"
    }

    if (isLogVariables) log.info("getDataType returning 'String'")
    return "String"
}

def extractValueBetweenStrings(String input, String startString, String endString) {
    if (isLogTrace && isLogDetails) log.trace("<b>extractValueBetweenStrings: Entering (parameters not shown)</b>")
    if (input == null || startString == null || endString == null) return null
    def startIndex = input.indexOf(startString)
    if (startIndex == -1) return null
    def endIndex = input.indexOf(endString, startIndex + startString.length())
    if (endIndex != -1) {
        return input.substring(startIndex + startString.length(), endIndex)
    } else {
        return null
    }
}

def updateVariables() {
    if (republishDelay == null) {
        app.updateSetting("republishDelay", [value: "0", type: "enum"])
    }
    def currentVersion = state.variablesVersion?.toString()?.isInteger() ? state.variablesVersion.toString().toInteger() : 0
    if (currentVersion == 0) {
        log.info("Initializing variablesVersion to: 108")
        state.variablesVersion = "108"
        currentVersion = 108
    }
    if (currentVersion < 200) {
        log.info("Updating Variables to Version 2.0.0")
        app.updateSetting("republishDelay", [value: "0", type: "enum"])
        if (state.publish == null) state.publish = [:]
        state.publish.lastPublished = 0
        app.updateSetting('isLogStyles', false)
        app.updateSetting('isLogDateTime', false)
        app.updateSetting('isLogDeviceInfo', false)
        app.updateSetting('isLogAppPerformance', false)
        app.updateSetting('isLogDetails', false)
        app.updateSetting("gatherDeviceDetails", [value: ["deviceLabel", "deviceName", "lastActivity"], type: "enum"])
        app.updateSetting("defaultDateTimeFormat", [value: "3", type: "enum"])
        app.updateSetting("invalidAttribute", [value: "N/A", type: "enum"])
        state.variablesVersion = "200"
        currentVersion = 200
    }
    if (currentVersion < codeVersion()) {
        state.variablesVersion = codeVersion().toString()
    }
}

def convertToHex8(String hexColor, float opacity) {
    if (isLogTrace && isLogDetails) log.trace("<b>convertToHex8: Entering with $hexColor  $opacity</b>")
    if (hexColor == null || hexColor == "null") hexColor = "#000000"
    hexColor = hexColor.replace("#", "")
    if (hexColor.length() < 6) hexColor = hexColor.padRight(6, '0')
    opacity = Math.max(0.0f, Math.min(1.0f, opacity))
    def red = Integer.parseInt(hexColor.substring(0, 2), 16)
    def green = Integer.parseInt(hexColor.substring(2, 4), 16)
    def blue = Integer.parseInt(hexColor.substring(4, 6), 16)
    def alpha = Math.round(opacity * 255).toInteger()
    def Hex8 = String.format("#%02X%02X%02X%02X", red, green, blue, alpha)
    return Hex8
}

def compress(String hexValue) {
    if (isLogTrace && isLogDetails) log.trace("<b>compress: Entering with $hexValue.</b>")
    if (hexValue == null || hexValue == "null") return null

    int scrubLevel = (scrubHTMLlevel?.toString()?.isInteger() ? scrubHTMLlevel.toInteger() : 1)
    if (scrubLevel == 0) return hexValue

    if (scrubLevel >= 2) {
        return compressHexColor(hexValue)
    }

    def isCompressible = false
    String opacity = ""
    String cleanHex = hexValue.replace("#", "")
    if (cleanHex.length() == 3) return "#" + cleanHex

    if (cleanHex.length() >= 6) {
        def red = cleanHex.substring(0, 2)
        def green = cleanHex.substring(2, 4)
        def blue = cleanHex.substring(4, 6)
        if (red[0] == red[1] && green[0] == green[1] && blue[0] == blue[1]) isCompressible = true

        if (cleanHex.length() == 6) {
            if (isCompressible == true) return "#" + red[0] + green[0] + blue[0]
            else return "#" + cleanHex
        }
        if (cleanHex.length() == 8) {
            opacity = cleanHex.substring(6, 8)
            if (opacity == "FF" && isCompressible == true) {
                return "#" + red[0] + green[0] + blue[0]
            }
            if (opacity == "FF" && isCompressible == false) {
                return "#" + red + green + blue
            }
        }
    }
    return "#" + cleanHex
}

String compressHexColor(String hexColor) {
    if (isLogTrace && isLogDetails) log.trace("<b>compressHexColor: Entering with $hexColor</b>")
    if (hexColor == null || hexColor == "null") return "#000"
    String newColor = hexColor.trim()
    if (newColor.matches("^#[0-9a-fA-F]{6}\$")) {
        newColor = "#" + (1..5).step(2).collect { newColor[it] }.join()
    } else if (newColor.matches("^#[0-9a-fA-F]{8}\$")) {
        newColor = "#" + (1..7).step(2).collect { newColor[it] }.join()
    }
    if (newColor.length() > 4 && newColor.endsWith("F")) {
        newColor = newColor[0..-2]
    }
    return newColor
}

String buttonLink(String btnName, String linkText, int buttonNumber) {
    if (isLogTrace && isLogDetails) log.trace("<b>buttonLink: Entering with $btnName  $linkText  $buttonNumber</b>")
    def myColor, myText
    Integer myFont = 16

    int activeBtn = (settings.activeButton?.toString()?.isInteger() ? settings.activeButton.toInteger() : 1)
    if (buttonNumber == activeBtn) myColor = "#00FF00" else myColor = "#000000"
    if (buttonNumber == activeBtn) myText = "<b><u>${linkText}</u></b>" else myText = "<b>${linkText}</b>"

    return "<div class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='color:${myColor};cursor:pointer;font-size:${myFont}px'>${myText}</div></div><input type='hidden' name='settings[$btnName]' value=''>"
}

def emptyFunction(message) {
    log.info("Child: $message")
}