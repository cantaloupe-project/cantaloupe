package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Application configuration. Provides read-only or read-write access to
 * key-value pairs (either as {@link Key}s or as arbitrary strings) in some
 * kind of underlying storage.
 */
public interface Configuration {

    /**
     * @return Global application configuration instance.
     */
    static Configuration getInstance() {
        return ConfigurationFactory.getInstance();
    }

    /**
     * Clears any key-value pairs from the instance (but not its persistent
     * store, if available).
     */
    void clear();

    /**
     * @see #clearProperty(String)
     */
    default void clearProperty(Key key) {
        clearProperty(key.key());
    }

    /**
     * Removes a single key-value pair from the instance (but not its
     * persistent store, if available).
     *
     * @param key Key to remove.
     */
    void clearProperty(String key);

    /**
     * @see #getBoolean(String)
     */
    default boolean getBoolean(Key key) {
        return getBoolean(key.key());
    }

    /**
     * @throws NoSuchElementException
     * @throws NumberFormatException
     */
    boolean getBoolean(String key);

    /**
     * @see #getBoolean(String, boolean)
     */
    default boolean getBoolean(Key key, boolean defaultValue) {
        return getBoolean(key.key(), defaultValue);
    }

    /**
     * @return Boolean value corresponding to the given key, or the given
     *         default value if the target value is null or cannot be coerced
     *         to a boolean.
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * @throws NoSuchElementException
     * @throws NumberFormatException
     */
    default double getDouble(Key key) {
        return getDouble(key.key());
    }

    /**
     * @throws NoSuchElementException
     * @throws NumberFormatException
     */
    double getDouble(String key);

    /**
     * @see #getDouble(String, double)
     */
    default double getDouble(Key key, double defaultValue) {
        return getDouble(key.key(), defaultValue);
    }

    /**
     * @return Double value corresponding to the given key, or the given
     *         default value if the target value is null or cannot be coerced
     *         to a double.
     */
    double getDouble(String key, double defaultValue);

    /**
     * @see #getFloat(String)
     */
    default float getFloat(Key key) {
        return getFloat(key.key());
    }

    /**
     * @throws NoSuchElementException
     * @throws NumberFormatException
     */
    float getFloat(String key);

    /**
     * @see #getFloat(String, float)
     */
    default float getFloat(Key key, float defaultValue) {
        return getFloat(key.key(), defaultValue);
    }

    /**
     * @return Float value corresponding to the given key, or the given
     *         default value if the target value is null or cannot be coerced
     *         to a float.
     */
    float getFloat(String key, float defaultValue);

    /**
     * @see #getInt(String)
     */
    default int getInt(Key key) {
        return getInt(key.key());
    }

    /**
     * @throws NoSuchElementException
     * @throws NumberFormatException
     */
    int getInt(String key);

    /**
     * @see #getInt(String, int)
     */
    default int getInt(Key key, int defaultValue) {
        return getInt(key.key(), defaultValue);
    }

    /**
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
     * @see #getLong(String)
     */
    default long getLong(Key key) {
        return getLong(key.key());
    }

    /**
     * @throws NoSuchElementException
     * @throws NumberFormatException
     */
    long getLong(String key);

    /**
     * @see #getLong(String, long)
     */
    default long getLong(Key key, long defaultValue) {
        return getLong(key.key(), defaultValue);
    }

    /**
     * @return Long value corresponding to the given key, or the given default
     *         value if the target value is null or cannot be coerced to a
     *         long.
     */
    long getLong(String key, long defaultValue);

    /**
     * @see #getLongBytes(String)
     */
    default long getLongBytes(Key key) {
        return getLongBytes(key.key());
    }

    /**
     * @throws NoSuchElementException
     * @throws NumberFormatException
     */
    default long getLongBytes(String key) {
        String str = getString(key);
        if (str != null) {
            return StringUtils.toByteSize(str);
        }
        throw new NoSuchElementException(key);
    }

    /**
     * @see #getLongBytes(String, long)
     */
    default long getLongBytes(Key key, long defaultValue) {
        return getLongBytes(key.key(), defaultValue);
    }

    /**
     * @return Byte size corresponding to the given key, or the given default
     *         value if the target value is null or cannot be coerced to a
     *         byte size.
     * @see edu.illinois.library.cantaloupe.util.StringUtils#toByteSize(String)
     *      which is used to parse the string.
     */
    default long getLongBytes(String key, long defaultValue) {
        String str = getString(key);
        if (str != null && !str.isEmpty()) {
            try {
                return StringUtils.toByteSize(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * @see #getProperty(String)
     */
    default Object getProperty(Key key) {
        return getProperty(key.key());
    }

    /**
     * @return Object value corresponding to the given key, or {@literal null}
     *         if not set.
     */
    Object getProperty(String key);

    /**
     * @see #getString(String)
     */
    default String getString(Key key) {
        return getString(key.key());
    }

    /**
     * @return String value corresponding to the given key, or {@literal null}
     *         if not set.
     */
    String getString(String key);

    /**
     * @see #getString(String, String)
     */
    default String getString(Key key, String defaultValue) {
        return getString(key.key(), defaultValue);
    }

    /**
     * @return String value corresponding to the given key, or the given
     *         default value if the target value is null.
     */
    String getString(String key, String defaultValue);

    /**
     * Reloads the configuration from its persistent store.
     *
     * @throws IOException            if there is a physical problem reloading
     *                                the configuration.
     * @throws ConfigurationException if there is a logical problem reloading
     *                                the configuration.
     */
    void reload() throws IOException, ConfigurationException;

    /**
     * Saves the configuration. Implementations that don't support saving do
     * nothing.
     *
     * @throws IOException if there is a problem saving the configuration.
     */
    void save() throws IOException;

    /**
     * @see #setProperty(String, Object)
     */
    default void setProperty(Key key, Object value) {
        setProperty(key.key(), value);
    }

    void setProperty(String key, Object value);

    /**
     * This default implementation uses the {@link Iterator} returned by {@link
     * #getKeys} in conjunction with {@link #getProperty(String)} to build a
     * map. Implementations should override if they can do it more efficiently.
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
