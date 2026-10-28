/**
 * Currently Weather SVG Tile
 * Hubitat Elevation App
 *
 * Prototype v0.1.0
 * Reads the selected OpenWeatherMap Multi-API Weather Driver and serves
 * a dynamically generated SVG from a local app endpoint.
 *
 * This app does not modify the weather driver or its attributes.
 */

definition(
    name: "Currently Weather SVG Tile",
    namespace: "jshimota",
    author: "OpenAI",
    description: "Serves a responsive SVG image of current weather for a Hubitat Dashboard tile.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/HubitatCommunity/HubitatPublic/master/images/app-blank.png",
    iconX2Url: "https://raw.githubusercontent.com/HubitatCommunity/HubitatPublic/master/images/app-blank.png",
    singleInstance: false
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Currently Weather SVG Tile", install: true, uninstall: true) {
        section("Weather source") {
            input "weatherDevice", "capability.temperatureMeasurement",
                title: "OpenWeatherMap weather device",
                description: "Select your OpenWeatherMap Multi-API Weather Driver",
                required: true, multiple: false
        }
        section("Display options") {
            input "cityLabel", "text", title: "City name to display", defaultValue: "Dundee", required: true
            input "backgroundColor", "text", title: "Background color (CSS/SVG color)", defaultValue: "#78919a", required: true
            input "textColor", "text", title: "Text color", defaultValue: "#ffffff", required: true
            input "showWindImage", "bool", title: "Use wind-direction image when available", defaultValue: true, required: true
        }
        section("Local SVG endpoint") {
            paragraph "After saving, use the local SVG endpoint shown below as the source for an image tile or an HTML image tag."
            paragraph endpointDisplay()
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    if (!state.accessToken) {
        try {
            createAccessToken()
            state.accessToken = state.accessToken ?: atomicState.accessToken
        } catch (Exception e) {
            log.warn "Could not create app access token automatically. Open the app endpoint settings and create one if required."
        }
    }
    // Keep a stable token for the endpoint URL where possible.
    if (!state.accessToken) {
        state.accessToken = atomicState.accessToken
    }
}

mappings {
    path("/svg") {
        action: [GET: "getWeatherSvg"]
    }
    path("/preview") {
        action: [GET: "getWeatherSvg"]
    }
}

private String endpointDisplay() {
    String base = ""
    try {
        base = getFullLocalApiServerUrl()
    } catch (Exception ignored) {
        base = "http://HUB-IP/apps/api/${app.id}"
    }
    String token = state.accessToken ?: atomicState.accessToken ?: "ACCESS_TOKEN"
    return "${base}/svg?access_token=${token}"
}

def getWeatherSvg() {
    String svg = buildWeatherSvg()
    render contentType: "image/svg+xml", data: svg, status: 200,
        headers: ["Cache-Control": "no-cache, no-store, must-revalidate", "Pragma": "no-cache"]
}

private String buildWeatherSvg() {
    def d = settings.weatherDevice
    String city = safeText(settings.cityLabel ?: "Dundee")
    String condition = safeText(d?.currentValue("currentConditionTypeDesc"))
    if (!condition || condition == "--" || condition == "null") condition = "Weather unavailable"
    condition = titleWords(condition)

    String temp = safeText(d?.currentValue("currentTemperature"))
    String feels = safeText(d?.currentValue("currentFeelsLike"))
    String hi = safeText(d?.currentValue("todayTempMax"))
    String lo = safeText(d?.currentValue("todayTempMin"))
    String humidity = safeText(d?.currentValue("currentHumidity"))
    String wind = safeText(d?.currentValue("currentWindSpeed"))
    String tempUnit = safeText(d?.currentValue("temperatureUnit"))
    String windUnit = safeText(d?.currentValue("windSpeedUnit"))
    String iconUrl = safeText(d?.currentValue("currentConditionAltImage"))
    String windIconUrl = safeText(d?.currentValue("currentWindDirImageUrl"))

    if (!tempUnit || tempUnit == "null") tempUnit = "°F"
    if (!windUnit || windUnit == "null") windUnit = "mph"
    if (!temp || temp == "null") temp = "--"
    if (!feels || feels == "null") feels = temp
    if (!hi || hi == "null") hi = "--"
    if (!lo || lo == "null") lo = "--"
    if (!humidity || humidity == "null") humidity = "--"
    if (!wind || wind == "null") wind = "--"

    // Round numeric values to whole numbers, matching the existing HTML tile.
    temp = rounded(temp)
    feels = rounded(feels)
    hi = rounded(hi)
    lo = rounded(lo)
    wind = rounded(wind)

    // City is explicitly configured in this renderer app because the source driver
    // keeps its resolved city in private state rather than exposing it as an attribute.
    String cityText = city ?: "Local Weather"

    String bg = safeColor(settings.backgroundColor ?: "#78919a")
    String fg = safeColor(settings.textColor ?: "#ffffff")

    String icon = ""
    if (iconUrl && iconUrl != "null") {
        icon = """<image href="${xmlAttr(iconUrl)}" x="74" y="82" width="86" height="86" preserveAspectRatio="xMidYMid meet"/>"""
    } else {
        icon = """<text x="117" y="145" text-anchor="middle" font-size="62">☀</text>"""
    }

    String windIcon = ""
    if (settings.showWindImage == true && windIconUrl && windIconUrl != "null") {
        windIcon = """<image href="${xmlAttr(windIconUrl)}" x="214" y="326" width="22" height="22" preserveAspectRatio="xMidYMid meet"/>"""
    } else {
        windIcon = """<text x="225" y="345" text-anchor="middle" font-size="22">↓</text>"""
    }

    return """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 400 400" width="100%" height="100%" preserveAspectRatio="xMidYMid meet">
<rect x="0" y="0" width="400" height="400" rx="10" fill="${bg}"/>
<g fill="${fg}" font-family="Arial,Helvetica,sans-serif" text-anchor="middle">
<text x="200" y="39" font-size="25" font-weight="700">${xmlText("Currently in " + cityText)}</text>
${icon}
<text x="260" y="139" font-size="49" font-weight="700">${xmlText(temp + tempUnit)}</text>
<text x="200" y="178" font-size="17" fill="#eeeeee">${xmlText("(feels like " + feels + tempUnit + ")")}</text>
<text x="200" y="245" font-size="26">${xmlText(condition)}</text>
<text x="200" y="294" font-size="24">High: <tspan font-weight="700">${xmlText(hi)}°</tspan>  Low: <tspan font-weight="700">${xmlText(lo)}°</tspan></text>
<text x="151" y="345" font-size="23">💧 ${xmlText(humidity)}%</text>
<text x="200" y="345" font-size="23">|</text>
${windIcon}
<text x="286" y="345" font-size="23">${xmlText(wind + " " + windUnit)}</text>
</g></svg>"""
}

private String safeText(def value) {
    return value == null ? "" : value.toString().trim()
}

private String rounded(String value) {
    try {
        return new BigDecimal(value).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
    } catch (Exception ignored) {
        return value
    }
}

private String titleWords(String value) {
    return value.toLowerCase(Locale.US).split(/\s+/).collect { String word ->
        word ? word.substring(0, 1).toUpperCase(Locale.US) + word.substring(1) : ""
    }.join(" ")
}

private String xmlText(String value) {
    return (value ?: "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

private String xmlAttr(String value) {
    return xmlText(value).replace("\"", "&quot;").replace("'", "&apos;")
}

private String safeColor(String value) {
    // Restrict configurable colors to common safe SVG color syntax.
    return (value ==~ /#[0-9a-fA-F]{3,8}/ || value ==~ /[A-Za-z]{3,20}/) ? value : "#78919a"
}
