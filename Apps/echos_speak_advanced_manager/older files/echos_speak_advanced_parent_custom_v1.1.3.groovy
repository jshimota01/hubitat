/**
 * Echos Speak Advanced (Custom)
 * Smart Application for Hubitat Elevation
 *
 * Purpose:
 * Central parent application managing connection to local Echo API bridge,
 * discovering Amazon Echo hardware devices, and maintaining child devices.
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
  * v1.1.3    09/09/26    jshimota    Merged expanded device map, filtered out blocked/ignored devices, and added category visibility switches for discovery list.
  * v1.1.2    09/09/26    jshimota    Integrated legacy deviceStyleMap lookup table to translate Amazon device type IDs into friendly style names and mapped icon filenames.
  * v1.1.1    09/09/26    jshimota    Added custom icon and image source URL host location setting to bridgeConfigPage().
  * v1.1.0    09/09/26    jshimota    Appended application version directly into definition name / app instance label display.
  * v1.0.9    09/09/26    jshimota    Added [INSTALLED] / [NEW] discovery device labels and user-configurable device label prefix options.
  * v1.0.8    09/09/26    jshimota    Added Amazon re-authentication proxy link helper section to bridgeConfigPage().
  * v1.0.7    09/09/26    jshimota    Standardized trace log version update banner demarcation on app state updates.
  * v1.0.6    09/09/26    jshimota    Enhanced HTTP error tracing and response payload logging for HTTP 500 diagnostic visibility.
  * v1.0.5    09/09/26    jshimota    Expanded handleDiscoveryResponse() payload dispatch to forward extended attributes (firmwareVer, followUpMode, alexaWakeWord, alarmVolume, permissions, lastVoiceActivity) to child drivers.
  * v1.0.4    09/08/26    jshimota    Standardized naming to EchosSpeak and normalized DNI to raw Amazon Serial Numbers to eliminate bridge routing 404 errors.
  * v1.0.3    09/08/26    jshimota    Replaced legacy actionName UI element with native Hubitat submit button to resolve MissingMethodException.
  * v1.0.2    09/08/26    jshimota    Added getDiscoveredDeviceMap() helper method and updated handleDiscoveryResponse() to support selective child device creation.
  * v1.0.1    09/08/26    jshimota    Fix of iconX URLS for now.
  * v1.0.0    09/08/26    jshimota    Initial release of EchosSpeak Advanced (Custom) parent application.
 **/
// [KEEP-EXACT] See possible changelog.txt for past changelog history.

static String version() { return '1.1.3' }
def timeStamp() { return "2026/09/09 10:50 AM" }

import groovy.transform.Field

