# Alexa Manager Advanced — NAS Foundation

Version 0.2.0. This is an independent service and does not share code runtime, data, credentials, ports, containers, or services with Echos Speak.

## Current scope

- Node.js 24 / npm 12 container.
- Express API and health endpoint at `/api/health`.
- API-key protected status endpoint at `/api/v1/status`.
- OAuth authorization-code foundation at `/oauth/authorize` and `/oauth/token`.
- PKCE S256, short-lived single-use authorization codes, one-hour access tokens, and rotating 180-day refresh tokens.
- Hashed authorization-code and token records persisted in `./data/oauth-store.json`.
- OAuth authorization is fail-closed until redirect hosts are explicitly configured.

The Alexa directive endpoint remains HTTP 501. This build is not yet an Alexa Smart Home skill endpoint and must not be exposed to the internet.

## Deployment notes

Keep the existing `/volume1/docker/alexa-manager-advanced/.env` and `data/` directory. Do not overwrite or delete either during an update. Back up the project folder before replacing application files.

The Compose project remains bound to `192.168.1.12:4100`; this build does not change router forwarding, reverse proxy, certificates, DNS, or firewall settings.

Set unique values for `OAUTH_USERNAME` and `OAUTH_PASSWORD` in `.env` before testing the login flow. Keep `OAUTH_CLIENT_ID` and `OAUTH_ALLOWED_REDIRECT_HOSTS` empty for now. We will fill those only after confirming Amazon's exact skill client and redirect configuration. Do not put Amazon redirect domains in the allow-list based on guesses.

Relevant `.env` entries:

```dotenv
OAUTH_USERNAME=your-private-login-name
OAUTH_PASSWORD=use-a-long-unique-random-password
OAUTH_CLIENT_ID=
OAUTH_ALLOWED_REDIRECT_HOSTS=
```

The credentials above are for the AMA local sign-in page, not Amazon credentials. Do not reuse your NAS, Microsoft, or Amazon password.

## Local test endpoints

- `GET http://192.168.1.12:4100/api/health`
- `GET http://192.168.1.12:4100/api/v1/status` with `Authorization: Bearer <AMA_API_KEY>`

OAuth authorization will return `invalid_request` until the redirect host allow-list is configured. That is intentional. No public access is enabled in this milestone.

## Changelog

See `CHANGELOG.md`.
