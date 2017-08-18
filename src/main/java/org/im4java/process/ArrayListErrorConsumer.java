/**************************************************************************
/* This class is an ErrorConsumer which saves the output to an ArrayList.
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

import java.io.IOException;
import java.io.InputStream;


/**
   This class is an ErrorConsumer which saves the output to an ArrayList.

   @version $Revision: 1.1 $
   @author  $Author: bablokb $
 
   @since 1.4.0
*/

public class ArrayListErrorConsumer extends ArrayListConsumer 
                                                     implements ErrorConsumer {

  //////////////////////////////////////////////////////////////////////////////

  /**
     Default Constructor.

     @since 1.4.0
  */

  public  ArrayListErrorConsumer() {
    super();
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Constructor taking a charset-name as argument.

     @param pCharset charset-name for internal InputStreamReader

     @since 1.4.0
  */

  public  ArrayListErrorConsumer(String pCharset) {
    super(pCharset);
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Read command output and save in an internal field.
     @see org.im4java.process.ErrorConsumer#consumeError(java.io.InputStream)

     @since 1.4.0
  */


  public void consumeError(InputStream pInputStream) throws IOException {
    consume(pInputStream);
  }
}