@Field static final Map<String, Map<String, Object>> DEVICE_STYLE_MAP = [
    // Multiroom Audio (#1)
    "A3C9PE6TNYLTCH" : [name: "Multiroom Audio Group", icon: "echo_wha.png", family: "WHA", cat: "multiroom"],

    // Fire TV Devices (#2)
    "A12GXV8XMS007S" : [name: "Fire TV (Gen1)", icon: "firetv_gen1.png", family: "FIRE_TV", cat: "firetv"],
    "A2E0SNTXJVT7WK" : [name: "Fire TV (Gen2)", icon: "firetv_gen2.png", family: "FIRE_TV", cat: "firetv"],
    "A2GFL5ZMWNE0PX" : [name: "Fire TV (Gen3)", icon: "firetv_gen3.png", family: "FIRE_TV", cat: "firetv"],
    "AKPGW064GI9HE"  : [name: "Fire TV Stick 4K (Gen3)", icon: "firetv_stick_gen1.png", family: "FIRE_TV", cat: "firetv"],
    "AN630UQPG2CA4"  : [name: "Fire TV (Toshiba)", icon: "firetv_toshiba.png", family: "FIRE_TV", cat: "firetv"],
    "A2JKHJ0PX4J3L3" : [name: "Fire TV Cube (Gen2)", icon: "firetv_cube.png", family: "FIRE_TV", cat: "firetv"],
    "A265XOI9586NML" : [name: "Fire TV Stick", icon: "firetv_stick_gen1.png", family: "FIRE_TV", cat: "firetv"],
    "A2LWARUGJLBYEW" : [name: "Fire TV Stick (Gen2)", icon: "firetv_stick_gen1.png", family: "FIRE_TV", cat: "firetv"],
    "A1F8D55J0FWDTN" : [name: "Fire TV (Toshiba)", icon: "toshiba_firetv.png", family: "FIRE_TV", cat: "firetv"],
    "AP4RS91ZQ0OOI"  : [name: "Fire TV (Toshiba)", icon: "toshiba_firetv.png", family: "FIRE_TV", cat: "firetv"],
    "AFF5OAL5E3DIU"  : [name: "Fire TV", icon: "toshiba_firetv.png", family: "FIRE_TV", cat: "firetv"],
    "A3HF4YRA2L7XGC" : [name: "Fire TV Cube", icon: "firetv_cube.png", family: "FIRE_TV", cat: "firetv"],
    "A1VGB7MHSIEYFK" : [name: "Fire TV Cube Gen3", icon: "firetv_cube.png", family: "FIRE_TV", cat: "firetv"],
    "AFF50AL5E3DIU"  : [name: "Fire TV (Insignia)", icon: "insignia_firetv.png", family: "FIRE_TV", cat: "firetv"],
    "ADVBD696BHNV5"  : [name: "Fire TV Stick (Gen1)", icon: "firetv_stick_gen1.png", family: "FIRE_TV", cat: "firetv"],
    "A1P7E7V3FCZKU6" : [name: "Fire TV (Gen3)", icon: "firetv_gen3.png", family: "FIRE_TV", cat: "firetv"],
    "A3IKB0DFR7GKZW" : [name: "Fire TV (Gen3)", icon: "firetv_gen3.png", family: "FIRE_TV", cat: "firetv"],
    "A1WZKXFLI43K86" : [name: "Fire TV Stick MAX", icon: "firetv_stick_gen1.png", family: "FIRE_TV", cat: "firetv"],
    "A31DTMEEVDDOIV" : [name: "Fire TV Stick Lite", icon: "firetv_stick_gen1.png", family: "FIRE_TV", cat: "firetv"],

    // Amazon Tablets (#3)
    "A1Q7QCGNMXAKYW" : [name: "Generic Tablet", icon: "amazon_tablet.png", family: "TABLET", cat: "tablets"],
    "ATNLRCEBX3W4P"  : [name: "Generic Tablet", icon: "amazon_tablet.png", family: "TABLET", cat: "tablets"],
    "A1J16TEDOYCZTN" : [name: "Fire Tablet", icon: "amazon_tablet.png", family: "TABLET", cat: "tablets"],
    "A2M4YX06LWP8WI" : [name: "Fire Tablet", icon: "amazon_tablet.png", family: "TABLET", cat: "tablets"],
    "A1C66CX2XD756O" : [name: "Fire Tablet HD", icon: "amazon_tablet.png", family: "TABLET", cat: "tablets"],
    "A2N49KXGVA18AR" : [name: "Fire Tablet HD 10 Plus", icon: "amazon_tablet.png", family: "TABLET", cat: "tablets"],
    "A3L0T0VL9A921N" : [name: "Fire Tablet HD 8", icon: "tablet_hd10.png", family: "TABLET", cat: "tablets"],
    "AVU7CPPF2ZRAS"  : [name: "Fire Tablet HD 8", icon: "tablet_hd10.png", family: "TABLET", cat: "tablets"],
    "A38EHHIB10L47V" : [name: "Fire Tablet HD 8", icon: "tablet_hd10.png", family: "TABLET", cat: "tablets"],
    "A3R9S4ZZECZ6YL" : [name: "Fire Tablet HD 10", icon: "tablet_hd10.png", family: "TABLET", cat: "tablets"],
    "A2V9UEGZ82H4KZ" : [name: "Fire Tablet HD 10", icon: "tablet_hd10.png", family: "TABLET", cat: "tablets"],

    // Amazon Echo Standard, Dots & Shows (#4)
    "AB72C64C86AW2"  : [name: "Echo (Gen1)", icon: "echo_gen1.png", family: "ECHO", cat: "echoes"],
    "A7WXQPH584YP"   : [name: "Echo (Gen2)", icon: "echo_gen2.png", family: "ECHO", cat: "echoes"],
    "A3FX4UWTP28V1P" : [name: "Echo (Gen3)", icon: "echo_gen3.png", family: "ECHO", cat: "echoes"],
    "A30YDR2MK8HMRV" : [name: "Echo (Gen3)", icon: "echo_gen3.png", family: "ECHO", cat: "echoes"],
    "A2M35JJZWCQOMZ" : [name: "Echo Plus (Gen1)", icon: "echo_plus_gen1.png", family: "ECHO", cat: "echoes"],
    "A18O6U1UQFJ0XK" : [name: "Echo Plus (Gen2)", icon: "echo_plus_gen2.png", family: "ECHO", cat: "echoes"],
    "ASQZWP4GPYUT7"  : [name: "Echo Pop", icon: "echo_plus_gen2.png", family: "ECHO", cat: "echoes"],
    "AKNO1N0KSFN8L"  : [name: "Echo Dot (Gen1)", icon: "echo_dot_gen1.png", family: "ECHO", cat: "echoes"],
    "A3S5BH2HU6VAYF" : [name: "Echo Dot (Gen2)", icon: "echo_dot_gen2.png", family: "ECHO", cat: "echoes"],
    "A32DDESGESSHZA" : [name: "Echo Dot (Gen3)", icon: "echo_dot_gen3.png", family: "ECHO", cat: "echoes"],
    "A32DOYMUN6DTXA" : [name: "Echo Dot (Gen3)", icon: "echo_dot_gen3.png", family: "ECHO", cat: "echoes"],
    "A1RABVCI4QCIKC" : [name: "Echo Dot (Gen3)", icon: "echo_dot_gen3.png", family: "ECHO", cat: "echoes"],
    "A3RMGO6LYLH7YN" : [name: "Echo Dot (Gen4)", icon: "echo_dot_gen4.png", family: "ECHO", cat: "echoes"],
    "A4ZXE0RM7LQ7A"  : [name: "Echo Dot (Gen5)", icon: "echo_dot_gen5.png", family: "ECHO", cat: "echoes"],
    "A2DS1Q2TPDJ48U" : [name: "Echo Dot Clock (Gen5)", icon: "echo_dot_clock_gen5.png", family: "ECHO", cat: "echoes"],
    "A2H4LV5GIZ1JFT" : [name: "Echo Dot Clock (Gen4)", icon: "echo_dot_clock_gen4.png", family: "ECHO", cat: "echoes"],
    "A2U21SRK4QGSE1" : [name: "Echo Dot Clock (Gen4)", icon: "echo_dot_clock_gen4.png", family: "ECHO", cat: "echoes"],
    "A10A33FOX2NUBK" : [name: "Echo Spot", icon: "echo_spot_gen1.png", family: "KNIGHT", cat: "echoes"],
    "A3EH2E0YZ30OD6" : [name: "Echo Spot (Gen2)", icon: "echo_spot_gen1.png", family: "KNIGHT", cat: "echoes"],
    "A1NL4BVLQ4L3N3" : [name: "Echo Show (Gen1)", icon: "echo_show_gen1.png", family: "KNIGHT", cat: "echoes"],
    "AWZZ5CVHX2CD"   : [name: "Echo Show (Gen2)", icon: "echo_show_gen2.png", family: "KNIGHT", cat: "echoes"],
    "A4ZP7ZC4PI6TO"  : [name: "Echo Show 5 (Gen1)", icon: "echo_show_5.png", family: "KNIGHT", cat: "echoes"],
    "A1XWJRHALS1REP" : [name: "Echo Show 5 (Gen2)", icon: "echo_show_5.png", family: "KNIGHT", cat: "echoes"],
    "A1Z88NGR2BK6A2" : [name: "Echo Show 8 (Gen1)", icon: "echo_show_8.png", family: "KNIGHT", cat: "echoes"],
    "A15996VY63BQ2D" : [name: "Echo Show 8 (Gen2)", icon: "echo_show_8.png", family: "KNIGHT", cat: "echoes"],
    "AIPK7MM90V7TB"  : [name: "Echo Show 10 (Gen3)", icon: "echo_show_10_gen3.png", family: "KNIGHT", cat: "echoes"],
    "A2LLN0UXRW4N50" : [name: "Echo Show 11 (Gen4)", icon: "echo_show_11_gen4.png", family: "KNIGHT", cat: "echoes"],
    "AQ24620N8QD5Q"  : [name: "Echo Show 15 (Gen2)", icon: "echo_show_15.png", family: "KNIGHT", cat: "echoes"],
    "A1EIANJ7PNB0Q7" : [name: "Echo Show 15 (Gen1)", icon: "echo_show_15.png", family: "KNIGHT", cat: "echoes"],
    "AZANTNEUTYY6L"  : [name: "Echo Show 21 (Gen1)", icon: "echo_show_21.png", family: "KNIGHT", cat: "echoes"],
    "A11QM4H9HGV71H" : [name: "Echo Show 5 (Gen3)", icon: "echo_show_5.png", family: "KNIGHT", cat: "echoes"],
    "A2UONLFQW0PADH" : [name: "Echo Show 8 (Gen3)", icon: "echo_show_5.png", family: "KNIGHT", cat: "echoes"],
    "A345JHMQDGG1M5" : [name: "Echo Show 8 (Gen4)", icon: "echo_show_8.png", family: "KNIGHT", cat: "echoes"],
    "ADMKNMEVNL158"  : [name: "Echo Hub", icon: "unknown.png", family: "KNIGHT", cat: "echoes"],

    // Audio & Accessories (#5)
    "A303PJF6ISQ7IC" : [name: "Echo Auto", icon: "echo_auto.png", family: "ALEXA_AUTO", cat: "audio_acc"],
    "A195TXHV1M5D4A" : [name: "Echo Auto", icon: "echo_auto.png", family: "ALEXA_AUTO", cat: "audio_acc"],
    "ALT9P69K6LORD"  : [name: "Echo Auto", icon: "echo_auto.png", family: "ALEXA_AUTO", cat: "audio_acc"],
    "A13W6HQIHKEN3Z" : [name: "Echo Auto", icon: "echo_auto.png", family: "ALEXA_AUTO", cat: "audio_acc"],
    "A38949IHXHRQ5P" : [name: "Echo Tap", icon: "echo_tap.png", family: "ECHO", cat: "audio_acc"],
    "A1JJ0KFC4ZPNJ3" : [name: "Echo Input", icon: "echo_input.png", family: "ECHO", cat: "audio_acc"],
    "A3IYPH06PH1HRA" : [name: "Echo Frames", icon: "echo_frames.png", family: "ECHO", cat: "audio_acc"],
    "A3RBAYBE7VM004" : [name: "Echo Studio", icon: "echo_studio.png", family: "ECHO", cat: "audio_acc"],
    "A2RU4B77X9R9NZ" : [name: "Echo Link Amp", icon: "echo_link_amp.png", family: "ECHO", cat: "audio_acc"],
    "A3VRME03NAXFUB" : [name: "Echo Flex", icon: "echo_flex.png", family: "ECHO", cat: "audio_acc"],
    "A3SSG6GR8UU7SN" : [name: "Echo Sub", icon: "echo_sub_gen1.png", family: "ECHO", cat: "audio_acc"],
    "A27VEYGQBW3YR5" : [name: "Echo Link", icon: "echo_link.png", family: "ECHO", cat: "audio_acc"],
    "A15QWUTQ6FSMYX" : [name: "Echo Buds (Gen2)", icon: "echo_buds_gen2.png", family: "ECHO", cat: "audio_acc"],

    // Third-Party Speakers & TV Integrations (#8)
    "A3NPD82ABCPIDP" : [name: "Sonos Beam", icon: "sonos_beam.png", family: "OTHER", cat: "third_party"],
    "AVD3HM0HOJAAL"  : [name: "Sonos", icon: "sonos_generic.png", family: "OTHER", cat: "third_party"],
    "AHJYKVA63YCAQ"  : [name: "Sonos", icon: "sonos_generic.png", family: "OTHER", cat: "third_party"],
    "A2EZ3TS0L1S2KV" : [name: "Sonos Beam", icon: "sonos_generic.png", family: "OTHER", cat: "third_party"],
    "A18BI6KPKDOEI4" : [name: "Ecobee4", icon: "ecobee4.png", family: "OTHER", cat: "third_party"],
    "A2X8WT9JELC577" : [name: "Ecobee5", icon: "ecobee4.png", family: "OTHER", cat: "third_party"],
    "AA1IN44SS3X6O"  : [name: "Ecobee Thermostat Premium", icon: "unknown.png", family: "OTHER", cat: "third_party"],
    "A3B5K1G3EITBIF" : [name: "Facebook Portal", icon: "facebook_portal.png", family: "OTHER", cat: "third_party"],
    "A3D4YURNTARP5K" : [name: "Facebook Portal TV", icon: "facebook_portal.png", family: "OTHER", cat: "third_party"],
    "A2HZENIFNYTXZD" : [name: "Facebook Portal", icon: "facebook_portal.png", family: "OTHER", cat: "third_party"],
    "A52ARKF0HM2T4"  : [name: "Facebook Portal+", icon: "facebook_portal.png", family: "OTHER", cat: "third_party"]
]

