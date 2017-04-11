package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Used in logback.xml to filter out log messages with a severity lower than
 * WARN.
 */
public class ApplicationErrorLogFilter extends Filter<ILoggingEvent> {

    public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel().levelInt < Level.WARN_INT) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

}
