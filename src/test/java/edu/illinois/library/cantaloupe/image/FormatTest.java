package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FormatTest extends BaseTest {

    @Test
    void inferFormatWithIdentifier() {
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
    void inferFormatWithString() {
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
    void getPreferredExtension() {
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
    void getPreferredMediaType() {
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
    void toMap() {
        Map<String,Object> map = Format.JPG.toMap();
        assertEquals("jpg", map.get("extension"));
        assertEquals("image/jpeg", map.get("media_type"));
    }

    @Test
    void testToString() {
        for (Format format : Format.values()) {
            assertEquals(format.getPreferredExtension(),
                    format.toString());
        }
    }

}
