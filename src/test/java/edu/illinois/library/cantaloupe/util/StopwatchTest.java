package edu.illinois.library.cantaloupe.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class StopwatchTest {

    private Stopwatch instance;

    @Before
    public void setUp() {
        instance = new Stopwatch();
    }

    @Test
    public void testTimeElapsed() throws Exception {
        Thread.sleep(2);
        assertTrue(instance.timeElapsed() > 1);
    }

    @Test
    public void testToString() {
        assertTrue(instance.toString().matches("\\d+ msec"));
    }

}