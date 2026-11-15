import { Router } from 'express';
import { timingSafeEqual } from 'node:crypto';
import { config } from '../config.js';
import { deviceStore } from '../device-store.js';
import { handleAlexaDirective } from '../alexa.js';
const router = Router();

function requireApiKey(req, res, next) {
  const supplied = req.get('authorization') || '';
  const expected = `Bearer ${config.apiKey}`;
  const a = Buffer.from(supplied); const b = Buffer.from(expected);
  if (!config.apiKey || a.length !== b.length || !timingSafeEqual(a, b)) return res.status(401).json({ error: 'unauthorized' });
  next();
}

router.get('/health', (_req, res) => res.json({
  service: 'alexa-manager-advanced', version: '0.4.0', status: 'ok', timestamp: new Date().toISOString()
}));

router.get('/v1/status', requireApiKey, async (_req, res, next) => {
  try {
    res.json({
      service: 'alexa-manager-advanced', version: '0.4.0',
      phase: 'hubitat-lan-integration', amazonSkill: 'not-configured',
      directiveInterfaces: ['Alexa.Discovery', 'Alexa.PowerController', 'Alexa.ReportState'],
      backend: 'mixed', deviceCount: (await deviceStore.list()).length,
      hubitatBackend: 'lan-push-and-command-callback'
    });
  } catch (error) { next(error); }
});

router.post('/v1/devices', requireApiKey, async (req, res, next) => {
  try {
    const device = await deviceStore.create(req.body || {});
    res.status(201).json(device);
  } catch (error) {
    if (error instanceof TypeError || error instanceof RangeError || error.message === 'endpointId already exists') return res.status(400).json({ error: 'invalid_device', message: error.message });
    next(error);
  }
});

router.get('/v1/devices', requireApiKey, async (_req, res, next) => {
  try { res.json({ devices: await deviceStore.list() }); } catch (error) { next(error); }
});

// Hubitat app registration and state synchronization (LAN-only integration).
router.put('/v1/devices/:endpointId', requireApiKey, async (req, res, next) => {
  try {
    if (req.params.endpointId !== req.body?.endpointId) return res.status(400).json({ error: 'endpoint_mismatch' });
    const device = await deviceStore.registerHubitat(req.body || {});
    res.status(200).json(device);
  } catch (error) {
    if (error instanceof TypeError || error instanceof RangeError) return res.status(400).json({ error: 'invalid_device', message: error.message });
    next(error);
  }
});
router.put('/v1/devices/:endpointId/state', requireApiKey, async (req, res, next) => {
  try {
    const device = await deviceStore.updatePower(req.params.endpointId, req.body?.powerState);
    return device ? res.json({ endpointId: device.endpointId, powerState: device.powerState, updatedAt: device.updatedAt }) : res.status(404).json({ error: 'not_found' });
  } catch (error) {
    if (error instanceof TypeError) return res.status(400).json({ error: 'invalid_state' });
    next(error);
  }
});

router.delete('/v1/devices/:endpointId', requireApiKey, async (req, res, next) => {
  try {
    const removed = await deviceStore.remove(req.params.endpointId);
    return removed ? res.status(204).end() : res.status(404).json({ error: 'not_found' });
  } catch (error) { next(error); }
});

// Lambda relay entry point. The API key authenticates the relay; the Alexa bearer token authenticates the linked user.
router.post('/v1/alexa/directive', requireApiKey, async (req, res, next) => {
  try { res.json(await handleAlexaDirective(req.body)); } catch (error) { next(error); }
});
export default router;
