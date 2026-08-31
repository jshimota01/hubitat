/**
 * Device Information Viewer (Custom) App
 * Platform: Hubitat Elevation
 * Notes: Custom application to view and export device states, data, and metadata as HTML or CSV
 **/
/**
 * Copyright 2026 James Shimota / Original 2026 Claude & John Land
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
 *  Purpose:
 *  View and export device current states, device data, and device details as an HTML table or downloadable CSV.
 *  
 *  Changelog:
 *  v1.8.0    08/30/26    jshimota    Renamed application to Device Information Viewer (Custom) and updated brand headers
 *  v1.7.1    08/30/26    jshimota    Applied App Master Template v1.2.0, suppressed constant debug log output, fixed JS table sort hook & CSV double-quoting
 *  v1.7.0    08/30/26    jshimota    Applied initial App Master Template v1.1.0
 *  v1.6.0    07/22/26    John Land   Fixed sort order issues on label columns
 *  v1.5.0    07/20/26    John Land   Defaulted to capability.* for all devices
 **/

static String version() { return '1.8.0' }
def timeStamp() { return "2026/08/30 10:37 AM" }

definition(
    name: "Device Information Viewer (Custom)",
    namespace: "jshimota",
    author: "James Shimota / Original by Claude & John Land",
    description: "View and export device current states, device data, and device details as HTML table or CSV",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/jshimota01/hubitat/main/Apps/device_information_viewer/Device_Information_Viewer.groovy",
    oauth: true 
)

preferences {
    page(name: "mainPage")
    page(name: "viewData")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        String currentVersion = version()

        /* Styled App Header Banner */
        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Device Information Viewer (Custom)</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        section("<b>Device Selection</b>") {
            input "includeAllDevices", "bool", title: "Show ALL devices including Groups and Virtual in pick list below.", defaultValue: true, submitOnChange: true
            
            def showAll = (settings.includeAllDevices != null) ? settings.includeAllDevices : true
            def capType = showAll ? "capability.*" : "capability.refresh"
            
            input "selectedDevices", capType, title: "Choose Devices (Leave empty to process ALL matching devices)", multiple: true, required: false
        }
        
        section("<b>Data to Include</b>") {
            input "includeCurrentStates", "bool", title: "Include Current States (attributes)", defaultValue: true
            input "includeDeviceData", "bool", title: "Include Device Data", defaultValue: true
            input "includeDeviceDetails", "bool", title: "Include Device Details (metadata)", defaultValue: true
        }
        
        section("<b>Default Sort Options</b>") {
            input "defaultSortColumn", "text", title: "Default Sort Column Name (leave blank for Device Name)", description: "Enter exact column name (e.g., 'roomName', 'switch', 'manufacturer')", required: false
            input "defaultSortDirection", "enum", title: "Default Sort Direction", options: ["asc": "Ascending ▲", "desc": "Descending ▼"], defaultValue: "asc"
        }
        
        section("<b>Export Options</b>") {
            input "includeTimestamp", "bool", title: "Include timestamp in filename", defaultValue: true
            input "csvDelimiter", "enum", title: "CSV Delimiter", options: ["comma": "Comma (,)", "semicolon": "Semicolon (;)", "tab": "Tab"], defaultValue: "comma"
        }
        
        section("<b>View & Export</b>") {
            href "viewData", title: "View Device Data", description: "Click to view all device data in table format"
            paragraph "<i>Tip: Click any column header in the rendered table to sort dynamically by that column.</i>"
        }
        
        section("<b>CSV Download Endpoints</b>") {
            paragraph "Access remote or local CSV export endpoints below:"
            if (!state.accessToken) {
                initialize()
            }
            if (state.accessToken) {
                def localUrl = "${fullLocalApiServerUrl}/csv?access_token=${state.accessToken}"
                def cloudUrl = "${fullApiServerUrl}/csv?access_token=${state.accessToken}"
                
                paragraph "<b>Local Endpoint:</b> <a href='${localUrl}' target='_blank'>${localUrl}</a>"
                if (getSettingBool("enableCloud", false)) {
                    paragraph "<b>Cloud Endpoint:</b> <a href='${cloudUrl}' target='_blank'>${cloudUrl}</a>"
                }
            }
            input "enableCloud", "bool", title: "Enable Cloud Endpoint Access", defaultValue: false, submitOnChange: true
        }

        /* Collapsible App Preferences & Logging Options */
        section("<b>App Preferences & Logging Options</b>", hideable: true, hidden: true) {
            input name: "showVersionInLabel", type: "bool", title: "Show Version in App Label?", defaultValue: true

            paragraph "<hr style='border:0; border-top:1px solid #E0E0E0; margin:8px 0;'/>"

            input name: "logInfoEnable", type: "bool", title: "Logging - Enable Info Logging", defaultValue: true, required: true
            input name: "logErrorEnable", type: "bool", title: "Logging - Enable Error Logging", defaultValue: true, required: true
            input name: "logWarnEnable", type: "bool", title: "Logging - Enable Warning Logging", defaultValue: true, required: true
            input name: "logDebugEnable", type: "bool", title: "Logging - Enable Debug Logging", defaultValue: false, required: true
            input name: "logTraceEnable", type: "bool", title: "Logging - Enable Trace Logging", defaultValue: false, required: true
        }

        section("<b>Note</b>") {
            paragraph "<i>Note: 'Create Time' and 'Last Update Time' shown in Hubitat's device details page are not accessible through the public app API and cannot be exported.</i>"
        }
    }
}

