package edu.illinois.library.cantaloupe.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {

    public static String millisecondsToHumanTime(long msec) {
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

    /**
     * @param seconds Number of seconds.
     * @return String in {@code hh:mm:ss} format.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public static String toHMS(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Argument is negative");
        }
        int h = (int) Math.floor(seconds / 60.0 / 60.0);
        int m = (int) Math.floor(seconds / 60.0) - h * 60;
        int s = seconds - m * 60 - h * 60 * 60;
        return (String.format("%2s", h) + ":" + String.format("%2s", m) + ":" +
                String.format("%2s", s)).replace(' ', '0');
    }

    /**
     * @param hms String in {@code hh:mm:ss} format.
     * @return Number of seconds in the given string.
     * @throws IllegalArgumentException if the argument is invalid.
     */
    public static int toSeconds(String hms) {
        String[] parts = hms.split(":");
        if (parts.length == 3) {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int s = Integer.parseInt(parts[2]);
            if (h < 0 || m < 0 || s < 0) {
                throw new IllegalArgumentException(
                        "Argument contains a negative component");
            }
            return h * 60 * 60 + m * 60 + s;
        } else {
            throw new IllegalArgumentException("Unrecognized format: " + hms);
        }
    }

    private TimeUtils() {}

}
