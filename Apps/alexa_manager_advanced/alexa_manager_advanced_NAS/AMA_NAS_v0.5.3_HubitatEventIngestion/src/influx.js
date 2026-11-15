import { config } from './config.js';
import { log } from './logger.js';

let lastWriteAt = null;
let lastError = null;
let failureCount = 0;
let successCount = 0;
let queue = Promise.resolve();

const escapeTag = value => String(value ?? '')
  .replace(/\\/g, '\\\\').replace(/,/g, '\\,').replace(/ /g, '\\ ').replace(/=/g, '\\=');
const escapeFieldString = value => JSON.stringify(String(value ?? '').slice(0, 512));

function lineProtocol(measurement, tags, fields, timestampNs) {
  const tagText = Object.entries(tags || {})
    .filter(([, value]) => value !== undefined && value !== null && String(value) !== '')
    .map(([key, value]) => `,${escapeTag(key)}=${escapeTag(value)}`).join('');
  const fieldText = Object.entries(fields || {})
    .filter(([, value]) => value !== undefined && value !== null)
    .map(([key, value]) => {
      if (typeof value === 'boolean') return `${escapeTag(key)}=${value}`;
      if (typeof value === 'number' && Number.isFinite(value)) return `${escapeTag(key)}=${value}`;
      return `${escapeTag(key)}=${escapeFieldString(value)}`;
    }).join(',');
  if (!fieldText) throw new TypeError('InfluxDB telemetry event requires at least one field');
  return `${escapeTag(measurement)}${tagText} ${fieldText} ${timestampNs}`;
}

function endpoint() {
  const url = new URL('/api/v2/write', config.influxUrl);
  url.searchParams.set('org', config.influxOrg);
  url.searchParams.set('bucket', config.influxBucket);
  url.searchParams.set('precision', 'ns');
  return url;
}

async function sendLine(line, { test = false } = {}) {
  if (!config.influxUrl || !config.influxOrg || !config.influxBucket || !config.influxToken) {
    throw new Error('InfluxDB is not fully configured; set INFLUX_URL, INFLUX_ORG, INFLUX_BUCKET, and INFLUX_TOKEN.');
  }
  const response = await fetch(endpoint(), {
    method: 'POST',
    headers: {
      Authorization: `Token ${config.influxToken}`,
      'Content-Type': 'text/plain; charset=utf-8',
      Accept: 'application/json'
    },
    body: line,
    signal: AbortSignal.timeout(8000)
  });
  if (!response.ok) {
    const body = (await response.text().catch(() => '')).slice(0, 400);
    throw new Error(`InfluxDB returned HTTP ${response.status}${body ? `: ${body}` : ''}`);
  }
  lastWriteAt = new Date().toISOString();
  lastError = null;
  successCount += 1;
  if (test) log('info', `InfluxDB connection test SUCCESS (HTTP ${response.status}); test point accepted by bucket '${config.influxBucket}'.`);
  return { success: true, status: response.status, timestamp: lastWriteAt };
}

export function influxStatus() {
  return {
    enabled: config.influxEnabled,
    configured: Boolean(config.influxUrl && config.influxOrg && config.influxBucket && config.influxToken),
    organization: config.influxOrg || null,
    bucket: config.influxBucket || null,
    lastWriteAt,
    lastError,
    failureCount,
    successCount
  };
}

export async function testInfluxWrite() {
  const timestampNs = BigInt(Date.now()) * 1000000n;
  const line = lineProtocol('ama_nas', { source: 'synology_nas', kind: 'connection_test' }, {
    event: 'influx_connection_test',
    service: 'alexa-manager-advanced',
    app_version: config.appVersion
  }, timestampNs);
  try {
    const result = await sendLine(line, { test: true });
    return { ...result, status: 'SUCCESS' };
  } catch (error) {
    lastError = String(error?.message || error).slice(0, 400);
    failureCount += 1;
    log('warn', `InfluxDB connection test FAILED: ${lastError}`);
    return { success: false, status: 'FAILED', error: lastError };
  }
}

export function recordTelemetry(event, fields = {}, tags = {}) {
  if (!config.influxEnabled) return;
  const operation = queue.then(async () => {
    const timestampNs = BigInt(Date.now()) * 1000000n;
    const line = lineProtocol('ama_nas', {
      source: 'synology_nas',
      event: String(event || 'unknown').slice(0, 64),
      ...tags
    }, {
      app_version: config.appVersion,
      ...fields
    }, timestampNs);
    try {
      await sendLine(line);
    } catch (error) {
      lastError = String(error?.message || error).slice(0, 400);
      failureCount += 1;
      log('warn', `InfluxDB telemetry write failed: ${lastError}`);
    }
  });
  queue = operation.catch(error => {
    lastError = String(error?.message || error).slice(0, 400);
    failureCount += 1;
    log('warn', `InfluxDB telemetry queue failed: ${lastError}`);
  });
}
