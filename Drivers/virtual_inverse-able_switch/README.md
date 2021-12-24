Virtual Inverse-able Switch w-autoOff and Toggle

Why?

On a number of occasions, I've installed an App on my Hubitat hub that have an option to use a switch to disable or enable the app.
The problem I had was I like a switch to be ON to show the app is ON but in so many cases, you 'enable' the switch to DISABLE the app!
This confusion bugged me. I went looking for a driver or a virtual switch that could be 'reversed'.  In the end. I took a stab at 
making my own driver and this is the result.

Description
This driver is very, very simple - you download the code and add it to your drivers and save it.  Then, create a device and choose this driver.
The result is you have a Virtual Switch, the same as you would if you used the Hubitat provided virtual switch.
BUT. in the preferences is an option for Reverse Action.  When Enabled/On/True the On value of the switch sets the STATE of the switch to OFF.
Also the device state is displayed as True or False so you can review verify it actually is working as you want.


Requirements

I have not tested this on any platforms besides my Hubitat c5 currently running 2.2.9.130 Firmware.  That said, it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world develops.

New Features
1.x		Initial release
1.1		Added support parent and initialization to off state regardless of Reverse Action preference switch
2.0     Added autoOff and Toggle
2.1     Released via HPM - deprecated Previous version

Installation and Configuration

    Virtual Inverse-able Switch
        · First, using HPM (Hubitat Package Manager) locate and install this driver
        · Open the Devices page for your Hubitat Hub and click Add Virtual Device
        · Enter a Device Name and optionally a Device Label
        · In the Type drop-down list select the Virtual Inverse-able Switch
        · In the preference - Notice the defualt of Reverse Action is Off.  If you want this to reverse, Enable Reverse Action.
        · If changes occur to any preference, click Save Preferences below the preferences section
        · Click Save Device to create the new virtual device
        
Additional Features

    None at this time

Use the driver in RM rules
    
    Standard 'switch' values of Rule Manager will reflect the true state of the switch. If your switch displays 'On' in a dashboard
    you will see 'Off' in the rule when Reverse Action is enabled. it's tricky!
