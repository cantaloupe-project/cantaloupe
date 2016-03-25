package edu.illinois.library.cantaloupe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Singleton class.
 */
public class Configuration {

    private static Logger logger = LoggerFactory.getLogger(Configuration.class);

    public static final String CONFIG_FILE_VM_ARGUMENT = "cantaloupe.config";

    private static volatile Configuration instance;
    private static final Object lock = new Object();

    private Properties properties = new Properties();

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
        properties.clear();
    }

    public synchronized void reloadConfigurationFile() {
        final File configFile = getConfigurationFile();
        if (configFile != null) {
            logger.info("Reloading configuration file: {}", configFile);

            try (FileInputStream is = new FileInputStream(configFile)) {
                properties.load(is);
            } catch (IOException e) {
                // The logger has probably not been initialized yet, as it
                // depends on a working configuration.
                System.out.println(e.getMessage());
            }
        }
    }

    public boolean getBoolean(String key) throws ConversionException {
        Object propValue = properties.get(key);
        if (propValue != null) {
            String propString = propValue.toString().trim();
            if (propString.length() > 0) {
                if (new HashSet<>(Arrays.asList("1", "true")).
                        contains(propString)) {
                    return true;
                } else if (new HashSet<>(Arrays.asList("0", "false")).
                        contains(propString)) {
                    return false;
                }
            }
        }
        throw new ConversionException("getBoolean(): " + key +
                " does not map to a boolean");
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (ConversionException e) {
            // noop
        }
        return defaultValue;
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

    public float getFloat(String key) throws ConversionException {
        Object propValue = properties.get(key);
        if (propValue != null) {
            String propString = propValue.toString().trim();
            if (propString.length() > 0) {
                try {
                    return Float.parseFloat(propString);
                } catch (NumberFormatException e) {
                    // noop
                }
            }
        }
        throw new ConversionException(
                "getFloat(): " + key + " does not map to a float");
    }

    public float getFloat(String key, float defaultValue) {
        float value = defaultValue;
        try {
            value = getFloat(key);
        } catch (ConversionException e) {
            // noop
        }
        return value;
    }

    public int getInt(String key) throws ConversionException {
        Object propValue = properties.get(key);
        if (propValue != null) {
            String propString = propValue.toString().trim();
            if (propString.length() > 0) {
                try {
                    return Integer.parseInt(propString);
                } catch (NumberFormatException e) {
                    // noop
                }
            }
        }
        throw new ConversionException("getInt(): " + key +
                " does not map to an int");
    }

    public int getInt(String key, int defaultValue) {
        int value = defaultValue;
        try {
            value = getInt(key);
        } catch (ConversionException e) {
            // noop
        }
        return value;
    }

    public Iterator<String> getKeys() {
        Set<String> keys = new HashSet<>();
        for (Object key : properties.keySet()) {
            keys.add(key.toString());
        }
        return keys.iterator();
    }

    public long getLong(String key) throws ConversionException {
        Object propValue = properties.get(key);
        if (propValue != null) {
            String propString = propValue.toString().trim();
            if (propString.length() > 0) {
                try {
                    return Long.parseLong(propString);
                } catch (NumberFormatException e) {
                    // noop
                }
            }
        }
        throw new ConversionException("getLong(): " + key +
                " does not map to a long");
    }

    public long getLong(String key, long defaultValue) {
        long value = defaultValue;
        try {
            value = getLong(key);
        } catch (ConversionException e) {
            // noop
        }
        return value;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public String getString(String key) {
        Object propValue = properties.get(key);
        if (propValue != null) {
            return propValue.toString().trim();
        }
        return null;
    }

    public String getString(String key, String defaultValue) {
        String value = (String) properties.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public synchronized void save() throws ConfigurationException {
        try (FileOutputStream out =
                     new FileOutputStream(getConfigurationFile())) {
            properties.store(out, "");
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    public synchronized void setProperty(String key, Object value) {
        properties.put(key, value);
    }

}
