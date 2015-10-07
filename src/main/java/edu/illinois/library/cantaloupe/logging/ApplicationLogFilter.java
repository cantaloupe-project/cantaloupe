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
        return (event.getLoggerName().equals("org.restlet") &&
                event.getLevel().equals(Level.INFO) &&
                event.getMessage().contains("Starting the internal HTTP client")) ?
                FilterReply.DENY : FilterReply.NEUTRAL;
    }

}
