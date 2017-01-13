package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.ImageInfo;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>Interface to be implemented by cache that cache derivative images and
 * metadata.</p>
 *
 * <p>Implementations must be thread-safe.</p>
 */
public interface DerivativeCache extends Cache {

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
     * <p>If an image corresponding to the given list exists in the cache but
     * is expired, implementations should delete it before returning null.</p>
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
     * Deletes the cached image corresponding to the given operation list.
     *
     * @param opList
     * @throws CacheException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge(OperationList opList) throws CacheException;

    /**
     * <p>Adds image information to the cache.</p>
     *
     * <p>If the information corresponding to the given identifier already
     * exists, it should be overwritten.</p>
     *
     * <p>If writing is interrupted, implementations should perform cleanup,
     * if necessary.</p>
     *
     * @param identifier Image identifier.
     * @param imageInfo ImageInfo containing information about the image with
     *                  the given identifier.
     * @throws CacheException
     */
    void put(Identifier identifier, ImageInfo imageInfo) throws CacheException;

}
