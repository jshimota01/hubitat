/**
 * Copyright (c) 2020, Denny Page
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Idle Node Refresher (Child application)
 *
 * Version 1.0.0    Initial release
 * Version 1.0.1    Bug fix - incorrect use of idle interval instead of
 *                  refresh interval for min sleep.
 */

/**
 *  Battery Info
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

/**
 *  Change History:
 *
 *      ORIGINAL SOURCE
 *      2020          cococafe     0.1.0.1         Copyright (c) 2020, Denny Page     https://raw.githubusercontent.com/dennypage/hubitat/master/applications/idle-node-refresher/idle-node-refresher.groovy
 *
 *      Date          Source        Version          What                                              URL
 *      ----          ------        -------          ----                                              ---
 *      2021-10-07    jshimota      0.1.0.2          My 'fork' of this driver taken from the above original source
 *
 */

static String version() { return '0.1.0.2' }

// Definition Name below was modified so as not to step on existing app - this may cause problems with developer repository as a PR may fail with file not found -
// jshimota - 10-15-2021
definition(
        name: "Battery Info Connector Child",
        namespace: "jshimota",
        author: "James Shimota",
        description: "Child - Battery Info Connector To Google Sheets",
        category: "Convenience",
        parent: "jshimota:Battery Info Connector",
        filename: "battery_info.groovy",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/battery_info/battery_info.groovy",
        iconUrl: "",
        iconX2Url: "",
        iconX3Url: ""
)

preferences
        {
            page(name: "configPage")
        }

def configPage()
{
    dynamicPage(name: "", title: "Battery Info Connector List to Google Sheets", install: true, uninstall: true, refreshInterval: 0)
            {
                section("")
                        {
                            input "name", "text", title: "<b>Battery Info List of Monitored devices</b>", multiple: false, required: true
                        }
                section("")
                        {
                            paragraph "<b>Battery powered devices to be monitored</b>: Select the devices to be monitored from the device list below." +
                                    "  It is strongly suggested NOT to select any devices that are virtual as they don't actually have batteries!" +
                                    "  This app can not yet determine if a device is virtual or not for you.  </br>Devices selected will be sent and monitored in Google Sheets each day." +
                                    "</br></br><center><b>(Selected nodes below all report as having batteries)</b></center>"
                            input "nodes", "capability.battery", title: "Battery powered devices to be monitored:", multiple: true, required: true
                        }
                section("")
                        {
                            paragraph "<b>Update Schedule</b>: Normally this application runs every morning.  However this can be adjusted " +
                                    "by entering the number of days between updates.  It is likely unnecessary to update more often than" +
                                    "once per day, which is the default."

                            input "updateDays", "number", title: "Number of days between updates", required: true, defaultValue: 1
                        }
            }
}

def installed()
{
    app.updateLabel(name)
    updateLastCache()
    updateSortedIndex()
    runIn(1, refreshNode)
}

def updated()
{
    createDataChildDevice()
    unschedule()
    installed()
}

def createDataChildDevice() {
    dataName = "TestBI"
    if(logEnable) log.debug "In createDataChildDevice (${state.version})"
    statusMessageD = ""
    if(!getChildDevice(dataName)) {
        if(logEnable) log.debug "In createDataChildDevice - Child device not found - Creating device: ${dataName}"
        try {
//            addChildDevice("jshimota", "Battery Info Device", dataName, 1234, ["name": "${dataName}", isComponent: false])
            addChildDevice("jshimota", "Battery Info Device", TestBI, 1234, ["TestBI": "TestBI", isComponent: false])
            if(logEnable) log.debug "In createDataChildDevice - Child device has been created! (TestBI)"
            statusMessageD = "<b>Device has been been created. (TestBI)</b>"
        } catch (e) { if(logEnable) log.debug "Battery Info unable to create device - ${e}" }
    } else {
        statusMessageD = "<b>Device Name (TestBI) already exists.</b>"
    }
    return statusMessageD
}
private Long lastNodeActivity(node)
{
    // 2020-11-17 01:56:54+0000
    return Date.parse("yyyy-MM-dd HH:mm:ssZ", "${nodes[node].getLastActivity()}").getTime()
}

def updateLastCache()
{
    state.lastCache = []
    (0 .. nodes.size() - 1).each
            {
                node -> state.lastCache[node] = lastNodeActivity(node)
            }
}

def updateSortedIndex()
{
    def indexList = []
    (0 .. nodes.size() - 1).each
            {
                node -> indexList[node] = node
            }
    state.sortedIndex = indexList.sort({a, b -> state.lastCache[a] <=> state.lastCache[b]})
}

def refreshNode()
{
    Long idleMillis = idleHours * 3600000
    Long refreshMillis = refreshMinutes * 60000
    Long now = now()

    for (int i = 0; i < state.sortedIndex.size(); i++)
    {
        node = state.sortedIndex[i]

        Long elapsed = now - state.lastCache[node]
        if (elapsed >= idleMillis)
        {
            // Update our cached value
            state.lastCache[node] = lastNodeActivity(node)
            elapsed = now - state.lastCache[node]

            if (elapsed >= idleMillis)
            {
                // Put the node at the end of the line
                // NB: Even if refresh() does not update lastActivity, we won't refresh
                //     the node again until idleHours has expired
                state.lastCache[node] = now

                try
                {
                    Integer hours = elapsed / 1000 / 60 / 60
                    log.info "Node ${nodes[node].getDisplayName()}: last activity was ${hours} hours ago. Refreshing..."
                    nodes[node].refresh()
                }
                catch (Exception e)
                {
                    log.warn "Node ${nodes[node].getDisplayName()}: ${e}"
                }
                break
            }
        }
    }
}

def logsOff() {
    log.info "${app.label} - Debug logging auto disabled"
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}