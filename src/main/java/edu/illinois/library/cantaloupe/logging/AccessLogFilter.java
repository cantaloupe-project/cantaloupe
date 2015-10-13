package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Used in logback.xml.
 */
public class AccessLogFilter extends Filter<ILoggingEvent> {

    /**
     * Rejects Restlet "Processing request to:" messages.
     */
    public FilterReply decide(ILoggingEvent event) {
        return event.getMessage().contains("Processing request to") ?
                FilterReply.DENY : FilterReply.NEUTRAL;
    }

}
