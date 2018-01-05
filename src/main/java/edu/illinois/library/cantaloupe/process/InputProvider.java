package edu.illinois.library.cantaloupe.process;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Supplies input to a process.
 */
interface InputProvider {

    /**
     * The InputProvider must write the input to the given OutputStream.
     */
    void provideInput(OutputStream pOutputStream) throws IOException;

}
