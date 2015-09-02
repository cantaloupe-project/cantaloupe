package edu.illinois.library.image;

/**
 * Created by alexd on 9/2/15.
 */
public class Region {

    private Integer height;
    private boolean isFull = false;
    private boolean isPercent = false;
    private Integer width;
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
                region.setWidth(Integer.parseUnsignedInt(parts[2]));
                region.setHeight(Integer.parseUnsignedInt(parts[3]));
            } else {
                throw new IllegalArgumentException("Invalid region");
            }
        }
        return region;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
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

    public void setHeight(Integer height) throws IllegalArgumentException {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    public void setPercent(boolean isPercent) {
        this.isPercent = isPercent;
    }

    public void setWidth(Integer width) throws IllegalArgumentException {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    public void setX(Float x) throws IllegalArgumentException {
        if (x <= 0) {
            throw new IllegalArgumentException("X must be nonzero");
        }
        this.x = x;
    }

    public void setY(Float y) throws IllegalArgumentException {
        if (y <= 0) {
            throw new IllegalArgumentException("Y must be nonzero");
        }
        this.y = y;
    }

}
