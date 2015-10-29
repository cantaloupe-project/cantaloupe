package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import org.restlet.data.MediaType;

public class SourceFormatTest extends CantaloupeTestCase {

    public void testValues() {
        assertNotNull(SourceFormat.valueOf("BMP"));
        assertNotNull(SourceFormat.valueOf("GIF"));
        assertNotNull(SourceFormat.valueOf("JP2"));
        assertNotNull(SourceFormat.valueOf("JPG"));
        assertNotNull(SourceFormat.valueOf("PDF"));
        assertNotNull(SourceFormat.valueOf("PNG"));
        assertNotNull(SourceFormat.valueOf("TIF"));
        assertNotNull(SourceFormat.valueOf("WEBP"));
        assertNotNull(SourceFormat.valueOf("UNKNOWN"));
    }

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

    public void testGetExtensions() {
        assertTrue(SourceFormat.BMP.getExtensions().contains("bmp"));
        assertTrue(SourceFormat.GIF.getExtensions().contains("gif"));
        assertTrue(SourceFormat.JP2.getExtensions().contains("jp2"));
        assertTrue(SourceFormat.JPG.getExtensions().contains("jpg"));
        assertTrue(SourceFormat.JPG.getExtensions().contains("jpeg"));
        assertTrue(SourceFormat.PDF.getExtensions().contains("pdf"));
        assertTrue(SourceFormat.PNG.getExtensions().contains("png"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("ptif"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("tif"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("tiff"));
        assertTrue(SourceFormat.TIF.getExtensions().contains("tiff"));
        assertTrue(SourceFormat.WEBP.getExtensions().contains("webp"));
        assertTrue(SourceFormat.UNKNOWN.getExtensions().contains("unknown"));
    }

    public void testGetMediaTypes() {
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
        assertTrue(SourceFormat.PDF.getMediaTypes().
                contains(new MediaType("application/pdf")));
        assertTrue(SourceFormat.PNG.getMediaTypes().
                contains(new MediaType("image/png")));
        assertTrue(SourceFormat.TIF.getMediaTypes().
                contains(new MediaType("image/tiff")));
        assertTrue(SourceFormat.WEBP.getMediaTypes().
                contains(new MediaType("image/webp")));
        assertTrue(SourceFormat.UNKNOWN.getMediaTypes().
                contains(new MediaType("unknown/unknown")));
    }

    public void testGetName() {
        assertEquals("BMP", SourceFormat.BMP.getName());
        assertEquals("GIF", SourceFormat.GIF.getName());
        assertEquals("JPEG2000", SourceFormat.JP2.getName());
        assertEquals("JPEG", SourceFormat.JPG.getName());
        assertEquals("PDF", SourceFormat.PDF.getName());
        assertEquals("PNG", SourceFormat.PNG.getName());
        assertEquals("TIFF", SourceFormat.TIF.getName());
        assertEquals("WebP", SourceFormat.WEBP.getName());
        assertEquals("Unknown", SourceFormat.UNKNOWN.getName());
    }

    public void testGetPreferredMediaType() {
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
        assertEquals("image/png",
                SourceFormat.PNG.getPreferredMediaType().toString());
        assertEquals("image/tiff",
                SourceFormat.TIF.getPreferredMediaType().toString());
        assertEquals("image/webp",
                SourceFormat.WEBP.getPreferredMediaType().toString());
        assertEquals("unknown/unknown",
                SourceFormat.UNKNOWN.getPreferredMediaType().toString());
    }

    public void testGetPreferredExtension() {
        assertEquals("bmp", SourceFormat.BMP.getPreferredExtension());
        assertEquals("gif", SourceFormat.GIF.getPreferredExtension());
        assertEquals("jp2", SourceFormat.JP2.getPreferredExtension());
        assertEquals("jpg", SourceFormat.JPG.getPreferredExtension());
        assertEquals("pdf", SourceFormat.PDF.getPreferredExtension());
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
