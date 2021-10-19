/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package btconfig;

import javax.swing.JColorChooser;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.Color;
import java.awt.Font;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;

import java.io.*;


public class displayframe_edit extends javax.swing.JFrame {

  JFontChooser jfc;
  JFileChooser chooser;


  helpFrame hf;
  Boolean did_init=false;
  public colorSelect cs;
  public BTFrame parent;
  public Color col1;
  public Color col2;
  public Color col3;
  public Color col4;
  public Color col5;
  BigText bt1;
  BigText bt2;
  BigText bt3;
  BigText bt4;
  BigText bt5;

  int fsz1=288;
  int fsz2=144;
  int fsz3=72;
  int fsz4=72;
  int fsz5=72;

  int fstyle1 = Font.PLAIN;
  int fstyle2 = Font.PLAIN;
  int fstyle3 = Font.PLAIN;
  int fstyle4 = Font.PLAIN;
  int fstyle5 = Font.PLAIN;

  String fname1="SansSerif";
  String fname2="SansSerif";
  String fname3="SansSerif";
  String fname4="SansSerif";
  String fname5="SansSerif";

  String ts1 ="";
  String ts2 ="";
  String ts3 ="";
  String ts4 ="";
  String ts5 ="";

  float dw=1.0f;

    /**
     * Creates new form displayframe_edit
     */
    public displayframe_edit(BTFrame p, BigText b1, BigText b2, BigText b3, BigText b4, BigText b5) {
      initComponents();
      parent = p;

      hf = new helpFrame();

      cs = new colorSelect();

      jfc = new JFontChooser();
      jfc.setSize(1024,768);

      bt1 = b1;
      bt2 = b2;
      bt3 = b3;
      bt4 = b4;
      bt5 = b5;

      col1 = new Color(128,0,128);
      col2 = Color.white; 
      col3 = Color.red; 
      col4 = Color.cyan; 
      col5 = Color.yellow; 

      chooser = new JFileChooser();

      setSize(1215,550);

    }

