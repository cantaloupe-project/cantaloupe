package edu.illinois.library.cantaloupe.script;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CaffeineInvocationCache implements InvocationCache {

    private static Logger logger = LoggerFactory.
            getLogger(CaffeineInvocationCache.class);

    private Cache<Object, Object> store;

    CaffeineInvocationCache() {
        final long maxSize = getMaxSize();
        logger.info("Invocation cache limit: {}", maxSize);
        store = Caffeine.newBuilder().maximumSize(maxSize).build();
    }

    @Override
    public Object get(Object key) {
        return store.getIfPresent(key);
    }

    private long getMaxSize() {
        // TODO: this is very crude and needs tuning.
        final Runtime runtime = Runtime.getRuntime();
        return Math.round(runtime.maxMemory() / 1024f / 2f);
    }

    @Override
    public void purge() {
        store.invalidateAll();
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    @Override
    public long size() {
        return store.estimatedSize();
    }

}
