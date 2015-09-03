package edu.illinois.library.cantaloupe.image;

import junit.framework.TestCase;

public class FormatTest extends TestCase {

    public void testValues() {
        assertNotNull(Format.valueOf("GIF"));
        assertNotNull(Format.valueOf("JP2"));
        assertNotNull(Format.valueOf("JPG"));
        assertNotNull(Format.valueOf("PDF"));
        assertNotNull(Format.valueOf("PNG"));
        assertNotNull(Format.valueOf("TIF"));
        assertNotNull(Format.valueOf("WEBP"));
    }

    public void testExtensions() {
        assertEquals("gif", Format.GIF.getExtension());
        assertEquals("jp2", Format.JP2.getExtension());
        assertEquals("jpg", Format.JPG.getExtension());
        assertEquals("pdf", Format.PDF.getExtension());
        assertEquals("png", Format.PNG.getExtension());
        assertEquals("tif", Format.TIF.getExtension());
        assertEquals("webp", Format.WEBP.getExtension());
    }

    public void testMediaTypes() {
        assertEquals("image/gif", Format.GIF.getMediaType());
        assertEquals("image/jp2", Format.JP2.getMediaType());
        assertEquals("image/jpeg", Format.JPG.getMediaType());
        assertEquals("application/pdf", Format.PDF.getMediaType());
        assertEquals("image/png", Format.PNG.getMediaType());
        assertEquals("image/tiff", Format.TIF.getMediaType());
        assertEquals("image/webp", Format.WEBP.getMediaType());
    }

}
