package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Operation;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates a string overlay applied to an image.</p>
 *
 * <p>Instances should be obtained from the
 * {@link OverlayService}.</p>
 */
public class StringOverlay extends Overlay implements Operation {

    private static final Logger logger = LoggerFactory.
            getLogger(StringOverlay.class);

    private Color backgroundColor;
    private Color color;
    private Font font;
    private int minSize;
    private String string;
    private Color strokeColor;
    private float strokeWidth;

    public StringOverlay(String string, Position position, int inset,
                         Font font, int minSize, Color color,
                         Color backgroundColor, Color strokeColor,
                         float strokeWidth) {
        super(position, inset);
        this.setString(string);
        this.setFont(font);
        this.setColor(color);
        this.setBackgroundColor(backgroundColor);
        this.setMinSize(minSize);
        this.setStrokeColor(strokeColor);
        this.setStrokeWidth(strokeWidth);
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getColor() {
        return color;
    }

    public Font getFont() {
        return font;
    }

    public int getMinSize() {
        return minSize;
    }

    public String getString() {
        return string;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * @return True if the string length is greater than 0; false if not.
     */
    @Override
    public boolean hasEffect() {
        return (getString() != null && getString().length() > 0);
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public void setString(String string) {
        this.string = string;
    }

    public void setStrokeColor(Color color) {
        this.strokeColor = color;
    }

    /**
     * @param width Width in pixels.
     */
    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with <var>background_color</var>, <var>class</var>,
     *         <var>color</var>, <var>font</var>, <var>font_size</var>,
     *         <var>font_weight</var>, <var>glyph_spacing</var>,
     *         <var>inset</var>, <var>position</var>, <var>string</var>,
     *         <var>stroke_color</var>, and <var>stroke_width</var> keys.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("background_color", getBackgroundColor().toRGBAHex());
        map.put("class", getClass().getSimpleName());
        map.put("color", getColor().toRGBAHex());
        map.put("font", getFont().getName());
        map.put("font_size", getFont().getSize());
        map.put("font_weight",
                getFont().getAttributes().get(TextAttribute.WEIGHT));
        map.put("glyph_spacing",
                getFont().getAttributes().get(TextAttribute.TRACKING));
        map.put("inset", getInset());
        map.put("position", getPosition().toString());
        map.put("string", getString());
        map.put("stroke_color", getStrokeColor().toRGBAHex());
        map.put("stroke_width", getStrokeWidth());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     *         represent the instance, but not guaranteed to be in any
     *         particular format.
     */
    @Override
    public String toString() {
        String string;
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(getString().getBytes(Charset.forName("UTF8")));
            string = Hex.encodeHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.error("toString(): {}", e.getMessage());
            string = getString().replaceAll("[^A-Za-z0-9]", "");
        }
        // minSize is not included, as it is more of a potential property than
        // a property.
        return String.format("%s_%s_%d_%s_%d_%.1f_%.01f_%s_%s_%s_%.1f",
                string,
                getPosition(),
                getInset(),
                getFont().getName(),
                getFont().getSize(),
                getFont().getAttributes().get(TextAttribute.WEIGHT),
                getFont().getAttributes().get(TextAttribute.TRACKING),
                getColor().toRGBAHex(),
                getBackgroundColor().toRGBAHex(),
                getStrokeColor().toRGBAHex(),
                getStrokeWidth());
    }

}
