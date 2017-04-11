package edu.illinois.library.cantaloupe.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is public in order to be accessible from Ruby via the JRuby-Java
 * bridge.
 */
public abstract class Helper {

    private static Logger logger = LoggerFactory.getLogger(Helper.class);

    /**
     * <p>Public accessor for accessing the Logger instance from the delegate
     * script.</p>
     *
     * <p>Example Ruby invocation:</p>
     *
     * <pre>Java::edu.illinois.library.cantaloupe.script.Helper.logger.debug('message')</pre>
     *
     * @return Shared Logger instance.
     */
    public static Logger logger() {
        return logger;
    }

}
