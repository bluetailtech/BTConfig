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

import java.nio.*;
import java.util.*;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.*;
import javax.swing.*;

import pcmsampledsp.*;



public class audio {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  class updateTask extends java.util.TimerTask
  {
    long NS_PER_MS = 1000000; 
    long DELAY_TARGET_MS = NS_PER_MS; 

      public void run()
      {
    while(true) {

         long t0 = System.nanoTime(); 
         while (System.nanoTime() < t0+DELAY_TARGET_MS) {
           try {
             Thread.sleep(0, 1000);
           } catch(Exception e) {
           }
         }

        try {

          int blen = sourceDataLine.getBufferSize()-sourceDataLine.available();
          if(sourceDataLine.isRunning() && blen > 0) {
            int kb = (int) ( (float) blen / (float) 1024 );

            /*
            System.out.print("\r\n");
            for(int i=0;i<kb;i++) {
              System.out.print("#");
            }
            System.out.print("\r\n");
            */
            //parent.audio_prog.setValue(kb);
          }


          /*
          if(voice_count > 0) {
            vc_timer++;
            if(vc_timer>150) {
              do_drain=1;
              vc_timer=0;
            }
          } 
          */

          //////////////////////
          //////////////////////
          //if(do_drain==1) {
           // do_drain=0;

            /*
            if(sourceDataLine.isRunning() && sourceDataLine.getBufferSize()!=sourceDataLine.available() ) {
              if(debug) System.out.println("stop");
              sourceDataLine.drain();
              if(do_start==0) {
                sourceDataLine.stop();
                parent.audio_prog.setValue(0);
              }
              else {
                if(debug) System.out.println("abort stop.");
                stop_timer=500;
                do_start=0;
              }
              voice_count=0;
            }
            else {
              */
                if(did_audio!=0) {
                  did_audio--;
                }
                else {
                  int bsize = sourceDataLine.getBufferSize();
                  int bavail = sourceDataLine.available();
                  int kb = (int) ( (float) blen / (float) 1024 );

                  if(sourceDataLine.isRunning() && kb < 50) {
                    byte[] b = new byte[3200*4];
                    sourceDataLine.write(b, 0, 3200*4);
                  }
                }
            //}
          //}

              /*
          if(stop_timer>0) {
            stop_timer--;
            if(stop_timer==0) {
              if(debug) System.out.println("stop");
              sourceDataLine.drain();
              sourceDataLine.stop();
              parent.audio_prog.setValue(0);
            }
          }
              */
          if(start_timer>0) {
            start_timer--;
            if(start_timer==0) {
              if(debug) System.out.println("short start");
              if(!sourceDataLine.isRunning()) {
                sourceDataLine.start();
              }
            }
          }


        } catch(Exception e) {
        }
      }
    }
  }

  int vc_timer;

  java.util.Timer utimer;
  int do_drain=0;
  int do_start=0;
  int stop_timer=0;
  int start_timer=0;
  int did_audio=0;

  Boolean initialized=false;
  AudioFormat af=null;
  Mixer mixer=null;
  SourceDataLine sourceDataLine;
  Resampler resamp;
  float agc_gain = 1.0f;
  int start_playing=0;

FloatControl mixer_gainControl;
FloatControl mixer_volume;
FloatControl src_gainControl;
FloatControl src_volume;


BTFrame parent;

  Vector mixer_v;
  int dev_changed=0;

  int prev_selection = -1;
  int sel_i=0;
  Mixer.Info[] mixerInfo;
  LineListener listener=null;
  DataLine.Info dataLineInfo;
  Mixer.Info mixer_info;

  Line.Info[] lineInfo;

  int audio_srate=48000;

  int voice_count=0;

