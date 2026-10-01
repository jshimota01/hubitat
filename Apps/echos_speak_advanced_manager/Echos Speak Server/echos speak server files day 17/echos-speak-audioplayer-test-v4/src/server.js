// Version: 1.1.3

const express = require('express');
const authManager = require('./authManager');
const commandDispatcher = require('./commandDispatcher');
const logger = require('./logger');

const app = express();
const PORT = process.env.PORT || 4000;
const PROXY_PORT = process.env.PROXY_PORT || 4001;
const PROXY_HOST = process.env.PROXY_PUBLIC_HOST || '192.168.1.12';

app.use(express.json());

app.use((req, res, next) => {
  res.removeHeader('Access-Control-Allow-Origin');
  next();
});

// ============================================================
// Authentication
// ============================================================

app.get('/auth/status', (req, res) => {
  const status = authManager.getStatus();

  logger.info(
    `[Server] GET /auth/status requested from ${req.ip} -> State: ${status.state}`
  );

  res.json(status);
});

app.post('/auth/login', (req, res) => {
  logger.info(`[Server] POST /auth/login received from ${req.ip}`);

  const currentStatus = authManager.getStatus();

  if (currentStatus.state === 'AUTHENTICATING') {
    logger.info(
      '[Server] Proxy login already in progress. Returning existing proxy URL...'
    );

    return res.json({
      message: 'Proxy login already in progress.',
      proxyUrl: `http://${PROXY_HOST}:${PROXY_PORT}`,
      status: currentStatus,
    });
  }

  logger.info('[Server] Triggering authManager.startProxyAuth()...');
  authManager.startProxyAuth();

  res.json({
    message: 'Proxy authentication initialized.',
    proxyUrl: `http://${PROXY_HOST}:${PROXY_PORT}`,
    instructions:
      `Open http://${PROXY_HOST}:${PROXY_PORT} in a browser and complete Amazon login.`,
  });
});

// ============================================================
// Device Discovery
// ============================================================

app.get('/api/devices', (req, res) => {
  const status = authManager.getStatus();

  if (!status.authenticated) {
    return res.status(401).json({
      success: false,
      error: 'Bridge server is not authenticated with Amazon.',
    });
  }

  commandDispatcher.getDevices((err, devices) => {
    if (err) {
      logger.error('[Server] Device discovery failed:', {
        error: err.message,
      });

      return res.status(500).json({
        success: false,
        error: err.message,
      });
    }

    res.json({
      success: true,
      count: devices.length,
      devices,
    });
  });
});


// ============================================================
// Temporary Music Provider Diagnostic
// ============================================================

app.get('/debug/music-providers', (req, res) => {

  const alexa = authManager.getAlexa();

  alexa.getMusicProviders((err, result) => {

    if (err) {
      return res.status(500).json({
        success: false,
        error: err.message
      });
    }

    res.json({
      success: true,
      providers: result
    });

  });

});

// ============================================================
// Playback State Query
// ============================================================
// Read-only convenience endpoint. Uses the same authoritative
// getPlaybackState() path exposed through POST /api/command.

app.get('/api/media/:serialNumber', (req, res) => {
  const status = authManager.getStatus();

  if (!status.authenticated) {
    return res.status(401).json({
      success: false,
      error: 'Bridge server is not authenticated with Amazon.',
    });
  }

  const serialNumber = req.params.serialNumber;

  commandDispatcher.getPlaybackState(serialNumber, (err, result) => {
    if (err) {
      logger.error('[Server] Playback state query failed:', {
        serialNumber,
        errorCode: err.code || null,
        error: err.message,
      });

      return res.status(err.code === 'COMMAND_TIMEOUT' ? 504 : 400).json({
        success: false,
        error: err.message,
      });
    }

    res.json({
      success: true,
      serialNumber,
      result,
    });
  });
});

// ============================================================
// Command API
// ============================================================

