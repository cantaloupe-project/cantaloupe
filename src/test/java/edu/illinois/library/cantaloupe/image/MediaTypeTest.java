package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MediaTypeTest extends BaseTest {

    private static final Map<Format, Path> FILES = new HashMap<>();

    private MediaType instance;

    @BeforeAll
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        for (Format format : Format.getAllFormats()) {
            if (format.equals(Format.UNKNOWN)) {
                continue;
            }
            String fixtureName = format.getKey().toLowerCase();
            FILES.put(format, TestUtil.getImage(fixtureName));
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new MediaType("image/jpeg");
    }

    @Test
    void testSerialization() throws IOException {
        MediaType type = new MediaType("image/jpeg");
        try (StringWriter writer = new StringWriter()) {
            new ObjectMapper().writeValue(writer, type);
            assertEquals("\"image/jpeg\"", writer.toString());
        }
    }

    @Test
    void testDeserialization() throws IOException {
        MediaType type = new ObjectMapper().readValue("\"image/jpeg\"",
                MediaType.class);
        assertEquals("image/jpeg", type.toString());
    }

    /* detectMediaTypes(byte[]) */

    @Test
    void testDetectMediaTypesWithByteArray() throws Exception {
        for (Format format : FILES.keySet()) {
            Path file = FILES.get(format);
            byte[] bytes = Files.readAllBytes(file);
            List<MediaType> formatMediaTypes = format.getMediaTypes();
            boolean result = !Collections.disjoint(
                    MediaType.detectMediaTypes(bytes),
                    formatMediaTypes);
            if (!result) {
                System.err.println("detection failed:" +
                        "\tformat: " + format +
                        "\tfile: " + file.getFileName());
            }

            // detectMediaTypes() doesn't understand these.
            if (!Set.of("avi", "webm").contains(file.getFileName().toString())) {
                assertTrue(result);
            }
        }
    }

    /* detectMediaTypes(Path) */

    @Test
    void testDetectMediaTypesWithPath() throws Exception {
        for (Format format : FILES.keySet()) {
            Path file = FILES.get(format);
            List<MediaType> formatMediaTypes = format.getMediaTypes();
            boolean result = !Collections.disjoint(
                    MediaType.detectMediaTypes(file),
                    formatMediaTypes);
            if (!result) {
                System.err.println("detection failed: " +
                        "[format: " + format + "] " +
                        "[file: " + file.getFileName() + "]");
            }

            // detectMediaTypes() doesn't understand these.
            if (!Set.of("avi", "webm").contains(file.getFileName().toString())) {
                assertTrue(result);
            }
        }
    }

    /* detectMediaTypes(InputStream) */

    @Test
    void testDetectMediaTypesWithInputStream() throws Exception {
        for (Format format : FILES.keySet()) {
            Path file = FILES.get(format);
            List<MediaType> formatMediaTypes = format.getMediaTypes();
            try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
                boolean result = !Collections.disjoint(
                        MediaType.detectMediaTypes(is),
                        formatMediaTypes);
                if (!result) {
                    System.err.println("detection failed:" +
                            "\tformat: " + format +
                            "\tfile: " + file.getFileName());
                }

                // detectMediaTypes() doesn't understand these.
                if (!Set.of("avi", "webm").contains(file.getFileName().toString())) {
                    assertTrue(result);
                }
            }
        }
    }

    @Test
    void testFromContentType() {
        assertEquals(new MediaType("image/jp2"),
                MediaType.fromContentType("image/jp2"));
        assertEquals(new MediaType("image/jp2"),
                MediaType.fromContentType("image/jp2; charset=UTF-8"));
    }

    /* MediaType(String) */

    @Test
    void testConstructorWithValidString() {
        instance = new MediaType("image/jpeg");
        assertEquals("image/jpeg", instance.toString());
    }

    @Test
    void testConstructorWithInvalidString() {
        assertThrows(IllegalArgumentException.class,
                () -> new MediaType("cats"));
    }

    /* equals() */

    @Test
    void testEquals() {
        assertEquals(instance, new MediaType("image/jpeg"));
        assertNotEquals(instance, new MediaType("image/gif"));
        assertNotEquals(null, instance);
    }

    /* toFormat() */

    @Test
    void testToFormat() {
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
        assertEquals(Format.XPM, new MediaType("image/x-xpixmap").toFormat());
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("image/jpeg", instance.toString());
    }

}
