package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.ConversionException;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory configuration that cannot be persisted.
 */
class MemoryConfiguration extends AbstractConfiguration
        implements Configuration {

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

    @Override
    public boolean getBoolean(String key) {
        Object value = configuration.get(key);
        boolean bool;
        if (value != null) {
            switch (value.toString()) {
                case "1":
                case "true":
                    bool = true;
                    break;
                case "0":
                case "false":
                    bool = false;
                    break;
                default:
                    throw new ConversionException(key);
            }
            return bool;
        } else {
            throw new NoSuchElementException(key);
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (NoSuchElementException | ConversionException e) {
            return defaultValue;
        }
    }

    @Override
    public double getDouble(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new ConversionException(e.getMessage(), e);
            }
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        try {
            return getDouble(key);
        } catch (NoSuchElementException | ConversionException e) {
            return defaultValue;
        }
    }

    @Override
    public float getFloat(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            try {
                return Float.parseFloat(value.toString());
            } catch (NumberFormatException e) {
                throw new ConversionException(e.getMessage(), e);
            }
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        try {
            return getFloat(key);
        } catch (NoSuchElementException | ConversionException e) {
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key) {
        Object value = configuration.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new ConversionException(e.getMessage(), e);
            }
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (NoSuchElementException | ConversionException e) {
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
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                throw new ConversionException(e.getMessage(), e);
            }
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        try {
            return getLong(key);
        } catch (NoSuchElementException | ConversionException e) {
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

    /**
     * No-op.
     */
    @Override
    public void startWatching() {}

    /**
     * No-op.
     */
    @Override
    public void stopWatching() {}

    @Override
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(configuration);
    }

}
