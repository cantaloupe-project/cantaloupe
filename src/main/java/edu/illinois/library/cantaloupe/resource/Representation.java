package edu.illinois.library.cantaloupe.resource;

import java.io.IOException;
import java.io.OutputStream;

/**
 * HTTP response representation a.k.a. body.
 */
public interface Representation {

    /**
     * Writes some kind of data (image data, HTML, JSON, etc.) to the given
     * response output stream for transmission back to the client.
     */
    void write(OutputStream outputStream) throws IOException;

}
