package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.MissingConfigurationException;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PostConstruct;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Spring Boot main application class for Cantaloupe image server.
 */
@SpringBootApplication
public class CantaloupeApplication {

    /**
     * Prints a list of available fonts to stdout.
     */
    static final String LIST_FONTS_ARGUMENT = "-list-fonts";

    private static final String NEWLINE = System.getProperty("line.separator");

    static {
        // Suppress a Dock icon in macOS.
        System.setProperty("java.awt.headless", "true");
    }

    public static void main(String[] args) {
        handleArguments(args);

        try {
            // Validate configuration before starting Spring Boot
            validateConfiguration();

            SpringApplication.run(CantaloupeApplication.class, args);

        } catch (MissingConfigurationException e) {
            printUsage();
            SystemUtils.exit(-1);
        } catch (Exception e) {
            System.err.println("Failed to start Cantaloupe: " + e.getMessage());
            SystemUtils.exit(-1);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("Cantaloupe image server started successfully on Spring Boot!");

        try {
            Configuration config = Configuration.getInstance();
            boolean httpEnabled = config.getBoolean(Key.HTTP_ENABLED, true);
            boolean httpsEnabled = config.getBoolean(Key.HTTPS_ENABLED, false);

            if (httpEnabled) {
                int port = config.getInt(Key.HTTP_PORT, 8182);
                String host = config.getString(Key.HTTP_HOST, "0.0.0.0");
                System.out.println("HTTP server running on http://" + host + ":" + port);
            }

            if (httpsEnabled) {
                int httpsPort = config.getInt(Key.HTTPS_PORT, 8183);
                String httpsHost = config.getString(Key.HTTPS_HOST, "0.0.0.0");
                System.out.println("HTTPS server running on https://" + httpsHost + ":" + httpsPort);
            }
        } catch (Exception e) {
            System.err.println("Could not determine server configuration: " + e.getMessage());
        }
    }

    private static void handleArguments(String... args) {
        if (args.length > 0) {
            if (LIST_FONTS_ARGUMENT.equals(args[0])) {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                for (String family : ge.getAvailableFontFamilyNames()) {
                    System.out.println(family);
                }
                SystemUtils.exit(0);
            }
        }
    }

    private static void validateConfiguration() throws MissingConfigurationException {
        // Will throw an exception if the config VM argument is missing.
        Configuration.getInstance();

        Optional<Path> optConfigFile = getConfigFile();
        if (optConfigFile.isEmpty()) {
            printUsage();
            SystemUtils.exit(-1);
        } else {
            final Path configFile = optConfigFile.get();
            if (!Files.exists(configFile)) {
                System.out.println("Does not exist: " + configFile);
                printUsage();
                SystemUtils.exit(-1);
            } else if (!Files.isRegularFile(configFile) && !Files.isSymbolicLink(configFile)) {
                System.out.println("Not a file: " + configFile);
                printUsage();
                SystemUtils.exit(-1);
            } else if (!Files.isReadable(configFile)) {
                System.out.println("Not readable: " + configFile);
                printUsage();
                SystemUtils.exit(-1);
            }
        }
    }

    private static Optional<Path> getConfigFile() {
        return Configuration.getInstance().getFile();
    }

    /**
     * Prints program usage to {@link System#out}.
     */
    private static void printUsage() {
        System.out.println(NEWLINE + usage());
    }

    /**
     * @return Program usage message.
     */
    static String usage() {
        return String.format("Usage: java <VM args> -jar %s <command args>%s" + // 1
                        "%s" +                                                  // 2
                        "VM arguments:%s" +                                     // 3
                        "  -D%s=<path>       Configuration file (REQUIRED)%s" + // 4
                        "%s" +                                                  // 5
                        "Command arguments:%s" +                                // 6
                        "  %s                      List fonts%s",               // 7
                "cantaloupe-6.0-SNAPSHOT.jar", NEWLINE,                  // 1
                NEWLINE,                                          // 2
                NEWLINE,                                          // 3
                ConfigurationFactory.CONFIG_VM_ARGUMENT, NEWLINE, // 4
                NEWLINE,                                          // 5
                NEWLINE,                                          // 6
                LIST_FONTS_ARGUMENT, NEWLINE);                    // 7
    }
}
