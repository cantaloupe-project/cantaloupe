package edu.illinois.library.cantaloupe.image;

import junit.framework.TestCase;

public class FormatTest extends TestCase {

    public void testValues() {
        assertNotNull(Format.valueOf("GIF"));
        assertNotNull(Format.valueOf("JP2"));
        assertNotNull(Format.valueOf("JPG"));
        assertNotNull(Format.valueOf("PDF"));
        assertNotNull(Format.valueOf("PNG"));
        assertNotNull(Format.valueOf("TIF"));
        assertNotNull(Format.valueOf("WEBP"));
    }

}
