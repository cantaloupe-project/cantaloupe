package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Filter;
import org.junit.Test;

import static org.junit.Assert.*;

public class QualityTest {

    @Test
    public void testValues() {
        assertNotNull(Quality.valueOf("BITONAL"));
        assertNotNull(Quality.valueOf("COLOR"));
        assertNotNull(Quality.valueOf("DEFAULT"));
        assertNotNull(Quality.valueOf("GRAY"));
        assertEquals(4, Quality.values().length);
    }

    @Test
    public void testToFilter() {
        assertEquals(Filter.BITONAL, Quality.BITONAL.toFilter());
        assertNull(Quality.COLOR.toFilter());
        assertNull(Quality.DEFAULT.toFilter());
        assertEquals(Filter.GRAY, Quality.GRAY.toFilter());
    }

}
