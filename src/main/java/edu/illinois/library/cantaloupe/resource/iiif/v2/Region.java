package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.util.StringUtil;

/**
 * Encapsulates the "region" component of an IIIF request URI.
 *
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
     * @param uriRegion The "region" component of an IIIF URI.
     * @return
     * @throws IllegalArgumentException
     */
    public static Region fromUri(String uriRegion)
            throws IllegalArgumentException {
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
                    throw new IllegalArgumentException("Invalid region");
                }
                break;
        }
        return region;
    }

    @Override
    public boolean equals(Object obj) {
        final float fudge = 0.0001f;
        if (obj instanceof Region) {
            final Region region = (Region) obj;
            if (getType().equals(Type.SQUARE) &&
                    region.getType().equals(Type.SQUARE)) {
                return true;
            }
            return getType().equals(region.getType()) &&
                    Math.abs(getX() - region.getX()) < fudge &&
                    Math.abs(getY() - region.getY()) < fudge &&
                    Math.abs(getWidth() - region.getWidth()) < fudge &&
                    Math.abs(getHeight() - region.getHeight()) < fudge;
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
                        Math.abs(getX() - crop.getX() * 100) < fudge &&
                        Math.abs(getY() - crop.getY() * 100) < fudge &&
                        Math.abs(getWidth() - crop.getWidth() * 100) < fudge &&
                        Math.abs(getHeight() - crop.getHeight() * 100) < fudge;
            }
            return getType().equals(Type.PERCENT) ==
                    crop.getUnit().equals(Crop.Unit.PERCENT) &&
                    Math.abs(getX() - crop.getX()) < fudge &&
                    Math.abs(getY() - crop.getY()) < fudge &&
                    Math.abs(getWidth() - crop.getWidth()) < fudge &&
                    Math.abs(getHeight() - crop.getHeight()) < fudge;
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

    public void setHeight(Float height) throws IllegalArgumentException {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    public void setType(Type type) {
        this.type = type;
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
                break;
        }
        return str;
    }

}
