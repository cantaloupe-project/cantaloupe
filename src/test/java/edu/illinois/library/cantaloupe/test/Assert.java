package edu.illinois.library.cantaloupe.test;

import static org.junit.Assert.assertEquals;

public abstract class Assert {

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

}
