
//MIT License
//
//Copyright (c) 2020 bluetailtech
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package btconfig;

import java.io.*;
import java.awt.*;
import javax.swing.filechooser.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fazecast.jSerialComm.*;
import net.sourceforge.lame.mp3.*;
import net.sourceforge.lame.lowlevel.*;
//import net.sourceforge.lame.mpeg.*;

import javax.sound.sampled.*;

import java.util.prefs.Preferences;


///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class BTFrame extends javax.swing.JFrame {

  /*
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class insertZeroTask extends java.util.TimerTask
{
    byte[] zero320 = new byte[320+32];
    int icount=0;

    public void run()
    {
      try {
        if(aud!=null ) {
          if(icount++<30) {
            if(aud!=null) aud.playBuf(zero320);
            //System.out.println("insert 176 zero samples into aud stream");
          }
        }
      } catch(Exception e) {
        //e.printStackTrace(System.out);
      }
    }
}
  */

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class updateTask extends java.util.TimerTask
{

    public void run()
    {
      try {


        if(  aud!=null && ( new java.util.Date().getTime() - audio_tick_start ) > 55) {
          aud.audio_tick();
          audio_tick_start = new java.util.Date().getTime();
        }

        if(status_timeout>0) {
          status_timeout--;
        }

        if(  status_timeout==0 && new java.util.Date().getTime() - status_time  > 2000) {
          status_time = new java.util.Date().getTime();
          setStatus("");
        }


        /*
        long usb_ctime = new java.util.Date().getTime();
        if(wdog_time==0 || usb_ctime - wdog_time > 5000) {
          wdog_time = usb_ctime;
          if(sys_config!=null && is_mac_osx==0) sys_config.do_usb_watchdog(serial_port);
          //System.out.println("\r\nusb watchdog");
        }
        */

        if(bluetooth_streaming_timer>0) {
          bluetooth_streaming_timer--;
          if(bluetooth_streaming_timer==0) {
            bluetooth_streaming=0;
          }
        }

        if(command_input_timeout>0) {
           command_input_timeout--;
          if(command_input_timeout==0) {
            command_input=0;
          }
        }

        if(p25_status_timeout>0) {
          p25_status_timeout--;
          if(p25_status_timeout==0 || do_write_config==1) {
            p25_status_timeout=3000;
            //status.setText("");
            l3.setText("");
            tg_indicator.setBackground(java.awt.Color.black);
            tg_indicator.setForeground(java.awt.Color.black);
            sq_indicator.setForeground( java.awt.Color.black );
            sq_indicator.setBackground( java.awt.Color.black );
            sysid.setText("");
            wacn.setText("");
            nac.setText("");
            rfid.setText("");
            siteid.setText("");
            l3.setText("NO SIG");

            do_synced=false;
          }
          else {
            do_synced=true;
          }
        }


        if(is_connected==1 && do_test_freqs==1 && do_update_firmware==0) {
          do_test_freqs=0;
          if(roaming_tests!=null) roaming_tests.test_selected_freqs(parent,serial_port);
        }

        if(is_connected==1 && do_roam_freq==1 && do_update_firmware==0) {
          do_roam_freq=0;
          if(roaming_tests!=null) {
            int[] rows = freq_table.getSelectedRows();
            if(rows.length>0) {
              freq_table.setRowSelectionInterval(rows[0],rows[0]);
              String freq_to_use = (String) freq_table.getModel().getValueAt(rows[0],3);

              double d = 0.0; 
              try {
                d = new Double(freq_to_use).doubleValue();
                freq_to_use = String.format("%3.8f", d);

                if(freq_table.getModel().getValueAt(rows[0],1)==null) freq_table.getModel().setValueAt("",rows[0],1);
                if(freq_table.getModel().getValueAt(rows[0],8)==null) freq_table.getModel().setValueAt("",rows[0],8);
                if(freq_table.getModel().getValueAt(rows[0],9)==null) freq_table.getModel().setValueAt("",rows[0],9);

                frequency_tf1.setText(freq_to_use);

                do_read_config=1;
                do_write_config=1;
                cpanel.reset_ref_est();

                current_sys_id = 0;
                current_wacn_id = 0; 
                wacn.setText("");
                sysid.setText("");
                nac.setText("");

                if(prefs!=null) {
                  String str1 = (String)  parent.freq_table.getModel().getValueAt(rows[0],8);
                  String str2 = (String) parent.freq_table.getModel().getValueAt(rows[0],9);
                  String str3 = (String) parent.freq_table.getModel().getValueAt(rows[0],1);
                  if(str1!=null && str1.length()>0 && str2!=null && str2.length()>0 && str3!=null && str3.length()>0) {
                    if(prefs!=null) parent.prefs.put("city_state_"+freq_to_use.trim(), str1+","+str2+"_"+str3 );
                  }
                }
              } catch(Exception e) {
              }
            }
          }
        }

        //auto re-connect?
        if(serial_port!=null && !serial_port.isOpen() && is_connected==1 && do_update_firmware==0) {
          is_connected=0;
          try {
            setStatus("device reset detected");
            SLEEP(100);
          } catch(Exception e) {
          }
          do_connect=1;
          do_read_config=1;
        }


        if(do_zipsearch==1) {
          do_zipsearch=0;
          if(zs==null) zs = new zipsearch(parent);
          String[] args = new String[2];
          String zip_str = "";
          try {
            zip_str = new Integer(zipcode.getText().trim()).toString();
          } catch(Exception e) {
          }
          args[0] = zip_str; 
          args[1] = search_radius.getText().trim();
          zs.search(args);
        }

        if(do_zipsearch2==1) {
          do_zipsearch2=0;
          if(zs==null) zs = new zipsearch(parent);
          String[] args = new String[3];
          args[0] = city.getText().trim();
          args[1] = state.getText().trim();
          args[2] = search_radius.getText().trim();
          zs.search2(args);
        }

        if(do_read_config==1 && serial_port!=null && is_connected==1) {
          if(sys_config==null) sys_config = new SYSConfig(parent);
          if(sys_config!=null) {

            String cmd= new String("en_voice_send 0\r\n");
            serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
            cmd= new String("logging -999\r\n");
            serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

            sys_config.read_sysconfig(parent, serial_port);

            cmd= new String("en_voice_send 1\r\n");
            serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
            cmd= new String("logging 0\r\n");
            serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

            do_read_roaming=1;
          }
        }

        mp3_separate_files.setSelected( false ); 


        if(do_agc_update==1) {
          do_agc_update=0;
            //System.out.println(evt);
        }

        if(do_restore_roaming==1 && is_connected==1 && do_update_firmware==0 && do_read_talkgroups==0) {

          try {
            JFileChooser chooser = new JFileChooser();

            File cdir = new File(home_dir+"p25rx");
            chooser.setCurrentDirectory(cdir);


            FileNameExtensionFilter filter = new FileNameExtensionFilter( "p25rx_roaming backups", "rom");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showDialog(parent, "Restore Roaming Records");

            BufferedInputStream bis=null;

            if(returnVal == JFileChooser.APPROVE_OPTION) {
              File file = chooser.getSelectedFile();
              bis = new BufferedInputStream(new FileInputStream(file));
              System.out.println("restoring roaming records from: " + file.getAbsolutePath()); 
            }

            if(bis!=null) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

              if(roaming_tests!=null) roaming_tests.restore_roaming(parent, bis, serial_port);



              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
            }
            setProgress(-1);
          } catch(Exception e) {
            //e.printStackTrace();
          }

          do_restore_roaming=0;
        }

        //Import alias CSV file
        if(do_alias_import==1 && is_connected==1 && do_update_firmware==0 && do_read_talkgroups==0) {

          try {

            JFileChooser chooser = new JFileChooser();

            File cdir = new File(home_dir+"p25rx");
            chooser.setCurrentDirectory(cdir);


            FileNameExtensionFilter filter = new FileNameExtensionFilter( "p25rx_alias_import", "csv");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showDialog(parent, "Import CSV Alias Records");

            LineNumberReader lnr=null;

            if(returnVal == JFileChooser.APPROVE_OPTION) {
              File file = chooser.getSelectedFile();
              lnr = new LineNumberReader( new FileReader(file) );
              System.out.println("importing aliases from: " + file.getAbsolutePath()); 
            }

            if(lnr!=null) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

              if(tg_config==null) tg_config = new TGConfig(parent);
              alias.import_alias_csv(parent, lnr);

              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
            }
            setProgress(-1);
          } catch(Exception e) {
            //e.printStackTrace();
          }

          do_alias_import=0;
        }

        //RESTORE talkgroup CSV file
        if(do_restore_tg_csv==1 && is_connected==1 && do_update_firmware==0 && do_read_talkgroups==0) {

          try {

            JFileChooser chooser = new JFileChooser();

            File cdir = new File(home_dir+"p25rx");
            chooser.setCurrentDirectory(cdir);


            FileNameExtensionFilter filter = new FileNameExtensionFilter( "p25rx_talkgroup backups", "csv","dsd");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showDialog(parent, "Import CSV Talk Group Records");

            LineNumberReader lnr=null;

            if(returnVal == JFileChooser.APPROVE_OPTION) {
              File file = chooser.getSelectedFile();
              lnr = new LineNumberReader( new FileReader(file) );
              System.out.println("importing talkgroups from: " + file.getAbsolutePath()); 
            }

            if(lnr!=null) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

              if(tg_config==null) tg_config = new TGConfig(parent);
              tg_config.import_talkgroups_csv(parent, lnr, serial_port);
              do_read_talkgroups=1;

              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
            }
            setProgress(-1);
          } catch(Exception e) {
            //e.printStackTrace();
          }

          do_restore_tg_csv=0;
        }

        //RESTORE TGP file
        if(do_restore_tg==1 && is_connected==1 && do_update_firmware==0 && do_read_talkgroups==0) {

          try {

            JFileChooser chooser = new JFileChooser();

            File cdir = new File(home_dir+"p25rx");
            chooser.setCurrentDirectory(cdir);


            FileNameExtensionFilter filter = new FileNameExtensionFilter( "p25rx_talkgroup backups", "tgp");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showDialog(parent, "Restore Talk Group Records");

            BufferedInputStream bis=null;

            if(returnVal == JFileChooser.APPROVE_OPTION) {
              File file = chooser.getSelectedFile();
              bis = new BufferedInputStream(new FileInputStream(file));
              System.out.println("restoring talkgroups from: " + file.getAbsolutePath()); 
            }

            if(bis!=null) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

              if(tg_config==null) tg_config = new TGConfig(parent);
              tg_config.restore_talkgroups(parent, bis, serial_port);
              do_read_talkgroups=1;

              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
            }
            setProgress(-1);
          } catch(Exception e) {
            //e.printStackTrace();
          }

          do_restore_tg=0;
        }


        if(firmware_checked==1) {
          check_firmware.setEnabled(false);
        }

        if(sig_meter_timeout>0) {
          sig_meter_timeout-=10;

          /*
          if(sig_meter_timeout<=0) {
            l1.setVisible(false);
            l2.setVisible(false);
            l3.setVisible(false);
            rssim1.setVisible(false);
            rssim2.setVisible(false);
          }
          else {
            l1.setVisible(true);
            l2.setVisible(true);
            l3.setVisible(true);
            rssim1.setVisible(true);
            rssim2.setVisible(true);
          }
          */

          if(sig_meter_timeout<=0) {
            rssim1.setValue(-130,false);
            rssim2.setValue(-130,false);
            p25_status_timeout=3000;
            sig_meter_timeout=1000;
            //l3.setText("Desc:                          ");
            //l3.setText("");
          }
          else {
          }
        }

        if( is_connected==0 ) {
          send_tg.setEnabled(false);
          read_tg.setEnabled(false);
          discover.setEnabled(true);
          disconnect.setEnabled(false);
          read_config.setEnabled(false);
          write_config.setEnabled(false);

        }
        else {
          send_tg.setEnabled(true);
          read_tg.setEnabled(true);
          discover.setEnabled(false);
          disconnect.setEnabled(true);
          read_config.setEnabled(true);
          write_config.setEnabled(true);
        }

        if(is_connected==1 && do_update_firmware==1) {
          if(firmware_checked==0) {
            check_firmware.setEnabled(false);

              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

            BufferedInputStream bis = new BufferedInputStream( getClass().getResourceAsStream("/btconfig/main.aes") );
            new firmware_update().send_firmware(parent, bis, serial_port);
            setProgress(-1);
            //firmware_checked=1;
            //do_update_firmware=0;
              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
          }
        }
        else if(is_connected==1 && do_backup_roaming==1) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

                if(roaming_tests!=null) roaming_tests.backup_roaming(parent, serial_port);
                setProgress(-1);
                do_backup_roaming=0;

              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
        }
        else if(is_connected==1 && do_erase_roaming==1) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

                if(roaming_tests!=null) roaming_tests.erase_roaming(parent, serial_port);
                setProgress(-1);
                do_erase_roaming=0;

              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
        }
        else if(is_connected==1 && do_write_roaming_flash_only==1) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

                if(roaming_tests!=null) roaming_tests.write_roaming_flash(parent, serial_port);
                setProgress(-1);
                do_write_roaming_flash_only=0;

              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
        }
        else if(is_connected==1 && do_append_roaming==1) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

                if(roaming_tests!=null) roaming_tests.append_roaming(parent, serial_port);
                setProgress(-1);
                do_append_roaming=0;

              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
        }
        else if(is_connected==1 && do_update_roaming==1) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

                if(roaming_tests!=null) roaming_tests.send_roaming(parent, serial_port, 0);
                setProgress(-1);
                do_update_roaming=0;

              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
        }
        else if(is_connected==1 && do_update_talkgroups==1) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
          if(tg_config==null) tg_config = new TGConfig(parent);
          tg_config.send_talkgroups(parent, serial_port);
          setProgress(-1);
          do_update_talkgroups=0;
              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
        }
        else if(is_connected==1 && do_update_firmware==0 && do_read_roaming==1 && skip_bytes==0) {
          String cmd= new String("en_voice_send 0\r\n");
          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
          cmd= new String("logging -999\r\n");
          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

            if(roaming_tests!=null) roaming_tests.read_roaming(parent, serial_port);
            setProgress(-1);

            //if(parent.zs!=null) {
              //parent.zs.search3();
              //do_read_roaming=0;
            //}

          if(do_read_roaming==0) {
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
          }
        }
        else if(is_connected==1 && do_update_firmware==0 && do_read_talkgroups==1 && skip_bytes==0) {
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
          if(tg_config==null) tg_config = new TGConfig(parent);
          tg_config.read_talkgroups(parent, serial_port);
          setProgress(-1);

          if(do_read_talkgroups==0) {
              cmd= new String("en_voice_send 1\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
          }

          if(do_update_firmware2==1) {
            do_update_firmware2=0;
            do_update_firmware=1;
          }

          status.setText(system_alias.getText());
          status.setVisible(true);
        }
        else if(is_connected==0 && do_connect==1) {

            serial_port = find_serial_port();

            macid.setVisible(false);
            macid.setText("");

            if(serial_port==null) {
              setStatus("\r\ndiscovering device.  Please wait...");
              SLEEP(600);
            }

            if(serial_port!=null && serial_port.openPort(200)==false) {
              setStatus("\r\nserial port busy. please wait. retrying....");
            }
            else if(serial_port!=null) {
              do_connect=0;

              serial_port_name = serial_port.getSystemPortName();
              serial_port.setBaudRate( 1000000 ); //this probably doesn't really matter
              serial_port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);

              is_connected=1;
              SLEEP(600);

              check_firmware.setEnabled(true);

                do_read_talkgroups=1;
                do_read_roaming=1;
                do_read_config=1;

              discover.setEnabled(false);
              int baudrate = serial_port.getBaudRate();

              byte[] result=new byte[1024];
              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              SLEEP(100);
              int rlen=serial_port.readBytes( result, 1024);
              //System.out.println("result: "+new String(result) );
              result=new byte[1024];
              cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              SLEEP(100);
              rlen=serial_port.readBytes( result, 1024);


              result=new byte[64];
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              SLEEP(100);
              rlen=serial_port.readBytes( result, 64);
              //System.out.println("result: "+new String(result) );


              for(int i=0;i<15;i++) {

                  result=new byte[64];
                  String mcmd = "mac_id\r\n";  
                  serial_port.writeBytes( mcmd.getBytes(), mcmd.length(), 0);
                  SLEEP(100);
                  rlen=serial_port.readBytes( result, 64);

                  String mid = ""; 
                  try {
                    mid = mid.replace(" ","_");
                    mid = new String(result,0,16).trim();
                  } catch(Exception e) {
                    mid="";
                  }
                  if(mid.startsWith("0x") && mid.length()==14) {
                    System.out.println("mac_id:"+mid +":");
                    parent.sys_mac_id = mid;

                    update_prefs();

                    open_audio_output_files();
                    macid.setVisible(true);
                    macid.setText("MAC: "+sys_mac_id);

                    if(alias==null) alias = new Alias(parent, parent.sys_mac_id, home_dir);

                    break;
                  }

                  if(i==9) {
                    System.out.println("mac_id_not_good:"+mid +":");
                    //JOptionPane.showMessageDialog(parent, "Couldn't find device serial number.  Closing application.", "ok", JOptionPane.OK_OPTION);
                    //System.exit(0);
                    serial_port=null;
                    do_connect=1;
                    is_connected=0;
                    return;
                  }

                  System.out.println("error reading serial number.  Retry "+i);
                    result=new byte[4096];
                    cmd= new String("en_voice_send 0\r\n");
                    serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                    SLEEP(500);
                    rlen=serial_port.readBytes( result, 4096);
                    SLEEP(500);
                    rlen=serial_port.readBytes( result, 4096);
              }





            }
        }
        else if(is_connected==1 && do_toggle_record==1 && skip_bytes==0) {
          do_toggle_record=0;

          String cmd= new String("logging 0\r\n");
          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);


          toggle_recording( !record_to_mp3.isSelected() );

        }
        else if(is_connected==1 && do_update_firmware==0) {
          avail = serial_port.bytesAvailable();
          str_idx=0;

          //if( (rx_state>0 && avail>=32) || (rx_state==0 && avail>0 && skip_bytes==0) ) {
          if( avail>0 ) {


            try {
              int len = serial_port.readBytes(b, avail);

              if(len>256000) len = 256000;

              for(int i=0;i<len;i++) {

                if(skip_bytes>0 && rx_state==6) {
                    skip_bytes--;

                    try {
                      if(tdma_idx<256) tdma_bytes[tdma_idx++] = b[i];

                      if(tdma_idx==256) {
                        tdma_idx=0;
                        rx_state=0;
                        //fos_tdma.write(tdma_bytes,0,256);
                        //fos_tdma.flush();
                      }
                    } catch(Exception e) {
                    }
                }
                else if(skip_bytes>0 && rx_state==5) {
                  skip_bytes--;

                  if(const_idx<320) constellation_bytes[const_idx++] = b[i];

                    //System.out.println("constellation "+skip_bytes);
                  //constellation data
                  if(skip_bytes==0) {
                    rx_state=0;
                    const_idx=0;
                    //System.out.println("read constellation");

                    //int j=0;
                    //for(int k=0;k<14;k++) {
                     // System.out.println( String.format("%d,%d",constellation_bytes[j], constellation_bytes[j+1]) ); 
                      //j+=2;
                    //}
                    //plot here
                    cpanel.addData( constellation_bytes, do_synced );
                    //if(do_mini_const) ConstPlotPanel.static_paint(tiny_const.getGraphics(),-15,15,26);
                  }
                }
                else if(skip_bytes>0 && rx_state==4) {
                  skip_bytes--;

                  if(pcm_idx<320) pcm_bytes[pcm_idx++] = b[i];

                  if( !enable_mp3.isSelected() && skip_bytes==0 ) {
                    do_meta();
                    //System.out.println("read voice");
                    try {
                      start_time = new java.util.Date().getTime();
                          tg_indicator.setBackground(java.awt.Color.yellow);
                          tg_indicator.setForeground(java.awt.Color.yellow);
                          tg_indicator.setEnabled(true);
                      if(aud!=null ) {
                        //if(iztimer!=null) iztimer.cancel();
                        if(aud!=null) aud.playBuf(pcm_bytes);
                        cpanel.addAudio(pcm_bytes);
                        do_audio_tick=0;
                        audio_tick_start = new java.util.Date().getTime();
                        //iztimer = new java.util.Timer();
                        //iztimer.schedule( new insertZeroTask(), 22, 22);
                      }
                    } catch(Exception e) {
                      //e.printStackTrace();
                    }
                    rx_state=0;
                    pcm_idx=0;
                  }
                  else if(skip_bytes==0 && record_to_mp3.isSelected() && enable_mp3.isSelected()) {
                    //System.out.println("read voice");

                    try {
                      start_time = new java.util.Date().getTime();
                          tg_indicator.setBackground(java.awt.Color.yellow);
                          tg_indicator.setForeground(java.awt.Color.yellow);
                          tg_indicator.setEnabled(true);
                      if(aud!=null ) {
                        //if(iztimer!=null) iztimer.cancel();
                        if(aud!=null) aud.playBuf(pcm_bytes);
                        cpanel.addAudio(pcm_bytes);
                        do_audio_tick=0;
                        audio_tick_start = new java.util.Date().getTime();
                        //iztimer = new java.util.Timer();
                        //iztimer.schedule( new insertZeroTask(), 22, 22);
                      }
                    } catch(Exception e) {
                      //e.printStackTrace();
                    }


                    //addTextConsole("\r\npcm_idx: "+pcm_idx);
                    byte[] mp3_bytes = encode_mp3(pcm_bytes);

                    if(mp3_bytes!=null) {

                      String date = formatter_date.format(new java.util.Date() );
                      if( current_date==null || !current_date.equals(date) || mp3_separate_files.isSelected() && sys_mac_id!=null && sys_mac_id.length()>0) {
                        current_date=new String(date);  //date changed

                        boolean is_ms=mp3_separate_files.isSelected();

                        try {

                          if(!is_ms) {
                            if(fos_mp3!=null) fos_mp3.close();
                            if(fos_meta!=null) fos_meta.close();
                            if(encoder!=null) encoder.close();

                            fos_mp3 = null;
                            fos_meta = null;
                            skip_header=1;
                            encoder=null;
                          }
                        } catch(Exception e) {
                          //e.printStackTrace();
                        }

                        try {



                          String mp3_tg = "";
                          if(is_ms && mp3_separate_files.isSelected()) {
                            current_talkgroup = current_talkgroup.replace(',','_');


                            if(reset_session==1 || mp3_time.length()==0) {
                              mp3_time = time_format.format(new java.util.Date() );

                            }

                            mp3_tg = "_"+mp3_time+"_TG_"+current_talkgroup;

                            if(reset_session==1) {
                              if(fos_mp3!=null) fos_mp3.close();
                              if(encoder!=null) encoder.close();
                              encoder=null;
                              skip_header=1;
                            }

                            reset_session=0;
                          }


                          open_audio_output_files();

                        } catch(Exception e) {
                          //e.printStackTrace();
                        }

                      }


                      do_meta();

                      if(skip_header==1) {
                        skip_header=0;
                      }
                      else {
                        fos_mp3.write(mp3_bytes,0,mp3_bytes.length);  //write Int num records
                        fos_mp3.flush();
                      }

                      boolean is_ms=mp3_separate_files.isSelected();
                      if(is_ms) {
                        fos_mp3.write(mp3_bytes,0,mp3_bytes.length);  //write Int num records
                        fos_mp3.flush();
                      }

                    }
                    pcm_idx=0;
                    rx_state=0;
                  }
                }
                else if(skip_bytes>0) {
                  skip_bytes--; //handle unknown state
                  if(skip_bytes==0) rx_state=0;
                }

                if(skip_bytes==0) {

                  //b2 5f 9c 71
                  if(rx_state==0 && b[i]==(byte) 0xb2) {
                    rx_state=1;
                  }
                  else if( (rx_state==1 && b[i]==(byte) 0x5f) || (rx_state==1 && b[i]==(byte) 0x5b) || (rx_state==1 && b[i]==(byte) 0x59) || (rx_state==1 && b[i]==(byte) 0x98) || (rx_state==1 && b[i]==(byte) 0x51) ) {
                    rx_state=2;
                  }
                  else if( (rx_state==2 && b[i]==(byte) 0x9c) || (rx_state==2 && b[i]==(byte) 0x12) || (rx_state==2 && b[i]==(byte) 0xef) || (rx_state==2 && b[i]==(byte) 0x72) || (rx_state==2 && b[i]==(byte) 0x70) ) {
                    rx_state=3;
                  }
                  else if( (rx_state==3 && b[i]==(byte) 0x71) || (rx_state==3 && b[i]==(byte) 0xe4) || (rx_state==3 && b[i]==(byte) 0x72) || (rx_state==3 && b[i]==(byte) 0x31) || (rx_state==3 && b[i]==(byte) 0x15) ) {
                    //addTextConsole("\r\nfound voice header");

                    if(b[i]==(byte) 0x71) {
                      skip_bytes=320+1;
                      rx_state=4;
                      //System.out.println("do voice");
                    }
                    else if(b[i]==(byte) 0xe4) {
                      skip_bytes=320+1;
                      rx_state=5;
                      //System.out.println("do const");
                    }
                    else if(b[i]==(byte) 0x72) {
                      skip_bytes=256+1;
                      rx_state=6;
                      //System.out.println("do tdma");
                    }
                    else if(b[i]==(byte) 0x15) {
                      skip_bytes=140+1;
                      rx_state=0;
                      //System.out.println("do sysinfo");
                    }
                    else if(b[i]==(byte) 0x31) {
                      //audio flush
                      //System.out.println("\r\naudio flush");

                      if(aud!=null) {
                        aud.playStop();
                      }

                      rx_state=0;
                      skip_bytes=0;
                    }
                    else {
                      rx_state=0;
                      skip_bytes=0;
                    }
                  }
                  else {
                    //rx_state=0;  dont do this

                    if(rx_state==0 && skip_bytes==0 ) {
                      int isprint=1;

                      if( b[i]>=(byte) 0x00 && b[i]<=(byte)0x1f && b[i]!=(byte)0x0a && b[i]!=(byte)0x0d) isprint=0;
                      if( b[i]>(byte)'z') isprint=0;

                      if(isprint==1) str_b[str_idx++] = b[i];
                    }
                  }
                }
              }

              if(str_idx>0) {
                addTextConsole( new String(str_b,0,str_idx) );
                str_idx=0;
              }


            } catch(Exception e) {
              e.printStackTrace();
            }
          }

        }
        if( do_disconnect==1) {
          do_disconnect=0;
          is_connected=0;
          if(serial_port!=null) serial_port.closePort();
          serial_port=null;
        }

        tick_mod++;

        long ctime = new java.util.Date().getTime();

        if( tg_follow_blink>0 && ctime-tg_blink_time>2000) {

          tg_blink_time=ctime;

          tg_indicator.setEnabled(true);
          tg_blink^=0x01;
          if(tg_blink==1) {
            tg_indicator.setBackground(java.awt.Color.yellow);
            tg_indicator.setForeground(java.awt.Color.yellow);
          }
          else {
            tg_indicator.setBackground(java.awt.Color.black);
            tg_indicator.setForeground(java.awt.Color.black);
          }
        }
        else if(tg_follow_blink==0) {
          //tg_blink=0x01;
          //tg_indicator.setBackground(java.awt.Color.black);
          //tg_indicator.setForeground(java.awt.Color.black);
        }

        if(bluetooth_error==0 && bluetooth_streaming==1 && tick_mod%500==0) {
          bt_indicator.setEnabled(true);

          bluetooth_blink^=1;
          if(bluetooth_blink==1) {
            bt_indicator.setBackground(java.awt.Color.blue);
            bt_indicator.setForeground(java.awt.Color.blue);
          }
          else {
            bt_indicator.setBackground(java.awt.Color.black);
            bt_indicator.setForeground(java.awt.Color.black);
          }
        }
        else if(bluetooth_error==0 && bluetooth_streaming==0) {
          bt_indicator.setEnabled(true);
          bt_indicator.setBackground(java.awt.Color.black);
          bt_indicator.setForeground(java.awt.Color.black);
        }
        else if(bluetooth_error==1 && bluetooth_streaming==1) { //module is turned off
          bt_indicator.setEnabled(true);
          bt_indicator.setBackground(java.awt.Color.black);
          bt_indicator.setForeground(java.awt.Color.black);
        }


      } catch(Exception e) {
        e.printStackTrace(System.out);
      }

      long time = new java.util.Date().getTime();
      Boolean isWindows = System.getProperty("os.name").startsWith("Windows");
      int stop_time=50;
      //if(isWindows || is_mac_osx==1) stop_time=50;
      if(isWindows ) stop_time=50;
        else stop_time=500; 
      if(time-start_time>stop_time) {
        //if(aud!=null) aud.playStop();
        if(tg_follow_blink==0) {
          tg_indicator.setBackground(java.awt.Color.black);
          tg_indicator.setForeground(java.awt.Color.black);
        }
      }


    }
}

