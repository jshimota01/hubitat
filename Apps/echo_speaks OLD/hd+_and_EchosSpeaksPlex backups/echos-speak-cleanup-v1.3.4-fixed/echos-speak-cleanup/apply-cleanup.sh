#!/bin/sh
set -eu

MAIN="/volume1/docker/echos-speak"
BACKUP_ROOT="/volume1/docker/echos-speak-cleanup-backups"
HDPLUS="/volume1/docker/hdplus-proxy"
PLEX="/volume1/docker/echos-speak-plex-bridge"

# Resolve the bundle directory from this script, not the current working directory.
SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
BUNDLE="$SCRIPT_DIR"

echo "Bundle directory: $BUNDLE"
for f in start.sh docker-compose.yml src/server.js package.json package-lock.json; do
  if [ ! -f "$BUNDLE/$f" ]; then
    echo "ERROR: Bundle is missing $f at $BUNDLE/$f" >&2
    echo "Extract the ZIP so apply-cleanup.sh and src/server.js are in the same bundle directory." >&2
    exit 1
  fi
done

[ -d "$MAIN/src" ] || { echo "ERROR: Main project src directory not found: $MAIN/src" >&2; exit 1; }
[ -f "$MAIN/src/server.js" ] || { echo "ERROR: Main server source not found: $MAIN/src/server.js" >&2; exit 1; }
[ -f "$MAIN/docker-compose.yml" ] || { echo "ERROR: Main docker-compose.yml not found" >&2; exit 1; }

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="$BACKUP_ROOT/$STAMP"
mkdir -p "$BACKUP/main/src"

echo "Creating backup at $BACKUP"
cp -p "$MAIN/start.sh" "$MAIN/docker-compose.yml" "$MAIN/package.json" "$MAIN/package-lock.json" "$MAIN/CHANGELOG.md" "$BACKUP/main/"
cp -p "$MAIN/src/server.js" "$BACKUP/main/src/server.js"
if [ -f "$MAIN/src/hdPlusMakerProxy.js" ]; then cp -p "$MAIN/src/hdPlusMakerProxy.js" "$BACKUP/main/src/hdPlusMakerProxy.js"; fi
if [ -d "$HDPLUS" ]; then
  tar -czf "$BACKUP/hdplus-proxy.tar.gz" -C /volume1/docker hdplus-proxy
fi
if [ -d "$PLEX" ]; then
  tar -czf "$BACKUP/echos-speak-plex-bridge.tar.gz" -C /volume1/docker echos-speak-plex-bridge
fi

echo "Installing corrected main project files"
cp -p "$BUNDLE/start.sh" "$MAIN/start.sh"
cp -p "$BUNDLE/docker-compose.yml" "$MAIN/docker-compose.yml"
cp -p "$BUNDLE/src/server.js" "$MAIN/src/server.js"
[ ! -f "$MAIN/src/hdPlusMakerProxy.js" ] || rm -f "$MAIN/src/hdPlusMakerProxy.js"
cp -p "$BUNDLE/CHANGELOG.md" "$MAIN/CHANGELOG.md"

echo "Validating Docker Compose configuration"
sudo docker compose -f "$MAIN/docker-compose.yml" config >/dev/null

echo "Checking JavaScript syntax using Node.js inside the existing Docker image"
IMAGE="$(sudo docker inspect --format '{{.Config.Image}}' echos-speak)"
[ -n "$IMAGE" ] || { echo "ERROR: Could not determine existing echos-speak image" >&2; exit 1; }
sudo docker run --rm --entrypoint node -v "$MAIN":/app -w /app "$IMAGE" --check /app/src/server.js

echo "Rebuilding and recreating Echos Speak"
sudo docker compose -f "$MAIN/docker-compose.yml" up -d --build

echo "Waiting briefly for container status"
sleep 5
if [ "$(sudo docker inspect --format '{{.State.Running}}' echos-speak 2>/dev/null || true)" != "true" ]; then
  echo "ERROR: Echos Speak container is not running. Backups remain at $BACKUP" >&2
  sudo docker logs --tail 80 echos-speak 2>&1 || true
  echo "Do NOT delete the separate project folders. Review logs and restore from backup if needed." >&2
  exit 1
fi

echo "Checking published ports"
sudo docker ps --filter name='^/echos-speak$' --format 'table {{.Names}}\t{{.Ports}}'

echo "Recent Echos Speak logs:"
sudo docker logs --tail 40 echos-speak 2>&1 || true

echo "Removing the two abandoned project folders (backups are preserved)"
[ ! -d "$HDPLUS" ] || sudo rm -rf "$HDPLUS"
[ ! -d "$PLEX" ] || sudo rm -rf "$PLEX"

echo
echo "Cleanup complete."
echo "Backup location: $BACKUP"
echo "Preserved: $MAIN/data"
echo "Timeout settings and voiceAsText logic were not changed."
