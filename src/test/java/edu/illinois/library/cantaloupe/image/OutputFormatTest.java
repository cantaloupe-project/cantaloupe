package edu.illinois.library.cantaloupe.image;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class OutputFormatTest {

    @Test
    public void testValues() {
        assertNotNull(OutputFormat.valueOf("GIF"));
        assertNotNull(OutputFormat.valueOf("JP2"));
        assertNotNull(OutputFormat.valueOf("JPG"));
        assertNotNull(OutputFormat.valueOf("PDF"));
        assertNotNull(OutputFormat.valueOf("PNG"));
        assertNotNull(OutputFormat.valueOf("TIF"));
        assertNotNull(OutputFormat.valueOf("WEBP"));
    }

    @Test
    public void testGetOutputFormatWithString() {
        assertEquals(OutputFormat.GIF,
                OutputFormat.getOutputFormat("image/gif"));
        assertEquals(OutputFormat.JP2,
                OutputFormat.getOutputFormat("image/jp2"));
        assertEquals(OutputFormat.JPG,
                OutputFormat.getOutputFormat("image/jpeg"));
        assertEquals(OutputFormat.PDF,
                OutputFormat.getOutputFormat("application/pdf"));
        assertEquals(OutputFormat.PNG,
                OutputFormat.getOutputFormat("image/png"));
        assertEquals(OutputFormat.TIF,
                OutputFormat.getOutputFormat("image/tiff"));
        assertEquals(OutputFormat.WEBP,
                OutputFormat.getOutputFormat("image/webp"));
    }

    @Test
    public void testExtensions() {
        assertEquals("gif", OutputFormat.GIF.getExtension());
        assertEquals("jp2", OutputFormat.JP2.getExtension());
        assertEquals("jpg", OutputFormat.JPG.getExtension());
        assertEquals("pdf", OutputFormat.PDF.getExtension());
        assertEquals("png", OutputFormat.PNG.getExtension());
        assertEquals("tif", OutputFormat.TIF.getExtension());
        assertEquals("webp", OutputFormat.WEBP.getExtension());
    }

    @Test
    public void testIsEqualWithOutputFormat() {
        assertTrue(OutputFormat.GIF.isEqual(OutputFormat.GIF));
        assertTrue(OutputFormat.JP2.isEqual(OutputFormat.JP2));
        assertTrue(OutputFormat.JPG.isEqual(OutputFormat.JPG));
        assertTrue(OutputFormat.PDF.isEqual(OutputFormat.PDF));
        assertTrue(OutputFormat.PNG.isEqual(OutputFormat.PNG));
        assertTrue(OutputFormat.TIF.isEqual(OutputFormat.TIF));
        assertTrue(OutputFormat.WEBP.isEqual(OutputFormat.WEBP));
        assertFalse(OutputFormat.GIF.isEqual(OutputFormat.JPG));
    }

    @Test
    public void testIsEqualWithSourceFormat() {
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

    @Test
    public void testMediaTypes() {
        assertEquals("image/gif", OutputFormat.GIF.getMediaType());
        assertEquals("image/jp2", OutputFormat.JP2.getMediaType());
        assertEquals("image/jpeg", OutputFormat.JPG.getMediaType());
        assertEquals("application/pdf", OutputFormat.PDF.getMediaType());
        assertEquals("image/png", OutputFormat.PNG.getMediaType());
        assertEquals("image/tiff", OutputFormat.TIF.getMediaType());
        assertEquals("image/webp", OutputFormat.WEBP.getMediaType());
    }

    @Test
    public void testSupportsTransparency() {
        assertTrue(OutputFormat.GIF.supportsTransparency());
        assertTrue(OutputFormat.JP2.supportsTransparency());
        assertFalse(OutputFormat.JPG.supportsTransparency());
        assertFalse(OutputFormat.PDF.supportsTransparency());
        assertTrue(OutputFormat.PNG.supportsTransparency());
        assertTrue(OutputFormat.TIF.supportsTransparency());
        assertTrue(OutputFormat.WEBP.supportsTransparency());
    }

    @Test
    public void testToMap() {
        Map<String,Object> map = OutputFormat.JPG.toMap();
        assertEquals("jpg", map.get("extension"));
        assertEquals("image/jpeg", map.get("media_type"));
    }

    @Test
    public void testToString() {
        for (OutputFormat outputFormat : OutputFormat.values()) {
            assertEquals(outputFormat.getExtension(), outputFormat.toString());
        }
    }

}
