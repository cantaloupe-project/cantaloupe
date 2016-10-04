package edu.illinois.library.cantaloupe.config;

import java.io.IOException;

public abstract class ConfigurationFactory {

    public static final String CONFIG_FILE_VM_ARGUMENT = "cantaloupe.config"; // TODO: rename to CONFIG_VM_ARGUMENT

    private static volatile Configuration instance;
    private static final Object lock = new Object();

    public static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * @return Global application configuration instance.
     */
    public static Configuration getInstance() {
        Configuration config = instance;
        if (config == null) {
            synchronized (lock) {
                config = instance;
                if (config == null) {
                    config = new PropertiesConfiguration();
                    try {
                        config.reload();
                    } catch (IOException e) {
                        System.err.println("ConfigurationFactory.getInstance(): " +
                                e.getMessage());
                    }
                    instance = config;
                }
            }
        }
        return config;
    }

}
