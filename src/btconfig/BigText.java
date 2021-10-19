package btconfig;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class BigText extends JLabel {

    private int SIZE = 128;
    private BufferedImage image;
    private Color fg_color;
    Font font;

    private float dwidth=1.0f;

    public BigText(String string, int size, Color color) {
        super(string);
        SIZE = size;
        font = new Font(Font.SERIF, Font.PLAIN, SIZE);

        image = createImage(super.getText());
        fg_color = color;
    }

    public void setColor( Color c) {
      fg_color = c;
    }

    public void setText(String text) {
        super.setText(text);
        image = createImage(super.getText());
        repaint();
    }

    public void setFontSize(int size) {
      SIZE = size;
    }

    public void setFont(String name, int style, int size) {
      font = new Font(name, style, size);
    }

    public void setDWidth(float dw) {
      dwidth = dw;
    }

    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth() / 2, image.getHeight() / 2);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, (int) ((float) getWidth()*dwidth), getHeight(), null);
    }

    private BufferedImage createImage(String label) {
        if( label.length()==0 ) return null;

        if(font==null) font = new Font(Font.SERIF, Font.PLAIN, SIZE);

        FontRenderContext frc = new FontRenderContext(null, true, true);
        TextLayout layout = new TextLayout(label, font, frc);
        Rectangle r = layout.getPixelBounds(null, 0, 0);
        //System.out.println(r);
        BufferedImage bi = new BufferedImage(
            r.width + 128, r.height + 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) bi.getGraphics();
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        //g2d.setColor(getBackground());
        g2d.setColor( java.awt.Color.black );

        g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());

        //g2d.setColor(getForeground());
        g2d.setColor( fg_color );

        layout.draw(g2d, 0, -r.y);
        g2d.dispose();
        return bi;
    }

}
