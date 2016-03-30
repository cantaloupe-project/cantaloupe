package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Used in logback.xml.
 */
public class ApplicationLogFilter extends Filter<ILoggingEvent> {

    public FilterReply decide(ILoggingEvent event) {
        // Filter out useless log messages.
        if (event.getLoggerName().equals("org.restlet") &&
                event.getLevel().equals(Level.INFO) &&
                event.getMessage().contains("ing the internal HTTP client")) {
            return FilterReply.DENY;
        }
        // Reject Jetty access log messages.
        if (event.getLoggerName().equals("LogService")) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

}
