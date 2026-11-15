# Alexa Manager Advanced — NAS Foundation
Version 0.1.0 — foundation scaffold; not yet connected to Amazon or Hubitat.

This is an independent project. It does not mount, import, call, or share credentials/data with Echos Speak.

## Target
- Synology: `192.168.1.12`
- Project: `/volume1/docker/alexa-manager-advanced`
- Container: `alexa-manager-advanced`
- Port: `4100`, bound only to NAS LAN address `192.168.1.12`

## Included
- Node.js 20 / Express service in its own Docker project
- Security headers and structured JSON logging
- `GET /api/health`
- API-key-protected `GET /api/v1/status`
- Reserved protected `POST /api/v1/alexa/directive`
- Separate persistent data mount reserved for AMA-owned state

## Not enabled in this build
- Amazon OAuth/account linking
- Alexa directive processing
- AWS Lambda relay
- Hubitat app, drivers, or backend calls
- Public reverse proxy, TLS, router forwarding, or DNS changes

The directive endpoint deliberately returns HTTP 501. OAuth endpoints are not advertised until authorization-code flow, token persistence, client registration, and TLS requirements are implemented and tested.

## Deploy only when authorized
1. Copy this folder to `/volume1/docker/alexa-manager-advanced`.
2. Copy `.env.example` to `.env`; set `AMA_API_KEY` to a unique random secret. Never reuse Echos Speak credentials.
3. Run from the project directory:
   ```sh
   docker compose build
   docker compose up -d
   docker logs --tail 50 alexa-manager-advanced
   ```
4. Test from a LAN client:
   ```sh
   curl http://192.168.1.12:4100/api/health
   ```
5. Protected status:
   ```sh
   curl -H "Authorization: Bearer YOUR_AMA_API_KEY" http://192.168.1.12:4100/api/v1/status
   ```

Do not create a router forward or public reverse proxy yet. Validate on the LAN first.

## Security
- `.env` is excluded from the Docker build context.
- API key is unique to AMA.
- Container runs unprivileged, drops Linux capabilities, and has a read-only root filesystem.
- No public port is opened by this compose file.
- Do not commit `.env` or real tokens.

## Changelog
### 0.1.0 — Initial NAS foundation
- Independent Node.js/Express service scaffold.
- Health and protected status endpoints.
- Reserved directive route with explicit 501 response.
- Structured logs and baseline container hardening.
- Independent persistent data mount.
