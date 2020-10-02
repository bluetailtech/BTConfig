package btconfig; 

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.nio.*;

public class ConstPlotPanel extends JPanel {

  double[] win128 = {
    -1.387778780781446e-17 ,0.0002204846265978777 ,0.0008842693835267035 ,0.001998313117544823 ,0.003574101648471856 ,0.005627480629291748 ,0.008178424032046938 ,0.01125074071156361
    ,0.01487172215751585 ,0.01907173517278627 ,0.0238837638062516 ,0.02934290541510913 ,0.03548582623019152 ,0.04235018224239888 ,0.04997401161493553 ,0.05839510515057771
    ,0.0676503616024359 ,0.07777513480795592 ,0.08880257974726191 ,0.1007630046770813 ,0.1136832364698327 ,0.1275860061941068 ,0.1424893618085668 ,0.1584061146077571
    ,0.1753433257576353 ,0.1933018388937232 ,0.2122758643290848 ,0.2322526199370117 ,0.2532120332389672 ,0.2751265086472146 ,0.297960763189246 ,0.3216717333837154
    ,0.3462085552514348 ,0.3715126187368337 ,0.3975176970919893 ,0.4241501510439971 ,0.4513292068342076 ,0.4789673064918618 ,0.506970527992046 ,0.5352390722556231
    ,0.5636678132836842 ,0.5921469070876366 ,0.6205624544845052 ,0.6487972122812109 ,0.6767313468768822 ,0.7042432238735404 ,0.7312102269071388 ,0.7575095985966889
    ,0.7830192962622085 ,0.8076188548849877 ,0.8311902496779788 ,0.8536187506011261 ,0.874793761196537 ,0.8946096342312827 ,0.9129664568203139 ,0.9297707979567269
    ,0.9449364116991064 ,0.9583848896527869 ,0.9700462568300098 ,0.9798595054787754 ,0.9877730620269392 ,0.9937451828914111 ,0.9977442755464379 ,0.9997491419236825
    ,0.9997491419236825 ,0.9977442755464379 ,0.9937451828914112 ,0.9877730620269393 ,0.9798595054787754 ,0.9700462568300098 ,0.9583848896527869 ,0.9449364116991065
    ,0.929770797956727 ,0.9129664568203139 ,0.8946096342312828 ,0.8747937611965373 ,0.8536187506011264 ,0.8311902496779787 ,0.8076188548849879 ,0.7830192962622088
    ,0.757509598596689 ,0.731210226907139 ,0.7042432238735405 ,0.6767313468768824 ,0.6487972122812109 ,0.6205624544845051 ,0.5921469070876365 ,0.5636678132836845
    ,0.5352390722556234 ,0.5069705279920462 ,0.4789673064918619 ,0.4513292068342081 ,0.424150151043997 ,0.3975176970919891 ,0.3715126187368336 ,0.346208555251435
    ,0.3216717333837155 ,0.2979607631892461 ,0.2751265086472146 ,0.2532120332389676 ,0.2322526199370117 ,0.2122758643290848 ,0.1933018388937234 ,0.1753433257576355
    ,0.1584061146077572 ,0.1424893618085668 ,0.1275860061941068 ,0.113683236469833 ,0.1007630046770813 ,0.08880257974726184 ,0.07777513480795607 ,0.06765036160243595
    ,0.05839510515057778 ,0.04997401161493557 ,0.04235018224239902 ,0.03548582623019163 ,0.02934290541510911 ,0.02388376380625157 ,0.01907173517278628 ,0.01487172215751588
    ,0.01125074071156365 ,0.008178424032046938 ,0.005627480629291789 ,0.00357410164847187 ,0.001998313117544823 ,0.0008842693835267451 ,0.0002204846265978638 ,-1.387778780781446e-17
  };

   static int DATA_SIZE=256;

   int draw_mod=0;
   boolean do_log=true;

   static int[] plot_data;
    int[] scaled_data;
   int plot_idx=0;

   int xoff=0;
   int yoff=+96;
   double avg_mag=0.0;
   double scale=1.0;

   int gains_idx;
   static float[] gains = new float[256*3];

   int sync_idx;
   static int[] syncs = new int[256*3];

