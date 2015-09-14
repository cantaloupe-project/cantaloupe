package edu.illinois.library.cantaloupe.request;

import junit.framework.TestCase;

public class ParametersTest extends TestCase {

    private Parameters parameters;

    public void setUp() {
        parameters = new Parameters("identifier", "full", "full", "0",
                "default", "jpg");
    }

    public void testToString() {
        String expected = "identifier/full/full/0/default.jpg";
        assertEquals(expected, parameters.toString());
    }

}
