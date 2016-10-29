package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class StringWatermarkTest {

    private StringWatermark instance;

    @Before
    public void setUp() {
        instance = new StringWatermark();
    }

    @Test
    public void testConstructor() throws Exception {
        assertNull(instance.getString());
        assertNull(instance.getPosition());
        assertEquals(0, instance.getInset());
    }

    @Test
    public void testToMap() {
        instance.setString("cats");
        instance.setInset(5);
        instance.setColor(Color.blue);
        instance.setPosition(Position.BOTTOM_RIGHT);

        Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getString(), map.get("string"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(ColorUtil.getHex(instance.getColor()), map.get("color"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
    }

    @Test
    public void testToString() throws IOException {
        instance.setString("DOGSdogs123!@#$%\n%^&*()");
        instance.setPosition(Position.BOTTOM_LEFT);
        instance.setInset(10);
        instance.setColor(Color.red);
        assertEquals("DOGSdogs123_SW_10_#FF0000", instance.toString());
    }

}
