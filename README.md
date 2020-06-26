
# BTConfig
Configuration Software for the Bluetail Technologies <a href="https://bluetailtechnologies.com/products/p25rx-digital-police-receiver"> P25RX Digital Police Receiver </a> 
<BR><BR>Latest Experimental (experimental / possibly unstable)
https://github.com/bluetailtech/BTConfig/blob/master/releases_exe/BTConfig-2020-06-26_1434.exe
<BR>NOTE: if you are using these verions, please keep track of which releases work well for your area and which ones don't.  It should make it much easier to isolate issues. Thanks.
<BR>0983 - may have been an issue with new AGC code.  attempt to fix.
<BR>Correction of rssi offset (was reading 4dB high).
<BR>Better performance at low signal levels
<BR>Configurable options for corrupted voice sub-frame.  Replace with: Silence, repeat last good voice subframe up to 4 times, analog noise
<BR>Improvements to RF AGC
<BR>Unlock AGC if on control channel and TSBK_PS is falls below 10/sec
<BR>Automatic detection of Mac OS X
<BR>This version will reset the configuration

<BR>Latest Executable (testing)
https://github.com/bluetailtech/BTConfig/blob/master/releases_exe/BTConfig-2020-06-21_1822.exe
<BR>Change Audio AGC rate in in radio and BTConfig software. reduce some audio glitches  (firmware update)
<BR>Allow voiced audio with more bit errors  (firmware update)
<BR>Increase RF AGC hysteresis

<BR>Previous Release (stable)  
https://github.com/bluetailtech/BTConfig/blob/master/releases_exe/BTConfig-2020-06-15_2226.exe  
<BR>fixes "allow unknown talk groups" always enabled issue
<BR>fixes "zip code search" issue with leading zero
<BR>add new option to PC speaker audio "insert zero" to get rid of glitches in Java->PC Speaker audio
  
<BR><BR>Previous Releases
https://github.com/bluetailtech/BTConfig/blob/master/releases_exe/
  
<BR><BR>User Manual
https://github.com/bluetailtech/BTConfig/blob/master/Documentation/p25rx_user_manual.pdf


<BR><BR>Acquiring and Building From Source
<PRE>
Get Latest BTConfig source :
git clone https://github.com/bluetailtech/BTConfig.git

1) Install Oracle Java 1.8  (or greater)

2) Install Netbeans 8.1   
(older version of Netbeans. Not susceptable to the same security issues that some newer versions are )
  
  Dowload Netbeans Installer from https://netbeans.org/downloads/old/8.1/
    Excecute Netbeans installer.
   Linux :
      chmod +x netbeans-8.1-linux.sh
    ./netbeans-8.1-linux.sh
   Windows :
      netbeans-8.1-javase-windows.exe

3) Start netbeans and open the project BTConfig/Source Packages/btconfig/BTFrame.java

4) In the Netbeans editor :  select <Run> then "Run Project". This will build and execute the 
resulting BTConfig.jar. The file will be in BTConfig/dist

5) Now BTConfig.exe can be built from inside the BTConfig directory with 'sh build.sh' or 'ant exe' 
(note: you may be able to skip steps 2-4 for a build-only)

</PRE>
<BR><BR>
Starting the software
<PRE>
For Windows 7,10 systems with Java 1.8 or greater installed,  
  double click on the BTConfig/exe/BTConfig.exe executable
  
For another OS (such as Linux),  you can use the .exe as well.  Start with 'java -jar BTConfig.exe'
</PRE>
    
<BR><BR>BTConfig in Monitor Mode  
<img src="https://raw.githubusercontent.com/bluetailtech/BTConfig/master/images/ss1.png">

<BR><BR>BTConfig Integrated Frequency Database
<img src="https://raw.githubusercontent.com/bluetailtech/BTConfig/master/images/ss4.png">
  
<BR><BR>BTConfig - General Configuration Of P25RX Device
<img src="https://raw.githubusercontent.com/bluetailtech/BTConfig/master/images/ss2.png">
  
 <BR><BR>
Other projects used by BTConfig
<BR>
https://github.com/xiasc01/java-lame
<BR>
https://github.com/Fazecast/jSerialComm
<BR>
https://github.com/hendriks73/pcmsampledsp