// Silent Hard Filter - Ignored & Blocked Non-Speaker Device Types (#6 & #7)
@Field static final List<String> IGNORED_DEVICE_TYPES = [
    "A112LJ20W14H95", "A1GC6GEE1XF1G9", "A1MPSLFC7L5AFK", "A1ORT4KZ23OY88", "A1VS6XVTGTLC00", 
    "A1VZJGJYCRI78V", "A1ZB65LA390I4K", "A21X6I4DKINIZU", "A21Z3CGI8UIP0F", "A2825NDLA7WDZV", 
    "A29L394LN0I8HN", "A2IVLV5VM2W81", "A2T0P32DY3F7VB", "A2TF17PFR55MTB", "A2TOXM6L8SFS8A", 
    "A2V3E2XUH5Z7M8", "A1FWRGKHME4LXH", "A26TRKU1GG059T", "AU4IFDJDRSBC1", "A2ZOTUOF1IBEYI", 
    "ABP0V5EHO8A4U", "AD2YUJTRVBNOF", "ADQRVG6LYK4LQ", "A1GPVMRI4IOS0M", "A2Z8O30CD35N8F", 
    "A1XN1MKELB7WUF", "AINRG27IL8AS0", "A3NVKTZUPX1J3X", "A3NWHXTQ4EBCZS", "A2RG3FY1YV97SS", 
    "A3H674413M2EKB", "AGZWSPR7FLP9E", "AILBSA2LNTOYL", "A2RJLFEH0UEKI9", "AKOAGQTKAS9YB", 
    "ATH4K2BAIXVHQ", "A1WAR447VT003J", "A1GA2W150VBSDI", "A2S24G29BFP88", "A1NAFO69AAQ16Bk", 
    "A1NAFO69AAQ16B", "A3L2K717GERE73", "A222D4HGE48EOR", "A19JK51Y4N50K5", "A2T1VHZ1WSVSEY", 
    "AF746J4KLOSL0", "A3M91KUSTM6A3P", "AUZCREI6S9QS5", "A1DL2DVDQVK3Q", "A1H0CMF1XM0ZP4", 
    "A1X7HJX9QL16M5", "A3SSWQ04XYPXBH", "AKKLQD9FZWWQS", "AP1F6KUH00XPV", "A2WFDCBDEXOXR8", 
    "AVN2TMX8MU2YM", "A3F1S88NTZZXS9", "A25EC4GIHFOCSG"
]

