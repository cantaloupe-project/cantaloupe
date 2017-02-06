package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.ColorUtil;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StringOverlayTest extends BaseTest {

    private StringOverlay instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.FAMILY, "Helvetica");
        attributes.put(TextAttribute.SIZE, 12);
        attributes.put(TextAttribute.WEIGHT, 2.0f);
        attributes.put(TextAttribute.TRACKING, 0.1f);
        final Font font = Font.getFont(attributes);

        instance = new StringOverlay("cats", Position.BOTTOM_RIGHT, 5,
                font, 11, Color.blue, Color.orange, Color.red, 5f);
    }

    @Test
    public void testHasEffect() {
        assertTrue(instance.hasEffect());
        instance.setString("");
        assertFalse(instance.hasEffect());
    }

    @Test
    public void testToMap() {
        Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(ColorUtil.getRGBAHex(instance.getBackgroundColor()),
                map.get("background_color"));
        assertEquals(ColorUtil.getRGBAHex(instance.getColor()), map.get("color"));
        assertEquals(instance.getFont().getFamily(), map.get("font"));
        assertEquals(instance.getFont().getSize(), map.get("font_size"));
        assertEquals(instance.getFont().getAttributes().get(TextAttribute.WEIGHT),
                map.get("font_weight"));
        assertEquals(instance.getFont().getAttributes().get(TextAttribute.TRACKING),
                map.get("glyph_spacing"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
        assertEquals(instance.getString(), map.get("string"));
        assertEquals(ColorUtil.getRGBAHex(instance.getStrokeColor()),
                map.get("stroke_color"));
        assertEquals(5f, map.get("stroke_width"));
    }

    @Test
    public void testToString() throws IOException {
        instance.setString("DOGSdogs123!@#$%\n%^&*()");
        assertEquals("801774c691b35cbd89e3bd8cb6803681_SE_5_Helvetica_12_2.0_0.1_#0000FFFF_#FFC800FF_#FF0000FF_5.0",
                instance.toString());
    }

}
