/**
 * Alexa Manager Advanced - Device Management Child Application
 * Version: 0.1.1 | 2026/11/09
 * Initial branch placeholder. Device approval and Amazon exposure will be implemented here.
 * This release intentionally performs no registration or deregistration with Amazon.
 */
static String version() { '0.1.1' }

definition(name: 'AMA Device Management Child', namespace: 'jshimota', author: 'James Shimota',
    description: 'Reviews discovered devices and manages explicit Amazon exposure approval.',
    category: 'Integrations', parent: 'jshimota:Alexa Manager Advanced', iconUrl: '', iconX2Url: '', iconX3Url: '', importUrl: '')

preferences { page(name: 'mainPage') }

def mainPage() {
    dynamicPage(name: 'mainPage', title: '', install: true, uninstall: true) {
        section() {
            paragraph "<div style='background-color:#1A252F;color:#FFFFFF;padding:12px;border-radius:6px;text-align:center;'><h2 style='color:#FFFFFF;margin:0;font-size:20px;'>AMA Device Management Child</h2><span style='font-size:12px;'>Version ${version()}</span></div>"
            paragraph 'This branch will provide the device review table, Hubitat Name / Hubitat Label / Amazon Name fields, capability selection, naming intelligence, voice test library, and explicit approval controls.'
            paragraph '<b>Current release:</b> No device is automatically approved or published to Amazon.'
        }
    }
}
void installed() { log.info 'AMA Device Management Child v0.1.1 installed.' }
