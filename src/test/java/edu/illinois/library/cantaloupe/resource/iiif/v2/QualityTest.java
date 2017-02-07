package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class QualityTest extends BaseTest {

    @Test
    public void testValues() {
        assertNotNull(Quality.valueOf("BITONAL"));
        assertNotNull(Quality.valueOf("COLOR"));
        assertNotNull(Quality.valueOf("DEFAULT"));
        assertNotNull(Quality.valueOf("GRAY"));
        assertEquals(4, Quality.values().length);
    }

    @Test
    public void testToColorTransform() {
        assertEquals(ColorTransform.BITONAL, Quality.BITONAL.toColorTransform());
        assertNull(Quality.COLOR.toColorTransform());
        assertNull(Quality.DEFAULT.toColorTransform());
        assertEquals(ColorTransform.GRAY, Quality.GRAY.toColorTransform());
    }

}
