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

// GET /auth/status (Preserved)
app.get('/auth/status', (req, res) => {
  const status = authManager.getStatus();
  logger.info(`[Server] GET /auth/status requested from ${req.ip} -> State: ${status.state}`);
  res.json(status);
});

// POST /auth/login (Preserved)
app.post('/auth/login', (req, res) => {
  logger.info(`[Server] POST /auth/login received from ${req.ip}`);
  const currentStatus = authManager.getStatus();
  if (currentStatus.state === 'AUTHENTICATING') {
    return res.json({
      message: 'Proxy login already in progress.',
      proxyUrl: `http://${PROXY_HOST}:${PROXY_PORT}`,
      status: currentStatus,
    });
  }
  authManager.startProxyAuth();
  res.json({
    message: 'Proxy authentication initialized.',
    proxyUrl: `http://${PROXY_HOST}:${PROXY_PORT}`,
    instructions: `Open http://${PROXY_HOST}:${PROXY_PORT} in a browser and complete Amazon login.`,
  });
});

// GET /api/devices
app.get('/api/devices', (req, res) => {
  const status = authManager.getStatus();
  if (!status.authenticated) {
    return res.status(401).json({ success: false, error: 'Bridge server is not authenticated with Amazon.' });
  }

  commandDispatcher.getDevices((err, devices) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }
    res.json({ success: true, count: devices.length, devices });
  });
});

// POST /api/command
app.post('/api/command', (req, res) => {
  const status = authManager.getStatus();
  if (!status.authenticated) {
    return res.status(401).json({ success: false, error: 'Bridge server is not authenticated with Amazon.' });
  }

  const { serialNumber, command, payload } = req.body;

  if (!serialNumber && command !== 'announceAll') {
    return res.status(400).json({ success: false, error: "Missing 'serialNumber' in request body." });
  }
  if (!command) {
    return res.status(400).json({ success: false, error: "Missing 'command' in request body." });
  }

  commandDispatcher.dispatch(serialNumber, command, payload || {}, (err, result) => {
    if (err) {
      return res.status(400).json({ success: false, error: err.message });
    }
    res.json({ success: true, serialNumber, command, result: result || 'Command executed successfully.' });
  });
});

app.listen(PORT, () => {
  logger.info(`==================================================`);
  logger.info(`Echos Speak Server running on port ${PORT}`);
  logger.info(`Configured Proxy Public Host: http://${PROXY_HOST}:${PROXY_PORT}`);
  logger.info(`==================================================`);
  authManager.boot();
});