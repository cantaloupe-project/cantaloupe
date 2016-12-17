package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.ConversionException;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory configuration that cannot be persisted.
 */
class MemoryConfiguration implements Configuration {

    private ConcurrentMap<String,Object> configuration = new ConcurrentHashMap<>();

    public void clear() {
        configuration.keySet().clear();
    }

    public boolean getBoolean(String key) {
        Object value = configuration.get(key);
        boolean bool;
        if (value != null) {
            String stringValue = value.toString();
            if (stringValue.equals("1") || stringValue.equals("true")) {
                bool = true;
            } else if (stringValue.equals("0") || stringValue.equals("false")) {
                bool = false;
            } else {
                throw new ConversionException(key);
            }
            return bool;
        } else {
            throw new NoSuchElementException(key);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return Double.parseDouble(value.toString());
        }
        throw new NoSuchElementException(key);
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return getDouble(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * @return <code>null</code>.
     */
    public File getFile() {
        return null;
    }

    public float getFloat(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return Float.parseFloat(value.toString());
        }
        throw new NoSuchElementException(key);
    }

    public float getFloat(String key, float defaultValue) {
        try {
            return getInt(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getInt(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        throw new NoSuchElementException(key);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    public Iterator<String> getKeys() {
        return configuration.keySet().iterator();
    }

    public long getLong(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        throw new NoSuchElementException(key);
    }

    public long getLong(String key, long defaultValue) {
        try {
            return getLong(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    public Object getProperty(String key) {
        return configuration.get(key);
    }

    public String getString(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public String getString(String key, String defaultValue) {
        String string = getString(key);
        if (string != null) {
            return string;
        }
        return defaultValue;
    }

    /**
     * No-op.
     */
    public synchronized void reload() {}

    /**
     * No-op.
     */
    public void save() {}

    public void setProperty(String key, Object value) {
        configuration.put(key, value);
    }

    /**
     * No-op.
     */
    public void startWatching() {}

    /**
     * No-op.
     */
    public void stopWatching() {}

}
