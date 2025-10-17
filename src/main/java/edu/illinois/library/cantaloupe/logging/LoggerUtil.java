package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter2;
import edu.illinois.library.cantaloupe.config.Configuration;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Iterator;

public final class LoggerUtil {

    /**
     * Reloads the Logback configuration from logback.xml.
     */
    public static synchronized void reloadConfiguration() {
        Configuration appConfig = Configuration.getInstance();
        if (appConfig != null) {
            // Reset the logger context.
            LoggerContext loggerContext =
                    (LoggerContext) LoggerFactory.getILoggerFactory();

            // Clear any existing status listeners to prevent duplicate messages
            loggerContext.getStatusManager().clear();

            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(loggerContext);
            loggerContext.reset();

            // Copy logging-related configuration key/values into logger
            // context properties.
            final Iterator<String> it = appConfig.getKeys();
            while (it.hasNext()) {
                final String key = it.next();
                // EnvironmentConfiguration keys start with "LOG_";
                // all others start with "log."
                if (key.startsWith("log.") || key.startsWith("LOG_")) {
                    loggerContext.putProperty(key, appConfig.getString(key));
                }
            }

            // Finally, reload the Logback configuration.
            try (InputStream stream = LoggerUtil.class.getClassLoader().
                    getResourceAsStream("logback.xml")) {
                jc.doConfigure(stream);
            } catch (Exception e) {
                e.printStackTrace();
            }
            StatusPrinter2 statusPrinter = new StatusPrinter2();
            statusPrinter.printIfErrorsOccured(loggerContext);
        }
    }

    private LoggerUtil() {}

}
