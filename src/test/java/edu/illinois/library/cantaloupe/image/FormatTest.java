package edu.illinois.library.cantaloupe.image;

import org.junit.Test;
import org.restlet.data.MediaType;

import java.util.Map;

import static org.junit.Assert.*;

public class FormatTest {

    @Test
    public void testValues() {
        assertNotNull(Format.valueOf("AVI"));
        assertNotNull(Format.valueOf("BMP"));
        assertNotNull(Format.valueOf("GIF"));
        assertNotNull(Format.valueOf("JP2"));
        assertNotNull(Format.valueOf("JPG"));
        assertNotNull(Format.valueOf("MOV"));
        assertNotNull(Format.valueOf("MP4"));
        assertNotNull(Format.valueOf("MPG"));
        assertNotNull(Format.valueOf("PDF"));
        assertNotNull(Format.valueOf("PNG"));
        assertNotNull(Format.valueOf("TIF"));
        assertNotNull(Format.valueOf("WEBM"));
        assertNotNull(Format.valueOf("WEBP"));
        assertNotNull(Format.valueOf("UNKNOWN"));
    }

    @Test
    public void testGetFormatWithIdentifier() {
        // Valid extensions
        assertEquals(Format.AVI,
                Format.getFormat(new Identifier("bla.avi")));
        assertEquals(Format.AVI,
                Format.getFormat(new Identifier("bla.AVI")));
        assertEquals(Format.BMP,
                Format.getFormat(new Identifier("bla.bmp")));
        assertEquals(Format.GIF,
                Format.getFormat(new Identifier("bla.gif")));
        assertEquals(Format.JP2,
                Format.getFormat(new Identifier("bla.jp2")));
        assertEquals(Format.JPG,
                Format.getFormat(new Identifier("bla.jpg")));
        assertEquals(Format.MOV,
                Format.getFormat(new Identifier("bla.mov")));
        assertEquals(Format.MP4,
                Format.getFormat(new Identifier("bla.mp4")));
        assertEquals(Format.MPG,
                Format.getFormat(new Identifier("bla.mpg")));
        assertEquals(Format.PDF,
                Format.getFormat(new Identifier("bla.pdf")));
        assertEquals(Format.PNG,
                Format.getFormat(new Identifier("bla.png")));
        assertEquals(Format.TIF,
                Format.getFormat(new Identifier("bla.tif")));
        assertEquals(Format.WEBM,
                Format.getFormat(new Identifier("bla.webm")));
        assertEquals(Format.WEBP,
                Format.getFormat(new Identifier("bla.webp")));

        // Invalid extension
        assertEquals(Format.UNKNOWN,
                Format.getFormat(new Identifier("bla.bogus")));
    }

    @Test
    public void testGetFormatWithMediaTypeString() {
        assertEquals(Format.AVI, Format.getFormat("video/avi"));
        assertEquals(Format.BMP, Format.getFormat("image/bmp"));
        assertEquals(Format.GIF, Format.getFormat("image/gif"));
        assertEquals(Format.JP2, Format.getFormat("image/jp2"));
        assertEquals(Format.JPG, Format.getFormat("image/jpeg"));
        assertEquals(Format.MOV, Format.getFormat("video/quicktime"));
        assertEquals(Format.MP4, Format.getFormat("video/mp4"));
        assertEquals(Format.MPG, Format.getFormat("video/mpeg"));
        assertEquals(Format.PDF, Format.getFormat("application/pdf"));
        assertEquals(Format.PNG, Format.getFormat("image/png"));
        assertEquals(Format.TIF, Format.getFormat("image/tiff"));
        assertEquals(Format.WEBM, Format.getFormat("video/webm"));
        assertEquals(Format.WEBP, Format.getFormat("image/webp"));
    }

    @Test
    public void testGetExtensions() {
        assertTrue(Format.AVI.getExtensions().contains("avi"));
        assertTrue(Format.BMP.getExtensions().contains("bmp"));
        assertTrue(Format.BMP.getExtensions().contains("dib"));
        assertTrue(Format.GIF.getExtensions().contains("gif"));
        assertTrue(Format.JP2.getExtensions().contains("jp2"));
        assertTrue(Format.JP2.getExtensions().contains("j2k"));
        assertTrue(Format.JPG.getExtensions().contains("jpg"));
        assertTrue(Format.JPG.getExtensions().contains("jpeg"));
        assertTrue(Format.MOV.getExtensions().contains("mov"));
        assertTrue(Format.MP4.getExtensions().contains("mp4"));
        assertTrue(Format.MP4.getExtensions().contains("m4v"));
        assertTrue(Format.MPG.getExtensions().contains("mpg"));
        assertTrue(Format.PDF.getExtensions().contains("pdf"));
        assertTrue(Format.PNG.getExtensions().contains("png"));
        assertTrue(Format.TIF.getExtensions().contains("ptif"));
        assertTrue(Format.TIF.getExtensions().contains("tif"));
        assertTrue(Format.TIF.getExtensions().contains("tiff"));
        assertTrue(Format.WEBM.getExtensions().contains("webm"));
        assertTrue(Format.WEBP.getExtensions().contains("webp"));
        assertTrue(Format.UNKNOWN.getExtensions().contains("unknown"));
    }

