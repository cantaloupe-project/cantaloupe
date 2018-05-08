package edu.illinois.library.cantaloupe.test.Assert;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public final class ImageAssert {

    /**
     * Asserts that all of the pixels in the given image are either black or
     * white.
     *
     * @param image Image to test.
     */
    public static void assertBitonal(BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int maxValue =
                (int) Math.pow(2, image.getColorModel().getComponentSize(0)) - 1;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int pixel = image.getRGB(x, y);
                final int red = (pixel >> 16) & 0xff;
                final int green = (pixel >> 8) & 0xff;
                final int blue = (pixel) & 0xff;
                // Allow a bit of leeway. // TODO: why is this necessary?
                assertTrue((red == maxValue && green == maxValue && blue == maxValue) ||
                        (red == maxValue - 1 && green == maxValue - 1 && blue == maxValue - 1) ||
                        (red == 0 && green == 0 && blue == 0) ||
                        (red == 1 && green == 1 && blue == 1));
            }
        }
    }

    /**
     * Asserts that all of the pixels in the given image are some shade of gray.
     *
     * @param image Image to test.
     */
    public static void assertGray(BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                assertGray(image.getRGB(x, y));
            }
        }
    }

    /**
     * Asserts that the given pixel is some shade of gray.
     *
     * @param pixel Pixel to test.
     */
    public static void assertGray(int pixel) {
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        if (red != green || green != blue) {
            fail("Red: " + red + " green: " + green + " blue: " + blue);
        }
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
                try {
                    reader.setInput(image);
                    BufferedImage bImage = reader.read(0);
                    assertEquals(expectedSampleSize,
                            bImage.getColorModel().getComponentSize(0));
                } finally {
                    reader.dispose();
                }
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
