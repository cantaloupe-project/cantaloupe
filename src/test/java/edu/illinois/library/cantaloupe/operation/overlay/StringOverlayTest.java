package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StringOverlayTest extends BaseTest {

    private StringOverlay instance;

    private Font newFont(Float weight, Float tracking) {
        final Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.FAMILY, "SansSerif");
        attributes.put(TextAttribute.SIZE, 12);
        attributes.put(TextAttribute.WEIGHT, weight);
        attributes.put(TextAttribute.TRACKING, tracking);
        return Font.getFont(attributes);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new StringOverlay("cats", Position.BOTTOM_RIGHT, 5,
                newFont(2.0f,  0.1f), 11, Color.BLUE, Color.ORANGE, Color.RED,
                5f, false);
    }

    @Test
    void hasEffect() {
        assertTrue(instance.hasEffect());
        instance.setString("");
        assertFalse(instance.hasEffect());
    }

    @Test
    void setBackgroundColorThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setBackgroundColor(Color.RED));
    }

    @Test
    void setColorThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setColor(Color.RED));
    }

    @Test
    void setFontThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setFont(newFont(2.0f,  0.1f)));
    }

    @Test
    void setMinSizeThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setMinSize(1));
    }

    @Test
    void setStringThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setString(""));
    }

    @Test
    void setStringReplacesNewlines() {
        instance.setString("test\\ntest");
        assertEquals("test\ntest", instance.getString());
    }

    @Test
    void setStrokeColorThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setStrokeColor(Color.RED));
    }

    @Test
    void setStrokeWidthThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setStrokeWidth(3));
    }

    @Test
    void setWordWrapThrowsExceptionWhenFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setWordWrap(true));
    }

    @Test
    void toMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);

        assertEquals(13, map.size());
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
        assertEquals(false, map.get("word_wrap"));
    }

    @Test
    void toMapReturnsDefaultValueForMissingFontAttributes() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        StringOverlay fontModifiedInstance = new StringOverlay("cats",
                Position.BOTTOM_RIGHT, 5, newFont(null,  null), 11,
                Color.BLUE, Color.ORANGE, Color.RED, 5f, false);
        Map<String,Object> map = fontModifiedInstance.toMap(fullSize, scaleConstraint);
        assertEquals(TextAttribute.WEIGHT_REGULAR, map.get("font_weight"));
        assertEquals(0.0f, map.get("glyph_spacing"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        instance.setString("DOGSdogs123!@#$%\n%^&*()");
        assertEquals("801774c691b35cbd89e3bd8cb6803681_SE_5_SansSerif_12_2.0_0.1_#0000FFFF_#FFA500FF_#FF0000FF_5.0_false",
                instance.toString());
    }

}
