# Changelog

## 0.5.3 - 2026-11-11
- Corrects the runtime `appVersion` in `src/config.js` to match the package release version.
- Corrects NAS and Hubitat connection documentation to use `192.168.1.23:4100`; retains `192.168.1.12:8086` for InfluxDB.
- Includes the versioned `data/` snapshot in the release archive while excluding the live `.env` secrets file.
- Clarifies that the archive contains a recoverable data snapshot but routine deployment preserves live NAS data.

## 0.5.2 - 2026-11-10
- Adds authenticated Hubitat event ingestion at `/api/v1/telemetry/hubitat-event`.
- NAS normalizes Hubitat event metadata into searchable InfluxDB tags and fields.
- Preserves the complete received event payload as a JSON field.
- Keeps the existing Alexa directive endpoint and endpoint telemetry behavior unchanged.

## 0.5.0 — 2026-11-10
- Added direct NAS-to-InfluxDB 2.x telemetry using a dedicated NAS token.
- Added independent telemetry enable/configuration environment variables.
- Added authenticated telemetry status and test-write API endpoints; test writes work while continuous telemetry is disabled.
- Added non-blocking telemetry for service startup, Hubitat device registration/state updates, and Alexa directive outcomes.
- Telemetry failures are logged and isolated from Alexa/Hubitat request handling.

# Changelog

## 0.5.3 - 2026-11-11
- Corrects the runtime `appVersion` in `src/config.js` to match the package release version.
- Corrects NAS and Hubitat connection documentation to use `192.168.1.23:4100`; retains `192.168.1.12:8086` for InfluxDB.
- Includes the versioned `data/` snapshot in the release archive while excluding the live `.env` secrets file.
- Clarifies that the archive contains a recoverable data snapshot but routine deployment preserves live NAS data.

## 0.4.0 — 2026-11-04
- Added Hubitat switch registration/upsert API.
- Added Hubitat switch state synchronization API.
- Added NAS-to-Hubitat command callback client for TurnOn/TurnOff.
- Alexa PowerController now routes Hubitat-backed devices to their local Hubitat app endpoint while retaining local simulation behavior.
- Added Hubitat parent app source for selecting switches, registration, event subscription, state reporting, and command callbacks.
- Preserved OAuth, token store, existing simulated test device behavior, and independent Echos Speak isolation.

## 0.3.0 — 2026-11-04
- Added Alexa Smart Home API v3 Discovery, PowerController, and StateReport.
- Added persistent device registry and local simulation backend.