definition(
    name: "Echos Speak Advanced (Custom) (v${version()})",
    namespace: "jshimota",
    author: "James Shimota",
    description: "Parent app for EchosSpeak Advanced. Controls local proxy connectivity, device discovery, and state sync.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/echo_speaks_3.1x.png",
    iconX2Url: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/echo_speaks_3.2x.png",
    iconX3Url: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/echo_speaks_3.3x.png",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/echos-speak-advanced/echos-speak-advanced_custom.groovy"
)

preferences {
    page(name: "mainPage")
    page(name: "bridgeConfigPage")
    page(name: "deviceDiscoveryPage")
}

private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.appVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.appVersion = currentVer
    }
}

private void updateAppLabelVersion() {
    String desiredLabel = "Echos Speak Advanced (Custom) (v${version()})"
    if (app.label != desiredLabel) {
        logInfo "Updating application instance label to: '${desiredLabel}'"
        app.updateLabel(desiredLabel)
    }
}

/* =========================================================================================
   UI PAGES & CONFIGURATION LAYOUT
   ========================================================================================= */

def mainPage() {
    checkAndLogVersionDemarcation()
    updateAppLabelVersion()

    dynamicPage(name: "mainPage", title: "Echos Speak Advanced (Custom) v${version()}", install: true, uninstall: true) {
        section("<b>Local API Bridge Connection</b>") {
            String statusText = state.bridgeConnected ? "<font color='green'><b>Connected</b></font>" : "<font color='red'><b>Disconnected</b></font>"
            paragraph "Bridge Status: ${statusText}"
            href name: "toBridgeConfig", page: "bridgeConfigPage", title: "Configure API Bridge Settings", description: "Set Bridge IP address, port, and icon asset URL"
        }

        section("<b>Device Management</b>") {
            href name: "toDeviceDiscovery", page: "deviceDiscoveryPage", title: "Discover & Sync Echo Devices", description: "Scan Amazon account via Bridge and update child devices"
        }

        section("<b>Application Logging Options</b>") {
            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: true, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: true, required: true
        }
    }
}

