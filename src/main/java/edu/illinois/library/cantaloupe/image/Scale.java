package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates an absolute or relative scale operation.</p>
 *
 * <p>Absolute instances will have a non-null width and/or height. Relative
 * instances will have a non-null percent and a null width and height.</p>
 */
public class Scale implements Operation {

    public enum Mode {
        ASPECT_FIT_HEIGHT, ASPECT_FIT_WIDTH, ASPECT_FIT_INSIDE,
        NON_ASPECT_FILL, FULL
    }

    private Integer height;
    private Mode scaleMode = Mode.ASPECT_FIT_INSIDE;
    private Float percent;
    private Integer width;

    /**
     * @return Absolute pixel height. May be null.
     */
    public Integer getHeight() {
        return height;
    }

    public Mode getMode() {
        return scaleMode;
    }

    /**
     * @return Float from 0 to 1. May be null.
     */
    public Float getPercent() {
        return percent;
    }

    /**
     * @param fullSize
     * @return Resulting dimensions when the scale is applied to the given full
     * size.
     */
    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        Dimension size = new Dimension(fullSize.width, fullSize.height);
        if (this.getPercent() != null) {
            size.width *= this.getPercent();
            size.height *= this.getPercent();
        } else {
            switch (this.getMode()) {
                case ASPECT_FIT_HEIGHT:
                    if (this.getHeight() < size.height) {
                        double scalePct = this.getHeight() /
                                (double) size.height;
                        size.width *= scalePct;
                        size.height *= scalePct;
                    }
                    break;
                case ASPECT_FIT_WIDTH:
                    if (this.getWidth() < size.width) {
                        double scalePct = this.getWidth() /
                                (double) size.width;
                        size.width *= scalePct;
                        size.height *= scalePct;
                    }
                    break;
                case ASPECT_FIT_INSIDE:
                    if (this.getHeight() < size.height &&
                            this.getWidth() < size.width) {
                        double scalePct = Math.min(
                                this.getWidth() / (double) size.width,
                                this.getHeight() / (double) size.height);
                        size.width *= scalePct;
                        size.height *= scalePct;
                    }
                    break;
                case NON_ASPECT_FILL:
                    if (this.getWidth() < size.width) {
                        size.width = this.getWidth();
                    }
                    if (this.getHeight() < size.height) {
                        size.height = this.getHeight();
                    }
                    break;
            }
        }
        return size;
    }

    /**
     * @return Absolute pixel width. May be null.
     */
    public Integer getWidth() {
        return width;
    }

    @Override
    public boolean isNoOp() {
        return (this.getMode() == Mode.FULL) ||
                (this.getPercent() != null && Math.abs(this.getPercent() - 1f) < 0.000001f) ||
                (this.getPercent() == null && this.getHeight() == null && this.getWidth() == null);
    }

    public void setHeight(Integer height) throws IllegalArgumentException {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * @param percent Float greater than 0
     * @throws IllegalArgumentException
     */
    public void setPercent(Float percent) throws IllegalArgumentException {
        if (percent <= 0) {
            throw new IllegalArgumentException("Percent must be greater than zero");
        }
        this.percent = percent;
    }

    public void setMode(Mode scaleMode) {
        this.scaleMode = scaleMode;
    }

    public void setWidth(Integer width) throws IllegalArgumentException {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with <code>width</code> and <code>height</code> keys
     *         and integer values corresponding to the resulting pixel size of
     *         the operation.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
        final Dimension resultingSize = getResultingSize(fullSize);
        final Map<String,Object> map = new HashMap<>();
        map.put("operation", "scale");
        map.put("width", resultingSize.width);
        map.put("height", resultingSize.height);
        return map;
    }

    /**
     * @return String representation of the instance, guaranteed to uniquely
     * represent the instance.
     */
    @Override
    public String toString() {
        String str = "";
        if (this.isNoOp()) {
            str += "none";
        } else if (this.getPercent() != null) {
            str += NumberUtil.removeTrailingZeroes(this.getPercent() * 100) + "%";
        } else {
            if (this.getMode().equals(Mode.ASPECT_FIT_INSIDE)) {
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
