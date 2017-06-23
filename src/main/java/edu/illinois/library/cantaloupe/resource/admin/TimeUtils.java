package edu.illinois.library.cantaloupe.resource.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

abstract class TimeUtils {

    static String millisecondsToHumanTime(long msec) {
        final long days = TimeUnit.MILLISECONDS.toDays(msec);
        msec -= TimeUnit.DAYS.toMillis(days);
        final long hours = TimeUnit.MILLISECONDS.toHours(msec);
        msec -= TimeUnit.HOURS.toMillis(hours);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(msec);
        msec -= TimeUnit.MINUTES.toMillis(minutes);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(msec);

        final List<String> parts = new ArrayList<>(4);

        if (days > 0) {
            parts.add(days + pluralize(" day", days));
        }
        if (hours > 0) {
            parts.add(hours + pluralize(" hour", hours));
        }
        if (minutes > 0) {
            parts.add(minutes + pluralize(" minute", minutes));
        }
        if (seconds > 0 || (days < 1 && hours < 1 && minutes < 1)) {
            parts.add(seconds + pluralize(" second", seconds));
        }

        return String.join(", ", parts);
    }

    private static String pluralize(String unit, long number) {
        if (number != 1) {
            unit += "s";
        }
        return unit;
    }

}
