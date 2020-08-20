package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FormatTest extends BaseTest {

    @Test
    void testAll() {
        Set<String> expected = Set.of("avi", "bmp", "flv", "gif", "jp2", "jpg",
                "mov", "mp4", "mpg", "pdf", "png", "tif", "webm", "webp",
                "xpm");
        Set<String> actual = Format.all()
                .stream()
                .map(Format::getKey)
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    void testGetWithValidKey() {
        assertEquals(FormatRegistry.formatWithKey("jpg"), Format.get("jpg"));
    }

    @Test
    void testGetWithInvalidKey() {
        assertNull(Format.get("bogus"));
    }

    @Test
    void testInferFormatWithIdentifier() {
        // AVI
        assertEquals(Format.get("avi"),
                Format.inferFormat(new Identifier("bla.avi")));
        assertEquals(Format.get("avi"),
                Format.inferFormat(new Identifier("bla.AVI")));
        // BMP
        assertEquals(Format.get("bmp"),
                Format.inferFormat(new Identifier("bla.bmp")));
        // FLV
        assertEquals(Format.get("flv"),
                Format.inferFormat(new Identifier("bla.flv")));
        // GIF
        assertEquals(Format.get("gif"),
                Format.inferFormat(new Identifier("bla.gif")));
        // JP2
        assertEquals(Format.get("jp2"),
                Format.inferFormat(new Identifier("bla.jp2")));
        assertEquals(Format.get("jp2"),
                Format.inferFormat(new Identifier("bla.jpx")));
        // JPG
        assertEquals(Format.get("jpg"),
                Format.inferFormat(new Identifier("bla.jpg")));
        // MOV
        assertEquals(Format.get("mov"),
                Format.inferFormat(new Identifier("bla.mov")));
        // MP4
        assertEquals(Format.get("mp4"),
                Format.inferFormat(new Identifier("bla.mp4")));
        // MPG
        assertEquals(Format.get("mpg"),
                Format.inferFormat(new Identifier("bla.mpg")));
        // PDF
        assertEquals(Format.get("pdf"),
                Format.inferFormat(new Identifier("bla.pdf")));
        // PNG
        assertEquals(Format.get("png"),
                Format.inferFormat(new Identifier("bla.png")));
        // TIF
        assertEquals(Format.get("tif"),
                Format.inferFormat(new Identifier("bla.tif")));
        // WEBM
        assertEquals(Format.get("webm"),
                Format.inferFormat(new Identifier("bla.webm")));
        // WEBP
        assertEquals(Format.get("webp"),
                Format.inferFormat(new Identifier("bla.webp")));
        // XPM
        assertEquals(Format.get("xpm"),
                Format.inferFormat(new Identifier("bla.xpm")));
    }

    @Test
    void testInferFormatWithString() {
        // AVI
        assertEquals(Format.get("avi"), Format.inferFormat("bla.avi"));
        assertEquals(Format.get("avi"), Format.inferFormat("bla.AVI"));
        // BMP
        assertEquals(Format.get("bmp"), Format.inferFormat("bla.bmp"));
        // FLV
        assertEquals(Format.get("flv"), Format.inferFormat("bla.flv"));
        // GIF
        assertEquals(Format.get("gif"), Format.inferFormat("bla.gif"));
        // JP2
        assertEquals(Format.get("jp2"), Format.inferFormat("bla.jp2"));
        assertEquals(Format.get("jp2"), Format.inferFormat("bla.jpx"));
        // JPG
        assertEquals(Format.get("jpg"), Format.inferFormat("bla.jpg"));
        // MOV
        assertEquals(Format.get("mov"), Format.inferFormat("bla.mov"));
        // MP4
        assertEquals(Format.get("mp4"), Format.inferFormat("bla.mp4"));
        // MPG
        assertEquals(Format.get("mpg"), Format.inferFormat("bla.mpg"));
        // PDF
        assertEquals(Format.get("pdf"), Format.inferFormat("bla.pdf"));
        // PNG
        assertEquals(Format.get("png"), Format.inferFormat("bla.png"));
        // TIF
        assertEquals(Format.get("tif"), Format.inferFormat("bla.tif"));
        // UNKNOWN
        assertEquals(Format.UNKNOWN, Format.inferFormat("bla.bogus"));
        // WEBM
        assertEquals(Format.get("webm"), Format.inferFormat("bla.webm"));
        // WEBP
        assertEquals(Format.get("webp"), Format.inferFormat("bla.webp"));
        // XPM
        assertEquals(Format.get("xpm"), Format.inferFormat("bla.xpm"));
    }

    @Test
    void testWithExtensionAndAMatch() {
        assertEquals(Format.get("jpg"), Format.withExtension("jpg"));
        assertEquals(Format.get("jpg"), Format.withExtension(".jpg"));
        assertEquals(Format.get("jpg"), Format.withExtension("JPG"));
        assertEquals(Format.get("jpg"), Format.withExtension(".JPG"));
    }

    @Test
    void testWithExtensionAndNoMatch() {
        assertNull(Format.withExtension("bogus"));
    }

    @Test
    void testCompareTo() {
        assertTrue(Format.get("avi").compareTo(Format.get("tif")) < 0);
        assertEquals(0, Format.get("avi").compareTo(Format.get("avi")));
        assertTrue(Format.get("tif").compareTo(Format.get("avi")) > 0);
    }

    @Test
    void testEqualsWithEqualInstances() {
        assertEquals(Format.get("jpg"), Format.get("jpg"));
    }

    @Test
    void testEqualsWithUnequalInstances() {
        assertNotEquals(Format.get("jpg"), Format.get("tif"));
    }

    @Test
    void testGetPreferredExtension() {
        assertEquals("avi", Format.get("avi").getPreferredExtension());
        assertEquals("bmp", Format.get("bmp").getPreferredExtension());
        assertEquals("flv", Format.get("flv").getPreferredExtension());
        assertEquals("gif", Format.get("gif").getPreferredExtension());
        assertEquals("jp2", Format.get("jp2").getPreferredExtension());
        assertEquals("jpg", Format.get("jpg").getPreferredExtension());
        assertEquals("mov", Format.get("mov").getPreferredExtension());
        assertEquals("mp4", Format.get("mp4").getPreferredExtension());
        assertEquals("mpg", Format.get("mpg").getPreferredExtension());
        assertEquals("pdf", Format.get("pdf").getPreferredExtension());
        assertEquals("png", Format.get("png").getPreferredExtension());
        assertEquals("tif", Format.get("tif").getPreferredExtension());
        assertEquals("unknown", Format.UNKNOWN.getPreferredExtension());
        assertEquals("webm", Format.get("webm").getPreferredExtension());
        assertEquals("webp", Format.get("webp").getPreferredExtension());
        assertEquals("xpm", Format.get("xpm").getPreferredExtension());
    }

    @Test
    void testGetPreferredMediaType() {
        assertEquals("video/avi",
                Format.get("avi").getPreferredMediaType().toString());
        assertEquals("image/bmp",
                Format.get("bmp").getPreferredMediaType().toString());
        assertEquals("video/x-flv",
                Format.get("flv").getPreferredMediaType().toString());
        assertEquals("image/gif",
                Format.get("gif").getPreferredMediaType().toString());
        assertEquals("image/jp2",
                Format.get("jp2").getPreferredMediaType().toString());
        assertEquals("image/jpeg",
                Format.get("jpg").getPreferredMediaType().toString());
        assertEquals("video/quicktime",
                Format.get("mov").getPreferredMediaType().toString());
        assertEquals("video/mp4",
                Format.get("mp4").getPreferredMediaType().toString());
        assertEquals("video/mpeg",
                Format.get("mpg").getPreferredMediaType().toString());
        assertEquals("application/pdf",
                Format.get("pdf").getPreferredMediaType().toString());
        assertEquals("image/png",
                Format.get("png").getPreferredMediaType().toString());
        assertEquals("image/tiff",
                Format.get("tif").getPreferredMediaType().toString());
        assertEquals("unknown/unknown",
                Format.UNKNOWN.getPreferredMediaType().toString());
        assertEquals("video/webm",
                Format.get("webm").getPreferredMediaType().toString());
        assertEquals("image/webp",
                Format.get("webp").getPreferredMediaType().toString());
        assertEquals("image/x-xpixmap",
                Format.get("xpm").getPreferredMediaType().toString());
    }

    @Test
    void testHashCodeWithEqualInstances() {
        assertEquals(Format.get("jpg").hashCode(), Format.get("jpg").hashCode());
    }

    @Test
    void testHashCodeWithUnequalInstances() {
        assertNotEquals(Format.get("jpg").hashCode(), Format.get("tif").hashCode());
    }

    @Test
    void testToMap() {
        Map<String, Object> map = Format.get("jpg").toMap();
        assertEquals("jpg", map.get("extension"));
        assertEquals("image/jpeg", map.get("media_type"));

        //noinspection ConstantConditions
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("cats", "cats"));
    }

    @Test
    void testToString() {
        for (Format format : Format.all()) {
            assertEquals(format.getPreferredExtension(),
                    format.toString());
        }
    }

}
