/**
 *  InfluxDB v2 Connection Test App
 *
 *  Copyright 2026 J. Shimota
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

definition(
    name: "InfluxDB v2 Connection Tester",
    namespace: "jshimota",
    author: "J. Shimota",
    description: "Validates connection settings and permissions for InfluxDB v2 endpoints.",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    singleThreaded: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "InfluxDB v2 Connection Tester", install: true, uninstall: true) {
        
        section("<b>InfluxDB Server Details</b>") {
            input name: "prefServerUrl", type: "text", title: "Server Host / IP & Port", description: "e.g., http://192.168.1.12:8086", defaultValue: "http://192.168.1.12:8086", required: true
            input name: "prefOrg", type: "text", title: "Organization", description: "e.g., DundeeHome", required: true
            input name: "prefBucket", type: "text", title: "Bucket Name", description: "e.g., Hubitat", required: true
            input name: "prefToken", type: "password", title: "API Authentication Token", description: "InfluxDB v2 Token", required: true
        }
        
        section("<b>Connection Test</b>") {
            input name: "btnTestConnection", type: "button", title: "Test InfluxDB Connection", submitOnChange: true
            
            if (state.testResult != null) {
                paragraph "${state.testResult}"
            }
        }
        
        section("<b>Logging Options</b>") {
            input name: "prefLogEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true
        }
    }
}

def installed() {
    logDebug "Installed with settings: ${settings}"
}

def updated() {
    logDebug "Updated with settings: ${settings}"
}

def appButtonHandler(btn) {
    switch(btn) {
        case "btnTestConnection":
            testInfluxDbConnection()
            break
        default:
            log.warn "Unhandled button press: ${btn}"
            break
    }
}

def testInfluxDbConnection() {
    def baseUrl = settings?.prefServerUrl ? settings.prefServerUrl.replaceAll("/+\$", "") : ""
    def orgVal = settings?.prefOrg ?: ""
    def bucketVal = settings?.prefBucket ?: ""
    def tokenVal = settings?.prefToken ?: ""
    
    def testPath = "/api/v2/write"
    
    // Construct the complete request URL string
    def fullTargetUrl = "${baseUrl}${testPath}?org=${orgVal}&bucket=${bucketVal}&precision=ns"
    
    // Log complete string PRIOR to test execution
    log.info "[InfluxDB Connection Tester] Request URL: ${fullTargetUrl}"
    
    def testMetric = "hubitat_connection_test,hub=${location.name.replaceAll(' ', '\\\\ ')} status=\"online\" ${now()}000000"
    
    def postParams = [
        uri: baseUrl,
        path: testPath,
        query: [
            org: orgVal,
            bucket: bucketVal,
            precision: "ns"
        ],
        headers: [
            "Authorization": "Token ${tokenVal}",
            "Content-Type": "text/plain; charset=utf-8"
        ],
        body: testMetric,
        timeout: 10
    ]
    
    try {
        httpPost(postParams) { response ->
            if (response.status == 204 || response.status == 200) {
                state.testResult = "<font color='green'><b>SUCCESS (HTTP ${response.status}):</b> Successfully connected and wrote test payload to bucket '${bucketVal}'.</font>"
                log.info "[InfluxDB Connection Tester] Result: Connection Successful (HTTP ${response.status})"
            } else {
                state.testResult = "<font color='orange'><b>WARNING (HTTP ${response.status}):</b> Connected, but server responded with status ${response.status}.</font>"
                log.info "[InfluxDB Connection Tester] Result: Unexpected HTTP status ${response.status}"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        def statusCode = e.response?.status
        def errorMessage = e.message
        
        state.testResult = "<font color='red'><b>FAILED (HTTP ${statusCode}):</b> ${errorMessage}. Check your token permissions or Org/Bucket names.</font>"
        log.info "[InfluxDB Connection Tester] Result: Failed - HTTP ${statusCode} (${errorMessage})"
    } catch (Exception e) {
        state.testResult = "<font color='red'><b>ERROR:</b> ${e.message}. Verify that the server IP/Port is reachable from Hubitat.</font>"
        log.info "[InfluxDB Connection Tester] Result: Error - ${e.message}"
    }
}

private void logDebug(msg) {
    if (settings?.prefLogEnable != false) {
        log.debug "[InfluxDB Connection Tester] ${msg}"
    }
}