def bridgeConfigPage() {
    checkAndLogVersionDemarcation()
    dynamicPage(name: "bridgeConfigPage", title: "Bridge & Asset Configuration", nextPage: "mainPage") {
        section("<b>Local Node.js / Docker Server</b>") {
            input name: "bridgeIp", type: "string", title: "Bridge IP Address", required: true, defaultValue: "192.168.1.12"
            input name: "bridgePort", type: "string", title: "Bridge REST Port", required: true, defaultValue: "8093"
            input name: "proxyPort", type: "string", title: "Bridge Amazon Auth Proxy Port", required: true, defaultValue: "8094"
        }

        section("<b>Custom Icon & Image Asset Host</b>") {
            paragraph "Base URL location used for device icons and media tiles when creating or updating devices:"
            input name: "imageSourceUrl", type: "string", title: "Custom Icon Source Base URL", required: false, defaultValue: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/"
        }

        section("<b>Amazon Account Authentication</b>") {
            paragraph "If your bridge reports <i>'Cookie invalid, Renew unsuccessful'</i> or HTTP 500 errors during discovery, click the button below to re-authenticate with Amazon:"
            
            String proxyUrl = "http://${settings.bridgeIp ?: '192.168.1.12'}:${settings.proxyPort ?: '8094'}"
            paragraph "<a href='${proxyUrl}' target='_blank' style='background-color:#007185;color:white;padding:8px 12px;text-decoration:none;border-radius:4px;display:inline-block;font-weight:bold;'>🔐 Open Amazon Proxy Login (${proxyUrl})</a>"
        }
    }
}

