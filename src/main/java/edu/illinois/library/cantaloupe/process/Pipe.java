package edu.illinois.library.cantaloupe.process;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * <p>Implements a pipe. Useful for piping input to a process or piping
 * output/error from a process to other streams.</p>
 *
 * <p>The same Pipe instance can be used at both ends of a process pipeline,
 * but cannot be used as both an {@link OutputConsumer} and
 * {@link ErrorConsumer} simultaneously.</p>
 */
public class Pipe implements InputProvider, OutputConsumer, ErrorConsumer {

    /**
     * Default buffer size of the pipe, in bytes.
     */
    private static final int BUFFER_SIZE = 65536;

    /**
     * Source of data (i.e. this pipe will provide input for a process).
     */
    private InputStream sourceStream;

    /**
     * Sink for data (i.e. this pipe will consume output of a process).
     */
    private OutputStream sinkStream;

    /**
     * N.B.: At least one of the arguments should not be {@literal null}.
     */
    public Pipe(InputStream source, OutputStream sink) {
        sourceStream = source;
        sinkStream = sink;
    }

    /**
     * Writes the input to the given {@link OutputStream}.
     */
    public void provideInput(OutputStream processOutputStream)
            throws IOException {
        copyBytes(sourceStream, processOutputStream);
    }

    /**
     * Reads the output of a process from the given {@link InputStream}.
     */
    public void consumeOutput(InputStream processInputStream)
            throws IOException {
        if (sinkStream != null) {
            copyBytes(processInputStream, sinkStream);
        }
    }

    /**
     * Reads the error of a process from the given {@link InputStream}.
     */
    public void consumeError(InputStream processInputStream)
            throws IOException {
        if (sinkStream != null) {
            copyBytes(processInputStream, sinkStream);
        }
    }

    /**
     * Reads bytes from an {@link InputStream} and writes them to an
     * {@link OutputStream}.
     */
    private void copyBytes(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            int byteCount = is.read(buffer);
            if (byteCount == -1) {
                break;
            }
            synchronized (os) {
                os.write(buffer, 0, byteCount);
            }
        }
        os.flush();
    }

}
