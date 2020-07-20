package btconfig; 

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.nio.*;

public class ConstPlotPanel extends JPanel {

   static int DATA_SIZE=256;

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

   static String current_gain="";
   BTFrame parent;

   public ConstPlotPanel(BTFrame parent) {
     this.parent = parent;

     plot_data = new int[8192*2];
     scaled_data = new int[ 8192*2 ];
     DATA_SIZE = 256;

     for(int i=0;i<256*3;i++) {
       gains[i] = 1024.0f;
     }
   }
   
   public void addData( byte[] data ) {

     int j=0;
     //System.out.println("add data");
     try {

       for(int i=0;i<316/2;i++) {

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

       
       ByteBuffer bb = ByteBuffer.wrap(data);
       bb.order(ByteOrder.LITTLE_ENDIAN);

       float gain = bb.getFloat(316);
       //System.out.println("gain: "+java.lang.Math.log10(gain)*20.0f);
       current_gain = "soft agc gain: "+String.format("%3.1f", java.lang.Math.log10(gain)*20.0f)+" dB";


       gains[gains_idx++] = (float) java.lang.Math.log10(gain)*20.0f;
       if(gains_idx==256*3) gains_idx=0;


       //invalidate();
       repaint();
       parent.jPanel24.repaint();
     } catch(Exception e) {
       plot_idx=0;
       DATA_SIZE = (int) java.lang.Math.pow( 2.0, parent.nsymbols.getSelectedIndex()+8 ); 
       e.printStackTrace();
     }
   }
   
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
         avg_mag += java.lang.Math.pow( ((double) ii * (double) ii) + ((double) qq * (double) qq), 0.5 );
       } catch(Exception e) {
       }

     }

     avg_mag /= DATA_SIZE; 
     scale = 50.0 / avg_mag;

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

     //draw agc gains
     g2d.setColor( Color.white ); 
     j=0;
     for(int i=0;i<256*3;i++) {
       //g2d.drawLine( j+128, (int) gains[j]+256, j+129, (int) gains[j+1]+256 );
       //j+=2;
       g2d.drawRoundRect(i+128, (int) (875 - ((2.5f*gains[j++])+320) ),1, 1, 1, 1);
     }

     g2d.setColor( Color.white ); 
     g2d.drawString(current_gain, 300+256,50);
   }

}
