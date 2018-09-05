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
        // PDFBox log messages are unfortunately not calibrated very well.
        // Would be better to reduce their level, but we can't here...
        if (event.getLoggerName().startsWith("org.apache.pdfbox") &&
                event.getLevel().equals(Level.WARN)) {
            return FilterReply.DENY;
        }
        // Reject Jetty debug messages as they totally overwhelm the debug log.
        else if (event.getLoggerName().startsWith("org.eclipse.jetty") &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // Reject Jetty static content access messages.
        else if ("org.eclipse.jetty.server.ResourceService".equals(event.getLoggerName()) &&
                Level.INFO.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // Reject Jetty access log messages.
        else if (event.getLoggerName().equals("LogService")) {
            return FilterReply.DENY;
        }
        // Reject Velocity debug messages.
        else if (event.getLoggerName().startsWith("org.apache.velocity") &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // The Amazon S3 client wraps an Apache HTTP client which is extremely
        // verbose.
        else if ("org.apache.http.wire".equals(event.getLoggerName()) &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

}
