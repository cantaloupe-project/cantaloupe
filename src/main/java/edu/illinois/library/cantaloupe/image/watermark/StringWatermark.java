package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.image.Operation;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates a string watermark applied to an image.</p>
 *
 * <p>Instances should be obtained from the
 * {@link WatermarkService}.</p>
 */
public class StringWatermark extends Watermark implements Operation {

    private Color color;
    private String string;

    /**
     * No-op constructor.
     */
    public StringWatermark() {}

    public StringWatermark(String string, Position position, int inset,
                           Color color) {
        super(position, inset);
        this.setString(string);
        this.setColor(color);
    }

    public Color getColor() {
        return color;
    }

    public String getString() {
        return string;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setString(String string) {
        this.string = string;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with <code>string</code>, <code>color</code>,
     *         <code>position</code>, and <code>inset</code> keys.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("string", getString());
        map.put("position", getPosition().toString());
        map.put("inset", getInset());
        map.put("color", ColorUtil.getHex(getColor()));
        return map;
    }

    /**
     * @return String representation of the instance, in the format
     * "{alphanumeric string}_{position}_{inset}_{color}".
     */
    @Override
    public String toString() {
        return String.format("%s_%s_%d_%s",
                getString().replaceAll("[^A-Za-z0-9]", ""),
                getPosition(), getInset(), ColorUtil.getHex(getColor()));
    }

}
