const AlexaRemote = require('alexa-remote2');
const logger = require('./logger');

class AlexaWrapper {
  constructor() {
    this.alexa = new AlexaRemote();
  }

  getAlexa() {
    return this.alexa;
  }

  init(options, callback) {
    const attemptId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

    logger.info(`[AlexaWrapper] ===== alexa.init() START [${attemptId}] =====`);

    logger.info('[AlexaWrapper] Library versions/configuration:', {
      amazonPage: options.amazonPage,
      baseAmazonPage: options.baseAmazonPage,
      amazonPageProxyLanguage: options.amazonPageProxyLanguage,
      acceptLanguage: options.acceptLanguage,
      userLanguage: options.userLanguage,
      proxyOnly: options.proxyOnly,
      proxyOwnIp: options.proxyOwnIp,
      proxyPort: options.proxyPort,
      proxyListenBind: options.proxyListenBind,
      alexaServiceHost: options.alexaServiceHost,
      hasFormerRegistrationData: !!options.formerRegistrationData
    });

    /*
     * Do not install a raw alexa-cookie2 logger here.
     *
     * alexa-cookie2 can log authentication cookies, tokens and other
     * sensitive data. The previous instrumentation wrote those messages
     * directly to stdout and bypassed our sanitizing logger.
     */
    delete options.logger;

    this.alexa.init(options, (err, result) => {

      logger.info(
        `[AlexaWrapper] ===== alexa.init() CALLBACK [${attemptId}] =====`
      );

      logger.info('[AlexaWrapper] Callback result:', {
        hasError: !!err,
        errorType: err ? typeof err : null,
        errorCode: err?.code || null,
        errorMessage: err?.message || null,
        hasResult: !!result,
        resultType: result ? typeof result : null,
        resultKeys:
          result && typeof result === 'object'
            ? Object.keys(result)
            : null
      });

      if (err) {
        logger.warn('[AlexaWrapper] Callback ERROR details:', {
          name: err.name || null,
          code: err.code || null,
          message: err.message || String(err),
          stack: err.stack || null
        });
      }

      /*
       * Only report the presence of sensitive fields.
       * Never log their actual values.
       */
      if (result && typeof result === 'object') {
        logger.info('[AlexaWrapper] Callback RESULT field presence:', {
          hasLoginUrl: !!result.loginUrl,
          hasCookie: !!result.cookie,
          hasFormerRegistrationData: !!result.formerRegistrationData,
          hasRefreshToken: !!result.refreshToken,
          hasAccessToken: !!result.accessToken,
          hasRegistrationData: !!result.registrationData
        });
      }

      logger.info(
        `[AlexaWrapper] ===== alexa.init() END [${attemptId}] =====`
      );

      callback(err, result);
    });
  }

  /*
   * alexa-remote2 stores the complete authentication payload in
   * _options.formerRegistrationData after successful authentication.
   *
   * This is the object that must be persisted for session restoration.
   */
  getSessionData() {
    return {
      formerRegistrationData:
        this.alexa?._options?.formerRegistrationData || null
    };
  }

  validateSession(callback) {
    logger.info(
      '[AlexaWrapper] Executing alexa.checkAuthentication()...'
    );

    this.alexa.checkAuthentication((authenticated, err) => {
      logger.info(
        '[AlexaWrapper] alexa.checkAuthentication() response:',
        {
          authenticated: !!authenticated,
          hasError: !!err,
          errorMessage: err
            ? (err.message || String(err))
            : null
        }
      );

      callback(authenticated, err);
    });
  }

  getMediaDiagnostics(serialNumber, callback) {
    const device = this.alexa?.find(serialNumber);

    if (!device) {
      return callback(new Error('Unknown Device or Serial number'), null);
    }

    const result = {
      serialNumber: device.serialNumber,
      deviceType: device.deviceType,
      mediaState: null,
      playerInfo: null,
      mediaSessions: null
    };

    let pending = 3;
    let firstError = null;

    const done = (err) => {
      if (err && !firstError) firstError = err;
      pending -= 1;
      if (pending === 0) callback(firstError, result);
    };

    this.alexa.getMedia(device, (err, data) => {
      result.mediaState = data || null;
      done(err);
    });

    this.alexa.getPlayerInfo(device, (err, data) => {
      result.playerInfo = data || null;
      done(err);
    });

    this.alexa.httpsGet(
      `/api/np/list-media-sessions?deviceSerialNumber=${encodeURIComponent(device.serialNumber)}&deviceType=${encodeURIComponent(device.deviceType)}`,
      (err, data) => {
        result.mediaSessions = data || null;
        done(err);
      }
    );
  }

  stopProxy() {
    logger.info(
      '[AlexaWrapper] Attempting to stop proxy listener...'
    );

    if (
      this.alexa &&
      typeof this.alexa.stopProxy === 'function'
    ) {
      try {
        this.alexa.stopProxy();

        logger.info(
          '[AlexaWrapper] Proxy listener stopped successfully.'
        );

      } catch (e) {
        logger.warn(
          '[AlexaWrapper] Exception caught stopping proxy:',
          {
            error: e.message
          }
        );
      }
    }
  }
}

module.exports = new AlexaWrapper();