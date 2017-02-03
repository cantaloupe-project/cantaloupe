package edu.illinois.library.cantaloupe.config;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Class wrapping a Commons PropertyConfiguration instance for writing
 * properties files retaining structure and comments.
 */
class PropertiesConfiguration extends FileConfiguration implements Configuration {

    private org.apache.commons.configuration.PropertiesConfiguration commonsConfig =
            new org.apache.commons.configuration.PropertiesConfiguration();

    @Override
    public void clear() {
        commonsConfig.clear();
    }

    @Override
    public void clearProperty(String key) {
        commonsConfig.clearProperty(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return commonsConfig.getBoolean(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return commonsConfig.getBoolean(key, defaultValue);
    }

    @Override
    public double getDouble(String key) {
        return commonsConfig.getDouble(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return commonsConfig.getDouble(key, defaultValue);
    }

    @Override
    public float getFloat(String key) {
        return commonsConfig.getFloat(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return commonsConfig.getFloat(key, defaultValue);
    }

    @Override
    public int getInt(String key) {
        return commonsConfig.getInt(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return commonsConfig.getInt(key, defaultValue);
    }

    @Override
    public Iterator<String> getKeys() {
        return commonsConfig.getKeys();
    }

    @Override
    public long getLong(String key) {
        return commonsConfig.getLong(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return commonsConfig.getLong(key, defaultValue);
    }

    @Override
    public Object getProperty(String key) {
        return commonsConfig.getProperty(key);
    }

    @Override
    public String getString(String key) {
        // Commas in values are interpreted as multiple values. We don't want
        // that, so we join them into one value.
        String[] parts = commonsConfig.getStringArray(key);
        StringBuilder builder = new StringBuilder();
        for (int i = 0, count = parts.length; i < count; i++) {
            builder.append(parts[i]);
            if (i < count - 1) {
                builder.append(",");
            }
        }
        String str = builder.toString();
        return str.length() > 0 ? str : null;
    }

    @Override
    public String getString(String key, String defaultValue) {
        String str = getString(key);
        if (str == null) {
            str = defaultValue;
        }
        return str;
    }

    @Override
    public synchronized void reload() {
        final File configFile = getFile();
        if (configFile != null) {
            try {
                if (commonsConfig != null) {
                    System.out.println("Reloading config file: " + configFile);
                } else {
                    System.out.println("Loading config file: " + configFile);
                }
                commonsConfig = new org.apache.commons.configuration.
                        PropertiesConfiguration(configFile);
            } catch (org.apache.commons.configuration.ConfigurationException e) {
                // The logger may not have been initialized yet, as it depends
                // on a working configuration. (Also, we don't want to
                // introduce a dependency on the logger.)
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Saves the configuration to the file returned by
     * {@link #getFile()}, if available, or does nothing if not.
     *
     * @throws IOException
     */
    @Override
    public synchronized void save() throws IOException {
        try {
            commonsConfig.save();
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public synchronized void setProperty(String key, Object value) {
        commonsConfig.setProperty(key, value);
    }

}
