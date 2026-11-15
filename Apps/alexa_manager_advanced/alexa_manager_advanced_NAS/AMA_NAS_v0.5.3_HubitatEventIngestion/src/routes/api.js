import { Router } from 'express';
import { timingSafeEqual } from 'node:crypto';
import { config } from '../config.js';
import { deviceStore } from '../device-store.js';
import { handleAlexaDirective } from '../alexa.js';
import { influxStatus, testInfluxWrite, recordTelemetry } from '../influx.js';
const router = Router();

function requireApiKey(req, res, next) {
  const supplied = req.get('authorization') || '';
  const expected = `Bearer ${config.apiKey}`;
  const a = Buffer.from(supplied); const b = Buffer.from(expected);
  if (!config.apiKey || a.length !== b.length || !timingSafeEqual(a, b)) return res.status(401).json({ error: 'unauthorized' });
  next();
}

router.get('/health', (_req, res) => res.json({
  service: 'alexa-manager-advanced', version: config.appVersion, status: 'ok', timestamp: new Date().toISOString()
}));

router.get('/v1/status', requireApiKey, async (_req, res, next) => {
  try {
    res.json({
      service: 'alexa-manager-advanced', version: config.appVersion,
      phase: 'hubitat-lan-integration', amazonSkill: 'not-configured',
      directiveInterfaces: ['Alexa.Discovery', 'Alexa.PowerController', 'Alexa.ReportState'],
      backend: 'mixed', deviceCount: (await deviceStore.list()).length,
      hubitatBackend: 'lan-push-and-command-callback',
      telemetry: influxStatus()
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
    recordTelemetry('hubitat_device_registered', { endpoint_id: device.endpointId, backend: device.backend });
    res.status(200).json(device);
  } catch (error) {
    if (error instanceof TypeError || error instanceof RangeError) return res.status(400).json({ error: 'invalid_device', message: error.message });
    next(error);
  }
});
router.put('/v1/devices/:endpointId/state', requireApiKey, async (req, res, next) => {
  try {
    const device = await deviceStore.updatePower(req.params.endpointId, req.body?.powerState);
    if (device) recordTelemetry('hubitat_device_state_updated', { endpoint_id: device.endpointId, power_state: device.powerState });
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
  try {
    const result = await handleAlexaDirective(req.body);
    const directive = req.body?.directive;
    recordTelemetry('alexa_directive', {
      namespace: directive?.header?.namespace || 'unknown',
      directive_name: directive?.header?.name || 'unknown',
      response_name: result?.event?.header?.name || 'unknown',
      error_type: result?.event?.payload?.type || 'none'
    });
    res.json(result);
  } catch (error) { next(error); }
});


// Hubitat-to-NAS telemetry intake. The Hubitat Parent authenticates this request;
// the NAS owns normalization and the InfluxDB schema.
router.post('/v1/telemetry/hubitat-event', requireApiKey, (req, res) => {
  try {
    const event = req.body || {};
    const eventName = String(event.event || event.eventName || 'hubitat_event').slice(0, 64);
    const receivedPayload = event.payload && typeof event.payload === 'object' && !Array.isArray(event.payload)
      ? event.payload : {};
    const payload = receivedPayload.payload && typeof receivedPayload.payload === 'object' && !Array.isArray(receivedPayload.payload)
      ? receivedPayload.payload : receivedPayload;
    const endpointId = payload.endpointId || payload.endpoint_id || event.endpointId || event.endpoint_id;
    const hubitatDeviceId = payload.hubitatDeviceId || payload.hubitat_device_id ||
      (typeof endpointId === 'string' && endpointId.startsWith('hubitat-') ? endpointId.slice(8) : undefined);
    const deviceName = payload.deviceName || payload.device_name;
    const attribute = payload.attribute || payload.name;
    const value = payload.value;
    const powerState = payload.powerState || payload.power_state ||
      (attribute === 'switch' && value != null ? String(value).toUpperCase() : undefined);
    const eventTimestamp = receivedPayload.eventTimestamp || payload.timestamp || payload.eventTimestamp || event.timestamp;

    recordTelemetry('hubitat_event', {
      hubitat_device_id: hubitatDeviceId == null ? undefined : String(hubitatDeviceId),
      device_name: deviceName == null ? undefined : String(deviceName),
      attribute: attribute == null ? undefined : String(attribute),
      value: value == null ? undefined : String(value),
      power_state: powerState == null ? undefined : String(powerState).toUpperCase(),
      event_name: eventName,
      event_timestamp: eventTimestamp == null ? undefined : String(eventTimestamp),
      payload: JSON.stringify(event).slice(0, 4000)
    }, {
      endpoint_id: endpointId == null ? undefined : String(endpointId),
      origin: 'hubitat'
    });
    res.status(202).json({ accepted: true, event: eventName, endpointId: endpointId || null });
  } catch (error) {
    res.status(400).json({ error: 'invalid_telemetry_event', message: error.message });
  }
});

// NAS-to-InfluxDB telemetry controls. Test writes are independent of continuous reporting.
router.get('/v1/telemetry/status', requireApiKey, (_req, res) => {
  res.json(influxStatus());
});
router.post('/v1/telemetry/test', requireApiKey, async (_req, res) => {
  const result = await testInfluxWrite();
  res.status(result.success ? 200 : 502).json(result);
});
export default router;
