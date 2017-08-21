package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dimension;
import java.util.ArrayList;
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
    private boolean interlace = false;
    /** May be null to indicate no max. */
    private Integer maxSampleSize;
    private int quality = MAX_QUALITY;

    public Encode(Format format) {
        setFormat(format);
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
     * @return Maximum sample size to encode. May be <code>null</code> to
     *         indicate no max.
     */
    public Integer getMaxSampleSize() {
        return maxSampleSize;
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
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return The input size.
     */
    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    /**
     * @return <code>true</code>.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    /**
     *
     * @param fullSize Full size of the source image.
     * @param opList Operation list of which the operation may or may not be a
     *               member.
     * @return <code>true</code>.
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
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    public void setCompression(Compression compression) {
        this.compression = compression;
    }

    public void setFormat(Format format) {
        if (format == null) {
            format = Format.UNKNOWN;
        }
        this.format = format;
    }

    public void setInterlacing(boolean interlace) {
        this.interlace = interlace;
    }

    /**
     * @param depth Maximum sample size to encode. Supply <code>null</code> to
     *              indicate no max.
     */
    public void setMaxSampleSize(Integer depth) {
        this.maxSampleSize = depth;
    }

    /**
     * @param quality
     * @throws IllegalArgumentException If the given quality is outside the
     *         range of 1-{@link #MAX_QUALITY}.
     */
    public void setQuality(int quality) {
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
     * @param fullSize Ignored.
     * @return Map representation of the instance.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        if (getBackgroundColor() != null) {
            map.put("background_color", getBackgroundColor().toRGBHex());
        }
        map.put("compression", getCompression().toString());
        map.put("format", getFormat().getPreferredMediaType());
        map.put("interlace", isInterlacing());
        map.put("quality", getQuality());
        map.put("max_sample_size", getMaxSampleSize());
        return map;
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
        parts.add(getMaxSampleSize() + "");

        return StringUtils.join(parts, "_");
    }

}
