**OpenWeatherMap Multi-API Weather Driver**

**Thank you for looking at my code!**

This driver is shared under HPM and found with a search for Weather, or OpenWeatherMaps.

A direct link - here: https://raw.githubusercontent.com/jshimota01/hubitat/main/Drivers/owm_multi-API_weather/owm_multi-API_weather.groovy

My name is Jim Shimota and I rewrote this older driver to improve its use for myself and have decided to share it.

The last author seemed excited so I'm comfortable giving this a shot! The previous driver, called [Weather-Display With OWM-Alerts Forecast Driver](https://community.hubitat.com/t/release-weather-display-with-owm-alerts-forecast-driver/38557) had been a valuable tool on my dashboard, but I forked off it a few years ago. It has been around and evolved from 2020! I felt it needed a rewrite since I was getting errors with secondary sources, and many issues with Units and Precision. This is the result. Other than an Alternative Icons map, the code is completely new, little if anything was lifted from the original. As you can see by the Title of the previous driver, it was very Alerts centric. I made this more of a full service weather tool.

### Acknowledgement:

*Written by @matthew*
This driver has morphed many, many times, so the genesis is very blurry now.  It stated as a WeatherUnderground
driver, then when they restricted their API it morphed into an APIXU driver.  When APIXU ceased it became a
Dark Sky driver .... and now that Dark Sky is going away it is morphing into a OpenWeatherMap driver.

Many people contributed to the creation of this driver.  Significant contributors include:
- **@Cobra** who adapted it from @mattw01's work and I thank them for that!
- **@bangali f**or his original APIXU.COM base code that much of the early versions of this driver was
  adapted from.
- **@bangali** for his the Sunrise-Sunset.org code used to calculate illuminance/lux and the more
  recent adaptations of that code from @csteele in his continuation driver 'wx-ApiXU'.
- **@csteele** (and prior versions from @bangali) for the attribute selection code.
- @csteele for his examples on how to convert to asyncHttp calls to reduce Hub resource utilization.
- **@bangali** also contributed the icon work from
  https://github.com/jebbett for new cooler 'Alternative' weather icons with icons courtesy
  of https://www.deviantart.com/vclouds/art/VClouds-Weather-Icons-179152045.
- **@storageanarchy** for his Dark Sky Icon mapping and some new icons to compliment the Vclouds set.
- **@nh.schottfam** for lots of code clean up and optimizations.



Another important reason I went down this path is that OWM uses API keys. These keys have evolved themselves over the years. The original Key I had was called 'API 2.5'. Later OWM released a new token counting system for tracking how often you polled, it was called 'One-Call.' So the API key name changed to One-Call 2.5. a few years later, One-Call 3.0 was released, which basically adjusted the polling pricing and free levels. This year OpenWeatherMaps has declared 'One-Call 2.5' obsolete. They also released 'One-Call 4.0' and any new key you obtain is detailed as such. Old keys CURRENTLY are still supported BUT OpenWeatherMaps has warned that the 'One-Call 2.5' support will end any day. Also, you can no longer obtain a One-Call 3.0 if you want, that option is no longer available.
 All this said - this driver will accept ANY of the three One-Call versions - so 2.5, 3.0 and 4.0 are supported! I also want to note that One-Call 4.0 API is a radical departure in the polled data returned! I do NOT parse the 4.0 API yet. It is more sophisticated and there are multiple polls required to return Current data and Forecast Data - to name but one major change.

** often you'll see OWM references - it may not be obvious to some but that is OpenWeatherMaps.

Finally, here is a link to some Icons and Images folders - [hubitat/Icons-Images at bb40e3b8ec5e9454ae1c2d632e6cf08ec1ed2fd0 · jshimota01/hubitat · GitHub](https://github.com/jshimota01/hubitat/tree/bb40e3b8ec5e9454ae1c2d632e6cf08ec1ed2fd0/Icons-Images)

This folder includes subfolders of Wind Direction images, Moon Phase Images (including SVG components), the original OWM icons, and the HubitatPublic Icons as well. I myself have a local NAS that servers images without leaving my local subnet and my driver allows you to set yourself however you see fit.

**Goals:**

1. Support     data for my future use of blinds. I wanted sun angles that were accurate     and reliable.
2. Moon     phase – At a glance moon phase for fishing.
3. Control     of image paths - Localized icons and images option, fallback to icons
4. Integrated     Twilight and day/night, Solar, Moon feature so one app replaces three I     currently use
5. Improved     city/long/lat lookup with reverse city name
6. Pre-written     attributes that have the Unit with the value – ie; 5 mph.
7. Added     barometer, and improved outdoor luminance

**Extra features:**

- Tile     length debug
- Huge     embedded comments content to assist in the future
- Optimized     SendEvent to minimize database writes

**AI Reviews:**
 ChatGPT - Overall, I'd score **v2.4.0 at approximately 9.6/10**

It includes:

- Support     for multiple OWM API version keys (2.5, 3.0 and 4.0)
- Dynamic     polling based on sunrise/sunset
- Extensive     unit conversion
- Extensive     precision detail
- Multiple     date/time formats
- Alternate     icon packs
- Wind     direction graphics
- Moon     phase graphics (SVG/PNG/Emoji)
- Automatic     city lookup overrides
- Weather     alerts
- Solar     position calculations
- Twilight     calculations
- Illuminance     estimation
- Four     dashboard tiles
- Huge     number of device attributes
- Comprehensive     logging controls

**Gemini - Grade: A+ (9.6 / 10)**

This driver is written at a commercial/community-expert level. The code is clean, deeply defensive, and shows a strong understanding of Hubitat’s platform quirks (such as state committing delays and tile string limits).

 