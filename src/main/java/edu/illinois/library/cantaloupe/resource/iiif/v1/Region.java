package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtil;

/**
 * Encapsulates the "region" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters-region">IIIF Image
 * API 1.1</a>
 */
class Region {

    private Float height;
    private boolean isFull = false;
    private boolean isPercent = false;
    private Float width;
    private Float x;
    private Float y;

    /**
     * @param uriRegion Region component of a URI.
     * @return Region corresponding to the argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Region fromUri(String uriRegion) {
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
                throw new IllegalClientArgumentException("Invalid region");
            }
        }
        return region;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        final float delta = 0.0001f;
        if (obj instanceof Region) {
            final Region region = (Region) obj;
            return isFull() == region.isFull() &&
                    isPercent() == region.isPercent() &&
                    Math.abs(getX() - region.getX()) < delta &&
                    Math.abs(getY() - region.getY()) < delta &&
                    Math.abs(getWidth() - region.getWidth()) < delta &&
                    Math.abs(getHeight() - region.getHeight()) < delta;
        }
        if (obj instanceof Crop) {
            final Crop crop = (Crop) obj;
            if (this.isPercent()) {
                return isFull() == crop.isFull() &&
                        isPercent() == crop.getUnit().equals(Crop.Unit.PERCENT) &&
                        Math.abs(getX() - crop.getX() * 100) < delta &&
                        Math.abs(getY() - crop.getY() * 100) < delta &&
                        Math.abs(getWidth() - crop.getWidth() * 100) < delta &&
                        Math.abs(getHeight() - crop.getHeight() * 100) < delta;
            }
            return isFull() == crop.isFull() &&
                    isPercent() == crop.getUnit().equals(Crop.Unit.PERCENT) &&
                    Math.abs(getX() - crop.getX()) < delta &&
                    Math.abs(getY() - crop.getY()) < delta &&
                    Math.abs(getWidth() - crop.getWidth()) < delta &&
                    Math.abs(getHeight() - crop.getHeight()) < delta;
        }
        return super.equals(obj);
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

    public void setHeight(Float height) {
        if (height <= 0) {
            throw new IllegalClientArgumentException(
                    "Height must be a positive integer");
        }
        this.height = height;
    }

    public void setPercent(boolean isPercent) {
        this.isPercent = isPercent;
    }

    public void setWidth(Float width) {
        if (width <= 0) {
            throw new IllegalClientArgumentException(
                    "Width must be a positive integer");
        }
        this.width = width;
    }

    public void setX(Float x) {
        if (x < 0) {
            throw new IllegalClientArgumentException("X must be a positive float");
        }
        this.x = x;
    }

    public void setY(Float y) {
        if (y < 0) {
            throw new IllegalClientArgumentException("Y must be a positive float");
        }
        this.y = y;
    }

    public Crop toCrop() {
        Crop crop = new Crop();
        crop.setFull(this.isFull());
        crop.setUnit(this.isPercent() ? Crop.Unit.PERCENT : Crop.Unit.PIXELS);
        if (this.getX() != null) {
            crop.setX(this.isPercent() ? this.getX() / 100f : this.getX());
        }
        if (this.getY() != null) {
            crop.setY(this.isPercent() ? this.getY() / 100f : this.getY());
        }
        if (this.getWidth() != null) {
            crop.setWidth(this.isPercent() ?
                    this.getWidth() / 100f : this.getWidth());
        }
        if (this.getHeight() != null) {
            crop.setHeight(this.isPercent() ?
                    this.getHeight() / 100f : this.getHeight());
        }
        return crop;
    }

    /**
     * @return Value compatible with the region component of a URI.
     */
    public String toString() {
        String str = "";
        if (this.isFull()) {
            str += "full";
        } else {
            String x, y;
            if (this.isPercent()) {
                x = StringUtil.removeTrailingZeroes(this.getX());
                y = StringUtil.removeTrailingZeroes(this.getY());
                str += "pct:";
            } else {
                x = Integer.toString(Math.round(this.getX()));
                y = Integer.toString(Math.round(this.getY()));
            }
            str += String.format("%s,%s,%s,%s", x, y,
                    StringUtil.removeTrailingZeroes(this.getWidth()),
                    StringUtil.removeTrailingZeroes(this.getHeight()));
        }
        return str;
    }

}
