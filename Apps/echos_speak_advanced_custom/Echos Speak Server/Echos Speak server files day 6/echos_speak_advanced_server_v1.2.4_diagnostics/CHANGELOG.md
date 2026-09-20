# Changelog

## 1.2.4
- Added temporary authenticated media diagnostics endpoint: GET /api/debug/media/:serialNumber.
- Queries alexa-remote2 media state (/api/media/state), player info (/api/np/player), and the established private media-session endpoint (/api/np/list-media-sessions).
- No playback command semantics were changed.
- Purpose: compare the active Echo Show music and video session metadata before implementing video-specific control.

## 1.2.3
- Restored alexa-remote2 short media command names (play/pause/next/previous).
