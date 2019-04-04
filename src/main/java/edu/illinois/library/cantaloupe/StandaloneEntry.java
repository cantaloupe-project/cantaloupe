package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

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
     * <p>When provided (no value required), a list of available fonts will be
     * printed to stdout.</p>
     *
     * <p>The main reason this is a VM option and not a command-line argument
     * is that, due to the way the application is packaged, this class needs to
     * have as few dependencies as possible. All of the other would-be
     * arguments are VM options too, so let's preserve uniformity.</p>
     */
    static final String LIST_FONTS_VM_ARGUMENT = "cantaloupe.list_fonts";

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
     *     will be disabled. Should only be supplied when testing.</dd>
     * </dl>
     *
     * @param args       Ignored.
     * @throws Exception if there is a problem starting the web server.
     */
    public static void main(String... args) throws Exception {
        final Configuration config = Configuration.getInstance();
        if (config == null) {
            printUsage();
            exitUnlessTesting(-1);
        } else {
            Optional<Path> configFile = getConfigFile();
            if (configFile.isEmpty()) {
                printUsage();
                exitUnlessTesting(-1);
            } else if (!Files.exists(configFile.get())) {
                System.out.println("Does not exist: " + configFile);
                printUsage();
                exitUnlessTesting(-1);
            } else if (!Files.isRegularFile(configFile.get()) &&
                    !Files.isSymbolicLink(configFile.get())) {
                System.out.println("Not a file: " + configFile);
                printUsage();
                exitUnlessTesting(-1);
            } else if (!Files.isReadable(configFile.get())) {
                System.out.println("Not readable: " + configFile);
                printUsage();
                exitUnlessTesting(-1);
            }
        }
        getAppServer().start();
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
        System.out.println("\n" + usage());
    }

    /**
     * @return Program usage message.
     */
    static String usage() {
        return "Usage: java <VM options> -jar " + getWARFile().getName() +
                "\n\n" +
                "VM options:\n" +
                "-D" + ConfigurationFactory.CONFIG_VM_ARGUMENT + "=<config>" +
                "           Configuration file (REQUIRED)\n" +
                "-D" + LIST_FONTS_VM_ARGUMENT +
                "                List fonts\n";
    }

}
