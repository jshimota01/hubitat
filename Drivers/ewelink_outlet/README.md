### Zigbee eWeLink-CMARS-Seedan Outlet

**Why?**

After I had tried a couple other brands I began using these low-cost Zigbee outlets because they were well, Zigbee, low-cost and seemed to work really well.  I also selected them because they act as a repeater making my Zigbee mesh more solid.  After my first purchase of 4, I liked them so much I went back to buy them again, but Amazon no longer sold them - they sold 'CMARS' which looked identical, so I took a shot.  And later still, I needed more but all I could find this time was 'Seedan' named units which again looked identical.  Oddly, when identified during installation using the generic Zigbee driver provided by Hubitat - all the units described themselves as 'Manufacturer - eWeLink'.

Over a short period of time, I began to see errors show up in the watchdog.  It seemed that if I didn't use them daily, they didn't seem to 'call home' and report they were still there.  Discussing this in the Hubitat forums, @bravnel said they were lacking a refresh.  I searched for more information and stumbled on a driver from Markus Liljergren (of 'OhLa Labs' fame (see https://oh-lalabs.com/) that had a really nice feature for 'Presence' detection and gave it a try.  The devices responded really nicely.  <u>And I was pleased.</u>

Sometime later I was working on Presence for my HSM and Away modes and realized when I wanted to select a Presence device in a Rule, every one of my outlets showed up in the list.  In and of itself, this isn't a big deal - but it was unwieldy and I didn't like devices that I knew were always present, (and not actually presence sensors) showing up.
So I took it upon myself and edited the driver from Markus, removing the 'Presence' capability.  I left all the rest of the driver alone so as not to tinker with any of the functionality of the refresh (presence is related to the device polling and refreshing).  <u>And I was pleased.</u>

During the editing, I saw the 'fingerprint' functionality that Markus had built in.  This allowed a device to identify itself so the driver would auto-magically detect and be offered. It also defines the features and options of the device so the driver can control what is used and offered - it was after all a generic driver to support many manufacturers.  Using a separate tool (ALSO provided by Markus on his website) I generated my own fingerprint for my outlets and added it to the driver.  Now, any time I added another eWeLink, CMARS or Seedan unit, automation kicked in very nicely.  <u>And I was pleased.</u>

Much later - I had 4 outlets on a power strip doing various controls in the garage.  The problem was I couldn't tell which device was which.  I needed a 'Flash' feature (the ability to toggle back and forth the on off, so I could ID the device I wanted to work on).  And speaking of toggle, that too was not available in the driver Markus had built.  So carefully, and with a huge learning curve due to Markus skill being light-years ahead I toyed and struggled until I was able to get the addition commands to function.  I tested as much as I could, and finally I felt the project was completed. 

<u>And I was pleased.</u>

While reading some online posts on Amazon, it became clear that the driver I had made was needed by others.  Because the eWeLink (and subsequent devices) were popular due to price and function - there was a current demand for them, but only the Generic Driver was being offered.  I felt it was important to see that what modifications I had made were important to Markus.  I reached out to him and asked.  He described that he no longer was actively supporting the drivers for this but thanked me for asking.
I posted on the Hubitat forums about my customized driver asking for clarity on how to make the driver available without taking credit or risking blow-back for building work on top of Markus'.  I got a sort of tepid response, that wasn't ultra clear.  The gist of what I learned was:

* Send a PR (pull request for GitHub)
* Be Nice
* Ask permission
* and follow common courtesy.


I reached out again to Markus, this time explaining in no uncertain terms what I wanted to do. His response was:

> (Markus - Developer)
> Hi Jim!
> 
> When it comes to keeping credits and original code referenced it’s not easy to “do it right”. You could do as gadget suggests and base it off a fork of my repo, but if you’re only going to change this one driver that is probably not ideal considering my repo has a lot of other drivers as well.
> 
> With that said, it’s perfectly fine to just take the one driver file and add to the list of contributors in the header with a clear note about what you added/changed. Adding a link to the original repo in the header could also be a good thing to do.
> 
> I’m happy my drivers can still help people even though I’ve had to discontinue support for them. Always glad to hear my work comes to good use!


**Description**

Zigbee eWeLink-CMARS-Seedan Outlet is a driver that is modified from the great code of Markus Liljergren of 'OhLa Labs' fame (see https://oh-lalabs.com/)
This driver, when installed correctly on a Hubitat platform, provides a selectable device driver that is designed as a Generic Zigbee Outlet driver but has the additional capability to refresh and be polled as well as has a Toggle and Flash command for added functionality.

This driver can be used for more than just eWeLink/CMARS/Seedan Zigbee outlets (CMARS and Seedan are two known repackages of the eWeLink device).  In addition to the targeted device other manufacturers of Zigbee outlets are also supported.  


**Requirements**

I have not tested this on any platforms besides my Hubitat c5 currently running 2.2.9.130 Firmware.  That said, it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world develops.

Changelog (as of today, 2021-10-15 - all changes by JAS)

<table>
<thead>
	<tr>
		<th>10/7/2021</th>
		<th>0.1.0.2</th>
		<th>My &#39;fork&#39; of this driver taken from the above original source</th>
	</tr>
</thead>
<tbody>
	<tr>
		<td>10/7/2021</td>
		<td>0.1.0.3</td>
		<td>Fingerprint for eWeLink added</td>
	</tr>
	<tr>
		<td>10/8/2021</td>
		<td>0.1.0.4</td>
		<td>Removed Presence capability so it doesn't show as a presence device in HE (left presence code intact for polling)</td>
	</tr>
	<tr>
		<td>10/9/2021</td>
		<td>0.1.0.5</td>
		<td>Added Flash command to flash light for locating device</td>
	</tr>
	<tr>
		<td>10/9/2021</td>
		<td>0.1.0.6</td>
		<td>Added Toggle command to flip state programmatically</td>
	</tr>
	<tr>
		<td>10/10/2021</td>
		<td>0.1.0.7</td>
		<td>Changed toggle from using x06 02 to x06 01/x06 00 so as to work with other devices that do not have toggle command in device code</td>
	</tr>
	<tr>
		<td>10/15/2021</td>
		<td>0.1.0.8</td>
		<td>Modified Text about send to developer so Markus doesn't get Fingerprint calls</td>
	</tr>
	<tr>
		<td>10/15/2021</td>
		<td>0.1.1.0</td>
		<td>Released version</td>
	</tr>
	<tr>
		<td>11/23/2021</td>
		<td>0.1.1.1</td>
		<td>Added AutoOff to Driver</td>
	</tr>
</tbody>
</table>



**Installation and Configuration**

Zigbee eWeLink-CMARS-Seedan Outlet

1. First, using HPM (Hubitat Package Manager) locate and install this driver
2. Open the Devices page for your Hubitat Hub and click Add Device (top right of Devices page)
3. Select the Zigbee button under Add Devices Manually:
4. Follow the necessary steps to pair your new device (usually just 'start pairing')
5. When prompted - name your device and select 'view device details'
6. Review your device - verify that Type shows Zigbee eWeLink-CMARS-Seedan Outlet
7. Click Save Device to complete the add device process


Generally the device defaults will provide all that is necessary and there is nothing further to change or adjust.  It is recommended that Enable Last Checkin Date and Enable Presence be left on - as this is used during polling and refresh of the device.  You may also desire to Enable debug logging initially to monitor and verify your device in the HE Log system.  Don't forget to Save Preferences and Save Device!

**Additional Features (not found in Markus' original driver)**

The Presence capability was removed as noted.  This is simply the advertisement of your outlet as a Presence detector.  Presence of the device is still functioning, it just won't display as an available device unnecessarily.

*Toggle* - as the name states, the toggle command was added to the code to provide a button and programmatic functionality to the device in case 'toggle' was needed for Rules etc.  Toggle literally changes the state of the device from off to on, or on to off.

*Flash* - the flash command is more commonly found on bulbs devices.  When the Flash button on the device page is pressed the device begins to cycle or change state back and forth from on to off.  It will continue this behavior ad-infinitum until the button is pushed again, or On or Off are pressed.

*AutoOff* - the autoOff command offers a method to 'toggle Off' the switch/outlet.  I wanted this feature so I could replace Rules I had made that were used in a garage door opener relay.  The values range from 0 (off) to 3600 seconds (1 hour).  Additional logging info commands were also added so user would know that the driver kicked the outlet off.  

**Future updates**

It is not my intention to 'take over' Markus' driver.  Although he did mention he would be happy to have someone take over the whole repository, it was a scope well beyond my ability.  Rather, I am simply providing my effort on 'forking' this particular driver in the hopes it will help others. 
As Markus has been clear he is no longer providing further support on this and other drivers he has made, there is area of this driver that refers back to him which could be a cause for confusion.  When running this driver it is possible a user will see a reference to communicate a fingerprint of the previously undeclared device.  This information used to point to Markus, but I have updated it to point at me - for this driver.  (not his original driver, just my modified version).  If I can implement anyone's fingerprint for them, I'll be happy to do so.  Other than that, I have no intention of changing this driver further (except any fixes needed for work I've done).
