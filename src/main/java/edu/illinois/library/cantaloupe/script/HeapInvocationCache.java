package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.util.ObjectCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HeapInvocationCache implements InvocationCache {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(HeapInvocationCache.class);

    private ObjectCache<Object, Object> store;

    HeapInvocationCache() {
        final long maxSize = computeMaxSize();
        LOGGER.info("Invocation cache limit: {}", maxSize);
        store = new ObjectCache<>(maxSize);
    }

    private long computeMaxSize() {
        // TODO: this is very crude and needs tuning.
        final Runtime runtime = Runtime.getRuntime();
        return Math.round(runtime.maxMemory() / 1024f / 2f);
    }

    @Override
    public Object get(Object key) {
        return store.get(key);
    }

    @Override
    public void purge() {
        store.purge();
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    @Override
    public long size() {
        return store.size();
    }

}
