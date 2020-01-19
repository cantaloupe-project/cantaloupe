package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.MissingConfigurationException;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Optional;

/**
 * <p>Serves as the main application class in a standalone context.</p>
 *
 * <p>This class will be unavailable in a Servlet container, so it should not
 * be referred to externally. It should also have as few non-JDK dependencies
 * as possible. When the non-JDK dependencies are changed, the part of the POM
 * that controls how the application is packaged will need to be updated as
 * well.</p>
 */
public class StandaloneEntry {

    /**
     * Prints a list of available fonts to stdout.
     */
    static final String LIST_FONTS_ARGUMENT = "-list-fonts";

    private static final String NEWLINE = System.getProperty("line.separator");

    private static ApplicationServer appServer;

    static {
        // Suppress a Dock icon in macOS.
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * Calls {@link System#exit(int)} unless {@link Application#isTesting()}
     * returns {@literal true}.
     *
     * @param status Process return status.
     */
    static void exitUnlessTesting(int status) {
        if (!Application.isTesting()) {
            System.exit(status);
        }
    }

    /**
     * <p>Checks the configuration VM option and starts the embedded web
     * container. The following configuration options are available:</p>
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
     * @param args       Ignored.
     * @throws Exception if there is a problem starting the web server.
     */
    public static void main(String... args) throws Exception {
        handleArguments(args);
        try {
            // Will throw an exception if the config VM argument is missing.
            Configuration.getInstance();
            Optional<Path> optConfigFile = getConfigFile();
            if (optConfigFile.isEmpty()) {
                printUsage();
                exitUnlessTesting(-1);
            } else {
                final Path configFile = optConfigFile.get();
                if (!Files.exists(configFile)) {
                    System.out.println("Does not exist: " + configFile);
                    printUsage();
                    exitUnlessTesting(-1);
                } else if (!Files.isRegularFile(configFile) &&
                        !Files.isSymbolicLink(configFile)) {
                    System.out.println("Not a file: " + configFile);
                    printUsage();
                    exitUnlessTesting(-1);
                } else if (!Files.isReadable(configFile)) {
                    System.out.println("Not readable: " + configFile);
                    printUsage();
                    exitUnlessTesting(-1);
                }
            }
            getAppServer().start();
        } catch (MissingConfigurationException e) {
            printUsage();
            exitUnlessTesting(-1);
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
                exitUnlessTesting(0);
            }
        }
    }

    private static Optional<Path> getConfigFile() {
        return Configuration.getInstance().getFile();
    }

    static File getWARFile() {
        ProtectionDomain protectionDomain =
                ApplicationServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        return new File(location.getFile());
    }

    /**
     * @return Application web server instance.
     */
    public static synchronized ApplicationServer getAppServer() {
        if (appServer == null) {
            appServer = new ApplicationServer(Configuration.getInstance());
        }
        return appServer;
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
                getWARFile().getName(), NEWLINE,                  // 1
                NEWLINE,                                          // 2
                NEWLINE,                                          // 3
                ConfigurationFactory.CONFIG_VM_ARGUMENT, NEWLINE, // 4
                NEWLINE,                                          // 5
                NEWLINE,                                          // 6
                LIST_FONTS_ARGUMENT, NEWLINE);                    // 7
    }

}
