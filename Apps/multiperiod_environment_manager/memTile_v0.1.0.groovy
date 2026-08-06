/*
 *  Multiperiod Environment Manager (MEM) Dashboard Tile (Virtual Mem Dashboard Tile Driver)
 *  
 *  Used to hold tile and necesary attributes derived from MEM app for use in Dashboards.  Values of AQI average, Thermostat Schedules, 
 *  Thermostat setpoints, Room Air Filter and Lights that reflect AQI Warning
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Change History:
 *
 *      Date          Source        Version     What                                                
 *      ----          ------        -------     ----
 * 		2026-08-04    Jshimota		0.1.0		Initial Starting Version
 *
 */

static String version() { return '0.1.0' }

metadata {
    definition(
        name: "MEM Dashboard Tile",
        namespace: "jshimota",
        author: "James Shimota"
    ) {
        capability "Sensor"

        attribute "memTile", "string"
    }
}