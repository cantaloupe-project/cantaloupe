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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.Assert.*;

public class ImageOverlayTest extends BaseTest {

    private ImageOverlay instance;

    @Before
    public void setUp() throws IOException {
        instance = new ImageOverlay(TestUtil.getImage("jpg"),
                Position.BOTTOM_RIGHT, 5);
    }

    @Test
    public void getIdentifier() throws Exception {
        assertEquals("jpg", instance.getIdentifier());
        instance.setURI(new URI("http://example.org/dogs"));
        assertEquals("dogs", instance.getIdentifier());
    }

    @Test
    public void openStream() throws IOException {
        InputStream is = instance.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(is, os);
        assertEquals(5439, os.toByteArray().length);
    }

    @Test(expected = IOException.class)
    public void openStreamWithNonexistentImage() throws IOException {
        instance = new ImageOverlay(new File("/dev/cats"),
                Position.BOTTOM_RIGHT, 5);
        instance.openStream();
    }

    @Test(expected = IllegalStateException.class)
    public void setFileThrowsExceptionWhenFrozen() {
        instance.freeze();
        instance.setFile(new File("/dev/null"));
    }

    @Test(expected = IllegalStateException.class)
    public void setURIThrowsExceptionWhenFrozen() throws Exception {
        instance.freeze();
        try {
            instance.setURI(new URI("http://example.org/cats"));
        } catch (URISyntaxException e) {
            fail();
        }
    }

    @Test
    public void toMap() {
        Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getIdentifier(), map.get("filename"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        map.put("test", "test");
    }

    @Test
    public void testToString() throws IOException {
        instance.setFile(TestUtil.getImage("jpg"));
        assertEquals("jpg_SE_5", instance.toString());
    }

}
