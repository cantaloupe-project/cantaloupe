package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.Scale;
import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates the "size" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters-size">IIIF Image API
 * 1.1</a>
 */
class Size {

    public enum ScaleMode {

        /** <code>,h</code> in an IIIF request URI */
        ASPECT_FIT_HEIGHT,

        /** <code>w,</code> in an IIIF request URI */
        ASPECT_FIT_WIDTH,

        /** <code>!w,h</code> in an IIIF request URI */
        ASPECT_FIT_INSIDE,

        /** <code>w,h</code> in an IIIF request URI */
        NON_ASPECT_FILL,

        /** <code>full</code> in an IIIF request URI */
        FULL;

        public edu.illinois.library.cantaloupe.image.Scale.Mode toMode() {
            return edu.illinois.library.cantaloupe.image.Scale.Mode.
                    valueOf(this.toString());
        }

    }

    private Integer height;
    private ScaleMode scaleMode;
    private Float percent;
    private Integer width;

    /**
     * @param uriSize The "size" component of an IIIF URI.
     * @return
     * @throws IllegalArgumentException
     */
    public static Size fromUri(String uriSize) throws IllegalArgumentException {
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
                    size.setPercent(Float.parseFloat(StringUtils.stripStart(uriSize, "pct:")));
                } else if (uriSize.startsWith("!")) {
                    size.setScaleMode(ScaleMode.ASPECT_FIT_INSIDE);
                    String[] parts = StringUtils.stripStart(uriSize, "!").split(",");
                    size.setWidth(Integer.parseInt(parts[0]));
                    size.setHeight(Integer.parseInt(parts[1]));
                } else {
                    size.setScaleMode(ScaleMode.NON_ASPECT_FILL);
                    String[] parts = uriSize.split(",");
                    if (parts.length == 2) {
                        size.setWidth(Integer.parseInt(parts[0]));
                        size.setHeight(Integer.parseInt(parts[1]));
                    } else {
                        throw new IllegalArgumentException("Invalid size");
                    }
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid size");
        }
        return size;
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

    public void setHeight(Integer height) throws IllegalArgumentException {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * @param percent Float from 0-100
     * @throws IllegalArgumentException
     */
    public void setPercent(Float percent) throws IllegalArgumentException {
        if (percent <= 0) {
            throw new IllegalArgumentException("Percent must be positive");
        }
        this.percent = percent;
    }

    public void setScaleMode(ScaleMode scaleMode) {
        this.scaleMode = scaleMode;
    }

    public void setWidth(Integer width) throws IllegalArgumentException {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    public Scale toScale() {
        Scale scale = new Scale();
        if (this.getHeight() != null) {
            scale.setHeight(this.getHeight());
        }
        if (this.getWidth() != null) {
            scale.setWidth(this.getWidth());
        }
        if (this.getPercent() != null) {
            scale.setPercent(this.getPercent() / 100f);
        }
        if (this.getScaleMode() != null) {
            scale.setMode(this.getScaleMode().toMode());
        }
        return scale;
    }

    /**
     * @return Value compatible with the size component of an IIIF URI.
     */
    public String toString() {
        String str = "";
        if (this.getScaleMode() == ScaleMode.FULL) {
            str += "full";
        } else if (this.getPercent() != null) {
            str += "pct:" + NumberUtil.formatForUrl(this.getPercent());
        } else {
            if (this.getScaleMode() == ScaleMode.ASPECT_FIT_INSIDE) {
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
