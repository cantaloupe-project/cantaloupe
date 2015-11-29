package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface to be implemented by all caches. Instances will be shared
 * Singletons, so must be thread-safe.
 */
public interface Cache {

    /**
     * Deletes the entire cache contents.
     *
     * @throws IOException
     */
    void flush() throws IOException;

    /**
     * Deletes the cached image and dimensions corresponding to the given
     * parameters.
     *
     * @param ops
     * @throws IOException
     */
    void flush(OperationList ops) throws IOException;

    /**
     * Deletes expired images and dimensions from the cache.
     *
     * @throws IOException
     */
    void flushExpired() throws IOException;

    /**
     * <p>Returns an InputStream corresponding to the given parameters.</p>
     *
     * <p>If an image corresponding to the given parameters exists in the
     * cache but is expired, implementations should delete it.</p>
     *
     * @param params IIIF request parameters
     * @return An input stream corresponding to the given parameters, or null
     * if a non-expired image corresponding to the given parameters does not
     * exist in the cache.
     */
    InputStream getImageInputStream(OperationList params);

    /**
     * <p>Reads cached dimension information.</p>
     *
     * <p>If a dimension corresponding to the given parameters exists in the
     * cache but is expired, implementations should delete it.</p>
     *
     * @param identifier IIIF identifier
     * @return Dimension corresponding to the given identifier, or null if no
     * non-expired dimension exists in the cache.
     */
    Dimension getDimension(Identifier identifier) throws IOException;

    /**
     * @param ops IIIF request parameters
     * @return OutputStream to which an image corresponding to the supplied
     * parameters can be written.
     * @throws IOException
     */
    OutputStream getImageOutputStream(OperationList ops) throws IOException;

    /**
     * Adds an image's dimension information to the cache.
     *
     * @param identifier IIIF identifier
     * @param size Dimension containing width and height in pixels.
     */
    void putDimension(Identifier identifier, Dimension size) throws IOException;

}
