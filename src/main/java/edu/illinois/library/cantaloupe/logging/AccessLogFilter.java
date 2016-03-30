package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Used in logback.xml.
 */
public class AccessLogFilter extends Filter<ILoggingEvent> {

    public FilterReply decide(ILoggingEvent event) {
        // Accept Jetty access log messages; deny everything else.
        return event.getLoggerName().equals("LogService") ?
                FilterReply.ACCEPT : FilterReply.DENY;
    }

}
