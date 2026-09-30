const fs = require('fs');
const path = require('path');
const alexaWrapper = require('./alexaWrapper');
const logger = require('./logger');

const DATA_DIR = path.join(__dirname, '../data');
const SESSION_FILE = path.join(DATA_DIR, 'session.json');

const STATES = {
  UNINITIALIZED: 'UNINITIALIZED',
  VALIDATING: 'VALIDATING',
  AUTHENTICATING: 'AUTHENTICATING',
  AUTHENTICATED: 'AUTHENTICATED',
  AUTH_REQUIRED: 'AUTH_REQUIRED',
  DEGRADED: 'DEGRADED',
};

class AuthManager {
  constructor() {
    this.state = STATES.UNINITIALIZED;
    this.lastValidated = null;
    this.lastError = null;
    this.ensureDataDir();
  }

  setState(newState, reason = '') {
    const oldState = this.state;
    this.state = newState;

    logger.info(
      `[AuthManager] State transition: ${oldState} -> ${newState}` +
      `${reason ? ` (${reason})` : ''}`
    );
  }

  ensureDataDir() {
    if (!fs.existsSync(DATA_DIR)) {
      logger.info(
        `[AuthManager] Creating missing data directory at: ${DATA_DIR}`
      );

      fs.mkdirSync(DATA_DIR, { recursive: true });
    }
  }

  getStatus() {
    return {
      state: this.state,
      authenticated: this.state === STATES.AUTHENTICATED,
      lastValidated: this.lastValidated
        ? this.lastValidated.toISOString()
        : null,
      lastError: this.lastError,
    };
  }

  boot() {
    logger.info('[AuthManager] Starting boot pass...');

    if (!fs.existsSync(SESSION_FILE)) {
      logger.info('[AuthManager] No session file found in /data.');

      this.setState(
        STATES.AUTH_REQUIRED,
        'no session file'
      );

      return;
    }

    try {
      logger.info(
        `[AuthManager] Reading session file from ${SESSION_FILE}...`
      );

      const rawData = fs.readFileSync(
        SESSION_FILE,
        'utf8'
      );

      const sessionData = JSON.parse(rawData);

      logger.info(
        '[AuthManager] Session file successfully parsed. Attempting restore...'
      );

      this.restoreSession(sessionData);

    } catch (err) {
      logger.error(
        '[AuthManager] Failed to read/parse /data/session.json:',
        {
          error: err.message
        }
      );

      this.lastError = 'Corrupt session file in /data';

      this.setState(
        STATES.AUTH_REQUIRED,
        'session file corrupt'
      );
    }
  }

  restoreSession(sessionData) {
    this.setState(
      STATES.VALIDATING,
      'restoring session from disk'
    );

    this.lastError = null;

    /*
     * alexa-remote2 expects both:
     *
     *   formerRegistrationData
     *   cookie
     *
     * when restoring a previously authenticated session.
     *
     * The successful authentication payload saved by saveSession()
     * contains the localCookie field that must be supplied as cookie.
     */
    const registrationData =
      sessionData.formerRegistrationData || sessionData;

    const initConfig = {
      amazonPage: 'amazon.com',
      baseAmazonPage: 'amazon.com',
      amazonPageProxyLanguage: 'en_US',
      acceptLanguage: 'en-US',
      userLanguage: 'en-US',
      formerRegistrationData: registrationData,
      cookie: registrationData.localCookie,
      useLocalCookie: true,
      alexaServiceHost: 'alexa.amazon.com',
    };

    logger.info(
      '[AuthManager] Passing formerRegistrationData and saved cookie into alexaWrapper.init()...'
    );

    alexaWrapper.init(initConfig, (err) => {

      if (err) {
        const isNetworkErr =
          err.code === 'EHOSTUNREACH' ||
          err.code === 'ETIMEDOUT' ||
          err.message?.includes('timeout');

        if (isNetworkErr) {
          logger.warn(
            '[AuthManager] Network timeout during restore. Preserving credentials.',
            {
              error: err.message
            }
          );

          this.lastError =
            'Network or DNS timeout during Amazon sync';

          this.setState(
            STATES.DEGRADED,
            'network timeout'
          );

          return;
        }

        logger.warn(
          '[AuthManager] Amazon rejected stored registration data.',
          {
            error: err.message
          }
        );

        this.lastError =
          err.message ||
          'Authentication rejected by Amazon';

        this.setState(
          STATES.AUTH_REQUIRED,
          'credentials rejected'
        );

        return;
      }

      logger.info(
        '[AuthManager] Library restored session successfully. Validating active session...'
      );

      this.validateActiveSession();
    });
  }

  validateActiveSession() {
    logger.info(
      '[AuthManager] Executing explicit session validation...'
    );

    alexaWrapper.validateSession(
      (authenticated, err) => {

        if (authenticated) {
          logger.info(
            '[AuthManager] Amazon session explicitly VALIDATED!'
          );

          this.lastValidated = new Date();
          this.lastError = null;

          this.setState(
            STATES.AUTHENTICATED,
            'session verified via Amazon'
          );

        } else {

          if (
            err &&
            (
              err.code === 'ETIMEDOUT' ||
              err.code === 'EHOSTUNREACH'
            )
          ) {
            logger.warn(
              '[AuthManager] Validation check timed out. Retaining session.',
              {
                error: err?.message
              }
            );

            this.lastError =
              'Validation check timed out';

            this.setState(
              STATES.DEGRADED,
              'validation timed out'
            );

            return;
          }

          logger.warn(
            '[AuthManager] Validation returned FALSE (HTTP 401/403). Session required.',
            {
              error: err?.message
            }
          );

          this.lastError =
            'Session expired or invalidated by Amazon';

          this.setState(
            STATES.AUTH_REQUIRED,
            'validation failed'
          );
        }
      }
    );
  }

