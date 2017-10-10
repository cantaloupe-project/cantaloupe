package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.ConversionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.concurrent.locks.StampedLock;

/**
 * Class wrapping a Commons PropertyConfiguration instance for writing
 * properties files retaining structure and comments.
 */
class PropertiesConfiguration extends FileConfiguration
        implements Configuration {

    private org.apache.commons.configuration.PropertiesConfiguration commonsConfig =
            new org.apache.commons.configuration.PropertiesConfiguration();
    private byte[] contentsChecksum = new byte[] {};
    private final StampedLock stampedLock = new StampedLock();

    public PropertiesConfiguration() {
        // Prevent commas in values from being interpreted as list item
        // delimiters.
        commonsConfig.setDelimiterParsingDisabled(true);
    }

    @Override
    public void clear() {
        final long stamp = stampedLock.writeLock();
        try {
            commonsConfig.clear();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public void clearProperty(String key) {
        final long stamp = stampedLock.writeLock();
        try {
            commonsConfig.clearProperty(key);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public boolean getBoolean(String key) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getBoolean(key);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getBoolean(key, defaultValue);
        } catch (ConversionException e) {
            return defaultValue;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public double getDouble(String key) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getDouble(key);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getDouble(key, defaultValue);
        } catch (ConversionException e) {
            return defaultValue;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public float getFloat(String key) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getFloat(key);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getFloat(key, defaultValue);
        } catch (ConversionException e) {
            return defaultValue;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public int getInt(String key) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getInt(key);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getInt(key, defaultValue);
        } catch (ConversionException e) {
            return defaultValue;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public Iterator<String> getKeys() {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getKeys();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public long getLong(String key) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getLong(key);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getLong(key, defaultValue);
        } catch (ConversionException e) {
            return defaultValue;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public Object getProperty(String key) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getProperty(key);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public String getString(String key) {
        final long stamp = stampedLock.readLock();
        try {
            return commonsConfig.getString(key);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        final long stamp = stampedLock.readLock();
        try {
            String str = getString(key);
            if (str == null) {
                str = defaultValue;
            }
            return str;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public void reload() {
        final File configFile = getFile();
        if (configFile != null) {
            final long stamp = stampedLock.writeLock();
            try {
                // Calculate the checksum of the file contents and compare it
                // to what has already been loaded. If the checksums match,
                // skip the reload.
                try {
                    byte[] fileBytes = Files.readAllBytes(configFile.toPath());
                    final MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] digestBytes = md.digest(fileBytes);

                    if (digestBytes == contentsChecksum) {
                        return;
                    }
                    contentsChecksum = digestBytes;
                } catch (FileNotFoundException e) {
                    System.err.println("File not found: " + e.getMessage());
                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println(e.getMessage());
                }

                if (commonsConfig != null) {
                    System.out.println("Reloading config file: " + configFile);
                } else {
                    System.out.println("Loading config file: " + configFile);
                }
                commonsConfig = new org.apache.commons.configuration.PropertiesConfiguration();
                // Prevent commas in values from being interpreted as list item
                // delimiters.
                commonsConfig.setDelimiterParsingDisabled(true);
                commonsConfig.setFile(configFile);
                try {
                    commonsConfig.load();
                } catch (org.apache.commons.configuration.ConfigurationException e) {
                    // The logger may not have been initialized yet, as it
                    // depends on a working configuration. (Also, we don't want
                    // to introduce a dependency on the logger.)
                    System.err.println(e.getMessage());
                }
            } finally {
                stampedLock.unlock(stamp);
            }
        }
    }

    /**
     * Saves the configuration to the file returned by {@link #getFile}, if
     * available, or does nothing if not.
     */
    @Override
    public void save() throws IOException {
        final long stamp = stampedLock.writeLock();
        try {
            commonsConfig.save();
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public void setProperty(String key, Object value) {
        final long stamp = stampedLock.writeLock();
        try {
            commonsConfig.setProperty(key, value);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

}
