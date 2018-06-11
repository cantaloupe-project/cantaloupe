package edu.illinois.library.cantaloupe.config;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public interface Configuration {

    /**
     * @return Global application configuration instance.
     */
    static Configuration getInstance() {
        return ConfigurationFactory.getInstance();
    }

    /**
     * Clears the key-value pairs from the instance (but not its persistent
     * store, if available).
     */
    void clear();

    /**
     * Removes a single key-value pair from the instance (but not its
     * persistent store, if available).
     *
     * @param key Key to remove.
     */
    void clearProperty(Key key);

    void clearProperty(String key);

    /**
     * @throws java.util.NoSuchElementException
     */
    boolean getBoolean(Key key);

    /**
     * @throws java.util.NoSuchElementException
     */
    boolean getBoolean(String key);

    /**
     * @see #getBoolean(String, boolean)
     */
    boolean getBoolean(Key key, boolean defaultValue);

    /**
     * @param key
     * @param defaultValue
     * @return Boolean value corresponding to the given key, or the given
     *         default value if the target value is null or cannot be coerced
     *         to a boolean.
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * @throws java.util.NoSuchElementException
     */
    double getDouble(Key key);

    /**
     * @throws java.util.NoSuchElementException
     */
    double getDouble(String key);

    double getDouble(Key key, double defaultValue);

    /**
     * @param key
     * @param defaultValue
     * @return Double value corresponding to the given key, or the given
     *         default value if the target value is null or cannot be coerced
     *         to a double.
     */
    double getDouble(String key, double defaultValue);

    /**
     * @throws java.util.NoSuchElementException
     */
    float getFloat(Key key);

    /**
     * @throws java.util.NoSuchElementException
     */
    float getFloat(String key);

    float getFloat(Key key, float defaultValue);

    /**
     * @param key
     * @param defaultValue
     * @return Float value corresponding to the given key, or the given
     *         default value if the target value is null or cannot be coerced
     *         to a float.
     */
    float getFloat(String key, float defaultValue);

    /**
     * @throws java.util.NoSuchElementException
     */
    int getInt(Key key);

    /**
     * @throws java.util.NoSuchElementException
     */
    int getInt(String key);

    int getInt(Key key, int defaultValue);

    /**
     * @param key
     * @param defaultValue
     * @return Int value corresponding to the given key, or the given default
     *         value if the target value is null or cannot be coerced to an
     *         int.
     */
    int getInt(String key, int defaultValue);

    /**
     * @return All keys contained in the configuration.
     */
    Iterator<String> getKeys();

    /**
     * @throws java.util.NoSuchElementException
     */
    long getLong(Key key);

    /**
     * @throws java.util.NoSuchElementException
     */
    long getLong(String key);

    long getLong(Key key, long defaultValue);

    /**
     * @param key
     * @param defaultValue
     * @return Long value corresponding to the given key, or the given default
     *         value if the target value is null or cannot be coerced to a
     *         long.
     */
    long getLong(String key, long defaultValue);

    Object getProperty(Key key);

    Object getProperty(String key);

    String getString(Key key);

    String getString(String key);

    String getString(Key key, String defaultValue);

    /**
     * @param key
     * @param defaultValue
     * @return String value corresponding to the given key, or the given
     *         default value if the target value is null.
     */
    String getString(String key, String defaultValue);

    /**
     * Reloads the configuration from its persistent store.
     *
     * @throws IOException If there is a physical problem reloading the
     *                     configuration.
     * @throws IOException If there is a logical problem reloading the
     *                     configuration.
     */
    void reload() throws IOException, ConfigurationException;

    /**
     * Saves the configuration. Implementations that don't support saving
     * should just do nothing.
     *
     * @throws IOException If there is a problem saving the file.
     */
    void save() throws IOException;

    void setProperty(Key key, Object value);

    void setProperty(String key, Object value);

    /**
     * Starts watching the configuration file for changes. Implementations that
     * don't support watching should just do nothing.
     */
    void startWatching();

    /**
     * Stops watching the configuration file for changes. Implementations that
     * don't support watching should just do nothing.
     */
    void stopWatching();

    /**
     * This default implementation uses the Iterator returned by
     * {@link #getKeys} in conjunction with {@link #getProperty(String)} to
     * build a map. Implementations should override it if they can do it more
     * efficiently.
     *
     * @return Configuration keys and values in a read-only map.
     */
    default Map<String,Object> toMap() {
        final Map<String,Object> map = new LinkedHashMap<>();
        final Iterator<String> keys = getKeys();
        while (keys.hasNext()) {
            final String key = keys.next();
            map.put(key, getProperty(key));
        }
        return Collections.unmodifiableMap(map);
    }

}
