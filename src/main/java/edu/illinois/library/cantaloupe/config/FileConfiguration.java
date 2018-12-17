package edu.illinois.library.cantaloupe.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface FileConfiguration extends Configuration {

    /**
     * @return Configuration file based on the {@link
     *         ConfigurationFactory#CONFIG_VM_ARGUMENT}.
     */
    default Path getFile() {
        String configFilePath = System.
                getProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        if (configFilePath != null) {
            // expand paths that start with "~"
            configFilePath = configFilePath.replaceFirst("^~",
                    System.getProperty("user.home"));
            return Paths.get(configFilePath).toAbsolutePath();
        }
        return null;
    }

}
