package edu.illinois.library.cantaloupe.image;

import junit.framework.TestCase;

public class SourceFormatTest extends TestCase {

    public void testValues() {
        assertNotNull(SourceFormat.valueOf("JP2"));
        assertNotNull(SourceFormat.valueOf("JPG"));
        assertNotNull(SourceFormat.valueOf("PNG"));
        assertNotNull(SourceFormat.valueOf("TIF"));
        assertNotNull(SourceFormat.valueOf("UNKNOWN"));
    }

    public void testExtensions() {
        assertEquals("jp2", SourceFormat.JP2.getExtension());
        assertEquals("jpg", SourceFormat.JPG.getExtension());
        assertEquals("png", SourceFormat.PNG.getExtension());
        assertEquals("tif", SourceFormat.TIF.getExtension());
        assertEquals("unknown", SourceFormat.UNKNOWN.getExtension());
    }

    public void testMediaTypes() {
        assertEquals("image/jp2", SourceFormat.JP2.getMediaType());
        assertEquals("image/jpeg", SourceFormat.JPG.getMediaType());
        assertEquals("image/png", SourceFormat.PNG.getMediaType());
        assertEquals("image/tiff", SourceFormat.TIF.getMediaType());
        assertEquals("unknown/unknown", SourceFormat.UNKNOWN.getMediaType());
    }

    public void testToString() {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            assertEquals(sourceFormat.getExtension(), sourceFormat.toString());
        }
    }

}
