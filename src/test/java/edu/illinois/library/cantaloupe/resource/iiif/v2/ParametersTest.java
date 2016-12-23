package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParametersTest extends BaseTest {

    private static final float FUDGE = 0.00000001f;

    private Parameters params;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        params = new Parameters("identifier", "full", "full", "0", "default",
                "jpg");
    }

    @Test
    public void testFromUri() {
        params = Parameters.fromUri("bla/20,20,50,50/pct:90/15/bitonal.jpg");
        assertEquals("bla", params.getIdentifier().toString());
        assertEquals("20,20,50,50", params.getRegion().toString());
        assertEquals(90f, params.getSize().getPercent(), FUDGE);
        assertEquals(15f, params.getRotation().getDegrees(), FUDGE);
        assertEquals(Quality.BITONAL, params.getQuality());
        assertEquals(Format.JPG, params.getOutputFormat());

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

    @Test
    public void testCompareTo() {
        // TODO: write this
    }

    @Test
    public void testToOperationList() {
        // TODO: write this
    }

}
