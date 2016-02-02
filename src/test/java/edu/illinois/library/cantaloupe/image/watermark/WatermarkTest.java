package edu.illinois.library.cantaloupe.image.watermark;

import org.junit.Test;

import static org.junit.Assert.*;

public class WatermarkTest {

    @Test
    public void testConstructor() throws Exception {
        Watermark watermark = new Watermark();
        assertNull(watermark.getImage());
        assertNull(watermark.getPosition());
        assertEquals(0, watermark.getInset());
    }

}
