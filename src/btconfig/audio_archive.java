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


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class audio_archive {

LameEncoder encoder=null;
byte[] mp3_buffer;
FileOutputStream fos_mp3;
File mp3_file=null;
java.text.SimpleDateFormat mp3_time_format = new java.text.SimpleDateFormat( "HH:mm:ss" );
String mp3_time ="";
private int do_audio_encode=0;
private byte[] audio_buffer=null;
private int is_high_q=0;

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  class updateTask extends java.util.TimerTask
  {

      public void run()
      {
        try {

          if( do_audio_encode!=0 && audio_buffer!=null) {
            do_audio_encode=0;

            if( parent.do_mp3.isSelected() ) {
              //System.out.println("encode mp3");

              if( audio_buffer!=null ) {
                byte[] buffer = encode_mp3(audio_buffer);

                if(buffer!=null && buffer.length>0) {
                  try {
                    fos_mp3 = new FileOutputStream( "/tmp/test.mp3", true );
                    fos_mp3.write(buffer,0,buffer.length);  //write Int num records
                    fos_mp3.flush();
                    fos_mp3.close();
                  } catch(Exception e) {
                    e.printStackTrace();
                  }
                }
              }
            }
            else if( parent.do_wav.isSelected() ) {
              //System.out.println("encode wav");
            }
          }
        } catch(Exception e) {
          e.printStackTrace();
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

      if(parent.audio_hiq.isSelected()) is_high_q=1;
        else is_high_q=0;
    } catch(Exception e) {
     e.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void addAudio(byte[] pcm) {
    if(do_audio_encode!=0) return; //shouldn't happen

    if(audio_buffer==null || audio_buffer.length!=pcm.length) audio_buffer = new byte[pcm.length];
    for(int i=0;i<pcm.length;i++) {
      audio_buffer[i]=pcm[i];
    }
    do_audio_encode=1;
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


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    public byte[] encode_mp3(byte[] pcm) {

      int len=0;
      byte[] b=null;


      try {

        int high_q=0;
        if(parent.audio_hiq.isSelected()) high_q=1;
            else high_q=0;

        if(encoder==null || high_q!=is_high_q) {
          AudioFormat inputFormat = new AudioFormat( 8000.0f, 16, 1, true, false);  //booleans are signed, big-endian
          if( parent.audio_hiq.isSelected()) {
            encoder = new LameEncoder(inputFormat, 256, MPEGMode.MONO, Lame.QUALITY_HIGHEST, true);
            System.out.println("mp3 high quality");
          }
          else {
            encoder = new LameEncoder(inputFormat, 32, MPEGMode.MONO, Lame.QUALITY_LOWEST, true);
            System.out.println("mp3 low quality");
          }
          mp3_buffer = new byte[encoder.getPCMBufferSize()];
        }

        is_high_q = high_q;

        len = encoder.encodeBuffer(pcm, 0, 320, mp3_buffer);

        if(len==0) return null;

        b = new byte[len];

        if(len>0) {
          for(int i=0;i<len;i++) {
            b[i] = mp3_buffer[i];
          }
        }
      } catch(Exception e) {
        e.printStackTrace();
      }

      return b;
    }


    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    public byte[] encode_wav(byte[] pcm) {
      int len=0;
      byte[] b=null;

            /*
      typedef struct wav_header {
          // RIFF Header
          char riff_header[4]; // Contains "RIFF"
          int wav_size; // Size of the wav portion of the file, which follows the first 8 bytes. File size - 8
          char wave_header[4]; // Contains "WAVE"
          
          // Format Header
          char fmt_header[4]; // Contains "fmt " (includes trailing space)
          int fmt_chunk_size; // Should be 16 for PCM
          short audio_format; // Should be 1 for PCM. 3 for IEEE Float
          short num_channels;
          int sample_rate;
          int byte_rate; // Number of bytes per second. sample_rate * num_channels * Bytes Per Sample
          short sample_alignment; // num_channels * Bytes Per Sample
          short bit_depth; // Number of bits per sample
          
          // Data
          char data_header[4]; // Contains "data"
          int data_bytes; // Number of bytes in data. Number of samples * num_channels * sample byte size
          // uint8_t bytes[]; // Remainder of wave file is bytes
      } wav_header;
          */

       return b;
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


