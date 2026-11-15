ALEXA MANAGER ADVANCED (AMA) - HUBITAT v0.5.1 INITIAL ARCHITECTURE
Date: 2026/11/09

CONTENTS
1. alexa_manager_advanced_parent_v0.5.1.groovy
2. ama_device_handling_child_v0.1.1.groovy
3. ama_device_management_child_v0.1.1.groovy
4. ama_monitoring_analytics_child_v0.1.1.groovy

PURPOSE
This is the first parent/child architecture foundation for Alexa Manager Advanced.
The first functional branch is Hubitat device discovery and inventory.
Device Management Child and Monitoring & Analytics Child are registered child-app shells for the
next development stages; they do not yet publish endpoints or send InfluxDB telemetry.

IMPORTANT TRANSITION SAFEGUARD
The parent retains the old v0.4.x switch selection setting and local /command callback.
It continues subscribing to the previously selected switch devices and forwards their
state changes to the existing AMA NAS endpoint. It deliberately does NOT re-register
or deregister endpoints during initialization. Do not clear the hidden legacy switch
selection until the new Device Management Child exposure workflow has replaced it.

INSTALLATION ORDER
1. Back up/export the currently installed v0.4.1 Hubitat app source and note its settings.
2. In Hubitat Apps Code, replace the existing Alexa Manager Advanced parent source with
   alexa_manager_advanced_parent_v0.5.1.groovy and Save.
3. Add the three child application sources in Apps Code and Save each one:
   - AMA Device Handling Child
   - AMA Device Management Child
   - AMA Monitoring & Analytics Child
4. Return to the installed Alexa Manager Advanced app and Save/Done to run updated().
5. Confirm the existing v0.4.x selected switches still appear under the hidden
   LEGACY ENDPOINT COMPATIBILITY section (expand it if necessary).
6. Open the parent app and install AMA Device Handling Child from its branch link.
7. Open Device Handling Child and run Scan Hubitat Devices.
8. Review the scan count and result. No device should be newly exposed to Amazon.

FIRST SCAN EXPECTATIONS
The inventory is keyed by Hubitat device ID. It records:
- Hubitat Name, Label, display name, room, device network ID
- Driver type
- Manufacturer/model/firmware and selected device data values
- Detected capabilities
- Supported attributes and commands
- Current values for supported attributes
- First/last discovery timestamps and presence in the current Hubitat fleet

RESCAN BEHAVIOR
- A rescan refreshes scanner-owned metadata.
- AMA-owned fields (Amazon Name, selected Amazon capabilities, approval, exposure state,
  and naming-review status) are preserved if already present.
- Devices omitted because virtual devices are filtered remain in the inventory.
- Devices no longer found in Hubitat are retained and marked absent; they are not deleted.
- Scanning does not register, expose, disable, or delete Amazon endpoints.

KNOWN IMPLEMENTATION BOUNDARY
This initial scanner stores the device library in the Device Handling Child application's Hubitat
state. The agreed NAS SQLite registry synchronization is a subsequent step and requires
matching the actual AMA NAS API contract before implementing any write operation.
The scanner's capability detection uses Hubitat-reported capability metadata when
available and falls back to attribute/command evidence. Custom driver declarations may
need later mapping refinements after the first real inventory is reviewed.

VALIDATION
These files have been reviewed for internal consistency, but they have not been compiled
or executed on the user's Hubitat hub. The first installation should be treated as a
controlled test. Preserve the existing v0.4.1 source backup for immediate rollback.
