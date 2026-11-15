import test, { after } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createHash, randomBytes } from 'node:crypto';

const testDir = await mkdtemp(join(tmpdir(), 'ama-v040-'));
process.env.DATA_DIR = testDir;
const { oauthStore } = await import('../src/oauth-store.js');
const { deviceStore } = await import('../src/device-store.js');
const { handleAlexaDirective } = await import('../src/alexa.js');

const verifier = randomBytes(32).toString('base64url');
const challenge = createHash('sha256').update(verifier).digest('base64url');
const code = await oauthStore.createCode({ clientId: 'test-client', redirectUri: 'https://test.example/cb', codeChallenge: challenge, scope: 'alexa::all' });
const grant = await oauthStore.redeemCode({ code, clientId: 'test-client', redirectUri: 'https://test.example/cb', verifier });
const tokens = await oauthStore.issueTokens(grant.scope, 'test-client');
const token = tokens.access_token;
const header = (namespace, name) => ({ namespace, name, payloadVersion: '3', messageId: randomBytes(16).toString('hex'), correlationToken: 'test-correlation' });

after(async () => { await rm(testDir, { recursive: true, force: true }); });

test('Discovery returns enabled endpoints for valid bearer token', async () => {
    await deviceStore.create({ endpointId: 'test-switch-1', friendlyName: 'Test Switch', manufacturerName: 'AMA Test', modelName: 'Virtual Switch' });
    const result = await handleAlexaDirective({ directive: { header: header('Alexa.Discovery', 'Discover'), payload: { scope: { type: 'BearerToken', token } } } });
    assert.equal(result.event.header.name, 'Discover.Response');
    assert.equal(result.event.payload.endpoints.length, 1);
    assert.equal(result.event.payload.endpoints[0].endpointId, 'test-switch-1');
    assert.ok(result.event.payload.endpoints[0].capabilities.some(item => item.interface === 'Alexa.PowerController'));
});
test('PowerController TurnOn and TurnOff update local simulation', async () => {
    for (const [name, expected] of [['TurnOn', 'ON'], ['TurnOff', 'OFF']]) {
      const result = await handleAlexaDirective({ directive: { header: header('Alexa.PowerController', name), endpoint: { endpointId: 'test-switch-1', scope: { type: 'BearerToken', token } }, payload: {} } });
      assert.equal(result.event.header.name, 'Response');
      assert.equal(result.context.properties[0].value, expected);
    }
});
test('ReportState returns power and connectivity', async () => {
    const result = await handleAlexaDirective({ directive: { header: header('Alexa', 'ReportState'), endpoint: { endpointId: 'test-switch-1', scope: { type: 'BearerToken', token } }, payload: {} } });
    assert.equal(result.event.header.name, 'StateReport');
    assert.equal(result.context.properties.find(item => item.name === 'powerState').value, 'OFF');
    assert.equal(result.context.properties.find(item => item.name === 'connectivity').value.value, 'OK');
});
test('Invalid bearer token is rejected', async () => {
    const result = await handleAlexaDirective({ directive: { header: header('Alexa.Discovery', 'Discover'), payload: { scope: { type: 'BearerToken', token: 'bad-token' } } } });
    assert.equal(result.event.header.name, 'ErrorResponse');
    assert.equal(result.event.payload.type, 'INVALID_AUTHORIZATION_CREDENTIAL');
});

test('Hubitat-backed switch forwards Alexa power commands to its local callback', async () => {
    await deviceStore.registerHubitat({
      endpointId: 'hubitat-123', hubitatDeviceId: '123', friendlyName: 'Hubitat Test Switch',
      commandUrl: 'http://192.168.1.50/apps/api/99/command?access_token=test', powerState: 'OFF'
    });
    const originalFetch = globalThis.fetch;
    let called = null;
    globalThis.fetch = async (url, options) => {
      called = { url, body: JSON.parse(options.body) };
      return { ok: true, status: 202 };
    };
    try {
      const result = await handleAlexaDirective({ directive: {
        header: header('Alexa.PowerController', 'TurnOn'),
        endpoint: { endpointId: 'hubitat-123', scope: { type: 'BearerToken', token } }, payload: {}
      } });
      assert.equal(called.url, 'http://192.168.1.50/apps/api/99/command?access_token=test');
      assert.equal(called.body.command, 'ON');
      assert.equal(result.context.properties[0].value, 'ON');
    } finally { globalThis.fetch = originalFetch; }
});
