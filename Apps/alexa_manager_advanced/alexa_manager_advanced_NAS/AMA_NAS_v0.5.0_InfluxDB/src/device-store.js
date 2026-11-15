import { mkdir, readFile, writeFile, rename } from 'node:fs/promises';
import { dirname } from 'node:path';
import { randomUUID } from 'node:crypto';
import { config } from './config.js';

const file = config.deviceFile;
let queue = Promise.resolve();

async function readDevices() {
  try {
    const parsed = JSON.parse(await readFile(file, 'utf8'));
    return Array.isArray(parsed.devices) ? parsed.devices : [];
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }
}
async function writeDevices(devices) {
  await mkdir(dirname(file), { recursive: true });
  const temp = `${file}.tmp`;
  await writeFile(temp, JSON.stringify({ devices }, null, 2), { mode: 0o600 });
  await rename(temp, file);
}
async function mutate(fn) {
  const operation = queue.then(async () => {
    const devices = await readDevices();
    const result = await fn(devices);
    await writeDevices(devices);
    return result;
  });
  queue = operation.catch(() => {});
  return operation;
}

function cleanText(value, name, max = 128) {
  if (typeof value !== 'string' || !value.trim() || value.length > max) throw new TypeError(`Invalid ${name}`);
  return value.trim();
}

export const deviceStore = {
  async list() { return readDevices(); },
  async get(endpointId) { return (await readDevices()).find(device => device.endpointId === endpointId) || null; },
  async create(input = {}) {
    return mutate(devices => {
      if (devices.length >= 300) throw new RangeError('Maximum of 300 endpoints reached');
      const endpointId = input.endpointId ? cleanText(input.endpointId, 'endpointId', 256) : `ama-${randomUUID()}`;
      if (!/^[A-Za-z0-9 _\-=#;:?@&]+$/.test(endpointId)) throw new TypeError('Invalid endpointId characters');
      if (devices.some(device => device.endpointId === endpointId)) throw new Error('endpointId already exists');
      const device = {
        endpointId,
        friendlyName: cleanText(input.friendlyName, 'friendlyName', 128),
        description: typeof input.description === 'string' ? input.description.slice(0, 256) : 'Alexa Manager Advanced managed device',
        manufacturerName: typeof input.manufacturerName === 'string' && input.manufacturerName.trim() ? input.manufacturerName.trim().slice(0, 128) : 'Unspecified',
        modelName: typeof input.modelName === 'string' && input.modelName.trim() ? input.modelName.trim().slice(0, 128) : 'Unspecified',
        displayCategories: Array.isArray(input.displayCategories) && input.displayCategories.length ? input.displayCategories.slice(0, 5) : ['SWITCH'],
        enabled: input.enabled !== false,
        powerState: input.powerState === 'ON' ? 'ON' : 'OFF',
        backend: 'local-simulation'
      };
      devices.push(device);
      return device;
    });
  },
  async registerHubitat(input = {}) {
    return mutate(devices => {
      const endpointId = cleanText(input.endpointId, 'endpointId', 256);
      if (!/^hubitat-[A-Za-z0-9_-]+$/.test(endpointId)) throw new TypeError('Invalid Hubitat endpointId');
      const friendlyName = cleanText(input.friendlyName, 'friendlyName', 128);
      const commandUrl = cleanText(input.commandUrl, 'commandUrl', 2048);
      const parsed = new URL(commandUrl);
      if (!['http:', 'https:'].includes(parsed.protocol) || parsed.username || parsed.password) throw new TypeError('Invalid commandUrl');
      const powerState = input.powerState === 'ON' ? 'ON' : 'OFF';
      let device = devices.find(item => item.endpointId === endpointId);
      const record = {
        endpointId, friendlyName,
        description: typeof input.description === 'string' ? input.description.slice(0, 256) : 'Hubitat-managed switch',
        manufacturerName: 'Hubitat', modelName: typeof input.modelName === 'string' ? input.modelName.slice(0, 128) : 'Hubitat Device',
        displayCategories: ['SWITCH'], enabled: input.enabled !== false, powerState,
        backend: 'hubitat', hubitatDeviceId: cleanText(String(input.hubitatDeviceId), 'hubitatDeviceId', 64),
        commandUrl, updatedAt: new Date().toISOString()
      };
      if (device) Object.assign(device, record); else { device = record; devices.push(device); }
      return device;
    });
  },
  async updatePower(endpointId, powerState) {
    if (!['ON', 'OFF'].includes(powerState)) throw new TypeError('Invalid powerState');
    return mutate(devices => {
      const device = devices.find(item => item.endpointId === endpointId && item.enabled);
      if (!device) return null;
      device.powerState = powerState;
      device.updatedAt = new Date().toISOString();
      return device;
    });
  },
  async setPower(endpointId, powerState) {
    if (!['ON', 'OFF'].includes(powerState)) throw new TypeError('Invalid powerState');
    return mutate(devices => {
      const device = devices.find(item => item.endpointId === endpointId && item.enabled);
      if (!device) return null;
      device.powerState = powerState;
      device.updatedAt = new Date().toISOString();
      return device;
    });
  },
  async remove(endpointId) {
    return mutate(devices => {
      const index = devices.findIndex(device => device.endpointId === endpointId);
      if (index < 0) return false;
      devices.splice(index, 1);
      return true;
    });
  }
};
