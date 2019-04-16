package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.util.ObjectCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HeapInvocationCache implements InvocationCache {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(HeapInvocationCache.class);

    private static final int EXPECTED_AVERAGE_VALUE_SIZE = 2048;
    private static final double MAX_HEAP_PERCENT = 0.05;

    private ObjectCache<Object, Object> store;

    HeapInvocationCache() {
        final long maxCount = computeMaxCount();

        LOGGER.info("Max {} capacity: {} ({}% max heap / {}-byte expected average value size)",
                HeapInvocationCache.class.getSimpleName(),
                maxCount,
                Math.round(MAX_HEAP_PERCENT * 100),
                EXPECTED_AVERAGE_VALUE_SIZE);
        store = new ObjectCache<>(maxCount);
    }

    private long computeMaxCount() {
        final long maxByteSize = Math.round(
                Runtime.getRuntime().maxMemory() * MAX_HEAP_PERCENT);
        return Math.round(maxByteSize / (double) EXPECTED_AVERAGE_VALUE_SIZE);
    }

    @Override
    public Object get(Object key) {
        return store.get(key);
    }

    /**
     * @return Auto-computed max size.
     */
    @Override
    public long maxSize() {
        return store.maxSize();
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
