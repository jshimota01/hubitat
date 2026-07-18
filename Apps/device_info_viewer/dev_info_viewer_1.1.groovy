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
*/

definition(
    name: "Device Information Viewer",
    namespace: "Ver. 1.1",
    author: "Custom",
    description: "View and export device current states, device data, and device details as HTML table or CSV",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)
preferences {
    page(name: "mainPage")
    page(name: "viewData")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        section("Device Selection", hideable: true, hidden: true) {
            input "selectedDevices", "capability.*", title: "Choose Devices", multiple: true, required: true
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
        
        section("CSV Download") {
            paragraph "After installing this app, you can access the CSV export via:"
            paragraph "<b>Local URL:</b> http://[HUB-IP]:8080/local/[APP-ID]/csv"
            if (state.accessToken) {
                paragraph "<b>Cloud URL:</b> ${getFullApiServerUrl()}/csv?access_token=${state.accessToken}"
            }
            input "enableCloud", "bool", title: "Enable Cloud Endpoint (allows external access)", defaultValue: false, submitOnChange: true
        }
        
        section("Note") {
            paragraph "<i>Note: 'Create Time' and 'Last Update Time' shown in Hubitat's device details are not accessible through the app API and cannot be exported.</i>"
        }
    }
}

def viewData() {
    dynamicPage(name: "viewData", title: "", install: false, uninstall: false) {
        section {
            paragraph generateHtmlTable()
        }
        
        section("Actions") {
            paragraph """
                <button onclick="copyTableToClipboard()">Copy Table as CSV</button>
                <button onclick="downloadSortedCsv()">Download CSV (current sort)</button>
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
                    const rows = table.querySelectorAll('tr');
                    let csv = '';
                    
                    rows.forEach(row => {
                        const cols = row.querySelectorAll('td, th');
                        const rowData = Array.from(cols).map(col => {
                            let text = col.textContent.trim();
                            // Escape quotes and wrap in quotes
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
            paragraph "<pre style='background: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto;'>${generateCsv()}</pre>"
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
    if (enableCloud && !state.accessToken) {
        createAccessToken()
    } else if (!enableCloud && state.accessToken) {
        state.accessToken = null
    }
}

def collectDeviceData() {
    def allColumns = [] as Set
    def deviceDataList = []
    
    selectedDevices?.each { device ->
        def deviceValues = [:]
        
        // Collect Device Details (metadata) first
        if (includeDeviceDetails) {
            try {
                // Last Activity
                try {
                    def lastActivity = device.lastActivity
                    if (lastActivity) {
                        deviceValues['detail.lastActivityAt'] = lastActivity.toString()
                        allColumns.add('detail.lastActivityAt')
                    }
                } catch (Exception e) {
                    log.debug "Could not get lastActivity: ${e.message}"
                }
                
                // Controller Type
                try {
                    def controllerType = device.controllerType
                    if (controllerType) {
                        deviceValues['detail.controllerType'] = controllerType.toString()
                        allColumns.add('detail.controllerType')
                    }
                } catch (Exception e) {
                    log.debug "Could not get controllerType: ${e.message}"
                }
                
                // Type Name
                try {
                    def typeName = device.typeName
                    if (typeName) {
                        deviceValues['detail.typeName'] = typeName.toString()
                        allColumns.add('detail.typeName')
                    }
                } catch (Exception e) {
                    log.debug "Could not get typeName: ${e.message}"
                }
                
                // Device Network ID
                try {
                    def deviceNetworkId = device.deviceNetworkId
                    if (deviceNetworkId) {
                        deviceValues['detail.deviceNetworkId'] = deviceNetworkId.toString()
                        allColumns.add('detail.deviceNetworkId')
                    }
                } catch (Exception e) {
                    log.debug "Could not get deviceNetworkId: ${e.message}"
                }
                
                // Room Name
                try {
                    def roomName = device.roomName
                    if (roomName) {
                        deviceValues['detail.roomName'] = roomName.toString()
                        allColumns.add('detail.roomName')
                    }
                } catch (Exception e) {
                    log.debug "Could not get roomName: ${e.message}"
                }
                
            } catch (Exception e) {
                log.warn "Could not access details for device ${device.displayName}: ${e.message}"
            }
        }
        
        // Collect Device Data
        if (includeDeviceData) {
            try {
                def deviceDataMap = device.data
                if (deviceDataMap) {
                    deviceDataMap.each { key, value ->
                        def columnName = "data.${key}"
                        deviceValues[columnName] = value?.toString() ?: ""
                        allColumns.add(columnName)
                    }
                }
            } catch (Exception e) {
                log.warn "Could not access data for device ${device.displayName}: ${e.message}"
            }
        }
        
        // Collect Current States (attributes)
        if (includeCurrentStates) {
            device.currentStates?.each { state ->
                def columnName = "state.${state.name}"
                deviceValues[columnName] = state.value?.toString() ?: ""
                allColumns.add(columnName)
            }
        }
        
        deviceDataList.add([
            name: device.displayName,
            id: device.id,
            values: deviceValues
        ])
    }
    
    // Sort devices by name (A-Z) by default
    deviceDataList.sort { a, b ->
        a.name.toLowerCase() <=> b.name.toLowerCase()
    }
    
    // Convert to map with id as key (maintaining sort order)
    def deviceData = [:]
    deviceDataList.each { device ->
        deviceData[device.id] = device
    }
    
    // Sort columns: device info first, then detail.*, then data.*, then state.*
    // Within each section, sort alphabetically (case-insensitive)
    def sortedColumns = allColumns.sort { a, b ->
        def aPrefix = a.tokenize('.')[0]
        def bPrefix = b.tokenize('.')[0]
        def prefixOrder = ['detail': 1, 'data': 2, 'state': 3]
        
        def aOrder = prefixOrder[aPrefix] ?: 4
        def bOrder = prefixOrder[bPrefix] ?: 4
        
        if (aOrder != bOrder) {
            return aOrder <=> bOrder
        } else {
            // Case-insensitive alphabetical sort within each section
            return a.toLowerCase() <=> b.toLowerCase()
        }
    }
    
    return [
        columns: sortedColumns,
        devices: deviceData
    ]
}

def getDisplayName(String columnName) {
    // Remove prefixes for display
    if (columnName.startsWith("detail.")) {
        return columnName.substring(7) // Remove "detail."
    } else if (columnName.startsWith("data.")) {
        return columnName.substring(5) // Remove "data."
    } else if (columnName.startsWith("state.")) {
        return columnName.substring(6) // Remove "state."
    }
    return columnName
}

def getDefaultSortColumnIndex(columns) {
    // If no default sort column specified, use Device Name (index 0)
    if (!defaultSortColumn || defaultSortColumn.trim() == "") {
        return 0
    }
    
    // Try to find the column by display name (without prefix)
    def targetColumn = defaultSortColumn.trim()
    def columnIndex = 2 // Start at 2 (0=Device Name, 1=Device ID)
    
    for (colName in columns) {
        def displayName = getDisplayName(colName)
        if (displayName.equalsIgnoreCase(targetColumn)) {
            return columnIndex
        }
        columnIndex++
    }
    
    // Also try matching with prefixes
    columnIndex = 2
    for (colName in columns) {
        if (colName.equalsIgnoreCase(targetColumn) || 
            colName.equalsIgnoreCase("detail.${targetColumn}") ||
            colName.equalsIgnoreCase("data.${targetColumn}") ||
            colName.equalsIgnoreCase("state.${targetColumn}")) {
            return columnIndex
        }
        columnIndex++
    }
    
    // If not found, default to Device Name
    log.warn "Column '${targetColumn}' not found, defaulting to Device Name"
    return 0
}

def generateHtmlTable() {
    def data = collectDeviceData()
    def columns = data.columns
    def devices = data.devices
    
    // Determine default sort column index
    def defaultSortIndex = getDefaultSortColumnIndex(columns)
    def sortDir = defaultSortDirection ?: "asc"
    
    def html = """
        <style>
            table {
                border-collapse: collapse;
                width: 100%;
                margin: 10px 0;
                font-size: 11px;
            }
            th, td {
                border: 1px solid #ddd;
                padding: 6px;
                text-align: left;
                white-space: nowrap;
            }
            th {
                background-color: #4CAF50;
                color: white;
                position: sticky;
                top: 0;
                cursor: pointer;
                user-select: none;
            }
            th:hover {
                background-color: #45a049;
            }
            th.detail-col {
                background-color: #2196F3;
            }
            th.detail-col:hover {
                background-color: #0b7dda;
            }
            th.data-col {
                background-color: #FF9800;
            }
            th.data-col:hover {
                background-color: #e68900;
            }
            th.state-col {
                background-color: #4CAF50;
            }
            th.state-col:hover {
                background-color: #45a049;
            }
            th.sort-asc::after {
                content: ' ▲';
                font-size: 0.8em;
            }
            th.sort-desc::after {
                content: ' ▼';
                font-size: 0.8em;
            }
            tr:nth-child(even) {
                background-color: #f2f2f2;
            }
            .scrollable {
                overflow-x: auto;
                max-width: 100%;
            }
        </style>
        <div class="scrollable">
        <table id="deviceTable">
            <thead>
                <tr>
                    <th onclick="sortTable(0)">Device Name</th>
                    <th onclick="sortTable(1)">Device ID</th>
    """
    
    // Add column headers
    def colIndex = 2
    columns.each { colName ->
        def colClass = ""
        if (colName.startsWith("detail.")) {
            colClass = "detail-col"
        } else if (colName.startsWith("data.")) {
            colClass = "data-col"
        } else if (colName.startsWith("state.")) {
            colClass = "state-col"
        }
        
        def displayName = getDisplayName(colName)
        html += "<th class='${colClass}' onclick='sortTable(${colIndex})'>${displayName}</th>"
        colIndex++
    }
    
    html += """
                </tr>
            </thead>
            <tbody>
    """
    
    // Add rows for each device
    devices.each { deviceId, deviceInfo ->
        html += """
                <tr>
                <td><b><a href='/device/edit/${deviceInfo.id}' target='_blank' style='color: inherit; text-decoration: none;'>${deviceInfo.name}</a></b></td>
                <td>${deviceInfo.id}</td>
        """
        
        // Add cells for each column
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
        let sortDirections = {${defaultSortIndex}: '${sortDir}'}; // Initialize with default sort
        
        function sortTable(columnIndex) {
            const table = document.getElementById('deviceTable');
            const tbody = table.querySelector('tbody');
            const rows = Array.from(tbody.querySelectorAll('tr'));
            const headers = table.querySelectorAll('th');
            
            // Determine sort direction
            const currentDirection = sortDirections[columnIndex] || 'asc';
            const newDirection = currentDirection === 'asc' ? 'desc' : 'asc';
            sortDirections[columnIndex] = newDirection;
            
            // Remove sort indicators from all headers
            headers.forEach(header => {
                header.classList.remove('sort-asc', 'sort-desc');
            });
            
            // Add sort indicator to current header
            headers[columnIndex].classList.add('sort-' + newDirection);
            
            // Sort rows
            rows.sort((a, b) => {
                const aCell = a.querySelectorAll('td')[columnIndex];
                const bCell = b.querySelectorAll('td')[columnIndex];
                const aText = aCell ? aCell.textContent.trim() : '';
                const bText = bCell ? bCell.textContent.trim() : '';
                
                // Try to parse as numbers for numeric sorting
                const aNum = parseFloat(aText.replace(/[^0-9.-]/g, ''));
                const bNum = parseFloat(bText.replace(/[^0-9.-]/g, ''));
                
                let comparison = 0;
                
                if (!isNaN(aNum) && !isNaN(bNum)) {
                    // Numeric comparison
                    comparison = aNum - bNum;
                } else {
                    // String comparison (case-insensitive)
                    comparison = aText.toLowerCase().localeCompare(bText.toLowerCase());
                }
                
                return newDirection === 'asc' ? comparison : -comparison;
            });
            
            // Re-append sorted rows
            rows.forEach(row => tbody.appendChild(row));
        }
        
        // Apply default sort on page load
        (function() {
            sortTable(${defaultSortIndex});
        })();
        </script>
    """
    
    return html
}

def generateCsv() {
    def delimiter = getDelimiter()
    def data = collectDeviceData()
    def columns = data.columns
    def devices = data.devices
    
    // Header row
    def csv = "\"Device Name\"${delimiter}\"Device ID\""
    columns.each { colName ->
        def displayName = getDisplayName(colName)
        csv += "${delimiter}\"${displayName}\""
    }
    csv += "\n"
    
    // Data rows (already sorted by device name from collectDeviceData)
    devices.each { deviceId, deviceInfo ->
        csv += "\"${deviceInfo.name}\"${delimiter}${deviceInfo.id}"
        
        columns.each { colName ->
            def value = deviceInfo.values[colName] ?: ""
            csv += "${delimiter}\"${value}\""
        }
        
        csv += "\n"
    }
    
    return csv
}

def getDelimiter() {
    switch(csvDelimiter) {
        case "semicolon":
            return ";"
        case "tab":
            return "\t"
        default:
            return ","
    }
}

def getFilename() {
    def timestamp = includeTimestamp ? "_${new Date().format('yyyyMMdd_HHmmss')}" : ""
    return "device_data${timestamp}.csv"
}

// Mappings for endpoints
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
