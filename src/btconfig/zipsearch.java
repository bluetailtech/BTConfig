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
import java.util.zip.*;
import java.awt.geom.*;

public class zipsearch
{
  public freqSearch fs;
  public Boolean city_state=false;
  private BufferedReader br_loc;
  private BufferedReader br_freq;
  private BufferedReader br_zips;
  private BTFrame parent;
  private int do_search3=0;

  private int MAXREC=8000;
  private Hashtable en_hash;


  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public zipsearch(BTFrame p) {
    parent = p;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public void search3(Hashtable en_hash) {
    do_search3=1;
    this.en_hash = en_hash;

    try {
      do_search(null);
    } catch(Exception e) {
    }
    do_search3=0;
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public void search2(String[] arg) {
   try {
    city_state=true;
    ZipInputStream zis3 = new ZipInputStream( getClass().getResourceAsStream("/btconfig/us_zipcodes.csv.zip") );
    ZipEntry ze3 = zis3.getNextEntry();
    br_zips = new BufferedReader( new InputStreamReader( zis3 ));
     String[] newarg = null; 
    String line="";

      ///////////////////////////////////////////////////////
      //find lat,lng for zip code 
      ///////////////////////////////////////////////////////
      while( ( line = br_zips.readLine() ) != null ) {
        String[] strs = line.split(",");
        //1,9,10
        String zip = strs[1];
        String city = strs[2].toUpperCase().trim();
        String state = strs[4].toUpperCase().trim();
        String lat = strs[9];
        String lng = strs[10];

        if( city.contains(arg[0].toUpperCase().trim()) && state.trim().equals(arg[1].toUpperCase().trim()) ) {
          newarg = new String[2];
          newarg[0] = zip;
          newarg[1] = arg[2];
          break;
        }

      }
    do_search(newarg);
   } catch(Exception e) {
     e.printStackTrace();
   }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public void search(String[] arg) {
    city_state=false;
    do_search(arg);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  public void do_search(String[] arg) {
    String line = null;

    parent.lat_lon_hash1 = new Hashtable();

    Hashtable netid_hash = new Hashtable();
    Hashtable freq_hash = new Hashtable();
    Hashtable loc_hash = new Hashtable();
    Hashtable freqonly_hash = new Hashtable();

    Boolean include_vhf=true;
    Boolean include_all_radio_service_codes=true;
    Boolean gov_only=false;

    if(parent.inc_vhf.isSelected()) {
      include_vhf=true;
    }
    else {
      include_vhf=false;
    }

    try {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() { 
          parent.jScrollPane8.getVerticalScrollBar().setValue(0);
        }
      });

    } catch(Exception e) {
    }

    /*
    if(fs==null) {
      fs = new freqSearch();
      fs.setSize(600,800);
    }
    fs.show();
    fs.freqsearch_ta.setText("");
    */

    parent.testfreqs.setEnabled(true);
    parent.use_freq_primary.setEnabled(true);

      parent.freq_table.setRowSelectionInterval(0,0);

    try {
      if(do_search3==1) {
        for(int i=0;i<MAXREC;i++) {
         addTableObject( null, i, 4); 
        }
      }

      if(do_search3==0) {

        for(int i=0;i<MAXREC;i++) {
         addTableObject( null, i, 0); 
         addTableObject( null, i, 1); 
         addTableObject( null, i, 2); 
         addTableObject( null, i, 3); 
         addTableObject( null, i, 4); 
         addTableObject( null, i, 5); 
         addTableObject( null, i, 6); 
         addTableObject( null, i, 7); 
         addTableObject( null, i, 8); 
         addTableObject( null, i, 9); 
         addTableObject( null, i, 10); 
         addTableObject( null, i, 11); 
        }
        if(arg==null || arg.length<2) {
          parent.setStatus("no records found");
          return;
        }
        if(arg.length>=2) {
          for(int i=2;i<arg.length;i++) {
            arg[i] = arg[i].trim();
            if( arg[i].equals("vhf") ) {
              include_vhf=true;
              //System.out.println("arg: "+arg[i]);
            }
            if( arg[i].equals("allrc") ) {
              include_all_radio_service_codes=true;
              //System.out.println("arg: "+arg[i]);
            }
            if( arg[i].equals("gov") ) {
              gov_only=true;
              //System.out.println("arg: "+arg[i]);
            }
          }
        }
      }


      //ZipInputStream zis1 = new ZipInputStream( new FileInputStream("locations.csv.zip") );
      //ZipInputStream zis2 = new ZipInputStream( new FileInputStream("freqs.csv.zip") );
      //ZipInputStream zis3 = new ZipInputStream( new FileInputStream("us_zipcodes.csv.zip") );
      //ZipInputStream zis1 = new ZipInputStream( getClass().getResourceAsStream("/btconfig/locations.csv.zip") );
      ZipInputStream zis2 = new ZipInputStream( getClass().getResourceAsStream("/btconfig/freqs.csv.zip") );
      ZipInputStream zis3 = new ZipInputStream( getClass().getResourceAsStream("/btconfig/us_zipcodes.csv.zip") );

      //ZipEntry ze1 = zis1.getNextEntry();
      ZipEntry ze2 = zis2.getNextEntry();
      ZipEntry ze3 = zis3.getNextEntry();

      //br_loc = new BufferedReader( new FileReader( "locations.csv" ) );
      //br_freq = new BufferedReader( new FileReader( "freqs.csv" ) );
      //br_zips = new BufferedReader( new FileReader( "us_zipcodes.csv" ) );
      //br_loc = new BufferedReader( new InputStreamReader( zis1 ) );
      br_freq = new BufferedReader( new InputStreamReader( zis2 ) );
      br_zips = new BufferedReader( new InputStreamReader( zis3 ));

      int nets=0;

      double center_lat = 0.0; 
      double center_lng = 0.0; 
      double radius_deg = 0.0; 
      String city="";
      String console="";


      if(do_search3==0) {

        radius_deg = new Double(arg[1]).doubleValue() / 69.169;  //convert miles to degrees
        ///////////////////////////////////////////////////////
        //find lat,lng for zip code 
        ///////////////////////////////////////////////////////
        while( ( line = br_zips.readLine() ) != null ) {
          String[] strs = line.split(",");
          //1,9,10
          String zip = strs[1];
          city = strs[2];
          String lat = strs[9];
          String lng = strs[10];

          if(zip.equals(arg[0])) {
            center_lat = new Double( lat ).doubleValue();
            center_lng = new Double( lng ).doubleValue();
            break;
          }

        }

        if( center_lat==0.0 || center_lng==0.0) {
          parent.setStatus("no records found");
          return;
        }


        console = "searching "+arg[1]+" miles around "+city;
        //console = console+"\n\n"+"LICENSE, GRANTEE, ENTITY, FREQ, CLASS_CODE, CITY, STATE"; 
        //console = console+"\n"+  "--------------------------------------------------------------------------------------------------";
        System.out.println(console);
        //fs.freqsearch_ta.setText(console+"\n");
        parent.setStatus(console);
      }


      int nrecs=0;
      ///////////////////////////////////////////////////////
      //find nets within xx degrees of center lat,lng 
      ///////////////////////////////////////////////////////
      while( ( line = br_freq.readLine() ) != null ) {
        String[] strs = line.split(",");

        if(strs.length!=10) continue; //corrupt record

        String freq = strs[0];
        String desc = strs[1];
        String cityo = strs[2];
        String state = strs[3];
        String net_id = strs[4];
        String entity = strs[5];
        String radio_service_code = strs[6];
        String lat = strs[7];
        String lng = strs[8];
        String emission = strs[9];


        double llat=0.0;
        double llng=0.0;
        double freq_d=0.0;
        try {
          llat = new Double(lat).doubleValue();
          llng = new Double(lng).doubleValue();
        } catch(Exception e) {
          continue; //corrupt location
        }

        try {
          freq_d = new Double(freq).doubleValue();
        } catch(Exception e) {
        }

        Point2D.Double p2d=null;
        if(do_search3==1) {
          p2d = (Point2D.Double) parent.lat_lon_hash2.get( String.format("%3.8f",freq_d) );
          //if(p2d!=null) System.out.println("p2d not null");
        }

        if( do_search3==1 && p2d!=null && p2d.x == llat && p2d.y == llng ) {


            if(line.contains("7K6") && !parent.inc_dmr.isSelected()) continue;  //include DMR?
            //if( (line.contains("8K10F1D")||line.contains("8K10F1E")||line.contains("8K30"))  && !parent.inc_p25p1.isSelected()) continue;  //include P25P1?
            //if( (line.contains("8K10F1W")||line.contains("9K80D7W")||line.contains("9K70F1D")||line.contains("9K70F1E")||line.contains("9K80F1D")||line.contains("9K80F1E"))  && !parent.inc_p25p2.isSelected()) continue;  //include P25P2?
            if( (line.contains("8K10")||line.contains("8K30")||line.contains("8K70")||line.contains("9K8")||line.contains("9K7"))  && !parent.inc_p25.isSelected()) continue;  //include P25P2?

            double f = 0.0;
            try {
              f = new Double(freq).doubleValue();
              if(!include_vhf && f>130.0 && f<400.0) f = 0.0;
              else if(!parent.inc_400mhz.isSelected() && f>=400.0 && f<700.0) f = 0.0;
              else if(!parent.inc_700mhz.isSelected() && f>=700.0 && f<800.0) f = 0.0;
              else if(!parent.inc_800mhz.isSelected() && f>=800.0 && f<900.0) f = 0.0;
              else if(!parent.inc_900mhz.isSelected() && f>=900.0 && f<960.0) f = 0.0;
            } catch(Exception e) {
            }

            if(f==0.0 || (freq_hash.get(freq)!=null && !parent.inc_dup_freq.isSelected()) ) {
              continue;  //no records with the same freq as other records
            }
            else {
              freq = String.format("%3.8f", f);
              freq_hash.put(freq,freq);
              parent.lat_lon_hash1.put(freq, new Point2D.Double(llat, llng) );
            }

            if( (!radio_service_code.startsWith("Y") && !radio_service_code.equals("SY")) && parent.inc_trunked_only.isSelected()) continue; //only show trunked systems?

            if(!parent.inc_gov.isSelected() && entity.startsWith("G")) continue; 
            if(!parent.inc_bus.isSelected() && entity.startsWith("B")) continue; 


            System.out.println(line);        
            String report = net_id+","+desc+","+entity+","+freq+","+radio_service_code+","+cityo+","+state+","+emission;
            loc_hash.put(report,report);
            nrecs++;
        }

        if( do_search3==0 && 
            llat > (center_lat-radius_deg) && 
            llat < (center_lat+radius_deg) && 
            llng > (center_lng-radius_deg) && 
            llng < (center_lng+radius_deg) ) {

            if(line.contains("7K6") && !parent.inc_dmr.isSelected()) continue;  //include DMR?
            //if( (line.contains("8K10F1D")||line.contains("8K10F1E")||line.contains("8K30"))  && !parent.inc_p25p1.isSelected()) continue;  //include P25P1?
            //if( (line.contains("8K10F1W")||line.contains("9K80D7W")||line.contains("9K70F1D")||line.contains("9K70F1E")||line.contains("9K80F1D")||line.contains("9K80F1E"))  && !parent.inc_p25p2.isSelected()) continue;  //include P25P2?
            if( (line.contains("8K10")||line.contains("8K30")||line.contains("8K70")||line.contains("9K8")||line.contains("9K7"))  && !parent.inc_p25.isSelected()) continue;  //include P25P2?

            double f = 0.0;
            try {
              f = new Double(freq).doubleValue();
              if(!include_vhf && f>130.0 && f<400.0) f = 0.0;
              else if(!parent.inc_400mhz.isSelected() && f>=400.0 && f<700.0) f = 0.0;
              else if(!parent.inc_700mhz.isSelected() && f>=700.0 && f<800.0) f = 0.0;
              else if(!parent.inc_800mhz.isSelected() && f>=800.0 && f<900.0) f = 0.0;
              else if(!parent.inc_900mhz.isSelected() && f>=900.0 && f<960.0) f = 0.0;
            } catch(Exception e) {
            }

            if(f==0.0 || (freq_hash.get(freq)!=null && !parent.inc_dup_freq.isSelected()) ) {
              //if( desc.toLowerCase().contains("solano") ) System.out.println("1:"+line);
              continue;  //no records with the same freq as other records
            }
            else {
              freq = String.format("%3.8f", f);
              freq_hash.put(freq,freq);
              //System.out.println("adding freq "+freq);
              parent.lat_lon_hash1.put(freq, new Point2D.Double(llat, llng) );
            }

            if( (!radio_service_code.startsWith("Y") && !radio_service_code.equals("SY")) && parent.inc_trunked_only.isSelected()) continue; //only show trunked systems?

            if(!parent.inc_gov.isSelected() && entity.startsWith("G")) continue; 
            if(!parent.inc_bus.isSelected() && entity.startsWith("B")) continue; 


            System.out.println(line);        
            String report = net_id+","+desc+","+entity+","+freq+","+radio_service_code+","+cityo+","+state+","+emission;
            loc_hash.put(report,report);
            nrecs++;
        }


     }
     System.out.println("found records "+nrecs);


    int nresults=0;
    int idx=0;
    javax.swing.JButton test_button = new javax.swing.JButton();
    javax.swing.JButton use_button = new javax.swing.JButton();
    test_button.setText("TEST");
    use_button.setText("USE");
     //sort output 
     List<String> tmp = Collections.list(loc_hash.keys());
     Collections.sort(tmp);
     Iterator<String> it = tmp.iterator();
     while(it.hasNext()) {
       String p25f = it.next();

       String[] strs = p25f.split(",");

       if( strs[7].equals("8K10F1D") ) strs[7] = "P25P1_8K10F1D"; //41759 records  COMMON  DATA   (phase 1)
       if( strs[7].equals("8K10F1E") ) strs[7] = "P25P1_8K10F1E"; //59403 records  VERY COMMON  VOICE   (phase 1)
       if( strs[7].equals("8K10F1W") ) strs[7] = "P25_8K10F1W"; //8661 records   not sure if phase 1 or 2 or both 
       if( strs[7].equals("8K30F1W") ) strs[7] = "P25P1_V_AND_D"; //4275 records   strange hybrid  could be part time control channel + voice
       if( strs[7].equals("8K70D1W") ) strs[7] = "P25P1_P2_LSM_ASTRO_8K70D1W"; //51995 records VERY COMMON (shared P1 and P2 LSM on same P1 control channel)
       if( strs[7].equals("9K70F1D") ) strs[7] = "P25P1_P2_LSM_9K70F1D"; //2239 records   NOT COMMON
       if( strs[7].equals("9K70F1E") ) strs[7] = "P25P1_P2_LSM_9K70F1E"; //2244 records   NOT COMMON
       if( strs[7].equals("9K80D7W") ) strs[7] = "P25P2_DQPSK";   //46000 records  VERY COMMON  PHASE 2 ONLY  
       if( strs[7].equals("7K60FXD") ) strs[7] = "DMR_DATA";
       if( strs[7].equals("7K60FXE") ) strs[7] = "DMR_VOICE";
       if( strs[7].equals("7K60FXW") ) strs[7] = "DMR_V_AND_D";

       try {
         addTableObject( strs[0], idx, 0); 
         addTableObject( strs[1], idx, 1); 
         addTableObject( strs[2], idx, 2); 
         addTableObject( strs[3], idx, 3); 

         if(strs[3].equals(parent.frequency_tf1.getText())) {
           addTableObject( "PRIM", idx, 4); 
         }

         //addTableObject( test_button, idx, 4); 
         //addTableObject( use_button, idx, 6); 

         addTableObject( strs[4], idx, 7); 
         addTableObject( strs[5], idx, 8); 
         addTableObject( strs[6], idx, 9); 
         addTableObject( strs[7], idx, 10); 
         idx++;

       } catch(Exception e) {
       }

       //String ta = fs.freqsearch_ta.getText();
       //ta = ta.concat(p25f+"\n");
       //fs.freqsearch_ta.setText(ta);
       nresults++;
       if(idx>MAXREC-1) break;
     }

     if(nresults>0) {
      if(parent.op_mode.getSelectedIndex()==0) parent.testfreqs.setEnabled(true);
      parent.use_freq_primary.setEnabled(true);
     }

     parent.do_read_roaming=1;

     if(do_search3==1) {
       parent.do_read_roaming=0;

     }

     try {
       List<String> tmp2 = Collections.list(parent.no_loc_freqs.keys());
       Collections.sort(tmp2);
       Iterator<String> it2 = tmp2.iterator();
       while(it2.hasNext() && idx<MAXREC) {
         String p25f = it2.next();
         System.out.println("no loc freq "+p25f);
         addTableObject( p25f, idx, 3); 
         addTableObject( "X", idx, 6); 
         if(parent.frequency_tf1.getText().trim().equals(p25f.trim())) {
           parent.freq_table.getModel().setValueAt("PRIM",idx,4);
         } 
         else {
           parent.freq_table.getModel().setValueAt("",idx,4);
         }

         try {
           byte r_en = 0; 
           try {
             r_en = ((Byte) en_hash.get(p25f)).byteValue();
           } catch(Exception e) {
             r_en=0;
           }
           System.out.println("r_en: "+r_en);

           if(r_en>0) {
             parent.freq_table.getModel().setValueAt(true,idx,11);
           }
           else {
             parent.freq_table.getModel().setValueAt(false,idx,11);
           }
         } catch(Exception e) {
           e.printStackTrace();
         }


         idx++;
       }
       console = "\nfound "+nresults+" records with unique frequencies.";
     } catch(Exception e) {
       System.out.println("no roaming records found.");
       parent.setStatus(console);
       return;
     }

     //String ta = fs.freqsearch_ta.getText();
     //ta = ta.concat(console+"\n");
     System.out.println(console);
     parent.setStatus(console);
     //fs.freqsearch_ta.setText(ta);

    } catch( Exception e ) {
      e.printStackTrace();
    }
  }
  //////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////
  void addTableObject(Object obj, int row, int col) {
    parent.freq_table.getModel().setValueAt(obj,row,col);
  }
  //////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////
  Object getTableObject(int row, int col) {
    return parent.freq_table.getModel().getValueAt(row,col);
  }

}
