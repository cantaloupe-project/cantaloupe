package edu.illinois.library.cantaloupe.image;

public class Scale {

    public enum Mode {

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
    private Mode scaleMode;
    private Float percent;
    private Integer width;

    public Integer getHeight() {
        return height;
    }

    /**
     * @return Float from 0-1
     */
    public Float getPercent() {
        return percent;
    }

    public Mode getScaleMode() {
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
     * @param percent Float from 0-1
     * @throws IllegalArgumentException
     */
    public void setPercent(Float percent) throws IllegalArgumentException {
        if (percent <= 0 || percent > 1) {
            throw new IllegalArgumentException("Percent must be between 0-1");
        }
        this.percent = percent;
    }

    public void setScaleMode(Mode scaleMode) {
        this.scaleMode = scaleMode;
    }

    public void setWidth(Integer width) throws IllegalArgumentException {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

}
