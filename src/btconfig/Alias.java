
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

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Alias
{

java.util.Hashtable alias_hash;
private int NRECS=8000;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
public void addRID(BTFrame parent, String rid) {
  int first_empty_row=0;

  //System.out.println("Alias.addRID()");

  if(rid==null ) return;

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

  if(alias_hash.get(rid.trim())!=null) return;  //already found this one

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

}

}
