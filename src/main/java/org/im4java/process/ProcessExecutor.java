/**************************************************************************
/* This class subclasses ThreadPoolExecutor and implements a
/* pooling-service for threads running processes.
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
   This class subclasses ThreadPoolExecutor and implements a
   pooling-service for threads running processes.

   @version $Revision: 1.3 $
   @author  $Author: bablokb $
 
   @since 1.1.0
 */

public class ProcessExecutor 
                    extends ThreadPoolExecutor implements ProcessEventListener {

  //////////////////////////////////////////////////////////////////////////////

  /**
     Synchronized collection of Processes added to this ProcessExecutor.
  */

  private Set<Process> iProcesses = new LinkedHashSet<Process>();

  //////////////////////////////////////////////////////////////////////////////

  /**
     Flag to prevent any processes to start during {@link shutdownNow}.
  */

  private boolean iShutdownNowInProgress=false;

  //////////////////////////////////////////////////////////////////////////////

  /**
     The default constructor creates a fixed-sized thread-pool with
     corePoolSize == maximumPoolSize.
     The maximum number of concurrently running processes is controlled
     by the system-property <em>im4java.maxProcs</em>. If unset or if the 
     property has the value <em>auto</em>, the number returned by
     Runtime.availableProcessors() is used.
  */

  public ProcessExecutor() {
    this(getDefaultPoolSize());
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Create a ProcessExecutor with given number of threads for
     corePoolSize and maximumPoolSize.

     @param pProcs Number of concurrently executing processes.
  */

  public ProcessExecutor(int pProcs) {
    // create superclass with pProcs threads
    super(pProcs,pProcs,60,TimeUnit.SECONDS,
                                            new LinkedBlockingQueue<Runnable>());
    prestartAllCoreThreads();
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Return the initial pool-size.
  */

  private static int getDefaultPoolSize() {
    int nProcs;
    String maxProcs = System.getProperty("im4java.maxProcs");
    if (maxProcs == null || maxProcs.equals("auto")) {
      nProcs = Runtime.getRuntime().availableProcessors();
    } else {
      nProcs=Integer.parseInt(maxProcs);
    }
    return Math.max(1,nProcs);
  }

  ////////////////////////////////////////////////////////////////////////////

  /**
     Register this ProcessExecutor as a  {@link ProcessEventListener}.

     {@inheritDoc}
  */

  protected void beforeExecute(Thread t, Runnable r) {
    // ugly hack to prevent process to start
    if (iShutdownNowInProgress) {
      t.stop();     // we just stop the target-thread for this Runnable
      // to be on the safe side, use try-catch
      try {
	super.beforeExecute(t,r);
      } catch (Exception e) {
      }
      return;
    }
    if (r instanceof ProcessTask) {
      ProcessTask pt = (ProcessTask) r;
      pt.getProcessStarter().addProcessEventListener(this);
    }
    super.beforeExecute(t,r);
  }

  ////////////////////////////////////////////////////////////////////////////

  /**
     Deregister this ProcessExecutor as a {@link ProcessEventListener}.

     {@inheritDoc}
  */

  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r,t);
    if (r instanceof ProcessTask) {
      ProcessTask pt = (ProcessTask) r;
      pt.getProcessStarter().removeProcessEventListener(this);
    }
  }

  ////////////////////////////////////////////////////////////////////////////

  /**
     {@inheritDoc}
  */

  public void processInitiated(ProcessEvent pEvent) {
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     {@inheritDoc}

     This method is called at process startup. We keep track of processes
     so we can destroy them if necessary.
  */

  public void processStarted(ProcessEvent pEvent) {
    synchronized (iProcesses) {
      iProcesses.add(pEvent.getProcess());
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     {@inheritDoc}

     This method is called at normal or abnormal process termination.
  */

  public void processTerminated(ProcessEvent pEvent) {
    synchronized (iProcesses) {
      iProcesses.remove(pEvent.getProcess());
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     {@inheritDoc}
  */

  public List<Runnable> shutdownNow() {
    iShutdownNowInProgress=true;         // to prevent any processes to
    destroy();                           // start during call to destroy()
    return super.shutdownNow();
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
     Destroy all active processes.
  */

  public void destroy() {
    synchronized (iProcesses) {
      Iterator<Process> it = iProcesses.iterator();
      while (it.hasNext()) {
	it.next().destroy();
      }
    }
  }
}
