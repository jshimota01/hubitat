const LOG_LEVELS = { ERROR: 0, WARN: 1, INFO: 2, DEBUG: 3 };
const CURRENT_LEVEL = LOG_LEVELS[process.env.LOG_LEVEL?.toUpperCase()] ?? LOG_LEVELS.INFO;

const SENSITIVE_KEY_PATTERN = /(cookie|token|csrf|password|auth|ubid|at-main|session|macDms|formerRegistrationData|registrationData)/i;

function sanitize(data) {
  if (typeof data !== 'object' || data === null) return data;
  if (Array.isArray(data)) return data.map(sanitize);

  const clean = {};
  for (const [key, val] of Object.entries(data)) {
    if (SENSITIVE_KEY_PATTERN.test(key)) {
      clean[key] = '[REDACTED]';
    } else if (typeof val === 'object' && val !== null) {
      clean[key] = sanitize(val);
    } else {
      clean[key] = val;
    }
  }
  return clean;
}

function getLocalTimestamp() {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Los_Angeles',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    fractionalSecondDigits: 3,
    hour12: false,
    timeZoneName: 'short'
  }).formatToParts(new Date());

  const values = {};
  for (const part of parts) {
    values[part.type] = part.value;
  }

  return `${values.year}-${values.month}-${values.day} ${values.hour}:${values.minute}:${values.second}.${values.fractionalSecond} ${values.timeZoneName}`;
}

function log(levelName, message, data = null) {
  const targetLevel = LOG_LEVELS[levelName];
  if (targetLevel === undefined || targetLevel > CURRENT_LEVEL) return;

  const timestamp = getLocalTimestamp();
  let line = `[${timestamp}] [${levelName}] ${message}`;
  if (data) {
    line += ` | ${JSON.stringify(sanitize(data))}`;
  }
  console.log(line);
}

module.exports = {
  error: (msg, data) => log('ERROR', msg, data),
  warn:  (msg, data) => log('WARN', msg, data),
  info:  (msg, data) => log('INFO', msg, data),
  debug: (msg, data) => log('DEBUG', msg, data),
};