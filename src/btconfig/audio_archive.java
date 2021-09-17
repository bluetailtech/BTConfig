package btconfig;

import java.nio.*;
import java.util.*;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.*;
import javax.swing.*;

import net.sourceforge.lame.mp3.*;
import net.sourceforge.lame.lowlevel.*;

import pcmsampledsp.*;



public class audio_archive {
LameEncoder encoder=null;
byte[] mp3_buffer;
FileOutputStream fos_mp3;
File mp3_file=null;

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  class updateTask extends java.util.TimerTask
  {

      public void run()
      {
        try {

        } catch(Exception e) {
        }
      }
  }

  java.util.Timer utimer;
  BTFrame parent;
  boolean debug=true;



  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void init() {

    try {
      utimer = new java.util.Timer();
      utimer.schedule( new updateTask(), 100, 1);
    } catch(Exception e) {
     e.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public audio_archive(BTFrame p) {
    parent = p;
    init();
  }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
private void SLEEP(long val) {
  try {
    parent.SLEEP(val);
  } catch(Exception e) {
    e.printStackTrace();
  }
}
}

    /*
    mp3_file = new File(home_dir+"p25rx"+fs+sys_mac_id+fs+"p25rx_recording_"+current_date+".mp3");
    fos_mp3 = new FileOutputStream( mp3_file, true ); 
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
      e.printStackTrace();
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
    */

      /*
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
                      e.printStackTrace();
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
                          e.printStackTrace();
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
                          e.printStackTrace();
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
    */


      /*
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
        e.printStackTrace();
      }

      return b;
    }
    */
