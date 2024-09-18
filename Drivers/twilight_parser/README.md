Twilight Parser 

Why?

Recent discussion (early 2024) on how to values for use in a Grafana annotation - to mark on a time series graph the period between sunrise and sunset occurred.  Further exploration made me realize that I would also like the time just before and just after (known as twilight) was deemed important for handling bulbs connected to circadian rythme apps.

Having already done some date/time work with my Meteorological Seasons driver I wrote this driver this AM to allow me to parse values pulled from the json result of the API at sunrise-sunset.org (referred to as sr-ss or SR-SS.org etc).
These values unfortunately cannot go straight into Influx (where I store my data) since they are string values.  I struggled with that using Telegraf and gave up.

In the end I decided to generate a virtual tile just like my Date-Time Parser that holds the 10 values that are returned by the API call.  then taking the strings of the time generating the Epoch date and storing them so I can push the date through the influxdb logger app easily.

Updates overview as of 04/01/2024

 Change History:
 
Date         Source      Version What                                        URL
----         ------      ------- ----                                        ---
2024-04-01   jshimota    0.1.0   Starting version
2024-04-01   danielwinks 0.1.1   Used 2023-12-10 Weather Service JSON API app components   https://github.com/DanielWinks/Hubitat-Public/blob/main/Drivers/HTTP/NWSForecast.groovyhttps://community.hubitat.com/t/project-weather-service-monitor-4-hubitat-driver/38112
2024-04-01   jshimota    0.1.2   Alpha release for testing
2024-04-01   jshimota    0.1.3   Worked on formatting cleanup and logging
2024-04-01   jshimota    0.1.4   clean long value stuff
2024-04-01   jshimota    0.1.3   Finalizing
 
Description

Twilight Parser is a driver that provides when installed correctly on a Hubitat platform a selectable device driver.
This creates a virtual device and from it dashboard tiles variables for Rules and other nice features are produced.

Requirements

I have not tested this on any platforms besides my Hubitat c7 currently running 2.3.8.xxx Firmware.  That said it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world evolves.


Installation and Configuration

    Twilight Parser
        · First using HPM (Hubitat Package Manager) locate and install this driver
        · Open the Devices page for your Hubitat Hub and click Add Virtual Device
        · Enter a Device Name and optionally a Device Label
        · In the Type drop-down list select the Twilight Parser
        · If changes occur to any preference click Save Preferences below the preferences section
        · Click Save Device to create the new virtual device
        
		It is possible that not in all cases does a 'Refresh' run at the time of install.  Click the Initialize button to initially set the values of the variables and attributes.
        
Additional Features

    The basic default settings are designed to cover most cases.  Normally DescriptionText and Debug logging are disabled but may be Enabled if necessary.  
	For testing and odd case use the automatic update can be disabled but is Enabled by default.   Auto Update interval is once per hour.  A tool such as this should have minimal impact so the default is 1 hour.
    
Use the driver in RM rules

    To access these variables in an app such as Rule Machine - use the example here as a quide (an example Rule Machine 5.1 Rule).  Below is simply an example!
    
    Create a Rule then name it
    Click Select Trigger Events
    Click 'click to set' from Select capability for new Trigger Event
    Select Custom Attribute from the Select Capability drop down
    Click Select Device
    Select your Virtual Device you created at installation
    Select Attribute from the drop down


Usable Variables with example :
Input:
lat = 45.120244 (Lat) (Latitude - defaults to what hub is set for or can be overridden) required for SR-SS API call
lng = -123.206557 (Long) (Longitude - defaults to what hub is set for or can be overridden) required for SR-SS API call
date = today (Date value is general todays date - can be overridden with specific date in form of YYYY-mm-dd) required for SR-SS API call
tzid = America/Los_Angeles (Timezone Identifier - defaults to America/Los_Angeles - can be overrriden with specific if properly TZID is provided) required for SR-SS API call

The API Call from SR-SS returns 10 strings.  Each string is simply a text value of the calculated result time of day.  Since it is text, it is unformatted.  Taking the INPUT value from 'date' then concatenating it into a properly formatted string, it is finally converted to an epoch timestamp and stored in numeric Long format datatype so it can be picked up and sent to Influx for use.

For twilight - SR-SS returns 3 sets of start and ends.  Generally, Civil is the most commonly used!

Output:
sunrise
sunset
solar_noon
day_length
civil_twilight_begin
civil_twilight_end
nautical_twilight_begin
nautical_twilight_end
astronomical_twilight_begin
astronomical_twilight_end