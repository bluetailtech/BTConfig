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

  public final int DMR_CC1=(1<<0);
  public final int DMR_CC2=(1<<1);
  public final int DMR_CC3=(1<<2);
  public final int DMR_CC4=(1<<3);
  public final int DMR_CC5=(1<<4);
  public final int DMR_CC6=(1<<5);
  public final int DMR_CC7=(1<<6);
  public final int DMR_CC8=(1<<7);
  public final int DMR_CC9=(1<<8);
  public final int DMR_CC10=(1<<9);
  public final int DMR_CC11=(1<<10);
  public final int DMR_CC12=(1<<11);
  public final int DMR_CC13=(1<<12);
  public final int DMR_CC14=(1<<13);
  public final int DMR_CC15=(1<<14);
  public final int DMR_ISCC=(1<<15);
  public final int DMR_SLOT1=(1<<16);
  public final int DMR_SLOT2=(1<<17);

java.util.Timer utimer;
BTFrame parent;
SerialPort serial_port;
java.text.SimpleDateFormat formatter_date;

int did_warning=0;
int did_crc_reset=0;
int prev_op_mode=-1;



///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
void do_usb_watchdog(SerialPort sp) {

  try {
    byte[] out_buffer = new byte[16+32]; //size of bl_op
    ByteBuffer bb = ByteBuffer.wrap(out_buffer);
    bb.order(ByteOrder.LITTLE_ENDIAN);

    bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
    bb.putInt( (int) Long.parseLong("9", 10) ); //usb watchdog reset
    bb.putInt( (int) new Long((long) 0x00000000 ).longValue() );  //address to return
    bb.putInt( (int) Long.parseLong("0", 10) );  //data len  to return

    if(sp!=null) sp.writeBytes( out_buffer, 48); //16 + data len=0

  } catch(Exception e) {
    e.printStackTrace();
  }
}


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


          byte[] bresult=new byte[64];
          //stop following
          String cmd = "f 0"+"\r\n";
          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
          Thread.sleep(20);
          int rlen=serial_port.readBytes( bresult, 64);
          System.out.println("bresult: "+new String(bresult) );
          Thread.sleep(10);


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
              rlen=0;
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
              rlen=0;
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
                          //int result2 = JOptionPane.showConfirmDialog(parent, "Would you like to erase talk group and roaming frequency flash?", "Erase Config Areas?", JOptionPane.YES_NO_OPTION);
                          //if(result2==JOptionPane.YES_OPTION) {
                           // String cmd = "clear_configs\r\n";
                            //serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                            //Thread.sleep(3000);
                          //}
                            Thread.sleep(3000);
                        return;
                      }


                    if(crc == config_crc) {
                        parent.do_read_talkgroups=1;
                        parent.do_read_config=1;

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

                          double reffreq = bb3.getDouble(112);
                          parent.ref_freq.setText( String.format("%5.0f", reffreq) );


                          int duid_enh = bb3.getInt(156);
                          int freq_correct_on_voice = bb3.getInt(160);
                          int add_tdu_silence = bb3.getInt(164);

                          if(duid_enh==1) parent.duid_enh.setSelected(true);
                            else parent.duid_enh.setSelected(false);

                          if(freq_correct_on_voice==1) parent.freq_correct_on_voice.setSelected(true);
                            else parent.freq_correct_on_voice.setSelected(false);

                          if(add_tdu_silence==1) parent.add_tdu_silence.setSelected(true);
                            else parent.add_tdu_silence.setSelected(false);


                          int is_wacn_en=0;
                          is_wacn_en = bb3.getInt(184);

                          if(is_wacn_en==1) parent.wacn_en.setSelected(true);
                              else parent.wacn_en.setSelected(false);

                          int en_usb_wdog = bb3.getInt(208);
                          if( en_usb_wdog==0 ) parent.en_usb_wdog.setSelected(false);
                            else parent.en_usb_wdog.setSelected(true);


                          int iscontrol = bb3.getInt(36);
                          int is_analog = bb3.getInt(52);

                          if( iscontrol==0 ) parent.conventionalchannel.setSelected(true);
                            else parent.controlchannel.setSelected(true);

                          if(parent.conventionalchannel.isSelected()) parent.freq_label.setText("Conventional Channel Frequency");
                          if(parent.controlchannel.isSelected()) parent.freq_label.setText("Control Channel Frequency");

                          int op_mode = bb3.getInt(516);

                          parent.is_dmr_mode=0;
                          if(op_mode==2) parent.is_dmr_mode=1;

                          if(op_mode==3) parent.freq_label.setText("FM NB Frequency");

                          parent.op_mode.setSelectedIndex( op_mode-1 );

                          prev_op_mode = op_mode-1;

                          int dmr_config = bb3.getInt(512);

                          if( (dmr_config & DMR_CC1) > 0 ) parent.dmr_cc_en1.setSelected(true);
                            else parent.dmr_cc_en1.setSelected(false);
                          if( (dmr_config & DMR_CC2) > 0 ) parent.dmr_cc_en2.setSelected(true);
                            else parent.dmr_cc_en2.setSelected(false);
                          if( (dmr_config & DMR_CC3) > 0 ) parent.dmr_cc_en3.setSelected(true);
                            else parent.dmr_cc_en3.setSelected(false);
                          if( (dmr_config & DMR_CC4) > 0 ) parent.dmr_cc_en4.setSelected(true);
                            else parent.dmr_cc_en4.setSelected(false);
                          if( (dmr_config & DMR_CC5) > 0 ) parent.dmr_cc_en5.setSelected(true);
                            else parent.dmr_cc_en5.setSelected(false);
                          if( (dmr_config & DMR_CC6) > 0 ) parent.dmr_cc_en6.setSelected(true);
                            else parent.dmr_cc_en6.setSelected(false);
                          if( (dmr_config & DMR_CC7) > 0 ) parent.dmr_cc_en7.setSelected(true);
                            else parent.dmr_cc_en7.setSelected(false);
                          if( (dmr_config & DMR_CC8) > 0 ) parent.dmr_cc_en8.setSelected(true);
                            else parent.dmr_cc_en8.setSelected(false);
                          if( (dmr_config & DMR_CC9) > 0 ) parent.dmr_cc_en9.setSelected(true);
                            else parent.dmr_cc_en9.setSelected(false);
                          if( (dmr_config & DMR_CC10) > 0 ) parent.dmr_cc_en10.setSelected(true);
                            else parent.dmr_cc_en10.setSelected(false);
                          if( (dmr_config & DMR_CC11) > 0 ) parent.dmr_cc_en11.setSelected(true);
                            else parent.dmr_cc_en11.setSelected(false);
                          if( (dmr_config & DMR_CC12) > 0 ) parent.dmr_cc_en12.setSelected(true);
                            else parent.dmr_cc_en12.setSelected(false);
                          if( (dmr_config & DMR_CC13) > 0 ) parent.dmr_cc_en13.setSelected(true);
                            else parent.dmr_cc_en13.setSelected(false);
                          if( (dmr_config & DMR_CC14) > 0 ) parent.dmr_cc_en14.setSelected(true);
                            else parent.dmr_cc_en14.setSelected(false);
                          if( (dmr_config & DMR_CC15) > 0 ) parent.dmr_cc_en15.setSelected(true);
                            else parent.dmr_cc_en15.setSelected(false);

                          if( (dmr_config & DMR_ISCC) > 0 ) parent.dmr_conplus.setSelected(true);
                            else parent.dmr_conventional.setSelected(true);
                          parent.update_dmr_lcn1_label();

                          if( (dmr_config & DMR_SLOT1) > 0 ) parent.dmr_slot1.setSelected(true);
                            else parent.dmr_slot1.setSelected(false);

                          if( (dmr_config & DMR_SLOT2) > 0 ) parent.dmr_slot2.setSelected(true);
                            else parent.dmr_slot2.setSelected(false);


                          parent.dmr_sys_id.setText( String.format("%d", bb3.getInt(536)) );


                          float vol = bb3.getFloat(12);
                          vol *= 100.0f;
                          parent.lineout_vol_slider.setValue( (int) vol );
                          parent.volume_label.setText( String.format("%3.2f", vol/100.0f) );

                          Boolean b = true;
                          if(bb3.getInt(88)==1) b=true;
                              else b=false;
                          parent.en_bluetooth_cb.setSelected(b); 



                          int bt_reset = bb3.getInt(260)/60;
                          if(bt_reset>0 && bt_reset<5) bt_reset=10;
                          parent.bluetooth_reset.setText( String.format("%d", bt_reset) );


                          parent.lcn1_freq.setText( String.format("%3.6f", bb3.getDouble(384)) );
                          parent.lcn2_freq.setText( String.format("%3.6f", bb3.getDouble(392)) );
                          parent.lcn3_freq.setText( String.format("%3.6f", bb3.getDouble(400)) );
                          parent.lcn4_freq.setText( String.format("%3.6f", bb3.getDouble(408)) );
                          parent.lcn5_freq.setText( String.format("%3.6f", bb3.getDouble(416)) );
                          parent.lcn6_freq.setText( String.format("%3.6f", bb3.getDouble(424)) );
                          parent.lcn7_freq.setText( String.format("%3.6f", bb3.getDouble(432)) );
                          parent.lcn8_freq.setText( String.format("%3.6f", bb3.getDouble(440)) );
                          parent.lcn9_freq.setText( String.format("%3.6f", bb3.getDouble(448)) );
                          parent.lcn10_freq.setText( String.format("%3.6f", bb3.getDouble(456)) );
                          parent.lcn11_freq.setText( String.format("%3.6f", bb3.getDouble(464)) );
                          parent.lcn12_freq.setText( String.format("%3.6f", bb3.getDouble(472)) );
                          parent.lcn13_freq.setText( String.format("%3.6f", bb3.getDouble(480)) );
                          parent.lcn14_freq.setText( String.format("%3.6f", bb3.getDouble(488)) );
                          parent.lcn15_freq.setText( String.format("%3.6f", bb3.getDouble(496)) );

                          if(parent.lcn1_freq.getText().equals("0.000000")) parent.lcn1_freq.setText("");
                          if(parent.lcn2_freq.getText().equals("0.000000")) parent.lcn2_freq.setText("");
                          if(parent.lcn3_freq.getText().equals("0.000000")) parent.lcn3_freq.setText("");
                          if(parent.lcn4_freq.getText().equals("0.000000")) parent.lcn4_freq.setText("");
                          if(parent.lcn5_freq.getText().equals("0.000000")) parent.lcn5_freq.setText("");
                          if(parent.lcn6_freq.getText().equals("0.000000")) parent.lcn6_freq.setText("");
                          if(parent.lcn7_freq.getText().equals("0.000000")) parent.lcn7_freq.setText("");
                          if(parent.lcn8_freq.getText().equals("0.000000")) parent.lcn8_freq.setText("");
                          if(parent.lcn9_freq.getText().equals("0.000000")) parent.lcn9_freq.setText("");
                          if(parent.lcn10_freq.getText().equals("0.000000")) parent.lcn10_freq.setText("");
                          if(parent.lcn11_freq.getText().equals("0.000000")) parent.lcn11_freq.setText("");
                          if(parent.lcn12_freq.getText().equals("0.000000")) parent.lcn12_freq.setText("");
                          if(parent.lcn13_freq.getText().equals("0.000000")) parent.lcn13_freq.setText("");
                          if(parent.lcn14_freq.getText().equals("0.000000")) parent.lcn14_freq.setText("");
                          if(parent.lcn15_freq.getText().equals("0.000000")) parent.lcn15_freq.setText("");


                          int tgtimeout = bb3.getInt(372);
                          switch(tgtimeout) {
                            case  100  :
                              parent.vtimeout.setSelectedIndex(0);
                            break;
                            case  250  :
                              parent.vtimeout.setSelectedIndex(1);
                            break;
                            case  500  :
                              parent.vtimeout.setSelectedIndex(2);
                            break;
                            case  1000  :
                              parent.vtimeout.setSelectedIndex(3);
                            break;
                            case  1500  :
                              parent.vtimeout.setSelectedIndex(4);
                            break;
                            case  2000  :
                              parent.vtimeout.setSelectedIndex(5);
                            break;
                            case  3000  :
                              parent.vtimeout.setSelectedIndex(6);
                            break;
                            case  5000  :
                              parent.vtimeout.setSelectedIndex(7);
                            break;
                            case  10000  :
                              parent.vtimeout.setSelectedIndex(8);
                            break;
                            case  30000  :
                              parent.vtimeout.setSelectedIndex(9);
                            break;
                            default :
                              parent.vtimeout.setSelectedIndex(5);
                            break;
                          }


                          float kp = bb3.getFloat(292);

                          if(kp == 0.0001f) {
                            parent.agc_kp.setSelectedIndex(0);
                          }
                          else if(kp == 0.00025f) {
                            parent.agc_kp.setSelectedIndex(1);
                          }
                          else if(kp == 0.0005f) {
                            parent.agc_kp.setSelectedIndex(2);
                          }
                          else {
                            parent.agc_kp.setSelectedIndex(2);
                          }

                          //int rfmg = bb3.getInt(296)-4;
                          //if(rfmg<0) rfmg=0;
                          //parent.rfmaxgain.setSelectedIndex( rfmg ); 

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


                          parent.update_op_mode(op_mode-1);



                          /*
                          byte[] result=new byte[64];
                          String cmd = "mac_id\r\n";  
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(100);
                          rlen=serial_port.readBytes( result, 64);

                          String macid = new String(result,0,16).trim();
                          if(macid.startsWith("0x")) {
                            System.out.println("mac_id:"+macid +":");
                            parent.sys_mac_id = macid;
                          }
                          */

                        } catch(Exception e) {
                          e.printStackTrace();
                        }

                      }
                      else {
                        try {

                          cmd = ""; 
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
                          byte[] result=new byte[64];

                          cmd = "logging -999"+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);

                          //stop following
                          cmd = "f 0"+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);

                          int reset_on_save=0;

                          int op_mode = parent.op_mode.getSelectedIndex();
                          if( prev_op_mode != op_mode || (op_mode!=0) ) {
                            //reset_on_save=1;
                          }
                          op_mode++;

                          result=new byte[64];
                          cmd = "op_mode "+op_mode+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);


                          String freq_to_use="";
                          double freq_d = 859.9625;

                          parent.freq.setText( "Freq: "+parent.frequency_tf1.getText().trim() );
                          freq_to_use=parent.frequency_tf1.getText().trim();

                          try {
                            freq_d = new Double(freq_to_use).doubleValue();
                          } catch(Exception e) {
                            freq_to_use="859.9625";
                          }

                          result=new byte[64];
                          cmd = "freq "+freq_to_use+"\r\n";  

                          if( !is_valid_freq(freq_d) ) {
                            JOptionPane.showMessageDialog(parent, "Invalid Frequency "+freq_to_use);
                            freq_to_use = "859.9625";
                            cmd = "freq "+freq_to_use+"\r\n";  
                          }

                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          result=new byte[64];
                          cmd = "no_voice_roam_sec "+parent.no_voice_secs.getText()+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          result=new byte[64];
                          //if( ! new String(result).trim().contains("frequency: "+parent.frequency_tf.getText().trim()) ) return;
                          cmd = "vol "+(float) parent.lineout_vol_slider.getValue()/100.0f+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          int agckp = parent.agc_kp.getSelectedIndex();
                          float kp = 0.0005f;

                          switch(agckp) {
                            case  0  :
                              kp = 0.0001f;
                            break;
                            case  1  :
                              kp = 0.00025f;
                            break;
                            case  2  :
                              kp = 0.0005f;
                            break;

                            default :
                              kp = 0.0005f;
                              parent.agc_kp.setSelectedIndex(2);
                            break;
                          }

                          //fixed value of "high"
                          kp = 0.0005f;

                          result=new byte[64];
                          cmd = "quad_agc_bw "+kp+"\r\n";  
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          int vt = parent.vtimeout.getSelectedIndex();
                          int vto = 1000;
                          switch(vt) {
                            case  0  :
                              vto = 100;
                            break;
                            case  1  :
                              vto = 250;
                            break;
                            case  2  :
                              vto = 500;
                            break;
                            case  3  :
                              vto = 1000;
                            break;
                            case  4  :
                              vto = 1500;
                            break;
                            case  5  :
                              vto = 2000;
                            break;
                            case  6  :
                              vto = 3000;
                            break;
                            case  7  :
                              vto = 5000;
                            break;
                            case  8  :
                              vto = 10000;
                            break;
                            case  9  :
                              vto = 30000;
                            break;
                            default :
                              vto = 2000;
                            break;
                          }
                          result=new byte[64];
                          cmd = "tgtimeout "+vto+"\r\n";  
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          result=new byte[64];



                          //if(duid_enh==1) parent.duid_enh.setSelected(true);
                          //  else parent.duid_enh.setSelected(false);

                          //if(freq_correct_on_voice==1) parent.freq_correct_on_voice.setSelected(true);
                           // else parent.freq_correct_on_voice.setSelected(false);

                          //if(add_tdu_silence==1) parent.add_tdu_silence.setSelected(true);
                           // else parent.add_tdu_silence.setSelected(false);

                          int duid_enh=0;
                          int freq_correct_on_voice=0;
                          int add_tdu_silence=0;

                          if( parent.duid_enh.isSelected() ) duid_enh=1;
                          if( parent.freq_correct_on_voice.isSelected() ) freq_correct_on_voice=1;
                          if( parent.add_tdu_silence.isSelected() ) add_tdu_silence=1;


                          result=new byte[64];
                          cmd = "duid_enh "+duid_enh+"\r\n";  
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          result=new byte[64];
                          cmd = "freq_corr_on_voice "+freq_correct_on_voice+"\r\n";  
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          result=new byte[64];
                          cmd = "add_tdu_silence "+add_tdu_silence+"\r\n";  
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );



                          //cmd = "bt_reset "+parent.bluetooth_reset.getText()+"\r\n";
                          cmd = "bt_reset 0"+"\r\n";  //always disabled for now
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(50);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          result=new byte[64];

                          boolean b = parent.controlchannel.isSelected();
                          if(b) cmd = "is_control 1\r\n";
                            else cmd = "is_control 0\r\n"; 

                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(50);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );



                          result=new byte[64];

                          b = parent.wacn_en.isSelected();
                          if(b) cmd = "wacn_en 1\r\n";
                            else cmd = "wacn_en 0\r\n"; 

                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(50);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          result=new byte[64];

                          b = parent.en_usb_wdog.isSelected();
                          if(b && parent.is_mac_osx==0) cmd = "en_usb_wdog 1\r\n";
                            else cmd = "en_usb_wdog 0\r\n"; 

                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(50);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );



                          result=new byte[64];

                          b = parent.en_bluetooth_cb.isSelected();
                          if(b) cmd = "bluetooth 1\r\n";
                            else cmd = "bluetooth 0\r\n"; 

                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(50);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          b = parent.allow_unknown_tg_cb.isSelected();
                          if(b) cmd = "en_unknown_tg 1\r\n";
                            else cmd = "en_unknown_tg 0\r\n"; 

                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          b = parent.roaming.isSelected();
                          if(b) cmd = "roaming 1\r\n";
                            else cmd = "roaming 0\r\n"; 

                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          result=new byte[64];
                          b = parent.enable_leds.isSelected();
                          if(b) cmd = "led_mode 1\r\n";
                            else cmd = "led_mode 0\r\n"; 
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );

                          result=new byte[64];
                          cmd = "sys_name "+parent.system_alias.getText()+"\r\n"; 
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(10);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );


                          //int maxgain = parent.rfmaxgain.getSelectedIndex()+4;
                          //cmd = "rf_max_gain "+maxgain+"\r\n"; 
                          //serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          //Thread.sleep(10);
                          //rlen=serial_port.readBytes( result, 64);
                          //System.out.println("result: "+new String(result) );

                          //do this one last
                          //cmd = "is_control 1\r\n";
                          //serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          //Thread.sleep(20);
                          //rlen=serial_port.readBytes( result, 64);
                          //System.out.println("result: "+new String(result) );
                          //Thread.sleep(10);

                          result=new byte[64];
                          int dmr_sys_id = 1; 
                          try {
                            dmr_sys_id = new Integer( parent.dmr_sys_id.getText() ).intValue();
                          } catch(Exception e) {
                          }
                          if(dmr_sys_id<=0) dmr_sys_id=1;

                          cmd = "dmr_sys_id "+dmr_sys_id+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);


                          int dmr_config = 0;
                          if(parent.dmr_slot1.isSelected()) dmr_config |= DMR_SLOT1;
                          if(parent.dmr_slot2.isSelected()) dmr_config |= DMR_SLOT2;
                          if(parent.dmr_cc_en1.isSelected()) dmr_config |= DMR_CC1;
                          if(parent.dmr_cc_en2.isSelected()) dmr_config |= DMR_CC2;
                          if(parent.dmr_cc_en3.isSelected()) dmr_config |= DMR_CC3;
                          if(parent.dmr_cc_en4.isSelected()) dmr_config |= DMR_CC4;
                          if(parent.dmr_cc_en5.isSelected()) dmr_config |= DMR_CC5;
                          if(parent.dmr_cc_en6.isSelected()) dmr_config |= DMR_CC6;
                          if(parent.dmr_cc_en7.isSelected()) dmr_config |= DMR_CC7;
                          if(parent.dmr_cc_en8.isSelected()) dmr_config |= DMR_CC8;
                          if(parent.dmr_cc_en9.isSelected()) dmr_config |= DMR_CC9;
                          if(parent.dmr_cc_en10.isSelected()) dmr_config |= DMR_CC10;
                          if(parent.dmr_cc_en11.isSelected()) dmr_config |= DMR_CC11;
                          if(parent.dmr_cc_en12.isSelected()) dmr_config |= DMR_CC12;
                          if(parent.dmr_cc_en13.isSelected()) dmr_config |= DMR_CC13;
                          if(parent.dmr_cc_en14.isSelected()) dmr_config |= DMR_CC14;
                          if(parent.dmr_cc_en15.isSelected()) dmr_config |= DMR_CC15;
                          if(parent.dmr_conplus.isSelected()) dmr_config |= DMR_ISCC;

                          result=new byte[64];
                          cmd = "dmr_config "+String.format("%08x", dmr_config)+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);

                          if( parent.lcn1_freq.getText().equals("") ) parent.lcn1_freq.setText("0.000000");
                          if( parent.lcn2_freq.getText().equals("") ) parent.lcn2_freq.setText("0.000000");
                          if( parent.lcn3_freq.getText().equals("") ) parent.lcn3_freq.setText("0.000000");
                          if( parent.lcn4_freq.getText().equals("") ) parent.lcn4_freq.setText("0.000000");
                          if( parent.lcn5_freq.getText().equals("") ) parent.lcn5_freq.setText("0.000000");
                          if( parent.lcn6_freq.getText().equals("") ) parent.lcn6_freq.setText("0.000000");
                          if( parent.lcn7_freq.getText().equals("") ) parent.lcn7_freq.setText("0.000000");
                          if( parent.lcn8_freq.getText().equals("") ) parent.lcn8_freq.setText("0.000000");
                          if( parent.lcn9_freq.getText().equals("") ) parent.lcn9_freq.setText("0.000000");
                          if( parent.lcn10_freq.getText().equals("") ) parent.lcn10_freq.setText("0.000000");
                          if( parent.lcn11_freq.getText().equals("") ) parent.lcn11_freq.setText("0.000000");
                          if( parent.lcn12_freq.getText().equals("") ) parent.lcn12_freq.setText("0.000000");
                          if( parent.lcn13_freq.getText().equals("") ) parent.lcn13_freq.setText("0.000000");
                          if( parent.lcn14_freq.getText().equals("") ) parent.lcn14_freq.setText("0.000000");
                          if( parent.lcn15_freq.getText().equals("") ) parent.lcn15_freq.setText("0.000000");

                          result=new byte[64];
                          cmd = "dmr_lcn1 "+String.format("%3.6f", Double.valueOf(parent.lcn1_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          result=new byte[64];
                          cmd = "dmr_lcn2 "+String.format("%3.6f", Double.valueOf(parent.lcn2_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          result=new byte[64];
                          cmd = "dmr_lcn3 "+String.format("%3.6f", Double.valueOf(parent.lcn3_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          result=new byte[64];
                          cmd = "dmr_lcn4 "+String.format("%3.6f", Double.valueOf(parent.lcn4_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          result=new byte[64];
                          cmd = "dmr_lcn5 "+String.format("%3.6f", Double.valueOf(parent.lcn5_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          result=new byte[64];
                          cmd = "dmr_lcn6 "+String.format("%3.6f", Double.valueOf(parent.lcn6_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          result=new byte[64];
                          cmd = "dmr_lcn7 "+String.format("%3.6f", Double.valueOf(parent.lcn7_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          result=new byte[64];
                          cmd = "dmr_lcn8 "+String.format("%3.6f", Double.valueOf(parent.lcn8_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          result=new byte[64];
                          cmd = "dmr_lcn9 "+String.format("%3.6f", Double.valueOf(parent.lcn9_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          cmd = "dmr_lcn10 "+String.format("%3.6f", Double.valueOf(parent.lcn10_freq.getText()))+"\r\n";
                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          cmd = "dmr_lcn11 "+String.format("%3.6f", Double.valueOf(parent.lcn11_freq.getText()))+"\r\n";
                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          cmd = "dmr_lcn12 "+String.format("%3.6f", Double.valueOf(parent.lcn12_freq.getText()))+"\r\n";
                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          cmd = "dmr_lcn13 "+String.format("%3.6f", Double.valueOf(parent.lcn13_freq.getText()))+"\r\n";
                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          cmd = "dmr_lcn14 "+String.format("%3.6f", Double.valueOf(parent.lcn14_freq.getText()))+"\r\n";
                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);
                          cmd = "dmr_lcn15 "+String.format("%3.6f", Double.valueOf(parent.lcn15_freq.getText()))+"\r\n";
                          result=new byte[64];
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);

                          result=new byte[64];

                          cmd = "ref_freq "+String.format("%5.0f", Double.valueOf(parent.ref_freq.getText()))+"\r\n";
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          Thread.sleep(20);
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(10);

                          result=new byte[64];
                          if(reset_on_save==1) {
                            cmd = "save 1\r\n";
                          }
                          else {
                            cmd = "save\r\n";
                          }
                          serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
                          if(parent.op_mode.getSelectedIndex()==2) {
                            Thread.sleep(2000);
                          }
                          else {
                            Thread.sleep(10);
                          }
                          rlen=serial_port.readBytes( result, 64);
                          System.out.println("result: "+new String(result) );
                          Thread.sleep(2000);

                          parent.setStatus("sys_config update ok."); 


                          parent.do_write_config=0;


                          if(reset_on_save==1) {
                            parent.is_connected=0;
                            parent.do_connect=1;
                          }
                          else {
                            parent.do_read_config=1;
                          }
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
                        cmd = "system_reset\r\n";
                        serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);

                        try {
                          Thread.sleep(5000);
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
