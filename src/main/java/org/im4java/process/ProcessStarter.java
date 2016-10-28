/**************************************************************************
 /* This class implements the processing of os-commands using Runtime.exec()
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 This class implements the processing of os-commands using a
 ProcessBuilder.

 <p>
 This is the core class of the im4java-library where all the
 magic takes place. It does add some overhead compared to a
 direct call of ProcessBuilder, but you gain additional features
 like piping and asynchronous execution.
 </p>

 @version $Revision: 1.38 $
 @author  $Author: bablokb $

 @since 0.95
 */

public class ProcessStarter {

  //////////////////////////////////////////////////////////////////////////////

  /**
   Buffer size of process input-stream (used for reading the
   output (sic!) of the process). Currently 64KB.
   */

  public static final int BUFFER_SIZE=65536;

  //////////////////////////////////////////////////////////////////////////////

  /**
   Static global search path for executables.
   */

  private static String iGlobalSearchPath = null;

  //////////////////////////////////////////////////////////////////////////////

  /**
   Per instance search path for executables.
   */

  private String iSearchPath = null;

  //////////////////////////////////////////////////////////////////////////////

  /**
   The value of the global process-id counter.
   */

  private static AtomicInteger iPIDCounter = new AtomicInteger(0);

  //////////////////////////////////////////////////////////////////////////////

  /**
   The value of the process-id of the ProcessStarter.
   */

  private int iPID;

  //////////////////////////////////////////////////////////////////////////////

  /**
   The InputProvider for the ProcessStarter (if used as a pipe).
   */

  private InputProvider iInputProvider = null;

  //////////////////////////////////////////////////////////////////////////////

  /**
   The OutputConsumer for the ProcessStarter (if used as a pipe).
   */

  private OutputConsumer iOutputConsumer = null;

  //////////////////////////////////////////////////////////////////////////////

  /**
   The ErrorConsumer for the stderr of the ProcessStarter.
   */

  private ErrorConsumer iErrorConsumer = null;


  ////////////////////////////////////////////////////////////////////////////

  /**
   Execution-mode. If true, run asynchronously.
   */

  private boolean iAsyncMode = false;

  ////////////////////////////////////////////////////////////////////////////

  /**
   The ProcessListeners for this ProcessStarter.

   <p>
   This field is for compatibility only and will be removed in future
   versions.
   </p>
   */

  @SuppressWarnings("deprecation")
  private LinkedList<ProcessListener> iProcessListener;

  ////////////////////////////////////////////////////////////////////////////

  /**
   The ProcessEventListeners for this ProcessStarter.
   */
  private LinkedList<ProcessEventListener> iProcessEventListener;

  //////////////////////////////////////////////////////////////////////////////

  /**
   Static initializer
   */

