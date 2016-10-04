package edu.illinois.library.cantaloupe.config;

import org.apache.commons.configuration.ConversionException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Class for reading and writing JSON configuration files.
 */
class JsonConfiguration extends FileConfiguration implements Configuration {

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

    @SuppressWarnings("unchecked")
    public synchronized void reload() {
        final File configFile = getFile();
        if (configFile != null) {
            try {
                System.out.println("Reloading config file: " + configFile);

                // Quick-and-dirty JSON parser to avoid depending on Jackson,
                // which is hard to package into the standalone WAR.
                final byte[] encoded = Files.readAllBytes(configFile.toPath());
                final String json = new String(encoded, StandardCharsets.UTF_8);
                final String[] lines = json.replace("{", "").replace("}", "").
                        split(",");
                for (String line : lines) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        key = key.substring(1, key.length() - 1);
                        String value = parts[1].trim();
                        if (value.startsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        setProperty(key, value);
                    }
                }
            } catch (IOException e) {
                // The logger may not have been initialized yet, as it depends
                // on a working configuration. (Also, we don't want to
                // introduce a dependency on the logger.)
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Saves the configuration to the file returned by
     * {@link #getFile()}, if available.
     *
     * @throws IOException If there is a problem writing the file.
     */
    public void save() throws IOException {
        try (PrintWriter out = new PrintWriter(getFile())) {
            out.println(toJson());
        }
    }

    public void setProperty(String key, Object value) {
        configuration.put(key, value);
    }

    /**
     * @return JSON representation of the instance.
     */
    String toJson() {
        // Quick-and-dirty custom implementation to avoid depending on Jackson,
        // which is hard to package into the standalone WAR.
        final List<String> lines = new ArrayList<>();
        for (String key : configuration.keySet()) {
            String line = "    \"" + key + "\": ";
            Object value = configuration.get(key);
            if (value instanceof Number || value instanceof Boolean) {
                line += value.toString();
            } else if (value != null) {
                line += "\"" + value + "\"";
            }
            lines.add(line);
        }
        return "{\n" + StringUtils.join(lines, ",\n") + "\n}";
    }

}
