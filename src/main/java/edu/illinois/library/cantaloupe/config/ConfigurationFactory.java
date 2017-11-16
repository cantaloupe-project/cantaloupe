package edu.illinois.library.cantaloupe.config;

public final class ConfigurationFactory {

    public static final String CONFIG_VM_ARGUMENT = "cantaloupe.config";

    private static volatile Configuration instance;

    public static synchronized void clearInstance() {
        if (instance != null) {
            instance.stopWatching();
            instance = null;
        }
    }

    /**
     * Returns the shared application configuration instance. The
     * {@link #CONFIG_VM_ARGUMENT} VM argument must be set to an absolute or
     * relative pathname of a configuration file. It may also be set to the
     * string <code>memory</code> to use an in-memory configuration.
     *
     * @return Shared application configuration instance.
     * @throws RuntimeException If the {@link #CONFIG_VM_ARGUMENT} VM argument
     *                          is not set.
     */
    static Configuration getInstance() {
        Configuration config = instance;
        if (config == null) {
            synchronized (ConfigurationFactory.class) {
                config = instance;
                if (config == null) {
                    final String configArg = System.getProperty(CONFIG_VM_ARGUMENT);
                    if (configArg != null) {
                        if (configArg.equals("memory")) {
                            config = new MemoryConfiguration();
                        } else {
                            config = new HeritablePropertiesConfiguration();
                        }
                        try {
                            config.reload();
                        } catch (Exception e) {
                            System.err.println("ConfigurationFactory.getInstance(): " +
                                    e.getMessage());
                        }
                        instance = config;
                    } else {
                        throw new RuntimeException("Missing " +
                                CONFIG_VM_ARGUMENT + " VM option.");
                    }
                }
            }
        }
        return config;
    }

    private ConfigurationFactory() {}

}