   static String current_gain="";
   static String current_rf_gain="";
   static String err_hz="";
   static String est_hz="";
   static String sync_state="";
   static String ref_freq_est="";
   BTFrame parent;
   int last_sync_state=0;

   ByteBuffer bb;
   boolean do_synced=false;

   int est_ref_cnt=0;
   double[] est_ref_array = new double[2048];
   int est_ref_tot=0;
   double ref_freq_error=0.0;

   boolean do_display_ref_est=true;

   short[] audio_bytes;

   int paint_audio;
   int audio_frame_count;
   FastFourierTransform fft;

   double audio_in[];
   double audio_out[];
   boolean did_draw_audio_fft=true;


   ///////////////////////////////////////////////////////////////////////////////////////
   ///////////////////////////////////////////////////////////////////////////////////////
   public ConstPlotPanel(BTFrame parent) {
     this.parent = parent;

     fft = new FastFourierTransform(256);
     audio_in = new double[256];
     audio_out = new double[256*2];

     plot_data = new int[8192*2];
     scaled_data = new int[ 8192*2 ];
     DATA_SIZE = 256;

     paint_audio=0;

     audio_bytes = new short[160];

     for(int i=0;i<256*3;i++) {
       gains[i] = 1024.0f;
     }
     for(int i=0;i<256*3;i++) {
       syncs[i] = -1; 
     }
   }

   ///////////////////////////////////////////////////////////////////////////////////////
   ///////////////////////////////////////////////////////////////////////////////////////
   public void reset_ref_est() {
     est_ref_cnt=0;
     est_ref_array = new double[2048];
     est_ref_tot=0;
     ref_freq_error=0.0;
   }

   ///////////////////////////////////////////////////////////////////////////////////////
   ///////////////////////////////////////////////////////////////////////////////////////
   public void addAudio(byte[] pcm) {
     try {
       bb = ByteBuffer.wrap(pcm);
       bb.order(ByteOrder.LITTLE_ENDIAN);
       for(int i=0;i<160;i++) {
         audio_bytes[i] = bb.getShort();
         if(i<128) {
           audio_in[i] = (double) audio_bytes[i] * win128[i];
         }
         audio_bytes[i] /= 200;
       }

       if(did_draw_audio_fft) {
         fft.applyReal( audio_in, 0, true, audio_out, 0);  //real in, complex out
         did_draw_audio_fft=false;
       }

       paint_audio=15;
       audio_frame_count++;
       if(audio_frame_count%2==0) repaint();
     } catch(Exception e) {
     }
   }
   
