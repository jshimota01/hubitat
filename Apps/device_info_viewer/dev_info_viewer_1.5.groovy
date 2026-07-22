/**
 *  Device Information Viewer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * NO copyright claimed, released to the public domain. Written by Claude under the direction of John Land
 *
2026-01-24: Initial version
2026-01-25: Changed device names to hot links to the corresponding Hubitat device page
2026-07-18: Added device label column next to device name
2026-07-19: repair device label not showing associated name
2026-07-20: added option to include ALL devices (defaults to things that only have Refresh - thats why list isn't complete)
2026-07-20: Refactored to default to ALL devices (capability.*) enabled
*/

definition(
    name: "Device Information Viewer",
    namespace: "Ver. 1.5",
    author: "Custom",
    description: "View and export device current states, device data, and device details as HTML table or CSV",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    oauth: true 
)

preferences {
    page(name: "mainPage")
    page(name: "viewData")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Device Information Viewer", install: true, uninstall: true) {
        section("Device Selection") {
            // Defaulted to true so capability.* is active by default
            input "includeAllDevices", "bool", title: "Show ALL devices inlcuding Groups and Virtual in pick list below.", defaultValue: true, submitOnChange: true
            
            // Evaluates includeAllDevices setting; defaults to capability.* when null/true
            def showAll = (settings.includeAllDevices != null) ? settings.includeAllDevices : true
            def capType = showAll ? "capability.*" : "capability.refresh"
            
            input "selectedDevices", capType, title: "Choose Devices", multiple: true, required: false
        }
        
        section("Data to Include") {
            input "includeCurrentStates", "bool", title: "Include Current States (attributes)", defaultValue: true
            input "includeDeviceData", "bool", title: "Include Device Data", defaultValue: true
            input "includeDeviceDetails", "bool", title: "Include Device Details (metadata)", defaultValue: true
        }
        
        section("Default Sort Options") {
            input "defaultSortColumn", "text", title: "Default Sort Column Name (leave blank for Device Name)", description: "Enter exact column name (e.g., 'roomName', 'switch', 'manufacturer')", required: false
            input "defaultSortDirection", "enum", title: "Default Sort Direction", options: ["asc": "Ascending ▲", "desc": "Descending ▼"], defaultValue: "asc"
        }
        
        section("Export Options") {
            input "includeTimestamp", "bool", title: "Include timestamp in filename", defaultValue: true
            input "csvDelimiter", "enum", title: "CSV Delimiter", options: ["comma": "Comma (,)", "semicolon": "Semicolon (;)", "tab": "Tab"], defaultValue: "comma"
        }
        
        section("View & Export") {
            href "viewData", title: "View Device Data", description: "Click to view all device data"
            paragraph "<i>Tip: Click any column header in the table to sort by that column.</i>"
        }
        
        section("CSV Download Links") {
            paragraph "After installing this app, you can access the CSV export via endpoints below."
            if (!state.accessToken) {
                initialize()
            }
            if (state.accessToken) {
                def localUrl = "${getFullLocalApiServerUrl()}/csv?access_token=${state.accessToken}"
                def cloudUrl = "${getFullApiServerUrl()}/csv?access_token=${state.accessToken}"
                
                paragraph "<b>Local URL:</b> <a href='${localUrl}' target='_blank'>${localUrl}</a>"
                if (enableCloud) {
                    paragraph "<b>Cloud URL:</b> <a href='${cloudUrl}' target='_blank'>${cloudUrl}</a>"
                }
            }
            input "enableCloud", "bool", title: "Enable Cloud Endpoint Access", defaultValue: false, submitOnChange: true
        }
        
        section("Note") {
            paragraph "<i>Note: 'Create Time' and 'Last Update Time' shown in Hubitat's device details are not accessible through the app API and cannot be exported.</i>"
        }
    }
}

def viewData() {
    dynamicPage(name: "viewData", title: "Data Viewer", install: false, uninstall: false) {
        section {
            paragraph generateHtmlTable()
        }
        
        section("Actions") {
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
        
        section("CSV Preview") {
            paragraph "<pre style='background: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto; max-height: 400px;'>${generateCsv()}</pre>"
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    if (!state.accessToken) {
        try {
            createAccessToken()
        } catch (e) {
            log.error "Failed to create access token. Ensure OAuth is checked in app configurations. Error: ${e.message}"
        }
    }
}

def collectDeviceData() {
    def allColumns = [] as Set
    def deviceDataList = []
    
    def combinedDevices = []
    if (selectedDevices) combinedDevices.addAll(selectedDevices)
    combinedDevices = combinedDevices.unique { it.id }
    
    combinedDevices.each { device ->
        def deviceValues = [:]
        
        if (includeDeviceDetails) {
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
        
        if (includeDeviceData) {
            def deviceDataMap = device.data
            if (deviceDataMap) {
                deviceDataMap.each { key, value ->
                    def columnName = "data.${key}"
                    deviceValues[columnName] = value?.toString() ?: ""
                    allColumns.add(columnName)
                }
            }
        }
        
        if (includeCurrentStates) {
            device.currentStates?.each { state ->
                def columnName = "state.${state.name}"
                deviceValues[columnName] = state.value?.toString() ?: ""
                allColumns.add(columnName)
            }
        }
        
        deviceDataList.add([
            name: device.name ?: "Unknown Device", // Maps cleanly to the Hubitat Device Name
            label: device.label ?: "",              // Maps cleanly to the Hubitat Device Label
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
        let sortDirections = {${defaultSortIndex}: '${sortDir}'};
        
        function sortTable(columnIndex) {
            const table = document.getElementById('deviceTable');
            if(!table) return;
            const tbody = table.querySelector('tbody');
            const rows = Array.from(tbody.querySelectorAll('tr'));
            const headers = table.querySelectorAll('th');
            
            const currentDirection = sortDirections[columnIndex] || 'asc';
            const newDirection = currentDirection === 'asc' ? 'desc' : 'asc';
            sortDirections[columnIndex] = newDirection;
            
            headers.forEach(header => header.classList.remove('sort-asc', 'sort-desc'));
            headers[columnIndex].classList.add('sort-' + newDirection);
            
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
        }
        
        document.addEventListener("DOMContentLoaded", function() {
            sortTable(${defaultSortIndex});
        });
        </script>
    """
    return html
}

def escapeCsvValue(val) {
    if (!val) return ""
    String cleanVal = val.toString()
    if (cleanVal.contains('"') || cleanVal.contains(',') || cleanVal.contains(';') || cleanVal.contains('\n') || cleanVal.contains('\r')) {
        return '"' + cleanVal.replace('"', '""') + '"'
    }
    return '"' + cleanVal + '"'
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