app.post('/api/command', (req, res) => {
  const status = authManager.getStatus();

  if (!status.authenticated) {
    return res.status(401).json({
      success: false,
      error: 'Bridge server is not authenticated with Amazon.',
    });
  }

  const { serialNumber, command, payload } = req.body;

  if (!command) {
    return res.status(400).json({
      success: false,
      error: "Missing 'command' in request body.",
    });
  }

  if (!serialNumber && command !== 'announceAll') {
    return res.status(400).json({
      success: false,
      error: "Missing 'serialNumber' in request body.",
    });
  }

  // Volume commands are asynchronous by design.
  //
  // The existing dispatcher performs the actual Amazon operation in
  // the background. Hubitat should not wait for that operation to
  // finish. Final volume state is returned through /api/devices.
  //
  // We still detect synchronous validation errors so malformed volume
  // requests can receive an appropriate HTTP error immediately.
  if (
    command === 'volume' ||
    command === 'volumeUp' ||
    command === 'volumeDown'
  ) {
    let synchronousError = null;
    let responseSent = false;

    commandDispatcher.dispatch(
      serialNumber,
      command,
      payload || {},
      (err, result) => {
        if (responseSent) {
          if (err) {
            logger.error(
              '[Server] Volume command failed after HTTP acknowledgement:',
              {
                command,
                serialNumber,
                errorCode: err.code || null,
                error: err.message,
              }
            );
          }
          return;
        }

        // Dispatcher returned synchronously. Preserve validation errors.
        if (err) {
          synchronousError = err;
        }
      }
    );

    if (synchronousError) {
      return res.status(
        synchronousError.code === 'COMMAND_TIMEOUT' ? 504 : 400
      ).json({
        success: false,
        error: synchronousError.message,
      });
    }

    responseSent = true;

    return res.json({
      success: true,
      serialNumber,
      command,
      status: 'pending',
      result: 'Volume command accepted.',
    });
  }

  commandDispatcher.dispatch(
    serialNumber,
    command,
    payload || {},
    (err, result) => {
      if (err) {
        logger.error('[Server] Command dispatch failed:', {
          command,
          serialNumber,
          errorCode: err.code || null,
          error: err.message,
        });

        return res.status(err.code === 'COMMAND_TIMEOUT' ? 504 : 400).json({
          success: false,
          error: err.message,
        });
      }

      res.json({
        success: true,
        serialNumber,
        command,
        result: result || 'Command executed successfully.',
      });
    }
  );
});

// ============================================================
// Server Startup
// ============================================================



// ============================================================
// Temporary AudioPlayer URL Diagnostic
// ============================================================

app.get('/debug/audioplayer-test', (req, res) => {

  const alexa = authManager.getAlexa();

  const serial = 'G6G22N063482007T';

  const url = 'http://192.168.1.20:32400/library/parts/328046/1787426940/file.mp3';

  const command = {
    '@type': 'com.amazon.alexa.behaviors.model.Sequence',
    'startNode': {
      '@type': 'com.amazon.alexa.behaviors.model.OpaquePayloadOperationNode',
      'type': 'Alexa.AudioPlayer.Play',
      'operationPayload': JSON.stringify({
        'url': url
      })
    }
  };

  alexa.sendSequenceCommand(
    serial,
    command,
    null,
    null,
    (err, result) => {

      if (err) {
        return res.status(500).json({
          success: false,
          error: err.message,
          code: err.code || null,
          name: err.name || null,
          response: err.response || null,
          statusCode: err.statusCode || null,
          body: err.body || null,
          stack: err.stack || null
        });
      }

      res.json({
        success: true,
        result
      });

    }
  );

});


const server = app.listen(PORT, () => {
  logger.info('==================================================');
  logger.info(`Echos Speak Server running on port ${PORT}`);
  logger.info(
    `Configured Proxy Public Host: http://${PROXY_HOST}:${PROXY_PORT}`
  );
  logger.info('==================================================');

  authManager.boot();
});

const HUB_HTTP_TIMEOUT_MS = 5000;

// Hubitat -> Echos Speak HTTP boundary.
// Keep this shorter than the Amazon command watchdog so a stalled
// Amazon operation cannot hold a Hubitat HTTP request indefinitely.
server.setTimeout(HUB_HTTP_TIMEOUT_MS);
server.requestTimeout = HUB_HTTP_TIMEOUT_MS;
server.headersTimeout = HUB_HTTP_TIMEOUT_MS + 1000;

logger.info('[Server] HTTP request timeout configured', {
  timeoutMs: HUB_HTTP_TIMEOUT_MS
});