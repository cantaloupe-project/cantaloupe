package edu.illinois.library.cantaloupe.image.watermark;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class ColorUtilTest {

    @Test
    public void testGetHex() {
        assertEquals("#000000", ColorUtil.getHex(Color.black));
        assertEquals("#FF0000", ColorUtil.getHex(Color.red));
        assertEquals("#00FF00", ColorUtil.getHex(Color.green));
        assertEquals("#0000FF", ColorUtil.getHex(Color.blue));
    }

}
