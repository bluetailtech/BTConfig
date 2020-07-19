package btconfig; 

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.nio.*;

public class ConstPlotPanel extends JPanel {

   boolean do_log=true;

   static int[] plot_data;
   int plot_idx=0;

   int xoff=0;
   int yoff=+96;
   double avg_mag=0.0;

   int gains_idx;
   static float[] gains = new float[256*3];

   static String current_gain="";
   BTFrame parent;

   public ConstPlotPanel(BTFrame parent) {
     this.parent = parent;

     plot_data = new int[2048];

     for(int i=0;i<256*3;i++) {
       gains[i] = 1024.0f;
     }
   }
   
   public void addData( byte[] data ) {
     do_log = parent.log_const.isSelected();

     int j=0;
     //System.out.println("add data");
     try {
       avg_mag = 0.0;

       for(int i=0;i<316/2;i++) {

         int ii = (int) ((double) data[j++]);
         int qq = (int) ((double) data[j++]);


         try {
           avg_mag += java.lang.Math.pow( ((double) ii * (double) ii) + ((double) qq * (double) qq), 0.5 );
         } catch(Exception e) {
         }


         if(plot_idx>=2048) {
           plot_idx=0;
           //System.out.println("rollover");
         }
       }

       avg_mag /= 158.0;
       //System.out.println("avg "+avg_mag);

       double scale = 80.0 / avg_mag;

       if(!parent.autoscale_const.isSelected()) scale=2.0;

       j = 0;
       for(int i=0;i<316/2;i++) {

         int ii = (int) ((double) data[j++]);
         int qq = (int) ((double) data[j++]);

         if(do_log) {
           int idir=1;
           int qdir=1;
           if(ii<0) idir=-1;
           if(qq<0) qdir=-1;

           plot_data[plot_idx++] = (int) ((double) java.lang.Math.log10( java.lang.Math.abs(ii))*10.0*4.0*idir ); 
           plot_data[plot_idx++] = (int) ((double) java.lang.Math.log10( java.lang.Math.abs(qq))*10.0*4.0*qdir ); 
         }
         else {
           plot_data[plot_idx++] = (int) ((double)ii*scale); 
           plot_data[plot_idx++] = (int) ((double)qq*scale); 
         }

         if(plot_idx>=2048) {
           plot_idx=0;
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

     g2d.setColor( Color.yellow ); 
     int j=0;
     for(int i=0;i<2048/2;i++) {
       int ii = plot_data[j++];
       int qq = plot_data[j++];
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




   static public void static_paint(Graphics g, int x, int y, int scale ) {
     //super.paint(g);
     Graphics2D g2d = (Graphics2D) g;

     g2d.setStroke( new BasicStroke(2.0f) );

      //clear to black
     g2d.setColor( Color.black ); 
     g2d.fill3DRect(0,0,256,256,false); 

     g2d.setColor( Color.green ); 

     //default w/h  is 256 x 256
     int scale1 = scale;
     int scale2 = scale/2;

     g2d.drawLine(x+5+scale2,scale-y+5,x+5+scale2+scale,scale-y+5); 
     g2d.drawLine(x+5+scale,scale2-y+5,x+5+scale,scale2+scale-y+5); 

     //draw constellation
     g2d.setColor( Color.yellow ); 
     int j=0;
     for(int i=0;i<2048/2;i++) {
       int ii = (int) ((float) plot_data[j++] * (float) 1.0f/84.0f * (float) scale2);
       int qq = (int) ((float) plot_data[j++] * (float) 1.0f/84.0f * (float) scale2);
       g2d.drawRoundRect(4+ii+scale/2, 4+qq+scale/2, 1, 1, 1, 1);
     }



   }

}
