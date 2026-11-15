import express from 'express';
import helmet from 'helmet';
import { config } from './config.js';
import api from './routes/api.js';
import oauth from './routes/oauth.js';
import { log } from './logger.js';
import { recordTelemetry } from './influx.js';

if (!config.oauthUsername || !config.oauthPassword) {
  log('warn', 'OAuth login is disabled: set OAUTH_USERNAME and OAUTH_PASSWORD before enabling account linking');
}
if (!config.oauthAllowedRedirectHosts.length) {
  log('warn', 'OAuth authorization is fail-closed until OAUTH_ALLOWED_REDIRECT_HOSTS is configured');
}

const app = express();
app.disable('x-powered-by');
app.use(helmet({ contentSecurityPolicy: { directives: { defaultSrc: ["'self'"], styleSrc: ["'unsafe-inline'"] } } }));
app.use(express.urlencoded({ extended: false, limit: '16kb' }));
app.use(express.json({ limit: '256kb', strict: true }));
app.use('/oauth', oauth);
app.use('/api', api);
app.get('/', (_req, res) => res.type('text/plain').send('Alexa Manager Advanced NAS service'));
app.use((err, _req, res, _next) => {
  log('error', 'Request processing error', { message: err.message });
  res.status(400).json({ error: 'bad_request' });
});
app.listen(config.port, '0.0.0.0', () => {
  log('info', `Service listening on port ${config.port}`, { publicBaseUrl: config.publicBaseUrl });
  recordTelemetry('service_started', { port: config.port });
});
