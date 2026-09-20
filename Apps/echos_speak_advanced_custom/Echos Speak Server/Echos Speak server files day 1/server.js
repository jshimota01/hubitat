/**
 * Echos Speak Advanced Local API Bridge
 * Node.js / Docker Server for Synology NAS
 *
 * Author: James Shimota
 * Version: 1.1.5
 **/

const express = require('express');
const Alexa = require('alexa-remote2');
const path = require('path');
const fs = require('fs');

/* =========================================================================================
   SERVER & ENVIRONMENT CONFIGURATION
   ========================================================================================= */

const REST_PORT = process.env.REST_PORT || 8093;
const PROXY_PORT = process.env.PROXY_PORT || 8094;
const DATA_DIR = path.join(__dirname, 'data');
const COOKIE_FILE = path.join(DATA_DIR, '.alexa-cookie.json');

if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
}

const app = express();
app.use(express.json());

const alexa = new Alexa();

const alexaConfig = {
    cookieFile: COOKIE_FILE,
    alexaServiceHost: 'alexa.amazon.com',
    amazonPage: 'amazon.com',
    amazonPageProxy: 'amazon.com',
    baseAmazonPage: 'amazon.com',
    acceptLanguage: 'en-US',
    proxyLanguage: 'en',
    amazonPageProxyLanguage: 'en_US',

    autoControl: true,
    cookieRefreshInterval: 7 * 24 * 60 * 60 * 1000,

    proxyOnly: true,
    proxyOwnIp: process.env.BRIDGE_IP || '192.168.1.12',
    proxyPort: PROXY_PORT,
    proxyListenBind: '0.0.0.0',
    useCalculatedMac: true
};

/* =========================================================================================
   ALEXA REMOTE INITIALIZATION & EVENT LISTENERS
   ========================================================================================= */

console.log('Initializing Alexa Remote connection...');

alexa.init(alexaConfig, (err) => {
    if (err) {
        console.error('Alexa Remote initialization failed or cookie expired:', err.message);
        console.log(`Re-authentication required. Open browser to http://${alexaConfig.proxyOwnIp}:${PROXY_PORT}`);
    } else {
        console.log('Alexa Remote successfully authenticated via existing cookie!');
    }
});

alexa.on('cookie', (cookie) => {
    console.log('New Amazon session cookie captured successfully via proxy!');
    alexa.authenticated = true;
    try {
        const dataToSave = alexa.cookieData || cookie;
        fs.writeFileSync(COOKIE_FILE, typeof dataToSave === 'string' ? dataToSave : JSON.stringify(dataToSave), 'utf8');
        console.log('Cookie successfully flushed to disk at:', COOKIE_FILE);
    } catch (err) {
        console.error('Failed to write cookie file to disk:', err);
    }
});

/* =========================================================================================
   HELPER UTILITIES & PROMISIFIED ALEXA API WRAPPERS
   ========================================================================================= */

const getDevicesAsync = () => {
    return new Promise((resolve, reject) => {
        alexa.getDevices((err, result) => {
            if (err) reject(err);
            else resolve(result);
        });
    });
};

const getVoiceActivitiesAsync = () => {
    return new Promise((resolve) => {
        alexa.getActivities({ records: 15 }, (err, result) => {
            if (err || !result || !Array.isArray(result)) resolve([]);
            else resolve(result);
        });
    });
};

const sendVoiceCommandAsync = (serialNumber, textCommand) => {
    return new Promise((resolve, reject) => {
        alexa.sendSequenceCommand(serialNumber, 'textCommand', textCommand, (err, result) => {
            if (err) reject(err);
            else resolve(result);
        });
    });
};

/* =========================================================================================
   REST API ENDPOINTS & ROUTING
   ========================================================================================= */

app.get('/status', (req, res) => {
    res.json({
        status: 'online',
        authenticated: alexa.authenticated || false,
        timestamp: new Date().toISOString()
    });
});

app.get('/api/devices', async (req, res) => {
    if (!alexa.authenticated) {
        return res.status(500).json({ error: 'Cookie invalid, Renew unsuccessful' });
    }

    try {
        const rawDevices = await getDevicesAsync();
        if (!rawDevices || !Array.isArray(rawDevices.devices)) {
            return res.json({ devices: [] });
        }

        const voiceActivities = await getVoiceActivitiesAsync();

        const deviceList = rawDevices.devices.map(dev => {
            const serialNumber = dev.serialNumber;
            const lastAct = voiceActivities.find(a => a.deviceSerialNumber === serialNumber);
            const spokenText = (lastAct && lastAct.description && lastAct.description.summary) 
                ? lastAct.description.summary 
                : null;

            return {
                serialNumber: serialNumber,
                accountName: dev.accountName || 'Echo Device',
                deviceFamily: dev.deviceFamily || 'ECHO',
                deviceStyle: dev.deviceType || 'ECHO',
                deviceImageUrl: dev.deviceImageUrl || null,
                online: dev.online !== false,
                ipAddress: 'Masked by Amazon',
                macAddress: 'Masked by Amazon',
                firmwareVer: dev.softwareVersion || 'Unknown',
                followUpMode: dev.capabilities ? dev.capabilities.includes('TIMERS_AND_ALARMS') : false,
                alexaWakeWord: dev.wakeWord || 'ALEXA',
                volume: (dev.volume !== undefined) ? dev.volume : 20,
                alarmVolume: 50,
                permissions: 'FULL',
                lastVoiceActivity: spokenText,
                phraseSpoken: spokenText
            };
        });

        return res.json({ devices: deviceList });
    } catch (err) {
        console.error('Failed to fetch devices from Amazon:', err);
        return res.status(500).json({ error: err.message || 'Cookie invalid, Renew unsuccessful' });
    }
});

