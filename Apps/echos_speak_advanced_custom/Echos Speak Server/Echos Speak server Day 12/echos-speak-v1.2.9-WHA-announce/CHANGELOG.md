# Echos Speak Advanced Server

## v1.2.7

- Added `mute` and `unmute` bridge commands.
- `mute` uses the existing supported Alexa Device Controls volume sequence and sets volume to 0.
- `unmute` restores the volume level supplied by the Hubitat child driver.
- No undocumented/direct Alexa mute sequence is assumed.
- Preserved the existing dispatcher timeout, result validation, and command logging.


## v1.2.6

- Added Fire TV video-control test commands: `fireTVPauseVideo`, `fireTVResumeVideo`, `fireTVStartVideo`, and `fireTVStopVideo`.
- `fireTVPauseVideo` maps to `Alexa.Operation.FireTV.PauseVideo` through the authenticated Alexa Fire TV routine sequence path.
- `fireTVResumeVideo` and `fireTVStartVideo` map to `Alexa.Operation.FireTV.ResumeVideo` (Start is an explicit test alias for Resume).
- `fireTVStopVideo` uses the existing `deviceStop` sequence as a separate stop test; it is not aliased to Pause.
- Fire TV test commands require `deviceFamily == FIRE_TV` and a valid `deviceAccountId`.
- Preserved the existing 15-second timeout, Alexa result validation, and command lifecycle logging.

## v1.2.1

- Corrected Alexa result handling so application-level failures such as `No routes found` are not reported as successful commands.
- Added target diagnostics to command logs: device family/type, online state, controllability, music-player state, and capabilities.
- Extended `/api/devices` with non-sensitive capability/route metadata for troubleshooting.
- Corrected Amazon announcement content locale to `en-US` for the `amazon.com` deployment instead of relying on alexa-remote2 v8.1.1's hard-coded `de-DE` announcement locale.
- Kept the proper `AlexaAnnouncement` target structure and device serial/type identifiers.
- Preserved the corrected `sendCommand(serialOrName, command, value, callback)` contract.
- `ALEXA_COMMAND_FAILED` now returns HTTP 502 so callers can distinguish an Alexa rejection from a malformed bridge request.

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
After extraction into `/volume1/docker/echos-speak`, rebuild the image so the source changes are included.
