package btconfig; 

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.nio.*;

public class ConstPlotPanel extends JPanel {

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
   BTFrame parent;
   int last_sync_state=0;

   ByteBuffer bb;
   boolean do_synced=false;

   ///////////////////////////////////////////////////////////////////////////////////////
   ///////////////////////////////////////////////////////////////////////////////////////
   public ConstPlotPanel(BTFrame parent) {
     this.parent = parent;

     plot_data = new int[8192*2];
     scaled_data = new int[ 8192*2 ];
     DATA_SIZE = 256;

     for(int i=0;i<256*3;i++) {
       gains[i] = 1024.0f;
     }
     for(int i=0;i<256*3;i++) {
       syncs[i] = -1; 
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

       
       bb = ByteBuffer.wrap(data);
       bb.order(ByteOrder.LITTLE_ENDIAN);

       float err_hz_f = bb.getFloat(300);
       float est_hz_f = bb.getFloat(304);
       int rfgain = bb.getInt(308);
       float gain = bb.getFloat(312);

       est_hz = "Frequency Error Estimate: "+String.format("%3.1f", est_hz_f)+" Hz";
       err_hz = "Applied Frequency Correction: "+String.format("%3.1f", err_hz_f)+" Hz";

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

     g2d.setColor( Color.yellow ); 
     j=0;
     for(int i=0;i<DATA_SIZE/2;i++) {
       int ii = scaled_data[j++];
       int qq = scaled_data[j++];
       g2d.drawRoundRect(xoff+256+ii, 256+qq-yoff, 1, 1, 1, 1);
     }

     int yoff2=-80;

     //draw agc gains
     g2d.setColor( Color.white ); 
     j=0;
     for(int i=0;i<256*3;i++) {
       //g2d.drawLine( j+128, (int) gains[j]+256, j+129, (int) gains[j+1]+256 );
       //j+=2;
       g2d.drawRoundRect(i+128, (int) (yoff2 + 875 - ((2.5f*gains[j++])+320) ),1, 1, 1, 1);
     }

     g2d.setColor( Color.white ); 
     g2d.drawString(current_gain, 300+256,50);
     g2d.drawString(current_rf_gain, 300+256,75);

     try {
       g2d.drawString(est_hz, 300+256,125);
       g2d.drawString(err_hz, 300+256,150);
     } catch(Exception e) {
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
         g2d.drawRoundRect(i+128, (int) yoff2 + 470 - syncs[j++]*sync_off,1, 1, 1, 1);
       }

       if(last_sync_state==1) {
         g2d.setColor( Color.green ); 
         g2d.drawString(sync_state+" (Synced)", 300+256,100);
       }
       else if(last_sync_state==0)  {
         g2d.setColor( Color.red ); 
         g2d.drawString(sync_state+" (No Sync)", 300+256,100);
       }
       else if(last_sync_state==-2)  {
         g2d.setColor( Color.yellow ); 
         g2d.drawString(sync_state+" (TDU)", 300+256,100);
       }
       else {
         g2d.setColor( Color.black ); 
         g2d.drawString(sync_state+" (No Signal)", 300+256,100);
       }
     }

   }

}
