package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.Color;
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
        assertEquals(Color.BITONAL, Quality.BITONAL.toFilter());
        assertNull(Quality.COLOR.toFilter());
        assertNull(Quality.DEFAULT.toFilter());
        assertEquals(Color.GRAY, Quality.GRAY.toFilter());
    }

}
