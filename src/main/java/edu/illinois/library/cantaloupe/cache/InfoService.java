package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.ThreadPool;
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

/**
 * <p>Used to obtain {@link Info} instances in an efficient way, utilizing
 * (optionally) several tiers of caching.</p>
 */
public class InfoService {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(InfoService.class);

    /**
     * We can't know the average size of an info in advance, but 100 bytes is
     * a reasonable estimate for an image with no other embedded subimages.
     * Infos for multiresolution images may be bigger. It's better to
     * overestimate this than underestimate it.
     */
    private static final int EXPECTED_AVERAGE_INFO_SIZE = 150;

    /**
     * Cached infos will consume, at most, this much of max heap.
     *
     * Of course, infos are tiny and in typical use they'll never consume
     * anywhere near this much of a reasonable-sized heap.
     */
    private static final float MAX_HEAP_PERCENT = 0.1f;

    private static InfoService instance;

    private final ObjectCache<Identifier, Info> objectCache;

    /**
     * For testing purposes only.
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

    private InfoService() {
        final long maxByteSize =
                Math.round(Runtime.getRuntime().maxMemory() * MAX_HEAP_PERCENT);
        final long maxCount = Math.round(maxByteSize /
                (float) EXPECTED_AVERAGE_INFO_SIZE);

        LOGGER.info("Max {} capacity: {} ({}% max heap / {}-byte expected average info size)",
                ObjectCache.class.getSimpleName(),
                maxCount,
                Math.round(MAX_HEAP_PERCENT * 100),
                EXPECTED_AVERAGE_INFO_SIZE);
        objectCache = new ObjectCache<>(maxCount);
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
     * </ol>
     *
     * @param identifier   Identifier of the source image for which to retrieve
     *                     the info.
     * @return             Info for the image with the given identifier.
     * @throws IOException If there is an error reading or writing to or
     *                     from the cache.
     * @see #getOrReadInfo(Identifier, Processor)
     */
    Info getInfo(final Identifier identifier) throws IOException {
        // Check the local object cache.
        Info info = objectCache.get(identifier);

        if (info != null) {
            LOGGER.debug("getInfo(): retrieved info of {} from {}",
                    identifier, objectCache.getClass().getSimpleName());
        } else {
            // Check the derivative cache.
            final DerivativeCache derivCache = CacheFactory.getDerivativeCache();
            if (derivCache != null) {
                Stopwatch watch = new Stopwatch();
                info = derivCache.getImageInfo(identifier);
                if (info != null) {
                    LOGGER.debug("getInfo(): retrieved info of {} from " +
                                    "{} in {} msec",
                            identifier,
                            derivCache.getClass().getSimpleName(),
                            watch.timeElapsed());

                    // Add it to the object cache (which it may already exist
                    // in, but it doesn't matter).
                    putInObjectCache(identifier, info);
                }
            }
        }
        return info;
    }

    public long getObjectCacheMaxSize() {
        return objectCache.maxSize();
    }

    public long getObjectCacheSize() {
        return objectCache.size();
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
    Info getOrReadInfo(final Identifier identifier, final Processor proc)
            throws IOException {
        // Try to retrieve it from an object or derivative cache.
        Info info = getInfo(identifier);
        if (info == null) {
            Stopwatch watch = new Stopwatch();

            // Read it from the processor and then add it to both the
            // derivative and object caches.
            info = readInfo(identifier, proc);

            LOGGER.debug("getOrReadInfo(): read info of {} from {} " +
                            "in {} msec",
                    identifier,
                    proc.getClass().getSimpleName(),
                    watch.timeElapsed());

            // Add it to the derivative and object caches.
            final DerivativeCache derivCache = CacheFactory.getDerivativeCache();
            putInCachesAsync(identifier, info, derivCache);
        }
        return info;
    }

    boolean isObjectCacheEnabled() {
        return Configuration.getInstance().
                getBoolean(Key.INFO_CACHE_ENABLED, false);
    }

    public void purgeObjectCache() {
        LOGGER.debug("purgeObjectCache()");
        objectCache.removeAll();
    }

    void purgeObjectCache(Identifier identifier) {
        LOGGER.debug("purgeObjectCache(): purging {}", identifier);
        objectCache.remove(identifier);
    }

    /**
     * Adds an info to the object cache synchronously.
     */
    void putInObjectCache(Identifier identifier, Info info) {
        if (isObjectCacheEnabled()) {
            LOGGER.debug("putInObjectCache(): adding info: {} (new size: {})",
                    identifier,
                    objectCache.size() + 1);
            objectCache.put(identifier, info);
        } else {
            LOGGER.debug("putInObjectCache(): {} is disabled; doing nothing",
                    objectCache.getClass().getSimpleName());
        }
    }

    /**
     * Adds an info to the object and derivative caches asynchronously.
     */
    private void putInCachesAsync(Identifier identifier,
                                  Info info,
                                  DerivativeCache derivCache) {
        ThreadPool.getInstance().submit(() -> {
            putInObjectCache(identifier, info);
            try {
                derivCache.put(identifier, info);
            } catch (IOException e) {
                LOGGER.error("putInCachesAsync(): {}", e.getMessage());
            }
            return null;
        }, ThreadPool.Priority.LOW);
    }

    /**
     * Reads the information of a source image from the given processor.
     */
    private Info readInfo(final Identifier identifier,
                          final Processor proc) throws IOException {
        final Stopwatch watch = new Stopwatch();
        final Info info = proc.readImageInfo();
        LOGGER.debug("readInfo(): read {} from {} in {} msec",
                identifier,
                proc.getClass().getSimpleName(),
                watch.timeElapsed());
        return info;
    }

}
