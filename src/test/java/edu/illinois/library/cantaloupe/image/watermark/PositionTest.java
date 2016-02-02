package edu.illinois.library.cantaloupe.image.watermark;

import org.junit.Test;

import static org.junit.Assert.*;

public class PositionTest {

    @Test
    public void testToString() throws Exception {
        assertEquals("N", Position.TOP_CENTER.toString());
        assertEquals("NE", Position.TOP_RIGHT.toString());
        assertEquals("E", Position.RIGHT_CENTER.toString());
        assertEquals("SE", Position.BOTTOM_RIGHT.toString());
        assertEquals("S", Position.BOTTOM_CENTER.toString());
        assertEquals("SW", Position.BOTTOM_LEFT.toString());
        assertEquals("W", Position.LEFT_CENTER.toString());
        assertEquals("NW", Position.TOP_LEFT.toString());
        assertEquals("C", Position.CENTER.toString());
    }

}
