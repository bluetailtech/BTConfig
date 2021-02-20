
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


public Alias(BTFrame parent) {
  this.parent = parent;
  read_alias();
}

private void read_alias() {
  try {
    if(alias_hash==null) alias_hash = new java.util.Hashtable();

    if(prefs==null) prefs = Preferences.userRoot().node("p25rx_aliasdefs");

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

  read_alias();

  try {
    int zcheck = new Integer(rid).intValue();
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
    for(int i=0;i<NRECS;i++) {
      try {
        Object o1 = parent.getAliasObject(i,0);
        Object o2 = parent.getAliasObject(i,1);

        if(o1!=null && ((String) o1).equals(rid.trim()) )  {
          parent.setAlias( (String) o2 ); 
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
    save_alias();

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

  save_alias();

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
  } catch(Exception e) {
    e.printStackTrace();
  }
}

}
