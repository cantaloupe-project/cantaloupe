package edu.illinois.library.cantaloupe.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public interface FileConfiguration extends Configuration {

    /**
     * @return Configuration file based on the {@link
     *         ConfigurationFactory#CONFIG_VM_ARGUMENT}.
     */
    @Override
    default Optional<Path> getFile() {
        String configFilePath = System.
                getProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        if (configFilePath != null) {
            // expand paths that start with "~"
            configFilePath = configFilePath.replaceFirst("^~",
                    System.getProperty("user.home"));
            return Optional.of(Paths.get(configFilePath).toAbsolutePath());
        }
        return Optional.empty();
    }

    /**
     * This default implementation uses the {@link Iterator} returned by {@link
     * #getKeys} in conjunction with {@link #getProperty(String)} to build a
     * map. Implementations should override if they can do it more efficiently.
     *
     * @return Configuration keys and values in a read-only map.
     */
    default Map<String,Object> toMap() {
        final Map<String,Object> map = new LinkedHashMap<>();
        final Iterator<String> keys = getKeys();
        while (keys.hasNext()) {
            final String key = keys.next();
            map.put(key, getProperty(key));
        }
        return Collections.unmodifiableMap(map);
    }

}
