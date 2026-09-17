const alexaWrapper = require('./alexaWrapper');
const logger = require('./logger');

class CommandDispatcher {
  /**
   * Retrieves mapped list of Amazon Echo devices from alexa-remote2
   */
  getDevices(callback) {
    const alexa = alexaWrapper.alexa;
    if (!alexa || !alexa.devices) {
      return callback(new Error('Alexa instance not initialized or no devices loaded.'));
    }

    try {
      const deviceMap = Object.values(alexa.devices).map((dev) => ({
        serialNumber: dev.serialNumber,
        accountName: dev.accountName || dev.displayName || 'Echo Device',
        deviceFamily: dev.deviceFamily || 'UNKNOWN',
        deviceType: dev.deviceType || 'UNKNOWN',
        online: dev.online !== undefined ? dev.online : true,
        currentVolume: dev.volume !== undefined ? dev.volume : null,
      }));

      callback(null, deviceMap);
    } catch (err) {
      logger.error('[CommandDispatcher] Error mapping devices:', { error: err.message });
      callback(err);
    }
  }

  /**
   * Dispatches command to target serial number
   */
  dispatch(serialNumber, command, payload = {}, callback) {
    const alexa = alexaWrapper.alexa;
    if (!alexa || !alexa.devices) {
      return callback(new Error('Alexa instance not initialized.'));
    }

    const targetDevice = alexa.devices[serialNumber] || 
      Object.values(alexa.devices).find(d => d.serialNumber === serialNumber);

    if (!targetDevice && command !== 'announceAll') {
      return callback(new Error(`Device with serialNumber '${serialNumber}' not found.`));
    }

    logger.info(`[CommandDispatcher] Executing '${command}' for serial [${serialNumber}]`, { payload });

    switch (command) {
      case 'speak':
        if (!payload.text) return callback(new Error("Missing 'text' in payload for speak command."));
        alexa.sendSequenceCommand(serialNumber, 'speak', payload.text, callback);
        break;

      case 'announce':
      case 'announceIncludeVis':
        if (!payload.text) return callback(new Error("Missing 'text' in payload for announcement."));
        alexa.sendSequenceCommand(serialNumber, 'announcement', payload.text, callback);
        break;

      case 'announceAll':
        if (!payload.text) return callback(new Error("Missing 'text' in payload for announceAll."));
        const allSerials = Object.keys(alexa.devices);
        let completed = 0;
        let errors = [];
        allSerials.forEach((sNum) => {
          alexa.sendSequenceCommand(sNum, 'announcement', payload.text, (err) => {
            if (err) errors.push({ serialNumber: sNum, error: err.message });
            completed++;
            if (completed === allSerials.length) {
              if (errors.length > 0) return callback(null, { status: 'partial_success', errors });
              callback(null, { status: 'success' });
            }
          });
        });
        break;

      case 'volume':
        if (payload.level === undefined) return callback(new Error("Missing 'level' (0-100) in payload."));
        const vol = parseInt(payload.level, 10);
        alexa.setVolume(targetDevice, vol, callback);
        break;

      case 'volumeUp':
        const currentVolUp = targetDevice.volume !== undefined ? targetDevice.volume : 30;
        alexa.setVolume(targetDevice, Math.min(100, currentVolUp + 5), callback);
        break;

      case 'volumeDown':
        const currentVolDown = targetDevice.volume !== undefined ? targetDevice.volume : 30;
        alexa.setVolume(targetDevice, Math.max(0, currentVolDown - 5), callback);
        break;

      case 'play':
      case 'pause':
      case 'stop':
        alexa.sendCommand(targetDevice, command, callback);
        break;

      case 'nextTrack':
        alexa.sendCommand(targetDevice, 'next', callback);
        break;

      case 'previousTrack':
        alexa.sendCommand(targetDevice, 'previous', callback);
        break;

      case 'dnd':
        const dndState = payload.state === true || payload.state === 'true';
        alexa.setDoNotDisturb(targetDevice, dndState, callback);
        break;

      case 'raw':
        if (!payload.rawCommand) return callback(new Error("Missing 'rawCommand' in payload for raw execution."));
        alexa.sendSequenceCommand(serialNumber, payload.rawCommand, payload.rawPayload || null, callback);
        break;

      default:
        callback(new Error(`Command '${command}' is not supported by the bridge dispatcher.`));
        break;
    }
  }
}

module.exports = new CommandDispatcher();