# Alexa Manager Advanced — NAS Service

**Package version: 0.5.3**  
**Service address: `http://192.168.1.23:4100`**  
**Hubitat platform:** local Hubitat C-7 integration  
**InfluxDB address:** `http://192.168.1.12:8086` (separate service; do not confuse this with the AMA NAS address)

AMA NAS is a standalone local service and remains completely separate from Echos Speak. Hubitat remains the source of truth for device state and commands.

## Version 0.5.3 changes

- Corrects `src/config.js` so the runtime-reported `appVersion` is `0.5.3`, matching `package.json`, this README, and the changelog.
- Documents the actual NAS bind address and Hubitat base URL: `192.168.1.23:4100`.
- Includes the `data/` directory as a versioned data snapshot, including the device registry and OAuth store present when this package was assembled.
- Clarifies the difference between the packaged data snapshot and the live data on the NAS during an update.
- Excludes the live `.env` file from the archive. Use `.env.example` as a template and retain the NAS's existing `.env` separately.

## What is in this archive

- Application source under `src/`, including the Hubitat event ingestion endpoint.
- `package.json`, `Dockerfile`, `docker-compose.yml`, and `.dockerignore`.
- `test/` and the release changelog.
- `data/devices.json` and `data/oauth-store.json` as the versioned snapshot accompanying this release.
- `.env.example` as the configuration template.
- Reverse-proxy reference backups. These are reference files, not automatic deployment instructions.

The archive intentionally does **not** include `.env`, because it contains live API keys, OAuth credentials, and InfluxDB tokens. The data snapshot does include OAuth token-store contents and must be treated as sensitive. Keep the archive in private, access-controlled storage; do not publish it or place it in a public source repository.

## Updating the existing Synology NAS installation

1. Back up the current AMA project directory, including its `.env` and `data/` directory.
2. Extract this release to a staging directory and review the files before copying them into the live project.
3. Copy the application source, package metadata, Dockerfile, and documentation into the existing project directory.
4. Keep the existing live `.env`. Do not replace it with `.env.example`; use `.env.example` only to identify newly introduced settings.
5. The archive includes a point-in-time `data/` snapshot for version control and recovery. **Do not blindly overwrite the live `data/` directory during a routine upgrade.** Preserve the live OAuth store and device registry unless you deliberately intend to restore the packaged snapshot. If restoring, stop the service first and make a separate backup of the current live data.
6. Review `docker-compose.yml` and retain the intended local bind address `192.168.1.23:4100`.
7. Rebuild and recreate the existing container using the normal project deployment procedure. Do not create a second container or change the existing container identity.
8. Verify `GET /api/health` and `GET /api/v1/status`; both should report application version `0.5.3`.

The package contains data so the release is reproducible and recoverable. The update procedure preserves live data so an ordinary source update cannot roll back current OAuth sessions or device registrations. These are complementary rules, not a contradiction.

## Hubitat configuration

Set the AMA NAS Base URL in the Hubitat Parent application to:

`http://192.168.1.23:4100`

Use the existing AMA API key from the NAS `.env` file. Do not put that key into source code or the archive. The Hubitat app uses Hubitat's local app endpoint and access token for callbacks; do not forward that endpoint through the router or expose it to the internet.

## Network addresses

| Service | Address | Purpose |
|---|---|---|
| AMA NAS | `192.168.1.23:4100` | AMA service; Docker host bind |
| InfluxDB | `192.168.1.12:8086` | Separate InfluxDB service |
| Hubitat | Local Hubitat C-7 address | Hubitat device state and command authority |

The InfluxDB URL remains `http://192.168.1.12:8086`; that address is correct for InfluxDB and is not the AMA NAS service address.

## API changes in 0.5.2

All `/api/v1/*` routes require `Authorization: Bearer <AMA_API_KEY>`.

- `POST /api/v1/telemetry/hubitat-event` accepts a JSON envelope containing `event`, `timestamp`, and a `payload` object.
- The NAS normalizes known device properties (Hubitat device ID, endpoint ID, device name, attribute, value, power state, and event timestamp) into InfluxDB tags/fields and retains the original payload as JSON.
- Hubitat telemetry forwarding is parent-mediated. Hubitat-side InfluxDB reporting is not routed through the NAS.

## NAS telemetry configuration

The NAS service writes directly to InfluxDB 2.x. It does not route telemetry through Hubitat.

Configure these variables in the existing NAS `.env` file:

- `INFLUX_ENABLED=false` initially, unless continuous NAS telemetry is intentionally enabled.
- `INFLUX_URL=http://192.168.1.12:8086`
- `INFLUX_ORG=DundeeHome`
- `INFLUX_BUCKET=HubAlexa`
- `INFLUX_TOKEN=` the actual NAS writer token.

The NAS test endpoint is independent of `INFLUX_ENABLED`:

- `POST /api/v1/telemetry/test` with the existing AMA API key in the Authorization header.
- `GET /api/v1/telemetry/status` reports configuration and in-memory write status.

A successful test returns HTTP 200 with `status: SUCCESS`; a failed test returns HTTP 502 with `status: FAILED` and an error message. Continuous telemetry remains disabled until `INFLUX_ENABLED=true`.

## Testing

Run:

```sh
npm run check
npm test
```

After deployment, verify the service health and version endpoints before enabling continuous telemetry.
