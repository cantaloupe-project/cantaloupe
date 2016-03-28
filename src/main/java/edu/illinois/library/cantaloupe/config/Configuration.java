package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Singleton class wrapping a Commons Configuration instance for writing
 * properties files retaining structure and comments.
 */
public class Configuration {

    public static final String CONFIG_FILE_VM_ARGUMENT = "cantaloupe.config";

    private static volatile Configuration instance;
    private static final Object lock = new Object();

    private org.apache.commons.configuration.Configuration commonsConfig =
            new BaseConfiguration();

    public static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * @return Global application configuration instance.
     */
    public static Configuration getInstance() {
        Configuration config = instance;
        if (config == null) {
            synchronized (lock) {
                config = instance;
                if (config == null) {
                    config = new Configuration();
                    config.reloadConfigurationFile();
                    instance = config;
                }
            }
        }
        return config;
    }

    protected Configuration() {}

    public void clear() {
        commonsConfig.clear();
    }

    public synchronized void reloadConfigurationFile() {
        final File configFile = getConfigurationFile();
        if (configFile != null) {
            try {
                if (commonsConfig != null && commonsConfig instanceof PropertiesConfiguration) {
                    System.out.println("Reloading config file: " + configFile);
                } else {
                    System.out.println("Loading config file: " + configFile);
                }
                commonsConfig = new PropertiesConfiguration(configFile);
            } catch (org.apache.commons.configuration.ConfigurationException e) {
                // The logger may not have been initialized yet, as it depends
                // on a working configuration. (Also, we don't want to
                // introduce a dependency on the logger.)
                System.out.println(e.getMessage());
            }
        }
    }

    public boolean getBoolean(String key) {
        return commonsConfig.getBoolean(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return commonsConfig.getBoolean(key, defaultValue);
    }

    public File getConfigurationFile() {
        String configFilePath = System.getProperty(CONFIG_FILE_VM_ARGUMENT);
        if (configFilePath != null) {
            try {
                // expand paths that start with "~"
                configFilePath = configFilePath.replaceFirst("^~",
                        System.getProperty("user.home"));
                return new File(configFilePath).getCanonicalFile();
            } catch (IOException e) {
                // The logger may not have been initialized yet, as it depends
                // on a working configuration. (Also, we don't want to
                // introduce a dependency on the logger.)
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    public float getFloat(String key) {
        return commonsConfig.getFloat(key);
    }

    public float getFloat(String key, float defaultValue) {
        return commonsConfig.getFloat(key, defaultValue);
    }

    public int getInt(String key) {
        return commonsConfig.getInt(key);
    }

    public int getInt(String key, int defaultValue) {
        return commonsConfig.getInt(key, defaultValue);
    }

    public Iterator<String> getKeys() {
        return commonsConfig.getKeys();
    }

    public long getLong(String key) {
        return commonsConfig.getLong(key);
    }

    public long getLong(String key, long defaultValue) {
        return commonsConfig.getLong(key, defaultValue);
    }

    public Object getProperty(String key) {
        return commonsConfig.getProperty(key);
    }

    public String getString(String key) {
        return commonsConfig.getString(key);
    }

    public String getString(String key, String defaultValue) {
        return commonsConfig.getString(key, defaultValue);
    }

    /**
     * Saves the configuration to the file returned by
     * {@link #getConfigurationFile()}, if available, or does nothing if not.
     *
     * @throws IOException
     */
    public synchronized void save() throws IOException {
        if (commonsConfig instanceof FileConfiguration) {
            try {
                ((FileConfiguration) commonsConfig).save();
            } catch (org.apache.commons.configuration.ConfigurationException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    public synchronized void setProperty(String key, Object value) {
        commonsConfig.setProperty(key, value);
    }

}