def viewData() {
    dynamicPage(name: "viewData", title: "", install: false, uninstall: false) {
        String currentVersion = version()

        section() {
            paragraph "<div style='background-color:#1A252F; color:#FFFFFF; padding:12px; border-radius:6px; text-align:center; margin-bottom:10px;'>" +
                      "<h2 style='color:#FFFFFF; margin:0; font-size:20px; font-weight:600;'>Device Information Data Viewer (Custom)</h2>" +
                      "<span style='font-size:12px; opacity:0.8;'>Version ${currentVersion} (${timeStamp()})</span></div>"
        }

        section("<b>Generated Device Table</b>") {
            paragraph generateHtmlTable()
        }
        
        section("<b>Export Actions</b>") {
            paragraph """
                <button onclick="copyTableToClipboard()" style="padding: 6px 12px; background: #2196F3; color: white; border: none; cursor: pointer; border-radius: 4px;">Copy Table as CSV</button>
                <button onclick="downloadSortedCsv()" style="padding: 6px 12px; background: #4CAF50; color: white; border: none; cursor: pointer; border-radius: 4px; margin-left: 10px;">Download CSV (current sort)</button>
                <script>
                function copyTableToClipboard() {
                    const csv = tableToCSV();
                    navigator.clipboard.writeText(csv).then(function() {
                        alert('CSV copied to clipboard!');
                    }, function() {
                        alert('Failed to copy CSV');
                    });
                }
                
                function downloadSortedCsv() {
                    const csv = tableToCSV();
                    const blob = new Blob([csv], { type: 'text/csv' });
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'device_data_' + new Date().toISOString().slice(0,19).replace(/:/g,'-') + '.csv';
                    a.click();
                    window.URL.revokeObjectURL(url);
                }
                
                function tableToCSV() {
                    const table = document.querySelector('table');
                    if(!table) return '';
                    const rows = table.querySelectorAll('tr');
                    let csv = '';
                    
                    rows.forEach(row => {
                        const cols = row.querySelectorAll('td, th');
                        const rowData = Array.from(cols).map(col => {
                            let text = col.textContent.trim();
                            text = text.replace(/"/g, '""');
                            return '"' + text + '"';
                        });
                        csv += rowData.join(',') + '\\n';
                    });
                    
                    return csv;
                }
                </script>
            """
        }
        
        section("<b>Raw CSV Preview</b>") {
            paragraph "<pre style='background: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto; max-height: 400px;'>${generateCsv()}</pre>"
        }
    }
}

// Single-Shot Version Demarcation Trace Logging Helper
private void checkAndLogVersionDemarcation() {
    String currentVer = version()
    if (state.lastLoggedVersion != currentVer) {
        logTrace "=================== APP VERSION UPDATE: v${currentVer} (${timeStamp()}) ==================="
        state.lastLoggedVersion = currentVer
    }
}

// Dynamic App Label Badging Helper
private void updateAppLabel() {
    Boolean showVersion = getSettingBool("showVersionInLabel", true)
    String baseLabel = "Device Information Viewer (Custom)"
    if (showVersion) baseLabel += " v${version()}"

    if (app.label != baseLabel) {
        app.updateLabel(baseLabel)
    }
}

// Settings Hash Snapshot Helper
private String captureSettingsSnapshot() {
    Map snapshot = [:]
    List<String> sortedKeys = settings.keySet()
        .collect { it.toString() }
        .findAll { k -> !(k == "label" || k.startsWith("btn")) }
        .sort()

    sortedKeys.each { k -> snapshot[k] = settings[k]?.toString() }
    String jsonString = groovy.json.JsonOutput.toJson(snapshot)
    return java.security.MessageDigest.getInstance("MD5").digest(jsonString.bytes).encodeHex().toString()
}

