package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.request.Identifier;
import edu.illinois.library.cantaloupe.request.Parameters;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface to be implemented by all caches. Instances will be shared
 * Singletons.
 */
public interface Cache {

    /**
     * Deletes the entire cache contents. Must be thread-safe.
     *
     * @throws IOException
     */
    void flush() throws IOException;

    /**
     * Deletes the cached image and dimensions corresponding to the given
     * parameters.
     *
     * @param params
     * @throws IOException
     */
    void flush(Parameters params) throws IOException;

    /**
     * Deletes expired images and dimensions from the cache.
     *
     * @throws IOException
     */
    void flushExpired() throws IOException;

    /**
     * @param params IIIF request parameters
     * @return An input stream corresponding to the given parameters, or null
     * if a non-expired image corresponding to the given parameters does not
     * exist in the cache.
     */
    InputStream getImageInputStream(Parameters params);

    /**
     * Reads cached dimension information.
     *
     * @param identifier IIIF identifier
     * @return Dimension corresponding to the given identifier, or null if no
     * non-expired dimension exists in the cache.
     */
    Dimension getDimension(Identifier identifier) throws IOException;

    /**
     * @param params IIIF request parameters
     * @return OutputStream pointed at the cache to which an image
     * corresponding to the supplied parameters can be written.
     * @throws IOException
     */
    OutputStream getImageOutputStream(Parameters params) throws IOException;

    /**
     * Adds an image's dimension information to the cache.
     *
     * @param identifier IIIF identifier
     * @param size Dimension containing width and height in pixels.
     */
    void putDimension(Identifier identifier, Dimension size) throws IOException;

}
