package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.request.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Cache {

    /**
     * Deletes the entire contents of the cache.
     *
     * @throws Exception
     */
    void flush() throws Exception;

    /**
     * Deletes expired images from the cache.
     * @throws Exception
     */
    void flushExpired() throws Exception;

    /**
     * @param params Request parameters
     * @return An input stream corresponding to the given parameters, or null
     * if a non-expired image corresponding to the given parameters does not
     * exist in the cache.
     */
    InputStream get(Parameters params);

    /**
     * @param params Request parameters
     * @return An OutputStream pointed at the cache to which an image
     * corresponding to the supplied parameters can be written.
     * @throws IOException
     */
    OutputStream getOutputStream(Parameters params) throws IOException;

}
