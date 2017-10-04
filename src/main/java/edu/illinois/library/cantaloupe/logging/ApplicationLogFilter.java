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
        // Filter out useless Restlet log messages.
        if (event.getLoggerName().startsWith("org.restlet") &&
                event.getLevel().equals(Level.INFO) &&
                event.getMessage().contains("ing the internal HTTP client")) {
            return FilterReply.DENY;
        }
        // Filter out debug messages from the embedded Jetty server as they
        // totally overwhelm the debug log.
        if (event.getLoggerName().startsWith("org.eclipse.jetty") &&
                event.getLevel().equals(Level.DEBUG)) {
            return FilterReply.DENY;
        }
        // Reject Jetty access log messages.
        if (event.getLoggerName().equals("LogService")) {
            return FilterReply.DENY;
        }
        // Reject Velocity debug messages.
        if (event.getLoggerName().startsWith("org.apache.velocity") &&
                event.getLevel().equals(Level.DEBUG)) {
            return FilterReply.DENY;
        }
        // The Amazon S3 client logs request/response bodies in binary. These
        // should really be trace-level.
        if (event.getLoggerName().equals("org.apache.http.wire") &&
                event.getLevel().equals(Level.DEBUG)) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

}
