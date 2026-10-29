package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilsTest extends BaseTest {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Other tests (e.g. StandaloneEntryTest) may leave an exit request
        // behind, and surefire runs test classes in random order.
        SystemUtils.clearExitRequest();
    }

    @Test
    void testClearExitRequest() {
        SystemUtils.exit(5);
        assertTrue(SystemUtils.exitRequested());
        assertEquals(5, SystemUtils.requestedExitCode());

        SystemUtils.clearExitRequest();
        assertFalse(SystemUtils.exitRequested());
    }

    @Test
    void testExitRequested() {
        assertFalse(SystemUtils.exitRequested());
        SystemUtils.exit(0);
        assertTrue(SystemUtils.exitRequested());
    }

    @Test
    void testRequestedCode() {
        SystemUtils.exit(5);
        assertEquals(5, SystemUtils.requestedExitCode());
    }

}