package edu.illinois.library.cantaloupe.script;

/**
 * Stores method invocations (method name and arguments) and corresponding
 * return values.
 */
public interface InvocationCache {

    /**
     * @param key
     *
     * @return Object associated with the given key, or <code>null</code>.
     */
    Object get(Object key);

    /**
     * Removes or invalidates all items in the cache.
     */
    void purge();

    void put(Object key, Object value);

    /**
     * @return Number of valid items in the cache.
     */
    long size();

}
