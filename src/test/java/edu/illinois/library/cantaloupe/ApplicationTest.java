package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ApplicationTest extends BaseTest {

    /**
     * getVersion() is not fully testable as it will return a different value
     * when the app is running from a .war.
     */
    @Test
    public void testGetVersion() {
        assertEquals("Unknown", Application.getVersion());
    }

}