app.post('/api/command', async (req, res) => {
    const { serialNumber, command, payload } = req.body;

    if (!serialNumber || !command) {
        return res.status(400).json({ error: 'Missing required parameter: serialNumber or command' });
    }

    if (!alexa.authenticated) {
        return res.status(500).json({ error: 'Cookie invalid, Renew unsuccessful' });
    }

    console.log(`Executing [${command}] for device [${serialNumber}]:`, payload || {});

    try {
        switch (command) {
            case 'speak':
                if (!payload || !payload.text) return res.status(400).json({ error: 'Missing payload parameter: text' });
                await alexa.sendSequenceCommand(serialNumber, 'speak', payload.text);
                break;

            case 'announce':
                if (!payload || !payload.text) return res.status(400).json({ error: 'Missing payload parameter: text' });
                if (payload.target === 'all') {
                    await alexa.sendAnnouncement(null, payload.text);
                } else {
                    await alexa.sendAnnouncement(serialNumber, payload.text);
                }
                break;

            case 'voiceCmd':
                if (!payload || !payload.command) return res.status(400).json({ error: 'Missing payload parameter: command' });
                await sendVoiceCommandAsync(serialNumber, payload.command);
                break;

            case 'volume':
            case 'setAlarmVolume':
                if (!payload || payload.level === undefined || payload.level === null) {
                    return res.status(400).json({ error: 'Missing payload parameter: level' });
                }
                
                let targetVol = parseInt(payload.level, 10);
                if (isNaN(targetVol)) targetVol = 30;
                if (targetVol < 0) targetVol = 0;
                if (targetVol > 100) targetVol = 100;

                await new Promise((resolve, reject) => {
                    alexa.sendSequenceCommand(serialNumber, 'volume', targetVol, (err, result) => {
                        if (err) reject(err);
                        else resolve(result);
                    });
                });
                break;

            case 'dnd':
                await alexa.setDoNotDisturb(serialNumber, payload?.state === true);
                break;

            case 'mute':
                await alexa.sendSequenceCommand(serialNumber, payload?.state ? 'mute' : 'unmute');
                break;

            case 'setAlarm':
                if (!payload || !payload.time) return res.status(400).json({ error: 'Missing payload parameter: time' });
                await alexa.setAlarm(serialNumber, payload.time);
                break;

            case 'createReminder':
                if (!payload || !payload.text || !payload.time) return res.status(400).json({ error: 'Missing payload parameters: text or time' });
                await alexa.createReminder(serialNumber, payload.text, payload.time);
                break;

            case 'mediaControl':
                if (!payload || !payload.type) return res.status(400).json({ error: 'Missing payload parameter: type' });
                await alexa.sendSequenceCommand(serialNumber, payload.type);
                break;

            case 'sendCmd':
                if (!payload || !payload.cmd) return res.status(400).json({ error: 'Missing payload parameter: cmd' });
                await alexa.sendSequenceCommand(serialNumber, payload.cmd, payload.val || '');
                break;

            default:
                return res.status(400).json({ error: `Unsupported command: ${command}` });
        }

        return res.json({ status: 'success', command });
    } catch (err) {
        console.error(`Execution error for command [${command}] on device [${serialNumber}]:`, err);
        return res.status(500).json({ error: err.message || 'Execution failed' });
    }
});

/* =========================================================================================
   GRACEFUL SHUTDOWN HANDLERS
   ========================================================================================= */

const handleShutdown = (signal) => {
    console.log(`Received ${signal}. Flushing session state to disk before exiting...`);
    
    if (alexa && (alexa.cookieData || alexa.cookie)) {
        try {
            const dataToSave = alexa.cookieData || alexa.cookie;
            fs.writeFileSync(COOKIE_FILE, typeof dataToSave === 'string' ? dataToSave : JSON.stringify(dataToSave), 'utf8');
            console.log('Successfully saved .alexa-cookie.json');
        } catch (err) {
            console.error('Failed to save cookie file during shutdown:', err);
        }
    }
    process.exit(0);
};

process.on('SIGTERM', () => handleShutdown('SIGTERM'));
process.on('SIGINT', () => handleShutdown('SIGINT'));

/* =========================================================================================
   SERVER STARTUP
   ========================================================================================= */

app.listen(REST_PORT, '0.0.0.0', () => {
    console.log(`================================================================`);
    console.log(`Echos Speak Advanced API Bridge (v1.1.5) Running`);
    console.log(`REST API Listening on Port: ${REST_PORT}`);
    console.log(`Amazon Auth Proxy Listening on Port: ${PROXY_PORT}`);
    console.log(`================================================================`);
});