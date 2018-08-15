package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Encapsulates an image encoding operation.</p>
 *
 * <p>This is typically the last operation before delivery.</p>
 */
public class Encode implements Operation {

    static final short MAX_QUALITY = 100;

    private Color backgroundColor;
    private Compression compression = Compression.UNDEFINED;
    private Format format = Format.UNKNOWN;
    private boolean interlace;
    private boolean isFrozen;
    private int maxComponentSize = 8;
    private int quality = MAX_QUALITY;

    public Encode(Format format) {
        setFormat(format);
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     * @return Color with which to fill the empty portions of the image when
     *         {@link #getFormat()} returns a format that does not
     *         support transparency and when either rotating by a non-90-degree
     *         multiple, or when flattening an image with alpha. May be
     *         <code>null</code>.
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @return Compression type. This only applies to certain output formats.
     *         May be <code>null</code>.
     */
    public Compression getCompression() {
        return compression;
    }

    public Format getFormat() {
        return format;
    }

    /**
     * @return Maximum sample size to encode. May be {@link Integer#MAX_VALUE}
     *         indicating no max.
     */
    public int getMaxComponentSize() {
        return maxComponentSize;
    }

    /**
     * @return Output quality in the range of 1-{@link #MAX_QUALITY}.
     *         This only applies to certain output formats, and perhaps also
     *         only with certain compressions.
     */
    public int getQuality() {
        return quality;
    }

    /**
     * @return {@literal true}.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    /**
     *
     * @param fullSize Full size of the source image.
     * @param opList   Operation list of which the operation may or may not be
     *                 a member.
     * @return         {@literal true}.
     */
    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        return hasEffect();
    }

    public boolean isInterlacing() {
        return interlace;
    }

    /**
     * @param color Background color.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setBackgroundColor(Color color) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.backgroundColor = color;
    }

    /**
     * @param compression Compression to set.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setCompression(Compression compression) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.compression = compression;
    }

    /**
     * @param format Format to set.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setFormat(Format format) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        if (format == null) {
            format = Format.UNKNOWN;
        }
        this.format = format;
    }

    /**
     * @param interlace Interlacing to set.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setInterlacing(boolean interlace) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.interlace = interlace;
    }

    /**
     * @param depth Maximum sample size to encode. Supply {@literal 0} to
     *              indicate no max.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setMaxComponentSize(int depth) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        this.maxComponentSize = (depth == 0) ? Integer.MAX_VALUE : depth;
    }

    /**
     * @param quality
     * @throws IllegalArgumentException if the given quality is outside the
     *         range of 1-{@link #MAX_QUALITY}.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setQuality(int quality) {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
        if (quality < 1 || quality > MAX_QUALITY) {
            throw new IllegalArgumentException(
                    "Quality must be in the range of 1-" + MAX_QUALITY + ".");
        }
        this.quality = quality;
    }

    /**
     * <p>Returns a map in the following format:</p>
     *
     * <pre>{
     *     class: "Encode",
     *     background_color: Hexadecimal string,
     *     compression: String,
     *     format: Media type string,
     *     interlace: Boolean,
     *     quality: Integer,
     *     max_sample_size: Integer
     * }</pre>
     *
     * @return Map representation of the instance.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        if (getBackgroundColor() != null) {
            map.put("background_color", getBackgroundColor().toRGBHex());
        }
        map.put("compression", getCompression().toString());
        map.put("format", getFormat().getPreferredMediaType());
        map.put("interlace", isInterlacing());
        map.put("quality", getQuality());
        map.put("max_sample_size", getMaxComponentSize());
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     *         represent it.
     */
    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        if (getFormat() != null) {
            parts.add(getFormat().getPreferredExtension());
        }
        if (getCompression() != null) {
            parts.add(getCompression().toString());
        }
        if (getQuality() < MAX_QUALITY) {
            parts.add("" + getQuality());
        }
        if (isInterlacing()) {
            parts.add("interlace");
        }
        if (getBackgroundColor() != null) {
            parts.add(getBackgroundColor().toRGBHex());
        }
        if (getMaxComponentSize() != Integer.MAX_VALUE) {
            parts.add(getMaxComponentSize() + "");
        }
        return String.join("_", parts);
    }

}
