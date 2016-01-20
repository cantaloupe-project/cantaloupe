package edu.illinois.library.cantaloupe.image;

import org.junit.Test;
import org.restlet.data.MediaType;

import static org.junit.Assert.*;

public class SourceFormatTest {

    @Test
    public void testValues() {
        assertNotNull(SourceFormat.valueOf("AVI"));
        assertNotNull(SourceFormat.valueOf("BMP"));
        assertNotNull(SourceFormat.valueOf("GIF"));
        assertNotNull(SourceFormat.valueOf("JP2"));
        assertNotNull(SourceFormat.valueOf("JPG"));
        assertNotNull(SourceFormat.valueOf("MOV"));
        assertNotNull(SourceFormat.valueOf("MP4"));
        assertNotNull(SourceFormat.valueOf("MPG"));
        assertNotNull(SourceFormat.valueOf("PDF"));
        assertNotNull(SourceFormat.valueOf("PNG"));
        assertNotNull(SourceFormat.valueOf("TIF"));
        assertNotNull(SourceFormat.valueOf("WEBM"));
        assertNotNull(SourceFormat.valueOf("WEBP"));
        assertNotNull(SourceFormat.valueOf("UNKNOWN"));
    }

    @Test
    public void testGetSourceFormatWithMediaType() {
        assertEquals(SourceFormat.JP2,
                SourceFormat.getSourceFormat(new MediaType("image/jp2")));
        assertEquals(SourceFormat.JPG,
                SourceFormat.getSourceFormat(new MediaType("image/jpeg")));
        assertEquals(SourceFormat.PDF,
                SourceFormat.getSourceFormat(new MediaType("application/pdf")));
        assertEquals(SourceFormat.TIF,
                SourceFormat.getSourceFormat(new MediaType("image/tiff")));
        assertEquals(SourceFormat.UNKNOWN,
                SourceFormat.getSourceFormat(new MediaType("image/bogus")));
    }

    @Test
    public void testGetSourceFormatWithString() {
        assertEquals(SourceFormat.JP2,
                SourceFormat.getSourceFormat(new Identifier("bla.jp2")));
        assertEquals(SourceFormat.JPG,
                SourceFormat.getSourceFormat(new Identifier("bla.jpeg")));
        assertEquals(SourceFormat.PDF,
                SourceFormat.getSourceFormat(new Identifier("bla.pdf")));
        assertEquals(SourceFormat.TIF,
                SourceFormat.getSourceFormat(new Identifier("bla.tiff")));
        assertEquals(SourceFormat.UNKNOWN,
                SourceFormat.getSourceFormat(new Identifier("bla.bogus")));
    }