int do_audio_tick=0;
int do_zipsearch=0;
int do_zipsearch2=0;
Boolean isWindows=true;
java.util.Timer utimer;
java.util.Timer iztimer;
int do_update_firmware;
int do_update_firmware2;
int do_disconnect;
int do_update_talkgroups;
int do_update_roaming=0;
int do_read_talkgroups;
static BTFrame parent;
SerialPort serial_port=null;
int firmware_checked=0;
int is_connected=0;
int do_connect=0;
String serial_port_name="";
char keydata[];
int keyindex;
int command_input=0;
int status_timeout=1;
rssimeter rssim1;
rssimeter rssim2;
String console_line;
int sig_meter_timeout=1;
javax.swing.JLabel l1;
javax.swing.JLabel l2;
javax.swing.JLabel l3;
int do_restore_tg=0;
int do_restore_tg_csv=0;
int did_tg_backup=1;  //don't do backup on startup
int bluetooth_streaming=0;
int bluetooth_error=0;
int bluetooth_blink;
int tg_follow_blink=0;
int tg_blink=0;
int wd_count=0;
int tick_mod;
int rx_state=0;
int skip_bytes=0;
byte[] pcm_bytes;
byte[] constellation_bytes;
byte[] tdma_bytes;
int pcm_idx=0;
int const_idx=0;
int tdma_idx=0;
LameEncoder encoder=null;
byte[] mp3_buffer;
String current_date=null;
String home_dir=null;
FileOutputStream fos_mp3;
FileOutputStream fos_meta;
FileOutputStream fos_conlog;
FileOutputStream fos_tdma;
File mp3_file=null;
File meta_file=null;
File conlog_file=null;
File tdma_file=null;
java.text.SimpleDateFormat formatter_date;
java.text.SimpleDateFormat time_format;
int current_sys_id = 0;
int current_wacn_id = 0; 
int do_toggle_record=1;
int did_metadata=0;
int meta_count=0;
int skip_header=1;
private Dimension parentSize;
int do_read_config=1;
int do_write_config=0;
audio aud = null; 
long start_time;
String freq_str="";
SYSConfig sys_config;
Roaming roaming_tests;
Preferences prefs;
int do_agc_update=0;
int system_crc=0;
int do_talkgroup_backup=0;
TGConfig tg_config=null;
zipsearch zs=null;
int do_test_freqs=0;
int do_roam_freq=0;
int do_read_roaming=0;
int do_roaming_backup=0;
int tg_update_pending=0;
int do_erase_roaming=0;
int do_backup_roaming=0;
int do_restore_roaming=0;
int do_append_roaming=0;
int do_console_output=0;
int do_write_roaming_flash_only=0;
int did_read_talkgroups=0;
int is_mac_osx=0;
int is_linux=0;
int is_windows=0;
int is_dmr_mode=0;
int tsbk_ps_i=0;
int bluetooth_streaming_timer=0;
int p25_status_timeout=1;
Hashtable lat_lon_hash1;
Hashtable lat_lon_hash2;
Hashtable supergroup_hash;
Hashtable no_loc_freqs;
Boolean do_tdma_messages=false;
ConstPlotPanel cpanel;
Boolean do_mini_const=false;
boolean do_synced;
double current_freq=0.0;
long audio_tick_start=0;
int command_input_timeout=0;
long tg_blink_time=0;
String current_talkgroup="";
int reset_session=0;
java.text.SimpleDateFormat mp3_time_format = new java.text.SimpleDateFormat( "HH:mm:ss" );
String mp3_time ="";
int tg_pri=0;
int do_select_home_dir=0;
String sys_mac_id="";
long wdog_time=0;
int did_freq_tests=0;
int sys_info_count=0;
int src_uid=0;
int is_enc=0;
Alias alias;
String current_alias;
int do_alias_import=0;
int do_alias_export=0;
byte[] b;
byte[] str_b;
int str_idx=0;
int avail=0;
int is_phase1=0;
int is_phase2=0;
long status_time;

  ///////////////////////////////////////////////////////////////////
    public BTFrame(String[] args) {
      initComponents();

      b = new byte[256000];
      str_b = new byte[256000];

      supergroup_hash = new Hashtable();

      agc_kp.setVisible(false);
      agc_kp_lb.setVisible(false);

      //jPanel25.remove(const_panel);
      cpanel = new ConstPlotPanel(this);
      const_panel.add(cpanel, java.awt.BorderLayout.CENTER);
      //jPanel25.add(cpanel);
      //

      signalinsightpanel.remove(jPanel8);

      if(zs==null) zs = new zipsearch(this);

      if(args.length>0) {
        for(int i=0;i<args.length;i++) {
          if(args[i].equals("-console")) {
            do_console_output=1;
            System.out.println("enable console output");
          }
          if(args[i].equals("-tdma")) {
            do_tdma_messages=true;
            System.out.println("enable tdma / phase 2 messages");
          }
          if(args[i].equals("-mac")) {
            is_mac_osx=1;
            System.out.println("\r\nenabling MAC_OSX option");
          }
          if(args[i].equals("-miniconst")) {
            do_mini_const=true;
          }
        }
      }

      roaming_tests = new Roaming();

      write_cc.setVisible(false);
      inc_p25.setVisible(false);
      macid.setVisible(false);

      //agc_gain.setVisible(false); //hide agc slider related
      //jLabel3.setVisible(false);
      //agc_level_lb.setVisible(false);

      inc_dmr.setVisible(false);

      fw_ver.setVisible(false);
      fw_installed.setVisible(false);

      pcm_bytes = new byte[320];
      constellation_bytes = new byte[320];
      tdma_bytes = new byte[256];

      write_config.setEnabled(false);
      disconnect.setEnabled(false);

      isWindows=false;

      if( System.getProperty("os.name").startsWith("Windows") ) {
        isWindows=true;
          System.out.println("\r\nenabling Windows option");
          os_string.setText("OS: Windows");
        is_windows=1;
      }

      //Mac OSX
      if( System.getProperty("os.name").toLowerCase().contains("mac os x") ) {
          is_mac_osx=1;
          System.out.println("\r\nenabling MAC_OSX option");
          os_string.setText("OS: Mac OSX");
      }

      if( System.getProperty("os.name").toLowerCase().contains("linux") ) {
          is_linux=1;
          System.out.println("\r\nenabling Linux option");
          os_string.setText("OS: Linux");
      }


      read_config.setVisible(false);  //read config button

      jLabel32.setVisible(false);
      rfmaxgain.setVisible(false);

      check_firmware.setEnabled(false);
      check_firmware.setVisible(false);

      record_to_mp3.setEnabled(false);
      record_to_mp3.setVisible(false);

      macid.setText("");
      wacn.setText("");
      sysid.setText("");
      nac.setText("");
      freq.setText("");
      rfid.setText("");
      siteid.setText("");


      record_to_mp3.setSelected(true);


      formatter_date = new java.text.SimpleDateFormat( "yyyy-MM-dd" );
      time_format = new java.text.SimpleDateFormat( "yyyy-MM-dd-HH:mm:ss" );



      fw_ver.setText("Latest Avail: FW Date: 202105171846");
      release_date.setText("Release: 2021-05-17 19:14");
      fw_installed.setText("   Installed FW: ");

      setProgress(-1);

      jTable1.setShowHorizontalLines(true);
      jTable1.setShowVerticalLines(true);

      freq_table.setShowHorizontalLines(true);
      freq_table.setShowVerticalLines(true);

      alias_table.setShowHorizontalLines(true);
      alias_table.setShowVerticalLines(true);

      keydata = new char[4096];
      keyindex=0;
      jTextArea1.getCaret().setVisible(true);
      jTextArea1.getCaret().setBlinkRate(250);
      setIconImage(new javax.swing.ImageIcon(getClass().getResource("/btconfig/images/iconsmall.gif")).getImage()); // NOI18N
      setTitle("BlueTail Technologies P25RX Configuration Software (c) 2020-21");

      rssim1 = new rssimeter();
      rssim2 = new rssimeter();
      //rssim1.setValue(-90,true);
      //rssim2.setValue(-20,false);

      l1 = new javax.swing.JLabel();
      l2 = new javax.swing.JLabel();
      l3 = new javax.swing.JLabel();
      l1.setText("RF Sig Level");
      l2.setText("Sig Quality");
      l1.setForeground(java.awt.Color.white);
      l2.setForeground(java.awt.Color.white);
      l3.setForeground(java.awt.Color.white);
      l3.setFont(new java.awt.Font("Monospaced", 0, 18)); // NOI18N

      //l3.setText("");
      l3.setText("NO SIG");
      desc_panel.add(l3);


      level_panel.add( l1 ); 
      level_panel.add(rssim1); 

      //level_panel.add( l2 ); 
      //level_panel.add(rssim2); 

      l1.setVisible(true);
      l2.setVisible(true);
      l3.setVisible(true);
      rssim1.setVisible(true);
      rssim2.setVisible(true);
      rssim1.setValue(-130,false);
      rssim2.setValue(-130,false);
      p25_status_timeout=1;
      l3.setText("");


      /*
      alias_table.setAutoCreateRowSorter(true);
      alias_table.getRowSorter().addRowSorterListener(new RowSorterListener() {
       public void sorterChanged(RowSorterEvent rse) {
        if (rse.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
            java.util.List<? extends RowSorter.SortKey> sortKeys = alias_table.getRowSorter().getSortKeys();
            if(sortKeys.get(0).getSortOrder() == SortOrder.ASCENDING) {
              alias_table.getRowSorter().toggleSortOrder(0);
            }
          }
        }
      });
      */


      /*
      freq_table.setAutoCreateRowSorter(true);

      freq_table.getRowSorter().addRowSorterListener(new RowSorterListener() {
       public void sorterChanged(RowSorterEvent rse) {
        if (rse.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
          java.util.List<? extends RowSorter.SortKey> sortKeys = freq_table.getRowSorter().getSortKeys();
          if(sortKeys.get(0).getSortOrder() == SortOrder.ASCENDING) {
            freq_table.getRowSorter().toggleSortOrder(0);
          }
        }
       }
      });
      */

      DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
      rightRenderer.setHorizontalAlignment(JLabel.LEFT);
      jTable1.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
      jTable1.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
      jTable1.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
      jTable1.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
      jTable1.setAutoCreateRowSorter(true);

      jTable1.getRowSorter().addRowSorterListener(new RowSorterListener() {
       public void sorterChanged(RowSorterEvent rse) {
        if (rse.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
          if(tg_config!=null) {
            //System.out.println(rse);
            //tg_config.postSort(parent);
            java.util.List<? extends RowSorter.SortKey> sortKeys = jTable1.getRowSorter().getSortKeys();
            if(sortKeys.get(0).getSortOrder() == SortOrder.ASCENDING) {
              //jTable1.getRowSorter().setSortKeys(null);
              jTable1.getRowSorter().toggleSortOrder(0);
            }
          }
        }
       }
      });

      level_panel.remove(sq_lb);
      level_panel.remove(tg_lb);
      level_panel.remove(bt_lb);

      level_panel.remove(bt_indicator);
      level_panel.remove(bt_indicator);
      level_panel.remove(tg_indicator);
      level_panel.remove(sq_indicator);

      sq_lb.setText("   SIG");
      level_panel.add(sq_lb);
      level_panel.add(sq_indicator);
      level_panel.add(tg_lb);
      level_panel.add(tg_indicator);
      level_panel.add(bt_lb);
      level_panel.add(bt_indicator);

      sq_indicator.setBackground(java.awt.Color.black);
      sq_indicator.setForeground(java.awt.Color.black);
      bt_indicator.setBackground(java.awt.Color.black);
      bt_indicator.setForeground(java.awt.Color.black);
      tg_indicator.setBackground(java.awt.Color.black);
      tg_indicator.setForeground(java.awt.Color.black);


      //parentSize = Toolkit.getDefaultToolkit().getScreenSize();
      //setSize(new Dimension((int) (parentSize.width * 0.75), (int) (parentSize.height * 0.8)));

        //jTabbedPane1.remove( buttong_config);

        btreset1.setVisible(false);
        btreset2.setVisible(false);
        bluetooth_reset.setVisible(false);

      InputMap inputMap = jTable1.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
      ActionMap actionMap = jTable1.getActionMap();
      String deleteAction = "delete";
      inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0),
                deleteAction);
      actionMap.put(deleteAction, new AbstractAction()
        {
          public void actionPerformed(java.awt.event.ActionEvent deleteEvent)
          {
            //if (jTable1.getSelectedRow() != -1) {
             // tableModel.removeRow(table.convertRowIndexToModel(table.getSelectedRow()));
            //}
            //if(evt.getKeyChar()==java.awt.event.KeyEvent.VK_DELETE) {
              //System.out.println(evt);
              int[] rows = jTable1.getSelectedRows();
              if(rows.length>0) {
                for(int i=0;i<rows.length;i++) {
                  jTable1.getModel().setValueAt(null,jTable1.convertRowIndexToModel(rows[i]),0);
                  jTable1.getModel().setValueAt(null,jTable1.convertRowIndexToModel(rows[i]),1);
                  jTable1.getModel().setValueAt(null,jTable1.convertRowIndexToModel(rows[i]),2);
                  jTable1.getModel().setValueAt(null,jTable1.convertRowIndexToModel(rows[i]),3);
                  jTable1.getModel().setValueAt(null,jTable1.convertRowIndexToModel(rows[i]),4);
                  jTable1.getModel().setValueAt(null,jTable1.convertRowIndexToModel(rows[i]),5);
                  jTable1.getModel().setValueAt(null,jTable1.convertRowIndexToModel(rows[i]),6);
                  //System.out.println("row "+i);
                } 
              }
            //}

            jTable1.getRowSorter().toggleSortOrder(0);
            jTable1.getRowSorter().toggleSortOrder(0);

            do_update_talkgroups=1;
            do_read_talkgroups=1;
            jTable1.setRowSelectionInterval(0,0);
          }
        });


      InputMap inputMap2 = freq_table.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
      ActionMap actionMap2 = freq_table.getActionMap();
      String deleteAction2 = "delete";
      inputMap2.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0),
                deleteAction2);
      actionMap2.put(deleteAction2, new AbstractAction()
        {
          public void actionPerformed(java.awt.event.ActionEvent deleteEvent)
          {
              int[] rows = freq_table.getSelectedRows();
              int flash_recs=0;

              if(did_freq_tests==1) return;

              for(int i=0;i<250;i++) {
                String str1 = (String) freq_table.getModel().getValueAt(i, 6);
                if(str1!=null && str1.equals("X") ) {
                  flash_recs++;
                }
              }

              if(rows.length>0) {
                for(int i=0;i<rows.length;i++) {
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),0);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),1);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),2);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),3);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),4);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),5);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),6);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),7);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),8);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),9);
                  freq_table.getModel().setValueAt(null,freq_table.convertRowIndexToModel(rows[i]),10);
                } 
              }
              if(flash_recs>0) do_write_roaming_flash_only=1;
          }
        });

      do_connect();

      //do this last
      utimer = new java.util.Timer();
      utimer.schedule( new updateTask(), 100, 1);
      setSize(1054,750);
    }

  //////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////
  public SerialPort find_serial_port()
  {

    int n=0;

    SerialPort[] ports = SerialPort.getCommPorts();

    for(int i=0; i<ports.length; i++) {
        String isopen="";
        if( ports[i].isOpen() ) isopen=" open";
          else isopen="  closed";


        System.out.println("\r\n["+i+"]  found device on : "+
          ports[i].getSystemPortName()+"  "+
          ports[i].getDescriptivePortName()+"  "+
          ports[i].getPortDescription()+"  "+
          ports[i].toString()+isopen);
    }


    for(int i=0; i<ports.length; i++) {
      //setStatus("\r\nport: "+ports[i]+" on " + ports[i].getSystemPortName());

      if( ports[i].toString().startsWith("BlueTail-P1") ) { //we are looking for this string in the serial port description
      //if( i>1 && ports[i].toString().startsWith("BlueTail-P1") ) { //we are looking for this string in the serial port description
        //setStatus("FOUND device");
        //System.out.println("\r\nfound device on : "+ports[i].getSystemPortName()+"  "+ports[i].getDescriptivePortName()+"  "+ports[i].getPortDescription());

        /*
        if( ports[i].isOpen() ) {

          Boolean isWindows = System.getProperty("os.name").startsWith("Windows");
          //setStatus("Device is currently open by another application.  Please close the application.");
          if(isWindows || is_mac_osx==1) {
            ports[i].closePort();
            return null;
          }
          else {
            ports[i].closePort();
            return ports[i];
          }
        }
        */
        if(!ports[i].isOpen()) {
          if( ports[i].openPort(200) ) {
            ports[i].closePort();
            System.out.println("using ["+i+"]  "+ports[i]);
            ser_dev.setText("PORT: "+ports[i].getSystemPortName());
            return ports[i];
          }
          else {
              System.out.println("attempting to close locked port "+ports[i]);
              ports[i].closePort();
              if( ports[i].openPort(200) ) {
                System.out.println("using ["+i+"]  "+ports[i]);
                return ports[i];
              }
          }
        }
      }
    }

    return null;
  }

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    public byte[] encode_mp3(byte[] pcm) {

      int len=0;
      byte[] b=null;


      try {

        if(encoder==null) {
          //AudioFormat inputFormat = new AudioFormat( new AudioFormat.Encoding("PCM_SIGNED"), 8000.0f, 16, 1, 160, 50, false);
          AudioFormat inputFormat = new AudioFormat( 8000.0f, 16, 1, true, false);  //booleans are signed, big-endian
          //encoder = new LameEncoder(inputFormat, 256, MPEGMode.MONO, Lame.QUALITY_LOWEST, false);
          encoder = new LameEncoder(inputFormat, 32, MPEGMode.MONO, Lame.QUALITY_LOWEST, true);
          //ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
          mp3_buffer = new byte[encoder.getPCMBufferSize()];

          //int bytesToTransfer = Math.min(buffer.length, pcm.length);
          //int bytesWritten;
          //int currentPcmPosition = 0;
          //while (0 < (bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer))) {
           // currentPcmPosition += bytesToTransfer;
            //bytesToTransfer = Math.min(buffer.length, pcm.length - currentPcmPosition);

            //mp3.write(buffer, 0, bytesWritten);
          //}

          //encoder.close();
          //return mp3.toByteArray();
        }
        else {
          len = encoder.encodeBuffer(pcm, 0, 320, mp3_buffer);
          //addTextConsole("\r\nencoder: "+len);
        }

        if(len==0) return null;

        b = new byte[len];

        if(len>0) {
          for(int i=0;i<len;i++) {
            b[i] = mp3_buffer[i];
          }
        }
      } catch(Exception e) {
        //e.printStackTrace();
      }

      return b;
    }

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    void addTableObject(Object obj, int row, int col) {
      jTable1.getModel().setValueAt(obj,row,col);
    }
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    Object getTableObject(int row, int col) {
      return jTable1.getModel().getValueAt(row,col);
    }
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    void addAliasObject(Object obj, int row, int col) {
      alias_table.getModel().setValueAt(obj,row,col);
    }
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    Object getAliasObject(int row, int col) {
      return alias_table.getModel().getValueAt(row,col);
    }


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    void addTextConsole(String str) {

      if(command_input==1) return;

     try {

      if(str!=null && do_console_output==1) System.out.println(str.trim());

      if( enable_conlog.isSelected() ) {
        try {
          String date = time_format.format(new java.util.Date() );
          str = str.replaceAll("DATA_SYNC",date+" "+"DATA_SYNC");
          str = str.replaceAll("found DMR_BS_VOICE_SYNC",date+" "+"found DMR_BS_VOICE_SYNC");
          str = str.replaceAll("$TDMA",date+" "+"$TDMA");

          fos_conlog.write(str.getBytes(),0,str.length());  //write Int num records
          fos_conlog.flush();
        } catch(Exception e) {
        }
      }

      String talkgroup="";
      String freqval="";
      String tsbk_ps="";

      if(console_line==null) console_line = new String("");
      console_line = console_line.concat(str);

      if( console_line.contains("$SYS_INFO") && !console_line.contains("nac 0x") ) {
        if(sys_info_count++<1) return;
        sys_info_count=0; 
        src_uid=0;
        is_enc=0;
      }

      if( console_line.contains("$P25_GRP_UP grp1:") && console_line.contains("ch2:") ) {
        StringTokenizer st = new StringTokenizer(console_line," ,\r\n");
        String st1 = ""; 
        String active_tg="Adjacent Active Talk Groups: ";

        while(st.hasMoreTokens()) {
          st1 = st.nextToken();
          if(st1!=null && st1.equals("grp1:")) {
            String grp1 = st.nextToken().trim();
            active_tg = active_tg.concat(grp1+", ");
          }
          if(st1!=null && st1.equals("grp2:")) {
            String grp2 = st.nextToken().trim();
            active_tg = active_tg.concat(grp2);
            parent.setStatus(active_tg);
          }
        }
      }

      if( console_line.contains("$P25_GRP_EXP_UP grp") && console_line.contains("rx:") ) {
        StringTokenizer st = new StringTokenizer(console_line," \r\n");
        String st1 = ""; 
        String active_tg="Adjacent Active Talk Groups: ";

        while(st.hasMoreTokens()) {
          st1 = st.nextToken();
          if(st1!=null && st1.equals("grp")) {
            String grp1 = st.nextToken().trim();
            active_tg = active_tg.concat(grp1);
          }
        }
      }

      if( console_line.contains("$SYS_INFO") && console_line.contains("nac 0x") ) {
        src_uid=0;
        is_enc=0;
        StringTokenizer st = new StringTokenizer(console_line," \r\n");
        String st1 = ""; 
        while(st.hasMoreTokens()) {
          st1 = st.nextToken();
          if(st1!=null && st1.equals("wacn")) {
            String w = st.nextToken().trim();
            current_wacn_id = Integer.parseInt(w.substring(2,w.length()),16);
          }
          if(st1!=null && st1.equals("sys_id")) {
            String s = st.nextToken().trim();
            current_sys_id = Integer.parseInt(s.substring(2,s.length()),16);
          }
        }
      }

      if( (console_line.contains("P25_P1: SRC_RID: ") || console_line.contains("P25_PII: SRC_RID: ")) && console_line.contains("$") && console_line.contains("GRP") ) {
        StringTokenizer st = new StringTokenizer(console_line," \r\n");
        String st1 = ""; 
        while(st.hasMoreTokens()) {
          st1 = st.nextToken();
          if(st1!=null && st1.equals("SRC_RID:")) {
            if( st.hasMoreTokens() ) {
              try {
                int src_uid_d = Integer.parseInt(st.nextToken());
                if(did_metadata==1 && src_uid_d!=0 && src_uid_d != src_uid) {
                  did_metadata=0;
                }
                src_uid = src_uid_d;
                try {
                  String src_rid_str = new Integer(src_uid).toString();
                  if(alias!=null && src_uid!=0) alias.addRID(this, src_rid_str);
                } catch(Exception e) {
                  e.printStackTrace();
                }

                    String src_uid_str="";
                    try {
                      if(src_uid!=0) src_uid_str = new Integer(src_uid).toString();
                    } catch(Exception e) {
                      e.printStackTrace();
                    }

                    if(src_uid_str!=null && src_uid_str.length()>0 && current_alias!=null) {
                      status.setText(system_alias.getText()+", RID: "+src_uid_str+", "+current_alias);
                    }
                    else if(src_uid_str!=null && src_uid_str.length()>0 ) {
                      status.setText(system_alias.getText()+", RID: "+src_uid_str);
                    }
                    else {
                      status.setText(system_alias.getText());
                    }

                    do_meta();

              } catch(Exception e) {
                src_uid = 0;
              }
            }
          }
        }
      }

      int do_add=1;

      if(console_line.contains("\r\n") && console_line.contains("Return To Control") ) {
        start_time = new java.util.Date().getTime();
      }

      try {
        if(console_line.contains("dmr mode enabled")) {
          freq.setText("");
        }
      } catch(Exception e) {
      }

      if( console_line.contains("DATA_SYNC") ) {
        sysid.setText("");
        wacn.setText("");
        nac.setText("");
        rfid.setText("");
        siteid.setText("");
      }

      if(console_line.contains("$TDMA")) {
        p25_status_timeout=6000;
        is_phase1=0;
        is_phase2=1;
      }

      if(console_line.contains("TG PRI")) {
        StringTokenizer st = new StringTokenizer(console_line,"\r\n");
        while(st.hasMoreTokens()) { 
          String l = st.nextToken();
          if(l.startsWith("TG PRI")) {
            tg_pri=1;
            setStatus(l);
          }
        }
      }

      if(console_line.contains("sig 1")) {
        sq_indicator.setForeground( java.awt.Color.green );
        sq_indicator.setBackground( java.awt.Color.green );
        p25_status_timeout=6000;
      }
      if(console_line.contains("sig 0")) {
        sq_indicator.setForeground( java.awt.Color.black );
        sq_indicator.setBackground( java.awt.Color.black );
      }

      if(console_line.contains("following talkgroup") ) {
        StringTokenizer st = new StringTokenizer(console_line," \r\n");
        if(st.countTokens()>=3) {
          st.nextToken();
          st.nextToken();
            try {
              tg_follow_blink = new Integer( st.nextToken() ).intValue();
            } catch(Exception e) {
            }
        }
        if(console_line.contains("un-following talkgroup")) {
          tg_follow_blink = 0; 
        }
      } 

      if(console_line.contains("\r\ngrant 0x02") && (console_line.contains("tgroup") && console_line.contains("TDMA")) ) {
        StringTokenizer st = new StringTokenizer(console_line," \r\n");
        String st1 = ""; 
        while(st.hasMoreTokens()) {
          st1 = st.nextToken();
          if(st1!=null && st1.contains("tgroup") && st.hasMoreTokens()) {
            try {
              int tg = new Integer( st.nextToken() ).intValue();
              String tg_str = new Integer(tg).toString();

              break;
            } catch(Exception e) {
              break;
            }
          }
        }

        st = new StringTokenizer(console_line," \r\n");
        st1 = ""; 
        while(st.hasMoreTokens()) {
          st1 = st.nextToken();
          if(st1!=null && st1.contains("follow") && st.hasMoreTokens()) {
            try {
              tg_follow_blink = new Integer( st.nextToken() ).intValue();
              break;
            } catch(Exception e) {
              break;
            }
          }
        }
      }

      if(console_line.contains("\r\n") && (console_line.contains("tgroup") && console_line.contains("rf_channel")) ) {
        StringTokenizer st = new StringTokenizer(console_line," \r\n");
        String st1 = ""; 
        while(st.hasMoreTokens()) {
          st1 = st.nextToken();
          if(st1!=null && st1.contains("follow") && st.hasMoreTokens()) {
            try {
              tg_follow_blink = new Integer( st.nextToken() ).intValue();
              break;
            } catch(Exception e) {
              break;
            }
          }
        }
      }
      if(console_line.contains("\r\n") && (console_line.contains("supergroup") && console_line.contains("rf_channel")) ) {
        StringTokenizer st = new StringTokenizer(console_line," \r\n");
        String st1 = ""; 
        while(st.hasMoreTokens()) {
          st1 = st.nextToken();
          if(st1!=null && st1.contains("supergroup") && st.hasMoreTokens()) {
            try {
              int supergroup = new Integer( st.nextToken() ).intValue();
              String sg = new Integer(supergroup).toString();
              supergroup_hash.put( sg, sg ); 
              break;
            } catch(Exception e) {
              break;
            }
          }
        }
      }


      if( (console_line.contains("DMR")) || (console_line.contains("rssi:") || console_line.contains("\r\n  ->(VOICE)")) && console_line.contains("$") ) {

        try {

            if(console_line.contains("->(VOICE)")) {
              is_phase1=1;
              is_phase2=0;
            }

            if(console_line.contains("VOICE") || console_line.contains("rssi:") ) {
              p25_status_timeout=6000;
            }

            if(console_line.contains("ue 0")) {
              bluetooth_error=0;
            }
            if(console_line.contains("ue 1")) {
              //if(bluetooth_error==0 && bluetooth_streaming==1) setStatus("Bluetooth Comm Error Detected.");
              bluetooth_error=1;
            }


            if(console_line.contains("sa 0")) {
              if(bluetooth_streaming==1 && bluetooth_error==0) setStatus("Bluetooth Audio Streaming Stopped");
              bluetooth_streaming=0;
            }
            if(console_line.contains("sa 1")) {
              if(bluetooth_streaming==0 && bluetooth_error==0) setStatus("Bluetooth Audio Streaming Started");
              bluetooth_streaming=1;
            }

          if(console_line.contains("sa 1")) {
            bluetooth_streaming_timer=60000;
          }


          //do_add=0;
          StringTokenizer st = new StringTokenizer(console_line," \r\n");
          while(st.hasMoreTokens()) {
            String st1 = st.nextToken();

            if(st1.equals("TGroup:") && st.hasMoreTokens()) {
              String tg_id = st.nextToken();


              int has_comma=0;

              if(tg_id!=null) {
                current_talkgroup = tg_id;

                talkgroup = ", TG "+tg_id;

                if(tg_id.contains(",")) has_comma=1;


                talkgroup = talkgroup.substring(0,talkgroup.length()-1)+" ";

                try {
                  //System.out.println("checking tgroup superg: "+tg_id.trim().substring(0,tg_id.length()-1));
                  if( supergroup_hash.get( tg_id.trim().substring(0,tg_id.length()-1)  ) != null ) {
                    talkgroup = talkgroup+"(P) ";
                  } 
                } catch(Exception e) {
                }
              }


              if(is_dmr_mode==1) {
                try {
                  current_sys_id = Integer.valueOf( dmr_sys_id.getText() ); 
                } catch(Exception e) {
                  current_sys_id=1;
                }
              }

              if( tg_id!=null && tg_id.length()>0 && tg_config!=null && current_sys_id!=0) {
                String city="unknown";
                try {
                  double d = 0.0; 
                  String ff="";
                  try {
                    d = new Double(freq.getText().substring(6,15));
                    ff = String.format("%3.8f", d);
                  } catch(Exception e) {
                  }
                  if(prefs!=null) city = prefs.get("city_state_"+ff, "unknown");
                  if(city==null) city="";


                } catch(Exception e) {
                }

                if(has_comma==1) tg_config.addUknownTG(parent, tg_id, new Integer(current_sys_id).toString(), city, new Integer(current_wacn_id).toString() ); 
              }
            }

            if(console_line.contains("ENCRYPTED talkgroup") && st1.equals("talkgroup") ) {
              String tg_id = st.nextToken();
              tg_config.disable_enc_tg(parent, tg_id, new Integer(current_sys_id).toString() );
              is_enc=1;
            }

            if(st1.equals("freq") && !console_line.contains("grant") ) {
              freq_str = st.nextToken();
              if(freq_str!=null) {
                try {
                  double fval = new Double(freq_str).doubleValue();
                  try {
                    current_freq = Double.valueOf(freq_str)*1e6;
                  } catch(Exception e) {
                  }

                  if(fval!=0) {
                    freq.setText("Freq: "+freq_str);
                  }
                  else {
                    freq.setText("");
                  }
                } catch(Exception e) {
                    freq.setText("");
                }
              }
            }

            /*
            if(st1.equals("mac")) {
              String macid_str = st.nextToken();
              try {
                long macidval = Long.parseLong(macid_str,16);
                if(macidval!=0) {
                  macid.setText("P25RX MAC ID: "+String.format("0x%12x", macidval));
                }
                else {
                  macid.setText("");
                }
              } catch(Exception e) {
                  macid.setText("");
              }
            }
            */

            if(st1.equals("site_id")) {
              String siteid_str = st.nextToken();
              try {
                int siteidval = Integer.parseInt(siteid_str,10);
                if(siteidval!=0) {
                  siteid.setText("SITE ID: "+String.format("%03d", siteidval));
                }
                else {
                  siteid.setText("");
                }
              } catch(Exception e) {
                  siteid.setText("");
              }
            }

            if(st1.equals("rf_id")) {
              String rfid_str = st.nextToken();
              try {
                int rfidval = Integer.parseInt(rfid_str,10);
                if(rfidval!=0) {
                  rfid.setText("RFSS ID: "+String.format("%03d", rfidval));
                }
                else {
                  rfid.setText("");
                }
              } catch(Exception e) {
                  rfid.setText("");
              }
            }

            if(st1.equals("nac")) {
              String nac_str = st.nextToken();
              try {
                int nacval = Integer.parseInt(nac_str.substring(2,nac_str.length()),16);
                if(nacval!=0 && nac_str.startsWith("0x") ) {
                  nac.setText("NAC: "+String.format("0x%03x", nacval));
                }
                else {
                  if(nacval==0) nac.setText("");
                }
              } catch(Exception e) {
                  nac.setText("");
              }
            }

            if(console_line.contains("DMR DATA_SYNC")) {
              is_dmr_mode=1;
              did_metadata=0;
              p25_status_timeout=6000;
            }

            if(console_line.contains("Con+ Voice Grant:")) {
              is_dmr_mode=1;
              did_metadata=0;
              p25_status_timeout=6000;
            }

            if(st1.equals("sys_id")) {
              String sys_id_str = st.nextToken();
              try {
                int sys_id = Integer.parseInt(sys_id_str.substring(2,sys_id_str.length()),16);
                if(sys_id!=0) {
                  sysid.setText("SYS_ID: "+String.format("0x%03x", sys_id));
                }
                else {
                  sysid.setText("");
                }
              } catch(Exception e) {
                  sysid.setText("");
              }
            }

            if(st1.equals("wacn_id")) {
              String wacn_id = st.nextToken();
              try {
                int t_current_wacn_id = Integer.parseInt(wacn_id.substring(2,wacn_id.length()),16);
                if(t_current_wacn_id!=0) {
                  wacn.setText("WACN: "+wacn_id); 
                }
                else {
                  wacn.setText("");
                }

                is_dmr_mode=0;
              } catch(Exception e) {
                  wacn.setText("");
                //e.printStackTrace();
              }
            }

            if(st1.equals("freq:")) {
              freqval = st.nextToken();
              freqval = freqval.substring(0,freqval.length()-1);

              try {
                current_freq = Double.valueOf(freqval)*1e6;
              } catch(Exception e) {
              }

              freq.setText("Freq: "+freqval);
              freqval = " "+freqval+" MHz, ";

            }

            if( (st1.contains("Skipping") && do_tdma_messages) || st1.contains("MOT_GRG") ) {
              String text = log_ta.getText();
              String phase2_str = console_line.trim();
              log_ta.setText(text.concat("\r\n"+phase2_str));

              try {
                phase2_str = "\r\n"+phase2_str;
                fos_meta.write(phase2_str.getBytes(),0,phase2_str.length());  //write Int num records
                fos_meta.flush();
              } catch(Exception e) {
              }
            }

            if(st1.contains("rssi:")) {
              String rssi = st.nextToken();
              rssi = rssi.replace(","," ").trim();
              rssim1.setValue( Integer.valueOf(rssi).intValue(),true );
              sig_meter_timeout=20000;
              if(l3.getText().contains("NO SIG")) l3.setText("");
            }


            if(st1.equals("tsbk_ps")) {
              did_metadata=0;
              meta_count=0;
              tsbk_ps = st.nextToken();
              tsbk_ps = tsbk_ps.replace(","," ").trim();
              String sys_id_str="";
                sys_id_str = new Integer(current_sys_id).toString();
                sys_id_str = "SYS_ID: "+sys_id_str;
                String hex_nac = String.format("0x%03x", current_sys_id);
                sys_id_str = sys_id_str.concat(" ("+hex_nac+" hex)");
              if(is_dmr_mode==0 && current_sys_id==0) {
                //sys_id_str = new Integer(current_sys_id).toString();
                //sys_id_str = "SYS_ID: "+sys_id_str;
                //sys_id_str = sys_id_str.concat("<-invalid");
                sys_id_str="";
              }

              if(tg_update_pending==1) {
                tg_update_pending=0;
                do_update_talkgroups=1;
              }

              int blks_per_sec=0;
              try {
                blks_per_sec = Integer.valueOf(tsbk_ps);
              } catch(Exception e) {
                e.printStackTrace();
              }

              if(is_dmr_mode==1) {
                if( rssim1.getValue()<-127 && blks_per_sec==0) {
                  l3.setText("NO SIG");
                }
                else {
                  l3.setText("  DMR BLKS_PER_SEC "+tsbk_ps);
                  reset_session=1;
                }
              }
              else {
                if( rssim1.getValue()<-127 && blks_per_sec==0) {
                  l3.setText("NO SIG");
                }
                else {
                  l3.setText("  P25P1 CONTROL CHANNEL BLKS_PER_SEC "+tsbk_ps);
                  reset_session=1;
                  if(system_alias.getText()!=null && system_alias.getText().length()>0 ) {

                    status.setVisible(true);
                  }
                }
              }
              p25_status_timeout=6000;
              String city="";
              try {

                double d = 0.0; 
                String ff="";
                try {
                  d = new Double(freq.getText().substring(6,15));
                  ff = String.format("%3.8f", d);
                } catch(Exception e) {
                }

                if(prefs!=null) city = prefs.get("city_state_"+ff, "unknown");
                if(city!=null && city.length()>0 && status_timeout==0) {
                  status.setVisible(true);
                  if(city.equals("unknown")) city="";
                  if(city.contains("null")) city="";
                  if(city.contains("NULL")) city="";
                  //status.setText("    System: "+city+"  "+sys_id_str);
                  p25_status_timeout=6000;
                }
              } catch(Exception e) {
              }

              try {
                tsbk_ps_i = new Integer(tsbk_ps);

                long time = new java.util.Date().getTime();
                if(time-start_time > 1500) {

                  /*
                  if(tsbk_ps_i>30) rssim2.setValue(-50,false);
                  else if(tsbk_ps_i>25) rssim2.setValue(-80,false);
                  else if(tsbk_ps_i>20) rssim2.setValue(-85,false);
                  else if(tsbk_ps_i>15) rssim2.setValue(-90,false);
                  else if(tsbk_ps_i>10) rssim2.setValue(-95,false);
                  else if(tsbk_ps_i>5) rssim2.setValue(-110,false);
                  else if(tsbk_ps_i>0) rssim2.setValue(-120,false);
                  else rssim2.setValue(-130,false);
                  */
                  if(tsbk_ps_i>20) {
                    sq_indicator.setForeground( java.awt.Color.green );
                    sq_indicator.setBackground( java.awt.Color.green );
                  }
                  else if(tsbk_ps_i>10) {
                    sq_indicator.setForeground( java.awt.Color.blue );
                    sq_indicator.setBackground( java.awt.Color.blue );
                  }
                  else if(tsbk_ps_i>0) {
                    sq_indicator.setForeground( java.awt.Color.red );
                    sq_indicator.setBackground( java.awt.Color.red );
                  }
                  else {
                    sq_indicator.setForeground( java.awt.Color.black );
                    sq_indicator.setBackground( java.awt.Color.black );
                  }
                 }
                 else {
                   //don't update until 1 sec of TSBK
                    sq_indicator.setForeground( java.awt.Color.black );
                    sq_indicator.setBackground( java.awt.Color.black );
                 }

                } catch(Exception e) {
                  //e.printStackTrace();
                }
            }

            String st2 = new String("");
            if(st1.contains("Desc:") ) {

              while(st.hasMoreTokens()) {
                String next_str  = st.nextToken()+" ";
                if(next_str.contains("Con+")) break;
                
                st2 = st2.concat(next_str);
                if(st2.contains(",") && st2.length()>2) {

                  //String l3_line = freqval+st2.substring(0,st2.length()-2)+talkgroup;
                  freqval = freqval.substring(0,freqval.length()-2);
                  String l3_line = freqval+talkgroup+", "+st2.substring(0,st2.length()-2);
                  if(l3_line!=null && l3_line.length()>46) l3_line = l3_line.substring(0,45);

                  if( is_phase1==1 ) {
                    l3.setText("P25P1"+l3_line);
                  }
                  else if( is_phase2==1 ) {
                    l3.setText("P25P2"+l3_line);
                  }
                  p25_status_timeout=6000;
                  break;
                }
              }

            }
          }

            if(console_line.contains("vqg")) { 
              sq_indicator.setForeground( java.awt.Color.green );
              sq_indicator.setBackground( java.awt.Color.green );
            }
            else if(console_line.contains("vqb")) { 
              sq_indicator.setForeground( java.awt.Color.blue );
              sq_indicator.setBackground( java.awt.Color.blue );
            }
            else if(console_line.contains("vqn")) { 
              sq_indicator.setForeground( java.awt.Color.black );
              sq_indicator.setBackground( java.awt.Color.black );
            }
            else if(console_line.contains("vql")) { 
              sq_indicator.setForeground( java.awt.Color.black );
              sq_indicator.setBackground( java.awt.Color.black );
            }
            else if( console_line.contains("tsbk_ps") ) { 
              if( is_dmr_mode==1 ) tsbk_ps_i*=2;
              if( tsbk_ps_i > 20) {
                sq_indicator.setForeground( java.awt.Color.green );
                sq_indicator.setBackground( java.awt.Color.green );
              }
              else if( tsbk_ps_i > 10) {
                sq_indicator.setForeground( java.awt.Color.blue );
                sq_indicator.setBackground( java.awt.Color.blue );
              }
              else if( tsbk_ps_i > 0) {
                sq_indicator.setForeground( java.awt.Color.red );
                sq_indicator.setBackground( java.awt.Color.red );
              }
              else {
                sq_indicator.setForeground( java.awt.Color.black );
                sq_indicator.setBackground( java.awt.Color.black );
              }
            }

          console_line = new String("");
        } catch(Exception e) {
          console_line = new String("");
          //e.printStackTrace();
        }
      }

      if( jTextArea1.getText().length() > 128000 ) {
        String text = jTextArea1.getText();
        String new_text = text.substring(64000,text.length()-1);
        jTextArea1.setText(new_text);
      }

      //TODO: fix
      if(do_add==1) {

        if(str.length()>0 ) {

          String date = time_format.format(new java.util.Date() );
          //str = str.replaceAll("DATA_SYNC",date+" "+"DATA_SYNC");
          //str = str.replaceAll("found DMR_BS_VOICE_SYNC",date+" "+"found DMR_BS_VOICE_SYNC");

          jTextArea1.append(str);
          jTextArea1.setCaretPosition(jTextArea1.getText().length());

          jTextArea1.getCaret().setVisible(true);
          jTextArea1.getCaret().setBlinkRate(250);
        }
      }
     } catch(Exception e) {
       e.printStackTrace();
     }
    }
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    void setAlias(String alias) {
      current_alias=alias;
    }
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    void setProgress(int pcomplete) {
      if(pcomplete<0) {
        progbar.setVisible(false);
        progbar.setValue(0);
        progress_label.setVisible(false);
      }
      else {
        progbar.setVisible(true);
        progress_label.setVisible(true);
        progbar.setValue(pcomplete);
      }
      repaint();
    }

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    void setStatus(String str) {
      if(str==null) return;
      status.setVisible(true);
      status.setText("Status: "+str);
      status_timeout=1600;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonGroup5 = new javax.swing.ButtonGroup();
        buttonGroup6 = new javax.swing.ButtonGroup();
        buttonGroup7 = new javax.swing.ButtonGroup();
        buttonGroup8 = new javax.swing.ButtonGroup();
        buttonGroup9 = new javax.swing.ButtonGroup();
        buttonGroup10 = new javax.swing.ButtonGroup();
        buttonGroup11 = new javax.swing.ButtonGroup();
        buttonGroup12 = new javax.swing.ButtonGroup();
        buttonGroup13 = new javax.swing.ButtonGroup();
        bottom_panel = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        status_panel = new javax.swing.JPanel();
        status = new javax.swing.JLabel();
        tiny_const = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        progress_label = new javax.swing.JLabel();
        progbar = new javax.swing.JProgressBar();
        jSeparator2 = new javax.swing.JSeparator();
        meter_panel = new javax.swing.JPanel();
        desc_panel = new javax.swing.JPanel();
        level_panel = new javax.swing.JPanel();
        sq_lb = new javax.swing.JLabel();
        sq_indicator = new javax.swing.JToggleButton();
        tg_lb = new javax.swing.JLabel();
        tg_indicator = new javax.swing.JToggleButton();
        bt_lb = new javax.swing.JLabel();
        bt_indicator = new javax.swing.JToggleButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();

        p25rxconfigpanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        discover = new javax.swing.JButton();
        disconnect = new javax.swing.JButton();
        check_firmware = new javax.swing.JButton();
        write_config = new javax.swing.JButton();
        fw_ver = new javax.swing.JLabel();
        fw_installed = new javax.swing.JLabel();
        read_config = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        lineout_vol_slider = new javax.swing.JSlider();
        en_bluetooth_cb = new javax.swing.JCheckBox();
        allow_unknown_tg_cb = new javax.swing.JCheckBox();
        volume_label = new javax.swing.JLabel();
        enable_leds = new javax.swing.JCheckBox();
        btreset1 = new javax.swing.JLabel();
        bluetooth_reset = new javax.swing.JTextField();
        btreset2 = new javax.swing.JLabel();
        frequency_tf1 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        no_voice_panel = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        no_voice_secs = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        roaming = new javax.swing.JCheckBox();
        freq_label = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        rfmaxgain = new javax.swing.JComboBox<>();
        vtimeout = new javax.swing.JComboBox<>();
        jLabel34 = new javax.swing.JLabel();
        agc_kp_lb = new javax.swing.JLabel();
        agc_kp = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        op_mode = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        ref_freq = new javax.swing.JTextField();
        controlchannel = new javax.swing.JRadioButton();
        conventionalchannel = new javax.swing.JRadioButton();
        os_string = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        system_alias = new javax.swing.JTextField();
        jPanel25 = new javax.swing.JPanel();
        jPanel26 = new javax.swing.JPanel();
        jPanel29 = new javax.swing.JPanel();
        dmr_cc_en1 = new javax.swing.JCheckBox();
        jSeparator4 = new javax.swing.JSeparator();
        dmr_lcn1_label = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        lcn1_freq = new javax.swing.JTextField();
        jSeparator38 = new javax.swing.JSeparator();
        dmr_clear_freqs = new javax.swing.JButton();
        jPanel31 = new javax.swing.JPanel();
        dmr_cc_en2 = new javax.swing.JCheckBox();
        jSeparator6 = new javax.swing.JSeparator();
        jLabel36 = new javax.swing.JLabel();
        jSeparator7 = new javax.swing.JSeparator();
        lcn2_freq = new javax.swing.JTextField();
        jPanel32 = new javax.swing.JPanel();
        dmr_cc_en3 = new javax.swing.JCheckBox();
        jSeparator8 = new javax.swing.JSeparator();
        jLabel37 = new javax.swing.JLabel();
        jSeparator9 = new javax.swing.JSeparator();
        lcn3_freq = new javax.swing.JTextField();
        jSeparator39 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        dmr_sys_id = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        jPanel33 = new javax.swing.JPanel();
        dmr_cc_en4 = new javax.swing.JCheckBox();
        jSeparator10 = new javax.swing.JSeparator();
        jLabel38 = new javax.swing.JLabel();
        jSeparator11 = new javax.swing.JSeparator();
        lcn4_freq = new javax.swing.JTextField();
        jPanel34 = new javax.swing.JPanel();
        dmr_cc_en5 = new javax.swing.JCheckBox();
        jSeparator12 = new javax.swing.JSeparator();
        jLabel39 = new javax.swing.JLabel();
        jSeparator13 = new javax.swing.JSeparator();
        lcn5_freq = new javax.swing.JTextField();
        jPanel35 = new javax.swing.JPanel();
        dmr_cc_en6 = new javax.swing.JCheckBox();
        jSeparator14 = new javax.swing.JSeparator();
        jLabel40 = new javax.swing.JLabel();
        jSeparator15 = new javax.swing.JSeparator();
        lcn6_freq = new javax.swing.JTextField();
        jPanel36 = new javax.swing.JPanel();
        dmr_cc_en7 = new javax.swing.JCheckBox();
        jSeparator16 = new javax.swing.JSeparator();
        jLabel41 = new javax.swing.JLabel();
        jSeparator17 = new javax.swing.JSeparator();
        lcn7_freq = new javax.swing.JTextField();
        jPanel37 = new javax.swing.JPanel();
        dmr_cc_en8 = new javax.swing.JCheckBox();
        jSeparator18 = new javax.swing.JSeparator();
        jLabel42 = new javax.swing.JLabel();
        jSeparator19 = new javax.swing.JSeparator();
        lcn8_freq = new javax.swing.JTextField();
        jPanel38 = new javax.swing.JPanel();
        dmr_cc_en9 = new javax.swing.JCheckBox();
        jSeparator20 = new javax.swing.JSeparator();
        jLabel43 = new javax.swing.JLabel();
        jSeparator21 = new javax.swing.JSeparator();
        lcn9_freq = new javax.swing.JTextField();
        jPanel39 = new javax.swing.JPanel();
        dmr_cc_en10 = new javax.swing.JCheckBox();
        jSeparator22 = new javax.swing.JSeparator();
        jLabel44 = new javax.swing.JLabel();
        jSeparator23 = new javax.swing.JSeparator();
        lcn10_freq = new javax.swing.JTextField();
        jPanel40 = new javax.swing.JPanel();
        dmr_cc_en11 = new javax.swing.JCheckBox();
        jSeparator24 = new javax.swing.JSeparator();
        jLabel45 = new javax.swing.JLabel();
        jSeparator25 = new javax.swing.JSeparator();
        lcn11_freq = new javax.swing.JTextField();
        jPanel41 = new javax.swing.JPanel();
        dmr_cc_en12 = new javax.swing.JCheckBox();
        jSeparator26 = new javax.swing.JSeparator();
        jLabel46 = new javax.swing.JLabel();
        jSeparator27 = new javax.swing.JSeparator();
        lcn12_freq = new javax.swing.JTextField();
        jPanel42 = new javax.swing.JPanel();
        dmr_cc_en13 = new javax.swing.JCheckBox();
        jSeparator28 = new javax.swing.JSeparator();
        jLabel47 = new javax.swing.JLabel();
        jSeparator29 = new javax.swing.JSeparator();
        lcn13_freq = new javax.swing.JTextField();
        jPanel43 = new javax.swing.JPanel();
        dmr_cc_en14 = new javax.swing.JCheckBox();
        jSeparator30 = new javax.swing.JSeparator();
        jLabel48 = new javax.swing.JLabel();
        jSeparator31 = new javax.swing.JSeparator();
        lcn14_freq = new javax.swing.JTextField();
        jPanel44 = new javax.swing.JPanel();
        dmr_cc_en15 = new javax.swing.JCheckBox();
        jSeparator32 = new javax.swing.JSeparator();
        jLabel49 = new javax.swing.JLabel();
        jSeparator33 = new javax.swing.JSeparator();
        lcn15_freq = new javax.swing.JTextField();
        jPanel46 = new javax.swing.JPanel();
        dmr_conplus = new javax.swing.JRadioButton();
        dmr_conventional = new javax.swing.JRadioButton();
        jSeparator36 = new javax.swing.JSeparator();
        dmr_slot1 = new javax.swing.JCheckBox();
        dmr_slot2 = new javax.swing.JCheckBox();
        jSeparator37 = new javax.swing.JSeparator();
        jPanel30 = new javax.swing.JPanel();
        dmr_backup = new javax.swing.JButton();
        jSeparator34 = new javax.swing.JSeparator();
        dmr_restore = new javax.swing.JButton();
        jSeparator35 = new javax.swing.JSeparator();
        dmr_write_config = new javax.swing.JButton();
        jPanel47 = new javax.swing.JPanel();
        audiopanel = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        enable_mp3 = new javax.swing.JCheckBox();
        enable_audio = new javax.swing.JCheckBox();
        mp3_separate_files = new javax.swing.JCheckBox();
        jScrollPane3 = new javax.swing.JScrollPane();
        audio_dev_list = new javax.swing.JList<>();
        jLabel3 = new javax.swing.JLabel();
        select_home = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        home_dir_label = new javax.swing.JLabel();
        audio_dev_play = new javax.swing.JRadioButton();
        audio_dev_all = new javax.swing.JRadioButton();
        jPanel50 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        audio_rate = new javax.swing.JTextField();
        jLabel35 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        apply_audio_rate = new javax.swing.JButton();
        jPanel13 = new javax.swing.JPanel();
        freqdb_panel = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        freq_table = new javax.swing.JTable();
        jPanel16 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        inc_bus = new javax.swing.JCheckBox();
        inc_gov = new javax.swing.JCheckBox();
        inc_trunked_only = new javax.swing.JCheckBox();
        inc_p25 = new javax.swing.JCheckBox();
        inc_dmr = new javax.swing.JCheckBox();
        jPanel18 = new javax.swing.JPanel();
        inc_vhf = new javax.swing.JCheckBox();
        inc_400mhz = new javax.swing.JCheckBox();
        inc_700mhz = new javax.swing.JCheckBox();
        inc_800mhz = new javax.swing.JCheckBox();
        inc_900mhz = new javax.swing.JCheckBox();
        inc_dup_freq = new javax.swing.JCheckBox();
        jPanel14 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        search_radius = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        zipcode = new javax.swing.JTextField();
        freq_search = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel16 = new javax.swing.JLabel();
        city = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        state = new javax.swing.JTextField();
        freq_search2 = new javax.swing.JButton();
        jPanel19 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        testfreqs = new javax.swing.JButton();
        write_cc = new javax.swing.JButton();
        append_cc = new javax.swing.JButton();
        use_freq_primary = new javax.swing.JButton();
        jPanel20 = new javax.swing.JPanel();
        restore_roam = new javax.swing.JButton();
        backup_roam = new javax.swing.JButton();
        erase_roaming = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        talkgroup_panel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel22 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        enable_table_rows = new javax.swing.JButton();
        disable_table_rows = new javax.swing.JButton();
        read_tg = new javax.swing.JButton();
        send_tg = new javax.swing.JButton();
        backup_tg = new javax.swing.JButton();
        jPanel23 = new javax.swing.JPanel();
        restore_tg = new javax.swing.JButton();
        import_csv = new javax.swing.JButton();
        auto_flash_tg = new javax.swing.JCheckBox();
        disable_encrypted = new javax.swing.JCheckBox();
        auto_pop_table = new javax.swing.JCheckBox();
        consolePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel6 = new javax.swing.JPanel();
        jPanel51 = new javax.swing.JPanel();
        enable_voice_const = new javax.swing.JRadioButton();
        enable_commands = new javax.swing.JRadioButton();
        enable_conlog = new javax.swing.JCheckBox();
        jPanel52 = new javax.swing.JPanel();
        follow_tg = new javax.swing.JButton();
        skip_tg = new javax.swing.JButton();
        logpanel = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        log_ta = new javax.swing.JTextArea();
        buttong_config = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        button_write_config = new javax.swing.JButton();
        jPanel27 = new javax.swing.JPanel();
        single_click_opt1 = new javax.swing.JRadioButton();
        single_click_opt2 = new javax.swing.JRadioButton();
        single_click_opt3 = new javax.swing.JRadioButton();
        single_click_opt4 = new javax.swing.JRadioButton();
        single_click_opt5 = new javax.swing.JRadioButton();
        single_click_opt6 = new javax.swing.JRadioButton();
        jPanel28 = new javax.swing.JPanel();
        double_click_opt1 = new javax.swing.JRadioButton();
        double_click_opt2 = new javax.swing.JRadioButton();
        double_click_opt3 = new javax.swing.JRadioButton();
        double_click_opt4 = new javax.swing.JRadioButton();
        double_click_opt5 = new javax.swing.JRadioButton();
        double_click_opt6 = new javax.swing.JRadioButton();
        jPanel45 = new javax.swing.JPanel();
        triple_click_opt1 = new javax.swing.JRadioButton();
        triple_click_opt2 = new javax.swing.JRadioButton();
        triple_click_opt3 = new javax.swing.JRadioButton();
        triple_click_opt4 = new javax.swing.JRadioButton();
        triple_click_opt5 = new javax.swing.JRadioButton();
        triple_click_opt6 = new javax.swing.JRadioButton();
        jPanel48 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        skip_tg_to = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jSeparator40 = new javax.swing.JSeparator();
        roaming_ret_to_cc = new javax.swing.JCheckBox();
        jLabel33 = new javax.swing.JLabel();
        jPanel49 = new javax.swing.JPanel();
        quad_click_opt1 = new javax.swing.JRadioButton();
        quad_click_opt2 = new javax.swing.JRadioButton();
        quad_click_opt3 = new javax.swing.JRadioButton();
        quad_click_opt4 = new javax.swing.JRadioButton();
        quad_click_opt5 = new javax.swing.JRadioButton();
        quad_click_opt6 = new javax.swing.JRadioButton();
        alias_panel = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        alias_table = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        import_alias = new javax.swing.JButton();
        advancedpanel = new javax.swing.JPanel();
        duid_enh = new javax.swing.JCheckBox();
        freq_correct_on_voice = new javax.swing.JCheckBox();
        add_tdu_silence = new javax.swing.JCheckBox();
        adv_write_config = new javax.swing.JButton();
        wacn_en = new javax.swing.JCheckBox();
        en_encout = new javax.swing.JCheckBox();
        en_p2_tones = new javax.swing.JCheckBox();
        p25_reinit = new javax.swing.JCheckBox();
        p25_tone_vol = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        en_zero_rid = new javax.swing.JCheckBox();
        enc_mode = new javax.swing.JCheckBox();
        allow_tg_pri_int = new javax.swing.JCheckBox();
        en_visuals = new javax.swing.JCheckBox();
        process_rid_alias = new javax.swing.JCheckBox();
        p1_ch_bw = new javax.swing.JComboBox<>();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        p2_ch_bw = new javax.swing.JComboBox<>();
        jLabel54 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        signalinsightpanel = new javax.swing.JPanel();
        const_panel = new javax.swing.JPanel();
        jPanel24 = new javax.swing.JPanel();
        autoscale_const = new javax.swing.JCheckBox();
        nsymbols = new javax.swing.JComboBox<>();
        jPanel8 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jButton2 = new javax.swing.JButton();
        jLabel29 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox<>();
        jLabel30 = new javax.swing.JLabel();
        jComboBox4 = new javax.swing.JComboBox<>();
        jButton3 = new javax.swing.JButton();
        jLabel31 = new javax.swing.JLabel();
        jComboBox5 = new javax.swing.JComboBox<>();
        jPanel10 = new javax.swing.JPanel();
        logo_panel = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        wacn = new javax.swing.JLabel();
        sysid = new javax.swing.JLabel();
        nac = new javax.swing.JLabel();
        freq = new javax.swing.JLabel();
        siteid = new javax.swing.JLabel();
        rfid = new javax.swing.JLabel();
        macid = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        minimize = new javax.swing.JToggleButton();
        mute = new javax.swing.JToggleButton();
        record_to_mp3 = new javax.swing.JToggleButton();
        release_date = new javax.swing.JLabel();
        ser_dev = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                formComponentMoved(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        bottom_panel.setLayout(new javax.swing.BoxLayout(bottom_panel, javax.swing.BoxLayout.Y_AXIS));

        jPanel9.setLayout(new java.awt.BorderLayout());

        status_panel.setBackground(new java.awt.Color(0, 0, 0));
        status_panel.setMinimumSize(new java.awt.Dimension(99, 33));
        status_panel.setPreferredSize(new java.awt.Dimension(1004, 33));
        status_panel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 7));

        status.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        status.setForeground(new java.awt.Color(255, 255, 255));
        status.setText("Status: Idle");
        status.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        status_panel.add(status);

        jPanel9.add(status_panel, java.awt.BorderLayout.CENTER);

        tiny_const.setBackground(new java.awt.Color(0, 0, 0));
        tiny_const.setPreferredSize(new java.awt.Dimension(33, 33));
        tiny_const.setRequestFocusEnabled(false);
        jPanel9.add(tiny_const, java.awt.BorderLayout.EAST);

        bottom_panel.add(jPanel9);

        jPanel2.setBackground(new java.awt.Color(0, 0, 0));
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        progress_label.setForeground(new java.awt.Color(255, 255, 255));
        progress_label.setText("Working...");
        jPanel2.add(progress_label);

        progbar.setBackground(new java.awt.Color(204, 204, 204));
        progbar.setForeground(new java.awt.Color(102, 102, 102));
        progbar.setToolTipText("");
        progbar.setDoubleBuffered(true);
        progbar.setPreferredSize(new java.awt.Dimension(320, 14));
        progbar.setStringPainted(true);
        jPanel2.add(progbar);

        jSeparator2.setForeground(new java.awt.Color(255, 255, 255));
        jSeparator2.setEnabled(false);
        jSeparator2.setPreferredSize(new java.awt.Dimension(250, 0));
        jPanel2.add(jSeparator2);

        bottom_panel.add(jPanel2);

        meter_panel.setBackground(new java.awt.Color(0, 0, 0));
        meter_panel.setForeground(new java.awt.Color(255, 255, 255));
        meter_panel.setLayout(new javax.swing.BoxLayout(meter_panel, javax.swing.BoxLayout.LINE_AXIS));

        desc_panel.setBackground(new java.awt.Color(0, 0, 0));
        desc_panel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        meter_panel.add(desc_panel);

        level_panel.setBackground(new java.awt.Color(0, 0, 0));
        level_panel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        sq_lb.setForeground(new java.awt.Color(255, 255, 255));
        sq_lb.setText("SIG");
        level_panel.add(sq_lb);

        sq_indicator.setFont(new java.awt.Font("Dialog", 1, 8)); // NOI18N
        sq_indicator.setText("SQ");
        sq_indicator.setToolTipText("");
        sq_indicator.setBorderPainted(false);
        sq_indicator.setFocusPainted(false);
        level_panel.add(sq_indicator);

        tg_lb.setForeground(new java.awt.Color(255, 255, 255));
        tg_lb.setText("TG");
        level_panel.add(tg_lb);

        tg_indicator.setFont(new java.awt.Font("Dialog", 1, 8)); // NOI18N
        tg_indicator.setText("TG");
        tg_indicator.setToolTipText("");
        tg_indicator.setBorderPainted(false);
        tg_indicator.setFocusPainted(false);
        level_panel.add(tg_indicator);

        bt_lb.setForeground(new java.awt.Color(255, 255, 255));
        bt_lb.setText("BT");
        level_panel.add(bt_lb);

        bt_indicator.setFont(new java.awt.Font("Dialog", 1, 8)); // NOI18N
        bt_indicator.setText("BT");
        bt_indicator.setToolTipText("");
        bt_indicator.setBorderPainted(false);
        bt_indicator.setFocusPainted(false);
        level_panel.add(bt_indicator);

        meter_panel.add(level_panel);

        bottom_panel.add(meter_panel);

        getContentPane().add(bottom_panel, java.awt.BorderLayout.SOUTH);

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(1115, 659));
        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });
        jTabbedPane1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTabbedPane1MouseClicked(evt);
            }
        });

        p25rxconfigpanel.setMinimumSize(new java.awt.Dimension(1110, 554));
        p25rxconfigpanel.setPreferredSize(new java.awt.Dimension(1110, 554));
        p25rxconfigpanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(204, 204, 204));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        discover.setText("Connect To P25RX");
        discover.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discoverActionPerformed(evt);
            }
        });
        jPanel12.add(discover);

        disconnect.setText("Disconnect");
        disconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectActionPerformed(evt);
            }
        });
        jPanel12.add(disconnect);

        check_firmware.setText("Install Latest Firmware");
        check_firmware.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                check_firmwareActionPerformed(evt);
            }
        });
        jPanel12.add(check_firmware);

        write_config.setText("Write Config To P25RX");
        write_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                write_configActionPerformed(evt);
            }
        });
        jPanel12.add(write_config);

        fw_ver.setText("FW Date: 2020-03-04 16:00 ");
        jPanel12.add(fw_ver);

        fw_installed.setText("FW currently installed:");
        jPanel12.add(fw_installed);

        read_config.setText("Read Config From P25RX");
        read_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_configActionPerformed(evt);
            }
        });
        jPanel12.add(read_config);

        jPanel1.add(jPanel12);

        p25rxconfigpanel.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 444, 1110, 70));

        jLabel5.setText("Line Out Volume");
        p25rxconfigpanel.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 130, -1, -1));

        lineout_vol_slider.setPaintLabels(true);
        lineout_vol_slider.setPaintTicks(true);
        lineout_vol_slider.setToolTipText("This option control the audio line-out level for driving powered speakers or line-in on an external device.");
        lineout_vol_slider.setValue(100);
        lineout_vol_slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                lineout_vol_sliderStateChanged(evt);
            }
        });
        p25rxconfigpanel.add(lineout_vol_slider, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 130, -1, 30));

        en_bluetooth_cb.setSelected(true);
        en_bluetooth_cb.setText("Enable Bluetooth On Power-Up");
        en_bluetooth_cb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en_bluetooth_cbActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(en_bluetooth_cb, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 170, -1, -1));

        allow_unknown_tg_cb.setSelected(true);
        allow_unknown_tg_cb.setText("Allow Unknown Talkgroups");
        p25rxconfigpanel.add(allow_unknown_tg_cb, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 290, -1, -1));

        volume_label.setText("1.0");
        p25rxconfigpanel.add(volume_label, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 130, -1, -1));

        enable_leds.setSelected(true);
        enable_leds.setText("Enable Status LEDS");
        p25rxconfigpanel.add(enable_leds, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 330, -1, -1));

        btreset1.setText("Reset Bluetooth Link Every ");
        p25rxconfigpanel.add(btreset1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 250, -1, -1));

        bluetooth_reset.setColumns(5);
        p25rxconfigpanel.add(bluetooth_reset, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 240, -1, -1));

        btreset2.setText("Minutes.  [ 5 - xxxxx, 0=no BT reset (default)  ]");
        p25rxconfigpanel.add(btreset2, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 250, -1, -1));

        frequency_tf1.setColumns(10);
        frequency_tf1.setToolTipText("Optionally, Use The <Search DB For Nearby Control Channels> Tab To Configure The Primary Control Channel.");
        p25rxconfigpanel.add(frequency_tf1, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 40, -1, 30));

        jLabel7.setText("MHz");
        p25rxconfigpanel.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 50, -1, -1));

        jLabel2.setText("<primary>");
        p25rxconfigpanel.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 50, -1, -1));

        jLabel25.setText("Switch Control Channel After");
        no_voice_panel.add(jLabel25);

        no_voice_secs.setColumns(5);
        no_voice_secs.setText("180");
        no_voice_panel.add(no_voice_secs);

        jLabel24.setText("Secs Of Inactivity ( 0=USE CC IDLE)");
        no_voice_panel.add(jLabel24);

        p25rxconfigpanel.add(no_voice_panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 360, 550, 40));

        roaming.setText("Enable Roaming");
        roaming.setToolTipText("This option is intended for mobile operation.  Disable if you itend to listen to a single local control channel.");
        roaming.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roamingActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(roaming, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 370, -1, -1));

        freq_label.setText("Control Channel Frequency");
        p25rxconfigpanel.add(freq_label, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 50, -1, -1));

        jLabel32.setText("RF Max Gain");
        p25rxconfigpanel.add(jLabel32, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 280, -1, -1));

        rfmaxgain.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "47 dB (max sensitivity)", "44 dB", "41 dB", "38 dB", "35 dB", "32 dB (default)", "29 dB", "26 dB", "23 dB", "20 dB", "14 dB", "8 dB (max linearity)", " " }));
        p25rxconfigpanel.add(rfmaxgain, new org.netbeans.lib.awtextra.AbsoluteConstraints(790, 270, -1, 30));

        vtimeout.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "100ms", "250ms", "500ms", "1sec", "1.5sec", "2sec", "3sec", "5sec", "10sec", "30sec" }));
        vtimeout.setToolTipText("The time since the last activity on a talk group before the receiver will follow a different talk group.");
        p25rxconfigpanel.add(vtimeout, new org.netbeans.lib.awtextra.AbsoluteConstraints(760, 120, -1, -1));

        jLabel34.setText("Talk Group Timeout");
        p25rxconfigpanel.add(jLabel34, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 125, -1, -1));

        agc_kp_lb.setText("AGC Kp");
        p25rxconfigpanel.add(agc_kp_lb, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 180, -1, -1));

        agc_kp.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Low (optimized for fixed location)", "Med", "High (optimized for mobile)" }));
        p25rxconfigpanel.add(agc_kp, new org.netbeans.lib.awtextra.AbsoluteConstraints(760, 170, -1, -1));

        jLabel6.setText("Power-on Mode");
        p25rxconfigpanel.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 90, -1, -1));

        op_mode.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "P25", "DMR", "NXDN4800", "FM NB" }));
        op_mode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                op_modeActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(op_mode, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 80, 80, 30));

        jLabel4.setText("Ref Freq");
        p25rxconfigpanel.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 50, -1, -1));

        ref_freq.setColumns(12);
        ref_freq.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ref_freqActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(ref_freq, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 50, -1, -1));

        buttonGroup7.add(controlchannel);
        controlchannel.setSelected(true);
        controlchannel.setText("Control Channel");
        controlchannel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlchannelActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(controlchannel, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 90, -1, -1));

        buttonGroup7.add(conventionalchannel);
        conventionalchannel.setText("Conventional");
        conventionalchannel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                conventionalchannelActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(conventionalchannel, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 90, -1, -1));

        os_string.setText("OS: ");
        p25rxconfigpanel.add(os_string, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        jLabel11.setText("System Alias");
        p25rxconfigpanel.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 210, -1, -1));
        p25rxconfigpanel.add(system_alias, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 199, 350, 30));

        jTabbedPane1.addTab("P25RX Configuration", p25rxconfigpanel);

        jPanel25.setLayout(new java.awt.BorderLayout());

        jPanel26.setLayout(new java.awt.GridLayout(16, 0));

        jPanel29.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en1.setText("Control Channel");
        jPanel29.add(dmr_cc_en1);

        jSeparator4.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator4.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel29.add(jSeparator4);

        dmr_lcn1_label.setText("LCN1 Frequency");
        jPanel29.add(dmr_lcn1_label);

        jSeparator5.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator5.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel29.add(jSeparator5);

        lcn1_freq.setColumns(15);
        jPanel29.add(lcn1_freq);

        jSeparator38.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator38.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel29.add(jSeparator38);

        dmr_clear_freqs.setText("Clear All Frequencies");
        dmr_clear_freqs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dmr_clear_freqsActionPerformed(evt);
            }
        });
        jPanel29.add(dmr_clear_freqs);

        jPanel26.add(jPanel29);

        jPanel31.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en2.setText("Control Channel");
        jPanel31.add(dmr_cc_en2);

        jSeparator6.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator6.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel31.add(jSeparator6);

        jLabel36.setText("LCN2 Frequency");
        jPanel31.add(jLabel36);

        jSeparator7.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator7.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel31.add(jSeparator7);

        lcn2_freq.setColumns(15);
        jPanel31.add(lcn2_freq);

        jPanel26.add(jPanel31);

        jPanel32.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en3.setText("Control Channel");
        jPanel32.add(dmr_cc_en3);

        jSeparator8.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator8.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel32.add(jSeparator8);

        jLabel37.setText("LCN3 Frequency");
        jPanel32.add(jLabel37);

        jSeparator9.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator9.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel32.add(jSeparator9);

        lcn3_freq.setColumns(15);
        jPanel32.add(lcn3_freq);

        jSeparator39.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator39.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel32.add(jSeparator39);

        jLabel8.setText("Sys ID");
        jPanel32.add(jLabel8);

        dmr_sys_id.setColumns(5);
        dmr_sys_id.setText("1");
        jPanel32.add(dmr_sys_id);

        jLabel26.setText("Decimal");
        jPanel32.add(jLabel26);

        jPanel26.add(jPanel32);

        jPanel33.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en4.setText("Control Channel");
        jPanel33.add(dmr_cc_en4);

        jSeparator10.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator10.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel33.add(jSeparator10);

        jLabel38.setText("LCN4 Frequency");
        jPanel33.add(jLabel38);

        jSeparator11.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator11.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel33.add(jSeparator11);

        lcn4_freq.setColumns(15);
        jPanel33.add(lcn4_freq);

        jPanel26.add(jPanel33);

        jPanel34.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en5.setText("Control Channel");
        jPanel34.add(dmr_cc_en5);

        jSeparator12.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator12.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel34.add(jSeparator12);

        jLabel39.setText("LCN5 Frequency");
        jPanel34.add(jLabel39);

        jSeparator13.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator13.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel34.add(jSeparator13);

        lcn5_freq.setColumns(15);
        jPanel34.add(lcn5_freq);

        jPanel26.add(jPanel34);

        jPanel35.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en6.setText("Control Channel");
        jPanel35.add(dmr_cc_en6);

        jSeparator14.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator14.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel35.add(jSeparator14);

        jLabel40.setText("LCN6 Frequency");
        jPanel35.add(jLabel40);

        jSeparator15.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator15.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel35.add(jSeparator15);

        lcn6_freq.setColumns(15);
        jPanel35.add(lcn6_freq);

        jPanel26.add(jPanel35);

        jPanel36.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en7.setText("Control Channel");
        jPanel36.add(dmr_cc_en7);

        jSeparator16.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator16.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel36.add(jSeparator16);

        jLabel41.setText("LCN7 Frequency");
        jPanel36.add(jLabel41);

        jSeparator17.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator17.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel36.add(jSeparator17);

        lcn7_freq.setColumns(15);
        jPanel36.add(lcn7_freq);

        jPanel26.add(jPanel36);

        jPanel37.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en8.setText("Control Channel");
        jPanel37.add(dmr_cc_en8);

        jSeparator18.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator18.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel37.add(jSeparator18);

        jLabel42.setText("LCN8 Frequency");
        jPanel37.add(jLabel42);

        jSeparator19.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator19.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel37.add(jSeparator19);

        lcn8_freq.setColumns(15);
        jPanel37.add(lcn8_freq);

        jPanel26.add(jPanel37);

        jPanel38.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en9.setText("Control Channel");
        jPanel38.add(dmr_cc_en9);

        jSeparator20.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator20.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel38.add(jSeparator20);

        jLabel43.setText("LCN9 Frequency");
        jPanel38.add(jLabel43);

        jSeparator21.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator21.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel38.add(jSeparator21);

        lcn9_freq.setColumns(15);
        jPanel38.add(lcn9_freq);

        jPanel26.add(jPanel38);

        jPanel39.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en10.setText("Control Channel");
        jPanel39.add(dmr_cc_en10);

        jSeparator22.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator22.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel39.add(jSeparator22);

        jLabel44.setText("LCN10 Frequency");
        jPanel39.add(jLabel44);

        jSeparator23.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator23.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel39.add(jSeparator23);

        lcn10_freq.setColumns(15);
        jPanel39.add(lcn10_freq);

        jPanel26.add(jPanel39);

        jPanel40.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en11.setText("Control Channel");
        jPanel40.add(dmr_cc_en11);

        jSeparator24.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator24.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel40.add(jSeparator24);

        jLabel45.setText("LCN11 Frequency");
        jPanel40.add(jLabel45);

        jSeparator25.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator25.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel40.add(jSeparator25);

        lcn11_freq.setColumns(15);
        jPanel40.add(lcn11_freq);

        jPanel26.add(jPanel40);

        jPanel41.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en12.setText("Control Channel");
        jPanel41.add(dmr_cc_en12);

        jSeparator26.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator26.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel41.add(jSeparator26);

        jLabel46.setText("LCN12 Frequency");
        jPanel41.add(jLabel46);

        jSeparator27.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator27.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel41.add(jSeparator27);

        lcn12_freq.setColumns(15);
        jPanel41.add(lcn12_freq);

        jPanel26.add(jPanel41);

        jPanel42.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en13.setText("Control Channel");
        jPanel42.add(dmr_cc_en13);

        jSeparator28.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator28.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel42.add(jSeparator28);

        jLabel47.setText("LCN13 Frequency");
        jPanel42.add(jLabel47);

        jSeparator29.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator29.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel42.add(jSeparator29);

        lcn13_freq.setColumns(15);
        jPanel42.add(lcn13_freq);

        jPanel26.add(jPanel42);

        jPanel43.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en14.setText("Control Channel");
        jPanel43.add(dmr_cc_en14);

        jSeparator30.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator30.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel43.add(jSeparator30);

        jLabel48.setText("LCN14 Frequency");
        jPanel43.add(jLabel48);

        jSeparator31.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator31.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel43.add(jSeparator31);

        lcn14_freq.setColumns(15);
        jPanel43.add(lcn14_freq);

        jPanel26.add(jPanel43);

        jPanel44.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dmr_cc_en15.setText("Control Channel");
        jPanel44.add(dmr_cc_en15);

        jSeparator32.setMinimumSize(new java.awt.Dimension(150, 0));
        jSeparator32.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel44.add(jSeparator32);

        jLabel49.setText("LCN15 Frequency");
        jPanel44.add(jLabel49);

        jSeparator33.setMinimumSize(new java.awt.Dimension(50, 0));
        jSeparator33.setPreferredSize(new java.awt.Dimension(50, 0));
        jPanel44.add(jSeparator33);

        lcn15_freq.setColumns(15);
        jPanel44.add(lcn15_freq);

        jPanel26.add(jPanel44);

        jPanel46.setPreferredSize(new java.awt.Dimension(885, 50));

        buttonGroup6.add(dmr_conplus);
        dmr_conplus.setSelected(true);
        dmr_conplus.setText("Con+ Control");
        dmr_conplus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dmr_conplusActionPerformed(evt);
            }
        });
        jPanel46.add(dmr_conplus);

        buttonGroup6.add(dmr_conventional);
        dmr_conventional.setText("Conventional");
        dmr_conventional.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dmr_conventionalActionPerformed(evt);
            }
        });
        jPanel46.add(dmr_conventional);

        jSeparator36.setPreferredSize(new java.awt.Dimension(100, 0));
        jPanel46.add(jSeparator36);

        dmr_slot1.setSelected(true);
        dmr_slot1.setText("Enable Slot1");
        jPanel46.add(dmr_slot1);

        dmr_slot2.setSelected(true);
        dmr_slot2.setText("Enable Slot2");
        jPanel46.add(dmr_slot2);

        jSeparator37.setPreferredSize(new java.awt.Dimension(100, 0));
        jPanel46.add(jSeparator37);

        jPanel26.add(jPanel46);

        jPanel25.add(jPanel26, java.awt.BorderLayout.CENTER);

        dmr_backup.setText("Backup To File");
        dmr_backup.setEnabled(false);
        dmr_backup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dmr_backupActionPerformed(evt);
            }
        });
        jPanel30.add(dmr_backup);

        jSeparator34.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel30.add(jSeparator34);

        dmr_restore.setText("Restore From File");
        dmr_restore.setEnabled(false);
        dmr_restore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dmr_restoreActionPerformed(evt);
            }
        });
        jPanel30.add(dmr_restore);

        jSeparator35.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel30.add(jSeparator35);

        dmr_write_config.setText("Write Configuration");
        dmr_write_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dmr_write_configActionPerformed(evt);
            }
        });
        jPanel30.add(dmr_write_config);

        jPanel25.add(jPanel30, java.awt.BorderLayout.SOUTH);

        jPanel47.setPreferredSize(new java.awt.Dimension(50, 100));
        jPanel25.add(jPanel47, java.awt.BorderLayout.WEST);

        jTabbedPane1.addTab("DMR", jPanel25);

        audiopanel.setLayout(new java.awt.BorderLayout());

        jPanel11.setEnabled(false);
        jPanel11.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        enable_mp3.setSelected(true);
        enable_mp3.setText("Enable .mp3 audio file generation");
        enable_mp3.setToolTipText("This option will generate mp3 files in the p25rx directory located in the user home directory.  ~/p25rx on Linux and Documents/p25rx on Windows.");
        enable_mp3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_mp3ActionPerformed(evt);
            }
        });
        jPanel11.add(enable_mp3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 210, -1, -1));

        enable_audio.setSelected(true);
        enable_audio.setText("Enable PC Audio Output (PC Speakers)");
        enable_audio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_audioActionPerformed(evt);
            }
        });
        jPanel11.add(enable_audio, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 350, -1, -1));

        mp3_separate_files.setText("Generate separate files per session  ");
        mp3_separate_files.setEnabled(false);
        mp3_separate_files.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mp3_separate_filesActionPerformed(evt);
            }
        });
        jPanel11.add(mp3_separate_files, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 240, -1, -1));

        jScrollPane3.setAutoscrolls(true);

        audio_dev_list.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Default" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        audio_dev_list.setSelectedIndex(0);
        audio_dev_list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                audio_dev_listValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(audio_dev_list);

        jPanel11.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 40, 510, 430));

        jLabel3.setText("PC Output Audio Device Selection");
        jPanel11.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(680, 20, -1, -1));

        select_home.setText("Select");
        select_home.setEnabled(false);
        select_home.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                select_homeActionPerformed(evt);
            }
        });
        jPanel11.add(select_home, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 120, -1, -1));

        jLabel10.setText("Audio Output Dir:");
        jPanel11.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 150, -1, -1));

        home_dir_label.setText("/home/p25rx");
        jPanel11.add(home_dir_label, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 170, -1, -1));

        buttonGroup8.add(audio_dev_play);
        audio_dev_play.setText("Show Play Devices Only");
        audio_dev_play.setEnabled(false);
        audio_dev_play.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audio_dev_playActionPerformed(evt);
            }
        });
        jPanel11.add(audio_dev_play, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 480, -1, -1));

        buttonGroup8.add(audio_dev_all);
        audio_dev_all.setSelected(true);
        audio_dev_all.setText("Show All Devices");
        audio_dev_all.setEnabled(false);
        audio_dev_all.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audio_dev_allActionPerformed(evt);
            }
        });
        jPanel11.add(audio_dev_all, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 480, -1, -1));

        jLabel9.setText("Audio Playback Rate");
        jPanel50.add(jLabel9);

        audio_rate.setColumns(8);
        audio_rate.setText("48000");
        jPanel50.add(audio_rate);

        jLabel35.setText("Hz");
        jPanel50.add(jLabel35);

        jLabel50.setText("Default: 48000 Hz");
        jPanel50.add(jLabel50);

        jPanel11.add(jPanel50, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 420, 290, 50));

        apply_audio_rate.setText("Apply Rate");
        apply_audio_rate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apply_audio_rateActionPerformed(evt);
            }
        });
        jPanel11.add(apply_audio_rate, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 420, -1, -1));

        audiopanel.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel13.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        audiopanel.add(jPanel13, java.awt.BorderLayout.PAGE_START);

        jTabbedPane1.addTab("PC Audio", audiopanel);

        freqdb_panel.setLayout(new java.awt.BorderLayout());

        freq_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));

        freq_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [8000][11],
            new String [] {
                "LICENSE", "GRANTEE", "ENTITY(GOV,BUS)", "CC FREQ", "TEST", "RESULTS", "INFLASH", "SRV_CLASS", "CITY", "STATE", "EMISSION"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class,java.lang.String.class,java.lang.String.class,java.lang.String.class,java.lang.String.class,java.lang.String.class,

            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        freq_table.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                freq_tableKeyTyped(evt);
            }
        });
        jScrollPane8.setViewportView(freq_table);

        freqdb_panel.add(jScrollPane8, java.awt.BorderLayout.CENTER);

        jPanel16.setLayout(new javax.swing.BoxLayout(jPanel16, javax.swing.BoxLayout.Y_AXIS));

        jPanel17.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        inc_bus.setSelected(true);
        inc_bus.setText("Include Businesses");
        inc_bus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inc_busActionPerformed(evt);
            }
        });
        jPanel17.add(inc_bus);

        inc_gov.setSelected(true);
        inc_gov.setText("Include Government");
        jPanel17.add(inc_gov);

        inc_trunked_only.setSelected(true);
        inc_trunked_only.setText("Trunked Only");
        jPanel17.add(inc_trunked_only);

        inc_p25.setSelected(true);
        inc_p25.setText("Include P25");
        inc_p25.setEnabled(false);
        inc_p25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inc_p25ActionPerformed(evt);
            }
        });
        jPanel17.add(inc_p25);

        inc_dmr.setText("Include DMR");
        inc_dmr.setEnabled(false);
        jPanel17.add(inc_dmr);

        jPanel16.add(jPanel17);

        jPanel18.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        inc_vhf.setSelected(true);
        inc_vhf.setText("Include 150-300 MHz");
        jPanel18.add(inc_vhf);

        inc_400mhz.setSelected(true);
        inc_400mhz.setText("Include 400 MHz");
        jPanel18.add(inc_400mhz);

        inc_700mhz.setSelected(true);
        inc_700mhz.setText("Include 700 MHz");
        jPanel18.add(inc_700mhz);

        inc_800mhz.setSelected(true);
        inc_800mhz.setText("Include 800 MHz");
        jPanel18.add(inc_800mhz);

        inc_900mhz.setSelected(true);
        inc_900mhz.setText("Include 900 MHz");
        jPanel18.add(inc_900mhz);

        inc_dup_freq.setText("Include Duplicate Frequencies");
        inc_dup_freq.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inc_dup_freqActionPerformed(evt);
            }
        });
        jPanel18.add(inc_dup_freq);

        jPanel16.add(jPanel18);

        jPanel14.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel14.setText("Search Distance: ");
        jPanel14.add(jLabel14);

        search_radius.setColumns(3);
        search_radius.setText("30");
        jPanel14.add(search_radius);

        jLabel15.setText("miles   ");
        jPanel14.add(jLabel15);

        zipcode.setColumns(6);
        zipcode.setText("99352");
        jPanel14.add(zipcode);

        freq_search.setText("Zipcode Search");
        freq_search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                freq_searchActionPerformed(evt);
            }
        });
        jPanel14.add(freq_search);

        jSeparator3.setPreferredSize(new java.awt.Dimension(30, 0));
        jPanel14.add(jSeparator3);

        jLabel16.setText("City  ");
        jPanel14.add(jLabel16);

        city.setColumns(16);
        city.setText("Richland");
        jPanel14.add(city);

        jLabel17.setText("  State  ");
        jPanel14.add(jLabel17);

        state.setColumns(3);
        state.setText("Wa");
        jPanel14.add(state);

        freq_search2.setText("City/State Search");
        freq_search2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                freq_search2ActionPerformed(evt);
            }
        });
        jPanel14.add(freq_search2);

        jPanel16.add(jPanel14);

        freqdb_panel.add(jPanel16, java.awt.BorderLayout.PAGE_START);

        jPanel19.setLayout(new javax.swing.BoxLayout(jPanel19, javax.swing.BoxLayout.Y_AXIS));

        jPanel15.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        testfreqs.setText("Test Selected Frequencies");
        testfreqs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testfreqsActionPerformed(evt);
            }
        });
        jPanel15.add(testfreqs);

        write_cc.setText("Write Selected To Roaming Flash");
        write_cc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                write_ccActionPerformed(evt);
            }
        });
        jPanel15.add(write_cc);

        append_cc.setText("Add Selected Frequencies To Roaming Flash");
        append_cc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                append_ccActionPerformed(evt);
            }
        });
        jPanel15.add(append_cc);

        use_freq_primary.setText("Set Selected Frequency As Primary");
        use_freq_primary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                use_freq_primaryActionPerformed(evt);
            }
        });
        jPanel15.add(use_freq_primary);

        jPanel19.add(jPanel15);

        jPanel20.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        restore_roam.setText("Restore From Backup");
        restore_roam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restore_roamActionPerformed(evt);
            }
        });
        jPanel20.add(restore_roam);

        backup_roam.setText("Make Backup");
        backup_roam.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backup_roamActionPerformed(evt);
            }
        });
        jPanel20.add(backup_roam);

        erase_roaming.setText("Erase Roaming Flash");
        erase_roaming.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                erase_roamingActionPerformed(evt);
            }
        });
        jPanel20.add(erase_roaming);

        jLabel23.setText("   Use DEL key to delete selected records from flash.");
        jPanel20.add(jLabel23);

        jPanel19.add(jPanel20);

        freqdb_panel.add(jPanel19, java.awt.BorderLayout.SOUTH);

        jTabbedPane1.addTab("Search DB For CC", freqdb_panel);

        talkgroup_panel.setLayout(new java.awt.BorderLayout());

        jTable1.setDoubleBuffered(true);
        jTable1.setEditingColumn(1);
        jTable1.setEditingRow(1);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object[4916][7],
            new String [] {
                "Enabled", "SYS_ID(HEX)", "Priority", "TGRP", "AlphaTag", "Description", "WACN(HEX)"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTable1KeyTyped(evt);
            }
        });
        jScrollPane2.setViewportView(jTable1);

        talkgroup_panel.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel22.setLayout(new javax.swing.BoxLayout(jPanel22, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setBackground(new java.awt.Color(0, 0, 0));

        enable_table_rows.setText("Enable Selected");
        enable_table_rows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_table_rowsActionPerformed(evt);
            }
        });
        jPanel3.add(enable_table_rows);

        disable_table_rows.setText("Disable Selected");
        disable_table_rows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disable_table_rowsActionPerformed(evt);
            }
        });
        jPanel3.add(disable_table_rows);

        read_tg.setText("Read Talk Groups");
        read_tg.setToolTipText("reads talk groups from P25RX Device");
        read_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_tgActionPerformed(evt);
            }
        });
        jPanel3.add(read_tg);

        send_tg.setText("Write Talk Groups");
        send_tg.setToolTipText("writes talkgroups to P25RX device");
        send_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                send_tgActionPerformed(evt);
            }
        });
        jPanel3.add(send_tg);

        backup_tg.setText("Export TGP/CSV");
        backup_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backup_tgActionPerformed(evt);
            }
        });
        jPanel3.add(backup_tg);

        jPanel22.add(jPanel3);

        jPanel23.setBackground(new java.awt.Color(0, 0, 0));
        jPanel23.setForeground(new java.awt.Color(255, 255, 255));

        restore_tg.setText("Restore From TGP");
        restore_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restore_tgActionPerformed(evt);
            }
        });
        jPanel23.add(restore_tg);

        import_csv.setText("Import CSV");
        import_csv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                import_csvActionPerformed(evt);
            }
        });
        jPanel23.add(import_csv);

        auto_flash_tg.setForeground(new java.awt.Color(255, 255, 255));
        auto_flash_tg.setSelected(true);
        auto_flash_tg.setText("AUTO FLASH");
        auto_flash_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                auto_flash_tgActionPerformed(evt);
            }
        });
        jPanel23.add(auto_flash_tg);

        disable_encrypted.setForeground(new java.awt.Color(255, 255, 255));
        disable_encrypted.setSelected(true);
        disable_encrypted.setText("AUTO DISABLE ENCRYPTED");
        disable_encrypted.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disable_encryptedActionPerformed(evt);
            }
        });
        jPanel23.add(disable_encrypted);

        auto_pop_table.setForeground(new java.awt.Color(255, 255, 255));
        auto_pop_table.setSelected(true);
        auto_pop_table.setText("AUTO POPULATE TABLE");
        auto_pop_table.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                auto_pop_tableActionPerformed(evt);
            }
        });
        jPanel23.add(auto_pop_table);

        jPanel22.add(jPanel23);

        talkgroup_panel.add(jPanel22, java.awt.BorderLayout.SOUTH);

        jTabbedPane1.addTab("Talk Group Editor", talkgroup_panel);

        consolePanel.setPreferredSize(new java.awt.Dimension(963, 500));
        consolePanel.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                consolePanelFocusGained(evt);
            }
        });
        consolePanel.setLayout(new java.awt.BorderLayout());

        jTextArea1.setEditable(false);
        jTextArea1.setBackground(new java.awt.Color(0, 0, 0));
        jTextArea1.setColumns(120);
        jTextArea1.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        jTextArea1.setForeground(new java.awt.Color(255, 255, 255));
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(40);
        jTextArea1.setText("\nBlueTail Technologies P25RX Console\n\n$ ");
        jTextArea1.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        jTextArea1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTextArea1FocusGained(evt);
            }
        });
        jTextArea1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextArea1KeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(jTextArea1);

        consolePanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel6.setLayout(new java.awt.GridLayout(2, 1));

        buttonGroup4.add(enable_voice_const);
        enable_voice_const.setSelected(true);
        enable_voice_const.setText("Enable Logging");
        enable_voice_const.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_voice_constActionPerformed(evt);
            }
        });
        jPanel51.add(enable_voice_const);

        buttonGroup4.add(enable_commands);
        enable_commands.setText("Enter Commands");
        enable_commands.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_commandsActionPerformed(evt);
            }
        });
        jPanel51.add(enable_commands);

        enable_conlog.setText("Enable Log To File");
        enable_conlog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_conlogActionPerformed(evt);
            }
        });
        jPanel51.add(enable_conlog);

        jPanel6.add(jPanel51);

        follow_tg.setText("Follow TG");
        follow_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                follow_tgActionPerformed(evt);
            }
        });
        jPanel52.add(follow_tg);

        skip_tg.setText("Skip TG");
        skip_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                skip_tgActionPerformed(evt);
            }
        });
        jPanel52.add(skip_tg);

        jPanel6.add(jPanel52);

        consolePanel.add(jPanel6, java.awt.BorderLayout.PAGE_END);

        jTabbedPane1.addTab("Console", consolePanel);

        logpanel.setPreferredSize(new java.awt.Dimension(963, 500));
        logpanel.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                logpanelFocusGained(evt);
            }
        });
        logpanel.setLayout(new java.awt.BorderLayout());

        log_ta.setEditable(false);
        log_ta.setBackground(new java.awt.Color(0, 0, 0));
        log_ta.setColumns(120);
        log_ta.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        log_ta.setForeground(new java.awt.Color(255, 255, 255));
        log_ta.setLineWrap(true);
        log_ta.setRows(40);
        log_ta.setText("P25RX Received Talk group Log\n\n");
        log_ta.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        log_ta.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                log_taFocusGained(evt);
            }
        });
        log_ta.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                log_taKeyTyped(evt);
            }
        });
        jScrollPane7.setViewportView(log_ta);

        logpanel.add(jScrollPane7, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Talk Group Log", logpanel);

        buttong_config.setLayout(new java.awt.BorderLayout());

        jPanel21.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel18.setText("Single Click");
        jPanel21.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 70, -1, -1));

        jLabel19.setText("Double Click");
        jPanel21.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 140, -1, 20));

        jLabel20.setText("Triple Click");
        jPanel21.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 220, -1, -1));

        button_write_config.setText("Write Config");
        button_write_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_write_configActionPerformed(evt);
            }
        });
        jPanel21.add(button_write_config, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 390, -1, -1));

        jPanel27.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        buttonGroup11.add(single_click_opt1);
        single_click_opt1.setSelected(true);
        single_click_opt1.setText("Follow TG");
        jPanel27.add(single_click_opt1);

        buttonGroup11.add(single_click_opt2);
        single_click_opt2.setText("Bluetooth Pairing");
        jPanel27.add(single_click_opt2);

        buttonGroup11.add(single_click_opt3);
        single_click_opt3.setText("Enable/Disable Status Leds");
        jPanel27.add(single_click_opt3);

        buttonGroup11.add(single_click_opt4);
        single_click_opt4.setText("Skip TG");
        jPanel27.add(single_click_opt4);

        buttonGroup11.add(single_click_opt5);
        single_click_opt5.setText("Enable/Disable Unknown TG");
        jPanel27.add(single_click_opt5);

        buttonGroup11.add(single_click_opt6);
        single_click_opt6.setText("Enable/Disable Roaming");
        jPanel27.add(single_click_opt6);

        jPanel21.add(jPanel27, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 80, 1010, 40));

        jPanel28.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        buttonGroup10.add(double_click_opt1);
        double_click_opt1.setText("Follow TG");
        jPanel28.add(double_click_opt1);

        buttonGroup10.add(double_click_opt2);
        double_click_opt2.setSelected(true);
        double_click_opt2.setText("Bluetooth Pairing");
        jPanel28.add(double_click_opt2);

        buttonGroup10.add(double_click_opt3);
        double_click_opt3.setText("Enable/Disable Status Leds");
        jPanel28.add(double_click_opt3);

        buttonGroup10.add(double_click_opt4);
        double_click_opt4.setText("Skip TG");
        jPanel28.add(double_click_opt4);

        buttonGroup10.add(double_click_opt5);
        double_click_opt5.setText("Enable/Disable Unknown TG");
        jPanel28.add(double_click_opt5);

        buttonGroup10.add(double_click_opt6);
        double_click_opt6.setText("Enable/Disable Roaming");
        jPanel28.add(double_click_opt6);

        jPanel21.add(jPanel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 153, -1, 70));

        jPanel45.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        buttonGroup9.add(triple_click_opt1);
        triple_click_opt1.setText("Follow TG");
        jPanel45.add(triple_click_opt1);

        buttonGroup9.add(triple_click_opt2);
        triple_click_opt2.setText("Bluetooth Pairing");
        jPanel45.add(triple_click_opt2);

        buttonGroup9.add(triple_click_opt3);
        triple_click_opt3.setSelected(true);
        triple_click_opt3.setText("Enable/Disable Status Leds");
        jPanel45.add(triple_click_opt3);

        buttonGroup9.add(triple_click_opt4);
        triple_click_opt4.setText("Skip TG");
        triple_click_opt4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                triple_click_opt4ActionPerformed(evt);
            }
        });
        jPanel45.add(triple_click_opt4);

        buttonGroup9.add(triple_click_opt5);
        triple_click_opt5.setText("Enable/Disable Unknown TG");
        jPanel45.add(triple_click_opt5);

        buttonGroup9.add(triple_click_opt6);
        triple_click_opt6.setText("Enable/Disable Roaming");
        jPanel45.add(triple_click_opt6);

        jPanel21.add(jPanel45, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 230, 1040, 40));

        jLabel21.setText("Skip TG Timeout ");
        jPanel48.add(jLabel21);

        skip_tg_to.setColumns(5);
        skip_tg_to.setText("60");
        jPanel48.add(skip_tg_to);

        jLabel22.setText("Minutes");
        jPanel48.add(jLabel22);

        jSeparator40.setMinimumSize(new java.awt.Dimension(75, 10));
        jSeparator40.setPreferredSize(new java.awt.Dimension(75, 0));
        jPanel48.add(jSeparator40);

        roaming_ret_to_cc.setSelected(true);
        roaming_ret_to_cc.setText("Roaming Return To Primary On Disable");
        jPanel48.add(roaming_ret_to_cc);

        jPanel21.add(jPanel48, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 350, 870, 50));

        jLabel33.setText("Quad Click");
        jPanel21.add(jLabel33, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 290, -1, -1));

        jPanel49.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        buttonGroup13.add(quad_click_opt1);
        quad_click_opt1.setText("Follow TG");
        jPanel49.add(quad_click_opt1);

        buttonGroup13.add(quad_click_opt2);
        quad_click_opt2.setText("Bluetooth Pairing");
        jPanel49.add(quad_click_opt2);

        buttonGroup13.add(quad_click_opt3);
        quad_click_opt3.setText("Enable/Disable Status Leds");
        jPanel49.add(quad_click_opt3);

        buttonGroup13.add(quad_click_opt4);
        quad_click_opt4.setText("Skip TG");
        quad_click_opt4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quad_click_opt4ActionPerformed(evt);
            }
        });
        jPanel49.add(quad_click_opt4);

        buttonGroup13.add(quad_click_opt5);
        quad_click_opt5.setText("Enable/Disable Unknown TG");
        jPanel49.add(quad_click_opt5);

        buttonGroup13.add(quad_click_opt6);
        quad_click_opt6.setSelected(true);
        quad_click_opt6.setText("Enable/Disable Roaming");
        jPanel49.add(quad_click_opt6);

        jPanel21.add(jPanel49, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 300, 1030, -1));

        buttong_config.add(jPanel21, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Button CFG", buttong_config);

        alias_panel.setLayout(new java.awt.BorderLayout());

        alias_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object[16000][2],
            new String [] {
                "Radio ID", "Alias_And_Comments"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane5.setViewportView(alias_table);

        alias_panel.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        import_alias.setText("Import CSV");
        import_alias.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                import_aliasActionPerformed(evt);
            }
        });
        jPanel5.add(import_alias);

        alias_panel.add(jPanel5, java.awt.BorderLayout.SOUTH);

        jTabbedPane1.addTab("Alias", alias_panel);

        advancedpanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        duid_enh.setSelected(true);
        duid_enh.setText("DUID Enhancements (default on)  Recommended for VHF systems");
        duid_enh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                duid_enhActionPerformed(evt);
            }
        });
        advancedpanel.add(duid_enh, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 20, -1, -1));

        freq_correct_on_voice.setText("Correct Frequency On Trunked Voice Channels (default off)");
        freq_correct_on_voice.setEnabled(false);
        freq_correct_on_voice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                freq_correct_on_voiceActionPerformed(evt);
            }
        });
        advancedpanel.add(freq_correct_on_voice, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 50, -1, -1));

        add_tdu_silence.setSelected(true);
        add_tdu_silence.setText("Add Silent Period To Audio Buffer On Phase 1 TDU / TDULC (default off)");
        add_tdu_silence.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_tdu_silenceActionPerformed(evt);
            }
        });
        advancedpanel.add(add_tdu_silence, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 80, -1, -1));

        adv_write_config.setText("Write Config");
        adv_write_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adv_write_configActionPerformed(evt);
            }
        });
        advancedpanel.add(adv_write_config, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 430, -1, -1));

        wacn_en.setText("Include The WACN field in talk group lookup");
        wacn_en.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wacn_enActionPerformed(evt);
            }
        });
        advancedpanel.add(wacn_en, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 110, -1, -1));

        en_encout.setText("Enable Encrypted Audio Output");
        advancedpanel.add(en_encout, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 170, -1, -1));

        en_p2_tones.setSelected(true);
        en_p2_tones.setText("Enable Phase II Tone Output");
        advancedpanel.add(en_p2_tones, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 200, -1, -1));

        p25_reinit.setSelected(true);
        p25_reinit.setText("P25 Mode: Re-init P25RX after 3 minutes of not receiving a P25 sync word");
        advancedpanel.add(p25_reinit, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 260, -1, -1));

        p25_tone_vol.setColumns(5);
        p25_tone_vol.setText("1.0");
        p25_tone_vol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                p25_tone_volActionPerformed(evt);
            }
        });
        advancedpanel.add(p25_tone_vol, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 230, -1, -1));

        jLabel12.setText("P25 Phase II Tone Volume");
        advancedpanel.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 230, -1, -1));

        jLabel13.setText("Range (0.01 to 1.0), Default 1.0");
        advancedpanel.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 230, -1, -1));

        en_zero_rid.setSelected(true);
        en_zero_rid.setText("Allow Logging Of RID = 0 (some transmissions report SRC ID of 0) (no need to write config)");
        en_zero_rid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en_zero_ridActionPerformed(evt);
            }
        });
        advancedpanel.add(en_zero_rid, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 290, -1, -1));

        enc_mode.setText("Return To Control Control Channel And Skip TG for 30 Sec If Encrypted");
        enc_mode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enc_modeActionPerformed(evt);
            }
        });
        advancedpanel.add(enc_mode, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 320, -1, -1));

        allow_tg_pri_int.setSelected(true);
        allow_tg_pri_int.setText("Allow Talk Group Priority Interrupts");
        allow_tg_pri_int.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allow_tg_pri_intActionPerformed(evt);
            }
        });
        advancedpanel.add(allow_tg_pri_int, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 350, -1, -1));

        en_visuals.setSelected(true);
        en_visuals.setText("Enable Visuals In Signal Insights Tab  (disable for system with over-taxed CPU) (no need to write config)");
        en_visuals.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en_visualsActionPerformed(evt);
            }
        });
        advancedpanel.add(en_visuals, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 380, -1, -1));

        process_rid_alias.setSelected(true);
        process_rid_alias.setText("Process RID / Alias (no need to write config)");
        process_rid_alias.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                process_rid_aliasActionPerformed(evt);
            }
        });
        advancedpanel.add(process_rid_alias, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 410, -1, -1));

        p1_ch_bw.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "10.0 kHz", "10.4 kHz", "10.6 kHz", "11.0 kHz", "11.4 kHz", "12.0 kHz", "12.4 kHz", "13.0 kHz", "13.4 kHz", "14.2 kHz", "14.8 kHz", "15.6 kHz", "16.4 kHz", "17.2 kHz", "18.2 kHz", "19.4 kHz", "20.0 kHz", "22.0 kHz", "24.0 kHz" }));
        advancedpanel.add(p1_ch_bw, new org.netbeans.lib.awtextra.AbsoluteConstraints(780, 70, -1, 30));

        jLabel51.setText("P25 P2 Channel Filter BW");
        advancedpanel.add(jLabel51, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 130, -1, -1));

        jLabel52.setText("Default of 15.6 kHz");
        advancedpanel.add(jLabel52, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 130, -1, -1));

        jLabel53.setText("P25 P1 Channel Filter BW");
        advancedpanel.add(jLabel53, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 80, -1, -1));

        p2_ch_bw.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "10.0 kHz", "10.4 kHz", "10.6 kHz", "11.0 kHz", "11.4 kHz", "12.0 kHz", "12.4 kHz", "13.0 kHz", "13.4 kHz", "14.2 kHz", "14.8 kHz", "15.6 kHz", "16.4 kHz", "17.2 kHz", "18.2 kHz", "19.4 kHz", "20.0 kHz", "22.0 kHz", "24.0 kHz" }));
        advancedpanel.add(p2_ch_bw, new org.netbeans.lib.awtextra.AbsoluteConstraints(780, 120, -1, 30));

        jLabel54.setText("Default of 15.6 kHz");
        advancedpanel.add(jLabel54, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 80, -1, -1));

        jLabel55.setText("For P25 P2, the best bw setting is likely to be in the range 12.4 kHz - 15.6 kHz");
        advancedpanel.add(jLabel55, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 200, -1, -1));

        jLabel56.setText("For P25 P1, the best bw setting is likely to be in the range 12.4 kHz - 15.6 kHz");
        advancedpanel.add(jLabel56, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 170, -1, -1));

        jTabbedPane1.addTab("Advanced", advancedpanel);

        signalinsightpanel.setLayout(new javax.swing.BoxLayout(signalinsightpanel, javax.swing.BoxLayout.X_AXIS));

        const_panel.setBackground(new java.awt.Color(0, 0, 0));
        const_panel.setMaximumSize(new java.awt.Dimension(1512, 1512));
        const_panel.setMinimumSize(new java.awt.Dimension(512, 512));
        const_panel.setPreferredSize(new java.awt.Dimension(512, 512));
        const_panel.setLayout(new java.awt.BorderLayout());

        jPanel24.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        autoscale_const.setSelected(true);
        autoscale_const.setText("Auto Scale");
        autoscale_const.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoscale_constActionPerformed(evt);
            }
        });
        jPanel24.add(autoscale_const);

        nsymbols.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "256 Symbols", "512 Symbols", "1024 Symbols", "2048 Symbols", "4096 Symbols", "8192 Symbols" }));
        nsymbols.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nsymbolsActionPerformed(evt);
            }
        });
        jPanel24.add(nsymbols);

        const_panel.add(jPanel24, java.awt.BorderLayout.NORTH);

        signalinsightpanel.add(const_panel);

        jPanel8.setMaximumSize(new java.awt.Dimension(1512, 2147483647));
        jPanel8.setPreferredSize(new java.awt.Dimension(512, 1065));
        jPanel8.setLayout(new java.awt.BorderLayout());

        jPanel7.setEnabled(false);
        jPanel7.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.setEnabled(false);
        jPanel7.add(jComboBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 90, -1, -1));

        jLabel27.setText("Max RF Gain");
        jPanel7.add(jLabel27, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 95, -1, 20));

        jLabel28.setText("AGC Hysteresis");
        jPanel7.add(jLabel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 150, -1, -1));

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox2.setEnabled(false);
        jPanel7.add(jComboBox2, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 140, -1, -1));

        jButton2.setText("Test Changes");
        jButton2.setEnabled(false);
        jPanel7.add(jButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 440, -1, -1));

        jLabel29.setText("SOFT AGC BW");
        jPanel7.add(jLabel29, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 250, -1, -1));

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox3.setEnabled(false);
        jPanel7.add(jComboBox3, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 190, -1, -1));

        jLabel30.setText("HW AGC REF");
        jPanel7.add(jLabel30, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 200, -1, -1));

        jComboBox4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox4.setEnabled(false);
        jPanel7.add(jComboBox4, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 240, -1, -1));

        jButton3.setText("Reset To Defaults");
        jButton3.setEnabled(false);
        jPanel7.add(jButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 440, -1, -1));

        jLabel31.setText("DVGA GAIN");
        jPanel7.add(jLabel31, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 300, -1, -1));

        jComboBox5.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox5.setEnabled(false);
        jPanel7.add(jComboBox5, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 300, -1, -1));

        jPanel8.add(jPanel7, java.awt.BorderLayout.CENTER);

        signalinsightpanel.add(jPanel8);

        jTabbedPane1.addTab("Signal Insights", signalinsightpanel);

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        jPanel10.setBackground(new java.awt.Color(255, 255, 255));
        jPanel10.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        logo_panel.setBackground(new java.awt.Color(255, 255, 255));
        logo_panel.setMinimumSize(new java.awt.Dimension(877, 10));
        logo_panel.setName(""); // NOI18N
        logo_panel.setPreferredSize(new java.awt.Dimension(1150, 80));
        logo_panel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jSeparator1.setMaximumSize(new java.awt.Dimension(32767, 0));
        jSeparator1.setMinimumSize(new java.awt.Dimension(40, 0));
        jSeparator1.setPreferredSize(new java.awt.Dimension(40, 0));
        logo_panel.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 50, -1, -1));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/btconfig/images/btlogo_small.gif"))); // NOI18N
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jButton1.setFocusPainted(false);
        jButton1.setFocusable(false);
        jButton1.setRequestFocusEnabled(false);
        jButton1.setRolloverEnabled(false);
        jButton1.setVerifyInputWhenFocusTarget(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        logo_panel.add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 5, -1, -1));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel1.setText("P25RX");
        logo_panel.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 10, -1, -1));

        wacn.setText("WACN:");
        logo_panel.add(wacn, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 40, -1, -1));

        sysid.setText("SYS_ID:");
        logo_panel.add(sysid, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 40, -1, -1));

        nac.setText("NAC:");
        logo_panel.add(nac, new org.netbeans.lib.awtextra.AbsoluteConstraints(800, 40, -1, -1));

        freq.setText("Freq:");
        logo_panel.add(freq, new org.netbeans.lib.awtextra.AbsoluteConstraints(890, 40, -1, -1));

        siteid.setText("SITE ID:");
        logo_panel.add(siteid, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 60, -1, -1));

        rfid.setText("RFSS ID:");
        logo_panel.add(rfid, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 60, -1, -1));

        macid.setText("MAC ID:");
        logo_panel.add(macid, new org.netbeans.lib.awtextra.AbsoluteConstraints(800, 60, -1, -1));

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        minimize.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        minimize.setText("MON");
        minimize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minimizeActionPerformed(evt);
            }
        });
        jPanel4.add(minimize);

        mute.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        mute.setText("MUTE");
        mute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                muteActionPerformed(evt);
            }
        });
        jPanel4.add(mute);

        record_to_mp3.setText("REC");
        record_to_mp3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                record_to_mp3ActionPerformed(evt);
            }
        });
        jPanel4.add(record_to_mp3);

        release_date.setText("V: ");
        jPanel4.add(release_date);

        logo_panel.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 0, 360, -1));

        ser_dev.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        ser_dev.setText("PORT:");
        logo_panel.add(ser_dev, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, -1, -1));

        jPanel10.add(logo_panel);

        getContentPane().add(jPanel10, java.awt.BorderLayout.NORTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void discoverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discoverActionPerformed
      do_connect();

      //if(serial_port!=null) serial_port.closePort();
      //is_connected=0;
      //discover.setEnabled(true);
    }//GEN-LAST:event_discoverActionPerformed

    public void do_connect() {
      do_connect=1;
      do_read_config=1;
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextArea1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextArea1KeyTyped
    // TODO add your handling code here:
      char c = evt.getKeyChar();

      //System.out.println("got key");

      //if(skip_bytes==0 && do_update_firmware==0) {
      if(do_update_firmware==0) {
        if(c=='\n') {
          String str = String.copyValueOf(keydata,0,keyindex);
          str = str.trim()+"\r\n";
            //handle str 
            byte b[] = new byte[4096];
            for(int i=0;i<keyindex+2;i++) {
              //b[i] = (byte) keydata[i];
              b = str.getBytes();
            }
            if(serial_port!=null) serial_port.writeBytes(b,keyindex+2);
          keyindex=0;
          command_input=0;
        } else if(c=='\b') {
          if(keyindex>0) {
            keyindex--;
            String str = jTextArea1.getText();
            if(str.length()>0) jTextArea1.setText( str.substring(0, str.length()-1) );
          }
        } else {
          if(command_input==0) addTextConsole("\r\n$ ");
          command_input=1;
          command_input_timeout=5000;
          keydata[keyindex++] = c;
          jTextArea1.append( new Character(c).toString() );
        }
      }
    }//GEN-LAST:event_jTextArea1KeyTyped

    private void jTextArea1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextArea1FocusGained
      if(jTabbedPane1.getSelectedIndex()==5) jTextArea1.requestFocus();
    }//GEN-LAST:event_jTextArea1FocusGained

    private void consolePanelFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_consolePanelFocusGained
      if(jTabbedPane1.getSelectedIndex()==5) jTextArea1.requestFocus();
    }//GEN-LAST:event_consolePanelFocusGained

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
      if(jTabbedPane1.getSelectedIndex()==5) jTextArea1.requestFocus();
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void send_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_send_tgActionPerformed
      do_update_talkgroups=1;
    }//GEN-LAST:event_send_tgActionPerformed

    private void read_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_read_tgActionPerformed
      do_read_talkgroups=1;
    }//GEN-LAST:event_read_tgActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
      resizeColumns();
      resizeColumns2();
      resizeColumns3();
      //if(minimize.isSelected()) {
       // setSize(1054,192);
      //}
      save_position();
    }//GEN-LAST:event_formComponentResized

    private void disconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disconnectActionPerformed
      do_disconnect=1;
      firmware_checked=0;
      do_update_firmware=0;
    }//GEN-LAST:event_disconnectActionPerformed

    private void check_firmwareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_check_firmwareActionPerformed
      do_update_firmware=1;
      if(is_connected==0) do_connect=1;
    }//GEN-LAST:event_check_firmwareActionPerformed

    private void minimizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_minimizeActionPerformed
      if(minimize.isSelected()) {
        if(isWindows ) {
          setSize(1020,200);
        }
        else  {
          setSize(1020,185);  //linux and Mac
        }
      }
      else {
        setSize(1054,750);
        //parentSize = Toolkit.getDefaultToolkit().getScreenSize();
        //setSize(new Dimension((int) (parentSize.width * 0.75), (int) (parentSize.height * 0.8)));
      }
    }//GEN-LAST:event_minimizeActionPerformed

    private void restore_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restore_tgActionPerformed
      if(is_connected==0) do_connect();
      do_restore_tg=1;
    }//GEN-LAST:event_restore_tgActionPerformed

    private void record_to_mp3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_record_to_mp3ActionPerformed
      do_toggle_record=1;
    }//GEN-LAST:event_record_to_mp3ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
      if(serial_port!=null) {
        String cmd= new String("en_voice_send 0\r\n");
        serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
        cmd= new String("logging 0\r\n");
        serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
      }

      save_position();
      if(alias!=null) alias.save_alias();

    }//GEN-LAST:event_formWindowClosing

    private void en_bluetooth_cbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_en_bluetooth_cbActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_en_bluetooth_cbActionPerformed

    private void read_configActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_read_configActionPerformed
      do_read_config=1;
    }//GEN-LAST:event_read_configActionPerformed

    private void write_configActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_write_configActionPerformed
      do_read_config=1;
      do_write_config=1;
      cpanel.reset_ref_est();

      current_sys_id = 0;
      current_wacn_id = 0; 
      wacn.setText("");
      sysid.setText("");
      nac.setText("");

      if(prefs!=null) prefs.put( "system_alias", system_alias.getText() );
    }//GEN-LAST:event_write_configActionPerformed

    private void backup_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backup_tgActionPerformed
      did_tg_backup=0;
      do_read_talkgroups=1;
      do_talkgroup_backup=1;
    }//GEN-LAST:event_backup_tgActionPerformed

    private void lineout_vol_sliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lineout_vol_sliderStateChanged
      volume_label.setText( String.format( "%3.2f", (float) lineout_vol_slider.getValue() / 100.0f ) );
    }//GEN-LAST:event_lineout_vol_sliderStateChanged

    private void disable_table_rowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disable_table_rowsActionPerformed
      int[] rows = jTable1.getSelectedRows();
      if(rows.length>0) {
        for(int i=0;i<rows.length;i++) {
          jTable1.getModel().setValueAt(false,jTable1.convertRowIndexToModel(rows[i]),0);
          System.out.println("row "+i);
        } 
      }
    }//GEN-LAST:event_disable_table_rowsActionPerformed

    private void enable_table_rowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_table_rowsActionPerformed
      int[] rows = jTable1.getSelectedRows();
      if(rows.length>0) {
        for(int i=0;i<rows.length;i++) {
          jTable1.getModel().setValueAt(true,jTable1.convertRowIndexToModel(rows[i]),0);
          System.out.println("row "+i);
        } 
      }
    }//GEN-LAST:event_enable_table_rowsActionPerformed

    private void log_taFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_log_taFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_log_taFocusGained

    private void log_taKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_log_taKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_log_taKeyTyped

    private void logpanelFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_logpanelFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_logpanelFocusGained

    private void jTable1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTable1KeyTyped
    }//GEN-LAST:event_jTable1KeyTyped

    private void jTabbedPane1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTabbedPane1MouseClicked
      //System.out.println("evt tab");
      if(jTabbedPane1.getSelectedIndex()==5) {
        //System.out.println("evt tab");
        jTextArea1.requestFocus();
      }
    }//GEN-LAST:event_jTabbedPane1MouseClicked

    private void enable_mp3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_mp3ActionPerformed
      if(prefs!=null) prefs.putBoolean("enable_mp3", enable_mp3.isSelected());
    }//GEN-LAST:event_enable_mp3ActionPerformed

    private void enable_audioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_audioActionPerformed
      if(prefs!=null) prefs.putBoolean("enable_audio", enable_audio.isSelected());
    }//GEN-LAST:event_enable_audioActionPerformed

    private void freq_searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_freq_searchActionPerformed
      do_zipsearch=1;
    }//GEN-LAST:event_freq_searchActionPerformed

    private void freq_search2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_freq_search2ActionPerformed
      do_zipsearch2=1;
    }//GEN-LAST:event_freq_search2ActionPerformed

    private void inc_busActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inc_busActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_inc_busActionPerformed

    private void inc_p25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inc_p25ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_inc_p25ActionPerformed

    private void testfreqsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testfreqsActionPerformed
      if(is_connected==0) do_connect();
      do_test_freqs=1;
    }//GEN-LAST:event_testfreqsActionPerformed

    private void write_ccActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_write_ccActionPerformed
      if(is_connected==0) do_connect();
      do_update_roaming=1;
    }//GEN-LAST:event_write_ccActionPerformed

    private void append_ccActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_append_ccActionPerformed
      if(is_connected==0) do_connect();
      do_append_roaming=1;
    }//GEN-LAST:event_append_ccActionPerformed

    private void use_freq_primaryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_use_freq_primaryActionPerformed
          try {
              for(int i=0;i<250;i++) {
               freq_table.getModel().setValueAt( null, i, 5); 
              }
          } catch(Exception e) {
            e.printStackTrace();
          }
      if(is_connected==0) do_connect();
      do_roam_freq=1;
    }//GEN-LAST:event_use_freq_primaryActionPerformed

    private void inc_dup_freqActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inc_dup_freqActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_inc_dup_freqActionPerformed

    private void restore_roamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restore_roamActionPerformed
      if(is_connected==0) do_connect();
      do_restore_roaming=1;
    }//GEN-LAST:event_restore_roamActionPerformed

    private void backup_roamActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backup_roamActionPerformed
      if(is_connected==0) do_connect();
      do_backup_roaming=1;
    }//GEN-LAST:event_backup_roamActionPerformed

    private void button_write_configActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_write_configActionPerformed
      do_read_config=1;
      do_write_config=1;
      cpanel.reset_ref_est();

      current_sys_id = 0;
      current_wacn_id = 0; 
      wacn.setText("");
      sysid.setText("");
      nac.setText("");
    }//GEN-LAST:event_button_write_configActionPerformed

    private void erase_roamingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_erase_roamingActionPerformed
      if(is_connected==0) do_connect();
      do_erase_roaming=1;
    }//GEN-LAST:event_erase_roamingActionPerformed

    private void auto_flash_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_auto_flash_tgActionPerformed
      if(prefs!=null) prefs.putBoolean( "tg_auto_flash", auto_flash_tg.isSelected());
    }//GEN-LAST:event_auto_flash_tgActionPerformed

    private void disable_encryptedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disable_encryptedActionPerformed
      if(prefs!=null) prefs.putBoolean( "enc_auto_flash", disable_encrypted.isSelected());
    }//GEN-LAST:event_disable_encryptedActionPerformed

    private void roamingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_roamingActionPerformed
      if( roaming.isSelected() ) {
        no_voice_panel.setVisible(true);
      }
      else {
        no_voice_panel.setVisible(false);
      }
    }//GEN-LAST:event_roamingActionPerformed

    private void freq_tableKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_freq_tableKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_freq_tableKeyTyped

    private void enable_voice_constActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_voice_constActionPerformed
      //String cmd= new String("en_voice_send 1\r\n");
      //serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
      String cmd= new String("logging 0\r\n");
      serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
      if(jTabbedPane1.getSelectedIndex()==5) jTextArea1.requestFocus();
    }//GEN-LAST:event_enable_voice_constActionPerformed

    private void enable_commandsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_commandsActionPerformed
      //String cmd= new String("en_voice_send 0\r\n");
      //serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
      String cmd= new String("logging -999\r\n");
      serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
      if(jTabbedPane1.getSelectedIndex()==5) jTextArea1.requestFocus();
    }//GEN-LAST:event_enable_commandsActionPerformed

    private void autoscale_constActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoscale_constActionPerformed
      if(prefs!=null) prefs.putBoolean("autoscale_const", autoscale_const.isSelected());
    }//GEN-LAST:event_autoscale_constActionPerformed

    private void nsymbolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nsymbolsActionPerformed
       if(prefs!=null) prefs.putInt("nsymbols", nsymbols.getSelectedIndex());
    }//GEN-LAST:event_nsymbolsActionPerformed

    private void enable_conlogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_conlogActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_enable_conlogActionPerformed

    private void dmr_backupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmr_backupActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dmr_backupActionPerformed

    private void dmr_restoreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmr_restoreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dmr_restoreActionPerformed

    private void dmr_write_configActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmr_write_configActionPerformed
      do_read_config=1;
      do_write_config=1;

      op_mode.setSelectedIndex(1);

      cpanel.reset_ref_est();


      current_sys_id = 0;
      current_wacn_id = 0; 
      wacn.setText("");
      sysid.setText("");
      nac.setText("");
    }//GEN-LAST:event_dmr_write_configActionPerformed

    private void dmr_conplusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmr_conplusActionPerformed
      update_dmr_lcn1_label();
    }//GEN-LAST:event_dmr_conplusActionPerformed

    private void dmr_clear_freqsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmr_clear_freqsActionPerformed
      lcn1_freq.setText("");
      lcn2_freq.setText("");
      lcn3_freq.setText("");
      lcn4_freq.setText("");
      lcn5_freq.setText("");
      lcn6_freq.setText("");
      lcn7_freq.setText("");
      lcn8_freq.setText("");
      lcn9_freq.setText("");
      lcn10_freq.setText("");
      lcn11_freq.setText("");
      lcn12_freq.setText("");
      lcn13_freq.setText("");
      lcn14_freq.setText("");
      lcn15_freq.setText("");
    }//GEN-LAST:event_dmr_clear_freqsActionPerformed

    private void ref_freqActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ref_freqActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ref_freqActionPerformed

    private void op_modeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_op_modeActionPerformed
      if( op_mode.getSelectedIndex() == 0) {
        controlchannel.setVisible(true);
        conventionalchannel.setVisible(true);
      }
      else {
        controlchannel.setVisible(false);
        conventionalchannel.setVisible(false);
      }

      //update_op_mode( op_mode.getSelectedIndex() );
      int mode = op_mode.getSelectedIndex();
      if( mode == 0) {
        freq_label.setText("P25 Frequency");
      }
      if( mode == 1) {
        freq_label.setText("Frequency");
      }
      if( mode == 2) {
        freq_label.setText("NXDN Frequency");
      }
      if( mode == 3) {
        freq_label.setText("FM NB Frequency");
      }

    }//GEN-LAST:event_op_modeActionPerformed

    public void update_op_mode(int mode) {

      if( mode == 0) {
        freq_label.setText("P25 Frequency");
        jTabbedPane1.setEnabledAt(3, true);
      }
      if( mode == 1) {
        freq_label.setText("Frequency");
        //jTabbedPane1.setEnabledAt(3, false);
      }
      if( mode == 2) {
        freq_label.setText("NXDN Frequency");
        //jTabbedPane1.setEnabledAt(3, false);
      }
      if( mode == 3) {
        freq_label.setText("FM NB Frequency");
        //jTabbedPane1.setEnabledAt(3, false);
      }
    }

    private void controlchannelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlchannelActionPerformed
      freq_label.setText("Control Channel Frequency");
    }//GEN-LAST:event_controlchannelActionPerformed

    private void conventionalchannelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_conventionalchannelActionPerformed
      freq_label.setText("Conventional Channel Frequency");
    }//GEN-LAST:event_conventionalchannelActionPerformed

    private void dmr_conventionalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmr_conventionalActionPerformed
      update_dmr_lcn1_label();
    }//GEN-LAST:event_dmr_conventionalActionPerformed

    private void freq_correct_on_voiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_freq_correct_on_voiceActionPerformed
    }//GEN-LAST:event_freq_correct_on_voiceActionPerformed

    private void adv_write_configActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adv_write_configActionPerformed
      do_read_config=1;
      do_write_config=1;
      cpanel.reset_ref_est();

      current_sys_id = 0;
      current_wacn_id = 0; 
      wacn.setText("");
      sysid.setText("");
      nac.setText("");
    }//GEN-LAST:event_adv_write_configActionPerformed

    private void import_csvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_csvActionPerformed
      if(is_connected==0) do_connect();
      do_restore_tg_csv=1;
    }//GEN-LAST:event_import_csvActionPerformed

    private void wacn_enActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wacn_enActionPerformed
    }//GEN-LAST:event_wacn_enActionPerformed

    private void mp3_separate_filesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mp3_separate_filesActionPerformed
     if(prefs!=null) prefs.putBoolean("mp3_separate_files", mp3_separate_files.isSelected());
    }//GEN-LAST:event_mp3_separate_filesActionPerformed

    private void audio_dev_listValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_audio_dev_listValueChanged
      try {
        String str = (String) audio_dev_list.getSelectedValue();
        if(str!=null) {
          if(prefs!=null) prefs.put("audio_output_device", str); 
          if(aud!=null) aud.dev_changed();
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
    }//GEN-LAST:event_audio_dev_listValueChanged

    private void select_homeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_select_homeActionPerformed
      do_select_home_dir=1;
    }//GEN-LAST:event_select_homeActionPerformed

    private void add_tdu_silenceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_tdu_silenceActionPerformed
    }//GEN-LAST:event_add_tdu_silenceActionPerformed

    private void duid_enhActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_duid_enhActionPerformed
    }//GEN-LAST:event_duid_enhActionPerformed

    private void formComponentMoved(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentMoved
      save_position();
    }//GEN-LAST:event_formComponentMoved

    private void audio_dev_playActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audio_dev_playActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_audio_dev_playActionPerformed

    private void audio_dev_allActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audio_dev_allActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_audio_dev_allActionPerformed

    private void import_aliasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_aliasActionPerformed
      do_alias_import=1;
    }//GEN-LAST:event_import_aliasActionPerformed

    private void p25_tone_volActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_p25_tone_volActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_p25_tone_volActionPerformed

    private void triple_click_opt4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_triple_click_opt4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_triple_click_opt4ActionPerformed

    private void quad_click_opt4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quad_click_opt4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_quad_click_opt4ActionPerformed

    private void en_zero_ridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_en_zero_ridActionPerformed
      if(prefs!=null) prefs.putBoolean("en_zero_rid", en_zero_rid.isSelected());
    }//GEN-LAST:event_en_zero_ridActionPerformed

    private void apply_audio_rateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apply_audio_rateActionPerformed
      try {
        if(aud!=null) {
          aud.update_audio_rate(); 
        }

        if(prefs!=null) prefs.put("audio_sample_rate", audio_rate.getText() );
      } catch(Exception e) {
        e.printStackTrace();
      }
    }//GEN-LAST:event_apply_audio_rateActionPerformed

    private void enc_modeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enc_modeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_enc_modeActionPerformed

    private void auto_pop_tableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_auto_pop_tableActionPerformed
      if(prefs!=null) prefs.putBoolean( "tg_auto_pop_table", auto_pop_table.isSelected());
    }//GEN-LAST:event_auto_pop_tableActionPerformed

    private void follow_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_follow_tgActionPerformed
      String cmd= new String("f \r\n");
      serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
    }//GEN-LAST:event_follow_tgActionPerformed

    private void skip_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_skip_tgActionPerformed
      String cmd= new String("s \r\n");
      serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
    }//GEN-LAST:event_skip_tgActionPerformed

    private void allow_tg_pri_intActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allow_tg_pri_intActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_allow_tg_pri_intActionPerformed

    private void en_visualsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_en_visualsActionPerformed
      if(prefs!=null) prefs.putBoolean( "en_visuals", en_visuals.isSelected());
    }//GEN-LAST:event_en_visualsActionPerformed

    private void muteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_muteActionPerformed
      if(mute.isSelected()) {
        mute.setBackground(java.awt.Color.green);
      }
      else {
        mute.setBackground(java.awt.Color.gray);
      }
    }//GEN-LAST:event_muteActionPerformed

    private void process_rid_aliasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_process_rid_aliasActionPerformed
      if(prefs!=null) prefs.putBoolean("process_rid_alias", process_rid_alias.isSelected());
    }//GEN-LAST:event_process_rid_aliasActionPerformed

    public void enable_voice() {
      frequency_tf1.setEnabled(false);
      roaming.setSelected(false);
      roaming.setEnabled(false);
    }

    public void enable_cc() {
      frequency_tf1.setEnabled(true);
      roaming.setSelected(true);
      roaming.setEnabled(true);
    }

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
public void update_dmr_lcn1_label() {
  if(dmr_conventional.isSelected()) {
    dmr_lcn1_label.setText("DMR Conventional Frequency");
  }
  else {
    dmr_lcn1_label.setText("LCN1 Frequency");
  }
}
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
public void do_meta() {


  if( is_dmr_mode==0 && src_uid==0 && !en_zero_rid.isSelected() ) {
    did_metadata=1;
    return;
  }

  if( did_metadata==0 ) meta_count++;

  if(l3!=null & did_metadata==0 && meta_count>9 && l3.getText().trim().length()>0) {

    if(l3.getText().contains("CONTROL CHANNEL BLKS")) return; 
    if(l3.getText().contains("DMR BLKS_PER_SEC")) return; 
    if(l3.getText().contains("NO SIG")) return; 
    if(l3.getText().contains("TG 0")) return; 

    if( is_dmr_mode==1 ) freq_str = freq.getText();


    String src_uid_str = "";
    String is_enc_str = "";
    String alias_str = "";
    String phase_str="";

    if(is_phase1==1) phase_str="P1";
    if(is_phase2==1) phase_str="P2";

    if(current_alias!=null && src_uid!=0 && current_alias.length()>0) alias_str = current_alias+",";
      current_alias="";

    src_uid_str = "RID "+new Integer(src_uid).toString()+",";

    if(is_enc!=0) is_enc_str = "(ENC),";

    is_enc=0;

    //meta String
    String metadata =""; 
    if(enable_mp3.isSelected()) {
      metadata = "\r\n"+l3.getText()+","+time_format.format(new java.util.Date())+","+rssim1.getValue()+" dbm,"+mp3_file.length()+", cc_freq "+freq_str+" mhz,"+src_uid_str+is_enc_str + alias_str + ","+phase_str;
    }
    else {
      metadata = "\r\n"+l3.getText()+","+time_format.format(new java.util.Date())+","+rssim1.getValue()+" dbm,"+"0"+", cc_freq "+freq_str+" mhz,"+src_uid_str+is_enc_str + alias_str + ","+phase_str;
    }

    if(tg_pri>0) {
      metadata = metadata.concat(" (TG PRI)");
    }

    if(freq_str==null || freq_str.trim().length()==0) freq_str = frequency_tf1.getText();
    if(freq_str.length()==0) metadata=null;


    if(metadata!=null && metadata.length()>0 && !metadata.contains("ctrl ") ) {
      if(enable_mp3.isSelected()) {
        try {
          fos_meta.write(metadata.getBytes(),0,metadata.length());  //write int num records
          fos_meta.flush();
        } catch(Exception e) {
          e.printStackTrace();
        }
      }

      try {
        //add meta info to log tab
        String text = log_ta.getText();

        StringTokenizer st = new StringTokenizer(metadata,",");
        String str1 = "";
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");
        if(st.hasMoreTokens()) st.nextToken(); //mp3 file len
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");

        //src uid
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");
        //(ENC)
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");
        //current alias
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");
        //phase
        if(st.hasMoreTokens()) str1 = str1.concat(st.nextToken()+", ");

        if(tg_pri>0) {
          str1 = str1.concat(" (TG PRI)");
        }

        log_ta.setText(text.concat( new String(str1.getBytes()) ));

        if( log_ta.getText().length() > 16000 ) {
          String new_text = text.substring(8000,text.length()-1);
          log_ta.setText(new_text);
        }

        log_ta.setCaretPosition(log_ta.getText().length());
        log_ta.getCaret().setVisible(true);
        log_ta.getCaret().setBlinkRate(250);

      } catch(Exception e) {
        //e.printstacktrace();
      }
    }
    did_metadata=1;
    tg_pri=0;
  }
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
public void open_audio_output_files() {
  try {
    String fs =  System.getProperty("file.separator");

    Path path = Paths.get(home_dir+"p25rx"+fs+sys_mac_id);
    Files.createDirectories(path);

    home_dir_label.setText(home_dir+"p25rx"+fs+sys_mac_id+fs);

    String date = formatter_date.format(new java.util.Date() );
    current_date=new String(date);  //date changed

    mp3_file = new File(home_dir+"p25rx"+fs+sys_mac_id+fs+"p25rx_recording_"+current_date+".mp3");
    meta_file = new File(home_dir+"p25rx"+fs+sys_mac_id+fs+"p25rx_recmeta_"+current_date+".txt");
    //conlog_file = new File(home_dir+"p25rx_conlog_"+current_date+".txt");
    String exe_path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath().toString();
    exe_path = exe_path.replace("BTConfig.exe", "");
    System.out.println("log file path: "+exe_path+"p25rx_conlog_"+current_date+".txt");
    //tdma_file = new File(exe_path+"p25rx_TDMA_PACKED_DIBITS_"+current_date+".bin");

    fos_mp3 = new FileOutputStream( mp3_file, true ); 
    fos_meta = new FileOutputStream( meta_file, true ); 
    try {
      conlog_file = new File(exe_path+"p25rx_conlog_"+current_date+".txt");
      fos_conlog = new FileOutputStream( conlog_file, true ); 
    } catch(Exception e) {
      e.printStackTrace();
    }

  } catch(Exception e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    JOptionPane.showMessageDialog(this, sw.toString(), "ok", JOptionPane.OK_OPTION);
  }


  try {
    if(aud==null && parent!=null) {
      aud = new audio(parent);
    }
    if(aud!=null) aud.dev_changed();
    if(aud!=null) aud.audio_tick();
  } catch(Exception e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    JOptionPane.showMessageDialog(this, sw.toString(), "ok", JOptionPane.OK_OPTION);
  }
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
public void update_prefs() {

  try {
    //if(prefs==null) prefs = Preferences.userRoot().node(this.getClass().getName()+"_"+sys_mac_id);
    System.out.println("sys_mac_id: "+sys_mac_id);
    if(prefs==null) prefs = Preferences.userRoot().node(sys_mac_id);

    if( !prefs.getBoolean("did_new_agc1", false) ) {
      prefs.putInt("agc_gain", 50);
      prefs.putBoolean("did_new_agc1", true);
    }
    //agc_gain.setValue(65);
    do_agc_update=1;

    if(prefs!=null) {
      int i = prefs.getInt("audio_buffer_system",1);

      enable_mp3.setSelected( prefs.getBoolean("enable_mp3", true) ); 
      enable_audio.setSelected( prefs.getBoolean("enable_audio", true) ); 
      auto_flash_tg.setSelected( prefs.getBoolean("tg_auto_flash", false) );
      auto_pop_table.setSelected( prefs.getBoolean("tg_auto_pop_table", true) );
      disable_encrypted.setSelected( prefs.getBoolean("enc_auto_flash", false) );
      autoscale_const.setSelected( prefs.getBoolean("autoscale_const", true) );
      mp3_separate_files.setSelected( prefs.getBoolean("mp3_separate_files", false) );
      nsymbols.setSelectedIndex( prefs.getInt("nsymbols", 0) );

      en_visuals.setSelected( prefs.getBoolean("en_visuals", true) );

      system_alias.setText( prefs.get("system_alias", "") );


      en_zero_rid.setSelected( prefs.getBoolean("en_zero_rid", true) );

      process_rid_alias.setSelected( prefs.getBoolean("process_rid_alias", true) );

      audio_rate.setText( prefs.get("audio_sample_rate", "47000" ) ); 

      int constellation = prefs.getInt("const_select", 1);
    }

      JFileChooser chooser = new JFileChooser();
      File file = chooser.getCurrentDirectory();  //better for windows to do it this way
      String fs =  System.getProperty("file.separator");
      String home_dir_str = file.getAbsolutePath()+fs;


      home_dir = prefs.get("p25rx_home_dir", home_dir_str);
      home_dir_label.setText(home_dir);
      System.out.println("home_dir: "+home_dir);

      restore_position();


  } catch(Exception e) {
    e.printStackTrace();
  }
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
public void save_position() {
  try {
    if(prefs==null) return;

    Rectangle r = getBounds();

    prefs.putInt("form_x", r.x);
    prefs.putInt("form_y", r.y);
    prefs.putInt("form_width", r.width);
    prefs.putInt("form_height", r.height);

    Boolean b = minimize.isSelected();
    prefs.putBoolean("form_min", b);

  } catch(Exception e) {
  }
}
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
public void restore_position() {
  try {
    if(prefs==null) return;

    Boolean b = prefs.getBoolean("form_min", false);


    int x = prefs.getInt("form_x",50);
    int y = prefs.getInt("form_y",50);
    int width = prefs.getInt("form_width", 1054);
    int height = prefs.getInt("form_height",750);
      //setSize(1054,750);
    Rectangle r = new Rectangle(x,y,width,height);
    setBounds(r);

    if(height==200) b = true;
    if(height==185) b = true;
    if(height==750) b = false;

    minimize.setSelected(b);

  } catch(Exception e) {
  }
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
void toggle_recording(Boolean isrec) {

  boolean is_recording=!isrec;

  String recnow="";
  if(is_recording) recnow="1";  //toggle
  if(!is_recording) recnow="0"; //toggle

  //enable binary voice output for mp3 recording
  for(int i=0;i<99;i++) {
    serial_port.writeBytes( new String("en_voice_send "+recnow+"\r\n").getBytes(), 17, 0);
    try {
      SLEEP(50);
      if(serial_port.bytesAvailable()>29) break;
    } catch(Exception e) {
      //e.printStackTrace();
    }
    byte[] b = new byte[32];
    int len = serial_port.readBytes(b, 30);
    if(len>0) {
      String s = new String(b);
      if(s.contains("en_voice_send "+recnow)) {
        is_recording = !isrec; 
        break;
      };
    }
  }

  if(is_recording) {
    record_to_mp3.setSelected(true);
    record_to_mp3.setBackground(java.awt.Color.red);
    record_to_mp3.setForeground(java.awt.Color.black);
  }
  else {
    record_to_mp3.setSelected(false);
    record_to_mp3.setBackground(java.awt.Color.white);
    record_to_mp3.setForeground(java.awt.Color.black);
  }
}
    
//SUMS 1
float[] columnWidthPercentage = {.075f, .10f, .075f, .075f, .25f, .325f, 0.1f };
private void resizeColumns() {
  // Use TableColumnModel.getTotalColumnWidth() if your table is included in a JScrollPane
  //int tW = jTable1.getWidth();
  int tW = jTable1.getColumnModel().getTotalColumnWidth();
  TableColumn column;
  TableColumnModel jTableColumnModel = jTable1.getColumnModel();
  int cantCols = jTableColumnModel.getColumnCount();
  for (int i = 0; i < cantCols; i++) {
    column = jTableColumnModel.getColumn(i);
    int pWidth = Math.round(columnWidthPercentage[i] * tW);
    column.setPreferredWidth(pWidth);
  }
}

//SUMS 1
float[] columnWidthPercentage2 = {0.08f, 0.25f, .05f, 0.12f, 0.05f, 0.05f, 0.058f, 0.05f, 0.1f, 0.035f, 0.13f };
private void resizeColumns2() {
  int tW = freq_table.getColumnModel().getTotalColumnWidth();
  TableColumn column;
  TableColumnModel jTableColumnModel = freq_table.getColumnModel();
  int cantCols = jTableColumnModel.getColumnCount();
  for (int i = 0; i < cantCols; i++) {
    column = jTableColumnModel.getColumn(i);
    int pWidth = Math.round(columnWidthPercentage2[i] * tW);
    column.setPreferredWidth(pWidth);
  }
}

//SUMS 1
float[] columnWidthPercentage3 = {0.1f, 0.9f};
private void resizeColumns3() {
  int tW = freq_table.getColumnModel().getTotalColumnWidth();
  TableColumn column;
  TableColumnModel jTableColumnModel = alias_table.getColumnModel();
  int cantCols = jTableColumnModel.getColumnCount();
  for (int i = 0; i < cantCols; i++) {
    column = jTableColumnModel.getColumn(i);
    int pWidth = Math.round(columnWidthPercentage3[i] * tW);
    column.setPreferredWidth(pWidth);
  }
}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void SLEEP(long val) {
  try {
    if(is_windows==1) {
      Thread.sleep(val);
    }
    else {
      long start_time  = new java.util.Date().getTime();
      long end_time=start_time+val;
      while(end_time>start_time) {
        start_time  = new java.util.Date().getTime();
      }
    }
  } catch(Exception e) {
    e.printStackTrace();
  }
}

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(BTFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(BTFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(BTFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(BTFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
              parent = new BTFrame(args);
              parent.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JCheckBox add_tdu_silence;
    private javax.swing.JButton adv_write_config;
    private javax.swing.JPanel advancedpanel;
    public javax.swing.JComboBox<String> agc_kp;
    private javax.swing.JLabel agc_kp_lb;
    private javax.swing.JPanel alias_panel;
    public javax.swing.JTable alias_table;
    public javax.swing.JCheckBox allow_tg_pri_int;
    public javax.swing.JCheckBox allow_unknown_tg_cb;
    public javax.swing.JButton append_cc;
    private javax.swing.JButton apply_audio_rate;
    public javax.swing.JRadioButton audio_dev_all;
    public javax.swing.JList<String> audio_dev_list;
    public javax.swing.JRadioButton audio_dev_play;
    public javax.swing.JTextField audio_rate;
    private javax.swing.JPanel audiopanel;
    public javax.swing.JCheckBox auto_flash_tg;
    public javax.swing.JCheckBox auto_pop_table;
    public javax.swing.JCheckBox autoscale_const;
    public javax.swing.JButton backup_roam;
    private javax.swing.JButton backup_tg;
    public javax.swing.JTextField bluetooth_reset;
    private javax.swing.JPanel bottom_panel;
    private javax.swing.JToggleButton bt_indicator;
    private javax.swing.JLabel bt_lb;
    private javax.swing.JLabel btreset1;
    private javax.swing.JLabel btreset2;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup10;
    private javax.swing.ButtonGroup buttonGroup11;
    private javax.swing.ButtonGroup buttonGroup12;
    private javax.swing.ButtonGroup buttonGroup13;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.ButtonGroup buttonGroup5;
    private javax.swing.ButtonGroup buttonGroup6;
    private javax.swing.ButtonGroup buttonGroup7;
    private javax.swing.ButtonGroup buttonGroup8;
    private javax.swing.ButtonGroup buttonGroup9;
    private javax.swing.JButton button_write_config;
    private javax.swing.JPanel buttong_config;
    private javax.swing.JButton check_firmware;
    private javax.swing.JTextField city;
    private javax.swing.JPanel consolePanel;
    private javax.swing.JPanel const_panel;
    public javax.swing.JRadioButton controlchannel;
    public javax.swing.JRadioButton conventionalchannel;
    private javax.swing.JPanel desc_panel;
    public javax.swing.JCheckBox disable_encrypted;
    private javax.swing.JButton disable_table_rows;
    private javax.swing.JButton disconnect;
    private javax.swing.JButton discover;
    private javax.swing.JButton dmr_backup;
    public javax.swing.JCheckBox dmr_cc_en1;
    public javax.swing.JCheckBox dmr_cc_en10;
    public javax.swing.JCheckBox dmr_cc_en11;
    public javax.swing.JCheckBox dmr_cc_en12;
    public javax.swing.JCheckBox dmr_cc_en13;
    public javax.swing.JCheckBox dmr_cc_en14;
    public javax.swing.JCheckBox dmr_cc_en15;
    public javax.swing.JCheckBox dmr_cc_en2;
    public javax.swing.JCheckBox dmr_cc_en3;
    public javax.swing.JCheckBox dmr_cc_en4;
    public javax.swing.JCheckBox dmr_cc_en5;
    public javax.swing.JCheckBox dmr_cc_en6;
    public javax.swing.JCheckBox dmr_cc_en7;
    public javax.swing.JCheckBox dmr_cc_en8;
    public javax.swing.JCheckBox dmr_cc_en9;
    private javax.swing.JButton dmr_clear_freqs;
    public javax.swing.JRadioButton dmr_conplus;
    public javax.swing.JRadioButton dmr_conventional;
    public javax.swing.JLabel dmr_lcn1_label;
    private javax.swing.JButton dmr_restore;
    public javax.swing.JCheckBox dmr_slot1;
    public javax.swing.JCheckBox dmr_slot2;
    public javax.swing.JTextField dmr_sys_id;
    private javax.swing.JButton dmr_write_config;
    public javax.swing.JRadioButton double_click_opt1;
    public javax.swing.JRadioButton double_click_opt2;
    public javax.swing.JRadioButton double_click_opt3;
    public javax.swing.JRadioButton double_click_opt4;
    public javax.swing.JRadioButton double_click_opt5;
    public javax.swing.JRadioButton double_click_opt6;
    public javax.swing.JCheckBox duid_enh;
    public javax.swing.JCheckBox en_bluetooth_cb;
    public javax.swing.JCheckBox en_encout;
    public javax.swing.JCheckBox en_p2_tones;
    public javax.swing.JCheckBox en_visuals;
    public javax.swing.JCheckBox en_zero_rid;
    public javax.swing.JCheckBox enable_audio;
    private javax.swing.JRadioButton enable_commands;
    private javax.swing.JCheckBox enable_conlog;
    public javax.swing.JCheckBox enable_leds;
    public javax.swing.JCheckBox enable_mp3;
    private javax.swing.JButton enable_table_rows;
    private javax.swing.JRadioButton enable_voice_const;
    public javax.swing.JCheckBox enc_mode;
    public javax.swing.JButton erase_roaming;
    private javax.swing.JButton follow_tg;
    public javax.swing.JLabel freq;
    public javax.swing.JCheckBox freq_correct_on_voice;
    public javax.swing.JLabel freq_label;
    private javax.swing.JButton freq_search;
    private javax.swing.JButton freq_search2;
    public javax.swing.JTable freq_table;
    private javax.swing.JPanel freqdb_panel;
    public javax.swing.JTextField frequency_tf1;
    public javax.swing.JLabel fw_installed;
    public javax.swing.JLabel fw_ver;
    public javax.swing.JLabel home_dir_label;
    private javax.swing.JButton import_alias;
    private javax.swing.JButton import_csv;
    public javax.swing.JCheckBox inc_400mhz;
    public javax.swing.JCheckBox inc_700mhz;
    public javax.swing.JCheckBox inc_800mhz;
    public javax.swing.JCheckBox inc_900mhz;
    public javax.swing.JCheckBox inc_bus;
    public javax.swing.JCheckBox inc_dmr;
    public javax.swing.JCheckBox inc_dup_freq;
    public javax.swing.JCheckBox inc_gov;
    public javax.swing.JCheckBox inc_p25;
    public javax.swing.JCheckBox inc_trunked_only;
    public javax.swing.JCheckBox inc_vhf;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JComboBox<String> jComboBox4;
    private javax.swing.JComboBox<String> jComboBox5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    public javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel35;
    private javax.swing.JPanel jPanel36;
    private javax.swing.JPanel jPanel37;
    private javax.swing.JPanel jPanel38;
    private javax.swing.JPanel jPanel39;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel40;
    private javax.swing.JPanel jPanel41;
    private javax.swing.JPanel jPanel42;
    private javax.swing.JPanel jPanel43;
    private javax.swing.JPanel jPanel44;
    private javax.swing.JPanel jPanel45;
    private javax.swing.JPanel jPanel46;
    private javax.swing.JPanel jPanel47;
    private javax.swing.JPanel jPanel48;
    private javax.swing.JPanel jPanel49;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel50;
    private javax.swing.JPanel jPanel51;
    private javax.swing.JPanel jPanel52;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane7;
    public javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JSeparator jSeparator13;
    private javax.swing.JSeparator jSeparator14;
    private javax.swing.JSeparator jSeparator15;
    private javax.swing.JSeparator jSeparator16;
    private javax.swing.JSeparator jSeparator17;
    private javax.swing.JSeparator jSeparator18;
    private javax.swing.JSeparator jSeparator19;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator20;
    private javax.swing.JSeparator jSeparator21;
    private javax.swing.JSeparator jSeparator22;
    private javax.swing.JSeparator jSeparator23;
    private javax.swing.JSeparator jSeparator24;
    private javax.swing.JSeparator jSeparator25;
    private javax.swing.JSeparator jSeparator26;
    private javax.swing.JSeparator jSeparator27;
    private javax.swing.JSeparator jSeparator28;
    private javax.swing.JSeparator jSeparator29;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator30;
    private javax.swing.JSeparator jSeparator31;
    private javax.swing.JSeparator jSeparator32;
    private javax.swing.JSeparator jSeparator33;
    private javax.swing.JSeparator jSeparator34;
    private javax.swing.JSeparator jSeparator35;
    private javax.swing.JSeparator jSeparator36;
    private javax.swing.JSeparator jSeparator37;
    private javax.swing.JSeparator jSeparator38;
    private javax.swing.JSeparator jSeparator39;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator40;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JTabbedPane jTabbedPane1;
    public javax.swing.JTable jTable1;
    private javax.swing.JTextArea jTextArea1;
    public javax.swing.JTextField lcn10_freq;
    public javax.swing.JTextField lcn11_freq;
    public javax.swing.JTextField lcn12_freq;
    public javax.swing.JTextField lcn13_freq;
    public javax.swing.JTextField lcn14_freq;
    public javax.swing.JTextField lcn15_freq;
    public javax.swing.JTextField lcn1_freq;
    public javax.swing.JTextField lcn2_freq;
    public javax.swing.JTextField lcn3_freq;
    public javax.swing.JTextField lcn4_freq;
    public javax.swing.JTextField lcn5_freq;
    public javax.swing.JTextField lcn6_freq;
    public javax.swing.JTextField lcn7_freq;
    public javax.swing.JTextField lcn8_freq;
    public javax.swing.JTextField lcn9_freq;
    private javax.swing.JPanel level_panel;
    public javax.swing.JSlider lineout_vol_slider;
    private javax.swing.JTextArea log_ta;
    private javax.swing.JPanel logo_panel;
    private javax.swing.JPanel logpanel;
    public javax.swing.JLabel macid;
    private javax.swing.JPanel meter_panel;
    private javax.swing.JToggleButton minimize;
    public javax.swing.JCheckBox mp3_separate_files;
    public javax.swing.JToggleButton mute;
    private javax.swing.JLabel nac;
    public javax.swing.JPanel no_voice_panel;
    public javax.swing.JTextField no_voice_secs;
    public javax.swing.JComboBox<String> nsymbols;
    public javax.swing.JComboBox<String> op_mode;
    private javax.swing.JLabel os_string;
    public javax.swing.JComboBox<String> p1_ch_bw;
    public javax.swing.JCheckBox p25_reinit;
    public javax.swing.JTextField p25_tone_vol;
    private javax.swing.JPanel p25rxconfigpanel;
    public javax.swing.JComboBox<String> p2_ch_bw;
    public javax.swing.JCheckBox process_rid_alias;
    private javax.swing.JProgressBar progbar;
    private javax.swing.JLabel progress_label;
    public javax.swing.JRadioButton quad_click_opt1;
    public javax.swing.JRadioButton quad_click_opt2;
    public javax.swing.JRadioButton quad_click_opt3;
    public javax.swing.JRadioButton quad_click_opt4;
    public javax.swing.JRadioButton quad_click_opt5;
    public javax.swing.JRadioButton quad_click_opt6;
    private javax.swing.JButton read_config;
    private javax.swing.JButton read_tg;
    private javax.swing.JToggleButton record_to_mp3;
    public javax.swing.JTextField ref_freq;
    private javax.swing.JLabel release_date;
    public javax.swing.JButton restore_roam;
    private javax.swing.JButton restore_tg;
    public javax.swing.JLabel rfid;
    public javax.swing.JComboBox<String> rfmaxgain;
    public javax.swing.JCheckBox roaming;
    public javax.swing.JCheckBox roaming_ret_to_cc;
    private javax.swing.JTextField search_radius;
    private javax.swing.JButton select_home;
    private javax.swing.JButton send_tg;
    private javax.swing.JLabel ser_dev;
    private javax.swing.JPanel signalinsightpanel;
    public javax.swing.JRadioButton single_click_opt1;
    public javax.swing.JRadioButton single_click_opt2;
    public javax.swing.JRadioButton single_click_opt3;
    public javax.swing.JRadioButton single_click_opt4;
    public javax.swing.JRadioButton single_click_opt5;
    public javax.swing.JRadioButton single_click_opt6;
    public javax.swing.JLabel siteid;
    private javax.swing.JButton skip_tg;
    public javax.swing.JTextField skip_tg_to;
    private javax.swing.JToggleButton sq_indicator;
    private javax.swing.JLabel sq_lb;
    private javax.swing.JTextField state;
    private javax.swing.JLabel status;
    private javax.swing.JPanel status_panel;
    private javax.swing.JLabel sysid;
    public javax.swing.JTextField system_alias;
    private javax.swing.JPanel talkgroup_panel;
    public javax.swing.JButton testfreqs;
    private javax.swing.JToggleButton tg_indicator;
    private javax.swing.JLabel tg_lb;
    private javax.swing.JPanel tiny_const;
    public javax.swing.JRadioButton triple_click_opt1;
    public javax.swing.JRadioButton triple_click_opt2;
    public javax.swing.JRadioButton triple_click_opt3;
    public javax.swing.JRadioButton triple_click_opt4;
    public javax.swing.JRadioButton triple_click_opt5;
    public javax.swing.JRadioButton triple_click_opt6;
    public javax.swing.JButton use_freq_primary;
    public javax.swing.JLabel volume_label;
    public javax.swing.JComboBox<String> vtimeout;
    private javax.swing.JLabel wacn;
    public javax.swing.JCheckBox wacn_en;
    public javax.swing.JButton write_cc;
    private javax.swing.JButton write_config;
    private javax.swing.JTextField zipcode;
    // End of variables declaration//GEN-END:variables
}
