/**
 * <p>Contains source and derivative image caches.</p>
 *
 * <h3>Writing Custom Caches</h3>
 *
 * <p>Custom derivative caches must implement
 * {@link edu.illinois.library.cantaloupe.cache.DerivativeCache}. An instance
 * of the cache will be shared across threads, so implementations must be
 * thread-safe.</p>
 *
 * <p>Once the custom cache is written, the
 * {@link edu.illinois.library.cantaloupe.cache.CacheFactory#getAllDerivativeCaches()}
 * method must be modified to include it. Then, it can be used like any other
 * derivative cache.</p>
 */
package edu.illinois.library.cantaloupe.cache;
