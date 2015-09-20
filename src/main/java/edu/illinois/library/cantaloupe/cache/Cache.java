package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.request.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Cache {

    /**
     * @param params Request parameters
     * @return An input stream corresponding to the given parameters, or null
     * if a non-expired image corresponding to the given parameters does not
     * exist in the cache.
     */
    InputStream get(Parameters params);

    /**
     * @param params Request parameters
     * @return An OutputStream pointed at the cache
     * @throws IOException
     */
    OutputStream getOutputStream(Parameters params) throws IOException;

}
