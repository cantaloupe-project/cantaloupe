package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.ConversionException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Configuration whose accessors access an ordered list of other configurations,
 * which enables "falling back" from one configuration source to another.
 */
public final class ConfigurationProvider extends AbstractConfiguration
        implements Configuration {

    private List<Configuration> wrappedConfigs;

    /**
     * @param wrappedConfigs Backing configurations in the order they should be
     *                       consulted.
     */
    ConfigurationProvider(List<Configuration> wrappedConfigs) {
        this.wrappedConfigs = wrappedConfigs;
    }

    @Override
    public void clear() {
        wrappedConfigs.forEach(Configuration::clear);
    }

    @Override
    public void clearProperty(String key) {
        wrappedConfigs.forEach(c -> c.clearProperty(key));
    }

    @Override
    public boolean getBoolean(String key) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getBoolean(key);
            } catch (NoSuchElementException ignore) {}
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getBoolean(key);
            } catch (NoSuchElementException | ConversionException ignore) {}
        }
        return defaultValue;
    }

    @Override
    public double getDouble(String key) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getDouble(key);
            } catch (NoSuchElementException ignore) {}
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getDouble(key);
            } catch (NoSuchElementException | ConversionException ignore) {}
        }
        return defaultValue;
    }

    @Override
    public float getFloat(String key) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getFloat(key);
            } catch (NoSuchElementException ignore) {}
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getFloat(key);
            } catch (NoSuchElementException | ConversionException ignore) {}
        }
        return defaultValue;
    }

    @Override
    public int getInt(String key) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getInt(key);
            } catch (NoSuchElementException ignore) {}
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getInt(key);
            } catch (NoSuchElementException | ConversionException ignore) {}
        }
        return defaultValue;
    }

    @Override
    public Iterator<String> getKeys() {
        final List<Iterator<String>> iterators = wrappedConfigs.stream()
                .map(c -> c.toMap().keySet().iterator())
                .collect(Collectors.toList());

        return new Iterator<String>() {
            private int configIndex = 0;

            @Override
            public boolean hasNext() {
                if (!iterators.get(configIndex).hasNext()) {
                    configIndex++;
                    return configIndex < iterators.size() &&
                            iterators.get(configIndex).hasNext();
                }
                return true;
            }

            @Override
            public String next() {
                return iterators.get(configIndex).next();
            }
        };
    }

    @Override
    public long getLong(String key) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getLong(key);
            } catch (NoSuchElementException ignore) {}
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        for (Configuration config : wrappedConfigs) {
            try {
                return config.getLong(key);
            } catch (NoSuchElementException | ConversionException ignore) {}
        }
        return defaultValue;
    }

    @Override
    public Object getProperty(String key) {
        Object value = null;
        for (Configuration config : wrappedConfigs) {
            value = config.getProperty(key);
            if (value != null) {
                break;
            }
        }
        return value;
    }

    @Override
    public String getString(String key) {
        String value = null;
        for (Configuration config : wrappedConfigs) {
            value = config.getString(key);
            if (value != null) {
                break;
            }
        }
        return value;
    }

    @Override
    public String getString(String key, String defaultValue) {
        for (Configuration config : wrappedConfigs) {
            String value = config.getString(key);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    public List<Configuration> getWrappedConfigurations() {
        return wrappedConfigs;
    }

    @Override
    public void reload() {
        wrappedConfigs.forEach(c -> {
            try {
                c.reload();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void save() {
        wrappedConfigs.forEach(c -> {
            try {
                c.save();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void setProperty(String key, Object value) {
        wrappedConfigs.forEach(c -> c.setProperty(key, value));
    }

    @Override
    public void startWatching() {
        wrappedConfigs.forEach(Configuration::startWatching);
    }

    @Override
    public void stopWatching() {
        wrappedConfigs.forEach(Configuration::stopWatching);
    }

}
