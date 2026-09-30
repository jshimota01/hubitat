const fs = require('fs');
const path = require('path');
const alexaWrapper = require('./alexaWrapper');
const logger = require('./logger');

const DATA_DIR = path.join(__dirname, '../data');
const SESSION_FILE = path.join(DATA_DIR, 'session.json');

// Proactively verify the live Amazon session every 15 minutes.
// This is intentionally validation-only; it does not expose or log credentials.
const WATCHDOG_INTERVAL_MS = 15 * 60 * 1000;

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
    this.lastValidationAttempt = null;
    this.lastError = null;
    this.sessionSavedAt = null;

    this.watchdogTimer = null;
    this.nextValidationTime = null;
    this.validationInProgress = false;

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

  getAlexa() {
    return alexaWrapper.getAlexa();
  }

  getStatus() {
    const now = Date.now();

    let sessionAgeSeconds = null;

    if (
      this.sessionSavedAt instanceof Date &&
      !Number.isNaN(this.sessionSavedAt.getTime())
    ) {
      sessionAgeSeconds = Math.max(
        0,
        Math.floor(
          (now - this.sessionSavedAt.getTime()) / 1000
        )
      );
    }

    let nextValidationSeconds = null;

    if (
      this.nextValidationTime &&
      this.state !== STATES.AUTH_REQUIRED
    ) {
      nextValidationSeconds = Math.max(
        0,
        Math.floor(
          (this.nextValidationTime - now) / 1000
        )
      );
    }

    return {
      state: this.state,

      authenticated:
        this.state === STATES.AUTHENTICATED,

      // Timestamp of the most recent successful live
      // Amazon validation.
      lastValidated:
        this.lastValidated
          ? this.lastValidated.toISOString()
          : null,

      // Timestamp of the most recent validation attempt,
      // successful or unsuccessful.
      lastValidationAttempt:
        this.lastValidationAttempt
          ? this.lastValidationAttempt.toISOString()
          : null,

      // Timestamp when the currently stored Amazon
      // authentication payload was persisted.
      //
      // This is age information, NOT validity information.
      sessionSavedAt:
        this.sessionSavedAt
          ? this.sessionSavedAt.toISOString()
          : null,

      sessionAgeSeconds:
        sessionAgeSeconds,

      nextValidationSeconds:
        nextValidationSeconds,

      validationInProgress:
        this.validationInProgress,

      watchdogIntervalSeconds:
        Math.floor(
          WATCHDOG_INTERVAL_MS / 1000
        ),

      lastError:
        this.lastError,
    };
  }

  boot() {
    logger.info(
      '[AuthManager] Starting boot pass...'
    );

    this.startWatchdog();

    if (!fs.existsSync(SESSION_FILE)) {
      logger.info(
        '[AuthManager] No session file found in /data.'
      );

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

      const sessionData =
        JSON.parse(rawData);

      if (sessionData.savedAt) {
        const parsedSavedAt =
          new Date(sessionData.savedAt);

        if (
          !Number.isNaN(
            parsedSavedAt.getTime()
          )
        ) {
          this.sessionSavedAt =
            parsedSavedAt;
        } else {
          logger.warn(
            '[AuthManager] session.json contains an invalid savedAt timestamp.'
          );
        }
      }

      logger.info(
        '[AuthManager] Session file successfully parsed. Attempting restore...'
      );

      this.restoreSession(
        sessionData
      );

    } catch (err) {
      logger.error(
        '[AuthManager] Failed to read/parse /data/session.json:',
        {
          error: err.message
        }
      );

      this.lastError =
        'Corrupt session file in /data';

      this.setState(
        STATES.AUTH_REQUIRED,
        'session file corrupt'
      );
    }
  }

  startWatchdog() {
    if (this.watchdogTimer) {
      clearInterval(
        this.watchdogTimer
      );

      this.watchdogTimer = null;
    }

    logger.info(
      `[AuthManager] Initializing Amazon Session Watchdog ` +
      `(${WATCHDOG_INTERVAL_MS / 60000}m interval)...`
    );

    this.nextValidationTime =
      Date.now() +
      WATCHDOG_INTERVAL_MS;

    this.watchdogTimer =
      setInterval(() => {

        this.nextValidationTime =
          Date.now() +
          WATCHDOG_INTERVAL_MS;

        if (
          this.state !== STATES.AUTHENTICATED &&
          this.state !== STATES.DEGRADED
        ) {
          logger.debug(
            `[AuthManager] Watchdog tick skipped; current state is ${this.state}.`
          );

          return;
        }

        if (this.validationInProgress) {
          logger.warn(
            '[AuthManager] Watchdog tick skipped because an Amazon session validation is already in progress.'
          );

          return;
        }

        logger.info(
          '[AuthManager] Watchdog timer triggered proactive session revalidation...'
        );

        this.validateActiveSession();

      }, WATCHDOG_INTERVAL_MS);
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
     * The successful authentication payload saved by
     * saveSession() contains the localCookie field that
     * must be supplied as cookie.
     */
    const registrationData =
      sessionData.formerRegistrationData ||
      sessionData;

    const initConfig = {
      amazonPage: 'amazon.com',
      baseAmazonPage: 'amazon.com',
      amazonPageProxyLanguage: 'en_US',
      acceptLanguage: 'en-US',
      userLanguage: 'en-US',

      formerRegistrationData:
        registrationData,

      cookie:
        registrationData.localCookie,

      useLocalCookie:
        true,

      alexaServiceHost:
        'alexa.amazon.com',
    };

    logger.info(
      '[AuthManager] Passing formerRegistrationData and saved cookie into alexaWrapper.init()...'
    );

    alexaWrapper.init(
      initConfig,
      (err) => {

        if (err) {

          const isNetworkErr =
            err.code === 'EHOSTUNREACH' ||
            err.code === 'ETIMEDOUT' ||
            err.code === 'ENOTFOUND' ||
            err.code === 'EAI_AGAIN' ||
            err.message?.toLowerCase().includes(
              'timeout'
            );

          if (isNetworkErr) {

            logger.warn(
              '[AuthManager] Network timeout/error during restore. Preserving credentials.',
              {
                error: err.message
              }
            );

            this.lastError =
              'Network or DNS error during Amazon sync';

            this.setState(
              STATES.DEGRADED,
              'network error'
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
      }
    );
  }

  validateActiveSession() {

    if (this.validationInProgress) {
      logger.debug(
        '[AuthManager] Validation request ignored because another validation is already in progress.'
      );

      return;
    }

    this.validationInProgress =
      true;

    this.lastValidationAttempt =
      new Date();

    logger.info(
      '[AuthManager] Executing explicit session validation...'
    );

    alexaWrapper.validateSession(
      (authenticated, err) => {

        this.validationInProgress =
          false;

        if (authenticated) {

          logger.info(
            '[AuthManager] Amazon session explicitly VALIDATED!'
          );

          this.lastValidated =
            new Date();

          this.lastError =
            null;

          this.setState(
            STATES.AUTHENTICATED,
            'session verified via Amazon'
          );

          return;
        }

        /*
         * A network failure is not evidence that the
         * Amazon session is invalid.
         *
         * Preserve the credentials and report DEGRADED.
         */
        if (
          err &&
          (
            err.code === 'ETIMEDOUT' ||
            err.code === 'EHOSTUNREACH' ||
            err.code === 'ENOTFOUND' ||
            err.code === 'EAI_AGAIN' ||
            err.code === 'ECONNRESET' ||
            err.code === 'ECONNREFUSED'
          )
        ) {

          logger.warn(
            '[AuthManager] Validation check encountered a network error. Retaining session.',
            {
              error: err?.message
            }
          );

          this.lastError =
            'Validation check failed due to network/DNS error';

          this.setState(
            STATES.DEGRADED,
            'validation network error'
          );

          return;
        }

        /*
         * A failed authentication response is treated as
         * a real session failure.
         *
         * We do NOT infer expiration from session age.
         */
        logger.warn(
          '[AuthManager] Validation returned FALSE (authentication rejected by Amazon). Session required.',
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
    );
  }

  startProxyAuth() {

    if (
      this.state ===
      STATES.AUTHENTICATING
    ) {

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

    this.nextValidationTime =
      null;

    const proxyHost =
      process.env.PROXY_PUBLIC_HOST ||
      '192.168.1.12';

    const proxyPort =
      parseInt(
        process.env.PROXY_PORT ||
        '4001',
        10
      );

    logger.info(
      `[AuthManager] Starting proxy listener on ${proxyHost}:${proxyPort} (US English Forced)...`
    );

    const proxyConfig = {

      amazonPage:
        'amazon.com',

      baseAmazonPage:
        'amazon.com',

      amazonPageProxyLanguage:
        'en_US',

      acceptLanguage:
        'en-US',

      userLanguage:
        'en-US',

      proxyOnly:
        true,

      proxyOwnIp:
        proxyHost,

      proxyPort:
        proxyPort,

      proxyListenBind:
        '0.0.0.0',

      alexaServiceHost:
        'alexa.amazon.com',

      formerRegistrationData:
        null,

      proxyLogLevel:
        'debug',
    };

    alexaWrapper.init(
      proxyConfig,
      (err, res) => {

        /*
         * alexa-remote2 normally completes proxy
         * authentication with res === undefined.
         *
         * The authentication payload is stored
         * internally by the AlexaRemote instance.
         */
        if (err) {

          const errStr =
            err.message ||
            String(err);

          const isProxyListeningMsg =
            errStr.includes(
              'open this URL'
            ) ||
            errStr.includes(
              'Proxy listening'
            ) ||
            errStr.includes(
              String(proxyPort)
            ) ||
            errStr.includes(
              'http://'
            );

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

          this.lastError =
            errStr;

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
         * Retrieve the complete authentication
         * payload from the AlexaRemote instance.
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
         * The complete formerRegistrationData object
         * is what alexa-remote2 expects when restoring
         * a session.
         */
        if (
          sessionData &&
          sessionData.formerRegistrationData
        ) {

          logger.info(
            '[AuthManager] SUCCESS: Captured Amazon authentication payload!'
          );

          const saved =
            this.saveSession(
              sessionData
            );

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
           * Verify the live authenticated session before
           * declaring the server AUTHENTICATED.
           */
          this.validateActiveSession();

          return;
        }

        /*
         * No error was returned, but the expected
         * authentication payload was not found.
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

      const now =
        new Date();

      const dataToPersist = {

        formerRegistrationData:
          sessionData.formerRegistrationData,

        savedAt:
          now.toISOString(),
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

      // Keep the in-memory status synchronized
      // with the persisted session timestamp.
      this.sessionSavedAt =
        now;

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

module.exports =
  new AuthManager();