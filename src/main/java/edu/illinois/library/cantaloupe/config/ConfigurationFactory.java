package edu.illinois.library.cantaloupe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class ConfigurationFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ConfigurationFactory.class);

    public static final String CONFIG_VM_ARGUMENT = "cantaloupe.config";

    private static Configuration instance;

    public static synchronized void clearInstance() {
        if (instance != null) {
            instance = null;
        }
    }

    /**
     * Returns the shared application configuration instance. The
     * {@link #CONFIG_VM_ARGUMENT} VM argument must be set to an absolute or
     * relative pathname of a configuration file. It may also be set to the
     * string {@literal memory} to use an in-memory configuration (which will
     * be empty).
     *
     * @return Shared application configuration instance.
     * @throws MissingConfigurationException if the {@link #CONFIG_VM_ARGUMENT}
     *         VM argument is not set.
     */
    static synchronized Configuration getInstance() {
        if (instance == null) {
            // We are going to return a ConfigurationProvider with either a
            // MapConfiguration at position 0 (for testing) or an
            // EnvironmentConfiguration at position 0 and a
            // HeritablePropertiesConfiguration at position 1 (for production).
            final List<Configuration> configs = new ArrayList<>();

            final String configArg = System.getProperty(CONFIG_VM_ARGUMENT);
            if (configArg != null) {
                switch (configArg) {
                    case "memory": // we are in "test mode"
                        configs.add(new MapConfiguration());
                        break;
                    default:
                        configs.add(new EnvironmentConfiguration());
                        configs.add(new HeritablePropertiesConfiguration());
                        break;
                }
            } else {
                throw new MissingConfigurationException(
                        "Missing " + CONFIG_VM_ARGUMENT + " VM argument.");
            }

            configs.forEach(c -> {
                try {
                    c.reload();
                } catch (Exception e) {
                    LOGGER.error("getInstance(): {}", e.getMessage(), e);
                }
            });
            instance = new ConfigurationProvider(configs);
        }
        return instance;
    }

    private ConfigurationFactory() {}

}
