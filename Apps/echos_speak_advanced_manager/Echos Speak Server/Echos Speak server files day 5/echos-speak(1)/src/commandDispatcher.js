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

    logger.info(
      `[CommandDispatcher] Executing '${command}' for serial [${serialNumber}]`,
      { payload }
    );

    switch (command) {

      case 'speak':
        if (!payload.text) {
          return callback(
            new Error("Missing 'text' in payload for speak command.")
          );
        }

        alexa.sendSequenceCommand(
          serialNumber,
          'speak',
          payload.text,
          callback
        );
        break;


      case 'announce':
      case 'announceIncludeVis':
        if (!payload.text) {
          return callback(
            new Error("Missing 'text' in payload for announcement.")
          );
        }

        alexa.sendSequenceCommand(
          serialNumber,
          'announcement',
          payload.text,
          callback
        );
        break;


      case 'announceAll': {
        if (!payload.text) {
          return callback(
            new Error("Missing 'text' in payload for announceAll.")
          );
        }

        const allSerials = Object.keys(alexa.serialNumbers);

        if (allSerials.length === 0) {
          return callback(
            new Error('No Alexa devices are available.')
          );
        }

        let completed = 0;
        const errors = [];

        allSerials.forEach((sNum) => {
          alexa.sendSequenceCommand(
            sNum,
            'announcement',
            payload.text,
            (err) => {
              if (err) {
                errors.push({
                  serialNumber: sNum,
                  error: err.message,
                });
              }

              completed++;

              if (completed === allSerials.length) {
                if (errors.length > 0) {
                  return callback(null, {
                    status: 'partial_success',
                    errors,
                  });
                }

                callback(null, {
                  status: 'success',
                });
              }
            }
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

        const vol = parseInt(payload.level, 10);

        if (Number.isNaN(vol) || vol < 0 || vol > 100) {
          return callback(
            new Error(
              "Invalid 'level'. Volume must be a number from 0 to 100."
            )
          );
        }

        // Use Alexa Device Controls sequence rather than
        // the /api/np/command VolumeLevelCommand endpoint.
        alexa.sendSequenceCommand(
          serialNumber,
          'volume',
          vol,
          callback
        );

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
        alexa.sendSequenceCommand(
          serialNumber,
          'volume',
          newVol,
          callback
        );

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
        alexa.sendSequenceCommand(
          serialNumber,
          'volume',
          newVol,
          callback
        );

        break;
      }


      case 'play':
      case 'pause':
        alexa.sendCommand(
          serialNumber,
          command,
          callback
        );
        break;


      case 'nextTrack':
        alexa.sendCommand(
          serialNumber,
          'next',
          callback
        );
        break;


      case 'previousTrack':
        alexa.sendCommand(
          serialNumber,
          'previous',
          callback
        );
        break;


      case 'dnd': {
        const dndState =
          payload.state === true ||
          payload.state === 'true';

        alexa.setDoNotDisturb(
          serialNumber,
          dndState,
          callback
        );

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

        alexa.sendSequenceCommand(
          serialNumber,
          payload.rawCommand,
          payload.rawPayload || null,
          callback
        );

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