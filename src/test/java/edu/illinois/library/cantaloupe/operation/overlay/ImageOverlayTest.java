package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class ImageOverlayTest {

    private ImageOverlay instance;

    @Before
    public void setUp() {
        instance = new ImageOverlay(new File("/dev/cats"),
                Position.BOTTOM_RIGHT, 5);
    }

    @Test
    public void testToMap() {
        Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getImage().getName(), map.get("filename"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
    }

    @Test
    public void testToString() throws IOException {
        instance.setImage(TestUtil.getImage("jpg"));
        assertEquals("jpg_SE_5", instance.toString());
    }

}
