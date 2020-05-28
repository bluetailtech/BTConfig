
<meta name="google-site-verification" content="6MOVT7WLZ02v5-FP-dq7bhOBzZPCfAUuDLI3jJjINz0" />
# BTConfig
Configuration Software for the Bluetail Technologies <a href="https://bluetailtechnologies.com/products/p25rx-digital-police-receiver"> P25RX Digital Police Receiver </a> 

<BR><BR>Executable
https://github.com/bluetailtech/BTConfig/blob/master/releases_exe/BTConfig-2020-05-22_0746.exe
<BR><BR>User Manual
https://github.com/bluetailtech/BTConfig/blob/master/Documentation/p25rx_user_manual.pdf


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
Other projects used by BTConfig
<BR>
https://github.com/xiasc01/java-lame
<BR>
https://github.com/Fazecast/jSerialComm
<BR>
https://github.com/hendriks73/pcmsampledsp
