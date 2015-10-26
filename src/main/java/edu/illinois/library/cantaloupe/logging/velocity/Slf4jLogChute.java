package edu.illinois.library.cantaloupe.logging.velocity;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log chute for Velocity to use slf4j for logging. Velocity 2.0 has a
 * Slf4jLogChute class built-in, but it has not been released yet as of
 * 10/2015.
 *
 * @see <a href="https://velocity.apache.org/engine/devel/apidocs/index.html?org/apache/velocity/slf4j/Slf4jLogChute.html">
 *     Velocity 2.0 javadoc</a>
 */
public class Slf4jLogChute implements LogChute {

    private static Logger logger = LoggerFactory.getLogger(Slf4jLogChute.class);

    @Override
    public void init(RuntimeServices runtimeServices) throws Exception {
    }

    @Override
    public void log(int level, String message) {
        switch (level) {
            case DEBUG_ID:
                logger.debug(message);
                break;
            case INFO_ID:
                logger.info(message);
                break;
            case WARN_ID:
                logger.warn(message);
                break;
            case ERROR_ID:
                logger.error(message);
                break;
        }
    }

    @Override
    public void log(int level, String message, Throwable throwable) {
        switch (level) {
            case DEBUG_ID:
                logger.debug(message, throwable);
                break;
            case INFO_ID:
                logger.info(message, throwable);
                break;
            case WARN_ID:
                logger.warn(message, throwable);
                break;
            case ERROR_ID:
                logger.error(message, throwable);
                break;
        }
    }

    @Override
    public boolean isLevelEnabled(int level) {
        boolean result = false;
        switch (level) {
            case DEBUG_ID:
                result = logger.isDebugEnabled();
                break;
            case INFO_ID:
                result = logger.isInfoEnabled();
                break;
            case WARN_ID:
                result = logger.isWarnEnabled();
                break;
            case ERROR_ID:
                result = logger.isErrorEnabled();
                break;
        }
        return result;
    }

}
