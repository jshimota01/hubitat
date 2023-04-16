Virtual Presence, Contact and Switch
Why?

I initially wrote this for a one-off case - or so I thought.  I needed a way to use presence pulling from a mobile device to show On/Off to Alexa.

Later - a simiilar community need was discussed, the ability to show Open or Closed but also reflect a switch.

Description
This driver is very, very simple - you download the code and add it to your drivers and save it.  Then, create a device and choose this driver.
The result is you have a Virtual Switch, the same as you would if you used the Hubitat provided virtual switch.
BUT. You will also find Open Closed Arrived Departed commands.

Open, Present (arrived) and On are all tied together.  So if you're Present, you are Open (like a contact) and you are On for a switch.
The opposite is true as well  Closed is Not Present is Off.

In addition in the preferences are 3 optional dropdowns for choosing a delay to toggle the state of these values. 
If any value is trigged by button or software, the other 2 states follow.

I needed the ability to autoOn the switch.  So if it got turned off, it turned itself back on based on the timer picked in the preferences.

Each of the pairs of states (On/Off, Open/Closed, Present/Not Present) has a dedicated autoOn trigger timer.  If you are set for AutoOn 2 seconds, then if you turn off the switch, 2 seconds later it will go on again.
Its important to note they are independent.  So you can control the auto trigger of a single pair, but still manually set the values as needed for the others and the state will hold true.


Requirements

I have not tested this on any platforms besides my Hubitat c7 currently running 2.3.5.125 Firmware.  Also, the age of the Groovy Libraries is 2.4.21 - quite old. but I'm not doing anything crazy here. That said, it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world develops.

Versions
 *      2022-11-27    jshimota      0.1.0.0          My original fork (I took this from another Github Repo
 *      2022-11-27    jshimota      0.1.0.1          Added Toggle command to flip state programmatically
 *      2022-12-05    jshimota      0.1.1.0          The version I was using for months without any problems!
 *      2022-12-08    jshimota      0.1.1.1          Added AutoOff
 *      2023-01-14    jshimota      0.1.1.2          Added Variable Auto Presence array for my own needs
 *		2023-04-15	  jshimota		0.1.1.3			 Added Open/Closed variables for community request / changed Command button label
 *      2023-04-15    jshimota      0.1.1.4          Debug work on auto on off for all 3 states

Installation and Configuration

    Virtual Presence Contact and Switch
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
    
    Standard 'switch' values of Rule Manager will reflect the true state of the switch. Same with Presence and Contact Sensor.  All 3 capabilities will show.