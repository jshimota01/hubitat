## 

# **OpenWeatherMap Multi-API Weather Driver** 

## Thank you for looking at my code!

My name is Jim Shimota and I rewrote this old driver to improve its use for myself and have decided to share it.

The last author seemed excited so I'm comfortable giving this a shot!  The previous driver, called  [Weather-Display With OWM-Alerts Forecast Driver](https://community.hubitat.com/t/release-weather-display-with-owm-alerts-forecast-driver/38557) had been a valuable tool on my dashboard, but I forked off it a few years ago.  It has been around and evolved from 2020!  I felt it needed a rewrite since I was getting errors with secondary sources, and many issues with Units and Precision.  This is the result.  Other than an Alernative Icons map, the code is completely new, little if anything was lifted from the original.  As you can see by the Title of the previous driver, it was very Alerts centric.  I made this more of a full service weather tool.  

Another important reason I went down this path is that OWM uses API keys.  These keys have evolved themselves over the years.  The original Key I had was called 'API 2.5'.  Later OWM released a new token counting system for tracking how often you polled, it was called 'One-Call.'  So the API key name changed to One-Call 2.5.  a few years later, One-Call 3.0 was released, which basically adjusted the polling pricing and free levels.  This year OpenWeatherMaps has declared 'One-Call 2.5' obsolete.  They also released 'One-Call 4.0' and any new key you obtain is detailed as such.  Old keys CURRENTLY are still supported BUT OpenWeatherMaps has warned that the 'One-Call 2.5' support will end any day.  Also, you can no longer obtain a One-Call 3.0 if you want, that option is no longer available.  
All this said - this driver will accept ANY of the three One-Call versions - so 2.5, 3.0 and 4.0 are supported!  I also want to note that One-Call 4.0 API is a radical departure in the polled data returned!  I do NOT parse the 4.0 API yet.  It is more sophisticated and there are multiple polls required to return Current data and Forecast Data - to name but one major change.  



** often you'll see OWM references - it may not be obvious to some but that is OpenWeatherMaps.





**Goals:**

1. Support data for my future use of blinds. I wanted sun angles that were accurate and reliable.
2.  Moon phase – At a glance moon phase for fishing.
3.  Control of image paths - Localized icons and images option, fallback to icons
4.  integrated Twilight and day/night, Solar, Moon feature so one app replaces three I currently use
5.  Improved city/long/lat lookup with reverse city name
6.  Prewritten attributes that have the Unit with the value – ie; 5 mph. 
7.  Added barometer, and improved outdoor illuminance

 

**Extra features:**

-  Tile length debug
-  Huge embedded comments content to assist in the future
-  Optimized SendEvent to minimize database writes

 

**AI Reviews:**
 ChatGPT - Overall, I'd score **v2.4.0 at approximately 9.6/10**

It includes:

- Support for multiple OWM API version keys (2.5, 3.0 and 4.0) 
- Dynamic polling based on sunrise/sunset 
- Extensive unit conversion 
- Extensive precision detail
- Multiple date/time formats 
- Alternate icon packs 
- Wind direction graphics 
- Moon phase graphics (SVG/PNG/Emoji) 
- Automatic city lookup overrides 
- Weather alerts 
- Solar position calculations 
- Twilight calculations 
- Illuminance estimation 
- Four dashboard tiles 
- Huge number of device attributes 
- Comprehensive logging controls 

 

**Gemini - Grade: A+ (9.6 / 10)**

This driver is written at a commercial/community-expert level. The code is clean, deeply defensive, and shows a strong understanding of Hubitat’s platform quirks (such as state committing delays and tile string limits).

 