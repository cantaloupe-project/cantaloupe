package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtils;

/**
 * Encapsulates the "region" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#region">IIIF Image API 2.0</a>
 * @see <a href="http://iiif.io/api/image/2.1/#region">IIIF Image API 2.1</a>
 */
class Region {

    enum Type {
        FULL, PERCENT, PIXELS, SQUARE
    }

    private Float height;
    private Type type;
    private Float width;
    private Float x;
    private Float y;

    /**
     * @param uriRegion Region component of a URI.
     * @return
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Region fromUri(String uriRegion) {
        Region region = new Region();

        switch (uriRegion) {
            case "full":
                region.setType(Type.FULL);
                break;
            case "square":
                region.setType(Type.SQUARE);
                break;
            default:
                String csv;
                if (uriRegion.startsWith("pct:")) {
                    region.setType(Type.PERCENT);
                    String[] tmp = uriRegion.split(":");
                    csv = tmp[1];
                } else {
                    region.setType(Type.PIXELS);
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
                break;
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
            if (getType().equals(Type.SQUARE) &&
                    region.getType().equals(Type.SQUARE)) {
                return true;
            }
            return getType().equals(region.getType()) &&
                    Math.abs(getX() - region.getX()) < delta &&
                    Math.abs(getY() - region.getY()) < delta &&
                    Math.abs(getWidth() - region.getWidth()) < delta &&
                    Math.abs(getHeight() - region.getHeight()) < delta;
        }
        if (obj instanceof Crop) {
            final Crop crop = (Crop) obj;
            if (getType().equals(Type.FULL) && crop.isFull()) {
                return true;
            } else if (getType().equals(Type.SQUARE) &&
                    crop.getShape().equals(Crop.Shape.SQUARE)) {
                return true;
            } else if (getType().equals(Type.PERCENT)) {
                return crop.getUnit().equals(Crop.Unit.PERCENT) &&
                        Math.abs(getX() - crop.getX() * 100) < delta &&
                        Math.abs(getY() - crop.getY() * 100) < delta &&
                        Math.abs(getWidth() - crop.getWidth() * 100) < delta &&
                        Math.abs(getHeight() - crop.getHeight() * 100) < delta;
            }
            return getType().equals(Type.PERCENT) ==
                    crop.getUnit().equals(Crop.Unit.PERCENT) &&
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

    public Type getType() {
        return type;
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

    public void setHeight(Float height){
        if (height <= 0) {
            throw new IllegalClientArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setWidth(Float width) {
        if (width <= 0) {
            throw new IllegalClientArgumentException("Width must be a positive integer");
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

    Crop toCrop() {
        Crop crop = new Crop();
        crop.setFull(this.getType().equals(Type.FULL));
        crop.setShape(this.getType().equals(Type.SQUARE) ?
                Crop.Shape.SQUARE : Crop.Shape.ARBITRARY);
        crop.setUnit(this.getType().equals(Type.PERCENT) ?
                Crop.Unit.PERCENT : Crop.Unit.PIXELS);
        if (this.getX() != null) {
            crop.setX(this.getType().equals(Type.PERCENT) ?
                    this.getX() / 100f : this.getX());
        }
        if (this.getY() != null) {
            crop.setY(this.getType().equals(Type.PERCENT) ?
                    this.getY() / 100f : this.getY());
        }
        if (this.getWidth() != null) {
            crop.setWidth(this.getType().equals(Type.PERCENT) ?
                    this.getWidth() / 100f : this.getWidth());
        }
        if (this.getHeight() != null) {
            crop.setHeight(this.getType().equals(Type.PERCENT) ?
                    this.getHeight() / 100f : this.getHeight());
        }
        return crop;
    }

    /**
     * @return Value compatible with the region component of an IIIF URI.
     */
    public String toString() {
        String str = "";
        switch (getType()) {
            case FULL:
                str += "full";
                break;
            case SQUARE:
                str += "square";
                break;
            default:
                String x, y;
                if (getType().equals(Type.PERCENT)) {
                    x = StringUtils.removeTrailingZeroes(this.getX());
                    y = StringUtils.removeTrailingZeroes(this.getY());
                    str += "pct:";
                } else {
                    x = Integer.toString(Math.round(this.getX()));
                    y = Integer.toString(Math.round(this.getY()));
                }
                str += String.format("%s,%s,%s,%s", x, y,
                        StringUtils.removeTrailingZeroes(this.getWidth()),
                        StringUtils.removeTrailingZeroes(this.getHeight()));
                break;
        }
        return str;
    }

}
