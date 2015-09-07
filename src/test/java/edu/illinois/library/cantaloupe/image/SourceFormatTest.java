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

    public void testExtensions() {
        assertEquals("bmp", SourceFormat.BMP.getExtension());
        assertEquals("gif", SourceFormat.GIF.getExtension());
        assertEquals("jp2", SourceFormat.JP2.getExtension());
        assertEquals("jpg", SourceFormat.JPG.getExtension());
        assertEquals("png", SourceFormat.PNG.getExtension());
        assertEquals("tif", SourceFormat.TIF.getExtension());
        assertEquals("webp", SourceFormat.WEBP.getExtension());
        assertEquals("unknown", SourceFormat.UNKNOWN.getExtension());
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

    public void testToString() {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            assertEquals(sourceFormat.getExtension(), sourceFormat.toString());
        }
    }

}
