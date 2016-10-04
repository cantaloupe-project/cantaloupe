package edu.illinois.library.cantaloupe.config;

import java.io.File;
import java.io.IOException;

abstract class FileConfiguration {

    public File getFile() {
        String configFilePath = System.
                getProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        if (configFilePath != null) {
            try {
                // expand paths that start with "~"
                configFilePath = configFilePath.replaceFirst("^~",
                        System.getProperty("user.home"));
                return new File(configFilePath).getCanonicalFile();
            } catch (IOException e) {
                // The logger may not have been initialized yet, as it depends
                // on a working configuration. (Also, we don't want to
                // introduce a dependency on the logger.)
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

}
