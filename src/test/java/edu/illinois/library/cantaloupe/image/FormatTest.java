package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class FormatTest extends BaseTest {

    @Test
    public void inferFormatWithIdentifier() {
        // AVI
        assertEquals(Format.AVI,
                Format.inferFormat(new Identifier("bla.avi")));
        assertEquals(Format.AVI,
                Format.inferFormat(new Identifier("bla.AVI")));
        // BMP
        assertEquals(Format.BMP,
                Format.inferFormat(new Identifier("bla.bmp")));
        // DCM
        assertEquals(Format.DCM,
                Format.inferFormat(new Identifier("bla.dcm")));
        // FLV
        assertEquals(Format.FLV,
                Format.inferFormat(new Identifier("bla.flv")));
        // GIF
        assertEquals(Format.GIF,
                Format.inferFormat(new Identifier("bla.gif")));
        // JP2
        assertEquals(Format.JP2,
                Format.inferFormat(new Identifier("bla.jp2")));
        assertEquals(Format.JP2,
                Format.inferFormat(new Identifier("bla.jpx")));
        // JPG
        assertEquals(Format.JPG,
                Format.inferFormat(new Identifier("bla.jpg")));
        // MOV
        assertEquals(Format.MOV,
                Format.inferFormat(new Identifier("bla.mov")));
        // MP4
        assertEquals(Format.MP4,
                Format.inferFormat(new Identifier("bla.mp4")));
        // MPG
        assertEquals(Format.MPG,
                Format.inferFormat(new Identifier("bla.mpg")));
        // PDF
        assertEquals(Format.PDF,
                Format.inferFormat(new Identifier("bla.pdf")));
        // PNG
        assertEquals(Format.PNG,
                Format.inferFormat(new Identifier("bla.png")));
        // TIF
        assertEquals(Format.TIF,
                Format.inferFormat(new Identifier("bla.tif")));
        // WEBM
        assertEquals(Format.WEBM,
                Format.inferFormat(new Identifier("bla.webm")));
        // WEBP
        assertEquals(Format.WEBP,
                Format.inferFormat(new Identifier("bla.webp")));
        // UNKNOWN
        assertEquals(Format.UNKNOWN,
                Format.inferFormat(new Identifier("bla.bogus")));
    }

    @Test
    public void inferFormatWithString() {
        // AVI
        assertEquals(Format.AVI, Format.inferFormat("bla.avi"));
        assertEquals(Format.AVI, Format.inferFormat("bla.AVI"));
        // BMP
        assertEquals(Format.BMP, Format.inferFormat("bla.bmp"));
        // DCM
        assertEquals(Format.DCM, Format.inferFormat("bla.dcm"));
        // FLV
        assertEquals(Format.FLV, Format.inferFormat("bla.flv"));
        // GIF
        assertEquals(Format.GIF, Format.inferFormat("bla.gif"));
        // JP2
        assertEquals(Format.JP2, Format.inferFormat("bla.jp2"));
        assertEquals(Format.JP2, Format.inferFormat("bla.jpx"));
        // JPG
        assertEquals(Format.JPG, Format.inferFormat("bla.jpg"));
        // MOV
        assertEquals(Format.MOV, Format.inferFormat("bla.mov"));
        // MP4
        assertEquals(Format.MP4, Format.inferFormat("bla.mp4"));
        // MPG
        assertEquals(Format.MPG, Format.inferFormat("bla.mpg"));
        // PDF
        assertEquals(Format.PDF, Format.inferFormat("bla.pdf"));
        // PNG
        assertEquals(Format.PNG, Format.inferFormat("bla.png"));
        // TIF
        assertEquals(Format.TIF, Format.inferFormat("bla.tif"));
        // WEBM
        assertEquals(Format.WEBM, Format.inferFormat("bla.webm"));
        // WEBP
        assertEquals(Format.WEBP, Format.inferFormat("bla.webp"));
        // UNKNOWN
        assertEquals(Format.UNKNOWN, Format.inferFormat("bla.bogus"));
    }

    @Test
    public void getExtensions() {
        // AVI
        assertEquals(List.of("avi"), Format.AVI.getExtensions());
        // BMP
        assertEquals(List.of("bmp", "dib"), Format.BMP.getExtensions());
        // DCM
        assertEquals(List.of("dcm", "dic"), Format.DCM.getExtensions());
        // FLV
        assertEquals(List.of("flv", "f4v"), Format.FLV.getExtensions());
        // GIF
        assertEquals(List.of("gif"), Format.GIF.getExtensions());
        // JP2
        assertEquals(List.of("jp2", "j2k", "jpx", "jpf"), Format.JP2.getExtensions());
        // JPG
        assertEquals(List.of("jpg", "jpeg"), Format.JPG.getExtensions());
        // MOV
        assertEquals(List.of("mov", "qt"), Format.MOV.getExtensions());
        // MP4
        assertEquals(List.of("mp4", "m4v"), Format.MP4.getExtensions());
        // MPG
        assertEquals(List.of("mpg"), Format.MPG.getExtensions());
        // PDF
        assertEquals(List.of("pdf"), Format.PDF.getExtensions());
        // PNG
        assertEquals(List.of("png"), Format.PNG.getExtensions());
        // TIF
        assertEquals(List.of("tif", "ptif", "tiff"),
                Format.TIF.getExtensions());
        // WEBM
        assertEquals(List.of("webm"), Format.WEBM.getExtensions());
        // WEBP
        assertEquals(List.of("webp"), Format.WEBP.getExtensions());
        // UNKNOWN
        assertEquals(List.of("unknown"), Format.UNKNOWN.getExtensions());
    }

    @Test
    public void getImageType() {
        assertEquals(Format.ImageType.RASTER, Format.AVI.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.BMP.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.DCM.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.FLV.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.GIF.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.JP2.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.JPG.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.MOV.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.MP4.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.MPG.getImageType());
        assertEquals(Format.ImageType.VECTOR, Format.PDF.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.PNG.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.TIF.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.WEBM.getImageType());
        assertEquals(Format.ImageType.RASTER, Format.WEBP.getImageType());
        assertEquals(Format.ImageType.UNKNOWN, Format.UNKNOWN.getImageType());
    }

    @Test
    public void getMaxSampleSize() {
        assertEquals(8, Format.AVI.getMaxSampleSize());
        assertEquals(8, Format.BMP.getMaxSampleSize());
        assertEquals(16, Format.DCM.getMaxSampleSize());
        assertEquals(8, Format.FLV.getMaxSampleSize());
        assertEquals(3, Format.GIF.getMaxSampleSize());
        assertEquals(16, Format.JP2.getMaxSampleSize());
        assertEquals(8, Format.JPG.getMaxSampleSize());
        assertEquals(8, Format.MOV.getMaxSampleSize());
        assertEquals(8, Format.MP4.getMaxSampleSize());
        assertEquals(8, Format.MPG.getMaxSampleSize());
        assertEquals(16, Format.PDF.getMaxSampleSize());
        assertEquals(16, Format.PNG.getMaxSampleSize());
        assertEquals(16, Format.TIF.getMaxSampleSize());
        assertEquals(8, Format.WEBM.getMaxSampleSize());
        assertEquals(8, Format.WEBP.getMaxSampleSize());
        assertEquals(0, Format.UNKNOWN.getMaxSampleSize());
    }

    @Test
    public void getMediaTypes() {
        // AVI
        assertEquals(List.of(
                new MediaType("video/avi"),
                new MediaType("video/msvideo"),
                new MediaType("video/x-msvideo")),
                Format.AVI.getMediaTypes());
        // BMP
        assertEquals(List.of(
                new MediaType("image/bmp"),
                new MediaType("image/x-bmp"),
                new MediaType("image/x-ms-bmp")),
                Format.BMP.getMediaTypes());
        // DCM
        assertEquals(List.of(
                new MediaType("application/dicom")),
                Format.DCM.getMediaTypes());
        // FLV
        assertEquals(List.of(
                new MediaType("video/x-flv")),
                Format.FLV.getMediaTypes());
        // GIF
        assertEquals(List.of(
                new MediaType("image/gif")),
                Format.GIF.getMediaTypes());
        // JP2
        assertEquals(List.of(
                new MediaType("image/jp2")),
                Format.JP2.getMediaTypes());
        // JPG
        assertEquals(List.of(
                new MediaType("image/jpeg")),
                Format.JPG.getMediaTypes());
        // MOV
        assertEquals(List.of(
                new MediaType("video/quicktime"),
                new MediaType("video/x-quicktime")),
                Format.MOV.getMediaTypes());
        // MP4
        assertEquals(List.of(
                new MediaType("video/mp4")),
                Format.MP4.getMediaTypes());
        // MPG
        assertEquals(List.of(
                new MediaType("video/mpeg")),
                Format.MPG.getMediaTypes());
        // PDF
        assertEquals(List.of(
                new MediaType("application/pdf")),
                Format.PDF.getMediaTypes());
        // PNG
        assertEquals(List.of(
                new MediaType("image/png")),
                Format.PNG.getMediaTypes());
        // TIF
        assertEquals(List.of(
                new MediaType("image/tiff")),
                Format.TIF.getMediaTypes());
        // WEBM
        assertEquals(List.of(
                new MediaType("video/webm")),
                Format.WEBM.getMediaTypes());
        // WEBP
        assertEquals(List.of(
                new MediaType("image/webp")),
                Format.WEBP.getMediaTypes());
        // UNKNOWN
        assertEquals(List.of(
                new MediaType("unknown/unknown")),
                Format.UNKNOWN.getMediaTypes());
    }

    @Test
    public void getName() {
        assertEquals("AVI", Format.AVI.getName());
        assertEquals("BMP", Format.BMP.getName());
        assertEquals("DICOM", Format.DCM.getName());
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
    public void getPreferredExtension() {
        assertEquals("avi", Format.AVI.getPreferredExtension());
        assertEquals("bmp", Format.BMP.getPreferredExtension());
        assertEquals("dcm", Format.DCM.getPreferredExtension());
        assertEquals("flv", Format.FLV.getPreferredExtension());
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
    public void getPreferredMediaType() {
        assertEquals("video/avi",
                Format.AVI.getPreferredMediaType().toString());
        assertEquals("image/bmp",
                Format.BMP.getPreferredMediaType().toString());
        assertEquals("application/dicom",
                Format.DCM.getPreferredMediaType().toString());
        assertEquals("video/x-flv",
                Format.FLV.getPreferredMediaType().toString());
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
    public void getType() {
        assertEquals(Format.Type.VIDEO, Format.AVI.getType());
        assertEquals(Format.Type.IMAGE, Format.BMP.getType());
        assertEquals(Format.Type.IMAGE, Format.DCM.getType());
        assertEquals(Format.Type.VIDEO, Format.FLV.getType());
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
        assertEquals(Format.Type.UNKNOWN, Format.UNKNOWN.getType());
    }

    @Test
    public void isImage() {
        assertFalse(Format.AVI.isImage());
        assertTrue(Format.BMP.isImage());
        assertTrue(Format.DCM.isImage());
        assertFalse(Format.FLV.isImage());
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
    public void isVideo() {
        assertTrue(Format.AVI.isVideo());
        assertFalse(Format.BMP.isVideo());
        assertFalse(Format.DCM.isVideo());
        assertTrue(Format.FLV.isVideo());
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
    public void supportsTransparency() {
        assertFalse(Format.AVI.supportsTransparency());
        assertTrue(Format.BMP.supportsTransparency());
        assertFalse(Format.DCM.supportsTransparency());
        assertFalse(Format.FLV.supportsTransparency());
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
    public void toMap() {
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
