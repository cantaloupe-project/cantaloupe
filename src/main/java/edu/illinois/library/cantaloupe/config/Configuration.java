package edu.illinois.library.cantaloupe.config;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

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

    boolean getBoolean(Key key);

    boolean getBoolean(String key);

    boolean getBoolean(Key key, boolean defaultValue);

    /**
     * @param key
     * @param defaultValue
     * @return Boolean value corresponding to the given key, or the given
     *         default value if the target value is null or cannot be coerced
     *         to a boolean.
     */
    boolean getBoolean(String key, boolean defaultValue);

    double getDouble(Key key);

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
     * @return File corresponding to the persistent configuration file, or
     *         <code>null</code> if the implementation is not file-based.
     */
    File getFile();

    float getFloat(Key key);

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

    int getInt(Key key);

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

    long getLong(Key key);

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

}
