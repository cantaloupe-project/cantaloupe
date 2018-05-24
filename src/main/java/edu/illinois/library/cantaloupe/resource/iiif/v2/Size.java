package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates the "size" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#size">IIIF Image API 2.0</a>
 * @see <a href="http://iiif.io/api/image/2.1/#size">IIIF Image API 2.1</a>
 */
class Size {

    enum ScaleMode {

        /**
         * Represents a size argument in {@literal ,h} format.
         */
        ASPECT_FIT_HEIGHT(Scale.Mode.ASPECT_FIT_HEIGHT),

        /**
         * Represents a size argument in {@literal w,} format.
         */
        ASPECT_FIT_WIDTH(Scale.Mode.ASPECT_FIT_WIDTH),

        /**
         * Represents a size argument in {@literal !w,h} format.
         */
        ASPECT_FIT_INSIDE(Scale.Mode.ASPECT_FIT_INSIDE),

        /**
         * Represents a {@literal full} (Image API 2.0 & 2.1) or {@literal max}
         * (Image API 2.1) size argument.
         */
        MAX(Scale.Mode.FULL),

        /**
         * Represents a size argument in {@literal w,h} format.
         */
        NON_ASPECT_FILL(Scale.Mode.NON_ASPECT_FILL);

        private Scale.Mode equivalentScaleMode;

        ScaleMode(Scale.Mode equivalentScaleMode) {
            this.equivalentScaleMode = equivalentScaleMode;
        }

        public edu.illinois.library.cantaloupe.operation.Scale.Mode toMode() {
            return this.equivalentScaleMode;
        }

    }

    private Integer height;
    private ScaleMode scaleMode;
    private Float percent;
    private Integer width;

    /**
     * @param uriSize The {@literal size} component of a URI.
     * @return        Size corresponding to the argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Size fromUri(String uriSize) {
        Size size = new Size();
        try {
            if (uriSize.equals("max") || uriSize.equals("full")) {
                size.setScaleMode(ScaleMode.MAX);
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
     * @return Float from 0-100
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
            throw new IllegalClientArgumentException(
                    "Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * @param percent Float from 0-100
     * @throws IllegalClientArgumentException
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
            throw new IllegalClientArgumentException(
                    "Width must be a positive integer");
        }
        this.width = width;
    }

    public Scale toScale() {
        Scale scale = new Scale();
        if (getHeight() != null) {
            scale.setHeight(getHeight());
        }
        if (getWidth() != null) {
            scale.setWidth(getWidth());
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
    public String toString() {
        String str = "";
        if (ScaleMode.MAX.equals(getScaleMode())) {
            // Use "full" because "max" is not available in Image API 2.0.
            str += "full";
        } else if (getPercent() != null) {
            str += "pct:" + StringUtil.removeTrailingZeroes(getPercent());
        } else {
            if (ScaleMode.ASPECT_FIT_INSIDE.equals(getScaleMode())) {
                str += "!";
            }
            if (getWidth() != null && getWidth() > 0) {
                str += getWidth();
            }
            str += ",";
            if (getHeight() != null && getHeight() > 0) {
                str += getHeight();
            }
        }
        return str;
    }

}
