import { Router } from 'express';
import { timingSafeEqual } from 'node:crypto';
import { config } from '../config.js';
import { oauthStore } from '../oauth-store.js';
import { log } from '../logger.js';

const router = Router();
const html = value => String(value).replace(/[&<>"']/g, c => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' }[c]));
const validVerifier = value => typeof value === 'string' && /^[A-Za-z0-9._~-]{43,128}$/.test(value);

function safeEqual(a, b) {
  const aa = Buffer.from(String(a));
  const bb = Buffer.from(String(b));
  return aa.length === bb.length && timingSafeEqual(aa, bb);
}

function authenticateClient(req) {
  const header = req.get('authorization') || '';
  const match = /^Basic\s+([A-Za-z0-9+/]+={0,2})$/i.exec(header);
  if (!match || !config.oauthClientId || !config.oauthClientSecret) return null;

  let credentials;
  try {
    credentials = Buffer.from(match[1], 'base64').toString('utf8');
  } catch {
    return null;
  }

  const separator = credentials.indexOf(':');
  if (separator < 1) return null;

  const clientId = credentials.slice(0, separator);
  const clientSecret = credentials.slice(separator + 1);

  if (!safeEqual(clientId, config.oauthClientId) ||
      !safeEqual(clientSecret, config.oauthClientSecret)) return null;

  return clientId;
}

function validRedirect(uri) {
  try {
    const parsed = new URL(uri);
    return parsed.protocol === 'https:' && parsed.port === '' && config.oauthAllowedRedirectHosts.some(host => parsed.hostname === host || parsed.hostname.endsWith(`.${host}`));
  } catch { return false; }
}

function oauthError(res, error, status = 400) {
  return res.status(status).json({ error });
}

router.get('/authorize', (req, res) => {
  const { response_type, client_id, redirect_uri, state, code_challenge, code_challenge_method, scope } = req.query;

  if (response_type !== 'code' || !client_id || !redirect_uri || !state || !validRedirect(redirect_uri) ||
      (config.oauthClientId && client_id !== config.oauthClientId) ||
      code_challenge_method !== 'S256' ||
      typeof code_challenge !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(code_challenge)) {
    return oauthError(res, 'invalid_request');
  }

  const safe = Object.fromEntries(['client_id','redirect_uri','state','code_challenge','code_challenge_method','scope'].map(k => [k, String(req.query[k] || '')]));
  const hidden = Object.entries(safe).map(([k,v]) => `<input type="hidden" name="${k}" value="${html(v)}">`).join('');

  res.type('html').send(`<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Alexa Manager Advanced</title><style>body{font:16px system-ui;max-width:420px;margin:8vh auto;padding:24px;color:#222}input{box-sizing:border-box;width:100%;padding:12px;margin:8px 0 16px}button{padding:12px 20px;width:100%}</style></head><body><h1>Alexa Manager Advanced</h1><p>Sign in to link your Alexa account.</p><form method="post" action="/oauth/authorize">${hidden}<label>Username<input name="username" autocomplete="username" required></label><label>Password<input name="password" type="password" autocomplete="current-password" required></label><button type="submit">Sign in and link</button></form></body></html>`);
});

router.post('/authorize', async (req, res, next) => {
  try {
    const { client_id, redirect_uri, state, code_challenge, code_challenge_method, scope, username, password } = req.body || {};

    if (!client_id || !redirect_uri || !state || !validRedirect(redirect_uri) ||
        (config.oauthClientId && client_id !== config.oauthClientId) ||
        code_challenge_method !== 'S256' ||
        typeof code_challenge !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(code_challenge)) {
      return oauthError(res, 'invalid_request');
    }

    if (!oauthStore.verifyLogin(username, password)) {
      log('warn', 'OAuth login rejected');
      return res.status(401).type('text/plain').send('Login failed. Return to Alexa and try again.');
    }

    const code = await oauthStore.createCode({
      clientId: client_id,
      redirectUri: redirect_uri,
      codeChallenge: code_challenge,
      scope: scope || 'alexa::all'
    });

    const destination = new URL(redirect_uri);
    destination.searchParams.set('code', code);
    destination.searchParams.set('state', state);

    log('info', 'OAuth authorization code issued');
    res.redirect(302, destination.toString());
  } catch (error) {
    next(error);
  }
});

router.post('/token', async (req, res, next) => {
  try {
    const clientId = authenticateClient(req);
    if (!clientId) {
      log('warn', 'OAuth token request rejected: invalid client authentication');
      return oauthError(res, 'invalid_client', 401);
    }

    const { grant_type, code, client_id, redirect_uri, code_verifier, refresh_token } = req.body || {};

    if (client_id && client_id !== clientId) {
      return oauthError(res, 'invalid_client', 401);
    }

    let tokens;

    if (grant_type === 'authorization_code') {
      if (!code || !redirect_uri || !validVerifier(code_verifier)) {
        return oauthError(res, 'invalid_request');
      }

      const grant = await oauthStore.redeemCode({
        code,
        clientId,
        redirectUri: redirect_uri,
        verifier: code_verifier
      });

      if (!grant) return oauthError(res, 'invalid_grant');

      tokens = await oauthStore.issueTokens(grant.scope, clientId);

    } else if (grant_type === 'refresh_token') {
      if (!refresh_token) return oauthError(res, 'invalid_request');

      tokens = await oauthStore.rotateRefreshToken(refresh_token, clientId);
      if (!tokens) return oauthError(res, 'invalid_grant');

    } else {
      return oauthError(res, 'unsupported_grant_type');
    }

    res.set('Cache-Control', 'no-store').set('Pragma', 'no-cache').json(tokens);

  } catch (error) {
    next(error);
  }
});

export default router;
