package edu.illinois.library.cantaloupe.request;

import junit.framework.TestCase;

public class ParametersTest extends TestCase {

    private Parameters parameters;

    public void setUp() {
        parameters = new Parameters("identifier", "full", "full", "0",
                "default", "jpg");
    }

    public void testGetCanonicalUri() {
        String expected = "http://example.org/iiif/identifier/full/full/0/default.jpg";
        String actual = parameters.getCanonicalUri("http://example.org/iiif");
        assertEquals(expected, actual);

        expected = "http://example.org/iiif/identifier/full/full/0/default.jpg";
        actual = parameters.getCanonicalUri("http://example.org/iiif/");
        assertEquals(expected, actual);
    }

}
