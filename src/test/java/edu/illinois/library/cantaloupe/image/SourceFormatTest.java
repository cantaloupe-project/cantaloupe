package edu.illinois.library.cantaloupe.image;

import junit.framework.TestCase;

public class SourceFormatTest extends TestCase {

    public void testValues() {
        assertNotNull(SourceFormat.valueOf("BMP"));
        assertNotNull(SourceFormat.valueOf("GIF"));
        assertNotNull(SourceFormat.valueOf("JP2"));
        assertNotNull(SourceFormat.valueOf("JPG"));
        assertNotNull(SourceFormat.valueOf("PNG"));
        assertNotNull(SourceFormat.valueOf("TIF"));
        assertNotNull(SourceFormat.valueOf("WEBP"));
        assertNotNull(SourceFormat.valueOf("UNKNOWN"));
    }

    public void testGetSourceFormat() {
        assertEquals(SourceFormat.JP2, SourceFormat.getSourceFormat("bla.jp2"));
        assertEquals(SourceFormat.JPG, SourceFormat.getSourceFormat("bla.jpeg"));
        assertEquals(SourceFormat.TIF, SourceFormat.getSourceFormat("bla.tiff"));
        assertEquals(SourceFormat.UNKNOWN, SourceFormat.getSourceFormat("bla.bogus"));
    }

    public void testExtensions() {
        assertTrue(SourceFormat.BMP.getExtensions().contains("bmp"));
        assertTrue(SourceFormat.GIF.getExtensions().contains("gif"));
        assertTrue(SourceFormat.JP2.getExtensions().contains("jp2"));
        assertTrue(SourceFormat.JPG.getExtensions().contains("jpg"));
        assertTrue(SourceFormat.JPG.getExtensions().contains("jpeg"));
        assertTrue(SourceFormat.PNG.getExtensions().contains("png"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("tif"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("tiff"));
        assertTrue(SourceFormat.WEBP.getExtensions().contains("webp"));
        assertTrue(SourceFormat.UNKNOWN.getExtensions().contains("unknown"));
    }

    public void testMediaTypes() {
        assertEquals("image/bmp", SourceFormat.BMP.getMediaType());
        assertEquals("image/gif", SourceFormat.GIF.getMediaType());
        assertEquals("image/jp2", SourceFormat.JP2.getMediaType());
        assertEquals("image/jpeg", SourceFormat.JPG.getMediaType());
        assertEquals("image/png", SourceFormat.PNG.getMediaType());
        assertEquals("image/tiff", SourceFormat.TIF.getMediaType());
        assertEquals("image/webp", SourceFormat.WEBP.getMediaType());
        assertEquals("unknown/unknown", SourceFormat.UNKNOWN.getMediaType());
    }

    public void testPreferredExtension() {
        assertEquals("bmp", SourceFormat.BMP.getPreferredExtension());
        assertEquals("gif", SourceFormat.GIF.getPreferredExtension());
        assertEquals("jp2", SourceFormat.JP2.getPreferredExtension());
        assertEquals("jpg", SourceFormat.JPG.getPreferredExtension());
        assertEquals("png", SourceFormat.PNG.getPreferredExtension());
        assertEquals("tif", SourceFormat.TIF.getPreferredExtension());
        assertEquals("webp", SourceFormat.WEBP.getPreferredExtension());
        assertEquals("unknown", SourceFormat.UNKNOWN.getPreferredExtension());
    }

    public void testToString() {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            assertEquals(sourceFormat.getPreferredExtension(),
                    sourceFormat.toString());
        }
    }

}
