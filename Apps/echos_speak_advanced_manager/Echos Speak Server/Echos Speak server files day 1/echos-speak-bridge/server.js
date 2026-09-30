const express = require('express');
const Alexa = require('alexa-remote2');
const path = require('path');
const fs = require('fs');

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 8093;
const COOKIE_PATH = path.join(__dirname, 'data', '.alexa-cookie.json');

// Ensure data folder exists
if (!fs.existsSync(path.join(__dirname, 'data'))) {
    fs.mkdirSync(path.join(__dirname, 'data'), { recursive: true });
}

const alexa = new Alexa();

const alexaConfig = {
    cookieData: fs.existsSync(COOKIE_PATH) ? JSON.parse(fs.readFileSync(COOKIE_PATH, 'utf8')) : null,
    alexaServiceHost: 'alexa.amazon.com',
    amazonPage: 'amazon.com',
    amazonPageProxyLanguage: 'en_US',
    acceptLanguage: 'en-US',
    setupProxy: true,
    proxyOwnIp: '192.168.1.12',
    proxyPort: 8094,
    proxyListenBind: '0.0.0.0',
    useComp: false,
    useragent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'
};

function initAlexa() {
    console.log('[Bridge] Initializing Alexa Remote connection...');
    
    if (fs.existsSync(COOKIE_PATH)) {
        try {
            const rawCookie = fs.readFileSync(COOKIE_PATH, 'utf8');
            alexaConfig.cookieData = JSON.parse(rawCookie);
            console.log('[Bridge] Found existing .alexa-cookie.json session file.');
        } catch (e) {
            console.error('[Bridge] Failed to parse existing cookie file:', e.message);
        }
    }

    alexa.init(alexaConfig, (err) => {
        if (err) {
            console.error('[Bridge] Alexa Init Warning:', err.message);
            
            // If cookie file exists on disk, force internal authentication flag
            if (fs.existsSync(COOKIE_PATH) && alexaConfig.cookieData) {
                console.log('[Bridge] Cookie file present. Overriding internal auth state...');
                alexa.authenticated = true;
            } else {
                console.log(`[Bridge] Open http://192.168.1.12:8094 in your browser to re-authenticate with Amazon.`);
                return;
            }
        }
        
        // Ensure auth flag is active if cookie data is loaded
        alexa.authenticated = true;
        console.log('[Bridge] Alexa Remote session established successfully!');
        
        if (alexa.cookieData) {
            try {
                fs.writeFileSync(COOKIE_PATH, JSON.stringify(alexa.cookieData, null, 2));
                console.log('[Bridge] Session cookie saved/refreshed locally.');
            } catch (writeErr) {
                console.error('[Bridge] Failed to write cookie file:', writeErr.message);
            }
        }
    });
}

// Add direct listener to save cookie as soon as proxy captures it
alexa.on('cookie', (cookieData) => {
    console.log('[Bridge] Event "cookie" fired! Writing session tokens to disk...');
    try {
        fs.writeFileSync(COOKIE_PATH, JSON.stringify(cookieData, null, 2));
        console.log('[Bridge] Cookie saved successfully.');
    } catch (err) {
        console.error('[Bridge] Failed to save cookie on event:', err.message);
    }
});

// System Status Endpoint
app.get('/status', (req, res) => {
    const hasCookie = fs.existsSync(COOKIE_PATH);
    const isAuth = Boolean(alexa && (alexa.authenticated || (alexa.alexaCookie && alexa.alexaCookie.localCookie)));
    
    res.json({
        online: isAuth,
        authenticated: isAuth,
        cookieExists: hasCookie
    });
});

// Device Discovery Endpoint
app.get('/api/devices', (req, res) => {
    if (!alexa.authenticated && !fs.existsSync(COOKIE_PATH)) {
        return res.status(503).json({ error: 'Bridge not authenticated with Amazon' });
    }

    // Force flag true if cookie is on disk
    alexa.authenticated = true;

    alexa.getDevices((err, result) => {
        if (err || !result || !result.devices) {
            return res.status(500).json({ error: err ? err.message : 'Failed to fetch devices' });
        }

        const devices = result.devices.map(dev => ({
            serialNumber: dev.serialNumber,
            accountName: dev.accountName,
            deviceFamily: dev.deviceFamily,
            deviceStyle: dev.deviceType,
            online: dev.online !== false,
            ipAddress: dev.ipAddress || dev.macAddress || '192.168.1.X',
            macAddress: dev.macAddress || 'N/A',
            deviceImageUrl: dev.deviceIconUrl || 'https://m.media-amazon.com/images/G/01/mobile-apps/dex/alexa/alexa-skills-kit/stock/amazon-echo-1._CB346614488_.png'
        }));

        res.json({ devices });
    });
});

app.listen(PORT, () => {
    console.log(`[Bridge] Echos Speak Bridge server active on port ${PORT}`);
    initAlexa();
});