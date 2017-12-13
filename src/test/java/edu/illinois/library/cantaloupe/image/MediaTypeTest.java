package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.*;

public class MediaTypeTest {

    private static final Map<Format, Path> files = new HashMap<>();

    private MediaType instance;

    @BeforeClass
    public static void beforeClass() throws IOException {
        for (Format format : Format.values()) {
            if (format.equals(Format.UNKNOWN)) {
                continue;
            }
            String fixtureName = format.name().toLowerCase();
            files.put(format, TestUtil.getImage(fixtureName));
        }
    }

    @Before
    public void setUp() {
        instance = new MediaType("image/jpeg");
    }

    @Test
    public void testSerialization() throws IOException {
        MediaType type = new MediaType("image/jpeg");
        try (StringWriter writer = new StringWriter()) {
            new ObjectMapper().writeValue(writer, type);
            assertEquals("\"image/jpeg\"", writer.toString());
        }
    }

    @Test
    public void testDeserialization() throws IOException {
        MediaType type = new ObjectMapper().readValue("\"image/jpeg\"",
                MediaType.class);
        assertEquals("image/jpeg", type.toString());
    }

    /* detectMediaTypes(File) */

    @Test
    public void testDetectMediaTypes() throws Exception {
        for (Format format : files.keySet()) {
            Path file = files.get(format);
            MediaType preferredMediaType = format.getPreferredMediaType();

            boolean result = MediaType.detectMediaTypes(file).
                    contains(preferredMediaType);
            if (!result) {
                System.err.println("detection failed:" +
                        "\tformat: " + format +
                        "\tfile: " + file.getFileName());
            }

            // TODO: detectMediaTypes() doesn't understand these
            if (!new HashSet<>(Arrays.asList("avi", "bmp", "sgi", "webm")).
                    contains(file.getFileName().toString())) {
                assertTrue(result);
            }
        }
    }

    /* MediaType(String) */

    @Test
    public void testConstructorWithValidString() {
        instance = new MediaType("image/jpeg");
        assertEquals("image/jpeg", instance.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithInvalidString() {
        new MediaType("cats");
    }

    /* equals() */

    @Test
    public void testEquals() {
        assertTrue(instance.equals("image/jpeg"));
        assertFalse(instance.equals("image/gif"));
    }

    /* toFormat() */

    @Test
    public void testToFormat() {
        assertEquals(Format.AVI, new MediaType("video/avi").toFormat());
        assertEquals(Format.BMP, new MediaType("image/bmp").toFormat());
        assertEquals(Format.DCM, new MediaType("application/dicom").toFormat());
        assertEquals(Format.GIF, new MediaType("image/gif").toFormat());
        assertEquals(Format.JP2, new MediaType("image/jp2").toFormat());
        assertEquals(Format.JPG, new MediaType("image/jpeg").toFormat());
        assertEquals(Format.MOV, new MediaType("video/quicktime").toFormat());
        assertEquals(Format.MP4, new MediaType("video/mp4").toFormat());
        assertEquals(Format.MPG, new MediaType("video/mpeg").toFormat());
        assertEquals(Format.PDF, new MediaType("application/pdf").toFormat());
        assertEquals(Format.PNG, new MediaType("image/png").toFormat());
        assertEquals(Format.TIF, new MediaType("image/tiff").toFormat());
        assertEquals(Format.WEBM, new MediaType("video/webm").toFormat());
        assertEquals(Format.WEBP, new MediaType("image/webp").toFormat());
    }

    /* toString() */

    @Test
    public void testToString() {
        assertEquals("image/jpeg", instance.toString());
    }

}
