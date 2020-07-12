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
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

import pcmsampledsp.*;

public class audio {

  Boolean initialized=false;
  AudioFormat af=null;
  Mixer mixer=null;
  SourceDataLine sourceDataLine;
  Resampler resamp;
  audio_agc agc;
  float agc_gain = 1.0f;
  int start_playing=0;

FloatControl mixer_gainControl;
FloatControl mixer_volume;
FloatControl src_gainControl;
FloatControl src_volume;

BTFrame parent;
float initial_level=0.85f;

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public audio(BTFrame p) {
    parent = p;

    resamp = new Resampler( Rational.valueOf( (48000.0f/8000.0f) ) ); 
    agc = new audio_agc();

    try {
      if(!initialized) {
        initial_level = (float) ( ((float) parent.initial_audio_level.getValue()+0.01f) ) / 100.0f;
        try {
          Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
          int count = 0;
          for (Mixer.Info i : mixerInfo) {
            System.out.println("["+(count++)+"]" + i.getName() + " - " + i.getDescription()+" - "+i.getVendor());
          }
          //default
          mixer = AudioSystem.getMixer(mixerInfo[0]);

          System.out.println("mixer "+mixer);
        } catch(Exception e) {
          e.printStackTrace();
        }
        try {
          if(mixer!=null) {
            if(mixer_gainControl==null) mixer_gainControl = (FloatControl) mixer.getControl(FloatControl.Type.MASTER_GAIN);
            mixer_gainControl.setValue( 0.01f + (mixer_gainControl.getMaximum() * initial_level) );
          }
        } catch(Exception e) {
          //e.printStackTrace();
        }
        try {
          if(mixer!=null) {
            if(mixer_volume==null) mixer_volume = (FloatControl) mixer.getControl(FloatControl.Type.VOLUME);
            mixer_volume.setValue( 0.01f + (mixer_volume.getMaximum() * initial_level) );
          }
        } catch(Exception e) {
          //e.printStackTrace();
        }
        initialized=true;
      }

        //try one channel
      if(af==null) {

              if(parent.audio_slow_rate.isSelected()) {
                  af = new AudioFormat(
                    47040,
                    16,  // sample size in bits
                    2,  // channels
                    true,  // signed
                    false  // bigendian
                  );
              }
              else {
                  af = new AudioFormat(
                    48000,
                    16,  // sample size in bits
                    2,  // channels
                    true,  // signed
                    false  // bigendian
                  );
              }

            try {
              DataLine.Info dataLineInfo = new DataLine.Info( SourceDataLine.class, af);
              sourceDataLine = (SourceDataLine)AudioSystem.getLine( dataLineInfo);

              if(parent.audio_buffer_system.isSelected()) {
                sourceDataLine.open(af);
              }
              else {
                sourceDataLine.open(af, 48000*4);
              }


              sourceDataLine.start();

              try {
                if(src_gainControl==null) src_gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                src_gainControl.setValue( 0.01f + (src_gainControl.getMaximum() * initial_level) );
              } catch(Exception e) {
                //e.printStackTrace();
              }
              try {
                if(src_volume==null) src_volume = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
                src_volume.setValue( 0.01f + (src_volume.getMaximum() * initial_level) );
              } catch(Exception e) {
                //e.printStackTrace();
              }
              System.out.println("two channel");
            } catch(Exception e) {
            }

          LineListener listener = new LineListener() {
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

          sourceDataLine.addLineListener(listener);
      }
    } catch(Exception e) {
     e.printStackTrace();
    }
  }
  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  void updateLevels() {
    try {
      initial_level = (float) ( ((float) parent.initial_audio_level.getValue()+0.01f) ) / 100.0f;
      if(initialized) {
        try {
          src_volume.setValue( 0.01f + (src_volume.getMaximum() * initial_level) );
        } catch(Exception e) {
        }
        try {
          src_gainControl.setValue( 0.01f + (src_gainControl.getMaximum() * initial_level) );
        } catch(Exception e) {
        }
        try {
          mixer_gainControl.setValue( 0.01f + (mixer_gainControl.getMaximum() * initial_level) );
        } catch(Exception e) {
        }
        try {
          mixer_volume.setValue( 0.01f + (mixer_volume.getMaximum() * initial_level) );
        } catch(Exception e) {
        }
      }
    } catch(Exception e) {
     //e.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void closeAll() {
    if(sourceDataLine!=null) sourceDataLine.stop();
    if(sourceDataLine!=null) sourceDataLine.close();
    if(mixer!=null) mixer.close();
    try {
      Thread.sleep(1);
    } catch(Exception e) {
    }
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
  public void playStop() {
    if(sourceDataLine==null) return;


    Boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    //if(!isWindows && !sourceDataLine.isRunning() && start_playing>0) sourceDataLine.start();

    //if(isWindows) {
      if(sourceDataLine.isOpen() && sourceDataLine.isRunning()) sourceDataLine.drain();
      if(sourceDataLine.isRunning()) sourceDataLine.stop();
    //}


  }
  /////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////
  public void playBuf(byte[] buffer) throws LineUnavailableException
  {

    try {
      //System.out.println("buffer len: "+buffer.length);

      if(!parent.enable_audio.isSelected()) return;

      try {
        if( af!=null ) {

          if(buffer==null || buffer.length!=320) return; 

          ByteBuffer bg = ByteBuffer.wrap(buffer);
          bg.order(ByteOrder.LITTLE_ENDIAN);

          int[] buffer_in = new int[ buffer.length/2 ];
          for(int i=0;i<buffer_in.length;i++) {
            buffer_in[i] = (int) bg.getShort();
          }

          int[] buffer_out = new int[ buffer_in.length * 6 ];

          int nsamp = resamp.resample(buffer_in, buffer_out, 0, 1);

          //int[] buffer_out_agc = agc.update_gain_s16(buffer_out, buffer_out.length, 200.0f + (31500.0f*agc_gain*1.2f) , 29.42f, 0.05f);
          int[] buffer_out_agc = agc.update_gain_s16(buffer_out, buffer_out.length, 200.0f + (31500.0f*agc_gain) , 1351.0f, 1.0f);

          //for(int i=0;i<buffer_out.length;i++) {
           // System.out.println("i="+i+"  "+buffer_out[i]);
          //}

          //if(!sourceDataLine.isOpen()) sourceDataLine.open();


          Boolean isWindows = System.getProperty("os.name").startsWith("Windows");

          //may reduce java audio glitches
            if(parent.audio_insert_zero.isSelected()) {
              if(!sourceDataLine.isRunning()) {
                byte[] zero = new byte[24000];
                sourceDataLine.write(zero, 0, zero.length);
                sourceDataLine.start();
              }
            }

          if(isWindows || parent.is_mac_osx==1) {
            //if(!sourceDataLine.isRunning()) sourceDataLine.start();
            if( parent.audio_buffer_system.isSelected() ) {
              if(!sourceDataLine.isRunning() ) sourceDataLine.start();
            }
            else {
              if(!sourceDataLine.isRunning()) sourceDataLine.start();
            }
          }
          else {


            if( parent.audio_buffer_system.isSelected() ) {
              if(!sourceDataLine.isRunning() && start_playing++>100) sourceDataLine.start();
            }
            else {
              if(!sourceDataLine.isRunning()) sourceDataLine.start();
            }

          }

          byte[] outbytes = new byte[ buffer_out_agc.length * 2 *2]; 

          int idx=0;
          for(int i=0;i<buffer_out_agc.length;i++) {
            outbytes[idx+0] = (byte)(buffer_out_agc[i]&(byte)0xff);
            outbytes[idx+1] = (byte)(buffer_out_agc[i]>>8&(byte)0xff);

            outbytes[idx+2] = (byte)(buffer_out_agc[i]&(byte)0xff);
            outbytes[idx+3] = (byte)(buffer_out_agc[i]>>8&(byte)0xff);

            idx+=4;
          }

          sourceDataLine.write(outbytes, 0, outbytes.length);

        }

      } catch(Exception e) {
        e.printStackTrace();
      }



    } catch(Exception e) {
      e.printStackTrace();
    }


  }
}
