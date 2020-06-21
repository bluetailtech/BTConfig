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

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
class firmware_update
{

String new_firmware_crc = "";
java.util.Timer utimer;
BTFrame parent;
SerialPort serial_port;

////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////
  public void send_firmware(BTFrame parent, BufferedInputStream bis, SerialPort serial_port)
  {
    this.serial_port = serial_port;

    byte[] appcrc = new byte[4]; 
    byte[] image_buffer = new byte[128 * 1024 * 6];

    for( int i=0; i< 128 * 1024 *6; i++) {
      image_buffer[i] = (byte) 0xff;
    }

    int firmware_len = 0;

		//File f = new File(firmware_path);
    //parent.setStatus("loading firmware image "+firmware_path);

    try {

			//FileInputStream fis = new FileInputStream(f);

      bis.read(appcrc, 0, 4);
      firmware_len = bis.read(image_buffer, 0, 128*1024*6);

      //int crc = crc32.crc32_range(image_buffer, 128 * 1024 * 6);
      int crc=0;

      ByteBuffer bbcrc = ByteBuffer.wrap(appcrc);
      bbcrc.order(ByteOrder.LITTLE_ENDIAN);
      crc = bbcrc.getInt();

      parent.setProgress(5);
      //parent.setStatus( String.format(" len = %d, crc: 0x%08x", firmware_len, crc) );
      parent.setStatus("Checking installed firmware version..."); 

      int state = -1; 
      int app_crc_valid=0;
      int is_bl = 0;

      while(true) {


          if(state==-1) {
            if(serial_port!=null && serial_port.isOpen()) {
              state=0;
            } 
          }


          //state app_crc
          if(state==0) {

            //stop all the high bw stuff on usb i/o
            String cmd= new String("en_voice_send 0\r\n");
            serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
            cmd= new String("logging -999\r\n");
            Thread.sleep(100);
            serial_port.writeBytes( cmd.getBytes(), cmd.length(), 0);
            Thread.sleep(100);
            /////////////


            String res = send_cmd("app_crc\r\n", 1100);
            //parent.setStatus("resp: "+res+":");

            StringTokenizer st = new StringTokenizer(res," \r\n");
            if(st.countTokens()<2) {
              parent.setStatus("\r\nSearching for device.");
              //System.exit(0);
              //return;
              Thread.sleep(100);
              parent.is_connected=0;
              parent.do_connect=1;
              return;
            }

            while(st!=null && st.hasMoreTokens()) {
              String str1 = st.nextToken();
              if(str1!=null && str1.trim().equals("app_crc") && st.hasMoreTokens()) {

                String crc_str = st.nextToken();
                crc_str = crc_str.substring(2,crc_str.length());


                int app_crc=0;
                try {
                  //must process as long because... signed ints only
                  app_crc = (int) Long.parseLong(crc_str, 16);
                } catch(Exception e) {
                  e.printStackTrace();
                  break;
                }

                if(app_crc == crc) {
                  parent.setStatus("\r\nfirmware is up-to-date");
                  state=1;
                  app_crc_valid=1;
                  parent.setProgress(100);
                  break;
                }
                else if(app_crc != crc) {
                  parent.setStatus("\r\nfirmware is not up-to-date");
                  parent.setProgress(0);
                  state=1;
                  app_crc_valid=0;
                  break;
                }
              }
            }
          } //state==0

          //state boot_or_app
          if(state==1) {
            String res = send_cmd("bl_or_app\r\n", 1000).trim();
            System.out.println("resp: "+res+":");

            if(res.contains("bl_or_app bl")) {
              parent.setStatus("device is in: bootloader state");
              is_bl=1;
              state=2;
            }
            else if(res.contains("bl_or_app app")) {
              parent.setStatus("device is in: application state");
              is_bl=0;
              if(app_crc_valid==1) state=2;
              if(app_crc_valid==0) state=3;
            }
            else {
              Thread.sleep(100);
            }

          } //state==1

          //state switch to bootloader mode
          if(state==3 && is_bl==0) {

              parent.setStatus("\r\nsetting boot cmd to bootloader state");

              byte[] out_buffer = new byte[48]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);
              //uint32_t magic;
              //uint32_t op;
              //uint32_t addr;
              //uint32_t len;
              //uint8_t  data[32]; 

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("2", 10) ); //write bootloader boot_cmd
              bb.putInt( (int) Long.parseLong("08020000", 16) );
              bb.putInt( (int) Long.parseLong("8", 10) );
              //boot cmd area
              bb.putInt( (int) Long.parseLong("1", 10) ); //verify app and boot 
              bb.putInt( (int) crc);  //app crc 

              serial_port.writeBytes( out_buffer, 48, 0);

              //TODO: need to check for ack
              try {
                Thread.sleep(100);
              } catch(Exception e) {
              }

              parent.setStatus("\r\nresetting device");
              send_cmd("system_reset\r\n", 100);

              try {
                Thread.sleep(3000);
              } catch(Exception e) {
              }
              //serial_port.closePort();
            //
              parent.is_connected=0;
              parent.do_connect=1;

              state=-1;
          }


          if(state==5) {
              parent.setStatus("\r\nreading boot_cmd area");

              byte[] out_buffer = new byte[48]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);
              //uint32_t magic;
              //uint32_t op;
              //uint32_t addr;
              //uint32_t len;
              //uint8_t  data[32]; 

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("1", 10) ); //read bootloader boot_cmd
              bb.putInt( (int) Long.parseLong("08020000", 16) );
              bb.putInt( (int) Long.parseLong("8", 10) );
              for(int i=0;i<32;i++) {
                bb.put((byte) 0x00);
              }
              serial_port.writeBytes( out_buffer, 48, 0);

