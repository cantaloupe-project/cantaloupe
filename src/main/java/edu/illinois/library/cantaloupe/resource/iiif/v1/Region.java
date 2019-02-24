package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtils;

/**
 * Encapsulates the "region" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters-region">IIIF Image
 * API 1.1</a>
 */
final class Region {

    private static final double DELTA = 0.00000001;

    private float x, y, width, height;
    private boolean isFull, isPercent;

    /**
     * @param uriRegion Region component of a URI.
     * @return Region corresponding to the argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Region fromUri(String uriRegion) {
        final Region region = new Region();
        if ("full".equals(uriRegion)) {
            region.setFull(true);
        } else {
            region.setFull(false);
            String csv;
            if (uriRegion.startsWith("pct:")) {
                region.setPercent(true);
                csv = uriRegion.substring(4);
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
                throw new IllegalClientArgumentException("Invalid region");
            }
        }
        return region;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Region) {
            final Region region = (Region) obj;
            if (isFull() && region.isFull()) {
                return true;
            }
            return isPercent() == region.isPercent() &&
                    Math.abs(getX() - region.getX()) < DELTA &&
                    Math.abs(getY() - region.getY()) < DELTA &&
                    Math.abs(getWidth() - region.getWidth()) < DELTA &&
                    Math.abs(getHeight() - region.getHeight()) < DELTA;
        }
        return super.equals(obj);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
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

    public void setPercent(boolean isPercent) {
        this.isPercent = isPercent;
    }

    public void setX(float x) {
        if (x < 0) {
            throw new IllegalClientArgumentException("X must be >= 0");
        }
        this.x = x;
    }

    public void setY(float y) {
        if (y < 0) {
            throw new IllegalClientArgumentException("Y must be >= 0");
        }
        this.y = y;
    }

    public void setWidth(float width) {
        if (width <= 0) {
            throw new IllegalClientArgumentException("Width must be > 0");
        }
        this.width = width;
    }

    public void setHeight(float height) {
        if (height <= 0) {
            throw new IllegalClientArgumentException("Height must be > 0");
        }
        this.height = height;
    }

    /**
     * @return Equivalent instance, or {@literal null} if {@link #isFull()}
     *         returns {@literal true}.
     */
    Crop toCrop() {
        if (isFull()) {
            return new CropByPercent(); // 100% crop
        } else if (isPercent()) {
            return new CropByPercent(
                    getX() / 100.0, getY() / 100.0,
                    getWidth() / 100.0, getHeight() / 100.0);
        } else {
            return new CropByPixels(
                    Math.round(getX()), Math.round(getY()),
                    Math.round(getWidth()), Math.round(getHeight()));
        }
    }

    /**
     * @return Value compatible with the region component of a URI.
     */
    public String toString() {
        String str;
        if (isFull()) {
            str = "full";
        } else {
            String x, y;
            if (isPercent()) {
                str = "pct:";
                x = StringUtils.removeTrailingZeroes(this.getX());
                y = StringUtils.removeTrailingZeroes(this.getY());
            } else {
                str = "";
                x = Integer.toString(Math.round(this.getX()));
                y = Integer.toString(Math.round(this.getY()));
            }
            str += String.format("%s,%s,%s,%s", x, y,
                    StringUtils.removeTrailingZeroes(this.getWidth()),
                    StringUtils.removeTrailingZeroes(this.getHeight()));
        }
        return str;
    }

}
