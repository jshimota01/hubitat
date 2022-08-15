Date & Time Parser (aka schedule_ur_garbage_cans :) )

Why?

Recent discussion (early 2022) on how to ID what day of the week it is in a rule prompted me to want a rule too... (in my case it was for garbage can day, which is split - every other week for all cans, grass/recycle is every week).

A need developed for me to be able to set rules and automations within my Hubitat environment that would allow me to gain control over time itself!  Okay. Nothing so "Doc Strange" but honestly, just being able to compare the day of the week, or the month, or week number of the year - each had a reason for me.  The idea hatched itself, as I was following dialog on the Hubitat community and others wanted 'what day of the week is it?' was discussed, I realized immediately afterwards that I too needed that specific bit of info.  Having dabbled with the season of the year (see Meteorological Seasons driver in HPM) I felt it wasn't much of a stretch to rip the guts out of my first driver and make a second along the lines of this new idea. 

Having extensive PHP knowledge, I am very comfortable with date/time and formats so I decided I would see what an effort looked like - and here it is!
Having already done some date/time work with my Meteorological Seasons driver, I wrote this driver this AM to allow me to parse values so I can easily control rules. Initially I was pulling the variables list from PHP but then I learned that Java comes up... um. short. At least in pre-defined ways.
  
Besides the power this app is showing to me (well. I wrote it so.. yea.) I've just added an Even and Odd Week variable. so Folks scheduling garbage can pick ups... Tada! Point your rule and enjoy.

***yet another update ***So, I've come to learn that boolean values for custom attributes aren't supported in Rules. This is known since...well .. 2020 at least.. regardless - I've converted all booleans to strings and adjusted their names to be a bit more humanistic. Much MUCH thanks to @sburke781 who just is huge as a persona around here!

***yet another - user id'd a case sensitive mistake - W is week of month, w is week of year.  Fixed. also he suggested adding week of month, so I did as well as odd and even month boolean.

Updates overview as of 08/12/2022

 Change History:
 
Date         Source      Version What                                        URL
----         ------      ------- ----                                        ---
2022-01-19   jshimota    0.1.0   Starting version
2022-01-19   Simon Burke 0.1.1   Used 2021-09-30 DateFormat app components   https://raw.githubusercontent.com/sburke781/hubitat/master/UtilityDrivers/DateFormat.groovy
2022-01-19   jshimota    0.1.2   Alpha release for testing
2022-01-20   jshimota    0.1.3   Worked on Scheduling cleanup and logging
2022-01-20   jshimota    0.1.4   First efforts to identify workarounds on php variations not found in Java
2022-01-20   jshimota    0.1.5   Heavy work done on basic function cleanup, as well as optimization
2022-01-20   jshimota    0.1.6   Added final missing attributes - DST, ObservesDST, LeapYear, Day Suffix and Ordinal
2022-01-20   jshimota    0.1.7   Tried adding Simons time and date stuff back, changed mind
2022-01-20   jshimota    0.1.8   Added update schedule ability
2022-01-20   jshimota    0.1.9   Commented tile features completely - no intent to support
2022-01-20   jshimota    0.2.0   Release (getting HPM value for package)
2022-01-20   jshimota    0.2.1   Added user compare value requests
2022-01-21   jshimota    0.2.2   Fixed switch case for Suffix, added noLead to minutes var, scheduler drop down and values
2022-01-22   jshimota    0.2.3   Added WeekOfYearOdd/Even for garbage cans.
2022-01-22   jshimota    0.2.4   with SBurke help - fixed boolean's  not supported by HE on comparators
2022-01-22   jshimota    0.2.5   Add of Even/Odd value to day of month number variables
2022-01-22   jshimota    0.2.6   Add of Even/Odd value to day of year number variables
2022-01-23   jshimota    0.2.7   TimeHour24NumNoLead fixed - added debug logging check to a line
2022-01-26   jshimota    0.2.8   Added String versions of comparison date times for user
2022-04-30   jshimota    0.2.9   2 minor text changes for clarity, attempt to fix schedule loop
2022-08-12   jshimota    0.3.0   Week of Year was case sensitive and showing week of month, added week of month as well
2022-08-15   jshimota    0.3.1   Typo error found in Week of Mon variables
 
