/**
 * <p>Package relating to all aspects of caching of images and their
 * characteristics.</p>
 *
 * <p>There are two main kinds of caches&mdash;source and
 * derivative&mdash;defined by
 * {@link edu.illinois.library.cantaloupe.cache.SourceCache} and
 * {@link edu.illinois.library.cantaloupe.cache.DerivativeCache}. Source caches
 * cache source images on the filesystem. Derivative caches cache
 * {@link edu.illinois.library.cantaloupe.image.Info image properties} and
 * post-processed images.</p>
 *
 * <p>Additionally, {@link edu.illinois.library.cantaloupe.cache.InfoService}
 * uses an in-memory {@link edu.illinois.library.cantaloupe.util.ObjectCache}
 * as a "level 1" cache for {@link edu.illinois.library.cantaloupe.image.Info}
 * instances.</p>
 *
 * <p>In light of all of the above, clients are encouraged to use a
 * {@link edu.illinois.library.cantaloupe.cache.CacheFacade} to simplify
 * interactions with the caching architecture.</p>
 *
 * <h3>Writing Custom Caches</h3>
 *
 * <p>Custom derivative caches must implement
 * {@link edu.illinois.library.cantaloupe.cache.DerivativeCache}. A single
 * instance of the cache will be shared across threads, so implementations must
 * be thread-safe.</p>
 *
 * <p>Once the custom cache is written, the
 * {@link edu.illinois.library.cantaloupe.cache.CacheFactory#getAllDerivativeCaches()}
 * method must be modified to return it. Then, it can be used like any other
 * derivative cache.</p>
 */
package edu.illinois.library.cantaloupe.cache;
