import { randomUUID } from 'node:crypto';
import { oauthStore } from './oauth-store.js';
import { deviceStore } from './device-store.js';
import { sendHubitatCommand } from './hubitat-client.js';

const now = () => new Date().toISOString();
const property = (namespace, name, value) => ({
  namespace, name, value, timeOfSample: now(), uncertaintyInMilliseconds: 0
});
const responseHeader = (requestHeader, namespace, name) => ({
  namespace, name, messageId: randomUUID(), payloadVersion: '3',
  ...(requestHeader.correlationToken ? { correlationToken: requestHeader.correlationToken } : {})
});

function errorResponse(requestHeader, type, message) {
  return { event: { header: responseHeader(requestHeader, 'Alexa', 'ErrorResponse'), payload: { type, message } } };
}

async function authorize(token) {
  if (typeof token !== 'string' || !token) return null;
  return oauthStore.validateAccessToken(token);
}

function capabilities() {
  return [
    { type: 'AlexaInterface', interface: 'Alexa', version: '3' },
    {
      type: 'AlexaInterface', interface: 'Alexa.PowerController', version: '3',
      properties: { supported: [{ name: 'powerState' }], proactivelyReported: false, retrievable: true }
    },
    {
      type: 'AlexaInterface', interface: 'Alexa.EndpointHealth', version: '3',
      properties: { supported: [{ name: ' connectivity'.trim() }], proactivelyReported: false, retrievable: true }
    }
  ];
}

function discoveryEndpoint(device) {
  return {
    endpointId: device.endpointId,
    manufacturerName: device.manufacturerName,
    friendlyName: device.friendlyName,
    description: device.description,
    displayCategories: device.displayCategories,
    additionalAttributes: { manufacturer: device.manufacturerName, model: device.modelName },
    cookie: {},
    capabilities: capabilities()
  };
}

export async function handleAlexaDirective(body) {
  const directive = body?.directive;
  const header = directive?.header;
  if (!header || typeof header.namespace !== 'string' || typeof header.name !== 'string' || header.payloadVersion !== '3') {
    return errorResponse(header || {}, 'INVALID_DIRECTIVE', 'A valid Smart Home API v3 directive is required.');
  }

  if (header.namespace === 'Alexa.Discovery' && header.name === 'Discover') {
    const token = directive.payload?.scope?.token;
    const auth = await authorize(token);
    if (!auth) return errorResponse(header, 'INVALID_AUTHORIZATION_CREDENTIAL', 'The access token is invalid or expired.');
    const devices = (await deviceStore.list()).filter(device => device.enabled);
    return { event: { header: responseHeader(header, 'Alexa.Discovery', 'Discover.Response'), payload: { endpoints: devices.map(discoveryEndpoint) } } };
  }

  if (header.namespace === 'Alexa.PowerController' && ['TurnOn', 'TurnOff'].includes(header.name)) {
    const endpoint = directive.endpoint;
    const token = endpoint?.scope?.token;
    const auth = await authorize(token);
    if (!auth) return errorResponse(header, 'INVALID_AUTHORIZATION_CREDENTIAL', 'The access token is invalid or expired.');
    if (typeof endpoint?.endpointId !== 'string') return errorResponse(header, 'INVALID_DIRECTIVE', 'The endpointId is missing.');
    const powerState = header.name === 'TurnOn' ? 'ON' : 'OFF';
    let device = await deviceStore.get(endpoint.endpointId);
    if (!device || !device.enabled) return errorResponse(header, 'NO_SUCH_ENDPOINT', 'The endpoint does not exist or is disabled.');
    if (device.backend === 'hubitat') {
      try { await sendHubitatCommand(device, powerState); }
      catch { return errorResponse(header, 'ENDPOINT_UNREACHABLE', 'Hubitat could not be reached to execute the command.'); }
      device = { ...device, powerState };
    } else {
      device = await deviceStore.setPower(endpoint.endpointId, powerState);
    }
    return {
      event: {
        header: responseHeader(header, 'Alexa', 'Response'),
        endpoint: { endpointId: device.endpointId },
        payload: {}
      },
      context: { properties: [property('Alexa.PowerController', 'powerState', device.powerState)] }
    };
  }

  if (header.namespace === 'Alexa' && header.name === 'ReportState') {
    const endpoint = directive.endpoint;
    const auth = await authorize(endpoint?.scope?.token);
    if (!auth) return errorResponse(header, 'INVALID_AUTHORIZATION_CREDENTIAL', 'The access token is invalid or expired.');
    if (typeof endpoint?.endpointId !== 'string') return errorResponse(header, 'INVALID_DIRECTIVE', 'The endpointId is missing.');
    const device = await deviceStore.get(endpoint.endpointId);
    if (!device || !device.enabled) return errorResponse(header, 'NO_SUCH_ENDPOINT', 'The endpoint does not exist or is disabled.');
    return {
      event: {
        header: responseHeader(header, 'Alexa', 'StateReport'),
        endpoint: { endpointId: device.endpointId },
        payload: {}
      },
      context: { properties: [
        property('Alexa.PowerController', 'powerState', device.powerState),
        property('Alexa.EndpointHealth', 'connectivity', { value: 'OK' })
      ] }
    };
  }

  return errorResponse(header, 'INVALID_DIRECTIVE', `Unsupported directive: ${header.namespace}.${header.name}`);
}
