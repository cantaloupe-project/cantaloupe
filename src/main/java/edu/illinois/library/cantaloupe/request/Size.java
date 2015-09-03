package edu.illinois.library.cantaloupe.request;

import org.apache.commons.lang3.StringUtils;

public class Size {

    public enum ScaleMode {
        FILL_HEIGHT, FILL_WIDTH, ASPECT_FIT_INSIDE, NON_ASPECT_FIT_INSIDE, FULL
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
                size.setScaleMode(ScaleMode.FILL_WIDTH);
                size.setWidth(Integer.parseInt(StringUtils.stripEnd(uriSize, ",")));
            } else if (uriSize.startsWith(",")) {
                size.setScaleMode(ScaleMode.FILL_HEIGHT);
                size.setHeight(Integer.parseInt(StringUtils.stripStart(uriSize, ",")));
            } else if (uriSize.startsWith("pct:")) {
                size.setPercent(Float.parseFloat(StringUtils.stripStart(uriSize, "pct:")));
            } else if (uriSize.startsWith("!")) {
                size.setScaleMode(ScaleMode.ASPECT_FIT_INSIDE);
                String[] parts = StringUtils.stripStart(uriSize, "!").split(",");
                size.setWidth(Integer.parseInt(parts[0]));
                size.setHeight(Integer.parseInt(parts[1]));
            } else {
                size.setScaleMode(ScaleMode.NON_ASPECT_FIT_INSIDE);
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

}