  ////////////////////////////////////////
  ////////////////////////////////////////
  String do_subs(String s1) {
    String s2 = s1; 

    try {


      String f = parent.freqval;


      if(f!=null) {
        f = f.trim();
        //System.out.println("freqval: "+f);
        f = f.replace(","," ");
        s2 = s2.replaceAll(Matcher.quoteReplacement("$FREQ$"), f );
        s2 = s2.replaceAll(Matcher.quoteReplacement("$V_FREQ$"), f );
        if(s2==null) s2 = s1;
      }
      else {
        s2 = s2.replaceAll(Matcher.quoteReplacement("$FREQ$"), " " );
        s2 = s2.replaceAll(Matcher.quoteReplacement("$V_FREQ$"), " " );
        if(s2==null) s2 = s1;
      }

      if(parent.talkgroup_name!=null && f!=null) {
        s2 = s2.replaceAll(Matcher.quoteReplacement("$TG_NAME$"), parent.talkgroup_name.trim() );
        if(s2==null) s2 = s1;
      }
      else {
        s2 = s2.replaceAll(Matcher.quoteReplacement("$TG_NAME$"), " " );
        if(s2==null) s2 = s1;
      }

      if(parent.current_talkgroup!=null && f!=null) {
        s2 = s2.replaceAll(Matcher.quoteReplacement("$TG_ID$"), parent.current_talkgroup.trim() );
        if(s2==null) s2 = s1;
      }
      else {
        s2 = s2.replaceAll(Matcher.quoteReplacement("$TG_ID$"), " " );
        if(s2==null) s2 = s1;
      }

      if(parent.rssi!=null) {
        String rssi_d = String.format( "%-3d", Integer.valueOf(parent.rssi) );
        if(rssi_d.length()==3) rssi_d = rssi_d+" ";
        s2 = s2.replaceAll(Matcher.quoteReplacement("$RSSI$"), rssi_d);
        if(s2==null) s2 = s1;
      }


      String wacn = "";
      String sysid = "";
      String nac = ""; 
      String siteid = ""; 
      String cc_freq = ""; 
      String rfid = ""; 
      String rid = "";
      String rid_alias = "";
      String evm_p = "";



      if(parent.wacn.getText()!=null) wacn = parent.wacn.getText().trim();
      if(parent.sysid.getText()!=null) sysid = parent.sysid.getText().trim();
      if(parent.nac.getText()!=null) nac = parent.nac.getText().trim();
      if(parent.siteid.getText()!=null) siteid = parent.siteid.getText().trim();
      if(parent.rfid.getText()!=null) rfid = parent.rfid.getText().trim();
      if(parent.freq.getText()!=null) cc_freq = parent.freq.getText().trim();

      rid = parent.src_uid_str;
      rid_alias = parent.current_alias;

      if(rid==null) rid="";
      if(rid_alias==null) rid_alias="";

      if( wacn.contains("WACN:") && wacn.length()>5) wacn = wacn.substring(5,wacn.length());
      if( sysid.contains("SYS_ID:") && sysid.length()>7) sysid = sysid.substring(7,sysid.length());
      if( nac.contains("NAC:") && nac.length()>4) nac = nac.substring(4,nac.length());
      if( rfid.contains("RFSS ID:") && rfid.length()>8) rfid = rfid.substring(8,rfid.length());
      if( siteid.contains("SITE ID:") && siteid.length()>8) siteid = siteid.substring(8,siteid.length());
      if( cc_freq.contains("Freq:") && cc_freq.length()>5) cc_freq = cc_freq.substring(5,cc_freq.length());

      s2 = s2.replaceAll(Matcher.quoteReplacement("$WACN$"), wacn );
        if(s2==null) s2 = s1;
      s2 = s2.replaceAll(Matcher.quoteReplacement("$SYS_ID$"), sysid );
        if(s2==null) s2 = s1;
      s2 = s2.replaceAll(Matcher.quoteReplacement("$NAC$"), nac );
        if(s2==null) s2 = s1;
      s2 = s2.replaceAll(Matcher.quoteReplacement("$SITE_ID$"), siteid );
        if(s2==null) s2 = s1;
      s2 = s2.replaceAll(Matcher.quoteReplacement("$RFSS_ID$"), rfid );
        if(s2==null) s2 = s1;
      s2 = s2.replaceAll(Matcher.quoteReplacement("$CC_FREQ$"), cc_freq );
        if(s2==null) s2 = s1;

      s2 = s2.replaceAll(Matcher.quoteReplacement("$RID$"), rid );
        if(s2==null) s2 = s1;
      s2 = s2.replaceAll(Matcher.quoteReplacement("$RID_ALIAS$"), rid_alias );
        if(s2==null) s2 = s1;

      if(parent.is_phase1==1) {
      s2 = s2.replaceAll(Matcher.quoteReplacement("$P25_MODE$"), "P25-P1" );
        if(s2==null) s2 = s1;
      }
      if(parent.is_phase2==1) {
      s2 = s2.replaceAll(Matcher.quoteReplacement("$P25_MODE$"), "P25-P2" );
        if(s2==null) s2 = s1;
      }

      try {
        //evm_p = String.format("%3.0f", parent.current_evm_percent);
      } catch(Exception e) {
      }

      s2 = s2.replaceAll(Matcher.quoteReplacement("$EVM_P$"), evm_p );
        if(s2==null) s2 = s1;

      String sysname = parent.system_alias.getText();
      if(sysname==null || sysname.length()==0) sysname="SYS_NAME";
      s2 = s2.replaceAll(Matcher.quoteReplacement("$SYS_NAME$"), sysname );
        if(s2==null) s2 = s1;
    } catch(Exception e) {
      e.printStackTrace();
    }

    if(s2==null) s2=s1;

    return s2;
  }
  ////////////////////////////////////////
  ////////////////////////////////////////
    public void update_colors() {

      if( parent.prefs!=null ) {
        col1 = new Color( parent.prefs.getInt("dfcol1", new Color(128,0,128).getRGB() ) );
        col2 = new Color( parent.prefs.getInt("dfcol2", Color.white.getRGB() ));
        col3 = new Color( parent.prefs.getInt("dfcol3", Color.red.getRGB() ));
        col4 = new Color( parent.prefs.getInt("dfcol4", Color.cyan.getRGB() ));
        col5 = new Color( parent.prefs.getInt("dfcol5", Color.yellow.getRGB() ));

        Boolean b = parent.prefs.getBoolean("dfen1",true); 
        if(!b) col1 = Color.black;
        b = parent.prefs.getBoolean("dfen2",true); 
        if(!b) col2 = Color.black;
        b = parent.prefs.getBoolean("dfen3",true); 
        if(!b) col3 = Color.black;
        b = parent.prefs.getBoolean("dfen4",true); 
        if(!b) col4 = Color.black;
        b = parent.prefs.getBoolean("dfen5",true); 
        if(!b) col5 = Color.black;


        fname1 = parent.prefs.get("df_font1", "Serif"); 
        fname2 = parent.prefs.get("df_font2", "Serif"); 
        fname3 = parent.prefs.get("df_font3", "Serif"); 
        fname4 = parent.prefs.get("df_font4", "Serif"); 
        fname5 = parent.prefs.get("df_font5", "Serif"); 

        fsz1 = parent.prefs.getInt("df_font_size1", 288);
        fsz2 = parent.prefs.getInt("df_font_size2", 144);
        fsz3 = parent.prefs.getInt("df_font_size3", 72);
        fsz4 = parent.prefs.getInt("df_font_size4", 72);
        fsz5 = parent.prefs.getInt("df_font_size5", 72);

        fstyle1 = parent.prefs.getInt("df_font_style1", Font.PLAIN);
        fstyle2 = parent.prefs.getInt("df_font_style2", Font.PLAIN);
        fstyle3 = parent.prefs.getInt("df_font_style3", Font.PLAIN);
        fstyle4 = parent.prefs.getInt("df_font_style4", Font.PLAIN);
        fstyle5 = parent.prefs.getInt("df_font_style5", Font.PLAIN);

        ts1 = parent.prefs.get("dftok1", "$TG_NAME$");
        ts2 = parent.prefs.get("dftok2", "TGID $TG_ID$");
        ts3 = parent.prefs.get("dftok3", "$FREQ$");
        ts4 = parent.prefs.get("dftok4", "$SYS_NAME$");
        ts5 = parent.prefs.get("dftok5", "WACN: $WACN$  SYS_ID: $SYS_ID$  NAC: $NAC$");

        clrnv1.setSelected( parent.prefs.getBoolean("clrnv1", false) );
        clrnv2.setSelected( parent.prefs.getBoolean("clrnv2", false) );
        clrnv3.setSelected( parent.prefs.getBoolean("clrnv3", false) );
        clrnv4.setSelected( parent.prefs.getBoolean("clrnv4", false) );
        clrnv5.setSelected( parent.prefs.getBoolean("clrnv5", false) );

      }

      if( !did_init ) {
        did_init=true;

        tok1.setText(ts1);
        tok2.setText(ts2);
        tok3.setText(ts3);
        tok4.setText(ts4);
        tok5.setText(ts5);

        dw = parent.prefs.getFloat("dwidth", 1.0f);
        dwidth.setText( String.format("%3.2f", dw) );
      }

      String tts1 = do_subs(ts1);
      String tts2 = do_subs(ts2);
      String tts3 = do_subs(ts3);
      String tts4 = do_subs(ts4);
      String tts5 = do_subs(ts5);

      if(clrnv1.isSelected() && parent.freqval!=null && parent.freqval.length()==0) tts1=" ";
      if(clrnv2.isSelected() && parent.freqval!=null && parent.freqval.length()==0) tts2=" ";
      if(clrnv3.isSelected() && parent.freqval!=null && parent.freqval.length()==0) tts3=" ";
      if(clrnv4.isSelected() && parent.freqval!=null && parent.freqval.length()==0) tts4=" ";
      if(clrnv5.isSelected() && parent.freqval!=null && parent.freqval.length()==0) tts5=" ";

      bt1.setText(tts1);
      bt2.setText(tts2);
      bt3.setText(tts3);
      bt4.setText(tts4);
      bt5.setText(tts5);

      dw = Float.valueOf( dwidth.getText() );
      bt1.setDWidth(dw);
      bt2.setDWidth(dw);
      bt3.setDWidth(dw);
      bt4.setDWidth(dw);
      bt5.setDWidth(dw);

      bt1.setFont( fname1, fstyle1, fsz1);
      bt2.setFont( fname2, fstyle2, fsz2);
      bt3.setFont( fname3, fstyle3, fsz3);
      bt4.setFont( fname4, fstyle4, fsz4);
      bt5.setFont( fname5, fstyle5, fsz5);

      bt1.setColor(col1);
      bt2.setColor(col2);
      bt3.setColor(col3);
      bt4.setColor(col4);
      bt5.setColor(col5);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        tok1 = new javax.swing.JTextField();
        selfont1 = new javax.swing.JButton();
        dvcol1 = new javax.swing.JButton();
        en1 = new javax.swing.JCheckBox();
        clrnv1 = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        tok2 = new javax.swing.JTextField();
        selfont2 = new javax.swing.JButton();
        dvcol2 = new javax.swing.JButton();
        en2 = new javax.swing.JCheckBox();
        clrnv2 = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        tok3 = new javax.swing.JTextField();
        selfont3 = new javax.swing.JButton();
        dvcol3 = new javax.swing.JButton();
        en3 = new javax.swing.JCheckBox();
        clrnv3 = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        tok4 = new javax.swing.JTextField();
        selfont4 = new javax.swing.JButton();
        dvcol4 = new javax.swing.JButton();
        en4 = new javax.swing.JCheckBox();
        clrnv4 = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        tok5 = new javax.swing.JTextField();
        selfont5 = new javax.swing.JButton();
        dvcol5 = new javax.swing.JButton();
        en5 = new javax.swing.JCheckBox();
        clrnv5 = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        dwidth = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        close = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        export_df = new javax.swing.JButton();
        import_df = new javax.swing.JButton();
        showkeyw = new javax.swing.JButton();
        saveconfig = new javax.swing.JButton();

        jPanel1.setLayout(new java.awt.GridLayout(6, 1));

        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        tok1.setColumns(60);
        tok1.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        tok1.setText("$TG_NAME$");
        tok1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                fs4KeyTyped(evt);
            }
        });
        jPanel2.add(tok1);

        selfont1.setText("Select Font");
        selfont1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selfont1ActionPerformed(evt);
            }
        });
        jPanel2.add(selfont1);

        dvcol1.setText("Edit Color");
        dvcol1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dvcol1ActionPerformed(evt);
            }
        });
        jPanel2.add(dvcol1);

        en1.setSelected(true);
        en1.setText("Enable");
        en1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en1ActionPerformed(evt);
            }
        });
        jPanel2.add(en1);

        clrnv1.setText("Clear On No Voice");
        clrnv1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clrnv1ActionPerformed(evt);
            }
        });
        jPanel2.add(clrnv1);

        jPanel1.add(jPanel2);

        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        tok2.setColumns(60);
        tok2.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        tok2.setText("$TG_ID$");
        tok2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                fs4KeyTyped(evt);
            }
        });
        jPanel3.add(tok2);

        selfont2.setText("Select Font");
        selfont2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selfont2ActionPerformed(evt);
            }
        });
        jPanel3.add(selfont2);

        dvcol2.setText("Edit Color");
        dvcol2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dvcol2ActionPerformed(evt);
            }
        });
        jPanel3.add(dvcol2);

        en2.setSelected(true);
        en2.setText("Enable");
        en2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en2ActionPerformed(evt);
            }
        });
        jPanel3.add(en2);

        clrnv2.setText("Clear On No Voice");
        clrnv2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clrnv2ActionPerformed(evt);
            }
        });
        jPanel3.add(clrnv2);

        jPanel1.add(jPanel3);

        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        tok3.setColumns(60);
        tok3.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        tok3.setText("$FREQ$");
        tok3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                fs4KeyTyped(evt);
            }
        });
        jPanel4.add(tok3);

        selfont3.setText("Select Font");
        selfont3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selfont3ActionPerformed(evt);
            }
        });
        jPanel4.add(selfont3);

        dvcol3.setText("Edit Color");
        dvcol3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dvcol3ActionPerformed(evt);
            }
        });
        jPanel4.add(dvcol3);

        en3.setSelected(true);
        en3.setText("Enable");
        en3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en3ActionPerformed(evt);
            }
        });
        jPanel4.add(en3);

        clrnv3.setText("Clear On No Voice");
        clrnv3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clrnv3ActionPerformed(evt);
            }
        });
        jPanel4.add(clrnv3);

        jPanel1.add(jPanel4);

        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        tok4.setColumns(60);
        tok4.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        tok4.setText("$SYS_NAME$");
        tok4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                fs4KeyTyped(evt);
            }
        });
        jPanel5.add(tok4);

        selfont4.setText("Select Font");
        selfont4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selfont4ActionPerformed(evt);
            }
        });
        jPanel5.add(selfont4);

        dvcol4.setText("Edit Color");
        dvcol4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dvcol4ActionPerformed(evt);
            }
        });
        jPanel5.add(dvcol4);

        en4.setSelected(true);
        en4.setText("Enable");
        en4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en4ActionPerformed(evt);
            }
        });
        jPanel5.add(en4);

        clrnv4.setText("Clear On No Voice");
        clrnv4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clrnv4ActionPerformed(evt);
            }
        });
        jPanel5.add(clrnv4);

        jPanel1.add(jPanel5);

        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        tok5.setColumns(60);
        tok5.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        tok5.setText("$WACN$ $SYS_ID$ $NAC$");
        tok5.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                fs4KeyTyped(evt);
            }
        });
        jPanel6.add(tok5);

        selfont5.setText("Select Font");
        selfont5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selfont5ActionPerformed(evt);
            }
        });
        jPanel6.add(selfont5);

        dvcol5.setText("Edit Color");
        dvcol5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dvcol5ActionPerformed(evt);
            }
        });
        jPanel6.add(dvcol5);

        en5.setSelected(true);
        en5.setText("Enable");
        en5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                en5ActionPerformed(evt);
            }
        });
        jPanel6.add(en5);

        clrnv5.setText("Clear On No Voice");
        clrnv5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clrnv5ActionPerformed(evt);
            }
        });
        jPanel6.add(clrnv5);

        jPanel1.add(jPanel6);

        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel6.setText("Display Width Relative To Window");
        jPanel8.add(jLabel6);

        dwidth.setColumns(4);
        dwidth.setText("1.0");
        dwidth.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                dwidthKeyReleased(evt);
            }
        });
        jPanel8.add(dwidth);

        jSeparator1.setPreferredSize(new java.awt.Dimension(150, 0));
        jPanel8.add(jSeparator1);

        close.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        close.setText("Close");
        close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeActionPerformed(evt);
            }
        });
        jPanel8.add(close);

        jPanel1.add(jPanel8);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel7.setLayout(new java.awt.GridLayout(10, 1, 25, 25));

        export_df.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        export_df.setText("Export");
        export_df.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                export_dfActionPerformed(evt);
            }
        });
        jPanel7.add(export_df);

        import_df.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        import_df.setText("Import");
        import_df.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                import_dfActionPerformed(evt);
            }
        });
        jPanel7.add(import_df);

        showkeyw.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        showkeyw.setText("Show Key Words");
        showkeyw.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showkeywActionPerformed(evt);
            }
        });
        jPanel7.add(showkeyw);

        saveconfig.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        saveconfig.setText("Save Config");
        saveconfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveconfigActionPerformed(evt);
            }
        });
        jPanel7.add(saveconfig);

        getContentPane().add(jPanel7, java.awt.BorderLayout.EAST);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void export_dfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export_dfActionPerformed
      try {

        FileNameExtensionFilter filter = new FileNameExtensionFilter( "displayview file", "dvp");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showDialog(parent, "Export Display View Profile");

        ObjectOutputStream oos;

        if(returnVal == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          oos = new ObjectOutputStream( new FileOutputStream(file) );

          oos.writeInt((int) 100); //version

          oos.writeInt(col1.getRGB());
          oos.writeInt(col2.getRGB());
          oos.writeInt(col3.getRGB());
          oos.writeInt(col4.getRGB());
          oos.writeInt(col5.getRGB());


          Boolean b = parent.prefs.getBoolean("dfen1",true); 
          oos.writeBoolean(b);
          b = parent.prefs.getBoolean("dfen2",true); 
          oos.writeBoolean(b);
          b = parent.prefs.getBoolean("dfen3",true); 
          oos.writeBoolean(b);
          b = parent.prefs.getBoolean("dfen4",true); 
          oos.writeBoolean(b);
          b = parent.prefs.getBoolean("dfen5",true); 
          oos.writeBoolean(b);

          oos.writeUTF(fname1);
          oos.writeUTF(fname2);
          oos.writeUTF(fname3);
          oos.writeUTF(fname4);
          oos.writeUTF(fname5);

          oos.writeInt(fsz1);
          oos.writeInt(fsz2);
          oos.writeInt(fsz3);
          oos.writeInt(fsz4);
          oos.writeInt(fsz5);

          oos.writeInt(fstyle1);
          oos.writeInt(fstyle2);
          oos.writeInt(fstyle3);
          oos.writeInt(fstyle4);
          oos.writeInt(fstyle5);

          oos.writeUTF(ts1);
          oos.writeUTF(ts2);
          oos.writeUTF(ts3);
          oos.writeUTF(ts4);
          oos.writeUTF(ts5);

          b = parent.prefs.getBoolean("clrnv1", false);
          oos.writeBoolean(b);
          b = parent.prefs.getBoolean("clrnv2", false);
          oos.writeBoolean(b);
          b = parent.prefs.getBoolean("clrnv3", false);
          oos.writeBoolean(b);
          b = parent.prefs.getBoolean("clrnv4", false);
          oos.writeBoolean(b);
          b = parent.prefs.getBoolean("clrnv5", false);
          oos.writeBoolean(b);

          oos.writeFloat(dw);

          oos.flush();
          oos.close();

        }

      } catch(Exception e) {
        e.printStackTrace();
      }

    }//GEN-LAST:event_export_dfActionPerformed

    private void dvcol1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dvcol1ActionPerformed
      //cs.setVisible(true);
      Color color = JColorChooser.showDialog(parent, "Color1", col1); 
      if(color!=null) col1=color;
      if( parent.prefs!=null && color!=null) {
        parent.prefs.putInt("dfcol1",  color.getRGB() );
      }
      update_colors();
    }//GEN-LAST:event_dvcol1ActionPerformed

    private void dvcol2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dvcol2ActionPerformed
      //cs.setVisible(true);
      Color color = JColorChooser.showDialog(parent, "Color1", col2); 
      if(color!=null) col2=color;
      if( parent.prefs!=null && color!=null) {
        parent.prefs.putInt("dfcol2",  color.getRGB() );
      }
      update_colors();
    }//GEN-LAST:event_dvcol2ActionPerformed

    private void dvcol3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dvcol3ActionPerformed
      //cs.setVisible(true);
      Color color = JColorChooser.showDialog(parent, "Color1", col3); 
      if(color!=null) col3=color;
      if( parent.prefs!=null && color!=null) {
        parent.prefs.putInt("dfcol3",  color.getRGB() );
      }
      update_colors();
    }//GEN-LAST:event_dvcol3ActionPerformed

    private void dvcol4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dvcol4ActionPerformed
      //cs.setVisible(true);
      Color color = JColorChooser.showDialog(parent, "Color1", col4); 
      if(color!=null) col4=color;
      if( parent.prefs!=null && color!=null) {
        parent.prefs.putInt("dfcol4",  color.getRGB() );
      }
      update_colors();
    }//GEN-LAST:event_dvcol4ActionPerformed

    private void dvcol5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dvcol5ActionPerformed
      //cs.setVisible(true);
      Color color = JColorChooser.showDialog(parent, "Color1", col5); 
      if(color!=null) col5=color;
      if( parent.prefs!=null && color!=null) {
        parent.prefs.putInt("dfcol5",  color.getRGB() );
      }
      update_colors();
    }//GEN-LAST:event_dvcol5ActionPerformed

    private void closeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeActionPerformed
      setVisible(false);
    }//GEN-LAST:event_closeActionPerformed

      //col1 = new Color(128,0,128);
      //col2 = Color.white; 
      //col3 = Color.red; 
      //col4 = Color.cyan; 
      //col5 = Color.yellow; 
    private void en1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_en1ActionPerformed
        // TODO add your handling code here:
      if( parent.prefs!=null) {
        parent.prefs.putBoolean("dfen1",  en1.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_en1ActionPerformed

    private void en2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_en2ActionPerformed
        // TODO add your handling code here:
      if( parent.prefs!=null) {
        parent.prefs.putBoolean("dfen2",  en2.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_en2ActionPerformed

    private void en3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_en3ActionPerformed
        // TODO add your handling code here:
      if( parent.prefs!=null) {
        parent.prefs.putBoolean("dfen3",  en3.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_en3ActionPerformed

    private void en4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_en4ActionPerformed
        // TODO add your handling code here:
      if( parent.prefs!=null) {
        parent.prefs.putBoolean("dfen4",  en4.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_en4ActionPerformed

    private void en5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_en5ActionPerformed
        // TODO add your handling code here:
      if( parent.prefs!=null) {
        parent.prefs.putBoolean("dfen5",  en5.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_en5ActionPerformed

    private void saveconfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveconfigActionPerformed
      if(parent.prefs!=null) {

        parent.prefs.put("dftok1", tok1.getText() );
        parent.prefs.put("dftok2", tok2.getText() );
        parent.prefs.put("dftok3", tok3.getText() );
        parent.prefs.put("dftok4", tok4.getText() );
        parent.prefs.put("dftok5", tok5.getText() );

        parent.prefs.putFloat("dwidth", Float.valueOf( dwidth.getText()).floatValue() );

      }

      update_colors();
      saveconfig.setEnabled(false);
    }//GEN-LAST:event_saveconfigActionPerformed

    private void fs4KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_fs4KeyTyped
      //System.out.println("evt:"+evt);
      saveconfig.setEnabled(true);
    }//GEN-LAST:event_fs4KeyTyped

    private void clrnv1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clrnv1ActionPerformed
      if(parent.prefs!=null) {
        parent.prefs.putBoolean("clrnv1", clrnv1.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_clrnv1ActionPerformed

    private void clrnv2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clrnv2ActionPerformed
      if(parent.prefs!=null) {
        parent.prefs.putBoolean("clrnv2", clrnv2.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_clrnv2ActionPerformed

    private void clrnv3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clrnv3ActionPerformed
      if(parent.prefs!=null) {
        parent.prefs.putBoolean("clrnv3", clrnv3.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_clrnv3ActionPerformed

    private void clrnv4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clrnv4ActionPerformed
      if(parent.prefs!=null) {
        parent.prefs.putBoolean("clrnv4", clrnv4.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_clrnv4ActionPerformed

    private void clrnv5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clrnv5ActionPerformed
      if(parent.prefs!=null) {
        parent.prefs.putBoolean("clrnv5", clrnv5.isSelected() );
      }
      update_colors();
    }//GEN-LAST:event_clrnv5ActionPerformed

    private void dwidthKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dwidthKeyReleased
      saveconfig.setEnabled(true);
    }//GEN-LAST:event_dwidthKeyReleased

    private void showkeywActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showkeywActionPerformed
      String kw="";

      kw = kw.concat("\n$CC_FREQ$");
      kw = kw.concat("\n$EVM_P$");
      kw = kw.concat("\n$FREQ$");
      kw = kw.concat("\n$NAC$");
      kw = kw.concat("\n$P25_MODE$");
      kw = kw.concat("\n$RFSS_ID$");
      kw = kw.concat("\n$RID$");
      kw = kw.concat("\n$RID_ALIAS$");
      kw = kw.concat("\n$RSSI$");
      kw = kw.concat("\n$SITE_ID$");
      kw = kw.concat("\n$SYS_ID$");
      kw = kw.concat("\n$SYS_NAME$");
      kw = kw.concat("\n$TG_ID$");
      kw = kw.concat("\n$TG_NAME$");
      kw = kw.concat("\n$V_FREQ$");
      kw = kw.concat("\n$WACN$");
      hf.setText(kw);

      hf.setVisible(true);
    }//GEN-LAST:event_showkeywActionPerformed

    private void import_dfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_dfActionPerformed
      try {

        FileNameExtensionFilter filter = new FileNameExtensionFilter( "displayview file", "dvp");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showDialog(parent, "Import Display View Profile");

        ObjectInputStream ois;

        if(returnVal == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          ois = new ObjectInputStream( new FileInputStream(file) );

          int version = ois.readInt(); //version

          if(version==100 && parent.prefs!=null) {

            parent.prefs.putInt( "dfcol1", ois.readInt() );
            parent.prefs.putInt( "dfcol2", ois.readInt() );
            parent.prefs.putInt( "dfcol3", ois.readInt() );
            parent.prefs.putInt( "dfcol4", ois.readInt() );
            parent.prefs.putInt( "dfcol5", ois.readInt() );

            parent.prefs.putBoolean( "dfen1", ois.readBoolean() );
            parent.prefs.putBoolean( "dfen2", ois.readBoolean() );
            parent.prefs.putBoolean( "dfen3", ois.readBoolean() );
            parent.prefs.putBoolean( "dfen4", ois.readBoolean() );
            parent.prefs.putBoolean( "dfen5", ois.readBoolean() );

            parent.prefs.put("df_font1", ois.readUTF() );
            parent.prefs.put("df_font2", ois.readUTF() );
            parent.prefs.put("df_font3", ois.readUTF() );
            parent.prefs.put("df_font4", ois.readUTF() );
            parent.prefs.put("df_font5", ois.readUTF() );

            parent.prefs.putInt("df_font_size1", ois.readInt() );
            parent.prefs.putInt("df_font_size2", ois.readInt() );
            parent.prefs.putInt("df_font_size3", ois.readInt() );
            parent.prefs.putInt("df_font_size4", ois.readInt() );
            parent.prefs.putInt("df_font_size5", ois.readInt() );

            parent.prefs.putInt("df_font_style1", ois.readInt() );
            parent.prefs.putInt("df_font_style2", ois.readInt() );
            parent.prefs.putInt("df_font_style3", ois.readInt() );
            parent.prefs.putInt("df_font_style4", ois.readInt() );
            parent.prefs.putInt("df_font_style5", ois.readInt() );

            parent.prefs.put("dftok1", ois.readUTF() );
            parent.prefs.put("dftok2", ois.readUTF() );
            parent.prefs.put("dftok3", ois.readUTF() );
            parent.prefs.put("dftok4", ois.readUTF() );
            parent.prefs.put("dftok5", ois.readUTF() );

            parent.prefs.putBoolean("clrnv1", ois.readBoolean() );
            parent.prefs.putBoolean("clrnv2", ois.readBoolean() );
            parent.prefs.putBoolean("clrnv3", ois.readBoolean() );
            parent.prefs.putBoolean("clrnv4", ois.readBoolean() );
            parent.prefs.putBoolean("clrnv5", ois.readBoolean() );

            parent.prefs.putFloat("dwidth", ois.readFloat() );

            ois.close();

            did_init=false;
          }
          else {
            System.out.println("import display file: wrong version");
          }

        }

      } catch(Exception e) {
        e.printStackTrace();
      }
    }//GEN-LAST:event_import_dfActionPerformed

    private void selfont1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selfont1ActionPerformed

      jfc.setSelectedFontFamily(fname1);
      jfc.setSelectedFontSize(fsz1);
      jfc.setSelectedFontStyle(fstyle1);


      int result = jfc.showDialog(this);
      if( result == JFontChooser.OK_OPTION ) {
        bt1.setFont( jfc.getSelectedFontFamily(), jfc.getSelectedFontStyle(), jfc.getSelectedFontSize() );
      }
      if(parent.prefs!=null) {
        parent.prefs.put("df_font1", jfc.getSelectedFontFamily() );
        parent.prefs.putInt("df_font_style1", jfc.getSelectedFontStyle() );
        parent.prefs.putInt("df_font_size1", jfc.getSelectedFontSize() );
      }
      update_colors();
    }//GEN-LAST:event_selfont1ActionPerformed

    private void selfont2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selfont2ActionPerformed
      jfc.setSelectedFontFamily(fname2);
      jfc.setSelectedFontSize(fsz2);
      jfc.setSelectedFontStyle(fstyle2);

      int result = jfc.showDialog(this);
      if( result == JFontChooser.OK_OPTION ) {
        bt2.setFont( jfc.getSelectedFontFamily(), jfc.getSelectedFontStyle(), jfc.getSelectedFontSize() );
      }
      if(parent.prefs!=null) {
        parent.prefs.put("df_font2", jfc.getSelectedFontFamily() );
        parent.prefs.putInt("df_font_style2", jfc.getSelectedFontStyle() );
        parent.prefs.putInt("df_font_size2", jfc.getSelectedFontSize() );
      }
      update_colors();
    }//GEN-LAST:event_selfont2ActionPerformed

    private void selfont3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selfont3ActionPerformed
      jfc.setSelectedFontFamily(fname3);
      jfc.setSelectedFontSize(fsz3);
      jfc.setSelectedFontStyle(fstyle3);

      int result = jfc.showDialog(this);
      if( result == JFontChooser.OK_OPTION ) {
        bt3.setFont( jfc.getSelectedFontFamily(), jfc.getSelectedFontStyle(), jfc.getSelectedFontSize() );
      }
      if(parent.prefs!=null) {
        parent.prefs.put("df_font3", jfc.getSelectedFontFamily() );
        parent.prefs.putInt("df_font_style3", jfc.getSelectedFontStyle() );
        parent.prefs.putInt("df_font_size3", jfc.getSelectedFontSize() );
      }
      update_colors();
    }//GEN-LAST:event_selfont3ActionPerformed

    private void selfont4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selfont4ActionPerformed
      jfc.setSelectedFontFamily(fname4);
      jfc.setSelectedFontSize(fsz4);
      jfc.setSelectedFontStyle(fstyle4);

      int result = jfc.showDialog(this);
      if( result == JFontChooser.OK_OPTION ) {
        bt4.setFont( jfc.getSelectedFontFamily(), jfc.getSelectedFontStyle(), jfc.getSelectedFontSize() );
      }
      if(parent.prefs!=null) {
        parent.prefs.put("df_font4", jfc.getSelectedFontFamily() );
        parent.prefs.putInt("df_font_style4", jfc.getSelectedFontStyle() );
        parent.prefs.putInt("df_font_size4", jfc.getSelectedFontSize() );
      }
      update_colors();
    }//GEN-LAST:event_selfont4ActionPerformed

    private void selfont5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selfont5ActionPerformed
      jfc.setSelectedFontFamily(fname5);
      jfc.setSelectedFontSize(fsz5);
      jfc.setSelectedFontStyle(fstyle5);

      int result = jfc.showDialog(this);
      if( result == JFontChooser.OK_OPTION ) {
        bt5.setFont( jfc.getSelectedFontFamily(), jfc.getSelectedFontStyle(), jfc.getSelectedFontSize() );
      }
      if(parent.prefs!=null) {
        parent.prefs.put("df_font5", jfc.getSelectedFontFamily() );
        parent.prefs.putInt("df_font_style5", jfc.getSelectedFontStyle() );
        parent.prefs.putInt("df_font_size5", jfc.getSelectedFontSize() );
      }
      update_colors();
    }//GEN-LAST:event_selfont5ActionPerformed

  /*
    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(displayframe_edit.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(displayframe_edit.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(displayframe_edit.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(displayframe_edit.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new displayframe_edit().setVisible(true);
            }
        });
    }

  */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton close;
    private javax.swing.JCheckBox clrnv1;
    private javax.swing.JCheckBox clrnv2;
    private javax.swing.JCheckBox clrnv3;
    private javax.swing.JCheckBox clrnv4;
    private javax.swing.JCheckBox clrnv5;
    private javax.swing.JButton dvcol1;
    private javax.swing.JButton dvcol2;
    private javax.swing.JButton dvcol3;
    private javax.swing.JButton dvcol4;
    private javax.swing.JButton dvcol5;
    private javax.swing.JTextField dwidth;
    public javax.swing.JCheckBox en1;
    public javax.swing.JCheckBox en2;
    public javax.swing.JCheckBox en3;
    public javax.swing.JCheckBox en4;
    public javax.swing.JCheckBox en5;
    private javax.swing.JButton export_df;
    private javax.swing.JButton import_df;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JButton saveconfig;
    private javax.swing.JButton selfont1;
    private javax.swing.JButton selfont2;
    private javax.swing.JButton selfont3;
    private javax.swing.JButton selfont4;
    private javax.swing.JButton selfont5;
    private javax.swing.JButton showkeyw;
    private javax.swing.JTextField tok1;
    private javax.swing.JTextField tok2;
    private javax.swing.JTextField tok3;
    private javax.swing.JTextField tok4;
    private javax.swing.JTextField tok5;
    // End of variables declaration//GEN-END:variables
}
