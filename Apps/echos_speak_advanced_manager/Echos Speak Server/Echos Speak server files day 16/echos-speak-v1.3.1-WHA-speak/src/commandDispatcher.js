const alexaWrapper = require('./alexaWrapper');
const logger = require('./logger');

class CommandDispatcher {

  constructor() {
    // Coalesce concurrent reads for the same device so duplicate Hubitat
    // requests do not create duplicate Amazon/alexa-remote2 work.
    this.inFlightFeatureRequests = new Map();
    this.inFlightPlaybackRequests = new Map();

    // A1: Coalesce rapid volume changes per physical Echo.
    // Each Echo has an independent pending target and 1200ms quiet period.
    this.volumeBurstStates = new Map();
    this.volumeTargets = new Map();

    // Latest logical volume transaction for each physical Echo.
    // This is the authoritative volume command state exposed
    // through getDevices().
    this.volumeCommandStates = new Map();
  }

  _beginInFlight(map, key, callback) {
    const existing = map.get(key);

    if (existing) {
      existing.push(callback);
      return false;
    }

    map.set(key, [callback]);
    return true;
  }

  _finishInFlight(map, key, err, result) {
    const callbacks = map.get(key) || [];
    map.delete(key);

    callbacks.forEach(cb => {
      try {
        cb(err, result);
      } catch (callbackErr) {
        logger.error(
          `[CommandDispatcher] Consumer callback threw for in-flight operation [${key}]`,
          {
            error: callbackErr.message || String(callbackErr)
          }
        );
      }
    });
  }

  _getInitialVolume(targetDevice, serialNumber) {
    if (this.volumeTargets.has(serialNumber)) {
      return this.volumeTargets.get(serialNumber);
    }

    const deviceVolume =
      targetDevice &&
      targetDevice.volume !== undefined &&
      targetDevice.volume !== null
        ? parseInt(targetDevice.volume, 10)
        : 30;

    const initial = Number.isNaN(deviceVolume)
      ? 30
      : deviceVolume;

    this.volumeTargets.set(serialNumber, initial);

    return initial;
  }

