package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
// StatusPrinter2 removed for Spring Boot compatibility
import edu.illinois.library.cantaloupe.config.Configuration;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Iterator;

public final class LoggerUtil {

    /**
     * Placeholder method for Spring Boot compatibility.
     * In Spring Boot, logging configuration is handled automatically.
     */
    public static synchronized void reloadConfiguration() {
        // No-op for Spring Boot - logging is configured via application.properties
        // or logback-spring.xml if custom configuration is needed
    }

    private LoggerUtil() {}

}
