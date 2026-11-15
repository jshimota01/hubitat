# Changelog

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
