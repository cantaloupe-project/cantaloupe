/**************************************************************************
/* This class is the base class with common methods for
/* ArrayListOutputConsumer and  ArrayListErrorConsumer.
/*
/* Copyright (c) 2009-2013 by Bernhard Bablok (mail@bablokb.de)
/*
/* This program is free software; you can redistribute it and/or modify
/* it under the terms of the GNU Library General Public License as published
/* by  the Free Software Foundation; either version 2 of the License or
/* (at your option) any later version.
/*
/* This program is distributed in the hope that it will be useful, but
/* WITHOUT ANY WARRANTY; without even the implied warranty of
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/* GNU Library General Public License for more details.
/*
/* You should have received a copy of the GNU Library General Public License
/* along with this program; see the file COPYING.LIB.  If not, write to
/* the Free Software Foundation Inc., 59 Temple Place - Suite 330,
/* Boston, MA  02111-1307 USA
/**************************************************************************/

package org.im4java.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
   Base class for ArrayListOutputConsumer and ArrayListErrorConsumer
   with common methods.

   @version $Revision: 1.1 $
   @author  $Author: bablokb $
 
   @since 1.4.0

   @see ArrayListOutputConsumer
   @see ArrayListErrorConsumer
*/

public abstract class ArrayListConsumer {

  //////////////////////////////////////////////////////////////////////////////

  /**
     The output list.
     
     @since 1.4.0
  */

  private ArrayList<String> iOutputLines = new ArrayList<String>();

  //////////////////////////////////////////////////////////////////////////////

  /**
     The charset-name for the internal InputStreamReader.

     @since 1.4.0
  */

  private String iCharset = null;

  //////////////////////////////////////////////////////////////////////////////

  /**
     Default Constructor.

     @since 1.4.0
  */

  public  ArrayListConsumer() {
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Constructor taking a charset-name as argument.

     @param pCharset charset-name for internal InputStreamReader

     @since 1.4.0
  */

  public  ArrayListConsumer(String pCharset) {
    iCharset = pCharset;
  }

  //////////////////////////////////////////////////////////////////////////////
  
  /**
     Return array with output-lines.

     @since 1.4.0
  */

  public ArrayList<String> getOutput() {
    return iOutputLines;
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Clear the output.

     @since 1.4.0
  */

  public void clear() {
    iOutputLines.clear();
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Read from InputStream and save in an internal field.

     @param pInputStream source InputStream

     @since 1.4.0
  */

  protected void consume(InputStream pInputStream) throws IOException {
    InputStreamReader isr = null;
    if (iCharset == null) {
      isr = new InputStreamReader(pInputStream);
    } else {
      isr = new InputStreamReader(pInputStream,iCharset);
    }
    BufferedReader reader = new BufferedReader(isr);
    String line;
    while ((line=reader.readLine()) != null) {
      iOutputLines.add(line);
    }
    reader.close();
  }
}
