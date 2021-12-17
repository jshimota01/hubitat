Meteorological Seasons
Previously - Meteorological Seasons of the Northern Hemisphere

Why?

A need developed for me to be able to set Rules and automations within my Hubitat environment that would allow me to gain control seasonally.  Originally, with community help I was able to build a Rule that would keep a Global Variable up to date.  However this wasn't as smooth of a method.  So I wrote this.

Description

Meteorological Seasons is a driver that provides, when installed correctly on a Hubitat platform, a selectable device driver.
This creates a virtual device, and from it, dashboard tiles, variables for Rules and other nice features are produced.

Requirements

I have not tested this on any platforms besides my Hubitat c5 currently running 2.2.8 Firmware.  That said, it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world develops.

New Features
1.x		Initial release
2.x		Added support for BOTH hemispheres

Installation and Configuration

    Meteorological Seasons
        · First, using HPM (Hubitat Package Manager) locate and install this driver
        · Open the Devices page for your Hubitat Hub and click Add Virtual Device
        · Enter a Device Name and optionally a Device Label
        · In the Type drop-down list select the Meteorological Seasons
        · In the preference - verify your Hemisphere. (In version 2.x the addition of Northern and Southern hemisphere support was added)
        · If changes occur to any preference, click Save Preferences below the preferences section
        · Click Save Device to create the new virtual device
        
		It is possible that not in all cases does a 'Refresh' run at the time of install.  Click the Current Season to initialize the values.
        
		You may also override the current season value calculated by choosing one of the seasons and clicking.  However, by default - the driver WILL reset at 6am the next day to the current season.
			*Unless you have disabled Autoupdate - in which case it will never change.

Additional Features

    The basic default settings are designed to cover most cases.  Normally, DescriptionText logging is enabled, but Debug logging is disabled.  Both are configurable.
    
    The default update is enabled and scheduled at 6am each day.  The system clears all schedules, then adds another upon each morning refresh.  This guarantees no inadvertent multiple schedules will occur.
    If you disable updates you can keep an override value in place (as described above by choosing and click any season)  This is useful for testing but is recommended to be left enabled.
    
    The value of the image is managed separately from the value of the textual response.  In this way, when creating dashboard icons, you can choose as needed.  However in some cases a tile is needed that contains both, and in HTML layout.
    Enabling the HTML attribute will provide a default layout HTML tile for use with SuperTiles and other dashboard apps.  It is NOT fully fleshed and tested but works in a basic form.
    As of x.2.10 - The HTML snippet used as a complete tile option (by selecting it in attributes when making a dashboard tile) has been radically altered.  This was done to provide more functional/accurate support for Hubitat Dashboard for Android - a tool I rely on.
    Additionally, as of x.2.1x further features to support the tiles have been added - Font Color can be specified either as hex #xxxxxx or using websafe color names.  Font-size can be specified from 1%-500% or using standard font values such as 12px or LG etc.  Lastly, the vertical position within the tile of the test ovelay is now adjustable!  the default, 55 (percent) puts the text about 2/3rds from the top. but you can specify 1-100 (%) and control it from top edge to bottom edge.
    
    The driver calls to the internet for the icons, stored in the GitHub repository:
    " https://raw.githubusercontent.com/jshimota01/hubitat/main/meteorological_seasons/season_icons/ "
    A user could easily download the five icons (fall.svg, winter.svg, spring.svg, summer.svg and unknown.svg) and put them on their Hubitat or in a local web folder.
    By updating the Alternate path for season icons, you can override the reference to your images location in this manner.  (**NOTE – end your reference path with a “/” (forward slash!))
	Also, it is possible that you may wish to use your own images and replace the default SVG files. Simply make sure you name your replacement files correctly and identically as they are hardcoded.
	
    As of version 2.x the ability to set for the southern hemisphere was added.  The default is “Northern” hemisphere, but turning OFF the switch and saving preferences will flip the world and align the correct seasons, their dates, names and images.
   
    *** Important - If changes occur to any preference, click Save Preferences below the preferences section

Use the driver in RM rules

    To access Rules values - the example here is for a Predicate (RM 5!) - but it can be used wherever.  Below is simply an Example!
    Create a Rule, then name it.
    Enabled Use Predicate Conditions
    Click Define Predicate Conditions
    Select New Condition in the drop down
    Select Custom Attribute from the Select Capability drop down
    Click Select Device
    Select your Virtual Device you created at installation
    Select Attribute from the drop down
    Choose SeasonName
    Choose the Equals sign
    Enter a season text value – It MUST be one ofthe following: “Fall”, “Summer”, “Winter” or “Spring” [IE; Uppercase first letter!]
    Press enter to complete the text line entry
    Click Done with this Condition.

Future updates

      When I wrote this driver, I was reminded of some basics about this topic.  

      First, there are two main forms of seasons - Meteorological and Astronomical.  Astronomical seasons are calculated using the equinoxes (which are not static dates each year!), and therefore varies.
      
      Example for Fall
      Meteorological = September 1 through November 30
      Astronomical = September 22 through December 21
      
      The ability for a user to choose between these 2 forms of Seasons was intended, but my programming skills couldn't yet deal with the implementation.
      
      The ability for users to be able to create a start/stop for each season was also intended, but never implemented.
      
      The ability for a user to enter any date to get the correct value was intended and implement but then removed as the date field entry was complicated and I didn't want the support hassle.
