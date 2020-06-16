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

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class updateTask extends java.util.TimerTask
{

    public void run()
    {
      try {

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
                    parent.prefs.put("city_state_"+freq_to_use.trim(), str1+","+str2+"_"+str3 );
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
          if(sys_config==null) sys_config = new SYSConfig();
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

        if(prefs==null) {
          prefs = Preferences.userRoot().node(this.getClass().getName());
          agc_gain.setValue(prefs.getInt("agc_gain", 85));
          //agc_gain.setValue(65);
          do_agc_update=1;
        }

        if(prefs!=null) {
          int i = prefs.getInt("audio_buffer_system",1);
          if(i==1) audio_buffer_system.setSelected(true);
            else audio_buffer_user.setSelected(true);

          enable_mp3.setSelected( prefs.getBoolean("enable_mp3", true) ); 
          enable_audio.setSelected( prefs.getBoolean("enable_audio", true) ); 
          audio_insert_zero.setSelected( prefs.getBoolean("audio_insert_zero", true) ); 
          initial_audio_level.setValue( prefs.getInt("initial_audio_level", 75) );
          auto_flash_tg.setSelected( prefs.getBoolean("tg_auto_flash", true) );
          disable_encrypted.setSelected( prefs.getBoolean("enc_auto_flash", true) );
        }

        //keep this after prefs
        if(aud==null && parent!=null) {
          aud = new audio(parent);
          if(aud!=null) aud.updateLevels();
        }

        if(do_agc_update==1) {
          do_agc_update=0;
            //System.out.println(evt);
            if(prefs!=null) prefs.putInt("agc_gain", agc_gain.getValue() );
            if(aud!=null) aud.setAgcGain( ( 0.01f + (float) agc_gain.getValue()) / 100.0f );
            //System.out.println("agc_gain: "+agc_gain.getValue());
            agc_level_lb.setText("Val "+ agc_gain.getValue()+"%");
        }

        if(do_restore_roaming==1 && is_connected==1 && do_update_firmware==0 && do_read_talkgroups==0) {

          try {
            JFileChooser chooser = new JFileChooser();

            File cdir = new File(home_dir+"p25rx");
            chooser.setCurrentDirectory(cdir);


            FileNameExtensionFilter filter = new FileNameExtensionFilter( "p25rx_roaming backups", "rom");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(parent);

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

        if(do_restore_tg==1 && is_connected==1 && do_update_firmware==0 && do_read_talkgroups==0) {

          try {
            //here image_buffer contains the talkgroup records
            //String home = System.getProperty("user.home");
            //String fs =  System.getProperty("file.separator");

            JFileChooser chooser = new JFileChooser();

            File cdir = new File(home_dir+"p25rx");
            chooser.setCurrentDirectory(cdir);


            FileNameExtensionFilter filter = new FileNameExtensionFilter( "p25rx_talkgroup backups", "tgp");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(parent);

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

              if(tg_config==null) tg_config = new TGConfig();
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
            //l3.setText("Desc:                          ");
            //l3.setText("");
          }
          else {
          }
        }

        if(status_timeout>0) {
          status_timeout--;
          if(status_timeout==0) {
            setStatus("");
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
          if(tg_config==null) tg_config = new TGConfig();
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
          if(tg_config==null) tg_config = new TGConfig();
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
        }
        else if(is_connected==0 && do_connect==1) {

            serial_port = find_serial_port();
            if(serial_port==null) {
              setStatus("\r\ncan't find device");
              Thread.sleep(600);
            }

          /*
            while(true) {
              if(serial_port!=null && serial_port.openPort(100)==true) break;
              if(serial_port==null) break;
              if(serial_port!=null) serial_port.closePort();
              Thread.sleep(50);
            }
          */



            if(serial_port!=null && serial_port.openPort(200)==false) {
              setStatus("\r\ncould not open the device serial port (another app may have the port open). try again....");
            }
            else if(serial_port!=null) {
              do_connect=0;

              serial_port_name = serial_port.getSystemPortName();
              serial_port.setBaudRate( 4000000 ); //this probably doesn't really matter
              serial_port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

              is_connected=1;
              Thread.sleep(600);

              check_firmware.setEnabled(true);

              //if(firmware_checked==0) {
               // do_update_firmware=1;
              //}
              //else {
                do_read_talkgroups=1;
                do_read_roaming=1;
              //}

              discover.setEnabled(false);
              int baudrate = serial_port.getBaudRate();

              String cmd= new String("en_voice_send 0\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
              cmd= new String("logging -999\r\n");
              serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

            }
        }
        else if(is_connected==1 && do_toggle_record==1 && skip_bytes==0) {
          do_toggle_record=0;

          String cmd= new String("logging 0\r\n");
          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

          toggle_recording( !record_to_mp3.isSelected() );

        }
        else if(is_connected==1 && do_update_firmware==0) {
          byte[] b = new byte[1024];
          byte[] str_b = new byte[1024];
          int str_idx=0;

          int avail = serial_port.bytesAvailable();
          if(avail>1024) avail=1024;

          //if( (rx_state>0 && avail>=32) || (rx_state==0 && avail>0 && skip_bytes==0) ) {
          if( avail>0 ) {

            try {
              int len = serial_port.readBytes(b, avail);

              for(int i=0;i<len;i++) {
                if(skip_bytes>0) {
                  pcm_bytes[pcm_idx++] = b[i];

                  skip_bytes--;

                  if( !enable_mp3.isSelected() && skip_bytes==0 ) {
                    try {
                      start_time = new java.util.Date().getTime();
                          tg_indicator.setBackground(java.awt.Color.yellow);
                          tg_indicator.setForeground(java.awt.Color.yellow);
                          tg_indicator.setEnabled(true);
                      if(aud!=null ) {
                        //if(iztimer!=null) iztimer.cancel();
                        if(aud!=null) aud.playBuf(pcm_bytes);
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

                    try {
                      start_time = new java.util.Date().getTime();
                          tg_indicator.setBackground(java.awt.Color.yellow);
                          tg_indicator.setForeground(java.awt.Color.yellow);
                          tg_indicator.setEnabled(true);
                      if(aud!=null ) {
                        //if(iztimer!=null) iztimer.cancel();
                        if(aud!=null) aud.playBuf(pcm_bytes);
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
                      if( current_date==null || !current_date.equals(date) ) {
                        current_date=new String(date);  //date changed

                        try {
                          if(fos_mp3!=null) fos_mp3.close();
                          if(fos_meta!=null) fos_meta.close();
                          if(encoder!=null) encoder.close();
                          fos_mp3 = null;
                          fos_meta = null;
                          encoder=null;
                          skip_header=1;
                        } catch(Exception e) {
                          //e.printStackTrace();
                        }

                        try {


                          Path path = Paths.get(home_dir+"p25rx");
                          Files.createDirectories(path);

                          mp3_file = new File(home_dir+"p25rx/p25rx_recording_"+current_date+".mp3");
                          meta_file = new File(home_dir+"p25rx/p25rx_recmeta_"+current_date+".txt");

                          fos_mp3 = new FileOutputStream( mp3_file, true ); 
                          fos_meta = new FileOutputStream( meta_file, true ); 
                        } catch(Exception e) {
                          //e.printStackTrace();
                        }

                      }


                      if(did_metadata==0 && l3.getText().trim().length()>0) {

                        //META string
                        String metadata = "\r\n"+l3.getText()+","+time_format.format(new java.util.Date())+","+rssim1.getValue()+" dBm,"+mp3_file.length()+", CC_FREQ "+freq_str+" MHz,";

                        if(freq_str==null || freq_str.trim().length()==0) freq_str = frequency_tf1.getText();
                        if(freq_str.length()==0) metadata=null;


                        if(metadata!=null && metadata.length()>0 && !metadata.contains("CTRL ") ) {
                          fos_meta.write(metadata.getBytes(),0,metadata.length());  //write Int num records
                          fos_meta.flush();

                          try {
                            //add meta info to Log tab
                            String text = log_ta.getText();

                            StringTokenizer st = new StringTokenizer(metadata,",");
                            String str1 = "";
                            str1 = str1.concat(st.nextToken()+", ");
                            str1 = str1.concat(st.nextToken()+", ");
                            str1 = str1.concat(st.nextToken()+", ");
                            str1 = str1.concat(st.nextToken()+", ");
                            str1 = str1.concat(st.nextToken()+", ");
                            st.nextToken(); //mp3 file len
                            str1 = str1.concat(st.nextToken()+", ");

                            log_ta.setText(text.concat( new String(str1.getBytes()) ));

                            if( log_ta.getText().length() > 128000 ) {
                              String new_text = text.substring(64000,text.length()-1);
                              log_ta.setText(new_text);
                            }

                            log_ta.setCaretPosition(log_ta.getText().length());
                            log_ta.getCaret().setVisible(true);
                            log_ta.getCaret().setBlinkRate(250);

                          } catch(Exception e) {
                            //e.printStackTrace();
                          }
                        }
                        did_metadata=1;
                      }

                      if(skip_header==1) {
                        skip_header=0;
                      }
                      else {
                        fos_mp3.write(mp3_bytes,0,mp3_bytes.length);  //write Int num records
                        fos_mp3.flush();
                      }

                    }

                    pcm_idx=0;
                    rx_state=0;
                  }
                }
                //b2 5f 9c 71
                else if(rx_state==0 && b[i]==(byte) 0xb2) {
                  rx_state=1;
                }
                else if(rx_state==1 && b[i]==(byte) 0x5f) {
                  rx_state=2;
                }
                else if(rx_state==2 && b[i]==(byte) 0x9c) {
                  rx_state=3;
                }
                else if(rx_state==3 && b[i]==(byte) 0x71) {
                  rx_state=4;
                  //addTextConsole("\r\nfound voice header");
                  skip_bytes=320;
                }
                else if(rx_state==0 && skip_bytes==0 ) {
                  str_b[str_idx++] = b[i];

                }
              }

              if(str_idx>0) {
                addTextConsole( new String(str_b,0,str_idx) );
                str_idx=0;
              }


            } catch(Exception e) {
              //e.printStackTrace();
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
        //e.printStackTrace(System.out);
      }

      long time = new java.util.Date().getTime();
      Boolean isWindows = System.getProperty("os.name").startsWith("Windows");
      int stop_time=50;
      if(isWindows) stop_time=50;
        else stop_time=500; 
      if(time-start_time>stop_time) {
        if(aud!=null) aud.playStop();
        tg_indicator.setBackground(java.awt.Color.black);
        tg_indicator.setForeground(java.awt.Color.black);
      }


    }
}

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
int status_timeout;
rssimeter rssim1;
rssimeter rssim2;
String console_line;
int sig_meter_timeout=0;
javax.swing.JLabel l1;
javax.swing.JLabel l2;
javax.swing.JLabel l3;
int do_restore_tg=0;
int did_tg_backup=1;  //don't do backup on startup
int bluetooth_streaming=0;
int bluetooth_error=0;
int bluetooth_blink;
int tick_mod;
int rx_state=0;
int skip_bytes=0;
byte[] pcm_bytes;
int pcm_idx=0;
LameEncoder encoder=null;
byte[] mp3_buffer;
String current_date=null;
String home_dir=null;
FileOutputStream fos_mp3;
FileOutputStream fos_meta;
File mp3_file=null;
File meta_file=null;
java.text.SimpleDateFormat formatter_date;
java.text.SimpleDateFormat time_format;
int current_sys_id = 0;
int current_wacn_id = 0; 
int do_toggle_record=1;
int did_metadata=0;
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
Hashtable lat_lon_hash1;
Hashtable lat_lon_hash2;
Hashtable no_loc_freqs;
Boolean do_tdma_messages=false;

  ///////////////////////////////////////////////////////////////////
    public BTFrame(String[] args) {
      initComponents();

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
        }
      }

      roaming_tests = new Roaming();

      write_cc.setVisible(false);
      inc_p25.setVisible(false);

      //agc_gain.setVisible(false); //hide agc slider related
      //jLabel3.setVisible(false);
      //agc_level_lb.setVisible(false);

      inc_dmr.setVisible(false);

      fw_ver.setVisible(false);
      fw_installed.setVisible(false);

      pcm_bytes = new byte[320];

      write_config.setEnabled(false);
      disconnect.setEnabled(false);


      isWindows = System.getProperty("os.name").startsWith("Windows");

      read_config.setVisible(false);  //read config button

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

      JFileChooser chooser = new JFileChooser();
      File file = chooser.getCurrentDirectory();  //better for windows to do it this way
      String fs =  System.getProperty("file.separator");
      home_dir = file.getAbsolutePath()+fs;

      formatter_date = new java.text.SimpleDateFormat( "yyyy-MM-dd" );
      time_format = new java.text.SimpleDateFormat( "yyyy-MM-dd-HH:mm:ss" );

      fw_ver.setText("Latest Avail: FW Date: 202005220746");
      release_date.setText("Release: 2020-05-22 07:46");
      fw_installed.setText("   Installed FW: ");

      setProgress(-1);

      jTable1.setShowHorizontalLines(true);
      jTable1.setShowVerticalLines(true);

      freq_table.setShowHorizontalLines(true);
      freq_table.setShowVerticalLines(true);

      keydata = new char[4096];
      keyindex=0;
      jTextArea1.getCaret().setVisible(true);
      jTextArea1.getCaret().setBlinkRate(250);
      setIconImage(new javax.swing.ImageIcon(getClass().getResource("/btconfig/images/iconsmall.gif")).getImage()); // NOI18N
      setTitle("BlueTail Technologies P25RX Configuration Software (c) 2020");

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

      l3.setText("");
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
      l3.setText("");

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

      //do this last
      utimer = new java.util.Timer();
      utimer.schedule( new updateTask(), 100, 1);
      setSize(1054,720);

      //parentSize = Toolkit.getDefaultToolkit().getScreenSize();
      //setSize(new Dimension((int) (parentSize.width * 0.75), (int) (parentSize.height * 0.8)));

        jTabbedPane1.remove( buttong_config);

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
                  jTable1.getModel().setValueAt(null,rows[i],0);
                  jTable1.getModel().setValueAt(null,rows[i],1);
                  jTable1.getModel().setValueAt(null,rows[i],2);
                  jTable1.getModel().setValueAt(null,rows[i],3);
                  jTable1.getModel().setValueAt(null,rows[i],4);
                  jTable1.getModel().setValueAt(null,rows[i],5);
                  //System.out.println("row "+i);
                } 
              }
            //}
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

              for(int i=0;i<250;i++) {
                String str1 = (String) freq_table.getModel().getValueAt(i, 6);
                if(str1!=null && str1.equals("X") ) {
                  flash_recs++;
                }
              }

              if(rows.length>0) {
                for(int i=0;i<rows.length;i++) {
                  freq_table.getModel().setValueAt(null,rows[i],0);
                  freq_table.getModel().setValueAt(null,rows[i],1);
                  freq_table.getModel().setValueAt(null,rows[i],2);
                  freq_table.getModel().setValueAt(null,rows[i],3);
                  freq_table.getModel().setValueAt(null,rows[i],4);
                  freq_table.getModel().setValueAt(null,rows[i],5);
                  freq_table.getModel().setValueAt(null,rows[i],6);
                  freq_table.getModel().setValueAt(null,rows[i],7);
                  freq_table.getModel().setValueAt(null,rows[i],8);
                  freq_table.getModel().setValueAt(null,rows[i],9);
                  freq_table.getModel().setValueAt(null,rows[i],10);
                } 
              }
              if(flash_recs>0) do_write_roaming_flash_only=1;
          }
        });

      do_connect();
    }

  //////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////
  public SerialPort find_serial_port()
  {

    SerialPort[] ports = SerialPort.getCommPorts();
    for(int i=0; i<ports.length; i++) {
      //setStatus("\r\nport: "+ports[i]+" on " + ports[i].getSystemPortName());

      if( ports[i].toString().startsWith("BlueTail-P1") ) { //we are looking for this string in the serial port description
        //setStatus("FOUND device");
        System.out.println("\r\nfound device on : "+ports[i].getSystemPortName());

        if( ports[i].isOpen() ) {
          Boolean isWindows = System.getProperty("os.name").startsWith("Windows");
          //setStatus("Device is currently open by another application.  Please close the application.");
          if(isWindows) {
            ports[i].closePort();
            return null;
          }
          else {
            ports[i].closePort();
            return ports[i];
          }
        }
        return ports[i];
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
    void addTextConsole(String str) {

      if(command_input==1) return;

      if(str!=null && do_console_output==1) System.out.println(str.trim());

      String talkgroup="";
      String freqval="";
      String tsbk_ps="";

      if(console_line==null) console_line = new String("");
      console_line = console_line.concat(str);

      int do_add=1;

      if(console_line.contains("\r\n") && console_line.contains("Return To Control") ) {
        start_time = new java.util.Date().getTime();
      }

      if(console_line.contains("\r\n") && (console_line.contains("Time:") || console_line.contains("ue")) ) {

        try {
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

          //do_add=0;
          StringTokenizer st = new StringTokenizer(console_line," \r\n");
          while(st.hasMoreTokens()) {
            String st1 = st.nextToken();

            if(st1.equals("TGroup:")) {
              String tg_id = st.nextToken();
              talkgroup = ", TG "+tg_id;
              talkgroup = talkgroup.substring(0,talkgroup.length()-1)+" ";

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
                  city = prefs.get("city_state_"+ff, "unknown");
                } catch(Exception e) {
                }
                tg_config.addUknownTG(parent, tg_id, new Integer(current_sys_id).toString(), city); 
              }
            }

            if(console_line.contains("Disabling talkgroup") && st1.equals("talkgroup") ) {
              String tg_id = st.nextToken();
              tg_config.disable_enc_tg(parent, tg_id, new Integer(current_sys_id).toString() );
            }

            if(st1.equals("freq") && !console_line.contains("grant") ) {
              freq_str = st.nextToken();
              if(freq_str!=null) {
                try {
                  double fval = new Double(freq_str).doubleValue();
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

            if(st1.equals("sys_id")) {
              String sys_id_str = st.nextToken();
              try {
                int sys_id = Integer.parseInt(sys_id_str.substring(2,sys_id_str.length()),16);
                current_sys_id = sys_id;
                if(current_sys_id!=0) {
                  sysid.setText("SYS_ID: "+String.format("0x%03x", current_sys_id));
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
                current_wacn_id = Integer.parseInt(wacn_id.substring(2,wacn_id.length()),16);
                if(current_wacn_id!=0) {
                  wacn.setText("WACN: "+wacn_id); 
                }
                else {
                  wacn.setText("");
                }
              } catch(Exception e) {
                  wacn.setText("");
                //e.printStackTrace();
              }
            }

            if(st1.equals("freq:")) {
              freqval = st.nextToken();
              freqval = freqval.substring(0,freqval.length()-1);
              freqval = " "+freqval+" MHz, ";
            }

            if(st1.contains("Skipping") && do_tdma_messages) {
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
              sig_meter_timeout=5000;
            }

            if(st1.equals("tsbk_ps")) {
              did_metadata=0;
              tsbk_ps = st.nextToken();
              tsbk_ps = tsbk_ps.replace(","," ").trim();
              String sys_id_str="";
                sys_id_str = new Integer(current_sys_id).toString();
                sys_id_str = "SYS_ID: "+sys_id_str;
                String hex_nac = String.format("0x%03x", current_sys_id);
                sys_id_str = sys_id_str.concat(" ("+hex_nac+" hex)");
              if(current_sys_id==0) {
                //sys_id_str = new Integer(current_sys_id).toString();
                //sys_id_str = "SYS_ID: "+sys_id_str;
                //sys_id_str = sys_id_str.concat("<-invalid");
                sys_id_str="";
              }

              if(tg_update_pending==1) {
                tg_update_pending=0;
                do_update_talkgroups=1;
              }

              //l3.setText("  CTRL TSBK_PS "+tsbk_ps+"  "+sys_id_str);
              l3.setText("  CONTROL CHANNEL TSBK_PER_SEC "+tsbk_ps);
              String city="";
              try {

                double d = 0.0; 
                String ff="";
                try {
                  d = new Double(freq.getText().substring(6,15));
                  ff = String.format("%3.8f", d);
                } catch(Exception e) {
                }

                city = prefs.get("city_state_"+ff, "unknown");
                if(city!=null && city.length()>0 && status_timeout==0) {
                  status.setVisible(true);
                  if(city.equals("unknown")) city="";
                  if(city.contains("null")) city="";
                  if(city.contains("NULL")) city="";
                  status.setText("    System: "+city+"  "+sys_id_str);
                }
              } catch(Exception e) {
              }

              try {
                int tsbk_ps_i = new Integer(tsbk_ps);

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
            if(st1.contains("Desc:")) {
              st2 = st2.concat(st.nextToken()+" ");
              if(st2.contains(",")) {
                l3.setText(freqval+st2.substring(0,st2.length()-2)+talkgroup);
                break;
              } 
              st2 = st2.concat(st.nextToken()+" ");
              if(st2.contains(",")) { 
                l3.setText(freqval+st2.substring(0,st2.length()-2)+talkgroup);
                break;
              }
              st2 = st2.concat(st.nextToken()+" ");
              if(st2.contains(",")) {
                l3.setText(freqval+st2.substring(0,st2.length()-2)+talkgroup);
                break;
              } 
            }
          }

          /*
          if(console_line.contains("*")) rssim2.setValue(-120,false);
          if(console_line.contains("**")) rssim2.setValue(-110,false);
          if(console_line.contains("***")) rssim2.setValue(-105,false);
          if(console_line.contains("****")) rssim2.setValue(-105,false);
          if(console_line.contains("****")) rssim2.setValue(-100,false);
          if(console_line.contains("*****")) rssim2.setValue(-95,false);
          if(console_line.contains("******")) rssim2.setValue(-90,false);
          if(console_line.contains("*******")) rssim2.setValue(-85,false);
          if(console_line.contains("*******")) rssim2.setValue(-80,false);
          if(console_line.contains("*******")) rssim2.setValue(-50,false);
          */
          
          /*
          if(console_line.contains("\n") && !console_line.contains("_") ) {
            if(console_line.contains("********")) { 
              sq_indicator.setForeground( java.awt.Color.green );
              sq_indicator.setBackground( java.awt.Color.green );
            }
            else if(console_line.contains("*******")) { 
              sq_indicator.setForeground( java.awt.Color.blue );
              sq_indicator.setBackground( java.awt.Color.blue );
            }
            else if(console_line.contains("*")) { 
              sq_indicator.setForeground( java.awt.Color.red );
              sq_indicator.setBackground( java.awt.Color.red );
            }
            else { 
              //sq_indicator.setForeground( java.awt.Color.black );
              //sq_indicator.setBackground( java.awt.Color.black );
            }
          }
          */
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
            else { 
              //sq_indicator.setForeground( java.awt.Color.black );
              //sq_indicator.setBackground( java.awt.Color.black );
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

        /*
        str = str.replace("*"," ");
        //str = str.trim();
        if(str.equals("_")) str="";
        if(str.equals("__")) str="";
        if(str.equals("___")) str="";
        if(str.equals("____")) str="";
        if(str.equals("_____")) str="";
        if(str.equals("______")) str="";
        if(str.equals("_______")) str="";
        if(str.equals("________")) str="";

        int do_append=1;
        /*
        for(int i=0;i<str.length();i++) {
          if(!str.substring(i,i+1).equals(" ") && !str.substring(i,i+1).equals("_")) {
            do_append=1;
            break;
          }
        }
        */


        if(str.length()>0 ) {
          jTextArea1.append(str);
          jTextArea1.setCaretPosition(jTextArea1.getText().length());

          jTextArea1.getCaret().setVisible(true);
          jTextArea1.getCaret().setBlinkRate(250);
        }
      }
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
      if(str.length()==0) {
        status.setVisible(false);
        return;
      }
      status.setVisible(true);
      status.setText("Status: "+str);
      status_timeout=600;
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
        bottom_panel = new javax.swing.JPanel();
        status_panel = new javax.swing.JPanel();
        status = new javax.swing.JLabel();
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
        jLabel5 = new javax.swing.JLabel();
        lineout_vol_slider = new javax.swing.JSlider();
        jLabel6 = new javax.swing.JLabel();
        en_bluetooth_cb = new javax.swing.JCheckBox();
        bt_volume_slider = new javax.swing.JSlider();
        read_config = new javax.swing.JButton();
        allow_unknown_tg_cb = new javax.swing.JCheckBox();
        volume_label = new javax.swing.JLabel();
        btgain_label = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
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
        jLabel22 = new javax.swing.JLabel();
        audiopanel = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        audio_buffer_system = new javax.swing.JRadioButton();
        audio_buffer_user = new javax.swing.JRadioButton();
        enable_mp3 = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        agc_gain = new javax.swing.JSlider();
        agc_level_lb = new javax.swing.JLabel();
        enable_audio = new javax.swing.JCheckBox();
        jLabel8 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        initial_audio_level = new javax.swing.JSlider();
        initial_audio_level_lb = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        audio_insert_zero = new javax.swing.JCheckBox();
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
        restore_tg = new javax.swing.JButton();
        backup_tg = new javax.swing.JButton();
        jPanel23 = new javax.swing.JPanel();
        auto_flash_tg = new javax.swing.JCheckBox();
        disable_encrypted = new javax.swing.JCheckBox();
        jLabel26 = new javax.swing.JLabel();
        consolePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        logpanel = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        log_ta = new javax.swing.JTextArea();
        buttong_config = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jCheckBox5 = new javax.swing.JCheckBox();
        jCheckBox6 = new javax.swing.JCheckBox();
        jCheckBox7 = new javax.swing.JCheckBox();
        button_single_follow_tg = new javax.swing.JRadioButton();
        button_single_next_roaming = new javax.swing.JRadioButton();
        button_write_config = new javax.swing.JButton();
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
        record_to_mp3 = new javax.swing.JToggleButton();
        release_date = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        bottom_panel.setLayout(new javax.swing.BoxLayout(bottom_panel, javax.swing.BoxLayout.Y_AXIS));

        status_panel.setBackground(new java.awt.Color(0, 0, 0));
        status_panel.setMinimumSize(new java.awt.Dimension(99, 33));
        status_panel.setPreferredSize(new java.awt.Dimension(1004, 33));
        status_panel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 7));

        status.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        status.setForeground(new java.awt.Color(255, 255, 255));
        status.setText("Status: Idle");
        status.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        status_panel.add(status);

        bottom_panel.add(status_panel);

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

        getContentPane().add(bottom_panel, java.awt.BorderLayout.PAGE_END);

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

        jLabel6.setText("BlueTooth Volume");
        p25rxconfigpanel.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 210, -1, -1));

        en_bluetooth_cb.setSelected(true);
        en_bluetooth_cb.setText("Enable Bluetooth On Power-Up");
        en_bluetooth_cb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en_bluetooth_cbActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(en_bluetooth_cb, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 170, -1, -1));

        bt_volume_slider.setPaintLabels(true);
        bt_volume_slider.setPaintTicks(true);
        bt_volume_slider.setToolTipText("This option control the audio level sent wirelessly to a remote bluetooth audio device.");
        bt_volume_slider.setValue(100);
        bt_volume_slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                bt_volume_sliderStateChanged(evt);
            }
        });
        p25rxconfigpanel.add(bt_volume_slider, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 210, -1, -1));

        read_config.setText("Read Config From P25RX");
        read_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                read_configActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(read_config, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 400, -1, -1));

        allow_unknown_tg_cb.setSelected(true);
        allow_unknown_tg_cb.setText("Allow Unknown Talkgroups");
        p25rxconfigpanel.add(allow_unknown_tg_cb, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 290, -1, -1));

        volume_label.setText("1.0");
        p25rxconfigpanel.add(volume_label, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 130, -1, -1));

        btgain_label.setText("1.0");
        p25rxconfigpanel.add(btgain_label, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 210, -1, -1));

        jLabel9.setText("Default: 0.8");
        p25rxconfigpanel.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 130, -1, -1));

        jLabel10.setText("Default 0.8");
        p25rxconfigpanel.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 210, -1, -1));

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
        p25rxconfigpanel.add(frequency_tf1, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 40, -1, 30));

        jLabel7.setText("MHz");
        p25rxconfigpanel.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 50, -1, -1));

        jLabel2.setText("<primary>");
        p25rxconfigpanel.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 50, -1, -1));

        jLabel25.setText("Shuffle Control Channels After");
        no_voice_panel.add(jLabel25);

        no_voice_secs.setColumns(5);
        no_voice_secs.setText("180");
        no_voice_panel.add(no_voice_secs);

        jLabel24.setText("Seconds Of Silence ( 0 to disable)");
        no_voice_panel.add(jLabel24);

        p25rxconfigpanel.add(no_voice_panel, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 360, 550, 40));

        roaming.setSelected(true);
        roaming.setText("Enable Roaming On Loss Of Signal");
        roaming.setToolTipText("This option is intended for mobile operation.  Disable if you itend to listen to a single local control channel.");
        roaming.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roamingActionPerformed(evt);
            }
        });
        p25rxconfigpanel.add(roaming, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 370, -1, -1));

        jLabel22.setText("Control Channel Frequency");
        p25rxconfigpanel.add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 50, -1, -1));

        jTabbedPane1.addTab("P25RX Configuration", p25rxconfigpanel);

        audiopanel.setLayout(new java.awt.BorderLayout());

        jPanel11.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        buttonGroup2.add(audio_buffer_system);
        audio_buffer_system.setSelected(true);
        audio_buffer_system.setText("Let System Set Audio Buffer Size");
        audio_buffer_system.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audio_buffer_systemActionPerformed(evt);
            }
        });
        jPanel11.add(audio_buffer_system, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 60, -1, -1));

        buttonGroup2.add(audio_buffer_user);
        audio_buffer_user.setText("Use Large Audio Buffer Size");
        audio_buffer_user.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audio_buffer_userActionPerformed(evt);
            }
        });
        jPanel11.add(audio_buffer_user, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 90, -1, -1));

        enable_mp3.setSelected(true);
        enable_mp3.setText("Enable .mp3 audio file generation");
        enable_mp3.setToolTipText("This option will generate mp3 files in the p25rx directory located in the user home directory.  ~/p25rx on Linux and Documents/p25rx on Windows.");
        enable_mp3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_mp3ActionPerformed(evt);
            }
        });
        jPanel11.add(enable_mp3, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 210, -1, -1));

        jLabel3.setText("PC Audio AGC Target Level (volume)");
        jPanel11.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 260, -1, -1));

        agc_gain.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                agc_gainStateChanged(evt);
            }
        });
        jPanel11.add(agc_gain, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 260, -1, -1));

        agc_level_lb.setText("Default: 50%");
        jPanel11.add(agc_level_lb, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 260, -1, -1));

        enable_audio.setSelected(true);
        enable_audio.setText("Enable PC Audio Output (PC Speakers)");
        enable_audio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_audioActionPerformed(evt);
            }
        });
        jPanel11.add(enable_audio, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 360, -1, -1));

        jLabel8.setText("This option may give better audio performance on some systems");
        jPanel11.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 90, -1, 20));

        jLabel11.setText("If you hear clipping, try setting this lower. (default 75)");
        jPanel11.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 310, -1, -1));

        jLabel12.setText("Initial Master Volume Level");
        jPanel11.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 310, -1, -1));

        initial_audio_level.setValue(85);
        initial_audio_level.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                initial_audio_levelStateChanged(evt);
            }
        });
        jPanel11.add(initial_audio_level, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 310, -1, -1));

        initial_audio_level_lb.setText("Val 85");
        jPanel11.add(initial_audio_level_lb, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 310, -1, -1));

        jLabel13.setText("If you hear clipping, try setting this lower. (default 85)");
        jPanel11.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(680, 260, -1, -1));

        audio_insert_zero.setSelected(true);
        audio_insert_zero.setText("Insert Zeros At Start Of Buffer    (might prevent java audio glitches)");
        audio_insert_zero.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audio_insert_zeroActionPerformed(evt);
            }
        });
        jPanel11.add(audio_insert_zero, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 130, -1, -1));

        audiopanel.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel13.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        audiopanel.add(jPanel13, java.awt.BorderLayout.PAGE_START);

        jTabbedPane1.addTab("PC Audio", audiopanel);

        freqdb_panel.setLayout(new java.awt.BorderLayout());

        freq_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "LICENSE", "GRANTEE", "ENTITY(GOV,BUS)", "FREQ", "TEST", "RESULTS", "INFLASH", "SRV_CLASS", "CITY", "STATE", "EMISSION"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
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

        jTabbedPane1.addTab("Search DB For Nearby Control Channels", freqdb_panel);

        talkgroup_panel.setLayout(new java.awt.BorderLayout());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                { new Boolean(false), null, null, null, "", ""},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Enabled", "SYS_ID", "Priority", "TGRP", "AlphaTag", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.setDoubleBuffered(true);
        jTable1.setEditingColumn(1);
        jTable1.setEditingRow(1);
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

        restore_tg.setText("Restore From Backup");
        restore_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restore_tgActionPerformed(evt);
            }
        });
        jPanel3.add(restore_tg);

        backup_tg.setText("Make Backup");
        backup_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backup_tgActionPerformed(evt);
            }
        });
        jPanel3.add(backup_tg);

        jPanel22.add(jPanel3);

        jPanel23.setBackground(new java.awt.Color(0, 0, 0));
        jPanel23.setForeground(new java.awt.Color(255, 255, 255));

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

        jLabel26.setForeground(new java.awt.Color(255, 255, 255));
        jLabel26.setText("   Use DEL key to delete selected records.");
        jPanel23.add(jLabel26);

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
        jPanel21.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 70, -1, -1));

        jLabel19.setText("Double Click");
        jPanel21.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 200, -1, -1));

        jLabel20.setText("Triple Click");
        jPanel21.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 290, -1, -1));

        jLabel21.setText("Quad Click");
        jPanel21.add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 390, -1, -1));

        jCheckBox5.setSelected(true);
        jCheckBox5.setText("Toggle Status LEDs On/Off");
        jCheckBox5.setEnabled(false);
        jPanel21.add(jCheckBox5, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 330, -1, -1));

        jCheckBox6.setSelected(true);
        jCheckBox6.setText("Bluetooth Firmware Bootloader");
        jCheckBox6.setEnabled(false);
        jPanel21.add(jCheckBox6, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 430, -1, -1));

        jCheckBox7.setSelected(true);
        jCheckBox7.setText("Bluetooth Pairing");
        jCheckBox7.setEnabled(false);
        jCheckBox7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox7ActionPerformed(evt);
            }
        });
        jPanel21.add(jCheckBox7, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 240, -1, -1));

        buttonGroup3.add(button_single_follow_tg);
        button_single_follow_tg.setSelected(true);
        button_single_follow_tg.setText("Toggle Follow Talk Group On/Off");
        button_single_follow_tg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_single_follow_tgActionPerformed(evt);
            }
        });
        jPanel21.add(button_single_follow_tg, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 110, -1, -1));

        buttonGroup3.add(button_single_next_roaming);
        button_single_next_roaming.setText("Next Channel In Roaming List");
        jPanel21.add(button_single_next_roaming, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 150, -1, -1));

        button_write_config.setText("Write Config");
        button_write_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_write_configActionPerformed(evt);
            }
        });
        jPanel21.add(button_write_config, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 460, -1, -1));

        buttong_config.add(jPanel21, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Button CFG", buttong_config);

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
        minimize.setText("MONITOR");
        minimize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minimizeActionPerformed(evt);
            }
        });
        jPanel4.add(minimize);

        record_to_mp3.setText("REC");
        record_to_mp3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                record_to_mp3ActionPerformed(evt);
            }
        });
        jPanel4.add(record_to_mp3);

        release_date.setText("Release: ");
        jPanel4.add(release_date);

        logo_panel.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 0, 360, -1));

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

      if(skip_bytes==0 && do_update_firmware==0) {
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
          keydata[keyindex++] = c;
          jTextArea1.append( new Character(c).toString() );
        }
      }
    }//GEN-LAST:event_jTextArea1KeyTyped

    private void jTextArea1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextArea1FocusGained
      if(jTabbedPane1.getSelectedIndex()==4) jTextArea1.requestFocus();
    }//GEN-LAST:event_jTextArea1FocusGained

    private void consolePanelFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_consolePanelFocusGained
      if(jTabbedPane1.getSelectedIndex()==4) jTextArea1.requestFocus();
    }//GEN-LAST:event_consolePanelFocusGained

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
      if(jTabbedPane1.getSelectedIndex()==4) jTextArea1.requestFocus();
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
      //if(minimize.isSelected()) {
       // setSize(1054,192);
      //}
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
        if(isWindows) {
          setSize(1020,200);
        }
        else  {
          setSize(1020,185);
        }
      }
      else {
        setSize(1054,720);
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

      current_sys_id = 0;
      current_wacn_id = 0; 
      wacn.setText("");
      sysid.setText("");
      nac.setText("");
    }//GEN-LAST:event_write_configActionPerformed

    private void backup_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backup_tgActionPerformed
      did_tg_backup=0;
      do_read_talkgroups=1;
      do_talkgroup_backup=1;
    }//GEN-LAST:event_backup_tgActionPerformed

    private void lineout_vol_sliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lineout_vol_sliderStateChanged
      volume_label.setText( String.format( "%3.2f", (float) lineout_vol_slider.getValue() / 100.0f ) );
    }//GEN-LAST:event_lineout_vol_sliderStateChanged

    private void bt_volume_sliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bt_volume_sliderStateChanged
      btgain_label.setText( String.format( "%3.2f", (float) bt_volume_slider.getValue() / 100.0f ) );
    }//GEN-LAST:event_bt_volume_sliderStateChanged

    private void disable_table_rowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disable_table_rowsActionPerformed
      int[] rows = jTable1.getSelectedRows();
      if(rows.length>0) {
        for(int i=0;i<rows.length;i++) {
          jTable1.getModel().setValueAt(false,rows[i],0);
          System.out.println("row "+i);
        } 
      }
    }//GEN-LAST:event_disable_table_rowsActionPerformed

    private void enable_table_rowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_table_rowsActionPerformed
      int[] rows = jTable1.getSelectedRows();
      if(rows.length>0) {
        for(int i=0;i<rows.length;i++) {
          jTable1.getModel().setValueAt(true,rows[i],0);
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
      if(jTabbedPane1.getSelectedIndex()==4) {
        //System.out.println("evt tab");
        jTextArea1.requestFocus();
      }
    }//GEN-LAST:event_jTabbedPane1MouseClicked

    private void agc_gainStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_agc_gainStateChanged
      do_agc_update=1;
    }//GEN-LAST:event_agc_gainStateChanged

    private void audio_buffer_systemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audio_buffer_systemActionPerformed
      //if(aud!=null) aud.closeAll();
      //aud = new audio(parent);
      prefs.putInt("audio_buffer_system",1);
      //if(aud!=null) aud.setAgcGain( (0.01f + (float) agc_gain.getValue()) / 100.0f );
      JOptionPane.showMessageDialog(parent, "Change will take place on next software start-up.");
    }//GEN-LAST:event_audio_buffer_systemActionPerformed

    private void audio_buffer_userActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audio_buffer_userActionPerformed
      //if(aud!=null) aud.closeAll();
      //aud = new audio(parent);
      prefs.putInt("audio_buffer_system",0);
      //if(aud!=null) aud.setAgcGain( ( 0.01f + (float) agc_gain.getValue()) / 100.0f );
      JOptionPane.showMessageDialog(parent, "Change will take place on next software start-up.");
    }//GEN-LAST:event_audio_buffer_userActionPerformed

    private void enable_mp3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_mp3ActionPerformed
      prefs.putBoolean("enable_mp3", enable_mp3.isSelected());
    }//GEN-LAST:event_enable_mp3ActionPerformed

    private void enable_audioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_audioActionPerformed
      prefs.putBoolean("enable_audio", enable_audio.isSelected());
    }//GEN-LAST:event_enable_audioActionPerformed

    private void initial_audio_levelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_initial_audio_levelStateChanged
        prefs.putInt("initial_audio_level", initial_audio_level.getValue() );
        initial_audio_level_lb.setText( "Val "+initial_audio_level.getValue()+"%");
        //if(aud!=null) aud.closeAll();
        //aud = new audio(parent);
        if(aud!=null) aud.updateLevels();
    }//GEN-LAST:event_initial_audio_levelStateChanged

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

    private void jCheckBox7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBox7ActionPerformed

    private void button_single_follow_tgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_single_follow_tgActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_button_single_follow_tgActionPerformed

    private void button_write_configActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_write_configActionPerformed
      do_read_config=1;
      do_write_config=1;

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

    private void audio_insert_zeroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audio_insert_zeroActionPerformed
      prefs.putBoolean("audio_insert_zero", audio_insert_zero.isSelected());
    }//GEN-LAST:event_audio_insert_zeroActionPerformed

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
void toggle_recording(Boolean isrec) {

  boolean is_recording=!isrec;

  String recnow="";
  if(is_recording) recnow="1";  //toggle
  if(!is_recording) recnow="0"; //toggle

  //enable binary voice output for mp3 recording
  for(int i=0;i<99;i++) {
    serial_port.writeBytes( new String("en_voice_send "+recnow+"\r\n").getBytes(), 17, 0);
    try {
      Thread.sleep(50);
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
float[] columnWidthPercentage = {.075f, .075f, .075f, .075f, .25f, .45f };
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
    public javax.swing.JSlider agc_gain;
    private javax.swing.JLabel agc_level_lb;
    public javax.swing.JCheckBox allow_unknown_tg_cb;
    public javax.swing.JButton append_cc;
    public javax.swing.JRadioButton audio_buffer_system;
    public javax.swing.JRadioButton audio_buffer_user;
    public static javax.swing.JCheckBox audio_insert_zero;
    private javax.swing.JPanel audiopanel;
    public javax.swing.JCheckBox auto_flash_tg;
    public javax.swing.JButton backup_roam;
    private javax.swing.JButton backup_tg;
    public javax.swing.JTextField bluetooth_reset;
    private javax.swing.JPanel bottom_panel;
    private javax.swing.JToggleButton bt_indicator;
    private javax.swing.JLabel bt_lb;
    public javax.swing.JSlider bt_volume_slider;
    public javax.swing.JLabel btgain_label;
    private javax.swing.JLabel btreset1;
    private javax.swing.JLabel btreset2;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    public javax.swing.JRadioButton button_single_follow_tg;
    public javax.swing.JRadioButton button_single_next_roaming;
    private javax.swing.JButton button_write_config;
    private javax.swing.JPanel buttong_config;
    private javax.swing.JButton check_firmware;
    private javax.swing.JTextField city;
    private javax.swing.JPanel consolePanel;
    private javax.swing.JPanel desc_panel;
    public javax.swing.JCheckBox disable_encrypted;
    private javax.swing.JButton disable_table_rows;
    private javax.swing.JButton disconnect;
    private javax.swing.JButton discover;
    public javax.swing.JCheckBox en_bluetooth_cb;
    public javax.swing.JCheckBox enable_audio;
    public javax.swing.JCheckBox enable_leds;
    public javax.swing.JCheckBox enable_mp3;
    private javax.swing.JButton enable_table_rows;
    public javax.swing.JButton erase_roaming;
    public javax.swing.JLabel freq;
    private javax.swing.JButton freq_search;
    private javax.swing.JButton freq_search2;
    public javax.swing.JTable freq_table;
    private javax.swing.JPanel freqdb_panel;
    public javax.swing.JTextField frequency_tf1;
    public javax.swing.JLabel fw_installed;
    public javax.swing.JLabel fw_ver;
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
    public javax.swing.JSlider initial_audio_level;
    private javax.swing.JLabel initial_audio_level_lb;
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JCheckBox jCheckBox7;
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
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
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
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane7;
    public javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTabbedPane jTabbedPane1;
    public javax.swing.JTable jTable1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JPanel level_panel;
    public javax.swing.JSlider lineout_vol_slider;
    private javax.swing.JTextArea log_ta;
    private javax.swing.JPanel logo_panel;
    private javax.swing.JPanel logpanel;
    public javax.swing.JLabel macid;
    private javax.swing.JPanel meter_panel;
    private javax.swing.JToggleButton minimize;
    private javax.swing.JLabel nac;
    public javax.swing.JPanel no_voice_panel;
    public javax.swing.JTextField no_voice_secs;
    private javax.swing.JPanel p25rxconfigpanel;
    private javax.swing.JProgressBar progbar;
    private javax.swing.JLabel progress_label;
    private javax.swing.JButton read_config;
    private javax.swing.JButton read_tg;
    private javax.swing.JToggleButton record_to_mp3;
    private javax.swing.JLabel release_date;
    public javax.swing.JButton restore_roam;
    private javax.swing.JButton restore_tg;
    public javax.swing.JLabel rfid;
    public javax.swing.JCheckBox roaming;
    private javax.swing.JTextField search_radius;
    private javax.swing.JButton send_tg;
    public javax.swing.JLabel siteid;
    private javax.swing.JToggleButton sq_indicator;
    private javax.swing.JLabel sq_lb;
    private javax.swing.JTextField state;
    private javax.swing.JLabel status;
    private javax.swing.JPanel status_panel;
    private javax.swing.JLabel sysid;
    private javax.swing.JPanel talkgroup_panel;
    public javax.swing.JButton testfreqs;
    private javax.swing.JToggleButton tg_indicator;
    private javax.swing.JLabel tg_lb;
    public javax.swing.JButton use_freq_primary;
    public javax.swing.JLabel volume_label;
    private javax.swing.JLabel wacn;
    public javax.swing.JButton write_cc;
    private javax.swing.JButton write_config;
    private javax.swing.JTextField zipcode;
    // End of variables declaration//GEN-END:variables
}