    @Test
    public void testGetExtensions() {
        assertTrue(SourceFormat.AVI.getExtensions().contains("avi"));
        assertTrue(SourceFormat.BMP.getExtensions().contains("bmp"));
        assertTrue(SourceFormat.GIF.getExtensions().contains("gif"));
        assertTrue(SourceFormat.JP2.getExtensions().contains("jp2"));
        assertTrue(SourceFormat.JP2.getExtensions().contains("j2k"));
        assertTrue(SourceFormat.JPG.getExtensions().contains("jpg"));
        assertTrue(SourceFormat.JPG.getExtensions().contains("jpeg"));
        assertTrue(SourceFormat.MOV.getExtensions().contains("mov"));
        assertTrue(SourceFormat.MP4.getExtensions().contains("mp4"));
        assertTrue(SourceFormat.MPG.getExtensions().contains("mpg"));
        assertTrue(SourceFormat.PDF.getExtensions().contains("pdf"));
        assertTrue(SourceFormat.PNG.getExtensions().contains("png"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("ptif"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("tif"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("tiff"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("tiff"));
        assertTrue(SourceFormat.WEBM.getExtensions().contains("webm"));
        assertTrue(SourceFormat.WEBP.getExtensions().contains("webp"));
        assertTrue(SourceFormat.UNKNOWN.getExtensions().contains("unknown"));
    }

    @Test
    public void testGetMediaTypes() {
        assertTrue(SourceFormat.AVI.getMediaTypes().
                contains(new MediaType("video/avi")));
        assertTrue(SourceFormat.AVI.getMediaTypes().
                contains(new MediaType("video/msvideo")));
        assertTrue(SourceFormat.AVI.getMediaTypes().
                contains(new MediaType("video/x-msvideo")));
        assertTrue(SourceFormat.BMP.getMediaTypes().
                contains(new MediaType("image/bmp")));
        assertTrue(SourceFormat.BMP.getMediaTypes().
                contains(new MediaType("image/x-ms-bmp")));
        assertTrue(SourceFormat.GIF.getMediaTypes().
                contains(new MediaType("image/gif")));
        assertTrue(SourceFormat.JP2.getMediaTypes().
                contains(new MediaType("image/jp2")));
        assertTrue(SourceFormat.JPG.getMediaTypes().
                contains(new MediaType("image/jpeg")));
        assertTrue(SourceFormat.MOV.getMediaTypes().
                contains(new MediaType("video/quicktime")));
        assertTrue(SourceFormat.MOV.getMediaTypes().
                contains(new MediaType("video/x-quicktime")));
        assertTrue(SourceFormat.MP4.getMediaTypes().
                contains(new MediaType("video/mp4")));
        assertTrue(SourceFormat.MPG.getMediaTypes().
                contains(new MediaType("video/mpeg")));
        assertTrue(SourceFormat.PDF.getMediaTypes().
                contains(new MediaType("application/pdf")));
        assertTrue(SourceFormat.PNG.getMediaTypes().
                contains(new MediaType("image/png")));
        assertTrue(SourceFormat.TIF.getMediaTypes().
                contains(new MediaType("image/tiff")));
        assertTrue(SourceFormat.WEBM.getMediaTypes().
                contains(new MediaType("video/webm")));
        assertTrue(SourceFormat.WEBP.getMediaTypes().
                contains(new MediaType("image/webp")));
        assertTrue(SourceFormat.UNKNOWN.getMediaTypes().
                contains(new MediaType("unknown/unknown")));
    }

    @Test
    public void testGetName() {
        assertEquals("AVI", SourceFormat.AVI.getName());
        assertEquals("BMP", SourceFormat.BMP.getName());
        assertEquals("GIF", SourceFormat.GIF.getName());
        assertEquals("JPEG2000", SourceFormat.JP2.getName());
        assertEquals("JPEG", SourceFormat.JPG.getName());
        assertEquals("QuickTime", SourceFormat.MOV.getName());
        assertEquals("MPEG-4", SourceFormat.MP4.getName());
        assertEquals("MPEG", SourceFormat.MPG.getName());
        assertEquals("PDF", SourceFormat.PDF.getName());
        assertEquals("PNG", SourceFormat.PNG.getName());
        assertEquals("TIFF", SourceFormat.TIF.getName());
        assertEquals("WebM", SourceFormat.WEBM.getName());
        assertEquals("WebP", SourceFormat.WEBP.getName());
        assertEquals("Unknown", SourceFormat.UNKNOWN.getName());
    }

    @Test
    public void testGetPreferredMediaType() {
        assertEquals("video/avi",
                SourceFormat.AVI.getPreferredMediaType().toString());
        assertEquals("application/pdf",
                SourceFormat.PDF.getPreferredMediaType().toString());
        assertEquals("image/bmp",
                SourceFormat.BMP.getPreferredMediaType().toString());
        assertEquals("image/gif",
                SourceFormat.GIF.getPreferredMediaType().toString());
        assertEquals("image/jp2",
                SourceFormat.JP2.getPreferredMediaType().toString());
        assertEquals("image/jpeg",
                SourceFormat.JPG.getPreferredMediaType().toString());
        assertEquals("video/quicktime",
                SourceFormat.MOV.getPreferredMediaType().toString());
        assertEquals("video/mp4",
                SourceFormat.MP4.getPreferredMediaType().toString());
        assertEquals("video/mpeg",
                SourceFormat.MPG.getPreferredMediaType().toString());
        assertEquals("image/png",
                SourceFormat.PNG.getPreferredMediaType().toString());
        assertEquals("image/tiff",
                SourceFormat.TIF.getPreferredMediaType().toString());
        assertEquals("video/webm",
                SourceFormat.WEBM.getPreferredMediaType().toString());
        assertEquals("image/webp",
                SourceFormat.WEBP.getPreferredMediaType().toString());
        assertEquals("unknown/unknown",
                SourceFormat.UNKNOWN.getPreferredMediaType().toString());
    }

    @Test
    public void testGetPreferredExtension() {
        assertEquals("avi", SourceFormat.AVI.getPreferredExtension());
        assertEquals("bmp", SourceFormat.BMP.getPreferredExtension());
        assertEquals("gif", SourceFormat.GIF.getPreferredExtension());
        assertEquals("jp2", SourceFormat.JP2.getPreferredExtension());
        assertEquals("jpg", SourceFormat.JPG.getPreferredExtension());
        assertEquals("mov", SourceFormat.MOV.getPreferredExtension());
        assertEquals("mp4", SourceFormat.MP4.getPreferredExtension());
        assertEquals("mpg", SourceFormat.MPG.getPreferredExtension());
        assertEquals("pdf", SourceFormat.PDF.getPreferredExtension());
        assertEquals("png", SourceFormat.PNG.getPreferredExtension());
        assertEquals("tif", SourceFormat.TIF.getPreferredExtension());
        assertEquals("webm", SourceFormat.WEBM.getPreferredExtension());
        assertEquals("webp", SourceFormat.WEBP.getPreferredExtension());
        assertEquals("unknown", SourceFormat.UNKNOWN.getPreferredExtension());
    }

    @Test
    public void testGetType() {
        assertEquals(SourceFormat.Type.VIDEO, SourceFormat.AVI.getType());
        assertEquals(SourceFormat.Type.IMAGE, SourceFormat.BMP.getType());
        assertEquals(SourceFormat.Type.IMAGE, SourceFormat.GIF.getType());
        assertEquals(SourceFormat.Type.IMAGE, SourceFormat.JP2.getType());
        assertEquals(SourceFormat.Type.IMAGE, SourceFormat.JPG.getType());
        assertEquals(SourceFormat.Type.VIDEO, SourceFormat.MOV.getType());
        assertEquals(SourceFormat.Type.VIDEO, SourceFormat.MP4.getType());
        assertEquals(SourceFormat.Type.VIDEO, SourceFormat.MPG.getType());
        assertEquals(SourceFormat.Type.IMAGE, SourceFormat.PDF.getType());
        assertEquals(SourceFormat.Type.IMAGE, SourceFormat.PNG.getType());
        assertEquals(SourceFormat.Type.IMAGE, SourceFormat.TIF.getType());
        assertEquals(SourceFormat.Type.VIDEO, SourceFormat.WEBM.getType());
        assertEquals(SourceFormat.Type.IMAGE, SourceFormat.WEBP.getType());
        assertNull(SourceFormat.UNKNOWN.getType());
    }

    @Test
    public void testIsImage() {
        assertFalse(SourceFormat.AVI.isImage());
        assertTrue(SourceFormat.BMP.isImage());
        assertTrue(SourceFormat.GIF.isImage());
        assertTrue(SourceFormat.JP2.isImage());
        assertTrue(SourceFormat.JPG.isImage());
        assertFalse(SourceFormat.MOV.isImage());
        assertFalse(SourceFormat.MP4.isImage());
        assertFalse(SourceFormat.MPG.isImage());
        assertTrue(SourceFormat.PDF.isImage());
        assertTrue(SourceFormat.PNG.isImage());
        assertTrue(SourceFormat.TIF.isImage());
        assertFalse(SourceFormat.WEBM.isImage());
        assertTrue(SourceFormat.WEBP.isImage());
        assertFalse(SourceFormat.UNKNOWN.isImage());
    }

    @Test
    public void testIsVideo() {
        assertTrue(SourceFormat.AVI.isVideo());
        assertFalse(SourceFormat.BMP.isVideo());
        assertFalse(SourceFormat.GIF.isVideo());
        assertFalse(SourceFormat.JP2.isVideo());
        assertFalse(SourceFormat.JPG.isVideo());
        assertTrue(SourceFormat.MOV.isVideo());
        assertTrue(SourceFormat.MP4.isVideo());
        assertTrue(SourceFormat.MPG.isVideo());
        assertFalse(SourceFormat.PDF.isVideo());
        assertFalse(SourceFormat.PNG.isVideo());
        assertFalse(SourceFormat.TIF.isVideo());
        assertTrue(SourceFormat.WEBM.isVideo());
        assertFalse(SourceFormat.WEBP.isVideo());
        assertFalse(SourceFormat.UNKNOWN.isVideo());
    }

    @Test
    public void testToString() {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            assertEquals(sourceFormat.getPreferredExtension(),
                    sourceFormat.toString());
        }
    }

}
