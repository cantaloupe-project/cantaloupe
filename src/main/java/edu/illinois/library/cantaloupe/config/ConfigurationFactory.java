package edu.illinois.library.cantaloupe.config;

import java.io.IOException;

public abstract class ConfigurationFactory {

    public static final String CONFIG_VM_ARGUMENT = "cantaloupe.config";

    private static volatile Configuration instance;
    private static final Object lock = new Object();

    public static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * @return Shared application configuration instance.
     */
    public static Configuration getInstance() {
        Configuration config = instance;
        if (config == null) {
            synchronized (lock) {
                config = instance;
                if (config == null) {
                    final String configArg = System.getProperty(CONFIG_VM_ARGUMENT);
                    if (configArg == null || configArg.length() < 1) {
                        config = new EnvironmentConfiguration();
                    } else if (configArg.endsWith(".json")) {
                        config = new JsonConfiguration();
                    } else if (configArg.equals("memory")) {
                        config = new MemoryConfiguration();
                    } else {
                        config = new PropertiesConfiguration();
                    }
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
