
# BTConfig
Configuration Software for the Bluetail Technologies <a href="https://bluetailtechnologies.com/products/p25rx-digital-police-receiver"> P25RX Digital Police Receiver </a> 
<BR>
<BR>Recommended Release (stable)  
https://github.com/bluetailtech/BTConfig/blob/master/releases_exe/BTConfig-2020-11-13_1817.exe 
  
<BR><BR>User Manual
https://github.com/bluetailtech/BTConfig/blob/master/Documentation/p25rx_user_manual.pdf  (updated 2020-10-19)


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

2020-07-28
add decoders dir for decoders covered by GPL licensing
<BR><BR>
2020-09-13 The P25RX firmware was inspired by portions of the following projects:<BR>
https://github.com/jgaeddert/liquid-dsp<BR>
https://github.com/szechyjs/dsd<BR>
https://github.com/szechyjs/mbelib<BR>
https://github.com/uwarc/dsd-dmr<BR>
https://github.com/osmocom/op25  (Our partial port of TDMA to C can be found on our github page in the decoders/p25_phase_2 directory)
