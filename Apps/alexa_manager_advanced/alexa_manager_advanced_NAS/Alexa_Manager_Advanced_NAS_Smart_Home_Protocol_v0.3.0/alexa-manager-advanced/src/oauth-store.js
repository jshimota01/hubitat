import { mkdir, readFile, writeFile, rename } from 'node:fs/promises';
import { dirname } from 'node:path';
import { randomBytes, createHash, timingSafeEqual } from 'node:crypto';
import { config } from './config.js';

const hash = value => createHash('sha256').update(value).digest('hex');
const equal = (a, b) => {
  const aa = Buffer.from(String(a)); const bb = Buffer.from(String(b));
  return aa.length === bb.length && timingSafeEqual(aa, bb);
};
let queue = Promise.resolve();

async function readStore() {
  try {
    const parsed = JSON.parse(await readFile(config.dataFile, 'utf8'));
    return { authorizationCodes: parsed.authorizationCodes || {}, refreshTokens: parsed.refreshTokens || {}, accessTokens: parsed.accessTokens || {} };
  } catch (error) {
    if (error.code === 'ENOENT') return { authorizationCodes: {}, refreshTokens: {}, accessTokens: {} };
    throw error;
  }
}
async function writeStore(store) {
  await mkdir(dirname(config.dataFile), { recursive: true });
  const temp = `${config.dataFile}.tmp`;
  await writeFile(temp, JSON.stringify(store, null, 2), { mode: 0o600 });
  await rename(temp, config.dataFile);
}
async function mutate(fn) {
  const operation = queue.then(async () => { const store = await readStore(); const result = await fn(store); await writeStore(store); return result; });
  queue = operation.catch(() => {});
  return operation;
}

export const oauthStore = {
  verifyLogin(username, password) {
    return Boolean(config.oauthUsername && config.oauthPassword && equal(username, config.oauthUsername) && equal(password, config.oauthPassword));
  },
  async createCode({ clientId, redirectUri, codeChallenge, scope }) {
    const code = randomBytes(32).toString('base64url');
    await mutate(store => { store.authorizationCodes[hash(code)] = { clientId, redirectUri, codeChallenge, scope, expiresAt: Date.now() + 5 * 60_000 }; });
    return code;
  },
  async redeemCode({ code, clientId, redirectUri, verifier }) {
    return mutate(store => {
      const key = hash(code); const record = store.authorizationCodes[key];
      delete store.authorizationCodes[key];
      if (!record || record.expiresAt < Date.now() || record.clientId !== clientId || record.redirectUri !== redirectUri) return null;
      const challenge = createHash('sha256').update(verifier).digest('base64url');
      if (!equal(challenge, record.codeChallenge)) return null;
      return { scope: record.scope };
    });
  },
  async issueTokens(scope, clientId) {
    if (typeof clientId !== 'string' || !clientId) throw new TypeError('clientId is required');
    const accessToken = randomBytes(32).toString('base64url');
    const refreshToken = randomBytes(48).toString('base64url');
    const now = Date.now();
    await mutate(store => {
      store.refreshTokens[hash(refreshToken)] = { clientId, scope, createdAt: now, expiresAt: now + 180 * 24 * 60 * 60_000 };
      store.accessTokens[hash(accessToken)] = { clientId, scope, expiresAt: now + 60 * 60_000 };
    });
    return { access_token: accessToken, token_type: 'Bearer', expires_in: 3600, refresh_token: refreshToken, scope };
  },
  async validateAccessToken(accessToken, clientId) {
    const store = await readStore(); const record = store.accessTokens[hash(accessToken)];
    return record && record.expiresAt >= Date.now() && (clientId === undefined || record.clientId === clientId) ? { clientId: record.clientId, scope: record.scope } : null;
  },
  async rotateRefreshToken(refreshToken, clientId) {
    let scope = null;
    await mutate(store => {
      const key = hash(refreshToken); const record = store.refreshTokens[key];
      if (record && record.clientId === clientId) {
        delete store.refreshTokens[key];
        if (record.expiresAt >= Date.now()) scope = record.scope;
      }
    });
    return scope === null ? null : this.issueTokens(scope, clientId);
  }
};
