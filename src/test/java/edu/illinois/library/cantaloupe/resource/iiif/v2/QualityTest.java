package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QualityTest extends BaseTest {

    @Test
    void testToColorTransform() {
        assertEquals(ColorTransform.BITONAL, Quality.BITONAL.toColorTransform());
        assertNull(Quality.COLOR.toColorTransform());
        assertNull(Quality.DEFAULT.toColorTransform());
        assertEquals(ColorTransform.GRAY, Quality.GRAY.toColorTransform());
    }

}