              byte[] input_buffer = new byte[48];

              int rlen=0;
              int ack_timeout=0;
                while(rlen!=48) {

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      Thread.sleep(1);
                      if(count++>50) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }

                  rlen=serial_port.readBytes( input_buffer, 48);

                  if(rlen==48) break;

                }

                for(int i=0;i<16+8;i++) {
                  if(i==0) System.out.print(String.format("\r\n%02x,",input_buffer[i]));
                   else System.out.print(String.format("%02x,",input_buffer[i]));
                }
            parent.setStatus("");

            //System.exit(0);
            return;
          }

          //state switch to application mode
          if(state==2 && is_bl==1) {

            if(app_crc_valid==1) {
              parent.setStatus("\r\nsetting boot cmd to applicaton state");

              byte[] out_buffer = new byte[48]; //size of bl_op
              ByteBuffer bb = ByteBuffer.wrap(out_buffer);
              bb.order(ByteOrder.LITTLE_ENDIAN);
              //uint32_t magic;
              //uint32_t op;
              //uint32_t addr;
              //uint32_t len;
              //uint8_t  data[32]; 

              bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
              bb.putInt( (int) Long.parseLong("2", 10) ); //write bootloader boot_cmd
              bb.putInt( (int) Long.parseLong("08020000", 16) );
              bb.putInt( (int) Long.parseLong("8", 10) );
              //boot cmd area
              bb.putInt( (int) Long.parseLong("2", 10) ); //verify app and boot 
              bb.putInt( (int) crc);  //app crc 

              serial_port.writeBytes( out_buffer, 48, 0);

              //TODO: need to check for ack
              try {
                Thread.sleep(100);
              } catch(Exception e) {
              }

              parent.setProgress(90);
              parent.setStatus("\r\nresetting device");
              send_cmd("system_reset\r\n", 10);

              try {
                Thread.sleep(5000);
              } catch(Exception e) {
              }
              //serial_port.closePort();
              parent.is_connected=0;
              parent.do_connect=1;

              state=-1;
            }

            if(app_crc_valid==0) {
              parent.setStatus("\r\nerasing app area, sending new firmware image...");

              int offset = 0;
              parent.setProgress(20);

              while(offset<firmware_len) {
              //while(offset<(1024*128*6)) {

                byte[] out_buffer = new byte[16+32]; //size of bl_op
                ByteBuffer bb = ByteBuffer.wrap(out_buffer);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                //uint32_t magic;
                //uint32_t op;
                //uint32_t addr;
                //uint32_t len;
                //uint8_t  data[32]; 

                bb.putInt( (int) Long.parseLong("d35467A6", 16) );  //magic
                //bb.putInt( (int) Long.parseLong("3", 10) ); //write flash cmd   (3 could be used to flash unencrypted binary
                                                              //starting at 0x0804000
                bb.putInt( (int) Long.parseLong("6", 10) ); //write encrypted flash cmd (same as "3", but decryption first) 
                bb.putInt( (int) new Long((long) 0x08040000 + offset).longValue() );
                bb.putInt( (int) Long.parseLong("32", 10) );  //data len

                for(int i=0;i<32;i++) {
                  //if(i+offset<firmware_len) bb.put( image_buffer[i+offset] ); 
                   // else bb.put((byte) 255);
                  bb.put( image_buffer[i+offset] ); 
                }



                byte[] input_buffer = new byte[48];
                int rlen = 0;
                int ack_timeout=0;

                while(rlen!=48) {
                  serial_port.writeBytes( out_buffer, 16+32, 0);

                  try {
                    int count=0;
                    while(serial_port.bytesAvailable()<48) {
                      Thread.sleep(1);
                      if(count++>5000) break;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }

                  rlen=serial_port.readBytes( input_buffer, 48);

                  if(rlen==48) {
                    ack_timeout=0;
                    break;
                  }


                }

                //for(int i=0;i<48;i++) {
                //  if(i==0) System.out.print(String.format("\r\n%02x,",input_buffer[i]));
                 //   else System.out.print(String.format("%02x,",input_buffer[i]));
                //}
                //if(input_buffer[4]==4) parent.setStatus("ack");

                if(input_buffer[4]==5) {
                  state=-1;
                  parent.setStatus("NACK");
                  break;
                }

                offset+=32;
                if(offset%8192==0) System.out.print("\rsent "+offset+"        ");

                int pcomplete = (int)  (((float) offset/(float) firmware_len)*80.0);
                parent.setProgress((int) pcomplete);
              }
                System.out.print("\rsent "+offset+"        ");



              //TODO: need to check for ack
              try {
                Thread.sleep(100);
              } catch(Exception e) {
              }

              parent.setStatus("\r\nresetting device");

              Boolean isWindows = System.getProperty("os.name").startsWith("Windows");
              if(isWindows || parent.is_mac_osx==1) {
                send_cmd("system_reset\r\n", 10);
              }
              else {
                //send_cmd("system_reset\r\n", 1000);
                send_cmd("system_reset\r\n", 10);
              }

              try {
                Thread.sleep(3000);
              } catch(Exception e) {
              }
              //serial_port.closePort();
              parent.is_connected=0;
              parent.do_connect=1;


              state=-1;
            }

          }

          //are we in application mode with good crc?
          if(state==2 && is_bl==0) {
            //serial_port.closePort();
            parent.setStatus("all done.  Firmware is up-to-date");
            send_cmd("save\r\n", 1000); //flush new changes from global_post_read() to flash memory
            Thread.sleep(1000);

            //parent.firmware_checked=1;
            parent.do_read_config=1;
            //parent.do_read_talkgroups=1;
            //parent.is_connected=1;

            parent.do_update_firmware=0;

            //System.exit(0);
            return;
          }

        } //while(true) 
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////
  public String send_cmd(String cmd, int timeout)
  {

    byte[] data_cmd = cmd.getBytes();
    int len = serial_port.writeBytes( data_cmd, data_cmd.length, 0);
    byte[] data_buffer = new byte[2048];
    int i=0;

      try {
        Thread.sleep(timeout);
      } catch(Exception e) {
        e.printStackTrace();
      }

      len = serial_port.readBytes( data_buffer, 512, timeout);

      if(len>=3) { 
        return new String(data_buffer);
      }

    return "";
  }

  //////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////
  public int waitPrompt()
  {
    byte[] data_cmd = new String("\r\n").getBytes();
    int len = serial_port.writeBytes( data_cmd, data_cmd.length, 0);
    byte[] data_buffer = new byte[2048];
    int i=0;

      try {
        Thread.sleep(5);
      } catch(Exception e) {
        e.printStackTrace();
      }

    while(i++<10) {
      len = serial_port.readBytes( data_buffer, 255, 5);

      //look for prompt 
      if(len>=3 && new String(data_buffer).contains("~$ ") ) {
        //parent.setStatus("\r\nfound prompt");
        return 1;
      }
      else if(len>0) {
        parent.setStatus(":"+new String(data_buffer)+":");
      }

      try {
        Thread.sleep(5);
      } catch(Exception e) {
        e.printStackTrace();
      }

      serial_port.writeBytes( data_cmd, data_cmd.length, 0);
    }
    return 0;
  }

}
