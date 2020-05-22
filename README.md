
# BTConfig
Configuration Software for the Bluetail Technologies <a href="https://www.tindie.com/products/20075/">P25RX Digital Police Receiver </a> <img src="https://raw.githubusercontent.com/bluetailtech/BTConfig/master/images/p25rx_small.png">

<BR><BR>Executable
https://github.com/bluetailtech/BTConfig/blob/master/releases_exe/BTConfig-2020-05-16_2041.exe
<BR><BR>User Manual
https://github.com/bluetailtech/BTConfig/blob/master/Documentation/p25rx_user_manual.pdf
<BR><BR><BR>Sample audio (local event 05-17-20)
https://github.com/bluetailtech/BTConfig/blob/master/audio_samples/local_suspects_found_05-17-20.wav

<BR><BR>Acquiring and Building From Source
<PRE>
Get Latest BTConfig source :
git clone https://github.com/bluetailtech/BTConfig.git

Install Oracle Java 1.8  (or greater)

Install Netbeans 8.1
  
  Dowload Netbeans Installer from https://netbeans.org/downloads/old/8.1/
    Excecute Netbeans installer.
   Linux :
      chmod +x netbeans-8.1-linux.sh
    ./netbeans-8.1-linux.sh
   Windows :
      netbeans-8.1-javase-windows.exe

Start netbeans and open the project BTConfig/Source Packages/btconfig/BTFrame.java

In the Netbeans editor :  select <Run> then "Run Project". This will build and execute the 
resulting BTConfig.jar. The file will be in BTConfig/dist

Now BTConfig.exe can be built from inside the BTConfig directory with 'sh build.sh' or 'ant exe'
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
  
<BR><BR>BTConfig - Talk Group Configuration
<img src="https://raw.githubusercontent.com/bluetailtech/BTConfig/master/images/ss5.png">
  
<BR><BR>
Video of configuration software in action https://youtu.be/Pr4ATosuvKU
  
 <BR><BR>
Other projects used by BTConfig
<BR>
https://github.com/xiasc01/java-lame
<BR>
https://github.com/Fazecast/jSerialComm
<BR>
https://github.com/hendriks73/pcmsampledsp
