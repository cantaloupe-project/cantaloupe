package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.Operation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates a string overlay applied to an image.</p>
 *
 * <p>Instances should be obtained from the
 * {@link OverlayService}.</p>
 */
public class StringOverlay extends Overlay implements Operation {

    private Color color;
    private Font font;
    private String string;
    private Color strokeColor;
    private float strokeWidth;

    public StringOverlay(String string, Position position, int inset,
                         Font font, Color color, Color strokeColor,
                         float strokeWidth) {
        super(position, inset);
        this.setString(string);
        this.setFont(font);
        this.setColor(color);
        this.setStrokeColor(strokeColor);
        this.setStrokeWidth(strokeWidth);
    }

    public Color getColor() {
        return color;
    }

    public Font getFont() {
        return font;
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

    public void setColor(Color color) {
        this.color = color;
    }

    public void setFont(Font font) {
        this.font = font;
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
     * @return Map with <var>string</var>, <var>font</var>,
     *         <var>font_size</var>, <var>color</var>, <var>position</var>,
     *         and <var>inset</var> keys.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("string", getString());
        map.put("position", getPosition().toString());
        map.put("inset", getInset());
        map.put("font", getFont().getFamily());
        map.put("font_size", getFont().getSize());
        map.put("color", ColorUtil.getHex(getColor()));
        map.put("stroke_color", ColorUtil.getHex(getStrokeColor()));
        map.put("stroke_width", getStrokeWidth());
        return map;
    }

    /**
     * @return String representation of the instance, in the format
     * "{alphanumeric string}_{position}_{inset}_{color}".
     */
    @Override
    public String toString() {
        return String.format("%s_%s_%d_%s_%d_%s_%s_%.1f",
                getString().replaceAll("[^A-Za-z0-9]", ""),
                getPosition(),
                getInset(),
                getFont().getFamily(),
                getFont().getSize(),
                ColorUtil.getHex(getColor()),
                ColorUtil.getHex(getStrokeColor()),
                getStrokeWidth());
    }

}
