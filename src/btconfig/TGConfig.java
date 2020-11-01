
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

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
class TGConfig
{

java.util.Timer utimer;
SerialPort serial_port;
java.text.SimpleDateFormat formatter_date;
int did_write_tg=0;
java.util.Hashtable tg_hash;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void addUknownTG(BTFrame parent, String talkgroup, String sys_id, String city, String wacn) {
  int first_empty_row=0;

  if(talkgroup==null || sys_id==null || wacn==null) return;

  talkgroup = talkgroup.replace(",","");
  talkgroup = talkgroup.trim();

  String wacn_hex = Integer.toString( new Integer(wacn).intValue(), 16);

  try {
    int tg = new Integer(talkgroup).intValue();
    if(tg==0) return;
    int wacn_i = new Integer(wacn).intValue();
    if(wacn_i==0) return;
  } catch(Exception e) {
  }


  sys_id = sys_id.trim();

  if(tg_hash==null) tg_hash = new java.util.Hashtable();
    else tg_hash.clear();

  for(int i=0;i<800;i++) {
    try {
      Object o1 = parent.getTableObject(i,1);
      Object o2 = parent.getTableObject(i,3);
      Object o3 = parent.getTableObject(i,6);
      if(o1!=null && o2!=null && o3!=null)  {
        tg_hash.put(o1.toString().trim()+"_"+o2.toString().trim()+"_"+o3.toString().trim(),  
          o1.toString().trim()+"_"+o2.toString().trim()+"_"+o3.toString().trim());
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  if(tg_hash.get(sys_id+"_"+talkgroup+"_"+wacn_hex)!=null) return;  //already found this one

  try {
    int i = new Integer(talkgroup).intValue();
  } catch(Exception e) {
    e.printStackTrace();
    return;
  }

  for(int i=0;i<800;i++) {
    try {
      Object o1 = parent.getTableObject(i,1);
      Object o2 = parent.getTableObject(i,3);
      Object o3 = parent.getTableObject(i,6);
      if(o1!=null && o2!=null && o3!=null)  {
        first_empty_row++;
      }
      else {
        break;
      }

    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  System.out.println("first empty row "+first_empty_row);

  int idx = first_empty_row;

    try {

        parent.addTableObject( true, idx, 0);
        parent.addTableObject( new Integer(sys_id), idx, 1);
        parent.addTableObject( new Integer(1), idx, 2);
        parent.addTableObject( new Integer(talkgroup), idx, 3);
        parent.addTableObject( new String(talkgroup+"_unknown"), idx, 4);

        //parent.addTableObject( new String(talkgroup+"_unknown"), idx, 5);
        parent.addTableObject( city, idx, 5);


        parent.addTableObject( wacn_hex, idx, 6);

        if(parent.did_read_talkgroups==1 && parent.auto_flash_tg.isSelected()) parent.tg_update_pending=1;  //write them to flash

     } catch(Exception e) {
      e.printStackTrace();
     }

}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void disable_enc_tg(BTFrame parent, String talkgroup, String sys_id) {
  int first_empty_row=0;

  if(talkgroup==null || sys_id==null) return;

  talkgroup = talkgroup.trim();
  sys_id = sys_id.trim();

  int tg1 = new Integer(talkgroup).intValue();
  int sys1 = new Integer(sys_id).intValue();

  for(int i=0;i<800;i++) {
    try {
      Object o1 = parent.getTableObject(i,1);
      Object o2 = parent.getTableObject(i,3);
      if(o1!=null && o2!=null)  {
        if( tg1 == ( (Integer) o2 ).intValue() && sys1 ==( (Integer) o1 ).intValue() ) {

          if( ((String) parent.getTableObject(i,4)).contains("unknown") ) {
            if(parent.did_read_talkgroups==1 && parent.disable_encrypted.isSelected()) {
              parent.addTableObject( false, i, 0);  //disable
              parent.addTableObject( new String(talkgroup+"_encrypted") , i, 4);  
              parent.tg_update_pending=1;  //write them to flash
            }
          }
          return;
        }
      }
    } catch(Exception e) {
      //e.printStackTrace();
    }
  }

}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void restore_talkgroups(BTFrame parent, BufferedInputStream bis, SerialPort serial_port)
{
  this.serial_port = serial_port;

  byte[] image_buffer = new byte[128 * 1024 * 6];


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
    if(!header_str.equals("TALK")) {
      System.out.println("bad talk group file");
      JOptionPane.showMessageDialog(parent, "invalid Talk Group file format.  Could be from an older version of the software");
      return;
    }


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
            parent.setStatus("Writing talkgroups to P25RX device..."); 

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
              bb.putInt( (int) new Long((long) 0x08120000 + offset).longValue() );
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
                  Thread.sleep(500);
                }

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      Thread.sleep(1);
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
                    if( op==4 && bb_verify.getInt()==0x8120000+offset) { //address
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
              Thread.sleep(100);
            } catch(Exception e) {
            }

            parent.setStatus("\r\nCompleted restoring talkgroups.");
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
public void send_talkgroups(BTFrame parent, SerialPort serial_port)
{
  this.serial_port = serial_port;

  byte[] image_buffer = new byte[128 * 1024 * 6];


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
            //Boolean b;
            //if(enabled==1) b=true;
            //  else b=false;
            //parent.addTableObject( b, i, 0);
            //parent.addTableObject( new Integer(sys_id), i, 1);
            //parent.addTableObject( new Integer(priority), i, 2);
            //parent.addTableObject( new Integer(talkgroup), i, 3);
            //parent.addTableObject( new String(desc), i, 4);
            //parent.addTableObject( new String(loc), i, 5);

            int nrecs=0;
            ByteBuffer bb_image = ByteBuffer.wrap(image_buffer);
            bb_image.order(ByteOrder.LITTLE_ENDIAN);

            for(int i=0;i<800;i++) {
              try {
                Boolean enabled = (Boolean) parent.getTableObject(i, 0);
                Integer sys_id = (Integer) parent.getTableObject(i, 1);
                Integer priority = (Integer) parent.getTableObject(i, 2);
                Integer talkgroup = (Integer) parent.getTableObject(i, 3);
                String desc = (String) parent.getTableObject(i,4);
                String loc = (String) parent.getTableObject(i,5);
                //String wacn = (String) parent.getTableObject(i,6);

                /*
                System.out.println("\r\n\r\n");
                System.out.println("enabled: "+enabled);
                System.out.println("sys_id: "+sys_id);
                System.out.println("priority: "+priority);
                System.out.println("talkgroup: "+talkgroup);
                System.out.println("desc: "+desc);
                System.out.println("loc: "+loc);
                */

                if(sys_id!=null && priority!=null && talkgroup!=null && desc!=null && loc!=null ) {
                  //if(enabled==null) enabled = new Boolean(true);
                  nrecs++;
                }
              } catch(Exception e) {
              }
            }

            if(nrecs<=0) {
              parent.setStatus("No records to write.");
              return;
            }
            else {
              parent.setStatus(nrecs+" records.");
            }

            bb_image.putInt(nrecs); //number of records is 1st 4 bytes
            config_length=4;

            int nrecs_w = 0;
            for(int i=0;i<800;i++) {
              try {
                Boolean enabled = (Boolean) parent.getTableObject(i, 0);
                Integer sys_id = (Integer) parent.getTableObject(i, 1);
                Integer priority = (Integer) parent.getTableObject(i, 2);
                Integer talkgroup = (Integer) parent.getTableObject(i, 3);
                String desc = (String) parent.getTableObject(i,4);
                String loc = (String) parent.getTableObject(i,5);
                Integer wacn = (Integer) Integer.valueOf( (String) parent.getTableObject(i,6), 16 );

                desc = desc.trim();
                loc = loc.trim();

                /*
                System.out.println("\r\n\r\n");
                System.out.println("enabled: "+enabled);
                System.out.println("sys_id: "+sys_id);
                System.out.println("priority: "+priority);
                System.out.println("talkgroup: "+talkgroup);
                System.out.println("desc: "+desc);
                System.out.println("loc: "+loc);
                */

                if(sys_id!=null && priority!=null && talkgroup!=null && desc!=null && loc!=null && wacn!=null) {


                  int en = 0;
                  if(enabled==null || enabled==false) en=0; 
                    else en=1;

                  int wacn_sys_id = sys_id.intValue() + ( wacn.intValue()*4096 );

                  bb_image.putInt(en);
                  bb_image.putInt(wacn_sys_id);
                  bb_image.putInt(priority);
                  bb_image.putInt(talkgroup);
                  byte[] b = new byte[32];
                  byte[] dbytes = desc.getBytes();
                  byte[] lbytes = loc.getBytes();

                  for(int j=0;j<32;j++) {
                    if(j<dbytes.length) b[j] = dbytes[j];
                      else b[j]=0;
                    bb_image.put(b[j]);
                  }
                  for(int j=0;j<32;j++) {
                    if(j<lbytes.length) b[j] = lbytes[j];
                      else b[j]=0;
                    bb_image.put(b[j]);
                  }

                  config_length+=80;  //length of record
                  nrecs_w++;
                  if(nrecs_w==nrecs) break;
                }
              } catch(Exception e) {
              }
            }

            //parent.setStatus("nrecs "+nrecs);
            //if(true) return;

            parent.setProgress(5);
            parent.setStatus("Writing talkgroups to P25RX device..."); 

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
              bb.putInt( (int) new Long((long) 0x08120000 + offset).longValue() );
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
                  Thread.sleep(500);
                }

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      Thread.sleep(1);
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
                    if( op==4 && bb_verify.getInt()==0x8120000+offset) { //address
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
              Thread.sleep(100);
            } catch(Exception e) {
            }

            parent.setStatus("\r\nCompleted sending talkgroups.");
            parent.setProgress(100);
            did_write_tg=1;
            return;
        }

    } //while(true) 
  } catch (Exception e) {
    e.printStackTrace();
  }
}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void read_talkgroups(BTFrame parent, SerialPort serial_port)
{
  this.serial_port = serial_port;

  byte[] image_buffer = new byte[128 * 1024 * 6];

  for( int i=0; i< 128 * 1024 *6; i++) {
    image_buffer[i] = (byte) 0xff;
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

    if(did_write_tg==0 && parent.do_talkgroup_backup==1) {

      if(JOptionPane.showConfirmDialog(null, "Backup will read talk groups from the device before saving them to a file.  Continue?", "WARNING",
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
      } 
      else {
        parent.setStatus("Backup talkgroups cancelled.");
        parent.do_talkgroup_backup=0;
        parent.did_tg_backup=1;
        parent.do_read_talkgroups=0;
        return;
      }
    }

    while(true) {


        if( parent.system_crc==0) {
          parent.do_read_talkgroups=0;
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
          parent.setStatus("Reading talkgroups from P25RX device..."); 

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
              bb.putInt( (int) new Long((long) 0x08120000 + offset).longValue() );  //address to return
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len  to return



              byte[] input_buffer = new byte[48];
              int rlen=0;
              while(true) {

                serial_port.writeBytes( out_buffer, 48, 0); //16 + data len=0

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      Thread.sleep(1);
                      if(count++>500) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }

                rlen=serial_port.readBytes( input_buffer, 48 );

                if(rlen==48) {
                  ByteBuffer bb_verify = ByteBuffer.wrap(input_buffer);
                  bb_verify.order(ByteOrder.LITTLE_ENDIAN);


                  if( bb_verify.getInt()== (int) 0xd35467A6L) {//magic

                    bb_verify.getInt();  //op
                    if( bb_verify.getInt()==0x8120000+offset) { //address
                      bb_verify.getInt();  //len
                      nrecs = bb_verify.getInt();
                      if(nrecs>2048) nrecs=2048;
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

                if(nrecs>0) break;
              }

            if(nrecs>0) break;
          }

          if(nrecs>0) {
            parent.setStatus("\r\nCompleted reading talkgroups. nrecs: "+nrecs);
          }
          else {
            parent.setStatus("\r\nNo talkgroup records found.");
          }
          parent.setProgress(10);


          offset = 4; //skip the nrecs int

          while(true) {

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("6", 10) ); //read cfg flash
              bb.putInt( (int) new Long((long) 0x08120000 + offset).longValue() );  //address to return
              bb.putInt( (int) Long.parseLong("32", 10) );  //data len  to return


              byte[] input_buffer = new byte[48];
              int rlen=0;
              while(rlen!=48) {
                serial_port.writeBytes( out_buffer, 48, 0); //16 + data len=0
                if(offset==0) {
                  Thread.sleep(500);
                }

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      Thread.sleep(1);
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
                    if( bb_verify.getInt()==0x8120000+offset) { //address
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
                int raddress = (bb2.getInt()-0x08120000-4) ;  //address
                bb2.getInt();  //len

                if(raddress>=0) {
                  for(int i=0;i<32;i++) {
                    image_buffer[i+raddress] = bb2.get();
                  }

                  offset+=32;
                  if(offset > nrecs*80) { //finished?

                  //read records
                  //
                  /*
                  typedef struct {
                    uint32_t enabled;
                    int32_t sys_id;
                    int32_t  priority;
                    int32_t talkgroup;
                    char    desc[32];
                    char    location[32];
                  } tgrecord;
                  */

                    ByteBuffer bb3 = ByteBuffer.wrap(image_buffer);
                    bb3.order(ByteOrder.LITTLE_ENDIAN);

                    byte[] desc = new byte[32];
                    byte[] loc = new byte[32];

                    for(int i=0;i<nrecs;i++) {

                      int enabled = bb3.getInt();
                      int wacn = bb3.getInt();
                      int sys_id = (wacn&0xfff);
                      wacn = wacn>>>12; 

                      System.out.println("wacn_str: "+Integer.toString(wacn,16));

                      int priority = bb3.getInt();
                      int talkgroup = bb3.getInt();

                      //temporary,  switch from NAC to SYS_ID
                //if(sys_id==832) sys_id=842;
                //if(sys_id==1361) sys_id=1365;
                //if(sys_id==1817) sys_id=1813;

                      for(int j=0;j<32;j++) {
                        desc[j] = bb3.get();
                      }
                      for(int j=0;j<32;j++) {
                        loc[j] = bb3.get();
                      }

                      //System.out.println("\r\n\r\n");
                      //System.out.println("enabled: "+enabled);
                      //System.out.println("sys_id: "+sys_id);
                      //System.out.println("priority: "+priority);
                      //System.out.println("talkgroup: "+talkgroup);
                      //System.out.println("description: "+new String(desc));
                      //System.out.println("location: "+new String(loc));

                      Boolean b;
                      if(enabled==1) b=true;
                        else b=false;
                      parent.addTableObject( b, i, 0);
                      parent.addTableObject( new Integer(sys_id), i, 1);
                      parent.addTableObject( new Integer(priority), i, 2);
                      parent.addTableObject( new Integer(talkgroup), i, 3);
                      parent.addTableObject( new String(desc).trim(), i, 4);
                      parent.addTableObject( new String(loc).trim(), i, 5);

                      parent.addTableObject( Integer.toString(wacn,16) , i, 6);

                    }

                    if(nrecs<800) {
                      for(int i=nrecs;i<800;i++) {

                        parent.addTableObject( null, i, 0);
                        parent.addTableObject( null, i, 1);
                        parent.addTableObject( null, i, 2);
                        parent.addTableObject(  null,i, 3);
                        parent.addTableObject(  null,i, 4);
                        parent.addTableObject(  null,i, 5);
                        parent.addTableObject(  null,i, 6);

                      }
                    }

                    //make a backup
                    if(nrecs>0 && parent.did_tg_backup==0) {
                      try {
                        byte[] blen = new byte[4];
                        ByteBuffer bb4 = ByteBuffer.wrap(blen);
                        bb4.order(ByteOrder.LITTLE_ENDIAN);
                        bb4.putInt(nrecs);

                        //here image_buffer contains the talkgroup records
                        String home = System.getProperty("user.home");
                        String fs =  System.getProperty("file.separator");


                        formatter_date = new java.text.SimpleDateFormat( "yyyy-MM-dd" );
                        String date = formatter_date.format(new java.util.Date() );

                        //String path = home+fs+"p25rx_talkgroup_backup_"+date+".bin";
                        //System.out.println("opening file: "+path);

                        if(parent.do_talkgroup_backup==0) {
                          //File file = new File(path);
                          JFileChooser chooser = new JFileChooser();

                          File file = chooser.getCurrentDirectory();  //better for windows to do it this way
                          Path path = Paths.get(file.getAbsolutePath()+fs+"p25rx");
                          Files.createDirectories(path);

                          file = new File(file.getAbsolutePath()+fs+"p25rx"+fs+"p25rx_talkgroup_backup_"+date+".tgp");

                          System.out.println("opening file: "+file.getAbsolutePath());

                          FileOutputStream fos = new FileOutputStream(file);

                          byte[] header = new String("TALK").getBytes();
                          fos.write(header,0,4);

                          fos.write(blen,0,4);  //write Int num records
                          fos.write(image_buffer,0,nrecs*80);
                          fos.flush();
                          fos.close();

                          parent.setStatus("backup talkgroups to "+file.getAbsolutePath());

                          parent.did_tg_backup=1;
                        }
                        else if(parent.do_talkgroup_backup==1) {

                          parent.do_talkgroup_backup=0;

                          JFileChooser chooser = new JFileChooser();

                          home = System.getProperty("user.home");
                          fs =  System.getProperty("file.separator");
                          File file = chooser.getCurrentDirectory();  //better for windows to do it this way
                          Path path = Paths.get(file.getAbsolutePath()+fs+"p25rx");
                          Files.createDirectories(path);

                          File cdir = new File(file.getAbsolutePath()+fs+"p25rx");
                          chooser.setCurrentDirectory(cdir);

                          FileNameExtensionFilter filter = new FileNameExtensionFilter( "p25rx_talkgroup backups", "tgp");
                          chooser.setFileFilter(filter);
                          int returnVal = chooser.showOpenDialog(parent);


                          if(returnVal == JFileChooser.APPROVE_OPTION) {
                            file = chooser.getSelectedFile();
                            String file_path = file.getAbsolutePath();
                            if(!file_path.endsWith(".tgp")) {
                              file = new File(file_path+".tgp");
                              file_path = file.getAbsolutePath();
                            }

                            parent.setStatus("backup talkgroups to "+file_path);

                            //System.out.println("opening file: "+file.getAbsolutePath());
                            FileOutputStream fos = new FileOutputStream(file);

                            byte[] header = new String("TALK").getBytes();
                            fos.write(header,0,4);

                            fos.write(blen,0,4);  //write Int num records
                            fos.write(image_buffer,0,nrecs*80);
                            fos.flush();
                            fos.close();
                          }

                          parent.did_tg_backup=1;
                        }

                      } catch(Exception e) {
                        e.printStackTrace();
                      }
                    }

                    return; 
                  }

                  //parent.setStatus("read "+offset+" bytes");
                  parent.setStatus("read "+nrecs+" talkgroups."); 
                  parent.setProgress( (int) ((float)offset/((float)nrecs*(float)80) * 100.0) );

                  parent.do_read_talkgroups=0;
                  parent.did_read_talkgroups=1;
                }
              }
          }


        }

    } //while(true) 
  } catch (Exception e) {
    e.printStackTrace();
  }
}


}
