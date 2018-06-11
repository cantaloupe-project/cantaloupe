package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.ConversionException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>Read-only configuration backed by the environment.</p>
 *
 * <p>To make key names environment-legal and prevent clashes with other
 * applications, they are {@link #ENVIRONMENT_KEY_PREFIX prefixed with a
 * string}, converted to uppercase, and all non-alphanumerics are {@link
 * #ENVIRONMENT_KEY_REPLACEMENT replaced with underscores}. Example:</p>
 *
 * <pre>this_is_a_key.name --&gt; CANTALOUPE_THIS_IS_A_KEY_NAME</pre>
 */
class EnvironmentConfiguration extends AbstractConfiguration
        implements Configuration {

    static final String ENVIRONMENT_KEY_PREFIX = "CANTALOUPE";
    static final String ENVIRONMENT_KEY_REPLACEMENT = "[^A-Za-z0-9]";

    static String toEnvironmentKey(String key) {
        return ENVIRONMENT_KEY_PREFIX + "_" +
                key.toUpperCase().replaceAll(ENVIRONMENT_KEY_REPLACEMENT, "_");
    }

    /**
     * Does nothing, as this implementation is not writable.
     */
    @Override
    public void clear() {}

    /**
     * Does nothing, as this implementation is not writable.
     */
    @Override
    public void clearProperty(String key) {}

    @Override
    public boolean getBoolean(String key) {
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            switch (value) {
                case "1":
                case "true":
                    return true;
                case "0":
                case "false":
                    return false;
                default:
                    throw new ConversionException(key);
            }
        }
        throw new NoSuchElementException(key);
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
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new ConversionException(e);
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
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                throw new ConversionException(e);
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
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new ConversionException(e);
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
        return System.getenv().keySet().iterator();
    }

    @Override
    public long getLong(String key) {
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new ConversionException(e);
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
        return getString(key);
    }

    @Override
    public String getString(String key) {
        key = toEnvironmentKey(key);
        return System.getenv(key);
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
     * Does nothing, as this implementation does not maintain state.
     */
    @Override
    public void reload() {}

    /**
     * Does nothing, as this implementation is not writable.
     */
    @Override
    public void save() {}

    /**
     * Does nothing, as this implementation is not writable.
     */
    @Override
    public void setProperty(String key, Object value) {
    }

    /**
     * Does nothing, as this implementation does not support watching.
     */
    @Override
    public void startWatching() {}

    /**
     * Does nothing, as this implementation does not support watching.
     */
    @Override
    public void stopWatching() {}

}
