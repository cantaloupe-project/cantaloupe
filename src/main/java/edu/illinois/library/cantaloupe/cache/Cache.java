package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.ImageInfo;

/**
 * <p>Interface to be implemented by all caches. A cache stores and retrieves
 * unique images corresponding to {@link OperationList} objects, as well as
 * {@link ImageInfo} objects corresponding to {@link Identifier} objects.</p>
 *
 * <p>Implementations must be thread-safe.</p>
 */
public interface Cache {

    String PURGE_MISSING_CONFIG_KEY = "cache.server.purge_missing";
    String RESOLVE_FIRST_CONFIG_KEY = "cache.server.resolve_first";
    String TTL_CONFIG_KEY = "cache.server.ttl_seconds";

    /**
     * <p>Cleans up the cache.</p>
     *
     * <p>This method should <strong>not</strong> duplicate the behavior of
     * any of the purging-related methods. Other than that, implementations
     * may interpret "clean up" however they wish--ideally, they will not need
     * to do anything at all.</p>
     *
     * <p>The frequency with which this method will be called may vary. It may
     * never be called. Implementations should try to keep themselves clean
     * without relying on this method.</p>
     *
     * @throws CacheException
     */
    void cleanUp() throws CacheException;

    /**
     * Deletes the entire cache contents.
     *
     * @throws CacheException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge() throws CacheException;

    /**
     * Deletes expired images and dimensions from the cache.
     *
     * @throws CacheException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purgeExpired() throws CacheException;

    /**
     * Deletes all cached content (source image, derivative image(s), and
     * info) corresponding to the image with the given identifier.
     *
     * @param identifier
     * @throws CacheException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purgeImage(Identifier identifier) throws CacheException;

}