// Hubitat App Lifecycle Routines
void installed() {
    checkAndLogVersionDemarcation()
    logInfo "Installing app v${version()} (${timeStamp()})..."
    state.lastSettingsSnapshot = captureSettingsSnapshot()
    initialize(true)
}

void updated() {
    checkAndLogVersionDemarcation()
    logInfo "Updating app configuration..."

    String currentSnapshot = captureSettingsSnapshot()
    Boolean settingsChanged = (state.lastSettingsSnapshot == null || state.lastSettingsSnapshot != currentSnapshot)
    Boolean codeVersionChanged = (state.lastInitializedVersion != version())

    if (settingsChanged || codeVersionChanged) {
        logInfo "Settings or code version modification detected. Re-establishing subscriptions and tokens..."
        state.lastSettingsSnapshot = currentSnapshot
        unsubscribe()
        unschedule()
        initialize(false)
    } else {
        logDebug "App closed without setting or version changes. Skipping re-initialization."
    }
    updateAppLabel()
}

void uninstalled() {
    logInfo "Uninstalling Device Information Viewer (Custom) app..."
    unsubscribe()
    unschedule()
}

private void initialize(Boolean isInstall = false) {
    state.lastInitializedVersion = version()

    if (!state.accessToken) {
        try {
            createAccessToken()
            logInfo "Created new OAuth access token for CSV endpoints."
        } catch (e) {
            logError "Failed to create access token. Ensure OAuth is enabled in App Details. Error: ${e.message}"
        }
    }

    updateAppLabel()

    if (isInstall) {
        app.updateSetting("logDebugEnable", [type: "bool", value: true])
        logInfo "Debug logging enabled for 30 minutes."
        runIn(1800, "disableDebugLogging")
    } else if (getSettingBool("logDebugEnable", false)) {
        logInfo "Debug logging active. Automatic turn-off scheduled."
        runIn(1800, "disableDebugLogging", [overwrite: false])
    } else {
        unschedule("disableDebugLogging")
    }
}

// Auto-Disable Debug Routine
void disableDebugLogging() {
    if (getSettingBool("logDebugEnable", false)) {
        logWarn "30 minutes have elapsed. Automatically disabling debug logging."
        app.updateSetting("logDebugEnable", [type: "bool", value: false])
    }
}

// Core Data Collector Engine
def collectDeviceData() {
    def allColumns = [] as Set
    def deviceDataList = []
    
    def combinedDevices = []
    if (selectedDevices) {
        combinedDevices.addAll(selectedDevices)
    } else {
        def showAll = (settings.includeAllDevices != null) ? settings.includeAllDevices : true
        if (showAll) {
            combinedDevices = getAllDevices()
        }
    }
    
    combinedDevices = combinedDevices.unique { it.id }
    logTrace "collectDeviceData() processing ${combinedDevices.size()} device(s)..."

    combinedDevices.each { device ->
        def deviceValues = [:]
        
        if (getSettingBool("includeDeviceDetails", true)) {
            if (device.lastActivity) {
                deviceValues['detail.lastActivityAt'] = device.lastActivity.toString()
                allColumns.add('detail.lastActivityAt')
            }
            if (device.controllerType) {
                deviceValues['detail.controllerType'] = device.controllerType.toString()
                allColumns.add('detail.controllerType')
            }
            if (device.typeName) {
                deviceValues['detail.typeName'] = device.typeName.toString()
                allColumns.add('detail.typeName')
            }
            if (device.deviceNetworkId) {
                deviceValues['detail.deviceNetworkId'] = device.deviceNetworkId.toString()
                allColumns.add('detail.deviceNetworkId')
            }
            if (device.roomName) {
                deviceValues['detail.roomName'] = device.roomName.toString()
                allColumns.add('detail.roomName')
            }
        }
        
        if (getSettingBool("includeDeviceData", true)) {
            def deviceDataMap = device.data
            if (deviceDataMap) {
                deviceDataMap.each { key, value ->
                    def columnName = "data.${key}"
                    deviceValues[columnName] = value?.toString() ?: ""
                    allColumns.add(columnName)
                }
            }
        }
        
        if (getSettingBool("includeCurrentStates", true)) {
            device.currentStates?.each { state ->
                def columnName = "state.${state.name}"
                deviceValues[columnName] = state.value?.toString() ?: ""
                allColumns.add(columnName)
            }
        }
        
        deviceDataList.add([
            name: device.name ?: "Unknown Device",
            label: device.label ?: "",
            id: device.id,
            values: deviceValues
        ])
    }
    
    deviceDataList.sort { a, b -> a.name.toLowerCase() <=> b.name.toLowerCase() }
    
    def deviceData = [:]
    deviceDataList.each { device -> deviceData[device.id] = device }
    
    def sortedColumns = allColumns.sort { a, b ->
        def aPrefix = a.tokenize('.')[0]
        def bPrefix = b.tokenize('.')[0]
        def prefixOrder = ['detail': 1, 'data': 2, 'state': 3]
        
        def aOrder = prefixOrder[aPrefix] ?: 4
        def bOrder = prefixOrder[bPrefix] ?: 4
        
        if (aOrder != bOrder) {
            return aOrder <=> bOrder
        } else {
            return a.toLowerCase() <=> b.toLowerCase()
        }
    }
    
    return [columns: sortedColumns, devices: deviceData]
}

