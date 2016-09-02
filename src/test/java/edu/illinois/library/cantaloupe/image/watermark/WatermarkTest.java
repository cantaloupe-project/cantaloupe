package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class WatermarkTest {

    @Test
    public void testConstructor() throws Exception {
        Watermark watermark = new Watermark();
        assertNull(watermark.getImage());
        assertNull(watermark.getPosition());
        assertEquals(0, watermark.getInset());
    }

    @Test
    public void testToMap() {
        // TODO: write this
    }

    @Test
    public void testToString() throws IOException {
        Watermark watermark = new Watermark();
        watermark.setImage(TestUtil.getImage("jpg"));
        watermark.setPosition(Position.BOTTOM_LEFT);
        watermark.setInset(10);
        assertEquals("jpg_SW_10", watermark.toString());
    }

}
