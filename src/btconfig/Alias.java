
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

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.fazecast.jSerialComm.*;
import javax.swing.filechooser.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.prefs.Preferences;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Alias
{

java.util.Hashtable alias_hash;
private int NRECS=8000;
private BTFrame parent; 
Preferences prefs;
int[] recent_rows;
int recent_idx=0;
int previous_rid;
String home_dir;
String sys_mac_id;
TableRowSorter trs;

public Alias(BTFrame parent, String sys_mac_id, String home_dir) {
  this.parent = parent;
  this.home_dir = home_dir;
  this.sys_mac_id = sys_mac_id;

  alias_hash = new java.util.Hashtable();
  prefs = Preferences.userRoot().node(sys_mac_id);
  read_alias();
  recent_rows = new int[8];
  recent_rows[0]=-1;
  recent_rows[1]=-1;
  recent_rows[2]=-1;
  recent_rows[3]=-1;
  recent_rows[4]=-1;
  recent_rows[5]=-1;
  recent_rows[6]=-1;
  recent_rows[7]=-1;
  parent.alias_table.setRowSelectionAllowed(true);
  parent.alias_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

  trs = new TableRowSorter(parent.alias_table.getModel());
  trs.setComparator(0, new IntComparator());
  parent.alias_table.setRowSorter(trs);
}

private void read_alias() {
  try {

    String fs =  System.getProperty("file.separator");
    File cdir = new File(home_dir+"p25rx"+fs+sys_mac_id+fs+"p25rx_aliases.csv");
    try {
      if(cdir.length()==0) {
        do_import_from_prefs();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }


    LineNumberReader lnr = new LineNumberReader( new FileReader(cdir) );
    import_alias_csv(parent, lnr);

  } catch(Exception e) {
    e.printStackTrace();
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void import_alias_csv(BTFrame parent, LineNumberReader lnr)
{

  try {

    int number_of_records=0;

    String in_line="";
    String[] strs = null;

    System.out.println("import aliases");

    while(number_of_records<NRECS) {

      in_line = lnr.readLine();
      
      if(in_line!=null && in_line.length()>1) {
        in_line = in_line.trim();

        strs = in_line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

        if(strs!=null && strs.length>=1) { 

            String str1="";
            String str2=null;

            if(strs[0]!=null) str1 = strs[0];  
            if(strs.length>1 && strs[1]!=null) str2 = strs[1]; 

            //if( str1!=null && str1.startsWith("0x") ) str1 = str1.substring(2,str1.length()); 
            //Integer rid_hex = Integer.valueOf(str1,16);
            //System.out.println(":"+str1+":"+str2+":");

            try {
              if( str1!=null ) parent.addAliasObject(new Integer(str1), number_of_records,0);
            } catch(Exception e) {
              e.printStackTrace();
            }
            try {
              if( str2!=null ) parent.addAliasObject(str2, number_of_records,1);
                else parent.addAliasObject(null, number_of_records,1);
            } catch(Exception e) {
              e.printStackTrace();
            }


        }
      }
      else {
        //parent.addAliasObject(null, number_of_records,0);
        //parent.addAliasObject(null, number_of_records,1);
        break;
      }

      number_of_records++;
    }

    NRECS=number_of_records;

    if(parent!=null) ((DefaultTableModel) parent.alias_table.getModel()).setRowCount(NRECS);

    System.out.println(number_of_records+" alias records");

    lnr.close();

  } catch (Exception e) {
    e.printStackTrace();
  }
}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void addRID(BTFrame parent, String rid) {
  int first_empty_row=0;

  //System.out.println("Alias.addRID()");

  if(rid==null ) return;


  try {
    int zcheck = Integer.valueOf(rid);

    if(zcheck==previous_rid) return;
    previous_rid=zcheck;

    if(zcheck==0) return;
  } catch(Exception e) {
    return;
  }

  if(alias_hash==null) alias_hash = new java.util.Hashtable();
    else alias_hash.clear();

  for(int i=0;i<NRECS;i++) {
    try {
      Object o1 = parent.getAliasObject(i,0);

      if(o1!=null )  {
        alias_hash.put(o1.toString().trim(),  o1.toString().trim());
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  if(alias_hash.get(rid.trim())!=null) {
    int i=0;
    for(i=0;i<NRECS;i++) {
      try {
        Object o1 = parent.getAliasObject(i,0);
        Object o2 = parent.getAliasObject(i,1);

        if(o1!=null && ((Integer) o1).toString().equals(rid.trim()) )  {
          parent.setAlias( (String) o2 ); 
          break;
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
    
    recent_rows[recent_idx++]=i;
    recent_idx = (recent_idx&0x7);

    //parent.alias_table.scrollRectToVisible(new java.awt.Rectangle(parent.alias_table.getCellRect(i, 0, true)));

    if(i!=previous_rid) {
      /*
      for(int n=0;n<8;n++) {
        if(n==i) {
          if(recent_rows[n]>=0) parent.alias_table.setRowSelectionInterval(recent_rows[n],recent_rows[n]);
        }
        else {
          if(recent_rows[n]>=0) parent.alias_table.addRowSelectionInterval(recent_rows[n],recent_rows[n]);
        }
      }
      */
      save_alias();
    }
    previous_rid=i;

    return;  //already found this one
  }

  if(parent!=null) {
    NRECS++;
    ((DefaultTableModel) parent.alias_table.getModel()).setRowCount(NRECS);
  }


  //check for NAN
  try {
    int i = Integer.valueOf(rid);
  } catch(Exception e) {
    e.printStackTrace();
    return;
  }

   //find first empty row
  for(int i=0;i<NRECS;i++) {
    try {
      Object o1 = parent.getAliasObject(i,0);

      if(o1!=null )  {
        first_empty_row++;
      }
      else {
        break;
      }

    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  System.out.println("first empty row "+first_empty_row);

  int idx = first_empty_row;

    try {
        parent.addAliasObject( new Integer(rid), idx, 0);
     } catch(Exception e) {
      e.printStackTrace();
     }

    recent_rows[recent_idx++]=first_empty_row;
    recent_idx = (recent_idx&0x3);
    //parent.alias_table.scrollRectToVisible(new java.awt.Rectangle(parent.alias_table.getCellRect(first_empty_row, 0, true)));

  if(first_empty_row!=previous_rid) {
    for(int n=0;n<8;n++) {
      /*
      if(n==first_empty_row) {
        if(recent_rows[n]>=0) parent.alias_table.setRowSelectionInterval(recent_rows[n],recent_rows[n]);
      }
      else {
        if(recent_rows[n]>=0) parent.alias_table.addRowSelectionInterval(recent_rows[n],recent_rows[n]);
      }
      */
    }
    save_alias();
  }
  previous_rid = first_empty_row;


}

private void do_import_from_prefs() {

    int rec_n=0;

    System.out.println("\r\ndo import from prefs");

    if(prefs!=null) {
      for(int i=0;i<NRECS;i++) {
        String idx_rid = i+"_rid";
        String idx_alias = i+"_alias";
        String rid_str = prefs.get( idx_rid, null );
        String alias_str = prefs.get( idx_alias, null );

        try {
          if( rid_str!=null && alias_str!=null && rid_str.length()>0 && alias_str.length()>0) {
            parent.addAliasObject(new Integer(rid_str), rec_n,0);
            parent.addAliasObject(alias_str, rec_n,1);
            rec_n++;
          }
        } catch(Exception e) {
          e.printStackTrace();
        }
        try {
        } catch(Exception e) {
          e.printStackTrace();
        }
      }

      save_alias();
    }
}
private void save_alias() {


    try {

      String fs =  System.getProperty("file.separator");
      File file = new File(home_dir+"p25rx"+fs+sys_mac_id+fs+"p25rx_aliases.csv");

      //System.out.println("\r\nsaving alias file "+file);

      FileOutputStream fos = new FileOutputStream(file);

      for(int i=0;i<NRECS;i++) {
        String rid_str="";
        String alias_str="";

        try {
          rid_str = (String) parent.getAliasObject(i,0).toString();
        } catch(Exception e) {
          e.printStackTrace();
        }
        try {
          alias_str = (String) parent.getAliasObject(i,1);
        } catch(Exception e) {
          e.printStackTrace();
        }

        if(rid_str==null || rid_str.equals("null")) rid_str="";
        if(alias_str==null || alias_str.equals("null")) alias_str="";

        if(rid_str!=null && rid_str.length()>0) {
          String out_line = rid_str+","+alias_str+"\r\n";
          fos.write(out_line.getBytes()); 
        }

      }

      fos.flush();
      fos.close();

  } catch(Exception e) {
    e.printStackTrace();
  }
}

class IntComparator implements Comparator {
  public int compare(Object o1, Object o2) {
    Integer int1 = (Integer)o1;
    Integer int2 = (Integer)o2;
    return int1.compareTo(int2);
  }

  public boolean equals(Object o2) {
    return this.equals(o2);
  }
}

}
