package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class StringWatermarkTest {

    private StringWatermark instance;

    @Before
    public void setUp() {
        instance = new StringWatermark("cats", Position.BOTTOM_RIGHT, 5,
                new Font("Helvetica", Font.PLAIN, 12), Color.blue);
    }

    @Test
    public void testToMap() {
        Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getString(), map.get("string"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(ColorUtil.getHex(instance.getColor()), map.get("color"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
        assertEquals(instance.getFont().getFamily(), map.get("font"));
        assertEquals(instance.getFont().getSize(), map.get("font_size"));
    }

    @Test
    public void testToString() throws IOException {
        instance.setString("DOGSdogs123!@#$%\n%^&*()");
        assertEquals("DOGSdogs123_SE_5_Helvetica_12_#0000FF", instance.toString());
    }

}
