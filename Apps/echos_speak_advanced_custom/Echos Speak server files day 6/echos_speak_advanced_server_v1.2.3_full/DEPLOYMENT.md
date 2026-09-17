# Echos Speak Advanced Server v1.2.3

## Included
- Dockerfile
- docker-compose.yml
- package.json
- package-lock.json
- CHANGELOG.md
- src/alexaWrapper.js
- src/authManager.js
- src/commandDispatcher.js
- src/logger.js
- src/server.js

## Important
The persistent Amazon session is intentionally NOT included in this ZIP. Your existing `data/session.json` contains the authenticated session and must remain on the Synology host.

The compose file mounts `./data` into `/app/data`, so the existing session is preserved across container rebuilds.

## Deploy on Synology
```bash
cd /volume1/docker/echos-speak
docker compose down
docker compose build --no-cache
docker compose up -d
docker ps
docker logs --tail 100 echos-speak
```

Do not delete the existing `data/session.json`.

## Current media command semantics
The server passes the short command names expected by alexa-remote2 v8.1.1:
- play
- pause
- next
- previous

The library converts these internally to the corresponding Amazon command types.

`stop` remains the alexa-remote2 `deviceStop` sequence pending the music-vs-video test.
