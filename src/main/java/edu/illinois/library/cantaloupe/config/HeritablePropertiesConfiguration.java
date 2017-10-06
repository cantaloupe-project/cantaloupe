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
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Properties configuration that allows file-based inheritance. A file can be
 * linked to a parent file using {@link #EXTENDS_KEY}. Keys in child files
 * override ones in ancestor files.
 */
class HeritablePropertiesConfiguration extends HeritableFileConfiguration
        implements Configuration {

    private static final String EXTENDS_KEY = "extends";

    /**
     * Map of PropertiesConfigurations in order from leaf to trunk.
     */
    private Map<File, PropertiesConfiguration> commonsConfigs =
            new LinkedHashMap<>();
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
        return commonsConfigs.keySet();
    }

    //////////////////////// Configuration methods //////////////////////////

    @Override
    public void clear() {
        commonsConfigs.values().stream().forEach(PropertiesConfiguration::clear);
        mainContentsChecksum = new byte[] {};
    }

    @Override
    public void clearProperty(String key) {
        commonsConfigs.values().stream().forEach(c -> c.clearProperty(key));
    }

    @Override
    public boolean getBoolean(String key) {
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                return commonsConfig.getBoolean(key);
            }
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
                if (commonsConfig.containsKey(key)) {
                    return commonsConfig.getBoolean(key, defaultValue);
                }
            }
        } catch (ConversionException e) {
            return defaultValue;
        }
        return defaultValue;
    }

    @Override
    public double getDouble(String key) {
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                return commonsConfig.getDouble(key);
            }
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        try {
            for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
                if (commonsConfig.containsKey(key)) {
                    return commonsConfig.getDouble(key, defaultValue);
                }
            }
        } catch (ConversionException e) {
            return defaultValue;
        }
        return defaultValue;
    }

    @Override
    public float getFloat(String key) {
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                return commonsConfig.getFloat(key);
            }
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        try {
            for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
                if (commonsConfig.containsKey(key)) {
                    return commonsConfig.getFloat(key, defaultValue);
                }
            }
        } catch (ConversionException e) {
            return defaultValue;
        }
        return defaultValue;
    }

    @Override
    public int getInt(String key) {
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                return commonsConfig.getInt(key);
            }
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
                if (commonsConfig.containsKey(key)) {
                    return commonsConfig.getInt(key, defaultValue);
                }
            }
        } catch (ConversionException e) {
            return defaultValue;
        }
        return defaultValue;
    }

    /**
     * @return Iterator of all keys grouped by the file in which they reside,
     *         from the main file up through ancestor files.
     */
    @Override
    public Iterator<String> getKeys() {
        // Compile an ordered list of all keys from all config files.
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
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                return commonsConfig.getLong(key);
            }
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        try {
            for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
                if (commonsConfig.containsKey(key)) {
                    return commonsConfig.getLong(key, defaultValue);
                }
            }
        } catch (ConversionException e) {
            return defaultValue;
        }
        return defaultValue;
    }

    @Override
    public Object getProperty(String key) {
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                return commonsConfig.getProperty(key);
            }
        }
        return null;
    }

    @Override
    public String getString(String key) {
        for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
            if (commonsConfig.containsKey(key)) {
                return commonsConfig.getString(key);
            }
        }
        return null;
    }

    @Override
    public String getString(String key, String defaultValue) {
        String str = getString(key);
        if (str == null) {
            str = defaultValue;
        }
        return str;
    }

    @Override
    public synchronized void reload() throws ConfigurationException {
        final File mainConfigFile = getFile();
        if (mainConfigFile != null) {
            // Calculate the checksum of the file contents and compare it to
            // what has already been loaded. If the checksums match, skip the
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
    }

    /**
     * Saves all configuration files.
     *
     * @throws IOException If any of the files fail to save.
     */
    @Override
    public synchronized void save() throws IOException {
        try {
            for (PropertiesConfiguration commonsConfig : commonsConfigs.values()) {
                commonsConfig.save();
            }
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Attempts to set the given property in the first matching config file
     * that already contains the given key, searching the files in order of
     * main config file to most distant ancestor. If no config files already
     * contain the key, it will be set in the main file.
     *
     * @param key
     * @param value
     */
    @Override
    public synchronized void setProperty(String key, Object value) {
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
    }

    private synchronized void loadFileAndAncestors(File file)
            throws ConfigurationException {
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
            System.out.println(e.getMessage());
        }
    }

}
