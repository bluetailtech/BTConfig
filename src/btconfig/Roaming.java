
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.fazecast.jSerialComm.*;
import javax.swing.filechooser.*;
import javax.swing.*;
import javax.swing.*;
import java.awt.geom.*;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Roaming
{

java.util.Timer utimer;
BTFrame parent;
SerialPort serial_port;
java.text.SimpleDateFormat formatter_date;

private int get_offset_only=0;
private int offset_only_length; 
private int write_flash_only=0;
private Hashtable append_hash;

int did_warning=0;
int did_crc_reset=0;

int did_write_roaming=0;
int append_mode=0;

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
public void test_selected_freqs(BTFrame parent, SerialPort serial_port) {
  this.serial_port = serial_port;

  try {

    this.parent = parent;

    int state = -1; 

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

    parent.did_freq_tests=1;

    int[] rows = parent.freq_table.getSelectedRows();
    if(rows==null) return;

    for(int cnt=0;cnt<rows.length;cnt++) {
     parent.freq_table.getModel().setValueAt( null, rows[cnt], 4); 
     parent.freq_table.getModel().setValueAt( null, rows[cnt], 5); 
     parent.freq_table.getModel().setValueAt( null, rows[cnt], 6); 
    }



    Boolean first_row=true;
    int total=0;
    int total_cc=0;

    for(int cnt=0;cnt<rows.length;) {

      String tfreq = (String) parent.freq_table.getModel().getValueAt( rows[cnt], 3 );


      if(state==0) {
        byte[] out_buffer = new byte[16+32]; //size of bl_op
        ByteBuffer bb = ByteBuffer.wrap(out_buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
        bb.putInt( (int) Long.parseLong("7", 10) ); //test freq op
        bb.putInt( (int) new Long((long) 0x55555555 ).longValue() );  //address to return
        bb.putInt( (int) Long.parseLong("32", 10) );  //data len  to return

        try {
          double df = (double) Double.parseDouble(tfreq);
          if( !is_valid_freq(df) ) {
            cnt++;
            if(cnt==rows.length) return;
            continue;
          }
        } catch(Exception e) {
            cnt++;
            if(cnt==rows.length) return;
          continue;
        }

        bb.putDouble( (double) Double.parseDouble(tfreq) ); //freq to test 
        parent.setStatus("testing freq "+tfreq);
        System.out.println("testing freq "+tfreq);

        byte[] input_buffer = new byte[48];
        int rlen=0;
        while(rlen!=48) {
          serial_port.writeBytes( out_buffer, 48, 0); //16 + data len=0

            try {
              int count=0;
              while(serial_port.bytesAvailable()<48) {
                SLEEP(1);
                if(count++>2000) {
                  System.out.println("freq test timeout");
                  break;
                }
              }
            } catch(Exception e) {
              e.printStackTrace();
            }

          rlen=serial_port.readBytes( input_buffer, 48 );
          if(rlen==48) {
            break;
          }
        }

        parent.setStatus("get results for freq "+tfreq);

        ByteBuffer bb2 = ByteBuffer.wrap(input_buffer);
        bb2.order(ByteOrder.LITTLE_ENDIAN);


        if( bb2.getInt()== 0xd35467A6) {//magic
          int op = bb2.getInt();  //op
          int addr = bb2.getInt();  //address   //this should be 0x55555555
          int slen = bb2.getInt();  //len
          byte[] retb = new byte[slen];
          for(int i=0;i<slen;i++) {
            retb[i] = bb2.get();
          }
          if(op==4 && addr==0x55555555) {
            System.out.println("op"+op+"  results: "+new String(retb)+" for freq "+tfreq);

            if(new String(retb).equals("P25")) {
              retb = new String("P1").getBytes();
            }


            parent.freq_table.getModel().setValueAt( new String(retb),rows[cnt], 5 );

            if( new String(retb).equals("P25CC")) {
              parent.freq_table.getModel().setValueAt( new String("CTRL>"),rows[cnt], 4 );

              try {
                if(parent.prefs!=null) {
                  String str1 = (String)  parent.freq_table.getModel().getValueAt(rows[cnt],8);
                  String str2 = (String) parent.freq_table.getModel().getValueAt(rows[cnt],9);
                  String str3 = (String) parent.freq_table.getModel().getValueAt(rows[cnt],1);
                  if(str1!=null && str1.length()>0 && str2!=null && str2.length()>0 && str3!=null && str3.length()>0) {
                    parent.prefs.put("city_state_"+tfreq.trim(), str1+","+str2+"_"+str3 );
                  }
                }
              } catch(Exception e) {
              }

              total_cc++;
              if(first_row) {
                parent.freq_table.setRowSelectionInterval(rows[cnt],rows[cnt]);
                first_row=false;
              }
              else {
                parent.freq_table.addRowSelectionInterval(rows[cnt],rows[cnt]);
              }
            }
            else if( new String(retb).equals("P1")) {
              parent.freq_table.getModel().setValueAt( new String("SYNC"),rows[cnt], 4 );
            }
            else {
              parent.freq_table.getModel().setValueAt( new String("X"),rows[cnt], 4 );
            }


            total++;
            cnt++;
            if(cnt==rows.length) {
              parent.setStatus("tested "+total+" frequencies. Found "+total_cc+" P25 control channels.");
              return;
            }
          }
        }
        else {
          //flush the input buffers
          byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
          if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
        }
      }
    }
  } catch (Exception e) {
    e.printStackTrace();
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void restore_roaming(BTFrame parent, BufferedInputStream bis, SerialPort serial_port)
{
  this.parent = parent;
  this.serial_port = serial_port;

  byte[] image_buffer = new byte[128 * 1024 * 6];

  parent.did_freq_tests=0;

  for( int i=0; i< 128 * 1024 *6; i++) {
    image_buffer[i] = (byte) 0xff;
  }
    try {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() { 
          parent.jScrollPane2.getVerticalScrollBar().setValue(0);
        }
      });

    } catch(Exception e) {
    }

  int config_length = 0;

  try {

    byte[] header = new byte[4];
    config_length = bis.read(header, 0, 4);

    String header_str = new String(header);
    if(!header_str.equals("ROAM")) {
      System.out.println("bad roaming file");
      JOptionPane.showMessageDialog(parent, "invalid Roaming Frequency file format.  Could be from an older version of the software");
      return;
    }

    parent.do_read_roaming=1;

    config_length = bis.read(image_buffer, 0, 128*1024*6);

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


        if(state==0) {

            parent.setProgress(5);
            parent.setStatus("Writing roaming to P25RX device..."); 

            int offset = 0;

            while(offset<config_length) {

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);
              //uint32_t magic;
              //uint32_t op;
              //uint32_t addr;
              //uint32_t len;
              //uint8_t  data[32]; 

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("3", 10) ); //write flash cmd 
              bb.putInt( (int) new Long((long) 0x08160000 + offset).longValue() );
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len

              for(int i=0;i<32;i++) {
                bb.put( image_buffer[i+offset] ); 
              }



              byte[] input_buffer = new byte[48];
              int rlen = 0;
              int ack_timeout=0;

              while(rlen!=48) {

                serial_port.writeBytes( out_buffer, 16+32, 0);

                if(offset==0) {
                  SLEEP(500);
                }

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      SLEEP(1);
                      if(count++>500) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }


                rlen=serial_port.readBytes( input_buffer, 48);

                  ByteBuffer bb_verify = ByteBuffer.wrap(input_buffer);
                  bb_verify.order(ByteOrder.LITTLE_ENDIAN);
                  if( bb_verify.getInt()== 0xd35467A6) {//magic
                    int op = bb_verify.getInt();  //op
                    //System.out.println("op "+op);
                    if( op==4 && bb_verify.getInt()==0x8160000+offset) { //address
                      break;
                    }
                    else {
                      rlen=0;  //need this to keep loop going
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                    }
                  }
                  else {
                    //System.out.println( String.format( "0x%08x", bb_verify.getInt(0) ) );
                    //System.out.println( String.format( "op 0x%08x", bb_verify.getInt(4) ) );
                    //System.out.println( String.format( "addr 0x%08x", bb_verify.getInt(8) ) );
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                  }

                if(rlen==48) break;
              }

              /*
              if(input_buffer[4]==5) {
                state=-1;
                parent.setStatus("NACK");
                break;
              }
              */

              offset+=32;

              int pcomplete = (int)  (((float) offset/(float) config_length)*80.0);
              parent.setProgress((int) pcomplete);
            }


            //TODO: need to check for ack
            try {
              SLEEP(100);
            } catch(Exception e) {
            }

            parent.do_read_roaming=1;
              for(int i=0;i<250;i++) {
               parent.freq_table.getModel().setValueAt( null, i, 0); 
               parent.freq_table.getModel().setValueAt( null, i, 1); 
               parent.freq_table.getModel().setValueAt( null, i, 2); 
               parent.freq_table.getModel().setValueAt( null, i, 3); 
               parent.freq_table.getModel().setValueAt( null, i, 4); 
               parent.freq_table.getModel().setValueAt( null, i, 5); 
               parent.freq_table.getModel().setValueAt( null, i, 6); 
               parent.freq_table.getModel().setValueAt( null, i, 7); 
               parent.freq_table.getModel().setValueAt( null, i, 8); 
               parent.freq_table.getModel().setValueAt( null, i, 9); 
               parent.freq_table.getModel().setValueAt( null, i, 10); 
              }
            parent.setStatus("\r\nCompleted restoring roaming.");
            parent.setProgress(100);
            return;

        }

    } //while(true) 
  } catch (Exception e) {
    e.printStackTrace();
  }
}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void erase_roaming(BTFrame parent, SerialPort serial_port)
{
  this.serial_port = serial_port;
  this.parent = parent;

  byte[] image_buffer = new byte[128 * 1024 * 6];

  parent.did_freq_tests=0;

  for( int i=0; i< 128 * 1024 *6; i++) {
    image_buffer[i] = (byte) 0xff;
  }

  int config_length = 0;

  try {

    //config_length = bis.read(image_buffer, 0, 128*1024*6);

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


        if(state==0) {

            int nrecs=0;
            ByteBuffer bb_image = ByteBuffer.wrap(image_buffer);
            bb_image.order(ByteOrder.LITTLE_ENDIAN);

              try {
                for(int j=0;j<32;j++) {
                  bb_image.put((byte) 0xff);
                }

                config_length+=32;  //length of record
              } catch(Exception e) {
              }
            }

            parent.setProgress(5);
            parent.setStatus("erasing roaming flash on P25RX device..."); 

            int offset = 0;

            while(offset<config_length) {

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);
              //uint32_t magic;
              //uint32_t op;
              //uint32_t addr;
              //uint32_t len;
              //uint8_t  data[32]; 

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("3", 10) ); //write flash cmd 
              bb.putInt( (int) new Long((long) 0x08160000 + offset).longValue() );
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len

              for(int i=0;i<32;i++) {
                bb.put( image_buffer[i+offset] ); 
              }

              byte[] input_buffer = new byte[48];
              int rlen = 0;
              int ack_timeout=0;

              while(rlen!=48) {

                serial_port.writeBytes( out_buffer, 16+32, 0);

                if(offset==0) {
                  SLEEP(500);
                }

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      SLEEP(1);
                      if(count++>500) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }


                rlen=serial_port.readBytes( input_buffer, 48);

                  ByteBuffer bb_verify = ByteBuffer.wrap(input_buffer);
                  bb_verify.order(ByteOrder.LITTLE_ENDIAN);
                  if( bb_verify.getInt()== 0xd35467A6) {//magic
                    int op = bb_verify.getInt();  //op
                    //System.out.println("op "+op);
                    if( op==4 && bb_verify.getInt()==0x8160000+offset) { //address
                      break;
                    }
                    else {
                      rlen=0;  //need this to keep loop going
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                    }
                  }
                  else {
                    //System.out.println( String.format( "0x%08x", bb_verify.getInt(0) ) );
                    //System.out.println( String.format( "op 0x%08x", bb_verify.getInt(4) ) );
                    //System.out.println( String.format( "addr 0x%08x", bb_verify.getInt(8) ) );
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                  }

                if(rlen==48) break;
              }

              offset+=32;

              int pcomplete = (int)  (((float) offset/(float) config_length)*80.0);
              parent.setProgress((int) pcomplete);
            }


            //TODO: need to check for ack
            try {
              SLEEP(100);
            } catch(Exception e) {
            }

            parent.setStatus("\r\nCompleted erasing roaming flash.");
            parent.setProgress(100);
            did_write_roaming=1;
            parent.do_read_roaming=1;
              for(int i=0;i<250;i++) {
               parent.freq_table.getModel().setValueAt( null, i, 0); 
               parent.freq_table.getModel().setValueAt( null, i, 1); 
               parent.freq_table.getModel().setValueAt( null, i, 2); 
               parent.freq_table.getModel().setValueAt( null, i, 3); 
               parent.freq_table.getModel().setValueAt( null, i, 4); 
               parent.freq_table.getModel().setValueAt( null, i, 5); 
               parent.freq_table.getModel().setValueAt( null, i, 6); 
               parent.freq_table.getModel().setValueAt( null, i, 7); 
               parent.freq_table.getModel().setValueAt( null, i, 8); 
               parent.freq_table.getModel().setValueAt( null, i, 9); 
               parent.freq_table.getModel().setValueAt( null, i, 10); 
              }
            parent.freq_table.setRowSelectionInterval(0,0);
            return;
        }
  } catch (Exception e) {
    e.printStackTrace();
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void write_roaming_flash(BTFrame parent, SerialPort serial_port)
{
  this.serial_port = serial_port;
  this.parent = parent;
  write_flash_only=1;

  parent.did_freq_tests=0;

  System.out.println("write roaming flash_only");
  try {
    send_roaming(parent, serial_port, 0);
  } catch(Exception e) {
    e.printStackTrace();
  }

  write_flash_only=0;

}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void append_roaming(BTFrame parent, SerialPort serial_port)
{
  this.serial_port = serial_port;
  this.parent = parent;


  int config_length = 0;
  append_mode=1;
  parent.did_freq_tests=0;

  try {
    get_offset_only=1;
    backup_roaming(parent, serial_port);
    System.out.println("offset: "+offset_only_length);

    if( offset_only_length/32 >= 250) {
      JOptionPane.showMessageDialog(parent, "Roaming flash already contains max number of frequency entries. (250).");
      get_offset_only=0;
      return;
    }

    send_roaming(parent, serial_port, offset_only_length);

    //if(parent.zs!=null) parent.zs.search3();
  } catch(Exception e) {
  }

  get_offset_only=0;
  append_mode=0;
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void send_roaming(BTFrame parent, SerialPort serial_port, int start_offset)
{
  this.serial_port = serial_port;
  this.parent = parent;

  byte[] image_buffer = new byte[128 * 1024 * 6];

  parent.did_freq_tests=0;

  for( int i=0; i< 128 * 1024 *6; i++) {
    image_buffer[i] = (byte) 0xff;
  }

  int config_length = 0;

  try {

    //config_length = bis.read(image_buffer, 0, 128*1024*6);

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


        if(state==0) {

            int nrecs=0;
            ByteBuffer bb_image = ByteBuffer.wrap(image_buffer);
            bb_image.order(ByteOrder.LITTLE_ENDIAN);

            int[] rows = parent.freq_table.getSelectedRows();
            config_length=0;

            if(write_flash_only==1) {
              int[] r = new int[250];
              int n=0;
              for(int i=0;i<250;i++) {
                if(parent.freq_table.getModel().getValueAt(i,6)==null) {
                  r[i] = -1; 
                }
                else if(((String) parent.freq_table.getModel().getValueAt(i,6)).equals("X") ) {
                  r[i] = i;
                  n++;
                }
              }
              if(n<0) return;

              if(n==0) {
                erase_roaming(parent, serial_port); //no more records left, so erase flash
                parent.do_read_roaming=1;
                return;
              }

              rows = new int[n];
              int idx=0;
              for(int i=0;i<250;i++) {
                if(r[i]>=0) {
                  rows[idx++] = i;
                }
              }
            }

            for(int i=0;i<rows.length;i++) {
              try {
                  String freq = (String) parent.freq_table.getModel().getValueAt(rows[i],3);
                  String test_result = (String) parent.freq_table.getModel().getValueAt(rows[i],5);
                  if(test_result==null) test_result="NOSIG";

                  System.out.println("freq "+freq);

                  double rfreq = 0.0;
                  try {
                    rfreq = new Double(freq).doubleValue();
                    parent.freq_table.getModel().setValueAt(String.format("%3.8f",rfreq),rows[i],3);

                  } catch(Exception e) {
                  }

                  if(parent.freq_table.getModel().getValueAt(rows[i],1)==null) parent.freq_table.getModel().setValueAt("",rows[i],1);
                  if(parent.freq_table.getModel().getValueAt(rows[i],1)==null) parent.freq_table.getModel().setValueAt("",rows[i],8);
                  if(parent.freq_table.getModel().getValueAt(rows[i],1)==null) parent.freq_table.getModel().setValueAt("",rows[i],9);
                  
                  if( is_valid_freq(rfreq) ) {
                    bb_image.putDouble(rfreq);

                    byte rec_result=0;
                    if(test_result.equals("P1")) rec_result=1;
                    if(test_result.equals("P25CC")) rec_result=2;
                    if(test_result.equals("NOSIG")) rec_result=3;
                    if(test_result.equals("P25CC")) rec_result=4;

                    if(String.format("%3.8f",rfreq).equals(parent.frequency_tf1.getText()) ) {
                      parent.freq_table.getModel().setValueAt("PRIM",rows[i],4);
                      rec_result=5;
                    }

                      try {
                        if(parent.prefs!=null) {
                          String str1 = (String)  parent.freq_table.getModel().getValueAt(rows[i],8);
                          String str2 = (String) parent.freq_table.getModel().getValueAt(rows[i],9);
                          String str3 = (String) parent.freq_table.getModel().getValueAt(rows[i],1);
                          if(str1!=null && str1.length()>0 && str2!=null && str2.length()>0 && str3!=null && str3.length()>0) {
                            parent.prefs.put("city_state_"+freq.trim(), str1+","+str2+"_"+str3 );
                          }
                        }
                      } catch(Exception e) {
                      }


                    bb_image.put((byte) rec_result);  //  test result,  0=unknown, 1=p25_sync, 2=p1_cc, 3=nosig, 4 = primary_cc

                    double lat = 0.0;
                    double lon = 0.0;
                    Point2D.Double p2d = (Point2D.Double) parent.lat_lon_hash1.get( String.format("%3.8f",rfreq) );

                    if(p2d!=null) {
                      for(int j=0;j<3;j++) {
                        bb_image.put((byte) 0);
                      }
                      bb_image.putDouble( p2d.x );
                      bb_image.putDouble( p2d.y );
                    }
                    else {
                      for(int j=0;j<19;j++) {
                        bb_image.put((byte) 0);
                      }
                    }

                    byte[] rec_bytes = new byte[32-4]; 
                    for(int j=0;j<32-4;j++) {
                      rec_bytes[j] = image_buffer[j+config_length];
                    }

                    int crc = crc32.crc32_range(rec_bytes, 32-4);
                    bb_image.putInt(crc);

                    config_length+=32;  //length of record
                 }
              } catch(Exception e) {
              }
            }

            //System.out.println("writing "+config_length+" bytes");

            //parent.setStatus("nrecs "+nrecs);
            //if(true) return;

            parent.setProgress(5);
            parent.setStatus("Writing roaming to P25RX device..."); 

            int offset = start_offset;
            int offset_out = start_offset;

            int is_redundant=0;

            while(offset<config_length+start_offset) {

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);
              //uint32_t magic;
              //uint32_t op;
              //uint32_t addr;
              //uint32_t len;
              //uint8_t  data[32]; 

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              if(append_mode==1) {
                bb.putInt( (int) Long.parseLong("8", 10) ); //write flash cmd, no flash erase
              }
              else {
                bb.putInt( (int) Long.parseLong("3", 10) ); //write flash cmd 
              }
              bb.putInt( (int) new Long((long) 0x08160000 + offset_out).longValue() );
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len

              byte[] freq_test_rec = new byte[32];

              for(int i=0;i<32;i++) {
                bb.put( image_buffer[i+offset-start_offset] ); 
                freq_test_rec[i] = image_buffer[i+offset-start_offset];
              }

              try {
                if(write_flash_only==0) {
                  ByteBuffer bb_freq = ByteBuffer.wrap(freq_test_rec);
                  bb_freq.order(ByteOrder.LITTLE_ENDIAN);
                  double freq_d = bb_freq.getDouble();
                  if( append_hash.get( String.format("%3.8f", freq_d) ) != null) {
                    System.out.println("redundant frequency skipping "+String.format("%3.8f",freq_d));
                    is_redundant=1;
                  }
                  else {
                    System.out.println("writing frequency "+String.format("%3.8f",freq_d));
                    is_redundant=0;
                    String freq_s = String.format("%3.8f",freq_d);
                    append_hash.put(freq_s,freq_s);
                  }
                }
              } catch(Exception e) {
                e.printStackTrace();
              }

              byte[] input_buffer = new byte[48];
              int rlen = 0;
              int ack_timeout=0;

              while(rlen!=48 && is_redundant==0) {

                serial_port.writeBytes( out_buffer, 16+32, 0);

                if(offset==0) {
                  SLEEP(500);
                }

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      SLEEP(1);
                      if(count++>500) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }


                rlen=serial_port.readBytes( input_buffer, 48);

                  ByteBuffer bb_verify = ByteBuffer.wrap(input_buffer);
                  bb_verify.order(ByteOrder.LITTLE_ENDIAN);
                  if( bb_verify.getInt()== 0xd35467A6) {//magic
                    int op = bb_verify.getInt();  //op
                    //System.out.println("op "+op);
                    if( op==4 && bb_verify.getInt()==0x8160000+offset_out) { //address
                      break;
                    }
                    else {
                      rlen=0;  //need this to keep loop going
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                    }
                  }
                  else {
                    //System.out.println( String.format( "0x%08x", bb_verify.getInt(0) ) );
                    //System.out.println( String.format( "op 0x%08x", bb_verify.getInt(4) ) );
                    //System.out.println( String.format( "addr 0x%08x", bb_verify.getInt(8) ) );
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                  }

                if(rlen==48) break;
              }

              offset+=32;
              if(is_redundant==0) offset_out+=32;

              int pcomplete = (int)  (((float) offset/(float) (config_length+start_offset) )*80.0);
              parent.setProgress((int) pcomplete);
            }


            //TODO: need to check for ack
            try {
              SLEEP(100);
            } catch(Exception e) {
            }

            parent.setStatus("\r\nCompleted sending roaming.");
            parent.setProgress(100);
            did_write_roaming=1;
            parent.do_read_roaming=1;

          try {
              for(int i=0;i<250;i++) {
               parent.freq_table.getModel().setValueAt( null, i, 5); 
              }
          } catch(Exception e) {
            e.printStackTrace();
          }

          try {
              for(int i=0;i<250;i++) {
               parent.freq_table.getModel().setValueAt( null, i, 0); 
               parent.freq_table.getModel().setValueAt( null, i, 1); 
               parent.freq_table.getModel().setValueAt( null, i, 2); 
               parent.freq_table.getModel().setValueAt( null, i, 3); 
               parent.freq_table.getModel().setValueAt( null, i, 4); 
               parent.freq_table.getModel().setValueAt( null, i, 5); 
               parent.freq_table.getModel().setValueAt( null, i, 6); 
               parent.freq_table.getModel().setValueAt( null, i, 7); 
               parent.freq_table.getModel().setValueAt( null, i, 8); 
               parent.freq_table.getModel().setValueAt( null, i, 9); 
               parent.freq_table.getModel().setValueAt( null, i, 10); 
              }
          } catch(Exception e) {
          }

            parent.freq_table.setRowSelectionInterval(0,0);
            
            if(parent.op_mode.getSelectedIndex()==0) { 
              set_freq_binary( parent.frequency_tf1.getText() );
            }
            return;
        }

    } //while(true) 

  } catch (Exception e) {
    e.printStackTrace();
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void backup_roaming(BTFrame parent, SerialPort serial_port) {

    if(did_write_roaming==0 && get_offset_only==0) {

      if(JOptionPane.showConfirmDialog(null, "Backup will read roaming frequencies from the device before saving them to a file.  Continue?", "WARNING",
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
      } 
      else {
        parent.setStatus("Backup roaming cancelled.");
        parent.do_roaming_backup=0;
        parent.do_read_roaming=0;
        return;
      }
    }
  byte[] image_buffer = new byte[128 * 1024 * 6];

  parent.did_freq_tests=0;

  for( int i=0; i< 128 * 1024 *6; i++) {
    image_buffer[i] = (byte) 0xff;
  }

    try {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() { 
          parent.jScrollPane8.getVerticalScrollBar().setValue(0);
        }
      });

    } catch(Exception e) {
    }

  int config_length = 0;

    try {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() { 
          parent.jScrollPane2.getVerticalScrollBar().setValue(0);
        }
      });

    } catch(Exception e) {
    }

  try {

    int state = -1; 

    int records_length=0;

    while(true) {


        if( parent.system_crc==0) {
          parent.do_read_roaming=0;
          return;
        }

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
          ByteBuffer bb_image = ByteBuffer.wrap(image_buffer);
          bb_image.order(ByteOrder.LITTLE_ENDIAN);

          parent.setProgress(5);
          parent.setStatus("Reading roaming from P25RX device..."); 

          int offset = 0;

          Hashtable freq_hash = new Hashtable();
          Hashtable rectype_hash = new Hashtable();

          while(true) {

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("6", 10) ); //read cfg flash
              bb.putInt( (int) new Long((long) 0x08160000 + offset).longValue() );  //address to return
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len  to return


              byte[] input_buffer = new byte[48];
              int rlen=0;
              while(rlen!=48) {
                serial_port.writeBytes( out_buffer, 48, 0); //16 + data len=0
                if(offset==0) {
                  SLEEP(500);
                }

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      SLEEP(1);
                      if(count++>500) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }

                rlen=serial_port.readBytes( input_buffer, 48 );

                if(rlen==48) {

                  ByteBuffer bb_verify = ByteBuffer.wrap(input_buffer);
                  bb_verify.order(ByteOrder.LITTLE_ENDIAN);
                  if( bb_verify.getInt()== 0xd35467A6) {//magic
                    bb_verify.getInt();  //op
                    if( bb_verify.getInt()==0x8160000+offset) { //address
                      break;
                    }
                    else {
                      rlen=0;  //need this to keep loop going
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                    }
                  }
                  else {
                    //System.out.println( String.format( "0x%08x", bb_verify.getInt(0) ) );
                    //System.out.println( String.format( "op 0x%08x", bb_verify.getInt(4) ) );
                    //System.out.println( String.format( "addr 0x%08x", bb_verify.getInt(8) ) );
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                  }
                }
                else {
                    byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                    if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                }
              }

              ByteBuffer bb2 = ByteBuffer.wrap(input_buffer);
              bb2.order(ByteOrder.LITTLE_ENDIAN);

              if( bb2.getInt()== 0xd35467A6) {//magic
                bb2.getInt();  //op
                int raddress = bb2.getInt();
                int rec_len = bb2.getInt();  //len

                byte[] rec_bytes = new byte[32];

                if(rec_len==32) {
                  for(int i=0;i<32-4;i++) {
                    rec_bytes[i] = bb2.get();
                  }
                  int crc = bb2.getInt();

                  int crc_32 = crc32.crc32_range(rec_bytes, 32-4);
                  offset+=32;

                  if(crc_32==crc) {
                    bb2 = ByteBuffer.wrap(input_buffer);
                    bb2.order(ByteOrder.LITTLE_ENDIAN);
                    bb2.getInt();
                    bb2.getInt();
                    bb2.getInt();
                    bb2.getInt();

                    for(int i=0;i<32;i++) {
                      bb_image.put( bb2.get() );
                    }
                    records_length+=32;

                  }
                  else {
                    if(get_offset_only==1) {
                      offset_only_length=records_length;
                      return;
                    }

                    //here image_buffer contains the talkgroup records
                    String home = System.getProperty("user.home");
                    String fs =  System.getProperty("file.separator");

                    JFileChooser chooser = new JFileChooser();

                    home = System.getProperty("user.home");
                    fs =  System.getProperty("file.separator");
                    File file = chooser.getCurrentDirectory();  //better for windows to do it this way
                    Path path = Paths.get(file.getAbsolutePath()+fs+"p25rx");
                    Files.createDirectories(path);

                    File cdir = new File(file.getAbsolutePath()+fs+"p25rx");
                    chooser.setCurrentDirectory(cdir);

                    FileNameExtensionFilter filter = new FileNameExtensionFilter( "p25rx_roaming backups", "rom");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showDialog(parent, "Backup Roaming Records");


                    if(returnVal == JFileChooser.APPROVE_OPTION) {
                      file = chooser.getSelectedFile();
                      String file_path = file.getAbsolutePath();
                      if(!file_path.endsWith(".rom")) {
                        file = new File(file_path+".rom");
                        file_path = file.getAbsolutePath();
                      }

                      parent.setStatus("backup roaming to "+file_path);

                      //System.out.println("opening file: "+file.getAbsolutePath());
                      FileOutputStream fos = new FileOutputStream(file);

                      byte[] header = new String("ROAM").getBytes();
                      fos.write(header,0,4);

                      fos.write(image_buffer,0,records_length);
                      fos.flush();
                      fos.close();
                    }

                    parent.setStatus("complete."); 
                    return;
                  }
                }
              }

          parent.setProgress(5+offset/32);
        }
     }

    } //while(true) 
  } catch (Exception e) {
    e.printStackTrace();
    parent.do_read_roaming=0;
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void read_roaming(BTFrame parent, SerialPort serial_port)
{
  this.serial_port = serial_port;
  this.parent = parent;

  byte[] image_buffer = new byte[128 * 1024 * 6];

  parent.did_freq_tests=0;

  for( int i=0; i< 128 * 1024 *6; i++) {
    image_buffer[i] = (byte) 0xff;
  }

  append_hash = new Hashtable();

    try {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() { 
          parent.jScrollPane8.getVerticalScrollBar().setValue(0);
        }
      });

    } catch(Exception e) {
    }

  int config_length = 0;

    try {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() { 
          parent.jScrollPane2.getVerticalScrollBar().setValue(0);
        }
      });

    } catch(Exception e) {
    }

  try {

    int state = -1; 


    while(true) {


        if( parent.system_crc==0) {
          parent.do_read_roaming=0;
          return;
        }

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
          parent.setStatus("Reading roaming from P25RX device..."); 

          int offset = 0;

          Hashtable freq_hash = new Hashtable();
          Hashtable rectype_hash = new Hashtable();
          parent.lat_lon_hash2 = new Hashtable();
          parent.no_loc_freqs = new Hashtable();

          while(true) {

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("6", 10) ); //read cfg flash
              bb.putInt( (int) new Long((long) 0x08160000 + offset).longValue() );  //address to return
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len  to return


              byte[] input_buffer = new byte[48];
              int rlen=0;
              while(rlen!=48) {
                serial_port.writeBytes( out_buffer, 48, 0); //16 + data len=0
                if(offset==0) {
                  SLEEP(500);
                }

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      SLEEP(1);
                      if(count++>500) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }

                rlen=serial_port.readBytes( input_buffer, 48 );

                if(rlen==48) {

                  ByteBuffer bb_verify = ByteBuffer.wrap(input_buffer);
                  bb_verify.order(ByteOrder.LITTLE_ENDIAN);
                  if( bb_verify.getInt()== 0xd35467A6) {//magic
                    bb_verify.getInt();  //op
                    if( bb_verify.getInt()==0x8160000+offset) { //address
                      break;
                    }
                    else {
                      rlen=0;  //need this to keep loop going
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                    }
                  }
                  else {
                    //System.out.println( String.format( "0x%08x", bb_verify.getInt(0) ) );
                    //System.out.println( String.format( "op 0x%08x", bb_verify.getInt(4) ) );
                    //System.out.println( String.format( "addr 0x%08x", bb_verify.getInt(8) ) );
                      byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                      if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                  }
                }
                else {
                    byte[] b = new byte[ serial_port.bytesAvailable()+1 ];
                    if(b.length>0)serial_port.readBytes( b, b.length-1 );  //flush buffer
                }
              }

              ByteBuffer bb2 = ByteBuffer.wrap(input_buffer);
              bb2.order(ByteOrder.LITTLE_ENDIAN);

              if( bb2.getInt()== 0xd35467A6) {//magic
                bb2.getInt();  //op
                int raddress = bb2.getInt();
                int rec_len = bb2.getInt();  //len

                byte[] rec_bytes = new byte[32];

                if(rec_len==32) {
                  for(int i=0;i<32-4;i++) {
                    rec_bytes[i] = bb2.get();
                  }
                  int crc = bb2.getInt();

                  int crc_32 = crc32.crc32_range(rec_bytes, 32-4);
                  offset+=32;

                  if(crc_32==crc) {
                    ByteBuffer bb4 = ByteBuffer.wrap(rec_bytes);
                    bb4.order(ByteOrder.LITTLE_ENDIAN);
                    double freq = bb4.getDouble();
                    byte result = bb4.get();
                    bb4.get();  //padding
                    bb4.get();  //padding
                    bb4.get();  //padding

                    double lat = bb4.getDouble();
                    double lon = bb4.getDouble();
                    if(lat!=0.0 && lon!=0.0) {
                      System.out.println("read freq "+String.format("%3.8f",freq) + ","+lat+","+lon); 
                      String freq_s = String.format("%3.8f", freq);
                      append_hash.put(freq_s,freq_s);

                      rectype_hash.put(freq_s,new Byte(result));
                      freq_hash.put(freq_s,freq_s);
                      parent.lat_lon_hash2.put(freq_s, new Point2D.Double(lat,lon) );
                    }
                    else if(lat==0.0 && lon==0.0) {
                      String freq_s = String.format("%3.8f", freq);
                      parent.no_loc_freqs.put(freq_s,freq_s);
                    }
                  }
                  else {
                    //if(parent.zs!=null) parent.zs.search3();

                    //pass lat_lon_hash to zip search3 to fill in the blanks
                    parent.do_read_roaming=0;
                    fill_freq_table(freq_hash, rectype_hash);

                    parent.do_read_roaming=0;
                    parent.setStatus("complete."); 
                    return;
                  }
                }
              }

          parent.setProgress(5+offset/32);
        }
     }

    } //while(true) 
  } catch (Exception e) {
    e.printStackTrace();
    parent.do_read_roaming=0;
  }
}

