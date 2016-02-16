package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;

import java.awt.Dimension;
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
     * <p>Cleans up the cache.</p>
     *
     * <p>This method should <strong>not</strong> purge any content. Other
     * than that, implementations may interpret "clean up" however they
     * wish.</p>
     *
     * <p>The frequency with which this method will be called may vary.
     * It may never be called. Implementations should try to keep themselves
     * "clean" without relying on this method.</p>
     *
     * @throws CacheException
     */
    void cleanUp() throws CacheException;

    /**
     * <p>Reads cached dimension information.</p>
     *
     * <p>If a dimension corresponding to the given identifier exists in the
     * cache but is expired, implementations should delete it.</p>
     *
     * @param identifier Image identifier for which to retrieve a dimension.
     * @return Dimension corresponding to the given identifier, or null if no
     * non-expired dimension exists in the cache.
     * @throws CacheException
     */
    Dimension getDimension(Identifier identifier) throws CacheException;

    /**
     * <p>Returns an input stream corresponding to the given OperationList,
     * or null if a non-expired image corresponding to the given operation
     * list does not exist in the cache..</p>
     *
     * <p>If an image corresponding to the given parameters exists in the
     * cache but is expired, implementations should delete it.</p>
     *
     * @param opList Operation list for which to retrieve an input stream for
     *               reading from the cache.
     * @return Input stream corresponding to the given operation list, or null
     *         if a non-expired image corresponding to the given operation
     *         list does not exist in the cache.
     */
    InputStream getImageInputStream(OperationList opList);

    /**
     * @param opList Operation list for which to retrieve an output stream for
     *               writing to the cache.
     * @return Output stream to which an image corresponding to the given
     *         operation list can be written.
     * @throws CacheException
     */
    OutputStream getImageOutputStream(OperationList opList)
            throws CacheException;

    /**
     * Deletes the entire cache contents.
     *
     * @throws CacheException If the cache is misconfigured or there is some
     * other error.
     * @throws CacheException
     */
    void purge() throws CacheException;

    /**
     * Deletes all cached content corresponding to the image with the given
     * identifier.
     *
     * @param identifier
     * @throws CacheException
     */
    void purge(Identifier identifier) throws CacheException;

    /**
     * Deletes the cached image corresponding to the given operation list.
     *
     * @param opList
     * @throws CacheException
     */
    void purge(OperationList opList) throws CacheException;

    /**
     * Deletes expired images and dimensions from the cache.
     *
     * @throws CacheException
     */
    void purgeExpired() throws CacheException;

    /**
     * Adds an image's dimension information to the cache. If the writing of
     * the dimension is interrupted, implementations should clean it up, if
     * necessary.
     *
     * @param identifier Identifier of the image corresponding to the given
     *                   size.
     * @param size Dimension containing width and height in pixels.
     * @throws CacheException
     */
    void putDimension(Identifier identifier, Dimension size)
            throws CacheException;

}
