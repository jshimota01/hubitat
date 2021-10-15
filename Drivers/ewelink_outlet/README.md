eWeLink Outlet

Why?

I began using these lowcost Zigbee outlets because they were Zigbee, cheap and seemed to work really well after trying a couple other brands.  I also
selected them because they act as a repeater making my Zigbee mesh more solid.  After my first purchase of 4, I liked them so much I went back and bought
them again, but Amazon no longer sold them - they sold 'CMARS'.  And later, I bought them again but all I could find was 'Seedan' named units.
Oddly, when identified during installation using Generic Zigbee driver from Hubitat - all the units described themselves as 'Manufacturer - eWeLink'.

Over a short period of time, I began to see them show up in the watchdog.  If I didn't use them, they didn't seem to call home.  Discussing this in 
the Hubitat forums, @bravnel said they were lacking a refresh.  I searched for more information and stumbled on a driver from Markus Liljergren (of
'OhLa Labs' fame (see https://oh-lalabs.com/) that had a really nice feature for 'Presence' detection and gave it a try. 
The devices responded really nicely and I was pleased.

Sometime later I was working on Presence for my HSM and away modes and realized when I wanted to select a Presence device in a Rule, every one of my outlets 
showed up in the list.  In and of itself, this isn't a big deal but it was unwieldy and I didn't like devices that I knew were always present, showing up.
So I took it upon myself and edited the driver, removing the 'Presence' capability.  I left all the rest of the driver alone so as not to tinker with any
of the functionality of the refresh (presence is related to the device polling and refreshing).  And I was pleased.

During the editing, I saw the 'fingerprint' functionality that Markus had built in.  This allowed a device to identify itself so the driver would automagically
detect and be offered. It also defines the features and options of the device so the driver can control what is used and offered - it was after all a
generic driver to support many manufacturers.  Using a seperate tool (ALSO provided by Markus on his website) I generated my own fingerprint for my outlets
and added it to the driver.  Now, any time I added another eWeLink, CMARS or Seedan unit, automation kicked in very nicely.  And I was pleased.

Much later - I had 4 outlets on a power strip doing various controls in the garage.  The problem was I couldn't tell which device was which.  I needed a 
'Flash' feature (the ability to toggle back and forth the on off so I could ID the device I wanted to work on).  And speaking of toggle, that too was not 
available in the driver Markus had built.  So carefully, and with a huge learning curve due to Markus skill being lightyears ahead I toyed and struggled
until I was able to get the addition commands to function.  I tested as much as I could and finally I felt the project was completed.

And I was pleased.

While reading some online posts on Amazon, it became clear that the driver I had made was needed by others.  Because the eWeLink (and subsequent devices) 
were popular due to price and function - there was a current demand for them, but only the Generic Driver was being offered.  I felt it was important
to see that what modifications I had made were important to Markus.  I reached out to him and asked.  He described that he no longer was actively 
supporting the drivers for this but thanked me for asking.
I posted on the Hubitat forums about my customized driver asking for clarity on how to make the driver available without taking credit or risking blowback
for building work on top of Markus'.  I got a sort of tepid response, that wasn't ultra clear.  The gist of what I learned was:

Send a PR (pull request for Github)
Be Nice
Ask permission
and follow common courtesy. 

I reached out again to Markus, this time explaining in no uncertain terms what I wanted to do. His response was:

(Markus - Developer)
Hi Jim!

When it comes to keeping credits and original code referenced it’s not easy to “do it right”. You could do as gadget suggests and base it off a fork of my repo, but if you’re only going to change this one driver that is probably not ideal considering my repo has a lot of other drivers as well.

With that said, it’s perfectly fine to just take the one driver file and add to the list of contributors in the header with a clear note about what you added/changed. Adding a link to the original repo in the header could also be a good thing to do.

I’m happy my drivers can still help people even though I’ve had to discontinue support for them. Always glad to hear my work comes to good use!


Description

eWeLink Outlet is a driver that is modified from the great code of Markus Liljergren of 'OhLa Labs' fame (see https://oh-lalabs.com/)
This driver, when installed correctly on a Hubitat platform, provides selectable device driver that is designed for Generic Zigbee Outlets but has the additional
capability to refresh and be polled as well as has a Toggle and Flash command for added functionality.

This driver can be used for more than just eWeLink zigbee outlets (CMARS and Seedan are two known repackages of the eWeLink device).  Besides the targeted device
other manufacturers of Zigbee outlets are also supported.  

Requirements

I have not tested this on any platforms besides my Hubitat c5 currently running 2.2.9.130 Firmware.  That said, it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world develops.

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


Future updates

	It is not my intention to 'take over' Markus' driver.  Rather, I am simply providing my effort in the hopes it will help others.  However, it is possible
	that anyone using this driver for another manufacturer may indeed be prompted for a 'fingerprint' (that is left over from Markus code).  I did not remove
	the 'fingerprint' code but I did redirect it to me.  If I can implement anyones fingerprint for them, I'll be happy to do so.  Other than that, I have no
	intention of changing this driver further (with the exception of any fixes needed for work I've done).
