package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>Interface to be implemented by all caches. A cache stores and retrieves
 * unique images corresponding to
 * {@link edu.illinois.library.cantaloupe.image.OperationList} objects, as
 * well as {@link java.awt.Dimension} objects corresponding to
 * {@link edu.illinois.library.cantaloupe.image.Identifier} objects.</p>
 *
 * <p>Implementations must be thread-safe.</p>
 */
public interface Cache {

    /**
     * <p>Reads cached dimension information.</p>
     *
     * <p>If a dimension corresponding to the given identifier exists in the
     * cache but is expired, implementations should delete it.</p>
     *
     * @param identifier Image identifier for which to retrieve a dimension.
     * @return Dimension corresponding to the given identifier, or null if no
     * non-expired dimension exists in the cache.
     */
    Dimension getDimension(Identifier identifier) throws IOException;

    /**
     * <p>Returns an InputStream corresponding to the given OperationList.</p>
     *
     * <p>If an image corresponding to the given parameters exists in the
     * cache but is expired, implementations should delete it.</p>
     *
     * @param opList Operation list for which to retrieve an input stream, for
     *               reading from the cache.
     * @return An input stream corresponding to the given parameters, or null
     * if a non-expired image corresponding to the given parameters does not
     * exist in the cache.
     */
    InputStream getImageInputStream(OperationList opList);

    /**
     * @param opList Operation list for which to retrieve an output stream, for
     *               writing to the cache.
     * @return OutputStream to which an image corresponding to the supplied
     * parameters can be written.
     * @throws IOException If an output stream cannot be returned for any
     * reason.
     */
    OutputStream getImageOutputStream(OperationList opList) throws IOException;

    /**
     * Deletes the entire cache contents.
     *
     * @throws IOException If any part of the process fails.
     */
    void purge() throws IOException;

    /**
     * Deletes all cached content corresponding to the image with the given
     * identifier.
     *
     * @param identifier
     * @throws IOException If any part of the process fails.
     */
    void purge(Identifier identifier) throws IOException;

    /**
     * Deletes the cached image corresponding to the given operation list.
     *
     * @param opList
     * @throws IOException If any part of the process fails.
     */
    void purge(OperationList opList) throws IOException;

    /**
     * Deletes expired images and dimensions from the cache.
     *
     * @throws IOException If any part of the process fails.
     */
    void purgeExpired() throws IOException;

    /**
     * Adds an image's dimension information to the cache.
     *
     * @param identifier Identifier of the image corresponding to the given
     *                   size.
     * @param size Dimension containing width and height in pixels.
     * @throws IOException If the dimension cannot be saved for any reason.
     */
    void putDimension(Identifier identifier, Dimension size) throws IOException;

}
