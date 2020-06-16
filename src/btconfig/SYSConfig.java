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

import java.util.*;
import java.io.*;
import java.nio.*;
import com.fazecast.jSerialComm.*;
import javax.swing.filechooser.*;
import javax.swing.*;
import javax.swing.*;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
class SYSConfig
{

java.util.Timer utimer;
BTFrame parent;
SerialPort serial_port;
java.text.SimpleDateFormat formatter_date;

int did_warning=0;
int did_crc_reset=0;


//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
Boolean is_valid_freq(double freq) {
  int band = 0;

  if(freq >= 130.0 && freq <= 245.0) band= 1;  //band 1
  if(freq >= 256.0 && freq <= 365.0) band= 2;  //band 2
  if(freq >= 380.0 && freq <= 490.0) band= 3;  //band 3

  if(freq >= 763.0 && freq <= 824.0) band= 4;  //band 4
  if(freq >= 849.0 && freq <= 869.0) band= 5;  //band 5
  if(freq >= 894.0 && freq <= 960.0) band= 6;  //band 6


  if(band!=0) return true;

  return false; 
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void read_sysconfig(BTFrame parent, SerialPort serial_port)
{
  this.serial_port = serial_port;

  byte[] image_buffer = new byte[128 * 1024 * 6];

  for( int i=0; i< 128 * 1024 *6; i++) {
    image_buffer[i] = (byte) 0xff;
  }

  int config_length = 0;

  try {

    int state = -1; 

    while(true) {


        if(state==-1) {
          if(serial_port!=null && serial_port.isOpen()) {
            state=0;
          } 
        }
        else {
          parent.setProgress(-1);
          parent.setStatus("\r\ncouldn't find device");
          return;
        }


      //get the number of recs
        if(state==0) {

          parent.setProgress(5);
          parent.setStatus("Reading sys_config from P25RX device..."); 

          int offset = 0;
          //while(offset<config_length) {


          int nrecs=0;
          int timeout=0;
          while(true) {

              if(timeout++>10) break;

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("6", 10) ); //read cfg flash
              bb.putInt( (int) new Long((long) 0x08100000 + offset).longValue() );  //address to return
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len  to return



              byte[] input_buffer = new byte[48];
              int rlen=0;
              while(rlen!=48) {
                serial_port.writeBytes( out_buffer, 48, 0); //16 + data len=0

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      Thread.sleep(1);
                      if(count++>50) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }

                rlen=serial_port.readBytes( input_buffer, 48 );
                if(rlen==48) {
                  break;
                }
                //else {
                 // System.out.println("rlen<>48");
                //}
              }

              ByteBuffer bb2 = ByteBuffer.wrap(input_buffer);
              bb2.order(ByteOrder.LITTLE_ENDIAN);


              if( bb2.getInt()== 0xd35467A6) {//magic
                bb2.getInt();  //op
                bb2.getInt();  //address
                bb2.getInt();  //len
                nrecs = bb2.getInt();
                if(nrecs>0 && nrecs<1280000) break;
              }
              else {
                //flush the input buffers
                byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
              }
          }

          if(nrecs>0) {
            parent.setStatus("\r\nCompleted reading sys_config. nrecs: "+nrecs);
          }
          else {
            parent.setStatus("\r\nNo talkgroup records found.");
          }
          parent.setProgress(10);



          offset = 0; //skip the nrecs int

          while(true) {

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("6", 10) ); //read cfg flash
              bb.putInt( (int) new Long((long) 0x08100000 + offset).longValue() );  //address to return
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len  to return


              byte[] input_buffer = new byte[48];
              int rlen=0;
              while(rlen!=48) {
                serial_port.writeBytes( out_buffer, 48, 0); //16 + data len=0

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      Thread.sleep(1);
                      if(count++>50) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }

                rlen=serial_port.readBytes( input_buffer, 48 );
                if(rlen==48) break;
              }

              ByteBuffer bb2 = ByteBuffer.wrap(input_buffer);
              bb2.order(ByteOrder.LITTLE_ENDIAN);


              if( bb2.getInt()== 0xd35467A6) {//magic
                bb2.getInt();  //op
                int raddress = (bb2.getInt()-0x08100000) ;  //address
                bb2.getInt();  //len

                if(raddress>=0) {
                  for(int i=0;i<32;i++) {
                    image_buffer[i+raddress] = bb2.get();
                  }

                  offset+=32;
                  if(offset >= 552+32) { //finished?

                    ByteBuffer bb3 = ByteBuffer.wrap(image_buffer);
                    bb3.order(ByteOrder.LITTLE_ENDIAN);
                    int crc = crc32.crc32_range(image_buffer, 548);
                    parent.system_crc=crc;
                    System.out.println(String.format("config crc 0x%08x", crc));

                    int config_crc = bb3.getInt(548);

                      if(crc==0) {
                        parent.do_update_firmware=1;
                        parent.do_update_firmware2=1;
                        parent.do_read_talkgroups=0;
                        parent.do_read_config=0;
                          int result2 = JOptionPane.showConfirmDialog(parent, "Would you like to erase talk group and roaming frequency flash?", "Erase Config Areas?", JOptionPane.YES_NO_OPTION);
                          if(result2==JOptionPane.YES_OPTION) {
                            String cmd = "clear_configs\r\n";
                            serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                            Thread.sleep(3000);
                          }
                        return;
                      }


                    if(crc == config_crc) {

                      parent.system_crc=crc;

                      parent.setStatus("sys_config crc ok."); 

                      System.out.println( String.format("\r\nfrequency: %3.6f",bb3.getDouble()) );
                      System.out.println( String.format("\r\nis_control: %d",bb3.getInt(36)) );
                      System.out.println( String.format("\r\nvolume: %3.2f",bb3.getFloat(12)) );
                      System.out.println( String.format("\r\nbluetooth: %d",bb3.getInt(88)) );
                      System.out.println( String.format("\r\nbluetooth reset: %d",bb3.getInt(260)/5) );
                      System.out.println( String.format("\r\nbt_gain: %3.2f",bb3.getFloat(176)) );
                      System.out.println( String.format("\r\nled_mode: %d",bb3.getInt(196)) );
                      System.out.println( String.format("\r\nallow unknown tg: %d",bb3.getInt(130)) );
                      System.out.println( String.format("\r\nenable_roaming %d",bb3.getInt(68)) );
                      System.out.println( String.format("\r\nno_voice_roam_sec",bb3.getInt(280)) );

                      System.out.println( String.format("\r\nconfig verson: %d",bb3.getInt(544)) );
                      System.out.println( String.format("\r\nconfig crc: 0x%08x",config_crc) );
                      String fw_ver = "";
                      byte[] fw_version = new byte[12];
                      for(int c=0;c<12;c++) {
                        fw_version[c] = (byte) bb3.get(264+c);
                      } 

                      fw_ver = new String( fw_version );
                      parent.fw_installed.setText("   Installed FW: "+fw_ver);

                      if(parent.fw_ver.getText().contains(fw_ver)) {
                        parent.fw_ver.setVisible(false);
                        parent.fw_installed.setVisible(false);
                      }
                      else {
                        parent.fw_ver.setVisible(true);
                        parent.fw_installed.setVisible(true);
                      }

                      if( did_warning==0 && !parent.fw_ver.getText().contains(fw_ver) ) {
                        int result = JOptionPane.showConfirmDialog(parent, "Proceed With Firmware Update?  Cancel To Exit Application.", "Update Firmware?", JOptionPane.OK_CANCEL_OPTION);
                        if(result==JOptionPane.OK_OPTION) {
                          parent.do_update_firmware2=1;
                          did_warning=1;
                        }
                        else {
                          System.exit(0);
                        }
                      }
                      else if( did_warning==1 && !parent.fw_ver.getText().contains(fw_ver) ) {
                          parent.do_update_firmware2=1;
                      }



                      parent.do_read_config=0;

                      if( parent.do_write_config==0) {
                        try {


                          float vol = bb3.getFloat(12);
                          vol *= 100.0f;
                          parent.lineout_vol_slider.setValue( (int) vol );
                          parent.volume_label.setText( String.format("%3.2f", vol/100.0f) );

                          Boolean b = true;
                          if(bb3.getInt(88)==1) b=true;
                              else b=false;
                          parent.en_bluetooth_cb.setSelected(b); 


                          vol = bb3.getFloat(176);
                          vol *= 100.0f;
                          parent.bt_volume_slider.setValue( (int) vol );
                          parent.btgain_label.setText( String.format("%3.2f", vol/100.0f) );

                          int bt_reset = bb3.getInt(260)/60;
                          if(bt_reset>0 && bt_reset<5) bt_reset=10;
                          parent.bluetooth_reset.setText( String.format("%d", bt_reset) );


                          if(bb3.getInt(130)==1) b=true;
                              else b=false;
                          parent.allow_unknown_tg_cb.setSelected(b); 

                          if(bb3.getInt(36)==1) b=true;
                              else b=false;

                          if(bb3.getInt(196)==1) b=true;
                              else b=false;
                          parent.enable_leds.setSelected(b); 

                          if(bb3.getInt(68)==1) b=true;
                              else b=false;
                          parent.roaming.setSelected(b); 
                          if(b) {
                            parent.no_voice_panel.setVisible(true);
                          }
                          else {
                            parent.no_voice_panel.setVisible(false);
                          }


                          //if( parent.is_cc.isSelected() ) {
                            parent.frequency_tf1.setText( String.format("%3.8f", bb3.getDouble(0)) );
                            parent.freq.setText( "Freq: "+String.format("%3.8f", bb3.getDouble(0)) );
                          //}
                          //else {
                           // parent.vfrequency_tf.setText( String.format("%3.8f", bb3.getDouble(0)) );
                            //parent.freq.setText( "Freq: "+String.format("%3.8f", bb3.getDouble(0)) );
                          //}

                          int no_voice_secs = bb3.getInt(280);
                          parent.no_voice_secs.setText( String.format("%d", no_voice_secs) );



                        } catch(Exception e) {
                          e.printStackTrace();
                        }

                      }
                      else {
                        try {

                          String cmd = ""; 
                          parent.setStatus("writing configuration to flash..."); 

                          /*
                          if( parent.roaming.isSelected() ) {
                            byte[] result=new byte[64];
                            cmd = "save_alt_cc\r\n";
                            serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                            Thread.sleep(3000);
                            rlen=serial_port.readBytes( result, 64);
                          }
                          */


                          String freq_to_use="";
                          double freq_d = 859.9625;

                          parent.freq.setText( "Freq: "+parent.frequency_tf1.getText().trim() );
                          freq_to_use=parent.frequency_tf1.getText().trim();

                          try {
                            freq_d = new Double(freq_to_use).doubleValue();
                          } catch(Exception e) {
                            freq_to_use="859.9625";
                          }

                          cmd = "freq "+freq_to_use+"\r\n";  

                          if( !is_valid_freq(freq_d) ) {
                            JOptionPane.showMessageDialog(parent, "Invalid Frequency "+freq_to_use);
                            freq_to_use = "859.9625";
                            cmd = "freq "+freq_to_use+"\r\n";  
                          }

                          byte[] result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          cmd = "no_voice_roam_sec "+parent.no_voice_secs.getText()+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          //if( ! new String(result).trim().contains("frequency: "+parent.frequency_tf.getText().trim()) ) return;
                          cmd = "vol "+(float) parent.lineout_vol_slider.getValue()/100.0f+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          cmd = "bt_gain "+(float) parent.bt_volume_slider.getValue()/100.0f+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          //cmd = "bt_reset "+parent.bluetooth_reset.getText()+"\r\n";
                          cmd = "bt_reset 0"+"\r\n";  //always disabled for now
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          Boolean b = parent.en_bluetooth_cb.isSelected();
                          if(b) cmd = "bluetooth 1\r\n";
                            else cmd = "bluetooth 0\r\n"; 

                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          b = parent.allow_unknown_tg_cb.isSelected();
                          if(b) cmd = "en_unknown_tg 1\r\n";
                            else cmd = "en_unknown_tg 0\r\n"; 

                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          b = parent.roaming.isSelected();
                          if(b) cmd = "roaming 1\r\n";
                            else cmd = "roaming 0\r\n"; 

                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          b = parent.enable_leds.isSelected();
                          if(b) cmd = "led_mode 1\r\n";
                            else cmd = "led_mode 0\r\n"; 
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          //do this one last
                          cmd = "is_control 1\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);


                          cmd = "save\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(1500);

                          parent.do_write_config=0;
                          parent.do_read_config=1;

                          parent.setStatus("sys_config update ok."); 
                        } catch(Exception e) {
                          e.printStackTrace();
                        }
                      }


                    }
                    else {
                      parent.setStatus("sys_config crc not ok."); 
                      System.out.println(String.format("sys_config crc NOT OK. Resetting device.  0x%08x, 0x%08x", crc, config_crc));
                        //parent.is_connected=0;
                        //parent.do_connect=1;
                        //Thread.sleep(1000);
                        //return;

                      //if(did_crc_reset==0) {
                      //  did_crc_reset=1;

                        parent.setStatus("\r\nresetting device");
                        String cmd = "system_reset\r\n";
                        serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

                        try {
                          Thread.sleep(3000);
                        } catch(Exception e) {
                        }

                      //}

                      parent.is_connected=0;
                      parent.do_connect=1;
                    }


                    parent.setProgress(-1); 

                    return; 
                  }

                  //parent.setStatus("read "+offset+" bytes");
                  parent.setStatus("read sys_config."); 
                  parent.setProgress( (int) ((float)offset/552.0f * 100.0) );
                }
              }
              else {
                //flush buffers
                byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
              }
          }


        }

    } //while(true) 
  } catch (Exception e) {
    e.printStackTrace();
  }
}


}
