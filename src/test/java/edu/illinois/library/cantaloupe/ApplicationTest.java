package edu.illinois.library.cantaloupe;

import org.junit.Test;

import static org.junit.Assert.*;

public class ApplicationTest {

    /**
     * getVersion() is not fully testable as it will return a different value
     * when the app is running from a .war.
     */
    @Test
    public void testGetVersion() {
        assertEquals("SNAPSHOT", Application.getVersion());
    }

}
