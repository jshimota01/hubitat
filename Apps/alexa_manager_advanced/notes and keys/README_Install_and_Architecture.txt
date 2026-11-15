AMA HUBITAT MODULE PACKAGE v0.5.3
Date: 2026-11-09

ARCHITECTURE
The AMA parent is the only communication hub. There are NO child-to-child calls, references, subscriptions, or shared-state access. Each child communicates upward to the parent; the parent routes information downward to the appropriate child through public APIs.

MODULES
- Alexa Manager Advanced Parent v0.5.3: orchestration, shared NAS service boundary, legacy v0.4.x compatibility, public module APIs, and message routing.
- AMA Device Handling Child v0.1.3: authorized device discovery and scanner-owned inventory. Publishes completed inventory status upward to parent.
- AMA Device Management Child v0.1.2: owns management records and receives inventory notifications from parent. Reads inventory through parent APIs only. No Amazon registration/removal is implemented.
- AMA Monitoring & Analytics Child v0.1.2: receives parent-routed events and owns local telemetry history. Future InfluxDB reporting will be direct from Hubitat child to InfluxDB, not through AMA NAS. Network reporting is not enabled in this release.

UPDATE / INSTALL NOTES
1. Do not uninstall any existing AMA app. These sources preserve the existing definition names, namespaces, parent identity, and child appName identities.
2. Update the existing parent source in place with alexa_manager_advanced_parent_v0.5.3.groovy and Save.
3. Update each existing child source in place with its matching v0.1.3 / v0.1.2 file and Save. Keep each app's existing installation.
4. Open each app page once so the versioned app label is refreshed. Child app labels will show their own independent versions.
5. Confirm the parent lists all three children and that the existing 172-device inventory remains present in Device Handling. Do not run a scan just for this architecture update.
6. If desired later, run one normal scan; a completed scan reports inventory status up to parent, which routes a notification to Management and Monitoring.

STATE PRESERVATION
Device Handling keeps state.deviceInventory and existing scanner settings. Device Management introduces state.managementRecords without altering scanner inventory. Monitoring introduces a bounded state.eventHistory. Parent adds only module communication status fields.

IMPORTANT LIMITATIONS
- This package implements the communication foundation, not the complete naming/approval UI, Amazon exposure workflow, or InfluxDB transport.
- Discovery remains limited to devices explicitly authorized by the capability.* selector.
- A scan never automatically approves, publishes, or removes an Amazon endpoint.
- Static review only was performed here; Hubitat runtime behavior must be confirmed on the installed hub.