  boolean debug=true;



  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void update_audio_rate() {
    try {

      audio_srate = 48000;
      dev_changed();
      parent.setStatus("Audio Sample Rate updated");
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void dev_changed() {
    dev_changed=1;
    prev_selection=-1;
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void init() {

    try {
      utimer = new java.util.Timer();
      utimer.schedule( new updateTask(), 100, 1);

        closeAll();

          try {
            int count = 0;

            sel_i=0;
            String dev_str = parent.prefs.get("audio_output_device", "default");
            if(dev_str==null) {
              dev_str="default";
              parent.prefs.put("audio_output_device", "default");
            }

            for (Mixer.Info i : mixerInfo) {
              String mixer_str = "["+count+"]" + i.getName() + " - " + i.getDescription()+" - "+i.getVendor();
              System.out.println(mixer_str);

              if(dev_str!=null) {
                if( dev_str.equals(mixer_str) ) {
                  sel_i = count; 
                }
              }
              count++;
            }


            mixer_info = mixerInfo[ sel_i ];
            mixer = AudioSystem.getMixer( mixer_info );

            if(mixer==null) {
              System.out.println("mixer is null!!!");
              JOptionPane.showMessageDialog(parent, "AudioSystem.getMixerInfo() returns null", "ok", JOptionPane.OK_OPTION);
            }

            System.out.println("\r\nusing "+ mixer_info.getName() + " - " + mixer_info.getDescription()+" - "+mixer_info.getVendor());
          } catch(Exception e) {
            e.printStackTrace();
          }



        try {
          if(mixer!=null) {
            //if(mixer_gainControl==null) mixer_gainControl = (FloatControl) mixer.getControl(FloatControl.Type.MASTER_GAIN);
            //if(mixer_gainControl!=null) mixer_gainControl.setValue( 0.01f + (mixer_gainControl.getMaximum() * initial_level) );
          }
        } catch(Exception e) {
          //e.printStackTrace();
        }
        try {
          if(mixer!=null) {
            //if(mixer_volume==null) mixer_volume = (FloatControl) mixer.getControl(FloatControl.Type.VOLUME);
            //if(mixer_volume!=null) mixer_volume.setValue( 0.01f + (mixer_volume.getMaximum() * initial_level) );
          }
        } catch(Exception e) {
          //e.printStackTrace();
        }


            //if(af==null) {
              af = new AudioFormat(
                audio_srate,
                16,  // sample size in bits
                2,  // channels
                true,  // signed
                false  // bigendian
              );
            //}

            try {
              dataLineInfo = new DataLine.Info( SourceDataLine.class, af);
              if( dataLineInfo.isFormatSupported(af) ) {
                System.out.println("af is supported");
              }
              else {
                try {
                  audio_srate = 48000;

                  af = new AudioFormat(
                    audio_srate,
                    16,  // sample size in bits
                    2,  // channels
                    true,  // signed
                    false  // bigendian
                  );
                  dataLineInfo = new DataLine.Info( SourceDataLine.class, af);
                } catch(Exception e) {
                    e.printStackTrace();
                } 

              }

              //sourceDataLine = (SourceDataLine)AudioSystem.getLine( dataLineInfo);

              //sourceDataLine = (SourceDataLine)mixer.getLine( dataLineInfo);
                //
              //Line.Info[] line_info = mixer.getSourceLineInfo();
              //for (Line.Info i : lineInfo) {
               // System.out.println(i);
              //}
              

                /*
              if( mixer.isLineSupported(dataLineInfo) ) {
                System.out.println("line is supported");
              }
              else {
                System.out.println("line is NOT supported");
              }
                */

              sourceDataLine = AudioSystem.getSourceDataLine( af, mixer_info );

              if(parent.is_linux==1) {
                sourceDataLine.open(af, 48000*4);
              }
              else {
                sourceDataLine.open(af, 48000*4);
              }


              try {
                if(src_gainControl==null) src_gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                //src_gainControl.setValue( 0.01f + (src_gainControl.getMaximum() * initial_level) );
              } catch(Exception e) {
                //e.printStackTrace();
              }
              try {
                if(src_volume==null) src_volume = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
                //src_volume.setValue( 0.01f + (src_volume.getMaximum() * initial_level) );
              } catch(Exception e) {
                //e.printStackTrace();
              }
              System.out.println("two channel");
            } catch(Exception e) {
            }

                /*

          //if(listener==null) {
            listener = new LineListener() {
            public void update(LineEvent event) {
              if (event.getType() == LineEvent.Type.OPEN) {
                //System.out.println("LINE OPEN EVENT");
              }
              if (event.getType() == LineEvent.Type.CLOSE) {
                //System.out.println("LINE CLOSE EVENT");
              }
              if (event.getType() == LineEvent.Type.STOP) {
                //System.out.println("LINE STOP EVENT");
                start_playing=0;
              }
              if (event.getType() == LineEvent.Type.START) {
                //System.out.println("LINE START EVENT");
              }
            }
          };
        //}
                */

        prev_selection = sel_i;

        //sourceDataLine.addLineListener(listener);


    } catch(Exception e) {
     e.printStackTrace();
    }


    dev_changed=0;
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public audio(BTFrame p) {
    parent = p;

    resamp = new Resampler( Rational.valueOf( (48000.0f/8000.0f) ) ); 



    //if(!initialized) {
      try {
        mixerInfo = AudioSystem.getMixerInfo();
        int count = 0;
        mixer_v = new Vector();

        sel_i=0;
        String dev_str = parent.prefs.get("audio_output_device", "default");
        if(dev_str==null || mixerInfo==null) {
          dev_str="default";
          parent.prefs.put("audio_output_device", "default");

          if(mixerInfo==null) {
            mixer_v.addElement( new String("default (no audio devices found)") );
          }
          else {
            mixer_v.addElement( new String("default") );
          }
        }

        try {
          if(mixerInfo!=null) {
            for (Mixer.Info i : mixerInfo) {
              String mixer_str = "["+count+"]" + i.getName() + " - " + i.getDescription()+" - "+i.getVendor();

              //if( (parent.audio_dev_play.isSelected() && parent.is_windows==1 && mixer_str.contains("Playback")) 
              //  || !parent.audio_dev_play.isSelected() ||
              //     parent.is_windows==0 ) {

                if(parent.prefs!=null && dev_str!=null) {
                  if( dev_str.equals(mixer_str) ) {
                    sel_i = count; 
                  }
                }
                mixer_v.addElement( mixer_str );
                count++;
              //}
            }
          }
          else {
            JOptionPane.showMessageDialog(parent, "AudioSystem.getMixerInfo() returns null", "ok", JOptionPane.OK_OPTION);
          }
        } catch(Exception e) {
          e.printStackTrace();
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          JOptionPane.showMessageDialog(parent, sw.toString(), "ok", JOptionPane.OK_OPTION);
        }

     } catch(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        JOptionPane.showMessageDialog(parent, sw.toString(), "ok", JOptionPane.OK_OPTION);
     }

    parent.audio_dev_list.setListData(mixer_v);
    parent.audio_dev_list.setSelectedIndex( sel_i );
    prev_selection = sel_i;

    init();

  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void closeAll() {
    if(mixer!=null) mixer.close();
    if(sourceDataLine!=null) sourceDataLine.stop();
    if(sourceDataLine!=null) sourceDataLine.close();
    parent.audio_prog.setValue(0);
    //if(sourceDataLine!=null) sourceDataLine.removeLineListener(listener);
    try {
      SLEEP(1);
    } catch(Exception e) {
    }
    sourceDataLine=null;
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void setMixerGain(float val) {
    //if(mixer_gainControl!=null) mixer_gainControl.setValue(val);
    //if(mixer_volume!=null) mixer_volume.setValue(val);
    //if(src_volume!=null) src_volume.setValue(val);
    //if(src_gainControl!=null) src_gainControl.setValue(val);
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void setAgcGain(float val) {
    agc_gain = val;
  }


  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void audio_tick() {

    if(dev_changed==1) {
      init();
      return;
    }

    if(sourceDataLine==null) return;

    if(sourceDataLine.isRunning() && sourceDataLine.getBufferSize()==sourceDataLine.available() ) {
      voice_count=0;
      if(debug) System.out.println("stop");
      sourceDataLine.stop();
      parent.audio_prog.setValue(0);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void playStop() {

    if(debug) System.out.println("drain");
    //do_drain=1;
    byte[] b = new byte[3200];
    sourceDataLine.write(b, 0, 3200);

    int bsize = sourceDataLine.getBufferSize();
    int bavail = sourceDataLine.available();
    if( ((float) bavail / (float) bsize) < 0.8 ) { 

      if(!sourceDataLine.isRunning()) {
        sourceDataLine.start();
      }
    }

  }
  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void playBuf(byte[] buffer) throws LineUnavailableException
  {

    try {
      //System.out.println("buffer len: "+buffer.length);

      if(dev_changed==1) return;

      if(!parent.enable_audio.isSelected()) return;

      vc_timer=0;

      try {
        if( af!=null ) {

          if(buffer==null || buffer.length!=320) return; 

          if(parent.mute.isSelected()) return;

          ByteBuffer bg = ByteBuffer.wrap(buffer);
          bg.order(ByteOrder.LITTLE_ENDIAN);

          int[] buffer_in = new int[ buffer.length/2 ];
          for(int i=0;i<buffer_in.length;i++) {
            buffer_in[i] = (int) bg.getShort()/2;
          }

          int[] buffer_out = new int[ buffer_in.length * 6 ];

          int nsamp = resamp.resample(buffer_in, buffer_out, 0, 1);

          int[] buffer_out_agc = buffer_out; 

          Boolean isWindows = System.getProperty("os.name").startsWith("Windows");

          byte[] outbytes = new byte[ buffer_out_agc.length * 2 *2]; 

          int idx=0;
          for(int i=0;i<buffer_out_agc.length;i++) {
            outbytes[idx+0] = (byte)(buffer_out_agc[i]&(byte)0xff);
            outbytes[idx+1] = (byte)(buffer_out_agc[i]>>8&(byte)0xff);

            outbytes[idx+2] = (byte)(buffer_out_agc[i]&(byte)0xff);
            outbytes[idx+3] = (byte)(buffer_out_agc[i]>>8&(byte)0xff);

            idx+=4;
          }

          sourceDataLine.write(outbytes, 0, idx);
          did_audio=180;

          int bsize = sourceDataLine.getBufferSize();
          int bavail = sourceDataLine.available();
          //if(voice_count++>10) {
          if( ((float) bavail / (float) bsize) < 0.8 ) { 
            voice_count=0;
            start_timer=0;

            if(!sourceDataLine.isRunning()) {
              byte[] b = new byte[3200*4];
              sourceDataLine.write(b, 0, 3200*4);

              sourceDataLine.start();
              do_start=1;
              if(debug) System.out.println("source: Start");
            }
          }
          else {
            if(!sourceDataLine.isRunning()) {
              start_timer=1000;
            }
          }

        }

      } catch(Exception e) {
        e.printStackTrace();
      }



    } catch(Exception e) {
      e.printStackTrace();
    }


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
