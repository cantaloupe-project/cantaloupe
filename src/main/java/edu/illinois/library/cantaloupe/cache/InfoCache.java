package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.util.ObjectCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ObjectCache}-backed cache for {@link Info} instances.
 *
 * @since 4.0
 */
public final class InfoCache {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InfoCache.class);

    /**
     * This includes all {@link Info} properties including embedded metadata
     * (which will probably comprise most of this size, if present).
     */
    private static final int EXPECTED_AVERAGE_INFO_SIZE = 4096;

    /**
     * Cached infos will consume, at most, this much of max heap.
     */
    private static final double MAX_HEAP_PERCENT = 0.05;

    private final ObjectCache<Identifier, Info> objectCache;

    InfoCache() {
        final long maxByteSize =
                Math.round(Runtime.getRuntime().maxMemory() * MAX_HEAP_PERCENT);
        final long maxCount = Math.round(maxByteSize /
                (float) EXPECTED_AVERAGE_INFO_SIZE);

        LOGGER.info("Max {} capacity: {} ({}% max heap / {}-byte expected average info size)",
                InfoCache.class.getSimpleName(),
                maxCount,
                Math.round(MAX_HEAP_PERCENT * 100),
                EXPECTED_AVERAGE_INFO_SIZE);
        objectCache = new ObjectCache<>(maxCount);
    }

    /**
     * @param identifier Identifier of the source image for which to retrieve
     *                   the info.
     * @return           Info for the image with the given identifier.
     */
    Info get(final Identifier identifier) {
        return objectCache.get(identifier);
    }

    public long maxSize() {
        return objectCache.maxSize();
    }

    void purge() {
        LOGGER.debug("purge()");
        objectCache.removeAll();
    }

    void purge(Identifier identifier) {
        LOGGER.debug("purge(Identifier): purging {}", identifier);
        objectCache.remove(identifier);
    }

    /**
     * Adds an info.
     */
    void put(Identifier identifier, Info info) {
        LOGGER.debug("putInObjectCache(): adding info: {} (new size: {})",
                identifier,
                objectCache.size() + 1);
        objectCache.put(identifier, info);
    }

    /**
     * @return Number of items in the cache.
     */
    public long size() {
        return objectCache.size();
    }

}