def deviceDiscoveryPage() {
    checkAndLogVersionDemarcation()
    dynamicPage(name: "deviceDiscoveryPage", title: "Device Discovery", nextPage: "mainPage") {
        section("<b>Device Filtering Preferences</b>") {
            paragraph "Select which categories of devices to display in the discovery import list below:"
            input name: "showMultiroom", type: "bool", title: "1. Include Multiroom Audio Groups?", defaultValue: true, submitOnChange: true
            input name: "showFireTV", type: "bool", title: "2. Include Fire TV Devices & Cubes?", defaultValue: true, submitOnChange: true
            input name: "showTablets", type: "bool", title: "3. Include Amazon Fire Tablets?", defaultValue: false, submitOnChange: true
            input name: "showEchoes", type: "bool", title: "4. Include Amazon Echoes, Dots & Shows?", defaultValue: true, submitOnChange: true
            input name: "showAudioAccessories", type: "bool", title: "5. Include Echo Audio & Accessories (Studio, Flex, Link, Auto)?", defaultValue: true, submitOnChange: true
            input name: "showThirdParty", type: "bool", title: "8. Include Third-Party Speakers (Sonos, Ecobee, Portal)?", defaultValue: true, submitOnChange: true
        }

        section("<b>Device Label Naming Options</b>") {
            input name: "enableDevicePrefix", type: "bool", title: "Add Prefix to Device Labels?", defaultValue: true, submitOnChange: true
            if (settings.enableDevicePrefix != false) {
                input name: "devicePrefixText", type: "string", title: "Custom Prefix Text", defaultValue: "Echo - ", required: false
            }
        }

        section("<b>Select Echo Devices</b>") {
            paragraph "Devices marked as <b>[INSTALLED]</b> already exist on your hub. Unchecked or new devices show as <b>[NEW]</b>."
            input name: "selectedDevices", type: "enum", title: "Devices to Import", options: getDiscoveredDeviceMap(), multiple: true, required: false, submitOnChange: true
            input name: "btnSaveDiscovery", type: "button", title: "Save & Create Selected Child Devices"
        }
    }
}

void appButtonHandler(String btn) {
    checkAndLogVersionDemarcation()
    if (btn == "btnSaveDiscovery") {
        executeDeviceDiscovery()
    }
}

