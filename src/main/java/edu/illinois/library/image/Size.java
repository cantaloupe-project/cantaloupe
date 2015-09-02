package edu.illinois.library.image;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by alexd on 9/2/15.
 */
public class Size {

    private boolean fitHeight = false;
    private boolean fitWidth = false;
    private Integer height;
    private boolean isFull = false;
    private boolean scaleToFit = false;
    private Float scaleToPercent;
    private Integer width;

    /**
     * @param uriSize The "size" component of an IIIF URI.
     * @return
     * @throws IllegalArgumentException
     */
    public static Size fromUri(String uriSize) throws IllegalArgumentException {
        Size size = new Size();
        if (uriSize.equals("full")) {
            size.setFull(true);
        } else {
            size.setFull(false);
            if (uriSize.endsWith(",")) {
                size.setScaleToFit(true);
                size.setWidth(Integer.parseInt(StringUtils.stripEnd(uriSize, ",")));
            } else if (uriSize.startsWith(",")) {
                size.setScaleToFit(true);
                size.setHeight(Integer.parseInt(StringUtils.stripStart(uriSize, ",")));
            } else if (uriSize.startsWith("pct:")) {
                size.setScaleToPercent(Float.parseFloat(StringUtils.stripStart(uriSize, "pct:")));
            } else if (uriSize.startsWith("!")) {
                String[] parts = StringUtils.stripStart(uriSize, "!").split(",");
                size.setWidth(Integer.parseInt(parts[0]));
                size.setHeight(Integer.parseInt(parts[1]));
                size.setScaleToFit(true);
            } else {
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

    public Float getScaleToPercent() {
        return scaleToPercent;
    }

    public Integer getWidth() {
        return width;
    }

    public boolean isFitHeight() {
        return fitHeight;
    }

    public boolean isFitWidth() {
        return fitWidth;
    }

    public boolean isFull() {
        return isFull;
    }

    public boolean isScaleToFit() {
        return scaleToFit;
    }

    public void setFitHeight(boolean fitHeight) {
        this.fitHeight = fitHeight;
    }

    public void setFitWidth(boolean fitWidth) {
        this.fitWidth = fitWidth;
    }

    public void setFull(boolean isFull) {
        this.isFull = isFull;
    }

    public void setHeight(Integer height) throws IllegalArgumentException {
        if (height == 0) {
            throw new IllegalArgumentException("Height must be nonzero");
        }
        this.height = height;
    }

    public void setScaleToFit(boolean scaleToFit) {
        this.scaleToFit = scaleToFit;
    }

    public void setScaleToPercent(Float scaleToPercent) {
        this.scaleToPercent = scaleToPercent;
    }

    public void setWidth(Integer width) throws IllegalArgumentException {
        if (width == 0) {
            throw new IllegalArgumentException("Width must be nonzero");
        }
        this.width = width;
    }

}
