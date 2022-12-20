package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

/**
 * <p>Properties configuration that allows file-based inheritance. A file can
 * be linked to a parent file using {@link #EXTENDS_KEY}. Keys in child files
 * override ones in ancestor files.</p>
 *
 * <p>This implementation uses a {@link StampedLock} for good thread-safe
 * performance.</p>
 */
class HeritablePropertiesConfiguration implements MultipleFileConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HeritablePropertiesConfiguration.class);

    private static final String EXTENDS_KEY = "extends";

    private final StampedLock lock = new StampedLock();

    /**
     * Map of PropertiesConfigurations in order from leaf to trunk.
     */
    private final Map<Path,PropertiesDocument> propertiesDocs =
            new LinkedHashMap<>();

    /**
     * Checksum of the main configuration file contents. When the watcher
     * receives a change event from the filesystem, it will compute the new
     * checksum and reload only if they don't match. (That's because there are
     * often multiple events per change.)
     */
    private byte[] mainContentsChecksum = new byte[0];

    /**
     * @return Wrapped configurations in order from main to most distant
     *         ancestor.
     */
    List<PropertiesDocument> getConfigurationTree() {
        return new ArrayList<>(propertiesDocs.values());
    }

    ////////////////// MultipleFileConfiguration methods ///////////////////

    @Override
    public Set<Path> getFiles() {
        final long stamp = lock.readLock();
        try {
            return propertiesDocs.keySet();
        } finally {
            lock.unlock(stamp);
        }
    }

    //////////////////////// Configuration methods //////////////////////////

    @Override
    public void clear() {
        final long stamp = lock.writeLock();
        try {
            propertiesDocs.values().forEach(PropertiesDocument::clear);
            mainContentsChecksum = new byte[0];
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public void clearProperty(final String key) {
        final long stamp = lock.writeLock();
        try {
            propertiesDocs.values().forEach(doc -> doc.clearKey(key));
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
        } catch (NumberFormatException e) {
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

    @Nullable
    private Boolean readBoolean(@Nonnull String key) {
        Boolean bool = null;
        for (PropertiesDocument doc : propertiesDocs.values()) {
            if (doc.containsKey(key)) {
                bool = StringUtils.toBoolean(doc.get(key));
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
        } catch (NumberFormatException e) {
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
        for (PropertiesDocument doc : propertiesDocs.values()) {
            if (doc.containsKey(key)) {
                String value = doc.get(key);
                dub = Double.parseDouble(value);
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
        } catch (NumberFormatException e) {
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
        for (PropertiesDocument doc : propertiesDocs.values()) {
            if (doc.containsKey(key)) {
                String value = doc.get(key);
                flo = Float.parseFloat(value);
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
        } catch (NumberFormatException e) {
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
        for (PropertiesDocument doc : propertiesDocs.values()) {
            if (doc.containsKey(key)) {
                String value = doc.get(key);
                integer = Integer.parseInt(value);
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
        final List<String> allKeys = new ArrayList<>(Key.values().length + 100);
        for (PropertiesDocument doc : propertiesDocs.values()) {
            final Iterator<String> it = doc.getKeys();
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
        } catch (NumberFormatException e) {
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
        for (PropertiesDocument doc : propertiesDocs.values()) {
            if (doc.containsKey(key)) {
                String value = doc.get(key);
                lon = Long.parseLong(value);
                break;
            }
        }
        return lon;
    }

    @Nullable
    @Override
    public Object getProperty(@Nonnull String key) {
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
        for (PropertiesDocument doc : propertiesDocs.values()) {
            if (doc.containsKey(key)) {
                prop = doc.get(key);
                break;
            }
        }
        return prop;
    }

    @Nullable
    @Override
    public String getString(@Nonnull String key) {
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
        for (PropertiesDocument doc : propertiesDocs.values()) {
            if (doc.containsKey(key)) {
                str = doc.get(key);
                break;
            }
        }
        return str;
    }

    @Override
    public void reload() {
        getFile().ifPresent(mainConfigFile -> {
            final long stamp = lock.writeLock();
            try {
                // Calculate the checksum of the file contents and compare it
                // to what has already been loaded. If the sums match, skip the
                // reload.
                byte[] fileBytes = Files.readAllBytes(mainConfigFile);
                final MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digestBytes = md.digest(fileBytes);
                if (Arrays.equals(digestBytes, mainContentsChecksum)) {
                    return;
                }
                mainContentsChecksum = digestBytes;

                propertiesDocs.clear();
                loadFileAndAncestors(mainConfigFile);
            } catch (IOException | NoSuchAlgorithmException e) {
                LOGGER.error("reload(): {}", e.getMessage(), e);
            } finally {
                lock.unlock(stamp);
            }
        });
    }

    /**
     * Saves all configuration files.
     *
     * @throws IOException if any of the files fail to save.
     */
    @Override
    public void save() throws IOException {
        final long stamp = lock.writeLock();
        try {
            for (Map.Entry<Path,PropertiesDocument> entry : propertiesDocs.entrySet()) {
                entry.getValue().save(entry.getKey());
            }
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * Attempts to set the given property in the first matching config file
     * that already contains the given key, searching the files in order from
     * main config file to most distant ancestor. If no files already contain
     * the key, it will be set in the main file.
     */
    @Override
    public void setProperty(String key, Object value) {
        final long stamp = lock.writeLock();
        try {
            // Try to set it in the first matching config file that already
            // contains it.
            boolean wasSet = false;
            for (PropertiesDocument doc : propertiesDocs.values()) {
                if (doc.containsKey(key)) {
                    doc.set(key, value.toString());
                    wasSet = true;
                }
            }
            // Fall back to setting it in the main config file.
            if (!wasSet) {
                Iterator<PropertiesDocument> it = propertiesDocs.values().iterator();
                if (it.hasNext()) {
                    PropertiesDocument doc = it.next();
                    doc.set(key, value.toString());
                }
            }
        } finally {
            lock.unlock(stamp);
        }
    }

    private synchronized void loadFileAndAncestors(Path file) {
        System.out.println("Loading config file: " + file);

        PropertiesDocument doc = new PropertiesDocument();
        try {
            doc.load(file);
            propertiesDocs.put(file, doc);

            // The file has been loaded. If it contains a key specifying a
            // parent file, load that too.
            String parent = doc.get(EXTENDS_KEY);
            if (parent != null && !parent.isEmpty()) {
                // Expand paths that start with "~"
                parent = parent.replaceFirst("^~",
                        System.getProperty("user.home"));
                if (!parent.contains(File.separator)) {
                    parent = file.getParent().toAbsolutePath() +
                            File.separator + new File(parent).getName();
                }
                Path parentFile = Paths.get(parent).toAbsolutePath();
                if (!propertiesDocs.containsKey(parentFile)) {
                    loadFileAndAncestors(parentFile);
                } else {
                    LOGGER.error("Inheritance loop in {}", parent);
                }
            }
        } catch (IOException e) {
            // The logger may not have been initialized yet, as it depends
            // on a working configuration. (Also, we don't want to
            // introduce a dependency on the logger.)
            System.err.println(e.getMessage());
        }
    }

}
