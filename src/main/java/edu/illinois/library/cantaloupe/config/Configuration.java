package edu.illinois.library.cantaloupe.config;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public interface Configuration {

    /**
     * Clears the key-value pairs from the instance (but not its persistent
     * store).
     */
    void clear();

    boolean getBoolean(String key);

    boolean getBoolean(String key, boolean defaultValue);

    double getDouble(String key);

    double getDouble(String key, double defaultValue);

    /**
     * @return File corresponding to the persistent configuration file, or
     *         <code>null</code> if the implementation is not file-based.
     */
    File getFile();

    float getFloat(String key);

    float getFloat(String key, float defaultValue);

    int getInt(String key);

    int getInt(String key, int defaultValue);

    Iterator<String> getKeys();

    long getLong(String key);

    long getLong(String key, long defaultValue);

    Object getProperty(String key);

    String getString(String key);

    String getString(String key, String defaultValue);

    /**
     * Reloads the configuration from its persistent store.
     *
     * @throws IOException If there is a problem reloading the configuration.
     */
    void reload() throws IOException;

    /**
     * Saves the configuration. Not all implementations will be able to do
     * this, which is fine.
     *
     * @throws IOException If there is a problem saving the file.
     */
    void save() throws IOException;

    void setProperty(String key, Object value);

}
