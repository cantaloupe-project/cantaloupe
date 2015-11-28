package edu.illinois.library.cantaloupe.image;

public class Scale implements Operation {

    public enum Mode {
        ASPECT_FIT_HEIGHT, ASPECT_FIT_WIDTH, ASPECT_FIT_INSIDE,
        NON_ASPECT_FILL, FULL
    }

    private Integer height;
    private Mode scaleMode;
    private Float percent;
    private Integer width;

    public Integer getHeight() {
        return height;
    }

    public Mode getMode() {
        return scaleMode;
    }

    /**
     * @return Float from 0 to 1
     */
    public Float getPercent() {
        return percent;
    }

    public Integer getWidth() {
        return width;
    }

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
     * @param percent Float from 0 to 1
     * @throws IllegalArgumentException
     */
    public void setPercent(Float percent) throws IllegalArgumentException {
        if (percent <= 0 || percent > 1) {
            throw new IllegalArgumentException("Percent must be between 0-1");
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
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to be meaningful.
     */
    @Override
    public String toString() {
        String str = "";
        if (this.getMode() == Mode.FULL) {
            str += "full";
        } else if (this.getPercent() != null) {
            str += "pct:" + NumberUtil.removeTrailingZeroes(this.getPercent());
        } else {
            if (this.getMode() == Mode.ASPECT_FIT_INSIDE) {
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
