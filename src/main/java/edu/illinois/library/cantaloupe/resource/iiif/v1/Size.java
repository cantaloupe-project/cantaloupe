package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates the "size" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters-size">IIIF Image API
 * 1.1</a>
 */
class Size {

    public enum ScaleMode {

        /**
         * Represents a size argument in {@literal ,h} format.
         */
        ASPECT_FIT_HEIGHT,

        /**
         * Represents a size argument in {@literal w,} format.
         */
        ASPECT_FIT_WIDTH,

        /**
         * Represents a size argument in {@literal !w,h} format.
         */
        ASPECT_FIT_INSIDE,

        /**
         * Represents a size argument in {@literal w,h} format.
         */
        NON_ASPECT_FILL,

        /**
         * Represents a {@literal full} size argument.
         */
        FULL;

        public edu.illinois.library.cantaloupe.operation.Scale.Mode toMode() {
            return edu.illinois.library.cantaloupe.operation.Scale.Mode.
                    valueOf(this.toString());
        }

    }

    private Integer height;
    private ScaleMode scaleMode;
    private Float percent;
    private Integer width;

    /**
     * @param uriSize The {@literal size} component of a URI.
     * @return Size corresponding to the given URI size component.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Size fromUri(String uriSize) {
        Size size = new Size();
        try {
            if (uriSize.equals("full")) {
                size.setScaleMode(ScaleMode.FULL);
            } else {
                if (uriSize.endsWith(",")) {
                    size.setScaleMode(ScaleMode.ASPECT_FIT_WIDTH);
                    size.setWidth(Integer.parseInt(StringUtils.stripEnd(uriSize, ",")));
                } else if (uriSize.startsWith(",")) {
                    size.setScaleMode(ScaleMode.ASPECT_FIT_HEIGHT);
                    size.setHeight(Integer.parseInt(StringUtils.stripStart(uriSize, ",")));
                } else if (uriSize.startsWith("pct:")) {
                    size.setScaleMode(ScaleMode.ASPECT_FIT_INSIDE);
                    size.setPercent(Float.parseFloat(StringUtils.stripStart(uriSize, "pct:")));
                } else if (uriSize.startsWith("!")) {
                    size.setScaleMode(ScaleMode.ASPECT_FIT_INSIDE);
                    String[] parts = StringUtils.stripStart(uriSize, "!").split(",");
                    if (parts.length == 2) {
                        size.setWidth(Integer.parseInt(parts[0]));
                        size.setHeight(Integer.parseInt(parts[1]));
                    }
                } else {
                    size.setScaleMode(ScaleMode.NON_ASPECT_FILL);
                    String[] parts = uriSize.split(",");
                    if (parts.length == 2) {
                        size.setWidth(Integer.parseInt(parts[0]));
                        size.setHeight(Integer.parseInt(parts[1]));
                    } else {
                        throw new IllegalClientArgumentException("Invalid size");
                    }
                }
            }
        } catch (IllegalClientArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalClientArgumentException("Invalid size");
        }
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Size) {
            Size otherSize = (Size) obj;
            return toString().equals(otherSize.toString());
        }
        return false;
    }

    public Integer getHeight() {
        return height;
    }

    /**
     * @return Float in the range of 0-100.
     */
    public Float getPercent() {
        return percent;
    }

    public ScaleMode getScaleMode() {
        return scaleMode;
    }

    public Integer getWidth() {
        return width;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public void setHeight(Integer height) {
        if (height != null && height <= 0) {
            throw new IllegalClientArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * @param percent Float from 0-100
     */
    public void setPercent(Float percent) {
        if (percent != null && percent <= 0) {
            throw new IllegalClientArgumentException("Percent must be positive");
        }
        this.percent = percent;
    }

    public void setScaleMode(ScaleMode scaleMode) {
        this.scaleMode = scaleMode;
    }

    public void setWidth(Integer width) {
        if (width != null && width <= 0) {
            throw new IllegalClientArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    public Scale toScale() {
        Scale scale = new Scale();
        if (getHeight() != null) {
            scale.setHeight(this.getHeight());
        }
        if (getWidth() != null) {
            scale.setWidth(this.getWidth());
        }
        if (getPercent() != null) {
            scale.setPercent(getPercent() / 100.0);
        }
        if (getScaleMode() != null) {
            scale.setMode(getScaleMode().toMode());
        }
        return scale;
    }

    /**
     * @return Value compatible with the size component of a URI.
     */
    @Override
    public String toString() {
        String str = "";
        if (ScaleMode.FULL.equals(getScaleMode())) {
            str += "full";
        } else if (getPercent() != null) {
            str += "pct:" + StringUtil.removeTrailingZeroes(getPercent());
        } else {
            if (ScaleMode.ASPECT_FIT_INSIDE.equals(this.getScaleMode())) {
                str += "!";
            }
            if (this.getWidth() != null && this.getWidth() > 0) {
                str += this.getWidth();
            }
            str += ",";
            if (this.getHeight() != null && this.getHeight() > 0) {
                str += this.getHeight();
            }
        }
        return str;
    }

}
