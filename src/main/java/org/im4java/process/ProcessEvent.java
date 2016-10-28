/**************************************************************************
/* This class wraps return-code and Exceptions of a terminated process.
/*
/* Copyright (c) 2009-2010 by Bernhard Bablok (mail@bablokb.de)
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

/**
   This class  wraps return-code and Exceptions of a terminated process. 

   @version $Revision: 1.4 $
   @author  $Author: bablokb $
 
   @since 0.06
 */

public class ProcessEvent {
  
  ////////////////////////////////////////////////////////////////////////////

  /**
     The Process this ProcessEvent belongs to.
  */

  private Process iProcess;
  
  ////////////////////////////////////////////////////////////////////////////

  /**
     The Process-ID of the process. Note that this ID has nothing to do with
     the process-id of the operating-system for the given process.
  */

  private int iPID;
  
  ////////////////////////////////////////////////////////////////////////////

  /**
     The ProcessStarter which started the given process.
  */

  private ProcessStarter iProcessStarter;
  
  ////////////////////////////////////////////////////////////////////////////
  
  /**
    The return-code of the process. Note that this field is only valid, if
    no exception occured.
  */
  
  private int iReturnCode=Integer.MIN_VALUE;
  
  ////////////////////////////////////////////////////////////////////////////
  
  /**
    If this field is not null, the process ended with this exception.
  */
  
  private Exception iException=null;

  //////////////////////////////////////////////////////////////////////////////
  
  /**
     Constructor (sets pid and ProcessStarter).

     @param pPID             the process-id of this proces
     @param pProcessStarter  the ProcessStarter generating this event
  */
  
  public ProcessEvent(int pPID, ProcessStarter pProcessStarter) {
    iPID = pPID;
    iProcessStarter = pProcessStarter;
  }

  ///////////////////////////////////////////////////////////////////////////////

  /**
     Set the return-code of the process.

     @param pReturnCode the return-code to set.
  */

  public void setReturnCode(int pReturnCode) {
    iReturnCode = pReturnCode;
  }

  ////////////////////////////////////////////////////////////////////////////
  
  /**
     Get the return-code of the process. The return-code is only valid if
     no exception occured.

     @return the return-code of this Process.
  */
  
  public int getReturnCode() {
    return iReturnCode;
  }

  ///////////////////////////////////////////////////////////////////////////////

  /**
     Set the Process-object of this ProcessEvent.

     @param pProcess the Process to set
  */
  public void setProcess(Process pProcess) {
    iProcess = pProcess;
  }

  ////////////////////////////////////////////////////////////////////////////
  
  /**
     Get the Process-object of this ProcessEvent.

     @return the Process-object
  */
  public Process getProcess() {
    return iProcess;
  }

  /////////////////////////////////////////////////////////////////////////////
  
  /**
     Set the exception-field of this event.

     @param pException the iException to set
  */
  public void setException(Exception pException) {
    iException = pException;
  }

  ////////////////////////////////////////////////////////////////////////////
  
  /**
     Query the exception-field of this event.

     @return the exception-field of this event
  */
  public Exception getException() {
    return iException;
  }

  ////////////////////////////////////////////////////////////////////////////
  
  /**
     Query the process-id field of this event.

     @return the process-id of the process related to this event.
  */
  public int getPID() {
    return iPID;
  }

  ////////////////////////////////////////////////////////////////////////////
  
  /**
     Query the ProcessStarter field of this event.

     @return the ProcessStarter which startetd the process related to this event.
  */
  public ProcessStarter getProcessStarter() {
    return iProcessStarter;
  }
}
