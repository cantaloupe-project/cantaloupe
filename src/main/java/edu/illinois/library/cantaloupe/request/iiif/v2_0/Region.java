package edu.illinois.library.cantaloupe.request.iiif.v2_0;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.util.NumberUtil;

/**
 * Encapsulates the "region" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#region">IIIF Image API 2.0</a>
 */
public class Region {

    private Float height;
    private boolean isFull = false;
    private boolean isPercent = false;
    private Float width;
    private Float x;
    private Float y;

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

    public Crop toCrop() {
        Crop crop = new Crop();
        crop.setFull(this.isFull());
        if (this.getHeight() != null) {
            crop.setHeight(this.getHeight());
        }
        if (this.getWidth() != null) {
            crop.setWidth(this.getWidth());
        }
        crop.setPercent(this.isPercent());
        if (this.getX() != null) {
            crop.setX(this.getX());
        }
        if (this.getY() != null) {
            crop.setY(this.getY());
        }
        return crop;
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
