package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.codec.bmp.BMPImageReader;
import edu.illinois.library.cantaloupe.processor.codec.xpm.XPMImageReader;
import edu.illinois.library.cantaloupe.processor.codec.gif.GIFImageReader;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.JPEGImageReader;
import edu.illinois.library.cantaloupe.processor.codec.png.PNGImageReader;
import edu.illinois.library.cantaloupe.processor.codec.tiff.TIFFImageReader;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class ImageReaderFactoryTest extends BaseTest {

    private ImageReaderFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageReaderFactory();
    }

    @Test
    void testSupportedFormats() {
        final HashSet<Format> expected = new HashSet<>();
        // Scan for Image I/O-supported formats by media type.
        for (String mediaTypeStr : ImageIO.getReaderMIMETypes()) {
            if (mediaTypeStr.isBlank() || mediaTypeStr.equals("image/jp2")) {
                continue;
            }
            final Format format = new MediaType(mediaTypeStr).toFormat();
            if (format != null && !format.equals(Format.UNKNOWN)) {
                expected.add(format);
            }
        }
        // Scan by extension, just in case the media type scan missed anything.
        for (String extension : ImageIO.getReaderFileSuffixes()) {
            if (extension.isBlank() || extension.equals("jp2")) {
                continue;
            }
            final Format format = Format.withExtension(extension);
            if (format != null && !format.equals(Format.UNKNOWN)) {
                expected.add(format);
            }
        }
        assertEquals(expected, ImageReaderFactory.supportedFormats());
    }

    @Test
    void testNewImageReaderWithFormatUnknown() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.newImageReader(Format.UNKNOWN, Paths.get("/dev/null")));
    }

    @Test
    void testNewImageReaderWithFormatBMP() {
        ImageReader reader = instance.newImageReader(Format.BMP);
        assertTrue(reader instanceof BMPImageReader);
    }

    @Test
    void testNewImageReaderWithFormatGIF() {
        ImageReader reader = instance.newImageReader(Format.GIF);
        assertTrue(reader instanceof GIFImageReader);
    }

    @Test
    void testNewImageReaderWithFormatJPEG() {
        ImageReader reader = instance.newImageReader(Format.JPG);
        assertTrue(reader instanceof JPEGImageReader);
    }

    @Test
    void testNewImageReaderWithFormatPNG() {
        ImageReader reader = instance.newImageReader(Format.PNG);
        assertTrue(reader instanceof PNGImageReader);
    }

    @Test
    void testNewImageReaderWithFormatTIF() {
        ImageReader reader = instance.newImageReader(Format.TIF);
        assertTrue(reader instanceof TIFFImageReader);
    }

    @Test
    void testNewImageReaderWithFormatXPM() {
        ImageReader reader = instance.newImageReader(Format.XPM);
        assertTrue(reader instanceof XPMImageReader);
    }

    @Test
    void testNewImageReaderWithPath() throws Exception {
        instance.newImageReader(Format.JPG, Paths.get("/dev/null"));
    }

    @Test
    void testNewImageReaderWithInputStream() throws Exception {
        try (InputStream is = Files.newInputStream(TestUtil.getImage("jpg"))) {
            instance.newImageReader(Format.JPG, is);
        }
    }

    @Test
    void testNewImageReaderWithImageInputStream() throws Exception {
        try (ImageInputStream iis = ImageIO.createImageInputStream(TestUtil.getImage("jpg").toFile())) {
            instance.newImageReader(Format.JPG, iis);
        }
    }

    @Test
    void testNewImageReaderWithStreamSource() throws Exception {
        StreamFactory source = new PathStreamFactory(Paths.get("/dev/null"));
        instance.newImageReader(Format.JPG, source);
    }

}
