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
            name: "Battery Info",
            namespace: "jshimota",
            author: "James Shimota",
            description: "Child - Battery Info connector to Google Sheets",
            category: "Convenience",
            parent: "jshimota:Battery Infos",
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
    dynamicPage(name: "", title: "Battery Info List", install: true, uninstall: true, refreshInterval: 0)
            {
                section("")
                        {
                            input "name", "text", title: "<b>Battery Info List of Monitored devices</b>", multiple: false, required: true
                        }
                section("")
                        {
                            paragraph "<b>Battery powered Nodes to be monitored</b>: The nodes to be monitored and sent/monitored in Google Sheets each day. " +
                                    "Selected nodes all have batteries."
                            input "nodes", "capability.battery", title: "Battery powered Nodes to be monitored", multiple: true, required: true
                        }
                section("")
                        {
                            paragraph "<b>Inactivity Idle Hours</b>: The number of hours of inactivity before a node " +
                                    "is considered idle. A reasonable value for this is 24 (1 day) or greater. The " +
                                    "minimum value is 1."
                            input "idleHours", "number", title: "Inactivity Idle Hours", required: true, defaultValue: 25
                        }
                section("")
                        {
                            paragraph "<b>Refresh Interval Minutes</b>: The minimum number of minutes between idle " +
                                    "node refreshes. Only one node will be refreshed per interval. A reasonable " +
                                    "value for this is 5 or greater. The minimum value is 2."
                            input "refreshMinutes", "number", title: "Refresh Interval Minutes", required: true, defaultValue: 10
                        }
                section("")
                        {
                            paragraph "<b>Refresh Switch</b>: If this option is enabled and the device being " +
                                    "refreshed has Switch capability, one minute after calling refresh() on the device " +
                                    "we will read the current value of the switch and call on() or off() as appropriate " +
                                    "to refresh the current state. The advantage of refreshing switch state is that it " +
                                    "will trigger Z-Wave Plus devices to update their route if appropriate. The " +
                                    "disadvantage of switch refresh is additional network traffic. Additionally, if the " +
                                    "state in the driver is stale due to prior communication issues, it could result in " +
                                    "the switch mysteriously turning on or off."
                            input "refreshSwitch", "bool", title: "Refresh switch state (on/off)", defaultValue: false
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
    if (idleHours < 1)
    {
        app.updateSetting("idleHours", 1)
        log.warn "Input value for Inactivity Idle Hours too low: value changed to 1 hour"
    }
    if (refreshMinutes < 2)
    {
        app.updateSetting("refreshMinutes", 2)
        log.warn "Input value for Refresh Interval Minutes too low: value changed to 2 minutes"
    }

    unschedule()
    installed()
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

def switchRefresh(args)
{
    node = args.node
    try
    {
        String state = nodes[node].currentState("switch", true).value
        log.info "Node ${nodes[node].getDisplayName()}: Refreshing switch state (${state})..."

        if (state && state == "on")
        {
            nodes[node].on()
        }
        else
        {
            nodes[node].off()
        }
    }
    catch (Exception e)
    {
        log.warn "Node ${nodes[node].getDisplayName()}: ${e}"
    }
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

                // Refresh switch state if requested
                if (refreshSwitch && nodes[node].hasCapability("Switch")) runIn(60, switchRefresh, [data: [node: node]])
                break
            }
        }
    }

    // Resort to integrate any activity cache changes
    updateSortedIndex()

    // Schedule our next refresh
    millis = state.lastCache[state.sortedIndex[0]] + idleMillis - now
    if (millis < refreshMillis) millis = refreshMillis
    runInMillis(millis, refreshNode)
}
