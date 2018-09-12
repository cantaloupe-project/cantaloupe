package edu.illinois.library.cantaloupe.process;

import edu.illinois.library.cantaloupe.async.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Wraps a {@link ProcessBuilder}.
 */
public final class ProcessStarter {

    /**
     * Buffer size of process input-stream (used for reading the
     * output (sic!) of the process). Currently 64KB.
     */
    private static final int BUFFER_SIZE = 65536;

    private InputProvider inputProvider = null;
    private OutputConsumer outputConsumer = null;
    private ErrorConsumer errorConsumer = null;

    /**
     * Pipe input to the command. This is done asynchronously.
     */
    private void processInput(OutputStream processOutputStream)
            throws IOException {
        final BufferedOutputStream bos =
                new BufferedOutputStream(processOutputStream, BUFFER_SIZE);
        try {
            inputProvider.provideInput(bos);
        } finally {
            try {
                bos.close();
            } finally {
                processOutputStream.close();
            }
        }
    }

    private void processOutput(InputStream processInputStream,
                               OutputConsumer processConsumer)
            throws IOException {
        BufferedInputStream bis =
                new BufferedInputStream(processInputStream, BUFFER_SIZE);
        processConsumer.consumeOutput(bis);
        try {
            bis.close();
        } finally {
            processInputStream.close();
        }
    }

    private void processError(InputStream processInputStream,
                              ErrorConsumer processConsumer)
            throws IOException {
        BufferedInputStream bis =
                new BufferedInputStream(processInputStream, BUFFER_SIZE);
        processConsumer.consumeError(bis);
        try {
            bis.close();
        } finally {
            processInputStream.close();
        }
    }

    /**
     * Invokes the command.
     *
     * @param args Command arguments.
     */
    public int run(List<String> args) throws Exception {
        Process pr = startProcess(args);
        return waitForProcess(pr);
    }

    /**
     * Sets the InputProvider (if used as a pipe).
     */
    public void setInputProvider(InputProvider inputProvider) {
        this.inputProvider = inputProvider;
    }

    /**
     * Sets the OutputConsumer (if used as a pipe).
     */
    public void setOutputConsumer(OutputConsumer outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    /**
     * Sets the ErrorConsumer (if used as a pipe).
     */
    public void setErrorConsumer(ErrorConsumer errorConsumer) {
        this.errorConsumer = errorConsumer;
    }

    /**
     * Invokes the command.
     */
    private Process startProcess(List<String> pArgs) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(pArgs);
        return builder.start();
    }

    /**
     * Performs process input/output and waits for process to terminate.
     */
    private int waitForProcess(final Process process)
            throws IOException, InterruptedException {
        FutureTask<Object> outTask = null;
        FutureTask<Object> errTask = null;

        if (inputProvider != null) {
            processInput(process.getOutputStream());
        }

        // Process stdout and stderr of subprocess in parallel. This prevents
        // deadlock under Windows, if there is a lot of stderr output.

        if (outputConsumer != null) {
            outTask = new FutureTask<>(() -> {
                processOutput(process.getInputStream(), outputConsumer);
                return null;
            });
            ThreadPool.getInstance().submit(outTask);
        }
        if (errorConsumer != null) {
            errTask = new FutureTask<>(() -> {
                processError(process.getErrorStream(), errorConsumer);
                return null;
            });
            ThreadPool.getInstance().submit(errTask);
        }

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
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new IllegalStateException(e);
            }
        }

        process.waitFor();
        int exitValue = process.exitValue();

        try {
            process.getInputStream().close();
        } finally {
            try {
                process.getOutputStream().close();
            } finally {
                process.getErrorStream().close();
            }
        }
        return exitValue;
    }

}
