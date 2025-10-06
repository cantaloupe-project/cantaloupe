package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.MissingConfigurationException;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * <p>Main Spring Boot application class for Cantaloupe.</p>
 *
 * <p>This replaces the standalone StandaloneEntry class and integrates
 * Cantaloupe with Spring Boot's embedded server and dependency injection.</p>
 */
@SpringBootApplication(scanBasePackages = {
    "edu.illinois.library.cantaloupe.resource",
    "edu.illinois.library.cantaloupe.config.spring"
})
public class CantalouperApplication {

    /**
     * Prints a list of available fonts to stdout.
     */
    static final String LIST_FONTS_ARGUMENT = "-list-fonts";

    private static final String NEWLINE = System.getProperty("line.separator");

    static {
        // Suppress a Dock icon in macOS.
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * <p>Main method that starts the Spring Boot application. The following
     * configuration options are available:</p>
     *
     * <dl>
     *     <dt><code>-Dcantaloupe.config</code></dt>
     *     <dd>Use the configuration file at the corresponding pathname.
     *     Required.</dd>
     *     <dt><code>-Dcantaloupe.test</code></dt>
     *     <dd>If set to <code>true</code>, calls to {@link System#exit(int)}
     *     are disabled. Should only be supplied when testing.</dd>
     * </dl>
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        try {
            handleArguments(args);

            // Check configuration before starting Spring
            Configuration.getInstance();
            Optional<Path> optConfigFile = getConfigFile();
            if (optConfigFile.isEmpty()) {
                printUsage();
                SystemUtils.exit(-1);
                return;
            } else {
                final Path configFile = optConfigFile.get();
                if (!Files.exists(configFile)) {
                    System.out.println("Does not exist: " + configFile);
                    printUsage();
                    SystemUtils.exit(-1);
                    return;
                } else if (!Files.isRegularFile(configFile) &&
                        !Files.isSymbolicLink(configFile)) {
                    System.out.println("Not a file: " + configFile);
                    printUsage();
                    SystemUtils.exit(-1);
                    return;
                } else if (!Files.isReadable(configFile)) {
                    System.out.println("Not readable: " + configFile);
                    printUsage();
                    SystemUtils.exit(-1);
                    return;
                }
            }

            // Start Spring Boot application
            ConfigurableApplicationContext context = SpringApplication.run(CantalouperApplication.class, args);

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (context.isActive()) {
                    context.close();
                }
            }));

        } catch (MissingConfigurationException e) {
            printUsage();
            SystemUtils.exit(-1);
        } catch (Exception e) {
            System.err.println("Failed to start Cantaloupe: " + e.getMessage());
            e.printStackTrace();
            SystemUtils.exit(-1);
        }
    }

    private static void handleArguments(String... args) {
        if (args.length > 0) {
            if (LIST_FONTS_ARGUMENT.equals(args[0])) {
                GraphicsEnvironment ge =
                        GraphicsEnvironment.getLocalGraphicsEnvironment();
                for (String family : ge.getAvailableFontFamilyNames()) {
                    System.out.println(family);
                }
                SystemUtils.exit(0);
            }
        }
    }

    private static Optional<Path> getConfigFile() {
        return Configuration.getInstance().getFile();
    }

    static File getJARFile() {
        return new File(CantalouperApplication.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getFile());
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
                getJARFile().getName(), NEWLINE,                  // 1
                NEWLINE,                                          // 2
                NEWLINE,                                          // 3
                ConfigurationFactory.CONFIG_VM_ARGUMENT, NEWLINE, // 4
                NEWLINE,                                          // 5
                NEWLINE,                                          // 6
                LIST_FONTS_ARGUMENT, NEWLINE);                    // 7
    }
}