    @Test
    public void testGetMediaTypes() {
        assertTrue(Format.AVI.getMediaTypes().
                contains(new MediaType("video/avi")));
        assertTrue(Format.AVI.getMediaTypes().
                contains(new MediaType("video/msvideo")));
        assertTrue(Format.AVI.getMediaTypes().
                contains(new MediaType("video/x-msvideo")));
        assertTrue(Format.BMP.getMediaTypes().
                contains(new MediaType("image/bmp")));
        assertTrue(Format.BMP.getMediaTypes().
                contains(new MediaType("image/x-bmp")));
        assertTrue(Format.BMP.getMediaTypes().
                contains(new MediaType("image/x-ms-bmp")));
        assertTrue(Format.GIF.getMediaTypes().
                contains(new MediaType("image/gif")));
        assertTrue(Format.JP2.getMediaTypes().
                contains(new MediaType("image/jp2")));
        assertTrue(Format.JPG.getMediaTypes().
                contains(new MediaType("image/jpeg")));
        assertTrue(Format.MOV.getMediaTypes().
                contains(new MediaType("video/quicktime")));
        assertTrue(Format.MOV.getMediaTypes().
                contains(new MediaType("video/x-quicktime")));
        assertTrue(Format.MP4.getMediaTypes().
                contains(new MediaType("video/mp4")));
        assertTrue(Format.MPG.getMediaTypes().
                contains(new MediaType("video/mpeg")));
        assertTrue(Format.PDF.getMediaTypes().
                contains(new MediaType("application/pdf")));
        assertTrue(Format.PNG.getMediaTypes().
                contains(new MediaType("image/png")));
        assertTrue(Format.TIF.getMediaTypes().
                contains(new MediaType("image/tiff")));
        assertTrue(Format.WEBM.getMediaTypes().
                contains(new MediaType("video/webm")));
        assertTrue(Format.WEBP.getMediaTypes().
                contains(new MediaType("image/webp")));
        assertTrue(Format.UNKNOWN.getMediaTypes().
                contains(new MediaType("unknown/unknown")));
    }

    @Test
    public void testGetName() {
        assertEquals("AVI", Format.AVI.getName());
        assertEquals("BMP", Format.BMP.getName());
        assertEquals("GIF", Format.GIF.getName());
        assertEquals("JPEG2000", Format.JP2.getName());
        assertEquals("JPEG", Format.JPG.getName());
        assertEquals("QuickTime", Format.MOV.getName());
        assertEquals("MPEG-4", Format.MP4.getName());
        assertEquals("MPEG", Format.MPG.getName());
        assertEquals("PDF", Format.PDF.getName());
        assertEquals("PNG", Format.PNG.getName());
        assertEquals("TIFF", Format.TIF.getName());
        assertEquals("WebM", Format.WEBM.getName());
        assertEquals("WebP", Format.WEBP.getName());
        assertEquals("Unknown", Format.UNKNOWN.getName());
    }

    @Test
    public void testGetPreferredExtension() {
        assertEquals("avi", Format.AVI.getPreferredExtension());
        assertEquals("bmp", Format.BMP.getPreferredExtension());
        assertEquals("gif", Format.GIF.getPreferredExtension());
        assertEquals("jp2", Format.JP2.getPreferredExtension());
        assertEquals("jpg", Format.JPG.getPreferredExtension());
        assertEquals("mov", Format.MOV.getPreferredExtension());
        assertEquals("mp4", Format.MP4.getPreferredExtension());
        assertEquals("mpg", Format.MPG.getPreferredExtension());
        assertEquals("pdf", Format.PDF.getPreferredExtension());
        assertEquals("png", Format.PNG.getPreferredExtension());
        assertEquals("tif", Format.TIF.getPreferredExtension());
        assertEquals("webm", Format.WEBM.getPreferredExtension());
        assertEquals("webp", Format.WEBP.getPreferredExtension());
        assertEquals("unknown", Format.UNKNOWN.getPreferredExtension());
    }

