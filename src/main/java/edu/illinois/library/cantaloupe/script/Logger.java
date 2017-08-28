package edu.illinois.library.cantaloupe.script;

import org.slf4j.LoggerFactory;

/**
 * This class is public in order to be accessible from Ruby via the JRuby-Java
 * bridge.
 */
public final class Logger {

    private static final org.slf4j.Logger logger =
            LoggerFactory.getLogger(ScriptEngine.class);

    public static void debug(String message) {
        logger.debug(message);
    }

    public static void error(String message) {
        logger.error(message);
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void trace(String message) {
        logger.trace(message);
    }

    private Logger() {}

}