def getDisplayName(String columnName) {
    if (columnName.startsWith("detail.")) return columnName.substring(7)
    if (columnName.startsWith("data.")) return columnName.substring(5)
    if (columnName.startsWith("state.")) return columnName.substring(6)
    return columnName
}

def getDefaultSortColumnIndex(columns) {
    if (!defaultSortColumn || defaultSortColumn.trim() == "") return 0
    
    def targetColumn = defaultSortColumn.trim()
    
    if (targetColumn.equalsIgnoreCase("label")) return 1
    if (targetColumn.equalsIgnoreCase("id") || targetColumn.equalsIgnoreCase("device id")) return 2
    
    def columnIndex = 3 
    
    for (colName in columns) {
        if (getDisplayName(colName).equalsIgnoreCase(targetColumn)) return columnIndex
        columnIndex++
    }
    
    columnIndex = 3
    for (colName in columns) {
        if (colName.equalsIgnoreCase(targetColumn) || 
            colName.equalsIgnoreCase("detail.${targetColumn}") ||
            colName.equalsIgnoreCase("data.${targetColumn}") ||
            colName.equalsIgnoreCase("state.${targetColumn}")) {
            return columnIndex
        }
        columnIndex++
    }
    return 0
}

def generateHtmlTable() {
    def data = collectDeviceData()
    def columns = data.columns
    def devices = data.devices
    
    def defaultSortIndex = getDefaultSortColumnIndex(columns)
    def sortDir = defaultSortDirection ?: "asc"
    
    def html = """
        <style>
            table { border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 11px; }
            th, td { border: 1px solid #ddd; padding: 6px; text-align: left; white-space: nowrap; }
            th { background-color: #4CAF50; color: white; position: sticky; top: 0; cursor: pointer; user-select: none; }
            th:hover { background-color: #45a049; }
            th.detail-col { background-color: #2196F3; }
            th.detail-col:hover { background-color: #0b7dda; }
            th.data-col { background-color: #FF9800; }
            th.data-col:hover { background-color: #e68900; }
            th.state-col { background-color: #4CAF50; }
            th.state-col:hover { background-color: #45a049; }
            th.sort-asc::after { content: ' ▲'; font-size: 0.8em; }
            th.sort-desc::after { content: ' ▼'; font-size: 0.8em; }
            tr:nth-child(even) { background-color: #f2f2f2; }
            .scrollable { overflow-x: auto; max-width: 100%; max-height: 500px; }
        </style>
        <div class="scrollable">
        <table id="deviceTable">
            <thead>
                <tr>
                    <th onclick="sortTable(0)">Device Name</th>
                    <th onclick="sortTable(1)">Device Label</th>
                    <th onclick="sortTable(2)">Device ID</th>
    """
    
    def colIndex = 3
    columns.each { colName ->
        def colClass = colName.startsWith("detail.") ? "detail-col" : (colName.startsWith("data.") ? "data-col" : "state-col")
        def displayName = getDisplayName(colName)
        html += "<th class='${colClass}' onclick='sortTable(${colIndex})'>${displayName}</th>"
        colIndex++
    }
    
    html += "</tr></thead><tbody>"
    
    devices.each { deviceId, deviceInfo ->
        html += """
                <tr>
                <td><b><a href='/device/edit/${deviceInfo.id}' target='_blank' style='color: #2196F3; text-decoration: underline;'>${deviceInfo.name}</a></b></td>
                <td>${deviceInfo.label}</td>
                <td>${deviceInfo.id}</td>
        """
        columns.each { colName ->
            def value = deviceInfo.values[colName] ?: ""
            html += "<td>${value}</td>"
        }
        html += "</tr>"
    }
    
    html += """
            </tbody>
        </table>
        </div>
<script>
(function() {
    let sortDirections = {};

    window.sortTable = function(columnIndex, forcedDirection = null) {
        const table = document.getElementById('deviceTable');
        if(!table) return;
        const tbody = table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr'));
        const headers = table.querySelectorAll('th');
        
        const currentDirection = sortDirections[columnIndex] || 'desc';
        const newDirection = forcedDirection || (currentDirection === 'asc' ? 'desc' : 'asc');
        
        sortDirections[columnIndex] = newDirection;
        
        headers.forEach(header => header.classList.remove('sort-asc', 'sort-desc'));
        if (headers[columnIndex]) {
            headers[columnIndex].classList.add('sort-' + newDirection);
        }
        
        rows.sort((a, b) => {
            const aCell = a.querySelectorAll('td')[columnIndex];
            const bCell = b.querySelectorAll('td')[columnIndex];
            const aText = aCell ? aCell.textContent.trim() : '';
            const bText = bCell ? bCell.textContent.trim() : '';
            
            const aNum = parseFloat(aText.replace(/[^0-9.-]/g, ''));
            const bNum = parseFloat(bText.replace(/[^0-9.-]/g, ''));
            
            let comparison = 0;
            if (!isNaN(aNum) && !isNaN(bNum)) {
                comparison = aNum - bNum;
            } else {
                comparison = aText.toLowerCase().localeCompare(bText.toLowerCase());
            }
            return newDirection === 'asc' ? comparison : -comparison;
        });
        
        rows.forEach(row => tbody.appendChild(row));
    };

    setTimeout(function() {
        window.sortTable(${defaultSortIndex}, '${sortDir}');
    }, 50);
})();
</script>
    """
    return html
}

