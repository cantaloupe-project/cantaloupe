package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Filter;

public class QualityTest extends CantaloupeTestCase {

    public void testValues() {
        assertNotNull(Quality.valueOf("BITONAL"));
        assertNotNull(Quality.valueOf("COLOR"));
        assertNotNull(Quality.valueOf("GRAY"));
        assertNotNull(Quality.valueOf("NATIVE"));
        assertEquals(4, Quality.values().length);
    }

    public void testToFilter() {
        assertEquals(Filter.BITONAL, Quality.BITONAL.toFilter());
        assertNull(Quality.COLOR.toFilter());
        assertEquals(Filter.GRAY, Quality.GRAY.toFilter());
        assertNull(Quality.NATIVE.toFilter());
    }

}
