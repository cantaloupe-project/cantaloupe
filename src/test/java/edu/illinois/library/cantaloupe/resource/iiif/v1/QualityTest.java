package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class QualityTest extends BaseTest {

    @Test
    public void testValues() {
        assertNotNull(Quality.valueOf("BITONAL"));
        assertNotNull(Quality.valueOf("COLOR"));
        assertNotNull(Quality.valueOf("GREY"));
        assertNotNull(Quality.valueOf("NATIVE"));
        assertEquals(4, Quality.values().length);
    }

    @Test
    public void testToColorTransform() {
        assertEquals(ColorTransform.BITONAL, Quality.BITONAL.toColorTransform());
        assertNull(Quality.COLOR.toColorTransform());
        assertEquals(ColorTransform.GRAY, Quality.GREY.toColorTransform());
        assertNull(Quality.NATIVE.toColorTransform());
    }

}