def escapeCsvValue(val) {
    if (val == null) return ""
    String cleanVal = val.toString()
    if (cleanVal.contains('"') || cleanVal.contains(',') || cleanVal.contains(';') || cleanVal.contains('\n') || cleanVal.contains('\r')) {
        return '"' + cleanVal.replace('"', '""') + '"'
    }
    return cleanVal
}

def generateCsv() {
    def delimiter = getDelimiter()
    def data = collectDeviceData()
    def columns = data.columns
    def devices = data.devices
    
    def csv = "\"Device Name\"${delimiter}\"Device Label\"${delimiter}\"Device ID\""
    columns.each { colName ->
        csv += "${delimiter}\"${getDisplayName(colName)}\""
    }
    csv += "\n"
    
    devices.each { deviceId, deviceInfo ->
        csv += "${escapeCsvValue(deviceInfo.name)}${delimiter}${escapeCsvValue(deviceInfo.label)}${delimiter}${escapeCsvValue(deviceInfo.id)}"
        columns.each { colName ->
            csv += "${delimiter}${escapeCsvValue(deviceInfo.values[colName])}"
        }
        csv += "\n"
    }
    return csv
}

def getDelimiter() {
    if (csvDelimiter == "semicolon") return ";"
    if (csvDelimiter == "tab") return "\t"
    return ","
}

def getFilename() {
    def timestamp = includeTimestamp ? "_${new Date().format('yyyyMMdd_HHmmss')}" : ""
    return "device_data${timestamp}.csv"
}

mappings {
    path("/csv") {
        action: [GET: "downloadCsv"]
    }
}

def downloadCsv() {
    def csv = generateCsv()
    def filename = getFilename()
    
    render(
        contentType: "text/csv",
        data: csv,
        headers: ["Content-Disposition": "attachment; filename=\"${filename}\""]
    )
}

// Centralized Logging Engine
private void logMessage(String level, String msg) {
    String lowerLevel = level?.toLowerCase() ?: "info"
    String appLabel = app.label ?: app.name ?: "App"

    String settingKey = "log${lowerLevel.capitalize()}Enable"
    Boolean defaultEnabled = (lowerLevel in ["info", "warn", "error"])

    if (getSettingBool(settingKey, defaultEnabled)) {
        log."${lowerLevel}" "${appLabel}: ${msg}"
    }
}

private void logInfo(String msg)  { logMessage("info", msg) }
private void logDebug(String msg) { logMessage("debug", msg) }
private void logTrace(String msg) { logMessage("trace", msg) }
private void logWarn(String msg)  { logMessage("warn", msg) }
private void logError(String msg) { logMessage("error", msg) }

private Boolean getSettingBool(String key, Boolean defaultVal = false) {
    def val = settings[key]
    if (val == null) return defaultVal
    if (val instanceof Boolean) return val
    return val.toString().toBoolean()
}