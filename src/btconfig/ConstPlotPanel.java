package btconfig; 

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

public class ConstPlotPanel extends JPanel {

   static int[] plot_data;
   int plot_idx=0;

   int xoff=0;
   int yoff=+96;

   public ConstPlotPanel() {
     plot_data = new int[2048];
   }
   
   public void addData( byte[] data ) {
     int j=0;
     //System.out.println("add data");
     try {
       for(int i=0;i<320/2;i++) {
         plot_data[plot_idx++] = (int) ((double) data[j++]*1.5);
         plot_data[plot_idx++] = (int) ((double) data[j++]*1.5);
         if(plot_idx>=2048) {
           plot_idx=0;
           //System.out.println("rollover");
         }
       }
       //invalidate();
       repaint();
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
     g2d.fill3DRect(r.x,r.y,r.width,r.height,false); 

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

     g2d.setColor( Color.yellow ); 
     int j=0;
     for(int i=0;i<2048/2;i++) {
       int ii = (int) ((float) plot_data[j++] * (float) 1.0f/84.0f * (float) scale2);
       int qq = (int) ((float) plot_data[j++] * (float) 1.0f/84.0f * (float) scale2);
       g2d.drawRoundRect(4+ii+scale/2, 4+qq+scale/2, 1, 1, 1, 1);
     }

   }

}
