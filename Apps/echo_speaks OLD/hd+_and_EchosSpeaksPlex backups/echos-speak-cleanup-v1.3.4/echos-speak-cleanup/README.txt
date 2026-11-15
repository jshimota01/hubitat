ECHOS SPEAK CLEANUP — VERSION 1.3.4

Changes:
- Stops starting src/hdPlusMakerProxy.js.
- Removes the HD+ proxy's NAS port 80 and 443 mappings from docker-compose.yml.
- Removes the abandoned /debug/audioplayer-test Plex experiment route.
- Does not alter Alexa command dispatch, voiceAsText behavior, or timeout settings.
- Preserves the existing /volume1/docker/echos-speak/data directory and Amazon session.

INSTALLATION
1. In Synology File Station, create /volume1/docker/echos-speak-cleanup.
2. Upload this ZIP there and extract it. The extracted folder should be:
   /volume1/docker/echos-speak-cleanup/echos-speak-cleanup/
3. In PuTTY, run:
   sudo sh /volume1/docker/echos-speak-cleanup/echos-speak-cleanup/apply-cleanup.sh
4. Review the output. The script makes a timestamped backup before changing project files.

The script uses Docker's Node 20 image to validate src/server.js. It does not expect Node.js to be installed on the Synology host or inside the existing container before rebuilding.

Rollback files are saved under /volume1/docker/echos-speak-backup-YYYYMMDD-HHMMSS. The script does not delete the backup or the persistent data/session.json.

Expected result:
- Echos Speak API remains on 192.168.1.22:4000.
- Amazon login proxy remains on 192.168.1.22:4001.
- Ports 80 and 443 are no longer published by this container.
