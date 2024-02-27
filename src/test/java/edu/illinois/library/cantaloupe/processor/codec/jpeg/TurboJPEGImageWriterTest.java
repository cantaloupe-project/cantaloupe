package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.Rational;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.assertRGBA;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess") // a JMH test extends this
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

    @BeforeEach
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
    public void testSetXMP() throws Exception {
        try (TurboJPEGImageReader reader = new TurboJPEGImageReader()) {
            reader.setSource(inputStream);
            BufferedImage image = reader.readAsBufferedImage(new Rectangle());

            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            final String xmp = "<xmp>this is some fake data</xmp>";
            instance.setXMP(xmp);
            instance.write(image, os);
            final byte[] actual = os.toByteArray();

            final ByteArrayOutputStream expectedSegment = new ByteArrayOutputStream();
            final byte[] headerBytes = "http://ns.adobe.com/xap/1.0/\0".getBytes();
            final byte[] xmpBytes = Metadata.encapsulateXMP(xmp).
                    getBytes(StandardCharsets.UTF_8);
            // write segment marker
            expectedSegment.write(new byte[]{(byte) 0xff, (byte) 0xe1});
            // write segment length
            expectedSegment.write(ByteBuffer.allocate(2)
                    .putShort((short) (headerBytes.length + xmpBytes.length + 3))
                    .array());
            // write segment header
            expectedSegment.write(headerBytes);
            // write XMP data
            expectedSegment.write(xmpBytes);
            // write null terminator
            expectedSegment.write(new byte[]{0x00});
            final byte[] expected = expectedSegment.toByteArray();

            assertArrayEquals(expected,
                    Arrays.copyOfRange(actual, 20, expected.length + 20));
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

    @Test
    public void testWriteWithGrayBufferedImage() throws Exception {
        BufferedImage image = new BufferedImage(50, 50,
                BufferedImage.TYPE_BYTE_GRAY);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.write(image, os);
            assertDimensions(os, image.getWidth(), image.getHeight());
        }
    }

    @Test
    public void testWriteWithBufferedImageWithBackgroundColor()
            throws Exception {
        BufferedImage image = new BufferedImage(50, 50,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setBackground(Color.RED);
        g2d.clearRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.write(image, os);
            try (InputStream is = new ByteArrayInputStream(os.toByteArray())) {
                ImageInputStream iis = ImageIO.createImageInputStream(is);
                BufferedImage jpegImage = ImageIO.read(iis);
                assertRGBA(jpegImage.getRGB(0, 0), 254, 0, 0, 255);
            }
        }
    }

}
