package edu.illinois.library.cantaloupe.process;

import java.io.InputStream;
import java.io.IOException;

/**
 * Reads output from a process' stdout.
 */
interface OutputConsumer {

    /**
     * Reads the output of a process from the given {@link InputStream}.
     */
    void consumeOutput(InputStream processInputStream) throws IOException;

}
