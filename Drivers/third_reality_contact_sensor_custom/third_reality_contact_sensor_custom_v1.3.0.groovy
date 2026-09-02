/*
 * Third Reality Contact Sensor (Custom)
 *
 * Licensed under the Apache License, Version 2.0
*/
/*
 * [KEEP]
 * Date			Version	Dev			Note
 * ========================================================================================
 * 2026-08-27 	1.3.0	jshimota 	Prefixed all logging statements with device displayName.
 * 2026-08-27 	1.2.0	jshimota 	Added logText option and logging for sendEvent actions.
 * 2026-08-27 	1.1.0	jshimota 	Added 1 Hour and 3 Hours options to check-in interval preference.
 * 2026-08-27 	1.0.0	jshimota 	Initial release of Third Reality Contact Sensor driver.
 */

static String version() { return "1.3.0" }

metadata {
    definition (
        name: "Third Reality Contact Sensor (Custom)", 
        namespace: "jshimota", 
        author: "James Shimota",
        description: "Targeted Zigbee driver for Third Reality 3RDS17BZ contact sensors featuring forced battery check-ins to maintain active status."
    ) {
        capability "Contact Sensor"
        capability "Battery"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"

        // Explicit fingerprint for 3RDS17BZ
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0500", outClusters: "0019", manufacturer: "3Real", model: "3RDS17BZ", deviceJoinName: "Third Reality Contact Sensor"
        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0500", outClusters: "0019", manufacturer: "Third Reality, Inc.", model: "3RDS17BZ", deviceJoinName: "Third Reality Contact Sensor"
    }

    preferences {
        input name: "checkinInterval", type: "enum", title: "Mandatory Battery Check-in Interval", options: ["3600":"1 Hour", "10800":"3 Hours", "21600":"6 Hours", "43200":"12 Hours", "86400":"24 Hours"], defaultValue: "43200"
        input name: "txtEnable", type: "bool", title: "Enable Description Text Logging", defaultValue: true
        input name: "logEnable", type: "bool", title: "Enable Debug Logging", defaultValue: false
    }
}

def parse(String description) {
    if (logEnable) log.debug "${device.displayName}: Parse raw: ${description}"
    
    // Parse standard IAS Zone (0x0500) contact status
    if (description?.startsWith("zone status")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        int zoneStatus = Integer.parseInt(descMap.zoneStatus, 16)
        boolean isOpen = (zoneStatus & 1) == 1
        String val = isOpen ? "open" : "closed"
        String descText = "was turned ${val}"
        
        if (txtEnable) log.info "${device.displayName}: ${descText}"
        sendEvent(name: "contact", value: val, descriptionText: "${device.displayName} ${descText}")
    } 
    // Parse Power Configuration Cluster (0x0001) - Battery Percentage
    else if (description?.startsWith("read attr - raw:") || description?.startsWith("catchall:")) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        
        if (descMap.cluster == "0001" && descMap.attrId == "0021") {
            int rawVal = Integer.parseInt(descMap.value, 16)
            int batteryVal = Math.round(rawVal / 2) // Standard Zigbee ½% scaling
            if (batteryVal > 100) batteryVal = 100
            
            if (logEnable) log.debug "${device.displayName}: Battery report received: ${batteryVal}%"
            
            String descText = "battery check-in: ${batteryVal}%"
            if (txtEnable) log.info "${device.displayName}: ${descText}"
            
            // isStateChange: true forces Hubitat to update 'Last Activity At' 
            // even if the percentage hasn't changed since the last report.
            sendEvent(
                name: "battery", 
                value: batteryVal, 
                unit: "%", 
                isStateChange: true, 
                descriptionText: "${device.displayName} ${descText}"
            )
        }
    }
}

def configure() {
    if (txtEnable) log.info "${device.displayName}: Configuring reporting intervals..."
    int maxSecs = (settings.checkinInterval ?: "43200").toInteger()
    
    // Bind to Power Cluster (0x0001), Attribute (0x0021), UInt8 (0x20)
    // Send report at least once every 'maxSecs' even if value has not changed.
    return zigbee.configureReporting(0x0001, 0x0021, 0x20, 30, maxSecs, 0x01) + 
           zigbee.enrollResponse() + 
           zigbee.readAttribute(0x0001, 0x0021)
}

def updated() {
    if (txtEnable) log.info "${device.displayName}: Preferences updated, re-applying configuration..."
    return configure()
}

def refresh() {
    if (logEnable) log.debug "${device.displayName}: Refresh requested"
    return zigbee.readAttribute(0x0001, 0x0021) + zigbee.enrollResponse()
}