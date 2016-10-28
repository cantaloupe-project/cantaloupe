/**************************************************************************
/* This class represents a runnable process.
/*
/* Copyright (c) 2010 by Bernhard Bablok (mail@bablokb.de)
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

import java.util.*;
import java.util.concurrent.*;

/**
   This class represents a runnable process. It extends FutureTask so
   it can be used to query the result of an asynchronously run
   process.

   @version $Revision: 1.2 $
   @author  $Author: bablokb $
 
   @since 1.1.0
 */

public class ProcessTask extends FutureTask<ProcessEvent> {

  //////////////////////////////////////////////////////////////////////////////

  /**
     The {@link ProcessStarter} which created this ProcessTask.
  */

  private ProcessStarter iProcessStarter;

  //////////////////////////////////////////////////////////////////////////////

  /**
     The {@link ProcessEvent} of this ProcessTask.
  */

  private ProcessEvent iProcessEvent ;

  //////////////////////////////////////////////////////////////////////////////

  /**
     The constructor.

     @param pProcessStarter The creating ProcessStarter
     @param pArgs           Arguments for the process
     @param pProcessEvent   The ProcessEvent for this ProcessTask
  */

  public ProcessTask(final ProcessStarter pProcessStarter, 
                     final List<String> pArgs,
                     final ProcessEvent pProcessEvent) {
    super(new Runnable() {
	public void run() {
	  pProcessStarter.runAndNotify(pArgs,pProcessEvent);
	}
      },pProcessEvent);
    iProcessStarter = pProcessStarter;
    iProcessEvent   = pProcessEvent;
  }

  ////////////////////////////////////////////////////////////////////////////
  
  /**
     Query the ProcessStarter field of this ProcessTask.

     @return the ProcessStarter which created this ProcessTask
  */
  public ProcessStarter getProcessStarter() {
    return iProcessStarter;
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     {@inheritDoc}
  */

  public boolean cancel(boolean mayInterruptIfRunning) {
    if (isDone()) {
      return super.cancel(mayInterruptIfRunning);
    }
    Process p = iProcessEvent.getProcess();
    if (p != null) {
      p.destroy();
    }
    return super.cancel(mayInterruptIfRunning);
  }

}
