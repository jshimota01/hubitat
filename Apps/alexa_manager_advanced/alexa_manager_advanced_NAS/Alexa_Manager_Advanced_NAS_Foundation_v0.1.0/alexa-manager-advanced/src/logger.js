const levels = { error: 0, warn: 1, info: 2, debug: 3 };
const threshold = levels[process.env.LOG_LEVEL || "info"] ?? levels.info;
export function log(level, message, details = undefined) {
  if ((levels[level] ?? 99) > threshold) return;
  console.log(JSON.stringify({
    timestamp: new Date().toISOString(), level,
    service: "alexa-manager-advanced", message,
    ...(details ? { details } : {})
  }));
}
