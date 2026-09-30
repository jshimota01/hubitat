# Echos Speak Advanced Server

## v1.2.0

- Corrected `alexa-remote2` v8.1.1 `sendCommand()` callback argument order for `play`, `pause`, `nextTrack`, and `previousTrack`.
- Kept `sendSequenceCommand()` mappings aligned with the v8.1.1 callback contract.
- Reworked `announceAll` to use the library-supported array target form and the same VOX exclusion used by discovery.
- Preserved the 15-second dispatcher timeout and command lifecycle logging for diagnostic visibility.
- Preserved proper `deviceStop` behavior; Stop is not aliased to Pause.
- Preserved raw sequence command support while retaining explicit `rawPayload` values such as `false` or `0`.
- Tightened volume validation to require an integer from 0 through 100.

## Deployment

The archive is intended to replace the server project files (except the persistent `data/session.json`).
After extraction into `/volume1/docker/echos-speak`, rebuild the image so the source changes are included:

```bash
docker compose down
docker compose build --no-cache
docker compose up -d
```
