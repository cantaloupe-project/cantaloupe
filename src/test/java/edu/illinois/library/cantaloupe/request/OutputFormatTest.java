package edu.illinois.library.cantaloupe.request;

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
