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
/**
 *
 * @author radioactive
 */
public class tglog_editor extends javax.swing.JFrame {
BTFrame parent;
JFileChooser chooser;
    /**
     * Creates new form tglog_editor
     */
    public tglog_editor(BTFrame p) {
      initComponents();
      parent = p;
      chooser = new JFileChooser();

      //would be better defaults
      //$DATE$ $TIME$, $TG_NAME$, TG_$TG_ID$, RID $RID$, $RID_ALIAS$, RSSI $RSSI$, VFREQ $V_FREQ$, CCFREQ $CC_FREQ$, SYS $WACN$-$SYS_ID$, NAC $NAC$, SITE $SITE_ID$, RFSS $RFSS_ID$, ERR_RATE $ERR_RATE$

      if( parent.prefs!=null) {
        log_format.setText( parent.prefs.get("tglog_format", "$P25_MODE$ $V_FREQ$ MHz,  TG $TG_ID$ ,  $TG_NAME$, $DATE$-$TIME$, $RSSI$ dbm,  cc_freq $CC_FREQ$ mhz, RID $RID$, $P25_MODE$, EVM  $EVM_P$%, ") );
      }
    }

    public String getFormat() {
      return log_format.getText();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        log_format = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        tigger_rid = new javax.swing.JRadioButton();
        trigger_grant = new javax.swing.JRadioButton();
        trigger_voice = new javax.swing.JRadioButton();
        te_import = new javax.swing.JButton();
        te_export = new javax.swing.JButton();
        help = new javax.swing.JButton();
        reset = new javax.swing.JButton();
        close = new javax.swing.JButton();
        save = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();

        setTitle("TG Log Editor");

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        log_format.setColumns(200);
        log_format.setText("$P25_MODE$ $V_FREQ$ MHz,  TG $TG_ID$ ,  $TG_NAME$, $DATE$-$TIME$, $RSSI$ dbm,  cc_freq $CC_FREQ$ mhz, RID $RID$, $P25_MODE$, EVM  $EVM_P$%, ");
        log_format.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                log_formatActionPerformed(evt);
            }
        });
        jPanel1.add(log_format);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Log Trigger"));

        buttonGroup1.add(tigger_rid);
        tigger_rid.setText("Voice Grant");
        tigger_rid.setEnabled(false);
        jPanel4.add(tigger_rid);

        buttonGroup1.add(trigger_grant);
        trigger_grant.setSelected(true);
        trigger_grant.setText("RID Update");
        trigger_grant.setEnabled(false);
        jPanel4.add(trigger_grant);

        trigger_voice.setText("Voice Audio");
        trigger_voice.setEnabled(false);
        jPanel4.add(trigger_voice);

        jPanel2.add(jPanel4);

        te_import.setText("Import");
        te_import.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                te_importActionPerformed(evt);
            }
        });
        jPanel2.add(te_import);

        te_export.setText("Export");
        te_export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                te_exportActionPerformed(evt);
            }
        });
        jPanel2.add(te_export);

        help.setText("Show Keywords");
        help.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpActionPerformed(evt);
            }
        });
        jPanel2.add(help);

        reset.setText("Reset To Defaults");
        reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetActionPerformed(evt);
            }
        });
        jPanel2.add(reset);

        close.setText("Close");
        close.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeActionPerformed(evt);
            }
        });
        jPanel2.add(close);

        save.setText("Save");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });
        jPanel2.add(save);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

        jLabel2.setText("Log File Output Format");
        jPanel3.add(jLabel2);

        getContentPane().add(jPanel3, java.awt.BorderLayout.NORTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeActionPerformed
      setVisible(false);
    }//GEN-LAST:event_closeActionPerformed

    private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed
      if( parent.prefs!=null) {
        parent.prefs.put("tglog_format", getFormat());
      }
    }//GEN-LAST:event_saveActionPerformed

    private void helpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpActionPerformed
      if(parent.dframe!=null) parent.dframe.show_help();
    }//GEN-LAST:event_helpActionPerformed

    private void log_formatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_log_formatActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_log_formatActionPerformed

    private void resetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetActionPerformed
        log_format.setText("$P25_MODE$ $V_FREQ$ MHz,  TG $TG_ID$ ,  $TG_NAME$, $DATE$-$TIME$, $RSSI$ dbm,  cc_freq $CC_FREQ$ mhz, RID $RID$, $P25_MODE$, EVM  $EVM_P$%, ");
    }//GEN-LAST:event_resetActionPerformed

    private void te_importActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_te_importActionPerformed
      try {

        FileNameExtensionFilter filter = new FileNameExtensionFilter( "TG log format file", "fmt");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showDialog(parent, "Import TG log fomrat file (.fmt) file");


        if(returnVal == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          FileInputStream fis = new FileInputStream(file);
          ObjectInputStream ois = new ObjectInputStream(fis);
          log_format.setText( ois.readUTF() );

          if( parent.prefs!=null) {
            parent.prefs.put("tglog_format", getFormat());
          }
          parent.setStatus("TG log format imported.");

        }

      } catch(Exception e) {
        e.printStackTrace();
      }
    }//GEN-LAST:event_te_importActionPerformed

    private void te_exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_te_exportActionPerformed
      try {

        FileNameExtensionFilter filter = new FileNameExtensionFilter( "tg format file", "fmt");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showDialog(parent, "Export TG Format Export .fmt file");

        ObjectOutputStream oos;

        if(returnVal == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          oos = new ObjectOutputStream( new FileOutputStream(file) );

          oos.writeUTF(log_format.getText());

          oos.flush();
          oos.close();

          parent.setStatus("TG log format exported.");

        }

      } catch(Exception e) {
        e.printStackTrace();
      }
    }//GEN-LAST:event_te_exportActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(tglog_editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(tglog_editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(tglog_editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(tglog_editor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                //new tglog_editor().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton close;
    private javax.swing.JButton help;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField log_format;
    private javax.swing.JButton reset;
    private javax.swing.JButton save;
    private javax.swing.JButton te_export;
    private javax.swing.JButton te_import;
    public javax.swing.JRadioButton tigger_rid;
    public javax.swing.JRadioButton trigger_grant;
    private javax.swing.JRadioButton trigger_voice;
    // End of variables declaration//GEN-END:variables
}
