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
* Idle Node Refreshers (Parent application)
*
* Version 1.0.0    Initial release
*/

/**
 *  Change History:
 *
 *      ORIGINAL SOURCE
 *      2020          cococafe     0.1.0.0         Copyright (c) 2020, Denny Page    https://raw.githubusercontent.com/dennypage/hubitat/master/applications/idle-node-refresher/idle-node-refreshers.groovy
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
        name: "Battery Info Connector",
        namespace: "jshimota",
        author: "James Shimota",
        description: "Parent - Battery info connector to Google Sheets",
        category: "Convenience",
        singleInstance: true,
        filename: "battery_infos.groovy",
        importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/battery_info/battery_infos.groovy",
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
    dynamicPage(name: "", title: "Battery Info Connectors To Google Sheets", install: true, uninstall: true, refreshInterval: 0)
            {
                if (app.getInstallationState() == 'COMPLETE')
                {
                    section
                            {
                                app(name: "childApps", appName: "Battery Info Connector Child", namespace: "jshimota", title: "Create an Battery Info list", multiple: true)
                            }
                }
                else
                {
                    section("")
                            {
                                paragraph "<b>Click Done to complete the installation.</b>"
                            }
                }
            }
}

def installed()
{
    log.info "There are now ${childApps.size()} Battery Info Child lists"
    childApps.each
            {
                child -> log.info "  Battery Info: ${child.label}"
            }
}

def updated()
{
    installed()
}
