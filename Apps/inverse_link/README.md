### Inverse Link Two Switches

**Why?**

I fully understand the basic boolean concept of 'Not'.  
For a switch that is 'NOT ON' obviously means it is off.  However, when doing certain work I generally don't like to build scripts with logic using NOT.  It confuses me and makes my head hurt.

I found this very rudimentary driver that would link two switches - so I modified it to work as a flipper- if a switch goes on, the other goes off.  I use this for example in 'Awake/Asleep'.
I have two virtual switches, but so many rules all over that I can't see if I'm controlling the switches correctly.  So I built this - if I switch to 'Asleep' (ON), then 'Awake' goes OFF.


**Requirements**

I have not tested this on any platforms besides my Hubitat c7 currently running 2.3.6.144 HE Firmware.  That said, it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world develops.