  static {
    iGlobalSearchPath=System.getenv("IM4JAVA_TOOLPATH");
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Constructor.
   */

  @SuppressWarnings("deprecation")
  public ProcessStarter() {
    iProcessListener = new LinkedList<ProcessListener>();
    iProcessEventListener = new LinkedList<ProcessEventListener>();
    iPID = iPIDCounter.getAndAdd(1);
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Set the InputProvider for the ProcessStarter (if used as a pipe).
   */

  public void setInputProvider(InputProvider pInputProvider) {
    iInputProvider = pInputProvider;
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Set the OutputConsumer for the ProcessStarter (if used as a pipe).
   */

  public void setOutputConsumer(OutputConsumer pOutputConsumer) {
    iOutputConsumer = pOutputConsumer;
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Set the ErrorConsumer for the stderr of the ProcessStarter.
   */

  public void setErrorConsumer(ErrorConsumer pErrorConsumer) {
    iErrorConsumer = pErrorConsumer;
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Add a ProcessEventListener to this ProcessStarter.

   @param pListener the ProcessEventListener to add

   */

  public void addProcessEventListener(ProcessEventListener pListener) {
    iProcessEventListener.add(pListener);
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Remove a ProcessEventListener from this ProcessStarter.

   @param pListener the ProcessEventListener to remove

   */

  public void removeProcessEventListener(ProcessEventListener pListener) {
    iProcessEventListener.remove(pListener);
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Add a ProcessListener to this ProcessStarter.

   @param pProcessListener the ProcessListener to add

   @deprecated use {@link #addProcessEventListener} instead
   */
  public void addProcessListener(ProcessListener pProcessListener) {
    iProcessListener.add(pProcessListener);
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Pipe input to the command. This is done asynchronously.
   */

  private void processInput(OutputStream pOutputStream) throws IOException {
    final BufferedOutputStream bos =
            new BufferedOutputStream(pOutputStream,BUFFER_SIZE);
    (new Thread() {
      public void run() {
        try {
          iInputProvider.provideInput(bos);
        } catch (IOException iex) {
          // we do nothing, since we are in an asynchronous thread anyway
        }
      }
    }).run();
    bos.close();
    if (pOutputStream != null) {
      pOutputStream.close();
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Let the OutputConsumer process the output of the command.
   */

  private void processOutput(InputStream pInputStream,
                             OutputConsumer pConsumer) throws IOException{
    BufferedInputStream bis = new BufferedInputStream(pInputStream,BUFFER_SIZE);
    pConsumer.consumeOutput(bis);
    bis.close();
    if (pInputStream != null) {
      pInputStream.close();
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Let the ErrorConsumer process the stderr-stream.
   */

  private void processError(InputStream pInputStream,
                            ErrorConsumer pConsumer) throws IOException{
    BufferedInputStream bis = new BufferedInputStream(pInputStream,BUFFER_SIZE);
    pConsumer.consumeError(bis);
    bis.close();
    if (pInputStream != null) {
      pInputStream.close();
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Execute the command.

   @param pArgs         arguments for ProcessBuilder
   */

  public int run(List<String> pArgs)
          throws IOException, InterruptedException, Exception {

    // create and execute process (synchronous mode)
    if (! iAsyncMode) {
      Process pr = startProcess(pArgs);
      int rc = waitForProcess(pr);
      finished(rc);
      return rc;
    } else {
      ProcessTask pt = getProcessTask(pArgs);
      (new Thread(pt)).start();
      return 0;
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Return a ProcessTask for future execution.

   @param pArgs         arguments for ProcessBuilder
   */

  protected ProcessTask getProcessTask(List<String> pArgs)  {
    // prepare ProcessEvent and call processInitiated
    final ProcessEvent pe = new ProcessEvent(iPID,this);
    pe.setReturnCode(-1);
    for (ProcessEventListener pel:iProcessEventListener) {
      pel.processInitiated(pe);
    }

    // create ProcessTask for future execution
    return new ProcessTask(this,pArgs,pe);
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Run the process and notify all listeners.

   @param pArgs         arguments for ProcessBuilder
   @param pProcessEvent store state in this ProcessEvent
   */

  @SuppressWarnings("deprecation")
  void runAndNotify(List<String> pArgs, ProcessEvent pProcessEvent) {
    int rc;
    try {
      Process pr = startProcess(pArgs);
      pProcessEvent.setProcess(pr);
      for (ProcessEventListener pel:iProcessEventListener) {
        pel.processStarted(pProcessEvent);
      }
      // TODO: remove in future version
      for (ProcessListener pl:iProcessListener) {
        pl.processStarted(pr);
      }
      rc = waitForProcess(pr);
      pProcessEvent.setReturnCode(rc);
      finished(rc);
    } catch (Exception e) {
      pProcessEvent.setException(e);
      try {
        finished(e);
      } catch (Exception e2) {
        pProcessEvent.setException(e2);
      }
    }
    for (ProcessEventListener pel:iProcessEventListener) {
      pel.processTerminated(pProcessEvent);
    }
    // TODO: remove in future version
    for (ProcessListener pl:iProcessListener) {
      pl.processTerminated(pProcessEvent);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Execute the command.
   */

  private Process startProcess(List<String> pArgs)
          throws IOException, InterruptedException {

    // if a global or per object search path is set, resolve the
    // the executable

    if (iSearchPath != null) {
      String cmd = pArgs.get(0);
      cmd = searchForCmd(cmd,iSearchPath);
      pArgs.set(0,cmd);
    } else if (iGlobalSearchPath != null) {
      String cmd = pArgs.get(0);
      cmd = searchForCmd(cmd,iGlobalSearchPath);
      pArgs.set(0,cmd);
    }
    ProcessBuilder builder = new ProcessBuilder(pArgs);
    return builder.start();
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   Perform process input/output and wait for process to terminate.
   */

  private int waitForProcess(final Process pProcess)
          throws IOException, InterruptedException {

    FutureTask<Object> outTask = null;
    FutureTask<Object> errTask = null;

    if (iInputProvider != null) {
      processInput(pProcess.getOutputStream());
    }

    // Process stdout and stderr of subprocess in parallel.
    // This prevents deadlock under Windows, if there is a lot of
    // stderr-output (e.g. from ghostscript called by convert)

    if (iOutputConsumer != null) {
      outTask = new FutureTask<Object>(new Callable<Object>() {
        public Object call() throws IOException {
          processOutput(pProcess.getInputStream(), iOutputConsumer);
          return null;
        }
      });
      new Thread(outTask).start();
    }
    if (iErrorConsumer != null) {
      errTask = new FutureTask<Object>(new Callable<Object>() {
        public Object call() throws IOException {
          processError(pProcess.getErrorStream(), iErrorConsumer);
          return null;
        }
      });
      new Thread(errTask).start();
    }

    // Wait and check IO exceptions (FutureTask.get() blocks).
    try {
      if (outTask != null) {
        outTask.get();
      }
      if (errTask != null) {
        errTask.get();
      }
    } catch (ExecutionException e) {
      Throwable t = e.getCause();

      if (t instanceof IOException) {
        throw (IOException) t;
      } else if(t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else {
        throw new IllegalStateException(e);
      }
    }

    pProcess.waitFor();
    int rc=pProcess.exitValue();

    // just to be on the safe side
    try {
      pProcess.getInputStream().close();
      pProcess.getOutputStream().close();
      pProcess.getErrorStream().close();
    } catch (Exception e) {
    }
    return rc;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Set the async-execution mode.

   @param pAsyncMode the iAsyncMode to set
   */
  public void setAsyncMode(boolean pAsyncMode) {
    iAsyncMode = pAsyncMode;
  }

  ////////////////////////////////////////////////////////////////////////////

  /**
   Query the async-execution mode.

   @return the iAsyncMode
   */
  public boolean isAsyncMode() {
    return iAsyncMode;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Set the global (static) search path. You can override this search path
   on a per object basis.

   @param pGlobalSearchPath the global search path
   */

  public static void setGlobalSearchPath(String pGlobalSearchPath) {
    iGlobalSearchPath = pGlobalSearchPath;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Query the global (static) search path.
   */

  public static String getGlobalSearchPath() {
    return iGlobalSearchPath;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Set the per object search path. This overrides the global search path
   (if set).

   @param pSearchPath the search path
   */

  public void setSearchPath(String pSearchPath) {
    iSearchPath = pSearchPath;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Query the per object search path.
   */

  public String getSearchPath() {
    return iSearchPath;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Set the process-id counter of the class.

   @param pPID the process-id
   */

  public static void setPIDCounter(int pPID) {
    iPIDCounter.set(pPID);
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Set the process-id of this ProcessStarter.

   @param pPID the process-id
   */

  public void setPID(int pPID) {
    iPID = pPID;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Query the process-id.
   */

  public int getPID() {
    return iPID;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Post-processing after the process has terminated. Subclasses might
   override this method to do some specific post-processing.

   @param pReturnCode  the return-code of the process
   */

  protected void finished(int pReturnCode) throws Exception {
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Post-processing after the process has terminated with an
   exception. Subclasses might override this method to do some
   specific post-processing. This method is only called in
   asynchronous execution mode (in synchronous mode, the exception
   just propagates as usual to the caller).

   <p>Note that if this method throws an exception in asynchronous
   execution mode, the original exception is lost and not propagated
   to any ProcessEventListeners. Therefore, you should take care to fill
   in any exception and stack-trace information.
   </p>

   @param pException  the exception of the process
   */

  protected void finished(Exception pException) throws Exception {
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   Query the per object search path.

   @param pCmd  the  command to search for
   @param pPath the  search path
   */

  public String searchForCmd(String pCmd, String pPath)
          throws IOException, FileNotFoundException {
    // check is pCmd is absolute
    if ((new File(pCmd)).isAbsolute()) {
      return pCmd;
    }

    // special processing on windows-systems.
    // File.pathSeparator is hopefully more robust than 
    // System.getProperty("os.name") ?!
    boolean isWindows=File.pathSeparator.equals(";");

    String dirs[] = pPath.split(File.pathSeparator);
    for (int i=0; i<dirs.length; ++i) {
      if (isWindows) {
        // try thre typical extensions
        File cmd = new File(dirs[i],pCmd+".exe");
        if (cmd.exists()) {
          return cmd.getCanonicalPath();
        }
        cmd = new File(dirs[i],pCmd+".cmd");
        if (cmd.exists()) {
          return cmd.getCanonicalPath();
        }
        cmd = new File(dirs[i],pCmd+".bat");
        if (cmd.exists()) {
          return cmd.getCanonicalPath();
        }
      } else {
        File cmd = new File(dirs[i],pCmd);
        if (cmd.exists()) {
          return cmd.getCanonicalPath();
        }
      }
    }
    throw new FileNotFoundException(pCmd);
  }
}
