package edu.illinois.library.cantaloupe.request.iiif.v2_0;

import edu.illinois.library.cantaloupe.util.NumberUtil;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Encapsulates the "region" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#region">IIIF Image API 2.0</a>
 */
public class Region {

    private Float height = 0.0f;
    private boolean isFull = false;
    private boolean isPercent = false;
    private Float width = 0.0f;
    private Float x = 0.0f;
    private Float y = 0.0f;

    /**
     * @param uriRegion The "region" component of an IIIF URI.
     * @return
     * @throws IllegalArgumentException
     */
    public static Region fromUri(String uriRegion)
            throws IllegalArgumentException {
        Region region = new Region();

        if (uriRegion.equals("full")) {
            region.setFull(true);
        } else {
            region.setFull(false);
            String csv;
            if (uriRegion.startsWith("pct:")) {
                region.setPercent(true);
                String[] tmp = uriRegion.split(":");
                csv = tmp[1];
            } else {
                region.setPercent(false);
                csv = uriRegion;
            }
            String[] parts = csv.split(",");
            if (parts.length == 4) {
                region.setX(Float.parseFloat(parts[0]));
                region.setY(Float.parseFloat(parts[1]));
                region.setWidth(Float.parseFloat(parts[2]));
                region.setHeight(Float.parseFloat(parts[3]));
            } else {
                throw new IllegalArgumentException("Invalid region");
            }
        }
        return region;
    }

    public Float getHeight() {
        return height;
    }

    /**
     * @param fullSize Full-sized image dimensions.
     * @return Region coordinates relative to the given full-sized image
     * dimensions.
     */
    public Rectangle getRectangle(Dimension fullSize) {
        int x, y, width, height;
        if (this.isFull()) {
            x = 0;
            y = 0;
            width = fullSize.width;
            height = fullSize.height;
        } else if (this.isPercent()) {
            x = Math.round((this.getX() / 100f) * fullSize.width);
            y = Math.round((this.getY() / 100f) * fullSize.height);
            width = Math.round((this.getWidth() / 100f) * fullSize.width);
            height = Math.round((this.getHeight() / 100f) * fullSize.height);
        } else {
            x = Math.round(this.getX());
            y = Math.round(this.getY());
            width = Math.round(this.getWidth());
            height = Math.round(this.getHeight());
        }
        return new Rectangle(x, y, width, height);
    }

    public Float getWidth() {
        return width;
    }

    public Float getX() {
        return x;
    }

    public Float getY() {
        return y;
    }

    public boolean isFull() {
        return this.isFull;
    }

    public boolean isPercent() {
        return this.isPercent;
    }

    public void setFull(boolean isFull) {
        this.isFull = isFull;
    }

    public void setHeight(Float height) throws IllegalArgumentException {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    public void setPercent(boolean isPercent) {
        this.isPercent = isPercent;
    }

    public void setWidth(Float width) throws IllegalArgumentException {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    public void setX(Float x) throws IllegalArgumentException {
        if (x < 0) {
            throw new IllegalArgumentException("X must be a positive float");
        }
        this.x = x;
    }

    public void setY(Float y) throws IllegalArgumentException {
        if (y < 0) {
            throw new IllegalArgumentException("Y must be a positive float");
        }
        this.y = y;
    }

    public edu.illinois.library.cantaloupe.image.Region toRegion() {
        edu.illinois.library.cantaloupe.image.Region region =
                new edu.illinois.library.cantaloupe.image.Region();
        region.setFull(this.isFull());
        region.setHeight(this.getHeight());
        region.setWidth(this.getWidth());
        region.setPercent(this.isPercent());
        region.setX(this.getX());
        region.setY(this.getY());
        return region;
    }

    /**
     * @return Value compatible with the region component of an IIIF URI.
     */
    public String toString() {
        String str = "";
        if (this.isFull()) {
            str += "full";
        } else {
            String x, y;
            if (this.isPercent()) {
                x = NumberUtil.removeTrailingZeroes(this.getX());
                y = NumberUtil.removeTrailingZeroes(this.getY());
                str += "pct:";
            } else {
                x = Integer.toString(Math.round(this.getX()));
                y = Integer.toString(Math.round(this.getY()));
            }
            str += String.format("%s,%s,%s,%s", x, y,
                    NumberUtil.removeTrailingZeroes(this.getWidth()),
                    NumberUtil.removeTrailingZeroes(this.getHeight()));
        }
        return str;
    }

}
