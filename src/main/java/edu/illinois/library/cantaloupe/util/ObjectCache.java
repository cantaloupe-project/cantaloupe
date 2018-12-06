package edu.illinois.library.cantaloupe.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Size-bounded heap cache.
 */
public final class ObjectCache<K, V> {

    private final long maxSize;

    // This is thread-safe.
    private Cache<K, V> store;

    /**
     * Creates an instance with the given max size.
     */
    public ObjectCache(long maxSize) {
        this.maxSize = maxSize;
        store = Caffeine.newBuilder().softValues().maximumSize(maxSize).build();
    }

    public void cleanUp() {
        store.cleanUp();
    }

    public V get(K key) {
        return store.getIfPresent(key);
    }

    public long maxSize() {
        return maxSize;
    }

    public void purge() {
        store.invalidateAll();
    }

    public void put(K key, V value) {
        store.put(key, value);
    }

    public void remove(K key) {
        store.invalidate(key);
    }

    public void removeAll() {
        store.invalidateAll();
    }

    public long size() {
        return store.estimatedSize();
    }

}
