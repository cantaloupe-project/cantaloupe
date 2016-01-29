package edu.illinois.library.cantaloupe.resolver;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides input streams to read from.
 */
public interface StreamSource {

    /**
     * Provides a new input stream to read from. The returned class may be an
     * subclass of InputStream that can provide more efficient access.
     *
     * @return New input stream to read from.
     * @throws IOException
     */
    InputStream newStream() throws IOException;

}
