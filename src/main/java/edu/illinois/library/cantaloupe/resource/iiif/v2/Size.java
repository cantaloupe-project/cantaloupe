package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtils;

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
         * Represents a {@literal full} size argument.
         */
        FULL,

        /**
         * Represents a {@literal max} size argument.
         */
        MAX,

        /**
         * Represents a size argument in {@literal w,h} format.
         */
        NON_ASPECT_FILL

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
            if (uriSize.equals("full")) {
                size.setScaleMode(ScaleMode.FULL);
            } else if (uriSize.equals("max")) {
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

    ScaleMode getScaleMode() {
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

    void setScaleMode(ScaleMode scaleMode) {
        this.scaleMode = scaleMode;
    }

    public void setWidth(Integer width) {
        if (width != null && width <= 0) {
            throw new IllegalClientArgumentException(
                    "Width must be a positive integer");
        }
        this.width = width;
    }

    Scale toScale() {
        if (getPercent() != null) {
            return new ScaleByPercent(getPercent() / 100.0);
        }
        switch (getScaleMode()) {
            case FULL:
                return new ScaleByPercent();
            case MAX:
                return new ScaleByPercent();
            case ASPECT_FIT_WIDTH:
                return new ScaleByPixels(
                        getWidth(), null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
            case ASPECT_FIT_HEIGHT:
                return new ScaleByPixels(
                        null, getHeight(), ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
            case ASPECT_FIT_INSIDE:
                return new ScaleByPixels(
                        getWidth(), getHeight(), ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
            case NON_ASPECT_FILL:
                return new ScaleByPixels(
                        getWidth(), getHeight(), ScaleByPixels.Mode.NON_ASPECT_FILL);
            default:
                throw new IllegalArgumentException(
                        "Unknown scale mode. This is probably a bug.");
        }
    }

    /**
     * @return Value compatible with the size component of a URI.
     * @see    #toCanonicalString(Dimension)
     */
    @Override
    public String toString() {
        String str = "";
        if (ScaleMode.FULL.equals(getScaleMode())) {
            str += "full";
        } else if (ScaleMode.MAX.equals(getScaleMode())) {
            str += "max";
        } else if (getPercent() != null) {
            str += "pct:" + StringUtils.removeTrailingZeroes(getPercent());
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

    /**
     * @param fullSize Full source image dimensions.
     * @return         Canonical value compatible with the size component of a
     *                 URI.
     * @see            #toString()
     */
    String toCanonicalString(Dimension fullSize) {
        if (ScaleMode.FULL.equals(getScaleMode()) ||
                ScaleMode.MAX.equals(getScaleMode())) {
            return toString();
        } else if (ScaleMode.NON_ASPECT_FILL.equals(getScaleMode())) { // w,h syntax
            return getWidth() + "," + getHeight();
        } else { // w, syntax
            long width;
            if (getPercent() != null) {
                width = Math.round(fullSize.width() * getPercent() / 100.0);
            } else {
                if (getWidth() == null || getWidth() == 0) {
                    double scale = getHeight() / fullSize.height();
                    width = Math.round(fullSize.width() * scale);
                } else {
                    width = getWidth();
                }
            }
            return width + ",";
        }
    }

}
