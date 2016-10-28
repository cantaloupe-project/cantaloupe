/**************************************************************************
/* This class implements a pipe.
/*
/* Copyright (c) 2009 by Bernhard Bablok (mail@bablokb.de)
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

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;


/**
   This class implements a pipe. Useful for piping input to a process
   or piping output/error from a process to other streams.

   <p>You can use the same Pipe-object for both ends of a process-pipeline.
   But you cannot use the same Pipe-object as an OutputConsumer and
   ErrorConsumer at the same time.</p>

   @version $Revision: 1.8 $
   @author  $Author: bablokb $
 
   @since 0.95
*/

public class Pipe implements InputProvider, OutputConsumer, ErrorConsumer {

  //////////////////////////////////////////////////////////////////////////////

  /**
     Default buffer size of the pipe. Currently 64KB.
  */

  public static final int BUFFER_SIZE=65536;

 //////////////////////////////////////////////////////////////////////////////

  /**
     The source of data (i.e. this pipe will provide input for a process).
  */

  private InputStream iSource;

 //////////////////////////////////////////////////////////////////////////////

  /**
     The sink for data (i.e. this pipe will consume output of a process).
  */

  private OutputStream iSink;

 //////////////////////////////////////////////////////////////////////////////

  /**
     Constructor. At least one of the arguments should not be null. 
  */

  public Pipe(InputStream pSource, OutputStream pSink) {
    iSource = pSource;
    iSink = pSink;
  }

 //////////////////////////////////////////////////////////////////////////////

  /**
     The InputProvider must write the input to the given OutputStream.
  */

  public void provideInput(OutputStream pOutputStream) throws IOException {
    copyBytes(iSource,pOutputStream);
  }

 //////////////////////////////////////////////////////////////////////////////

  /**
     The OutputConsumer must read the output of a process from the given
     InputStream.
  */

  public void consumeOutput(InputStream pInputStream) throws IOException {
    if (iSink != null) {
      copyBytes(pInputStream,iSink);
    }
  }

 //////////////////////////////////////////////////////////////////////////////

  /**
     The ErrorConsumer must read the error of a process from the given
     InputStream.
  */

  public void consumeError(InputStream pInputStream) throws IOException {
    if (iSink != null) {
      copyBytes(pInputStream,iSink);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Copy bytes from an InputStream to an OutputStream.
  */

  private void copyBytes(InputStream pIs, OutputStream pOs) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    while (true) {
     int byteCount = pIs.read(buffer);
     if (byteCount == -1) {
       break;
     }
     // synchronize on OutputStream
     synchronized(pOs) {
       pOs.write(buffer,0,byteCount);
     }
    }
    pOs.flush();
   }
}
