package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.util.ObjectCache;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * <p>Used to obtain {@link Info} instances in an efficient way, utilizing
 * (optionally) several tiers of caching.</p>
 *
 * @since 3.4
 */
public final class InfoService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InfoService.class);

    private static InfoService instance;

    private final InfoCache infoCache = new InfoCache();

    /**
     * For testing only!
     */
    static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * @return Shared instance.
     */
    public static synchronized InfoService getInstance() {
        if (instance == null) {
            instance = new InfoService();
        }
        return instance;
    }

    /**
     * <p>Returns an {@link Info} for the source image corresponding to the
     * given identifier.</p>
     *
     * <p>The following sources are consulted in order of preference:</p>
     *
     * <ol>
     *     <li>An internal {@link ObjectCache}, if enabled by the
     *     {@link Key#INFO_CACHE_ENABLED} configuration key</li>
     *     <li>The derivative cache returned by
     *     {@link CacheFactory#getDerivativeCache()}</li>
     * </ol>
     *
     * @param identifier   Identifier of the source image for which to retrieve
     *                     the info.
     * @return             Info for the image with the given identifier.
     * @throws IOException If there is an error reading or writing to or
     *                     from the cache.
     * @see #getOrReadInfo(Identifier, Processor)
     */
    Optional<Info> getInfo(final Identifier identifier) throws IOException {
        // Check the info cache.
        Info info = infoCache.get(identifier);
        if (info != null) {
            LOGGER.debug("getInfo(): retrieved from {}: {}",
                    infoCache.getClass().getSimpleName(), identifier);
            return Optional.of(info);
        }
        // Check the derivative cache.
        final DerivativeCache derivCache =
                CacheFactory.getDerivativeCache().orElse(null);
        if (derivCache != null) {
            Stopwatch watch = new Stopwatch();
            Optional<Info> optInfo = derivCache.getInfo(identifier);
            if (optInfo.isPresent()) {
                LOGGER.debug("getInfo(): retrieved info of {} from {} in {}",
                        identifier,
                        derivCache.getClass().getSimpleName(),
                        watch);
                // Add it to the object cache (where it may already exist,
                // but it doesn't matter).
                putInObjectCache(identifier, optInfo.get());
            }
            return optInfo;
        }
        return Optional.empty();
    }

    /**
     * @return The backing info cache.
     */
    public InfoCache getInfoCache() {
        return infoCache;
    }

    /**
     * <p>Returns an {@link Info} for the source image corresponding to the
     * given identifier.</p>
     *
     * <p>The following sources are consulted in order of preference:</p>
     *
     * <ol>
     *     <li>An internal {@link ObjectCache}, if enabled by the
     *     {@link Key#INFO_CACHE_ENABLED} configuration key;</li>
     *     <li>The derivative cache returned by
     *     {@link CacheFactory#getDerivativeCache()};</li>
     *     <li>The given processor. If this is the case, it will also be cached
     *     in whichever of the above caches are available. (This may happen
     *     asynchronously.)</li>
     * </ol>
     *
     * @param identifier Identifier of the source image for which to retrieve
     *                   the info.
     * @param proc       Processor to use to read the info if necessary.
     * @return           Info for the image with the given identifier.
     * @throws IOException        If there is an error reading or writing to or
     *                            from the cache.
     * @see #getInfo(Identifier)
     */
    Optional<Info> getOrReadInfo(final Identifier identifier,
                                 final Processor proc) throws IOException {
        // Try to retrieve it from an object or derivative cache.
        Optional<Info> optInfo = getInfo(identifier);
        if (optInfo.isEmpty()) {
            // Read it from the processor and then add it to both the
            // derivative and object caches.
            Info info = readInfo(identifier, proc);

            // Add it to the derivative and object caches.
            final DerivativeCache derivCache =
                    CacheFactory.getDerivativeCache().orElse(null);
            putInCachesAsync(identifier, info, derivCache);
            optInfo = Optional.of(info);
        }
        LOGGER.trace("Info for {}: {}", identifier, optInfo.get().toJSON());
        return optInfo;
    }

    boolean isObjectCacheEnabled() {
        return Configuration.getInstance().
                getBoolean(Key.INFO_CACHE_ENABLED, false);
    }

    public void purgeObjectCache() {
        LOGGER.debug("purgeObjectCache()");
        infoCache.purge();
    }

    void purgeObjectCache(Identifier identifier) {
        LOGGER.debug("purgeObjectCache(): purging {}", identifier);
        infoCache.purge(identifier);
    }

    /**
     * Adds an info to the object cache synchronously.
     */
    void putInObjectCache(Identifier identifier, Info info) {
        if (isObjectCacheEnabled()) {
            LOGGER.debug("putInObjectCache(): adding info: {} (new size: {})",
                    identifier,
                    infoCache.size() + 1);
            infoCache.put(identifier, info);
        } else {
            LOGGER.trace("putInObjectCache(): {} is disabled; doing nothing",
                    infoCache.getClass().getSimpleName());
        }
    }

    /**
     * Adds an info to the object and derivative caches asynchronously.
     */
    private void putInCachesAsync(Identifier identifier,
                                  Info info,
                                  DerivativeCache derivCache) {
        TaskQueue.getInstance().submit(() -> {
            putInObjectCache(identifier, info);
            if (derivCache != null) {
                try {
                    derivCache.put(identifier, info);
                } catch (IOException e) {
                    LOGGER.error("putInCachesAsync(): {}", e.getMessage());
                }
            }
            return null;
        });
    }

    /**
     * Reads the information of a source image from the given processor.
     */
    private Info readInfo(final Identifier identifier,
                          final Processor proc) throws IOException {
        final Stopwatch watch = new Stopwatch();
        final Info info = proc.readInfo();

        LOGGER.debug("readInfo(): read {} from {} in {}",
                identifier,
                proc.getClass().getSimpleName(),
                watch);

        info.setIdentifier(identifier);
        return info;
    }

}
