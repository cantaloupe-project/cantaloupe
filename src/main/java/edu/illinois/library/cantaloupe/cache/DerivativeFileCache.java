package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;

/**
 * <p>Interface to be implemented by derivative caches that cache derivative
 * images and metadata in files, one file per image or metadata.</p>
 */
public interface DerivativeFileCache extends DerivativeCache {

    /**
     * @return Whether a non-expired image corresponding to the given
     *         OperationList exists in the cache.
     * @throws CacheException
     */
    boolean derivativeImageExists(OperationList opList) throws CacheException;

    /**
     * @return Whether a non-expired info corresponding to the given
     *         identifier exists in the cache.
     * @throws CacheException
     */
    boolean infoExists(Identifier identifier) throws CacheException;

    /**
     * @param identifier Identifier describing the info to retrieve a
     *                   pathname for.
     * @return Pathname of the info corresponding to the given identifier,
     *         relative to the root of the cache storage.
     */
    String getRelativePathname(Identifier identifier);

    /**
     * @param opList Operation list describing the image to retrieve a
     *               pathname for.
     * @return Pathname of the image corresponding to the given operation list,
     *         relative to the root of the cache storage.
     */
    String getRelativePathname(OperationList opList);

}
