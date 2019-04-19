package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StopwatchTest extends BaseTest {

    private Stopwatch instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Stopwatch();
    }

    @Test
    void testTimeElapsed() throws Exception {
        Thread.sleep(2);
        assertTrue(instance.timeElapsed() > 1);
    }

    @Test
    void testToString() {
        assertTrue(instance.toString().matches("\\d+ msec"));
    }

}