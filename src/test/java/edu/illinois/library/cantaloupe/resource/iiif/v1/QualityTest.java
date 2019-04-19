package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QualityTest extends BaseTest {

    @Test
    void testToColorTransform() {
        assertEquals(ColorTransform.BITONAL, Quality.BITONAL.toColorTransform());
        assertNull(Quality.COLOR.toColorTransform());
        assertEquals(ColorTransform.GRAY, Quality.GREY.toColorTransform());
        assertNull(Quality.NATIVE.toColorTransform());
    }

}
