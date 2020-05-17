# BTConfig
Configuration / monitoring software for the Bluetail Technologies P25RX receiver

<BR><BR>Executable
https://github.com/bluetailtech/BTConfig/blob/master/releases_exe/BTConfig-2020-05-16_2041.exe
<BR><BR>User Manual
https://github.com/bluetailtech/BTConfig/blob/master/Documentation/p25rx_user_manual.pdf

<BR><BR>Acquiring and Building From Source
<PRE>
Install Oracle Java 1.8  (or greater)

Install Netbeans 8.1

(https://netbeans.org/downloads/old/8.1/
  select Download Bundle ALL)
netbeans-8.1-linux.sh downloaded
chmod +x netbeans-8.1-linux.sh
./netbeans-8.1-linux.sh
Choose install path for netbeans
Select jvm location (/usr/lib/jvm/java-1.8.0-openjdk-amd64)
Select install location for glassfish  

Select install
Select finish

Get the source from :
git clone https://github.com/bluetailtech/BTConfig.git

Start netbeans and open the project BTConfig/Source Packages/btconfig/BTFrame.java

Select <Run> then "Run Project" in the Netbeans editor.  This will build and configure Netbeans on your system

If the previous is successful in building BTConfig then
BTConfig will now be excecuting, and the resulting BTConfig.jar file will be in BTConfig/dist

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
  
 <BR><BR>
Other projects used by BTConfig
<BR>
https://github.com/xiasc01/java-lame
<BR>
https://github.com/Fazecast/jSerialComm
<BR>
https://github.com/hendriks73/pcmsampledsp