    @Test
    public void testGetPreferredMediaType() {
        assertEquals("video/avi",
                Format.AVI.getPreferredMediaType().toString());
        assertEquals("image/bmp",
                Format.BMP.getPreferredMediaType().toString());
        assertEquals("image/gif",
                Format.GIF.getPreferredMediaType().toString());
        assertEquals("image/jp2",
                Format.JP2.getPreferredMediaType().toString());
        assertEquals("image/jpeg",
                Format.JPG.getPreferredMediaType().toString());
        assertEquals("video/quicktime",
                Format.MOV.getPreferredMediaType().toString());
        assertEquals("video/mp4",
                Format.MP4.getPreferredMediaType().toString());
        assertEquals("video/mpeg",
                Format.MPG.getPreferredMediaType().toString());
        assertEquals("application/pdf",
                Format.PDF.getPreferredMediaType().toString());
        assertEquals("image/png",
                Format.PNG.getPreferredMediaType().toString());
        assertEquals("image/tiff",
                Format.TIF.getPreferredMediaType().toString());
        assertEquals("video/webm",
                Format.WEBM.getPreferredMediaType().toString());
        assertEquals("image/webp",
                Format.WEBP.getPreferredMediaType().toString());
        assertEquals("unknown/unknown",
                Format.UNKNOWN.getPreferredMediaType().toString());
    }

    @Test
    public void testGetType() {
        assertEquals(Format.Type.VIDEO, Format.AVI.getType());
        assertEquals(Format.Type.IMAGE, Format.BMP.getType());
        assertEquals(Format.Type.IMAGE, Format.GIF.getType());
        assertEquals(Format.Type.IMAGE, Format.JP2.getType());
        assertEquals(Format.Type.IMAGE, Format.JPG.getType());
        assertEquals(Format.Type.VIDEO, Format.MOV.getType());
        assertEquals(Format.Type.VIDEO, Format.MP4.getType());
        assertEquals(Format.Type.VIDEO, Format.MPG.getType());
        assertEquals(Format.Type.IMAGE, Format.PDF.getType());
        assertEquals(Format.Type.IMAGE, Format.PNG.getType());
        assertEquals(Format.Type.IMAGE, Format.TIF.getType());
        assertEquals(Format.Type.VIDEO, Format.WEBM.getType());
        assertEquals(Format.Type.IMAGE, Format.WEBP.getType());
        assertNull(Format.UNKNOWN.getType());
    }

    @Test
    public void testIsImage() {
        assertFalse(Format.AVI.isImage());
        assertTrue(Format.BMP.isImage());
        assertTrue(Format.GIF.isImage());
        assertTrue(Format.JP2.isImage());
        assertTrue(Format.JPG.isImage());
        assertFalse(Format.MOV.isImage());
        assertFalse(Format.MP4.isImage());
        assertFalse(Format.MPG.isImage());
        assertTrue(Format.PDF.isImage());
        assertTrue(Format.PNG.isImage());
        assertTrue(Format.TIF.isImage());
        assertFalse(Format.WEBM.isImage());
        assertTrue(Format.WEBP.isImage());
        assertFalse(Format.UNKNOWN.isImage());
    }

    @Test
    public void testIsVideo() {
        assertTrue(Format.AVI.isVideo());
        assertFalse(Format.BMP.isVideo());
        assertFalse(Format.GIF.isVideo());
        assertFalse(Format.JP2.isVideo());
        assertFalse(Format.JPG.isVideo());
        assertTrue(Format.MOV.isVideo());
        assertTrue(Format.MP4.isVideo());
        assertTrue(Format.MPG.isVideo());
        assertFalse(Format.PDF.isVideo());
        assertFalse(Format.PNG.isVideo());
        assertFalse(Format.TIF.isVideo());
        assertTrue(Format.WEBM.isVideo());
        assertFalse(Format.WEBP.isVideo());
        assertFalse(Format.UNKNOWN.isVideo());
    }

    @Test
    public void testSupportsTransparency() {
        assertFalse(Format.AVI.supportsTransparency());
        assertTrue(Format.BMP.supportsTransparency());
        assertTrue(Format.GIF.supportsTransparency());
        assertTrue(Format.JP2.supportsTransparency());
        assertFalse(Format.JPG.supportsTransparency());
        assertFalse(Format.MP4.supportsTransparency());
        assertFalse(Format.MPG.supportsTransparency());
        assertFalse(Format.PDF.supportsTransparency());
        assertTrue(Format.PNG.supportsTransparency());
        assertTrue(Format.TIF.supportsTransparency());
        assertFalse(Format.WEBM.supportsTransparency());
        assertTrue(Format.WEBP.supportsTransparency());
    }

    @Test
    public void testToMap() {
        Map<String,Object> map = Format.JPG.toMap();
        assertEquals("jpg", map.get("extension"));
        assertEquals("image/jpeg", map.get("media_type"));
    }

    @Test
    public void testToString() {
        for (Format format : Format.values()) {
            assertEquals(format.getPreferredExtension(),
                    format.toString());
        }
    }

}