   ///////////////////////////////////////////////////////////////////////////////////////
   ///////////////////////////////////////////////////////////////////////////////////////
   public void addData( byte[] data , boolean do_synced) {

     int j=0;
     //System.out.println("add data");
     try {
       this.do_synced = do_synced;

       for(int i=0;i<300/2;i++) {

         int ii = (int) ((double) data[j++]);
         int qq = (int) ((double) data[j++]);

         plot_data[plot_idx++] = ii;
         plot_data[plot_idx++] = qq;

         if(plot_idx>=DATA_SIZE) {
           plot_idx=0;
           //System.out.println("rollover");
           DATA_SIZE = (int) java.lang.Math.pow( 2.0, parent.nsymbols.getSelectedIndex()+8 ); 
         }
       }

       do_display_ref_est=false;

       /*
       if( parent.op_mode.getSelectedIndex()==1 && parent.dmr_conventional.isSelected()) {
       }
       */
       if( parent.op_mode.getSelectedIndex()==0 && parent.controlchannel.isSelected()) {
         do_display_ref_est=true;
       }

       
       bb = ByteBuffer.wrap(data);
       bb.order(ByteOrder.LITTLE_ENDIAN);

       float err_hz_f = bb.getFloat(300);
       float est_hz_f = bb.getFloat(304);
       int rfgain = bb.getInt(308);
       float gain = bb.getFloat(312);

       double ppb_est = 0.0; 
       double ppb2 = 0.0;
       if(parent.current_freq!=0.0) {
         ppb_est = est_hz_f / parent.current_freq;
         ppb_est *= 1e9;
         ppb2 = err_hz_f / parent.current_freq;
         ppb2 *= 1e9;
       }

       double rfreq = 0.0;
       int ref_correct = 0;
       double rfreq_cor=0.0;
       double est_ref_sum=0.0;

       try {
         rfreq = new Double( parent.ref_freq.getText() ).doubleValue();
         rfreq_cor = rfreq/1e9;
         rfreq_cor *= (ppb_est+ppb2);
         ref_correct = (int) (rfreq-rfreq_cor);;

         est_ref_array[est_ref_cnt++] = ref_correct; 
         if(est_ref_cnt==2048) est_ref_cnt=0;
         est_ref_tot++;

         for(int i=0;i<2048;i++) {
           est_ref_sum += est_ref_array[i];
         }
         est_ref_sum /= 2048.0;

         ref_freq_error = java.lang.Math.abs( rfreq-est_ref_sum );

       } catch(Exception e) {
       }
       
       //if( ppb_est!=0.0 && ppb2!=0.0) {
         if(est_ref_tot>2048 && est_ref_sum > 39999820 && est_ref_sum < 39999965 ) {
           ref_freq_est = "Estimated Reference Frequency: "+ String.format( "%3.0f", est_ref_sum);
         }
         else {
           ref_freq_est = "Estimated Reference Frequency: ";
         }
       //}
       //else {
        //   ref_freq_est = "";
       //}

       if(ppb_est!=0.0) {
         est_hz = "Frequency Error Estimate: "+String.format("%3.1f", est_hz_f)+" Hz,   "+String.format("%3.0f", ppb_est)+" ppb";
         err_hz = "Applied Frequency Correction: "+String.format("%3.1f", err_hz_f)+" Hz";
       }
       else {
         est_hz = "Frequency Error Estimate: "+String.format("%3.1f", est_hz_f)+" Hz";
         err_hz = "Applied Frequency Correction: "+String.format("%3.1f", err_hz_f)+" Hz";
       }

       //System.out.println("gain: "+java.lang.Math.log10(gain)*20.0f);
       current_gain = "soft agc gain: "+String.format("%3.1f", java.lang.Math.log10(gain)*20.0f)+" dB";
       current_rf_gain = "front-end RF gain: "+String.format("%d", rfgain)+" dB";


       gains[gains_idx++] = (float) java.lang.Math.log10(gain)*20.0f;
       if(gains_idx==256*3) gains_idx=0;

       int synced = bb.getInt(316);
       if(synced>0) synced=1;
       sync_state = "sync state: "+synced;
       syncs[sync_idx++] = synced; 
       if(sync_idx==256*3) sync_idx=0;

       last_sync_state=synced;

       if(draw_mod++%2==0) {
         repaint();
         parent.jPanel24.repaint();
       }

     } catch(Exception e) {
       plot_idx=0;
       gains_idx=0;
       sync_idx=0;
       DATA_SIZE = (int) java.lang.Math.pow( 2.0, parent.nsymbols.getSelectedIndex()+8 ); 
       e.printStackTrace();
     }
   }
   
