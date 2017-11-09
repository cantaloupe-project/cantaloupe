package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.util.ObjectCache;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Obtains {@link Info} instances in an efficient, cache-aware way.
 */
class InfoService {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(InfoService.class);

    private static volatile InfoService instance;

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
    static InfoService getInstance() {
        InfoService service = instance;
        if (service == null) {
            synchronized (InfoService.class) {
                service = instance;
                if (service == null) {
                    instance = new InfoService();
                    service = instance;
                }
            }
        }
        return service;
    }

    private InfoService() {
        final Runtime runtime = Runtime.getRuntime();
        final long maxSize = Math.round(runtime.maxMemory() / 1024f);
        objectCache = new ObjectCache<>(maxSize);
    }

    /**
     * <p>Returns an {@link Info} for the source image corresponding to the
     * given identifier.</p>
     *
     * <p>The following sources are consulted in order of preference from
     * most to least efficient:</p>
     *
     * <ol>
     *     <li>A local in-memory heap cache;</li>
     *     <li>The current derivative cache;</li>
     *     <li>The given processor. If this is the case, it will also be cached
     *     in the current derivative cache, if available. (This may happen
     *     asynchronously.)</li>
     * </ol>
     *
     * @param identifier Identifier of the source image for which to retrieve
     *                   the info.
     * @param proc       Processor to use to read the info if necessary.
     * @return           Info for the image with the given identifier.
     * @throws CacheException     If there is an error reading or writing to or
     *                            from the cache.
     * @throws ProcessorException If there is an error reading the info from
     *                            the processor.
     */
    Info getOrReadInfo(final Identifier identifier, final Processor proc)
            throws CacheException, ProcessorException {
        // Check the local object cache.
        Info info = objectCache.get(identifier);

        if (info != null) {
            LOGGER.debug("getOrReadInfo(): retrieved info of {} from {}",
                    identifier, objectCache.getClass().getSimpleName());
        } else {
            // Check the derivative cache.
            final DerivativeCache derivCache = CacheFactory.getDerivativeCache();
            if (derivCache != null) {
                Stopwatch watch = new Stopwatch();
                info = derivCache.getImageInfo(identifier);
                if (info != null) {
                    LOGGER.debug("getOrReadInfo(): retrieved info of {} from " +
                                    "{} in {} msec",
                            identifier,
                            derivCache.getClass().getSimpleName(),
                            watch.timeElapsed());

                    // Add it to the object cache (which it may already exist
                    // in, but it doesn't matter).
                    put(identifier, info);
                } else {
                    watch = new Stopwatch();

                    // The derivative cache does not contain the info, so read
                    // it from the processor and then add it to both the
                    // derivative and object caches.
                    info = readInfo(identifier, proc);

                    LOGGER.debug("getOrReadInfo(): read info of {} from {} " +
                                    "in {} msec",
                            identifier,
                            proc.getClass().getSimpleName(),
                            watch.timeElapsed());

                    // Add it to the derivative and object caches.
                    putAsync(identifier, info, derivCache);
                }
            } else {
                // There is no derivative cache available, so fall back to
                // reading it from the processor.
                info = readInfo(identifier, proc);

                // Add it to the object cache.
                put(identifier, info);
            }
        }
        return info;
    }

    void purgeObjectCache() {
        objectCache.removeAll();
    }

    void purgeObjectCache(Identifier identifier) {
        objectCache.remove(identifier);
    }

    /**
     * Adds an info to the object cache synchronously.
     */
    private void put(Identifier identifier, Info info) {
        if (Configuration.getInstance().getBoolean(Key.INFO_CACHE_ENABLED, true)) {
            LOGGER.debug("put(): adding info to object cache: {} (new size: {})",
                    identifier, objectCache.size() + 1);
            objectCache.put(identifier, info);
        } else {
            LOGGER.debug("put(): info cache is disabled; doing nothing");
        }
    }

    /**
     * Adds an info to the object and derivative caches asynchronously.
     */
    private void putAsync(Identifier identifier,
                          Info info,
                          DerivativeCache derivCache) {
        ThreadPool.getInstance().submit(() -> {
            put(identifier, info);
            if (derivCache != null) {
                derivCache.put(identifier, info);
            }
            return null;
        }, ThreadPool.Priority.LOW);
    }

    /**
     * Reads the information of a source image from the given processor.
     */
    private Info readInfo(final Identifier identifier,
                          final Processor proc) throws ProcessorException {
        final Stopwatch watch = new Stopwatch();
        final Info info = proc.readImageInfo();
        LOGGER.debug("readInfo(): read {} from {} in {} msec",
                identifier,
                proc.getClass().getSimpleName(),
                watch.timeElapsed());
        return info;
    }

}
