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

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

public class rssimeter extends javax.swing.JPanel
{

    int value;
    int rssi;
    boolean show_sig=false;


    public rssimeter()
    {
        setPreferredSize(new Dimension(190,15));
        initComponents();
        setValue(-120,false);
    }

    public int getValue() {
      return rssi;
    }

    public void setValue(int val, boolean show)
    {
        show_sig = show;
        rssi = val;
        value = (int) (((double) val * 2.4) +293.0);
        repaint();
    }

    private void initComponents()
    {

        setLayout(new java.awt.BorderLayout());

        setBackground(new java.awt.Color(0, 0, 0));
        setForeground(new java.awt.Color(0, 0, 255));

        int list_screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
        int list_fontSize = (int)Math.round(8.0 * list_screenRes / 72.0);
        Font list_font = new Font("Arial", Font.PLAIN, 12);

        setFont( list_font );
    }


    private void formComponentResized (java.awt.event.ComponentEvent evt)
    {
        repaint();
    }


    public void paintComponent(Graphics g)
    {

        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                             java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        //fill with black background
        g2d.setColor(new java.awt.Color(0,0,0));
        g2d.fillRect(0,0,this.getWidth(),this.getHeight());

        int x=5;
        int y=5;
        int width=20;
        int height=5;
        int step = x + width;

        g2d.setColor(new java.awt.Color(255,0,0));
        g2d.fillRect(x,y,width,height);

        if(show_sig) {
          g2d.setColor(new java.awt.Color(252,138,0));  //yellow/orange
        }
        else {
          g2d.setColor(java.awt.Color.blue);
        }
        g2d.fillRect(x+step,y,width,height);

        g2d.setColor(new java.awt.Color(31,234,9));
        g2d.fillRect(x+step*2,y,width,height);

        //g2d.setColor(new java.awt.Color(31,234,9));
        g2d.fillRect(x+step*3,y,width,height);

        //g2d.setColor(new java.awt.Color(31,234,9));
        g2d.fillRect(x+step*4,y,width,height);

        //red
        //offcolor = new java.awt.Color(10,10,10);
        //oncolor_low = new java.awt.Color(75,0,0);
        //oncolor_hi = new java.awt.Color(255,0,0);
        //amber
        //offcolor = new java.awt.Color(13,13,13);
        //oncolor_low = new java.awt.Color(145,79,0);
        //oncolor_hi = new java.awt.Color(252,138,0);
        //green
        //offcolor = new java.awt.Color(15,17,15);
        //oncolor_low = new java.awt.Color(24,188,7);
        //oncolor_hi = new java.awt.Color(31,234,9);

        x = value;
        g2d.setColor(new java.awt.Color(0,0,0));
        g2d.fillRect(x,0,130-x,15);

        g2d.setColor(new java.awt.Color(255,255,255));
        if(show_sig) {
          g2d.drawString( String.format("%d dBm",rssi), 130, 12);
          //if(rssi>-120) g2d.drawString( String.format("%d dBm",rssi), 130, 12);
          //else g2d.drawString( String.format("No Sig"), 130, 12);
        }

    }

}
