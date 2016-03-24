package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Singleton class.
 */
public class Configuration {

    private static Logger logger = LoggerFactory.getLogger(Configuration.class);

    public static final String CONFIG_FILE_VM_ARGUMENT = "cantaloupe.config";

    private static Configuration instance;

    private org.apache.commons.configuration.Configuration commonsConfig;

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
            instance.reloadConfigurationFile();
        }
        return instance;
    }

    protected Configuration() {
        commonsConfig = new BaseConfiguration();
    }

    public void clear() {
        commonsConfig.clear();
    }

    public synchronized void reloadConfigurationFile() {
        try {
            File configFile = getConfigurationFile();
            if (configFile != null) {
                logger.info("Reloading configuration file: {}", configFile);
                PropertiesConfiguration propConfig = new PropertiesConfiguration();
                propConfig.load(configFile);
                propConfig.setFile(configFile);
                commonsConfig = propConfig;
            }
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            // The logger has probably not been initialized yet, as it
            // depends on a working configuration.
            System.out.println(e.getMessage());
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
                // the logger has probably not been initialized yet
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

    public synchronized void save() throws ConfigurationException {
        if (commonsConfig instanceof FileConfiguration) {
            try {
                ((FileConfiguration) commonsConfig).save();
            } catch (org.apache.commons.configuration.ConfigurationException e) {
                throw new ConfigurationException(e.getMessage());
            }
        }
    }

    public synchronized void setProperty(String key, Object value) {
        commonsConfig.setProperty(key, value);
    }

}
