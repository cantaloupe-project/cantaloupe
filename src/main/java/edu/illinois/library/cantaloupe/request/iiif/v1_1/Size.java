package edu.illinois.library.cantaloupe.request.iiif.v1_1;

import edu.illinois.library.cantaloupe.util.NumberUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates the "size" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#size">IIIF Image API 2.0</a>
 */
public class Size {

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
        FULL
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
                size.setWidth(Integer.parseInt(parts[0]));
                size.setHeight(Integer.parseInt(parts[1]));
            }
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

    /**
     * @return Value compatible with the size component of an IIIF URI.
     */
    public String toString() {
        String str = "";
        if (this.getScaleMode() == ScaleMode.FULL) {
            str += "full";
        } else if (this.getPercent() != null) {
            str += "pct:" + NumberUtil.removeTrailingZeroes(this.getPercent());
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
