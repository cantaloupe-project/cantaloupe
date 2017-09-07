package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StringOverlayTest extends BaseTest {

    private StringOverlay instance;

    private Font newFont() {
        final Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.FAMILY, "Arial");
        attributes.put(TextAttribute.SIZE, 12);
        attributes.put(TextAttribute.WEIGHT, 2.0f);
        attributes.put(TextAttribute.TRACKING, 0.1f);
        return Font.getFont(attributes);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new StringOverlay("cats", Position.BOTTOM_RIGHT, 5,
                newFont(), 11, Color.BLUE, Color.ORANGE, Color.RED, 5f);
    }

    @Test
    public void hasEffect() {
        assertTrue(instance.hasEffect());
        instance.setString("");
        assertFalse(instance.hasEffect());
    }

    @Test(expected = IllegalStateException.class)
    public void setBackgroundColorThrowsExceptionWhenFrozen() {
        instance.freeze();
        instance.setBackgroundColor(Color.RED);
    }

    @Test(expected = IllegalStateException.class)
    public void setColorThrowsExceptionWhenFrozen() {
        instance.freeze();
        instance.setColor(Color.RED);
    }

    @Test(expected = IllegalStateException.class)
    public void setFontThrowsExceptionWhenFrozen() {
        instance.freeze();
        instance.setFont(newFont());
    }

    @Test(expected = IllegalStateException.class)
    public void setMinSizeThrowsExceptionWhenFrozen() {
        instance.freeze();
        instance.setMinSize(1);
    }

    @Test(expected = IllegalStateException.class)
    public void setStringThrowsExceptionWhenFrozen() {
        instance.freeze();
        instance.setString("");
    }

    @Test(expected = IllegalStateException.class)
    public void setStrokeColorThrowsExceptionWhenFrozen() {
        instance.freeze();
        instance.setStrokeColor(Color.RED);
    }

    @Test(expected = IllegalStateException.class)
    public void setStrokeWidthThrowsExceptionWhenFrozen() {
        instance.freeze();
        instance.setStrokeWidth(3);
    }

    @Test
    public void toMap() {
        Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(instance.getBackgroundColor().toRGBAHex(),
                map.get("background_color"));
        assertEquals(instance.getColor().toRGBAHex(), map.get("color"));
        assertEquals(instance.getFont().getName(), map.get("font"));
        assertEquals(instance.getFont().getSize(), map.get("font_size"));
        assertEquals(instance.getFont().getAttributes().get(TextAttribute.WEIGHT),
                map.get("font_weight"));
        assertEquals(instance.getFont().getAttributes().get(TextAttribute.TRACKING),
                map.get("glyph_spacing"));
        assertEquals(instance.getInset(), map.get("inset"));
        assertEquals(instance.getPosition().toString(), map.get("position"));
        assertEquals(instance.getString(), map.get("string"));
        assertEquals(instance.getStrokeColor().toRGBAHex(),
                map.get("stroke_color"));
        assertEquals(5f, map.get("stroke_width"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        map.put("test", "test");
    }

    @Test
    public void testToString() throws IOException {
        instance.setString("DOGSdogs123!@#$%\n%^&*()");
        assertEquals("801774c691b35cbd89e3bd8cb6803681_SE_5_Arial_12_2.0_0.1_#0000FFFF_#FFA500FF_#FF0000FF_5.0",
                instance.toString());
    }

}
