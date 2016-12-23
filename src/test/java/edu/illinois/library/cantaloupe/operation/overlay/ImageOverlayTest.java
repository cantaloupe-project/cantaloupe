package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.*;

public class ImageOverlayTest extends BaseTest {

    private ImageOverlay instance;

    @Before
    public void setUp() throws IOException {
        instance = new ImageOverlay(TestUtil.getImage("jpg"),
                Position.BOTTOM_RIGHT, 5);
    }

    // getIdentifier()

    @Test
    public void testGetIdentifier() throws Exception {
        assertEquals("jpg", instance.getIdentifier());
        instance.setURL(new URL("http://example.org/dogs"));
        assertEquals("dogs", instance.getIdentifier());
    }

    // openStream()

    @Test
    public void testOpenStream() throws IOException {
        InputStream is = instance.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(is, os);
        assertEquals(5439, os.toByteArray().length);
    }

    @Test
    public void testOpenStreamWithNonexistentImage() throws IOException {
        instance = new ImageOverlay(new File("/dev/cats"),
                Position.BOTTOM_RIGHT, 5);
        try {
            instance.openStream();
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

    // toMap()

    @Test
    public void testToMap() {
        Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getIdentifier(), map.get("filename"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
    }

    // toString()

    @Test
    public void testToString() throws IOException {
        instance.setFile(TestUtil.getImage("jpg"));
        assertEquals("jpg_SE_5", instance.toString());
    }

}
