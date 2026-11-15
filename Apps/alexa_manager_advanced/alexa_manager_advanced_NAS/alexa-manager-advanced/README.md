# Alexa Manager Advanced — NAS Service

Version 0.4.0 adds the first local Hubitat integration while preserving the existing Alexa OAuth and Smart Home API foundation. This project is completely separate from Echos Speak.

## v0.4.0 scope

- Keeps the v0.3.0 OAuth, Discovery, PowerController, StateReport, and simulated device backend.
- Adds Hubitat switch registration via `PUT /api/v1/devices/:endpointId`.
- Adds Hubitat state synchronization via `PUT /api/v1/devices/:endpointId/state`.
- Adds NAS-to-Hubitat local command callbacks for TurnOn and TurnOff.
- Hubitat app source is supplied separately as `Hubitat_Alexa_Manager_Advanced_v0.4.0.groovy`.
- No cloud services, router changes, public exposure, reverse proxy, or Echos Speak dependencies.

## Install/update on Synology

Back up the project folder first. Replace application source files from this archive, but **preserve the existing `.env`, `data/`, and `docker-compose.yml`**. The archive intentionally does not include production secrets or persistent data. Rebuild and recreate the existing container using the commands in the project notes. The container remains bound to `192.168.1.12:4100`.

## Hubitat setup

1. Add the supplied Groovy app as a user app in Hubitat and install it.
2. Set AMA Base URL to `http://192.168.1.12:4100`.
3. Enter the existing AMA API key in the app's password field.
4. Select one or more switch devices and save.
5. The app registers each selected device, pushes state changes, exposes a local authenticated command callback to AMA, and removes devices from AMA when they are deselected or the app is uninstalled.

The app uses Hubitat's local app endpoint and access token. Do not forward this endpoint through the router or expose it to the internet.

## API changes

All `/api/v1/*` routes require `Authorization: Bearer <AMA_API_KEY>`.

- `PUT /api/v1/devices/:endpointId` — register/update a Hubitat switch, including its local command callback URL.
- `PUT /api/v1/devices/:endpointId/state` — update the last reported ON/OFF state.
- Existing `GET /api/v1/devices`, `POST /api/v1/devices`, `DELETE /api/v1/devices/:endpointId`, and `/api/v1/alexa/directive` remain.

Hubitat command callback requests are made only from the NAS to the callback URL registered by the Hubitat app.

## Limitations in this first integration

- Switch capability only (on/off).
- State synchronization is event-driven plus initial registration.
- No dimmer, thermostat, lock, contact, motion, sensor, or fan mapping yet.
- No Amazon skill or AWS Lambda deployment is included in this release.

## Testing

Run `npm run check` and `npm test`.