  _beginVolumeTransaction(serialNumber, target) {
    const commandId =
      `vol-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

    const state = {
      volumeCommandState: 'pending',
      volumeCommandTarget: target,
      volumeCommandId: commandId,
      volumeCommandUpdatedAt: Date.now(),
      volumeCommandError: null
    };

    this.volumeCommandStates.set(serialNumber, state);

    logger.info(
      `[CommandDispatcher] VOLUME TRANSACTION PENDING for serial [${serialNumber}]`,
      {
        commandId,
        target
      }
    );

    return commandId;
  }

  _completeVolumeTransaction(serialNumber, commandId, err) {
    const state = this.volumeCommandStates.get(serialNumber);

    // A newer volume transaction has already replaced this one.
    // Never allow a late Amazon callback from the older transaction
    // to modify the newer transaction's state.
    if (!state || state.volumeCommandId !== commandId) {
      logger.warn(
        `[CommandDispatcher] Ignoring stale volume transaction callback for serial [${serialNumber}]`,
        {
          commandId,
          currentCommandId: state
            ? state.volumeCommandId
            : null
        }
      );
      return;
    }

    state.volumeCommandState = err ? 'failed' : 'success';
    state.volumeCommandUpdatedAt = Date.now();
    state.volumeCommandError = err
      ? (err.message || String(err))
      : null;

    logger.info(
      `[CommandDispatcher] VOLUME TRANSACTION ${state.volumeCommandState.toUpperCase()} for serial [${serialNumber}]`,
      {
        commandId,
        target: state.volumeCommandTarget,
        error: state.volumeCommandError
      }
    );
  }

  _queueVolumeBurst(serialNumber, target, sendFinal, callback) {
    const QUIET_PERIOD_MS = 1200;

    let state = this.volumeBurstStates.get(serialNumber);

    if (!state) {
      state = {
        pendingTarget: null,
        timer: null,
        sending: false,
        activeCallbacks: [],
        pendingCallbacks: [],
        activeTransactionId: null,
        pendingTransactionId: null
      };

      this.volumeBurstStates.set(serialNumber, state);
    }

    // Every new request replaces the target for the current
    // pending burst. The newest target always wins.
    state.pendingTarget = target;
    state.pendingCallbacks.push(callback);

    if (state.sending) {
      // Amazon is currently executing another physical command.
      // This starts the next logical transaction.
      if (!state.pendingTransactionId) {
        state.pendingTransactionId =
          this._beginVolumeTransaction(serialNumber, target);
      } else {
        const pendingState =
          this.volumeCommandStates.get(serialNumber);

        if (
          pendingState &&
          pendingState.volumeCommandId === state.pendingTransactionId
        ) {
          pendingState.volumeCommandTarget = target;
          pendingState.volumeCommandUpdatedAt = Date.now();
          pendingState.volumeCommandError = null;
        }
      }
    } else {
      // No physical command is running. This request belongs to
      // the current burst.
      if (!state.activeTransactionId) {
        state.activeTransactionId =
          this._beginVolumeTransaction(serialNumber, target);
      } else {
        const activeState =
          this.volumeCommandStates.get(serialNumber);

        if (
          activeState &&
          activeState.volumeCommandId === state.activeTransactionId
        ) {
          activeState.volumeCommandTarget = target;
          activeState.volumeCommandUpdatedAt = Date.now();
          activeState.volumeCommandError = null;
        }
      }
    }

    // Any new request restarts the 800ms quiet period.
    if (state.timer) {
      clearTimeout(state.timer);
      state.timer = null;
    }

    const flush = () => {
      state.timer = null;

      // A physical Amazon command is already in flight.
      // Do not interfere with it. Any requests received while
      // it is running remain pending and become a separate burst.
      if (state.sending) {
        return;
      }

      const finalTarget = state.pendingTarget;
      const transactionId = state.activeTransactionId;

      if (finalTarget === null || finalTarget === undefined) {
        if (
          !state.sending &&
          !state.timer &&
          state.activeCallbacks.length === 0 &&
          state.pendingCallbacks.length === 0
        ) {
          this.volumeBurstStates.delete(serialNumber);
        }
        return;
      }

      // Move this burst's callbacks to the active command.
      // Requests arriving after this point belong to the next burst.
      const callbacks = state.pendingCallbacks;
      state.activeCallbacks = callbacks;
      state.pendingCallbacks = [];
      state.pendingTarget = null;
      state.sending = true;

      const transactionState =
        this.volumeCommandStates.get(serialNumber);

      if (
        transactionState &&
        transactionState.volumeCommandId === transactionId
      ) {
        transactionState.volumeCommandTarget = finalTarget;
        transactionState.volumeCommandUpdatedAt = Date.now();
      }

      logger.info(
        `[CommandDispatcher] VOLUME BURST FLUSH for serial [${serialNumber}]`,
        {
          finalTarget,
          callbackCount: callbacks.length
        }
      );

      sendFinal(
        finalTarget,
        (err, result) => {
          state.sending = false;
          state.activeCallbacks = [];

          this._completeVolumeTransaction(
            serialNumber,
            transactionId,
            err
          );

          // Complete only the callbacks belonging to the physical
          // command that just finished.
          callbacks.forEach(cb => {
            try {
              cb(err, result);
            } catch (callbackErr) {
              logger.error(
                `[CommandDispatcher] Volume burst callback threw for serial [${serialNumber}]`,
                {
                  error: callbackErr.message || String(callbackErr)
                }
              );
            }
          });

          // If new volume input arrived while the Amazon command
          // was running, it is a NEW burst. Wait for 800ms of
          // silence before sending its newest target.
          if (
            state.pendingTarget !== null &&
            state.pendingTarget !== undefined
          ) {
            state.activeTransactionId =
              state.pendingTransactionId;
            state.pendingTransactionId = null;

            if (!state.timer) {
              state.timer = setTimeout(flush, QUIET_PERIOD_MS);
            }
            return;
          }

          // Nothing else is pending. This burst is completely done.
          if (
            !state.sending &&
            !state.timer &&
            state.pendingCallbacks.length === 0
          ) {
            this.volumeBurstStates.delete(serialNumber);
          }
        }
      );
    };

    state.timer = setTimeout(flush, QUIET_PERIOD_MS);
  }

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
          ...dev,
          accountName:
            dev.accountName ||
            dev.displayName ||
            'Echo Device',
          deviceFamily:
            dev.deviceFamily ||
            'UNKNOWN',
          deviceType:
            dev.deviceType ||
            'UNKNOWN',
          online:
            dev.online !== undefined
              ? dev.online
              : true,
          currentVolume:
            dev.volume !== undefined
              ? dev.volume
              : null,

          // Latest logical volume transaction for this Echo.
          ...(this.volumeCommandStates.get(dev.serialNumber) || {}),
        }));

      callback(null, deviceMap);

    } catch (err) {
      logger.error('[CommandDispatcher] Error mapping devices:', {
        error: err.message,
      });

      callback(err);
    }
  }


  getDeviceFeatures(serialNumber, callback) {
    const alexa = alexaWrapper.alexa;

    if (!alexa || !alexa.serialNumbers) {
      return callback(new Error('Alexa instance not initialized.'));
    }

    const targetDevice =
      alexa.serialNumbers[serialNumber] ||
      Object.values(alexa.serialNumbers).find(
        (device) => device.serialNumber === serialNumber
      );

    if (!targetDevice) {
      return callback(
        new Error(`Device with serialNumber '${serialNumber}' not found.`)
      );
    }

    if (
      !this._beginInFlight(
        this.inFlightFeatureRequests,
        serialNumber,
        callback
      )
    ) {
      logger.debug(
        `[CommandDispatcher] Coalescing concurrent getDeviceFeatures request for [${serialNumber}]`
      );
      return;
    }

    const complete = (err, result) => {
      this._finishInFlight(
        this.inFlightFeatureRequests,
        serialNumber,
        err,
        result
      );
    };

    const caps = Array.isArray(targetDevice.capabilities)
      ? targetDevice.capabilities
      : [];

    // CAPABILITIES = what Alexa says the device supports.
    const capabilities = {
      // Normalized capability names used by the Hubitat Parent.
      // The server is the authoritative Amazon -> capability mapping.
      temperatureMeasurement:
        caps.includes('TEMPERATURE_SENSOR'),

      motionSensor:
        caps.includes('MOTION_DETECTION'),

      displayBrightness:
        caps.includes('DISPLAY_BRIGHTNESS_ADJUST'),

      musicPlayer:
        caps.includes('AUDIO_PLAYER'),

      audioVolume:
        caps.includes('VOLUME_SETTING'),

      speechSynthesis:
        caps.includes('ALEXA_VOICE'),

      audioNotification:
        caps.includes('TIMERS_ALARMS_NOTIFICATIONS_VOLUME') ||
        (String(targetDevice.deviceFamily || '').toUpperCase() === 'WHA' && Array.isArray(targetDevice.clusterMembers) && targetDevice.clusterMembers.length > 0),

      notification:
        caps.includes('TIMERS_ALARMS_NOTIFICATIONS_VOLUME'),

      // Echo-specific feature flags.
      display:
        caps.includes('DISPLAY_POWER_TOGGLE') ||
        caps.includes('DISPLAY_BRIGHTNESS_ADJUST') ||
        caps.includes('DISPLAY_ADAPTIVE_BRIGHTNESS'),

      adaptiveBrightness:
        caps.includes('DISPLAY_ADAPTIVE_BRIGHTNESS'),
    };

    // STATE = current values. Unsupported capabilities remain null.
    const state = {
      temperature: null,
      temperatureScale: null,
      motion: null,
      displayBrightness: null,
      displayPower: null,
      adaptiveBrightness: null,
    };

    // ERRORS = supported capabilities whose current state could not be read.
    const errors = {};

    const getSetting = (
      capability,
      methodName,
      propertyName,
      transform,
      next
    ) => {
      if (!caps.includes(capability)) {
        return next();
      }

      if (typeof alexa[methodName] !== 'function') {
        errors[propertyName] =
          `Installed alexa-remote2 does not expose ${methodName}().`;
        return next();
      }

      try {
        alexa[methodName](serialNumber, (err, value) => {
          if (err) {
            errors[propertyName] = err.message || String(err);
          } else {
            state[propertyName] = transform ? transform(value) : value;
          }
          next();
        });
      } catch (err) {
        errors[propertyName] = err.message || String(err);
        next();
      }
    };

    const finish = () => {
      complete(null, {
        serialNumber: targetDevice.serialNumber,
        capabilities,
        state,
        errors,
      });
    };

    const readSensors = () => {
      const wantsTemperature = capabilities.temperatureMeasurement === true;
      const wantsMotion = capabilities.motionSensor === true;

      if (!wantsTemperature && !wantsMotion) {
        return finish();
      }

      this.getSensorState(
        targetDevice,
        wantsTemperature,
        wantsMotion,
        (err, sensorState) => {
          if (err) {
            if (wantsTemperature) {
              errors.temperature = err.message || String(err);
            }
            if (wantsMotion) {
              errors.motion = err.message || String(err);
            }
            return finish();
          }

          if (wantsTemperature) {
            state.temperature = sensorState.temperature;
            state.temperatureScale = sensorState.temperatureScale;
          }

          if (wantsMotion) {
            state.motion = sensorState.motion;
          }

          finish();
        }
      );
    };

    getSetting(
      'DISPLAY_BRIGHTNESS_ADJUST',
      'getBrightnessSetting',
      'displayBrightness',
      (value) => (value == null ? null : Number(value)),
      () =>
        getSetting(
          'DISPLAY_POWER_TOGGLE',
          'getDisplayPowerSetting',
          'displayPower',
          (value) => (value == null ? null : !!value),
          () =>
            getSetting(
              'DISPLAY_ADAPTIVE_BRIGHTNESS',
              'getAdaptiveBrightnessSetting',
              'adaptiveBrightness',
              (value) => (value == null ? null : !!value),
              readSensors
            )
        )
    );
  }

  getSensorState(
    targetDevice,
    wantsTemperature,
    wantsMotion,
    callback
  ) {
    const alexa = alexaWrapper.alexa;
    const serialNumber = targetDevice?.serialNumber;

    if (
      !alexa ||
      typeof alexa.getSmarthomeDevices !== 'function'
    ) {
      return callback(
        new Error(
          'Installed alexa-remote2 does not expose getSmarthomeDevices().'
        )
      );
    }

    if (!serialNumber) {
      return callback(
        new Error(
          'Cannot query Echo sensors without a serialNumber.'
        )
      );
    }

    const result = {
      temperature: null,
      temperatureScale: null,
      motion: null,
      errors: {},
    };

    const appliances = [];
    const seenObjects = new Set();

    // Use an explicit stack rather than recursive descent. Amazon's
    // smart-home payload can be large and its nesting depth is not a
    // contract we control; iterative traversal avoids call-stack risk.
    const collectAppliances = (rootValue) => {
      const stack = [rootValue];

      while (stack.length) {
        const value = stack.pop();

        if (!value || typeof value !== 'object') {
          continue;
        }

        if (seenObjects.has(value)) {
          continue;
        }

        seenObjects.add(value);

        if (
          Array.isArray(value.alexaDeviceIdentifierList) ||
          value.applianceId ||
          value.entityId ||
          value.applianceKey
        ) {
          appliances.push(value);
        }

        if (Array.isArray(value)) {
          for (let i = value.length - 1; i >= 0; i--) {
            stack.push(value[i]);
          }
        } else {
          Object.keys(value).forEach(key => {
            stack.push(value[key]);
          });
        }
      }
    };

    const hasInterface = (
      appliance,
      interfaceName
    ) => {
      const applianceCapabilities =
        appliance?.capabilities;

      if (!Array.isArray(applianceCapabilities)) {
        return false;
      }

      return applianceCapabilities.some(cap =>
        cap &&
        (
          cap.interfaceName === interfaceName ||
          cap.namespace === interfaceName
        )
      );
    };

    const linkedToSerial = (appliance) => {
      const list =
        appliance?.alexaDeviceIdentifierList;

      if (!Array.isArray(list)) {
        return false;
      }

      return list.some(entry =>
        entry &&
        (
          entry.dmsDeviceSerialNumber === serialNumber ||
          entry.serialNumber === serialNumber
        )
      );
    };

    const unique = values =>
      [...new Set(values.filter(Boolean))];

    try {
      alexa.getSmarthomeDevices(
        (err, data) => {
          if (err) {
            return callback(err);
          }

          collectAppliances(data);

          const temperatureIds = [];
          const motionIds = [];

          appliances.forEach(appliance => {
            if (!linkedToSerial(appliance)) {
              return;
            }

            const ids = unique([
              appliance.applianceId,
              appliance.entityId,
              appliance.applianceKey,
              ...(Array.isArray(
                appliance.mergedApplianceIds
              )
                ? appliance.mergedApplianceIds
                : [])
            ]);

            if (
              wantsTemperature &&
              hasInterface(
                appliance,
                'Alexa.TemperatureSensor'
              )
            ) {
              temperatureIds.push(...ids);
            }

            if (
              wantsMotion &&
              hasInterface(
                appliance,
                'Alexa.MotionSensor'
              )
            ) {
              motionIds.push(...ids);
            }
          });

          const stateIds = unique([
            ...temperatureIds,
            ...motionIds
          ]);

          if (
            wantsTemperature &&
            !temperatureIds.length
          ) {
            result.errors.temperature =
              `No Alexa Smart Home temperature entity was found for serialNumber '${serialNumber}'.`;
          }

          if (
            wantsMotion &&
            !motionIds.length
          ) {
            result.errors.motion =
              `No Alexa Smart Home motion entity was found for serialNumber '${serialNumber}'.`;
          }

          if (!stateIds.length) {
            return callback(null, result);
          }

          if (
            typeof alexa.querySmarthomeDevices !==
            'function'
          ) {
            return callback(
              new Error(
                'Installed alexa-remote2 does not expose querySmarthomeDevices().'
              )
            );
          }

          const requests = stateIds.map(entityId => ({
            entityId,
            entityType: 'APPLIANCE',
          }));

          alexa.querySmarthomeDevices(
            requests,
            'APPLIANCE',
            15000,
            (stateErr, stateData) => {
              if (stateErr) {
                return callback(stateErr);
              }

              const states = [];

              const collectStates = (rootValue) => {
                const stack = [rootValue];

                while (stack.length) {
                  const value = stack.pop();

                  if (!value) {
                    continue;
                  }

                  if (Array.isArray(value)) {
                    for (let i = value.length - 1; i >= 0; i--) {
                      stack.push(value[i]);
                    }
                    continue;
                  }

                  if (typeof value !== 'object') {
                    continue;
                  }

                  if (Array.isArray(value.capabilityStates)) {
                    value.capabilityStates.forEach(sensorState => {
                      if (typeof sensorState === 'string') {
                        try {
                          states.push(JSON.parse(sensorState));
                        } catch (_) {
                          // Ignore malformed individual states.
                        }
                      } else if (
                        sensorState &&
                        typeof sensorState === 'object'
                      ) {
                        states.push(sensorState);
                      }
                    });
                  }

                  Object.keys(value).forEach(key => {
                    if (key !== 'capabilityStates') {
                      stack.push(value[key]);
                    }
                  });
                }
              };

              collectStates(stateData);

              states.forEach(sensorState => {
                if (
                  !sensorState ||
                  typeof sensorState !==
                    'object'
                ) {
                  return;
                }

                if (
                  wantsTemperature &&
                  sensorState.namespace ===
                    'Alexa.TemperatureSensor' &&
                  sensorState.name ===
                    'temperature'
                ) {
                  const value =
                    sensorState.value;

                  if (
                    value &&
                    typeof value === 'object'
                  ) {
                    if (
                      value.value !== undefined
                    ) {
                      result.temperature =
                        value.value;
                    } else if (
                      value.temperature !==
                      undefined
                    ) {
                      result.temperature =
                        value.temperature;
                    }

                    result.temperatureScale =
                      value.scale ||
                      value.unitOfMeasurement ||
                      value.unit ||
                      null;
                  } else if (
                    value !== undefined &&
                    value !== null
                  ) {
                    result.temperature =
                      value;
                  }
                }

                if (
                  wantsMotion &&
                  sensorState.namespace ===
                    'Alexa.MotionSensor' &&
                  sensorState.name ===
                    'detectionState'
                ) {
                  result.motion =
                    sensorState.value;
                }
              });

              if (
                wantsTemperature &&
                result.temperature === null &&
                !result.errors.temperature
              ) {
                result.errors.temperature =
                  `Alexa returned no temperature state for serialNumber '${serialNumber}'.`;
              }

              if (
                wantsMotion &&
                result.motion === null &&
                !result.errors.motion
              ) {
                result.errors.motion =
                  `Alexa returned no motion state for serialNumber '${serialNumber}'.`;
              }

              callback(null, result);
            }
          );
        }
      );
    } catch (err) {
      callback(err);
    }
  }


  setDeviceFeature(serialNumber, capability, methodName, value, callback) {
    const alexa = alexaWrapper.alexa;

    if (!alexa || !alexa.serialNumbers) {
      return callback(new Error('Alexa instance not initialized.'));
    }

    const targetDevice =
      alexa.serialNumbers[serialNumber] ||
      Object.values(alexa.serialNumbers).find(
        (device) => device.serialNumber === serialNumber
      );

    if (!targetDevice) {
      return callback(
        new Error(`Device with serialNumber '${serialNumber}' not found.`)
      );
    }

    const caps = Array.isArray(targetDevice.capabilities)
      ? targetDevice.capabilities
      : [];

    if (!caps.includes(capability)) {
      return callback(
        new Error(
          `Device '${targetDevice.accountName || serialNumber}' does not advertise capability '${capability}'.`
        )
      );
    }

    if (typeof alexa[methodName] !== 'function') {
      return callback(
        new Error(`Installed alexa-remote2 library does not expose ${methodName}().`)
      );
    }

    try {
      alexa[methodName](serialNumber, value, callback);
    } catch (err) {
      callback(err);
    }
  }


  getPlaybackState(serialNumber, callback) {
    const alexa = alexaWrapper.alexa;

    if (!alexa || !alexa.serialNumbers) {
      return callback(new Error('Alexa instance not initialized.'));
    }

    const targetDevice =
      alexa.serialNumbers[serialNumber] ||
      Object.values(alexa.serialNumbers).find(
        (device) => device.serialNumber === serialNumber
      );

    if (!targetDevice) {
      return callback(
        new Error(`Device with serialNumber '${serialNumber}' not found.`)
      );
    }

    if (typeof alexa.getPlayerInfo !== 'function') {
      return callback(
        new Error(
          'Installed alexa-remote2 library does not expose getPlayerInfo().'
        )
      );
    }

    if (
      !this._beginInFlight(
        this.inFlightPlaybackRequests,
        serialNumber,
        callback
      )
    ) {
      logger.debug(
        `[CommandDispatcher] Coalescing concurrent getPlaybackState request for [${serialNumber}]`
      );
      return;
    }

    const complete = (err, result) => {
      this._finishInFlight(
        this.inFlightPlaybackRequests,
        serialNumber,
        err,
        result
      );
    };

    const startedAt = Date.now();

    logger.info(
      `[CommandDispatcher] START 'getPlaybackState' for serial [${serialNumber}]`
    );

    try {
      alexa.getPlayerInfo(serialNumber, (err, response) => {
        const durationMs = Date.now() - startedAt;

        if (err) {
          logger.error(
            `[CommandDispatcher] CALLBACK ERROR 'getPlaybackState' for serial [${serialNumber}] after ${durationMs}ms`,
            {
              errorCode: err.code || null,
              errorMessage: err.message || String(err)
            }
          );

          return complete(err);
        }

        const playerInfo = response?.playerInfo || {};

        // Normalize track artwork metadata for Hubitat clients.
        // Prefer mainArt (typically 600px) and fall back to miniArt.
        const trackArt =
          playerInfo.mainArt?.url ||
          playerInfo.miniArt?.url ||
          playerInfo.template?.art?.url ||
          null;

        const trackArtContentType =
          playerInfo.mainArt?.contentType ||
          playerInfo.miniArt?.contentType ||
          null;

        const mediaProvider =
          playerInfo.provider?.providerName ||
          null;

        const state =
          typeof playerInfo.state === 'string'
            ? playerInfo.state.toUpperCase()
            : null;

        const result = {
          serialNumber: targetDevice.serialNumber,
          deviceType: targetDevice.deviceType || null,
          state,
          playing: state === 'PLAYING',

          title: playerInfo.infoText?.title || null,

          artist: playerInfo.infoText?.subText1 || null,

          album: playerInfo.infoText?.subText2 || null,

          trackImage: trackArt,

          trackImageContentType: trackArtContentType,

          trackImageHtml: trackArt
            ? `<img src="${trackArt}" alt="Album Art" />`
            : null,

          mediaProvider,

          progressSeconds:
            playerInfo.progress?.mediaProgress != null
              ? Number(playerInfo.progress.mediaProgress) / 1000
              : null,

          mediaLengthSeconds:
            playerInfo.progress?.mediaLength != null
              ? Number(playerInfo.progress.mediaLength) / 1000
              : null,

          muted:
            playerInfo.volume?.muted !== undefined
              ? !!playerInfo.volume.muted
              : null,

          volume:
            playerInfo.volume?.volume !== undefined
              ? playerInfo.volume.volume
              : null,

          playerInfo,
        };

        logger.info(
          `[CommandDispatcher] CALLBACK SUCCESS 'getPlaybackState' for serial [${serialNumber}] after ${durationMs}ms`,
          {
            state: result.state,
            playing: result.playing
          }
        );

        complete(null, result);
      });

    } catch (err) {
      logger.error(
        `[CommandDispatcher] EXCEPTION 'getPlaybackState' for serial [${serialNumber}]`,
        {
          error: err.message || String(err)
        }
      );

      complete(err);
    }
  }


  /*
   * Issue play/pause and verify the resulting player state.
   *
   * alexa-remote2 can execute sendCommand() successfully on the Echo
   * without invoking its completion callback.  Therefore play/pause
   * cannot use the normal callback timeout wrapper as proof of success.
   *
   * We issue the command, then poll the authoritative player state.
   */
  playPauseAndVerify(serialNumber, command, callback) {
    const alexa = alexaWrapper.alexa;

    if (!alexa || !alexa.serialNumbers) {
      return callback(new Error('Alexa instance not initialized.'));
    }

    const expectedState =
      command === 'pause'
        ? 'PAUSED'
        : 'PLAYING';

    const startedAt = Date.now();

    logger.info(
      `[CommandDispatcher] START '${command}/verify' for serial [${serialNumber}]`,
      {
        expectedState
      }
    );

    let commandIssued = false;
    let completed = false;
    let pollTimer = null;
    let hardTimeout = null;

    const cleanup = () => {
      if (pollTimer) {
        clearTimeout(pollTimer);
        pollTimer = null;
      }

      if (hardTimeout) {
        clearTimeout(hardTimeout);
        hardTimeout = null;
      }
    };

    const finish = (err, result) => {
      if (completed) {
        return;
      }

      completed = true;
      cleanup();

      const durationMs = Date.now() - startedAt;

      if (err) {
        logger.error(
          `[CommandDispatcher] '${command}/verify' FAILED for serial [${serialNumber}] after ${durationMs}ms`,
          {
            errorCode: err.code || null,
            errorMessage: err.message || String(err)
          }
        );
      } else {
        logger.info(
          `[CommandDispatcher] '${command}/verify' SUCCESS for serial [${serialNumber}] after ${durationMs}ms`,
          {
            state: result?.state || null,
            playing: result?.playing
          }
        );
      }

      callback(err, result);
    };

    const pollState = () => {
      if (completed) {
        return;
      }

      try {
        alexa.getPlayerInfo(serialNumber, (err, response) => {
          if (completed) {
            return;
          }

          if (!err) {
            const playerInfo = response?.playerInfo || {};

            const state =
              typeof playerInfo.state === 'string'
                ? playerInfo.state.toUpperCase()
                : null;

            if (state === expectedState) {
              const targetDevice =
                alexa.serialNumbers[serialNumber] ||
                Object.values(alexa.serialNumbers).find(
                  (device) => device.serialNumber === serialNumber
                );

              const result = {
                serialNumber,
                deviceType: targetDevice?.deviceType || null,
                state,
                playing: state === 'PLAYING',

                title: playerInfo.infoText?.title || null,
                artist: playerInfo.infoText?.subText1 || null,
                album: playerInfo.infoText?.subText2 || null,

                ...this._extractPlaybackMetadata(playerInfo),

                progressSeconds:
                  playerInfo.progress?.mediaProgress != null
                    ? Number(playerInfo.progress.mediaProgress) / 1000
                    : null,

                mediaLengthSeconds:
                  playerInfo.progress?.mediaLength != null
                    ? Number(playerInfo.progress.mediaLength) / 1000
                    : null,

                muted:
                  playerInfo.volume?.muted !== undefined
                    ? !!playerInfo.volume.muted
                    : null,

                volume:
                  playerInfo.volume?.volume !== undefined
                    ? playerInfo.volume.volume
                    : null,

                playerInfo,
              };

              return finish(null, result);
            }
          }

          pollTimer = setTimeout(pollState, 500);
        });

      } catch (err) {
        pollTimer = setTimeout(pollState, 500);
      }
    };

    /*
     * Do not wait for sendCommand()'s callback.
     *
     * The callback has already been demonstrated to be unreliable
     * for pause on this server: the Echo paused but the callback
     * never arrived.
     */
    try {
      alexa.sendCommand(
        serialNumber,
        command,
        () => {
          logger.info(
            `[CommandDispatcher] '${command}' Alexa callback received for serial [${serialNumber}]`
          );
        }
      );

      commandIssued = true;

      logger.info(
        `[CommandDispatcher] '${command}' issued to serial [${serialNumber}]; verifying player state`
      );

      pollState();

    } catch (err) {
      return finish(err);
    }

    /*
     * Five seconds is enough for the Echo state to propagate.
     * This is NOT the old 15-second command callback timeout.
     */
    hardTimeout = setTimeout(() => {
      if (completed) {
        return;
      }

      const err = new Error(
        `Alexa command '${command}' was issued but the player state did not reach '${expectedState}' within 5000ms.`
      );

      err.code = 'PLAYBACK_VERIFY_TIMEOUT';

      finish(err);

    }, 5000);
  }


  dispatch(serialNumber, command, payload = {}, callback) {
    const alexa = alexaWrapper.alexa;

    if (!alexa || !alexa.serialNumbers) {
      return callback(new Error('Alexa instance not initialized.'));
    }

    if (command === 'getDeviceFeatures') {
      if (!serialNumber) {
        return callback(
          new Error(
            "Missing 'serialNumber' for getDeviceFeatures command."
          )
        );
      }

      return this.getDeviceFeatures(serialNumber, callback);
    }

    if (command === 'getPlaybackState') {
      if (!serialNumber) {
        return callback(
          new Error(
            "Missing 'serialNumber' for getPlaybackState command."
          )
        );
      }

      return this.getPlaybackState(serialNumber, callback);
    }

    const isMultiTarget =
      Array.isArray(serialNumber) && serialNumber.length > 1;

    const targetDevice =
      !isMultiTarget
        ? (
            alexa.serialNumbers[serialNumber] ||
            Object.values(alexa.serialNumbers).find(
              (device) => device.serialNumber === serialNumber
            )
          )
        : null;

    // Multi-device serial arrays are currently supported only by commands
    // that alexa-remote2 can target as a single sequence. The raw command
    // passes the array through unchanged to sendSequenceCommand().
    if (!targetDevice && command !== 'announceAll' && !(isMultiTarget && command === 'raw')) {
      return callback(
        new Error(
          `Device with serialNumber '${serialNumber}' not found.`
        )
      );
    }

    const invokeAlexa = (operation, send, completion = callback, logPayload = payload) => {
      const startedAt = Date.now();
      let finished = false;

      logger.info(
        `[CommandDispatcher] START '${operation}' for serial [${serialNumber}]`,
        {
          payload: logPayload
        }
      );

      const finish = (err, result) => {
        const durationMs = Date.now() - startedAt;

        if (finished) {
          logger.warn(
            `[CommandDispatcher] LATE CALLBACK '${operation}' for serial [${serialNumber}] after timeout`,
            {
              durationMs,
              error: err ? (err.message || String(err)) : null
            }
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
              resultType:
                result === null
                  ? 'null'
                  : typeof result,

              hasResult:
                result !== undefined &&
                result !== null
            }
          );
        }

        completion(err, result);
      };

      const timeoutHandle = setTimeout(() => {
        const durationMs = Date.now() - startedAt;

        const err = new Error(
          `Alexa command '${operation}' did not complete within 7500ms.`
        );

        err.code = 'COMMAND_TIMEOUT';

        if (finished) {
          return;
        }

        finished = true;

        logger.error(
          `[CommandDispatcher] TIMEOUT '${operation}' for serial [${serialNumber}] after ${durationMs}ms`,
          {
            errorCode: err.code
          }
        );

        completion(err);

      }, 7500);

      try {
        send(finish);

      } catch (err) {
        finish(err);
      }
    };


    switch (command) {

      case 'speak':

        if (!payload.text) {
          return callback(
            new Error(
              "Missing 'text' in payload for speak command."
            )
          );
        }

        // When a voice is supplied, use Alexa SSML voice selection.
        // Without a voice, preserve the existing normal speak behavior.
        const selectedVoice =
          typeof payload.voice === 'string'
            ? payload.voice.trim()
            : '';

        let sequenceCommand = 'speak';
        let sequencePayload = payload.text;

        if (selectedVoice) {
          const escapeXmlText = (value) =>
            String(value)
              .replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;');

          const escapeXmlAttribute = (value) =>
            String(value)
              .replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;')
              .replace(/'/g, '&apos;');

          sequenceCommand = 'ssml';

          sequencePayload =
            `<speak><voice name="${escapeXmlAttribute(selectedVoice)}">` +
            `${escapeXmlText(payload.text)}` +
            `</voice></speak>`;
        }

        invokeAlexa(
          selectedVoice ? 'speak/ssml' : 'speak',
          (done) => {
            // WHA virtual endpoints accept the command at the bridge/API
            // level but do not produce speech on the physical members.
            // Reuse the existing WHA cluster-member routing already used
            // by announce, so speak/SSML is sent to the actual Echo devices.
            let speakTarget = serialNumber;

            if (
              targetDevice &&
              String(targetDevice.deviceFamily || '').toUpperCase() === 'WHA' &&
              Array.isArray(targetDevice.clusterMembers) &&
              targetDevice.clusterMembers.length > 0
            ) {
              speakTarget = targetDevice.clusterMembers;
              logger.info('[CommandDispatcher] WHA speak expanded to physical cluster members', {
                whaSerialNumber: serialNumber,
                clusterMembers: speakTarget,
                sequenceCommand
              });
            }

            alexa.sendSequenceCommand(
              speakTarget,
              sequenceCommand,
              sequencePayload,
              done
            );
          }
        );

        break;


      case 'announce':

        if (!payload.text) {
          return callback(
            new Error(
              "Missing 'text' in payload for announcement."
            )
          );
        }

        invokeAlexa('announce', (done) => {
          let announcementTarget = serialNumber;

          if (
            targetDevice &&
            String(targetDevice.deviceFamily || '').toUpperCase() === 'WHA' &&
            Array.isArray(targetDevice.clusterMembers) &&
            targetDevice.clusterMembers.length > 0
          ) {
            announcementTarget = targetDevice.clusterMembers;
            logger.info('[CommandDispatcher] WHA announce expanded to physical cluster members', {
              whaSerialNumber: serialNumber,
              clusterMembers: announcementTarget
            });
          }

          alexa.sendSequenceCommand(
            announcementTarget,
            'announcement',
            payload.text,
            done
          );
        });

        break;


      case 'stop':

        // Proper Alexa device stop. Do not alias Stop to Pause.
        invokeAlexa('stop/deviceStop', (done) => {
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
            new Error(
              "Missing 'text' in payload for announceAll."
            )
          );
        }

        const allSerials =
          Object.keys(alexa.serialNumbers);

        if (allSerials.length === 0) {
          return callback(
            new Error(
              'No Alexa devices are available.'
            )
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

              if (
                completed ===
                allSerials.length
              ) {

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
            new Error(
              "Missing 'level' (0-100) in payload."
            )
          );
        }

        const vol =
          parseInt(payload.level, 10);

        if (
          Number.isNaN(vol) ||
          vol < 0 ||
          vol > 100
        ) {
          return callback(
            new Error(
              "Invalid 'level'. Volume must be a number from 0 to 100."
            )
          );
        }

        this.volumeTargets.set(serialNumber, vol);

        this._queueVolumeBurst(
          serialNumber,
          vol,
          (finalTarget, done) => {
            invokeAlexa(
              'volume',
              (sendDone) => {
alexa.sendSequenceCommand(
                  serialNumber,
                  'volume',
                  finalTarget,
                  sendDone
                );
              },
              done,
              { level: finalTarget }
            );
          },
          callback
        );

        break;
      }


      case 'volumeUp': {

        const currentVol = this._getInitialVolume(
          targetDevice,
          serialNumber
        );

        const newVol = Math.min(100, currentVol + 5);

        this.volumeTargets.set(serialNumber, newVol);

        this._queueVolumeBurst(
          serialNumber,
          newVol,
          (finalTarget, done) => {
            invokeAlexa(
              'volume',
              (sendDone) => {
alexa.sendSequenceCommand(
                  serialNumber,
                  'volume',
                  finalTarget,
                  sendDone
                );
              },
              done,
              { level: finalTarget }
            );
          },
          callback
        );

        break;
      }


      case 'volumeDown': {

        const currentVol = this._getInitialVolume(
          targetDevice,
          serialNumber
        );

        const newVol = Math.max(0, currentVol - 5);

        this.volumeTargets.set(serialNumber, newVol);

        this._queueVolumeBurst(
          serialNumber,
          newVol,
          (finalTarget, done) => {
            invokeAlexa(
              'volume',
              (sendDone) => {
alexa.sendSequenceCommand(
                  serialNumber,
                  'volume',
                  finalTarget,
                  sendDone
                );
              },
              done,
              { level: finalTarget }
            );
          },
          callback
        );

        break;
      }


      case 'play':
      case 'pause':

        /*
         * Do NOT use invokeAlexa() here.
         *
         * alexa-remote2 may execute the command on the Echo
         * without invoking its completion callback.
         *
         * Instead, issue the command and verify the resulting
         * player state through getPlayerInfo().
         */
        return this.playPauseAndVerify(
          serialNumber,
          command,
          callback
        );


      case 'nextTrack':

        invokeAlexa('nextTrack/next', (done) => {

          alexa.sendCommand(
            serialNumber,
            'next',
            done
          );

        });

        break;


      case 'previousTrack':

        invokeAlexa(
          'previousTrack/previous',
          (done) => {

            alexa.sendCommand(
              serialNumber,
              'previous',
              done
            );

          }
        );

        break;


      case 'getDisplayBrightness':
        if (!targetDevice.capabilities?.includes('DISPLAY_BRIGHTNESS_ADJUST')) {
          return callback(new Error(`Device '${targetDevice.accountName || serialNumber}' does not advertise DISPLAY_BRIGHTNESS_ADJUST.`));
        }
        invokeAlexa('getDisplayBrightness', (done) => {
          alexa.getBrightnessSetting(serialNumber, done);
        });
        break;


      case 'setDisplayBrightness': {
        if (payload.level === undefined) {
          return callback(new Error("Missing 'level' (0-100) in payload for setDisplayBrightness."));
        }

        const brightness = Number(payload.level);
        if (!Number.isFinite(brightness) || brightness < 0 || brightness > 100) {
          return callback(new Error("Invalid 'level'. Display brightness must be a number from 0 to 100."));
        }

        invokeAlexa('setDisplayBrightness', (done) => {
          this.setDeviceFeature(
            serialNumber,
            'DISPLAY_BRIGHTNESS_ADJUST',
            'setBrightnessSetting',
            brightness,
            done
          );
        });
        break;
      }


      case 'getDisplayPower':
        if (!targetDevice.capabilities?.includes('DISPLAY_POWER_TOGGLE')) {
          return callback(new Error(`Device '${targetDevice.accountName || serialNumber}' does not advertise DISPLAY_POWER_TOGGLE.`));
        }
        invokeAlexa('getDisplayPower', (done) => {
          alexa.getDisplayPowerSetting(serialNumber, done);
        });
        break;


      case 'setDisplayPowerOn':
        invokeAlexa('setDisplayPowerOn', (done) => {
          this.setDeviceFeature(
            serialNumber,
            'DISPLAY_POWER_TOGGLE',
            'setDisplayPowerSetting',
            true,
            done
          );
        });
        break;


      case 'setDisplayPowerOff':
        invokeAlexa('setDisplayPowerOff', (done) => {
          this.setDeviceFeature(
            serialNumber,
            'DISPLAY_POWER_TOGGLE',
            'setDisplayPowerSetting',
            false,
            done
          );
        });
        break;


      case 'getAdaptiveBrightness':
        if (!targetDevice.capabilities?.includes('DISPLAY_ADAPTIVE_BRIGHTNESS')) {
          return callback(new Error(`Device '${targetDevice.accountName || serialNumber}' does not advertise DISPLAY_ADAPTIVE_BRIGHTNESS.`));
        }
        invokeAlexa('getAdaptiveBrightness', (done) => {
          alexa.getAdaptiveBrightnessSetting(serialNumber, done);
        });
        break;


      case 'setAdaptiveBrightnessOn':
        invokeAlexa('setAdaptiveBrightnessOn', (done) => {
          this.setDeviceFeature(
            serialNumber,
            'DISPLAY_ADAPTIVE_BRIGHTNESS',
            'setAdaptiveBrightnessSetting',
            true,
            done
          );
        });
        break;


      case 'setAdaptiveBrightnessOff':
        invokeAlexa('setAdaptiveBrightnessOff', (done) => {
          this.setDeviceFeature(
            serialNumber,
            'DISPLAY_ADAPTIVE_BRIGHTNESS',
            'setAdaptiveBrightnessSetting',
            false,
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


      case 'voiceCmdAsText':

        if (!payload.text) {
          return callback(
            new Error(
              "Missing 'text' in payload for voiceCmdAsText command."
            )
          );
        }

        // Public Echos Speak API command is voiceCmdAsText.
        // alexa-remote2 internally calls this sequence operation textCommand.
        invokeAlexa(
          'voiceCmdAsText/textCommand',
          (done) => {

            alexa.sendSequenceCommand(
              serialNumber,
              'textCommand',
              payload.text,
              done
            );

          }
        );

        break;


      case 'raw':

        // serialNumber may be either one physical Echo serial or an array
        // of physical Echo serials. Keep an array intact so alexa-remote2
        // can construct a multi-device sequence target.
        if (!payload.rawCommand) {
          return callback(
            new Error(
              "Missing 'rawCommand' in payload for raw execution."
            )
          );
        }

        invokeAlexa(
          `raw/${payload.rawCommand}`,
          (done) => {

            alexa.sendSequenceCommand(
              serialNumber,
              payload.rawCommand,
              payload.rawPayload || null,
              done
            );

          }
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