import { resolve } from 'node:path';

const port = Number.parseInt(process.env.PORT || '4100', 10);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid PORT');

export const config = Object.freeze({
  port,
  apiKey: process.env.AMA_API_KEY || '',
  publicBaseUrl: (process.env.PUBLIC_BASE_URL || 'https://alexa.e-wiz.com').replace(/\/$/, ''),
  oauthUsername: process.env.OAUTH_USERNAME || '',
  oauthPassword: process.env.OAUTH_PASSWORD || '',
  oauthClientId: process.env.OAUTH_CLIENT_ID || '',
  oauthAllowedRedirectHosts: (process.env.OAUTH_ALLOWED_REDIRECT_HOSTS || '').split(',').map(x => x.trim().toLowerCase()).filter(Boolean),
  dataFile: resolve(process.env.DATA_DIR || '/app/data', 'oauth-store.json')
});
