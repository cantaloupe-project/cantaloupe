package edu.illinois.library.cantaloupe.config;

import java.io.File;
import java.util.Iterator;

/**
 * <p>Class for reading configuration from the environment.</p>
 *
 * <p>This configuration uses different keys than the other configurations.
 * Keys are all-uppercase and prepended with <var>CANTALOUPE_</var>, and dots
 * are changed to underscores; so, for example:</p>
 *
 * <p><var>endpoint.iiif.2.enabled</var></p>
 *
 * <p>Is recognized by this configuration as:</p>
 *
 * <p><var>CANTALOUPE_ENDPOINT_IIIF_2_ENABLED</var></p>
 *
 * <p>The getter methods understand the non-transformed keys, but the
 * environment must contain the transformed versions.</p>
 *
 * <p>This class is read-only.</p>
 */
public class EnvironmentConfiguration implements Configuration {

    private static final String ENVIRONMENT_KEY_PREFIX = "CANTALOUPE_";

    private org.apache.commons.configuration.Configuration commonsConfig =
            new org.apache.commons.configuration.EnvironmentConfiguration();

    public void clear() {
        commonsConfig.clear();
    }

    /**
     * Transforms a configuration key to one that contains only
     * environment-safe characters.
     *
     * @param key Configuration key
     * @return Transformed key compatible with the environment.
     */
    private String environmentKey(String key) {
        return ENVIRONMENT_KEY_PREFIX + key.replace(".", "_").toUpperCase();
    }

    public boolean getBoolean(String key) {
        return commonsConfig.getBoolean(environmentKey(key));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return commonsConfig.getBoolean(environmentKey(key), defaultValue);
    }

    public double getDouble(String key) {
        return commonsConfig.getDouble(environmentKey(key));
    }

    public double getDouble(String key, double defaultValue) {
        return commonsConfig.getDouble(environmentKey(key), defaultValue);
    }

    /**
     * @return <code>null</code>.
     */
    public File getFile() {
        return null;
    }

    public float getFloat(String key) {
        return commonsConfig.getFloat(environmentKey(key));
    }

    public float getFloat(String key, float defaultValue) {
        return commonsConfig.getFloat(environmentKey(key), defaultValue);
    }

    public int getInt(String key) {
        return commonsConfig.getInt(environmentKey(key));
    }

    public int getInt(String key, int defaultValue) {
        return commonsConfig.getInt(environmentKey(key), defaultValue);
    }

    public Iterator<String> getKeys() {
        return commonsConfig.getKeys();
    }

    public long getLong(String key) {
        return commonsConfig.getLong(environmentKey(key));
    }

    public long getLong(String key, long defaultValue) {
        return commonsConfig.getLong(environmentKey(key), defaultValue);
    }

    public Object getProperty(String key) {
        return commonsConfig.getProperty(environmentKey(key));
    }

    public String getString(String key) {
        return commonsConfig.getString(environmentKey(key));
    }

    public String getString(String key, String defaultValue) {
        return commonsConfig.getString(environmentKey(key), defaultValue);
    }

    /**
     * No-op.
     */
    public void reload() {}

    /**
     * No-op.
     */
    public void save() {}

    public synchronized void setProperty(String key, Object value) {
        commonsConfig.setProperty(environmentKey(key), value);
    }

}
