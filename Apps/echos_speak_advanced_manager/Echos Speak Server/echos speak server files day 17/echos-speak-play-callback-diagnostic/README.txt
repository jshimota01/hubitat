Echos Speak v1.3.3 - Play Callback Diagnostic

Purpose
-------
This is a temporary diagnostic patch only. It fixes the argument position of the alexa-remote2 sendCommand callback so the server can log Amazon's actual HTTP callback/error for play/pause.

It does NOT change the PlayCommand payload. The value argument is explicitly null because play does not consume a value in alexa-remote2 8.1.1.

Install
-------
1. Put apply_play_callback_diagnostic.py in /volume1/docker/echos-speak/
2. From that directory run:

   python3 apply_play_callback_diagnostic.py

3. Verify syntax:

   docker compose build --no-cache

   docker compose up -d

4. Test Office Echo Left:

   curl -i --max-time 15 \
     -X POST \
     http://127.0.0.1:4000/api/command \
     -H "Content-Type: application/json" \
     --data '{"serialNumber":"G6G22N063482007T","command":"play","payload":{}}'

5. Get the relevant log:

   docker logs --tail 150 echos-speak | grep -E "play|Alexa callback|CommandDispatcher"

Safety
------
The script makes a backup named src/commandDispatcher.js.pre-play-callback-diagnostic before changing the file. It refuses to patch if the expected original block is not found.

Restore
-------
If you want to restore the exact pre-patch file:

   cp src/commandDispatcher.js.pre-play-callback-diagnostic src/commandDispatcher.js

Then rebuild/restart normally.

Baseline
--------
This patch does not modify the v1.3.3 baseline image/archive. It is intended as a temporary experiment only.
