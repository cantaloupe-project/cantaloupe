package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>Interface to be implemented by all caches. A cache stores and retrieves
 * unique images corresponding to
 * {@link edu.illinois.library.cantaloupe.image.OperationList} objects, as
 * well as {@link ImageInfo} objects corresponding to
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
     * wish--ideally, they will not need to do anything at all.</p>
     *
     * <p>The frequency with which this method will be called may vary.
     * It may never be called. Implementations should try to keep themselves
     * clean without relying on this method.</p>
     *
     * @throws CacheException
     */
    void cleanUp() throws CacheException;

    /**
     * <p>Reads cached image information.</p>
     *
     * <p>If image information corresponding to the given identifier exists in
     * the cache but is expired, implementations should delete it.</p>
     *
     * @param identifier Image identifier for which to retrieve information.
     * @return ImageInfo corresponding to the given identifier, or null if no
     * non-expired info exists in the cache.
     * @throws CacheException
     */
    ImageInfo getImageInfo(Identifier identifier) throws CacheException;

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
     * @throws CacheException
     */
    InputStream getImageInputStream(OperationList opList) throws CacheException;

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
     * @throws CacheException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge() throws CacheException;

    /**
     * Deletes all cached content corresponding to the image with the given
     * identifier.
     *
     * @param identifier
     * @throws CacheException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge(Identifier identifier) throws CacheException;

    /**
     * Deletes the cached image corresponding to the given operation list.
     *
     * @param opList
     * @throws CacheException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge(OperationList opList) throws CacheException;

    /**
     * Deletes expired images and dimensions from the cache.
     *
     * @throws CacheException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purgeExpired() throws CacheException;

    /**
     * Adds image information to the cache. If the writing of the dimension is
     * interrupted, implementations should clean it up, if necessary.
     *
     * @param identifier Identifier of the image corresponding to the given
     *                   size.
     * @param imageInfo ImageInfo containing image information.
     * @throws CacheException
     */
    void putImageInfo(Identifier identifier, ImageInfo imageInfo)
            throws CacheException;

}
