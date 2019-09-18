package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;

/**
 * <p>Encapsulates a string overlay applied to an image.</p>
 *
 * <p>Instances should be obtained from the
 * {@link OverlayService}.</p>
 */
public class StringOverlay extends Overlay implements Operation {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringOverlay.class);

    private Color backgroundColor;
    private Color color;
    private Font font;
    private int minSize;
    private String string;
    private Color strokeColor;
    private float strokeWidth;

    public StringOverlay(String string,
                         Position position,
                         int inset,
                         Font font,
                         int minSize,
                         Color color,
                         Color backgroundColor,
                         Color strokeColor,
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

    /**
     * @param color Background color to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setBackgroundColor(Color color) {
        checkFrozen();
        this.backgroundColor = color;
    }

    /**
     * @param color Color to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setColor(Color color) {
        checkFrozen();
        this.color = color;
    }

    /**
     * @param font Font to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setFont(Font font) {
        checkFrozen();
        this.font = font;
    }

    /**
     * @param minSize Minimum size in pixels
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setMinSize(int minSize) {
        checkFrozen();
        this.minSize = minSize;
    }

    /**
     * @param string String to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setString(String string) {
        checkFrozen();
        this.string = string.replace("\\n", "\n");
    }

    /**
     * @param color Color to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setStrokeColor(Color color) {
        checkFrozen();
        this.strokeColor = color;
    }

    /**
     * @param width Width in pixels.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setStrokeWidth(float width) {
        checkFrozen();
        this.strokeWidth = width;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with {@literal background_color}, {@literal class},
     *         {@literal color}, {@literal font}, {@literal font_size},
     *         {@literal font_weight}, {@literal glyph_spacing},
     *         {@literal inset}, {@literal position}, {@literal string},
     *         {@literal stroke_color}, and {@literal stroke_width} keys.
     */
    @Override
    public Map<String, Object> toMap(Dimension fullSize,
                                     ScaleConstraint scaleConstraint) {
        Float fontWeight = TextAttribute.WEIGHT_REGULAR;
        if (getFont().getAttributes().get(TextAttribute.WEIGHT) != null) {
            fontWeight = (Float)getFont().getAttributes().get(TextAttribute.WEIGHT);
        }
        Float tracking = 0.0f;
        if (getFont().getAttributes().get(TextAttribute.TRACKING) != null) {
            tracking = (Float)getFont().getAttributes().get(TextAttribute.TRACKING);
        }
        return Map.ofEntries(
                Map.entry("background_color", getBackgroundColor().toRGBAHex()),
                Map.entry("class", getClass().getSimpleName()),
                Map.entry("color", getColor().toRGBAHex()),
                Map.entry("font", getFont().getName()),
                Map.entry("font_size", getFont().getSize()),
                Map.entry("font_weight", fontWeight),
                Map.entry("glyph_spacing", tracking),
                Map.entry("inset", getInset()),
                Map.entry("position", getPosition().toString()),
                Map.entry("string", getString()),
                Map.entry("stroke_color", getStrokeColor().toRGBAHex()),
                Map.entry("stroke_width", getStrokeWidth()));
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     *         represent it, but not guaranteed to be in any particular format.
     */
    @Override
    public String toString() {
        // minSize is not included, as it is more of a potential property than
        // a property.
        return String.format("%s_%s_%d_%s_%d_%.1f_%.01f_%s_%s_%s_%.1f",
                StringUtils.md5(getString()),
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
