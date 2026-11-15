ECHOS SPEAK CLEANUP — corrected bundle

This package is built around the actual Echos Speak project layout:
  /volume1/docker/echos-speak
and the two separate abandoned project folders:
  /volume1/docker/hdplus-proxy
  /volume1/docker/echos-speak-plex-bridge

INSTALL
1. In File Station, extract this ZIP into:
   /volume1/docker/echos-speak-cleanup
   Ensure the resulting files are inside:
   /volume1/docker/echos-speak-cleanup/echos-speak-cleanup/
2. Run in PuTTY:
   sudo sh /volume1/docker/echos-speak-cleanup/echos-speak-cleanup/apply-cleanup.sh

The script backs up the main project files and both abandoned project folders,
checks bundle paths, validates compose, checks JavaScript syntax using Node
inside the existing Docker image, rebuilds/recreates Echos Speak, and only
then removes the abandoned folders. It does not require Node.js on the NAS.
It preserves /volume1/docker/echos-speak/data and does not change timeout logic.
