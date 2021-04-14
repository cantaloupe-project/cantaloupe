package edu.illinois.library.cantaloupe.cache;

import java.io.IOException;

/**
 * @since 4.1.9
 */
public class CompletableNullOutputStream extends CompletableOutputStream {

    @Override
    public void write(int b) throws IOException {
    }

}
