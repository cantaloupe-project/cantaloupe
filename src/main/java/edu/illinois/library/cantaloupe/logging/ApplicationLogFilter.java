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
        // Filter out useless PDFBox log messages. Would be better to reduce
        // their level, but we can't here...
        if (event.getLoggerName().startsWith("org.apache.pdfbox") &&
                event.getLevel().equals(Level.WARN)) {
            return FilterReply.DENY;
        }
        // Filter out debug messages from the embedded Jetty server as they
        // totally overwhelm the debug log.
        if (event.getLoggerName().startsWith("org.eclipse.jetty") &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // Jetty wants to log static content accesses at info level. We don't.
        if (event.getLoggerName().equals(org.eclipse.jetty.server.ResourceService.class.getName()) &&
                Level.INFO.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // Reject Jetty access log messages.
        if (event.getLoggerName().equals("LogService")) {
            return FilterReply.DENY;
        }
        // Reject Velocity debug messages.
        if (event.getLoggerName().startsWith("org.apache.velocity") &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // The Amazon S3 client wraps an Apache HTTP client which is extremely
        // verbose.
        if (event.getLoggerName().equals("org.apache.http.wire") &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

}
