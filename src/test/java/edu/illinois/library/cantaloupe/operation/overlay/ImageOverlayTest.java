package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ImageOverlayTest extends BaseTest {

    private ImageOverlay instance;

    @BeforeEach
    public void setUp() throws Exception {
        URI imageURI = TestUtil.getImage("jpg").toUri();
        instance = new ImageOverlay(imageURI, Position.BOTTOM_RIGHT, 5);
    }

    @Test
    void testOpenStream() throws Exception {
        try (InputStream is = instance.openStream();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            is.transferTo(os);
            assertEquals(1584, os.toByteArray().length);
        }
    }

    @Test
    void testOpenStreamWithNonexistentImage() {
        instance = new ImageOverlay(new File("/dev/cats").toURI(),
                Position.BOTTOM_RIGHT, 5);
        assertThrows(IOException.class, () -> {
            try (InputStream is = instance.openStream()) {
            }
        });
    }

    @Test
    void testSetURIThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setURI(new URI("http://example.org/cats")));
    }

    @Test
    void testToMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getURI().toString(), map.get("uri"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
    }

    @Test
    void testToMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() throws IOException {
        URI uri = TestUtil.getImage("jpg").toUri();
        instance.setURI(uri);
        assertEquals(uri.toString() + "_SE_5", instance.toString());
    }

}
