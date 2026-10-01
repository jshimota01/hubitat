from pathlib import Path

p = Path("src/commandDispatcher.js")
s = p.read_text()
old = r      alexa.sendCommand(
        serialNumber,
        command,
        () => {
          logger.info(
            `[CommandDispatcher] '${command}' Alexa callback received for serial [${serialNumber}]`
          );
        }
      );
new = r      alexa.sendCommand(
        serialNumber,
        command,
        null,
        (err, response) => {
          logger.info(
            `[CommandDispatcher] '${command}' Alexa callback received for serial [${serialNumber}]`,
            {
              hasError: !!err,
              errorCode: err?.code || null,
              errorMessage: err?.message || null,
              responseType: response ? typeof response : null,
              response:
                response && typeof response === 'object'
                  ? response
                  : response ?? null
            }
          );
        }
      );
if old not in s:
    raise SystemExit("ERROR: Expected original sendCommand block was not found. No changes made.")
backup=p.with_name(p.name+'.pre-play-callback-diagnostic')
if not backup.exists():
    backup.write_text(s)
p.write_text(s.replace(old,new,1))
print('SUCCESS: play callback diagnostic patch applied.')
print(f'Backup: {backup}')