Description

Date & Time Parser is a driver that provides, when installed correctly on a Hubitat platform, a selectable device driver.
This creates a virtual device, and from it, dashboard tiles, variables for Rules and other nice features are produced.

Requirements

I have not tested this on any platforms besides my Hubitat c7 currently running 2.3.0.xxx Firmware.  That said, it is relatively benign and should work on any HE hub and with fingers crossed will continue to work as the HE world evolves.


Installation and Configuration

    Date & Time Parser
        · First, using HPM (Hubitat Package Manager) locate and install this driver
        · Open the Devices page for your Hubitat Hub and click Add Virtual Device
        · Enter a Device Name and optionally a Device Label
        · In the Type drop-down list select the Date & Time Parser
        · If changes occur to any preference, click Save Preferences below the preferences section
        · Click Save Device to create the new virtual device
        
		It is possible that not in all cases does a 'Refresh' run at the time of install.  Click the Initialize button to initially set the values of the variables and attributes.
        
Additional Features

    The basic default settings are designed to cover most cases.  Normally, DescriptionText and Debug logging are disabled, but may be Enabled if necessary.  For testing and odd case use, the automatic update can be disabled, but is Enabled by default.   Auto Update interval can be set by picking from a drop down list from 1 - 59 minutes.  A tool such as this should have minimal impact so the default is 59 minutes.
    
Use the driver in RM rules

    To access these variables in an app such as Rule Machine - use the example here as a quide (an example Rule Machine 5.1 Rule).  Below is simply an example!
    
    Create a Rule, then name it
    Click Select Trigger Events
    Click 'click to set' from Select capability for new Trigger Event
    Select Custom Attribute from the Select Capability drop down
    Click Select Device
    Select your Virtual Device you created at installation
    Select Attribute from the drop down
    Choose DayNameText3 (for example)
    Choose the Equals sign
    Enter the 3 character day name (Case sensitive!) text value –  [IE; Uppercase first letter!]
    Press enter to complete the text line entry
    Click Done with this Condition.

Usable Variables with example :
    DayName : Wednesday (string)
    DayNameText3 : Wed (string)
    DayOfMonNum : 02 (number)
    DayOfMonNumNoLead : 2 (number)
    DayOfMonOrd : 2nd (string)
    DayOfMonSuf : nd (string)
    DayOfWeekNum : 3 (number)
    DayOfYearNum : 61 (number)
    DaysInMonthNum : 31 (number)
    GMTDiffHours : -0800 (string)
    IsDSTActive : false (string)
    IsDayOfMonNumEven : true (string)
    IsDayOfMonNumOdd : false (string)
    IsDayOfYearNumEven : false (string)
    IsDayOfYearNumOdd : true (string)
    IsLeapYear : false (string)
    IsObservesDST : true (string)
    IsWeekOfMonNumEven : false (string)
    IsWeekOfMonNumOdd : true (string)
    IsWeekOfYearNumEven : false (string)
    IsWeekOfYearNumOdd : true (string)
    MonthName : March (string)
    MonthNameText3 : Mar (string)
    MonthNum : 03 (number)
    MonthNumNoLead : 3 (number)
    TZID : Pacific Standard Time (string)
    TZIDText3 : PST (string)
    TimeAntePostLower : am (string)
    TimeAntePostUpper : AM (string)
    TimeHour12Num : 05 (number)
    TimeHour12NumNoLead : 5 (number)
    TimeHour24Num : 05 (number)
    TimeHour24NumNoLead : 5 (number)
    TimeMinNum : 00 (number)
    TimeMinNumNoLead : 0 (number)
    WeekOfMonNum : 4 (number)
    WeekOfYearNum : 33 (number)
    YearNum2Dig : 22 (number)
    YearNum4Dig : 2022 (number)
    comparisonDate : 20220302 (number)
    comparisonDateStr : 20220302 (string)
    comparisonDateTime : 202203020500 (number)
    comparisonDateTimeStr : 202203020500 (string)
    comparisonTime : 0500 (number)
    comparisonTimeStr : 0500 (string)
