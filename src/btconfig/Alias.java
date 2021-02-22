
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


public Alias(BTFrame parent, String sys_mac) {
  this.parent = parent;
  alias_hash = new java.util.Hashtable();
  //prefs = Preferences.userRoot().node("p25rx_aliasdef");
  //prefs = Preferences.userRoot().node("0x123456789");
  //System.out.println( "prefs:" + prefs.toString() );
  prefs = Preferences.userRoot().node(sys_mac);
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
}

private void read_alias() {
  try {

    if(prefs!=null) {
      for(int i=0;i<NRECS;i++) {
        String idx_rid = i+"_rid";
        String idx_alias = i+"_alias";
        String rid_str = prefs.get( idx_rid, null );
        String alias_str = prefs.get( idx_alias, null );

        try {
          if( rid_str!=null ) parent.addAliasObject(rid_str, i,0);
        } catch(Exception e) {
          e.printStackTrace();
        }
        try {
          if( alias_str!=null ) parent.addAliasObject(alias_str, i,1);
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
  } catch(Exception e) {
    e.printStackTrace();
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void addRID(BTFrame parent, String rid) {
  int first_empty_row=0;

  //System.out.println("Alias.addRID()");

  if(rid==null ) return;



  try {
    int zcheck = new Integer(rid).intValue();

    if(zcheck!=previous_rid) return;
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

        if(o1!=null && ((String) o1).equals(rid.trim()) )  {
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
      for(int n=0;n<8;n++) {
        if(n==i) {
          if(recent_rows[n]>=0) parent.alias_table.setRowSelectionInterval(recent_rows[n],recent_rows[n]);
        }
        else {
          if(recent_rows[n]>=0) parent.alias_table.addRowSelectionInterval(recent_rows[n],recent_rows[n]);
        }
      }
      save_alias();
    }
    previous_rid=i;

    return;  //already found this one
  }

  //check for NAN
  try {
    int i = new Integer(rid).intValue();
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
        parent.addAliasObject( rid, idx, 0);
     } catch(Exception e) {
      e.printStackTrace();
     }

    recent_rows[recent_idx++]=first_empty_row;
    recent_idx = (recent_idx&0x3);
    //parent.alias_table.scrollRectToVisible(new java.awt.Rectangle(parent.alias_table.getCellRect(first_empty_row, 0, true)));

  if(first_empty_row!=previous_rid) {
    for(int n=0;n<8;n++) {
      if(n==first_empty_row) {
        if(recent_rows[n]>=0) parent.alias_table.setRowSelectionInterval(recent_rows[n],recent_rows[n]);
      }
      else {
        if(recent_rows[n]>=0) parent.alias_table.addRowSelectionInterval(recent_rows[n],recent_rows[n]);
      }
    }
    save_alias();
  }
  previous_rid = first_empty_row;


}

private void save_alias() {
  try {
    if(prefs!=null) {

      for(int i=0;i<NRECS;i++) {

        String rid_str="";
        String alias_str="";

        try {
          rid_str = (String) parent.getAliasObject(i,0);
        } catch(Exception e) {
          e.printStackTrace();
        }
        try {
          alias_str = (String) parent.getAliasObject(i,1);
        } catch(Exception e) {
          e.printStackTrace();
        }

        String idx_rid = i+"_rid";
        String idx_alias = i+"_alias";
        if(rid_str!=null && rid_str.length()>0) prefs.put( idx_rid, rid_str );
        if(alias_str!=null && alias_str.length()>0) prefs.put( idx_alias, alias_str );

      }
    }

    //prefs.flush();
  } catch(Exception e) {
    e.printStackTrace();
  }
}

}
