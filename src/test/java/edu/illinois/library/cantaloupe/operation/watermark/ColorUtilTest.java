package edu.illinois.library.cantaloupe.operation.watermark;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class ColorUtilTest {

    @Test
    public void testFromString() {
        assertEquals(Color.black, ColorUtil.fromString("black"));
        assertEquals(Color.black, ColorUtil.fromString("#000000"));
        assertEquals(Color.black, ColorUtil.fromString("rgb(0, 0, 0)"));

        assertEquals(Color.red, ColorUtil.fromString("red"));
        assertEquals(Color.red, ColorUtil.fromString("#ff0000"));
        assertEquals(Color.red, ColorUtil.fromString("rgb(255, 0, 0)"));

        assertEquals(Color.green, ColorUtil.fromString("green"));
        assertEquals(Color.green, ColorUtil.fromString("#00ff00"));
        assertEquals(Color.green, ColorUtil.fromString("rgb(0, 255, 0)"));

        assertEquals(Color.blue, ColorUtil.fromString("blue"));
        assertEquals(Color.blue, ColorUtil.fromString("#0000ff"));
        assertEquals(Color.blue, ColorUtil.fromString("rgb(0, 0, 255)"));

        assertEquals(Color.white, ColorUtil.fromString("white"));
        assertEquals(Color.white, ColorUtil.fromString("#ffffff"));
        assertEquals(Color.white, ColorUtil.fromString("rgb(255, 255, 255)"));
    }

    @Test
    public void testGetHex() {
        assertEquals("#000000", ColorUtil.getHex(Color.black));
        assertEquals("#FF0000", ColorUtil.getHex(Color.red));
        assertEquals("#00FF00", ColorUtil.getHex(Color.green));
        assertEquals("#0000FF", ColorUtil.getHex(Color.blue));
    }

}
