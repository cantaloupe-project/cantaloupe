/**
 * <p>Package relating to all aspects of caching of images and their
 * characteristics.</p>
 *
 * <p>There are three main kinds of caches:</p>
 *
 * <ul>
 *     <li>{@link edu.illinois.library.cantaloupe.cache.SourceCache Source
 *     caches} cache source images on the filesystem.</li>
 *     <li>{@link edu.illinois.library.cantaloupe.cache.DerivativeCache
 *     Derivative caches} cache
 *     {@link edu.illinois.library.cantaloupe.image.Info image information} and
 *     post-processed (derivative) images.</li>
 *     <li>The {@link edu.illinois.library.cantaloupe.cache.InfoCache info
 *     cache} caches image information in the heap, and may be used either on
 *     its own, or as a faster "level 1" cache in front of a derivative
 *     cache.</li>
 * </ul>
 *
 * <p>Clients are encouraged to use a
 * {@link edu.illinois.library.cantaloupe.cache.CacheFacade} to simplify
 * interactions with the caching architecture.</p>
 *
 * <h1>Writing Custom Caches</h1>
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
