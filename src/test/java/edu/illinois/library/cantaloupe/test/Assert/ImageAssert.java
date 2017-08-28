package edu.illinois.library.cantaloupe.test.Assert;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public final class ImageAssert {

    /**
     * Asserts that the given pixel is some shade of gray.
     *
     * @param pixel Pixel to test.
     */
    public static void assertGray(int pixel) {
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(red, green);
        assertEquals(green, blue);
    }

    /**
     * @param pixel Pixel to test.
     * @param expectedRed Expected red value of the pixel.
     * @param expectedGreen Expected green value of the pixel.
     * @param expectedBlue Expected blue value of the pixel.
     * @param expectedAlpha Expected alpha value of the pixel.
     */
    public static void assertRGBA(int pixel,
                                  int expectedRed,
                                  int expectedGreen,
                                  int expectedBlue,
                                  int expectedAlpha) {
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(expectedAlpha, alpha);
        assertEquals(expectedRed, red);
        assertEquals(expectedGreen, green);
        assertEquals(expectedBlue, blue);
    }

    public static void assertSampleSize(int expectedSampleSize,
                                        Object image) throws IOException {
        try {
            if (image instanceof byte[]) {
                image = new ByteArrayInputStream((byte[]) image);
                image = ImageIO.createImageInputStream(image);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(image);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(image);
                BufferedImage bImage = reader.read(0);
                assertEquals(expectedSampleSize,
                        bImage.getColorModel().getComponentSize(0));
            } else {
                throw new IOException("No reader available for " + image);
            }
        } finally {
            if (image instanceof Closeable) {
                ((Closeable) image).close();
            }
        }
    }

    private ImageAssert() {}

}
