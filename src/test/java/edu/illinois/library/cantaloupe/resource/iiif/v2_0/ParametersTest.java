package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Quality;

public class ParametersTest extends CantaloupeTestCase {

    private Parameters params;

    public void testFromUri() {
        params = Parameters.fromUri("bla/20,20,50,50/pct:90/15/bitonal.jpg");
        assertEquals("bla", params.getIdentifier().toString());
        assertEquals("20,20,50,50", params.getRegion().toString());
        assertEquals(90f, params.getSize().getPercent());
        assertEquals(15f, params.getRotation().getDegrees());
        assertEquals(Quality.BITONAL, params.getQuality());
        assertEquals(OutputFormat.JPG, params.getOutputFormat());

        try {
            Parameters.fromUri("bla/20,20,50,50/15/bitonal.jpg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            Parameters.fromUri("bla/20,20,50,50/pct:90/15/bitonal");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    public void setUp() {
        params = new Parameters("identifier", "full", "full", "0", "default",
                "jpg");
    }

    public void testCompareTo() {
        // TODO: write this
    }

    public void testToOperations() {
        // TODO: write this
    }

}
