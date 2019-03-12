package edu.illinois.library.cantaloupe.processor.codec.turbojpeg;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.Rational;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class TurboJPEGImageWriterTest extends BaseTest {

    private TurboJPEGImageWriter instance;
    private InputStream inputStream;

    private static void assertDimensions(ByteArrayOutputStream data,
                                         int width,
                                         int height) throws IOException {
        ImageInputStream is = ImageIO.createImageInputStream(
                new ByteArrayInputStream(data.toByteArray()));
        BufferedImage image = ImageIO.read(is); // also closes the stream
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());

    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Path image = TestUtil.getImage("jpg");
        inputStream = Files.newInputStream(image);
        instance = new TurboJPEGImageWriter();
    }

    @Test
    public void testSetProgressive() throws Exception {
        try (TurboJPEGImageReader reader = new TurboJPEGImageReader()) {
            reader.setSource(inputStream);
            reader.setScale(new Rational(1, 2));
            TurboJPEGImage image = reader.read();

            ByteArrayOutputStream os1 = new ByteArrayOutputStream();
            instance.setProgressive(false);
            instance.write(image, os1);

            ByteArrayOutputStream os2 = new ByteArrayOutputStream();
            instance.setProgressive(true);
            instance.write(image, os2);

            assertNotEquals(os1.size(), os2.size());
        }
    }

    @Test
    public void testSetQuality() throws Exception {
        try (TurboJPEGImageReader reader = new TurboJPEGImageReader()) {
            reader.setSource(inputStream);
            reader.setScale(new Rational(1, 2));
            TurboJPEGImage image = reader.read();

            ByteArrayOutputStream os1 = new ByteArrayOutputStream();
            instance.setQuality(90);
            instance.write(image, os1);

            ByteArrayOutputStream os2 = new ByteArrayOutputStream();
            instance.setQuality(30);
            instance.write(image, os2);

            assertTrue(os1.size() > os2.size());
        }
    }

    @Test
    public void testWriteWithCompressedTurboJPEGImage() throws Exception {
        try (TurboJPEGImageReader reader = new TurboJPEGImageReader()) {
            reader.setSource(inputStream);
            reader.setRegion(0, 0, 50, 30);
            TurboJPEGImage image = reader.read();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            instance.write(image, os);
            assertDimensions(os, image.getScaledWidth(), image.getScaledHeight());
        }
    }

    @Test
    public void testWriteWithDecompressedTurboJPEGImage() throws Exception {
        try (TurboJPEGImageReader reader = new TurboJPEGImageReader()) {
            reader.setSource(inputStream);
            reader.setScale(new Rational(1, 2));
            TurboJPEGImage image = reader.read();

            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                instance.write(image, os);
                assertDimensions(
                        os, image.getScaledWidth(), image.getScaledHeight());
            }
        }
    }

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        Path path = TestUtil.getImage("jpg");
        BufferedImage image = ImageIO.read(path.toFile());

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.write(image, os);
            assertDimensions(os, image.getWidth(), image.getHeight());
        }
    }

}