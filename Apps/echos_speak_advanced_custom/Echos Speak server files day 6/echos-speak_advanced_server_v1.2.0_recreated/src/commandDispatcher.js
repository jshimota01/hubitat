const alexaWrapper = require('./alexaWrapper');
const logger = require('./logger');

class CommandDispatcher {

  getDevices(callback) {
    const alexa = alexaWrapper.alexa;

    if (!alexa || !alexa.serialNumbers) {
      return callback(
        new Error('Alexa instance not initialized or no devices loaded.')
      );
    }

    try {
      const deviceMap = Object.values(alexa.serialNumbers)
        .filter((dev) => dev.deviceFamily !== 'VOX')
        .map((dev) => ({
          serialNumber: dev.serialNumber,
          accountName: dev.accountName || dev.displayName || 'Echo Device',
          deviceFamily: dev.deviceFamily || 'UNKNOWN',
          deviceType: dev.deviceType || 'UNKNOWN',
          online: dev.online !== undefined ? dev.online : true,
          currentVolume: dev.volume !== undefined ? dev.volume : null,
        }));

      callback(null, deviceMap);

    } catch (err) {
      logger.error('[CommandDispatcher] Error mapping devices:', {
        error: err.message,
      });

      callback(err);
    }
  }

  dispatch(serialNumber, command, payload = {}, callback) {
    const alexa = alexaWrapper.alexa;

    if (!alexa || !alexa.serialNumbers) {
      return callback(new Error('Alexa instance not initialized.'));
    }

    const targetDevice =
      alexa.serialNumbers[serialNumber] ||
      Object.values(alexa.serialNumbers).find(
        (device) => device.serialNumber === serialNumber
      );

    if (!targetDevice && command !== 'announceAll') {
      return callback(
        new Error(
          `Device with serialNumber '${serialNumber}' not found.`
        )
      );
    }

    const invokeAlexa = (operation, send) => {
      const startedAt = Date.now();
      let finished = false;

      logger.info(
        `[CommandDispatcher] START '${operation}' for serial [${serialNumber}]`,
        { payload }
      );

      const finish = (err, result) => {
        const durationMs = Date.now() - startedAt;

        if (finished) {
          logger.warn(
            `[CommandDispatcher] LATE CALLBACK '${operation}' for serial [${serialNumber}] after timeout`,
            { durationMs, error: err ? (err.message || String(err)) : null }
          );
          return;
        }

        finished = true;
        clearTimeout(timeoutHandle);

        if (err) {
          logger.error(
            `[CommandDispatcher] CALLBACK ERROR '${operation}' for serial [${serialNumber}] after ${durationMs}ms`,
            {
              errorType: typeof err,
              errorName: err.name || null,
              errorCode: err.code || null,
              errorMessage: err.message || String(err)
            }
          );
        } else {
          logger.info(
            `[CommandDispatcher] CALLBACK SUCCESS '${operation}' for serial [${serialNumber}] after ${durationMs}ms`,
            {
              resultType: result === null ? 'null' : typeof result,
              hasResult: result !== undefined && result !== null
            }
          );
        }

        callback(err, result);
      };

      const timeoutHandle = setTimeout(() => {
        const durationMs = Date.now() - startedAt;
        const err = new Error(
          `Alexa command '${operation}' did not complete within 15000ms.`
        );
        err.code = 'COMMAND_TIMEOUT';

        if (finished) return;
        finished = true;

        logger.error(
          `[CommandDispatcher] TIMEOUT '${operation}' for serial [${serialNumber}] after ${durationMs}ms`,
          { errorCode: err.code }
        );

        callback(err);
      }, 15000);

      try {
        send(finish);
      } catch (err) {
        finish(err);
      }
    };

    // Command mappings below intentionally mirror alexa-remote2 v8.1.1.
    // In particular, sendCommand() requires a value argument before callback;
    // sendSequenceCommand() accepts the callback in its 4th argument because
    // it promotes a function supplied as overrideCustomerId.
    switch (command) {

      case 'speak':
        if (!payload.text) {
          return callback(
            new Error("Missing 'text' in payload for speak command.")
          );
        }

        invokeAlexa('speak', (done) => {
          alexa.sendSequenceCommand(
            serialNumber,
            'speak',
            payload.text,
            done
          );
        });
        break;


      case 'announce':
        if (!payload.text) {
          return callback(
            new Error("Missing 'text' in payload for announcement.")
          );
        }

        invokeAlexa('announce', (done) => {
          alexa.sendSequenceCommand(
            serialNumber,
            'announcement',
            payload.text,
            done
          );
        });
        break;


      case 'stop':
        // Proper Alexa device stop. Do not alias Stop to Pause.
        invokeAlexa('stop', (done) => {
          alexa.sendSequenceCommand(
            serialNumber,
            'deviceStop',
            null,
            done
          );
        });
        break;


      case 'announceAll': {
        if (!payload.text) {
          return callback(
            new Error("Missing 'text' in payload for announceAll.")
          );
        }

        // Use the same supported-device filter as discovery.
        // alexa-remote2 supports an array of serials for announcement targets,
        // which produces one properly formed AlexaAnnouncement sequence.
        const allSerials = Object.values(alexa.serialNumbers)
          .filter((dev) => dev.deviceFamily !== 'VOX')
          .map((dev) => dev.serialNumber);

        if (allSerials.length === 0) {
          return callback(
            new Error('No supported Alexa devices are available.')
          );
        }

        invokeAlexa('announceAll', (done) => {
          alexa.sendSequenceCommand(
            allSerials,
            'announcement',
            payload.text,
            done
          );
        });

        break;
      }


      case 'volume': {
        if (payload.level === undefined) {
          return callback(
            new Error("Missing 'level' (0-100) in payload.")
          );
        }

        const vol = Number(payload.level);

        if (!Number.isInteger(vol) || vol < 0 || vol > 100) {
          return callback(
            new Error(
              "Invalid 'level'. Volume must be a number from 0 to 100."
            )
          );
        }

        // Use Alexa Device Controls sequence rather than
        // the /api/np/command VolumeLevelCommand endpoint.
        invokeAlexa('volume', (done) => {
          alexa.sendSequenceCommand(
            serialNumber,
            'volume',
            vol,
            done
          );
        });

        break;
      }


      case 'volumeUp': {
        const currentVol =
          targetDevice.volume !== undefined &&
          targetDevice.volume !== null
            ? parseInt(targetDevice.volume, 10)
            : 30;

        const newVol = Math.min(
          100,
          (Number.isNaN(currentVol) ? 30 : currentVol) + 5
        );

        // Use the same Alexa Device Controls sequence path
        // as direct volume setting.
        invokeAlexa('volumeUp', (done) => {
          alexa.sendSequenceCommand(
            serialNumber,
            'volume',
            newVol,
            done
          );
        });

        break;
      }


      case 'volumeDown': {
        const currentVol =
          targetDevice.volume !== undefined &&
          targetDevice.volume !== null
            ? parseInt(targetDevice.volume, 10)
            : 30;

        const newVol = Math.max(
          0,
          (Number.isNaN(currentVol) ? 30 : currentVol) - 5
        );

        // Use the same Alexa Device Controls sequence path
        // as direct volume setting.
        invokeAlexa('volumeDown', (done) => {
          alexa.sendSequenceCommand(
            serialNumber,
            'volume',
            newVol,
            done
          );
        });

        break;
      }


      case 'play':
      case 'pause':
        invokeAlexa(command, (done) => {
          // alexa-remote2 v8.1.1 signature:
          // sendCommand(serialOrName, command, value, callback)
          alexa.sendCommand(
            serialNumber,
            command,
            null,
            done
          );
        });
        break;


      case 'nextTrack':
        invokeAlexa('nextTrack', (done) => {
          // alexa-remote2 v8.1.1 signature:
          // sendCommand(serialOrName, command, value, callback)
          alexa.sendCommand(
            serialNumber,
            'next',
            null,
            done
          );
        });
        break;


      case 'previousTrack':
        invokeAlexa('previousTrack', (done) => {
          // alexa-remote2 v8.1.1 signature:
          // sendCommand(serialOrName, command, value, callback)
          alexa.sendCommand(
            serialNumber,
            'previous',
            null,
            done
          );
        });
        break;


      case 'dnd': {
        const dndState =
          payload.state === true ||
          payload.state === 'true';

        invokeAlexa('dnd', (done) => {
          alexa.setDoNotDisturb(
            serialNumber,
            dndState,
            done
          );
        });

        break;
      }


      case 'raw':
        if (!payload.rawCommand) {
          return callback(
            new Error(
              "Missing 'rawCommand' in payload for raw execution."
            )
          );
        }

        invokeAlexa(`raw/${payload.rawCommand}`, (done) => {
          alexa.sendSequenceCommand(
            serialNumber,
            payload.rawCommand,
            Object.prototype.hasOwnProperty.call(payload, 'rawPayload')
              ? payload.rawPayload
              : null,
            done
          );
        });

        break;


      default:
        callback(
          new Error(
            `Command '${command}' is not supported by the bridge dispatcher.`
          )
        );
        break;
    }
  }
}

module.exports = new CommandDispatcher();