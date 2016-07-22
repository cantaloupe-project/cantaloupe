package edu.illinois.library.cantaloupe.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filters out log messages with a level higher than INFO.
 */
public class StandardOutputLogFilter extends Filter<ILoggingEvent> {

    private static final Set<Level> EVENTS_TO_KEEP =
            new HashSet<>(Arrays.asList(Level.TRACE, Level.DEBUG, Level.INFO));

    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (EVENTS_TO_KEEP.contains(event.getLevel())) {
            return FilterReply.NEUTRAL;
        } else {
            return FilterReply.DENY;
        }
    }

}
