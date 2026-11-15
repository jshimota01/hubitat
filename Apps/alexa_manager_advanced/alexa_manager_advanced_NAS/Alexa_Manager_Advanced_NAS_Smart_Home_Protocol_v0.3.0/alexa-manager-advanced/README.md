# Alexa Manager Advanced — NAS Service

Version 0.3.0. This is an independent service and does not share code runtime, data, credentials, ports, containers, or services with Echos Speak.

## Current scope

- Node.js 24 / npm 12 container.
- Express API and health endpoint at `/api/health`.
- API-key protected management and status endpoints.
- OAuth authorization-code foundation at `/oauth/authorize` and `/oauth/token`, using PKCE S256.
- Hashed authorization-code and token records persisted in `./data/oauth-store.json`.
- Access and refresh tokens are bound to the OAuth client ID; refresh requires the same client ID.
- Alexa Smart Home API v3 protocol handling for `Alexa.Discovery.Discover`, `Alexa.PowerController.TurnOn`, `Alexa.PowerController.TurnOff`, and `Alexa.ReportState`.
- Local-simulation device registry persisted in `./data/devices.json`.

**Important:** Devices created in this version are local simulations. Power commands only update the NAS-side simulation record; they do not control Hubitat or physical devices. The Hubitat backend will be a later, separate integration step.

## API endpoints

All `/api/v1/*` endpoints require `Authorization: Bearer <AMA_API_KEY>`.

- `GET /api/health` — health and version; no authentication.
- `GET /api/v1/status` — service phase, supported directives, backend mode, and device count.
- `GET /api/v1/devices` — list simulated devices.
- `POST /api/v1/devices` — create a simulated device.
- `DELETE /api/v1/devices/:endpointId` — remove a simulated device.
- `POST /api/v1/alexa/directive` — Alexa Smart Home API v3 directive entry point for the Lambda relay.

Example device creation body:

```json
{
  "endpointId": "ama-test-switch-01",
  "friendlyName": "AMA Test Switch",
  "description": "Local protocol test switch",
  "manufacturerName": "Alexa Manager Advanced",
  "modelName": "Simulation",
  "displayCategories": ["SWITCH"],
  "powerState": "OFF"
}
```

Supported categories are passed through to Alexa. Use valid Alexa display category values. Endpoint IDs must be unique and use Alexa-supported characters.

## Testing

Run `npm test` for local protocol tests and `npm run check` for syntax checks. Tests use temporary storage and do not touch the production `data` directory.

## Deployment notes

Keep the existing `/volume1/docker/alexa-manager-advanced/.env` and `data/` directory. Do not overwrite or delete either during an update. Back up the project folder before replacing application files.

The Compose project remains bound to `192.168.1.12:4100`; this build does not change router forwarding, reverse proxy, certificates, DNS, or firewall settings. Do not expose it publicly yet.

OAuth authorization remains fail-closed until the exact Amazon redirect hosts are confirmed and configured. Keep `OAUTH_CLIENT_ID` and `OAUTH_ALLOWED_REDIRECT_HOSTS` unchanged for now.

## Changelog

See `CHANGELOG.md`.
