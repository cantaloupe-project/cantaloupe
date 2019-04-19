package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilsTest extends BaseTest {

    @Test
    void testMillisecondsToHumanTime() {
        // seconds
        assertEquals("0 seconds", TimeUtils.millisecondsToHumanTime(50));
        assertEquals("1 second", TimeUtils.millisecondsToHumanTime(1000));
        assertEquals("2 seconds", TimeUtils.millisecondsToHumanTime(2000));

        // minutes
        assertEquals("1 minute, 1 second",
                TimeUtils.millisecondsToHumanTime(61000));
        assertEquals("2 minutes, 2 seconds",
                TimeUtils.millisecondsToHumanTime(122000));

        // hours
        assertEquals("1 hour",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60));
        assertEquals("1 hour, 1 second",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 + 1000));
        assertEquals("1 hour, 2 minutes, 1 second",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 + 120 * 1000 + 1000));

        // days
        assertEquals("1 day",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 * 24));
        assertEquals("1 day, 1 second",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 * 24 + 1000));
        assertEquals("1 day, 2 hours, 2 minutes, 1 second",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 * 24 + 60 * 60 * 2 * 1000 + 120 * 1000 + 1000));
    }

}
