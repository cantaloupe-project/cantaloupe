package edu.illinois.library.cantaloupe.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Size-bounded heap cache.
 */
public final class ObjectCache<K, V> {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ObjectCache.class);

    // This is thread-safe.
    private Cache<K, V> store;

    /**
     * Creates an instance with the given max size.
     */
    public ObjectCache(long maxSize) {
        LOGGER.info("{} limit: {}", getClass().getSimpleName(), maxSize);
        store = Caffeine.newBuilder().maximumSize(maxSize).build();
    }

    public void cleanUp() {
        store.cleanUp();
    }

    public V get(K key) {
        return store.getIfPresent(key);
    }

    public void purge() {
        store.invalidateAll();
    }

    public void put(K key, V value) {
        store.put(key, value);
    }

    public long size() {
        return store.estimatedSize();
    }

}
