package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.image.Crop;

/**
 * Encapsulates the "region" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#region">IIIF Image API 2.0</a>
 */
class Region {

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

    @Override
    public boolean equals(Object obj) {
        final float fudge = 0.0001f;
        if (obj instanceof Region) {
            final Region region = (Region) obj;
            return isFull() == region.isFull() &&
                    isPercent() == region.isPercent() &&
                    Math.abs(getX() - region.getX()) < fudge &&
                    Math.abs(getY() - region.getY()) < fudge &&
                    Math.abs(getWidth() - region.getWidth()) < fudge &&
                    Math.abs(getHeight() - region.getHeight()) < fudge;
        }
        if (obj instanceof Crop) {
            final Crop crop = (Crop) obj;
            if (this.isPercent()) {
                return isFull() == crop.isFull() &&
                        isPercent() == crop.isPercent() &&
                        Math.abs(getX() - crop.getX() * 100) < fudge &&
                        Math.abs(getY() - crop.getY() * 100) < fudge &&
                        Math.abs(getWidth() - crop.getWidth() * 100) < fudge &&
                        Math.abs(getHeight() - crop.getHeight() * 100) < fudge;
            }
            return isFull() == crop.isFull() &&
                    isPercent() == crop.isPercent() &&
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
        crop.setPercent(this.isPercent());
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
     * @return Value compatible with the region component of an IIIF URI.
     */
    public String toString() {
        String str = "";
        if (this.isFull()) {
            str += "full";
        } else {
            String x, y;
            if (this.isPercent()) {
                x = NumberUtil.formatForUrl(this.getX());
                y = NumberUtil.formatForUrl(this.getY());
                str += "pct:";
            } else {
                x = Integer.toString(Math.round(this.getX()));
                y = Integer.toString(Math.round(this.getY()));
            }
            str += String.format("%s,%s,%s,%s", x, y,
                    NumberUtil.formatForUrl(this.getWidth()),
                    NumberUtil.formatForUrl(this.getHeight()));
        }
        return str;
    }

}
