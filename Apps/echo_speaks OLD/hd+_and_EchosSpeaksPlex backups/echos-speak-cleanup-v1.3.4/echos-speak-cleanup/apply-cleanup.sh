#!/bin/sh
set -eu

PROJECT="/volume1/docker/echos-speak"
BUNDLE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
STAMP=$(date +%Y%m%d-%H%M%S)
BACKUP="/volume1/docker/echos-speak-backup-${STAMP}"

if [ ! -d "$PROJECT" ]; then
  echo "ERROR: Project directory not found: $PROJECT" >&2
  exit 1
fi
if [ ! -f "$PROJECT/docker-compose.yml" ] || [ ! -f "$PROJECT/src/server.js" ]; then
  echo "ERROR: Expected Echos Speak project files were not found in $PROJECT" >&2
  exit 1
fi
for f in start.sh docker-compose.yml package.json package-lock.json CHANGELOG.md src/server.js; do
  if [ ! -f "$BUNDLE_DIR/$f" ]; then
    echo "ERROR: Bundle is missing $f" >&2
    exit 1
  fi
done

mkdir -p "$BACKUP/src"
for f in start.sh docker-compose.yml package.json package-lock.json CHANGELOG.md src/server.js src/hdPlusMakerProxy.js; do
  if [ -f "$PROJECT/$f" ]; then
    mkdir -p "$BACKUP/$(dirname "$f")"
    cp -p "$PROJECT/$f" "$BACKUP/$f"
  fi
done

echo "Backup created at: $BACKUP"

cp -p "$BUNDLE_DIR/start.sh" "$PROJECT/start.sh"
cp -p "$BUNDLE_DIR/docker-compose.yml" "$PROJECT/docker-compose.yml"
cp -p "$BUNDLE_DIR/package.json" "$PROJECT/package.json"
cp -p "$BUNDLE_DIR/package-lock.json" "$PROJECT/package-lock.json"
cp -p "$BUNDLE_DIR/CHANGELOG.md" "$PROJECT/CHANGELOG.md"
cp -p "$BUNDLE_DIR/src/server.js" "$PROJECT/src/server.js"
rm -f "$PROJECT/src/hdPlusMakerProxy.js"

cd "$PROJECT"

echo "Validating Docker Compose configuration..."
docker compose config -q

echo "Checking start.sh shell syntax..."
sh -n start.sh

echo "Building the updated image (Node.js runs inside the Docker image, not on the NAS host)..."
docker compose build --no-cache

echo "Checking server.js syntax with Node.js inside the newly built image..."
docker compose run --rm --no-deps --entrypoint node echos-speak --check /app/src/server.js

echo "Recreating Echos Speak with the updated configuration..."
docker compose up -d --force-recreate

echo "Container status:"
docker ps --filter name='^/echos-speak$'
echo "Recent logs:"
docker logs --tail 60 echos-speak

echo "Cleanup deployed. Backup retained at: $BACKUP"
