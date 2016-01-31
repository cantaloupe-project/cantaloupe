package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.OutputFormat;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParametersTest {

    private Parameters params;

    @Test
    public void testFromUri() {
        params = Parameters.fromUri("bla/20,20,50,50/pct:90/15/native.jpg");
        assertEquals("bla", params.getIdentifier().toString());
        assertEquals("20,20,50,50", params.getRegion().toString());
        assertEquals(90f, params.getSize().getPercent(), 0.0000001f);
        assertEquals(15f, params.getRotation().getDegrees(), 0.0000001f);
        assertEquals(Quality.NATIVE, params.getQuality());
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

    @Test
    public void testCompareTo() {
        // TODO: write this
    }

    @Test
    public void testToOperationList() {
        // TODO: write this
    }

}
