package btconfig;

import java.nio.*;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.*;
import javax.swing.*;

import net.sourceforge.lame.mp3.*;
import net.sourceforge.lame.lowlevel.*;

import pcmsampledsp.*;
import java.util.concurrent.TimeUnit;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class audio_archive {

LameEncoder encoder=null;
byte[] mp3_buffer;
FileOutputStream fos_mp3;
FileOutputStream fos_wav;
FileOutputStream prev_fos_mp3;
FileOutputStream prev_fos_wav;

FileOutputStream rdio_wav;
FileOutputStream prev_rdio_wav;

FileOutputStream rid_fos_mp3;
FileOutputStream rid_fos_wav;
FileOutputStream rid_prev_fos_mp3;
FileOutputStream rid_prev_fos_wav;

File mp3_file=null;
java.text.SimpleDateFormat mp3_time_format;
java.text.SimpleDateFormat formatter_date;
java.text.SimpleDateFormat rdio_date;
java.text.SimpleDateFormat rdio_time;
java.text.SimpleDateFormat rdio_time_full;
String mp3_time ="";
private int do_audio_encode=0;
private byte[] audio_buffer=null;
private int is_high_q=0;
BTFrame parent;
boolean debug=false;
String tg="";
String home_dir;
String wacn="";
String sysid="";
String sysid_dec="";

java.util.Timer utimer;

int p25_follow=0;
String hold_str="";

boolean is_silence=false;

double freq_hz=0.0;
String rdio_ndate = ""; 
String rdio_ntime = ""; 

String rdio_path="";

private long start_time;
private long end_time;

private String temp_name="";
private String rdio_final_name="";

private int prev_uid;
private int src_uid;

  /*
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  class updateTask extends java.util.TimerTask
  {
    long NS_PER_US = 1000; 
    long DELAY_TARGET_US = NS_PER_US*100; 

      public void run()
      {
    while(true) {

         long t0 = System.nanoTime(); 
         while (System.nanoTime() < t0+DELAY_TARGET_US) {
           try {
             Thread.sleep(0, 100);
           } catch(Exception e) {
           }
         }; 

    }
      }
  }
    */

  private void _write_audio() {
        try {

          String fs =  System.getProperty("file.separator");

          if( parent.enable_mp3.isSelected() && audio_buffer!=null && home_dir!=null) {
            do_audio_encode=0;


            if( parent.do_mp3.isSelected() ) {
              //System.out.println("encode mp3");

              if( audio_buffer!=null ) {
                byte[] buffer = encode_mp3(audio_buffer);

                if(buffer!=null && buffer.length>0) {
                  try {
                    if(tg==null || tg.length()==0) return; 

                    String ndate = formatter_date.format(new java.util.Date() );

                    if( parent.separate_rid.isSelected() && parent.src_uid!=0) {
                      try {
                        Path path = Paths.get(new File(home_dir+"/TG-"+tg+"_"+ndate+"-"+wacn+"-"+sysid).getAbsolutePath() );
                        Files.createDirectories(path);
                        String abspath = path.toString();
                        String rid_str = String.format("%d", parent.src_uid);
                        rid_fos_mp3 = new FileOutputStream( abspath+"/"+"RID-"+rid_str+".mp3", true );
                      } catch(Exception e) {
                      }
                    }

                    if(parent.mp3_separate_files.isSelected()) {
                      fos_mp3 = new FileOutputStream( home_dir+"/TG-"+tg+"_"+hold_str+ndate+"-"+wacn+"-"+sysid+".mp3", true );
                    }
                    else {
                      fos_mp3 = new FileOutputStream( home_dir+"/p25rx_record"+"_"+hold_str+ndate+"-"+wacn+"-"+sysid+".mp3", true );
                    }

                    if(parent.separate_rid.isSelected() && parent.src_uid!=0) {
                      rid_fos_mp3.write(buffer,0,buffer.length);  //write Int num records
                      if(rid_prev_fos_mp3!=rid_fos_mp3) {
                        if(rid_prev_fos_mp3!=null) rid_prev_fos_mp3.close();
                        rid_prev_fos_mp3 = rid_fos_mp3;
                      }
                    }


                    fos_mp3.write(buffer,0,buffer.length);  //write Int num records
                    if(prev_fos_mp3!=fos_mp3) {
                      if(prev_fos_mp3!=null) prev_fos_mp3.close();
                      prev_fos_mp3 = fos_mp3;
                    }
                  } catch(Exception e) {
                    e.printStackTrace();
                  }
                }
              }
            }
            else if( parent.do_wav.isSelected() ) {
              //System.out.println("encode wav");
              if( audio_buffer!=null ) {

                try {
                  if(tg==null || tg.length()==0) return; 

                  String ndate = formatter_date.format(new java.util.Date() );

                  if( parent.separate_rid.isSelected() && parent.src_uid!=0) {
                    try {
                      Path path = Paths.get(new File(home_dir+"/TG-"+tg+"_"+ndate+"-"+wacn+"-"+sysid).getAbsolutePath() );
                      Files.createDirectories(path);
                      String abspath = path.toString();
                      String rid_str = String.format("%d", parent.src_uid);

                      String wfname = abspath+"/"+"RID-"+rid_str+".wav";
                      check_wav_header(wfname);
                      rid_fos_wav = new FileOutputStream( wfname, true );
                    } catch(Exception e) {
                    }
                  }

                  if(parent.mp3_separate_files.isSelected()) {
                    String wfname = home_dir+"/TG-"+tg+"_"+hold_str+ndate+"-"+wacn+"-"+sysid+".wav";
                    check_wav_header(wfname);
                    fos_wav = new FileOutputStream( wfname, true );
                  }
                  else {
                    String wfname = home_dir+"/p25rx_record"+"_"+hold_str+ndate+"-"+wacn+"-"+sysid+".wav";
                    check_wav_header(wfname);
                    fos_wav = new FileOutputStream( wfname, true );
                  }

                  if( parent.separate_rid.isSelected() && parent.src_uid!=0) {
                    rid_fos_wav.write(audio_buffer,0,audio_buffer.length);  //write Int num records
                    if(rid_prev_fos_wav!=rid_fos_wav) {
                      if(rid_prev_fos_wav!=null) rid_prev_fos_wav.close();
                      rid_prev_fos_wav = rid_fos_wav;
                    }
                  }

                  fos_wav.write(audio_buffer,0,audio_buffer.length);  //write Int num records
                  if(prev_fos_wav!=fos_wav) {
                    if(prev_fos_wav!=null) prev_fos_wav.close();
                    prev_fos_wav = fos_wav;
                  }
                } catch(Exception e) {
                  e.printStackTrace();
                }
              }
            }


            //RDIO support
            //rdio_mask.setText("TG_#TG_#DATE_#TIME_#SYS_#MHZ_");
          }

          if( !is_silence && parent.en_rdio.isSelected() && audio_buffer!=null && home_dir!=null) {
            if( audio_buffer!=null ) {

              try {
                if(tg==null || tg.length()==0) return; 


                String freq_str = String.format("%d", (int) freq_hz);


                Path path=null;
                try {
                  path = Paths.get(new File(home_dir+fs+"rdio_dirwatch").getAbsolutePath());
                  Files.createDirectories(path);
                } catch(Exception e) {
                  e.printStackTrace();
                }

                try {
                  java.util.Date datetime = new java.util.Date();
                  rdio_ndate = rdio_date.format(datetime);
                  rdio_ntime = rdio_time_full.format(datetime);

                  //String abspath = path.toString()+fs+"TG_"+tg+"_"+rdio_ndate+"_"+rdio_ntime+"_"+sysid_dec+"_"+freq_str+"_"+wacn+".wav";
                  String abspath = path.toString()+fs+"TG_"+tg+"_"+rdio_ndate+"_"+rdio_ntime+"_"+sysid_dec+"_"+freq_str;

                  if( rdio_wav==null ) { 
                    //System.out.println("creat new file: "+abspath);
                    rdio_final_name = abspath;
                    temp_name = path.toString()+fs+"temp.out"; 
                    rdio_path = temp_name; 

                    check_wav_header(rdio_path);
                    rdio_wav = new FileOutputStream( rdio_path, true );
                    prev_rdio_wav=rdio_wav;
                  }
                } catch(Exception e) {
                  e.printStackTrace();
                }

                try {
                  //System.out.println("write to file:");
                  if(rdio_wav!=null) {
                    
                    start_time = System.nanoTime(); 
                    end_time = start_time;

                    rdio_wav.write(audio_buffer,0,audio_buffer.length);  //write Int num records
                  }
                } catch(Exception e) {
                  e.printStackTrace();
                }
              } catch(Exception e) {
                e.printStackTrace();
              }
            }
          }

        } catch(Exception e) {
          e.printStackTrace();
        }
  }
  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void set_follow(int tg) {
    this.p25_follow = tg;
    if(p25_follow>0) hold_str="HOLD_EVENT_";
      else hold_str="";
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void init() {

    try {

      if(parent.audio_hiq.isSelected()) is_high_q=1;
        else is_high_q=0;

      mp3_time_format = new java.text.SimpleDateFormat( "HH:mm:ss" );
      formatter_date = new java.text.SimpleDateFormat( "yyyy-MM-dd" );

      rdio_date = new java.text.SimpleDateFormat( "yyyyMMdd" );
      rdio_time = new java.text.SimpleDateFormat( "HHmm" );
      rdio_time_full = new java.text.SimpleDateFormat( "HHmmss" );

      //utimer = new java.util.Timer();
      //utimer.schedule( new updateTask(), 0, 1);

    } catch(Exception e) {
     e.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void addAudio(byte[] pcm, String talkgroup, String home_dir, int wacn, int sysid, int mode_b, int p25_demod, double freq) {

    is_silence=false;

    int current_mod_type=0;

    freq_hz = freq;


    //map to gui combobox
      if( mode_b ==1 || mode_b == 129) {
        if(p25_demod==0) current_mod_type = 0;
        if(p25_demod==1) current_mod_type = 1;
      }
      if(mode_b==2) current_mod_type=3;
      if(mode_b==3) current_mod_type=4;
      if(mode_b==4) current_mod_type=6;
      if(mode_b==5) current_mod_type=2;
      if(mode_b==6) current_mod_type=5;
      if(mode_b==7) current_mod_type=7;
      if(mode_b==8) current_mod_type=8;

    if( current_mod_type==6 ) {
      //talkgroup="FMNB_"+String.format("%3.6f", freq/1e6);
      //talkgroup="FMNB_"+String.format("%3.6f", freq/1e6);
      //talkgroup = talkgroup.replace('.','_');
      talkgroup="510";
      wacn=0x12345;
      sysid=592;
    }
    if( current_mod_type==7 ) {
      //talkgroup="AM_"+String.format("%3.6f", freq/1e6);
      //talkgroup = talkgroup.replace('.','_');
      talkgroup="511";
      wacn=0x12345;
      sysid=592;
    }
    if( current_mod_type==8 ) {
      //talkgroup="AM_AAGC_"+String.format("%3.6f", freq/1e6);
      //talkgroup = talkgroup.replace('.','_');
      talkgroup="512";
      wacn=0x12345;
      sysid=592;
    }

    do_audio_encode=0;

    int err=0;
    try {
      //make sure it is an integer
      int tg = Integer.valueOf(talkgroup);
    } catch(Exception e) {
      return;
    }


    if(do_audio_encode!=0) return; //shouldn't happen
    if(home_dir==null) return;
    if(talkgroup==null || talkgroup.trim().equals("0") ) return;
    if(wacn==0) return;
    if(sysid==0) return;
    if(freq==0.0) return;

    this.home_dir = home_dir;

    if(audio_buffer==null || audio_buffer.length!=pcm.length) audio_buffer = new byte[pcm.length];
    for(int i=0;i<pcm.length;i++) {
      audio_buffer[i]=pcm[i];
    }

    tg = talkgroup;

    this.wacn = String.format("%05X", wacn);
    this.sysid = String.format("%03X",sysid);

    this.sysid_dec = String.format("%d",sysid);


    _write_audio();

  }
  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void tick(int uid) {

    if(rdio_wav==null) return;

    end_time = System.nanoTime(); 

    if(uid!=0) this.src_uid = uid;

    long diff_time_ms = (end_time - start_time)/1000000; //convert to ms

    if(prev_rdio_wav!=null && diff_time_ms>2000) {
      try {
        System.out.println(String.format("diff_time: %d ms, close file:", diff_time_ms));
        close_all_rdio();
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void close_all_rdio() {
      try {

        prev_rdio_wav.close();
        File tmp_file = new File(temp_name); 

        rdio_final_name = rdio_final_name +"_"+src_uid+".wav";
        tmp_file.renameTo( new File(rdio_final_name) );

        prev_rdio_wav=null;
      } catch(Exception e) {
        prev_rdio_wav=null;
        e.printStackTrace();
      }
      try {
        if(rdio_wav!=null) rdio_wav.close();
        rdio_wav=null;
        end_time = start_time = 0;
      } catch(Exception e) {
        rdio_wav=null;
        e.printStackTrace();
      }

    this.src_uid=0;

  }
  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void addSilence(int silent_time, String talkgroup, String home_dir, int wacn, int sysid) {

    is_silence=true;

    if(do_audio_encode!=0) return; //shouldn't happen
    if(home_dir==null) return;
    if( talkgroup==null ) return;
    if(wacn==0) return;
    if(sysid==0) return;

    this.home_dir = home_dir;

    int len = 320 * (silent_time/20);
    if(len<=0) return;

    if(audio_buffer==null|| audio_buffer.length!=len) audio_buffer = new byte[len];
    for(int i=0;i<len;i++) {
      audio_buffer[i]=0;
    }
    do_audio_encode=1;

    tg = talkgroup;

    this.wacn = String.format("%05X", wacn);
    this.sysid = String.format("%03X",sysid);

    _write_audio();

  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public audio_archive(BTFrame p) {
    parent = p;
    init();
  }

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    public byte[] encode_mp3(byte[] pcm) {

      int len=320;
      byte[] b=null;
      byte[] b2=null;


      try {

        int high_q=0;
        if(parent.audio_hiq.isSelected()) high_q=1;
            else high_q=0;

        if(encoder==null || high_q!=is_high_q) {
          AudioFormat inputFormat = new AudioFormat( 8000.0f, 16, 1, true, false);  //booleans are signed, big-endian
          if( parent.audio_hiq.isSelected()) {
            encoder = new LameEncoder(inputFormat, 256, MPEGMode.MONO, Lame.QUALITY_HIGHEST, false); //true=VBR, ignores bitrate
            System.out.println("mp3 high quality");
          }
          else {
            encoder = new LameEncoder(inputFormat, 32, MPEGMode.MONO, Lame.QUALITY_LOWEST, true); //true=VBR, ignores bitrate
            System.out.println("mp3 low quality");
          }
          mp3_buffer = new byte[encoder.getPCMBufferSize()];
        }

        is_high_q = high_q;

        int nframes = pcm.length/320;
        //System.out.println("nframes "+nframes);

        len *= nframes;

        b = new byte[len*4];
        //System.out.println("blen: "+b.length);

        int idx=0;

        for(int j=0;j<nframes;j++) {

          int n = encoder.encodeBuffer(pcm, 0, 320, mp3_buffer);
          if(n==0) continue;

          for(int i=0;i<n;i++) {
            b[idx++] = mp3_buffer[i];
          }
        }

        b2 = new byte[idx];
        //System.out.println("b2len: "+b2.length);

        for(int i=0;i<idx;i++) {
          b2[i] = b[i]; 
        }

        //System.out.println("mp3 encoded "+b2.length+" bytes");
      } catch(Exception e) {
        e.printStackTrace();
      }

      return b2;
    }


    //////////////////////////////////////////////////////////////////////
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
    //////////////////////////////////////////////////////////////////////
    public void check_wav_header(String fname) {

      File file;
      FileOutputStream out;
      try {
        file = new File(fname);
        if( file.exists() && file.length()>=44 ) return;

        out = new FileOutputStream(file,false);

        byte[] header = new byte[44];

          int totalDataLen = 999999999*3; //little more than 24 hours
          int totalAudioLen = 999999999*3;  //little more than 24 hours
          int channels=1;
          int longSampleRate=8000;
          int byteRate=longSampleRate*4;
          byte RECORDER_BPP=16;

          header[0] = 'R';  // RIFF/WAVE header
          header[1] = 'I';
          header[2] = 'F';
          header[3] = 'F';
          header[4] = (byte) (totalDataLen & 0xff);
          header[5] = (byte) ((totalDataLen >> 8) & 0xff);
          header[6] = (byte) ((totalDataLen >> 16) & 0xff);
          header[7] = (byte) ((totalDataLen >> 24) & 0xff);
          header[8] = 'W';
          header[9] = 'A';
          header[10] = 'V';
          header[11] = 'E';
          header[12] = 'f';  // 'fmt ' chunk
          header[13] = 'm';
          header[14] = 't';
          header[15] = ' ';
          header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
          header[17] = 0;
          header[18] = 0;
          header[19] = 0;
          header[20] = 1;  // format = 1
          header[21] = 0;
          header[22] = (byte) channels;
          header[23] = 0;
          header[24] = (byte) (longSampleRate & 0xff);
          header[25] = (byte) ((longSampleRate >> 8) & 0xff);
          header[26] = (byte) ((longSampleRate >> 16) & 0xff);
          header[27] = (byte) ((longSampleRate >> 24) & 0xff);
          header[28] = (byte) (byteRate & 0xff);
          header[29] = (byte) ((byteRate >> 8) & 0xff);
          header[30] = (byte) ((byteRate >> 16) & 0xff);
          header[31] = (byte) ((byteRate >> 24) & 0xff);
          header[32] = (byte) (2 * 16 / 8);  // block align
          header[33] = 0;
          header[34] = RECORDER_BPP;  // bits per sample
          header[35] = 0;
          header[36] = 'd';
          header[37] = 'a';
          header[38] = 't';
          header[39] = 'a';
          header[40] = (byte) (totalAudioLen & 0xff);
          header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
          header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
          header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

          out.write(header, 0, 44);
      } catch(Exception e) {
        e.printStackTrace();
      }



    }


}

