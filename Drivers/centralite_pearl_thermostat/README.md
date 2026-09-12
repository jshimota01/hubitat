# Centralite Pearl (model 31577100) Zigbee Thermostat 

### Why?

My environment is pure Zigbee.  The options available for thermostats in the Zigbee area are limited, and the units that are available, have hardware functionality and features that can cause issues.  I recently moved to a house which had a great integrated heater and air conditioning which caused me issues with my previous (OLD) thermostat.  It did not support A/C, just heating.  I located after a lot of searching, a well liked Zigbee unit that could do both Heat and Cooling.  Initially it was great with the Generic HE driver BUT it would stumble if I put it into Auto mode.  I thought it was a driver issue - it was but it wasn't.  The device actually didn't support Auto!  HE Generic allowed me to turn it to auto, but it was dead and my heater-A/C unit would sit quiet and not function. (it was Off.)

I reached out to the community asking for any driver I could use as a start point and was blessed to receive a pointer to a Smartthings driver written years ago.

Taking the original source and making heavy modifications to it, and with the help of AI, This is the resultant driver.  It is working extremely well!

### Customizations
### I 

One shortcoming of this model unit is it has no ability to take advantage of the Fan Circulate function.  My solution was to create a 30 minute on/30 minute off cycle.  The cycle disables for any other Fan Mode, but will simply push air through the system regardless if the thermostat is in heat, cool or Off.

Another modification needed was to make up for the lack of an 'Auto' mode.  Most thermostats that control both heat and air conditioning have an Auto mode that will automatically select heat or cooling based on the current temperature.  Using the setpoints, if the device temperature sensor detects the temperature is above the Cooling setpoint or below the Heating setpoint, they switch on the appropriate source.  In this case, Auto mode is not used.  Selecting it on the UI of the device will shortly return to the previous mode with no impact.

Other modifications was adding Power source - the unit does a great job of providing information about the batteries, and if using the C-Wire (24v) it will reflect that is the source of power.

Below is the updates overview as of 6-3-2026

 Change History:
 
Change History:
2021-09-30    dagrider      0.1.0       Starting version
2026-06-02    jshimota      0.1.1       Initial edit, cleanup GNU, basics, remove excess comments
2026-06-03    jshimota      0.2.0       Gemini Modernization and optimization
2026-06-03    jshimota      0.2.1       Adding in log and debug control
2026-06-03    jshimota      0.2.2       Gemini improvements and bug hunt
2026-06-03    jshimota      0.2.8       Intercept Fan Circulate Exception
2026-06-03    jshimota      0.2.9       Sanitized inline tracking tokens
2026-06-03    jshimota      0.3.0       Implemented 30-min on/off circulation scheduling logic
2026-06-03    jshimota      0.3.1       Fixed UI dropdown options mapping to use platform standard tokens
2026-06-03    jshimota      0.3.2       Added explicit Initialize command to reveal UI button
2026-06-03    jshimota      0.3.3       Added explicit fanOff command to reveal UI action button
2026-06-03    jshimota      0.3.4       Added supportedThermostatModes attribute definition and variable                                             assignment to populate system UI controls
2026-06-03	  jshimota      0.3.5       Repaired Battery and Power Source code
2026-06-03    jshimota      0.3.6       Changed Setpoint Level button namespace
2026-06-03    jshimota      0.3.7       Fixed 'Auto' mode (which isn't supported on this device)
2026-06-03    jshimota	    0.3.9	    Modified text string to display temperature values with °F
2026-06-03    jshimota      0.4.1	    Text cleanup of UI to show Celsius or Fahrenheit as needed.
2026-06-03	  jshimota      0.4.3 	    Text change of UI of Heat and Cool to Heating and Cooling
 
### Description

Centralite Pearl Zigbee Thermostat is a driver that provides, when installed correctly on a Hubitat platform, a selectable device driver that can be used with the Centralite Pearl Thermostat (sometimes known from it's model #: 31577100.

### Requirements

I have not tested this on any platforms besides my Hubitat c7 currently running 2.4.4.xxx Firmware.  That said, it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world evolves.


### Installation and Configuration

    Centralite Pearl Zigbee Thermostat
        · First, using HPM (Hubitat Package Manager) locate and install this driver
        · Open the Devices page for your Hubitat Hub and click Add Device
        · Begin the pairing function of the Pearl Thermostat
        · Once it is connected, verify the driver is in use from the Device Info page checking "Type"
        · On the Commands page for your device, click Initialize and Configure (they will run automatically    anyway but it's always good to fill the Current States with starting values)
        · Check your logging, and verify that no errors are newly present
        
        
		It is possible that not in all cases does a 'Refresh' run at the time of install.  Click the Initialize button to initially set the values of the variables and attributes.
        
### Additional Features

    The basic default settings are designed to cover most cases.  Normally, DescriptionText is Enabled and Debug logging is disabled, but for a first time use, the debug logging turns on for 30 minutes before turning itself off.
    
    Toggle Hold Mode was a bit confusing at first.  When enabled, it will override all manual controls and allow only the in-device schedule to function.  This should be OFF in almost all cases!  But, I provided the option for user control as it was already built into the Smartthings original code and I validated it.
     
### Use the driver in RM rules

    The Centralite Pearl Zigbee Thermostat can now be used in any rules, exactly like any other Thermostat.  I don't feel this is necessary to elaborate on, see the Hubitat Community for more answers.