  startProxyAuth() {
    if (this.state === STATES.AUTHENTICATING) {
      logger.info(
        '[AuthManager] Proxy authentication request ignored (already in AUTHENTICATING state).'
      );

      return;
    }

    this.setState(
      STATES.AUTHENTICATING,
      'proxy auth initialized'
    );

    this.lastError = null;

    const proxyHost =
      process.env.PROXY_PUBLIC_HOST ||
      '192.168.1.12';

    const proxyPort =
      parseInt(
        process.env.PROXY_PORT || '4001',
        10
      );

    logger.info(
      `[AuthManager] Starting proxy listener on ${proxyHost}:${proxyPort} (US English Forced)...`
    );

    const proxyConfig = {
      amazonPage: 'amazon.com',
      baseAmazonPage: 'amazon.com',
      amazonPageProxyLanguage: 'en_US',
      acceptLanguage: 'en-US',
      userLanguage: 'en-US',
      proxyOnly: true,
      proxyOwnIp: proxyHost,
      proxyPort: proxyPort,
      proxyListenBind: '0.0.0.0',
      alexaServiceHost: 'alexa.amazon.com',
      formerRegistrationData: null,
      proxyLogLevel: 'debug',
    };

    alexaWrapper.init(
      proxyConfig,
      (err, res) => {

        /*
         * alexa-remote2 normally completes proxy authentication
         * with res === undefined. The authentication payload is
         * stored internally by the AlexaRemote instance.
         */

        if (err) {
          const errStr =
            err.message || String(err);

          const isProxyListeningMsg =
            errStr.includes('open this URL') ||
            errStr.includes('Proxy listening') ||
            errStr.includes(String(proxyPort)) ||
            errStr.includes('http://');

          if (isProxyListeningMsg) {
            logger.info(
              `[AuthManager] Proxy active. Open in browser: http://${proxyHost}:${proxyPort}`
            );

            return;
          }

          logger.error(
            '[AuthManager] Proxy auth encountered terminal error:',
            {
              error: errStr
            }
          );

          alexaWrapper.stopProxy();

          this.lastError = errStr;

          this.setState(
            STATES.AUTH_REQUIRED,
            'proxy error'
          );

          return;
        }

        logger.info(
          '[AuthManager] alexa.init() completed successfully.'
        );

        logger.info(
          `[AuthManager] alexa.init() callback result present: ${!!res}`
        );

        /*
         * Retrieve the complete authentication payload from the
         * AlexaRemote instance.
         */
        const sessionData =
          alexaWrapper.getSessionData();

        logger.info(
          '[AuthManager] Retrieved authenticated session from AlexaRemote instance:',
          {
            hasFormerRegistrationData:
              !!sessionData?.formerRegistrationData
          }
        );

        /*
         * The complete formerRegistrationData object is what
         * alexa-remote2 expects when restoring a session.
         */
        if (
          sessionData &&
          sessionData.formerRegistrationData
        ) {
          logger.info(
            '[AuthManager] SUCCESS: Captured Amazon authentication payload!'
          );

          const saved =
            this.saveSession(sessionData);

          if (!saved) {
            this.lastError =
              'Failed to persist Amazon authentication session';

            this.setState(
              STATES.AUTH_REQUIRED,
              'session persistence failed'
            );

            return;
          }

          /*
           * Verify the live authenticated session before declaring
           * the server AUTHENTICATED.
           */
          this.validateActiveSession();

          return;
        }

        /*
         * No error was returned, but the expected authentication
         * payload was not found.
         */
        logger.error(
          '[AuthManager] alexa.init() succeeded but no formerRegistrationData was found on the AlexaRemote instance.'
        );

        this.lastError =
          'Amazon authentication completed but session data was not available';

        alexaWrapper.stopProxy();

        this.setState(
          STATES.AUTH_REQUIRED,
          'session data unavailable'
        );
      }
    );
  }

  saveSession(sessionData) {
    try {
      logger.info(
        '[AuthManager] Persisting session data to disk...'
      );

      if (
        !sessionData ||
        !sessionData.formerRegistrationData
      ) {
        logger.error(
          '[AuthManager] Cannot persist session: formerRegistrationData is missing.'
        );

        return false;
      }

      const dataToPersist = {
        formerRegistrationData:
          sessionData.formerRegistrationData,

        savedAt:
          new Date().toISOString(),
      };

      fs.writeFileSync(
        SESSION_FILE,
        JSON.stringify(
          dataToPersist,
          null,
          2
        ),
        'utf8'
      );

      logger.info(
        `[AuthManager] Successfully wrote session file to ${SESSION_FILE}`
      );

      return true;

    } catch (err) {
      logger.error(
        '[AuthManager] Failed to write session file to /data/session.json:',
        {
          error: err.message
        }
      );

      return false;
    }
  }
}

module.exports = new AuthManager();