
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

int NRECS=4915; //3 banks of 128k flash with record size of 80 bytes

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void addUknownTG(BTFrame parent, String talkgroup, String sys_id, String city, String wacn) {
  int first_empty_row=0;

  if(talkgroup==null || sys_id==null ) return;

  if(parent.is_dmr_mode==0 && wacn==null) {
    return; //p25
  }
  else if(parent.is_dmr_mode==1) {
    wacn = "0"; //dmr
  }

  talkgroup = talkgroup.replace(",","");
  talkgroup = talkgroup.trim();

  String wacn_hex = "0x"+Integer.toString( new Integer(wacn).intValue(), 16);
  String sys_id_hex = "0x"+Integer.toString( new Integer(sys_id).intValue(), 16);

  try {
    int tg = new Integer(talkgroup).intValue();
    if(tg==0) return;
    int wacn_i = new Integer(wacn).intValue();
    if(wacn_i==0 && parent.is_dmr_mode==0) return;
  } catch(Exception e) {
    return;
  }


  sys_id = sys_id_hex.trim();

  if(tg_hash==null) tg_hash = new java.util.Hashtable();
    else tg_hash.clear();

  for(int i=0;i<NRECS;i++) {
    try {
      Object o1 = parent.getTableObject(i,1);
      Object o2 = parent.getTableObject(i,3);
      Object o3 = parent.getTableObject(i,6);

      if(parent.is_dmr_mode==1) o3 = new String("0"); //don't pay attention to WACN for DMR

      if(o1!=null && o2!=null && o3!=null)  {
        tg_hash.put(o1.toString().trim()+"_"+o2.toString().trim()+"_"+o3.toString().trim(),  
          o1.toString().trim()+"_"+o2.toString().trim()+"_"+o3.toString().trim());
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }


  if(parent.is_dmr_mode==1) wacn_hex="0"; 

  if(tg_hash.get(sys_id_hex.trim()+"_"+talkgroup.trim()+"_"+wacn_hex.trim())!=null) return;  //already found this one

  try {
    int i = new Integer(talkgroup).intValue();
  } catch(Exception e) {
    e.printStackTrace();
    return;
  }

  for(int i=0;i<NRECS;i++) {
    try {
      Object o1 = parent.getTableObject(i,1);
      Object o2 = parent.getTableObject(i,3);
      Object o3 = parent.getTableObject(i,6);

      if(parent.is_dmr_mode==1) o3 = new String("0"); //don't pay attention to WACN for DMR

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
        parent.addTableObject( new String(sys_id_hex), idx, 1);
        parent.addTableObject( new Integer(1), idx, 2);
        parent.addTableObject( new Integer(talkgroup), idx, 3);
        parent.addTableObject( new String(talkgroup+"_unknown"), idx, 4);
        parent.addTableObject( city, idx, 5);
        parent.addTableObject( new String(wacn_hex), idx, 6);

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
  String sys_id_hex = "0x"+Integer.toString( new Integer(sys_id).intValue(), 16);

  for(int i=0;i<NRECS;i++) {
    try {
      Object o1 = parent.getTableObject(i,1);
      Object o2 = parent.getTableObject(i,3);
      if(o1!=null && o2!=null)  {
        if( tg1 == ( (Integer) o2 ).intValue() && sys_id_hex.contains( (String) o1) ) {

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
public void import_talkgroups_csv(BTFrame parent, LineNumberReader lnr, SerialPort serial_port)
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

  String header_line="";

  try {

    header_line = lnr.readLine();

    //ENABLED,SYS_ID_HEX,PRIORITY,TGRP,ALPHATAG,DESCRIPTION,WACN_HEX

    if( !header_line.toUpperCase().contains("ENABLED") &&
        !header_line.toUpperCase().contains("SYS_ID_HEX") &&
        !header_line.toUpperCase().contains("PRIORITY") &&
        !header_line.toUpperCase().contains("TGRP") &&
        !header_line.toUpperCase().contains("ALPHATAG") &&
        !header_line.toUpperCase().contains("DESCRIPTION") &&
        !header_line.toUpperCase().contains("WACN_HEX")
    ) {
      JOptionPane.showMessageDialog(parent, "No valid records found. invalid talk group csv file format.  First line must contain column headers");
      return;
    }

    int config_length=0;
    //int config_length = bis.read(image_buffer, 0, 128*1024*6);

    int number_of_records=0;

    ByteBuffer bb_csv = ByteBuffer.wrap(image_buffer);
    bb_csv.order(ByteOrder.LITTLE_ENDIAN);

    bb_csv.putInt(0);  //temporary length,  re-write at end when nrecords is known

    String in_line="";

    while(number_of_records<NRECS) {

      in_line = lnr.readLine();
      if(in_line==null) break;

      in_line = in_line.trim();

      //String[] strs = in_line.split(",");
      String[] strs = null;
      
      if(in_line!=null && in_line.length()>10) strs = in_line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

      if(strs!=null) { 

          String str1="";
          String str2="";
          String str3="";
          String str4="";
          String str5="";
          String str6="";
          String str7="";

          if(strs[0]!=null) str1 = strs[0];  
          if(strs[1]!=null) str2 = strs[1]; 
          if(strs[2]!=null) str3 = strs[2]; 
          if(strs[3]!=null) str4 = strs[3]; 
          if(strs[4]!=null) str5 = strs[4]; 
          if(strs[5]!=null) str6 = strs[5]; 
          if(strs[6]!=null) str7 = strs[6]; 

          System.out.println(":"+str1+":"+str2+":"+str3+":"+str4+":"+str5+":"+str6+":"+str7+":");

          if(str7!=null && str7.length()>0 && str7.length()>7) {
            str7 = str7.substring(0,7);
          }

          int en = 0;

          if( str1.trim().equals("1") || str1.toUpperCase().equals("TRUE") ) en=1;
            else en=0;

          if( str2!=null && str2.startsWith("0x") ) str2 = str2.substring(2,str2.length()); 
          if( str2!=null && str2.startsWith("0X") ) str2 = str2.substring(2,str2.length()); 

          if( str7!=null && str7.startsWith("0x") ) str7 = str7.substring(2,str7.length()); 
          if( str7!=null && str7.startsWith("0X") ) str7 = str7.substring(2,str7.length()); 

          Integer sys_id = Integer.valueOf(str2,16);
          Integer wacn = Integer.valueOf(str7,16);

          if(wacn.intValue()==0) wacn = 0xbee00; //assume this

          int wacn_sys_id = sys_id.intValue() + ( wacn.intValue()*4096 );

          int priority = Integer.parseInt( str3 );
          int talkgroup = Integer.parseInt( str4 );

          byte[] dbytes = str5.getBytes();  //alpha
          byte[] lbytes = str6.getBytes();  //description

          bb_csv.putInt(en);
          bb_csv.putInt(wacn_sys_id);
          bb_csv.putInt(priority);
          bb_csv.putInt(talkgroup);
          byte[] b = new byte[32];

          //alpha
          for(int j=0;j<32;j++) {
            if(j<dbytes.length) b[j] = dbytes[j];
              else b[j]=0;
            bb_csv.put(b[j]);
          }
          //desc
          for(int j=0;j<32;j++) {
            if(j<lbytes.length) b[j] = lbytes[j];
              else b[j]=0;
            bb_csv.put(b[j]);
          }

        number_of_records++;
      }

    }

    if(number_of_records==0) {
      JOptionPane.showMessageDialog(parent, "invalid talk group csv file format.  First line must contain column headers.  Values cannot be null");
      return;
    }


    config_length = (number_of_records*80)+4;

    //update the number of records
    bb_csv.putInt(0,number_of_records);
    System.out.println("number of records imported: "+number_of_records);


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
              bb.putInt( (int) new Long((long) 0x08180000 + offset).longValue() );
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
                    if( op==4 && bb_verify.getInt()==0x8180000+offset) { //address
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

            parent.setStatus("\r\nCompleted importing talkgroups.");
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
              bb.putInt( (int) new Long((long) 0x08180000 + offset).longValue() );
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
                    if( op==4 && bb_verify.getInt()==0x8180000+offset) { //address
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

            for(int i=0;i<NRECS;i++) {
              try {
                Boolean enabled = (Boolean) parent.getTableObject(i, 0);
                String sys_id = (String) parent.getTableObject(i, 1);
                Integer priority = (Integer) parent.getTableObject(i, 2);
                Integer talkgroup = (Integer) parent.getTableObject(i, 3);
                String desc = (String) parent.getTableObject(i,4);
                String loc = (String) parent.getTableObject(i,5);
                String wacn = (String) parent.getTableObject(i,6);
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

                if(sys_id!=null && priority!=null && talkgroup!=null && desc!=null && loc!=null && wacn!=null) {
                  //if(enabled==null) enabled = new Boolean(true);
                  nrecs++;
                }
              } catch(Exception e) {
              }
            }

            if(nrecs==0) {
              int i=0;
              parent.addTableObject( true, i, 0);

              parent.addTableObject( "0x123", i, 1);

              parent.addTableObject( new Integer(1), i, 2);
              parent.addTableObject( new Integer(1), i, 3);
              parent.addTableObject( new String("alpha").trim(), i, 4);
              parent.addTableObject( new String("desc").trim(), i, 5);

              parent.addTableObject( "0x456", i, 6);
              nrecs++;
            }

            if(nrecs<=0) {
              parent.setStatus("No records to write.");
              return;
            }
            else {
              parent.setStatus(nrecs+" records.");
            }

            if(nrecs>NRECS) {
              nrecs=NRECS;
            }

            bb_image.putInt(nrecs); //number of records is 1st 4 bytes
            config_length=4;

            int nrecs_w = 0;
            for(int i=0;i<NRECS;i++) {
              try {
                Boolean enabled = (Boolean) parent.getTableObject(i, 0);
                Integer priority = (Integer) parent.getTableObject(i, 2);
                Integer talkgroup = (Integer) parent.getTableObject(i, 3);
                String desc = (String) parent.getTableObject(i,4);
                String loc = (String) parent.getTableObject(i,5);


                String sys_id_str = (String) parent.getTableObject(i,1);
                if( sys_id_str!=null && sys_id_str.startsWith("0x") ) sys_id_str = sys_id_str.substring(2,sys_id_str.length()); 
                if( sys_id_str!=null && sys_id_str.startsWith("0X") ) sys_id_str = sys_id_str.substring(2,sys_id_str.length()); 

                String wacn_str = (String) parent.getTableObject(i,6);
                if( wacn_str!=null && wacn_str.startsWith("0x") ) wacn_str = wacn_str.substring(2,wacn_str.length()); 
                if( wacn_str!=null && wacn_str.startsWith("0X") ) wacn_str = wacn_str.substring(2,wacn_str.length()); 

                Integer wacn = (Integer) Integer.valueOf( wacn_str, 16 );
                Integer sys_id = (Integer) Integer.valueOf( sys_id_str, 16 );

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
                  if(nrecs_w>=nrecs) break;
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
              bb.putInt( (int) new Long((long) 0x08180000 + offset).longValue() );
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
                    if( op==4 && bb_verify.getInt()==0x8180000+offset) { //address
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


          int nrecs=-1;
          int timeout=0;
          while(true) {

              if(timeout++>10) break;

              byte[] out_buffer = new byte[16+32]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("6", 10) ); //read cfg flash
              bb.putInt( (int) new Long((long) 0x08180000 + offset).longValue() );  //address to return
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
                    if( bb_verify.getInt()==0x8180000+offset) { //address
                      bb_verify.getInt();  //len

                      nrecs = bb_verify.getInt();
                      if(nrecs>NRECS || nrecs < 0) nrecs=0;

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

                if(nrecs>=0) break;
              }

            if(nrecs>=0) break;
          }

          if(nrecs>=0) {
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
              bb.putInt( (int) new Long((long) 0x08180000 + offset).longValue() );  //address to return
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
                    if( bb_verify.getInt()==0x8180000+offset) { //address
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
                int raddress = (bb2.getInt()-0x08180000-4) ;  //address
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

                      //System.out.println("wacn_str: "+Integer.toString(wacn,16));

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

                      parent.addTableObject( "0x"+Integer.toString(sys_id,16) , i, 1);

                      parent.addTableObject( new Integer(priority), i, 2);
                      parent.addTableObject( new Integer(talkgroup), i, 3);
                      parent.addTableObject( new String(desc).trim(), i, 4);
                      parent.addTableObject( new String(loc).trim(), i, 5);

                      parent.addTableObject( "0x"+Integer.toString(wacn,16) , i, 6);

                    }

                    if(nrecs<NRECS) {
                      for(int i=nrecs;i<NRECS;i++) {

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

                          FileNameExtensionFilter filter = new FileNameExtensionFilter( "TGP and CSV export files", "tgp", "csv");
                          chooser.setFileFilter(filter);
                          int returnVal = chooser.showDialog(parent,"Export Talk Group CSV and TGP Files");


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

                            //write the CSV file
                            file_path = file.getAbsolutePath();
                            if(file_path!=null && file_path.contains(".")) {
                              StringTokenizer st = new StringTokenizer(file_path,".");
                              file_path = st.nextToken();
                            }


                            file = new File(file_path+".csv");
                            fos = new FileOutputStream(file);

                            String header_line = "ENABLED,SYS_ID_HEX,PRIORITY,TGRP,ALPHATAG,DESCRIPTION,WACN_HEX\r\n";
                            fos.write( header_line.getBytes() );

                            bb = ByteBuffer.wrap(image_buffer);
                            bb.order(ByteOrder.LITTLE_ENDIAN);

                            for(int i=0;i<nrecs;i++) {

                              desc = new byte[32];
                              loc = new byte[32];

                              int enabled = bb.getInt();
                              int wacn = bb.getInt();
                              int sys_id = (wacn&0xfff);
                              wacn = wacn>>>12; 

                              int priority = bb.getInt();
                              int talkgroup = bb.getInt();

                              for(int j=0;j<32;j++) {
                                desc[j] = bb.get();
                              }
                              for(int j=0;j<32;j++) {
                                loc[j] = bb.get();
                              }

                              String alpha_str = new String(desc).trim();
                              String desc_str = new String(loc).trim();
                              alpha_str = alpha_str.replace(',',' ');
                              desc_str = desc_str.replace(',',' ');

                              String record_out = enabled+","+"0x"+Integer.toString(sys_id,16)+","+priority+","+talkgroup+","+
                                                  alpha_str+","+
                                                  desc_str+","+
                                                  "0x"+Integer.toString(wacn,16)+"\r\n";
                              fos.write( record_out.getBytes() );
                            }

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
