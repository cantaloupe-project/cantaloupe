package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Used in logback.xml.
 */
public class ApplicationLogFilter extends Filter<ILoggingEvent> {

    /**
     * Filters out useless log messages.
     */
    public FilterReply decide(ILoggingEvent event) {
        // stop the Amazon S3 client from logging HTTP responses
        if (event.getLoggerName().startsWith("org.apache.http.") &&
                event.getLevel().equals(Level.DEBUG)) {
            return FilterReply.DENY;
        } else if (event.getLoggerName().equals("org.restlet") &&
                event.getLevel().equals(Level.INFO) &&
                event.getMessage().contains("ing the internal HTTP client")) {
            return FilterReply.DENY;
        } else if (event.getLoggerName().startsWith("org.restlet") &&
                event.getLevel().equals(Level.INFO) &&
                event.getMessage().startsWith("Starting") &&
                event.getMessage().endsWith("application")) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

}