/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
public void fill_freq_table(Hashtable freq_hash, Hashtable rectype_hash) {
 if(parent==null || parent.freq_table==null) return;

   Hashtable found_freqs = new Hashtable();

   int first_null=0;
   for(int i=0;i<250;i++) {
     String val =  (String) parent.freq_table.getModel().getValueAt(i,3);
     if(val!=null && freq_hash.get(val)!=null ) {
       parent.freq_table.getModel().setValueAt("X", i, 6);
       byte rectype = 0; 
       try {
         rectype = ((Byte) rectype_hash.get(val)).byteValue();
       } catch(Exception e) {
         rectype=0;
       }
       String rec="";
       if(rectype==1) rec="P1";
       if(rectype==2) rec="P25CC";
       //if(rectype==3) rec="NOSIG";
       if(rectype==3) rec="";
       if(rectype==4) rec="P25CC";
       //System.out.println("rectype "+rectype+"  rec "+rec);
       parent.freq_table.getModel().setValueAt(rec,i,5);

       if(parent.frequency_tf1.getText().trim().equals(val.trim())) {
         parent.freq_table.getModel().setValueAt("PRIM",i,4);
       } 
       else {
         parent.freq_table.getModel().setValueAt("",i,4);
       }

       found_freqs.put(val,val);
     }
     else {
       parent.freq_table.getModel().setValueAt(null, i, 5);
       parent.freq_table.getModel().setValueAt(null, i, 6);
       if(first_null==0 && val==null) {
         first_null=i;
         if(i==0) break;
       }
     }
   }

   int do_s3=0;
   if(first_null==0) do_s3=1;

   List<String> tmp = Collections.list(freq_hash.keys());
   Collections.sort(tmp);
   Iterator<String> it = tmp.iterator();
   while(it.hasNext()) {
     String f = it.next();
     if( found_freqs.get(f)==null ) {
       parent.freq_table.getModel().setValueAt(f, first_null, 3);
       parent.freq_table.getModel().setValueAt("X", first_null, 6);
       byte rectype = 0; 
       try {
         rectype = ((Byte) rectype_hash.get(f)).byteValue();
       } catch(Exception e) {
       }
       String rec="";
       if(rectype==1) rec="P1";
       if(rectype==2) rec="P25CC";
       //if(rectype==3) rec="NOSIG";
       if(rectype==3) rec="";
       if(rectype==4) rec="P25CC";

       if(parent.frequency_tf1.getText().trim().equals(f.trim())) {
         parent.freq_table.getModel().setValueAt("PRIM",first_null,4);
       } 
       parent.freq_table.getModel().setValueAt(rec,first_null,5);
       //parent.freq_table.getModel().setValueAt("IN FLASH",first_null,1);
       first_null++;
       if(first_null==250) break;
     }
   }


  if(parent.zs!=null && do_s3==1) {
    parent.zs.search3();
  }
  else {
     int idx=0;
     for(int i=0;i<250;i++) {
       if(parent.freq_table.getModel().getValueAt(i,3)!=null && parent.freq_table.getModel().getValueAt(i,0)==null) {
         break;
       }
       idx++;
     }
     List<String> tmp2 = Collections.list(parent.no_loc_freqs.keys());
     Collections.sort(tmp2);
     Iterator<String> it2 = tmp2.iterator();
     while(it2.hasNext() && idx<250) {
       String p25f = it2.next();
       System.out.println("no loc freq "+p25f);
       parent.freq_table.getModel().setValueAt( p25f, idx, 3); 
       parent.freq_table.getModel().setValueAt( "X", idx, 6); 

       if(parent.frequency_tf1.getText().trim().equals(p25f.trim())) {
         parent.freq_table.getModel().setValueAt("PRIM",idx,4);
       } 
       else {
         parent.freq_table.getModel().setValueAt("",idx,4);
       }



       idx++;
     }
  }
}
////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////
public void set_freq_binary(String freq_d) {

    byte[] out_buffer = new byte[16+32]; //size of bl_op
    ByteBuffer bb = ByteBuffer.wrap(out_buffer);
    bb.order(ByteOrder.LITTLE_ENDIAN);

    bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
    bb.putInt( (int) Long.parseLong("7", 10) ); //test freq op
    bb.putInt( (int) new Long((long) 0x55555555 ).longValue() );  //address to return
    bb.putInt( (int) Long.parseLong("32", 10) );  //data len  to return

    double freq=0.0;

    try {
      freq = (double) Double.parseDouble(freq_d);
      if( !is_valid_freq(freq) ) {
        return;
      }
    } catch(Exception e) {
    }

    bb.putDouble( (double) freq ); //freq to test 

    byte[] input_buffer = new byte[48];
    int rlen=0;
    while(rlen!=48) {
      //send new freq
      System.out.println("send new freq");
      serial_port.writeBytes( out_buffer, 48, 0); //16 + data len=0

        try {
          int count=0;
          while(serial_port.bytesAvailable()<48) {
            SLEEP(1);
            if(count++>2000) {
              System.out.println("test freq timeout");
              break;
            }
          }
        } catch(Exception e) {
          e.printStackTrace();
        }

      rlen=serial_port.readBytes( input_buffer, 48 );
      if(rlen==48) {
        break;
      }
    }

    ByteBuffer bb2 = ByteBuffer.wrap(input_buffer);
    bb2.order(ByteOrder.LITTLE_ENDIAN);


    if( bb2.getInt()== 0xd35467A6) {//magic
      int op = bb2.getInt();  //op
      int addr = bb2.getInt();  //address   //this should be 0x55555555
      int slen = bb2.getInt();  //len
      byte[] retb = new byte[slen];
      for(int i=0;i<slen;i++) {
        retb[i] = bb2.get();
      }
      if(op==4 && addr==0x55555555) {

        if(new String(retb).equals("P25")) {
          retb = new String("P1").getBytes();
        }
     }
   }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
private void SLEEP(long val) {
  try {
    if(parent.is_windows==1) {
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
  }
}
}
