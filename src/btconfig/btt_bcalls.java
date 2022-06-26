package btconfig;

import java.net.*;
import java.net.URLConnection.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class btt_bcalls implements Runnable {

java.text.SimpleDateFormat time_format;
bcalls_config bttcfg;

StringBuilder sb;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
// user configured information
/////////////////////////////////////////////////////////////////////////////////////////////////////////////
  boolean IS_MP3=false; //when false, convert to m4a (AAC Audio) if supported
  String base_path = "";

  String ffmpeg_bin="";

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////
  // assigned key /system pair
  String apikey="";
  String node_id="";  //unfortunately this is assigned with the name systemId from broadcastify, 
                          //elsewhere they refer to as NODE id.
  String p25_sysid=""; //we need this to verify it matches the node_id
/////////////////////////////////////////////////////////////////////////////////////////////////////////////


  //////////////////////////////////////////////////////////////////////
  String charset = "UTF-8";
  String requestURL = "https://api.broadcastify.com/call-upload";
  String ts="";
  String tg="";
  String freq_str="";
  String freq="";
  String call_duration="";
  String src="";

  int rec_mod=0;
  boolean is_done=true;

  boolean did_init=false;

  /////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////
  public btt_bcalls(bcalls_config config) {
    time_format = new java.text.SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    bttcfg = config;
    sb = new StringBuilder(32000);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////
  public void update_config() {
    try {
      ffmpeg_bin = bttcfg.ffmpeg_path.getText();
      apikey = bttcfg.apikey_tf.getText();
      node_id = bttcfg.sid_tf.getText();
      p25_sysid = bttcfg.p25_sys_id.getText();

      base_path = bttcfg.parent.broadcastify_calls_dir;
    } catch(Exception e) {
    }

    try {
      if( is_m4a_supported(ffmpeg_bin) ) {
        System.out.println("system supports m4a audio conversion.");
        IS_MP3=false;
      }
      else {
        System.out.println("system doesn't support m4a audio conversion. using mp3 conversion");
        IS_MP3=true;
      }
    } catch(Exception e) {
    }

    did_init=true;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////
  public void run() {
    if(!did_init) {
      update_config();
    }
    while(true) {
      try {
        if( !scan_files() ) return; //we finished
        Thread.sleep(50);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

/*
  /////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) {
    btt_bcalls p = new btt_bcalls();

    if( p.is_m4a_supported() ) {
      System.out.println("system supports m4a audio conversion.");
      p.IS_MP3=false;
    }
    else {
      System.out.println("system doesn't support m4a audio conversion. using mp3 conversion");
      p.IS_MP3=true;
    }

    while(true) {
      try {
        p.scan_files();
        Thread.sleep(50);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }
*/

  /////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////
  public boolean _scan_files(File f) {

    try {
        if( f.getAbsolutePath().endsWith(".wav")) {
          //System.out.println( "\r\n\r\nConverting: "+f.getAbsolutePath());
          System.out.println( "\r\nConverting: "+f.getName());

          StringTokenizer st = new StringTokenizer(f.getName(), "_");
          if(st!=null && st.countTokens()==8) {
            //System.out.println(st.nextToken());
            st.nextToken(); //skip
            tg = st.nextToken(); //TG
            ts = st.nextToken(); //TS
            String sys = st.nextToken(); //p25 sys_id


            freq_str = st.nextToken(); //freq
            long freq_long = Long.valueOf(freq_str);
            //System.out.println("freq_str:"+freq_str);

            freq = String.format("%3.5f", ((double) freq_long)/1000000.0 );
            src = st.nextToken();
            call_duration = st.nextToken();

            if(sb.length()>8000) sb.setLength(0);

            java.util.Date d = new java.util.Date();
            String ctime = time_format.format(d);
            //System.out.println("\r\nCurrent Time: "+ctime);
            sb = sb.append("\r\nCurrent Time: "+ctime+"\r\n");
            sb = sb.append("TG:"+tg+"\r\n");
            sb = sb.append("TS:"+ts+"\r\n");
            sb = sb.append("NODE-ID:"+node_id+"\r\n");
            sb = sb.append("SYS-ID:"+p25_sysid+"\r\n");
            sb = sb.append("FREQ:"+freq+"\r\n");
            sb = sb.append("RID:"+src+"\r\n");
            sb = sb.append("DURATION:"+call_duration+"\r\n");

            try {

              if(sys.equals(p25_sysid)) { 
                convert_and_upload(f, IS_MP3);
              }
              else {
                sb = sb.append("WARNING!!: Wrong P25_SYS_ID for "+f.getName()+" Removing without send"+"\r\n");
                //wrong system. delete it.
              }

              f.delete();
            } catch(Exception e) {
              try {
                f.delete();
              } catch(Exception e2) {
              }
            }

            try {
              bttcfg.parent.btt_bcalls_console.setText( sb.toString() );
              bttcfg.parent.btt_bcalls_console.setCaretPosition(sb.length());
              bttcfg.parent.btt_bcalls_console.getCaret().setVisible(true);
              bttcfg.parent.btt_bcalls_console.getCaret().setBlinkRate(250);

            } catch(Exception e) {
            }

          }
          
          return true;
        }
    } catch(Exception e2) {
    }
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////
  public boolean scan_files() {
    try {
      File in_files = new File(base_path);
      File[] file_list = in_files.listFiles();
      for(int i=0;i<file_list.length;i++) {
        File f = file_list[i];
        _scan_files(f);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////
  //    enable-libfdk-aac
  /////////////////////////////////////////////////////////////////////////////////////////////
  public boolean is_mp3_supported(String path_to_exe) {
    try {
      byte[] buffer = new byte[128000];
      Process proc = Runtime.getRuntime().exec(path_to_exe);
      InputStream is = proc.getErrorStream();
      int off=0;
      int len=0;
      int avail=0;

      //while(is.available()<=0);
      //Thread.sleep(100);
      proc.waitFor();

      while( is.available() > 0) {
        avail = is.available();
        if(avail>0) {
          len = is.read(buffer,off,avail); 
          off += len;
        }
      }
      String ret = new String(buffer);
      ret = ret.trim();
      //System.out.println("ret:"+ret+":");
      
      if( ret.contains("enable-libmp3lame") ) return true;
    } catch(Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////
  //    enable-libfdk-aac
  /////////////////////////////////////////////////////////////////////////////////////////////
  public boolean is_m4a_supported(String path_to_exe) {
    try {
      byte[] buffer = new byte[128000];
      Process proc = Runtime.getRuntime().exec(path_to_exe);
      InputStream is = proc.getErrorStream();
      int off=0;
      int len=0;
      int avail=0;

      //while(is.available()<=0);
      //Thread.sleep(100);
      proc.waitFor();

      while( is.available() > 0) {
        avail = is.available();
        if(avail>0) {
          len = is.read(buffer,off,avail); 
          off += len;
        }
      }
      String ret = new String(buffer);
      ret = ret.trim();
      //System.out.println("ret:"+ret+":");
      
      if( ret.contains("enable-libfdk-aac") ) return true;
    } catch(Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////
  public void convert_and_upload(File f, boolean is_mp3) {
    File ufile=null;
    try {
      String ffmpeg_cmd = ""; 
      if(is_mp3) {
        String fname = "test_"+bttcfg.parent.sys_mac_id+".mp3";
        ufile = new File(fname);
        //MP3 audio
        ffmpeg_cmd = ffmpeg_bin+" -f wav -ac 1 -guess_layout_max 0 -i "+f.getAbsolutePath()+" "+"-b:a 32k -cutoff 18000 "+fname;
      }
      else {
        String fname = "test_"+bttcfg.parent.sys_mac_id+".m4a";
        ufile = new File(fname);
        //AAC audio
        ffmpeg_cmd = ffmpeg_bin+" -f wav -ac 1 -guess_layout_max 0 -i "+f.getAbsolutePath()+" "+"-c:a libfdk_aac -b:a 32k -cutoff 18000 "+fname;
      }
      //System.out.println(ffmpeg_cmd);
      System.out.println("Running FFMPEG");
      Process proc = Runtime.getRuntime().exec(ffmpeg_cmd);
      proc.waitFor();

      upload_file( ufile, is_mp3 ); 

      proc.destroy();

    } catch(Exception e) {
      e.printStackTrace();
    }
    finally {
      f.delete();
      if(ufile!=null) ufile.delete();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////
  public void upload_file(File f, boolean is_mp3) {

    try {
      Multipart multipart = new Multipart(requestURL, charset);

      multipart.addFormField("apiKey", apikey);
      multipart.addFormField("systemId", node_id);  //this is really the assigned node id
      multipart.addFormField("callDuration", call_duration);
      multipart.addFormField("ts", ts);
      multipart.addFormField("tg", tg);
      multipart.addFormField("src", src);
      multipart.addFormField("freq", freq);
      if(is_mp3) {
        multipart.addFormField("enc", "mp3");
      }
      else {
        multipart.addFormField("enc", "m4a");
      }

      List<String> response = multipart.finish();

      String errcode="";
      String url="";

      for (String line : response) {
        StringTokenizer st = new StringTokenizer(line," ");
        if(st!=null && st.countTokens()>=2) {
          errcode = st.nextToken();
          url = st.nextToken();

        }
      }

      //0 = no error
      if(errcode.trim().startsWith("0")) {
          sb = sb.append("Received One-time Upload URL\r\n");

          //System.out.println("err:"+errcode);
          //System.out.println("url:"+url);

          sb = sb.append("Sending file...\r\n");
          multipart.send_file(url, f, is_mp3); //url, file, is_mp3

          List<String> response2 = multipart.finish();
          for (String line : response2) {
            System.out.println(response2);
          }

          sb = sb.append("Call Completed.\r\n");
          is_done=true;

          did_init=true;
      }
      else {
        sb = sb.append("WARNING!!!!: SERVER ERROR CODE: "+errcode +" Description: "+url+"\r\n");
      }


    } catch(Exception e) {
      e.printStackTrace();
    }
  }

}
