package edu.illinois.library.cantaloupe.process;

import java.io.InputStream;
import java.io.IOException;

/**
 * Reads output from a process' stderr.
 */
interface ErrorConsumer {

    /**
     * Reads the output of a process from the given {@link InputStream}.
     */
    void consumeError(InputStream pInputStream) throws IOException;

}
