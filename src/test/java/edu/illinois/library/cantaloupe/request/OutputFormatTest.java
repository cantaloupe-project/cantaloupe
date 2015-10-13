package edu.illinois.library.cantaloupe.request;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import junit.framework.TestCase;

public class OutputFormatTest extends TestCase {

    public void testValues() {
        assertNotNull(OutputFormat.valueOf("GIF"));
        assertNotNull(OutputFormat.valueOf("JP2"));
        assertNotNull(OutputFormat.valueOf("JPG"));
        assertNotNull(OutputFormat.valueOf("PDF"));
        assertNotNull(OutputFormat.valueOf("PNG"));
        assertNotNull(OutputFormat.valueOf("TIF"));
        assertNotNull(OutputFormat.valueOf("WEBP"));
    }

    public void testExtensions() {
        assertEquals("gif", OutputFormat.GIF.getExtension());
        assertEquals("jp2", OutputFormat.JP2.getExtension());
        assertEquals("jpg", OutputFormat.JPG.getExtension());
        assertEquals("pdf", OutputFormat.PDF.getExtension());
        assertEquals("png", OutputFormat.PNG.getExtension());
        assertEquals("tif", OutputFormat.TIF.getExtension());
        assertEquals("webp", OutputFormat.WEBP.getExtension());
    }

    public void testIsEqual() {
        assertTrue(OutputFormat.GIF.isEqual(SourceFormat.GIF));
        assertTrue(OutputFormat.JP2.isEqual(SourceFormat.JP2));
        assertTrue(OutputFormat.JPG.isEqual(SourceFormat.JPG));
        assertTrue(OutputFormat.PDF.isEqual(SourceFormat.PDF));
        assertTrue(OutputFormat.PNG.isEqual(SourceFormat.PNG));
        assertTrue(OutputFormat.TIF.isEqual(SourceFormat.TIF));
        assertTrue(OutputFormat.WEBP.isEqual(SourceFormat.WEBP));
        assertFalse(OutputFormat.GIF.isEqual(SourceFormat.UNKNOWN));
        assertFalse(OutputFormat.GIF.isEqual(SourceFormat.JPG));
    }

    public void testMediaTypes() {
        assertEquals("image/gif", OutputFormat.GIF.getMediaType());
        assertEquals("image/jp2", OutputFormat.JP2.getMediaType());
        assertEquals("image/jpeg", OutputFormat.JPG.getMediaType());
        assertEquals("application/pdf", OutputFormat.PDF.getMediaType());
        assertEquals("image/png", OutputFormat.PNG.getMediaType());
        assertEquals("image/tiff", OutputFormat.TIF.getMediaType());
        assertEquals("image/webp", OutputFormat.WEBP.getMediaType());
    }

    public void testToString() {
        for (OutputFormat outputFormat : OutputFormat.values()) {
            assertEquals(outputFormat.getExtension(), outputFormat.toString());
        }
    }

}
