package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class ImageWatermarkTest {

    private ImageWatermark instance;

    @Before
    public void setUp() {
        instance = new ImageWatermark();
    }

    @Test
    public void testConstructor() throws Exception {
        assertNull(instance.getImage());
        assertNull(instance.getPosition());
        assertEquals(0, instance.getInset());
    }

    @Test
    public void testToMap() {
        instance.setImage(new File("/dev/cats"));
        instance.setInset(5);
        instance.setPosition(Position.BOTTOM_RIGHT);

        Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getImage().getName(), map.get("filename"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
    }

    @Test
    public void testToString() throws IOException {
        ImageWatermark watermark = new ImageWatermark();
        watermark.setImage(TestUtil.getImage("jpg"));
        watermark.setPosition(Position.BOTTOM_LEFT);
        watermark.setInset(10);
        assertEquals("jpg_SW_10", watermark.toString());
    }

}
