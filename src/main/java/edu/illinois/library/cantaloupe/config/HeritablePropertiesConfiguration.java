package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.StampedLock;

/**
 * <p>Properties configuration that allows file-based inheritance. A file can be
 * linked to a parent file using {@link #EXTENDS_KEY}. Keys in child files
 * override ones in ancestor files.</p>
 *
 * <p>This implementation uses optimistic reads via a {@link StampedLock} for
 * good performance with thread-safety.</p>
 */
class HeritablePropertiesConfiguration extends HeritableFileConfiguration
        implements Configuration {

    private static final String EXTENDS_KEY = "extends";

    private final StampedLock lock = new StampedLock();

    /**
     * Map of PropertiesConfigurations in order from leaf to trunk.
     */
    private LinkedHashMap<File, PropertiesConfiguration> commonsConfigs =
            new LinkedHashMap<>();

    /**
     * Checksum of the main configuration file contents. When the watcher
     * receives a change event from the filesystem, it will compute the new
     * checksum and reload only if they don't match. That's because there are
     * often multiple events per change.
     */
    private byte[] mainContentsChecksum = new byte[] {};

    /**
     * @return Wrapped configurations in order from main to most distant
     *         ancestor.
     */
    List<PropertiesConfiguration> getConfigurationTree() {
        return new ArrayList<>(commonsConfigs.values());
    }

    ////////////////// HeritableFileConfiguration methods ///////////////////

    @Override
    Collection<File> getFiles() {
        final long stamp = lock.readLock();
        try {
            return commonsConfigs.keySet();
        } finally {
            lock.unlock(stamp);
        }
    }

    //////////////////////// Configuration methods //////////////////////////

    @Override
    public void clear() {
        final long stamp = lock.writeLock();
        try {
            commonsConfigs.values().parallelStream()
                    .forEach(PropertiesConfiguration::clear);
            mainContentsChecksum = new byte[]{};
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public void clearProperty(final String key) {
        final long stamp = lock.writeLock();
        try {
            commonsConfigs.values().parallelStream()
                    .forEach(c -> c.clearProperty(key));
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public boolean getBoolean(final String key) {
        Boolean bool = readBooleanOptimistically(key);
        if (bool != null) {
            return bool;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            Boolean bool = readBooleanOptimistically(key);
            return (bool != null) ? bool : defaultValue;
        } catch (ConversionException e) {
            return defaultValue;
        }
    }

    private Boolean readBooleanOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Boolean bool = readBoolean(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                bool = readBoolean(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return bool;
    }

    private Boolean readBoolean(String key) {
        Boolean bool = null;
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                bool = commonsConfig.getBoolean(key, null);
                break;
            }
        }
        return bool;
    }

    @Override
    public double getDouble(String key) {
        Double dub = readDoubleOptimistically(key);
        if (dub != null) {
            return dub;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        try {
            Double dub = readDoubleOptimistically(key);
            return (dub != null) ? dub : defaultValue;
        } catch (ConversionException e) {
            return defaultValue;
        }
    }

    private Double readDoubleOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Double dub = readDouble(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                dub = readDouble(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return dub;
    }

    private Double readDouble(String key) {
        Double dub = null;
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                dub = commonsConfig.getDouble(key, null);
                break;
            }
        }
        return dub;
    }

    @Override
    public float getFloat(String key) {
        Float flo = readFloatOptimistically(key);
        if (flo != null) {
            return flo;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        try {
            Float flo = readFloatOptimistically(key);
            return (flo != null) ? flo : defaultValue;
        } catch (ConversionException e) {
            return defaultValue;
        }
    }

    private Float readFloatOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Float flo = readFloat(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                flo = readFloat(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return flo;
    }

    private Float readFloat(String key) {
        Float flo = null;
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                flo = commonsConfig.getFloat(key, null);
                break;
            }
        }
        return flo;
    }

    @Override
    public int getInt(String key) {
        Integer integer = readIntegerOptimistically(key);
        if (integer != null) {
            return integer;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            Integer integer = readIntegerOptimistically(key);
            return (integer != null) ? integer : defaultValue;
        } catch (ConversionException e) {
            return defaultValue;
        }
    }

    private Integer readIntegerOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Integer integer = readInteger(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                integer = readInteger(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return integer;
    }

    private Integer readInteger(String key) {
        Integer integer = null;
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                integer = commonsConfig.getInteger(key, null);
                break;
            }
        }
        return integer;
    }

    /**
     * @return Iterator of all keys grouped by the file in which they reside,
     *         from the main file up through ancestor files.
     */
    @Override
    public Iterator<String> getKeys() {
        final long stamp = lock.readLock();
        try {
            return readKeys();
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * @return Ordered list of all keys from all config files.
     */
    private Iterator<String> readKeys() {
        final List<String> allKeys = new ArrayList<>();
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            final Iterator<String> it = commonsConfig.getKeys();
            while (it.hasNext()) {
                allKeys.add(it.next());
            }
        }
        return allKeys.iterator();
    }

    @Override
    public long getLong(String key) {
        Long lon = readLongOptimistically(key);
        if (lon != null) {
            return lon;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        try {
            Long lon = readLongOptimistically(key);
            return (lon != null) ? lon : defaultValue;
        } catch (ConversionException e) {
            return defaultValue;
        }
    }

    private Long readLongOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Long lon = readLong(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                lon = readLong(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return lon;
    }

    private Long readLong(String key) {
        Long lon = null;
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                lon = commonsConfig.getLong(key, null);
                break;
            }
        }
        return lon;
    }

    @Override
    public Object getProperty(String key) {
        return readPropertyOptimistically(key);
    }

    private Object readPropertyOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Object prop = readProperty(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                prop = readProperty(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return prop;
    }

    private Object readProperty(String key) {
        Object prop = null;
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                prop = commonsConfig.getProperty(key);
                break;
            }
        }
        return prop;
    }

    @Override
    public String getString(String key) {
        return readStringOptimistically(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        String str = readStringOptimistically(key);
        return (str != null) ? str : defaultValue;
    }

    private String readStringOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        String str = readString(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                str = readString(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return str;
    }

    private String readString(String key) {
        String str = null;
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                str = commonsConfig.getString(key, null);
                break;
            }
        }
        return str;
    }

    @Override
    public void reload() throws ConfigurationException {
        final long stamp = lock.writeLock();
        try {
            final File mainConfigFile = getFile();
            if (mainConfigFile != null) {
                // Calculate the checksum of the file contents and compare it to
                // what has already been loaded. If the sums match, skip the
                // reload.
                try {
                    byte[] fileBytes = Files.readAllBytes(mainConfigFile.toPath());
                    final MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] digestBytes = md.digest(fileBytes);

                    if (digestBytes == mainContentsChecksum) {
                        return;
                    }
                    mainContentsChecksum = digestBytes;
                } catch (FileNotFoundException e) {
                    System.err.println("File not found: " + e.getMessage());
                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println(e.getMessage());
                }

                commonsConfigs.clear();
                loadFileAndAncestors(mainConfigFile);
            }
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * Saves all configuration files.
     *
     * @throws IOException If any of the files fail to save.
     */
    @Override
    public void save() throws IOException {
        final long stamp = lock.writeLock();
        try {
            for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
                commonsConfig.save();
            }
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * Attempts to set the given property in the first matching config file
     * that already contains the given key, searching the files in order from
     * main config file to most distant ancestor. If no config files already
     * contain the key, it will be set in the main file.
     */
    @Override
    public void setProperty(String key, Object value) {
        final long stamp = lock.writeLock();
        try {
            // Try to set it in the first matching config file that already
            // contains it.
            boolean wasSet = false;
            for (PropertiesConfiguration commonsConfig :
                    commonsConfigs.values()) {
                if (commonsConfig.containsKey(key)) {
                    commonsConfig.setProperty(key, value);
                    wasSet = true;
                }
            }
            // Fall back to setting it in the main config file.
            if (!wasSet) {
                Iterator<PropertiesConfiguration> it = commonsConfigs.values().iterator();
                if (it.hasNext()) {
                    PropertiesConfiguration commonsConfig = it.next();
                    commonsConfig.setProperty(key, value);
                }
            }
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * N.B.: Not thread-safe!
     */
    private void loadFileAndAncestors(File file) throws ConfigurationException {
        System.out.println("Loading config file: " + file);

        PropertiesConfiguration commonsConfig = new PropertiesConfiguration();
        // Prevent commas in values from being interpreted as list item
        // delimiters.
        commonsConfig.setDelimiterParsingDisabled(true);
        commonsConfig.setFile(file);
        try {
            commonsConfig.load();
            commonsConfigs.put(file, commonsConfig);

            // The file has been loaded. If it contains a key specifying a
            // parent file, load that too.
            String parent = commonsConfig.getString(EXTENDS_KEY);
            if (parent != null && parent.length() > 0) {
                // Expand paths that start with "~"
                parent = parent.replaceFirst("^~",
                        System.getProperty("user.home"));
                if (!parent.contains(File.separator)) {
                    parent = file.getParentFile().getAbsolutePath() +
                            File.separator + new File(parent).getName();
                }
                File parentFile = new File(parent).getCanonicalFile();
                if (!commonsConfigs.keySet().contains(parentFile)) {
                    loadFileAndAncestors(parentFile);
                } else {
                    throw new ConfigurationException("Inheritance loop");
                }
            }
        } catch (IOException |
                org.apache.commons.configuration.ConfigurationException e) {
            // The logger may not have been initialized yet, as it depends
            // on a working configuration. (Also, we don't want to
            // introduce a dependency on the logger.)
            System.err.println(e.getMessage());
        }
    }

}
