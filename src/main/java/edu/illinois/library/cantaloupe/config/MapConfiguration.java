package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.util.StringUtils;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory configuration that cannot be persisted.
 */
public class MapConfiguration implements Configuration {

    private final ConcurrentMap<String,Object> configuration =
            new ConcurrentHashMap<>();

    @Override
    public void clear() {
        configuration.keySet().clear();
    }

    @Override
    public void clearProperty(String key) {
        configuration.remove(key);
    }

    public Map<String,Object> getBackingMap() {
        return configuration;
    }

    @Override
    public boolean getBoolean(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return StringUtils.toBoolean(value.toString());
        } else {
            throw new NoSuchElementException(key);
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public double getDouble(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return Double.parseDouble(value.toString());
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        try {
            return getDouble(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Optional<Path> getFile() {
        return Optional.empty();
    }

    @Override
    public float getFloat(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return Float.parseFloat(value.toString());
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        try {
            return getFloat(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Iterator<String> getKeys() {
        return configuration.keySet().iterator();
    }

    @Override
    public long getLong(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        try {
            return getLong(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Object getProperty(String key) {
        return configuration.get(key);
    }

    @Override
    public String getString(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    @Override
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
    @Override
    public void reload() {}

    /**
     * No-op.
     */
    @Override
    public void save() {}

    @Override
    public void setProperty(String key, Object value) {
        configuration.put(key, value);
    }

}
