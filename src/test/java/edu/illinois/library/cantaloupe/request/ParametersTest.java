package edu.illinois.library.cantaloupe.request;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class ParametersTest extends CantaloupeTestCase {

    private Parameters parameters;

    public void setUp() {
        parameters = new Parameters("identifier", "full", "full", "0",
                "default", "jpg");
    }

    public void testIsUnmodified() {
        parameters = new Parameters("identifier", "full", "full", "0",
                "default", "jpg");
        assertFalse(parameters.isRequestingUnmodifiedSource());

        parameters = new Parameters("identifier.jpg", "full", "full", "0",
                "default", "jpg");
        assertTrue(parameters.isRequestingUnmodifiedSource());

        parameters = new Parameters("identifier.gif", "full", "full", "0",
                "default", "gif");
        assertTrue(parameters.isRequestingUnmodifiedSource());

        parameters = new Parameters("identifier.gif", "30,30,30,30", "full", "0",
                "default", "gif");
        assertFalse(parameters.isRequestingUnmodifiedSource());

        parameters = new Parameters("identifier.gif", "full", "pct:100", "0",
                "default", "gif");
        assertTrue(parameters.isRequestingUnmodifiedSource());

        parameters = new Parameters("identifier.gif", "full", "pct:50", "0",
                "default", "gif");
        assertFalse(parameters.isRequestingUnmodifiedSource());

        parameters = new Parameters("identifier.gif", "full", "full", "2",
                "default", "gif");
        assertFalse(parameters.isRequestingUnmodifiedSource());

        parameters = new Parameters("identifier.gif", "full", "full", "!0",
                "default", "gif");
        assertTrue(parameters.isRequestingUnmodifiedSource());

        parameters = new Parameters("identifier.gif", "full", "full", "0",
                "color", "gif");
        assertTrue(parameters.isRequestingUnmodifiedSource());
    }

    public void testToString() {
        String expected = "identifier/full/full/0/default.jpg";
        assertEquals(expected, parameters.toString());
    }

}