Map getDiscoveredDeviceMap() {
    checkAndLogVersionDemarcation()
    Map devMap = [:]
    if (!settings.bridgeIp || !settings.bridgePort) return devMap

    String targetUrl = "${getBridgeUrl()}/api/devices"
    logTrace "getDiscoveredDeviceMap(): Fetching devices synchronous from ${targetUrl}"

    Set<String> installedDnis = getChildDevices()*.deviceNetworkId as Set

    try {
        httpGet([uri: targetUrl, timeout: 10]) { resp ->
            logTrace "getDiscoveredDeviceMap(): HTTP Status=${resp.status}"
            if (resp.data && resp.data.devices) {
                resp.data.devices.each { dev ->
                    // 1. Check if device is hard-filtered in IGNORED_DEVICE_TYPES (#6 & #7)
                    if (IGNORED_DEVICE_TYPES.contains(dev.deviceStyle)) return

                    // 2. Lookup style metadata from DEVICE_STYLE_MAP
                    Map styleMeta = DEVICE_STYLE_MAP[dev.deviceStyle]
                    String category = styleMeta ? styleMeta.cat : "echoes"

                    // 3. Evaluate category visibility switches (1-8)
                    Boolean allowCategory = true
                    switch (category) {
                        case "multiroom":  allowCategory = (settings.showMultiroom != false); break
                        case "firetv":     allowCategory = (settings.showFireTV != false); break
                        case "tablets":    allowCategory = (settings.showTablets != false); break
                        case "echoes":     allowCategory = (settings.showEchoes != false); break
                        case "audio_acc":  allowCategory = (settings.showAudioAccessories != false); break
                        case "third_party": allowCategory = (settings.showThirdParty != false); break
                    }

                    if (allowCategory) {
                        Boolean isInstalled = installedDnis.contains(dev.serialNumber)
                        String statusTag = isInstalled ? "[INSTALLED]" : "[NEW]"
                        String friendlyModel = styleMeta ? styleMeta.name : (dev.deviceFamily ?: 'Echo Device')

                        devMap[dev.serialNumber] = "${statusTag} ${dev.accountName} (${friendlyModel})"
                    }
                }
            } else {
                logDebug "getDiscoveredDeviceMap(): Bridge returned response without devices array: ${resp.data}"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        String errorResponseBody = e.response?.data ? e.response.data.toString() : "No body text"
        logError "Failed to fetch device map from bridge [HTTP ${e.statusCode}]: ${e.message} | Response Body: ${errorResponseBody}"
    } catch (Exception e) {
        logError "Failed to fetch device map from bridge (General Exception): ${e.class.name} - ${e.message}"
    }
    return devMap
}

/* =========================================================================================
   HUBITAT APP LIFECYCLE ROUTINES
   ========================================================================================= */

void installed() {
    checkAndLogVersionDemarcation()
    updateAppLabelVersion()
    logInfo "Installing Echos Speak Advanced (Custom) v${version()}..."
    initialize()
}

void updated() {
    checkAndLogVersionDemarcation()
    updateAppLabelVersion()
    logInfo "Updating preferences for Echos Speak Advanced (Custom)..."
    initialize()
}

private void initialize() {
    checkAndLogVersionDemarcation()
    updateAppLabelVersion()
    unschedule()
    
    // Poll bridge every 5 minutes for state/IP updates
    schedule("0 */5 * ? * * *", "syncDeviceStates")
    
    // Initial connection test
    testBridgeConnection()
}

/* =========================================================================================
   BRIDGE COMMUNICATIONS & COMMAND DISPATCH
   ========================================================================================= */

private String getBridgeUrl() {
    return "http://${settings.bridgeIp}:${settings.bridgePort}"
}

void testBridgeConnection() {
    checkAndLogVersionDemarcation()
    if (!settings.bridgeIp || !settings.bridgePort) return
    
    String targetUrl = "${getBridgeUrl()}/status"
    logTrace "testBridgeConnection(): Testing endpoint ${targetUrl}"

    asynchttpGet("handleConnectionResponse", [
        uri: targetUrl,
        timeout: 10
    ])
}

void handleConnectionResponse(response, data) {
    checkAndLogVersionDemarcation()
    if (response.hasError()) {
        logError "Bridge Connection Failed [HTTP ${response.getStatus()}]: ${response.getErrorMessage()} | Body: ${response.getErrorData()}"
        state.bridgeConnected = false
    } else {
        logInfo "Local API Bridge connection verified."
        state.bridgeConnected = true
    }
}

void executeDeviceDiscovery() {
    checkAndLogVersionDemarcation()
    String targetUrl = "${getBridgeUrl()}/api/devices"
    logInfo "Executing Echo device discovery request to ${targetUrl}..."
    
    asynchttpGet("handleDiscoveryResponse", [
        uri: targetUrl,
        timeout: 15
    ])
}

void handleDiscoveryResponse(response, data) {
    checkAndLogVersionDemarcation()
    logTrace "handleDiscoveryResponse(): Received HTTP ${response.getStatus()}"

    if (response.hasError()) {
        String rawErrorBody = response.getErrorData() ?: "No error data body"
        logError "Device discovery failed [HTTP ${response.getStatus()}]: ${response.getErrorMessage()} | Detailed Bridge Error: ${rawErrorBody}"
        return
    }

    Map json = response.getJson()
    logTrace "handleDiscoveryResponse(): Parsed JSON: ${json}"

    if (!json || !json.devices) {
        logWarn "No devices returned from bridge. Response body: ${response.getData()}"
        return
    }

    List<String> chosenSerials = settings.selectedDevices ? (settings.selectedDevices instanceof List ? settings.selectedDevices : [settings.selectedDevices]) : []

    // Calculate dynamic label prefix based on user preferences
    Boolean usePrefix = settings.enableDevicePrefix != false
    String prefix = usePrefix ? (settings.devicePrefixText != null ? settings.devicePrefixText : "Echo - ") : ""
    
    // Determine configured image source base path
    String baseImgUrl = settings.imageSourceUrl ?: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Icons-Images/echo_speaks/resources/icons/"
    if (!baseImgUrl.endsWith("/")) baseImgUrl += "/"

    json.devices.each { dev ->
        if (chosenSerials.contains(dev.serialNumber)) {
            String dni = dev.serialNumber
            String computedLabel = "${prefix}${dev.accountName}".trim()
            def child = getChildDevice(dni)
            
            // Resolve model style name and icon file from DEVICE_STYLE_MAP
            Map styleMeta = DEVICE_STYLE_MAP[dev.deviceStyle]
            String resolvedStyle = styleMeta ? styleMeta.name : (dev.deviceStyle ?: "Echo Device")
            String iconFile = styleMeta ? styleMeta.icon : "echo_gen1.png"
            String finalIconUrl = "${baseImgUrl}${iconFile}"

            if (!child) {
                logInfo "Creating child device: ${computedLabel} (${dni})"
                child = addChildDevice(
                    "jshimota", 
                    "Echos Speak Advanced Device (Custom)", 
                    dni, 
                    [name: dev.accountName, label: computedLabel]
                )
            } else {
                if (child.label != computedLabel) {
                    logInfo "Updating label for device ${dni} to '${computedLabel}'"
                    child.setLabel(computedLabel)
                }
            }

            // Push ALL extended attributes to child driver
            child.parseDeviceData([
                ipAddress: dev.ipAddress,
                macAddress: dev.macAddress,
                deviceFamily: dev.deviceFamily ?: (styleMeta ? styleMeta.family : "ECHO"),
                deviceStyle: resolvedStyle,
                deviceImageUrl: finalIconUrl,
                online: dev.online,
                firmwareVer: dev.firmwareVer,
                followUpMode: dev.followUpMode,
                alexaWakeWord: dev.alexaWakeWord,
                alarmVolume: dev.alarmVolume,
                permissions: dev.permissions,
                lastVoiceActivity: dev.lastVoiceActivity
            ])
        }
    }
}

void syncDeviceStates() {
    checkAndLogVersionDemarcation()
    if (!state.bridgeConnected) testBridgeConnection()
    executeDeviceDiscovery()
}

void sendBridgeCommand(String deviceDni, String commandType, Map commandPayload = [:]) {
    checkAndLogVersionDemarcation()
    String serialNumber = deviceDni.trim()
    
    Map bodyData = [
        serialNumber: serialNumber,
        command: commandType,
        payload: commandPayload
    ]

    logDebug "Sending Bridge Command (${commandType}) for ${serialNumber}: ${commandPayload}"

    asynchttpPost("handleCommandResponse", [
        uri: "${getBridgeUrl()}/api/command",
        requestContentType: "application/json",
        contentType: "application/json",
        body: groovy.json.JsonOutput.toJson(bodyData),
        timeout: 10
    ], [dni: deviceDni, command: commandType])
}

void handleCommandResponse(response, data) {
    checkAndLogVersionDemarcation()
    if (response.hasError()) {
        logError "Command failed for ${data.dni} (${data.command}) [HTTP ${response.getStatus()}]: ${response.getErrorMessage()} | Body: ${response.getErrorData()}"
    } else {
        logDebug "Command ${data.command} completed successfully for ${data.dni}"
    }
}

void refreshDeviceData(String deviceDni) {
    checkAndLogVersionDemarcation()
    syncDeviceStates()
}

/* =========================================================================================
   LOGGING ENGINE
   ========================================================================================= */

private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (settings[settingKey] != null ? settings[settingKey] as Boolean : defaultEnabled) {
        log."${lowerLevel}" "Echos Speak Advanced App: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }