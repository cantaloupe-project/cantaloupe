package edu.illinois.library.cantaloupe.process;

import java.io.IOException;
import java.io.InputStream;

/**
 * Saves output to an {@link java.util.ArrayList}.
 */
public final class ArrayListOutputConsumer extends ArrayListConsumer
        implements OutputConsumer {

    /**
     * Reads command output and stores it internally.
     */
    public void consumeOutput(InputStream processInputStream)
            throws IOException {
        consume(processInputStream);
    }

}
