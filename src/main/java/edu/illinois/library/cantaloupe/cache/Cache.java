package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.io.IOException;

/**
 * <p>Interface to be implemented by all caches. A cache stores and retrieves
 * unique images corresponding to {@link OperationList} objects, as well as
 * {@link Info} objects corresponding to {@link Identifier} objects.</p>
 *
 * <p>Implementations must be thread-safe.</p>
 */
public interface Cache {

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
     * <p>The default implementation does nothing.</p>
     *
     * @throws IOException
     * @see #shutdown()
     */
    default void cleanUp() throws IOException {}

    /**
     * <p>Implementations should perform all necessary initialization in this
     * method rather than a constructor or static initializer.</p>
     *
     * <p>The default implementation does nothing.</p>
     */
    default void initialize() {}

    /**
     * Deletes the entire cache contents.
     *
     * @throws IOException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge() throws IOException;

    /**
     * Deletes all cached content (source image, derivative image(s), and
     * info) corresponding to the image with the given identifier.
     *
     * @param identifier
     * @throws IOException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge(Identifier identifier) throws IOException;

    /**
     * Deletes expired images and dimensions from the cache.
     *
     * @throws IOException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purgeExpired() throws IOException;

    /**
     * <p>Shuts down the instance, freeing any resource handles, stopping any
     * worker threads, etc.</p>
     *
     * <p>The default implementation does nothing.</p>
     *
     * @see #cleanUp()
     */
    default void shutdown() {}

}