   ///////////////////////////////////////////////////////////////////////////////////////
   ///////////////////////////////////////////////////////////////////////////////////////
   public void paint(Graphics g){
     //super.paint(g);
     Graphics2D g2d = (Graphics2D) g;

     //Rectangle r = g2d.getClipBounds();
     Rectangle r = getBounds(); 
     g2d.setStroke( new BasicStroke(2.0f) );
      //clear to black
     g2d.setColor( Color.black ); 
     g2d.fill3DRect(r.x-250,r.y-250,r.width+500,r.height+500,false); 

     g2d.setColor( Color.green ); 
     g2d.drawLine(xoff+128,256-yoff,xoff+128+256,256-yoff); 
     g2d.drawLine(xoff+256,128-yoff,xoff+256,128+256-yoff); 


     //System.out.println("avg "+avg_mag);
     do_log = parent.log_const.isSelected();

     if( parent.off_const.isSelected() ) return;

     int j = 0;
     avg_mag = 0.0;
     for(int i=0;i<DATA_SIZE;i++) {

       int ii = (int) ((double) plot_data[j++]);
       int qq = (int) ((double) plot_data[j++]);
       try {
         double mag = java.lang.Math.pow( ((double) ii * (double) ii) + ((double) qq * (double) qq), 0.5 );
         if(mag > avg_mag) avg_mag = mag; 
       } catch(Exception e) {
       }

     }

     //avg_mag /= DATA_SIZE; 
     scale = 100.0 / avg_mag;

     if(!parent.autoscale_const.isSelected()) scale=2.0;


     j=0;
     int j2=0;
     for(int i=0;i<DATA_SIZE;i++) {

         int ii = (int) ((double) plot_data[j++]);
         int qq = (int) ((double) plot_data[j++]);

         if(do_log) {
           int idir=1;
           int qdir=1;
           if(ii<0) idir=-1;
           if(qq<0) qdir=-1;

           scaled_data[j2++] = (int) ((double) java.lang.Math.log10( java.lang.Math.abs(ii))*10.0*4.0*idir ); 
           scaled_data[j2++] = (int) ((double) java.lang.Math.log10( java.lang.Math.abs(qq))*10.0*4.0*qdir ); 
         }
         else {
           scaled_data[j2++] = (int) ((double)ii*scale); 
           scaled_data[j2++] = (int) ((double)qq*scale); 
         }
      }

     g2d.setColor( Color.white ); 
     g2d.drawString("I/Q Symbol Plot", 150,24);
     g2d.drawString("RF AGC Gain", 10,350);
     g2d.drawString("Sync Status", 10,380);

     //draw x/y plot
     g2d.setColor( Color.yellow ); 
     j=0;
     for(int i=0;i<DATA_SIZE/2;i++) {
       int ii = scaled_data[j++];
       int qq = scaled_data[j++];
       g2d.drawRoundRect(xoff+256+ii, 256+qq-yoff, 1, 1, 1, 1);
     }

     /*
     g2d.drawString("I/Q Symbol Time Domain", 850,470);
     
     int iq_yoff = 420;
     int iq_xoff = 850;

     //draw I
     //g2d.setColor( Color.red ); 
     g2d.setColor( new Color(1.0f, 0.0f, 0.0f, 0.7f) ); 
     j=0;
     int iq_skip=0;
     int ii1 = 0; 
     int qq1 = 0; 
     int ii2 = 0;
     int qq2 = 0;
     for(int i=0;i<DATA_SIZE/2;i++) {

       if(iq_skip++%2==0) { 
         ii1 = scaled_data[j++]/2;
         qq1 = scaled_data[j++]/2;
         ii2 = scaled_data[j++]/2;
         qq2 = scaled_data[j++]/2;
       }
       g2d.drawLine(iq_xoff++, ii1+iq_yoff, iq_xoff, ii2+iq_yoff );
     }
     //draw Q
     //g2d.setColor( Color.blue ); 
     g2d.setColor( new Color(0.0f, 0.0f, 1.0f, 0.7f) ); 
     j=0;
     iq_skip=0;
     iq_xoff = 850;

     for(int i=0;i<DATA_SIZE/2;i++) {

       if(iq_skip++%2==0) { 
         ii1 = scaled_data[j++]/2;
         qq1 = scaled_data[j++]/2;
         ii2 = scaled_data[j++]/2;
         qq2 = scaled_data[j++]/2;
       }

       g2d.drawLine(iq_xoff++, qq1+iq_yoff, iq_xoff, qq2+iq_yoff );
     }
     */

     int yoff2=-80;
     int xoff2=70;

     //draw agc gains
     g2d.setColor( Color.white ); 
     j=0;
     for(int i=0;i<256*3;i++) {
       //g2d.drawLine( j+xoff2, (int) gains[j]+256, j+129, (int) gains[j+1]+256 );
       //j+=2;
       g2d.drawRoundRect(i+xoff2, (int) (yoff2 + 875 - ((2.5f*gains[j++])+320) ),1, 1, 1, 1);
     }
     
     
     

     //int text_xoff = 300+256;
     int text_xoff = 250+256;

     g2d.setColor( Color.white ); 
     g2d.drawString(current_gain, text_xoff,50);
     g2d.drawString(current_rf_gain, text_xoff,75);

     try {
       g2d.drawString(est_hz, text_xoff,125);
       g2d.drawString(err_hz, text_xoff,150);
     } catch(Exception e) {
     }

     try {
        if( (int) ref_freq_error < 3 ) {
          g2d.setColor( Color.green ); 
        }
        else if( (int) ref_freq_error < 4 ) {
          g2d.setColor( Color.white ); 
        }
        else if( (int) ref_freq_error < 8 ) {
          g2d.setColor( Color.yellow ); 
        }
        else {
          g2d.setColor( Color.red ); 
        }

        if( do_display_ref_est )  {
          if(est_ref_tot>2048 && ref_freq_est!=null )  g2d.drawString(ref_freq_est, text_xoff,175);
        }
     } catch(Exception e) {
     }

     //////////////////////////////////
     //audio plot
     //////////////////////////////////
     boolean b=true;
     if(paint_audio>0 || b) {

       g2d.setColor( Color.white ); 
       g2d.drawString("Audio Frame Count "+audio_frame_count, 840, 125);
       g2d.drawString("Audio FFT 0-8kHz", 860,375);

       int a_xoff = 830;

       g2d.setColor( Color.green ); 
       for(int i=0;i<160-1;i++) {
         //g2d.drawRoundRect(i+850, audio_bytes[i]+132, 1, 1, 1, 1);
         g2d.drawLine(a_xoff++, audio_bytes[i]+132, a_xoff, audio_bytes[i+1]+132 );
       }

       a_xoff = 850;

       //draw grid 
       g2d.setColor( new Color(0.5f,0.5f,0.5f,0.3f ) ); 

       g2d.drawLine(a_xoff, 355, a_xoff, 350-135);
       g2d.drawLine(a_xoff+128, 355, a_xoff+128, 350-135);

       double g_ystep = 140.0/10.0; 
       double g_xstep = 128.0/10.0; 

       for(int i=0;i<11;i++) {
         g2d.drawLine(a_xoff, 215+(int)((double)i*g_ystep), a_xoff+128, 215+(int)((double)i*g_ystep));
       }
       for(int i=0;i<11;i++) {
         g2d.drawLine(a_xoff+(int)((double)i*g_xstep), 355, a_xoff+(int)((double)i*g_xstep), 350-135);
       }



       //draw audio fft 0-8 kHz
       g2d.setColor( Color.yellow ); 
       j=0;
       double prev_mag=350.0;

       if(paint_audio>12 && !did_draw_audio_fft) {



         for(int i=0;i<128-1;i++) {
           double ii = audio_out[j++];
           double qq = audio_out[j++];
           ii = ii*ii;
           qq = qq*qq;
           double mag = 50.0 * java.lang.Math.log10( java.lang.Math.pow(ii+qq, 0.5) );

           if(i>0 && mag > 0 && prev_mag > 0 && mag < 300 && prev_mag < 300) {
             g2d.drawLine(a_xoff++, 340-(int)prev_mag, a_xoff, 340-(int)mag);
           }
           prev_mag = mag;
         }

         did_draw_audio_fft=true;
       }

       paint_audio--;
       if(paint_audio==0) {
         audio_frame_count=0;
         did_draw_audio_fft=true;
       }
     }

     int sync_off=5;

     if(do_synced) {
       //draw sync status 
       g2d.setColor( Color.green ); 
       j=0;
       for(int i=0;i<256*3;i++) {
         if(syncs[j]==1) {
           g2d.setColor( Color.green ); 
           sync_off=5;
         }
         else if(syncs[j]==0)  {
           g2d.setColor( Color.red ); 
           sync_off=0;
         }
         else if(syncs[j]==-2)  {
           g2d.setColor( Color.yellow ); 
           sync_off=0;
         }
         else {
           g2d.setColor( Color.black ); 
           sync_off=0;
         }
         g2d.drawRoundRect(i+xoff2, (int) yoff2 + 470 - syncs[j++]*sync_off,1, 1, 1, 1);
       }

       if(last_sync_state==1) {
         g2d.setColor( Color.green ); 
         g2d.drawString(sync_state+" (Synced)", text_xoff,100);
       }
       else if(last_sync_state==0)  {
         g2d.setColor( Color.red ); 
         g2d.drawString(sync_state+" (No Sync)", text_xoff,100);
       }
       else if(last_sync_state==-2)  {
         g2d.setColor( Color.yellow ); 
         g2d.drawString(sync_state+" (TDU)", text_xoff,100);
       }
       else {
         g2d.setColor( Color.black ); 
         g2d.drawString(sync_state+" (No Signal)", text_xoff,100);
       }
     }

   }

}
